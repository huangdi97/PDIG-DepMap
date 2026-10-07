// VNextModels —— PDIG UI vNext 演示层表现模型（Presentation Layer，全 synthetic）。
//
// 铁律（spec/ui-vnext/PRESENTATION_PROFILE_SCHEMA.json、DESIGN_TOKENS.json policy）：
//  - 这些模型只驱动表现；绝不改变 node identity / dependencies / evidence / confirmation；
//  - 绝不写入 .depmap frozen payload；存储只走本地 preferences（本演示层为内存态）；
//  - region 是表现层派生属性（来自 synthetic fixture），不改变 canonical schema。

import SwiftUI

/// 一级/二级/基础设施二级页面（IA.md §1–§3；Screen Registry §4）。
enum VScreen: Hashable {
    case now
    case infrastructure
    case change
    case records
    case sources
    case settings
    case search
    case overview
    case cards
    case numbers
    case accounts
    case emails
    case devices
    case services
    case weaknesses
    case cardDetail(String)
    case numberDetail(String)
    case cardCustomization(String)
    case numberCustomization(String)
    case personalization
    case changePhone

    /// 底部导航/一级导航归属（再点当前项回到页面语义，INTERACTION_CONTRACT §6）。
    var primaryTab: VScreen {
        switch self {
        case .now: return .now
        case .infrastructure, .overview, .cards, .cardDetail, .numbers, .numberDetail,
             .cardCustomization, .numberCustomization: return .infrastructure
        case .change, .changePhone: return .change
        case .records: return .records
        default: return .settings // sources / settings / personalization 归属设置入口
        }
    }

    var route: String {
        switch self {
        case .now: return "now"
        case .infrastructure, .overview: return "infrastructure"
        case .cards: return "infrastructure/cards"
        case .cardDetail: return "infrastructure/cards/{id}"
        case .cardCustomization: return "infrastructure/cards/{id}/customize"
        case .numbers: return "infrastructure/numbers"
        case .numberDetail: return "infrastructure/numbers/{id}"
        case .numberCustomization: return "infrastructure/numbers/{id}/customize"
        case .personalization: return "settings/personalization"
        case .changePhone: return "change/phone"
        case .change: return "change"
        case .records: return "records"
        case .sources: return "sources"
        case .settings: return "settings"
        case .search: return "search"
        case .accounts: return "infrastructure/accounts"
        case .emails: return "infrastructure/emails"
        case .devices: return "infrastructure/devices"
        case .services: return "infrastructure/services"
        case .weaknesses: return "infrastructure/weaknesses"
        }
    }

    /// iPad content-level secondary navigation only. Focused child flows intentionally hide it.
    var isInfrastructureSibling: Bool {
        switch self {
        case .infrastructure, .overview, .cards, .numbers, .accounts, .emails, .devices, .services, .weaknesses:
            return true
        default:
            return false
        }
    }

    /// Used only to highlight the correct infrastructure sibling in an expanded workspace.
    var infrastructureFamily: String? {
        switch self {
        case .infrastructure, .overview: return "overview"
        case .cards, .cardDetail, .cardCustomization: return "cards"
        case .numbers, .numberDetail, .numberCustomization: return "numbers"
        case .accounts: return "accounts"
        case .emails: return "emails"
        case .devices: return "devices"
        case .services: return "services"
        case .weaknesses: return "weaknesses"
        default: return nil
        }
    }
}

enum VChangeProjection: String, CaseIterable, Identifiable {
    case current
    case transition
    case after

    var id: String { rawValue }
    var title: String {
        switch self {
        case .current: return "当前"
        case .transition: return "迁移中"
        case .after: return "完成后（计划）"
        }
    }
}

/// Globe 状态机（INTERACTION_CONTRACT §1）：
/// GLOBAL → REGION_HOVER → REGION_SELECTED → REGION_DETAIL，Escape 逐级回退。
enum VGlobeState { case global, regionHover, regionSelected, regionDetail }

/// 地区呈现模型（IA.md §5；值只由 synthetic fixture 计算；无数据 = 不显示/0，禁止伪造）。
struct RegionPresentation: Identifiable, Hashable {
    let regionCode: String
    let displayName: String
    let latitude: Double
    let longitude: Double
    var cardCount: Int = 0
    var phoneCount: Int = 0
    var accountCount: Int = 0
    var serviceCount: Int = 0
    var attentionCount: Int = 0

    var id: String { regionCode }
}

