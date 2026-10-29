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
 * R19 shared Medium/Expanded world plane.
 *
 * Phone R9 and adaptive layouts now use the same GPU Earth / camera projection
 * family, while keeping different surrounding information architecture.
 * This is not a phone layout stretched wider: only the spatial renderer and
 * geographic annotation semantics are shared.
 */
@Composable
internal fun R19AdaptiveWorldScene(
    app: VAppState,
    regions: List<RegionPresentation>,
    arcingPairs: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    onRegionChosen: (RegionPresentation) -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .testTag("pdig.r19.adaptive-world-stage"),
    ) {
        R15WorldScene(
            controller = app.globe,
            regions = regions,
            arcingPairs = if (app.emptyDemo) emptyList() else arcingPairs,
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
