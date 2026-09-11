import { describe, expect, it } from 'vitest'
import type {
  Capability,
  Criticality,
  Dependency,
  DependencyGroup,
  Relation,
} from '../../src/domain/types.ts'
import { canonicalGroupKey } from '../../src/domain/types.ts'
import { simulateScenario, simulateDisable } from '../../src/impact/kernel.ts'

// ---------------------------------------------------------------------------
// helpers — 构造纯内存图输入（kernel 只接收 confirmed Dependency / Group）
// ---------------------------------------------------------------------------

let seq = 0
function dep(
  from: string,
  to: string,
  opts: {
    capability?: Capability
    criticality?: Criticality
    relation?: Relation
    state?: 'active' | 'retired'
  } = {},
): Dependency {
  seq += 1
  const rel = opts.relation ?? 'funding_source'
  const cap = opts.capability ?? 'payment'
  return {
    id: `e${seq}`,
    from,
    relation: rel,
    to,
    capability: cap,
    criticality: opts.criticality ?? 'unknown',
    groupId: null,
    state: opts.state ?? 'active',
    origin: 'manual',
    confirmedAt: '2026-01-01T00:00:00.000Z',
    lastVerifiedAt: '2026-01-01T00:00:00.000Z',
    retiredAt: opts.state === 'retired' ? '2026-02-01T00:00:00.000Z' : null,
    evidenceRefs: [],
    createdAt: '2026-01-01T00:00:00.000Z',
    updatedAt: '2026-01-01T00:00:00.000Z',
  }
}

function anyGroup(target: string, members: Dependency[]): DependencyGroup {
  return group(target, 'ANY', members)
}

function allGroup(target: string, members: Dependency[]): DependencyGroup {
  return group(target, 'ALL', members)
}

function group(target: string, mode: 'ANY' | 'ALL', members: Dependency[]): DependencyGroup {
  const memberKeys = members.map((m) => `${m.from}|${m.relation}|${m.to}|${m.capability}`)
  return {
    id: `g-${target}-${mode}`,
    groupKey: canonicalGroupKey(target, 'payment', mode, memberKeys),
    targetNodeId: target,
    capability: 'payment',
    mode,
    memberEdgeIds: members.map((m) => m.id),
    state: 'active',
    confirmedAt: '2026-01-01T00:00:00.000Z',
    lastVerifiedAt: '2026-01-01T00:00:00.000Z',
    createdAt: '2026-01-01T00:00:00.000Z',
    updatedAt: '2026-01-01T00:00:00.000Z',
  }
}

const key = (nodeId: string, capability: Capability = 'payment') => ({ nodeId, capability })

const NODE_NAMES: Record<string, string> = {
  A: '节点A',
  B: '节点B',
  C: '节点C',
  CMB4417: '招行4417',
  CCB8821: '建行8821',
  WeChat: '微信',
  TencentVideo: '腾讯视频',
}

function statusOf(result: ReturnType<typeof simulateScenario>, nodeId: string) {
  const t = result.targets.find((x) => x.nodeId === nodeId)
  expect(t, `target ${nodeId} should be evaluated`).toBeDefined()
  return t!
}

// ---------------------------------------------------------------------------
// T1–T12 + 附加要求（GOAL_MVP01 §7.2）
// ---------------------------------------------------------------------------

