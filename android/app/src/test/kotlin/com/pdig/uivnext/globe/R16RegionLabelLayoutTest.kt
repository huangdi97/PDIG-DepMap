package com.pdig.uivnext.globe

import com.pdig.uivnext.model.RegionPresentation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class R16RegionLabelLayoutTest {
    private fun region(code: String, lat: Double, lon: Double, count: Int = 1) =
        RegionPresentation(code, code, lat, lon, cardCount = count)

    private val asia = focusCamera(16f, 107f)
    private fun labels(
        regions: List<RegionPresentation>,
        camera: GlobeCamera = asia,
        selected: String? = null,
    ) = r16ProjectedRegionLabels(regions, camera, 1080f, 660f, 3f, selected)

    @Test
    fun LabelMovesWithCameraInsteadOfRemainingInAFixedCorner() {
        val regions = listOf(region("CN", 35.86, 104.19))
        val initial = labels(regions).single()
        val rotated = labels(regions, asia.copy(yawDeg = asia.yawDeg + 35f)).single()
        assertTrue("Camera yaw must move the geographic anchor",
            abs(initial.anchorX - rotated.anchorX) > 60f)
        assertTrue("Its label must move with the anchor",
            abs(initial.left - rotated.left) > 40f)
    }

    @Test
    fun BackHemisphereCountryDoesNotAppearAsAFloatingLabel() {
        val region = region("CN", 16.0, 107.0)
        assertEquals(1, labels(listOf(region)).size)
        assertTrue(labels(listOf(region), asia.copy(yawDeg = asia.yawDeg + 180f)).isEmpty())
    }

    @Test
    fun NearbyRealRegionsClusterAndRemainSelectableIndividually() {
        val nearby = listOf(
            region("HK", 22.32, 114.17),
            region("MO", 22.20, 113.55),
            region("CN", 23.00, 112.90),
        )
        val tags = labels(nearby)
        assertEquals(1, tags.size)
        assertEquals(3, tags.single().groupedCount)
        assertEquals(setOf("HK", "MO", "CN"),
            tags.single().members.map { it.regionCode }.toSet())
    }

    @Test
    fun ManyCountriesRespectBudgetTouchBoundsAndDoNotOverlap() {
        val countries = (-165..165 step 11).mapIndexed { i, lon ->
            region("R$i", 14.0 + (i % 4) * 4, lon.toDouble())
        }
        val tags = labels(countries)
        assertTrue("Keep visible chips bounded for high country counts", tags.size <= 4)
        for (tag in tags) {
            assertTrue(tag.left >= 0 && tag.top >= 0)
            assertTrue(tag.left + tag.width <= 1080f)
            assertTrue(tag.top + tag.height <= 660f)
        }
        for (i in tags.indices) for (j in i + 1 until tags.size) {
            val a = tags[i]
            val b = tags[j]
            assertFalse("Touch regions of different tags may not overlap",
                a.left < b.left + b.width && a.left + a.width > b.left &&
                a.top < b.top + b.height && a.top + a.height > b.top)
        }
    }

    @Test
    fun RendererAndCountryAnchorsUseExactlyTheSameZoomClamp() {
        assertEquals(660f * .47f * .70f,
            r16SceneRadius(1080f, 660f, .65f), .0001f)
        assertEquals(660f * .47f * 1.9f,
            r16SceneRadius(1080f, 660f, 3f), .0001f)
    }

    @Test
    fun EmptyOrNonpositiveViewportHasNoFloatingCountries() {
        assertTrue(labels(emptyList()).isEmpty())
        assertTrue(r16ProjectedRegionLabels(listOf(region("CN", 20.0, 110.0)),
            asia, 0f, 300f, 3f).isEmpty())
    }
}
