package com.pdig.uivnext.ui

import com.pdig.uivnext.production.VNextProductionPlan
import com.pdig.uivnext.production.VNextProductionPlanAction
import com.pdig.uivnext.production.VNextProductionPlanImpactItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The primary call to action is a projection of authoritative eligibility, never
 * a second opinion about it. These tests pin the four possible answers the user
 * can be given: complete, verify, reanalyze, and "nothing is executable now".
 */
class ProductionNextStepTest {
    private val key = "account-a|recovery"

    private fun action(
        id: String,
        title: String,
        phase: String = "change",
        done: Boolean = false,
        verificationStatus: String? = null,
        prerequisites: List<String> = emptyList(),
        resolves: List<String> = emptyList(),
    ) = VNextProductionPlanAction(
        id = id,
        title = title,
        phase = phase,
        done = done,
        verificationStatus = verificationStatus,
        verificationEvidenceRefs = emptyList(),
        prerequisiteActionIds = prerequisites,
        resolvesImpactKeys = resolves,
    )

    private fun plan(
        actions: List<VNextProductionPlanAction>,
        stale: Boolean = false,
        unresolved: List<String> = listOf(key),
        readiness: String = "blocked",
        retirementScopeReady: Boolean? = null,
        scenario: String = "replace_phone_number",
    ) = VNextProductionPlan(
        id = "plan-next-step",
        scenario = scenario,
        title = "更换手机号",
        workflowState = "in_progress",
        effectiveState = if (stale) "needs_revalidation" else null,
        baselineGraphRevision = 4,
        lastAnalyzedGraphRevision = 5,
        currentGraphRevision = if (stale) 6 else 5,
        targetNodeId = "phone-old",
        targetNodeName = "旧手机号",
        effectiveDate = null,
        readiness = readiness,
        affectedServiceCount = 1,
        mustChangeKeys = listOf(key),
        unresolvedMustChangeKeys = unresolved,
        actions = actions,
        targetKeys = listOf(key),
        impactItems = listOf(
            VNextProductionPlanImpactItem(
                key = key,
                nodeId = "account-a",
                nodeName = "主账户",
                capability = "recovery",
                status = "must_change",
            ),
        ),
        retirementScopeReady = retirementScopeReady,
    )

    @Test
    fun firstExecutableStepBecomesTheSinglePrimaryCallToAction() {
        val step = productionNextStep(
            plan(
                listOf(
                    action("rpn-prepare-1", "检查旧手机号承担的恢复与认证能力", phase = "prepare"),
                    action("rpn-change-1", "添加新手机号并迁移关键账户", prerequisites = listOf("rpn-prepare-1")),
                ),
            ),
            canMutate = true,
        )

        assertEquals(ProductionNextStepMode.COMPLETE, step.mode)
        assertEquals("rpn-prepare-1", step.actionId)
        assertEquals("没有前置步骤。", step.prerequisites)
        assertTrue(step.reason.isNotBlank())
    }

    @Test
    fun recordedButUnverifiedStepBecomesAVerificationCallToAction() {
        val step = productionNextStep(
            plan(
                listOf(
                    action("rpn-prepare-1", "检查旧手机号承担的恢复与认证能力", phase = "prepare", done = true),
                    action(
                        "rpn-change-1",
                        "添加新手机号并迁移关键账户",
                        done = true,
                        verificationStatus = "pending",
                        prerequisites = listOf("rpn-prepare-1"),
                    ),
                ),
            ),
            canMutate = true,
        )

        assertEquals(ProductionNextStepMode.VERIFY, step.mode)
        assertEquals("rpn-change-1", step.actionId)
        assertEquals("前置步骤已完成：检查旧手机号承担的恢复与认证能力。", step.prerequisites)
        assertTrue(step.verificationNote.contains("确认验证会写入正式记录"))
    }

    @Test
    fun completingAStepThatNeedsVerificationSaysVerificationIsStillRequired() {
        val step = productionNextStep(
            plan(
                listOf(
                    action(
                        "rpn-change-1",
                        "添加新手机号并迁移关键账户",
                        verificationStatus = "pending",
                        resolves = listOf(key),
                    ),
                ),
            ),
            canMutate = true,
        )

        assertEquals(ProductionNextStepMode.COMPLETE, step.mode)
        assertTrue(step.verificationNote.contains("完成 ≠ 已验证"))
    }

