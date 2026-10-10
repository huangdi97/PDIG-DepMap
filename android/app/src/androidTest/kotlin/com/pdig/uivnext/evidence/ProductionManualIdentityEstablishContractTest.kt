package com.pdig.uivnext.evidence

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pdig.app.data.AppContainer
import com.pdig.app.platform.AndroidSqliteDriver
import com.pdig.core.domain.confirmedIdentityAnchorProfile
import com.pdig.core.generated.IdentityAnchorSubtype
import com.pdig.core.generated.NodeKind
import com.pdig.core.schema.migrate
import com.pdig.uivnext.production.createProductionVNextSession
import com.pdig.uivnext.ui.ProductionManualEstablishScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * R38 end-to-end source/runtime contract for the production Manual Establish form.
 *
 * The test uses a real migrated SQLite database and authoritative AppContainer;
 * no Preview fixture participates in the save path.
 */
@RunWith(AndroidJUnit4::class)
class ProductionManualIdentityEstablishContractTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun phoneFormCreatesGovernedSubtypeAndIdentifierWithoutDependency() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbFile = File(context.filesDir, "r38-production-manual-phone-ui.db")
            .apply { if (exists()) delete() }
        val driver = AndroidSqliteDriver.open(dbFile, "r38-production-manual-phone-ui")
        migrate(driver, "2026-10-10T00:00:00.000Z")
        val app = AppContainer.forDriver(driver)
        val session = createProductionVNextSession(app)

        try {
            compose.setContent {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    ProductionManualEstablishScreen(session)
                }
            }
            compose.waitForIdle()

            compose.onNodeWithTag(
                "pdig.production-vnext.manual.kind.phone_number",
                useUnmergedTree = true,
            ).performClick()
            compose.onNodeWithTag(
                "pdig.production-vnext.manual.name",
                useUnmergedTree = true,
            ).performTextInput("香港主号")
            compose.onNodeWithTag(
                "pdig.production-vnext.manual.identity.value",
                useUnmergedTree = true,
            ).performTextInput("+852 6123 4567")

            compose.onNodeWithTag(
                "pdig.production-vnext.manual.save",
                useUnmergedTree = true,
            ).performClick()
            compose.waitForIdle()

            compose.onNodeWithText("已写入已确认数据").assertIsDisplayed()
            compose.onNodeWithText("手机号身份").assertIsDisplayed()
            compose.onNodeWithText("+852 6123 4567").assertIsDisplayed()

            val node = app.nodes().single()
            assertEquals("identity_anchor", node.kind)
            assertEquals("香港主号", node.name)
            val profile = confirmedIdentityAnchorProfile(
                NodeKind.IDENTITY_ANCHOR,
                node.fieldsJson,
            )
            requireNotNull(profile)
            assertEquals(IdentityAnchorSubtype.PHONE_NUMBER, profile.subtype)
            assertEquals("+852 6123 4567", profile.identifier?.value)
            assertEquals(1, app.graphRevision())
            assertTrue(app.dependencies().isEmpty())
        } finally {
            driver.close()
        }
    }

    @Test
    fun emailFormPersistsExactConfirmedValueButMasksResultPresentation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbFile = File(context.filesDir, "r38-production-manual-email-ui.db")
            .apply { if (exists()) delete() }
        val driver = AndroidSqliteDriver.open(dbFile, "r38-production-manual-email-ui")
        migrate(driver, "2026-10-10T00:00:00.000Z")
        val app = AppContainer.forDriver(driver)
        val session = createProductionVNextSession(app).also {
            it.appState.privacyMask = true
        }

        try {
            compose.setContent {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    ProductionManualEstablishScreen(session)
                }
            }
            compose.waitForIdle()

            compose.onNodeWithTag(
                "pdig.production-vnext.manual.kind.email_address",
                useUnmergedTree = true,
            ).performClick()
            compose.onNodeWithTag(
                "pdig.production-vnext.manual.name",
                useUnmergedTree = true,
            ).performTextInput("恢复邮箱")
            compose.onNodeWithTag(
                "pdig.production-vnext.manual.identity.value",
                useUnmergedTree = true,
            ).performTextInput("user@example.com")
            compose.onNodeWithTag(
                "pdig.production-vnext.manual.save",
                useUnmergedTree = true,
            ).performClick()
            compose.waitForIdle()

            compose.onNodeWithText("邮箱身份").assertIsDisplayed()
            compose.onNodeWithText("标识值已遮蔽").assertIsDisplayed()
            compose.onNodeWithText("user@example.com").assertDoesNotExist()

            val node = app.nodes().single()
            val profile = confirmedIdentityAnchorProfile(
                NodeKind.IDENTITY_ANCHOR,
                node.fieldsJson,
            )
            requireNotNull(profile)
            assertEquals(IdentityAnchorSubtype.EMAIL_ADDRESS, profile.subtype)
            assertEquals("user@example.com", profile.identifier?.value)
            assertEquals(1, app.graphRevision())
            assertTrue(app.dependencies().isEmpty())
        } finally {
            driver.close()
        }
    }
}
