# PLATFORM_RELEASE_MATRIX.md — PDIG 平台发布矩阵

> 分层判定：`SOURCE_READY` → `BUILD_READY` → `DEVICE_VERIFIED` → `SIGNING_READY` →
> `STORE_ASSETS_READY` → `STORE_METADATA_READY` → `STORE_SUBMISSION_READY` → `STORE_SUBMITTED`
> **不得把以上合并成"已上线"。** 本机环境：Windows（win32），JDK 1.8，无 Android SDK / DevEco / Xcode / HBuilderX。

---

## Android

| 层 | 状态 | 证据 / 原因 |
|---|---|---|
| SOURCE_READY | PASS | `platforms/android/**`（Gradle KTS ×3、Kotlin 安全层 ×5、Golden test ×1、Manifest）；`app/uni_modules/*/app-android` |
| BUILD_READY | **BLOCKED** | 无 JDK17+（实测 `java 1.8.0_441`）/ 无 Android SDK / 无 Gradle wrapper（**B1**） |
| DEVICE_VERIFIED | **BLOCKED** | 无构建产物（B1） |
| SIGNING_READY | **BLOCKED** | 无 release keystore（**B4**，用户提供） |
| STORE_ASSETS_READY | **BLOCKED** | 无正式 icon/splash（待资产） |
| STORE_METADATA_READY | 见 RC 报告 | `store/*` 已就绪 |
| STORE_SUBMISSION_READY | **BLOCKED** | 依赖 B1 + B4 + 商店账号（B5） |
| STORE_SUBMITTED | NO | 本轮不提交 |

## HarmonyOS

| 层 | 状态 | 证据 / 原因 |
|---|---|---|
| SOURCE_READY | PASS | `platforms/harmonyos/**`（app.json5、module.json5、ArkTS 适配）；`app/uni_modules/*/app-harmony` |
| BUILD_READY | **BLOCKED** | 无 DevEco Studio / HarmonyOS SDK / hvigor（**B2**） |
| DEVICE_VERIFIED | **BLOCKED** | B2 |
| SIGNING_READY | **BLOCKED** | 无 HarmonyOS release signing（**B7**） |
| STORE_ASSETS_READY | **BLOCKED** | 待资产 |
| STORE_METADATA_READY | 见 RC 报告 | `store/*` 已就绪 |
| STORE_SUBMISSION_READY | **BLOCKED** | 依赖 B2 + B7 + AppGallery 身份（B6） |
| STORE_SUBMITTED | NO | 本轮不提交 |

## iOS

| 层 | 状态 | 证据 / 原因 |
|---|---|---|
| SOURCE_READY | PASS | `platforms/ios/**`（Package.swift、Swift 适配、XCTest golden）；`app/uni_modules/*/app-ios` |
| BUILD_READY | **BLOCKED** | **非 macOS**（Windows 无 Xcode，**B3**）——按 §100/§140 不得伪造 COMPILED |
| DEVICE_VERIFIED | **BLOCKED** | B3 |
| SIGNING_READY | **BLOCKED** | 无 Apple Developer Account / 证书（**B8/B9**） |
| TESTFLIGHT_READY | **BLOCKED** | B3 + B8 |
| APPSTORE_READY | **BLOCKED** | B3 + B8/B9 |
| STORE_SUBMITTED | NO | 本轮不提交 |

## Core（Node 22，本机唯一可构建目标）

| 层 | 状态 | 证据 |
|---|---|---|
| SOURCE_READY | PASS | `core/src/**` |
| BUILD_READY | PASS | `npm ci` 可执行；`npm run check` / `check:full` 全绿 |
| TESTED | PASS | 453/453（43 files） |
| DEVICE_VERIFIED | N/A | 纯库 |
| STORE_READY | N/A | 不单独上架 |

## UI（uni-app x）

| 层 | 状态 | 原因 |
|---|---|---|
| SOURCE_READY | PASS | 17 页 `.uvue` + 5 UTS 插件 |
| BUILD_READY | **BLOCKED** | 无 HBuilderX / uni-app x 工具链（**B10**） |
| DEVICE_VERIFIED | **BLOCKED** | B10 |
| STATIC_GATE | PASS | `check-ui.mjs` 机械校验 |

---

## 汇总（本轮结论）

```
ANDROID_SOURCE_READY   = PASS      ANDROID_BUILD_READY   = BLOCKED (B1)
HARMONY_SOURCE_READY   = PASS      HARMONY_BUILD_READY   = BLOCKED (B2)
IOS_SOURCE_READY       = PASS      IOS_BUILD_READY       = BLOCKED (B3, non-macOS)
CORE_SOURCE_READY      = PASS      CORE_BUILD_READY      = PASS
UI_SOURCE_READY        = PASS      UI_BUILD_READY        = BLOCKED (B10)
```

**至少一个可实际运行目标平台 build PASS = YES（Core / Node 22）** —— 满足 §136 该条要求。
**但 Android / HarmonyOS / iOS 的 BUILD_READY 均 BLOCKED，故不能声明三端上线。**
