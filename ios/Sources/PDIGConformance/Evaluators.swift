// canonical 用例求值器。
//
// 每个求值器把 fixture 的 `input` 喂给 **PDIGCore 的真实实现**，产出与
// `expected` 同形的 Json。**不做**任何"按 expected 反推输出"的镜像实现 ——
// 那样就是自比自，不构成证据。
//
// 尚未移植的分类返回 `.notImplemented` / `.blocked`，由 runner 如实记账，
// 不写成 PASS。

import Foundation
import PDIGCore

public enum EvalOutcome {
    case value(Json)
    case notImplemented
    case blocked(String)
}

public struct EvalError: Error, CustomStringConvertible, Equatable {
    public let message: String
    public init(_ message: String) { self.message = message }
    public var description: String { message }
}

public enum Evaluators {

    public static func evaluate(category: String, caseId: String, input: JsonObject) throws -> EvalOutcome {
        switch category {
        case "relations": return .value(try relations(input))
        case "jcs": return .value(try jcs(input))
        case "scenario": return .value(try scenario(input))
        case "timeline": return try timeline(caseId, input)
        case "migration": return try migration(caseId, input)
        default: return .notImplemented
        }
    }

    // ---------------------------------------------------------------- relations

    private static func relations(_ input: JsonObject) throws -> Json {
        let relation = input["relation"]?.stringValue ?? ""
        // group 用例的输入形如 {relation, mode}；use 用例形如
        // {id, fromKind, relation, toKind, capability}。按有无 mode 分流。
        if let modeRaw = input["mode"]?.stringValue {
            guard let mode = GroupMode(rawValue: modeRaw) else {
                throw EvalError("unknown group mode in fixture: \(modeRaw)")
            }
            let r = RelationRegistry.validateGroupUse(relation: relation, mode: mode)
            return resultJson(r)
        }
        guard let capability = input["capability"]?.stringValue else {
            throw EvalError("relations fixture missing capability")
        }
        let fromKind = try optionalNodeKind(input["fromKind"])
        let toKind = try optionalNodeKind(input["toKind"])
        let r = RelationRegistry.validateUse(
            fromKind: fromKind, relation: relation, toKind: toKind, capability: capability
        )
        return resultJson(r)
    }

    private static func optionalNodeKind(_ v: Json?) throws -> NodeKind? {
        guard let v = v else { return nil }
        if v.isNull { return nil }
        guard let s = v.stringValue else { throw EvalError("kind must be string or null") }
        guard let k = NodeKind(rawValue: s) else { throw EvalError("unknown NodeKind: \(s)") }
        return k
    }

    private static func resultJson(_ r: RelationValidationResult) -> Json {
        var fields: [(String, Json)] = [("ok", .bool(r.ok))]
        if let reason = r.reason { fields.append(("reason", .str(reason))) }
        return .obj(JsonObject(fields))
    }

    // --------------------------------------------------------------------- jcs

    private static func jcs(_ input: JsonObject) throws -> Json {
        func section(_ name: String, rejectMode: Bool) throws -> Json {
            guard let arr = input[name]?.arrayValue else { return .obj(JsonObject([])) }
            var out: [(String, Json)] = []
            for item in arr {
                guard let o = item.objectValue,
                      let id = o["id"]?.stringValue else { continue }
                let value = o["input"] ?? .null
                if rejectMode {
                    do {
                        _ = try Jcs.stringify(value)
                        // 受限域外的浮点被静默序列化 = 缺陷，必须 FAIL，不能记 Error。
                        out.append((id, .str("accepted")))
                    } catch is JcsError {
                        out.append((id, .str("Error")))
                    }
                } else {
                    let canonical = try Jcs.stringify(value)
                    out.append((id, .str(canonical)))
                }
            }
            return .obj(JsonObject(out))
        }
        return .obj(JsonObject([
            ("canonical", try section("cases", rejectMode: false)),
            ("rejections", try section("rejectCases", rejectMode: true)),
        ]))
    }

    // ---------------------------------------------------------------- scenario

