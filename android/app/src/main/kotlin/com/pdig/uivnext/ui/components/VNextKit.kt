package com.pdig.uivnext.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.AttentionItem
import com.pdig.uivnext.model.RegionPresentation
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VTouchTarget
import com.pdig.uivnext.theme.statusColor
import com.pdig.uivnext.theme.statusLabelZh
/** StatusBadge：icon + label + color 三通道（状态永不只靠颜色）。 */
@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val color = statusColor(status)
    val icon: ImageVector = when (status) {
        "critical", "blocked", "expiring_soon" -> Icons.Filled.Warning
        "verifying", "waiting", "not_started" -> Icons.Filled.Refresh
        "completed", "active", "ok", "migrated" -> Icons.Filled.CheckCircle
        else -> Icons.Filled.Info
    }
    Surface(
        modifier = modifier,
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Sm),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Row(
            Modifier.padding(horizontal = VSpacing.Sm, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
            Text(
                statusLabelZh(status),
                style = MaterialTheme.typography.labelSmall,
                color = color,
            )
        }
    }
}

/** SectionHeader：小节标题 + hairline。 */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = PdigV2Colors.TextPrimary,
            )
            trailing?.invoke()
        }
        Spacer(Modifier.height(6.dp))
        HorizontalDivider(color = PdigV2Colors.BorderSubtle, thickness = 1.dp)
    }
}

/** AttentionRow：icon + label + color；critical 最突出；点击去向 target。 */
@Composable
fun AttentionRow(item: AttentionItem, onClick: (AttentionItem) -> Unit, modifier: Modifier = Modifier) {
    val color = statusColor(item.severity)
    val icon = if (item.severity == "critical") Icons.Filled.Clear else Icons.Filled.Warning
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(item) },
        color = if (item.severity == "critical") PdigV2Colors.Critical.copy(alpha = 0.12f) else PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, if (item.severity == "critical") PdigV2Colors.Critical.copy(alpha = 0.4f) else PdigV2Colors.BorderSubtle),
    ) {
        Row(
            Modifier.padding(horizontal = VSpacing.Lg, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(VSpacing.Lg))
            Text(
                item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = PdigV2Colors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = PdigV2Colors.TextMuted, modifier = Modifier.size(16.dp))
        }
    }
}

/** RegionListItem：Globe 的非视觉替代（screen reader / 键盘 / 触屏均可操作）。 */
@Composable
fun RegionListItem(
    region: RegionPresentation,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (selected) PdigV2Colors.Primary.copy(alpha = 0.18f) else PdigV2Colors.Surface
    val borderColor = if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.BorderSubtle
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = containerColor,
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            Modifier.padding(horizontal = VSpacing.Lg, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(if (region.attentionCount > 0) PdigV2Colors.Warning else PdigV2Colors.PrimaryBright, CircleShape),
            )
            Spacer(Modifier.width(VSpacing.Lg))
            Column(Modifier.weight(1f)) {
                Text(region.displayName, style = MaterialTheme.typography.bodyMedium, color = PdigV2Colors.TextPrimary)
                Text(
                    "${region.cardCount} 张卡 · ${region.phoneCount} 个号码 · ${region.serviceCount} 项服务",
                    style = MaterialTheme.typography.bodySmall,
                    color = PdigV2Colors.TextSecondary,
                )
            }
            if (region.attentionCount > 0) {
                Surface(color = PdigV2Colors.Warning.copy(alpha = 0.18f), shape = RoundedCornerShape(VRadius.Sm)) {
                    Text(
                        "${region.attentionCount}",
                        Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = PdigV2Colors.Warning,
                    )
                }
            }
        }
    }
}

/** 隐私遮蔽 chip：全局 Privacy Mask 开关提示。 */
@Composable
fun MaskEnabledIndicator(enabled: Boolean, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = PdigV2Colors.TextMuted, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            if (enabled) "隐私遮蔽已开启" else "隐私遮蔽已关闭",
            style = MaterialTheme.typography.labelSmall,
            color = PdigV2Colors.TextSecondary,
        )
    }
}

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
                color = PdigV2Colors.Primary,
                shape = RoundedCornerShape(VRadius.Md),
            ) {
                Text(
                    primaryCta,
                    Modifier.padding(horizontal = VSpacing.Lg, vertical = VSpacing.Sm),
                    color = PdigV2Colors.CanvasDeep,
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
