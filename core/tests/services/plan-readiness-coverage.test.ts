import { describe, expect, it } from 'vitest'
import {
  computePlanReadiness,
  computeScenarioCoverage,
  type ChangePlan,
  type PlanReadinessInput,
  type ScenarioCoverageInput,
} from '../../src/index.ts'

/**
 * MVP03 §20 / §24 —— PlanReadiness 与 ScenarioCoverage 纯规则 Gate。
 */

function basePlan(overrides: Partial<ChangePlan> = {}): ChangePlan {
  return {
    id: 'plan-1',
    templateId: 'replace_payment_card',
    scenario: 'replace_payment_card',
    title: '更换银行卡',
    workflowState: 'analyzed',
    baselineGraphRevision: 3,
    lastAnalyzedGraphRevision: 3,
    targetNodeId: 'n-card',
    effectiveDate: null,
    params: {},
    impactSnapshot: null,
    actions: [],
    createdAt: '2026-09-13T00:00:00Z',
    updatedAt: '2026-09-13T00:00:00Z',
    ...overrides,
  }
}

function readinessInput(overrides: Partial<PlanReadinessInput> = {}): PlanReadinessInput {
  return {
    plan: basePlan(),
    currentGraphRevision: 3,
    pendingMustChange: 0,
    pendingNeedsReview: 0,
    unresolvedCandidates: 0,
    pendingRelevantProposals: 0,
    staleRelevantDependencies: 0,
    unfinishedChangeActions: 0,
    ...overrides,
  }
}

describe('PlanReadiness（纯规则）', () => {
  it('must_change pending → blocked', () => {
    expect(computePlanReadiness(readinessInput({ pendingMustChange: 1 }))).toBe('blocked')
  })

  it('must_change done + needs_review 存在 → review_required', () => {
    expect(computePlanReadiness(readinessInput({ pendingNeedsReview: 2 }))).toBe('review_required')
  })

  it('unknown criticality → review_required', () => {
    // unknown criticality 体现为 pendingNeedsReview（criticality_unknown reasonCode）
    expect(computePlanReadiness(readinessInput({ pendingNeedsReview: 1 }))).toBe('review_required')
  })

  it('unresolved candidate → review_required', () => {
    expect(computePlanReadiness(readinessInput({ unresolvedCandidates: 1 }))).toBe(
      'review_required',
    )
  })

  it('revision mismatch（needs_revalidation）→ review_required', () => {
    expect(
      computePlanReadiness(readinessInput({ currentGraphRevision: 5 })), // plan lastAnalyzed=3
    ).toBe('review_required')
  })

  it('全部已知要求处理完毕 → ready_with_known_scope', () => {
    expect(computePlanReadiness(readinessInput({}))).toBe('ready_with_known_scope')
  })

  it('unfinished change action → review_required', () => {
    expect(computePlanReadiness(readinessInput({ unfinishedChangeActions: 1 }))).toBe(
      'review_required',
    )
  })

  it('Proposal confidence .999 也不得绕过 review（输入通道不存在 confidence）', () => {
    // 结构性：readiness 输入只有 pendingRelevantProposals 计数，无 confidence 字段
    const input = readinessInput({ pendingRelevantProposals: 1 })
    expect('confidenceScore' in input).toBe(false)
    expect(computePlanReadiness(input)).toBe('review_required')
  })

  it('absence 不得提高 readiness（输入通道不存在 absence）', () => {
    // 结构性：没有「本月未见交易」之类的输入；ready 只能由正对照义务全部满足达成
    const input = readinessInput({})
    expect(Object.keys(input).some((k) => k.toLowerCase().includes('absen'))).toBe(false)
    // 即使零信号，也只给 ready_with_known_scope（带免责声明），绝不是 safe/all-clear
    expect(computePlanReadiness(input)).toBe('ready_with_known_scope')
  })

  it('stale relevant dependency → review_required', () => {
    expect(computePlanReadiness(readinessInput({ staleRelevantDependencies: 1 }))).toBe(
      'review_required',
    )
  })
})

