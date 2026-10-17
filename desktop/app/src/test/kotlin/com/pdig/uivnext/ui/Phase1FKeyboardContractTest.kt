package com.pdig.uivnext.ui

import com.pdig.uivnext.createVNextAppState
import com.pdig.uivnext.model.VScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * PHASE 1F 键盘/a11y 契约（§47）：空态 CTA、Studio 控件、Continuity 投影选择器
 * 所在屏幕的键盘可达性 + palette 检索/执行（聚焦模型驱动，确定性）。
 */
class Phase1FKeyboardContractTest {

    private fun paletteRoundTrip(screen: VScreen, extra: (VAppState) -> Unit = {}): Boolean {
        val app = createVNextAppState(screen, "global")
        extra(app)
        val focus = KeyboardFocusState()
        val opened = routeKey(app, focus, "ctrl+k") == KeyAction.OPEN_PALETTE
        val closed = routeKey(app, focus, "escape") == KeyAction.CLOSE_PALETTE
        return opened && closed
    }

    @Test
    fun emptyStateScreensAreKeyboardReachable() {
        assertTrue(paletteRoundTrip(VScreen.CARDS) { it.demoEmptyCards = true }, "Cards Empty")
        assertTrue(paletteRoundTrip(VScreen.NUMBERS) { it.demoEmptyNumbers = true }, "Numbers Empty")
        assertTrue(paletteRoundTrip(VScreen.NOW) { it.demoEmptyAttention = true; it.demoEmptyChanges = true }, "Now Empty")
        assertTrue(paletteRoundTrip(VScreen.OVERVIEW) { it.demoEmptyRegion = true }, "Overview Region Empty")
    }

    @Test
    fun studioAndProjectionScreensAreKeyboardReachable() {
        assertTrue(paletteRoundTrip(VScreen.CARD_CUSTOMIZATION) { it.openCardCustomization("card-cn-1") }, "Card Studio")
        assertTrue(paletteRoundTrip(VScreen.NUMBER_CUSTOMIZATION) { it.openNumberCustomization("num-cn-1") }, "Number Studio")
        assertTrue(paletteRoundTrip(VScreen.CHANGE_PHONE) { it.changeProjection = "after" }, "Change Phone / projection selector")
    }

    @Test
    fun paletteKeyboardExecutesToProjectionSelectorScreen() {
        val app = createVNextAppState(VScreen.OVERVIEW, "global")
        val focus = KeyboardFocusState()
        assertEquals(KeyAction.OPEN_PALETTE, routeKey(app, focus, "ctrl+k"))
        // 键入 "更"（中文检索按字符累积；开始更换手机号 label 含「更换手机号」）
        assertEquals(KeyAction.NONE, routeKey(app, focus, "更"))
        assertEquals(KeyAction.NONE, routeKey(app, focus, "换"))
        assertTrue(filterPalette(paletteEntries(), "更换手机号").any { it.id == "start-change-phone" })
        assertEquals(KeyAction.EXECUTE, routeKey(app, focus, "enter"))
        assertEquals(VScreen.CHANGE_PHONE, app.screen, "palette Enter must land on Change Phone (projection selector screen)")
    }

    @Test
    fun paletteSearchStillFindsCardsAndNumbers() {
        assertTrue(filterPalette(paletteEntries(), "card").any { it.id == "open-cards" })
        assertTrue(filterPalette(paletteEntries(), "卡").isNotEmpty(), "Chinese query should still work with prefix matching")
    }
}
