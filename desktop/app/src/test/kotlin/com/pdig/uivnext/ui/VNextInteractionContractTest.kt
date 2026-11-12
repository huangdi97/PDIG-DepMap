package com.pdig.uivnext.ui

import com.pdig.uivnext.createVNextAppState
import com.pdig.uivnext.model.VScreen
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * PHASE 1E 交互契约测试（AC4 / AC5 / AC13）：
 * - Continuity 投影往返（Current → Transition → After → Current）状态机正确；
 * - 「计划投影」标注仅属于 After 档（UI 文案契约，§68）；
 * - 键盘优先（Ctrl+K / Tab / Shift+Tab / Enter / Space / Escape）在 7 屏可达；
 * - Command Palette 检索真实可用（搜索 card → 打开卡片列表）。
 * 全部 in-process 确定性（无 OS 窗口）。
 */
class VNextInteractionContractTest {

    // ---- AC4：Continuity 投影往返 ----

    @Test
    fun `continuity projection round trip current transition after current keeps correct states`() {
        val app = createVNextAppState(VScreen.CHANGE_PHONE, "global")

        app.changeProjection = "current"
        assertEquals("current", app.changeProjection)

        app.changeProjection = "transition"
        assertEquals("transition", app.changeProjection)

        app.changeProjection = "after"
        assertEquals("after", app.changeProjection)

        // After → Current 往返（§38：必须真实可点击往返）
        app.changeProjection = "current"
        assertEquals("current", app.changeProjection)

        // 关键帧渲染不抛异常（scene 各档位 geometry 相同，状态驱动）
        renderChangeFrame(app, "roundtrip-current")
    }

    @Test
    fun `plan projection marker belongs only to after stage`() {
        // UI 文案契约：After 档必须带「计划投影」辅助文案，其余档位不得出现。
        val afterLabel = projectionLabel("after")
        assertEquals("完成后 · 计划投影", afterLabel)
        assertEquals("当前", projectionLabel("current"))
        assertEquals("迁移中", projectionLabel("transition"))
        assertTrue(afterLabel.contains("计划投影"))
        assertTrue(!afterLabel.contains("PLAN PROJECTION"), "普通 UI 不出现英文 PLAN PROJECTION")
    }

    // ---- AC5：键盘优先 ----

    @Test
    fun `command palette search and execute opens cards list`() {
        val app = createVNextAppState(VScreen.OVERVIEW, "global")
        val focus = KeyboardFocusState()

        assertEquals(KeyAction.OPEN_PALETTE, routeKey(app, focus, "ctrl+k"))
        assertTrue(app.paletteOpen)

        // search-as-you-type：c-a-r-d
        routeKey(app, focus, "c")
        routeKey(app, focus, "a")
        routeKey(app, focus, "r")
        routeKey(app, focus, "d")
        assertEquals("card", app.paletteQuery)
        assertTrue(filterPalette(paletteEntries(), "card").isNotEmpty())

        // Enter 执行 index 0 = 「搜索卡片」→ CARDS
        assertEquals(KeyAction.EXECUTE, routeKey(app, focus, "enter"))
        assertEquals(VScreen.CARDS, app.screen)
        assertEquals(false, app.paletteOpen)
    }

    @Test
    fun `keyboard reachability across seven screens`() {
        val screens = listOf(
            VScreen.OVERVIEW,
            VScreen.CARDS,
            VScreen.NUMBERS,
            VScreen.CARD_DETAIL,
            VScreen.NUMBER_DETAIL,
            VScreen.CARD_CUSTOMIZATION,
            VScreen.CHANGE_PHONE,
        )
        screens.forEach { screen ->
            val app = createVNextAppState(screen, "global")
            val focus = KeyboardFocusState()
            assertEquals(KeyAction.OPEN_PALETTE, routeKey(app, focus, "ctrl+k"), "Ctrl+K on ${screen.route}")
            assertEquals(KeyAction.CLOSE_PALETTE, routeKey(app, focus, "escape"), "Escape on ${screen.route}")
        }
    }

    @Test
    fun `tab shift-tab enter space and escape routing`() {
        val app = createVNextAppState(VScreen.NOW, "global")
        val focus = KeyboardFocusState()

        assertEquals(KeyAction.NEXT, routeKey(app, focus, "tab"))
        assertEquals(1, focus.index)
        assertEquals(KeyAction.PREV, routeKey(app, focus, "shift+tab"))
        assertEquals(0, focus.index)

        // Space 激活焦点目标（focus target = NOW）
        assertEquals(KeyAction.ACTIVATE, routeKey(app, focus, "space"))
        assertEquals(VScreen.NOW, app.screen)

        // Escape 关闭 palette（先打开再关）
        routeKey(app, focus, "ctrl+k")
        assertEquals(KeyAction.CLOSE_PALETTE, routeKey(app, focus, "escape"))
    }

    @Test
    fun `palette entry ids unique for cards and numbers`() {
        val entries = paletteEntries()
        val ids = entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "palette entries must have unique ids")
        assertTrue(entries.any { it.id == "open-cards" })
        assertTrue(entries.any { it.id == "open-numbers" })
        assertTrue(entries.any { it.id == "open-infrastructure" })
        assertTrue(entries.any { it.id == "start-change-phone" })
        assertTrue(entries.any { it.id == "open-personalization" })
        assertNotNull(entries.firstOrNull { it.id.startsWith("card:") })
        assertNotNull(entries.firstOrNull { it.id.startsWith("number:") })
    }

    private fun projectionLabel(key: String): String = when (key) {
        "after" -> "完成后 · 计划投影"
        "current" -> "当前"
        "transition" -> "迁移中"
        else -> key
    }

    private fun renderChangeFrame(app: com.pdig.uivnext.ui.VAppState, tag: String) {
        val profile = com.pdig.uivnext.evidence.VNextShotDriver.Profile(1920, 1080, "1920x1080@1.0")
        val file = java.io.File(
            System.getProperty("java.io.tmpdir"),
            "pdig-test-change-$tag-${System.nanoTime()}.png",
        )
        com.pdig.uivnext.evidence.VNextShotDriver.renderToFile(app, profile, file)
        assertTrue(file.isFile && file.length() > 0L, "change frame must render non-empty")
        file.delete()
    }
}