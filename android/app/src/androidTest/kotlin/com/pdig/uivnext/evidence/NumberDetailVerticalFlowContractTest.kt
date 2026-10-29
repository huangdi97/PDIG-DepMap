package com.pdig.uivnext.evidence

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pdig.uivnext.VNextApp
import com.pdig.uivnext.createVNextAppState
import com.pdig.uivnext.model.VTestIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * B5 NumberDetailVerticalFlowContractTest（brief §8/§21）——
 * Identity Hero → Summary → 关联服务 必须连续；Summary 是正式信息层，不应被旧契约误判为死空白。
 */
@RunWith(AndroidJUnit4::class)
class NumberDetailVerticalFlowContractTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun numberDetailVerticalFlowHasNoDeadSpace() {
        val app = createVNextAppState().apply { openNumber("num-cn-1") }
        compose.setContent { VNextApp(app) }
        compose.waitForIdle()

        val hero = compose.onNodeWithTag(VTestIds.NUMBER_DETAIL_HERO, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val summary = compose.onNodeWithTag("pdig.number.detail.summary", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val services = compose.onNodeWithTag(VTestIds.NUMBER_DETAIL_SERVICES, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val heroToSummary = summary.top - hero.bottom
        val summaryToServices = services.top - summary.bottom
        val density = compose.density
        val threshold = with(density) { 32.dp.toPx() }
        assertTrue(
            "hero→summary gap must stay within 32dp (gap=$heroToSummary, threshold=$threshold)",
            heroToSummary <= threshold,
        )
        assertTrue(
            "summary→services gap must stay within 32dp (gap=$summaryToServices, threshold=$threshold)",
            summaryToServices <= threshold,
        )
        assertTrue("section order must be hero → summary → services", hero.bottom <= summary.top && summary.bottom <= services.top)
    }
    @Test
    fun numberAliasEditorRoundTripChangesIdentityFaceWithoutChangingRecordedNumber() {
        val app = createVNextAppState().apply { openNumber("num-cn-1") }
        val original = app.numberAlias("num-cn-1")
        compose.setContent { VNextApp(app) }
        compose.waitForIdle()

        compose.onNodeWithText("修改名称 →").performClick()
        val input = compose.onNodeWithTag("pdig.r10.number.alias.input")
        input.assertExists()
        input.performTextInput("HK-Main")
        // Require actual Compose editor state before pressing save; merely
        // dispatching a shell input event is not acceptance evidence.
        input.assertTextContains("HK-Main")
        compose.onNodeWithText("保存名称").performClick()
        compose.waitForIdle()

        assertEquals("HK-Main", app.numberAlias("num-cn-1"))
        assertEquals("HK-Main", app.numberDisplayNameForScreen("num-cn-1", "+86 138****8823"))
        assertTrue(
            "Saved local alias must be shown on the real Number Detail screen",
            compose.onAllNodesWithText("HK-Main", substring = true).fetchSemanticsNodes().isNotEmpty(),
        )
        // No fixture or canonical number is changed by a visual alias edit.
        assertEquals("+86 138****8823", com.pdig.uivnext.demo.UiVNextDemoFixture.numberById("num-cn-1")?.maskedNumber)

        // Cleanup stays local to this test-state instance.
        if (original.isBlank()) app.renameNumber("num-cn-1", "")
        else app.renameNumber("num-cn-1", original)
    }

}