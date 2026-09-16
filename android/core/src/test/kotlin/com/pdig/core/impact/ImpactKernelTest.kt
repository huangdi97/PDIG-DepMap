package com.pdig.core.impact

import com.pdig.core.domain.Dependency
import com.pdig.core.domain.DependencyGroup
import com.pdig.core.domain.ImpactGraph
import com.pdig.core.domain.ImpactProposalInput
import com.pdig.core.domain.ImpactStateKey
import com.pdig.core.generated.Capability
import com.pdig.core.generated.Criticality
import com.pdig.core.generated.GroupMode
import com.pdig.core.generated.ImpactReasonCode
import com.pdig.core.generated.ImpactTargetStatus
import com.pdig.core.generated.Relation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Impact Kernel 单元测试（移植自 spec/impact 的语义）。
 *
 * 这些不是"看一眼源码就写"的测试：每条都对应一个真实业务后果，
 * 尤其是 **Proposal 任何置信度都不得被提升为 must_change** 这条机器推断红线。
 */
class ImpactKernelTest {

    private val payment = Capability.PAYMENT

    private fun edge(
        id: String,
        from: String,
        to: String,
        criticality: Criticality = Criticality.REQUIRED,
        capability: Capability = payment,
    ) = Dependency(
        id = id,
        from = from,
        relation = Relation.MERCHANT_AGREEMENT,
        to = to,
        capability = capability,
        criticality = criticality,
    )

    private fun graph(
        dependencies: List<Dependency>,
        groups: List<DependencyGroup> = emptyList(),
        proposals: List<ImpactProposalInput> = emptyList(),
        names: Map<String, String> = emptyMap(),
    ) = ImpactGraph(dependencies, groups, proposals, names)

    private fun targetOf(result: ImpactResult, nodeId: String) =
        result.targets.first { it.nodeId == nodeId }

    // ------------------------------------------------------------------
    // 已确认的必需路径
    // ------------------------------------------------------------------

    @Test
    fun requiredEdgeWithoutAlternativeIsMustChange() {
        val g = graph(listOf(edge("e1", "pay-card", "svc-video", Criticality.REQUIRED)))
        val result = simulateDisable(g, "pay-card")

        val t = targetOf(result, "svc-video")
        assertEquals(ImpactTargetStatus.MUST_CHANGE, t.status)
        assertEquals(ImpactReasonCode.REQUIRED_EDGE_NO_ALTERNATIVE, t.reasonCode)
        kotlin.test.assertFalse(t.available, "must_change 意味着支付能力失效")
    }

    // ------------------------------------------------------------------
    // 未确认（unknown）路径 —— 不得被机器当成"确定能继续"
    // ------------------------------------------------------------------

    @Test
    fun unknownCriticalityIsNeedsReviewNotUnaffected() {
        val g = graph(listOf(edge("e1", "pay-card", "svc-video", Criticality.UNKNOWN)))
        val result = simulateDisable(g, "pay-card")

        val t = targetOf(result, "svc-video")
        assertEquals(ImpactTargetStatus.NEEDS_REVIEW, t.status, "unknown 只能 needs_review")
        assertEquals(ImpactReasonCode.CRITICALITY_UNKNOWN, t.reasonCode)
        assertTrue(t.available, "needs_review 不宣称已失效")
    }

    @Test
    fun unconfirmedAlternativeExistsIsNeedsReview() {
        // svc 同时被另一张卡支付，但没有确认的备用组合 ⇒ 不得自动认为有备份
        val g = graph(
            listOf(
                edge("e1", "pay-card", "svc-video"),
                edge("e2", "pay-other", "svc-video"),
            ),
        )
        val result = simulateDisable(g, "pay-card")

        val t = targetOf(result, "svc-video")
        assertEquals(ImpactTargetStatus.NEEDS_REVIEW, t.status)
        assertEquals(ImpactReasonCode.UNCONFIRMED_ALTERNATIVE_EXISTS, t.reasonCode)
    }

    @Test
    fun proposalNeverEscalatesToMustChangeEvenAtMaxConfidence() {
        val g = graph(
            dependencies = emptyList(),
            proposals = listOf(
                ImpactProposalInput(
                    key = "pay-card|merchant_agreement|svc-video|payment",
                    from = "pay-card",
                    to = "svc-video",
                    capability = payment,
                    confidenceScore = 0.999,
                ),
            ),
            names = mapOf("pay-card" to "银行卡", "svc-video" to "视频会员"),
        )
        val result = simulateDisable(g, "pay-card")

        val t = targetOf(result, "svc-video")
        assertEquals(
            ImpactTargetStatus.NEEDS_REVIEW, t.status,
            "任何置信度的 Proposal 都不得被 Machine 推断为必须失效（红线）",
        )
        assertEquals(ImpactReasonCode.PROPOSAL_ONLY, t.reasonCode)
        assertTrue(result.targets.none { it.status == ImpactTargetStatus.MUST_CHANGE })
    }

