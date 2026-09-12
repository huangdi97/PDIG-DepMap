# MUTATION_TEST_REPORT.md — 变异测试基线（Engineering Baseline V1，2026-09-13）

> 工具：Stryker 10（`@stryker-mutator/core` + vitest runner，`--no-save` 安装，配置
> `core/stryker.conf.mjs`）。范围：Core critical modules 中语义密度最高的
> `src/impact/kernel.ts` + `src/domain/relation-registry.ts`（532 mutants）。
> 覆盖分析：perTest；concurrency 2；单轮耗时 ≈ 13 分钟。
> 复现：`npm i --no-save @stryker-mutator/core @stryker-mutator/vitest-runner && npx stryker run`
> （HTML/JSON 报告输出 `core/reports/mutation/`，已 gitignore）。

## 结果（含 3 条补测后的最终轮）

| 文件 | mutants | killed | timeout | survived | no-coverage | score | covered score |
|---|---|---|---|---|---|---|---|
| impact/kernel.ts | 436 | 242 | 2 | 164 | 28 | 55.96% | 59.80% |
| domain/relation-registry.ts | 96 | 93 | 0 | 3 | 0 | 96.88% | 96.88% |

## 人工变异验证（Proposal / Fingerprint 域，Stryker 范围外）

| 变异 | 内容 | 结果 |
|---|---|---|
| M1 | `REPROPOSAL_MIN_NEW_OBSERVATIONS 3→0`（rejected 无门槛重提） | **KILLED**（proposal-lifecycle 3 用例红） |
| M2 | fingerprint HMAC 输入去掉 `sourceInstanceId`（跨实例指纹串扰） | **KILLED**（source-instance-scope 2 用例红） |
| M3 | `canonicalGroupKey` 成员不排序（[A,B]≠[B,A]） | **KILLED**（repositories/proposal-lifecycle 2 用例红） |

## Survived 分类（164 个 kernel 幸存变异）

| 类别 | 数量（约） | 判定 |
|---|---|---|
| StringLiteral（reasonText 中文文案模板，23 处） | 23 | **可接受**：展示文本；逐一断言精确文案价值低 |
| 排序 comparator 的 capability tie（91–94/130–138 等） | ~25 | **等价变异**：kernel 只评估 payment，capability 恒等，分支不可达 |
| BFS guard 上限机制（309–310） | ~5 | **等价变异**：wave-BFS 天然终止（状态集只增），guard 为防御上限 |
| no-coverage 28 处 | 28 | 防御分支（单 capability 下不可达），与 COVERAGE_POLICY 缺口同一批 |
| evaluateTarget / merge / checklist 的条件与相等变异 | ~80 | **下一里程碑目标**：同严重度合并、groupKeys 输出、proposal_only 断言等已补测 3 条（+7 killed）；剩余按「关键正确性优先」逐步消灭 |

## 本轮已补测（tests/impact/kernel-mutation-baseline.test.ts）

- M-K1 同严重度重评估的 edgeKeys 并集与排序（kills severity-merge tie 系列变异）
- M-K2 confirmed group 失败/覆盖路径的 groupKeys 输出（kills coveringGroups map/sort 变异）
- M-K3 proposal_only 的 available=true / edgeKeys=[] / proposalKeys 记录（kills 该分支 Boolean/Array 变异）

## 政策

1. 变异测试为**一次性 baseline + 按需重跑**（不入 `check:full` 常驻：13 分钟成本不适合每次收口）。
2. 目标：下一里程碑 kernel **covered score ≥ 70%**（排除 StringLiteral 与等价变异口径后衡量）。
3. 任何 HIGH 风险变更（CHANGE_RISK_POLICY）涉及 kernel/registry 语义时，重跑本基线并确认
   无新增 survived。
4. 禁止为杀变异写「断言实现细节」的测试；只补语义级断言。
