# GLOBAL_ARCHITECTURE_AUDIT.md

> PDIG / DepMap —— 全仓工程治理审计 v0.1.0 round（2026-09-23）
> 审计范围与结论：见 CODEBASE_ARCHITECTURE_MAP.md（模块图）与 QUALITY_GATES_V1.md（Gate 清单）。
> 本文档回答：架构是否满足 AGENTS §1-§24 的长期约束、本轮是否改坏、剩下什么。

## 1. 审计范围（scope）与诚实声明

- **Active production scope（本轮 gating 对象）**：`android/{app,core,conformance}/src/main` + `desktop/**/src/main`。
  这些目录必须满足 C2 全部计数 = 0（已通过：`node scripts/quality/check-quality.mjs` → VERDICT PASS）。
- **共享 canonical**：`spec/` + `fixtures/` + `tools/`（goldset / 生成器 / 编排；canonical 语义冻结）。
- **只读静态审计（环境受限，不参与 gating）**：`ios/`（BLOCKED_BY_MACOS，无 Xcode）、`harmony/`（本轮 PAUSED，
  主机 63/63 用例曾 PASS、设备侧 RUNTIME_NOT_RUN）、`legacy/` + `app/` + `platforms/` + 根 `conformance/`（旧路线 oracle，
  AGENTS §24.5 要求 Cutover 前绝不删除）、`core/`（TS oracle）。
  → 这些目录存在 >300 行文件（如 `ios/Sources/.../Evaluators.swift` 1145 行、`harmony/.../ConformanceRunner.ets` 1977 行、
  `platforms/harmonyos/hvigorw.js` 17834 行 vendored）——**如实登记为环境受限静态范围**，
  不把“没跑通就标 PASS”，也不把“暂停轨道上的旧文件”算进本轮的 UNJUSTIFIED 计数（GLOBAL_CODE_QUALITY_AUDIT §scope 同口径）。

## 2. 分层与依赖方向（实测）

- UI（android/app、desktop/app）→ 应用层 Repository（android/repos，纯 JVM）→ Domain（android/core，纯 JVM）→ Ports（core.db.SqliteDriver / core.crypto / core.schema）→ Infrastructure 实现 Ports。
- 本轮把 Repository 从 `:app` 提取为 `:repos`（纯 JVM）后，桌面端直接复用同一份应用层 —— 不再存在“第二套 Repository/第二套 Domain”。
- `:core` 不放宽：不 import androidx/compose；只依赖 BouncyCastle（Argon2id）与 JDK。
- Gate `DEPENDENCY_CYCLE = 0`：包级 import 图 DFS 实测无环。

## 3. 高内聚 / 单一职责（本轮检查）

- `AppContainer.kt` 1425 → 205 行：现在是纯门面（7 个 repo 字段 + 31 个委托），不再包揽 SQL 细节；
- UI：`DataScreens.kt` 809 / `PlanScreens.kt` 362 / `ImpactJourneyScreens.kt` 301 → 13 个单屏文件（34–265 行），
  每个文件一个屏面职责；共享组件留在 `ui/components`；
- core：`Parsers.kt` 684 → 5 文件（Parsers / AmountNormalizer / WechatParser / GenericCsvParser / OfxParser），
  `ImpactKernel.kt` 387 → ImpactKernel + ImpactTargetEvaluation，`Timeline.kt` 351 → Timeline + TimelineBuild；
- conformance：`Main.kt` 1095 → Main(265) + 5 个 Runner（按 fixture category 切分）。
- 残留判断：`GraphSerialize.kt`(≈440) / `Migrations.kt`(≈400) 因“schema 契约 + 逐字节一致”与“迁移 DDL”**注册豁免**，
  不做无意义拆分（拆分它们反而制造跨文件的状态一致性风险，属 schema/migration 类例外）。

## 4. 事务与 Revision 语义（C7 专项）

- `GraphRevisionMachine`（core StateMachines）列出的 bumpsOn/neverBumpsOn 是**权威清单**；
- Repository 实现遵守：Reality mutation（node/dependency/group/drift_resolve）与 `graph_revision+1` 在同一
  `driver.transaction {}` 内；`proposal_upsert / proposal_decision / candidate_* / drift_detect|dismiss /
timeline_build / import_session / plan_* / action_*` 不 bump（GraphRepository.bumpRevision 只在明确列出的操作中调用）；
- 无 Repository 偷偷 commit：`SqliteDriver.transaction` 是唯一提交点，查询路径全部只读；
- Desktop 的内存库同样走 `:repos` 事务语义，落盘仅有 `exportGraph → container` 一条明文出口（加密）。

## 5. 修改安全（Minimum Safe Refactor）

- 所有 split 均为机械搬移：同包移动顶层声明/类成员、补 import、private→internal（仅 cross-file 需要处，逐处登记）；
- 行为证据：`:conformance:run` 91/91 逐字比对（split 前后 byte-exact）、`:core:test` 71/71、`:app:testDebugUnitTest` 9/9 全绿；
- 未在重构中改任何领域语义：Impact/Readiness/Coverage/Container/schema/relation 规则零改动；
- 未做无关格式化、未删除有价值注释、未混入业务改动。

