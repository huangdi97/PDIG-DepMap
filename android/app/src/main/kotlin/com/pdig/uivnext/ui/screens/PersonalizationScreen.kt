package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VTouchTarget
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.LabelChip
import com.pdig.uivnext.ui.components.SectionHeader

/** 个性化：只改变本机显示与交互偏好，不改变基础设施事实。 */
@Composable
fun PersonalizationScreen(app: VAppState, breakpoint: MediaBreakpoint) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(pagePadding(breakpoint)),
        verticalArrangement = Arrangement.spacedBy(if (breakpoint == MediaBreakpoint.COMPACT) 16.dp else 18.dp),
    ) {
        if (breakpoint != MediaBreakpoint.COMPACT) {
            Text(if (app.screen == VScreen.SETTINGS) "设置" else "个性化", color = PdigV2Colors.TextPrimary, fontSize = pageTitleSize(breakpoint), fontWeight = FontWeight.Bold)
        }
        Text(
            "这些设置只改变你看到的界面，不会修改卡片、号码、依赖关系或变更记录。",
            color = PdigV2Colors.TextSecondary,
            fontSize = 13.sp,
        )

        SectionHeader("外观")
        AppearancePreview(app)
        ActionRow("卡片外观", "在每张卡片详情中单独定制") { app.navigate(VScreen.CARDS) }
        ActionRow("号码外观", "在号码详情中单独定制") { app.navigate(VScreen.NUMBERS) }

        SectionHeader("隐私")
        ToggleRow(
            label = "隐藏敏感信息",
            value = if (app.privacyMask) "已隐藏卡号、号码等敏感字段" else "敏感字段当前可见",
            enabled = app.privacyMask,
        ) { app.privacyMask = !app.privacyMask }

        SectionHeader("动效")
        PreferenceRow(
            "动效节奏",
            if (app.reduceMotion) "简化" else "标准",
            status = if (app.reduceMotion) "减弱" else "标准",
        )
        ToggleRow(
            label = "减弱动态效果",
            value = if (app.reduceMotion) "关闭地球自动旋转并简化界面动画" else "保留标准动效",
            enabled = app.reduceMotion,
        ) { app.reduceMotion = !app.reduceMotion }

        if (breakpoint != MediaBreakpoint.COMPACT) {
            SectionHeader("平板导航")
            ToggleRow(
                label = "展开侧栏",
                value = if (app.railExpanded) "显示完整导航名称" else "仅显示导航图标",
                enabled = app.railExpanded,
            ) { app.railExpanded = !app.railExpanded }
        }

        SectionHeader("首页")
        PreferenceRow("需要你处理", "存在必须处理事项时始终显示", status = "固定")
        PreferenceRow("进行中的变更", "有变更计划时显示当前阶段", status = "固定")
        ToggleRow(
            label = "即将到来",
            value = if (app.showUpcoming) "显示已知的到期与时间节点" else "已从首页隐藏",
            enabled = app.showUpcoming,
        ) { app.showUpcoming = !app.showUpcoming }

        SectionHeader("地区")
        PreferenceRow("地区分组", "按地理区域自动分组", status = "自动")

        SectionHeader("数据与来源")
        ActionRow(
            label = "数据源",
            value = "查看当前工作区覆盖范围、事实边界与已记录对象",
        ) { app.openUtility(VScreen.SOURCES) }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PdigV2Colors.PrimarySoft.copy(alpha = 0.72f),
            shape = RoundedCornerShape(VRadius.Md),
            border = BorderStroke(1.dp, PdigV2Colors.Primary.copy(alpha = 0.36f)),
        ) {
            Text(
                "重要事项不会因为个性化设置而被隐藏。",
                Modifier.padding(14.dp),
                color = PdigV2Colors.TextSecondary,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun AppearancePreview(app: VAppState) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTagLocal("pdig.personalization.visual-preview"),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Xl),
        border = BorderStroke(1.dp, PdigV2Colors.Primary.copy(alpha = 0.18f)),
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("亮色 · 当前视觉方向", color = PdigV2Colors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("浅色空间层级 · 真实地球 · 资产身份优先", color = PdigV2Colors.TextMuted, fontSize = 11.sp)
                }
                LabelChip("当前", highlight = true)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VisualPreviewTile("界面", "亮色", PdigV2Colors.SurfaceRaised, Modifier.weight(1f))
                VisualPreviewTile(
                    "地球",
                    if (app.reduceMotion) "真实地球 · 静态" else "真实地球 · 动态",
                    PdigV2Colors.GlobeDeep,
                    Modifier.weight(1f),
                    dark = true,
                )
                VisualPreviewTile("资产", "卡片 / 号码", PdigV2Colors.PrimarySoft, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun VisualPreviewTile(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
) {
    Surface(
        modifier = modifier,
        color = color,
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, color = if (dark) PdigV2Colors.GlobeTextSecondary else PdigV2Colors.TextMuted, fontSize = 10.sp)
            Text(value, color = if (dark) PdigV2Colors.GlobeTextPrimary else PdigV2Colors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun PreferenceRow(label: String, value: String, status: String? = null) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, color = PdigV2Colors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(value, color = PdigV2Colors.TextMuted, fontSize = 11.sp)
            }
            if (status != null) LabelChip(status)
        }
    }
}

@Composable
private fun ToggleRow(label: String, value: String, enabled: Boolean, onToggle: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = VTouchTarget.Min)
            .clickableLocal(onClick = onToggle),
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, color = PdigV2Colors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(value, color = PdigV2Colors.TextMuted, fontSize = 11.sp)
            }
            LabelChip(if (enabled) "已开启" else "已关闭", highlight = enabled)
        }
    }
}


@Composable
private fun ActionRow(label: String, value: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = VTouchTarget.Min)
            .clickableLocal(onClick = onClick),
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, color = PdigV2Colors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(value, color = PdigV2Colors.TextMuted, fontSize = 11.sp)
            }
            Text("查看 →", color = PdigV2Colors.PrimaryBright, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
