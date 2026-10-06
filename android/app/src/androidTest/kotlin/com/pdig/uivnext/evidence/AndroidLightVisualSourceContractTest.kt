package com.pdig.uivnext.evidence

import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pdig.uivnext.VNextApp
import com.pdig.uivnext.createVNextAppState
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Human-selected Android Light Visual Reference 的结构防回退契约。
 *
 * 这不是像素验收，也不把生成参考图当 functional truth；只守住已经落地的 presentation
 * hierarchy，避免后续实现重新退回深色 dashboard / generic rows / form-like migration。
 */
@RunWith(AndroidJUnit4::class)
class AndroidLightVisualSourceContractTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun compactTopBar_usesBrandOnNow_andContextTitleOnFocusedPages() {
        val app = createVNextAppState(screen = VScreen.NOW)
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 360) }
        compose.waitForIdle()

        compose.onNodeWithTag("pdig.nav.top.brand", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("PDIG", useUnmergedTree = true).assertIsDisplayed()

        app.navigate(VScreen.CARDS)
        compose.waitForIdle()
        compose.onNodeWithTag("pdig.nav.top.title", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("卡片", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag(VTestIds.CARD_LIST, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun compactInfrastructure_isManagementHub_notSecondNow() {
        val app = createVNextAppState(screen = VScreen.NOW)
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 360) }
        compose.waitForIdle()

        val nowGlobe = compose.onNodeWithTag(VTestIds.NOW_GLOBE, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot

        app.navigate(VScreen.OVERVIEW)
        compose.waitForIdle()

        compose.onNodeWithTag("pdig.overview.infrastructure-hub", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("pdig.nav.infra.secondary", useUnmergedTree = true).assertIsNotDisplayed()
        val overviewGlobe = compose.onNodeWithTag(VTestIds.GLOBE_STAGE, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Phone Infrastructure should keep a smaller regional globe than Now's world-view hero",
            overviewGlobe.height < nowGlobe.height,
        )

        app.navigate(VScreen.CARDS)
        compose.waitForIdle()
        compose.onNodeWithTag("pdig.nav.infra.secondary", useUnmergedTree = true).assertIsNotDisplayed()
        compose.onNodeWithTag(VTestIds.CARD_LIST, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun compactNumbers_areCommunicationIdentityAssets() {
        val app = createVNextAppState(screen = VScreen.NUMBERS)
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 360) }
        compose.waitForIdle()

        val thumbnails = compose
            .onAllNodesWithTag("pdig.number.identity.thumbnail", useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue("Numbers must render communication-identity thumbnails", thumbnails.isNotEmpty())
    }

    @Test
    fun compactChangePhone_isContinuityScene_notThreeIndependentForms() {
        val app = createVNextAppState(screen = VScreen.CHANGE_PHONE).apply {
            changeProjection = "transition"
        }
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 360) }
        compose.waitForIdle()

        compose.onNodeWithTag(VTestIds.CHANGE_OLD, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag(VTestIds.CHANGE_SERVICES, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag(VTestIds.CHANGE_NEW, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag(VTestIds.CHANGE_STEPPER_MINI, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun supportingSurfaces_keepLightReferenceHierarchy() {
        val app = createVNextAppState(screen = VScreen.RECORDS)
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 360) }
        compose.waitForIdle()
        compose.onNodeWithTag("pdig.records.summary", useUnmergedTree = true).assertIsDisplayed()

        app.navigate(VScreen.PERSONALIZATION)
        compose.waitForIdle()
        compose.onNodeWithTag("pdig.personalization.visual-preview", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun detailAndTruthSurfaces_areAssetFirstAndExplicit() {
        val app = createVNextAppState().apply { openNumber("num-cn-1") }
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 360) }
        compose.waitForIdle()
        compose.onNodeWithTag("pdig.number.detail.summary", useUnmergedTree = true).assertIsDisplayed()

        app.openCard("card-cn-2")
        compose.waitForIdle()
        compose.onNodeWithTag("pdig.card.detail.summary", useUnmergedTree = true).assertIsDisplayed()

        app.openUtility(VScreen.SOURCES)
        compose.waitForIdle()
        compose.onNodeWithTag("pdig.sources.boundary-hero", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun secondaryInfrastructure_pagesUseAssetIdentitySurfaces() {
        val app = createVNextAppState(screen = VScreen.ACCOUNTS)
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 360) }
        compose.waitForIdle()

        listOf(
            VScreen.ACCOUNTS,
            VScreen.EMAILS,
            VScreen.DEVICES,
            VScreen.SERVICES,
            VScreen.WEAKNESSES,
        ).forEach { screen ->
            app.navigate(screen)
            compose.waitForIdle()
            assertTrue(
                "$screen must render at least one light-reference infrastructure object card",
                compose.onAllNodesWithTag("pdig.infra.object-card", useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty(),
            )
        }
    }

    @Test
    fun personalization_declaresLightThemeAsCurrent() {
        val app = createVNextAppState(screen = VScreen.PERSONALIZATION)
        compose.setContent { VNextApp(app, forcedViewportWidthDp = 360) }
        compose.waitForIdle()

        compose.onNodeWithText("亮色", useUnmergedTree = true).assertIsDisplayed()
        assertTrue(
            "personalization must mark current visual choices",
            compose.onAllNodesWithText("当前", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty(),
        )
    }
}
