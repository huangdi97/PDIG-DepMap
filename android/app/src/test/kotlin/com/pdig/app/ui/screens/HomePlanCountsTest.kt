package com.pdig.app.ui.screens

import com.pdig.app.data.PlanRow
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Home "需要你处理" 计划聚合计数的纯 JVM 测试（goal §16/§17）。
 * 计数规则与 UI 解耦，确保 review_required / verifying 等状态被计入。
 */
class HomePlanCountsTest {

    private fun plan(workflowState: String) = PlanRow(
        id = "p-1",
        title = "更换银行卡",
        scenario = "replace_payment_card",
        workflowState = workflowState,
        lastAnalyzedRevision = 0,
        effectiveDate = null,
    )

    @Test
    fun reviewRequiredPlansCountAsNeedingHandling() {
        val plans = listOf(plan("review_required"), plan("completed"))
        assertEquals(1, plansNeedingHandling(plans))
    }

    @Test
    fun verifyingPlansCountAsNeedingHandling() {
        val plans = listOf(plan("verifying"), plan("ready"), plan("cancelled"))
        assertEquals(1, plansNeedingHandling(plans))
    }

    @Test
    fun blockedAndNeedsRevalidationAlsoCount() {
        val plans = listOf(plan("blocked"), plan("needs_revalidation"), plan("draft"))
        assertEquals(2, plansNeedingHandling(plans))
    }

    @Test
    fun settledPlansDoNotCount() {
        val plans = listOf(
            plan("draft"), plan("analyzed"), plan("ready"),
            plan("in_progress"), plan("completed"), plan("cancelled"),
        )
        assertEquals(0, plansNeedingHandling(plans))
    }

    @Test
    fun emptyPlansGiveZero() {
        assertEquals(0, plansNeedingHandling(emptyList()))
    }
}
