// RecoveryCycle —— Canonical vNext (v0.3.0) 确定性引擎（Swift 移植）。
//
// 源头：core/src/impact/recovery-cycle.ts（TypeScript reference，逐行忠实移植）。
//
// 铁律：
// - 只有 Confirmed Reality 上的 active 边才能产生 confirmed_cycle（V030-RC-01）。
// - Proposal / Candidate 最多产生 potential_cycle。
// - retired 边不参与（如同 Impact kernel 的 retired 不传播）。
// - capability 分开检测（mixed capability 不产生 confirmed cycle）。

import Foundation

public struct RecoveryHintedEdge: Equatable, Sendable {
    public let from: String
    public let to: String

    public init(from: String, to: String) {
        self.from = from
        self.to = to
    }
}

public struct RecoveryCycleInput: Equatable, Sendable {
    public let capability: Capability
    /// 已确认 Reality 的 active 边（同一 capability）。
    public let dependencies: [Dependency]
    /// pending Proposal / DiscoveryCandidate 暗示的边（同一 capability）。
    public let hintedEdges: [RecoveryHintedEdge]
    /// 检测上限，防止超长路径爆栈。
    public let maxPathLength: Int?

    public init(
        capability: Capability,
        dependencies: [Dependency],
        hintedEdges: [RecoveryHintedEdge],
        maxPathLength: Int? = nil
    ) {
        self.capability = capability
        self.dependencies = dependencies
        self.hintedEdges = hintedEdges
        self.maxPathLength = maxPathLength
    }
}

public struct RecoveryCycleResult: Equatable, Sendable {
    public let status: RecoveryCycleStatus
    /// confirmed 环的节点路径（若有；确定性排序）。
    public let confirmedCycles: [[String]]
    /// 仅由 proposal/candidate 支撑的潜在环（用户需确认）。
    public let potentialCycles: [[String]]
    /// 参与检测的 capability。
    public let capability: Capability

    public init(
        status: RecoveryCycleStatus,
        confirmedCycles: [[String]],
        potentialCycles: [[String]],
        capability: Capability
    ) {
        self.status = status
        self.confirmedCycles = confirmedCycles
        self.potentialCycles = potentialCycles
        self.capability = capability
    }
}

public enum RecoveryCycleEngine {
    private static let defaultMaxPathLength = 16

    /// 在「边集」内找有向环。为确定性输出：环被归一化为
    /// 「字典序最小的旋转 + 首节点最小」后去重排序。
    private static func findCycles(
        _ edges: [(from: String, to: String)],
        maxLen: Int
    ) -> [[String]] {
        var adj: [String: [String]] = [:]
        for e in edges {
            var list = adj[e.from] ?? []
            list.append(e.to)
            adj[e.from] = list
        }
        // 确定性遍历顺序：每条邻接表排序
        for k in adj.keys {
            adj[k]?.sort()
        }

        var cycles = Set<String>()
        let nodes = adj.keys.sorted()

        for start in nodes {
            var stack: [String] = [start]
            var visited = Set<String>([start])
            walk(
                start,
                adj: adj,
                startNode: start,
                maxLen: maxLen,
                stack: &stack,
                visited: &visited,
                cycles: &cycles
            )
        }

        return cycles
            .map { $0.split(separator: "|").map { String($0) } }
            .sorted(by: cycleLessThan)
    }

    private static func walk(
        _ current: String,
        adj: [String: [String]],
        startNode: String,
        maxLen: Int,
        stack: inout [String],
        visited: inout Set<String>,
        cycles: inout Set<String>
    ) {
        for next in adj[current] ?? [] {
            if next == startNode {
                // 环闭合：栈长必须 >= 2（自环无意义）且 <= maxLen
                if stack.count >= 2 && stack.count <= maxLen {
                    cycles.insert(normalizeCycle(stack))
                }
                continue
            }
            if visited.contains(next) { continue }
            if stack.count >= maxLen { continue }
            visited.insert(next)
            stack.append(next)
            walk(next, adj: adj, startNode: startNode, maxLen: maxLen, stack: &stack, visited: &visited, cycles: &cycles)
            stack.removeLast()
            visited.remove(next)
        }
    }

    /// 环归一化：找字典序最小的旋转（并要求该旋转首节点最小）。
    private static func normalizeCycle(_ path: [String]) -> String {
        var rotations: [String] = []
        for i in 0..<path.count {
            let rotated = Array(path[i...]) + Array(path[..<i])
            rotations.append(rotated.joined(separator: "|"))
        }
        return rotations.min() ?? path.joined(separator: "|")
    }

    /// 与 TS `sort((a, b) => …)` 等价的确定性环排序：
    /// 逐元素字典序；前缀短者在前。
    private static func cycleLessThan(_ a: [String], _ b: [String]) -> Bool {
        let n = min(a.count, b.count)
        for i in 0..<n {
            let la = a[i]
            let lb = b[i]
            if la != lb { return la < lb }
        }
        return a.count < b.count
    }

    public static func detectRecoveryCycles(_ input: RecoveryCycleInput) -> RecoveryCycleResult {
        let maxLen = input.maxPathLength ?? defaultMaxPathLength

        let confirmedEdges: [(from: String, to: String)] = input.dependencies
            .filter { $0.capability == input.capability && $0.state == .active }
            .map { (from: $0.from, to: $0.to) }
        // 自环无意义（恢复自依赖需要另一跳才成环）
        let hinted = input.hintedEdges.filter { $0.from != $0.to }

        let confirmedCycles = findCycles(confirmedEdges, maxLen: maxLen)
        if !confirmedCycles.isEmpty {
            return RecoveryCycleResult(
                status: .confirmedCycle,
                confirmedCycles: confirmedCycles,
                potentialCycles: [],
                capability: input.capability
            )
        }

        // 潜在环：confirmed 边 + hinted 边 的并集上存在环，且该环必须用到至少一条 hinted 边，
        // 否则它就是 confirmed 环（上一分支会抓住）。
        let merged = confirmedEdges + hinted.map { (from: $0.from, to: $0.to) }
        let allCycles = findCycles(merged, maxLen: maxLen)
        let potentialCycles = allCycles.filter { cycle in
            for i in 0..<cycle.count {
                let from = cycle[i]
                let to = cycle[(i + 1) % cycle.count]
                if hinted.contains(where: { $0.from == from && $0.to == to }) { return true }
            }
            return false
        }

        if !potentialCycles.isEmpty {
            return RecoveryCycleResult(
                status: .potentialCycle,
                confirmedCycles: [],
                potentialCycles: potentialCycles,
                capability: input.capability
            )
        }
        return RecoveryCycleResult(
            status: .noCycle,
            confirmedCycles: [],
            potentialCycles: [],
            capability: input.capability
        )
    }
}
