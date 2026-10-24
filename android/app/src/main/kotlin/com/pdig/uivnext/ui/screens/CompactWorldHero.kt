package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    // Phone reference has three discrete vertical layers; unlike the old one-Box
    // construction this never allows oversized globe art or region chips to cover
    // the title / bottom metrics. All chip facts come from current regions.
    Surface(
        modifier = Modifier.fillMaxWidth().height(382.dp).testTag(VTestIds.NOW_GLOBE),
        color = Color(0xFFF1F7FF),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, Color(0xFFD7E7FC)),
    ) {
        Column(
            Modifier.fillMaxSize()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFF8FBFF), Color(0xFFDDEEFF), Color(0xFFC5DEFA), Color(0xFFEDF6FF)),
                    ),
                )
                .padding(horizontal = 11.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().height(53.dp)
                    .testTag("pdig.now.world-title"),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    "你的全球数字基础设施",
                    color = PdigV2Colors.TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    if (regions.isEmpty()) "暂无已记录的地区" else "连接 ${regions.size} 个地区 · 一览全局",
                    color = PdigV2Colors.TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }

            Box(Modifier.fillMaxWidth().weight(1f).testTag("pdig.now.world-stage")) {
                // R19 uses the same GPU Earth + live camera-projected region
                // annotations as the accepted Preview direction. No fixed-corner
                // country pills: labels remain attached to geographic anchors.
                R19AdaptiveWorldScene(
                    app = app,
                    regions = regions,
                    arcingPairs = arcs,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 5.dp),
                    onRegionChosen = { region ->
                        app.selectRegion(region.regionCode)
                        app.navigate(VScreen.OVERVIEW)
                    },
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth().height(59.dp)
                    .testTag("pdig.now.world-metrics"),
                color = Color.White.copy(alpha = 0.96f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Color(0xFFD4E4F9)),
            ) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
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
