package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
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
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.demo.demoCards
import com.pdig.uivnext.demo.demoNumbers
import com.pdig.uivnext.globe.VNextGlobe
import com.pdig.uivnext.model.RegionPresentation
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.ui.VAppState

/** Search-first infrastructure entry; real navigation, no inert search placeholder. */
@Composable
internal fun CompactInfrastructureSearch(app: VAppState) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(48.dp)
            .clickable { app.navigate(VScreen.SEARCH) }
            .testTag("pdig.overview.search"),
        color = Color.White.copy(alpha = 0.96f),
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E9F6)),
    ) {
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = PdigV2Colors.TextMuted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(9.dp))
            Text("搜索卡片、号码、账户、服务…", modifier = Modifier.weight(1f), color = PdigV2Colors.TextMuted, fontSize = 12.sp)
            Text("全部", color = PdigV2Colors.PrimaryText, fontSize = 11.sp)
        }
    }
}

/**
 * The reference's compact world panel is a region-distribution workspace, NOT
 * another oversized empty dashboard card. The globe and list are the same data.
 */
@Composable
internal fun CompactInfrastructureRegionPanel(
    app: VAppState,
    regions: List<RegionPresentation>,
    arcs: List<Pair<String, String>>,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(220.dp).testTag(VTestIds.GLOBE_STAGE),
        color = Color(0xFFF6FAFF),
        shape = RoundedCornerShape(23.dp),
        border = BorderStroke(1.dp, Color(0xFFD7E5F7)),
    ) {
        Column(Modifier.padding(top = 10.dp, bottom = 8.dp, start = 11.dp, end = 11.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("地区分布", color = PdigV2Colors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("${regions.size} 个地区", color = PdigV2Colors.TextSecondary, fontSize = 11.sp)
            }
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(
                    Modifier.weight(0.53f).fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.radialGradient(listOf(Color(0xFFB7D5FE), Color(0xFFEAF4FF), Color(0xFFF7FBFF)))),
                ) {
                    VNextGlobe(
                        controller = app.globe,
                        regions = regions,
                        arcingPairs = arcs,
                        reduceMotion = app.reduceMotion,
                    )
                }
                Column(
                    Modifier.weight(0.47f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (regions.isEmpty()) {
                        Text("暂无已记录地区", color = PdigV2Colors.TextSecondary, fontSize = 12.sp)
                    } else {
                        regions.take(5).forEach { region ->
                            Row(
                                Modifier.fillMaxWidth().height(31.dp)
                                    .clickable { app.selectRegion(region.regionCode) }
                                    .padding(horizontal = 1.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val symbol = when (region.regionCode) {
                                    "CN" -> "🇨🇳"
                                    "HK" -> "🇭🇰"
                                    "GB" -> "🇬🇧"
                                    "US" -> "🇺🇸"
                                    "SG" -> "🇸🇬"
                                    "MO" -> "🇲🇴"
                                    else -> "🌐"
                                }
                                Text(symbol, fontSize = 12.sp)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    region.displayName,
                                    Modifier.weight(1f),
                                    color = PdigV2Colors.TextPrimary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "${region.cardCount}",
                                    color = PdigV2Colors.PrimaryText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
