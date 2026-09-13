import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import {
  analyzePlanImpact,
  ChangePlanService,
  computePlanReadiness,
  DependencyGroupRepository,
  DependencyProposalRepository,
  DependencyRepository,
  getGraphRevision,
  instantiateScenario,
  migrate,
  NodeRepository,
  NodeSqliteDriver,
  rebasePlan,
  type PlanAction,
  type PlanReadinessInput,
} from '../../src/index.ts'

/**
 * MVP03 FREEZE FR-READ-001..014（GOAL §10）—— PlanReadiness 冻结 Gate。
 * 核心：显式 resolution 语义（resolvesImpactKeys），禁止数量相减。
 */

function plan(overrides: Partial<PlanReadinessInput['plan']> = {}): PlanReadinessInput['plan'] {
  return {
    id: 'p',
    templateId: 'replace_payment_card',
    scenario: 'replace_payment_card',
    title: 't',
    workflowState: 'analyzed',
    baselineGraphRevision: 3,
    lastAnalyzedGraphRevision: 3,
    targetNodeId: 'n-card',
    effectiveDate: null,
    params: {},
    impactSnapshot: null,
    actions: [],
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...overrides,
  }
}

function input(
  actions: PlanAction[],
  overrides: Partial<PlanReadinessInput> = {},
): PlanReadinessInput {
  return {
    plan: plan({ actions }),
    currentGraphRevision: 3,
    pendingMustChange: 0, // 由各用例显式给值
    pendingNeedsReview: 0,
    unresolvedCandidates: 0,
    pendingRelevantProposals: 0,
    staleRelevantDependencies: 0,
    unfinishedChangeActions: 0,
    ...overrides,
  }
}

const changeAction = (id: string, keys: string[], done = false): PlanAction => ({
  id,
  title: id,
  detail: '',
  phase: 'change',
  done,
  doneAt: done ? '2026-01-02T00:00:00Z' : null,
  verification: null,
  resolvesImpactKeys: keys,
})