    @Test
    fun stalePlanMakesAuthoritativeReanalysisThePrimaryCallToAction() {
        val step = productionNextStep(
            plan(
                listOf(action("rpn-prepare-1", "检查旧手机号承担的恢复与认证能力", phase = "prepare")),
                stale = true,
            ),
            canMutate = true,
        )

        assertEquals(ProductionNextStepMode.REANALYZE, step.mode)
        assertEquals("重新分析当前影响", step.title)
        assertEquals(null, step.actionId)
        assertTrue(step.reason.contains("基础设施已变化"))
        assertTrue(step.verificationNote.contains("不会把任何步骤标记为已完成或已验证"))
    }

    @Test
    fun maskedScopeNeverAdvertisesAnExecutionCallToAction() {
        val step = productionNextStep(
            plan(listOf(action("rpn-prepare-1", "检查旧手机号承担的恢复与认证能力", phase = "prepare"))),
            canMutate = false,
            privacyMask = true,
        )

        assertEquals(ProductionNextStepMode.NONE, step.mode)
        assertEquals(null, step.actionId)
        assertTrue(step.title.contains("遮蔽"))
    }

    @Test
    fun viewOnlyAuthorityNeverAdvertisesAnExecutionCallToAction() {
        val step = productionNextStep(
            plan(listOf(action("rpn-prepare-1", "检查旧手机号承担的恢复与认证能力", phase = "prepare"))),
            canMutate = false,
        )

        assertEquals(ProductionNextStepMode.NONE, step.mode)
        assertTrue(step.title.contains("查看权限"))
    }

    @Test
    fun unresolvedWorkWithoutAnExecutableStepIsNotPresentedAsCompletion() {
        val step = productionNextStep(
            plan(
                listOf(
                    action(
                        "rpn-change-2",
                        "停用旧手机号（新路径全部验证后）",
                        prerequisites = listOf("rpn-verify-1"),
                    ),
                ),
                unresolved = listOf(key),
                retirementScopeReady = false,
            ),
            canMutate = true,
        )

        assertEquals(ProductionNextStepMode.NONE, step.mode)
        assertTrue(step.reason.contains("必须处理影响"))
        assertTrue(step.prerequisites.contains("仍未完成"))
        assertFalse(step.title.contains("完成"))
    }

    @Test
    fun retirementStaysUnavailableUntilEveryNewPathIsVerified() {
        val unverified = productionNextStep(
            plan(
                listOf(
                    action("rpn-change-1", "添加新手机号并迁移关键账户", done = true, verificationStatus = "verified"),
                    action("rpn-verify-1", "验证新手机号恢复路径", phase = "verify", done = true, verificationStatus = "pending"),
                    action("rpn-change-2", "停用旧手机号（新路径全部验证后）", prerequisites = listOf("rpn-verify-1")),
                ),
                unresolved = emptyList(),
                readiness = "review_required",
                retirementScopeReady = false,
            ),
            canMutate = true,
        )
        // The next executable step is the pending verification, never retirement.
        assertEquals(ProductionNextStepMode.VERIFY, unverified.mode)
        assertEquals("rpn-verify-1", unverified.actionId)

        val verified = productionNextStep(
            plan(
                listOf(
                    action("rpn-change-1", "添加新手机号并迁移关键账户", done = true, verificationStatus = "verified"),
                    action("rpn-verify-1", "验证新手机号恢复路径", phase = "verify", done = true, verificationStatus = "verified"),
                    action("rpn-change-2", "停用旧手机号（新路径全部验证后）", prerequisites = listOf("rpn-verify-1")),
                ),
                unresolved = emptyList(),
                readiness = "ready_with_known_scope",
                retirementScopeReady = true,
            ),
            canMutate = true,
        )
        assertEquals(ProductionNextStepMode.COMPLETE, verified.mode)
        assertEquals("rpn-change-2", verified.actionId)
    }
}
