package com.pdig.desktop.ui

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.PdigCard
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.SectionDivider

/** 候选对象：从账单中发现、尚未成为节点的对象。 */
@Composable
fun CandidatesScreen(ui: UiState) {
    val candidates = ui.session.candidates.pendingCandidates()
    PdigPage(
        title = "候选对象",
        subtitle = "发现 ≠ 确认：接受后才会创建节点",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        Column {
            if (candidates.isEmpty()) {
                EmptyState("没有待处理的候选对象。")
            } else {
                SectionDivider("待处理（${candidates.size}）")
                candidates.forEach { c ->
                    PdigCard(
                        title = c.label,
                        subtitle = "类型：${kindLabel(c.candidateKind)} · 观察 ${c.observationCount} 次",
                        trailing = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { acceptCandidate(ui, c.id) }) { Text("接受为节点") }
                                TextButton(onClick = { dismissCandidate(ui, c.id) }) { Text("忽略") }
                            }
                        },
                    )
                }
            }
        }
    }
}

private fun acceptCandidate(ui: UiState, id: String) {
    try {
        val nodeId = ui.session.candidates.acceptCandidate(id)
        ui.notice = "已创建节点 $nodeId"
        ui.refresh()
    } catch (t: Throwable) {
        ui.showError(t)
    }
}

private fun dismissCandidate(ui: UiState, id: String) {
    try {
        ui.session.candidates.dismissCandidate(id)
        ui.refresh()
    } catch (t: Throwable) {
        ui.showError(t)
    }
}