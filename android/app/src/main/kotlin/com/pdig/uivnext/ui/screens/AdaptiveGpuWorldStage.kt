package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.pdig.uivnext.globe.R15WorldScene
import com.pdig.uivnext.model.RegionPresentation
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.r9.R16ProjectedRegionOverlay

/**
 * Shared R19 spatial stage for Medium / Expanded surfaces.
 *
 * Phone preview already uses the R15/R16 world system through R17. Wider layouts
 * must use the same GPU Earth + camera-projected region annotations rather than
 * silently falling back to the legacy VNextGlobe renderer.
 */
@Composable
internal fun AdaptiveGpuWorldStage(
    app: VAppState,
    regions: List<RegionPresentation>,
    arcs: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    onRegionChosen: (RegionPresentation) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("pdig.r19.adaptive-gpu-world"),
    ) {
        R15WorldScene(
            controller = app.globe,
            regions = regions,
            arcingPairs = if (app.emptyDemo) emptyList() else arcs,
            reduceMotion = app.reduceMotion,
            onRegionChosen = onRegionChosen,
        )
        R16ProjectedRegionOverlay(
            controller = app.globe,
            regions = regions,
            onRegionChosen = onRegionChosen,
        )
    }
}
