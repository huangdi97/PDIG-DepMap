// FailureDomain —— Canonical vNext (v0.3.0) 确定性引擎（Swift 移植）。
//
// 源头：core/src/domain/failure-domain.ts（TypeScript reference，逐行忠实移植）。
//
// 铁律：
// - FailureDomain 本身服从 Reality Boundary：只有用户确认或权威证据才允许 status=confirmed；
//   机器推断最多产生 needs_review（V030-FD-01 / FD-CONFIRM-GATE）。
// - 两个恢复/认证路径共享同一故障域 → 不独立（V030-IP-01）。
// - pathCount ≠ independentPathCount：路径数多不代表独立路径数多。
//
// 确定性：结果字段全部排序；共享故障域取 union 后排序。

import Foundation

public struct PathIndependenceInput: Equatable, Sendable {
    public let targetNodeId: String
    public let capability: Capability
    /// 只使用 active + 指定 capability 的边（retired 排除）。
    public let dependencies: [Dependency]
    /// 已确认的故障域。key = `\(kind)|\(subjectRef)`，value 为 id。
    public let confirmedDomains: [String: String]
    /// 机器推断的可能故障域（needs_review）。
    public let suspectedDomains: [String: String]
    /// 边 id → 它所属的故障域 key 列表。
    public let edgeToDomainKeys: [String: [String]]

    public init(
        targetNodeId: String,
        capability: Capability,
        dependencies: [Dependency],
        confirmedDomains: [String: String],
        suspectedDomains: [String: String],
        edgeToDomainKeys: [String: [String]]
    ) {
        self.targetNodeId = targetNodeId
        self.capability = capability
        self.dependencies = dependencies
        self.confirmedDomains = confirmedDomains
        self.suspectedDomains = suspectedDomains
        self.edgeToDomainKeys = edgeToDomainKeys
    }
}

public struct PathIndependenceResult: Equatable, Sendable {
    public let targetNodeId: String
    public let capability: Capability
    /// 候选恢复/认证路径总数（confirmed active edges 指向 target 的边）。
    public let pathCount: Int
    /// 互不共享故障域的路径数。
    public let independentPathCount: Int
    /// 被确认共享的故障域（dedup + 确定性排序）。
    public let sharedFailureDomains: [String]
    /// 机器无法自行判定的假设（needs_review 的共享可能）。
    public let unresolvedAssumptions: [String]

    public init(
        targetNodeId: String,
        capability: Capability,
        pathCount: Int,
        independentPathCount: Int,
        sharedFailureDomains: [String],
        unresolvedAssumptions: [String]
    ) {
        self.targetNodeId = targetNodeId
        self.capability = capability
        self.pathCount = pathCount
        self.independentPathCount = independentPathCount
        self.sharedFailureDomains = sharedFailureDomains
        self.unresolvedAssumptions = unresolvedAssumptions
    }
}

public enum FailureDomainEngine {
    /// 计算 target 的恢复/认证路径独立程度。
    public static func computePathIndependence(_ input: PathIndependenceInput) -> PathIndependenceResult {
        let edges = input.dependencies.filter { d in
            d.to == input.targetNodeId && d.capability == input.capability && d.state == .active
        }
        let pathCount = edges.count

        // 共享键：所有入边故障域 key 的 union，去重 + 排序
        var sharedKeySet = Set<String>()
        for e in edges {
            for key in input.edgeToDomainKeys[e.id] ?? [] {
                sharedKeySet.insert(key)
            }
        }
        let sharedKeys = sharedKeySet.sorted()

        let sharedFailureDomains = sharedKeys.compactMap { input.confirmedDomains[$0] }
        let unresolvedAssumptions = sharedKeys.compactMap { input.suspectedDomains[$0] }

        // independentPathCount：在 confirmed 故障域上去重后的有效路径数。
        // 每个 confirmed 故障域内的多余路径只算 1 条（same phone 2 条 → 1；不同设备 → 2）。
        var domainCounts: [String: Int] = [:]
        for e in edges {
            for key in input.edgeToDomainKeys[e.id] ?? [] {
                if input.confirmedDomains[key] != nil {
                    domainCounts[key, default: 0] += 1
                }
            }
        }
        var redundancy = 0
        for count in domainCounts.values {
            redundancy += max(0, count - 1)
        }
        let independentPathCount = max(0, pathCount - redundancy)

        return PathIndependenceResult(
            targetNodeId: input.targetNodeId,
            capability: input.capability,
            pathCount: pathCount,
            independentPathCount: independentPathCount,
            sharedFailureDomains: Array(Set(sharedFailureDomains)).sorted(),
            unresolvedAssumptions: Array(Set(unresolvedAssumptions)).sorted()
        )
    }

    /// 构建 `edgeToDomainKeys`：把「故障域 subject（device/phone/provider）→ 边」的映射
    /// 变成边 → 故障域 key 的反向索引。kind 从节点 kind/subtype 推导，由调用方提供。
    public static func buildEdgeToDomainIndex(
        edges: [(id: String, from: String)],
        subjectToEdges: [String: [String]]
    ) -> [String: [String]] {
        var index: [String: [String]] = [:]
        for (subject, edgeIds) in subjectToEdges {
            for edgeId in edgeIds {
                var existing = index[edgeId] ?? []
                existing.append(subject)
                index[edgeId] = existing
            }
        }
        return index
    }
}
