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

/**
 * Android UI vNext presentation palette.
 *
 * Desktop Dark Reference stays frozen in generated tokens. Android translates that reference into the
 * Human-selected light-first visual direction without changing Canonical/domain semantics.
 * Earth/space and asset faces retain dark local canvases so Globe and identity surfaces keep depth.
 */
object PdigV2Colors {
    val Canvas = Color(0xFFF5F8FD)
    val CanvasDeep = Color(0xFFEAF2FF)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceRaised = Color(0xFFF3F7FD)
    val SurfaceGlass = Color(0xE8FFFFFF)
    val BorderSubtle = Color(0x1F315178)
    val BorderStrong = Color(0x4D3977E8)
    val Primary = Color(0xFF2F6BFF)
    val PrimaryBright = Color(0xFF1769FF)
    val PrimarySoft = Color(0xFFE8F0FF)
    val TextPrimary = Color(0xFF10213A)
    val TextSecondary = Color(0xFF40536F)
    val TextMuted = Color(0xFF71819A)
    val Positive = Color(0xFF14966A)
    val Warning = Color(0xFFD98500)
    val Critical = Color(0xFFD94552)
    val Unknown = Color(0xFF7B8799)

    val GlobeDeep = Color(0xFF06162F)
    val GlobeTextPrimary = Color(0xFFF7FAFF)
    val GlobeTextSecondary = Color(0xFFC9D8F2)
    val AssetTextPrimary = Color(0xFFF7FAFF)
    val AssetTextSecondary = Color(0xFFD4DFF2)
    val AssetTextMuted = Color(0xFFA6B7D2)

    val RegionNodeHi = hexColor(T.COLORS_REGION_NODE_HI)
    val RegionNodeLo = rgbaColor(T.COLORS_REGION_NODE_LO)
    val ArcActive = rgbaColor(T.COLORS_ARC_ACTIVE)
    val ArcQuiet = rgbaColor(T.COLORS_ARC_QUIET)
    val AtmosphereInner = rgbaColor(T.COLORS_ATMOSPHERE_INNER)
    val AtmosphereOuter = rgbaColor(T.COLORS_ATMOSPHERE_OUTER)
    val FocusRing = PrimaryBright
}

/** 语义状态色（icon+label+color 三通道；色仅为第三通道）。 */
fun statusColor(status: String): Color = when (status) {
    "critical", "blocked", "expiring_soon" -> PdigV2Colors.Critical
    "warning", "verifying", "plan", "unresolved" -> PdigV2Colors.Warning
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
    "unresolved" -> "待处理"
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
