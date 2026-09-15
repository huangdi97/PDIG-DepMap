# PLATFORM_RELEASE_MATRIX.md — PDIG 平台发布矩阵

> 分层判定：`SOURCE_READY` → `TOOLCHAIN_READY` → `BUILD_READY` → `INSTALL_READY` →
> `DEVICE_VERIFIED` → `SIGNING_READY` → `STORE_READY` → `STORE_SUBMITTED`。
>
> **不得把以上合并成「已上线」。** 每条都必须能指到一个真实命令或真实文件。
>
> 状态取值只允许：`PASS` / `FAIL` / `BLOCKED` / `NOT_RUN` / `PARTIAL_WITH_REPORT`。
>
> **2026-09-15 重写**：旧版称「本机无 JDK / 无 Android SDK / 无 DevEco / 无 HBuilderX」，
> 经实测**全部与事实不符**；旧版还把 B10 描述为「CLI 无 build 命令」，本轮实测
> `cli pack` 是 DCloud 官方文档定义的 uni-app x 打包命令，真正的闸门是**账号**。
> 详见 `AGENT_PLATFORM_HANDOFF_AUDIT.md` 与 `docs/UTS_COMPILE_VERIFICATION.md`。

---

## 0. 本轮新发生的两件事（区别于历史所有轮次）

| 事项                            | 证据                                                                                                                                   |
| ------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------- |
| **UTS 三端编译首次真实验证**    | `npm run check:uts` → **15/15 compiled, PASS**（Android→Kotlin / iOS→Swift / HarmonyOS→ArkTS）。并借此查出并修复 2 个 iOS UTS 语法错误 |
| **前序 Agent 的零提交工作入库** | 19 个已跟踪文件 + 52 项未跟踪内容全部保全，按 11 个逻辑提交入库；`git status -uall` 归零                                               |

同时必须明确的三条边界：

1. **AAR ≠ APK**：Android 产物是 Android Library，不可安装。
2. **HAP 是原生验证工程产物**：只含最小 `EntryAbility` + `Index.ets`，**不含** 24 个 `.uvue`，且 unsigned。
3. **`.uvue` 从未被真实编译器验证过**：`check:ui` 是静态门，不等于编译通过。
   UTS 门禁覆盖的是 `utssdk/**`，**不覆盖页面**。

---

## 1. UI（uni-app x）

| 状态               | 结果                         | 证据 / 原因                                                                                         |
| ------------------ | ---------------------------- | --------------------------------------------------------------------------------------------------- |
| `UI_SOURCE_READY`  | **PASS**                     | 24 页 `.uvue` + 5 个 `dp-*` 组件 + 5 个 UTS 插件 + 设计 token；`pages.json` 24/24 解析、tabBar 4/4  |
| `UI_STATIC_GATE`   | **PASS**                     | `check:ui`（U1–U9）0 命中；30 `.uvue`、34 色 token                                                  |
| `UI_BUILD_READY`   | **BLOCKED（B10）**           | `.uvue` 需 HBuilderX 的 uni-app x 编译器；`cli pack` 存在但需 **DCloud 账号登录 + 云打包配额**      |
| `UI_RUNTIME_READY` | **BLOCKED（B10 + B18/B24）** | 无编译产物 + 无设备                                                                                 |
| `UI_UX_READY`      | **PARTIAL_WITH_REPORT**      | 源码级判定通过（首页 3 秒原则、无内部工程术语、中文映射、空/错/加载态齐备）；**运行时判定 NOT_RUN** |
| `UI_DEVICE_QA`     | **NOT_RUN**                  | 无设备；逐页 QA 清单已备（`PRODUCTION_RUNTIME_UI_AUDIT.md`）                                        |

## 2. Android

