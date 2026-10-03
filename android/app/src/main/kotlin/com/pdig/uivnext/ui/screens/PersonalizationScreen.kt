package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.LabelChip
import com.pdig.uivnext.ui.components.SectionHeader

/** 个性化：只改变本机显示与交互偏好，不改变基础设施事实。 */
@Composable
fun PersonalizationScreen(app: VAppState) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("个性化", color = PdigV2Colors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(
            "这些设置只改变你看到的界面，不会修改卡片、号码、依赖关系或变更记录。",
            color = PdigV2Colors.TextSecondary,
            fontSize = 13.sp,
        )

        SectionHeader("外观")
        PreferenceRow("界面主题", "深色界面")
        PreferenceRow("卡片与号码外观", "在对应详情页中单独定制")

        SectionHeader("隐私")
        ToggleRow(
            label = "隐藏敏感信息",
            value = if (app.privacyMask) "已隐藏卡号、号码等敏感字段" else "敏感字段当前可见",
            enabled = app.privacyMask,
        ) { app.privacyMask = !app.privacyMask }

        SectionHeader("动效")
        ToggleRow(
            label = "减少动效",
            value = if (app.reduceMotion) "Globe 与界面动画已简化" else "保留平滑动效",
            enabled = app.reduceMotion,
        ) { app.reduceMotion = !app.reduceMotion }

        SectionHeader("平板导航")
        ToggleRow(
            label = "展开侧栏",
            value = if (app.railExpanded) "显示完整导航名称" else "仅显示导航图标",
            enabled = app.railExpanded,
        ) { app.railExpanded = !app.railExpanded }

        SectionHeader("首页")
        PreferenceRow("需要你处理", "存在必须处理事项时始终显示", status = "固定")
        PreferenceRow("进行中的变更", "有变更计划时显示当前阶段", status = "显示")
        PreferenceRow("即将到来", "显示已知的到期与时间节点", status = "显示")

        SectionHeader("地区")
        PreferenceRow("地区分组", "中国大陆 / 港澳 / 欧洲 / 北美 / 东南亚")

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
