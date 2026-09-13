# PLAN_REBASE.md — 计划重分析（MVP03 §14–§16）

## 触发

`currentGraphRevision != ChangePlan.lastAnalyzedGraphRevision` 且计划未 completed/cancelled
→ effectiveStatus = `needs_revalidation`。用户进入计划点击「重新分析」→ `rebasePlan()`。

## 行为

1. revision 未变 → no-op（PRB-001，不产生假差异）。
2. 终态计划 → 永不改写（PRB-009/010）。
3. 重新 `simulateDisable(target)` → 新 ImpactSnapshot。
4. Diff（确定性排序，按 nodeId/capability）：
   - `addedImpacts`（新出现）/ `removedImpacts`（消失）/ `changedImpacts`（status/reasonCode 变化）。
5. **动作合并（PRB-008）**：rebase 不新增/不完成任何动作；已有动作的 done/verification 原样保留。
6. `lastAnalyzedGraphRevision ← currentGraphRevision`（PRB-007，同一次调用内完成）。

## 用户语义（UI 文案）

「你的数字基础设施在创建此计划后发生了变化。新增影响：N / 解除影响：N / 状态变化：N。请重新检查。」
禁止显示「计划已失效，请重新创建」（数据可迁移，除非不可迁移的显式场景）。

## 测试证据（PRB-001..011，tests/services/change-plan-rebase.test.ts）

no-rebase / needs_revalidation / new dep → changed impact / retire → impact 变化 /
unknown relation 恒 needs_review / proposal 不触发 / revision 更新 / 动作保留 /
completed 历史 / cancelled 不重开 / deterministic ordering。
人工变异 M-R4（revision 未变也 rebase）= KILLED。

## Invariant

INV-15：`lastAnalyzedGraphRevision ≤ currentGraphRevision` 恒成立。
INV-17：completed 计划的快照不被 rebase 改写。
