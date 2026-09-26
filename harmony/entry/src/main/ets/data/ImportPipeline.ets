// ImportPipeline —— source-neutral 导入管线（纯 ArkTS，零 @ohos；主机单测可执行）。
//
// 移植自 core/src/services/import-coordinator.ts + import-pipeline.ts 的冻结基准
// （GOAL MVP02 §19）。三段式：begin（parse + normalize + 商户分组 + 首次 resolution）
// → resolveMerchant（用户确认）→ finalize（指纹落库 → recurrence → 路由建议 →
// proposals/evidence/session 同事务提交）。
//
// 诚实边界：
//   - **哈希缝**：指纹哈希（HMAC-SHA256/SHA-256）属设备 crypto，本管线通过
//     `FingerprintHasher` 注入；主机测试注入确定性 mock，设备层注入 cryptoFramework 实现。
//   - 路由默认 = merchant_agreement(accountNodeId → service)（WeChat adapter 的
//     funding_source 建议属设备层 enrich，主机不伪造）。
//   - 未 resolution 的商户**不生成 Proposal**（AGENTS §15 / GOAL §14）。
//
// 分层约束：依赖 ../data/{GraphRepository,MerchantResolver,RecurrenceDetector}、
// ../sources/Parsers、../domain/SourceInstance、../data/{SchemaV3,PayloadCodec}。

import { Observation, ParseResult, WechatParser } from '../sources/Parsers';
import { ObservationDirection } from '../generated/CanonicalEnums';
import { GraphRepository } from '../data/GraphRepository';
import { ResolvableEntity, resolveMerchant } from '../data/MerchantResolver';
import { detectRecurrence } from '../data/RecurrenceDetector';
import { FingerprintScope } from '../domain/SourceInstance';
import {
  LEGACY_WECHAT_ADAPTER_ID,
  LEGACY_WECHAT_ADAPTER_VERSION,
  LEGACY_WECHAT_SOURCE_INSTANCE_ID,
} from '../data/SchemaV3';
import { NULL_VALUE, newRecord } from '../data/PayloadCodec';

/** 指纹哈希缝（设备层：cryptoFramework HMAC-SHA256 / SHA-256；主机测试：确定性 mock）。 */
export interface FingerprintHasher {
  hash(secret: string, scope: string): string;
}

export const FINGERPRINT_VERSION: number = 1;

export class MerchantCandidate {
  merchantRaw: string = '';
  resolvedNodeId: string = '';
  resolution: string = 'pending';
  observationCount: number = 0;
  firstObservedAt: string = '';
  lastObservedAt: string = '';
}

export class BeginResult {
  rawCount: number = 0;
  candidates: MerchantCandidate[] = [];
  errors: string[] = [];
}

export class FinalizeResult {
  newUniqueCount: number = 0;
  duplicateCount: number = 0;
  errorCount: number = 0;
  proposalKeys: string[] = [];
  unresolvedMerchants: string[] = [];
}

interface ScopedFingerprint {
  fingerprint: string;
  sourceInstanceId: string;
  version: number;
}

function merchantKeyOf(n: Record<string, Object>): string {
  const raw = n['merchantRaw'];
  return (typeof raw === 'string' ? (raw as string) : '').trim();
}

function observationOf(n: Record<string, Object>): Observation {
  const o = new Observation();
  o.source = LEGACY_WECHAT_ADAPTER_ID;
  const txn = n['sourceTxnId'];
  o.sourceTxnId = (typeof txn === 'string') ? (txn as string) : null;
  o.merchantTxnId = null;
  o.occurredAt = String(n['occurredAt']);
  o.merchantRaw = String(n['merchantRaw']);
  o.description = String(n['description']);
  o.amount = Number(n['amount']);
  o.currency = String(n['currency']);
  o.direction = (String(n['direction']) === 'out') ? ObservationDirection.OUT : ObservationDirection.IN;
  o.paymentMethodRaw = String(n['paymentMethodHint']);
  o.status = String(n['status']);
  o.note = '';
  return o;
}

/**
 * 导入管线（三段式，状态在实例内；同一实例不可复用）。
 * 构造时确保 legacy SourceInstance 与微信账户节点存在。
 */
export class ImportPipeline {
  private readonly repo: GraphRepository;
  private readonly hasher: FingerprintHasher;
  private readonly fpSecret: string;
  private accountNodeId: string = '';
  private begun: boolean = false;
  private finalized: boolean = false;
  private normalized: Array<Record<string, Object>> = [];
  private errors: string[] = [];
  private candidates = new Map<string, MerchantCandidate>();
  private manual = new Map<string, string>();
  private sessionId: string = '';

