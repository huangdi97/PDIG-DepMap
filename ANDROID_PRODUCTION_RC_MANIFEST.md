# ANDROID_PRODUCTION_RC_MANIFEST.md

> 生成时间：2026-09-23（ANDROID_2026_PRODUCTION_REALITY_CLOSURE · API36 工程全速收口）
> 依据：粘贴 Goal §21/§22/§67 + 批准契约 E2。
> ⚠ **诚实声明**：本清单对应的 AAB/APK 由 **NON-PRODUCTION test key** 签名
> （`CN=PDIG NON-PRODUCTION TEST KEY`）。**不是**可上传 Play 的最终生产产物：
> 正式 Upload Key 定案后必须重新签名并更新本清单（生产 keystore 为外部 blocker E-2）。
> 本清单同时保留未签名基线构建哈希，供对比。

---

## 1. 产物身份

| 项                              | 值                                                                               |
| ------------------------------- | -------------------------------------------------------------------------------- |
| Git SHA (HEAD)                  | `b13f2f72f89cf3ee384ac9c7c149f7bff724fbab`                                       |
| Git tag                         | 无（工作分支 `feat/android-production-release`）                                 |
| versionName                     | `0.1.0-milestone`（占位，R-3 OPEN）                                              |
| versionCode                     | `1`（占位，R-3 OPEN）                                                            |
| applicationId                   | `com.pdig.app`（占位，R-1 OPEN）                                                 |
| compileSdk / targetSdk / minSdk | 36 / 36 / 26                                                                     |
| AGP / Gradle / JDK / Kotlin     | 8.5.2 / 8.9 wrapper / OpenJDK 21.0.10 (Android Studio JBR) / 2.0.0               |
| 构建环境                        | Windows 11 10.0.26200（`E:\AI\号卡管理`，输出重定向 `%USERPROFILE%\pdig-build`） |

## 2. 产物清单（2026-09-23 实跑构建）

| 产物                               | 路径                                                                                     | bytes      | SHA256                                                             | 签名                                           |
| ---------------------------------- | ---------------------------------------------------------------------------------------- | ---------- | ------------------------------------------------------------------ | ---------------------------------------------- |
| **release AAB（NON-PROD signed）** | `C:\Users\Kaiser\pdig-build\app\outputs\bundle\release\app-release.aab`                  | 20,894,496 | `D1AD7635FEBDAAE9A6CC3292131691979B987B302281F1C72617E396EAEEEC9`  | `pdig-nonprod.jks`（JKS，v? jarsigner 已验证） |
| **release APK（NON-PROD signed）** | `C:\Users\Kaiser\pdig-build\app\outputs\apk\release\app-release.apk`                     | 33,122,221 | `7FD81247120E858C4A2CA16641A97928013D20B12FB837BD36E8217600A8BE55` | apksigner v2 ✅                                |
| release AAB（基线未签名）          | `C:\Users\Kaiser\pdig-build\app\outputs\bundle\release\app-release.aab`（覆盖前基线）    | 20,861,455 | `C4E70F3D4885456DAAE856EA0BACA30A661A07DD552C4DA65D7EC7B5C6F27465` | 无（未签名基线）                               |
| debug APK                          | `C:\Users\Kaiser\pdig-build\app\outputs\apk\debug\app-debug.apk`                         | 37,123,563 | `73008F14B5C9272E2EED2B9FE3044F6237785525B9D6E19F521AFE60A9C03835` | debug 签名                                     |
| androidTest APK                    | `C:\Users\Kaiser\pdig-build\app\outputs\apk\androidTest\debug\app-debug-androidTest.apk` | 1,137,154  | `69A35474ED801610CB09C20F67276E1DD84D945AF9864B7F4816706246957680` | debug 签名                                     |

> 注：基线未签名 AAB 哈希来自 b13f2f7 构建（`ANDROID_PLATFORM_BASELINE.md` §5）。
> 本轮 NON-PROD signed 构建覆盖同名文件，产出新的 signed AAB/APK 如上。

## 3. 签名证据

### 3.1 APK（apksigner verify --verbose --print-certs）

```
Verifies
Verified using v2 scheme (APK Signature Scheme v2): true
Number of signers: 1
V2 Signer: certificate DN: CN=PDIG NON-PRODUCTION TEST KEY, OU=Local Build Verification, O=PDIG, L=Local, ST=Local, C=CN
V2 Signer: certificate SHA-256 digest: 953dc774fcefcdf11423594748aecfa894c8754dc232d6047c39e25373b13ad4
V2 Signer: key algorithm: RSA / key size (bits): 2048
```

### 3.2 AAB（jarsigner -verify，自签名密钥的预期告警可忽略）

```
jar 已验证（exit=0）；条目 X.509 证书逐项列出（CN=PDIG NON-PRODUCTION TEST KEY）
警告（预期内，不影响验证结果）：
  - PKIX path building failed（自签名 non-prod 证书，无 CA 链）
  - AAB 为 JAR-签名结构，JarInputStream 读取告警为 AAB 正常形态
```

### 3.3 证书指纹（NON-PRODUCTION only）

- SHA-256: `953dc774fcefcdf11423594748aecfa894c8754dc232d6047c39e25373b13ad4`
- SHA-1: `d8d281c2bc46e95fe64d1f8622732860542684b2`

> ⚠ 以上为 **NON-PRODUCTION TEST KEY**。生产证书指纹必须来自用户提供的 Upload Key，
> 提交 Play 时以 Play Console 显示的 App Signing Key / Upload Key 指纹为准（粘贴 Goal §22/§47）。

## 4. 测试汇总（本轮实跑，对应产物）

| 套件                      | 结果                                    | 时间       |
| ------------------------- | --------------------------------------- | ---------- |
| `:core:test`              | 71/71                                   | 2026-09-23 |
| `:app:testDebugUnitTest`  | 9/9                                     | 2026-09-23 |
| `:conformance:run`        | 91/91                                   | 2026-09-23 |
| connectedDebugAndroidTest | 59/59（API36 AVD）                      | 2026-09-23 |
| Core Journey E2E v4       | 见 `ANDROID_16_API36_CLOSURE_REPORT.md` | 2026-09-23 |
| Three-scenario E2E        | 见 `ANDROID_16_API36_CLOSURE_REPORT.md` | 2026-09-23 |

## 5. 达到 Play 可上传状态尚缺（依 BLOCKERS.md）

1. R-1 applicationId 定案（上架后不可改）
2. R-3 versionName/versionCode 按 `ANDROID_VERSIONING_POLICY.md` 定案
3. K-1..K-4 生产 Upload Key（keystore + passwords，用户提供）
4. Play 开发者账号（E-5）→ 创建 App → Play App Signing
5. 重新签名 → 重新生成本清单（生产证书指纹 + Play 复核）
