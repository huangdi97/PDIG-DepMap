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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VSection
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
                    TopCommandBar(app)
                    VNextContentHost(app, breakpoint)
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                TopCommandBar(app)
                if (app.screen.section == VSection.INFRA) InfraChipRow(app)
                VNextContentHost(app, breakpoint, Modifier.weight(1f))
                BottomNav(app)
            }
        }
    }
}

/** L2 Top Command：状态位 + 隐私遮蔽指示 + 触控可发现的搜索入口（任务书 §23）。 */
@Composable
private fun TopCommandBar(app: VAppState) {
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
                .padding(horizontal = VSpacing.Xxl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(app.screen.titleZh, color = PdigV2Colors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            if (app.screen != VScreen.SEARCH) {
                Surface(
                    modifier = Modifier
                        .clickable { app.navigate(VScreen.SEARCH) }
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("pdig.search.entry"),
                    color = PdigV2Colors.SurfaceRaised,
                    shape = RoundedCornerShape(VRadius.Sm),
                    border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                ) {
                    Row(
                        Modifier.padding(horizontal = VSpacing.Lg, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "搜索与快捷操作",
                            tint = PdigV2Colors.TextMuted,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(VSpacing.Sm))
                        Text("搜索", color = PdigV2Colors.TextMuted, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.width(VSpacing.Lg))
            }
            MaskEnabledIndicator(app.privacyMask)
        }
    }
}
