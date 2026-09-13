import {
  type ActionVerification,
  type ChangePlan,
  type ChangePlanWorkflowState,
  type PlanAction,
  type PlanReadinessInput,
} from '../domain/change-plan.ts'
import { ChangePlanRepository } from '../repositories/change-plan-repository.ts'
import { DependencyRepository } from '../repositories/dependency-repository.ts'
import { DependencyGroupRepository } from '../repositories/group-repository.ts'
import { DependencyProposalRepository } from '../repositories/proposal-repository.ts'
import { DiscoveryCandidateRepository } from '../repositories/discovery-candidate-repository.ts'
import { getGraphRevision } from '../repositories/graph-revision.ts'
import { analyzePlanImpact } from './plan-analysis.ts'
import { computePlanReadiness } from './plan-readiness.ts'
import type { ImpactGraphRepos } from './graph-view.ts'
import type { SqliteDriver } from '../db/driver.ts'
import { nowIso } from '../utils/ids.ts'

/**
 * ChangePlan 应用服务（MVP03 §12–§18 + §52–§54）。
 * UI 只能通过本服务操作计划；readiness 由纯规则引擎计算，UI 不得自行实现。
 */

/** 合法工作流迁移表（终态：completed / cancelled）。 */
const ALLOWED_TRANSITIONS: Record<ChangePlanWorkflowState, ChangePlanWorkflowState[]> = {
  draft: ['analyzed', 'review_required', 'ready', 'in_progress', 'cancelled'],
  analyzed: ['review_required', 'ready', 'in_progress', 'cancelled'],
  review_required: ['ready', 'in_progress', 'cancelled', 'analyzed'],
  ready: ['in_progress', 'cancelled', 'review_required'],
  in_progress: ['verifying', 'completed', 'cancelled'],
  verifying: ['completed', 'cancelled', 'in_progress'],
  completed: [],
  cancelled: [],
}

export class ChangePlanService {
  private readonly driver: SqliteDriver
  readonly plans: ChangePlanRepository
  private readonly deps: DependencyRepository
  private readonly groups: DependencyGroupRepository
  private readonly proposals: DependencyProposalRepository
  private readonly candidates: DiscoveryCandidateRepository

  constructor(driver: SqliteDriver) {
    this.driver = driver
    this.plans = new ChangePlanRepository(driver)
    this.deps = new DependencyRepository(driver)
    this.groups = new DependencyGroupRepository(driver)
    this.proposals = new DependencyProposalRepository(driver)
    this.candidates = new DiscoveryCandidateRepository(driver)
  }

  private graphRepos(): ImpactGraphRepos {
    return { deps: this.deps, groups: this.groups, proposals: this.proposals }
  }

  /** 装配 readiness 输入（relevance 口径：与计划目标节点直接相关的图对象）。 */
  assembleReadinessInput(plan: ChangePlan, staleDependencyDays = 90): PlanReadinessInput {
    const currentRevision = getGraphRevision(this.driver)
    const snapshot = analyzePlanImpact(this.driver, this.graphRepos(), plan)
    const pendingMustChange = snapshot.targets.filter((t) => t.status === 'must_change').length
    const pendingNeedsReview = snapshot.targets.filter((t) => t.status === 'needs_review').length

    const target = plan.targetNodeId
    const relevantProposals =
      target === null
        ? 0
        : this.proposals
            .listAll()
            .filter((p) => p.decision === 'pending' && (p.to === target || p.from === target))
            .length
    const unresolvedCandidates = this.candidates.listByStatus('pending').length

    const thresholdMs = staleDependencyDays * 86_400_000
    const now = Date.now()
    const staleRelevantDependencies =
      target === null
        ? 0
        : this.deps
            .listAll()
            .filter(
              (d) =>
                d.state === 'active' &&
                (d.from === target || d.to === target) &&
                now - Date.parse(d.lastVerifiedAt) > thresholdMs,
            ).length

    const unfinishedChangeActions = plan.actions.filter((a) => a.phase === 'change' && !a.done)
      .length

    return {
      plan,
      currentGraphRevision: currentRevision,
      pendingMustChange,
      pendingNeedsReview,
      unresolvedCandidates,
      pendingRelevantProposals: relevantProposals,
      staleRelevantDependencies,
      unfinishedChangeActions,
    }
  }

