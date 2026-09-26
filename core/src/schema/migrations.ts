import type { SqliteDriver } from '../db/driver.ts'

/**
 * Schema v1 migration — CANONICAL_DESIGN §5 / GOAL_MVP01 §5-§6
 *
 * 要求：transaction / rollback / idempotent / 重启重复执行安全 /
 * 失败不得留下半迁移 DB。
 */

export const SCHEMA_VERSION = 3

/** Schema v4（Canonical vNext）：应用层显式升级到 v4 的目标版本；冻结的 legacy fixture 默认仍停在 v3。 */
export const LATEST_SCHEMA_VERSION = 4

/** 冻结的 payload schema 版本：.depmap 导出/恢复始终使用 payload v3（DEPMAP_CONTAINER_V1 不变）。 */
export const PAYLOAD_SCHEMA_VERSION = 3

/** deterministic legacy WeChat SourceInstance（重复 migration 不得创建第二个）。 */
export const LEGACY_WECHAT_SOURCE_INSTANCE_ID = 'legacy-wechat-statement'
export const LEGACY_WECHAT_ADAPTER_ID = 'wechat_statement'
export const LEGACY_WECHAT_ADAPTER_VERSION = 1

export interface Migration {
  version: number
  statements: string[]
}

export const SCHEMA_V1_STATEMENTS: string[] = [
  `CREATE TABLE IF NOT EXISTS meta (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
  )`,

  `CREATE TABLE IF NOT EXISTS nodes (
    id TEXT PRIMARY KEY,
    kind TEXT NOT NULL,
    template_id TEXT,
    name TEXT NOT NULL,
    issuer TEXT,
    last4 TEXT,
    owner TEXT NOT NULL DEFAULT 'self',
    archived INTEGER NOT NULL DEFAULT 0,
    fields_json TEXT NOT NULL DEFAULT '{}',
    vault_ref TEXT,
    wallet_ref TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
  )`,
  `CREATE INDEX IF NOT EXISTS idx_nodes_kind ON nodes(kind)`,

  `CREATE TABLE IF NOT EXISTS dependencies (
    id TEXT PRIMARY KEY,
    from_node TEXT NOT NULL,
    relation TEXT NOT NULL CHECK (relation IN ('funding_source','merchant_agreement','verifies','recovers','bound_to')),
    to_node TEXT NOT NULL,
    capability TEXT NOT NULL CHECK (capability IN ('payment','access','recovery','identity')),
    criticality TEXT NOT NULL DEFAULT 'unknown' CHECK (criticality IN ('required','unknown')),
    group_id TEXT,
    state TEXT NOT NULL DEFAULT 'active' CHECK (state IN ('active','retired')),
    origin TEXT NOT NULL CHECK (origin IN ('manual','proposal')),
    confirmed_at TEXT NOT NULL,
    last_verified_at TEXT NOT NULL,
    retired_at TEXT,
    evidence_refs_json TEXT NOT NULL DEFAULT '[]',
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    UNIQUE (from_node, relation, to_node, capability)
  )`,
  `CREATE INDEX IF NOT EXISTS idx_dep_to ON dependencies(to_node, capability, state)`,
  `CREATE INDEX IF NOT EXISTS idx_dep_from ON dependencies(from_node, capability, state)`,

  `CREATE TABLE IF NOT EXISTS dependency_groups (
    id TEXT PRIMARY KEY,
    group_key TEXT NOT NULL UNIQUE,
    target_node_id TEXT NOT NULL,
    capability TEXT NOT NULL CHECK (capability IN ('payment','access','recovery','identity')),
    mode TEXT NOT NULL CHECK (mode IN ('ANY','ALL')),
    member_edge_ids_json TEXT NOT NULL,
    state TEXT NOT NULL DEFAULT 'active' CHECK (state IN ('active','retired')),
    confirmed_at TEXT NOT NULL,
    last_verified_at TEXT NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
  )`,

  `CREATE TABLE IF NOT EXISTS dependency_proposals (
    id TEXT PRIMARY KEY,
    key TEXT NOT NULL UNIQUE,
    from_node TEXT NOT NULL,
    relation TEXT NOT NULL CHECK (relation IN ('funding_source','merchant_agreement','verifies','recovers','bound_to')),
    to_node TEXT NOT NULL,
    capability TEXT NOT NULL CHECK (capability IN ('payment','access','recovery','identity')),
    proposal_type TEXT NOT NULL,
    source TEXT NOT NULL,
    parser_id TEXT NOT NULL,
    parser_version INTEGER NOT NULL,
    confidence_score REAL NOT NULL,
    path_json TEXT NOT NULL DEFAULT '[]',
    evidence_id TEXT,
    decision TEXT NOT NULL DEFAULT 'pending' CHECK (decision IN ('pending','accepted','rejected')),
    decided_at TEXT,
    criticality_decision TEXT CHECK (criticality_decision IN ('required','unknown')),
    observation_count INTEGER NOT NULL DEFAULT 0,
    rejected_at TEXT,
    rejected_at_observation_count INTEGER,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
  )`,

  `CREATE TABLE IF NOT EXISTS dependency_group_proposals (
    id TEXT PRIMARY KEY,
    key TEXT NOT NULL UNIQUE,
    target_node_id TEXT NOT NULL,
    capability TEXT NOT NULL,
    mode TEXT NOT NULL CHECK (mode IN ('ANY','ALL')),
    member_dependency_keys_json TEXT NOT NULL,
    decision TEXT NOT NULL DEFAULT 'pending' CHECK (decision IN ('pending','accepted','rejected')),
    decided_at TEXT,
    rejected_at TEXT,
    rejected_at_observation_count INTEGER,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
  )`,

  `CREATE TABLE IF NOT EXISTS evidence (
    id TEXT PRIMARY KEY,
    proposal_key TEXT NOT NULL UNIQUE,
    source_type TEXT NOT NULL,
    parser_id TEXT NOT NULL,
    parser_version INTEGER NOT NULL,
    last_import_session_id TEXT NOT NULL,
    first_observed_at TEXT NOT NULL,
    last_observed_at TEXT NOT NULL,
    observation_count INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
  )`,

  `CREATE TABLE IF NOT EXISTS observation_fingerprints (
    fingerprint TEXT NOT NULL,
    source TEXT NOT NULL,
    fingerprint_version INTEGER NOT NULL DEFAULT 1,
    import_session_id TEXT NOT NULL,
    first_seen_at TEXT NOT NULL,
    UNIQUE (source, fingerprint)
  )`,

  `CREATE TABLE IF NOT EXISTS import_sessions (
    id TEXT PRIMARY KEY,
    source_type TEXT NOT NULL,
    parser_id TEXT NOT NULL,
    parser_version INTEGER NOT NULL,
    started_at TEXT NOT NULL,
    completed_at TEXT,
    raw_count INTEGER NOT NULL DEFAULT 0,
    new_unique_count INTEGER NOT NULL DEFAULT 0,
    duplicate_count INTEGER NOT NULL DEFAULT 0,
    proposal_count INTEGER NOT NULL DEFAULT 0,
    error_count INTEGER NOT NULL DEFAULT 0
  )`,
]

