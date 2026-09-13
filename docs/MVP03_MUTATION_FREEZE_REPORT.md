# MVP03_MUTATION_FREEZE_REPORT.md — 变异冻结报告（Freeze §65–§67/§99）

> 方法：targeted manual mutation（backup → mutate → focused suite → restore）。
> 要求：**无存活 critical correctness mutant**。日期：2026-09-13。

## 定向变异结果（10/10 KILLED）

| 变异 | 目标语义 | 测试集 | 结果 |
|---|---|---|---|
| M-R1 | bumpGraphRevision 去 +1（revision 不再推进） | graph-revision GR | KILLED（8 红） |
| M-R2 | readiness blocked 规则失效 | plan-readiness-coverage + invariants | KILLED（2 红） |
| M-R3 | drift 新建阈值失效 | reality-drift RD | KILLED（1 红） |
| M-R4 | revision 未变也 rebase | change-plan-rebase PRB | KILLED（3 红） |
| M-R5 | dismissed 无门槛重提 | discovery-candidate PC | KILLED（1 红） |
| FM-1 | **声明即 resolved（忽略 done 检查）** | plan-readiness-freeze | KILLED（FR-READ-001） |
| FM-2 | **claiming 分配给已完成动作（事后追认）** | plan-readiness-freeze | KILLED（FR-READ-017，本轮补） |
| FM-3 | **verified/failed 可被 evidence suggestion 覆盖** | action-verification + state-machine | KILLED（FREEZE 用例，本轮补） |
| FM-4 | **已确认来源也产生 drift（already_confirmed guard 失效）** | reality-drift | KILLED（FREEZE 用例，本轮补） |
| FM-5 | **ChangePlan 状态迁移表失效** | state-machine-freeze | KILLED（illegal transition） |

重点命中 Freeze §66 高风险点：`===↔!==`（FM-1 done 检查）、blocked↔review（M-R2/FM-1）、
absence guard（M-R3）、confirmation guard（FM-4）、verified guard（FM-3）、
action/impact mapping（FM-1/FM-2）、revision equality（M-R4）。

## Stryker 基线（范围外沿用）

Engineering Baseline V1 报告口径：kernel 55.96% / registry 96.88%（532 mutants，
survived 以 reasonText 文案与单 capability 等价变异为主，无 critical correctness 存活——
详见 docs/MUTATION_TEST_REPORT.md 分类）。

## 结论

**MUTATION_TESTS = PARTIAL_WITH_REPORT，且 0 critical survived**（§99 允许 PASS 的前提成立）。
