package com.pdig.uivnext.production

import com.pdig.app.data.CandidateRow
import com.pdig.app.data.DriftRow
import com.pdig.app.data.NodeRow
import com.pdig.app.data.ProposalRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionVNextReviewTest {
    @Test
    fun reviewQueueKeepsProposalsCandidatesAndDriftsOutsideReality() {
        val queue = buildProductionReviewQueue(
            nodes = listOf(
                NodeRow("card-1", "payment_instrument", "主卡", false, "{}"),
                NodeRow("svc-1", "service", "视频服务", false, "{}"),
            ),
            proposals = listOf(
                ProposalRow(
                    id = "p1",
                    key = "k1",
                    from = "card-1",
                    to = "svc-1",
                    relation = "funding_source",
                    capability = "payment",
                    confidence = 0.91,
                    observationCount = 3,
                ),
            ),
            candidates = listOf(
                CandidateRow("c1", "service", "候选服务", 2, "pending"),
            ),
            drifts = listOf(
                DriftRow(
                    id = "d1",
                    kind = "replacement",
                    targetNodeId = "card-1",
                    capability = "payment",
                    candidateFrom = null,
                    candidateRelation = "funding_source",
                    relatedDependencyIds = listOf("dep-1"),
                    observationCount = 2,
                    detectedAt = "2026-10-10T00:00:00Z",
                ),
            ),
        )

        assertEquals(3, queue.total)
        assertEquals("主卡", queue.proposals.single().fromName)
        assertEquals("视频服务", queue.proposals.single().toName)
        assertEquals(0.91, queue.proposals.single().confidence, 0.0001)
        assertEquals("候选服务", queue.candidates.single().label)
        assertEquals("主卡", queue.drifts.single().targetNodeName)
        assertTrue(queue.proposals.single().observationCount > 0)
    }

    @Test
    fun consumerProjectionKeepsReviewKindsAndAuthorityBoundaryExplicit() {
        val queue = VNextProductionReviewQueue(
            proposals = listOf(
                VNextProposalReviewItem(
                    id = "p1",
                    fromId = "card-1",
                    fromName = "主卡",
                    toId = "svc-1",
                    toName = "视频服务",
                    relation = "funding_source",
                    capability = "payment",
                    confidence = 0.97,
                    observationCount = 3,
                ),
            ),
            candidates = listOf(
                VNextCandidateReviewItem(
                    id = "c1",
                    candidateKind = "service",
                    label = "候选服务",
                    observationCount = 2,
                ),
            ),
            drifts = listOf(
                VNextDriftReviewItem(
                    id = "d1",
                    kind = "replacement",
                    targetNodeId = "card-1",
                    targetNodeName = "主卡",
                    capability = "payment",
                    candidateRelation = "funding_source",
                    observationCount = 1,
                    detectedAt = "2026-10-10T00:00:00Z",
                ),
            ),
        )

        val inbox = buildProductionReviewConsumerInbox(queue)

        assertEquals(3, inbox.total)
        assertEquals(listOf("确认关系", "拒绝"), inbox.proposals.single().decisions)
        assertTrue(inbox.proposals.single().summary.contains("确认前不会进入依赖图"))
        assertEquals(listOf("确认对象", "忽略"), inbox.candidates.single().decisions)
        assertTrue(inbox.drifts.single().summary.contains("只有明确确认后才修改 Reality"))
        assertEquals(
            listOf("已替换", "两个都在用", "没有变化", "稍后确认"),
            inbox.drifts.single().decisions,
        )
    }

    @Test
    fun coordinatorMapsConsumerDecisionsToAuthoritativeGatewayAndLaterIsNoOp() {
        val initial = VNextProductionReviewQueue(
            proposals = listOf(
                VNextProposalReviewItem(
                    id = "p1",
                    fromId = "a",
                    fromName = "A",
                    toId = "b",
                    toName = "B",
                    relation = "funding_source",
                    capability = "payment",
                    confidence = 0.8,
                    observationCount = 2,
                ),
            ),
            candidates = emptyList(),
            drifts = listOf(
                VNextDriftReviewItem(
                    id = "d1",
                    kind = "replacement",
                    targetNodeId = "a",
                    targetNodeName = "A",
                    capability = "payment",
                    candidateRelation = "funding_source",
                    observationCount = 1,
                    detectedAt = "2026-10-10T00:00:00Z",
                ),
            ),
        )
        var calls = mutableListOf<String>()
        val source = object : VNextReviewSource {
            override fun reviewQueue(): VNextProductionReviewQueue = initial
        }
        val gateway = object : VNextReviewActionGateway {
            private fun done(call: String): VNextProductionReviewQueue {
                calls.add(call)
                return VNextProductionReviewQueue(emptyList(), emptyList(), emptyList())
            }
            override fun acceptProposal(id: String) = done("acceptProposal:$id")
            override fun rejectProposal(id: String) = done("rejectProposal:$id")
            override fun acceptCandidate(id: String) = done("acceptCandidate:$id")
            override fun dismissCandidate(id: String) = done("dismissCandidate:$id")
            override fun resolveDriftAsReplacement(id: String) = done("replacement:$id")
            override fun resolveDriftAsAdditionalPath(id: String) = done("additional:$id")
            override fun dismissDrift(id: String) = done("dismissDrift:$id")
        }
        val coordinator = ProductionReviewCoordinator(source, gateway)

        val afterConfirm = coordinator.decide(
            ProductionReviewConsumerKind.RELATION_PROPOSAL,
            "p1",
            ProductionReviewDecision.CONFIRM_RELATION,
        )
        assertEquals(listOf("acceptProposal:p1"), calls)
        assertEquals(0, afterConfirm.total)

        calls.clear()
        val later = coordinator.decide(
            ProductionReviewConsumerKind.REALITY_DRIFT,
            "d1",
            ProductionReviewDecision.LATER,
        )
        assertTrue(calls.isEmpty())
        assertEquals(2, later.total)
    }

    @Test(expected = IllegalStateException::class)
    fun coordinatorRejectsDecisionThatDoesNotMatchReviewKind() {
        val empty = VNextProductionReviewQueue(emptyList(), emptyList(), emptyList())
        val source = object : VNextReviewSource {
            override fun reviewQueue(): VNextProductionReviewQueue = empty
        }
        val gateway = object : VNextReviewActionGateway {
            override fun acceptProposal(id: String) = empty
            override fun rejectProposal(id: String) = empty
            override fun acceptCandidate(id: String) = empty
            override fun dismissCandidate(id: String) = empty
            override fun resolveDriftAsReplacement(id: String) = empty
            override fun resolveDriftAsAdditionalPath(id: String) = empty
            override fun dismissDrift(id: String) = empty
        }
        ProductionReviewCoordinator(source, gateway).decide(
            ProductionReviewConsumerKind.OBJECT_CANDIDATE,
            "c1",
            ProductionReviewDecision.CONFIRM_RELATION,
        )
    }

    @Test
    fun unknownNodeNamesStayHonestInsteadOfLeakingInternalIds() {
        val queue = buildProductionReviewQueue(
            nodes = emptyList(),
            proposals = listOf(
                ProposalRow(
                    id = "p1",
                    key = "k1",
                    from = "internal-from",
                    to = "internal-to",
                    relation = "linked",
                    capability = "unknown",
                    confidence = 0.5,
                    observationCount = 1,
                ),
            ),
            candidates = emptyList(),
            drifts = emptyList(),
        )

        assertEquals("未命名对象", queue.proposals.single().fromName)
        assertEquals("未命名对象", queue.proposals.single().toName)
    }
}
