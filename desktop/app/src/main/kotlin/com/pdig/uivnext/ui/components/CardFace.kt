package com.pdig.uivnext.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.model.UiVNextCard
import com.pdig.uivnext.theme.ACCENT_SWATCHES
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VType

/**
 * AssetCard —— 卡面 = 真实支付卡资产身份（Review §7/§8）。
 *
 * 视觉语法分层：Background Material → Issuer Identity → Card Identity →
 * Financial Metadata → Status Overlay；layout preset 可改变各层位置：
 *   standard（issuer 顶部 / number 中部 / metadata+status 底部）、
 *   minimal-content（仅 nickname+number+network）、
 *   emblem（issuer 大标题顶 / number 左下 / network 右下）。
 *
 * 8 个视觉预设真实不同（非全蓝渐变）：
 *   minimal / matte / glass / metal / region / city / abstract / deep-space；
 * 全部程序化（token 色），零远程图片；保持 1.586 ratio。
 */

/** 卡片主题中文名（用户语言；高级内部值不进普通 UI）。 */
internal fun cardThemeLabel(theme: String): String = when (theme) {
    "minimal" -> "极简"
    "matte" -> "哑光"
    "glass" -> "玻璃"
    "metal" -> "金属"
    "region" -> "地区"
    "city" -> "城市"
    "abstract" -> "抽象"
    "deep-space" -> "深空"
    else -> theme
}

internal fun cardLayoutLabel(layout: String): String = when (layout) {
    "emblem" -> "徽章"
    "minimal-content" -> "精简内容"
    else -> "标准"
}

@Composable
fun AssetCard(
    card: UiVNextCard,
    privacyMask: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    profile: PresentationProfile? = null,
) {
    val p = profile ?: PresentationProfile.defaultFor("card", card.id, card.preset)
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(VRadius.Xl))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(VRadius.Xl),
        color = Color.Transparent,
    ) {
        Box(
            Modifier
                .aspectRatio(1.586f)
                .drawBehind { drawCardFaceBackdrop(p) }
                .border(1.dp, cardFaceBorder(p), RoundedCornerShape(VRadius.Xl))
                .padding(VSpacing.Xl),
        ) {
            when (p.layout) {
                "emblem" -> EmblemContent(card, p, privacyMask)
                "minimal-content" -> MinimalContent(card, p, privacyMask)
                else -> StandardContent(card, p, privacyMask)
            }
        }
    }
}

/** 标准布局：issuer 顶部 / 卡号中部 / metadata+status 底部。 */
@Composable
private fun StandardContent(card: UiVNextCard, p: PresentationProfile, privacyMask: Boolean) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column {
                Text(card.issuer, style = VType.Label, color = PdigV2Colors.TextSecondary, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text(card.nickname, style = VType.Body, fontWeight = FontWeight.SemiBold, color = PdigV2Colors.TextPrimary, maxLines = 1)
            }
            NetworkChip(card.network)
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardChip(p)
            Spacer(Modifier.width(VSpacing.Md))
            Text(
                maskedNumber(card, privacyMask, p.maskSensitive),
                style = VType.Mono,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = PdigV2Colors.TextPrimary,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(VSpacing.Lg))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
                    FormChip(card.form)
                    CategoryChip(card.type)
                }
                if (p.backgroundKind == "preset" || p.layout == "standard") {
                    Spacer(Modifier.height(VSpacing.Sm))
                    Text(
                        "${card.region} · ${card.currency} · 到期 ${card.expiry}",
                        style = VType.Meta,
                        color = PdigV2Colors.TextMuted,
                        maxLines = 1,
                    )
                }
            }
            StatusBadge(card.status)
        }
    }
}

/** 精简内容布局：仅 nickname + 大号卡号 + network。 */
@Composable
private fun MinimalContent(card: UiVNextCard, p: PresentationProfile, privacyMask: Boolean) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(card.nickname, style = VType.Label, color = PdigV2Colors.TextPrimary, maxLines = 1, modifier = Modifier.weight(1f))
            NetworkChip(card.network)
        }
        Spacer(Modifier.weight(1f))
        Text(
            maskedNumber(card, privacyMask, p.maskSensitive),
            style = VType.Mono,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = PdigV2Colors.TextPrimary,
            maxLines = 1,
        )
    }
}

