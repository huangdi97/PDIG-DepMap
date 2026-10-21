import Foundation
import PDIGCore

/// Production-only ChangePlan mutation policy for iOS vNext.
///
/// The UI never edits a plan locally. This authority validates the persisted
/// prerequisite graph, graph freshness, terminal state and verification state
/// before producing the next immutable plan value for repository persistence.
enum ProductionChangeMutationError: Error, Equatable {
    case sessionLocked
    case planNotFound
    case planFrozen
    case needsReanalysis
    case actionNotFound
    case actionAlreadyDone
    case missingPrerequisite(String)
    case prerequisiteIncomplete(String)
    case prerequisiteUnverified(String)
    case retirementScopeNeedsReview
    case verificationNotRequired
    case actionNotDone
    case verificationFrozen
}

enum ProductionChangeAuthority {
    static func canCompleteAction(
        plan: ChangePlan,
        currentGraphRevision: Int,
        actionId: String,
        pendingReviewCount: Int = 0,
        pendingCandidateCount: Int = 0,
        openDriftCount: Int = 0
    ) -> Bool {
        (try? completeAction(
            plan: plan,
            currentGraphRevision: currentGraphRevision,
            actionId: actionId,
            pendingReviewCount: pendingReviewCount,
            pendingCandidateCount: pendingCandidateCount,
            openDriftCount: openDriftCount
        )) != nil
    }

    static func canVerifyAction(
        plan: ChangePlan,
        currentGraphRevision: Int,
        actionId: String
    ) -> Bool {
        (try? verifyAction(
            plan: plan,
            currentGraphRevision: currentGraphRevision,
            actionId: actionId
        )) != nil
    }

    static func completeAction(
        plan: ChangePlan,
        currentGraphRevision: Int,
        actionId: String,
        pendingReviewCount: Int = 0,
        pendingCandidateCount: Int = 0,
        openDriftCount: Int = 0
    ) throws -> ChangePlan {
        try requireMutable(plan, currentGraphRevision: currentGraphRevision)
        guard let index = plan.actions.firstIndex(where: { $0.id == actionId }) else {
            throw ProductionChangeMutationError.actionNotFound
        }
        let action = plan.actions[index]
        guard !action.done else {
            throw ProductionChangeMutationError.actionAlreadyDone
        }

        try requirePrerequisites(action, in: plan)

        if plan.scenario == ScenarioFlow.replacePhoneScenarioId &&
            action.id == "phone-retire-old" {
            // iOS keeps add + migrate as separate path-construction actions and
            // uses phone-verify-new as the independent verification of that
            // resulting path. Make-Before-Break therefore requires the whole
            // persisted chain to be internally consistent before retirement.
            guard pendingReviewCount == 0,
                  pendingCandidateCount == 0,
                  openDriftCount == 0 else {
                throw ProductionChangeMutationError.retirementScopeNeedsReview
            }

            for requiredPathActionId in ["phone-add-new", "phone-migrate"] {
                guard let pathAction = plan.actions.first(where: { $0.id == requiredPathActionId }) else {
                    throw ProductionChangeMutationError.missingPrerequisite(requiredPathActionId)
                }
                guard pathAction.done else {
                    throw ProductionChangeMutationError.prerequisiteIncomplete(requiredPathActionId)
                }
            }

            guard let verification = plan.actions.first(where: { $0.id == "phone-verify-new" }) else {
                throw ProductionChangeMutationError.missingPrerequisite("phone-verify-new")
            }
            guard verification.done else {
                throw ProductionChangeMutationError.prerequisiteIncomplete("phone-verify-new")
            }
            guard verification.verification?.status == .verified else {
                throw ProductionChangeMutationError.prerequisiteUnverified("phone-verify-new")
            }
        }

        var actions = plan.actions
        actions[index] = replacing(action, done: true)
        return rebuild(plan, actions: actions)
    }

