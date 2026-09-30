package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.ui.components.SectionHeader
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.ACCENT_SWATCHES
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VType
import com.pdig.uivnext.ui.components.accentColorOf

/**
 * Customization Studio 三栏框架（G7/G9）：
 * LEFT 对象/卡片库（22%）→ CENTER 大尺寸实时卡面预览（46%，主角）→ RIGHT 属性编辑器（32%）。
 * 编辑对象 = PresentationProfile（本地偏好；绝不写 .depmap）。
 * 每次修改 live preview 即时重绘；保存 = 本地偏好语义。
 */
@Composable
fun StudioFrame(
    title: String,
    libraryItems: List<Pair<String, String>>,
    selectedLibraryId: String,
    onSelectLibrary: (String) -> Unit,
    presets: List<String>,
    materials: List<String>,
    layouts: List<String>,
    profile: PresentationProfile,
    onProfileChange: (PresentationProfile) -> Unit,
    preview: @Composable (String) -> Unit,
) {
    var saved by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(VSpacing.Xxl), verticalArrangement = Arrangement.spacedBy(VSpacing.Xxl)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = VType.PageTitle, color = PdigV2Colors.TextPrimary)
                Text("视觉预设 ≠ 语义角色 · 编辑只影响呈现层", style = VType.Secondary, color = PdigV2Colors.TextSecondary)
            }
            Surface(
                color = if (saved) PdigV2Colors.Positive.copy(alpha = 0.2f) else PdigV2Colors.Primary,
                shape = RoundedCornerShape(VRadius.Md),
                modifier = Modifier.clickable { saved = true },
            ) {
                Text(
                    if (saved) "已保存（本地偏好）" else "保存",
                    Modifier.padding(horizontal = VSpacing.Lg, vertical = VSpacing.Sm),
                    color = if (saved) PdigV2Colors.Positive else PdigV2Colors.CanvasDeep,
                    style = VType.Label,
                )
            }
        }
        Row(Modifier.fillMaxSize()) {
            // LEFT：对象库（22%）
            Column(
                Modifier
                    .weight(0.22f)
                    .fillMaxSize()
                    .testTagLocal(VTestIds.CUSTOMIZATION_LIBRARY),
                verticalArrangement = Arrangement.spacedBy(VSpacing.Sm),
            ) {
                SectionHeader("对象库")
                libraryItems.forEach { (id, label) ->
                    LibraryItem(label, selected = selectedLibraryId == id) { onSelectLibrary(id) }
                }
                Spacer(Modifier.height(VSpacing.Md))
                SectionHeader("预设")
                presets.forEach { preset ->
                    ChipRow(
                        label = preset,
                        selected = profile.themeId == preset,
                        onClick = { onProfileChange(profile.copy(themeId = preset, backgroundValue = preset)) },
                    )
                }
            }
            Spacer(Modifier.width(VSpacing.Lg))
            // CENTER：大尺寸实时预览（46%，主角）
            Column(
                Modifier
                    .weight(0.46f)
                    .fillMaxSize()
                    .testTagLocal(VTestIds.CUSTOMIZATION_PREVIEW),
            ) {
                SectionHeader("实时预览")
                Spacer(Modifier.height(VSpacing.Md))
                Surface(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(0.dp),
                    color = PdigV2Colors.Surface.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(VRadius.Xl),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                ) {
                    Box(Modifier.fillMaxSize().padding(VSpacing.Xxl), contentAlignment = Alignment.Center) {
                        preview(selectedLibraryId)
                    }
                }
                Spacer(Modifier.height(VSpacing.Sm))
                Text(
                    "素材全部来自 bundled local / procedural；不加载远程图片。",
                    color = PdigV2Colors.TextMuted,
                    style = VType.Meta,
                )
            }
            Spacer(Modifier.width(VSpacing.Lg))
            // RIGHT：属性编辑器（32%）
            Column(
                Modifier
                    .weight(0.32f)
                    .fillMaxSize()
                    .testTagLocal(VTestIds.CUSTOMIZATION_INSPECTOR)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(VSpacing.Sm),
            ) {
                SectionHeader("卡面设计")
                if (materials.isNotEmpty()) {
                    GroupLabel("材质")
                    Row(horizontalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
                        materials.forEach { m ->
                            ChipRow(label = m, selected = profile.material == m, compact = true) {
                                onProfileChange(profile.copy(material = m))
                            }
                        }
                    }
                }
                GroupLabel("布局")
                Row(horizontalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
                    layouts.forEach { l ->
                        ChipRow(label = l, selected = profile.layout == l, compact = true) {
                            onProfileChange(profile.copy(layout = l))
                        }
                    }
                }
                GroupLabel("强调色")
                Row(horizontalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
                    ACCENT_SWATCHES.forEach { (key, color) ->
                        SwatchDot(color, selected = profile.accentColor == key) {
                            onProfileChange(profile.copy(accentColor = key))
                        }
                    }
                }
                ToggleRow("敏感信息遮蔽", profile.maskSensitive) {
                    onProfileChange(profile.copy(maskSensitive = !profile.maskSensitive))
                }
                Spacer(Modifier.height(VSpacing.Sm))
                SectionHeader("内容信息")
                InfoRow("主题", profile.themeId)
                InfoRow("背景", profile.backgroundValue)
                InfoRow("遮蔽", if (profile.maskSensitive) "已开启" else "已关闭")
                Spacer(Modifier.height(VSpacing.Sm))
                SectionHeader("样式")
                InfoRow("圆角", "xl · 18px（token）")
                InfoRow("边框", "borderSubtle / borderStrong")
                Spacer(Modifier.height(VSpacing.Sm))
                SectionHeader("高级")
                Text(
                    "PresentationProfile 是本地 app 偏好，绝不写入 .depmap / PersonalReality；修改不影响依赖、证据与确认。",
                    style = VType.Meta,
                    color = PdigV2Colors.TextMuted,
                )
            }
        }
    }
}

