# MVP03_FREEZE_PRE_AUDIT.md — Freeze 启动前现状审计（2026-09-13）

> 原则：先记录现状、实际检查代码与测试（不凭报告假设正确）。

## 1. Git / 环境

| 项         | 值                                                                                               |
| ---------- | ------------------------------------------------------------------------------------------------ |
| HEAD       | `cb3fe15` docs(mvp03): final report PASS + acceptance all-gates checked by evidence              |
| branch     | `feat/mvp03-living-graph`（干净，0 dirty；6 个 Freeze 控制文件 untracked，为用户提供的任务输入） |
| tags       | `v0.2.0-mvp02`（v0.3.0-mvp03 待 Freeze PASS 后创建）                                             |
| Node / npm | v22.15.0 / 11.3.0                                                                                |

## 2. 基线状态（实跑）

- 总测试：**427/427 PASS（39 文件，0 skip）**；check / check:full / stability ×3 全绿（MVP03 收口轮）
- coverage：src line 93.45% / branch 81.4%（COVERAGE_POLICY 已含 MVP03 模块 Baseline）
- mutation：MVP03 人工 M-R1..R5 全 KILLED；Stryker baseline（kernel/registry 55.96%/96.88%，PARTIAL_WITH_REPORT）
- schemaVersion = **3**（`core/src/schema/migrations.ts`）；DEPMAP formatVersion = **1**（不变）
- Real Data NOT_RUN；平台编译 BLOCKED（B1–B3/B10）

## 3. MVP03 九模块实现文件（实际核验）

| 模块                             | 文件                                                                                                                      |
| -------------------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| graphRevision                    | `core/src/repositories/graph-revision.ts`（+ dependency/group 仓库挂钩）                                                  |
| ChangePlan + Rebase              | `core/src/domain/change-plan.ts`、`core/src/repositories/change-plan-repository.ts`、`core/src/services/plan-analysis.ts` |
| PlanReadiness / ScenarioCoverage | `core/src/services/plan-readiness.ts`（纯函数）+ `core/src/services/change-plan-service.ts`（输入装配）                   |
| RealityDrift                     | `core/src/repositories/reality-drift-repository.ts`、`core/src/services/reality-drift-service.ts`                         |
| DiscoveryCandidate               | `core/src/repositories/discovery-candidate-repository.ts`、`core/src/services/discovery-service.ts`                       |
| ScenarioTemplate                 | `core/src/scenarios/registry.ts`                                                                                          |
| Timeline                         | `core/src/services/timeline.ts`                                                                                           |
| Verification                     | `core/src/services/change-plan-service.ts`（Action.verification 状态机）                                                  |
| Migration v2→v3                  | `core/src/schema/migrations.ts`（SCHEMA_V3_STATEMENTS）+ `core/tests/repository/migration-v3.test.ts`                     |
| payload migration                | `core/src/services/graph-serialize.ts`（migratePayloadV2toV3 + v1 组合）                                                  |

UI：`app/pages/` 16 页（home/scenarios/plans×3/timeline/drift + 既有 11）。

## 4. P0 发现（本轮必须修）

**PlanReadiness 数量相减 correctness bug（确认存在）**：
`change-plan-service.ts` `assembleReadinessInput()` 当前实现：

```ts
const doneChangeActions = plan.actions.filter((a) => a.phase === 'change' && a.done).length
const pendingMustChange = Math.max(0, mustChangeTargets - doneChangeActions)
```

违反 §5–§7：must_change target 与 Action 非一一对应（1 target ↔ N actions；1 action ↔ N targets）。
**修复方向**：`PlanAction.resolvesImpactKeys[]` 显式映射 +「该 key 的全部 claiming change 动作 done 才 resolved」，
配 FR-READ-001..014 冻结测试。详见 MVP03_FREEZE_REPORT.md 修复记录。

## 5. 当前已知 blockers

B1（Android JDK17/SDK）/ B2（DevEco）/ B3（macOS/Xcode）/ B10（HBuilderX）→ 编译/真机；
B4–B9/B11–B13 发布材料。均不阻塞 Core Freeze。

## 6. MVP03_FINAL_REPORT 当前 verdict

`MVP03_LIVING_GRAPH_CHANGE_SAFETY = PASS`（427 tests）。本轮 = 冻结复核，不是重做；
已验证 Gate 按 §112 标 Verified，§113 的 milestone regression 全部重跑。
