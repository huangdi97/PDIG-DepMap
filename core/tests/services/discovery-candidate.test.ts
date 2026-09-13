import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import {
  DependencyRepository,
  DiscoveryService,
  getGraphRevision,
  migrate,
  NodeRepository,
  NodeSqliteDriver,
} from '../../src/index.ts'

/**
 * MVP03 §34 —— DiscoveryCandidate Gate（PC）。
 */

describe('DiscoveryCandidate（MVP03 PC）', () => {
  let dir: string
  let driver: NodeSqliteDriver
  let discovery: DiscoveryService

  beforeEach(() => {
    dir = mkdtempSync(join(tmpdir(), 'depmap-pc-'))
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
    migrate(driver)
    discovery = new DiscoveryService(driver)
  })

  afterEach(() => {
    driver.close()
    rmSync(dir, { recursive: true, force: true })
  })

  it('同 normalized key upsert：累计观测数与证据引用，不重复建候选', () => {
    const r1 = discovery.upsertCandidate({
      candidateKind: 'payment_instrument',
      displayLabel: '建设银行 8821',
      normalizedKey: 'ccb:8821',
      sourceInstanceId: 's1',
      evidenceRef: 's1#1',
    })
    expect(r1.changed).toBe(true)
    const r2 = discovery.upsertCandidate({
      candidateKind: 'payment_instrument',
      displayLabel: '建设银行 8821',
      normalizedKey: 'ccb:8821',
      sourceInstanceId: 's1',
      evidenceRef: 's1#2',
    })
    expect(r2.changed).toBe(true)
    expect(r2.candidate.id).toBe(r1.candidate.id)
    expect(r2.candidate.observationCount).toBe(2)
    expect(r2.candidate.evidenceRefs).toEqual(['s1#1', 's1#2'])
    expect(discovery.listPending().length).toBe(1)
  })

  it('不同 SourceInstance provenance 保留（引用合并，不复制内容）', () => {
    const a = discovery.upsertCandidate({
      candidateKind: 'service',
      displayLabel: 'Netflix',
      normalizedKey: 'svc:netflix',
      sourceInstanceId: 'inst-A',
      evidenceRef: 'inst-A#5',
    })
    const b = discovery.upsertCandidate({
      candidateKind: 'service',
      displayLabel: 'Netflix Inc.',
      normalizedKey: 'svc:netflix',
      sourceInstanceId: 'inst-B',
      evidenceRef: 'inst-B#9',
    })
    expect(b.candidate.id).toBe(a.candidate.id)
    expect(b.candidate.evidenceRefs).toEqual(['inst-A#5', 'inst-B#9'])
  })

  it('accept → 恰好一个 Node；accept replay → 无重复 Node', () => {
    const { candidate } = discovery.upsertCandidate({
      candidateKind: 'payment_instrument',
      displayLabel: '建设银行 8821',
      normalizedKey: 'ccb:8821',
      sourceInstanceId: 's1',
    })
    const nodes = new NodeRepository(driver)
    const before = nodes.list().length
    const a1 = discovery.accept(candidate.id)
    expect(a1.created).toBe(true)
    expect(nodes.list().length).toBe(before + 1)
    const a2 = discovery.accept(candidate.id)
    expect(a2.created).toBe(false)
    expect(a2.nodeId).toBe(a1.nodeId)
    expect(nodes.list().length).toBe(before + 1)
  })

  it('dismiss → 不建 Node；dismissed 后新证据保守重提（<2 不回 pending，≥2 才回）', () => {
    const { candidate } = discovery.upsertCandidate({
      candidateKind: 'service',
      displayLabel: 'Spotify',
      normalizedKey: 'svc:spotify',
      sourceInstanceId: 's1',
    })
    discovery.dismiss(candidate.id)
    const nodes = new NodeRepository(driver)
    expect(nodes.list().length).toBe(0)

    const r1 = discovery.upsertCandidate({
      candidateKind: 'service', displayLabel: 'Spotify', normalizedKey: 'svc:spotify',
      sourceInstanceId: 's1', evidenceRef: 's1#9',
    })
    expect(r1.changed).toBe(false)
    expect(r1.candidate.status).toBe('dismissed')

    const r2 = discovery.upsertCandidate({
      candidateKind: 'service', displayLabel: 'Spotify', normalizedKey: 'svc:spotify',
      sourceInstanceId: 's1', evidenceRef: 's1#10',
    })
    expect(r2.changed).toBe(true)
    expect(r2.candidate.status).toBe('pending')
  })

  it('Candidate 不 bump graphRevision（upsert / dismiss / accept 均不提升）', () => {
    const before = getGraphRevision(driver)
    const { candidate } = discovery.upsertCandidate({
      candidateKind: 'payment_instrument',
      displayLabel: '中行 6222',
      normalizedKey: 'boc:6222',
      sourceInstanceId: 's1',
    })
    discovery.dismiss(candidate.id)
    expect(getGraphRevision(driver)).toBe(before)
    // superseded 同样不 bump（accept 的 Node 创建也不 bump；Reality 关系才 bump）
    const { candidate: c2 } = discovery.upsertCandidate({
      candidateKind: 'service', displayLabel: 'X', normalizedKey: 'svc:x', sourceInstanceId: 's1',
    })
    discovery.accept(c2.id)
    expect(getGraphRevision(driver)).toBe(before)
  })

  it('Candidate 不进入 Impact（不在 Node 依赖图关系中，无 Reality 边）', () => {
    const deps = new DependencyRepository(driver)
    const { candidate } = discovery.upsertCandidate({
      candidateKind: 'payment_instrument',
      displayLabel: '中行 6222',
      normalizedKey: 'boc:6222',
      sourceInstanceId: 's1',
    })
    discovery.accept(candidate.id)
    // 只有 Node，没有任何 Dependency/Group → 影响图无 active 边
    expect(deps.countAll()).toBe(0)
  })

  it('accepted 的候选 superseded：同名新信号不再打扰', () => {
    const { candidate } = discovery.upsertCandidate({
      candidateKind: 'service', displayLabel: 'X', normalizedKey: 'svc:x', sourceInstanceId: 's1',
    })
    discovery.accept(candidate.id)
    const r = discovery.upsertCandidate({
      candidateKind: 'service', displayLabel: 'X', normalizedKey: 'svc:x', sourceInstanceId: 's2', evidenceRef: 's2#1',
    })
    expect(r.changed).toBe(false)
    expect(r.candidate.status).toBe('accepted')
  })
})
