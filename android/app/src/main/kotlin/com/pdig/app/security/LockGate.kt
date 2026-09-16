package com.pdig.app.security

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 应用锁门 —— **application/security state**，与 Reality Graph 完全无关。
 *
 * 不变量（每条都由设备内测试断言）：
 *
 * 1. **fail-closed 初值**：进程刚起来时一定是 LOCKED。不存在"先把 HOME 渲染出来
 *    再决定要不要锁"的窗口 —— 渲染 HOME 的 NavHost 在 LOCKED 时**根本不会被组合**。
 * 2. **只有显式动作能解锁**：[locked] 只能由 [unlock] 置为 false，而 [unlock] 的调用点
 *    只有两处 —— 生物识别/设备凭据成功，以及用户在"设备无任何凭据"时显式确认继续。
 * 3. **回到后台立即回锁**：`Lifecycle.Event.ON_STOP` 一律 [lockNow]。
 * 4. **不写任何持久化**：进程被杀 → 重新创建 → 回到 LOCKED。
 *
 * 用 Compose 的 snapshot state 保存，因此它的变化会驱动重组。
 */
object LockGate {

    /** 初值 = true。fail-closed，不是"默认不锁"。 */
    var locked: Boolean by mutableStateOf(true)
        private set

    /** 唯一解锁入口。调用方必须已经完成验证（或用户显式确认）。 */
    fun unlock() {
        locked = false
    }

    /** 立即回锁。冷启动、前后台切换、用户主动"立即锁定"都走这里。 */
    fun lockNow() {
        locked = true
    }
}