// ---------------------------------------------------------------------------
// Schema v2 statements（v1 → v2）：SourceInstance / fingerprint scope /
// evidence provenance / evidenceRefs join / verificationBasis / session provenance
// ---------------------------------------------------------------------------

export const SCHEMA_V2_STATEMENTS: string[] = [
  // 1. SourceInstance 一等实体
  `CREATE TABLE source_instances (
    id TEXT PRIMARY KEY,
    adapter_id TEXT NOT NULL,
    adapter_version INTEGER NOT NULL,
    source_kind TEXT NOT NULL CHECK (source_kind IN ('statement_file','platform_export','open_banking','manual','discovery')),
    provider_id TEXT,
    account_node_id TEXT,
    label TEXT NOT NULL,
    country TEXT,
    jurisdiction TEXT,
    currencies_json TEXT NOT NULL DEFAULT '[]',
    state TEXT NOT NULL DEFAULT 'active' CHECK (state IN ('active','retired')),
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    last_ingested_at TEXT
  )`,

  // 2. Fingerprint 重建：UNIQUE(source_instance_id, fingerprint_version, fingerprint)
  `CREATE TABLE observation_fingerprints_v2 (
    fingerprint TEXT NOT NULL,
    source_instance_id TEXT NOT NULL,
    source TEXT NOT NULL DEFAULT '',
    fingerprint_version INTEGER NOT NULL DEFAULT 1,
    import_session_id TEXT NOT NULL,
    first_seen_at TEXT NOT NULL,
    UNIQUE (source_instance_id, fingerprint_version, fingerprint)
  )`,
  `INSERT INTO observation_fingerprints_v2 (fingerprint, source_instance_id, source, fingerprint_version, import_session_id, first_seen_at)
   SELECT fingerprint, '${LEGACY_WECHAT_SOURCE_INSTANCE_ID}', source, fingerprint_version, import_session_id, first_seen_at FROM observation_fingerprints`,
  `DROP TABLE observation_fingerprints`,
  `ALTER TABLE observation_fingerprints_v2 RENAME TO observation_fingerprints`,

  // 3. Evidence 重建：按 (proposal_key, source_instance_id) 分流 + adapter provenance
  `CREATE TABLE evidence_v2 (
    id TEXT PRIMARY KEY,
    proposal_key TEXT NOT NULL,
    source_instance_id TEXT NOT NULL,
    adapter_id TEXT NOT NULL,
    adapter_version INTEGER NOT NULL,
    evidence_kind TEXT NOT NULL DEFAULT 'transaction_stream',
    source_type TEXT NOT NULL,
    parser_id TEXT NOT NULL,
    parser_version INTEGER NOT NULL,
    last_import_session_id TEXT NOT NULL,
    first_observed_at TEXT NOT NULL,
    last_observed_at TEXT NOT NULL,
    observation_count INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    UNIQUE (proposal_key, source_instance_id)
  )`,
  `INSERT INTO evidence_v2 (id, proposal_key, source_instance_id, adapter_id, adapter_version, evidence_kind, source_type, parser_id, parser_version, last_import_session_id, first_observed_at, last_observed_at, observation_count, created_at, updated_at)
   SELECT id, proposal_key, '${LEGACY_WECHAT_SOURCE_INSTANCE_ID}', '${LEGACY_WECHAT_ADAPTER_ID}', COALESCE(parser_version, 1), 'transaction_stream', source_type, parser_id, parser_version, last_import_session_id, first_observed_at, last_observed_at, observation_count, created_at, updated_at FROM evidence`,
  `DROP TABLE evidence`,
  `ALTER TABLE evidence_v2 RENAME TO evidence`,

  // 4. dependency_proposals 重建：evidence_id → join table；rejected 计数 → per-stream JSON
  `CREATE TABLE proposal_evidence_refs (
    proposal_key TEXT NOT NULL,
    evidence_id TEXT NOT NULL,
    position INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (proposal_key, evidence_id)
  )`,
  `CREATE TABLE dependency_proposals_v2 (
    id TEXT PRIMARY KEY,
    key TEXT NOT NULL UNIQUE,
    from_node TEXT NOT NULL,
    relation TEXT NOT NULL CHECK (relation IN ('funding_source','merchant_agreement','verifies','recovers','bound_to')),
    to_node TEXT NOT NULL,
    capability TEXT NOT NULL CHECK (capability IN ('payment','access','recovery','identity')),
    proposal_type TEXT NOT NULL,
    source TEXT NOT NULL,
    parser_id TEXT NOT NULL,
    parser_version INTEGER NOT NULL,
    confidence_score REAL NOT NULL,
    path_json TEXT NOT NULL DEFAULT '[]',
    decision TEXT NOT NULL DEFAULT 'pending' CHECK (decision IN ('pending','accepted','rejected')),
    decided_at TEXT,
    criticality_decision TEXT CHECK (criticality_decision IN ('required','unknown')),
    observation_count INTEGER NOT NULL DEFAULT 0,
    rejected_at TEXT,
    rejected_at_stream_counts_json TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
  )`,
  `INSERT INTO dependency_proposals_v2 (id, key, from_node, relation, to_node, capability, proposal_type, source, parser_id, parser_version, confidence_score, path_json, decision, decided_at, criticality_decision, observation_count, rejected_at, rejected_at_stream_counts_json, created_at, updated_at)
   SELECT id, key, from_node, relation, to_node, capability, proposal_type, source, parser_id, parser_version, confidence_score, path_json, decision, decided_at, criticality_decision, observation_count, rejected_at, NULL, created_at, updated_at FROM dependency_proposals`,
  `INSERT INTO proposal_evidence_refs (proposal_key, evidence_id, position)
   SELECT p.key, p.evidence_id, 0 FROM dependency_proposals p WHERE p.evidence_id IS NOT NULL`,
  `DROP TABLE dependency_proposals`,
  `ALTER TABLE dependency_proposals_v2 RENAME TO dependency_proposals`,

  // 5. verificationBasis（既有数据 → user_confirmed）
  `ALTER TABLE dependencies ADD COLUMN verification_basis_type TEXT NOT NULL DEFAULT 'user_confirmed'`,
  `ALTER TABLE dependencies ADD COLUMN verification_basis_json TEXT`,
  `ALTER TABLE dependency_groups ADD COLUMN verification_basis_type TEXT NOT NULL DEFAULT 'user_confirmed'`,
  `ALTER TABLE dependency_groups ADD COLUMN verification_basis_json TEXT`,

  // 6. ImportSession v2 provenance（legacy 行归属 legacy 实例）
  `ALTER TABLE import_sessions ADD COLUMN source_instance_id TEXT`,
  `ALTER TABLE import_sessions ADD COLUMN adapter_id TEXT`,
  `ALTER TABLE import_sessions ADD COLUMN adapter_version INTEGER`,
  `UPDATE import_sessions SET source_instance_id = '${LEGACY_WECHAT_SOURCE_INSTANCE_ID}', adapter_id = '${LEGACY_WECHAT_ADAPTER_ID}', adapter_version = 1 WHERE source_instance_id IS NULL`,

  // 7. deterministic legacy WeChat SourceInstance（id 固定 + 条件插入 → 重复 migration 不重建）
  `INSERT INTO source_instances (id, adapter_id, adapter_version, source_kind, label, currencies_json, state, created_at, updated_at)
   SELECT '${LEGACY_WECHAT_SOURCE_INSTANCE_ID}', '${LEGACY_WECHAT_ADAPTER_ID}', 1, 'statement_file', 'Legacy WeChat Statement Source', '["CNY"]', 'active', '1970-01-01T00:00:00.000Z', '1970-01-01T00:00:00.000Z'
   WHERE NOT EXISTS (SELECT 1 FROM source_instances WHERE id = '${LEGACY_WECHAT_SOURCE_INSTANCE_ID}')`,
]

