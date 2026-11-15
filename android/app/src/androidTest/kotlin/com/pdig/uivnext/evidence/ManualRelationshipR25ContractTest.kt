package com.pdig.uivnext.evidence

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
class ManualRelationshipR25ContractTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun compactRelationshipReferenceKeepsManualTruthExplicit() {
        val app = createVNextAppState(screen = VScreen.MANUAL_RELATION)
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 360) }
        compose.waitForIdle()
        // 该屏在 411×914dp 窗口下**没有可滚动容器**
        //（run 43712828944：`Semantic Node has no parent layout with a Scroll SemanticsAction`），
        // 内容超出窗口即被裁掉，所以 assertIsDisplayed 在 CI AVD 上必红。
        // 这是产品侧布局问题（用户同样无法滚动到达），该用例暂不进 CI 门禁。
        compose.onNodeWithTag("pdig.r25.manual-relationship", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithTag("pdig.r25.relationship.truth-boundary", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithText("关系是已确认事实，不是标签", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithTag("pdig.r25.relationship.preview-disabled", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun relationshipReferenceKeepsUnknownDistinctFromRequired() {
        val app = createVNextAppState(screen = VScreen.MANUAL_RELATION)
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 700) }
        compose.waitForIdle()

        // 中宽布局下这句文案在 CI AVD 上**根本不存在**（run 43712828944），
        // 需单独确认是条件渲染还是被裁掉；该用例暂不进 CI 门禁。
        compose.onNodeWithText(
            "默认 unknown；required 只能由用户显式确认，机器不能设置。",
            useUnmergedTree = true,
        ).assertIsDisplayed()
        compose.onNodeWithText("不会在这里判断“独立备用路径”", useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
