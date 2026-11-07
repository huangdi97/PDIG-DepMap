package com.pdig.uivnext.evidence

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pdig.app.data.AppContainer
import com.pdig.app.data.ManualDependencyCreateRequest
import com.pdig.app.data.ManualIdentityAnchorCreateRequest
import com.pdig.app.data.ManualNodeCreateRequest
import com.pdig.app.data.ScenarioPlanRequest
import com.pdig.app.platform.AndroidSqliteDriver
import com.pdig.app.security.DatabaseKeyStore
import com.pdig.core.generated.Capability
import com.pdig.core.generated.IdentityAnchorSubtype
import com.pdig.core.generated.NodeKind
import com.pdig.core.generated.Relation
import com.pdig.core.schema.LATEST_SCHEMA_VERSION
import com.pdig.core.schema.migrate
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant

/**
 * Debug/evidence-only Reality seeding for the post-freeze runtime capture round.
 *
 * This writes into the app's OWN encrypted Personal Reality database through the
 * same [AppContainer] authority the UI uses, then closes it so the app can open
 * the file itself. It is seeded test Reality, never Production user data, and it
 * is only reachable from the instrumentation APK - it is not part of the app's
 * runtime code path and cannot inject fixtures into Production.
 *
 * What it does NOT do: it never completes, verifies or retires a ChangePlan
 * action. Every action in the journey is executed later by tapping the real UI,
 * so "记录完成" and "确认验证" remain authoritative user actions.
 */
@RunWith(AndroidJUnit4::class)
class PostFreezeEvidenceSeedTest {
    private class Harness(
        val driver: AndroidSqliteDriver,
        val app: AppContainer,
        val dbFile: File,
    ) {
        fun close() {
            driver.close()
        }

        fun delete() {
            dbFile.delete()
        }
    }

    private fun freshReality(): Harness {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbFile = File(context.filesDir, "pdig.db")
        dbFile.delete()
        context.deleteDatabase("pdig.db")
        val driver = AndroidSqliteDriver.open(dbFile, DatabaseKeyStore.passphrase(context))
        migrate(driver, Instant.now().toString(), targetVersion = LATEST_SCHEMA_VERSION)
        return Harness(driver, AppContainer.forDriver(driver), dbFile)
    }

    /**
     * Phone identity + two confirmed recovery/authentication dependencies, plus
     * the account / email / device / service identities the Production detail
     * surfaces must render from governed facts only.
     */
    private fun seedIdentities(app: AppContainer): String {
        val phone = app.createManualIdentityAnchor(
            ManualIdentityAnchorCreateRequest(
                subtype = IdentityAnchorSubtype.PHONE_NUMBER,
                name = "旧主号",
                identifierValue = "+852 6123 4567",
            ),
        ).node
        val email = app.createManualIdentityAnchor(
            ManualIdentityAnchorCreateRequest(
                subtype = IdentityAnchorSubtype.EMAIL_ADDRESS,
                name = "主邮箱",
                identifierValue = "work@example.com",
            ),
        ).node
        val account = app.createManualNode(
            ManualNodeCreateRequest(NodeKind.ACCOUNT, "主账户"),
        ).node
        val service = app.createManualNode(
            ManualNodeCreateRequest(NodeKind.SERVICE, "登录服务"),
        ).node
        // Canonical spec restricts runtime-creatable node kinds to
        // payment_instrument / account / service, so a Device node cannot be
        // created by any governed runtime path in this build. The Device surface
        // is therefore captured as its real empty state instead of fabricating one.

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
        // The email identity is seeded WITHOUT a recovery dependency on the same
        // account: an alternative recovery path would legitimately turn the
        // phone's recovery relationship into "needs_review", which correctly keeps
        // the plan in review_required and makes retirement permanently
        // unavailable. This seed therefore models one phone as the only recorded
        // recovery path, which is exactly the Make-Before-Break situation.
        return phone.id
    }

    @Test
    fun seedPhoneMakeBeforeBreakJourney() {
        val harness = freshReality()
        try {
            val phoneId = seedIdentities(harness.app)
            val planId = harness.app.createPlanForScenario(
                ScenarioPlanRequest(scenarioId = "replace_phone_number", targetNodeId = phoneId),
            )
            val detail = requireNotNull(harness.app.planDetail(planId))
            assertTrue(detail.actions.isNotEmpty())
            // No action is completed or verified here: the whole Make-Before-Break
            // chain is executed later through the real UI.
            assertTrue(detail.actions.none { it.done })
            println("SEEDED_PLAN=$planId")
        } finally {
            harness.close()
        }
    }

    /**
     * Same Reality as the journey seed, then one governed required dependency
     * AFTER the plan was analyzed. This both makes the plan stale and adds one
     * authoritative must-change impact, so evidence proves the delta renderer
     * rather than only a graph-revision bump.
     */
    @Test
    fun seedStalePhoneChange() {
        val harness = freshReality()
        try {
            val phoneId = seedIdentities(harness.app)
            val planId = harness.app.createPlanForScenario(
                ScenarioPlanRequest(scenarioId = "replace_phone_number", targetNodeId = phoneId),
            )
            val laterAccount = harness.app.createManualNode(
                ManualNodeCreateRequest(NodeKind.ACCOUNT, "后来新增的账户"),
            ).node
            harness.app.createManualDependency(
                ManualDependencyCreateRequest(
                    fromNodeId = phoneId,
                    relation = Relation.AUTHENTICATES,
                    toNodeId = laterAccount.id,
                    capability = Capability.AUTHENTICATION,
                    required = true,
                ),
            )
            val detail = requireNotNull(harness.app.planDetail(planId))
            assertTrue(detail.currentGraphRevision > detail.lastAnalyzedGraphRevision)
            println("SEEDED_STALE_PLAN=$planId")
        } finally {
            harness.close()
        }
    }
}
