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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UI_CONTINUITY_REFERENCE_FINDINGS
import com.pdig.uivnext.demo.UiContinuityFinding
import com.pdig.uivnext.demo.UiContinuityFindingKind
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.demo.continuityFindingLabel
import com.pdig.uivnext.demo.continuityFindingSeverity
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
                        onClick = { app.openSecondaryObject(VScreen.SERVICE_DETAIL, service.id) },
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
        it.uniqueRecoveryPath == true &&
            (app.regionFilter == null || it.region == app.regionFilter)
    }
    val cards = UiVNextDemoFixture.cards.filter {
        it.status == "expiring_soon" && (app.regionFilter == null || it.region == app.regionFilter)
    }
    val emails = UiVNextDemoFixture.emails.filter {
        it.uniqueRecoveryPath == true &&
            (app.regionFilter == null || it.region == app.regionFilter)
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

        if (!app.emptyDemo) {
            SectionHeader("连续性 Findings（参考）")
            Text(
                "这里覆盖 v0.3 的完整 Finding 语法。内容来自隔离的 Synthetic Reference，" +
                    "不是由当前演示边数量临时推导；正式版必须由 Continuity engine 提供。",
                color = PdigV2Colors.TextMuted,
                fontSize = 10.sp,
            )
            UI_CONTINUITY_REFERENCE_FINDINGS.forEach { finding ->
                ContinuityFindingCard(
                    finding = finding,
                    onOpen = { openFindingTarget(app, finding) },
                )
            }
        }

        val knownWeaknessCount = numbers.size + cards.size + emails.size + devices.size + if (showPhoneMigration) 1 else 0
        InfraSummaryHero(
            value = knownWeaknessCount.toString(),
            label = "维护 / 迁移提醒",
            hint = "与上方 Continuity Findings 分开统计；这里只计算到期、设备复核与迁移阻塞",
            warning = knownWeaknessCount > 0,
        )

        if (!hasRecordedWeakness) {
            EmptyState(
                kind = EmptyKind.DEPENDENCIES,
                title = "当前地区没有已记录的维护 / 迁移提醒",
                description = "这只表示当前没有这类提醒；Continuity Findings 与未记录依赖仍需分别查看。",
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
                ) { app.openSecondaryObject(VScreen.EMAIL_DETAIL, email.id) }
            }
        }

        if (devices.isNotEmpty()) {
            SectionHeader("设备复核（" + devices.size + "）")
            devices.forEach { device ->
                WeaknessRow(
                    device.name + " 需要确认是否仍应保持信任",
                    "最近记录：" + device.lastSeen + "。未确认前不要把它视为可用恢复设备。",
                    PdigV2Colors.Warning,
                ) { app.openSecondaryObject(VScreen.DEVICE_DETAIL, device.id) }
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


@Composable
private fun ContinuityFindingCard(
    finding: UiContinuityFinding,
    onOpen: () -> Unit,
) {
    val critical = continuityFindingSeverity(finding.kind) == "critical"
    val tint = if (critical) PdigV2Colors.Critical else PdigV2Colors.Warning
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pdig.r27.finding.${finding.id}"),
        color = tint.copy(alpha = 0.075f),
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.22f)),
        onClick = onOpen,
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LabelChip(continuityFindingLabel(finding.kind), highlight = critical)
                Text(
                    if (critical) "已确认事实" else "需要核对",
                    color = tint,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(finding.title, color = PdigV2Colors.TextPrimary, fontSize = 13.sp,
                fontWeight = FontWeight.Bold)
            FindingFactLine("为什么", finding.why)
            FindingFactLine("确认依据", finding.confirmedBasis)
            FindingFactLine("仍未知", finding.unknowns)
            Text(
                "下一步 · " + finding.nextAction,
                color = PdigV2Colors.PrimaryText,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun FindingFactLine(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, color = PdigV2Colors.TextMuted, fontSize = 9.sp,
            modifier = Modifier.padding(top = 1.dp))
        Text(value, color = PdigV2Colors.TextSecondary, fontSize = 10.sp,
            lineHeight = 15.sp, modifier = Modifier.weight(1f))
    }
}

private fun openFindingTarget(app: VAppState, finding: UiContinuityFinding) {
    when (finding.targetKind) {
        "number" -> finding.targetId?.let(app::openNumber)
        "device" -> finding.targetId?.let { app.openSecondaryObject(VScreen.DEVICE_DETAIL, it) }
        "change" -> app.navigate(VScreen.CHANGE)
        else -> Unit
    }
}
