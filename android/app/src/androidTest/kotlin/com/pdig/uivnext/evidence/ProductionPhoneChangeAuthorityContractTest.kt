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
import com.pdig.core.generated.ChangePlanWorkflowState
import com.pdig.core.generated.IdentityAnchorSubtype
import com.pdig.core.generated.NodeKind
import com.pdig.core.generated.Relation
import com.pdig.core.schema.LATEST_SCHEMA_VERSION
import com.pdig.core.schema.getSchemaVersion
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
    fun newlyDiscoveredUnboundImpactBlocksOldPhoneRetirementAtAuthorityBoundary() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbFile = File(context.filesDir, "production-phone-retirement-unbound.db")
            .apply { if (exists()) delete() }
        val driver = AndroidSqliteDriver.open(dbFile, "production-phone-retirement-unbound")
        migrate(
            driver,
            "2026-10-19T00:00:00.000Z",
            targetVersion = LATEST_SCHEMA_VERSION,
        )
        val app = AppContainer.forDriver(driver)

        try {
            val phone = app.createManualIdentityAnchor(
                ManualIdentityAnchorCreateRequest(
                    subtype = IdentityAnchorSubtype.PHONE_NUMBER,
                    name = "旧主号",
                    identifierValue = "+852 6000 0001",
                ),
            ).node
            val accountA = app.createManualNode(
                ManualNodeCreateRequest(NodeKind.ACCOUNT, "账户 A"),
            ).node
            app.createManualDependency(
                ManualDependencyCreateRequest(
                    fromNodeId = phone.id,
                    relation = Relation.RECOVERS,
                    toNodeId = accountA.id,
                    capability = Capability.RECOVERY,
                    required = true,
                ),
            )

            val planId = app.createPlanForScenario(
                ScenarioPlanRequest(
                    scenarioId = "replace_phone_number",
                    targetNodeId = phone.id,
                ),
            )
            app.completeAction(planId, "rpn-prepare-1")
            app.completeAction(planId, "rpn-change-1")

            // New Reality appears after the migration step was recorded done.
            // Reanalysis must keep it unresolved, not bind it to old-path retirement.
            val accountB = app.createManualNode(
                ManualNodeCreateRequest(NodeKind.ACCOUNT, "账户 B"),
            ).node
            app.createManualDependency(
                ManualDependencyCreateRequest(
                    fromNodeId = phone.id,
                    relation = Relation.AUTHENTICATES,
                    toNodeId = accountB.id,
                    capability = Capability.AUTHENTICATION,
                    required = true,
                ),
            )
            val refreshed = app.reanalyzePlan(planId)
            val newKey = "${accountB.id}|authentication"
            assertFalse(refreshed.retirementScopeReady == true)
            assertTrue(newKey in refreshed.unresolvedMustChangeKeys)
            assertFalse(
                refreshed.actions.first { it.id == "rpn-change-2" }
                    .resolvesImpactKeys.contains(newKey),
            )

            app.completeAction(planId, "rpn-verify-1")
            app.verifyAction(planId, "rpn-verify-1")

            var retirementRejected = false
            try {
                app.completeAction(planId, "rpn-change-2")
            } catch (e: IllegalStateException) {
                assertEquals("unresolved_must_change", e.message)
                retirementRejected = true
            }
            assertTrue(
                "authority must never retire the old phone while a newly discovered impact is unresolved",
                retirementRejected,
            )
            assertFalse(
                requireNotNull(app.planDetail(planId))
                    .actions.first { it.id == "rpn-change-2" }.done,
            )
        } finally {
            driver.close()
            dbFile.delete()
        }
    }

    @Test
    fun phoneRetirementRejectsReviewRequiredScopeEvenAfterMustChangeIsResolved() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbFile = File(context.filesDir, "production-phone-retirement-review.db")
            .apply { if (exists()) delete() }
        val driver = AndroidSqliteDriver.open(dbFile, "production-phone-retirement-review")
        migrate(
            driver,
            "2026-10-19T00:00:00.000Z",
            targetVersion = LATEST_SCHEMA_VERSION,
        )
        val app = AppContainer.forDriver(driver)

        try {
            val phone = app.createManualIdentityAnchor(
                ManualIdentityAnchorCreateRequest(
                    subtype = IdentityAnchorSubtype.PHONE_NUMBER,
                    name = "旧主号",
                    identifierValue = "+852 6000 0002",
                ),
            ).node
            val requiredAccount = app.createManualNode(
                ManualNodeCreateRequest(NodeKind.ACCOUNT, "必须迁移账户"),
            ).node
            val reviewAccount = app.createManualNode(
                ManualNodeCreateRequest(NodeKind.ACCOUNT, "待核对账户"),
            ).node
            app.createManualDependency(
                ManualDependencyCreateRequest(
                    fromNodeId = phone.id,
                    relation = Relation.RECOVERS,
                    toNodeId = requiredAccount.id,
                    capability = Capability.RECOVERY,
                    required = true,
                ),
            )
            app.createManualDependency(
                ManualDependencyCreateRequest(
                    fromNodeId = phone.id,
                    relation = Relation.AUTHENTICATES,
                    toNodeId = reviewAccount.id,
                    capability = Capability.AUTHENTICATION,
                    required = false,
                ),
            )

            val planId = app.createPlanForScenario(
                ScenarioPlanRequest(
                    scenarioId = "replace_phone_number",
                    targetNodeId = phone.id,
                ),
            )
            val initial = requireNotNull(app.planDetail(planId))
            assertTrue(initial.mustChangeKeys.isNotEmpty())
            assertTrue(initial.needsReviewKeys.isNotEmpty())

            app.completeAction(planId, "rpn-prepare-1")
            app.completeAction(planId, "rpn-change-1")
            app.completeAction(planId, "rpn-verify-1")
            app.verifyAction(planId, "rpn-verify-1")

            // must-change work has been recorded done, but the unknown/review
            // scope remains. Destructive retirement must still fail closed.
            val beforeRetirement = requireNotNull(app.planDetail(planId))
            assertTrue(beforeRetirement.unresolvedMustChangeKeys.isEmpty())
            assertFalse(beforeRetirement.retirementScopeReady == true)
            assertEquals(
                com.pdig.core.generated.PlanReadiness.REVIEW_REQUIRED,
                beforeRetirement.readiness,
            )

            var rejected = false
            try {
                app.completeAction(planId, "rpn-change-2")
            } catch (e: IllegalStateException) {
                assertEquals("retirement_scope_not_ready", e.message)
                rejected = true
            }
            assertTrue("review-required scope must block destructive retirement", rejected)
            assertFalse(
                requireNotNull(app.planDetail(planId))
                    .actions.first { it.id == "rpn-change-2" }.done,
            )
        } finally {
            driver.close()
            dbFile.delete()
        }
    }

    @Test
    fun phoneImpactPlanAndReanalysisUseRecordedRuntimeCapabilities() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbFile = File(context.filesDir, "production-phone-change-authority.db")
            .apply { if (exists()) delete() }
        val driver = AndroidSqliteDriver.open(dbFile, "production-phone-change-authority")
        migrate(
            driver,
            "2026-10-19T00:00:00.000Z",
            targetVersion = LATEST_SCHEMA_VERSION,
        )
        assertEquals(
            LATEST_SCHEMA_VERSION,
            getSchemaVersion(driver),
        )
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
            app.completeAction(planId, "rpn-change-1")
            app.completeAction(planId, "rpn-verify-1")

            val beforeVerifiedRetirement = requireNotNull(app.planDetail(planId))
            assertTrue(beforeVerifiedRetirement.actions.first { it.id == "rpn-verify-1" }.done)
            assertFalse(beforeVerifiedRetirement.retirementScopeReady == true)
            assertEquals(
                "pending",
                beforeVerifiedRetirement.actions.first { it.id == "rpn-verify-1" }
                    .verification?.status?.wire,
            )
            assertEquals(
                "pending",
                beforeVerifiedRetirement.actions.first { it.id == "rpn-change-1" }
                    .verification?.status?.wire,
            )

            var retirementRejected = false
            try {
                app.completeAction(planId, "rpn-change-2")
            } catch (e: IllegalStateException) {
                assertEquals("verification_prerequisite_not_met", e.message)
                retirementRejected = true
            }
            assertTrue(
                "old phone retirement must wait for all new-path evidence to be verified",
                retirementRejected,
            )

            // The dedicated recovery-path check alone is still insufficient:
            // migration itself has an independent done != verified boundary.
            app.verifyAction(planId, "rpn-verify-1")
            val recoveryVerifiedOnly = requireNotNull(app.planDetail(planId))
            assertFalse(recoveryVerifiedOnly.retirementScopeReady == true)

            var migrationVerificationRejected = false
            try {
                app.completeAction(planId, "rpn-change-2")
            } catch (e: IllegalStateException) {
                assertEquals("verification_prerequisite_not_met", e.message)
                migrationVerificationRejected = true
            }
            assertTrue(
                "old phone retirement must also wait for the migration action verification",
                migrationVerificationRejected,
            )

            app.verifyAction(planId, "rpn-change-1")
            val allNewPathsVerified = requireNotNull(app.planDetail(planId))
            assertTrue(allNewPathsVerified.retirementScopeReady == true)

            app.completeAction(planId, "rpn-change-2")

            val afterRetirementGate = requireNotNull(app.planDetail(planId))
            assertTrue(afterRetirementGate.actions.first { it.id == "rpn-prepare-1" }.done)
            assertTrue(afterRetirementGate.actions.first { it.id == "rpn-change-1" }.done)
            assertEquals(
                "verified",
                afterRetirementGate.actions.first { it.id == "rpn-change-1" }
                    .verification?.status?.wire,
            )
            assertEquals(
                "verified",
                afterRetirementGate.actions.first { it.id == "rpn-verify-1" }
                    .verification?.status?.wire,
            )
            assertTrue(afterRetirementGate.actions.first { it.id == "rpn-change-2" }.done)
            assertEquals(
                ChangePlanWorkflowState.COMPLETED,
                afterRetirementGate.workflowState,
            )
        } finally {
            driver.close()
            dbFile.delete()
        }
    }
}
