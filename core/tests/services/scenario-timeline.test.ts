import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import {
  buildTimeline,
  ChangePlanService,
  DependencyRepository,
  instantiateScenario,
  listActiveTemplates,
  migrate,
  NodeRepository,
  NodeSqliteDriver,
  PLANNED_TEMPLATES,
  RealityDriftService,
  getScenarioTemplate,
  SCENARIO_TEMPLATES,
  SourceInstanceRepository,
  type PlanAction,
} from '../../src/index.ts'

/**
 * MVP03 ST（ScenarioTemplate）与 TL（Timeline）Gate。
 */

const NOW = '2026-09-13T00:00:00.000Z'

describe('ScenarioTemplate（MVP03 ST）', () => {
  let dir: string
  let driver: NodeSqliteDriver

  beforeEach(() => {
    dir = mkdtempSync(join(tmpdir(), 'depmap-st-'))
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
    migrate(driver)
  })

  afterEach(() => {
    driver.close()
    rmSync(dir, { recursive: true, force: true })
  })

  it('ST-001: 4 个 active 模板（3 支付 + replace_phone_number），capability 在 v0.3.0 runtime 集合内', () => {
    const active = listActiveTemplates()
    expect(active.map((t) => t.id).sort()).toEqual([
      'close_payment_instrument',
      'expiring_payment_card',
      'replace_payment_card',
      'replace_phone_number',
    ])
    const paymentTemplates = active.filter((t) => t.category === 'payment')
    expect(paymentTemplates.length).toBe(3)
    for (const t of paymentTemplates) {
      expect(t.supportedCapabilities).toEqual(['payment'])
    }
    const phone = active.find((t) => t.id === 'replace_phone_number')
    expect(phone).toBeDefined()
    expect(phone?.category).toBe('identity')
    expect(phone?.supportedCapabilities).toEqual([
      'access',
      'authentication',
      'recovery',
      'communication',
    ])
    expect(getScenarioTemplate('expiring_payment_card')?.recommendedLeadTimeDays).toBe(30)
  })

  it('ST-002: planned 模板不可执行（无 factory → instantiate 拒绝）', () => {
    for (const planned of PLANNED_TEMPLATES) {
      expect(planned.availability).toBe('planned')
      expect(planned.scenarioFactory).toBeNull()
      expect(() => instantiateScenario(driver, planned.id, {})).toThrowError(/not executable/)
    }
    expect(listActiveTemplates().some((t) => t.availability === 'planned')).toBe(false)
  })

  it('ST-003: scenarioFactory 产出 ChangePlan（含 templateId / baseline=当前 revision / 默认动作）', () => {
    const nodes = new NodeRepository(driver)
    const card = nodes.create({ kind: 'payment_instrument', name: '招行 4417', last4: '4417' })
    const plan = instantiateScenario(driver, 'replace_payment_card', {
      targetPaymentInstrumentId: card.id,
      effectiveDate: '2026-10-01',
    })
    expect(plan.templateId).toBe('replace_payment_card')
    expect(plan.targetNodeId).toBe(card.id)
    expect(plan.workflowState).toBe('draft')
    expect(plan.baselineGraphRevision).toBe(plan.lastAnalyzedGraphRevision)
    expect(plan.actions.map((a) => a.phase)).toEqual(['prepare', 'change', 'verify'])
    expect(plan.actions.every((a) => !a.done)).toBe(true) // rebase/创建不自动完成动作
  })

  it('ST-004: 缺 required input → 明确报错；未知模板 → 报错', () => {
    expect(() => instantiateScenario(driver, 'replace_payment_card', {})).toThrowError(
      /missing required input/,
    )
    expect(() => instantiateScenario(driver, 'nonexistent', {})).toThrowError(/not found/)
  })

  it('ST-005: 政策 gate —— 注册表中不存在日常生活提醒类模板', () => {
    const forbidden = [
      'water',
      'birthday',
      'plant',
      'meeting',
      'exercise',
      'exam',
      'anniversary',
      'holiday',
    ]
    for (const t of [...SCENARIO_TEMPLATES, ...PLANNED_TEMPLATES]) {
      for (const word of forbidden) {
        expect(t.id).not.toContain(word)
      }
      // 每个模板必须声明数字基础设施相关的 capability
      expect(t.supportedCapabilities.length).toBeGreaterThan(0)
    }
  })
})

