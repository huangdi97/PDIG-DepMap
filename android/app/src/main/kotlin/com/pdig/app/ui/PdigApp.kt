package com.pdig.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
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
import com.pdig.app.ui.screens.FindingsScreen
import com.pdig.app.ui.screens.GraphScreen
import com.pdig.app.ui.screens.HomeScreen
import com.pdig.app.ui.screens.ImpactScreen
import com.pdig.app.ui.screens.ImportScreen
import com.pdig.app.ui.screens.InfrastructureScreen
import com.pdig.app.ui.screens.LockCheckingScreen
import com.pdig.app.ui.screens.LockScreen
import com.pdig.app.ui.screens.NodeDetailScreen
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
    /**
     * ONBOARDING：产品决策（2026-09-19 Android Product Finalization）正式取消 ——
     * 不再注册该路由，避免"页面存在但无入口"的 ghost 状态（见 ANDROID_FINAL_73_AUDIT.md / D-条款）。
     */


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
    const val FINDINGS = "findings"
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
 *  - 深链：NavHost 未组合（且 manifest 中除 LAUNCHER 外没有任何 intent-filter）
 *  - 直接跳转：没有任何可调用的 `nav.navigate(Route.LOCK)` 之外的入口
 *
 * 前后台：`ON_STOP` 一律回锁 —— 这是 application/security state，不触碰 Reality Graph。
 *
 * ---------------------------------------------------------------------------
 * ## D-16：导航位置也要跨锁存活
 *
 * 「锁定时不组合 NavHost」在安全上是正确的，但副作用是：用户从导入向导切到
 * 外部文件选择器 → 回锁 → 解锁后回到的是 `startDestination = HOME`，
 * **不是**他离开时的那个向导页。
 *
 * 这里把"当前导航链"记在 [PdigApp] 的 `remember` 里 —— [PdigApp] 本身
 * **不会因为锁定而离开组合树**（只有 NavHost 那一支会），所以它能跨锁存活；
 * 解锁后 [AppNavHost] 依据它把栈重建回来。
 * ---------------------------------------------------------------------------
 */
@Composable
fun PdigApp() {
    val context = LocalContext.current
    var capability by remember { mutableStateOf<LockCapability?>(null) }
    var checkGeneration by remember { mutableIntStateOf(0) }

    // 跨锁保持的导航链（只记录**无参数**路由：带 {arg} 的路由无法在缺少实参时重建）。
    var routeChain by remember { mutableStateOf<List<String>>(emptyList()) }

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

    // 回到前台**重新读一次**设备能力再锁。
    //
    // 真实缺陷（2026-09-17 取证发现）：能力原本只在冷启动读一次，之后一直缓存。
    // 于是出现这样一条可复现的 fail-closed 失效路径 ——
    //   1. 设备无凭据时冷启动 App → 能力被缓存为「无凭据」，锁屏显示
    //      「已知悉风险，本次进入」；
    //   2. 用户（或脚本）此时去系统设置里设了锁屏 PIN，App 进程没有被杀；
    //   3. 再回到前台，App 用的还是旧能力 → **继续用手动放行按钮让用户进 HOME**，
    //      而设备此刻明明已经可以验证身份。
    // 决定"能不能进"这件事，必须在**决策的当下**依据设备的当前状态，
    // 不能依赖进程启动时的一次快照。
    //
    // 用 `ON_RESUME` 而不是 `ON_START`：`ON_START` 在"App 本就已经在前台"时
    // 不会再次派发，而 `ON_RESUME` 在每一次真正回到前台都会触发
    // （2026-09-17 实测：`am start` 一个已经前台的 Activity 不会重发 ON_START，
    //   于是能力仍然没被刷新，锁屏继续用旧能力放行用户）。
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        checkGeneration += 1
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
        else -> AppNavHost(
            resumeChain = routeChain,
            onDestinationChanged = { route -> routeChain = foldRoute(routeChain, route) },
        )
    }
}

/**
 * 把一次目的地变化折叠进导航链：
 *  - 已在链中出现过 ⇒ 视为**回退**到该层，截断其后所有层
 *  - 否则 ⇒ 视为**前进**，追加
 *  - 带 `{arg}` 的路由（如 `node/{nodeId}`）无法在缺少实参时重建，**跳过**
 */
internal fun foldRoute(chain: List<String>, route: String): List<String> {
    if (route.contains("{")) return chain
    if (chain.lastOrNull() == route) return chain
    val existing = chain.indexOfLast { it == route }
    if (existing >= 0) return chain.subList(0, existing + 1)
    return chain + route
}

/** 解锁后的 App 本体。锁定时它不会被组合，这是"锁不可绕过"的结构性保证。 */
@Composable
fun AppNavHost(
    resumeChain: List<String> = emptyList(),
    onDestinationChanged: (String) -> Unit = {},
) {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    // 截图保护：按当前路由设置 / 清除 FLAG_SECURE（敏感页清单见 SecureWindow.kt）
    SecureWindow(route = backStackEntry?.destination?.route)

    // startDestination 必须**冻结**在首次组合时的值：resumeChain 在组合期间还会继续变化，
    // 而 NavHost 的 startDestination 一旦变化就会重建整张 graph。
    val startDestination = remember {
        resumeChain.firstOrNull()?.takeIf { it.isNotBlank() } ?: Route.HOME
    }

    // 解锁后把用户离开时的导航栈补回来（D-16）。
    // 只补第一层之后的层级：第一层已经作为 startDestination 生效。
    var restored by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (restored) return@LaunchedEffect
        restored = true
        resumeChain.drop(1).forEach { route ->
            runCatching { nav.navigate(route) }
        }
    }

    val onRoute = rememberUpdatedState(onDestinationChanged)
    DisposableEffect(nav) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            val route = destination.route
            if (!route.isNullOrBlank()) onRoute.value.invoke(route)
        }
        nav.addOnDestinationChangedListener(listener)
        onDispose { nav.removeOnDestinationChangedListener(listener) }
    }

    NavHost(navController = nav, startDestination = startDestination) {
        composable(Route.HOME) { HomeScreen(nav) }
        composable(Route.SCENARIOS) { ScenarioCenterScreen(nav) }
        composable(Route.TIMELINE) { TimelineScreen(nav) }
        composable(Route.REVIEW) { PendingReviewScreen(nav) }
        composable(Route.DRIFT) { RealityDriftScreen(nav) }
        composable(Route.CANDIDATES) { CandidateReviewScreen(nav) }
        composable(Route.INFRASTRUCTURE) { InfrastructureScreen(nav) }
        composable(Route.FINDINGS) { FindingsScreen(nav) }
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
