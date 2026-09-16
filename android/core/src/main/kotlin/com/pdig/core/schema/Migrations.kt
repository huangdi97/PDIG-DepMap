package com.pdig.core.schema

import com.pdig.core.db.SqliteDriver

/**
 * PDIG 逻辑 Schema 迁移（v1 → v2 → v3）。
 *
 * 逐条移植自 core/src/schema/migrations.ts。**三端迁移行为必须一致**：
 *  - transactional：每个版本在独立事务内执行
 *  - failure rollback：失败回滚，绝不留下半迁移 DB
 *  - idempotent：重复执行严格 no-op
 *  - IDs preserved / Proposal decisions preserved / Evidence preserved /
 *    Group preserved / SourceInstance preserved
 *  - 未来 schema version 明确 reject，绝不猜测兼容
 */

const val SCHEMA_VERSION = 3

/** deterministic legacy WeChat SourceInstance（重复 migration 不得创建第二个）。 */
const val LEGACY_WECHAT_SOURCE_INSTANCE_ID = "legacy-wechat-statement"
const val LEGACY_WECHAT_ADAPTER_ID = "wechat_statement"
const val LEGACY_WECHAT_ADAPTER_VERSION = 1

data class Migration(val version: Int, val statements: List<String>)

val SCHEMA_V1_STATEMENTS: List<String> = listOf(
    """
    CREATE TABLE IF NOT EXISTS meta (
      key TEXT PRIMARY KEY,
      value TEXT NOT NULL
    )
    """.trimIndent(),

    """
    CREATE TABLE IF NOT EXISTS nodes (
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
    )
    """.trimIndent(),
    "CREATE INDEX IF NOT EXISTS idx_nodes_kind ON nodes(kind)",

    """
    CREATE TABLE IF NOT EXISTS dependencies (
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
    )
    """.trimIndent(),
    "CREATE INDEX IF NOT EXISTS idx_dep_to ON dependencies(to_node, capability, state)",
    "CREATE INDEX IF NOT EXISTS idx_dep_from ON dependencies(from_node, capability, state)",

    """
    CREATE TABLE IF NOT EXISTS dependency_groups (
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
    )
    """.trimIndent(),

    """
    CREATE TABLE IF NOT EXISTS dependency_proposals (
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
    )
    """.trimIndent(),

    """
    CREATE TABLE IF NOT EXISTS dependency_group_proposals (
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
    )
    """.trimIndent(),

    """
    CREATE TABLE IF NOT EXISTS evidence (
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
    )
    """.trimIndent(),

    """
    CREATE TABLE IF NOT EXISTS observation_fingerprints (
      fingerprint TEXT NOT NULL,
      source TEXT NOT NULL,
      fingerprint_version INTEGER NOT NULL DEFAULT 1,
      import_session_id TEXT NOT NULL,
      first_seen_at TEXT NOT NULL,
      UNIQUE (source, fingerprint)
    )
    """.trimIndent(),

    """
    CREATE TABLE IF NOT EXISTS import_sessions (
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
    )
    """.trimIndent(),
)

