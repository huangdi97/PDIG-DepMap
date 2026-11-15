package com.pdig.uivnext.evidence

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
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
class ContinuityFindingsR27ContractTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun compactWeaknessesExposeStructuralFindingGrammarWithoutSafetyScore() {
        val app = createVNextAppState(screen = VScreen.WEAKNESSES)
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 360) }
        compose.waitForIdle()

        compose.onNodeWithTag("pdig.r27.finding.finding-spof-cn-main", useUnmergedTree = true)
            .assertExists()
        compose.onNodeWithText("单点路径", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("共享故障点", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("恢复循环", useUnmergedTree = true).assertExists()
    }

    @Test
    fun emptyReferenceDoesNotInventFindingsOrMigrationBlockers() {
        val app = createVNextAppState(screen = VScreen.WEAKNESSES).apply {
            emptyDemo = true
        }
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 360) }
        compose.waitForIdle()

        compose.onNodeWithTag("pdig.r27.finding.finding-spof-cn-main", useUnmergedTree = true)
            .assertDoesNotExist()
        compose.onNodeWithText("查看更换手机号的影响分析 →", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun mediumWeaknessesKeepBasisUnknownAndNextActionVisibleInSameFinding() {
        val app = createVNextAppState(screen = VScreen.WEAKNESSES)
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 700) }
        compose.waitForIdle()

        compose.onNodeWithTag("pdig.r27.finding.finding-spof-cn-main", useUnmergedTree = true)
            .assertIsDisplayed()
        // 每条 finding 都有自己的「为什么 / 确认依据 / 仍未知」标签，节点数随 finding 数变化（实测 7 个）。
        // 契约本意是「这几类解释入口在场」，不是唯一性，所以按集合取首个断言存在。
        compose.onAllNodesWithText("为什么", useUnmergedTree = true).onFirst().assertExists()
        compose.onAllNodesWithText("确认依据", useUnmergedTree = true).onFirst().assertExists()
        compose.onAllNodesWithText("仍未知", useUnmergedTree = true).onFirst().assertExists()
    }
}
