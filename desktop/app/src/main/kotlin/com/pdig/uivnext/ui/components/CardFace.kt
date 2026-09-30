package com.pdig.uivnext.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
 * AssetCard —— 卡面 = 真实支付卡资产身份（G6）。
 *
 *  - 1.586 ratio；issuer/nickname/masked number/network/category 有明确位置；
 *  - region/currency/type/status 在 metadata 层；
 *  - physical/virtual 有明显但克制的区别（chip 样式差异）；
 *  - 卡面可自定义（PresentationProfile<Card>）；不同 preset 视觉差异明显
 *    （minimal/deep-space/region/city/glass/metal/abstract 各有底色/材质/质感）；
 *  - 背景全部程序化（token 色），零远程图片。
 */

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
            CardFaceContent(card, p, privacyMask)
        }
    }
}

@Composable
private fun CardFaceContent(card: UiVNextCard, p: PresentationProfile, privacyMask: Boolean) {
    Column(Modifier.fillMaxWidth()) {
        // 顶部：issuer + network（身份首行）
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column {
                Text(card.issuer, style = VType.Label, color = PdigV2Colors.TextSecondary, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text(card.nickname, style = VType.Body, fontWeight = FontWeight.SemiBold, color = PdigV2Colors.TextPrimary, maxLines = 1)
            }
            NetworkChip(card.network)
        }
        Spacer(Modifier.weight(1f))
        // 中部：卡号 + chip
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
        // 底部：category / form + metadata + status
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
                    FormChip(card.form)
                    CategoryChip(card.type)
                }
                if (p.layout != "minimal-content") {
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
        border = androidx.compose.foundation.BorderStroke(
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
        border = androidx.compose.foundation.BorderStroke(1.dp, PdigV2Colors.TextPrimary.copy(alpha = 0.35f)),
    ) {}
}

internal fun accentColorOf(accent: String): Color =
    ACCENT_SWATCHES.firstOrNull { it.first == accent }?.second ?: PdigV2Colors.Primary

private fun cardFaceBorder(p: PresentationProfile): Color = when (p.material) {
    "glass" -> PdigV2Colors.BorderStrong.copy(alpha = 0.6f)
    "metal" -> PdigV2Colors.TextSecondary.copy(alpha = 0.35f)
    else -> PdigV2Colors.BorderSubtle
}

/** 卡面背景：theme 底色 + material 质感 + theme 图案（全部 token 色）。 */
private fun DrawScope.drawCardFaceBackdrop(p: PresentationProfile) {
    val w = size.width
    val h = size.height
    val base: Brush = when (p.themeId) {
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
    // material 质感
    when (p.material) {
        "glass" -> drawRect(
            Brush.linearGradient(
                listOf(PdigV2Colors.TextPrimary.copy(alpha = 0.07f), Color.Transparent),
                start = Offset(0f, 0f),
                end = Offset(w, h),
            ),
        )
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
            drawRect(Brush.horizontalGradient(listOf(Color.Transparent, PdigV2Colors.TextMuted.copy(alpha = 0.22f), Color.Transparent)), topLeft = Offset(0f, h * 0.44f), size = Size(w, 1f))
        }
        "matte" -> drawRect(PdigV2Colors.Surface.copy(alpha = 0.40f))
        else -> Unit
    }
    // theme 图案
    when (p.themeId) {
        "deep-space" -> {
            for (i in 0 until 14) {
                val x = ((i * 37.5f) % 100f) / 100f * w
                val y = ((i * 23.7f) % 100f) / 100f * h
                drawCircle(PdigV2Colors.Star.copy(alpha = 0.35f + (i % 3) * 0.12f), radius = 1.0f + (i % 2), center = Offset(x, y))
            }
        }
        "city" -> {
            for (row in 0 until 4) {
                for (col in 0 until 10) {
                    val x = 12f + col * ((w - 24f) / 9f)
                    val y = h * (0.38f + 0.14f * row)
                    drawCircle(PdigV2Colors.NightCityLight.copy(alpha = 0.30f + (col % 3) * 0.18f), radius = 1.2f, center = Offset(x, y))
                }
            }
        }
        "region" -> {
            drawCircle(PdigV2Colors.LandBase.copy(alpha = 0.5f), radius = w * 0.34f, center = Offset(w * 0.78f, h * 0.30f))
            drawCircle(PdigV2Colors.OceanBase.copy(alpha = 0.55f), radius = w * 0.26f, center = Offset(w * 0.20f, h * 0.85f))
        }
        "abstract" -> {
            drawCircle(PdigV2Colors.PrimaryBright.copy(alpha = 0.22f), radius = w * 0.30f, center = Offset(w * 0.75f, h * 0.25f))
            drawCircle(PdigV2Colors.NightCityLight.copy(alpha = 0.16f), radius = w * 0.16f, center = Offset(w * 0.30f, h * 0.80f))
        }
        "glass" -> drawRect(
            Brush.linearGradient(
                listOf(PdigV2Colors.AtmosphereRim.copy(alpha = 0.30f), Color.Transparent),
                start = Offset(0f, h),
                end = Offset(w, 0f),
            ),
        )
        else -> Unit
    }
    // 左缘 accent 洗色（克制的品牌强调）
    val accent = accentColorOf(p.accentColor)
    drawRect(
        Brush.horizontalGradient(listOf(accent.copy(alpha = 0.16f), Color.Transparent)),
        topLeft = Offset(0f, 0f),
        size = Size(w * 0.55f, h),
    )
}
