package com.pdig.uivnext.evidence

import android.os.Build
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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

        fun bounds(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot

        val hero = bounds(VTestIds.NUMBER_DETAIL_HERO)
        val servicesHeader = bounds(VTestIds.NUMBER_DETAIL_SERVICES)
        val service0 = bounds("pdig.number.detail.service.0")
        val service1 = bounds("pdig.number.detail.service.1")
        val riskHeader = compose.onNodeWithText("风险", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val gapHeroToServices = servicesHeader.top - hero.bottom
        val gapServicesToFirst = service0.top - servicesHeader.bottom
        val gapService1To2 = service1.top - service0.bottom
        val gapService2ToRisk = riskHeader.top - service1.bottom

        // 顺序不变式（真实数值而非仅 PASS）
        assertTrue("hero must be above services header", hero.bottom <= servicesHeader.top)
        assertTrue("services header must be above first service", servicesHeader.bottom <= service0.top)
        assertTrue("service0 must be above service1", service0.bottom <= service1.top)
        assertTrue("service1 must be above risk header", service1.bottom <= riskHeader.top)

        // 无死空白：每个间隙都必须落在 (0, 200)dp 有界合理区间
        val gaps = listOf(
            "heroBottom_to_servicesHeaderTop" to gapHeroToServices,
            "servicesHeaderBottom_to_firstServiceTop" to gapServicesToFirst,
            "service1Bottom_to_service2Top" to gapService1To2,
            "service2Bottom_to_riskHeaderTop" to gapService2ToRisk,
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
        json.put("pxDensity", resources.displayMetrics.density)
        for ((label, gapPx) in gaps) {
            val gapDp = gapPx / density.density
            json.put("${label}_dp", gapDp.toDouble())
            assertTrue("$label gap must be in (0, 200)dp, got $gapDp", gapDp > 0f && gapDp < 200f)
        }

        val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "source-complete-probes")
            .apply { mkdirs() }
        File(dir, "NUMBER_DETAIL_LAYOUT_PROBE.json").writeText(json.toString(2))
    }
}
