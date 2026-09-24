# DEPENDENCY_AUDIT.md — 依赖审计（2026-09-23 · Round 0.1.0）

> 审计轮次：2026-09-23（四联审计：代码规模 / 依赖 / 代码质量 / 测试质量）。
> 来源：`android/{app,core,conformance,repos}/build.gradle.kts`、`desktop/{build,app/build}.gradle.kts`、`core/package.json`；
> License 引用 `THIRD_PARTY_NOTICES.md`（2026-09-13 刷新）与 `ANDROID_DEPENDENCY_LICENSE_REPORT.md`（2026-09-20，解析版本取自 `:app:dependencies --configuration debugRuntimeClasspath`）。
> 本轮未运行 gradle（任务约束），解析版本引用上一份 license report 的记录；依赖策略遵循 spec §238：优先平台 SDK、避免大型第三方框架、无网络库、无 analytics/crash/广告 SDK。
> 未做在线 CVE 扫描 → 记为 `CVE_SCAN = NOT_RUN_ONLINE`（不把「未扫描」写成「无漏洞」）。

---

## 1. android/core（纯 Kotlin JVM 领域层）

| 组件                            | 版本   | 用途                                    | License                                                   | 安全相关性                                                                                       | 移除?              |
| ------------------------------- | ------ | --------------------------------------- | --------------------------------------------------------- | ------------------------------------------------------------------------------------------------ | ------------------ |
| org.bouncycastle:bcprov-jdk18on | 1.78.1 | **Argon2id**（DEPMAP_CONTAINER_V1 KDF） | Bouncy Castle License（MIT 风格，见 THIRD_PARTY_NOTICES） | **安全关键**：KDF 为冻结协议 `argon2id v19`，不可降级为 PBKDF2/HKDF；模块内唯一非 JDK 运行时依赖 | **否**（协议强制） |

> core 的 JSON / JCS / 时间解析 / Base64 全部为自带实现 + JDK 标准库（避免第三方解析器影响 conformance 逐字段可比性）。
> 测试依赖：org.junit.jupiter:junit-jupiter 5.10.2（EPL-2.0，仅测试）、org.xerial:sqlite-jdbc 3.46.1.0（Apache-2.0，仅测试，与 :conformance 同版本）、kotlin("test")。

## 2. android/app（Android 运行时）

| 组件                                                                                        | 版本              | 用途                                           | License                                   | 安全相关性                                                                                                                                    | 移除?                                 |
| ------------------------------------------------------------------------------------------- | ----------------- | ---------------------------------------------- | ----------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------- |
| androidx.compose:compose-bom                                                                | 2024.09.02        | Compose 版本管理                               | Apache-2.0                                | —                                                                                                                                             | 否                                    |
| androidx.compose.ui:ui / ui-graphics / ui-tooling-preview / material3 / material-icons-core | 1.7.x（BOM）      | UI                                             | Apache-2.0                                | —                                                                                                                                             | 否                                    |
| androidx.activity:activity-compose                                                          | 1.9.1             | Activity + Compose                             | Apache-2.0                                | —                                                                                                                                             | 否                                    |
| androidx.navigation:navigation-compose                                                      | 2.8.1             | 导航                                           | Apache-2.0                                | —                                                                                                                                             | 否                                    |
| androidx.lifecycle:lifecycle-viewmodel-compose / runtime-compose                            | 2.8.5             | ViewModel/生命周期                             | Apache-2.0                                | —                                                                                                                                             | 否                                    |
| androidx.biometric:biometric                                                                | 1.1.0             | App Lock 生物识别                              | Apache-2.0                                | 绑定 FragmentActivity 基类                                                                                                                    | 否                                    |
| androidx.fragment:fragment                                                                  | **1.7.1**         | FragmentActivity（BiometricPrompt 载体）       | Apache-2.0                                | **显式钉住**：否则传递 1.2.5（2020）与 activity 1.9.x 混用 → 真机 `IllegalArgumentException: Can only use lower 16 bits for requestCode` 崩溃 | 否                                    |
| androidx.security:security-crypto                                                           | 1.1.0-**alpha06** | 加密原语                                       | Apache-2.0                                | **alpha 风险已登记**（引用即风险）；当前仅用于受限用途                                                                                        | 否（观察项：正式版 1.1.0 发布后升级） |
| androidx.sqlite:sqlite-framework                                                            | 2.4.0             | SQLite 框架层                                  | Apache-2.0                                | —                                                                                                                                             | 否                                    |
| net.zetetic:sqlcipher-android                                                               | 4.5.5             | **SQLCipher 密文库**（LOCAL 库加密，spec §62） | BSD-3-Clause（Zetetic；见报告 §1.1 注释） | **安全关键**：数据库整库加密载体                                                                                                              | 否                                    |
| androidx.core:core-ktx                                                                      | 1.13.1            | AndroidX 核心                                  | Apache-2.0                                | —                                                                                                                                             | 否                                    |
| org.jetbrains.kotlinx:kotlinx-coroutines-android                                            | 1.8.1             | 协程                                           | Apache-2.0                                | —                                                                                                                                             | 否                                    |
| org.jetbrains.kotlin:kotlin-stdlib                                                          | 2.0.0             | 标准库                                         | Apache-2.0                                | —                                                                                                                                             | 否                                    |

