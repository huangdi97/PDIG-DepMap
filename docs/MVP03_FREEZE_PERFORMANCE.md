# MVP03_FREEZE_PERFORMANCE.md — 性能冻结记录（Freeze §48）

> 环境：Windows 10.0.26200 / Node v22.15.0。判定口径：明显退化检查，非绝对毫秒门。
> 日期：2026-09-13 Freeze 轮实跑。

## Freeze 轮实测

| 场景 | Freeze 实测 | 收口轮基线 | 判定 |
|---|---|---|---|
| 100 ChangePlans create+rebase | ~0.9 s | 0.9 s | 无退化 |
| 1k+ TimelineItems 投影（确定性两次一致） | 14 ms | 14 ms | 无退化 |
| **10k TimelineItems 投影 + 排序确定性**（Freeze 新增） | **83 ms** | —（新增 heavy smoke） | 通过（<10s 预算） |
| 500 open Drifts 检测+列举 | ~4.5 s（宽预算 10s，Windows fsync 波动） | 4.5 s | 无退化 |
| 500 DiscoveryCandidates upsert | ~2.3 s | 2.3 s | 无退化 |
| 1k-node Graph rebase 分析 | 76 ms | 66–87 ms | 波动内 |
| 既有 perf smoke（parse/fingerprint/impact/crypto/10k 导入） | 全部在预算内（check:full 实跑） | 见 docs/PERFORMANCE_BASELINE.md | 无退化 |

## 结论

PERFORMANCE = PASS：全部场景相对 MVP03 收口轮基线无退化；10k timeline heavy smoke
一次通过。无 premature optimization 引入。