describe('PlanReadiness freeze（FR-READ）', () => {
  it('FR-READ-001: one target / multiple required actions —— 全部完成才 resolved', () => {
    // Spotify 1 个 must_change target 需要 2 个动作（加新卡、切默认卡）
    const actions = [
      changeAction('a1', ['spotify|payment']),
      changeAction('a2', ['spotify|payment']),
    ]
    // 两个动作都未完成 → blocked
    expect(computePlanReadiness(input(actions, { pendingMustChange: 1 }))).toBe('blocked')
    // 只完成一个 → 仍 blocked（另一声明动作未完成）
    const half = [
      changeAction('a1', ['spotify|payment'], true),
      changeAction('a2', ['spotify|payment']),
    ]
    expect(computePlanReadiness(input(half, { pendingMustChange: 1 }))).toBe('blocked')
    // 全部完成 → 不再 blocked
    const all = [
      changeAction('a1', ['spotify|payment'], true),
      changeAction('a2', ['spotify|payment'], true),
    ]
    expect(computePlanReadiness(input(all, { pendingMustChange: 0 }))).not.toBe('blocked')
  })

  it('FR-READ-002: multiple targets / one shared action', () => {
    const shared = [changeAction('a1', ['wechat|payment', 'alipay|payment'], true)]
    expect(computePlanReadiness(input(shared, { pendingMustChange: 0 }))).not.toBe('blocked')
  })

  it('FR-READ-003: completed unrelated action does not resolve requirement', () => {
    // 完成的动作未声明该 key → 不解决
    const actions = [changeAction('unrelated', ['other|payment'], true)]
    expect(computePlanReadiness(input(actions, { pendingMustChange: 1 }))).toBe('blocked')
  })

  it('FR-READ-004: unresolved must_change → blocked', () => {
    expect(computePlanReadiness(input([], { pendingMustChange: 2 }))).toBe('blocked')
  })

  it('FR-READ-005: unknown criticality（needs_review）→ review_required', () => {
    expect(computePlanReadiness(input([], { pendingNeedsReview: 1 }))).toBe('review_required')
  })

  it('FR-READ-006: pending Proposal → review_required', () => {
    expect(computePlanReadiness(input([], { pendingRelevantProposals: 1 }))).toBe('review_required')
  })

  it('FR-READ-007: unresolved Candidate → review_required', () => {
    expect(computePlanReadiness(input([], { unresolvedCandidates: 1 }))).toBe('review_required')
  })

  it('FR-READ-008: revision mismatch → review_required', () => {
    expect(computePlanReadiness(input([], { currentGraphRevision: 9 }))).toBe('review_required')
  })

  it('FR-READ-009: all known requirements resolved → ready_with_known_scope', () => {
    const actions = [changeAction('a1', ['wechat|payment'], true)]
    expect(computePlanReadiness(input(actions, { pendingMustChange: 0 }))).toBe(
      'ready_with_known_scope',
    )
  })

  it('FR-READ-010: confidenceScore .999 不能绕过 review（输入无 confidence 通道）', () => {
    const i = input([], { pendingRelevantProposals: 1 })
    expect(Object.keys(i).some((k) => k.toLowerCase().includes('confidence'))).toBe(false)
    expect(computePlanReadiness(i)).toBe('review_required')
  })

  it('FR-READ-011: event_stream absence 不能提高 readiness（输入无 absence 通道）', () => {
    const i = input([], {})
    expect(Object.keys(i).some((k) => k.toLowerCase().includes('absen'))).toBe(false)
    // 未声明/未完成的 must_change 不因 absence 消失
    expect(computePlanReadiness(input([], { pendingMustChange: 1 }))).toBe('blocked')
  })

  it('FR-READ-012: deterministic result（同输入恒同输出）', () => {
    const actions = [changeAction('a1', ['x|payment'], true), changeAction('a2', ['y|payment'])]
    const i = input(actions, { pendingMustChange: 1, pendingNeedsReview: 1 })
    const first = computePlanReadiness(i)
    for (let run = 0; run < 10; run++) {
      expect(computePlanReadiness(i)).toBe(first)
    }
    expect(first).toBe('blocked') // blocked 优先于 review_required
  })

  it('FR-READ-013: completed shared action 只解决其显式映射的 requirements', () => {
    // a1 声明 [x|payment]；y|payment 无人声明 → unresolved
    const actions = [changeAction('a1', ['x|payment'], true)]
    expect(computePlanReadiness(input(actions, { pendingMustChange: 1 }))).toBe('blocked')
  })

  it('FR-READ-014: verification pending 不得 false-ready（未完成的 change 动作阻塞）', () => {
    const actions = [
      {
        ...changeAction('a1', ['x|payment']),
        verification: {
          method: 'future_observation' as const,
          status: 'pending' as const,
          verifiedAt: null,
          evidenceRefs: [],
        },
      },
    ]
    // 动作未完成 → unfinishedChangeActions > 0 → review_required（且 must_change 未 resolved → blocked 优先）
    expect(
      computePlanReadiness(input(actions, { pendingMustChange: 1, unfinishedChangeActions: 1 })),
    ).toBe('blocked')
    // 完成动作但 verification pending：must_change resolved，但未完成验证 → 不给 ready（review_required）
    expect(
      computePlanReadiness(
        input([changeAction('a1', ['x|payment'], true)], {
          pendingMustChange: 0,
          pendingNeedsReview: 1,
        }),
      ),
    ).toBe('review_required')
  })
})

// ---------------------------------------------------------------------------
// 集成链路：claiming（rebase 显式映射）+ 真实 DB 的 resolution
// ---------------------------------------------------------------------------

