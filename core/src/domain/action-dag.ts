import type { PlanAction } from './change-plan.ts'

/**
 * Action DAG — Canonical vNext (v0.3.0)
 *
 * ChangePlan 的动作之间可以有 prerequisite（prerequisiteActionIds[]）。
 * 硬性要求：
 * - DAG deterministic + stable topological order（V030-DAG-03）
 * - cycle 必须被拒绝（V030-DAG-01）
 * - missing prerequisite / unknown action id 必须被拒绝（V030-DAG-02）
 * UI 不得暴露 DAG / topological sort 术语 —— 用户只看到「必须先完成 / 完成后才能继续」。
 */

export type ActionDagViolation = 'cycle' | 'missing_prerequisite' | 'unknown_action' | 'none'

export interface ActionDagValidationResult {
  ok: boolean
  violation: ActionDagViolation
  /** 违反的 action id（cycle 时为环上的 action；missing 时为引用方）。 */
  offendingActionId: string | null
  /** 稳定的拓扑顺序（相同输入永远相同输出）。 */
  topologicalOrder: string[]
}

export interface ActionDagInput {
  actions: PlanAction[]
}

/** 稳定拓扑排序：Kahn + 确定性小根堆（按 actionId 字典序）。 */
export function stableTopologicalOrder(
  actionIds: string[],
  edges: Map<string, string[]>,
): string[] {
  const indegree = new Map<string, number>()
  for (const id of actionIds) indegree.set(id, 0)
  for (const [, deps] of edges) {
    for (const dep of deps) {
      indegree.set(dep, (indegree.get(dep) ?? 0) + 1)
    }
  }
  // 小根堆：始终取字典序最小的零入度节点 → 输出确定性
  const heap: string[] = actionIds.filter((id) => indegree.get(id) === 0)
  const heapify = (): void => {
    heap.sort((a, b) => (a < b ? -1 : 1))
  }
  heapify()
  const order: string[] = []
  while (heap.length > 0) {
    const node = heap.shift() as string
    order.push(node)
    for (const dep of edges.get(node) ?? []) {
      const next = (indegree.get(dep) ?? 0) - 1
      indegree.set(dep, next)
      if (next === 0) heap.push(dep)
    }
    heapify()
  }
  return order
}

export function validateActionDag(input: ActionDagInput): ActionDagValidationResult {
  const ids = input.actions.map((a) => a.id)
  const idSet = new Set(ids)

  // 1. unknown prerequisite → missing_prerequisite / unknown_action
  for (const a of input.actions) {
    for (const prereq of a.prerequisiteActionIds ?? []) {
      if (!idSet.has(prereq)) {
        return {
          ok: false,
          violation: 'missing_prerequisite',
          offendingActionId: a.id,
          topologicalOrder: [],
        }
      }
    }
  }

  // 2. 建边：action → 依赖它的动作（prerequisite 必须先完成）
  const edges = new Map<string, string[]>()
  for (const a of input.actions) {
    for (const prereq of a.prerequisiteActionIds ?? []) {
      const list = edges.get(prereq) ?? []
      list.push(a.id)
      edges.set(prereq, list)
    }
  }

  const order = stableTopologicalOrder(ids, edges)
  if (order.length !== ids.length) {
    // 存在环：找出环上的一个 action 作为 offendingActionId
    const orderedSet = new Set(order)
    const inCycle = ids.find((id) => !orderedSet.has(id)) ?? null
    return { ok: false, violation: 'cycle', offendingActionId: inCycle, topologicalOrder: [] }
  }

  return { ok: true, violation: 'none', offendingActionId: null, topologicalOrder: order }
}
