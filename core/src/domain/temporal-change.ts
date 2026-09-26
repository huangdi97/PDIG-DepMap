/**
 * TemporalChange — Canonical vNext (v0.3.0)
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

export type TemporalChangePhase = 'before' | 'transition' | 'after'

export interface TemporalChangeWindow {
  effectiveAt: string
  verificationNotBefore: string | null
  verificationDueAt: string | null
  retireOldPathAfter: string | null
}

export interface TemporalChangeResult {
  phase: TemporalChangePhase
  validOrder: boolean
  /** 允许 retirement 的日期（满足 gate 时才非空）。 */
  retireAllowedAt: string | null
  /** 阻止 retirement 的原因（自然语言给应用层）。 */
  retireBlockedReason: string | null
}

export function classifyTemporalPhase(
  window: TemporalChangeWindow,
  now: string,
  allKeyNewPathsVerified: boolean,
): TemporalChangeResult {
  const orderOk = validateTemporalOrder(window)

  if (!orderOk) {
    return {
      phase: 'before',
      validOrder: false,
      retireAllowedAt: null,
      retireBlockedReason: '时间安排不合法：开始、验证、停用的顺序不正确。',
    }
  }

  const phase: TemporalChangePhase =
    now < window.effectiveAt
      ? 'before'
      : window.retireOldPathAfter !== null && now >= window.retireOldPathAfter
        ? 'after'
        : 'transition'

  if (window.retireOldPathAfter === null) {
    return { phase, validOrder: true, retireAllowedAt: null, retireBlockedReason: '未设置停用时间' }
  }

  if (!allKeyNewPathsVerified) {
    return {
      phase,
      validOrder: true,
      retireAllowedAt: null,
      retireBlockedReason: '新路径尚未全部验证，暂时不能停用旧路径。',
    }
  }

  return {
    phase,
    validOrder: true,
    retireAllowedAt: window.retireOldPathAfter,
    retireBlockedReason: null,
  }
}

export function validateTemporalOrder(window: TemporalChangeWindow): boolean {
  const times: Array<string | null> = [
    window.effectiveAt,
    window.verificationNotBefore,
    window.verificationDueAt,
    window.retireOldPathAfter,
  ]
  let prev: string | null = null
  for (const t of times) {
    if (t === null) continue
    if (prev !== null && t < prev) return false
    prev = t
  }
  return true
}