/// 卡片（UI 展示模型；缺失 = 未设置/未知，禁止推断）。
struct VCard: Identifiable, Hashable {
    let id: String
    let nickname: String
    let issuer: String
    let last4: String
    let masked: String
    let region: String
    let currency: String
    let type: String            // debit | credit
    let form: String            // physical | virtual
    let network: String
    let expiry: String          // YYYY-MM
    let status: String          // active | expiring_soon | ...
    let attention: Bool
    let usages: [String]
    let preset: String          // themeId
}

/// 号码（UI 展示模型）。
struct VNumber: Identifiable, Hashable {
    let id: String
    let nickname: String
    let maskedNumber: String
    let region: String
    let countryCode: String
    let carrier: String
    let simKind: String         // SIM | eSIM
    let role: String            // primary | secondary | keep
    let usages: [String]
    let status: String
    let recoveryOnly: Bool
    let attention: Bool
    let preset: String
}

struct VService: Identifiable, Hashable {
    let id: String
    let name: String
    let region: String
    let kind: String
}

struct VAccount: Identifiable, Hashable {
    let id: String
    let name: String
    let provider: String
    let maskedIdentifier: String
    let region: String
    let roles: [String]
    let authMethods: [String]
    let recoveryRoute: String
    let status: String
    let attention: Bool
}

struct VEmail: Identifiable, Hashable {
    let id: String
    let name: String
    let maskedAddress: String
    let provider: String
    let region: String
    let roles: [String]
    let linkedServiceCount: Int
    let recoveryOnly: Bool
    let status: String
}

struct VDevice: Identifiable, Hashable {
    let id: String
    let name: String
    let platform: String
    let kind: String
    let region: String
    let roles: [String]
    let trust: String
    let lastSeen: String
    let attention: Bool
}

struct VRelation: Hashable {
    let from: String
    let to: String
    let kind: String            // funding | authenticates | twoFA
}

struct VAttentionItem: Identifiable, Hashable {
    let id: String
    let severity: String        // critical | warning
    let title: String
    let target: String
}

struct VActiveChange: Identifiable, Hashable {
    let id: String
    let title: String
    let phase: String
    let target: String
}

struct VUpcomingItem: Identifiable, Hashable {
    let id: String
    let title: String
    let days: Int
}

struct VChangeStage: Identifiable, Hashable {
    let stage: Int
    let key: String
    let status: String          // completed | verifying | not_started | blocked | upcoming
    let blockReason: String?

    var id: Int { stage }
}

struct VMigration: Hashable {
    let service: String
    let status: String          // migrated | waiting | not_started | blocked
}

/// PresentationProfile（本地偏好；绝不进 .depmap；spec PRESENTATION_PROFILE_SCHEMA.json）。
struct VPresentationProfile: Hashable, Codable {
    let targetType: String
    let targetId: String
    let themeId: String
    let material: String
    let accentColor: String
    let backgroundKind: String
    let backgroundValue: String
    let layout: String
    let maskSensitive: Bool

    static func defaultFor(targetType: String, targetId: String, preset: String) -> VPresentationProfile {
        VPresentationProfile(
            targetType: targetType,
            targetId: targetId,
            themeId: preset,
            material: "default",
            accentColor: "default",
            backgroundKind: "preset",
            backgroundValue: preset,
            layout: "standard",
            maskSensitive: false
        )
    }

    func replacingTheme(_ themeId: String) -> VPresentationProfile {
        VPresentationProfile(
            targetType: targetType,
            targetId: targetId,
            themeId: themeId,
            material: material,
            accentColor: accentColor,
            backgroundKind: backgroundKind,
            backgroundValue: themeId,
            layout: layout,
            maskSensitive: maskSensitive
        )
    }

    func replacingMaterial(_ material: String) -> VPresentationProfile {
        VPresentationProfile(
            targetType: targetType, targetId: targetId, themeId: themeId,
            material: material, accentColor: accentColor,
            backgroundKind: backgroundKind, backgroundValue: backgroundValue,
            layout: layout, maskSensitive: maskSensitive
        )
    }

    func replacingAccent(_ accentColor: String) -> VPresentationProfile {
        VPresentationProfile(
            targetType: targetType, targetId: targetId, themeId: themeId,
            material: material, accentColor: accentColor,
            backgroundKind: backgroundKind, backgroundValue: backgroundValue,
            layout: layout, maskSensitive: maskSensitive
        )
    }

    func replacingLayout(_ layout: String) -> VPresentationProfile {
        VPresentationProfile(
            targetType: targetType, targetId: targetId, themeId: themeId,
            material: material, accentColor: accentColor,
            backgroundKind: backgroundKind, backgroundValue: backgroundValue,
            layout: layout, maskSensitive: maskSensitive
        )
    }

