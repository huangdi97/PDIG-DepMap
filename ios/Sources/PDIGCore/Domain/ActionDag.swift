// ActionDag —— Canonical vNext (v0.3.0) 确定性引擎（Swift 移植）。
//
// 源头：core/src/domain/action-dag.ts（TypeScript reference，逐行忠实移植）。
//
// 硬性要求：
// - DAG deterministic + stable topological order（V030-DAG-03）
// - cycle 必须被拒绝（V030-DAG-01）
// - missing prerequisite / unknown action id 必须被拒绝（V030-DAG-02）
// UI 不得暴露 DAG / topological sort 术语 —— 用户只看到「必须先完成 / 完成后才能继续」。

import Foundation

/// ChangePlan 动作的最小投影：DAG 校验只需要 id 与前置引用。
public struct DagAction: Equatable, Sendable {
    public let id: String
    public let prerequisiteActionIds: [String]

    public init(id: String, prerequisiteActionIds: [String] = []) {
        self.id = id
        self.prerequisiteActionIds = prerequisiteActionIds
    }
}

public struct ActionDagValidationResult: Equatable, Sendable {
    public let ok: Bool
    public let violation: ActionDagViolation
    /// 违反的 action id（cycle 时为环上的 action；missing 时为引用方）。
    public let offendingActionId: String?
    /// 稳定的拓扑顺序（相同输入永远相同输出）。
    public let topologicalOrder: [String]

    public init(
        ok: Bool,
        violation: ActionDagViolation,
        offendingActionId: String?,
        topologicalOrder: [String]
    ) {
        self.ok = ok
        self.violation = violation
        self.offendingActionId = offendingActionId
        self.topologicalOrder = topologicalOrder
    }
}

public enum ActionDagEngine {
    /// 稳定拓扑排序：Kahn + 确定性小根堆（按 actionId 字典序）。
    public static func stableTopologicalOrder(
        actionIds: [String],
        edges: [String: [String]]
    ) -> [String] {
        var indegree: [String: Int] = [:]
        for id in actionIds { indegree[id] = 0 }
        for (_, deps) in edges {
            for dep in deps {
                indegree[dep, default: 0] += 1
            }
        }
        // 小根堆：始终取字典序最小的零入度节点 → 输出确定性
        var heap = actionIds.filter { indegree[$0] == 0 }
        heap.sort()
        var order: [String] = []
        while !heap.isEmpty {
            let node = heap.removeFirst()
            order.append(node)
            for dep in edges[node] ?? [] {
                let next = (indegree[dep] ?? 0) - 1
                indegree[dep] = next
                if next == 0 { heap.append(dep) }
            }
            heap.sort()
        }
        return order
    }

    public static func validateActionDag(actions: [DagAction]) -> ActionDagValidationResult {
        let ids = actions.map { $0.id }
        let idSet = Set(ids)

        // 1. unknown prerequisite → missing_prerequisite
        for a in actions {
            for prereq in a.prerequisiteActionIds where !idSet.contains(prereq) {
                return ActionDagValidationResult(
                    ok: false,
                    violation: .missingPrerequisite,
                    offendingActionId: a.id,
                    topologicalOrder: []
                )
            }
        }

        // 2. 建边：action → 依赖它的动作（prerequisite 必须先完成）
        var edges: [String: [String]] = [:]
        for a in actions {
            for prereq in a.prerequisiteActionIds {
                var list = edges[prereq] ?? []
                list.append(a.id)
                edges[prereq] = list
            }
        }

        let order = stableTopologicalOrder(actionIds: ids, edges: edges)
        if order.count != ids.count {
            // 存在环：找出环上的一个 action 作为 offendingActionId
            let orderedSet = Set(order)
            let inCycle = ids.first { !orderedSet.contains($0) }
            return ActionDagValidationResult(
                ok: false,
                violation: .cycle,
                offendingActionId: inCycle,
                topologicalOrder: []
            )
        }

        return ActionDagValidationResult(
            ok: true,
            violation: .none,
            offendingActionId: nil,
            topologicalOrder: order
        )
    }
}
