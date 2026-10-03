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
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.demo.demoRegions
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.EmptyKind
import com.pdig.uivnext.ui.components.EmptyState
import com.pdig.uivnext.ui.components.LabelChip
import com.pdig.uivnext.ui.components.SectionHeader

/**
 * 基础设施二级屏：账户 / 邮箱 / 设备 / 服务 / 薄弱点。
 * 全部为 presentation 层派生（fixture），缺数据用 honest unknown 空态（未记录 ≠ 无风险）。
 * 不得伪造账户/邮箱/设备实体。
 */
@Composable
fun SecondaryInfraScreen(app: VAppState, screen: VScreen) {
    when (screen) {
        VScreen.SERVICES -> ServicesScreen()
        VScreen.WEAKNESSES -> WeaknessesScreen(app)
        VScreen.ACCOUNTS -> AccountsScreen()
        VScreen.EMAILS -> EmailsScreen()
        VScreen.DEVICES -> DevicesScreen()
        else -> ServicesScreen()
    }
}

/** 服务：全部已记录服务列表（name / region / kind），按地区分组计数。 */
@Composable
private fun ServicesScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("服务", color = PdigV2Colors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("已记录的服务与订阅（${UiVNextDemoFixture.services.size} 项；来自演示数据）。", color = PdigV2Colors.TextSecondary, fontSize = 13.sp)
        UiVNextDemoFixture.services.groupBy { it.region }.forEach { (region, services) ->
            SectionHeader("$region · ${services.size} 项")
            services.forEach { service ->
                Surface(
                    color = PdigV2Colors.Surface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(VRadius.Md),
                    border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(service.name, color = PdigV2Colors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("地区 ${service.region} · ${service.kind}", color = PdigV2Colors.TextMuted, fontSize = 12.sp)
                        }
                        LabelChip(service.kind)
                    }
                }
            }
        }
    }
}

/** 薄弱点：由已确认语义派生（唯一恢复路径 / 即将到期 / 迁移阻塞 / 未知依赖）。 */
@Composable
private fun WeaknessesScreen(app: VAppState) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("薄弱点", color = PdigV2Colors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("风险表面（由已确认的恢复路径 / 到期 / 迁移状态派生；未知 = 未知）。", color = PdigV2Colors.TextSecondary, fontSize = 13.sp)

        val recoveryOnly = UiVNextDemoFixture.numbers.filter { it.recoveryOnly }
        SectionHeader("唯一恢复路径（${recoveryOnly.size}）")
        recoveryOnly.forEach { n ->
            WeaknessRow("${n.maskedNumber} 是账户的唯一恢复路径", "更换/注销前必须先建立新的恢复方式", PdigV2Colors.Critical) {
                app.openNumber(n.id)
            }
        }

        val expiring = UiVNextDemoFixture.cards.filter { it.status == "expiring_soon" }
        SectionHeader("即将到期（${expiring.size}）")
        expiring.forEach { c ->
            WeaknessRow("${c.nickname} ${c.expiry} 到期", "绑定服务可能中断；建议提前更换卡后重新绑定", PdigV2Colors.Critical) {
                app.openCard(c.id)
            }
        }

        SectionHeader("迁移阻塞（1）")
        WeaknessRow("停用旧号码被阻止", "新手机号验证通过后才能停用（make-before-break）", PdigV2Colors.Warning) {
            app.navigate(VScreen.CHANGE_PHONE)
        }

        SectionHeader("未知依赖")
        Text(
            "尚未确认的依赖关系不会在此显示为「安全」：接入生产数据源后，未确认项应保持未知状态。",
            color = PdigV2Colors.TextMuted,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun WeaknessRow(title: String, hint: String, accent: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickableLocal(onClick = onClick),
        color = accent.copy(alpha = 0.10f),
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, color = PdigV2Colors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(hint, color = PdigV2Colors.TextSecondary, fontSize = 12.sp)
        }
    }
}

/** 账户：按地区呈现账户计数（账户对象未进入 fixture；诚实呈现派生信息）。 */
@Composable
private fun AccountsScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("账户", color = PdigV2Colors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("按地区的账户概览（数量来自地区聚合；账户对象尚未录入演示数据）。", color = PdigV2Colors.TextSecondary, fontSize = 13.sp)
        UiVNextDemoFixture.regionSummaries().filter { it.accountCount > 0 }.forEach { region ->
            Surface(
                color = PdigV2Colors.Surface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(VRadius.Md),
                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(region.displayName, color = PdigV2Colors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    LabelChip("${region.accountCount} 个账户")
                }
            }
        }
        Text(
            "账户明细（登录名 / 恢复方式 / 已验证依赖）将在生产数据源接入后展示；当前不伪造实体。",
            color = PdigV2Colors.TextMuted,
            fontSize = 12.sp,
        )
    }
}

/** 邮箱：无记录 → honest unknown 空态。 */
@Composable
private fun EmailsScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text("邮箱", color = PdigV2Colors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        EmptyState(
            kind = EmptyKind.DEPENDENCIES,
            title = "尚未记录邮箱账户",
            description = "演示数据中没有邮箱账户。没有记录 ≠ 没有风险：邮箱可能是登录 / 恢复路径。",
            primaryCta = "返回基础设施总览",
            onPrimary = { /* 保持本地演示壳；真实入口在生产数据源接入后提供 */ },
            secondaryCta = "查看薄弱点",
            onSecondary = { /* 保持本地演示壳 */ },
        )
    }
}

/** 设备：无记录 → honest unknown 空态。 */
@Composable
private fun DevicesScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text("设备", color = PdigV2Colors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        EmptyState(
            kind = EmptyKind.DEPENDENCIES,
            title = "尚未记录设备",
            description = "演示数据中没有设备。没有记录 ≠ 没有风险：设备可能是验证 / 恢复路径。",
            primaryCta = "返回基础设施总览",
            onPrimary = { /* 保持本地演示壳；真实入口在生产数据源接入后提供 */ },
            secondaryCta = "查看薄弱点",
            onSecondary = { /* 保持本地演示壳 */ },
        )
    }
}
