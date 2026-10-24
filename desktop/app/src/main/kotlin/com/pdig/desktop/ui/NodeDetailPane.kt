package com.pdig.desktop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pdig.app.data.DependencyRow
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.InfoRow
import com.pdig.desktop.ui.components.PdigRow
import com.pdig.desktop.ui.components.SectionHeader
import com.pdig.desktop.ui.theme.PdigDesktopTokens as T
import com.pdig.desktop.ui.theme.PdigType

/**
 * 基础设施右侧详情面板：
 * - nodeId 非空 → 对象详情（身份/能力/依赖/证据/恢复路径/相关场景）
 * - capability 非空 → 能力概览（属于该能力的对象清单）
 * 只读；修改前往对象详情。
 */
@Composable
internal fun NodeDetailPane(ui: UiState, nodeId: String?) {
    val node = nodeId?.let { id -> ui.session.graph.nodes(includeArchived = true).firstOrNull { it.id == id } }
    val capability = nodeId?.takeIf { node == null }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = T.SpaceXxl, vertical = T.SpaceLg),
    ) {
        when {
            node != null -> NodeDetailContent(ui, node.id, node.name)
            capability != null -> CapabilityDetailContent(ui, capability)
            else -> EmptyState("在左侧选择一个对象，查看详情与相关依赖。")
        }
    }
}

@Composable
private fun NodeDetailContent(ui: UiState, nodeId: String, nodeName: String) {
    val node = ui.session.graph.nodes(includeArchived = true).firstOrNull { it.id == nodeId }
    if (node == null) return
    SectionHeader("对象信息")
    InfoRow("名称", node.name)
    InfoRow("类型", kindLabel(node.kind))
    if (node.kind == "payment_instrument") {
        SectionHeader("操作")
        Surface(
            Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(T.RadiusMd),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.padding(T.SpaceLg)) {
                Text("如果这张卡停用，会影响到什么", style = PdigType.Body, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text("打开对象详情，查看影响分析与变更计划", style = PdigType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text(
                    "查看影响",
                    Modifier.clickable {
                        ui.selectedNodeId = node.id
                        ui.screen = Screen.NODE
                    },
                    style = PdigType.Label,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(T.SpaceSm))
    }
    SectionHeader("相关依赖")
    val deps = ui.session.graph.dependencies().filter { it.from == node.id || it.to == node.id }
    if (deps.isEmpty()) {
        EmptyState("该对象没有任何依赖关系。")
    } else {
        deps.forEach { d ->
            PdigRow(
                title = "${d.fromName} → ${d.toName}",
                subtitle = dependencySummary(d),
                onClick = {
                    val counter = if (d.from == node.id) d.to else d.from
                    if (counter.isNotBlank()) {
                        ui.selectedNodeId = counter
                        ui.screen = Screen.NODE
                    }
                },
                trailing = {
                    Icon(Icons.Filled.ArrowForward, contentDescription = "前往", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                },
            )
        }
    }
}

@Composable
private fun CapabilityDetailContent(ui: UiState, capability: String) {
    val nodes = ui.session.graph.nodes()
    val label = capabilityLabelOf(capability)
    val members = nodes.filter { capabilityOf(it.kind) == capability }
    SectionHeader(label)
    Text(
        "属于「$label」的对象",
        style = PdigType.Secondary,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(T.SpaceSm))
    if (members.isEmpty()) {
        EmptyState("还没有对象属于这个能力。")
    } else {
        members.forEach { n ->
            PdigRow(
                title = n.name,
                subtitle = kindLabel(n.kind),
                leading = {
                    Icon(capabilityIcon(capability), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(T.IconSize))
                },
                onClick = {
                    ui.selectedNodeId = n.id
                    ui.screen = Screen.NODE
                },
                trailing = {
                    Icon(Icons.Filled.ArrowForward, contentDescription = "前往", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                },
            )
        }
    }
}

internal fun capabilityLabelOf(key: String): String = when (key) {
    "identity" -> "身份与恢复"
    "access" -> "访问与认证"
    "payment" -> "支付"
    "device" -> "设备"
    "service" -> "关键服务"
    else -> "未分类"
}

private fun capabilityIcon(key: String): ImageVector = when (key) {
    "identity" -> Icons.Filled.PhoneAndroid
    "access" -> Icons.Filled.Email
    "payment" -> Icons.Filled.AccountBox
    "device" -> Icons.Filled.PhoneAndroid
    "service" -> Icons.Filled.AccountBox
    else -> Icons.Filled.AccountBox
}

/** 依赖行的人话摘要：关系 · 能力 · 重要程度 ·（非有效状态）。 */
internal fun dependencySummary(d: DependencyRow): String = listOfNotNull(
    "关系：${relationLabel(d.relation)}",
    "能力：${capabilityLabel(d.capability)}",
    criticalityLabel(d.criticality),
    if (d.state != "active") dependencyStateLabel(d.state) else null,
).joinToString(" · ")