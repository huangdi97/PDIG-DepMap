package com.pdig.core.domain

import com.pdig.core.generated.Capability
import com.pdig.core.generated.DependencyState

/**
 * FailureDomain — Canonical v0.3.0（port of core/src/domain/failure-domain.ts）。
 *
 * 铁律：
 * - FailureDomain 本身服从 Reality Boundary：只有用户确认或权威证据才允许 status=confirmed；
 *   机器推断最多产生 needs_review（V030-FD-01 / FD-CONFIRM-GATE）。
 * - 两个恢复/认证路径共享同一故障域 → 不独立（V030-IP-01）。
 * - pathCount ≠ independentPathCount：路径数多不代表独立路径多。
 */

data class PathIndependenceResult(
    val targetNodeId: String,
    val capability: Capability,
    /** 候选恢复/认证路径总数（confirmed active edges 指向 target 的边）。 */
    val pathCount: Int,
    /** 互不共享故障域的路径数。 */
    val independentPathCount: Int,
    /** 被确认共享的故障域（dedup + 确定性排序）。 */
    val sharedFailureDomains: List<String>,
    /** 机器无法自行判定的假设（needs_review 的共享可能）。 */
    val unresolvedAssumptions: List<String>,
)

data class PathIndependenceInput(
    val targetNodeId: String,
    val capability: Capability,
    /** 只使用 active + 指定 capability 的边（retired 排除）。 */
    val dependencies: List<Dependency>,
    /** 已确认的故障域。key = `${kind}|${subjectRef}`，value 为 id。 */
    val confirmedDomains: Map<String, String>,
    /** 机器推断的可能故障域（needs_review）。 */
    val suspectedDomains: Map<String, String>,
    /** 边 id → 它所属的故障域 key 列表。 */
    val edgeToDomainKeys: Map<String, List<String>>,
)

/** buildEdgeToDomainIndex 的边引用（TS 签名兼容；本函数不读取 from）。 */
data class DomainEdgeRef(val id: String, val from: String)

internal fun sortUnique(items: List<String>): List<String> = items.distinct().sorted()

/**
 * 计算 target 的恢复/认证路径独立程度。
 * 确定性：结果字段全部排序；共享故障域取 union 后排序。
 */
fun computePathIndependence(input: PathIndependenceInput): PathIndependenceResult {
    val edges = input.dependencies.filter { d ->
        d.to == input.targetNodeId && d.capability == input.capability && d.state == DependencyState.ACTIVE
    }
    val pathCount = edges.size

    val sharedKeys = sortUnique(edges.flatMap { e -> input.edgeToDomainKeys[e.id] ?: emptyList() })
    val sharedFailureDomains = sharedKeys.mapNotNull { k -> input.confirmedDomains[k] }
    val unresolvedAssumptions = sharedKeys.mapNotNull { k -> input.suspectedDomains[k] }

    // independentPathCount：在 confirmed 故障域上去重后的有效路径数。
    // 每个 confirmed 故障域内的多余路径只算 1 条（same phone 2 条 → 1；不同设备 → 2）。
    val domainCounts = mutableMapOf<String, Int>()
    for (e in edges) {
        for (key in input.edgeToDomainKeys[e.id] ?: emptyList()) {
            if (input.confirmedDomains.containsKey(key)) {
                domainCounts[key] = (domainCounts[key] ?: 0) + 1
            }
        }
    }
    var redundancy = 0
    for (count in domainCounts.values) redundancy += maxOf(0, count - 1)
    val independentPathCount = maxOf(0, pathCount - redundancy)

    return PathIndependenceResult(
        targetNodeId = input.targetNodeId,
        capability = input.capability,
        pathCount = pathCount,
        independentPathCount = independentPathCount,
        sharedFailureDomains = sortUnique(sharedFailureDomains),
        unresolvedAssumptions = sortUnique(unresolvedAssumptions),
    )
}

/**
 * 构建 `edgeToDomainKeys`：把「故障域 subject（device/phone/provider）→ 边」的映射
 * 变成边 → 故障域 key 的反向索引。kind 从节点 kind/subtype 推导，由调用方提供。
 */
fun buildEdgeToDomainIndex(
    edges: List<DomainEdgeRef>,
    subjectToEdges: Map<String, List<String>>,
): Map<String, List<String>> {
    val index = mutableMapOf<String, MutableList<String>>()
    for ((subject, edgeIds) in subjectToEdges) {
        for (edgeId in edgeIds) {
            index.getOrPut(edgeId) { mutableListOf() }.add(subject)
        }
    }
    return index
}
