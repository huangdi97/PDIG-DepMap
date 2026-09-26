// GraphRepository —— 内存图仓库（纯 ArkTS，零 @ohos；主机单测可执行）。
//
// 移植自 core/src/repositories/{dependency,node,graph-revision,group,
// proposal,evidence,fingerprint,import-session,source-instance}-repository.ts
// 与 core/src/services/graph-serialize.ts（importGraph 原子替换）的**逻辑语义**。
//
// 语义铁律（AGENTS §9 / persistence-contract.md / migration-spec.md）：
//   - Dependency 存在即用户确认；同一 logical key：INSERT / verify(UPDATE) / re-activate(同 id)
//   - 机器永不自动产生 required（缺省 criticality = unknown）
//   - 仅 Confirmed Reality Mutation bump graph_revision，且与 mutation **同事务**
//   - proposal 同 key UPSERT（pending 累计；accepted 不重复问；rejected 需新证据）
//   - evidence / fingerprint 唯一键（SRC-01 维度）
//   - payload import 原子替换：任一校验失败 → 存储逐字节保持原样
//   - 行对象只换不改（replace-discipline），withTransaction 失败整体回滚
//
// 分层约束：依赖 ../data/{SchemaV3,PayloadCodec,PayloadMigration,GraphStore}。

import {
  DEPENDENCIES_COLUMNS,
  DEPENDENCY_GROUPS_COLUMNS,
  DEPENDENCY_GROUP_PROPOSALS_COLUMNS,
  DEPENDENCY_PROPOSALS_COLUMNS,
  EVIDENCE_COLUMNS,
  IMPORT_SESSIONS_COLUMNS,
  LEGACY_WECHAT_ADAPTER_ID,
  LEGACY_WECHAT_ADAPTER_VERSION,
  LEGACY_WECHAT_SOURCE_INSTANCE_ID,
  META_COLUMNS,
  NODES_COLUMNS,
  OBSERVATION_FINGERPRINTS_COLUMNS,
  PROPOSAL_EVIDENCE_REFS_COLUMNS,
  SOURCE_INSTANCES_COLUMNS,
} from '../data/SchemaV3';
import {
  NULL_VALUE,
  buildPayloadJson,
  newRecord,
  parsePayloadObject,
  validatePayload,
} from '../data/PayloadCodec';
import { migrateIfNeeded } from '../data/PayloadMigration';
import {
  META_KEY_GRAPH_REVISION,
  META_KEY_SCHEMA_VERSION,
  GraphStore,
} from '../data/GraphStore';

const NOW: string = '2026-09-25T00:00:00.000Z';

/** 逻辑键：from|relation|to|capability（persistence-contract §1.1）。 */
export function dependencyLogicalKeyOf(from: string, relation: string, to: string, capability: string): string {
  return `${from}|${relation}|${to}|${capability}`;
}

export class ConfirmResult {
  id: string = '';
  reactivated: boolean = false;
  verified: boolean = false;
}

export class GraphRepository {
  private readonly store: GraphStore;
  private seq: number = 0;

  constructor(store: GraphStore) {
    this.store = store;
  }

  // ---------------------------------------------------------------------------
  // 事务 / 快照
  // ---------------------------------------------------------------------------

  /** 事务：fn 抛异常 → 存储整体回滚到事务前；成功 → 提交。 */
  withTransaction<T>(fn: () => T): T {
    const before = this.store.clone();
    try {
      return fn();
    } catch (e) {
      this.store.restoreFrom(before);
      const err = (e instanceof Error) ? (e as Error) : new Error(String(e));
      throw err;
    }
  }

  // ---------------------------------------------------------------------------
  // meta / revision
  // ---------------------------------------------------------------------------

  getRevision(): number {
    const v = this.store.meta.get(META_KEY_GRAPH_REVISION);
    if (v === undefined) {
      return 0;
    }
    const n = Number(v);
    return Number.isFinite(n) ? n : 0;
  }

  private bumpRevision(): void {
    const next = this.getRevision() + 1;
    this.store.meta.set(META_KEY_GRAPH_REVISION, String(next));
  }

  getSchemaVersion(): number {
    const v = this.store.meta.get(META_KEY_SCHEMA_VERSION);
    return (v !== undefined) ? Number(v) : this.store.schemaVersion;
  }

  setSchemaVersion(v: number): void {
    this.store.schemaVersion = v;
    this.store.meta.set(META_KEY_SCHEMA_VERSION, String(v));
  }

