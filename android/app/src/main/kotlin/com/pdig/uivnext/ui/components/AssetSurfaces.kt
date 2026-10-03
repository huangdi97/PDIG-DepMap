package com.pdig.uivnext.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.ChangeStage
import com.pdig.uivnext.model.UiVNextNumber
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.statusColor
import com.pdig.uivnext.theme.statusLabelZh

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
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(VRadius.Lg))
            .clickable(onClick = onClick)
            .border(1.dp, PdigV2Colors.BorderSubtle, RoundedCornerShape(VRadius.Lg)),
        color = Color.Transparent,
        shape = RoundedCornerShape(VRadius.Lg),
    ) {
        Column(
            Modifier
                .background(numberFaceBrush(number.preset))
                .padding(VSpacing.Xl),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                DialArc(accent = numberFaceAccent(number.preset))
                Spacer(Modifier.width(VSpacing.Md))
                Text(number.nickname, color = PdigV2Colors.TextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                StatusBadge(number.status)
            }
            Spacer(Modifier.height(VSpacing.Md))
            Text(
                number.maskedNumber,
                color = PdigV2Colors.TextPrimary,
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(VSpacing.Md))
            Row(horizontalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
                LabelChip(if (number.simKind == "eSIM") "eSIM" else "实体 SIM")
                LabelChip(roleLabel(number.role))
                LabelChip("${number.countryCode} · ${number.region}")
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

/** LabelChip：小标签（用途/角色/徽标）。 */
@Composable
fun LabelChip(text: String, highlight: Boolean = false, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = if (highlight) PdigV2Colors.Warning.copy(alpha = 0.16f) else PdigV2Colors.PrimarySoft,
        shape = RoundedCornerShape(VRadius.Sm),
    ) {
        Text(
            text,
            Modifier.padding(horizontal = VSpacing.Sm, vertical = 3.dp),
            color = if (highlight) PdigV2Colors.Warning else PdigV2Colors.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * ContinuityRail：current → prerequisite → transition → verification → completion。
 * 语义可视化；blocked 必须带明文原因（make-before-break 闸门）。
 */
@Composable
fun ContinuityRail(
    stages: List<ChangeStage>,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
            stages.forEachIndexed { index, stage ->
                RailNode(stage)
                if (index < stages.lastIndex) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(2.dp)
                            .padding(top = 17.dp)
                            .background(
                                if (stage.status == "completed") PdigV2Colors.Positive else PdigV2Colors.BorderSubtle,
                                RoundedCornerShape(1.dp),
                            ),
                    )
                }
            }
        }
        // 闸门/原因说明块（blocked 步骤）
        stages.firstOrNull { it.status == "blocked" }?.let { blocked ->
            Surface(color = PdigV2Colors.Critical.copy(alpha = 0.12f), shape = RoundedCornerShape(VRadius.Md)) {
                Row(
                    Modifier.padding(horizontal = VSpacing.Lg, vertical = VSpacing.Sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Clear, contentDescription = null, tint = PdigV2Colors.Critical, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(VSpacing.Sm))
                    Text(
                        blocked.blockReason ?: "需要先完成前置步骤",
                        color = PdigV2Colors.TextPrimary,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun RailNode(stage: ChangeStage) {
    val color = statusColor(stage.status)
    val label = statusLabelZh(stage.status)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
        Box(
            Modifier
                .size(34.dp)
                .background(
                    if (stage.status == "completed") color else PdigV2Colors.SurfaceRaised,
                    CircleShape,
                )
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            when (stage.status) {
                "completed" -> Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF061225), modifier = Modifier.size(16.dp))
                "blocked" -> Icon(Icons.Filled.Clear, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                "verifying" -> Icon(Icons.Filled.Refresh, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                else -> Text("${stage.stage}", color = color, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stageLabel(stage.key),
            color = if (stage.status == "not_started") PdigV2Colors.TextMuted else PdigV2Colors.TextPrimary,
            fontSize = 10.sp,
            maxLines = 2,
        )
        Text(label, color = color, fontSize = 9.sp)
    }
}

private fun stageLabel(key: String): String = when (key) {
    "impact-analysis" -> "影响分析"
    "establish-new-number" -> "建立新号码"
    "verify-new-number" -> "验证新号码"
    "migrate-key-accounts" -> "迁移关键账户"
    "check-recovery-paths" -> "检查恢复路径"
    "retire-old-number" -> "停用旧号码"
    else -> key
}
