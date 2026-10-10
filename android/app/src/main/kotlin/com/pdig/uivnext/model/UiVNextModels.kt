package com.pdig.uivnext.model

/**
 * PDIG UI vNext — 纯 UI 层表现模型（Presentation Layer）。
 *
 * 铁律（spec §29-§31）：
 *  - 这些模型只驱动表现；绝不改变 node identity / dependencies / evidence / confirmation；
 *  - 不写入 .depmap frozen payload；存储只走本地 preferences；
 *  - region 是表现层派生属性（来自 synthetic fixture），不改变 canonical schema。
 */

/**
 * Android adaptive window-width classes.
 * Compact < 600dp; Medium 600–839dp; Expanded >= 840dp.
 * Keep navigation and content on the same thresholds so 600–699dp never mixes rail navigation with compact content.
 */
enum class MediaBreakpoint { COMPACT, MEDIUM, EXPANDED }

/** 页面（一级导航 + 基础设施二级 + 变更 + 设置）。 */
enum class VScreen(val route: String, val titleZh: String, val section: VSection) {
    NOW("now", "现在", VSection.PRIMARY),
    INFRASTRUCTURE("infrastructure", "基础设施", VSection.PRIMARY),
    CHANGE("change", "变更", VSection.PRIMARY),
    RECORDS("records", "记录", VSection.PRIMARY),
    ME("me", "我", VSection.PRIMARY),
    REVIEW("review", "待复核", VSection.SECONDARY),
    IMPORT("import", "建立基础设施", VSection.SECONDARY),
    MANUAL_ADD("manual-add", "手工记录", VSection.SECONDARY),
    MANUAL_RELATION("manual-relation", "手工记录关系", VSection.SECONDARY),
    SOURCES("sources", "数据源", VSection.SECONDARY),
    SETTINGS("settings", "设置", VSection.SECONDARY),
    OVERVIEW("overview", "总览", VSection.INFRA),
    CARDS("cards", "卡片", VSection.INFRA),
    NUMBERS("numbers", "号码", VSection.INFRA),
    ACCOUNTS("accounts", "账户", VSection.INFRA),
    EMAILS("emails", "邮箱", VSection.INFRA),
    DEVICES("devices", "设备", VSection.INFRA),
    SERVICES("services", "服务", VSection.INFRA),
    WEAKNESSES("weaknesses", "薄弱点", VSection.INFRA),
    CARD_DETAIL("card-detail", "卡片详情", VSection.INFRA),
    NUMBER_DETAIL("number-detail", "号码详情", VSection.INFRA),
    ACCOUNT_DETAIL("account-detail", "账户详情", VSection.INFRA),
    EMAIL_DETAIL("email-detail", "邮箱详情", VSection.INFRA),
    DEVICE_DETAIL("device-detail", "设备详情", VSection.INFRA),
    SERVICE_DETAIL("service-detail", "服务详情", VSection.INFRA),
    CARD_CUSTOMIZATION("card-customization", "卡面定制", VSection.INFRA),
    NUMBER_CUSTOMIZATION("number-customization", "号码面定制", VSection.INFRA),
    PERSONALIZATION("personalization", "个性化", VSection.SETTINGS),
    CHANGE_PHONE("change-phone", "更换手机号", VSection.CHANGE),
    CHANGE_CARD("change-card", "更换银行卡", VSection.CHANGE),
    SEARCH("search", "搜索与快捷操作", VSection.SECONDARY),
}

enum class VSection(val labelZh: String) {
    PRIMARY("主要"),
    INFRA("基础设施"),
    CHANGE("变更"),
    SETTINGS("设置"),
    SECONDARY("系统"),
}

/** 全局状态（Navigation + Globe 的 UI 状态机）。 */
data class VGlobalUiState(
    val screen: VScreen = VScreen.NOW,
    val regionFilter: String? = null,
    val globeState: VGlobeState = VGlobeState.GLOBAL,
    val privacyMask: Boolean = false,
    val reduceMotion: Boolean = false,
    val showRegionList: Boolean = false,
)

enum class VGlobeState { GLOBAL, REGION_HOVER, REGION_SELECTED, REGION_DETAIL }

/** 地区呈现模型（值由真实数据计算；无数据 = 不显示/0，禁止伪造）。 */
data class RegionPresentation(
    val regionCode: String,
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val cardCount: Int = 0,
    val phoneCount: Int = 0,
    val accountCount: Int = 0,
    val serviceCount: Int = 0,
    val attentionCount: Int = 0,
)

/** 卡片（UI 展示模型；数据缺失 = 未设置/未知，禁止推断）。 */
data class UiVNextCard(
    val id: String,
    val nickname: String,
    val issuer: String,
    val last4: String,
    val masked: String,
    val region: String,
    val currency: String,
    val type: String,           // debit | credit
    val form: String,           // physical | virtual
    val network: String,        // 银联 / Visa / Mastercard …
    val expiry: String,         // YYYY-MM
    val status: String,         // active | expiring_soon | ...
    val attention: Boolean,
    val usages: List<String>,
    val preset: String,         // themeId
)

/** 号码（UI 展示模型）。 */
data class UiVNextNumber(
    val id: String,
    val nickname: String,
    val maskedNumber: String,
    val region: String,
    val countryCode: String,
    val carrier: String,
    val simKind: String,        // SIM | eSIM
    val role: String,           // primary | secondary | keep
    val usages: List<String>,
    val status: String,
    val recoveryOnly: Boolean,
    val attention: Boolean,
    val preset: String = "country", // 呈现层默认主题（PresentationProfile 无关语义）
    val uniqueRecoveryPath: Boolean? = null, // null = 未知；不得由 recoveryOnly 推断
)

