package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.ACCENT_SWATCHES
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VType
import com.pdig.uivnext.ui.components.SectionHeader

/**
 * Customization Studio 三栏框架（PHASE 1D §18–21）：
 * LEFT 对象库（thumbnail + nickname，选中小高亮）+ 主题（2 列大 visual tile，无 toggle dot）→
 * CENTER 大尺寸实时预览（预览占 center pane 70–78%，卡宽 @1920 ≈560–700px；
 * 中性空间舞台 + 卡本地光 + ground/reflection；tilt 随 reduced-motion 关闭）→
 * RIGHT 视觉化 inspector（卡面 / 材质 visual tile / 背景 / 布局 / 信息 / 隐私：
 * visual selector / swatch / thumbnail / toggle，不用文字 chip 堆砌）。
 * 编辑对象 = PresentationProfile（本地偏好；绝不写 .depmap）。
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
    reduceMotion: Boolean,
    thumbnail: @Composable (String) -> Unit,
    preview: @Composable (String) -> Unit,
    libraryThumbnail: @Composable (String) -> Unit = {},
) {
    var saved by remember { mutableStateOf(false) }
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
            // LEFT：对象库 + 主题（20%；紧凑、无整体滚动）
            Column(
                Modifier
                    .weight(0.20f)
                    .fillMaxSize()
                    .testTagLocal(VTestIds.CUSTOMIZATION_LIBRARY),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SectionHeader("对象")
                libraryItems.forEach { (id, label) ->
                    LibraryItem(
                        label = label,
                        selected = selectedLibraryId == id,
                        thumbnail = { libraryThumbnail(id) },
                        onClick = { onSelectLibrary(id) },
                    )
                }
                Spacer(Modifier.height(VSpacing.Sm))
                SectionHeader("主题")
                presets.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        pair.forEach { preset ->
                            ThemeThumb(
                                thumbnail = thumbnail,
                                preset = preset,
                                selected = profile.themeId == preset,
                                onClick = { onProfileChange(profile.copy(themeId = preset, backgroundValue = preset)) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(2 - pair.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Spacer(Modifier.width(VSpacing.Lg))
            // CENTER：大尺寸实时预览（主角；预览 70–78% pane 宽）
            Column(
                Modifier
                    .weight(0.55f)
                    .fillMaxSize()
                    .testTagLocal(VTestIds.CUSTOMIZATION_PREVIEW),
            ) {
                SectionHeader("实时预览")
                Spacer(Modifier.height(VSpacing.Md))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .drawBehind { drawPreviewStage() },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.75f)
                            .widthIn(max = 700.dp)
                            .graphicsLayer {
                                rotationX = if (reduceMotion) 0f else 5f
                                cameraDistance = 24f * density
                            },
                    ) {
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
            // RIGHT：视觉化 inspector（卡面 / 材质 / 背景 / 布局 / 信息 / 隐私）
            Column(
                Modifier
                    .weight(0.25f)
                    .fillMaxSize()
                    .testTagLocal(VTestIds.CUSTOMIZATION_INSPECTOR)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(VSpacing.Sm),
            ) {
                SectionHeader("卡面")
                if (materials.isNotEmpty()) {
                    GroupLabel("材质")
                    Row(horizontalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
                        materials.forEach { m ->
                            MaterialTile(
                                material = m,
                                profile = profile,
                                selected = profile.material == m,
                                onClick = { onProfileChange(profile.copy(material = m)) },
                            )
                        }
                    }
                }
                GroupLabel("背景")
                Row(horizontalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
                    ChipRow(label = "预设背景", selected = profile.backgroundKind == "preset", compact = true) {
                        onProfileChange(profile.copy(backgroundKind = "preset", backgroundValue = profile.themeId))
                    }
                    ChipRow(label = "纯色", selected = profile.backgroundKind == "plain", compact = true) {
                        onProfileChange(profile.copy(backgroundKind = "plain", backgroundValue = ""))
                    }
                }
                GroupLabel("布局")
                Row(horizontalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
                    layouts.forEach { l ->
                        ChipRow(label = layoutLabel(l), selected = profile.layout == l, compact = true) {
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
                Spacer(Modifier.height(VSpacing.Sm))
                SectionHeader("信息")
                ToggleRow("隐藏部分信息（隐私遮蔽）", profile.maskSensitive) {
                    onProfileChange(profile.copy(maskSensitive = !profile.maskSensitive))
                }
                InfoRow("Logo / 尾号 / 卡组织", if (profile.maskSensitive) "受遮蔽保护" else "可见")
                InfoRow("币种 / 地区", if (profile.layout == "minimal-content") "精简隐藏" else "显示")
                Spacer(Modifier.height(VSpacing.Sm))
                SectionHeader("隐私")
                Text(
                    "PresentationProfile 是本地 app 偏好，绝不写入 .depmap / PersonalReality；修改不影响依赖、证据与确认。",
                    style = VType.Meta,
                    color = PdigV2Colors.TextMuted,
                )
            }
        }
    }
}
