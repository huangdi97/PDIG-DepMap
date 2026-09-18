# FLAKY_TEST_REPORT.md — 稳定性报告（Engineering Baseline V1，2026-09-13）

## 方法

- 命令：`npm run test:stability`（`core/scripts/test-stability.mjs`：完整套件连续 **3 轮**，
  每轮独立 vitest forks 进程；无 retry 配置 —— 禁止 retry 掩盖）。
- 关键域（Impact / Parser / Migration / Crypto / Multi-source E2E）除 ×3 外，另有
  既有确定性/幂等重复用例覆盖：impact ×50、migration ×50、parser ×50、CSV ×50、
  dual-source replay ×20、crypto mutation 30+20 轮、property numRuns 150–200。

## 结果

| 轮  | Test Files | 结果 |
| --- | ---------- | ---- |
| 1   | 28 passed  | 绿   |
| 2   | 28 passed  | 绿   |
| 3   | 28 passed  | 绿   |

**stability gate PASS（3 consecutive green runs），0 flaky，0 retry。**

## 历史 flaky

- MVP01 轮曾修复 1 例 flaky（见 370b0f6 commit 记录）；本轮 3 连跑未复现。
- 已知非 flaky 的「慢」用例：perf smoke（约 12s）依赖真实计时，跨机器波动大 ——
  判定口径见 docs/PERFORMANCE_BASELINE.md（宽松预算，非精确毫秒断言）。

## 政策

1. 发现 flaky 必须修复根因（时序/共享状态/未清理资源），禁止 `--retry` 掩盖。
2. 每个 milestone 收口轮跑一次 `test:stability` 并刷新本报告。
