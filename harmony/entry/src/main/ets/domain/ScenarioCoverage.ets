// DO NOT EDIT 之外的语义 — 语义必须与 TS / Android 冻结基准一致
// 端口自 core/src/services/plan-readiness.ts 的 computeScenarioCoverage 部分
// （MVP03 §21–§23）。
//
// 分层约束：只依赖 `./Entities`、`./PlanReadiness` 与 `../generated/CanonicalEnums`。
//
// 语义定位（原文照录）：ScenarioCoverage 是**信息覆盖描述，不是安全概率**。
// 禁止：source 数量 → 百分比、absence 提升结论、confidence 参与分级。
// 每一档都必须给出可解释的 explanations（§23），不允许只返回一个等级。

import { CoverageLevel } from '../generated/CanonicalEnums';
import { CoverageCounts, ScenarioCoverage, ScenarioCoverageInput } from './Entities';
import { daysBetween } from './PlanReadiness';

export function computeScenarioCoverage(input: ScenarioCoverageInput): ScenarioCoverage {
  const explanations: string[] = [];
  const counts = new CoverageCounts();
  counts.confirmedDirectDependencies = input.confirmedDirectDependencies;
  counts.confirmedIndirectDependencies = input.confirmedIndirectDependencies;
  counts.pendingProposals = input.pendingProposals;
  counts.unresolvedCandidates = input.unresolvedCandidates;
  counts.staleDependencies = input.staleDependencies;
  counts.unknownCriticalityCount = input.unknownCriticalityCount;
  counts.unverifiedActions = input.unverifiedActions;

  let freshSourceCount = 0;
  let staleSourceCount = 0;
  for (let i = 0; i < input.sources.length; i++) {
    const s = input.sources[i];
    if (daysBetween(s.lastIngestedAt, input.now) <= input.freshnessThresholdDays) {
      freshSourceCount += 1;
    } else {
      staleSourceCount += 1;
    }
  }

  // unknown：无来源且无已确认直接依赖 —— 系统对本次变更几乎一无所知
  if (input.sources.length === 0 && input.confirmedDirectDependencies === 0) {
    explanations.push('没有导入过任何相关来源，也没有已确认的直接依赖。');
    return makeCoverage(input.scenarioId, CoverageLevel.UNKNOWN, explanations, counts);
  }

  // limited：有来源但全部过期，或没有任何已确认直接依赖
  if (input.sources.length > 0 && freshSourceCount === 0) {
    explanations.push(
      `已有 ${staleSourceCount} 个来源，但都超过新鲜度阈值（${input.freshnessThresholdDays} 天）未刷新。`,
    );
  }
  if (input.confirmedDirectDependencies === 0) {
    explanations.push('尚无已确认的直接依赖（目标对象没有 confirmed 支付关系）。');
  }
  if (explanations.length > 0) {
    return makeCoverage(input.scenarioId, CoverageLevel.LIMITED, explanations, counts);
  }

  // well_evidenced 前置检查：存在任何未决信号则降级为 partial（可解释）
  let partial = false;
  if (input.pendingProposals > 0) {
    explanations.push(`${input.pendingProposals} 个相关 Proposal 待确认（未确认不当作事实）。`);
    partial = true;
  }
  if (input.unresolvedCandidates > 0) {
    explanations.push(`${input.unresolvedCandidates} 个发现对象未解析（不进入依赖图）。`);
    partial = true;
  }
  if (input.staleDependencies > 0) {
    explanations.push(`${input.staleDependencies} 条相关依赖长期未验证。`);
    partial = true;
  }
  if (input.unknownCriticalityCount > 0) {
    explanations.push(
      `${input.unknownCriticalityCount} 条依赖 criticality=unknown（是否必需未确认）。`,
    );
    partial = true;
  }
  if (input.unverifiedActions > 0) {
    explanations.push(`${input.unverifiedActions} 个动作已完成但尚未验证。`);
    partial = true;
  }

  if (partial) {
    return makeCoverage(input.scenarioId, CoverageLevel.PARTIAL, explanations, counts);
  }

  explanations.push(
    `${freshSourceCount} 个来源在新鲜度阈值内；${input.confirmedDirectDependencies} 条直接依赖已确认，无未决 Proposal / 候选 / unknown criticality。`,
  );
  return makeCoverage(input.scenarioId, CoverageLevel.WELL_EVIDENCED, explanations, counts);
}

function makeCoverage(
  scenarioId: string,
  level: CoverageLevel,
  explanations: string[],
  counts: CoverageCounts,
): ScenarioCoverage {
  const c = new ScenarioCoverage();
  c.scenarioId = scenarioId;
  c.coverageLevel = level;
  c.explanations = explanations;
  c.counts = counts;
  return c;
}
