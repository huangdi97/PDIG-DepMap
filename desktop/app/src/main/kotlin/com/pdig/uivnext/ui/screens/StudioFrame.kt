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
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.ACCENT_SWATCHES
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VType
import com.pdig.uivnext.ui.components.SectionHeader

/**
 * Customization Studio 三栏框架（Review §9–§12）：
 * LEFT 对象库 + 预设（22%，紧凑无整体滚动；主题 = 确定性视觉缩略图，两列网格）→
 * CENTER 大尺寸实时预览（46%，主角：spotlight / floor / 环境辉光；卡占 72% 宽；tilt 随 reduced-motion 关闭）→
 * RIGHT 属性编辑器（32%，用户语言：材质 / 背景 / 布局 / 强调色 / 显示内容 / 隐私；删除开发者 token 行）。
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
            // LEFT：对象库 + 预设（22%；紧凑、无整体滚动，规避 Row 内 scroll 无限高约束）
            Column(
                Modifier
                    .weight(0.20f)
                    .fillMaxSize()
                    .testTagLocal(VTestIds.CUSTOMIZATION_LIBRARY),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SectionHeader("对象库")
                libraryItems.forEach { (id, label) ->
                    LibraryItem(label, selected = selectedLibraryId == id, compact = true) { onSelectLibrary(id) }
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
            // CENTER：大尺寸实时预览（46%，主角）
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
                ) {
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(0.9f)
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
            // RIGHT：属性编辑器（32%，用户语言）
            Column(
                Modifier
                    .weight(0.25f)
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
                            ChipRow(label = materialLabel(m), selected = profile.material == m, compact = true) {
                                onProfileChange(profile.copy(material = m))
                            }
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
                SectionHeader("显示内容")
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

/** 舞台绘制：spotlight（中心聚光）+ 卡片下方 soft floor 椭圆反射 + 环境辉光。 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPreviewStage() {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(PdigV2Colors.CanvasDeep.copy(alpha = 0.6f), PdigV2Colors.Canvas)))
    drawCircle(
        brush = Brush.radialGradient(
            listOf(PdigV2Colors.LocalIllum.copy(alpha = 0.40f), Color.Transparent),
            center = Offset(w * 0.5f, h * 0.44f),
            radius = w * 0.55f,
        ),
        radius = w * 0.55f,
        center = Offset(w * 0.5f, h * 0.44f),
    )
    drawOval(
        brush = Brush.radialGradient(
            listOf(PdigV2Colors.PrimaryBright.copy(alpha = 0.12f), Color.Transparent),
            center = Offset(w * 0.5f, h * 0.86f),
            radius = w * 0.30f,
        ),
        topLeft = Offset(w * 0.5f - w * 0.30f, h * 0.86f - w * 0.10f),
        size = androidx.compose.ui.geometry.Size(w * 0.60f, w * 0.20f),
    )
}

private fun materialLabel(m: String): String = when (m) {
    "matte" -> "哑光"
    "glass" -> "玻璃"
    "metal" -> "金属"
    "minimal" -> "极简"
    else -> m
}

private fun layoutLabel(l: String): String = when (l) {
    "emblem" -> "徽章"
    "minimal-content" -> "精简"
    else -> "标准"
}

/** 主题视觉缩略图（固定尺寸、固定渲染器；选中 = 描边高亮 + 圆点）。 */
@Composable
private fun ThemeThumb(
    thumbnail: @Composable (String) -> Unit,
    preset: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.clickable(onClick = onClick)) {
        thumbnail(preset)
        Surface(
            modifier = Modifier.align(Alignment.CenterEnd).size(20.dp).padding(end = 6.dp).clip(CircleShape).clickable(onClick = onClick),
            color = if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.SurfaceRaised,
            border = BorderStroke(1.dp, if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.BorderSubtle),
        ) {}
    }
}

@Composable
private fun LibraryItem(label: String, selected: Boolean, compact: Boolean = false, onClick: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = if (selected) PdigV2Colors.PrimaryBright.copy(alpha = 0.14f) else Color.Transparent,
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, if (selected) PdigV2Colors.PrimaryBright.copy(alpha = 0.3f) else PdigV2Colors.BorderSubtle),
    ) {
        Text(
            label,
            Modifier.padding(horizontal = VSpacing.Md, vertical = if (compact) 4.dp else VSpacing.Sm),
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
        color = if (selected) PdigV2Colors.PrimaryBright.copy(alpha = 0.14f) else PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Sm),
        border = BorderStroke(1.dp, if (selected) PdigV2Colors.PrimaryBright.copy(alpha = 0.3f) else PdigV2Colors.BorderSubtle),
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
private fun SwatchDot(color: Color, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        color = color,
        border = BorderStroke(
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
