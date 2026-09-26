package com.pdig.core.domain

import com.pdig.core.generated.TemporalChangePhase

/**
 * TemporalChange — Canonical v0.3.0（port of core/src/domain/temporal-change.ts）。
 *
 * 最小模型（不实现通用调度系统）：只支持 replace_phone_number / identity-recovery
 * 所需的 transition window：
 *
 *   G_before → G_transition → G_after
 *
 * 字段：
 *   effectiveAt            变更生效时间
 *   verificationNotBefore  验证不早于
 *   verificationDueAt      验证截止
 *   retireOldPathAfter     旧路径最早退休时间（必须在所有关键新路径 verified 之后）
 *
 * 门（TC-ORDER-GATE / TC-RETIRE-GATE）：
 *   effectiveAt <= verificationNotBefore <= verificationDueAt <= retireOldPathAfter
 *   retireOldPathAfter 允许的前提 = all key new paths verified（BREAK_BEFORE_MAKE = FORBIDDEN）
 */

data class TemporalChangeWindow(
    val effectiveAt: String,
    val verificationNotBefore: String? = null,
    val verificationDueAt: String? = null,
    val retireOldPathAfter: String? = null,
)

data class TemporalChangeResult(
    val phase: TemporalChangePhase,
    val validOrder: Boolean,
    /** 允许 retirement 的日期（满足 gate 时才非空）。 */
    val retireAllowedAt: String?,
    /** 阻止 retirement 的原因（自然语言给应用层）。 */
    val retireBlockedReason: String?,
)

fun validateTemporalOrder(window: TemporalChangeWindow): Boolean {
    val times = listOf(
        window.effectiveAt,
        window.verificationNotBefore,
        window.verificationDueAt,
        window.retireOldPathAfter,
    )
    var prev: String? = null
    for (t in times) {
        if (t == null) continue
        if (prev != null && t < prev) return false
        prev = t
    }
    return true
}

fun classifyTemporalPhase(
    window: TemporalChangeWindow,
    now: String,
    allKeyNewPathsVerified: Boolean,
): TemporalChangeResult {
    val orderOk = validateTemporalOrder(window)

    if (!orderOk) {
        return TemporalChangeResult(
            phase = TemporalChangePhase.BEFORE,
            validOrder = false,
            retireAllowedAt = null,
            retireBlockedReason = "时间安排不合法：开始、验证、停用的顺序不正确。",
        )
    }

    val phase: TemporalChangePhase =
        if (now < window.effectiveAt) {
            TemporalChangePhase.BEFORE
        } else if (window.retireOldPathAfter != null && now >= window.retireOldPathAfter) {
            TemporalChangePhase.AFTER
        } else {
            TemporalChangePhase.TRANSITION
        }

    if (window.retireOldPathAfter == null) {
        return TemporalChangeResult(phase, true, null, "未设置停用时间")
    }

    if (!allKeyNewPathsVerified) {
        return TemporalChangeResult(phase, true, null, "新路径尚未全部验证，暂时不能停用旧路径。")
    }

    return TemporalChangeResult(phase, true, window.retireOldPathAfter, null)
}
