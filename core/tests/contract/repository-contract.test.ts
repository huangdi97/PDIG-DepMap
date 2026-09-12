import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import {
  canonicalGroupKey,
  DependencyGroupRepository,
  DependencyProposalRepository,
  DependencyRepository,
  EvidenceRepository,
  FingerprintRepository,
  MetaRepository,
  migrate,
  NodeSqliteDriver,
  REPROPOSAL_MIN_NEW_OBSERVATIONS,
  SCHEMA_VERSION,
  type SqliteDriver,
  type UpsertProposalInput,
} from '../../src/index.ts'

/**
 * Engineering Baseline V1 — RepositoryContract（参数化契约 harness）。
 *
 * 当前 reference implementation = NodeSqliteDriver（node:sqlite）。
 * 未来任何第二持久化实现（如平台端 SQLite 封装）必须在 DRIVERS 注册并跑通同一套语义，
 * 否则不得声明 Repository 层 TESTED（见 docs/TEST_STRATEGY.md 契约层）。
 *
 * R1 Dependency 逻辑键 UPSERT 生命周期（insert / verify / retire → reactivate 同一 id）
 * R2 Group canonical key + 幂等 confirm + 复活
 * R3 Proposal 生命周期（pending 累计 / accepted 不重问 / rejected 重提阈值）
 * R4 Evidence 流语义（每 (proposalKey, sourceInstance) 一条流；min/max/count 单调）
 * R5 Fingerprint 作用域去重（批内 + 库内 + 跨实例隔离）
 * R6 Migration 幂等 ×10（版本恒定，无漂移）
 * R7 fpSecret create-once
 */

interface DriverHandle {
  driver: SqliteDriver
  dispose: () => void
}

/** 已注册驱动实现。新增实现时在此追加。 */
const DRIVERS: Array<{ label: string; open: (label: string) => DriverHandle }> = [
  {
    label: 'NodeSqliteDriver (node:sqlite)',
    open: (label) => {
      const dir = mkdtempSync(join(tmpdir(), `depmap-repocontract-${label}-`))
      const driver = new NodeSqliteDriver(join(dir, 'test.db'))
      driver.open()
      return {
        driver,
        dispose: () => {
          driver.close()
          rmSync(dir, { recursive: true, force: true })
        },
      }
    },
  },
]

