package com.pdig.uivnext.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.globe.R15WorldScene
import com.pdig.uivnext.production.ProductionConsumerInventory
import com.pdig.uivnext.production.VNextProductionSnapshot
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.ui.r9.R16ProjectedRegionOverlay

/**
 * Production Reality world context.
 *
 * R39 upgrades this surface from "GPU globe with zero geography" to a governed
 * Region Lens:
 * - RegionFact is the only geographic authority;
 * - same-priority conflicts are not silently assigned;
 * - only presentation anchors with known coordinates are plotted;
 * - cross-region arcs require an actual confirmed Dependency with both endpoints
 *   already carrying unambiguous Region Lens selection;
 * - missing geography remains unknown rather than inferred.
 */
@Composable
internal fun ProductionWorldContext(
    app: VAppState,
    snapshot: VNextProductionSnapshot,
    inventory: ProductionConsumerInventory,
    modifier: Modifier = Modifier,
) {
    val projection = buildProductionWorldProjection(snapshot, inventory)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pdig.production-vnext.world-context"),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Xl),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .testTag("pdig.production-vnext.governed-world"),
            ) {
                androidx.compose.foundation.layout.Box(
                    Modifier
                        .fillMaxWidth()
                        .then(Modifier),
                ) {
                    androidx.compose.foundation.layout.Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(0.dp),
                    ) {
                        androidx.compose.foundation.layout.Box(
                            Modifier
                                .fillMaxWidth()
                                .testTag("pdig.production-vnext.world-stage"),
                        ) {
                            // The explicit height lives at the stage so the explanatory
                            // truth strip below remains outside the camera viewport.
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(0.dp),
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("pdig.production-vnext.world-canvas"),
                                    color = PdigV2Colors.Surface,
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(0.dp),
                                    ) {
                                        androidx.compose.foundation.layout.Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(0.dp),
                                        ) {
                                            // Size is applied through the outer world scene
                                            // modifier to keep GLSurfaceView and annotation
                                            // viewport identical.
                                            Box(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(0.dp),
                                            ) {
                                                androidx.compose.foundation.layout.Box(
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .testTag("pdig.production-vnext.world-r39"),
                                                ) {
                                                    R15WorldScene(
                                                        controller = app.globe,
                                                        regions = projection.regions,
                                                        arcingPairs = projection.arcingPairs,
                                                        reduceMotion = app.reduceMotion,
                                                        onRegionChosen = { },
                                                    )
                                                    R16ProjectedRegionOverlay(
                                                        controller = app.globe,
                                                        regions = projection.regions,
                                                        onRegionChosen = { region ->
                                                            app.globe.focusRegion(region)
                                                        },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = PdigV2Colors.Surface.copy(alpha = 0.96f),
                border = BorderStroke(0.5.dp, PdigV2Colors.BorderSubtle),
            ) {
                Column(
                    Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                ) {
                    Text(
                        "全球上下文 · 已确认地区事实",
                        color = PdigV2Colors.TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    val line = when {
                        inventory.regions.isEmpty() ->
                            "尚无已确认 RegionFact · 不从币种、号码前缀、品牌或位置推测"
                        projection.unplottableTerritoryCodes.isEmpty() ->
                            "${inventory.regions.size} 个地区已进入 Region Lens · 地图只显示受治理事实"
                        else ->
                            "${inventory.regions.size} 个已确认地区 · 地图定位 ${projection.regions.size} 个 · " +
                                "${projection.unplottableTerritoryCodes.size} 个仅文字展示"
                    }
                    Text(
                        line,
                        color = PdigV2Colors.TextMuted,
                        fontSize = 9.sp,
                    )
                    if (projection.needsReviewObjectCount > 0) {
                        Text(
                            "${projection.needsReviewObjectCount} 个对象存在同优先级地区冲突，未自动落点",
                            color = PdigV2Colors.Warning,
                            fontSize = 9.sp,
                        )
                    }
                    if (projection.unknownObjectCount > 0) {
                        Text(
                            "${projection.unknownObjectCount} 个对象地区仍未知",
                            color = PdigV2Colors.TextMuted,
                            fontSize = 9.sp,
                        )
                    }
                }
            }
        }
    }
}
