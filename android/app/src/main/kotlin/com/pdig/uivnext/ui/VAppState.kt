package com.pdig.uivnext.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pdig.uivnext.globe.GlobeCamera
import com.pdig.uivnext.globe.GlobeController
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
    initialCamera: GlobeCamera = GlobeCamera(0f, 30f, 1f),
    initialPresentationProfiles: Map<String, PresentationProfile> = emptyMap(),
    private val onPresentationProfileSaved: (PresentationProfile) -> Unit = {},
) {
    var screen by mutableStateOf(initialScreen)
    var regionFilter by mutableStateOf<String?>(null)
    var privacyMask by mutableStateOf(true)
    var reduceMotion by mutableStateOf(false)
    var railExpanded by mutableStateOf(true)
    val globe = GlobeController(initialCamera)

    /** 变更投影（Desktop Continuity 三态）：current / transition / after。After = Plan Projection（非现实）。 */
    var changeProjection by mutableStateOf("transition")

    /** 证据参数：Studio 主题覆盖 + 实际渲染回读（expected==actual 校验）。 */
    var evidenceThemeId by mutableStateOf<String?>(null)

    /** 证据参数：空 fixture 模式（空态截图；与真实持久化隔离，绝不读用户 profile）。 */
    var emptyDemo by mutableStateOf(false)

    /** 本次运行中的 PresentationProfile；只影响显示，不写入 PersonalReality / Canonical。 */
    private val presentationProfiles = mutableStateMapOf<String, PresentationProfile>().apply {
        putAll(initialPresentationProfiles)
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
    var selectedNumberId by mutableStateOf<String?>(null)

    /** 顶层导航/直接切换（bottom nav / rail / chips）：只有 search/change 进栈（保持既有语义）。 */
    fun navigate(next: VScreen) {
        when (next) {
            VScreen.SEARCH, VScreen.CHANGE_PHONE -> {
                if (screen != next) navBackTarget = screen
                screen = next
            }
            else -> {
                backStack.clear()
                screen = next
            }
        }
    }

    /** 从搜索进入目标页，保留 Search 作为返回目标；普通顶层导航仍保持原语义。 */
    fun navigateFromSearch(next: VScreen) {
        if (screen != VScreen.SEARCH || next == VScreen.SEARCH) {
            navigate(next)
            return
        }
        navBackTarget = VScreen.SEARCH
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

    fun clearRegion() {
        regionFilter = null
        globe.backToGlobal()
    }

    /** 设置 globe 外部初始 camera（--globe-camera 参数 / 截图确定性）。 */
    fun applyCameraPreset(preset: String) {
        globe.camera = cameraPreset(preset)
    }

    /**
     * System back：region drawer → 关闭抽屉；back stack 非空 → 弹栈返回；
     * 空栈 → 不做处理，交回系统默认（退出）。
     */
    fun back() {
        if (globe.state == VGlobeState.REGION_DETAIL) {
            clearRegion()
            return
        }
        if (backStack.isNotEmpty()) {
            screen = backStack.removeAt(backStack.lastIndex)
        }
    }

    /** 是否有可返回的内部层级（BackHandler enabled 条件）。 */
    fun canGoBack(): Boolean = backStack.isNotEmpty() || globe.state == VGlobeState.REGION_DETAIL

    private fun cameraPreset(preset: String): GlobeCamera {
        if (preset == "global") return GlobeCamera(0f, 30f, 1f)
        val region = com.pdig.uivnext.demo.UiVNextDemoFixture.regions.firstOrNull { it.regionCode.equals(preset, ignoreCase = true) }
        if (region != null) {
            val focus = com.pdig.uivnext.globe.focusCamera(region.latitude.toFloat(), region.longitude.toFloat())
            return focus.copy(zoom = 1.35f)
        }
        return GlobeCamera(0f, 30f, 1f)
    }
}