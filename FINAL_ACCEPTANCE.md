# FINAL_ACCEPTANCE.md

> PDIG / DepMap — FINAL PRODUCTION CLOSURE V1 最终验收表（A–S）。
> 状态枚举统一为：**PASS / FAIL / BLOCKED / NOT_RUN / PARTIAL_WITH_REPORT**。
> 任何 PASS 必须对应命令、退出码、测试结果、产物或真实证据。

---

## 汇总

| 段  | 域               | 状态                                  |
| --- | ---------------- | ------------------------------------- |
| A   | Core Correctness | **PASS**                              |
| B   | Code Style       | **PASS**                              |
| C   | Type Safety      | **PASS**                              |
| D   | Architecture     | **PASS**                              |
| E   | Testing          | **PASS（Core）/ BLOCKED（平台）**     |
| F   | Migration        | **PASS**                              |
| G   | Security         | **PASS（代码侧）**                    |
| H   | Privacy          | **PASS（代码侧）/ BLOCKED（URL）**    |
| I   | Performance      | **PASS**                              |
| J   | UI/UX            | **PASS（源码级）**                    |
| K   | Frontend         | **PASS（源码级）**                    |
| L   | Android          | **SOURCE_READY PASS / BUILD BLOCKED** |
| M   | Harmony          | **SOURCE_READY PASS / BUILD BLOCKED** |
| N   | iOS              | **SOURCE_READY PASS / BUILD BLOCKED** |
| O   | Backup/Restore   | **PASS（Core）/ BLOCKED（设备）**     |
| P   | Release          | **PARTIAL_WITH_REPORT**               |
| Q   | Store            | **REQUIRES_USER_RELEASE_DECISION**    |
| R   | Real Data        | **NOT_RUN**                           |
| S   | Final            | **PARTIAL_WITH_REPORT**               |

---

## A. Core Correctness — PASS

| 验收项                                              | 证据                                                      | 状态 |
| --------------------------------------------------- | --------------------------------------------------------- | ---- |
| Observation ≠ Reality / Proposal ≠ Reality          | invariant + negative 套件                                 | PASS |
| DiscoveryCandidate 不进入 Impact                    | `invariants/mvp03-invariants.test.ts`                     | PASS |
| RealityDrift 不直接 mutate Reality                  | 同上 + `services/reality-drift.test.ts`                   | PASS |
| absence 不能 retire 已确认 Reality                  | 同上                                                      | PASS |
| machine inference 不能设置 `required`               | 同上 + `domain/relation-registry.test.ts`                 | PASS |
| 不同 SourceInstance 不共享指纹命名空间              | `repository/source-instance-scope.test.ts` + `sources/*`  | PASS |
| 多源 Evidence 增加溯源而非确定性                    | `contract/adapter-contract.test.ts`                       | PASS |
| graphRevision 仅在 Reality mutation 时 +1（同事务） | `property/graph-revision-freeze.test.ts`（40 组随机序列） | PASS |
| done ≠ verified（两段式）                           | `services/action-verification.test.ts`                    | PASS |
| TimelineItem ≠ Truth（纯投影）                      | `services/scenario-timeline.test.ts`                      | PASS |
| ScenarioCoverage ≠ PlanReadiness                    | `services/plan-readiness-coverage.test.ts`                | PASS |
| DEPMAP_CONTAINER_V1 兼容                            | golden vector 冻结 + `crypto/depmap.test.ts`              | PASS |
| 全量测试                                            | `npm run check` → **EXIT=0**，453/453                     | PASS |

---

## B. Code Style — PASS

| 验收项                                                                     | 证据                                                   | 状态 |
| -------------------------------------------------------------------------- | ------------------------------------------------------ | ---- |
| `.editorconfig` / `.gitattributes` / `.gitignore`                          | 存在且完备（本轮补 Gradle 条目）                       | PASS |
| prettier 配置                                                              | `core/.prettierrc` + 根 `.prettierrc.json`（本轮新增） | PASS |
| eslint 配置                                                                | `core/eslint.config.js`                                | PASS |
| tsconfig                                                                   | 见 C 段                                                | PASS |
| `format:check`（core）                                                     | EXIT=0                                                 | PASS |
| `format:docs` / `format:docs:check`                                        | EXIT=0，幂等（本轮新增）                               | PASS |
| `lint`                                                                     | 0 error / 0 warning                                    | PASS |
| UTF-8 / LF / final newline / trailing whitespace                           | editorconfig + gitattributes 强制                      | PASS |
| import order / 文件命名 / 目录命名 / Domain 命名 / 测试命名 / fixture 命名 | 一致，无违规                                           | PASS |
| 规则禁用指令                                                               | `eslint-disable*` / `@ts-*` 全仓 **0**                 | PASS |

