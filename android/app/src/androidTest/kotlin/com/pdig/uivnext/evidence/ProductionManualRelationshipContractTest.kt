package com.pdig.uivnext.evidence

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pdig.app.data.AppContainer
import com.pdig.app.data.ManualNodeCreateRequest
import com.pdig.app.platform.AndroidSqliteDriver
import com.pdig.core.generated.NodeKind
import com.pdig.core.schema.migrate
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.production.createProductionVNextSession
import com.pdig.uivnext.ui.ProductionVNextShell
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductionManualRelationshipContractTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun productionFormCommitsCurrentRuntimeRelationThroughAuthority() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(ctx.filesDir, "r31-production-manual-relation-ui.db").apply {
            if (exists()) delete()
        }
        val driver = AndroidSqliteDriver.open(file, "r31-production-manual-relation-ui")
        try {
            migrate(driver, "2026-10-10T00:00:00.000Z")
            val app = AppContainer.forDriver(driver)
            val account = app.createManualNode(
                ManualNodeCreateRequest(NodeKind.ACCOUNT, "主账户"),
            ).node
            val service = app.createManualNode(
                ManualNodeCreateRequest(NodeKind.SERVICE, "视频服务"),
            ).node
            val before = app.graphRevision()
            val session = createProductionVNextSession(
                appContainer = app,
                initialScreen = VScreen.MANUAL_RELATION,
                nowIso = "2026-10-10T00:00:00.000Z",
            )

            compose.setContent {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    ProductionVNextShell(session, forcedViewportWidthDp = 390)
                }
            }
            compose.waitForIdle()

            compose.onNodeWithTag(
                "pdig.production-vnext.manual-relationship",
                useUnmergedTree = true,
            ).assertIsDisplayed()

            compose.onNodeWithTag(
                "pdig.production-vnext.manual-relation.from.${account.id}",
                useUnmergedTree = true,
            ).performClick()
            compose.waitForIdle()

            compose.onNodeWithTag(
                "pdig.production-vnext.manual-relation.relation.merchant_agreement",
                useUnmergedTree = true,
            ).performClick()
            compose.waitForIdle()

            compose.onNodeWithTag(
                "pdig.production-vnext.manual-relation.to.${service.id}",
                useUnmergedTree = true,
            ).performClick()
            compose.waitForIdle()

            compose.onNodeWithTag(
                "pdig.production-vnext.manual-relation.save",
                useUnmergedTree = true,
            ).performClick()
            compose.waitForIdle()

            compose.onNodeWithTag(
                "pdig.production-vnext.manual-relation.result",
                useUnmergedTree = true,
            ).assertIsDisplayed()

            val relation = app.dependencies().single()
            assertEquals(account.id, relation.from)
            assertEquals(service.id, relation.to)
            assertEquals("merchant_agreement", relation.relation)
            assertEquals("payment", relation.capability)
            assertEquals("unknown", relation.criticality)
            assertEquals(before + 1, app.graphRevision())

            val raw = driver.prepare(
                "SELECT origin, verification_basis_type FROM dependencies WHERE id = ?",
            ).get(relation.id)
            assertEquals("manual", raw?.str("origin"))
            assertEquals("user_confirmed", raw?.str("verification_basis_type"))
            assertTrue(app.dependencies().size == 1)
        } finally {
            driver.close()
        }
    }
}