> 测试类：junit:junit 4.13.2（EPL-1.0）、androidx.test runner 1.6.2 / rules 1.6.1 / ext-junit 1.2.1、compose ui-test-junit4 1.7.2、ui-test-manifest（debug）— 均仅测试源集，不进生产包。
> 签名：非生产测试 keystore 不在仓库（`local_private/build-chain`，仅 `-PpdigNonProdSigning=true` 且本地存在时才生效）；口令一律环境变量/`-P` 注入，仓库零字面量。

## 3. android/conformance + desktop（JVM 工具/桌面）

| 组件                                                        | 版本               | 使用模块                                  | 用途                                                                    | License    | 安全相关性                       | 移除?                 |
| ----------------------------------------------------------- | ------------------ | ----------------------------------------- | ----------------------------------------------------------------------- | ---------- | -------------------------------- | --------------------- |
| org.xerial:sqlite-jdbc                                      | 3.46.1.0           | :conformance；:core test                  | JVM 真实 SQLite 驱动（migration/container 验证）                        | Apache-2.0 | —；仅 harness 与测试             | 否                    |
| kotlinx-coroutines-core                                     | 1.8.1              | :repos（SourceRepository Dispatchers.IO） | 协程核心                                                                | Apache-2.0 | —                                | 否                    |
| net.java.dev.jna:jna / jna-platform                         | **5.14.0**         | desktop                                   | **Windows DPAPI**（DesktopSecurityPort/DeviceUnlockStore 系统凭据绑定） | Apache-2.0 | Windows 系统级密钥绑定；平台边界 | 否                    |
| org.bouncycastle:bcprov-jdk18on                             | 1.78.1             | desktop                                   | 与 :core 同版本，显式声明供 runtime classpath                           | BC License | 同 §1                            | 否                    |
| org.jetbrains.kotlin.plugin.compose / org.jetbrains.compose | 2.0.0 / **1.6.11** | desktop                                   | Compose Desktop 插件                                                    | Apache-2.0 | —                                | 否（本轮新增，见 §6） |

## 4. repos 模块

| 组件                                          | 版本  | 用途                                                                    | License    | 移除? |
| --------------------------------------------- | ----- | ----------------------------------------------------------------------- | ---------- | ----- |
| org.jetbrains.kotlinx:kotlinx-coroutines-core | 1.8.1 | SourceRepository parseFile/previewImport/commitImport 用 Dispatchers.IO | Apache-2.0 | 否    |

## 5. legacy core/（typescript oracle，dev-only，不随发布产物分发）

| 依赖                                 | 版本    | 用途                                    | License    | 说明                                       |
| ------------------------------------ | ------- | --------------------------------------- | ---------- | ------------------------------------------ |
| hash-wasm                            | ^4.12.0 | JS 侧 argon2id/hash 实现（oracle 对拍） | MIT        | 生产 runtime 唯一依赖（core-TS 仅 oracle） |
| typescript                           | ^5.8.0  | 类型检查                                | Apache-2.0 | dev                                        |
| vitest + @vitest/coverage-v8         | ^3.x    | 测试/覆盖率                             | MIT        | dev                                        |
| eslint + typescript-eslint + globals | ^10/8   | lint                                    | MIT        | dev                                        |
| prettier                             | ^3.9.6  | 格式                                    | MIT        | dev                                        |
| @types/node                          | ^22     | 类型                                    | MIT        | dev                                        |
| fast-check                           | ^4.10.0 | property-based 测试                     | MIT        | dev                                        |
| iconv-lite                           | ^0.7.3  | fixture 生成脚本编码                    | MIT        | dev                                        |

