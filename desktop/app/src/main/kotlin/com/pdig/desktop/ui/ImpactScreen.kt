package com.pdig.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pdig.app.data.ScenarioPlanRequest
import com.pdig.desktop.ui.components.ChipTone
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.InfoRow
import com.pdig.desktop.ui.components.PdigCard
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.SectionDivider
import com.pdig.desktop.ui.components.StatusChip

/** 影响分析：展示 ImpactKernel 的确定性与不确定性结果。 */
@Composable
fun ImpactScreen(ui: UiState) {
    val target = ui.selectedNodeId
    PdigPage(
        title = "影响分析",
        subtitle = target ?: "未选择目标节点",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        if (target == null) {
            EmptyState("未选择目标节点。请从「基础设施」进入节点详情后再点击影响分析。")
            return@PdigPage
        }
        val impact = ui.session.graph.impactFor(target)
        Column {
            SectionDivider("概览")
            InfoRow("本次停用", impact.unavailable.size.toString() + " 项")
            InfoRow("受影响", impact.lostKeys.size.toString() + " 项")
            InfoRow("本次分析依据（已检查）", impact.processedKeys.size.toString() + " 项")
            SectionDivider("检查清单（${impact.checklist.size}）")
            if (impact.checklist.isEmpty()) {
                EmptyState("没有必须处理的事项（不代表一切安全）。")
            } else {
                impact.checklist.forEach { item ->
                    PdigCard(
                        title = item.title,
                        subtitle = item.detail,
                        trailing = { StatusChip(impactLevelLabel(item.level.wire), toneFor(item.level.wire)) },
                    )
                }
            }
            SectionDivider("操作")
            Button(onClick = { createPlanFromImpact(ui, target) }) { Text("生成变更计划") }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = { ui.screen = Screen.NODE }) { Text("返回节点详情") }
        }
    }
}

private fun toneFor(levelWire: String): ChipTone = when (levelWire) {
    "must_change" -> ChipTone.BAD
    "backup_path", "degraded", "needs_review" -> ChipTone.WARN
    else -> ChipTone.NEUTRAL
}

private fun createPlanFromImpact(ui: UiState, targetId: String) {
    try {
        val planId = ui.session.plans.createPlanForScenario(
            ScenarioPlanRequest(scenarioId = "replace_payment_card", targetNodeId = targetId),
        )
        ui.selectedPlanId = planId
        ui.screen = Screen.PLAN
    } catch (t: Throwable) {
        ui.showError(t)
    }
}