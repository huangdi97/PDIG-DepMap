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
    fun PreviewAsiaSunGivesLightToActualAsiaCameraCenter() {
        val eastAsia = latLonToVec(16f, 107f)
        val asiaDay = dayFactor(eastAsia.x, eastAsia.y, eastAsia.z, R9_REFERENCE_SUN_DIR)
        val west = latLonToVec(16f, -90f)
        val westDay = dayFactor(west.x, west.y, west.z, R9_REFERENCE_SUN_DIR)
        org.junit.Assert.assertTrue("R9 should illuminate East Asia", asiaDay > 0.82f)
        org.junit.Assert.assertTrue("R9 still has a night hemisphere", westDay < 0.32f)
    }

    @Test
    fun PreviewReferenceGradeChangesDarkAlbedoButKeepsWhiteBounded() {
        val grade = liftChannel(34, 225, 0.24f)
        org.junit.Assert.assertTrue("Preview must perceptibly lift a dark Earth texture", grade >= 75)
        assertEquals(255, liftChannel(255, 255, 0.30f))
        assertEquals(34, liftChannel(34, 225, 0f))
    }

    @Test
    fun PreviewOceanSpecularIsWaterOnlyAndDaylit() {
        val ocean = oceanSpecularStrength(25, 62, 129, 1f, 1f)
        org.junit.Assert.assertTrue("Sunlit ocean should have a visible glint", ocean > .30f)
        assertEquals(0f, oceanSpecularStrength(190, 115, 65, 1f, 1f), 0.00001f)
        org.junit.Assert.assertTrue("Glint must remain narrow",
            oceanSpecularStrength(25, 62, 129, .75f, 1f) < .001f)
        assertEquals(0f, oceanSpecularStrength(25, 62, 129, 1f, 0f), 0.00001f)
    }

    @Test
    fun NegativeLongitudeIsPreservedForUnitedStates() {
        assertEquals((-PI / 2).toFloat(), earthLongitudeRad(latLonToVec(0f, -90f)), 0.0001f)
    }

    @Test
    fun DefaultAsiaPacificCameraIsNotAccidentallyPolar() {
        val camera = focusCamera(16f, 107f)
        val focalPoint = project(latLonToVec(16f, 107f), camera, 200f, 200f, 200f)
        assertEquals(200f, focalPoint.x, 0.2f)
        assertEquals(200f, focalPoint.y, 0.2f)
        org.junit.Assert.assertTrue("East Asia must be front-facing", focalPoint.zDepth > 0.99f)

        val northPole = project(latLonToVec(90f, 0f), camera, 200f, 200f, 200f)
        org.junit.Assert.assertTrue(
            "The selected direction must not center the north pole",
            kotlin.math.abs(northPole.y - 200f) > 100f,
        )
    }
}
