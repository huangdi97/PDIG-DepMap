package com.pdig.uivnext.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.production.VNextProductionObject
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius

/**
 * Production payment-asset identity face.
 *
 * Unlike the synthetic/reference Card renderer this surface accepts only fields
 * that production Reality can currently prove: name / issuer / last4.
 *
 * It deliberately does NOT invent:
 * region / currency / network / debit-credit / physical-virtual / expiry.
 */
@Composable
internal fun ProductionPaymentAssetFace(
    asset: VNextProductionObject,
    presentation: PresentationProfile?,
    privacyMask: Boolean,
    modifier: Modifier = Modifier,
) {
    val issuer = asset.issuer?.takeIf { it.isNotBlank() } ?: "发行方未记录"
    val identity = CardIdentityProfile.forIssuer(asset.issuer.orEmpty())
    val theme = cardThemeInfo(presentation?.themeId ?: "minimal")
    val material = when (presentation?.material) {
        "glass" -> CardMaterial.GLASS
        "metal" -> CardMaterial.METAL
        "brushed" -> CardMaterial.BRUSHED
        "matte" -> CardMaterial.MATTE
        "satin" -> CardMaterial.SATIN
        "translucent" -> CardMaterial.TRANSLUCENT
        else -> null
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pdig.production-vnext.card.face"),
        color = Color.Transparent,
        shape = RoundedCornerShape(VRadius.Xl),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
        shadowElevation = 2.dp,
    ) {
        Box(
            Modifier
                .aspectRatio(1.586f)
                .background(cardFaceBaseBrush(identity, theme))
                .padding(18.dp),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCardArtwork(identity, theme, material)
            }
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            asset.name,
                            color = PdigV2Colors.AssetTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text(
                            issuer,
                            color = PdigV2Colors.AssetTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                        )
                    }
                    Text(
                        "支付工具",
                        color = PdigV2Colors.AssetTextMuted,
                        fontSize = 10.sp,
                    )
                }

                Spacer(Modifier.weight(1f))

                Text(
                    when {
                        privacyMask -> "••••"
                        asset.last4.isNullOrBlank() -> "尾号未记录"
                        else -> "••••  ${asset.last4}"
                    },
                    color = PdigV2Colors.AssetTextPrimary,
                    fontSize = 19.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "只显示已确认身份字段",
                    color = PdigV2Colors.AssetTextMuted,
                    fontSize = 9.sp,
                )
            }
        }
    }
}

internal data class ProductionCardThemeChoice(
    val id: String,
    val label: String,
)

internal val PRODUCTION_CARD_THEME_CHOICES = listOf(
    ProductionCardThemeChoice("minimal", "原生"),
    ProductionCardThemeChoice("glass", "玻璃"),
    ProductionCardThemeChoice("metal", "金属"),
    ProductionCardThemeChoice("abstract", "抽象"),
    ProductionCardThemeChoice("deep-space", "深空"),
)

/**
 * Tiny in-detail appearance control — intentionally not a second product.
 */
@Composable
internal fun ProductionCardAppearanceStrip(
    selectedTheme: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().testTag("pdig.production-vnext.card.appearance"),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            "卡面外观",
            color = PdigV2Colors.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PRODUCTION_CARD_THEME_CHOICES.forEach { choice ->
                val selected = choice.id == selectedTheme
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(choice.id) },
                    color = if (selected) PdigV2Colors.PrimarySoft else PdigV2Colors.Surface,
                    shape = RoundedCornerShape(VRadius.Md),
                    border = BorderStroke(
                        1.dp,
                        if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.BorderSubtle,
                    ),
                ) {
                    Box(
                        Modifier.padding(horizontal = 6.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            choice.label,
                            color = if (selected) PdigV2Colors.PrimaryText else PdigV2Colors.TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
        Text(
            "仅改变本机呈现；不会修改卡片身份、关系或影响分析。",
            color = PdigV2Colors.TextMuted,
            fontSize = 9.sp,
        )
    }
}
