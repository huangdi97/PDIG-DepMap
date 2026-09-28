package com.pdig.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pdig.core.domain.ActionVerificationStatus
import com.pdig.core.domain.PlanAction
import com.pdig.desktop.ui.components.ContinuityRail
import com.pdig.desktop.ui.components.ContinuityStep
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.InfoRow
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.RailAction
import com.pdig.desktop.ui.components.RailState
import com.pdig.desktop.ui.components.SectionHeader
import com.pdig.desktop.ui.theme.PdigDesktopTokens as T
import com.pdig.desktop.ui.theme.PdigType

/**
 * 变更计划（spec §35）：action-oriented structured plan ——
 * Continuity Rail 表达「必须先完成 / 当前进行 / 可以并行 / 等待验证 / 之后才能继续 / 已完成」。
 * Make-before-break（spec §32）：新路径未验证 → 停用旧路径步骤 blocked + 明文原因（不止 disabled）。
 */
@Composable
fun PlanScreen(ui: UiState) {
    val planId = ui.selectedPlanId
    val detail = planId?.let { ui.session.plans.planDetail(it) }
    PdigPage(
        title = "变更计划",
        subtitle = detail?.title ?: "未选择计划",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        if (detail == null) {
            EmptyState("未找到计划。请先在场景中创建。")
            return@PdigPage
        }
        val pid = planId ?: return@PdigPage
        Column(Modifier.fillMaxWidth()) {
            // 计划状态概览
            SectionHeader("状态")
            Surface(
                Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(T.RadiusMd),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(T.SpaceLg)) {
                    Text(
                        readinessLabel(detail.readiness.wire),
                        style = PdigType.SectionTitle,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(T.SpaceXs))
                    Text(
                        "目标：${detail.targetNodeName} · 受影响服务 ${detail.affectedServiceCount} 个 · 必须处理 ${detail.mustChangeKeys.size} 条",
                        style = PdigType.Secondary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (detail.unresolvedMustChangeKeys.isNotEmpty()) {
                        Spacer(Modifier.height(T.SpaceXs))
                        Text(
                            "仍未解决：${detail.unresolvedMustChangeKeys.size} 条",
                            style = PdigType.Label,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            // 变更步骤 —— Continuity Rail
            SectionHeader("变更步骤")
            if (detail.actions.isEmpty()) {
                EmptyState("该计划没有动作。")
            } else {
                val steps = detail.actions.map { a ->
                    ContinuityStep(
                        label = a.title,
                        state = actionRailState(a),
                        description = planActionDescription(a),
                        stateLabel = actionRailLabel(a),
                        content = {
                            PlanActionRow(ui, pid, a, detail.actions)
                        },
                    )
                }
                ContinuityRail(
                    steps = steps,
                    blockedReason = if (steps.any { it.state == RailState.BLOCKED }) {
                        "新路径验证通过后才能执行这一步（先建立新路径，再停用旧路径）。"
                    } else null,
                )
            }

            Spacer(Modifier.height(T.SpaceLg))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { ui.screen = Screen.ACTIONS }) { Text("行动计划") }
                TextButton(onClick = { ui.screen = Screen.VERIFICATION }) { Text("验证") }
                TextButton(onClick = { ui.screen = Screen.IMPACT }) { Text("查看影响") }
            }
        }
    }
}

private fun actionRailState(a: PlanAction): RailState {
    val v = a.verification
    if (v != null && v.status == ActionVerificationStatus.VERIFIED) return RailState.VERIFIED
    if (a.done) return RailState.SUCCESS
    if (a.title.contains("停用旧") || a.title.contains("移除旧")) return RailState.BLOCKED
    val prereqsDone = a.prerequisiteActionIds.isEmpty()
    return if (prereqsDone) RailState.ACTIVE else RailState.BLOCKED
}

private fun actionRailLabel(a: PlanAction): String? = when (actionRailState(a)) {
    RailState.VERIFIED -> "已验证"
    RailState.SUCCESS -> "已完成"
    RailState.BLOCKED -> "等待前置完成"
    RailState.ACTIVE -> "进行中"
    RailState.UPCOMING -> null
}

private fun planActionDescription(a: PlanAction): String? {
    val v = a.verification
    val parts = listOfNotNull(
        "阶段：${actionPhaseLabel(a.phase.wire)}",
        v?.let { "验证：${verificationStatusLabel(it.status.wire)}" },
    )
    return parts.joinToString(" · ").takeIf { it.isNotEmpty() }
}

/** 步骤下的操作行：完成 / 验证按钮（带前置闸门语义）。 */
@Composable
private fun PlanActionRow(ui: UiState, planId: String, action: PlanAction, allActions: List<PlanAction>) {
    val v = action.verification
    val prereqsDone = action.prerequisiteActionIds.all { p ->
        allActions.firstOrNull { it.id == p }?.done == true
    }
    val canComplete = prereqsDone && !action.done
    val canVerify = canVerifyAction(action)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RailAction("标记完成", enabled = canComplete, onClick = { completePlanAction(ui, planId, action.id) })
        if (v != null) {
            RailAction("验证", enabled = canVerify, onClick = { verifyPlanAction(ui, planId, action.id) })
        }
    }
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