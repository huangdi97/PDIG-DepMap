package com.pdig.uivnext.evidence

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pdig.uivnext.VNextApp
import com.pdig.uivnext.createVNextAppState
import com.pdig.uivnext.model.VTestIds
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * B5 NumberDetailVerticalFlowContractTest（brief §8/§21）——
 * Identity Hero → 关联服务 必须连续（heroBottom → servicesTop 间距 ≤ token threshold，禁止数百 dp 死空白）。
 */
@RunWith(AndroidJUnit4::class)
class NumberDetailVerticalFlowContractTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun numberDetailVerticalFlowHasNoDeadSpace() {
        val app = createVNextAppState().apply { openNumber("num-cn-1") }
        compose.setContent { VNextApp(app) }
        compose.waitForIdle()

        val hero = compose.onNodeWithTag(VTestIds.NUMBER_DETAIL_HERO, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val services = compose.onNodeWithTag(VTestIds.NUMBER_DETAIL_SERVICES, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val gap = services.top - hero.bottom
        val density = compose.density
        val threshold = with(density) { 48.dp.toPx() }
        assertTrue(
            "heroBottom→servicesTop gap must be <= 48dp (gap=$gap, threshold=$threshold)",
            gap <= threshold,
        )
        assertTrue("section order must be preserved (hero above services)", hero.bottom <= services.top)
    }
}