# TIMELINE_UPCOMING.md — 即将到来 / 时间线（MVP03 §42–§45）

## 定义

Timeline 是 **derived read model / projection**（`buildTimeline(driver, now)`，纯函数）：
不持久化、不是第二真相数据库、可随时重建。TimelineItem 本身不是 Reality。

## 数据来源（全部可溯源 sourceType/sourceId）

| 来源 | kind | 桶 |
|---|---|---|
| ChangePlan stale | needs_attention | attention |
| ChangePlan effectiveDate | upcoming_change | 按日期 |
| verify 动作未验证 | verification_pending | 按计划日期 |
| open RealityDrift | drift_review | attention |
| Node fields.expiryDate | expiration | 按日期 |
| SourceInstance 超 45 天未刷新 | freshness_review | attention |

## 类别边界（§43）

只展示数字基础设施事项。禁止普通生日 / 普通日程 / 喝水 / 会议 / 运动。

## 排序（deterministic，TL-005）

```
attention → overdue → today → 7d → 30d → 90d → later
同桶：priority 降序（needs_attention/drift=3 > verification/upcoming/expiration=2 > freshness=1）
再 tie-break：scheduledAt → id
```

## 测试证据（TL-001..007）

空 graph / 计划进桶 + stale→attention / drift + verification / node expiry + stale 来源 /
排序确定性（两次构建完全一致）/ 全项可溯源 + 投影不写库 / completed 不产生项。
perf：1000+ items 投影 14ms（mvp03-perf）。

## UI

「即将到来」页按桶分组展示；点按可跳转 ChangePlan 详情或 Drift 处理。
