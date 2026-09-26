// 薄弱点发现（Findings）—— 组合 PDIGCore 引擎：
//   FailureDomainEngine.computePathIndependence（单点/共享故障点/未确认备用）
//   RecoveryCycleEngine.detectRecoveryCycles（恢复循环）
//   依赖新鲜度 / criticality / 未验证动作（辅助判据）
//
// 铁律（AGENTS §14）：任何「必须处理」只能来自已确认现实。
// 本模块产出的都是"发现/建议"，不产生 must_change 断言。

import Foundation
import PDIGCore

/// 一条薄弱点发现。type 为内部类型（不直接上屏），UI 只渲染中文文案。
public struct FindingItem: Equatable, Sendable, Identifiable {
    public enum Kind: String, Equatable, Sendable {
        case singlePointOfFailure
        case sharedFailureDomain
        case recoveryCycle
        case unconfirmedFallback
        case staleRecoveryInformation
        case unknownCriticalPath
        case pendingVerification
    }

    public let id: String
    public let kind: Kind
    public let title: String
    public let what: String
    public let why: String
    public let confirmedBasis: String
    public let unknowns: String
    public let affectedCapability: String
    public let recommendedNextAction: String
    public let targetNodeId: String?

    public init(
        id: String,
        kind: Kind,
        title: String,
        what: String,
        why: String,
        confirmedBasis: String,
        unknowns: String,
        affectedCapability: String,
        recommendedNextAction: String,
        targetNodeId: String? = nil
    ) {
        self.id = id
        self.kind = kind
        self.title = title
        self.what = what
        self.why = why
        self.confirmedBasis = confirmedBasis
        self.unknowns = unknowns
        self.affectedCapability = affectedCapability
        self.recommendedNextAction = recommendedNextAction
        self.targetNodeId = targetNodeId
    }
}

public struct FindingsInput: Equatable, Sendable {
    public let snapshot: GraphSnapshot
    /// 当前未完成验证的变更动作（来自 ChangePlan），用于 pendingVerification 判据。
    public let pendingVerifications: [String]
    /// 判定"最近确认"的时间口径（ISO-8601）。nil 则不做新鲜度告警。
    public let nowIso: String?
    public let freshnessThresholdDays: Int

    public init(
        snapshot: GraphSnapshot,
        pendingVerifications: [String] = [],
        nowIso: String? = nil,
        freshnessThresholdDays: Int = 90
    ) {
        self.snapshot = snapshot
        self.pendingVerifications = pendingVerifications
        self.nowIso = nowIso
        self.freshnessThresholdDays = freshnessThresholdDays
    }
}

public enum FindingsViewModel {

    /// 恢复/认证域：这两个 capability 的入边是"恢复路径"。
    private static let recoveryCapabilities: [Capability] = [.recovery, .authentication]

