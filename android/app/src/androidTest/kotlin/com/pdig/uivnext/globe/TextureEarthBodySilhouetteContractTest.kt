package com.pdig.uivnext.globe

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Human-pixel regression guard for the textured Earth silhouette.
 *
 * Round4 exposed a faceted/octagonal Now Globe when the on-screen sphere was larger than the
 * capped texture bitmap. The renderer must always normalize geometry in bitmap space so the
 * texture body stays circular regardless of display radius/cap ratio.
 */
@RunWith(AndroidJUnit4::class)
class TextureEarthBodySilhouetteContractTest {

    @Test
    fun cappedTextureStillRendersAFullCircularDisc() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val assets = EarthMaterialAssets.load(context, EarthQuality.HIGH)
            ?: error("bundled Earth assets must load for silhouette contract")

        val rect = 256
        val bmp = renderEarthBody(
            assets = assets,
            rect = rect,
            centerX = 0,
            centerY = 0,
            // Deliberately much larger than rect/2 to reproduce the Round4 capped-texture condition.
            radiusPx = 720f,
            cam = GlobeCamera(yawDeg = 0f, pitchDeg = 30f, zoom = 1f),
        )

        val c = rect / 2
        fun alphaAt(x: Int, y: Int): Int = (bmp.getPixel(x, y) ushr 24) and 0xFF

        assertTrue("center must be textured", alphaAt(c, c) > 0)

        // Cardinal points well inside the nominal circle must stay textured even when display radius
        // is far larger than the capped bitmap radius.
        val cardinal = (rect * 0.43f).toInt()
        assertTrue("left silhouette must reach circular edge", alphaAt(c - cardinal, c) > 0)
        assertTrue("right silhouette must reach circular edge", alphaAt(c + cardinal, c) > 0)
        assertTrue("top silhouette must reach circular edge", alphaAt(c, c - cardinal) > 0)
        assertTrue("bottom silhouette must reach circular edge", alphaAt(c, c + cardinal) > 0)

        // Diagonal point outside x^2+y^2 <= r^2 must remain transparent: this distinguishes
        // a true circular disc from a square/faceted capped bitmap.
        val outside = (rect * 0.40f).toInt()
        assertEquals("outer diagonal must be transparent", 0, alphaAt(c + outside, c + outside))

        bmp.recycle()
    }
}
