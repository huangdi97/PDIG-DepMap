package com.pdig.desktop.ui

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.pdig.core.domain.ActionVerificationStatus
import com.pdig.core.domain.PlanAction
import com.pdig.desktop.ui.components.ChipTone
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.InfoRow
import com.pdig.desktop.ui.components.PdigCard
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.SectionDivider
import com.pdig.desktop.ui.components.StatusChip

/** 变更计划详情：信息 + 动作清单（完成 / 验证）。 */
@Composable
fun PlanScreen(ui: UiState) {
    val planId = ui.selectedPlanId
    val detail = planId?.let { ui.session.plans.planDetail(it) }
    PdigPage(
        title = "变更计划",
        subtitle = planId ?: "未选择计划",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        if (detail == null) {
            EmptyState("未找到计划 ${planId ?: ""}。请先在场景中创建。")
            return@PdigPage
        }
        val pid = planId ?: return@PdigPage
        Column {
            SectionDivider("计划信息")
            InfoRow("标题", detail.title)
            InfoRow("场景", detail.scenario)
            InfoRow("工作流状态", detail.workflowState.wire)
            detail.effectiveState?.let { s -> InfoRow("派生生效状态", s.wire) }
            InfoRow("基线图版本", detail.baselineGraphRevision.toString())
            InfoRow("最近分析版本", detail.lastAnalyzedGraphRevision.toString())
            InfoRow("当前图版本", detail.currentGraphRevision.toString())
            InfoRow("目标节点", detail.targetNodeName)
            InfoRow("生效日期", detail.effectiveDate ?: "未设置")
            InfoRow("就绪度", detail.readiness.wire)
            InfoRow("受影响服务数", detail.affectedServiceCount.toString())
            InfoRow("必须处理键", detail.mustChangeKeys.size.toString())
            if (detail.unresolvedMustChangeKeys.isNotEmpty()) {
                InfoRow("仍未解决", detail.unresolvedMustChangeKeys.joinToString("；"))
            }
            SectionDivider("动作（${detail.actions.size}）")
            if (detail.actions.isEmpty()) {
                EmptyState("该计划没有动作。")
            } else {
                detail.actions.forEach { a -> PlanActionCard(ui, pid, a) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { ui.screen = Screen.ACTIONS }) { Text("行动计划") }
                TextButton(onClick = { ui.screen = Screen.VERIFICATION }) { Text("验证") }
            }
        }
    }
}

/** 单个计划动作卡片：阶段 / done / verification + 完成 / 验证按钮。 */
@Composable
internal fun PlanActionCard(ui: UiState, planId: String, action: PlanAction) {
    val v = action.verification
    PdigCard(
        title = action.title,
        subtitle = listOfNotNull(
            "动作 ID: ${action.id}",
            "阶段：${action.phase.wire}",
            v?.let { "验证：${it.method.wire} / ${it.status.wire}" },
        ).joinToString(" · "),
        trailing = {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                StatusChip(if (action.done) "done" else "todo", if (action.done) ChipTone.GOOD else ChipTone.NEUTRAL)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { completePlanAction(ui, planId, action.id) }, enabled = !action.done) { Text("标记完成") }
                    val canVerify = canVerifyAction(action)
                    TextButton(onClick = { verifyPlanAction(ui, planId, action.id) }, enabled = canVerify) { Text("验证") }
                }
            }
        },
    )
}

internal fun canVerifyAction(action: PlanAction): Boolean {
    val v = action.verification ?: return false
    return action.done && (v.status == ActionVerificationStatus.PENDING ||
        v.status == ActionVerificationStatus.EVIDENCE_SUGGESTED)
}

internal fun completePlanAction(ui: UiState, planId: String, actionId: String) {
    try {
        ui.session.plans.completeAction(planId, actionId)
        ui.refresh()
    } catch (t: Throwable) {
        ui.showError(t)
    }
}

internal fun verifyPlanAction(ui: UiState, planId: String, actionId: String) {
    try {
        ui.session.plans.verifyAction(planId, actionId)
        ui.refresh()
    } catch (t: Throwable) {
        ui.showError(t)
    }
}