import { describe, expect, it, beforeEach, afterEach } from 'vitest'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { NodeSqliteDriver } from '../../src/db/node-driver.ts'
import { migrate } from '../../src/schema/migrations.ts'
import { DependencyProposalRepository, REPROPOSAL_MIN_NEW_OBSERVATIONS } from '../../src/repositories/proposal-repository.ts'
import { DependencyGroupProposalRepository } from '../../src/repositories/group-proposal-repository.ts'
import { EvidenceRepository } from '../../src/repositories/evidence-repository.ts'

const BASE = {
  from: 'card1',
  relation: 'funding_source' as const,
  to: 'wechat',
  capability: 'payment' as const,
  proposalType: 'recurring_payment_route',
  source: 'statement',
  parserId: 'wechat',
  parserVersion: 1,
  confidenceScore: 0.9
}

describe('Proposal lifecycle (Schema v1)', () => {
  let dir: string
  let driver: NodeSqliteDriver
  let proposals: DependencyProposalRepository
  let groupProposals: DependencyGroupProposalRepository
  let evidence: EvidenceRepository

  beforeEach(() => {
    dir = mkdtempSync(join(tmpdir(), 'depmap-prop-'))
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
    migrate(driver)
    proposals = new DependencyProposalRepository(driver)
    groupProposals = new DependencyGroupProposalRepository(driver)
    evidence = new EvidenceRepository(driver)
  })

  afterEach(() => {
    driver.close()
    rmSync(dir, { recursive: true, force: true })
  })

  it('同 key UPSERT：pending 继续累计 observationCount，不创建第二条', () => {
    const r1 = proposals.upsert({ ...BASE, newObservations: 2 })
    expect(r1.changed).toBe(true)
    expect(r1.proposal.observationCount).toBe(2)
    const r2 = proposals.upsert({ ...BASE, newObservations: 4, confidenceScore: 0.96 })
    expect(r2.changed).toBe(true)
    expect(r2.proposal.observationCount).toBe(6)
    expect(r2.proposal.confidenceScore).toBe(0.96)
    expect(proposals.listAll()).toHaveLength(1)
  })

  it('accepted 后不再重复问', () => {
    const r1 = proposals.upsert(BASE)
    proposals.decide(r1.proposal.key, 'accepted')
    const r2 = proposals.upsert({ ...BASE, newObservations: 10 })
    expect(r2.alreadyAccepted).toBe(true)
    expect(r2.changed).toBe(false)
    expect(r2.proposal.decision).toBe('accepted')
  })

  it('rejected 后：新观测不足（<3 或 <1 完整周期）不得重提', () => {
    const r1 = proposals.upsert({ ...BASE, newObservations: 4 })
    proposals.decide(r1.proposal.key, 'rejected')
    const rejected = proposals.getByKey(r1.proposal.key)!
    expect(rejected.rejectedAtObservationCount).toBe(4)
    expect(rejected.rejectedAt).not.toBeNull()

    // +2 新观测（< 3）→ 不重提
    const r2 = proposals.upsert({ ...BASE, newObservations: 2 }, { cyclesCovered: 1 })
    expect(r2.suppressed).toBe(true)
    expect(r2.proposal.decision).toBe('rejected')

    // +3 新观测但没有覆盖完整周期 → 不重提
    const r3 = proposals.upsert({ ...BASE, newObservations: 3 }, { cyclesCovered: 0 })
    expect(r3.suppressed).toBe(true)
    expect(r3.proposal.decision).toBe('rejected')
  })

  it('rejected 后：新观测 ≥3 且覆盖 ≥1 完整周期 → 软性重提回 pending', () => {
    const r1 = proposals.upsert({ ...BASE, newObservations: 4 })
    proposals.decide(r1.proposal.key, 'rejected')
    const r2 = proposals.upsert({ ...BASE, newObservations: REPROPOSAL_MIN_NEW_OBSERVATIONS }, { cyclesCovered: 1 })
    expect(r2.suppressed).toBe(false)
    expect(r2.changed).toBe(true)
    expect(r2.proposal.decision).toBe('pending')
    expect(r2.proposal.rejectedAt).toBeNull()
    expect(r2.proposal.observationCount).toBe(7)
  })

  it('evidence 只累计新 unique observations 且 min/max 正确', () => {
    const a = evidence.accumulate({
      proposalKey: 'card1|funding_source|wechat|payment',
      sourceType: 'wechat_bill',
      parserId: 'wechat',
      parserVersion: 1,
      importSessionId: 's1',
      observedAt: '2026-04-15',
      newObservations: 2
    })
    expect(a.created).toBe(true)
    const b = evidence.accumulate({
      proposalKey: 'card1|funding_source|wechat|payment',
      sourceType: 'wechat_bill',
      parserId: 'wechat',
      parserVersion: 1,
      importSessionId: 's2',
      observedAt: '2026-03-01',
      newObservations: 1
    })
    expect(b.created).toBe(false)
    const e = evidence.getByProposalKey('card1|funding_source|wechat|payment')!
    expect(e.observationCount).toBe(3)
    expect(e.firstObservedAt).toBe('2026-03-01')
    expect(e.lastObservedAt).toBe('2026-04-15')
    expect(e.lastImportSessionId).toBe('s2')
  })

  it('GroupProposal：canonical key 去重（成员乱序同组）', () => {
    const members = ['b|funding_source|wechat|payment', 'a|funding_source|wechat|payment']
    const r1 = groupProposals.upsert({ targetNodeId: 'wechat', capability: 'payment', mode: 'ANY', memberDependencyKeys: members })
    const r2 = groupProposals.upsert({
      targetNodeId: 'wechat',
      capability: 'payment',
      mode: 'ANY',
      memberDependencyKeys: [...members].reverse()
    })
    expect(r2.proposal.id).toBe(r1.proposal.id)
    expect(groupProposals.listAll()).toHaveLength(1)
  })

  it('GroupProposal：拒绝后需显著新 evidence 才重提', () => {
    const r1 = groupProposals.upsert({
      targetNodeId: 'wechat',
      capability: 'payment',
      mode: 'ANY',
      memberDependencyKeys: ['a|funding_source|wechat|payment', 'b|funding_source|wechat|payment'],
      newObservations: 2
    })
    groupProposals.decide(r1.proposal.key, 'rejected')
    const r2 = groupProposals.upsert(
      { targetNodeId: 'wechat', capability: 'payment', mode: 'ANY', memberDependencyKeys: r1.proposal.memberDependencyKeys, newObservations: 2 },
      { cyclesCovered: 1 }
    )
    expect(r2.suppressed).toBe(true)
    expect(r2.proposal.decision).toBe('rejected')

    const r3 = groupProposals.upsert(
      { targetNodeId: 'wechat', capability: 'payment', mode: 'ANY', memberDependencyKeys: r1.proposal.memberDependencyKeys, newObservations: 3 },
      { cyclesCovered: 1 }
    )
    expect(r3.suppressed).toBe(false)
    expect(r3.proposal.decision).toBe('pending')
  })
})
