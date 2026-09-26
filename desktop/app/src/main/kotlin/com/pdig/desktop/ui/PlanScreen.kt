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

/** 变更计划详情：信息 + 动作清单（前置关系人话 + 完成 / 验证）。 */
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
            InfoRow("工作流状态", workflowStateLabel(detail.workflowState.wire))
            detail.effectiveState?.let { s -> InfoRow("派生生效状态", workflowStateLabel(s.wire)) }
            InfoRow("目标节点", detail.targetNodeName)
            InfoRow("生效日期", detail.effectiveDate ?: "未设置")
            InfoRow("就绪度", readinessLabel(detail.readiness.wire))
            InfoRow("受影响服务数", detail.affectedServiceCount.toString())
            InfoRow("必须处理的事项", "${detail.mustChangeKeys.size} 条")
            InfoRow("仍未解决", "${detail.unresolvedMustChangeKeys.size} 条")
            SectionDivider("动作（${detail.actions.size}）")
            if (detail.actions.isEmpty()) {
                EmptyState("该计划没有动作。")
            } else {
                detail.actions.forEach { a ->
                    PlanActionCard(ui, pid, a, detail.actions)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { ui.screen = Screen.ACTIONS }) { Text("行动计划") }
                TextButton(onClick = { ui.screen = Screen.VERIFICATION }) { Text("验证") }
            }
        }
    }
}

/**
 * 单个计划动作卡片：阶段 / done / verification + 前置关系人话 + 完成 / 验证按钮。
 * 前置关系只展示用户语义（必须先完成 / 完成后才能继续 / 等待验证 / 验证后才能移除旧路径 / 可以并行处理），
 * 不把 DAG / prerequisite / 内部枚举名泄漏给用户。
 */
@Composable
internal fun PlanActionCard(ui: UiState, planId: String, action: PlanAction, allActions: List<PlanAction>) {
    val v = action.verification
    val prereqTitles = action.prerequisiteActionIds.mapNotNull { pid -> allActions.firstOrNull { it.id == pid }?.title }
    val dependsTitles = allActions.filter { pid -> action.id in pid.prerequisiteActionIds }.map { it.title }
    val preconditionLines = buildPreconditionLines(action, prereqTitles, dependsTitles)
    PdigCard(
        title = action.title,
        subtitle = listOfNotNull(
            "阶段：${actionPhaseLabel(action.phase.wire)}",
            v?.let { "验证：${verificationMethodLabel(it.method.wire)} / ${verificationStatusLabel(it.status.wire)}" },
            preconditionLines.takeIf { it.isNotEmpty() },
        ).joinToString(" · "),
        trailing = {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                StatusChip(if (action.done) "done" else "todo", if (action.done) ChipTone.GOOD else ChipTone.NEUTRAL)
                if (preconditionLines.isNotEmpty()) {
                    Text(
                        preconditionLines.joinToString("\n"),
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val prereqsDone = action.prerequisiteActionIds.all { p ->
                        allActions.firstOrNull { it.id == p }?.done == true
                    }
                    TextButton(
                        onClick = { completePlanAction(ui, planId, action.id) },
                        enabled = prereqsDone && !action.done,
                    ) { Text("标记完成") }
                    val canVerify = canVerifyAction(action)
                    TextButton(onClick = { verifyPlanAction(ui, planId, action.id) }, enabled = canVerify) { Text("验证") }
                }
            }
        },
    )
}

/** 前置关系人话：每条一句话，正面给出用户可以理解的动作依赖。 */
internal fun buildPreconditionLines(
    action: PlanAction,
    prereqTitles: List<String>,
    dependsTitles: List<String>,
): List<String> = buildList {
    if (prereqTitles.isNotEmpty()) {
        add("必须先完成：${prereqTitles.joinToString("、")}")
    }
    if (dependsTitles.isNotEmpty()) {
        add("完成后才能继续：${dependsTitles.joinToString("、")}")
    }
    val v = action.verification
    if (action.done && v != null && v.status == ActionVerificationStatus.PENDING) {
        add("等待验证")
    }
    if (action.title.contains("停用旧") && prereqTitles.isNotEmpty()) {
        add("验证后才能移除旧路径")
    }
    if (isEmpty()) {
        add("可以并行处理")
    }
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