// ---------------------------------------------------------------------------
// Schema v3 — MVP03 Living Graph & Change Safety
// 新增持久化实体：change_plans / reality_drifts / discovery_candidates。
// graphRevision 不建新表：存 meta.graph_revision（初始 0，见 repositories/graph-revision.ts）。
// DEPMAP_CONTAINER_V1（crypto 协议）不受应用 Schema 版本影响。
// ---------------------------------------------------------------------------

export const SCHEMA_V3_STATEMENTS: string[] = [
  `CREATE TABLE IF NOT EXISTS change_plans (
    id TEXT PRIMARY KEY,
    template_id TEXT,
    scenario TEXT NOT NULL,
    title TEXT NOT NULL,
    workflow_state TEXT NOT NULL CHECK (workflow_state IN ('draft','analyzed','review_required','ready','in_progress','verifying','completed','cancelled')),
    baseline_graph_revision INTEGER NOT NULL,
    last_analyzed_graph_revision INTEGER NOT NULL,
    target_node_id TEXT,
    effective_date TEXT,
    params_json TEXT NOT NULL DEFAULT '{}',
    impact_snapshot_json TEXT,
    action_items_json TEXT NOT NULL DEFAULT '[]',
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
  )`,
  `CREATE TABLE IF NOT EXISTS reality_drifts (
    id TEXT PRIMARY KEY,
    kind TEXT NOT NULL CHECK (kind IN ('possible_replacement','possible_additional_path','relation_reappeared')),
    target_node_id TEXT NOT NULL,
    capability TEXT NOT NULL CHECK (capability IN ('payment','access','recovery','identity')),
    candidate_from TEXT,
    candidate_relation TEXT NOT NULL DEFAULT 'funding_source',
    related_dependency_ids_json TEXT NOT NULL DEFAULT '[]',
    evidence_refs_json TEXT NOT NULL DEFAULT '[]',
    proposal_keys_json TEXT NOT NULL DEFAULT '[]',
    observation_count INTEGER NOT NULL DEFAULT 0,
    detected_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('open','confirmed_change','dismissed','superseded'))
  )`,
  `CREATE TABLE IF NOT EXISTS discovery_candidates (
    id TEXT PRIMARY KEY,
    candidate_kind TEXT NOT NULL,
    display_label TEXT NOT NULL,
    normalized_key TEXT NOT NULL,
    source_instance_id TEXT NOT NULL,
    evidence_refs_json TEXT NOT NULL DEFAULT '[]',
    observation_count INTEGER NOT NULL DEFAULT 0,
    first_seen_at TEXT NOT NULL,
    last_seen_at TEXT NOT NULL,
    dismissed_at_observation_count INTEGER,
    accepted_node_id TEXT,
    status TEXT NOT NULL CHECK (status IN ('pending','accepted','dismissed','superseded')),
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
  )`,
  `CREATE UNIQUE INDEX IF NOT EXISTS idx_discovery_candidates_normalized_key
   ON discovery_candidates (normalized_key)`,
  `CREATE INDEX IF NOT EXISTS idx_change_plans_state ON change_plans (workflow_state)`,
  `CREATE INDEX IF NOT EXISTS idx_reality_drifts_status ON reality_drifts (status)`,
]

