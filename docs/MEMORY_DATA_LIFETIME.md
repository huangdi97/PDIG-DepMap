# MEMORY_DATA_LIFETIME.md — 数据生命周期与内存政策（Engineering Baseline V1）

## 生命周期分层（CANONICAL_DESIGN §12 的工程落地）

| 数据                                       | 生命周期                                    | 存储位置                    |
| ------------------------------------------ | ------------------------------------------- | --------------------------- |
| raw bill bytes（`SourceInput.data`）       | 仅导入会话内存，finalize 后由调用方释放引用 | 不持久化、不缓存            |
| Observation / NormalizedPaymentObservation | 仅导入会话内存                              | 不持久化                    |
| Fingerprint                                | 持久化（作用域唯一键）                      | `observation_fingerprints`  |
| Evidence Summary                           | 持久化（每流一条：计数/时间边界/引用）      | `evidence`                  |
| Proposal 状态                              | 持久化                                      | `dependency_proposals(_v2)` |
| 用户确认后的图实体                         | 持久化（全库加密）                          | nodes/dependencies/groups   |
| ImportSession Summary                      | 持久化（计数/错误数）                       | `import_sessions`           |

## 内存纪律（代码评审 + 审计项）

1. **不缓存 raw file**：Coordinator/Adapter 不得把 `SourceInput.data` 挂到长生命周期对象上。
   现状：`ImportCoordinator` 的 begin 状态挂在实例上（module 级 WeakMap），
   实例随导入结束释放 —— 调用方（UI/CLI）必须在 finalize 后丢弃 coordinator 引用。
2. **UI state 不得长期持有 raw statement / Observation 数组**：UI 只消费
   CoordinatorOutcome（计数、candidates、recurrences 元数据、proposalKeys）。
3. **无全局 Observation store**：业务核心无模块级可变缓存（architecture check + 人工审计）。
4. 10k 级 synthetic workload（perf smoke）兼作粗粒度 memory smoke：
   完整导入无堆异常、无 OOM 即通过；100k 级属 heavy/nightly 可选，不入 check:full。

## 审计方式

- 静态：grep 全局缓存模式（`module-level Map/Set` 持有 observation/raw 字段）每轮 PRE_AUDIT。
- 动态：`npm run test:perf`（10k CSV/OFX + 3 实例并发）作为内存冒烟。
