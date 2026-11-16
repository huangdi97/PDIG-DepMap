package com.pdig.uivnext.evidence

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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
class ManualEstablishR24ContractTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun compactManualRecordExplainsRealityAndRelationBoundary() {
        val app = createVNextAppState(screen = VScreen.MANUAL_ADD)
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 360) }
        compose.waitForIdle()

        // `pdig.r24.manual-establish` 是滚动容器本身（Column + verticalScroll），
        // 它没有可滚动的父级，只能直接断言 displayed；内容节点才用 performScrollTo。
        compose.onNodeWithTag("pdig.r24.manual-establish", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithTag("pdig.r24.manual.truth-boundary", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText(
            "你确认的对象会进入正式数据；关系仍要单独确认",
            useUnmergedTree = true,
        )
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithTag("pdig.r24.manual.preview-disabled", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun mediumManualRecordShowsGovernedIdentityAuthorityButKeepsPreviewReadOnly() {
        val app = createVNextAppState(screen = VScreen.MANUAL_ADD)
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 700) }
        compose.waitForIdle()

        // 顶栏返回键（`pdig.r10.top.back`）只在 preview flavor 渲染
        //（VNextShell.kt: `if (BuildConfig.FLAVOR == "preview") R10TopBar(app)`），
        // production 构建里不存在，因此这里不再断言它。
        compose.onNodeWithText("手机号 / 邮箱", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText(
            "可正式记录 · Preview 只读",
            useUnmergedTree = true,
        )
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("其他身份", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("当前 Preview 不提供“保存”按钮", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
    }
}
