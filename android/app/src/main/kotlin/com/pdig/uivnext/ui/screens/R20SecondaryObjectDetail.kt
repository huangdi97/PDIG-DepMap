package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.demo.accountImpactLens
import com.pdig.uivnext.demo.deviceImpactLens
import com.pdig.uivnext.demo.emailImpactLens
import com.pdig.uivnext.demo.serviceImpactLens
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.serviceKindLabelZh
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.ObjectImpactLens

/**
 * R20 closes the focused-detail gap for the long-tail infrastructure objects
 * already present in the synthetic Android reference.
 *
 * The page is deliberately read-only. It exposes identity, recorded context,
 * confirmed/recorded relations and Impact Lens without inventing a Change CTA
 * for primitives that are not production-supported.
 */
@Composable
internal fun R20SecondaryObjectDetail(
    app: VAppState,
    screen: VScreen,
    breakpoint: MediaBreakpoint,
) {
    val objectId = app.selectedSecondaryObjectId
    if (objectId == null) {
        R20MissingDetail("没有选中的基础设施对象。")
        return
    }

    val maxWidth = when (breakpoint) {
        MediaBreakpoint.COMPACT -> 620.dp
        MediaBreakpoint.MEDIUM -> 760.dp
        MediaBreakpoint.EXPANDED -> 920.dp
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = maxWidth)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (breakpoint == MediaBreakpoint.COMPACT) 13.dp else 22.dp,
                    vertical = if (breakpoint == MediaBreakpoint.COMPACT) 12.dp else 20.dp,
                )
                .testTag("pdig.r20.secondary-detail.${screen.route}"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (screen) {
                VScreen.ACCOUNT_DETAIL -> AccountDetail(app, objectId)
                VScreen.EMAIL_DETAIL -> EmailDetail(app, objectId)
                VScreen.DEVICE_DETAIL -> DeviceDetail(app, objectId)
                VScreen.SERVICE_DETAIL -> ServiceDetail(app, objectId)
                else -> R20MissingDetail("当前页面不是可识别的对象详情。")
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun AccountDetail(app: VAppState, id: String) {
    val account = UiVNextDemoFixture.accounts.firstOrNull { it.id == id }
    if (account == null || app.emptyDemo) {
        R20MissingDetail("没有已记录的账户详情。")
        return
    }
    DetailHero(
        glyph = "人",
        title = account.name,
        subtitle = "${account.provider} · ${detailRegionFlag(account.region)}",
        status = if (account.attention) "需要核对" else "已记录",
        statusColor = if (account.attention) PdigV2Colors.Warning else PdigV2Colors.PrimaryBright,
    )
    DetailSection("账户身份", "登录入口、控制权限与恢复方式") {
        DetailFact("登录标识", if (app.privacyMask) "标识已遮蔽" else account.maskedIdentifier)
        DetailFact("角色", account.roles.joinToString(" / ").ifBlank { "未记录" })
        DetailFact("状态", account.status)
    }
    DetailSection("验证与恢复", "只展示当前明确记录的信息") {
        DetailFact("验证方式", account.authMethods.joinToString(" / ").ifBlank { "未记录" })
        DetailFact("恢复路径", account.recoveryRoute.ifBlank { "未记录" })
        DetailFact("唯一恢复", "未知")
        DetailTruthNote("记录了恢复方式，不等于已经证明它是唯一恢复路径或独立备用路径。")
    }
    ObjectImpactLens(impact = accountImpactLens(id))
    UnsupportedChangeNote()
}

@Composable
private fun EmailDetail(app: VAppState, id: String) {
    val email = UiVNextDemoFixture.emails.firstOrNull { it.id == id }
    if (email == null || app.emptyDemo) {
        R20MissingDetail("没有已记录的邮箱详情。")
        return
    }
    DetailHero(
        glyph = "@",
        title = email.name,
        subtitle = "${email.provider} · ${detailRegionFlag(email.region)}",
        status = when {
            email.uniqueRecoveryPath == true -> "唯一恢复"
            email.recoveryOnly -> "恢复用途"
            else -> "已记录"
        },
        statusColor = when {
            email.uniqueRecoveryPath == true -> PdigV2Colors.Critical
            email.recoveryOnly -> PdigV2Colors.Warning
            else -> PdigV2Colors.PrimaryBright
        },
    )
    DetailSection("通信 / 恢复身份", "登录、通知与恢复职责") {
        DetailFact("邮箱", if (app.privacyMask) "邮箱已遮蔽" else email.maskedAddress)
        DetailFact("角色", email.roles.joinToString(" / ").ifBlank { "未记录" })
        DetailFact("已记录关联", "${email.linkedServiceCount} 项")
        DetailFact(
            "恢复语义",
            when {
                email.uniqueRecoveryPath == true -> "已确认唯一恢复路径"
                email.recoveryOnly -> "恢复用途 · 唯一性未知"
                else -> "恢复唯一性未知"
            },
        )
    }
    ObjectImpactLens(impact = emailImpactLens(id))
    UnsupportedChangeNote()
}

@Composable
private fun DeviceDetail(app: VAppState, id: String) {
    val device = UiVNextDemoFixture.devices.firstOrNull { it.id == id }
    if (device == null || app.emptyDemo) {
        R20MissingDetail("没有已记录的设备详情。")
        return
    }
    DetailHero(
        glyph = "▤",
        title = device.name,
        subtitle = "${device.platform} · ${device.kind} · ${detailRegionFlag(device.region)}",
        status = if (device.attention) "待检查" else device.trust,
        statusColor = if (device.attention) PdigV2Colors.Warning else PdigV2Colors.PrimaryBright,
    )
    DetailSection("设备身份", "设备、验证与恢复权限") {
        DetailFact("平台", device.platform)
        DetailFact("类型", device.kind)
        DetailFact("角色", device.roles.joinToString(" / ").ifBlank { "未记录" })
        DetailFact("已记录状态", device.trust)
        DetailFact("最后记录", device.lastSeen.ifBlank { "未记录" })
    }
    DetailSection("连续性边界", "设备状态本身不能证明恢复路径独立") {
        DetailFact("唯一恢复", "未知")
        DetailFact("独立备用方式", "未记录")
        DetailTruthNote("受信任设备、验证器和恢复设备可能共享同一故障域；这里不根据数量推断独立性。")
    }
    ObjectImpactLens(impact = deviceImpactLens(id))
    UnsupportedChangeNote()
}

@Composable
private fun ServiceDetail(app: VAppState, id: String) {
    val service = UiVNextDemoFixture.services.firstOrNull { it.id == id }
    if (service == null || app.emptyDemo) {
        R20MissingDetail("没有已记录的服务详情。")
        return
    }
    val incoming = UiVNextDemoFixture.relations.filter { it.to == id }
    DetailHero(
        glyph = service.name.take(1),
        title = service.name,
        subtitle = "${serviceKindLabelZh(service.kind)} · ${detailRegionFlag(service.region)}",
        status = "已记录",
        statusColor = PdigV2Colors.PrimaryBright,
    )
    DetailSection("服务身份", "服务与已记录依赖入口") {
        DetailFact("服务类型", serviceKindLabelZh(service.kind))
        DetailFact("地区", detailRegionName(service.region))
        DetailFact("已记录关系", "${incoming.size} 条")
    }
    DetailSection("谁依赖它", "只列出当前已经记录的关系") {
        if (incoming.isEmpty()) {
            DetailTruthNote("没有已记录关系；这不代表真实世界中不存在依赖。")
        } else {
            incoming.forEach { relation ->
                val source = UiVNextDemoFixture.cards.firstOrNull { it.id == relation.from }?.nickname
                    ?: UiVNextDemoFixture.numbers.firstOrNull { it.id == relation.from }?.nickname
                    ?: relation.from
                DetailFact(source, detailRelationLabel(relation.kind))
            }
        }
    }
    ObjectImpactLens(impact = serviceImpactLens(id))
    UnsupportedChangeNote()
}

@Composable
private fun DetailHero(
    glyph: String,
    title: String,
    subtitle: String,
    status: String,
    statusColor: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Xl),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                color = statusColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(VRadius.Lg),
            ) {
                Box(
                    Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(glyph, color = statusColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    title,
                    color = PdigV2Colors.TextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    color = PdigV2Colors.TextMuted,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                color = statusColor.copy(alpha = 0.10f),
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(
                    status,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    color = statusColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Xl),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(
            Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = PdigV2Colors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = PdigV2Colors.TextMuted, fontSize = 9.sp)
            }
            content()
        }
    }
}

