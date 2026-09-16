package com.pdig.core.impact

import com.pdig.core.domain.Dependency
import com.pdig.core.domain.ImpactGraph
import com.pdig.core.domain.ImpactProposalInput
import com.pdig.core.domain.ImpactStateKey
import com.pdig.core.domain.dependencyLogicalKey
import com.pdig.core.generated.Capability
import com.pdig.core.generated.ImpactReasonCode
import com.pdig.core.generated.ImpactTargetStatus

/**
 * PDIG Impact Kernel —— MVP payment domain。
 *
 * 这是 core/src/impact/kernel.ts 的**忠实移植**，不是重新设计。
 * 任何行为差异都必须先改 spec/ 再改三端，不得在 Kotlin 侧自行"修正"。
 *
 * 铁律：
 *  - 状态键是 (nodeId, capability)，不是 nodeId（禁止 visited: Set<nodeId>）
 *  - 图允许有环：wave-BFS，每 key 最多处理一次
 *  - Proposal 任何 confidence 都不参与确定性失效传播（最多 needs_review）
 *  - must_change 必须可追溯到 confirmed reality
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
    val level: com.pdig.core.generated.ImpactLevel,
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

private val PAYMENT = Capability.PAYMENT

private fun sortKeys(keys: List<ImpactStateKey>): List<ImpactStateKey> =
    keys.sortedWith(compareBy({ it.nodeId }, { it.capability.wire }))

fun simulateDisable(graph: ImpactGraph, nodeId: String): ImpactResult =
    simulateScenario(graph, listOf(ImpactStateKey(nodeId, PAYMENT)))

fun simulateScenario(graph: ImpactGraph, unavailable: List<ImpactStateKey>): ImpactResult {
    // 非 payment 初始键被忽略；按 keyString 值级去重（processedKeys 唯一不变量）
    val initialSeen = mutableSetOf<String>()
    val initial = sortKeys(
        unavailable.filter { k ->
            if (k.capability != PAYMENT) return@filter false
            if (initialSeen.contains(k.keyString())) return@filter false
            initialSeen.add(k.keyString())
            true
        },
    )

    val activePaymentDeps: List<Dependency> = graph.dependencies
        .filter { it.state == com.pdig.core.generated.DependencyState.ACTIVE && it.capability == PAYMENT }
        .sortedBy { it.id }
    val activePaymentGroups = graph.groups
        .filter { it.state == com.pdig.core.generated.GroupState.ACTIVE && it.capability == PAYMENT }
        .sortedBy { it.id }
    val paymentProposals: List<ImpactProposalInput> = graph.proposals.filter { it.capability == PAYMENT }

    fun nodeName(id: String): String = graph.nodeNames[id] ?: id
    fun keyOf(node: String): String = "$node|${PAYMENT.wire}"

    val unavailableSet = initial.map { it.keyString() }.toMutableSet()
    val lostKeys = initial.toMutableList()
    val processedKeys = initial.map { it.keyString() }.toMutableList()
    val processedSeen = processedKeys.toMutableSet()
    val results = LinkedHashMap<String, ImpactTargetResult>()

    fun edgeKeysOf(deps: List<Dependency>): List<String> =
        deps.map { dependencyLogicalKey(it.from, it.relation, it.to, it.capability) }

    // 不确定性集合：needs_review 向下游传播"不确定"，但永不升级为 must_change
    val uncertainSet = mutableSetOf<String>()

    fun evaluateTarget(t: String, depth: Int): ImpactTargetResult {
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

    // wave-BFS：unavailableSet / uncertainSet 只增长 → 天然防环终止
    val initialNodeIds = initial.map { it.nodeId }.toSet()
    var frontier: List<String> = initial.map { it.nodeId }
    var depth = 0
    var guard = 0
    val maxGuard = activePaymentDeps.size * 2 + initial.size + 8

    while (frontier.isNotEmpty() && guard <= maxGuard) {
        guard += 1
        depth += 1
        val pending = LinkedHashSet<String>()
        for (n in frontier) {
            for (d in activePaymentDeps) {
                if (d.from == n && !unavailableSet.contains(keyOf(d.to))) pending.add(d.to)
            }
            if (initialNodeIds.contains(n)) {
                for (p in paymentProposals) {
                    if (p.from == n && !unavailableSet.contains(keyOf(p.to))) pending.add(p.to)
                }
            }
        }
        val orderedPending = pending.sorted()
        val nextFrontier = mutableListOf<String>()
        var grew = false

        for (t in orderedPending) {
            val k = keyOf(t)
            if (unavailableSet.contains(k)) continue
            val result = evaluateTarget(t, depth)
            val prev = results[k]
            if (prev == null || severity(result.status) > severity(prev.status)) {
                results[k] = result
            } else if (prev != null && severity(result.status) == severity(prev.status)) {
                results[k] = prev.copy(
                    edgeKeys = (prev.edgeKeys + result.edgeKeys).distinct().sorted(),
                    groupKeys = (prev.groupKeys + result.groupKeys).distinct().sorted(),
                    proposalKeys = (prev.proposalKeys + result.proposalKeys).distinct().sorted(),
                )
            }
            when {
                result.status == ImpactTargetStatus.MUST_CHANGE && !unavailableSet.contains(k) -> {
                    unavailableSet.add(k)
                    lostKeys.add(ImpactStateKey(t, PAYMENT))
                    if (!processedSeen.contains(k)) { processedKeys.add(k); processedSeen.add(k) }
                    uncertainSet.remove(t)
                    nextFrontier.add(t)
                    grew = true
                }
                result.status == ImpactTargetStatus.NEEDS_REVIEW && !uncertainSet.contains(t) -> {
                    uncertainSet.add(t)
                    if (!processedSeen.contains(k)) { processedKeys.add(k); processedSeen.add(k) }
                    nextFrontier.add(t)
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
    for (t in targets) {
        when (t.status) {
            ImpactTargetStatus.UNAFFECTED -> Unit
            ImpactTargetStatus.MUST_CHANGE -> checklist.add(
                ImpactChecklistItem(
                    level = com.pdig.core.generated.ImpactLevel.MUST_CHANGE,
                    nodeId = t.nodeId,
                    capability = t.capability,
                    title = "必须处理：${t.nodeName} 的支付能力将失效",
                    detail = t.reasonText,
                ),
            )
            ImpactTargetStatus.BACKUP_PATH -> checklist.add(
                ImpactChecklistItem(
                    level = com.pdig.core.generated.ImpactLevel.BACKUP_PATH,
                    nodeId = t.nodeId,
                    capability = t.capability,
                    title = "有备用路径：${t.nodeName} 可切换（能力降级）",
                    detail = t.reasonText,
                ),
            )
            else -> checklist.add(
                ImpactChecklistItem(
                    level = com.pdig.core.generated.ImpactLevel.NEEDS_REVIEW,
                    nodeId = t.nodeId,
                    capability = t.capability,
                    title = "建议检查：${t.nodeName}",
                    detail = t.reasonText,
                ),
            )
        }
    }
    // 原始注销/停用动作强制最后（IMP-09）
    for (k in initial) {
        checklist.add(
            ImpactChecklistItem(
                level = com.pdig.core.generated.ImpactLevel.TARGET_OPERATION,
                nodeId = k.nodeId,
                capability = k.capability,
                title = "最后一步：注销/停用 ${nodeName(k.nodeId)}（原始操作）",
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

private fun severity(s: ImpactTargetStatus): Int = when (s) {
    ImpactTargetStatus.MUST_CHANGE -> 3
    ImpactTargetStatus.BACKUP_PATH, ImpactTargetStatus.DEGRADED -> 2
    ImpactTargetStatus.NEEDS_REVIEW -> 1
    else -> 0
}
