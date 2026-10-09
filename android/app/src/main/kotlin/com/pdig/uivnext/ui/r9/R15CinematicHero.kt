package com.pdig.uivnext.ui.r9

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.globe.R15WorldScene
import com.pdig.uivnext.model.RegionPresentation
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.screens.arcPairs

/**
 * A single art-directed world composition replacing the separate old header,
 * rectangular globe widget, region row, and metrics card. The Earth is the
 * centerpiece; small factual labels and metrics form one coherent light stage.
 * The scene is still accessible via the full region list on Infrastructure.
 */
@Composable
internal fun R15CinematicHero(
    app: VAppState,
    regions: List<RegionPresentation>,
    cards: Int,
    numbers: Int,
    accounts: Int,
    services: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth()
            .height(435.dp)
            .testTag(VTestIds.NOW_GLOBE)
            .testTag("pdig.r9.world.stage")
            .testTag("pdig.r13.world.experience")
            .testTag("pdig.r15.hero"),
        color = Color(0xFFEFF7FF),
        shape = RoundedCornerShape(23.dp),
        border = BorderStroke(1.dp, Color(0xFFC6E0FA)),
    ) {
        Column(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF8FCFF), Color(0xFFE4F3FF), Color(0xFFD5ECFF), Color(0xFFF2FAFF)),
                ),
            ).padding(horizontal = 8.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().height(53.dp).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("你的全球数字基础设施", color = R9.Ink,
                        fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(if (regions.isEmpty()) "暂无已记录地区"
                        else "连接 " + regions.size + " 个地区 · 轻触地球探索",
                        color = R9.Muted, fontSize = 10.sp, maxLines = 1)
                }
                Surface(
                    color = Color.White.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(11.dp),
                    border = BorderStroke(1.dp, R9.Line),
                    modifier = Modifier.clickable { app.navigate(VScreen.OVERVIEW) },
                ) {
                    Text("总览 ›", Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
                        color = R9.Blue, fontSize = 10.sp)
                }
            }

            // A GPU scene is not a photo background. All connection nodes remain
            // attached to real region coordinates and respond to the same camera.
            Box(
                Modifier.fillMaxWidth().weight(1f)
                    .testTag("pdig.r9.world.hero")
                    .testTag("pdig.r15.hero.spatial-stage"),
            ) {
                R15WorldScene(
                    controller = app.globe,
                    regions = regions,
                    arcingPairs = if (app.emptyDemo) emptyList() else arcPairs(),
                    reduceMotion = app.reduceMotion,
                    onRegionChosen = { region ->
                        app.selectRegion(region.regionCode)
                        app.navigate(VScreen.OVERVIEW)
                    },
                )

                val byCode = regions.associateBy { it.regionCode }
                val placements = listOf(
                    Triple("US", Alignment.TopStart, 25.dp),
                    Triple("GB", Alignment.TopEnd, 10.dp),
                    Triple("CN", Alignment.CenterEnd, 0.dp),
                    Triple("HK", Alignment.BottomStart, 29.dp),
                    Triple("SG", Alignment.BottomEnd, 12.dp),
                )
                placements.forEach { (code, align, verticalInset) ->
                    val region = byCode[code] ?: return@forEach
                    val verticalModifier = when (align) {
                        Alignment.TopStart, Alignment.TopEnd -> Modifier.padding(top = verticalInset)
                        Alignment.BottomStart, Alignment.BottomEnd -> Modifier.padding(bottom = verticalInset)
                        else -> Modifier
                    }
                    R15RegionOverlay(
                        region,
                        modifier = verticalModifier.align(align).padding(horizontal = 3.dp),
                        onClick = {
                            app.selectRegion(region.regionCode)
                            app.navigate(VScreen.OVERVIEW)
                        },
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth().height(62.dp)
                    .testTag("pdig.r13.world.asset-rail")
                    .testTag("pdig.r15.hero.asset-rail"),
                color = Color.White.copy(alpha = .95f),
                shape = RoundedCornerShape(17.dp),
                border = BorderStroke(1.dp, R9.Line),
            ) {
                Row(Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    R9Counter(cards, "银行卡", R9.Amber, "▣",
                        Modifier.weight(1f).clickable { app.navigate(VScreen.CARDS) })
                    R9Counter(numbers, "手机号", R9.Green, "▤",
                        Modifier.weight(1f).clickable { app.navigate(VScreen.NUMBERS) })
                    R9Counter(accounts, "账户", R9.Blue, "◎",
                        Modifier.weight(1f).clickable { app.navigate(VScreen.ACCOUNTS) })
                    R9Counter(services, "服务", Color(0xFF926CE5), "✧",
                        Modifier.weight(1f).clickable { app.navigate(VScreen.SERVICES) })
                }
            }
        }
    }
}

@Composable
private fun R15RegionOverlay(region: RegionPresentation, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.width(110.dp).clickable(onClick = onClick)
            .testTag("pdig.r15.hero.region." + region.regionCode),
        color = Color.White.copy(alpha = .94f),
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, Color(0xFFD0E3F9)),
        shadowElevation = 2.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 7.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(regionFlag(region.regionCode), fontSize = 16.sp)
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(region.displayName, color = R9.Ink,
                    fontWeight = FontWeight.Bold, fontSize = 10.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    region.cardCount.toString() + " 张卡 · " + region.phoneCount + " 号",
                    fontSize = 8.sp, color = R9.Muted, maxLines = 1,
                )
            }
        }
    }
}
