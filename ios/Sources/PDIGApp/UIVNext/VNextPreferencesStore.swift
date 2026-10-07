// VNextPreferencesStore —— presentation-only local preferences.
//
// This store contains no PersonalReality / Canonical / .depmap facts. It is deliberately limited to
// workspace appearance preferences and PresentationProfile objects.

import Foundation

struct VWorkspacePreferences: Codable, Equatable {
    var privacyMask: Bool = true
    var reduceMotion: Bool = false
    var showUpcoming: Bool = true
}

final class VNextPreferencesStore {
    static let live = VNextPreferencesStore(defaults: .standard)

    private let defaults: UserDefaults
    private let workspaceKey = "pdig.uiVNext.workspace.v1"
    private let profilesKey = "pdig.uiVNext.presentationProfiles.v1"

    init(defaults: UserDefaults) {
        self.defaults = defaults
    }

    func loadWorkspace() -> VWorkspacePreferences {
        guard let data = defaults.data(forKey: workspaceKey),
              let value = try? JSONDecoder().decode(VWorkspacePreferences.self, from: data) else {
            return VWorkspacePreferences()
        }
        return value
    }

    func saveWorkspace(_ value: VWorkspacePreferences) {
        guard let data = try? JSONEncoder().encode(value) else { return }
        defaults.set(data, forKey: workspaceKey)
    }

    func loadProfiles() -> [String: VPresentationProfile] {
        guard let data = defaults.data(forKey: profilesKey),
              let value = try? JSONDecoder().decode([String: VPresentationProfile].self, from: data) else {
            return [:]
        }
        return value
    }

    func saveProfiles(_ value: [String: VPresentationProfile]) {
        guard let data = try? JSONEncoder().encode(value) else { return }
        defaults.set(data, forKey: profilesKey)
    }

    static func profileKey(targetType: String, targetId: String) -> String {
        targetType + "::" + targetId
    }
}
