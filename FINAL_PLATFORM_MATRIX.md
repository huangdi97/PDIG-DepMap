# FINAL_PLATFORM_MATRIX.md

> PDIG / DepMap — FINAL PRODUCTION CLOSURE V1，PHASE J–N。
> 每个状态对应**实测证据**或**明确 BLOCKED**；不存在「检测到程序存在即 PASS」的判定。

---

## 1. 汇总矩阵

| 平台                | SOURCE_READY | COMPILED    | BUILD_READY | DEVICE_VERIFIED | CRYPTO_VERIFIED | SIGNING_READY | STORE_READY |
| ------------------- | ------------ | ----------- | ----------- | --------------- | --------------- | ------------- | ----------- |
| **Android**         | **PASS**     | **BLOCKED** | **BLOCKED** | **BLOCKED**     | **BLOCKED**     | **BLOCKED**   | **BLOCKED** |
| **HarmonyOS**       | **PASS**     | **BLOCKED** | **BLOCKED** | **BLOCKED**     | **BLOCKED**     | **BLOCKED**   | **BLOCKED** |
| **iOS**             | **PASS**     | **BLOCKED** | **BLOCKED** | **BLOCKED**     | **BLOCKED**     | **BLOCKED**   | **BLOCKED** |
| **UI（uni-app x）** | **PASS**     | **BLOCKED** | **BLOCKED** | **BLOCKED**     | N/A             | N/A           | **BLOCKED** |
| **Core（Node）**    | **PASS**     | **PASS**    | **PASS**    | N/A             | **PASS**        | N/A           | N/A         |

> iOS 另需 `TESTFLIGHT_READY = BLOCKED`、`APPSTORE_READY = BLOCKED`。

---

## 2. Android

### 2.1 工具链实测（B1 前提修正）

| 组件           | 实测                                                                                                                                                                      |
| -------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Android SDK    | **存在** `<ANDROID_SDK_ROOT>`                                                                                                                                            |
| platforms      | `android-36.1`、`android-37.0`                                                                                                                                            |
| build-tools    | `36.1.0`、`37.0.0`                                                                                                                                                        |
| cmdline-tools  | `latest`（含 `sdkmanager.bat`）                                                                                                                                           |
| licenses       | **7 个 license 哈希文件全部存在 → 已接受**                                                                                                                                |
| JDK            | **17.0.12**（`<DEVECO_HOME>\jbr`）；**21.0.10**（`<ANDROID_STUDIO_HOME>\jbr`）                                                                           |
| Android Studio | **存在**（`AI-253.32098.37.2534.15232325`）                                                                                                                               |
| adb            | `<ANDROID_SDK_ROOT>\adb.exe` 可用                                                                                                                                                 |
| Gradle         | **无可用发行版** —— `~/.gradle/wrapper/dists/gradle-9.3.1-bin/` 仅 0 字节 `.part`/`.lck`；Android Studio 内无完整发行版；工程内无 `gradlew` / `gradle-wrapper.properties` |
| 构建依赖网络   | `repo1.maven.org` HTTP 200；**Gradle 发行包 CDN `curl` exit 7（不可达）**                                                                                                 |

### 2.2 状态判定

| 状态            | 判定        | 依据                                                                                                                                                                                                                                                                                                                                                                             |
| --------------- | ----------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| SOURCE_READY    | **PASS**    | `platforms/android/{settings,build}.gradle.kts`、`core/build.gradle.kts`、`core/src/main/AndroidManifest.xml`、5 个 Kotlin 源（`DepmapContainerV1`、`DepmapSchemaV1`、`BiometricGateAdapter`、`KeystoreSecureKeyAdapter`、`SqlCipherSecureDatabaseAdapter`）、1 个 golden 互操作测试；manifest 最小权限（仅 `USE_BIOMETRIC`）、`allowBackup=false`、`usesCleartextTraffic=false` |
| COMPILED        | **BLOCKED** | 无可用 Gradle 发行版；AGP 8.5.2 / Kotlin 2.0.0 / androidx / SQLCipher / BouncyCastle 需联网解析且 CDN 不可达；`compileSdk = 34` 但已装 platform 为 36.1/37.0                                                                                                                                                                                                                     |
| BUILD_READY     | **BLOCKED** | 同上；无 APK/AAB 产物                                                                                                                                                                                                                                                                                                                                                            |
| DEVICE_VERIFIED | **BLOCKED** | `adb devices` → 无设备                                                                                                                                                                                                                                                                                                                                                           |
| CRYPTO_VERIFIED | **BLOCKED** | `DepmapContainerV1GoldenTest.kt` 未执行                                                                                                                                                                                                                                                                                                                                          |
| SIGNING_READY   | **BLOCKED** | 无 release keystore（`signing/` 已 gitignore）                                                                                                                                                                                                                                                                                                                                   |
| STORE_READY     | **BLOCKED** | 依赖上述全部 + B11（applicationId 为占位 `com.example.depmap`）+ B4/B5                                                                                                                                                                                                                                                                                                           |

