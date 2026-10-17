package com.pdig.uivnext.evidence

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pdig.uivnext.ui.components.StudioKind
import com.pdig.uivnext.ui.components.ThemeThumbnail
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

/**
 * B12 StudioThemeThumbnailDistinct（brief §13/§21）—— 主题 thumbnail 视觉差异契约：
 * Human 必须一眼分辨 Glass / City / Metal / Region 等主题；不能只靠 selected label。
 * 同一台设备上 glass 与 city 的 thumbnail 渲染必须产生可测量（非仅 SHA）的像素差异
 * （diffRatio ≥ 1%），同时 SHA256 不同。
 */
@RunWith(AndroidJUnit4::class)
class StudioThumbnailDistinctTest {

    @get:Rule
    val compose = createComposeRule()

    @Composable
    private fun ThumbRow(glassTag: String, cityTag: String) {
        Column {
            ThemeThumbnail(StudioKind.CARD, "glass", Modifier.testTag(glassTag))
            ThemeThumbnail(StudioKind.CARD, "city", Modifier.testTag(cityTag))
        }
    }

    @Test
    fun glassAndCityThumbnails_areVisuallyDistinct() {
        compose.setContent { ThumbRow("thumb-glass", "thumb-city") }
        compose.waitForIdle()

        val glassBmp = compose.onNodeWithTag("thumb-glass", useUnmergedTree = true).captureToImage().asAndroidBitmap()
        val cityBmp = compose.onNodeWithTag("thumb-city", useUnmergedTree = true).captureToImage().asAndroidBitmap()

        assertTrue("glass thumbnail must be non-empty", glassBmp.width > 0 && glassBmp.height > 0)
        assertTrue("city thumbnail must be non-empty", cityBmp.width > 0 && cityBmp.height > 0)

        val shaGlass = sha256(glassBmp)
        val shaCity = sha256(cityBmp)
        assertTrue("glass and city thumbnail SHA256 must differ (glass=$shaGlass, city=$shaCity)", shaGlass != shaCity)

        assertEqualsSafe(glassBmp.width, cityBmp.width)
        assertEqualsSafe(glassBmp.height, cityBmp.height)
        val diffRatio = pixelDiffRatio(glassBmp, cityBmp)
        assertTrue("glass vs city thumbnail visual diff ratio must be >= 1% (ratio=$diffRatio)", diffRatio >= 0.01)
    }

    private fun assertEqualsSafe(a: Int, b: Int) {
        assertTrue("thumbnail dimensions must match (${a} vs ${b})", a == b)
    }

    private fun sha256(bmp: android.graphics.Bitmap): String {
        val out = ByteArrayOutputStream()
        bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(out.toByteArray())
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun pixelDiffRatio(a: android.graphics.Bitmap, b: android.graphics.Bitmap): Double {
        val w = minOf(a.width, b.width)
        val h = minOf(a.height, b.height)
        var diff = 0L
        var total = 0L
        for (y in 0 until h) {
            for (x in 0 until w) {
                val pa = a.getPixel(x, y)
                val pb = b.getPixel(x, y)
                total++
                val dr = ((pa shr 16) and 0xFF) - ((pb shr 16) and 0xFF)
                val dg = ((pa shr 8) and 0xFF) - ((pb shr 8) and 0xFF)
                val db = (pa and 0xFF) - (pb and 0xFF)
                if (dr * dr + dg * dg + db * db > 900) diff++ // ΔRGB² > 30²
            }
        }
        return if (total > 0) diff.toDouble() / total else 0.0
    }
}