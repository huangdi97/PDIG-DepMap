package com.pdig.uivnext.evidence

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pdig.uivnext.VNextApp
import com.pdig.uivnext.createVNextAppState
import com.pdig.uivnext.model.VTestIds
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compact Studio craft contract.
 *
 * The phone Studio must remain preview-first and use a horizontally browsable theme gallery.
 * This prevents a regression back to the tall full-width settings-list presentation that consumed
 * most of the first viewport in the first source-complete runtime pack.
 */
@RunWith(AndroidJUnit4::class)
class PhoneStudioLayoutContractTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun compactCardStudio_usesHorizontalThemeGallery() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue("phone-only contract", ctx.resources.configuration.screenWidthDp < 600)

        val app = createVNextAppState().apply {
            evidenceThemeId = "glass"
            openCardCustomization("card-cn-2")
        }
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 360) }
        compose.waitForIdle()

        compose.onNodeWithTag(VTestIds.CUSTOMIZATION_PREVIEW, useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(VTestIds.CUSTOMIZATION_LIBRARY, useUnmergedTree = true).assertExists()

        val tiles = compose.onAllNodesWithTag(VTestIds.STUDIO_THEME_TILE, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .filter { it.boundsInRoot.width > 0f && it.boundsInRoot.height > 0f }
        assertTrue("compact gallery must lay out at least two theme tiles", tiles.size >= 2)

        val first = tiles[0].boundsInRoot
        val second = tiles[1].boundsInRoot
        val density = compose.density
        val sameRowTolerance = with(density) { 8.dp.toPx() }
        val maxTileWidth = with(density) { 180.dp.toPx() }

        assertTrue(
            "first two compact theme tiles must share one horizontal row",
            kotlin.math.abs(first.top - second.top) <= sameRowTolerance,
        )
        assertTrue("second tile must be to the right of first", second.left > first.left)
        assertTrue("compact theme tile must stay card-like rather than full-width", first.width <= maxTileWidth)
    }
}
