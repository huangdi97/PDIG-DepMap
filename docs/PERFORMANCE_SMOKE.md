# PERFORMANCE_SMOKE.md — 性能 Smoke（RC PHASE L/AF）

> 只防异常退化，不做 premature optimization。
> 复现：`cd core && npm run test:perf`（阈值在测试内断言；数值随机器浮动）
> 实测（2026-09-12，Node v22.15.0 / win32-x64）：

| 项目                                               | 实测       | 阈值   | 结果                     |
| -------------------------------------------------- | ---------- | ------ | ------------------------ |
| parse 10k rows（微信 CSV）                         | ~89 ms     | < 10s  | PASS                     |
| fingerprint 10k（HMAC-SHA256）                     | ~84–135 ms | < 10s  | PASS                     |
| import+proposal finalize（2000 rows 全管道）       | ~90 ms     | < 15s  | PASS                     |
| impact 1k-node chain 传播                          | ~72–122 ms | < 5s   | PASS                     |
| impact 100-node cycle                              | ~1.9 ms    | < 2s   | PASS                     |
| malicious KDF bounds rejection ×100                | ~1.4 ms    | < 0.5s | PASS（先于 KDF，纯校验） |
| large synthetic build+simulate（500 节点 / 1k 边） | ~4.5 s     | < 10s  | PASS                     |
| export 500-node graph                              | ~8 ms      | < 10s  | PASS                     |
| `.depmap` create+open（65536/3/1 golden 参数）     | ~555 ms    | < 10s  | PASS                     |

## PHASE AF — Large Synthetic Smoke

- 500 节点 / 1000 唯一 (from,to) 依赖边：建库 + 模拟 + 导出全部完成，无失败（见
  `tests/perf/performance-smoke.test.ts` 的 large synthetic 用例，30s 测试超时）。
- 10k observations 导入：通过 parser（10k 行 fixtures 生成）+ fingerprint + finalize 全管道验证。
- 100 proposals / 50 groups 层级：proposal 生成在 integration/idempotency 套件覆盖
  （2000 行账单 → 2 proposal 路径），群组确认在 pipeline E2E 覆盖；结构性大规模
  proposals/groups 灌库不影响 kernel（kernel 输入为纯内存数组，复杂度 O(V·E) 波次）。

## 观察

- Argon2id 65536/3/1 单次 ~0.5s：导出/导入的 UX 预期按“秒级”设计，不可调低（协议冻结）。
- Impact kernel 波次 BFS 在 1k 节点深链 <150ms，无退化迹象。
