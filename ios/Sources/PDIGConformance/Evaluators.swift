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

    public static func evaluate(
        category: String,
        caseId: String,
        input: JsonObject,
        store: FixtureStore
    ) throws -> EvalOutcome {
        switch category {
        case "relations": return .value(try relations(input))
        case "jcs": return .value(try jcs(input))
        case "scenario": return .value(try scenario(input))
        case "parser": return .value(try parser(input, store))
        case "readiness": return .value(try readiness(input))
        case "coverage": return .value(try coverage(input))
        case "state-machine": return try stateMachine(caseId)
        case "impact": return .value(try impact(input))
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

    // ------------------------------------------------------------------ parser

    /// 把 fixture 指向的导入文件喂给**真实解析器**，输出与 expected 同形的 Json。
    ///
    /// 只投影 sourceTxnId 之外的字段：fixture 是冻结契约，sourceTxnId 由
    /// 各端内部生成、不参与跨端比对（见 fixture 的 description）。
    private static func parser(_ input: JsonObject, _ store: FixtureStore) throws -> Json {
        guard let adapterId = input["adapterId"]?.stringValue else {
            throw EvalError("parser fixture missing adapterId")
        }
        guard let file = input["file"]?.stringValue else {
            throw EvalError("parser fixture missing file")
        }
        let bytes = try store.readBytes("fixtures/import/" + file)
        let result: ParseResult
        switch adapterId {
        case "wechat":
            result = try WechatParser.parse(bytes)
        case "ofx_qfx":
            result = try OfxParser.parse(bytes)
        case "generic_csv":
            result = try GenericCsvParser.parse(bytes, try mappingProfile(input["mapping"]))
        default:
            throw EvalError("unknown adapter: \(adapterId)")
        }

        var obs: [Json] = []
        for o in result.observations {
            obs.append(.obj(JsonObject([
                ("occurredAt", .str(o.occurredAt)),
                ("amount", .num(numberText(o.amount))),
                ("currency", .str(o.currency)),
                ("direction", .str(o.direction.rawValue)),
                ("merchantRaw", .str(o.merchantRaw)),
                ("status", .str(o.status)),
            ])))
        }
        var errs: [Json] = []
        for e in result.errors {
            errs.append(.obj(JsonObject([
                ("line", .num(String(e.line))),
                ("reason", .str(e.reason)),
            ])))
        }
        return .obj(JsonObject([
            ("observationCount", .num(String(result.observations.count))),
            ("observations", .arr(obs)),
            ("errorCount", .num(String(result.errors.count))),
            ("errors", .arr(errs)),
        ]))
    }

    /// 数字字面形式与 TS oracle / Android `num()` 对齐：整数不带小数点。
    /// （JsonDeepEqual 按数值比较，这里保持同形只是为了让失败时的 diff 可读。）
    private static func numberText(_ d: Double) -> String {
        if d == d.rounded() && abs(d) < 1e15 { return String(Int64(d)) }
        return String(d)
    }

    /// 显式字段映射。fixture 没给 mapping（wechat / ofx_qfx）时返回 nil。
    private static func mappingProfile(_ v: Json?) throws -> MappingProfile? {
        guard let v = v, let m = v.objectValue else { return nil }
        guard let columnsJson = m["columns"]?.objectValue,
              let dateTime = columnsJson["dateTime"]?.stringValue else {
            throw EvalError("generic_csv mapping missing columns.dateTime")
        }
        let columns = MappingColumns(
            transactionId: columnsJson["transactionId"]?.stringValue,
            dateTime: dateTime,
            amount: columnsJson["amount"]?.stringValue,
            debit: columnsJson["debit"]?.stringValue,
            credit: columnsJson["credit"]?.stringValue,
            description: columnsJson["description"]?.stringValue,
            counterparty: columnsJson["counterparty"]?.stringValue,
            currency: columnsJson["currency"]?.stringValue,
            balance: columnsJson["balance"]?.stringValue,
            transactionType: columnsJson["transactionType"]?.stringValue,
            paymentMethod: columnsJson["paymentMethod"]?.stringValue
        )
        let o = m["options"]?.objectValue ?? JsonObject([])
        let formats = o["dateFormats"]?.arrayValue?.compactMap { $0.stringValue } ?? ["YYYY-MM-DD"]
        let decRaw = o["decimalSeparator"]?.stringValue ?? "."
        guard let dec = decRaw.first else { throw EvalError("empty decimalSeparator") }
        let options = MappingOptions(
            delimiter: o["delimiter"]?.stringValue ?? ",",
            dateFormats: formats,
            decimalSeparator: dec,
            amountSignMode: o["amountSignMode"]?.stringValue ?? "outward_positive",
            hasHeaderRow: o["hasHeaderRow"]?.boolValue ?? true,
            encoding: o["encoding"]?.stringValue ?? "utf-8",
            positiveDirection: o["positiveDirection"]?.stringValue
        )
        return MappingProfile(columns: columns, options: options)
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

    // --------------------------------------------------------- state-machine

    /// 状态机用例报告的是**本平台实现的规则视图**：状态表、守卫、合同文本都取自
    /// PDIGCore 的常量，而不是回显 spec 文档 —— 否则"文档改了实现没改"不会被发现。
    private static func stateMachine(_ caseId: String) throws -> EvalOutcome {
        switch caseId {
        case "state-machine-change-plan":
            return .value(changePlanMachine())
        case "state-machine-graph-revision":
            return .value(try graphRevisionMachine())
        case "state-machine-action-verification":
            return .value(verificationMachine())
        case "state-machine-discovery-candidate":
            return .value(candidateMachine())
        case "state-machine-reality-drift":
            return .value(driftMachine())
        default:
            return .notImplemented
        }
    }

    private static func changePlanMachine() -> Json {
        let order: [ChangePlanWorkflowState] = [
            .draft, .analyzed, .reviewRequired, .ready, .inProgress, .verifying, .completed, .cancelled,
        ]
        var fields: [(String, Json)] = []
        for s in order {
            let next = (ChangePlanMachine.transitions[s] ?? []).map { Json.str($0.wire) }
            fields.append((s.wire, .arr(next)))
        }
        let d = ChangePlanMachine.derivedStatus
        return .obj(JsonObject([
            ("transitions", .obj(JsonObject(fields))),
            ("terminal", .arr(ChangePlanMachine.terminal.map { Json.str($0.wire) })),
            ("derivedStatus", .obj(JsonObject([
                ("name", .str(d.name)),
                ("value", .str(d.value)),
                ("rule", .str(d.rule)),
                ("persisted", .bool(d.persisted)),
            ]))),
            ("guards", .arr(ChangePlanMachine.guards.map { guardJson($0) })),
        ]))
    }

    private static func graphRevisionMachine() throws -> Json {
        // 交叉验证：白名单里的每个 mutation 都必须被本平台认可 bump，
        // 黑名单里的每个都必须**不** bump。这让"三端同一套白名单"成为可执行断言。
        for m in GraphRevisionMachine.bumpsOn where !GraphRevision.doesMutationBumpRevision(m) {
            throw EvalError("mutation must bump revision but does not: \(m)")
        }
        for m in GraphRevisionMachine.neverBumpsOn where GraphRevision.doesMutationBumpRevision(m) {
            throw EvalError("mutation must never bump revision but does: \(m)")
        }
        return .obj(JsonObject([
            ("initial", .num("0")),
            ("monotonic", .bool(GraphRevisionMachine.monotonic)),
            ("atomicity", .str(GraphRevisionMachine.atomicity)),
            ("bumpsOn", .arr(GraphRevisionMachine.bumpsOn.map { Json.str($0) })),
            ("neverBumpsOn", .arr(GraphRevisionMachine.neverBumpsOn.map { Json.str($0) })),
        ]))
    }

    private static func verificationMachine() -> Json {
        let order: [ActionVerificationStatus] = [
            .notRequired, .pending, .evidenceSuggested, .verified, .failed,
        ]
        var fields: [(String, Json)] = []
        for s in order {
            let next = (VerificationMachine.transitions[s] ?? []).map { Json.str($0.wire) }
            fields.append((s.wire, .arr(next)))
        }
        let r = VerificationMachine.evidenceSignalRule
        return .obj(JsonObject([
            ("initial", .str(VerificationMachine.initial.wire)),
            ("terminal", .arr(VerificationMachine.terminal.map { Json.str($0.wire) })),
            ("transitions", .obj(JsonObject(fields))),
            ("evidenceSignalRule", .obj(JsonObject([
                ("appliesOnlyTo", .arr(r.appliesOnlyTo.map { Json.str($0.wire) })),
                ("requiresMethod", .str(r.requiresMethod)),
                ("requiresMatch", .str(r.requiresMatch)),
                ("effect", .str(r.effect)),
                ("never", .arr(r.never.map { Json.str($0) })),
            ]))),
            ("guards", .arr(VerificationMachine.guards.map { guardJson($0) })),
        ]))
    }

    private static func candidateMachine() -> Json {
        let order: [CandidateStatus] = [.pending, .accepted, .dismissed, .superseded]
        var fields: [(String, Json)] = []
        for s in order {
            let next = (CandidateMachine.transitions[s] ?? []).map { Json.str($0.wire) }
            fields.append((s.wire, .arr(next)))
        }
        let a = CandidateMachine.accept
        let d = CandidateMachine.dismiss
        return .obj(JsonObject([
            ("initial", .str(CandidateMachine.initial.wire)),
            ("transitions", .obj(JsonObject(fields))),
            ("accept", .obj(JsonObject([
                ("mutatesReality", .bool(a.mutatesReality)),
                ("effects", .arr(a.effects.map { Json.str($0) })),
                ("bumpsGraphRevision", .bool(a.bumpsGraphRevision)),
                ("replay", .str(a.replay)),
            ]))),
            ("dismiss", .obj(JsonObject([
                ("mutatesReality", .bool(d.mutatesReality)),
                ("effects", .arr(d.effects.map { Json.str($0) })),
                ("reappeal", .str(d.reappeal)),
            ]))),
            ("guards", .arr(CandidateMachine.guards.map { guardJson($0) })),
        ]))
    }

    private static func driftMachine() -> Json {
        let order: [DriftStatus] = [.`open`, .confirmedChange, .dismissed, .superseded]
        var fields: [(String, Json)] = []
        for s in order {
            let next = (DriftMachine.transitions[s] ?? []).map { Json.str($0.wire) }
            fields.append((s.wire, .arr(next)))
        }
        let c = DriftMachine.creationRule
        return .obj(JsonObject([
            ("initial", .str(DriftMachine.initial.wire)),
            ("transitions", .obj(JsonObject(fields))),
            ("creationRule", .obj(JsonObject([
                ("requires", .str(c.requires)),
                ("minObservations", .num(String(c.minObservations))),
                ("absenceOnly", .str(c.absenceOnly)),
                ("alreadyConfirmedSource", .str(c.alreadyConfirmedSource)),
                ("belowThreshold", .str(c.belowThreshold)),
                ("duplicateEvidenceRef", .str(c.duplicateEvidenceRef)),
                ("upsert", .str(c.upsert)),
            ]))),
            ("guards", .arr(DriftMachine.guards.map { guardJson($0) })),
        ]))
    }

    private static func guardJson(_ g: Guard) -> Json {
        .obj(JsonObject([
            ("id", .str(g.id)),
            ("rule", .str(g.rule)),
            ("errorCode", .str(g.errorCode)),
        ]))
    }

    // -------------------------------------------------------------- readiness

    /// readiness 的输出就是一个 wire 字符串（"ready" / "blocked" / …）。
    private static func readiness(_ input: JsonObject) throws -> Json {
        guard let planJson = input["plan"]?.objectValue else {
            throw EvalError("readiness input has no plan object")
        }
        var actions: [PlanAction] = []
        for item in planJson["actions"]?.arrayValue ?? [] {
            guard let o = item.objectValue else { continue }
            actions.append(
                PlanAction(
                    id: try requireString(o, "id"),
                    title: try requireString(o, "title"),
                    phase: try wire(o, "phase", PlanActionPhase.self),
                    done: o["done"]?.boolValue ?? false,
                    resolvesImpactKeys: o["resolvesImpactKeys"]?.arrayValue?
                        .compactMap { $0.stringValue } ?? []
                )
            )
        }
        let plan = ChangePlan(
            id: try requireString(planJson, "id"),
            templateId: planJson["templateId"]?.stringValue,
            scenario: try requireString(planJson, "scenario"),
            title: try requireString(planJson, "title"),
            workflowState: try wire(planJson, "workflowState", ChangePlanWorkflowState.self),
            baselineGraphRevision: try requireInt(planJson, "baselineGraphRevision"),
            lastAnalyzedGraphRevision: try requireInt(planJson, "lastAnalyzedGraphRevision"),
            targetNodeId: planJson["targetNodeId"]?.stringValue,
            effectiveDate: planJson["effectiveDate"]?.stringValue,
            actions: actions
        )
        let readinessInput = PlanReadinessInput(
            plan: plan,
            currentGraphRevision: try requireInt(input, "currentGraphRevision"),
            pendingMustChange: try requireInt(input, "pendingMustChange"),
            pendingNeedsReview: try requireInt(input, "pendingNeedsReview"),
            unresolvedCandidates: try requireInt(input, "unresolvedCandidates"),
            pendingRelevantProposals: try requireInt(input, "pendingRelevantProposals"),
            staleRelevantDependencies: try requireInt(input, "staleRelevantDependencies"),
            unfinishedChangeActions: try requireInt(input, "unfinishedChangeActions")
        )
        return .str(PlanRules.computePlanReadiness(readinessInput).wire)
    }

    // --------------------------------------------------------------- coverage

    private static func coverage(_ input: JsonObject) throws -> Json {
        var sources: [CoverageSourceInfo] = []
        for item in input["sources"]?.arrayValue ?? [] {
            guard let o = item.objectValue else { continue }
            sources.append(
                CoverageSourceInfo(
                    id: try requireString(o, "id"),
                    label: try requireString(o, "label"),
                    lastIngestedAt: o["lastIngestedAt"]?.stringValue
                )
            )
        }
        let covInput = ScenarioCoverageInput(
            scenarioId: try requireString(input, "scenarioId"),
            sources: sources,
            confirmedDirectDependencies: try requireInt(input, "confirmedDirectDependencies"),
            confirmedIndirectDependencies: try requireInt(input, "confirmedIndirectDependencies"),
            pendingProposals: try requireInt(input, "pendingProposals"),
            unresolvedCandidates: try requireInt(input, "unresolvedCandidates"),
            staleDependencies: try requireInt(input, "staleDependencies"),
            unknownCriticalityCount: try requireInt(input, "unknownCriticalityCount"),
            unverifiedActions: try requireInt(input, "unverifiedActions"),
            freshnessThresholdDays: try requireInt(input, "freshnessThresholdDays"),
            now: try requireString(input, "now")
        )
        let cov = PlanRules.computeScenarioCoverage(covInput)
        let counts: Json = .obj(JsonObject([
            ("confirmedDirectDependencies", .num(String(cov.counts.confirmedDirectDependencies))),
            ("confirmedIndirectDependencies", .num(String(cov.counts.confirmedIndirectDependencies))),
            ("pendingProposals", .num(String(cov.counts.pendingProposals))),
            ("unresolvedCandidates", .num(String(cov.counts.unresolvedCandidates))),
            ("staleDependencies", .num(String(cov.counts.staleDependencies))),
            ("unknownCriticalityCount", .num(String(cov.counts.unknownCriticalityCount))),
            ("unverifiedActions", .num(String(cov.counts.unverifiedActions))),
        ]))
        return .obj(JsonObject([
            ("scenarioId", .str(cov.scenarioId)),
            ("coverageLevel", .str(cov.coverageLevel.wire)),
            ("explanations", .arr(cov.explanations.map { Json.str($0) })),
            ("counts", counts),
        ]))
    }

    private static func requireInt(_ o: JsonObject, _ field: String) throws -> Int {
        guard let v = o[field] else { throw EvalError("missing int field '\(field)'") }
        return Int(try v.asLong)
    }

    // ------------------------------------------------------------------ impact

    /// 用 fixture 的 graph 直接驱动 `ImpactKernel.simulateScenario`。
    ///
    /// 刻意**不**读 proposal 的 `confidenceScore`：确认度不参与确定性失效传播，
    /// 读进来就等于给"confidence 可以影响结论"留一道门。
    private static func impact(_ input: JsonObject) throws -> Json {
        let g = input["graph"]?.objectValue ?? JsonObject([])

        var deps: [Dependency] = []
        for item in g["dependencies"]?.arrayValue ?? [] {
            guard let o = item.objectValue else { continue }
            deps.append(
                Dependency(
                    id: try requireString(o, "id"),
                    from: try requireString(o, "from"),
                    relation: try wire(o, "relation", Relation.self),
                    to: try requireString(o, "to"),
                    capability: try wire(o, "capability", Capability.self),
                    criticality: try wire(o, "criticality", Criticality.self),
                    groupId: o["groupId"]?.stringValue,
                    state: try wire(o, "state", DependencyState.self),
                    origin: try wire(o, "origin", DependencyOrigin.self),
                    lastVerifiedAt: try requireString(o, "lastVerifiedAt")
                )
            )
        }

        var groups: [DependencyGroup] = []
        for item in g["groups"]?.arrayValue ?? [] {
            guard let o = item.objectValue else { continue }
            groups.append(
                DependencyGroup(
                    id: try requireString(o, "id"),
                    groupKey: try requireString(o, "groupKey"),
                    targetNodeId: try requireString(o, "targetNodeId"),
                    capability: try wire(o, "capability", Capability.self),
                    mode: try wire(o, "mode", GroupMode.self),
                    memberEdgeIds: o["memberEdgeIds"]?.arrayValue?.compactMap { $0.stringValue } ?? [],
                    state: try wire(o, "state", GroupState.self)
                )
            )
        }

        var proposals: [ImpactProposalInput] = []
        for item in g["proposals"]?.arrayValue ?? [] {
            guard let o = item.objectValue else { continue }
            proposals.append(
                ImpactProposalInput(
                    key: try requireString(o, "key"),
                    from: try requireString(o, "from"),
                    to: try requireString(o, "to"),
                    capability: try wire(o, "capability", Capability.self)
                )
            )
        }

        var nodeNames: [String: String] = [:]
        if let names = g["nodeNames"]?.objectValue {
            for (k, _) in names.fields { nodeNames[k] = names[k]?.stringValue ?? "" }
        }

        var unavailable: [ImpactStateKey] = []
        for item in input["unavailable"]?.arrayValue ?? [] {
            guard let o = item.objectValue else { continue }
            unavailable.append(
                ImpactStateKey(
                    try requireString(o, "nodeId"),
                    try wire(o, "capability", Capability.self)
                )
            )
        }

        let result = ImpactKernel.simulateScenario(
            graph: ImpactGraph(
                dependencies: deps, groups: groups, proposals: proposals, nodeNames: nodeNames
            ),
            unavailable: unavailable
        )

        var targets: [Json] = []
        for t in result.targets {
            targets.append(.obj(JsonObject([
                ("nodeId", .str(t.nodeId)),
                ("nodeName", .str(t.nodeName)),
                ("capability", .str(t.capability.wire)),
                ("depth", .num(String(t.depth))),
                ("status", .str(t.status.wire)),
                ("available", .bool(t.available)),
                ("redundancyDegraded", .bool(t.redundancyDegraded)),
                ("reasonCode", .str(t.reasonCode.wire)),
                ("reasonText", .str(t.reasonText)),
                ("edgeKeys", .arr(t.edgeKeys.map { Json.str($0) })),
                ("groupKeys", .arr(t.groupKeys.map { Json.str($0) })),
                ("proposalKeys", .arr(t.proposalKeys.map { Json.str($0) })),
            ])))
        }
        var checklist: [Json] = []
        for c in result.checklist {
            checklist.append(.obj(JsonObject([
                ("level", .str(c.level.wire)),
                ("nodeId", c.nodeId.map { Json.str($0) } ?? .null),
                ("capability", c.capability.map { Json.str($0.wire) } ?? .null),
                ("title", .str(c.title)),
                ("detail", .str(c.detail)),
            ])))
        }
        return .obj(JsonObject([
            ("unavailable", .arr(result.unavailable.map { stateKeyJson($0) })),
            ("lostKeys", .arr(result.lostKeys.map { stateKeyJson($0) })),
            ("targets", .arr(targets)),
            ("checklist", .arr(checklist)),
            ("processedKeys", .arr(result.processedKeys.map { Json.str($0) })),
        ]))
    }

    private static func stateKeyJson(_ k: ImpactStateKey) -> Json {
        .obj(JsonObject([("nodeId", .str(k.nodeId)), ("capability", .str(k.capability.wire))]))
    }

    private static func requireString(_ o: JsonObject, _ field: String) throws -> String {
        guard let s = o[field]?.stringValue else {
            throw EvalError("missing string field '\(field)'")
        }
        return s
    }

    /// wire → 枚举。未知 wire 直接抛错（fail closed，不静默取默认值）。
    private static func wire<T: RawRepresentable>(
        _ o: JsonObject, _ field: String, _ type: T.Type
    ) throws -> T where T.RawValue == String {
        let raw = try requireString(o, field)
        guard let v = T(rawValue: raw) else {
            throw EvalError("unknown wire value for '\(field)': \(raw)")
        }
        return v
    }

    // ---------------------------------------------------------------- timeline

    private static func timeline(_ caseId: String, _ input: JsonObject) throws -> EvalOutcome {
        guard let scenario = timelineScenario(caseId) else { return .notImplemented }
        guard let now = input["now"]?.stringValue else {
            throw EvalError("timeline fixture missing now")
        }
        // Json 没有 doubleValue（数字一律存 raw 文本，取值时才转），
        // 且 `try` 只能用在 throwing 的 asDouble 上。
        let freshness: Int
        if let raw = input["freshnessThresholdDays"] {
            freshness = Int(try raw.asDouble)
        } else {
            freshness = 45
        }
        let items = Timeline.buildTimelinePure(scenario, now, freshness)
        switch caseId {
        case "timeline-buckets-and-ordering":
            let again = Timeline.buildTimelinePure(scenario, now, freshness)
            let deterministic = (items.count == again.count)
                && zip(items, again).allSatisfy { pair in pair.0.id == pair.1.id }
            // 显式写出 `Json.`：`map` 闭包的返回类型无法穿过两层枚举 case
            // （.value(.obj(JsonObject([...])))）反向推断。
            return .value(.obj(JsonObject([
                ("buckets", .arr(items.map { Json.str($0.bucket) })),
                ("ids", .arr(items.map { Json.str($0.id) })),
                ("kinds", .arr(items.map { Json.str($0.kind) })),
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
                ("kinds", .arr(items.map { Json.str($0.kind) })),
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
