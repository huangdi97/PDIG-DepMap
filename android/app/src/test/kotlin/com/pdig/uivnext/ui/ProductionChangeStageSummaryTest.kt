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
    fun phoneRetirementRequiresRepositoryProjectedScopeReadiness() {
        val migration = VNextProductionPlanAction(
            id = "rpn-change-1",
            title = "迁移关键账户",
            phase = "change",
            done = true,
            verificationStatus = "verified",
            verificationEvidenceRefs = emptyList(),
            prerequisiteActionIds = emptyList(),
            resolvesImpactKeys = emptyList(),
        )
        val verify = VNextProductionPlanAction(
            id = "rpn-verify-1",
            title = "验证新手机号恢复路径",
            phase = "verify",
            done = true,
            verificationStatus = "verified",
            verificationEvidenceRefs = emptyList(),
            prerequisiteActionIds = emptyList(),
            resolvesImpactKeys = emptyList(),
        )
        val retirement = VNextProductionPlanAction(
            id = "rpn-change-2",
            title = "停用旧手机号",
            phase = "change",
            done = false,
            verificationStatus = null,
            verificationEvidenceRefs = emptyList(),
            prerequisiteActionIds = listOf("rpn-verify-1"),
            resolvesImpactKeys = emptyList(),
        )
        fun plan(scopeReady: Boolean?) = VNextProductionPlan(
            id = "plan-retirement",
            scenario = "replace_phone_number",
            title = "更换手机号",
            workflowState = "in_progress",
            effectiveState = null,
            baselineGraphRevision = 9,
            lastAnalyzedGraphRevision = 9,
            currentGraphRevision = 9,
            targetNodeId = "phone-old",
            targetNodeName = "旧手机号",
            effectiveDate = null,
            readiness = "review_required",
            affectedServiceCount = 0,
            mustChangeKeys = emptyList(),
            unresolvedMustChangeKeys = emptyList(),
            actions = listOf(migration, verify, retirement),
            retirementScopeReady = scopeReady,
        )

        assertFalse(productionPlanCompletionEnabled(plan(false), retirement, true))
        assertFalse(productionPlanCompletionEnabled(plan(null), retirement, true))
        assertTrue(productionPlanCompletionEnabled(plan(true), retirement, true))
        assertTrue(
            productionPlanActionGuidance(retirement, plan(false), true)
                .contains("影响范围仍需处理或核对"),
        )
    }

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
        assertEquals("处理已记录 · 待验证", account.transitionState)
        assertEquals("计划仍待验证", account.afterState)
        // done does not become verified in the stage projection.
        assertFalse(account.afterState.contains("已验证"))

        val service = rows.first { it.key == keyB }
        assertEquals("需要核对", service.currentState)
        assertEquals("等待核对", service.transitionState)
        assertEquals("计划仍需核对", service.afterState)

        val failed = productionChangeImpactRows(
            plan.copy(
                actions = plan.actions.map {
                    if (it.id == "change-a") it.copy(verificationStatus = "failed") else it
                },
            ),
        ).first { it.key == keyA }
        assertEquals("处理已记录 · 验证未通过", failed.transitionState)
        assertEquals("计划验证未通过", failed.afterState)

        val verified = productionChangeImpactRows(
            plan.copy(
                actions = plan.actions.map {
                    if (it.id == "change-a") it.copy(verificationStatus = "verified") else it
                },
            ),
        ).first { it.key == keyA }
        assertEquals("处理已记录", verified.transitionState)
        assertEquals("计划已处理", verified.afterState)
    }

    @Test
    fun blockedPlanCanStillCompleteVerificationWithoutUnlockingRetirement() {
        val verify = VNextProductionPlanAction(
            id = "rpn-verify-1",
            title = "验证新手机号恢复路径",
            phase = "verify",
            done = false,
            verificationStatus = "pending",
            verificationEvidenceRefs = emptyList(),
            prerequisiteActionIds = emptyList(),
            resolvesImpactKeys = emptyList(),
        )
        val plan = VNextProductionPlan(
            id = "plan-blocked-verify",
            scenario = "replace_phone_number",
            title = "更换手机号",
            workflowState = "in_progress",
            effectiveState = null,
            baselineGraphRevision = 12,
            lastAnalyzedGraphRevision = 12,
            currentGraphRevision = 12,
            targetNodeId = "phone-old",
            targetNodeName = "旧手机号",
            effectiveDate = null,
            readiness = "blocked",
            affectedServiceCount = 2,
            mustChangeKeys = listOf("account-b|authentication"),
            unresolvedMustChangeKeys = listOf("account-b|authentication"),
            actions = listOf(verify),
            retirementScopeReady = false,
        )

        assertTrue(productionPlanCompletionEnabled(plan, verify, true))
        assertTrue(
            productionPlanActionGuidance(verify, plan, true)
                .contains("可以继续验证已经建立的路径"),
        )
    }

    @Test
    fun oldPhoneRetirementRequiresVerifiedNewPathNotMerelyDoneVerificationStep() {
        fun action(
            id: String,
            phase: String,
            done: Boolean,
            verification: String? = null,
            prereqs: List<String> = emptyList(),
        ) = VNextProductionPlanAction(
            id = id,
            title = id,
            phase = phase,
            done = done,
            verificationStatus = verification,
            verificationEvidenceRefs = emptyList(),
            prerequisiteActionIds = prereqs,
            resolvesImpactKeys = emptyList(),
        )

        val verifyPending = action(
            id = "rpn-verify-1",
            phase = "verify",
            done = true,
            verification = "pending",
            prereqs = listOf("rpn-change-1"),
        )
        val retire = action(
            id = "rpn-change-2",
            phase = "change",
            done = false,
            prereqs = listOf("rpn-verify-1"),
        )
        val base = VNextProductionPlan(
            id = "phone-plan",
            scenario = "replace_phone_number",
            title = "更换手机号",
            workflowState = "in_progress",
            effectiveState = null,
            baselineGraphRevision = 9,
            lastAnalyzedGraphRevision = 9,
            currentGraphRevision = 9,
            targetNodeId = "phone-old",
            targetNodeName = "旧手机号",
            effectiveDate = null,
            readiness = "ready_with_known_scope",
            affectedServiceCount = 1,
            mustChangeKeys = emptyList(),
            unresolvedMustChangeKeys = emptyList(),
            actions = listOf(
                action("rpn-prepare-1", "prepare", true),
                action("rpn-change-1", "change", true, "pending", listOf("rpn-prepare-1")),
                verifyPending,
                retire,
            ),
        )

        assertFalse(productionPhoneRetirementVerificationSatisfied(base, retire))
        assertFalse(productionPlanCompletionEnabled(base, retire, true))
        assertTrue(
            productionPlanActionGuidance(retire, base, true)
                .contains("全部验证完成前不能停用旧手机号"),
        )

        val recoveryOnlyVerified = base.copy(
            actions = base.actions.map {
                if (it.id == "rpn-verify-1") it.copy(verificationStatus = "verified") else it
            },
            retirementScopeReady = false,
        )
        val recoveryOnlyRetire = recoveryOnlyVerified.actions.first { it.id == "rpn-change-2" }
        assertFalse(
            productionPhoneRetirementVerificationSatisfied(recoveryOnlyVerified, recoveryOnlyRetire),
        )
        assertFalse(productionPlanCompletionEnabled(recoveryOnlyVerified, recoveryOnlyRetire, true))

        val verifiedPlan = recoveryOnlyVerified.copy(
            actions = recoveryOnlyVerified.actions.map {
                if (it.id == "rpn-change-1") it.copy(verificationStatus = "verified") else it
            },
            retirementScopeReady = true,
        )
        val verifiedRetire = verifiedPlan.actions.first { it.id == "rpn-change-2" }
        assertTrue(productionPhoneRetirementVerificationSatisfied(verifiedPlan, verifiedRetire))
        assertTrue(productionPlanCompletionEnabled(verifiedPlan, verifiedRetire, true))
    }

    @Test
    fun unboundMustChangeRequiresReplanAndShiftsFocusedActionIndex() {
        val action = VNextProductionPlanAction(
            id = "change",
            title = "迁移服务",
            phase = "change",
            done = false,
            verificationStatus = "pending",
            verificationEvidenceRefs = emptyList(),
            prerequisiteActionIds = emptyList(),
            resolvesImpactKeys = emptyList(),
        )
        val plan = VNextProductionPlan(
            id = "plan-unbound",
            scenario = "replace_phone_number",
            title = "更换手机号",
            workflowState = "in_progress",
            effectiveState = null,
            baselineGraphRevision = 7,
            lastAnalyzedGraphRevision = 7,
            currentGraphRevision = 7,
            targetNodeId = "phone-old",
            targetNodeName = "旧手机号",
            effectiveDate = null,
            readiness = "blocked",
            affectedServiceCount = 1,
            mustChangeKeys = listOf("account-b|authentication"),
            unresolvedMustChangeKeys = listOf("account-b|authentication"),
            actions = listOf(action),
        )

        assertEquals(
            listOf("account-b|authentication"),
            productionUnboundMustChangeKeys(plan),
        )
        // Base 5 + unresolved banner + replan banner.
        assertEquals(7, productionFocusedActionLazyIndex(plan, "change", false))
        assertEquals(8, productionFocusedActionLazyIndex(plan, "change", true))

        val claimed = plan.copy(
            actions = listOf(action.copy(resolvesImpactKeys = listOf("account-b|authentication"))),
        )
        assertTrue(productionUnboundMustChangeKeys(claimed).isEmpty())
        // Replan banner disappears; only unresolved banner remains.
        assertEquals(6, productionFocusedActionLazyIndex(claimed, "change", false))
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
            "A fresh CHANGE action that explicitly resolves the current blocker must remain executable.",
            productionPlanCompletionEnabled(blockedWithWork, change, true),
        )
        val prepare = freshReady.actions.first().copy(done = false)
        assertTrue(
            "PREPARE steps must remain usable while must-change blockers exist.",
            productionPlanCompletionEnabled(
                blockedWithWork.copy(actions = listOf(prepare, change)),
                prepare,
                true,
            ),
        )
        val unrelated = change.copy(
            id = "unrelated",
            resolvesImpactKeys = emptyList(),
            prerequisiteActionIds = emptyList(),
        )
        assertFalse(
            "Blocked plans must not open unrelated CHANGE actions.",
            productionPlanCompletionEnabled(
                blockedWithWork.copy(actions = listOf(unrelated)),
                unrelated,
                true,
            ),
        )
        assertTrue(
            "A completed step can be verified while other must-change work remains.",
            productionPlanVerificationEnabled(
                blockedWithWork,
                completedPending,
                true,
            ),
        )

        assertFalse(productionPlanExecutable(freshReady.copy(readiness = "blocked")))
        assertFalse(productionPlanExecutable(freshReady.copy(effectiveState = "needs_revalidation")))
        // Readiness alone cannot override a changed authoritative graph revision.
        assertFalse(productionPlanExecutable(plan.copy(readiness = "ready", effectiveState = null)))
        assertTrue(productionPlanActionGuidance(change, plan, true).contains("重新分析当前影响范围"))
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
