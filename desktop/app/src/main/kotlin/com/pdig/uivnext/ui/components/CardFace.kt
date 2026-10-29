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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.model.UiVNextCard
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VType
import com.pdig.uivnext.ui.components.collectVNextInteraction
import com.pdig.uivnext.ui.components.rememberVNextInteractionSource
import java.io.File

/**
 * AssetCard —— 卡面 = 真实支付卡资产身份（PHASE 1D §12–§15 + PHASE 1F §7–§11）。
 *
 * 视觉语法分层：Identity Background（§9 身份色板 + motif）→ Material → Content；
 * 内容排版（standard / emblem / minimal-content）拆分在 CardFaceContent.kt。
 * 每张 demo 卡 ≥3 个身份要素（palette / accent geometry / material / artwork），
 * 永不是「深色矩形 + issuer + PAN」。保持 1.586 ratio。
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

/** 卡默认视觉（PHASE 1D §13 + PHASE 1F §9：issuer 差异化，synthetic demo；仅呈现层）。 */
fun cardProfileOf(card: UiVNextCard): PresentationProfile = defaultCardProfile(card)

@Composable
fun AssetCard(
    card: UiVNextCard,
    privacyMask: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    profile: PresentationProfile? = null,
) {
    val p = profile ?: cardProfileOf(card)
    // 自定义背景：app-managed 文件 → 解码为位图（缩略图/预览/网格共用；解码失败回退主题）。
    val backgroundBitmap: ImageBitmap? = remember(p.backgroundKind, p.backgroundValue) {
        if (p.backgroundKind == "imported" && p.backgroundValue.isNotBlank()) {
            decodeLocalImage(File(p.backgroundValue))
        } else {
            null
        }
    }
    val source = rememberVNextInteractionSource()
    val hover = collectVNextInteraction(source).hovered
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(VRadius.Xl))
            .graphicsLayer {
                // PHASE 1E §17/§42：hover 时才倾斜 2–4°（确定性；非 idle 动画）
                rotationX = if (hover) 3f else 0f
                cameraDistance = 24f * density
            }
            .clickable(
                interactionSource = source,
                indication = null,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(VRadius.Xl),
        color = Color.Transparent,
    ) {
        Box(
            Modifier
                .aspectRatio(1.586f)
                .drawBehind { drawCardFaceBackdrop(p, card, backgroundBitmap) }
                .border(1.dp, cardFaceBorder(p), RoundedCornerShape(VRadius.Xl))
                .padding(VSpacing.Xl),
        ) {
            CardContent(card, p, privacyMask)
        }
    }
}

/** 本地图片解码（app-managed storage；解码失败返回 null，回退主题渲染）。 */
internal fun decodeLocalImage(file: File): ImageBitmap? = try {
    if (!file.isFile) null
    else org.jetbrains.skia.Image.makeFromEncoded(file.readBytes())?.toComposeImageBitmap()
} catch (e: Throwable) {
    null
}

/**
 * 卡面背景：Identity 基础色对 + motif 艺术层 + 材质层 + accent 洗色 + rim。
 * internal：供 StudioFrame 的确定性视觉缩略图复用同一渲染器。
 */
internal fun DrawScope.drawCardFaceBackdrop(
    p: PresentationProfile,
    card: UiVNextCard? = null,
    backgroundBitmap: ImageBitmap? = null,
) {
    val w = size.width
    val h = size.height
    val identity = resolveCardIdentity(p, card)
    // 1. Identity base（§9 色板；非 near-black 空占位）
    drawRect(Brush.verticalGradient(listOf(identity.top, identity.bottom)))
    // 2. 艺术层：导入背景 → 用户图片；否则 → identity motif + glow。
    //    PHASE 1F-HF（§4）：artwork 一律使用当前 canvas/local bounds，
    //    clipRect 保证任何几何（circle/glow/arc/contour/city/sweep/ring）都不会
    //    越出本卡面/缩略图边界（ThemeThumbnailBoundsContractTest 覆盖）。
    if (backgroundBitmap != null) {
        drawImage(
            image = backgroundBitmap,
            dstOffset = androidx.compose.ui.unit.IntOffset.Zero,
            dstSize = androidx.compose.ui.unit.IntSize(w.toInt(), h.toInt()),
        )
        drawRect(PdigV2Colors.CanvasDeep.copy(alpha = 0.18f))
    } else {
        clipRect {
            drawCardIdentity(identity, w, h)
        }
    }
    // 3. 材质层（matte vignette / metal 拉丝 / glass rim 同样以本地 bounds 裁剪）
    clipRect {
        CardMaterial.material(this, p, w, h)
    }
    // 4. 左缘 accent 洗色（克制的品牌强调）
    val accent = identity.accent
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
 * 确定性视觉缩略图（Studio 主题选择器用；固定尺寸、固定渲染器、
 * 共享 PresentationProfile 渲染；避免在滚动容器内使用 aspectRatio）。
 * PHASE 1F-HF：Modifier.clip 在 drawBehind 之前，保证 artwork 越界像素
 * 被裁切在缩略图圆角边界内（配合渲染器内部 clipRect 双保险）。
 */
@Composable
fun CardFaceThumbnail(preset: String, modifier: Modifier = Modifier) {
    val profile = PresentationProfile.defaultFor("card", "thumb", preset).copy(
        material = if (preset == "matte") "matte" else if (preset == "glass") "glass" else if (preset == "metal") "metal" else "minimal",
    )
    Box(
        modifier
            .fillMaxWidth(0.92f)
            .height(112.dp)
            .clip(RoundedCornerShape(VRadius.Sm))
            .drawBehind { drawCardFaceBackdrop(profile) }
            .border(1.dp, PdigV2Colors.BorderSubtle, RoundedCornerShape(VRadius.Sm)),
    ) {
        Column(Modifier.fillMaxSize().padding(6.dp)) {
            Text(cardThemeLabel(preset), style = VType.Meta, color = PdigV2Colors.TextSecondary, maxLines = 1)
            Spacer(Modifier.weight(1f))
            Text("•••• •••• •••• ••••", style = VType.Mono, fontSize = 9.sp, color = PdigV2Colors.TextPrimary, maxLines = 1)
        }
    }
}
