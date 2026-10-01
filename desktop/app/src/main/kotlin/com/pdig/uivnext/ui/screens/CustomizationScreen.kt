package com.pdig.uivnext.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.CARD_THEME_PRESETS
import com.pdig.uivnext.model.NUMBER_THEME_PRESETS
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.AssetCard
import com.pdig.uivnext.ui.components.CardFaceThumbnail
import com.pdig.uivnext.ui.components.NumberFace
import com.pdig.uivnext.ui.components.NumberFaceThumbnail
import com.pdig.uivnext.ui.components.cardProfileOf

/**
 * Card / Number Customization Studio 入口（Review §9–§15）。
 * 三栏（对象库 22% / 大尺寸实时预览 46% / 分组编辑器 32%）由 StudioFrame 提供；
 * 编辑对象 = PresentationProfile（本地偏好，绝不写 .depmap）。
 * 主题选择 = 确定性视觉缩略图（共享 PresentationProfile 渲染器，不依赖视觉判断）。
 */

@Composable
fun CardCustomizationScreen(app: VAppState) {
    val initial = UiVNextDemoFixture.cardById(app.selectedCardId ?: "card-cn-1") ?: return
    var selectedId by remember { mutableStateOf(initial.id) }
    val themeOverride = app.initialCustomTheme
    var profile by remember {
        mutableStateOf(
            app.profileStore.load("card", initial.id)
                ?: if (themeOverride != null) {
                    PresentationProfile.defaultFor("card", initial.id, themeOverride)
                } else {
                    cardProfileOf(initial)
                },
        )
    }
    StudioFrame(
        title = "卡面定制",
        libraryItems = UiVNextDemoFixture.cards.map { it.id to it.nickname },
        selectedLibraryId = selectedId,
        onSelectLibrary = {
            selectedId = it
            profile = app.profileStore.load("card", it)
                ?: PresentationProfile.defaultFor("card", it, UiVNextDemoFixture.cardById(it)?.preset ?: "minimal")
        },
        presets = CARD_THEME_PRESETS,
        materials = listOf("minimal", "matte", "glass", "metal"),
        layouts = listOf("standard", "emblem", "minimal-content"),
        profile = profile,
        onProfileChange = { profile = it },
        reduceMotion = app.reduceMotion,
        thumbnail = { preset -> CardFaceThumbnail(preset) },
        libraryThumbnail = { id ->
            val card = UiVNextDemoFixture.cardById(id) ?: return@StudioFrame
            AssetCard(
                card = card,
                privacyMask = app.privacyMask,
                onClick = {},
            )
        },
        preview = { id ->
            val card = UiVNextDemoFixture.cardById(id) ?: return@StudioFrame
            AssetCard(
                card = card.copy(preset = profile.themeId),
                privacyMask = app.privacyMask,
                onClick = {},
                profile = profile,
            )
        },
        onSave = {
            // PresentationProfile 持久化（PHASE 1E §66）；绝不写 .depmap / canonical。
            app.profileStore.save(profile)
        },
    )
}

@Composable
fun NumberCustomizationScreen(app: VAppState) {
    val initial = UiVNextDemoFixture.numberById(app.selectedNumberId ?: "num-cn-1") ?: return
    var selectedId by remember { mutableStateOf(initial.id) }
    var profile by remember {
        mutableStateOf(
            app.profileStore.load("phoneNumber", initial.id)
                ?: PresentationProfile.defaultFor("phoneNumber", initial.id, app.initialCustomTheme ?: "country"),
        )
    }
    StudioFrame(
        title = "号码面定制",
        libraryItems = UiVNextDemoFixture.numbers.map { it.id to it.nickname },
        selectedLibraryId = selectedId,
        onSelectLibrary = {
            selectedId = it
            profile = app.profileStore.load("phoneNumber", it)
                ?: PresentationProfile.defaultFor("phoneNumber", it, "country")
        },
        presets = NUMBER_THEME_PRESETS,
        materials = emptyList(),
        layouts = listOf("standard", "minimal-content"),
        profile = profile,
        onProfileChange = { profile = it },
        reduceMotion = app.reduceMotion,
        thumbnail = { preset -> NumberFaceThumbnail(preset) },
        libraryThumbnail = { id ->
            val number = UiVNextDemoFixture.numberById(id) ?: return@StudioFrame
            NumberFace(
                number = number,
                privacyMask = app.privacyMask,
                onClick = {},
            )
        },
        preview = { id ->
            val number = UiVNextDemoFixture.numberById(id) ?: return@StudioFrame
            NumberFace(
                number = number,
                privacyMask = app.privacyMask,
                onClick = {},
                profile = profile,
            )
        },
        onSave = {
            app.profileStore.save(profile)
        },
    )
}