  private nextId(prefix: string): string {
    this.seq++;
    return `${prefix}-${String(this.seq)}`;
  }

  // ---------------------------------------------------------------------------
  // Node
  // ---------------------------------------------------------------------------

  /** node_create → revision +1（GR 契约 bumpsOn）。 */
  createNode(id: string, kind: string, name: string): string {
    return this.withTransaction(() => {
      const row = newRecord();
      row['id'] = id;
      row['kind'] = kind;
      row['template_id'] = NULL_VALUE;
      row['name'] = name;
      row['issuer'] = NULL_VALUE;
      row['last4'] = NULL_VALUE;
      row['owner'] = 'self';
      row['archived'] = 0;
      row['fields_json'] = '{}';
      row['vault_ref'] = NULL_VALUE;
      row['wallet_ref'] = NULL_VALUE;
      row['created_at'] = NOW;
      row['updated_at'] = NOW;
      this.store.nodes.set(id, row);
      this.bumpRevision();
      return id;
    });
  }

  /** node_archive → revision +1。 */
  archiveNode(id: string): void {
    this.withTransaction(() => {
      const row = this.store.nodes.get(id);
      if (row === undefined) {
        throw new Error(`node not found: ${id}`);
      }
      const updated = cloneRow(row);
      updated['archived'] = 1;
      updated['updated_at'] = NOW;
      this.store.nodes.set(id, updated);
      this.bumpRevision();
    });
  }

  // ---------------------------------------------------------------------------
  // Dependency —— 与 dependency-repository.ts 逐语义对齐
  // ---------------------------------------------------------------------------

  findDependencyByLogicalKey(from: string, relation: string, to: string, capability: string): Record<string, Object> | null {
    for (const row of this.store.dependencies.values()) {
      if (String(row['from_node']) === from && String(row['relation']) === relation &&
        String(row['to_node']) === to && String(row['capability']) === capability) {
        return row;
      }
    }
    return null;
  }

  getDependency(id: string): Record<string, Object> | null {
    const row = this.store.dependencies.get(id);
    return (row === undefined) ? null : row;
  }

  /** 确认边：INSERT / verify / re-activate（同 id）。 */
  confirmDependency(
    from: string, relation: string, to: string, capability: string,
    criticality: string, origin: string, idHint: string,
  ): ConfirmResult {
    return this.withTransaction(() => {
      const existing = this.findDependencyByLogicalKey(from, relation, to, capability);
      if (existing === null) {
        const id = (idHint.length > 0) ? idHint : this.nextId('dep');
        const row = newRecord();
        row['id'] = id;
        row['from_node'] = from;
        row['relation'] = relation;
        row['to_node'] = to;
        row['capability'] = capability;
        // 仓库边界安全：无效/缺失 criticality 收敛为 unknown，**永不自动 required**
        // （AGENTS §9：机器推断不得设置 required；与 TS 的 `?? 'unknown'` 同向且更强）
        row['criticality'] = (criticality.length > 0) ? criticality : 'unknown';
        row['group_id'] = NULL_VALUE;
        row['state'] = 'active';
        row['origin'] = origin;
        row['confirmed_at'] = NOW;
        row['last_verified_at'] = NOW;
        row['retired_at'] = NULL_VALUE;
        row['evidence_refs_json'] = '[]';
        row['verification_basis_type'] = 'user_confirmed';
        row['verification_basis_json'] = NULL_VALUE;
        row['created_at'] = NOW;
        row['updated_at'] = NOW;
        this.store.dependencies.set(id, row);
        this.bumpRevision(); // GR：Dependency created → +1（同事务）
        const r = new ConfirmResult();
        r.id = id;
        r.reactivated = false;
        r.verified = false;
        return r;
      }
      if (String(existing['state']) === 'active') {
        // verify 更新：合并 evidence refs、criticality 不降级 —— 不 bump
        const merged = mergeEvidenceRefs(existing, []);
        const updated = cloneRow(existing);
        updated['last_verified_at'] = NOW;
        updated['evidence_refs_json'] = merged;
        updated['criticality'] = (criticality.length > 0) ? criticality : existing['criticality'];
        updated['updated_at'] = NOW;
        this.store.dependencies.set(String(existing['id']), updated);
        const r = new ConfirmResult();
        r.id = String(existing['id']);
        r.reactivated = false;
        r.verified = true;
        return r;
      }
      // retired → re-activate 同一 id → bump
      const merged = mergeEvidenceRefs(existing, []);
      const updated = cloneRow(existing);
      updated['state'] = 'active';
      updated['retired_at'] = NULL_VALUE;
      updated['confirmed_at'] = NOW;
      updated['last_verified_at'] = NOW;
      updated['evidence_refs_json'] = merged;
      if (criticality.length > 0) {
        updated['criticality'] = criticality;
      }
      updated['updated_at'] = NOW;
      this.store.dependencies.set(String(existing['id']), updated);
      this.bumpRevision(); // GR：retired → re-activate → +1
      const r = new ConfirmResult();
      r.id = String(existing['id']);
      r.reactivated = true;
      r.verified = false;
      return r;
    });
  }

