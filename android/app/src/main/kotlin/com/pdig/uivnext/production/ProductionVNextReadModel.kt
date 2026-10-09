package com.pdig.uivnext.production

import com.pdig.app.data.AppContainer
import com.pdig.app.data.CandidateRow
import com.pdig.app.data.DependencyRow
import com.pdig.app.data.DriftRow
import com.pdig.app.data.NodeRow
import com.pdig.app.data.PlanDetailView
import com.pdig.app.data.PlanRow
import com.pdig.app.data.ProposalRow
import com.pdig.app.data.SourceRow
import com.pdig.core.impact.ImpactResult
import com.pdig.core.timeline.TimelineItem

/**
 * Production-facing VNext seam.
 *
 * This layer is intentionally read-only. It does not parse synthetic fixture
 * lifecycle fields, does not write PresentationProfile into Reality, and does
 * not reproduce impact/readiness logic in UI code.
 */
internal interface VNextReadModelSource {
    fun snapshot(nowIso: String? = null): VNextProductionSnapshot
    fun impact(targetNodeId: String): VNextProductionImpact
    fun plan(planId: String): VNextProductionPlan?
}

internal enum class VNextProjectionTruth {
    CONFIRMED,
    DERIVED,
    PENDING_REVIEW,
}

internal data class VNextProductionObject(
    val id: String,
    val kind: String,
    val name: String,
    val truth: VNextProjectionTruth = VNextProjectionTruth.CONFIRMED,
)

internal data class VNextProductionDependency(
    val id: String,
    val fromId: String,
    val fromName: String,
    val relation: String,
    val toId: String,
    val toName: String,
    val capability: String,
    val criticality: String,
    val truth: VNextProjectionTruth = VNextProjectionTruth.CONFIRMED,
)

internal data class VNextProductionTimelineItem(
    val id: String,
    val kind: String,
    val title: String,
    val subtitle: String,
    val scheduledAt: String?,
    val bucket: String,
    val priority: Int,
    val sourceType: String,
    val sourceId: String,
    val actionTarget: String?,
    val status: String,
    val truth: VNextProjectionTruth = VNextProjectionTruth.DERIVED,
)

internal data class VNextProductionPlanSummary(
    val id: String,
    val title: String,
    val scenario: String,
    val workflowState: String,
    val lastAnalyzedRevision: Int,
    val effectiveDate: String?,
    val truth: VNextProjectionTruth = VNextProjectionTruth.CONFIRMED,
)

internal data class VNextPendingReviewSummary(
    val proposalCount: Int,
    val candidateCount: Int,
    val driftCount: Int,
)

internal data class VNextSourceCoverageSummary(
    val sourceCount: Int,
    val activeSourceCount: Int,
)

internal data class VNextProductionSnapshot(
    val revision: Int,
    val objects: List<VNextProductionObject>,
    val confirmedDependencies: List<VNextProductionDependency>,
    val timeline: List<VNextProductionTimelineItem>,
    val plans: List<VNextProductionPlanSummary>,
    val pendingReview: VNextPendingReviewSummary,
    val sourceCoverage: VNextSourceCoverageSummary,
)

internal data class VNextProductionImpactTarget(
    val nodeId: String,
    val nodeName: String,
    val capability: String,
    val status: String,
    val depth: Int,
    val reasonCode: String,
    val reasonText: String,
)

internal data class VNextProductionImpactChecklistItem(
    val level: String,
    val nodeId: String?,
    val capability: String?,
    val title: String,
    val detail: String,
)

internal data class VNextProductionImpact(
    val targetNodeId: String,
    val targets: List<VNextProductionImpactTarget>,
    val checklist: List<VNextProductionImpactChecklistItem>,
)

internal data class VNextProductionPlanAction(
    val id: String,
    val title: String,
    val phase: String,
    val done: Boolean,
    val verificationStatus: String?,
    val prerequisiteActionIds: List<String>,
    val resolvesImpactKeys: List<String>,
)

internal data class VNextProductionPlan(
    val id: String,
    val scenario: String,
    val title: String,
    val workflowState: String,
    val effectiveState: String?,
    val baselineGraphRevision: Int,
    val lastAnalyzedGraphRevision: Int,
    val currentGraphRevision: Int,
    val targetNodeId: String?,
    val targetNodeName: String,
    val effectiveDate: String?,
    val readiness: String,
    val affectedServiceCount: Int,
    val mustChangeKeys: List<String>,
    val unresolvedMustChangeKeys: List<String>,
    val actions: List<VNextProductionPlanAction>,
)

/**
 * Real production adapter over the existing lock-gated AppContainer.
 *
 * The adapter deliberately projects only facts that AppContainer already owns.
 * Object-specific R19 lifecycle fields remain absent until their Canonical
 * proposal is accepted and implemented.
 */
