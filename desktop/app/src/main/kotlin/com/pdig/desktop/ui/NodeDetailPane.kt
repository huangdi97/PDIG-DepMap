package com.pdig.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pdig.app.data.DependencyRow
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.InfoRow
import com.pdig.desktop.ui.components.PdigCard
import com.pdig.desktop.ui.components.SectionDivider

/** 基础设施右侧详情面板：对象信息 + 相关依赖（只读，点击跳全页）。 */
@Composable
internal fun NodeDetailPane(ui: UiState, nodeId: String?) {
    val node = nodeId?.let { id -> ui.session.graph.nodes(includeArchived = true).firstOrNull { it.id == id } }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        if (node == null) {
            EmptyState("在左侧选择一个对象，查看详情与相关依赖。")
            return@Column
        }
        SectionDivider("对象信息")
        InfoRow("名称", node.name)
        InfoRow("类型", kindLabel(node.kind))
        if (node.kind == "payment_instrument") {
            SectionDivider("操作")
            PdigCard(
                title = "如果这张卡停用，会影响到什么",
                subtitle = "打开对象详情，查看影响分析与变更计划",
                onClick = {
                    ui.selectedNodeId = node.id
                    ui.screen = Screen.NODE
                },
            )
        }
        SectionDivider("相关依赖")
        val deps = ui.session.graph.dependencies().filter { it.from == node.id || it.to == node.id }
        if (deps.isEmpty()) {
            EmptyState("该对象没有任何依赖关系。")
        } else {
            deps.forEach { d ->
                PdigCard(
                    title = "${d.fromName} → ${d.toName}",
                    subtitle = dependencySummary(d),
                    onClick = {
                        val counter = if (d.from == node.id) d.to else d.from
                        if (counter.isNotBlank()) {
                            ui.selectedNodeId = counter
                            ui.screen = Screen.NODE
                        }
                    },
                )
            }
        }
    }
}

/** 依赖行的人话摘要：关系 · 能力 · 重要程度 ·（非有效状态）。 */
internal fun dependencySummary(d: DependencyRow): String = listOfNotNull(
    "关系：${relationLabel(d.relation)}",
    "能力：${capabilityLabel(d.capability)}",
    criticalityLabel(d.criticality),
    if (d.state != "active") dependencyStateLabel(d.state) else null,
).joinToString(" · ")
