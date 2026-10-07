import XCTest
@testable import PDIGApp

final class VNextModelTests: XCTestCase {

    @MainActor
    func testInfrastructureDetailStudioBackContinuity() {
        let model = VNextModel(preferencesStore: nil)

        model.selectPrimary(.infrastructure)
        XCTAssertEqual(model.screen, .infrastructure)
        XCTAssertEqual(model.primary, .infrastructure)

        model.navigate(.cards)
        model.openCard("card-cn-1")
        XCTAssertEqual(model.screen, .cardDetail("card-cn-1"))

        model.openCardCustomization("card-cn-1")
        XCTAssertEqual(model.screen, .cardCustomization("card-cn-1"))
        XCTAssertTrue(model.canGoBack)

        model.back()
        XCTAssertEqual(model.screen, .cardDetail("card-cn-1"))
        XCTAssertEqual(model.primary, .infrastructure)

        model.back()
        XCTAssertEqual(model.screen, .cards)
        XCTAssertEqual(model.primary, .infrastructure)
    }

    @MainActor
    func testCrossTabChangeFlowReturnsToInfrastructureContext() {
        let model = VNextModel(preferencesStore: nil)
        model.selectPrimary(.infrastructure)
        model.navigate(.numbers)
        model.openNumber("num-cn-1")

        model.navigate(.changePhone)
        XCTAssertEqual(model.primary, .change)
        XCTAssertTrue(model.canGoBack)

        model.back()
        XCTAssertEqual(model.screen, .numberDetail("num-cn-1"))
        XCTAssertEqual(model.primary, .infrastructure)
    }

    @MainActor
    func testWorkspacePreferencesPersistLocally() {
        let suite = "pdig-ui-vnext-tests-\(UUID().uuidString)"
        guard let defaults = UserDefaults(suiteName: suite) else {
            XCTFail("Unable to create isolated UserDefaults suite")
            return
        }
        defaults.removePersistentDomain(forName: suite)
        defer { defaults.removePersistentDomain(forName: suite) }

        let store = VNextPreferencesStore(defaults: defaults)
        let first = VNextModel(preferencesStore: store)
        first.privacyMask = false
        first.reduceMotion = true
        first.showUpcoming = false

        let second = VNextModel(preferencesStore: store)
        XCTAssertFalse(second.privacyMask)
        XCTAssertTrue(second.reduceMotion)
        XCTAssertFalse(second.showUpcoming)
    }

    @MainActor
    func testPresentationProfilePersistsWithoutTouchingDomainData() {
        let suite = "pdig-ui-vnext-profile-tests-\(UUID().uuidString)"
        guard let defaults = UserDefaults(suiteName: suite) else {
            XCTFail("Unable to create isolated UserDefaults suite")
            return
        }
        defaults.removePersistentDomain(forName: suite)
        defer { defaults.removePersistentDomain(forName: suite) }

        let store = VNextPreferencesStore(defaults: defaults)
        let first = VNextModel(preferencesStore: store)
        let profile = VPresentationProfile.defaultFor(
            targetType: "card",
            targetId: "card-cn-1",
            preset: "minimal"
        ).replacingTheme("metal")
        first.savePresentationProfile(profile)

        let second = VNextModel(preferencesStore: store)
        let loaded = second.presentationProfile(
            targetType: "card",
            targetId: "card-cn-1",
            fallbackPreset: "minimal"
        )
        XCTAssertEqual(loaded.themeId, "metal")
        XCTAssertEqual(loaded.targetId, "card-cn-1")
    }

    func testBundledEarthMaterialLoadsFromSwiftPMResources() {
        XCTAssertNotNil(VEarthMaterialAssets.shared)
    }

    func testEarthCacheKeyIsDeterministic() {
        let camera = VGlobeCamera(yawDeg: 12.2, pitchDeg: 29.7, zoom: 1.03)
        XCTAssertEqual(
            VEarthTextureRenderer.cacheKey(camera: camera, rect: 320),
            VEarthTextureRenderer.cacheKey(camera: camera, rect: 320)
        )
    }
}
