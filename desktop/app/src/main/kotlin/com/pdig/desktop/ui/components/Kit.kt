package com.pdig.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** 屏面骨架：标题 + 可滚动内容 + 可选顶部提示。 */
@Composable
fun PdigPage(
    title: String,
    subtitle: String? = null,
    notice: String? = null,
    error: String? = null,
    onDismissNotice: (() -> Unit)? = null,
    onDismissError: (() -> Unit)? = null,
    scrollable: Boolean = true,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (notice != null) NoticeStrip(notice, onDismissNotice)
        if (error != null) ErrorStrip(error, onDismissError)
        Spacer(Modifier.height(12.dp))
        val scrollModifier = if (scrollable) {
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        } else {
            Modifier.fillMaxSize()
        }
        Box(scrollModifier) { content() }
    }
}

@Composable
fun NoticeStrip(text: String, onDismiss: (() -> Unit)? = null) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.bodySmall)
            if (onDismiss != null) {
                Spacer(Modifier.width(8.dp))
                Text("关闭", Modifier.clickable { onDismiss() }, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
fun ErrorStrip(text: String, onDismiss: (() -> Unit)? = null) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            if (onDismiss != null) {
                Spacer(Modifier.width(8.dp))
                Text("知道了", Modifier.clickable { onDismiss() }, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
fun PdigCard(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            trailing?.invoke()
        }
    }
    Spacer(Modifier.height(8.dp))
}

/** 状态小徽标：required/active/pending/verified 等语义色。 */
@Composable
fun StatusChip(label: String, tone: ChipTone = ChipTone.NEUTRAL) {
    val color = when (tone) {
        ChipTone.GOOD -> MaterialTheme.colorScheme.tertiaryContainer
        ChipTone.WARN -> MaterialTheme.colorScheme.secondaryContainer
        ChipTone.BAD -> MaterialTheme.colorScheme.errorContainer
        ChipTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(color = color, shape = RoundedCornerShape(6.dp)) {
        Text(label, Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall)
    }
}

enum class ChipTone { GOOD, WARN, BAD, NEUTRAL }

@Composable
fun EmptyState(message: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp)) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SectionDivider(text: String) {
    HorizontalDivider(Modifier.padding(vertical = 10.dp))
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
}

@Composable
fun ActionRow(
    title: String,
    description: String,
    onAction: () -> Unit,
    actionLabel: String,
    enabled: Boolean = true,
    tone: ChipTone = ChipTone.NEUTRAL,
) {
    PdigCard(title = title, subtitle = description) {
        androidx.compose.material3.TextButton(onClick = onAction, enabled = enabled) {
            Text(actionLabel)
        }
    }
}