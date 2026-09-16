package com.pdig.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.pdig.app.ui.PdigApp
import com.pdig.app.ui.theme.PDIGTheme

/**
 * 单一 Activity + Compose Navigation（spec §53）。无 WebView、无 uni-app runtime。
 *
 * ⚠ 基类必须是 [FragmentActivity]，不能是 `ComponentActivity`：
 * `androidx.biometric.BiometricPrompt` 的构造签名只接受 `FragmentActivity`
 * （它内部要挂一个 Fragment 来承载系统验证界面）。
 * 在此之前这里继承的是 `ComponentActivity`，于是 `LockScreen` 里
 * `context.findFragmentActivity()` **恒返回 null** —— 解锁按钮一按就落到
 * 「无法启动验证」分支。也就是说：**生物识别路径在结构上从来没有可能成功**，
 * 与"设备有没有指纹"无关。这是 2026-09-16 的 P0-A 修复项之一。
 *
 * FragmentActivity 不要求 AppCompat 主题，现有框架主题 `Theme.PDIG` 可直接使用。
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PDIGTheme {
                PdigApp()
            }
        }
    }
}
