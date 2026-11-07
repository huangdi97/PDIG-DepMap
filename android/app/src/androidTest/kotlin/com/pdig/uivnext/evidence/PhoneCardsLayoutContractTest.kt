package com.pdig.uivnext.evidence

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pdig.uivnext.VNextApp
import com.pdig.uivnext.createVNextAppState
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * B3 PhoneCardsLayoutContractTest（brief §4/§21）—— COMPACT 卡面布局契约：
 *  - NO_VERTICAL_TEXT   任何卡面文本（nickname/issuer/masked/meta）不得成竖排块（height ≤ width）；
 *  - NO_FORM_LABEL_STACK 实体/虚拟卡 form 标签必须与 nickname 同行带（不得被挤到下一行堆叠）；
 *  - NO_CRITICAL_CLIP    masked 与 meta 文本必须完整落在卡面 bounds 内；
 *  - CARD_MIN_READABLE_WIDTH 卡面宽度 ≥ 280dp（1 列整卡，禁止两列挤压）。
 * 仅手机设备运行（tablet 由 TabletAdaptiveContractTest 覆盖）。
 */
@RunWith(AndroidJUnit4::class)
class PhoneCardsLayoutContractTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun compactCardsNoVerticalText_noStack_noClip_minWidth() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue("phone-only contract", ctx.resources.configuration.screenWidthDp < 600)
        val app = createVNextAppState().apply { navigate(VScreen.CARDS) }
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 360) }
        compose.waitForIdle()

        // Human-selected light reference defaults compact Cards to the visual list.
        compose.onNodeWithTag(VTestIds.CARD_LIST, useUnmergedTree = true).assertExists()
        // Full card-face geometry is still a supported user mode and remains contract-tested.
        compose.onNodeWithTag(VTestIds.CARD_VIEW_TOGGLE, useUnmergedTree = true).performClick()
        compose.waitForIdle()

        val faceNodes = compose.onAllNodesWithTag(VTestIds.CARD_FACE, useUnmergedTree = true).fetchSemanticsNodes()
        val nicknameNodes = compose.onAllNodesWithTag(VTestIds.CARD_NICKNAME, useUnmergedTree = true).fetchSemanticsNodes()
        val issuerNodes = compose.onAllNodesWithTag(VTestIds.CARD_ISSUER, useUnmergedTree = true).fetchSemanticsNodes()
        val formNodes = compose.onAllNodesWithTag(VTestIds.CARD_FORM, useUnmergedTree = true).fetchSemanticsNodes()
        val maskedNodes = compose.onAllNodesWithTag(VTestIds.CARD_MASKED, useUnmergedTree = true).fetchSemanticsNodes()
        val metaNodes = compose.onAllNodesWithTag(VTestIds.CARD_META, useUnmergedTree = true).fetchSemanticsNodes()

        assertTrue("cards grid must render at least one card face", faceNodes.isNotEmpty())
        assertTrue("nickname must render on every face", nicknameNodes.size == faceNodes.size)
        assertTrue("issuer must render on every face", issuerNodes.size == faceNodes.size)
        assertTrue("form label must render on every face", formNodes.size == faceNodes.size)
        val density = compose.density
        val minWidthPx = with(density) { 280.dp.toPx() }
        val laidOut = faceNodes.mapIndexedNotNull { i, face ->
            if (face.boundsInRoot.width > 0f && i < nicknameNodes.size) (face to i) else null
        }
        assertTrue("at least one card face must be laid out", laidOut.isNotEmpty())
        laidOut.forEach { (face, i) ->
            // CARD_MIN_READABLE_WIDTH
            assertTrue("face #$i width ${face.boundsInRoot.width}px must be >= 280dp", face.boundsInRoot.width >= minWidthPx)

            val nickname = nicknameNodes[i].boundsInRoot
            val issuer = issuerNodes[i].boundsInRoot
            val form = formNodes[i].boundsInRoot
            val masked = maskedNodes[i].boundsInRoot
            val meta = metaNodes[i].boundsInRoot
            val faceBounds = face.boundsInRoot

            // NO_VERTICAL_TEXT：文本块不得是竖排（height > width 即说明被窄列挤成堆叠）
            assertTrue("nickname not vertical (h=${nickname.height}, w=${nickname.width})", nickname.height <= nickname.width)
            assertTrue("issuer not vertical", issuer.height <= issuer.width)
            assertTrue("masked not vertical", masked.height <= masked.width)
            assertTrue("meta not vertical", meta.height <= meta.width)

            // NO_FORM_LABEL_STACK：form 与 nickname 同行带（form.top ≤ nickname.bottom + 16dp 容差）
            val bandTolerance = with(density) { 16.dp.toPx() }
            assertTrue(
                "form label must sit in nickname band (form.top=${form.top}, nickname.bottom=${nickname.bottom})",
                form.top <= nickname.bottom + bandTolerance,
            )

            // NO_CRITICAL_CLIP：masked / meta 在卡面 bounds 内
            assertTrue("masked inside face (top=${masked.top} >= ${faceBounds.top})", masked.top >= faceBounds.top - 1f)
            assertTrue("masked inside face (bottom=${masked.bottom} <= ${faceBounds.bottom})", masked.bottom <= faceBounds.bottom + 1f)
            assertTrue("meta inside face", meta.bottom <= faceBounds.bottom + 1f)
            assertTrue("meta inside face", meta.top >= faceBounds.top - 1f)
        }

        // NODE 级完整性：face 存在即证明 grid 已渲染（无空 grid）
        compose.onNodeWithTag(VTestIds.CARD_GRID).assertExists()
    }
}