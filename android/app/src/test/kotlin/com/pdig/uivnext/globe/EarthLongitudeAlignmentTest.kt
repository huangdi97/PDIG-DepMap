package com.pdig.uivnext.globe

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI

/** Regression: geographic anchor coordinates and textured Earth must share one longitude axis. */
class EarthLongitudeAlignmentTest {
    @Test
    fun GreenwichIsZeroOnTexture() {
        assertEquals(0.0f, earthLongitudeRad(latLonToVec(0f, 0f)), 0.0001f)
    }

    @Test
    fun AsiaFacesObserverAtPositiveNinetyLongitude() {
        val front = latLonToVec(0f, 90f)
        assertEquals(1f, front.z, 0.0001f)
        assertEquals((PI / 2).toFloat(), earthLongitudeRad(front), 0.0001f)
    }

    @Test
    fun NegativeLongitudeIsPreservedForUnitedStates() {
        assertEquals((-PI / 2).toFloat(), earthLongitudeRad(latLonToVec(0f, -90f)), 0.0001f)
    }
}
