# FINAL_TEST_MATRIX.md

> PDIG / DepMap — FINAL PRODUCTION CLOSURE V1。
> 记录最终测试套件、命令、计数、平台测试、手工/设备测试、受阻测试。
> 全部数字为 2026-09-14 本轮实测。

---

## 1. 套件总览

| 项        | 实测                                                 |
| --------- | ---------------------------------------------------- |
| 测试文件  | **43**                                               |
| 用例总数  | **453**                                              |
| 运行器    | `vitest 3.1.0`（`pool: forks`，`environment: node`） |
| 全量命令  | `npm test`（`vitest run`）                           |
| 快速 Gate | `npm run check` → **EXIT=0**                         |
| 完整 Gate | `npm run check:full` → **EXIT=0**                    |

---

## 2. 套件分类矩阵

| 类别                          | 目录                                                                           | 文件数           | 覆盖内容                                                                                                 |
| ----------------------------- | ------------------------------------------------------------------------------ | ---------------- | -------------------------------------------------------------------------------------------------------- |
| **Unit**                      | `tests/unit/`                                                                  | 7                | `crypto-negative`、`db-integrity`、`determinism`、`idempotency`、`negative`、`property-fuzz`、`resolver` |
| **Integration**               | `tests/integration/`                                                           | 4                | `pipeline`、`multi-source-e2e`、`graph-payload-v2`、`coverage-semantics`                                 |
| **Contract**                  | `tests/contract/`                                                              | 2                | `adapter-contract`（WeChat / CSV / OFX）、`repository-contract`                                          |
| **Migration**                 | `tests/repository/`                                                            | 2（迁移）        | `migration.test.ts`、`migration-v3.test.ts`                                                              |
| **Invariant**                 | `tests/invariants/`                                                            | 2                | `invariants`、`mvp03-invariants`                                                                         |
| **Property**                  | `tests/property/`                                                              | 3                | `impact-properties`、`mvp03-properties`、`graph-revision-freeze`                                         |
| **Fuzz**                      | `tests/unit/property-fuzz.test.ts` + `tests/crypto/container-mutation.test.ts` | 2                | CSV/OFX/JSON/depmap 变异、错误 UTF-8、畸形头、极端长度                                                   |
| **Mutation**                  | `tests/impact/kernel-mutation-baseline.test.ts` + `stryker.conf.mjs`           | 1 + 配置         | Impact Kernel、RelationRegistry                                                                          |
| **Regression**                | `tests/repository/` + `tests/services/`                                        | 14               | Repository 与 Service 回归                                                                               |
| **Crypto**                    | `tests/crypto/`                                                                | 2                | `depmap`、`container-mutation`                                                                           |
| **Parser / Sources**          | `tests/parser/`、`tests/sources/`                                              | 3                | `wechat`、`generic-csv`、`ofx-qfx`                                                                       |
| **Domain**                    | `tests/domain/`                                                                | 1                | `relation-registry`                                                                                      |
| **Performance**               | `tests/perf/`                                                                  | 3                | `performance-smoke`、`mvp03-perf`、`timeline-10k-freeze`                                                 |
| **UI component / Navigation** | —                                                                              | **0（BLOCKED）** | 见 §5                                                                                                    |
| **Synthetic E2E**             | `tests/integration/multi-source-e2e.test.ts`                                   | 1                | 合成数据端到端                                                                                           |
| **Upgrade**                   | `tests/repository/migration.test.ts`                                           | 1                | 旧 DB / 旧 payload 升级                                                                                  |
| **Backup / Restore**          | `tests/crypto/depmap.test.ts`                                                  | 1                | 容器创建/打开/负向                                                                                       |
| **Security**                  | `tests/unit/crypto-negative.test.ts` + `check:secrets` + `check:network`       | —                | 见 `FINAL_SECURITY_REPORT.md`                                                                            |
| **Platform**                  | —                                                                              | **0（BLOCKED）** | Android/Harmony/iOS 均受阻，见 §5                                                                        |

---

## 3. 逐文件清单（43）

```
contract/adapter-contract.test.ts
contract/repository-contract.test.ts
crypto/container-mutation.test.ts
crypto/depmap.test.ts
domain/relation-registry.test.ts
impact/kernel-mutation-baseline.test.ts
impact/kernel.test.ts
integration/coverage-semantics.test.ts
integration/graph-payload-v2.test.ts
integration/multi-source-e2e.test.ts
integration/pipeline.test.ts
invariants/invariants.test.ts
invariants/mvp03-invariants.test.ts
parser/wechat.test.ts
perf/mvp03-perf.test.ts
perf/performance-smoke.test.ts
perf/timeline-10k-freeze.test.ts
property/graph-revision-freeze.test.ts
property/impact-properties.test.ts
property/mvp03-properties.test.ts
repository/graph-revision.test.ts
repository/migration-v3.test.ts
repository/migration.test.ts
repository/proposal-lifecycle.test.ts
repository/repositories.test.ts
repository/source-instance-scope.test.ts
services/action-verification.test.ts
services/change-plan-rebase.test.ts
services/discovery-candidate.test.ts
services/plan-readiness-coverage.test.ts
services/plan-readiness-freeze.test.ts
services/reality-drift.test.ts
services/scenario-timeline.test.ts
services/state-machine-freeze.test.ts
sources/generic-csv.test.ts
sources/ofx-qfx.test.ts
unit/crypto-negative.test.ts
unit/db-integrity.test.ts
unit/determinism.test.ts
unit/idempotency.test.ts
unit/negative.test.ts
unit/property-fuzz.test.ts
unit/resolver.test.ts
```

