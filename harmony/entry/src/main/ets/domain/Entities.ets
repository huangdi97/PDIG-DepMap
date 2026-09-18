// DO NOT EDIT 之外的语义 — 语义必须与 Android 冻结基准一致
// 端口自 android/core/src/main/kotlin/com/pdig/core/domain/Models.kt
// 冻结依据：ANDROID_NATIVE_CORE_FREEZE.md @ HEAD 6053f3c
//
// 分层约束（HARMONY_N3_IMPLEMENTATION_STATUS.md §G）：
// 本模块属于 domain 层，**不得**依赖 ArkUI / Ability / relationalStore / HUKS /
// NAPI / cryptoFramework。当前导入集合仅含 `../generated/CanonicalEnums`。
//
// 语义铁律（spec/domain/entities.md）：
//  - Dependency 存在即代表用户确认
//  - criticality 只有 required / unknown，机器永不产生 required
//  - retired 表示"不再成立"，不是删除；重新成立时 re-activate 原行

import { capabilityFromWire, Capability, ChangePlanWorkflowState, CoverageLevel, Criticality, DependencyOrigin, DependencyState, GroupMode, GroupState, NodeKind, PlanActionPhase, Relation } from '../generated/CanonicalEnums';

export class DepNode {
  id: string = '';
  kind: NodeKind = NodeKind.CUSTOM;
  name: string = '';
  issuer: string | null = null;
  last4: string | null = null;
  archived: boolean = false;
}

/** `from` 为 `to` 提供支付 → from 失效时 to 受影响。 */
export class Dependency {
  id: string = '';
  from: string = '';
  relation: Relation = Relation.FUNDING_SOURCE;
  to: string = '';
  capability: Capability = Capability.PAYMENT;
  criticality: Criticality = Criticality.UNKNOWN;
  groupId: string | null = null;
  state: DependencyState = DependencyState.ACTIVE;
  origin: DependencyOrigin = DependencyOrigin.MANUAL;
  lastVerifiedAt: string = '';
}

export class DependencyGroup {
  id: string = '';
  groupKey: string = '';
  targetNodeId: string = '';
  capability: Capability = Capability.PAYMENT;
  mode: GroupMode = GroupMode.ANY;
  memberEdgeIds: string[] = [];
  state: GroupState = GroupState.ACTIVE;
}

export class ImpactProposalInput {
  key: string = '';
  from: string = '';
  to: string = '';
  capability: Capability = Capability.PAYMENT;
  confidenceScore: number | null = null;
}

export class ImpactGraph {
  dependencies: Dependency[] = [];
  groups: DependencyGroup[] = [];
  proposals: ImpactProposalInput[] = [];
  nodeNames: Map<string, string> = new Map<string, string>();
}

/**
 * ImpactStateKey —— 状态键是 (nodeId, capability)，不是 nodeId。
 *
 * 这是一条不可退化的不变式：仅以 nodeId 作 visited 会漏掉
 * "同一节点在两个 capability 上状态不同"的情形（CANONICAL_DESIGN §7）。
 */
export class ImpactStateKey {
  nodeId: string = '';
  capability: Capability = Capability.PAYMENT;

  constructor(nodeId: string = '', capability: Capability = Capability.PAYMENT) {
    this.nodeId = nodeId;
    this.capability = capability;
  }

  /** 解析 `nodeId|capability`；格式非法返回 undefined（调用方决定如何报错）。 */
  static parse(s: string): ImpactStateKey | undefined {
    const i = s.indexOf('|');
    if (i <= 0) {
      return undefined;
    }
    const nodeId = s.substring(0, i);
    const capWire = s.substring(i + 1);
    const cap = capabilityFromWire(capWire);
    if (cap === undefined) {
      return undefined;
    }
    return new ImpactStateKey(nodeId, cap);
  }

  keyString(): string {
    return `${this.nodeId}|${this.capability}`;
  }
}

/** logical key: `from|relation|to|capability` — 跨端必须完全一致（spec §9）。 */
export function dependencyLogicalKey(
  from: string,
  relation: Relation,
  to: string,
  capability: Capability,
): string {
  return `${from}|${relation}|${to}|${capability}`;
}

/** logical key 的四个分段，供接收结构化输入的调用方复用同一套口径。 */
export class DependencyLogicalKeyParts {
  from: string = '';
  relation: Relation = Relation.FUNDING_SOURCE;
  to: string = '';
  capability: Capability = Capability.PAYMENT;
}

