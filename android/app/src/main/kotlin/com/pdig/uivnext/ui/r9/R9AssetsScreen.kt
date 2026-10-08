package com.pdig.uivnext.ui.r9

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.demoCards
import com.pdig.uivnext.demo.demoNumbers
import com.pdig.uivnext.model.UiVNextCard
import com.pdig.uivnext.model.UiVNextNumber
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.CardIdentityThumbnail
import com.pdig.uivnext.ui.components.NumberIdentityThumbnail

/** R9 financial identity list, structurally separate from R8's management rows. */
@Composable
internal fun R9CardsScreen(app: VAppState) {
    var kind by remember { mutableStateOf("all") }
    val scoped = app.demoCards().filter { app.regionFilter == null || it.region == app.regionFilter }
    val filtered = scoped.filter {
        when(kind) {
            "credit" -> it.type == "credit"
            "debit" -> it.type == "debit"
            "virtual" -> it.form == "virtual"
            else -> true
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 13.dp, vertical = 12.dp)
            .testTag("pdig.r9.screen.cards"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("全球支付卡片", fontSize = 16.sp, color = R9.Ink, fontWeight = FontWeight.Bold)
                Text("共 ${scoped.size} 张卡 · 独立资产与关联风险", color = R9.Muted, fontSize = 11.sp)
            }
            R9Badge("数据源 →", R9.Blue, Modifier.clickable { app.navigate(VScreen.SOURCES) })
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("all" to "全部（${scoped.size}）","credit" to "信用卡","debit" to "储蓄卡","virtual" to "虚拟卡")
                .forEach { (key,label) ->
                    Surface(
                        modifier = Modifier.clickable { kind = key },
                        color = if(key == kind) R9.Blue else Color.White,
                        border = BorderStroke(1.dp, if(key == kind) R9.Blue else R9.Line),
                        shape = RoundedCornerShape(11.dp),
                    ) {
                        Text(label, Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                            color = if(key == kind) Color.White else R9.Muted,
                            fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
        }
        if (filtered.isEmpty()) {
            Text("当前筛选没有已记录的卡片；未知不代表安全。",
                color = R9.Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 23.dp))
        } else {
            filtered.forEach { card -> R9CardRow(card, app) }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun R9CardRow(card: UiVNextCard, app: VAppState) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { app.openCard(card.id) }
            .testTag(VTestIds.CARD_ROW).testTag("pdig.r9.card.${card.id}"),
        color = Color.White, shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, R9.Line),
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            CardIdentityThumbnail(card, app.privacyMask,
                modifier = Modifier.width(114.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(card.nickname, color = R9.Ink, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                Text("${card.issuer} · ${card.last4}", color = R9.Muted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${regionFlag(card.region)} ${card.currency} · ${card.network}",
                        modifier = Modifier.weight(1f), color = R9.Muted, maxLines = 1,
                        fontSize = 9.sp)
                    R9Badge(if(card.status == "expiring_soon") "将到期" else "已记录",
                        if(card.status == "expiring_soon") R9.Amber else R9.Green)
                }
            }
            Text("›", color = R9.Muted, fontSize = 19.sp)
        }
    }
}

/** Numbers have their own communication identity; no fake bank-card surfaces. */
@Composable
internal fun R9NumbersScreen(app: VAppState) {
    var selected by remember { mutableStateOf("all") }
    val scoped = app.demoNumbers().filter { app.regionFilter == null || it.region == app.regionFilter }
    val filtered = scoped.filter {
        when(selected) {
            "primary" -> it.role == "primary"
            "recovery" -> it.recoveryOnly
            "esim" -> it.simKind == "eSIM"
            else -> true
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 13.dp, vertical = 12.dp)
            .testTag("pdig.r9.screen.numbers"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        R9SectionTitle("号码 · 通信身份", "查看地区 →") { app.navigate(VScreen.OVERVIEW) }
        Text("管理登录、短信验证码、恢复路径与关联服务",
            color = R9.Muted, fontSize = 11.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("all" to "全部","primary" to "主号码","recovery" to "恢复路径","esim" to "eSIM")
                .forEach { (key,label) ->
                    Surface(
                        modifier = Modifier.clickable { selected = key },
                        color = if(key == selected) R9.Blue else Color.White,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if(key == selected) R9.Blue else R9.Line),
                    ) {
                        Text(label, Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            color = if(key == selected) Color.White else R9.Muted,
                            fontSize = 11.sp)
                    }
                }
        }
        if(filtered.isEmpty()) Text("当前筛选暂无号码记录；未知不等于安全。",
            color = R9.Muted, fontSize = 12.sp)
        else filtered.forEach { number -> R9NumberRow(number, app) }
    }
}

@Composable
private fun R9NumberRow(number: UiVNextNumber, app: VAppState) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { app.openNumber(number.id) }
            .testTag(VTestIds.NUMBER_ROW).testTag("pdig.r9.number.${number.id}"),
        color = Color.White, shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, R9.Line),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            NumberIdentityThumbnail(number, app.privacyMask, Modifier.width(107.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(number.nickname, color = R9.Ink, fontWeight = FontWeight.Bold,
                    fontSize = 12.sp, maxLines = 1)
                Text(number.maskedNumber, color = R9.Ink, fontSize = 11.sp, maxLines = 1)
                Text("${regionFlag(number.region)} ${number.carrier} · ${number.simKind}",
                    color = R9.Muted, fontSize = 9.sp, maxLines = 1)
                if(number.recoveryOnly) R9Badge("恢复依赖 · 待核实", R9.Amber)
            }
            Text("›", color = R9.Muted, fontSize = 20.sp)
        }
    }
}