/** v1 → v2：SourceInstance / fingerprint scope / evidence provenance / verificationBasis。 */
val SCHEMA_V2_STATEMENTS: List<String> = listOf(
    // 1. SourceInstance 一等实体
    """
    CREATE TABLE source_instances (
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
    )
    """.trimIndent(),

    // 2. Fingerprint 重建：UNIQUE(source_instance_id, fingerprint_version, fingerprint)
    """
    CREATE TABLE observation_fingerprints_v2 (
      fingerprint TEXT NOT NULL,
      source_instance_id TEXT NOT NULL,
      source TEXT NOT NULL DEFAULT '',
      fingerprint_version INTEGER NOT NULL DEFAULT 1,
      import_session_id TEXT NOT NULL,
      first_seen_at TEXT NOT NULL,
      UNIQUE (source_instance_id, fingerprint_version, fingerprint)
    )
    """.trimIndent(),
    """
    INSERT INTO observation_fingerprints_v2 (fingerprint, source_instance_id, source, fingerprint_version, import_session_id, first_seen_at)
     SELECT fingerprint, '$LEGACY_WECHAT_SOURCE_INSTANCE_ID', source, fingerprint_version, import_session_id, first_seen_at FROM observation_fingerprints
    """.trimIndent(),
    "DROP TABLE observation_fingerprints",
    "ALTER TABLE observation_fingerprints_v2 RENAME TO observation_fingerprints",

    // 3. Evidence 重建：按 (proposal_key, source_instance_id) 分流 + adapter provenance
    """
    CREATE TABLE evidence_v2 (
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
    )
    """.trimIndent(),
    """
    INSERT INTO evidence_v2 (id, proposal_key, source_instance_id, adapter_id, adapter_version, evidence_kind, source_type, parser_id, parser_version, last_import_session_id, first_observed_at, last_observed_at, observation_count, created_at, updated_at)
     SELECT id, proposal_key, '$LEGACY_WECHAT_SOURCE_INSTANCE_ID', '$LEGACY_WECHAT_ADAPTER_ID', COALESCE(parser_version, 1), 'transaction_stream', source_type, parser_id, parser_version, last_import_session_id, first_observed_at, last_observed_at, observation_count, created_at, updated_at FROM evidence
    """.trimIndent(),
    "DROP TABLE evidence",
    "ALTER TABLE evidence_v2 RENAME TO evidence",

    // 4. dependency_proposals 重建：evidence_id → join table
    """
    CREATE TABLE proposal_evidence_refs (
      proposal_key TEXT NOT NULL,
      evidence_id TEXT NOT NULL,
      position INTEGER NOT NULL DEFAULT 0,
      PRIMARY KEY (proposal_key, evidence_id)
    )
    """.trimIndent(),
    """
    CREATE TABLE dependency_proposals_v2 (
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
    )
    """.trimIndent(),
    """
    INSERT INTO dependency_proposals_v2 (id, key, from_node, relation, to_node, capability, proposal_type, source, parser_id, parser_version, confidence_score, path_json, decision, decided_at, criticality_decision, observation_count, rejected_at, rejected_at_stream_counts_json, created_at, updated_at)
     SELECT id, key, from_node, relation, to_node, capability, proposal_type, source, parser_id, parser_version, confidence_score, path_json, decision, decided_at, criticality_decision, observation_count, rejected_at, NULL, created_at, updated_at FROM dependency_proposals
    """.trimIndent(),
    """
    INSERT INTO proposal_evidence_refs (proposal_key, evidence_id, position)
     SELECT p.key, p.evidence_id, 0 FROM dependency_proposals p WHERE p.evidence_id IS NOT NULL
    """.trimIndent(),
    "DROP TABLE dependency_proposals",
    "ALTER TABLE dependency_proposals_v2 RENAME TO dependency_proposals",

    // 5. verificationBasis（既有数据 → user_confirmed）
    "ALTER TABLE dependencies ADD COLUMN verification_basis_type TEXT NOT NULL DEFAULT 'user_confirmed'",
    "ALTER TABLE dependencies ADD COLUMN verification_basis_json TEXT",
    "ALTER TABLE dependency_groups ADD COLUMN verification_basis_type TEXT NOT NULL DEFAULT 'user_confirmed'",
    "ALTER TABLE dependency_groups ADD COLUMN verification_basis_json TEXT",

    // 6. ImportSession v2 provenance（legacy 行归属 legacy 实例）
    "ALTER TABLE import_sessions ADD COLUMN source_instance_id TEXT",
    "ALTER TABLE import_sessions ADD COLUMN adapter_id TEXT",
    "ALTER TABLE import_sessions ADD COLUMN adapter_version INTEGER",
    "UPDATE import_sessions SET source_instance_id = '$LEGACY_WECHAT_SOURCE_INSTANCE_ID', adapter_id = '$LEGACY_WECHAT_ADAPTER_ID', adapter_version = 1 WHERE source_instance_id IS NULL",

    // 7. deterministic legacy WeChat SourceInstance（条件插入 → 重复 migration 不重建）
    """
    INSERT INTO source_instances (id, adapter_id, adapter_version, source_kind, label, currencies_json, state, created_at, updated_at)
     SELECT '$LEGACY_WECHAT_SOURCE_INSTANCE_ID', '$LEGACY_WECHAT_ADAPTER_ID', 1, 'statement_file', 'Legacy WeChat Statement Source', '["CNY"]', 'active', '1970-01-01T00:00:00.000Z', '1970-01-01T00:00:00.000Z'
     WHERE NOT EXISTS (SELECT 1 FROM source_instances WHERE id = '$LEGACY_WECHAT_SOURCE_INSTANCE_ID')
    """.trimIndent(),
)

