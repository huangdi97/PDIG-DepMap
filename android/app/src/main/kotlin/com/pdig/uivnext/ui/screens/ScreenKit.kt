package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.regionLabelZh
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VTouchTarget
import com.pdig.uivnext.ui.VAppState

/** 屏幕共享 UI 助手。 */
internal fun Modifier.testTagLocal(tag: String): Modifier = this.then(testTag(tag))

internal fun Modifier.clickableLocal(onClick: () -> Unit): Modifier = this.then(clickable(onClick = onClick))

/** Android reference 的统一页面节奏：Phone 紧凑，Tablet 保留更宽呼吸区。 */
internal fun pagePadding(breakpoint: MediaBreakpoint): Dp =
    if (breakpoint == MediaBreakpoint.COMPACT) 16.dp else 24.dp

internal fun pageTitleSize(breakpoint: MediaBreakpoint): TextUnit =
    if (breakpoint == MediaBreakpoint.COMPACT) 22.sp else 26.sp

internal fun pageSectionGap(breakpoint: MediaBreakpoint): Dp =
    if (breakpoint == MediaBreakpoint.COMPACT) 16.dp else 20.dp

/** Consumer-facing 地区名；内部继续使用稳定 regionCode。 */
internal fun regionLabel(code: String): String = regionLabelZh(code)

/** Global Infrastructure Navigator 的地区上下文；显式允许返回全球。 */
@Composable
internal fun RegionScopeBanner(app: VAppState) {
    val code = app.regionFilter ?: return
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = VTouchTarget.Min)
            .clickableLocal { app.clearRegion() },
        color = PdigV2Colors.PrimarySoft.copy(alpha = 0.72f),
        shape = RoundedCornerShape(VRadius.Md),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "当前地区 · " + regionLabel(code),
                color = PdigV2Colors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text("查看全球 →", color = PdigV2Colors.PrimaryBright, fontSize = 12.sp)
        }
    }
}


/**
 * Light-reference infrastructure object surface.
 *
 * Secondary infrastructure pages must still feel like an asset system rather than a settings list:
 * identity badge + strong title/subtitle + semantic trailing state + supporting facts.
 */
@Composable
internal fun LightObjectCard(
    title: String,
    subtitle: String,
    badge: String,
    accent: Color = PdigV2Colors.Primary,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val base = Modifier
        .fillMaxWidth()
        .testTagLocal("pdig.infra.object-card")
    val interactive = if (onClick != null) base.clickableLocal(onClick) else base
    Surface(
        modifier = interactive,
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.16f)),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    color = accent.copy(alpha = 0.11f),
                    shape = RoundedCornerShape(VRadius.Md),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.18f)),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            badge,
                            color = accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        color = PdigV2Colors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        subtitle,
                        color = PdigV2Colors.TextMuted,
                        fontSize = 11.sp,
                    )
                }
                if (trailing != null) {
                    Spacer(Modifier.width(10.dp))
                    trailing()
                }
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}
