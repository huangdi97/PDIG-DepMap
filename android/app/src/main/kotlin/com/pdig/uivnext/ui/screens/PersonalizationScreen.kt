package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.SectionHeader

/** Personalization Center（设置 → 个性化）：统一管理主题/密度/默认值/遮蔽/首页模块/地区分组/动效。 */
@Composable
fun PersonalizationScreen(app: VAppState) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("个性化", color = PdigV2Colors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("以下均为本地偏好（Presentation Layer）；不影响依赖/证据/确认，也不写入 .depmap 备份。", color = PdigV2Colors.TextSecondary, fontSize = 13.sp)

        SectionHeader("工作区外观")
        ToggleRow("Workspace 主题", "深空（vNext）· 系统跟随", enabled = true)

        SectionHeader("Globe 主题")
        ToggleRow("极慢空闲旋转", if (app.reduceMotion) "已关闭（减少动效）" else "开启（交互后暂停）", enabled = !app.reduceMotion)
        ToggleRow("弧线动画", if (app.reduceMotion) "静态" else "620ms 平滑", enabled = !app.reduceMotion)

        SectionHeader("导航密度")
        ToggleRow("导航栏展开宽度", if (app.railExpanded) "188px（展开）" else "80px（收起）", enabled = app.railExpanded) {
            app.railExpanded = !app.railExpanded
        }

        SectionHeader("呈现默认值")
        ToggleRow("卡片默认主题", "沿用各卡当前 preset，可在卡面定制中修改", enabled = false)
        ToggleRow("号码面默认主题", "Country / 可逐号码定制", enabled = false)

        SectionHeader("隐私遮蔽（全局）")
        ToggleRow("遮蔽 last4 / 号码 / 账户名", if (app.privacyMask) "已开启（截图/演示/公共场合推荐）" else "已关闭", enabled = app.privacyMask) {
            app.privacyMask = !app.privacyMask
        }

        SectionHeader("首页模块")
        listOf("需要你处理", "进行中的变更", "即将到来", "地区快捷访问").forEach { module ->
            Surface(
                color = PdigV2Colors.SurfaceRaised,
                shape = RoundedCornerShape(VRadius.Md),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(module, color = PdigV2Colors.TextSecondary, fontSize = 13.sp)
                    Text("显示 · 可上移/下移", color = PdigV2Colors.TextMuted, fontSize = 11.sp)
                }
            }
        }

        SectionHeader("地区分组")
        Text("按国家 / 按区域组（中国大陆 / 港澳 / 欧洲 / 北美 / 东南亚 / 自定义）", color = PdigV2Colors.TextSecondary, fontSize = 13.sp)

        SectionHeader("动效")
        ToggleRow("减少动效（Reduce Motion）", if (app.reduceMotion) "已开启" else "关闭", enabled = app.reduceMotion) {
            app.reduceMotion = !app.reduceMotion
        }

        Surface(color = PdigV2Colors.Critical.copy(alpha = 0.1f), shape = RoundedCornerShape(VRadius.Md), modifier = Modifier.fillMaxWidth()) {
            Text(
                "P0 提示：首页「需要你处理」模块在存在必须处理事项时不可隐藏。",
                Modifier.padding(14.dp),
                color = PdigV2Colors.TextPrimary,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, value: String, enabled: Boolean, onToggle: (() -> Unit)? = null) {
    Surface(
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Md),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, color = PdigV2Colors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(value, color = PdigV2Colors.TextMuted, fontSize = 11.sp)
            }
            Surface(
                Modifier.width(72.dp).padding(start = 8.dp),
                color = if (enabled) PdigV2Colors.Primary.copy(alpha = 0.3f) else PdigV2Colors.Surface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(VRadius.Sm),
                border = BorderStroke(1.dp, if (enabled) PdigV2Colors.PrimaryBright else PdigV2Colors.BorderSubtle),
            ) {
                Text(
                    if (enabled) "已开启" else "已关闭",
                    Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    color = if (enabled) PdigV2Colors.PrimaryBright else PdigV2Colors.TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                )
            }
            if (onToggle != null) {
                Text("点按切换", Modifier.padding(start = 8.dp).clickableLocal { onToggle() }, color = PdigV2Colors.PrimaryBright, fontSize = 11.sp)
            }
        }
    }
}
