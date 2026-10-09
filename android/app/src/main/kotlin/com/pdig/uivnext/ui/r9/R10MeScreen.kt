package com.pdig.uivnext.ui.r9

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
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
import com.pdig.uivnext.demo.demoAttention
import com.pdig.uivnext.demo.demoCards
import com.pdig.uivnext.demo.demoNumbers
import com.pdig.uivnext.demo.demoRegions
import com.pdig.uivnext.demo.demoUpcoming
import com.pdig.uivnext.demo.demoChanges
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.ui.VAppState

/**
 * R11 consumer "Me" workspace. Not an empty settings list.
 * Every count comes from the active synthetic fixture; "unknown" remains unknown.
 */
@Composable
internal fun R10MeScreen(app: VAppState, onHelp: (() -> Unit)? = null) {
    val regions = app.demoRegions()
    val cards = app.demoCards()
    val numbers = app.demoNumbers()
    val accountCount = if(app.emptyDemo) 0 else UiVNextDemoFixture.accounts.size
    val serviceCount = if(app.emptyDemo) 0 else UiVNextDemoFixture.services.size
    val attention = app.demoAttention()
    val changes = app.demoChanges()
    val upcoming = app.demoUpcoming()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 13.dp, vertical = 12.dp)
            .testTag("pdig.r10.screen.me"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // One clear personal header and high-density factual summary.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(21.dp),
            color = Color.White,
            border = BorderStroke(1.dp, R9.Line),
        ) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    Surface(color = R9.Mist, shape = CircleShape,
                        modifier = Modifier.size(52.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Person, contentDescription = null,
                                tint = R9.Blue, modifier = Modifier.size(28.dp))
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("我的数字生活", color = R9.Ink, fontSize = 19.sp,
                            fontWeight = FontWeight.Bold)
                        Text("本机体验空间 · ${regions.size} 个已记录地区",
                            color = R9.Muted, fontSize = 11.sp)
                    }
                    R9Badge("本机", R9.Blue)
                }
                Surface(shape = RoundedCornerShape(13.dp), color = R9.Ice) {
                    Text("你的账户、号码与设备是一张连续性网络，而不只是资产清单。",
                        Modifier.fillMaxWidth().padding(11.dp),
                        fontSize = 11.sp, color = R9.Muted, lineHeight = 18.sp)
                }
            }
        }
        // Identity comes first, not an anonymous wall of aggregate KPIs.
        // These are already-recorded objects, never inferred security claims.
        val leadNumber = numbers.firstOrNull { it.role == "primary" } ?: numbers.firstOrNull()
        val leadEmail = if (app.emptyDemo) null
            else UiVNextDemoFixture.emails.firstOrNull { it.recoveryOnly }
                ?: UiVNextDemoFixture.emails.firstOrNull()
        val leadDevice = if (app.emptyDemo) null
            else UiVNextDemoFixture.devices.firstOrNull { "主设备" in it.roles }
                ?: UiVNextDemoFixture.devices.firstOrNull()
        R9SectionTitle("我的关键身份", "查看全部 →") { app.navigate(VScreen.INFRASTRUCTURE) }
        Row(
            Modifier.fillMaxWidth().testTag("pdig.r16.me.identity-workspace"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                modifier = Modifier.weight(1.12f).height(140.dp)
                    .clickable {
                        if (leadNumber != null) app.openNumber(leadNumber.id)
                        else app.navigate(VScreen.NUMBERS)
                    },
                color = Color(0xFF16376D),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(
                    Modifier.fillMaxSize().padding(13.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("▤   我的号码", color = Color(0xFFC7E3FF), fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold)
                    Text(
                        if (leadNumber != null)
                            app.numberDisplayNameForScreen(leadNumber.id, leadNumber.maskedNumber)
                        else "尚未记录",
                        color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        maxLines = 2,
                    )
                    Text(
                        if (leadNumber != null)
                            regionFlag(leadNumber.region) + " " + leadNumber.carrier + " · 查看恢复依赖 →"
                        else "添加或核对你的通信身份 →",
                        color = Color(0xFFBDDAF7), fontSize = 9.sp, maxLines = 2,
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                R16MeIdentityMini(
                    "恢复邮箱",
                    leadEmail?.maskedAddress ?: "尚未记录",
                    when {
                        leadEmail?.uniqueRecoveryPath == true -> "唯一恢复 · 需建立替代路径"
                        leadEmail?.recoveryOnly == true -> "恢复用途 · 唯一性未知"
                        else -> "查看已记录邮箱"
                    },
                    R9.Amber,
                ) { app.navigate(VScreen.EMAILS) }
                R16MeIdentityMini(
                    "常用设备", leadDevice?.name ?: "尚未记录",
                    leadDevice?.trust ?: "查看已记录设备",
                    R9.Green,
                ) { app.navigate(VScreen.DEVICES) }
            }
        }

        Surface(color = Color.White, shape = RoundedCornerShape(19.dp),
            border = BorderStroke(1.dp, R9.Line), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 7.dp, vertical = 13.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    R9Counter(cards.size, "银行卡", R9.Amber, "▣", Modifier.weight(1f).clickable { app.navigate(VScreen.CARDS) })
                    R9Counter(numbers.size, "号码", R9.Green, "▤", Modifier.weight(1f).clickable { app.navigate(VScreen.NUMBERS) })
                    R9Counter(accountCount, "账户", R9.Blue, "◎", Modifier.weight(1f).clickable { app.navigate(VScreen.ACCOUNTS) })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    R9Counter(serviceCount, "服务", Color(0xFF8A71DE), "✧",
                        Modifier.weight(1f).clickable { app.navigate(VScreen.SERVICES) })
                    R9Counter(regions.size, "地区", Color(0xFF437CCD), "◉",
                        Modifier.weight(1f).clickable { app.navigate(VScreen.OVERVIEW) })
                    R9Counter(attention.size, "待处理", R9.Rose, "!",
                        Modifier.weight(1f).clickable { app.navigate(VScreen.RECORDS) })
                }
            }
        }

        R9SectionTitle("隐私与个人偏好", "详细设置 →") { app.navigate(VScreen.SETTINGS) }
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { app.privacyMask = !app.privacyMask }
                .testTag("pdig.r10.me.privacy"),
            color = Color.White, shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, R9.Line),
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = R9.Blue,
                    modifier = Modifier.size(23.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("敏感信息遮蔽", color = R9.Ink,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("初始不遮蔽，开启和关闭由你决定",
                        color = R9.Muted, fontSize = 10.sp)
                }
                R9Badge(if(app.privacyMask) "已开启" else "已关闭",
                    if(app.privacyMask) R9.Blue else R9.Muted)
            }
        }

        R9SectionTitle("我的全球分布", "基础设施 →") {
            app.navigate(VScreen.INFRASTRUCTURE)
        }
        Surface(
            modifier = Modifier.fillMaxWidth().testTag("pdig.r11.me.regions"),
            color = Color.White, shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, R9.Line),
        ) {
            Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                if (regions.isEmpty()) {
                    Text("尚无已记录地区", fontSize = 12.sp, color = R9.Muted)
                }
                regions.take(5).forEach { region ->
                    Row(
                        Modifier.fillMaxWidth().defaultMinSize(minHeight = 38.dp)
                            .clickable {
                                app.selectRegion(region.regionCode)
                                app.navigate(VScreen.OVERVIEW)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Text(regionFlag(region.regionCode), fontSize = 20.sp)
                        Text(region.displayName, Modifier.weight(1f),
                            color = R9.Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("${region.cardCount} 卡 · ${region.phoneCount} 号码",
                            color = R9.Muted, fontSize = 10.sp)
                        Text("›", color = R9.Blue, fontSize = 18.sp)
                    }
                }
            }
        }

        R9SectionTitle("连续性概览", "查看记录 →") { app.navigate(VScreen.RECORDS) }
        Surface(color = R9.Ice, shape = RoundedCornerShape(17.dp),
            border = BorderStroke(1.dp, R9.Line), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    R9Badge("${changes.size} 项变更", R9.Blue)
                    Spacer(Modifier.width(7.dp))
                    R9Badge("${attention.size} 项待处理", R9.Rose)
                    Spacer(Modifier.width(7.dp))
                    R9Badge("${upcoming.size} 个时间节点", R9.Amber)
                }
                if (changes.isEmpty()) {
                    Text("目前没有已记录的进行中变更。", color = R9.Muted, fontSize = 11.sp)
                } else {
                    changes.take(2).forEach { change ->
                        Row(Modifier.fillMaxWidth()
                            .clickable { app.navigate(VScreen.CHANGE_PHONE) },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(change.title, Modifier.weight(1f),
                                color = R9.Ink, fontSize = 11.sp, maxLines = 2)
                            Text("继续查看 →", color = R9.Blue, fontSize = 10.sp)
                        }
                    }
                }
                if (attention.isNotEmpty()) {
                    Text("优先核对 · " + attention.first().title,
                        fontSize = 11.sp, color = R9.Rose, maxLines = 2,
                        modifier = Modifier.clickable { app.navigate(VScreen.RECORDS) })
                }
            }
        }

        R9SectionTitle("我的管理")
        Surface(shape = RoundedCornerShape(17.dp), color = Color.White,
            border = BorderStroke(1.dp, R9.Line)) {
            Column {
                R10MeAction("号码与名称", "自定义号码称呼，未命名显示号码", "numbers") {
                    app.navigate(VScreen.NUMBERS)
                }
                R10MeAction("卡面图片", "简单换图；卡片详情内完成", "cards") {
                    app.navigate(VScreen.CARDS)
                }
                R10MeAction("数据源与记录范围", "查看记录来源、关联和未知边界", "sources") {
                    app.navigate(VScreen.SOURCES)
                }
                R10MeAction("偏好设置", "隐私、显示、减弱动态效果", "settings") {
                    app.navigate(VScreen.SETTINGS)
                }
                if (onHelp != null) {
                    R10MeAction("新手引导", "重新查看使用说明", "guide") { onHelp() }
                }
            }
        }
        Surface(shape = RoundedCornerShape(14.dp), color = R9.Mist,
            modifier = Modifier.fillMaxWidth()) {
            Text("这是预览数据，不是实时账户同步。未知关系仍为未知，计划中的迁移不会伪装成已经完成。",
                Modifier.padding(13.dp),
                color = R9.Muted, fontSize = 11.sp, lineHeight = 18.sp)
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun R10MeAction(title: String, subtitle: String, tag: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 62.dp)
            .clickable(onClick = onClick).testTag("pdig.r10.me.$tag")
            .padding(horizontal = 15.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = R9.Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = R9.Muted, fontSize = 10.sp)
        }
        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null,
            tint = R9.Muted, modifier = Modifier.size(20.dp))
    }
}

/** Deliberately compact, meaningful digital identity rather than KPI-only rows. */
@Composable
private fun R16MeIdentityMini(
    title: String, value: String, hint: String, color: Color, action: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(66.dp).clickable { action() },
        color = Color.White, shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, R9.Line),
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(color, CircleShape))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = R9.Muted, fontSize = 9.sp)
                Text(value, color = R9.Ink, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, maxLines = 1)
                Text(hint, color = R9.Muted, fontSize = 8.sp, maxLines = 1)
            }
        }
    }
}
