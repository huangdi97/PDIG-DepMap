# LIVING_GRAPH.md — 活图谱总览（MVP03）

> 恢复入口四件套：AGENTS.md / WORK_STATUS.md / MVP03_ACCEPTANCE.md / GOAL_MVP03_LIVING_GRAPH_CHANGE_SAFETY.md。

## 产品链（MVP03 完成段）

```
Evidence → Proposal → Human Confirmation → Confirmed Reality Graph
    ↓
Living Graph Maintenance（graphRevision / RealityDrift / DiscoveryCandidate）
    ↓
Scenario（ScenarioTemplate）→ Impact → ChangePlan → Plan Rebase → PlanReadiness
    ↓
Action → Verification → Reality Update（user_confirmed）
```

## 九个闭环模块与落位

| 模块 | 代码 | 文档 |
|---|---|---|
| graphRevision | `src/repositories/graph-revision.ts`（bump 与 Reality mutation 同事务） | GRAPH_REVISION.md |
| ChangePlan + Rebase | `src/domain/change-plan.ts` + `repositories/change-plan-repository.ts` + `services/plan-analysis.ts` | CHANGEPLAN_STATE_MACHINE.md / PLAN_REBASE.md |
| PlanReadiness | `src/services/plan-readiness.ts`（纯规则） | PLAN_READINESS.md |
| ScenarioCoverage | 同上 | SCENARIO_COVERAGE.md |
| RealityDrift | `repositories/reality-drift-repository.ts` + `services/reality-drift-service.ts` | REALITY_DRIFT.md |
| DiscoveryCandidate | `repositories/discovery-candidate-repository.ts` + `services/discovery-service.ts` | DISCOVERY_CANDIDATE.md |
| ScenarioTemplate | `src/scenarios/registry.ts` | SCENARIO_TEMPLATE.md / SCENARIO_TEMPLATE_POLICY.md |
| Timeline | `src/services/timeline.ts`（纯投影） | TIMELINE_UPCOMING.md |
| Verification | ChangePlan actions 内嵌状态机（`services/change-plan-service.ts`） | ACTION_VERIFICATION.md |

## 语义铁律（继承 + 新增）

- Observation ≠ Reality；Proposal ≠ Reality；**RealityDrift ≠ Reality change**；
  **DiscoveryCandidate ≠ Node**；**Timeline reminder ≠ Graph fact**；
  **ScenarioTemplate ≠ 业务真相**；**Future observation ≠ 自动 verification**；
  **Graph revision change ≠ 自动接受新 Reality**。
- 一切 Reality 改变仍必须经 `user_confirmed`（MVP03 默认）。
- 宁可漏报，不可把「不确定」伪装成「必须处理」。

## 测试证据

407+ tests：GR 12 / PRB 11 / Readiness+Coverage 17 / RD 12 / PC 7 / ST 5 / TL 7 / VF 8 /
MIG3 6 / INV-15..21 7 / PI 3 / perf 4 + 既有 336。详见 docs/MVP03_TEST_MATRIX.md。
