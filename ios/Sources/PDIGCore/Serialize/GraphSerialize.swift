// 逻辑图序列化 —— `.depmap` 备份 payload 层。
// 移植自 android/core/.../serialize/GraphSerialize.kt（Android 已 CORE_FROZEN）。
//
// 关键分离：**crypto 容器 formatVersion（V1）与 payload schemaVersion（3）互相独立**
//  - export：读取全部持久化实体 → payload JSON (schemaVersion=3)
//  - import：payloadVersion/schemaVersion 校验（未来版本明确拒绝）→
//    必要时 in-memory migrate → 完整校验 → 单事务原子替换；失败回滚
//  - 等价性：export → import → export 逐字节一致

import Foundation

public let GRAPH_PAYLOAD_KIND = "depmap-logical-graph"
public let GRAPH_PAYLOAD_VERSION = 3

public struct GraphImportError: Error, CustomStringConvertible {
    public let message: String
    public init(_ message: String) { self.message = message }
    public var description: String { message }
}

public struct PayloadTable: Sendable {
    public let table: String
    public let columns: [String]
    public init(_ table: String, _ columns: [String]) {
        self.table = table
        self.columns = columns
    }
}

public let PAYLOAD_TABLES: [PayloadTable] = [
    PayloadTable("meta", ["key", "value"]),
    PayloadTable(
        "source_instances",
        [
            "id", "adapter_id", "adapter_version", "source_kind", "provider_id", "account_node_id",
            "label", "country", "jurisdiction", "currencies_json", "state", "created_at",
            "updated_at", "last_ingested_at",
        ]
    ),
    PayloadTable(
        "nodes",
        [
            "id", "kind", "template_id", "name", "issuer", "last4", "owner", "archived",
            "fields_json", "vault_ref", "wallet_ref", "created_at", "updated_at",
        ]
    ),
    PayloadTable(
        "dependencies",
        [
            "id", "from_node", "relation", "to_node", "capability", "criticality", "group_id",
            "state", "origin", "confirmed_at", "last_verified_at", "retired_at",
            "evidence_refs_json", "verification_basis_type", "verification_basis_json",
            "created_at", "updated_at",
        ]
    ),
    PayloadTable(
        "dependency_groups",
        [
            "id", "group_key", "target_node_id", "capability", "mode", "member_edge_ids_json",
            "state", "confirmed_at", "last_verified_at", "verification_basis_type",
            "verification_basis_json", "created_at", "updated_at",
        ]
    ),
    PayloadTable(
        "dependency_proposals",
        [
            "id", "key", "from_node", "relation", "to_node", "capability", "proposal_type",
            "source", "parser_id", "parser_version", "confidence_score", "path_json", "decision",
            "decided_at", "criticality_decision", "observation_count", "rejected_at",
            "rejected_at_stream_counts_json", "created_at", "updated_at",
        ]
    ),
    PayloadTable("proposal_evidence_refs", ["proposal_key", "evidence_id", "position"]),
    PayloadTable(
        "dependency_group_proposals",
        [
            "id", "key", "target_node_id", "capability", "mode", "member_dependency_keys_json",
            "decision", "decided_at", "rejected_at", "rejected_at_observation_count",
            "created_at", "updated_at",
        ]
    ),
    PayloadTable(
        "evidence",
        [
            "id", "proposal_key", "source_instance_id", "adapter_id", "adapter_version",
            "evidence_kind", "source_type", "parser_id", "parser_version",
            "last_import_session_id", "first_observed_at", "last_observed_at",
            "observation_count", "created_at", "updated_at",
        ]
    ),
    PayloadTable(
        "observation_fingerprints",
        [
            "fingerprint", "source_instance_id", "source", "fingerprint_version",
            "import_session_id", "first_seen_at",
        ]
    ),
    PayloadTable(
        "import_sessions",
        [
            "id", "source_type", "parser_id", "parser_version", "source_instance_id",
            "adapter_id", "adapter_version", "started_at", "completed_at", "raw_count",
            "new_unique_count", "duplicate_count", "proposal_count", "error_count",
        ]
    ),
]

private let DELETE_ORDER: [String] = [
    "proposal_evidence_refs",
    "dependency_groups",
    "dependencies",
    "dependency_group_proposals",
    "dependency_proposals",
    "evidence",
    "observation_fingerprints",
    "import_sessions",
    "source_instances",
    "nodes",
    "meta",
]

