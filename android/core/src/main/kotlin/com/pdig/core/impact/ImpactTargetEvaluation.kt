package com.pdig.core.impact

import com.pdig.core.domain.Dependency
import com.pdig.core.domain.DependencyGroup
import com.pdig.core.domain.ImpactProposalInput
import com.pdig.core.generated.Capability
import com.pdig.core.generated.Criticality
import com.pdig.core.generated.GroupMode
import com.pdig.core.generated.ImpactReasonCode
import com.pdig.core.generated.ImpactTargetStatus

/**
 * Capability-scoped target evaluation used by the unified v0.3.0 Impact kernel.
 *
 * Payment wording remains byte-compatible with the frozen payment fixtures;
 * other runtime capabilities use the Canonical capability-specific terms.
 */
internal fun evaluateTarget(
    t: String,
    depth: Int,
    capability: Capability,
    terms: CapabilityTerms,
    activeDeps: List<Dependency>,
    activeGroups: List<DependencyGroup>,
    proposals: List<ImpactProposalInput>,
    nodeName: (String) -> String,
    keyOf: (String) -> String,
    unavailableSet: MutableSet<String>,
    uncertainSet: MutableSet<String>,
    edgeKeysOf: (List<Dependency>) -> List<String>,
): ImpactTargetResult {
    val incoming = activeDeps.filter { it.to == t }
    val lostEdges = incoming.filter { unavailableSet.contains(keyOf(it.from)) }
    val uncertainEdges = incoming.filter {
        !unavailableSet.contains(keyOf(it.from)) && uncertainSet.contains(it.from)
    }

    if (lostEdges.isEmpty()) {
        val propKeys = proposals
            .filter { it.to == t && unavailableSet.contains(keyOf(it.from)) }
            .map { it.key }
            .sorted()

        if (uncertainEdges.isNotEmpty()) {
            return ImpactTargetResult(
                nodeId = t,
                nodeName = nodeName(t),
                capability = capability,
                status = ImpactTargetStatus.NEEDS_REVIEW,
                available = true,
                redundancyDegraded = false,
                depth = depth,
                reasonCode = ImpactReasonCode.UPSTREAM_UNCERTAIN,
                reasonText =
                    "上游${terms.capabilityLabel}存在未确认风险，${nodeName(t)} 的${terms.noun}是否受影响需人工核实",
                edgeKeys = edgeKeysOf(uncertainEdges),
                groupKeys = emptyList(),
                proposalKeys = emptyList(),
            )
        }

        if (propKeys.isNotEmpty()) {
            return ImpactTargetResult(
                nodeId = t,
                nodeName = nodeName(t),
                capability = capability,
                status = ImpactTargetStatus.NEEDS_REVIEW,
                available = true,
                redundancyDegraded = false,
                depth = depth,
                reasonCode = ImpactReasonCode.PROPOSAL_ONLY,
                reasonText =
                    "检测到未确认的${terms.relationLabel}建议（置信度不改变结论），需人工核实 ${nodeName(t)} 的${terms.noun}是否受影响",
                edgeKeys = emptyList(),
                groupKeys = emptyList(),
                proposalKeys = propKeys,
            )
        }

        return ImpactTargetResult(
            nodeId = t,
            nodeName = nodeName(t),
            capability = capability,
            status = ImpactTargetStatus.UNAFFECTED,
            available = true,
            redundancyDegraded = false,
            depth = depth,
            reasonCode = ImpactReasonCode.CRITICALITY_UNKNOWN,
            reasonText = "未发现受影响的已确认${terms.relationLabel}",
            edgeKeys = emptyList(),
            groupKeys = emptyList(),
            proposalKeys = emptyList(),
        )
    }

    val lostEdgeIds = lostEdges.map { it.id }.toSet()
    val coveringGroups = activeGroups.filter { group ->
        group.targetNodeId == t && group.memberEdgeIds.any { it in lostEdgeIds }
    }

    if (coveringGroups.isNotEmpty()) {
        val allFailed = coveringGroups.all { group ->
            val memberEdges = activeDeps.filter { it.id in group.memberEdgeIds }
            val availableMembers = memberEdges.filter { !unavailableSet.contains(keyOf(it.from)) }
            when (group.mode) {
                GroupMode.ANY -> availableMembers.isEmpty()
                GroupMode.ALL -> availableMembers.size != memberEdges.size
            }
        }

        if (allFailed) {
            return ImpactTargetResult(
                nodeId = t,
                nodeName = nodeName(t),
                capability = capability,
                status = ImpactTargetStatus.MUST_CHANGE,
                available = false,
                redundancyDegraded = false,
                depth = depth,
                reasonCode = ImpactReasonCode.CONFIRMED_GROUP_FAILED,
                reasonText =
                    "已确认的${terms.sourceLabel}组合（${coveringGroups.joinToString("/") { it.mode.wire }}）全部失效，${nodeName(t)} 的${terms.capabilityLabel}将失效",
                edgeKeys = edgeKeysOf(lostEdges),
                groupKeys = coveringGroups.map { it.groupKey }.distinct().sorted(),
                proposalKeys = emptyList(),
            )
        }

        return ImpactTargetResult(
            nodeId = t,
            nodeName = nodeName(t),
            capability = capability,
            status = ImpactTargetStatus.BACKUP_PATH,
            available = true,
            redundancyDegraded = true,
            depth = depth,
            reasonCode = ImpactReasonCode.CONFIRMED_GROUP_COVERED,
            reasonText =
                "已确认存在替代${terms.sourceLabel}，${nodeName(t)} 的${terms.noun}可继续，但冗余度下降（能力降级）",
            edgeKeys = edgeKeysOf(lostEdges),
            groupKeys = coveringGroups.map { it.groupKey }.distinct().sorted(),
            proposalKeys = emptyList(),
        )
    }

    val otherEdges = incoming.filter { it.id !in lostEdgeIds }
    if (otherEdges.isNotEmpty()) {
        return ImpactTargetResult(
            nodeId = t,
            nodeName = nodeName(t),
            capability = capability,
            status = ImpactTargetStatus.NEEDS_REVIEW,
            available = true,
            redundancyDegraded = false,
            depth = depth,
            reasonCode = ImpactReasonCode.UNCONFIRMED_ALTERNATIVE_EXISTS,
            reasonText =
                "检测到其他${terms.sourceLabel}，但未确认备用组合可自动接管，需人工核实 ${nodeName(t)} 的${terms.pathLabel}",
            edgeKeys = edgeKeysOf(incoming),
            groupKeys = emptyList(),
            proposalKeys = emptyList(),
        )
    }

    if (lostEdges.any { it.criticality == Criticality.REQUIRED }) {
        return ImpactTargetResult(
            nodeId = t,
            nodeName = nodeName(t),
            capability = capability,
            status = ImpactTargetStatus.MUST_CHANGE,
            available = false,
            redundancyDegraded = false,
            depth = depth,
            reasonCode = ImpactReasonCode.REQUIRED_EDGE_NO_ALTERNATIVE,
            reasonText =
                "已确认 ${nodeName(t)} 的${terms.capabilityLabel}依赖此关系（required），且无其他已记录来源",
            edgeKeys = edgeKeysOf(lostEdges),
            groupKeys = emptyList(),
            proposalKeys = emptyList(),
        )
    }

    return ImpactTargetResult(
        nodeId = t,
        nodeName = nodeName(t),
        capability = capability,
        status = ImpactTargetStatus.NEEDS_REVIEW,
        available = true,
        redundancyDegraded = false,
        depth = depth,
        reasonCode = ImpactReasonCode.CRITICALITY_UNKNOWN,
        reasonText =
            "该${terms.relationLabel}未确认是否必需（criticality=unknown），需人工核实 ${nodeName(t)} 是否受影响",
        edgeKeys = edgeKeysOf(lostEdges),
        groupKeys = emptyList(),
        proposalKeys = emptyList(),
    )
}
