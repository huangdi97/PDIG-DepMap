package com.pdig.uivnext.evidence

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pdig.uivnext.VNextApp
import com.pdig.uivnext.createVNextAppState
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * vNext 无障碍基线（任务书 §30）：contentDescription / semantic roles / selected state /
 * 触控目标（≥48dp）/ Globe 非视觉替代（Region List）。
 */
@RunWith(AndroidJUnit4::class)
class VNextAccessibilityEvidenceTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun bottomNavAndSearchHaveAccessibleSemantics() {
        val app = createVNextAppState().apply { reduceMotion = true }
        compose.setContent { VNextApp(app) }
        compose.waitForIdle()

        // 一级导航 4 项：label 文本（merged tree 内）存在
        listOf("现在", "基础设施", "变更", "记录").forEach { label ->
            compose.onAllNodesWithText(label, substring = false)[0].assertExists()
        }
        // 搜索入口：可点击 + contentDescription（触控可发现，任务书 §23）
        compose.onNodeWithTag("pdig.search.entry").assertHasClickAction()
        compose.onNodeWithContentDescription("搜索与快捷操作").assertExists()
    }

    @Test
    fun globeHasAccessibleDescription_andRegionListFallbackExists() {
        val app = createVNextAppState().apply { reduceMotion = true; navigate(VScreen.OVERVIEW) }
        compose.setContent { VNextApp(app) }
        compose.waitForIdle()

        // Globe 画布：非视觉描述（任务书 §30：Globe marker 必须有可访问替代表示）
        compose.onNode(
            hasContentDescription("全球基础设施导航器", substring = true),
            useUnmergedTree = true,
        ).assertExists()

        // Region List 非视觉替代（screen reader / 键盘 / 触屏均可操作）
        compose.onNode(hasText("地区", substring = false)).assertExists()
        compose.onNode(hasText("中国大陆")).assertExists()
        compose.onNode(hasText("香港")).assertExists()
    }

    @Test
    fun touchTargetsMeetMinimumSize() {
        val app = createVNextAppState().apply { reduceMotion = true }
        compose.setContent { VNextApp(app) }
        compose.waitForIdle()

        // Bottom nav 触控目标由 NavigationBarItem 保证 ≥48dp（bounds 校验）
        val minTouchPx = with(compose.density) { 48.dp.toPx() }
        val navItem = compose.onNodeWithTag("pdig.nav.now", useUnmergedTree = true).fetchSemanticsNode()
        assertTrue("bottom nav touch target must be >= 48dp tall", navItem.boundsInRoot.height >= minTouchPx)

        // Studio 预设按钮 ≥48dp：测量可点击 ThemeTile，而不是其内部 Text glyph bounds。
        app.evidenceThemeId = "travel"
        app.openNumberCustomization("num-cn-1")
        compose.waitForIdle()
        val preset = compose.onAllNodesWithTag(VTestIds.STUDIO_THEME_TILE, useUnmergedTree = true)[0].fetchSemanticsNode()
        assertTrue("studio preset touch target must be >= 48dp tall", preset.boundsInRoot.height >= minTouchPx)
    }
}
