package com.pdig.uivnext.evidence

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pdig.app.data.AppContainer
import com.pdig.app.data.ManualDependencyCreateRequest
import com.pdig.app.data.ManualIdentityAnchorCreateRequest
import com.pdig.app.data.ManualNodeCreateRequest
import com.pdig.app.data.ScenarioPlanRequest
import com.pdig.app.platform.AndroidSqliteDriver
import com.pdig.core.generated.Capability
import com.pdig.core.generated.IdentityAnchorSubtype
import com.pdig.core.generated.NodeKind
import com.pdig.core.generated.Relation
import com.pdig.core.schema.migrate
import com.pdig.uivnext.production.createProductionVNextSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * End-to-end authority contract for replace_phone_number.
 *
 * Uses a real migrated SQLite database and AppContainer. No Preview fixture or
 * Compose-side impact calculation participates.
 */
@RunWith(AndroidJUnit4::class)
class ProductionPhoneChangeAuthorityContractTest {
    @Test
    fun phoneImpactPlanAndReanalysisUseRecordedRuntimeCapabilities() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbFile = File(context.filesDir, "production-phone-change-authority.db")
            .apply { if (exists()) delete() }
        val driver = AndroidSqliteDriver.open(dbFile, "production-phone-change-authority")
        migrate(driver, "2026-10-19T00:00:00.000Z")
        val app = AppContainer.forDriver(driver)

        try {
            val phone = app.createManualIdentityAnchor(
                ManualIdentityAnchorCreateRequest(
                    subtype = IdentityAnchorSubtype.PHONE_NUMBER,
                    name = "香港主号",
                    identifierValue = "+852 6123 4567",
                ),
            ).node
            val account = app.createManualNode(
                ManualNodeCreateRequest(NodeKind.ACCOUNT, "主账户"),
            ).node
            val service = app.createManualNode(
                ManualNodeCreateRequest(NodeKind.SERVICE, "登录服务"),
            ).node

            app.createManualDependency(
                ManualDependencyCreateRequest(
                    fromNodeId = phone.id,
                    relation = Relation.RECOVERS,
                    toNodeId = account.id,
                    capability = Capability.RECOVERY,
                    required = true,
                ),
            )
            app.createManualDependency(
                ManualDependencyCreateRequest(
                    fromNodeId = phone.id,
                    relation = Relation.AUTHENTICATES,
                    toNodeId = service.id,
                    capability = Capability.AUTHENTICATION,
                    required = true,
                ),
            )

            // Production VNext object detail must use the same governed graph.
            val session = createProductionVNextSession(app)
            val impact = requireNotNull(session.dataSource.productionImpact(phone.id))
            assertEquals(
                setOf(
                    "${account.id}|recovery",
                    "${service.id}|authentication",
                ),
                impact.targets.map { "${it.nodeId}|${it.capability}" }.toSet(),
            )
            assertTrue(impact.targets.all { it.status == "must_change" })
            assertTrue(impact.targets.none { it.capability == "payment" })

            val planId = app.createPlanForScenario(
                ScenarioPlanRequest(
                    scenarioId = "replace_phone_number",
                    targetNodeId = phone.id,
                ),
            )
            val plan = requireNotNull(app.planDetail(planId))
            assertEquals(
                setOf(
                    "${account.id}|recovery",
                    "${service.id}|authentication",
                ),
                plan.mustChangeKeys.toSet(),
            )
            assertEquals(plan.mustChangeKeys.toSet(), plan.unresolvedMustChangeKeys.toSet())
            val migration = plan.actions.first { it.id == "rpn-change-1" }
            assertEquals(plan.mustChangeKeys.toSet(), migration.resolvesImpactKeys.toSet())
            assertFalse(migration.done)

            // A later Reality mutation makes the plan stale. Authority itself,
            // not only Compose, must reject a stale completion.
            app.createManualNode(
                ManualNodeCreateRequest(NodeKind.ACCOUNT, "后来新增的账户"),
            )
            var staleRejected = false
            try {
                app.completeAction(planId, "rpn-prepare-1")
            } catch (e: IllegalStateException) {
                assertEquals("plan_stale", e.message)
                staleRejected = true
            }
            assertTrue("stale authoritative mutation must be rejected", staleRejected)

            val refreshed = app.reanalyzePlan(planId)
            assertEquals(refreshed.currentGraphRevision, refreshed.lastAnalyzedGraphRevision)
            assertEquals(plan.mustChangeKeys.toSet(), refreshed.mustChangeKeys.toSet())

            app.completeAction(planId, "rpn-prepare-1")
            val afterPrepare = requireNotNull(app.planDetail(planId))
            assertTrue(afterPrepare.actions.first { it.id == "rpn-prepare-1" }.done)
            assertFalse(afterPrepare.actions.first { it.id == "rpn-change-1" }.done)
        } finally {
            driver.close()
            dbFile.delete()
        }
    }
}
