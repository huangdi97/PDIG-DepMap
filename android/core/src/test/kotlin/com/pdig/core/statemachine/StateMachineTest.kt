package com.pdig.core.statemachine

import com.pdig.core.generated.ActionVerificationStatus
import com.pdig.core.generated.CandidateStatus
import com.pdig.core.generated.ChangePlanWorkflowState
import com.pdig.core.generated.DriftStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 状态机合法 / 非法迁移测试。
 *
 * 这些迁移表是运行期真正在用的（被 conformance 与 spec 双向比对），
 * 因此这里测的是"实现 == 声明"，而不是抄一份文档。
 */
class StateMachineTest {

    // ---------------- ChangePlan ----------------

    @Test
    fun changePlanAllowsDocumentedTransitions() {
        assertTrue(ChangePlanMachine.canTransition(ChangePlanWorkflowState.DRAFT, ChangePlanWorkflowState.ANALYZED))
        assertTrue(ChangePlanMachine.canTransition(ChangePlanWorkflowState.ANALYZED, ChangePlanWorkflowState.READY))
        assertTrue(ChangePlanMachine.canTransition(ChangePlanWorkflowState.READY, ChangePlanWorkflowState.IN_PROGRESS))
        assertTrue(ChangePlanMachine.canTransition(ChangePlanWorkflowState.IN_PROGRESS, ChangePlanWorkflowState.VERIFYING))
        assertTrue(ChangePlanMachine.canTransition(ChangePlanWorkflowState.VERIFYING, ChangePlanWorkflowState.COMPLETED))
    }

    @Test
    fun changePlanRejectsIllegalTransitions() {
        assertFalse(
            ChangePlanMachine.canTransition(ChangePlanWorkflowState.DRAFT, ChangePlanWorkflowState.COMPLETED),
            "draft 不得一步跳到 completed",
        )
        assertFalse(ChangePlanMachine.canTransition(ChangePlanWorkflowState.DRAFT, ChangePlanWorkflowState.VERIFYING))
        assertFalse(
            ChangePlanMachine.canTransition(ChangePlanWorkflowState.COMPLETED, ChangePlanWorkflowState.IN_PROGRESS),
            "completed 是终态",
        )
        assertFalse(ChangePlanMachine.canTransition(ChangePlanWorkflowState.CANCELLED, ChangePlanWorkflowState.READY))
    }

    @Test
    fun terminalStatesHaveNoOutgoingEdges() {
        for (s in ChangePlanMachine.terminal) {
            assertEquals(emptyList(), ChangePlanMachine.transitions[s], "$s 必须是终态（无出边）")
        }
        assertTrue(ChangePlanMachine.isFrozen(ChangePlanWorkflowState.COMPLETED))
        assertTrue(ChangePlanMachine.isFrozen(ChangePlanWorkflowState.CANCELLED))
        assertFalse(ChangePlanMachine.isFrozen(ChangePlanWorkflowState.IN_PROGRESS))
    }

    @Test
    fun needsRevalidationIsDerivedAndNeverPersisted() {
        assertEquals("needs_revalidation", ChangePlanMachine.derivedStatus.value)
        assertFalse(ChangePlanMachine.derivedStatus.persisted, "派生状态永不回写存储")
    }

    // ---------------- Drift ----------------

    @Test
    fun driftCanResolveOnlyFromOpen() {
        assertTrue(DriftMachine.transitions[DriftStatus.OPEN]!!.contains(DriftStatus.CONFIRMED_CHANGE))
        assertTrue(DriftMachine.canResolve(DriftStatus.OPEN))
        assertFalse(DriftMachine.canResolve(DriftStatus.DISMISSED))
        assertFalse(DriftMachine.canResolve(DriftStatus.CONFIRMED_CHANGE))
    }

