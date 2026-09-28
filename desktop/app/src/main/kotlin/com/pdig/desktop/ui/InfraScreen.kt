package com.pdig.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pdig.app.data.NodeRow
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.theme.PdigDesktopTokens as T
import com.pdig.desktop.ui.theme.PdigType

/**
 * 基础设施：左列（By Item / By Capability 分段切换）+ 右侧详情。
 * By Item：手机号/邮箱/账户/设备/卡/服务；By Capability：身份与恢复/访问与认证/支付/设备/关键服务。
 * 切换用清晰 segmented UI；不暴露 enum 名。
 */
@Composable
fun InfraScreen(ui: UiState) {
    val nodes = ui.session.graph.nodes()
    var view by remember { mutableStateOf(InfraView.BY_ITEM) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var selectedCapability by remember { mutableStateOf<String?>(null) }

    PdigPage(
        title = "基础设施",
        subtitle = "你的数字基础设施：对象与能力（只读；修改请前往对象详情）",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
        scrollable = false,
    ) {
        if (nodes.isEmpty()) {
            EmptyState(
                message = "还没有节点。先前往「数据来源 → 导入」添加账单，或在待确认服务中确认节点。",
                title = "这里还没有内容",
                next = "前往「数据来源 → 导入」开始",
            )
        } else {
            Row(Modifier.fillMaxSize()) {
                MasterPane(
                    nodes = nodes,
                    view = view,
                    selectedId = selectedId,
                    selectedCapability = selectedCapability,
                    onViewChange = { view = it },
                    onSelectId = { selectedId = it; selectedCapability = null },
                    onSelectCapability = { selectedCapability = it; selectedId = null },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                NodeDetailPane(ui, selectedId ?: selectedCapability)
            }
        }
    }
}

enum class InfraView(val label: String) {
    BY_ITEM("按对象"),
    BY_CAPABILITY("按能力"),
}

@Composable
private fun MasterPane(
    nodes: List<NodeRow>,
    view: InfraView,
    selectedId: String?,
    selectedCapability: String?,
    onViewChange: (InfraView) -> Unit,
    onSelectId: (String) -> Unit,
    onSelectCapability: (String) -> Unit,
) {
    Column(
        Modifier
            .width(T.InfraMasterWidth)
            .fillMaxHeight(),
    ) {
        // 分段切换（By Item / By Capability）
        Row(
            Modifier.fillMaxWidth().padding(T.SpaceMd),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            InfraView.entries.forEach { v ->
                val selected = view == v
                Surface(
                    Modifier.weight(1f).clickable { onViewChange(v) },
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(T.RadiusSm),
                ) {
                    Text(
                        v.label,
                        Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = PdigType.Label,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

        val scroll = rememberScrollState()
        if (view == InfraView.BY_ITEM) {
            Column(Modifier.weight(1f).verticalScroll(scroll)) {
                nodes.forEach { n ->
                    MasterRow(
                        title = n.name,
                        subtitle = kindLabel(n.kind),
                        icon = nodeIcon(n.kind),
                        selected = selectedId == n.id,
                        onClick = { onSelectId(n.id) },
                    )
                }
            }
        } else {
            Column(Modifier.weight(1f).verticalScroll(scroll)) {
                CAPABILITY_GROUPS.forEach { (key, label, icon) ->
                    val count = nodes.count { capabilityOf(it.kind) == key }
                    MasterRow(
                        title = label,
                        subtitle = if (count > 0) "$count 个对象" else "还没有对象",
                        icon = icon,
                        selected = selectedCapability == key,
                        onClick = { onSelectCapability(key) },
                    )
                }
            }
        }
    }
}

/** 能力分组（用户语义，非 enum；对应 spec §29）。 */
private val CAPABILITY_GROUPS: List<Triple<String, String, ImageVector>> = listOf(
    Triple("identity", "身份与恢复", Icons.Filled.PhoneAndroid),
    Triple("access", "访问与认证", Icons.Filled.Email),
    Triple("payment", "支付", Icons.Filled.Contactless),
    Triple("device", "设备", Icons.Filled.PhoneAndroid),
    Triple("service", "关键服务", Icons.Filled.AccountBox),
)

internal fun capabilityOf(kind: String): String = when (kind) {
    "identity_anchor" -> "identity"
    "payment_instrument" -> "payment"
    "account" -> "access"
    "device" -> "device"
    "service", "membership" -> "service"
    else -> "identity"
}

@Composable
private fun MasterRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .background(
                when {
                    selected -> MaterialTheme.colorScheme.primaryContainer
                    !enabled -> Color.Transparent
                    else -> Color.Transparent
                },
                RoundedCornerShape(4.dp),
            )
            .padding(horizontal = T.SpaceLg, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(T.IconSize),
        )
        Spacer(Modifier.width(T.SpaceMd))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = PdigType.Body,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Text(subtitle, style = PdigType.Meta, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

private fun nodeIcon(kind: String): ImageVector = when (kind) {
    "identity_anchor" -> Icons.Filled.PhoneAndroid
    "email" -> Icons.Filled.Email
    "payment_instrument" -> Icons.Filled.Contactless
    "account" -> Icons.Filled.AccountBox
    "device" -> Icons.Filled.PhoneAndroid
    else -> Icons.Filled.AccountBox
}