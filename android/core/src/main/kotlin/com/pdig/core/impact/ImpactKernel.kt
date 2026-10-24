package com.pdig.core.impact

import com.pdig.core.domain.Dependency
import com.pdig.core.domain.ImpactGraph
import com.pdig.core.domain.ImpactProposalInput
import com.pdig.core.domain.ImpactStateKey
import com.pdig.core.domain.dependencyLogicalKey
import com.pdig.core.generated.Capability
import com.pdig.core.generated.DependencyState
import com.pdig.core.generated.GroupState
import com.pdig.core.generated.ImpactLevel
import com.pdig.core.generated.ImpactReasonCode
import com.pdig.core.generated.ImpactTargetStatus

/**
 * PDIG Impact Kernel — unified (nodeId, capability) propagation.
 *
 * Canonical v0.3.0 rules:
 * - state identity is (nodeId, capability), never nodeId alone;
 * - each capability propagates independently through confirmed active edges/groups;
 * - Proposal confidence never upgrades an uncertain relationship to must_change;
 * - payment output stays compatible with the frozen payment fixtures;
 * - access/authentication/recovery/communication share the same kernel, not
 *   capability-specific side engines.
 */
data class ImpactTargetResult(
    val nodeId: String,
    val nodeName: String,
    val capability: Capability,
    val status: ImpactTargetStatus,
    val available: Boolean,
    val redundancyDegraded: Boolean,
    val depth: Int,
    val reasonCode: ImpactReasonCode,
    val reasonText: String,
    val edgeKeys: List<String>,
    val groupKeys: List<String>,
    val proposalKeys: List<String>,
)

data class ImpactChecklistItem(
    val level: ImpactLevel,
    val nodeId: String?,
    val capability: Capability?,
    val title: String,
    val detail: String,
)

data class ImpactResult(
    val unavailable: List<ImpactStateKey>,
    val lostKeys: List<ImpactStateKey>,
    val targets: List<ImpactTargetResult>,
    val checklist: List<ImpactChecklistItem>,
    val processedKeys: List<String>,
)

internal data class CapabilityTerms(
    val noun: String,
    val capabilityLabel: String,
    val sourceLabel: String,
    val relationLabel: String,
    val pathLabel: String,
    val operationVerb: String,
)

private val PAYMENT = Capability.PAYMENT

private val CAPABILITY_ORDER = listOf(
    Capability.PAYMENT,
    Capability.ACCESS,
    Capability.AUTHENTICATION,
    Capability.RECOVERY,
    Capability.COMMUNICATION,
    Capability.IDENTITY,
)

private val TERMS: Map<Capability, CapabilityTerms> = mapOf(
    Capability.PAYMENT to CapabilityTerms(
        noun = "支付",
        capabilityLabel = "支付能力",
        sourceLabel = "支付来源",
        relationLabel = "支付关系",
        pathLabel = "支付路径",
        operationVerb = "注销/停用",
    ),
    Capability.ACCESS to CapabilityTerms(
        noun = "访问",
        capabilityLabel = "访问能力",
        sourceLabel = "访问来源",
        relationLabel = "访问关系",
        pathLabel = "访问路径",
        operationVerb = "停用",
    ),
    Capability.AUTHENTICATION to CapabilityTerms(
        noun = "认证",
        capabilityLabel = "认证能力",
        sourceLabel = "认证方式",
        relationLabel = "认证关系",
        pathLabel = "认证路径",
        operationVerb = "停用",
    ),
    Capability.RECOVERY to CapabilityTerms(
        noun = "恢复",
        capabilityLabel = "恢复能力",
        sourceLabel = "恢复方式",
        relationLabel = "恢复关系",
        pathLabel = "恢复路径",
        operationVerb = "停用",
    ),
    Capability.COMMUNICATION to CapabilityTerms(
        noun = "通讯",
        capabilityLabel = "通讯能力",
        sourceLabel = "通讯方式",
        relationLabel = "通讯关系",
        pathLabel = "通讯路径",
        operationVerb = "停用",
    ),
    Capability.IDENTITY to CapabilityTerms(
        noun = "身份",
        capabilityLabel = "身份信息",
        sourceLabel = "身份来源",
        relationLabel = "身份关系",
        pathLabel = "身份路径",
        operationVerb = "停用",
    ),
)

