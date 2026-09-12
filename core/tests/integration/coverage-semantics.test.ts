import { describe, expect, it, beforeEach, afterEach } from 'vitest'
import { mkdtempSync, rmSync, readFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { NodeSqliteDriver } from '../../src/db/node-driver.ts'
import { migrate } from '../../src/schema/migrations.ts'
import { NodeRepository } from '../../src/repositories/node-repository.ts'
import { DependencyRepository } from '../../src/repositories/dependency-repository.ts'
import { DependencyGroupRepository } from '../../src/repositories/group-repository.ts'
import { DependencyProposalRepository } from '../../src/repositories/proposal-repository.ts'
import { ImportFlow } from '../../src/services/import-pipeline.ts'
import { ConfirmationService } from '../../src/services/confirmation-service.ts'
import { assertFileAdapterContract, type EvidenceSourceAdapter } from '../../src/sources/types.ts'
import { WeChatStatementAdapter } from '../../src/sources/wechat/adapter.ts'
import { GenericCsvAdapter } from '../../src/sources/generic-csv/adapter.ts'
import { OfxQfxAdapter } from '../../src/sources/ofx/adapter.ts'
import { simulateDisable } from '../../src/impact/kernel.ts'

/**
 * MVP02 H 段 —— Coverage Semantics。
 *
 * 承载 AGENTS 第一原则与 GOAL §25：
 * `event_stream` 的 absence **绝不**产生现实否定。
 *
 * 具体禁止：
 * - 未出现 → 自动 retire 已确认 Dependency
 * - 未出现 → 自动 reject Proposal
 * - 未出现 → 自动确认 fallback（“有备用路径”）
 * - 未出现 → 产生 must_change
 *
 * 若此文件变红，说明产品第一原则被破坏，优先级高于任何新功能。
 */

const FX = (name: string) =>
  new Uint8Array(
    readFileSync(join(dirname(fileURLToPath(import.meta.url)), '..', 'fixtures', name)),
  )

describe('Coverage semantics — absence never negates Reality (MVP02 H)', () => {
  let dir: string
  let driver: NodeSqliteDriver
  let nodes: NodeRepository
  let deps: DependencyRepository
  let groups: DependencyGroupRepository
  let proposals: DependencyProposalRepository
  let confirm: ConfirmationService

  beforeEach(() => {
    dir = mkdtempSync(join(tmpdir(), 'depmap-coverage-'))
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
    migrate(driver)
    nodes = new NodeRepository(driver)
    deps = new DependencyRepository(driver)
    groups = new DependencyGroupRepository(driver)
    proposals = new DependencyProposalRepository(driver)
    confirm = new ConfirmationService(driver)
  })

  afterEach(() => {
    driver.close()
    rmSync(dir, { recursive: true, force: true })
  })

  // -------------------------------------------------------------------------
  // H0 —— 三个文件 Adapter 必须声明 event_stream / authoritativeFor=[]
  // -------------------------------------------------------------------------

  it('H0: 三个文件 Adapter 全部 event_stream 且 authoritativeFor 为空', () => {
    const adapters: EvidenceSourceAdapter[] = [
      new WeChatStatementAdapter(driver),
      new GenericCsvAdapter(),
      new OfxQfxAdapter(),
    ]
    for (const a of adapters) {
      // 契约断言：任何一项被改成 snapshot 或非空 authoritativeFor 都会立即失败
      expect(() => assertFileAdapterContract(a)).not.toThrow()
      expect(a.coverageMode).toBe('event_stream')
      expect(a.authoritativeFor).toEqual([])
      expect(a.sourceKind).toBe('statement_file')
    }
  })

  it('H0b: 违反契约的 adapter 会被 assertFileAdapterContract 拒绝（守卫本身有效）', () => {
    expect(() =>
      assertFileAdapterContract({
        coverageMode: 'complete_snapshot',
        authoritativeFor: [],
        sourceKind: 'statement_file',
      }),
    ).toThrowError(/must be event_stream/)

    expect(() =>
      assertFileAdapterContract({
        coverageMode: 'event_stream',
        authoritativeFor: ['funding_source'],
        sourceKind: 'statement_file',
      }),
    ).toThrowError(/empty authoritativeFor/)
  })

  // -------------------------------------------------------------------------
  // H1 —— 已确认 Dependency 不因“本次账单未出现”而 retire
  // -------------------------------------------------------------------------

  it('H1: 后续导入未再出现该商户 → Dependency 保持 active，不自动 retire', async () => {
    const card = seedCard()
    const account = seedWechatAccount()
    const tencent = nodes.create({ kind: 'service', name: '腾讯视频' })

    // 第一次导入：确认 CMB → WeChat（funding_source）
    const flow1 = new ImportFlow(driver)
    await flow1.begin(FX('recurring-monthly.csv'))
    flow1.resolveMerchant('腾讯视频', tencent.id)
    const o1 = await flow1.finalize()
    for (const k of o1.proposalKeys) confirm.acceptProposal(k)

    const fundingKey = o1.proposalKeys.find((k) => k.includes('|funding_source|'))!
    const [from, relation, to, capability] = fundingKey.split('|') as [
      string,
      string,
      string,
      string,
    ]
    const dep = deps.findByLogicalKey(from, relation, to, capability)
    expect(dep).not.toBeNull()
    expect(dep!.state).toBe('active')
    expect(from).toBe(card.id)
    expect(to).toBe(account.id)

    // 第二次导入一份“完全没有腾讯视频/该卡”的账单
    const flow2 = new ImportFlow(driver)
    await flow2.begin(FX('non-recurring.csv'))
    const o2 = await flow2.finalize()
    // 该账单没有可解析的服务候选 → 不产生新 proposal
    expect(o2.proposalKeys).toHaveLength(0)

    // 核心断言：absence 没有否定已确认现实
    const after = deps.getById(dep!.id)!
    expect(after.state).toBe('active')
    expect(after.retiredAt).toBeNull()
    expect(deps.countAll()).toBe(o1.proposalKeys.length)
  })

  // -------------------------------------------------------------------------
  // H2 —— 已 rejected Proposal 不因 absence 被“自动确认”或“永久封杀”
  // -------------------------------------------------------------------------

  it('H2: 未再出现 → Proposal 维持 rejected，既不自动 accept 也不产生第二条', () => {
    const card = seedCard()
    const account = seedWechatAccount()
    const r1 = proposals.upsert({
      from: card.id,
      relation: 'funding_source',
      to: account.id,
      capability: 'payment',
      proposalType: 'recurring_payment_route',
      source: 'statement',
      parserId: 'wechat_statement',
      parserVersion: 1,
      confidenceScore: 0.9,
      newObservations: 5,
    })
    proposals.decide(r1.proposal.key, 'rejected')

    // absence：没有任何新观测 → upsert 不应把 rejected 变成 accepted
    const r2 = proposals.upsert({
      from: card.id,
      relation: 'funding_source',
      to: account.id,
      capability: 'payment',
      proposalType: 'recurring_payment_route',
      source: 'statement',
      parserId: 'wechat_statement',
      parserVersion: 1,
      confidenceScore: 0.99,
    })

    expect(r2.proposal.decision).toBe('rejected')
    expect(r2.alreadyAccepted).toBe(false)
    expect(proposals.listAll()).toHaveLength(1)
    // rejected 不是永久为假：仍有重提路径（由单流阈值控制）
    expect(r2.suppressed).toBe(true)
  })

  // -------------------------------------------------------------------------
  // H3 —— absence 不产生 fallback 确认（“有两条边 ≠ 有备用路径”）
  // -------------------------------------------------------------------------

  it('H3: 仅存在两条 funding 边、Group 未确认 → 仍为 needs_review，不得宣称有备用', () => {
    const cmb = seedCard()
    const ccb = nodes.create({
      kind: 'payment_instrument',
      name: '建行龙卡',
      issuer: '建设银行',
      last4: '8821',
    })
    const account = seedWechatAccount()

    deps.confirm({
      from: cmb.id,
      relation: 'funding_source',
      to: account.id,
      capability: 'payment',
      criticality: 'required',
    })
    deps.confirm({
      from: ccb.id,
      relation: 'funding_source',
      to: account.id,
      capability: 'payment',
      criticality: 'unknown',
    })
    // 未确认任何 Group
    expect(groups.listAllActive()).toHaveLength(0)

    const report = simulateDisable(
      {
        dependencies: deps.listActive(),
        groups: groups.listAllActive(),
        nodeNames: {},
      },
      cmb.id,
    )

    // 不得因为“还有另一条边”就判定备用路径成立。
    // 铁律：two edges ≠ confirmed fallback（SSOT §25）。
    const target = report.targets.find((t) => t.nodeId === account.id)
    expect(target).toBeDefined()
    expect(target!.status).toBe('needs_review')
    expect(target!.status).not.toBe('backup_path')
    expect(target!.status).not.toBe('must_change')
    // 未确认备用 → 冗余度不得被宣称下降（backup_path 专属信号）
    expect(target!.redundancyDegraded).toBe(false)
    // 目标必须仍被判定为“可继续使用”（不是失效）
    expect(target!.available).toBe(true)
    expect(target!.reasonCode).toBe('unconfirmed_alternative_exists')
    expect(report.lostKeys.map((k) => k.nodeId)).not.toContain(account.id)
  })

  // -------------------------------------------------------------------------
  // H4 —— absence 不驱动 must_change（必须处理只能来自已确认现实）
  // -------------------------------------------------------------------------

  it('H4: 仅有旧观测、无已确认依赖 → 不产生 must_change', () => {
    const card = seedCard()
    const account = seedWechatAccount()
    // 存在 pending Proposal（机器推断），但没有任何确认的 Dependency
    const proposal = proposals.upsert({
      from: card.id,
      relation: 'funding_source',
      to: account.id,
      capability: 'payment',
      proposalType: 'recurring_payment_route',
      source: 'statement',
      parserId: 'wechat_statement',
      parserVersion: 1,
      confidenceScore: 0.999,
      newObservations: 9,
    })
    expect(deps.countAll()).toBe(0)
    expect(proposal.proposal.decision).toBe('pending')

    // 把 pending proposal 一并喂给 kernel（含“机器推断”和可能的漂移来源）
    const report = simulateDisable(
      {
        dependencies: [],
        groups: [],
        proposals: [
          {
            key: proposal.proposal.key,
            from: card.id,
            to: account.id,
            capability: 'payment',
            confidenceScore: 0.999,
          },
        ],
        nodeNames: {},
      },
      card.id,
    )

    // 铁律：Proposal ≠ Reality。机器推断（任何 confidence）永不产生 must_change。
    expect(report.targets.filter((t) => t.status === 'must_change')).toHaveLength(0)
    // 初始节点不进入 targets（depth 0 的自身不产生 target）
    expect(report.lostKeys).toEqual([{ nodeId: card.id, capability: 'payment' }])
    // 下游只能获得 needs_review（proposal_only），且明确可继续使用
    const target = report.targets.find((t) => t.nodeId === account.id)
    expect(target).toBeDefined()
    expect(target!.status).toBe('needs_review')
    expect(target!.reasonCode).toBe('proposal_only')
    expect(target!.available).toBe(true)
    // must_change 清单为空（checklist 中不得出现 must_change 级别条目）
    expect(report.checklist.filter((c) => c.level === 'must_change')).toHaveLength(0)
  })

  function seedCard() {
    return nodes.create({
      kind: 'payment_instrument',
      name: '招行经典白',
      issuer: '招商银行',
      last4: '4417',
    })
  }

  function seedWechatAccount() {
    return nodes.create({
      kind: 'account',
      templateId: 'builtin.account.wechat',
      name: '微信支付',
    })
  }
})
