package com.pdig.core.schema

import com.pdig.core.generated.CanonicalSpec

/**
 * PDIG 逻辑 Schema 版本契约。
 *
 * 逻辑 Schema 必须跨端一致；物理 DDL 可以不同
 * （Android/iOS = SQLCipher，HarmonyOS = ArkData relationalStore）。
 *
 * 未来版本必须被**明确拒绝**，不得猜测兼容（spec §42 / MIG 原则）。
 */
object SchemaVersion {

    /** 应用逻辑 Schema 版本（spec/domain/domain.json: appSchemaVersion）。 */
    val current: Int = CanonicalSpec.APP_SCHEMA_VERSION

    /** 支持的 DB 迁移链。 */
    val migrations: List<Int> = listOf(1, 2, 3)

    /** Graph payload 版本契约。 */
    val payloadKind: String = CanonicalSpec.GRAPH_PAYLOAD_KIND
    val payloadVersion: Int = CanonicalSpec.GRAPH_PAYLOAD_VERSION
    val payloadMigratableFrom: List<Int> = listOf(1, 2)

    /** 逻辑图 payload 导出的表集合（change_plans/reality_drifts/discovery_candidates 不在其中）。 */
    val payloadTables: List<String> = listOf(
        "meta",
        "nodes",
        "dependencies",
        "dependency_groups",
        "dependency_proposals",
        "dependency_group_proposals",
        "proposal_evidence_refs",
        "evidence",
        "observation_fingerprints",
        "import_sessions",
        "source_instances",
    )

    /** 必须被拒绝的 schema 版本（不得猜测兼容）。 */
    val rejectedSchemaVersions: List<Int> = listOf(0, 4, 99, 100)

    /** 必须被拒绝的 payload 版本。 */
    val rejectedPayloadVersions: List<Int> = listOf(0, 4, 99)

    /** 这些动作**永不** bump graphRevision（spec/state-machines）。 */
    val graphRevisionNeverBumpedBy: List<String> = listOf(
        "evidence_record",
        "proposal_upsert",
        "proposal_decision",
        "candidate_upsert",
        "candidate_dismiss",
        "candidate_accept",
        "drift_detect",
        "drift_dismiss",
        "timeline_build",
        "import_session_record",
        "plan_create",
        "plan_rebase",
        "plan_transition",
        "action_complete",
        "action_verify",
    )

    /**
     * 未来版本一律拒绝。
     * 注意：这里**不做**"也许能读"的猜测 —— 前向不兼容是产品决策，不是工程便利。
     */
    fun acceptsSchemaVersion(v: Int): Boolean = v in 1..current

    fun acceptsPayloadVersion(v: Int): Boolean = v == payloadVersion || v in payloadMigratableFrom
}