// ---------------------------------------------------------------------------
// Schema v4 — Canonical vNext（v0.3.0）：Identity & Recovery / FailureDomain /
// ProviderPolicy / ChangePrimitive metadata / temporal verification fields。
// SQLite 不能 ALTER CHECK → 需要 widening 的表用「建临时表 → 拷贝 → 换名」重建；
// 每版本仍在独立事务内执行，失败回滚。.depmap payload schemaVersion 保持 3。
// ---------------------------------------------------------------------------

const CAPABILITY_CHECK_V4 =
  "capability IN ('payment','access','authentication','recovery','communication','identity')"
const RELATION_CHECK_V4 =
  "relation IN ('funding_source','merchant_agreement','verifies','recovers','bound_to','authenticates','controls')"

export const SCHEMA_V4_STATEMENTS: string[] = [
  // 1. dependencies：widening capability + relation CHECK（重建，保留数据与 UNIQUE）
  `CREATE TABLE dependencies_v4 (
    id TEXT PRIMARY KEY,
    from_node TEXT NOT NULL,
    relation TEXT NOT NULL CHECK (${RELATION_CHECK_V4}),
    to_node TEXT NOT NULL,
    capability TEXT NOT NULL CHECK (${CAPABILITY_CHECK_V4}),
    criticality TEXT NOT NULL DEFAULT 'unknown' CHECK (criticality IN ('required','unknown')),
    group_id TEXT,
    state TEXT NOT NULL DEFAULT 'active' CHECK (state IN ('active','retired')),
    origin TEXT NOT NULL CHECK (origin IN ('manual','proposal')),
    confirmed_at TEXT NOT NULL,
    last_verified_at TEXT NOT NULL,
    retired_at TEXT,
    evidence_refs_json TEXT NOT NULL DEFAULT '[]',
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    verification_basis_type TEXT NOT NULL DEFAULT 'user_confirmed',
    verification_basis_json TEXT,
    UNIQUE (from_node, relation, to_node, capability)
  )`,
  `INSERT INTO dependencies_v4 (id, from_node, relation, to_node, capability, criticality, group_id, state, origin, confirmed_at, last_verified_at, retired_at, evidence_refs_json, created_at, updated_at, verification_basis_type, verification_basis_json)
   SELECT id, from_node, relation, to_node, capability, criticality, group_id, state, origin, confirmed_at, last_verified_at, retired_at, evidence_refs_json, created_at, updated_at, verification_basis_type, verification_basis_json FROM dependencies`,
  `DROP TABLE dependencies`,
  `ALTER TABLE dependencies_v4 RENAME TO dependencies`,
  `CREATE INDEX IF NOT EXISTS idx_dep_to ON dependencies(to_node, capability, state)`,
  `CREATE INDEX IF NOT EXISTS idx_dep_from ON dependencies(from_node, capability, state)`,

  // 2. dependency_groups：widening capability CHECK
  `CREATE TABLE dependency_groups_v4 (
    id TEXT PRIMARY KEY,
    group_key TEXT NOT NULL UNIQUE,
    target_node_id TEXT NOT NULL,
    capability TEXT NOT NULL CHECK (${CAPABILITY_CHECK_V4}),
    mode TEXT NOT NULL CHECK (mode IN ('ANY','ALL')),
    member_edge_ids_json TEXT NOT NULL,
    state TEXT NOT NULL DEFAULT 'active' CHECK (state IN ('active','retired')),
    confirmed_at TEXT NOT NULL,
    last_verified_at TEXT NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    verification_basis_type TEXT NOT NULL DEFAULT 'user_confirmed',
    verification_basis_json TEXT
  )`,
  `INSERT INTO dependency_groups_v4 (id, group_key, target_node_id, capability, mode, member_edge_ids_json, state, confirmed_at, last_verified_at, created_at, updated_at, verification_basis_type, verification_basis_json)
   SELECT id, group_key, target_node_id, capability, mode, member_edge_ids_json, state, confirmed_at, last_verified_at, created_at, updated_at, verification_basis_type, verification_basis_json FROM dependency_groups`,
  `DROP TABLE dependency_groups`,
  `ALTER TABLE dependency_groups_v4 RENAME TO dependency_groups`,

  // 3. dependency_proposals：widening capability + relation CHECK（v2 重建遗留的 evidence_id 不再回来）
  `CREATE TABLE dependency_proposals_v4 (
    id TEXT PRIMARY KEY,
    key TEXT NOT NULL UNIQUE,
    from_node TEXT NOT NULL,
    relation TEXT NOT NULL CHECK (${RELATION_CHECK_V4}),
    to_node TEXT NOT NULL,
    capability TEXT NOT NULL CHECK (${CAPABILITY_CHECK_V4}),
    proposal_type TEXT NOT NULL,
    source TEXT NOT NULL,
    parser_id TEXT NOT NULL,
    parser_version INTEGER NOT NULL,
    confidence_score REAL NOT NULL,
    path_json TEXT NOT NULL DEFAULT '[]',
    decision TEXT NOT NULL DEFAULT 'pending' CHECK (decision IN ('pending','accepted','rejected')),
    decided_at TEXT,
    criticality_decision TEXT CHECK (criticality_decision IN ('required','unknown')),
    observation_count INTEGER NOT NULL DEFAULT 0,
    rejected_at TEXT,
    rejected_at_stream_counts_json TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
  )`,
  `INSERT INTO dependency_proposals_v4 (id, key, from_node, relation, to_node, capability, proposal_type, source, parser_id, parser_version, confidence_score, path_json, decision, decided_at, criticality_decision, observation_count, rejected_at, rejected_at_stream_counts_json, created_at, updated_at)
   SELECT id, key, from_node, relation, to_node, capability, proposal_type, source, parser_id, parser_version, confidence_score, path_json, decision, decided_at, criticality_decision, observation_count, rejected_at, rejected_at_stream_counts_json, created_at, updated_at FROM dependency_proposals`,
  `DROP TABLE dependency_proposals`,
  `ALTER TABLE dependency_proposals_v4 RENAME TO dependency_proposals`,

  // 4. reality_drifts：widening capability CHECK
  `CREATE TABLE reality_drifts_v4 (
    id TEXT PRIMARY KEY,
    kind TEXT NOT NULL CHECK (kind IN ('possible_replacement','possible_additional_path','relation_reappeared')),
    target_node_id TEXT NOT NULL,
    capability TEXT NOT NULL CHECK (${CAPABILITY_CHECK_V4}),
    candidate_from TEXT,
    candidate_relation TEXT NOT NULL DEFAULT 'funding_source',
    related_dependency_ids_json TEXT NOT NULL DEFAULT '[]',
    evidence_refs_json TEXT NOT NULL DEFAULT '[]',
    proposal_keys_json TEXT NOT NULL DEFAULT '[]',
    observation_count INTEGER NOT NULL DEFAULT 0,
    detected_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('open','confirmed_change','dismissed','superseded'))
  )`,
  `INSERT INTO reality_drifts_v4 (id, kind, target_node_id, capability, candidate_from, candidate_relation, related_dependency_ids_json, evidence_refs_json, proposal_keys_json, observation_count, detected_at, updated_at, status)
   SELECT id, kind, target_node_id, capability, candidate_from, candidate_relation, related_dependency_ids_json, evidence_refs_json, proposal_keys_json, observation_count, detected_at, updated_at, status FROM reality_drifts`,
  `DROP TABLE reality_drifts`,
  `ALTER TABLE reality_drifts_v4 RENAME TO reality_drifts`,
  `CREATE INDEX IF NOT EXISTS idx_reality_drifts_status ON reality_drifts (status)`,

  // 5. change_plans：Identity & Recovery / temporal / policy revision metadata
  `ALTER TABLE change_plans ADD COLUMN baseline_policy_revision INTEGER`,
  `ALTER TABLE change_plans ADD COLUMN last_analyzed_policy_revision INTEGER`,
  `ALTER TABLE change_plans ADD COLUMN change_primitive TEXT DEFAULT 'REPLACE'`,
  `ALTER TABLE change_plans ADD COLUMN temporal_effective_at TEXT`,
  `ALTER TABLE change_plans ADD COLUMN temporal_verification_not_before TEXT`,
  `ALTER TABLE change_plans ADD COLUMN temporal_verification_due_at TEXT`,
  `ALTER TABLE change_plans ADD COLUMN temporal_retire_old_path_after TEXT`,

  // 6. FailureDomain 持久化（v0.3.0）
  `CREATE TABLE IF NOT EXISTS failure_domains (
    id TEXT PRIMARY KEY,
    kind TEXT NOT NULL CHECK (kind IN ('DEVICE','PHONE_NUMBER','ACCOUNT','PROVIDER')),
    subject_ref TEXT NOT NULL,
    scope TEXT NOT NULL DEFAULT 'ALL_CAPABILITIES',
    evidence_refs_json TEXT NOT NULL DEFAULT '[]',
    status TEXT NOT NULL DEFAULT 'needs_review' CHECK (status IN ('confirmed','needs_review')),
    confirmed_at TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
  )`,

  // 7. ProviderPolicy 知识层（v0.3.0）
  `CREATE TABLE IF NOT EXISTS provider_policies (
    provider TEXT NOT NULL,
    policy_type TEXT NOT NULL,
    source_url TEXT NOT NULL,
    retrieved_at TEXT NOT NULL,
    last_verified_at TEXT,
    effective_from TEXT,
    effective_to TEXT,
    jurisdiction TEXT,
    account_type_scope TEXT,
    parameters_json TEXT NOT NULL DEFAULT '{}',
    policy_revision INTEGER NOT NULL,
    state TEXT NOT NULL DEFAULT 'needs_review' CHECK (state IN ('effective','superseded','needs_review')),
    PRIMARY KEY (provider, policy_type, policy_revision)
  )`,
]

