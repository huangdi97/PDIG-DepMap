// PDIGUIVNextUITests —— simulator evidence for the iOS/iPadOS vNext translation.
//
// This suite launches a synthetic presentation-only fixture with --uitest-vnext.
// It validates the current design branch, not the production Canonical persistence path.

import XCTest

final class PDIGUIVNextUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    private func launch() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments = ["--uitest-vnext"]
        app.launch()
        return app
    }

    private func shot(_ app: XCUIApplication, _ name: String) {
        let item = XCTAttachment(screenshot: app.screenshot())
        item.name = name
        item.lifetime = .keepAlways
        add(item)
    }

    private func tapPrimary(_ title: String, app: XCUIApplication) {
        let tab = app.tabBars.buttons[title]
        if tab.waitForExistence(timeout: 2) {
            tab.tap()
            return
        }
        let button = app.buttons[title]
        if button.waitForExistence(timeout: 2) {
            button.tap()
            return
        }
        let text = app.staticTexts[title]
        XCTAssertTrue(text.waitForExistence(timeout: 8), "一级入口不可达: \(title)")
        text.tap()
    }

    func testVNextPrimaryNavigationAndFlagshipEvidence() throws {
        let app = launch()
        XCTAssertTrue(app.staticTexts["你好"].waitForExistence(timeout: 20), "vNext Now 未出现")
        shot(app, "vnext-01-now")

        tapPrimary("基础设施", app: app)
        let hubOrOverview = app.staticTexts["基础设施"].waitForExistence(timeout: 5)
            || app.staticTexts["我的基础设施"].waitForExistence(timeout: 5)
        XCTAssertTrue(hubOrOverview, "基础设施入口未出现")
        shot(app, "vnext-02-infrastructure")

        tapPrimary("变更", app: app)
        XCTAssertTrue(app.staticTexts["更换手机号"].waitForExistence(timeout: 10), "Change Phone 未出现")
        XCTAssertTrue(app.segmentedControls.firstMatch.waitForExistence(timeout: 5), "Current/Transition/After 控件缺失")
        shot(app, "vnext-03-change-phone")

        tapPrimary("记录", app: app)
        XCTAssertTrue(app.staticTexts["记录"].waitForExistence(timeout: 10), "Records 未出现")
        shot(app, "vnext-04-records")
    }

    func testVNextUtilitiesAreReachable() throws {
        let app = launch()
        XCTAssertTrue(app.staticTexts["你好"].waitForExistence(timeout: 20))

        let search = app.buttons["搜索"]
        XCTAssertTrue(search.waitForExistence(timeout: 10), "搜索入口不可达")
        search.tap()
        XCTAssertTrue(app.staticTexts["搜索与快捷操作"].waitForExistence(timeout: 10), "Search 未出现")
        shot(app, "vnext-05-search")
    }
}
