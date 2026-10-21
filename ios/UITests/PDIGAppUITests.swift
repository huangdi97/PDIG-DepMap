// PDIGAppUITests —— XCUITest 冒烟（closure/ios-xcuitest）。
//
// 流程：Home → 场景中心 → replace_phone_number 设置 → 查看影响(Impact) →
// 恢复路径 → 共享故障点 → 变更计划(ChangePlan) → 备份/恢复 → 设置 →
// 删除所有数据（断言确认对话框后取消）。
//
// 诚实口径：
//  - 本包此前没有任何 accessibilityIdentifier，全部断言使用真实可见文案
//    （Button/Text 的 accessibility label），不虚构标识符。
//  - App 侧新增 `--uitest-demo` 启动参数（仅测试脚手架，不改产品逻辑）：
//    注入演示图并跳过 Onboarding/Lock —— 模拟器无法通过 LocalAuthentication
//    生物识别解锁，必须直入 ready。
//  - 每个关键屏附加 XCUIScreen 截图到 xcresult（IOS_VISUAL 证据）。
//  - 同一套测试在 iPhone 与 iPad 两个 destination 上各跑一遍（workflow 控制）。

import XCTest

final class PDIGAppUITests: XCTestCase {

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    /// 以演示模式启动 App。
    private func launchApp() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments = ["--uitest-demo"]
        app.launch()
        return app
    }

    /// 以 vNext synthetic reference 模式启动 App。
    ///
    /// This is not a Production Reality bypass: it only opens the explicitly
    /// synthetic reference shell so iPhone/iPad visual translation can be
    /// tested on a real iOS Simulator.
    private func launchVNextReference(
        extraArguments: [String] = []
    ) -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments = ["--uitest-vnext"] + extraArguments
        app.launch()
        return app
    }

    private func vnextRoot(
        _ app: XCUIApplication,
        id: String,
        fallbackLabel _: String
    ) -> XCUIElement {
        // SwiftUI TabView exposes phone roots as buttons, while List/sidebar
        // rows on iPad may surface as cells/other accessibility elements.
        // Stable identifier is the cross-form-factor contract; visible labels
        // are asserted separately and never used as a navigation fallback.
        app.descendants(matching: .any)
            .matching(NSPredicate(format: "identifier == %@", id))
            .firstMatch
    }

    private func vnextRoute(_ app: XCUIApplication, id: String) -> XCUIElement {
        app.descendants(matching: .any)
            .matching(NSPredicate(format: "identifier == %@", id))
            .firstMatch
    }

    /// 把当前模拟器画面附加进 xcresult（IOS_VISUAL 证据，keepAlways）。
    private func attachScreenshot(_ app: XCUIApplication, name: String) {
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    /// 等待元素出现 → 等待可用（若 disabled）→ 必要时滚动 → 点击。
    private func tapWhenReady(
        _ element: XCUIElement,
        in app: XCUIApplication,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        // SwiftUI may not materialize controls inside a LazyVGrid until their
        // enclosing ScrollView approaches the viewport. Searching only after a
        // 15-second existence wait deadlocks on legitimate below-fold entries.
        // Give immediately visible controls a short fast path, then scroll the
        // real product surface until the same query materializes.
        var attempts = 0
        if !element.waitForExistence(timeout: 2) {
            while !element.exists && attempts < 8 {
                app.swipeUp()
                attempts += 1
                _ = element.waitForExistence(timeout: 1)
            }
        }
        XCTAssertTrue(element.exists, "等待元素出现: \(element)", file: file, line: line)
        guard element.exists else { return }

        if !element.isEnabled {
            let predicate = NSPredicate(format: "isEnabled == true")
            wait(for: [expectation(for: predicate, evaluatedWith: element)], timeout: 10)
        }
        while !element.isHittable && attempts < 10 {
            app.swipeUp()
            attempts += 1
        }
        XCTAssertTrue(element.isHittable, "元素存在但不可点击: \(element)", file: file, line: line)
        if element.isHittable {
            element.tap()
        }
    }

    /// 卡片式按钮（VStack 内标题+副标题两个 Text）的 accessibility label 会被拼接
    /// 为 “标题, 副标题”。用 BEGINSWITH 前缀匹配，兼容两种拼接行为。
    private func buttonLabelStarts(_ app: XCUIApplication, _ prefix: String) -> XCUIElement {
        app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", prefix)).firstMatch
    }

    /// 全流程冒烟。iPhone / iPad 通用（两个 destination 都执行本测试）。
    func testSmokeFlowHomeScenarioPhonePlanBackupSettings() throws {
        let app = launchApp()

        // 1. Home（会话就绪后的根屏）
        XCTAssertTrue(app.staticTexts["个人数字依赖图"].waitForExistence(timeout: 20), "Home 未出现")
        attachScreenshot(app, name: "01-home")

        // 2. 场景中心（底部导航 tab）
        tapWhenReady(app.buttons["场景"], in: app)
        XCTAssertTrue(app.staticTexts["场景中心"].waitForExistence(timeout: 10), "场景中心未出现")
        attachScreenshot(app, name: "02-scenario-center")

        // 3. replace_phone_number 设置
        tapWhenReady(buttonLabelStarts(app, "更换手机号"), in: app)
        XCTAssertTrue(app.staticTexts["场景设置"].waitForExistence(timeout: 10), "换号设置未出现")
        attachScreenshot(app, name: "03-phone-setup")

        // 选择旧手机号（演示图 identityAnchor phone-1）
        tapWhenReady(app.buttons["138****0001"], in: app)

        // 4. 查看影响（Impact）—— make-before-break 顺序闸门文案
        tapWhenReady(app.buttons["查看影响"], in: app)
        XCTAssertTrue(app.staticTexts["查看影响"].waitForExistence(timeout: 10), "影响步骤未出现")
        attachScreenshot(app, name: "04-impact")

        // 恢复路径（必须先完成/验证后才能移除旧路径）
        tapWhenReady(app.buttons["下一步"], in: app)
        XCTAssertTrue(app.staticTexts["查看恢复路径"].waitForExistence(timeout: 10), "恢复路径步骤未出现")

        // 共享故障点
        tapWhenReady(app.buttons["下一步"], in: app)
        XCTAssertTrue(app.staticTexts["查看共享故障点"].waitForExistence(timeout: 10), "共享故障点步骤未出现")

        // 5. 变更计划（ChangePlan）
        tapWhenReady(app.buttons["下一步"], in: app)
        XCTAssertTrue(app.staticTexts["变更计划"].waitForExistence(timeout: 10), "变更计划步骤未出现")
        attachScreenshot(app, name: "05-change-plan")

        // 返回：换号流程 → 场景中心 → Home
        tapWhenReady(app.buttons["返回"], in: app)
        XCTAssertTrue(app.staticTexts["场景中心"].waitForExistence(timeout: 10), "未回到场景中心")
        tapWhenReady(app.buttons["返回"], in: app)
        XCTAssertTrue(app.staticTexts["个人数字依赖图"].waitForExistence(timeout: 10), "未回到 Home")

        // 6. 设置
        tapWhenReady(app.buttons["设置"], in: app)
        XCTAssertTrue(app.staticTexts["设置"].waitForExistence(timeout: 10), "设置未出现")
        attachScreenshot(app, name: "06-settings")

        // 7. 备份 / 恢复（导出加密备份 → BackupScreen）
        tapWhenReady(app.buttons["导出加密备份"], in: app)
        XCTAssertTrue(app.secureTextFields["设置导出密码"].waitForExistence(timeout: 10), "备份屏未出现")
        attachScreenshot(app, name: "07-backup")
        tapWhenReady(app.buttons["返回"], in: app)

        // 8. 删除所有数据（确认对话框出现即断言，然后取消，不真的删数据）
        tapWhenReady(app.buttons["删除所有数据"], in: app)
        XCTAssertTrue(app.staticTexts["确认删除所有数据？"].waitForExistence(timeout: 10), "删除确认对话框未出现")
        attachScreenshot(app, name: "08-delete-all-data-dialog")
        tapWhenReady(app.buttons["取消"], in: app)
    }

    /// iPad/iPhone 通用最小启动测试：Home + 主导航可达。
    func testHomeReachableOnAnyFormFactor() throws {
        let app = launchApp()
        XCTAssertTrue(app.staticTexts["个人数字依赖图"].waitForExistence(timeout: 20), "Home 未出现")
        XCTAssertTrue(app.buttons["场景"].exists, "底部导航 场景 不可达")
        XCTAssertTrue(app.buttons["设置"].exists, "底部导航 设置 不可达")
        attachScreenshot(app, name: "00-home-minimal")
    }

    /// Production VNext pilot runtime gate.
    ///
    /// This path is explicit opt-in and read-only. It must render the session's
    /// lock-gated Production projection, not VNextDemoFixture reference objects.
    func testProductionVNextPilotReadsSessionProjectionAndHonorsPrivacy() throws {
        let app = XCUIApplication()
        app.launchArguments = ["--uitest-production-vnext"]
        app.launch()

        XCTAssertTrue(
            vnextRoute(app, id: "pdig.ios.production-vnext.root")
                .waitForExistence(timeout: 20),
            "Production VNext pilot root 未出现"
        )

        tapWhenReady(
            vnextRoute(app, id: "pdig.ios.production-vnext.nav.infrastructure"),
            in: app
        )
        XCTAssertTrue(
            vnextRoute(app, id: "pdig.ios.production-vnext.infrastructure")
                .waitForExistence(timeout: 10),
            "Production 基础设施工作区未出现"
        )

        tapWhenReady(
            vnextRoute(app, id: "pdig.ios.production-vnext.infrastructure.accounts"),
            in: app
        )
        let account = vnextRoute(
            app,
            id: "pdig.ios.production-vnext.object.accounts.acct-bank"
        )
        XCTAssertTrue(
            account.waitForExistence(timeout: 10),
            "真实 session projection 的账户没有进入 Production collection"
        )
        XCTAssertTrue(
            app.staticTexts["招商银行账户"].exists,
            "Production collection 没有显示 session 中的真实投影对象"
        )
        XCTAssertFalse(
            vnextRoute(
                app,
                id: "pdig.ios.reference.secondary.item.accounts.acct-wechat"
            ).exists,
            "Production pilot 不得回退到 synthetic reference fixture"
        )
        attachScreenshot(app, name: "production-vnext-pilot-account")

        tapWhenReady(
            vnextRoute(app, id: "pdig.ios.production-vnext.nav.me"),
            in: app
        )
        let privacy = vnextRoute(app, id: "pdig.ios.production-vnext.privacy")
        XCTAssertTrue(privacy.waitForExistence(timeout: 10), "Production 隐私开关未出现")
        tapWhenReady(privacy, in: app)

        tapWhenReady(
            vnextRoute(app, id: "pdig.ios.production-vnext.nav.infrastructure"),
            in: app
        )
        XCTAssertTrue(
            app.staticTexts["账户（已遮蔽）"].firstMatch.waitForExistence(timeout: 10),
            "Production 全局遮蔽没有覆盖账户身份"
        )
        XCTAssertFalse(
            app.staticTexts["招商银行账户"].exists,
            "Production 遮蔽后仍泄露账户名称"
        )
        attachScreenshot(app, name: "production-vnext-pilot-account-masked")
    }

    /// vNext reference translation runtime gate.
    ///
    /// iPhone exercises the five bottom roots; iPad exercises the five sidebar
    /// roots. Both must expose the same secondary infrastructure pages.
    func testVNextAccessibilityLargeKeepsCoreNavigationAndSecondaryDetailUsable() throws {
        let app = launchVNextReference(
            extraArguments: [
                "-UIPreferredContentSizeCategoryName",
                "UICTContentSizeCategoryAccessibilityLarge"
            ]
        )

        let now = vnextRoot(app, id: "pdig.nav.now", fallbackLabel: "现在")
        XCTAssertTrue(now.waitForExistence(timeout: 20), "Accessibility Large 下 Now 不可达")

        let me = vnextRoot(app, id: "pdig.nav.me", fallbackLabel: "我")
        tapWhenReady(me, in: app)
        XCTAssertTrue(
            app.staticTexts["我的数字生活"].waitForExistence(timeout: 10),
            "Accessibility Large 下 Me 内容未出现"
        )
        attachScreenshot(app, name: "vnext-dynamic-type-accessibility-large-me")

        let infrastructure = vnextRoot(
            app,
            id: "pdig.nav.infrastructure",
            fallbackLabel: "基础设施"
        )
        tapWhenReady(infrastructure, in: app)
        XCTAssertTrue(
            app.staticTexts["我的基础设施"].waitForExistence(timeout: 10),
            "Accessibility Large 下基础设施不可达"
        )

        tapWhenReady(vnextRoute(app, id: "pdig.nav.infra.accounts"), in: app)
        let account = vnextRoute(
            app,
            id: "pdig.ios.reference.secondary.item.accounts.acct-wechat"
        )
        tapWhenReady(account, in: app)
        XCTAssertTrue(
            app.staticTexts["账户详情"].waitForExistence(timeout: 10),
            "Accessibility Large 下账户详情不可达"
        )
        attachScreenshot(app, name: "vnext-dynamic-type-accessibility-large-account-detail")
    }

    func testVNextGlobalPrivacyMasksCardAndNumberSurfaces() throws {
        let app = launchVNextReference()

        let me = vnextRoot(app, id: "pdig.nav.me", fallbackLabel: "我")
        tapWhenReady(me, in: app)
        XCTAssertTrue(app.switches.firstMatch.waitForExistence(timeout: 10), "隐私遮蔽开关未出现")
        tapWhenReady(app.switches.firstMatch, in: app)

        // Global Search keeps raw matching capability while redacting result copy.
        let searchOpen = vnextRoute(app, id: "pdig.search.open")
        tapWhenReady(searchOpen, in: app)
        let searchField = vnextRoute(app, id: "pdig.search.field")
        XCTAssertTrue(searchField.waitForExistence(timeout: 10), "全局搜索输入框未出现")
        searchField.tap()
        searchField.typeText("HSBC")
        XCTAssertTrue(
            app.staticTexts["支付工具（已遮蔽）"].firstMatch.waitForExistence(timeout: 10),
            "隐私状态下用原始字段搜索未命中已记录卡片"
        )
        XCTAssertFalse(app.staticTexts["HSBC 汇丰"].exists, "Search 泄露卡片发行方")
        XCTAssertFalse(app.staticTexts["HSBC 网银"].exists, "Search 泄露账户或服务名称")
        attachScreenshot(app, name: "vnext-privacy-search")
        tapWhenReady(vnextRoute(app, id: "pdig.search.close"), in: app)

        let infrastructure = vnextRoot(
            app,
            id: "pdig.nav.infrastructure",
            fallbackLabel: "基础设施"
        )
        tapWhenReady(infrastructure, in: app)
        XCTAssertTrue(app.staticTexts["我的基础设施"].waitForExistence(timeout: 10))

        tapWhenReady(vnextRoute(app, id: "pdig.nav.infra.cards"), in: app)
        XCTAssertTrue(
            app.staticTexts["支付工具（已遮蔽）"].firstMatch.waitForExistence(timeout: 10),
            "Cards 未遮蔽卡片身份"
        )
        XCTAssertFalse(app.staticTexts["招行储蓄卡"].exists, "Cards 泄露卡片昵称")
        XCTAssertFalse(app.staticTexts["招商银行"].exists, "Cards 泄露发行方")
        attachScreenshot(app, name: "vnext-privacy-cards")

        let now = vnextRoot(app, id: "pdig.nav.now", fallbackLabel: "现在")
        tapWhenReady(now, in: app)
        tapWhenReady(infrastructure, in: app)
        XCTAssertTrue(app.staticTexts["我的基础设施"].waitForExistence(timeout: 10))

        tapWhenReady(vnextRoute(app, id: "pdig.nav.infra.numbers"), in: app)
        XCTAssertTrue(
            app.staticTexts["手机号身份（已遮蔽）"].firstMatch.waitForExistence(timeout: 10),
            "Numbers 未遮蔽号码身份"
        )
        XCTAssertFalse(app.staticTexts["主号 中国移动"].exists, "Numbers 泄露号码昵称")
        XCTAssertFalse(app.staticTexts["+86 138****8823"].exists, "Numbers 泄露号码")
        attachScreenshot(app, name: "vnext-privacy-numbers")
    }

    func testVNextSecondaryCollectionsHonorRegionScope() throws {
        let app = launchVNextReference()

        let infrastructure = vnextRoot(
            app,
            id: "pdig.nav.infrastructure",
            fallbackLabel: "基础设施"
        )
        tapWhenReady(infrastructure, in: app)
        XCTAssertTrue(
            app.staticTexts["我的基础设施"].waitForExistence(timeout: 10),
            "基础设施总览未出现"
        )

        let chinaRegion = vnextRoute(app, id: "pdig.region.item.CN")
        tapWhenReady(chinaRegion, in: app)

        tapWhenReady(vnextRoute(app, id: "pdig.nav.infra.accounts"), in: app)
        XCTAssertTrue(
            app.staticTexts["中国大陆 · 已记录的数字基础设施对象 · 参考数据"]
                .waitForExistence(timeout: 10),
            "账户页没有继承地区筛选"
        )
        XCTAssertTrue(
            vnextRoute(app, id: "pdig.ios.reference.secondary.item.accounts.acct-wechat")
                .waitForExistence(timeout: 10),
            "中国地区账户未出现"
        )
        XCTAssertFalse(
            vnextRoute(app, id: "pdig.ios.reference.secondary.item.accounts.acct-hsbc").exists,
            "地区筛选错误泄露了香港账户"
        )

        tapWhenReady(
            vnextRoute(app, id: "pdig.ios.reference.secondary.scope.global.accounts"),
            in: app
        )
        XCTAssertTrue(
            vnextRoute(app, id: "pdig.ios.reference.secondary.item.accounts.acct-hsbc")
                .waitForExistence(timeout: 10),
            "查看全球后香港账户没有恢复"
        )
        attachScreenshot(app, name: "vnext-secondary-region-scope")
    }

    func testVNextReferenceFiveRootsAndSecondaryInfrastructure() throws {
        let app = launchVNextReference()

        let now = vnextRoot(app, id: "pdig.nav.now", fallbackLabel: "现在")
        XCTAssertTrue(now.waitForExistence(timeout: 20), "vNext 现在入口未出现")
        attachScreenshot(app, name: "vnext-01-now")

        let infrastructure = vnextRoot(
            app,
            id: "pdig.nav.infrastructure",
            fallbackLabel: "基础设施"
        )
        tapWhenReady(infrastructure, in: app)
        XCTAssertTrue(
            app.staticTexts["我的基础设施"].waitForExistence(timeout: 10),
            "vNext 基础设施未出现"
        )
        attachScreenshot(app, name: "vnext-02-infrastructure")

        // Account must be reachable through normal product navigation, not by
        // directly constructing a view in the test.
        let accountEntry = vnextRoute(app, id: "pdig.nav.infra.accounts")
        tapWhenReady(accountEntry, in: app)
        XCTAssertTrue(
            app.staticTexts["全球 · 已记录的数字基础设施对象 · 参考数据"]
                .waitForExistence(timeout: 10),
            "账户 collection 未出现"
        )
        let accountGrid = vnextRoute(
            app,
            id: "pdig.ios.reference.secondary.grid.accounts"
        )
        XCTAssertTrue(accountGrid.waitForExistence(timeout: 10), "账户 collection 缺少稳定 grid 语义")
        let expectedColumns = UIDevice.current.userInterfaceIdiom == .pad ? "2 列" : "1 列"
        XCTAssertEqual(
            accountGrid.value as? String,
            expectedColumns,
            "二级资产 collection 未按 iPhone/iPad 宽度翻译布局"
        )
        XCTAssertTrue(
            vnextRoute(app, id: "pdig.ios.reference.secondary.item.accounts.acct-wechat").exists,
            "账户 collection 缺少稳定对象入口"
        )
        attachScreenshot(app, name: "vnext-03-accounts")

        tapWhenReady(
            vnextRoute(app, id: "pdig.ios.reference.secondary.item.account.acct-wechat"),
            in: app
        )
        XCTAssertTrue(
            app.staticTexts["账户详情"].waitForExistence(timeout: 10),
            "账户详情未出现"
        )
        attachScreenshot(app, name: "vnext-04-account-detail")

        let change = vnextRoot(app, id: "pdig.nav.change", fallbackLabel: "变更")
        tapWhenReady(change, in: app)
        XCTAssertTrue(
            vnextRoute(app, id: "pdig.change.center").waitForExistence(timeout: 10),
            "变更中心未出现"
        )
        XCTAssertTrue(
            vnextRoute(app, id: "pdig.change.center.phone").exists,
            "变更中心缺少换号入口"
        )
        XCTAssertTrue(
            vnextRoute(app, id: "pdig.change.center.card").exists,
            "变更中心缺少换卡入口"
        )
        attachScreenshot(app, name: "vnext-05-change-center")

        tapWhenReady(vnextRoute(app, id: "pdig.change.center.card"), in: app)
        XCTAssertTrue(
            vnextRoute(app, id: "pdig.change.card").waitForExistence(timeout: 10),
            "卡片变更 Reference 未出现"
        )
        let cardAfterStage = vnextRoute(app, id: "pdig.change.card.stage.after")
        XCTAssertTrue(
            cardAfterStage.waitForExistence(timeout: 10),
            "换卡缺少可访问的 After 计划阶段"
        )
        attachScreenshot(app, name: "vnext-05a-change-card-current")

        // Assert the stage as an interaction, not as a SwiftUI element type.
        // VFilterChip is exposed as a control, so querying StaticText is brittle
        // even though the visible label is correct.
        tapWhenReady(cardAfterStage, in: app)
        XCTAssertTrue(
            app.staticTexts["完成后（计划）"].waitForExistence(timeout: 10),
            "换卡 After 计划内容未出现"
        )
        XCTAssertTrue(
            app.staticTexts["尚未选择替代卡，因此没有可展示的完成后计划。"]
                .waitForExistence(timeout: 5),
            "未选择替代卡时 After 必须保持计划边界"
        )
        attachScreenshot(app, name: "vnext-05a-change-card-after")

        tapWhenReady(vnextRoute(app, id: "pdig.nav.change"), in: app)
        tapWhenReady(vnextRoute(app, id: "pdig.change.center.phone"), in: app)
        XCTAssertTrue(
            app.staticTexts["更换手机号"].waitForExistence(timeout: 10),
            "换号 focused route 未出现"
        )
        attachScreenshot(app, name: "vnext-05b-change-phone")

        let records = vnextRoot(app, id: "pdig.nav.records", fallbackLabel: "记录")
        tapWhenReady(records, in: app)
        XCTAssertTrue(
            app.staticTexts["已发生、已记录完成与独立验证分开显示"]
                .waitForExistence(timeout: 10),
            "记录一级工作区未出现"
        )
        attachScreenshot(app, name: "vnext-06-records")

        let me = vnextRoot(app, id: "pdig.nav.me", fallbackLabel: "我")
        tapWhenReady(me, in: app)
        XCTAssertTrue(
            app.staticTexts["我的数字生活"].waitForExistence(timeout: 10),
            "我一级工作区未出现"
        )
        XCTAssertTrue(app.switches.firstMatch.exists, "隐私遮蔽开关未出现")
        attachScreenshot(app, name: "vnext-07-me")

        // Sources and Settings are real reference surfaces, not placeholders.
        let sources = vnextRoute(app, id: "pdig.nav.me.sources")
        tapWhenReady(sources, in: app)
        XCTAssertTrue(
            app.staticTexts["Reference fixture 未记录真实来源实例"].waitForExistence(timeout: 10),
            "数据源 reference surface 未出现"
        )
        attachScreenshot(app, name: "vnext-08-sources")

        tapWhenReady(vnextRoot(app, id: "pdig.nav.me", fallbackLabel: "我"), in: app)
        let settings = vnextRoute(app, id: "pdig.nav.me.settings")
        tapWhenReady(settings, in: app)
        XCTAssertTrue(
            app.staticTexts["隐私与偏好"].waitForExistence(timeout: 10),
            "设置 reference surface 未出现"
        )
        let upcoming = vnextRoute(app, id: "pdig.ios.reference.setting.upcoming")
        tapWhenReady(upcoming, in: app)
        tapWhenReady(vnextRoot(app, id: "pdig.nav.now", fallbackLabel: "现在"), in: app)
        XCTAssertFalse(
            vnextRoute(app, id: "pdig.now.upcoming").exists,
            "关闭“显示即将到来”后 Now 仍渲染 Upcoming"
        )
        attachScreenshot(app, name: "vnext-09-now-upcoming-hidden")

        tapWhenReady(vnextRoot(app, id: "pdig.nav.me", fallbackLabel: "我"), in: app)

        // Global privacy is a cross-workspace presentation contract, not a
        // card/number-only switch. Keep state/counts visible while identity-
        // bearing plan/action and old/new number copy is redacted.
        tapWhenReady(app.switches.firstMatch, in: app)

        let maskedRecords = vnextRoot(app, id: "pdig.nav.records", fallbackLabel: "记录")
        tapWhenReady(maskedRecords, in: app)
        XCTAssertTrue(
            app.staticTexts["操作步骤（已遮蔽）"].firstMatch.waitForExistence(timeout: 10),
            "Records 未应用全局隐私遮蔽"
        )
        XCTAssertFalse(app.staticTexts["影响分析"].exists, "Records 泄露未遮蔽动作标题")
        attachScreenshot(app, name: "vnext-10-records-masked")

        let maskedChange = vnextRoot(app, id: "pdig.nav.change", fallbackLabel: "变更")
        tapWhenReady(maskedChange, in: app)
        XCTAssertTrue(
            vnextRoute(app, id: "pdig.change.center").waitForExistence(timeout: 10),
            "隐私状态下 Change Center 未出现"
        )
        XCTAssertFalse(app.staticTexts["工行信用卡"].exists, "Change Center 泄露卡片身份")
        tapWhenReady(vnextRoute(app, id: "pdig.change.center.phone"), in: app)
        XCTAssertTrue(
            app.staticTexts["号码已遮蔽"].firstMatch.waitForExistence(timeout: 10),
            "Change Phone 未应用全局隐私遮蔽"
        )
        XCTAssertFalse(app.staticTexts["+86 138****8823"].exists, "Change Phone 泄露旧号码")
        attachScreenshot(app, name: "vnext-11-change-masked")
    }

}
