// ActionDag — Canonical v0.3.0（port of core/src/domain/action-dag.ts）。
//
// ChangePlan 的动作之间可以有 prerequisite（prerequisiteActionIds[]）。
// 硬性要求：
// - DAG deterministic + stable topological order（V030-DAG-03）
// - cycle 必须被拒绝（V030-DAG-01）
// - missing prerequisite / unknown action id 必须被拒绝（V030-DAG-02）
// UI 不得暴露 DAG / topological sort 术语 —— 用户只看到「必须先完成 / 完成后才能继续」。
//
// 分层约束：本模块是 domain 层纯逻辑，**零 @ohos 依赖**。

import { ActionDagViolation } from '../generated/CanonicalEnums';

/** 参与 DAG 校验的轻量 action（只取 id 与 prerequisites）。 */
export class DagAction {
  id: string = '';
  prerequisiteActionIds: string[] = [];
}

/** validateActionDag 的输入。 */
export class ActionDagInput {
  actions: DagAction[] = [];
}

/** validateActionDag 的确定性结果。 */
export class ActionDagValidationResult {
  ok: boolean = false;
  violation: ActionDagViolation = ActionDagViolation.NONE;
  /** 违反的 action id（cycle 时为环上的 action；missing 时为引用方）。 */
  offendingActionId: string | null = null;
  /** 稳定的拓扑顺序（相同输入永远相同输出）。 */
  topologicalOrder: string[] = [];
}

/**
 * 稳定拓扑排序：Kahn + 确定性小根堆（按 actionId 字典序）。
 * 每轮取字典序最小的零入度节点 → 输出确定性。
 */
export function stableTopologicalOrder(
  actionIds: string[],
  edges: Map<string, string[]>,
): string[] {
  const indegree = new Map<string, number>();
  for (let i = 0; i < actionIds.length; i++) {
    indegree.set(actionIds[i], 0);
  }
  for (const subset of edges) {
    const deps = subset[1];
    for (let i = 0; i < deps.length; i++) {
      const prev = indegree.get(deps[i]);
      indegree.set(deps[i], (prev === undefined ? 0 : prev) + 1);
    }
  }
  // 小根堆：始终取字典序最小的零入度节点 → 输出确定性
  const heap: string[] = [];
  for (let i = 0; i < actionIds.length; i++) {
    if (indegree.get(actionIds[i]) === 0) {
      heap.push(actionIds[i]);
    }
  }
  heap.sort();
  const order: string[] = [];
  while (heap.length > 0) {
    const node = heap[0];
    heap.splice(0, 1);
    order.push(node);
    const deps = edges.get(node);
    if (deps !== undefined) {
      for (let i = 0; i < deps.length; i++) {
        const prev = indegree.get(deps[i]);
        const next = (prev === undefined ? 0 : prev) - 1;
        indegree.set(deps[i], next);
        if (next === 0) {
          heap.push(deps[i]);
        }
      }
    }
    heap.sort();
  }
  return order;
}

function containsId(ids: string[], id: string): boolean {
  for (let i = 0; i < ids.length; i++) {
    if (ids[i] === id) {
      return true;
    }
  }
  return false;
}

export function validateActionDag(input: ActionDagInput): ActionDagValidationResult {
  const ids: string[] = [];
  for (let i = 0; i < input.actions.length; i++) {
    ids.push(input.actions[i].id);
  }

  // 1. unknown prerequisite → missing_prerequisite / unknown_action
  for (let i = 0; i < input.actions.length; i++) {
    const a = input.actions[i];
    for (let j = 0; j < a.prerequisiteActionIds.length; j++) {
      if (!containsId(ids, a.prerequisiteActionIds[j])) {
        const result = new ActionDagValidationResult();
        result.ok = false;
        result.violation = ActionDagViolation.MISSING_PREREQUISITE;
        result.offendingActionId = a.id;
        result.topologicalOrder = [];
        return result;
      }
    }
  }

  // 2. 建边：action → 依赖它的动作（prerequisite 必须先完成）
  const edges = new Map<string, string[]>();
  for (let i = 0; i < input.actions.length; i++) {
    const a = input.actions[i];
    for (let j = 0; j < a.prerequisiteActionIds.length; j++) {
      const prereq = a.prerequisiteActionIds[j];
      const existing = edges.get(prereq);
      if (existing === undefined) {
        const list: string[] = [];
        list.push(a.id);
        edges.set(prereq, list);
      } else {
        existing.push(a.id);
      }
    }
  }

  const order = stableTopologicalOrder(ids, edges);
  if (order.length !== ids.length) {
    // 存在环：找出环上的一个 action 作为 offendingActionId
    const result = new ActionDagValidationResult();
    result.ok = false;
    result.violation = ActionDagViolation.CYCLE;
    let inCycle: string | null = null;
    for (let i = 0; i < ids.length; i++) {
      if (!containsId(order, ids[i])) {
        inCycle = ids[i];
        break;
      }
    }
    result.offendingActionId = inCycle;
    result.topologicalOrder = [];
    return result;
  }

  const ok = new ActionDagValidationResult();
  ok.ok = true;
  ok.violation = ActionDagViolation.NONE;
  ok.offendingActionId = null;
  ok.topologicalOrder = order;
  return ok;
}