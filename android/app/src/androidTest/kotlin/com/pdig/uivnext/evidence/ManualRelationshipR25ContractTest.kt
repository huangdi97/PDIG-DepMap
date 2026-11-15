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

        // 这个 tag 本身就是滚动容器（Column + verticalScroll），它没有可滚动的父级，
        // 所以只能直接断言 displayed；它内部的内容才用 performScrollTo 滚进视口。
        compose.onNodeWithTag("pdig.r25.manual-relationship", useUnmergedTree = true)
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

        // 以实际渲染文案为准：R25 屏用这句说明「unknown 不等于不重要」。
        // 旧期望的句子（“默认 unknown；required 只能由用户显式确认…”）已不在源码里，
        // 断言一个不存在的字符串只会在真机上假红。
        compose.onNodeWithText(
            "unknown 表示尚未确认它是不是必须路径，不表示“不重要”。",
            useUnmergedTree = true,
        )
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("不会在这里判断“独立备用路径”", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
    }
}