    @Test
    fun driftRequiresPositiveEvidenceAndMinimumObservations() {
        assertEquals("positive evidence only", DriftMachine.creationRule.requires)
        assertEquals(2, DriftMachine.creationRule.minObservations)
        assertEquals("must never create a drift", DriftMachine.creationRule.absenceOnly)
    }

    @Test
    fun driftTerminalStatesHaveNoOutgoingEdges() {
        for (s in listOf(DriftStatus.CONFIRMED_CHANGE, DriftStatus.DISMISSED, DriftStatus.SUPERSEDED)) {
            assertEquals(emptyList(), DriftMachine.transitions[s], "$s 必须是终态")
        }
    }

    // ---------------- Candidate ----------------

    @Test
    fun candidateAcceptCreatesNodeButDoesNotBumpRevision() {
        assertTrue(CandidateMachine.accept.mutatesReality, "accept 会创建对象")
        assertEquals(listOf("create exactly one Node"), CandidateMachine.accept.effects)
        assertFalse(
            CandidateMachine.accept.bumpsGraphRevision,
            "候选对象接受不得 bump graphRevision（spec §26）",
        )
    }

    @Test
    fun candidateTransitions() {
        assertTrue(CandidateMachine.transitions[CandidateStatus.PENDING]!!.contains(CandidateStatus.ACCEPTED))
        assertTrue(CandidateMachine.transitions[CandidateStatus.PENDING]!!.contains(CandidateStatus.DISMISSED))
        assertFalse(
            CandidateMachine.transitions[CandidateStatus.ACCEPTED]!!.contains(CandidateStatus.PENDING),
            "accepted 不得退回 pending",
        )
        assertTrue(CandidateMachine.transitions[CandidateStatus.DISMISSED]!!.contains(CandidateStatus.PENDING))
    }

    @Test
    fun candidateDismissDoesNotMutateReality() {
        assertFalse(CandidateMachine.dismiss.mutatesReality)
        assertEquals("returns to pending only after newObservations >= 2", CandidateMachine.dismiss.reappeal)
    }

    // ---------------- Verification ----------------

    @Test
    fun verificationPendingCanBeVerifiedOrFailed() {
        assertTrue(VerificationMachine.transitions[ActionVerificationStatus.PENDING]!!.contains(ActionVerificationStatus.VERIFIED))
        assertTrue(VerificationMachine.transitions[ActionVerificationStatus.PENDING]!!.contains(ActionVerificationStatus.FAILED))
        assertTrue(
            VerificationMachine.transitions[ActionVerificationStatus.PENDING]!!
                .contains(ActionVerificationStatus.EVIDENCE_SUGGESTED),
        )
    }

    @Test
    fun verifiedIsTerminalAndCannotBeOverridden() {
        assertEquals(emptyList(), VerificationMachine.transitions[ActionVerificationStatus.VERIFIED])
        assertEquals(emptyList(), VerificationMachine.transitions[ActionVerificationStatus.FAILED])
        assertTrue(VerificationMachine.terminal.contains(ActionVerificationStatus.VERIFIED))
    }

    @Test
    fun evidenceSignalNeverAutoVerifiesAndNeverMutatesReality() {
        assertEquals(
            listOf(ActionVerificationStatus.PENDING, ActionVerificationStatus.EVIDENCE_SUGGESTED),
            VerificationMachine.evidenceSignalRule.appliesOnlyTo,
        )
        assertEquals("status becomes evidence_suggested", VerificationMachine.evidenceSignalRule.effect)
        assertTrue(
            VerificationMachine.evidenceSignalRule.never.contains("auto-verifies"),
            "evidence 信号绝不能自动变成 verified",
        )
        assertTrue(VerificationMachine.evidenceSignalRule.never.contains("mutates Reality"))
        assertTrue(VerificationMachine.evidenceSignalRule.never.contains("bumps graphRevision"))
    }

    @Test
    fun notRequiredHasNoTransitions() {
        assertEquals(emptyList(), VerificationMachine.transitions[ActionVerificationStatus.NOT_REQUIRED])
    }
}
