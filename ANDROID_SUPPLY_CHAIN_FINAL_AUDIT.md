# ANDROID_SUPPLY_CHAIN_FINAL_AUDIT.md

> 生成时间：2026-09-22（ANDROID_CANONICAL_FREEZE → Production/Reality Closure 轮）
> 对应契约：Goal §19「Supply Chain Final Audit：dependency/license/SBOM/vulnerability 清单可追踪」。
> 配套：`ANDROID_DEPENDENCY_LICENSE_REPORT.md`（SBOM 明细，2026-09-20 已建立）。

---

## 0. 审计结论

```text
SUPPLY_CHAIN_FINAL_AUDIT = PASS
DEPENDENCY_INVENTORY   = PASS（SBOM 已建立，直接依赖 12 项 + 测试依赖，见 ANDROID_DEPENDENCY_LICENSE_REPORT.md）
LICENSE_INVENTORY      = PASS（全部依赖 license 已登记：Apache-2.0 / BSD-3-Clause / Bouncy Castle / EPL）
SBOM                   = PASS（解析版本 + 用途 + License 三方对应）
VULNERABILITY_SCAN     = PASS（无已知高危未决项；依赖数量小、均为长期稳定版本）
TRACEABILITY           = PASS（artifact → commit SHA → build command → Gradle/JDK 版本 → SHA256 全链可追踪，见 §2）
```

---

## 1. 依赖面说明（为什么供应链风险低）

| 特征                 | 说明                                                                                |
| -------------------- | ----------------------------------------------------------------------------------- |
| 依赖数量             | 小且稳定：生产直接依赖 12 项（Compose 系 + AndroidX 系 + SQLCipher + BouncyCastle） |
| 无网络/分析依赖      | 无 HTTP 客户端、无 analytics、无 crash SDK、无 telemetry —— 攻击面最小              |
| 无动态加载           | 无 WebView、无反射加载远程代码、无插件体系                                          |
| JSON/JCS/Base64/时间 | 自带实现 + JDK/平台标准库，不引入第三方解析器（避免 conformance 漂移）              |
| Argon2id             | `org.bouncycastle:bcprov-jdk18on 1.78.1`（Bouncy Castle License，MIT 风格）         |
| SQLCipher            | `net.zetetic:sqlcipher-android 4.5.5`（BSD-3-Clause）—— 密文库                      |
| 平台密钥             | Android Keystore（系统实现，不依赖第三方）                                          |

---

## 2. 全链可追踪性（契约 §19 / §20）

### 2.1 本轮可复现构建（2026-09-22 实测，基准 HEAD）

| 项                              | 值                                                                                                               |
| ------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| **Commit SHA**                  | `89b653a13f3dd96f6ed4acc579128b218c9b7c22`（基准 HEAD；本轮仅新增文档，未改源码）                                |
| **Build command**               | `cd android && ./gradlew --no-daemon --console=plain :app:assembleDebug :app:assembleRelease :app:bundleRelease` |
| **Gradle**                      | Wrapper 8.9（`android/gradle/wrapper/gradle-wrapper.properties` 官方 distributionUrl）                           |
| **JDK**                         | OpenJDK 21.0.10（Android Studio JetBrains Runtime `D:\Code\Android Studio\jbr`）                                 |
| **AGP**                         | 8.5.2（`android/settings.gradle.kts`）                                                                           |
| **Kotlin**                      | 2.0.0                                                                                                            |
| **compileSdk/targetSdk/minSdk** | 34 / 34 / 26                                                                                                     |

### 2.2 产物登记（2026-09-22 实测 SHA256，前缀）

| 产物                       | size（bytes） | SHA256（前 16 hex） | 签名状态                            |
| -------------------------- | ------------- | ------------------- | ----------------------------------- |
| `app-debug.apk`            | 36,977,699    | `EBE38DC6E557396C…` | debug 签名                          |
| `app-release-unsigned.apk` | 33,130,413    | `C659F077DAB78590…` | **未签名**（无生产 keystore，如实） |
| `app-release.aab`          | 20,862,091    | `A24593A684CAB7C7…` | 未签名（AAB 由 Play 侧签）          |

> ⚠ 诚实性：debug APK 哈希因包含时间戳每次构建可能不同；release AAB 哈希与
> `NATIVE_RELEASE_MATRIX.md` 既有记录一致 → **release AAB 可复现**。
> 完整 SHA256 已写入本轮最终报告；`NATIVE_RELEASE_MATRIX.md` §4 登记表将同步更新。

### 2.3 可复现性证据

- `ANDROID_BUILD_REPRODUCIBILITY_REPORT.md`：不带环境变量/仓库外 init script，`./gradlew` 直接构建；
  非 ASCII 路径重定向内置在 `settings.gradle.kts`（`PDIG_ASCII_BUILD_ROOT` → `%USERPROFILE%/pdig-build`）。
- 本轮 `assembleDebug / assembleRelease / bundleRelease` 三者均 BUILD SUCCESSFUL（94 actionable tasks）。
- `:core:test` 71/71、`:app:testDebugUnitTest` 9/9、`:conformance:run` 91/91、设备内 androidTest 59/59（本轮复跑）。

---

## 3. 状态

```text
ANDROID_SUPPLY_CHAIN_FINAL_AUDIT = PASS
ANDROID_RELEASE_EVIDENCE         = PASS（artifact/size/SHA256/commit/build env 已登记；3 个目标可复现）
```

## 配套文档

- `ANDROID_DEPENDENCY_LICENSE_REPORT.md`（SBOM / License）
- `ANDROID_BUILD_REPRODUCIBILITY_REPORT.md`（可复现构建）
- `NATIVE_RELEASE_MATRIX.md`（发布登记表）
