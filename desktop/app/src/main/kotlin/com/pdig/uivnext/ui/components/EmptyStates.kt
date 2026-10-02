package com.pdig.uivnext.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VType

/**
 * PHASE 1F —— 紧凑空态（§37–§40）。
 * 不是 1600px 空边框面板：max width 600px 的紧凑组合，
 * 语义小插画（资产剪影）+ 标题 + 说明 + primary CTA + 可选 secondary CTA。
 * Healthy 文案必须诚实：禁止「一切安全 / 100% 正常 / 无风险」。
 */

/** 空态语义小插画（资产剪影；token 色，无 emoji）。 */
enum class EmptyMotif { CARD, NUMBER, REGION, CHECK, LIST }

@Composable
fun EmptyState(
    title: String,
    body: String,
    actionLabel: String? = null,
    secondaryLabel: String? = null,
    modifier: Modifier = Modifier,
    onAction: (() -> Unit)? = null,
    onSecondary: (() -> Unit)? = null,
    motif: EmptyMotif = EmptyMotif.CARD,
) {
    Column(
        modifier
            .widthIn(min = 480.dp, max = 600.dp)
            .padding(top = VSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(VSpacing.Md),
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(VRadius.Lg))
                .background(PdigV2Colors.SurfaceRaised.copy(alpha = 0.7f))
                .drawBehind { drawEmptyMotif(motif) },
            contentAlignment = Alignment.Center,
        ) {}
        Text(title, style = VType.SectionTitle, color = PdigV2Colors.TextPrimary)
        Text(body, style = VType.Secondary, color = PdigV2Colors.TextSecondary)
        if (actionLabel != null && onAction != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VSpacing.Md)) {
                Surface(
                    color = PdigV2Colors.Primary,
                    shape = RoundedCornerShape(VRadius.Md),
                    modifier = Modifier.clickable(onClick = onAction),
                ) {
                    Text(
                        actionLabel,
                        Modifier.padding(horizontal = VSpacing.Xl, vertical = VSpacing.Sm),
                        color = PdigV2Colors.CanvasDeep,
                        style = VType.Label,
                    )
                }
                if (secondaryLabel != null && onSecondary != null) {
                    Surface(
                        color = PdigV2Colors.SurfaceRaised,
                        shape = RoundedCornerShape(VRadius.Md),
                        modifier = Modifier.clickable(onClick = onSecondary),
                    ) {
                        Text(
                            secondaryLabel,
                            Modifier.padding(horizontal = VSpacing.Lg, vertical = VSpacing.Sm),
                            color = PdigV2Colors.TextSecondary,
                            style = VType.Label,
                        )
                    }
                }
            }
        }
    }
}

/** 语义插画：soft glow disc + 资产剪影（按 44dp 设计坐标以 size 比例缩放，
 *  保证在 64dp 展示盒内同样成立；全部 token 色、无 emoji）。 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEmptyMotif(motif: EmptyMotif) {
    val s = size.width / 44f
    val stroke = 2f * s
    val c = PdigV2Colors.TextMuted
    // 柔和背光圆盘（让剪影「站立」在语义背景上）
    drawCircle(
        brush = Brush.radialGradient(
            listOf(PdigV2Colors.PrimaryBright.copy(alpha = 0.12f), Color.Transparent),
            center = Offset(size.width * 0.5f, size.height * 0.5f),
            radius = size.width * 0.46f,
        ),
        radius = size.width * 0.46f,
        center = Offset(size.width * 0.5f, size.height * 0.5f),
    )
    drawCircle(
        color = PdigV2Colors.PrimaryBright.copy(alpha = 0.28f),
        radius = size.width * 0.42f,
        center = Offset(size.width * 0.5f, size.height * 0.5f),
        style = Stroke(width = 1f * s),
    )
    when (motif) {
        EmptyMotif.CARD -> {
            drawRoundRect(c.copy(alpha = 0.85f), Offset(10f * s, 12f * s), Size(24f * s, 20f * s), CornerRadius(3f * s), style = Stroke(width = stroke))
            drawLine(c.copy(alpha = 0.6f), Offset(14f * s, 18f * s), Offset(30f * s, 18f * s), strokeWidth = 1.5f * s)
            drawLine(c.copy(alpha = 0.4f), Offset(14f * s, 25f * s), Offset(26f * s, 25f * s), strokeWidth = 1.5f * s)
        }
        EmptyMotif.NUMBER -> {
            drawRoundRect(c.copy(alpha = 0.85f), Offset(12f * s, 10f * s), Size(20f * s, 24f * s), CornerRadius(10f * s), style = Stroke(width = stroke))
            drawLine(c.copy(alpha = 0.7f), Offset(17f * s, 17f * s), Offset(27f * s, 17f * s), strokeWidth = 1.5f * s)
            drawLine(c.copy(alpha = 0.7f), Offset(17f * s, 23f * s), Offset(27f * s, 23f * s), strokeWidth = 1.5f * s)
        }
        EmptyMotif.REGION -> {
            drawCircle(c.copy(alpha = 0.85f), radius = 9f * s, center = Offset(22f * s, 16f * s), style = Stroke(width = stroke))
            drawCircle(c.copy(alpha = 0.6f), radius = 2.5f * s, center = Offset(22f * s, 16f * s))
            drawLine(c.copy(alpha = 0.7f), Offset(22f * s, 25f * s), Offset(22f * s, 32f * s), strokeWidth = 1.5f * s)
        }
        EmptyMotif.CHECK -> {
            drawCircle(c.copy(alpha = 0.85f), radius = 13f * s, center = Offset(22f * s, 22f * s), style = Stroke(width = stroke))
            drawLine(c.copy(alpha = 0.9f), Offset(17f * s, 22f * s), Offset(21f * s, 26f * s), strokeWidth = 2f * s)
            drawLine(c.copy(alpha = 0.9f), Offset(21f * s, 26f * s), Offset(27f * s, 17f * s), strokeWidth = 2f * s)
        }
        EmptyMotif.LIST -> {
            drawLine(c.copy(alpha = 0.8f), Offset(12f * s, 14f * s), Offset(32f * s, 14f * s), strokeWidth = 2f * s)
            drawLine(c.copy(alpha = 0.5f), Offset(12f * s, 20f * s), Offset(32f * s, 20f * s), strokeWidth = 2f * s)
            drawLine(c.copy(alpha = 0.5f), Offset(12f * s, 26f * s), Offset(32f * s, 26f * s), strokeWidth = 2f * s)
        }
    }
}