/** 徽章布局：issuer 大标题顶部 / number 左下 / network 右下。 */
@Composable
private fun EmblemContent(card: UiVNextCard, p: PresentationProfile, privacyMask: Boolean) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(card.issuer, style = VType.SectionTitle, color = PdigV2Colors.TextPrimary, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text(card.nickname, style = VType.Meta, color = PdigV2Colors.TextSecondary, maxLines = 1)
            }
            StatusBadge(card.status)
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CardChip(p)
                    Spacer(Modifier.width(VSpacing.Md))
                    Text(
                        maskedNumber(card, privacyMask, p.maskSensitive),
                        style = VType.Mono,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = PdigV2Colors.TextPrimary,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.height(VSpacing.Sm))
                Text(
                    "${card.region} · ${card.currency} · ${if (card.form == "virtual") "虚拟" else "实体"} · 到期 ${card.expiry}",
                    style = VType.Meta,
                    color = PdigV2Colors.TextMuted,
                    maxLines = 1,
                )
            }
            NetworkChip(card.network)
        }
    }
}

private fun maskedNumber(card: UiVNextCard, privacyMask: Boolean, maskSensitive: Boolean): String =
    if (privacyMask && maskSensitive) "•••• •••• •••• ••••" else "•••• •••• •••• ${card.last4}"

/** 网络徽标（银联 / Visa / Mastercard…）。 */
@Composable
private fun NetworkChip(network: String) {
    Surface(color = PdigV2Colors.PrimarySoft, shape = RoundedCornerShape(VRadius.Sm)) {
        Text(
            network,
            Modifier.padding(horizontal = VSpacing.Sm, vertical = 3.dp),
            style = VType.Label,
            color = PdigV2Colors.PrimaryBright,
        )
    }
}

/** 物理/虚拟：形态差异（克制：实体 = 描边、虚拟 = 填充亮色）。 */
@Composable
private fun FormChip(form: String) {
    val virtual = form == "virtual"
    Surface(
        color = if (virtual) PdigV2Colors.PrimarySoft else Color.Transparent,
        shape = RoundedCornerShape(VRadius.Sm),
        border = BorderStroke(
            1.dp,
            if (virtual) PdigV2Colors.PrimaryBright else PdigV2Colors.BorderSubtle,
        ),
    ) {
        Text(
            if (virtual) "虚拟卡" else "实体卡",
            Modifier.padding(horizontal = VSpacing.Sm, vertical = 3.dp),
            style = VType.Label,
            color = if (virtual) PdigV2Colors.PrimaryBright else PdigV2Colors.TextSecondary,
        )
    }
}

@Composable
private fun CategoryChip(type: String) {
    Surface(color = PdigV2Colors.SurfaceRaised, shape = RoundedCornerShape(VRadius.Sm)) {
        Text(
            if (type == "credit") "信用卡" else "储蓄卡",
            Modifier.padding(horizontal = VSpacing.Sm, vertical = 3.dp),
            style = VType.Label,
            color = PdigV2Colors.TextSecondary,
        )
    }
}

/** 程序化支付芯片（点缀真实感；accent 色 token）。 */
@Composable
private fun CardChip(p: PresentationProfile) {
    val accent = accentColorOf(p.accentColor)
    Surface(
        Modifier.size(width = 34.dp, height = 24.dp),
        color = accent.copy(alpha = 0.85f),
        shape = RoundedCornerShape(5.dp),
        border = BorderStroke(1.dp, PdigV2Colors.TextPrimary.copy(alpha = 0.35f)),
    ) {}
}

internal fun accentColorOf(accent: String): Color =
    ACCENT_SWATCHES.firstOrNull { it.first == accent }?.second ?: PdigV2Colors.Primary

private fun cardFaceBorder(p: PresentationProfile): Color = when (p.material) {
    "glass" -> PdigV2Colors.BorderStrong.copy(alpha = 0.6f)
    "metal" -> PdigV2Colors.TextSecondary.copy(alpha = 0.35f)
    "minimal" -> PdigV2Colors.BorderSubtle
    else -> PdigV2Colors.BorderSubtle
}

/**
 * 卡面背景：theme 底色 + material 质感 + theme 图案（全部 token 色）。
 * internal：供 StudioFrame 的确定性视觉缩略图复用同一渲染器。
 */
