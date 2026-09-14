# MUTATION_TEST_REPORT.md — 变异测试基线（Engineering Baseline V1，2026-09-13）

> 工具：Stryker 10（`@stryker-mutator/core` + vitest runner，`--no-save` 安装，配置
> `core/stryker.conf.mjs`）。范围：Core critical modules 中语义密度最高的
> `src/impact/kernel.ts` + `src/domain/relation-registry.ts`（532 mutants）。
> 覆盖分析：perTest；concurrency 2。**耗时实测**：首轮（2026-09-13）≈ 13 分钟；
> 重跑（2026-09-14）**35 分 03 秒**（同一配置，机器负载不同；dry run 294 tests）。
> 复现：`npm i --no-save @stryker-mutator/core @stryker-mutator/vitest-runner`
> 然后 `node node_modules/@stryker-mutator/core/bin/stryker.js run`
> （**不要用 `npx stryker`**：本环境 npx 会解析到缓存中一个损坏的同名旧包，报 `Cannot find module 'rx'`）。
> HTML/JSON 报告输出 `core/reports/mutation/`，已 gitignore。

## 结果（含 3 条补测后的最终轮）

| 文件                        | mutants | killed | timeout | survived | no-coverage | score  | covered score |
| --------------------------- | ------- | ------ | ------- | -------- | ----------- | ------ | ------------- |
| impact/kernel.ts            | 436     | 242    | 2       | 164      | 28          | 55.96% | 59.80%        |
| domain/relation-registry.ts | 96      | 93     | 0       | 3        | 0           | 96.88% | 96.88%        |

## 人工变异验证（Proposal / Fingerprint 域，Stryker 范围外）

| 变异 | 内容                                                           | 结果                                                   |
| ---- | -------------------------------------------------------------- | ------------------------------------------------------ |
| M1   | `REPROPOSAL_MIN_NEW_OBSERVATIONS 3→0`（rejected 无门槛重提）   | **KILLED**（proposal-lifecycle 3 用例红）              |
| M2   | fingerprint HMAC 输入去掉 `sourceInstanceId`（跨实例指纹串扰） | **KILLED**（source-instance-scope 2 用例红）           |
| M3   | `canonicalGroupKey` 成员不排序（[A,B]≠[B,A]）                  | **KILLED**（repositories/proposal-lifecycle 2 用例红） |

## Survived 分类（164 个 kernel 幸存变异）—— 精确版

> 数据来源：`core/reports/mutation/mutation.json`（本轮重跑生成）。
> 全部数量为**脚本聚合的确切值**，非估计；行段由「相邻行间隔 ≤6」聚类得出，16 个簇合计恰为 164。

### 按 mutator 的精确分布

| mutator               | Survived | NoCoverage |
| --------------------- | -------- | ---------- |
| ConditionalExpression | 44       | 9          |
| StringLiteral         | 23       | 3          |
| EqualityOperator      | 22       | 6          |
| ArrayDeclaration      | 19       | 3          |
| MethodExpression      | 17       | 0          |
| BooleanLiteral        | 10       | 2          |
| LogicalOperator       | 7        | 0          |
| ArrowFunction         | 6        | 0          |
| BlockStatement        | 5        | 0          |
| CallExpression        | 5        | 0          |
| UnaryOperator         | 2        | 4          |
| ArithmeticOperator    | 2        | 0          |
| AssignmentOperator    | 1        | 0          |
| ObjectLiteral         | 1        | 1          |
| **合计**              | **164**  | **28**     |

Timeout 2 处：`kernel.ts:309`（BlockStatement → `{}`）、`kernel.ts:356`（ConditionalExpression → `true`）。
`relation-registry.ts` Survived 3 处：`:29` / `:47`（StringLiteral → `""`）、`:57`（ArrowFunction → `() => undefined`）。

### 按语义位置的精确分类（16 簇）