    public static func findings(_ input: FindingsInput) -> [FindingItem] {
        var items: [FindingItem] = []
        let snapshot = input.snapshot
        let nodeNames = Dictionary(uniqueKeysWithValues: snapshot.nodes.map { ($0.id, $0.name) })

        func name(_ id: String) -> String { nodeNames[id] ?? id }

        // 1. 恢复路径独立性（FailureDomainEngine）
        for capability in recoveryCapabilities {
            let targets = Set(
                snapshot.activeDependencies
                    .filter { $0.capability == capability }
                    .map { $0.to }
            )
            for target in targets.sorted() {
                let edges = snapshot.activeDependencies.filter { $0.to == target && $0.capability == capability }
                let confirmedDomains = domainMap(for: edges, snapshot: snapshot, confirmed: true)
                let suspectedDomains = domainMap(for: edges, snapshot: snapshot, confirmed: false)
                let edgeToDomainKeys = FailureDomainEngine.buildEdgeToDomainIndex(
                    edges: edges.map { (id: $0.id, from: $0.from) },
                    subjectToEdges: subjectToEdges(for: edges, snapshot: snapshot)
                )
                let result = FailureDomainEngine.computePathIndependence(
                    PathIndependenceInput(
                        targetNodeId: target,
                        capability: capability,
                        dependencies: snapshot.activeDependencies,
                        confirmedDomains: confirmedDomains,
                        suspectedDomains: suspectedDomains,
                        edgeToDomainKeys: edgeToDomainKeys
                    )
                )
                if result.pathCount == 0 { continue }
                if result.pathCount == 1 {
                    let edge = edges[0]
                    items.append(FindingItem(
                        id: "spof-\(target)-\(capability.wire)",
                        kind: .singlePointOfFailure,
                        title: CopyZh.findingSinglePointOfFailure,
                        what: "\(name(target)) 的恢复/验证只有 \(name(edge.from)) 一个来源，没有备用路径。",
                        why: "恢复路径独立度分析显示该对象只有 1 条已确认的恢复/验证路径。",
                        confirmedBasis: "\(CopyZh.relation(edge.relation.wire))：\(name(edge.from)) → \(name(target))（已确认，最后确认 \(displayDate(edge.lastVerifiedAt))）。",
                        unknowns: "这条路径失效后是否有其他方式找回账号尚不清楚。",
                        affectedCapability: CopyZh.capability(capability.wire),
                        recommendedNextAction: "为 \(name(target)) 添加至少一条独立的恢复/验证方式。",
                        targetNodeId: target
                    ))
                }
                if !result.sharedFailureDomains.isEmpty {
                    items.append(FindingItem(
                        id: "sfd-\(target)-\(capability.wire)",
                        kind: .sharedFailureDomain,
                        title: CopyZh.findingSharedFailureDomain,
                        what: "\(name(target)) 的 \(result.pathCount) 条恢复/验证路径共享同一个故障点，并非独立备用路径。",
                        why: "\(result.sharedFailureDomains.count) 个已确认共享故障域同时出现在这些路径上。",
                        confirmedBasis: "共享故障域来自用户确认（路径同属一台设备 / 同一个手机号 / 同一家服务商）。",
                        unknowns: "这些路径失效时是否还有第三条不共享该故障点的路径尚不清楚。",
                        affectedCapability: CopyZh.capability(capability.wire),
                        recommendedNextAction: "确认一条不共享该故障点的独立路径，或接受当前降级冗余。",
                        targetNodeId: target
                    ))
                }
                if !result.unresolvedAssumptions.isEmpty {
                    items.append(FindingItem(
                        id: "ufb-\(target)-\(capability.wire)",
                        kind: .unconfirmedFallback,
                        title: CopyZh.findingUnconfirmedFallback,
                        what: "\(name(target)) 存在 \(result.pathCount) 条恢复/验证路径，但备用路径尚未确认。",
                        why: "这些路径可能共享故障点（\(result.unresolvedAssumptions.count) 项需要确认），不能当作已确认的备用路径。",
                        confirmedBasis: "路径本身来自已确认关系；共享故障点是机器推断，需要用户确认。",
                        unknowns: "它们是否真的共享同一故障点还不知道。",
                        affectedCapability: CopyZh.capability(capability.wire),
                        recommendedNextAction: "确认或排除这些路径的共享故障点后，再决定是否作为备用路径。",
                        targetNodeId: target
                    ))
                }
                // 未知 criticality 的关键路径
                let unknownCritical = edges.filter { $0.criticality == .unknown }
                if !unknownCritical.isEmpty {
                    items.append(FindingItem(
                        id: "ucp-\(target)-\(capability.wire)",
                        kind: .unknownCriticalPath,
                        title: CopyZh.findingUnknownCriticalPath,
                        what: "\(name(target)) 的 \(unknownCritical.count) 条恢复/验证路径是否必需（关键）还不清楚。",
                        why: "这些关系没有被确认为必需，也没有被排除。",
                        confirmedBasis: "已确认的关系存在，但 criticality 保持未定。",
                        unknowns: "它失效时对 \(name(target)) 的影响到底有多大还不知道。",
                        affectedCapability: CopyZh.capability(capability.wire),
                        recommendedNextAction: "逐条确认这些路径是否必需。",
                        targetNodeId: target
                    ))
                }
                // 过期恢复信息
                if let nowIso = input.nowIso {
                    let stale = edges.filter { isStale($0.lastVerifiedAt, now: nowIso, thresholdDays: input.freshnessThresholdDays) }
                    if !stale.isEmpty {
                        items.append(FindingItem(
                            id: "stale-\(target)-\(capability.wire)",
                            kind: .staleRecoveryInformation,
                            title: CopyZh.findingStaleRecoveryInformation,
                            what: "\(name(target)) 的 \(stale.count) 条恢复/验证路径超过 \(input.freshnessThresholdDays) 天没有重新确认。",
                            why: "恢复信息会随换号/换设备/换卡失效，长期未确认的信息可能已经过时。",
                            confirmedBasis: "关系曾确认过，但最近确认时间已超过新鲜度阈值。",
                            unknowns: "这些路径现在是否仍然有效还不知道。",
                            affectedCapability: CopyZh.capability(capability.wire),
                            recommendedNextAction: "重新确认这 \(stale.count) 条恢复/验证路径是否仍然成立。",
                            targetNodeId: target
                        ))
                    }
                }
            }
        }

        // 2. 恢复循环（RecoveryCycleEngine）
        for capability in recoveryCapabilities {
            let input2 = RecoveryCycleInput(
                capability: capability,
                dependencies: snapshot.activeDependencies,
                hintedEdges: snapshot.pendingProposals
                    .filter { $0.capability == capability }
                    .map { RecoveryHintedEdge(from: $0.from, to: $0.to) }
            )
            let cycleResult = RecoveryCycleEngine.detectRecoveryCycles(input2)
            for cycle in cycleResult.confirmedCycles {
                let names = cycle.map { name($0) }.joined(separator: " → ")
                items.append(FindingItem(
                    id: "cycle-\(capability.wire)-\(names)",
                    kind: .recoveryCycle,
                    title: CopyZh.findingRecoveryCycle,
                    what: "恢复方式本身依赖你正在尝试恢复的对象：\(names)。",
                    why: CopyZh.recoveryCycleConfirmed,
                    confirmedBasis: "环路全部由已确认的恢复/验证关系构成。",
                    unknowns: "环路中的每一步是否都真正可用还不确定。",
                    affectedCapability: CopyZh.capability(capability.wire),
                    recommendedNextAction: "为环路中的对象添加一条不依赖该环路的独立恢复方式。",
                    targetNodeId: cycle.first
                ))
            }
            for cycle in cycleResult.potentialCycles {
                let names = cycle.map { name($0) }.joined(separator: " → ")
                items.append(FindingItem(
                    id: "pcycle-\(capability.wire)-\(names)",
                    kind: .recoveryCycle,
                    title: CopyZh.findingRecoveryCycle,
                    what: "可能存在的恢复循环：\(names)。",
                    why: CopyZh.recoveryCyclePotential,
                    confirmedBasis: "部分边来自待确认建议，环路尚未完全确认。",
                    unknowns: "这些建议是否成立还不知道。",
                    affectedCapability: CopyZh.capability(capability.wire),
                    recommendedNextAction: "确认相关建议后重新检查是否构成恢复循环。",
                    targetNodeId: cycle.first
                ))
            }
        }

        // 3. 未验证的变更动作
        if !input.pendingVerifications.isEmpty {
            items.append(FindingItem(
                id: "pending-verification",
                kind: .pendingVerification,
                title: CopyZh.findingPendingVerification,
                what: "有 \(input.pendingVerifications.count) 项变更已完成但尚未验证。",
                why: "未验证的变更不能视为已生效的恢复路径（添加 ≠ 验证）。",
                confirmedBasis: "变更计划中处于待验证状态的动作。",
                unknowns: "这些变更是否真正生效还不知道。",
                affectedCapability: "身份与恢复",
                recommendedNextAction: "进入验证流程，逐项验证后更新状态。",
                targetNodeId: nil
            ))
        }

        return items.sorted { $0.id < $1.id }
    }