---

## 4. 命令矩阵

| 命令                                        | 含义                                                                                                   | 本轮结果                                                           |
| ------------------------------------------- | ------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------ |
| `npm run check`                             | format:check + **format:docs:check** + lint + typecheck + test + architecture + network + secrets + ui | **EXIT=0**                                                         |
| `npm run check:full`                        | `check` + db-integrity + coverage + perf + deps                                                        | **EXIT=0**                                                         |
| `npm test`                                  | 全量单元/集成/契约/迁移/不变量/property/fuzz/perf                                                      | 453 passed / 43 files                                              |
| `npm run test:coverage`                     | 覆盖率                                                                                                 | 453 passed；Stmts 93.82 / Branch 82.22 / Funcs 94.55 / Lines 93.82 |
| `npm run test:perf`                         | 性能                                                                                                   | 16 passed / 3 files                                                |
| `npm run test:stability`                    | 全量 ×3                                                                                                | 3 green runs，EXIT=0                                               |
| `npm run check:invariants`                  | 不变量                                                                                                 | PASS                                                               |
| `npm run check:contract`                    | 契约                                                                                                   | PASS                                                               |
| `npm run check:property`                    | property                                                                                               | PASS                                                               |
| `npm run check:db-integrity`                | DB 完整性                                                                                              | 6 passed                                                           |
| `npm run check:architecture`                | 架构 + 循环依赖                                                                                        | PASS（48 files，circular = 0）                                     |
| `npm run check:network`                     | 网络零调用                                                                                             | PASS（118 files，0 原语）                                          |
| `npm run check:secrets`                     | 密钥扫描                                                                                               | PASS（404 files，0 production secrets）                            |
| `npm run check:ui`                          | UI 静态门禁 U1–U9                                                                                      | PASS（30 `.uvue`，24 pages，5 components）                         |
| `npm run check:deps`                        | 依赖 + license                                                                                         | PASS（audit 3 moderate dev-only）                                  |
| `npm run format:docs` / `format:docs:check` | docs 格式（**本轮新增**）                                                                              | EXIT=0，幂等                                                       |

---

## 5. 平台 / 设备测试矩阵

| 平台                      | 源码级           | 编译    | 单元测试       | 真机   | 说明                                                                                |
| ------------------------- | ---------------- | ------- | -------------- | ------ | ----------------------------------------------------------------------------------- |
| Core（Node）              | —                | **YES** | **YES（453）** | N/A    | 全绿                                                                                |
| Android（Kotlin 核心）    | **YES**          | **NO**  | **NO**         | **NO** | 无可用 Gradle 发行版 + 构建依赖 CDN 不可达；`compileSdk 34` 未安装；无真机          |
| HarmonyOS（ArkTS 适配器） | **YES**          | **NO**  | **NO**         | **NO** | hvigor 已真实执行，卡在 SDK 组件（API 13）解析；`platforms/harmonyos/` 为适配器片段 |
| iOS（Swift 核心）         | **YES**          | **NO**  | **NO**         | **NO** | 无 macOS / Xcode（Windows 环境，预期内）                                            |
| UI（uni-app x 24 页）     | **YES（U1–U9）** | **NO**  | **NO**         | **NO** | 无 HBuilderX / uni-app x 工具链（B10）                                              |

**手工/设备测试执行数 = 0**（无可执行环境）。**未以任何方式伪造设备证据。**

---

## 6. 受阻测试清单（明确 BLOCKED，不计入 PASS）

| 测试类                                                                   | 受阻原因                           | 状态        |
| ------------------------------------------------------------------------ | ---------------------------------- | ----------- |
| Android `DepmapContainerV1GoldenTest.kt`（golden 互操作）                | 无 Gradle 发行版 + 依赖不可获取    | **BLOCKED** |
| Android SQLCipher / Keystore / BiometricPrompt 真机验证                  | 无真机、无签名                     | **BLOCKED** |
| HarmonyOS ArkTS 编译 + ArkData / HUKS / auth 运行验证                    | SDK 组件需 DevEco SDK Manager 同步 | **BLOCKED** |
| iOS `swift test`（`DepmapContainerV1Tests.swift`）                       | 无 macOS / Xcode                   | **BLOCKED** |
| UI 组件测试 / 导航测试                                                   | 无 uni-app x 编译环境              | **BLOCKED** |
| 真机 E2E（安装 → 引导 → 解锁 → 导入 → 计划 → 时间轴 → 备份/恢复 → 重启） | 无设备                             | **BLOCKED** |
| Real Data 双 Gate（`REAL_DATA_CORRECTNESS` / `REAL_DATA_VALUE`）         | 无真实账单                         | **NOT_RUN** |

---

## 7. 覆盖率门槛核对

| 模块            | 策略门槛 | 实测   | 结果 |
| --------------- | -------- | ------ | ---- |
| 全量 Statements | —        | 93.82% | —    |
| 全量 Branches   | —        | 82.22% | —    |
| 全量 Functions  | —        | 94.55% | —    |
| 全量 Lines      | —        | 93.82% | —    |

> 模块级门槛明细见 `docs/COVERAGE_POLICY.md` 与 `docs/COVERAGE_REPORT.md`。本轮 Branches 实测 82.22，落在历史区间 82.21–82.24 内（v8 provider 抖动，非测试不稳定）。
