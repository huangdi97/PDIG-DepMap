// MakeBeforeBreak — Canonical v0.3.0（port of core/src/domain/make-before-break.ts）。
//
// Safety Invariant: BREAK_BEFORE_MAKE = FORBIDDEN（V030-MBB-01）
// 关键 recovery/access 变化必须按序：
//   new path established → new path verified → old path retired
// 测试必须证明：
//   new path not verified → retire old path = blocked
//   new path verified     → retire old path = allowed
//
// 分层约束：本模块是 domain 层纯逻辑，**零 @ohos 依赖**。
// ActionVerificationStatus 取自 domain/Entities（PlanAction.verification 的字段类型），
// MakeBeforeBreakStatus 取自 generated 枚举。

import { MakeBeforeBreakStatus } from '../generated/CanonicalEnums';
import { ActionVerificationStatus, PlanAction } from './Entities';

/** evaluateMakeBeforeBreak 的输入。 */
export class MakeBeforeBreakInput {
  /** 代表「新路径建立」的动作。 */
  newPathActions: PlanAction[] = [];
  /** 代表「验证新路径」的动作。 */
  verificationActions: PlanAction[] = [];
  /** 代表「退休旧路径」的动作（被 gate 保护）。 */
  retireActionId: string = '';
  /** 退休动作是否已完成（已完成无需再 gate）。 */
  retireAlreadyDone: boolean = false;
}

/** evaluateMakeBeforeBreak 的确定性结果。 */
export class MakeBeforeBreakResult {
  status: MakeBeforeBreakStatus = MakeBeforeBreakStatus.ALLOWED;
  /** 阻止退休的未验证新路径 action id 列表（确定性排序）。 */
  unverifiedNewPaths: string[] = [];
  /** 可解释的 blocking reason（面向应用层；UI 需要转成自然中文）。 */
  reason: string = '';
}

function sortedIds(actions: PlanAction[], keep: (a: PlanAction) => boolean): string[] {
  const out: string[] = [];
  for (let i = 0; i < actions.length; i++) {
    if (keep(actions[i])) {
      out.push(actions[i].id);
    }
  }
  out.sort();
  return out;
}

export function evaluateMakeBeforeBreak(input: MakeBeforeBreakInput): MakeBeforeBreakResult {
  if (input.retireAlreadyDone) {
    const done = new MakeBeforeBreakResult();
    done.status = MakeBeforeBreakStatus.ALLOWED;
    done.unverifiedNewPaths = [];
    done.reason = 'old path already retired';
    return done;
  }

  const unverifiedNewPaths = sortedIds(input.newPathActions, (a: PlanAction): boolean => !a.done);

  if (unverifiedNewPaths.length > 0) {
    const result = new MakeBeforeBreakResult();
    result.status = MakeBeforeBreakStatus.BLOCKED;
    result.unverifiedNewPaths = unverifiedNewPaths;
    result.reason = 'new path(s) not yet established: ' + unverifiedNewPaths.join(', ');
    return result;
  }

  // 新路径动作全部完成；再检查验证状态（done ≠ verified）
  const unverified = sortedIds(
    input.verificationActions,
    (a: PlanAction): boolean =>
      a.verification === null || a.verification.status !== ActionVerificationStatus.VERIFIED,
  );

  if (unverified.length > 0) {
    const result = new MakeBeforeBreakResult();
    result.status = MakeBeforeBreakStatus.BLOCKED;
    result.unverifiedNewPaths = unverified;
    result.reason = 'new path(s) not yet verified: ' + unverified.join(', ');
    return result;
  }

  const allowed = new MakeBeforeBreakResult();
  allowed.status = MakeBeforeBreakStatus.ALLOWED;
  allowed.unverifiedNewPaths = [];
  allowed.reason = 'all key new paths verified';
  return allowed;
}