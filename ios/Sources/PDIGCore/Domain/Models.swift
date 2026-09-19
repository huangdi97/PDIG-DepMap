// PDIG 领域模型 —— Swift 移植。
//
// 源头：android/core/.../domain/Models.kt（Android 已 CORE_FROZEN，是本端口的基准）。
// 本文件是**忠实移植**，不是重新设计：任何行为差异都必须先改 spec/ 再改三端，
// 不得在 Swift 侧自行"修正"。
//
// 铁律（spec/domain/entities.md）：
//  - Dependency 存在即代表用户确认
//  - criticality 只有 required / unknown，机器永不产生 required
//  - retired 表示"不再成立"，不是删除；重新成立时 re-activate 原行

import Foundation

public struct DepNode: Equatable, Sendable {
    public let id: String
    public let kind: NodeKind
    public let name: String
    public let issuer: String?
    public let last4: String?
    public let archived: Bool

    public init(
        id: String,
        kind: NodeKind,
        name: String,
        issuer: String? = nil,
        last4: String? = nil,
        archived: Bool = false
    ) {
        self.id = id
        self.kind = kind
        self.name = name
        self.issuer = issuer
        self.last4 = last4
        self.archived = archived
    }
}

/// `from` 为 `to` 提供支付 → from 失效时 to 受影响。
public struct Dependency: Equatable, Sendable {
    public let id: String
    public let from: String
    public let relation: Relation
    public let to: String
    public let capability: Capability
    public let criticality: Criticality
    public let groupId: String?
    public let state: DependencyState
    public let origin: DependencyOrigin
    public let lastVerifiedAt: String

    public init(
        id: String,
        from: String,
        relation: Relation,
        to: String,
        capability: Capability,
        criticality: Criticality,
        groupId: String? = nil,
        state: DependencyState = .active,
        origin: DependencyOrigin = .manual,
        lastVerifiedAt: String = ""
    ) {
        self.id = id
        self.from = from
        self.relation = relation
        self.to = to
        self.capability = capability
        self.criticality = criticality
        self.groupId = groupId
        self.state = state
        self.origin = origin
        self.lastVerifiedAt = lastVerifiedAt
    }
}

public struct DependencyGroup: Equatable, Sendable {
    public let id: String
    public let groupKey: String
    public let targetNodeId: String
    public let capability: Capability
    public let mode: GroupMode
    public let memberEdgeIds: [String]
    public let state: GroupState

    public init(
        id: String,
        groupKey: String,
        targetNodeId: String,
        capability: Capability,
        mode: GroupMode,
        memberEdgeIds: [String],
        state: GroupState = .active
    ) {
        self.id = id
        self.groupKey = groupKey
        self.targetNodeId = targetNodeId
        self.capability = capability
        self.mode = mode
        self.memberEdgeIds = memberEdgeIds
        self.state = state
    }
}

public struct ImpactProposalInput: Equatable, Sendable {
    public let key: String
    public let from: String
    public let to: String
    public let capability: Capability
    public let confidenceScore: Double?

    public init(
        key: String,
        from: String,
        to: String,
        capability: Capability,
        confidenceScore: Double? = nil
    ) {
        self.key = key
        self.from = from
        self.to = to
        self.capability = capability
        self.confidenceScore = confidenceScore
    }
}

public struct ImpactGraph: Equatable, Sendable {
    public let dependencies: [Dependency]
    public let groups: [DependencyGroup]
    public let proposals: [ImpactProposalInput]
    public let nodeNames: [String: String]

    public init(
        dependencies: [Dependency] = [],
        groups: [DependencyGroup] = [],
        proposals: [ImpactProposalInput] = [],
        nodeNames: [String: String] = [:]
    ) {
        self.dependencies = dependencies
        self.groups = groups
        self.proposals = proposals
        self.nodeNames = nodeNames
    }
}

/// 状态键是 (nodeId, capability)，**不是** nodeId。
public struct ImpactStateKey: Equatable, Hashable, Sendable {
    public let nodeId: String
    public let capability: Capability

    public init(_ nodeId: String, _ capability: Capability) {
        self.nodeId = nodeId
        self.capability = capability
    }

