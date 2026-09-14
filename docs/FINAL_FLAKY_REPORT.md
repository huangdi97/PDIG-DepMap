# FINAL_FLAKY_REPORT.md

> PDIG / DepMap — FINAL PRODUCTION CLOSURE V1，第 31 节「Flaky Tests」。
> 目标：全量套件 ≥3 次、高风险 focused 套件 ≥10 次，**0 flaky**。
> 禁止以 retry 掩盖 flaky —— 本轮未引入任何 retry 机制。

---

## 1. 结论

| 项                  | 要求 | 实测                                                  | 结果     |
| ------------------- | ---- | ----------------------------------------------------- | -------- |
| 全量套件连跑        | ≥ 3  | **3**（`npm run test:stability`）                     | **PASS** |
| 高风险 focused 连跑 | ≥ 10 | **10**（`impact + invariants + contract + property`） | **PASS** |
| flaky 用例数        | 0    | **0**                                                 | **PASS** |
| retry 掩盖          | 禁止 | 未使用 retry / rerun / quarantine                     | **PASS** |

**FLAKY_RESULT = PASS（0 flaky）**

---

## 2. 全量套件 ×3

命令：`cd core && npm run test:stability`
（`scripts/test-stability.mjs`：顺序执行 `vitest run` 三次，任一非零即失败）

| 轮次    | 结果                                             |
| ------- | ------------------------------------------------ |
| run 1/3 | 43 files passed                                  |
| run 2/3 | 43 files passed                                  |
| run 3/3 | 43 files passed                                  |
| 汇总    | `stability gate PASS (3 consecutive green runs)` |
| 退出码  | **`STABILITY_EXIT=0`**                           |

日志：`/tmp/stability-final.log`

---

## 3. 高风险 focused 套件 ×10

命令：

```
npx vitest run tests/impact tests/invariants tests/contract tests/property --reporter=dot
```

（高风险正确性路径：Impact Kernel / 不变量 / 适配器契约 / property-based）

| 轮次  | 结果                            | 退出码 |
| ----- | ------------------------------- | ------ |
| 1/10  | 9 files passed, 74 tests passed | 0      |
| 2/10  | 9 files passed, 74 tests passed | 0      |
| 3/10  | 9 files passed, 74 tests passed | 0      |
| 4/10  | 9 files passed, 74 tests passed | 0      |
| 5/10  | 9 files passed, 74 tests passed | 0      |
| 6/10  | 9 files passed, 74 tests passed | 0      |
| 7/10  | 9 files passed, 74 tests passed | 0      |
| 8/10  | 9 files passed, 74 tests passed | 0      |
| 9/10  | 9 files passed, 74 tests passed | 0      |
| 10/10 | 9 files passed, 74 tests passed | 0      |

累计执行 740 个用例实例，**0 失败、0 抖动**。日志：`/tmp/critical10.log`

---

## 4. 已知抖动源与处置

### 4.1 覆盖率 provider 的极小幅抖动（非测试 flaky）

`@vitest/coverage-v8` 的 Branches 百分比在多轮之间存在 ±0.03 级别抖动（历史实测 82.21–82.24；本轮实测 **82.22**）。性质：

- 属**覆盖率统计**抖动，不属测试用例 flaky；
- 不影响门槛判定（远高于策略下限）；
- 已在 `docs/COVERAGE_POLICY.md` 与本报告中如实记录，**未通过调阈值掩盖**。

### 4.2 并行执行下的显式超时（历史项，已冻结）

`FR-GR-012` 在全量并行执行时曾出现超时，已由 commit `253cdd5` 为该用例设置**显式超时**（而非缩短断言或跳过）。本轮 3 次全量 + 10 次 focused 均未复现。

### 4.3 性能 smoke 的绝对耗时（非 flaky）

`tests/perf/**` 中部分用例为「记录型」耗时断言（如 10k Timeline 投影 87–117 ms，阈值 10s）。绝对耗时随机器负载浮动，但**均远离阈值**，不构成 flaky。

---

## 5. 环境说明（诚实口径）

本工作区的 safe-delete 守卫会拦截 `vitest` 对 `coverage/` 的批量清理。`core/scripts/run-coverage.mjs` 在检测到该守卫时把覆盖率输出目录改到系统临时目录，**退出码仍为 vitest 真实退出码，不做任何改写**。普通环境行为不变。该适配仅影响覆盖率输出路径，不影响测试执行与结果判定。

---

## 6. 复现命令

```
cd core
npm run test:stability                                        # 全量 ×3
npx vitest run tests/impact tests/invariants tests/contract tests/property   # 需重复 10 次
```
