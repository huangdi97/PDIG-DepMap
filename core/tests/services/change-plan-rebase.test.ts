import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import {
  analyzePlanImpact,
  ChangePlanRepository,
  DependencyGroupRepository,
  DependencyProposalRepository,
  DependencyRepository,
  getGraphRevision,
  migrate,
  NodeRepository,
  NodeSqliteDriver,
  rebasePlan,
  type PlanAction,
} from '../../src/index.ts'

/**
 * MVP03 PRB-001..011 —— ChangePlan Rebase Gate。
 */

function repos(driver: NodeSqliteDriver) {
  return {
    deps: new DependencyRepository(driver),
    groups: new DependencyGroupRepository(driver),
    proposals: new DependencyProposalRepository(driver),
    plans: new ChangePlanRepository(driver),
  }
}

describe('ChangePlan Rebase（MVP03 PRB）', () => {
  let dir: string
  let driver: NodeSqliteDriver
  let nodes: NodeRepository
  let deps: DependencyRepository
  let proposals: DependencyProposalRepository
  let plans: ChangePlanRepository
  let cardA: string
  let cardB: string
  let wechat: string

  beforeEach(() => {
    dir = mkdtempSync(join(tmpdir(), 'depmap-prb-'))
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
    migrate(driver)
    nodes = new NodeRepository(driver)
    deps = new DependencyRepository(driver)
    proposals = new DependencyProposalRepository(driver)
    plans = new ChangePlanRepository(driver)
    cardA = nodes.create({ kind: 'payment_instrument', name: '招行 4417', last4: '4417' }).id
    cardB = nodes.create({ kind: 'payment_instrument', name: '建行 8821', last4: '8821' }).id
    wechat = nodes.create({ kind: 'account', templateId: 'builtin.account.wechat', name: '微信支付' }).id
    deps.confirm({ from: cardA, relation: 'funding_source', to: wechat, capability: 'payment', criticality: 'required' })
  })

  afterEach(() => {
    driver.close()
    rmSync(dir, { recursive: true, force: true })
  })

  /** 创建计划并立即建立影响基线（真实流程：模板创建 → 首次分析）。 */
  function makePlan(actions: PlanAction[] = []): string {
    const rev = getGraphRevision(driver)
    const plan = plans.create({
      scenario: 'replace_payment_card',
      title: '更换招行 4417',
      templateId: 'replace_payment_card',
      targetNodeId: cardA,
      actions,
      graphRevision: rev,
    })
    const snapshot = analyzePlanImpact(driver, { deps, groups: new DependencyGroupRepository(driver), proposals }, plan)
    return plans.updateAnalysis(plan.id, snapshot, rev, actions).id
  }

  it('PRB-001: revision unchanged → no rebase', () => {
    const id = makePlan()
    const before = plans.getExisting(id)
    const result = rebasePlan(driver, repos(driver), id)
    expect(result.revisionChanged).toBe(false)
    expect(result.diff.addedImpacts).toEqual([])
    expect(plans.getExisting(id)).toEqual(before)
  })

  it('PRB-002: revision changed → effective status needs_revalidation（rebase 后消除）', () => {
    const id = makePlan()
    deps.confirm({ from: cardB, relation: 'funding_source', to: wechat, capability: 'payment' })
    const stale = plans.getExisting(id)
    expect(stale.lastAnalyzedGraphRevision).toBeLessThan(getGraphRevision(driver))
    const result = rebasePlan(driver, repos(driver), id)
    expect(result.revisionChanged).toBe(true)
    expect(result.plan.lastAnalyzedGraphRevision).toBe(getGraphRevision(driver))
  })

  it('PRB-003: new Dependency creates new impact（must_change → needs_review 变化被记录）', () => {
    const id = makePlan() // 基线：disable cardA → wechat must_change（唯一 required 来源）
    deps.confirm({ from: cardB, relation: 'funding_source', to: wechat, capability: 'payment' })
    const result = rebasePlan(driver, repos(driver), id)
    expect(result.revisionChanged).toBe(true)
    const changed = result.diff.changedImpacts.filter((c) => c.nodeId === wechat)
    expect(changed.length).toBeGreaterThan(0)
    expect(changed[0]?.status).toBe('needs_review')
  })

  it('PRB-004: retired Dependency removes impact', () => {
    // cardB 备用 → wechat needs_review；retire cardB 边 → 影响回到 must_change 形态变化
    deps.confirm({ from: cardB, relation: 'funding_source', to: wechat, capability: 'payment' })
    const id = makePlan()
    const base = rebasePlan(driver, repos(driver), id)
    expect(base.plan.impactSnapshot?.targets.some((t) => t.status === 'needs_review')).toBe(true)
    const cardBEdge = deps.listActiveIncomingTo(wechat, 'payment').find((d) => d.from === cardB)
    expect(cardBEdge).toBeDefined()
    deps.retire(cardBEdge!.id)
    const result = rebasePlan(driver, repos(driver), id)
    expect(result.revisionChanged).toBe(true)
    expect(result.diff.changedImpacts.some((c) => c.nodeId === wechat && c.status === 'must_change')).toBe(true)
  })

  it('PRB-005: unknown relation remains review（unknown criticality 不产生 must_change）', () => {
    // cardB → wechat 为 unknown criticality；禁用 cardB 只产生 needs_review
    const unknownEdge = deps.confirm({ from: cardB, relation: 'funding_source', to: wechat, capability: 'payment' })
    expect(unknownEdge.dependency.criticality).toBe('unknown')
    const plan = plans.create({
      scenario: 'replace_payment_card',
      title: '更换建行 8821',
      targetNodeId: cardB,
      graphRevision: getGraphRevision(driver),
    })
    const snapshot = analyzePlanImpact(driver, { deps, groups: new DependencyGroupRepository(driver), proposals }, plan)
    plans.updateAnalysis(plan.id, snapshot, getGraphRevision(driver), [])
    const result = rebasePlan(driver, repos(driver), plan.id) // revision 未变 → no-op，读快照
    const stored = plans.getExisting(plan.id)!
    expect(stored.impactSnapshot?.targets.some((t) => t.status === 'needs_review')).toBe(true)
    expect(stored.impactSnapshot?.targets.some((t) => t.status === 'must_change')).toBe(false)
    void result
  })

  it('PRB-006: Proposal only does not bump revision（计划保持 analyzed 不触发 rebase）', () => {
    const id = makePlan()
    const base = rebasePlan(driver, repos(driver), id)
    const before = getGraphRevision(driver)
    proposals.upsert({
      from: cardB,
      relation: 'funding_source',
      to: wechat,
      capability: 'payment',
      proposalType: 'merchant_agreement',
      source: 'test',
      parserId: 'test',
      parserVersion: 1,
      confidenceScore: 0.999,
    })
    expect(getGraphRevision(driver)).toBe(before)
    const after = rebasePlan(driver, repos(driver), id)
    expect(after.revisionChanged).toBe(false)
  })

  it('PRB-007: rebase updates lastAnalyzedGraphRevision（同调用内完成）', () => {
    const id = makePlan()
    deps.confirm({ from: cardB, relation: 'funding_source', to: wechat, capability: 'payment' })
    const result = rebasePlan(driver, repos(driver), id)
    expect(result.plan.lastAnalyzedGraphRevision).toBe(getGraphRevision(driver))
    expect(plans.getExisting(id)!.lastAnalyzedGraphRevision).toBe(getGraphRevision(driver))
  })

  it('PRB-008: rebase does not auto-complete actions（done/verification 原样保留）', () => {
    const actions: PlanAction[] = [
      {
        id: 'a1',
        title: '迁移自动扣款',
        detail: '把订阅迁移到新卡',
        phase: 'change',
        done: true,
        doneAt: '2026-09-13T00:00:00Z',
        verification: { method: 'future_observation', status: 'evidence_suggested', verifiedAt: null, evidenceRefs: ['e1'] },
      },
      { id: 'a2', title: '销毁旧卡', detail: '', phase: 'change', done: false, doneAt: null, verification: null },
    ]
    const id = makePlan(actions)
    deps.confirm({ from: cardB, relation: 'funding_source', to: wechat, capability: 'payment' })
    const result = rebasePlan(driver, repos(driver), id)
    expect(result.revisionChanged).toBe(true)
    const after = plans.getExisting(id)!.actions
    expect(after.find((a) => a.id === 'a1')?.done).toBe(true)
    expect(after.find((a) => a.id === 'a1')?.verification?.status).toBe('evidence_suggested')
    expect(after.find((a) => a.id === 'a2')?.done).toBe(false)
  })

  it('PRB-009: completed plan remains historical（不 rebase 不改写）', () => {
    const id = makePlan()
    plans.updateWorkflowState(id, 'completed')
    const snapshot = plans.getExisting(id)!.impactSnapshot
    deps.confirm({ from: cardB, relation: 'funding_source', to: wechat, capability: 'payment' })
    const result = rebasePlan(driver, repos(driver), id)
    expect(result.revisionChanged).toBe(false)
    expect(plans.getExisting(id)!.impactSnapshot).toEqual(snapshot)
    expect(plans.getExisting(id)!.workflowState).toBe('completed')
  })

  it('PRB-010: cancelled plan not reopened', () => {
    const id = makePlan()
    plans.updateWorkflowState(id, 'cancelled')
    deps.confirm({ from: cardB, relation: 'funding_source', to: wechat, capability: 'payment' })
    const result = rebasePlan(driver, repos(driver), id)
    expect(result.revisionChanged).toBe(false)
    expect(plans.getExisting(id)!.workflowState).toBe('cancelled')
  })

  it('PRB-011: deterministic diff ordering（多目标差异排序确定，重复 rebase 无差异）', () => {
    const alipay = nodes.create({ kind: 'account', name: '支付宝' }).id
    // cardA 两条 required 出边 → 基线：wechat/alipay 都 must_change
    deps.confirm({ from: cardA, relation: 'funding_source', to: alipay, capability: 'payment', criticality: 'required' })
    const id = makePlan()
    // cardB 以 unknown criticality 接管两个账户 → 两目标 must_change → needs_review
    deps.confirm({ from: cardB, relation: 'funding_source', to: wechat, capability: 'payment' })
    deps.confirm({ from: cardB, relation: 'funding_source', to: alipay, capability: 'payment' })
    const r1 = rebasePlan(driver, repos(driver), id)
    const ids1 = r1.diff.changedImpacts.map((c) => `${c.nodeId}|${c.capability}`)
    expect(ids1.length).toBe(2)
    expect([...ids1]).toEqual([...ids1].sort())
    // 同一状态重复 rebase：无差异（revision 已同步）
    const r2 = rebasePlan(driver, repos(driver), id)
    expect(r2.revisionChanged).toBe(false)
    expect(r2.diff).toEqual({
      addedImpacts: [], removedImpacts: [], changedImpacts: [],
      addedActions: [], removedActions: [], changedActions: [],
    })
  })
})
