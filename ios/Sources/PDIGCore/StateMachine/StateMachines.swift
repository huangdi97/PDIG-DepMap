// PDIG 状态机 —— Swift 移植（spec/state-machines/）。
//
// 源头：android/core/.../statemachine/StateMachines.kt。
//
// 这些不是文档副本：它们是**运行期真正在用的迁移表与守卫**，
// 由 conformance runner 序列化后与 spec 逐字段比对，
// 从而证明"Swift 实现 == Canonical Spec"，而不是"Swift 自己猜了一套"。

import Foundation

public struct Guard: Equatable, Sendable {
    public let id: String
    public let rule: String
    public let errorCode: String

    public init(id: String, rule: String, errorCode: String) {
        self.id = id
        self.rule = rule
        self.errorCode = errorCode
    }
}

// ---------------------------------------------------------------------------
// ChangePlan
// ---------------------------------------------------------------------------

public enum ChangePlanMachine {
    /// 合法工作流迁移表；completed / cancelled 为终态（无出边）。
    public static let transitions: [ChangePlanWorkflowState: [ChangePlanWorkflowState]] = [
        .draft: [.analyzed, .reviewRequired, .ready, .inProgress, .cancelled],
        .analyzed: [.reviewRequired, .ready, .inProgress, .cancelled],
        .reviewRequired: [.ready, .inProgress, .cancelled, .analyzed],
        .ready: [.inProgress, .cancelled, .reviewRequired],
        .inProgress: [.verifying, .completed, .cancelled],
        .verifying: [.completed, .cancelled, .inProgress],
        .completed: [],
        .cancelled: [],
    ]

    public static let terminal: [ChangePlanWorkflowState] = [.completed, .cancelled]

    public struct DerivedStatusRule: Equatable, Sendable {
        public let name: String
        public let value: String
        public let rule: String
        public let persisted: Bool
    }

    /// needs_revalidation 是**派生**状态，永不回写存储（spec §20）。
    public static let derivedStatus = DerivedStatusRule(
        name: "PlanEffectiveStatus",
        value: "needs_revalidation",
        rule: "isStale(plan, currentGraphRevision) => workflowState NOT IN (completed, cancelled) AND currentGraphRevision > lastAnalyzedGraphRevision",
        persisted: false
    )

    public static let guards: [Guard] = [
        Guard(
            id: "PLAN-ACTION-FROZEN",
            rule: "completeAction on a plan whose workflowState is completed or cancelled MUST be rejected",
            errorCode: "action_frozen"
        ),
        Guard(
            id: "PLAN-ACTION-EXISTS",
            rule: "completeAction / verifyActionManually / suggestVerificationFromEvidence with an unknown actionId MUST be rejected",
            errorCode: "entity_not_found"
        ),
    ]

    public static func canTransition(from: ChangePlanWorkflowState, to: ChangePlanWorkflowState) -> Bool {
        transitions[from]?.contains(to) ?? false
    }

    public static func isFrozen(_ state: ChangePlanWorkflowState) -> Bool {
        terminal.contains(state)
    }
}

// ---------------------------------------------------------------------------
// RealityDrift
// ---------------------------------------------------------------------------

public enum DriftMachine {
    public static let initial: DriftStatus = .`open`

    public static let transitions: [DriftStatus: [DriftStatus]] = [
        .`open`: [.confirmedChange, .dismissed],
        .confirmedChange: [],
        .dismissed: [],
        .superseded: [],
    ]

    public struct CreationRule: Equatable, Sendable {
        public let requires: String
        public let minObservations: Int
        public let absenceOnly: String
        public let alreadyConfirmedSource: String
        public let belowThreshold: String
        public let duplicateEvidenceRef: String
        public let upsert: String
    }

    public static let creationRule = CreationRule(
        requires: "positive evidence only",
        minObservations: 2,
        absenceOnly: "must never create a drift",
        alreadyConfirmedSource: "ignored with reason already_confirmed",
        belowThreshold: "ignored with reason below_threshold",
        duplicateEvidenceRef: "does not re-create or re-count",
        upsert: "same drift key accumulates observationCount and evidenceRefs into ONE open drift"
    )