---

## C. Type Safety — PASS

| 验收项                                                                                                                                      | 证据                             | 状态 |
| ------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------- | ---- |
| `strict` / `noImplicitAny` / `noImplicitReturns` / `noFallthroughCasesInSwitch` / `noUncheckedIndexedAccess` / `exactOptionalPropertyTypes` | `core/tsconfig.json` 全部 `true` | PASS |
| `useUnknownInCatchVariables`                                                                                                                | 由 `strict` 隐含开启             | PASS |
| `any` / `as any` / `unknown as`                                                                                                             | **0 / 0 / 0**                    | PASS |
| 非空断言                                                                                                                                    | **0**                            | PASS |
| `tsc --noEmit`                                                                                                                              | EXIT=0                           | PASS |
| 关键正确性路径无 `any` 绕过                                                                                                                 | 逐模块审计 = 0                   | PASS |
| 是否降低 Core 标准以适配 UTS                                                                                                                | **否**                           | PASS |

详见 `docs/FINAL_TYPE_SAFETY_AUDIT.md`。

---

## D. Architecture — PASS

| 验收项                                                   | 证据                                   | 状态 |
| -------------------------------------------------------- | -------------------------------------- | ---- |
| `check:architecture`                                     | PASS（48 files）                       | PASS |
| circular dependencies                                    | **0**                                  | PASS |
| Domain 不依赖 UI / Core 不依赖 native / UI 不直接 SQLite | 架构检查 + `check-ui` U4               | PASS |
| UI 不实现 PlanReadiness / Impact / Drift 检测            | `rules.uts` 纯镜像 + U4                | PASS |
| UI 不直接改 graphRevision                                | U4 + 服务边界                          | PASS |
| Adapter 不创建 Reality                                   | `contract/adapter-contract.test.ts`    | PASS |
| Timeline 不写 Graph / ScenarioTemplate 不写 Dependency   | invariant 套件                         | PASS |
| Candidate 不进入 Impact                                  | invariant 套件                         | PASS |
| Verification suggestion 不改 Reality                     | `services/action-verification.test.ts` | PASS |
| Crypto / Migration 不依赖 UI                             | 架构检查                               | PASS |

---

## E. Testing — PASS（Core）/ BLOCKED（平台）

| 验收项                                                                               | 证据                                                    | 状态        |
| ------------------------------------------------------------------------------------ | ------------------------------------------------------- | ----------- |
| Unit / Integration / Contract / Migration / Invariant / Property / Fuzz / Regression | 43 文件 / 453 用例全绿                                  | PASS        |
| Mutation（定向人工）                                                                 | 10/10 KILLED，0 critical survived                       | PASS        |
| Mutation（Stryker 重跑）                                                             | 未执行                                                  | **NOT_RUN** |
| Determinism / Idempotency                                                            | `unit/determinism.test.ts` / `unit/idempotency.test.ts` | PASS        |
| Flaky（全量 ×3 + focused ×10）                                                       | 0 flaky                                                 | PASS        |
| UI component / Navigation 测试                                                       | 无运行环境                                              | **BLOCKED** |
| Platform 测试                                                                        | 无构建环境                                              | **BLOCKED** |
| `npm run check` / `check:full`                                                       | EXIT=0 / EXIT=0                                         | PASS        |

---

## F. Migration — PASS

| 验收项                                     | 状态 |
| ------------------------------------------ | ---- |
| fresh → latest                             | PASS |
| v2 → latest / v3 → latest                  | PASS |
| payload v1/v2/v3 迁移                      | PASS |
| 迁移后重启                                 | PASS |
| 幂等 ×50                                   | PASS |
| 失败回滚（无 partial state）               | PASS |
| 未来 schema 拒绝                           | PASS |
| 无孤儿 / ID 全保留 / decision state 全保留 | PASS |
| 升级回归（旧 DB / 旧 `.depmap`）           | PASS |