    public init(parse s: String) throws {
        guard let i = s.firstIndex(of: "|"), i > s.startIndex else {
            throw ImpactKeyError.badFormat(s)
        }
        let node = String(s[s.startIndex..<i])
        let capWire = String(s[s.index(after: i)...])
        guard let cap = Capability(rawValue: capWire) else {
            throw ImpactKeyError.unknownCapability(s)
        }
        self.nodeId = node
        self.capability = cap
    }

    public func keyString() -> String { "\(nodeId)|\(capability.wire)" }
}

public enum ImpactKeyError: Error, CustomStringConvertible {
    case badFormat(String)
    case unknownCapability(String)

    public var description: String {
        switch self {
        case .badFormat(let s): return "impact key must be 'nodeId|capability', got \(s)"
        case .unknownCapability(let s): return "unknown capability in key: \(s)"
        }
    }
}

/// logical key: `from|relation|to|capability` —— 跨端必须完全一致（spec §9）。
public func dependencyLogicalKey(
    from: String,
    relation: Relation,
    to: String,
    capability: Capability
) -> String {
    "\(from)|\(relation.wire)|\(to)|\(capability.wire)"
}

/// canonical groupKey: `target|capability|mode|sorted(dedup(members...))`（成员顺序无关，spec §4）。
public func canonicalGroupKey(
    targetNodeId: String,
    capability: Capability,
    mode: GroupMode,
    memberLogicalKeys: [String]
) -> String {
    let sorted = Array(Set(memberLogicalKeys)).sorted()
    return ([targetNodeId, capability.wire, mode.wire] + sorted).joined(separator: "|")
}

// ---------------------------------------------------------------------------
// ChangePlan / Readiness / Coverage
// ---------------------------------------------------------------------------

public struct ActionVerification: Equatable, Sendable {
    public let method: ActionVerificationMethod
    public let status: ActionVerificationStatus
    public let evidenceRefs: [String]
    public let expectedFromNodeId: String?
    public let expectedToNodeId: String?

    public init(
        method: ActionVerificationMethod,
        status: ActionVerificationStatus,
        evidenceRefs: [String] = [],
        expectedFromNodeId: String? = nil,
        expectedToNodeId: String? = nil
    ) {
        self.method = method
        self.status = status
        self.evidenceRefs = evidenceRefs
        self.expectedFromNodeId = expectedFromNodeId
        self.expectedToNodeId = expectedToNodeId
    }
}

public struct PlanAction: Equatable, Sendable {
    public let id: String
    public let title: String
    public let phase: PlanActionPhase
    public let done: Bool
    public let resolvesImpactKeys: [String]
    public let verification: ActionVerification?

    public init(
        id: String,
        title: String = "",
        phase: PlanActionPhase,
        done: Bool = false,
        resolvesImpactKeys: [String] = [],
        verification: ActionVerification? = nil
    ) {
        self.id = id
        self.title = title
        self.phase = phase
        self.done = done
        self.resolvesImpactKeys = resolvesImpactKeys
        self.verification = verification
    }
}

public struct ChangePlan: Equatable, Sendable {
    public let id: String
    public let templateId: String?
    public let scenario: String
    public let title: String
    public let workflowState: ChangePlanWorkflowState
    public let baselineGraphRevision: Int
    public let lastAnalyzedGraphRevision: Int
    public let targetNodeId: String?
    public let effectiveDate: String?
    public let actions: [PlanAction]

    public init(
        id: String,
        templateId: String? = nil,
        scenario: String = "",
        title: String = "",
        workflowState: ChangePlanWorkflowState = .draft,
        baselineGraphRevision: Int = 0,
        lastAnalyzedGraphRevision: Int = 0,
        targetNodeId: String? = nil,
        effectiveDate: String? = nil,
        actions: [PlanAction] = []
    ) {
        self.id = id
        self.templateId = templateId
        self.scenario = scenario
        self.title = title
        self.workflowState = workflowState
        self.baselineGraphRevision = baselineGraphRevision
        self.lastAnalyzedGraphRevision = lastAnalyzedGraphRevision
        self.targetNodeId = targetNodeId
        self.effectiveDate = effectiveDate
        self.actions = actions
    }
}

