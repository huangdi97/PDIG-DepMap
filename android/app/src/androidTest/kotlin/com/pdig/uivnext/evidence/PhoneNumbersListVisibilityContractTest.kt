package com.pdig.uivnext.evidence

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pdig.uivnext.VNextApp
import com.pdig.uivnext.createVNextAppState
import com.pdig.uivnext.demo.demoNumbers
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * B4 PhoneNumbersListVisibilityContractTest（brief §7/§21）—— COMPACT Numbers 结构契约：
 *  - expected count = 7（fixture 号码数；不得裁剪数据）；
 *  - visible rows >= 3（首屏真实可见 ≥3 行）；
 *  - list visible = true（PHONE_LIST 存在且可渲染）；
 *  - inspector on compact = false（COMPACT 不嵌 Desktop Inspector；点击行进入 Number Detail）。
 * 仅手机设备运行。
 */
@RunWith(AndroidJUnit4::class)
class PhoneNumbersListVisibilityContractTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun compactNumbersListVisible_inspectorAbsent_rowsVisible() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue("phone-only contract", ctx.resources.configuration.screenWidthDp < 600)
        val app = createVNextAppState().apply { navigate(VScreen.NUMBERS) }
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 360) }
        compose.waitForIdle()

        // 列表可见
        compose.onNodeWithTag(VTestIds.PHONE_LIST).assertExists()

        // COMPACT 不嵌 Desktop Inspector
        compose.onNodeWithTag(VTestIds.PHONE_INSPECTOR, useUnmergedTree = true).assertDoesNotExist()

        val rows = compose.onAllNodesWithTag(VTestIds.NUMBER_ROW, useUnmergedTree = true).fetchSemanticsNodes()
        // expected count = 7：fixture 数据量为 7（列表懒加载，首屏只组合可视行；数据不得裁剪）
        assertEquals("expected count = 7（fixture 号码数）", 7, app.demoNumbers().size)
        // 首屏可见行 ≥3（行 bottom 在可视区内；root 高度以内）
        val rootHeight = compose.onRoot().fetchSemanticsNode().size.height
        val visibleRows = rows.count { it.boundsInRoot.bottom <= rootHeight.toFloat() && it.boundsInRoot.top >= 0f }
        assertTrue("compact list must compose visible rows (composed=${rows.size})", rows.isNotEmpty())
        assertTrue("visible rows on first screen must be >= 3 (visible=$visibleRows)", visibleRows >= 3)
    }
}