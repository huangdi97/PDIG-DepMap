# RELEASE_VERSION_MATRIX.md — PDIG 版本矩阵（Production RC V1）

> 冻结口径。任何变更必须更新本文件并说明原因。

---

## 1. 版本号

| 项 | 值 | 说明 |
|---|---|---|
| 内部代号 | `DepMap` / `PDIG` | 工程名 |
| 中文工作名 | 个人数字依赖图 | 对外展示名（**正式品牌名待用户决策**，见 B14） |
| **App version** | `0.3.0-rc.1` | 由 `v0.3.0-mvp03` 派生，未擅自升到 1.0 |
| **Build number** | `1` | RC 首版；每次提交商店递增 |
| **Schema version** | `3` | `core/src/schema/migrations.ts` |
| **DEPMAP formatVersion** | `1` | `.depmap` 容器；**不得静默变更** |
| 数据格式版本（展示） | `v1` | 与 formatVersion 一致 |
| 数据结构版本（展示） | `v3` | 与 schemaVersion 一致 |

> **重要区分**：`formatVersion`（文件容器协议）≠ `schemaVersion`（数据库结构）。
> 二者独立演进；payload 版本随 schema 走（v1 / v2 / v3）。

## 2. Git 版本标记

| Tag | 对应 | 内容 |
|---|---|---|
| `v0.2.0-mvp02` | MVP02 | Global Source Abstraction |
| `v0.3.0-mvp03` | MVP03 | Living Graph / Change Safety（453 tests，Freeze PASS） |
| （本轮） | Production RC V1 | 不加 tag，由 `WORK_STATUS.md` 记录 |

## 3. 组件版本

| 组件 | 版本 | 位置 |
|---|---|---|
| Parser — WeChat | v1 | `core/src/sources/wechat/adapter.ts` |
| Parser — Generic CSV | v1 | `core/src/sources/generic-csv/adapter.ts` |
| Parser — OFX/QFX | v1 | `core/src/sources/ofx/adapter.ts` |
| RelationDefinitionRegistry | 见 `docs/ARCHITECTURE.md` | `core/src/domain/relation-registry.ts` |
| Adapter — Android | v1 | `platforms/android/kotlin/com/depmap/core/security/**` |
| Adapter — iOS | v1 | `platforms/ios/swift/**` |
| Adapter — HarmonyOS | v1 | `platforms/harmonyos/arkts/**` |
| UTS 插件（5 个） | 见各自 `package.json` | `app/uni_modules/**` |

## 4. 应用标识（未冻结）

| 平台 | 当前值 | 状态 |
|---|---|---|
| Android `applicationId` | `com.example.depmap`（占位） | **B11 — 待用户决策** |
| iOS `bundle id` | `com.example.depmap`（占位） | **B11** |
| HarmonyOS `bundleName` | 占位 | **B11** |

> 标识变更会导致签名与商店身份漂移。**发布前必须由用户确认。**

## 5. 平台最低要求

见 `store/PLATFORM_REQUIREMENTS.md`。

## 6. 依赖版本（运行时）

| 依赖 | 版本 | License |
|---|---|---|
| `hash-wasm` | ^4.12.0 | MIT |

开发依赖：`vitest` / `typescript` / `eslint` / `prettier` / `fast-check` / `iconv-lite` / `globals` /
`@types/node` / `@vitest/coverage-v8` / `typescript-eslint`（见 `THIRD_PARTY_NOTICES.md`）。
`npm audit`：3 moderate（**dev-only**，已登记 `docs/DEPENDENCY_POLICY.md`）。

## 7. 工具链版本（本机实测）

| 工具 | 本机实测 | 要求 | 状态 |
|---|---|---|---|
| Node | `v22.22.2` | ≥ 22.5.0 | PASS |
| npm | `10.9.7` | `packageManager: npm@11.3.0` | 可用（`npm ci` 成功） |
| JDK | `1.8.0_441` | 17+ | **BLOCKED（B1）** |
| adb | `1.0.32` | — | 过旧 |
| Android SDK / Gradle | 无 | — | **BLOCKED（B1）** |
| DevEco / HarmonyOS SDK | 无 | — | **BLOCKED（B2）** |
| Xcode / macOS | 无（win32） | — | **BLOCKED（B3）** |
| HBuilderX | 无 | — | **BLOCKED（B10）** |
