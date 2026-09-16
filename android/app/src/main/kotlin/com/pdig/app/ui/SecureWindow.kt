package com.pdig.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * 敏感页面截图保护（ANDROID_SCREEN_PROTECTION）。
 *
 * 采用 Android 平台标准方案 `WindowManager.LayoutParams.FLAG_SECURE`，覆盖两类泄露面：
 *  1. 系统截屏 / 录屏
 *  2. 最近任务（Recents）里的应用缩略图
 *
 * 判定标准：**展示具体依赖明细 / 原始导入数据 / 备份恢复入口的页面**为敏感页。
 * 概览与说明类页面（首页、场景中心、设置、隐私、关于、Onboarding、Lock）不设保护，
 * 避免为了"看起来安全"而无意义地损害正常可用性。
 *
 * 诚实边界：FLAG_SECURE 只阻止截屏与最近任务缩略图，
 * **不等于**数据加密，也不能防止已 Root 设备直接读取内存。
 */
private val SENSITIVE_ROUTES: Set<String> = setOf(
    Route.IMPORT,
    Route.BACKUP,
    Route.RESTORE,
    Route.SOURCES,
    Route.REVIEW,
    Route.DRIFT,
    Route.CANDIDATES,
    Route.INFRASTRUCTURE,
    Route.GRAPH,
    Route.NODE,
    Route.TIMELINE,
    Route.PLAN,
    // 本轮 P0 新增：影响面明细与场景选定要变更哪张卡，都直接暴露依赖明细
    Route.IMPACT,
    Route.SCENARIO_SETUP,
)

/** 唯一判定源：运行时与测试共用，避免两处口径不一致。 */
fun isSensitiveRoute(route: String?): Boolean = route != null && route in SENSITIVE_ROUTES

/** 供测试枚举全部受保护路由。 */
fun sensitiveRoutes(): Set<String> = SENSITIVE_ROUTES

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** 按当前路由设置 / 清除 FLAG_SECURE。仅在路由变化时执行，不是每帧执行。 */
@Composable
fun SecureWindow(route: String?) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val sensitive = isSensitiveRoute(route)
    DisposableEffect(route, activity) {
        val window = activity?.window
        val flag = WindowManager.LayoutParams.FLAG_SECURE
        if (window != null) {
            if (sensitive) {
                window.addFlags(flag)
            } else {
                window.clearFlags(flag)
            }
        }
        onDispose {
            window?.clearFlags(flag)
        }
    }
}