@Composable
private fun LibraryItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = if (selected) PdigV2Colors.Primary.copy(alpha = 0.28f) else PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Md),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.BorderSubtle),
    ) {
        Text(
            label,
            Modifier.padding(horizontal = VSpacing.Md, vertical = VSpacing.Sm),
            color = if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.TextPrimary,
            style = VType.Label,
            maxLines = 1,
        )
    }
}

@Composable
private fun ChipRow(label: String, selected: Boolean, compact: Boolean = false, onClick: () -> Unit) {
    Surface(
        Modifier.clickable(onClick = onClick),
        color = if (selected) PdigV2Colors.Primary.copy(alpha = 0.28f) else PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Sm),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.BorderSubtle),
    ) {
        Text(
            label,
            Modifier.padding(horizontal = if (compact) VSpacing.Md else VSpacing.Lg, vertical = if (compact) 4.dp else 6.dp),
            color = if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.TextSecondary,
            style = VType.Label,
        )
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(text, style = VType.Label, color = PdigV2Colors.TextMuted)
}

@Composable
private fun SwatchDot(color: androidx.compose.ui.graphics.Color, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        color = color,
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 3.dp else 1.dp,
            if (selected) PdigV2Colors.TextPrimary else PdigV2Colors.BorderSubtle,
        ),
    ) {}
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Surface(
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Md),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    ) {
        Row(Modifier.padding(horizontal = VSpacing.Md, vertical = VSpacing.Sm).clickable(onClick = onToggle), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = PdigV2Colors.TextSecondary, style = VType.Secondary)
            Surface(color = if (checked) PdigV2Colors.Primary.copy(alpha = 0.3f) else PdigV2Colors.Surface, shape = RoundedCornerShape(VRadius.Sm)) {
                Text(
                    if (checked) "开" else "关",
                    Modifier.padding(horizontal = VSpacing.Md, vertical = 3.dp),
                    color = if (checked) PdigV2Colors.PrimaryBright else PdigV2Colors.TextMuted,
                    style = VType.Label,
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = PdigV2Colors.TextMuted, style = VType.Secondary)
        Text(value, color = PdigV2Colors.TextPrimary, style = VType.Secondary, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
    }
}
