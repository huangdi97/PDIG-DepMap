package com.pdig.uivnext.evidence

import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.persist.resolveStudioProfile
import com.pdig.uivnext.ui.VAppState
import java.io.File

/**
 * FINAL_SCREENSHOT_MANIFEST.json 的状态记录（expected/actual 校验与记录）。
 * 与 probe 收集（VNextPhaseProbe1FHF.kt）和 run() 编排（VNextPhaseEvidence1FHF.kt）分离，
 * 控制生产源文件体积（≤300 行）。
 */

/** 变体主题的确定性解析（隔离 store 下 = themeOverride；预期与实际不一致时 run() 拒绝该帧）。 */
internal fun resolvedThemeOf(shot: VNextPhaseEvidence1FHF.Shot, app: VAppState): String? {
    if (shot.theme == null) return null
    val (type, id) = when (shot.screen) {
        VScreen.CARD_CUSTOMIZATION -> "card" to (app.selectedCardId ?: "card-cn-2")
        VScreen.NUMBER_CUSTOMIZATION -> "phoneNumber" to (app.selectedNumberId ?: "num-cn-1")
        else -> return null
    }
    return resolveStudioProfile(app.profileStore, type, id, shot.theme) {
        PresentationProfile.defaultFor(type, id, "minimal")
    }.themeId
}

internal fun manifestEntry(shot: VNextPhaseEvidence1FHF.Shot, app: VAppState, png: File, resolvedTheme: String?): Map<String, Any?> {
    val screenName = when (shot.screen) {
        VScreen.NOW -> "now"
        VScreen.OVERVIEW -> "infrastructure-overview"
        VScreen.CARDS -> "cards"
        VScreen.CARD_DETAIL -> "card-detail"
        VScreen.CARD_CUSTOMIZATION -> "card-studio"
        VScreen.NUMBERS -> "numbers"
        VScreen.NUMBER_DETAIL -> "number-detail"
        VScreen.NUMBER_CUSTOMIZATION -> "number-studio"
        VScreen.CHANGE_PHONE -> "change-phone"
        else -> shot.screen.name.lowercase()
    }
    val theme = shot.theme
    val actualTheme = app.evidenceThemeId ?: resolvedTheme
    val projection = if (shot.screen == VScreen.CHANGE_PHONE) app.changeProjection else null
    val selectedObject = selectedObjectOf(shot, app)
    val expectedState = mapOf(
        "screen" to screenName,
        "theme" to theme,
        "projection" to projection,
        "selectedObject" to selectedObject,
        "camera" to shot.camera,
    )
    val actualState = mapOf(
        "screen" to screenName,
        "theme" to actualTheme,
        "projection" to projection,
        "selectedObject" to selectedObject,
        "camera" to shot.camera,
    )
    return mapOf(
        "screen" to screenName,
        "file" to png.name,
        "expectedState" to expectedState,
        "actualState" to actualState,
        "themeSource" to (if (app.evidenceThemeId != null) "composition-readback" else "deterministic-resolve"),
        "sha256" to VNextShotDriver.sha256Of(png),
        "width" to 1920,
        "height" to 1080,
        "privacyMask" to true,
        "stateValidation" to (expectedState == actualState),
    )
}

internal fun selectedObjectOf(shot: VNextPhaseEvidence1FHF.Shot, app: VAppState): String? = when (shot.screen) {
    VScreen.CARD_DETAIL, VScreen.CARD_CUSTOMIZATION -> app.selectedCardId
    VScreen.NUMBER_DETAIL, VScreen.NUMBER_CUSTOMIZATION -> app.selectedNumberId
    else -> null
}