// ---------------------------------------------------------------------------
// 值序列化（必须与 JS JSON.stringify 的 number 输出对齐）
// ---------------------------------------------------------------------------

/// 已知局限：REAL 列的文本化暂用 Swift 描述格式。当前 payload 表内所有
/// 实际出现的值都是 INTEGER / TEXT / NULL（confidence_score 是唯一 REAL 列，
/// 冻结 fixture 的 dependency_proposals 为空），因此不影响跨端等价性判定。
/// 若将来 fixture 覆盖 REAL，需改为 Java/JS 双精度格式化后再比对。
internal func formatSqlReal(_ v: Double) -> String {
    if v == v.rounded(.towardZero) && abs(v) < 1e15 {
        return String(Int64(v))
    }
    return String(v)
}

internal func sqlValueToJson(_ v: SqlValue?) -> Json {
    guard let v = v else { return .null }
    switch v {
    case .null: return .null
    case .integer(let i): return .num(String(i))
    case .real(let d): return .num(formatSqlReal(d))
    case .text(let s): return .str(s)
    case .blob(let b): return .str(String(decoding: b, as: UTF8.self))
    }
}

internal func jsonToSqlValue(_ v: Json) throws -> SqlValue {
    switch v {
    case .null: return .null
    case .str(let s): return .text(s)
    case .bool(let b): return .integer(b ? 1 : 0)
    case .num(let raw):
        if let i = Int64(raw) { return .integer(i) }
        if let d = Double(raw) { return .real(d) }
        throw GraphImportError("bad number: \(raw)")
    default:
        throw GraphImportError("unsupported payload value")
    }
}

// ---------------------------------------------------------------------------
// export
// ---------------------------------------------------------------------------

public struct GraphExportResult: Sendable {
    public let payloadJson: String
    public let counts: [String: Int]
    public init(payloadJson: String, counts: [String: Int]) {
        self.payloadJson = payloadJson
        self.counts = counts
    }
}

public func exportGraph(_ driver: SqliteDriver) throws -> GraphExportResult {
    var fields: [(String, Json)] = [
        ("payloadKind", .str(GRAPH_PAYLOAD_KIND)),
        ("payloadVersion", .num(String(GRAPH_PAYLOAD_VERSION))),
        ("schemaVersion", .num(String(Migrations.schemaVersion))),
    ]
    var counts: [String: Int] = [:]
    for t in PAYLOAD_TABLES {
        let rows = try driver.prepare("SELECT * FROM \(t.table)").all([])
        counts[t.table] = rows.count
        fields.append((t.table, .arr(rows.map { rowToJson($0) })))
    }
    return GraphExportResult(payloadJson: JsonWriter.write(.obj(JsonObject(fields))), counts: counts)
}

private func rowToJson(_ row: SqliteRow) -> Json {
    .obj(JsonObject(row.columns().map { col in (col, sqlValueToJson(row.raw(col))) }))
}

// ---------------------------------------------------------------------------
// payload 版本迁移（纯函数，不触碰 DB）
// ---------------------------------------------------------------------------