  constructor(repo: GraphRepository, hasher: FingerprintHasher, fpSecret: string) {
    this.repo = repo;
    this.hasher = hasher;
    this.fpSecret = fpSecret;
    // 微信账户节点（幂等：已有同名 account 节点则复用）
    let found = '';
    for (const node of repo.storeRef().nodes.values()) {
      if (String(node['kind']) === 'account' && String(node['name']) === '微信支付') {
        found = String(node['id']);
        break;
      }
    }
    this.accountNodeId = (found !== '') ? found : repo.createNode('node-wechat-account', 'account', '微信支付');
    repo.ensureLegacySourceInstance();
  }

  /** 阶段 1：parse + normalize + 商户分组 + 首次 resolution。 */
  begin(bytes: Uint8Array): BeginResult {
    if (this.begun) {
      throw new Error('pipeline already begun');
    }
    this.begun = true;
    const parsed: ParseResult = WechatParser.parse(bytes);
    this.errors = [];
    for (const e of parsed.errors) {
      this.errors.push(`line ${String(e.line)}: ${e.reason}`);
    }

    // normalize（identity 映射 + 默认值；WeChat adapter 级 enrich 属设备层）
    const normalized: Array<Record<string, Object>> = [];
    for (const o of parsed.observations) {
      const n = newRecord();
      n['sourceTxnId'] = (o.sourceTxnId === null) ? NULL_VALUE : o.sourceTxnId;
      n['occurredAt'] = o.occurredAt;
      n['merchantRaw'] = o.merchantRaw;
      n['description'] = o.description;
      n['amount'] = o.amount;
      n['currency'] = o.currency;
      n['direction'] = (o.direction === ObservationDirection.OUT) ? 'out' : 'in';
      n['paymentMethodHint'] = o.paymentMethodRaw;
      n['status'] = o.status;
      normalized.push(n);
    }
    this.normalized = normalized;
    this.sessionId = 'sess-import-1';

    // 可解析实体 = 现有节点
    const entities: ResolvableEntity[] = [];
    for (const node of this.repo.storeRef().nodes.values()) {
      const e = new ResolvableEntity();
      e.nodeId = String(node['id']);
      e.name = String(node['name']);
      entities.push(e);
    }

    // 商户分组 + 首次 resolution
    const grouped = new Map<string, number[]>();
    for (let i = 0; i < normalized.length; i++) {
      const key = merchantKeyOf(normalized[i]);
      if (key === '') {
        continue;
      }
      const list = grouped.get(key) ?? [];
      list.push(i);
      grouped.set(key, list);
    }
    for (const entry of grouped) {
      const merchantRaw = entry[0];
      const idxList = entry[1];
      const r = resolveMerchant(merchantRaw, entities);
      const cand = new MerchantCandidate();
      cand.merchantRaw = merchantRaw;
      cand.resolvedNodeId = (r.status === 'resolved') ? r.nodeId : '';
      cand.resolution = (r.status === 'resolved') ? 'auto' : 'pending';
      cand.observationCount = idxList.length;
      const firstIdx = idxList[0];
      const lastIdx = idxList[idxList.length - 1];
      if (firstIdx !== undefined && lastIdx !== undefined) {
        cand.firstObservedAt = String(normalized[firstIdx]['occurredAt']);
        cand.lastObservedAt = String(normalized[lastIdx]['occurredAt']);
      }
      this.candidates.set(merchantRaw, cand);
    }

    const result = new BeginResult();
    result.rawCount = normalized.length;
    result.errors = this.errors;
    const sorted: MerchantCandidate[] = [];
    for (const c of this.candidates.values()) {
      sorted.push(c);
    }
    sorted.sort((a, b) => b.observationCount - a.observationCount);
    result.candidates = sorted;
    return result;
  }

  /** 阶段 2：用户确认商户描述符 → 节点。 */
  resolveMerchant(merchantRaw: string, nodeId: string): void {
    if (!this.begun) {
      throw new Error('begin() must be called first');
    }
    const exists = this.repo.storeRef().nodes.has(nodeId);
    if (!exists) {
      throw new Error(`node not found: ${nodeId}`);
    }
    const candidate = this.candidates.get(merchantRaw);
    if (candidate === undefined) {
      throw new Error(`merchant not in this import: ${merchantRaw}`);
    }
    candidate.resolvedNodeId = nodeId;
    candidate.resolution = 'manual';
    this.manual.set(merchantRaw, nodeId);
  }

