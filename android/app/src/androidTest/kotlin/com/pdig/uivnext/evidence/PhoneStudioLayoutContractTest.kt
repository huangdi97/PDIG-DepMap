package com.pdig.uivnext.evidence

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.printToLog
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

        val density = compose.density.density
        val rootNode = compose.onRoot(useUnmergedTree = true).fetchSemanticsNode()
        val root = rootNode.boundsInRoot
        val action = compose.onNodeWithTag("pdig.card.detail.change-art", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val minTouch = with(compose.density) { 48.dp.toPx() }

        // 已知产品缺陷（run 43893296796）：该控件在语义树里 0.0x0.0dp，合并树/未合并树一致，
        // 根节点 411.4x841.5dp 正常。这里再把带 tag 的节点及其子节点转储出来，
        // 判断它到底是「空壳」还是「有内容但被压成 0」。
        val dump = StringBuilder()
        fun walk(node: SemanticsNode, depth: Int) {
            if (dump.length > 2400 || depth > 4) return
            val b = node.boundsInRoot
            val tag = node.config.getOrNull(SemanticsProperties.TestTag)
            val texts = node.config.getOrNull(SemanticsProperties.Text)
            val label = texts?.joinToString("|") { it.text } ?: ""
            val w = b.width / density
            val h = b.height / density
            dump.append("  ".repeat(depth))
            if (tag != null) dump.append("#").append(tag).append(" ")
            if (label.isNotEmpty()) dump.append("T[").append(label.take(18)).append("] ")
            dump.append(String.format("%.1fx%.1f", w, h)).append("\n")
            node.children.forEach { walk(it, depth + 1) }
        }
        walk(rootNode, 0)
        // 完整树走 logcat（断言消息会被截断；logcat 在应用被卸载后仍可读）。
        compose.onRoot(useUnmergedTree = true).printToLog("PROBE_TREE")
        val actionDp = action.height / density
        val minTouchDp = minTouch / density
        // 树只放进失败消息（日志会截断，但至少给出前若干层）。
        // 想拿完整树：用 `onRoot().printToLog("PROBE_TREE")` + 同一 action 内
        // `adb logcat -d -s PROBE_TREE`（logcat 在应用卸载后仍在，run-as 不行）。
        assertTrue(
            "card-art touch height must be >=${minTouchDp}dp (measured ${actionDp}dp)" +
                " root ${root.width / density}x${root.height / density}dp tree=[$dump]",
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
