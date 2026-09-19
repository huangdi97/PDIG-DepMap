// PDIG 逻辑 Schema 迁移（v1 → v2 → v3）—— **由生成器产出，不要手改**。
//
// 生成源：android/core/src/main/kotlin/com/pdig/core/schema/Migrations.kt
// 生成器：tools/ios/gen-migrations-swift.mjs
//
// 三端迁移行为必须一致：
//  - transactional：每个版本在独立事务内执行
//  - failure rollback：失败回滚，绝不留下半迁移 DB
//  - idempotent：重复执行严格 no-op
//  - IDs preserved / Proposal decisions preserved / Evidence preserved /
//    Group preserved / SourceInstance preserved
//  - 未来 schema version 明确 reject，绝不猜测兼容
//
// 逻辑 Schema 必须跨端一致；物理 DDL 可以不同
// （Android/iOS = SQLCipher，HarmonyOS = ArkData relationalStore）。
// 本文件的 DDL 逐字来自 Android 冻结源，SQLCipher 与 SQLite DDL 兼容。

import Foundation

public enum Migrations {

    public static let schemaVersion = 3

    /// deterministic legacy WeChat SourceInstance（重复 migration 不得创建第二个）。
    public static let legacyWechatSourceInstanceId = "legacy-wechat-statement"
    public static let legacyWechatAdapterId = "wechat_statement"
    public static let legacyWechatAdapterVersion = 1

    public struct Migration: Equatable, Sendable {
        public let version: Int
        public let statements: [String]
        public init(version: Int, statements: [String]) {
            self.version = version
            self.statements = statements
        }
    }

