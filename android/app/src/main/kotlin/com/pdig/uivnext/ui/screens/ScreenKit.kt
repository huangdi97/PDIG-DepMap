package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
