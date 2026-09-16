package com.pdig.core.plan

import com.pdig.core.domain.ChangePlan
import com.pdig.core.domain.PlanAction
import com.pdig.core.domain.PlanReadinessInput
import com.pdig.core.domain.ScenarioCoverageInput
import com.pdig.core.domain.CoverageSourceInfo
import com.pdig.core.generated.ChangePlanWorkflowState
import com.pdig.core.generated.CoverageLevel
import com.pdig.core.generated.PlanActionPhase
import com.pdig.core.generated.PlanReadiness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * PlanReadiness / 显式 resolution 测试。
 *
 * 重点不是"数对不对"，而是**禁止用减法近似**：
 * readiness 只认"每个 must_change key 是否被已完成的 change 动作显式声明解决"，
 * 绝不接受「目标数 − 完成动作数」这种口径。
 */
class PlanReadinessTest {

    private fun plan(
        actions: List<PlanAction>,
        analyzed: Int = 1,
        state: ChangePlanWorkflowState = ChangePlanWorkflowState.IN_PROGRESS,
    ) = ChangePlan(
        id = "plan-1",
        scenario = "replace_payment_card",
        workflowState = state,
        baselineGraphRevision = 1,
        lastAnalyzedGraphRevision = analyzed,
        actions = actions,
    )

    private fun action(id: String, keys: List<String>, done: Boolean, phase: PlanActionPhase = PlanActionPhase.CHANGE) =
        PlanAction(id = id, title = id, phase = phase, done = done, resolvesImpactKeys = keys)

    private fun input(
        plan: ChangePlan,
        mustChange: Int = 0,
        needsReview: Int = 0,
        current: Int = 1,
    ) = PlanReadinessInput(
        plan = plan,
        currentGraphRevision = current,
        pendingMustChange = mustChange,
        pendingNeedsReview = needsReview,
        unresolvedCandidates = 0,
        pendingRelevantProposals = 0,
        staleRelevantDependencies = 0,
        unfinishedChangeActions = 0,
    )

    @Test
    fun unresolvedMustChangeBlocksThePlan() {
        assertEquals(
            PlanReadiness.BLOCKED,
            computePlanReadiness(input(plan(emptyList()), mustChange = 2)),
        )
    }

    @Test
    fun pendingNeedsReviewYieldsReviewRequired() {
        assertEquals(
            PlanReadiness.REVIEW_REQUIRED,
            computePlanReadiness(input(plan(emptyList()), needsReview = 3)),
        )
    }

    @Test
    fun staleGraphRevisionYieldsReviewRequired() {
        val stale = plan(emptyList(), analyzed = 1)
        assertEquals(
            PlanReadiness.REVIEW_REQUIRED,
            computePlanReadiness(input(stale, current = 2)),
            "图谱版本落后必须要求重新确认",
        )
    }

    @Test
    fun everythingResolvedYieldsReadyWithKnownScope() {
        assertEquals(
            PlanReadiness.READY_WITH_KNOWN_SCOPE,
            computePlanReadiness(input(plan(emptyList()))),
        )
    }

    @Test
    fun blockedWinsOverReviewRequired() {
        val p = plan(emptyList(), analyzed = 1)
        val res = computePlanReadiness(
            PlanReadinessInput(
                plan = p,
                currentGraphRevision = 5, // stale
                pendingMustChange = 1,
                pendingNeedsReview = 1,
                unresolvedCandidates = 1,
                pendingRelevantProposals = 1,
                staleRelevantDependencies = 1,
                unfinishedChangeActions = 1,
            ),
        )
        assertEquals(PlanReadiness.BLOCKED, res, "blocked 必须优先于 review_required")
    }

    // ------------------------------------------------------------------
    // 显式 resolution（resolvesImpactKeys）
    // ------------------------------------------------------------------

    @Test
    fun unclaimedMustChangeKeyIsUnresolved() {
        val actions = listOf(action("a1", keys = listOf("other|payment"), done = true))
        assertEquals(1, countUnresolvedMustChange(actions, listOf("svc-a|payment")))
    }

