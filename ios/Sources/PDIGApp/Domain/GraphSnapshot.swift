// 图快照 + Payload 编解码 —— App 层数据模型。
//
// PDIGCore 提供 exportGraph / importGraph / SqliteDriver 协议，但没有 repository。
// 本模块是 App 的存储边界：
//  - GraphSnapshot = 内存中的图（节点/关系/组/待确认建议/来源）
//  - PayloadCodec  = snapshot ↔ canonical payload JSON（与 exportGraph 同构）
//
// 诚实边界：payload 只包含 canonical 表；ChangePlan / Drift / Candidate 由
// GraphRepository 单独持久化（schema v3 的 change_plans / reality_drifts /
// discovery_candidates 表），不属于备份 payload。

import Foundation
import PDIGCore

/// 待确认建议（dependency proposal）的最小持久化模型。
public struct PendingProposal: Equatable, Sendable {
    public let id: String
    public let key: String
    public let from: String
    public let relation: Relation
    public let to: String
    public let capability: Capability
    public let proposalType: String
    public let source: String
    public let confidenceScore: Double?
    public let decision: ProposalDecision
    public let decidedAt: String?
    public let observationCount: Int
    public let createdAt: String
    public let updatedAt: String

    public init(
        id: String,
        key: String,
        from: String,
        relation: Relation,
        to: String,
        capability: Capability,
        proposalType: String = ProposalType.recurringPaymentRoute.wire,
        source: String = ProposalSource.statement.wire,
        confidenceScore: Double? = nil,
        decision: ProposalDecision = .pending,
        decidedAt: String? = nil,
        observationCount: Int = 0,
        createdAt: String,
        updatedAt: String
    ) {
        self.id = id
        self.key = key
        self.from = from
        self.relation = relation
        self.to = to
        self.capability = capability
        self.proposalType = proposalType
        self.source = source
        self.confidenceScore = confidenceScore
        self.decision = decision
        self.decidedAt = decidedAt
        self.observationCount = observationCount
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }
}

/// 已导入的来源实例。
public struct SourceInstanceRow: Equatable, Sendable {
    public let id: String
    public let adapterId: String
    public let adapterVersion: Int
    public let sourceKind: String
    public let label: String
    public let state: String
    public let lastIngestedAt: String?
    public let createdAt: String
    public let updatedAt: String

    public init(
        id: String,
        adapterId: String,
        adapterVersion: Int,
        sourceKind: String,
        label: String,
        state: String = SourceInstanceState.active.wire,
        lastIngestedAt: String? = nil,
        createdAt: String = "1970-01-01T00:00:00.000Z",
        updatedAt: String = "1970-01-01T00:00:00.000Z"
    ) {
        self.id = id
        self.adapterId = adapterId
        self.adapterVersion = adapterVersion
        self.sourceKind = sourceKind
        self.label = label
        self.state = state
        self.lastIngestedAt = lastIngestedAt
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }
}

/// 内存图快照。所有字段只读；修改通过 repository 的替换式写回。
public struct GraphSnapshot: Equatable, Sendable {
    public let nodes: [DepNode]
    public let dependencies: [Dependency]
    public let groups: [DependencyGroup]
    public let pendingProposals: [PendingProposal]
    public let sources: [SourceInstanceRow]
    public let graphRevision: Int

    public init(
        nodes: [DepNode] = [],
        dependencies: [Dependency] = [],
        groups: [DependencyGroup] = [],
        pendingProposals: [PendingProposal] = [],
        sources: [SourceInstanceRow] = [],
        graphRevision: Int = 0
    ) {
        self.nodes = nodes
        self.dependencies = dependencies
        self.groups = groups
        self.pendingProposals = pendingProposals
        self.sources = sources
        self.graphRevision = graphRevision
    }

    public func node(id: String) -> DepNode? { nodes.first { $0.id == id } }

    public func name(of id: String) -> String { node(id: id)?.name ?? id }

    /// 仅 active 的依赖（Impact/Findings 引擎的输入口径）。
    public var activeDependencies: [Dependency] {
        dependencies.filter { $0.state == .active }
    }
}

/// Payload 编解码：snapshot ↔ canonical payload JSON（键顺序与 exportGraph 一致）。
public enum PayloadCodec {

    public static func toPayloadJson(_ snapshot: GraphSnapshot) -> String {
        let fields: [(String, Json)] = [
            ("payloadKind", .str(GRAPH_PAYLOAD_KIND)),
            ("payloadVersion", .num(String(GRAPH_PAYLOAD_VERSION))),
            ("schemaVersion", .num(String(Migrations.schemaVersion))),
            ("meta", .arr(metaRows(snapshot.graphRevision))),
            ("source_instances", .arr(snapshot.sources.map { sourceRowJson($0) })),
            ("nodes", .arr(snapshot.nodes.map { nodeRowJson($0) })),
            ("dependencies", .arr(snapshot.dependencies.map { dependencyRowJson($0) })),
            ("dependency_groups", .arr(snapshot.groups.map { groupRowJson($0) })),
            ("dependency_proposals", .arr(snapshot.pendingProposals.map { proposalRowJson($0) })),
            ("proposal_evidence_refs", .arr([])),
            ("dependency_group_proposals", .arr([])),
            ("evidence", .arr([])),
            ("observation_fingerprints", .arr([])),
            ("import_sessions", .arr([])),
        ]
        return JsonWriter.write(.obj(JsonObject(fields)))
    }

