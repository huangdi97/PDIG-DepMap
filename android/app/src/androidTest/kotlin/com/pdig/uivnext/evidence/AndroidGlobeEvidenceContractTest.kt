package com.pdig.uivnext.evidence

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pdig.uivnext.VNextApp
import com.pdig.uivnext.createVNextAppState
import com.pdig.uivnext.globe.GlobeRenderState
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.ui.VAppState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.sqrt

/**
 * B7 AndroidGlobeEvidenceContractTest（brief §10/§23）—— Phone + Tablet Globe 渲染证据契约：
 * 前置条件：GlobeRenderState == TEXTURE_READY（合理 timeout，超时 FAIL）—— 禁止截 near-black empty sphere。
 * 像素断言（disc 半径 = controller.renderRadiusPx；仅捕获「明显失败的近黑渲染」，不过拟合）：
 *  - earth non-empty（采样点 ≥500）
 *  - mean luminance ≥ 0.12
 *  - non-black ratio（luma>0.12）≥ 0.30
 *  - surface variance（luma stddev）≥ 0.04
 *  - region marker visible（蓝色主导的亮像素 ≥5）
 */
@RunWith(AndroidJUnit4::class)
class AndroidGlobeEvidenceContractTest {

    @get:Rule
    val compose = createComposeRule()

    // 阈值命名：捕获 near-black failed render（黑球 disc：mean≈0.06-0.10 / nonBlack<0.2 / stddev≈0.01-0.02 / markers=0）
    private val MIN_LUMA = 0.12
    private val MIN_NON_BLACK_RATIO = 0.30
    private val MIN_STDDEV = 0.04
    private val MIN_MARKERS = 5
    private val MIN_SAMPLES = 500
    private val TEXTURE_TIMEOUT_MS = 20_000L

    @Test
    fun globeMustReachTextureReady_andEarthMustBeNonNearBlack() {
        val app = createVNextAppState().apply { reduceMotion = true }
        compose.setContent { VNextApp(app) }
        compose.waitForIdle()

        // 前置条件：TEXTURE_READY（超时 → FAIL，禁止黑球截图 PASS）
        val deadline = System.currentTimeMillis() + TEXTURE_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            compose.waitForIdle()
            if (app.globe.renderState == GlobeRenderState.TEXTURE_READY) break
            Thread.sleep(150)
        }
        assertTrue(
            "globe must reach TEXTURE_READY within 20s (state=${app.globe.renderState})",
            app.globe.renderState == GlobeRenderState.TEXTURE_READY,
        )

        compose.waitForIdle()
        val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
        val canvasBounds = compose.onNodeWithTag(VTestIds.GLOBE_CANVAS, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

        val cx = canvasBounds.center.x
        val cy = canvasBounds.center.y
        val radius = app.globe.renderRadiusPx
        assertTrue("render radius must be positive ($radius)", radius > 0f)

        val m = analyzeDisc(bmp, cx, cy, radius)
        assertTrue("earth disc must be non-empty (samples=${m.samples}, radius=$radius)", m.samples >= MIN_SAMPLES)
        assertTrue("earth mean luminance >= 0.12 (mean=${m.meanLuma})", m.meanLuma >= MIN_LUMA)
        assertTrue("earth non-black ratio >= 0.30 (ratio=${m.nonBlackRatio})", m.nonBlackRatio >= MIN_NON_BLACK_RATIO)
        assertTrue("earth surface variance >= 0.04 (stddev=${m.stddev})", m.stddev >= MIN_STDDEV)
        assertTrue("region markers visible (count=${m.markers})", m.markers >= MIN_MARKERS)
    }

    private data class GlobeMetrics(
        val meanLuma: Double,
        val nonBlackRatio: Double,
        val stddev: Double,
        val markers: Int,
        val samples: Int,
    )

    private fun analyzeDisc(bmp: Bitmap, cx: Float, cy: Float, r: Float): GlobeMetrics {
        val r2 = r * r
        var sum = 0.0
        var sumSq = 0.0
        var nonBlack = 0
        var markers = 0
        var n = 0
        val cxi = cx.toInt()
        val cyi = cy.toInt()
        val ri = r.toInt()
        for (y in (cyi - ri)..(cyi + ri)) {
            for (x in (cxi - ri)..(cxi + ri)) {
                if (x < 0 || y < 0 || x >= bmp.width || y >= bmp.height) continue
                val dx = x - cx
                val dy = y - cy
                if (dx * dx + dy * dy > r2) continue
                val argb = bmp.getPixel(x, y)
                val red = (argb shr 16) and 0xFF
                val green = (argb shr 8) and 0xFF
                val blue = argb and 0xFF
                val luma = (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0
                sum += luma
                sumSq += luma * luma
                n++
                if (luma > 0.12) nonBlack++
                // region markers：蓝主导高亮像素（锚点是 bright blue，非陆地/云/夜灯）
                if (luma > 0.35 && blue > red + 40 && blue > green + 25) markers++
            }
        }
        val mean = if (n > 0) sum / n else 0.0
        val variance = if (n > 0) sumSq / n - mean * mean else 0.0
        return GlobeMetrics(
            meanLuma = mean,
            nonBlackRatio = if (n > 0) nonBlack.toDouble() / n else 0.0,
            stddev = sqrt(maxOf(0.0, variance)),
            markers = markers,
            samples = n,
        )
    }
}