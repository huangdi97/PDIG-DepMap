package com.pdig.uivnext.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VTouchTarget

/**
 * EmptyState —— 空态（任务书 §22）：semantic illustration + title + description + primary/secondary CTA。
 * honest unknown 语义：description 不得出现「一切安全 / 100% safe / 没有问题」等伪安全词；
 * 数据缺失时使用「未记录 ≠ 无风险」措辞。
 */
@Composable
fun EmptyState(
    kind: EmptyKind,
    title: String,
    description: String,
    primaryCta: String,
    onPrimary: () -> Unit,
    secondaryCta: String? = null,
    onSecondary: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val accent = PdigV2Colors.PrimaryBright
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = VSpacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VSpacing.Md),
    ) {
        // Semantic illustration（程序化、确定性；非装饰性占位图）
        Box(
            Modifier
                .size(96.dp)
                .background(PdigV2Colors.SurfaceGlass, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            when (kind) {
                EmptyKind.CARDS -> CardEmptyIcon(accent)
                EmptyKind.NUMBERS -> SignalEmptyIcon(accent)
                EmptyKind.REGION -> GlobeEmptyIcon(accent)
                EmptyKind.CHANGE -> ContinuityEmptyIcon(accent)
                EmptyKind.ATTENTION -> AttentionEmptyIcon(accent)
                EmptyKind.DEPENDENCIES -> DependencyEmptyIcon(accent)
            }
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = PdigV2Colors.TextPrimary, textAlign = TextAlign.Center)
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = PdigV2Colors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.85f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(VSpacing.Md)) {
            Surface(
                modifier = Modifier
                    .defaultMinSize(minHeight = VTouchTarget.Min)
                    .clickable(onClick = onPrimary)
                    .testTag("pdig.empty.primary"),
                color = PdigV2Colors.PrimaryBright,
                shape = RoundedCornerShape(VRadius.Md),
            ) {
                Text(
                    primaryCta,
                    Modifier.padding(horizontal = VSpacing.Lg, vertical = VSpacing.Sm),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
            if (secondaryCta != null && onSecondary != null) {
                Surface(
                    modifier = Modifier
                        .defaultMinSize(minHeight = VTouchTarget.Min)
                        .clickable(onClick = onSecondary),
                    color = PdigV2Colors.SurfaceRaised,
                    shape = RoundedCornerShape(VRadius.Md),
                    border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                ) {
                    Text(
                        secondaryCta,
                        Modifier.padding(horizontal = VSpacing.Lg, vertical = VSpacing.Sm),
                        color = PdigV2Colors.TextPrimary,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

/** 空态语义种类（与任务书 §22 六类对应）。 */
enum class EmptyKind { CARDS, NUMBERS, REGION, CHANGE, ATTENTION, DEPENDENCIES }

// ── Semantic illustration 图标（程序化几何，每类语义可区分） ─────────────────────────

@Composable
private fun CardEmptyIcon(accent: Color) {
    Box(
        Modifier
            .width(44.dp)
            .height(28.dp)
            .border(1.5.dp, accent.copy(alpha = 0.8f), RoundedCornerShape(4.dp)),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 5.dp)
                .width(10.dp)
                .height(2.dp)
                .background(accent.copy(alpha = 0.6f)),
        )
    }
}

@Composable
private fun SignalEmptyIcon(accent: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        listOf(6, 12, 18, 24).forEachIndexed { index, h ->
            Box(
                Modifier
                    .width(4.dp)
                    .height(h.dp)
                    .background(accent.copy(alpha = if (index < 3) 0.85f else 0.35f), RoundedCornerShape(2.dp)),
            )
        }
    }
}

@Composable
private fun GlobeEmptyIcon(accent: Color) {
    Box(
        Modifier
            .size(40.dp)
            .border(1.5.dp, accent.copy(alpha = 0.8f), CircleShape),
    ) {
        Box(
            Modifier
                .align(Alignment.Center)
                .width(40.dp)
                .height(1.dp)
                .background(accent.copy(alpha = 0.45f)),
        )
    }
}

@Composable
private fun ContinuityEmptyIcon(accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(accent.copy(alpha = 0.4f), CircleShape))
        Box(Modifier.width(20.dp).height(2.dp).background(accent.copy(alpha = 0.5f)))
        Box(Modifier.size(12.dp).background(accent, CircleShape))
    }
}

@Composable
private fun AttentionEmptyIcon(accent: Color) {
    Box(
        Modifier
            .size(14.dp)
            .background(accent.copy(alpha = 0.25f), CircleShape)
            .border(1.5.dp, accent.copy(alpha = 0.8f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(4.dp).background(accent, CircleShape))
    }
}

@Composable
private fun DependencyEmptyIcon(accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(accent.copy(alpha = 0.5f), CircleShape))
        Box(Modifier.size(10.dp).background(accent.copy(alpha = 0.5f), CircleShape))
    }
}