| 状态                         | 结果                     | 证据 / 原因                                                                                       |
| ---------------------------- | ------------------------ | ------------------------------------------------------------------------------------------------- |
| `ANDROID_SOURCE_READY`       | **PASS**                 | `platforms/android/**`（Gradle KTS + Kotlin 安全层 + golden test + Manifest）                     |
| `ANDROID_TOOLCHAIN_READY`    | **PASS**                 | JDK 21.0.10（AGP 可用）+ Android SDK（platforms 34/36.1/37.0、build-tools、licenses）+ Gradle 8.9 |
| `ANDROID_NATIVE_BUILD_READY` | **PASS**                 | `assembleDebug/Release` → BUILD SUCCESSFUL；AAR ×2（72,376 / 68,731 B）                           |
| `ANDROID_NATIVE_TESTED`      | **PASS**                 | 黄金向量 4/4 × debug+release = 8/8 PASS（0 failures / 0 errors / 0 skipped）                      |
| `ANDROID_BUILD_READY`        | **BLOCKED（B10）**       | **产品级 APK/AAB 不可产出** —— 需 HBuilderX 打包（账号）                                          |
| `ANDROID_INSTALL_READY`      | **BLOCKED（B10 + B24）** | 无安装包；且 `adb devices` 为空                                                                   |
| `ANDROID_DEVICE_VERIFIED`    | **BLOCKED（B10 + B24）** | 无产物 + 无设备。唯一 AVD `Medium_Phone_API_35` 缺 system image，不可启动（不得伪造模拟器）       |
| `ANDROID_SIGNING_READY`      | **BLOCKED（B4）**        | 无 release keystore（用户提供；禁止自动生成生产凭据、禁止入库）                                   |
| `ANDROID_STORE_READY`        | **BLOCKED**              | 依赖 B10 + B4 + B5（Google Play 账号）+ B11（正式包名）                                           |

## 3. HarmonyOS

| 状态                         | 结果                    | 证据 / 原因                                                                                                     |
| ---------------------------- | ----------------------- | --------------------------------------------------------------------------------------------------------------- |
| `HARMONY_SOURCE_READY`       | **PASS**                | `platforms/harmonyos/**`（完整 DevEco Stage 工程骨架）+ `app-harmony` UTS 编译通过                              |
| `HARMONY_TOOLCHAIN_READY`    | **PASS**                | DevEco 5.0.5.310 + SDK 5.0.1.115（API 13）+ hvigor 5.13.2 + ohpm 5.0.10                                         |
| `HARMONY_NATIVE_BUILD_READY` | **PASS**                | `hvigorw assembleHap` → **BUILD SUCCESSFUL in 42 s 263 ms**；HAP 18,986 B，含 `ets/modules.abc`（ArkTS 字节码） |
| `HARMONY_BUILD_READY`        | **BLOCKED（B10）**      | **产品级 HAP 不可产出** —— 当前 HAP 是原生验证工程产物，不含 24 页 `.uvue`                                      |
| `HARMONY_INSTALL_READY`      | **BLOCKED（B10 + B7）** | HAP unsigned；无签名材料与正式 bundleName                                                                       |
| `HARMONY_DEVICE_VERIFIED`    | **BLOCKED（B18）**      | 无 HarmonyOS 设备（`hdc` 无可用目标）                                                                           |
| `HARMONY_SIGNING_READY`      | **BLOCKED（B7）**       | 需 AGC 证书 / Profile                                                                                           |
| `HARMONY_STORE_READY`        | **BLOCKED**             | 依赖 B10 + B7 + B6（华为开发者身份）+ B11                                                                       |

## 4. iOS

