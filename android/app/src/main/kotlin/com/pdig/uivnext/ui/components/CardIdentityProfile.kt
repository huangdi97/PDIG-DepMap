package com.pdig.uivnext.ui.components

import androidx.compose.ui.graphics.Color
import com.pdig.uivnext.theme.hexColor

/**
 * Card Identity System —— 冻结的 Desktop CardIdentitySystem 的 Android 翻译。
 *
 * 铁律（brief §5 / CANONICAL designSystem.cards）：
 *  - 每个 issuer 拥有独立 identity（palette / material / motif / accent / layout / regional mark），
 *    绝不退化为「同一蓝色渐变 + 文字」；
 *  - 所有视觉来自 bundled local / procedural 合成资产；禁止下载真实银行 logo；
 *  - 同一 CardIdentityProfile 由 Grid / Card Detail / Card Studio 通过唯一 renderer 复现
 *    （CardIdentityRenderer.kt 的 CardIdentityFace，禁止三处各自实现视觉）。
 *
 * 这是纯 Presentation 层：不影响依赖 / 证据 / 确认，也不写入 .depmap。
 */

/** 卡面材质（identity 维度之一；consumer 标签见 [CardMaterial.labelZh]）。 */
enum class CardMaterial(val labelZh: String) {
    GLASS("玻璃"),
    METAL("金属"),
    BRUSHED("拉丝"),
    MATTE("哑光"),
    SATIN("缎面"),
    TRANSLUCENT("透光"),
}

/** 卡面主 motif（identity 维度之一；每种几何可被 Human 一眼区分）。 */
enum class CardMotif {
    RING,             // CMB：铜色环形 motif
    RED_LINE,         // ICBC：石墨底 + 红色线
    CONTOUR,          // BOC：克制金属轮廓
    WARM_WINDOW,      // HSBC：暖色窗口 / 城市夜景
    GEO_CONTOUR,      // BOCHK：地理等高线
    CORAL_ARC,        // Monzo：炭黑 + 珊瑚弧
    DISPERSION,       // Revolut：玻璃弥散
    BRUSHED_STRUCTURE, // Chase：拉丝结构
}

/** 卡面版式（identity 维度之一）。 */
enum class CardLayout(val labelZh: String) {
    STANDARD("标准"),
    EDGE_BAND("侧边带"),
    SPLIT_BAND("分栏带"),
    TOP_LIGHT("顶部光带"),
}

