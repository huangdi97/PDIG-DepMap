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
class ManualRelationshipR25ContractTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun compactRelationshipReferenceKeepsManualTruthExplicit() {
        val app = createVNextAppState(screen = VScreen.MANUAL_RELATION)
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 360) }
        compose.waitForIdle()

        // 注意：assertIsDisplayed 依赖测试窗口高度（compose rule 用的是设备窗口，
        // 这里只 forcedViewportWidthDp 锁了宽度）。先把节点滚进视口再断言「可见」，
        // 语义不变 —— 仍然要求它真的显示出来，只是不再要求它一开始就在首屏。
        compose.onNodeWithTag("pdig.r25.manual-relationship", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithTag("pdig.r25.relationship.truth-boundary", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("关系是已确认事实，不是标签", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithTag("pdig.r25.relationship.preview-disabled", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun relationshipReferenceKeepsUnknownDistinctFromRequired() {
        val app = createVNextAppState(screen = VScreen.MANUAL_RELATION)
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 700) }
        compose.waitForIdle()

        compose.onNodeWithText(
            "默认 unknown；required 只能由用户显式确认，机器不能设置。",
            useUnmergedTree = true,
        )
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("不会在这里判断“独立备用路径”", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
    }
}
