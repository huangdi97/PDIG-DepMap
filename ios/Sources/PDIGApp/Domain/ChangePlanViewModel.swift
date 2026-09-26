// ChangePlanViewModel —— 变更计划（task #9）。
//
// 能力：
//  - readiness：PlanRules.computePlanReadiness（blocked / review_required /
//    ready_with_known_scope）+ stale → needs_revalidation（派生，不持久化）
//  - CTA 映射：blocked→处理必须事项 / review_required→继续确认 /
//    needs_revalidation→重新检查 / ready_with_known_scope→继续下一步 /
//    verifying→查看验证 / completed→查看结果
//  - 前置关系 plain language：必须先完成 / 完成后才能继续 / 等待验证 /
//    可以并行处理 / 验证后才能移除旧路径（ActionDagEngine 提供稳定拓扑顺序）

import Foundation
import PDIGCore

public struct PlanView: Equatable, Sendable {
    public let plan: ChangePlan
    public let readiness: PlanReadiness
    public let effectiveStatus: String        // wire（派生 needs_revalidation）
    public let readinessText: String
    public let ctaTitle: String
    public let workflowText: String
    /// 每条动作的展示：id / 标题 / 阶段 / 完成态 / 验证状态 / 前置说明。
    public let actions: [PlanActionView]
    /// 未解决 must_change 的条数（用于 blocked 提示）。
    public let unresolvedMustChange: Int

    public init(
        plan: ChangePlan,
        readiness: PlanReadiness,
        effectiveStatus: String,
        readinessText: String,
        ctaTitle: String,
        workflowText: String,
        actions: [PlanActionView],
        unresolvedMustChange: Int
    ) {
        self.plan = plan
        self.readiness = readiness
        self.effectiveStatus = effectiveStatus
        self.readinessText = readinessText
        self.ctaTitle = ctaTitle
        self.workflowText = workflowText
        self.actions = actions
        self.unresolvedMustChange = unresolvedMustChange
    }
}

public struct PlanActionView: Equatable, Sendable, Identifiable {
    public let id: String
    public let title: String
    public let phaseText: String
    public let done: Bool
    public let verificationStatusText: String
    public let prerequisiteText: String

    public init(
        id: String, title: String, phaseText: String, done: Bool,
        verificationStatusText: String, prerequisiteText: String
    ) {
        self.id = id
        self.title = title
        self.phaseText = phaseText
        self.done = done
        self.verificationStatusText = verificationStatusText
        self.prerequisiteText = prerequisiteText
    }
}

public struct PlanInput: Equatable, Sendable {
    public let plan: ChangePlan
    public let currentGraphRevision: Int
    public let prerequisiteEdges: [String: [String]]
    /// 待确认/未解析的计数（readiness 输入）。
    public let pendingMustChange: Int
    public let pendingNeedsReview: Int
    public let unresolvedCandidates: Int
    public let pendingRelevantProposals: Int
    public let staleRelevantDependencies: Int
    public let unfinishedChangeActions: Int

    public init(
        plan: ChangePlan,
        currentGraphRevision: Int,
        prerequisiteEdges: [String: [String]] = [:],
        pendingMustChange: Int = 0,
        pendingNeedsReview: Int = 0,
        unresolvedCandidates: Int = 0,
        pendingRelevantProposals: Int = 0,
        staleRelevantDependencies: Int = 0,
        unfinishedChangeActions: Int = 0
    ) {
        self.plan = plan
        self.currentGraphRevision = currentGraphRevision
        self.prerequisiteEdges = prerequisiteEdges
        self.pendingMustChange = pendingMustChange
        self.pendingNeedsReview = pendingNeedsReview
        self.unresolvedCandidates = unresolvedCandidates
        self.pendingRelevantProposals = pendingRelevantProposals
        self.staleRelevantDependencies = staleRelevantDependencies
        self.unfinishedChangeActions = unfinishedChangeActions
    }
}

public enum ChangePlanViewModel {

    /// 计划是否 stale（派生状态 needs_revalidation，不持久化）。
    public static func isStale(_ plan: ChangePlan, currentGraphRevision: Int) -> Bool {
        PlanRules.isPlanStale(plan: plan, currentGraphRevision: currentGraphRevision)
    }
    public static func ctaTitle(plan: ChangePlan, readiness: PlanReadiness, currentGraphRevision: Int) -> String {
        if plan.workflowState == .completed { return CopyZh.ctaCompleted }
        if plan.workflowState == .verifying { return CopyZh.ctaVerifying }
        if PlanRules.isPlanStale(plan: plan, currentGraphRevision: currentGraphRevision) {
            return CopyZh.ctaNeedsRevalidation
        }
        switch readiness {
        case .blocked: return CopyZh.ctaBlocked
        case .reviewRequired: return CopyZh.ctaReviewRequired
        case .readyWithKnownScope: return CopyZh.ctaReadyWithKnownScope
        }
    }