    @Test
    fun claimedButUnfinishedActionLeavesKeyUnresolved() {
        val actions = listOf(action("a1", keys = listOf("svc-a|payment"), done = false))
        assertEquals(1, countUnresolvedMustChange(actions, listOf("svc-a|payment")))
    }

    @Test
    fun allClaimantsDoneResolvesTheKey() {
        val actions = listOf(
            action("a1", keys = listOf("svc-a|payment"), done = true),
            action("a2", keys = listOf("svc-a|payment"), done = true),
        )
        assertEquals(0, countUnresolvedMustChange(actions, listOf("svc-a|payment")))
    }

    @Test
    fun oneUnfinishedClaimantKeepsKeyUnresolved() {
        val actions = listOf(
            action("a1", keys = listOf("svc-a|payment"), done = true),
            action("a2", keys = listOf("svc-a|payment"), done = false),
        )
        assertEquals(1, countUnresolvedMustChange(actions, listOf("svc-a|payment")))
    }

    @Test
    fun unrelatedCompletedActionCannotResolveRequirement() {
        // 做了很多别的动作，但没有一个声明解决 svc-a ⇒ 仍然 unresolved
        val actions = listOf(
            action("a1", keys = listOf("svc-x|payment"), done = true),
            action("a2", keys = listOf("svc-y|payment"), done = true),
            action("a3", keys = emptyList(), done = true),
        )
        assertEquals(
            1,
            countUnresolvedMustChange(actions, listOf("svc-a|payment")),
            "完成无关动作不得抵扣 must_change（禁止减法近似）",
        )
    }

    @Test
    fun readinessIsNotSubtractionOfActionCounts() {
        // 2 个 must_change；1 个动作解决其中一个 ⇒ 仍应 blocked，而不是"2-1=1 已解决"
        val actions = listOf(action("a1", keys = listOf("svc-a|payment"), done = true))
        val unresolved = countUnresolvedMustChange(actions, listOf("svc-a|payment", "svc-b|payment"))
        assertEquals(1, unresolved)

        assertEquals(
            PlanReadiness.BLOCKED,
            computePlanReadiness(input(plan(actions), mustChange = unresolved)),
        )
    }

    // ------------------------------------------------------------------
    // coverage ≠ readiness
    // ------------------------------------------------------------------

    @Test
    fun coverageLevelIsNotReadiness() {
        val covered = computeScenarioCoverage(
            ScenarioCoverageInput(
                scenarioId = "replace_payment_card",
                sources = listOf(CoverageSourceInfo("src-1", "账单", "2026-09-10T00:00:00Z")),
                confirmedDirectDependencies = 3,
                confirmedIndirectDependencies = 2,
                pendingProposals = 0,
                unresolvedCandidates = 0,
                staleDependencies = 0,
                unknownCriticalityCount = 0,
                unverifiedActions = 0,
                freshnessThresholdDays = 30,
                now = "2026-09-11T00:00:00Z",
            ),
        )
        assertEquals(CoverageLevel.WELL_EVIDENCED, covered.coverageLevel)

        // 同一状态下计划仍可能 blocked（coverage 描述信息充分度，不描述可执行性）
        assertEquals(
            PlanReadiness.BLOCKED,
            computePlanReadiness(input(plan(emptyList()), mustChange = 1)),
        )
    }

    @Test
    fun isPlanStaleIgnoresTerminalStates() {
        val completed = plan(emptyList(), analyzed = 1, state = ChangePlanWorkflowState.COMPLETED)
        kotlin.test.assertFalse(isPlanStale(completed, 9), "completed 是终态，不需要重新验证")

        val draft = plan(emptyList(), analyzed = 1, state = ChangePlanWorkflowState.DRAFT)
        assertTrue(isPlanStale(draft, 9), "非终态且版本落后 ⇒ 需要重新验证")
    }

    @Test
    fun readinessIsDeterministic() {
        val i = input(plan(emptyList()), mustChange = 1, needsReview = 2)
        assertEquals(computePlanReadiness(i), computePlanReadiness(i))
    }
}
