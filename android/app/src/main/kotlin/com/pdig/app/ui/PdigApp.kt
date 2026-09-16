package com.pdig.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pdig.app.security.AppLock
import com.pdig.app.security.LockCapability
import com.pdig.app.security.LockGate
import com.pdig.app.ui.screens.AboutScreen
import com.pdig.app.ui.screens.BackupScreen
import com.pdig.app.ui.screens.CandidateReviewScreen
import com.pdig.app.ui.screens.ChangePlanScreen
import com.pdig.app.ui.screens.GraphScreen
import com.pdig.app.ui.screens.HomeScreen
import com.pdig.app.ui.screens.ImpactScreen
import com.pdig.app.ui.screens.ImportScreen
import com.pdig.app.ui.screens.InfrastructureScreen
import com.pdig.app.ui.screens.LockCheckingScreen
import com.pdig.app.ui.screens.LockScreen
import com.pdig.app.ui.screens.NodeDetailScreen
import com.pdig.app.ui.screens.OnboardingScreen
import com.pdig.app.ui.screens.PendingReviewScreen
import com.pdig.app.ui.screens.PrivacyScreen
import com.pdig.app.ui.screens.RealityDriftScreen
import com.pdig.app.ui.screens.RestoreScreen
import com.pdig.app.ui.screens.ScenarioCenterScreen
import com.pdig.app.ui.screens.ScenarioSetupScreen
import com.pdig.app.ui.screens.SettingsScreen
import com.pdig.app.ui.screens.SourceManagementScreen
import com.pdig.app.ui.screens.TimelineScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Route {
    const val ONBOARDING = "onboarding"

    /**
     * 应用锁**不是** NavGraph 中的一个目的地 —— 它是 App 的门（见 [PdigApp]）。
     * 常量保留是因为截图保护口径表需要为它显式归类，且它记录了"锁定"这个语义位置；
     * 但 NavHost 里**刻意不注册**它：只要锁着，NavHost 本体就不会参与组合，
     * 因此不存在"navigate 到 LOCK 之后还能 Back 回 HOME"这类绕过路径。
     */
    const val LOCK = "lock"
    const val HOME = "home"
    const val SCENARIOS = "scenarios"
    const val PLAN = "plan/{planId}"
    const val TIMELINE = "timeline"
    const val REVIEW = "review"
    const val DRIFT = "drift"
    const val CANDIDATES = "candidates"
    const val INFRASTRUCTURE = "infrastructure"
    const val GRAPH = "graph"
    const val NODE = "node/{nodeId}"
    const val SOURCES = "sources"
    const val IMPORT = "import"
    const val BACKUP = "backup"
    const val RESTORE = "restore"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"
    const val ABOUT = "about"
    const val IMPACT = "impact/{nodeId}"
    const val SCENARIO_SETUP = "scenario/{templateId}"
}

/**
 * 应用根：**锁门在最外层**。
 *
 * 启动顺序（P0-A）：
 *  1. 第一帧只渲染 [LockCheckingScreen] —— 中性、不含任何用户数据；
 *     此刻 [AppLock.capability] 还没读回来，NavHost 也没有被组合。
 *  2. 读到设备能力后，一律进入 LOCKED（`LockGate` 的初值就是 true，冷启动不解锁）。
 *  3. 只有 `LockGate.unlock()` 之后才组合 [AppNavHost]。
 *
 * 因此以下绕过路径在结构上不存在（不是靠"记得别写 navigate"来保证）：
 *  - BACK 键：锁定时栈里根本没有非锁页面
 *  - 深链：NavHost 未组合（且 manifest 中除 LAUNCHER 外没有任何 intent-filter）
 *  - 直接跳转：没有任何可调用的 `nav.navigate(Route.LOCK)` 之外的入口
 *
 * 前后台：`ON_STOP` 一律回锁 —— 这是 application/security state，不触碰 Reality Graph。
 */
@Composable
fun PdigApp() {
    val context = LocalContext.current
    var capability by remember { mutableStateOf<LockCapability?>(null) }
    var checkGeneration by remember { mutableIntStateOf(0) }

    LaunchedEffect(checkGeneration) {
        // Binder 查询放到后台线程；在此之前本页不展示任何用户数据。
        capability = withContext(Dispatchers.Default) { AppLock.capability(context) }
        // 冷启动与"重新检查"都回到锁定：存在可用凭据 ≠ 本次会话已解锁。
        LockGate.lockNow()
    }

    // 回到后台立即回锁（产品策略：不设宽限期，离前台即锁）。
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        LockGate.lockNow()
    }

    val cap = capability
    when {
        cap == null -> LockCheckingScreen()
        LockGate.locked -> LockScreen(
            capability = cap,
            onUnlocked = { LockGate.unlock() },
            onRecheck = { checkGeneration += 1 },
        )
        else -> AppNavHost()
    }
}

/** 解锁后的 App 本体。锁定时它不会被组合，这是"锁不可绕过"的结构性保证。 */
@Composable
fun AppNavHost(startDestination: String = Route.HOME) {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    // 截图保护：按当前路由设置 / 清除 FLAG_SECURE（敏感页清单见 SecureWindow.kt）
    SecureWindow(route = backStackEntry?.destination?.route)
    NavHost(navController = nav, startDestination = startDestination) {
        composable(Route.ONBOARDING) { OnboardingScreen(nav) }
        composable(Route.HOME) { HomeScreen(nav) }
        composable(Route.SCENARIOS) { ScenarioCenterScreen(nav) }
        composable(Route.TIMELINE) { TimelineScreen(nav) }
        composable(Route.REVIEW) { PendingReviewScreen(nav) }
        composable(Route.DRIFT) { RealityDriftScreen(nav) }
        composable(Route.CANDIDATES) { CandidateReviewScreen(nav) }
        composable(Route.INFRASTRUCTURE) { InfrastructureScreen(nav) }
        composable(Route.GRAPH) { GraphScreen(nav) }
        composable(Route.SOURCES) { SourceManagementScreen(nav) }
        composable(Route.IMPORT) { ImportScreen(nav) }
        composable(Route.BACKUP) { BackupScreen(nav) }
        composable(Route.RESTORE) { RestoreScreen(nav) }
        composable(Route.SETTINGS) { SettingsScreen(nav) }
        composable(Route.PRIVACY) { PrivacyScreen(nav) }
        composable(Route.ABOUT) { AboutScreen(nav) }
        composable(
            Route.IMPACT,
            arguments = listOf(navArgument("nodeId") { type = NavType.StringType }),
        ) { backStack -> ImpactScreen(nav, backStack.arguments?.getString("nodeId") ?: "") }
        composable(
            Route.SCENARIO_SETUP,
            arguments = listOf(navArgument("templateId") { type = NavType.StringType }),
        ) { backStack -> ScenarioSetupScreen(nav, backStack.arguments?.getString("templateId") ?: "") }
        composable(
            Route.PLAN,
            arguments = listOf(navArgument("planId") { type = NavType.StringType }),
        ) { backStack -> ChangePlanScreen(nav, backStack.arguments?.getString("planId") ?: "") }
        composable(
            Route.NODE,
            arguments = listOf(navArgument("nodeId") { type = NavType.StringType }),
        ) { backStack -> NodeDetailScreen(nav, backStack.arguments?.getString("nodeId") ?: "") }
    }
}
