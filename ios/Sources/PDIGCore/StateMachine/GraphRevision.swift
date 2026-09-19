// graphRevision 语义判定 —— Swift 移植。
//
// 源头：harmony/entry/.../domain/GraphRevision.ets，契约原文见
// `spec/state-machines/state-machines.json` → GraphRevision.bumpsOn / neverBumpsOn。
//
// 铁律（原文照录）：
//   GraphRevision 是 Confirmed Reality Graph 的**语义版本**。
//   仅 Reality mutation 提升（在同一次 DB 事务内）：
//     Dependency created / retired / reactivated / criticality 被用户修改
//     DependencyGroup confirmed / retired / membership 确认变化
//   以下一律不提升：
//     Observation / ImportSession / Fingerprint / Evidence /
//     Proposal（创建与 confidence）/ RealityDrift / DiscoveryCandidate /
//     Timeline projection / UI 状态。
//
// 为什么单独成模块：判定"哪些操作提升 graphRevision"是一条**正确性铁律**，
// 它决定了 ChangePlan 何时被判 stale、Timeline 何时提示需重校验。集中在纯函数
// 里，才能在不接触数据库的情况下逐条验证。

import Foundation

public enum GraphRevision {

    // --- mutation 词汇表（契约用词，不是内部发明） ---------------------------
    //
    // 这些字符串是**跨端共享的冻结词汇表**：
    //   spec/state-machines/state-machines.json → machines[].bumpsOn
    //   fixtures/state-machine/state-machine-graph-revision.json → expected.bumpsOn
    //   android/core/.../StateMachines.kt → GraphRevisionMachine.bumpsOn
    // 自造词汇的后果是：契约里每一个真实 mutation 都被判为"不提升"。
    public static let mutNodeCreate = "node_create"
    public static let mutNodeUpdate = "node_update"
    public static let mutNodeArchive = "node_archive"
    public static let mutDependencyConfirmCreate = "dependency_confirm_create"
    public static let mutDependencyConfirmUpdate = "dependency_confirm_update"
    public static let mutDependencyRetire = "dependency_retire"
    public static let mutDependencyReactivate = "dependency_reactivate"
    public static let mutGroupConfirm = "group_confirm"
    public static let mutGroupRetire = "group_retire"
    public static let mutDriftResolveReplacement = "drift_resolve_replacement"
    public static let mutDriftResolveAdditionalPath = "drift_resolve_additional_path"

    /// 会被提升的 mutation 全集（白名单，逐条对应契约 bumpsOn，不多不少）。
    public static let bumpsOn: [String] = [
        mutNodeCreate,
        mutNodeUpdate,
        mutNodeArchive,
        mutDependencyConfirmCreate,
        mutDependencyConfirmUpdate,
        mutDependencyRetire,
        mutDependencyReactivate,
        mutGroupConfirm,
        mutGroupRetire,
        mutDriftResolveReplacement,
        mutDriftResolveAdditionalPath,
    ]

    /// 持久化键名（与 TS 基准一致：复用 meta 表，初始 0，不做 event sourcing）。
    public static let graphRevisionKey = "graph_revision"

    /// **白名单**而非黑名单：只有被显式列为 Reality mutation 的操作才提升。
    ///
    /// 失效方向是选白名单的理由 —— 未来新增实体/事件时，黑名单会默认"提升"
    /// （把非 Reality 变化误记成 Reality 变更，让所有计划无端变 stale），
    /// 白名单则默认"不提升"（最多漏一次提示，不会误报）。
    public static func doesMutationBumpRevision(_ kind: String) -> Bool {
        bumpsOn.contains(kind)
    }

    /// 解析持久化的 revision 值：缺失 → 0；非有限数 → 0；负值 → 0。
    /// revision 是单调递增计数器，负数会让所有 stale 判定反转。
    public static func parseStoredRevision(_ raw: String?) -> Int {
        guard let raw = raw, !raw.isEmpty else { return 0 }
        guard let v = Double(raw), v.isFinite else { return 0 }
        if v < 0 { return 0 }
        return Int(v)
    }

    /// 下一次 revision（+1）。恢复/回滚语义由调用方的事务保证。
    public static func nextRevision(_ current: Int) -> Int { current + 1 }

    /// 计划相对于当前图版本是否需要重校验。与 PlanRules.isPlanStale 同口径。
    public static func revisionRequiresRevalidation(
        _ lastAnalyzedGraphRevision: Int, _ currentGraphRevision: Int
    ) -> Bool {
        currentGraphRevision > lastAnalyzedGraphRevision
    }
}
