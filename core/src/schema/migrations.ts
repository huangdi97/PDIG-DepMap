import type { SqliteDriver } from '../db/driver.ts'

/**
 * Schema v1 migration — CANONICAL_DESIGN §5 / GOAL_MVP01 §5-§6
 *
 * 要求：transaction / rollback / idempotent / 重启重复执行安全 /
 * 失败不得留下半迁移 DB。
 */

export const SCHEMA_VERSION = 1

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
    relation TEXT NOT NULL,
    to_node TEXT NOT NULL,
    capability TEXT NOT NULL,
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
    capability TEXT NOT NULL,
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
    relation TEXT NOT NULL,
    to_node TEXT NOT NULL,
    capability TEXT NOT NULL,
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

export const MIGRATIONS: Migration[] = [{ version: 1, statements: SCHEMA_V1_STATEMENTS }]

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
       ON CONFLICT(key) DO UPDATE SET value = excluded.value`
    )
    .run(String(version))
  driver
    .prepare(
      `INSERT INTO meta (key, value) VALUES ('schema_version_updated_at', ?)
       ON CONFLICT(key) DO UPDATE SET value = excluded.value`
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
      `database schema_version (${current}) is newer than supported (${SCHEMA_VERSION})`
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
