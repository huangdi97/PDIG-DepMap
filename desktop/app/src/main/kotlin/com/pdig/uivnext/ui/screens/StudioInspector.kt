package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VType

/**
 * Studio 右侧 Inspector（PHASE 1E §20）：分组折叠（材质 / 背景 / 布局 / 信息 / 隐私），
 * 默认只展开当前编辑组，降低 cognitive load；accent = 6–8 curated swatches（28–32dp）
 * + 自定义框（§21），仅改 PresentationProfile。
 */
@Composable
fun StudioInspector(
    profile: PresentationProfile,
    onProfileChange: (PresentationProfile) -> Unit,
    materials: List<String>,
    layouts: List<String>,
    currentGroup: String = "卡面",
) {
    var openGroup by remember { mutableStateOf(currentGroup) }
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(VSpacing.Sm),
    ) {
        InspectorGroup("材质", openGroup == "材质", onToggle = { openGroup = if (openGroup == "材质") "" else "材质" }) {
            if (materials.isNotEmpty()) {
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
        }
        InspectorGroup("背景", openGroup == "背景", onToggle = { openGroup = if (openGroup == "背景") "" else "背景" }) {
            Row(horizontalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
                ChipRow(label = "预设背景", selected = profile.backgroundKind == "preset", compact = true) {
                    onProfileChange(profile.copy(backgroundKind = "preset", backgroundValue = profile.themeId))
                }
                ChipRow(label = "纯色", selected = profile.backgroundKind == "plain", compact = true) {
                    onProfileChange(profile.copy(backgroundKind = "plain", backgroundValue = ""))
                }
                ChipRow(label = "导入图片", selected = profile.backgroundKind == "imported", compact = true) {
                    // 导入背景（PHASE 1E §62）由宿主页面提供文件选择；此处仅切换 kind。
                    onProfileChange(profile.copy(backgroundKind = "imported"))
                }
            }
        }
        InspectorGroup("布局", openGroup == "布局", onToggle = { openGroup = if (openGroup == "布局") "" else "布局" }) {
            Row(horizontalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
                layouts.forEach { l ->
                    ChipRow(label = layoutLabel(l), selected = profile.layout == l, compact = true) {
                        onProfileChange(profile.copy(layout = l))
                    }
                }
            }
        }
        InspectorGroup("强调色", openGroup == "强调色", onToggle = { openGroup = if (openGroup == "强调色") "" else "强调色" }) {
            AccentSwatches(profile.accentColor) { accent ->
                onProfileChange(profile.copy(accentColor = accent))
            }
        }
        InspectorGroup("信息", openGroup == "信息", onToggle = { openGroup = if (openGroup == "信息") "" else "信息" }) {
            ToggleRow("隐藏部分信息（隐私遮蔽）", profile.maskSensitive) {
                onProfileChange(profile.copy(maskSensitive = !profile.maskSensitive))
            }
            InfoRow("Logo / 尾号 / 卡组织", if (profile.maskSensitive) "受遮蔽保护" else "可见")
            InfoRow("币种 / 地区", if (profile.layout == "minimal-content") "精简隐藏" else "显示")
        }
        InspectorGroup("隐私", openGroup == "隐私", onToggle = { openGroup = if (openGroup == "隐私") "" else "隐私" }) {
            Text(
                "外观设置只改变显示方式，不会修改你的基础设施关系或确认状态。",
                style = VType.Meta,
                color = PdigV2Colors.TextMuted,
            )
        }
    }
}

/** 6–8 个 curated swatch（28–32dp，选中 ring；§21）。 */
private val CURATED_ACCENTS: List<Pair<String, Color>> = listOf(
    "primary" to PdigV2Colors.Primary,
    "crimson" to PdigV2Colors.Critical,
    "warm" to PdigV2Colors.Warning,
    "coral" to PdigV2Colors.TerminatorLight,
    "cool" to PdigV2Colors.Unknown,
    "gold" to PdigV2Colors.NightCityLight,
    "jade" to PdigV2Colors.Positive,
    "navy" to PdigV2Colors.PrimarySoft,
)

@Composable
private fun AccentSwatches(selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
            CURATED_ACCENTS.forEach { (key, color) ->
                SwatchDot(color, selected = selected == key, onClick = { onSelect(key) }, size = 30.dp)
            }
        }
        // custom：手动输入 hex（有限验证，演示 seam）。
        Text(
            "自定义（可选）：输入 #RRGGBB",
            style = VType.Meta,
            color = PdigV2Colors.TextMuted,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
            ChipRow(label = selected.takeIf { it.startsWith("#") } ?: "自定义", selected = selected.startsWith("#"), compact = true) {
                // 保持当前自定义值；若未设置则用 swatch 首个色值。
                onSelect(if (selected.startsWith("#")) selected else CURATED_ACCENTS.first().first)
            }
        }
    }
}

@Composable
private fun InspectorGroup(
    title: String,
    open: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = VSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                title,
                style = VType.Label,
                color = if (open) PdigV2Colors.PrimaryBright else PdigV2Colors.TextSecondary,
            )
            Text(
                if (open) "收起 −" else "展开 +",
                style = VType.Meta,
                color = PdigV2Colors.TextMuted,
            )
        }
        if (open) {
            content()
            Spacer(Modifier.height(VSpacing.Sm))
        }
        androidx.compose.material3.HorizontalDivider(color = PdigV2Colors.BorderSubtle.copy(alpha = 0.4f), thickness = 1.dp)
    }
}