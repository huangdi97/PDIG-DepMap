import { describe, expect, it } from 'vitest'
import {
  simulateScenario,
  type Dependency,
  type DependencyGroup,
  type ImpactStateKey,
} from '../../src/index.ts'

/**
 * Engineering Baseline V1 — 针对 mutation baseline 中幸存的关键语义变异补测：
 *   M-K1 同严重度合并：同深度同严重度重评估必须并集 edgeKeys（severity merge tie）
 *   M-K2 confirmed group 失败/覆盖路径：groupKeys 必须输出 group id 且按序
 *   M-K3 proposal_only：available=true 且 edgeKeys 为空（建议项不改变可用性）
 * 见 docs/MUTATION_TEST_REPORT.md。
 */

const T0 = '2026-01-01T00:00:00Z'

function dep(
  id: string,
  from: string,
  to: string,
  criticality: 'required' | 'unknown',
): Dependency {
  return {
    id,
    from,
    relation: 'funding_source',
    to,
    capability: 'payment',
    criticality,
    groupId: null,
    state: 'active',
    origin: 'manual',
    confirmedAt: T0,
    lastVerifiedAt: T0,
    retiredAt: null,
    evidenceRefs: [],
    verificationBasis: { type: 'user_confirmed', verifiedAt: T0 },
    createdAt: T0,
    updatedAt: T0,
  }
}

function group(
  id: string,
  target: string,
  mode: 'ANY' | 'ALL',
  members: string[],
): DependencyGroup {
  return {
    id,
    groupKey: `gk-${id}`,
    targetNodeId: target,
    capability: 'payment',
    mode,
    memberEdgeIds: members,
    state: 'active',
    confirmedAt: T0,
    lastVerifiedAt: T0,
    verificationBasis: { type: 'user_confirmed', verifiedAt: T0 },
    createdAt: T0,
    updatedAt: T0,
  }
}

describe('Impact kernel — mutation baseline 补测（Engineering Baseline V1）', () => {
  it('M-K1: 同深度同严重度重评估 —— edgeKeys 并集且排序', () => {
    // card-a 与 card-b 同时失效，各自有一条 required 边指向 svc（同深度评估两次）
    const deps = [dep('e1', 'card-a', 'svc', 'required'), dep('e2', 'card-b', 'svc', 'required')]
    const unavailable = new Set<ImpactStateKey>([
      { nodeId: 'card-a', capability: 'payment' },
      { nodeId: 'card-b', capability: 'payment' },
    ])
    const r = simulateScenario({ dependencies: deps, groups: [] }, unavailable)
    const svc = r.targets.find((t) => t.nodeId === 'svc')
    expect(svc?.status).toBe('must_change')
    expect(svc?.edgeKeys).toEqual([
      'card-a|funding_source|svc|payment',
      'card-b|funding_source|svc|payment',
    ])
  })

  it('M-K2: confirmed group 失败 → groupKeys 输出该组且排序；covered → backup_path', () => {
    const failed = [dep('e1', 'card-a', 'svc', 'required'), dep('e2', 'card-b', 'svc', 'unknown')]
    const gAny = group('g1', 'svc', 'ANY', ['e1', 'e2'])
    // 两个成员都失效 → ANY 全失效 → must_change（confirmed_group_failed）
    const rFail = simulateScenario(
      { dependencies: failed, groups: [gAny] },
      new Set<ImpactStateKey>([
        { nodeId: 'card-a', capability: 'payment' },
        { nodeId: 'card-b', capability: 'payment' },
      ]),
    )
    const failTarget = rFail.targets.find((t) => t.nodeId === 'svc')
    expect(failTarget?.status).toBe('must_change')
    expect(failTarget?.reasonCode).toBe('confirmed_group_failed')
    expect(failTarget?.groupKeys).toEqual(['gk-g1'])

    // 一员失效一员存活 → ANY 覆盖 → backup_path（confirmed_group_covered）
    const covered = [dep('e1', 'card-a', 'svc', 'required'), dep('e2', 'card-b', 'svc', 'unknown')]
    const rCover = simulateScenario(
      { dependencies: covered, groups: [gAny] },
      new Set<ImpactStateKey>([{ nodeId: 'card-a', capability: 'payment' }]),
    )
    const coverTarget = rCover.targets.find((t) => t.nodeId === 'svc')
    expect(coverTarget?.status).toBe('backup_path')
    expect(coverTarget?.reasonCode).toBe('confirmed_group_covered')
    expect(coverTarget?.groupKeys).toEqual(['gk-g1'])
    expect(coverTarget?.available).toBe(true)
    expect(coverTarget?.redundancyDegraded).toBe(true)
  })

  it('M-K3: proposal_only 目标 —— available=true、edgeKeys 空、proposalKeys 记录', () => {
    // 目标 svc 无任何确认入边，仅 pending proposal 指向它
    const r = simulateScenario(
      {
        dependencies: [],
        groups: [],
        proposals: [{ key: 'p1', from: 'card-a', to: 'svc', capability: 'payment' }],
      },
      new Set<ImpactStateKey>([{ nodeId: 'card-a', capability: 'payment' }]),
    )
    const svc = r.targets.find((t) => t.nodeId === 'svc')
    expect(svc?.status).toBe('needs_review')
    expect(svc?.reasonCode).toBe('proposal_only')
    expect(svc?.available).toBe(true)
    expect(svc?.edgeKeys).toEqual([])
    expect(svc?.proposalKeys).toEqual(['p1'])
  })
})