    private static func metaRows(_ revision: Int) -> [Json] {
        [.obj(JsonObject([
            ("key", .str(GraphRevision.graphRevisionKey)),
            ("value", .str(String(revision))),
        ]))]
    }

    private static func sourceRowJson(_ s: SourceInstanceRow) -> Json {
        .obj(JsonObject([
            ("id", .str(s.id)),
            ("adapter_id", .str(s.adapterId)),
            ("adapter_version", .num(String(s.adapterVersion))),
            ("source_kind", .str(s.sourceKind)),
            ("provider_id", .null),
            ("account_node_id", .null),
            ("label", .str(s.label)),
            ("country", .null),
            ("jurisdiction", .null),
            ("currencies_json", .str(#"["CNY"]"#)),
            ("state", .str(s.state)),
            ("created_at", .str(s.createdAt)),
            ("updated_at", .str(s.updatedAt)),
            ("last_ingested_at", s.lastIngestedAt.map { .str($0) } ?? .null),
        ]))
    }

    private static func nodeRowJson(_ n: DepNode) -> Json {
        .obj(JsonObject([
            ("id", .str(n.id)),
            ("kind", .str(n.kind.wire)),
            ("template_id", .null),
            ("name", .str(n.name)),
            ("issuer", n.issuer.map { .str($0) } ?? .null),
            ("last4", n.last4.map { .str($0) } ?? .null),
            ("owner", .null),
            ("archived", .num(n.archived ? "1" : "0")),
            ("fields_json", .null),
            ("vault_ref", .null),
            ("wallet_ref", .null),
            ("created_at", .str("1970-01-01T00:00:00.000Z")),
            ("updated_at", .str("1970-01-01T00:00:00.000Z")),
        ]))
    }

    private static func dependencyRowJson(_ d: Dependency) -> Json {
        .obj(JsonObject([
            ("id", .str(d.id)),
            ("from_node", .str(d.from)),
            ("relation", .str(d.relation.wire)),
            ("to_node", .str(d.to)),
            ("capability", .str(d.capability.wire)),
            ("criticality", .str(d.criticality.wire)),
            ("group_id", d.groupId.map { .str($0) } ?? .null),
            ("state", .str(d.state.wire)),
            ("origin", .str(d.origin.wire)),
            ("confirmed_at", .str(d.origin == .proposal ? "" : d.lastVerifiedAt)),
            ("last_verified_at", .str(d.lastVerifiedAt)),
            ("retired_at", d.state == .retired ? .str(d.lastVerifiedAt) : .null),
            ("evidence_refs_json", .null),
            ("verification_basis_type", .str(VerificationBasisType.userConfirmed.wire)),
            ("verification_basis_json", .null),
            ("created_at", .str("1970-01-01T00:00:00.000Z")),
            ("updated_at", .str("1970-01-01T00:00:00.000Z")),
        ]))
    }

    private static func groupRowJson(_ g: DependencyGroup) -> Json {
        .obj(JsonObject([
            ("id", .str(g.id)),
            ("group_key", .str(g.groupKey)),
            ("target_node_id", .str(g.targetNodeId)),
            ("capability", .str(g.capability.wire)),
            ("mode", .str(g.mode.wire)),
            ("member_edge_ids_json", .str(jsonArrayString(g.memberEdgeIds))),
            ("state", .str(g.state.wire)),
            ("confirmed_at", .str("1970-01-01T00:00:00.000Z")),
            ("last_verified_at", .str("1970-01-01T00:00:00.000Z")),
            ("verification_basis_type", .str(VerificationBasisType.userConfirmed.wire)),
            ("verification_basis_json", .null),
            ("created_at", .str("1970-01-01T00:00:00.000Z")),
            ("updated_at", .str("1970-01-01T00:00:00.000Z")),
        ]))
    }

    private static func proposalRowJson(_ p: PendingProposal) -> Json {
        .obj(JsonObject([
            ("id", .str(p.id)),
            ("key", .str(p.key)),
            ("from_node", .str(p.from)),
            ("relation", .str(p.relation.wire)),
            ("to_node", .str(p.to)),
            ("capability", .str(p.capability.wire)),
            ("proposal_type", .str(p.proposalType)),
            ("source", .str(p.source)),
            ("parser_id", .str("generic_csv")),
            ("parser_version", .num("1")),
            ("confidence_score", p.confidenceScore.map { .num(String($0)) } ?? .null),
            ("path_json", .null),
            ("decision", .str(p.decision.wire)),
            ("decided_at", p.decidedAt.map { .str($0) } ?? .null),
            ("criticality_decision", .null),
            ("observation_count", .num(String(p.observationCount))),
            ("rejected_at", .null),
            ("rejected_at_stream_counts_json", .null),
            ("created_at", .str(p.createdAt)),
            ("updated_at", .str(p.updatedAt)),
        ]))
    }

    private static func jsonArrayString(_ items: [String]) -> String {
        "[" + items.map { "\"" + $0 + "\"" }.joined(separator: ",") + "]"
    }
}