private fun sortKeys(keys: List<ImpactStateKey>): List<ImpactStateKey> =
    keys.sortedWith(compareBy({ it.nodeId }, { it.capability.wire }))

fun simulateDisable(
    graph: ImpactGraph,
    nodeId: String,
    capability: Capability = PAYMENT,
): ImpactResult =
    simulateScenario(graph, listOf(ImpactStateKey(nodeId, capability)))

fun simulateScenario(
    graph: ImpactGraph,
    unavailable: List<ImpactStateKey>,
): ImpactResult {
    val initialSeen = mutableSetOf<String>()
    val initial = sortKeys(
        unavailable.filter { key ->
            initialSeen.add(key.keyString())
        },
    )

    val byCapability = initial.groupBy { it.capability }
    val orderedCapabilities = byCapability.keys.sortedBy { capability ->
        CAPABILITY_ORDER.indexOf(capability).let { if (it < 0) Int.MAX_VALUE else it }
    }

    val allTargets = mutableListOf<ImpactTargetResult>()
    val lostKeys = mutableListOf<ImpactStateKey>()
    val processedKeys = mutableListOf<String>()
    val checklist = mutableListOf<ImpactChecklistItem>()

    for (capability in orderedCapabilities) {
        val keys = byCapability[capability].orEmpty()
        if (keys.isEmpty()) continue
        val result = propagateCapability(
            graph = graph,
            initial = keys,
            capability = capability,
            terms = TERMS.getValue(capability),
        )
        allTargets += result.targets
        lostKeys += result.lostKeys
        processedKeys += result.processedKeys
        checklist += result.checklist
    }

    val targets = allTargets.sortedWith(
        compareBy({ it.depth }, { it.nodeId }, { it.capability.wire }),
    )

    return ImpactResult(
        unavailable = initial,
        lostKeys = lostKeys,
        targets = targets,
        checklist = checklist,
        processedKeys = processedKeys,
    )
}

