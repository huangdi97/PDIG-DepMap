package com.pdig.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pdig.core.domain.PlanAction
import com.pdig.desktop.ui.components.ContinuityRail
import com.pdig.desktop.ui.components.ContinuityStep
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.InfoRow
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.RailAction
import com.pdig.desktop.ui.components.RailState
import com.pdig.desktop.ui.components.SectionHeader
import com.pdig.desktop.ui.theme.PdigType

/** 行动计划：当前计划全部动作的完成 / 验证操作（与 Plan 共用 Continuity Rail 语义）。 */
@Composable
fun ActionsScreen(ui: UiState) {
    val planId = ui.selectedPlanId
    val detail = planId?.let { ui.session.plans.planDetail(it) }
    PdigPage(
        title = "行动计划",
        subtitle = detail?.title ?: "未选择计划",
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
            EmptyState("未找到计划。")
            return@PdigPage
        }
        Column(Modifier.fillMaxWidth()) {
            InfoRow("计划", detail.title)
            InfoRow("工作流状态", workflowStateLabel(detail.workflowState.wire))
            SectionHeader("动作（${detail.actions.size}）")
            if (detail.actions.isEmpty()) {
                EmptyState("该计划没有动作。")
            } else {
                val steps = detail.actions.map { a -> actionToStep(a, ui, planId, detail.actions) }
                ContinuityRail(
                    steps = steps,
                    blockedReason = if (steps.any { it.state == RailState.BLOCKED }) {
                        "新路径验证通过后才能执行这一步。"
                    } else null,
                )
            }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = { ui.screen = Screen.PLAN }) { Text("返回计划") }
        }
    }
}

private fun actionToStep(
    a: PlanAction,
    ui: UiState,
    planId: String,
    allActions: List<PlanAction>,
): ContinuityStep {
    val v = a.verification
    val state = when {
        v != null && v.status == com.pdig.core.domain.ActionVerificationStatus.VERIFIED -> RailState.VERIFIED
        a.done -> RailState.SUCCESS
        a.prerequisiteActionIds.isEmpty() -> RailState.ACTIVE
        else -> RailState.BLOCKED
    }
    return ContinuityStep(
        label = a.title,
        state = state,
        description = "阶段：${actionPhaseLabel(a.phase.wire)}",
        stateLabel = when (state) {
            RailState.VERIFIED -> "已验证"
            RailState.SUCCESS -> "已完成"
            RailState.BLOCKED -> "等待前置完成"
            else -> null
        },
        content = {
            val canComplete = a.prerequisiteActionIds.all { p ->
                allActions.firstOrNull { it.id == p }?.done == true
            } && !a.done
            val canVerify = canVerifyAction(a)
            Row {
                RailAction("标记完成", enabled = canComplete, onClick = { completePlanAction(ui, planId, a.id) })
                if (v != null) {
                    RailAction("验证", enabled = canVerify, onClick = { verifyPlanAction(ui, planId, a.id) })
                }
            }
        },
    )
}