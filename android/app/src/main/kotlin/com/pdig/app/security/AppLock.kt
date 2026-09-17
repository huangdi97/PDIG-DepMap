package com.pdig.app.security

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * 设备当前可用的身份验证能力（只读的设备事实，不是会话状态）。
 *
 * 把「能力」与「锁状态」分开，是为了让 fail-closed 语义可验证：
 * 能力只描述设备能做什么；**能不能进入应用**永远由 [LockGate] 决定，
 * 且只能由一次显式的用户动作离开 LOCKED。
 */
data class LockCapability(
    val biometricUsable: Boolean,
    val credentialUsable: Boolean,
    val hardwareMissing: Boolean,
) {
    /** 是否存在任何可用于验证身份的手段。为 false 时必须 fail-closed。 */
    val usable: Boolean get() = biometricUsable || credentialUsable

    companion object {
        /** 保守初值：在读到真实设备事实之前，一律按"没有可用手段"处理。 */
        val UNKNOWN = LockCapability(
            biometricUsable = false,
            credentialUsable = false,
            hardwareMissing = false,
        )
    }
}

/**
 * App Lock —— BiometricPrompt（spec §60 / §155）。
 *
 * 覆盖：cancel / failure / fallback policy / background-resume。
 * 产品语义三端统一（spec §155），平台实现各自不同；**任何失败路径一律 fail-closed**。
 *
 * 与 2026-09-16 之前版本的差别（修的是真实缺陷，不是重设计安全模型）：
 *
 * 1. 旧 `state()` 只查 `BIOMETRIC_WEAK`。设备已设置锁屏 PIN/图案但没有录入指纹时，
 *    它会返回 `NOT_CONFIGURED` —— 明明**可以**验证身份，却被当成"没有凭据"。
 *    现在同时查询 `DEVICE_CREDENTIAL`，有任一种可用即视为 `LOCKED`（可验证）。
 * 2. 旧 `state()` 直接返回 `LOCKED` 并把它当成"锁状态"使用，语义被混用。
 *    现在 `state()` 只回答"设备要求什么验证"，返回 `UNLOCKED` 是不可能的路径，
 *    并且由测试显式断言这一点。
 * 3. 旧的 `authenticate()` 恒设 `setNegativeButtonText("取消")`。一旦将来允许
 *    `DEVICE_CREDENTIAL`，`PromptInfo.Builder.build()` 会直接抛
 *    `IllegalArgumentException` —— 这是一个尚未被触发的真实地雷，这里一并消除。
 */
object AppLock {

    /**
     * 读取设备能力。只读、无副作用；可在后台线程调用。
     *
     * **为什么设备凭据不能只问 `BiometricManager`**（2026-09-17 真机取证发现）：
     * 在 AVD（API 34 / AOSP `android-34/default/x86_64`）执行
     * `adb shell locksettings set-pin 1234` 并验证通过后，
     * `dumpsys lock_settings` 明确显示 `CredentialType: PIN`，
     * 但 `BiometricManager.canAuthenticate(DEVICE_CREDENTIAL)` 仍返回非 SUCCESS ——
     * 于是锁屏走了「设备无任何凭据」分支，用一个「已知悉风险，本次进入」按钮放行用户。
     * 那是一次真实的安全语义缺陷：设备**能够**验证身份，却被降级为手动确认。
     *
     * `KeyguardManager.isDeviceSecure()` 判断的正是"是否存在 PIN/图案/密码"这件事本身，
     * 不依赖 biometric service 的绑定状态，因此以它为准；`BiometricManager` 的结果作为补充。
     */
    fun capability(context: Context): LockCapability {
        val manager = BiometricManager.from(context)
        val biometric = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        val credential = manager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        val deviceSecure = runCatching {
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
            km.isDeviceSecure
        }.getOrDefault(false)
        return LockCapability(
            biometricUsable = biometric == BiometricManager.BIOMETRIC_SUCCESS,
            credentialUsable = credential == BiometricManager.BIOMETRIC_SUCCESS || deviceSecure,
            // 只有"生物识别与设备凭据都报告没有硬件"才算真的 UNAVAILABLE；
            // 其余未知情况一律落到 NOT_CONFIGURED，两者都是 fail-closed。
            hardwareMissing = biometric == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE &&
                credential == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
        )
    }

    /**
     * 设备要求的验证方式（spec §155 四态）。
     *
     * **本函数永远不会返回 `UNLOCKED`**：解锁是会话状态，只能由 [LockGate.unlock]
     * 在一次显式、成功的用户动作之后设置。这条不变量由设备内测试断言。
     */
    fun state(capability: LockCapability): LockState = when {
        capability.usable -> LockState.LOCKED
        capability.hardwareMissing -> LockState.UNAVAILABLE
        else -> LockState.NOT_CONFIGURED
    }

    /** 便捷重载：直接读设备能力再判定。 */
    fun state(context: Context): LockState = state(capability(context))

    /**
     * 选择本次提示允许的身份验证方式。
     *
     * `BIOMETRIC_WEAK | DEVICE_CREDENTIAL` 的组合只在 API 30+ 合法；
     * 低版本组合会抛 `IllegalArgumentException: Device credential not supported with biometrics`。
     */
    fun allowedAuthenticators(capability: LockCapability): Int = when {
        capability.biometricUsable && capability.credentialUsable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        capability.biometricUsable -> BiometricManager.Authenticators.BIOMETRIC_WEAK
        else -> BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }

    /**
     * 发起一次身份验证。
     *
     * 成功 → `onSuccess`（调用方才能把 [LockGate] 置为 unlocked）。
     * 取消 / 失败 → 分别回调，**两者都保持 LOCKED**（fail-closed）。
     */
    fun authenticate(
        activity: FragmentActivity,
        capability: LockCapability,
        title: String = "解锁 PDIG",
        subtitle: String = "验证身份后查看你的基础设施",
        onSuccess: () -> Unit,
        onFailure: (Int, CharSequence) -> Unit,
        onCancel: () -> Unit,
    ) {
        require(capability.usable) { "no_authenticator_available" }
        val allowed = allowedAuthenticators(capability)
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // 用户取消与真实错误分开上报；两者都保持锁定（fail-closed）
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    ) {
                        onCancel()
                    } else {
                        onFailure(errorCode, errString)
                    }
                }

                override fun onAuthenticationFailed() {
                    // 单次匹配失败（例如指纹不对）：不结束会话，但仍不得解锁
                    onFailure(-1, "验证未通过")
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(allowed)
        // 允许 DEVICE_CREDENTIAL 时**不得**设置 negative button（系统自带取消入口），
        // 否则 PromptInfo.Builder.build() 抛 IllegalArgumentException。
        if (allowed == BiometricManager.Authenticators.BIOMETRIC_WEAK) {
            info.setNegativeButtonText("取消")
        }
        prompt.authenticate(info.build())
    }
}
