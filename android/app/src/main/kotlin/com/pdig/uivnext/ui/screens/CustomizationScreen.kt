package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.CARD_THEME_PRESETS
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.NUMBER_THEME_PRESETS
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.model.hexColorOrNull
import com.pdig.uivnext.model.layoutLabelZh
import com.pdig.uivnext.model.materialLabelZh
import com.pdig.uivnext.model.themeLabelZh
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VTouchTarget
import com.pdig.uivnext.model.CARD_MATERIAL_CHOICES
import com.pdig.uivnext.model.NUMBER_MATERIAL_CHOICES
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.AssetCard
import com.pdig.uivnext.ui.components.NumberFace
import com.pdig.uivnext.ui.components.StudioInspector
import com.pdig.uivnext.ui.components.StudioKind
import com.pdig.uivnext.ui.components.StudioPropRow
import com.pdig.uivnext.ui.components.ThemeTile

/**
 * Card / Number Customization Studio（brief §13-§16）。
 * 手机：Preview 在上、编辑面板在下；编辑对象 = PresentationProfile（本地偏好，绝不写 .depmap；
 * 外观资源仅用于本机显示）。主题选择使用真实 visual thumbnail + 用户语言；
 * Inspector 使用 consumer 语言（外观 / 材质 / 布局 / 信息 / 隐私），不暴露 hex / internal enum / preset id。
 * 证据：`app.evidenceThemeId` 覆盖初始主题并在修改时回写（expected==actual；见 Variant 契约）。
 */
@Composable
fun CardCustomizationScreen(app: VAppState, breakpoint: MediaBreakpoint) {
    val card = UiVNextDemoFixture.cardById(app.selectedCardId ?: "card-cn-1") ?: return
    val savedProfile = app.presentationProfile("card", card.id, card.preset)
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
            )
        },
        rows = consumerCardRows(profile),
        toggles = listOf("昵称", "卡组织", "地区", "币种", "状态"),
        privacyMasked = profile.maskSensitive,
        onPrivacy = { profile = profile.copy(maskSensitive = it) },
        breakpoint = breakpoint,
        isDirty = profile != savedProfile,
        onSave = { app.savePresentationProfile(profile) },
    )
}

@Composable
fun NumberCustomizationScreen(app: VAppState, breakpoint: MediaBreakpoint) {
    val number = UiVNextDemoFixture.numberById(app.selectedNumberId ?: "num-cn-1") ?: return
    val savedProfile = app.presentationProfile("phoneNumber", number.id, number.preset)
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
            )
        },
        rows = consumerNumberRows(profile),
        toggles = listOf("昵称", "运营商", "SIM 类型", "主副号", "用途标签"),
        privacyMasked = profile.maskSensitive,
        onPrivacy = { profile = profile.copy(maskSensitive = it) },
        breakpoint = breakpoint,
        isDirty = profile != savedProfile,
        onSave = { app.savePresentationProfile(profile) },
    )
}

/** Card Studio consumer 属性行（外观 / 背景 / 布局 / 强调色样；禁止 hex 文本）。 */
private fun consumerCardRows(profile: PresentationProfile): List<StudioPropRow> = listOf(
    StudioPropRow("外观", themeLabelZh("card", profile.themeId), swatch = hexColorOrNull(profile.accentColor)),
    StudioPropRow("背景", materialLabelZh(profile.material)),
    StudioPropRow("布局", layoutLabelZh(profile.layout)),
)

/** Number Studio consumer 属性行（communication identity；不显示内部 preset id）。 */
private fun consumerNumberRows(profile: PresentationProfile): List<StudioPropRow> = listOf(
    StudioPropRow("外观", themeLabelZh("number", profile.themeId), swatch = hexColorOrNull(profile.accentColor)),
    StudioPropRow("背景", materialLabelZh(profile.material)),
    StudioPropRow("布局", layoutLabelZh(profile.layout)),
)

