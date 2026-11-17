package com.pdig.uivnext.evidence

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
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

        // 重要：`boundsInRoot` 会裁剪到根的范围。控件在折叠下方时读到的是 0x0，
        // 看起来像「零尺寸缺陷」——run 43893296796 就是这个假象。
        // logcat 的完整树（run 44023339937）证明该节点真实存在且尺寸正确：
        //   Node #148 (l=762,t=3927,r=1041,b=4053)px, Tag: pdig.card.detail.change-art
        //   Actions=[OnClick, RequestFocus], Text='[更换卡面图片 →]'
        //   → 279x126px @ density 2.625 = 106.3x48.0dp
        // 所以这里先把节点滚进视口，再测尺寸。
        val target = compose.onNodeWithTag("pdig.card.detail.change-art", useUnmergedTree = true)
        target.performScrollTo()

        val action = target.fetchSemanticsNode().boundsInRoot
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val density = compose.density.density
        val minTouch = with(compose.density) { 48.dp.toPx() }
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
