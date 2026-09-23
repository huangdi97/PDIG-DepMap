# ANDROID_PLATFORM_BASELINE.md

> 生成时间：2026-09-23（ANDROID_2026_PRODUCTION_REALITY_CLOSURE · API36 工程全速收口）
> 依据：粘贴 Goal §6「Phase B — Android 16 / API36 Production Delta」+ 批准契约 B1/B2。
> 本文档用 **maven 依赖树、build.gradle.kts 实查、Gradle 运行输出** 为准，不用猜。

---

## 0. 结论

| 项 | 值 | 备注 |
|----|----|------|
| compileSdk | **36** | Android 16 (Baklava) 平台 |
| targetSdk | **36** | 本轮由基线 `b13f2f7` 从 34 提升到 36 |
| minSdk | **26** | Android 8.0，保持不变（只能提高、不降低） |
| AGP | **8.5.2** | 官方测试上限为 compileSdk 34；已显式声明 `android.suppressUnsupportedCompileSdk=36` |
| Gradle | **8.9**（wrapper） | `android/gradle/wrapper/gradle-wrapper.properties` 实查 |
| JDK | **OpenJDK 21.0.10（Android Studio JBR）** | `build.gradle.kts` 顶部强制检查 `VERSION_21` |
| Kotlin | **2.0.0** | `kotlin("jvm")` / `kotlin("plugin.serialization")` / `kotlin.plugin.compose` 同名版本 |
| Compose BOM | **2024.09.02** | 解析到 compose-ui 1.7.2 / material3 1.3.0 / material-icons-core 1.7.2 |
| Navigation Compose | **2.8.1** | `androidx.navigation:navigation-compose` |
| AndroidX Activity | **1.9.1** | `androidx.activity:activity-compose` |
| Biometric | **1.1.0** | `androidx.biometric:biometric` |
| SQLCipher | **4.5.5** | `net.zetetic:sqlcipher-android` + `androidx.sqlite:sqlite-framework:2.4.0` |
| R8 / minify | **未启用**（`isMinifyEnabled = false`，如实记录，非生产缺口） | 见 §4 |
| 构建输出重定向 | `%USERPROFILE%\pdig-build\<module>` | Windows 非 ASCII 仓库路径必需（`settings.gradle.kts`） |

---

## 1. 平台版本详细审计

### 1.1 SDK / compileSdk / targetSdk / minSdk（`android/app/build.gradle.kts` 实查）

```kotlin
namespace = "com.pdig.app"
compileSdk = 36
defaultConfig {
    applicationId = "com.pdig.app"
    minSdk = 26
    targetSdk = 36
    versionCode = 1
    versionName = "0.1.0-milestone"
}
```

- **compileSdk 34 → 36**、**targetSdk 34 → 36** 由提交 `b13f2f7`
  （`build(android): raise compileSdk/targetSdk to 36 (Android 16) baseline`）完成。
- **minSdk 26 不变**（SQLCipher / AES-256-GCM / BiometricPrompt / FileProvider 的能力下限；不降低、本轮不提高）。
- AVD 实测：`ro.build.version.sdk=36`、`ro.build.version.release=16`、`ro.build.version.codename=REL`
  （API36 Google APIs x86_64，pixel_9，1080×2424 @420dpi）。

### 1.2 AGP / Gradle / JDK / Kotlin

| 项 | 值 | 证据 |
|----|----|------|
| Android Gradle Plugin | `8.5.2` | `android/build.gradle.kts` `id("com.android.application") version "8.5.2" apply false` |
| Gradle | `8.9` | `android/gradle/wrapper/gradle-wrapper.properties` `distributionUrl=.../gradle-8.9-bin.zip` |
| JDK | OpenJDK 21.0.10（Android Studio `jbr`） | `D:\Code\Android Studio\jbr\bin\java.exe -version` → `openjdk version "21.0.10"`；`android/build.gradle.kts` 前置检查 `if (!JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_21)) throw` |
| Kotlin | `2.0.0` | `android/build.gradle.kts` 三个 `kotlin(...) version "2.0.0"`；依赖树 `kotlin-stdlib:2.0.0` |
| Java/Kotlin 目标 | `VERSION_17` / `jvmTarget = "17"`（:app）；`:core`/`:conformance` 用 `jvmToolchain(21)` | `android/app/build.gradle.kts` `compileOptions` + `kotlinOptions` |

> AGP 8.5.2 官方文档测试上限为 compileSdk 34；用于 36 会产生一条可抑制警告。
> 本轮已在 `gradle.properties` 显式声明 `android.suppressUnsupportedCompileSdk=36`，
> 且 `assembleDebug / assembleRelease / bundleRelease` 在 API36 上均 SUCCESSFUL（见 §5）。

### 1.3 关键 AndroidX / 加密栈版本（`android/app/build.gradle.kts` 实查）

| 依赖 | 版本 | 用途 |
|------|------|------|
| compose-bom | `2024.09.02` | UI 全家桶（ui 1.7.2 / material3 1.3.0 / icons-core 1.7.2） |
| androidx.activity:activity-compose | `1.9.1` | Activity + Compose + ActivityResultRegistry（D-16 依据） |
| androidx.navigation:navigation-compose | `2.8.1` | 导航 |
| androidx.lifecycle:lifecycle-viewmodel-compose / runtime-compose | `2.8.5` | ViewModel |
| androidx.biometric:biometric | `1.1.0` | 生物识别 |
| androidx.fragment:fragment | `1.7.1` | **显式钉住**：biometric 1.1.0 传递依赖 fragment 1.2.5（2020，requestCode 16 位上限崩溃源），必须与 activity 1.9.x 同代 |
| androidx.security:security-crypto | `1.1.0-alpha06` | 加密 |
| androidx.sqlite:sqlite-framework | `2.4.0` | SQLite 绑定 |
| net.zetetic:sqlcipher-android | `4.5.5` | SQLCipher 加密库 |
| androidx.core:core-ktx | `1.13.1` | KTX |
| kotlinx-coroutines-android | `1.8.1` | 协程 |
| org.bouncycastle:bcprov-jdk18on | `1.78.1`（:core） | Argon2id/密码学原语 |
| org.xerial:sqlite-jdbc | `3.46.1.0`（:core/:conformance 测试） | JVM 侧 SQLite |