describe('Timeline（MVP03 TL）', () => {
  let dir: string
  let driver: NodeSqliteDriver

  beforeEach(() => {
    dir = mkdtempSync(join(tmpdir(), 'depmap-tl-'))
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
    migrate(driver)
  })

  afterEach(() => {
    driver.close()
    rmSync(dir, { recursive: true, force: true })
  })

  it('TL-001: 空 graph → 空 timeline（退役 legacy 来源后；非 calendar）', () => {
    // fresh v3 迁移含 legacy WeChat 来源（从未导入 → freshness_review 合理存在）；
    // 退役后 graph 为空 → timeline 为空
    const instances = new SourceInstanceRepository(driver)
    instances.retire('legacy-wechat-statement')
    expect(buildTimeline(driver, NOW)).toEqual([])
  })

  it('TL-002: 计划 effectiveDate 进桶（7d/overdue）+ stale 计划进 attention', () => {
    const nodes = new NodeRepository(driver)
    const card = nodes.create({ kind: 'payment_instrument', name: '招行 4417' })
    const soon = instantiateScenario(driver, 'replace_payment_card', {
      targetPaymentInstrumentId: card.id,
      effectiveDate: '2026-09-18', // NOW +5d → 7d 桶
    })
    void soon
    const overdue = instantiateScenario(driver, 'close_payment_instrument', {
      targetPaymentInstrumentId: card.id,
      effectiveDate: '2026-09-01', // NOW -12d → overdue
    })
    void overdue
    const items = buildTimeline(driver, NOW)
    const planItems = items.filter((i) => i.sourceType === 'change_plan')
    expect(planItems.map((i) => i.bucket).sort()).toEqual(['7d', 'overdue'].sort())

    // 制造 stale：新 Reality mutation（confirm 依赖）后 revision > lastAnalyzed
    const deps = new DependencyRepository(driver)
    const target = nodes.create({ kind: 'account', name: '微信支付' })
    deps.confirm({
      from: card.id,
      relation: 'funding_source',
      to: target.id,
      capability: 'payment',
    })
    const items2 = buildTimeline(driver, NOW)
    const staleItems = items2.filter((i) => i.status === 'needs_revalidation')
    expect(staleItems.length).toBe(2) // 两个未完成计划都 stale
    expect(staleItems.every((i) => i.bucket === 'attention')).toBe(true)
  })

  it('TL-003: open drift → attention；verification pending → verification_pending', () => {
    const nodes = new NodeRepository(driver)
    const cardA = nodes.create({ kind: 'payment_instrument', name: '招行 4417' }).id
    const cardB = nodes.create({ kind: 'payment_instrument', name: '建行 8821' }).id
    const wechat = nodes.create({
      kind: 'account',
      templateId: 'builtin.account.wechat',
      name: '微信支付',
    }).id
    const deps = new DependencyRepository(driver)
    deps.confirm({
      from: cardA,
      relation: 'funding_source',
      to: wechat,
      capability: 'payment',
      criticality: 'required',
    })
    const driftService = new RealityDriftService(driver)
    const { created } = driftService.detectFromEvidence({
      targetNodeId: wechat,
      capability: 'payment',
      signals: [{ fromNodeId: cardB, observations: 2, evidenceRef: 'i#1' }],
    })
    const items = buildTimeline(driver, NOW)
    const driftItem = items.find((i) => i.sourceType === 'reality_drift')
    expect(driftItem?.bucket).toBe('attention')
    expect(driftItem?.sourceId).toBe(created[0]!.id)
    expect(driftItem?.actionTarget).toBe(wechat)

    // verify 阶段动作（未 verified）→ verification_pending
    const plan = instantiateScenario(driver, 'replace_payment_card', {
      targetPaymentInstrumentId: cardA,
    })
    void plan
    const items2 = buildTimeline(driver, NOW)
    const verif = items2.filter((i) => i.kind === 'verification_pending')
    expect(verif.length).toBe(1)
    expect(verif[0]?.sourceType).toBe('action_verification')
    expect(verif[0]?.sourceId).toMatch(/\//)
  })

  it('TL-004: node expiry → expiration 桶；stale 来源 → freshness_review', () => {
    const nodes = new NodeRepository(driver)
    nodes.create({
      kind: 'payment_instrument',
      name: '招行 4417',
      fields: { expiryDate: '2026-10-05' }, // NOW +22d → 30d 桶
    })
    const instances = new SourceInstanceRepository(driver)
    const inst = instances.create({
      adapterId: 'generic_csv',
      adapterVersion: 1,
      sourceKind: 'statement_file',
      label: '旧账单来源',
    })
    instances.touchIngested(inst.id, '2026-01-01T00:00:00.000Z') // 远超 45 天
    const items = buildTimeline(driver, NOW)
    expect(items.find((i) => i.kind === 'expiration')?.bucket).toBe('30d')
    const fresh = items.find((i) => i.kind === 'freshness_review')
    expect(fresh?.bucket).toBe('attention')
    expect(fresh?.sourceId).toBe(inst.id)
  })

  it('TL-005: 排序 deterministic（attention → overdue → today → 7d → 30d → 90d → later；同桶 priority 降序，tie 用 scheduledAt+id）', () => {
    const nodes = new NodeRepository(driver)
    const card = nodes.create({ kind: 'payment_instrument', name: '招行 4417' })
    const service = new ChangePlanService(driver)
    void service
    for (const d of [
      '2026-01-01',
      '2026-09-13',
      '2026-09-15',
      '2026-09-25',
      '2026-10-20',
      '2027-06-01',
    ]) {
      instantiateScenario(driver, 'replace_payment_card', {
        targetPaymentInstrumentId: card.id,
        effectiveDate: d,
      })
    }
    const items = buildTimeline(driver, NOW)
    const rank = ['attention', 'overdue', 'today', '7d', '30d', '90d', 'later']
    const ranks = items.map((i) => rank.indexOf(i.bucket))
    expect([...ranks]).toEqual([...ranks].sort((a, b) => a - b))
    // 重复构建结果完全一致（projection 纯函数）
    expect(buildTimeline(driver, NOW)).toEqual(items)
  })

  it('TL-006: TimelineItem 可溯源且不是 Reality（投影不写库）', () => {
    const nodes = new NodeRepository(driver)
    const card = nodes.create({ kind: 'payment_instrument', name: '招行 4417' })
    instantiateScenario(driver, 'replace_payment_card', { targetPaymentInstrumentId: card.id })
    const items = buildTimeline(driver, NOW)
    for (const item of items) {
      expect(item.sourceId).toBeTruthy()
      expect([
        'change_plan',
        'reality_drift',
        'action_verification',
        'node_expiry',
        'source_freshness',
      ]).toContain(item.sourceType)
    }
    // 重建前后的 DB 无变化：再跑一次结果一致且 countAll 不变
    const plans = new ChangePlanService(driver)
    const countBefore = plans.plans.countAll()
    buildTimeline(driver, NOW)
    expect(plans.plans.countAll()).toBe(countBefore)
  })

  it('TL-007: completed/cancelled 计划不产生 timeline 项', () => {
    const nodes = new NodeRepository(driver)
    const card = nodes.create({ kind: 'payment_instrument', name: '招行 4417' })
    const plan = instantiateScenario(driver, 'replace_payment_card', {
      targetPaymentInstrumentId: card.id,
      effectiveDate: '2026-09-20',
    })
    const service = new ChangePlanService(driver)
    service.transition(plan.id, 'in_progress')
    service.transition(plan.id, 'completed')
    expect(buildTimeline(driver, NOW).filter((i) => i.sourceType === 'change_plan')).toEqual([])
  })
})

// 类型冒烟：PlanAction 导出可用
const _smoke: PlanAction | null = null
void _smoke
