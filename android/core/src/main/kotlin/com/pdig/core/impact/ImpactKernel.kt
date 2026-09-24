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
            val result = evaluateTarget(
                t,
                depth,
                activePaymentDeps,
                activePaymentGroups,
                paymentProposals,
                ::nodeName,
                ::keyOf,
                unavailableSet,
                uncertainSet,
                ::edgeKeysOf,
            )
            val prev = results[k]
            if (prev == null) {
                results[k] = result
            } else {
                val current = severity(result.status)
                val old = severity(prev.status)
                if (current > old) {
                    results[k] = result
                } else if (current == old) {
                    results[k] = prev.copy(
                        edgeKeys = (prev.edgeKeys + result.edgeKeys).distinct().sorted(),
                        groupKeys = (prev.groupKeys + result.groupKeys).distinct().sorted(),
                        proposalKeys = (prev.proposalKeys + result.proposalKeys).distinct().sorted(),
                    )
                }
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
