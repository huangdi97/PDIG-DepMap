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

import {
  ActionVerificationMethod,
  ActionVerificationStatus,
  CandidateStatus,
  ChangePlanWorkflowState,
  DriftStatus,
  ErrorCode,
} from '../generated/CanonicalEnums';

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

/* ==========================================================================
 * 状态机守卫与另外三台状态机
 *
 * 为什么补上这三台（2026-09-18）：
 *   它们此前只存在于 conformance fixture 的 expected 里，platform 侧没有实现 ——
 *   于是三个用例一直记 NOT_IMPLEMENTED。而"哪些迁移合法、哪个守卫该拒绝什么"
 *   是**产品合同**，不是文档：三端必须拒绝同样的迁移、报同样的错误码。
 *
 * 取值来源：`spec/state-machines/state-machines.json`
 *   （ActionVerificationStatus / DiscoveryCandidateStatus / RealityDriftStatus）。
 *   守卫的 id / rule / errorCode 是**跨端合同原文**，必须逐字一致 ——
 *   所以它们在这里是常量而不是注释。
 * ========================================================================== */

/** 状态机守卫：id、规则原文、错误码三者都是合同的一部分。 */
export class StateGuard {
  id: string = '';
  rule: string = '';
  errorCode: string = '';

  constructor(id: string, rule: string, errorCode: string) {
    this.id = id;
    this.rule = rule;
    this.errorCode = errorCode;
  }
}

// ---------------------------------------------------------------------------
// ActionVerificationStatus（VF 铁律：done ≠ verified）
// ---------------------------------------------------------------------------

/** 状态次序 = spec 的自然序，也是 conformance 里 transitions 的键序。 */
export const VERIFICATION_STATES: string[] = [
  ActionVerificationStatus.NOT_REQUIRED,
  ActionVerificationStatus.PENDING,
  ActionVerificationStatus.EVIDENCE_SUGGESTED,
  ActionVerificationStatus.VERIFIED,
  ActionVerificationStatus.FAILED,
];

export const VERIFICATION_INITIAL: string = ActionVerificationStatus.PENDING;

/**
 * 合法迁移表。
 *
 * `evidence_suggested → evidence_suggested` 的自环是**刻意**的：
 * 同一条证据可以重复到达，效果是幂等置位，而不是非法迁移。
 */
const VERIFICATION_ALLOWED: Map<string, string[]> = new Map<string, string[]>([
  [ActionVerificationStatus.NOT_REQUIRED, []],
  [
    ActionVerificationStatus.PENDING,
    [
      ActionVerificationStatus.EVIDENCE_SUGGESTED,
      ActionVerificationStatus.VERIFIED,
      ActionVerificationStatus.FAILED,
      ActionVerificationStatus.NOT_REQUIRED,
    ],
  ],
  [
    ActionVerificationStatus.EVIDENCE_SUGGESTED,
    [
      ActionVerificationStatus.VERIFIED,
      ActionVerificationStatus.FAILED,
      ActionVerificationStatus.EVIDENCE_SUGGESTED,
    ],
  ],
  [ActionVerificationStatus.VERIFIED, []],
  [ActionVerificationStatus.FAILED, []],
]);

export function allowedVerificationNext(from: string): string[] {
  const list = VERIFICATION_ALLOWED.get(from);
  if (list === undefined) {
    return [];
  }
  const out: string[] = [];
  for (let i = 0; i < list.length; i++) {
    out.push(list[i]);
  }
  return out;
}

/** 终态：verified / failed。注意 **not_required 不是终态**（它为空的出边是"无处可去"，不是"结束"）。 */
export function isVerificationTerminal(state: string): boolean {
  return state === ActionVerificationStatus.VERIFIED || state === ActionVerificationStatus.FAILED;
}

export const VERIFICATION_GUARDS: StateGuard[] = [
  new StateGuard(
    'VF-FROZEN',
    'evidence suggestion must not override verified, failed or not_required',
    ErrorCode.ILLEGAL_STATE_TRANSITION,
  ),
];

/** 证据信号规则 —— appliesOnlyTo 就是"不可被覆盖"的那条守卫的落点。 */
export const VERIFICATION_EVIDENCE_APPLIES_TO: string[] = [
  ActionVerificationStatus.PENDING,
  ActionVerificationStatus.EVIDENCE_SUGGESTED,
];
export const VERIFICATION_EVIDENCE_METHOD: string = ActionVerificationMethod.FUTURE_OBSERVATION;
export const VERIFICATION_EVIDENCE_MATCH: string =
  'expectedFromNodeId AND expectedToNodeId must both equal the signal';
export const VERIFICATION_EVIDENCE_EFFECT: string = 'status becomes evidence_suggested';
export const VERIFICATION_EVIDENCE_NEVER: string[] = [
  'mutates Reality',
  'auto-verifies',
  'bumps graphRevision',
];

// ---------------------------------------------------------------------------
// DiscoveryCandidateStatus
// ---------------------------------------------------------------------------