| 行段     | 数量    | 主要 mutator 构成                                                                                      | 语义位置                                                        | 判定                                                           |
| -------- | ------- | ------------------------------------------------------------------------------------------------------ | --------------------------------------------------------------- | -------------------------------------------------------------- |
| 91–94    | 11      | Conditional×4 / Equality×4 / Method×1 / Arrow×1 / Unary×1                                              | `sortKeys` 比较器（nodeId 优先，capability 兜底）               | **等价**：MVP 仅评估 payment，capability 恒等 → 兜底分支不可达 |
| 130–138  | 29      | Conditional×11 / Equality×7 / Method×4 / Arrow×3 / Logical×2 / Unary×1 / Array×1                       | `activePaymentDeps` / `activePaymentGroups` 的 filter+sort      | **多数等价**：id 唯一 → 比较器 `-1/0/1` 边界不可区分           |
| 149–150  | 4       | Method×1 / Conditional×1 / Logical×1 / String×1                                                        | `uncertainEdges` 过滤                                           | 待消灭：需「上游不确定 + 另有已确认入边」用例                  |
| 163–164  | 5       | Method×2 / Conditional×2 / Logical×1                                                                   | `propKeys` 过滤（proposal_only 分支）                           | 部分已补测（M-K3）                                             |
| 171–189  | 11      | Boolean×3 / String×3 / Array×3 / Conditional×1 / Equality×1                                            | `upstream_uncertain` / `proposal_only` 返回对象                 | **文案可接受** + 部分待消灭                                    |
| 221      | 1       | Conditional×1                                                                                          | `ALL` 模式 `availableMembers.length === memberEdges.length`     | 待消灭（ALL 模式全可用场景）                                   |
| 229–240  | 7       | Method×2 / String×2 / Boolean×1 / Arrow×1 / Array×1                                                    | `allFailed` + `confirmed_group_failed` 返回                     | 文案可接受 + 部分待消灭                                        |
| 249–252  | 3       | String×1 / Method×1 / Array×1                                                                          | `confirmed_group_covered` 返回                                  | 同上                                                           |
| 264–281  | 8       | Array×4 / String×2 / Method×1 / Boolean×1                                                              | `unconfirmed_alternative_exists` + `required` 判定              | 待消灭（required 无替代路径）                                  |
| 288–294  | 5       | Boolean×2 / Array×2 / String×1                                                                         | `criticality_unknown` 返回                                      | 同上                                                           |
| 306–310  | 7       | Conditional×2 / Equality×2 / Arith×1 / Logical×1 / Assignment×1                                        | `maxGuard` 与 `while` 循环条件                                  | **等价**：wave-BFS 状态集只增 → 天然终止，guard 仅为防御上限   |
| 317–361  | 42      | Conditional×13 / Array×7 / Call×5 / Method×4 / Boolean×3 / Equality×3 / Block×3 / String×2 / Logical×2 | wave-BFS 主循环（pending 收集、severity 合并、失效/不确定传播） | **最大缺口**：需逐项针对性用例                                 |
| 369–378  | 13      | Conditional×6 / Equality×4 / Method×1 / Arrow×1 / Arith×1                                              | `if (!grew) break` + `targets` 排序比较器                       | 混合：比较器多为等价；`!grew` 待覆盖                           |
| 387–409  | 10      | String×5 / Conditional×2 / Equality×1 / Block×1 / Object×1                                             | checklist 三档 level 与 title 文案                              | **文案可接受** + 分支覆盖待补                                  |
| 420–421  | 2       | String×2                                                                                               | `target_operation` title/detail                                 | **文案可接受**                                                 |
| 434–441  | 6       | String×4 / Block×1 / Conditional×1                                                                     | `severity()` 映射                                               | **部分等价**：`degraded` case 在当前 status 联合类型下不可达   |
| **合计** | **164** |                                                                                                        |                                                                 |                                                                |

**关于「等价变异」与「真实缺口」的划分口径（重要）**：

上表的「判定」列是**人工语义评估**，**不是机器可判定的计数**。可机械核实的只有：

- **StringLiteral = 23 处**（reasonText / title / detail 中文文案模板）—— 客观事实；
- **NoCoverage = 28 处**（单 capability 下的防御分支）—— 客观事实；
- **最大单簇 = `317–361`（42 处，wave-BFS 主循环）**—— 客观事实，也是下一里程碑的首要目标。

因此本报告**刻意不给出**「等价变异总数」或「真实缺口总数」这类数字 ——
那需要逐条语义论证，把评估包装成测量会违反诚实口径。
已确证等价的是两处机制性不可达：`91–94`（capability 恒等）、`306–310`（BFS 天然终止）。

**NoCoverage 28 处的精确构成**：Conditional×9 / Equality×6 / Unary×4 / Array×3 / String×3 / Boolean×2 / Object×1
（均为单 capability 下的防御分支，与 `COVERAGE_POLICY` 记录的缺口同一批）。

## 本轮已补测（tests/impact/kernel-mutation-baseline.test.ts）

- M-K1 同严重度重评估的 edgeKeys 并集与排序（kills severity-merge tie 系列变异）
- M-K2 confirmed group 失败/覆盖路径的 groupKeys 输出（kills coveringGroups map/sort 变异）
- M-K3 proposal_only 的 available=true / edgeKeys=[] / proposalKeys 记录（kills 该分支 Boolean/Array 变异）

## 政策

1. 变异测试为**一次性 baseline + 按需重跑**（不入 `check:full` 常驻：实测 13–35 分钟成本不适合每次收口）。
2. 目标：下一里程碑 kernel **covered score ≥ 70%**（当前 59.80%）。首要目标是 `317–361`（42 处，wave-BFS 主循环）。
3. 任何 HIGH 风险变更（CHANGE_RISK_POLICY）涉及 kernel/registry 语义时，重跑本基线并确认
   无新增 survived。
4. 禁止为杀变异写「断言实现细节」的测试；只补语义级断言。

---

## 重跑确认（FINAL PRODUCTION CLOSURE V1，2026-09-14）

在**重跑执行树** `b0ed6b5` 上**真实重跑** Stryker（同一配置 `core/stryker.conf.mjs`，
Stryker **10.0.0**，`--no-save` 安装；dry run 294 tests 通过；耗时 **35 分 03 秒**；`STRYKER_EXIT=0`）：

| 文件                        | mutants | killed  | timeout | survived | no cov | errors | score      | covered    |
| --------------------------- | ------- | ------- | ------- | -------- | ------ | ------ | ---------- | ---------- |
| impact/kernel.ts            | 436     | 242     | 2       | 164      | 28     | 0      | 55.96%     | 59.80%     |
| domain/relation-registry.ts | 96      | 93      | 0       | 3        | 0      | 0      | 96.88%     | 96.88%     |
| **All files**               | **532** | **335** | **2**   | **167**  | **28** | **0**  | **63.35%** | **66.87%** |

**与本文档首轮基线（2026-09-13）逐项完全相同** —— 无回归、无漂移。
`MUTATION_STRYKER_RERUN = PASS`。报告产物 `core/reports/mutation/mutation.{json,html}`（已 gitignore）。
