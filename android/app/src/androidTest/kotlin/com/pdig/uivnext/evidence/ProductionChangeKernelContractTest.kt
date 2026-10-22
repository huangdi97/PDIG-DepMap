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
import com.pdig.core.schema.migrate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Authoritative Change Kernel invariants that are not scenario-specific.
 *
 * These are the three orderings the product must never violate, and each one is
 * asserted against a real migrated database through [AppContainer] - not against
 * a UI state machine:
 *
 *  1. a step cannot be completed while a prerequisite is unfinished;
 *  2. a step cannot be verified before it has been recorded complete;
 *  3. a terminal plan is history: reanalysis and further mutation are rejected.
 */
@RunWith(AndroidJUnit4::class)
class ProductionChangeKernelContractTest {
    private class Harness(
        val driver: AndroidSqliteDriver,
        val app: AppContainer,
        val dbFile: File,
    ) {
        fun close() {
            driver.close()
            dbFile.delete()
        }
    }

    private fun openContainer(name: String): Harness {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbFile = File(context.filesDir, "$name.db").apply { if (exists()) delete() }
        val driver = AndroidSqliteDriver.open(dbFile, name)
        migrate(driver, "2026-10-19T00:00:00.000Z", targetVersion = LATEST_SCHEMA_VERSION)
        return Harness(driver, AppContainer.forDriver(driver), dbFile)
    }
    @Test
    fun laterStepCannotCompleteWhileAPrerequisiteIsUnfinished() {
        val harness = openContainer("production-change-kernel-prerequisite")
        val app = harness.app
        try {
            val phone = app.createManualIdentityAnchor(
                ManualIdentityAnchorCreateRequest(
                    subtype = IdentityAnchorSubtype.PHONE_NUMBER,
                    name = "旧主号",
                    identifierValue = "+852 6000 0101",
                ),
            ).node
            val account = app.createManualNode(
                ManualNodeCreateRequest(NodeKind.ACCOUNT, "主账户"),
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
            val planId = app.createPlanForScenario(
                ScenarioPlanRequest(scenarioId = "replace_phone_number", targetNodeId = phone.id),
            )

            // rpn-change-1 declares rpn-prepare-1 as its prerequisite.
            var outOfOrderRejected = false
            try {
                app.completeAction(planId, "rpn-change-1")
            } catch (e: IllegalStateException) {
                assertEquals("action_missing_prerequisite", e.message)
                outOfOrderRejected = true
            }
            assertTrue(
                "a step must not be recordable while its prerequisite is unfinished",
                outOfOrderRejected,
            )
            val afterRejection = requireNotNull(app.planDetail(planId))
            assertFalse(afterRejection.actions.first { it.id == "rpn-change-1" }.done)

            // Once the prerequisite is genuinely recorded, the same command succeeds.
            app.completeAction(planId, "rpn-prepare-1")
            app.completeAction(planId, "rpn-change-1")
            val afterOrdered = requireNotNull(app.planDetail(planId))
            assertTrue(afterOrdered.actions.first { it.id == "rpn-prepare-1" }.done)
            assertTrue(afterOrdered.actions.first { it.id == "rpn-change-1" }.done)
            assertEquals(
                "pending",
                afterOrdered.actions.first { it.id == "rpn-change-1" }.verification?.status?.wire,
            )
        } finally {
            harness.close()
        }
    }

    @Test
    fun verificationCannotPrecedeRecordedCompletion() {
        val harness = openContainer("production-change-kernel-verification-order")
        val app = harness.app
        try {
            val phone = app.createManualIdentityAnchor(
                ManualIdentityAnchorCreateRequest(
                    subtype = IdentityAnchorSubtype.PHONE_NUMBER,
                    name = "旧副号",
                    identifierValue = "+852 6000 0102",
                ),
            ).node
            val account = app.createManualNode(
                ManualNodeCreateRequest(NodeKind.ACCOUNT, "副账户"),
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
            val planId = app.createPlanForScenario(
                ScenarioPlanRequest(scenarioId = "replace_phone_number", targetNodeId = phone.id),
            )
            app.completeAction(planId, "rpn-prepare-1")

            // rpn-change-1 is not done yet: done != verified must fail closed.
            var prematureVerificationRejected = false
            try {
                app.verifyAction(planId, "rpn-change-1")
            } catch (e: IllegalArgumentException) {
                assertEquals("not_done", e.message)
                prematureVerificationRejected = true
            }
            assertTrue(
                "verification must not be recordable before the step itself is complete",
                prematureVerificationRejected,
            )
            val unchanged = requireNotNull(app.planDetail(planId))
            assertFalse(unchanged.actions.first { it.id == "rpn-change-1" }.done)
            assertEquals(
                "pending",
                unchanged.actions.first { it.id == "rpn-change-1" }.verification?.status?.wire,
            )
        } finally {
            harness.close()
        }
    }

    @Test
    fun currentPlanReanalysisIsATrueNoOp() {
        val harness = openContainer("production-change-kernel-current-reanalysis")
        val app = harness.app
        try {
            val phone = app.createManualIdentityAnchor(
                ManualIdentityAnchorCreateRequest(
                    subtype = IdentityAnchorSubtype.PHONE_NUMBER,
                    name = "当前主号",
                    identifierValue = "+852 6000 0103",
                ),
            ).node
            val planId = app.createPlanForScenario(
                ScenarioPlanRequest(scenarioId = "replace_phone_number", targetNodeId = phone.id),
            )
            val before = requireNotNull(app.planDetail(planId))
            val sentinelUpdatedAt = "2000-01-01T00:00:00.000Z"
            harness.driver.prepare(
                "UPDATE change_plans SET updated_at = ? WHERE id = ?",
            ).run(sentinelUpdatedAt, planId)

            val after = app.reanalyzePlan(planId)

            assertEquals(before.lastAnalyzedGraphRevision, after.lastAnalyzedGraphRevision)
            assertEquals(before.actions, after.actions)
            assertEquals(
                sentinelUpdatedAt,
                harness.driver.prepare(
                    "SELECT updated_at FROM change_plans WHERE id = ?",
                ).get(planId)?.str("updated_at"),
            )
        } finally {
            harness.close()
        }
    }

    @Test
    fun staleReanalysisPreservesRecordedAndVerifiedActionHistory() {
        val harness = openContainer("production-change-kernel-reanalysis-history")
        val app = harness.app
        try {
            val phone = app.createManualIdentityAnchor(
                ManualIdentityAnchorCreateRequest(
                    subtype = IdentityAnchorSubtype.PHONE_NUMBER,
                    name = "历史主号",
                    identifierValue = "+852 6000 0104",
                ),
            ).node
            val account = app.createManualNode(
                ManualNodeCreateRequest(NodeKind.ACCOUNT, "历史账户"),
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
            val planId = app.createPlanForScenario(
                ScenarioPlanRequest(scenarioId = "replace_phone_number", targetNodeId = phone.id),
            )
            app.completeAction(planId, "rpn-prepare-1")
            app.completeAction(planId, "rpn-change-1")
            app.verifyAction(planId, "rpn-change-1")

            val before = requireNotNull(app.planDetail(planId))
            val beforeMigration = before.actions.first { it.id == "rpn-change-1" }
            assertTrue(beforeMigration.done)
            assertEquals("verified", beforeMigration.verification?.status?.wire)
            val verifiedAt = beforeMigration.verification?.verifiedAt

            // Any governed Reality mutation makes the plan stale. Reanalysis
            // may update impact scope, but it must not erase already-recorded
            // completion or independent verification history.
            app.createManualNode(
                ManualNodeCreateRequest(NodeKind.ACCOUNT, "后来新增但无关的账户"),
            )
            val stale = requireNotNull(app.planDetail(planId))
            assertTrue(stale.currentGraphRevision > stale.lastAnalyzedGraphRevision)

            val after = app.reanalyzePlan(planId)
            val afterMigration = after.actions.first { it.id == "rpn-change-1" }
            assertTrue(afterMigration.done)
            assertEquals("verified", afterMigration.verification?.status?.wire)
            assertEquals(verifiedAt, afterMigration.verification?.verifiedAt)
            assertFalse(after.actions.first { it.id == "rpn-verify-1" }.done)
            assertEquals(after.currentGraphRevision, after.lastAnalyzedGraphRevision)
        } finally {
            harness.close()
        }
    }

    @Test
    fun terminalPlanIsHistoryAndRejectsReanalysisAndMutation() {
        val harness = openContainer("production-change-kernel-terminal")
        val app = harness.app
        try {
            val card = app.createManualNode(
                ManualNodeCreateRequest(NodeKind.PAYMENT_INSTRUMENT, "招行信用卡 4417"),
            ).node
            val wallet = app.createManualNode(
                ManualNodeCreateRequest(NodeKind.ACCOUNT, "微信支付"),
            ).node
            // A second confirmed payment dependency gives the plan two actions.
            // With one action the frozen ChangePlanMachine moves review_required
            // straight to verifying, which it (correctly) refuses; two actions
            // walk the real review_required -> in_progress -> verifying ->
            // completed chain instead of asserting a state the machine forbids.
            val subscription = app.createManualNode(
                ManualNodeCreateRequest(NodeKind.ACCOUNT, "订阅扣款账户"),
            ).node
            app.createManualDependency(
                ManualDependencyCreateRequest(
                    fromNodeId = card.id,
                    relation = Relation.FUNDING_SOURCE,
                    toNodeId = wallet.id,
                    capability = Capability.PAYMENT,
                    required = true,
                ),
            )
            app.createManualDependency(
                ManualDependencyCreateRequest(
                    fromNodeId = card.id,
                    relation = Relation.FUNDING_SOURCE,
                    toNodeId = subscription.id,
                    capability = Capability.PAYMENT,
                    required = true,
                ),
            )
            val planId = app.createPlanForScenario(
                ScenarioPlanRequest(scenarioId = "replace_payment_card", targetNodeId = card.id),
            )
            val created = requireNotNull(app.planDetail(planId))
            assertEquals(
                setOf("${wallet.id}|payment", "${subscription.id}|payment"),
                created.mustChangeKeys.toSet(),
            )
            val actionIds = created.actions.map { it.id }
            assertEquals(2, actionIds.size)

            app.completeAction(planId, actionIds[0])
            assertEquals(
                ChangePlanWorkflowState.IN_PROGRESS,
                requireNotNull(app.planDetail(planId)).workflowState,
            )
            app.completeAction(planId, actionIds[1])
            assertEquals(
                ChangePlanWorkflowState.VERIFYING,
                requireNotNull(app.planDetail(planId)).workflowState,
            )
            app.verifyAction(planId, actionIds[0])
            app.verifyAction(planId, actionIds[1])

            val completed = requireNotNull(app.planDetail(planId))
            assertEquals(ChangePlanWorkflowState.COMPLETED, completed.workflowState)
            val actionId = actionIds[0]

            var reanalysisRejected = false
            try {
                app.reanalyzePlan(planId)
            } catch (e: IllegalArgumentException) {
                // reanalyzePlan guards with require(), so a frozen plan surfaces
                // as IllegalArgumentException("plan_frozen").
                assertEquals("plan_frozen", e.message)
                reanalysisRejected = true
            }
            assertTrue("a completed plan must not be rewritten by reanalysis", reanalysisRejected)

            var completionRejected = false
            try {
                app.completeAction(planId, actionId)
            } catch (e: IllegalStateException) {
                assertEquals("action_frozen", e.message)
                completionRejected = true
            }
            assertTrue("a completed plan must reject further completion", completionRejected)

            var verificationRejected = false
            try {
                app.verifyAction(planId, actionId)
            } catch (e: IllegalStateException) {
                assertEquals("action_frozen", e.message)
                verificationRejected = true
            }
            assertTrue("a completed plan must reject further verification", verificationRejected)

            val afterRejections = requireNotNull(app.planDetail(planId))
            assertEquals(ChangePlanWorkflowState.COMPLETED, afterRejections.workflowState)
            assertEquals(
                completed.lastAnalyzedGraphRevision,
                afterRejections.lastAnalyzedGraphRevision,
            )
        } finally {
            harness.close()
        }
    }
}
