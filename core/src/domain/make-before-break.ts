import type { PlanAction } from './change-plan.ts'

/**
 * Make-Before-Break — Canonical vNext (v0.3.0)
 *
 * Safety Invariant: BREAK_BEFORE_MAKE = FORBIDDEN（V030-MBB-01）
 *
 * 关键 recovery/access 变化必须按序：
 *   new path established → new path verified → old path retired
 * 测试必须证明：
 *   new path not verified → retire old path = blocked
 *   new path verified     → retire old path = allowed
 */

export type MakeBeforeBreakStatus = 'blocked' | 'allowed'

export interface MakeBeforeBreakResult {
  status: MakeBeforeBreakStatus
  /** 阻止退休的未验证新路径 action id 列表（确定性排序）。 */
  unverifiedNewPaths: string[]
  /** 可解释的 blocking reason（面向应用层；UI 需要转成自然中文）。 */
  reason: string
}

export interface MakeBeforeBreakInput {
  /** 代表「新路径建立」的动作。 */
  newPathActions: PlanAction[]
  /** 代表「验证新路径」的动作。 */
  verificationActions: PlanAction[]
  /** 代表「退休旧路径」的动作（被 gate 保护）。 */
  retireActionId: string
  /** 退休动作是否已完成（已完成无需再 gate）。 */
  retireAlreadyDone: boolean
}

export function evaluateMakeBeforeBreak(input: MakeBeforeBreakInput): MakeBeforeBreakResult {
  if (input.retireAlreadyDone) {
    return { status: 'allowed', unverifiedNewPaths: [], reason: 'old path already retired' }
  }

  const unverifiedNewPaths = input.newPathActions
    .filter((a) => !a.done)
    .map((a) => a.id)
    .sort()

  if (unverifiedNewPaths.length > 0) {
    return {
      status: 'blocked',
      unverifiedNewPaths,
      reason: `new path(s) not yet established: ${unverifiedNewPaths.join(', ')}`,
    }
  }

  // 新路径动作全部完成；再检查验证状态（done ≠ verified）
  const unverified = input.verificationActions
    .filter((v) => v.verification?.status !== 'verified')
    .map((v) => v.id)
    .sort()

  if (unverified.length > 0) {
    return {
      status: 'blocked',
      unverifiedNewPaths: unverified,
      reason: `new path(s) not yet verified: ${unverified.join(', ')}`,
    }
  }

  return { status: 'allowed', unverifiedNewPaths: [], reason: 'all key new paths verified' }
}
