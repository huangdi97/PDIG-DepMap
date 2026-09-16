package com.pdig.core.statemachine

import com.pdig.core.generated.ActionVerificationStatus
import com.pdig.core.generated.CandidateStatus
import com.pdig.core.generated.ChangePlanWorkflowState
import com.pdig.core.generated.DriftStatus

/**
 * PDIG 状态机 —— Kotlin 权威定义（spec/state-machines/）。
 *
 * 这些不是文档副本：它们是 **运行期真正在用的迁移表与守卫**，
 * 由 conformance runner 序列化后与 spec 逐字段比对，
 * 从而证明"Kotlin 实现 == Canonical Spec"，而不是"Kotlin 自己猜了一套"。
 */

// ---------------------------------------------------------------------------
// ChangePlan
// ---------------------------------------------------------------------------

object ChangePlanMachine {

    /** 合法工作流迁移表；completed / cancelled 为终态（无出边）。 */
    val transitions: Map<ChangePlanWorkflowState, List<ChangePlanWorkflowState>> = mapOf(
        ChangePlanWorkflowState.DRAFT to listOf(
            ChangePlanWorkflowState.ANALYZED,
            ChangePlanWorkflowState.REVIEW_REQUIRED,
            ChangePlanWorkflowState.READY,
            ChangePlanWorkflowState.IN_PROGRESS,
            ChangePlanWorkflowState.CANCELLED,
        ),
        ChangePlanWorkflowState.ANALYZED to listOf(
            ChangePlanWorkflowState.REVIEW_REQUIRED,
            ChangePlanWorkflowState.READY,
            ChangePlanWorkflowState.IN_PROGRESS,
            ChangePlanWorkflowState.CANCELLED,
        ),
        ChangePlanWorkflowState.REVIEW_REQUIRED to listOf(
            ChangePlanWorkflowState.READY,
            ChangePlanWorkflowState.IN_PROGRESS,
            ChangePlanWorkflowState.CANCELLED,
            ChangePlanWorkflowState.ANALYZED,
        ),
        ChangePlanWorkflowState.READY to listOf(
            ChangePlanWorkflowState.IN_PROGRESS,
            ChangePlanWorkflowState.CANCELLED,
            ChangePlanWorkflowState.REVIEW_REQUIRED,
        ),
        ChangePlanWorkflowState.IN_PROGRESS to listOf(
            ChangePlanWorkflowState.VERIFYING,
            ChangePlanWorkflowState.COMPLETED,
            ChangePlanWorkflowState.CANCELLED,
        ),
        ChangePlanWorkflowState.VERIFYING to listOf(
            ChangePlanWorkflowState.COMPLETED,
            ChangePlanWorkflowState.CANCELLED,
            ChangePlanWorkflowState.IN_PROGRESS,
        ),
        ChangePlanWorkflowState.COMPLETED to emptyList(),
        ChangePlanWorkflowState.CANCELLED to emptyList(),
    )

    val terminal: List<ChangePlanWorkflowState> = listOf(
        ChangePlanWorkflowState.COMPLETED,
        ChangePlanWorkflowState.CANCELLED,
    )

    data class DerivedStatusRule(
        val name: String,
        val value: String,
        val rule: String,
        val persisted: Boolean,
    )

    /** needs_revalidation 是**派生**状态，永不回写存储（spec §20）。 */
    val derivedStatus: DerivedStatusRule = DerivedStatusRule(
        name = "PlanEffectiveStatus",
        value = "needs_revalidation",
        rule = "isStale(plan, currentGraphRevision) => workflowState NOT IN (completed, cancelled) AND currentGraphRevision > lastAnalyzedGraphRevision",
        persisted = false,
    )

    val guards: List<Guard> = listOf(
        Guard(
            id = "PLAN-ACTION-FROZEN",
            rule = "completeAction on a plan whose workflowState is completed or cancelled MUST be rejected",
            errorCode = "action_frozen",
        ),
        Guard(
            id = "PLAN-ACTION-EXISTS",
            rule = "completeAction / verifyActionManually / suggestVerificationFromEvidence with an unknown actionId MUST be rejected",
            errorCode = "entity_not_found",
        ),
    )

    fun canTransition(from: ChangePlanWorkflowState, to: ChangePlanWorkflowState): Boolean =
        transitions[from]?.contains(to) ?: false

    fun isFrozen(state: ChangePlanWorkflowState): Boolean = terminal.contains(state)
}

// ---------------------------------------------------------------------------
// RealityDrift
// ---------------------------------------------------------------------------

object DriftMachine {

    val initial: DriftStatus = DriftStatus.OPEN

    val transitions: Map<DriftStatus, List<DriftStatus>> = mapOf(
        DriftStatus.OPEN to listOf(DriftStatus.CONFIRMED_CHANGE, DriftStatus.DISMISSED),
        DriftStatus.CONFIRMED_CHANGE to emptyList(),
        DriftStatus.DISMISSED to emptyList(),
        DriftStatus.SUPERSEDED to emptyList(),
    )