private fun propagateCapability(
    graph: ImpactGraph,
    initial: List<ImpactStateKey>,
    capability: Capability,
    terms: CapabilityTerms,
): ImpactResult {
    val activeDeps: List<Dependency> = graph.dependencies
        .filter { it.state == DependencyState.ACTIVE && it.capability == capability }
        .sortedBy { it.id }
    val activeGroups = graph.groups
        .filter { it.state == GroupState.ACTIVE && it.capability == capability }
        .sortedBy { it.id }
    val proposals: List<ImpactProposalInput> = graph.proposals
        .filter { it.capability == capability }

    fun nodeName(id: String): String = graph.nodeNames[id] ?: id
    fun keyOf(node: String): String = "$node|${capability.wire}"
    fun edgeKeysOf(deps: List<Dependency>): List<String> =
        deps.map { dependencyLogicalKey(it.from, it.relation, it.to, it.capability) }

    val unavailableSet = initial.map { it.keyString() }.toMutableSet()
    val lostKeys = initial.toMutableList()
    val processedKeys = initial.map { it.keyString() }.toMutableList()
    val processedSeen = processedKeys.toMutableSet()
    val results = LinkedHashMap<String, ImpactTargetResult>()
    val uncertainSet = mutableSetOf<String>()

    val initialNodeIds = initial.map { it.nodeId }.toSet()
    var frontier: List<String> = initial.map { it.nodeId }
    var depth = 0
    var guard = 0
    val maxGuard = activeDeps.size * 2 + initial.size + 8

    while (frontier.isNotEmpty() && guard <= maxGuard) {
        guard += 1
        depth += 1
        val pending = LinkedHashSet<String>()

        for (node in frontier) {
            for (dependency in activeDeps) {
                if (
                    dependency.from == node &&
                    !unavailableSet.contains(keyOf(dependency.to))
                ) {
                    pending.add(dependency.to)
                }
            }
            if (node in initialNodeIds) {
                for (proposal in proposals) {
                    if (
                        proposal.from == node &&
                        !unavailableSet.contains(keyOf(proposal.to))
                    ) {
                        pending.add(proposal.to)
                    }
                }
            }
        }

        val nextFrontier = mutableListOf<String>()
        var grew = false

        for (targetNode in pending.sorted()) {
            val key = keyOf(targetNode)
            if (key in unavailableSet) continue

            val result = evaluateTarget(
                t = targetNode,
                depth = depth,
                capability = capability,
                terms = terms,
                activeDeps = activeDeps,
                activeGroups = activeGroups,
                proposals = proposals,
                nodeName = ::nodeName,
                keyOf = ::keyOf,
                unavailableSet = unavailableSet,
                uncertainSet = uncertainSet,
                edgeKeysOf = ::edgeKeysOf,
            )

            val previous = results[key]
            if (previous == null) {
                results[key] = result
            } else {
                val currentSeverity = severity(result.status)
                val oldSeverity = severity(previous.status)
                if (currentSeverity > oldSeverity) {
                    results[key] = result
                } else if (currentSeverity == oldSeverity) {
                    results[key] = previous.copy(
                        edgeKeys = (previous.edgeKeys + result.edgeKeys).distinct().sorted(),
                        groupKeys = (previous.groupKeys + result.groupKeys).distinct().sorted(),
                        proposalKeys =
                            (previous.proposalKeys + result.proposalKeys).distinct().sorted(),
                    )
                }
            }

            when {
                result.status == ImpactTargetStatus.MUST_CHANGE && key !in unavailableSet -> {
                    unavailableSet.add(key)
                    lostKeys.add(ImpactStateKey(targetNode, capability))
                    if (processedSeen.add(key)) processedKeys.add(key)
                    uncertainSet.remove(targetNode)
                    nextFrontier.add(targetNode)
                    grew = true
                }

                result.status == ImpactTargetStatus.NEEDS_REVIEW &&
                    targetNode !in uncertainSet -> {
                    uncertainSet.add(targetNode)
                    if (processedSeen.add(key)) processedKeys.add(key)
                    nextFrontier.add(targetNode)
                    grew = true
                }
            }
        }

        if (!grew) break
        frontier = nextFrontier
    }

    val targets = results.values.sortedWith(
        compareBy({ it.depth }, { it.nodeId }, { it.capability.wire }),
    )

    val checklist = mutableListOf<ImpactChecklistItem>()
    for (target in targets) {
        when (target.status) {
            ImpactTargetStatus.UNAFFECTED -> Unit
            ImpactTargetStatus.MUST_CHANGE -> checklist.add(
                ImpactChecklistItem(
                    level = ImpactLevel.MUST_CHANGE,
                    nodeId = target.nodeId,
                    capability = target.capability,
                    title =
                        "必须处理：${target.nodeName} 的${terms.capabilityLabel}将失效",
                    detail = target.reasonText,
                ),
            )

            ImpactTargetStatus.BACKUP_PATH -> checklist.add(
                ImpactChecklistItem(
                    level = ImpactLevel.BACKUP_PATH,
                    nodeId = target.nodeId,
                    capability = target.capability,
                    title = "有备用路径：${target.nodeName} 可切换（能力降级）",
                    detail = target.reasonText,
                ),
            )

            else -> checklist.add(
                ImpactChecklistItem(
                    level = ImpactLevel.NEEDS_REVIEW,
                    nodeId = target.nodeId,
                    capability = target.capability,
                    title = "建议检查：${target.nodeName}",
                    detail = target.reasonText,
                ),
            )
        }
    }

    for (key in initial) {
        checklist.add(
            ImpactChecklistItem(
                level = ImpactLevel.TARGET_OPERATION,
                nodeId = key.nodeId,
                capability = key.capability,
                title =
                    "最后一步：${terms.operationVerb} ${nodeName(key.nodeId)}（原始操作）",
                detail = "以上事项处理完成后再执行原始操作。",
            ),
        )
    }

    return ImpactResult(
        unavailable = initial,
        lostKeys = lostKeys,
        targets = targets,
        checklist = checklist,
        processedKeys = processedKeys,
    )
}

private fun severity(status: ImpactTargetStatus): Int = when (status) {
    ImpactTargetStatus.MUST_CHANGE -> 3
    ImpactTargetStatus.BACKUP_PATH, ImpactTargetStatus.DEGRADED -> 2
    ImpactTargetStatus.NEEDS_REVIEW -> 1
    else -> 0
}
