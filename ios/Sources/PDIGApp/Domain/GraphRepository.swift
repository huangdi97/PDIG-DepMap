// GraphRepository —— App 持久化边界。
//
// 包装 PDIGCore 的 SqliteDriver 协议：
//  - 图数据：GraphSnapshot ↔ canonical payload（importGraph / 直接 SELECT）
//  - ChangePlan / Drift / Candidate：schema v3 的专用表 + JSON 列
//
// 诚实边界：本文件不引入 SQLCipher。SQLCipher 持久化在 iOS 侧
// IOS_SQLCIPHER_PERSISTENCE = NOT_RUN；App 落盘用系统 sqlite（同 host harness
// 口径），at-rest 加密由平台安全层后续接入（见 Platform/SqliteProvider.swift）。

import Foundation
import PDIGCore

/// ChangePlan 的 JSON 编解码（schema v3 change_plans 表）。
public enum PlanJson {
    public static func toJson(_ plan: ChangePlan) -> Json {
        .obj(JsonObject([
            ("id", .str(plan.id)),
            ("template_id", plan.templateId.map { .str($0) } ?? .null),
            ("scenario", .str(plan.scenario)),
            ("title", .str(plan.title)),
            ("workflow_state", .str(plan.workflowState.wire)),
            ("baseline_graph_revision", .num(String(plan.baselineGraphRevision))),
            ("last_analyzed_graph_revision", .num(String(plan.lastAnalyzedGraphRevision))),
            ("target_node_id", plan.targetNodeId.map { .str($0) } ?? .null),
            ("effective_date", plan.effectiveDate.map { .str($0) } ?? .null),
            ("params_json", .str("{}")),
            ("impact_snapshot_json", .null),
            ("action_items_json", .str(JsonWriter.write(.arr(plan.actions.map { actionJson($0) })))),
            ("created_at", .str("1970-01-01T00:00:00.000Z")),
            ("updated_at", .str("1970-01-01T00:00:00.000Z")),
        ]))
    }

    public static func actionJson(_ a: PlanAction) -> Json {
        var fields: [(String, Json)] = [
            ("id", .str(a.id)),
            ("title", .str(a.title)),
            ("phase", .str(a.phase.wire)),
            ("done", .num(a.done ? "1" : "0")),
            ("resolves_impact_keys", .arr(a.resolvesImpactKeys.map { .str($0) })),
        ]
        if let v = a.verification {
            fields.append(("verification", .obj(JsonObject([
                ("method", .str(v.method.wire)),
                ("status", .str(v.status.wire)),
                ("evidence_refs", .arr(v.evidenceRefs.map { .str($0) })),
                ("expected_from_node_id", v.expectedFromNodeId.map { .str($0) } ?? .null),
                ("expected_to_node_id", v.expectedToNodeId.map { .str($0) } ?? .null),
            ]))))
        }
        return .obj(JsonObject(fields))
    }

    public static func parsePlan(_ o: JsonObject) throws -> ChangePlan {
        let wfRaw = o["workflow_state"]?.stringValue ?? ChangePlanWorkflowState.draft.wire
        guard let wf = ChangePlanWorkflowState(rawValue: wfRaw) else {
            throw GraphRepositoryError("unknown workflow_state \(wfRaw)")
        }
        let actions = parseActions(o["action_items_json"]?.stringValue ?? "[]")
        let rev = Int(o["last_analyzed_graph_revision"]?.stringValue ?? "0") ?? 0
        return ChangePlan(
            id: o["id"]?.stringValue ?? "",
            templateId: o["template_id"]?.stringValue,
            scenario: o["scenario"]?.stringValue ?? "",
            title: o["title"]?.stringValue ?? "",
            workflowState: wf,
            baselineGraphRevision: Int(o["baseline_graph_revision"]?.stringValue ?? "0") ?? 0,
            lastAnalyzedGraphRevision: rev,
            targetNodeId: o["target_node_id"]?.stringValue,
            effectiveDate: o["effective_date"]?.stringValue,
            actions: actions
        )
    }

