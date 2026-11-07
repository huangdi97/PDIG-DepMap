package com.pdig.uivnext.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.UiVNextCard
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.model.regionLabelZh
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing

/**
 * Card Identity Renderer —— 唯一卡面 renderer（Grid / Card Detail / Card Studio 全部复用）。
 *
 * 层次（每层都有视觉身份职责，禁止退化为「蓝色渐变 + 文字」）：
 *   L1 issuer palette 渐变（CardIdentityProfile.palette）
 *   L2 版式带（profile.layout） / L3 issuer motif（profile.motif） / L4 材质（profile.material）
 *   L5 Studio 主题 artwork（card.preset 主题层；主题切换真实改变画面）
 *
 * 几何绘制集中在 CardIdentityArtwork.kt；本文件只负责组合与内容排版。
 * 测试 tag：pdig.card.*（PhoneCardsLayoutContractTest 读取真实 bounds）。
 */

/** Studio 主题的 artwork 种类（每主题几何显著不同，Human 可一眼区分）。 */
enum class CardThemeArtwork { MINIMAL, DEEP_SPACE, REGION, CITY, GLASS, METAL, ABSTRACT }

/** 主题信息：内部 id 保留（渲染 / 存储），用户语言走 [com.pdig.uivnext.model.themeLabelZh]。 */
data class CardThemeInfo(val id: String, val artwork: CardThemeArtwork)

fun cardThemeInfo(preset: String): CardThemeInfo = when (preset) {
    "deep-space" -> CardThemeInfo(preset, CardThemeArtwork.DEEP_SPACE)
    "region" -> CardThemeInfo(preset, CardThemeArtwork.REGION)
    "city" -> CardThemeInfo(preset, CardThemeArtwork.CITY)
    "glass" -> CardThemeInfo(preset, CardThemeArtwork.GLASS)
    "metal" -> CardThemeInfo(preset, CardThemeArtwork.METAL)
    "abstract" -> CardThemeInfo(preset, CardThemeArtwork.ABSTRACT)
    else -> CardThemeInfo(preset, CardThemeArtwork.MINIMAL)
}

/** L1 + 主题微调背景：issuer palette 为底，identity 仍由 issuer 主导。 */
internal fun cardFaceBaseBrush(profile: CardIdentityProfile, theme: CardThemeInfo): Brush = when (theme.artwork) {
    CardThemeArtwork.GLASS -> Brush.linearGradient(
        listOf(profile.palette.first().copy(alpha = 0.92f), profile.palette.getOrElse(1) { profile.palette.first() }.copy(alpha = 0.7f)),
    )
    CardThemeArtwork.DEEP_SPACE -> Brush.radialGradient(
        listOf(profile.palette.first(), profile.palette.getOrElse(1) { profile.palette.first() }, Color(0xFF030A18)),
        radius = 1200f,
    )
    else -> Brush.linearGradient(
        listOf(profile.palette.first(), profile.palette.getOrElse(1) { profile.palette.first() }, profile.palette.getOrElse(2) { profile.palette.last() }),
    )
}

/**
 * AssetCard —— 独立可操作对象卡面（grid / detail / studio 唯一入口）。
 * 全部视觉委托给 [CardIdentityFace]；不允许在别处重复实现卡面。
 */
@Composable
fun AssetCard(
    card: UiVNextCard,
    privacyMask: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    presentationMaterial: String? = null,
    presentationAccent: Color? = null,
    presentationLayout: String? = null,
) = CardIdentityFace(
    card = card,
    privacyMask = privacyMask,
    onClick = onClick,
    modifier = modifier,
    presentationMaterial = presentationMaterial,
    presentationAccent = presentationAccent,
    presentationLayout = presentationLayout,
)