export const MIGRATIONS: Migration[] = [
  { version: 1, statements: SCHEMA_V1_STATEMENTS },
  { version: 2, statements: SCHEMA_V2_STATEMENTS },
  { version: 3, statements: SCHEMA_V3_STATEMENTS },
  { version: 4, statements: SCHEMA_V4_STATEMENTS },
]

function ensureMetaTable(driver: SqliteDriver): void {
  driver.exec(`CREATE TABLE IF NOT EXISTS meta (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
  )`)
}

function getSchemaVersion(driver: SqliteDriver): number {
  ensureMetaTable(driver)
  const row = driver.prepare(`SELECT value FROM meta WHERE key = 'schema_version'`).get()
  if (!row) return 0
  return Number(row.value)
}

function setSchemaVersion(driver: SqliteDriver, version: number, nowIso: string): void {
  driver
    .prepare(
      `INSERT INTO meta (key, value) VALUES ('schema_version', ?)
       ON CONFLICT(key) DO UPDATE SET value = excluded.value`,
    )
    .run(String(version))
  driver
    .prepare(
      `INSERT INTO meta (key, value) VALUES ('schema_version_updated_at', ?)
       ON CONFLICT(key) DO UPDATE SET value = excluded.value`,
    )
    .run(nowIso)
}

