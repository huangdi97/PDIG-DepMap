# CODE_SIZE_AUDIT.md — 代码规模审计（2026-09-23 · Round 0.1.0）

> 审计轮次：2026-09-23（四联审计：代码规模 / 依赖 / 代码质量 / 测试质量）。
> 本审计发生在本轮拆分修复**之前**；拆分是修复的一部分。本文「最终状态」为**当前工作树**（as of NOW）。
> 行数口径：物理行（LF 计数，与 Read 工具 totalLines 一致；末行无换行符按实际行计）。
> 扫描方法：PowerShell `[IO.File]::ReadAllText + LF 计数`；排除 `build/`、`.gradle/`、`.kotlin/`、`node_modules/`、`local_private/`、`.tmp_audit/`、`.pi/`。
> Gate 口径：`scripts/quality/check-quality.mjs` 仅 gating `android/app|core|conformance/src/main` + `desktop`（下文凡「>300 例外」均指该范围）。

---

## 1. android/app —— 应用层（Kotlin，32 文件 / 4,736 行，全部 ≤300）

| 文件 | 行数 | 类别 | >300 | 处置 |
|---|---|---|---|---|
| data/AppContainer.kt | 182 | app-domain（组合根/DI） | 否 | SPLIT（本轮 232→182，继续二次抽取 > 目标 ~205） |
| data/BackupRepository.kt | 159 | app-domain（MediaStore 绑定，留在 :app） | 否 | KEEP |
| workflow/FileWorkflowCoordinator.kt | 285 | app-domain（工作流编排） | 否 | KEEP |
| workflow/FileWorkflowEngine.kt | 195 | app-domain（引擎） | 否 | SPLIT（抽取自原大文件） |
| workflow/FileWorkflowState.kt | 193 | app-domain（状态机） | 否 | KEEP |
| workflow/LocalFileWorkflow.kt | 16 | app-domain | 否 | KEEP |
| platform/AndroidSqliteDriver.kt | 202 | platform（SQLCipher 驱动） | 否 | KEEP |
| security/AppLock.kt | 170 | platform/security | 否 | KEEP |
| security/LockGate.kt | 36 | platform/security | 否 | KEEP |
| security/Security.kt | 91 | platform/security | 否 | KEEP |
| MainActivity.kt | 94 | platform | 否 | KEEP（lateinit 注册处 L56，见 EXCEPTIONS.json） |
| PdigApplication.kt | 21 | platform | 否 | KEEP |
| ui/PdigApp.kt | 252 | app-ui | 否 | KEEP（Route.GRAPH 注册 L227） |
| ui/components/Components.kt | 191 | app-ui | 否 | KEEP |
| ui/SecureWindow.kt | 76 | app-ui/platform | 否 | KEEP |
| ui/theme/Theme.kt | 124 | app-ui | 否 | KEEP |
| ui/screens/ImportScreen.kt | 265 | app-ui | 否 | KEEP |
| ui/screens/HomeScreens.kt | 261 | app-ui | 否 | KEEP（多屏并存文件） |
| ui/screens/InfraScreens.kt | 226 | app-ui | 否 | KEEP（GraphScreen 定义于 L74） |
| ui/screens/LockScreen.kt | 205 | app-ui | 否 | KEEP |
| ui/screens/RestoreScreen.kt | 192 | app-ui | 否 | KEEP |
| ui/screens/BackupScreen.kt | 177 | app-ui | 否 | KEEP |
| ui/screens/ImpactScreen.kt | 175 | app-ui | 否 | KEEP |
| ui/screens/ScenarioSetupScreen.kt | 175 | app-ui | 否 | KEEP |
| ui/screens/ChangePlanScreen.kt | 164 | app-ui | 否 | KEEP |
| ui/screens/RealityDriftScreen.kt | 116 | app-ui | 否 | KEEP |
| ui/screens/SettingsScreen.kt | 117 | app-ui | 否 | KEEP |
| ui/screens/ImportScreenHelpers.kt | 116 | app-ui（辅助） | 否 | KEEP |
| ui/screens/PendingReviewScreen.kt | 100 | app-ui | 否 | KEEP |
| ui/screens/CandidateReviewScreen.kt | 90 | app-ui | 否 | KEEP |
| ui/screens/PrivacyScreen.kt | 36 | app-ui | 否 | KEEP |
| ui/screens/AboutScreen.kt | 34 | app-ui | 否 | KEEP |

