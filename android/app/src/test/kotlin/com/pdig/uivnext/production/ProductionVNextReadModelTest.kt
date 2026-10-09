package com.pdig.uivnext.production

import com.pdig.app.data.CandidateRow
import com.pdig.app.data.DependencyRow
import com.pdig.app.data.DriftRow
import com.pdig.app.data.NodeRow
import com.pdig.app.data.PlanDetailView
import com.pdig.app.data.PlanRow
import com.pdig.app.data.ProposalRow
import com.pdig.app.data.SourceRow
import com.pdig.core.domain.ActionVerification
import com.pdig.core.domain.ActionVerificationMethod
import com.pdig.core.domain.ActionVerificationStatus
import com.pdig.core.domain.PlanAction
import com.pdig.core.generated.ChangePlanWorkflowState
import com.pdig.core.generated.PlanActionPhase
import com.pdig.core.generated.PlanReadiness
import com.pdig.core.timeline.TimelineItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionVNextReadModelTest {

    @Test
    fun snapshotKeepsConfirmedRealitySeparateFromPendingReview() {
        val snapshot = buildProductionSnapshot(
            revision = 7,
            nodes = listOf(
                NodeRow(
                    id = "card-1",
                    kind = "payment_instrument",
                    name = "主卡",
                    archived = false,
                    fieldsJson = "{}",
                    issuer = "示例银行",
                    last4 = "8823",
                ),
                NodeRow("old-1", "account", "已归档", true, "{}"),
            ),
            dependencies = listOf(
                DependencyRow(
                    id = "dep-active",
                    from = "card-1",
                    fromName = "主卡",
                    relation = "funding_source",
                    to = "svc-1",
                    toName = "服务",
                    capability = "payment",
                    criticality = "required",
                    state = "active",
                ),
                DependencyRow(
                    id = "dep-retired",
                    from = "card-1",
                    fromName = "主卡",
                    relation = "funding_source",
                    to = "svc-old",
                    toName = "旧服务",
                    capability = "payment",
                    criticality = "unknown",
                    state = "retired",
                ),
            ),
            timeline = listOf(
                TimelineItem(
                    id = "tl-1",
                    kind = "expiration",
                    title = "主卡即将到期",
                    subtitle = "检查仍依赖此对象的支付路径。",
                    scheduledAt = "2026-11-15T00:00:00Z",
                    bucket = "30d",
                    priority = 2,
                    sourceType = "node_expiry",
                    sourceId = "card-1",
                    actionTarget = "card-1",
                    status = "scheduled",
                ),
            ),
            plans = listOf(
                PlanRow(
                    id = "plan-1",
                    title = "换卡",
                    scenario = "replace_payment_card",
                    workflowState = "in_progress",
                    lastAnalyzedRevision = 7,
                    effectiveDate = "2026-11-10T00:00:00Z",
                ),
            ),
            proposals = listOf(
                ProposalRow(
                    id = "prop-1",
                    key = "k1",
                    from = "card-1",
                    to = "svc-proposed",
                    relation = "funding_source",
                    capability = "payment",
                    confidence = 0.92,
                    observationCount = 3,
                ),
            ),
            candidates = listOf(
                CandidateRow("cand-1", "service", "候选服务", 2, "pending"),
            ),
            drifts = listOf(
                DriftRow(
                    id = "drift-1",
                    kind = "replacement",
                    targetNodeId = "card-1",
                    capability = "payment",
                    candidateFrom = null,
                    candidateRelation = "funding_source",
                    relatedDependencyIds = listOf("dep-active"),
                    observationCount = 2,
                    detectedAt = "2026-10-09T00:00:00Z",
                ),
            ),
            sources = listOf(
                SourceRow("src-1", "账单", "statement", "active", "2026-10-01T00:00:00Z"),
                SourceRow("src-2", "旧来源", "statement", "retired", null),
            ),
        )

        assertEquals(7, snapshot.revision)
        assertEquals(listOf("card-1"), snapshot.objects.map { it.id })
        assertEquals("示例银行", snapshot.objects.single().issuer)
        assertEquals("8823", snapshot.objects.single().last4)
        assertEquals(VNextProductionSurfaceKind.PAYMENT_ASSET, snapshot.objects.single().surfaceKind)
        assertEquals(listOf("dep-active"), snapshot.confirmedDependencies.map { it.id })
        assertTrue(snapshot.confirmedDependencies.all { it.truth == VNextProjectionTruth.CONFIRMED })
        assertEquals(VNextProjectionTruth.DERIVED, snapshot.timeline.single().truth)
        assertEquals(1, snapshot.pendingReview.proposalCount)
        assertEquals(1, snapshot.pendingReview.candidateCount)
        assertEquals(1, snapshot.pendingReview.driftCount)
        assertEquals(2, snapshot.sourceCoverage.sourceCount)
        assertEquals(1, snapshot.sourceCoverage.activeSourceCount)

        // Pending proposals are counted as pending review, never promoted into Reality edges.
        assertFalse(snapshot.confirmedDependencies.any { it.toId == "svc-proposed" })
    }

    @Test
    fun identityAnchorIsNotSilentlyPromotedToPhoneNumber() {
        assertEquals(
            VNextProductionSurfaceKind.IDENTITY_ANCHOR_GENERIC,
            productionSurfaceKind("identity_anchor"),
        )
        assertEquals(
            VNextProductionSurfaceKind.CUSTOM_GENERIC,
            productionSurfaceKind("unknown_future_kind"),
        )
    }

    @Test
    fun consumerInventoryKeepsGenericIdentitySeparateFromPhoneSurface() {
        val snapshot = VNextProductionSnapshot(
            revision = 9,
            objects = listOf(
                VNextProductionObject(
                    id = "card-1",
                    kind = "payment_instrument",
                    name = "主卡",
                    surfaceKind = VNextProductionSurfaceKind.PAYMENT_ASSET,
                    issuer = "示例银行",
                    last4 = "8823",
                ),
                VNextProductionObject(
                    id = "identity-1",
                    kind = "identity_anchor",
                    name = "登录身份",
                    surfaceKind = VNextProductionSurfaceKind.IDENTITY_ANCHOR_GENERIC,
                ),
            ),
            confirmedDependencies = listOf(
                VNextProductionDependency(
                    id = "dep-1",
                    fromId = "card-1",
                    fromName = "主卡",
                    relation = "funding_source",
                    toId = "svc-1",
                    toName = "服务",
                    capability = "payment",
                    criticality = "required",
                ),
            ),
            timeline = emptyList(),
            plans = emptyList(),
            pendingReview = VNextPendingReviewSummary(1, 2, 3),
            sourceCoverage = VNextSourceCoverageSummary(2, 1),
        )

        val projected = buildProductionConsumerInventory(snapshot)

        assertEquals(1, projected.counts.paymentAssets)
        assertEquals(1, projected.counts.genericIdentityAnchors)
        assertEquals("示例银行", projected.paymentAssets.single().issuer)
        assertEquals("8823", projected.paymentAssets.single().last4)
        assertEquals(1, projected.paymentAssets.single().confirmedDependencyCount)
        assertEquals("identity-1", projected.genericIdentityAnchors.single().id)
        assertEquals(6, projected.pendingReviewCount)
        assertEquals(1, projected.activeSourceCount)
    }

    @Test
    fun productionPlanPreservesDoneNotVerified() {
        val detail = PlanDetailView(
            id = "plan-phone",
            scenario = "replace_phone_number",
            title = "更换手机号",
            workflowState = ChangePlanWorkflowState.IN_PROGRESS,
            effectiveState = ChangePlanWorkflowState.IN_PROGRESS,
            baselineGraphRevision = 4,
            lastAnalyzedGraphRevision = 5,
            currentGraphRevision = 5,
            targetNodeId = "phone-old",
            targetNodeName = "旧手机号",
            effectiveDate = null,
            actions = listOf(
                PlanAction(
                    id = "action-1",
                    title = "迁移关键账户",
                    phase = PlanActionPhase.CHANGE,
                    done = true,
                    verification = ActionVerification(
                        method = ActionVerificationMethod.MANUAL_CONFIRMATION,
                        status = ActionVerificationStatus.PENDING,
                    ),
                    prerequisiteActionIds = listOf("prepare-1"),
                ),
            ),
            mustChangeKeys = listOf("svc-1|payment"),
            unresolvedMustChangeKeys = listOf("svc-1|payment"),
            readiness = PlanReadiness.REVIEW_REQUIRED,
            affectedServiceCount = 1,
        )

        val mapped = mapProductionPlan(detail)
        val action = mapped.actions.single()

        assertTrue(action.done)
        assertEquals("pending", action.verificationStatus)
        assertEquals("change", action.phase)
        assertEquals("review_required", mapped.readiness)
        assertEquals(listOf("svc-1|payment"), mapped.unresolvedMustChangeKeys)
    }
}
