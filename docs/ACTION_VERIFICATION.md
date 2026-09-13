# ACTION_VERIFICATION.md — 动作验证（MVP03 §52–§54）

## 状态机（PlanAction.verification）

```
method:   manual_confirmation | future_observation | authoritative_source（仅定义，不自动执行）
status:   not_required → pending → evidence_suggested → verified
                                        ↘ failed
```

## 铁律

1. **Action 完成 ≠ Verified**（VF-001）：`completeAction` 只置 done/doneAt，
   verification 状态纹丝不动。
2. **manual_confirmation**：用户显式确认 → verified + verifiedAt（VF-002）。
3. **future_observation**：新 Evidence 路径命中动作的
   `expectedFromNodeId → expectedToNodeId` → **只能**置 `evidence_suggested`
   （VF-003）——UI 文案：「新数据表明这项变更可能已经生效，请确认。」
   用户确认后才 verified。**禁止自动 verified**。
4. **evidence suggestion 不修改 Reality**（VF-004）：revision 不变、零 Dependency 写入。
5. duplicate evidence 不重复记录（VF-005，refs 去重）。
6. **错误来源不产生 suggestion**（VF-006）：watch 字段不匹配 → 无任何状态变化。
7. verified 状态跨 restart 稳定（VF-007，随 action_items_json 持久化）。

## 与 Reality 的关系

verification 全部完成后，相关 Reality 变更（如 retire 旧边）仍必须经用户显式操作
（ConfirmationService / Drift resolution）——verification 记录本身不是 Reality mutation。

## 测试证据

tests/services/action-verification.test.ts（VF-001..007 + 两段式确认链路）。