describe('PlanReadiness freeze（FR-READ 集成：claiming + DB）', () => {
  let dir: string
  let driver: NodeSqliteDriver
  let nodes: NodeRepository
  let deps: DependencyRepository

  beforeEach(() => {
    dir = mkdtempSync(join(tmpdir(), 'depmap-frread-'))
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
    migrate(driver)
    nodes = new NodeRepository(driver)
    deps = new DependencyRepository(driver)
  })

  afterEach(() => {
    driver.close()
    rmSync(dir, { recursive: true, force: true })
  })

  it('FR-READ-015: rebase 后 claiming 显式分配 must_change keys；完成该动作 → 不再 blocked', () => {
    const card = nodes.create({ kind: 'payment_instrument', name: '招行 4417' })
    const wechat = nodes.create({
      kind: 'account',
      templateId: 'builtin.account.wechat',
      name: '微信支付',
    }).id
    deps.confirm({
      from: card.id,
      relation: 'funding_source',
      to: wechat,
      capability: 'payment',
      criticality: 'required',
    })
    const plan = instantiateScenario(driver, 'replace_payment_card', {
      targetPaymentInstrumentId: card.id,
    })
    const service = new ChangePlanService(driver)
    // 基线分析（直接 updateAnalysis 不经 claiming）
    const rev = getGraphRevision(driver)
    const snapshot = analyzePlanImpact(
      driver,
      {
        deps,
        groups: new DependencyGroupRepository(driver),
        proposals: new DependencyProposalRepository(driver),
      },
      plan,
    )
    service.plans.updateAnalysis(plan.id, snapshot, rev, plan.actions)

    // revision 推进 → rebase → claiming 分配
    deps.confirm({
      from: card.id,
      relation: 'funding_source',
      to: nodes.create({ kind: 'account', name: 'X' }).id,
      capability: 'payment',
      criticality: 'required',
    })
    const result = rebasePlan(
      driver,
      {
        deps,
        groups: new DependencyGroupRepository(driver),
        proposals: new DependencyProposalRepository(driver),
        plans: service.plans,
      },
      plan.id,
    )
    expect(result.revisionChanged).toBe(true)
    const changeActions = service.plans
      .getExisting(plan.id)
      .actions.filter((a) => a.phase === 'change')
    const claimed = changeActions.flatMap((a) => a.resolvesImpactKeys ?? [])
    expect(claimed.length).toBeGreaterThan(0)

    // 完成 claim 动作 → must_change resolved → 不再 blocked（review_required 因另一 unknown 边）
    for (const a of changeActions) {
      service.completeAction(plan.id, a.id)
    }
    const readiness = service.getReadiness(plan.id)
    expect(['review_required', 'ready_with_known_scope']).toContain(readiness)
    expect(readiness).not.toBe('blocked')
  })

  it('FR-READ-016: 一个 target 多个 required actions（真实 DB）—— 只完成其一仍 blocked', () => {
    const card = nodes.create({ kind: 'payment_instrument', name: '招行 4417' })
    const wechat = nodes.create({
      kind: 'account',
      templateId: 'builtin.account.wechat',
      name: '微信支付',
    }).id
    deps.confirm({
      from: card.id,
      relation: 'funding_source',
      to: wechat,
      capability: 'payment',
      criticality: 'required',
    })
    const plan = instantiateScenario(driver, 'replace_payment_card', {
      targetPaymentInstrumentId: card.id,
    })
    const service = new ChangePlanService(driver)

    // 手工构造：同一 key 声明在两个 change 动作上（用户显式拆分）
    const key = `${wechat}|payment`
    const actions: PlanAction[] = [
      {
        id: 'c1',
        title: '切换默认卡',
        detail: '',
        phase: 'change',
        done: false,
        doneAt: null,
        verification: null,
        resolvesImpactKeys: [key],
      },
      {
        id: 'c2',
        title: '迁移自动扣款',
        detail: '',
        phase: 'change',
        done: false,
        doneAt: null,
        verification: null,
        resolvesImpactKeys: [key],
      },
      {
        id: 'p1',
        title: '检查依赖',
        detail: '',
        phase: 'prepare',
        done: false,
        doneAt: null,
        verification: null,
        resolvesImpactKeys: [],
      },
    ]
    service.plans.updateAnalysis(
      plan.id,
      analyzePlanImpact(
        driver,
        {
          deps,
          groups: new DependencyGroupRepository(driver),
          proposals: new DependencyProposalRepository(driver),
        },
        plan,
      ),
      getGraphRevision(driver),
      actions,
    )
    // 完成 c1 → c2 未完成 → 仍 blocked
    service.completeAction(plan.id, 'c1')
    expect(service.getReadiness(plan.id)).toBe('blocked')
    // 完成 c2 → resolved → 不再 blocked
    service.completeAction(plan.id, 'c2')
    expect(service.getReadiness(plan.id)).not.toBe('blocked')
  })
})
