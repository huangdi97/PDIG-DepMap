package com.pdig.app.ui.screens

import com.pdig.core.domain.ActionVerificationStatus
import com.pdig.core.domain.PlanAction

/**
 * 步骤轨道状态：verified > completed > in_progress > blocked > upcoming（UIUX_FREEZE §9 / spec §36）。
 * verified 必须视觉与语义上都强于 completed —— 这两个状态在 UI 上必须同时可见。
 */
internal enum class StepState { VERIFIED, COMPLETED, IN_PROGRESS, BLOCKED, UPCOMING }

internal fun stepStateOf(action: PlanAction, actions: List<PlanAction>): StepState {
    val status = action.verification?.status
    if (action.done && status == ActionVerificationStatus.VERIFIED) return StepState.VERIFIED
    if (action.done) return StepState.COMPLETED
    val allPrereqsDone = action.prerequisiteActionIds.all { pid -> actions.any { it.id == pid && it.done } }
    if (!allPrereqsDone) return StepState.BLOCKED
    val idx = actions.indexOfFirst { it.id == action.id }
    val earlierDone = if (idx < 0) true else actions.subList(0, idx).all { it.done }
    return if (earlierDone) StepState.IN_PROGRESS else StepState.UPCOMING
}

internal fun stepStateLabel(state: StepState): String = when (state) {
    StepState.VERIFIED -> "已验证"
    StepState.COMPLETED -> "已完成"
    StepState.IN_PROGRESS -> "进行中"
    StepState.BLOCKED -> "等待前置"
    StepState.UPCOMING -> "即将到来"
}

/**
 * Make-Before-Break 明文闸门（UIUX_FREEZE §3/§32，spec §36）：
 * 新路径未验证前，停用旧路径的动作必须显示明文原因，禁止只 disabled。
 */
internal const val GATE_REASON_DEACTIVATE_OLD =
    "新手机号验证通过后才能停用旧手机号（先建立新路径，再移除旧路径）"

internal fun gateReasonFor(action: PlanAction): String? =
    if (action.title.contains("停用旧") && action.verification?.status != ActionVerificationStatus.VERIFIED) {
        GATE_REASON_DEACTIVATE_OLD
    } else {
        null
    }

/** 验证状态人话标签。done ≠ verified —— 这两个状态在 UI 上必须同时可见。 */
internal fun verificationLabel(status: ActionVerificationStatus?): String = when (status) {
    null -> "无需验证"
    ActionVerificationStatus.NOT_REQUIRED -> "无需验证"
    ActionVerificationStatus.PENDING -> "待验证"
    ActionVerificationStatus.EVIDENCE_SUGGESTED -> "发现可能是变更后的证据"
    ActionVerificationStatus.VERIFIED -> "已验证"
    ActionVerificationStatus.FAILED -> "验证未通过"
}

/** 前置关系人话：每条一句话，正面给出用户可以理解的动作依赖（对齐 Desktop PlanScreen）。 */
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
    if (isEmpty()) {
        add("可以并行处理")
    }
}