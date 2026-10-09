package com.pdig.uivnext.ui.r9

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import com.pdig.uivnext.globe.GlobeController
import com.pdig.uivnext.globe.r16ProjectedRegionLabels
import com.pdig.uivnext.model.RegionPresentation
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * R16: a country chip is a live geographic annotation, not a corner badge.
 * Every frame uses the GPU renderer's camera, radius, viewport and front-face
 * convention. 68dp baseline chip; grouped nodes show +N and open a chooser.
 *
 * The GPU Earth and its gesture recognizer are unchanged and remain fluid.
 */
@Composable
internal fun R16ProjectedRegionOverlay(
    controller: GlobeController,
    regions: List<RegionPresentation>,
    onRegionChosen: (RegionPresentation) -> Unit,
) {
    var pendingGroup by remember { mutableStateOf<List<RegionPresentation>?>(null) }
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().testTag("pdig.r16.geo-label-layer"),
    ) {
        val density = LocalDensity.current
        val pxPerDp = with(density) { 1.dp.toPx() }
        // Compose observes mutableStateOf camera and repositions labels during drag.
        val camera = controller.camera
        val labels = r16ProjectedRegionLabels(
            regions = regions,
            camera = camera,
            widthPx = constraints.maxWidth.toFloat(),
            heightPx = constraints.maxHeight.toFloat(),
            pxPerDp = pxPerDp,
            selectedCode = controller.selectedRegion,
        )
        // Thin tethers are geographic callouts only, NOT canonical graph edges.
        Canvas(Modifier.fillMaxSize().testTag("pdig.r16.geo-tethers")) {
            labels.forEach { tag ->
                val centerX = tag.left + tag.width / 2f
                val centerY = tag.top + tag.height / 2f
                if (abs(centerX - tag.anchorX) + abs(centerY - tag.anchorY) > 17f * pxPerDp) {
                    drawLine(
                        color = Color(0xFF349CEA).copy(alpha = .55f),
                        start = Offset(tag.anchorX, tag.anchorY),
                        end = Offset(centerX, centerY),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            }
        }
        labels.forEach { tag ->
            val all = tag.members
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(tag.left.roundToInt(), tag.top.roundToInt())
                    }
                    .size(
                        width = with(density) { tag.width.toDp() },
                        height = with(density) { tag.height.toDp() },
                    )
                    .clickable {
                        if (all.size == 1) onRegionChosen(all.first())
                        else pendingGroup = all
                    }
                    .semantics {
                        contentDescription = if (all.size == 1)
                            "地球地区：${tag.primary.displayName}"
                        else "地球地区组：${tag.primary.displayName}等${all.size}个地区，点击选择"
                    }
                    .testTag("pdig.r16.geo-region.${tag.primary.regionCode}"),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(26.dp),
                    color = Color.White.copy(alpha = .96f),
                    shape = RoundedCornerShape(13.dp),
                    border = BorderStroke(
                        1.dp,
                        if (controller.selectedRegion == tag.primary.regionCode) R9.Blue else R9.Line,
                    ),
                    shadowElevation = 2.dp,
                ) {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(regionFlag(tag.primary.regionCode), fontSize = 12.sp, maxLines = 1)
                        Text(
                            tag.primary.displayName.take(if (all.size > 1) 2 else 3),
                            modifier = Modifier.weight(1f, fill = false),
                            fontSize = 9.sp, fontWeight = FontWeight.SemiBold,
                            color = R9.Ink, maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (all.size > 1) {
                            Text("+${all.size - 1}", fontSize = 9.sp,
                                color = R9.Blue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
    val choose = pendingGroup
    if (choose != null) {
        AlertDialog(
            onDismissRequest = { pendingGroup = null },
            title = {
                Text("选择地区", color = R9.Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(
                    Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    choose.forEach { region ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                pendingGroup = null
                                onRegionChosen(region)
                            }.padding(vertical = 9.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(regionFlag(region.regionCode), fontSize = 18.sp)
                            Text(region.displayName, Modifier.weight(1f),
                                color = R9.Ink, fontSize = 13.sp)
                            Text("${region.cardCount} 卡 · ${region.phoneCount} 号",
                                fontSize = 10.sp, color = R9.Muted)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pendingGroup = null }) { Text("关闭") }
            },
            containerColor = Color.White,
        )
    }
}