public func migratePayloadV1toV2(_ payloadJson: String) throws -> String {
    let parsed = try parsePayloadObject(payloadJson)
    guard parsed["payloadKind"]?.stringValue == GRAPH_PAYLOAD_KIND else {
        throw GraphImportError("payloadKind must be \(GRAPH_PAYLOAD_KIND)")
    }
    guard let versionRaw = parsed["payloadVersion"], case .num(let raw) = versionRaw,
          let version = Int(raw), version == 1 else {
        throw GraphImportError("migratePayloadV1toV2 expects payloadVersion 1")
    }
    let legacyNow = "1970-01-01T00:00:00.000Z"
    let legacyInstance: Json = .obj(JsonObject([
        ("id", .str(Migrations.legacyWechatSourceInstanceId)),
        ("adapter_id", .str(Migrations.legacyWechatAdapterId)),
        ("adapter_version", .num(String(Migrations.legacyWechatAdapterVersion))),
        ("source_kind", .str("statement_file")),
        ("provider_id", .null),
        ("account_node_id", .null),
        ("label", .str("Legacy WeChat Statement Source")),
        ("country", .null),
        ("jurisdiction", .null),
        ("currencies_json", .str(#"["CNY"]"#)),
        ("state", .str("active")),
        ("created_at", .str(legacyNow)),
        ("updated_at", .str(legacyNow)),
        ("last_ingested_at", .null),
    ]))

    let fingerprints = try rowsOf(parsed, "observation_fingerprints").map { (r: JsonObject) -> Json in
        .obj(JsonObject([
            ("fingerprint", r["fingerprint"] ?? .null),
            ("source_instance_id", .str(Migrations.legacyWechatSourceInstanceId)),
            ("source", .str(r["source"]?.stringValue ?? "")),
            ("fingerprint_version", r["fingerprint_version"] ?? .num("1")),
            ("import_session_id", r["import_session_id"] ?? .null),
            ("first_seen_at", r["first_seen_at"] ?? .null),
        ]))
    }
    let evidence = try rowsOf(parsed, "evidence").map { (r: JsonObject) -> Json in
        var adapterVersion: Json = .num("1")
        if case .num(let raw) = r["parser_version"] ?? .null, let l = Int64(raw) {
            adapterVersion = .num(String(l))
        }
        return .obj(JsonObject([
            ("id", r["id"] ?? .null),
            ("proposal_key", r["proposal_key"] ?? .null),
            ("source_instance_id", .str(Migrations.legacyWechatSourceInstanceId)),
            ("adapter_id", .str(Migrations.legacyWechatAdapterId)),
            ("adapter_version", adapterVersion),
            ("evidence_kind", .str("transaction_stream")),
            ("source_type", r["source_type"] ?? .null),
            ("parser_id", r["parser_id"] ?? .null),
            ("parser_version", r["parser_version"] ?? .null),
            ("last_import_session_id", r["last_import_session_id"] ?? .null),
            ("first_observed_at", r["first_observed_at"] ?? .null),
            ("last_observed_at", r["last_observed_at"] ?? .null),
            ("observation_count", r["observation_count"] ?? .num("0")),
            ("created_at", r["created_at"] ?? .null),
            ("updated_at", r["updated_at"] ?? .null),
        ]))
    }
    let v1Proposals = try rowsOf(parsed, "dependency_proposals")
    let proposals: [Json] = v1Proposals.map { r in
        var f = r.fields.filter { $0.0 != "evidence_id" }
        f.append(("rejected_at_stream_counts_json", .null))
        return .obj(JsonObject(f))
    }
    var evidenceRefs: [Json] = []
    for (i, r) in v1Proposals.enumerated() {
        guard let ev = r["evidence_id"], !ev.isNull else { continue }
        evidenceRefs.append(.obj(JsonObject([
            ("proposal_key", r["key"] ?? .null),
            ("evidence_id", ev),
            ("position", .num(String(i))),
        ])))
    }
    let sessions: [Json] = try rowsOf(parsed, "import_sessions").map { r in
        var f = r.fields
        f.append(("source_instance_id", .str(Migrations.legacyWechatSourceInstanceId)))
        f.append(("adapter_id", .str(Migrations.legacyWechatAdapterId)))
        f.append(("adapter_version", .num(String(Migrations.legacyWechatAdapterVersion))))
        return .obj(JsonObject(f))
    }
    let deps = try rowsOf(parsed, "dependencies").map { withVerificationBasis($0) }
    let groups = try rowsOf(parsed, "dependency_groups").map { withVerificationBasis($0) }

    let v2 = JsonObject([
        ("payloadKind", .str(GRAPH_PAYLOAD_KIND)),
        ("payloadVersion", .num("2")),
        ("schemaVersion", .num("2")),
        ("meta", parsed["meta"] ?? .arr([])),
        ("source_instances", .arr([legacyInstance])),
        ("nodes", parsed["nodes"] ?? .arr([])),
        ("dependencies", .arr(deps)),
        ("dependency_groups", .arr(groups)),
        ("dependency_proposals", .arr(proposals)),
        ("proposal_evidence_refs", .arr(evidenceRefs)),
        ("dependency_group_proposals", parsed["dependency_group_proposals"] ?? .arr([])),
        ("evidence", .arr(evidence)),
        ("observation_fingerprints", .arr(fingerprints)),
        ("import_sessions", .arr(sessions)),
    ])
    return JsonWriter.write(.obj(v2))
}

private func withVerificationBasis(_ r: JsonObject) -> Json {
    var f = r.fields
    if r["verification_basis_type"] == nil {
        f.append(("verification_basis_type", .str("user_confirmed")))
    }
    if r["verification_basis_json"] == nil {
        f.append(("verification_basis_json", .null))
    }
    return .obj(JsonObject(f))
}

public func migratePayloadV2toV3(_ payloadJson: String) throws -> String {
    let parsed = try parsePayloadObject(payloadJson)
    guard let versionRaw = parsed["payloadVersion"], case .num(let raw) = versionRaw,
          let version = Int(raw), version == 2 else {
        throw GraphImportError("migratePayloadV2toV3 expects payloadVersion 2")
    }
    let meta = try rowsOf(parsed, "meta")
    let hasRevision = meta.contains { $0["key"]?.stringValue == "graph_revision" }
    var newMeta: [Json] = meta.map { Json.obj($0) }
    if !hasRevision {
        newMeta.append(.obj(JsonObject([
            ("key", .str("graph_revision")),
            ("value", .str("0")),
        ])))
    }
    let v3 = JsonObject([
        ("payloadKind", .str(GRAPH_PAYLOAD_KIND)),
        ("payloadVersion", .num(String(GRAPH_PAYLOAD_VERSION))),
        ("schemaVersion", .num(String(Migrations.schemaVersion))),
        ("meta", .arr(newMeta)),
        ("source_instances", parsed["source_instances"] ?? .arr([])),
        ("nodes", parsed["nodes"] ?? .arr([])),
        ("dependencies", parsed["dependencies"] ?? .arr([])),
        ("dependency_groups", parsed["dependency_groups"] ?? .arr([])),
        ("dependency_proposals", parsed["dependency_proposals"] ?? .arr([])),
        ("proposal_evidence_refs", parsed["proposal_evidence_refs"] ?? .arr([])),
        ("dependency_group_proposals", parsed["dependency_group_proposals"] ?? .arr([])),
        ("evidence", parsed["evidence"] ?? .arr([])),
        ("observation_fingerprints", parsed["observation_fingerprints"] ?? .arr([])),
        ("import_sessions", parsed["import_sessions"] ?? .arr([])),
    ])
    return JsonWriter.write(.obj(v3))
}

public func migrateIfNeeded(_ payloadJson: String) throws -> String {
    let parsed = try JsonParser.parse(payloadJson)
    if case .obj(let o) = parsed, case .num(let raw) = o["payloadVersion"] ?? .null, let v = Int(raw) {
        switch v {
        case 1: return try migratePayloadV2toV3(migratePayloadV1toV2(payloadJson))
        case 2: return try migratePayloadV2toV3(payloadJson)
        default: break
        }
    }
    return payloadJson
}

private func parsePayloadObject(_ payloadJson: String) throws -> JsonObject {
    let parsed: Json
    do {
        parsed = try JsonParser.parse(payloadJson)
    } catch {
        throw GraphImportError("payload is not valid JSON")
    }
    guard case .obj(let o) = parsed else { throw GraphImportError("payload must be an object") }
    return o
}

private func rowsOf(_ payload: JsonObject, _ table: String) throws -> [JsonObject] {
    guard let v = payload[table] else { return [] }
    guard case .arr(let items) = v else { throw GraphImportError("payload.\(table) must be an array") }
    var out: [JsonObject] = []
    for it in items {
        guard case .obj(let o) = it else {
            throw GraphImportError("payload.\(table) rows must be objects")
        }
        out.append(o)
    }
    return out
}

// ---------------------------------------------------------------------------
// import
// ---------------------------------------------------------------------------

private func tableRows(_ payload: JsonObject, _ table: String, _ required: [String]) throws -> [JsonObject] {
    let rows = try rowsOf(payload, table)
    for row in rows {
        for col in required {
            if !row.has(col) { throw GraphImportError("payload.\(table) row missing column \(col)") }
        }
    }
    return rows
}

private func validatePayload(_ payloadJson: String) throws -> JsonObject {
    let payload = try parsePayloadObject(payloadJson)
    guard payload["payloadKind"]?.stringValue == GRAPH_PAYLOAD_KIND else {
        throw GraphImportError("payloadKind must be \(GRAPH_PAYLOAD_KIND)")
    }
    guard case .num(let pvRaw) = payload["payloadVersion"] ?? .null, let pv = Int(pvRaw) else {
        throw GraphImportError("payloadVersion must be an integer")
    }
    if pv != GRAPH_PAYLOAD_VERSION {
        throw GraphImportError("unsupported payloadVersion: \(pv) (expected \(GRAPH_PAYLOAD_VERSION))")
    }
    guard let svRaw = payload["schemaVersion"], case .num(let svText) = svRaw else {
        throw GraphImportError("schemaVersion must be an integer")
    }
    guard let sv = Int(svText), Json.num(svText).isSafeInteger else {
        throw GraphImportError("schemaVersion must be an integer")
    }
    if sv > Migrations.schemaVersion {
        throw GraphImportError("payload schemaVersion (\(sv)) newer than supported (\(Migrations.schemaVersion))")
    }
    for t in PAYLOAD_TABLES {
        _ = try tableRows(payload, t.table, t.columns)
    }
    return payload
}

/// 原子导入：校验通过后单事务替换全部数据；任何失败回滚，不留半恢复状态。
public func importGraph(_ driver: SqliteDriver, _ payloadJson: String) throws -> [String: Int] {
    let normalized = try migrateIfNeeded(payloadJson)
    let payload = try validatePayload(normalized)
    return try driver.transaction {
        var imported: [String: Int] = [:]
        for table in DELETE_ORDER { try driver.exec("DELETE FROM \(table)") }
        for t in PAYLOAD_TABLES {
            let rows = try tableRows(payload, t.table, t.columns)
            if rows.isEmpty {
                imported[t.table] = 0
                continue
            }
            let placeholders = t.columns.map { _ in "?" }.joined(separator: ", ")
            let stmt = try driver.prepare(
                "INSERT INTO \(t.table) (\(t.columns.joined(separator: ", "))) VALUES (\(placeholders))"
            )
            for row in rows {
                var values: [SqlValue] = []
                for col in t.columns { values.append(try jsonToSqlValue(try row.require(col))) }
                try stmt.run(values)
            }
            imported[t.table] = rows.count
        }
        return imported
    }
}

// ---------------------------------------------------------------------------
// 完整性（孤儿检测）
// ---------------------------------------------------------------------------

public struct OrphanReport: Sendable {
    public let orphanDependencies: [String]
    public let orphanGroups: [String]
    public let danglingGroupMembers: [String]
    public let orphanEvidence: [String]
    public let orphanFingerprints: [String]
    public init(
        orphanDependencies: [String],
        orphanGroups: [String],
        danglingGroupMembers: [String],
        orphanEvidence: [String],
        orphanFingerprints: [String]
    ) {
        self.orphanDependencies = orphanDependencies
        self.orphanGroups = orphanGroups
        self.danglingGroupMembers = danglingGroupMembers
        self.orphanEvidence = orphanEvidence
        self.orphanFingerprints = orphanFingerprints
    }
}

public func checkGraphIntegrity(_ driver: SqliteDriver) throws -> OrphanReport {
    return OrphanReport(
        orphanDependencies: try driver.prepare(
            """
            SELECT d.id FROM dependencies d
             WHERE d.from_node NOT IN (SELECT id FROM nodes) OR d.to_node NOT IN (SELECT id FROM nodes)
            """
        ).all([]).map { $0.str("id") ?? "" },
        orphanGroups: try driver.prepare(
            """
            SELECT g.id FROM dependency_groups g WHERE g.target_node_id NOT IN (SELECT id FROM nodes)
            """
        ).all([]).map { $0.str("id") ?? "" },
        danglingGroupMembers: try driver.prepare(
            """
            SELECT g.id FROM dependency_groups g
             WHERE EXISTS (SELECT 1 FROM json_each(g.member_edge_ids_json) je
                            WHERE je.value NOT IN (SELECT id FROM dependencies))
            """
        ).all([]).map { $0.str("id") ?? "" },
        orphanEvidence: try driver.prepare(
            """
            SELECT e.id FROM evidence e WHERE e.source_instance_id NOT IN (SELECT id FROM source_instances)
            """
        ).all([]).map { $0.str("id") ?? "" },
        orphanFingerprints: try driver.prepare(
            """
            SELECT f.rowid FROM observation_fingerprints f
             WHERE f.source_instance_id NOT IN (SELECT id FROM source_instances)
            """
        ).all([]).map { $0.str("rowid") ?? "" }
    )
}
