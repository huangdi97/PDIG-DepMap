package com.pdig.uivnext.ui.r9

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.demo.demoCards
import com.pdig.uivnext.demo.demoNumbers
import com.pdig.uivnext.demo.demoRegions
import com.pdig.uivnext.globe.VNextGlobe
import com.pdig.app.BuildConfig
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pdig.uivnext.model.RegionPresentation
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.ui.INFRA_ENTRIES
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.screens.arcPairs

/** Reference-first eight-category hub, search, world distribution; phone only. */
@Composable
internal fun R9InfrastructureScreen(app: VAppState) {
    val regions = app.demoRegions()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 13.dp, vertical = 12.dp)
            .testTag("pdig.r9.screen.infrastructure"),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Text("管理你的全球数字基础设施", color = R9.Muted, fontSize = 12.sp)
        app.regionFilter?.let { selected ->
            val regionName = regions.firstOrNull { it.regionCode == selected }?.displayName
                ?: selected
            Surface(
                modifier = Modifier.fillMaxWidth()
                    .testTag("pdig.r9.infrastructure.selected-region"),
                color = R9.Mist,
                shape = RoundedCornerShape(13.dp),
                border = BorderStroke(1.dp, R9.Line),
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("已选地区：$regionName", color = R9.Ink,
                        fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text("查看全球 →", color = R9.Blue, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { app.clearRegion() }
                            .testTag("pdig.r9.infrastructure.clear-region"))
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().height(45.dp).clickable { app.navigate(VScreen.SEARCH) }
                .testTag("pdig.r9.infrastructure.search"),
            color = Color.White, shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, R9.Line),
        ) {
            Row(Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = R9.Muted, modifier = Modifier.size(17.dp))
                Text("搜索卡片、号码、账户、服务...", Modifier.weight(1f), color = R9.Muted, fontSize = 11.sp)
                Text("全部", color = R9.Blue, fontSize = 10.sp)
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().testTag("pdig.r9.infrastructure.categories"),
            color = Color.White, shape = RoundedCornerShape(21.dp),
            border = BorderStroke(1.dp, R9.Line),
        ) {
            Column(Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("基础设施", fontSize = 15.sp, color = R9.Ink, fontWeight = FontWeight.Bold)
                    Text("8 类资产", fontSize = 11.sp, color = R9.Blue)
                }
                INFRA_ENTRIES.chunked(4).forEach { chunk ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        chunk.forEach { e ->
                            val accent = when(e.screen) {
                                VScreen.CARDS -> Color(0xFFF78B4D)
                                VScreen.NUMBERS -> Color(0xFF19B89E)
                                VScreen.ACCOUNTS -> Color(0xFF438BEA)
                                VScreen.EMAILS -> Color(0xFF8A79E6)
                                VScreen.DEVICES -> Color(0xFF547FC9)
                                VScreen.SERVICES -> Color(0xFFEDA24F)
                                VScreen.WEAKNESSES -> Color(0xFFE37078)
                                else -> R9.Blue
                            }
                            Column(
                                Modifier.weight(1f).clickable { app.navigate(e.screen) }
                                    .padding(vertical = 3.dp).testTag("pdig.r9.category.${e.screen.route}"),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Surface(
                                    modifier = Modifier.size(39.dp),
                                    color = if(e.screen == VScreen.OVERVIEW) accent else accent.copy(alpha = .13f),
                                    shape = RoundedCornerShape(13.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(e.icon, contentDescription = null, modifier = Modifier.size(22.dp),
                                            tint = if(e.screen == VScreen.OVERVIEW) Color.White else accent)
                                    }
                                }
                                Text(e.screen.titleZh, color = R9.Ink, fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(r9CategoryCount(e.screen, app), color = R9.Muted, fontSize = 9.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        R9RegionDistribution(app, regions)
        R9SectionTitle("地区", "查看卡片 →") { app.navigate(VScreen.CARDS) }
        if(regions.isEmpty()) {
            Text("没有已记录地区，未知不等于安全。", color = R9.Muted, fontSize = 12.sp)
        } else {
            regions.take(6).chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { r ->
                        Surface(
                            modifier = Modifier.weight(1f).clickable {
                                app.selectRegion(r.regionCode)
                                app.navigate(VScreen.CARDS)
                            }, color = Color.White, shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, R9.Line),
                        ) {
                            Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(regionFlag(r.regionCode), fontSize = 16.sp)
                                Column {
                                    Text(r.displayName, fontWeight = FontWeight.SemiBold,
                                        color = R9.Ink, fontSize = 11.sp, maxLines = 1)
                                    Text("${r.cardCount} 张卡 / ${r.phoneCount} 号",
                                        color = R9.Muted, fontSize = 9.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                    if(pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun R9RegionDistribution(app: VAppState, regions: List<RegionPresentation>) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(224.dp).testTag(VTestIds.GLOBE_STAGE),
        color = Color.White, shape = RoundedCornerShape(21.dp),
        border = BorderStroke(1.dp, R9.Line),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("地区分布", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = R9.Ink)
                Text("${regions.size} 个地区", fontSize = 11.sp, color = R9.Muted)
            }
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.weight(.55f).fillMaxHeight()
                        .clip(RoundedCornerShape(17.dp)).background(R9.World)
                        .testTag("pdig.r9.infrastructure.world"),
                ) {
                    var gpuFailed by remember { mutableStateOf(false) }
                    if (BuildConfig.FLAVOR == "preview" && !gpuFailed) {
                        R12NativeEarth(
                            controller = app.globe, regions = regions,
                            links = if(app.emptyDemo) emptyList() else arcPairs(),
                            onFailure = { gpuFailed = true },
                        )
                    } else {
                        VNextGlobe(
                            controller = app.globe, regions = regions,
                            arcingPairs = if (app.emptyDemo) emptyList() else arcPairs(),
                            reduceMotion = app.reduceMotion,
                        )
                    }
                }
                Column(Modifier.weight(.45f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (regions.isEmpty()) Text("暂无地区", fontSize = 12.sp, color = R9.Muted)
                    regions.take(5).forEach { r ->
                        Row(Modifier.fillMaxWidth().height(29.dp)
                            .clickable { app.selectRegion(r.regionCode) }
                            .testTag("pdig.r9.infrastructure.region.${r.regionCode}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(regionFlag(r.regionCode), fontSize = 13.sp)
                            Text(r.displayName, Modifier.weight(1f), fontSize = 10.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, color = R9.Ink)
                            Text(r.cardCount.toString(), color = R9.Blue,
                                fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun r9CategoryCount(screen: VScreen, app: VAppState): String {
    if (screen == VScreen.WEAKNESSES) return "查看风险"
    val region = app.regionFilter
    fun ok(code: String) = region == null || code == region
    return when(screen) {
        VScreen.OVERVIEW -> if (region == null) "全球" else "已选地区"
        VScreen.CARDS -> "${app.demoCards().count { ok(it.region) }} 张"
        VScreen.NUMBERS -> "${app.demoNumbers().count { ok(it.region) }} 个"
        VScreen.ACCOUNTS -> "${if (app.emptyDemo) 0 else UiVNextDemoFixture.accounts.count { ok(it.region) }} 个"
        VScreen.EMAILS -> "${if (app.emptyDemo) 0 else UiVNextDemoFixture.emails.count { ok(it.region) }} 个"
        VScreen.DEVICES -> "${if (app.emptyDemo) 0 else UiVNextDemoFixture.devices.count { ok(it.region) }} 台"
        VScreen.SERVICES -> "${if (app.emptyDemo) 0 else UiVNextDemoFixture.services.count { ok(it.region) }} 项"
        else -> ""
    }
}