  /** retire：不存在抛错；已 retired 幂等（不重复 bump）。 */
  retireDependency(id: string): void {
    this.withTransaction(() => {
      const row = this.store.dependencies.get(id);
      if (row === undefined) {
        throw new Error(`dependency not found: ${id}`);
      }
      if (String(row['state']) === 'retired') {
        return;
      }
      const updated = cloneRow(row);
      updated['state'] = 'retired';
      updated['retired_at'] = NOW;
      updated['updated_at'] = NOW;
      this.store.dependencies.set(id, updated);
      this.bumpRevision(); // GR：retired → +1（同事务）
    });
  }

  /** criticality 变更（仅用户路径调用）→ 变化时 bump。 */
  updateCriticality(id: string, criticality: string): void {
    this.withTransaction(() => {
      const row = this.store.dependencies.get(id);
      if (row === undefined) {
        throw new Error(`dependency not found: ${id}`);
      }
      if (String(row['criticality']) === criticality) {
        return;
      }
      const updated = cloneRow(row);
      updated['criticality'] = criticality;
      updated['updated_at'] = NOW;
      this.store.dependencies.set(id, updated);
      this.bumpRevision();
    });
  }

  // ---------------------------------------------------------------------------
  // Group
  // ---------------------------------------------------------------------------

  /** group_confirm → revision +1（重复 confirm 幂等 verify，不 bump）。 */
  confirmGroup(groupKey: string, targetNodeId: string, mode: string, memberEdgeIds: string[], idHint: string): string {
    return this.withTransaction(() => {
      const existing = this.findGroupByKey(groupKey);
      if (existing !== null) {
        if (String(existing['state']) === 'retired') {
          const updated = cloneRow(existing);
          updated['state'] = 'active';
          updated['retired_at'] = NULL_VALUE;
          updated['confirmed_at'] = NOW;
          updated['updated_at'] = NOW;
          this.store.dependencyGroups.set(String(existing['id']), updated);
          this.bumpRevision();
          return String(existing['id']);
        }
        return String(existing['id']);
      }
      const id = (idHint.length > 0) ? idHint : this.nextId('grp');
      const row = newRecord();
      row['id'] = id;
      row['group_key'] = groupKey;
      row['target_node_id'] = targetNodeId;
      row['capability'] = 'payment';
      row['mode'] = mode;
      row['member_edge_ids_json'] = JSON.stringify(memberEdgeIds);
      row['state'] = 'active';
      row['confirmed_at'] = NOW;
      row['last_verified_at'] = NOW;
      row['verification_basis_type'] = 'user_confirmed';
      row['verification_basis_json'] = NULL_VALUE;
      row['created_at'] = NOW;
      row['updated_at'] = NOW;
      this.store.dependencyGroups.set(id, row);
      // 成员边回填 group_id
      for (const edgeId of memberEdgeIds) {
        const edge = this.store.dependencies.get(edgeId);
        if (edge !== undefined) {
          const updatedEdge = cloneRow(edge);
          updatedEdge['group_id'] = id;
          updatedEdge['updated_at'] = NOW;
          this.store.dependencies.set(edgeId, updatedEdge);
        }
      }
      this.bumpRevision(); // GR：group_confirm → +1
      return id;
    });
  }

  retireGroup(id: string): void {
    this.withTransaction(() => {
      const row = this.store.dependencyGroups.get(id);
      if (row === undefined) {
        throw new Error(`group not found: ${id}`);
      }
      if (String(row['state']) === 'retired') {
        return;
      }
      const updated = cloneRow(row);
      updated['state'] = 'retired';
      updated['retired_at'] = NOW;
      updated['updated_at'] = NOW;
      this.store.dependencyGroups.set(id, updated);
      this.bumpRevision(); // GR：group_retire → +1
    });
  }

