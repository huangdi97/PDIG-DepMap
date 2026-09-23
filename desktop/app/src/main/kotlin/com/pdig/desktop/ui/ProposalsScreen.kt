package com.pdig.desktop.ui

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.PdigCard
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.SectionDivider

/** Proposal 确认：机器推断的依赖建议，用户决定是否进入 Reality。 */
@Composable
fun ProposalsScreen(ui: UiState) {
    val proposals = ui.session.proposals.pendingProposals()
    PdigPage(
        title = "Proposal 确认",
        subtitle = "机器推断 ≠ 现实：只有您确认才会写入依赖图",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        Column {
            if (proposals.isEmpty()) {
                EmptyState("没有待确认的 Proposal。")
            } else {
                SectionDivider("待确认（${proposals.size}）")
                proposals.forEach { p ->
                    PdigCard(
                        title = "${p.from} → ${p.to}",
                        subtitle = "关系：${p.relation} · 能力：${p.capability} · 观察 ${p.observationCount} 次" +
                            " · 置信度 ${(p.confidence * 100).toInt()}%",
                        trailing = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { acceptProposal(ui, p.id) }) { Text("接受") }
                                TextButton(onClick = { rejectProposal(ui, p.id) }) { Text("拒绝") }
                            }
                        },
                    )
                }
            }
        }
    }
}

private fun acceptProposal(ui: UiState, id: String) {
    try {
        ui.session.proposals.acceptProposal(id)
        ui.notice = "已接受，边已进入 Reality。注意：新增边的 criticality 仍为 unknown，" +
            "不会自动升级为 required；如确属必需，请在节点详情的依赖中手动设为必需。"
        ui.refresh()
    } catch (t: Throwable) {
        ui.showError(t)
    }
}

private fun rejectProposal(ui: UiState, id: String) {
    try {
        ui.session.proposals.rejectProposal(id)
        ui.refresh()
    } catch (t: Throwable) {
        ui.showError(t)
    }
}