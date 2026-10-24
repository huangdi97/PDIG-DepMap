package com.pdig.uivnext.production

import com.pdig.app.data.AppContainer
import com.pdig.app.data.CandidateRow
import com.pdig.app.data.DriftRow
import com.pdig.app.data.NodeRow
import com.pdig.app.data.ProposalRow

/**
 * Human-confirmation seam for VNext.
 *
 * Review is the authority boundary:
 * Observation / proposal / candidate / drift NEVER becomes Personal Reality
 * merely because it is visible in the UI.
 */
internal data class VNextProposalReviewItem(
    val id: String,
    val fromId: String,
    val fromName: String,
    val toId: String,
    val toName: String,
    val relation: String,
    val capability: String,
    val confidence: Double,
    val observationCount: Int,
)

internal data class VNextCandidateReviewItem(
    val id: String,
    val candidateKind: String,
    val label: String,
    val observationCount: Int,
)

internal data class VNextDriftReviewItem(
    val id: String,
    val kind: String,
    val targetNodeId: String,
    val targetNodeName: String,
    val capability: String,
    val candidateRelation: String,
    val observationCount: Int,
    val detectedAt: String,
)

internal data class VNextProductionReviewQueue(
    val proposals: List<VNextProposalReviewItem>,
    val candidates: List<VNextCandidateReviewItem>,
    val drifts: List<VNextDriftReviewItem>,
) {
    val total: Int get() = proposals.size + candidates.size + drifts.size
}

internal fun buildProductionReviewQueue(
    nodes: List<NodeRow>,
    proposals: List<ProposalRow>,
    candidates: List<CandidateRow>,
    drifts: List<DriftRow>,
): VNextProductionReviewQueue {
    val names = nodes.associate { it.id to it.name }
    fun name(id: String): String = names[id] ?: "未命名对象"

    return VNextProductionReviewQueue(
        proposals = proposals.map {
            VNextProposalReviewItem(
                id = it.id,
                fromId = it.from,
                fromName = name(it.from),
                toId = it.to,
                toName = name(it.to),
                relation = it.relation,
                capability = it.capability,
                confidence = it.confidence,
                observationCount = it.observationCount,
            )
        },
        candidates = candidates.map {
            VNextCandidateReviewItem(
                id = it.id,
                candidateKind = it.candidateKind,
                label = it.label,
                observationCount = it.observationCount,
            )
        },
        drifts = drifts.map {
            VNextDriftReviewItem(
                id = it.id,
                kind = it.kind,
                targetNodeId = it.targetNodeId,
                targetNodeName = name(it.targetNodeId),
                capability = it.capability,
                candidateRelation = it.candidateRelation,
                observationCount = it.observationCount,
                detectedAt = it.detectedAt,
            )
        },
    )
}

internal interface VNextReviewSource {
    fun reviewQueue(): VNextProductionReviewQueue
}

internal class AppContainerVNextReviewSource(
    private val app: AppContainer,
) : VNextReviewSource {
    override fun reviewQueue(): VNextProductionReviewQueue =
        buildProductionReviewQueue(
            nodes = app.nodes(includeArchived = true),
            proposals = app.pendingProposals(),
            candidates = app.pendingCandidates(),
            drifts = app.openDrifts(),
        )
}

/**
 * Every mutation delegates to the existing production authority and then re-reads
 * the queue. This prevents a local UI dismissal from masquerading as a Reality
 * transition.
 */
internal interface VNextReviewActionGateway {
    fun acceptProposal(id: String): VNextProductionReviewQueue
    fun rejectProposal(id: String): VNextProductionReviewQueue
    fun acceptCandidate(id: String): VNextProductionReviewQueue
    fun dismissCandidate(id: String): VNextProductionReviewQueue
    fun resolveDriftAsReplacement(id: String): VNextProductionReviewQueue
    fun resolveDriftAsAdditionalPath(id: String): VNextProductionReviewQueue
    fun dismissDrift(id: String): VNextProductionReviewQueue
}

internal class AppContainerVNextReviewActionGateway(
    private val app: AppContainer,
) : VNextReviewActionGateway {
    private val source = AppContainerVNextReviewSource(app)

    override fun acceptProposal(id: String): VNextProductionReviewQueue {
        app.acceptProposal(id)
        return source.reviewQueue()
    }

    override fun rejectProposal(id: String): VNextProductionReviewQueue {
        app.rejectProposal(id)
        return source.reviewQueue()
    }

    override fun acceptCandidate(id: String): VNextProductionReviewQueue {
        app.acceptCandidate(id)
        return source.reviewQueue()
    }

    override fun dismissCandidate(id: String): VNextProductionReviewQueue {
        app.dismissCandidate(id)
        return source.reviewQueue()
    }

    override fun resolveDriftAsReplacement(id: String): VNextProductionReviewQueue {
        app.resolveDriftAsReplacement(id)
        return source.reviewQueue()
    }

    override fun resolveDriftAsAdditionalPath(id: String): VNextProductionReviewQueue {
        app.resolveDriftAsAdditionalPath(id)
        return source.reviewQueue()
    }

    override fun dismissDrift(id: String): VNextProductionReviewQueue {
        app.dismissDrift(id)
        return source.reviewQueue()
    }
}
