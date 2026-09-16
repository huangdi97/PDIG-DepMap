package com.pdig.core.statemachine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * GraphRevision bump / never-bump 语义测试。
 *
 * 这张表是"什么算 Reality 变更"的唯一定义：
 * 观测、证据、Proposal、候选、Drift 检测、时间线、计划与动作都**不是** Reality 变更，
 * 因此都不得 bump。反过来依赖确认 / 停用 / 重新启用 / 组合确认都必须 bump。
 */
class GraphRevisionSemanticsTest {

    private val expectedBumps = listOf(
        "dependency_confirm_create",
        "dependency_confirm_update",
        "dependency_retire",
        "dependency_reactivate",
        "group_confirm",
        "group_retire",
        "node_create",
    )

    private val expectedNeverBumps = listOf(
        "evidence_record",
        "proposal_upsert",
        "proposal_decision",
        "candidate_upsert",
        "candidate_dismiss",
        "candidate_accept",
        "drift_detect",
        "drift_dismiss",
        "timeline_build",
        "import_session_record",
        "plan_create",
        "plan_rebase",
        "plan_transition",
        "action_complete",
        "action_verify",
    )

    @Test
    fun realityMutationsBumpTheRevision() {
        for (op in expectedBumps) {
            assertTrue(op in GraphRevisionMachine.bumpsOn, "$op 必须 bump graphRevision")
        }
    }

    @Test
    fun nonRealityOperationsNeverBumpTheRevision() {
        for (op in expectedNeverBumps) {
            assertTrue(op in GraphRevisionMachine.neverBumpsOn, "$op 绝不能 bump graphRevision")
        }
    }

    @Test
    fun confirmedCriticalityChangeBumpsRevision() {
        // "用户确认 criticality" 属于 dependency_confirm_update，不是观测
        assertTrue("dependency_confirm_update" in GraphRevisionMachine.bumpsOn)
    }

    @Test
    fun bumpAndNeverBumpSetsAreDisjoint() {
        val overlap = GraphRevisionMachine.bumpsOn.intersect(GraphRevisionMachine.neverBumpsOn.toSet())
        assertEquals(emptySet(), overlap, "同一操作不得同时属于 bump 与 never-bump")
    }

    @Test
    fun revisionIsMonotonicAndStartsAtZero() {
        assertEquals(0, GraphRevisionMachine.initial)
        assertTrue(GraphRevisionMachine.monotonic)
    }

    @Test
    fun atomicityIsExplicitlyRequired() {
        assertTrue(
            GraphRevisionMachine.atomicity.contains("one database transaction"),
            "Reality 变更与 revision 自增必须在同一事务内",
        )
    }

    @Test
    fun acceptingAProposalIsNotARealityMutationOnItsOwn() {
        // Proposal 决策本身不 bump；真正的 Reality 变更（依赖创建）才 bump
        assertTrue("proposal_decision" in GraphRevisionMachine.neverBumpsOn)
        assertTrue("dependency_confirm_create" in GraphRevisionMachine.bumpsOn)
    }
}
