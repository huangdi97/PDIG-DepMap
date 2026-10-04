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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.ui.components.MaskEnabledIndicator

/**
 * vNext 演示壳（Android 原生 Compose）。
 * 大屏（≥600dp）：NavigationRail + TopCommandBar；手机：TopCommandBar + 底部导航（4 项 ≤5）。
 * [forcedViewportWidthDp] 供证据测试冻结宽度（决定 rail/bottom-nav 分支），不参与截图像素。
 */
@Composable
fun VNextShell(app: VAppState, forcedViewportWidthDp: Int? = null) {
    // System back：detail/studio/search/change → 返回上一层；region drawer → 关闭；root → 系统退出。
    BackHandler(enabled = app.canGoBack()) { app.back() }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val viewportWidthDp: Dp = if (forcedViewportWidthDp != null) Dp(forcedViewportWidthDp.toFloat()) else maxWidth
        val wide = viewportWidthDp >= 600.dp
        val breakpoint = when {
            viewportWidthDp >= 1200.dp -> MediaBreakpoint.EXPANDED
            viewportWidthDp >= 700.dp -> MediaBreakpoint.MEDIUM
            else -> MediaBreakpoint.COMPACT
        }
        if (wide) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail(app)
                Column(Modifier.weight(1f)) {
                    TopCommandBar(app, compact = false)
                    if (isInfraRootScreen(app.screen)) InfraChipRow(app, compact = false)
                    VNextContentHost(app, breakpoint)
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                TopCommandBar(app, compact = true)
                if (isInfraRootScreen(app.screen)) InfraChipRow(app, compact = true)
                VNextContentHost(app, breakpoint, Modifier.weight(1f))
                BottomNav(app)
            }
        }
    }
}

/** L2 Top Command：状态位 + 隐私遮蔽指示 + 触控可发现的搜索入口（任务书 §23）。 */
@Composable
private fun TopCommandBar(app: VAppState, compact: Boolean) {
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
            if (app.canGoBack()) {
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { app.back() },
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
            Text(
                app.screen.titleZh,
                color = PdigV2Colors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (app.screen != VScreen.SEARCH) {
                Surface(
                    modifier = (if (compact) Modifier.size(48.dp) else Modifier.defaultMinSize(minHeight = 48.dp))
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
                            Text("搜索", color = PdigV2Colors.TextMuted, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.width(VSpacing.Sm))
            }

            if (app.screen != VScreen.SETTINGS && app.screen != VScreen.PERSONALIZATION) {
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
