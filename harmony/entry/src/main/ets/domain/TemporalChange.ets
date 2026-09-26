// TemporalChange — Canonical v0.3.0（port of core/src/domain/temporal-change.ts）。
//
// 最小模型（不实现通用调度系统）：只支持 replace_phone_number / identity-recovery
// 所需的 transition window：
//
//   G_before → G_transition → G_after
//
// 字段：
//   effectiveAt            变更生效时间
//   verificationNotBefore  验证不早于
//   verificationDueAt      验证截止
//   retireOldPathAfter     旧路径最早退休时间（必须在所有关键新路径 verified 之后）
//
// 门（TC-ORDER-GATE / TC-RETIRE-GATE）：
//   effectiveAt <= verificationNotBefore <= verificationDueAt <= retireOldPathAfter
//   retireOldPathAfter 允许的前提 = all key new paths verified（BREAK_BEFORE_MAKE = FORBIDDEN）
//
// 分层约束：本模块是 domain 层纯逻辑，**零 @ohos 依赖**。

import { TemporalChangePhase } from '../generated/CanonicalEnums';

/** 时间窗（ISO 8601 字符串；null 表示未设置）。 */
export class TemporalChangeWindow {
  effectiveAt: string = '';
  verificationNotBefore: string | null = null;
  verificationDueAt: string | null = null;
  retireOldPathAfter: string | null = null;
}

/** classifyTemporalPhase 的确定性结果。 */
export class TemporalChangeResult {
  phase: TemporalChangePhase = TemporalChangePhase.BEFORE;
  validOrder: boolean = true;
  /** 允许 retirement 的日期（满足 gate 时才非空）。 */
  retireAllowedAt: string | null = null;
  /** 阻止 retirement 的原因（自然语言给应用层）。 */
  retireBlockedReason: string | null = null;
}

export function validateTemporalOrder(window: TemporalChangeWindow): boolean {
  const times: Array<string | null> = [
    window.effectiveAt,
    window.verificationNotBefore,
    window.verificationDueAt,
    window.retireOldPathAfter,
  ];
  let prev: string | null = null;
  for (let i = 0; i < times.length; i++) {
    const t = times[i];
    if (t === null) {
      continue;
    }
    if (prev !== null && t < prev) {
      return false;
    }
    prev = t;
  }
  return true;
}

export function classifyTemporalPhase(
  window: TemporalChangeWindow,
  now: string,
  allKeyNewPathsVerified: boolean,
): TemporalChangeResult {
  const orderOk = validateTemporalOrder(window);

  if (!orderOk) {
    const result = new TemporalChangeResult();
    result.phase = TemporalChangePhase.BEFORE;
    result.validOrder = false;
    result.retireAllowedAt = null;
    result.retireBlockedReason = '时间安排不合法：开始、验证、停用的顺序不正确。';
    return result;
  }

  let phase: TemporalChangePhase = TemporalChangePhase.TRANSITION;
  if (now < window.effectiveAt) {
    phase = TemporalChangePhase.BEFORE;
  } else if (window.retireOldPathAfter !== null && now >= window.retireOldPathAfter) {
    phase = TemporalChangePhase.AFTER;
  }

  if (window.retireOldPathAfter === null) {
    const result = new TemporalChangeResult();
    result.phase = phase;
    result.validOrder = true;
    result.retireAllowedAt = null;
    result.retireBlockedReason = '未设置停用时间';
    return result;
  }

  if (!allKeyNewPathsVerified) {
    const result = new TemporalChangeResult();
    result.phase = phase;
    result.validOrder = true;
    result.retireAllowedAt = null;
    result.retireBlockedReason = '新路径尚未全部验证，暂时不能停用旧路径。';
    return result;
  }

  const result = new TemporalChangeResult();
  result.phase = phase;
  result.validOrder = true;
  result.retireAllowedAt = window.retireOldPathAfter;
  result.retireBlockedReason = null;
  return result;
}