| 状态                   | 结果                      | 证据 / 原因                                                                                                                                                                                                                                                                 |
| ---------------------- | ------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `IOS_SOURCE_READY`     | **PARTIAL_WITH_REPORT**   | `platforms/ios/**` 存在；本轮修复 2 个 UTS 语法错误 + 1 处不存在符号引用（`DepmapSchemaV1.shared`）；<br>**未闭环的结构性缺口**：**G-1** `swift/` 无 `DepmapContainerV1.swift`（iOS 侧无 `.depmap` 容器实现）；**G-2** SQLCipher 未接入 SPM；**G-3** 容器测试全部 `XCTSkip` |
| `IOS_UTS_COMPILED`     | **PASS**                  | 5/5 UTS → Swift 编译通过（语法/降级层；`npm run check:uts`）                                                                                                                                                                                                                |
| `IOS_TOOLCHAIN_READY`  | **BLOCKED（B3）**         | 非 macOS，无 `xcodebuild`。**不得伪造**                                                                                                                                                                                                                                     |
| `IOS_BUILD_READY`      | **BLOCKED（B3）**         | 同上                                                                                                                                                                                                                                                                        |
| `IOS_DEVICE_VERIFIED`  | **BLOCKED（B3 + B18）**   | 无 macOS + 无设备                                                                                                                                                                                                                                                           |
| `IOS_SIGNING_READY`    | **BLOCKED（B8/B9）**      | 无 Apple Developer Account / 证书 / Provisioning                                                                                                                                                                                                                            |
| `IOS_TESTFLIGHT_READY` | **BLOCKED（B3 + B8）**    | —                                                                                                                                                                                                                                                                           |
| `IOS_APPSTORE_READY`   | **BLOCKED（B3 + B8/B9）** | —                                                                                                                                                                                                                                                                           |

> Mac 侧执行手册见 `docs/IOS_RELEASE_HANDOFF.md`（分步命令 + 源自 Android 实测的易错点 + 本轮 UTS 结论）。

## 5. Core / 工程

| 状态                      | 结果                    | 证据                                                                                                            |
| ------------------------- | ----------------------- | --------------------------------------------------------------------------------------------------------------- |
| `CORE_SOURCE_READY`       | **PASS**                | `core/src/**`                                                                                                   |
| `CORE_BUILD_READY`        | **PASS**                | `npm ci` 可执行；`npm run check` EXIT=0                                                                         |
| `CORE_TESTED`             | **PASS**                | **453 passed / 453（43 文件）**；architecture 48 files/circular 0；network 0 原语；secret 0；UI 静态门 PASS     |
| `ENGINEERING_BASELINE_V1` | **PASS**                | 未降低 type safety / architecture / privacy / crypto                                                            |
| `MVP01/02/03_REGRESSION`  | **PASS**                | 同一套 453 测试（MVP01–03 的套件全部在内）                                                                      |
| `UTS_SOURCE_READY`        | **PASS**                | 15 个 UTS 实现（5 插件 × 3 平台）全部存在                                                                       |
| `UTS_DOWNLEVEL_COMPILED`  | **PASS（本轮新增）**    | `npm run check:uts` → 15/15                                                                                     |
| `UTS_HOST_LINKED`         | **NOT_RUN**             | 跨语言符号解析需 HBuilderX / Xcode（B10 / B3）                                                                  |
| `CLEAN_INSTALL`           | **PARTIAL_WITH_REPORT** | lockfile ↔ manifest 同步、`check:deps` tree/lockfile OK；**破坏性 `npm ci`** 被沙箱 safe-delete 守卫拦截（B23） |
| `CLEAN_CLONE`             | **BLOCKED（B23）**      | 工作区外批量写入被沙箱截断。等价证据：`git status -uall` = 0 行 ⇒ 磁盘树 ≡ 提交树，且该树上全门禁 EXIT=0        |
| `MUTATION`                | **PARTIAL_WITH_REPORT** | Core 侧本轮未改动正确性代码；引用与当前 HEAD 兼容的近期 targeted mutation 证据                                  |

## 6. 商店 / 数据

| 状态                     | 结果                    | 证据 / 原因                                                                |
| ------------------------ | ----------------------- | -------------------------------------------------------------------------- |
| `STORE_METADATA_READY`   | **PARTIAL_WITH_REPORT** | `store/*` 5 份就绪；正式标识与品牌名未决（B11/B14）                        |
| `STORE_ASSETS_READY`     | **BLOCKED**             | 图标/启动图仅**占位**（U-1 已闭环）；截图依赖可运行环境（B17）             |
| `STORE_SUBMISSION_READY` | **BLOCKED**             | 依赖 B10 + B4–B9 + B11 + B12/b                                             |
| `STORE_SUBMITTED`        | **NO**                  | 本轮不提交（未授权），也未访问任何开发者后台                               |
| `REAL_DATA_CORRECTNESS`  | **NOT_RUN**             | 无真实账单（B13）；`validate-real-bill.ts` 已就绪；**禁止** synthetic 冒充 |
| `REAL_DATA_VALUE`        | **NOT_RUN**             | 同上；需用户决策（B19）                                                    |

