package com.pdig.uivnext.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.CARD_MATERIAL_CHOICES
import com.pdig.uivnext.model.CARD_THEME_PRESETS
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.NUMBER_MATERIAL_CHOICES
import com.pdig.uivnext.model.NUMBER_THEME_PRESETS
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.model.accentLabelZh
import com.pdig.uivnext.model.hexColorOrNull
import com.pdig.uivnext.model.layoutLabelZh
import com.pdig.uivnext.model.materialLabelZh
import com.pdig.uivnext.model.themeLabelZh
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.AssetCard
import com.pdig.uivnext.ui.components.NumberFace
import com.pdig.uivnext.ui.components.StudioKind
import com.pdig.uivnext.ui.components.StudioPropRow

@Composable
fun CardCustomizationScreen(app: VAppState, breakpoint: MediaBreakpoint) {
    val card = UiVNextDemoFixture.cardById(app.selectedCardId ?: "card-cn-1") ?: return
    val baseProfile = PresentationProfile.defaultFor("card", card.id, card.preset)
    val savedProfile = app.savedPresentationProfile("card", card.id) ?: baseProfile
    val initialTheme = app.evidenceThemeId ?: savedProfile.themeId
    var profile by remember(card.id, initialTheme) {
        mutableStateOf(savedProfile.copy(themeId = initialTheme, backgroundValue = initialTheme))
    }

    CustomizationFrame(
        kind = StudioKind.CARD,
        title = "卡面定制 · ${card.nickname}",
        presets = CARD_THEME_PRESETS,
        profile = profile,
        onPreset = {
            profile = profile.copy(themeId = it, backgroundValue = it)
            if (app.evidenceThemeId != null) app.evidenceThemeId = it
        },
        onMaterial = { profile = profile.copy(material = it) },
        materials = CARD_MATERIAL_CHOICES,
        preview = {
            AssetCard(
                card = card.copy(preset = profile.themeId),
                privacyMask = app.privacyMask || profile.maskSensitive,
                onClick = {},
                presentationMaterial = profile.material,
                presentationAccent = hexColorOrNull(profile.accentColor),
                presentationLayout = profile.layout,
            )
        },
        rows = consumerCardRows(profile),
        displayFields = listOf("昵称", "卡组织", "地区", "币种", "状态"),
        privacyMasked = profile.maskSensitive,
        globalPrivacyMask = app.privacyMask,
        onPrivacy = { profile = profile.copy(maskSensitive = it) },
        onLayout = { profile = profile.copy(layout = it) },
        onAccent = { profile = profile.copy(accentColor = it) },
        breakpoint = breakpoint,
        isDirty = profile != savedProfile,
        canReset = profile != baseProfile,
        onReset = { profile = baseProfile },
        onSave = { app.savePresentationProfile(profile) },
    )
}

@Composable
fun NumberCustomizationScreen(app: VAppState, breakpoint: MediaBreakpoint) {
    val number = UiVNextDemoFixture.numberById(app.selectedNumberId ?: "num-cn-1") ?: return
    val baseProfile = PresentationProfile.defaultFor("phoneNumber", number.id, number.preset)
    val savedProfile = app.savedPresentationProfile("phoneNumber", number.id) ?: baseProfile
    val initialTheme = app.evidenceThemeId ?: savedProfile.themeId
    var profile by remember(number.id, initialTheme) {
        mutableStateOf(savedProfile.copy(themeId = initialTheme, backgroundValue = initialTheme))
    }

    CustomizationFrame(
        kind = StudioKind.NUMBER,
        title = "号码面定制 · ${number.nickname}",
        presets = NUMBER_THEME_PRESETS,
        profile = profile,
        onPreset = {
            profile = profile.copy(themeId = it, backgroundValue = it)
            if (app.evidenceThemeId != null) app.evidenceThemeId = it
        },
        onMaterial = { profile = profile.copy(material = it) },
        materials = NUMBER_MATERIAL_CHOICES,
        preview = {
            NumberFace(
                number = number.copy(preset = profile.themeId),
                privacyMask = app.privacyMask || profile.maskSensitive,
                onClick = {},
                presentationMaterial = profile.material,
                presentationAccent = hexColorOrNull(profile.accentColor),
                presentationLayout = profile.layout,
            )
        },
        rows = consumerNumberRows(profile),
        displayFields = listOf("昵称", "运营商", "SIM 类型", "主副号", "用途标签"),
        privacyMasked = profile.maskSensitive,
        globalPrivacyMask = app.privacyMask,
        onPrivacy = { profile = profile.copy(maskSensitive = it) },
        onLayout = { profile = profile.copy(layout = it) },
        onAccent = { profile = profile.copy(accentColor = it) },
        breakpoint = breakpoint,
        isDirty = profile != savedProfile,
        canReset = profile != baseProfile,
        onReset = { profile = baseProfile },
        onSave = { app.savePresentationProfile(profile) },
    )
}

/** Card Studio consumer 属性行（外观 / 背景 / 布局 / 强调色样；禁止 hex 文本）。 */
private fun consumerCardRows(profile: PresentationProfile): List<StudioPropRow> = listOf(
    StudioPropRow("外观", themeLabelZh("card", profile.themeId), swatch = hexColorOrNull(profile.accentColor)),
    StudioPropRow("背景", materialLabelZh(profile.material)),
    StudioPropRow("布局", layoutLabelZh(profile.layout)),
    StudioPropRow("强调", accentLabelZh(profile.accentColor), swatch = hexColorOrNull(profile.accentColor)),
)

/** Number Studio consumer 属性行（communication identity；不显示内部 preset id）。 */
private fun consumerNumberRows(profile: PresentationProfile): List<StudioPropRow> = listOf(
    StudioPropRow("外观", themeLabelZh("number", profile.themeId), swatch = hexColorOrNull(profile.accentColor)),
    StudioPropRow("背景", materialLabelZh(profile.material)),
    StudioPropRow("布局", layoutLabelZh(profile.layout)),
    StudioPropRow("强调", accentLabelZh(profile.accentColor), swatch = hexColorOrNull(profile.accentColor)),
)