/**
 * Lifecycle facts shown by the consumer UI.
 *
 * These are presentation-layer records for the UI vNext synthetic fixture. They are
 * deliberately separate from Canonical dependency truth: a recorded fee/due date is
 * not provider truth, and an absent value must render as "未记录" rather than "无".
 */
data class UiVNextCardLifecycle(
    val annualFee: String? = null,
    val annualFeeDue: String? = null,
    val billingDay: String? = null,
    val paymentDueDay: String? = null,
    val installmentSummary: String? = null,
    val autoPaySummary: String? = null,
)

data class UiVNextNumberLifecycle(
    val billingMode: String? = null,
    val planCost: String? = null,
    val keepAliveDue: String? = null,
    val keepAliveCycle: String? = null,
    val lastKeepAlive: String? = null,
    val renewalMethod: String? = null,
)

data class UiVNextService(
    val id: String,
    val name: String,
    val region: String,
    val kind: String,
)

data class UiVNextRelation(
    val from: String,
    val to: String,
    val kind: String,           // funding | authenticates | twoFA
)

data class AttentionItem(val id: String, val severity: String, val title: String, val target: String)

data class ActiveChange(val id: String, val title: String, val phase: String, val target: String)

data class UpcomingItem(val id: String, val title: String, val days: Int)

data class ChangeStage(val stage: Int, val key: String, val status: String, val blockReason: String? = null)

data class ChangeMigration(val service: String, val status: String)

/** PresentationProfile（本地偏好；绝不进 .depmap）。 */
data class PresentationProfile(
    val targetType: String,
    val targetId: String,
    val themeId: String,
    val material: String,
    val accentColor: String,
    val backgroundKind: String,
    val backgroundValue: String,
    val layout: String,
    val maskSensitive: Boolean,
) {
    companion object {
        fun defaultFor(targetType: String, targetId: String, preset: String) = PresentationProfile(
            targetType = targetType,
            targetId = targetId,
            themeId = preset,
            material = "default",
            accentColor = "default",
            backgroundKind = "preset",
            backgroundValue = preset,
            layout = "standard",
            maskSensitive = false,
        )
    }
}

/** 材料列表（bundled local / procedural，禁远程 URL）。 */
val CARD_THEME_PRESETS = listOf("minimal", "deep-space", "region", "city", "glass", "metal", "abstract")
val NUMBER_THEME_PRESETS = listOf("country", "city", "minimal", "banking", "travel", "recovery", "work", "private")

/** 稳定 testId 常量（四端语义一致）。 */
object VTestIds {
    const val NAV_RAIL = "pdig.nav.rail"
    const val NAV_BOTTOM = "pdig.nav.bottom"
    const val NAV_TOP = "pdig.nav.top"
    const val GLOBE_STAGE = "pdig.globe.stage"
    const val GLOBE_CANVAS = "pdig.globe.canvas"
    const val OVERVIEW_ACTIVITY = "pdig.overview.activity"
    const val OVERVIEW_QUICK = "pdig.overview.quick"
    const val NOW_ATTENTION = "pdig.now.attention"
    const val NOW_CHANGES = "pdig.now.changes"
    const val NOW_UPCOMING = "pdig.now.upcoming"
    const val CARD_GRID = "pdig.card.grid"
    const val CARD_LIST = "pdig.card.list"
    const val CARD_ROW = "pdig.card.row"
    const val CARD_VIEW_TOGGLE = "pdig.card.viewToggle"
    const val CARD_INSPECTOR = "pdig.card.inspector"
    const val PHONE_LIST = "pdig.phone.list"
    const val PHONE_INSPECTOR = "pdig.phone.inspector"
    const val CARD_DETAIL_IDENTITY = "pdig.card.detail.identity"
    const val CARD_FACE = "pdig.card.face"
    const val CARD_NICKNAME = "pdig.card.nickname"
    const val CARD_ISSUER = "pdig.card.issuer"
    const val CARD_FORM = "pdig.card.form"
    const val CARD_MASKED = "pdig.card.masked"
    const val CARD_META = "pdig.card.meta"
    const val NUMBER_ROW = "pdig.number.row"
    const val NUMBER_DETAIL_HERO = "pdig.number.detail.hero"
    const val NUMBER_DETAIL_SERVICES = "pdig.number.detail.services"
    const val CHANGE_OLD = "pdig.change.old"
    const val CHANGE_SERVICES = "pdig.change.services"
    const val CHANGE_NEW = "pdig.change.new"
    const val CHANGE_STEPPER_MINI = "pdig.change.stepper.mini"
    const val CHANGE_STEPPER_CURRENT = "pdig.change.stepper.current"
    const val STUDIO_THEME_TILE = "pdig.studio.theme.tile"
    const val CARD_DETAIL_INFO = "pdig.card.detail.info"
    const val CUSTOMIZATION_LIBRARY = "pdig.customization.library"
    const val NOW_GLOBE = "pdig.now.globe"
    const val CUSTOMIZATION_PREVIEW = "pdig.customization.preview"
    const val CUSTOMIZATION_INSPECTOR = "pdig.customization.inspector"
}
