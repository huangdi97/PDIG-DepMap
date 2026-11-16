package com.pdig.uivnext.evidence

import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
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
 * Compact card-image craft contract.
 *
 * Card personalization is intentionally a small consumer feature: one preview,
 * local gallery/built-in pictures, Save & return. Engineering theme/material/layout
 * controls must not reappear.
 */
@RunWith(AndroidJUnit4::class)
class PhoneStudioLayoutContractTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun compactCardDetail_keepsArtworkAsSecondaryMicroAction() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue("phone-only contract", ctx.resources.configuration.screenWidthDp < 600)

        val app = createVNextAppState().apply { openCard("card-cn-1") }
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 360) }
        compose.waitForIdle()

        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val action = compose.onNodeWithTag("pdig.card.detail.change-art", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val minTouch = with(compose.density) { 48.dp.toPx() }

        // 已知产品缺陷（run 43893296796）：该控件在语义树里 **0.0×0.0dp**，
        // 合并树（useUnmergedTree=false）与未合并树都是 0，根节点却是 411.4×841.5dp
        // —— 屏幕布局正常、控件零尺寸、不可点击。两处源码都挂了
        // `defaultMinSize(minHeight = 48.dp)`，最小尺寸未生效，原因待查。
        // 该用例暂不进 main 门禁；断言口径仍为「>= 48dp」，不降级。
        val density = compose.density.density
        val actionDp = action.height / density
        val minTouchDp = minTouch / density
        assertTrue(
            "card-art touch height must be >=${minTouchDp}dp (measured ${actionDp}dp)",
            action.height >= minTouch,
        )
        assertTrue(
            "card-art action must remain a minor action, not a full-width hero",
            action.width < root.width * 0.72f,
        )
    }

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
