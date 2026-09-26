import type { Capability, Dependency } from './types.ts'

/**
 * FailureDomain — Canonical vNext (v0.3.0)
 *
 * 铁律：
 * - FailureDomain 本身服从 Reality Boundary：只有用户确认或权威证据才允许 status=confirmed；
 *   机器推断最多产生 needs_review（V030-FD-01 / FD-CONFIRM-GATE）。
 * - 两个恢复/认证路径共享同一故障域 → 不独立（V030-IP-01）。
 * - pathCount ≠ independentPathCount：路径数多不代表独立路径多。
 */

export type FailureDomainKind = 'DEVICE' | 'PHONE_NUMBER' | 'ACCOUNT' | 'PROVIDER'
export type FailureDomainStatus = 'confirmed' | 'needs_review'

export interface FailureDomain {
  id: string
  kind: FailureDomainKind
  /** 故障域主体（device id / phone anchor id / account id / provider name）。 */
  subjectRef: string
  scope: string
  evidenceRefs: string[]
  status: FailureDomainStatus
  confirmedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface PathIndependenceResult {
  targetNodeId: string
  capability: Capability
  /** 候选恢复/认证路径总数（confirmed active edges 指向 target 的边）。 */
  pathCount: number
  /** 互不共享故障域的路径数。 */
  independentPathCount: number
  /** 被确认共享的故障域（dedup + 确定性排序）。 */
  sharedFailureDomains: string[]
  /** 机器无法自行判定的假设（needs_review 的共享可能）。 */
  unresolvedAssumptions: string[]
}

export interface PathIndependenceInput {
  targetNodeId: string
  capability: Capability
  /** 只使用 active + 指定 capability 的边（retired 排除）。 */
  dependencies: Dependency[]
  /** 已确认的故障域。key = `${kind}|${subjectRef}`，value 为 id。 */
  confirmedDomains: Map<string, string>
  /** 机器推断的可能故障域（needs_review）。 */
  suspectedDomains: Map<string, string>
  /** 边 id → 它所属的故障域 key 列表。 */
  edgeToDomainKeys: Map<string, string[]>
}

function sortUnique(items: string[]): string[] {
  return [...new Set(items)].sort()
}

/**
 * 计算 target 的恢复/认证路径独立程度。
 * 确定性：结果字段全部排序；共享故障域取 union 后排序。
 */
export function computePathIndependence(input: PathIndependenceInput): PathIndependenceResult {
  const edges = input.dependencies.filter(
    (d) => d.to === input.targetNodeId && d.capability === input.capability && d.state === 'active',
  )
  const pathCount = edges.length

  const sharedKeys = sortUnique(edges.flatMap((e) => input.edgeToDomainKeys.get(e.id) ?? []))
  const sharedFailureDomains = sharedKeys
    .map((k) => input.confirmedDomains.get(k))
    .filter((id): id is string => id !== undefined)
  const unresolvedAssumptions = sharedKeys
    .map((k) => input.suspectedDomains.get(k))
    .filter((id): id is string => id !== undefined)

  // independentPathCount：在 confirmed 故障域上去重后的有效路径数。
  // 每个 confirmed 故障域内的多余路径只算 1 条（same phone 2 条 → 1；不同设备 → 2）。
  const domainCounts = new Map<string, number>()
  for (const e of edges) {
    for (const key of input.edgeToDomainKeys.get(e.id) ?? []) {
      if (input.confirmedDomains.has(key)) {
        domainCounts.set(key, (domainCounts.get(key) ?? 0) + 1)
      }
    }
  }
  let redundancy = 0
  for (const count of domainCounts.values()) {
    redundancy += Math.max(0, count - 1)
  }
  const independentPathCount = Math.max(0, pathCount - redundancy)

  return {
    targetNodeId: input.targetNodeId,
    capability: input.capability,
    pathCount,
    independentPathCount,
    sharedFailureDomains: sortUnique(sharedFailureDomains),
    unresolvedAssumptions: sortUnique(unresolvedAssumptions),
  }
}

/**
 * 构建 `edgeToDomainKeys`：把「故障域 subject（device/phone/provider）→ 边」的映射
 * 变成边 → 故障域 key 的反向索引。kind 从节点 kind/subtype 推导，由调用方提供。
 */
export function buildEdgeToDomainIndex(
  edges: Array<{ id: string; from: string }>,
  subjectToEdges: Map<string, string[]>,
): Map<string, string[]> {
  const index = new Map<string, string[]>()
  for (const [subject, edgeIds] of subjectToEdges) {
    for (const edgeId of edgeIds) {
      const existing = index.get(edgeId) ?? []
      existing.push(subject)
      index.set(edgeId, existing)
    }
  }
  return index
}
