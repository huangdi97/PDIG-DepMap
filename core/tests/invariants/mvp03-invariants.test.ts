import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import {
  buildTimeline,
  ChangePlanRepository,
  ChangePlanService,
  DependencyRepository,
  DependencyGroupRepository,
  DependencyProposalRepository,
  DiscoveryService,
  getGraphRevision,
  instantiateScenario,
  listActiveTemplates,
  migrate,
  NodeRepository,
  NodeSqliteDriver,
  rebasePlan,
  RealityDriftService,
  SourceInstanceRepository,
} from '../../src/index.ts'

/**
 * MVP03 INV-15..21（§60）—— Living Graph 不变量（真实 DB）。
 */

describe('MVP03 Living Graph invariants（INV-15..21）', () => {
  let dir: string
  let driver: NodeSqliteDriver
  let nodes: NodeRepository
  let deps: DependencyRepository
  let groups: DependencyGroupRepository
  let proposals: DependencyProposalRepository
  let plans: ChangePlanRepository
  let card: string

  beforeEach(() => {
    dir = mkdtempSync(join(tmpdir(), 'depmap-inv3-'))
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
    migrate(driver)
    nodes = new NodeRepository(driver)
    deps = new DependencyRepository(driver)
    groups = new DependencyGroupRepository(driver)
    proposals = new DependencyProposalRepository(driver)
    plans = new ChangePlanRepository(driver)
    card = nodes.create({ kind: 'payment_instrument', name: '招行 4417' }).id
  })

  afterEach(() => {
    driver.close()
    rmSync(dir, { recursive: true, force: true })
  })

  function graphRepos() {
    return { deps, groups, proposals, plans }
  }

  it('INV-15: 任何序列后 ChangePlan.lastAnalyzedGraphRevision ≤ 当前 graphRevision', () => {
    const plan = instantiateScenario(driver, 'replace_payment_card', { targetPaymentInstrumentId: card })
    // 若干 Reality mutation 之后
    const account = nodes.create({ kind: 'account', name: '微信支付' }).id
    deps.confirm({ from: card, relation: 'funding_source', to: account, capability: 'payment' })
    expect(plans.getExisting(plan.id)!.lastAnalyzedGraphRevision).toBeLessThanOrEqual(getGraphRevision(driver))
    // rebase 后相等
    rebasePlan(driver, graphRepos(), plan.id)
    expect(plans.getExisting(plan.id)!.lastAnalyzedGraphRevision).toBe(getGraphRevision(driver))
    // 再 mutation 后仍 ≤
    deps.retire(deps.listActiveOutgoingFrom(card, 'payment')[0]!.id)
    expect(plans.getExisting(plan.id)!.lastAnalyzedGraphRevision).toBeLessThanOrEqual(getGraphRevision(driver))
  })

  it('INV-16: ready_with_known_scope 必须 revision current + must_change 已处理', () => {
    const plan = instantiateScenario(driver, 'replace_payment_card', { targetPaymentInstrumentId: card })
    deps.confirm({ from: card, relation: 'funding_source', to: nodes.create({ kind: 'account', name: 'X' }).id, capability: 'payment', criticality: 'required' })
    const service = new ChangePlanService(driver)
    // 未 rebase（stale）且有未处理 must_change → blocked（blocked 优先于 review_required）
    expect(service.getReadiness(plan.id)).toBe('blocked')
    rebasePlan(driver, graphRepos(), plan.id)
    // must_change 未处理（change 动作未完成）→ blocked
    expect(service.getReadiness(plan.id)).toBe('blocked')
    // 完成 change 动作 → must_change 全部处理 → ready_with_known_scope
    const updated = service.plans.getExisting(plan.id)
    for (const action of updated.actions.filter((a) => a.phase === 'change')) {
      service.completeAction(plan.id, action.id)
    }
    expect(service.getReadiness(plan.id)).toBe('ready_with_known_scope')
  })

  it('INV-17: completed plan 不被 rebase 改写历史', () => {
    const plan = instantiateScenario(driver, 'replace_payment_card', { targetPaymentInstrumentId: card })
    const account = nodes.create({ kind: 'account', name: '微信支付' }).id
    deps.confirm({ from: card, relation: 'funding_source', to: account, capability: 'payment', criticality: 'required' })
    rebasePlan(driver, graphRepos(), plan.id)
    service_transition(plans, plan.id, 'completed')
    const snapshot = plans.getExisting(plan.id)!.impactSnapshot
    // 再来一次 Reality mutation + rebase
    deps.confirm({ from: card, relation: 'funding_source', to: nodes.create({ kind: 'account', name: 'Y' }).id, capability: 'payment' })
    rebasePlan(driver, graphRepos(), plan.id)
    expect(plans.getExisting(plan.id)!.impactSnapshot).toEqual(snapshot)
  })

  it('INV-18: confirmed_change drift 必对应成功 Reality mutation', () => {
    const cardB = nodes.create({ kind: 'payment_instrument', name: '建行 8821' }).id
    const wechat = nodes.create({ kind: 'account', templateId: 'builtin.account.wechat', name: '微信支付' }).id
    deps.confirm({ from: card, relation: 'funding_source', to: wechat, capability: 'payment', criticality: 'required' })
    const drifts = new RealityDriftService(driver)
    const { created } = drifts.detectFromEvidence({
      targetNodeId: wechat,
      capability: 'payment',
      signals: [{ fromNodeId: cardB, observations: 2, evidenceRef: 'i#1' }],
    })
    const before = getGraphRevision(driver)
    drifts.resolveAsReplacement(created[0]!.id)
    // 对应成功 mutation：新边存在且 revision 增加
    expect(deps.findByLogicalKey(cardB, 'funding_source', wechat, 'payment')).not.toBeNull()
    expect(getGraphRevision(driver)).toBeGreaterThan(before)
  })

  it('INV-19: accepted DiscoveryCandidate 不得创建 duplicate logical Node', () => {
    const discovery = new DiscoveryService(driver)
    const { candidate } = discovery.upsertCandidate({
      candidateKind: 'payment_instrument',
      displayLabel: '建行 8821',
      normalizedKey: 'ccb:8821',
      sourceInstanceId: 's1',
    })
    const nodesCountBefore = nodes.list().length
    const a1 = discovery.accept(candidate.id)
    const a2 = discovery.accept(candidate.id)
    expect(a1.nodeId).toBe(a2.nodeId)
    expect(nodes.list().length).toBe(nodesCountBefore + 1)
  })

  it('INV-20: TimelineItem 必须存在有效 source reference', () => {
    // 布置全部来源种类
    const plan = instantiateScenario(driver, 'replace_payment_card', { targetPaymentInstrumentId: card, effectiveDate: '2026-10-01' })
    const cardB = nodes.create({ kind: 'payment_instrument', name: '建行 8821' }).id
    const wechat = nodes.create({ kind: 'account', templateId: 'builtin.account.wechat', name: '微信支付' }).id
    deps.confirm({ from: card, relation: 'funding_source', to: wechat, capability: 'payment', criticality: 'required' })
    const drifts = new RealityDriftService(driver)
    drifts.detectFromEvidence({ targetNodeId: wechat, capability: 'payment', signals: [{ fromNodeId: cardB, observations: 2, evidenceRef: 'i#1' }] })
    const instances = new SourceInstanceRepository(driver)
    const inst = instances.create({ adapterId: 'generic_csv', adapterVersion: 1, sourceKind: 'statement_file', label: '旧来源' })
    instances.touchIngested(inst.id, '2026-01-01T00:00:00.000Z')
    void plan

    const items = buildTimeline(driver, '2026-09-13T00:00:00.000Z')
    expect(items.length).toBeGreaterThan(0)
    for (const item of items) {
      switch (item.sourceType) {
        case 'change_plan':
          expect(plans.getById(item.sourceId)).not.toBeNull()
          break
        case 'reality_drift':
          expect(new RealityDriftService(driver).listOpen().some((d) => d.id === item.sourceId)).toBe(true)
          break
        case 'node_expiry':
          expect(nodes.getById(item.sourceId)).not.toBeNull()
          break
        case 'source_freshness':
          expect(instances.getById(item.sourceId)).not.toBeNull()
          break
        case 'action_verification':
          expect(item.sourceId).toMatch(/\//)
          break
      }
    }
  })

  it('INV-21: active ScenarioTemplate 必须对应已支持 capability（payment）', () => {
    for (const t of listActiveTemplates()) {
      expect(t.availability).toBe('active')
      expect(t.scenarioFactory).not.toBeNull()
      for (const cap of t.supportedCapabilities) {
        expect(cap).toBe('payment') // MVP Impact 仅 payment
      }
    }
  })
})

function service_transition(plans: ChangePlanRepository, id: string, state: 'completed'): void {
  plans.updateWorkflowState(id, state)
}