  private findGroupByKey(groupKey: string): Record<string, Object> | null {
    for (const row of this.store.dependencyGroups.values()) {
      if (String(row['group_key']) === groupKey) {
        return row;
      }
    }
    return null;
  }

  // ---------------------------------------------------------------------------
  // Proposal（同 key UPSERT；不 bump revision —— GR 契约 neverBumpsOn）
  // ---------------------------------------------------------------------------

  /** 同 key：pending 累计 observation_count；accepted / rejected 状态保留。 */
  upsertProposal(key: string, id: string, from: string, relation: string, to: string,
    capability: string, decision: string, observationCount: number): void {
    this.withTransaction(() => {
      const existing = this.store.dependencyProposals.get(key);
      if (existing === undefined) {
        const row = newRecord();
        row['id'] = id;
        row['key'] = key;
        row['from_node'] = from;
        row['relation'] = relation;
        row['to_node'] = to;
        row['capability'] = capability;
        row['proposal_type'] = 'transaction_stream';
        row['source'] = 'wechat_statement';
        row['parser_id'] = 'wechat';
        row['parser_version'] = 1;
        row['confidence_score'] = 0.9;
        row['path_json'] = '[]';
        row['decision'] = decision;
        row['decided_at'] = NULL_VALUE;
        row['criticality_decision'] = NULL_VALUE;
        row['observation_count'] = observationCount;
        row['rejected_at'] = NULL_VALUE;
        row['rejected_at_stream_counts_json'] = NULL_VALUE;
        row['created_at'] = NOW;
        row['updated_at'] = NOW;
        this.store.dependencyProposals.set(key, row);
        return;
      }
      const updated = cloneRow(existing);
      const cur = Number(updated['observation_count']);
      updated['observation_count'] = (Number.isFinite(cur) ? cur : 0) + observationCount;
      updated['updated_at'] = NOW;
      // accepted 不重复问：状态与 decided_at 保留
      this.store.dependencyProposals.set(key, updated);
    });
  }

  /** 决策切换：pending → accepted/rejected；accepted 后同 key 不再产生新 pending。 */
  setProposalDecision(key: string, decision: string): void {
    this.withTransaction(() => {
      const row = this.store.dependencyProposals.get(key);
      if (row === undefined) {
        throw new Error(`proposal not found: ${key}`);
      }
      if (String(row['decision']) === decision) {
        return;
      }
      const updated = cloneRow(row);
      updated['decision'] = decision;
      updated['decided_at'] = NOW;
      updated['updated_at'] = NOW;
      this.store.dependencyProposals.set(key, updated);
    });
  }

  /** SRC-04：rejected 后重提需要该 stream 的新 evidence（≥3）。 */
  canRePropose(key: string, streamCounts: Map<string, number>): boolean {
    const row = this.store.dependencyProposals.get(key);
    if (row === undefined || String(row['decision']) !== 'rejected') {
      return false;
    }
    const cur = Number(row['observation_count']);
    let maxNew = 0;
    for (const c of streamCounts.values()) {
      maxNew += c;
    }
    return (Number.isFinite(cur) ? cur : 0) + maxNew >= 3;
  }

  // ---------------------------------------------------------------------------
  // Evidence / Fingerprint / ImportSession / SourceInstance
  // ---------------------------------------------------------------------------

  /** evidence 唯一键：(proposal_key, source_instance_id)；同键累计，不 bump。 */
  recordEvidence(proposalKey: string, sourceInstanceId: string, id: string, count: number): void {
    this.withTransaction(() => {
      const key = `${proposalKey}\u0000${sourceInstanceId}`;
      const existing = this.store.evidence.get(key);
      if (existing === undefined) {
        const row = newRecord();
        row['id'] = id;
        row['proposal_key'] = proposalKey;
        row['source_instance_id'] = sourceInstanceId;
        row['adapter_id'] = LEGACY_WECHAT_ADAPTER_ID;
        row['adapter_version'] = LEGACY_WECHAT_ADAPTER_VERSION;
        row['evidence_kind'] = 'transaction_stream';
        row['source_type'] = 'statement_file';
        row['parser_id'] = 'wechat';
        row['parser_version'] = 1;
        row['last_import_session_id'] = NULL_VALUE;
        row['first_observed_at'] = NOW;
        row['last_observed_at'] = NOW;
        row['observation_count'] = count;
        row['created_at'] = NOW;
        row['updated_at'] = NOW;
        this.store.evidence.set(key, row);
        return;
      }
      const updated = cloneRow(existing);
      const cur = Number(updated['observation_count']);
      updated['observation_count'] = (Number.isFinite(cur) ? cur : 0) + count;
      updated['last_observed_at'] = NOW;
      updated['updated_at'] = NOW;
      this.store.evidence.set(key, updated);
    });
  }