> 本轮拆分结果：**13 个独立 per-screen 文件（34–265 行）**；HomeScreens / InfraScreens / ImportScreenHelpers 为 3 个分组文件。

**app 测试源集**：`src/test` 1 文件 / 170 行（FileWorkflowStateTest，9 用例，纯 JVM）；`src/androidTest` 12 文件 / 2,324 行（evidence 证据测试，84–334 行；PersistenceEvidenceTest 334、FileWorkflowD16Test 301 > 300 —— 均为**设备测试源集，不在 gate 生产范围**，不计入豁免）。

## 2. android/core —— 领域层（纯 Kotlin JVM，22 文件 / 4,600 行）

| 文件 | 行数 | 类别 | >300 | 处置 |
|---|---|---|---|---|
| schema/Migrations.kt | 434 | migration | **是** | **EXEMPT（EXCEPTIONS.json 注册：migration，schema v1→v3 DDL）** |
| serialize/GraphSerialize.kt | 470 | schema | **是** | **EXEMPT（EXCEPTIONS.json 注册：schema，PAYLOAD_TABLES 字节级契约）** |
| generated/CanonicalEnums.kt | 564 | generated | **是** | **EXEMPT（gate 自动识别 generated/ 路径，无需注册）** |
| crypto/DepmapContainer.kt | 298 | domain（安全核心 Argon2id/AES-GCM） | 否 | KEEP（DepmapException 定义于 L32） |
| statemachine/StateMachines.kt | 298 | domain | 否 | KEEP |
| impact/ImpactKernel.kt | 235 | domain | 否 | **SPLIT（本轮拆分结果）** |
| impact/ImpactTargetEvaluation.kt | 190 | domain | 否 | **SPLIT（本轮拆分结果）** |
| json/Json.kt | 233 | domain | 否 | KEEP |
| domain/Models.kt | 215 | domain | 否 | KEEP |
| plan/Rules.kt | 191 | domain | 否 | KEEP |
| sources/Parsers.kt | 237 | domain（parser 门面） | 否 | **SPLIT（本轮拆分结果）** |
| sources/WechatParser.kt | 134 | domain（parser） | 否 | **SPLIT（本轮拆分结果）** |
| sources/GenericCsvParser.kt | 126 | domain（parser） | 否 | **SPLIT（本轮拆分结果）** |
| sources/OfxParser.kt | 127 | domain（parser） | 否 | **SPLIT（本轮拆分结果）** |
| sources/AmountNormalizer.kt | 70 | domain（parser 辅助） | 否 | **SPLIT（本轮拆分结果）** |
| timeline/Timeline.kt | 245 | domain | 否 | **SPLIT（本轮拆分结果）** |
| timeline/TimelineBuild.kt | 108 | domain | 否 | **SPLIT（本轮拆分结果）** |
| scenario/ScenarioRegistry.kt | 129 | domain | 否 | KEEP |
| domain/Relations.kt | 104 | domain | 否 | KEEP |
| schema/SchemaVersion.kt | 73 | domain/schema | 否 | KEEP |
| crypto/Jcs.kt | 74 | domain | 否 | KEEP |
| db/SqliteDriver.kt | 45 | port（接口） | 否 | KEEP |

**core 测试源集**：`src/test` 7 文件 / 1,230 行 = JdbcTestDriver 109（真实 sqlite-jdbc 驱动辅助）+ 6 个测试类（DomainInvariantTest 233 / StateMachineTest 152 / ImpactKernelTest 249 / PlanReadinessTest 215 / MigrationSemanticsTest 182 / GraphRevisionSemanticsTest 90），共 **71 用例**。

## 3. android/conformance —— 一致性验证 harness（7 文件 / 1,286 行）

