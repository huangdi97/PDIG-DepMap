package com.pdig.uivnext.ui

import com.pdig.uivnext.production.VNextProductionFinding
import com.pdig.uivnext.production.VNextProductionFindingType
import com.pdig.uivnext.production.VNextProjectionTruth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Do not let Finding evidence re-identify personal infrastructure while masked. */
class ProductionFindingPrivacyTest {
    private val finding = VNextProductionFinding(
        id = "finding:1",
        type = VNextProductionFindingType.SINGLE_POINT_OF_FAILURE,
        title = "「+852 6123 4567」只有一条已确认恢复来源",
        why = "工资卡关联 +852 6123 4567",
        confirmedBasis = "已确认关系：edge-secret-93",
        unknowns = "备用邮箱 example@domain.test 未确认",
        recommendedNextAction = "验证 HK-Main 能否找回工资卡",
        evidenceRefs = listOf("edge-secret-93"),
        truth = VNextProjectionTruth.DERIVED,
    )

    @Test
    fun globalMaskHidesAllUserControlledFindingDescriptionsButNotGovernedType() {
        val fields = listOf(
            finding.title to "连续性发现（已遮蔽）",
            finding.why to "原因说明已遮蔽",
            finding.confirmedBasis to "证据摘要已遮蔽",
            finding.unknowns to "未知项说明已遮蔽",
            finding.recommendedNextAction to "操作建议已遮蔽",
        )
        val masked = fields.map { (text, label) ->
            productionVisibleFindingText(text, privacyMask = true, maskedLabel = label)
        }
        assertEquals(fields.map { it.second }, masked)
        assertFalse(masked.joinToString().contains("+852"))
        assertFalse(masked.joinToString().contains("工资卡"))
        assertFalse(masked.joinToString().contains("edge-secret"))
        assertFalse(masked.joinToString().contains("example@domain"))
        assertEquals(VNextProductionFindingType.SINGLE_POINT_OF_FAILURE, finding.type)
        assertEquals(VNextProjectionTruth.DERIVED, finding.truth)
        assertEquals(1, finding.evidenceRefs.size)
    }

    @Test
    fun unmaskedModeRestoresExactFindingCopyAndMissingIsNotClaimedHidden() {
        val source = listOf(
            finding.title, finding.why, finding.confirmedBasis,
            finding.unknowns, finding.recommendedNextAction,
        )
        assertEquals(
            source,
            source.map { productionVisibleFindingText(it, false, "已遮蔽") },
        )
        assertEquals("未记录", productionVisibleFindingText("", true, "已遮蔽"))
        assertEquals("未记录", productionVisibleFindingText("   ", true, "已遮蔽"))
        assertTrue(productionVisibleFindingText(finding.why, true, "说明已遮蔽").contains("已遮蔽"))
    }
}
