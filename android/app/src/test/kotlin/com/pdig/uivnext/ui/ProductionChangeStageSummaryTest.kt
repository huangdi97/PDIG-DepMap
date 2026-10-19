package com.pdig.uivnext.ui

import com.pdig.uivnext.production.VNextProductionPlan
import com.pdig.uivnext.production.VNextProductionPlanAction
import com.pdig.uivnext.production.VNextProductionPlanImpactItem
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
    fun localCardAndPhoneMasksBlockNewPlanButLeaveExistingPlanBrowsable() {
        assertFalse(productionChangeMask("replace_payment_card", false, false, false))
        assertFalse(productionChangeMask("replace_phone_number", false, false, false))
        assertTrue(productionChangeMask("replace_payment_card", false, true, false))
        assertFalse(productionChangeMask("replace_payment_card", false, false, true))
        assertTrue(productionChangeMask("replace_phone_number", false, false, true))
        assertFalse(productionChangeMask("replace_phone_number", false, true, false))
        assertTrue(productionChangeMask("replace_phone_number", true, false, false))
        assertTrue(productionChangeMask("replace_payment_card", true, false, false))
        val cardMasked = productionChangeMask("replace_payment_card", false, true, false)
        val numberMasked = productionChangeMask("replace_phone_number", false, false, true)
        assertFalse(productionChangeEntryEnabled(false, true, cardMasked))
        assertFalse(productionChangeEntryEnabled(false, true, numberMasked))
        assertTrue(productionChangeEntryEnabled(true, false, cardMasked))
        assertTrue(productionChangeEntryEnabled(true, false, numberMasked))
    }

    @Test
    fun maskedStepsCannotBeCompletedOrVerifiedBlindly() {
        assertFalse(productionChangeActionMutationEnabled(authorityAvailable = false, privacyMask = false))
        assertFalse(productionChangeActionMutationEnabled(authorityAvailable = false, privacyMask = true))
        assertFalse(productionChangeActionMutationEnabled(authorityAvailable = true, privacyMask = true))
        assertTrue(productionChangeActionMutationEnabled(authorityAvailable = true, privacyMask = false))
    }

    @Test
    fun impactRowsChangeStateByStageWithoutInventingVerification() {
        val keyA = "account-a|recovery"
        val keyB = "service-b|authentication"
        val plan = VNextProductionPlan(
            id = "plan-impact",
            scenario = "replace_phone_number",
            title = "更换手机号",
            workflowState = "in_progress",
            effectiveState = null,
            baselineGraphRevision = 4,
            lastAnalyzedGraphRevision = 5,
            currentGraphRevision = 5,
            targetNodeId = "phone-old",
            targetNodeName = "旧手机号",
            effectiveDate = null,
            readiness = "review_required",
            affectedServiceCount = 2,
            mustChangeKeys = listOf(keyA),
            unresolvedMustChangeKeys = emptyList(),
            actions = listOf(
                VNextProductionPlanAction(
                    id = "change-a",
                    title = "迁移恢复方式",
                    phase = "change",
                    done = true,
                    verificationStatus = "pending",
                    verificationEvidenceRefs = emptyList(),
                    prerequisiteActionIds = emptyList(),
                    resolvesImpactKeys = listOf(keyA),
                ),
            ),
            impactItems = listOf(
                VNextProductionPlanImpactItem(
                    key = keyA,
                    nodeId = "account-a",
                    nodeName = "主账户",
                    capability = "recovery",
                    status = "must_change",
                ),
                VNextProductionPlanImpactItem(
                    key = keyB,
                    nodeId = "service-b",
                    nodeName = "登录服务",
                    capability = "authentication",
                    status = "needs_review",
                ),
            ),
        )

        val rows = productionChangeImpactRows(plan)
        val account = rows.first { it.key == keyA }
        assertEquals("必须处理", account.currentState)
        assertEquals("处理已记录", account.transitionState)
        assertEquals("计划已处理", account.afterState)
        // done does not become verified in the stage projection.
        assertFalse(account.afterState.contains("已验证"))

        val service = rows.first { it.key == keyB }
        assertEquals("需要核对", service.currentState)
        assertEquals("等待核对", service.transitionState)
        assertEquals("计划仍需核对", service.afterState)
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

        // A focused record must land on exactly its live action, including
        // every conditional header before the LazyColumn actions. It is only a
        // scroll target, not an execution permit or an invented action.
        assertEquals(6, productionFocusedActionLazyIndex(plan, "prepare", false))
        assertEquals(7, productionFocusedActionLazyIndex(plan, "change", false))
        assertEquals(8, productionFocusedActionLazyIndex(plan, "change", true))
        assertEquals(null, productionFocusedActionLazyIndex(plan, "deleted", false))
        assertEquals(null, productionFocusedActionLazyIndex(plan, null, false))
        assertEquals(6, productionFocusedActionLazyIndex(
            plan.copy(unresolvedMustChangeKeys = emptyList()), "change", false,
        ))

        val summary = productionChangeStageSummary(plan)
        assertTrue(summary.current.contains("当前数据版本 9"))
        assertTrue(summary.current.contains("分析基准 8"))
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

        // UI eligibility is advisory, but must fail closed on every stale
        // re-read before calling the authoritative AppContainer gateway.
        assertTrue(productionPlanCompletionEnabled(freshReady, change, true))
        assertFalse(productionPlanCompletionEnabled(freshReady, change, false))
        assertFalse(productionPlanCompletionEnabled(plan, change, true))
        assertFalse(productionPlanCompletionEnabled(freshReady.copy(readiness = "blocked"), change, true))
        assertFalse(productionPlanCompletionEnabled(freshReady.copy(effectiveState = "needs_revalidation"), change, true))
        assertFalse(productionPlanCompletionEnabled(freshReady.copy(currentGraphRevision = 10), change, true))
        assertFalse(productionPlanCompletionEnabled(freshReady, change.copy(done = true), true))
        assertFalse(productionPlanCompletionEnabled(
            freshReady.copy(actions = listOf(freshReady.actions.first().copy(done = false), change)),
            change, true,
        ))
        assertFalse(productionPlanCompletionEnabled(
            freshReady.copy(actions = listOf(change)), change, true,
        )) // a missing prerequisite is not an implicitly satisfied prerequisite
        val completedPending = change.copy(done = true, verificationStatus = "pending")
        val suggested = completedPending.copy(verificationStatus = "evidence_suggested")
        assertTrue(productionPlanVerificationEnabled(freshReady, completedPending, true))
        assertTrue(productionPlanVerificationEnabled(freshReady, suggested, true))
        assertFalse(productionPlanVerificationEnabled(freshReady, completedPending, false))
        assertFalse(productionPlanVerificationEnabled(plan, completedPending, true))
        assertTrue(productionPlanVerificationEnabled(freshReady.copy(readiness = "blocked"), completedPending, true))
        assertFalse(productionPlanVerificationEnabled(freshReady.copy(effectiveState = "needs_revalidation"), completedPending, true))
        assertFalse(productionPlanVerificationEnabled(freshReady.copy(currentGraphRevision = 10), completedPending, true))
        assertFalse(productionPlanVerificationEnabled(freshReady, change, true))
        assertFalse(productionPlanVerificationEnabled(freshReady, completedPending.copy(verificationStatus = "verified"), true))
        assertFalse(productionPlanVerificationEnabled(freshReady, completedPending.copy(verificationStatus = "failed"), true))
        assertFalse(productionPlanVerificationEnabled(
            freshReady, completedPending.copy(done = false), true,
        ))
        assertTrue(productionPlanVerificationEnabled(
            freshReady.copy(readiness = "blocked"), completedPending, true,
        ))

        val blockerKey = change.resolvesImpactKeys.single()
        val blockedWithWork = freshReady.copy(
            readiness = "blocked",
            unresolvedMustChangeKeys = listOf(blockerKey),
        )
        assertTrue(
            productionPlanCompletionEnabled(blockedWithWork, change, true),
            "A fresh CHANGE action that explicitly resolves the current blocker must remain executable.",
        )
        val prepare = freshReady.actions.first().copy(done = false)
        assertTrue(
            productionPlanCompletionEnabled(
                blockedWithWork.copy(actions = listOf(prepare, change)),
                prepare,
                true,
            ),
            "PREPARE steps must remain usable while must-change blockers exist.",
        )
        val unrelated = change.copy(
            id = "unrelated",
            resolvesImpactKeys = emptyList(),
            prerequisiteActionIds = emptyList(),
        )
        assertFalse(
            productionPlanCompletionEnabled(
                blockedWithWork.copy(actions = listOf(unrelated)),
                unrelated,
                true,
            ),
            "Blocked plans must not open unrelated CHANGE actions.",
        )
        assertTrue(
            productionPlanVerificationEnabled(
                blockedWithWork,
                completedPending,
                true,
            ),
            "A completed step can be verified while other must-change work remains.",
        )

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
