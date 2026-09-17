package com.pdig.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.pdig.app.ui.PdigApp
import com.pdig.app.ui.theme.PDIGTheme
import com.pdig.app.workflow.FileWorkflowCoordinator
import com.pdig.app.workflow.LocalFileWorkflow

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
 *
 * ---------------------------------------------------------------------------
 * ## D-16（方案 A）：外部文件选择器的 ActivityResult 注册位置
 *
 * 之前的注册方式是页面里的 `rememberLauncherForActivityResult`。它会在
 * **离开组合树时 `unregister()`**，于是这条链必然发生：
 *
 * ```
 * 拉起 DocumentsUI（独立任务）
 *   → MainActivity.onStop
 *   → LockGate.lockNow()
 *   → NavHost 离开组合树
 *   → DisposableEffect.onDispose → launcher.unregister()
 *   → ActivityResultRegistry 里那条"待投递"记录被主动注销
 *   → 用户选完文件回来，结果被丢弃
 * ```
 *
 * 现在把注册放到 **Activity 层**（`onCreate` 内，早于 `STARTED`）：
 * 它的生命周期与 Activity 相同，**不依赖任何组合树的存活**，
 * 因此 LockScreen 出现时 launcher 仍然存在，结果一定会被投递到
 * [FileWorkflowCoordinator]。
 *
 * 同时把 coordinator 本身创建在 **Activity 的 ViewModelStore** 里，
 * 使工作流状态与 Activity 同寿（≥ MainActivity 生命周期），
 * 但**不进入** Domain Reality Graph —— 它是 application/presentation workflow state。
 * ---------------------------------------------------------------------------
 */
class MainActivity : FragmentActivity() {

    /** Activity 作用域的工作流协调器。测试用 `activityWorkflow()` 取到同一个实例。 */
    private lateinit var coordinator: FileWorkflowCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        coordinator = ViewModelProvider(this)[FileWorkflowCoordinator::class.java]
        // 进程重建时归还上一次可能残留的可持久化 URI grant（见 D-16 §7 CASE B）。
        coordinator.releaseStaleGrant(this)

        // 唯一的、稳定的外部文件选择器入口。
        //
        // 用 OpenDocument（ACTION_OPEN_DOCUMENT）而不是 GetContent：
        // 只有 OpenDocument 会给出**可持久化**的 URI grant，
        // 而业务需要在"用户重新认证之后"再读取这个文件。
        val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            coordinator.onPickerResult(this, uri)
        }
        coordinator.attachLauncher { mime -> picker.launch(arrayOf(mime)) }

        setContent {
            PDIGTheme {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalFileWorkflow provides coordinator,
                ) {
                    PdigApp()
                }
            }
        }
    }

    override fun onDestroy() {
        coordinator.detachLauncher()
        super.onDestroy()
    }

    /** 供设备内取证测试取得**同一个** Activity 作用域实例。 */
    fun activityWorkflow(): FileWorkflowCoordinator = coordinator
}
