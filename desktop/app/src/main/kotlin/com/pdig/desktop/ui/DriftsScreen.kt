package com.pdig.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.PdigCard
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.SectionHeader

/** 现实变化（Drift）：观察到的支付路径可能已变化，需用户选择如何修正 Reality。 */
@Composable
fun DriftsScreen(ui: UiState) {
    val drifts = ui.session.drifts.openDrifts()
    PdigPage(
        title = "现实变化",
        subtitle = "观察 ≠ 事实：由您决定现实该如何修正",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        Column(Modifier.fillMaxWidth()) {
            if (drifts.isEmpty()) {
                EmptyState("没有待处理的现实变化。")
            } else {
                SectionHeader("待处理（${drifts.size}）")
                drifts.forEach { d ->
                    val targetName = ui.session.graph.nodes().firstOrNull { it.id == d.targetNodeId }?.name ?: "（未知对象）"
                    PdigCard(
                        title = "${d.candidateFrom ?: "（未知来源）"} → $targetName",
                        subtitle = "类型：${driftKindLabel(d.kind)} · 能力：${capabilityLabel(d.capability)} · 观察 ${d.observationCount} 次" +
                            " · 关联依赖 ${d.relatedDependencyIds.size} 条 · 发现于 ${d.detectedAt.take(19)}",
                        trailing = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { resolve(ui, d.id, replacement = true) }) { Text("已替换") }
                                TextButton(onClick = { resolve(ui, d.id, replacement = false) }) { Text("另有备用路径") }
                                TextButton(onClick = { dismissDrift(ui, d.id) }) { Text("无变化") }
                            }
                        },
                    )
                }
            }
        }
    }
}

private fun resolve(ui: UiState, id: String, replacement: Boolean) {
    try {
        if (replacement) ui.session.drifts.resolveDriftAsReplacement(id)
        else ui.session.drifts.resolveDriftAsAdditionalPath(id)
        ui.refresh()
    } catch (t: Throwable) {
        ui.showError(t)
    }
}

private fun dismissDrift(ui: UiState, id: String) {
    try {
        ui.session.drifts.dismissDrift(id)
        ui.refresh()
    } catch (t: Throwable) {
        ui.showError(t)
    }
}