internal fun DrawScope.drawCardFaceBackdrop(p: PresentationProfile) {
    val w = size.width
    val h = size.height
    // 1. Theme base
    val base: Brush = when (p.themeId) {
        "minimal" -> Brush.verticalGradient(listOf(PdigV2Colors.SurfaceRaised, PdigV2Colors.CanvasDeep))
        "deep-space" -> Brush.radialGradient(
            listOf(PdigV2Colors.Surface, PdigV2Colors.CanvasDeep),
            center = Offset(w * 0.42f, h * 0.34f),
            radius = w,
        )
        "region" -> Brush.linearGradient(listOf(PdigV2Colors.LandHighlight, PdigV2Colors.OceanBase, PdigV2Colors.OceanDeep))
        "city" -> Brush.linearGradient(listOf(PdigV2Colors.SurfaceRaised, PdigV2Colors.CanvasDeep))
        "glass" -> Brush.linearGradient(listOf(PdigV2Colors.SurfaceGlass, PdigV2Colors.Surface.copy(alpha = 0.65f)))
        "metal" -> Brush.linearGradient(listOf(PdigV2Colors.SurfaceRaised, PdigV2Colors.CanvasDeep, PdigV2Colors.SurfaceRaised))
        "abstract" -> Brush.linearGradient(
            listOf(PdigV2Colors.Primary, PdigV2Colors.PrimarySoft, PdigV2Colors.Surface),
            start = Offset(0f, 0f),
            end = Offset(w, h),
        )
        else -> Brush.linearGradient(listOf(PdigV2Colors.Surface, PdigV2Colors.CanvasDeep))
    }
    drawRect(base)
    // 2. Theme motifs（视觉身份，不全是蓝渐变）
    when (p.themeId) {
        "deep-space" -> {
            drawCircle(
                brush = Brush.radialGradient(listOf(PdigV2Colors.PrimarySoft.copy(alpha = 0.7f), Color.Transparent)),
                radius = w * 0.55f,
                center = Offset(w * 0.30f, h * 0.20f),
            )
            for (i in 0 until 16) {
                val x = ((i * 37.5f) % 100f) / 100f * w
                val y = ((i * 23.7f) % 100f) / 100f * h
                drawCircle(PdigV2Colors.Star.copy(alpha = 0.30f + (i % 3) * 0.12f), radius = 1.0f + (i % 2), center = Offset(x, y))
            }
        }
        "region" -> {
            drawCircle(PdigV2Colors.LandBase.copy(alpha = 0.55f), radius = w * 0.34f, center = Offset(w * 0.78f, h * 0.30f))
            drawCircle(PdigV2Colors.LandTextureHi.copy(alpha = 0.25f), radius = w * 0.18f, center = Offset(w * 0.62f, h * 0.38f))
            drawCircle(PdigV2Colors.OceanBase.copy(alpha = 0.55f), radius = w * 0.26f, center = Offset(w * 0.20f, h * 0.85f))
            drawCircle(PdigV2Colors.RegionNodeHi.copy(alpha = 0.85f), radius = 3f, center = Offset(w * 0.78f, h * 0.30f))
        }
        "city" -> {
            val skyline = Path()
            skyline.moveTo(0f, h * 0.72f)
            skyline.lineTo(w * 0.10f, h * 0.72f)
            skyline.lineTo(w * 0.10f, h * 0.55f)
            skyline.lineTo(w * 0.18f, h * 0.55f)
            skyline.lineTo(w * 0.18f, h * 0.62f)
            skyline.lineTo(w * 0.27f, h * 0.62f)
            skyline.lineTo(w * 0.27f, h * 0.45f)
            skyline.lineTo(w * 0.36f, h * 0.45f)
            skyline.lineTo(w * 0.36f, h * 0.60f)
            skyline.lineTo(w * 0.48f, h * 0.60f)
            skyline.lineTo(w * 0.48f, h * 0.50f)
            skyline.lineTo(w * 0.58f, h * 0.50f)
            skyline.lineTo(w * 0.58f, h * 0.70f)
            skyline.lineTo(w, h * 0.70f)
            skyline.lineTo(w, h)
            skyline.lineTo(0f, h)
            skyline.close()
            drawPath(skyline, color = PdigV2Colors.CanvasDeep.copy(alpha = 0.75f))
            for (row in 0 until 4) {
                for (col in 0 until 10) {
                    val x = 12f + col * ((w - 24f) / 9f)
                    val y = h * (0.30f + 0.13f * row)
                    drawCircle(PdigV2Colors.NightCityLight.copy(alpha = 0.22f + (col % 3) * 0.14f), radius = 1.1f, center = Offset(x, y))
                }
            }
        }
        "abstract" -> {
            val diag = Path().apply {
                moveTo(w * 0.30f, 0f)
                lineTo(w, h * 0.62f)
                lineTo(w, h)
                lineTo(w * 0.12f, h)
                close()
            }
            drawPath(diag, color = PdigV2Colors.PrimaryBright.copy(alpha = 0.18f))
            drawCircle(PdigV2Colors.PrimaryBright.copy(alpha = 0.30f), radius = w * 0.22f, center = Offset(w * 0.78f, h * 0.28f))
            drawCircle(PdigV2Colors.NightCityLight.copy(alpha = 0.18f), radius = w * 0.10f, center = Offset(w * 0.28f, h * 0.78f))
            for (i in 1 until 6) {
                val x = w * i / 6f
                drawLine(
                    color = PdigV2Colors.TextMuted.copy(alpha = 0.08f),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1f,
                )
            }
        }
        "glass" -> {
            drawRect(
                Brush.linearGradient(
                    listOf(PdigV2Colors.AtmosphereRim.copy(alpha = 0.30f), Color.Transparent),
                    start = Offset(0f, h),
                    end = Offset(w, 0f),
                ),
            )
        }
        else -> Unit
    }
    // 3. Material 质感
    when (p.material) {
        "matte" -> {
            for (i in 0 until 26) {
                val x = ((i * 41.7f) % 100f) / 100f * w
                val y = ((i * 29.3f) % 100f) / 100f * h
                drawCircle(PdigV2Colors.TextMuted.copy(alpha = 0.06f + (i % 3) * 0.03f), radius = 0.6f + (i % 2) * 0.4f, center = Offset(x, y))
            }
        }
        "metal" -> {
            drawRect(
                Brush.linearGradient(
                    listOf(
                        PdigV2Colors.TextPrimary.copy(alpha = 0.10f),
                        Color.Transparent,
                        PdigV2Colors.TextPrimary.copy(alpha = 0.05f),
                    ),
                    start = Offset(0f, h * 0.28f),
                    end = Offset(0f, h * 0.72f),
                ),
            )
            drawRect(Brush.horizontalGradient(listOf(Color.Transparent, PdigV2Colors.TextMuted.copy(alpha = 0.20f), Color.Transparent)), topLeft = Offset(0f, h * 0.44f), size = Size(w, 1f))
            for (i in 1 until 7) {
                drawLine(
                    color = PdigV2Colors.TextMuted.copy(alpha = 0.05f),
                    start = Offset(0f, h * i / 7f),
                    end = Offset(w, h * i / 7f),
                    strokeWidth = 1f,
                )
            }
        }
        "glass" -> drawRect(
            Brush.linearGradient(
                listOf(PdigV2Colors.TextPrimary.copy(alpha = 0.07f), Color.Transparent),
                start = Offset(0f, 0f),
                end = Offset(w, h),
            ),
        )
        else -> Unit
    }
    // 4. 左缘 accent 洗色（克制的品牌强调）
    val accent = accentColorOf(p.accentColor)
    drawRect(
        Brush.horizontalGradient(listOf(accent.copy(alpha = 0.16f), Color.Transparent)),
        topLeft = Offset(0f, 0f),
        size = Size(w * 0.55f, h),
    )
    // 5. 顶部 rim 高光（空间层次；仅 glass/metal）
    if (p.material == "glass" || p.material == "metal") {
        drawRect(
            Brush.verticalGradient(listOf(PdigV2Colors.TextPrimary.copy(alpha = 0.14f), Color.Transparent)),
            topLeft = Offset(0f, 0f),
            size = Size(w, h * 0.16f),
        )
    }
}

/**
 * 确定性视觉缩略图（Studio 主题选择器用；固定尺寸 76dp、固定渲染器、
 * 共享 PresentationProfile 渲染；避免在滚动容器内使用 aspectRatio）。
 */
@Composable
fun CardFaceThumbnail(preset: String, modifier: Modifier = Modifier) {
    val profile = PresentationProfile.defaultFor("card", "thumb", preset).copy(
        material = if (preset == "matte") "matte" else if (preset == "glass") "glass" else if (preset == "metal") "metal" else "minimal",
    )
    Box(
        modifier
            .fillMaxWidth(0.92f)
            .height(76.dp)
            .drawBehind { drawCardFaceBackdrop(profile) }
            .border(1.dp, PdigV2Colors.BorderSubtle, RoundedCornerShape(VRadius.Sm)),
    ) {
        Column(Modifier.fillMaxSize().padding(6.dp)) {
            Text(cardThemeLabel(preset), style = VType.Meta, color = PdigV2Colors.TextSecondary, maxLines = 1)
            Spacer(Modifier.weight(1f))
            Text("•••• ••••", style = VType.Mono, fontSize = 9.sp, color = PdigV2Colors.TextPrimary)
        }
    }
}
