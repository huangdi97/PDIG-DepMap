package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.ui.VAppState

/**
 * Human-selected phone reference: the globe is a spatial navigator with attached
 * real region identities. Nothing in these chips invents a relationship or a safe status.
 * A blank fixture must not display fabricated region callouts.
 */
@Composable
internal fun CompactWorldHero(
    app: VAppState,
    regions: List<RegionPresentation>,
    arcs: List<Pair<String, String>>,
    cards: Int,
    numbers: Int,
    accounts: Int,
    services: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(382.dp).testTag(VTestIds.NOW_GLOBE),
        color = Color(0xFFF1F7FF),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, Color(0xFFD7E7FC)),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFF9FCFF), Color(0xFFDFEDFF), Color(0xFFCADEF9), Color(0xFFEEF6FF)),
                    ),
                ),
        ) {
            Box(
                Modifier.fillMaxSize()
                    .padding(top = 27.dp, bottom = 51.dp)
                    .graphicsLayer(scaleX = 1.61f, scaleY = 1.61f),
            ) {
                VNextGlobe(
                    controller = app.globe,
                    regions = regions,
                    arcingPairs = arcs,
                    reduceMotion = app.reduceMotion,
                )
            }

            Column(
                Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 15.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    "你的全球数字基础设施",
                    color = PdigV2Colors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (regions.isEmpty()) "暂无已记录的地区" else "连接 ${regions.size} 个地区 · 触手可及",
                    color = PdigV2Colors.TextSecondary,
                    fontSize = 11.sp,
                )
            }

            // Region identity is derived exclusively from the current data fixture.
            // Asymmetric positions keep the Earth as the hero instead of a list of statistics.
            val positions = listOf(
                Alignment.CenterStart to (-87).dp,
                Alignment.CenterEnd to (-80).dp,
                Alignment.CenterEnd to (-3).dp,
                Alignment.CenterStart to 48.dp,
                Alignment.CenterStart to 93.dp,
            )
            val preferred = listOf("US", "GB", "CN", "HK", "SG")
            preferred.mapNotNull { code -> regions.firstOrNull { it.regionCode == code } }
                .take(5).forEachIndexed { index, region ->
                    val (alignment, y) = positions[index]
                    RegionIdentityChip(
                        region = region,
                        modifier = Modifier.align(alignment)
                            .offset(y = y)
                            .padding(horizontal = 7.dp),
                        onClick = {
                            app.selectRegion(region.regionCode)
                            app.navigate(VScreen.OVERVIEW)
                        },
                    )
                }

            Surface(
                modifier = Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
                color = Color.White.copy(alpha = 0.94f),
                shape = RoundedCornerShape(19.dp),
                border = BorderStroke(1.dp, Color(0xFFD4E4F9)),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 9.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    HeroMetric(cards, "银行卡", "▣", Modifier.weight(1f))
                    HeroMetric(numbers, "手机号", "▤", Modifier.weight(1f))
                    HeroMetric(accounts, "账户", "◎", Modifier.weight(1f))
                    HeroMetric(services, "服务", "✧", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RegionIdentityChip(
    region: RegionPresentation,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val flag = when (region.regionCode) {
        "US" -> "🇺🇸"
        "GB" -> "🇬🇧"
        "CN" -> "🇨🇳"
        "HK" -> "🇭🇰"
        "SG" -> "🇸🇬"
        "MO" -> "🇲🇴"
        else -> "🌐"
    }
    Surface(
        modifier = modifier.clickable(onClick = onClick)
            .testTag("pdig.now.region-chip.${region.regionCode}"),
        color = Color.White.copy(alpha = 0.95f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFC5DDF7)),
        shadowElevation = 2.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(flag, fontSize = 17.sp)
            Column {
                Text(
                    region.displayName,
                    fontSize = 11.sp,
                    color = PdigV2Colors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${region.cardCount} 张卡 · ${region.phoneCount} 个号",
                    color = PdigV2Colors.TextSecondary,
                    fontSize = 9.sp,
                )
            }
        }
    }
}

@Composable
private fun HeroMetric(value: Int, label: String, symbol: String, modifier: Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        val accent = when (label) {
            "银行卡" -> Color(0xFFFF8B3D)
            "手机号" -> Color(0xFF14B881)
            "账户" -> Color(0xFF2887EE)
            else -> Color(0xFFAB7BFF)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Surface(
                modifier = Modifier.size(20.dp),
                shape = RoundedCornerShape(6.dp),
                color = accent,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(symbol, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(value.toString(), color = PdigV2Colors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Text(label, color = PdigV2Colors.TextSecondary, fontSize = 10.sp)
    }
}
