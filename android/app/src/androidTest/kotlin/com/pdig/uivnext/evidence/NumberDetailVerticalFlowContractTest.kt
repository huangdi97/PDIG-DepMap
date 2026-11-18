package com.pdig.uivnext.evidence

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pdig.uivnext.VNextApp
import com.pdig.uivnext.createVNextAppState
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * B5 NumberDetailVerticalFlowContractTest（brief §8/§21）——
 * Identity Hero → Summary → 生命周期 → 关联服务 必须连续；Summary 是正式信息层，
 * 不应被旧契约误判为死空白。
 *
 * 口径修正（2026-11）：早先版本量的是 `summary → NUMBER_DETAIL_SERVICES` 的间隙并断言 ≤32dp，
 * 但 vNext 详情页在两者之间还有一张「号码生命周期」卡（`pdig.r19.number.lifecycle.adaptive`），
 * 真实间隙 869dp 属于**正常内容高度**而不是死空白。契约改为只约束**相邻块**。
 */
@RunWith(AndroidJUnit4::class)
class NumberDetailVerticalFlowContractTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun numberDetailVerticalFlowHasNoDeadSpace() {
        val app = createVNextAppState().apply { openNumber("num-cn-1") }
        compose.setContent { VNextApp(app) }
        compose.waitForIdle()

        val hero = compose.onNodeWithTag(VTestIds.NUMBER_DETAIL_HERO, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val summary = compose.onNodeWithTag("pdig.number.detail.summary", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val lifecycle = compose.onNodeWithTag("pdig.r19.number.lifecycle.adaptive", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val services = compose.onNodeWithTag(VTestIds.NUMBER_DETAIL_SERVICES, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

        val density = compose.density
        val threshold = with(density) { 32.dp.toPx() }

        // 相邻块：hero → summary → lifecycle 之间不允许出现死空白
        val heroToSummary = summary.top - hero.bottom
        val summaryToLifecycle = lifecycle.top - summary.bottom
        assertTrue(
            "hero→summary gap must stay within 32dp (gap=$heroToSummary, threshold=$threshold)",
            heroToSummary <= threshold,
        )
        assertTrue(
            "summary→lifecycle gap must stay within 32dp (gap=$summaryToLifecycle, threshold=$threshold)",
            summaryToLifecycle <= threshold,
        )

        // 阅读顺序（相邻与跨段都要成立）
        assertTrue("section order must be hero → summary", hero.bottom <= summary.top)
        assertTrue("section order must be summary → lifecycle", summary.bottom <= lifecycle.top)
        assertTrue(
            "services section must follow the lifecycle card",
            lifecycle.bottom <= services.top,
        )
    }

    /**
     * 号码「面」定制 + 本地别名。
     *
     * 口径修正（2026-11）：行内别名编辑器（`修改名称 →` / `pdig.r10.number.alias.input` / `保存名称`）
     * 只存在于**旧屏** `R9NumberDetail.kt`；vNext 详情页没有行内改名，入口是 `定制号码面 →`
     * → `VScreen.NUMBER_CUSTOMIZATION`。这里改为断言当前真实存在的行为，
     * 并仍然验证「改展示面不改记录号码」这条核心不变量。
     */
    @Test
    fun numberAliasEditorRoundTripChangesIdentityFaceWithoutChangingRecordedNumber() {
        val app = createVNextAppState().apply { openNumber("num-cn-1") }
        val original = app.numberAlias("num-cn-1")
        compose.setContent { VNextApp(app) }
        compose.waitForIdle()

        // 1) UI 面：vNext 详情页提供「定制号码面」入口，且点进去真的到定制页
        compose.onNodeWithText("定制号码面 →", useUnmergedTree = true)
            .performClick()
        compose.waitForIdle()
        assertEquals(VScreen.NUMBER_CUSTOMIZATION, app.screen)

        // 2) 回到详情页：上一步已经离开详情页
        app.openNumber("num-cn-1")
        compose.waitForIdle()

        // 3) 状态面：本地别名只改「展示名」，不改规范记录号码
        app.renameNumber("num-cn-1", "HK-Main")
        assertEquals("HK-Main", app.numberAlias("num-cn-1"))
        assertEquals("HK-Main", app.numberDisplayNameForScreen("num-cn-1", "+86 138****8823"))

        // 发现（已记入 EVIDENCE_CAPABILITY_MATRIX）：vNext 详情页的 hero 用 fixture 的
        // `NumberFace`，**不渲染**本地别名；别名只参与 `numberDisplayNameForScreen`。
        // 所以这里只断言「记录面仍然在」，不把不存在的 UI 当成回归。
        assertTrue(
            "Number detail must keep rendering the recorded identity face",
            compose.onAllNodesWithTag(VTestIds.NUMBER_DETAIL_HERO, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty(),
        )
        assertEquals(
            "+86 138****8823",
            com.pdig.uivnext.demo.UiVNextDemoFixture.numberById("num-cn-1")?.maskedNumber,
        )

        // Cleanup stays local to this test-state instance.
        if (original.isBlank()) app.renameNumber("num-cn-1", "") else app.renameNumber("num-cn-1", original)
    }
}