    public static let guards: [Guard] = [
        Guard(
            id: "DRIFT-OPEN-ONLY",
            rule: "resolve/dismiss on a non-open drift MUST be rejected",
            errorCode: "illegal_state_transition"
        ),
    ]

    public static func canResolve(_ status: DriftStatus) -> Bool { status == .`open` }
}

// ---------------------------------------------------------------------------
// DiscoveryCandidate
// ---------------------------------------------------------------------------

public enum CandidateMachine {
    public static let initial: CandidateStatus = .pending

    public static let transitions: [CandidateStatus: [CandidateStatus]] = [
        .pending: [.accepted, .dismissed],
        .accepted: [],
        .dismissed: [.pending],
        .superseded: [],
    ]

    public struct AcceptEffect: Equatable, Sendable {
        public let mutatesReality: Bool
        public let effects: [String]
        public let bumpsGraphRevision: Bool
        public let replay: String
    }

    public struct DismissEffect: Equatable, Sendable {
        public let mutatesReality: Bool
        public let effects: [String]
        public let reappeal: String
    }

    public static let accept = AcceptEffect(
        mutatesReality: true,
        effects: ["create exactly one Node"],
        bumpsGraphRevision: false,
        replay: "idempotent - accept replay returns created=false and the same nodeId"
    )

    public static let dismiss = DismissEffect(
        mutatesReality: false,
        effects: ["record dismissedAtObservationCount"],
        reappeal: "returns to pending only after newObservations >= 2"
    )

    public static let guards: [Guard] = [
        Guard(
            id: "CANDIDATE-PENDING-ONLY",
            rule: "accept on a dismissed or accepted candidate MUST be rejected",
            errorCode: "illegal_state_transition"
        ),
    ]
}

// ---------------------------------------------------------------------------
// Action Verification
// ---------------------------------------------------------------------------

public enum VerificationMachine {
    public static let initial: ActionVerificationStatus = .pending

    public static let terminal: [ActionVerificationStatus] = [.verified, .failed]

    public static let transitions: [ActionVerificationStatus: [ActionVerificationStatus]] = [
        .notRequired: [],
        .pending: [.evidenceSuggested, .verified, .failed, .notRequired],
        .evidenceSuggested: [.verified, .failed, .evidenceSuggested],
        .verified: [],
        .failed: [],
    ]

    public struct EvidenceSignalRule: Equatable, Sendable {
        public let appliesOnlyTo: [ActionVerificationStatus]
        public let requiresMethod: String
        public let requiresMatch: String
        public let effect: String
        public let never: [String]
    }

    public static let evidenceSignalRule = EvidenceSignalRule(
        appliesOnlyTo: [.pending, .evidenceSuggested],
        requiresMethod: "future_observation",
        requiresMatch: "expectedFromNodeId AND expectedToNodeId must both equal the signal",
        effect: "status becomes evidence_suggested",
        never: ["mutates Reality", "auto-verifies", "bumps graphRevision"]
    )

    public static let guards: [Guard] = [
        Guard(
            id: "VF-FROZEN",
            rule: "evidence suggestion must not override verified, failed or not_required",
            errorCode: "illegal_state_transition"
        ),
    ]
}

// ---------------------------------------------------------------------------
// GraphRevision
// ---------------------------------------------------------------------------

public enum GraphRevisionMachine {
    public static let initial = 0
    public static let monotonic = true
    public static let atomicity =
        "the Reality mutation and the revision increment MUST happen inside one database transaction on every platform"

    public static let bumpsOn: [String] = [
        "node_create",
        "node_update",
        "node_archive",
        "dependency_confirm_create",
        "dependency_confirm_update",
        "dependency_retire",
        "dependency_reactivate",
        "group_confirm",
        "group_retire",
        "drift_resolve_replacement",
        "drift_resolve_additional_path",
    ]

    public static let neverBumpsOn: [String] = [
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
}
