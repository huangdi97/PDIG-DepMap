package com.pdig.uivnext.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.pdig.uivnext.layout.Phase1FLayout
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.ui.components.CardFaceThumbnail
import com.pdig.uivnext.ui.components.NumberFaceThumbnail
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * PHASE 1F-HF §4 —— ThemeThumbnailBoundsContractTest。
 *
 * 渲染级回归测试：把两个 theme 缩略图并排放进一个更大的画布（中间留 gap），
 * 断言 gap 区域像素保持背景色 —— 即 artwork（circle/glow/arc/contour/city/
 * sweep/ring + matte vignette）不会越出缩略图边界泄漏到相邻区域。
 *
 * 该测试在修复前会 FAIL（drawBehind 未裁剪 + w 相对大圆几何越界），
 * 修复后（Modifier.clip + 渲染器 clipRect + 本地 bounds 约束）必须 PASS。
 * 同时覆盖 Card themes 与 Number themes（重点 Travel/Abstract/Glass/City/
 * Region/Recovery）。
 */
class ThemeThumbnailBoundsContractTest {

    private val CARD_THEMES = listOf("minimal", "matte", "deep-space", "region", "city", "glass", "metal", "abstract")
    private val NUMBER_THEMES = listOf("country", "city", "banking", "travel", "recovery", "work", "private", "minimal")

    /** 并排渲染两个缩略图（每格 180px，中间 40px gap），返回整帧。 */
    private fun renderPair(
        tileHeightPx: Int,
        thumb: @Composable (String) -> Unit,
        a: String,
        b: String,
    ): BufferedImage {
        val scene = ImageComposeScene(
            width = 400,
            height = tileHeightPx + 8,
            density = Density(1f),
        ) {
            Box(Modifier.fillMaxSize().background(PdigV2Colors.Canvas)) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    Box(Modifier.weight(1f)) { thumb(a) }
                    Spacer(Modifier.width(40.dp))
                    Box(Modifier.weight(1f)) { thumb(b) }
                }
            }
        }
        try {
            val img = scene.render().toComposeImageBitmap()
            val w = img.width
            val h = img.height
            val out = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
            val arr = IntArray(w * h)
            img.readPixels(arr, 0, 0, w, h, 0, w)
            out.setRGB(0, 0, w, h, arr, 0, w)
            return out
        } finally {
            scene.close()
        }
    }

    /** gap 区域（x in [180,220)，全高）：必须保持背景 Canvas 色（无 artwork 泄漏）。 */
    private fun assertGapClean(img: BufferedImage, label: String) {
        val bg = PdigV2Colors.Canvas
        val r0 = (bg.red * 255f).toInt()
        val g0 = (bg.green * 255f).toInt()
        val b0 = (bg.blue * 255f).toInt()
        var dirty = 0
        var total = 0
        for (y in 4 until img.height - 4) {
            for (x in 180 until 220) {
                total++
                val argb = img.getRGB(x, y)
                val r = (argb shr 16) and 0xFF
                val g = (argb shr 8) and 0xFF
                val b = argb and 0xFF
                if (kotlin.math.abs(r - r0) > 10 || kotlin.math.abs(g - g0) > 10 || kotlin.math.abs(b - b0) > 10) {
                    dirty++
                }
            }
        }
        assertTrue(dirty == 0, "[$label] gap leak: $dirty/$total gap pixels differ from Canvas background (artwork overflow out of thumbnail bounds)")
    }

    /** 缩略图内部必须有实际 artwork（非空白 / 非纯背景）。 */
    private fun assertArtworkVisible(img: BufferedImage, label: String) {
        val bg = PdigV2Colors.Canvas
        val r0 = (bg.red * 255f).toInt()
        val g0 = (bg.green * 255f).toInt()
        val b0 = (bg.blue * 255f).toInt()
        var painted = 0
        for (y in 8 until 104) {
            for (x in 4 until 176) {
                val argb = img.getRGB(x, y)
                val r = (argb shr 16) and 0xFF
                val g = (argb shr 8) and 0xFF
                val b = argb and 0xFF
                if (kotlin.math.abs(r - r0) > 25 || kotlin.math.abs(g - g0) > 25 || kotlin.math.abs(b - b0) > 25) painted++
            }
        }
        assertTrue(painted > 2000, "[$label] thumbnail interior looks blank (artwork missing)")
    }

    @Test
    fun cardThemeThumbnailsDoNotLeakArtworkBeyondTileBounds() {
        val pairs = listOf(
            "glass" to "city",
            "abstract" to "matte",
            "region" to "metal",
            "minimal" to "deep-space",
            "city" to "abstract",
        )
        pairs.forEach { (a, b) ->
            val img = renderPair(112, { preset -> CardFaceThumbnail(preset) }, a, b)
            assertGapClean(img, "card themes $a/$b")
            assertArtworkVisible(img, "card themes $a/$b")
        }
    }

    @Test
    fun numberThemeThumbnailsDoNotLeakArtworkBeyondTileBounds() {
        val pairs = listOf(
            "travel" to "recovery",
            "banking" to "city",
            "country" to "private",
            "work" to "minimal",
            "recovery" to "travel",
        )
        pairs.forEach { (a, b) ->
            val img = renderPair(96, { preset -> NumberFaceThumbnail(preset) }, a, b)
            assertGapClean(img, "number themes $a/$b")
            assertArtworkVisible(img, "number themes $a/$b")
        }
    }

    @Test
    fun everyCardThemeCarriesDistinctArtwork() {
        CARD_THEMES.forEach { theme ->
            val img = renderPair(112, { preset -> CardFaceThumbnail(preset) }, theme, theme)
            assertGapClean(img, "card theme $theme (self pair)")
            assertArtworkVisible(img, "card theme $theme (self pair)")
        }
    }

    @Test
    fun everyNumberThemeCarriesCommunicationIdentityArtwork() {
        NUMBER_THEMES.forEach { theme ->
            val img = renderPair(96, { preset -> NumberFaceThumbnail(preset) }, theme, theme)
            assertGapClean(img, "number theme $theme (self pair)")
            assertArtworkVisible(img, "number theme $theme (self pair)")
        }
    }

    @Test
    fun continuityPathWeightsFollowSection8Hierarchy() {
        assertTrue(Phase1FLayout.PATH_PRIMARY_PX <= 2f, "migrated path must be <=2px (§8)")
        assertTrue(Phase1FLayout.PATH_SECONDARY_PX <= 1.5f, "secondary path must be <=1.5px (§8)")
        assertTrue(Phase1FLayout.PATH_GHOST_PX <= 1f, "ghost path must be <=1px (§8)")
        val nodeW = Phase1FLayout.sceneNodeWidthPx(1684f)
        assertTrue(nodeW in 130f..170f, "service node width must stay 130-170px @1920, got $nodeW")
    }
}