@Composable
private fun CustomizationFrame(
    kind: StudioKind,
    title: String,
    presets: List<String>,
    profile: PresentationProfile,
    onPreset: (String) -> Unit,
    onMaterial: (String) -> Unit,
    materials: List<String>,
    preview: @Composable () -> Unit,
    rows: List<StudioPropRow>,
    toggles: List<String>,
    privacyMasked: Boolean,
    onPrivacy: (Boolean) -> Unit,
    breakpoint: MediaBreakpoint,
    isDirty: Boolean,
    onSave: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = PdigV2Colors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Surface(
                color = if (isDirty) PdigV2Colors.Primary else PdigV2Colors.Positive.copy(alpha = 0.18f),
                shape = RoundedCornerShape(VRadius.Md),
                modifier = Modifier
                    .defaultMinSize(minHeight = VTouchTarget.Min)
                    .clickableLocal { if (isDirty) onSave() },
            ) {
                Text(
                    if (isDirty) "保存" else "已保存",
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = if (isDirty) PdigV2Colors.CanvasDeep else PdigV2Colors.Positive,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
        }
        if (breakpoint == MediaBreakpoint.EXPANDED || breakpoint == MediaBreakpoint.MEDIUM) {
            WideCustomization(kind, presets, profile, onPreset, onMaterial, materials, preview, rows, toggles, privacyMasked, onPrivacy)
        } else {
            CompactCustomization(kind, presets, profile, onPreset, onMaterial, materials, preview, rows, toggles, privacyMasked, onPrivacy)
        }
    }
}

@Composable
private fun WideCustomization(
    kind: StudioKind,
    presets: List<String>,
    profile: PresentationProfile,
    onPreset: (String) -> Unit,
    onMaterial: (String) -> Unit,
    materials: List<String>,
    preview: @Composable () -> Unit,
    rows: List<StudioPropRow>,
    toggles: List<String>,
    privacyMasked: Boolean,
    onPrivacy: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxSize()) {
        // Asset Library（左 22%）—— ThemeTile：真实 thumbnail + 用户语言
        Column(
            Modifier
                .weight(0.22f)
                .fillMaxSize()
                .testTagLocal(VTestIds.CUSTOMIZATION_LIBRARY),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("主题", color = PdigV2Colors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            presets.forEach { preset ->
                ThemeTile(
                    kind = kind,
                    preset = preset,
                    selected = profile.themeId == preset,
                    onClick = { onPreset(preset) },
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        // Live Preview（中 46%）
        Column(
            Modifier
                .weight(0.46f)
                .testTagLocal(VTestIds.CUSTOMIZATION_PREVIEW),
            verticalArrangement = Arrangement.Top,
        ) {
            Text("实时预览", color = PdigV2Colors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Surface(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                color = PdigV2Colors.Surface.copy(alpha = 0.7f),
                shape = RoundedCornerShape(VRadius.Xl),
            ) {
                Column(Modifier.padding(20.dp)) { preview() }
            }
            Text(
                "外观素材仅用于本机显示，不会改变实际信息。",
                color = PdigV2Colors.TextMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        // Property Inspector（右 32%）—— consumer 语言
        Column(
            Modifier
                .weight(0.32f)
                .fillMaxSize()
                .testTagLocal(VTestIds.CUSTOMIZATION_INSPECTOR),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StudioInspector(rows, materials, profile.material, onMaterial, toggles, privacyMasked, onPrivacy)
        }
    }
}

@Composable
private fun CompactCustomization(
    kind: StudioKind,
    presets: List<String>,
    profile: PresentationProfile,
    onPreset: (String) -> Unit,
    onMaterial: (String) -> Unit,
    materials: List<String>,
    preview: @Composable () -> Unit,
    rows: List<StudioPropRow>,
    toggles: List<String>,
    privacyMasked: Boolean,
    onPrivacy: (Boolean) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Live Preview 在上（手机不复制 desktop inspector）
        Column(
            Modifier
                .fillMaxWidth()
                .testTagLocal(VTestIds.CUSTOMIZATION_PREVIEW),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("实时预览", color = PdigV2Colors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Surface(
                Modifier.fillMaxWidth(),
                color = PdigV2Colors.Surface.copy(alpha = 0.7f),
                shape = RoundedCornerShape(VRadius.Xl),
            ) {
                Column(Modifier.padding(20.dp)) { preview() }
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .testTagLocal(VTestIds.CUSTOMIZATION_LIBRARY),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("主题", color = PdigV2Colors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            presets.forEach { preset ->
                ThemeTile(
                    kind = kind,
                    preset = preset,
                    selected = profile.themeId == preset,
                    onClick = { onPreset(preset) },
                )
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .testTagLocal(VTestIds.CUSTOMIZATION_INSPECTOR),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StudioInspector(rows, materials, profile.material, onMaterial, toggles, privacyMasked, onPrivacy)
        }
    }
}