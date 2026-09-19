// PDIG Impact Kernel —— Swift 移植（MVP payment domain）。
//
// 源头：android/core/.../impact/ImpactKernel.kt。忠实移植，不是重新设计。
//
// 铁律：
//  - 状态键是 (nodeId, capability)，不是 nodeId（禁止 visited: Set<nodeId>）
//  - 图允许有环：wave-BFS，每 key 最多处理一次
//  - Proposal 任何 confidence 都不参与确定性失效传播（最多 needs_review）
//  - must_change 必须可追溯到 confirmed reality

import Foundation

public struct ImpactTargetResult: Equatable, Sendable {
    public let nodeId: String
    public let nodeName: String
    public let capability: Capability
    public let status: ImpactTargetStatus
    public let available: Bool
    public let redundancyDegraded: Bool
    public let depth: Int
    public let reasonCode: ImpactReasonCode
    public let reasonText: String
    public let edgeKeys: [String]
    public let groupKeys: [String]
    public let proposalKeys: [String]
}

public struct ImpactChecklistItem: Equatable, Sendable {
    public let level: ImpactLevel
    public let nodeId: String?
    public let capability: Capability?
    public let title: String
    public let detail: String
}

public struct ImpactResult: Equatable, Sendable {
    public let unavailable: [ImpactStateKey]
    public let lostKeys: [ImpactStateKey]
    public let targets: [ImpactTargetResult]
    public let checklist: [ImpactChecklistItem]
    public let processedKeys: [String]
}

public enum ImpactKernel {
    private static let payment = Capability.payment

    public static func simulateDisable(graph: ImpactGraph, nodeId: String) -> ImpactResult {
        simulateScenario(graph: graph, unavailable: [ImpactStateKey(nodeId, payment)])
    }