---

## G. Security — PASS（代码侧）

| 验收项                  | 证据                                                   | 状态        |
| ----------------------- | ------------------------------------------------------ | ----------- |
| Secret scan             | 404 files，**0 production secrets**                    | PASS        |
| 敏感文件未入库          | `git ls-files` 无命中                                  | PASS        |
| Network zero            | 118 files，0 原语；业务网络调用 = 0                    | PASS        |
| Logging                 | `core/src` `console.*` = 0；无敏感字段日志             | PASS        |
| Permissions（最小权限） | Android `USE_BIOMETRIC` only / app `[]` / Harmony `[]` | PASS        |
| Crypto fail-closed      | 变异矩阵全绿；不返回 partial                           | PASS        |
| DB 完整性               | `check:db-integrity` 6 passed                          | PASS        |
| Backup 加密             | 容器 + golden vector                                   | PASS        |
| Dependency（runtime）   | 0 critical / 0 high；3 moderate dev-only               | PASS        |
| License                 | runtime MIT；无未知 license                            | PASS        |
| Supply chain            | lockfile 提交；无 file 依赖 / 无 CDN script            | PASS        |
| 设备端安全验证          | 无构建环境                                             | **BLOCKED** |

详见 `FINAL_SECURITY_REPORT.md`。

---

## H. Privacy — PASS（代码侧）/ BLOCKED（URL）

| 验收项                                   | 状态                                         |
| ---------------------------------------- | -------------------------------------------- |
| 本地数据原则（无云端、无账号）           | PASS（代码一致）                             |
| 披露事实矩阵                             | PASS（`store/PRIVACY_DISCLOSURE_MATRIX.md`） |
| 用户须知（非银行 / 非顾问 / 非自动支付） | PASS（`docs/USER_NOTICE_DRAFT.md`）          |
| 隐私政策 URL                             | **BLOCKED（B12）**                           |
| iOS 相机用途声明与实现一致               | **待复核（U-2）**                            |

---

## I. Performance — PASS

| 验收项                                                                                                                                            | 状态                                           |
| ------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------- |
| 固定 synthetic workload（10k 行 / 500 Nodes / 1k Dependencies / 100 Plans / 500 Drifts / 500 Candidates / 1k Timeline / 1k-node Impact / Rebase） | PASS（16 perf tests）                          |
| 只防退化                                                                                                                                          | PASS（阈值宽松，实测远离阈值）                 |
| 内存（导入后不被 global state 长期引用）                                                                                                          | PASS（源码级；`docs/MEMORY_DATA_LIFETIME.md`） |

---

## J. UI/UX — PASS（源码级）

IA / 首页 3 秒问答 / 场景中心（仅 active）/ ChangePlan 四态区分 / Readiness 文案（无「完全安全」「100%」）/ Drift「可能」/ Candidate「待确认」/ Coverage 无评分 / Timeline 纯投影 / Import 如实标注 / Backup 如实标注 / Settings 无假开关 / Onboarding / 设计系统 token 唯一真相源 / U3 工程词 0 命中 / U8 每页空错加载态。
Accessibility = **PARTIAL_WITH_REPORT**；Responsive = **PARTIAL_WITH_REPORT**；Visual QA = **NOT_RUN**。

详见 `FINAL_UI_UX_REPORT.md`。

---

## K. Frontend — PASS（源码级）

| 验收项                                  | 状态               |
| --------------------------------------- | ------------------ |
| 单一数据边界（`depmap-service.uts`）    | PASS               |
| 纯规则层（`rules.uts`，无 IO / 确定性） | PASS               |
| 状态管理（`stores/app-state.uts`）      | PASS               |
| 页面禁止直连 SQLite（U4）               | PASS               |
| 错误 / 空 / 加载态（U8）                | PASS               |
| 运行期验证（编译 / 渲染）               | **BLOCKED（B10）** |

---

## L. Android — SOURCE_READY PASS / BUILD BLOCKED

