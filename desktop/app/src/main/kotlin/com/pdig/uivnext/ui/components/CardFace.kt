package com.pdig.uivnext.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VType

/**
 * AssetCard —— 卡面 = 真实支付卡资产身份（PHASE 1D §12–§15）。
 *
 * 视觉语法分层：Background Material → Issuer Identity → Card Identity →
 * Financial Metadata → Status Overlay；内容排版（standard / emblem /
 * minimal-content）拆分在 CardFaceContent.kt（≤300 行）。
 * 8 个视觉预设真实不同（非全蓝渐变）；全部程序化（token 色），零远程图片；
 * 保持 1.586 ratio。
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

/** 卡默认视觉（PHASE 1D §13：issuer 差异化，synthetic demo；仅呈现层，绝不写 .depmap）。 */
fun cardProfileOf(card: UiVNextCard): PresentationProfile {
    val vp = com.pdig.uivnext.demo.cardVisualProfileFor(card.id)
    return PresentationProfile.defaultFor("card", card.id, vp.theme).copy(
        material = vp.material,
        accentColor = vp.accent,
        layout = vp.layout,
        backgroundKind = "preset",
        backgroundValue = vp.theme,
    )
}

@Composable
fun AssetCard(
    card: UiVNextCard,
    privacyMask: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    profile: PresentationProfile? = null,
) {
    val p = profile ?: cardProfileOf(card)
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
            CardContent(card, p, privacyMask)
        }
    }
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
    // 2+3. 委托 CardVisualRenderer（PHASE 1C 拆分：CardArtwork + CardMaterial）
    CardArtwork.artwork(this, p, w, h)
    CardMaterial.material(this, p, w, h)
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
            .height(84.dp)
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