    // ------------------------------------------------------------------
    // 已确认的 Group
    // ------------------------------------------------------------------

    @Test
    fun confirmedAnyGroupWithSurvivingMemberIsBackupPath() {
        val e1 = edge("e1", "pay-card", "svc-video")
        val e2 = edge("e2", "pay-backup", "svc-video")
        val g = graph(
            dependencies = listOf(e1, e2),
            groups = listOf(
                DependencyGroup(
                    id = "grp-1",
                    groupKey = "svc-video|payment|ANY|...",
                    targetNodeId = "svc-video",
                    capability = payment,
                    mode = GroupMode.ANY,
                    memberEdgeIds = listOf("e1", "e2"),
                ),
            ),
        )
        val result = simulateDisable(g, "pay-card")

        val t = targetOf(result, "svc-video")
        assertEquals(ImpactTargetStatus.BACKUP_PATH, t.status)
        assertEquals(ImpactReasonCode.CONFIRMED_GROUP_COVERED, t.reasonCode)
        assertTrue(t.redundancyDegraded, "备用路径必须同时说明冗余度下降")
        assertTrue(t.available)
    }

    @Test
    fun confirmedAnyGroupWithAllMembersLostIsMustChange() {
        val e1 = edge("e1", "pay-card", "svc-video")
        val g = graph(
            dependencies = listOf(e1),
            groups = listOf(
                DependencyGroup(
                    id = "grp-1",
                    groupKey = "svc-video|payment|ANY|...",
                    targetNodeId = "svc-video",
                    capability = payment,
                    mode = GroupMode.ANY,
                    memberEdgeIds = listOf("e1"),
                ),
            ),
        )
        val result = simulateDisable(g, "pay-card")

        val t = targetOf(result, "svc-video")
        assertEquals(ImpactTargetStatus.MUST_CHANGE, t.status)
        assertEquals(ImpactReasonCode.CONFIRMED_GROUP_FAILED, t.reasonCode)
    }

    // ------------------------------------------------------------------
    // 图结构安全
    // ------------------------------------------------------------------

    @Test
    fun cyclicGraphTerminatesAndEachKeyProcessedOnce() {
        val g = graph(
            listOf(
                edge("e1", "svc-a", "svc-b"),
                edge("e2", "svc-b", "svc-a"),
            ),
        )
        val result = simulateDisable(g, "svc-a")

        assertEquals(
            result.processedKeys.size, result.processedKeys.distinct().size,
            "processedKeys 必须唯一（环相同处理 key 的不变量）",
        )
        assertTrue(result.targets.isNotEmpty(), "环也必须产生结论，而不是静默返回空")
    }

    @Test
    fun deterministicOrderingAcrossRuns() {
        val g = graph(
            listOf(
                edge("e1", "pay-card", "svc-b"),
                edge("e2", "pay-card", "svc-a"),
                edge("e3", "svc-a", "svc-c"),
                edge("e4", "svc-b", "svc-c"),
            ),
        )
        val first = simulateDisable(g, "pay-card")
        val second = simulateDisable(g, "pay-card")

        assertEquals(first.targets.map { it.nodeId }, second.targets.map { it.nodeId })
        assertEquals(first.processedKeys, second.processedKeys)
        assertEquals(first.checklist.map { it.title }, second.checklist.map { it.title })
    }

    @Test
    fun originalOperationIsAlwaysTheLastChecklistItem() {
        val g = graph(listOf(edge("e1", "pay-card", "svc-a")))
        val result = simulateDisable(g, "pay-card")

        val last = result.checklist.last()
        kotlin.test.assertNotNull(last.nodeId)
        assertEquals("pay-card", last.nodeId, "最后一步必须是原始操作（IMP-09）")
        assertTrue(last.detail.contains("以上事项处理完成后"), "原始操作必须排在清单最后")
    }

    @Test
    fun nonPaymentCapabilityKeysAreIgnored() {
        val g = graph(listOf(edge("e1", "pay-card", "svc-a")))
        val result = simulateScenario(g, listOf(ImpactStateKey("pay-card", Capability.ACCESS)))

        assertTrue(
            result.targets.isEmpty(),
            "MVP payment domain 只传播 payment capability；access 初始键必须被忽略",
        )
    }

    @Test
    fun accessCapabilityEdgesDoNotParticipateInPaymentPropagation() {
        val g = graph(
            listOf(
                edge("e1", "pay-card", "svc-a", capability = Capability.ACCESS),
            ),
        )
        val result = simulateDisable(g, "pay-card")
        assertTrue(result.targets.isEmpty(), "非 payment 边不参与 payment 传播")
    }
}