    private static func parseActions(_ json: String) -> [PlanAction] {
        guard let parsed = try? JsonParser.parse(json),
              case .arr(let items) = parsed else { return [] }
        var out: [PlanAction] = []
        for item in items {
            guard case .obj(let o) = item else { continue }
            guard let phaseRaw = o["phase"]?.stringValue,
                  let phase = PlanActionPhase(rawValue: phaseRaw) else { continue }
            var verification: ActionVerification? = nil
            if let v = o["verification"]?.objectValue {
                verification = ActionVerification(
                    method: ActionVerificationMethod(rawValue: v["method"]?.stringValue ?? "manual_confirmation") ?? .manualConfirmation,
                    status: ActionVerificationStatus(rawValue: v["status"]?.stringValue ?? "pending") ?? .pending,
                    evidenceRefs: (v["evidence_refs"]?.arrayValue ?? []).compactMap { $0.stringValue },
                    expectedFromNodeId: v["expected_from_node_id"]?.stringValue,
                    expectedToNodeId: v["expected_to_node_id"]?.stringValue
                )
            }
            out.append(PlanAction(
                id: o["id"]?.stringValue ?? "",
                title: o["title"]?.stringValue ?? "",
                phase: phase,
                done: o["done"]?.stringValue == "1",
                resolvesImpactKeys: (o["resolves_impact_keys"]?.arrayValue ?? []).compactMap { $0.stringValue },
                verification: verification
            ))
        }
        return out
    }
}

public struct GraphRepositoryError: Error, CustomStringConvertible {
    public let message: String
    public init(_ message: String) { self.message = message }
    public var description: String { message }
}

/// 图 + 计划 + 漂移 + 候选 的统一仓储。
public final class GraphRepository {
    private let driver: SqliteDriver

    public init(driver: SqliteDriver, nowIso: String = "1970-01-01T00:00:00.000Z") throws {
        self.driver = driver
        try SchemaMigrator.migrate(driver, nowIso: nowIso)
    }

    public var isClosed: Bool = false

    // MARK: - graph

    public func loadSnapshot() throws -> GraphSnapshot {
        let nodes = try loadNodes()
        let deps = try loadDependencies()
        let groups = try loadGroups()
        let proposals = try loadProposals()
        let sources = try loadSources()
        let revision = try loadRevision()
        return GraphSnapshot(
            nodes: nodes, dependencies: deps, groups: groups,
            pendingProposals: proposals, sources: sources, graphRevision: revision
        )
    }

    /// 直接以 canonical payload 原子替换（恢复备份用；importGraph 语义）。
    @discardableResult
    public func replacePayload(_ payloadJson: String) throws -> [String: Int] {
        try importGraph(driver, payloadJson)
    }

    /// 替换式写回：payload 校验 + 单事务原子替换（importGraph 语义）。
    @discardableResult
    public func replaceGraph(_ snapshot: GraphSnapshot) throws -> [String: Int] {
        try importGraph(driver, PayloadCodec.toPayloadJson(snapshot))
    }

    /// 图变更后 revision +1（与 Reality mutation 同事务，GraphRevisionMachine 语义）。
    public func bumpRevisionIfNeeded(mutation: String) throws {
        if !GraphRevision.doesMutationBumpRevision(mutation) { return }
        let current = try loadRevision()
        let next = GraphRevision.nextRevision(current)
        try driver.prepare(
            "INSERT INTO meta (key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value"
        ).run([.text(GraphRevision.graphRevisionKey), .text(String(next))])
    }

    public func deleteAllData() throws {
        let tables = ["change_plans", "reality_drifts", "discovery_candidates", "meta",
                      "source_instances", "nodes", "dependencies", "dependency_groups",
                      "dependency_proposals", "proposal_evidence_refs",
                      "dependency_group_proposals", "evidence", "observation_fingerprints",
                      "import_sessions"]
        try driver.transaction {
            for t in tables {
                try driver.exec("DELETE FROM \(t)")
            }
            try driver.prepare(
                "INSERT INTO meta (key, value) VALUES (?, ?)"
            ).run([.text(GraphRevision.graphRevisionKey), .text("0")])
        }
    }

    // MARK: - change plans

