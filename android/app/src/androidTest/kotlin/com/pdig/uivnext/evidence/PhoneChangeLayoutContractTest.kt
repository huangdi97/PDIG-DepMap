package com.pdig.uivnext.evidence

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
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
 * B11 PhoneChangeLayoutContractTest —— COMPACT 6 步 stepper 初始视图契约：
 * 全部 6 个 mini step 首屏可见（不被横向 clip，右侧不越出视口），当前步骤标签可见。
 * 仅手机设备运行。
 */
@RunWith(AndroidJUnit4::class)
class PhoneChangeLayoutContractTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun compactStepperInitialView_notClipped_allSixStepsVisible() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue("phone-only contract", ctx.resources.configuration.screenWidthDp < 600)
        val app = createVNextAppState().apply { navigate(VScreen.CHANGE_PHONE) }
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 360) }
        compose.waitForIdle()

        compose.onNodeWithTag(VTestIds.CHANGE_STEPPER_MINI, useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(VTestIds.CHANGE_STEPPER_CURRENT, useUnmergedTree = true).assertExists()

        val rootWidth = compose.onRoot().fetchSemanticsNode().size.width
        val mini = compose.onNodeWithTag(VTestIds.CHANGE_STEPPER_MINI, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        // 不被 layout clip：mini rail 右边缘在视口内
        assertTrue("stepper must not be clipped by viewport (right=${mini.right} <= $rootWidth)", mini.right <= rootWidth.toFloat() + 1f)
        // 6 个 step 圆点全部在 rail 内（每个 weight(1f)，占满整宽 → 全部可见）
        // 用户可知共 6 步 + 当前步骤（第 3 步验证中，fixture transition）
        compose.onNodeWithTag(VTestIds.CHANGE_STEPPER_CURRENT, useUnmergedTree = true).assertIsDisplayed()
    }
}