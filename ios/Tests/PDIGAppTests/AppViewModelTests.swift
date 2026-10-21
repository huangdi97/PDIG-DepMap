import XCTest
import PDIGCore
@testable import PDIGApp

// PDIGApp 纯逻辑单测：导航 / Findings 组合 / replace_phone 前置与闸门 / CTA 映射。
// 运行于 macOS runner（swift test），不依赖任何 UI。

final class AppViewModelTests: XCTestCase {

    // MARK: - Production VNext read boundary

    @MainActor
    func testProductionVNextFindingsRemainLockGated() {
        let session = AppSession()
        XCTAssertNil(session.productionVNextSnapshot())
        XCTAssertNil(session.productionVNextFindings())
    }

    func testProductionRecordsDoNotProjectFuturePendingVerificationSteps() {
        let future = VIosProductionPlanAction(
            id: "future",
            title: "未来步骤",
            phase: "verify",
            done: false,
            verificationStatus: "pending",
            verificationEvidenceRefs: [],
            resolvesImpactKeys: []
        )
        let completed = VIosProductionPlanAction(
            id: "completed",
            title: "已发生步骤",
            phase: "change",
            done: true,
            verificationStatus: "pending",
            verificationEvidenceRefs: [],
            resolvesImpactKeys: []
        )

        XCTAssertFalse(vIosProductionActionHasRecordedEvent(future))
        XCTAssertTrue(vIosProductionActionHasRecordedEvent(completed))
    }

    func testProductionVNextProjectionKeepsConfirmedRealitySeparateFromReview() {
        let snapshot = GraphSnapshot(
            nodes: [
                DepNode(
                    id: "card-1",
                    kind: .paymentInstrument,
                    name: "主卡",
                    issuer: "示例银行",
                    last4: "8823"
                ),
                DepNode(id: "account-1", kind: .account, name: "账户 A"),
                DepNode(
                    id: "identity-phone-looking",
                    kind: .identityAnchor,
                    name: "+86 138 0000 8823"
                ),
                DepNode(
                    id: "identity-email-looking",
                    kind: .identityAnchor,
                    name: "user@example.com"
                ),
                DepNode(
                    id: "archived-service",
                    kind: .service,
                    name: "已归档服务",
                    archived: true
                ),
            ],
            dependencies: [
                Dependency(
                    id: "dep-active",
                    from: "card-1",
                    relation: .fundingSource,
                    to: "account-1",
                    capability: .payment,
                    criticality: .required,
                    state: .active
                ),
                Dependency(
                    id: "dep-retired",
                    from: "identity-phone-looking",
                    relation: .authenticates,
                    to: "account-1",
                    capability: .authentication,
                    criticality: .unknown,
                    state: .retired
                ),
            ],
            pendingProposals: [
                PendingProposal(
                    id: "proposal-1",
                    key: "proposal-key",
                    from: "identity-phone-looking",
                    relation: .authenticates,
                    to: "account-1",
                    capability: .authentication,
                    confidenceScore: 0.99,
                    observationCount: 3,
                    createdAt: "2026-10-21T00:00:00Z",
                    updatedAt: "2026-10-21T00:00:00Z"
                ),
            ],
            sources: [
                SourceInstanceRow(
                    id: "source-active",
                    adapterId: "statement",
                    adapterVersion: 1,
                    sourceKind: SourceKind.statementFile.wire,
                    label: "账单",
                    state: SourceInstanceState.active.wire
                ),
                SourceInstanceRow(
                    id: "source-retired",
                    adapterId: "statement",
                    adapterVersion: 1,
                    sourceKind: SourceKind.statementFile.wire,
                    label: "旧账单",
                    state: SourceInstanceState.retired.wire
                ),
            ],
            graphRevision: 7
        )

        let projected = buildIosProductionVNextSnapshot(
            snapshot: snapshot,
            plans: [],
            pendingCandidateCount: 2,
            openDriftCount: 1
        )

        XCTAssertEqual(projected.revision, 7)
        XCTAssertEqual(Set(projected.objects.map(\.id)), Set([
            "card-1", "account-1", "identity-phone-looking", "identity-email-looking",
        ]))
        XCTAssertEqual(projected.confirmedDependencies.map(\.id), ["dep-active"])
        XCTAssertFalse(projected.confirmedDependencies.contains { $0.id == "dep-retired" })
        XCTAssertEqual(projected.pendingReview.proposalCount, 1)
        XCTAssertEqual(projected.pendingReview.candidateCount, 2)
        XCTAssertEqual(projected.pendingReview.driftCount, 1)
        XCTAssertEqual(projected.pendingReview.total, 4)
        XCTAssertEqual(projected.sourceCoverage.sourceCount, 2)
        XCTAssertEqual(projected.sourceCoverage.activeSourceCount, 1)
        XCTAssertEqual(
            projected.objects.first { $0.id == "card-1" }?.confirmedDependencyCount,
            1
        )
        XCTAssertEqual(
            projected.objects.first { $0.id == "account-1" }?.confirmedDependencyCount,
            1
        )
    }

