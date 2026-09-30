package com.pdig.uivnext.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * vNext 环境/数据/状态/地球色（全部来自 GeneratedPdigV2Tokens，页面不得散落魔数颜色）。
 * earth.* 语义别名来自 DESIGN_TOKENS.json semantic.earth。
 */
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
    // semantic.earth 别名（真实地球渲染）
    val OceanBase = hexColor(T.COLORS_OCEAN_BASE)
    val OceanDeep = hexColor(T.COLORS_OCEAN_DEEP)
    val LandBase = hexColor(T.COLORS_LAND_BASE)
    val LandHighlight = hexColor(T.COLORS_LAND_HIGHLIGHT)
    val NightCityLight = hexColor(T.COLORS_NIGHT_CITY_LIGHT)
    val NightCityGlow = rgbaColor(T.COLORS_NIGHT_CITY_GLOW)
    val Star = hexColor(T.COLORS_STAR)
    val AtmosphereRim = rgbaColor(T.COLORS_ATMOSPHERE_RIM)
    val TerminatorLight = hexColor(T.COLORS_TERMINATOR_LIGHT)
    val OceanSpecular = hexColor(T.COLORS_OCEAN_SPECULAR)
    val LandTextureHi = hexColor(T.COLORS_LAND_TEXTURE_HI)
    val LandTextureLo = hexColor(T.COLORS_LAND_TEXTURE_LO)
    val Cloud = hexColor(T.COLORS_CLOUD)
    val LocalIllum = hexColor(T.COLORS_LOCAL_ILLUM)
}

/** 语义状态色（icon+label+color 三通道；色仅为第三通道）。 */
fun statusColor(status: String): Color = when (status) {
    "critical", "blocked", "expiring_soon" -> PdigV2Colors.Critical
    "warning", "verifying", "manual" -> PdigV2Colors.Warning
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
    "manual" -> "手动处理"
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
    val Xxxxl = T.SPACING_XXXXL.dp
    val PagePadding = T.SPACING_PAGE_PADDING.dp
    val PagePaddingNarrow = T.SPACING_PAGE_PADDING_NARROW.dp
    val SectionGap = T.SPACING_SECTION_GAP.dp
    val SectionGapWide = T.SPACING_SECTION_GAP_WIDE.dp
    val GridGap = T.SPACING_GRID_GAP.dp
    val GridGapWide = T.SPACING_GRID_GAP_WIDE.dp
}

object VRadius {
    val Sm = T.RADIUS_SM.dp
    val Md = T.RADIUS_MD.dp
    val Lg = T.RADIUS_LG.dp
    val Xl = T.RADIUS_XL.dp
    val Xl2 = T.RADIUS_XL2.dp
    val Pill = T.RADIUS_PILL.dp
}

/**
 * vNext 字阶（DESIGN_TOKENS.json typography）。
 * Desktop 1920 基准：pageTitle 36 / majorNumber 30 / sectionTitle 20 / body 16 /
 * secondary 14 / label 13 / meta 12（仅少量 metadata）/ mono 14。
 */
object VType {
    val PageTitle = TextStyle(
        fontSize = T.TYPOGRAPHY_PAGE_TITLE_SIZE.sp,
        fontWeight = FontWeight.W700,
        lineHeight = (T.TYPOGRAPHY_PAGE_TITLE_SIZE * T.TYPOGRAPHY_PAGE_TITLE_LINE_HEIGHT).sp,
    )
    val MajorNumber = TextStyle(
        fontSize = T.TYPOGRAPHY_MAJOR_NUMBER_SIZE.sp,
        fontWeight = FontWeight.W700,
        lineHeight = (T.TYPOGRAPHY_MAJOR_NUMBER_SIZE * T.TYPOGRAPHY_MAJOR_NUMBER_LINE_HEIGHT).sp,
    )
    val SectionTitle = TextStyle(
        fontSize = T.TYPOGRAPHY_SECTION_TITLE_SIZE.sp,
        fontWeight = FontWeight.W600,
        lineHeight = (T.TYPOGRAPHY_SECTION_TITLE_SIZE * T.TYPOGRAPHY_SECTION_TITLE_LINE_HEIGHT).sp,
    )
    val Body = TextStyle(
        fontSize = T.TYPOGRAPHY_BODY_SIZE.sp,
        fontWeight = FontWeight.W400,
        lineHeight = (T.TYPOGRAPHY_BODY_SIZE * T.TYPOGRAPHY_BODY_LINE_HEIGHT).sp,
    )
    val Secondary = TextStyle(
        fontSize = T.TYPOGRAPHY_SECONDARY_SIZE.sp,
        fontWeight = FontWeight.W400,
        lineHeight = (T.TYPOGRAPHY_SECONDARY_SIZE * T.TYPOGRAPHY_SECONDARY_LINE_HEIGHT).sp,
    )
    val Meta = TextStyle(
        fontSize = T.TYPOGRAPHY_META_SIZE.sp,
        fontWeight = FontWeight.W400,
        lineHeight = (T.TYPOGRAPHY_META_SIZE * T.TYPOGRAPHY_META_LINE_HEIGHT).sp,
    )
    val Label = TextStyle(
        fontSize = T.TYPOGRAPHY_LABEL_SIZE.sp,
        fontWeight = FontWeight.W600,
        lineHeight = (T.TYPOGRAPHY_LABEL_SIZE * T.TYPOGRAPHY_LABEL_LINE_HEIGHT).sp,
    )
    val StatusLabel = TextStyle(
        fontSize = T.TYPOGRAPHY_STATUS_LABEL_SIZE.sp,
        fontWeight = FontWeight.W600,
        lineHeight = (T.TYPOGRAPHY_STATUS_LABEL_SIZE * T.TYPOGRAPHY_STATUS_LABEL_LINE_HEIGHT).sp,
    )
    val DisplayGlobe = TextStyle(
        fontSize = T.TYPOGRAPHY_DISPLAY_GLOBE_SIZE.sp,
        fontWeight = FontWeight.W600,
        lineHeight = (T.TYPOGRAPHY_DISPLAY_GLOBE_SIZE * T.TYPOGRAPHY_DISPLAY_GLOBE_LINE_HEIGHT).sp,
    )
    val Mono = TextStyle(
        fontSize = T.TYPOGRAPHY_MONO_SIZE.sp,
        fontWeight = FontWeight.W400,
        lineHeight = (T.TYPOGRAPHY_MONO_SIZE * T.TYPOGRAPHY_MONO_LINE_HEIGHT).sp,
        fontFamily = FontFamily.Monospace,
    )
}

/** 强调色可选项（全部来自 token palette；禁止页面自造色）。 */
val ACCENT_SWATCHES: List<Pair<String, Color>> = listOf(
    "primary" to PdigV2Colors.Primary,
    "primaryBright" to PdigV2Colors.PrimaryBright,
    "terminatorLight" to PdigV2Colors.TerminatorLight,
    "nightCityLight" to PdigV2Colors.NightCityLight,
    "positive" to PdigV2Colors.Positive,
    "warning" to PdigV2Colors.Warning,
)