export const CANDIDATE_STATES: string[] = [
  CandidateStatus.PENDING,
  CandidateStatus.ACCEPTED,
  CandidateStatus.DISMISSED,
  CandidateStatus.SUPERSEDED,
];

export const CANDIDATE_INITIAL: string = CandidateStatus.PENDING;

/**
 * `dismissed → pending` 是唯一的"回退"边：需要 ≥2 条**新**观测才允许
 * （reappeal 条件见 CANDIDATE_DISMISS_REAPPEAL）。迁移表本身不编码条数，
 * 条数由 reappeal 规则约束 —— 两处必须一起读。
 */
const CANDIDATE_ALLOWED: Map<string, string[]> = new Map<string, string[]>([
  [CandidateStatus.PENDING, [CandidateStatus.ACCEPTED, CandidateStatus.DISMISSED]],
  [CandidateStatus.ACCEPTED, []],
  [CandidateStatus.DISMISSED, [CandidateStatus.PENDING]],
  [CandidateStatus.SUPERSEDED, []],
]);

export function allowedCandidateNext(from: string): string[] {
  const list = CANDIDATE_ALLOWED.get(from);
  if (list === undefined) {
    return [];
  }
  const out: string[] = [];
  for (let i = 0; i < list.length; i++) {
    out.push(list[i]);
  }
  return out;
}

export const CANDIDATE_GUARDS: StateGuard[] = [
  new StateGuard(
    'CANDIDATE-PENDING-ONLY',
    'accept on a dismissed or accepted candidate MUST be rejected',
    ErrorCode.ILLEGAL_STATE_TRANSITION,
  ),
];

/**
 * 接受候选的效果。
 *
 * `bumpsGraphRevision: false` 值得注意：接受候选**会**改 Reality（创建 Node），
 * 却**不**提升 revision —— 因为 revision 是"已确认依赖图"的语义版本，
 * 而候选接受产生的节点由后续的用户确认才进入依赖图。
 * 这两条极易被写成"改 Reality 就等于 bump"，所以在此显式并列声明。
 */
export const CANDIDATE_ACCEPT_MUTATES_REALITY: boolean = true;
export const CANDIDATE_ACCEPT_BUMPS_REVISION: boolean = false;
export const CANDIDATE_ACCEPT_EFFECTS: string[] = ['create exactly one Node'];
export const CANDIDATE_ACCEPT_REPLAY: string =
  'idempotent - accept replay returns created=false and the same nodeId';

export const CANDIDATE_DISMISS_MUTATES_REALITY: boolean = false;
export const CANDIDATE_DISMISS_EFFECTS: string[] = ['record dismissedAtObservationCount'];
export const CANDIDATE_DISMISS_REAPPEAL: string =
  'returns to pending only after newObservations >= 2';

// ---------------------------------------------------------------------------
// RealityDriftStatus
// ---------------------------------------------------------------------------

export const DRIFT_STATES: string[] = [
  DriftStatus.OPEN,
  DriftStatus.CONFIRMED_CHANGE,
  DriftStatus.DISMISSED,
  DriftStatus.SUPERSEDED,
];

export const DRIFT_INITIAL: string = DriftStatus.OPEN;

const DRIFT_ALLOWED: Map<string, string[]> = new Map<string, string[]>([
  [DriftStatus.OPEN, [DriftStatus.CONFIRMED_CHANGE, DriftStatus.DISMISSED]],
  [DriftStatus.CONFIRMED_CHANGE, []],
  [DriftStatus.DISMISSED, []],
  [DriftStatus.SUPERSEDED, []],
]);

export function allowedDriftNext(from: string): string[] {
  const list = DRIFT_ALLOWED.get(from);
  if (list === undefined) {
    return [];
  }
  const out: string[] = [];
  for (let i = 0; i < list.length; i++) {
    out.push(list[i]);
  }
  return out;
}

export const DRIFT_GUARDS: StateGuard[] = [
  new StateGuard(
    'DRIFT-OPEN-ONLY',
    'resolve/dismiss on a non-open drift MUST be rejected',
    ErrorCode.ILLEGAL_STATE_TRANSITION,
  ),
];

/**
 * drift 创建规则。
 *
 * `minObservations: 2` + `absenceOnly: must never create a drift` 是这条规则的核心：
 * **只有正面证据能造出 drift，且至少 2 条观测**。
 * 这与项目全局取向一致 —— 缺失不能提升为结论。
 */
export const DRIFT_CREATION_REQUIRES: string = 'positive evidence only';
export const DRIFT_CREATION_MIN_OBSERVATIONS: number = 2;
export const DRIFT_CREATION_ABSENCE_ONLY: string = 'must never create a drift';
export const DRIFT_CREATION_ALREADY_CONFIRMED: string = 'ignored with reason already_confirmed';
export const DRIFT_CREATION_BELOW_THRESHOLD: string = 'ignored with reason below_threshold';
export const DRIFT_CREATION_DUPLICATE_REF: string = 'does not re-create or re-count';
export const DRIFT_CREATION_UPSERT: string =
  'same drift key accumulates observationCount and evidenceRefs into ONE open drift';
