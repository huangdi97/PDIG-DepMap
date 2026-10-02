package com.pdig.uivnext.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.pdig.uivnext.model.AttentionItem
import com.pdig.uivnext.model.RegionPresentation
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VType
import com.pdig.uivnext.theme.statusColor
import com.pdig.uivnext.theme.statusLabelZh

/** StatusBadge：icon + label + color 三通道（状态永不只靠颜色）。 */
@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val color = statusColor(status)
    val icon: ImageVector = when (status) {
        "critical", "blocked", "expiring_soon" -> Icons.Filled.Warning
        "verifying", "waiting", "not_started", "manual" -> Icons.Filled.HourglassEmpty
        "completed", "active", "ok", "migrated" -> Icons.Filled.CheckCircle
        else -> Icons.Filled.Error
    }
    Surface(
        modifier = modifier,
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Sm),
        border = androidx.compose.foundation.BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Row(
            Modifier.padding(horizontal = VSpacing.Sm, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Text(
                statusLabelZh(status),
                style = VType.StatusLabel,
                color = color,
            )
        }
    }
}

/** SectionHeader：小节标题（20sp）+ hairline。 */
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
                style = VType.SectionTitle,
                color = PdigV2Colors.TextPrimary,
            )
            trailing?.invoke()
        }
        Spacer(Modifier.height(6.dp))
        androidx.compose.material3.HorizontalDivider(color = PdigV2Colors.BorderSubtle, thickness = 1.dp)
    }
}

/** AttentionRow：icon + label + color；critical 最突出；点击去向 target。 */
@Composable
fun AttentionRow(item: AttentionItem, onClick: (AttentionItem) -> Unit, modifier: Modifier = Modifier) {
    val color = statusColor(item.severity)
    val icon = if (item.severity == "critical") Icons.Filled.Error else Icons.Filled.Warning
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(item) },
        color = if (item.severity == "critical") PdigV2Colors.Critical.copy(alpha = 0.12f) else PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Md),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (item.severity == "critical") PdigV2Colors.Critical.copy(alpha = 0.4f) else PdigV2Colors.BorderSubtle),
    ) {
        Row(
            Modifier.padding(horizontal = VSpacing.Lg, vertical = VSpacing.Lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(VSpacing.Lg))
            Text(
                item.title,
                style = VType.Body,
                color = PdigV2Colors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = PdigV2Colors.TextMuted, modifier = Modifier.size(18.dp))
        }
    }
}


/** RegionListItem（PHASE 1C）：更大地区身份（flag/region glyph 主、计数次级、lighter chrome）。 */
@Composable
fun RegionListItem(
    region: RegionPresentation,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (selected) PdigV2Colors.PrimaryBright.copy(alpha = 0.10f) else Color.Transparent
    val borderColor = if (selected) PdigV2Colors.PrimaryBright.copy(alpha = 0.35f) else PdigV2Colors.BorderSubtle.copy(alpha = 0.4f)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = containerColor,
        shape = RoundedCornerShape(VRadius.Md),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
    ) {
        Row(
            Modifier.padding(horizontal = VSpacing.Lg, vertical = VSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RegionBadge(region.regionCode, badgeSize = 44.dp)
            Spacer(Modifier.width(VSpacing.Lg))
            Column(Modifier.weight(1f)) {
                Text(region.displayName, style = VType.Body, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = PdigV2Colors.TextPrimary, maxLines = 1)
                Text(
                    "${region.cardCount} 张卡 · ${region.phoneCount} 个号码 · ${region.serviceCount} 项服务",
                    style = VType.Meta,
                    color = PdigV2Colors.TextMuted,
                    maxLines = 1,
                )
            }
            if (region.attentionCount > 0) {
                Surface(color = PdigV2Colors.Warning.copy(alpha = 0.16f), shape = RoundedCornerShape(VRadius.Sm)) {
                    Text(
                        "${region.attentionCount}",
                        Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = VType.Label,
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
        Icon(Icons.Filled.Lock, contentDescription = null, tint = PdigV2Colors.TextMuted, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            if (enabled) "隐私遮蔽已开启" else "隐私遮蔽已关闭",
            style = VType.Secondary,
            color = PdigV2Colors.TextSecondary,
        )
    }
}
