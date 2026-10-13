package com.pdig.uivnext.ui

import com.pdig.uivnext.production.VNextProductionPlan
import com.pdig.uivnext.production.VNextProductionPlanAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionChangeStageSummaryTest {
    @Test
    fun maskedTargetBlocksNewCardOrPhonePlanButCanOpenExistingPlanReadOnly() {
        assertFalse(productionChangeEntryEnabled(false, false, false))
        assertFalse(productionChangeEntryEnabled(false, true, true))
        assertFalse(productionChangeEntryEnabled(false, false, true))
        assertTrue(productionChangeEntryEnabled(false, true, false))
        // Existing ChangePlan detail remains viewable, while its commands
        // independently respect the privacy gate tested below.
        assertTrue(productionChangeEntryEnabled(true, false, true))
        assertTrue(productionChangeEntryEnabled(true, true, true))
    }

    @Test
    fun maskedStepsCannotBeCompletedOrVerifiedBlindly() {
        assertFalse(productionChangeActionMutationEnabled(authorityAvailable = false, privacyMask = false))
        assertFalse(productionChangeActionMutationEnabled(authorityAvailable = false, privacyMask = true))
        assertFalse(productionChangeActionMutationEnabled(authorityAvailable = true, privacyMask = true))
        assertTrue(productionChangeActionMutationEnabled(authorityAvailable = true, privacyMask = false))
    }

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

        val change = plan.actions.last()
        val freshReady = plan.copy(
            lastAnalyzedGraphRevision = 9,
            currentGraphRevision = 9,
            readiness = "ready",
            unresolvedMustChangeKeys = emptyList(),
        )
        assertFalse(productionPlanExecutable(plan))
        assertTrue(productionPlanExecutable(freshReady))
        val completedPending = change.copy(done = true, verificationStatus = "pending")
        val suggested = completedPending.copy(verificationStatus = "evidence_suggested")
        assertTrue(productionPlanVerificationEnabled(freshReady, completedPending, true))
        assertTrue(productionPlanVerificationEnabled(freshReady, suggested, true))
        assertFalse(productionPlanVerificationEnabled(freshReady, completedPending, false))
        assertFalse(productionPlanVerificationEnabled(plan, completedPending, true))
        assertFalse(productionPlanVerificationEnabled(freshReady.copy(readiness = "blocked"), completedPending, true))
        assertFalse(productionPlanVerificationEnabled(freshReady.copy(effectiveState = "needs_revalidation"), completedPending, true))
        assertFalse(productionPlanVerificationEnabled(freshReady.copy(currentGraphRevision = 10), completedPending, true))
        assertFalse(productionPlanVerificationEnabled(freshReady, change, true))
        assertFalse(productionPlanVerificationEnabled(freshReady, completedPending.copy(verificationStatus = "verified"), true))
        assertFalse(productionPlanVerificationEnabled(freshReady, completedPending.copy(verificationStatus = "failed"), true))

        assertFalse(productionPlanExecutable(freshReady.copy(readiness = "blocked")))
        assertFalse(productionPlanExecutable(freshReady.copy(effectiveState = "needs_revalidation")))
        // Readiness alone cannot override a changed authoritative graph revision.
        assertFalse(productionPlanExecutable(plan.copy(readiness = "ready", effectiveState = null)))
        assertTrue(productionPlanActionGuidance(change, plan, true).contains("重新核对影响范围"))
        assertTrue(productionPlanActionGuidance(change, freshReady.copy(readiness = "blocked"), true)
            .contains("阻断事项"))
        assertTrue(productionPlanActionGuidance(change, freshReady, false).contains("只有查看权限"))
        assertTrue(productionPlanActionGuidance(change, freshReady, true).contains("不等于结果已验证"))

        val waiting = freshReady.copy(
            actions = listOf(
                freshReady.actions.first().copy(done = false),
                change,
            ),
        )
        assertTrue(productionPlanActionGuidance(change, waiting, true).contains("准备备用路径"))
        assertTrue(productionPlanActionGuidance(change.copy(done = true), freshReady, true)
            .contains("结果尚未验证"))
        assertTrue(productionPlanActionGuidance(
            change.copy(done = true, verificationStatus = "failed"), freshReady, true,
        ).contains("不会自动重试"))
        assertTrue(productionPlanActionGuidance(
            change.copy(done = true, verificationStatus = "verified"), freshReady, true,
        ).contains("已完成并验证"))
    }
}
