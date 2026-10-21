import Foundation
import PDIGCore

/// Read-only iOS Production VNext projection.
///
/// This remains deliberately narrower than Android Production today: maintenance
/// profiles are not yet projected here. Governed fields_json identity and region
/// facts are allowed only through their typed Core readers. Names, issuer text,
/// number prefixes and identifier-like strings never create subtype authority.
enum VIosProductionSurfaceKind: String, Equatable, Sendable {
    case paymentAsset
    case account
    case service
    case membership
    case device
    case phoneIdentity
    case emailIdentity
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
    let identitySubtype: String?
    let identityIdentifierValue: String?
    let confirmedRegionCodes: [String]
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
    let prerequisiteActionIds: [String]
    let resolvesImpactKeys: [String]

    init(
        id: String,
        title: String,
        phase: String,
        done: Bool,
        verificationStatus: String?,
        verificationEvidenceRefs: [String],
        prerequisiteActionIds: [String] = [],
        resolvesImpactKeys: [String]
    ) {
        self.id = id
        self.title = title
        self.phase = phase
        self.done = done
        self.verificationStatus = verificationStatus
        self.verificationEvidenceRefs = verificationEvidenceRefs
        self.prerequisiteActionIds = prerequisiteActionIds
        self.resolvesImpactKeys = resolvesImpactKeys
    }
}

/// Records is an event trace, not a projection of future plan steps.
/// Verification templates are initialized as pending before execution; they
/// become recordable only after the action itself has an authoritative done event.
func vIosProductionActionHasRecordedEvent(
    _ action: VIosProductionPlanAction
) -> Bool {
    action.done
}

struct VIosProductionChangeStageSummary: Equatable, Sendable {
    let current: String
    let transition: String
    let after: String
}

func vIosProductionChangeStageSummary(
    _ plan: VIosProductionPlan
) -> VIosProductionChangeStageSummary {
    let completed = plan.actions.filter(\.done).count
    let verified = plan.actions.filter { $0.verificationStatus == "verified" }.count
    return VIosProductionChangeStageSummary(
        current: "当前数据版本 \(plan.currentGraphRevision) · 分析基准 \(plan.lastAnalyzedGraphRevision)",
        transition: "\(completed)/\(plan.actions.count) 步已记录完成 · \(verified) 步已验证",
        after: "完成后（计划）· 未执行与未验证内容仍不代表现实"
    )
}

func vIosProductionChangeElementId(
    _ role: String,
    actionId: String
) -> String {
    "pdig.ios.production-vnext.change.\(role).\(actionId)"
}

func vIosProductionRecordEvidenceText(
    _ action: VIosProductionPlanAction
) -> String {
    action.verificationEvidenceRefs.isEmpty
        ? "验证证据未记录"
        : "验证证据 \(action.verificationEvidenceRefs.count) 项"
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

func vIosProductionSurfaceKind(
    _ node: DepNode
) -> VIosProductionSurfaceKind {
    switch node.kind {
    case .paymentInstrument: return .paymentAsset
    case .account: return .account
    case .service: return .service
    case .membership: return .membership
    case .device: return .device
    case .custom: return .customGeneric
    case .identityAnchor:
        guard let profile = confirmedIdentityAnchorProfile(
            kind: node.kind,
            fieldsJson: node.fieldsJson ?? ""
        ) else {
            return .identityAnchorGeneric
        }
        switch profile.subtype {
        case .phoneNumber: return .phoneIdentity
        case .emailAddress: return .emailIdentity
        case .otherIdentity: return .identityAnchorGeneric
        }
    }
}

func vIosProductionSurfaceKind(
    for secondaryKind: VSecondaryKind
) -> VIosProductionSurfaceKind {
    switch secondaryKind {
    case .account: return .account
    case .email: return .emailIdentity
    case .device: return .device
    case .service: return .service
    }
}

func vIosProductionObjects(
    snapshot: VIosProductionSnapshot,
    surfaceKind: VIosProductionSurfaceKind,
    regionFilter: String?
) -> [VIosProductionObject] {
    snapshot.objects.filter { object in
        guard object.surfaceKind == surfaceKind else { return false }
        guard let regionFilter else { return true }
        return object.confirmedRegionCodes.contains(regionFilter)
    }
}

func vIosProductionSecondaryObjects(
    snapshot: VIosProductionSnapshot,
    kind: VSecondaryKind,
    regionFilter: String?
) -> [VIosProductionObject] {
    let expectedSurface = vIosProductionSurfaceKind(for: kind)
    return snapshot.objects.filter { object in
        guard object.surfaceKind == expectedSurface else { return false }
        guard let regionFilter else { return true }
        return object.confirmedRegionCodes.contains(regionFilter)
    }
}

func vIosVisibleProductionObjectName(
    _ object: VIosProductionObject,
    privacyMask: Bool
) -> String {
    guard privacyMask else { return object.name }
    switch object.surfaceKind {
    case .paymentAsset: return "支付工具（已遮蔽）"
    case .account: return "账户（已遮蔽）"
    case .service: return "服务（已遮蔽）"
    case .membership: return "会员 / 资格（已遮蔽）"
    case .device: return "设备（已遮蔽）"
    case .phoneIdentity: return "手机号身份（已遮蔽）"
    case .emailIdentity: return "邮箱身份（已遮蔽）"
    case .identityAnchorGeneric: return "身份对象（已遮蔽）"
    case .customGeneric: return "自定义对象（已遮蔽）"
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
        let identityProfile = node.kind == .identityAnchor
            ? confirmedIdentityAnchorProfile(
                kind: node.kind,
                fieldsJson: node.fieldsJson ?? ""
            )
            : nil
        let confirmedRegions = currentConfirmedRegionFacts(
            fieldsJson: node.fieldsJson ?? ""
        )
        return VIosProductionObject(
            id: node.id,
            kind: node.kind.wire,
            name: node.name,
            surfaceKind: vIosProductionSurfaceKind(node),
            issuer: node.issuer,
            last4: node.last4,
            identitySubtype: identityProfile?.subtype.wire,
            identityIdentifierValue: identityProfile?.identifier?.value,
            confirmedRegionCodes: Array(
                Set(confirmedRegions.map { $0.territoryCode })
            ).sorted(),
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
                    prerequisiteActionIds: action.prerequisiteActionIds,
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

func vIosProductionPendingVerificationActionIds(
    _ plans: [ChangePlan]
) -> [String] {
    plans
        .flatMap(\.actions)
        .filter { action in
            guard action.done, let status = action.verification?.status else {
                return false
            }
            return status == .pending || status == .evidenceSuggested
        }
        .map(\.id)
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

    /// Deterministic, read-only continuity findings over the same unlocked Reality.
    /// No reference fixture or Production mutation authority is involved.
    func productionVNextFindings(nowIso: String? = nil) -> [FindingItem]? {
        guard phase == .ready else { return nil }
        let pending = vIosProductionPendingVerificationActionIds(plans)
        return FindingsViewModel.findings(
            FindingsInput(
                snapshot: snapshot,
                pendingVerifications: pending,
                nowIso: nowIso,
                includePendingProposalCycles: false
            )
        )
    }
}
