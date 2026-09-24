# CODEBASE_ARCHITECTURE_MAP.md

> PDIG / DepMap —— 全仓工程治理审计 v0.1.0 round（2026-09-23）
> 本文件回答：仓库由哪些模块构成、各自职责、公开契约、依赖方向、数据流向、
> 平台依赖、测试归属、生成/手写边界、发布相关性。
> 生成依据：本轮对 `android/`、`desktop/`、共享 `core/spec/tools/fixtures` 的逐文件审计；
> iOS/Harmony/legacy/core-TS 按合同只读静态审计（环境受限，见 GLOBAL_ARCHITECTURE_AUDIT.md §scope）。

## 1. 顶层布局

| 顶层目录                                                     | 角色                                                                                                                                              | 发布相关性                  |
| ------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------- |
| `spec/`                                                      | **Canonical Truth Source**：domain/schema/security/state-machines/errors/ui（JSON 冻结）                                                          | 不发布（源码）              |
| `fixtures/`                                                  | canonical 测试夹具（impact/readiness/parser/depmap/backup/…，91 用例）                                                                            | 不发布（源码）              |
| `tools/`                                                     | 生成器与 gate（codegen、conformance 编排、harmony 探针）                                                                                          | 不发布（源码）              |
| `android/`                                                   | **Active Android 产品**：`core`(纯 JVM 领域) / `repos`(共享 Repository，纯 JVM) / `conformance`(JVM runner) / `app`(Kotlin/Compose UI + 平台实现) | v0.1.0 Preview APK          |
| `desktop/`                                                   | **Windows Desktop 产品（本轮新建）**：Kotlin/JVM + Compose Desktop，复用 `android/:core` 与 `android/:repos`                                      | v0.1.0 installer + portable |
| `core/`（TS）                                                | **LEGACY_REFERENCE / BEHAVIOR ORACLE**（uni-app x 时代 TS 领域实现）                                                                              | 不发布（保留为 oracle）     |
| `app/`（uvue）、`platforms/`、`legacy/`                      | 旧路线实现与中间产物（只读静态审计）                                                                                                              | 不发布                      |
| `ios/`、`harmony/`                                           | iOS(Swift)/HarmonyOS(ArkTS) 实现（本轮 PAUSED/BLOCKED，静态只读审计）                                                                             | 不发布                      |
| `store/`、`assets/`、`docs/`、`scripts/`、`conformance/`(根) | 商店文案/素材/文档/脚本/根级报告                                                                                                                  | 文档可随 Release 提示       |

## 2. Android 模块（`android/`，Gradle 多模块）

```
:app (com.android.application, compileSdk/targetSdk 36, minSdk 26)
 ├─ production flavor：applicationId com.pdig.app，versionCode 1（占位）
 └─ preview flavor：applicationId com.pdig.app.preview，versionCode 200001，versionName 0.1.0
     ├─ ui/（Compose Compose，22 屏 + Lock 门）
     ├─ workflow/（FileWorkflowCoordinator，文件选择器/导入向导状态机）
     ├─ platform/（AndroidSqliteDriver = SQLCipher；DatabaseKeyStore = Keystore）
     ├─ security/（AppLock/Biometric/PrivacyScreen）
     └─ data/（AppContainer 门面 + BackupRepository，其余 Repository 移至 :repos）
:repos (纯 Kotlin JVM，本轮从 :app 提取，Android 与 Desktop 共用)
 └─ com.pdig.app.data：Graph/Proposal/Candidate/Drift/Plan/Source Repository + Models/PlanSupport/DataUtils
:core (纯 Kotlin JVM，canonical frozen)
 └─ domain/ impact/ plan/ scenario/ serialize/ schema/ sources/ statemachine/ timeline/ crypto/ json/ db/ generated/
:conformance (纯 Kotlin JVM application —— conformance runner，91/91)
 └─ Main + 6 个 Runner 文件 + JdbcSqliteDriver
```

## 3. Desktop 模块（`desktop/`，本轮新建）

```
desktop/app (Compose Desktop，Kotlin 2.0.0 / Compose 1.6.11 / JDK 21)
 ├─ Main.kt（--smoke 入口 + Window 装配）
 ├─ data/DesktopSession.kt（组合 :repos；SQLite 内存库 JdbcSqliteDriver）
 ├─ persist/DepmapFileStore.kt（.depmap = DEPMAP_CONTAINER_V1(password, payload)）
 ├─ security/（DesktopSecurityPort = Windows DPAPI；DeviceUnlockStore）
 ├─ io/FileOps.kt（AWT 原生文件对话框适配）
 ├─ ui/（24 个屏面 + 骨架 + Kit）
 └─ SmokeRunner.kt（headless E2E，--smoke）
```

## 4. 依赖方向（单向，无环，本轮 Gate 实测 DEPENDENCY_CYCLE = 0）

