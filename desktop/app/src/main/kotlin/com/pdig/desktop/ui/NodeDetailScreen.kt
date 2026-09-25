package com.pdig.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pdig.desktop.ui.components.ChipTone
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.InfoRow
import com.pdig.desktop.ui.components.PdigCard
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.SectionDivider
import com.pdig.desktop.ui.components.StatusChip

/** 节点详情：字段 + 相关依赖的 criticality（required/unknown）编辑 + 影响分析入口。 */
@Composable
fun NodeDetailScreen(ui: UiState) {
    val nodeId = ui.selectedNodeId
    val node = nodeId?.let { id -> ui.session.graph.nodes(includeArchived = true).firstOrNull { it.id == id } }
    PdigPage(
        title = "节点详情",
        subtitle = nodeId ?: "未选择节点",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        if (node == null) {
            EmptyState("未找到节点 ${nodeId ?: ""}。")
            return@PdigPage
        }
        Column {
            SectionDivider("节点信息")
            InfoRow("名称", node.name)
            InfoRow("名称", node.name)
            InfoRow("类型", kindLabel(node.kind))
            InfoRow("归档", if (node.archived) "是" else "否")
            SectionDivider("相关依赖")
            val deps = ui.session.graph.dependencies().filter { it.from == node.id || it.to == node.id }
            if (deps.isEmpty()) {
                EmptyState("该节点没有任何依赖关系。")
            } else {
                deps.forEach { d ->
                    val isRequired = d.criticality == "required"
                    PdigCard(
                        title = "${d.fromName} → ${d.toName}",
                        subtitle = "能力：${capabilityLabel(d.capability)} · ${dependencyStateLabel(d.state)}",
                        trailing = {
                            Column(horizontalAlignment = Alignment.End) {
                                StatusChip(if (isRequired) "必需" else "未知", if (isRequired) ChipTone.GOOD else ChipTone.WARN)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { setCriticality(ui, d.id, true) }, enabled = !isRequired) { Text("设为必需") }
                                    TextButton(onClick = { setCriticality(ui, d.id, false) }, enabled = isRequired) { Text("设为未知") }
                                }
                            }
                        },
                    )
                }
            }
            SectionDivider("操作")
            Button(onClick = {
                ui.selectedNodeId = node.id
                ui.screen = Screen.IMPACT
            }) { Text("影响分析") }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = { ui.screen = Screen.INFRA }) { Text("返回基础设施") }
            Text("「必需」表示：该依赖对支付能力是必需路径；机器永不自动产生「必需」，只能由您确认。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun setCriticality(ui: UiState, depId: String, required: Boolean) {
    try {
        ui.session.graph.setDependencyCriticality(depId, required)
        ui.refresh()
    } catch (t: Throwable) {
        ui.showError(t)
    }
}