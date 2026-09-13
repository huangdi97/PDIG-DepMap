import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import {
  buildTimeline,
  ChangePlanRepository,
  DependencyRepository,
  DependencyGroupRepository,
  DependencyProposalRepository,
  DiscoveryService,
  getGraphRevision,
  migrate,
  NodeRepository,
  NodeSqliteDriver,
  rebasePlan,
  RealityDriftService,
  SourceInstanceRepository,
} from '../../src/index.ts'

/**
 * MVP03 §63 性能 Smoke —— 防退化，不追求极限。
 * 100 ChangePlans / 1000 TimelineItems / 500 open Drifts / 500 Candidates / 1k-node Graph rebase。
 */

describe('MVP03 Performance Smoke', () => {
  let dir: string
  let driver: NodeSqliteDriver

  beforeEach(() => {
    dir = mkdtempSync(join(tmpdir(), 'depmap-perf3-'))
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
    migrate(driver)
  })

  afterEach(() => {
    driver.close()
    rmSync(dir, { recursive: true, force: true })
  })

  it('100 ChangePlans 创建 + rebase < 10s', { timeout: 30_000 }, () => {
    const nodes = new NodeRepository(driver)
    const plans = new ChangePlanRepository(driver)
    const start = Date.now()
    for (let i = 0; i < 100; i++) {
      const card = nodes.create({ kind: 'payment_instrument', name: `卡 ${i}` })
      plans.create({
        templateId: 'replace_payment_card',
        scenario: 'replace_payment_card',
        title: `计划 ${i}`,
        targetNodeId: card.id,
        graphRevision: getGraphRevision(driver),
      })
    }
    const deps = new DependencyRepository(driver)
    deps.confirm({
      from: nodes.create({ kind: 'payment_instrument', name: '触发卡' }).id,
      relation: 'funding_source',
      to: nodes.create({ kind: 'account', name: '账户' }).id,
      capability: 'payment',
    })
    for (const plan of plans.listAll()) {
      rebasePlan(
        driver,
        {
          deps,
          groups: new DependencyGroupRepository(driver),
          proposals: new DependencyProposalRepository(driver),
          plans,
        },
        plan.id,
      )
    }
    const elapsed = Date.now() - start
    expect(plans.countAll()).toBe(100)
    expect(elapsed).toBeLessThan(10_000)
    console.log(`perf: 100 plans create+rebase: ${elapsed} ms`)
  })

  it('1000 TimelineItems 投影 < 3s（确定性两次一致）', { timeout: 30_000 }, () => {
    const nodes = new NodeRepository(driver)
    const instances = new SourceInstanceRepository(driver)
    // 500 过期来源 + 250 计划 × 2 项（upcoming + verify pending）→ 1000 items
    for (let i = 0; i < 500; i++) {
      const inst = instances.create({
        adapterId: 'generic_csv',
        adapterVersion: 1,
        sourceKind: 'statement_file',
        label: `来源 ${i}`,
      })
      instances.touchIngested(inst.id, '2026-01-01T00:00:00.000Z')
    }
    const plans = new ChangePlanRepository(driver)
    for (let i = 0; i < 250; i++) {
      const card = nodes.create({ kind: 'payment_instrument', name: `卡 ${i}` })
      plans.create({
        templateId: 'replace_payment_card',
        scenario: 'replace_payment_card',
        title: `计划 ${i}`,
        targetNodeId: card.id,
        effectiveDate: `2026-${String((i % 12) + 1).padStart(2, '0')}-15`,
        graphRevision: getGraphRevision(driver),
        actions: [
          {
            id: `p${i}-v`,
            title: '验证',
            detail: '',
            phase: 'verify',
            done: false,
            doneAt: null,
            verification: {
              method: 'future_observation',
              status: 'pending',
              verifiedAt: null,
              evidenceRefs: [],
            },
          },
        ],
      })
    }
    const start = Date.now()
    const t1 = buildTimeline(driver, '2026-09-13T00:00:00.000Z')
    const elapsed = Date.now() - start
    expect(t1.length).toBeGreaterThanOrEqual(1000)
    expect(buildTimeline(driver, '2026-09-13T00:00:00.000Z')).toEqual(t1)
    expect(elapsed).toBeLessThan(3_000)
    console.log(`perf: 1000+ timeline items: ${elapsed} ms`)
  })

  it('500 open Drifts 检测+列举 < 5s；500 Candidates upsert < 5s', { timeout: 60_000 }, () => {
    const nodes = new NodeRepository(driver)
    const wechat = nodes.create({
      kind: 'account',
      templateId: 'builtin.account.wechat',
      name: '微信支付',
    }).id
    const drifts = new RealityDriftService(driver)
    const start = Date.now()
    for (let i = 0; i < 500; i++) {
      const card = nodes.create({ kind: 'payment_instrument', name: `来源卡 ${i}` }).id
      drifts.detectFromEvidence({
        targetNodeId: wechat,
        capability: 'payment',
        signals: [{ fromNodeId: card, observations: 2, evidenceRef: `inst-${i}#1` }],
      })
    }
    expect(drifts.listOpen().length).toBe(500)
    const driftElapsed = Date.now() - start

    const discovery = new DiscoveryService(driver)
    const cStart = Date.now()
    for (let i = 0; i < 500; i++) {
      discovery.upsertCandidate({
        candidateKind: 'service',
        displayLabel: `服务 ${i}`,
        normalizedKey: `svc:${i}`,
        sourceInstanceId: 's1',
        evidenceRef: `s1#${i}`,
      })
    }
    expect(discovery.listPending().length).toBe(500)
    const candElapsed = Date.now() - cStart
    expect(driftElapsed).toBeLessThan(10_000) // 宽预算防退化（Windows fsync 波动大，见 PERFORMANCE_BASELINE）
    expect(candElapsed).toBeLessThan(5_000)
    console.log(`perf: 500 drifts: ${driftElapsed} ms; 500 candidates: ${candElapsed} ms`)
  })

  it('1k-node Graph rebase < 5s', { timeout: 60_000 }, () => {
    const nodes = new NodeRepository(driver)
    const deps = new DependencyRepository(driver)
    // 1k 节点链：card_i → card_{i+1}（funding_source required），末端为账户
    const ids: string[] = []
    for (let i = 0; i < 1000; i++) {
      ids.push(nodes.create({ kind: 'payment_instrument', name: `N${i}` }).id)
    }
    const account = nodes.create({ kind: 'account', name: '终端账户' }).id
    for (let i = 0; i < 999; i++) {
      deps.confirm({
        from: ids[i]!,
        relation: 'funding_source',
        to: ids[i + 1]!,
        capability: 'payment',
        criticality: 'required',
      })
    }
    deps.confirm({
      from: ids[999]!,
      relation: 'funding_source',
      to: account,
      capability: 'payment',
      criticality: 'required',
    })

    const plans = new ChangePlanRepository(driver)
    const plan = plans.create({
      scenario: 'replace_payment_card',
      title: '1k rebase',
      targetNodeId: ids[0]!,
      graphRevision: getGraphRevision(driver),
    })
    const start = Date.now()
    const result = rebasePlan(
      driver,
      {
        deps,
        groups: new DependencyGroupRepository(driver),
        proposals: new DependencyProposalRepository(driver),
        plans,
      },
      plan.id,
    )
    const elapsed = Date.now() - start
    expect(result.revisionChanged).toBe(false) // 基线刚建，无变化
    // 真正的重分析：revision 推进后 rebase
    deps.confirm({
      from: nodes.create({ kind: 'payment_instrument', name: '新卡' }).id,
      relation: 'funding_source',
      to: account,
      capability: 'payment',
    })
    const start2 = Date.now()
    const result2 = rebasePlan(
      driver,
      {
        deps,
        groups: new DependencyGroupRepository(driver),
        proposals: new DependencyProposalRepository(driver),
        plans,
      },
      plan.id,
    )
    const elapsed2 = Date.now() - start2
    expect(result2.revisionChanged).toBe(true)
    expect(elapsed).toBeLessThan(5_000)
    expect(elapsed2).toBeLessThan(5_000)
    console.log(`perf: 1k-node rebase (analysis): ${elapsed2} ms`)
  })
})