describe('Impact Kernel — payment domain (T1–T12)', () => {
  it('T1: required A→B；disable A => B.payment lost (must_change)', () => {
    const e1 = dep('A', 'B', { criticality: 'required' })
    const r = simulateDisable({ dependencies: [e1], groups: [], nodeNames: NODE_NAMES }, 'A')
    const b = statusOf(r, 'B')
    expect(b.status).toBe('must_change')
    expect(b.available).toBe(false)
    expect(b.reasonCode).toBe('required_edge_no_alternative')
  })

  it('T2: A,B ∈ confirmed ANY→C；disable A => C available + redundancy degraded', () => {
    const e1 = dep('A', 'C')
    const e2 = dep('B', 'C')
    const g = anyGroup('C', [e1, e2])
    const r = simulateDisable({ dependencies: [e1, e2], groups: [g], nodeNames: NODE_NAMES }, 'A')
    const c = statusOf(r, 'C')
    expect(c.available).toBe(true)
    expect(c.redundancyDegraded).toBe(true)
    expect(c.status).toBe('backup_path')
    expect(c.reasonCode).toBe('confirmed_group_covered')
  })

  it('T3: A,B ∈ confirmed ALL→C；disable A => C lost', () => {
    const e1 = dep('A', 'C')
    const e2 = dep('B', 'C')
    const g = allGroup('C', [e1, e2])
    const r = simulateDisable({ dependencies: [e1, e2], groups: [g], nodeNames: NODE_NAMES }, 'A')
    const c = statusOf(r, 'C')
    expect(c.status).toBe('must_change')
    expect(c.available).toBe(false)
    expect(c.reasonCode).toBe('confirmed_group_failed')
  })

  it('T4: proposal confidence .999；不得 must_change（Proposal 不参与确定性失效传播）', () => {
    // A→B 只存在于 Proposal 中，不存在 confirmed dependency
    const r = simulateDisable(
      {
        dependencies: [],
        groups: [],
        proposals: [
          {
            key: 'A|funding_source|B|payment',
            from: 'A',
            to: 'B',
            capability: 'payment',
            confidenceScore: 0.999,
          },
        ],
        nodeNames: NODE_NAMES,
      },
      'A',
    )
    const b = statusOf(r, 'B')
    expect(b.status).toBe('needs_review')
    expect(b.reasonCode).toBe('proposal_only')
    expect(b.status).not.toBe('must_change')
  })

  it('T5: 链式 A→B→C；disable A => B lost => C 传播', () => {
    const e1 = dep('A', 'B', { criticality: 'required' })
    const e2 = dep('B', 'C', { criticality: 'required' })
    const r = simulateDisable({ dependencies: [e1, e2], groups: [], nodeNames: NODE_NAMES }, 'A')
    const b = statusOf(r, 'B')
    const c = statusOf(r, 'C')
    expect(b.status).toBe('must_change')
    expect(b.depth).toBe(1)
    expect(c.status).toBe('must_change')
    expect(c.depth).toBe(2)
  })

  it('T6: 环 A→B→C→A 必须终止；每个 ImpactStateKey 最多处理一次', () => {
    const e1 = dep('A', 'B', { criticality: 'required' })
    const e2 = dep('B', 'C', { criticality: 'required' })
    const e3 = dep('C', 'A', { criticality: 'required' })
    const r = simulateDisable(
      { dependencies: [e1, e2, e3], groups: [], nodeNames: NODE_NAMES },
      'A',
    )
    // 终止
    expect(r.lostKeys).toHaveLength(3)
    const lostNodeIds = r.lostKeys.map((k) => k.nodeId).sort()
    expect(lostNodeIds).toEqual(['A', 'B', 'C'])
    // 每个 key 只出现一次
    const seen = new Set(r.lostKeys.map((k) => `${k.nodeId}|${k.capability}`))
    expect(seen.size).toBe(r.lostKeys.length)
    // processedKeys 无重复
    expect(new Set(r.processedKeys).size).toBe(r.processedKeys.length)
    // 大环（60 节点）也必须终止
    const ring: Dependency[] = []
    for (let i = 0; i < 60; i++) {
      ring.push(dep(`n${i}`, `n${(i + 1) % 60}`, { criticality: 'required' }))
    }
    const rr = simulateDisable({ dependencies: ring, groups: [], nodeNames: {} }, 'n0')
    expect(rr.lostKeys).toHaveLength(60)
  })

  it('T7: A,B→C 但 group 未确认；disable A => needs_review，NOT backup_path', () => {
    const e1 = dep('A', 'C')
    const e2 = dep('B', 'C')
    const r = simulateDisable({ dependencies: [e1, e2], groups: [], nodeNames: NODE_NAMES }, 'A')
    const c = statusOf(r, 'C')
    expect(c.status).toBe('needs_review')
    expect(c.status).not.toBe('backup_path')
    expect(c.reasonCode).toBe('unconfirmed_alternative_exists')
  })

  it('T8: A→C criticality unknown 且无 Group；disable A => needs_review', () => {
    const e1 = dep('A', 'C', { criticality: 'unknown' })
    const r = simulateDisable({ dependencies: [e1], groups: [], nodeNames: NODE_NAMES }, 'A')
    const c = statusOf(r, 'C')
    expect(c.status).toBe('needs_review')
    expect(c.reasonCode).toBe('criticality_unknown')
  })

  it('T9: retired dependency 不参与传播', () => {
    const e1 = dep('A', 'B', { criticality: 'required', state: 'retired' })
    const r = simulateDisable({ dependencies: [e1], groups: [], nodeNames: NODE_NAMES }, 'A')
    expect(r.targets).toHaveLength(0)
    expect(r.lostKeys.map((k) => k.nodeId)).toEqual(['A'])
  })

  it('T10: retired dependency reactivated 后重新参与传播', () => {
    const e1 = dep('A', 'B', { criticality: 'required', state: 'retired' })
    const before = simulateDisable({ dependencies: [e1], groups: [], nodeNames: NODE_NAMES }, 'A')
    expect(before.targets).toHaveLength(0)
    const reactivated: Dependency = { ...e1, state: 'active', retiredAt: null }
    const after = simulateDisable(
      { dependencies: [reactivated], groups: [], nodeNames: NODE_NAMES },
      'A',
    )
    expect(statusOf(after, 'B').status).toBe('must_change')
  })

  it('T11: scenario 同时 disable A,B；confirmed ANY group => C lost', () => {
    const e1 = dep('A', 'C')
    const e2 = dep('B', 'C')
    const g = anyGroup('C', [e1, e2])
    const r = simulateScenario(
      { dependencies: [e1, e2], groups: [g], nodeNames: NODE_NAMES },
      new Set([key('A'), key('B')]),
    )
    const c = statusOf(r, 'C')
    expect(c.status).toBe('must_change')
    expect(c.available).toBe(false)
  })

  it('T12: payment lost 不得错误传播到 recovery/access', () => {
    // A→B payment；B→D recovery；B→E access
    const e1 = dep('A', 'B', { criticality: 'required' })
    const e2 = dep('B', 'D', { capability: 'recovery', criticality: 'required' })
    const e3 = dep('B', 'E', { capability: 'access', criticality: 'required' })
    const r = simulateDisable(
      { dependencies: [e1, e2, e3], groups: [], nodeNames: NODE_NAMES },
      'A',
    )
    // 只有 B.payment 受影响
    expect(r.targets.map((t) => `${t.nodeId}:${t.capability}`)).toEqual(['B:payment'])
    expect(r.lostKeys.map((k) => `${k.nodeId}:${k.capability}`)).toEqual(['A:payment', 'B:payment'])
  })
})

