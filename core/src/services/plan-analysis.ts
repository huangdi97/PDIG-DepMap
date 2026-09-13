import {
  effectiveStatus,
  isPlanStale,
  type ChangePlan,
  type ImpactDiffItem,
  type ImpactSnapshot,
  type PlanAction,
  type PlanRebaseDiff,
  type PlanRebaseResult,
} from '../domain/change-plan.ts'
import { simulateDisable, type ImpactResult } from '../impact/kernel.ts'
import type { ChangePlanRepository } from '../repositories/change-plan-repository.ts'
import { getGraphRevision } from '../repositories/graph-revision.ts'
import type { SqliteDriver } from '../db/driver.ts'
import { buildImpactGraph, type ImpactGraphRepos } from './graph-view.ts'

/**
 * Plan 分析与 Rebase（MVP03 §12–§16）。
 *
 * - 快照固定为 (nodeId, capability, status, reasonCode) 有序集合 → deterministic diff。
 * - rebase 不自动完成任何 Action（PRB-008）：按 actionId 合并，保留 done/verification。
 * - completed / cancelled 是终态：不 rebase、不改写历史（PRB-009/010）。
 * - revision 未变 → 不 rebase（PRB-001）；effectiveStatus 派生 needs_revalidation（§13）。
 */

const IMPACT_ORDER: Record<string, number> = {
  must_change: 3,
  backup_path: 2,
  degraded: 2,
  needs_review: 1,
  unaffected: 0,
}

/** ImpactResult → 固定快照（targets 按 (nodeId, capability) 排序；仅非 unaffected）。 */
export function computeImpactSnapshot(result: ImpactResult): ImpactSnapshot {
  const targets = result.targets
    .filter((t) => t.status !== 'unaffected')
    .map((t) => ({
      nodeId: t.nodeId,
      capability: t.capability,
      status: t.status,
      reasonCode: t.reasonCode,
    }))
    .sort((a, b) =>
      a.nodeId !== b.nodeId
        ? a.nodeId < b.nodeId
          ? -1
          : 1
        : a.capability !== b.capability
          ? a.capability < b.capability
            ? -1
            : 1
          : (IMPACT_ORDER[a.status] ?? 0) !== (IMPACT_ORDER[b.status] ?? 0)
            ? (IMPACT_ORDER[a.status] ?? 0) - (IMPACT_ORDER[b.status] ?? 0)
            : a.reasonCode < b.reasonCode
              ? -1
              : 1,
    )
  const levelCounts = new Map<string, number>()
  for (const item of result.checklist) {
    levelCounts.set(item.level, (levelCounts.get(item.level) ?? 0) + 1)
  }
  const checklistLevels = [...levelCounts.entries()]
    .map(([level, count]) => ({ level, count }))
    .sort((a, b) => (a.level < b.level ? -1 : 1))
  return { unavailable: result.unavailable.map((k) => ({ ...k })), targets, checklistLevels }
}

function snapshotKey(item: ImpactDiffItem): string {
  return `${item.nodeId}|${item.capability}`
}

/** 差异：added（新出现）/ removed（消失）/ changed（同目标但 status/reasonCode 变化）。 */
export function diffImpactSnapshots(
  oldSnapshot: ImpactSnapshot,
  newSnapshot: ImpactSnapshot,
): Pick<PlanRebaseDiff, 'addedImpacts' | 'removedImpacts' | 'changedImpacts'> {
  const oldByKey = new Map<string, ImpactDiffItem>()
  for (const t of oldSnapshot.targets) oldByKey.set(snapshotKey(t), t)
  const newByKey = new Map<string, ImpactDiffItem>()
  for (const t of newSnapshot.targets) newByKey.set(snapshotKey(t), t)

  const addedImpacts: ImpactDiffItem[] = []
  const changedImpacts: ImpactDiffItem[] = []
  for (const [key, next] of newByKey) {
    const prev = oldByKey.get(key)
    if (!prev) {
      addedImpacts.push(next)
    } else if (prev.status !== next.status || prev.reasonCode !== next.reasonCode) {
      changedImpacts.push(next)
    }
  }
  const removedImpacts: ImpactDiffItem[] = []
  for (const [key, prev] of oldByKey) {
    if (!newByKey.has(key)) removedImpacts.push(prev)
  }
  const byImpact = (a: ImpactDiffItem, b: ImpactDiffItem): number =>
    a.nodeId !== b.nodeId ? (a.nodeId < b.nodeId ? -1 : 1) : a.capability < b.capability ? -1 : 1
  return {
    addedImpacts: [...addedImpacts].sort(byImpact),
    removedImpacts: [...removedImpacts].sort(byImpact),
    changedImpacts: [...changedImpacts].sort(byImpact),
  }
}

/** 按当前图重新模拟目标节点并产出快照。plan.targetNodeId 为空 → 空快照。 */
export function analyzePlanImpact(
  driver: SqliteDriver,
  repos: ImpactGraphRepos,
  plan: ChangePlan,
): ImpactSnapshot {
  if (!plan.targetNodeId) {
    return { unavailable: [], targets: [], checklistLevels: [] }
  }
  const graph = buildImpactGraph(driver, repos)
  return computeImpactSnapshot(simulateDisable(graph, plan.targetNodeId, 'payment'))
}

/**
 * Rebase：revision 未变 → no-op；revision 变了且计划非终态 → 重模拟 + 合并动作 +
 * 更新 lastAnalyzedGraphRevision（同一次调用内完成，PRB-007）。
 */
export function rebasePlan(
  driver: SqliteDriver,
  repos: ImpactGraphRepos & { plans: ChangePlanRepository },
  planId: string,
): PlanRebaseResult {
  const plan = repos.plans.getExisting(planId)
  const currentRevision = getGraphRevision(driver)

  if (plan.workflowState === 'completed' || plan.workflowState === 'cancelled') {
    // 终态计划保持历史（PRB-009/010）：不改写快照与动作，仅报告无差异
    return {
      plan,
      revisionChanged: false,
      diff: emptyDiff(),
    }
  }

  if (!isPlanStale(plan, currentRevision)) {
    return { plan, revisionChanged: false, diff: emptyDiff() }
  }

  const oldSnapshot = plan.impactSnapshot ?? { unavailable: [], targets: [], checklistLevels: [] }
  const newSnapshot = analyzePlanImpact(driver, repos, plan)
  const impacts = diffImpactSnapshots(oldSnapshot, newSnapshot)

  // 动作合并（PRB-008）：rebase 不新增/不完成任何动作，已有动作原样保留；
  // addedActions 恒为空（新增动作由模板/用户显式添加），removedActions 恒为空。
  const preservedActions: PlanAction[] = plan.actions.map((a) => ({ ...a }))

  const updated = repos.plans.updateAnalysis(
    plan.id,
    newSnapshot,
    currentRevision,
    preservedActions,
  )

  const diff: PlanRebaseDiff = {
    ...impacts,
    addedActions: [],
    removedActions: [],
    changedActions: [],
  }
  return { plan: updated, revisionChanged: true, diff }
}

function emptyDiff(): PlanRebaseDiff {
  return {
    addedImpacts: [],
    removedImpacts: [],
    changedImpacts: [],
    addedActions: [],
    removedActions: [],
    changedActions: [],
  }
}

/** effectiveStatus 便捷转发（UI / readiness 共用口径）。 */
export function planEffectiveStatus(
  plan: ChangePlan,
  currentGraphRevision: number,
): ReturnType<typeof effectiveStatus> {
  return effectiveStatus(plan, currentGraphRevision)
}