/** 单一 issuer identity profile：至少 4 个 identity 维度同时携带（palette / material / motif / accent / layout / regional）。 */
data class CardIdentityProfile(
    val issuerKey: String,
    val issuerLabelZh: String,
    val palette: List<Color>,
    val material: CardMaterial,
    val motif: CardMotif,
    val accent: Color,
    val layout: CardLayout,
    val regionalMark: String,
) {
    /** 供契约测试比较的 identity 维度序列（序稳定、可判等）。 */
    fun identityDimensions(): List<String> = listOf(
        "palette:${palette.joinToString(",") { it.value.toString(16) }}",
        "material:${material.name}",
        "motif:${motif.name}",
        "accent:${accent.value.toString(16)}",
        "layout:${layout.name}",
        "regional:${regionalMark}",
    )

    companion object {
        private fun color(hex: String) = hexColor(hex)

        /** 冻结方向（DESKTOP_REFERENCE_FREEZE §CardIdentitySystem）：wine/copper/ring。 */
        val CMB = CardIdentityProfile(
            issuerKey = "cmb",
            issuerLabelZh = "招商银行",
            palette = listOf(color("#5A1F3C"), color("#3A1230"), color("#1D0B1C")),
            material = CardMaterial.SATIN,
            motif = CardMotif.RING,
            accent = color("#C9A227"),
            layout = CardLayout.EDGE_BAND,
            regionalMark = "中国 · 深圳",
        )

        /** 冻结方向：graphite / red line / brushed。 */
        val ICBC = CardIdentityProfile(
            issuerKey = "icbc",
            issuerLabelZh = "中国工商银行",
            palette = listOf(color("#2A2E38"), color("#1B1E26"), color("#101218")),
            material = CardMaterial.BRUSHED,
            motif = CardMotif.RED_LINE,
            accent = color("#E0453A"),
            layout = CardLayout.TOP_LIGHT,
            regionalMark = "中国 · 北京",
        )

        /** 冻结方向：cool graphite / restrained metallic。 */
        val BOC = CardIdentityProfile(
            issuerKey = "boc",
            issuerLabelZh = "中国银行",
            palette = listOf(color("#23303C"), color("#152028"), color("#0B1216")),
            material = CardMaterial.MATTE,
            motif = CardMotif.CONTOUR,
            accent = color("#8FA8BC"),
            layout = CardLayout.STANDARD,
            regionalMark = "中国 · 北京",
        )

        /** 冻结方向：city / warm-window / Hong Kong night。 */
        val HSBC = CardIdentityProfile(
            issuerKey = "hsbc",
            issuerLabelZh = "HSBC 汇丰",
            palette = listOf(color("#2E1F3D"), color("#1A1226"), color("#0E0A18")),
            material = CardMaterial.GLASS,
            motif = CardMotif.WARM_WINDOW,
            accent = color("#F2B25C"),
            layout = CardLayout.SPLIT_BAND,
            regionalMark = "香港",
        )

        /** 冻结方向：blue / geographic contour。 */
        val BOCHK = CardIdentityProfile(
            issuerKey = "bochk",
            issuerLabelZh = "中银香港",
            palette = listOf(color("#123A5E"), color("#0B2740"), color("#051625")),
            material = CardMaterial.MATTE,
            motif = CardMotif.GEO_CONTOUR,
            accent = color("#3FA7FF"),
            layout = CardLayout.STANDARD,
            regionalMark = "香港",
        )

        /** 冻结方向：charcoal / coral accent。 */
        val MONZO = CardIdentityProfile(
            issuerKey = "monzo",
            issuerLabelZh = "Monzo",
            palette = listOf(color("#1F2229"), color("#14161C"), color("#0C0E12")),
            material = CardMaterial.MATTE,
            motif = CardMotif.CORAL_ARC,
            accent = color("#FF5A5F"),
            layout = CardLayout.EDGE_BAND,
            regionalMark = "英国",
        )

        /** 冻结方向：glass / dispersion。 */
        val REVOLUT = CardIdentityProfile(
            issuerKey = "revolut",
            issuerLabelZh = "Revolut",
            palette = listOf(color("#2A3350"), color("#1A2136"), color("#10131F")),
            material = CardMaterial.GLASS,
            motif = CardMotif.DISPERSION,
            accent = color("#9FB6FF"),
            layout = CardLayout.SPLIT_BAND,
            regionalMark = "英国",
        )

        /** 冻结方向：navy / brushed structure。 */
        val CHASE = CardIdentityProfile(
            issuerKey = "chase",
            issuerLabelZh = "Chase",
            palette = listOf(color("#14263F"), color("#0C1A2E"), color("#06101C")),
            material = CardMaterial.BRUSHED,
            motif = CardMotif.BRUSHED_STRUCTURE,
            accent = color("#7FB3FF"),
            layout = CardLayout.TOP_LIGHT,
            regionalMark = "美国",
        )

        /** 其余 issuer 的合成身份（不在 8 个冻结 issuer 集内；仍禁止统一蓝色渐变）。 */
        val NEUTRAL = CardIdentityProfile(
            issuerKey = "neutral",
            issuerLabelZh = "其他",
            palette = listOf(color("#1B3A6B"), color("#0B1A33")),
            material = CardMaterial.MATTE,
            motif = CardMotif.CONTOUR,
            accent = color("#4D74FF"),
            layout = CardLayout.STANDARD,
            regionalMark = "",
        )

        val CAPITAL_ONE = NEUTRAL.copy(
            issuerKey = "capital-one",
            issuerLabelZh = "Capital One",
            palette = listOf(color("#1A2E4A"), color("#0D1A2E")),
            accent = color("#6B9BFF"),
        )

        val DBS = NEUTRAL.copy(
            issuerKey = "dbs",
            issuerLabelZh = "DBS",
            palette = listOf(color("#0E4D46"), color("#072722")),
            motif = CardMotif.GEO_CONTOUR,
            accent = color("#43D9BE"),
            material = CardMaterial.GLASS,
            regionalMark = "新加坡",
        )

        /** 冻结的 8 个 issuer 集合（AndroidCardIdentityContractTest 必须逐项存在）。 */
        val FROZEN_ISSUERS: List<CardIdentityProfile> = listOf(CMB, ICBC, BOC, HSBC, BOCHK, MONZO, REVOLUT, CHASE)

        /** issuer 字符串 → profile（normalized 精确匹配 + 保守子串；不做模糊/LLM 推断，见 Node Resolver 顺序）。 */
        fun forIssuer(issuer: String): CardIdentityProfile {
            val key = issuer.trim().lowercase()
            if (key.contains("招商") || key.contains("cmb")) return CMB
            if (key.contains("工商") || key.contains("icbc")) return ICBC
            if (key.contains("中银") && key.contains("香港")) return BOCHK
            if (key.contains("中国银行") || key.contains("boc")) return BOC
            if (key.contains("汇丰") || key.contains("hsbc")) return HSBC
            if (key.contains("monzo")) return MONZO
            if (key.contains("revolut")) return REVOLUT
            if (key.contains("chase")) return CHASE
            if (key.contains("capital one")) return CAPITAL_ONE
            if (key.contains("dbs")) return DBS
            return NEUTRAL
        }
    }
}
