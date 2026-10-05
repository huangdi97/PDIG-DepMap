package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.UiVNextAccount
import com.pdig.uivnext.model.UiVNextDevice
import com.pdig.uivnext.model.UiVNextEmail
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.EmptyKind
import com.pdig.uivnext.ui.components.EmptyState
import com.pdig.uivnext.ui.components.LabelChip
import com.pdig.uivnext.ui.components.SectionHeader

@Composable
internal fun AccountsScreen(app: VAppState) {
    val accounts = UiVNextDemoFixture.accounts.filter { app.regionFilter == null || it.region == app.regionFilter }
    InfraPage(
        title = "账户",
        subtitle = "账户是服务之上的身份入口：重点关注验证方式、恢复路径和跨地区依赖。",
    ) {
        RegionScopeBanner(app)
        InfraSummaryHero(
            value = accounts.size.toString(),
            label = "已记录账户",
            hint = "身份入口、验证方式与恢复路径",
        )
        if (accounts.isEmpty()) {
            ScopedInfrastructureEmpty(app, "当前地区没有账户记录", "没有记录的账户与恢复关系仍保持未知。")
        } else {
            SectionHeader("已记录（" + accounts.size + "）")
            accounts.forEach { AccountRow(it) }
            UnknownBoundaryNote("未记录的账户、登录名、验证器或恢复方式仍保持未知。")
        }
    }
}

@Composable
private fun AccountRow(account: UiVNextAccount) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.Surface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(
            1.dp,
            if (account.attention) PdigV2Colors.Warning.copy(alpha = 0.55f) else PdigV2Colors.BorderSubtle,
        ),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(account.name, color = PdigV2Colors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(account.provider + " · " + account.maskedIdentifier + " · " + regionLabel(account.region), color = PdigV2Colors.TextMuted, fontSize = 12.sp)
                }
                if (account.attention) LabelChip("恢复需关注", highlight = true) else LabelChip("已记录")
            }
            ChipLine(account.roles)
            Text("验证 · " + account.authMethods.joinToString(" / "), color = PdigV2Colors.TextSecondary, fontSize = 12.sp)
            Text("恢复 · " + account.recoveryRoute, color = if (account.attention) PdigV2Colors.Warning else PdigV2Colors.TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
internal fun EmailsScreen(app: VAppState) {
    val emails = UiVNextDemoFixture.emails.filter { app.regionFilter == null || it.region == app.regionFilter }
    InfraPage(
        title = "邮箱",
        subtitle = "邮箱可能同时承担登录、通知和恢复职责；唯一恢复邮箱必须显式识别。",
    ) {
        RegionScopeBanner(app)
        InfraSummaryHero(
            value = emails.size.toString(),
            label = "已记录邮箱",
            hint = "登录、通知与恢复职责",
        )
        if (emails.isEmpty()) {
            ScopedInfrastructureEmpty(app, "当前地区没有邮箱记录", "没有记录的邮箱与恢复关系仍保持未知。")
        } else {
            SectionHeader("已记录（" + emails.size + "）")
            emails.forEach { EmailRow(it) }
            UnknownBoundaryNote("没有记录的邮箱与恢复关系不会被推断为不存在。")
        }
    }
}

@Composable
private fun EmailRow(email: UiVNextEmail) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.Surface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, if (email.recoveryOnly) PdigV2Colors.Critical.copy(alpha = 0.45f) else PdigV2Colors.BorderSubtle),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(email.name, color = PdigV2Colors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(email.maskedAddress + " · " + email.provider + " · " + regionLabel(email.region), color = PdigV2Colors.TextMuted, fontSize = 12.sp)
                }
                LabelChip(if (email.recoveryOnly) "唯一恢复" else email.linkedServiceCount.toString() + " 项关联", highlight = email.recoveryOnly)
            }
            ChipLine(email.roles)
            Text(
                if (email.recoveryOnly) "更换或停用前，必须先建立另一条恢复路径。" else "已记录 " + email.linkedServiceCount + " 项服务关联。",
                color = if (email.recoveryOnly) PdigV2Colors.Critical else PdigV2Colors.TextSecondary,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
internal fun DevicesScreen(app: VAppState) {
    val devices = UiVNextDemoFixture.devices.filter { app.regionFilter == null || it.region == app.regionFilter }
    InfraPage(
        title = "设备",
        subtitle = "可信设备、验证器和恢复设备构成连续性链路；长期未使用的设备需要人工复核。",
    ) {
        RegionScopeBanner(app)
        InfraSummaryHero(
            value = devices.size.toString(),
            label = "已记录设备",
            hint = "可信终端、验证器与恢复设备",
        )
        if (devices.isEmpty()) {
            ScopedInfrastructureEmpty(app, "当前地区没有设备记录", "设备未出现于当前列表，不代表它没有登录或恢复权限。")
        } else {
            SectionHeader("已记录（" + devices.size + "）")
            devices.forEach { DeviceRow(it) }
            UnknownBoundaryNote("设备未出现于当前列表，不代表它没有登录或恢复权限。")
        }
    }
}

@Composable
private fun DeviceRow(device: UiVNextDevice) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.Surface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, if (device.attention) PdigV2Colors.Warning.copy(alpha = 0.55f) else PdigV2Colors.BorderSubtle),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(device.name, color = PdigV2Colors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(devicePlatformLabel(device) + " · " + regionLabel(device.region), color = PdigV2Colors.TextMuted, fontSize = 12.sp)
                }
                LabelChip(device.trust, highlight = device.attention)
            }
            ChipLine(device.roles)
            Text("最近记录 · " + device.lastSeen, color = if (device.attention) PdigV2Colors.Warning else PdigV2Colors.TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ChipLine(labels: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.take(3).forEach { LabelChip(it) }
    }
}


@Composable
private fun ScopedInfrastructureEmpty(app: VAppState, title: String, description: String) {
    EmptyState(
        kind = EmptyKind.DEPENDENCIES,
        title = title,
        description = description,
        primaryCta = if (app.regionFilter != null) "查看全球" else "返回基础设施总览",
        onPrimary = {
            if (app.regionFilter != null) app.clearRegion() else app.navigate(com.pdig.uivnext.model.VScreen.OVERVIEW)
        },
        secondaryCta = "查看薄弱点",
        onSecondary = { app.navigate(com.pdig.uivnext.model.VScreen.WEAKNESSES) },
    )
}


private fun devicePlatformLabel(device: UiVNextDevice): String = when {
    device.kind == "安全密钥" -> "安全密钥"
    device.kind.isNotBlank() && device.platform.isNotBlank() -> device.platform + " · " + device.kind
    device.platform.isNotBlank() -> device.platform
    else -> device.kind
}