    public static let schemaV1Statements: [String] = [
        #"""
        CREATE TABLE IF NOT EXISTS meta (
          key TEXT PRIMARY KEY,
          value TEXT NOT NULL
        )
        """#,
        #"""
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
        """#,
        #"CREATE INDEX IF NOT EXISTS idx_nodes_kind ON nodes(kind)"#,
        #"""
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
        """#,
        #"CREATE INDEX IF NOT EXISTS idx_dep_to ON dependencies(to_node, capability, state)"#,
        #"CREATE INDEX IF NOT EXISTS idx_dep_from ON dependencies(from_node, capability, state)"#,
        #"""
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
        """#,
        #"""
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
        """#,
        #"""
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
        """#,
        #"""
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
        """#,
        #"""
        CREATE TABLE IF NOT EXISTS observation_fingerprints (
          fingerprint TEXT NOT NULL,
          source TEXT NOT NULL,
          fingerprint_version INTEGER NOT NULL DEFAULT 1,
          import_session_id TEXT NOT NULL,
          first_seen_at TEXT NOT NULL,
          UNIQUE (source, fingerprint)
        )
        """#,
        #"""
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
        """#,
    ]

    public static let schemaV2Statements: [String] = [
        #"""
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
        """#,
        #"""
        CREATE TABLE observation_fingerprints_v2 (
          fingerprint TEXT NOT NULL,
          source_instance_id TEXT NOT NULL,
          source TEXT NOT NULL DEFAULT '',
          fingerprint_version INTEGER NOT NULL DEFAULT 1,
          import_session_id TEXT NOT NULL,
          first_seen_at TEXT NOT NULL,
          UNIQUE (source_instance_id, fingerprint_version, fingerprint)
        )
        """#,
        #"""
        INSERT INTO observation_fingerprints_v2 (fingerprint, source_instance_id, source, fingerprint_version, import_session_id, first_seen_at)
         SELECT fingerprint, 'legacy-wechat-statement', source, fingerprint_version, import_session_id, first_seen_at FROM observation_fingerprints
        """#,
        #"DROP TABLE observation_fingerprints"#,
        #"ALTER TABLE observation_fingerprints_v2 RENAME TO observation_fingerprints"#,
        #"""
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
        """#,
        #"""
        INSERT INTO evidence_v2 (id, proposal_key, source_instance_id, adapter_id, adapter_version, evidence_kind, source_type, parser_id, parser_version, last_import_session_id, first_observed_at, last_observed_at, observation_count, created_at, updated_at)
         SELECT id, proposal_key, 'legacy-wechat-statement', 'wechat_statement', COALESCE(parser_version, 1), 'transaction_stream', source_type, parser_id, parser_version, last_import_session_id, first_observed_at, last_observed_at, observation_count, created_at, updated_at FROM evidence
        """#,
        #"DROP TABLE evidence"#,
        #"ALTER TABLE evidence_v2 RENAME TO evidence"#,
        #"""
        CREATE TABLE proposal_evidence_refs (
          proposal_key TEXT NOT NULL,
          evidence_id TEXT NOT NULL,
          position INTEGER NOT NULL DEFAULT 0,
          PRIMARY KEY (proposal_key, evidence_id)
        )
        """#,
        #"""
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
        """#,
        #"""
        INSERT INTO dependency_proposals_v2 (id, key, from_node, relation, to_node, capability, proposal_type, source, parser_id, parser_version, confidence_score, path_json, decision, decided_at, criticality_decision, observation_count, rejected_at, rejected_at_stream_counts_json, created_at, updated_at)
         SELECT id, key, from_node, relation, to_node, capability, proposal_type, source, parser_id, parser_version, confidence_score, path_json, decision, decided_at, criticality_decision, observation_count, rejected_at, NULL, created_at, updated_at FROM dependency_proposals
        """#,
        #"""
        INSERT INTO proposal_evidence_refs (proposal_key, evidence_id, position)
         SELECT p.key, p.evidence_id, 0 FROM dependency_proposals p WHERE p.evidence_id IS NOT NULL
        """#,
        #"DROP TABLE dependency_proposals"#,
        #"ALTER TABLE dependency_proposals_v2 RENAME TO dependency_proposals"#,
        #"ALTER TABLE dependencies ADD COLUMN verification_basis_type TEXT NOT NULL DEFAULT 'user_confirmed'"#,
        #"ALTER TABLE dependencies ADD COLUMN verification_basis_json TEXT"#,
        #"ALTER TABLE dependency_groups ADD COLUMN verification_basis_type TEXT NOT NULL DEFAULT 'user_confirmed'"#,
        #"ALTER TABLE dependency_groups ADD COLUMN verification_basis_json TEXT"#,
        #"ALTER TABLE import_sessions ADD COLUMN source_instance_id TEXT"#,
        #"ALTER TABLE import_sessions ADD COLUMN adapter_id TEXT"#,
        #"ALTER TABLE import_sessions ADD COLUMN adapter_version INTEGER"#,
        #"UPDATE import_sessions SET source_instance_id = 'legacy-wechat-statement', adapter_id = 'wechat_statement', adapter_version = 1 WHERE source_instance_id IS NULL"#,
        #"""
        INSERT INTO source_instances (id, adapter_id, adapter_version, source_kind, label, currencies_json, state, created_at, updated_at)
         SELECT 'legacy-wechat-statement', 'wechat_statement', 1, 'statement_file', 'Legacy WeChat Statement Source', '["CNY"]', 'active', '1970-01-01T00:00:00.000Z', '1970-01-01T00:00:00.000Z'
         WHERE NOT EXISTS (SELECT 1 FROM source_instances WHERE id = 'legacy-wechat-statement')
        """#,
    ]

    public static let schemaV3Statements: [String] = [
        #"""
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
        """#,
        #"""
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
        """#,
        #"""
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
        """#,
        #"""
        CREATE UNIQUE INDEX IF NOT EXISTS idx_discovery_candidates_normalized_key
         ON discovery_candidates (normalized_key)
        """#,
        #"CREATE INDEX IF NOT EXISTS idx_change_plans_state ON change_plans (workflow_state)"#,
        #"CREATE INDEX IF NOT EXISTS idx_reality_drifts_status ON reality_drifts (status)"#,
    ]

    public static let migrations: [Migration] = [
        Migration(version: 1, statements: schemaV1Statements),
        Migration(version: 2, statements: schemaV2Statements),
        Migration(version: 3, statements: schemaV3Statements),
    ]

    public static let migrationChain: [Int] = migrations.map { $0.version }

    /// 幂等迁移的纯判定部分：当前版本是否已达到目标（true 则 no-op）。
    /// 真正的 SQL 执行由平台持久层在**独立事务**内完成。
    public static func isUpToDate(_ current: Int) -> Bool { current >= schemaVersion }

    /// 未来版本一律拒绝：不猜测兼容（spec §42）。
    public static func accepts(_ current: Int) -> Bool { current <= schemaVersion }
}