/** v3 — MVP03 Living Graph：change_plans / reality_drifts / discovery_candidates。 */
val SCHEMA_V3_STATEMENTS: List<String> = listOf(
    """
    CREATE TABLE IF NOT EXISTS change_plans (
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
    )
    """.trimIndent(),
    """
    CREATE TABLE IF NOT EXISTS reality_drifts (
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
    )
    """.trimIndent(),
    """
    CREATE TABLE IF NOT EXISTS discovery_candidates (
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
    )
    """.trimIndent(),
    """
    CREATE UNIQUE INDEX IF NOT EXISTS idx_discovery_candidates_normalized_key
     ON discovery_candidates (normalized_key)
    """.trimIndent(),
    "CREATE INDEX IF NOT EXISTS idx_change_plans_state ON change_plans (workflow_state)",
    "CREATE INDEX IF NOT EXISTS idx_reality_drifts_status ON reality_drifts (status)",
)

val MIGRATIONS: List<Migration> = listOf(
    Migration(1, SCHEMA_V1_STATEMENTS),
    Migration(2, SCHEMA_V2_STATEMENTS),
    Migration(3, SCHEMA_V3_STATEMENTS),
)

internal fun ensureMetaTable(driver: SqliteDriver) {
    driver.exec(
        """
        CREATE TABLE IF NOT EXISTS meta (
          key TEXT PRIMARY KEY,
          value TEXT NOT NULL
        )
        """.trimIndent(),
    )
}

fun getSchemaVersion(driver: SqliteDriver): Int {
    ensureMetaTable(driver)
    val row = driver.prepare("SELECT value FROM meta WHERE key = 'schema_version'").get() ?: return 0
    return row.str("value")?.toIntOrNull() ?: 0
}

private fun setSchemaVersion(driver: SqliteDriver, version: Int, nowIso: String) {
    driver.prepare(
        """
        INSERT INTO meta (key, value) VALUES ('schema_version', ?)
        ON CONFLICT(key) DO UPDATE SET value = excluded.value
        """.trimIndent(),
    ).run(version.toString())
    driver.prepare(
        """
        INSERT INTO meta (key, value) VALUES ('schema_version_updated_at', ?)
        ON CONFLICT(key) DO UPDATE SET value = excluded.value
        """.trimIndent(),
    ).run(nowIso)
}

/**
 * 幂等迁移：当前版本已是最新则 no-op；
 * 每个版本在独立事务内执行，失败回滚，不留下半迁移 DB。
 */
fun migrate(driver: SqliteDriver, nowIso: String): Int {
    ensureMetaTable(driver)
    var current = getSchemaVersion(driver)
    if (current > SCHEMA_VERSION) {
        throw IllegalStateException(
            "database schema_version ($current) is newer than supported ($SCHEMA_VERSION)",
        )
    }
    for (m in MIGRATIONS) {
        if (m.version <= current) continue
        driver.transaction {
            for (sql in m.statements) driver.exec(sql)
            setSchemaVersion(driver, m.version, nowIso)
        }
        current = m.version
    }
    return current
}
