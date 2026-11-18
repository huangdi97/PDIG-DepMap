package com.pdig.uivnext.evidence

import android.os.Build
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pdig.uivnext.VNextApp
import com.pdig.uivnext.createVNextAppState
import com.pdig.uivnext.model.VTestIds
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Number Detail 布局探针（任务书 §25）：输出真实 dp 间隙值，绝不只写 PASS。
 *
 * 测量点（Phone + Tablet 各自运行）：
 * heroBottom → servicesHeaderTop
 * servicesHeaderBottom → firstServiceTop
 * service1Bottom → service2Top
 * service2Bottom → riskHeaderTop
 *
 * 重要：这是**测量探针**而不是跨屏契约。详情页比一屏高，未滚进视口的节点
 * `boundsInRoot` 会给出视口外坐标（同一父 Column 的布局偏移），因此
 * 「顺序」与「无死空白」两个不变式只在**两个端点同时位于视口内**时才成立。
 * 端点不同屏时不再断言顺序，改为在 JSON 里记 `inViewport=false`，
 * 保留真实数值供人工/矩阵复核——不把「测不到」伪装成「通过」。
 */
@RunWith(AndroidJUnit4::class)
class NumberDetailLayoutProbeTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun numberDetailLayoutProbe() {
        val app = createVNextAppState().apply { openNumber("num-cn-1") }
        compose.setContent { VNextApp(app) }
        compose.waitForIdle()

        val density = compose.density
        val viewport = compose.onRoot().fetchSemanticsNode().boundsInRoot

        data class Probe(val tag: String, val bounds: androidx.compose.ui.geometry.Rect) {
            // 视口内 = 高度非 0 且上下边都落在根节点范围内
            val inViewport: Boolean
                get() = bounds.height > 0f &&
                    bounds.top >= -0.5f &&
                    bounds.bottom <= viewportHeight + 0.5f
            var viewportHeight: Float = 0f
        }

        fun boundsOf(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot

        fun probe(tag: String): Probe =
            Probe(tag, boundsOf(tag)).also { it.viewportHeight = viewport.height }

        val riskHeaderBounds = compose.onNodeWithText("风险与恢复", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot

        val hero = probe(VTestIds.NUMBER_DETAIL_HERO)
        val servicesHeader = probe(VTestIds.NUMBER_DETAIL_SERVICES)
        val service0 = probe("pdig.number.detail.service.0")
        val service1 = probe("pdig.number.detail.service.1")
        val riskHeader = Probe("risk-header", riskHeaderBounds).also { it.viewportHeight = viewport.height }

        // 相邻对（只有两端都在视口内才断言顺序）
        val pairs = listOf(
            Triple("hero_to_servicesHeader", hero, servicesHeader),
            Triple("servicesHeader_to_service0", servicesHeader, service0),
            Triple("service0_to_service1", service0, service1),
            Triple("service1_to_riskHeader", service1, riskHeader),
        )

        val json = JSONObject()
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        json.put("screen", "number-detail")
        json.put(
            "deviceClass",
            if (resources.configuration.screenWidthDp >= 600) "tablet" else "phone",
        )
        json.put("api", Build.VERSION.SDK_INT)
        json.put("viewport", "${resources.displayMetrics.widthPixels}x${resources.displayMetrics.heightPixels}")
        json.put("density", resources.displayMetrics.density)
        json.put("orientation", if (resources.configuration.orientation == 2) "landscape" else "portrait")

        var assertedPairs = 0
        for ((label, from, to) in pairs) {
            val bothInViewport = from.inViewport && to.inViewport
            val gapPx = to.bounds.top - from.bounds.bottom
            json.put("${label}_dp", (gapPx / density.density).toDouble())
            json.put("${label}_bothInViewport", bothInViewport)
            if (bothInViewport) {
                assertedPairs++
                assertTrue("$label must keep reading order", from.bounds.bottom <= to.bounds.top)
                val gapDp = gapPx / density.density
                assertTrue("$label gap must be in (0, 200)dp, got $gapDp", gapDp > 0f && gapDp < 200f)
            }
        }
        json.put("assertedPairs", assertedPairs)
        // 探针必须至少证明一对相邻块已被真实测量，否则这次运行没有提供任何证据。
        assertTrue(
            "probe must measure at least one adjacent pair inside the viewport",
            assertedPairs >= 1,
        )

        val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "source-complete-probes")
            .apply { mkdirs() }
        File(dir, "NUMBER_DETAIL_LAYOUT_PROBE.json").writeText(json.toString(2))
    }
}
