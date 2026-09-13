# MVP03_TEST_MATRIX.md — 测试矩阵（MVP03）

> 口径：Engineering Baseline V1 继承 + MVP03 增量。全部为实跑（407+ tests）。

## 分层

| 层 | 文件 | 用例 |
|---|---|---|
| GraphRevision | tests/repository/graph-revision.test.ts | GR-001..012（12） |
| Migration v2→v3 | tests/repository/migration-v3.test.ts | MIG3-001..006（6） |
| ChangePlan Rebase | tests/services/change-plan-rebase.test.ts | PRB-001..011（11） |
| PlanReadiness / ScenarioCoverage | tests/services/plan-readiness-coverage.test.ts | §20 八条 + §24 七条（17） |
| RealityDrift | tests/services/reality-drift.test.ts | RD-001..010 + 2 补充（12） |
| DiscoveryCandidate | tests/services/discovery-candidate.test.ts | PC 七条（7） |
| ScenarioTemplate / Timeline | tests/services/scenario-timeline.test.ts | ST-001..005 + TL-001..007（12） |
| Action Verification | tests/services/action-verification.test.ts | VF-001..007 + 两段式（8） |
| Property（fast-check） | tests/property/mvp03-properties.test.ts | PI-1..3（3，numRuns 100–200） |
| Invariants | tests/invariants/mvp03-invariants.test.ts | INV-15..21（7） |
| Performance Smoke | tests/perf/mvp03-perf.test.ts | 100 plans / 1000 items / 500 drifts / 500 candidates / 1k rebase（4） |
| 既有（MVP01/02 + Baseline） | 28 文件 | 336 → 部分 J 套件扩展至 21（+4 v3） |

## Invariant 对照（§60）

| 不变量 | 证据 |
|---|---|
| lastAnalyzedGraphRevision ≤ current | INV-15 |
| ready_with_known_scope 必须 revision current + must_change 已处理 | INV-16 + PI-1 |
| completed plan 不被 rebase 改写 | INV-17 + PRB-009 |
| confirmed_change drift 必对应成功 Reality mutation | INV-18 + RD-005/006 |
| accepted candidate 无 duplicate logical Node | INV-19 |
| TimelineItem 溯源有效 | INV-20 + TL-006 |
| active template ↔ payment capability | INV-21 + ST-001/005 |
| 任何 Reality mutation revision 单调；非 Reality 操作不变 | GR-001..012 + GR-010 |
| Drift 永不直接修改 Reality | RD-003/004 |
| Candidate 永不进入 Impact | PC（deps.countAll()=0） |
| Proposal 永不直接 must_change | 既有 T4/K5/P4（继承） |
| absence 永不 retire Reality | 既有 H1 + INV-10（继承）+ RD-002 |
| Rebase / Timeline deterministic | PRB-011 + TL-005 |

## 人工变异（§61，全部 KILLED）

| 变异 | 目标 | 结果 |
|---|---|---|
| M-R1 | bumpGraphRevision 去 +1（revision 不再推进） | KILLED（8 用例红） |
| M-R2 | readiness blocked 规则失效 | KILLED（2 用例红） |
| M-R3 | drift 新建阈值失效 | KILLED（1 用例红） |
| M-R4 | revision 未变也 rebase | KILLED（3 用例红） |
| M-R5 | dismissed 无门槛重提 | KILLED（1 用例红） |

Stryker baseline（kernel/registry 532 mutants）沿用 Engineering Baseline V1 报告口径。

## UI 静态覆盖（§67，编译 BLOCKED B10）

home（4 状态区块）/ scenarios（active 3 + 无 planned）/ plans+plan-detail
（blocked/review_required/ready/needs_revalidation 派生与横幅）/ timeline（空/桶排序）/
drift（四选项）—— 源码级与 core 口径一致（SQL 派生镜像 effectiveStatus/bucket 规则）。
