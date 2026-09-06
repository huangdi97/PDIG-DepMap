import { describe, expect, it, beforeEach, afterEach } from 'vitest'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { NodeSqliteDriver } from '../../src/db/node-driver.ts'
import { migrate } from '../../src/schema/migrations.ts'
import { MetaRepository } from '../../src/repositories/meta-repository.ts'
import { NodeRepository } from '../../src/repositories/node-repository.ts'
import { DependencyRepository } from '../../src/repositories/dependency-repository.ts'
import { DependencyGroupRepository } from '../../src/repositories/group-repository.ts'
import { canonicalGroupKey } from '../../src/domain/types.ts'

describe('Repositories (Schema v1)', () => {
  let dir: string
  let driver: NodeSqliteDriver
  let nodes: NodeRepository
  let deps: DependencyRepository
  let groups: DependencyGroupRepository
  let meta: MetaRepository

  beforeEach(() => {
    dir = mkdtempSync(join(tmpdir(), 'depmap-repo-'))
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
    migrate(driver)
    nodes = new NodeRepository(driver)
    deps = new DependencyRepository(driver)
    groups = new DependencyGroupRepository(driver)
    meta = new MetaRepository(driver)
  })

  afterEach(() => {
    driver.close()
    rmSync(dir, { recursive: true, force: true })
  })

  // --------------------------------------------------------------- meta
  it('fpSecret 只创建一次且稳定', () => {
    const a = meta.getOrCreateFpSecret(() => 'secret-A')
    const b = meta.getOrCreateFpSecret(() => 'secret-B')
    expect(a.created).toBe(true)
    expect(b.created).toBe(false)
    expect(b.secret).toBe('secret-A')
  })

  // --------------------------------------------------------------- nodes
  it('node create/get/update roundtrip', () => {
    const n = nodes.create({ kind: 'payment_instrument', templateId: 'builtin.bank_card.credit', name: '招行经典白', issuer: '招商银行', last4: '4417' })
    expect(n.owner).toBe('self')
    const u = nodes.update(n.id, { fields: { billDay: 5 } })
    expect(u.fields['billDay']).toBe(5)
    expect(u.kind).toBe('payment_instrument')
    const a = nodes.update(n.id, { archived: true })
    expect(a.archived).toBe(true)
  })

  // --------------------------------------------------- dependency UPSERT
  it('同一 logical key：首次 INSERT，再次 confirm 是 verify 更新（不重复建边）', () => {
    const first = deps.confirm({ from: 'card1', relation: 'funding_source', to: 'wechat', capability: 'payment' })
    expect(first.reactivated).toBe(false)
    expect(first.verified).toBe(false)
    expect(deps.countAll()).toBe(1)

    const second = deps.confirm({
      from: 'card1',
      relation: 'funding_source',
      to: 'wechat',
      capability: 'payment',
      evidenceRefs: ['ev1']
    })
    expect(second.verified).toBe(true)
    expect(second.reactivated).toBe(false)
    expect(second.dependency.id).toBe(first.dependency.id)
    expect(second.dependency.evidenceRefs).toContain('ev1')
    expect(deps.countAll()).toBe(1)
    expect(second.dependency.lastVerifiedAt >= first.dependency.confirmedAt).toBe(true)
  })

  it('retired Dependency 重新确认 → re-activate 同一 id，不生成第二条逻辑边', () => {
    const first = deps.confirm({ from: 'card1', relation: 'funding_source', to: 'wechat', capability: 'payment' })
    const retired = deps.retire(first.dependency.id)
    expect(retired.state).toBe('retired')
    expect(retired.retiredAt).not.toBeNull()

    const again = deps.confirm({ from: 'card1', relation: 'funding_source', to: 'wechat', capability: 'payment' })
    expect(again.reactivated).toBe(true)
    expect(again.dependency.id).toBe(first.dependency.id)
    expect(again.dependency.state).toBe('active')
    expect(again.dependency.retiredAt).toBeNull()
    expect(deps.countAll()).toBe(1)
  })

  it('criticality 默认 unknown；显式 required 才生效', () => {
    const d = deps.confirm({ from: 'a', relation: 'funding_source', to: 'b', capability: 'payment' })
    expect(d.dependency.criticality).toBe('unknown')
    const d2 = deps.confirm({ from: 'a', relation: 'funding_source', to: 'b', capability: 'payment', criticality: 'required' })
    expect(d2.dependency.criticality).toBe('required')
  })

  it('listActiveIncomingTo / listActiveOutgoingFrom 过滤 state 与 capability', () => {
    deps.confirm({ from: 'a', relation: 'funding_source', to: 'c', capability: 'payment' })
    const b = deps.confirm({ from: 'b', relation: 'funding_source', to: 'c', capability: 'payment' })
    deps.confirm({ from: 'z', relation: 'recovers', to: 'c', capability: 'recovery' })
    deps.retire(b.dependency.id)

    expect(deps.listActiveIncomingTo('c', 'payment')).toHaveLength(1)
    expect(deps.listActiveIncomingTo('c', 'payment')[0]!.from).toBe('a')
    expect(deps.listActiveOutgoingFrom('a', 'payment')).toHaveLength(1)
    expect(deps.listActiveOutgoingFrom('z', 'payment')).toHaveLength(0)
  })

  // --------------------------------------------------------------- groups
  it('groupKey canonical：[A,B] 与 [B,A] 是同一组（UNIQUE 去重）', () => {
    const d1 = deps.confirm({ from: 'a', relation: 'funding_source', to: 'wechat', capability: 'payment' })
    const d2 = deps.confirm({ from: 'b', relation: 'funding_source', to: 'wechat', capability: 'payment' })

    const g1 = groups.confirm(
      { targetNodeId: 'wechat', capability: 'payment', mode: 'ANY', memberEdgeIds: [d1.dependency.id] },
      [dependencyKeyOf(d1.dependency.id), dependencyKeyOf(d2.dependency.id)]
    )
    expect(g1.reactivated).toBe(false)

    // 相同成员（乱序）再确认 → 同一 group，不新建
    const g2 = groups.confirm(
      { targetNodeId: 'wechat', capability: 'payment', mode: 'ANY', memberEdgeIds: [d2.dependency.id] },
      [dependencyKeyOf(d2.dependency.id), dependencyKeyOf(d1.dependency.id)]
    )
    expect(g2.group.id).toBe(g1.group.id)
    expect(g2.reactivated).toBe(false)

    // canonicalGroupKey 顺序无关
    expect(canonicalGroupKey('wechat', 'payment', 'ANY', ['a|funding_source|wechat|payment', 'b|funding_source|wechat|payment'])).toBe(
      canonicalGroupKey('wechat', 'payment', 'ANY', ['b|funding_source|wechat|payment', 'a|funding_source|wechat|payment'])
    )
  })

  it('retired group 可 re-activate', () => {
    const g = groups.confirm({ targetNodeId: 't', capability: 'payment', mode: 'ANY', memberEdgeIds: ['e1'] }, ['x|funding_source|t|payment'])
    groups.retire(g.group.id)
    const again = groups.confirm({ targetNodeId: 't', capability: 'payment', mode: 'ANY', memberEdgeIds: ['e1'] }, ['x|funding_source|t|payment'])
    expect(again.group.id).toBe(g.group.id)
    expect(again.group.state).toBe('active')
  })
})

function dependencyKeyOf(_id: string): string {
  // 测试辅助：member logical key 只需要唯一字符串，语义由 canonicalGroupKey 处理
  return `${_id}-logical`
}
