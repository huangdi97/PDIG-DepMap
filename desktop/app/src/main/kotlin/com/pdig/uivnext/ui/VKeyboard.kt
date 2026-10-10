package com.pdig.uivnext.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pdig.uivnext.model.VScreen

/**
 * Keyboard-first routing（PHASE 1E §40）—— 确定性的、可离屏验证的键盘状态机。
 *
 * 覆盖：Ctrl/Cmd+K（打开命令面板）、Escape（关闭/返回）、Enter/Space（执行/激活）、
 * Tab/Shift+Tab（在焦点顺序中移动）、Arrow Up/Down（面板内选择）。
 * 所有动作都写入 app 状态，无 OS 窗口依赖；运行时由 Compose onKeyEvent 转发，
 * 证据/测试由 routeKey 直接驱动（in-process 确定性标准）。
 */
/** 焦点顺序（跨屏可见焦点目标；每次 Tab 前进、Shift+Tab 后退）。 */
internal val FOCUS_ORDER: List<VScreen> = listOf(
    VScreen.NOW,
    VScreen.INFRASTRUCTURE,
    VScreen.CARDS,
    VScreen.NUMBERS,
    VScreen.CHANGE,
    VScreen.RECORDS,
    VScreen.SOURCES,
    VScreen.SETTINGS,
)

/** 当前焦点索引（shell 持有；Modifier.focusable 的焦点跟随同一顺序）。 */
class KeyboardFocusState(
    initialIndex: Int = 0,
) {
    var index by androidx.compose.runtime.mutableStateOf(initialIndex)
    val focusTarget: VScreen get() = FOCUS_ORDER[index.coerceIn(0, FOCUS_ORDER.lastIndex)]
    fun advance(shift: Boolean = false) {
        index = (index + if (shift) -1 else 1).let { i ->
            if (i < 0) FOCUS_ORDER.lastIndex else if (i > FOCUS_ORDER.lastIndex) 0 else i
        }
    }
}

/** 键盘动作结果（供 harness 断言）。 */
enum class KeyAction { OPEN_PALETTE, CLOSE_PALETTE, EXECUTE, ACTIVATE, NEXT, PREV, MOVE_DOWN, MOVE_UP, NONE }

/**
 * 纯函数键盘路由：根据当前 app 状态 + 按键（规范化字符串）返回并执行动作。
 * 返回执行的 KeyAction（harness 记录为 interaction log；NONE = 未消耗）。
 */
fun routeKey(app: VAppState, focus: KeyboardFocusState, key: String): KeyAction {
    return when {
        key == "ctrl+k" -> {
            app.paletteOpen = !app.paletteOpen
            if (app.paletteOpen) {
                app.paletteQuery = ""
                app.paletteSelectedIndex = 0
            }
            KeyAction.OPEN_PALETTE
        }
        key == "escape" && app.paletteOpen -> {
            app.paletteOpen = false
            KeyAction.CLOSE_PALETTE
        }
        key == "escape" && app.globe.selectedRegion != null -> {
            app.clearRegion()
            KeyAction.CLOSE_PALETTE
        }
        app.paletteOpen -> routePaletteKey(app, key)
        key == "tab" -> {
            focus.advance(shift = false)
            KeyAction.NEXT
        }
        key == "shift+tab" -> {
            focus.advance(shift = true)
            KeyAction.PREV
        }
        key == "enter" || key == "space" -> {
            // 焦点目标激活：导航到焦点屏幕（领域不变）。
            app.navigate(focus.focusTarget)
            KeyAction.ACTIVATE
        }
        else -> KeyAction.NONE
    }
}

/** 面板打开时的键：↑↓ 选择、Enter/Space 执行、query 字符累加。 */
internal fun routePaletteKey(app: VAppState, key: String): KeyAction {
    val entries = filterPalette(paletteEntries(), app.paletteQuery)
    return when (key) {
        "down" -> {
            if (entries.isNotEmpty()) {
                app.paletteSelectedIndex = (app.paletteSelectedIndex + 1).coerceAtMost(entries.size - 1)
            }
            KeyAction.MOVE_DOWN
        }
        "up" -> {
            if (entries.isNotEmpty()) {
                app.paletteSelectedIndex = (app.paletteSelectedIndex - 1).coerceAtLeast(0)
            }
            KeyAction.MOVE_UP
        }
        "enter", "space" -> {
            if (entries.isNotEmpty()) {
                val entry = entries[app.paletteSelectedIndex.coerceIn(0, entries.size - 1)]
                app.paletteSelectedIndex = app.paletteSelectedIndex.coerceIn(0, entries.size - 1)
                entry.run(app)
                app.paletteOpen = false
            }
            KeyAction.EXECUTE
        }
        "backspace" -> {
            app.paletteQuery = app.paletteQuery.dropLast(1)
            KeyAction.NONE
        }
        else -> {
            // 单打印字符 → 追加到 query（search-as-you-type）。
            if (key.length == 1) {
                app.paletteQuery = app.paletteQuery + key
                app.paletteSelectedIndex = 0
            }
            KeyAction.NONE
        }
    }
}

/** Windows / macOS 按键归一化（Compose KeyEvent → 标准串）。 */
internal fun normalizeKey(isCtrl: Boolean, isShift: Boolean, key: androidx.compose.ui.input.key.Key): String? {
    return when {
        isCtrl && key == androidx.compose.ui.input.key.Key.K -> "ctrl+k"
        isShift && key == androidx.compose.ui.input.key.Key.Tab -> "shift+tab"
        key == androidx.compose.ui.input.key.Key.Tab -> "tab"
        key == androidx.compose.ui.input.key.Key.Escape -> "escape"
        key == androidx.compose.ui.input.key.Key.Enter -> "enter"
        key == androidx.compose.ui.input.key.Key.Spacebar -> "space"
        key == androidx.compose.ui.input.key.Key.DirectionUp -> "up"
        key == androidx.compose.ui.input.key.Key.DirectionDown -> "down"
        key == androidx.compose.ui.input.key.Key.Backspace -> "backspace"
        else -> null
    }
}