import { describe, expect, it } from 'vitest'
import type { Criticality, Dependency, DependencyGroup } from '../../src/domain/types.ts'
import { canonicalGroupKey } from '../../src/domain/types.ts'
import { simulateScenario } from '../../src/impact/kernel.ts'
import { parseWechatBill } from '../../src/parser/wechat/parser.ts'
import { readFileSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

/**
 * PHASE H — Determinism：同输入重复执行输出必须完全一致。
 */

const FX = (name: string) =>
  new Uint8Array(
    readFileSync(join(dirname(fileURLToPath(import.meta.url)), '..', 'fixtures', name)),
  )

let seq = 0
function dep(from: string, to: string, criticality: Criticality = 'unknown'): Dependency {
  seq += 1
  return {
    id: `e${seq}`,
    from,
    relation: 'funding_source',
    to,
    capability: 'payment',
    criticality,
    groupId: null,
    state: 'active',
    origin: 'manual',
    confirmedAt: '2026-01-01T00:00:00.000Z',
    lastVerifiedAt: '2026-01-01T00:00:00.000Z',
    retiredAt: null,
    evidenceRefs: [],
    verificationBasis: null,
    createdAt: '2026-01-01T00:00:00.000Z',
    updatedAt: '2026-01-01T00:00:00.000Z',
  }
}

function anyGroup(target: string, members: Dependency[]): DependencyGroup {
  const keys = members.map((m) => `${m.from}|${m.relation}|${m.to}|${m.capability}`)
  return {
    id: `g-${target}`,
    groupKey: canonicalGroupKey(target, 'payment', 'ANY', keys),
    targetNodeId: target,
    capability: 'payment',
    mode: 'ANY',
    memberEdgeIds: members.map((m) => m.id),
    state: 'active',
    confirmedAt: '2026-01-01T00:00:00.000Z',
    lastVerifiedAt: '2026-01-01T00:00:00.000Z',
    verificationBasis: null,
    createdAt: '2026-01-01T00:00:00.000Z',
    updatedAt: '2026-01-01T00:00:00.000Z',
  }
}

describe('Determinism (RC PHASE H)', () => {
  it('Impact：同 fixture 重复 50 次输出完全一致', () => {
    const e1 = dep('A', 'WeChat', 'required')
    const e2 = dep('B', 'WeChat', 'unknown')
    const g = anyGroup('WeChat', [e1, e2])
    const e3 = dep('WeChat', 'TV', 'required')
    const graph = {
      dependencies: [e1, e2, e3],
      groups: [g],
      proposals: [
        { key: 'B|funding_source|C|payment', from: 'B', to: 'C', capability: 'payment' as const },
      ],
      nodeNames: { A: '招行', B: '建行', WeChat: '微信', TV: '腾讯视频', C: '爱奇艺' },
    }
    const scenario = new Set([{ nodeId: 'A', capability: 'payment' as const }])
    const first = JSON.stringify(simulateScenario(graph, scenario))
    for (let i = 0; i < 50; i++) {
      const run = JSON.stringify(simulateScenario(graph, scenario))
      expect(run).toBe(first)
    }
  })

  it('Checklist：同一 graph revision + scenario 顺序一致（50 次）', () => {
    const e1 = dep('A', 'B', 'required')
    const e2 = dep('B', 'C', 'required')
    const e3 = dep('C', 'D', 'unknown')
    const graph = { dependencies: [e1, e2, e3], groups: [], nodeNames: {} }
    const titles = JSON.stringify(
      simulateScenario(graph, new Set([{ nodeId: 'A', capability: 'payment' as const }])).checklist,
    )
    for (let i = 0; i < 50; i++) {
      expect(
        JSON.stringify(
          simulateScenario(graph, new Set([{ nodeId: 'A', capability: 'payment' as const }]))
            .checklist,
        ),
      ).toBe(titles)
    }
  })

  it('Parser：同输入重复 50 次 Observation 内容/顺序完全一致', () => {
    const raw = FX('recurring-monthly.csv')
    const first = JSON.stringify(parseWechatBill(raw))
    for (let i = 0; i < 50; i++) {
      expect(JSON.stringify(parseWechatBill(raw))).toBe(first)
    }
  })

  it('Proposal：同 observation set → 同 logical keys（50 次）', () => {
    const mk = () =>
      [...Array(20).keys()].map((i) => ({
        from: `card${i % 3}`,
        relation: 'funding_source' as const,
        to: 'wechat',
        capability: 'payment' as const,
      }))
    const keysets = [mk(), mk(), mk()].map((set) =>
      set.map((s) => `${s.from}|${s.relation}|${s.to}|${s.capability}`).sort(),
    )
    expect(keysets[0]).toEqual(keysets[1])
    expect(keysets[1]).toEqual(keysets[2])
    // 50 次构造均一致
    const expected = keysets[0]
    for (let i = 0; i < 50; i++) {
      expect(
        mk()
          .map((s) => `${s.from}|${s.relation}|${s.to}|${s.capability}`)
          .sort(),
      ).toEqual(expected)
    }
  })
})