@Composable
private fun DetailFact(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, color = PdigV2Colors.TextMuted, fontSize = 11.sp)
        Text(
            value,
            modifier = Modifier.widthIn(max = 520.dp),
            color = PdigV2Colors.TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DetailTruthNote(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Md),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
            color = PdigV2Colors.TextSecondary,
            fontSize = 10.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun UnsupportedChangeNote() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.PrimarySoft.copy(alpha = 0.55f),
        shape = RoundedCornerShape(VRadius.Lg),
    ) {
        Text(
            "当前只提供影响查看。没有已支持的可执行变更场景时，不提前显示“开始变更”入口。",
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            color = PdigV2Colors.TextSecondary,
            fontSize = 10.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun R20MissingDetail(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = PdigV2Colors.TextMuted, fontSize = 12.sp)
    }
}

private fun detailRelationLabel(kind: String): String = when (kind) {
    "funding" -> "支付来源"
    "authenticates" -> "认证"
    "twoFA" -> "双重验证"
    else -> "已记录关系"
}

private fun detailRegionName(code: String): String = when (code) {
    "CN" -> "中国大陆"
    "HK" -> "香港"
    "MO" -> "澳门"
    "GB" -> "英国"
    "US" -> "美国"
    "SG" -> "新加坡"
    else -> code
}

private fun detailRegionFlag(code: String): String = when (code) {
    "CN" -> "🇨🇳 中国大陆"
    "HK" -> "🇭🇰 香港"
    "MO" -> "🇲🇴 澳门"
    "GB" -> "🇬🇧 英国"
    "US" -> "🇺🇸 美国"
    "SG" -> "🇸🇬 新加坡"
    else -> "🌐 $code"
}