---

## 7. 汇总（禁止合并成一句「已上线」）

```
UI_SOURCE_READY            = PASS       UI_BUILD_READY             = BLOCKED (B10)
UI_RUNTIME_READY           = BLOCKED    UI_UX_READY                = PARTIAL_WITH_REPORT

ANDROID_SOURCE_READY       = PASS       ANDROID_TOOLCHAIN_READY    = PASS
ANDROID_NATIVE_BUILD_READY = PASS       ANDROID_NATIVE_TESTED      = PASS
ANDROID_BUILD_READY        = BLOCKED (B10)
ANDROID_INSTALL_READY      = BLOCKED    ANDROID_DEVICE_VERIFIED    = BLOCKED
ANDROID_SIGNING_READY      = BLOCKED (B4)                          ANDROID_STORE_READY = BLOCKED

HARMONY_SOURCE_READY       = PASS       HARMONY_TOOLCHAIN_READY    = PASS
HARMONY_NATIVE_BUILD_READY = PASS       HARMONY_BUILD_READY        = BLOCKED (B10)
HARMONY_INSTALL_READY      = BLOCKED    HARMONY_DEVICE_VERIFIED    = BLOCKED (B18)
HARMONY_SIGNING_READY      = BLOCKED (B7)                          HARMONY_STORE_READY = BLOCKED

IOS_SOURCE_READY           = PARTIAL_WITH_REPORT (G-1/G-2/G-3)
IOS_UTS_COMPILED           = PASS       IOS_TOOLCHAIN_READY        = BLOCKED (B3)
IOS_BUILD_READY            = BLOCKED    IOS_DEVICE_VERIFIED        = BLOCKED
IOS_SIGNING_READY          = BLOCKED    IOS_TESTFLIGHT_READY       = BLOCKED
IOS_APPSTORE_READY         = BLOCKED

UTS_SOURCE_READY           = PASS       UTS_DOWNLEVEL_COMPILED     = PASS (15/15)
UTS_HOST_LINKED            = NOT_RUN

CORE_SOURCE_READY          = PASS       CORE_BUILD_READY           = PASS
CORE_TESTED                = PASS (453) ENGINEERING_BASELINE_V1    = PASS
MVP01/02/03_REGRESSION     = PASS       CLEAN_INSTALL              = PARTIAL_WITH_REPORT
CLEAN_CLONE                = BLOCKED (B23)
MUTATION                   = PARTIAL_WITH_REPORT

STORE_METADATA_READY       = PARTIAL_WITH_REPORT
STORE_ASSETS_READY         = BLOCKED    STORE_SUBMISSION_READY     = BLOCKED
STORE_SUBMITTED            = NO
REAL_DATA_CORRECTNESS      = NOT_RUN    REAL_DATA_VALUE            = NOT_RUN

PRODUCTION_RC_V1           = PARTIAL_WITH_REPORT
```

**为什么不是 `PASS`**：Production RC PASS 规则要求「至少一个真实移动端 runtime smoke PASS」。
本轮**没有任何移动端 runtime**（无 APK/HAP 产品包、无设备），因此只能 `PARTIAL_WITH_REPORT`。
这不是「代码没写完」，而是**唯一技术闸门 B10（DCloud 账号）尚未通过**。

**为什么也不是 `FAIL`**：所有真正可自动推进的工作本轮都已推进到环境极限 ——
Core 全绿、Android/Harmony 原生层真实构建、UTS 三端首次真实编译、现场与文档收口干净。
