package com.pdig.app.ui.screens

import com.pdig.app.data.NodeRow
import com.pdig.core.domain.ActionVerificationStatus
import com.pdig.core.generated.ImpactLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** UI 文案映射的纯 JVM 测试（spec §65：人话文案，不暴露内部枚举名）。 */
class UiLabelMappingsTest {

    @Test
    fun verificationLabelMapsEvidenceSuggestedToUserPrompt() {
        assertEquals("发现新的依据，请确认", verificationLabel(ActionVerificationStatus.EVIDENCE_SUGGESTED))
    }

    @Test
    fun verificationLabelKeepsOtherStatuses() {
        assertEquals("待验证", verificationLabel(ActionVerificationStatus.PENDING))
        assertEquals("已验证", verificationLabel(ActionVerificationStatus.VERIFIED))
        assertEquals("验证失败", verificationLabel(ActionVerificationStatus.FAILED))
        assertEquals("无需验证", verificationLabel(ActionVerificationStatus.NOT_REQUIRED))
        assertEquals("无需验证", verificationLabel(null))
    }

    @Test
    fun nodeKindGroupLabelUsesHumanGroupNames() {
        assertEquals("支付方式", nodeKindGroupLabel("payment_instrument"))
        assertEquals("收款对象", nodeKindGroupLabel("service"))
        assertEquals("其他", nodeKindGroupLabel("account"))
        assertEquals("其他", nodeKindGroupLabel("device"))
    }

    @Test
    fun groupedNodesSkipsEmptyGroupsAndOrdersStably() {
        val nodes = listOf(
            NodeRow("c1", "payment_instrument", "卡1", false, "{}"),
            NodeRow("s1", "service", "商户1", false, "{}"),
            NodeRow("s2", "service", "商户2", false, "{}"),
        )
        val groups = groupedNodes(nodes)
        assertEquals(listOf("支付方式", "收款对象"), groups.map { it.first })
        assertEquals(1, groups[0].second.size)
        assertEquals(2, groups[1].second.size)
    }

    @Test
    fun groupedNodesEmptyInputGivesEmptyGroups() {
        assertTrue(groupedNodes(emptyList()).isEmpty())
    }

    @Test
    fun impactCaptionIsStableAndNeverExposesWireValues() {
        for (level in ImpactLevel.entries) {
            val caption = impactCaption(level)
            assertFalse("blank caption for $level", caption.isBlank())
            assertFalse(caption.contains("must_change"))
            assertFalse(caption.contains("needs_review"))
            assertFalse(caption.contains("backup_path"))
        }
    }
}