function runRepositoryContract(open: (l: string) => DriverHandle): void {
  let handle: DriverHandle

  beforeEach(() => {
    handle = open('main')
    migrate(handle.driver)
  })

  afterEach(() => handle.dispose())

  it('R1: Dependency 逻辑键 UPSERT —— insert / verify / retire→reactivate 恒同一行', () => {
    const deps = new DependencyRepository(handle.driver)
    const key = {
      from: 'n-card',
      relation: 'funding_source',
      to: 'n-wechat',
      capability: 'payment',
    } as const

    const first = deps.confirm(key)
    expect(first.reactivated).toBe(false)
    expect(first.verified).toBe(false)

    const verify = deps.confirm(key)
    expect(verify.dependency.id).toBe(first.dependency.id)
    expect(verify.verified).toBe(true)
    expect(deps.countAll()).toBe(1)

    deps.retire(first.dependency.id)
    expect(deps.getById(first.dependency.id)?.state).toBe('retired')

    const revive = deps.confirm(key)
    expect(revive.reactivated).toBe(true)
    expect(revive.dependency.id).toBe(first.dependency.id)
    expect(revive.dependency.state).toBe('active')
    expect(deps.countAll()).toBe(1)
    expect(deps.findByLogicalKey(key.from, key.relation, key.to, key.capability)?.id).toBe(
      first.dependency.id,
    )
  })

  it('R2: Group canonical key —— 成员顺序无关，重复 confirm 幂等，retired 可复活', () => {
    const deps = new DependencyRepository(handle.driver)
    const groups = new DependencyGroupRepository(handle.driver)
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

    const mk = () => ({
      targetNodeId: 'n-t',
      capability: 'payment' as const,
      mode: 'ANY' as const,
      memberEdgeIds: [d1.dependency.id, d2.dependency.id],
    })
    const keys1 = [`k-${d1.dependency.id}`, `k-${d2.dependency.id}`]
    const keys2 = [`k-${d2.dependency.id}`, `k-${d1.dependency.id}`]

    const g1 = groups.confirm(mk(), keys1).group
    const g2 = groups.confirm(mk(), keys2).group
    expect(g2.id).toBe(g1.id)
    expect(g1.groupKey).toBe(
      canonicalGroupKey('n-t', 'payment', 'ANY', [
        `k-${d1.dependency.id}`,
        `k-${d2.dependency.id}`,
      ]),
    )
    expect(groups.listAllActive().length).toBe(1)

    groups.retire(g1.id)
    const g3 = groups.confirm(mk(), keys1)
    expect(g3.reactivated).toBe(true)
    expect(g3.group.id).toBe(g1.id)
  })

  it('R3: Proposal 生命周期 —— pending 累计 / accepted 不重问 / rejected 重提阈值', () => {
    const proposals = new DependencyProposalRepository(handle.driver)
    const base: UpsertProposalInput = {
      from: 'n-card',
      relation: 'merchant_agreement',
      to: 'n-svc',
      capability: 'payment',
      proposalType: 'merchant_agreement',
      source: 'test',
      parserId: 'test-parser',
      parserVersion: 1,
      confidenceScore: 0.9,
    }

    const up1 = proposals.upsert({ ...base, newObservations: 2 })
    expect(up1.changed).toBe(true)
    const up2 = proposals.upsert({ ...base, newObservations: 4 })
    expect(up2.changed).toBe(false)
    expect(up2.alreadyAccepted).toBe(false)
    expect(up2.proposal.decision).toBe('pending')
    expect(up2.proposal.observationCount).toBe(6)

    proposals.decide(up1.proposal.key, 'accepted', 'required')
    const up3 = proposals.upsert({ ...base, newObservations: 1 })
    expect(up3.alreadyAccepted).toBe(true)
    expect(up3.changed).toBe(false)

    // rejected → 重提需「单流」新观测 ≥ REPROPOSAL_MIN_NEW_OBSERVATIONS 且 cyclesCovered ≥ 1
    const evidence = {
      sourceInstanceId: 's1',
      adapterId: 'test-adapter',
      adapterVersion: 1,
      sourceType: 'statement_file' as const,
      newObservations: REPROPOSAL_MIN_NEW_OBSERVATIONS - 1,
    }
    const r1 = proposals.upsert({ ...base, to: 'n-svc2', evidence })
    proposals.decide(r1.proposal.key, 'rejected')
    const short = proposals.upsert(
      { ...base, to: 'n-svc2', evidence: { ...evidence, newObservations: 2 } },
      { cyclesCovered: 1 },
    )
    expect(short.changed).toBe(false)
    expect(short.suppressed).toBe(true)
    const enough = proposals.upsert(
      {
        ...base,
        to: 'n-svc2',
        evidence: { ...evidence, newObservations: REPROPOSAL_MIN_NEW_OBSERVATIONS },
      },
      { cyclesCovered: 1 },
    )
    expect(enough.changed).toBe(true)
    expect(enough.proposal.decision).toBe('pending')
  })

  it('R4: Evidence 流语义 —— 每 (proposalKey, sourceInstance) 单流，min/max/count 单调', () => {
    const proposals = new DependencyProposalRepository(handle.driver)
    const evidence = new EvidenceRepository(handle.driver)
    const up = proposals.upsert({
      from: 'n-card',
      relation: 'merchant_agreement',
      to: 'n-svc',
      capability: 'payment',
      proposalType: 'merchant_agreement',
      source: 'test',
      parserId: 'test-parser',
      parserVersion: 1,
      confidenceScore: 0.8,
    })
    const key = up.proposal.key

    const mk = (obs: number, at: string) => ({
      proposalKey: key,
      sourceInstanceId: 's1',
      adapterId: 'test-adapter',
      adapterVersion: 1,
      sourceType: 'statement_file' as const,
      parserId: 'test-parser',
      parserVersion: 1,
      importSessionId: 'sess-1',
      firstObservedAt: at,
      lastObservedAt: at,
      newObservations: obs,
    })

    const a = evidence.accumulate(mk(2, '2026-01-05T00:00:00Z'))
    expect(a.created).toBe(true)
    const b = evidence.accumulate(mk(3, '2026-02-05T00:00:00Z'))
    expect(b.created).toBe(false)
    expect(b.evidence.id).toBe(a.evidence.id)
    expect(b.evidence.observationCount).toBe(5)
    expect(b.evidence.firstObservedAt).toBe('2026-01-05T00:00:00Z')
    expect(b.evidence.lastObservedAt).toBe('2026-02-05T00:00:00Z')

    // 同 proposalKey 不同实例 = 新流
    const c = evidence.accumulate({ ...mk(1, '2026-03-01T00:00:00Z'), sourceInstanceId: 's2' })
    expect(c.created).toBe(true)
    expect(c.evidence.id).not.toBe(a.evidence.id)
    expect(evidence.listByProposalKey(key).length).toBe(2)
  })

  it('R5: Fingerprint 作用域去重 —— 批内 + 库内去重，跨实例互不冲突', () => {
    const fps = new FingerprintRepository(handle.driver)
    const rec = (fingerprint: string, sourceInstanceId: string) => ({
      fingerprint,
      sourceInstanceId,
      fingerprintVersion: 1 as const,
      source: 'test',
      importSessionId: 'sess-1',
    })
    const r1 = fps.insertBatch([rec('fp-1', 's1'), rec('fp-1', 's1'), rec('fp-2', 's1')])
    expect(r1.fresh).toEqual(['fp-1', 'fp-2'])
    expect(r1.duplicates).toBe(1)

    const r2 = fps.insertBatch([rec('fp-1', 's1')])
    expect(r2.fresh).toEqual([])
    expect(r2.duplicates).toBe(1)

    // 同指纹在另一实例下是新鲜的（作用域隔离）
    const r3 = fps.insertBatch([rec('fp-1', 's2')])
    expect(r3.fresh).toEqual(['fp-1'])
    expect(fps.exists('s1', 'fp-1')).toBe(true)
    expect(fps.exists('s2', 'fp-1')).toBe(true)
  })

  it('R6: Migration 幂等 ×10 —— 版本恒定，重复迁移零漂移', () => {
    for (let i = 0; i < 10; i++) {
      expect(migrate(handle.driver)).toBe(SCHEMA_VERSION)
    }
  })

  it('R7: fpSecret create-once —— 首次创建，之后恒定', () => {
    const meta = new MetaRepository(handle.driver)
    let calls = 0
    const gen = () => {
      calls++
      return 'secret-value'
    }
    const a = meta.getOrCreateFpSecret(gen)
    expect(a.created).toBe(true)
    const b = meta.getOrCreateFpSecret(gen)
    expect(b.created).toBe(false)
    expect(b.secret).toBe(a.secret)
    expect(calls).toBe(1)
  })
}

describe('RepositoryContract（参数化 harness，Engineering Baseline V1）', () => {
  for (const d of DRIVERS) {
    describe(`[${d.label}]`, () => runRepositoryContract(d.open))
  }
})