### 2.3 解除动作（精确）

1. 获取可用 Gradle 发行版（`gradle-8.9-bin.zip` 或项目内新增 gradle wrapper）——
   注意 `services.gradle.org` 首跳 **307** 至 `github.com/gradle/gradle-distributions`，
   而**该最终地址当前 `curl: (7) CONNECT tunnel failed, 502` 不可达**（2026-09-14 复测），
   故需**离线投放**发行包，或放开该域名；
2. 允许 `google()` / `mavenCentral()` 制品下载；
3. `compileSdk` 对齐已安装 platform（34 未装；可装 34 或升至 36）→ **改动前需评审**；
4. 连接真机或启动 emulator；
5. 提供 release keystore + 正式 applicationId。

---

## 3. HarmonyOS

### 3.1 工具链实测（B2 前提修正）

| 组件           | 实测                                                                                                                                            |
| -------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| DevEco Studio  | **存在** `<DEVECO_HOME>`，version `5.0.5.310`                                                                                   |
| SDK            | `sdk/default/openharmony/{ets,js,native,previewer,toolchains}` + `sdk/default/hms/*`；`apiVersion 13`、`version 5.0.1.115`、`metaVersion 3.0.0` |
| hvigor         | `5.13.2`（内置）+ `@ohos/hvigor-ohos-plugin 5.13.2`                                                                                             |
| ohpm           | `5.0.10`（实测可执行）；registry `https://ohpm.openharmony.cn/ohpm/`                                                                            |
| 内置 node      | `v18.20.1`                                                                                                                                      |
| 工程模板       | `plugins/{openharmony,harmony}/lib/templates/**` 齐备                                                                                           |
| 工具链可执行性 | **已验证**：`hvigorw.js` 成功引导 pnpm 并解析工程模型                                                                                           |

### 3.2 已真实执行的构建（非仅检测存在）

在临时目录构造最小 Stage-model 工程（AppScope / `build-profile.json5` / `hvigorfile.ts` / `oh-package.json5` / `hvigor-config.json5` / entry 模块 / `EntryAbility.ets` / `Index.ets` / resources），执行：

```
node ".../tools/hvigor/bin/hvigorw.js" assembleHap --mode module -p product=default -p buildMode=debug --no-daemon
```

实测演进：

| 轮次  | hvigor 输出                                                                                                                                                                                                                                                                                                                                                                                                                         |
| ----- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1     | `hvigor ERROR: Please configure compileSdkVersion in the product.`                                                                                                                                                                                                                                                                                                                                                                  |
| 2     | `hvigor ERROR: Unsupported modelVersion of Hvigor 5.0.5. Detail: The supported Hvigor modelVersion is 5.0.2`                                                                                                                                                                                                                                                                                                                        |
| 3     | `Pnpm install success.` → 工程模型解析成功 → `hvigor ERROR: Unable to find the following components: toolchains:13 / ArkTS:13 / js:13 / native:13 / previewer:13`                                                                                                                                                                                                                                                                   |
| 4     | 调整 `sdk.dir` 至 `sdk\default` 后仍为同一组件解析错误                                                                                                                                                                                                                                                                                                                                                                              |
| **5** | **FINAL PRODUCTION CLOSURE V1 重跑（2026-09-14，第 144 节）**：在 OS 临时目录重建最小 Stage 工程（17 文件）后重跑 → **精确复现**组件错误，并暴露**新根因**：`OhRemoteComponentLoader` 请求 `https://repo.harmonyos.com/sdkmanager/v5/ohos/getSdkList` 返回 **HTTP 400**（`statusCode=undefined`）→ `TypeError: datas is not iterable`，即**远端组件列表路径亦不可用**；`hvigor ERROR: BUILD FAILED in 24 s 166 ms`，**无 HAP 产物** |

