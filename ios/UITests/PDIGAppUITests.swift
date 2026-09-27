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
        XCTAssertTrue(element.waitForExistence(timeout: 15), "等待元素出现: \(element)", file: file, line: line)
        if element.exists && !element.isEnabled {
            let predicate = NSPredicate(format: "isEnabled == true")
            wait(for: [expectation(for: predicate, evaluatedWith: element)], timeout: 10)
        }
        var attempts = 0
        while !element.isHittable && attempts < 6 {
            app.swipeUp()
            attempts += 1
        }
        element.tap()
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
        tapWhenReady(app.buttons["更换手机号"], in: app)
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
}
