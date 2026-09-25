// Payload 迁移（`depmap-logical-graph` v1/v2 → v3）—— 纯 ArkTS 实现，零 @ohos 依赖。
//
// 移植自 core/src/services/graph-serialize.ts 的冻结基准（migratePayloadV1toV2 /
// migratePayloadV2toV3 / migrateIfNeeded）。语义契约见 spec/migration/migration-spec.md §2：
//   - v1→v2：版本提升、生成 legacy SourceInstance、evidence_id → proposal_evidence_refs、
//     全部 legacy 数据归属 legacy 实例、verification_basis 补 user_confirmed
//   - v2→v3：payloadVersion 3 / schemaVersion 3、meta 补 graph_revision（缺省 '0'）
//   - 拒绝：payloadVersion ∈ {0,4,99} / 非整数、schemaVersion > 支持版本、
//     payloadKind 不匹配、非 JSON / '[]' / 'null'、步骤入口版本不匹配
//   - **纯内存**：不修改传入字符串、不触碰任何 DB；输出确定性（同输入逐字节一致）
//
// ArkTS 约束下的实现说明：
//   - 对象一律用 PayloadCodec.newRecord() 构造（禁止未声明类型字面量）；
//   - null 一律用 NULL_VALUE（ArkTS 不允许字面量 null 赋给 Object）；
//   - 不做 Object.keys / delete；按 SchemaV3 列清单构造新对象，
//     「缺失列 = key 不存在 = undefined」，与 TS 丢弃 undefined 键的行为对齐
//     （本实现选择**不写缺失列**，保持与 TS 输出逐字节一致）。

import {
  DEPENDENCY_GROUPS_COLUMNS,
  DEPENDENCIES_COLUMNS,
  DEPENDENCY_PROPOSALS_COLUMNS,
  GRAPH_PAYLOAD_KIND,
  IMPORT_SESSIONS_COLUMNS,
  LEGACY_WECHAT_ADAPTER_ID,
  LEGACY_WECHAT_ADAPTER_VERSION,
  LEGACY_WECHAT_SOURCE_INSTANCE_ID,
  SCHEMA_VERSION,
} from '../data/SchemaV3';
import {
  NULL_VALUE,
  PayloadError,
  newRecord,
  parsePayloadObject,
} from '../data/PayloadCodec';

/** deterministic legacy WeChat SourceInstance 行（payload 形状，与 TS 逐字节一致）。 */
export function legacyWechatInstance(): Record<string, Object> {
  const rec = newRecord();
  rec['id'] = LEGACY_WECHAT_SOURCE_INSTANCE_ID;
  rec['adapter_id'] = LEGACY_WECHAT_ADAPTER_ID;
  rec['adapter_version'] = LEGACY_WECHAT_ADAPTER_VERSION;
  rec['source_kind'] = 'statement_file';
  rec['provider_id'] = NULL_VALUE;
  rec['account_node_id'] = NULL_VALUE;
  rec['label'] = 'Legacy WeChat Statement Source';
  rec['country'] = NULL_VALUE;
  rec['jurisdiction'] = NULL_VALUE;
  rec['currencies_json'] = '["CNY"]';
  rec['state'] = 'active';
  rec['created_at'] = '1970-01-01T00:00:00.000Z';
  rec['updated_at'] = '1970-01-01T00:00:00.000Z';
  rec['last_ingested_at'] = NULL_VALUE;
  return rec;
}

/** 复制 src 中存在的列（缺失列 = undefined → 跳过，保持 TS 输出形状）。 */
function copyPresent(out: Record<string, Object>, src: Record<string, Object>, cols: string[]): void {
  for (const c of cols) {
    const v: Object = src[c];
    if (v !== undefined) {
      out[c] = v;
    }
  }
}

function numberEq(v: Object, expected: number): boolean {
  if (typeof v !== 'number') {
    return false;
  }
  return (v as number) === expected;
}

function rowsArrayOf(src: Record<string, Object>, table: string): Array<Record<string, Object>> {
  const value: Object = src[table];
  if (value === undefined || !(value instanceof Array)) {
    return [];
  }
  const out: Array<Record<string, Object>> = [];
  for (const r of (value as Array<Object>)) {
    if (r !== null && typeof r === 'object' && !(r instanceof Array)) {
      out.push(r as Record<string, Object>);
    }
  }
  return out;
}

