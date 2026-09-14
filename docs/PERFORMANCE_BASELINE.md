# PERFORMANCE_BASELINE.md — 性能基线（Engineering Baseline V1）

> 实测环境：Windows 10.0.26200 x64，Node v22.15.0。日期：2026-09-13。
> 命令：`npm run test:perf`。原则：防明显退化，不用绝对毫秒做 CI 硬门（跨机器噪声大）。

## Baseline 数值（同一轮实测）

| 场景                                                               | 耗时     | 预算（回归判定）               |
| ------------------------------------------------------------------ | -------- | ------------------------------ |
| WeChat parse 10k rows                                              | 125.1 ms | < 5s                           |
| Fingerprint 10k                                                    | 107.7 ms | < 5s                           |
| Import+proposal finalize（2000 rows）                              | 240.1 ms | < 15s                          |
| Impact 1k-node chain                                               | 63.7 ms  | < 5s                           |
| Impact 100-node cycle                                              | 1.5 ms   | < 2s                           |
| 恶意 KDF bounds 拒绝 ×100                                          | 0.9 ms   | < 1s（在 Argon2 前拒绝的证明） |
| Large synthetic（500 nodes / 1k deps / 100 proposals / 50 groups） | 5169 ms  | < 30s                          |
| Export 500-node graph                                              | 8.7 ms   | < 5s                           |
| depmap create+open（65536/3/1）                                    | 502 ms   | < 10s                          |
| MVP02: CSV parse 10k rows                                          | 46.3 ms  | < 5s                           |
| MVP02: OFX parse 10k txns                                          | 48.9 ms  | < 5s                           |
| MVP02: 3 SourceInstance 并发导入（2k×3）                           | 4681 ms  | < 30s                          |

预算为 perf 测试内断言值（宽松，吸收 CI 噪声）；显著超出预算即为回归信号，
须在 WORK_STATUS 记录并归因（算法复杂度变化 / 新增持久化 / 锁竞争）。

## 方法说明

- 固定 synthetic workload：10k 观测级数据流、500 节点/1k 边/100 proposals/50 groups、
  1k 节点链式图、100 节点环、golden 参数加密往返。
- 100k 观测级（heavy/nightly）暂未设常驻用例；如需可基于同 generator 扩展，不入 check:full。
- 内存粗测（MEMORY_DATA_LIFETIME 政策）：Observation/raw buffer 只在会话内存，
  finalize 后 coordinator 实例应被释放引用；perf 用例跑 10k 级数据未触发堆异常即视为通过。

## 判定流程

后续每轮 `check:full` 的 test:perf 输出与本表对照；> 2× 预算的恶化必须修复或书面归因后
更新本表（注明 commit 与原因），不允许静默下调预算。