  /** 当前 readiness（stale 计划天然 review_required，先 rebase 才可能 ready）。 */
  getReadiness(planId: string) {
    const plan = this.plans.getExisting(planId)
    return computePlanReadiness(this.assembleReadinessInput(plan))
  }

  /** 工作流迁移（终态不可迁出；非法迁移抛错）。 */
  transition(planId: string, next: ChangePlanWorkflowState): ChangePlan {
    const plan = this.plans.getExisting(planId)
    if (!ALLOWED_TRANSITIONS[plan.workflowState].includes(next)) {
      throw new Error(
        `illegal change plan transition: ${plan.workflowState} -> ${next} (plan ${plan.id})`,
      )
    }
    return this.plans.updateWorkflowState(planId, next)
  }

  // ---------------------------------------------------------------------------
  // Action / Verification（§52–§54：done ≠ verified）
  // ---------------------------------------------------------------------------

  /** 完成动作：只置 done，绝不动 verification 状态。 */
  completeAction(planId: string, actionId: string): ChangePlan {
    const plan = this.plans.getExisting(planId)
    if (plan.workflowState === 'completed' || plan.workflowState === 'cancelled') {
      throw new Error(`plan ${planId} is ${plan.workflowState}; actions are frozen`)
    }
    const actions = plan.actions.map((a) =>
      a.id === actionId ? { ...a, done: true, doneAt: nowIso() } : a,
    )
    if (!actions.some((a) => a.id === actionId)) {
      throw new Error(`action not found: ${actionId} (plan ${planId})`)
    }
    return this.plans.updateActions(planId, actions)
  }

  /** 手动验证（user_confirmed）：verification.status → verified。 */
  verifyActionManually(planId: string, actionId: string): ChangePlan {
    const plan = this.plans.getExisting(planId)
    const actions = plan.actions.map((a) =>
      a.id === actionId
        ? {
            ...a,
            verification: {
              method: 'manual_confirmation' as const,
              status: 'verified' as const,
              verifiedAt: nowIso(),
              evidenceRefs: a.verification?.evidenceRefs ?? [],
            },
          }
        : a,
    )
    if (!actions.some((a) => a.id === actionId)) {
      throw new Error(`action not found: ${actionId} (plan ${planId})`)
    }
    return this.plans.updateActions(planId, actions)
  }

  /**
   * future_observation 证据建议：新 Evidence 表明某 change 动作可能已生效。
   * 只置 evidence_suggested —— **绝不修改 Reality、绝不自动 verified**（VF 铁律）。
   * 幂等：已是 verified / evidence_suggested 的动作不重复处理。
   */
  suggestVerificationFromEvidence(
    planId: string,
    actionId: string,
    evidenceRef: string,
  ): ChangePlan {
    const plan = this.plans.getExisting(planId)
    const actions = plan.actions.map((a): PlanAction => {
      if (a.id !== actionId) return a
      const verification: ActionVerification = a.verification ?? {
        method: 'future_observation',
        status: 'pending',
        verifiedAt: null,
        evidenceRefs: [],
      }
      if (verification.status === 'verified') return a
      const refs = verification.evidenceRefs.includes(evidenceRef)
        ? verification.evidenceRefs
        : [...verification.evidenceRefs, evidenceRef]
      return {
        ...a,
        verification: {
          ...verification,
          method:
            verification.method === 'manual_confirmation' ? verification.method : 'future_observation',
          status: 'evidence_suggested',
          evidenceRefs: refs,
        },
      }
    })
    if (!actions.some((a) => a.id === actionId)) {
      throw new Error(`action not found: ${actionId} (plan ${planId})`)
    }
    return this.plans.updateActions(planId, actions)
  }
}

export { computePlanReadiness }
export type { PlanAction }
