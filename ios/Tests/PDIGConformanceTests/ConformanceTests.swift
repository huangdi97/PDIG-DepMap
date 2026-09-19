import XCTest
@testable import PDIGCore
import PDIGConformance

final class ConformanceTests: XCTestCase {

    /// 91 条 canonical 用例全跑一遍；断言账目与"已执行用例零失败"。
    ///
    /// 已执行 / 未移植 / 环境缺失三桶各自断言，**不**把"未移植"计入通过。
    func testCanonicalAccounting() throws {
        let report = try ConformanceRunner.run()
        print("\n" + report.render() + "\n")

        XCTAssertEqual(report.total, 91, "canonical 用例总数必须是 91")
        XCTAssertEqual(report.failed, 0, "已执行用例不允许有失败：\n"
            + report.outcomes.filter { $0.status == .fail }
                .map { "  - \($0.id): \($0.detail)" }.joined(separator: "\n"))
        // 已移植的分类：relations 18 + jcs 1 + scenario 1 + parser 22 + impact 13
        // + readiness 16 + coverage 6 + timeline 3 + state-machine 5
        // + migration-version-contract 1 = 86；剩余 4 条 = depmap 3 + backup 1
        //（需要 Argon2id / AES-GCM 容器层，尚未接入本包）。
        XCTAssertEqual(report.passed, 86, "本轮已移植用例数应为 86")
        XCTAssertEqual(report.envBlocked, 1, "migration-db-v1-to-v3 需要 SQLCipher，记环境缺失")
        XCTAssertEqual(report.implMissing, 91 - 86 - 1)
    }
}

final class CoreSmokeTests: XCTestCase {

    func testJcsSortsKeysByUtf16CodeUnit() throws {
        let v = Json.obj(JsonObject([("b", .num("1")), ("a", .num("2"))]))
        XCTAssertEqual(try Jcs.stringify(v), #"{"a":2,"b":1}"#)
    }

    func testJcsRejectsFloat() {
        let v = Json.obj(JsonObject([("f", .num("1.5"))]))
        XCTAssertThrowsError(try Jcs.stringify(v)) { e in
            XCTAssertTrue(e is JcsError, "受限域外的浮点必须抛 JcsError，不能静默序列化")
        }
    }

    func testGraphRevisionNeverBumpsOnNonRealityMutations() {
        for m in SchemaVersion.graphRevisionNeverBumpedBy {
            XCTAssertFalse(GraphRevision.doesMutationBumpRevision(m), "\(m) 不得提升 graphRevision")
        }
        XCTAssertTrue(GraphRevision.doesMutationBumpRevision("dependency_confirm_create"))
        XCTAssertEqual(GraphRevision.parseStoredRevision(nil), 0)
        XCTAssertEqual(GraphRevision.parseStoredRevision("-3"), 0)
    }

    func testGb18030RoundTripOnChinese() throws {
        // "银行" 的 GB18030 双字节编码
        let bytes: [UInt8] = [0xD2, 0xF8, 0xD0, 0xD0]
        XCTAssertEqual(try XCTUnwrap(Gb18030.decode(bytes)), "银行")
    }

    func testWechatTimeParsesOffset() {
        XCTAssertEqual(WechatParser.parseWechatTime("2026-01-15 10:23:00"), "2026-01-15T10:23:00+08:00")
        XCTAssertNil(WechatParser.parseWechatTime("2026-02-30 10:23:00"), "不存在的日历日期必须拒绝")
    }

    func testMissingAmountIsNotZero() {
        XCTAssertNil(parseMappingAmount("", "."), "空金额必须拒绝，不得当成 0 元")
        XCTAssertEqual(parseMappingAmount("¥1,234.56", "."), 1234.56)
    }
}
