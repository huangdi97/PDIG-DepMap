# RELEASE_VERSION_MATRIX.md — PDIG 版本矩阵（Production RC V1）

> 冻结口径。任何变更必须更新本文件并说明原因。

---

## 1. 版本号

| 项                       | 值                | 说明                                           |
| ------------------------ | ----------------- | ---------------------------------------------- |
| 内部代号                 | `DepMap` / `PDIG` | 工程名                                         |
| 中文工作名               | 个人数字依赖图    | 对外展示名（**正式品牌名待用户决策**，见 B14） |
| **App version**          | `0.3.0-rc.1`      | 由 `v0.3.0-mvp03` 派生，未擅自升到 1.0         |
| **Build number**         | `1`               | RC 首版；每次提交商店递增                      |
| **Schema version**       | `3`               | `core/src/schema/migrations.ts`                |
| **DEPMAP formatVersion** | `1`               | `.depmap` 容器；**不得静默变更**               |
| 数据格式版本（展示）     | `v1`              | 与 formatVersion 一致                          |
| 数据结构版本（展示）     | `v3`              | 与 schemaVersion 一致                          |

> **重要区分**：`formatVersion`（文件容器协议）≠ `schemaVersion`（数据库结构）。
> 二者独立演进；payload 版本随 schema 走（v1 / v2 / v3）。

## 2. Git 版本标记

| Tag            | 对应             | 内容                                                   |
| -------------- | ---------------- | ------------------------------------------------------ |
| `v0.2.0-mvp02` | MVP02            | Global Source Abstraction                              |
| `v0.3.0-mvp03` | MVP03            | Living Graph / Change Safety（453 tests，Freeze PASS） |
| （本轮）       | Production RC V1 | 不加 tag，由 `WORK_STATUS.md` 记录                     |

## 3. 组件版本

| 组件                       | 版本                      | 位置                                                   |
| -------------------------- | ------------------------- | ------------------------------------------------------ |
| Parser — WeChat            | v1                        | `core/src/sources/wechat/adapter.ts`                   |
| Parser — Generic CSV       | v1                        | `core/src/sources/generic-csv/adapter.ts`              |
| Parser — OFX/QFX           | v1                        | `core/src/sources/ofx/adapter.ts`                      |
| RelationDefinitionRegistry | 见 `docs/ARCHITECTURE.md` | `core/src/domain/relation-registry.ts`                 |
| Adapter — Android          | v1                        | `platforms/android/kotlin/com/depmap/core/security/**` |
| Adapter — iOS              | v1                        | `platforms/ios/swift/**`                               |
| Adapter — HarmonyOS        | v1                        | `platforms/harmonyos/arkts/**`                         |
| UTS 插件（5 个）           | 见各自 `package.json`     | `app/uni_modules/**`                                   |

## 4. 应用标识（未冻结）

| 平台                    | 当前值                       | 状态                 |
| ----------------------- | ---------------------------- | -------------------- |
| Android `applicationId` | `com.example.depmap`（占位） | **B11 — 待用户决策** |
| iOS `bundle id`         | `com.example.depmap`（占位） | **B11**              |
| HarmonyOS `bundleName`  | 占位                         | **B11**              |

> 标识变更会导致签名与商店身份漂移。**发布前必须由用户确认。**

## 5. 平台最低要求

见 `store/PLATFORM_REQUIREMENTS.md`。

## 6. 依赖版本（运行时）

| 依赖        | 版本    | License |
| ----------- | ------- | ------- |
| `hash-wasm` | ^4.12.0 | MIT     |

开发依赖：`vitest` / `typescript` / `eslint` / `prettier` / `fast-check` / `iconv-lite` / `globals` /
`@types/node` / `@vitest/coverage-v8` / `typescript-eslint`（见 `THIRD_PARTY_NOTICES.md`）。
`npm audit`：3 moderate（**dev-only**，已登记 `docs/DEPENDENCY_POLICY.md`）。

## 7. 工具链版本（本机实测）

> **2026-09-15 重写**：旧表（JDK 1.8 / 无 Android SDK / 无 DevEco / 无 HBuilderX）经实测
> **与事实不符**，已按本轮实测整体更新。

| 工具                | 本机实测                                           | 要求                         | 状态                                                        |
| ------------------- | -------------------------------------------------- | ---------------------------- | ----------------------------------------------------------- |
| Node                | `v22.22.2`                                         | ≥ 22.5.0                     | PASS                                                        |
| npm                 | `10.9.7`                                           | `packageManager: npm@11.3.0` | 可用（`npm ci` 成功）                                       |
| JDK（Android 构建） | **21.0.10**（Android Studio JBR）                  | 17+                          | **PASS**（旧记 1.8.0_441 已被推翻）                         |
| JDK（DevEco JBR）   | 17.0.12                                            | —                            | 可用                                                        |
| Android SDK         | `<ANDROID_SDK_ROOT>`（platforms 34 / 36.1 / 37.0） | —                            | **PASS**（旧记「无」已被推翻）                              |
| Gradle              | 8.9                                                | —                            | **PASS**（已获取可用发行版）                                |
| adb                 | 可用                                               | —                            | 可用；但 `adb devices` 为空、0 AVD（**B18**）               |
| DevEco Studio       | **5.0.5.310**                                      | —                            | **PASS**（旧记「无」已被推翻）                              |
| HarmonyOS SDK       | **5.0.1.115（API 13）**                            | —                            | **PASS**                                                    |
| hvigor              | 5.13.2 + ohpm 5.0.10                               | —                            | **PASS**（已真实产出 HAP）                                  |
| Xcode / macOS       | 无（win32）                                        | —                            | **BLOCKED（B3）**                                           |
| HBuilderX           | **5.24.2026081301**                                | —                            | 已安装；**CLI 无 build 命令** ⇒ 无头打包 BLOCKED（**B10**） |

## 8. 本轮真实产物（2026-09-15）

| 产物                                                       | 字节   | SHA-256                                                            |
| ---------------------------------------------------------- | ------ | ------------------------------------------------------------------ |
| `platforms/android/artifacts/core-debug.aar`               | 72,376 | `dd7d8c04b23041354b401b1f0e347b9fedf586131dae975c8df70520ef850a0d` |
| `platforms/android/artifacts/core-release.aar`             | 68,731 | `4ac2e7f4c5d407d6b6a2d2232913f343ff7b7ef0f27891d67c60a190bf43c32b` |
| `platforms/harmonyos/artifacts/entry-default-unsigned.hap` | 18,986 | `4f10d0597aaaac2aab4af8e27ec7138709e07e5ea81aaed705d249ed55bd0663` |

> `AAR ≠ APK`；该 HAP 为**原生验证工程产物，不是产品包**。详见 `PLATFORM_RELEASE_MATRIX.md` §0。
