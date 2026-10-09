package com.pdig.uivnext.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.app.BuildConfig
import com.pdig.uivnext.ui.r9.R9BottomNav
import com.pdig.uivnext.ui.r9.R10TopBar
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.ui.components.MaskEnabledIndicator

/**
 * vNext 演示壳（Android 原生 Compose）。
 * 大屏（≥600dp）：NavigationRail + TopCommandBar；手机：TopCommandBar + 底部导航（5 项）。
 * [forcedViewportWidthDp] 供证据测试冻结宽度（决定 rail/bottom-nav 分支），不参与截图像素。
 */
@Composable
fun VNextShell(app: VAppState, forcedViewportWidthDp: Int? = null, onHelp: (() -> Unit)? = null) {
    // System back：detail/studio/search/change → 返回上一层；region drawer → 关闭；root → 系统退出。
    BackHandler(enabled = app.canGoBack()) { app.back() }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val viewportWidthDp: Dp = if (forcedViewportWidthDp != null) Dp(forcedViewportWidthDp.toFloat()) else maxWidth
        val breakpoint = resolveMediaBreakpoint(viewportWidthDp)
        val wide = breakpoint != MediaBreakpoint.COMPACT
        if (wide) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail(app)
                Column(Modifier.weight(1f)) {
                    TopCommandBar(app, compact = false, onHelp = onHelp)
                    if (isInfraRootScreen(app.screen)) InfraChipRow(app)
                    VNextContentHost(app, breakpoint, Modifier.weight(1f), onHelp = onHelp)
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                if (BuildConfig.FLAVOR == "preview") R10TopBar(app)
                else TopCommandBar(app, compact = true, onHelp = onHelp)
                // Phone stays focused: Infrastructure secondary destinations live in the Overview hub,
                // not in a persistent horizontal strip above every child screen.
                VNextContentHost(app, breakpoint, Modifier.weight(1f), onHelp = onHelp)
                if (isCompactRootContext(app.screen)) {
                    if (BuildConfig.FLAVOR == "preview") R9BottomNav(app) else BottomNav(app)
                }
            }
        }
    }
}

