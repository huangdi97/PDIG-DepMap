import XCTest
import PDIGCore
@testable import PDIGApp

// PDIGApp 纯逻辑单测：导航 / Findings 组合 / replace_phone 前置与闸门 / CTA 映射。
// 运行于 macOS runner（swift test），不依赖任何 UI。

final class AppViewModelTests: XCTestCase {

    // MARK: - navigation

    func testNavigationPushPopRoot() {
        let session = AppSession()
        session.push(.findings)
        session.push(.timeline)
        XCTAssertEqual(session.route, .timeline)
        session.pop()
        XCTAssertEqual(session.route, .findings)
        session.pop()
        XCTAssertEqual(session.route, .home)
        session.push(.settings)
        session.root()
        XCTAssertEqual(session.route, .home)
        XCTAssertTrue(session.navigationStack.isEmpty)
    }

    // MARK: - findings composition（FailureDomainEngine + RecoveryCycleEngine）

    func testFindingsDetectSinglePointOfFailureAndSharedDomain() {
        let snapshot = DemoData.snapshot()
        let findings = FindingsViewModel.findings(FindingsInput(
            snapshot: snapshot,
            pendingVerifications: [],
            nowIso: DemoData.nowIso,
            freshnessThresholdDays: 400
        ))
        XCTAssertTrue(findings.contains { $0.kind == .singlePointOfFailure }, "应发现单点（无备用恢复路径）")
        XCTAssertTrue(findings.contains { $0.kind == .sharedFailureDomain }, "应发现共享故障点（两条恢复路径同机/同号）")
        XCTAssertTrue(findings.contains { $0.kind == .recoveryCycle }, "应发现恢复循环（account ↔ phone 互恢复）")
    }

    func testFindingsNoFalsePositiveOnEmptyGraph() {
        let findings = FindingsViewModel.findings(FindingsInput(snapshot: GraphSnapshot()))
        XCTAssertTrue(findings.isEmpty, "空图不得凭空产生任何发现")
    }

    func testFindingsPendingVerificationSurfaces() {
        let snap = DemoData.snapshot()
        let findings = FindingsViewModel.findings(FindingsInput(snapshot: snap, pendingVerifications: ["a", "b"]))
        XCTAssertTrue(findings.contains { $0.kind == .pendingVerification })
    }

    // MARK: - replace_phone flow（make-before-break 前置与闸门）

    func testReplacePhonePlanPrerequisitesOrder() throws {
        let snap = DemoData.snapshot()
        let sp = try ScenarioFlow.createPlan(
            scenarioId: ScenarioFlow.replacePhoneScenarioId,
            targetNodeId: "phone-1",
            graph: snap,
            effectiveDate: DemoData.nowIso,
            nowIso: DemoData.nowIso
        )
        XCTAssertTrue(sp.isIdentityFlow)
        // 停用旧路径必须排在验证新路径之后（plain-language：验证后才能移除旧路径）
        let retirePrereq = sp.prerequisiteEdges["phone-retire-old"] ?? []
        XCTAssertEqual(retirePrereq, ["phone-verify-new"])
        // 新路径未建立/未验证 → 停用被阻止
        XCTAssertEqual(sp.makeBeforeBreak?.status, MakeBeforeBreakStatus.blocked)
        // 时间窗顺序合法
        XCTAssertEqual(sp.temporal?.validOrder, true)
    }

    func testReplacePhoneRetireAllowedAfterVerify() throws {
        let snap = DemoData.snapshot()
        let mbb = MakeBeforeBreakEngine.evaluate(MakeBeforeBreakInput(
            newPathActions: [
                PlanAction(id: "phone-add-new", phase: .change, done: true),
                PlanAction(id: "phone-migrate", phase: .change, done: true),
            ],
            verificationActions: [
                PlanAction(
                    id: "phone-verify-new",
                    phase: .verify,
                    verification: ActionVerification(method: .futureObservation, status: .verified)
                ),
            ],
            retireActionId: "phone-retire-old",
            retireAlreadyDone: false
        ))
        XCTAssertEqual(mbb.status, MakeBeforeBreakStatus.allowed)
    }

    // MARK: - CTA 映射（task #9）

