package com.pdig.uivnext.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pdig.uivnext.globe.GlobeCamera
import com.pdig.uivnext.globe.GlobeController
import com.pdig.uivnext.globe.focusCamera
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.model.VGlobeState
import com.pdig.uivnext.model.VScreen

/**
 * vNext 演示应用状态（Presentation Layer；不触碰真实 domain repos）。
 * 持有导航、Globe、隐私遮蔽、变更投影与证据参数；全部为本地表现状态。
 *
 * Back/State（brief §31）：内部层级（detail/studio/search/change）使用 back stack，
 * 保证「Cards → Detail → Studio → Back → Back → Cards」与「Detail → Studio → Back = Detail」
 * 同时成立；system back 在 stack 清空后才交回系统退出。
 */
class VAppState(
    initialScreen: VScreen = VScreen.NOW,
    initialCamera: GlobeCamera = focusCamera(16f, 107f),
    initialPresentationProfiles: Map<String, PresentationProfile> = emptyMap(),
    initialNumberDisplayNames: Map<String, String> = emptyMap(),
    private val onNumberDisplayNameSaved: (String, String) -> Unit = { _, _ -> },
    initialWorkspacePreferences: WorkspacePreferences = WorkspacePreferences(),
    private val onPresentationProfileSaved: (PresentationProfile) -> Unit = {},
    private val onWorkspacePreferencesSaved: (WorkspacePreferences) -> Unit = {},
) {
    var screen by mutableStateOf(initialScreen)
    var regionFilter by mutableStateOf<String?>(null)

    private var privacyMaskState by mutableStateOf(initialWorkspacePreferences.privacyMask)
    private var reduceMotionState by mutableStateOf(initialWorkspacePreferences.reduceMotion)
    private var railExpandedState by mutableStateOf(initialWorkspacePreferences.railExpanded)
    private var showUpcomingState by mutableStateOf(initialWorkspacePreferences.showUpcoming)

    var privacyMask: Boolean
        get() = privacyMaskState
        set(value) {
            privacyMaskState = value
            persistWorkspacePreferences()
        }

    var reduceMotion: Boolean
        get() = reduceMotionState
        set(value) {
            reduceMotionState = value
            persistWorkspacePreferences()
        }

    var railExpanded: Boolean
        get() = railExpandedState
        set(value) {
            railExpandedState = value
            persistWorkspacePreferences()
        }

    var showUpcoming: Boolean
        get() = showUpcomingState
        set(value) {
            showUpcomingState = value
            persistWorkspacePreferences()
        }

    val globe = GlobeController(initialCamera)

    /** 手机号变更投影：current / transition / after。After = Plan Projection（非现实）。 */
    var changeProjection by mutableStateOf("transition")

    /** 银行卡变更投影独立保存，避免两个场景切换时互相污染。 */
    var cardChangeProjection by mutableStateOf("current")

    /** 证据参数：Studio 主题覆盖 + 实际渲染回读（expected==actual 校验）。 */
    var evidenceThemeId by mutableStateOf<String?>(null)

    /** 证据参数：空 fixture 模式（空态截图；与真实持久化隔离，绝不读用户 profile）。 */
    var emptyDemo by mutableStateOf(false)

    /** 本次运行中的 PresentationProfile；只影响显示，不写入 PersonalReality / Canonical。 */
    private val presentationProfiles = mutableStateMapOf<String, PresentationProfile>().apply {
        putAll(initialPresentationProfiles)
    }

    /** Local presentation aliases: the source number identity is never changed. */
    private val numberDisplayNames = mutableStateMapOf<String, String>().apply {
        putAll(initialNumberDisplayNames)
    }

    fun numberDisplayName(numberId: String, recordedNumber: String): String =
        displayNameForNumber(recordedNumber, numberDisplayNames[numberId])

    fun numberDisplayNameForScreen(numberId: String, recordedNumber: String): String =
        visibleNumberDisplayName(
            recordedNumber, numberDisplayNames[numberId],
            privacyMaskState ||
                (presentationProfiles[presentationKey("phoneNumber", numberId)]?.maskSensitive == true),
        )

    fun numberAlias(numberId: String): String = numberDisplayNames[numberId].orEmpty()

    fun renameNumber(numberId: String, input: String) {
        val alias = input.trim().take(32)
        if (alias.isEmpty()) numberDisplayNames.remove(numberId)
        else numberDisplayNames[numberId] = alias
        onNumberDisplayNameSaved(numberId, alias)
    }

    /** System back 返回栈（栈顶 = 下一返回目标；空栈 → 系统退出）。 */
    private val backStack = mutableStateListOf<VScreen>()

    /** 兼容 API：栈顶或 null。 */
    var navBackTarget: VScreen?
        get() = backStack.lastOrNull()
        set(value) {
            if (value == null) backStack.clear()
            else if (backStack.lastOrNull() != value) backStack.add(value)
        }

    var selectedCardId by mutableStateOf<String?>(null)
    var selectedReplacementCardId by mutableStateOf<String?>(null)
    var selectedNumberId by mutableStateOf<String?>(null)
    var selectedSecondaryObjectId by mutableStateOf<String?>(null)

    /**
     * Every forward navigation records the actual screen of departure.
     * Android/system/header Back always returns to the preceding screen, including
     * changes between bottom tabs. Initial app root alone delegates to system exit.
     * Canonical/PersonalReality are completely unaffected.
     */
    fun navigate(next: VScreen) {
        if (screen == next) return
        backStack.add(screen)
        screen = next
    }

    /** 从工具入口进入设置/数据源等辅助页，保留当前页面作为返回目标。 */
    fun openUtility(next: VScreen) {
        if (screen == next) return
        navBackTarget = screen
        screen = next
    }

    /** 从搜索进入目标页，保留 Search 作为返回目标；普通顶层导航仍保持原语义。 */
    fun navigateFromSearch(next: VScreen) {
        if (screen != VScreen.SEARCH || next == VScreen.SEARCH) {
            navigate(next)
            return
        }
        // Search remains the immediate predecessor; its predecessor is retained.
        backStack.add(VScreen.SEARCH)
        screen = next
    }

    private fun presentationKey(targetType: String, targetId: String): String = targetType + "::" + targetId

    fun presentationProfile(targetType: String, targetId: String, fallbackPreset: String): PresentationProfile =
        presentationProfiles[presentationKey(targetType, targetId)]
            ?: PresentationProfile.defaultFor(targetType, targetId, fallbackPreset).copy(maskSensitive = false)

    fun savedPresentationProfile(targetType: String, targetId: String): PresentationProfile? =
        presentationProfiles[presentationKey(targetType, targetId)]

    fun savePresentationProfile(profile: PresentationProfile) {
        presentationProfiles[presentationKey(profile.targetType, profile.targetId)] = profile
        onPresentationProfileSaved(profile)
    }

    private fun persistWorkspacePreferences() {
        onWorkspacePreferencesSaved(
            WorkspacePreferences(
                privacyMask = privacyMaskState,
                reduceMotion = reduceMotionState,
                railExpanded = railExpandedState,
                showUpcoming = showUpcomingState,
            ),
        )
    }

    fun openCard(cardId: String) {
        navBackTarget = screen
        screen = VScreen.CARD_DETAIL
        selectedCardId = cardId
    }

    fun openNumber(numberId: String) {
        navBackTarget = screen
        screen = VScreen.NUMBER_DETAIL
        selectedNumberId = numberId
    }

    fun openCardChange(cardId: String) {
        navBackTarget = screen
        selectedCardId = cardId
        selectedReplacementCardId = null
        screen = VScreen.CHANGE_CARD
    }

    fun chooseReplacementCard(cardId: String?) {
        selectedReplacementCardId = cardId
    }

    fun openSecondaryObject(detailScreen: VScreen, objectId: String) {
        require(
            detailScreen in setOf(
                VScreen.ACCOUNT_DETAIL,
                VScreen.EMAIL_DETAIL,
                VScreen.DEVICE_DETAIL,
                VScreen.SERVICE_DETAIL,
            ),
        ) { "Unsupported secondary detail route: $detailScreen" }
        navBackTarget = screen
        selectedSecondaryObjectId = objectId
        screen = detailScreen
    }

    fun openCardCustomization(cardId: String) {
        // 从 Detail 进入 Studio：把 Detail 也进栈，保证 Studio→Back→Detail→Back→原列表。
        if (screen == VScreen.CARD_DETAIL) navBackTarget = VScreen.CARD_DETAIL else navBackTarget = screen
        screen = VScreen.CARD_CUSTOMIZATION
        selectedCardId = cardId
    }

    fun openNumberCustomization(numberId: String) {
        if (screen == VScreen.NUMBER_DETAIL) navBackTarget = VScreen.NUMBER_DETAIL else navBackTarget = screen
        screen = VScreen.NUMBER_CUSTOMIZATION
        selectedNumberId = numberId
    }

    /** Globe → 地区选中（region filter + REGION_SELECTED）。 */
    fun selectRegion(code: String) {
        regionFilter = code
        globe.selectedRegion = code
        globe.state = VGlobeState.REGION_SELECTED
    }

    fun openRegionDetail() {
        globe.state = VGlobeState.REGION_DETAIL
    }

    /**
     * 关闭地区详情但保留当前地区上下文。
     * System Back 应当只退出 detail 层；只有显式“返回全球视图”才清除 regionFilter。
     */
    fun closeRegionDetail() {
        if (globe.state != VGlobeState.REGION_DETAIL) return
        globe.state = if (regionFilter != null) VGlobeState.REGION_SELECTED else VGlobeState.GLOBAL
    }

    fun clearRegion() {
        regionFilter = null
        globe.backToGlobal()
    }

    /** 设置 globe 外部初始 camera（--globe-camera 参数 / 截图确定性）。 */
    fun applyCameraPreset(preset: String) {
        globe.camera = cameraPreset(preset)
    }

    /**
     * System back：region drawer → 关闭详情并保留地区筛选；back stack 非空 → 弹栈返回；
     * 空栈 → 不做处理，交回系统默认（退出）。
     */
    fun back() {
        if (globe.state == VGlobeState.REGION_DETAIL) {
            closeRegionDetail()
            return
        }
        if (backStack.isNotEmpty()) {
            screen = backStack.removeAt(backStack.lastIndex)
        }
    }

    /**
     * The header arrow is hierarchical UP, not chronological system Back.
     * System Back keeps the true visited-page history. UP always returns to
     * the stable parent object/section so Card→Studio→UP = Card Detail.
     */
    fun upDestination(): VScreen? = when (screen) {
        VScreen.NOW, VScreen.INFRASTRUCTURE, VScreen.CHANGE, VScreen.RECORDS, VScreen.ME -> null
        VScreen.CARD_CUSTOMIZATION -> VScreen.CARD_DETAIL
        VScreen.CARD_DETAIL -> VScreen.CARDS
        VScreen.NUMBER_CUSTOMIZATION -> VScreen.NUMBER_DETAIL
        VScreen.NUMBER_DETAIL -> VScreen.NUMBERS
        VScreen.ACCOUNT_DETAIL -> VScreen.ACCOUNTS
        VScreen.EMAIL_DETAIL -> VScreen.EMAILS
        VScreen.DEVICE_DETAIL -> VScreen.DEVICES
        VScreen.SERVICE_DETAIL -> VScreen.SERVICES
        VScreen.CARDS, VScreen.NUMBERS, VScreen.ACCOUNTS, VScreen.EMAILS,
        VScreen.DEVICES, VScreen.SERVICES, VScreen.WEAKNESSES, VScreen.OVERVIEW -> VScreen.INFRASTRUCTURE
        VScreen.CHANGE_PHONE, VScreen.CHANGE_CARD -> VScreen.CHANGE
        VScreen.REVIEW -> VScreen.NOW
        VScreen.IMPORT -> VScreen.SOURCES
        VScreen.MANUAL_ADD -> VScreen.IMPORT
        VScreen.MANUAL_RELATION -> VScreen.MANUAL_ADD
        VScreen.SETTINGS, VScreen.PERSONALIZATION, VScreen.SOURCES -> VScreen.ME
        VScreen.SEARCH -> backStack.lastOrNull() ?: VScreen.NOW
    }

    fun canNavigateUp(): Boolean = upDestination() != null ||
        globe.state == VGlobeState.REGION_DETAIL

    fun navigateUp() {
        if (globe.state == VGlobeState.REGION_DETAIL) {
            closeRegionDetail()
            return
        }
        val parent = upDestination() ?: return
        if (parent == screen) return
        val parentIndex = backStack.indexOfLast { it == parent }
        if (parentIndex >= 0) {
            // Pop through the parent, preventing a loop where system Back
            // returns to the child just dismissed with the top arrow.
            while (backStack.size > parentIndex) backStack.removeAt(backStack.lastIndex)
        } else {
            // A detail may have been deep-linked or reached from another section.
            // Its new parent becomes the current page; prior real history remains.
            // No instance of the hierarchy parent in the prior history; keep the
            // actual preceding screen available to Android/system Back.
        }
        screen = parent
    }

    /** 是否有可返回的内部层级（BackHandler enabled 条件）。 */
    fun canGoBack(): Boolean = backStack.isNotEmpty() || globe.state == VGlobeState.REGION_DETAIL

    private fun cameraPreset(preset: String): GlobeCamera {
        if (preset == "global") return focusCamera(16f, 107f)
        val region = com.pdig.uivnext.demo.UiVNextDemoFixture.regions.firstOrNull { it.regionCode.equals(preset, ignoreCase = true) }
        if (region != null) {
            val focus = com.pdig.uivnext.globe.focusCamera(region.latitude.toFloat(), region.longitude.toFloat())
            return focus.copy(zoom = 1.35f)
        }
        return focusCamera(16f, 107f)
    }
}