    private static func scenario(_ input: JsonObject) throws -> Json {
        // 政策 gate：fixture 声明的排除域必须与注册表逐条一致。
        // 注册表悄悄多出一个"浇花提醒"模板时，这里 FAIL —— 这是本用例真正的牙齿。
        if let declared = input["excludedDomains"]?.arrayValue?.compactMap({ $0.stringValue }) {
            let platform = ScenarioRegistry.excludedDomains
            if Set(declared) != Set(platform) {
                throw EvalError(
                    "excludedDomains mismatch: fixture=\(declared.sorted()) platform=\(platform.sorted())"
                )
            }
        }
        var lead: [(String, Json)] = []
        for t in ScenarioRegistry.active {
            if let d = t.recommendedLeadTimeDays {
                lead.append((t.id, .num(String(d))))
            } else {
                lead.append((t.id, .null))
            }
        }
        return .obj(JsonObject([
            ("activeIds", .arr(ScenarioRegistry.active.map { .str($0.id) })),
            ("activeCategories", .arr(ScenarioRegistry.active.map { .str($0.category) })),
            ("plannedIds", .arr(ScenarioRegistry.planned.map { .str($0.id) })),
            ("plannedExecutable", .arr(ScenarioRegistry.planned.map {
                .bool(ScenarioRegistry.isExecutable($0.id))
            })),
            ("leadingTimeByTemplate", .obj(JsonObject(lead))),
        ]))
    }

    // ---------------------------------------------------------------- timeline

    private static func timeline(_ caseId: String, _ input: JsonObject) throws -> EvalOutcome {
        guard let scenario = timelineScenario(caseId) else { return .notImplemented }
        guard let now = input["now"]?.stringValue else {
            throw EvalError("timeline fixture missing now")
        }
        let freshness = Int(input["freshnessThresholdDays"]?.doubleValue ?? 45)
        let items = Timeline.buildTimelinePure(scenario, now, freshness)
        switch caseId {
        case "timeline-buckets-and-ordering":
            let again = Timeline.buildTimelinePure(scenario, now, freshness)
            let deterministic = (items.count == again.count)
                && zip(items, again).allSatisfy { pair in pair.0.id == pair.1.id }
            return .value(.obj(JsonObject([
                ("buckets", .arr(items.map { .str($0.bucket) })),
                ("ids", .arr(items.map { .str($0.id) })),
                ("kinds", .arr(items.map { .str($0.kind) })),
                ("deterministic", .bool(deterministic)),
                ("planCountUnchanged", .num(String(scenario.plans.count))),
            ])))
        case "timeline-attention-signals":
            let traceable = items.allSatisfy { !$0.sourceId.isEmpty }
            return .value(.obj(JsonObject([
                ("kinds", .arr(items.map { .str($0.kind) })),
                ("buckets", .arr(items.map { .str($0.bucket) })),
                ("sourceTypes", .arr(items.map { .str($0.sourceType) })),
                ("priorities", .arr(items.map { .num(String($0.priority)) })),
                ("statuses", .arr(items.map { .str($0.status) })),
                ("allTraceable", .bool(traceable)),
            ])))
        case "timeline-terminal-plans-excluded":
            return .value(.obj(JsonObject([
                ("count", .num(String(items.count))),
                ("kinds", .arr(items.map { .str($0.kind) })),
            ])))
        default:
            return .notImplemented
        }
    }

    /// 重建 generator 里的三个场景。
    ///
    /// fixture 的 `input` 只给出 now / graphRevision / 日期数组，
    /// 不足以决定 expected —— 三端（TS/Kotlin/ArkTS）都是在这里重建场景后
    /// 再喂给投影函数。此处按 Harmony 侧 `timelineScenario()` 逐条照录。
    private static func timelineScenario(_ caseId: String) -> Timeline.Input? {
        func legacySource() -> Timeline.SourceInput {
            Timeline.SourceInput(
                id: Timeline.legacyWechatSourceInstanceId,
                label: "微信账单（历史导入）",
                state: SourceInstanceState.active.rawValue,
                lastIngestedAt: nil
            )
        }
        func cardNode(_ expiry: String?) -> Timeline.NodeInput {
            var fields: [String: String] = [:]
            if let e = expiry { fields["expiryDate"] = e }
            return Timeline.NodeInput(id: "n-card", name: "招行 4417", archived: false, fields: fields)
        }
        func plan(_ id: String, _ title: String, _ effectiveDate: String?, _ lastAnalyzed: Int, _ state: String) -> Timeline.PlanInput {
            Timeline.PlanInput(
                id: id, title: title, scenario: "replace_payment_card", workflowState: state,
                lastAnalyzedGraphRevision: lastAnalyzed, effectiveDate: effectiveDate, actions: []
            )
        }

        switch caseId {
        case "timeline-buckets-and-ordering":
            let dates = ["2026-01-01", "2026-09-13", "2026-09-15", "2026-09-25", "2026-10-20", "2027-06-01"]
            var plans: [Timeline.PlanInput] = []
            for (i, d) in dates.enumerated() {
                // lastAnalyzed = 5 而 DB 的实际 revision 是 0 → 不 stale。
                plans.append(plan("plan-\(i + 1)", "计划 \(i + 1)", d, 5, "analyzed"))
            }
            return Timeline.Input(
                currentGraphRevision: 0, plans: plans, drifts: [],
                nodes: [cardNode(nil)], sources: [legacySource()]
            )
        case "timeline-attention-signals":
            let drift = Timeline.DriftInput(
                id: "drift-1", kind: DriftKind.possibleReplacement.rawValue,
                detectedAt: "2026-09-13T00:00:00.000Z", status: DriftStatus.`open`.rawValue,
                targetNodeId: "n-wechat"
            )
            let wechat = Timeline.NodeInput(id: "n-wechat", name: "微信支付", archived: false, fields: [:])
            let stale = Timeline.SourceInput(
                id: "inst-stale", label: "陈旧来源", state: SourceInstanceState.active.rawValue,
                lastIngestedAt: "2026-01-01T00:00:00.000Z"
            )
            // 落后计划：lastAnalyzed=0，DB revision 也是 0 → **不 stale**，
            // 且 effectiveDate 为空 → 一项都不产出。这正是基准的期望。
            return Timeline.Input(
                currentGraphRevision: 0,
                plans: [plan("plan-stale", "落后计划", nil, 0, "analyzed")],
                drifts: [drift], nodes: [cardNode("2026-10-05"), wechat],
                sources: [stale, legacySource()]
            )
        case "timeline-terminal-plans-excluded":
            return Timeline.Input(
                currentGraphRevision: 0,
                plans: [
                    plan("plan-done", "已完成", "2026-09-20", 5, "completed"),
                    plan("plan-cancel", "已取消", "2026-09-21", 5, "cancelled"),
                ],
                drifts: [], nodes: [cardNode(nil)], sources: [legacySource()]
            )
        default:
            return nil
        }
    }

