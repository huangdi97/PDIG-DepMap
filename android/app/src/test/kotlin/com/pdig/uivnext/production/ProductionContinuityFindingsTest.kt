package com.pdig.uivnext.production

import com.pdig.app.data.PlanDetailView
import com.pdig.app.data.ProposalRow
import com.pdig.core.domain.ActionVerification
import com.pdig.core.domain.ActionVerificationMethod
import com.pdig.core.domain.ActionVerificationStatus
import com.pdig.core.domain.Dependency
import com.pdig.core.domain.ImpactGraph
import com.pdig.core.domain.PlanAction
import com.pdig.core.generated.Capability
import com.pdig.core.generated.ChangePlanWorkflowState
import com.pdig.core.generated.Criticality
import com.pdig.core.generated.PlanActionPhase
import com.pdig.core.generated.PlanReadiness
import com.pdig.core.generated.Relation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionContinuityFindingsTest {
    @Test
    fun singleRecoverySourceCreatesBoundedSpofFinding() {
        val graph = ImpactGraph(
            dependencies = listOf(
                recovery("dep-1", "phone-1", "account-1"),
            ),
            nodeNames = mapOf(
                "phone-1" to "主号",
                "account-1" to "核心账户",
            ),
        )

        val report = buildProductionContinuityFindings(
            graph = graph,
            proposals = emptyList(),
            plans = emptyList(),
        )

        val spof = report.findings.single()
        assertEquals(VNextProductionFindingType.SINGLE_POINT_OF_FAILURE, spof.type)
        assertTrue(spof.title.contains("核心账户"))
        assertTrue(spof.unknowns.contains("未记录"))
        assertEquals(listOf("dep-1"), spof.evidenceRefs)
        assertTrue(report.unsupportedTypes.contains("SHARED_FAILURE_DOMAIN"))
        assertFalse(report.supportedTypes.contains("SHARED_FAILURE_DOMAIN"))
    }

    @Test
    fun confirmedRecoveryCycleUsesCoreCycleEvidence() {
        val graph = ImpactGraph(
            dependencies = listOf(
                recovery("dep-a", "a", "b"),
                recovery("dep-b", "b", "a"),
            ),
            nodeNames = mapOf("a" to "A", "b" to "B"),
        )

        val report = buildProductionContinuityFindings(
            graph = graph,
            proposals = emptyList(),
            plans = emptyList(),
        )

        val cycle = report.findings.first { it.type == VNextProductionFindingType.RECOVERY_CYCLE }
        assertEquals(listOf("dep-a", "dep-b"), cycle.evidenceRefs)
        assertTrue(cycle.title.contains("A"))
        assertTrue(cycle.title.contains("B"))
    }

    @Test
    fun pendingRecoveryProposalStaysPendingReview() {
        val report = buildProductionContinuityFindings(
            graph = ImpactGraph(nodeNames = mapOf("a" to "恢复方式", "b" to "账户")),
            proposals = listOf(
                ProposalRow(
                    id = "proposal-1",
                    key = "k",
                    from = "a",
                    to = "b",
                    relation = "recovers",
                    capability = "recovery",
                    confidence = 0.99,
                    observationCount = 9,
                ),
            ),
            plans = emptyList(),
        )

        val item = report.findings.single()
        assertEquals(VNextProductionFindingType.UNCONFIRMED_FALLBACK, item.type)
        assertEquals(VNextProjectionTruth.PENDING_REVIEW, item.truth)
        assertTrue(item.unknowns.contains("尚未确认"))
    }

    @Test
    fun donePendingVerificationIsFindingButDoneVerifiedIsNot() {
        val report = buildProductionContinuityFindings(
            graph = ImpactGraph(),
            proposals = emptyList(),
            plans = listOf(
                plan(
                    actions = listOf(
                        action("pending", ActionVerificationStatus.PENDING),
                        action("verified", ActionVerificationStatus.VERIFIED),
                    ),
                ),
            ),
        )

        val pending = report.findings.filter {
            it.type == VNextProductionFindingType.PENDING_VERIFICATION
        }
        assertEquals(1, pending.size)
        assertTrue(pending.single().id.endsWith(":pending"))
        assertFalse(report.findings.any { it.id.endsWith(":verified") })
    }

    private fun recovery(id: String, from: String, to: String) = Dependency(
        id = id,
        from = from,
        relation = Relation.RECOVERS,
        to = to,
        capability = Capability.RECOVERY,
        criticality = Criticality.UNKNOWN,
    )

    private fun action(id: String, status: ActionVerificationStatus) = PlanAction(
        id = id,
        title = id,
        phase = PlanActionPhase.VERIFY,
        done = true,
        verification = ActionVerification(
            method = ActionVerificationMethod.MANUAL_CONFIRMATION,
            status = status,
        ),
    )

    private fun plan(actions: List<PlanAction>) = PlanDetailView(
        id = "plan-1",
        scenario = "replace_phone_number",
        title = "更换号码",
        workflowState = ChangePlanWorkflowState.VERIFYING,
        effectiveState = ChangePlanWorkflowState.VERIFYING,
        baselineGraphRevision = 1,
        lastAnalyzedGraphRevision = 1,
        currentGraphRevision = 1,
        targetNodeId = "phone-old",
        targetNodeName = "旧号码",
        effectiveDate = null,
        actions = actions,
        mustChangeKeys = emptyList(),
        unresolvedMustChangeKeys = emptyList(),
        readiness = PlanReadiness.REVIEW_REQUIRED,
        affectedServiceCount = 0,
    )
}
