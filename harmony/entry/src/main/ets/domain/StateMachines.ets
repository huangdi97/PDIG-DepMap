// DO NOT EDIT 之外的语义 — 语义必须与 TS 冻结基准一致
// 端口自 core/src/services/change-plan-service.ts 的 ALLOWED_TRANSITIONS 与
// 动作冻结守卫（MVP03 §52–§54）。
//
// 分层约束：只依赖 `../generated/CanonicalEnums`，不依赖 service / repository。
//
// 为什么状态机要落在 domain 层而不是 service 里：
//   转换合法性是**产品语义**（哪些状态是终态、能否从 review 退回 analyzed），
//   不是存储细节。放在纯函数里，才能在无数据库的情况下逐条穷举验证
//   —— 一共 8 个状态，穷举 8×8 的合法性是完全可行的，
//   而散在 service 里的 if 分支无法被穷举。
//
// 铁律（原文照录）：
//   - completed / cancelled 是**终态**（出边为空），不可再转出；
//   - done ≠ verified：完成动作只置 done，绝不动 verification 状态；
//   - 终态下动作被冻结（actions are frozen），不可完成、不可验证。

import { ActionVerificationStatus, ChangePlanWorkflowState } from '../generated/CanonicalEnums';

/**
 * 状态转换白名单。**逐条照录** TS 基准的 ALLOWED_TRANSITIONS，不得增删。
 *
 * 用 Map 而非 Record<string, string[]>：ArkTS 不支持索引签名，
 * Map 是等价且类型安全的表达。
 */
const ALLOWED: Map<string, string[]> = new Map<string, string[]>([
  [
    ChangePlanWorkflowState.DRAFT,
    [
      ChangePlanWorkflowState.ANALYZED,
      ChangePlanWorkflowState.REVIEW_REQUIRED,
      ChangePlanWorkflowState.READY,
      ChangePlanWorkflowState.IN_PROGRESS,
      ChangePlanWorkflowState.CANCELLED,
    ],
  ],
  [
    ChangePlanWorkflowState.ANALYZED,
    [
      ChangePlanWorkflowState.REVIEW_REQUIRED,
      ChangePlanWorkflowState.READY,
      ChangePlanWorkflowState.IN_PROGRESS,
      ChangePlanWorkflowState.CANCELLED,
    ],
  ],
  [
    ChangePlanWorkflowState.REVIEW_REQUIRED,
    [
      ChangePlanWorkflowState.READY,
      ChangePlanWorkflowState.IN_PROGRESS,
      ChangePlanWorkflowState.CANCELLED,
      ChangePlanWorkflowState.ANALYZED,
    ],
  ],
  [
    ChangePlanWorkflowState.READY,
    [
      ChangePlanWorkflowState.IN_PROGRESS,
      ChangePlanWorkflowState.CANCELLED,
      ChangePlanWorkflowState.REVIEW_REQUIRED,
    ],
  ],
  [
    ChangePlanWorkflowState.IN_PROGRESS,
    [
      ChangePlanWorkflowState.VERIFYING,
      ChangePlanWorkflowState.COMPLETED,
      ChangePlanWorkflowState.CANCELLED,
    ],
  ],
  [
    ChangePlanWorkflowState.VERIFYING,
    [
      ChangePlanWorkflowState.COMPLETED,
      ChangePlanWorkflowState.CANCELLED,
      ChangePlanWorkflowState.IN_PROGRESS,
    ],
  ],
  // 终态：出边为空。
  [ChangePlanWorkflowState.COMPLETED, []],
  [ChangePlanWorkflowState.CANCELLED, []],
]);

/** 终态集合。是否可再编辑、动作是否冻结，都以本判定为准。 */
export function isTerminalState(state: string): boolean {
  return state === ChangePlanWorkflowState.COMPLETED || state === ChangePlanWorkflowState.CANCELLED;
}

/**
 * 转换合法性判定。
 *
 * 对于**未知状态**返回 false（fail-closed）：
 * 一个不在词表里的状态说明数据已损坏或被未来版本写入，
 * 此时允许转换会把损坏状态带入下一步，比拒绝更危险。
 */
export function isTransitionAllowed(from: string, to: string): boolean {
  const list = ALLOWED.get(from);
  if (list === undefined) {
    return false;
  }
  for (let i = 0; i < list.length; i++) {
    if (list[i] === to) {
      return true;
    }
  }
  return false;
}

/** 某状态的所有合法出边（未知状态返回空数组）。 */
export function allowedNextStates(from: string): string[] {
  const list = ALLOWED.get(from);
  if (list === undefined) {
    return [];
  }
  const out: string[] = [];
  for (let i = 0; i < list.length; i++) {
    out.push(list[i]);
  }
  return out;
}

/** 状态机的全部状态（用于穷举验证）。 */
export const ALL_WORKFLOW_STATES: string[] = [
  ChangePlanWorkflowState.DRAFT,
  ChangePlanWorkflowState.ANALYZED,
  ChangePlanWorkflowState.REVIEW_REQUIRED,
  ChangePlanWorkflowState.READY,
  ChangePlanWorkflowState.IN_PROGRESS,
  ChangePlanWorkflowState.VERIFYING,
  ChangePlanWorkflowState.COMPLETED,
  ChangePlanWorkflowState.CANCELLED,
];

// ---------------------------------------------------------------------------
// 动作冻结（§52–§54：done ≠ verified）
// ---------------------------------------------------------------------------

export class ActionMutationGuard {
  allowed: boolean = false;
  reason: string = '';
}

/**
 * 判定在当前计划状态下，动作是否可被修改（完成 / 验证）。
 *
 * 原文：`plan ${planId} is ${state}; actions are frozen`。
 * 冻结的意义不只是 UI 防误操作 —— 它保证"已完成/已取消的计划不再变化"，
 * 否则 Timeline 与 Coverage 的结论会在用户已认定结束时继续漂移。
 */
export function canMutateAction(planId: string, workflowState: string): ActionMutationGuard {
  const g = new ActionMutationGuard();
  if (isTerminalState(workflowState)) {
    g.allowed = false;
    g.reason = `plan ${planId} is ${workflowState}; actions are frozen`;
    return g;
  }
  g.allowed = true;
  return g;
}

/**
 * 完成动作后的 verification 状态。
 *
 * 这是 VF 铁律（done ≠ verified）的**唯一**表达点：
 * 无论动作是否完成，verification 状态都不因"完成"而改变 ——
 * 只有显式的人工确认或未来观察证据才改它。
 * 因此本函数恒返回**未变**的 verification 状态，
 * 存在的意义是把这条铁律写成可调用、可测试的代码，而不是一句注释。
 */
export function verificationAfterCompletion(current: string): string {
  return current;
}

/** 手动验证后应置为的状态（user_confirmed 路径）。 */
export function verificationAfterManualConfirm(): string {
  return ActionVerificationStatus.VERIFIED;
}
