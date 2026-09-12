import { describe, expect, it, beforeEach, afterEach } from 'vitest'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { NodeSqliteDriver } from '../../src/db/node-driver.ts'
import { migrate } from '../../src/schema/migrations.ts'
import { parseWechatBill } from '../../src/parser/wechat/parser.ts'
import { resolveMerchant, type ResolvableEntity } from '../../src/resolver/resolver.ts'
import { NodeRepository } from '../../src/repositories/node-repository.ts'
import { DependencyProposalRepository } from '../../src/repositories/proposal-repository.ts'
import { DependencyRepository } from '../../src/repositories/dependency-repository.ts'
import { ConfirmationService } from '../../src/services/confirmation-service.ts'
import { simulateScenario, simulateDisable } from '../../src/impact/kernel.ts'
import type { Dependency } from '../../src/domain/types.ts'

/**
 * PHASE J — Negative / Error Paths（parser/resolver/proposal/repository/impact）。
 * crypto 负向见 crypto-negative.test.ts。
 */

const enc = new TextEncoder()

function bill(lines: string[]): Uint8Array {
  return enc.encode(lines.join('\r\n') + '\r\n')
}

const HEADER =
  '交易时间,交易类型,交易对方,商品,收/支,金额(元),支付方式,当前状态,交易单号,商户单号,备注'

function dep(
  from: string,
  to: string,
  criticality: 'required' | 'unknown' = 'unknown',
): Dependency {
  return {
    id: `e-${from}-${to}`,
    from,
    relation: 'funding_source',
    to,
    capability: 'payment',
    criticality,
    groupId: null,
    state: 'active',
    origin: 'manual',
    confirmedAt: 't',
    lastVerifiedAt: 't',
    retiredAt: null,
    evidenceRefs: [],
    verificationBasis: null,
    createdAt: 't',
    updatedAt: 't',
  }
}

describe('Negative — Parser (RC PHASE J)', () => {
  it('空文件：0 观测 + 1 个 missing header 错误，不抛出', () => {
    const r = parseWechatBill(enc.encode(''))
    expect(r.observations).toHaveLength(0)
    expect(r.errors).toHaveLength(1)
    expect(r.errors[0]?.reason).toContain('missing column header')
  })

  it('缺列行：计入 errors', () => {
    const r = parseWechatBill(bill([HEADER, '2026-01-03 10:23:00,商户消费,只有,四列']))
    expect(r.observations).toHaveLength(0)
    expect(r.errors.length).toBeGreaterThan(0)
  })

  it('非法日期/金额：分别报错，好行继续', () => {
    const r = parseWechatBill(
      bill([
        HEADER,
        '2026-02-30 10:00:00,商户消费,坏日期,商品,支出,¥1.00,零钱,支付成功,T1',
        '2026-01-03 25:99:00,商户消费,坏时分秒,商品,支出,¥1.00,零钱,支付成功,T2',
        '2026-01-05 10:00:00,商户消费,坏金额,商品,支出,¥1..0,零钱,支付成功,T3',
        '2026-01-06 10:00:00,商户消费,好行,正常商品,支出,¥3.50,零钱,支付成功,T4',
      ]),
    )
    expect(r.observations).toHaveLength(1)
    expect(r.observations[0]?.merchantRaw).toBe('好行')
    expect(r.errors).toHaveLength(3)
  })

  it('emoji / 特殊字符：不崩溃、原样保留', () => {
    const r = parseWechatBill(
      bill([
        HEADER,
        '2026-01-03 10:23:00,商户消费,🎮游戏商城🎁,皮肤📦,支出,¥6.00,零钱,支付成功,T1,,"备注😀"',
      ]),
    )
    expect(r.observations).toHaveLength(1)
    expect(r.observations[0]?.merchantRaw).toBe('🎮游戏商城🎁')
    expect(r.observations[0]?.note).toBe('备注😀')
  })

  it('混合换行 CR/LF/CRLF：全部解析', () => {
    const raw = enc.encode(
      [
        HEADER,
        '2026-01-03 10:23:00,a,b,c,支出,¥1.00,零钱,支付成功,T1',
        '2026-01-04 10:23:00,d,e,f,支出,¥2.00,零钱,支付成功,T2',
      ]
        .join('\r\n')
        .replace('\r\n2026-01-04', '\r2026-01-04'),
    )
    const r = parseWechatBill(raw)
    expect(r.observations).toHaveLength(2)
  })

  it('重复 header 行：以首个为准，后续 header 行报错跳过', () => {
    const r = parseWechatBill(
      bill([HEADER, HEADER, '2026-01-03 10:23:00,a,b,c,支出,¥1.00,零钱,支付成功,T1']),
    )
    expect(r.observations).toHaveLength(1)
  })

  it('10k 行：全部解析且行数正确', () => {
    const lines = [HEADER]
    for (let i = 0; i < 10000; i++) {
      lines.push(`2026-01-03 10:23:00,商户消费,商户${i % 7},商品,支出,¥1.00,零钱,支付成功,T${i}`)
    }
    const r = parseWechatBill(bill(lines))
    expect(r.observations).toHaveLength(10000)
    expect(r.errors).toHaveLength(0)
  })
})

