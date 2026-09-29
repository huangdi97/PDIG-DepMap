package com.pdig.uivnext.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pdig.uivnext.globe.GlobeController
import com.pdig.uivnext.globe.GlobeCamera
import com.pdig.uivnext.model.VGlobeState
import com.pdig.uivnext.model.VScreen

/** vNext 演示应用状态（Presentation Layer；不触碰真实 domain repos）。 */
class VAppState(
    initialScreen: VScreen = VScreen.NOW,
    initialCamera: GlobeCamera = GlobeCamera(0f, 30f, 1f),
) {
    var screen by mutableStateOf(initialScreen)
    var regionFilter by mutableStateOf<String?>(null)
    var privacyMask by mutableStateOf(true)
    var reduceMotion by mutableStateOf(false)
    var railExpanded by mutableStateOf(true)
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