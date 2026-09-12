# COVERAGE_POLICY.md — 覆盖率政策（Engineering Baseline V1）

> 口径：v8 coverage，`include: src/**`（纯类型文件 `src/adapters/interfaces.ts`、
> `src/db/driver.ts` 排除 —— 声明文件无执行语句，计入只会制造噪音）。
> 实测：2026-09-13，`npm run test:coverage`，321 tests。

## 双层 Gate

- **BASELINE Gate（强制，禁止回归）**：任何模块不得低于下表 Baseline；整体 src line 不得低于 92%。
- **TARGET Gate（目标，缺口需说明理由）**：下表 Target 列。

| 模块 | line | branch | Baseline line | Baseline branch | Target | 缺口说明 |
|---|---|---|---|---|---|---|
| crypto（depmap/golden/jcs） | 99.34 | 89.18 | 99 | 89 | 95/95 | jcs 负向分支（非法输入抛错路径）已测主路径 |
| schema / migrations | 100 | 100 | 100 | 100 | 100/100 | — |
| domain（types/registry/source） | 100 | 96.42 | 100 | 96 | 100/100 | — |
| fingerprint | 100 | 95.23 | 100 | 95 | 100/95 | — |
| impact / kernel | 92.63 | 88.88 | 92 | 88 | 95/95 | 缺口为**单 capability 下构造性不可达分支**：target 排序的 capability tie（kernel 只评估 payment，恒等）与 severity 表 `degraded` case（kernel 永不产出该状态）。关闭路径 = MVP03 多 capability 扩展时收紧，或删除防御分支；本轮不为数字写无意义测试 |
| services（coordinator/confirmation/serialize） | 95.41 | 78.02 | 95 | 78 | 95/85 | branch 缺口为 mapping/路由边缘组合，随 Adapter 扩展补 |
| repositories | 92.52 | 78.35 | 92 | 78 | 95/85 | — |
| parser/wechat | 96.9 | 78.02 | 96 | 78 | 95/90 | — |
| resolver | 82.53 | 84.14 | 82 | 84 | 90/90 | fuzzy 边缘组合 |
| sources（generic-csv/ofx） | 84.61 | 83.33 | 84 | 83 | 90/90 | MVP02 新模块；坏行/编码边缘组合 |
| **src 整体** | ≈93 | ≈84 | **92** | 84 | 95/90 | — |

## 规则

1. 禁止 exclude 任何 Core 源文件来制造高覆盖率（上面两个纯类型文件是仅有的白名单）。
2. 禁止写只碰行不验证语义的测试凑数（由 mutation baseline 验证测试有效性，见
   `docs/MUTATION_TEST_REPORT.md`）。
3. Baseline Gate 建议由 CI/本地 `check:full` 的 coverage 步骤人工比对本表执行；
   数值调整必须同步更新本文件并在 commit message 注明理由。
4. UI（app/）不适用本表（未编译，无法产出可信运行时覆盖率）。
