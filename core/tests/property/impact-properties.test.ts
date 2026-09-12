import { describe, expect, it } from 'vitest'
import fc from 'fast-check'
import {
  simulateScenario,
  type Capability,
  type Criticality,
  type Dependency,
  type DependencyGroup,
  type DependencyState,
  type GroupMode,
  type ImpactGraph,
  type ImpactStateKey,
  type Relation,
} from '../../src/index.ts'

/**
 * Engineering Baseline V1 — Property-based Testing（fast-check）。
 *
 * 随机生成 0–30 节点 / 随机 Dependencies（required|unknown × active|retired）/
 * 随机 ANY|ALL Group / 随机 pending Proposals / 随机 unavailable 状态（含环），
 * 断言 Kernel 级 invariant：
 *   P1 Impact 永远终止，processedKeys 无重复
 *   P2 输出确定性（同输入两次运行逐字节一致）
 *   P3 retired 边/Group 不参与传播（剔除 retired 后输出完全等价）
 *   P4 Proposal 永不改变 must_change / backup_path 集合
 *   P5 must_change 必有已确认现实依据（required active 边或 confirmed ALL Group）
 *   P6 lostKeys ⊆ 初始 unavailable
 *
 * 复现：全局 seed 固定为 20260913（fast-check 失败时会给出可复现 seed/counterexample）。
 */
fc.configureGlobal({ seed: 20260913 })

const T0 = '2026-01-01T00:00:00Z'

function nodeIds(n: number): string[] {
  return Array.from({ length: n }, (_, i) => `n${i}`)
}

function mkDep(
  i: number,
  from: string,
  relation: Relation,
  to: string,
  criticality: Criticality,
  state: DependencyState,
): Dependency {
  return {
    id: `e${i}`,
    from,
    relation,
    to,
    capability: 'payment',
    criticality,
    groupId: null,
    state,
    origin: 'manual',
    confirmedAt: T0,
    lastVerifiedAt: T0,
    retiredAt: state === 'retired' ? T0 : null,
    evidenceRefs: [],
    verificationBasis: { type: 'user_confirmed', verifiedAt: T0 },
    createdAt: T0,
    updatedAt: T0,
  }
}

