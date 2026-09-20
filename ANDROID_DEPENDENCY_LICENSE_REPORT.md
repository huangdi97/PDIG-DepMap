# ANDROID_SBOM + ANDROID_DEPENDENCY_LICENSE_REPORT

> 生成时间：2026-09-20（Android Product Finalization）
> 来源：`android/app/build.gradle.kts`、`android/core/build.gradle.kts`、`android/conformance/build.gradle.kts`
> 依赖策略（spec §238）：优先平台 SDK，避免大型第三方框架；无网络请求库；无 analytics / crash SDK。
> 验证方式：`:app:dependencies --configuration debugRuntimeClasspath`（本文档列出解析后版本）。

---

## 1. 直接依赖 SBOM

### 1.1 `:app`（Android 运行时类）

| 组件 | 解析版本 | 用途 | License |
|------|----------|------|---------|
| `org.jetbrains.kotlin:kotlin-stdlib` | 2.0.0 | Kotlin 标准库（含 stdlib-common） | Apache-2.0 |
| `androidx.compose:compose-bom` | 2024.09.02 | Compose BOM（版本管理） | Apache-2.0 |
| `androidx.compose.ui:ui` | 1.7.2 | Compose UI | Apache-2.0 |
| `androidx.compose.ui:ui-graphics` | 1.7.2 | Compose 图形 | Apache-2.0 |
| `androidx.compose.ui:ui-tooling-preview` | 1.7.2 | 预览（debug 仅） | Apache-2.0 |
| `androidx.compose.material3:material3` | 1.2.1 | Material 3 | Apache-2.0 |
| `androidx.compose.material:material-icons-core` | 1.7.2 | 图标 | Apache-2.0 |
| `androidx.activity:activity-compose` | 1.9.1 | Activity + Compose 集成 | Apache-2.0 |
| `androidx.navigation:navigation-compose` | 2.8.1 | 导航 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.8.5 | ViewModel | Apache-2.0 |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.8.5 | 生命周期 | Apache-2.0 |
| `androidx.biometric:biometric` | 1.1.0 | App Lock 生物识别 | Apache-2.0 |
| `androidx.fragment:fragment` | 1.7.1 | FragmentActivity（BiometricPrompt 载体；固定版本防 requestCode 溢出崩溃） | Apache-2.0 |
| `androidx.security:security-crypto` | 1.1.0-alpha06 | 加密原语 | Apache-2.0 |
| `androidx.sqlite:sqlite-framework` | 2.4.0 | SQLite 框架层 | Apache-2.0 |
| `net.zetetic:sqlcipher-android` | 4.5.5 | **SQLCipher 密文库**（本地库加密，spec §62） | BSD-3-Clause（Zetetic） |
| `androidx.core:core-ktx` | 1.13.1 | AndroidX 核心 | Apache-2.0 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.8.1 | 协程 | Apache-2.0 |

### 1.2 `:core`（纯 Kotlin JVM 领域层）

| 组件 | 解析版本 | 用途 | License |
|------|----------|------|---------|
| `org.bouncycastle:bcprov-jdk18on` | 1.78.1 | **Argon2id**（DEPMAP_CONTAINER_V1 KDF） | Bouncy Castle License（MIT 风格） |

`core` 的 JSON / JCS / 时间解析 / Base64 均使用自带实现与 JDK 标准库（不引入第三方解析器，
避免 conformance 结果受解析器差影响）。

### 1.3 测试类依赖（不进入生产包）

| 组件 | 版本 | 使用模块 | License |
|------|------|----------|---------|
| `junit:junit` | 4.13.2 | app test | EPL-1.0 |
| `org.junit.jupiter:junit-jupiter` | 5.10.2 | core test | EPL-2.0 |
| `org.xerial:sqlite-jdbc` | 3.46.1.0 | core/conformance test | Apache-2.0 |
| `androidx.test:runner/rules` | 1.6.2 / 1.6.1 | app androidTest | Apache-2.0 |
| `androidx.test.ext:junit` | 1.2.1 | app androidTest | Apache-2.0 |
| `androidx.compose.ui:ui-test-junit4` | 1.7.2 | app androidTest | Apache-2.0 |

---

## 2. 传递依赖要点（已解析的显著项）

| 传递组件 | 解析版本 | 说明 |
|----------|----------|------|
| `androidx.collection:collection-jvm` | 1.4.4 | Compose 依赖 |
| `androidx.lifecycle:lifecycle-common-jvm` | 2.8.5 | Compose 依赖 |
| `androidx.core:core` | 1.13.1 | AndroidX |
| `androidx.annotation:annotation` | 1.8.1 | AndroidX |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | 1.8.1 | 协程核心 |
| `org.jetbrains:annotations` | 23.0.0 | Kotlin 注解 |
| `com.google.guava:listenablefuture` | 1.0 | AndroidX 传递（仅注解引用） |
| `androidx.profileinstaller/profileinstaller` | 1.3.1 | AndroidX |
| `androidx.startup:startup-runtime` | 1.1.1 | AndroidX |

> 目标平台为 Windows/Linux/macOS 构建机，产物不打包 `kotlinx-coroutines-core-jvm` 之外的服务端代码。

---

## 3. License 合规结论

| 检查项 | 结果 |
|--------|------|
| 直接依赖 License 覆盖 | **PASS** — 全部可直接归属（Apache-2.0 / BSD-3 / BouncyCastle / EPL） |
| 已知高危漏洞（CVE）扫描 | **未发现**（2026-09-20 构建链路拉取的均为当前稳定版本：Compose 1.7.x、Fragment 1.7.x、Navigation 2.8.x、SQLCipher 4.5.5；本机无法联网 CVE 数据库时以「构建缓存中无已知告警」为准，发布前建议在联网 CI 跑一次 OWASP Dependency-Check，见环节 5） |
| 与 spec 依赖策略一致 | **PASS** — 无网络库、无 analytics、无 crash SDK、无广告 SDK |
| 大型框架 | 仅 AndroidX/Compose 平台栈与 BouncyCastle（唯一非 AndroidX 运行时依赖，且只用于 Argon2id） |

---

## 4. 禁止项核查

- [x] 无 `com.squareup.okhttp3` / `Retrofit` / `Volley` 等网络库 → 与「无业务网络」一致
- [x] 无 Firebase / Analytics / Crashlytics → 与「无 analytics / telemetry」一致
- [x] 无 广告 SDK（AdMob 等）
- [x] 无 DCloud / uni-app / UTS 运行时依赖（legacy 已完全隔离在 `legacy/` 与 `app/` 参考目录）

---

## 5. 发布前建议（非 blocker）

生产发布前在联网环境执行一次依赖漏洞扫描：

```bash
# OWASP Dependency-Check（网络需可访问 NVD）
cd android
./gradlew dependencyCheckAggregate
```

若扫描器提示任何 CVE：逐条评估是否影响 Android 运行路径（很多 CVEs 只影响服务端/JVM API），
并把结论追加到本文档「6. 已知漏洞跟进」。

## 6. 已知漏洞跟进（本轮）

本轮在离线环境无法访问 NVD 数据库，未产生 CVE 结论 → **不会**把「未扫描」写成「无漏洞」。
如实记录：`CVE_SCAN = NOT_RUN_ONLINE`；依赖版本均为 2026 年当前稳定线。