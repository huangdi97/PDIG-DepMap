package com.pdig.uivnext.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pdig.uivnext.generated.GeneratedPdigV2Tokens as T

/** 解析 "#RRGGBB" 十六进制色。 */
fun hexColor(value: String): Color {
    val cleaned = value.removePrefix("#")
    require(cleaned.length == 6) { "unsupported hex color: $value" }
    val rgb = cleaned.toLong(16)
    return Color(0xFF000000 or rgb)
}

/** 解析 "rgba(r, g, b, a)" 字符串（token surfaceGlass 等）。 */
fun rgbaColor(value: String): Color {
    val inner = value.removePrefix("rgba(").removeSuffix(")").split(",").map { it.trim() }
    require(inner.size == 4) { "unsupported rgba color: $value" }
    return Color(
        red = inner[0].toFloat() / 255f,
        green = inner[1].toFloat() / 255f,
        blue = inner[2].toFloat() / 255f,
        alpha = inner[3].toFloat(),
    )
}

/** vNext 环境/数据/状态色（全部来自 GeneratedPdigV2Tokens，页面不得散落魔数颜色）。 */
object PdigV2Colors {
    val Canvas = hexColor(T.COLORS_CANVAS)
    val CanvasDeep = hexColor(T.COLORS_CANVAS_DEEP)
    val Surface = hexColor(T.COLORS_SURFACE)
    val SurfaceRaised = hexColor(T.COLORS_SURFACE_RAISED)
    val SurfaceGlass = rgbaColor(T.COLORS_SURFACE_GLASS)
    val BorderSubtle = rgbaColor(T.COLORS_BORDER_SUBTLE)
    val BorderStrong = rgbaColor(T.COLORS_BORDER_STRONG)
    val Primary = hexColor(T.COLORS_PRIMARY)
    val PrimaryBright = hexColor(T.COLORS_PRIMARY_BRIGHT)
    val PrimarySoft = hexColor(T.COLORS_PRIMARY_SOFT)
    val TextPrimary = hexColor(T.COLORS_TEXT_PRIMARY)
    val TextSecondary = hexColor(T.COLORS_TEXT_SECONDARY)
    val TextMuted = hexColor(T.COLORS_TEXT_MUTED)
    val Positive = hexColor(T.COLORS_POSITIVE)
    val Warning = hexColor(T.COLORS_WARNING)
    val Critical = hexColor(T.COLORS_CRITICAL)
    val Unknown = hexColor(T.COLORS_UNKNOWN)
    val RegionNodeHi = hexColor(T.COLORS_REGION_NODE_HI)
    val RegionNodeLo = rgbaColor(T.COLORS_REGION_NODE_LO)
    val ArcActive = rgbaColor(T.COLORS_ARC_ACTIVE)
    val ArcQuiet = rgbaColor(T.COLORS_ARC_QUIET)
    val AtmosphereInner = rgbaColor(T.COLORS_ATMOSPHERE_INNER)
    val AtmosphereOuter = rgbaColor(T.COLORS_ATMOSPHERE_OUTER)
    // semantic.focus = colors.primaryBright（别名；无独立色值）
    val FocusRing = PrimaryBright
}

/** 语义状态色（icon+label+color 三通道；色仅为第三通道）。 */
fun statusColor(status: String): Color = when (status) {
    "critical", "blocked", "expiring_soon" -> PdigV2Colors.Critical
    "warning", "verifying" -> PdigV2Colors.Warning
    "completed", "active", "ok", "verified", "migrated" -> PdigV2Colors.Positive
    else -> PdigV2Colors.TextMuted
}

/** 状态中文标签与状态码的映射表（文案进 copy 资源；此处为 vNext 演示固定文案）。 */
fun statusLabelZh(status: String): String = when (status) {
    "active" -> "使用中"
    "expiring_soon" -> "即将到期"
    "expired" -> "已过期"
    "blocked" -> "阻止"
    "critical" -> "必须处理"
    "warning" -> "需要确认"
    "verifying" -> "待验证"
    "completed" -> "已完成"
    "not_started" -> "未开始"
    "waiting" -> "等待中"
    "migrated" -> "已迁移"
    "plan" -> "计划"
    else -> "未知"
}

/** 数值间距 token 的 dp 便捷函数。 */
object VSpacing {
    val Xs = T.SPACING_XS.dp
    val Sm = T.SPACING_SM.dp
    val Md = T.SPACING_MD.dp
    val Lg = T.SPACING_LG.dp
    val Xl = T.SPACING_XL.dp
    val Xxl = T.SPACING_XXL.dp
    val Xxxl = T.SPACING_XXXL.dp
    val PagePadding = T.SPACING_PAGE_PADDING.dp
    val SectionGap = T.SPACING_SECTION_GAP.dp
    val GridGap = T.SPACING_GRID_GAP.dp
}

object VRadius {
    val Sm = T.RADIUS_SM.dp
    val Md = T.RADIUS_MD.dp
    val Lg = T.RADIUS_LG.dp
    val Xl = T.RADIUS_XL.dp
}

/** 触控目标最小尺寸（DESIGN_TOKENS components.touchTarget.android = 48dp）。 */
object VTouchTarget {
    val Min = T.COMPONENTS_TOUCH_TARGET_ANDROID.dp
    val Dp: Dp = Min
}
