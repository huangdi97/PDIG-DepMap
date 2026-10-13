package com.pdig.uivnext.ui

import com.pdig.uivnext.production.VNextProductionRecordItem
import com.pdig.uivnext.production.VNextProductionRecordState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionRecordTraceTest {
    private fun record(
        id: String,
        planId: String = "plan-a",
        state: VNextProductionRecordState,
        evidence: List<String> = emptyList(),
        occurredAt: String? = null,
    ) = VNextProductionRecordItem(
        id = id,
        planId = planId,
        planTitle = "更换手机号",
        scenario = "replace_phone_number",
        actionId = id,
        actionTitle = "验证新路径",
        phase = "verify",
        state = state,
        evidenceRefs = evidence,
        occurredAt = occurredAt,
    )

    @Test
    fun unknownOccurrenceTimeIsNeverFabricatedAndEvidenceCountIsGrounded() {
        val item = record("a", state = VNextProductionRecordState.VERIFIED)
        val projected = productionRecordPresentation(item)
        assertEquals("已验证", projected.stateLabel)
        assertEquals("无验证证据引用", projected.evidenceLabel)
        assertEquals("发生时间未记录", projected.timeLabel)

        val withEvidence = productionRecordPresentation(
            item.copy(evidenceRefs = listOf("e1", "e2"), occurredAt = "2026-10-11T09:00"),
        )
        assertEquals("验证证据 2 项", withEvidence.evidenceLabel)
        assertEquals("2026-10-11", withEvidence.timeLabel)
    }

    @Test
    fun completePendingAndFailedAreNotReportedAsVerified() {
        val states = listOf(
            VNextProductionRecordState.RECORDED_COMPLETE to "已记录完成",
            VNextProductionRecordState.PENDING_VERIFICATION to "待验证",
            VNextProductionRecordState.VERIFICATION_FAILED to "验证失败",
        )
        states.forEach { (state, label) ->
            assertEquals(label, productionRecordPresentation(record(state.name, state = state)).stateLabel)
        }
    }

    @Test
    fun maskingHidesUserSuppliedPlanAndStepNamesWithoutChangingAuthority() {
        val planTitle = "把 +852 6123 4567 从工资卡切走"
        val actionTitle = "验证工行尾号 8823 的支付路径"
        assertEquals(planTitle, productionVisiblePlanTitle(planTitle, false))
        assertEquals(actionTitle, productionVisibleActionTitle(actionTitle, false))
        assertEquals("变更计划（已遮蔽）", productionVisiblePlanTitle(planTitle, true))
        assertEquals("操作步骤（已遮蔽）", productionVisibleActionTitle(actionTitle, true))
        assertEquals("数据源（已遮蔽）", productionVisibleSourceTitle("个人工资导入", true))
        val record = record("secret", state = VNextProductionRecordState.VERIFIED)
        assertEquals("已验证", productionRecordPresentation(record).stateLabel)
        assertEquals("发生时间未记录", productionRecordPresentation(record).timeLabel)
        assertTrue(productionRecordsByPlan(listOf(record)).single().single() === record)
    }

    @Test
    fun localTargetMaskOrUnreadablePlanNeverLeaksRecordTitles() {
        // Global masking and per-object masking are combined upstream using
        // the authoritative ChangePlan target, not cached record text.
        assertEquals(false, productionRecordPlanMask(planReadable = true, targetMasked = false))
        assertEquals(true, productionRecordPlanMask(planReadable = true, targetMasked = true))
        assertEquals(true, productionRecordPlanMask(planReadable = false, targetMasked = false))
        assertEquals(true, productionRecordPlanMask(planReadable = false, targetMasked = true))

        val phoneTitle = "验证 +852 6123 4567 的恢复路径"
        val cardTitle = "迁移支付工具尾号 8823"
        assertEquals("变更计划（已遮蔽）",
            productionVisiblePlanTitle(phoneTitle, productionRecordPlanMask(true, true)))
        assertEquals("操作步骤（已遮蔽）",
            productionVisibleActionTitle(cardTitle, productionRecordPlanMask(false, false)))

        // Privacy only affects rendered titles: the verification authority and
        // evidence truth are unchanged.
        val item = record("verified", state = VNextProductionRecordState.VERIFIED,
            evidence = listOf("evidence-1"))
        assertEquals("已验证", productionRecordPresentation(item).stateLabel)
        assertEquals("验证证据 1 项", productionRecordPresentation(item).evidenceLabel)
    }

    @Test
    fun traceGroupsOnlyByActualPlanWithoutInventingChronologicalOrder() {
        val first = record("a1", planId = "a", state = VNextProductionRecordState.RECORDED_COMPLETE)
        val second = record("b1", planId = "b", state = VNextProductionRecordState.VERIFIED)
        val third = record("a2", planId = "a", state = VNextProductionRecordState.PENDING_VERIFICATION)
        val groups = productionRecordsByPlan(listOf(first, second, third))
        assertEquals(2, groups.size)
        assertEquals(listOf("a1", "a2"), groups[0].map { it.id })
        assertEquals(listOf("b1"), groups[1].map { it.id })
        assertTrue(productionRecordsByPlan(emptyList()).isEmpty())
    }
}
