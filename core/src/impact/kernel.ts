import type { Capability, Dependency, DependencyGroup } from '../domain/types.ts'
import { dependencyLogicalKey } from '../domain/types.ts'

/**
 * Impact Kernel — MVP payment domain (CANONICAL_DESIGN §7 / GOAL_MVP01 §7)
 *
 * 铁律：
 * - 状态键是 (nodeId, capability)，不是 nodeId（禁止 visited: Set<nodeId>）
 * - 图允许有环：wave-BFS + visited key，每 key 最多处理一次
 * - Proposal 任何 confidence 都不参与确定性失效传播（最多 needs_review）
 * - confirmed Group 未满足性判定只在同 capability 内
 * - must_change 必须可追溯到 confirmed reality（false positive = 0）
 */

export interface ImpactStateKey {
  nodeId: string
  capability: Capability
}

export interface ImpactProposalInput {
  key: string
  from: string
  to: string
  capability: Capability
  confidenceScore?: number
}

export interface ImpactGraph {
  /** 允许包含 retired 边；kernel 只使用 active 且 capability=payment 的边做传播。 */
  dependencies: Dependency[]
  /** 只使用 active confirmed groups。 */
  groups: DependencyGroup[]
  /** pending proposals（仅报告用途，永不产生 must_change）。 */
  proposals?: ImpactProposalInput[]
  nodeNames?: Record<string, string>
}

export type ImpactLevel =
  | 'must_change'
  | 'backup_path'
  | 'degraded'
  | 'needs_review'
  | 'unaffected'
  | 'target_operation'

export interface ImpactTargetResult {
  nodeId: string
  nodeName: string
  capability: Capability
  status: Exclude<ImpactLevel, 'target_operation'>
  available: boolean
  redundancyDegraded: boolean
  /** 失效传播深度（0 = 初始 unavailable 自身不产生 target；受影响目标从 1 开始） */
  depth: number
  reasonCode:
    | 'required_edge_no_alternative'
    | 'confirmed_group_failed'
    | 'confirmed_group_covered'
    | 'unconfirmed_alternative_exists'
    | 'criticality_unknown'
    | 'proposal_only'
    | 'upstream_uncertain'
  reasonText: string
  edgeKeys: string[]
  groupKeys: string[]
  proposalKeys: string[]
}

export interface ImpactChecklistItem {
  level: ImpactLevel
  nodeId: string | null
  capability: Capability | null
  title: string
  detail: string
}

export interface ImpactResult {
  unavailable: ImpactStateKey[]
  /** 最终失效键集合（含初始 unavailable），每个 key 至多出现一次 */
  lostKeys: ImpactStateKey[]
  /** 按 (depth, nodeId, capability) 确定性排序 */
  targets: ImpactTargetResult[]
  /** Action Checklist；原始注销/停用动作强制最后 */
  checklist: ImpactChecklistItem[]
  /** 处理过的 key（顺序记录，无重复）— 用于环终止测试 */
  processedKeys: string[]
}

const PAYMENT: Capability = 'payment'

function keyStr(k: ImpactStateKey): string {
  return `${k.nodeId}|${k.capability}`
}

function sortKeys(keys: ImpactStateKey[]): ImpactStateKey[] {
  return [...keys].sort((a, b) =>
    a.nodeId < b.nodeId ? -1 : a.nodeId > b.nodeId ? 1 : a.capability < b.capability ? -1 : a.capability > b.capability ? 1 : 0
  )
}

export function simulateDisable(
  graph: ImpactGraph,
  nodeId: string,
  capability: Capability = PAYMENT
): ImpactResult {
  return simulateScenario(graph, new Set([{ nodeId, capability }]))
}