---

## 2. 为什么升到 API36（Play 合规）

- Google Play 对 2026 年新应用/更新的 **targetSdk 要求**：Android 16（API 36）成为近期 Submission 的
  **新手机类**目标。粘贴 Goal §7 明确：`compileSdk >= 36 且 targetSdk >= 36` 是 Production 最终标准，
  不允许以 API35 继续作为正式提交流轨。
- 本轮基线 05f9887（canonical freeze）时 compileSdk/targetSdk 仍为 34；`b13f2f7` 已在
  冻结后立即完成 36 提升并执行了第一轮 API36 上的验证（`:core:test` 71、`:app:testDebugUnitTest` 9、
  `:conformance:run` 91、assembleDebug SUCCESSFUL）。
- 未申请 Play deadline extension 替代工程修复 —— 本轮继续在 API36 AVD 上做全量回归与
  行为 Gate（edge-to-edge / predictive back / adaptive layout），详见 `ANDROID_16_API36_CLOSURE_REPORT.md`。

---

## 3. Windows 非 ASCII 路径构建适配（必要前提）

仓库路径 `E:\AI\号卡管理` 含中文，AGP 在 Windows 上拒绝非 ASCII 工程路径（b.android.com/95744）。
`android/settings.gradle.kts` 内置两项适配（不依赖仓库外文件）：

1. `android.overridePathCheck=true`（gradle.properties，官方支持）
2. 构建输出与 `java.io.tmpdir` 重定向到纯 ASCII 根 `%USERPROFILE%\pdig-build\<module>` /
   `%USERPROFILE%\pdig-build\tmp`，修复两个实测缺陷（test worker @argfile UTF-8 破坏、
   Kotlin 编译器存活标记写入 %WINDIR% 失败）。

本轮所有构建产物路径为 `C:\Users\Kaiser\pdig-build\app\outputs\...`（全 ASCII），见 §5。

---

## 4. R8 / minify 现状（如实记录）

- `android/app/build.gradle.kts`：`release { isMinifyEnabled = false }` —— **release 构建未启用 minify/R8 代码收缩与混淆**。
- 记录为 **NOT_APPLICABLE（工程已生成但未启用收缩）**：粘贴 Goal §70 要求「若 minification 未启用：记录真实配置。
  不要为了『看起来像 Production』临时开启 R8 后不测试」。
- 本轮**不**擅自开关 R8；如需开启属未来 RC 决策（写入 `FUTURE.md`/版本策略文档，不由本轮自动决定）。
- proguard 规则文件：`proguard-rules.pro` 已存在（默认模板 + SQLCipher/Compose 保留规则见文件内容）。

---

## 5. 构建与回归证据基线（本轮实跑）

| 项 | 结果 | 证据 |
|----|------|------|
| `:core:test` | **71/71 PASS** | `%USERPROFILE%\pdig-build\core\test-results`（tests=71 failures=0 errors=0） |
| `:app:testDebugUnitTest` | **9/9 PASS** | `%USERPROFILE%\pdig-build\app\test-results\testDebugUnitTest`（tests=9） |
| `:conformance:run` | **91/91 PASS** | `pass=91 fail=0 notImplemented=0 total=91`（conformance/reports/android.json） |
| connectedDebugAndroidTest | **59/59 PASS** | API36 AVD `pdig_api36_phone`（16 / API36），`Finished 59 tests`，0 failed |
| assembleDebug / assembleRelease / bundleRelease | SUCCESSFUL | `android/` 下 `.\gradlew.bat` 实跑（见 `ANDROID_16_API36_CLOSURE_REPORT.md` §证据） |
| debug APK | `app-debug.apk` 37,123,563 B / SHA256 `73008F14…` | `C:\Users\Kaiser\pdig-build\app\outputs\apk\debug\` |
| release AAB | `app-release.aab` 20,861,455 B / SHA256 `C4E70F3D…` | `C:\Users\Kaiser\pdig-build\app\outputs\bundle\release\` |
| release APK | `app-release-unsigned.apk` 33,114,029 B / SHA256 `EEAF2864…` | 默认未签名（生产签名见 `ANDROID_PRODUCTION_SIGNING_ACCEPTANCE.md`，本轮 NON-PRODUCTION 验证） |

> 哈希在 RC 阶段会随签名方式产生 unsigned/non-prod-signed 两个版本；本表为 API36 基线构建哈希，
> 最终 RC 以 `ANDROID_PRODUCTION_RC_MANIFEST.md` 为准。

---

## 6. 平台状态声明（AGENTS §19）

| 项 | 状态 |
|----|------|
| API36 构建 | **TESTED**（assemble/bundle 三目标 + JVM 回归，API36 AVD） |
| API36 运行时 | **TESTED**（connectedDebugAndroidTest 59/59 + Core Journey E2E + 三场景 E2E，API36 AVD） |
| 真机 | **NOT_DEVICE_VERIFIED**（无真实设备，外部 blocker E-1） |
| Production Signing | **BLOCKED_BY_MISSING_PRODUCTION_KEYSTORE**（本轮 NON-PRODUCTION 验证，E-2） |
| Store 发布 | **BLOCKED_BY_STORE_ACCOUNT / APPLICATION_ID_OPEN**（E-3/E-5） |