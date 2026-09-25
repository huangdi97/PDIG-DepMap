// Schema / payload 契约常量 —— 平台侧**独立副本**（纯 ArkTS，零 @ohos 依赖）。
//
// 为什么必须在平台侧再放一份（而不是只让 conformance 用例持有）：
//   `migration-version-contract` canonical 用例刻意**独立声明**这份契约
//   （见 ConformanceRunner.ets：若用例与平台模块从同一常量派生，就退化成
//   “自己和比自己”，失去证据力）。因此平台模块必须持有自己的、与冻结契约
//   **逐字一致**的副本；两边的一致性由主机测试 `PersistenceHost.test.ets`
//   （schema-contract 用例）独立断言。
//
// 真相源（Single Source of Truth）：
//   spec/migration/migration-spec.md · spec/schema/persistence-contract.md
//   spec/schema/logical-schema.json · fixtures/migration/migration-version-contract.json
// 移植自 core/src/services/graph-serialize.ts 与 core/src/schema/migrations.ts
// 的**冻结基准**（LEGACY_REFERENCE_MANIFEST.md / tag v0.3.0-uniapp-reference）。
//
// 分层约束：本模块**只**输出常量与纯函数，不依赖 domain / conformance / 平台 API。

/** 当前逻辑 Schema 版本（DB schema 与 payload schemaVersion 共用）。 */
export const SCHEMA_VERSION: number = 3;
/** Schema 迁移链（顺序固定）。 */
export const MIGRATION_CHAIN: number[] = [1, 2, 3];
/** `.depmap` payload 层类型标识（与 DEPMAP_CONTAINER_V1 的 formatVersion 无关）。 */
export const GRAPH_PAYLOAD_KIND: string = 'depmap-logical-graph';
/** 当前 payload 版本。 */
export const GRAPH_PAYLOAD_VERSION: number = 3;
/** schemaVersion 高于支持版本时必须 fail-closed 拒绝的取值集合（冻结契约）。 */
export const REJECTED_SCHEMA_VERSIONS: number[] = [0, 4, 99, 100];
/** payloadVersion 必须拒绝的取值集合（冻结契约）。 */
export const REJECTED_PAYLOAD_VERSIONS: number[] = [0, 4, 99];

/** legacy WeChat SourceInstance 的固定 id（v1 降级归属目标）。 */
export const LEGACY_WECHAT_SOURCE_INSTANCE_ID: string = 'legacy-wechat-statement';
export const LEGACY_WECHAT_ADAPTER_ID: string = 'wechat_statement';
export const LEGACY_WECHAT_ADAPTER_VERSION: number = 1;

/** payload 表集合（顺序 = 冻结契约，导出时按此顺序输出）。 */
export const PAYLOAD_TABLES: string[] = [
  'meta',
  'nodes',
  'dependencies',
  'dependency_groups',
  'dependency_proposals',
  'dependency_group_proposals',
  'proposal_evidence_refs',
  'evidence',
  'observation_fingerprints',
  'import_sessions',
  'source_instances',
];

// ---------------------------------------------------------------------------
// 各表列清单（导出 / 校验 / 迁移共用；与 graph-serialize.ts PAYLOAD_TABLES 逐字一致）
// ---------------------------------------------------------------------------

