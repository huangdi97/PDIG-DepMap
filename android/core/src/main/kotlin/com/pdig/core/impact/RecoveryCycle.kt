package com.pdig.core.impact

import com.pdig.core.domain.Dependency
import com.pdig.core.generated.Capability
import com.pdig.core.generated.DependencyState
import com.pdig.core.generated.RecoveryCycleStatus

/**
 * RecoveryCycle — Canonical v0.3.0（port of core/src/impact/recovery-cycle.ts）。
 *
 * 铁律：
 * - 只有 Confirmed Reality 上的 active 边才能产生 confirmed_cycle（V030-RC-01）。
 * - Proposal / Candidate 最多产生 potential_cycle。
 * - retired 边不参与（如同 Impact kernel 的 retired 不传播）。
 * - capability 分开检测（mixed capability 不产生 confirmed cycle）。
 */

data class HintedEdge(val from: String, val to: String)

data class RecoveryCycleInput(
    val capability: Capability,
    /** 已确认 Reality 的 active 边（同一 capability）。 */
    val dependencies: List<Dependency>,
    /** pending Proposal / DiscoveryCandidate 暗示的边（同一 capability）。 */
    val hintedEdges: List<HintedEdge>,
    /** 检测上限，防止超长路径爆栈。 */
    val maxPathLength: Int? = null,
)

data class RecoveryCycleResult(
    val status: RecoveryCycleStatus,
    /** confirmed 环的节点路径（若有；确定性排序）。 */
    val confirmedCycles: List<List<String>>,
    /** 仅由 proposal/candidate 支撑的潜在环（用户需确认）。 */
    val potentialCycles: List<List<String>>,
    /** 参与检测的 capability。 */
    val capability: Capability,
)

private data class Edge(val from: String, val to: String)

private const val DEFAULT_MAX_PATH_LENGTH = 16

/**
 * 在「边集」内找有向环。为确定性输出：环被归一化为
 * 「字典序最小的旋转 + 首节点最小」后去重排序。
 */
private fun findCycles(edges: List<Edge>, maxLen: Int): List<List<String>> {
    val adj = mutableMapOf<String, MutableList<String>>()
    for (e in edges) {
        adj.getOrPut(e.from) { mutableListOf() }.add(e.to)
    }
    // 确定性遍历顺序
    for (list in adj.values) list.sort()

    val cycles = mutableSetOf<String>()
    val nodes = adj.keys.sorted()

    for (start in nodes) {
        val stack = mutableListOf(start)
        val visited = mutableSetOf(start)
        fun walk(current: String) {
            val nexts = adj[current] ?: emptyList()
            for (next in nexts) {
                if (next == start) {
                    if (stack.size >= 2 && stack.size <= maxLen) {
                        cycles.add(normalizeCycle(stack))
                    }
                    continue
                }
                if (visited.contains(next)) continue
                if (stack.size >= maxLen) continue
                visited.add(next)
                stack.add(next)
                walk(next)
                stack.removeAt(stack.size - 1)
                visited.remove(next)
            }
        }
        walk(start)
    }

    return cycles
        .map { it.split("|") }
        .sortedWith { a, b ->
            val n = minOf(a.size, b.size)
            for (i in 0 until n) {
                val la = a[i]
                val lb = b[i]
                if (la != lb) return@sortedWith if (la < lb) -1 else 1
            }
            a.size - b.size
        }
}

/** 环归一化：找字典序最小的旋转（并要求该旋转首节点最小）。 */
private fun normalizeCycle(path: List<String>): String {
    val rotations = path.indices.map { i -> path.subList(i, path.size) + path.subList(0, i) }
    val best = rotations.map { r -> r.joinToString("|") }.sorted().firstOrNull()
    return best ?: path.joinToString("|")
}

fun detectRecoveryCycles(input: RecoveryCycleInput): RecoveryCycleResult {
    val maxLen = input.maxPathLength ?: DEFAULT_MAX_PATH_LENGTH
    val confirmedEdges = input.dependencies
        .filter { it.capability == input.capability && it.state == DependencyState.ACTIVE }
        .map { Edge(it.from, it.to) }
    // 自环无意义（恢复自依赖需要另一跳才成环）
    val hinted = input.hintedEdges.filter { it.from != it.to }

    val confirmedCycles = findCycles(confirmedEdges, maxLen)
    if (confirmedCycles.isNotEmpty()) {
        return RecoveryCycleResult(
            status = RecoveryCycleStatus.CONFIRMED_CYCLE,
            confirmedCycles = confirmedCycles,
            potentialCycles = emptyList(),
            capability = input.capability,
        )
    }

    // 潜在环：confirmed 边 + hinted 边 的并集上存在环，且该环必须用到至少一条 hinted 边，
    // 否则它就是 confirmed 环（上一分支会抓住）。
    val merged = confirmedEdges + hinted.map { Edge(it.from, it.to) }
    val allCycles = findCycles(merged, maxLen)
    val potentialCycles = allCycles.filter { cycle ->
        cycle.indices.any { i ->
            val from = cycle[i]
            val to = cycle[(i + 1) % cycle.size]
            hinted.any { h -> h.from == from && h.to == to }
        }
    }

    if (potentialCycles.isNotEmpty()) {
        return RecoveryCycleResult(
            status = RecoveryCycleStatus.POTENTIAL_CYCLE,
            confirmedCycles = emptyList(),
            potentialCycles = potentialCycles,
            capability = input.capability,
        )
    }
    return RecoveryCycleResult(
        status = RecoveryCycleStatus.NO_CYCLE,
        confirmedCycles = emptyList(),
        potentialCycles = emptyList(),
        capability = input.capability,
    )
}
