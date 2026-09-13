import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import {
  bumpGraphRevision,
  DependencyGroupRepository,
  DependencyProposalRepository,
  DependencyRepository,
  EvidenceRepository,
  getGraphRevision,
  migrate,
  NodeSqliteDriver,
  SCHEMA_VERSION,
} from '../../src/index.ts'

/**
 * MVP03 GR-001..012 —— GraphRevision 语义 Gate。
 * 铁律：只有 Reality mutation 提升 revision，且与 Reality 写入同事务提交。
 */

describe('GraphRevision（MVP03 GR）', () => {
  let dir: string
  let driver: NodeSqliteDriver

  beforeEach(() => {
    dir = mkdtempSync(join(tmpdir(), 'depmap-gr-'))
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
    migrate(driver)
  })

  afterEach(() => {
    driver.close()
    rmSync(dir, { recursive: true, force: true })
  })

  it('GR-001: fresh graph revision = 0', () => {
    expect(getGraphRevision(driver)).toBe(0)
  })

  it('GR-002: confirm Dependency（新建）→ +1', () => {
    const deps = new DependencyRepository(driver)
    expect(
      deps.confirm({ from: 'n-a', relation: 'funding_source', to: 'n-b', capability: 'payment' })
        .reactivated,
    ).toBe(false)
    expect(getGraphRevision(driver)).toBe(1)
  })

  it('GR-003: Evidence accumulate 更新 → revision 不变', () => {
    const proposals = new DependencyProposalRepository(driver)
    const up = proposals.upsert({
      from: 'n-a',
      relation: 'merchant_agreement',
      to: 'n-svc',
      capability: 'payment',
      proposalType: 'merchant_agreement',
      source: 'test',
      parserId: 'test',
      parserVersion: 1,
      confidenceScore: 0.8,
    })
    const before = getGraphRevision(driver)
    const evidence = new EvidenceRepository(driver)
    evidence.accumulate({
      proposalKey: up.proposal.key,
      sourceInstanceId: 's1',
      adapterId: 'test',
      adapterVersion: 1,
      sourceType: 'statement_file',
      parserId: 'test',
      parserVersion: 1,
      importSessionId: 'sess',
      firstObservedAt: '2026-01-01T00:00:00Z',
      lastObservedAt: '2026-02-01T00:00:00Z',
      newObservations: 3,
    })
    expect(getGraphRevision(driver)).toBe(before)
  })

  it('GR-004: create / accumulate / decide Proposal → revision 不变', () => {
    const proposals = new DependencyProposalRepository(driver)
    const before = getGraphRevision(driver)
    const up = proposals.upsert({
      from: 'n-a',
      relation: 'merchant_agreement',
      to: 'n-svc',
      capability: 'payment',
      proposalType: 'merchant_agreement',
      source: 'test',
      parserId: 'test',
      parserVersion: 1,
      confidenceScore: 0.99,
    })
    proposals.upsert({
      ...{
        from: 'n-a',
        relation: 'merchant_agreement',
        to: 'n-svc',
        capability: 'payment',
        proposalType: 'merchant_agreement',
        source: 'test',
        parserId: 'test',
        parserVersion: 1,
        confidenceScore: 0.99,
      },
      newObservations: 5,
    })
    proposals.decide(up.proposal.key, 'accepted', 'required')
    expect(getGraphRevision(driver)).toBe(before)
  })

  it('GR-005: retire Dependency → +1；重复 retire 幂等不加', () => {
    const deps = new DependencyRepository(driver)
    const { dependency } = deps.confirm({
      from: 'n-a',
      relation: 'funding_source',
      to: 'n-b',
      capability: 'payment',
    })
    const base = getGraphRevision(driver) // =1（confirm）
    deps.retire(dependency.id)
    expect(getGraphRevision(driver)).toBe(base + 1)
    deps.retire(dependency.id)
    expect(getGraphRevision(driver)).toBe(base + 1)
  })

  it('GR-006: reactivate Dependency → +1', () => {
    const deps = new DependencyRepository(driver)
    const first = deps.confirm({
      from: 'n-a',
      relation: 'funding_source',
      to: 'n-b',
      capability: 'payment',
    })
    deps.retire(first.dependency.id)
    const base = getGraphRevision(driver)
    deps.confirm({ from: 'n-a', relation: 'funding_source', to: 'n-b', capability: 'payment' })
    expect(getGraphRevision(driver)).toBe(base + 1)
  })

  it('GR-007: confirm Group → +1；retire → +1；active 重确认幂等不加', () => {
    const deps = new DependencyRepository(driver)
    const groups = new DependencyGroupRepository(driver)
    const d1 = deps.confirm({
      from: 'n-a',
      relation: 'funding_source',
      to: 'n-t',
      capability: 'payment',
    })
    const d2 = deps.confirm({
      from: 'n-b',
      relation: 'funding_source',
      to: 'n-t',
      capability: 'payment',
    })
    const base = getGraphRevision(driver) // 2
    const mk = () => ({
      targetNodeId: 'n-t',
      capability: 'payment' as const,
      mode: 'ANY' as const,
      memberEdgeIds: [d1.dependency.id, d2.dependency.id],
    })
    const keys = [`k-${d1.dependency.id}`, `k-${d2.dependency.id}`]
    const { group } = groups.confirm(mk(), keys)
    expect(getGraphRevision(driver)).toBe(base + 1)
    groups.confirm(mk(), keys) // active 重确认 → verify 语义，不加
    expect(getGraphRevision(driver)).toBe(base + 1)
    groups.retire(group.id)
    expect(getGraphRevision(driver)).toBe(base + 2)
  })

  it('GR-008: migration/restart 保持 revision', () => {
    const deps = new DependencyRepository(driver)
    deps.confirm({ from: 'n-a', relation: 'funding_source', to: 'n-b', capability: 'payment' })
    expect(getGraphRevision(driver)).toBe(1)
    driver.close()
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
    expect(migrate(driver)).toBe(SCHEMA_VERSION)
    expect(getGraphRevision(driver)).toBe(1)
  })

  it('GR-009: 事务失败 → revision 不增加', () => {
    const deps = new DependencyRepository(driver)
    const before = getGraphRevision(driver)
    expect(() =>
      driver.transaction(() => {
        bumpGraphRevision(driver)
        deps.confirm({ from: 'n-x', relation: 'funding_source', to: 'n-y', capability: 'payment' })
        throw new Error('boom')
      }),
    ).toThrow('boom')
    expect(getGraphRevision(driver)).toBe(before)
    expect(deps.findByLogicalKey('n-x', 'funding_source', 'n-y', 'payment')).toBeNull()
  })

  it('GR-010: 顺序事务不丢 revision', () => {
    const deps = new DependencyRepository(driver)
    const groups = new DependencyGroupRepository(driver)
    const d1 = deps.confirm({
      from: 'n-a',
      relation: 'funding_source',
      to: 'n-t',
      capability: 'payment',
    })
    const d2 = deps.confirm({
      from: 'n-b',
      relation: 'funding_source',
      to: 'n-t',
      capability: 'payment',
    })
    groups.confirm(
      {
        targetNodeId: 'n-t',
        capability: 'payment',
        mode: 'ANY',
        memberEdgeIds: [d1.dependency.id, d2.dependency.id],
      },
      [`k-${d1.dependency.id}`, `k-${d2.dependency.id}`],
    )
    deps.retire(d1.dependency.id)
    deps.confirm({ from: 'n-a', relation: 'funding_source', to: 'n-t', capability: 'payment' })
    // confirm(+1) ×2 + group(+1) + retire(+1) + reactivate(+1) = 5
    expect(getGraphRevision(driver)).toBe(5)
  })

  it('GR-011: duplicate accepted replay / verify 重放 / criticality 不变 → 不重复增加', () => {
    const deps = new DependencyRepository(driver)
    const key = {
      from: 'n-a',
      relation: 'funding_source',
      to: 'n-b',
      capability: 'payment',
    } as const
    deps.confirm(key) // +1
    const base = getGraphRevision(driver)
    deps.confirm(key) // active verify 重放
    deps.confirm(key) // 再重放
    expect(getGraphRevision(driver)).toBe(base)
    deps.updateCriticality(
      deps.findByLogicalKey(key.from, key.relation, key.to, key.capability)!.id,
      'required',
    )
    expect(getGraphRevision(driver)).toBe(base + 1)
    deps.updateCriticality(
      deps.findByLogicalKey(key.from, key.relation, key.to, key.capability)!.id,
      'required',
    )
    expect(getGraphRevision(driver)).toBe(base + 1) // 值未变不 bump
  })

  it('GR-012: deterministic —— 相同操作序列恒定 revision', () => {
    const run = (): number => {
      const deps = new DependencyRepository(driver)
      deps.confirm({ from: 'n-a', relation: 'funding_source', to: 'n-b', capability: 'payment' })
      deps.confirm({ from: 'n-a', relation: 'funding_source', to: 'n-b', capability: 'payment' })
      const d = deps.findByLogicalKey('n-a', 'funding_source', 'n-b', 'payment')!
      deps.retire(d.id)
      deps.confirm({ from: 'n-a', relation: 'funding_source', to: 'n-b', capability: 'payment' })
      return getGraphRevision(driver)
    }
    const first = run()
    driver.close()
    driver = new NodeSqliteDriver(join(dir, 't2.db'))
    driver.open()
    migrate(driver)
    expect(run()).toBe(first)
    expect(first).toBe(3) // insert + retire + reactivate
  })
})
