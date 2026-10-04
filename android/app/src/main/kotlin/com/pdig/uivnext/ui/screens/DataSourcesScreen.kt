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
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VTouchTarget
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.LabelChip
import com.pdig.uivnext.ui.components.SectionHeader

/** 数据源：展示当前工作区覆盖范围与事实边界，不伪造尚未接入的来源。 */
@Composable
fun DataSourcesScreen(app: VAppState) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("数据源", color = PdigV2Colors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(
            "PDIG 只把已记录并确认的信息当作事实；没有来源的数据保持未知。",
            color = PdigV2Colors.TextSecondary,
            fontSize = 13.sp,
        )

        SectionHeader("当前工作区")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PdigV2Colors.Surface.copy(alpha = 0.92f),
            shape = RoundedCornerShape(VRadius.Lg),
            border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("本机数据", color = PdigV2Colors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text("用于当前工作区的已记录基础设施", color = PdigV2Colors.TextMuted, fontSize = 12.sp)
                    }
                    LabelChip("本机优先", highlight = true)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SourceMetric(UiVNextDemoFixture.cards.size.toString(), "卡片", Modifier.weight(1f))
                    SourceMetric(UiVNextDemoFixture.numbers.size.toString(), "号码", Modifier.weight(1f))
                    SourceMetric(UiVNextDemoFixture.accounts.size.toString(), "账户", Modifier.weight(1f))
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SourceMetric(UiVNextDemoFixture.emails.size.toString(), "邮箱", Modifier.weight(1f))
            SourceMetric(UiVNextDemoFixture.devices.size.toString(), "设备", Modifier.weight(1f))
            SourceMetric(UiVNextDemoFixture.services.size.toString(), "服务", Modifier.weight(1f))
        }

        SectionHeader("事实边界")
        BoundaryRow("已确认", "已经记录并可用于依赖分析的信息。")
        BoundaryRow("未知", "没有记录的信息不会被推断为安全、存在或不存在。")
        BoundaryRow("外观", "卡面与号码面的个性化只改变显示，不改变事实。")

        SectionHeader("快速查看")
        JumpRow("基础设施总览", "查看地区、卡片、号码与当前关注项") { app.navigate(VScreen.OVERVIEW) }
        JumpRow("卡片", "查看当前记录的支付基础设施") { app.navigate(VScreen.CARDS) }
        JumpRow("号码", "查看通信身份与恢复依赖") { app.navigate(VScreen.NUMBERS) }
        JumpRow("账户 / 邮箱 / 设备", "查看身份入口、恢复邮箱和可信设备") { app.navigate(VScreen.ACCOUNTS) }
        JumpRow("薄弱点", "查看唯一恢复路径、到期与迁移阻塞") { app.navigate(VScreen.WEAKNESSES) }
    }
}

@Composable
private fun SourceMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = PdigV2Colors.SurfaceRaised, shape = RoundedCornerShape(VRadius.Md)) {
        Column(Modifier.padding(12.dp)) {
            Text(value, color = PdigV2Colors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(label, color = PdigV2Colors.TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun BoundaryRow(title: String, description: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.SurfaceRaised.copy(alpha = 0.82f),
        shape = RoundedCornerShape(VRadius.Md),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = PdigV2Colors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(description, color = PdigV2Colors.TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun JumpRow(title: String, description: String, onClick: () -> Unit) {
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
                Text(title, color = PdigV2Colors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(description, color = PdigV2Colors.TextMuted, fontSize = 11.sp)
            }
            Text("查看 →", color = PdigV2Colors.PrimaryBright, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