  /** 阶段 3：指纹（scope）→ recurrence → 路由 → proposals/evidence/session（同事务）。 */
  finalize(): FinalizeResult {
    if (!this.begun || this.finalized) {
      throw new Error('begin() must be called first');
    }
    this.finalized = true;
    const normalized = this.normalized;

    // 1. 指纹（哈希缝注入）
    const fps: ScopedFingerprint[] = [];
    const ordinalCounters = new Map<string, number>();
    for (const n of normalized) {
      const txn = n['sourceTxnId'];
      let scope: string;
      if (typeof txn === 'string' && (txn as string).length > 0) {
        scope = FingerprintScope.scopeKey(LEGACY_WECHAT_ADAPTER_ID, LEGACY_WECHAT_SOURCE_INSTANCE_ID, txn as string);
      } else {
        const signed = String(n['direction']) === 'out' ? -Number(n['amount']) : Number(n['amount']);
        const core = FingerprintScope.canonicalRowCore(
          String(n['occurredAt']), signed.toFixed(2), String(n['currency']),
          String(n['description']), String(n['merchantRaw']), String(n['status']));
        const scoped = FingerprintScope.canonicalRowScope(LEGACY_WECHAT_ADAPTER_ID, LEGACY_WECHAT_SOURCE_INSTANCE_ID, core);
        const next = (ordinalCounters.get(scoped) ?? 0) + 1;
        ordinalCounters.set(scoped, next);
        scope = next === 1 ? scoped : scoped + '#' + String(next);
      }
      const fp: ScopedFingerprint = {
        fingerprint: this.hasher.hash(this.fpSecret, scope),
        sourceInstanceId: LEGACY_WECHAT_SOURCE_INSTANCE_ID,
        version: FINGERPRINT_VERSION,
      };
      fps.push(fp);
    }

    // 2. fresh / duplicates（批内去重 + 库内已存在判定）
    const store = this.repo.storeRef();
    const seenInBatch = new Set<string>();
    const fresh: string[] = [];
    let duplicates = 0;
    for (const rec of fps) {
      const scoped = `${rec.sourceInstanceId}\u0000${String(rec.version)}\u0000${rec.fingerprint}`;
      if (seenInBatch.has(scoped) || store.observationFingerprints.has(scoped)) {
        duplicates++;
      } else {
        seenInBatch.add(scoped);
        fresh.push(rec.fingerprint);
      }
    }
    const freshSet = new Set(fresh);
    const usedFresh = new Set<string>();
    const freshIdx: number[] = [];
    for (let i = 0; i < fps.length; i++) {
      const f = fps[i].fingerprint;
      if (freshSet.has(f) && !usedFresh.has(f)) {
        usedFresh.add(f);
        freshIdx.push(i);
      }
    }

    // 3. 商户分组（fresh only）→ recurrence → 路由 → proposals
    const proposalKeys: string[] = [];
    const unresolved: string[] = [];
    const merchantGroups = new Map<string, number[]>();
    for (const i of freshIdx) {
      const key = merchantKeyOf(normalized[i]);
      if (key === '') {
        continue;
      }
      const list = merchantGroups.get(key) ?? [];
      list.push(i);
      merchantGroups.set(key, list);
    }
    for (const entry of merchantGroups) {
      const merchantRaw = entry[0];
      const idxList = entry[1];
      const serviceNodeId = this.merchantToNode(merchantRaw);
      if (serviceNodeId === '') {
        unresolved.push(merchantRaw); // 未 resolution → 不产生 Proposal（GOAL §14）
        continue;
      }
      const obs: Observation[] = [];
      for (const i of idxList) {
        obs.push(observationOf(normalized[i]));
      }
      const rec = detectRecurrence(merchantRaw, obs);
      if (rec === null) {
        continue;
      }
      // 默认路由：merchant_agreement(accountNodeId → service)
      const proposalKey = `${this.accountNodeId}|merchant_agreement|${serviceNodeId}|payment`;
      this.repo.upsertProposal(proposalKey, 'prop-import', this.accountNodeId, 'merchant_agreement',
        serviceNodeId, 'payment', 'pending', idxList.length);
      this.repo.recordEvidence(proposalKey, LEGACY_WECHAT_SOURCE_INSTANCE_ID, 'ev-import', idxList.length);
      proposalKeys.push(proposalKey);
    }

    // 4. 指纹落库 + session 完成（proposal/evidence/fingerprint/session 不 bump revision）
    for (const rec of fps) {
      if (freshSet.has(rec.fingerprint)) {
        this.repo.recordFingerprint(rec.sourceInstanceId, rec.version, rec.fingerprint);
      }
    }
    const uniqueProposals = new Set(proposalKeys);
    this.repo.recordImportSession(this.sessionId, normalized.length, fresh.length, duplicates,
      uniqueProposals.size, this.errors.length);

    const result = new FinalizeResult();
    result.newUniqueCount = fresh.length;
    result.duplicateCount = duplicates;
    result.errorCount = this.errors.length;
    result.unresolvedMerchants = unresolved;
    for (const k of uniqueProposals) {
      result.proposalKeys.push(k);
    }
    return result;
  }

  private merchantToNode(merchantRaw: string): string {
    const candidate = this.candidates.get(merchantRaw);
    const auto = (candidate === undefined) ? '' : candidate.resolvedNodeId;
    if (auto !== '') {
      return auto;
    }
    return this.manual.get(merchantRaw) ?? '';
  }
}