    public static func simulateScenario(
        graph: ImpactGraph,
        unavailable: [ImpactStateKey]
    ) -> ImpactResult {
        // 非 payment 初始键被忽略；按 keyString 值级去重（processedKeys 唯一不变量）
        var initialSeen = Set<String>()
        let initial = sortKeys(
            unavailable.filter { k in
                guard k.capability == payment else { return false }
                let ks = k.keyString()
                if initialSeen.contains(ks) { return false }
                initialSeen.insert(ks)
                return true
            }
        )

        let activePaymentDeps = graph.dependencies
            .filter { $0.state == .active && $0.capability == payment }
            .sorted { $0.id < $1.id }
        let activePaymentGroups = graph.groups
            .filter { $0.state == .active && $0.capability == payment }
            .sorted { $0.id < $1.id }
        let paymentProposals = graph.proposals.filter { $0.capability == payment }

        func nodeName(_ id: String) -> String { graph.nodeNames[id] ?? id }
        func keyOf(_ node: String) -> String { "\(node)|\(payment.wire)" }
        func edgeKeysOf(_ deps: [Dependency]) -> [String] {
            deps.map { dependencyLogicalKey(from: $0.from, relation: $0.relation, to: $0.to, capability: $0.capability) }
        }

        var unavailableSet = Set(initial.map { $0.keyString() })
        var lostKeys = initial
        var processedKeys = initial.map { $0.keyString() }
        var processedSeen = Set(processedKeys)
        var results: [String: ImpactTargetResult] = [:]
        var resultsOrder: [String] = []

        // 不确定性集合：needs_review 向下游传播"不确定"，但永不升级为 must_change
        var uncertainSet = Set<String>()

        func evaluateTarget(_ t: String, _ depth: Int) -> ImpactTargetResult {
            let incoming = activePaymentDeps.filter { $0.to == t }
            let lostEdges = incoming.filter { unavailableSet.contains(keyOf($0.from)) }
            let uncertainEdges = incoming.filter {
                !unavailableSet.contains(keyOf($0.from)) && uncertainSet.contains($0.from)
            }

            if lostEdges.isEmpty {
                let propKeys = paymentProposals
                    .filter { $0.to == t && unavailableSet.contains(keyOf($0.from)) }
                    .map { $0.key }
                    .sorted()
                if !uncertainEdges.isEmpty {
                    return ImpactTargetResult(
                        nodeId: t, nodeName: nodeName(t), capability: payment,
                        status: .needsReview, available: true, redundancyDegraded: false,
                        depth: depth, reasonCode: .upstreamUncertain,
                        reasonText: "上游支付能力存在未确认风险，\(nodeName(t)) 的支付是否受影响需人工核实",
                        edgeKeys: edgeKeysOf(uncertainEdges), groupKeys: [], proposalKeys: []
                    )
                }
                if !propKeys.isEmpty {
                    return ImpactTargetResult(
                        nodeId: t, nodeName: nodeName(t), capability: payment,
                        status: .needsReview, available: true, redundancyDegraded: false,
                        depth: depth, reasonCode: .proposalOnly,
                        reasonText: "检测到未确认的支付关系建议（置信度不改变结论），需人工核实 \(nodeName(t)) 的支付是否受影响",
                        edgeKeys: [], groupKeys: [], proposalKeys: propKeys
                    )
                }
                return ImpactTargetResult(
                    nodeId: t, nodeName: nodeName(t), capability: payment,
                    status: .unaffected, available: true, redundancyDegraded: false,
                    depth: depth, reasonCode: .criticalityUnknown,
                    reasonText: "未发现受影响的已确认支付关系",
                    edgeKeys: [], groupKeys: [], proposalKeys: []
                )
            }

            let lostEdgeIds = Set(lostEdges.map { $0.id })
            let coveringGroups = activePaymentGroups.filter { g in
                g.targetNodeId == t && g.memberEdgeIds.contains(where: { lostEdgeIds.contains($0) })
            }

            if !coveringGroups.isEmpty {
                let groupResults = coveringGroups.map { g -> Bool in
                    let memberEdges = activePaymentDeps.filter { g.memberEdgeIds.contains($0.id) }
                    let availableMembers = memberEdges.filter { !unavailableSet.contains(keyOf($0.from)) }
                    if g.mode == .any {
                        return !availableMembers.isEmpty
                    }
                    return availableMembers.count == memberEdges.count
                }
                let allFailed = groupResults.allSatisfy { !$0 }
                if allFailed {
                    return ImpactTargetResult(
                        nodeId: t, nodeName: nodeName(t), capability: payment,
                        status: .mustChange, available: false, redundancyDegraded: false,
                        depth: depth, reasonCode: .confirmedGroupFailed,
                        reasonText: "已确认的支付来源组合（\(coveringGroups.map { $0.mode.wire }.joined(separator: "/"))）全部失效，\(nodeName(t)) 的支付能力将失效",
                        edgeKeys: edgeKeysOf(lostEdges),
                        groupKeys: coveringGroups.map { $0.groupKey }.sorted(),
                        proposalKeys: []
                    )
                }
                return ImpactTargetResult(
                    nodeId: t, nodeName: nodeName(t), capability: payment,
                    status: .backupPath, available: true, redundancyDegraded: true,
                    depth: depth, reasonCode: .confirmedGroupCovered,
                    reasonText: "已确认存在替代支付来源，\(nodeName(t)) 的支付可继续，但冗余度下降（能力降级）",
                    edgeKeys: edgeKeysOf(lostEdges),
                    groupKeys: coveringGroups.map { $0.groupKey }.sorted(),
                    proposalKeys: []
                )
            }

            let otherEdges = incoming.filter { !lostEdgeIds.contains($0.id) }
            if !otherEdges.isEmpty {
                return ImpactTargetResult(
                    nodeId: t, nodeName: nodeName(t), capability: payment,
                    status: .needsReview, available: true, redundancyDegraded: false,
                    depth: depth, reasonCode: .unconfirmedAlternativeExists,
                    reasonText: "检测到其他支付来源，但未确认备用组合可自动接管，需人工核实 \(nodeName(t)) 的支付路径",
                    edgeKeys: edgeKeysOf(incoming), groupKeys: [], proposalKeys: []
                )
            }

            if lostEdges.contains(where: { $0.criticality == .required }) {
                return ImpactTargetResult(
                    nodeId: t, nodeName: nodeName(t), capability: payment,
                    status: .mustChange, available: false, redundancyDegraded: false,
                    depth: depth, reasonCode: .requiredEdgeNoAlternative,
                    reasonText: "已确认 \(nodeName(t)) 的支付能力依赖此关系（required），且无其他已记录来源",
                    edgeKeys: edgeKeysOf(lostEdges), groupKeys: [], proposalKeys: []
                )
            }

            return ImpactTargetResult(
                nodeId: t, nodeName: nodeName(t), capability: payment,
                status: .needsReview, available: true, redundancyDegraded: false,
                depth: depth, reasonCode: .criticalityUnknown,
                reasonText: "该支付关系未确认是否必需（criticality=unknown），需人工核实 \(nodeName(t)) 是否受影响",
                edgeKeys: edgeKeysOf(lostEdges), groupKeys: [], proposalKeys: []
            )
        }

        // wave-BFS：unavailableSet / uncertainSet 只增长 → 天然防环终止
        let initialNodeIds = Set(initial.map { $0.nodeId })
        var frontier: [String] = initial.map { $0.nodeId }
        var depth = 0
        var guardCount = 0
        let maxGuard = activePaymentDeps.count * 2 + initial.count + 8

        while !frontier.isEmpty && guardCount <= maxGuard {
            guardCount += 1
            depth += 1
            var pending: [String] = []
            var pendingSeen = Set<String>()
            for n in frontier {
                for d in activePaymentDeps where d.from == n && !unavailableSet.contains(keyOf(d.to)) {
                    if pendingSeen.insert(d.to).inserted { pending.append(d.to) }
                }
                if initialNodeIds.contains(n) {
                    for p in paymentProposals where p.from == n && !unavailableSet.contains(keyOf(p.to)) {
                        if pendingSeen.insert(p.to).inserted { pending.append(p.to) }
                    }
                }
            }
            let orderedPending = pending.sorted()
            var nextFrontier: [String] = []
            var grew = false

            for t in orderedPending {
                let k = keyOf(t)
                if unavailableSet.contains(k) { continue }
                let result = evaluateTarget(t, depth)
                if let prev = results[k] {
                    if severity(result.status) > severity(prev.status) {
                        results[k] = result
                    } else if severity(result.status) == severity(prev.status) {
                        results[k] = ImpactTargetResult(
                            nodeId: prev.nodeId, nodeName: prev.nodeName, capability: prev.capability,
                            status: prev.status, available: prev.available,
                            redundancyDegraded: prev.redundancyDegraded, depth: prev.depth,
                            reasonCode: prev.reasonCode, reasonText: prev.reasonText,
                            edgeKeys: Array(Set(prev.edgeKeys + result.edgeKeys)).sorted(),
                            groupKeys: Array(Set(prev.groupKeys + result.groupKeys)).sorted(),
                            proposalKeys: Array(Set(prev.proposalKeys + result.proposalKeys)).sorted()
                        )
                    }
                } else {
                    results[k] = result
                    resultsOrder.append(k)
                }

                if result.status == .mustChange && !unavailableSet.contains(k) {
                    unavailableSet.insert(k)
                    lostKeys.append(ImpactStateKey(t, payment))
                    if !processedSeen.contains(k) {
                        processedKeys.append(k)
                        processedSeen.insert(k)
                    }
                    uncertainSet.remove(t)
                    nextFrontier.append(t)
                    grew = true
                } else if result.status == .needsReview && !uncertainSet.contains(t) {
                    uncertainSet.insert(t)
                    if !processedSeen.contains(k) {
                        processedKeys.append(k)
                        processedSeen.insert(k)
                    }
                    nextFrontier.append(t)
                    grew = true
                }
            }

            if !grew { break }
            frontier = nextFrontier
        }

        let targets = results.values.sorted { a, b in
            if a.depth != b.depth { return a.depth < b.depth }
            if a.nodeId != b.nodeId { return a.nodeId < b.nodeId }
            return a.capability.wire < b.capability.wire
        }

        var checklist: [ImpactChecklistItem] = []
        for t in targets {
            switch t.status {
            case .unaffected:
                break
            case .mustChange:
                checklist.append(ImpactChecklistItem(
                    level: .mustChange, nodeId: t.nodeId, capability: t.capability,
                    title: "必须处理：\(t.nodeName) 的支付能力将失效", detail: t.reasonText))
            case .backupPath:
                checklist.append(ImpactChecklistItem(
                    level: .backupPath, nodeId: t.nodeId, capability: t.capability,
                    title: "有备用路径：\(t.nodeName) 可切换（能力降级）", detail: t.reasonText))
            default:
                checklist.append(ImpactChecklistItem(
                    level: .needsReview, nodeId: t.nodeId, capability: t.capability,
                    title: "建议检查：\(t.nodeName)", detail: t.reasonText))
            }
        }
        // 原始注销/停用动作强制最后（IMP-09）
        for k in initial {
            checklist.append(ImpactChecklistItem(
                level: .targetOperation, nodeId: k.nodeId, capability: k.capability,
                title: "最后一步：注销/停用 \(nodeName(k.nodeId))（原始操作）",
                detail: "以上事项处理完成后再执行原始操作。"))
        }

        return ImpactResult(
            unavailable: initial,
            lostKeys: lostKeys,
            targets: targets,
            checklist: checklist,
            processedKeys: processedKeys
        )
    }

    private static func sortKeys(_ keys: [ImpactStateKey]) -> [ImpactStateKey] {
        keys.sorted { a, b in
            if a.nodeId != b.nodeId { return a.nodeId < b.nodeId }
            return a.capability.wire < b.capability.wire
        }
    }

    private static func severity(_ s: ImpactTargetStatus) -> Int {
        switch s {
        case .mustChange: return 3
        case .backupPath, .degraded: return 2
        case .needsReview: return 1
        default: return 0
        }
    }
}