/** L2 Top Command：状态位 + 隐私遮蔽指示 + 触控可发现的搜索入口（任务书 §23）。 */
@Composable
private fun TopCommandBar(app: VAppState, compact: Boolean, onHelp: (() -> Unit)?) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(48.dp)
            .testTag(VTestIds.NAV_TOP),
        color = PdigV2Colors.Surface.copy(alpha = 0.9f),
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = if (compact) VSpacing.Lg else VSpacing.Xxl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Up in the app bar follows PRODUCT HIERARCHY on every Android
            // viewport. System back still uses chronological app.back() history.
            if (app.canNavigateUp()) {
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { app.navigateUp() },
                    color = if (compact) PdigV2Colors.Surface.copy(alpha = 0f) else PdigV2Colors.SurfaceGlass,
                    shape = RoundedCornerShape(VRadius.Sm),
                ) {
                    Row(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = if (compact) 0.dp else VSpacing.Lg),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowLeft,
                            contentDescription = "返回",
                            tint = PdigV2Colors.TextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.width(VSpacing.Sm))
            }
            if (compact && app.screen == VScreen.NOW) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pdig.nav.top.brand"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(28.dp),
                        color = PdigV2Colors.PrimaryBright,
                        shape = RoundedCornerShape(VRadius.Sm),
                    ) {
                        Row(
                            Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text("P", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.width(VSpacing.Sm))
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        Text("PDIG", color = PdigV2Colors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        if (onHelp != null) {
                            Text("R9 · ${BuildConfig.GIT_SHA}", color = PdigV2Colors.PrimaryText, fontSize = 9.sp, maxLines = 1)
                        }
                    }
                }
            } else {
                Text(
                    if (compact) compactTopTitle(app.screen) else app.screen.titleZh,
                    color = PdigV2Colors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pdig.nav.top.title"),
                )
                if (compact && BuildConfig.FLAVOR == "preview") {
                    Text("R9", fontSize = 9.sp, color = PdigV2Colors.PrimaryBright,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 3.dp))
                }
            }
            if ((!compact || isCompactRootContext(app.screen)) && app.screen != VScreen.SEARCH) {
                Surface(
                    modifier = (if (compact) {
                        Modifier.size(48.dp)
                    } else {
                        Modifier
                            .widthIn(min = 220.dp, max = 380.dp)
                            .defaultMinSize(minHeight = 48.dp)
                    })
                        .clickable { app.navigate(VScreen.SEARCH) }
                        .testTag("pdig.search.entry"),
                    color = if (compact) PdigV2Colors.Surface.copy(alpha = 0f) else PdigV2Colors.SurfaceRaised,
                    shape = RoundedCornerShape(VRadius.Sm),
                    border = if (compact) null else BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                ) {
                    Row(
                        Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "搜索与快捷操作",
                            tint = PdigV2Colors.TextMuted,
                            modifier = Modifier.size(18.dp),
                        )
                        if (!compact) {
                            Spacer(Modifier.width(VSpacing.Sm))
                            Text(
                                "搜索卡片、号码、地区或服务",
                                color = PdigV2Colors.TextMuted,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(VSpacing.Sm))
            }

            if (
                (!compact || isCompactRootContext(app.screen)) &&
                app.screen != VScreen.SETTINGS &&
                app.screen != VScreen.PERSONALIZATION
            ) {
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { app.openUtility(VScreen.SETTINGS) }
                        .testTag("pdig.settings.entry"),
                    color = if (compact) PdigV2Colors.Surface.copy(alpha = 0f) else PdigV2Colors.SurfaceGlass,
                    shape = RoundedCornerShape(VRadius.Sm),
                ) {
                    Row(
                        Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "设置",
                            tint = PdigV2Colors.TextMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(Modifier.width(VSpacing.Sm))
            }

            if (onHelp != null && (!compact || isCompactRootContext(app.screen))) {
                Surface(
                    modifier = Modifier.size(48.dp)
                        .clickable(onClick = onHelp)
                        .testTag("pdig.onboarding.reopen"),
                    color = Color.Transparent,
                    shape = RoundedCornerShape(VRadius.Sm),
                ) {
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = "重新查看新手引导",
                            tint = PdigV2Colors.PrimaryText,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            if (compact) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = if (app.privacyMask) "隐私遮蔽已开启" else "隐私遮蔽已关闭",
                    tint = if (app.privacyMask) PdigV2Colors.PrimaryBright else PdigV2Colors.TextMuted,
                    modifier = Modifier.size(16.dp),
                )
            } else {
                MaskEnabledIndicator(app.privacyMask)
            }

        }
    }
}

internal fun resolveMediaBreakpoint(viewportWidthDp: Dp): MediaBreakpoint = when {
    viewportWidthDp >= 840.dp -> MediaBreakpoint.EXPANDED
    viewportWidthDp >= 600.dp -> MediaBreakpoint.MEDIUM
    else -> MediaBreakpoint.COMPACT
}

private fun compactTopTitle(screen: VScreen): String = when (screen) {
    VScreen.INFRASTRUCTURE, VScreen.OVERVIEW -> "基础设施"
    VScreen.CHANGE, VScreen.CHANGE_PHONE -> "更换手机号"
    else -> screen.titleZh
}

private fun isCompactRootContext(screen: VScreen): Boolean = screen in setOf(
    VScreen.NOW,
    VScreen.INFRASTRUCTURE,
    VScreen.CHANGE,
    VScreen.OVERVIEW,
    VScreen.CARDS,
    VScreen.NUMBERS,
    VScreen.ACCOUNTS,
    VScreen.EMAILS,
    VScreen.DEVICES,
    VScreen.SERVICES,
    VScreen.WEAKNESSES,
    VScreen.RECORDS,
    VScreen.ME,
)