    static func verifyAction(
        plan: ChangePlan,
        currentGraphRevision: Int,
        actionId: String
    ) throws -> ChangePlan {
        try requireMutable(plan, currentGraphRevision: currentGraphRevision)
        guard let index = plan.actions.firstIndex(where: { $0.id == actionId }) else {
            throw ProductionChangeMutationError.actionNotFound
        }
        let action = plan.actions[index]
        guard action.done else {
            throw ProductionChangeMutationError.actionNotDone
        }
        guard let verification = action.verification else {
            throw ProductionChangeMutationError.verificationNotRequired
        }
        guard verification.status == .pending || verification.status == .evidenceSuggested else {
            throw ProductionChangeMutationError.verificationFrozen
        }

        let verified = ActionVerification(
            method: verification.method,
            status: .verified,
            evidenceRefs: verification.evidenceRefs,
            expectedFromNodeId: verification.expectedFromNodeId,
            expectedToNodeId: verification.expectedToNodeId
        )
        var actions = plan.actions
        actions[index] = replacing(action, verification: verified)
        return rebuild(plan, actions: actions)
    }

    private static func requireMutable(
        _ plan: ChangePlan,
        currentGraphRevision: Int
    ) throws {
        if ChangePlanMachine.isFrozen(plan.workflowState) {
            throw ProductionChangeMutationError.planFrozen
        }
        if PlanRules.isPlanStale(
            plan: plan,
            currentGraphRevision: currentGraphRevision
        ) {
            throw ProductionChangeMutationError.needsReanalysis
        }
    }

    private static func requirePrerequisites(
        _ action: PlanAction,
        in plan: ChangePlan
    ) throws {
        for prerequisiteId in action.prerequisiteActionIds {
            guard let prerequisite = plan.actions.first(where: { $0.id == prerequisiteId }) else {
                throw ProductionChangeMutationError.missingPrerequisite(prerequisiteId)
            }
            guard prerequisite.done else {
                throw ProductionChangeMutationError.prerequisiteIncomplete(prerequisiteId)
            }
        }
    }

    private static func replacing(
        _ action: PlanAction,
        done: Bool? = nil,
        verification: ActionVerification? = nil
    ) -> PlanAction {
        PlanAction(
            id: action.id,
            title: action.title,
            phase: action.phase,
            done: done ?? action.done,
            resolvesImpactKeys: action.resolvesImpactKeys,
            prerequisiteActionIds: action.prerequisiteActionIds,
            verification: verification ?? action.verification
        )
    }

    private static func rebuild(
        _ plan: ChangePlan,
        actions: [PlanAction]
    ) -> ChangePlan {
        let workflow = synchronizedWorkflow(
            current: plan.workflowState,
            actions: actions
        )
        return ChangePlan(
            id: plan.id,
            templateId: plan.templateId,
            scenario: plan.scenario,
            title: plan.title,
            workflowState: workflow,
            baselineGraphRevision: plan.baselineGraphRevision,
            lastAnalyzedGraphRevision: plan.lastAnalyzedGraphRevision,
            targetNodeId: plan.targetNodeId,
            effectiveDate: plan.effectiveDate,
            actions: actions
        )
    }

    private static func synchronizedWorkflow(
        current: ChangePlanWorkflowState,
        actions: [PlanAction]
    ) -> ChangePlanWorkflowState {
        guard !actions.isEmpty else { return current }
        let anyDone = actions.contains(where: { $0.done })
        let allDone = actions.allSatisfy { $0.done }
        let allVerified = actions.allSatisfy { action in
            guard let verification = action.verification else { return true }
            return verification.status == .verified || verification.status == .notRequired
        }

        let desired: ChangePlanWorkflowState? = {
            if allDone && allVerified { return .completed }
            if allDone { return .verifying }
            if anyDone { return .inProgress }
            return nil
        }()
        guard let desired else { return current }
        if desired == current { return current }
        if ChangePlanMachine.canTransition(from: current, to: desired) {
            return desired
        }
        // A review/draft plan can become in-progress on the same authoritative
        // mutation that happens to make every current step done. Do not jump
        // over the state machine directly to verifying/completed.
        if anyDone && ChangePlanMachine.canTransition(from: current, to: .inProgress) {
            return .inProgress
        }
        return current
    }
}