    func testGraphRepositoryPreservesGovernedNodeFieldsAcrossSQLiteRoundTrip() throws {
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("pdig-fields-roundtrip-\(UUID().uuidString)", isDirectory: true)
        defer { try? FileManager.default.removeItem(at: directory) }

        let driver = try SqliteProvider.openDatabase(atDirectory: directory)
        let repository = try GraphRepository(
            driver: driver,
            nowIso: "2026-10-21T00:00:00.000Z"
        )
        let fields = #"""
        {"identity_anchor_profile":{"version":1,"subtype":"phone_number","verification_basis_type":"user_confirmed","confirmed_at":"2026-10-21T00:00:00Z","identifier":{"value":"+86 138 0000 8823","verification_basis_type":"authoritative_source","confirmed_at":"2026-10-21T00:01:00Z"}},"region_facts":{"version":1,"items":[{"id":"number","facet":"numbering_territory","territory_code":"CN","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"2026-10-21T00:00:00Z"}]}}
        """#
        let original = GraphSnapshot(
            nodes: [
                DepNode(
                    id: "identity-1",
                    kind: .identityAnchor,
                    name: "主号",
                    fieldsJson: fields
                ),
                DepNode(
                    id: "account-plain",
                    kind: .account,
                    name: "普通账户"
                ),
            ],
            graphRevision: 3
        )

        _ = try repository.replaceGraph(original)
        let loaded = try repository.loadSnapshot()
        let identity = try XCTUnwrap(loaded.nodes.first { $0.id == "identity-1" })
        let plain = try XCTUnwrap(loaded.nodes.first { $0.id == "account-plain" })

        XCTAssertEqual(loaded.nodes.count, 2)
        XCTAssertEqual(identity.fieldsJson, fields)
        XCTAssertEqual(plain.fieldsJson, "{}", "无扩展事实的节点必须使用 canonical 空对象而不是 SQL NULL")
        XCTAssertEqual(
            confirmedIdentityAnchorProfile(
                kind: identity.kind,
                fieldsJson: identity.fieldsJson ?? ""
            )?.subtype,
            .phoneNumber
        )
        XCTAssertEqual(
            currentConfirmedRegionFacts(
                fieldsJson: identity.fieldsJson ?? ""
            ).map(\.territoryCode),
            ["CN"]
        )
    }

    func testProductionVNextPromotesOnlyGovernedIdentityProfiles() {
        let phoneFields = #"""
        {"identity_anchor_profile":{"version":1,"subtype":"phone_number","verification_basis_type":"user_confirmed","confirmed_at":"2026-10-21T00:00:00Z","identifier":{"value":"+86 138 0000 8823","verification_basis_type":"authoritative_source","confirmed_at":"2026-10-21T00:01:00Z"}},"region_facts":{"version":1,"items":[{"id":"number","facet":"numbering_territory","territory_code":"CN","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"2026-10-21T00:00:00Z"}]}}
        """#
        let emailFields = #"""
        {"identity_anchor_profile":{"version":1,"subtype":"email_address","verification_basis_type":"authoritative_source","confirmed_at":"2026-10-21T00:00:00Z","identifier":{"value":"user@example.com","verification_basis_type":"authoritative_source","confirmed_at":"2026-10-21T00:01:00Z"}}}
        """#
        let snapshot = GraphSnapshot(
            nodes: [
                DepNode(
                    id: "phone-confirmed",
                    kind: .identityAnchor,
                    name: "主号",
                    fieldsJson: phoneFields
                ),
                DepNode(
                    id: "email-confirmed",
                    kind: .identityAnchor,
                    name: "恢复邮箱",
                    fieldsJson: emailFields
                ),
                DepNode(
                    id: "phone-unconfirmed",
                    kind: .identityAnchor,
                    name: "+86 139 0000 0000",
                    fieldsJson: #"{"subtype":"phone_number"}"#
                ),
            ]
        )

        let projected = buildIosProductionVNextSnapshot(
            snapshot: snapshot,
            plans: [],
            pendingCandidateCount: 0,
            openDriftCount: 0
        )

        let phone = projected.objects.first { $0.id == "phone-confirmed" }
        XCTAssertEqual(phone?.surfaceKind, .phoneIdentity)
        XCTAssertEqual(phone?.identitySubtype, IdentityAnchorSubtype.phoneNumber.wire)
        XCTAssertEqual(phone?.identityIdentifierValue, "+86 138 0000 8823")
        XCTAssertEqual(phone?.confirmedRegionCodes, ["CN"])

        let email = projected.objects.first { $0.id == "email-confirmed" }
        XCTAssertEqual(email?.surfaceKind, .emailIdentity)
        XCTAssertEqual(email?.identitySubtype, IdentityAnchorSubtype.emailAddress.wire)
        XCTAssertEqual(email?.identityIdentifierValue, "user@example.com")

        XCTAssertEqual(
            projected.objects.first { $0.id == "phone-unconfirmed" }?.surfaceKind,
            .identityAnchorGeneric
        )
    }

    func testProductionVNextNeverInfersPhoneOrEmailSubtypeFromIdentityName() {
        let snapshot = GraphSnapshot(
            nodes: [
                DepNode(id: "phone-looking", kind: .identityAnchor, name: "+44 7911 123456"),
                DepNode(id: "email-looking", kind: .identityAnchor, name: "me@example.com"),
                DepNode(id: "real-account", kind: .account, name: "me@example.com"),
            ]
        )

        let projected = buildIosProductionVNextSnapshot(
            snapshot: snapshot,
            plans: [],
            pendingCandidateCount: 0,
            openDriftCount: 0
        )

        XCTAssertEqual(
            projected.objects.first { $0.id == "phone-looking" }?.surfaceKind,
            .identityAnchorGeneric
        )
        XCTAssertEqual(
            projected.objects.first { $0.id == "email-looking" }?.surfaceKind,
            .identityAnchorGeneric
        )
        XCTAssertEqual(
            projected.objects.first { $0.id == "real-account" }?.surfaceKind,
            .account
        )
    }

    func testProductionVNextCoreAssetFiltersDoNotPromoteGenericIdentity() {
        let snapshot = VIosProductionSnapshot(
            revision: 12,
            objects: [
                VIosProductionObject(
                    id: "card-1", kind: "payment_instrument", name: "主卡",
                    surfaceKind: .paymentAsset, issuer: "银行", last4: "8823",
                    identitySubtype: nil, identityIdentifierValue: nil,
                    confirmedRegionCodes: ["CN"], confirmedDependencyCount: 1
                ),
                VIosProductionObject(
                    id: "phone-1", kind: "identity_anchor", name: "主号",
                    surfaceKind: .phoneIdentity, issuer: nil, last4: nil,
                    identitySubtype: "phone_number", identityIdentifierValue: "+86 138 0000 8823",
                    confirmedRegionCodes: ["CN"], confirmedDependencyCount: 2
                ),
                VIosProductionObject(
                    id: "identity-generic", kind: "identity_anchor", name: "+86 139 0000 0000",
                    surfaceKind: .identityAnchorGeneric, issuer: nil, last4: nil,
                    identitySubtype: nil, identityIdentifierValue: nil,
                    confirmedRegionCodes: ["CN"], confirmedDependencyCount: 0
                ),
            ],
            confirmedDependencies: [],
            plans: [],
            pendingReview: VIosProductionReviewSummary(
                proposalCount: 0, candidateCount: 0, driftCount: 0
            ),
            sourceCoverage: VIosProductionSourceCoverage(
                sourceCount: 0, activeSourceCount: 0
            )
        )

        XCTAssertEqual(
            vIosProductionObjects(
                snapshot: snapshot, surfaceKind: .paymentAsset, regionFilter: nil
            ).map(\.id),
            ["card-1"]
        )
        XCTAssertEqual(
            vIosProductionObjects(
                snapshot: snapshot, surfaceKind: .phoneIdentity, regionFilter: "CN"
            ).map(\.id),
            ["phone-1"]
        )
        XCTAssertFalse(
            vIosProductionObjects(
                snapshot: snapshot, surfaceKind: .phoneIdentity, regionFilter: nil
            ).contains { $0.id == "identity-generic" },
            "名字像手机号的 generic identity 不得进入号码 collection"
        )
        XCTAssertTrue(
            vIosProductionObjects(
                snapshot: snapshot, surfaceKind: .paymentAsset, regionFilter: "HK"
            ).isEmpty
        )
    }

    func testProductionVNextSecondaryProjectionUsesOnlyConfirmedRegionFacts() {
        let snapshot = VIosProductionSnapshot(
            revision: 9,
            objects: [
                VIosProductionObject(
                    id: "account-cn",
                    kind: "account",
                    name: "中国账户",
                    surfaceKind: .account,
                    issuer: nil,
                    last4: nil,
                    identitySubtype: nil,
                    identityIdentifierValue: nil,
                    confirmedRegionCodes: ["CN"],
                    confirmedDependencyCount: 2
                ),
                VIosProductionObject(
                    id: "account-unknown",
                    kind: "account",
                    name: "地区未记录账户",
                    surfaceKind: .account,
                    issuer: nil,
                    last4: nil,
                    identitySubtype: nil,
                    identityIdentifierValue: nil,
                    confirmedRegionCodes: [],
                    confirmedDependencyCount: 0
                ),
                VIosProductionObject(
                    id: "service-cn",
                    kind: "service",
                    name: "中国服务",
                    surfaceKind: .service,
                    issuer: nil,
                    last4: nil,
                    identitySubtype: nil,
                    identityIdentifierValue: nil,
                    confirmedRegionCodes: ["CN"],
                    confirmedDependencyCount: 1
                ),
            ],
            confirmedDependencies: [],
            plans: [],
            pendingReview: VIosProductionReviewSummary(
                proposalCount: 0,
                candidateCount: 0,
                driftCount: 0
            ),
            sourceCoverage: VIosProductionSourceCoverage(
                sourceCount: 0,
                activeSourceCount: 0
            )
        )

        XCTAssertEqual(
            vIosProductionSecondaryObjects(
                snapshot: snapshot,
                kind: .account,
                regionFilter: nil
            ).map(\.id),
            ["account-cn", "account-unknown"]
        )
        XCTAssertEqual(
            vIosProductionSecondaryObjects(
                snapshot: snapshot,
                kind: .account,
                regionFilter: "CN"
            ).map(\.id),
            ["account-cn"],
            "地区未记录不等于属于当前地区"
        )
        XCTAssertTrue(
            vIosProductionSecondaryObjects(
                snapshot: snapshot,
                kind: .account,
                regionFilter: "HK"
            ).isEmpty
        )
    }

    func testProductionVNextPrivacyMasksEveryObjectIdentityKind() {
        let cases: [(VIosProductionSurfaceKind, String)] = [
            (.paymentAsset, "支付工具（已遮蔽）"),
            (.account, "账户（已遮蔽）"),
            (.service, "服务（已遮蔽）"),
            (.membership, "会员 / 资格（已遮蔽）"),
            (.device, "设备（已遮蔽）"),
            (.phoneIdentity, "手机号身份（已遮蔽）"),
            (.emailIdentity, "邮箱身份（已遮蔽）"),
            (.identityAnchorGeneric, "身份对象（已遮蔽）"),
            (.customGeneric, "自定义对象（已遮蔽）"),
        ]
        for (index, item) in cases.enumerated() {
            let object = VIosProductionObject(
                id: "obj-\(index)",
                kind: "custom",
                name: "敏感名称 \(index)",
                surfaceKind: item.0,
                issuer: nil,
                last4: nil,
                identitySubtype: nil,
                identityIdentifierValue: nil,
                confirmedRegionCodes: [],
                confirmedDependencyCount: 0
            )
            XCTAssertEqual(
                vIosVisibleProductionObjectName(object, privacyMask: true),
                item.1
            )
            XCTAssertEqual(
                vIosVisibleProductionObjectName(object, privacyMask: false),
                "敏感名称 \(index)"
            )
        }
    }

    func testProductionVNextPreservesDoneVerificationAndStalePlanTruth() {
        let action = PlanAction(
            id: "change-1",
            title: "迁移关键账户",
            phase: .change,
            done: true,
            resolvesImpactKeys: ["account-1|recovery"],
            verification: ActionVerification(
                method: .manualConfirmation,
                status: .pending,
                evidenceRefs: ["evidence-1"]
            )
        )
        let plan = ChangePlan(
            id: "plan-1",
            scenario: "replace_phone_number",
            title: "更换手机号",
            workflowState: .inProgress,
            baselineGraphRevision: 4,
            lastAnalyzedGraphRevision: 6,
            targetNodeId: "identity-1",
            actions: [action]
        )
        let snapshot = GraphSnapshot(
            nodes: [
                DepNode(id: "identity-1", kind: .identityAnchor, name: "旧身份")
            ],
            graphRevision: 7
        )

        let projected = buildIosProductionVNextSnapshot(
            snapshot: snapshot,
            plans: [plan],
            pendingCandidateCount: 0,
            openDriftCount: 0
        )
        let output = projected.plans[0]
        let outputAction = output.actions[0]

        XCTAssertTrue(output.needsReanalysis)
        XCTAssertEqual(output.currentGraphRevision, 7)
        XCTAssertEqual(output.lastAnalyzedGraphRevision, 6)
        XCTAssertTrue(outputAction.done)
        XCTAssertEqual(outputAction.verificationStatus, ActionVerificationStatus.pending.wire)
        XCTAssertEqual(outputAction.verificationEvidenceRefs, ["evidence-1"])
        XCTAssertEqual(outputAction.resolvesImpactKeys, ["account-1|recovery"])
    }

    @MainActor
    func testProductionVNextProjectionCannotBypassSessionLockGate() {
        let session = AppSession()
        session.snapshot = GraphSnapshot(
            nodes: [DepNode(id: "account-1", kind: .account, name: "账户 A")]
        )

        session.phase = .locked
        XCTAssertNil(session.productionVNextSnapshot())

        session.phase = .ready
        XCTAssertEqual(session.productionVNextSnapshot()?.objects.map(\.id), ["account-1"])
    }

    func testGraphRepositoryRoundTripsPersistedPlanActions() throws {
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("pdig-plan-roundtrip-\(UUID().uuidString)", isDirectory: true)
        defer { try? FileManager.default.removeItem(at: directory) }

        let driver = try SqliteProvider.openDatabase(atDirectory: directory)
        let repository = try GraphRepository(
            driver: driver,
            nowIso: "2026-10-21T00:00:00.000Z"
        )
        let plan = ChangePlan(
            id: "plan-roundtrip",
            templateId: "replace_phone_number",
            scenario: "replace_phone_number",
            title: "更换手机号",
            workflowState: .inProgress,
            baselineGraphRevision: 4,
            lastAnalyzedGraphRevision: 5,
            targetNodeId: "identity-old",
            effectiveDate: "2026-11-01",
            actions: [
                PlanAction(
                    id: "change-1",
                    title: "迁移关键账户",
                    phase: .change,
                    done: true,
                    resolvesImpactKeys: ["account-1|recovery"],
                    verification: ActionVerification(
                        method: .futureObservation,
                        status: .pending,
                        evidenceRefs: ["evidence-1"],
                        expectedFromNodeId: "identity-new",
                        expectedToNodeId: "account-1"
                    )
                ),
            ]
        )

        try repository.savePlan(plan)
        let loaded = try repository.loadPlans()

        XCTAssertEqual(loaded.count, 1)
        XCTAssertEqual(loaded[0], plan)
        XCTAssertEqual(loaded[0].actions[0].verification?.status, .pending)
        XCTAssertEqual(loaded[0].actions[0].verification?.evidenceRefs, ["evidence-1"])
        XCTAssertEqual(loaded[0].actions[0].resolvesImpactKeys, ["account-1|recovery"])
    }

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

    func testSecondaryCollectionsHonorRegionScopeWithoutInventingObjects() {
        let allAccounts = vFilteredSecondaryAssets(
            VNextDemoFixture.allSecondaryAssets,
            kind: .account,
            regionFilter: nil
        )
        XCTAssertEqual(Set(allAccounts.map(\.id)), Set(["acct-wechat", "acct-hsbc", "acct-amazon"]))

        let chinaAccounts = vFilteredSecondaryAssets(
            VNextDemoFixture.allSecondaryAssets,
            kind: .account,
            regionFilter: "CN"
        )
        XCTAssertEqual(chinaAccounts.map(\.id), ["acct-wechat"])

        let singaporeAccounts = vFilteredSecondaryAssets(
            VNextDemoFixture.allSecondaryAssets,
            kind: .account,
            regionFilter: "SG"
        )
        XCTAssertTrue(singaporeAccounts.isEmpty)
    }

    func testServiceCollectionAndRegionSummaryShareOneReferenceProjection() {
        let projected = VNextDemoFixture.secondaryAssets(for: .service)
        XCTAssertEqual(Set(projected.map(\.id)), Set(VNextDemoFixture.services.map(\.id)))
        XCTAssertEqual(
            projected.first { $0.id == "svc-wxpay" }?.relationCount,
            VNextDemoFixture.relations.filter { $0.to == "svc-wxpay" || $0.from == "svc-wxpay" }.count
        )

        for region in VNextDemoFixture.regionSummaries() {
            let collectionCount = projected.filter { $0.region == region.regionCode }.count
            XCTAssertEqual(
                region.serviceCount,
                collectionCount,
                "地区总览与 Services collection 必须使用同一服务投影: \(region.regionCode)"
            )
        }
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

    func testFindingsNeverTreatGenericIdentityAsConfirmedPhoneFailureDomain() {
        let snapshot = GraphSnapshot(
            nodes: [
                DepNode(
                    id: "identity-generic",
                    kind: .identityAnchor,
                    name: "+86 139 0000 0000"
                ),
                DepNode(id: "account-1", kind: .account, name: "账户")
            ],
            dependencies: [
                Dependency(
                    id: "recovery-1",
                    from: "identity-generic",
                    relation: .recovers,
                    to: "account-1",
                    capability: .recovery,
                    criticality: .unknown
                ),
                Dependency(
                    id: "recovery-2",
                    from: "identity-generic",
                    relation: .recovers,
                    to: "account-1",
                    capability: .recovery,
                    criticality: .unknown
                ),
            ]
        )

        let findings = FindingsViewModel.findings(FindingsInput(snapshot: snapshot))
        XCTAssertFalse(
            findings.contains { $0.kind == .sharedFailureDomain },
            "generic identity_anchor 不得被推断成已确认 PHONE_NUMBER failure domain"
        )
    }

    func testProductionFindingPolicyExcludesPendingProposalCycles() {
        let snapshot = GraphSnapshot(
            nodes: [
                DepNode(id: "account-a", kind: .account, name: "A"),
                DepNode(id: "account-b", kind: .account, name: "B"),
            ],
            dependencies: [
                Dependency(
                    id: "confirmed-a-b",
                    from: "account-a",
                    relation: .recovers,
                    to: "account-b",
                    capability: .recovery,
                    criticality: .unknown
                )
            ],
            pendingProposals: [
                PendingProposal(
                    id: "proposal-b-a",
                    key: "proposal-b-a",
                    from: "account-b",
                    relation: .recovers,
                    to: "account-a",
                    capability: .recovery,
                    confidenceScore: 0.9,
                    observationCount: 2,
                    createdAt: "2026-10-21T00:00:00Z",
                    updatedAt: "2026-10-21T00:00:00Z"
                )
            ]
        )

        let reviewAware = FindingsViewModel.findings(
            FindingsInput(snapshot: snapshot, includePendingProposalCycles: true)
        )
        XCTAssertTrue(
            reviewAware.contains {
                $0.kind == .recoveryCycle && $0.id.hasPrefix("pcycle-")
            }
        )

        let production = FindingsViewModel.findings(
            FindingsInput(snapshot: snapshot, includePendingProposalCycles: false)
        )
        XCTAssertFalse(
            production.contains { $0.id.hasPrefix("pcycle-") },
            "Production weaknesses 不得把 pending Proposal cycle 混入已确认 Reality finding"
        )
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