## 6. 平台隔离（Adapter 检查）

| 能力     | Android 实现位置                               | Desktop 实现位置                              | Domain 是否感知              |
| -------- | ---------------------------------------------- | --------------------------------------------- | ---------------------------- |
| DB 驱动  | app/platform/AndroidSqliteDriver（SQLCipher）  | conformance/JdbcSqliteDriver（:memory:）      | 无（只认 SqliteDriver 接口） |
| 文件选择 | workflow/FileWorkflowCoordinator（MediaStore） | desktop/io/FileOps（AWT）                     | 无                           |
| 密钥     | security/DatabaseKeyStore + Biometric          | desktop/security/DesktopSecurityPort（DPAPI） | 无                           |
| 备份落盘 | data/BackupRepository（MediaStore）            | desktop/persist/DepmapFileStore（文件）       | 只认 exportGraph/importGraph |

结论：业务代码中不存在 Android/iOS/Harmony 条件分支；平台差异全部收敛在 Adapter 层。

## 7. 已知架构债务（如实登记，非本轮引入）

1. `data/BackupRepository.kt` 仍含 MediaStore 逻辑（Android-only）——它被 `:app` 独占，Desktop 不使用；
   后续若需 Android/Desktop 共用备份语义，可仿照 `:repos` 再提取“payload 层 + 平台 IO 层”。（本轮不拆：MediaStore 与
   文件 IO 是两种 IO，拆共享层收益低。）
2. `AppContainer.observations`… `:repos` 的 `SourceRepository.previewImport` 参数签名与旧版兼容，无债务。
3. `Route.GRAPH`（android）已注册但仍非首页；产品口径“Graph 不是主界面”保持（ANDROID_FINAL_73_AUDIT D-条款）。
4. Desktop 尚未实现 drift/candidate 的**生成**管线（与 Android 一致：消费 legacy 语义写入的数据；生成留给未来的
   统一发现管线，属产品边界内，不造第二个发现引擎）。

## 8. 结论

- ARCHITECTURE_QUALITY = **PASS**（active scope）：分层单向、无环、平台隔离、事务语义由 core 权威清单约束、
  Desktop 复用同一 Domain+Application，未创建第二 Domain；
- 所有豁免与静态范围均真实、可审核（见 scripts/quality/EXCEPTIONS.json 与本文件 §1）。

## 9. v0.1.2 质量迭代收口轮复核（2026-09-24）

- **本轮结论**：ARCHITECTURE_QUALITY = **PASS（保持）**；分层单向、无环、平台隔离、事务语义未变。
- **纯格式轮（FIXED 复核）**：121 个 md 经 prettier 规范化，commit `a9c1cab`（纯格式，`git diff -w`
  复核无业务语义变化）——架构文档与代码均无结构改动。
- **质量 Gate**：`node scripts/quality/check-quality.mjs` → **VERDICT PASS exit 0**（2026-09-24T06:56Z），
  10 项计数全 0（含 `DEPENDENCY_CYCLE=0`）；file-size OK（exempted 3）；scope=`android/app,core,conformance,repos` + desktop。
- **EXCEPTIONS.json 复核（JUSTIFIED）**：合法 UTF-8 JSON、reason 无乱码；fileSizeOver300 2 条
  （Migrations.kt=migration、GraphSerialize.kt=schema）、kotlinEscapes 2 条（MainActivity lateinit、
  conformance Main lateinit，均 JUSTIFIED）、secretPattern 2 条（SmokeRunner.kt:39、DepmapFileStoreTest.kt:21，
  fixture 合成口令）。
- **Dead code（FIXED）**：`desktop/app/src/main/kotlin/com/pdig/desktop/io/FileOps.kt` 删除无调用者的
  `DesktopFileOps.write` 默认方法（6 行；Grep 确认 desktop 全树无 `.write(` 调用者、无实现覆盖），
  保留 pickOpen/pickSave/readBytes。
- **行为回归锚点**：core `npm run check` 全绿（453 tests / 43 files、architecture circular=0）；
  conformance fresh SUMMARY（2026-09-24）android **91/91 PASS**（pass=91 fail=0 total=91）；
  Android `:core:test` 71/71、`:app:testDebugUnitTest` 9/9（--rerun-tasks fresh）；
  仪器化基线 `TEST-pdig36(AVD)-16-_app-preview.xml` **60/60 PASS**（2026-09-24 11:39）。
- **平台状态（如实）**：harmony 87/91（4 项 runtime-blocked，2026-09-19 旧记录，本机无 HarmonyOS 运行环境）；
  ios 91/91（2026-09-19 旧记录）；Android 运行时 smoke 本轮 **RUNTIME_ENVIRONMENT_BLOCKED**
  （2026-09-24 下午起本机所有 AVD full startup 静默退出——环境阻塞，不是回归）。