| 文件 | 行数 | 类别 | >300 | 处置 |
|---|---|---|---|---|
| Main.kt | 265 | harness（入口） | 否 | **SPLIT（本轮拆分结果；lateinit 注册处 L26，见 EXCEPTIONS.json）** |
| RunnerBackupTimeline.kt | 268 | harness | 否 | **SPLIT（本轮拆分结果）** |
| RunnerParser.kt | 228 | harness | 否 | **SPLIT（本轮拆分结果）** |
| RunnerImpact.kt | 217 | harness | 否 | **SPLIT（本轮拆分结果）** |
| JdbcSqliteDriver.kt | 148 | harness/platform | 否 | KEEP |
| RunnerDepmap.kt | 127 | harness | 否 | **SPLIT（本轮拆分结果）** |
| RunnerRelations.kt | 33 | harness | 否 | **SPLIT（本轮拆分结果）** |

> 说明：conformance 无独立 test 源集 —— runner 本身即对冻结 fixtures 的逐字段验证（本轮 :conformance:run 91/91）。

## 4. android/repos —— 共享仓储层（本轮从 :app 抽取，9 文件 / 1,328 行，全部 ≤300）

| 文件 | 行数 | 类别 | >300 | 处置 |
|---|---|---|---|---|
| PlanRepository.kt | 280 | app-data（repository） | 否 | **EXTRACT（本轮 :repos 抽取）** |
| GraphRepository.kt | 195 | app-data | 否 | EXTRACT |
| SourceRepository.kt | 187 | app-data | 否 | EXTRACT |
| Models.kt | 163 | app-data（共享模型） | 否 | EXTRACT |
| DriftRepository.kt | 126 | app-data | 否 | EXTRACT |
| PlanSupport.kt | 114 | app-data | 否 | EXTRACT |
| ProposalRepository.kt | 135 | app-data | 否 | EXTRACT |
| CandidateRepository.kt | 97 | app-data | 否 | EXTRACT |
| DataUtils.kt | 31 | app-data | 否 | EXTRACT |

> Android 与 Desktop 共用；约束同 :core（无 Compose / Android Context / Keystore）。**当前无测试源集（:repos:test = NO-SOURCE）**。

## 5. desktop —— Compose Desktop 应用（11 文件 / 768 行，全部 ≤300，无测试源集）

| 文件 | 行数 | 类别 | >300 |
|---|---|---|---|
| ui/components/Kit.kt | 158 | app-ui | 否 |
| persist/DepmapFileStore.kt | 108 | app-domain（容器文件存取） | 否 |
| ui/App.kt | 90 | app-ui | 否 |
| ui/UiState.kt | 90 | app-ui | 否 |
| security/DesktopSecurityPort.kt | 73 | platform（DPAPI/JNA） | 否 |
| data/DesktopSession.kt | 64 | app-domain | 否 |
| io/FileOps.kt | 62 | platform | 否 |
| security/DeviceUnlockStore.kt | 49 | platform | 否 |
| ui/Screens.kt | 38 | app-ui | 否 |
| Main.kt | 25 | platform | 否 |
| SmokeRunner.kt | 11 | harness | 否 |

## 6. legacy core-TS —— 行为 oracle（只读，READ_ONLY）

| 组 | 文件数 | 总行数 | >300 文件数 | 说明 |
|---|---|---|---|---|
| core/src | 48 | 8,743 | 8 | graph-serialize.ts 551、impact/kernel.ts 446、import-coordinator.ts 431、proposal-repository.ts 410、schema/migrations.ts 397、generic-csv/adapter.ts 340、crypto/depmap.ts 333、dependency-repository.ts 307 |
| core/tests | 43 | 12,013 | 15 | legacy 测试套件 43 文件（本轮记录 430 用例；LEGACY_REFERENCE_MANIFEST.md L45–46 冻结基线记 453，口径/时点差异） |

> READ_ONLY：冻结参考实现 v0.3.0-uniapp-reference，Native Migration 收口前不删除、不 gate。

