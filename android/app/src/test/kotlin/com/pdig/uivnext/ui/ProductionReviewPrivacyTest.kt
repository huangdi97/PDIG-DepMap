package com.pdig.uivnext.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionReviewPrivacyTest {
    @Test
    fun maskedReviewCannotEchoUnconfirmedNodeNamesOrRawEvidence() {
        val title = "待确认：+852 6123 4567 → 工资卡"
        val summary = "可能使用 example@domain.test 进行恢复"
        val evidence = "import-source-identity:raw-record-9932"
        val displayed = listOf(
            productionVisibleReviewText(title, true, "待复核对象（已遮蔽）"),
            productionVisibleReviewText(summary, true, "复核内容已遮蔽"),
            productionVisibleReviewText(evidence, true, "证据摘要已遮蔽"),
        )
        assertEquals(
            listOf("待复核对象（已遮蔽）", "复核内容已遮蔽", "证据摘要已遮蔽"),
            displayed,
        )
        assertFalse(displayed.joinToString().contains("+852"))
        assertFalse(displayed.joinToString().contains("example@"))
        assertFalse(displayed.joinToString().contains("raw-record"))
        assertFalse(productionReviewActionEnabled(true))
    }

    @Test
    fun unmaskedReviewShowsEvidenceBeforePermittingDecision() {
        assertEquals(
            "待确认：工资卡",
            productionVisibleReviewText("待确认：工资卡", false, "已遮蔽"),
        )
        assertTrue(productionReviewActionEnabled(false))
    }

    @Test
    fun missingReviewTextNeverTurnsIntoKnownButHiddenFact() {
        assertEquals("未记录", productionVisibleReviewText("", true, "已遮蔽"))
        assertEquals("未记录", productionVisibleReviewText("   ", false, "已遮蔽"))
    }
}
