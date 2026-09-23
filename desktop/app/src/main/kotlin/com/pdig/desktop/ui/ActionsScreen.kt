package com.pdig.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.InfoRow
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.SectionDivider

/** 行动计划：当前计划全部动作的完成 / 验证操作。 */
@Composable
fun ActionsScreen(ui: UiState) {
    val planId = ui.selectedPlanId
    val detail = planId?.let { ui.session.plans.planDetail(it) }
    PdigPage(
        title = "行动计划",
        subtitle = planId ?: "未选择计划",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        if (planId == null) {
            EmptyState("未选择变更计划。请先前往「场景中心」创建计划。")
            return@PdigPage
        }
        if (detail == null) {
            EmptyState("未找到计划 $planId。")
            return@PdigPage
        }
        Column {
            InfoRow("计划", detail.title)
            InfoRow("工作流状态", detail.workflowState.wire)
            SectionDivider("动作（${detail.actions.size}）")
            if (detail.actions.isEmpty()) {
                EmptyState("该计划没有动作。")
            } else {
                detail.actions.forEach { a -> PlanActionCard(ui, planId, a) }
            }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = { ui.screen = Screen.PLAN }) { Text("返回计划") }
        }
    }
}