    public func loadPlans() throws -> [ChangePlan] {
        guard let rows = try? driver.prepare("SELECT * FROM change_plans").all([]) else { return [] }
        var plans: [ChangePlan] = []
        for row in rows {
            guard let json = row.str("action_items_json") else { continue }
            guard let parsed = try? JsonParser.parse(json) else { continue }
            guard case .obj(let o) = parsed else { continue }
            var fields = o.fields
            fields.append(("id", .str(row.str("id") ?? "")))
            fields.append(("template_id", row.str("template_id").map { .str($0) } ?? .null))
            fields.append(("scenario", .str(row.str("scenario") ?? "")))
            fields.append(("title", .str(row.str("title") ?? "")))
            fields.append(("workflow_state", .str(row.str("workflow_state") ?? "")))
            fields.append(("baseline_graph_revision", .num(row.str("baseline_graph_revision") ?? "0")))
            fields.append(("last_analyzed_graph_revision", .num(row.str("last_analyzed_graph_revision") ?? "0")))
            fields.append(("target_node_id", row.str("target_node_id").map { .str($0) } ?? .null))
            fields.append(("effective_date", row.str("effective_date").map { .str($0) } ?? .null))
            if let plan = try? PlanJson.parsePlan(JsonObject(fields)) {
                plans.append(plan)
            }
        }
        return plans.sorted { $0.id < $1.id }
    }

    public func savePlan(_ plan: ChangePlan) throws {
        let j = PlanJson.toJson(plan)
        guard let o = j.objectValue else { throw GraphRepositoryError("plan serialization failed") }
        let actionJson = o["action_items_json"]?.stringValue ?? "[]"
        try driver.prepare(
            """
            INSERT INTO change_plans (id, template_id, scenario, title, workflow_state,
                baseline_graph_revision, last_analyzed_graph_revision, target_node_id,
                effective_date, params_json, impact_snapshot_json, action_items_json,
                created_at, updated_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT(id) DO UPDATE SET title=excluded.title, workflow_state=excluded.workflow_state,
                last_analyzed_graph_revision=excluded.last_analyzed_graph_revision,
                effective_date=excluded.effective_date, action_items_json=excluded.action_items_json,
                updated_at=excluded.updated_at
            """
        ).run([
            .text(plan.id),
            .text(plan.templateId ?? ""),
            .text(plan.scenario),
            .text(plan.title),
            .text(plan.workflowState.wire),
            .integer(Int64(plan.baselineGraphRevision)),
            .integer(Int64(plan.lastAnalyzedGraphRevision)),
            plan.targetNodeId.map { .text($0) } ?? .null,
            plan.effectiveDate.map { .text($0) } ?? .null,
            .text("{}"),
            .null,
            .text(actionJson),
            .text("1970-01-01T00:00:00.000Z"),
            .text("1970-01-01T00:00:00.000Z"),
        ])
    }

    public func deletePlan(id: String) throws {
        try driver.prepare("DELETE FROM change_plans WHERE id = ?").run([.text(id)])
    }

    // MARK: - reality drifts（开放漂移，Home / Timeline 使用）

    public struct DriftRow: Equatable, Sendable {
        public let id: String
        public let kind: String
        public let targetNodeId: String
        public let capability: String
        public let status: String
        public let detectedAt: String
        public let observationCount: Int
    }

    public func loadOpenDrifts() throws -> [DriftRow] {
        guard let rows = try? driver.prepare(
            "SELECT id, kind, target_node_id, capability, status, detected_at, observation_count FROM reality_drifts WHERE status = ?"
        ).all([.text(DriftStatus.`open`.wire)]) else { return [] }
        return rows.map { row in
            DriftRow(
                id: row.str("id") ?? "",
                kind: row.str("kind") ?? "",
                targetNodeId: row.str("target_node_id") ?? "",
                capability: row.str("capability") ?? Capability.payment.wire,
                status: row.str("status") ?? DriftStatus.`open`.wire,
                detectedAt: row.str("detected_at") ?? "",
                observationCount: Int(row.long("observation_count") ?? 0)
            )
        }
    }

