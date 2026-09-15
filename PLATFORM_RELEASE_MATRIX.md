# PLATFORM_RELEASE_MATRIX.md — PDIG 平台发布矩阵

> 分层判定：`SOURCE_READY` → `BUILD_READY` → `INSTALL_READY` → `DEVICE_VERIFIED` →
> `SIGNING_READY` → `STORE_ASSETS_READY` → `STORE_METADATA_READY` → `STORE_SUBMISSION_READY` → `STORE_SUBMITTED`
>
> **不得把以上合并成"已上线"。**
>
> **2026-09-15 重写说明（重要）**：本文件旧版第 5 行写「本机环境：Windows（win32），JDK 1.8，
> 无 Android SDK / DevEco / Xcode / HBuilderX」。该前提经本轮实测**全部与事实不符**，已整体重写。
> 实测环境：`<repo>`，Windows；JDK 21.0.10（Android Studio JBR）/ 17.0.12（DevEco JBR）；
> Android SDK `<ANDROID_SDK_ROOT>`；Gradle 8.9；DevEco Studio 5.0.5.310 + HarmonyOS SDK 5.0.1.115 (API 13)
>
> - hvigor 5.13.2；HBuilderX 5.24.2026081301；**无 Xcode（非 macOS）**；**无真实设备**。

---

## 0. 本轮「已真实发生」的三件事（区别于历史所有轮次）

| 事项                        | 证据                                                                                                                           | 报告                              |
| --------------------------- | ------------------------------------------------------------------------------------------------------------------------------ | --------------------------------- |
| Android 原生核心真实编译    | `BUILD SUCCESSFUL`；`core-debug.aar` 44,147 B / `core-release.aar` 42,352 B                                                    | `docs/ANDROID_BUILD_REPORT.md`    |
| Android↔Node 互操作真实跑通 | 黄金向量 **4/4 × debug + release = 8/8 PASS**，0 failures / 0 errors / 0 skipped                                               | `docs/ANDROID_BUILD_REPORT.md` §6 |
| HarmonyOS 真实产出 HAP      | `hvigor BUILD SUCCESSFUL in 42 s 263 ms`；`entry-default-unsigned.hap` 18,986 B，含 `ets/modules.abc` 10,568 B（ArkTS 字节码） | `docs/HARMONY_BUILD_REPORT.md`    |

**同时必须明确的三条边界**（防止把上述成果误读为"可上线"）：

1. **AAR ≠ APK**。Android 产物是 Android Library，无 `AndroidManifest` 入口、无页面、无 uni-app x 运行时，
   **不可安装**。
2. **该 HAP 是原生验证工程产物，不是产品包**。它只含最小 `EntryAbility` + `Index.ets` +
   `RelationalStoreSecureAdapter.ets`，**不含** 24 个 `.uvue` 页面。
3. **UI 从未被真实编译器验证过**。`check:ui` PASS 是静态门，不是编译通过。

---

## 1. Android

| 层                      | 状态                     | 证据 / 原因                                                                                                                                                                                                    |
| ----------------------- | ------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| SOURCE_READY            | **PASS**                 | `platforms/android/**`（Gradle KTS、Kotlin 安全层、Golden test、Manifest）；`app/uni_modules/*/app-android`                                                                                                    |
| **NATIVE_BUILD_READY**  | **PASS（本轮新增）**     | `gradle :core:assembleDebug :core:assembleRelease --rerun-tasks --no-build-cache` → BUILD SUCCESSFUL；AAR ×2 已入库                                                                                            |
| **NATIVE_TESTED**       | **PASS（本轮新增）**     | `:core:testDebugUnitTest` / `:core:testReleaseUnitTest` → 各 4/4 PASS                                                                                                                                          |
| **PRODUCT_BUILD_READY** | **BLOCKED（B10）**       | 产品 APK/AAB 需 HBuilderX 打包；实测 HBuilderX 已装但 **CLI 无 build 命令**，无头环境无法触发                                                                                                                  |
| INSTALL_READY           | **BLOCKED（B10）**       | 无 APK/AAB 产物                                                                                                                                                                                                |
| DEVICE_VERIFIED         | **BLOCKED（B10 + B18）** | 无产物 + 无设备（`adb devices` 空、0 AVD、无 system-image）；按指令不得伪造模拟器                                                                                                                              |
| SIGNING_READY           | **BLOCKED（B4）**        | 无 release keystore（用户提供）；按指令不得自动生成 Production Credentials                                                                                                                                     |
| STORE_ASSETS_READY      | **PARTIAL_WITH_REPORT**  | **本轮已生成占位资产并闭环 U-1**：`app/static/icons/{48,72,96,192,1024}.png` + `app/static/splash/{480x762,720x1242,960x1656,1242x2688}.png`；但为程序化生成的**品牌占位图**，非设计交付，正式资产仍待 B15/B16 |
| STORE_METADATA_READY    | 见 RC 报告               | `store/*` 已就绪；`applicationId` 仍为占位 `com.example.depmap`（B11）                                                                                                                                         |
| STORE_SUBMISSION_READY  | **BLOCKED**              | 依赖 B10 + B4 + B5 + B11                                                                                                                                                                                       |
| STORE_SUBMITTED         | **NO**                   | 本轮不提交                                                                                                                                                                                                     |