    func testCtaMapping() {
        let base = ChangePlan(
            id: "p", scenario: "replace_payment_card", title: "t",
            workflowState: .analyzed, lastAnalyzedGraphRevision: 1
        )
        var input = PlanInput(plan: base, currentGraphRevision: 1, pendingMustChange: 1)
        XCTAssertEqual(ChangePlanViewModel.view(input: input).ctaTitle, CopyZh.ctaBlocked)

        input = PlanInput(plan: base, currentGraphRevision: 1, pendingNeedsReview: 2)
        XCTAssertEqual(ChangePlanViewModel.view(input: input).ctaTitle, CopyZh.ctaReviewRequired)

        input = PlanInput(plan: base, currentGraphRevision: 2)
        XCTAssertEqual(ChangePlanViewModel.view(input: input).ctaTitle, CopyZh.ctaNeedsRevalidation)

        input = PlanInput(plan: base, currentGraphRevision: 1)
        XCTAssertEqual(ChangePlanViewModel.view(input: input).ctaTitle, CopyZh.ctaReadyWithKnownScope)

        let verifying = ChangePlan(id: "p2", scenario: "s", title: "t", workflowState: .verifying, lastAnalyzedGraphRevision: 1)
        XCTAssertEqual(ChangePlanViewModel.view(input: PlanInput(plan: verifying, currentGraphRevision: 1)).ctaTitle, CopyZh.ctaVerifying)

        let done = ChangePlan(id: "p3", scenario: "s", title: "t", workflowState: .completed, lastAnalyzedGraphRevision: 1)
        XCTAssertEqual(ChangePlanViewModel.view(input: PlanInput(plan: done, currentGraphRevision: 1)).ctaTitle, CopyZh.ctaCompleted)
    }

    func testPrerequisitePlainLanguage() {
        let actions = [
            PlanAction(id: "phone-add-new", title: CopyZh.addNewPhone, phase: .change),
            PlanAction(id: "phone-verify-new", title: CopyZh.verifyNewPhone, phase: .verify),
            PlanAction(id: "phone-retire-old", title: CopyZh.retireOldPhone, phase: .change),
        ]
        let text = ChangePlanViewModel.prerequisiteText(
            actionId: "phone-retire-old",
            actions: actions,
            edges: ["phone-retire-old": ["phone-verify-new"]]
        )
        XCTAssertTrue(text.contains(CopyZh.verifyBeforeRemove))
        let parallel = ChangePlanViewModel.prerequisiteText(actionId: "phone-add-new", actions: actions, edges: [:])
        XCTAssertEqual(parallel, CopyZh.parallel)
    }

    // MARK: - import 流水线（解析 + 解析 + 建议）

    func testImportPipelineDetectsRecurringProposal() {
        let csv = """
        日期,金额,商户,支付方式
        2029-01-05,68,视频会员,招商银行卡
        2029-02-05,68,视频会员,招商银行卡
        """
        let data = Array(csv.utf8)
        let kind = ImportFlow.detectKind(fileName: "a.csv", text: csv)
        XCTAssertEqual(kind, "csv")
        let state = try! ImportFlow.preview(
            kind: kind, data: data,
            mapping: MappingProfile(
                columns: MappingColumns(dateTime: "日期", amount: "金额", description: nil, counterparty: "商户", paymentMethod: "支付方式"),
                options: MappingOptions()
            )
        )
        XCTAssertEqual(state.summary?.total, 2)
        let proposals = ImportFlow.detectProposals(rows: state.rows)
        XCTAssertEqual(proposals.count, 1, "跨 2 个月的同商户同支付方式应产生 1 条周期扣款建议")
        XCTAssertTrue(proposals[0].confidence >= 0.16)
    }

    func testUnresolvedMerchantBecomesCandidate() {
        let csv = "日期,金额,商户\n2029-01-05,10,完全未知的新商户\n"
        let state = try! ImportFlow.preview(
            kind: "csv", data: Array(csv.utf8),
            mapping: MappingProfile(
                columns: MappingColumns(dateTime: "日期", amount: "金额", counterparty: "商户"),
                options: MappingOptions()
            )
        )
        let resolved = ImportFlow.resolveMerchants(rows: state.rows, snapshot: GraphSnapshot())
        XCTAssertEqual(resolved.candidates.count, 1)
        XCTAssertEqual(resolved.resolutions.first?.resolution, "candidate")
    }
}