/**
 * 幂等迁移：当前版本已是最新则 no-op；
 * 每个版本在独立事务内执行，失败回滚，不留下半迁移 DB。
 *
 * targetVersion 默认 = SCHEMA_VERSION（3，冻结的 legacy 行为，fixture 逐字节复现）。
 * 应用层升级到 v4 时显式传入 LATEST_SCHEMA_VERSION。
 */
export function migrate(
  driver: SqliteDriver,
  nowIso: string = new Date().toISOString(),
  targetVersion: number = SCHEMA_VERSION,
): number {
  if (!Number.isSafeInteger(targetVersion) || targetVersion < 1) {
    throw new Error(`invalid migration target version: ${String(targetVersion)}`)
  }
  if (targetVersion > LATEST_SCHEMA_VERSION) {
    throw new Error(
      `migration target (${targetVersion}) is newer than supported (${LATEST_SCHEMA_VERSION})`,
    )
  }
  ensureMetaTable(driver)
  let current = getSchemaVersion(driver)
  if (current > targetVersion) {
    throw new Error(`database schema_version (${current}) is newer than target (${targetVersion})`)
  }
  for (const migration of MIGRATIONS) {
    if (migration.version <= current) continue
    if (migration.version > targetVersion) break
    driver.transaction(() => {
      for (const sql of migration.statements) {
        driver.exec(sql)
      }
      setSchemaVersion(driver, migration.version, nowIso)
    })
    current = migration.version
  }
  return current
}

export function currentSchemaVersion(driver: SqliteDriver): number {
  return getSchemaVersion(driver)
}
