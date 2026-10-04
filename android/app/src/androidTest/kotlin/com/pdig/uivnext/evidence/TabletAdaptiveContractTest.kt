package com.pdig.uivnext.evidence
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pdig.uivnext.VNextApp
import com.pdig.uivnext.createVNextAppState
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.ui.VAppState
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * B8 TabletAdaptiveContractTest（brief §24/§10/§11）—— EXPANDED tablet-native composition：
 *  - Change Transition/After：OLD / SERVICE / NEW 三区首屏可见（three columns visible、无死空白、
 *    对象不被挤到折叠以下、线路不成为主角）；
 *  - Number Detail 无死空白（hero→services ≤ 48dp）；
 *  - Cards / Studio / Overview 真实渲染（face / preview / region list 存在）。
 * 仅 tablet 设备运行（phone 由 Phone*ContractTest 覆盖）。
 */
@RunWith(AndroidJUnit4::class)
class TabletAdaptiveContractTest {

    @get:Rule
    val compose = createComposeRule()

    private var contentSet = false
    private var slotApp by mutableStateOf<VAppState?>(null)

    private fun renderApp(app: VAppState) {
        if (!contentSet) {
            compose.setContent { slotApp?.let { VNextApp(it) } }
            contentSet = true
        }
        slotApp = app
        compose.waitForIdle()
    }

    @Test
    fun tabletChangeProjections_showThreeColumnsOnFirstScreen() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue("tablet-only contract", ctx.resources.configuration.screenWidthDp >= 600)

        for (projection in listOf("current", "transition", "after")) {
            val app = createVNextAppState().apply {
                changeProjection = projection
                navigate(VScreen.CHANGE_PHONE)
            }
            renderApp(app)

            val rootHeight = compose.onRoot().fetchSemanticsNode().size.height
            val old = compose.onNodeWithTag(VTestIds.CHANGE_OLD, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val services = compose.onNodeWithTag(VTestIds.CHANGE_SERVICES, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val new = compose.onNodeWithTag(VTestIds.CHANGE_NEW, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

            assertTrue("[$projection] OLD visible on first screen (bottom=${old.bottom} <= $rootHeight)", old.bottom <= rootHeight.toFloat())
            assertTrue("[$projection] SERVICES visible on first screen", services.bottom <= rootHeight.toFloat())
            assertTrue("[$projection] NEW visible on first screen", new.bottom <= rootHeight.toFloat())

            // 主角顺序 OLD → SERVICES → NEW（水平）
            assertTrue("[$projection] OLD must be left of SERVICES", old.right <= services.left)
            assertTrue("[$projection] SERVICES must be left of NEW", services.right <= new.left)

            // SERVICES 是主角（宽度占比最大 ≈0.42，允许观感误差）
            val servicesW = services.width
            val oldW = old.width
            assertTrue("[$projection] SERVICES must dominate width (services=$servicesW, old=$oldW)", servicesW > oldW)

            // 无大面死空白：三区顶部不垂下至折叠以下（顶部在视口上 80%）
            assertTrue("[$projection] scenes must not be pushed below fold (top=${old.top} <= ${rootHeight * 0.8f})", old.top <= rootHeight * 0.8f)
        }
    }

    @Test
    fun tabletInfrastructure_keepsPrimaryRailAndContentSecondaryNavigation() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue("tablet-only contract", ctx.resources.configuration.screenWidthDp >= 600)

        renderApp(createVNextAppState().apply { navigate(VScreen.CARDS) })
        compose.onNodeWithTag(VTestIds.NAV_RAIL, useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("pdig.nav.infra.secondary", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("pdig.nav.cards", useUnmergedTree = true).assertExists()
    }

    @Test
    fun tabletNumberDetail_noDeadSpace() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue("tablet-only contract", ctx.resources.configuration.screenWidthDp >= 600)
        val app = createVNextAppState().apply { openNumber("num-cn-1") }
        renderApp(app)

        val hero = compose.onNodeWithTag(VTestIds.NUMBER_DETAIL_HERO, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val services = compose.onNodeWithTag(VTestIds.NUMBER_DETAIL_SERVICES, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val gap = services.top - hero.bottom
        val density = compose.density
        val threshold = with(density) { 48.dp.toPx() }
        assertTrue("tablet number detail hero→services gap must be <= 48dp (gap=$gap)", gap <= threshold)
    }

    @Test
    fun tabletRadialScreens_renderRealContent() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue("tablet-only contract", ctx.resources.configuration.screenWidthDp >= 600)

        // Cards：网格 face 渲染
        renderApp(createVNextAppState().apply { navigate(VScreen.CARDS) })
        assertTrue("tablet cards face must render", compose.onAllNodesWithTag(VTestIds.CARD_FACE, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())

        // Card Detail：identity 列存在
        renderApp(createVNextAppState().apply { openCard("card-cn-2") })
        assertTrue(
            "tablet card detail identity column must render",
            compose.onAllNodesWithTag(VTestIds.CARD_DETAIL_IDENTITY, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty(),
        )

        // Card Studio（glass）：preview + library 存在
        renderApp(createVNextAppState().apply { evidenceThemeId = "glass"; openCardCustomization("card-cn-2") })
        compose.onNodeWithTag(VTestIds.CUSTOMIZATION_PREVIEW).assertExists()
        compose.onNodeWithTag(VTestIds.CUSTOMIZATION_LIBRARY).assertExists()
        assertTrue(
            "studio theme tiles must use consumer thumbnails",
            compose.onAllNodesWithTag(VTestIds.STUDIO_THEME_TILE, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty(),
        )

        // Overview：Region List 非视觉替代存在（Globe marker 的可访问等价物）
        renderApp(createVNextAppState().apply { navigate(VScreen.OVERVIEW) })
        compose.onNodeWithTag(VTestIds.GLOBE_STAGE).assertExists()
        compose.onNode(androidx.compose.ui.test.hasText("中国大陆")).assertExists()
    }
}