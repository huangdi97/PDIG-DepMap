# ENGINEERING_BASELINE_V1_REPORT.md — 最终报告（2026-09-13）

> 判定范围：代码侧工程基线（Core）。平台编译/真机（B1–B3）与 Real Data 不在本基线
> 判定范围，分别 BLOCKED / NOT_RUN，不虚报。

## 总判定

| 项                          | 值                                                                                                                                                   |
| --------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| **ENGINEERING_BASELINE_V1** | **PASS**                                                                                                                                             |
| FORMAT                      | PASS（prettier 3.9.6，.prettierignore 收敛）                                                                                                         |
| LINT                        | PASS（eslint 10 typed，0 errors / 0 warnings，无文件级豁免）                                                                                         |
| TYPECHECK                   | PASS（strict 全开 + noUncheckedIndexedAccess + **exactOptionalPropertyTypes**）                                                                      |
| ARCHITECTURE                | PASS（35 files，4 边界规则）                                                                                                                         |
| CIRCULAR_DEPS               | **0**（check:architecture 内置 DFS 环检测）                                                                                                          |
| DEAD_CODE                   | PASS（6 处真死代码删除，零引用验证；docs/DEAD_CODE_AUDIT.md）                                                                                        |
| UNIT_TESTS                  | PASS                                                                                                                                                 |
| INTEGRATION_TESTS           | PASS                                                                                                                                                 |
| CONTRACT_TESTS              | PASS（EvidenceSourceAdapterContract ×3 + RepositoryContract harness + crypto golden）                                                                |
| MIGRATION_TESTS             | PASS（T1–T10 + payload v1→v2；长期 Gate 规则入 TEST_STRATEGY）                                                                                       |
| INVARIANT_TESTS             | PASS（INV1–INV14 集中套件）                                                                                                                          |
| DETERMINISM                 | PASS（impact/parser/proposal ×50 + contract ×10/adapter + K6 ×20）                                                                                   |
| IDEMPOTENCY                 | PASS（migration ×50 / 重复导入 / 重放确认 / retire→reactivate / export-import）                                                                      |
| PROPERTY_TESTS              | PASS（fast-check P1–P6，seed=20260913 + 既有 mulberry32 套件）                                                                                       |
| FUZZ_TESTS                  | PASS（crypto 结构化变异矩阵 F1–F6 + 既有 30 轮单字符 fuzz + CSV ×100）                                                                               |
| MUTATION_TESTS              | **PARTIAL_WITH_REPORT**（Stryker 532 mutants：kernel 55.96% / registry 96.88% + 人工变异 3/3 KILLED + 补测 +7 killed；docs/MUTATION_TEST_REPORT.md） |
| COVERAGE                    | PASS（src 口径 line **94.67%** / branch 81.59%；双层 Gate 见 docs/COVERAGE_POLICY.md）                                                               |
| FAIL_CLOSED                 | PASS（20 行矩阵，docs/FAIL_CLOSED_MATRIX.md）                                                                                                        |
| SECRET_SCAN                 | PASS（283 files，0 production secrets）                                                                                                              |
| NETWORK_AUDIT               | PASS（业务网络调用 = 0，check:network 84 files）                                                                                                     |
| LOGGING_AUDIT               | PASS（src 零 console；docs/LOGGING_POLICY.md）                                                                                                       |
| PRIVACY_AUDIT               | PASS（S-01–S-15 矩阵全 NO/0）                                                                                                                        |
| DEPENDENCY_AUDIT            | PASS（check:deps：树健康 + lockfile 同步；audit 3 moderate dev-only 已登记）                                                                         |
| LICENSE_AUDIT               | PASS（全 MIT / Apache-2.0；THIRD_PARTY_NOTICES.md 刷新）                                                                                             |
| PERFORMANCE_BASELINE        | PASS（12 场景固化，docs/PERFORMANCE_BASELINE.md）                                                                                                    |
| MEMORY_AUDIT                | PASS（生命周期政策 docs/MEMORY_DATA_LIFETIME.md + 10k 级 perf 兼冒烟）                                                                               |
| CLEAN_INSTALL               | PASS（rm node_modules → npm ci → check 全绿）                                                                                                        |
| CLEAN_CLONE                 | PASS（git archive → temp → npm ci → check 全绿）                                                                                                     |
| README_COMMANDS             | PASS（npm ci / test / typecheck / check / check:full / validate-real-bill 实跑或本轮全链路覆盖验证）                                                 |
| AGENT_PROTOCOL              | PASS（docs/AGENT_DEVELOPMENT_PROTOCOL.md）                                                                                                           |
| DEFINITION_OF_DONE          | PASS（docs/DEFINITION_OF_DONE.md）                                                                                                                   |
| STABILITY（flaky）          | PASS（3 连跑全绿，0 retry；docs/FLAKY_TEST_REPORT.md）                                                                                               |

## 回归判定

| 项               | 值                                                     |
| ---------------- | ------------------------------------------------------ |
| MVP01_REGRESSION | PASS（既有 106→273 基线用例全部保留并通过）            |
| MVP02_REGRESSION | PASS（273 基线全部通过；本轮 +51 用例至 324，28 文件） |

## 平台状态（分别声明，不合并）

| 平台            | IMPLEMENTED | STATIC_AUDITED | COMPILED                                | TESTED           | DEVICE_VERIFIED |
| --------------- | ----------- | -------------- | --------------------------------------- | ---------------- | --------------- |
| Android         | YES         | YES            | **BLOCKED（B1：无 JDK17/Android SDK）** | BLOCKED（B1）    | NO              |
| HarmonyOS       | YES         | YES            | **BLOCKED（B2：无 DevEco）**            | BLOCKED（B2）    | NO              |
| iOS             | YES         | YES            | **BLOCKED（B3：无 macOS/Xcode）**       | BLOCKED（B3）    | NO              |
| Core（Node 22） | YES         | YES            | YES（typecheck）                        | YES（324 tests） | N/A             |

## Real Data Gate

- **NOT_RUN**（固定；无真实账单。`core/scripts/validate-real-bill.ts` 已就绪，
  等待用户提供 local_private 账单后执行双 Gate。）

## 本轮生产代码变更（均有回归）

1. `src/impact/kernel.ts` — 初始 unavailable 状态键值级去重（Set 值相等对象去重不彻底，
   导致 processedKeys 可能重复；由 fast-check P1 发现并修复）。
2. `exactOptionalPropertyTypes` 开启 — 修复 8 处真实类型问题（domain/source、
   fingerprint 输入、OfxTransaction、evidence 输入）。
3. 死代码删除 6 处（无行为影响，零引用验证）。

## 本轮新增自动化

- 测试：invariants（11）/ contract（26）/ property（6）/ crypto container-mutation（5）/
  kernel mutation-baseline（3）= +51 用例（273 → 324）
- 脚本：check:network / check:deps / test:stability / architecture 循环依赖检测
- 命令：check（+network gate）、check:full（+db-integrity/coverage/perf/deps）

## 结论

Engineering Baseline V1 代码侧全部 Gate 收口（唯 mutation 为 PARTIAL_WITH_REPORT，
符合 §51 允许口径）。下一轮入口：GOAL_MVP03_LIVING_GRAPH_CHANGE_SAFETY（未启动），
平台编译依赖 B1–B3 外部工具链解除后按 ANDROID/HARMONY/IOS_TOOLCHAIN_SETUP 推进。