public struct PlanReadinessInput: Equatable, Sendable {
    public let plan: ChangePlan
    public let currentGraphRevision: Int
    public let pendingMustChange: Int
    public let pendingNeedsReview: Int
    public let unresolvedCandidates: Int
    public let pendingRelevantProposals: Int
    public let staleRelevantDependencies: Int
    public let unfinishedChangeActions: Int

    public init(
        plan: ChangePlan,
        currentGraphRevision: Int,
        pendingMustChange: Int,
        pendingNeedsReview: Int,
        unresolvedCandidates: Int,
        pendingRelevantProposals: Int,
        staleRelevantDependencies: Int,
        unfinishedChangeActions: Int
    ) {
        self.plan = plan
        self.currentGraphRevision = currentGraphRevision
        self.pendingMustChange = pendingMustChange
        self.pendingNeedsReview = pendingNeedsReview
        self.unresolvedCandidates = unresolvedCandidates
        self.pendingRelevantProposals = pendingRelevantProposals
        self.staleRelevantDependencies = staleRelevantDependencies
        self.unfinishedChangeActions = unfinishedChangeActions
    }
}

public struct CoverageSourceInfo: Equatable, Sendable {
    public let id: String
    public let label: String
    public let lastIngestedAt: String?

    public init(id: String, label: String, lastIngestedAt: String?) {
        self.id = id
        self.label = label
        self.lastIngestedAt = lastIngestedAt
    }
}

public struct ScenarioCoverageInput: Equatable, Sendable {
    public let scenarioId: String
    public let sources: [CoverageSourceInfo]
    public let confirmedDirectDependencies: Int
    public let confirmedIndirectDependencies: Int
    public let pendingProposals: Int
    public let unresolvedCandidates: Int
    public let staleDependencies: Int
    public let unknownCriticalityCount: Int
    public let unverifiedActions: Int
    public let freshnessThresholdDays: Int
    public let now: String

    public init(
        scenarioId: String,
        sources: [CoverageSourceInfo],
        confirmedDirectDependencies: Int,
        confirmedIndirectDependencies: Int,
        pendingProposals: Int,
        unresolvedCandidates: Int,
        staleDependencies: Int,
        unknownCriticalityCount: Int,
        unverifiedActions: Int,
        freshnessThresholdDays: Int,
        now: String
    ) {
        self.scenarioId = scenarioId
        self.sources = sources
        self.confirmedDirectDependencies = confirmedDirectDependencies
        self.confirmedIndirectDependencies = confirmedIndirectDependencies
        self.pendingProposals = pendingProposals
        self.unresolvedCandidates = unresolvedCandidates
        self.staleDependencies = staleDependencies
        self.unknownCriticalityCount = unknownCriticalityCount
        self.unverifiedActions = unverifiedActions
        self.freshnessThresholdDays = freshnessThresholdDays
        self.now = now
    }
}

public struct ScenarioCoverage: Equatable, Sendable {
    public let scenarioId: String
    public let coverageLevel: CoverageLevel
    public let explanations: [String]
    public let counts: CoverageCounts

    public init(
        scenarioId: String,
        coverageLevel: CoverageLevel,
        explanations: [String],
        counts: CoverageCounts
    ) {
        self.scenarioId = scenarioId
        self.coverageLevel = coverageLevel
        self.explanations = explanations
        self.counts = counts
    }
}

public struct CoverageCounts: Equatable, Sendable {
    public let confirmedDirectDependencies: Int
    public let confirmedIndirectDependencies: Int
    public let pendingProposals: Int
    public let unresolvedCandidates: Int
    public let staleDependencies: Int
    public let unknownCriticalityCount: Int
    public let unverifiedActions: Int

    public init(
        confirmedDirectDependencies: Int,
        confirmedIndirectDependencies: Int,
        pendingProposals: Int,
        unresolvedCandidates: Int,
        staleDependencies: Int,
        unknownCriticalityCount: Int,
        unverifiedActions: Int
    ) {
        self.confirmedDirectDependencies = confirmedDirectDependencies
        self.confirmedIndirectDependencies = confirmedIndirectDependencies
        self.pendingProposals = pendingProposals
        self.unresolvedCandidates = unresolvedCandidates
        self.staleDependencies = staleDependencies
        self.unknownCriticalityCount = unknownCriticalityCount
        self.unverifiedActions = unverifiedActions
    }
}
