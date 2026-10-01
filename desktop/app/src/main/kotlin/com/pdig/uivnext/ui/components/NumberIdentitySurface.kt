package com.pdig.uivnext.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.model.UiVNextNumber
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VType
import com.pdig.uivnext.theme.statusColor

/**
 * NumberIdentitySurface —— 「全球通信身份」身份面（PHASE 1D §23）。
 *
 * 不是 payment card：1.9:1 横版身份面，以 地区身份 / 国际区号 / 号码 /
 * 运营商 / SIM 形态 / 角色 / 连续性状态 构成通信身份语法：
 *   - 左：大号地区 code（RegionBadge 放大）+ 国际区号 + 号码（MajorNumber）；
 *   - 右：continuity ring（连续环，status 语义）+ 状态徽标；
 *   - 底：运营商 / SIM 形态 / 角色 / 用途 chips + 一条「通信线路」视觉基线。
 * 确定性、全部 token 色、离线。
 */
@Composable
fun NumberIdentitySurface(
    number: UiVNextNumber,
    privacyMask: Boolean,
    modifier: Modifier = Modifier,
    profile: PresentationProfile? = null,
) {
    val p = profile ?: PresentationProfile.defaultFor("phoneNumber", number.id, "country")
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(VRadius.Xl))
            .drawBehind { drawIdentityBackdrop(p, number) },
        shape = RoundedCornerShape(VRadius.Xl),
        color = Color.Transparent,
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(Modifier.padding(VSpacing.Xxl), verticalArrangement = Arrangement.spacedBy(VSpacing.Md)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                // Region identity：放大地区码 + 国际区号
                Column {
                    RegionBadge(number.region, badgeSize = 44.dp)
                    Spacer(Modifier.height(VSpacing.Sm))
                    Text(
                        number.countryCode,
                        style = VType.Secondary,
                        color = PdigV2Colors.TextSecondary,
                        maxLines = 1,
                    )
                }
                // Continuity ring + status
                Column(horizontalAlignment = Alignment.End) {
                    ContinuityRing(number.status, size = 44.dp)
                    Spacer(Modifier.height(VSpacing.Sm))
                    StatusBadge(number.status)
                }
            }
            Spacer(Modifier.height(VSpacing.Xs))
            // Number（大号，横向，永不竖排）
            Text(
                if (privacyMask && p.maskSensitive) maskedFully(number.maskedNumber) else number.maskedNumber,
                style = VType.MajorNumber.copy(fontSize = 30.sp),
                fontFamily = FontFamily.Monospace,
                color = PdigV2Colors.TextPrimary,
                maxLines = 1,
            )
            // 通信线路视觉基线 + 运营商/SIM/角色
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                LabelChip(number.carrier)
                LabelChip(if (number.simKind == "eSIM") "eSIM" else "实体 SIM")
                LabelChip(roleLabel(number.role))
                if (number.recoveryOnly) LabelChip("唯一恢复路径", highlight = true)
            }
            Text(
                "用途：${number.usages.joinToString(" · ")}",
                style = VType.Secondary,
                color = PdigV2Colors.TextSecondary,
                maxLines = 2,
            )
        }
    }
}

/** 身份面背景：region-tinted 渐变 + 底部通信线路。 */
private fun DrawScope.drawIdentityBackdrop(p: PresentationProfile, number: UiVNextNumber) {
    val w = size.width
    val h = size.height
    val base: Brush = when (number.region) {
        "CN" -> Brush.linearGradient(listOf(PdigV2Colors.LandHighlight, PdigV2Colors.OceanBase, PdigV2Colors.CanvasDeep))
        "HK" -> Brush.linearGradient(listOf(PdigV2Colors.NightCityLight.copy(alpha = 0.28f), PdigV2Colors.CanvasDeep))
        "MO" -> Brush.linearGradient(listOf(PdigV2Colors.LandTextureHi.copy(alpha = 0.3f), PdigV2Colors.CanvasDeep))
        "GB" -> Brush.linearGradient(listOf(PdigV2Colors.TerminatorLight.copy(alpha = 0.18f), PdigV2Colors.CanvasDeep))
        "US" -> Brush.linearGradient(listOf(PdigV2Colors.RegionNodeHi.copy(alpha = 0.22f), PdigV2Colors.CanvasDeep))
        "SG" -> Brush.linearGradient(listOf(PdigV2Colors.Primary.copy(alpha = 0.26f), PdigV2Colors.CanvasDeep))
        else -> Brush.linearGradient(listOf(PdigV2Colors.Surface, PdigV2Colors.CanvasDeep))
    }
    drawRect(base)
    // 左缘 accent 洗色（克制）
    val accent = accentColorOf(p.accentColor)
    drawRect(
        Brush.horizontalGradient(listOf(accent.copy(alpha = 0.16f), Color.Transparent)),
        size = Size(w * 0.45f, h),
    )
    // 通信线路基线：横线 + 光点（identity 语法，非装饰网格）
    drawLine(
        color = PdigV2Colors.BorderStrong.copy(alpha = 0.35f),
        start = Offset(w * 0.04f, h - 6f),
        end = Offset(w * 0.96f, h - 6f),
        strokeWidth = 1.2f,
    )
    for (i in 0 until 5) {
        val x = w * (0.12f + 0.19f * i)
        drawCircle(PdigV2Colors.PrimaryBright.copy(alpha = 0.5f), radius = 1.6f, center = Offset(x, h - 6f))
    }
}

/** ContinuityRing：状态语义环（连续性）。 */
@Composable
fun ContinuityRing(status: String, size: Dp = 40.dp) {
    val color = statusColor(status)
    Box(
        Modifier
            .size(size)
            .drawBehind { drawRing(color, 0.82f) },
    ) {}
}

private fun DrawScope.drawRing(color: Color, sweepFraction: Float) {
    val stroke = size.minDimension * 0.09f
    drawCircle(
        color = color.copy(alpha = 0.18f),
        radius = size.minDimension / 2f - stroke,
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
    val start = -90f
    val sweep = 360f * sweepFraction
    drawArc(
        color = color,
        startAngle = start,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(stroke / 2f, stroke / 2f),
        size = Size(size.width - stroke, size.height - stroke),
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
}

private fun maskedFully(masked: String): String =
    masked.map { if (it.isDigit()) '•' else it }.joinToString("")
