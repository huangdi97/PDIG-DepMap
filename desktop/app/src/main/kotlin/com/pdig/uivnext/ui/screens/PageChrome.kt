package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VType

/**
 * PageHeader：页面主标题（pageTitle 36 / 700）+ 副标题（secondary 14）。
 * 所有 vNext 屏幕统一使用，保证字阶与留白一致（G4）。
 */
@Composable
fun PageHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = VType.PageTitle, color = PdigV2Colors.TextPrimary)
            if (subtitle != null) {
                Text(subtitle, style = VType.Secondary, color = PdigV2Colors.TextSecondary)
            }
        }
        trailing?.invoke()
    }
}

/** 分区卡片外壳（L3 Solid Data Surface：实体、高可读，不 blur）。 */
@Composable
fun DataPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    androidx.compose.material3.Surface(
        modifier = modifier,
        color = PdigV2Colors.Surface.copy(alpha = 0.96f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(com.pdig.uivnext.theme.VRadius.Xl),
        border = androidx.compose.foundation.BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        content()
    }
}