/**
 * canonical groupKey: `target|capability|mode|sorted(dedup(members...))`
 * 成员顺序无关（spec §4）。
 */
export function canonicalGroupKey(
  targetNodeId: string,
  capability: Capability,
  mode: GroupMode,
  memberLogicalKeys: string[],
): string {
  const deduped: string[] = [];
  for (let i = 0; i < memberLogicalKeys.length; i++) {
    const k = memberLogicalKeys[i];
    let seen = false;
    for (let j = 0; j < deduped.length; j++) {
      if (deduped[j] === k) {
        seen = true;
        break;
      }
    }
    if (!seen) {
      deduped.push(k);
    }
  }
  deduped.sort();
  const parts: string[] = [targetNodeId, capability, mode];
  for (let i = 0; i < deduped.length; i++) {
    parts.push(deduped[i]);
  }
  return parts.join('|');
}

/**
 * group proposal key：`group|<canonicalGroupKey>`。
 * 与 core/src/domain/types.ts 的 groupProposalKey 同口径。
 */
export function groupProposalKey(
  targetNodeId: string,
  capability: Capability,
  mode: GroupMode,
  memberLogicalKeys: string[],
): string {
  return `group|${canonicalGroupKey(targetNodeId, capability, mode, memberLogicalKeys)}`;
}

// ---------------------------------------------------------------------------
// ChangePlan / Readiness / Coverage（与 spec/domain/change-plan.ts 同口径）
// ---------------------------------------------------------------------------

export class ActionVerification {
  method: ActionVerificationMethod = ActionVerificationMethod.MANUAL_CONFIRMATION;
  status: ActionVerificationStatus = ActionVerificationStatus.NOT_REQUIRED;
  evidenceRefs: string[] = [];
  expectedFromNodeId: string | null = null;
  expectedToNodeId: string | null = null;
}

export enum ActionVerificationMethod {
  MANUAL_CONFIRMATION = 'manual_confirmation',
  FUTURE_OBSERVATION = 'future_observation',
  AUTHORITATIVE_SOURCE = 'authoritative_source',
}

export enum ActionVerificationStatus {
  NOT_REQUIRED = 'not_required',
  PENDING = 'pending',
  EVIDENCE_SUGGESTED = 'evidence_suggested',
  VERIFIED = 'verified',
  FAILED = 'failed',
}

export class PlanAction {
  id: string = '';
  title: string = '';
  phase: PlanActionPhase = PlanActionPhase.PREPARE;
  done: boolean = false;
  resolvesImpactKeys: string[] = [];
  verification: ActionVerification | null = null;
}

export class ChangePlan {
  id: string = '';
  templateId: string | null = null;
  scenario: string = '';
  title: string = '';
  workflowState: ChangePlanWorkflowState = ChangePlanWorkflowState.DRAFT;
  baselineGraphRevision: number = 0;
  lastAnalyzedGraphRevision: number = 0;
  targetNodeId: string | null = null;
  effectiveDate: string | null = null;
  actions: PlanAction[] = [];
}

export class PlanReadinessInput {
  plan: ChangePlan = new ChangePlan();
  currentGraphRevision: number = 0;
  pendingMustChange: number = 0;
  pendingNeedsReview: number = 0;
  unresolvedCandidates: number = 0;
  pendingRelevantProposals: number = 0;
  staleRelevantDependencies: number = 0;
  unfinishedChangeActions: number = 0;
}

export class CoverageSourceInfo {
  id: string = '';
  label: string = '';
  lastIngestedAt: string | null = null;
}

export class ScenarioCoverageInput {
  scenarioId: string = '';
  sources: CoverageSourceInfo[] = [];
  confirmedDirectDependencies: number = 0;
  confirmedIndirectDependencies: number = 0;
  pendingProposals: number = 0;
  unresolvedCandidates: number = 0;
  staleDependencies: number = 0;
  unknownCriticalityCount: number = 0;
  unverifiedActions: number = 0;
  freshnessThresholdDays: number = 0;
  now: string = '';
}

export class ScenarioCoverage {
  scenarioId: string = '';
  coverageLevel: CoverageLevel = CoverageLevel.UNKNOWN;
  explanations: string[] = [];
  counts: CoverageCounts = new CoverageCounts();
}

export class CoverageCounts {
  confirmedDirectDependencies: number = 0;
  confirmedIndirectDependencies: number = 0;
  pendingProposals: number = 0;
  unresolvedCandidates: number = 0;
  staleDependencies: number = 0;
  unknownCriticalityCount: number = 0;
  unverifiedActions: number = 0;
}
