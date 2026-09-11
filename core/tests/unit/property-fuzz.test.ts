import { describe, expect, it, beforeEach, afterEach } from 'vitest'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { NodeSqliteDriver } from '../../src/db/node-driver.ts'
import { migrate } from '../../src/schema/migrations.ts'
import type { Dependency, DependencyGroup } from '../../src/domain/types.ts'
import { canonicalGroupKey } from '../../src/domain/types.ts'
import { simulateScenario } from '../../src/impact/kernel.ts'
import { parseWechatBill } from '../../src/parser/wechat/parser.ts'
import { assignFingerprints } from '../../src/fingerprint/fingerprint.ts'
import { createDepmapContainer, openDepmapContainer, DepmapError } from '../../src/crypto/depmap.ts'
import { parseWechatBill as parseBill } from '../../src/parser/wechat/parser.ts'

/**
 * PHASE K — Property/Fuzz-style Smoke（deterministic pseudo-random，不引入重量框架）。
 * 性质：图必须终止 / retired 不传播 / Proposal 永不 must_change / 输出 deterministic。
 */

const enc = new TextEncoder()

/** mulberry32 —— 可复现伪随机。 */
function mulberry32(seed: number): () => number {
  let a = seed
  return () => {
    a |= 0
    a = (a + 0x6d2b79f5) | 0
    let t = Math.imul(a ^ (a >>> 15), 1 | a)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

interface FuzzNode {
  id: string
  retired: boolean
  required: boolean
}

function buildRandomGraph(
  rand: () => number,
  nodeCount: number,
): {
  dependencies: Dependency[]
  groups: DependencyGroup[]
  proposals: Array<{ key: string; from: string; to: string; capability: 'payment' }>
} {
  const nodes: FuzzNode[] = [...Array(nodeCount).keys()].map((i) => ({
    id: `n${i}`,
    retired: rand() < 0.2,
    required: rand() < 0.4,
  }))
  const dependencies: Dependency[] = []
  const edgeCount = Math.floor(nodeCount * 1.5)
  for (let e = 0; e < edgeCount; e++) {
    const from = nodes[Math.floor(rand() * nodeCount)]!
    const to = nodes[Math.floor(rand() * nodeCount)]!
    if (from.id === to.id) continue
    const state = from.retired || to.retired ? ('retired' as const) : ('active' as const)
    dependencies.push({
      id: `e${e}`,
      from: from.id,
      relation: 'funding_source',
      to: to.id,
      capability: 'payment',
      criticality: from.required ? 'required' : 'unknown',
      groupId: null,
      state,
      origin: 'manual',
      confirmedAt: 't',
      lastVerifiedAt: 't',
      retiredAt: state === 'retired' ? 't' : null,
      evidenceRefs: [],
      createdAt: 't',
      updatedAt: 't',
    })
  }
  // 随机确认一个 ANY group
  const groups: DependencyGroup[] = []
  if (nodeCount >= 3 && rand() < 0.5) {
    const target = nodes[Math.floor(rand() * nodeCount)]!
    const members = dependencies
      .filter((d) => d.to === target.id && d.state === 'active')
      .slice(0, 2)
    if (members.length === 2) {
      const keys = members.map((m) => `${m.from}|${m.relation}|${m.to}|${m.capability}`)
      groups.push({
        id: `g-${target.id}`,
        groupKey: canonicalGroupKey(target.id, 'payment', 'ANY', keys),
        targetNodeId: target.id,
        capability: 'payment',
        mode: 'ANY',
        memberEdgeIds: members.map((m) => m.id),
        state: 'active',
        confirmedAt: 't',
        lastVerifiedAt: 't',
        createdAt: 't',
        updatedAt: 't',
      })
    }
  }
  const proposals = [...Array(Math.floor(nodeCount / 2)).keys()].map((i) => ({
    key: `p${i}`,
    from: `n${Math.floor(rand() * nodeCount)}`,
    to: `n${Math.floor(rand() * nodeCount)}`,
    capability: 'payment' as const,
  }))
  return { dependencies, groups, proposals }
}

describe('Property/Fuzz Smoke (RC PHASE K)', () => {
  const scenarios = [
    { seed: 1, nodes: 20 },
    { seed: 7, nodes: 50 },
    { seed: 1337, nodes: 100 },
  ]

  for (const { seed, nodes } of scenarios) {
    it(`随机图 seed=${seed} nodes=${nodes}：终止/retired 不传播/Proposal 永不 must_change/确定性`, () => {
      const rand = mulberry32(seed)
      const graph = buildRandomGraph(rand, nodes)
      const unavailable = new Set([{ nodeId: 'n0', capability: 'payment' as const }])

      const r1 = simulateScenario(graph, unavailable)
      const r2 = simulateScenario(graph, unavailable)

      // deterministic
      expect(JSON.stringify(r1)).toBe(JSON.stringify(r2))
      // 终止：lost + targets 有界
      expect(r1.lostKeys.length).toBeLessThanOrEqual(nodes)
      expect(new Set(r1.lostKeys.map((k) => `${k.nodeId}|${k.capability}`)).size).toBe(
        r1.lostKeys.length,
      )
      // retired 不传播：lost 集中的节点不存在“只以 retired 出边连接”的传播路径
      // （直接验证：所有 lost 目标必有 active 入边来自 unavailable/lost 节点）
      const lostIds = new Set(r1.lostKeys.map((k) => k.nodeId))
      for (const t of r1.targets) {
        if (t.status === 'must_change') {
          const activeIncoming = graph.dependencies.filter(
            (d) => d.to === t.nodeId && d.state === 'active' && lostIds.has(d.from),
          )
          expect(activeIncoming.length).toBeGreaterThan(0)
        }
      }
      // Proposal 永不 must_change：把 proposal-only 目标独立验证
      for (const t of r1.targets) {
        if (t.reasonCode === 'proposal_only') {
          expect(t.status).toBe('needs_review')
          expect(t.status).not.toBe('must_change')
        }
      }
    })
  }

  it('CSV 行 mutation fuzz：100 次变异坏行计数，好行不污染', () => {
    const HEADER =
      '交易时间,交易类型,交易对方,商品,收/支,金额(元),支付方式,当前状态,交易单号,商户单号,备注'
    const goodRows = [...Array(10).keys()].map(
      (i) => `2026-01-03 10:23:00,商户消费,商户${i},商品,支出,¥1.00,零钱,支付成功,T${i}`,
    )
    for (let round = 0; round < 100; round++) {
      const rand = mulberry32(round + 500)
      const lines = [HEADER, ...goodRows]
      // 变异 2 行：破坏列数或金额
      const mutateIdx1 = 1 + Math.floor(rand() * 10)
      lines[mutateIdx1] = lines[mutateIdx1]!.slice(0, 10 + Math.floor(rand() * 20)) // 截断
      const mutateIdx2 = 1 + Math.floor(rand() * 10)
      if (mutateIdx2 !== mutateIdx1) lines[mutateIdx2] = `垃圾行${round}`
      const r = parseWechatBill(enc.encode(lines.join('\r\n')))
      // 好行数 ≥ 8（两条被变异），且所有好行 merchantRaw 未被污染
      expect(r.observations.length).toBeGreaterThanOrEqual(8)
      for (const obs of r.observations) {
        expect(obs.merchantRaw).not.toContain('垃圾行')
      }
      expect(r.observations.length + r.errors.length).toBe(10)
    }
  })

  it('fingerprint fuzz：随机观测集合 → 指纹唯一性（同会话内 ordinal 不碰撞）', () => {
    for (let round = 0; round < 20; round++) {
      const rand = mulberry32(round + 9000)
      const obs = [...Array(30).keys()].map((_i) => ({
        source: 'wechat',
        sourceTxnId: null,
        merchantTxnId: null,
        occurredAt: `2026-0${1 + Math.floor(rand() * 9)}-15 08:30:00+08:00`,
        merchantRaw: `商户${Math.floor(rand() * 3)}`,
        description: '商品',
        amount: 25,
        currency: 'CNY',
        direction: 'out' as const,
        paymentMethodRaw: '零钱',
        status: '支付成功',
        note: '',
      }))
      const fps = assignFingerprints('fuzz-secret', obs)
      // 同一 merchant/time 的重复 canonical 行获得 ordinal → 30 条观测 30 个指纹
      const unique = new Set(fps.map((f) => f.fingerprint))
      expect(unique.size).toBe(30)
    }
  })

  it('container golden mutation fuzz：随机 kdf 参数替换 → bounds/auth 拒绝（不 crash）', async () => {
    // 最小合法 KDF 参数：单次 Argon2 开销可控（20 轮 × 参数越界/错 key）
    const { json } = await createDepmapContainer(enc.encode('fuzz-payload'), 'fuzz-pwd', {
      memoryKiB: 16384,
      iterations: 1,
    })
    for (let round = 0; round < 20; round++) {
      const rand = mulberry32(round + 31337)
      const header = JSON.parse(json) as Record<string, unknown>
      const kdf = header['kdf'] as Record<string, unknown>
      // 随机替换一个 kdf 参数为不同值（越界 → bounds；在界 → 错 key → auth_failed）
      const pick = Math.floor(rand() * 3) as 0 | 1 | 2
      const fieldNames: ReadonlyArray<'memoryKiB' | 'iterations' | 'parallelism'> = [
        'memoryKiB',
        'iterations',
        'parallelism',
      ]
      const ranges: Array<[number, number, number]> = [
        [1, 300000, 65536],
        [0, 50, 1],
        [0, 20, 1],
      ]
      const range = ranges[pick]!
      const fieldName = fieldNames[pick]
      if (fieldName === undefined) throw new Error('bad fuzz pick')
      let v = range[0]
      do {
        v = range[0] + Math.floor(rand() * (range[1] - range[0]))
      } while (v === range[2])
      kdf[fieldName] = v
      try {
        await openDepmapContainer(JSON.stringify(header), 'fuzz-pwd')
        expect.unreachable('mutated kdf must fail')
      } catch (e) {
        expect(e).toBeInstanceOf(DepmapError)
        expect(['bounds', 'auth_failed', 'kdf']).toContain((e as DepmapError).code)
      }
    }
  }, 30000)

  it('同输入 10 轮 parser+fingerprint 输出一致', () => {
    const HEADER =
      '交易时间,交易类型,交易对方,商品,收/支,金额(元),支付方式,当前状态,交易单号,商户单号,备注'
    const raw = enc.encode(
      [
        HEADER,
        '2026-01-03 10:23:00,商户消费,腾讯视频,VIP,支出,¥25.00,招商银行信用卡(4417),支付成功,T1',
        '2026-02-15 08:30:00,商户消费,腾讯视频,VIP,支出,¥25.00,招商银行信用卡(4417),支付成功,T2',
      ].join('\r\n'),
    )
    const expected = JSON.stringify(assignFingerprints('s', parseBill(raw).observations))
    for (let i = 0; i < 10; i++) {
      expect(JSON.stringify(assignFingerprints('s', parseBill(raw).observations))).toBe(expected)
    }
  })

  describe('DB isolation', () => {
    let dir: string
    let driver: NodeSqliteDriver
    beforeEach(() => {
      dir = mkdtempSync(join(tmpdir(), 'depmap-fuzz-'))
      driver = new NodeSqliteDriver(join(dir, 't.db'))
      driver.open()
      migrate(driver)
    })
    afterEach(() => {
      driver.close()
      rmSync(dir, { recursive: true, force: true })
    })

    it('随机图数据落库→模拟→导出→导入 等价', () => {
      const rand = mulberry32(777)
      const graph = buildRandomGraph(rand, 30)
      const nodes = driver.prepare(`SELECT COUNT(*) AS c FROM nodes`).get()
      void nodes
      // 通过 kernel 直接验证随机图模拟的确定性（无 DB 依赖），已由上方覆盖；
      // 此处补充：图导出/导入等价在随机数据下成立
      const deps = graph.dependencies
      const insertNode = driver.prepare(
        `INSERT INTO nodes (id, kind, name, owner, archived, fields_json, created_at, updated_at) VALUES (?, 'account', ?, 'self', 0, '{}', 't', 't')`,
      )
      const ids = new Set<string>()
      for (const d of deps) {
        for (const id of [d.from, d.to]) {
          if (!ids.has(id)) {
            ids.add(id)
            insertNode.run(id, `节点${id}`)
          }
        }
      }
      const insertDep = driver.prepare(
        `INSERT OR IGNORE INTO dependencies (id, from_node, relation, to_node, capability, criticality, state, origin, confirmed_at, last_verified_at, retired_at, evidence_refs_json, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '[]', ?, ?)`,
      )
      for (const d of deps) {
        insertDep.run(
          d.id,
          d.from,
          d.relation,
          d.to,
          d.capability,
          d.criticality,
          d.state,
          d.origin,
          't',
          't',
          d.retiredAt,
          't',
          't',
        )
      }
      // 延迟 import 避免顶层循环依赖
      const { exportGraph, importGraph } = requireGraphSerialize()
      const e1 = exportGraph(driver)
      const driver2 = new NodeSqliteDriver(join(dir, 'r.db'))
      driver2.open()
      migrate(driver2)
      try {
        importGraph(driver2, e1.payloadJson)
        expect(exportGraph(driver2).payloadJson).toBe(e1.payloadJson)
      } finally {
        driver2.close()
      }
    })
  })
})

function requireGraphSerialize(): typeof import('../../src/services/graph-serialize.ts') {
  // 静态 import 即可（此处包装仅为测试内延迟引用语义清晰）
  return graphSerializeModule
}

import * as graphSerializeModule from '../../src/services/graph-serialize.ts'