/** 卡面 renderer 本体（唯一实现；测试通过 pdig.card.* tags 读取 bounds）。 */
@Composable
fun CardIdentityFace(
    card: UiVNextCard,
    privacyMask: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    presentationMaterial: String? = null,
    presentationAccent: Color? = null,
    presentationLayout: String? = null,
) {
    val profile = CardIdentityProfile.forIssuer(card.issuer)
    val theme = cardThemeInfo(card.preset)
    val compactLayout = presentationLayout == "compact"
    val focusedLayout = presentationLayout == "focused"
    val contentPadding = if (compactLayout) VSpacing.Lg else VSpacing.Xl
    val nicknameSize = if (focusedLayout) 16.sp else if (compactLayout) 13.sp else 14.sp
    val numberSize = if (focusedLayout) 19.sp else if (compactLayout) 14.sp else 16.sp
    val metaSize = if (compactLayout) 11.sp else 12.sp
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(VRadius.Lg))
            .clickable(onClick = onClick)
            .border(1.dp, PdigV2Colors.BorderSubtle, RoundedCornerShape(VRadius.Lg)),
        shape = RoundedCornerShape(VRadius.Lg),
        color = Color.Transparent,
    ) {
        Box(
            Modifier
                .background(cardFaceBaseBrush(profile, theme))
                .aspectRatio(1.586f)
                .padding(contentPadding)
                .testTag(VTestIds.CARD_FACE),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCardArtwork(profile, theme, presentationCardMaterial(presentationMaterial))
                presentationAccent?.let { accent ->
                    drawRect(
                        accent.copy(alpha = 0.82f),
                        topLeft = Offset(0f, size.height - 3.dp.toPx()),
                        size = Size(size.width, 3.dp.toPx()),
                    )
                    drawCircle(
                        accent.copy(alpha = 0.12f),
                        radius = size.width * 0.20f,
                        center = Offset(size.width * 0.88f, size.height * 0.18f),
                    )
                }
            }
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(card.nickname, color = PdigV2Colors.AssetTextPrimary, fontSize = nicknameSize, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag(VTestIds.CARD_NICKNAME))
                        Text(card.issuer, color = PdigV2Colors.AssetTextSecondary, fontSize = 12.sp, modifier = Modifier.testTag(VTestIds.CARD_ISSUER))
                    }
                    Text(
                        if (card.form == "virtual") "虚拟卡" else "实体卡",
                        color = PdigV2Colors.AssetTextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.testTag(VTestIds.CARD_FORM),
                    )
                }
                Spacer(Modifier.weight(1f))
                // Material identity: a visible EMV/contactless motif, rendered locally and
                // independently of sensitive card data. The network label remains factual.
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        color = Color(0xFFD9B979).copy(alpha = 0.91f),
                        shape = RoundedCornerShape(5.dp),
                    ) {
                        Text("▤", Modifier.padding(horizontal = 9.dp, vertical = 2.dp), color = Color(0xFF604620), fontSize = 16.sp)
                    }
                    Text(")))", color = PdigV2Colors.AssetTextSecondary, fontSize = 17.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (privacyMask) "•••• •••• •••• ••••" else "•••• •••• •••• ${card.last4}",
                    color = PdigV2Colors.AssetTextPrimary,
                    fontSize = numberSize,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag(VTestIds.CARD_MASKED),
                )
                Spacer(Modifier.height(VSpacing.Lg))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("${cardTypeLabel(card.type)} · ${card.network}", color = PdigV2Colors.AssetTextSecondary, fontSize = metaSize, modifier = Modifier.testTag(VTestIds.CARD_META))
                        Text("${regionLabelZh(card.region)} · ${card.currency} · 到期 ${card.expiry}", color = PdigV2Colors.AssetTextMuted, fontSize = metaSize)
                    }
                    StatusBadge(card.status)
                }
            }
        }
    }
}

/**
 * Compact card thumbnail used by phone list rows.
 * It reuses the same issuer palette and artwork language as the full identity face without forcing
 * full card metadata into a narrow miniature.
 */
@Composable
fun CardIdentityThumbnail(
    card: UiVNextCard,
    privacyMask: Boolean,
    modifier: Modifier = Modifier,
) {
    val profile = CardIdentityProfile.forIssuer(card.issuer)
    val theme = cardThemeInfo(card.preset)
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(VRadius.Md))
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(VRadius.Md)),
        color = Color.Transparent,
        shape = RoundedCornerShape(VRadius.Md),
    ) {
        Box(
            Modifier
                .background(cardFaceBaseBrush(profile, theme))
                .aspectRatio(1.586f)
                .padding(10.dp),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCardArtwork(profile, theme, null)
            }
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        card.issuer,
                        color = PdigV2Colors.AssetTextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    Text(")))", color = PdigV2Colors.AssetTextSecondary, fontSize = 9.sp)
                }
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (privacyMask) "••••" else "•••• " + card.last4,
                        color = PdigV2Colors.AssetTextPrimary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                    )
                    Text(
                        card.network,
                        color = PdigV2Colors.AssetTextPrimary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

internal fun cardTypeLabel(type: String): String = if (type == "credit") "信用卡" else "储蓄卡"

private fun presentationCardMaterial(id: String?): CardMaterial? = when (id) {
    "glass" -> CardMaterial.GLASS
    "metal" -> CardMaterial.METAL
    "brushed" -> CardMaterial.BRUSHED
    "matte" -> CardMaterial.MATTE
    "satin" -> CardMaterial.SATIN
    "translucent" -> CardMaterial.TRANSLUCENT
    else -> null
}