/**
 * v1 payload → v2 payload（in-memory，GOAL MVP02 §15 / migration-spec §2.3）。
 * 输入必须是 payloadVersion === 1 的 `depmap-logical-graph`，否则拒绝。
 */
export function migratePayloadV1toV2(payloadJson: string): string {
  const v1 = parsePayloadObject(payloadJson);
  const kind = v1['payloadKind'];
  if (typeof kind !== 'string' || (kind as string) !== GRAPH_PAYLOAD_KIND) {
    throw new PayloadError('payload_kind_mismatch', `payloadKind must be ${GRAPH_PAYLOAD_KIND}`);
  }
  if (!numberEq(v1['payloadVersion'], 1)) {
    throw new PayloadError(
      'payload_invalid',
      `migratePayloadV1toV2 expects payloadVersion 1, got ${String(v1['payloadVersion'])}`,
    );
  }

  const v2 = newRecord();
  v2['payloadKind'] = GRAPH_PAYLOAD_KIND;
  v2['payloadVersion'] = 2; // v1→v2 迁移器固定输出 v2 形状（随后由 migratePayloadV2toV3 升 v3）
  v2['schemaVersion'] = 2;
  // 未重建的表原样携带（meta / nodes / dependency_group_proposals）
  copyPresent(v2, v1, ['meta', 'nodes', 'dependency_group_proposals']);

  // fingerprints：归属 legacy 实例；fingerprint_version 缺省 1
  const fingerprints: Array<Record<string, Object>> = [];
  for (const r of rowsArrayOf(v1, 'observation_fingerprints')) {
    const out = newRecord();
    out['fingerprint'] = r['fingerprint'];
    out['source_instance_id'] = LEGACY_WECHAT_SOURCE_INSTANCE_ID;
    const srcV: Object = r['source'];
    out['source'] = (typeof srcV === 'string') ? (srcV as string) : '';
    const ver: Object = r['fingerprint_version'];
    out['fingerprint_version'] = (ver !== undefined) ? ver : 1;
    copyPresent(out, r, ['import_session_id', 'first_seen_at']);
    fingerprints.push(out);
  }

  // evidence：归属 legacy 实例 + provenance 列
  const evidence: Array<Record<string, Object>> = [];
  for (const r of rowsArrayOf(v1, 'evidence')) {
    const out = newRecord();
    out['id'] = r['id'];
    out['proposal_key'] = r['proposal_key'];
    out['source_instance_id'] = LEGACY_WECHAT_SOURCE_INSTANCE_ID;
    out['adapter_id'] = LEGACY_WECHAT_ADAPTER_ID;
    const pv: Object = r['parser_version'];
    out['adapter_version'] = (typeof pv === 'number') ? (pv as number) : LEGACY_WECHAT_ADAPTER_VERSION;
    out['evidence_kind'] = 'transaction_stream';
    copyPresent(out, r, ['source_type', 'parser_id', 'parser_version',
      'last_import_session_id', 'first_observed_at', 'last_observed_at',
      'observation_count', 'created_at', 'updated_at']);
    evidence.push(out);
  }

  // dependency_proposals：删 evidence_id（v3 列清单本无此项，自然不复制）+
  // 补 rejected_at_stream_counts_json 空快照（legacy 单流计数以 evidence 流计数为准）
  const proposals: Array<Record<string, Object>> = [];
  const refs: Array<Record<string, Object>> = [];
  let refIndex = 0;
  for (const r of rowsArrayOf(v1, 'dependency_proposals')) {
    const out = newRecord();
    copyPresent(out, r, DEPENDENCY_PROPOSALS_COLUMNS);
    out['rejected_at_stream_counts_json'] = NULL_VALUE;
    proposals.push(out);
    const ev: Object = r['evidence_id'];
    if (ev !== null && ev !== undefined && typeof ev === 'string') {
      const ref = newRecord();
      ref['proposal_key'] = r['key'];
      ref['evidence_id'] = ev;
      ref['position'] = refIndex;
      refs.push(ref);
      refIndex++;
    }
  }
  v2['source_instances'] = [legacyWechatInstance()];
  v2['observation_fingerprints'] = fingerprints;
  v2['evidence'] = evidence;
  v2['dependency_proposals'] = proposals;
  v2['proposal_evidence_refs'] = refs;

  // import_sessions：补 provenance 三列
  const sessions: Array<Record<string, Object>> = [];
  for (const r of rowsArrayOf(v1, 'import_sessions')) {
    const out = newRecord();
    copyPresent(out, r, IMPORT_SESSIONS_COLUMNS);
    out['source_instance_id'] = LEGACY_WECHAT_SOURCE_INSTANCE_ID;
    out['adapter_id'] = LEGACY_WECHAT_ADAPTER_ID;
    out['adapter_version'] = LEGACY_WECHAT_ADAPTER_VERSION;
    sessions.push(out);
  }
  v2['import_sessions'] = sessions;

  // dependencies / dependency_groups：补 verification_basis（默认 user_confirmed）
  const deps: Array<Record<string, Object>> = [];
  for (const r of rowsArrayOf(v1, 'dependencies')) {
    const out = newRecord();
    copyPresent(out, r, DEPENDENCIES_COLUMNS);
    if (out['verification_basis_type'] === undefined) {
      out['verification_basis_type'] = 'user_confirmed';
    }
    if (out['verification_basis_json'] === undefined) {
      out['verification_basis_json'] = NULL_VALUE;
    }
    deps.push(out);
  }
  v2['dependencies'] = deps;

  const groups: Array<Record<string, Object>> = [];
  for (const r of rowsArrayOf(v1, 'dependency_groups')) {
    const out = newRecord();
    copyPresent(out, r, DEPENDENCY_GROUPS_COLUMNS);
    if (out['verification_basis_type'] === undefined) {
      out['verification_basis_type'] = 'user_confirmed';
    }
    if (out['verification_basis_json'] === undefined) {
      out['verification_basis_json'] = NULL_VALUE;
    }
    groups.push(out);
  }
  v2['dependency_groups'] = groups;

  return JSON.stringify(v2);
}

