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
        assertEquals(2, snapshot.sources.size)
        assertEquals("账单", snapshot.sources.first { it.id == "src-1" }.label)
        assertEquals("2026-10-01T00:00:00Z", snapshot.sources.first { it.id == "src-1" }.lastIngestedAt)
        assertEquals(null, snapshot.sources.first { it.id == "src-2" }.lastIngestedAt)

        // No generic "stale after N days" verdict is invented here; the adapter
        // carries source facts and leaves freshness policy to governed semantics.

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
    fun governedIdentityProfileActivatesOnlyItsConfirmedProductionSurface() {
        val snapshot = buildProductionSnapshot(
            revision = 11,
            nodes = listOf(
                NodeRow(
                    id = "phone-1",
                    kind = "identity_anchor",
                    name = "+86 138****8823",
                    archived = false,
                    fieldsJson = """{"identity_anchor_profile":{"version":1,"subtype":"phone_number","verification_basis_type":"user_confirmed","confirmed_at":"2026-10-10T00:00:00Z","evidence_refs":["ev-phone"],"identifier":{"value":"+86 138 0000 8823","verification_basis_type":"user_confirmed","confirmed_at":"2026-10-10T02:00:00Z","evidence_refs":["ev-value"]}}}""",
                ),
                NodeRow(
                    id = "email-1",
                    kind = "identity_anchor",
                    name = "m***@example.com",
                    archived = false,
                    fieldsJson = """{"identity_anchor_profile":{"version":1,"subtype":"email_address","verification_basis_type":"authoritative_source","confirmed_at":"2026-10-10T01:00:00Z","evidence_refs":[],"identifier":{"value":"","verification_basis_type":"machine_guess","confirmed_at":""}}}""",
                ),
                NodeRow(
                    id = "bare-legacy",
                    kind = "identity_anchor",
                    name = "+1 415 ...",
                    archived = false,
                    fieldsJson = """{"subtype":"phone_number"}""",
                ),
                NodeRow(
                    id = "invalid-profile",
                    kind = "identity_anchor",
                    name = "looks-like-email@example.com",
                    archived = false,
                    fieldsJson = """{"identity_anchor_profile":{"version":1,"subtype":"sms","verification_basis_type":"user_confirmed","confirmed_at":"2026-10-10T00:00:00Z"}}""",
                ),
            ),
            dependencies = emptyList(),
            timeline = emptyList(),
            plans = emptyList(),
            proposals = emptyList(),
            candidates = emptyList(),
            drifts = emptyList(),
            sources = emptyList(),
        )

        assertEquals(
            VNextProductionSurfaceKind.PHONE_IDENTITY,
            snapshot.objects.first { it.id == "phone-1" }.surfaceKind,
        )
        assertEquals("phone_number", snapshot.objects.first { it.id == "phone-1" }.identitySubtype)
        assertEquals(
            "user_confirmed",
            snapshot.objects.first { it.id == "phone-1" }.identityVerificationBasisType,
        )
        assertEquals(
            listOf("ev-phone"),
            snapshot.objects.first { it.id == "phone-1" }.identityEvidenceRefs,
        )
        assertEquals(
            "+86 138 0000 8823",
            snapshot.objects.first { it.id == "phone-1" }.identityIdentifierValue,
        )
        assertEquals(
            "user_confirmed",
            snapshot.objects.first { it.id == "phone-1" }
                .identityIdentifierVerificationBasisType,
        )
        assertEquals(
            "2026-10-10T02:00:00Z",
            snapshot.objects.first { it.id == "phone-1" }.identityIdentifierConfirmedAt,
        )
        assertEquals(
            listOf("ev-value"),
            snapshot.objects.first { it.id == "phone-1" }.identityIdentifierEvidenceRefs,
        )

        assertEquals(
            VNextProductionSurfaceKind.EMAIL_IDENTITY,
            snapshot.objects.first { it.id == "email-1" }.surfaceKind,
        )
        assertEquals(
            null,
            snapshot.objects.first { it.id == "email-1" }.identityIdentifierValue,
        )
        assertEquals(
            VNextProductionSurfaceKind.IDENTITY_ANCHOR_GENERIC,
            snapshot.objects.first { it.id == "bare-legacy" }.surfaceKind,
        )
        assertEquals(
            VNextProductionSurfaceKind.IDENTITY_ANCHOR_GENERIC,
            snapshot.objects.first { it.id == "invalid-profile" }.surfaceKind,
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
        assertEquals(0, projected.counts.phoneIdentities)
        assertEquals(0, projected.counts.emailIdentities)
        assertEquals(1, projected.counts.genericIdentityAnchors)
        assertEquals("示例银行", projected.paymentAssets.single().issuer)
        assertEquals("8823", projected.paymentAssets.single().last4)
        assertEquals(1, projected.paymentAssets.single().confirmedDependencyCount)
        assertEquals("identity-1", projected.genericIdentityAnchors.single().id)
        assertEquals(6, projected.pendingReviewCount)
        assertEquals(1, projected.activeSourceCount)
    }

    @Test
    fun consumerInventorySeparatesConfirmedPhoneEmailFromGenericIdentity() {
        val snapshot = VNextProductionSnapshot(
            revision = 12,
            objects = listOf(
                VNextProductionObject(
                    id = "phone-1",
                    kind = "identity_anchor",
                    name = "主号",
                    surfaceKind = VNextProductionSurfaceKind.PHONE_IDENTITY,
                    identitySubtype = "phone_number",
                    identityVerificationBasisType = "user_confirmed",
                    identityConfirmedAt = "2026-10-10T00:00:00Z",
                    identityEvidenceRefs = listOf("ev-1"),
                ),
                VNextProductionObject(
                    id = "email-1",
                    kind = "identity_anchor",
                    name = "恢复邮箱",
                    surfaceKind = VNextProductionSurfaceKind.EMAIL_IDENTITY,
                    identitySubtype = "email_address",
                    identityVerificationBasisType = "authoritative_source",
                    identityConfirmedAt = "2026-10-10T01:00:00Z",
                ),
                VNextProductionObject(
                    id = "identity-generic",
                    kind = "identity_anchor",
                    name = "未分类身份",
                    surfaceKind = VNextProductionSurfaceKind.IDENTITY_ANCHOR_GENERIC,
                ),
            ),
            confirmedDependencies = emptyList(),
            timeline = emptyList(),
            plans = emptyList(),
            pendingReview = VNextPendingReviewSummary(0, 0, 0),
            sourceCoverage = VNextSourceCoverageSummary(0, 0),
        )

        val projected = buildProductionConsumerInventory(snapshot)

        assertEquals(1, projected.counts.phoneIdentities)
        assertEquals(1, projected.counts.emailIdentities)
        assertEquals(1, projected.counts.genericIdentityAnchors)
        assertEquals("phone-1", projected.phoneIdentities.single().id)
        assertEquals("user_confirmed", projected.phoneIdentities.single().verificationBasisType)
        assertEquals(1, projected.phoneIdentities.single().evidenceRefCount)
        assertEquals("email-1", projected.emailIdentities.single().id)
        assertEquals("identity-generic", projected.genericIdentityAnchors.single().id)
    }

    @Test
    fun governedMaintenanceFlowsIntoConsumerInventoryAndNowTimeline() {
        val phoneFields = """
            {
              "identity_anchor_profile": {
                "version": 1,
                "subtype": "phone_number",
                "verification_basis_type": "user_confirmed",
                "confirmed_at": "2026-08-07T00:00:00Z"
              },
              "maintenance_profile": {
                "version": 1,
                "facts": [
                  {
                    "id": "cost",
                    "kind": "number_plan_cost",
                    "value_type": "decimal_string",
                    "value": "5",
                    "state": "confirmed",
                    "verification_basis_type": "user_confirmed",
                    "confirmed_at": "2026-08-07T00:00:00Z"
                  },
                  {
                    "id": "currency",
                    "kind": "number_plan_currency",
                    "value_type": "currency_code",
                    "value": "USD",
                    "state": "confirmed",
                    "verification_basis_type": "user_confirmed",
                    "confirmed_at": "2026-08-07T00:00:00Z"
                  }
                ],
                "schedules": [
                  {
                    "id": "keep",
                    "kind": "number_keep_alive",
                    "state": "active",
                    "cadence": {
                      "kind": "interval_days",
                      "interval_days": 90,
                      "anchor_date": "2026-08-07"
                    },
                    "verification_basis_type": "user_confirmed",
                    "confirmed_at": "2026-08-07T00:00:00Z",
                    "last_completed_at": "2026-08-07T00:00:00Z"
                  }
                ]
              }
            }
        """.trimIndent()

        val snapshot = buildProductionSnapshot(
            revision = 21,
            nodes = listOf(
                NodeRow("phone-1", "identity_anchor", "美国保号", false, phoneFields),
            ),
            dependencies = emptyList(),
            timeline = emptyList(),
            plans = emptyList(),
            proposals = emptyList(),
            candidates = emptyList(),
            drifts = emptyList(),
            sources = emptyList(),
            maintenanceTodayIso = "2026-11-10",
        )

        val obj = snapshot.objects.single()
        assertEquals(2, obj.maintenanceFacts.size)
        assertEquals(1, obj.maintenanceSchedules.size)
        assertEquals("overdue", obj.maintenanceOccurrences.single().status)
        assertEquals("2026-11-05", obj.maintenanceOccurrences.single().dueDate)

        val inventory = buildProductionConsumerInventory(snapshot)
        val phone = inventory.phoneIdentities.single()
        assertEquals("5", phone.planCost)
        assertEquals("USD", phone.planCurrency)
        assertEquals("number_keep_alive", phone.keepAliveSchedule?.kind)

        val timelineItem = snapshot.timeline.single()
        assertEquals("number_keep_alive", timelineItem.kind)
        assertEquals("overdue", timelineItem.bucket)
        assertEquals("overdue", timelineItem.status)
        assertEquals("phone-1", timelineItem.actionTarget)
        assertTrue(timelineItem.subtitle.contains("保号"))
    }

    @Test
    fun productionRecordsKeepCompletionVerificationAndEvidenceDistinct() {
        fun planAction(
            id: String,
            done: Boolean,
            verification: String?,
            evidenceRefs: List<String> = emptyList(),
        ) = VNextProductionPlanAction(
            id = id,
            title = id,
            phase = "verify",
            done = done,
            verificationStatus = verification,
            verificationEvidenceRefs = evidenceRefs,
            prerequisiteActionIds = emptyList(),
            resolvesImpactKeys = emptyList(),
        )

        val plan = VNextProductionPlan(
            id = "plan-1",
            scenario = "replace_payment_card",
            title = "更换银行卡",
            workflowState = "verifying",
            effectiveState = "verifying",
            baselineGraphRevision = 3,
            lastAnalyzedGraphRevision = 3,
            currentGraphRevision = 3,
            targetNodeId = "card-1",
            targetNodeName = "主卡",
            effectiveDate = "2026-11-10T00:00:00Z",
            readiness = "ready_with_known_scope",
            affectedServiceCount = 2,
            mustChangeKeys = emptyList(),
            unresolvedMustChangeKeys = emptyList(),
            actions = listOf(
                planAction("done-no-verification", done = true, verification = null),
                planAction("verified", done = true, verification = "verified",
                    evidenceRefs = listOf("ev-1")),
                planAction("pending", done = true, verification = "pending"),
                planAction("failed", done = true, verification = "failed",
                    evidenceRefs = listOf("ev-failed")),
                planAction("future", done = false, verification = null),
            ),
        )

        val records = buildProductionRecordTrace(listOf(plan))
        assertEquals(4, records.size)
        assertEquals(
            VNextProductionRecordState.RECORDED_COMPLETE,
            records.first { it.actionId == "done-no-verification" }.state,
        )
        assertEquals(
            VNextProductionRecordState.VERIFIED,
            records.first { it.actionId == "verified" }.state,
        )
        assertEquals(
            listOf("ev-1"),
            records.first { it.actionId == "verified" }.evidenceRefs,
        )
        assertEquals(
            VNextProductionRecordState.PENDING_VERIFICATION,
            records.first { it.actionId == "pending" }.state,
        )
        assertEquals(
            VNextProductionRecordState.VERIFICATION_FAILED,
            records.first { it.actionId == "failed" }.state,
        )
        assertTrue(records.all { it.occurredAt == null })
        assertFalse(records.any { it.actionId == "future" })
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
                        evidenceRefs = listOf("ev-pending-1"),
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
        assertEquals(listOf("ev-pending-1"), action.verificationEvidenceRefs)
        assertEquals("change", action.phase)
        assertEquals("review_required", mapped.readiness)
        assertEquals(listOf("svc-1|payment"), mapped.unresolvedMustChangeKeys)
    }
}
