package com.pdig.uivnext.evidence

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pdig.uivnext.VNextApp
import com.pdig.uivnext.createVNextAppState
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Human-selected Android Light Visual Reference 的结构防回退契约。
 *
 * 这不是像素验收，也不把生成参考图当 functional truth；只守住已经落地的 presentation
 * hierarchy，避免后续实现重新退回深色 dashboard / generic rows / form-like migration。
 */
@RunWith(AndroidJUnit4::class)
class AndroidLightVisualSourceContractTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun compactRoot_usesStablePdigBrandLayer() {
        val app = createVNextAppState(screen = VScreen.CARDS)
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 360) }
        compose.waitForIdle()

        compose.onNodeWithText("PDIG", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(VTestIds.CARD_LIST, useUnmergedTree = true).assertExists()
    }

    @Test
    fun compactNumbers_areCommunicationIdentityAssets() {
        val app = createVNextAppState(screen = VScreen.NUMBERS)
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 360) }
        compose.waitForIdle()

        val thumbnails = compose
            .onAllNodesWithTag("pdig.number.identity.thumbnail", useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue("Numbers must render communication-identity thumbnails", thumbnails.isNotEmpty())
    }

    @Test
    fun compactChangePhone_isContinuityScene_notThreeIndependentForms() {
        val app = createVNextAppState(screen = VScreen.CHANGE_PHONE).apply {
            changeProjection = "transition"
        }
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 360) }
        compose.waitForIdle()

        compose.onNodeWithTag(VTestIds.CHANGE_OLD, useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(VTestIds.CHANGE_SERVICES, useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(VTestIds.CHANGE_NEW, useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(VTestIds.CHANGE_STEPPER_MINI, useUnmergedTree = true).assertExists()
    }

    @Test
    fun personalization_declaresLightThemeAsCurrent() {
        val app = createVNextAppState(screen = VScreen.PERSONALIZATION)
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 360) }
        compose.waitForIdle()

        compose.onNodeWithText("亮色", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("当前", useUnmergedTree = true).assertExists()
    }
}
