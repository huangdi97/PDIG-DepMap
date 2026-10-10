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
import com.pdig.app.BuildConfig
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VTouchTarget
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.LabelChip
import com.pdig.uivnext.ui.components.SectionHeader

/** 数据源：展示当前工作区覆盖范围与事实边界，不伪造尚未接入的来源。 */
@Composable
fun DataSourcesScreen(app: VAppState, breakpoint: MediaBreakpoint) {
    val previewReference = BuildConfig.FLAVOR == "preview"
    fun countOrBoundary(value: Int): String =
        if (previewReference) {
            if (app.emptyDemo) "0" else value.toString()
        } else {
            "—"
        }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(pagePadding(breakpoint)),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (breakpoint != MediaBreakpoint.COMPACT) {
            Text("数据源", color = PdigV2Colors.TextPrimary, fontSize = pageTitleSize(breakpoint), fontWeight = FontWeight.Bold)
        }
        Text(
            if (previewReference)
                "当前展示隔离的预览参考工作区，不代表真实账户同步状态；没有来源的数据保持未知。"
            else
                "当前界面尚未接入生产数据读模型；未接入的数据保持未知，不使用演示对象替代。",
            color = PdigV2Colors.TextSecondary,
            fontSize = 13.sp,
        )
        FactBoundaryHero()

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
                        Text(
                            if (previewReference) "预览参考数据" else "生产数据连接",
                            color = PdigV2Colors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                        Text(
                            if (previewReference) "隔离的参考数据，不读取真实用户账户"
                            else "尚未切换到 VNext 生产读模型",
                            color = PdigV2Colors.TextMuted,
                            fontSize = 12.sp,
                        )
                    }
                    LabelChip(if (previewReference) "演示数据" else "未接入", highlight = true)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SourceMetric(countOrBoundary(UiVNextDemoFixture.cards.size), "卡片", Modifier.weight(1f))
                    SourceMetric(countOrBoundary(UiVNextDemoFixture.numbers.size), "号码", Modifier.weight(1f))
                    SourceMetric(countOrBoundary(UiVNextDemoFixture.accounts.size), "账户", Modifier.weight(1f))
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SourceMetric(countOrBoundary(UiVNextDemoFixture.emails.size), "邮箱", Modifier.weight(1f))
            SourceMetric(countOrBoundary(UiVNextDemoFixture.devices.size), "设备", Modifier.weight(1f))
            SourceMetric(countOrBoundary(UiVNextDemoFixture.services.size), "服务", Modifier.weight(1f))
        }

        SectionHeader("事实边界")
        BoundaryRow("已确认", "已经记录并可用于依赖分析的信息。")
        BoundaryRow("未知", "没有记录的信息不会被推断为安全、存在或不存在。")
        BoundaryRow("外观", "卡面与号码面的个性化只改变显示，不改变事实。")

        SectionHeader("快速查看")
        JumpRow("基础设施总览", "查看地区、卡片、号码与当前关注项") { app.navigate(VScreen.OVERVIEW) }
        JumpRow("卡片", "查看当前记录的支付基础设施") { app.navigate(VScreen.CARDS) }
        JumpRow("号码", "查看通信身份与恢复依赖") { app.navigate(VScreen.NUMBERS) }
        JumpRow("账户", "查看账户身份、验证方式和恢复路径") { app.navigate(VScreen.ACCOUNTS) }
        JumpRow("邮箱", "查看登录、通知与恢复邮箱") { app.navigate(VScreen.EMAILS) }
        JumpRow("设备", "查看可信设备、验证器和恢复设备") { app.navigate(VScreen.DEVICES) }
        JumpRow("薄弱点", "查看唯一恢复路径、到期与迁移阻塞") { app.navigate(VScreen.WEAKNESSES) }
    }
}

@Composable
private fun FactBoundaryHero() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTagLocal("pdig.sources.boundary-hero"),
        color = PdigV2Colors.PrimarySoft.copy(alpha = 0.76f),
        shape = RoundedCornerShape(VRadius.Xl),
        border = BorderStroke(1.dp, PdigV2Colors.Primary.copy(alpha = 0.18f)),
        tonalElevation = 2.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BoundaryPillar("本机优先", "数据留在当前工作区", Modifier.weight(1f))
            BoundaryPillar("已确认", "才进入依赖分析", Modifier.weight(1f))
            BoundaryPillar("未知", "绝不自动推断为安全", Modifier.weight(1f))
        }
    }
}

@Composable
private fun BoundaryPillar(title: String, body: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = PdigV2Colors.PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(body, color = PdigV2Colors.TextSecondary, fontSize = 10.sp)
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
            Text("查看 →", color = PdigV2Colors.PrimaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
