package com.pdig.uivnext.evidence

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pdig.uivnext.VNextApp
import com.pdig.uivnext.createVNextAppState
import com.pdig.uivnext.model.VScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManualEstablishR24ContractTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun compactManualRecordExplainsRealityAndRelationBoundary() {
        val app = createVNextAppState(screen = VScreen.MANUAL_ADD)
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 360) }
        compose.waitForIdle()

        compose.onNodeWithTag("pdig.r24.manual-establish", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithTag("pdig.r24.manual.truth-boundary", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithText(
            "你确认的对象可以成为 Reality；关系仍要单独确认",
            useUnmergedTree = true,
        ).assertIsDisplayed()
        compose.onNodeWithTag("pdig.r24.manual.preview-disabled", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithTag("pdig.r10.top.back", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun mediumManualRecordDoesNotPretendUnsupportedIdentitySubtypeExists() {
        val app = createVNextAppState(screen = VScreen.MANUAL_ADD)
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 700) }
        compose.waitForIdle()

        compose.onNodeWithText("号码 / 邮箱 / 身份", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithText("等待 governed subtype / mapping", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithText("当前 Preview 不提供“保存”按钮", useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