    public func insertDrift(_ d: DriftRow, relatedDependencyIds: [String], evidenceRefs: [String]) throws {
        try driver.prepare(
            """
            INSERT INTO reality_drifts (id, kind, target_node_id, capability, candidate_from,
                candidate_relation, related_dependency_ids_json, evidence_refs_json,
                proposal_keys_json, observation_count, detected_at, updated_at, status)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
            """
        ).run([
            .text(d.id), .text(d.kind), .text(d.targetNodeId), .text(d.capability),
            .null, .text(Relation.fundingSource.wire),
            .text(jsonArray(relatedDependencyIds)), .text(jsonArray(evidenceRefs)),
            .text("[]"), .integer(Int64(d.observationCount)),
            .text(d.detectedAt), .text(d.detectedAt), .text(d.status),
        ])
    }

    // MARK: - discovery candidates

    public struct CandidateRow: Equatable, Sendable {
        public let id: String
        public let candidateKind: String
        public let displayLabel: String
        public let normalizedKey: String
        public let observationCount: Int
        public let status: String
        public let firstSeenAt: String
        public let lastSeenAt: String
    }

    public func loadPendingCandidates() throws -> [CandidateRow] {
        guard let rows = try? driver.prepare(
            "SELECT id, candidate_kind, display_label, normalized_key, observation_count, status, first_seen_at, last_seen_at FROM discovery_candidates WHERE status = ?"
        ).all([.text(CandidateStatus.pending.wire)]) else { return [] }
        return rows.map { row in
            CandidateRow(
                id: row.str("id") ?? "",
                candidateKind: row.str("candidate_kind") ?? "",
                displayLabel: row.str("display_label") ?? "",
                normalizedKey: row.str("normalized_key") ?? "",
                observationCount: Int(row.long("observation_count") ?? 0),
                status: row.str("status") ?? CandidateStatus.pending.wire,
                firstSeenAt: row.str("first_seen_at") ?? "",
                lastSeenAt: row.str("last_seen_at") ?? ""
            )
        }
    }

    public func upsertCandidate(_ c: CandidateRow, evidenceRefs: [String]) throws {
        try driver.prepare(
            """
            INSERT INTO discovery_candidates (id, candidate_kind, display_label, normalized_key,
                source_instance_id, evidence_refs_json, observation_count, first_seen_at,
                last_seen_at, dismissed_at_observation_count, accepted_node_id, status,
                created_at, updated_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT(normalized_key) DO UPDATE SET observation_count=excluded.observation_count,
                last_seen_at=excluded.last_seen_at, evidence_refs_json=excluded.evidence_refs_json,
                updated_at=excluded.updated_at
            """
        ).run([
            .text(c.id), .text(c.candidateKind), .text(c.displayLabel), .text(c.normalizedKey),
            .text("source-csv"), .text(jsonArray(evidenceRefs)),
            .integer(Int64(c.observationCount)), .text(c.firstSeenAt), .text(c.lastSeenAt),
            .null, .null, .text(c.status),
            .text(c.firstSeenAt), .text(c.lastSeenAt),
        ])
    }

    // MARK: - private loaders

    private func loadRevision() throws -> Int {
        guard let row = try driver.prepare("SELECT value FROM meta WHERE key = ?").get([.text(GraphRevision.graphRevisionKey)]) else {
            return 0
        }
        return GraphRevision.parseStoredRevision(row.str("value"))
    }

    private func loadNodes() throws -> [DepNode] {
        guard let rows = try? driver.prepare("SELECT id, kind, name, issuer, last4, archived FROM nodes").all([]) else { return [] }
        return rows.compactMap { row in
            guard let kind = NodeKind(rawValue: row.str("kind") ?? "") else { return nil }
            return DepNode(
                id: row.str("id") ?? "",
                kind: kind,
                name: row.str("name") ?? "",
                issuer: row.str("issuer"),
                last4: row.str("last4"),
                archived: row.str("archived") == "1"
            )
        }
    }

