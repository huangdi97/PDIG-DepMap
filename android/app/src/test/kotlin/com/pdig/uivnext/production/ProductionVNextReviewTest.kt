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
