package com.pdig.uivnext.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pdig.uivnext.globe.GlobeController
import com.pdig.uivnext.globe.GlobeCamera
import com.pdig.uivnext.model.VGlobeState
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.persist.PresentationProfileStore
import com.pdig.uivnext.persist.defaultProfileStore

/** vNext 演示应用状态（Presentation Layer；不触碰真实 domain repos）。 */
class VAppState(
    initialScreen: VScreen = VScreen.NOW,
    initialCamera: GlobeCamera = GlobeCamera(0f, 30f, 1f),
    val profileStore: PresentationProfileStore = defaultProfileStore(),
) {
    var screen by mutableStateOf(initialScreen)
    var regionFilter by mutableStateOf<String?>(null)
    var privacyMask by mutableStateOf(true)
    var reduceMotion by mutableStateOf(false)
    var railExpanded by mutableStateOf(true)
    var initialCustomTheme by mutableStateOf<String?>(null)
    /** 证据 seam：CustomizationScreen 渲染后回写实际 selected themeId（expected/actual variant 校验；仅证据用途，不影响产品行为）。 */
    var evidenceThemeId by mutableStateOf<String?>(null)
    /** 证据 seam（截图/交互日志）：强制空列表渲染（Cards/Numbers 空状态，§45–46）。 */
    var demoEmptyCards by mutableStateOf(false)
    var demoEmptyNumbers by mutableStateOf(false)
    /** PHASE 1F 空态证据 seam：Now 需要处理 / 正在进行 / Overview 地区空。 */
    var demoEmptyAttention by mutableStateOf(false)
    var demoEmptyChanges by mutableStateOf(false)
    var demoEmptyRegion by mutableStateOf(false)
    /** Command Palette（PHASE 1E §39）：真实可用键盘/搜索；仅影响导航。 */
    var paletteOpen by mutableStateOf(false)
    var paletteQuery by mutableStateOf("")
    var paletteSelectedIndex by mutableStateOf(0)
    /** 导入背景（Card / Number Studio）命中的本地文件路径（app-managed；证据 seam）。 */
    var lastImportedBackgroundPath by mutableStateOf<String?>(null)
    /** Change Phone 投影（current / transition / after；after = 计划投影，不冒充现实）。 */
    var changeProjection by mutableStateOf("transition")
    val globe = GlobeController(initialCamera)

    fun navigate(next: VScreen) {
        screen = next
    }

    fun openCard(cardId: String) {
        screen = VScreen.CARD_DETAIL
        selectedCardId = cardId
    }

    fun openNumber(numberId: String) {
        screen = VScreen.NUMBER_DETAIL
        selectedNumberId = numberId
    }

    fun openCardCustomization(cardId: String) {
        screen = VScreen.CARD_CUSTOMIZATION
        selectedCardId = cardId
    }

    fun openNumberCustomization(numberId: String) {
        screen = VScreen.NUMBER_CUSTOMIZATION
        selectedNumberId = numberId
    }

    var selectedCardId by mutableStateOf<String?>(null)
    var selectedNumberId by mutableStateOf<String?>(null)

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