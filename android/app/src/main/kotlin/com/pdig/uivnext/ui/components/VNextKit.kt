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
            .defaultMinSize(minHeight = VTouchTarget.Min)
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
            .defaultMinSize(minHeight = VTouchTarget.Min)
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
