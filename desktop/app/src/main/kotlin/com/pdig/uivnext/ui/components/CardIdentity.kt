package com.pdig.uivnext.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.pdig.uivnext.demo.cardVisualProfileFor
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.model.UiVNextCard
import com.pdig.uivnext.theme.PdigV2Colors

/**
 * PHASE 1F —— Card Identity System（§7–§9）。
 *
 * 每张 demo 卡的视觉身份 = palette（基础色对）+ accent + motif（艺术几何）。
 * 满足最小契约：至少 3 个身份要素（palette identity / material response /
 * artwork-texture / layout identity / regional motif / issuer motif /
 * strong accent geometry），且永不是「深色矩形 + issuer + PAN」。
 *
 * 身份解析规则：
 *  - 卡处于默认视觉（themeId/material 与 issuer 默认 profile 一致）→ 使用
 *    issuer 专属身份（§9 冻结方向，全部合成，不复制官方卡面）；
 *  - 用户在 Studio 中定制（theme/material 改变）→ 使用主题身份（保证编辑
 *    在预览中可见变化）。
 *  - 缩略图（card=null）→ 主题身份。
 * 全部颜色由 token 色 mix 合成（不改 DESIGN_TOKENS / spec）。
 * 绘制层见 CardIdentityDrawing.kt（本文件只含模型与解析，纯函数可测）。
 */

/** 艺术几何母题（一个母题可含多层，见 CardIdentityDrawing.kt）。 */
internal enum class CardMotif {
    NONE, CITY_NIGHT, CONTOUR, BRUSHED, SWEEP,
    STARFIELD, COPPER_RING, RED_LINE, CORAL_BAND, CHROMATIC,
}

/** 确定性身份：top/bottom 基础色对、glow、accent、motif。 */
internal data class CardIdentity(
    val top: Color,
    val bottom: Color,
    val glow: Color,
    val accent: Color,
    val motif: CardMotif,
)

private fun mix(a: Color, b: Color, t: Float): Color = lerp(a, b, t)

// —— 基础身份色板（全部 token mix；亮面亮度经过设计：无 near-black 空占位）——
private val BURGUNDY = CardIdentity(
    top = mix(PdigV2Colors.Critical, PdigV2Colors.CanvasDeep, 0.55f),
    bottom = mix(PdigV2Colors.Critical, PdigV2Colors.CanvasDeep, 0.80f),
    glow = mix(PdigV2Colors.NightCityLight, PdigV2Colors.Critical, 0.55f),
    accent = mix(PdigV2Colors.NightCityLight, PdigV2Colors.Warning, 0.45f),
    motif = CardMotif.COPPER_RING,
)
private val GRAPHITE = CardIdentity(
    top = mix(PdigV2Colors.SurfaceRaised, PdigV2Colors.TextSecondary, 0.14f),
    bottom = PdigV2Colors.CanvasDeep,
    glow = mix(PdigV2Colors.TextSecondary, PdigV2Colors.CanvasDeep, 0.6f),
    accent = PdigV2Colors.Critical,
    motif = CardMotif.RED_LINE,
)
private val COOL_METAL = CardIdentity(
    top = mix(PdigV2Colors.Unknown, PdigV2Colors.Surface, 0.45f),
    bottom = PdigV2Colors.CanvasDeep,
    glow = mix(PdigV2Colors.TextSecondary, PdigV2Colors.CanvasDeep, 0.55f),
    accent = PdigV2Colors.Unknown,
    motif = CardMotif.BRUSHED,
)
private val CHARCOAL_CORAL = CardIdentity(
    top = mix(PdigV2Colors.SurfaceRaised, PdigV2Colors.TextSecondary, 0.14f),
    bottom = PdigV2Colors.CanvasDeep,
    glow = mix(PdigV2Colors.Warning, PdigV2Colors.Critical, 0.55f),
    accent = mix(PdigV2Colors.Warning, PdigV2Colors.Critical, 0.55f),
    motif = CardMotif.CORAL_BAND,
)
private val NAVY_METAL = CardIdentity(
    top = mix(PdigV2Colors.Primary, PdigV2Colors.CanvasDeep, 0.55f),
    bottom = PdigV2Colors.CanvasDeep,
    glow = mix(PdigV2Colors.PrimaryBright, PdigV2Colors.CanvasDeep, 0.55f),
    accent = PdigV2Colors.PrimaryBright,
    motif = CardMotif.BRUSHED,
)
private val MIDNIGHT_SWEEP = CardIdentity(
    top = mix(PdigV2Colors.PrimarySoft, PdigV2Colors.CanvasDeep, 0.28f),
    bottom = PdigV2Colors.CanvasDeep,
    glow = PdigV2Colors.PrimarySoft,
    accent = PdigV2Colors.Critical,
    motif = CardMotif.SWEEP,
)
private val HK_CITY = CardIdentity(
    top = mix(PdigV2Colors.LandHighlight, PdigV2Colors.CanvasDeep, 0.5f),
    bottom = PdigV2Colors.CanvasDeep,
    glow = PdigV2Colors.NightCityLight,
    accent = mix(PdigV2Colors.NightCityLight, PdigV2Colors.Warning, 0.5f),
    motif = CardMotif.CITY_NIGHT,
)
private val HK_REGION = CardIdentity(
    top = mix(PdigV2Colors.OceanBase, PdigV2Colors.CanvasDeep, 0.35f),
    bottom = PdigV2Colors.CanvasDeep,
    glow = PdigV2Colors.Primary,
    accent = PdigV2Colors.Primary,
    motif = CardMotif.CONTOUR,
)
private val SG_NIGHT = CardIdentity(
    top = mix(PdigV2Colors.OceanBase, PdigV2Colors.CanvasDeep, 0.30f),
    bottom = PdigV2Colors.CanvasDeep,
    glow = PdigV2Colors.NightCityLight,
    accent = mix(PdigV2Colors.NightCityLight, PdigV2Colors.Warning, 0.45f),
    motif = CardMotif.CITY_NIGHT,
)
private val CHROMATIC = CardIdentity(
    top = mix(PdigV2Colors.PrimarySoft, PdigV2Colors.CanvasDeep, 0.25f),
    bottom = PdigV2Colors.CanvasDeep,
    glow = PdigV2Colors.AtmosphereRim,
    accent = PdigV2Colors.PrimaryBright,
    motif = CardMotif.CHROMATIC,
)
private val STARFIELD = CardIdentity(
    top = mix(PdigV2Colors.PrimarySoft, PdigV2Colors.CanvasDeep, 0.30f),
    bottom = PdigV2Colors.CanvasDeep,
    glow = PdigV2Colors.PrimarySoft,
    accent = PdigV2Colors.PrimaryBright,
    motif = CardMotif.STARFIELD,
)

