package com.pdig.uivnext.layout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * PHASE 1F 布局契约（§15/§29/§30/§32）：
 * Studio 预览 660–740px；ContinuityScene 尺寸与路径层级；Cards 3 列。
 */
class Phase1FLayoutContractTest {

    /** @1920 中央 pane 宽 ≈ 1688*0.55。 */
    private val studioCenter1920 = 1688f * 0.55f

    @Test
    fun studioPreviewWidthIn660To740() {
        listOf(studioCenter1920, 1200f, 900f, 760f).forEach { center ->
            val w = Phase1FLayout.studioPreviewWidthPx(center)
            assertTrue(w in 660f..740f, "preview $w not in 660..740 for center $center")
        }
    }

    @Test
    fun continuitySceneDimensionsMatchSection29() {
        val sceneWidth = 1684f // @1920: 1920 - rail 188 - padding 48
        val m = Phase1FLayout.continuityScene()
        val numW = Phase1FLayout.sceneNumberWidthPx(sceneWidth)
        val nodeW = Phase1FLayout.sceneNodeWidthPx(sceneWidth)
        assertTrue(numW in 250f..300f, "OLD/NEW identity width $numW (target 250–300)")
        assertTrue(nodeW in 130f..170f, "service node width $nodeW (target 130–170)")
        assertTrue(m.heightPx in 420..480, "scene height ${m.heightPx} (target 420–480)")
    }

    @Test
    fun pathHierarchyPrimaryOverSecondaryOverGhost() {
        assertTrue(Phase1FLayout.PATH_PRIMARY_PX > Phase1FLayout.PATH_SECONDARY_PX)
        assertTrue(Phase1FLayout.PATH_SECONDARY_PX > Phase1FLayout.PATH_GHOST_PX)
        assertEquals(2.5f, Phase1FLayout.PATH_PRIMARY_PX)
        assertEquals(2.0f, Phase1FLayout.PATH_SECONDARY_PX)
        assertEquals(1.5f, Phase1FLayout.PATH_GHOST_PX)
    }

    @Test
    fun cardsGridStaysThreeColumnsAt1920() {
        assertEquals(3, Phase1FLayout.CARDS_COLUMNS_1920)
        assertTrue(Phase1FLayout.CARD_ASPECT_RATIO in 1.5f..1.65f)
    }
}