export function simulateScenario(graph: ImpactGraph, unavailable: Set<ImpactStateKey>): ImpactResult {
  // MVP 只支持 payment domain：非 payment 初始键忽略（记录在文档）
  const initial = sortKeys([...unavailable].filter(k => k.capability === PAYMENT))

  const activePaymentDeps = graph.dependencies
    .filter(d => d.state === 'active' && d.capability === PAYMENT)
    .sort((a, b) => (a.id < b.id ? -1 : a.id > b.id ? 1 : 0))
  const activePaymentGroups = graph.groups
    .filter(g => g.state === 'active' && g.capability === PAYMENT)
    .sort((a, b) => (a.id < b.id ? -1 : a.id > b.id ? 1 : 0))
  const paymentProposals = (graph.proposals ?? []).filter(p => p.capability === PAYMENT)

  const nodeName = (id: string): string => graph.nodeNames?.[id] ?? id

  const unavailableSet = new Set(initial.map(keyStr))
  const lostKeys: ImpactStateKey[] = [...initial]
  const processedKeys: string[] = initial.map(keyStr)
  const results = new Map<string, ImpactTargetResult>()

  /** 评估目标节点 T 的 payment 状态（基于当前 unavailableSet 快照）。 */
  function evaluateTarget(T: string, depth: number): ImpactTargetResult {
    const incoming = activePaymentDeps.filter(d => d.to === T)
    const lostEdges = incoming.filter(d => unavailableSet.has(`${d.from}|${d.capability}`))
    const uncertainEdges = incoming.filter(
      d => !unavailableSet.has(`${d.from}|${d.capability}`) && uncertainSet.has(d.from)
    )
    const edgeKeysOf = (ds: Dependency[]) => ds.map(d => dependencyLogicalKey(d))

    const base = {
      nodeId: T,
      nodeName: nodeName(T),
      capability: PAYMENT as Capability,
      depth
    }

    if (lostEdges.length === 0) {
      // 无 confirmed 入边失效；检查上游不确定性与 proposal
      const propKeys = paymentProposals
        .filter(p => p.to === T && unavailableSet.has(`${p.from}|${p.capability}`))
        .map(p => p.key)
        .sort()
      if (uncertainEdges.length > 0) {
        return {
          ...base,
          status: 'needs_review',
          available: true,
          redundancyDegraded: false,
          reasonCode: 'upstream_uncertain',
          reasonText: `上游支付能力存在未确认风险，${nodeName(T)} 的支付是否受影响需人工核实`,
          edgeKeys: edgeKeysOf(uncertainEdges),
          groupKeys: [],
          proposalKeys: []
        }
      }
      if (propKeys.length > 0) {
        return {
          ...base,
          status: 'needs_review',
          available: true,
          redundancyDegraded: false,
          reasonCode: 'proposal_only',
          reasonText: `检测到未确认的支付关系建议（置信度不改变结论），需人工核实 ${nodeName(T)} 的支付是否受影响`,
          edgeKeys: [],
          groupKeys: [],
          proposalKeys: propKeys
        }
      }
      // 与场景无关（不应被调用）
      return {
        ...base,
        status: 'unaffected',
        available: true,
        redundancyDegraded: false,
        reasonCode: 'criticality_unknown',
        reasonText: '未发现受影响的已确认支付关系',
        edgeKeys: [],
        groupKeys: [],
        proposalKeys: []
      }
    }

    const lostEdgeIds = new Set(lostEdges.map(d => d.id))
    const coveringGroups = activePaymentGroups.filter(
      g => g.targetNodeId === T && g.memberEdgeIds.some(id => lostEdgeIds.has(id))
    )

    if (coveringGroups.length > 0) {
      const groupResults = coveringGroups.map(g => {
        const memberEdges = activePaymentDeps.filter(d => g.memberEdgeIds.includes(d.id))
        const availableMembers = memberEdges.filter(d => !unavailableSet.has(`${d.from}|${d.capability}`))
        const satisfied =
          g.mode === 'ANY' ? availableMembers.length > 0 : availableMembers.length === memberEdges.length
        return { g, satisfied, availableCount: availableMembers.length, memberCount: memberEdges.length }
      })
      const allFailed = groupResults.every(r => !r.satisfied)
      if (allFailed) {
        return {
          ...base,
          status: 'must_change',
          available: false,
          redundancyDegraded: false,
          reasonCode: 'confirmed_group_failed',
          reasonText: `已确认的支付来源组合（${coveringGroups.map(g => g.mode).join('/')}）全部失效，${nodeName(T)} 的支付能力将失效`,
          edgeKeys: edgeKeysOf(lostEdges),
          groupKeys: coveringGroups.map(g => g.groupKey).sort(),
          proposalKeys: []
        }
      }
      return {
        ...base,
        status: 'backup_path',
        available: true,
        redundancyDegraded: true,
        reasonCode: 'confirmed_group_covered',
        reasonText: `已确认存在替代支付来源，${nodeName(T)} 的支付可继续，但冗余度下降（能力降级）`,
        edgeKeys: edgeKeysOf(lostEdges),
        groupKeys: coveringGroups.map(g => g.groupKey).sort(),
        proposalKeys: []
      }
    }

    const otherEdges = incoming.filter(d => !lostEdgeIds.has(d.id))
    if (otherEdges.length > 0) {
      return {
        ...base,
        status: 'needs_review',
        available: true,
        redundancyDegraded: false,
        reasonCode: 'unconfirmed_alternative_exists',
        reasonText: `检测到其他支付来源，但未确认备用组合可自动接管，需人工核实 ${nodeName(T)} 的支付路径`,
        edgeKeys: edgeKeysOf(incoming),
        groupKeys: [],
        proposalKeys: []
      }
    }

    if (lostEdges.some(d => d.criticality === 'required')) {
      return {
        ...base,
        status: 'must_change',
        available: false,
        redundancyDegraded: false,
        reasonCode: 'required_edge_no_alternative',
        reasonText: `已确认 ${nodeName(T)} 的支付能力依赖此关系（required），且无其他已记录来源`,
        edgeKeys: edgeKeysOf(lostEdges),
        groupKeys: [],
        proposalKeys: []
      }
    }

    return {
      ...base,
      status: 'needs_review',
      available: true,
      redundancyDegraded: false,
      reasonCode: 'criticality_unknown',
      reasonText: `该支付关系未确认是否必需（criticality=unknown），需人工核实 ${nodeName(T)} 是否受影响`,
      edgeKeys: edgeKeysOf(lostEdges),
      groupKeys: [],
      proposalKeys: []
    }
  }

  // wave-BFS：unavailableSet / uncertainSet 只增长 → 天然防环终止
  // lost 传播确定性失效；needs_review（不确定）也向下游传播“不确定性”，
  // 但永不产生 must_change（CANONICAL §7 场景 A：WeChat needs_review → TencentVideo needs_review）
  const uncertainSet = new Set<string>()
  const initialNodeIds = new Set(initial.map(k => k.nodeId))
  let frontier: string[] = initial.map(k => k.nodeId)
  let depth = 0
  let guard = 0
  const maxGuard = activePaymentDeps.length * 2 + initial.length + 8
  const processedSeen = new Set(processedKeys)

  while (frontier.length > 0 && guard <= maxGuard) {
    guard += 1
    depth += 1
    // 收集本轮需要评估的目标：frontier 的 active payment 出边目标 +
    // 初始 unavailable 节点的 pending proposal 目标（proposal 不驱动下游级联）
    const pending = new Set<string>()
    for (const n of frontier) {
      for (const d of activePaymentDeps) {
        if (d.from === n && !unavailableSet.has(`${d.to}|${d.capability}`)) {
          pending.add(d.to)
        }
      }
      if (initialNodeIds.has(n)) {
        for (const p of paymentProposals) {
          if (p.from === n && !unavailableSet.has(`${p.to}|${p.capability}`)) {
            pending.add(p.to)
          }
        }
      }
    }
    const orderedPending = [...pending].sort()
    const nextFrontier: string[] = []
    let grew = false

    for (const T of orderedPending) {
      const k = `${T}|${PAYMENT}`
      if (unavailableSet.has(k)) continue
      const result = evaluateTarget(T, depth)
      const prev = results.get(k)
      // 状态合并：更差结果覆盖（must_change > backup_path > needs_review > unaffected）
      if (!prev || severity(result.status) > severity(prev.status)) {
        results.set(k, result)
      } else if (prev && severity(result.status) === severity(prev.status)) {
        prev.edgeKeys = [...new Set([...prev.edgeKeys, ...result.edgeKeys])].sort()
        prev.groupKeys = [...new Set([...prev.groupKeys, ...result.groupKeys])].sort()
        prev.proposalKeys = [...new Set([...prev.proposalKeys, ...result.proposalKeys])].sort()
      }
      if (result.status === 'must_change' && !unavailableSet.has(k)) {
        unavailableSet.add(k)
        lostKeys.push({ nodeId: T, capability: PAYMENT })
        if (!processedSeen.has(k)) {
          processedKeys.push(k)
          processedSeen.add(k)
        }
        uncertainSet.delete(T)
        nextFrontier.push(T)
        grew = true
      } else if (result.status === 'needs_review' && !uncertainSet.has(T)) {
        // 不确定性传播：needs_review 下游继续 needs_review，但不参与失效传播
        uncertainSet.add(T)
        if (!processedSeen.has(k)) {
          processedKeys.push(k)
          processedSeen.add(k)
        }
        nextFrontier.push(T)
        grew = true
      }
      // backup_path / unaffected：不传播（支付可继续 / 无关）
    }

    if (!grew) break
    frontier = nextFrontier
  }

  const targets = [...results.values()].sort((a, b) =>
    a.depth !== b.depth
      ? a.depth - b.depth
      : a.nodeId < b.nodeId
        ? -1
        : a.nodeId > b.nodeId
          ? 1
          : a.capability < b.capability
            ? -1
            : 1
  )

  const checklist: ImpactChecklistItem[] = []
  for (const t of targets) {
    if (t.status === 'unaffected') continue
    if (t.status === 'must_change') {
      checklist.push({
        level: 'must_change',
        nodeId: t.nodeId,
        capability: t.capability,
        title: `必须处理：${t.nodeName} 的支付能力将失效`,
        detail: t.reasonText
      })
    } else if (t.status === 'backup_path') {
      checklist.push({
        level: 'backup_path',
        nodeId: t.nodeId,
        capability: t.capability,
        title: `有备用路径：${t.nodeName} 可切换（能力降级）`,
        detail: t.reasonText
      })
    } else {
      checklist.push({
        level: 'needs_review',
        nodeId: t.nodeId,
        capability: t.capability,
        title: `建议检查：${t.nodeName}`,
        detail: t.reasonText
      })
    }
  }
  // 原始注销/停用动作强制最后
  for (const k of initial) {
    checklist.push({
      level: 'target_operation',
      nodeId: k.nodeId,
      capability: k.capability,
      title: `最后一步：注销/停用 ${nodeName(k.nodeId)}（原始操作）`,
      detail: '以上事项处理完成后再执行原始操作。'
    })
  }

  return {
    unavailable: initial,
    lostKeys,
    targets,
    checklist,
    processedKeys
  }
}

function severity(s: ImpactTargetResult['status']): number {
  switch (s) {
    case 'must_change':
      return 3
    case 'backup_path':
    case 'degraded':
      return 2
    case 'needs_review':
      return 1
    default:
      return 0
  }
}
