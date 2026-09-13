# CHANGEPLAN_STATE_MACHINE.md — ChangePlan 工作流状态（MVP03 §12–§13）

## 存储

Schema v3 `change_plans` 表：id / template_id(nullable) / scenario / title / workflow_state /
baseline_graph_revision / last_analyzed_graph_revision / target_node_id / effective_date /
params_json / impact_snapshot_json / action_items_json / created_at / updated_at。

## stored workflowState（合法迁移表）

```
draft → analyzed / review_required / ready / in_progress / cancelled
analyzed → review_required / ready / in_progress / cancelled
review_required → ready / in_progress / cancelled / analyzed
ready → in_progress / cancelled / review_required
in_progress → verifying / completed / cancelled
verifying → completed / cancelled / in_progress
completed（终态）  cancelled（终态）
```

非法迁移抛错（`ChangePlanService.transition`）。

## effectiveStatus（派生，不回写）

```
effectiveStatus = needs_revalidation   若 currentGraphRevision > lastAnalyzedGraphRevision
                                           且 workflowState ∉ {completed, cancelled}
                = stored workflowState  否则
```

**不做批量回写**：graphRevision 变化不触发全表扫描更新计划；`needs_revalidation`
在读取时派生（`effectiveStatus()` / `isPlanStale()`，UI 与 readiness 共用同一口径）。

## Rebase 语义

见 docs/PLAN_REBASE.md。completed / cancelled 为历史，永不改写。

## 动作与验证

action_items_json 内嵌 PlanAction[]（phase: prepare/change/verify + verification 状态机）。
`done ≠ verified`（docs/ACTION_VERIFICATION.md）。
