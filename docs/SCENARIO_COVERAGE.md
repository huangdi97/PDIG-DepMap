# SCENARIO_COVERAGE.md — 场景信息覆盖（MVP03 §21–§24）

## 定义

ScenarioCoverage 回答「这一次变更，系统知道多少？」——是**信息覆盖情况**，
不是安全概率。禁止「安全评分 87 分」、禁止伪造完整率、禁止 source 数量算百分比。

## 结构

`computeScenarioCoverage(input): ScenarioCoverage`（纯函数，`src/services/plan-readiness.ts`）

- 输入：scenarioId / sources（含 lastIngestedAt）/ confirmedDirect·IndirectDependencies /
  pendingProposals / unresolvedCandidates / staleDependencies / unknownCriticalityCount /
  unverifiedActions / freshnessThresholdDays / now。
- 输出：`coverageLevel` + **可解释 explanations[]** + counts。

## Level 判定

| Level | 条件 |
|---|---|
| `unknown` | 无来源且无已确认直接依赖 |
| `limited` | 有来源但全部超过新鲜度阈值，或没有任何已确认直接依赖 |
| `partial` | 有新鲜来源 + 有确认依赖，但存在 pending proposal / 未解析候选 / stale 依赖 / unknown criticality / 未验证动作 |
| `well_evidenced` | 新鲜相关来源 + 关键关系已确认 + 无任何未决信号 |

禁止 `safe` / `complete` / `100%`。**absence 不可能提高 coverage**（输入通道不存在）。
coverage 永远不等于 ready（与 PlanReadiness 是两个独立维度）。

## 测试证据（§24 七条，tests/services/plan-readiness-coverage.test.ts）

unknown / limited（含过期解释）/ partial（解释 pending 与 unknown）/ well_evidenced /
coverage ≠ ready / pending proposal 降级并解释 / absence 通道不存在。