## 2. HarmonyOS

| 层                        | 状态                    | 证据 / 原因                                                                                                                                                                 |
| ------------------------- | ----------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| SOURCE_READY              | **PASS**                | `platforms/harmonyos/**`；`app/uni_modules/*/app-harmony`                                                                                                                   |
| **ENGINE_SKELETON_READY** | **PASS（本轮新增）**    | 原工程只有 3 个文件且 `app.json5` 不含 Stage 模型要求顶层 `"app"` 键 → **从未被 hvigor 解析过**；本轮补齐 9/9 骨架项                                                        |
| **NATIVE_BUILD_READY**    | **PASS（本轮新增）**    | `hvigorw assembleHap --mode module -p product=default -p buildMode=debug --no-daemon` → **BUILD SUCCESSFUL in 42 s 263 ms**；HAP 18,986 B，ArkTS 已编译为 `ets/modules.abc` |
| **PRODUCT_BUILD_READY**   | **BLOCKED（B10）**      | 产品级 HAP 由 HBuilderX 产出；当前 HAP 仅为原生验证工程产物                                                                                                                 |
| INSTALL_READY             | **BLOCKED（B7）**       | HAP 为 **unsigned**；无签名材料与正式 bundleName                                                                                                                            |
| DEVICE_VERIFIED           | **BLOCKED（B18）**      | 无 HarmonyOS 设备（`hdc` 无可用目标）                                                                                                                                       |
| SIGNING_READY             | **BLOCKED（B7）**       | 需 AGC 证书 / Profile                                                                                                                                                       |
| STORE_ASSETS_READY        | **PARTIAL_WITH_REPORT** | 占位资产已生成（同 Android），正式资产待 B15/B16                                                                                                                            |
| STORE_METADATA_READY      | 见 RC 报告              | `store/*` 已就绪；bundleName 占位（B11）                                                                                                                                    |
| STORE_SUBMISSION_READY    | **BLOCKED**             | 依赖 B10 + B7 + B6 + B11                                                                                                                                                    |
| STORE_SUBMITTED           | **NO**                  | 本轮不提交                                                                                                                                                                  |

## 3. iOS

| 层               | 状态                      | 证据 / 原因                                                                                                                                                                                                   |
| ---------------- | ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| SOURCE_READY     | **PARTIAL_WITH_REPORT**   | `platforms/ios/**` 存在，但本轮实测发现**结构性缺口**：<br>**G-1（首要）** `swift/` 下**没有 `DepmapContainerV1.swift`** → iOS 侧无容器实现；<br>**G-2** SQLCipher 未接入 SPM；<br>**G-3** 测试全部 `XCTSkip` |
| BUILD_READY      | **BLOCKED（B3）**         | 非 macOS，Windows 无 Xcode。按 §100/§140 **不得伪造 COMPILED**，亦不得伪造 iOS 工具链                                                                                                                         |
| INSTALL_READY    | **BLOCKED（B3/B8）**      | 同上 + 无开发者账号                                                                                                                                                                                           |
| DEVICE_VERIFIED  | **BLOCKED（B3 + B18）**   | 无 macOS + 无设备                                                                                                                                                                                             |
| SIGNING_READY    | **BLOCKED（B8/B9）**      | 无 Apple Developer Account / 证书 / Provisioning                                                                                                                                                              |
| TESTFLIGHT_READY | **BLOCKED（B3 + B8）**    | —                                                                                                                                                                                                             |
| APPSTORE_READY   | **BLOCKED（B3 + B8/B9）** | —                                                                                                                                                                                                             |
| STORE_SUBMITTED  | **NO**                    | 本轮不提交                                                                                                                                                                                                    |

