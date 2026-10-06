package com.pdig.uivnext.evidence

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pdig.uivnext.VNextApp
import com.pdig.uivnext.createVNextAppState
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * D5 Back / State 回归（brief §31）：system back 不得直接退出（有内部层级时），逐级返回。
 * 覆盖：Cards→Detail→Back、Detail→Studio→Back、Numbers→Detail→Back、
 * Search→destination→Back、Change projection→Back。
 */
@RunWith(AndroidJUnit4::class)
class VNextBackStateRegressionTest {

    @get:Rule
    val compose = createComposeRule()

    private fun render(app: com.pdig.uivnext.ui.VAppState) {
        compose.setContent { VNextApp(app) }
        compose.waitForIdle()
    }

    @Test
    fun cardsDetailBack_drivesRealClick() {
        val app = createVNextAppState().apply { navigate(VScreen.CARDS) }
        render(app)
        // Compact Cards 默认是高密度资产列表；点击真实 row → Detail。
        compose.onAllNodesWithTag(VTestIds.CARD_ROW, useUnmergedTree = true)[0].performClick()
        compose.waitForIdle()
        assertEquals(VScreen.CARD_DETAIL, app.screen)
        assertTrue("must have internal back target", app.canGoBack())
        app.back()
        assertEquals(VScreen.CARDS, app.screen)
        assertTrue("root after back must be non-null-free (no system exit)", !app.canGoBack())
    }

    @Test
    fun detailStudioBack_returnsToDetail() {
        val app = createVNextAppState().apply { openCard("card-cn-2") }
        render(app)
        app.openCardCustomization("card-cn-2")
        compose.waitForIdle()
        assertEquals(VScreen.CARD_CUSTOMIZATION, app.screen)
        app.back()
        assertEquals(VScreen.CARD_DETAIL, app.screen)
        app.back()
        assertEquals(VScreen.NOW, app.screen) // detail was opened from Now
    }

    @Test
    fun numbersDetailBack_returnsToList() {
        val app = createVNextAppState().apply { navigate(VScreen.NUMBERS) }
        render(app)
        app.openNumber("num-cn-1")
        compose.waitForIdle()
        assertEquals(VScreen.NUMBER_DETAIL, app.screen)
        app.back()
        assertEquals(VScreen.NUMBERS, app.screen)
    }

    @Test
    fun searchDestinationBack_returnsToSearch() {
        val app = createVNextAppState().apply { navigate(VScreen.SEARCH) }
        render(app)
        assertEquals(VScreen.SEARCH, app.screen)
        // Search → Card 目的地
        app.openCard("card-cn-1")
        compose.waitForIdle()
        assertEquals(VScreen.CARD_DETAIL, app.screen)
        app.back()
        assertEquals(VScreen.SEARCH, app.screen)
        assertEquals("search's own back target must still point to previous root", VScreen.NOW, app.navBackTarget)
    }

    @Test
    fun changeProjectionBack_returnsToPrevious() {
        val app = createVNextAppState()
        render(app)
        app.navigate(VScreen.CHANGE_PHONE)
        compose.waitForIdle()
        assertEquals(VScreen.CHANGE_PHONE, app.screen)
        app.back()
        assertEquals(VScreen.NOW, app.screen)
    }
}