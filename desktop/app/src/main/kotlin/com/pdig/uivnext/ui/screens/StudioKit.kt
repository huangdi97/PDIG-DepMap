package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VType

/** 材质中文名。 */
internal fun materialLabel(m: String): String = when (m) {
    "matte" -> "哑光"
    "glass" -> "玻璃"
    "metal" -> "金属"
    "minimal" -> "极简"
    else -> m
}

/** 布局中文名。 */
internal fun layoutLabel(l: String): String = when (l) {
    "emblem" -> "徽章"
    "minimal-content" -> "精简"
    else -> "标准"
}

/** 舞台绘制：spotlight（中心聚光）+ 卡片下方 soft floor 椭圆反射 + 环境辉光。 */
internal fun DrawScope.drawPreviewStage() {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(PdigV2Colors.CanvasDeep.copy(alpha = 0.6f), PdigV2Colors.Canvas)))
    drawCircle(
        brush = Brush.radialGradient(
            listOf(PdigV2Colors.LocalIllum.copy(alpha = 0.40f), Color.Transparent),
            center = Offset(w * 0.5f, h * 0.44f),
            radius = w * 0.55f,
        ),
        radius = w * 0.55f,
        center = Offset(w * 0.5f, h * 0.44f),
    )
    drawOval(
        brush = Brush.radialGradient(
            listOf(PdigV2Colors.PrimaryBright.copy(alpha = 0.12f), Color.Transparent),
            center = Offset(w * 0.5f, h * 0.86f),
            radius = w * 0.30f,
        ),
        topLeft = Offset(w * 0.5f - w * 0.30f, h * 0.86f - w * 0.10f),
        size = Size(w * 0.60f, w * 0.20f),
    )
}

/** 材质 visual tile：直接渲染材质层小样（不读文字也能区分）。 */
@Composable
internal fun MaterialTile(
    material: String,
    profile: PresentationProfile,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val previewProfile = profile.copy(material = material)
    Box(
        Modifier
            .size(width = 64.dp, height = 44.dp)
            .clip(RoundedCornerShape(VRadius.Md))
            .drawBehind { drawMaterialTile(previewProfile) }
            .drawBehind { drawMaterialTileBorder(selected) }
            .clickable(onClick = onClick),
    ) {
        Text(
            materialLabel(material),
            Modifier.align(Alignment.BottomStart).padding(4.dp),
            color = PdigV2Colors.TextPrimary,
            fontSize = 9.sp,
            maxLines = 1,
        )
    }
}

private fun DrawScope.drawMaterialTile(p: PresentationProfile) {
    val w = size.width
    val h = size.height
    drawRect(Brush.linearGradient(listOf(PdigV2Colors.SurfaceRaised, PdigV2Colors.CanvasDeep)))
    com.pdig.uivnext.ui.components.CardMaterial.material(this, p, w, h)
}

private fun DrawScope.drawMaterialTileBorder(selected: Boolean) {
    val color = if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.BorderSubtle
    val stroke = if (selected) 2.dp.toPx() else 1.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke / 2f, stroke / 2f),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(8f),
        style = Stroke(width = stroke),
    )
}

/** 主题视觉缩略图（2 列大 tile；选中 = 边框高亮，无 toggle dot）。 */
@Composable
internal fun ThemeThumb(
    thumbnail: @Composable (String) -> Unit,
    preset: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .clickable(onClick = onClick)
            .then(
                if (selected) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = PdigV2Colors.PrimaryBright.copy(alpha = 0.9f),
                            style = Stroke(width = 2.dp.toPx()),
                            cornerRadius = CornerRadius(8f),
                        )
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        thumbnail(preset)
    }
}

/** 对象库行：小缩略图 + nickname，选中高亮。 */
@Composable
internal fun LibraryItem(
    label: String,
    selected: Boolean,
    thumbnail: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (selected) PdigV2Colors.PrimaryBright.copy(alpha = 0.14f) else Color.Transparent,
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, if (selected) PdigV2Colors.PrimaryBright.copy(alpha = 0.3f) else PdigV2Colors.BorderSubtle),
    ) {
        Row(
            Modifier.padding(horizontal = VSpacing.Sm, vertical = VSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(width = 40.dp, height = 26.dp).clip(RoundedCornerShape(VRadius.Sm))) {
                thumbnail()
            }
            Spacer(Modifier.width(VSpacing.Md))
            Text(
                label,
                color = if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.TextPrimary,
                style = VType.Label,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 文本 chip 选择器（背景/布局等次要选项）。 */
@Composable
internal fun ChipRow(label: String, selected: Boolean, compact: Boolean = false, onClick: () -> Unit) {
    Surface(
        Modifier.clickable(onClick = onClick),
        color = if (selected) PdigV2Colors.PrimaryBright.copy(alpha = 0.14f) else PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Sm),
        border = BorderStroke(1.dp, if (selected) PdigV2Colors.PrimaryBright.copy(alpha = 0.3f) else PdigV2Colors.BorderSubtle),
    ) {
        Text(
            label,
            Modifier.padding(horizontal = if (compact) VSpacing.Md else VSpacing.Lg, vertical = if (compact) 4.dp else 6.dp),
            color = if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.TextSecondary,
            style = VType.Label,
        )
    }
}

@Composable
internal fun GroupLabel(text: String) {
    Text(text, style = VType.Label, color = PdigV2Colors.TextMuted)
}

@Composable
internal fun SwatchDot(color: Color, selected: Boolean, onClick: () -> Unit, size: androidx.compose.ui.unit.Dp = 30.dp) {
    Surface(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        color = color,
        border = BorderStroke(
            if (selected) 3.dp else 1.dp,
            if (selected) PdigV2Colors.TextPrimary else PdigV2Colors.BorderSubtle,
        ),
    ) {}
}
@Composable
internal fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Surface(
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Md),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    ) {
        Row(Modifier.padding(horizontal = VSpacing.Md, vertical = VSpacing.Sm).clickable(onClick = onToggle), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = PdigV2Colors.TextSecondary, style = VType.Secondary)
            Surface(color = if (checked) PdigV2Colors.Primary.copy(alpha = 0.3f) else PdigV2Colors.Surface, shape = RoundedCornerShape(VRadius.Sm)) {
                Text(
                    if (checked) "开" else "关",
                    Modifier.padding(horizontal = VSpacing.Md, vertical = 3.dp),
                    color = if (checked) PdigV2Colors.PrimaryBright else PdigV2Colors.TextMuted,
                    style = VType.Label,
                )
            }
        }
    }
}

@Composable
internal fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = PdigV2Colors.TextMuted, style = VType.Secondary)
        Text(value, color = PdigV2Colors.TextPrimary, style = VType.Secondary, fontWeight = FontWeight.Medium)
    }
}
