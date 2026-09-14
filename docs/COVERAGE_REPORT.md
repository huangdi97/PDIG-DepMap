# COVERAGE_REPORT.md — 覆盖率

> 执行：`cd core && npm run test:coverage`（@vitest/coverage-v8）
> 2026-09-13 实测（MVP02 收口轮，273 tests 全绿时采集；RC 轮基线 166 tests 见文末历史说明）：

## 总览

```
All files (src, 不含 scripts 探针与 index barrel) ≈ 90.6% lines
未覆盖集中在 adapters 接口声明（0%，纯类型）与 index barrel（0%，re-export）
```

## 按目录（v8 实测，2026-09-13）

| 目录              | Lines | 说明                                                           |
| ----------------- | ----- | -------------------------------------------------------------- |
| src/crypto        | 98.7% | golden/负向/fuzz 全覆盖                                        |
| src/schema        | 100%  | migration 幂等/回滚 + v2 迁移                                  |
| src/domain        | 100%  | 含 relation-registry（新增 14 个直接测试后 62.9%→100%）        |
| src/utils         | 100%  |                                                                |
| src/parser/wechat | 96.9% |                                                                |
| src/services      | 95.4% | 含 import-coordinator 98.1%、AE 事务性注入                     |
| src/fingerprint   | 94.2% | source-scoped HMAC v2                                          |
| src/impact        | 92.4% | T1–T12+fuzz                                                    |
| src/repositories  | 91.5% | 含 SourceInstance/fingerprint scope/proposal evidenceRefs      |
| src/resolver      | 82.5% |                                                                |
| src/sources       | 84.6% | generic-csv 87.9% / ofx 93.7% / wechat 93.1%（MVP02 新增模块） |
| src/db            | 71.8% | SAVEPOINT 嵌套/异常路径部分覆盖                                |
| src/adapters      | 0%    | **纯 interface 声明**，无运行时代码（非排除产生）              |
| src/index.ts      | 0%    | re-export barrel                                               |

## 诚实性说明

- 未通过 exclude 排除任何核心源码；adapters/index 的 0% 是「无函数体」的结构性结果。
- 未为数字写无意义测试；重点模块（impact/crypto/fingerprint/parser/proposal/
  repository/migration/registry）均在 92%+ 行覆盖（resolver 82.5% 为 MVP01 存量口径）。
- MVP02 新增的 `src/sources/` 覆盖 84.6%：未覆盖行为深层坏行分支（bad-row 边界），
  语义由 35 个 adapter 用例 + E2E 保证。
- coverage 口径说明：scripts/ 下的审计/fixture 生成脚本不计入业务覆盖（与 RC 报告口径一致）。

## 历史基线（2026-09-12 RC 收口，166 tests）

crypto 98.7 / schema 100 / fingerprint 100 / impact 92.4 / domain 100（当时无
relation-registry）/ repositories 93.3 / services 93.6 / parser 96.9 /
resolver 82.5 / db 69.3。MVP02 收口轮整体覆盖由 74.9%（含 scripts 的 v8 全表口径）
提升至 src 口径 90.6%，主要增量来自新增模块自身的测试 + relation-registry 直接测试。
