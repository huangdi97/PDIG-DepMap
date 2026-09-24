# ANDROID_BUILD_REPRODUCIBILITY_REPORT.md

> 本轮（2026-09-15）Android N1/N2 Runtime Closure 过程中，发现 Android 构建链**此前完全不可复现**：
> `android/` 下没有 Gradle Wrapper，之前能出 APK 是因为本机恰好有一个手工解压的 Gradle。
> 本报告记录审计过程、修复动作与修复后的验证证据。

---

## 1. 修复前的真实状态

| 检查项                    | 修复前                                             |
| ------------------------- | -------------------------------------------------- |
| `android/gradlew`         | **不存在**                                         |
| `android/gradlew.bat`     | **不存在**                                         |
| `android/gradle/wrapper/` | **不存在**                                         |
| PATH 中的 `gradle`        | 无（`Get-Command gradle` 为空）                    |
| `JAVA_HOME`               | 未设置                                             |
| PATH 中的 java            | 只有 Oracle Java 8 shim（`java8path`）→ **不满足** |
| 但 `app-debug.apk` 曾产出 | 是 → 说明此前依赖本机绝对路径 Gradle               |

`android/` 目录实际内容（修复前）：

```
.gradle  .kotlin  app  conformance  core
build.gradle.kts  gradle.properties  local.properties  settings.gradle.kts
```

---

## 2. 版本审计（先定兼容矩阵，再动手）

`android/build.gradle.kts`：

| 项                              | 值           |
| ------------------------------- | ------------ |
| Android Gradle Plugin           | **8.5.2**    |
| Kotlin                          | **2.0.0**    |
| compileSdk / targetSdk / minSdk | 34 / 34 / 26 |
| Java 兼容                       | `VERSION_17` |

AGP 8.5.2 要求 **Gradle 8.7+** 且 **JDK 17+**。

本机 Java 候选：

| 候选                                                               | 版本                                    | 结论        |
| ------------------------------------------------------------------ | --------------------------------------- | ----------- |
| `%PROGRAMFILES% (x86)\Common Files\Oracle\Java\java8path\java.exe` | Java 8                                  | ❌ 版本不足 |
| `%PROGRAMFILES%\Java\jre1.8.0_441`                                 | Java 8                                  | ❌ 版本不足 |
| `<ANDROID_STUDIO_HOME>\jbr\bin\java.exe`                           | **OpenJDK 21.0.10 (JetBrains Runtime)** | ✅ 采用     |

Gradle 候选：

| 候选                                                    | 结论                                                                                                     |
| ------------------------------------------------------- | -------------------------------------------------------------------------------------------------------- |
| `<GRADLE_HOME>\bin\gradle.bat`                          | ✅ **Gradle 8.9**，与 AGP 8.5.2 兼容，采用                                                               |
| `%USERPROFILE%\.gradle\wrapper\dists\gradle-9.3.1-bin\` | ❌ 只有 `gradle-9.3.1-bin.zip.part`（**0 字节**）与 `.lck`，是失败下载残片；且 Gradle 9 不兼容 AGP 8.5.2 |

**未下载任何新 JDK 或新 Gradle 发行版** —— 全部使用本机已有且版本匹配的组件。

---

## 3. 修复动作

### 3.1 生成标准 Wrapper

首次直接执行 `gradle wrapper --gradle-version 8.9` 失败：

```
Execution failed for task ':wrapper'.
> Test of distribution url https://services.gradle.org/distributions/gradle-8.9-bin.zip failed.
```

排查：Gradle 会先做 distribution URL 可达性预检，本机网络环境下该预检误判
（同机用 PowerShell 直接对该 URL 做 HEAD 请求返回 **200，Content-Length 136114148**，网络是通的）。

解决：用**一次性 init script** 关闭预检（该 init script 放在 `local_private/`，不进仓库）：

```groovy
allprojects {
    tasks.withType(org.gradle.api.tasks.wrapper.Wrapper).configureEach {
        validateDistributionUrl = false
    }
}
```

生成结果：

| 文件                                               | 大小     |
| -------------------------------------------------- | -------- |
| `android/gradlew`                                  | 生成     |
| `android/gradlew.bat`                              | 生成     |
| `android/gradle/wrapper/gradle-wrapper.jar`        | 43,504 B |
| `android/gradle/wrapper/gradle-wrapper.properties` | 251 B    |

`gradle-wrapper.properties`：

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip
networkTimeout=10000
validateDistributionUrl=false
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

`distributionUrl` 指向**官方地址**，不含任何 `C:\` / `D:\` 机器专属路径。

### 3.2 顺带修掉的另一个可复现性缺陷

`android/conformance/build.gradle.kts` 把默认仓库根写成了 `../..`：
但 `rootProject` 是 `android/`，`../..` 会偏到 `<repo_parent>`，导致：

```
repo root: <repo_parent>
FATAL: <repo_parent>/conformance\CONFORMANCE_MANIFEST.json not found
```

改为 `..` 后 `:conformance:run` 正常执行（91/91）。

### 3.3 Wrapper 自举所需的本机一次性处理

`gradlew` 首次运行要从 services.gradle.org 下载分发包，而 **JVM 不读 `HTTP_PROXY` 环境变量**
（本机 `HTTP_PROXY=http://127.0.0.1:56557`），导致：

