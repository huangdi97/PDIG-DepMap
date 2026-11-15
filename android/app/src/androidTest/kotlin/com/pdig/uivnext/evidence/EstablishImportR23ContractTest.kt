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

        // 注意：`pdig.r10.top.back` 只在 preview flavor 渲染
        //（VNextShell.kt: `if (BuildConfig.FLAVOR == "preview") R10TopBar(app)`），
        // 而本 job 构建 production flavor，所以这里不再断言顶栏返回键（属过时假设）。
        // 内容断言保留「滚进视口后仍必须 visible」的口径，不降级成 assertExists。
        //
        // `pdig.r23.import-reference` 是滚动容器本身（Column + verticalScroll），
        // 它没有可滚动的父级，只能直接断言 displayed；正文节点才用 performScrollTo。
        compose.onNodeWithTag("pdig.r23.import-reference", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithTag("pdig.r23.import.truth-boundary", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("文件留在本机 · 发现不等于依赖", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithTag("pdig.r23.import.preview-disabled", useUnmergedTree = true)
            .performScrollTo()
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
            .performScrollTo()
            .assertIsDisplayed()
        // 中宽布局会把「建立基础设施」同时渲染成事实边界标题与主 CTA（>1 个节点）。
        // 契约本意是「这句话在场」，不是「只出现一次」，所以按集合取首个断言存在。
        compose.onAllNodesWithText("建立基础设施", useUnmergedTree = true)
            .onFirst()
            .assertExists()
    }
}
