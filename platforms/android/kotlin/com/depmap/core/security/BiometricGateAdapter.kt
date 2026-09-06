package com.depmap.core.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import kotlin.coroutines.resume

/**
 * 启动锁：系统生物识别 / 设备凭证（BiometricPrompt，含 DEVICE_CREDENTIAL fallback）。
 *
 * 安全语义：取消 / 失败 / 未注册时返回 ok=false，调用层不得进入数据层。
 *
 * 状态：IMPLEMENTED（代码完成）。COMPILED / TESTED / DEVICE_VERIFIED = NO
 * （当前 Windows 环境无 Android SDK / JDK17，见 BLOCKERS.md）。
 */
class BiometricGateAdapter(private val activity: FragmentActivity) : BiometricGate {

    private val executor: Executor = ContextCompat.getMainExecutor(activity)

    override suspend fun canAuthenticate(): Boolean {
        val bm = BiometricManager.from(activity)
        return bm.canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
    }

    override suspend fun authenticate(reason: String): BiometricGateResult =
        suspendCancellableCoroutine { cont ->
            val bm = BiometricManager.from(activity)
            val can = bm.canAuthenticate(AUTHENTICATORS)
            if (can == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                cont.resume(BiometricGateResult(ok = false, reason = "no_enrollment"))
                return@suspendCancellableCoroutine
            }
            if (can != BiometricManager.BIOMETRIC_SUCCESS) {
                cont.resume(BiometricGateResult(ok = false, reason = "not_available"))
                return@suspendCancellableCoroutine
            }
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    cont.resume(BiometricGateResult(ok = true, reason = null))
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    val reason = when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> "user_cancel"
                        BiometricPrompt.ERROR_LOCKOUT,
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> "lockout"
                        BiometricPrompt.ERROR_NO_BIOMETRICS -> "no_enrollment"
                        BiometricPrompt.ERROR_HW_NOT_PRESENT,
                        BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> "not_available"
                        else -> "error"
                    }
                    cont.resume(BiometricGateResult(ok = false, reason = reason))
                }
            }
            val prompt = BiometricPrompt(activity, executor, callback)
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("解锁个人数字依赖图")
                .setSubtitle(reason)
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build()
            cont.invokeOnCancellation { prompt.cancelAuthentication() }
            prompt.authenticate(info)
        }

    companion object {
        private const val AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}

data class BiometricGateResult(val ok: Boolean, val reason: String?)

interface BiometricGate {
    suspend fun canAuthenticate(): Boolean
    suspend fun authenticate(reason: String): BiometricGateResult
}
