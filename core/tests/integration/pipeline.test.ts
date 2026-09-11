import { describe, expect, it, beforeEach, afterEach } from 'vitest'
import { mkdtempSync, rmSync, readFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { NodeSqliteDriver } from '../../src/db/node-driver.ts'
import { migrate } from '../../src/schema/migrations.ts'
import { MetaRepository } from '../../src/repositories/meta-repository.ts'
import { NodeRepository } from '../../src/repositories/node-repository.ts'
import { DependencyRepository } from '../../src/repositories/dependency-repository.ts'
import { DependencyProposalRepository } from '../../src/repositories/proposal-repository.ts'
import { DependencyGroupRepository } from '../../src/repositories/group-repository.ts'
import { ImportFlow } from '../../src/services/import-pipeline.ts'
import { ConfirmationService } from '../../src/services/confirmation-service.ts'
import { simulateDisable, simulateScenario } from '../../src/impact/kernel.ts'
import { FINGERPRINT_VERSION } from '../../src/fingerprint/fingerprint.ts'

const FX = (name: string) =>
  new Uint8Array(readFileSync(join(dirname(fileURLToPath(import.meta.url)), '..', 'fixtures', name)))

describe('Integration — synthetic import flow (PHASE 9)', () => {
  let dir: string
  let driver: NodeSqliteDriver
  let nodes: NodeRepository
  let deps: DependencyRepository
  let proposals: DependencyProposalRepository
  let groups: DependencyGroupRepository
  let confirm: ConfirmationService
  let meta: MetaRepository

  beforeEach(() => {
    dir = mkdtempSync(join(tmpdir(), 'depmap-e2e-'))
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
    migrate(driver)
    nodes = new NodeRepository(driver)
    deps = new DependencyRepository(driver)
    proposals = new DependencyProposalRepository(driver)
    groups = new DependencyGroupRepository(driver)
    confirm = new ConfirmationService(driver)
    meta = new MetaRepository(driver)
  })

  afterEach(() => {
    driver.close()
    rmSync(dir, { recursive: true, force: true })
  })

  function seedCards() {
    const card = nodes.create({ kind: 'payment_instrument', name: '招行经典白', issuer: '招商银行', last4: '4417' })
    nodes.create({ kind: 'payment_instrument', name: '工行储蓄卡', issuer: '工商银行', last4: '1234' })
    return card
  }

  it('完整流程：begin → Node Resolution → finalize → 确认 → Group → Impact', () => {
    const card = seedCards()

    // 1. begin：解析 + 预解析
    const flow = new ImportFlow(driver)
    const begin = flow.begin(FX('recurring-monthly.csv'))
    expect(begin.errors).toHaveLength(0)
    expect(begin.rawCount).toBe(8)
    // 腾讯视频/美团/滴滴都无对应节点 → pending；招行卡节点不参与商户解析
    const tencentCandidate = begin.candidates.find(c => c.merchantRaw === '腾讯视频')!
    expect(tencentCandidate.resolution).toBe('pending')
    expect(tencentCandidate.observationCount).toBe(6)

    // 2. 用户在 Node Resolution 界面创建腾讯视频服务节点并确认映射
    const tencent = nodes.create({ kind: 'service', templateId: 'builtin.service.subscription', name: '腾讯视频' })
    flow.resolveMerchant('腾讯视频', tencent.id)

    // 3. finalize：指纹入库 + recurrence + proposals
    const outcome = flow.finalize()
    expect(outcome.newUniqueCount).toBe(8)
    expect(outcome.duplicateCount).toBe(0)
    expect(outcome.recurrences.some(x => x.merchantRaw === '腾讯视频' && x.period === 'monthly')).toBe(true)
    expect(outcome.unresolvedMerchants.sort()).toEqual(['滴滴出行', '美团平台商户'])
    // merchant_agreement(wechat→腾讯视频) + funding_source(招行4417→wechat)
    expect(outcome.proposalKeys).toHaveLength(2)
    const keys = [...outcome.proposalKeys].sort()
    expect(keys[0]).toMatch(/\|funding_source\|/)
    expect(keys[1]).toMatch(/\|merchant_agreement\|/)

    // session 统计
    expect(outcome.session.rawCount).toBe(8)
    expect(outcome.session.newUniqueCount).toBe(8)
    expect(outcome.session.proposalCount).toBe(2)
    expect(outcome.session.errorCount).toBe(0)

    // 4. 用户确认两条 proposal → Dependency（criticality 默认 unknown）
    for (const k of keys) confirm.acceptProposal(k)
    expect(deps.countAll()).toBe(2)

    // 5. 第二条 funding 边 + GroupProposal 检测 + 确认
    const ccb = nodes.create({ kind: 'payment_instrument', name: '建行龙卡', issuer: '建设银行', last4: '8821' })
    const wechat = nodes.list({ kind: 'account' }).find(n => n.templateId === 'builtin.account.wechat')!
    confirm.addManualDependency({ from: ccb.id, relation: 'funding_source', to: wechat.id, capability: 'payment' })
    const gpKeys = confirm.detectGroupProposals()
    expect(gpKeys).toHaveLength(1)
    const group = confirm.acceptGroupProposal(gpKeys[0]!)
    expect(group.mode).toBe('ANY')
    expect(group.memberEdgeIds).toHaveLength(2)

    // 6. simulateDisable 招行4417 → 微信有备用路径（能力降级）
    const graph = {
      dependencies: deps.listActive(),
      groups: groups.listAllActive(),
      nodeNames: Object.fromEntries(nodes.list().map(n => [n.id, n.name]))
    }
    const r = simulateDisable(graph, card.id)
    const wechatResult = r.targets.find(t => t.nodeId === wechat.id)!
    expect(wechatResult.status).toBe('backup_path')
    expect(wechatResult.redundancyDegraded).toBe(true)
    // checklist 原始操作最后
    expect(r.checklist[r.checklist.length - 1]!.level).toBe('target_operation')
    expect(r.checklist[r.checklist.length - 1]!.nodeId).toBe(card.id)

    // 7. 双卡同时失效 → 微信 lost
    const r2 = simulateScenario(graph, new Set([
      { nodeId: card.id, capability: 'payment' as const },
      { nodeId: ccb.id, capability: 'payment' as const }
    ]))
    expect(r2.targets.find(t => t.nodeId === wechat.id)!.status).toBe('must_change')
  })

  it('重复导入同一账单：指纹去重，不产生新观测/新 proposal', () => {
    seedCards()
    const tencent = nodes.create({ kind: 'service', name: '腾讯视频' })

    const flow1 = new ImportFlow(driver)
    flow1.begin(FX('recurring-monthly.csv'))
    flow1.resolveMerchant('腾讯视频', tencent.id)
    const o1 = flow1.finalize()
    expect(o1.newUniqueCount).toBe(8)

    // 确认所有 proposal（accepted → 不重复问）
    for (const k of o1.proposalKeys) confirm.acceptProposal(k)

    const flow2 = new ImportFlow(driver)
    // 腾讯视频现在可 auto-resolve（normalized exact），无需手动确认
    flow2.begin(FX('recurring-monthly.csv'))
    const o2 = flow2.finalize()
    expect(o2.newUniqueCount).toBe(0)
    expect(o2.duplicateCount).toBe(8)
    expect(o2.proposalKeys).toHaveLength(0)
    expect(o2.recurrences).toHaveLength(0) // 无 fresh 观测 → 无 recurrence
    // proposal 数据未被重复创建
    expect(proposals.listAll()).toHaveLength(2)
  })

  it('duplicate import 1–6 / 1–8 月：只累计 7–8 月并更新 evidence（GOAL §12 测试重点）', () => {
    seedCards()
    const tencent = nodes.create({ kind: 'service', name: '腾讯视频' })

    const flow1 = new ImportFlow(driver)
    flow1.begin(FX('dup-jan-jun.csv'))
    flow1.resolveMerchant('腾讯视频', tencent.id)
    const o1 = flow1.finalize()
    expect(o1.newUniqueCount).toBe(6)
    expect(o1.proposalKeys).toHaveLength(2)

    const flow2 = new ImportFlow(driver)
    flow2.begin(FX('dup-jan-aug.csv'))
    flow2.finalize() // 腾讯视频 auto-resolve，无需手动

    // evidence 汇总：merchant proposal 的 observationCount = 6 + 2 = 8
    const ev = driver
      .prepare(
        `SELECT observation_count, first_observed_at, last_observed_at FROM evidence WHERE proposal_key LIKE '%merchant_agreement%'`
      )
      .get()
    expect(Number(ev!.observation_count)).toBe(8)
    expect(String(ev!.first_observed_at)).toContain('2026-01-15')
    expect(String(ev!.last_observed_at)).toContain('2026-08-15')
  })

  it('未 resolution 商户不产生 Proposal；finalize 后放弃不会烧指纹', () => {
    seedCards()
    const flow = new ImportFlow(driver)
    flow.begin(FX('recurring-monthly.csv'))
    // 不做任何 resolveMerchant，直接 finalize
    const outcome = flow.finalize()
    expect(outcome.proposalKeys).toHaveLength(0)
    expect(outcome.unresolvedMerchants.length).toBeGreaterThan(0)
    // 指纹已入库（本次会话已消费——因为 finalize 执行了）
    expect(driver.prepare(`SELECT COUNT(*) AS c FROM observation_fingerprints`).get()).toMatchObject({ c: 8 })
  })

  it('retired dependency 复活：同一 logical key 同一 id', () => {
    seedCards()
    const tencent = nodes.create({ kind: 'service', name: '腾讯视频' })
    const flow = new ImportFlow(driver)
    flow.begin(FX('recurring-monthly.csv'))
    flow.resolveMerchant('腾讯视频', tencent.id)
    const o = flow.finalize()
    const merchantKey = o.proposalKeys.find(k => k.includes('merchant_agreement'))!
    confirm.acceptProposal(merchantKey)

    const [from, relation, to, capability] = merchantKey.split('|') as [string, string, string, string]
    const dep = deps.findByLogicalKey(from!, relation!, to!, capability)!
    deps.retire(dep.id)
    expect(deps.getById(dep.id)!.state).toBe('retired')

    const again = deps.confirm({
      from: from!,
      relation: relation as 'merchant_agreement',
      to: to!,
      capability: capability as import('../../src/domain/types.ts').Capability
    })
    expect(again.reactivated).toBe(true)
    expect(again.dependency.id).toBe(dep.id)
    expect(again.dependency.state).toBe('active')
  })

  it('rejected proposal：新 evidence 不足不重提；足够则软性重提', () => {
    seedCards()
    const tencent = nodes.create({ kind: 'service', name: '腾讯视频' })
    const flow = new ImportFlow(driver)
    flow.begin(FX('recurring-monthly.csv'))
    flow.resolveMerchant('腾讯视频', tencent.id)
    const o = flow.finalize()
    const merchantKey = o.proposalKeys.find(k => k.includes('merchant_agreement'))!
    confirm.rejectProposal(merchantKey)
    expect(proposals.getByKey(merchantKey)!.decision).toBe('rejected')

    // 重复导入 1–8 月（+2 新观测）→ 不足 3 → 保持 rejected
    const flow2 = new ImportFlow(driver)
    flow2.begin(FX('dup-jan-aug.csv'))
    flow2.finalize()
    expect(proposals.getByKey(merchantKey)!.decision).toBe('rejected')
  })

  it('malformed 账单：errors 计入 session，好行继续处理', () => {
    seedCards()
    const flow = new ImportFlow(driver)
    const begin = flow.begin(FX('malformed.csv'))
    expect(begin.errors).toHaveLength(4)
    const outcome = flow.finalize()
    expect(outcome.errorCount).toBe(4)
    expect(outcome.newUniqueCount).toBe(2)
    // 只有 1 次腾讯视频观测 → 无 recurrence → 无 proposal
    expect(outcome.proposalKeys).toHaveLength(0)
  })

  it('refund 观测不参与 recurrence', () => {
    seedCards()
    const flow = new ImportFlow(driver)
    flow.begin(FX('refund.csv'))
    const outcome = flow.finalize()
    expect(outcome.recurrences).toHaveLength(0)
    expect(outcome.proposalKeys).toHaveLength(0)
  })

  it('fpSecret 稳定 + 指纹版本号', () => {
    const a = meta.getOrCreateFpSecret(() => 'fixed')
    const b = meta.getOrCreateFpSecret(() => 'other')
    expect(a.secret).toBe(b.secret)
    expect(FINGERPRINT_VERSION).toBe(1)
  })
})