## 7. ios / harmony —— 平台实现（本轮 STATIC-ONLY）

| 平台 | 文件数 | 总行数 | >300 | 前四大文件 |
|---|---|---|---|---|
| ios | 28 | 7,619 | 10 | Evaluators.swift 1145、Parsers.swift 970、GraphSerialize.swift 508、CanonicalEnums.swift 505（generated） |
| harmony | 37 | 10,961 | 11 | ConformanceRunner.ets 1977、CanonicalEnums.ets 1071（generated）、Parsers.ets 967、ImpactKernel.ets 706 |

> 其余 ios >300：Timeline.swift 471、Models.swift 421、DepmapContainer.swift 402、Migrations.swift 378、Json.swift 342、ImpactKernel.swift 328。
> 其余 harmony >300：JsonText.ets 661、DomainSelfCheck.ets 645、Timeline.ets 480、Jcs.ets 416、StateMachines.ets 415、DepmapContainerV1.ets 353、FixtureBundle.ets 301（测试夹具）。
> **STATIC-ONLY**：本轮仅静态审计（tracks PASSED，环境受限：无 macOS/真机/Harmony DevEco），**注册为 NOT gated**；未宣称编译/运行状态。

## 8. platforms vendored + tools + scripts

| 组 | 文件数 | 总行数 | >300 | 说明 |
|---|---|---|---|---|
| platforms | 18 | 19,657 | 1 | hvigorw.js 17,834（vendored 构建工具）；其余 15–291（legacy UTS 适配器参考，READ_ONLY） |
| tools | 19 | 3,903 | 3 | harmony/check-compiled-reachability.mjs 547、encoding/generate-gb18030-table.mjs 543、codegen/generate.mjs 519（工具脚本，非 gated 生产代码） |
| scripts | 2 | 468 | 0 | quality/check-quality.mjs 279、android_real_device_acceptance.ps1 189 |

## 9. 汇总与豁免

| 组 | 文件数 | 总行数 | >300 且需注册例外 |
|---|---|---|---|
| android/app（main+test+androidTest） | 45 | 7,230 | 0（androidTest 2 个 >300 属设备测试源集） |
| android/core（main+test） | 29 | 5,830 | 3（见下） |
| android/conformance | 7 | 1,286 | 0 |
| android/repos | 9 | 1,328 | 0 |
| desktop | 11 | 768 | 0 |
| legacy core-TS | 91 | 20,756 | READ_ONLY |
| ios | 28 | 7,619 | STATIC-ONLY |
| harmony | 37 | 10,961 | STATIC-ONLY |
| platforms | 18 | 19,657 | vendored |
| tools + scripts | 21 | 4,371 | 工具脚本 |

**3 个 >300 豁免（gate 范围）**：
1. `android/core/.../schema/Migrations.kt` 434 行 — category `migration`（EXCEPTIONS.json 注册）。
2. `android/core/.../serialize/GraphSerialize.kt` 470 行 — category `schema`（EXCEPTIONS.json 注册）。
3. `android/core/.../generated/CanonicalEnums.kt` 564 行 — generated（gate 自动豁免，无需注册）。

**本轮拆分/抽取落地清单**：AppContainer 232→182；UI 拆为 13 个 per-screen 文件（34–265）；core Parsers 拆出 WechatParser/GenericCsvParser/OfxParser/AmountNormalizer（门面 Parsers.kt 237）；ImpactKernel 拆出 ImpactTargetEvaluation；Timeline 拆出 TimelineBuild；conformance Main 拆出 5 个 Runner；`:repos` 自 `:app` 抽取 9 文件（android/settings.gradle.kts 与 desktop/settings.gradle.kts 均已 include(":repos")）。

---

**结论**：gate 范围（app/core/conformance/desktop 生产源）无未注册 >300 文件；3 个豁免全部带类别与理由登记（migration/schema/generated）；本轮拆分全部落地且 `scripts/quality/check-quality.mjs` file-size 检查 PASS；ios/harmony/core-TS/platforms 为 STATIC-ONLY / READ_ONLY / vendored，不参与 gating。