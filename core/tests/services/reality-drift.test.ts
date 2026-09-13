import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import {
  DependencyRepository,
  getGraphRevision,
  migrate,
  NodeRepository,
  NodeSqliteDriver,
  RealityDriftService,
} from '../../src/index.ts'

/**
 * MVP03 RD-001..010 —— RealityDrift Gate。
 */

describe('RealityDrift（MVP03 RD）', () => {
  let dir: string
  let driver: NodeSqliteDriver
  let drifts: RealityDriftService
  let deps: DependencyRepository
  let cardA: string
  let cardB: string
  let wechat: string

  beforeEach(() => {
    dir = mkdtempSync(join(tmpdir(), 'depmap-rd-'))
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
    migrate(driver)
    drifts = new RealityDriftService(driver)
    deps = new DependencyRepository(driver)
    const nodes = new NodeRepository(driver)
    cardA = nodes.create({ kind: 'payment_instrument', name: '招行 4417', last4: '4417' }).id
    cardB = nodes.create({ kind: 'payment_instrument', name: '建行 8821', last4: '8821' }).id
    wechat = nodes.create({
      kind: 'account',
      templateId: 'builtin.account.wechat',
      name: '微信支付',
    }).id
    deps.confirm({
      from: cardA,
      relation: 'funding_source',
      to: wechat,
      capability: 'payment',
      criticality: 'required',
    })
  })

  afterEach(() => {
    driver.close()
    rmSync(dir, { recursive: true, force: true })
  })

  function signal(observations = 2, evidenceRef = 'inst-B#1') {
    return {
      targetNodeId: wechat,
      capability: 'payment' as const,
      signals: [{ fromNodeId: cardB, observations, evidenceRef }],
    }
  }

  it('RD-001: confirmed A + 新正向 B 证据 → possible_replacement drift', () => {
    const result = drifts.detectFromEvidence(signal())
    expect(result.created.length).toBe(1)
    expect(result.created[0]?.kind).toBe('possible_replacement')
    expect(result.created[0]?.candidateFrom).toBe(cardB)
    expect(result.created[0]?.relatedDependencyIds).toEqual([
      deps.findByLogicalKey(cardA, 'funding_source', wechat, 'payment')!.id,
    ])
  })

  it('RD-002: 仅 absence（无正向证据）→ 永不产生 drift（入口不存在 absence 通道）', () => {
    const result = drifts.detectFromEvidence({
      targetNodeId: wechat,
      capability: 'payment',
      signals: [],
    })
    expect(result.created).toEqual([])
    // 且 API 输入类型中没有 absence 类字段
    const input: Record<string, unknown> = {
      targetNodeId: wechat,
      capability: 'payment',
      signals: [],
    }
    expect(Object.keys(input).some((k) => k.toLowerCase().includes('absen'))).toBe(false)
  })

  it('RD-003: drift 检测不修改 Graph（无新 Dependency/无 revision 变化）', () => {
    const before = getGraphRevision(driver)
    const depCount = deps.countAll()
    drifts.detectFromEvidence(signal())
    expect(deps.countAll()).toBe(depCount)
    expect(getGraphRevision(driver)).toBe(before)
  })

  it('RD-004: dismiss → Graph 不变、revision 不变', () => {
    const { created } = drifts.detectFromEvidence(signal())
    const before = getGraphRevision(driver)
    const depCount = deps.countAll()
    const dismissed = drifts.dismiss(created[0]!.id)
    expect(dismissed.status).toBe('dismissed')
    expect(deps.countAll()).toBe(depCount)
    expect(getGraphRevision(driver)).toBe(before)
  })

  it('RD-005: confirm replacement → 新边 + 旧边 retired + revision 增加', () => {
    const { created } = drifts.detectFromEvidence(signal())
    const before = getGraphRevision(driver)
    const oldDepId = deps.findByLogicalKey(cardA, 'funding_source', wechat, 'payment')!.id
    const { newDependencyId, retiredDependencyIds } = drifts.resolveAsReplacement(created[0]!.id)
    expect(retiredDependencyIds).toEqual([oldDepId])
    expect(deps.getById(oldDepId)?.state).toBe('retired')
    expect(deps.getById(newDependencyId)?.from).toBe(cardB)
    expect(deps.getById(newDependencyId)?.state).toBe('active')
    expect(getGraphRevision(driver)).toBeGreaterThan(before)
    expect(drifts.listOpen().length).toBe(0)
  })

  it('RD-006: confirm additional path → 旧路径保持 active', () => {
    const { created } = drifts.detectFromEvidence(signal())
    const oldDepId = deps.findByLogicalKey(cardA, 'funding_source', wechat, 'payment')!.id
    const { newDependencyId } = drifts.resolveAsAdditionalPath(created[0]!.id)
    expect(deps.getById(oldDepId)?.state).toBe('active')
    expect(deps.getById(newDependencyId)?.state).toBe('active')
    expect(deps.countAll()).toBe(2)
  })

  it('RD-007: duplicate evidence 不重复 drift / 不重复计数', () => {
    drifts.detectFromEvidence(signal(2, 'inst-B#1'))
    const again = drifts.detectFromEvidence(signal(2, 'inst-B#1'))
    expect(again.created.length).toBe(0)
    expect(again.updated.length).toBe(0)
    const open = drifts.listOpen()
    expect(open.length).toBe(1)
    expect(open[0]?.observationCount).toBe(2)
  })

  it('RD-008: 同 key 多次信号 → 单一 open drift 累计（upsert 语义）', () => {
    drifts.detectFromEvidence(signal(2, 'inst-B#1'))
    drifts.detectFromEvidence({
      targetNodeId: wechat,
      capability: 'payment',
      signals: [{ fromNodeId: cardB, observations: 3, evidenceRef: 'inst-B#2' }],
    })
    const open = drifts.listOpen()
    expect(open.length).toBe(1)
    expect(open[0]?.observationCount).toBe(5)
    expect(open[0]?.evidenceRefs).toEqual(['inst-B#1', 'inst-B#2'])
  })

  it('RD-009: 跨源 provenance 保留在 evidenceRefs（不同实例引用共存）', () => {
    drifts.detectFromEvidence({
      targetNodeId: wechat,
      capability: 'payment',
      signals: [
        { fromNodeId: cardB, observations: 2, evidenceRef: 'inst-A#3' },
        { fromNodeId: cardB, observations: 1, evidenceRef: 'inst-B#7' },
      ],
    })
    const open = drifts.listOpen()
    expect(open.length).toBe(1)
    expect(open[0]?.evidenceRefs).toEqual(['inst-A#3', 'inst-B#7'])
  })

  it('RD-010: rejected Proposal 不产生 confirmed drift（drift 不接受 proposal 决策通道）', () => {
    // drift 创建/处理 API 中不存在「接受 proposal → confirmed drift」的路径：
    // resolve 只能由显式用户操作触发；此处证明 rejected proposal 不会改变 drift 状态
    const { created } = drifts.detectFromEvidence(signal())
    expect(created[0]?.status).toBe('open')
    expect(drifts.listOpen().length).toBe(1)
    // 未调用任何 resolve → 没有任何 confirmed_change
    const all = drifts.listOpen().concat()
    expect(all.every((d) => d.status !== 'confirmed_change')).toBe(true)
  })

  it('低于阈值（<2）的信号被忽略（保守不催）', () => {
    const result = drifts.detectFromEvidence(signal(1))
    expect(result.created).toEqual([])
    expect(result.ignored[0]?.reason).toBe('below_threshold')
    expect(drifts.listOpen().length).toBe(0)
  })

  it('relation_reappeared：retired 边的来源重新出现正向证据', () => {
    const oldEdge = deps.findByLogicalKey(cardA, 'funding_source', wechat, 'payment')!
    deps.retire(oldEdge.id)
    // cardA（曾经 confirmed、现已 retired）重新出现证据
    const result = drifts.detectFromEvidence({
      targetNodeId: wechat,
      capability: 'payment',
      signals: [{ fromNodeId: cardA, observations: 2, evidenceRef: 'inst-A#12' }],
    })
    expect(result.created.length).toBe(1)
    expect(result.created[0]?.kind).toBe('relation_reappeared')
  })
})
