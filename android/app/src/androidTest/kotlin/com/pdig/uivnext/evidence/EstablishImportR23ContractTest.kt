package com.pdig.uivnext.evidence

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
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

        // 顶栏按钮先断言可见（此时仍在首屏），保持原来的 displayed 口径不降级。
        compose.onNodeWithTag("pdig.r10.top.back", useUnmergedTree = true)
            .assertIsDisplayed()
        // assertIsDisplayed 依赖测试窗口高度；先把正文节点滚进视口再断言「可见」，语义不变。
        compose.onNodeWithTag("pdig.r23.import-reference", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithTag("pdig.r23.import.truth-boundary", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("文件留在本机 · 发现不等于依赖", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithTag("pdig.r23.import.preview-disabled", useUnmergedTree = true)
            .assertExists()
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
        // 中宽布局会把「建立基础设施」同时渲染成事实边界标题与主 CTA（>1 个节点）。
        // 契约本意是「这句话在场」，不是「只出现一次」，所以按集合取首个断言存在。
        compose.onAllNodesWithText("建立基础设施", useUnmergedTree = true)
            .onFirst()
            .assertExists()
    }
}