describe('ScenarioCoverage（信息覆盖，不是安全评分）', () => {
  const NOW = '2026-09-13T00:00:00Z'
  function covInput(overrides: Partial<ScenarioCoverageInput> = {}): ScenarioCoverageInput {
    return {
      scenarioId: 'replace_payment_card',
      sources: [],
      confirmedDirectDependencies: 0,
      confirmedIndirectDependencies: 0,
      pendingProposals: 0,
      unresolvedCandidates: 0,
      staleDependencies: 0,
      unknownCriticalityCount: 0,
      unverifiedActions: 0,
      freshnessThresholdDays: 45,
      now: NOW,
      ...overrides,
    }
  }

  it('无来源 → unknown', () => {
    const c = computeScenarioCoverage(covInput())
    expect(c.coverageLevel).toBe('unknown')
    expect(c.explanations.length).toBeGreaterThan(0)
  })

  it('只有旧来源 → limited', () => {
    const c = computeScenarioCoverage(
      covInput({
        sources: [{ id: 's1', label: 'Generic CSV', lastIngestedAt: '2026-01-01T00:00:00Z' }],
      }),
    )
    expect(c.coverageLevel).toBe('limited')
    expect(c.explanations.join(' ')).toContain('新鲜度')
  })

  it('部分确认（有新来源但有 pending proposal / unknown）→ partial，且可解释', () => {
    const c = computeScenarioCoverage(
      covInput({
        sources: [{ id: 's1', label: 'Generic CSV', lastIngestedAt: '2026-09-10T00:00:00Z' }],
        confirmedDirectDependencies: 3,
        pendingProposals: 2,
        unknownCriticalityCount: 1,
      }),
    )
    expect(c.coverageLevel).toBe('partial')
    expect(c.explanations.join(' ')).toContain('2 个相关 Proposal')
    expect(c.explanations.join(' ')).toContain('unknown')
  })

  it('新来源 + 关键关系全部确认 → well_evidenced', () => {
    const c = computeScenarioCoverage(
      covInput({
        sources: [
          { id: 's1', label: 'Generic CSV', lastIngestedAt: '2026-09-12T00:00:00Z' },
          { id: 's2', label: 'OFX', lastIngestedAt: '2026-09-11T00:00:00Z' },
        ],
        confirmedDirectDependencies: 5,
        confirmedIndirectDependencies: 2,
      }),
    )
    expect(c.coverageLevel).toBe('well_evidenced')
  })

  it('coverage 不等于 ready（well_evidenced 也不产生 safe 语义）', () => {
    const c = computeScenarioCoverage(
      covInput({
        sources: [{ id: 's1', label: 'Generic CSV', lastIngestedAt: '2026-09-12T00:00:00Z' }],
        confirmedDirectDependencies: 5,
      }),
    )
    expect(c.coverageLevel).toBe('well_evidenced')
    expect(['safe', 'complete', '100%']).not.toContain(c.coverageLevel)
  })

  it('新增 pending Proposal → 进入 partial 并解释 pending', () => {
    const base = covInput({
      sources: [{ id: 's1', label: 'Generic CSV', lastIngestedAt: '2026-09-12T00:00:00Z' }],
      confirmedDirectDependencies: 5,
    })
    expect(computeScenarioCoverage(base).coverageLevel).toBe('well_evidenced')
    const withPending = computeScenarioCoverage({ ...base, pendingProposals: 1 })
    expect(withPending.coverageLevel).toBe('partial')
  })

  it('event_stream absence 不得提高 coverage（输入无 absence 通道；缺证据只会降级）', () => {
    const withEvidence = covInput({
      sources: [{ id: 's1', label: 'Generic CSV', lastIngestedAt: '2026-09-12T00:00:00Z' }],
      confirmedDirectDependencies: 5,
    })
    expect(computeScenarioCoverage(withEvidence).coverageLevel).toBe('well_evidenced')
    // 同一场景「缺少证据」（来源变陈旧 / 依赖未确认）→ 只会降级，绝不出现 absence → 更高覆盖
    const missingFreshness = computeScenarioCoverage({
      ...withEvidence,
      sources: [{ id: 's1', label: 'Generic CSV', lastIngestedAt: '2026-01-01T00:00:00Z' }],
    })
    expect(missingFreshness.coverageLevel).toBe('limited')
    const input = covInput({})
    expect(Object.keys(input).some((k) => k.toLowerCase().includes('absen'))).toBe(false)
  })
})
