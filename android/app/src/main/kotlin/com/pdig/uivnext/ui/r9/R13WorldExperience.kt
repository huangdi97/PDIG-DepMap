package com.pdig.uivnext.ui.r9

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.globe.VNextGlobe
import com.pdig.uivnext.model.RegionPresentation
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.screens.arcPairs

/**
 * R13 Hero is an independent scene rather than the old large financial dashboard card.
 * World is the visual anchor; operational facts form a separate quiet layer.
 *
 * The Earth renderer is real textured Earth, not an emoji or gradient substitute.
 * Every region entry and cross-region arc comes from recorded fixture evidence.
 */
@Composable
internal fun R13WorldExperience(
    app: VAppState,
    regions: List<RegionPresentation>,
    cards: Int,
    numbers: Int,
    accounts: Int,
    services: Int,
) {
    Column(
        Modifier.fillMaxWidth().testTag(VTestIds.NOW_GLOBE)
            .testTag("pdig.r9.world.stage")
            .testTag("pdig.r13.world.experience"),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("你的全球数字基础设施", color = R9.Ink,
                    fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(
                    if (regions.isEmpty()) "尚无已记录地区"
                    else "已记录 ${regions.size} 个地区 · 轻触地球探索",
                    color = R9.Muted, fontSize = 11.sp, maxLines = 1,
                )
            }
            Surface(color = Color.White, shape = RoundedCornerShape(13.dp),
                border = BorderStroke(1.dp, R9.Line),
                modifier = Modifier.clickable { app.navigate(VScreen.OVERVIEW) }) {
                Text("总览 ↗", Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    color = R9.Blue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // This is one spatial plane; no inset widget/card-on-card presentation.
        Box(
            Modifier.fillMaxWidth().height(290.dp)
                .clip(RoundedCornerShape(25.dp))
                .background(
                    Brush.radialGradient(listOf(
                        Color(0xFFB6DBFF),
                        Color(0xFFD9EEFF),
                        Color(0xFFEAF7FF),
                        Color(0xFFF4FAFF),
                    )),
                )
                .testTag("pdig.r9.world.hero"),
        ) {
            // Soft distant haze is decorative light, never a fake graph edge.
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = .92f), Color.White.copy(alpha = 0f)),
                        center = center,
                        radius = size.minDimension * .64f,
                    ),
                    radius = size.minDimension * .64f,
                    center = center,
                )
            }
            Box(
                Modifier.fillMaxSize().padding(horizontal = 13.dp)
                    .graphicsLayer(scaleX = 1.27f, scaleY = 1.27f),
            ) {
                VNextGlobe(
                    controller = app.globe,
                    regions = regions,
                    arcingPairs = if (app.emptyDemo) emptyList() else arcPairs(),
                    reduceMotion = app.reduceMotion,
                )
            }
            // Two labels only: the old five freely floating labels hid continents
            // and were not actually attached to projected geodesic points.
            // An exact, accessible full region list stays below the globe.
            val byReach = regions.sortedWith(
                compareByDescending<RegionPresentation> {
                    it.cardCount + it.phoneCount + it.accountCount + it.serviceCount
                }.thenBy { it.regionCode },
            )
            byReach.firstOrNull()?.let { region ->
                R9RegionPill(
                    region,
                    Modifier.align(Alignment.TopStart).padding(start = 6.dp, top = 11.dp),
                ) {
                    app.selectRegion(region.regionCode)
                    app.navigate(VScreen.OVERVIEW)
                }
            }
            byReach.getOrNull(1)?.let { region ->
                R9RegionPill(
                    region,
                    Modifier.align(Alignment.BottomEnd).padding(end = 6.dp, bottom = 13.dp),
                ) {
                    app.selectRegion(region.regionCode)
                    app.navigate(VScreen.OVERVIEW)
                }
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 11.dp, bottom = 12.dp),
                color = Color.White.copy(alpha = .86f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("可旋转 · 点击地区节点", Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    color = R9.Muted, fontSize = 9.sp)
            }
        }

        // Distinct region control is sourced from the actual asset distribution.
        // Unlike fixed floating cards, labels here never cover the textured Earth.
        if (regions.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .testTag("pdig.r13.world.regions"),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                regions.forEach { region ->
                    Surface(
                        modifier = Modifier.defaultMinSize(minHeight = 43.dp)
                            .clickable {
                                app.selectRegion(region.regionCode)
                                app.navigate(VScreen.OVERVIEW)
                            }.testTag("pdig.r13.world.region.${region.regionCode}"),
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, R9.Line),
                    ) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(regionFlag(region.regionCode), fontSize = 14.sp)
                            Text(region.displayName, color = R9.Ink,
                                fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        // The object counts are now a clear independent lower rail, not an overlay
        // positioned over landmasses and node descriptions.
        Surface(
            modifier = Modifier.fillMaxWidth().height(68.dp)
                .testTag("pdig.r13.world.asset-rail"),
            color = Color.White, shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, R9.Line),
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                R9Counter(cards, "银行卡", R9.Amber, "▣",
                    Modifier.weight(1f).clickable { app.navigate(VScreen.CARDS) })
                R9Counter(numbers, "号码", R9.Green, "▤",
                    Modifier.weight(1f).clickable { app.navigate(VScreen.NUMBERS) })
                R9Counter(accounts, "账户", R9.Blue, "◎",
                    Modifier.weight(1f).clickable { app.navigate(VScreen.ACCOUNTS) })
                R9Counter(services, "服务", Color(0xFF8A71DE), "✧",
                    Modifier.weight(1f).clickable { app.navigate(VScreen.SERVICES) })
            }
        }
    }
}
