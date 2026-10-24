package com.pdig.uivnext.production

import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.ui.VAppState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class VNextRuntimeDataSourceTest {
    @Test
    fun referenceModeCannotProduceProductionReality() {
        assertEquals(
            VNextRuntimeDataMode.REFERENCE_PREVIEW,
            ReferenceVNextRuntimeDataSource.mode,
        )
        assertNull(ReferenceVNextRuntimeDataSource.productionSnapshot())
        assertNull(ReferenceVNextRuntimeDataSource.productionInventory())
        assertNull(ReferenceVNextRuntimeDataSource.productionImpact("anything"))
        assertNull(ReferenceVNextRuntimeDataSource.productionPlan("anything"))
        assertTrue(ReferenceVNextRuntimeDataSource.productionRecords().isEmpty())
        assertNull(ReferenceVNextRuntimeDataSource.productionFindings())
    }

    @Test
    fun productionModeProjectsOnlyAuthoritativeSourceData() {
        val source = FakeReadModelSource()
        val runtime = ProductionVNextRuntimeDataSource(source)

        assertEquals(VNextRuntimeDataMode.PRODUCTION_REALITY, runtime.mode)
        assertEquals(17, runtime.productionSnapshot().revision)
        assertEquals(1, runtime.productionInventory().counts.paymentAssets)
        assertEquals("主卡", runtime.productionInventory().paymentAssets.single().name)
        assertEquals("card-1", runtime.productionImpact("card-1").targetNodeId)
        assertEquals("plan-1", runtime.productionPlan("plan-1")?.id)
        assertEquals(1, runtime.productionRecords().size)
        assertEquals(
            listOf("SINGLE_POINT_OF_FAILURE"),
            runtime.productionFindings().supportedTypes,
        )
    }

    @Test
    fun productionSessionRejectsReferenceSource() {
        assertThrows(IllegalArgumentException::class.java) {
            ProductionVNextSession(
                appState = VAppState(initialScreen = VScreen.NOW),
                dataSource = ReferenceVNextRuntimeDataSource,
            )
        }
    }

    private class FakeReadModelSource : VNextReadModelSource {
        private val snapshot = VNextProductionSnapshot(
            revision = 17,
            objects = listOf(
                VNextProductionObject(
                    id = "card-1",
                    kind = "payment_instrument",
                    name = "主卡",
                    surfaceKind = VNextProductionSurfaceKind.PAYMENT_ASSET,
                    issuer = "示例银行",
                    last4 = "8823",
                ),
            ),
            confirmedDependencies = emptyList(),
            timeline = emptyList(),
            plans = listOf(
                VNextProductionPlanSummary(
                    id = "plan-1",
                    title = "更换主卡",
                    scenario = "replace_payment_card",
                    workflowState = "active",
                    lastAnalyzedRevision = 17,
                    effectiveDate = null,
                ),
            ),
            pendingReview = VNextPendingReviewSummary(0, 0, 0),
            sourceCoverage = VNextSourceCoverageSummary(1, 1),
        )

        override fun snapshot(nowIso: String?): VNextProductionSnapshot = snapshot

        override fun impact(targetNodeId: String): VNextProductionImpact =
            VNextProductionImpact(
                targetNodeId = targetNodeId,
                targets = emptyList(),
                checklist = emptyList(),
            )

        override fun plan(planId: String): VNextProductionPlan? =
            if (planId == "plan-1") {
                VNextProductionPlan(
                    id = "plan-1",
                    scenario = "replace_payment_card",
                    title = "更换主卡",
                    workflowState = "active",
                    effectiveState = null,
                    baselineGraphRevision = 17,
                    lastAnalyzedGraphRevision = 17,
                    currentGraphRevision = 17,
                    targetNodeId = "card-1",
                    targetNodeName = "主卡",
                    effectiveDate = null,
                    readiness = "ready",
                    affectedServiceCount = 0,
                    mustChangeKeys = emptyList(),
                    unresolvedMustChangeKeys = emptyList(),
                    actions = emptyList(),
                )
            } else {
                null
            }

        override fun records(): List<VNextProductionRecordItem> =
            listOf(
                VNextProductionRecordItem(
                    id = "record-1",
                    planId = "plan-1",
                    planTitle = "更换主卡",
                    scenario = "replace_payment_card",
                    actionId = "action-1",
                    actionTitle = "核对绑定",
                    phase = "prepare",
                    state = VNextProductionRecordState.PENDING_VERIFICATION,
                    evidenceRefs = emptyList(),
                ),
            )

        override fun findings(): VNextProductionFindingReport =
            VNextProductionFindingReport(
                findings = emptyList(),
                supportedTypes = listOf("SINGLE_POINT_OF_FAILURE"),
                unsupportedTypes = listOf("SHARED_FAILURE_DOMAIN"),
            )
    }
}
