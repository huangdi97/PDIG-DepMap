package com.pdig.core.domain

import com.pdig.core.generated.MakeBeforeBreakStatus

/**
 * Make-Before-Break — Canonical v0.3.0（port of core/src/domain/make-before-break.ts）。
 *
 * Safety Invariant: BREAK_BEFORE_MAKE = FORBIDDEN（V030-MBB-01）
 *
 * 关键 recovery/access 变化必须按序：
 *   new path established → new path verified → old path retired
 * 测试必须证明：
 *   new path not verified → retire old path = blocked
 *   new path verified     → retire old path = allowed
 */

data class MakeBeforeBreakInput(
    /** 代表「新路径建立」的动作。 */
    val newPathActions: List<PlanAction>,
    /** 代表「验证新路径」的动作。 */
    val verificationActions: List<PlanAction>,
    /** 代表「退休旧路径」的动作（被 gate 保护）。 */
    val retireActionId: String,
    /** 退休动作是否已完成（已完成无需再 gate）。 */
    val retireAlreadyDone: Boolean,
)

data class MakeBeforeBreakResult(
    val status: MakeBeforeBreakStatus,
    /** 阻止退休的未验证新路径 action id 列表（确定性排序）。 */
    val unverifiedNewPaths: List<String>,
    /** 可解释的 blocking reason（面向应用层；UI 需要转成自然中文）。 */
    val reason: String,
)

fun evaluateMakeBeforeBreak(input: MakeBeforeBreakInput): MakeBeforeBreakResult {
    if (input.retireAlreadyDone) {
        return MakeBeforeBreakResult(MakeBeforeBreakStatus.ALLOWED, emptyList(), "old path already retired")
    }

    val unverifiedNewPaths = input.newPathActions
        .filter { !it.done }
        .map { it.id }
        .sorted()

    if (unverifiedNewPaths.isNotEmpty()) {
        return MakeBeforeBreakResult(
            status = MakeBeforeBreakStatus.BLOCKED,
            unverifiedNewPaths = unverifiedNewPaths,
            reason = "new path(s) not yet established: ${unverifiedNewPaths.joinToString(", ")}",
        )
    }

    // 新路径动作全部完成；再检查验证状态（done ≠ verified）
    val unverified = input.verificationActions
        .filter { it.verification?.status != ActionVerificationStatus.VERIFIED }
        .map { it.id }
        .sorted()

    if (unverified.isNotEmpty()) {
        return MakeBeforeBreakResult(
            status = MakeBeforeBreakStatus.BLOCKED,
            unverifiedNewPaths = unverified,
            reason = "new path(s) not yet verified: ${unverified.joinToString(", ")}",
        )
    }

    return MakeBeforeBreakResult(MakeBeforeBreakStatus.ALLOWED, emptyList(), "all key new paths verified")
}
