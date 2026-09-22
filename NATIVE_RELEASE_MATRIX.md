# NATIVE_RELEASE_MATRIX.md

> 状态枚举：`NOT_STARTED` → `IMPLEMENTED` → `TESTED` → `RUNTIME_VERIFIED` → `STORE_READY`
> 商店提交**只准备，不自动提交**（§206）。

更新时间：2026-09-21（Android Product Finalization 收口；Android 行刷新为当前真实状态）

---
---

## 1. Android

| 项                        | 状态        | 说明                                     |
| 项                        | 状态        | 说明                                     |
| ------------------------- | ----------- | ---------------------------------------- |
| 源码                      | RUNTIME_VERIFIED | `android/` 三模块（:core / :conformance / :app）fresh clone 可构建 |
| 领域 Conformance          | **PASS**    | 91/91                                     |
| 单元测试                  | **PASS**    | :core 71/71 + :app JVM 9/9                |
| Integration / Migration   | **PASS**    | JVM MigrationSemantics 10 + conformance migration + 设备证据 |
| UI 测试                   | **PASS**    | 设备内 androidTest 59/59 + 三场景 E2E 40/40 + Core Journey 41/41 |
| debug APK                 | **PASS**    | `app-debug.apk` 36,974,885 B `030BD9E3…` |
| release AAB               | **PASS**（未签名） | `app-release.aab` 20,862,091 B `A24593A6…` |
| signing config            | BLOCKED     | **BLOCKED_BY_MISSING_PRODUCTION_KEYSTORE**（用户提供；runbook 就绪） |
| 设备 E2E                  | **RUNTIME_VERIFIED** | Core Journey 41/41 + 三场景 40/40（AVD API34 + API35） |
| 性能 smoke                | **PASS**    | 10,000 行 CSV 全解析 0 错误                |
| 无障碍                    | PARTIAL     | Compose 语义树 14/14 PASS；TalkBack 实机 NOT_RUN（环境受限） |
| Play Console metadata     | PARTIAL     | `ANDROID_STORE_METADATA.md` 文案完成；截图/图标/URL 待用户 |
| **STORE_READY**           | **NO**      | 阻塞：production keystore + 开发账号 + 正式 applicationId + 品牌素材 + 隐私 URL |
---

## 2. HarmonyOS

| 项                        | 状态        | 说明                       |
| ------------------------- | ----------- | -------------------------- |
| 源码                      | NOT_STARTED | 仅 codegen 产物            |
| Domain / UI               | NOT_STARTED |                            |
| hvigor build（HAP）        | NOT_STARTED |                            |
| signing（AGC）             | BLOCKED     | NB-3 / NB-6                |
| 设备 E2E                   | NOT_RUN     | NB-7                       |
| AppGallery metadata        | NOT_STARTED |                            |
| **STORE_READY**            | **NO**      |                            |

---

## 3. iOS

| 项                        | 状态                | 说明                  |
| ------------------------- | ------------------- | --------------------- |
| 源码                      | NOT_STARTED         | 仅 codegen 产物       |
| Source Parity             | NOT_STARTED         |                       |
| Xcode project             | NOT_STARTED         |                       |
| Build / Test              | **BLOCKED_BY_MACOS** | NB-1                 |
| Archive                   | BLOCKED             | NB-1 / NB-4           |
| TestFlight                | BLOCKED             | NB-4                  |
| App Store metadata        | NOT_STARTED         |                       |
| **STORE_READY**           | **NO**              |                       |

> 按 §212：无 Mac 时 `IOS_SOURCE_PARITY` 可 PASS、`IOS_BUILD = BLOCKED_BY_MACOS`。
> 因此"Native Source Migration"可 PASS，但 **`NATIVE_PRODUCTION_RC`
> 不得声称三端 Device Verified**。

---

## 4. 发布产物登记（§228：path / hash / version / HEAD / platform）

| 平台   | 产物 | path | hash | version | HEAD |
| ------ | ---- | ---- | ---- | ------- | ---- |
| —      | **尚无发布产物** | — | — | — | — |

---

## 5. 可复现性（§229）

| 平台   | Clean Clone → Build | 状态                    |
| ------ | ------------------- | ----------------------- |
| Android | `cd android && gradle :core:compileKotlin :conformance:run --args="<root>"` | **PASS**（本机实跑） |
| Harmony | 待建立              | NOT_STARTED             |
| iOS     | 待建立              | BLOCKED_BY_MACOS        |

---

## 6. 版本策略（§200/§201）

- 内部 milestone 版本：`0.1.0-milestone`
- 正式 App Version **待用户决定**，不擅自发布 1.0

---

## 7. Store 复用与更新项（§159/§226）

| 项             | 状态        | 说明                                   |
| -------------- | ----------- | -------------------------------------- |
| 已有 Store 素材 | 可复用      | `store/` 目录                          |
| 技术栈描述      | 待更新      | 需改为原生技术栈                        |
| 隐私说明        | 待更新      | 需反映"无后端/无分析/无网络"            |
| 权限说明        | 待更新      | 权限最小化审计后重写                    |
| 截图            | **必须重做** | 旧 uni-app 截图**不得**当 Native 截图（§227） |
| 正式隐私政策 URL | 待定        | NB-11                                  |
