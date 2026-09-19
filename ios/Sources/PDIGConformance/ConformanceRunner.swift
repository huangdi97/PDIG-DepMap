// iOS canonical conformance runner。
//
// 与 Harmony 主机侧同一套冻结 fixture（conformance/CONFORMANCE_MANIFEST.json 里
// 的 91 条），逐条跑 PDIGCore 的真实实现并与 fixture 的 `expected` 对比。
//
// 记账口径（四条独立的账，不得互相替代）：
//   HOST_EXECUTED    —— 已移植且在 macOS 上真实跑出结果的用例
//   HOST_IMPL_MISSING—— 纯逻辑尚未移植（不需要设备，只是没写）
//   ENV_BLOCKED      —— 需要尚未接入的依赖（SQLCipher / Argon2id 原生）
//   RUNTIME_BLOCKED  —— 需要真机 / 系统能力（Keychain / LocalAuthentication）
//
// 未被执行的用例一律如实记账，**不**计入 pass。

import Foundation
import PDIGCore

public enum ConformanceRunner {

    public enum CaseStatus: String, CaseIterable, Sendable {
        case pass
        case fail
        case implMissing
        case envBlocked
    }

    public struct CaseOutcome: Sendable {
        public let id: String
        public let category: String
        public let status: CaseStatus
        public let detail: String
        /// 实际产出（跨平台差分要用；未执行则为 nil）。
        public let actual: Json?
        public init(id: String, category: String, status: CaseStatus, detail: String, actual: Json? = nil) {
            self.id = id
            self.category = category
            self.status = status
            self.detail = detail
            self.actual = actual
        }

        /// Android 报告里的同名词，便于三端差分工具统一处理。
        public var wireStatus: String {
            switch status {
            case .pass: return "PASS"
            case .fail: return "FAIL"
            case .implMissing: return "NOT_IMPLEMENTED"
            case .envBlocked: return "ENV_BLOCKED"
            }
        }
    }

    public struct Report: Sendable {
        public let outcomes: [CaseOutcome]

        public func count(_ s: CaseStatus) -> Int { outcomes.filter { $0.status == s }.count }
        public var total: Int { outcomes.count }
        public var passed: Int { count(.pass) }
        public var failed: Int { count(.fail) }
        public var implMissing: Int { count(.implMissing) }
        public var envBlocked: Int { count(.envBlocked) }

        public func render() -> String {
            var lines: [String] = []
            lines.append("[ ios ] 用例   : \(total) 条")
            lines.append("[ ios ]        : pass=\(passed) fail=\(failed) implMissing=\(implMissing) envBlocked=\(envBlocked)")
            for o in outcomes where o.status == .fail {
                lines.append("[ ios ] FAIL   : \(o.id) — \(o.detail)")
            }
            for o in outcomes where o.status == .envBlocked {
                lines.append("[ ios ] BLOCKED: \(o.id) — \(o.detail)")
            }
            lines.append("IOS_TOTAL_CANONICAL      = \(total)")
            lines.append("IOS_HOST_EXECUTED        = \(passed)   (fail=\(failed))")
            lines.append("IOS_HOST_IMPL_MISSING    = \(implMissing)")
            lines.append("IOS_ENV_BLOCKED          = \(envBlocked)")
            lines.append("IOS_CONFORMANCE_HOST     = \(failed == 0 ? "PASS" : "FAIL")")
            lines.append("IOS_HOST_PASS            = \(passed)/\(total)")
            return lines.joined(separator: "\n")
        }
    }

    public static func run() throws -> Report {
        let store = try FixtureStore.locate()
        let entries = try store.manifest()
        var outcomes: [CaseOutcome] = []
        for entry in entries {
            let fixture = try store.fixture(entry)
            guard let input = fixture["input"]?.objectValue else {
                outcomes.append(CaseOutcome(id: entry.id, category: entry.category, status: .fail, detail: "fixture has no input object"))
                continue
            }
            guard let expected = fixture["expected"] else {
                outcomes.append(CaseOutcome(id: entry.id, category: entry.category, status: .fail, detail: "fixture has no expected"))
                continue
            }
            do {
                switch try Evaluators.evaluate(category: entry.category, caseId: entry.id, input: input, store: store) {
                case .value(let actual):
                    if JsonDeepEqual.equal(expected, actual) {
                        outcomes.append(CaseOutcome(id: entry.id, category: entry.category, status: .pass, detail: "", actual: actual))
                    } else {
                        outcomes.append(CaseOutcome(
                            id: entry.id, category: entry.category, status: .fail,
                            detail: JsonDeepEqual.describeDiff(expected: expected, actual: actual),
                            actual: actual
                        ))
                    }
                case .notImplemented:
                    outcomes.append(CaseOutcome(id: entry.id, category: entry.category, status: .implMissing, detail: "evaluator not ported for category '\(entry.category)'"))
                case .blocked(let reason):
                    outcomes.append(CaseOutcome(id: entry.id, category: entry.category, status: .envBlocked, detail: reason))
                }
            } catch let e {
                // 求值器抛错 = 该用例失败（不是"跳过"）：
                // 政策 gate 触发、契约不一致都走这里。
                outcomes.append(CaseOutcome(
                    id: entry.id, category: entry.category, status: .fail,
                    detail: "\(e)"
                ))
            }
        }
        return Report(outcomes: outcomes)
    }

    /// 与 Android `conformance/reports/android.json` 同形的机器可读报告，
    /// 供跨平台差分工具逐用例比对（N5）。
    public static func runAndWriteReport() throws -> Report {
        let report = try run()
        let store = try FixtureStore.locate()
        var fields: [(String, Json)] = []
        for o in report.outcomes {
            var entry: [(String, Json)] = [
                ("status", .str(o.wireStatus)),
                ("category", .str(o.category)),
            ]
            if let actual = o.actual { entry.append(("actual", actual)) }
            fields.append((o.id, .obj(JsonObject(entry))))
        }
        let json = Json.obj(JsonObject([
            ("platform", .str("ios")),
            ("specVersion", .str("1.0.0")),
            ("summary", .obj(JsonObject([
                ("pass", .num(String(report.passed))),
                ("fail", .num(String(report.failed))),
                ("notImplemented", .num(String(report.implMissing))),
                ("envBlocked", .num(String(report.envBlocked))),
                ("total", .num(String(report.total))),
            ]))),
            ("results", .obj(JsonObject(fields))),
        ]))
        let dir = (store.root as NSString).appendingPathComponent("conformance/reports")
        try FileManager.default.createDirectory(atPath: dir, withIntermediateDirectories: true)
        let path = (dir as NSString).appendingPathComponent("ios.json")
        try (JsonWriter.write(json) + "\n").write(toFile: path, atomically: true, encoding: .utf8)
        return report
    }
}