  /** fingerprint 唯一键：(source_instance_id, fingerprint_version, fingerprint)。 */
  recordFingerprint(sourceInstanceId: string, fingerprintVersion: number, fingerprint: string): void {
    this.withTransaction(() => {
      const key = `${sourceInstanceId}\u0000${String(fingerprintVersion)}\u0000${fingerprint}`;
      if (this.store.observationFingerprints.has(key)) {
        return; // 幂等
      }
      const row = newRecord();
      row['fingerprint'] = fingerprint;
      row['source_instance_id'] = sourceInstanceId;
      row['source'] = '';
      row['fingerprint_version'] = fingerprintVersion;
      row['import_session_id'] = NULL_VALUE;
      row['first_seen_at'] = NOW;
      this.store.observationFingerprints.set(key, row);
    });
  }

  recordImportSession(id: string, rawCount: number, newUniqueCount: number,
    duplicateCount: number, proposalCount: number, errorCount: number): void {
    this.withTransaction(() => {
      const row = newRecord();
      row['id'] = id;
      row['source_type'] = 'statement_file';
      row['parser_id'] = 'wechat';
      row['parser_version'] = 1;
      row['source_instance_id'] = LEGACY_WECHAT_SOURCE_INSTANCE_ID;
      row['adapter_id'] = LEGACY_WECHAT_ADAPTER_ID;
      row['adapter_version'] = LEGACY_WECHAT_ADAPTER_VERSION;
      row['started_at'] = NOW;
      row['completed_at'] = NOW;
      row['raw_count'] = rawCount;
      row['new_unique_count'] = newUniqueCount;
      row['duplicate_count'] = duplicateCount;
      row['proposal_count'] = proposalCount;
      row['error_count'] = errorCount;
      this.store.importSessions.set(id, row);
    });
  }

  /** 条件插入 legacy 实例（重复迁移不重建 —— migration-spec §1.2）。 */
  ensureLegacySourceInstance(): void {
    if (this.store.sourceInstances.has(LEGACY_WECHAT_SOURCE_INSTANCE_ID)) {
      return;
    }
    const row = newRecord();
    row['id'] = LEGACY_WECHAT_SOURCE_INSTANCE_ID;
    row['adapter_id'] = LEGACY_WECHAT_ADAPTER_ID;
    row['adapter_version'] = LEGACY_WECHAT_ADAPTER_VERSION;
    row['source_kind'] = 'statement_file';
    row['provider_id'] = NULL_VALUE;
    row['account_node_id'] = NULL_VALUE;
    row['label'] = 'Legacy WeChat Statement Source';
    row['country'] = NULL_VALUE;
    row['jurisdiction'] = NULL_VALUE;
    row['currencies_json'] = '["CNY"]';
    row['state'] = 'active';
    row['created_at'] = NOW;
    row['updated_at'] = NOW;
    row['last_ingested_at'] = NULL_VALUE;
    this.store.sourceInstances.set(LEGACY_WECHAT_SOURCE_INSTANCE_ID, row);
  }

  // ---------------------------------------------------------------------------
  // Payload 导出 / 原子导入
  // ---------------------------------------------------------------------------

  exportPayload(): string {
    return buildPayloadJson(this.store.exportTables());
  }

  /** 原子导入：migrate → validate → 整体替换；任一失败 → 存储逐字节保持原样。 */
  importPayload(payloadJson: string): void {
    this.withTransaction(() => {
      const normalized = migrateIfNeeded(payloadJson);
      validatePayload(normalized);
      const rec = parsePayloadObject(normalized);
      this.replaceAllTables(rec);
      // 导入不 bump graph_revision（payload 往返保持 revision —— migration-spec §2.7）
    });
  }

