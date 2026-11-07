package com.pdig.uivnext.model

/**
 * Studio / Card Identity 的用户语言（consumer copy；禁止把内部 preset id 直接展示给用户）。
 *
 * 铁律（brief §13/§15）：内部 theme id 继续保留用于存储与渲染，但 UI 只显示用户语言；
 * 「开发配置器式」暴露（hex / internal enum / standard / glass ID）不得进入 Human Reference。
 */

/** Card Studio 主题：内部 id → 用户语言。 */
val CARD_THEME_LABELS: Map<String, String> = mapOf(
    "minimal" to "极简",
    "deep-space" to "深空",
    "region" to "地域",
    "city" to "城市",
    "glass" to "玻璃",
    "metal" to "金属",
    "abstract" to "抽象",
)

/** Number Studio 主题：内部 id → 用户语言。 */
val NUMBER_THEME_LABELS: Map<String, String> = mapOf(
    "country" to "国家 / 地域",
    "city" to "城市",
    "minimal" to "极简",
    "banking" to "银行验证",
    "travel" to "旅行",
    "recovery" to "恢复",
    "work" to "工作",
    "private" to "私人",
)

/** 材质（PresentationProfile.material）→ 用户语言；未知保持原样但不暴露 internal id 语义。 */
fun materialLabelZh(material: String): String = when (material) {
    "default" -> "原始"
    "glass" -> "玻璃"
    "metal" -> "金属"
    "brushed" -> "拉丝"
    "matte" -> "哑光"
    "satin" -> "缎面"
    "translucent" -> "透光"
    else -> "标准"
}

/** 布局（PresentationProfile.layout）→ 用户语言。 */
fun layoutLabelZh(layout: String): String = when (layout) {
    "standard" -> "标准"
    "compact" -> "紧凑"
    "focused" -> "聚焦"
    else -> "标准"
}

/** 主题 user 标签（card / number 按 kind 区分；未知 id 回退显示原 id 以保持诚实，不伪造）。 */
fun themeLabelZh(kind: String, id: String): String =
    (if (kind == "card") CARD_THEME_LABELS else NUMBER_THEME_LABELS)[id] ?: id

/** 安全解析 "#RRGGBB"（studio 强调色 swatch；解析失败 → null，不展示 hex 文本）。 */
fun hexColorOrNull(value: String): androidx.compose.ui.graphics.Color? =
    runCatching { com.pdig.uivnext.theme.hexColor(value) }.getOrNull()

/** Studio 可选材质（内部 id → materialLabelZh 转用户语言）。 */
val CARD_MATERIAL_CHOICES = listOf("default", "glass", "metal", "brushed", "matte", "satin")
val NUMBER_MATERIAL_CHOICES = listOf("default", "matte", "glass", "metal")


/** Studio 布局选择；全部真实进入 renderer。 */
val PRESENTATION_LAYOUT_CHOICES = listOf("standard", "compact", "focused")

/** 强调色预设：value 存入 PresentationProfile，UI 只展示名称与色样。 */
data class PresentationAccentChoice(val value: String, val label: String)

val PRESENTATION_ACCENT_CHOICES = listOf(
    PresentationAccentChoice("default", "原始"),
    PresentationAccentChoice("#4D74FF", "蓝"),
    PresentationAccentChoice("#63D7C5", "青"),
    PresentationAccentChoice("#F4B85A", "金"),
    PresentationAccentChoice("#D06A8C", "玫红"),
)

fun accentLabelZh(value: String): String =
    PRESENTATION_ACCENT_CHOICES.firstOrNull { it.value == value }?.label ?: "自定义"
