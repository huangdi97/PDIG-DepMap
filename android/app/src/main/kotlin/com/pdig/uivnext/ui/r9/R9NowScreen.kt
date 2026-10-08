package com.pdig.uivnext.ui.r9

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.demo.demoAttention
import com.pdig.uivnext.demo.demoCards
import com.pdig.uivnext.demo.demoChanges
import com.pdig.uivnext.demo.demoNumbers
import com.pdig.uivnext.demo.demoRegions
import com.pdig.uivnext.globe.VNextGlobe
import com.pdig.uivnext.model.RegionPresentation
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.screens.arcPairs

/**
 * R9 phone NOW is a fresh composition; the R8 NowScreen is deliberately NOT used
 * in this preview route. All region/attention/metric facts come from the active
 * demo gate, including honest zero/empty states.
 */
@Composable
internal fun R9NowScreen(app: VAppState) {
    val regions = app.demoRegions()
    val attention = app.demoAttention()
    val changes = app.demoChanges()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 13.dp, vertical = 12.dp)
            .testTag("pdig.r9.screen.now"),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("早上好", color = R9.Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("你的数字基础设施 · 连接全球，触手可及", color = R9.Muted, fontSize = 11.sp)
            }
            if (attention.isNotEmpty()) {
                R9Badge("需要处理 ${attention.size}", R9.Rose)
            }
        }

        R9WorldStage(app, regions, app.demoCards().size, app.demoNumbers().size,
            if (app.emptyDemo) 0 else UiVNextDemoFixture.accounts.size,
            if (app.emptyDemo) 0 else UiVNextDemoFixture.services.size)

        R9SectionTitle("需要处理（${attention.size}）", if (attention.size > 1) "查看全部 →" else null) {
            app.navigate(VScreen.RECORDS)
        }
        if (attention.isEmpty()) {
            Surface(shape = RoundedCornerShape(15.dp), color = Color.White) {
                Text("暂无已记录事项；未知不等于安全。", Modifier.padding(16.dp),
                    color = R9.Muted, fontSize = 12.sp)
            }
        } else {
            val item = attention.first()
            Surface(
                modifier = Modifier.fillMaxWidth().clickable {
                    if (UiVNextDemoFixture.cardById(item.target) != null) app.openCard(item.target)
                    else if (app.demoNumbers().any { it.id == item.target }) app.openNumber(item.target)
                    else app.navigate(VScreen.RECORDS)
                }.testTag(VTestIds.NOW_ATTENTION),
                shape = RoundedCornerShape(15.dp),
                color = Color(0xFFFFF4F2),
                border = BorderStroke(1.dp, Color(0xFFF4C8CE)),
            ) {
                Row(Modifier.padding(horizontal = 13.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(30.dp).background(R9.Rose, RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center) {
                        Text("!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Text(item.title, Modifier.weight(1f), color = R9.Ink, fontSize = 12.sp,
                        lineHeight = 18.sp, maxLines = 3)
                    Text("›", color = R9.Muted, fontSize = 22.sp)
                }
            }
            if (attention.size > 1) {
                Text("还有 ${attention.size - 1} 项待处理 · 查看全部 →",
                    Modifier.fillMaxWidth().clickable { app.navigate(VScreen.RECORDS) }
                        .padding(vertical = 6.dp),
                    color = R9.Blue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        R9SectionTitle("进行中的变更", "查看全部 →") { app.navigate(VScreen.RECORDS) }
        if (changes.isEmpty()) {
            Text("没有记录正在执行的变更。", color = R9.Muted, fontSize = 12.sp)
        } else {
            changes.take(2).forEach { change ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { app.navigate(VScreen.CHANGE_PHONE) },
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, R9.Line),
                ) {
                    Row(Modifier.padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(change.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = R9.Ink)
                            Text("计划仍在进行 · 未验证的步骤不视为完成",
                                fontSize = 10.sp, color = R9.Muted)
                        }
                        Text("继续 →", fontSize = 11.sp, color = R9.Blue)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
internal fun R9WorldStage(
    app: VAppState,
    regions: List<RegionPresentation>,
    cards: Int,
    numbers: Int,
    accounts: Int,
    services: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(409.dp)
            .testTag(VTestIds.NOW_GLOBE)
            .testTag("pdig.r9.world.stage"),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, R9.Line),
        color = R9.Ice,
    ) {
        Column(Modifier.fillMaxSize().background(R9.Sky).padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Column(Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("你的全球数字基础设施", fontSize = 17.sp,
                    fontWeight = FontWeight.Bold, color = R9.Ink)
                Text(
                    if (regions.isEmpty()) "尚无地区信息" else "连接 ${regions.size} 个地区 · 一览全局",
                    fontSize = 11.sp, color = R9.Muted)
            }

            Box(Modifier.fillMaxWidth().weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(R9.World)
                .testTag("pdig.r9.world.hero")) {
                // Only the photographic planet is scaled, not the labels or counter rail.
                Box(Modifier.fillMaxSize().padding(horizontal = 31.dp)
                    .graphicsLayer(scaleX = 1.44f, scaleY = 1.44f)) {
                    VNextGlobe(
                        controller = app.globe,
                        regions = regions,
                        arcingPairs = if (app.emptyDemo) emptyList() else arcPairs(),
                        reduceMotion = app.reduceMotion,
                    )
                }
                val named = regions.associateBy { it.regionCode }
                listOf(
                    Triple("US", Alignment.TopStart, 16.dp),
                    Triple("GB", Alignment.TopEnd, 6.dp),
                    Triple("CN", Alignment.CenterEnd, 0.dp),
                    Triple("HK", Alignment.BottomStart, 27.dp),
                    Triple("SG", Alignment.BottomEnd, 12.dp),
                ).forEach { (code, align, offset) ->
                    val region = named[code] ?: return@forEach
                    val position = when(align) {
                        Alignment.TopStart, Alignment.TopEnd -> Modifier.padding(top = offset)
                        Alignment.BottomStart, Alignment.BottomEnd -> Modifier.padding(bottom = offset)
                        else -> Modifier
                    }
                    R9RegionPill(region, position.align(align).padding(horizontal = 3.dp)) {
                        app.selectRegion(region.regionCode)
                        app.navigate(VScreen.OVERVIEW)
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth().height(59.dp),
                color = Color.White.copy(alpha = 0.97f),
                shape = RoundedCornerShape(17.dp),
                border = BorderStroke(1.dp, R9.Line),
            ) {
                Row(Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    R9Counter(cards, "银行卡", R9.Amber, "▣", Modifier.weight(1f))
                    R9Counter(numbers, "手机号", R9.Green, "▤", Modifier.weight(1f))
                    R9Counter(accounts, "账户", R9.Blue, "◎", Modifier.weight(1f))
                    R9Counter(services, "服务", Color(0xFFA47FFF), "✧", Modifier.weight(1f))
                }
            }
        }
    }
}