（License 引用 THIRD_PARTY_NOTICES.md §开发工具链；`check:deps` 自动核对与 package-lock 一致性。）

## 6. 本轮新增依赖与理由

| 新增                                           | 版本   | 模块    | 理由                                                                                                                                                             |
| ---------------------------------------------- | ------ | ------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| org.jetbrains.compose（Compose Multiplatform） | 1.6.11 | desktop | Desktop 收尾需要与 Android 相同 Kotlin 2.0.0 的 Compose 栈；1.6.11 为与 Kotlin 2.0.0 匹配的稳定线（desktop/build.gradle.kts 注释）；JDK 21 / Gradle 8.9 验证通过 |
| net.java.dev.jna / jna-platform                | 5.14.0 | desktop | DesktopSecurityPort 走 Windows **DPAPI**（CryptProtectData）绑定系统用户凭据，JNA 是官方风格的平台绑定方式；替代方案（JNI/named pipes）复杂度更高且无收益        |

> 为什么可以接受：desktop 复用 android/:core（不含新增领域语义），新增依赖**只出现在 desktop 与 harness**，Android 发布包 SBOM 不变。

## 7. 禁止项核查

- [x] 无网络请求库（okhttp/Retrofit/Volley）→ 产品无业务网络。
- [x] 无 Firebase / Analytics / Crashlytics / telemetry / 广告 SDK。
- [x] 无 DCloud / uni-app / UTS 运行时依赖（legacy 参考目录已隔离）。
- [x] 无 LLM/AI SDK、无 vector/embedding 库。
- [x] 密钥/证书不进入仓库（keystore 在 `local_private/`，`check:secrets` / secret gate 为 0）。

## 8. 移除结论

**本轮无可移除项**：全部直接依赖均有真实调用点（bcprov→Argon2id；sqlcipher→密文库；jna→DPAPI；junit/androidx.test→测试源集）。三个观察项：`security-crypto 1.1.0-alpha06`（alpha 线，正式版发布后评估）、`ui-tooling-preview`（仅 debug）、`material-icons-core`（低风险）。

---

**结论**：Android 运行时依赖以 AndroidX/Compose 平台栈为主，唯一非平台第三方运行时依赖为 BouncyCastle（Argon2id，协议强制）与 SQLCipher（加密 DB）；Desktop 新增 Compose 1.6.11 + JNA 5.14.0（DPAPI），不影响 Android SBOM；legacy core-TS 依赖全为 dev-only oracle 工具链；本轮无移除项，CVE 扫描如实记为 NOT_RUN_ONLINE。

## v0.1.2 质量迭代收口轮复核（2026-09-24）

- **本轮依赖面**：无新增/移除运行时依赖；Windows 打包产物生成 SBOM
  `PDIG-0.1.2-SBOM.cyclonedx.json`（**CycloneDX 1.5，151 components**，由 jpackage-libs +
  Gradle previewReleaseRuntimeClasspath 依赖树生成）+ `THIRD-PARTY-NOTICES.md` + `SHA256SUMS.txt`；
  SBOM sha256 `fc5448782262e007a20f050c56043af6b5bfc197d5c2891774e341d7376eada9`、NOTICES
  `7275ec5b40aa5bd659e9938d8fc4491afbcc9ff095d1fa138f25b0e5deaee944`（见 `PRODUCT_V0_1_2_RELEASE_MANIFEST.md`）。
- **质量 Gate**：`check-quality.mjs` → **VERDICT PASS exit 0**（2026-09-24T06:56Z），`DEPENDENCY_CYCLE=0`；
  core `npm run check` network gate 0 primitives（130 文件）。
- **JUSTIFIED**：EXCEPTIONS.json 复核为合法 UTF-8 JSON、reason 无乱码（fileSizeOver300×2 migration/schema、
  kotlinEscapes×2 lateinit 均 JUSTIFIED、secretPattern×2 fixture 合成口令）。
- **BLOCKED（外部，未变化）**：CI billing E-10（GitHub 计费，CI_EXTERNAL_BLOCKED）继续阻塞远端 CI；
  在线 CVE 扫描本轮**未重跑**，保持 `CVE_SCAN = NOT_RUN_ONLINE`（不把「未扫描」写成「无漏洞」）。
- **结论**：依赖策略与 SBOM 一致；本轮发布物为 Windows 打包，Android SBOM 不受影响；观察项
  （security-crypto alpha / ui-tooling-preview / material-icons-core）状态不变。
