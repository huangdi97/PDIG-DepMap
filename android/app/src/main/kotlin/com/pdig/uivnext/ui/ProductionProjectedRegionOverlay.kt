package com.pdig.uivnext.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.globe.GlobeController
import com.pdig.uivnext.globe.r16ProjectedRegionLabels
import com.pdig.uivnext.model.RegionPresentation
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Production-owned geographic overlay.
 *
 * Production must not import Preview R9/R1x composables. This component shares only
 * the renderer-agnostic projection math from the globe package and renders confirmed
 * Production Region Lens facts.
 */
@Composable
internal fun ProductionProjectedRegionOverlay(
    controller: GlobeController,
    regions: List<RegionPresentation>,
    onRegionChosen: (RegionPresentation) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().testTag("pdig.production-vnext.geo-label-layer"),
    ) {
        val density = LocalDensity.current
        val pxPerDp = with(density) { 1.dp.toPx() }
        val labels = r16ProjectedRegionLabels(
            regions = regions,
            camera = controller.camera,
            widthPx = constraints.maxWidth.toFloat(),
            heightPx = constraints.maxHeight.toFloat(),
            pxPerDp = pxPerDp,
            selectedCode = controller.selectedRegion,
        )

        Canvas(Modifier.fillMaxSize().testTag("pdig.production-vnext.geo-tethers")) {
            labels.forEach { tag ->
                val centerX = tag.left + tag.width / 2f
                val centerY = tag.top + tag.height / 2f
                if (abs(centerX - tag.anchorX) + abs(centerY - tag.anchorY) > 17f * pxPerDp) {
                    drawLine(
                        color = Color(0xFF349CEA).copy(alpha = .38f),
                        start = Offset(tag.anchorX, tag.anchorY),
                        end = Offset(centerX, centerY),
                        strokeWidth = 1.dp.toPx(),
                    )
                    drawCircle(
                        color = Color(0xFF34A7F4).copy(alpha = .20f),
                        radius = 5.dp.toPx(),
                        center = Offset(tag.anchorX, tag.anchorY),
                    )
                }
            }
        }

        labels.forEach { tag ->
            val primary = tag.primary
            val all = tag.members
            Box(
                modifier = Modifier
                    .offset { IntOffset(tag.left.roundToInt(), tag.top.roundToInt()) }
                    .size(
                        width = with(density) { tag.width.toDp() },
                        height = with(density) { tag.height.toDp() },
                    )
                    .clickable { onRegionChosen(primary) }
                    .semantics {
                        val total = primary.cardCount + primary.phoneCount +
                            primary.accountCount + primary.serviceCount + primary.otherCount
                        contentDescription =
                            "已确认地区：${primary.displayName}，${total} 项已记录基础设施"
                    }
                    .testTag("pdig.production-vnext.geo-region.${primary.regionCode}"),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    color = Color.White.copy(alpha = .93f),
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(
                        1.dp,
                        if (controller.selectedRegion == primary.regionCode) {
                            Color(0xFF176EF2).copy(alpha = .78f)
                        } else {
                            Color.White.copy(alpha = .86f)
                        },
                    ),
                    shadowElevation = 3.dp,
                ) {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 7.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(regionFlag(primary.regionCode), fontSize = 14.sp, maxLines = 1)
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            Text(
                                primary.displayName,
                                color = Color(0xFF112646),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val total = primary.cardCount + primary.phoneCount +
                                primary.accountCount + primary.serviceCount + primary.otherCount
                            Text(
                                "${total} 项 · ${primary.cardCount} 卡 · ${primary.phoneCount} 号",
                                color = Color(0xFF617896),
                                fontSize = 7.sp,
                                maxLines = 1,
                            )
                        }
                        if (all.size > 1) {
                            Text(
                                "+${all.size - 1}",
                                color = Color(0xFF176EF2),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun regionFlag(code: String): String = when (code) {
    "CN" -> "🇨🇳"
    "HK" -> "🇭🇰"
    "MO" -> "🇲🇴"
    "GB" -> "🇬🇧"
    "US" -> "🇺🇸"
    "SG" -> "🇸🇬"
    else -> "🌐"
}
