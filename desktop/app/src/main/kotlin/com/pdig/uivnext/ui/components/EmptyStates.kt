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
            .widthIn(max = 600.dp)
            .padding(top = VSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(VSpacing.Md),
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(VRadius.Md))
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEmptyMotif(motif: EmptyMotif) {
    val stroke = 2f
    val c = PdigV2Colors.TextMuted
    when (motif) {
        EmptyMotif.CARD -> {
            drawRoundRect(c.copy(alpha = 0.85f), Offset(10f, 12f), Size(24f, 20f), CornerRadius(3f), style = Stroke(width = stroke))
            drawLine(c.copy(alpha = 0.6f), Offset(14f, 18f), Offset(30f, 18f), strokeWidth = 1.5f)
            drawLine(c.copy(alpha = 0.4f), Offset(14f, 25f), Offset(26f, 25f), strokeWidth = 1.5f)
        }
        EmptyMotif.NUMBER -> {
            drawRoundRect(c.copy(alpha = 0.85f), Offset(12f, 10f), Size(20f, 24f), CornerRadius(10f), style = Stroke(width = stroke))
            drawLine(c.copy(alpha = 0.7f), Offset(17f, 17f), Offset(27f, 17f), strokeWidth = 1.5f)
            drawLine(c.copy(alpha = 0.7f), Offset(17f, 23f), Offset(27f, 23f), strokeWidth = 1.5f)
        }
        EmptyMotif.REGION -> {
            drawCircle(c.copy(alpha = 0.85f), radius = 9f, center = Offset(22f, 16f), style = Stroke(width = stroke))
            drawCircle(c.copy(alpha = 0.6f), radius = 2.5f, center = Offset(22f, 16f))
            drawLine(c.copy(alpha = 0.7f), Offset(22f, 25f), Offset(22f, 32f), strokeWidth = 1.5f)
        }
        EmptyMotif.CHECK -> {
            drawCircle(c.copy(alpha = 0.85f), radius = 13f, center = Offset(22f, 22f), style = Stroke(width = stroke))
            drawLine(c.copy(alpha = 0.9f), Offset(17f, 22f), Offset(21f, 26f), strokeWidth = 2f)
            drawLine(c.copy(alpha = 0.9f), Offset(21f, 26f), Offset(27f, 17f), strokeWidth = 2f)
        }
        EmptyMotif.LIST -> {
            drawLine(c.copy(alpha = 0.8f), Offset(12f, 14f), Offset(32f, 14f), strokeWidth = 2f)
            drawLine(c.copy(alpha = 0.5f), Offset(12f, 20f), Offset(32f, 20f), strokeWidth = 2f)
            drawLine(c.copy(alpha = 0.5f), Offset(12f, 26f), Offset(32f, 26f), strokeWidth = 2f)
        }
    }
}
