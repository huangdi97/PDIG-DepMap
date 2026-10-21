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

    @MainActor
    func testVNextFiveRootOwnershipAndPrivacyDefault() {
        let model = VNextModel()
        XCTAssertFalse(model.privacyMask, "隐私遮蔽按冻结产品决策默认关闭")
        XCTAssertFalse(model.reduceMotion)
        XCTAssertTrue(model.showUpcoming, "即将到来默认显示；关闭只影响 Presentation")
        XCTAssertEqual(VScreen.now.primaryTab, .now)
        XCTAssertEqual(VScreen.accounts.primaryTab, .infrastructure)
        XCTAssertEqual(VScreen.secondaryDetail(.email, "mail-main").primaryTab, .infrastructure)
        XCTAssertEqual(VScreen.change.primaryTab, .change)
        XCTAssertEqual(VScreen.changePhone.primaryTab, .change)
        XCTAssertEqual(VScreen.changeCard.primaryTab, .change)
        XCTAssertEqual(VScreen.records.primaryTab, .records)
        XCTAssertEqual(VScreen.personalization.primaryTab, .me)
        XCTAssertEqual(VScreen.sources.primaryTab, .me)
        XCTAssertEqual(VScreen.settings.primaryTab, .me)
        XCTAssertEqual(VScreen.me.primaryTab, .me)
    }

    func testVNextPresentationProfileDoesNotMaskByDefault() {
        let card = VPresentationProfile.defaultFor(
            targetType: "card",
            targetId: "card-1",
            preset: "minimal"
        )
        let number = VPresentationProfile.defaultFor(
            targetType: "phoneNumber",
            targetId: "phone-1",
            preset: "country"
        )
        XCTAssertFalse(card.maskSensitive)
        XCTAssertFalse(number.maskSensitive)
    }

    func testSecondaryCollectionDensityTranslatesByWidthClass() {
        XCTAssertEqual(vSecondaryCollectionColumnCount(isRegularWidth: false), 1)
        XCTAssertEqual(vSecondaryCollectionColumnCount(isRegularWidth: true), 2)
    }

    func testVNextSecondaryReferenceSurfacesAreNotPlaceholders() {
        XCTAssertFalse(VNextDemoFixture.secondaryAssets(for: .account).isEmpty)
        XCTAssertFalse(VNextDemoFixture.secondaryAssets(for: .email).isEmpty)
        XCTAssertFalse(VNextDemoFixture.secondaryAssets(for: .device).isEmpty)
        XCTAssertFalse(VNextDemoFixture.secondaryAssets(for: .service).isEmpty)
        XCTAssertFalse(VNextDemoFixture.records.isEmpty)
        XCTAssertFalse(VNextDemoFixture.findings.isEmpty)
    }

    func testVNextGlobalPrivacyRedactsIdentityBearingWorkspaceCopy() {
        let attention = VNextDemoFixture.attentionItems[0]
        let change = VNextDemoFixture.activeChanges[0]
        let upcoming = VNextDemoFixture.upcoming[0]
        let record = VNextDemoFixture.records[0]
        let migration = VNextDemoFixture.changeMigrations[0]
        let number = VNextDemoFixture.numberById(VNextDemoFixture.changeOldNumberId)!

        XCTAssertNotEqual(vVisibleAttentionTitle(attention, privacyMask: false), "需要关注事项（已遮蔽）")
        XCTAssertEqual(vVisibleAttentionTitle(attention, privacyMask: true), "需要关注事项（已遮蔽）")
        XCTAssertEqual(vVisibleActiveChangeTitle(change, privacyMask: true), "变更计划（已遮蔽）")
        XCTAssertEqual(vVisibleUpcomingTitle(upcoming, privacyMask: true), "维护事项（已遮蔽）")
        XCTAssertEqual(vVisibleRecordPlanTitle(record, privacyMask: true), "变更计划（已遮蔽）")
        XCTAssertEqual(vVisibleRecordActionTitle(record, privacyMask: true), "操作步骤（已遮蔽）")
        XCTAssertEqual(vVisibleMigrationService(migration, privacyMask: true), "关联服务（已遮蔽）")
        let card = VNextDemoFixture.cards[0]
        let service = VNextDemoFixture.services[0]
        XCTAssertEqual(vVisibleCardNickname(card, privacyMask: true), "支付工具（已遮蔽）")
        XCTAssertEqual(vVisibleCardIssuer(card, privacyMask: true), "发行方已遮蔽")
        XCTAssertEqual(vVisibleCardLast4(card, privacyMask: true), "••••")
        XCTAssertEqual(vVisibleNumberNickname(number, privacyMask: true), "手机号身份（已遮蔽）")
        XCTAssertEqual(vVisibleNumberIdentifier(number, privacyMask: true), "号码已遮蔽")
        XCTAssertEqual(vVisibleNumberCarrier(number, privacyMask: true), "运营商信息已遮蔽")
        XCTAssertEqual(vVisibleServiceName(service, privacyMask: true), "关联服务（已遮蔽）")
    }

    func testVNextReferenceSearchMatchesRawFieldsWithoutPrivacyEcho() {
        let cardResults = vReferenceSearchResults(query: "招商", privacyMask: true)
        let card = cardResults.first { $0.id == "card:card-cn-1" }
        XCTAssertNotNil(card, "隐私遮蔽不能让已记录对象变得不可搜索")
        XCTAssertEqual(card?.title, "支付工具（已遮蔽）")
        XCTAssertFalse(card?.subtitle.contains("招商银行") == true)
        XCTAssertFalse(card?.subtitle.contains("4417") == true)

        let phoneResults = vReferenceSearchResults(query: "中国移动", privacyMask: true)
        let phone = phoneResults.first { $0.id == "number:num-cn-1" }
        XCTAssertNotNil(phone)
        XCTAssertEqual(phone?.title, "手机号身份（已遮蔽）")
        XCTAssertFalse(phone?.subtitle.contains("中国移动") == true)
        XCTAssertFalse(phone?.subtitle.contains("8823") == true)
    }

    @MainActor
    func testVNextFocusedChangeRoutesReturnToChangeCenter() {
        let model = VNextModel()
        model.navigate(.changePhone)
        model.back()
        XCTAssertEqual(model.screen, .change)

        model.navigate(.changeCard)
        model.back()
        XCTAssertEqual(model.screen, .change)
    }

    func testVNextCardChangeNeverPromotesPlanProjectionToReality() {
        XCTAssertEqual(
            vCardChangeServiceStatus(phase: .current, hasReplacement: false),
            "当前依赖"
        )
        XCTAssertEqual(
            vCardChangeServiceStatus(phase: .transition, hasReplacement: false),
            "等待选择替代卡"
        )
        XCTAssertEqual(
            vCardChangeServiceStatus(phase: .transition, hasReplacement: true),
            "计划迁移"
        )
        XCTAssertEqual(
            vCardChangeServiceStatus(phase: .after, hasReplacement: true),
            "计划迁移"
        )
        XCTAssertFalse(
            vCardChangeServiceStatus(phase: .after, hasReplacement: true).contains("已迁移")
        )
    }

    func testVNextReferenceSearchAdvertisesOnlyImplementedRoutes() {
        let quick = vReferenceSearchResults(query: "", privacyMask: false)
        XCTAssertTrue(quick.contains { $0.destination == .screen(.sources) })
        XCTAssertTrue(quick.contains { $0.destination == .screen(.settings) })
        XCTAssertTrue(quick.contains { $0.destination == .screen(.personalization) })
        XCTAssertTrue(quick.contains { $0.destination == .screen(.change) })
        XCTAssertTrue(quick.contains { $0.destination == .screen(.changePhone) })
        XCTAssertTrue(quick.contains { $0.destination == .screen(.changeCard) })
        XCTAssertFalse(quick.contains { $0.title.contains("待复核") })
        XCTAssertFalse(quick.contains { $0.title.contains("导入") })
        XCTAssertFalse(quick.contains { $0.title.contains("手工") })
    }

    @MainActor
    func testVNextReferenceSearchOpensRealDestinations() {
        let model = VNextModel()
        model.searchShown = true
        let account = vReferenceSearchResults(query: "微信账户", privacyMask: false)
            .first { $0.id == "secondary:account:acct-wechat" }
        XCTAssertNotNil(account)
        if let account {
            vReferenceOpenSearchResult(account, model: model)
        }
        XCTAssertEqual(model.screen, .secondaryDetail(.account, "acct-wechat"))
        XCTAssertFalse(model.searchShown)

        model.searchShown = true
        let region = vReferenceSearchResults(query: "香港", privacyMask: false)
            .first { $0.id == "region:HK" }
        XCTAssertNotNil(region)
        if let region {
            vReferenceOpenSearchResult(region, model: model)
        }
        XCTAssertEqual(model.regionFilter, "HK")
        XCTAssertEqual(model.screen, .overview)
        XCTAssertFalse(model.searchShown)
    }

    func testVNextRecoveryRoleNeverClaimsUniqueRecoveryPath() {
        XCTAssertEqual(VCopy.recoveryOnly, "恢复用途")
        XCTAssertEqual(VCopy.recoveryOnlyShort, "恢复")
        XCTAssertFalse(VCopy.recoveryOnly.contains("唯一"))
        XCTAssertFalse(VCopy.recoveryOnlyShort.contains("唯一"))
        XCTAssertTrue(VCopy.riskBody.contains("路径分析确认"))
    }

    func testVNextReferenceAttentionNeverInfersUniqueRecoveryPath() {
        let attentionCopy = VNextDemoFixture.attentionItems.map(\.title).joined(separator: "\n")
        XCTAssertFalse(attentionCopy.contains("唯一恢复路径"))
        XCTAssertTrue(attentionCopy.contains("唯一性未知"))
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