hvigor 自带修复建议：_Go to File > Settings > OpenHarmony SDK, download the components, and sync the project. Open SDK Manager._

### 3.3 状态判定

| 状态            | 判定        | 依据                                                                                                                                                                                                                                           |
| --------------- | ----------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| SOURCE_READY    | **PASS**    | `platforms/harmonyos/{app.json5, arkts/RelationalStoreSecureAdapter.ets, entry/src/main/module.json5}`；ArkTS 适配器使用 `@kit.ArkData` / `@kit.CryptoArchitectureKit` / `@kit.UserAuthenticationKit` / `@kit.ArkTS`；`requestPermissions: []` |
| COMPILED        | **BLOCKED** | SDK 组件（API 13）未被 hvigor 本地组件加载器识别，需 DevEco **SDK Manager 图形化同步**                                                                                                                                                         |
| BUILD_READY     | **BLOCKED** | 同上；且 `platforms/harmonyos/` 为**适配器片段**，非可构建 Stage 工程（缺 AppScope / build-profile / hvigorfile / oh-package / EntryAbility / resources / profile）                                                                            |
| DEVICE_VERIFIED | **BLOCKED** | 无设备                                                                                                                                                                                                                                         |
| CRYPTO_VERIFIED | **BLOCKED** | HUKS / UserAuth / ArkData 未运行验证                                                                                                                                                                                                           |
| SIGNING_READY   | **BLOCKED** | 无 HarmonyOS release 签名（B7）                                                                                                                                                                                                                |
| STORE_READY     | **BLOCKED** | 依赖上述 + B6（Huawei Developer / AGC 身份）+ B11（bundleName 占位）                                                                                                                                                                           |

### 3.4 重要说明

**产品 HarmonyOS 产物由 HBuilderX（uni-app x）产出，非 DevEco 直接产出。** `platforms/harmonyos/` 仅承载原生适配器参考实现。因此即使 DevEco 构建打通，产品级 HarmonyOS 构建仍受 **B10** 约束。

### 3.5 解除动作（精确）

1. 在 DevEco Studio 中打开任一 HarmonyOS 工程并执行 **SDK Manager 同步**（图形化步骤，需人工）；
2. 将 `platforms/harmonyos/` 补全为可构建 Stage 工程（属新实现工作，非本轮收口范围）；
3. 解除 B10 以获得产品级 HarmonyOS 产物；
4. 提供 HarmonyOS 签名材料与正式 bundleName。

---

## 4. iOS

### 4.1 工具链实测

| 组件          | 实测                               |
| ------------- | ---------------------------------- |
| OS            | `MINGW64_NT-10.0-26200`（Windows） |
| macOS / Xcode | **无**（`xcodebuild` 不存在）      |

### 4.2 状态判定

| 状态                   | 判定                       | 依据                                                                                                                                                                                                                                                                                                                                                                                                                              |
| ---------------------- | -------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| SOURCE_READY           | **PASS（最大化）**         | `platforms/ios/Package.swift`（swift-tools 5.9；iOS 14+/macOS 12+；**无第三方依赖**；`SQLCipherSecureDatabaseAdapter.swift` 显式 exclude，由 app target 链接）；`platforms/ios/swift/SQLCipherSecureDatabaseAdapter.swift`；`platforms/ios/Tests/DepMapCoreTests/DepmapContainerV1Tests.swift`（golden 互操作）；`app/manifest.json` 含 `dSYMs:false`、`capabilities.entitlements`、`privacyDescription`、`UIBackgroundModes: []` |
| COMPILED / BUILD_READY | **BLOCKED（macOS/Xcode）** | 环境不可用（预期内外部阻塞）                                                                                                                                                                                                                                                                                                                                                                                                      |
| DEVICE_VERIFIED        | **BLOCKED**                | 同上                                                                                                                                                                                                                                                                                                                                                                                                                              |
| SIGNING_READY          | **BLOCKED**                | 需 Apple Developer Account（B8）+ 签名/provisioning（B9）                                                                                                                                                                                                                                                                                                                                                                         |
| TESTFLIGHT_READY       | **BLOCKED**                | 同上                                                                                                                                                                                                                                                                                                                                                                                                                              |
| APPSTORE_READY         | **BLOCKED**                | 同上                                                                                                                                                                                                                                                                                                                                                                                                                              |