    func replacingMask(_ maskSensitive: Bool) -> VPresentationProfile {
        VPresentationProfile(
            targetType: targetType, targetId: targetId, themeId: themeId,
            material: material, accentColor: accentColor,
            backgroundKind: backgroundKind, backgroundValue: backgroundValue,
            layout: layout, maskSensitive: maskSensitive
        )
    }
}

/// 主题 preset 列表（spec asset-card / phone-card 契约；preset visual ≠ semantic role）。
let CARD_THEME_PRESETS = ["minimal", "deep-space", "region", "city", "glass", "metal", "abstract"]
let NUMBER_THEME_PRESETS = ["country", "city", "minimal", "banking", "travel", "recovery", "work", "private"]

/// 稳定 testId 常量（DESIGN_TOKENS.json testIds.prefixes；四端同一语义 id）。
enum VTestIds {
    static let navNow = "pdig.nav.now"
    static let navInfrastructure = "pdig.nav.infrastructure"
    static let navChange = "pdig.nav.change"
    static let navRecords = "pdig.nav.records"
    static let navSources = "pdig.nav.sources"
    static let navSettings = "pdig.nav.settings"
    static let navInfraOverview = "pdig.nav.infra.overview"
    static let navInfraCards = "pdig.nav.infra.cards"
    static let navInfraNumbers = "pdig.nav.infra.numbers"
    static let navInfraAccounts = "pdig.nav.infra.accounts"
    static let navInfraEmails = "pdig.nav.infra.emails"
    static let navInfraDevices = "pdig.nav.infra.devices"
    static let navInfraServices = "pdig.nav.infra.services"
    static let navInfraWeaknesses = "pdig.nav.infra.weaknesses"
    static let globeStage = "pdig.globe.stage"
    static let globeCanvas = "pdig.globe.canvas"
    static let globeRegion = "pdig.globe.region"
    static let regionList = "pdig.region.list"
    static let regionItem = "pdig.region.item"
    static let nowGlobe = "pdig.now.globe"
    static let nowAttention = "pdig.now.attention"
    static let nowAttentionItem = "pdig.now.attention.item"
    static let nowChanges = "pdig.now.changes"
    static let nowUpcoming = "pdig.now.upcoming"
    static let overviewActivity = "pdig.overview.activity"
    static let overviewQuick = "pdig.overview.quick"
    static let cardGrid = "pdig.card.grid"
    static let cardList = "pdig.card.list"
    static let cardViewToggle = "pdig.card.viewToggle"
    static let cardCard = "pdig.card.card"
    static let cardDetailIdentity = "pdig.card.detail.identity"
    static let cardDetailInfo = "pdig.card.detail.info"
    static let phoneList = "pdig.phone.list"
    static let phoneInspector = "pdig.phone.inspector"
    static let phoneRow = "pdig.phone.row"
    static let phoneDetailIdentity = "pdig.phone.detail.identity"
    static let changePhoneRail = "pdig.change.phone.rail"
    static let changePhoneGate = "pdig.change.phone.gate"
    static let changePhoneItem = "pdig.change.phone.item"
    static let customizationLibrary = "pdig.customization.library"
    static let customizationPreview = "pdig.customization.preview"
    static let customizationInspector = "pdig.customization.inspector"
    static let settingsPersonalization = "pdig.settings.personalization"
    static let search = "pdig.search"
    static let searchField = "pdig.search.field"
    static let records = "pdig.records"
    static let sources = "pdig.sources"
    static let infraAccountList = "pdig.account.list"
    static let infraEmailList = "pdig.email.list"
    static let infraDeviceList = "pdig.device.list"
    static let infraServiceList = "pdig.service.list"
    static let infraWeaknessList = "pdig.weakness.list"

    static func changePhoneStage(_ n: Int) -> String { "pdig.change.phone.stage\(n)" }
}

/// 状态 → 中文标签（desktop PdigV2Theme.statusLabelZh 移植）。
func vStatusLabel(_ status: String) -> String {
    switch status {
    case "active": return VCopy.statusActive
    case "expiring_soon": return VCopy.filterExpiring
    case "expired": return "已过期"
    case "blocked": return "阻止"
    case "critical": return "必须处理" // impact.mustChange
    case "warning": return "需要确认" // impact.needsReview
    case "verifying": return VCopy.verifying
    case "completed": return VCopy.completed
    case "not_started": return VCopy.notStarted
    case "waiting": return VCopy.waiting
    case "migrated": return VCopy.migrated
    case "plan": return VCopy.plan
    case "upcoming": return VCopy.upcomingStage
    default: return VCopy.unknown
    }
}
