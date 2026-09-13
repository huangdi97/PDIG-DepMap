import type {
  PlanReadiness,
  PlanReadinessInput,
  ScenarioCoverage,
  ScenarioCoverageInput,
} from '../domain/change-plan.ts'
import { isPlanStale } from '../domain/change-plan.ts'

/**
 * PlanReadiness / ScenarioCoverage —— 纯确定性规则引擎（MVP03 §17–§23）。
 *
 * 禁止：LLM 判断、confidence→readiness 映射、source 数量→百分比、absence 提升结论。
 * 输入中根本不存在 confidence 与 absence 通道（结构性保证，而非运行时检查）。
 */

export function computePlanReadiness(input: PlanReadinessInput): PlanReadiness {
  const { plan, currentGraphRevision } = input

  // 1. blocked：存在未处理的 must_change（当前影响）
  if (input.pendingMustChange > 0) return 'blocked'

  // 2. review_required：任何需要人工确认的信号
  if (isPlanStale(plan, currentGraphRevision)) return 'review_required' // needs_revalidation
  if (input.pendingNeedsReview > 0) return 'review_required'
  if (input.unresolvedCandidates > 0) return 'review_required'
  if (input.pendingRelevantProposals > 0) return 'review_required' // confidence 不参与判断
  if (input.staleRelevantDependencies > 0) return 'review_required'
  if (input.unfinishedChangeActions > 0) return 'review_required'

  // 3. ready_with_known_scope：已知范围内全部处理完毕（仍需展示免责声明）
  return 'ready_with_known_scope'
}

function daysBetween(fromIso: string | null, toIso: string): number {
  if (!fromIso) return Number.POSITIVE_INFINITY
  const from = Date.parse(fromIso)
  const to = Date.parse(toIso)
  if (Number.isNaN(from) || Number.isNaN(to)) return Number.POSITIVE_INFINITY
  return (to - from) / 86_400_000
}

export function computeScenarioCoverage(input: ScenarioCoverageInput): ScenarioCoverage {
  const explanations: string[] = []
  const counts = {
    confirmedDirectDependencies: input.confirmedDirectDependencies,
    confirmedIndirectDependencies: input.confirmedIndirectDependencies,
    pendingProposals: input.pendingProposals,
    unresolvedCandidates: input.unresolvedCandidates,
    staleDependencies: input.staleDependencies,
    unknownCriticalityCount: input.unknownCriticalityCount,
    unverifiedActions: input.unverifiedActions,
  }

  const freshSources = input.sources.filter(
    (s) => daysBetween(s.lastIngestedAt, input.now) <= input.freshnessThresholdDays,
  )
  const staleSources = input.sources.filter(
    (s) => daysBetween(s.lastIngestedAt, input.now) > input.freshnessThresholdDays,
  )

  // unknown：无来源且无已确认直接依赖 —— 系统对本次变更几乎一无所知
  if (input.sources.length === 0 && input.confirmedDirectDependencies === 0) {
    explanations.push('没有导入过任何相关来源，也没有已确认的直接依赖。')
    return {
      scenarioId: input.scenarioId,
      coverageLevel: 'unknown',
      explanations,
      counts,
    }
  }

  // limited：有来源但全部过期，或没有任何已确认直接依赖
  if (input.sources.length > 0 && freshSources.length === 0) {
    explanations.push(
      `已有 ${staleSources.length} 个来源，但都超过新鲜度阈值（${input.freshnessThresholdDays} 天）未刷新。`,
    )
  }
  if (input.confirmedDirectDependencies === 0) {
    explanations.push('尚无已确认的直接依赖（目标对象没有 confirmed 支付关系）。')
  }
  if (explanations.length > 0) {
    return {
      scenarioId: input.scenarioId,
      coverageLevel: 'limited',
      explanations,
      counts,
    }
  }

  // well_evidenced 前置检查：存在任何未决信号则降级为 partial（可解释）
  let partial = false
  if (input.pendingProposals > 0) {
    explanations.push(`${input.pendingProposals} 个相关 Proposal 待确认（未确认不当作事实）。`)
    partial = true
  }
  if (input.unresolvedCandidates > 0) {
    explanations.push(`${input.unresolvedCandidates} 个发现对象未解析（不进入依赖图）。`)
    partial = true
  }
  if (input.staleDependencies > 0) {
    explanations.push(`${input.staleDependencies} 条相关依赖长期未验证。`)
    partial = true
  }
  if (input.unknownCriticalityCount > 0) {
    explanations.push(`${input.unknownCriticalityCount} 条依赖 criticality=unknown（是否必需未确认）。`)
    partial = true
  }
  if (input.unverifiedActions > 0) {
    explanations.push(`${input.unverifiedActions} 个动作已完成但尚未验证。`)
    partial = true
  }

  if (partial) {
    return {
      scenarioId: input.scenarioId,
      coverageLevel: 'partial',
      explanations,
      counts,
    }
  }

  explanations.push(
    `${freshSources.length} 个来源在新鲜度阈值内；${input.confirmedDirectDependencies} 条直接依赖已确认，无未决 Proposal / 候选 / unknown criticality。`,
  )
  return {
    scenarioId: input.scenarioId,
    coverageLevel: 'well_evidenced',
    explanations,
    counts,
  }
}
