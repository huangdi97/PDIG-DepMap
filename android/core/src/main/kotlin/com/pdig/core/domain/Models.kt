package com.pdig.core.domain

import com.pdig.core.generated.Capability
import com.pdig.core.generated.Criticality
import com.pdig.core.generated.DependencyOrigin
import com.pdig.core.generated.DependencyState
import com.pdig.core.generated.GroupMode
import com.pdig.core.generated.GroupState
import com.pdig.core.generated.NodeKind
import com.pdig.core.generated.PlanActionPhase
import com.pdig.core.generated.Relation

/**
 * PDIG 领域模型 — 纯 Kotlin，与 spec/domain/domain.json 一一对应。
 *
 * 铁律（spec/domain/entities.md）：
 *  - Dependency 存在即代表用户确认
 *  - criticality 只有 required / unknown，机器永不产生 required
 *  - retired 表示"不再成立"，不是删除；重新成立时 re-activate 原行
 */

data class DepNode(
    val id: String,
    val kind: NodeKind,
    val name: String,
    val issuer: String? = null,
    val last4: String? = null,
    val archived: Boolean = false,
)

/** `from` 为 `to` 提供支付 → from 失效时 to 受影响。 */
data class Dependency(
    val id: String,
    val from: String,
    val relation: Relation,
    val to: String,
    val capability: Capability,
    val criticality: Criticality,
    val groupId: String? = null,
    val state: DependencyState = DependencyState.ACTIVE,
    val origin: DependencyOrigin = DependencyOrigin.MANUAL,
    val lastVerifiedAt: String = "",
)

data class DependencyGroup(
    val id: String,
    val groupKey: String,
    val targetNodeId: String,
    val capability: Capability,
    val mode: GroupMode,
    val memberEdgeIds: List<String>,
    val state: GroupState = GroupState.ACTIVE,
)

data class ImpactProposalInput(
    val key: String,
    val from: String,
    val to: String,
    val capability: Capability,
    val confidenceScore: Double? = null,
)

data class ImpactGraph(
    val dependencies: List<Dependency> = emptyList(),
    val groups: List<DependencyGroup> = emptyList(),
    val proposals: List<ImpactProposalInput> = emptyList(),
    val nodeNames: Map<String, String> = emptyMap(),
)

data class ImpactStateKey(val nodeId: String, val capability: Capability) {
    companion object {
        fun parse(s: String): ImpactStateKey {
            val i = s.indexOf('|')
            require(i > 0) { "impact key must be 'nodeId|capability', got $s" }
            return ImpactStateKey(
                nodeId = s.substring(0, i),
                capability = Capability.fromWire(s.substring(i + 1))
                    ?: error("unknown capability in key: $s"),
            )
        }
    }
    fun keyString(): String = "$nodeId|${capability.wire}"
}

/** logical key: `from|relation|to|capability` — 跨端必须完全一致（spec §9）。 */
fun dependencyLogicalKey(
    from: String,
    relation: Relation,
    to: String,
    capability: Capability,
): String = "$from|${relation.wire}|$to|${capability.wire}"

/**
 * canonical groupKey: `target|capability|mode|sorted(dedup(members...))`
 * 成员顺序无关（spec §4）。
 */
fun canonicalGroupKey(
    targetNodeId: String,
    capability: Capability,
    mode: GroupMode,
    memberLogicalKeys: List<String>,
): String {
    val sorted = memberLogicalKeys.distinct().sorted()
    return buildList {
        add(targetNodeId)
        add(capability.wire)
        add(mode.wire)
        addAll(sorted)
    }.joinToString("|")
}

// ---------------------------------------------------------------------------
// ChangePlan / Readiness / Coverage（与 spec/domain/change-plan.ts 同口径）
// ---------------------------------------------------------------------------

data class ActionVerification(
    val method: ActionVerificationMethod,
    val status: ActionVerificationStatus,
    val evidenceRefs: List<String> = emptyList(),
    val expectedFromNodeId: String? = null,
    val expectedToNodeId: String? = null,
)

enum class ActionVerificationMethod(val wire: String) {
    MANUAL_CONFIRMATION("manual_confirmation"),
    FUTURE_OBSERVATION("future_observation"),
    AUTHORITATIVE_SOURCE("authoritative_source"),
    ;
    companion object {
        fun fromWire(v: String) = entries.firstOrNull { it.wire == v }
    }
}

enum class ActionVerificationStatus(val wire: String) {
    NOT_REQUIRED("not_required"),
    PENDING("pending"),
    EVIDENCE_SUGGESTED("evidence_suggested"),
    VERIFIED("verified"),
    FAILED("failed"),
    ;
    companion object {
        fun fromWire(v: String) = entries.firstOrNull { it.wire == v }
    }
}

data class PlanAction(
    val id: String,
    val title: String = "",
    val phase: PlanActionPhase,
    val done: Boolean = false,
    val resolvesImpactKeys: List<String> = emptyList(),
    val verification: ActionVerification? = null,
    /** v0.3.0 (Canonical vNext)：前置动作 id 列表（必须先完成）。 */
    val prerequisiteActionIds: List<String> = emptyList(),
)

data class ChangePlan(
    val id: String,
    val templateId: String? = null,
    val scenario: String = "",
    val title: String = "",
    val workflowState: com.pdig.core.generated.ChangePlanWorkflowState =
        com.pdig.core.generated.ChangePlanWorkflowState.DRAFT,
    val baselineGraphRevision: Int = 0,
    val lastAnalyzedGraphRevision: Int = 0,
    val targetNodeId: String? = null,
    val effectiveDate: String? = null,
    val actions: List<PlanAction> = emptyList(),
)

data class PlanReadinessInput(
    val plan: ChangePlan,
    val currentGraphRevision: Int,
    val pendingMustChange: Int,
    val pendingNeedsReview: Int,
    val unresolvedCandidates: Int,
    val pendingRelevantProposals: Int,
    val staleRelevantDependencies: Int,
    val unfinishedChangeActions: Int,
)

data class CoverageSourceInfo(
    val id: String,
    val label: String,
    val lastIngestedAt: String?,
)

data class ScenarioCoverageInput(
    val scenarioId: String,
    val sources: List<CoverageSourceInfo>,
    val confirmedDirectDependencies: Int,
    val confirmedIndirectDependencies: Int,
    val pendingProposals: Int,
    val unresolvedCandidates: Int,
    val staleDependencies: Int,
    val unknownCriticalityCount: Int,
    val unverifiedActions: Int,
    val freshnessThresholdDays: Int,
    val now: String,
)

data class ScenarioCoverage(
    val scenarioId: String,
    val coverageLevel: com.pdig.core.generated.CoverageLevel,
    val explanations: List<String>,
    val counts: CoverageCounts,
)

data class CoverageCounts(
    val confirmedDirectDependencies: Int,
    val confirmedIndirectDependencies: Int,
    val pendingProposals: Int,
    val unresolvedCandidates: Int,
    val staleDependencies: Int,
    val unknownCriticalityCount: Int,
    val unverifiedActions: Int,
)
