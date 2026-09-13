import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import {
  ChangePlanService,
  DependencyGroupRepository,
  DependencyProposalRepository,
  DependencyRepository,
  DiscoveryService,
  getGraphRevision,
  instantiateScenario,
  migrate,
  NodeRepository,
  NodeSqliteDriver,
  RealityDriftService,
  rebasePlan,
  type PlanAction,
} from '../../src/index.ts'

/**
 * MVP03 FREEZE §54–§55 —— 四套状态机的非法 transition 必须 Domain/Repository 层 reject
 * （不能只靠 UI），负向测试固化。
 */

describe('State machine illegal transitions（Freeze §55）', () => {
  let dir: string
  let driver: NodeSqliteDriver
  let nodes: NodeRepository
  let deps: DependencyRepository

  beforeEach(() => {
    dir = mkdtempSync(join(tmpdir(), 'depmap-frsm-'))
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
    migrate(driver)
    nodes = new NodeRepository(driver)
    deps = new DependencyRepository(driver)
  })

  afterEach(() => {
    driver.close()
    rmSync(dir, { recursive: true, force: true })
  })

  it('ChangePlan：completed/cancelled 终态不可迁出；非法跳转 reject', () => {
    const card = nodes.create({ kind: 'payment_instrument', name: '卡' })
    const plan = instantiateScenario(driver, 'replace_payment_card', {
      targetPaymentInstrumentId: card.id,
    })
    const service = new ChangePlanService(driver)
    service.transition(plan.id, 'in_progress')
    service.transition(plan.id, 'completed')
    for (const next of ['draft', 'analyzed', 'ready', 'in_progress', 'verifying'] as const) {
      expect(() => service.transition(plan.id, next)).toThrowError(/illegal change plan transition/)
    }
    // 未定义的跳转同样 reject（draft → verifying 不在合法表）
    const plan2 = instantiateScenario(driver, 'close_payment_instrument', {
      targetPaymentInstrumentId: card.id,
    })
    expect(() => service.transition(plan2.id, 'verifying')).toThrowError(
      /illegal change plan transition/,
    )
    // completed/cancelled 计划的动作冻结
    expect(() => service.completeAction(plan.id, 'prepare-1')).toThrowError(/actions are frozen/)
  })

  it('RealityDrift：非 open 状态不可 resolve/dismiss（二次处理 reject）', () => {
    const cardA = nodes.create({ kind: 'payment_instrument', name: 'A' }).id
    const cardB = nodes.create({ kind: 'payment_instrument', name: 'B' }).id
    const wechat = nodes.create({
      kind: 'account',
      templateId: 'builtin.account.wechat',
      name: 'W',
    }).id
    deps.confirm({
      from: cardA,
      relation: 'funding_source',
      to: wechat,
      capability: 'payment',
      criticality: 'required',
    })
    const drifts = new RealityDriftService(driver)
    const { created } = drifts.detectFromEvidence({
      targetNodeId: wechat,
      capability: 'payment',
      signals: [{ fromNodeId: cardB, observations: 2, evidenceRef: 'i#1' }],
    })
    const id = created[0]!.id
    drifts.dismiss(id)
    expect(() => drifts.dismiss(id)).toThrowError(/is dismissed/)
    expect(() => drifts.resolveAsReplacement(id)).toThrowError(/is dismissed/)
    expect(() => drifts.resolveAsAdditionalPath(id)).toThrowError(/is dismissed/)
  })

  it('DiscoveryCandidate：非 pending 不可 accept（dismissed/accepted reject）', () => {
    const discovery = new DiscoveryService(driver)
    const { candidate } = discovery.upsertCandidate({
      candidateKind: 'service',
      displayLabel: 'X',
      normalizedKey: 'svc:x',
      sourceInstanceId: 's1',
    })
    discovery.dismiss(candidate.id)
    expect(() => discovery.accept(candidate.id)).toThrowError(/is dismissed/)
    // accepted 后 replay 幂等（不是 throw），但重新 accept 语义 = 同一 node
    const { candidate: c2 } = discovery.upsertCandidate({
      candidateKind: 'service',
      displayLabel: 'Y',
      normalizedKey: 'svc:y',
      sourceInstanceId: 's1',
    })
    discovery.accept(c2.id)
    expect(discovery.accept(c2.id)).toEqual(discovery.accept(c2.id))
  })

  it('Verification：verified 不可被 evidence suggestion 降级；failed 不可被 suggestion 覆盖', () => {
    const card = nodes.create({ kind: 'payment_instrument', name: '卡' })
    const service = new ChangePlanService(driver)
    const actions: PlanAction[] = [
      {
        id: 'v1',
        title: '验证',
        detail: '',
        phase: 'verify',
        done: false,
        doneAt: null,
        verification: {
          method: 'future_observation',
          status: 'verified',
          verifiedAt: '2026-01-01T00:00:00Z',
          evidenceRefs: [],
        },
      },
      {
        id: 'v2',
        title: '验证2',
        detail: '',
        phase: 'verify',
        done: false,
        doneAt: null,
        verification: {
          method: 'future_observation',
          status: 'failed',
          verifiedAt: null,
          evidenceRefs: [],
        },
      },
    ]
    const plan = service.plans.create({
      scenario: 'replace_payment_card',
      title: 't',
      targetNodeId: card.id,
      actions,
      graphRevision: getGraphRevision(driver),
    })
    const { matchedActionIds } = service.applyEvidenceSignal(plan.id, {
      fromNodeId: 'x',
      toNodeId: 'y',
      evidenceRef: 'e#1',
    })
    // 无 watch 字段匹配 → 无 suggestion；verified/failed 状态不因 suggestion 流程变化
    expect(matchedActionIds).toEqual([])
    const stored = service.plans.getExisting(plan.id).actions
    expect(stored.find((a) => a.id === 'v1')?.verification?.status).toBe('verified')
    expect(stored.find((a) => a.id === 'v2')?.verification?.status).toBe('failed')
  })

  it('Rebase 原子性：lastAnalyzedGraphRevision 仅在成功分析后推进', () => {
    const card = nodes.create({ kind: 'payment_instrument', name: '卡' })
    const plan = instantiateScenario(driver, 'replace_payment_card', {
      targetPaymentInstrumentId: card.id,
    })
    const service = new ChangePlanService(driver)
    const repos = () => ({
      deps,
      groups: new DependencyGroupRepository(driver),
      proposals: new DependencyProposalRepository(driver),
      plans: service.plans,
    })
    const beforeRevision = getGraphRevision(driver)
    // revision 未变 → no-op，lastAnalyzed 不变
    const noop = rebasePlan(driver, repos(), plan.id)
    expect(noop.revisionChanged).toBe(false)
    expect(service.plans.getExisting(plan.id).lastAnalyzedGraphRevision).toBe(beforeRevision)
    // revision 推进 → 成功 rebase → lastAnalyzed = current（成功后才写）
    deps.confirm({
      from: card.id,
      relation: 'funding_source',
      to: nodes.create({ kind: 'account', name: 'W' }).id,
      capability: 'payment',
    })
    const ok = rebasePlan(driver, repos(), plan.id)
    expect(ok.revisionChanged).toBe(true)
    expect(service.plans.getExisting(plan.id).lastAnalyzedGraphRevision).toBe(
      getGraphRevision(driver),
    )
  })
})
