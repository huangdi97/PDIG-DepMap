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
import com.pdig.uivnext.model.MediaBreakpoint
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
fun SecondaryInfraScreen(app: VAppState, screen: VScreen, breakpoint: MediaBreakpoint) {
    when (screen) {
        VScreen.SERVICES -> ServicesScreen(app, breakpoint)
        VScreen.WEAKNESSES -> WeaknessesScreen(app, breakpoint)
        VScreen.ACCOUNTS -> AccountsScreen(app, breakpoint)
        VScreen.EMAILS -> EmailsScreen(app, breakpoint)
        VScreen.DEVICES -> DevicesScreen(app, breakpoint)
        else -> ServicesScreen(app, breakpoint)
    }
}

@Composable
private fun ServicesScreen(app: VAppState, breakpoint: MediaBreakpoint) {
    val services = UiVNextDemoFixture.services
        .filter { app.regionFilter == null || it.region == app.regionFilter }

    InfraPage(
        title = "服务",
        subtitle = "查看已经记录的服务，以及它们所在的地区和承担的角色。",
        breakpoint = breakpoint,
    ) {
        RegionScopeBanner(app)
        InfraSummaryHero(
            value = services.size.toString(),
            label = "已记录服务",
            hint = if (app.regionFilter == null) "覆盖多个地区的服务与依赖入口" else "当前地区已记录的服务",
        )
        if (services.isEmpty()) {
            EmptyState(
                kind = EmptyKind.DEPENDENCIES,
                title = "当前地区没有服务记录",
                description = "没有记录的服务不会被推断为不存在，也不会被标记为无风险。",
                primaryCta = "查看全球",
                onPrimary = { app.clearRegion() },
                secondaryCta = "查看薄弱点",
                onSecondary = { app.navigate(VScreen.WEAKNESSES) },
            )
        } else {
            SectionHeader("已记录（" + services.size + "）")
            services.groupBy { it.region }.forEach { (region, regionServices) ->
                Text(
                    regionLabel(region),
                    color = PdigV2Colors.TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                regionServices.forEach { service ->
                    LightObjectCard(
                        title = service.name,
                        subtitle = regionLabel(service.region) + " · 已记录服务",
                        badge = serviceKindLabelZh(service.kind).take(1),
                        trailing = { LabelChip(serviceKindLabelZh(service.kind)) },
                    ) {
                        Text(
                            "此处只展示已记录关系；未记录的登录、恢复或支付依赖仍保持未知。",
                            color = PdigV2Colors.TextMuted,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
            UnknownBoundaryNote("尚未记录的服务不会被推断为不存在。")
        }
    }
}

@Composable
private fun WeaknessesScreen(app: VAppState, breakpoint: MediaBreakpoint) {
    val numbers = UiVNextDemoFixture.numbers.filter {
        it.recoveryOnly && (app.regionFilter == null || it.region == app.regionFilter)
    }
    val cards = UiVNextDemoFixture.cards.filter {
        it.status == "expiring_soon" && (app.regionFilter == null || it.region == app.regionFilter)
    }
    val emails = UiVNextDemoFixture.emails.filter {
        it.recoveryOnly && (app.regionFilter == null || it.region == app.regionFilter)
    }
    val devices = UiVNextDemoFixture.devices.filter {
        it.attention && (app.regionFilter == null || it.region == app.regionFilter)
    }
    val showPhoneMigration = app.regionFilter == null || app.regionFilter == "CN"
    val hasRecordedWeakness = numbers.isNotEmpty() || cards.isNotEmpty() || emails.isNotEmpty() ||
        devices.isNotEmpty() || showPhoneMigration

    InfraPage(
        title = "薄弱点",
        subtitle = "从已确认的恢复路径、到期状态和迁移计划中识别需要优先处理的风险。",
        breakpoint = breakpoint,
    ) {
        RegionScopeBanner(app)
        val knownWeaknessCount = numbers.size + cards.size + emails.size + devices.size + if (showPhoneMigration) 1 else 0
        InfraSummaryHero(
            value = knownWeaknessCount.toString(),
            label = "已知薄弱点",
            hint = "只统计已经记录并可验证的风险事实；未知关系仍保持未知",
            warning = knownWeaknessCount > 0,
        )

        if (!hasRecordedWeakness) {
            EmptyState(
                kind = EmptyKind.DEPENDENCIES,
                title = "当前地区没有已记录的薄弱点",
                description = "这只表示当前没有已记录的风险事实；未记录的依赖仍然保持未知。",
                primaryCta = "查看全球",
                onPrimary = { app.clearRegion() },
                secondaryCta = "返回基础设施",
                onSecondary = { app.navigate(VScreen.OVERVIEW) },
            )
            return@InfraPage
        }

        if (numbers.isNotEmpty()) {
            SectionHeader("唯一恢复路径（" + numbers.size + "）")
            numbers.forEach { number ->
                WeaknessRow(
                    number.maskedNumber + " 是账户的唯一恢复路径",
                    "更换或注销前，必须先建立新的恢复方式。",
                    PdigV2Colors.Critical,
                ) { app.openNumber(number.id) }
            }
        }

        if (cards.isNotEmpty()) {
            SectionHeader("即将到期（" + cards.size + "）")
            cards.forEach { card ->
                WeaknessRow(
                    card.nickname + " 将于 " + card.expiry + " 到期",
                    "绑定服务可能中断，建议提前完成换卡与重新绑定。",
                    PdigV2Colors.Critical,
                ) { app.openCard(card.id) }
            }
        }

        if (emails.isNotEmpty()) {
            SectionHeader("恢复邮箱（" + emails.size + "）")
            emails.forEach { email ->
                WeaknessRow(
                    email.maskedAddress + " 是唯一恢复邮箱",
                    "停用或更换前，先建立另一条恢复路径。",
                    PdigV2Colors.Critical,
                ) { app.navigate(VScreen.EMAILS) }
            }
        }

        if (devices.isNotEmpty()) {
            SectionHeader("设备复核（" + devices.size + "）")
            devices.forEach { device ->
                WeaknessRow(
                    device.name + " 需要确认是否仍应保持信任",
                    "最近记录：" + device.lastSeen + "。未确认前不要把它视为可用恢复设备。",
                    PdigV2Colors.Warning,
                ) { app.navigate(VScreen.DEVICES) }
            }
        }

        if (showPhoneMigration) {
            SectionHeader("迁移阻塞")
            WeaknessRow(
                "旧号码暂时不能停用",
                "新号码验证完成前，继续保留旧号码以避免恢复链路中断。",
                PdigV2Colors.Warning,
            ) { app.navigate(VScreen.CHANGE_PHONE) }
        }

        SectionHeader("未知关系")
        UnknownBoundaryNote("没有记录的依赖仍然是未知，不会被标记为安全。")
    }
}

@Composable
internal fun InfraPage(
    title: String,
    subtitle: String,
    breakpoint: MediaBreakpoint,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(pagePadding(breakpoint)),
        verticalArrangement = Arrangement.spacedBy(if (breakpoint == MediaBreakpoint.COMPACT) 12.dp else 14.dp),
    ) {
        if (breakpoint != MediaBreakpoint.COMPACT) {
            Text(title, color = PdigV2Colors.TextPrimary, fontSize = pageTitleSize(breakpoint), fontWeight = FontWeight.Bold)
        }
        Text(subtitle, color = PdigV2Colors.TextSecondary, fontSize = 13.sp)
        content()
    }
}

@Composable
internal fun InfraSummaryHero(
    value: String,
    label: String,
    hint: String,
    warning: Boolean = false,
) {
    val accent = if (warning) PdigV2Colors.Warning else PdigV2Colors.Primary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (warning) PdigV2Colors.Warning.copy(alpha = 0.10f) else PdigV2Colors.PrimarySoft.copy(alpha = 0.72f),
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                value,
                color = accent,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, color = PdigV2Colors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(hint, color = PdigV2Colors.TextMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
internal fun UnknownBoundaryNote(text: String) {
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
    LightObjectCard(
        title = title,
        subtitle = hint,
        badge = "!",
        accent = accent,
        onClick = onClick,
    ) {
        Text(
            "查看详情与下一步 →",
            color = accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