describe('Negative — Resolver (RC PHASE J)', () => {
  it('alias 冲突：两个实体命中同一别名 → ambiguous', () => {
    const ents: ResolvableEntity[] = [
      { nodeId: 'a', name: '腾讯视频' },
      { nodeId: 'b', name: '腾讯视频VIP', aliases: ['腾讯视频'] },
    ]
    const r = resolveMerchant('腾讯视频', ents)
    expect(r.status).toBe('ambiguous')
  })

  it('fuzzy 同分：唯一候选规则不成立 → ambiguous', () => {
    const ents: ResolvableEntity[] = [
      { nodeId: 'a', name: '测试商户甲' },
      { nodeId: 'b', name: '测试商户乙' },
    ]
    const r = resolveMerchant('测试商户丙', ents)
    expect(['ambiguous', 'unresolved']).toContain(r.status)
  })

  it('空 merchant / 空白：unresolved', () => {
    expect(resolveMerchant('', []).status).toBe('unresolved')
    expect(resolveMerchant('   ', []).status).toBe('unresolved')
  })

  it('unknown 商户：unresolved（不产生 Proposal 的前置）', () => {
    expect(resolveMerchant('完全未知的商户', []).status).toBe('unresolved')
  })
})

describe('Negative — Proposal / Confirmation (RC PHASE J)', () => {
  let dir: string
  let driver: NodeSqliteDriver
  let nodes: NodeRepository
  let proposals: DependencyProposalRepository
  let deps: DependencyRepository
  let confirm: ConfirmationService

  beforeEach(() => {
    dir = mkdtempSync(join(tmpdir(), 'depmap-neg-'))
    driver = new NodeSqliteDriver(join(dir, 't.db'))
    driver.open()
    migrate(driver)
    nodes = new NodeRepository(driver)
    void nodes
    proposals = new DependencyProposalRepository(driver)
    deps = new DependencyRepository(driver)
    confirm = new ConfirmationService(driver)
  })

  afterEach(() => {
    driver.close()
    rmSync(dir, { recursive: true, force: true })
  })

  it('missing node：acceptProposal 抛错且不落库', () => {
    const r = proposals.upsert({
      from: 'ghost-card',
      relation: 'funding_source',
      to: 'ghost-wechat',
      capability: 'payment',
      proposalType: 'recurring_payment_route',
      source: 'statement',
      parserId: 'wechat',
      parserVersion: 1,
      confidenceScore: 0.9,
    })
    expect(() => confirm.acceptProposal(r.proposal.key)).toThrowError(/node not found/)
    expect(deps.countAll()).toBe(0)
    // proposal 仍是 pending（accept 失败不改状态）
    expect(proposals.getByKey(r.proposal.key)?.decision).toBe('pending')
  })

  it('invalid capability/relation：CHECK 拒绝（fail closed）', () => {
    expect(() =>
      driver
        .prepare(
          `INSERT INTO dependencies (id, from_node, relation, to_node, capability, origin, confirmed_at, last_verified_at, created_at, updated_at)
           VALUES ('x','a','bogus_relation','b','payment','manual','t','t','t','t')`,
        )
        .run(),
    ).toThrowError()
    expect(() =>
      driver
        .prepare(
          `INSERT INTO dependencies (id, from_node, relation, to_node, capability, origin, confirmed_at, last_verified_at, created_at, updated_at)
           VALUES ('y','a','funding_source','b','bogus_cap','manual','t','t','t','t')`,
        )
        .run(),
    ).toThrowError()
  })

  it('duplicate suggestion：同 key upsert 不产生第二行', () => {
    const base = {
      from: 'c1',
      relation: 'funding_source' as const,
      to: 'w1',
      capability: 'payment' as const,
      proposalType: 'recurring_payment_route',
      source: 'statement',
      parserId: 'wechat',
      parserVersion: 1,
      confidenceScore: 0.8,
    }
    proposals.upsert(base)
    proposals.upsert({ ...base, confidenceScore: 0.99 })
    expect(proposals.listAll()).toHaveLength(1)
    expect(proposals.listAll()[0]?.confidenceScore).toBe(0.99)
  })
})

describe('Negative — Impact (RC PHASE J)', () => {
  it('empty graph + empty unavailable：无目标、无 checklist（除原始操作外为空）', () => {
    const r = simulateScenario({ dependencies: [], groups: [], nodeNames: {} }, new Set())
    expect(r.targets).toHaveLength(0)
    expect(r.lostKeys).toHaveLength(0)
    expect(r.checklist).toHaveLength(0)
  })

  it('disable 不存在的节点：只产生原始操作项，无崩溃', () => {
    const r = simulateDisable(
      { dependencies: [dep('A', 'B')], groups: [], nodeNames: {} },
      '不存在的节点',
    )
    expect(r.targets).toHaveLength(0)
    expect(r.checklist).toHaveLength(1)
    expect(r.checklist[0]?.level).toBe('target_operation')
  })

  it('all retired：无传播', () => {
    const e = { ...dep('A', 'B', 'required'), state: 'retired' as const, retiredAt: 't' }
    const r = simulateDisable({ dependencies: [e], groups: [], nodeNames: {} }, 'A')
    expect(r.targets).toHaveLength(0)
  })

  it('deep chain（200 层）：全部传播且 depth 递增', () => {
    const deps: Dependency[] = []
    for (let i = 0; i < 200; i++) deps.push(dep(`n${i}`, `n${i + 1}`, 'required'))
    const r = simulateDisable({ dependencies: deps, groups: [], nodeNames: {} }, 'n0')
    expect(r.lostKeys).toHaveLength(201)
    expect(r.targets.find((t) => t.nodeId === 'n200')?.depth).toBe(200)
  })

  it('large cycle（300 节点）：终止且每 key 一次', () => {
    const deps: Dependency[] = []
    for (let i = 0; i < 300; i++) deps.push(dep(`c${i}`, `c${(i + 1) % 300}`, 'required'))
    const r = simulateDisable({ dependencies: deps, groups: [], nodeNames: {} }, 'c0')
    expect(r.lostKeys).toHaveLength(300)
    expect(new Set(r.lostKeys.map((k) => `${k.nodeId}|${k.capability}`)).size).toBe(300)
  })
})