describe('Impact Kernel — 输出确定性 (GOAL §7.2 附加)', () => {
  const e1 = dep('CMB4417', 'WeChat', { criticality: 'required' })
  const e2 = dep('WeChat', 'TencentVideo', { criticality: 'required' })

  it('deterministic output order：同输入多次运行结果顺序一致（按 depth,nodeId）', () => {
    const graph = { dependencies: [e1, e2], groups: [], nodeNames: NODE_NAMES }
    const r1 = simulateDisable(graph, 'CMB4417')
    const r2 = simulateDisable(graph, 'CMB4417')
    expect(r1.targets.map((t) => t.nodeId)).toEqual(['WeChat', 'TencentVideo'])
    expect(r1.targets.map((t) => t.nodeId)).toEqual(r2.targets.map((t) => t.nodeId))
    expect(r1.checklist.map((i) => i.title)).toEqual(r2.checklist.map((i) => i.title))
  })

  it('target operation always last in Action Checklist', () => {
    const graph = { dependencies: [e1, e2], groups: [], nodeNames: NODE_NAMES }
    const r = simulateDisable(graph, 'CMB4417')
    expect(r.checklist.length).toBeGreaterThan(1)
    const last = r.checklist[r.checklist.length - 1]!
    expect(last.level).toBe('target_operation')
    expect(last.nodeId).toBe('CMB4417')
    // 其余 item 都不是 target_operation
    expect(r.checklist.slice(0, -1).every((i) => i.level !== 'target_operation')).toBe(true)
  })
})

describe('Impact Kernel — Canonical fixture (CANONICAL_DESIGN §7.9)', () => {
  // Nodes: CMB4417, CCB8821, WeChat, TencentVideo
  // CMB4417 --funding_source/payment--> WeChat
  // CCB8821 --funding_source/payment--> WeChat
  // WeChat  --merchant_agreement/payment--> TencentVideo
  function fixture() {
    const e1 = dep('CMB4417', 'WeChat', { relation: 'funding_source' })
    const e2 = dep('CCB8821', 'WeChat', { relation: 'funding_source' })
    const e3 = dep('WeChat', 'TencentVideo', { relation: 'merchant_agreement' })
    return { e1, e2, e3 }
  }

  it('场景 A：Group 未确认 — disable CMB4417 => WeChat needs_review, TencentVideo needs_review', () => {
    const { e1, e2, e3 } = fixture()
    const r = simulateDisable(
      { dependencies: [e1, e2, e3], groups: [], nodeNames: NODE_NAMES },
      'CMB4417',
    )
    expect(statusOf(r, 'WeChat').status).toBe('needs_review')
    expect(statusOf(r, 'TencentVideo').status).toBe('needs_review')
  })

  it('场景 B：confirmed ANY Group — disable CMB4417 => WeChat available/degraded；TencentVideo 不受影响', () => {
    const { e1, e2, e3 } = fixture()
    const g = anyGroup('WeChat', [e1, e2])
    const r = simulateDisable(
      { dependencies: [e1, e2, e3], groups: [g], nodeNames: NODE_NAMES },
      'CMB4417',
    )
    const wechat = statusOf(r, 'WeChat')
    expect(wechat.available).toBe(true)
    expect(wechat.redundancyDegraded).toBe(true)
    // TencentVideo 的入边 from=WeChat 未失效 → 不受影响，不在结果里
    expect(r.targets.find((t) => t.nodeId === 'TencentVideo')).toBeUndefined()
  })

  it('场景 B2：confirmed ANY Group — 同时 disable 双卡 => WeChat lost => TencentVideo must_change', () => {
    const { e1, e2, e3 } = fixture()
    // merchant_agreement 为用户确认的唯一支付路径（required）
    const e3req = { ...e3, criticality: 'required' as const }
    const g = anyGroup('WeChat', [e1, e2])
    const r = simulateScenario(
      { dependencies: [e1, e2, e3req], groups: [g], nodeNames: NODE_NAMES },
      new Set([key('CMB4417'), key('CCB8821')]),
    )
    expect(statusOf(r, 'WeChat').status).toBe('must_change')
    const tv = statusOf(r, 'TencentVideo')
    expect(tv.status).toBe('must_change')
    expect(tv.depth).toBe(2)
  })
})
