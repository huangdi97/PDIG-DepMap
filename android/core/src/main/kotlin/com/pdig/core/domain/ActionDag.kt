package com.pdig.core.domain

import com.pdig.core.generated.ActionDagViolation

/**
 * Action DAG — Canonical v0.3.0（port of core/src/domain/action-dag.ts）。
 *
 * ChangePlan 的动作之间可以有 prerequisite（prerequisiteActionIds[]）。
 * 硬性要求：
 * - DAG deterministic + stable topological order（V030-DAG-03）
 * - cycle 必须被拒绝（V030-DAG-01）
 * - missing prerequisite / unknown action id 必须被拒绝（V030-DAG-02）
 * UI 不得暴露 DAG / topological sort 术语 —— 用户只看到「必须先完成 / 完成后才能继续」。
 */

data class DagAction(
    val id: String,
    val prerequisiteActionIds: List<String> = emptyList(),
)

data class ActionDagInput(val actions: List<DagAction>)

data class ActionDagValidationResult(
    val ok: Boolean,
    val violation: ActionDagViolation,
    /** 违反的 action id（cycle 时为环上的 action；missing 时为引用方）。 */
    val offendingActionId: String?,
    /** 稳定的拓扑顺序（相同输入永远相同输出）。 */
    val topologicalOrder: List<String>,
)

/** 稳定拓扑排序：Kahn + 确定性小根堆（按 actionId 字典序）。 */
fun stableTopologicalOrder(
    actionIds: List<String>,
    edges: Map<String, List<String>>,
): List<String> {
    val indegree = mutableMapOf<String, Int>()
    for (id in actionIds) indegree[id] = 0
    for ((_, deps) in edges) {
        for (dep in deps) {
            indegree[dep] = (indegree[dep] ?: 0) + 1
        }
    }
    // 小根堆：始终取字典序最小的零入度节点 → 输出确定性
    val heap = actionIds.filter { indegree[it] == 0 }.toMutableList()
    heap.sort()
    val order = mutableListOf<String>()
    while (heap.isNotEmpty()) {
        val node = heap.removeAt(0)
        order.add(node)
        for (dep in edges[node] ?: emptyList()) {
            val next = (indegree[dep] ?: 0) - 1
            indegree[dep] = next
            if (next == 0) heap.add(dep)
        }
        heap.sort()
    }
    return order
}

fun validateActionDag(input: ActionDagInput): ActionDagValidationResult {
    val ids = input.actions.map { it.id }
    val idSet = ids.toSet()

    // 1. unknown prerequisite → missing_prerequisite / unknown_action
    for (a in input.actions) {
        for (prereq in a.prerequisiteActionIds) {
            if (!idSet.contains(prereq)) {
                return ActionDagValidationResult(
                    ok = false,
                    violation = ActionDagViolation.MISSING_PREREQUISITE,
                    offendingActionId = a.id,
                    topologicalOrder = emptyList(),
                )
            }
        }
    }

    // 2. 建边：action → 依赖它的动作（prerequisite 必须先完成）
    val edges = mutableMapOf<String, MutableList<String>>()
    for (a in input.actions) {
        for (prereq in a.prerequisiteActionIds) {
            edges.getOrPut(prereq) { mutableListOf() }.add(a.id)
        }
    }

    val order = stableTopologicalOrder(ids, edges)
    if (order.size != ids.size) {
        // 存在环：找出环上的一个 action 作为 offendingActionId
        val orderedSet = order.toSet()
        val inCycle = ids.find { !orderedSet.contains(it) }
        return ActionDagValidationResult(
            ok = false,
            violation = ActionDagViolation.CYCLE,
            offendingActionId = inCycle,
            topologicalOrder = emptyList(),
        )
    }

    return ActionDagValidationResult(
        ok = true,
        violation = ActionDagViolation.NONE,
        offendingActionId = null,
        topologicalOrder = order,
    )
}
