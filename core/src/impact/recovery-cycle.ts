import type { Capability, Dependency } from '../domain/types.ts'

/**
 * RecoveryCycle — Canonical vNext (v0.3.0)
 *
 * 铁律：
 * - 只有 Confirmed Reality 上的 active 边才能产生 confirmed_cycle（V030-RC-01）。
 * - Proposal / Candidate 最多产生 potential_cycle。
 * - retired 边不参与（如同 Impact kernel 的 retired 不传播）。
 * - capability 分开检测（mixed capability 不产生 confirmed cycle）。
 */

export type RecoveryCycleStatus = 'confirmed_cycle' | 'potential_cycle' | 'no_cycle'

export interface RecoveryCycleResult {
  status: RecoveryCycleStatus
  /** confirmed 环的节点路径（若有；确定性排序）。 */
  confirmedCycles: string[][]
  /** 仅由 proposal/candidate 支撑的潜在环（用户需确认）。 */
  potentialCycles: string[][]
  /** 参与检测的 capability。 */
  capability: Capability
}

export interface RecoveryCycleInput {
  capability: Capability
  /** 已确认 Reality 的 active 边（同一 capability）。 */
  dependencies: Dependency[]
  /** pending Proposal / DiscoveryCandidate 暗示的边（同一 capability）。 */
  hintedEdges: Array<{ from: string; to: string }>
  /** 检测上限，防止超长路径爆栈。 */
  maxPathLength?: number
}

const DEFAULT_MAX_PATH_LENGTH = 16

/**
 * 在「边集」内找有向环。为确定性输出：环被归一化为
 * 「字典序最小的旋转 + 首节点最小」后去重排序。
 */
function findCycles(edges: Array<{ from: string; to: string }>, maxLen: number): string[][] {
  const adj = new Map<string, string[]>()
  for (const e of edges) {
    const list = adj.get(e.from) ?? []
    list.push(e.to)
    adj.set(e.from, list)
  }
  // 确定性遍历顺序
  for (const list of adj.values()) list.sort()

  const cycles = new Set<string>()
  const nodes = [...adj.keys()].sort()

  for (const start of nodes) {
    const stack: string[] = [start]
    const visited = new Set<string>([start])
    walk(start)
    function walk(current: string): void {
      const nexts = adj.get(current) ?? []
      for (const next of nexts) {
        if (next === start) {
          if (stack.length >= 2 && stack.length <= maxLen) {
            cycles.add(normalizeCycle(stack))
          }
          continue
        }
        if (visited.has(next)) continue
        if (stack.length >= maxLen) continue
        visited.add(next)
        stack.push(next)
        walk(next)
        stack.pop()
        visited.delete(next)
      }
    }
  }

  return [...cycles]
    .map((c) => c.split('|'))
    .sort((a, b) => {
      const n = Math.min(a.length, b.length)
      for (let i = 0; i < n; i++) {
        const la = a[i]
        const lb = b[i]
        if (la !== undefined && lb !== undefined && la !== lb) return la < lb ? -1 : 1
      }
      return a.length - b.length
    })
}

/** 环归一化：找字典序最小的旋转（并要求该旋转首节点最小）。 */
function normalizeCycle(path: string[]): string {
  const rotations = path.map((_, i) => path.slice(i).concat(path.slice(0, i)))
  const best = rotations.map((r) => r.join('|')).sort((a, b) => (a < b ? -1 : a > b ? 1 : 0))[0]
  return best ?? path.join('|')
}

export function detectRecoveryCycles(input: RecoveryCycleInput): RecoveryCycleResult {
  const maxLen = input.maxPathLength ?? DEFAULT_MAX_PATH_LENGTH
  const confirmedEdges = input.dependencies
    .filter((d) => d.capability === input.capability && d.state === 'active')
    .map((d) => ({ from: d.from, to: d.to }))
  const hinted = input.hintedEdges.filter(
    (e) => e.from !== e.to, // 自环无意义（恢复自依赖需要另一跳才成环）
  )

  const confirmedCycles = findCycles(confirmedEdges, maxLen)
  if (confirmedCycles.length > 0) {
    return {
      status: 'confirmed_cycle',
      confirmedCycles,
      potentialCycles: [],
      capability: input.capability,
    }
  }

  // 潜在环：confirmed 边 + hinted 边 的并集上存在环，且该环必须用到至少一条 hinted 边，
  // 否则它就是 confirmed 环（上一分支会抓住）。
  const merged = [...confirmedEdges, ...hinted]
  const allCycles = findCycles(merged, maxLen)
  const potentialCycles = allCycles.filter((cycle) => {
    for (let i = 0; i < cycle.length; i++) {
      const from = cycle[i]
      const to = cycle[(i + 1) % cycle.length]
      if (hinted.some((h) => h.from === from && h.to === to)) return true
    }
    return false
  })

  if (potentialCycles.length > 0) {
    return {
      status: 'potential_cycle',
      confirmedCycles: [],
      potentialCycles,
      capability: input.capability,
    }
  }
  return {
    status: 'no_cycle',
    confirmedCycles: [],
    potentialCycles: [],
    capability: input.capability,
  }
}