    data class CreationRule(
        val requires: String,
        val minObservations: Int,
        val absenceOnly: String,
        val alreadyConfirmedSource: String,
        val belowThreshold: String,
        val duplicateEvidenceRef: String,
        val upsert: String,
    )

    val creationRule = CreationRule(
        requires = "positive evidence only",
        minObservations = 2,
        absenceOnly = "must never create a drift",
        alreadyConfirmedSource = "ignored with reason already_confirmed",
        belowThreshold = "ignored with reason below_threshold",
        duplicateEvidenceRef = "does not re-create or re-count",
        upsert = "same drift key accumulates observationCount and evidenceRefs into ONE open drift",
    )

    val guards = listOf(
        Guard(
            id = "DRIFT-OPEN-ONLY",
            rule = "resolve/dismiss on a non-open drift MUST be rejected",
            errorCode = "illegal_state_transition",
        ),
    )

    fun canResolve(status: DriftStatus): Boolean = status == DriftStatus.OPEN
}

// ---------------------------------------------------------------------------
// DiscoveryCandidate
// ---------------------------------------------------------------------------

object CandidateMachine {

    val initial: CandidateStatus = CandidateStatus.PENDING

    val transitions: Map<CandidateStatus, List<CandidateStatus>> = mapOf(
        CandidateStatus.PENDING to listOf(CandidateStatus.ACCEPTED, CandidateStatus.DISMISSED),
        CandidateStatus.ACCEPTED to emptyList(),
        CandidateStatus.DISMISSED to listOf(CandidateStatus.PENDING),
        CandidateStatus.SUPERSEDED to emptyList(),
    )

    data class AcceptEffect(
        val mutatesReality: Boolean,
        val effects: List<String>,
        val bumpsGraphRevision: Boolean,
        val replay: String,
    )

    data class DismissEffect(
        val mutatesReality: Boolean,
        val effects: List<String>,
        val reappeal: String,
    )

    val accept = AcceptEffect(
        mutatesReality = true,
        effects = listOf("create exactly one Node"),
        bumpsGraphRevision = false,
        replay = "idempotent - accept replay returns created=false and the same nodeId",
    )

    val dismiss = DismissEffect(
        mutatesReality = false,
        effects = listOf("record dismissedAtObservationCount"),
        reappeal = "returns to pending only after newObservations >= 2",
    )

    val guards = listOf(
        Guard(
            id = "CANDIDATE-PENDING-ONLY",
            rule = "accept on a dismissed or accepted candidate MUST be rejected",
            errorCode = "illegal_state_transition",
        ),
    )
}

// ---------------------------------------------------------------------------
// Action Verification
// ---------------------------------------------------------------------------

object VerificationMachine {

    val initial: ActionVerificationStatus = ActionVerificationStatus.PENDING

    val terminal: List<ActionVerificationStatus> = listOf(
        ActionVerificationStatus.VERIFIED,
        ActionVerificationStatus.FAILED,
    )

    val transitions: Map<ActionVerificationStatus, List<ActionVerificationStatus>> = mapOf(
        ActionVerificationStatus.NOT_REQUIRED to emptyList(),
        ActionVerificationStatus.PENDING to listOf(
            ActionVerificationStatus.EVIDENCE_SUGGESTED,
            ActionVerificationStatus.VERIFIED,
            ActionVerificationStatus.FAILED,
            ActionVerificationStatus.NOT_REQUIRED,
        ),
        ActionVerificationStatus.EVIDENCE_SUGGESTED to listOf(
            ActionVerificationStatus.VERIFIED,
            ActionVerificationStatus.FAILED,
            ActionVerificationStatus.EVIDENCE_SUGGESTED,
        ),
        ActionVerificationStatus.VERIFIED to emptyList(),
        ActionVerificationStatus.FAILED to emptyList(),
    )

    data class EvidenceSignalRule(
        val appliesOnlyTo: List<ActionVerificationStatus>,
        val requiresMethod: String,
        val requiresMatch: String,
        val effect: String,
        val never: List<String>,
    )

    val evidenceSignalRule = EvidenceSignalRule(
        appliesOnlyTo = listOf(
            ActionVerificationStatus.PENDING,
            ActionVerificationStatus.EVIDENCE_SUGGESTED,
        ),
        requiresMethod = "future_observation",
        requiresMatch = "expectedFromNodeId AND expectedToNodeId must both equal the signal",
        effect = "status becomes evidence_suggested",
        never = listOf("mutates Reality", "auto-verifies", "bumps graphRevision"),
    )

    val guards = listOf(
        Guard(
            id = "VF-FROZEN",
            rule = "evidence suggestion must not override verified, failed or not_required",
            errorCode = "illegal_state_transition",
        ),
    )
}

// ---------------------------------------------------------------------------
// GraphRevision
// ---------------------------------------------------------------------------

object GraphRevisionMachine {

    val initial = 0
    val monotonic = true
    val atomicity = "the Reality mutation and the revision increment MUST happen inside one database transaction on every platform"

    val bumpsOn = listOf(
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
    )

    val neverBumpsOn = listOf(
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
}

data class Guard(val id: String, val rule: String, val errorCode: String)
