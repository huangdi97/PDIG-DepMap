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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
 * NumberIdentitySurface —— 「全球通信身份」身份面（PHASE 1D §23 + PHASE 1F §22–24）。
 *
 * 不是 payment card：不使用卡语法（rounded-rect + 左右色块为主）。
 * 通信专属语法：
 *   - 左上：region 标记 + 国家/地区名
 *   - 主身份：大号国际区号（+86，最强视觉元素）
 *   - 号码：138 **** 8823（mono 大号）
 *   - 次要：运营商 · SIM 形态
 *   - 角色簇：主号 / 银行验证 / 2FA / 注册
 *   - 恢复：唯一恢复路径
 *   - 右上：continuity ring（小而有语义）
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
            // 左上 region 标记 + 国家名；右上 continuity ring + status
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    RegionBadge(number.region, badgeSize = 40.dp)
                    Spacer(Modifier.height(VSpacing.Sm))
                    Text(
                        regionNameZh(number.region),
                        style = VType.Label,
                        color = PdigV2Colors.TextSecondary,
                        maxLines = 1,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    ContinuityRing(number.status, size = 36.dp)
                    Spacer(Modifier.height(VSpacing.Sm))
                    StatusBadge(number.status)
                }
            }
            // 主身份：国际区号（最强视觉元素）
            Text(
                number.countryCode,
                style = VType.MajorNumber.copy(fontSize = 36.sp, fontWeight = FontWeight.Bold),
                color = PdigV2Colors.TextPrimary,
                maxLines = 1,
            )
            // 号码（大号，横向，永不竖排）
            Text(
                if (privacyMask && p.maskSensitive) maskedFully(number.maskedNumber) else number.maskedNumber,
                style = VType.MajorNumber.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                fontFamily = FontFamily.Monospace,
                color = PdigV2Colors.TextPrimary,
                maxLines = 1,
            )
            // 通信路径 + 运营商 · SIM 形态
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
                LabelChip(number.carrier)
                LabelChip(if (number.simKind == "eSIM") "eSIM" else "实体 SIM")
            }
            // 角色簇：主号 / 用途
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
                LabelChip(roleLabel(number.role))
                number.usages.take(3).forEach { usage -> LabelChip(usage) }
            }
            // 恢复能力
            if (number.recoveryOnly) {
                LabelChip("唯一恢复路径", highlight = true)
            }
        }
    }
}

internal fun regionNameZh(region: String): String = when (region) {
    "CN" -> "中国大陆"
    "HK" -> "中国香港"
    "MO" -> "中国澳门"
    "GB" -> "英国"
    "US" -> "美国"
    "SG" -> "新加坡"
    else -> region
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

/** ContinuityRing：状态语义环（连续性；小而有意）。 */
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
