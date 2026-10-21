import Foundation
import PDIGCore

/// Read-only iOS Production VNext projection.
///
/// This is deliberately narrower than Android Production today: iOS
/// GraphSnapshot does not yet expose governed fields_json identity profiles,
/// region facts or maintenance profiles. Therefore every identity_anchor stays
/// generic. Names, issuer text and identifier-like strings never promote an
/// object to phone/email.
enum VIosProductionSurfaceKind: String, Equatable, Sendable {
    case paymentAsset
    case account
    case service
    case membership
    case device
    case identityAnchorGeneric
    case customGeneric
}

struct VIosProductionObject: Equatable, Sendable, Identifiable {
    let id: String
    let kind: String
    let name: String
    let surfaceKind: VIosProductionSurfaceKind
    let issuer: String?
    let last4: String?
    let confirmedDependencyCount: Int
}

struct VIosProductionDependency: Equatable, Sendable, Identifiable {
    let id: String
    let fromId: String
    let fromName: String
    let relation: String
    let toId: String
    let toName: String
    let capability: String
    let criticality: String
}

struct VIosProductionPlanAction: Equatable, Sendable, Identifiable {
    let id: String
    let title: String
    let phase: String
    let done: Bool
    let verificationStatus: String?
    let verificationEvidenceRefs: [String]
    let resolvesImpactKeys: [String]
}

struct VIosProductionPlan: Equatable, Sendable, Identifiable {
    let id: String
    let scenario: String
    let title: String
    let workflowState: String
    let baselineGraphRevision: Int
    let lastAnalyzedGraphRevision: Int
    let currentGraphRevision: Int
    let targetNodeId: String?
    let targetNodeName: String?
    let effectiveDate: String?
    let actions: [VIosProductionPlanAction]

    var needsReanalysis: Bool {
        workflowState != ChangePlanWorkflowState.completed.wire &&
            workflowState != ChangePlanWorkflowState.cancelled.wire &&
            currentGraphRevision > lastAnalyzedGraphRevision
    }
}

struct VIosProductionReviewSummary: Equatable, Sendable {
    let proposalCount: Int
    let candidateCount: Int
    let driftCount: Int

    var total: Int { proposalCount + candidateCount + driftCount }
}

struct VIosProductionSourceCoverage: Equatable, Sendable {
    let sourceCount: Int
    let activeSourceCount: Int
}

struct VIosProductionSnapshot: Equatable, Sendable {
    let revision: Int
    let objects: [VIosProductionObject]
    let confirmedDependencies: [VIosProductionDependency]
    let plans: [VIosProductionPlan]
    let pendingReview: VIosProductionReviewSummary
    let sourceCoverage: VIosProductionSourceCoverage
}

func vIosProductionSurfaceKind(_ kind: NodeKind) -> VIosProductionSurfaceKind {
    switch kind {
    case .paymentInstrument: return .paymentAsset
    case .account: return .account
    case .service: return .service
    case .membership: return .membership
    case .device: return .device
    case .identityAnchor: return .identityAnchorGeneric
    case .custom: return .customGeneric
    }
}

func buildIosProductionVNextSnapshot(
    snapshot: GraphSnapshot,
    plans: [ChangePlan],
    pendingCandidateCount: Int,
    openDriftCount: Int
) -> VIosProductionSnapshot {
    let activeNodes = snapshot.nodes.filter { !$0.archived }
    let nodesById = Dictionary(uniqueKeysWithValues: activeNodes.map { ($0.id, $0) })

    // Dependency rows are confirmed Reality only when active. Pending proposals
    // remain review work and never enter this collection.
    let dependencies = snapshot.activeDependencies.compactMap { dependency
        -> VIosProductionDependency? in
        guard let from = nodesById[dependency.from],
              let to = nodesById[dependency.to] else {
            return nil
        }
        return VIosProductionDependency(
            id: dependency.id,
            fromId: dependency.from,
            fromName: from.name,
            relation: dependency.relation.wire,
            toId: dependency.to,
            toName: to.name,
            capability: dependency.capability.wire,
            criticality: dependency.criticality.wire
        )
    }

    let dependencyCounts = dependencies.reduce(into: [String: Int]()) { counts, dependency in
        counts[dependency.fromId, default: 0] += 1
        counts[dependency.toId, default: 0] += 1
    }

    let objects = activeNodes.map { node in
        VIosProductionObject(
            id: node.id,
            kind: node.kind.wire,
            name: node.name,
            surfaceKind: vIosProductionSurfaceKind(node.kind),
            issuer: node.issuer,
            last4: node.last4,
            confirmedDependencyCount: dependencyCounts[node.id, default: 0]
        )
    }.sorted { $0.id < $1.id }

    let projectedPlans = plans.map { plan in
        VIosProductionPlan(
            id: plan.id,
            scenario: plan.scenario,
            title: plan.title,
            workflowState: plan.workflowState.wire,
            baselineGraphRevision: plan.baselineGraphRevision,
            lastAnalyzedGraphRevision: plan.lastAnalyzedGraphRevision,
            currentGraphRevision: snapshot.graphRevision,
            targetNodeId: plan.targetNodeId,
            targetNodeName: plan.targetNodeId.flatMap { nodesById[$0]?.name },
            effectiveDate: plan.effectiveDate,
            actions: plan.actions.map { action in
                VIosProductionPlanAction(
                    id: action.id,
                    title: action.title,
                    phase: action.phase.wire,
                    done: action.done,
                    verificationStatus: action.verification?.status.wire,
                    verificationEvidenceRefs: action.verification?.evidenceRefs ?? [],
                    resolvesImpactKeys: action.resolvesImpactKeys
                )
            }
        )
    }.sorted { $0.id < $1.id }

    return VIosProductionSnapshot(
        revision: snapshot.graphRevision,
        objects: objects,
        confirmedDependencies: dependencies.sorted { $0.id < $1.id },
        plans: projectedPlans,
        pendingReview: VIosProductionReviewSummary(
            proposalCount: snapshot.pendingProposals.filter { $0.decision == .pending }.count,
            candidateCount: pendingCandidateCount,
            driftCount: openDriftCount
        ),
        sourceCoverage: VIosProductionSourceCoverage(
            sourceCount: snapshot.sources.count,
            activeSourceCount: snapshot.sources.filter {
                $0.state == SourceInstanceState.active.wire
            }.count
        )
    )
}

extension AppSession {
    /// Lock-gated read seam for future Production VNext screens.
    ///
    /// Returning nil outside .ready prevents a VNext caller from using this
    /// projection as a lock bypass. The normal Production router is unchanged.
    func productionVNextSnapshot() -> VIosProductionSnapshot? {
        guard phase == .ready else { return nil }
        return buildIosProductionVNextSnapshot(
            snapshot: snapshot,
            plans: plans,
            pendingCandidateCount: pendingCandidatesCount(),
            openDriftCount: openDrifts().count
        )
    }
}