### 4.3 Mac 交接

交接文档：`docs/IOS_RELEASE_HANDOFF.md`、`docs/IOS_MAC_HANDOFF.md`（含 Xcode 步骤、build/test 命令、archive、signing、device、TestFlight）。

**未伪造 iOS 构建结果。** `IOS_BUILD_READY = BLOCKED`，非 PASS。

---

## 5. 平台契约一致性（第 89 节）

| 契约项               | Android                    | HarmonyOS    | iOS                            | Core（参照）                       | 一致性                                     |
| -------------------- | -------------------------- | ------------ | ------------------------------ | ---------------------------------- | ------------------------------------------ |
| schemaVersion        | `DepmapSchemaV1.kt`        | ArkTS 适配器 | Swift 适配器                   | `SCHEMA_VERSION = 3`               | 见 `docs/CROSS_PLATFORM_CONTRACT_AUDIT.md` |
| depmap formatVersion | `DepmapContainerV1.kt`     | —            | `DepmapContainerV1Tests.swift` | `DEPMAP_FORMAT_VERSION = 1`        | golden vector 冻结                         |
| UTF-8 字节语义       | 密码精确 UTF-8（无归一化） | 同           | 同                             | 同（`crypto/depmap.test.ts` 覆盖） | 一致                                       |
| Base64               | 标准                       | 标准         | 标准                           | 标准                               | 一致                                       |
| error codes          | `auth_failed` 等           | 同           | 同                             | 同                                 | 一致（wrong password 与密文篡改不可区分）  |
| relation vocabulary  | 由 Core 定义               | 同           | 同                             | `RelationDefinitionRegistry`       | 一致                                       |

> 契约一致性为**源码级**结论；**跨平台运行期互操作验证 = BLOCKED**（需三端构建环境）。

---

## 6. 平台阻塞精确记录（替代旧口径）

| #           | 旧口径                                  | 修正后口径                                                                                                                                                                                                         |
| ----------- | --------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| B1          | 无 JDK17+ / Android SDK / Gradle        | **JDK 17.0.12 + 21.0.10 与 Android SDK 均已安装、license 已接受**；实际阻塞 = 无可用 Gradle 发行版 + 构建依赖 CDN 不可达 + `compileSdk 34` 未安装 + 无真机 + 无 keystore                                           |
| B2          | 无 DevEco Studio / HarmonyOS SDK        | **DevEco 5.0.5.310 + SDK API 13 + hvigor 5.13.2 + ohpm 5.0.10 均存在且可执行，hvigor 已真实运行至工程解析**；实际阻塞 = SDK 组件（API 13）需 DevEco SDK Manager 图形化同步；且 `platforms/harmonyos/` 为适配器片段 |
| B3          | 无 macOS / Xcode                        | **成立**（Windows，预期内）                                                                                                                                                                                        |
| B10         | 无 HBuilderX / uni-app x 工具链         | **成立** —— 产品级关键阻塞（全盘搜索无命中）                                                                                                                                                                       |
| B20/B21/B22 | App 端解析 / 加解密 / Core→UTS 桥接缺失 | **成立**（解除依赖 B10）                                                                                                                                                                                           |

---

## 7. 复现命令

```
# 工具链探测
java -version
ls /d/Code/Android/SDK/{platforms,build-tools,licenses}
"<DEVECO_HOME>/jbr/bin/javac.exe" -version
node "<DEVECO_HOME>/tools/ohpm/bin/pm-cli.js" -v
node "<DEVECO_HOME>/tools/hvigor/bin/hvigorw.js" --version

# Core 侧（唯一可执行 Gate）
cd core && npm run check:full
```