```
UI (android/app, desktop/app)
   ↓
Application/Repository (:repos —— 纯 JVM，无 Android 依赖)
   ↓
Domain (:core —— 纯 JVM，无 Compose/SQLite/平台依赖，只有 BouncyCastle + JDK)
   ↓
Ports/Interfaces (:core db.SqliteDriver / crypto / schema)
   ↑ 实现
Infrastructure：android(app)=SQLCipher+Keystore；desktop=sqlite-jdbc+DPAPI；conformance=sqlite-jdbc
```

关键实测约束：

- `:core` 只依赖 `org.bouncycastle:bcprov-jdk18on`（Argon2id），AES/Base64/SecureRandom 用 JDK；
- `:repos` 只依赖 `:core` + kotlinx-coroutines-core（SourceRepository 的 IO 调度）；
- `:app` 的 Android 依赖（SQLCipher/Keystore/Biometric/MediaStore）全部集中在 `platform/`、`security/`、`data/BackupRepository.kt`；
- `:conformance` 依赖 `:core` + sqlite-jdbc，**不依赖** `:app`；
- Desktop 只依赖 `:core` + `:repos` + `:conformance`（JdbcSqliteDriver 复用）+ Compose/JNA，**零 Android 依赖**。

## 5. 数据方向（不可逆语义）

```
账单文件 → Parser(:core sources) → Observation（仅内存，不落库）→ previewImport
   → 用户确认（Node Resolution：builtin→normalized→fuzzy→user）
   → commitImport（单事务：nodes + proposals + fingerprint/session，graphRevision 规则见下）
Proposal（pending 累计 evidence）→ 用户 accept/reject（accept → Dependency 创建，origin=proposal）
Dependency/DependencyGroup（= 用户确认的 Reality）→ ImpactKernel（BFS，(nodeId,capability) 状态键）
ImpactResult → ChangePlan（must_change 每 key 一条 CHANGE action）→ Action done → Verification
持久化：Reality mutation 与 graphRevision+1 **同事务**；proposal/candidate/drift/evidence/plan/action
        **永不** bump revision（:core StateMachines.GraphRevisionMachine 为权威清单）。
落盘：.depmap = exportGraph(payload JSON, schemaVersion=3) → DEPMAP_CONTAINER_V1 加密。
```

## 6. 平台依赖表

| 能力     | Android                      | Desktop (Windows)                      | Conformance (JVM) |
| -------- | ---------------------------- | -------------------------------------- | ----------------- |
| 本地 DB  | SQLCipher（加密）            | sqlite-jdbc `:memory:`（永不落盘明文） | sqlite-jdbc       |
| 密钥     | Android Keystore + Biometric | Windows DPAPI（OS-protected）          | —                 |
| 文件     | MediaStore / scoped storage  | AWT FileDialog                         | 直接 File         |
| 生物识别 | BiometricPrompt              | —（口令）                              | —                 |
| 隐私屏   | FLAG_SECURE                  | —（窗口）                              | —                 |

## 7. 测试归属

| 测试面                     | 位置                                        | 数量（本轮实测）                                                            |
| -------------------------- | ------------------------------------------- | --------------------------------------------------------------------------- |
| 领域 invariant（C5 全量）  | android/core/src/test                       | 71/71（含 done≠verified、unknown≠required、proposal 不升级 must_change 等） |
| 文件工作流状态机           | android/app/src/test                        | 9/9                                                                         |
| Android 设备内 androidTest | android/app/src/androidTest                 | 59（基线 API36；本轮 smoke 复跑见 Android v0.1.0 报告）                     |
| Canonical conformance      | android/conformance + fixtures              | 91/91（本轮 fresh run，报告 conformance/reports/android.json）              |
| legacy TS 领域             | core/tests（vitest）                        | 430 用例（oracle，只读）                                                    |
| Desktop（本轮新建）        | desktop/app/src/test + SmokeRunner(--smoke) | 见 DESKTOP 报告                                                             |

## 8. 生成/手写边界

| 文件                                                                                                       | 生成器                                                | 规则                                      |
| ---------------------------------------------------------------------------------------------------------- | ----------------------------------------------------- | ----------------------------------------- |
| android/core/.../generated/CanonicalEnums.kt、harmony/.../CanonicalEnums.ets、ios/.../CanonicalEnums.swift | tools/codegen/generate.mjs（spec/domain/domain.json） | 人工改生成结果 = 违规；改 spec 再重新生成 |
| conformance 期望值                                                                                         | tools/conformance（fixtures + embed）                 | fixtures 是 goldset（纯数据）             |
| GB18030 表、codegen 产物                                                                                   | tools/encoding、tools/codegen                         | 同上                                      |
| 其余全部                                                                                                   | 手写                                                  | 受 AGENTS 质量门约束                      |

## 9. Release relevance（v0.1.0）

- Android Preview APK：`android/app`（preview flavor，NON-PROD 签名链）
- Windows Desktop：`desktop/app`（jpackage/nsis 打包，WINDOWS_CODE_SIGNING = BLOCKED_BY_MISSING_CERTIFICATE）
- SBOM/Notice：见 PRODUCT_V0_1_0_RELEASE_MANIFEST.md（从 resolved dependency 实测生成）
- Canonical 语义：spec + fixtures + conformance 91/91 是发布门槛，产品发布不改语义