internal class AppContainerVNextReadModelSource(
    private val app: AppContainer,
) : VNextReadModelSource {
    override fun snapshot(nowIso: String?): VNextProductionSnapshot =
        buildProductionSnapshot(
            revision = app.graphRevision(),
            nodes = app.nodes(),
            dependencies = app.dependencies(),
            timeline = if (nowIso == null) app.timeline() else app.timeline(nowIso),
            plans = app.plans(),
            proposals = app.pendingProposals(),
            candidates = app.pendingCandidates(),
            drifts = app.openDrifts(),
            sources = app.sourceInstances(),
        )

    override fun impact(targetNodeId: String): VNextProductionImpact =
        mapProductionImpact(targetNodeId, app.impactFor(targetNodeId))

    override fun plan(planId: String): VNextProductionPlan? =
        app.planDetail(planId)?.let(::mapProductionPlan)
}

internal fun buildProductionSnapshot(
    revision: Int,
    nodes: List<NodeRow>,
    dependencies: List<DependencyRow>,
    timeline: List<TimelineItem>,
    plans: List<PlanRow>,
    proposals: List<ProposalRow>,
    candidates: List<CandidateRow>,
    drifts: List<DriftRow>,
    sources: List<SourceRow>,
): VNextProductionSnapshot {
    val objects = nodes
        .filterNot { it.archived }
        .map {
            VNextProductionObject(
                id = it.id,
                kind = it.kind,
                name = it.name,
            )
        }

    // AppContainer dependencies are confirmed Reality rows. Retired edges are
    // not part of the active VNext continuity projection.
    val confirmedDependencies = dependencies
        .filter { it.state == "active" }
        .map {
            VNextProductionDependency(
                id = it.id,
                fromId = it.from,
                fromName = it.fromName,
                relation = it.relation,
                toId = it.to,
                toName = it.toName,
                capability = it.capability,
                criticality = it.criticality,
            )
        }

    val timelineProjection = timeline.map {
        VNextProductionTimelineItem(
            id = it.id,
            kind = it.kind,
            title = it.title,
            subtitle = it.subtitle,
            scheduledAt = it.scheduledAt,
            bucket = it.bucket,
            priority = it.priority,
            sourceType = it.sourceType,
            sourceId = it.sourceId,
            actionTarget = it.actionTarget,
            status = it.status,
        )
    }

    val planProjection = plans.map {
        VNextProductionPlanSummary(
            id = it.id,
            title = it.title,
            scenario = it.scenario,
            workflowState = it.workflowState,
            lastAnalyzedRevision = it.lastAnalyzedRevision,
            effectiveDate = it.effectiveDate,
        )
    }

    return VNextProductionSnapshot(
        revision = revision,
        objects = objects,
        confirmedDependencies = confirmedDependencies,
        timeline = timelineProjection,
        plans = planProjection,
        pendingReview = VNextPendingReviewSummary(
            proposalCount = proposals.size,
            candidateCount = candidates.size,
            driftCount = drifts.size,
        ),
        sourceCoverage = VNextSourceCoverageSummary(
            sourceCount = sources.size,
            activeSourceCount = sources.count { it.state == "active" },
        ),
    )
}

internal fun mapProductionImpact(
    targetNodeId: String,
    impact: ImpactResult,
): VNextProductionImpact = VNextProductionImpact(
    targetNodeId = targetNodeId,
    targets = impact.targets.map {
        VNextProductionImpactTarget(
            nodeId = it.nodeId,
            nodeName = it.nodeName,
            capability = it.capability.wire,
            status = it.status.wire,
            depth = it.depth,
            reasonCode = it.reasonCode.wire,
            reasonText = it.reasonText,
        )
    },
    checklist = impact.checklist.map {
        VNextProductionImpactChecklistItem(
            level = it.level.wire,
            nodeId = it.nodeId,
            capability = it.capability?.wire,
            title = it.title,
            detail = it.detail,
        )
    },
)

internal fun mapProductionPlan(detail: PlanDetailView): VNextProductionPlan =
    VNextProductionPlan(
        id = detail.id,
        scenario = detail.scenario,
        title = detail.title,
        workflowState = detail.workflowState.wire,
        effectiveState = detail.effectiveState?.wire,
        baselineGraphRevision = detail.baselineGraphRevision,
        lastAnalyzedGraphRevision = detail.lastAnalyzedGraphRevision,
        currentGraphRevision = detail.currentGraphRevision,
        targetNodeId = detail.targetNodeId,
        targetNodeName = detail.targetNodeName,
        effectiveDate = detail.effectiveDate,
        readiness = detail.readiness.wire,
        affectedServiceCount = detail.affectedServiceCount,
        mustChangeKeys = detail.mustChangeKeys,
        unresolvedMustChangeKeys = detail.unresolvedMustChangeKeys,
        actions = detail.actions.map { action ->
            VNextProductionPlanAction(
                id = action.id,
                title = action.title,
                phase = action.phase.wire,
                done = action.done,
                verificationStatus = action.verification?.status?.wire,
                prerequisiteActionIds = action.prerequisiteActionIds,
                resolvesImpactKeys = action.resolvesImpactKeys,
            )
        },
    )
