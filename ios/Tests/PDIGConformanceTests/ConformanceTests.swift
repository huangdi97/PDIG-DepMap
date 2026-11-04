import XCTest
@testable import PDIGCore
import PDIGConformance

final class ConformanceTests: XCTestCase {

    /// 当前 manifest 中的全部 canonical 用例都真实执行；不把固定历史总数写死在测试里。
    ///
    /// 用例总数由 conformance/CONFORMANCE_MANIFEST.json 唯一决定。新增 canonical fixture
    /// 不应因为 XCTest 仍写着旧数字而制造假失败；真正的门禁是：manifest 非空、报告覆盖
    /// manifest 全量、0 fail、0 implMissing、0 envBlocked、passed == total。
    func testCanonicalAccounting() throws {
        let store = try FixtureStore.locate()
        let manifestCount = try store.manifest().count
        let report = try ConformanceRunner.runAndWriteReport()
        print("\n" + report.render() + "\n")

        XCTAssertGreaterThan(manifestCount, 0, "canonical manifest 不得为空")
        XCTAssertEqual(report.total, manifestCount, "报告必须覆盖当前 manifest 的全部 canonical 用例")
        XCTAssertEqual(report.failed, 0, "已执行用例不允许有失败：\n"
            + report.outcomes.filter { $0.status == .fail }
                .map { "  - \($0.id): \($0.detail)" }.joined(separator: "\n"))
        XCTAssertEqual(report.implMissing, 0, "canonical 纯逻辑不得出现未移植回退")
        XCTAssertEqual(report.envBlocked, 0, "当前 host canonical 集不得出现环境阻断")
        XCTAssertEqual(report.passed, report.total, "全部 canonical 用例都应真实执行并通过")
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
