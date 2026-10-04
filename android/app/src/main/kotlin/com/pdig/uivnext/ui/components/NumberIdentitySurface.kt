package com.pdig.uivnext.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.UiVNextNumber
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing

/** 号码面程序化背景（communication identity 语言；每 preset 确定性可区分，绝不退化为银行卡视觉）。 */
fun numberFaceBrush(preset: String): Brush = when (preset) {
    "country" -> Brush.linearGradient(listOf(Color(0xFF15395E), Color(0xFF0A1B33)))
    "city" -> Brush.linearGradient(listOf(Color(0xFF2A3F66), Color(0xFF101B2E)))
    "minimal" -> Brush.linearGradient(listOf(Color(0xFF1C2433), Color(0xFF0D141F)))
    "banking" -> Brush.linearGradient(listOf(Color(0xFF173A45), Color(0xFF0B1F26)))
    "travel" -> Brush.linearGradient(listOf(Color(0xFF2C3A5E), Color(0xFF16233D), Color(0xFF0B1730)))
    "recovery" -> Brush.linearGradient(listOf(Color(0xFF3A3A22), Color(0xFF1E1F12), Color(0xFF0F100A)))
    "work" -> Brush.linearGradient(listOf(Color(0xFF1F3352), Color(0xFF101B2F)))
    "private" -> Brush.linearGradient(listOf(Color(0xFF3A2C50), Color(0xFF1D1330)))
    else -> Brush.linearGradient(listOf(Color(0xFF15395E), Color(0xFF0A1B33)))
}

/** 号码面强调色（每 preset 独立；配合 DialArc / SignalBars 形成 communication identity）。 */
private fun numberFaceAccent(preset: String): Color = when (preset) {
    "travel" -> Color(0xFF7FB2FF)
    "recovery" -> Color(0xFFFFC864)
    "banking" -> Color(0xFF6FE3D4)
    "work" -> Color(0xFF9FC7FF)
    "private" -> Color(0xFFC8A7FF)
    "city" -> Color(0xFF7FE0FF)
    "minimal" -> Color(0xFFAEBFDF)
    else -> PdigV2Colors.PrimaryBright
}

/**
 * NumberFace：号码身份面（mobile 与 detail 顶部共用）。
 * communication identity：拨号弧 + 信号条 + preset 背景；禁止银行卡视觉。
 */
@Composable
fun NumberFace(
    number: UiVNextNumber,
    privacyMask: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    presentationMaterial: String? = null,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(VRadius.Lg))
            .clickable(onClick = onClick)
            .border(1.dp, PdigV2Colors.BorderSubtle, RoundedCornerShape(VRadius.Lg)),
        color = Color.Transparent,
        shape = RoundedCornerShape(VRadius.Lg),
    ) {
        Box(Modifier.background(numberFaceBrush(number.preset))) {
            Canvas(Modifier.matchParentSize()) {
                drawNumberMaterialOverlay(presentationMaterial)
            }
            Column(Modifier.padding(VSpacing.Xl)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    DialArc(accent = numberFaceAccent(number.preset))
                    Spacer(Modifier.width(VSpacing.Md))
                    Text(number.nickname, color = PdigV2Colors.TextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    StatusBadge(number.status)
                }
                Spacer(Modifier.height(VSpacing.Md))
                Text(
                    if (privacyMask) maskedNumberForPrivacy(number) else number.maskedNumber,
                    color = PdigV2Colors.TextPrimary,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(VSpacing.Md))
                Row(horizontalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
                    LabelChip(if (number.simKind == "eSIM") "eSIM" else "实体 SIM")
                    LabelChip(roleLabel(number.role))
                    LabelChip(number.countryCode)
                    if (number.recoveryOnly) LabelChip("唯一恢复路径", highlight = true)
                }
                Spacer(Modifier.height(VSpacing.Md))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        number.usages.joinToString(" · "),
                        color = PdigV2Colors.TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                    )
                    SignalBars(level = signalLevel(number), accent = numberFaceAccent(number.preset))
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNumberMaterialOverlay(material: String?) {
    when (material) {
        "glass" -> {
            drawRect(
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.18f), Color.Transparent, Color.White.copy(alpha = 0.05f)),
                    start = Offset(size.width * 0.08f, 0f),
                    end = Offset(size.width * 0.74f, size.height),
                ),
            )
            drawRect(
                Color.White.copy(alpha = 0.08f),
                topLeft = Offset(size.width * 0.18f, 0f),
                size = Size(size.width * 0.12f, size.height),
            )
        }
        "metal" -> {
            repeat(12) { i ->
                val y = size.height * i / 12f
                drawRect(
                    Color.White.copy(alpha = if (i % 2 == 0) 0.055f else 0.015f),
                    topLeft = Offset(0f, y),
                    size = Size(size.width, size.height / 12f + 1f),
                )
            }
        }
        "matte" -> {
            drawRect(Color.Black.copy(alpha = 0.08f))
            repeat(18) { i ->
                val x = size.width * ((i * 37) % 97) / 100f
                val y = size.height * ((i * 53) % 89) / 100f
                drawCircle(Color.White.copy(alpha = 0.025f), radius = 1.1f, center = Offset(x, y))
            }
        }
    }
}

/** 拨号弧（communication identity 主 motif）。 */
@Composable
private fun DialArc(accent: Color) {
    Canvas(Modifier.width(44.dp).height(22.dp)) {
        drawArc(
            color = accent.copy(alpha = 0.35f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(0f, 0f),
            size = Size(size.width, size.height * 2f),
            style = Stroke(width = 7.dp.toPx()),
        )
        drawArc(
            color = accent.copy(alpha = 0.9f),
            startAngle = 180f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(0f, 0f),
            size = Size(size.width, size.height * 2f),
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

/** 信号条（communication identity 主 motif；满格按角色）。 */
@Composable
private fun SignalBars(level: Int, accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
        listOf(6, 10, 14, 18, 22).forEachIndexed { index, h ->
            Box(
                Modifier
                    .width(4.dp)
                    .height(h.dp)
                    .background(
                        if (index < level) accent.copy(alpha = 0.9f) else accent.copy(alpha = 0.22f),
                        RoundedCornerShape(1.dp),
                    ),
            )
        }
    }
}

/** 信号满格（primary=5 / secondary=3 / keep=2；纯呈现派生，不改变语义角色）。 */
private fun signalLevel(number: UiVNextNumber): Int = when (number.role) {
    "primary" -> 5
    "secondary" -> 3
    "keep" -> 2
    else -> 4
}

private fun roleLabel(role: String): String = when (role) {
    "primary" -> "主号"
    "secondary" -> "副号"
    "keep" -> "保号"
    else -> role
}



private fun maskedNumberForPrivacy(number: UiVNextNumber): String =
    number.countryCode + " •••• ••••"
