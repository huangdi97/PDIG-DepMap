import { describe, it, expect } from 'vitest'
import { mkdtempSync, rmSync } from 'node:fs'
import { join } from 'node:path'
import { tmpdir } from 'node:os'
import { NodeSqliteDriver } from '../../src/db/node-driver.ts'
import { migrate } from '../../src/schema/migrations.ts'
import { NodeRepository } from '../../src/repositories/node-repository.ts'
import { DependencyProposalRepository } from '../../src/repositories/proposal-repository.ts'
import { ConfirmationService } from '../../src/services/confirmation-service.ts'
import {
  RELATION_DEFINITIONS,
  getRelationDefinition,
  listRuntimeRelationIds,
  validateRelationGroupUse,
  validateRelationUse,
} from '../../src/domain/relation-registry.ts'

/**
 * MVP02 PHASE 6 —— RelationDefinitionRegistry 直接测试（正/负路径）。
 *
 * GOAL §12：runtime 只支持 funding_source / merchant_agreement；
 * 所有 Proposal/Dependency writes 必须过 registry；Group 必须
 * capability-scoped 且模式受控；机器不得设置 required。
 */

describe('RelationDefinitionRegistry (MVP02 PHASE 6)', () => {
  it('runtime 词表 = 5 个 production relations；verifies/bound_to 仍不进入 runtime（v0.3.0）', () => {
    expect(listRuntimeRelationIds()).toEqual([
      'funding_source',
      'merchant_agreement',
      'recovers',
      'authenticates',
      'controls',
    ])
    expect(getRelationDefinition('verifies')).toBeNull()
    expect(getRelationDefinition('bound_to')).toBeNull()
    expect(getRelationDefinition('recovers')).not.toBeNull()
    expect(getRelationDefinition('authenticates')).not.toBeNull()
    expect(getRelationDefinition('controls')).not.toBeNull()
    expect(getRelationDefinition('funding_source')).not.toBeNull()
    expect(getRelationDefinition('merchant_agreement')).not.toBeNull()
  })

  it('定义治理字段：defaultCriticality=unknown、verificationPolicy=user_only、impactSemantics=dependency', () => {
    for (const def of RELATION_DEFINITIONS) {
      // v0.3.0：capability 从 payment-only 扩展到 5 个 runtime capabilities
      expect(['payment', 'access', 'authentication', 'recovery', 'communication']).toContain(
        def.capability,
      )
      expect(def.defaultCriticality).toBe('unknown')
      expect(def.verificationPolicy).toBe('user_only')
      expect(def.impactSemantics).toBe('dependency')
      expect(def.allowedGroupModes.length === 0 || def.allowedGroupModes.includes('ANY')).toBe(true)
      expect(def.allowedGroupModes).not.toContain('ALL')
    }
    // MVP 不允许 N-of-M / ALL 模式进入 registry
    expect(RELATION_DEFINITIONS.every((d) => !d.allowedGroupModes.includes('ALL'))).toBe(true)
  })

  // ---------------------------------------------------------------------------
  // validateRelationUse —— 正路径
  // ---------------------------------------------------------------------------

  it('funding_source：payment_instrument/account → account/payment_instrument, capability=payment 放行', () => {
    expect(
      validateRelationUse('payment_instrument', 'funding_source', 'account', 'payment').ok,
    ).toBe(true)
    expect(
      validateRelationUse('account', 'funding_source', 'payment_instrument', 'payment').ok,
    ).toBe(true)
  })

  it('merchant_agreement：account/payment_instrument → service/membership/account 放行', () => {
    expect(
      validateRelationUse('payment_instrument', 'merchant_agreement', 'service', 'payment').ok,
    ).toBe(true)
    expect(validateRelationUse('account', 'merchant_agreement', 'membership', 'payment').ok).toBe(
      true,
    )
  })

  // ---------------------------------------------------------------------------
  // validateRelationUse —— 负路径（每个拒绝都必须带 reason）
  // ---------------------------------------------------------------------------

  it('未注册 relation → 拒绝（DB CHECK 允许的 future 词表在 runtime 被拦截）', () => {
    const r = validateRelationUse('payment_instrument', 'verifies', 'service', 'payment')
    expect(r.ok).toBe(false)
    expect(r.reason).toMatch(/not in the runtime registry/)
  })

  it('capability 不匹配 → 拒绝（registry 是 capability-scoped）', () => {
    const r = validateRelationUse('payment_instrument', 'funding_source', 'account', 'access')
    expect(r.ok).toBe(false)
    expect(r.reason).toMatch(/requires capability 'payment'/)
  })

  it('fromKind 越界 → 拒绝（PAYMENT_KINDS 之外的 kind 不能作为 from）', () => {
    const r = validateRelationUse('device', 'funding_source', 'account', 'payment')
    expect(r.ok).toBe(false)
    expect(r.reason).toMatch(/does not allow fromKind 'device'/)
    // service 属于 PAYMENT_KINDS，作为 funding_source 的 from 是设计允许
    expect(validateRelationUse('service', 'funding_source', 'account', 'payment').ok).toBe(true)
  })

  it('toKind 越界 → 拒绝（如 funding_source 不能指向 service）', () => {
    const r = validateRelationUse('payment_instrument', 'funding_source', 'service', 'payment')
    expect(r.ok).toBe(false)
    expect(r.reason).toMatch(/does not allow toKind 'service'/)
  })

  it('merchant_agreement 方向不可逆：service → payment_instrument 拒绝', () => {
    const r = validateRelationUse('service', 'merchant_agreement', 'payment_instrument', 'payment')
    expect(r.ok).toBe(false)
    expect(r.reason).toMatch(/fromKind/)
  })

  // ---------------------------------------------------------------------------
  // validateRelationGroupUse —— Group 只允许用户确认的 funding_source ANY
  // ---------------------------------------------------------------------------

  it('funding_source + ANY 放行；ALL 拒绝（MVP 无 N-of-M/ALL）', () => {
    expect(validateRelationGroupUse('funding_source', 'ANY').ok).toBe(true)
    const all = validateRelationGroupUse('funding_source', 'ALL')
    expect(all.ok).toBe(false)
    expect(all.reason).toMatch(/does not allow group mode 'ALL'/)
  })

  it('merchant_agreement 不允许任何 Group（allowsGroup=false）', () => {
    const r = validateRelationGroupUse('merchant_agreement', 'ANY')
    expect(r.ok).toBe(false)
    expect(r.reason).toMatch(/does not allow groups/)
  })

  it('未注册 relation 的 Group 请求 → 拒绝', () => {
    const r = validateRelationGroupUse('verifies', 'ANY')
    expect(r.ok).toBe(false)
    expect(r.reason).toMatch(/not in the runtime registry/)
  })
})