  private replaceAllTables(rec: Record<string, Object>): void {
    this.store.nodes = rowsMapOf(rec, 'nodes', 'id');
    this.store.dependencies = rowsMapOf(rec, 'dependencies', 'id');
    this.store.dependencyGroups = rowsMapOf(rec, 'dependency_groups', 'id');
    this.store.dependencyProposals = rowsMapOf(rec, 'dependency_proposals', 'key');
    this.store.dependencyGroupProposals = rowsMapOf(rec, 'dependency_group_proposals', 'id');
    this.store.proposalEvidenceRefs = rowsArrayOf(rec, 'proposal_evidence_refs');
    this.store.evidence = evidenceMapOf(rec);
    this.store.observationFingerprints = fingerprintMapOf(rec);
    this.store.importSessions = rowsMapOf(rec, 'import_sessions', 'id');
    this.store.sourceInstances = rowsMapOf(rec, 'source_instances', 'id');
    this.store.meta = new Map();
    for (const row of rowsArrayOf(rec, 'meta')) {
      const k = String(row['key']);
      const v = String(row['value']);
      this.store.meta.set(k, v);
      if (k === META_KEY_SCHEMA_VERSION) {
        const n = Number(v);
        this.store.schemaVersion = Number.isFinite(n) ? n : this.store.schemaVersion;
      }
    }
  }

  // ---------------------------------------------------------------------------
  // 查询 / 计数（测试与自检用）
  // ---------------------------------------------------------------------------

  activeDependencyIds(): string[] {
    const out: string[] = [];
    for (const entry of this.store.dependencies) {
      const id = entry[0];
      const row = entry[1];
      if (String(row['state']) === 'active') {
        out.push(id);
      }
    }
    return out;
  }

  storeRef(): GraphStore {
    return this.store;
  }
}

function cloneRow(row: Record<string, Object>): Record<string, Object> {
  const out = newRecord();
  for (const k of ROW_COLUMNS) {
    const val = row[k];
    if (val !== undefined) {
      out[k] = val;
    }
  }
  return out;
}

function mergeEvidenceRefs(row: Record<string, Object>, incoming: string[]): string {
  const raw = String(row['evidence_refs_json']);
  const set = new Set<string>();
  try {
    const parsed = JSON.parse(raw) as Object;
    if (parsed instanceof Array) {
      for (const e of (parsed as Array<Object>)) {
        if (typeof e === 'string') {
          set.add(e as string);
        }
      }
    }
  } catch (e) {
    // 坏 JSON 视为空（与 TS parseJsonArray 一致）
  }
  for (const e of incoming) {
    set.add(e);
  }
  return JSON.stringify(Array.from(set));
}

function rowsMapOf(rec: Record<string, Object>, table: string, keyCol: string): Map<string, Record<string, Object>> {
  const out = new Map<string, Record<string, Object>>();
  for (const row of rowsArrayOf(rec, table)) {
    out.set(String(row[keyCol]), row);
  }
  return out;
}

function rowsArrayOf(rec: Record<string, Object>, table: string): Array<Record<string, Object>> {
  const value: Object = rec[table];
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

/** 行键集合 = SchemaV3 全部表列清单的并集（ArkTS 无 Object.keys，列清单驱动遍历）。 */
const ROW_COLUMNS: string[] = Array.from(new Set<string>(
  NODES_COLUMNS.concat(DEPENDENCIES_COLUMNS, DEPENDENCY_GROUPS_COLUMNS,
    DEPENDENCY_PROPOSALS_COLUMNS, PROPOSAL_EVIDENCE_REFS_COLUMNS,
    DEPENDENCY_GROUP_PROPOSALS_COLUMNS, EVIDENCE_COLUMNS,
    OBSERVATION_FINGERPRINTS_COLUMNS, IMPORT_SESSIONS_COLUMNS,
    SOURCE_INSTANCES_COLUMNS, META_COLUMNS)))

/** evidence 以 (proposal_key, source_instance_id) 为复合键（与 recordEvidence 一致）。 */
function evidenceMapOf(rec: Record<string, Object>): Map<string, Record<string, Object>> {
  const out = new Map<string, Record<string, Object>>();
  for (const row of rowsArrayOf(rec, 'evidence')) {
    out.set(`${String(row['proposal_key'])}\u0000${String(row['source_instance_id'])}`, row);
  }
  return out;
}

/** fingerprint 以 (source_instance_id, fingerprint_version, fingerprint) 为复合键。 */
function fingerprintMapOf(rec: Record<string, Object>): Map<string, Record<string, Object>> {
  const out = new Map<string, Record<string, Object>>();
  for (const row of rowsArrayOf(rec, 'observation_fingerprints')) {
    out.set(`${String(row['source_instance_id'])}\u0000${String(row['fingerprint_version'])}\u0000${String(row['fingerprint'])}`, row);
  }
  return out;
}