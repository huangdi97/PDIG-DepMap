package com.pdig.uivnext.ui

import com.pdig.uivnext.production.VNextProductionPlan
import com.pdig.uivnext.production.VNextProductionPlanAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionChangeStageSummaryTest {
    @Test
    fun currentAndTransitionFollowAuthoritativePlanWhileAfterRemainsOnlyProjection() {
        val plan = VNextProductionPlan(
            id = "plan-1",
            scenario = "replace_phone_number",
            title = "更换手机号",
            workflowState = "in_progress",
            effectiveState = null,
            baselineGraphRevision = 7,
            lastAnalyzedGraphRevision = 8,
            currentGraphRevision = 9,
            targetNodeId = "phone-old",
            targetNodeName = "旧手机号",
            effectiveDate = null,
            readiness = "blocked",
            affectedServiceCount = 2,
            mustChangeKeys = listOf("account-1:recovery"),
            unresolvedMustChangeKeys = listOf("account-1:recovery"),
            actions = listOf(
                VNextProductionPlanAction(
                    id = "prepare",
                    title = "准备备用路径",
                    phase = "prepare",
                    done = true,
                    verificationStatus = "verified",
                    verificationEvidenceRefs = emptyList(),
                    prerequisiteActionIds = emptyList(),
                    resolvesImpactKeys = emptyList(),
                ),
                VNextProductionPlanAction(
                    id = "change",
                    title = "迁移服务",
                    phase = "change",
                    done = false,
                    verificationStatus = "pending",
                    verificationEvidenceRefs = emptyList(),
                    prerequisiteActionIds = listOf("prepare"),
                    resolvesImpactKeys = listOf("account-1:recovery"),
                ),
            ),
        )

        val summary = productionChangeStageSummary(plan)
        assertTrue(summary.current.contains("当前图谱修订 9"))
        assertTrue(summary.current.contains("分析基线 8"))
        assertTrue(summary.transition.contains("1/2 步已记录完成"))
        assertTrue(summary.transition.contains("1 步已验证"))
        assertTrue(summary.after.contains("1 项必须处理事项未解决"))
        assertTrue(summary.after.contains("仅计划预期"))

        val readyProjection = productionChangeStageSummary(
            plan.copy(unresolvedMustChangeKeys = emptyList()),
        )
        assertEquals("计划预期 · 完成并验证前不代表现实", readyProjection.after)
    }
}
