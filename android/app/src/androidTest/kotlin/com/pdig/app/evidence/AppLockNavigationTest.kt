package com.pdig.app.evidence

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.fragment.app.FragmentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pdig.app.MainActivity
import com.pdig.app.security.AppLock
import com.pdig.app.security.LockCapability
import com.pdig.app.security.LockGate
import com.pdig.app.security.LockState
import com.pdig.app.ui.screens.findFragmentActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ANDROID_APP_LOCK 的**运行时可达性**证据（P0-A）。
 *
 * 2026-09-16 之前的状态：`LockScreen` 写好了、挂进了 NavGraph，但
 * `MainActivity` 固定 `startDestination = HOME`，全仓库没有任何 `navigate(Route.LOCK)` ——
 * 也就是说"锁"这件事**在真机上根本不可达**。本测试把可达性与不可绕过性都变成断言。
 *
 * 三条防线：
 *  1. 锁定时 NavHost **不参与组合**（HOME 文本在语义树里根本不存在）
 *  2. 冷启动/recreate 一律回到锁定，即使上一刻还是解锁态
 *  3. 宿主 Activity 必须是 FragmentActivity —— 否则 BiometricPrompt 永远无法启动
 */
@RunWith(AndroidJUnit4::class)
class AppLockNavigationTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun ctx() = InstrumentationRegistry.getInstrumentation().targetContext

    /** HOME 首页独有的文本（SectionHeader）。锁定态下它必须不存在。 */
    private val homeOnlyText = "我的基础设施"

    /** 锁定态的标题。 */
    private val lockTitle = "PDIG 已锁定"

    // ------------------------------------------------------------------
    // 1. 锁定态：HOME 与敏感路由在语义树里不存在
    // ------------------------------------------------------------------

    @Test
    fun whileLocked_theNavigationGraphIsNotComposed() {
        LockGate.lockNow()
        compose.activityRule.scenario.recreate()
        compose.waitUntil(timeoutMillis = 10_000) { LockGate.locked }
        compose.waitForIdle()

        compose.onNodeWithText(lockTitle).assertIsDisplayed()

        // NavHost 未组合 ⇒ 任何路由都不存在。这一条同时覆盖了
        // "BACK 键无法回到 HOME" 与 "深链无法绕过锁"：栈里没有可返回的页面。
        listOf(
            homeOnlyText,
            "数据来源与导入",
            "设置",
            "导入账单",
            "我的基础设施",
        ).forEach { text ->
            compose.onAllNodesWithText(text).assertCountEquals(0)
        }
        assertTrue("锁定状态必须保持不变", LockGate.locked)
    }

    // ------------------------------------------------------------------
    // 2. 冷启动/recreate 一律回锁
    // ------------------------------------------------------------------

    @Test
    fun coldStartAlwaysRelocks_evenIfThePreviousSessionWasUnlocked() {
        LockGate.unlock()
        assertFalse("前置条件：先进入解锁态", LockGate.locked)

        // recreate 等价于系统重建 Activity（冷启动路径），LaunchedEffect 会重新读设备能力。
        compose.activityRule.scenario.recreate()
        compose.waitUntil(timeoutMillis = 10_000) { LockGate.locked }
        compose.waitForIdle()

        assertTrue("重建后必须回到锁定（fail-closed）", LockGate.locked)
        compose.onNodeWithText(lockTitle).assertIsDisplayed()
        compose.onAllNodesWithText(homeOnlyText).assertCountEquals(0)
    }

    // ------------------------------------------------------------------
    // 3. 显式动作确实能解锁（锁不是死的）
    // ------------------------------------------------------------------

    @Test
    fun explicitAcknowledgmentUnlocksOnADeviceWithoutAnyCredential() {
        val capability = AppLock.capability(ctx())
        LockGate.lockNow()
        compose.activityRule.scenario.recreate()
        compose.waitUntil(timeoutMillis = 10_000) { LockGate.locked }
        compose.waitForIdle()

        if (capability.usable) {
            // 设备有生物识别/设备凭据：解锁必须走系统验证对话框，无法在测试里自动完成。
            // 这里只断言"锁是真的" —— 不把无法自动化的部分伪装成通过。
            compose.onNodeWithText("验证身份并解锁").assertIsDisplayed()
            assertTrue(LockGate.locked)
            return
        }

        // 无凭据设备：产品策略是"明确说明 + 用户显式确认"，不是伪装成已解锁。
        // 文案必须先说明无法验证身份，再给放行按钮。
        compose.onNodeWithText("已知悉风险，本次进入").assertIsDisplayed()
        compose.onNodeWithText("重新检查设备能力").assertIsDisplayed()
        compose.onNodeWithText("已知悉风险，本次进入").performClick()
        compose.waitUntil(timeoutMillis = 10_000) { !LockGate.locked }
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText(homeOnlyText).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(homeOnlyText).assertIsDisplayed()
    }

    // ------------------------------------------------------------------
    // 4. 宿主 Activity 必须能承载 BiometricPrompt
    // ------------------------------------------------------------------

    @Test
    fun hostingActivityCanActuallyHostABiometricPrompt() {
        compose.activityRule.scenario.onActivity { activity ->
            // androidx.biometric.BiometricPrompt 只接受 FragmentActivity。
            // 之前 MainActivity 继承 ComponentActivity，findFragmentActivity() 恒为 null，
            // 解锁按钮一按就落到"无法启动验证" —— 生物识别路径结构上不可能成功。
            assertTrue(
                "宿主 Activity 必须是 FragmentActivity，否则生物识别永远无法启动",
                activity is FragmentActivity,
            )
            assertNotNull(
                "必须能从 Activity 解析出可用的 FragmentActivity",
                activity.findFragmentActivity(),
            )
        }
    }

    // ------------------------------------------------------------------
    // 5. 状态机 fail-closed：任何设备能力组合都不会得到 UNLOCKED
    // ------------------------------------------------------------------

    @Test
    fun lockStateNeverReportsUnlockedForAnyCapabilityCombination() {
        for (biometric in listOf(false, true)) {
            for (credential in listOf(false, true)) {
                for (noHardware in listOf(false, true)) {
                    val capability = LockCapability(biometric, credential, noHardware)
                    val state = AppLock.state(capability)
                    val label = "bio=$biometric cred=$credential noHw=$noHardware"
                    assertNotEquals("$label 不得被判为 UNLOCKED", LockState.UNLOCKED, state)
                    val expected = when {
                        biometric || credential -> LockState.LOCKED
                        noHardware -> LockState.UNAVAILABLE
                        else -> LockState.NOT_CONFIGURED
                    }
                    assertEquals(label, expected, state)
                }
            }
        }
    }

    @Test
    fun realDeviceCapabilityIsReportedAndLocked() {
        val capability = AppLock.capability(ctx())
        val state = AppLock.state(capability)
        println("APP_LOCK_CAPABILITY bio=${capability.biometricUsable} cred=${capability.credentialUsable} hwMissing=${capability.hardwareMissing}")
        println("APP_LOCK_STATE=$state")
        assertNotEquals("真机状态永远不得是 UNLOCKED", LockState.UNLOCKED, state)
        if (capability.usable) {
            assertEquals(LockState.LOCKED, state)
        }
    }
}
