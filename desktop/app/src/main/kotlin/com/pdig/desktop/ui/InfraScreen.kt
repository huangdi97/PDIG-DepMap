package com.pdig.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.InfoRow
import com.pdig.desktop.ui.components.PdigCard
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.SectionDivider

/** 基础设施：节点与依赖只读浏览 + 图状态。图是模型，不是主界面。 */
@Composable
fun InfraScreen(ui: UiState) {
    val nodes = ui.session.graph.nodes()
    val deps = ui.session.graph.dependencies()
    val revision = ui.session.graph.graphRevision()
    val integrity = ui.session.graph.integrity()
    PdigPage(
        title = "基础设施",
        subtitle = "节点与依赖（Reality 只读；修改请前往节点详情）",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        Column {
            SectionDivider("图状态")
            InfoRow("graphRevision", revision.toString())
            InfoRow("节点数", nodes.size.toString())
            InfoRow("依赖边数", deps.size.toString())
            InfoRow("完整性孤立引用", integrity.orphanCount.toString())
            SectionDivider("节点（${nodes.size}）")
            if (nodes.isEmpty()) {
                EmptyState("还没有节点。先前往「导入」添加账单，或在候选对象中确认节点。")
            } else {
                nodes.forEach { n ->
                    PdigCard(
                        title = n.name,
                        subtitle = "ID: ${n.id} · ${n.kind}" + (if (n.archived) " · 已归档" else ""),
                        onClick = {
                            ui.selectedNodeId = n.id
                            ui.screen = Screen.NODE
                        },
                    )
                }
            }
            SectionDivider("依赖（${deps.size}）")
            if (deps.isEmpty()) {
                EmptyState("还没有依赖关系。接受 Proposal 或处理 Drift 后会出现。")
            } else {
                deps.forEach { d ->
                    PdigCard(
                        title = "${d.fromName} → ${d.toName}",
                        subtitle = "关系 ${d.relation} · 能力 ${d.capability} · criticality ${d.criticality} · ${d.state}",
                    )
                }
            }
        }
    }
}