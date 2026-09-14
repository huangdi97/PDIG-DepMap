# IMPACT_ENGINE.md — payment-domain Kernel

> 实现源：`core/src/impact/kernel.ts`；测试：`core/tests/impact/kernel.test.ts`（17 项全 PASS）

## API

```ts
type ImpactStateKey = { nodeId: string; capability: Capability }   // MVP 只支持 payment

simulateScenario(graph, unavailable: Set<ImpactStateKey>): ImpactResult
simulateDisable(graph, nodeId, capability = 'payment')            // 便捷包装
```

`graph` 只接收**已确认**实体：active Dependency + active confirmed DependencyGroup。
pending Proposal 以只读方式传入，仅用于产生 needs_review 报告，**永不**参与确定性失效传播。

## 判定规则（优先级从高到低）

对目标 T 的 payment 状态，当存在来自失效上游的 active 入边时：

1. **T 的失效入边属于 confirmed Group**：
   - 所有覆盖组全部失效（ANY 无可用成员 / ALL 有成员失效）→ `must_change`（capability lost）
   - 任一覆盖组仍满足 → `backup_path` + `redundancyDegraded=true`（有备用路径 / 能力降级）
2. **无 Group 但存在其他 active 入边**（未确认组合）→ `needs_review`（不得宣称备用路径）
3. **无其他边且失效边 criticality=required** → `must_change`
4. **无其他边且 criticality=unknown** → `needs_review`
5. **仅有 pending Proposal** → `needs_review`（proposal_only）
6. 上游不确定（needs_review）向下游传播 needs_review（upstream_uncertain），但不产生 must_change。

## 传播与防环

- wave-BFS：lost 集与 uncertain 集只增长；每个 `(nodeId, capability)` 最多进入一次 → 天然终止（60 节点环已测）。
- 只沿 `capability='payment'` 的 active 边传播；`recovery/access` 边不参与（T12）。
- 输出排序 `(depth, nodeId, capability)` 确定；checklist 中原始注销/停用动作强制最后（`target_operation`）。

## 状态标签（UI 等级）

| 内部 status                       | UI 等级               | 含义                            |
| --------------------------------- | --------------------- | ------------------------------- |
| must_change                       | 必须处理              | 已确认失效且无确认替代          |
| backup_path (+redundancyDegraded) | 有备用路径 / 能力降级 | confirmed Group 仍满足          |
| needs_review                      | 建议检查              | 未确认组合 / unknown / proposal |
| unaffected                        | 不受影响              | 与场景无确认关系                |

## 正确性目标

`confirmed false positive = 0`：must_change 只能来自 required 边失效或 confirmed Group 全失效。