describe('Registry 与写路径集成（Proposal/Dependency 必须过 registry）', () => {
  it('ConfirmationService.confirmProposal 对 runtime 未注册 relation 的 Proposal 抛 registry rejected', () => {
    const dir = mkdtempSync(join(tmpdir(), 'depmap-registry-'))
    const driver = new NodeSqliteDriver(join(dir, 't.db'))
    driver.open()
    migrate(driver)
    try {
      const nodes = new NodeRepository(driver)
      const card = nodes.create({ kind: 'payment_instrument', name: '测试卡' })
      const svc = nodes.create({ kind: 'service', name: '某服务' })
      const proposals = new DependencyProposalRepository(driver)
      // relation='verifies' 能通过 DB CHECK（v1 词表），但必须被 runtime registry 拦截
      proposals.upsert({
        from: card.id,
        relation: 'verifies',
        to: svc.id,
        capability: 'payment',
        proposalType: 'recurring_payment_route',
        source: 'statement',
        parserId: 'wechat',
        parserVersion: 1,
        confidenceScore: 0.9,
      })
      const confirm = new ConfirmationService(driver)
      const key = proposals.listAll()[0]!.key
      expect(() => confirm.acceptProposal(key)).toThrowError(/relation registry rejected/)
      // Proposal 状态未被推进为 accepted
      expect(proposals.getByKey(key)?.decision).toBe('pending')
    } finally {
      driver.close()
      rmSync(dir, { recursive: true, force: true })
    }
  })

  it('ConfirmationService.confirmProposal 合法 funding_source 路径正常确认（回归）', () => {
    const dir = mkdtempSync(join(tmpdir(), 'depmap-registry-ok-'))
    const driver = new NodeSqliteDriver(join(dir, 't.db'))
    driver.open()
    migrate(driver)
    try {
      const nodes = new NodeRepository(driver)
      const card = nodes.create({ kind: 'payment_instrument', name: '测试卡' })
      const acct = nodes.create({ kind: 'account', name: '微信支付' })
      const proposals = new DependencyProposalRepository(driver)
      proposals.upsert({
        from: card.id,
        relation: 'funding_source',
        to: acct.id,
        capability: 'payment',
        proposalType: 'recurring_payment_route',
        source: 'statement',
        parserId: 'wechat',
        parserVersion: 1,
        confidenceScore: 0.9,
      })
      const confirm = new ConfirmationService(driver)
      const key = proposals.listAll()[0]!.key
      const result = confirm.acceptProposal(key)
      expect(result.created).toBe(true)
      expect(result.dependency.verificationBasis?.type).toBe('user_confirmed')
    } finally {
      driver.close()
      rmSync(dir, { recursive: true, force: true })
    }
  })
})