    private func loadDependencies() throws -> [Dependency] {
        guard let rows = try? driver.prepare(
            "SELECT id, from_node, relation, to_node, capability, criticality, group_id, state, origin, last_verified_at FROM dependencies"
        ).all([]) else { return [] }
        return rows.compactMap { row in
            guard let relation = Relation(rawValue: row.str("relation") ?? ""),
                  let capability = Capability(rawValue: row.str("capability") ?? ""),
                  let criticality = Criticality(rawValue: row.str("criticality") ?? ""),
                  let state = DependencyState(rawValue: row.str("state") ?? ""),
                  let origin = DependencyOrigin(rawValue: row.str("origin") ?? "") else { return nil }
            return Dependency(
                id: row.str("id") ?? "",
                from: row.str("from_node") ?? "",
                relation: relation,
                to: row.str("to_node") ?? "",
                capability: capability,
                criticality: criticality,
                groupId: row.str("group_id"),
                state: state,
                origin: origin,
                lastVerifiedAt: row.str("last_verified_at") ?? ""
            )
        }
    }

    private func loadGroups() throws -> [DependencyGroup] {
        guard let rows = try? driver.prepare(
            "SELECT id, group_key, target_node_id, capability, mode, member_edge_ids_json, state FROM dependency_groups"
        ).all([]) else { return [] }
        return rows.compactMap { row in
            guard let capability = Capability(rawValue: row.str("capability") ?? ""),
                  let mode = GroupMode(rawValue: row.str("mode") ?? ""),
                  let state = GroupState(rawValue: row.str("state") ?? "") else { return nil }
            let members = parseJsonArray(row.str("member_edge_ids_json"))
            return DependencyGroup(
                id: row.str("id") ?? "",
                groupKey: row.str("group_key") ?? "",
                targetNodeId: row.str("target_node_id") ?? "",
                capability: capability,
                mode: mode,
                memberEdgeIds: members,
                state: state
            )
        }
    }

    private func loadProposals() throws -> [PendingProposal] {
        guard let rows = try? driver.prepare(
            "SELECT id, key, from_node, relation, to_node, capability, proposal_type, source, confidence_score, decision, decided_at, observation_count, created_at, updated_at FROM dependency_proposals"
        ).all([]) else { return [] }
        return rows.compactMap { row in
            guard let relation = Relation(rawValue: row.str("relation") ?? ""),
                  let capability = Capability(rawValue: row.str("capability") ?? ""),
                  let decision = ProposalDecision(rawValue: row.str("decision") ?? ProposalDecision.pending.wire) else { return nil }
            return PendingProposal(
                id: row.str("id") ?? "",
                key: row.str("key") ?? "",
                from: row.str("from_node") ?? "",
                relation: relation,
                to: row.str("to_node") ?? "",
                capability: capability,
                proposalType: row.str("proposal_type") ?? ProposalType.recurringPaymentRoute.wire,
                source: row.str("source") ?? ProposalSource.statement.wire,
                confidenceScore: row.double("confidence_score"),
                decision: decision,
                decidedAt: row.str("decided_at"),
                observationCount: Int(row.long("observation_count") ?? 0),
                createdAt: row.str("created_at") ?? "",
                updatedAt: row.str("updated_at") ?? ""
            )
        }
    }

    private func loadSources() throws -> [SourceInstanceRow] {
        guard let rows = try? driver.prepare(
            "SELECT id, adapter_id, adapter_version, source_kind, label, state, last_ingested_at, created_at, updated_at FROM source_instances"
        ).all([]) else { return [] }
        return rows.map { row in
            SourceInstanceRow(
                id: row.str("id") ?? "",
                adapterId: row.str("adapter_id") ?? "",
                adapterVersion: Int(row.long("adapter_version") ?? 1),
                sourceKind: row.str("source_kind") ?? SourceKind.statementFile.wire,
                label: row.str("label") ?? "",
                state: row.str("state") ?? SourceInstanceState.active.wire,
                lastIngestedAt: row.str("last_ingested_at"),
                createdAt: row.str("created_at") ?? "",
                updatedAt: row.str("updated_at") ?? ""
            )
        }
    }

    private func jsonArray(_ items: [String]) -> String {
        "[" + items.map { "\"" + $0 + "\"" }.joined(separator: ",") + "]"
    }

    private func parseJsonArray(_ raw: String?) -> [String] {
        guard let raw = raw, let parsed = try? JsonParser.parse(raw),
              case .arr(let items) = parsed else { return [] }
        return items.compactMap { $0.stringValue }
    }
}
