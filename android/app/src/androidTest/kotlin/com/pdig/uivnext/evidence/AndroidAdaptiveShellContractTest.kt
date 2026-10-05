package com.pdig.uivnext.evidence

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pdig.uivnext.VNextApp
import com.pdig.uivnext.createVNextAppState
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android adaptive shell/craft guard.
 *
 * Keeps navigation and content on the same canonical window-width thresholds and protects focused Phone
 * flows from regressing into a root-level dashboard shell.
 */
@RunWith(AndroidJUnit4::class)
class AndroidAdaptiveShellContractTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun compact599_usesBottomNavigationAndCompactContent() {
        val app = createVNextAppState()
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 599) }
        compose.waitForIdle()

        compose.onNodeWithTag("pdig.breakpoint.compact", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(VTestIds.NAV_BOTTOM, useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(VTestIds.NAV_RAIL, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun medium600_usesRailAndMediumContent() {
        val app = createVNextAppState()
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 600) }
        compose.waitForIdle()

        compose.onNodeWithTag("pdig.breakpoint.medium", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(VTestIds.NAV_RAIL, useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(VTestIds.NAV_BOTTOM, useUnmergedTree = true).assertDoesNotExist()

        app.navigate(VScreen.NUMBERS)
        compose.waitForIdle()
        compose.onNodeWithTag(VTestIds.PHONE_LIST, useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(VTestIds.PHONE_INSPECTOR, useUnmergedTree = true).assertDoesNotExist()

        app.navigate(VScreen.CHANGE_PHONE)
        compose.waitForIdle()
        compose.onNodeWithTag(VTestIds.CHANGE_STEPPER_MINI, useUnmergedTree = true).assertExists()
    }

    @Test
    fun expanded840_usesExpandedContent() {
        val app = createVNextAppState()
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 840) }
        compose.waitForIdle()

        compose.onNodeWithTag("pdig.breakpoint.expanded", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(VTestIds.NAV_RAIL, useUnmergedTree = true).assertExists()
    }

    @Test
    fun compactChildFlow_hidesRootNavigationAndGlobalUtilities() {
        val app = createVNextAppState().apply { openCard("card-cn-2") }
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 360) }
        compose.waitForIdle()

        compose.onNodeWithTag(VTestIds.NAV_BOTTOM, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("pdig.search.entry", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("pdig.settings.entry", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun wideNumbers_clickUpdatesInspectorInsteadOfLeavingSelectionStale() {
        val app = createVNextAppState(screen = VScreen.NUMBERS)
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 840) }
        compose.waitForIdle()

        compose.onNodeWithTag("pdig.phone.inspector.identity.num-cn-1", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("工作副号", useUnmergedTree = true).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("pdig.phone.inspector.identity.num-cn-2", useUnmergedTree = true).assertExists()
    }
}
