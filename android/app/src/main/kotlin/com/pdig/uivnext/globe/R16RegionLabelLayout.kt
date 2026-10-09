package com.pdig.uivnext.globe

import com.pdig.uivnext.model.RegionPresentation
import kotlin.math.hypot
import kotlin.math.min

/**
 * R16: all country labels are anchored to the same live camera / orthographic
 * projection as the GLSL planet. No fixed US/CN/HK corner positions.
 *
 * Front hemisphere only; dense locations cluster; a strict screen budget
 * prevents N recorded countries from generating N oversized floating cards.
 * Other factual regions remain available via the existing region list + dots.
 */
internal data class R16ProjectedRegionLabel(
    val primary: RegionPresentation,
    val members: List<RegionPresentation>,
    val anchorX: Float,
    val anchorY: Float,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
) {
    val groupedCount: Int get() = members.size
}

private data class R16Anchor(
    val region: RegionPresentation,
    val x: Float,
    val y: Float,
    val depth: Float,
)

internal fun r16SceneRadius(widthPx: Float, heightPx: Float, zoom: Float): Float =
    min(widthPx * 0.42f, heightPx * 0.47f) * zoom.coerceIn(0.70f, 1.90f)

internal fun r16ProjectedRegionLabels(
    regions: List<RegionPresentation>,
    camera: GlobeCamera,
    widthPx: Float,
    heightPx: Float,
    pxPerDp: Float,
    selectedCode: String? = null,
): List<R16ProjectedRegionLabel> {
    if (widthPx <= 0f || heightPx <= 0f || pxPerDp <= 0f) return emptyList()
    val radius = r16SceneRadius(widthPx, heightPx, camera.zoom)
    val cx = widthPx * .5f
    val cy = heightPx * .5f
    val candidates = regions.mapNotNull { region ->
        val p = project(
            latLonToVec(region.latitude.toFloat(), region.longitude.toFloat()),
            camera, radius, cx, cy,
        )
        if (p.zDepth <= 0.19f) null else R16Anchor(region, p.x, p.y, p.zDepth)
    }.sortedWith(
        compareByDescending<R16Anchor> { it.region.regionCode == selectedCode }
            .thenByDescending { it.region.cardCount + it.region.phoneCount +
                it.region.accountCount + it.region.serviceCount }
            .thenByDescending { it.depth }
            .thenBy { it.region.regionCode },
    )
    // Cluster screen-space proximity, not alphabetical country codes or an
    // invented real-world relationship. Any cluster is only a UI presentation.
    val groups = mutableListOf<MutableList<R16Anchor>>()
    val mergeDistance = (50f * pxPerDp).coerceAtMost(radius * .52f)
    for (anchor in candidates) {
        val near = groups.firstOrNull { members ->
            members.any { hypot(it.x - anchor.x, it.y - anchor.y) < mergeDistance }
        }
        if (near != null) near.add(anchor) else groups.add(mutableListOf(anchor))
    }
    val budget = when {
        widthPx < 320f * pxPerDp -> 2
        camera.zoom >= 1.35f -> 5
        else -> 4
    }
    val placed = mutableListOf<R16ProjectedRegionLabel>()
    val gap = 4f * pxPerDp
    for (group in groups) {
        if (placed.size >= budget) break
        val anchor = group.first()
        val primary = anchor.region
        val labelWidth = (if (group.size > 1) 87f else if (primary.regionCode == selectedCode) 83f else 68f) * pxPerDp
        // 43dp touch hitbox, only a 26dp actual visible pill inside it.
        val hitHeight = 43f * pxPerDp
        if (widthPx < labelWidth + gap * 2f || heightPx < hitHeight + gap * 2f) break
        val horizontal = listOf(0f, -22f * pxPerDp, 22f * pxPerDp)
        val vertical = listOf(-38f * pxPerDp, 10f * pxPerDp, -66f * pxPerDp)
        var found: R16ProjectedRegionLabel? = null
        place@ for (yShift in vertical) for (xShift in horizontal) {
            val left = (anchor.x - labelWidth * .5f + xShift)
                .coerceIn(gap, widthPx - labelWidth - gap)
            val top = (anchor.y + yShift).coerceIn(gap, heightPx - hitHeight - gap)
            val overlaps = placed.any { prev ->
                left < prev.left + prev.width + gap &&
                    left + labelWidth + gap > prev.left &&
                    top < prev.top + prev.height + gap &&
                    top + hitHeight + gap > prev.top
            }
            if (!overlaps) {
                found = R16ProjectedRegionLabel(
                    primary, group.map { it.region },
                    anchor.x, anchor.y, left, top, labelWidth, hitHeight,
                )
                break@place
            }
        }
        if (found != null) placed.add(found)
        // Colliding chip is deliberately omitted, not drawn over another label.
        // Its 3D region dot is still rendered by R15WorldScene.
    }
    return placed
}
