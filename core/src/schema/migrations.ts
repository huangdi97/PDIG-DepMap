import type { SqliteDriver } from '../db/driver.ts'

/**
 * Schema v1 migration — CANONICAL_DESIGN §5 / GOAL_MVP01 §5-§6
 *
 * 要求：transaction / rollback / idempotent / 重启重复执行安全 /
 * 失败不得留下半迁移 DB。
 */

export const SCHEMA_VERSION = 2

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

export const MIGRATIONS: Migration[] = [
  { version: 1, statements: SCHEMA_V1_STATEMENTS },
  { version: 2, statements: SCHEMA_V2_STATEMENTS },
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
 */
export function migrate(driver: SqliteDriver, nowIso: string = new Date().toISOString()): number {
  ensureMetaTable(driver)
  let current = getSchemaVersion(driver)
  if (current > SCHEMA_VERSION) {
    throw new Error(
      `database schema_version (${current}) is newer than supported (${SCHEMA_VERSION})`,
    )
  }
  for (const migration of MIGRATIONS) {
    if (migration.version <= current) continue
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
