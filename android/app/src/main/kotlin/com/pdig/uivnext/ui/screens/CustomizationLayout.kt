package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.PRESENTATION_ACCENT_CHOICES
import com.pdig.uivnext.model.PRESENTATION_LAYOUT_CHOICES
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VTouchTarget
import com.pdig.uivnext.ui.components.StudioInspector
import com.pdig.uivnext.ui.components.StudioKind
import com.pdig.uivnext.ui.components.StudioPropRow
import com.pdig.uivnext.ui.components.ThemeTile

@Composable
internal fun CustomizationFrame(
    kind: StudioKind,
    title: String,
    presets: List<String>,
    profile: PresentationProfile,
    onPreset: (String) -> Unit,
    onMaterial: (String) -> Unit,
    materials: List<String>,
    preview: @Composable () -> Unit,
    rows: List<StudioPropRow>,
    displayFields: List<String>,
    privacyMasked: Boolean,
    globalPrivacyMask: Boolean,
    onPrivacy: (Boolean) -> Unit,
    onLayout: (String) -> Unit,
    onAccent: (String) -> Unit,
    breakpoint: MediaBreakpoint,
    isDirty: Boolean,
    canReset: Boolean,
    onReset: () -> Unit,
    onSave: () -> Unit,
) {
    val compact = breakpoint == MediaBreakpoint.COMPACT
    Column(
        Modifier
            .fillMaxSize()
            .padding(if (compact) 16.dp else 24.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                color = PdigV2Colors.TextPrimary,
                fontSize = if (compact) 20.sp else 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
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
        if (breakpoint == MediaBreakpoint.EXPANDED) {
            WideCustomization(kind, presets, profile, onPreset, onMaterial, materials, preview, rows, displayFields, privacyMasked, globalPrivacyMask, onPrivacy, onLayout, onAccent, canReset, onReset)
        } else {
            CompactCustomization(kind, presets, profile, onPreset, onMaterial, materials, preview, rows, displayFields, privacyMasked, globalPrivacyMask, onPrivacy, onLayout, onAccent, canReset, onReset)
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
    displayFields: List<String>,
    privacyMasked: Boolean,
    globalPrivacyMask: Boolean,
    onPrivacy: (Boolean) -> Unit,
    onLayout: (String) -> Unit,
    onAccent: (String) -> Unit,
    canReset: Boolean,
    onReset: () -> Unit,
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
            StudioInspector(
                rows = rows,
                materials = materials,
                currentMaterial = profile.material,
                onMaterial = onMaterial,
                layouts = PRESENTATION_LAYOUT_CHOICES,
                currentLayout = profile.layout,
                onLayout = onLayout,
                accents = PRESENTATION_ACCENT_CHOICES,
                currentAccent = profile.accentColor,
                onAccent = onAccent,
                displayFields = displayFields,
                privacyMasked = privacyMasked,
                globalPrivacyMask = globalPrivacyMask,
                onPrivacy = onPrivacy,
                canReset = canReset,
                onReset = onReset,
            )
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
    displayFields: List<String>,
    privacyMasked: Boolean,
    globalPrivacyMask: Boolean,
    onPrivacy: (Boolean) -> Unit,
    onLayout: (String) -> Unit,
    onAccent: (String) -> Unit,
    canReset: Boolean,
    onReset: () -> Unit,
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
                Column(Modifier.padding(16.dp)) { preview() }
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .testTagLocal(VTestIds.CUSTOMIZATION_LIBRARY),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("主题", color = PdigV2Colors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presets.forEach { preset ->
                    item(key = preset) {
                        ThemeTile(
                            kind = kind,
                            preset = preset,
                            selected = profile.themeId == preset,
                            onClick = { onPreset(preset) },
                            compact = true,
                        )
                    }
                }
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .testTagLocal(VTestIds.CUSTOMIZATION_INSPECTOR),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StudioInspector(
                rows = rows,
                materials = materials,
                currentMaterial = profile.material,
                onMaterial = onMaterial,
                layouts = PRESENTATION_LAYOUT_CHOICES,
                currentLayout = profile.layout,
                onLayout = onLayout,
                accents = PRESENTATION_ACCENT_CHOICES,
                currentAccent = profile.accentColor,
                onAccent = onAccent,
                displayFields = displayFields,
                privacyMasked = privacyMasked,
                globalPrivacyMask = globalPrivacyMask,
                onPrivacy = onPrivacy,
                canReset = canReset,
                onReset = onReset,
            )
        }
    }
}