package com.pdig.uivnext.evidence

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pdig.uivnext.VNextApp
import com.pdig.uivnext.createVNextAppState
import com.pdig.uivnext.model.VScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HumanReviewR22ContractTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun compactReviewInbox_keepsDiscoveryOutsideReality() {
        val app = createVNextAppState(screen = VScreen.REVIEW)
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 360) }
        compose.waitForIdle()

        compose.onNodeWithTag("pdig.r22.review-inbox", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithTag("pdig.r22.review.truth-boundary", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithText("发现 ≠ 事实", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag(
            "pdig.r22.review.item.review-proposal-notion-revolut",
            useUnmergedTree = true,
        ).assertExists()
        compose.onNodeWithTag(
            "pdig.r22.review.item.review-candidate-adobe",
            useUnmergedTree = true,
        ).assertExists()
        compose.onNodeWithTag(
            "pdig.r22.review.item.review-drift-netflix",
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun reviewIsFocusedSecondaryRouteWithHierarchicalUp() {
        val app = createVNextAppState(screen = VScreen.REVIEW)
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 360) }
        compose.waitForIdle()

        compose.onNodeWithTag("pdig.r10.top.back", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("pdig.r22.review.summary", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun mediumReviewUsesSameTruthGrammar() {
        val app = createVNextAppState(screen = VScreen.REVIEW)
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 700) }
        compose.waitForIdle()

        compose.onNodeWithTag("pdig.r22.review-inbox", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithText("Preview 不执行", useUnmergedTree = true).assertIsDisplayed()
    }
}