    // --------------------------------------------------------------- migration

    private static func migration(_ caseId: String, _ input: JsonObject) throws -> EvalOutcome {
        switch caseId {
        case "migration-version-contract":
            return .value(try migrationVersionContract(input))
        case "migration-db-v1-to-v3":
            // 需要真实 SQL 执行（建表 / 迁移 / 断言保留项）。
            // 本包未引入 SQLCipher，如实记 BLOCKED —— 不伪造 PASS。
            return .blocked("requires SQLCipher-backed migration executor (not in package yet)")
        default:
            return .notImplemented
        }
    }

    private static func migrationVersionContract(_ input: JsonObject) throws -> Json {
        // 一致性断言：fixture 声明的 schema 版本必须是本平台认同的那一个。
        if let declaredRaw = input["logicalSchemaVersion"] {
            let declared = Int(try declaredRaw.asDouble)
            if declared != CanonicalSpec.appSchemaVersion {
                throw EvalError(
                    "logicalSchemaVersion mismatch: fixture=\(declared) platform=\(CanonicalSpec.appSchemaVersion)"
                )
            }
        }
        if let chainRaw = input["migrations"]?.arrayValue {
            var chain: [Int] = []
            for item in chainRaw { chain.append(Int(try item.asDouble)) }
            if chain != Migrations.migrationChain {
                throw EvalError("migration chain mismatch: fixture=\(chain) platform=\(Migrations.migrationChain)")
            }
        }
        // 负向交叉验证：清单里每个版本都必须被**拒绝**。
        for v in SchemaVersion.rejectedSchemaVersions where SchemaVersion.acceptsSchemaVersion(v) {
            throw EvalError("schema version must be rejected but was accepted: \(v)")
        }
        for v in SchemaVersion.rejectedPayloadVersions where SchemaVersion.acceptsPayloadVersion(v) {
            throw EvalError("payload version must be rejected but was accepted: \(v)")
        }
        // GraphRevision 白名单的**负向**交叉验证。
        for m in SchemaVersion.graphRevisionNeverBumpedBy where GraphRevision.doesMutationBumpRevision(m) {
            throw EvalError("mutation must never bump revision but does: \(m)")
        }
        return .obj(JsonObject([
            ("migrations", .arr(Migrations.migrationChain.map { .num(String($0)) })),
            ("payloadKind", .str(SchemaVersion.payloadKind)),
            ("payloadTables", .arr(SchemaVersion.payloadTables.map { .str($0) })),
            ("rejectedSchemaVersions", .arr(SchemaVersion.rejectedSchemaVersions.map { .num(String($0)) })),
            ("rejectedPayloadVersions", .arr(SchemaVersion.rejectedPayloadVersions.map { .num(String($0)) })),
            ("graphRevisionNeverBumpedBy", .arr(SchemaVersion.graphRevisionNeverBumpedBy.map { .str($0) })),
        ]))
    }
}