/** §9 冻结的 issuer 身份（synthetic only）。 */
private fun issuerIdentity(issuer: String): CardIdentity? = when (issuer) {
    "招商银行" -> BURGUNDY.copy(accent = mix(PdigV2Colors.NightCityLight, PdigV2Colors.Warning, 0.45f))
    "中国工商银行" -> GRAPHITE
    "中国银行" -> COOL_METAL
    "HSBC 汇丰" -> HK_CITY
    "中银香港" -> HK_REGION
    "Monzo" -> CHARCOAL_CORAL
    "Revolut" -> CHROMATIC
    "Chase" -> NAVY_METAL
    "Capital One" -> MIDNIGHT_SWEEP
    "DBS" -> SG_NIGHT
    else -> null
}

/** 主题身份（缩略图 + 用户定制后；accent 跟随 profile）。 */
private fun themeIdentity(p: PresentationProfile): CardIdentity {
    val accent = accentColorOf(p.accentColor)
    return when (p.themeId) {
        "matte" -> BURGUNDY.copy(accent = BURGUNDY.accent)
        "deep-space" -> STARFIELD.copy(accent = accent)
        "region" -> HK_REGION.copy(accent = accent)
        "city" -> HK_CITY.copy(accent = accent)
        "glass" -> CHROMATIC.copy(accent = accent)
        "metal" -> COOL_METAL.copy(accent = accent)
        "abstract" -> MIDNIGHT_SWEEP.copy(accent = accent)
        else -> GRAPHITE.copy(accent = accent)
    }
}

/** 默认卡 profile（issuer 默认视觉；§9 身份驱动）。 */
internal fun defaultCardProfile(card: UiVNextCard): PresentationProfile {
    val vp = cardVisualProfileFor(card.id)
    return PresentationProfile.defaultFor("card", card.id, vp.theme).copy(
        material = vp.material,
        accentColor = vp.accent,
        layout = vp.layout,
        backgroundKind = "preset",
        backgroundValue = vp.theme,
    )
}

/**
 * 身份解析（纯函数，供测试与渲染共用）：
 *  - card != null 且 profile 等于 issuer 默认视觉 → issuer 身份（§9）；
 *  - 否则（定制 / 缩略图）→ 主题身份。
 */
internal fun resolveCardIdentity(profile: PresentationProfile, card: UiVNextCard?): CardIdentity {
    if (card != null) {
        val d = defaultCardProfile(card)
        if (profile.themeId == d.themeId && profile.material == d.material) {
            return issuerIdentity(card.issuer) ?: themeIdentity(profile)
        }
    }
    return themeIdentity(profile)
}

/** 身份要素计数（PHASE 1F 契约：demo 卡 ≥3）。 */
internal fun identityElementCount(identity: CardIdentity, material: String): Int {
    var n = 0
    if (identity.top != identity.bottom) n++          // palette identity
    if (identity.motif != CardMotif.NONE) n++         // artwork-texture / accent geometry
    if (identity.accent != PdigV2Colors.Primary) n++  // accent identity
    if (material != "minimal") n++                    // material response
    if (identity.glow != Color.Transparent) n++       // glow / atmosphere
    return n
}