    public static func readiness(_ input: PlanInput) -> PlanReadiness {
        PlanRules.computePlanReadiness(PlanReadinessInput(
            plan: input.plan,
            currentGraphRevision: input.currentGraphRevision,
            pendingMustChange: input.pendingMustChange,
            pendingNeedsReview: input.pendingNeedsReview,
            unresolvedCandidates: input.unresolvedCandidates,
            pendingRelevantProposals: input.pendingRelevantProposals,
            staleRelevantDependencies: input.staleRelevantDependencies,
            unfinishedChangeActions: input.unfinishedChangeActions
        ))
    }

    public static func unresolvedMustChange(
        actions: [PlanAction],
        impactKeys: [String]
    ) -> Int {
        PlanRules.countUnresolvedMustChange(actions: actions, keys: impactKeys)
    }

    public static func workflowText(_ state: ChangePlanWorkflowState) -> String {
        switch state {
        case .draft: return CopyZh.planDraft
        case .analyzed: return CopyZh.planAnalyzed
        case .reviewRequired: return CopyZh.planReviewRequired
        case .ready: return CopyZh.planReady
        case .inProgress: return CopyZh.planInProgress
        case .verifying: return CopyZh.planVerifying
        case .completed: return CopyZh.planCompleted
        case .cancelled: return CopyZh.planCancelled
        }
    }

    public static func readinessText(_ readiness: PlanReadiness) -> String {
        switch readiness {
        case .blocked: return CopyZh.readinessBlocked
        case .reviewRequired: return CopyZh.readinessReviewRequired
        case .readyWithKnownScope: return CopyZh.readinessReadyWithKnownScope
        }
    }

    public static func verificationText(_ status: ActionVerificationStatus?) -> String {
        guard let status = status else { return CopyZh.verificationNotRequired }
        switch status {
        case .pending: return CopyZh.verificationPending
        case .evidenceSuggested: return CopyZh.verificationEvidenceSuggested
        case .verified: return CopyZh.verificationVerified
        case .failed: return CopyZh.verificationFailed
        case .notRequired: return CopyZh.verificationNotRequired
        }
    }

    /// 前置关系 → 白话（必须先完成 / 完成后才能继续 / 等待验证 / 可以并行处理 /
    /// 验证后才能移除旧路径）。
    public static func prerequisiteText(
        actionId: String,
        actions: [PlanAction],
        edges: [String: [String]]
    ) -> String {
        let prerequisites = edges[actionId] ?? []
        if prerequisites.isEmpty { return CopyZh.parallel }
        let names = prerequisites.map { id in actions.first { $0.id == id }?.title ?? id }
        if actionId.contains("retire") || actionId.hasSuffix("final") {
            return CopyZh.verifyBeforeRemove
        }
        let pending = prerequisites.filter { id in actions.first { $0.id == id }?.done != true }
        if pending.isEmpty { return CopyZh.parallel }
        let joined = names.joined(separator: "、")
        return "\(CopyZh.prerequisite) \(joined)（\(CopyZh.completesBefore)）"
    }

    /// 动作前置的等待验证语义：验证类动作未 verified 时，后续动作标"等待验证"。
    public static func isWaitingVerification(_ actionId: String, edges: [String: [String]], actions: [PlanAction]) -> Bool {
        for prereq in edges[actionId] ?? [] {
            if let a = actions.first(where: { $0.id == prereq }),
               a.phase == .verify,
               a.verification?.status != .verified {
                return true
            }
        }
        return false
    }

    /// 完整视图模型。
    public static func view(input: PlanInput, impactKeys: [String] = []) -> PlanView {
        let readinessValue = readiness(input)
        let stale = isStale(input.plan, currentGraphRevision: input.currentGraphRevision)
        let effectiveStatus = stale ? PlanEffectiveStatus.needsRevalidation.wire : input.plan.workflowState.wire
        let unresolved = unresolvedMustChange(actions: input.plan.actions, impactKeys: impactKeys)

        let actionViews: [PlanActionView] = input.plan.actions.map { a in
            let pre = prerequisiteText(actionId: a.id, actions: input.plan.actions, edges: input.prerequisiteEdges)
            let waiting = isWaitingVerification(a.id, edges: input.prerequisiteEdges, actions: input.plan.actions)
            let preText = waiting ? CopyZh.waitingVerification : pre
            return PlanActionView(
                id: a.id,
                title: a.title,
                phaseText: phaseText(a.phase),
                done: a.done,
                verificationStatusText: verificationText(a.verification?.status),
                prerequisiteText: preText
            )
        }

        return PlanView(
            plan: input.plan,
            readiness: readinessValue,
            effectiveStatus: effectiveStatus,
            readinessText: stale ? CopyZh.planNeedsRevalidation : readinessText(readinessValue),
            ctaTitle: ctaTitle(plan: input.plan, readiness: readinessValue, currentGraphRevision: input.currentGraphRevision),
            workflowText: workflowText(input.plan.workflowState),
            actions: actionViews,
            unresolvedMustChange: unresolved
        )
    }

    private static func phaseText(_ phase: PlanActionPhase) -> String {
        switch phase {
        case .prepare: return "准备"
        case .change: return "变更"
        case .verify: return "验证"
        }
    }
}
