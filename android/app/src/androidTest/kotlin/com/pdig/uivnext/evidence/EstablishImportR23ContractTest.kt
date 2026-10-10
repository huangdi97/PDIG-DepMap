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
class EstablishImportR23ContractTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun compactImportReferenceExplainsLocalFirstAndProposalBoundary() {
        val app = createVNextAppState(screen = VScreen.IMPORT)
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 360) }
        compose.waitForIdle()

        compose.onNodeWithTag("pdig.r23.import-reference", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithTag("pdig.r23.import.truth-boundary", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithText("文件留在本机 · 发现不等于依赖", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithTag("pdig.r23.import.preview-disabled", useUnmergedTree = true)
            .assertExists()
        compose.onNodeWithTag("pdig.r10.top.back", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun mediumImportReferenceUsesTheSameTruthGrammar() {
        val app = createVNextAppState(screen = VScreen.IMPORT)
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 700) }
        compose.waitForIdle()

        compose.onNodeWithTag("pdig.r23.import-reference", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithText("当前是隔离预览", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithText("建立基础设施", useUnmergedTree = true)
            .assertExists()
    }
}
