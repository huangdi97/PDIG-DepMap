import type { Capability } from './types.ts'

/**
 * MVP03 ChangePlan / Readiness / Coverage / Verification 领域类型。
 *
 * 语义铁律：
 * - ChangePlan 是「用户确认过的变更意图 + 影响快照」，不是 Reality 本身。
 * - Action done ≠ verified（VF 铁律）。
 * - PlanReadiness 只有 blocked / review_required / ready_with_known_scope，
 *   不存在 safe / 100% / all clear。
 * - ScenarioCoverage 是信息覆盖描述，不是安全概率。
 */

export type ChangePlanWorkflowState =
  | 'draft'
  | 'analyzed'
  | 'review_required'
  | 'ready'
  | 'in_progress'
  | 'verifying'
  | 'completed'
  | 'cancelled'

/**
 * effectiveStatus = stored workflowState + 派生覆盖。
 * lastAnalyzedGraphRevision < 当前 graphRevision 且计划未 completed/cancelled
 * → 派生 `needs_revalidation`（不批量回写存储状态，避免每次 Reality 变更扫描全部计划）。
 */
export type PlanEffectiveStatus = ChangePlanWorkflowState | 'needs_revalidation'

export type PlanActionPhase = 'prepare' | 'change' | 'verify'

export type ActionVerificationMethod =
  'manual_confirmation' | 'future_observation' | 'authoritative_source'

export type ActionVerificationStatus =
  'not_required' | 'pending' | 'evidence_suggested' | 'verified' | 'failed'

export interface ActionVerification {
  method: ActionVerificationMethod
  status: ActionVerificationStatus
  verifiedAt: string | null
  /** 支撑 verification 的 evidence 引用（只存引用，不复制 Evidence 内容）。 */
  evidenceRefs: string[]
  /**
   * future_observation 的匹配目标（可选）：证据路径 `expectedFrom → expectedTo`
   * 命中才允许 evidence_suggested（错误来源绝不产生 suggestion，VF-006）。
   */
  expectedFromNodeId?: string | undefined
  expectedToNodeId?: string | undefined
}

export interface PlanAction {
  id: string
  title: string
  detail: string
  phase: PlanActionPhase
  done: boolean
  doneAt: string | null
  verification: ActionVerification | null
  /**
   * 该动作显式解决的影响 key（`nodeId|capability`，与 ImpactSnapshot.targets 对应）。
   * Freeze 语义（GOAL §5–§7）：must_change requirement 只有被「声明的全部 change 动作
   * 都完成」才 resolved；禁止用 target 数量 − 动作数量的减法近似。
   */
  resolvesImpactKeys?: string[] | undefined
  /**
   * v0.3.0 (Canonical vNext)：前置动作 id 列表。必须先完成的动作。
   * 由 ActionDag 校验（cycle reject / missing prerequisite reject / stable topo order）。
   */
  prerequisiteActionIds?: string[] | undefined
}

/** 影响快照（创建/每次 rebase 时固定；用于 old vs new 差异）。 */
export interface ImpactSnapshot {
  unavailable: { nodeId: string; capability: Capability }[]
  /** (nodeId|capability|status|reasonCode) 有序集合，deterministic。 */
  targets: Array<{ nodeId: string; capability: Capability; status: string; reasonCode: string }>
  checklistLevels: Array<{ level: string; count: number }>
}

export interface ChangePlan {
  id: string
  templateId: string | null
  scenario: string
  title: string
  workflowState: ChangePlanWorkflowState
  baselineGraphRevision: number
  lastAnalyzedGraphRevision: number
  targetNodeId: string | null
  effectiveDate: string | null
  params: Record<string, string>
  impactSnapshot: ImpactSnapshot | null
  actions: PlanAction[]
  createdAt: string
  updatedAt: string
}

/** 便捷包装：stale 判定（UI 与 readiness 共用同一口径）。 */
export function isPlanStale(plan: ChangePlan, currentGraphRevision: number): boolean {
  return (
    plan.workflowState !== 'completed' &&
    plan.workflowState !== 'cancelled' &&
    currentGraphRevision > plan.lastAnalyzedGraphRevision
  )
}

/** effectiveStatus：纯派生函数（§13 不回写存储）。 */
export function effectiveStatus(
  plan: ChangePlan,
  currentGraphRevision: number,
): PlanEffectiveStatus {
  if (isPlanStale(plan, currentGraphRevision)) return 'needs_revalidation'
  return plan.workflowState
}

// ---------------------------------------------------------------------------
// PlanRebase 差异模型（§14）
// ---------------------------------------------------------------------------

export interface ImpactDiffItem {
  nodeId: string
  capability: Capability
  status: string
  reasonCode: string
}

export interface ActionDiffItem {
  actionId: string
  title: string
}

export interface PlanRebaseDiff {
  addedImpacts: ImpactDiffItem[]
  removedImpacts: ImpactDiffItem[]
  changedImpacts: ImpactDiffItem[]
  addedActions: ActionDiffItem[]
  removedActions: ActionDiffItem[]
  changedActions: ActionDiffItem[]
}

export interface PlanRebaseResult {
  plan: ChangePlan
  /** revision 未变 → 不做 rebase（PRB-001）。 */
  revisionChanged: boolean
  diff: PlanRebaseDiff
}

// ---------------------------------------------------------------------------
// PlanReadiness（§17–§19，纯规则，无 LLM / 无 confidence 映射）
// ---------------------------------------------------------------------------

export type PlanReadiness = 'blocked' | 'review_required' | 'ready_with_known_scope'

export interface PlanReadinessInput {
  plan: ChangePlan
  currentGraphRevision: number
  /** 当前（rebase 后）影响模拟中未处理的 must_change 数。 */
  pendingMustChange: number
  /** 当前影响中的 needs_review 目标数（含 unknown criticality）。 */
  pendingNeedsReview: number
  unresolvedCandidates: number
  pendingRelevantProposals: number
  staleRelevantDependencies: number
  /** 未完成的 change 阶段动作数（verify 阶段不阻塞 readiness）。 */
  unfinishedChangeActions: number
}

// ---------------------------------------------------------------------------
// ScenarioCoverage（§21–§23，信息覆盖，不是安全评分）
// ---------------------------------------------------------------------------

export type CoverageLevel = 'unknown' | 'limited' | 'partial' | 'well_evidenced'

export interface CoverageSourceInfo {
  id: string
  label: string
  /** null = 从未导入过（最陈旧）。 */
  lastIngestedAt: string | null
}

export interface ScenarioCoverageInput {
  scenarioId: string
  sources: CoverageSourceInfo[]
  confirmedDirectDependencies: number
  confirmedIndirectDependencies: number
  pendingProposals: number
  unresolvedCandidates: number
  staleDependencies: number
  unknownCriticalityCount: number
  unverifiedActions: number
  freshnessThresholdDays: number
  now: string
}

export interface ScenarioCoverage {
  scenarioId: string
  coverageLevel: CoverageLevel
  /** 可解释：为什么是当前级别（§23）。 */
  explanations: string[]
  counts: {
    confirmedDirectDependencies: number
    confirmedIndirectDependencies: number
    pendingProposals: number
    unresolvedCandidates: number
    staleDependencies: number
    unknownCriticalityCount: number
    unverifiedActions: number
  }
}
