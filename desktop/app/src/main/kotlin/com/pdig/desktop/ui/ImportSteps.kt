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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.InfoRow
import com.pdig.desktop.ui.components.PdigCard
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.SectionDivider

/** 导入 Stage2：提交前的预览（不写任何库）。 */
@Composable
internal fun ImportPreviewStep(ui: UiState) {
    PdigPage(
        title = "导入预览",
        subtitle = "数据来源：${ui.preview?.sourceLabel ?: "未知"}",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        val preview = ui.preview
        if (preview == null) {
            EmptyState("没有可预览的数据。请返回重新选择文件。")
            return@PdigPage
        }
        Column {
            SectionDivider("检测到的支付工具（${preview.instruments.size}）")
            if (preview.instruments.isEmpty()) {
                EmptyState("本次文件里没有识别到支付工具（可能被标记为解析错误，见下方明细）。")
            } else {
                preview.instruments.forEach { p ->
                    PdigCard(title = p.label, subtitle = "类型：${kindLabel(p.kind.wire)}")
                }
            }
            SectionDivider("检测到的商户 / 服务（${preview.counterparties.size}）")
            if (preview.counterparties.isEmpty()) {
                EmptyState("本次文件里没有识别到商户 / 服务（可能被标记为解析错误，见下方明细）。")
            } else {
                preview.counterparties.forEach { p ->
                    PdigCard(title = p.label, subtitle = "类型：${kindLabel(p.kind.wire)}")
                }
            }
            SectionDivider("概览")
            InfoRow("观察记录数", preview.observations.size.toString())
            InfoRow("预计 Proposal 数", preview.projectedProposalCount.toString())
            InfoRow("解析错误", preview.errors.size.toString())
            if (preview.errors.isNotEmpty()) {
                SectionDivider("解析错误明细（${preview.errors.size}）")
                preview.errors.forEach { err ->
                    Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { ui.importStage = 0 }) { Text("返回") }
                Button(onClick = { commitImport(ui) }) { Text("确认提交") }
            }
        }
    }
}

/** 导入 Stage3：提交结果展示。 */
@Composable
internal fun ImportDoneStep(ui: UiState) {
    PdigPage(
        title = "导入完成",
        subtitle = "已提交到当前会话",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        val result = ui.lastCommit
        if (result == null) {
            EmptyState("没有提交结果。")
            return@PdigPage
        }
        Column {
            SectionDivider("导入结果")
            InfoRow("来源实例", result.sourceInstanceId)
            InfoRow("导入会话", result.importSessionId)
            InfoRow("原始记录数", result.rawCount.toString())
            InfoRow("新增唯一记录", result.newUniqueCount.toString())
            InfoRow("重复记录", result.duplicateCount.toString())
            InfoRow("新建节点", result.nodeCount.toString())
            InfoRow("生成的 Proposal", result.proposalCount.toString())
            InfoRow("解析错误", result.errorCount.toString())
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                ui.importStage = 0
                ui.preview = null
                ui.lastCommit = null
                ui.refresh()
            }) { Text("完成") }
        }
    }
}

internal fun commitImport(ui: UiState) {
    val preview = ui.preview
    if (preview == null) return
    try {
        ui.lastCommit = ui.session.sources.commitImport(preview)
        ui.importStage = 3
        ui.refresh()
    } catch (t: Throwable) {
        ui.showError(t)
    }
}