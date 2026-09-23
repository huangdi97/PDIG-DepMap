package com.pdig.core.impact

import com.pdig.core.domain.Dependency
import com.pdig.core.domain.DependencyGroup
import com.pdig.core.domain.ImpactProposalInput
import com.pdig.core.generated.Capability
import com.pdig.core.generated.ImpactReasonCode
import com.pdig.core.generated.ImpactTargetStatus

/**
 * Impact target-node evaluation - private implementation of simulateScenario,
 * split out of ImpactKernel.kt. Called by simulateScenario, therefore internal
 * (module-visible, not public API). The context the original local fun captured
 * is passed in as parameters; the function body is byte-identical.
 */
private val PAYMENT = Capability.PAYMENT

internal fun evaluateTarget(
    t: String,
    depth: Int,
    activePaymentDeps: List<Dependency>,
    activePaymentGroups: List<DependencyGroup>,
    paymentProposals: List<ImpactProposalInput>,
    nodeName: (String) -> String,
    keyOf: (String) -> String,
    unavailableSet: MutableSet<String>,
    uncertainSet: MutableSet<String>,
    edgeKeysOf: (List<Dependency>) -> List<String>,
): ImpactTargetResult {
        val incoming = activePaymentDeps.filter { it.to == t }
        val lostEdges = incoming.filter { unavailableSet.contains(keyOf(it.from)) }
        val uncertainEdges = incoming.filter { !unavailableSet.contains(keyOf(it.from)) && uncertainSet.contains(it.from) }

        val status: ImpactTargetStatus
        val available: Boolean
        val redundancyDegraded: Boolean
        val reasonCode: ImpactReasonCode
        val reasonText: String
        var edgeKeys: List<String>
        var groupKeys: List<String> = emptyList()
        var proposalKeys: List<String> = emptyList()

        if (lostEdges.isEmpty()) {
            val propKeys = paymentProposals
                .filter { it.to == t && unavailableSet.contains(keyOf(it.from)) }
                .map { it.key }
                .sorted()
            when {
                uncertainEdges.isNotEmpty() -> {
                    status = ImpactTargetStatus.NEEDS_REVIEW
                    available = true
                    redundancyDegraded = false
                    reasonCode = ImpactReasonCode.UPSTREAM_UNCERTAIN
                    reasonText = "上游支付能力存在未确认风险，${nodeName(t)} 的支付是否受影响需人工核实"
                    edgeKeys = edgeKeysOf(uncertainEdges)
                }
                propKeys.isNotEmpty() -> {
                    status = ImpactTargetStatus.NEEDS_REVIEW
                    available = true
                    redundancyDegraded = false
                    reasonCode = ImpactReasonCode.PROPOSAL_ONLY
                    reasonText = "检测到未确认的支付关系建议（置信度不改变结论），需人工核实 ${nodeName(t)} 的支付是否受影响"
                    edgeKeys = emptyList()
                    proposalKeys = propKeys
                }
                else -> {
                    status = ImpactTargetStatus.UNAFFECTED
                    available = true
                    redundancyDegraded = false
                    reasonCode = ImpactReasonCode.CRITICALITY_UNKNOWN
                    reasonText = "未发现受影响的已确认支付关系"
                    edgeKeys = emptyList()
                }
            }
            return ImpactTargetResult(
                nodeId = t,
                nodeName = nodeName(t),
                capability = PAYMENT,
                status = status,
                available = available,
                redundancyDegraded = redundancyDegraded,
                depth = depth,
                reasonCode = reasonCode,
                reasonText = reasonText,
                edgeKeys = edgeKeys,
                groupKeys = groupKeys,
                proposalKeys = proposalKeys,
            )
        }

        val lostEdgeIds = lostEdges.map { it.id }.toSet()
        val coveringGroups = activePaymentGroups.filter { g ->
            g.targetNodeId == t && g.memberEdgeIds.any { lostEdgeIds.contains(it) }
        }

        if (coveringGroups.isNotEmpty()) {
            val groupResults = coveringGroups.map { g ->
                val memberEdges = activePaymentDeps.filter { g.memberEdgeIds.contains(it.id) }
                val availableMembers = memberEdges.filter { !unavailableSet.contains(keyOf(it.from)) }
                val satisfied = if (g.mode == com.pdig.core.generated.GroupMode.ANY) {
                    availableMembers.isNotEmpty()
                } else {
                    availableMembers.size == memberEdges.size
                }
                satisfied
            }
            val allFailed = groupResults.all { !it }
            return if (allFailed) {
                ImpactTargetResult(
                    nodeId = t,
                    nodeName = nodeName(t),
                    capability = PAYMENT,
                    status = ImpactTargetStatus.MUST_CHANGE,
                    available = false,
                    redundancyDegraded = false,
                    depth = depth,
                    reasonCode = ImpactReasonCode.CONFIRMED_GROUP_FAILED,
                    reasonText = "已确认的支付来源组合（${coveringGroups.joinToString("/") { it.mode.wire }}）全部失效，${nodeName(t)} 的支付能力将失效",
                    edgeKeys = edgeKeysOf(lostEdges),
                    groupKeys = coveringGroups.map { it.groupKey }.sorted(),
                    proposalKeys = emptyList(),
                )
            } else {
                ImpactTargetResult(
                    nodeId = t,
                    nodeName = nodeName(t),
                    capability = PAYMENT,
                    status = ImpactTargetStatus.BACKUP_PATH,
                    available = true,
                    redundancyDegraded = true,
                    depth = depth,
                    reasonCode = ImpactReasonCode.CONFIRMED_GROUP_COVERED,
                    reasonText = "已确认存在替代支付来源，${nodeName(t)} 的支付可继续，但冗余度下降（能力降级）",
                    edgeKeys = edgeKeysOf(lostEdges),
                    groupKeys = coveringGroups.map { it.groupKey }.sorted(),
                    proposalKeys = emptyList(),
                )
            }
        }

        val otherEdges = incoming.filter { !lostEdgeIds.contains(it.id) }
        if (otherEdges.isNotEmpty()) {
            return ImpactTargetResult(
                nodeId = t,
                nodeName = nodeName(t),
                capability = PAYMENT,
                status = ImpactTargetStatus.NEEDS_REVIEW,
                available = true,
                redundancyDegraded = false,
                depth = depth,
                reasonCode = ImpactReasonCode.UNCONFIRMED_ALTERNATIVE_EXISTS,
                reasonText = "检测到其他支付来源，但未确认备用组合可自动接管，需人工核实 ${nodeName(t)} 的支付路径",
                edgeKeys = edgeKeysOf(incoming),
                groupKeys = emptyList(),
                proposalKeys = emptyList(),
            )
        }

        if (lostEdges.any { it.criticality == com.pdig.core.generated.Criticality.REQUIRED }) {
            return ImpactTargetResult(
                nodeId = t,
                nodeName = nodeName(t),
                capability = PAYMENT,
                status = ImpactTargetStatus.MUST_CHANGE,
                available = false,
                redundancyDegraded = false,
                depth = depth,
                reasonCode = ImpactReasonCode.REQUIRED_EDGE_NO_ALTERNATIVE,
                reasonText = "已确认 ${nodeName(t)} 的支付能力依赖此关系（required），且无其他已记录来源",
                edgeKeys = edgeKeysOf(lostEdges),
                groupKeys = emptyList(),
                proposalKeys = emptyList(),
            )
        }

        return ImpactTargetResult(
            nodeId = t,
            nodeName = nodeName(t),
            capability = PAYMENT,
            status = ImpactTargetStatus.NEEDS_REVIEW,
            available = true,
            redundancyDegraded = false,
            depth = depth,
            reasonCode = ImpactReasonCode.CRITICALITY_UNKNOWN,
            reasonText = "该支付关系未确认是否必需（criticality=unknown），需人工核实 ${nodeName(t)} 是否受影响",
            edgeKeys = edgeKeysOf(lostEdges),
            groupKeys = emptyList(),
            proposalKeys = emptyList(),
        )
}