SOURCE_READY **PASS**；COMPILED / BUILD_READY / DEVICE_VERIFIED / CRYPTO_VERIFIED / SIGNING_READY / STORE_READY 全部 **BLOCKED**。
B1 前提已修正（JDK 17/21 + Android SDK + licenses 均存在）。详见 `FINAL_PLATFORM_MATRIX.md`。

---

## M. HarmonyOS — SOURCE_READY PASS / BUILD BLOCKED

SOURCE_READY **PASS**；其余 **BLOCKED**。
**hvigor 已真实执行**（引导 pnpm → 解析工程模型 → 卡在 SDK 组件 API 13 解析）。B2 前提已修正。

---

## N. iOS — SOURCE_READY PASS / BUILD BLOCKED

SOURCE_READY **PASS（最大化）**；BUILD_READY / DEVICE_VERIFIED / SIGNING_READY / TESTFLIGHT_READY / APPSTORE_READY 全部 **BLOCKED**（macOS/Xcode）。
交接文档：`docs/IOS_RELEASE_HANDOFF.md`。**未伪造 iOS 结果。**

---

## O. Backup/Restore — PASS（Core）/ BLOCKED（设备）

| 验收项                                          | 状态                              |
| ----------------------------------------------- | --------------------------------- |
| `.depmap` 加密容器格式（`DEPMAP_CONTAINER_V1`） | PASS                              |
| 备份/恢复幂等与回滚                             | PASS（Core 层）                   |
| 设备端导出 / 恢复                               | **BLOCKED（B21）**；UI 已如实标注 |

---

## P. Release — PARTIAL_WITH_REPORT

| 验收项                                                                                         | 状态                                           |
| ---------------------------------------------------------------------------------------------- | ---------------------------------------------- |
| Release 版本审计（`docs/RELEASE_VERSION_MATRIX.md`）                                           | PASS                                           |
| Release 配置无 debug menu / mock DB / demo fixture / 测试密码 / auth bypass / verbose 敏感日志 | PASS（源码级核对）                             |
| App IDs                                                                                        | **占位 `com.example.depmap` → BLOCKER（B11）** |
| App Icon / Splash                                                                              | **BLOCKED（B15/B16）**                         |
| 构建产物（APK/AAB/IPA/HAP）                                                                    | **无（BLOCKED）**                              |
| 签名                                                                                           | **BLOCKED（B4/B7/B9）**                        |
| Git 收口                                                                                       | PASS（`git diff --check` + secret scan）       |
| RC tag                                                                                         | 见 §132 决策（未擅自宣布 1.0）                 |

---

## Q. Store — REQUIRES_USER_RELEASE_DECISION

STORE_METADATA_READY = **PARTIAL_WITH_REPORT**；STORE_ASSETS_READY = **BLOCKED**；
STORE_SUBMISSION_READY = **REQUIRES_USER_RELEASE_DECISION**；STORE_SUBMITTED = **NO**。
详见 `FINAL_STORE_CHECKLIST.md`。

---

## R. Real Data — NOT_RUN

`REAL_DATA_CORRECTNESS = NOT_RUN`、`REAL_DATA_VALUE = NOT_RUN`。
**未以 synthetic 冒充 real data。** Pilot 规格见 `FINAL_STORE_CHECKLIST.md` §4。

---

## S. Final — PARTIAL_WITH_REPORT

| 项                           | 状态                    |
| ---------------------------- | ----------------------- |
| CORE_READY                   | **PASS**                |
| ENGINEERING_READY            | **PASS**                |
| UI_SOURCE_READY              | **PASS**                |
| UI_BUILD_READY               | **BLOCKED（B10）**      |
| ANDROID_READY                | **BLOCKED**             |
| HARMONY_READY                | **BLOCKED**             |
| IOS_SOURCE_READY             | **PASS**                |
| STORE_READY                  | **BLOCKED**             |
| REAL_DATA_VALIDATED          | **NOT_RUN**             |
| STORE_SUBMITTED              | **NO**                  |
| **FINAL_PRODUCTION_CLOSURE** | **PARTIAL_WITH_REPORT** |

**禁止把上述合并为一句「已经上线」。**
