package com.pdig.uivnext.evidence

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pdig.uivnext.VNextApp
import com.pdig.uivnext.VNextShellViewModel
import com.pdig.uivnext.createVNextAppState
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.model.VGlobeState
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.ui.PresentationProfileStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Source-complete interaction validation（任务书 §19/§20/§21/§22/§32/§33/§35）。
 *
 * 使用真实生产路径 [VNextShellViewModel]（真实 PresentationProfileStore /
 * WorkspacePreferenceStore）验证：
 * - card/number profile 保存 → 磁盘 store → 全新 ViewModel（模拟进程重启）恢复 → 渲染复用；
 * - workspace 偏好跨重建保留；
 * - Studio Save 按钮真实点击 → 落盘；
 * - Search 结果导航 + Back 回 Search；
 * - Region select / filter 传播 / clear；
 * - Card / Number Detail back stack；
 * - Region Drawer Back 仅关闭详情层并保留地区上下文；
 * - Change 三投影（After = Plan Projection）。
 */
@RunWith(AndroidJUnit4::class)
class SourceCompleteInteractionContractTest {

    @get:Rule
    val compose = createComposeRule()

    private fun target() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun app(): Application = target().applicationContext as Application

    private fun clearStores() {
        target().getSharedPreferences("pdig_ui_vnext_presentation", Context.MODE_PRIVATE)
            .edit().clear().commit()
        target().getSharedPreferences("pdig_ui_vnext_workspace", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    private fun cardCityProfile(): PresentationProfile =
        PresentationProfile.defaultFor("card", "card-cn-2", "region").copy(
            themeId = "city",
            backgroundValue = "city",
            material = "frosted",
            maskSensitive = true,
        )

    private fun numberTravelProfile(): PresentationProfile =
        PresentationProfile.defaultFor("phoneNumber", "num-cn-1", "country").copy(
            themeId = "travel",
            backgroundValue = "travel",
            material = "paper",
            maskSensitive = true,
        )

    @Test
    fun cardProfileSavePersistsAcrossRecreationAndRenders() {
        clearStores()
        val vm1 = VNextShellViewModel(app())
        vm1.app.savePresentationProfile(cardCityProfile())

        // 1) 保存后立即存在于真实 store
        val saved = PresentationProfileStore(target()).loadAll()["card::card-cn-2"]
        assertNotNull("profile must be in store after save", saved)
        assertEquals("city", saved!!.themeId)
        assertEquals("frosted", saved.material)
        assertTrue("mask must persist", saved.maskSensitive)

        // 2) 全新 ViewModel（= 进程重启后从磁盘重建）恢复同一 profile
        val vm2 = VNextShellViewModel(app())
        val restored = vm2.app.savedPresentationProfile("card", "card-cn-2")
        assertNotNull("restart must restore card profile", restored)
        assertEquals("city", restored!!.themeId)
        assertEquals("frosted", restored.material)
        assertTrue(restored.maskSensitive)

        // 3) 重启后的 Card Detail 渲染不崩溃且使用保存后的主题（identity 节点存在）
        vm2.app.openCard("card-cn-2")
        compose.setContent { VNextApp(vm2.app) }
        compose.waitForIdle()
        val identityNodes = compose.onAllNodesWithTag(VTestIds.CARD_DETAIL_IDENTITY, useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue("card detail identity must render", identityNodes.isNotEmpty())
        assertEquals("city", vm2.app.presentationProfile("card", "card-cn-2", "region").themeId)
    }

    @Test
    fun numberProfileSavePersistsAcrossRecreationAndRenders() {
        clearStores()
        val vm1 = VNextShellViewModel(app())
        vm1.app.savePresentationProfile(numberTravelProfile())

        val saved = PresentationProfileStore(target()).loadAll()["phoneNumber::num-cn-1"]
        assertNotNull("profile must be in store after save", saved)
        assertEquals("travel", saved!!.themeId)
        assertEquals("paper", saved.material)
        assertTrue("mask must persist", saved.maskSensitive)

        val vm2 = VNextShellViewModel(app())
        val restored = vm2.app.savedPresentationProfile("phoneNumber", "num-cn-1")
        assertNotNull("restart must restore number profile", restored)
        assertEquals("travel", restored!!.themeId)
        assertEquals("paper", restored.material)
        assertTrue(restored.maskSensitive)

        vm2.app.openNumber("num-cn-1")
        compose.setContent { VNextApp(vm2.app) }
        compose.waitForIdle()
        compose.onNodeWithTag(VTestIds.NUMBER_DETAIL_HERO, useUnmergedTree = true).fetchSemanticsNode()
        assertEquals("travel", vm2.app.presentationProfile("phoneNumber", "num-cn-1", "country").themeId)
    }

    @Test
    fun workspacePreferencesPersistAcrossRecreation() {
        clearStores()
        val vm1 = VNextShellViewModel(app())
        vm1.app.privacyMask = false
        vm1.app.reduceMotion = true
        vm1.app.railExpanded = false
        vm1.app.showUpcoming = false

        // VAppState setter 同步调用 persistWorkspacePreferences() → 真实 store
        val vm2 = VNextShellViewModel(app())
        assertFalse("privacyMask restored", vm2.app.privacyMask)
        assertTrue("reduceMotion restored", vm2.app.reduceMotion)
        assertFalse("railExpanded restored", vm2.app.railExpanded)
        assertFalse("showUpcoming restored", vm2.app.showUpcoming)
    }

    @Test
    fun studioUISaveFlowPersistsThroughSaveButton() {
        clearStores()
        val vm = VNextShellViewModel(app())
        vm.app.evidenceThemeId = "city"
        vm.app.openCardCustomization("card-cn-2")
        compose.setContent { VNextApp(vm.app) }
        compose.waitForIdle()

        val tiles = compose.onAllNodesWithTag(VTestIds.STUDIO_THEME_TILE, useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue("studio theme tiles must render", tiles.isNotEmpty())

        compose.onNodeWithText("保存", useUnmergedTree = true).performClick()
        compose.waitForIdle()

        val saved = PresentationProfileStore(target()).loadAll()["card::card-cn-2"]
        assertNotNull("save button must persist profile", saved)
        assertEquals("city", saved!!.themeId)
    }

    @Test
    fun searchResultNavigationAndBackToSearch() {
        val app = createVNextAppState()
        app.navigate(VScreen.SEARCH)
        app.navigateFromSearch(VScreen.CARDS)
        assertEquals("CARDS", app.screen.name)
        app.back()
        assertEquals("SEARCH", app.screen.name)
    }

    @Test
    fun searchQueryShowsCardResult() {
        val app = createVNextAppState().apply { navigate(VScreen.SEARCH) }
        compose.setContent { VNextApp(app) }
        compose.waitForIdle()
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(""))).performTextInput("招商")
        compose.waitForIdle()
        val results = compose.onAllNodesWithTag("pdig.search.result", useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue("query 招商 must produce results", results.isNotEmpty())
    }

    @Test
    fun regionSelectPropagatesAndClears() {
        val app = createVNextAppState()
        app.selectRegion("CN")
        assertEquals("CN", app.regionFilter)
        assertEquals(VGlobeState.REGION_SELECTED, app.globe.state)
        app.navigate(VScreen.CARDS)
        assertEquals("region filter must persist into Cards", "CN", app.regionFilter)
        app.navigate(VScreen.NUMBERS)
        assertEquals("region context consistent into Numbers", "CN", app.regionFilter)
        app.clearRegion()
        assertNull("clear restores global", app.regionFilter)
    }

    @Test
    fun cardDetailBackStackFlow() {
        val app = createVNextAppState().apply { navigate(VScreen.CARDS) }
        app.openCard("card-cn-2")
        app.openCardCustomization("card-cn-2")
        assertEquals("CARD_CUSTOMIZATION", app.screen.name)
        app.back()
        assertEquals("CARD_DETAIL", app.screen.name)
        app.back()
        assertEquals("CARDS", app.screen.name)
    }

    @Test
    fun numberDetailBackStackFlow() {
        val app = createVNextAppState().apply { navigate(VScreen.NUMBERS) }
        app.openNumber("num-cn-1")
        app.openNumberCustomization("num-cn-1")
        assertEquals("NUMBER_CUSTOMIZATION", app.screen.name)
        app.back()
        assertEquals("NUMBER_DETAIL", app.screen.name)
        app.back()
        assertEquals("NUMBERS", app.screen.name)
    }

    @Test
    fun regionDrawerBackClosesDetail() {
        val app = createVNextAppState().apply { navigate(VScreen.OVERVIEW) }
        app.selectRegion("CN")
        app.openRegionDetail()
        assertEquals(VGlobeState.REGION_DETAIL, app.globe.state)
        app.back()
        assertEquals(VGlobeState.REGION_SELECTED, app.globe.state)
        assertEquals("CN", app.regionFilter)
    }

    @Test
    fun changeProjectionStatesAreDistinctAndRender() {
        val app = createVNextAppState()
        app.changeProjection = "current"
        app.navigate(VScreen.CHANGE_PHONE)
        assertEquals("current", app.changeProjection)
        app.changeProjection = "transition"
        assertEquals("transition", app.changeProjection)
        app.changeProjection = "after"
        assertEquals("after", app.changeProjection)

        // After = Plan Projection（非现实）；渲染不崩溃且 NEW 区域存在
        compose.setContent { VNextApp(app) }
        compose.waitForIdle()
        compose.onNodeWithTag(VTestIds.CHANGE_NEW, useUnmergedTree = true).fetchSemanticsNode()
    }
}