function mkGroup(i: number, target: string, mode: GroupMode, members: string[]): DependencyGroup {
  return {
    id: `g${i}`,
    groupKey: `gk${i}`,
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

const graphArb: fc.Arbitrary<ImpactGraph> = fc.nat({ max: 29 }).chain((n0) => {
  const n = n0 + 1
  const ids = nodeIds(n)
  const idArb = fc.constantFrom(...ids)
  const relArb = fc.constantFrom<Relation>('funding_source', 'merchant_agreement')
  const critArb = fc.constantFrom<Criticality>('required', 'unknown')
  const stateArb = fc.constantFrom<DependencyState>('active', 'retired')
  return fc
    .array(
      fc.record({
        from: idArb,
        relation: relArb,
        to: idArb,
        criticality: critArb,
        state: stateArb,
      }),
      {
        maxLength: n * 2,
      },
    )
    .chain((depSpecs) => {
      const deps = depSpecs.map((s, i) =>
        mkDep(i, s.from, s.relation, s.to, s.criticality, s.state),
      )
      const activeIds = deps.filter((d) => d.state === 'active').map((d) => d.id)
      const targetArb =
        deps.length > 0 ? fc.constantFrom(...deps.map((d) => d.to)) : fc.constant(ids[0] ?? 'n0')
      const memberArb =
        activeIds.length > 0
          ? fc.array(fc.constantFrom(...activeIds), {
              minLength: 1,
              maxLength: Math.max(activeIds.length, 1),
            })
          : fc.constant([])
      const modeArb = fc.constantFrom<GroupMode>('ANY', 'ALL')
      return fc
        .record({
          deps: fc.constant(deps),
          groups: fc
            .array(fc.record({ target: targetArb, mode: modeArb, members: memberArb }), {
              maxLength: 3,
            })
            .map((specs) =>
              specs.map((g, i) => mkGroup(i, g.target, g.mode, [...new Set(g.members)])),
            ),
          proposals: fc
            .array(fc.record({ from: idArb, to: idArb }), { maxLength: 3 })
            .map(
              (specs): Array<{ key: string; from: string; to: string; capability: Capability }> =>
                specs.map((p, i) => ({
                  key: `p${i}`,
                  from: p.from,
                  to: p.to,
                  capability: 'payment',
                })),
            ),
        })
        .map(({ deps, groups, proposals }) => ({
          dependencies: deps,
          groups,
          proposals,
        }))
    })
})

const unavailableArb: fc.Arbitrary<Set<ImpactStateKey>> = fc.nat({ max: 29 }).chain((n0) => {
  const n = n0 + 1
  const capArb = fc.constantFrom('payment', 'payment', 'payment', 'access')
  return fc
    .array(fc.record({ nodeId: fc.constantFrom(...nodeIds(n)), capability: capArb }), {
      maxLength: n,
    })
    .map((keys) => new Set(keys.map((k) => ({ nodeId: k.nodeId, capability: k.capability }))))
})

const criticalSeverity = (r: ReturnType<typeof simulateScenario>) =>
  r.targets
    .filter((t) => t.status === 'must_change' || t.status === 'backup_path')
    .map((t) => `${t.nodeId}|${t.capability}|${t.status}|${t.reasonCode}`)
    .sort()

describe('Impact Kernel property-based invariants（fast-check，Engineering Baseline V1）', () => {
  it('P1: 随机图/随机 unavailable —— Impact 终止且 processedKeys 无重复', () => {
    fc.assert(
      fc.property(graphArb, unavailableArb, (g, unavailable) => {
        const r = simulateScenario(g, unavailable)
        expect(new Set(r.processedKeys).size).toBe(r.processedKeys.length)
      }),
      { numRuns: 200 },
    )
  })

  it('P2: 输出确定性 —— 同输入两次运行结果逐字节一致', () => {
    fc.assert(
      fc.property(graphArb, unavailableArb, (g, unavailable) => {
        const r1 = simulateScenario(g, unavailable)
        const r2 = simulateScenario(g, unavailable)
        expect(JSON.stringify(r2)).toBe(JSON.stringify(r1))
      }),
      { numRuns: 150 },
    )
  })

  it('P3: retired 边/Group 不参与传播 —— 剔除 retired 后输出完全等价', () => {
    fc.assert(
      fc.property(graphArb, unavailableArb, (g, unavailable) => {
        const stripped: ImpactGraph = {
          ...g,
          dependencies: g.dependencies.filter((d) => d.state === 'active'),
          groups: g.groups.filter((x) => x.state === 'active'),
        }
        const full = simulateScenario(g, unavailable)
        const withoutRetired = simulateScenario(stripped, unavailable)
        expect(JSON.stringify(withoutRetired)).toBe(JSON.stringify(full))
      }),
      { numRuns: 150 },
    )
  })

  it('P4: Proposal 永不改变 must_change / backup_path 集合', () => {
    fc.assert(
      fc.property(graphArb, unavailableArb, (g, unavailable) => {
        const withProposals = simulateScenario(g, unavailable)
        const withoutProposals = simulateScenario({ ...g, proposals: [] }, unavailable)
        expect(criticalSeverity(withoutProposals)).toEqual(criticalSeverity(withProposals))
      }),
      { numRuns: 150 },
    )
  })

  it('P5: must_change 必有已确认现实依据（required active 边或 confirmed ALL Group）', () => {
    fc.assert(
      fc.property(graphArb, unavailableArb, (g, unavailable) => {
        const r = simulateScenario(g, unavailable)
        // kernel 只按 `${nodeId}|payment` 判定失效（access 等初始键被忽略）
        const lostStr = new Set(
          [...unavailable]
            .filter((k) => k.capability === 'payment')
            .map((k) => `${k.nodeId}|payment`),
        )
        for (const t of r.targets) {
          if (t.status !== 'must_change') continue
          const lost = (node: string) => lostStr.has(`${node}|payment`)
          if (t.reasonCode === 'required_edge_no_alternative') {
            const evidence = g.dependencies.some(
              (d) =>
                d.to === t.nodeId &&
                d.state === 'active' &&
                d.capability === 'payment' &&
                d.criticality === 'required' &&
                lost(d.from),
            )
            expect(evidence).toBe(true)
          } else if (t.reasonCode === 'confirmed_group_failed') {
            const evidence = g.groups.some((grp) => {
              if (grp.state !== 'active' || grp.targetNodeId !== t.nodeId) {
                return false
              }
              return grp.memberEdgeIds.some((eid) => {
                const m = g.dependencies.find((d) => d.id === eid)
                return m !== undefined && m.state === 'active' && lost(m.from)
              })
            })
            expect(evidence).toBe(true)
          }
        }
      }),
      { numRuns: 150 },
    )
  })

  it('P6: lostKeys 值级唯一、全为 payment；初始键必在，传播键必有 must_change 依据', () => {
    fc.assert(
      fc.property(graphArb, unavailableArb, (g, unavailable) => {
        const r = simulateScenario(g, unavailable)
        // 全为 payment 且值级唯一
        for (const k of r.lostKeys) {
          expect(k.capability).toBe('payment')
        }
        const lostStr = r.lostKeys.map((k) => `${k.nodeId}|payment`)
        expect(new Set(lostStr).size).toBe(lostStr.length)
        // 初始 payment 键 ⊆ lostKeys
        const initialStr = [...unavailable]
          .filter((k) => k.capability === 'payment')
          .map((k) => `${k.nodeId}|payment`)
        for (const s of initialStr) {
          expect(lostStr).toContain(s)
        }
        // 传播产生的 lostKey（非初始）必有 must_change target 依据
        const initialSet = new Set(initialStr)
        for (const s of lostStr) {
          if (initialSet.has(s)) continue
          const nodeId = s.slice(0, -'|payment'.length)
          expect(r.targets.some((t) => t.nodeId === nodeId && t.status === 'must_change')).toBe(
            true,
          )
        }
      }),
      { numRuns: 150 },
    )
  })
})
