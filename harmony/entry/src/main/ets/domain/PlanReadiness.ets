// DO NOT EDIT 之外的语义 — 语义必须与 TS / Android 冻结基准一致
// 端口自 core/src/services/plan-readiness.ts 的 computePlanReadiness 部分
// （MVP03 §17–§19）。
//
// 分层约束：只依赖 `./Entities` 与 `../generated/CanonicalEnums`。
//
// 禁止（原文照录，且输入里根本不存在这些通道，属结构性保证）：
//   LLM 判断、confidence→readiness 映射、source 数量→百分比、absence 提升结论。
//
// 设计说明：本模块是**纯规则**，没有第 4 个 readiness 取值。
// 不存在 'safe' / '100%' / 'all clear' —— 最乐观也只是
// ready_with_known_scope，且该档位仍要求 UI 展示免责声明。
//
// ScenarioCoverage 已拆到 `./ScenarioCoverage`（两者是不同规则族：
// 一个判定"计划是否可执行"，一个描述"信息覆盖程度"）。

import { PlanReadiness as PlanReadinessEnum } from '../generated/CanonicalEnums';
import { PlanReadinessInput } from './Entities';

export function computePlanReadiness(input: PlanReadinessInput): string {
  const plan = input.plan;
  const currentGraphRevision = input.currentGraphRevision;

  // 1. blocked：存在未处理的 must_change（当前影响）
  if (input.pendingMustChange > 0) {
    return PlanReadinessEnum.BLOCKED;
  }

  // 2. review_required：任何需要人工确认的信号
  //
  // 顺序与原文一致。注意第一项是 stale 判定（派生 needs_revalidation），
  // 它不写回存储状态 —— 避免每次 Reality 变更都扫描并回写全部计划。
  if (isPlanStale(plan.workflowState, plan.lastAnalyzedGraphRevision, currentGraphRevision)) {
    return PlanReadinessEnum.REVIEW_REQUIRED;
  }
  if (input.pendingNeedsReview > 0) {
    return PlanReadinessEnum.REVIEW_REQUIRED;
  }
  if (input.unresolvedCandidates > 0) {
    return PlanReadinessEnum.REVIEW_REQUIRED;
  }
  // confidence 不参与判断：此处只看"有没有未决 proposal"，不看它多可信。
  if (input.pendingRelevantProposals > 0) {
    return PlanReadinessEnum.REVIEW_REQUIRED;
  }
  if (input.staleRelevantDependencies > 0) {
    return PlanReadinessEnum.REVIEW_REQUIRED;
  }
  if (input.unfinishedChangeActions > 0) {
    return PlanReadinessEnum.REVIEW_REQUIRED;
  }

  // 3. ready_with_known_scope：已知范围内全部处理完毕（仍需展示免责声明）
  return PlanReadinessEnum.READY_WITH_KNOWN_SCOPE;
}

/**
 * stale 判定：计划未完成/未取消，且当前 revision 已超过它最后一次分析的 revision。
 *
 * 与 core/src/domain/change-plan.ts 的 isPlanStale 同口径。
 * 提取为独立函数是因为 UI 与 readiness 必须共用同一判定 ——
 * 若两处各自实现，会出现"界面说需重校验、readiness 说 ready"的矛盾。
 */
export function isPlanStale(
  workflowState: string,
  lastAnalyzedGraphRevision: number,
  currentGraphRevision: number,
): boolean {
  if (workflowState === 'completed' || workflowState === 'cancelled') {
    return false;
  }
  return currentGraphRevision > lastAnalyzedGraphRevision;
}

/**
 * 两个 ISO 时间之间的天数。
 *
 * 不可解析或缺失一律返回 +Infinity —— 也就是"最陈旧"。
 * 这是 fail-closed 方向：宁可把来源判成过期（触发 review），
 * 也不能把时钟异常当作"很新鲜"而放过。
 */
export function daysBetween(fromIso: string | null, toIso: string): number {
  if (fromIso === null || fromIso.length === 0) {
    return Number.POSITIVE_INFINITY;
  }
  const from = Date.parse(fromIso);
  const to = Date.parse(toIso);
  if (Number.isNaN(from) || Number.isNaN(to)) {
    return Number.POSITIVE_INFINITY;
  }
  return (to - from) / 86400000;
}