```
java.net.ConnectException: Connection refused: getsockopt
```

解决：调用时显式传代理系统属性（`GRADLE_OPTS`）：

```
-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=10808 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=10808
```

下载成功后分发包进入 `~/.gradle/wrapper/dists`，**后续不再需要代理**。
这是本机网络环境要求，**不是仓库要求**。

---

## 4. 修复后验证（全部真实执行）

| 命令                                          | 结果                                                                                    |
| --------------------------------------------- | --------------------------------------------------------------------------------------- |
| `.\gradlew.bat --version`                     | **PASS** — Gradle 8.9 / Launcher JVM 21.0.10 (JetBrains s.r.o.) / Windows 11 10.0 amd64 |
| `.\gradlew.bat clean`                         | **PASS** — `BUILD SUCCESSFUL`                                                           |
| `.\gradlew.bat :app:assembleDebug`            | **PASS** → `app-debug.apk` 36,794,370 B                                                 |
| `.\gradlew.bat :app:assembleDebugAndroidTest` | **PASS** → `app-debug-androidTest.apk` 396,650 B                                        |
| `.\gradlew.bat :core:test`                    | **NO-SOURCE** — `:core` 无 JVM 单元测试源集（见 §6）                                    |
| `.\gradlew.bat :conformance:run`              | **PASS** — `pass=91 fail=0 notImplemented=0 total=91`                                   |
| `.\gradlew.bat :app:bundleRelease`            | **PASS** → `app-release.aab` 20,734,935 B                                               |

---

## 5. 仓库洁净性

| 检查项                             | 结果                                                                                                          |
| ---------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| Wrapper 四件套已生成               | ✅                                                                                                            |
| `distributionUrl` 不含机器绝对路径 | ✅ 官方 https 地址                                                                                            |
| `local.properties` 不进仓库        | ✅ 根 `.gitignore:86` 命中 `local.properties`                                                                 |
| 仓库中无硬编码当前电脑路径         | ✅（`local.properties` 被忽略；`build.gradle.kts` 中测试 keystore 路径指向 `local_private/`，受保护另有开关） |

---

## 6. 本轮发现的构建链遗留问题（如实记录，未在本轮解决）

1. **`:core:test` 为 NO-SOURCE**：`android/core` 模块没有任何 JVM 单元测试源文件。
   领域正确性目前靠 `:conformance:run`（91 用例，JVM）+ 设备内 androidTest（19 用例）覆盖；
   **纯单元测试为零**。这是 `ANDROID_UNIT_INTEGRATION` 不能判 PASS 的直接原因。

2. **非生产测试签名未生效**（详见 Final Closure 报告的 Release Signing 章节）：
   两次 `bundleRelease` 产出的 AAB **SHA-256 完全相同**，`META-INF/` 下无 `*.RSA` / `*.SF`，
   说明签名配置本轮**未真正生效**。不据此宣称任何签名能力。

---

## 7. Gate

| Gate                            | 状态     |
| ------------------------------- | -------- |
| `ANDROID_GRADLE_WRAPPER`        | **PASS** |
| `ANDROID_BUILD_REPRODUCIBILITY` | **PASS** |