export const META_COLUMNS: string[] = ['key', 'value'];
export const SOURCE_INSTANCES_COLUMNS: string[] = [
  'id', 'adapter_id', 'adapter_version', 'source_kind', 'provider_id',
  'account_node_id', 'label', 'country', 'jurisdiction', 'currencies_json',
  'state', 'created_at', 'updated_at', 'last_ingested_at',
];
export const NODES_COLUMNS: string[] = [
  'id', 'kind', 'template_id', 'name', 'issuer', 'last4', 'owner', 'archived',
  'fields_json', 'vault_ref', 'wallet_ref', 'created_at', 'updated_at',
];
export const DEPENDENCIES_COLUMNS: string[] = [
  'id', 'from_node', 'relation', 'to_node', 'capability', 'criticality',
  'group_id', 'state', 'origin', 'confirmed_at', 'last_verified_at',
  'retired_at', 'evidence_refs_json', 'verification_basis_type',
  'verification_basis_json', 'created_at', 'updated_at',
];
export const DEPENDENCY_GROUPS_COLUMNS: string[] = [
  'id', 'group_key', 'target_node_id', 'capability', 'mode',
  'member_edge_ids_json', 'state', 'confirmed_at', 'last_verified_at',
  'verification_basis_type', 'verification_basis_json', 'created_at', 'updated_at',
];
export const DEPENDENCY_PROPOSALS_COLUMNS: string[] = [
  'id', 'key', 'from_node', 'relation', 'to_node', 'capability', 'proposal_type',
  'source', 'parser_id', 'parser_version', 'confidence_score', 'path_json',
  'decision', 'decided_at', 'criticality_decision', 'observation_count',
  'rejected_at', 'rejected_at_stream_counts_json', 'created_at', 'updated_at',
];
export const PROPOSAL_EVIDENCE_REFS_COLUMNS: string[] = [
  'proposal_key', 'evidence_id', 'position',
];
export const DEPENDENCY_GROUP_PROPOSALS_COLUMNS: string[] = [
  'id', 'key', 'target_node_id', 'capability', 'mode',
  'member_dependency_keys_json', 'decision', 'decided_at', 'rejected_at',
  'rejected_at_observation_count', 'created_at', 'updated_at',
];
export const EVIDENCE_COLUMNS: string[] = [
  'id', 'proposal_key', 'source_instance_id', 'adapter_id', 'adapter_version',
  'evidence_kind', 'source_type', 'parser_id', 'parser_version',
  'last_import_session_id', 'first_observed_at', 'last_observed_at',
  'observation_count', 'created_at', 'updated_at',
];
export const OBSERVATION_FINGERPRINTS_COLUMNS: string[] = [
  'fingerprint', 'source_instance_id', 'source', 'fingerprint_version',
  'import_session_id', 'first_seen_at',
];
export const IMPORT_SESSIONS_COLUMNS: string[] = [
  'id', 'source_type', 'parser_id', 'parser_version', 'source_instance_id',
  'adapter_id', 'adapter_version', 'started_at', 'completed_at', 'raw_count',
  'new_unique_count', 'duplicate_count', 'proposal_count', 'error_count',
];

/** 永不 bump graph_revision 的 mutation 清单（冻结契约，见 migration-version-contract）。 */
export const NEVER_BUMPS_ON: string[] = [
  'evidence_record',
  'proposal_upsert',
  'proposal_decision',
  'candidate_upsert',
  'candidate_dismiss',
  'candidate_accept',
  'drift_detect',
  'drift_dismiss',
  'timeline_build',
  'import_session_record',
  'plan_create',
  'plan_rebase',
  'plan_transition',
  'action_complete',
  'action_verify',
];

/**
 * 取某 payload 表的列清单。
 * 未知表名返回空数组（fail closed：任何调用方把未知表当“无列”处理，不猜测）。
 */
export function getColumns(table: string): string[] {
  switch (table) {
    case 'meta':
      return META_COLUMNS;
    case 'source_instances':
      return SOURCE_INSTANCES_COLUMNS;
    case 'nodes':
      return NODES_COLUMNS;
    case 'dependencies':
      return DEPENDENCIES_COLUMNS;
    case 'dependency_groups':
      return DEPENDENCY_GROUPS_COLUMNS;
    case 'dependency_proposals':
      return DEPENDENCY_PROPOSALS_COLUMNS;
    case 'proposal_evidence_refs':
      return PROPOSAL_EVIDENCE_REFS_COLUMNS;
    case 'dependency_group_proposals':
      return DEPENDENCY_GROUP_PROPOSALS_COLUMNS;
    case 'evidence':
      return EVIDENCE_COLUMNS;
    case 'observation_fingerprints':
      return OBSERVATION_FINGERPRINTS_COLUMNS;
    case 'import_sessions':
      return IMPORT_SESSIONS_COLUMNS;
    default:
      return [];
  }
}