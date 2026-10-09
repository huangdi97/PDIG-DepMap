package com.pdig.uivnext.evidence

import androidx.compose.ui.test.junit4.createComposeRule
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
    fun compactCardCustomization_staysASimpleImageFeature() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue("phone-only contract", ctx.resources.configuration.screenWidthDp < 600)

        val app = createVNextAppState().apply {
            evidenceThemeId = "ocean"
            openCardCustomization("card-cn-2")
        }
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 360) }
        compose.waitForIdle()

        compose.onNodeWithTag(VTestIds.CUSTOMIZATION_PREVIEW, useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(VTestIds.CUSTOMIZATION_LIBRARY, useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("pdig.r10.card-art.choose-photo", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("pdig.r10.card-art.choice.original", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("pdig.r10.card-art.choice.ocean", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("pdig.r10.card-art.choice.night", useUnmergedTree = true).assertExists()

        val photo = compose.onNodeWithTag("pdig.r10.card-art.choose-photo", useUnmergedTree = true)
            .fetchSemanticsNode()
        val minTouch = with(compose.density) { 48.dp.toPx() }
        assertTrue("gallery picker must keep >=48dp touch target", photo.boundsInRoot.height >= minTouch)
    }
}