/**
 * v2 payload → v3 payload（in-memory，MVP03 §57 / migration-spec §2.4）：
 * - payloadVersion 2 → 3；schemaVersion → 3
 * - meta 补 graph_revision（缺省 '0'；已存在则保留）
 * 与 TS 一致：本步骤只校验 payloadVersion，不重复校验 kind。
 */
export function migratePayloadV2toV3(payloadJson: string): string {
  const v2 = parsePayloadObject(payloadJson);
  if (!numberEq(v2['payloadVersion'], 2)) {
    throw new PayloadError(
      'payload_invalid',
      `migratePayloadV2toV3 expects payloadVersion 2, got ${String(v2['payloadVersion'])}`,
    );
  }
  const v3 = newRecord();
  v3['payloadKind'] = GRAPH_PAYLOAD_KIND;
  v3['payloadVersion'] = 3;
  v3['schemaVersion'] = SCHEMA_VERSION;
  copyPresent(v3, v2, [
    'meta', 'nodes', 'dependencies', 'dependency_groups', 'dependency_proposals',
    'dependency_group_proposals', 'proposal_evidence_refs', 'evidence',
    'observation_fingerprints', 'import_sessions', 'source_instances',
  ]);
  const meta: Array<Record<string, Object>> = rowsArrayOf(v2, 'meta');
  let hasRevision = false;
  for (const m of meta) {
    if (m['key'] === 'graph_revision') {
      hasRevision = true;
      break;
    }
  }
  if (!hasRevision) {
    const extended: Array<Record<string, Object>> = meta.slice();
    const rev = newRecord();
    rev['key'] = 'graph_revision';
    rev['value'] = '0';
    extended.push(rev);
    v3['meta'] = extended;
  }
  return JSON.stringify(v3);
}

/**
 * 导入前的自动迁移分发（与 TS migrateIfNeeded 一致）：
 * v1 → v1toV2 → v2toV3；v2 → v2toV3；其它原样返回（由 validatePayload 拒绝）。
 */
export function migrateIfNeeded(payloadJson: string): string {
  let parsed: Object;
  try {
    parsed = JSON.parse(payloadJson) as Object;
  } catch (e) {
    return payloadJson;
  }
  if (parsed === null || typeof parsed !== 'object' || parsed instanceof Array) {
    return payloadJson;
  }
  const rec = parsed as Record<string, Object>;
  const pv: Object = rec['payloadVersion'];
  if (typeof pv === 'number') {
    if ((pv as number) === 1) {
      return migratePayloadV2toV3(migratePayloadV1toV2(payloadJson));
    }
    if ((pv as number) === 2) {
      return migratePayloadV2toV3(payloadJson);
    }
  }
  return payloadJson;
}