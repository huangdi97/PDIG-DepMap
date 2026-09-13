import { describe, expect, it } from 'vitest'
import fc from 'fast-check'
import {
  computePlanReadiness,
  isPlanStale,
  type ChangePlan,
  type PlanReadinessInput,
} from '../../src/index.ts'

/**
 * MVP03 PI-property（§59）—— 纯规则层的结构性质（fast-check）。
 * DB 层性质（revision 单调 / drift 不写 Reality / rebase deterministic）在
 * tests/invariants/mvp03-invariants.test.ts 用真实 DB 断言。
 */
fc.configureGlobal({ seed: 20260913 })

function plan(lastAnalyzed: number): ChangePlan {
  return {
    id: 'p',
    templateId: 'replace_payment_card',
    scenario: 'replace_payment_card',
    title: 't',
    workflowState: 'ready',
    baselineGraphRevision: lastAnalyzed,
    lastAnalyzedGraphRevision: lastAnalyzed,
    targetNodeId: 'n',
    effectiveDate: null,
    params: {},
    impactSnapshot: null,
    actions: [],
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  }
}

function input(
  lastAnalyzed: number,
  current: number,
  rest: Partial<PlanReadinessInput> = {},
): PlanReadinessInput {
  return {
    plan: plan(lastAnalyzed),
    currentGraphRevision: current,
    pendingMustChange: 0,
    pendingNeedsReview: 0,
    unresolvedCandidates: 0,
    pendingRelevantProposals: 0,
    staleRelevantDependencies: 0,
    unfinishedChangeActions: 0,
    ...rest,
  }
}

describe('MVP03 property invariants（fast-check）', () => {
  it('PI-1: revision mismatch ⇒ 未完成计划永远不能返回 ready_with_known_scope', () => {
    fc.assert(
      fc.property(
        fc.nat({ max: 50 }),
        fc.nat({ max: 50 }),
        fc.constantFrom(
          'draft',
          'analyzed',
          'review_required',
          'ready',
          'in_progress',
          'verifying',
        ),
        (analyzed, delta, state) => {
          const current = analyzed + delta + 1 // 恒 mismatch
          const p = { ...plan(analyzed), workflowState: state as ChangePlan['workflowState'] }
          const readiness = computePlanReadiness({
            plan: p,
            currentGraphRevision: current,
            pendingMustChange: 0,
            pendingNeedsReview: 0,
            unresolvedCandidates: 0,
            pendingRelevantProposals: 0,
            staleRelevantDependencies: 0,
            unfinishedChangeActions: 0,
          })
          expect(readiness).not.toBe('ready_with_known_scope')
        },
      ),
      { numRuns: 200 },
    )
  })

  it('PI-2: isPlanStale ↔ 严格「未终态且 current > lastAnalyzed」', () => {
    fc.assert(
      fc.property(
        fc.nat({ max: 50 }),
        fc.nat({ max: 50 }),
        fc.constantFrom(
          'draft',
          'analyzed',
          'review_required',
          'ready',
          'in_progress',
          'verifying',
          'completed',
          'cancelled',
        ),
        (analyzed, delta, state) => {
          const current = analyzed + delta
          const p = { ...plan(analyzed), workflowState: state }
          const terminal = state === 'completed' || state === 'cancelled'
          expect(isPlanStale(p, current)).toBe(!terminal && delta > 0)
        },
      ),
      { numRuns: 200 },
    )
  })

  it('PI-3: readiness 输入通道中不存在 confidence / absence（结构性禁止）', () => {
    fc.assert(
      fc.property(fc.nat({ max: 5 }), fc.nat({ max: 5 }), (a, b) => {
        const readinessInput = input(a, b)
        const keys = Object.keys(readinessInput)
        expect(keys.some((k) => k.toLowerCase().includes('confidence'))).toBe(false)
        expect(keys.some((k) => k.toLowerCase().includes('absen'))).toBe(false)
      }),
      { numRuns: 100 },
    )
  })
})
