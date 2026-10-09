package com.pdig.uivnext.ui.r9

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.serviceKindLabelZh
import com.pdig.uivnext.ui.VAppState

/** R9 preview-only object workspaces: each item comes from an existing fixture, never generated. */
@Composable
internal fun R9SecondaryScreen(app: VAppState, screen: VScreen) {
    val zero = app.emptyDemo
    val selectedRegion = app.regionFilter
    fun scoped(code: String) = selectedRegion == null || selectedRegion == code
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 13.dp, vertical = 12.dp)
            .testTag("pdig.r9.screen.${screen.route}"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        R9SectionTitle(screen.titleZh,
            if (selectedRegion == null) "全球" else "恢复全球 →") {
            app.clearRegion()
        }
        Text(
            when(screen) {
                VScreen.ACCOUNTS -> "验证方式与恢复路径 · 账户身份独立于银行卡"
                VScreen.EMAILS -> "邮箱用于登录、通知或恢复；唯一恢复邮箱必须明确标识"
                VScreen.DEVICES -> "验证器、可信终端和恢复设备构成连续性链路"
                VScreen.SERVICES -> "已记录的服务与关系，不推断未录入的依赖"
                else -> "从已记录的到期和恢复关系识别薄弱点"
            },
            color = R9.Muted, fontSize = 11.sp, lineHeight = 17.sp,
        )
        when(screen) {
            VScreen.ACCOUNTS -> {
                val items = if(zero) emptyList() else UiVNextDemoFixture.accounts.filter { scoped(it.region) }
                R9CounterBanner(items.size, "账户", "身份与恢复入口")
                items.forEach { a ->
                    R9ObjectRow(a.name, "${a.provider} · ${if (app.privacyMask) "标识已遮蔽" else a.maskedIdentifier}",
                        "验证：${a.authMethods.joinToString(" / ")}",
                        "恢复：${a.recoveryRoute}", regionFlag(a.region), a.provider.take(1),
                        if(a.attention) "恢复需关注" else "已记录",
                        if(a.attention) R9.Amber else R9.Green)
                }
                if(items.isEmpty()) R9UnknownEmpty("账户")
            }
            VScreen.EMAILS -> {
                val items = if(zero) emptyList() else UiVNextDemoFixture.emails.filter { scoped(it.region) }
                R9CounterBanner(items.size, "邮箱", "登录 · 通知 · 恢复")
                items.forEach { a ->
                    R9ObjectRow(a.name, "${if (app.privacyMask) "邮箱已遮蔽" else a.maskedAddress} · ${a.provider}",
                        "角色：${a.roles.joinToString(" / ")}",
                        "关联：${a.linkedServiceCount} 项", regionFlag(a.region), "@",
                        when {
                            a.uniqueRecoveryPath == true -> "唯一恢复"
                            a.recoveryOnly -> "恢复用途"
                            else -> "已记录"
                        },
                        if(a.uniqueRecoveryPath == true) R9.Rose
                        else if(a.recoveryOnly) R9.Amber else R9.Green)
                }
                if(items.isEmpty()) R9UnknownEmpty("邮箱")
            }
            VScreen.DEVICES -> {
                val items = if(zero) emptyList() else UiVNextDemoFixture.devices.filter { scoped(it.region) }
                R9CounterBanner(items.size, "设备", "可信设备与验证器")
                items.forEach { a ->
                    R9ObjectRow(a.name, "${a.platform} · ${a.kind}",
                        "角色：${a.roles.joinToString(" / ")}",
                        "最后记录：${a.lastSeen}", regionFlag(a.region), "▤",
                        a.trust, if(a.attention) R9.Amber else R9.Blue)
                }
                if(items.isEmpty()) R9UnknownEmpty("设备")
            }
            VScreen.SERVICES -> {
                val items = if(zero) emptyList() else UiVNextDemoFixture.services.filter { scoped(it.region) }
                R9CounterBanner(items.size, "服务", "支付、订阅与账号入口")
                items.forEach { a ->
                    val linkCount = UiVNextDemoFixture.relations.count { it.to == a.id }
                    R9ObjectRow(a.name, serviceKindLabelZh(a.kind),
                        "已记录关系：${linkCount} 条",
                        "未录入关系仍为未知", regionFlag(a.region), a.name.take(1),
                        "已记录", R9.Blue)
                }
                if(items.isEmpty()) R9UnknownEmpty("服务")
            }
            VScreen.WEAKNESSES -> {
                val riskyCards = if(zero) emptyList() else UiVNextDemoFixture.cards.filter { scoped(it.region) && it.status == "expiring_soon" }
                val recoveryPhones = if(zero) emptyList() else UiVNextDemoFixture.numbers.filter { scoped(it.region) && it.recoveryOnly }
                val recoveryEmails = if(zero) emptyList() else UiVNextDemoFixture.emails.filter { scoped(it.region) && it.recoveryOnly }
                val devices = if(zero) emptyList() else UiVNextDemoFixture.devices.filter { scoped(it.region) && it.attention }
                R9CounterBanner(riskyCards.size+recoveryPhones.size+recoveryEmails.size+devices.size,
                    "已记录关注项", "未知关系不包含在计数内")
                riskyCards.forEach { a ->
                    R9ObjectRow(a.nickname, a.issuer, "有效期：${a.expiry}",
                        "核对已记录扣款与绑定", regionFlag(a.region), "▣", "临近到期", R9.Amber)
                }
                recoveryPhones.forEach { a ->
                    R9ObjectRow(a.nickname, r9VisibleNumber(a.maskedNumber, app.privacyMask || app.savedPresentationProfile("phoneNumber", a.id)?.maskSensitive == true), "恢复路径：号码",
                        "更换前确认替代路径", regionFlag(a.region), "☎", "需要核对", R9.Amber)
                }
                recoveryEmails.forEach { a ->
                    R9ObjectRow(a.name, if (app.privacyMask) "邮箱已遮蔽" else a.maskedAddress, "恢复路径：邮箱",
                        "停用前确认替代路径", regionFlag(a.region), "@", "唯一恢复", R9.Rose)
                }
                devices.forEach { a ->
                    R9ObjectRow(a.name, a.platform, "设备状态：${a.trust}",
                        "检查恢复权限", regionFlag(a.region), "▤", "待核对", R9.Amber)
                }
                if(riskyCards.isEmpty()&&recoveryPhones.isEmpty()&&recoveryEmails.isEmpty()&&devices.isEmpty())
                    R9UnknownEmpty("薄弱点")
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { app.navigate(VScreen.CHANGE_PHONE) },
                    color = R9.Mist, shape = RoundedCornerShape(14.dp),
                ) { Text("查看更换手机号的影响分析 →", Modifier.padding(14.dp),
                        color = R9.Blue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
            }
            else -> {}
        }
        Surface(modifier = Modifier.fillMaxWidth(), color = R9.Mist, shape = RoundedCornerShape(14.dp)) {
            Text("当前仅展示已记录的演示对象。未记录 ≠ 不存在；未知 ≠ 安全。",
                modifier = Modifier.padding(13.dp), color = R9.Muted, fontSize = 11.sp, lineHeight = 18.sp)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun R9CounterBanner(count: Int, title: String, hint: String) {
    Surface(color = Color.White, shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, R9.Line), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(count.toString(), fontSize = 25.sp, fontWeight = FontWeight.Bold, color = R9.Blue)
            Column {
                Text("已记录${title}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = R9.Ink)
                Text(hint, color = R9.Muted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun R9UnknownEmpty(title: String) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, R9.Line)) {
        Text("当前地区没有已记录的${title}。这不代表不存在风险。",
            Modifier.padding(17.dp), fontSize = 12.sp, color = R9.Muted)
    }
}

@Composable
private fun R9ObjectRow(
    title: String,
    subtitle: String,
    meta: String,
    detail: String,
    flag: String,
    glyph: String,
    status: String,
    color: Color,
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White,
        shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, R9.Line)) {
        Row(Modifier.padding(horizontal = 11.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(color = color.copy(alpha=.13f), shape = RoundedCornerShape(11.dp)) {
                Box(Modifier.size(41.dp), contentAlignment = Alignment.Center) {
                    Text(glyph, color = color, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = R9.Ink)
                Text(flag+" "+subtitle, color = R9.Muted, fontSize = 10.sp, maxLines = 2)
                Text(meta, color = R9.Ink, fontSize = 10.sp)
                Text(detail, color = R9.Muted, fontSize = 10.sp)
            }
            R9Badge(status, color)
        }
    }
}
