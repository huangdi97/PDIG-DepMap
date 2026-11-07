package com.pdig.uivnext.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
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
 * CardFace 内容排版（PHASE 1D 拆分，≤300 行）：
 * standard / minimal-content / emblem 三种布局 + 小徽标（network/form/category/chip）+ accent 解析。
 * 渲染逻辑（背景材质/艺术作品）见 CardFace.kt + CardVisualRenderer.kt。
 */

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
            // 实体卡：EMV chip visual；虚拟卡：无 chip + 抽象数字标记（§11）
            if (card.form == "physical") {
                CardChip(p)
                Spacer(Modifier.width(VSpacing.Md))
            } else {
                VirtualMark()
                Spacer(Modifier.width(VSpacing.Md))
            }
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
                    if (card.form == "physical") {
                        CardChip(p)
                        Spacer(Modifier.width(VSpacing.Md))
                    } else {
                        VirtualMark()
                        Spacer(Modifier.width(VSpacing.Md))
                    }
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

/** 卡内容分发：按 layout preset 选择内容排版。 */
@Composable
internal fun CardContent(card: UiVNextCard, p: PresentationProfile, privacyMask: Boolean) {
    when (p.layout) {
        "emblem" -> EmblemContent(card, p, privacyMask)
        "minimal-content" -> MinimalContent(card, p, privacyMask)
        else -> StandardContent(card, p, privacyMask)
    }
}

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

/** 虚拟卡数字标记（无 EMV chip；抽象数字/填充点；§11）。 */
@Composable
private fun VirtualMark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            Box(
                Modifier
                    .size(5.dp + 2.dp * (i % 2))
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(PdigV2Colors.PrimaryBright.copy(alpha = 0.85f)),
            )
            if (i < 2) Spacer(Modifier.width(3.dp))
        }
    }
}

/** 程序化支付芯片（实体卡 EMV；accent 色 token）。 */
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

internal fun cardFaceBorder(p: PresentationProfile): Color = when (p.material) {
    "glass" -> PdigV2Colors.BorderStrong.copy(alpha = 0.6f)
    "metal" -> PdigV2Colors.TextSecondary.copy(alpha = 0.35f)
    "minimal" -> PdigV2Colors.BorderSubtle
    else -> PdigV2Colors.BorderSubtle
}
