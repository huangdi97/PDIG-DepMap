// PDIG 逻辑 Schema 版本契约 —— Swift 移植。
//
// 源头：android/core/.../schema/SchemaVersion.kt（Android 已 CORE_FROZEN）。
//
// 逻辑 Schema 必须跨端一致；物理 DDL 可以不同
// （Android/iOS = SQLCipher，HarmonyOS = ArkData relationalStore）。
//
// 未来版本必须被**明确拒绝**，不得猜测兼容（spec §42 / MIG 原则）。

import Foundation

public enum SchemaVersion {

    /// 应用逻辑 Schema 版本（spec/domain/domain.json: appSchemaVersion）。
    public static let current: Int = CanonicalSpec.appSchemaVersion

    /// 支持的 DB 迁移链。
    public static let migrations: [Int] = [1, 2, 3]

    /// Graph payload 版本契约。
    public static let payloadKind: String = CanonicalSpec.graphPayloadKind
    public static let payloadVersion: Int = CanonicalSpec.graphPayloadVersion
    public static let payloadMigratableFrom: [Int] = [1, 2]

    /// 逻辑图 payload 导出的表集合（change_plans/reality_drifts/discovery_candidates 不在其中）。
    public static let payloadTables: [String] = [
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
    ]

    /// 必须被拒绝的 schema 版本（不得猜测兼容）。
    public static let rejectedSchemaVersions: [Int] = [0, 4, 99, 100]

    /// 必须被拒绝的 payload 版本。
    public static let rejectedPayloadVersions: [Int] = [0, 4, 99]

    /// 这些动作**永不** bump graphRevision（spec/state-machines）。
    public static let graphRevisionNeverBumpedBy: [String] = [
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
    ]

    /// 未来版本一律拒绝 —— 这里**不做**"也许能读"的猜测：
    /// 前向不兼容是产品决策，不是工程便利。
    public static func acceptsSchemaVersion(_ v: Int) -> Bool { v >= 1 && v <= current }

    public static func acceptsPayloadVersion(_ v: Int) -> Bool {
        v == payloadVersion || payloadMigratableFrom.contains(v)
    }
}