    // MARK: - 故障域推导（设备/手机号/服务商；与 copy-zh failureDomain 对齐）

    /// 按节点归属推导故障域 subject：device → DEVICE|deviceId；identityAnchor → PHONE_NUMBER|phoneId；
    /// account（provider 归属）→ PROVIDER|issuer。
    private static func domainOfNode(_ n: DepNode) -> (kind: String, subject: String)? {
        switch n.kind {
        case .device: return ("DEVICE", n.id)
        case .identityAnchor: return ("PHONE_NUMBER", n.id)
        case .account:
            if let issuer = n.issuer, !issuer.isEmpty { return ("PROVIDER", issuer) }
            return nil
        default: return nil
        }
    }

    /// 边 → 所属故障域（机器推断 vs 用户确认：App 对 device/phone 的归属视为
    /// 已确认（节点本身是用户维护的现实对象），PROVIDER 归属视为 needs_review）。
    private static func subjectToEdges(
        for edges: [Dependency],
        snapshot: GraphSnapshot
    ) -> [String: [String]] {
        var subjectToEdges: [String: [String]] = [:]
        for edge in edges {
            guard let fromNode = snapshot.node(id: edge.from),
                  let domain = domainOfNode(fromNode) else { continue }
            let key = "\(domain.kind)|\(domain.subject)"
            var list = subjectToEdges[key] ?? []
            list.append(edge.id)
            subjectToEdges[key] = list
        }
        return subjectToEdges
    }

    /// 已确认故障域表：key = `KIND|subject`（与 buildEdgeToDomainIndex 的 subject 键一致），value = 节点 id。
    /// App 把 identityAnchor / device 的归属视为已确认（节点是用户维护的现实对象）；
    /// PROVIDER（issuer 归属）属于机器推断 → needs_review（suspected）。
    private static func domainMap(
        for edges: [Dependency],
        snapshot: GraphSnapshot,
        confirmed: Bool
    ) -> [String: String] {
        var map: [String: String] = [:]
        for edge in edges {
            guard let fromNode = snapshot.node(id: edge.from),
                  let domain = domainOfNode(fromNode) else { continue }
            let isConfirmedKind = fromNode.kind == .device || fromNode.kind == .identityAnchor
            if isConfirmedKind == confirmed {
                map["\(domain.kind)|\(domain.subject)"] = fromNode.id
            }
        }
        return map
    }

    private static func displayDate(_ iso: String) -> String {
        iso.isEmpty ? "从未" : String(iso.prefix(10))
    }

    private static func isStale(_ lastVerifiedAt: String, now: String, thresholdDays: Int) -> Bool {
        guard !lastVerifiedAt.isEmpty,
              let t = Timeline.parseIsoEpoch(lastVerifiedAt),
              let now = Timeline.parseIsoEpoch(now) else { return false }
        return (Double(now) - Double(t)) / Timeline.dayMs > Double(thresholdDays)
    }
}
