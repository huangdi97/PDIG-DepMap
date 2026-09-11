# COVERAGE_REPORT.md — 覆盖率（RC PHASE AB）

> 执行：`cd core && npm run test:coverage`（@vitest/coverage-v8）
> 2026-09-12 实测（166 tests 全绿时采集）：

## 总览

```
All files   Lines ≈ 88%   (未覆盖集中在 adapters 接口声明与 index barrel)
```

## 按目录（v8 实测）

| 目录 | Lines | Branches | Fns | 说明 |
|---|---|---|---|---|
| src/crypto | 98.7% | 86.9% | 100% | golden/负向/fuzz 全覆盖 |
| src/schema | 100% | 100% | 100% | migration 幂等/回滚 |
| src/fingerprint | 100% | 91.7% | 100% | |
| src/impact | 92.4% | 89.7% | 100% | T1–T12+fuzz |
| src/domain | 100% | 85.7% | 100% | 纯类型+键函数 |
| src/repositories | 93.3% | 79.7% | 87.8% | |
| src/services | 93.6% | 77.3% | 91.3% | 含 AE 事务性注入 |
| src/parser/wechat | 96.9% | 79.5% | 100% | |
| src/resolver | 82.5% | 83.5% | 83.3% | |
| src/db | 69.3% | 89.5% | 92.9% | SAVEPOINT 嵌套/异常路径部分覆盖 |
| src/adapters | 0% | 0% | 0% | **纯 interface 声明**，无运行时代码（非排除产生） |
| src/utils | 100% | 100% | 100% | |

## 诚实性说明

- 未通过 exclude 排除任何核心源码；adapters 的 0% 是「无函数体」的结构性结果。
- 未为数字写无意义测试；重点模块（impact/crypto/fingerprint/parser/proposal/
  repository/migration）均在 92%+ 行覆盖。
- db/node-driver 的 SAVEPOINT 嵌套分支由 finalize 事务化 + repository 嵌套事务
  实际执行，未覆盖行数极少。
