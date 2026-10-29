package com.pdig.uivnext.copy

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * PHASE 1F 空态/healthy 文案契约（§37–§40）：
 * 六个空态文案齐备、健康文案诚实（禁止「一切安全/100% 正常/无风险」）。
 */
class Phase1FEmptyCopyContractTest {

    private val allCopy = listOf(
        Phase1FEmptyCopy.CARDS_TITLE,
        Phase1FEmptyCopy.CARDS_BODY,
        Phase1FEmptyCopy.CARDS_CTA,
        Phase1FEmptyCopy.CARDS_SECONDARY,
        Phase1FEmptyCopy.NUMBERS_TITLE,
        Phase1FEmptyCopy.NUMBERS_BODY,
        Phase1FEmptyCopy.NUMBERS_CTA,
        Phase1FEmptyCopy.REGION_TITLE,
        Phase1FEmptyCopy.REGION_BODY,
        Phase1FEmptyCopy.NO_CHANGE_TITLE,
        Phase1FEmptyCopy.NO_CHANGE_BODY,
        Phase1FEmptyCopy.NO_ATTENTION_TITLE,
        Phase1FEmptyCopy.NO_ATTENTION_BODY,
        Phase1FEmptyCopy.NO_DEPS_TITLE,
        Phase1FEmptyCopy.NO_DEPS_BODY,
        Phase1FEmptyCopy.NO_BACKUP_TITLE,
        Phase1FEmptyCopy.NO_HISTORY_TITLE,
    )

    @Test
    fun sixEmptyStatesArePresent() {
        assertTrue(allCopy.contains(Phase1FEmptyCopy.CARDS_TITLE), "Cards Empty")
        assertTrue(allCopy.contains(Phase1FEmptyCopy.NUMBERS_TITLE), "Numbers Empty")
        assertTrue(allCopy.contains(Phase1FEmptyCopy.REGION_TITLE), "Region Empty")
        assertTrue(allCopy.contains(Phase1FEmptyCopy.NO_CHANGE_TITLE), "No Active Change")
        assertTrue(allCopy.contains(Phase1FEmptyCopy.NO_ATTENTION_TITLE), "No Attention")
        assertTrue(allCopy.contains(Phase1FEmptyCopy.NO_DEPS_TITLE), "No Known Dependencies")
    }

    @Test
    fun healthyCopyIsHonestNoFalseSafetyClaims() {
        val healthy = listOf(
            Phase1FEmptyCopy.NO_ATTENTION_TITLE,
            Phase1FEmptyCopy.NO_ATTENTION_BODY,
            Phase1FEmptyCopy.NO_CHANGE_BODY,
        ).joinToString("。")
        Phase1FEmptyCopy.BANNED_PHRASES.forEach { banned ->
            assertTrue(!healthy.contains(banned), "banned phrase in healthy copy: $banned")
        }
        assertTrue(healthy.contains(Phase1FEmptyCopy.HEALTHY_REQUIRED_FRAGMENT), "healthy copy must keep the honest unknown caveat")
    }

    @Test
    fun unknownIsNeverPresentedAsNone() {
        assertTrue(Phase1FEmptyCopy.NO_DEPS_BODY.contains("未知 ≠ 没有"), "No Known Dependencies must not claim absence")
        assertTrue(Phase1FEmptyCopy.REGION_BODY.contains("未知 ≠ 没有"), "Region Empty must not claim absence")
    }

    @Test
    fun emptyStatesProvidePrimaryCtas() {
        assertTrue(Phase1FEmptyCopy.CARDS_CTA.isNotBlank())
        assertTrue(Phase1FEmptyCopy.CARDS_SECONDARY.isNotBlank())
        assertTrue(Phase1FEmptyCopy.NUMBERS_CTA.isNotBlank())
    }
}
