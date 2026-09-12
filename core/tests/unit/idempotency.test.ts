import { describe, expect, it, beforeEach, afterEach } from 'vitest'
import { mkdtempSync, rmSync, readFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { NodeSqliteDriver } from '../../src/db/node-driver.ts'
import { migrate, currentSchemaVersion, SCHEMA_VERSION } from '../../src/schema/migrations.ts'
import { NodeRepository } from '../../src/repositories/node-repository.ts'
import { DependencyRepository } from '../../src/repositories/dependency-repository.ts'
import { DependencyGroupRepository } from '../../src/repositories/group-repository.ts'
import { DependencyProposalRepository } from '../../src/repositories/proposal-repository.ts'
import { ImportFlow } from '../../src/services/import-pipeline.ts'
import { ConfirmationService } from '../../src/services/confirmation-service.ts'
import { exportGraph, importGraph } from '../../src/services/graph-serialize.ts'

/**
 * PHASE I — Idempotency / Replay。
 */

const FX = (name: string) =>
  new Uint8Array(
    readFileSync(join(dirname(fileURLToPath(import.meta.url)), '..', 'fixtures', name)),
  )

describe('Idempotency / Replay (RC PHASE I)', () => {
  let dir: string
  let driver: NodeSqliteDriver

  beforeEach(() => {
    dir = mkdtempSync(join(tmpdir(), 'depmap-idem-'))
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
    migrate(driver)
  })

  afterEach(() => {
    driver.close()
    rmSync(dir, { recursive: true, force: true })
  })

  it('migration 重复执行 50 次安全（版本稳定、无重复）', () => {
    for (let i = 0; i < 50; i++) {
      migrate(driver)
      expect(currentSchemaVersion(driver)).toBe(SCHEMA_VERSION)
    }
    const tableCount = driver
      .prepare(`SELECT COUNT(*) AS c FROM sqlite_master WHERE type='table' AND name='dependencies'`)
      .get()
    expect(Number((tableRow(tableCount) as Record<string, unknown>)['c'])).toBe(1)
  })

  it('相同账单重复导入：Evidence/Proposal/Dependency/Group 均不重复', async () => {
    const nodes = new NodeRepository(driver)
    const deps = new DependencyRepository(driver)
    const proposals = new DependencyProposalRepository(driver)
    const groups = new DependencyGroupRepository(driver)
    const confirm = new ConfirmationService(driver)

    const tencent = nodes.create({ kind: 'service', name: '腾讯视频' })
    // 卡节点使 funding_source 提案可匹配（支付方式含 招商银行信用卡(4417)）
    nodes.create({
      kind: 'payment_instrument',
      name: '招行经典白',
      issuer: '招商银行',
      last4: '4417',
    })

    for (let round = 0; round < 3; round++) {
      const flow = new ImportFlow(driver)
      await flow.begin(FX('recurring-monthly.csv'))
      if (round === 0) flow.resolveMerchant('腾讯视频', tencent.id)
      await flow.finalize()
    }

    // 三个 round 只产生 2 条 proposal（merchant + funding），evidence 不重复翻倍
    expect(proposals.listAll()).toHaveLength(2)
    const evidenceRows = driver.prepare(`SELECT COUNT(*) AS c FROM evidence`).get()
    expect(Number((evidenceRows as Record<string, unknown>)['c'])).toBe(2)

    for (const p of proposals.listAll()) confirm.acceptProposal(p.key)
    expect(deps.countAll()).toBe(2)

    // 再次导入（已确认）→ 不新建 proposal、不新建 dependency
    const flow2 = new ImportFlow(driver)
    await flow2.begin(FX('recurring-monthly.csv'))
    await flow2.finalize()
    expect(proposals.listAll()).toHaveLength(2)
    expect(deps.countAll()).toBe(2)
    expect(groups.listAllActive()).toHaveLength(0)
  })

  it('accepted Proposal 重放确认：不产生第二条 Dependency', () => {
    const proposals = new DependencyProposalRepository(driver)
    const nodes = new NodeRepository(driver)
    const confirm = new ConfirmationService(driver)
    const card = nodes.create({ kind: 'payment_instrument', name: '卡', last4: '1234' })
    void card
    const wechat = nodes.create({ kind: 'account', name: '微信' })

    const r = proposals.upsert({
      from: card.id,
      relation: 'funding_source',
      to: wechat.id,
      capability: 'payment',
      proposalType: 'recurring_payment_route',
      source: 'statement',
      parserId: 'wechat',
      parserVersion: 1,
      confidenceScore: 0.9,
    })
    const first = confirm.acceptProposal(r.proposal.key)
    const second = confirm.acceptProposal(r.proposal.key)
    expect(first.dependency.id).toBe(second.dependency.id)
    const deps = new DependencyRepository(driver)
    expect(deps.countAll()).toBe(1)
  })

  it('retire/reactivate 多轮：ID 恒定', () => {
    const deps = new DependencyRepository(driver)
    const first = deps.confirm({
      from: 'a',
      relation: 'funding_source',
      to: 'b',
      capability: 'payment',
    })
    const currentId = first.dependency.id
    for (let round = 0; round < 5; round++) {
      deps.retire(currentId)
      expect(deps.getById(currentId)!.state).toBe('retired')
      const again = deps.confirm({
        from: 'a',
        relation: 'funding_source',
        to: 'b',
        capability: 'payment',
      })
      expect(again.reactivated).toBe(true)
      expect(again.dependency.id).toBe(currentId)
    }
  })

  it('export → import → export：逻辑图深度等价；unsupported payloadVersion 拒绝', async () => {
    const nodes = new NodeRepository(driver)
    const confirm = new ConfirmationService(driver)
    const flow = new ImportFlow(driver)

    // 卡节点用于 funding_source 提案匹配（last4 4417）
    nodes.create({
      kind: 'payment_instrument',
      name: '招行经典白',
      issuer: '招商银行',
      last4: '4417',
    })
    const tencent = nodes.create({ kind: 'service', name: '腾讯视频' })
    await flow.begin(FX('recurring-monthly.csv'))
    flow.resolveMerchant('腾讯视频', tencent.id)
    const outcome = await flow.finalize()
    for (const k of outcome.proposalKeys) confirm.acceptProposal(k)
    const ccb = nodes.create({ kind: 'payment_instrument', name: '建行龙卡', last4: '8821' })
    const wechat = nodes.list({ kind: 'account' })[0]
    if (!wechat) throw new Error('wechat account node missing')
    confirm.addManualDependency({
      from: ccb.id,
      relation: 'funding_source',
      to: wechat.id,
      capability: 'payment',
    })
    const gp = confirm.detectGroupProposals()
    for (const k of gp) confirm.acceptGroupProposal(k)

    const export1 = exportGraph(driver)

    // 导入到全新 DB
    const driver2 = new NodeSqliteDriver(join(dir, 'restore.db'))
    driver2.open()
    migrate(driver2)
    try {
      importGraph(driver2, export1.payloadJson)
      const export2 = exportGraph(driver2)
      expect(export2.payloadJson).toBe(export1.payloadJson)

      // 等价性：关键实体计数一致
      expect(export2.counts['dependencies']).toBe(export1.counts['dependencies'])
      expect(export2.counts['dependency_groups']).toBe(export1.counts['dependency_groups'])
      // 导入后再导出再导入：幂等
      const driver3 = new NodeSqliteDriver(join(dir, 'restore2.db'))
      driver3.open()
      migrate(driver3)
      try {
        importGraph(driver3, export2.payloadJson)
        expect(exportGraph(driver3).payloadJson).toBe(export1.payloadJson)
      } finally {
        driver3.close()
      }
    } finally {
      driver2.close()
    }

    // unsupported payloadVersion 明确失败且不影响现有 DB
    const bad = JSON.stringify({ ...JSON.parse(export1.payloadJson), payloadVersion: 99 })
    const before = exportGraph(driver).payloadJson
    expect(() => importGraph(driver, bad)).toThrowError(/unsupported payloadVersion/)
    expect(exportGraph(driver).payloadJson).toBe(before)
  })
})

function tableRow(v: unknown): unknown {
  return v
}