> 本轮在 Windows 侧已尽最大可能推进 iOS Source Ready：修复 `Package.swift` 空 target
> （`exclude` 导致 `DepMapCore` 无源文件）与 `LAPolicy()` 枚举实例化语法错误，
> 并重写 `docs/IOS_RELEASE_HANDOFF.md`（含黄金向量、算法契约、三个源自 Android 实测的易错点）。

## 4. Core（Node 22）

| 层              | 状态 | 证据                                                                                                                            |
| --------------- | ---- | ------------------------------------------------------------------------------------------------------------------------------- |
| SOURCE_READY    | PASS | `core/src/**`                                                                                                                   |
| BUILD_READY     | PASS | `npm ci` 可执行；`npm run check` EXIT=0                                                                                         |
| TESTED          | PASS | **453 passed / 453（43 文件）**；architecture 48 files/0 circular；network 129 files/0 原语；secrets 580 files/0；UI 30 `.uvue` |
| DEVICE_VERIFIED | N/A  | 纯库                                                                                                                            |
| STORE_READY     | N/A  | 不单独上架                                                                                                                      |

## 5. UI（uni-app x）

| 层                      | 状态                     | 原因                                                            |
| ----------------------- | ------------------------ | --------------------------------------------------------------- |
| SOURCE_READY            | PASS                     | 24 页 `.uvue` + 5 个 `dp-*` 组件 + 5 UTS 安全插件 + 设计 token  |
| STATIC_GATE             | PASS                     | `check-ui.mjs` 9 类机械校验（U1–U9），0 命中                    |
| **UI_COMPILED**         | **BLOCKED（B10）**       | HBuilderX 已装但 CLI 无 build 命令 → **从未被真实编译器验证过** |
| **UI_RUNTIME_VERIFIED** | **BLOCKED（B10 + B18）** | 无编译产物 + 无设备；详见 `PRODUCTION_RUNTIME_UI_AUDIT.md`      |
| DEVICE_VERIFIED         | **BLOCKED（B10 + B18）** | 同上                                                            |

---

## 6. 汇总（本轮结论，禁止合并成一句「已上线」）

```
CORE_SOURCE_READY          = PASS      CORE_BUILD_READY           = PASS
ANDROID_SOURCE_READY       = PASS      ANDROID_NATIVE_BUILD_READY = PASS
ANDROID_NATIVE_TESTED      = PASS      ANDROID_PRODUCT_BUILD_READY= BLOCKED (B10)
ANDROID_INSTALL_READY      = BLOCKED   ANDROID_DEVICE_VERIFIED    = BLOCKED (B10+B18)
ANDROID_SIGNING_READY      = BLOCKED (B4)

HARMONY_SOURCE_READY       = PASS      HARMONY_ENGINE_SKELETON    = PASS
HARMONY_NATIVE_BUILD_READY = PASS      HARMONY_PRODUCT_BUILD_READY= BLOCKED (B10)
HARMONY_INSTALL_READY      = BLOCKED (B7 unsigned)  HARMONY_DEVICE_VERIFIED = BLOCKED (B18)
HARMONY_SIGNING_READY      = BLOCKED (B7)

IOS_SOURCE_READY           = PARTIAL_WITH_REPORT (G-1/G-2/G-3)
IOS_BUILD_READY            = BLOCKED (B3, non-macOS)
IOS_INSTALL_READY          = BLOCKED (B3/B8)       IOS_DEVICE_VERIFIED = BLOCKED (B3+B18)
IOS_SIGNING_READY          = BLOCKED (B8/B9)

UI_SOURCE_READY            = PASS      UI_STATIC_GATE             = PASS
UI_COMPILED                = BLOCKED (B10)
UI_RUNTIME_VERIFIED        = BLOCKED (B10 + B18)

STORE_METADATA_READY       = PASS      STORE_ASSETS_READY         = PARTIAL_WITH_REPORT
STORE_SUBMISSION_READY     = BLOCKED   STORE_SUBMITTED            = NO
REAL_DATA_VALIDATED        = NOT_RUN
PRODUCTION_RC_V1           = PARTIAL_WITH_REPORT
```

**至少一个可实际运行目标平台 build PASS = YES**（Core / Node 22；且 Android 原生层与 HarmonyOS
原生层均已有真实构建产物）—— 满足 §136 该条要求。

**但三端的 PRODUCT_BUILD_READY / INSTALL_READY / DEVICE_VERIFIED / SIGNING_READY 全部未达成，
UI 从未编译，故不能声明三端上线。**
