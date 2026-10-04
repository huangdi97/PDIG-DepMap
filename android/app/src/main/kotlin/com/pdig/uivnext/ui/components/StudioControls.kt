package com.pdig.uivnext.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.PresentationAccentChoice
import com.pdig.uivnext.model.hexColorOrNull
import com.pdig.uivnext.model.layoutLabelZh
import com.pdig.uivnext.model.materialLabelZh
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VTouchTarget

/** Studio 控制面板：每个可操作项必须真实进入 renderer。 */
@Composable
fun StudioInspector(
    rows: List<StudioPropRow>,
    materials: List<String>,
    currentMaterial: String,
    onMaterial: (String) -> Unit,
    layouts: List<String>,
    currentLayout: String,
    onLayout: (String) -> Unit,
    accents: List<PresentationAccentChoice>,
    currentAccent: String,
    onAccent: (String) -> Unit,
    displayFields: List<String>,
    privacyMasked: Boolean,
    globalPrivacyMask: Boolean,
    onPrivacy: (Boolean) -> Unit,
    canReset: Boolean,
    onReset: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle("当前外观")
        rows.forEach { row -> PropertyRow(row) }

        SectionTitle("材质")
        ChoiceStrip {
            materials.forEach { material ->
                ChoiceChip(
                    label = materialLabelZh(material),
                    selected = material == currentMaterial,
                    onClick = { onMaterial(material) },
                )
            }
        }

        SectionTitle("布局")
        ChoiceStrip {
            layouts.forEach { layout ->
                ChoiceChip(
                    label = layoutLabelZh(layout),
                    selected = layout == currentLayout,
                    onClick = { onLayout(layout) },
                )
            }
        }

        SectionTitle("强调")
        ChoiceStrip {
            accents.forEach { choice ->
                AccentChip(
                    choice = choice,
                    selected = choice.value == currentAccent,
                    onClick = { onAccent(choice.value) },
                )
            }
        }

        SectionTitle("显示内容")
        displayFields.forEach { DisplayInfoRow(it) }

        SectionTitle("隐私")
        InspectorToggle(
            label = "对象单独遮蔽",
            on = privacyMasked,
            onToggle = { onPrivacy(!privacyMasked) },
        )
        if (globalPrivacyMask) {
            Text(
                "全局隐私遮蔽已开启；当前预览仍会隐藏敏感字段。",
                color = PdigV2Colors.TextMuted,
                fontSize = 11.sp,
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = VTouchTarget.Min)
                .clickable(enabled = canReset) { if (canReset) onReset() },
            color = if (canReset) PdigV2Colors.SurfaceRaised else PdigV2Colors.SurfaceGlass,
            shape = RoundedCornerShape(VRadius.Md),
            border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
        ) {
            Text(
                "恢复原始外观",
                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                color = if (canReset) PdigV2Colors.TextSecondary else PdigV2Colors.TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = PdigV2Colors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun PropertyRow(row: StudioPropRow) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(row.label, color = PdigV2Colors.TextMuted, fontSize = 12.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            row.swatch?.let { swatch ->
                Box(Modifier.size(14.dp).background(swatch, RoundedCornerShape(3.dp)))
                Spacer(Modifier.width(6.dp))
            }
            Text(row.value, color = PdigV2Colors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ChoiceStrip(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        content = content,
    )
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        Modifier
            .defaultMinSize(minHeight = VTouchTarget.Min)
            .clickable(onClick = onClick),
        color = if (selected) PdigV2Colors.Primary.copy(alpha = 0.30f) else PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Sm),
        border = BorderStroke(1.dp, if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.BorderSubtle),
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.TextSecondary,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun AccentChip(choice: PresentationAccentChoice, selected: Boolean, onClick: () -> Unit) {
    Surface(
        Modifier
            .defaultMinSize(minHeight = VTouchTarget.Min)
            .clickable(onClick = onClick),
        color = if (selected) PdigV2Colors.Primary.copy(alpha = 0.30f) else PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Sm),
        border = BorderStroke(1.dp, if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.BorderSubtle),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            hexColorOrNull(choice.value)?.let { color ->
                Box(Modifier.size(12.dp).background(color, RoundedCornerShape(3.dp)))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                choice.label,
                color = if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.TextSecondary,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun DisplayInfoRow(label: String) {
    Surface(
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Md),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = PdigV2Colors.TextSecondary, fontSize = 13.sp)
            LabelChip("保留")
        }
    }
}

@Composable
private fun InspectorToggle(label: String, on: Boolean, onToggle: () -> Unit) {
    Surface(
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Md),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = VTouchTarget.Min)
            .clickable(onClick = onToggle),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = PdigV2Colors.TextSecondary, fontSize = 13.sp)
            Surface(
                color = if (on) PdigV2Colors.Primary.copy(alpha = 0.30f) else PdigV2Colors.Surface.copy(alpha = 0.50f),
                shape = RoundedCornerShape(VRadius.Sm),
            ) {
                Text(
                    if (on) "开" else "关",
                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = if (on) PdigV2Colors.PrimaryBright else PdigV2Colors.TextMuted,
                    fontSize = 11.sp,
                )
            }
        }
    }
}
