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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.serviceKindLabelZh
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.EmptyKind
import com.pdig.uivnext.ui.components.EmptyState
import com.pdig.uivnext.ui.components.LabelChip
import com.pdig.uivnext.ui.components.SectionHeader

/** 基础设施二级：账户 / 邮箱 / 设备 / 服务 / 薄弱点。未知永远保持未知。 */
@Composable
fun SecondaryInfraScreen(app: VAppState, screen: VScreen) {
    when (screen) {
        VScreen.SERVICES -> ServicesScreen()
        VScreen.WEAKNESSES -> WeaknessesScreen(app)
        VScreen.ACCOUNTS -> AccountsScreen()
        VScreen.EMAILS -> EmailsScreen(app)
        VScreen.DEVICES -> DevicesScreen(app)
        else -> ServicesScreen()
    }
}

@Composable
private fun ServicesScreen() {
    val services = UiVNextDemoFixture.services
    InfraPage(
        title = "服务",
        subtitle = "查看已经记录的服务，以及它们所在的地区和承担的角色。",
    ) {
        SectionHeader("已记录（${services.size}）")
        services.groupBy { it.region }.forEach { (region, regionServices) ->
            Text(
                regionLabel(region),
                color = PdigV2Colors.TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            regionServices.forEach { service ->
                Surface(
                    color = PdigV2Colors.Surface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(VRadius.Md),
                    border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                service.name,
                                color = PdigV2Colors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                regionLabel(service.region),
                                color = PdigV2Colors.TextMuted,
                                fontSize = 12.sp,
                            )
                        }
                        LabelChip(serviceKindLabelZh(service.kind))
                    }
                }
            }
        }
        UnknownBoundaryNote("尚未记录的服务不会被推断为不存在。")
    }
}

@Composable
private fun WeaknessesScreen(app: VAppState) {
    InfraPage(
        title = "薄弱点",
        subtitle = "从已确认的恢复路径、到期状态和迁移计划中识别需要优先处理的风险。",
    ) {
        val recoveryOnly = UiVNextDemoFixture.numbers.filter { it.recoveryOnly }
        SectionHeader("唯一恢复路径（${recoveryOnly.size}）")
        recoveryOnly.forEach { number ->
            WeaknessRow(
                "${number.maskedNumber} 是账户的唯一恢复路径",
                "更换或注销前，必须先建立新的恢复方式。",
                PdigV2Colors.Critical,
            ) { app.openNumber(number.id) }
        }

        val expiring = UiVNextDemoFixture.cards.filter { it.status == "expiring_soon" }
        SectionHeader("即将到期（${expiring.size}）")
        expiring.forEach { card ->
            WeaknessRow(
                "${card.nickname} 将于 ${card.expiry} 到期",
                "绑定服务可能中断，建议提前完成换卡与重新绑定。",
                PdigV2Colors.Critical,
            ) { app.openCard(card.id) }
        }

        SectionHeader("迁移阻塞")
        WeaknessRow(
            "旧号码暂时不能停用",
            "新号码验证完成前，继续保留旧号码以避免恢复链路中断。",
            PdigV2Colors.Warning,
        ) { app.navigate(VScreen.CHANGE_PHONE) }

        SectionHeader("未知关系")
        UnknownBoundaryNote("没有记录的依赖仍然是未知，不会被标记为安全。")
    }
}

@Composable
private fun AccountsScreen() {
    val regions = UiVNextDemoFixture.regionSummaries().filter { it.accountCount > 0 }
    InfraPage(
        title = "账户",
        subtitle = "按地区查看当前已知的账户规模；未记录的登录和恢复关系保持未知。",
    ) {
        SectionHeader("地区分布")
        regions.forEach { region ->
            Surface(
                color = PdigV2Colors.Surface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(VRadius.Md),
                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            region.displayName,
                            color = PdigV2Colors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            "当前只记录了地区级数量",
                            color = PdigV2Colors.TextMuted,
                            fontSize = 12.sp,
                        )
                    }
                    LabelChip("${region.accountCount} 个账户")
                }
            }
        }
        UnknownBoundaryNote("账户明细尚未记录时，不推断登录名、验证方式或恢复路径。")
    }
}

@Composable
private fun EmailsScreen(app: VAppState) {
    InfraPage(
        title = "邮箱",
        subtitle = "邮箱可能承担登录、通知和恢复角色，因此未记录不等于没有依赖。",
    ) {
        EmptyState(
            kind = EmptyKind.DEPENDENCIES,
            title = "尚未记录邮箱",
            description = "当前没有可展示的邮箱。相关登录与恢复关系会一直保持未知，直到你记录它们。",
            primaryCta = "返回基础设施总览",
            onPrimary = { app.navigate(VScreen.OVERVIEW) },
            secondaryCta = "查看薄弱点",
            onSecondary = { app.navigate(VScreen.WEAKNESSES) },
        )
    }
}

@Composable
private fun DevicesScreen(app: VAppState) {
    InfraPage(
        title = "设备",
        subtitle = "设备可能承担登录、验证和恢复角色；未记录的设备关系保持未知。",
    ) {
        EmptyState(
            kind = EmptyKind.DEPENDENCIES,
            title = "尚未记录设备",
            description = "当前没有可展示的设备。验证器、恢复设备和可信终端不会被自动推断。",
            primaryCta = "返回基础设施总览",
            onPrimary = { app.navigate(VScreen.OVERVIEW) },
            secondaryCta = "查看薄弱点",
            onSecondary = { app.navigate(VScreen.WEAKNESSES) },
        )
    }
}

@Composable
private fun InfraPage(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(title, color = PdigV2Colors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = PdigV2Colors.TextSecondary, fontSize = 13.sp)
        content()
    }
}

@Composable
private fun UnknownBoundaryNote(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.SurfaceRaised.copy(alpha = 0.72f),
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Text(
            text,
            Modifier.padding(14.dp),
            color = PdigV2Colors.TextMuted,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun WeaknessRow(title: String, hint: String, accent: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickableLocal(onClick = onClick),
        color = accent.copy(alpha = 0.10f),
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = PdigV2Colors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(hint, color = PdigV2Colors.TextSecondary, fontSize = 12.sp)
        }
    }
}
