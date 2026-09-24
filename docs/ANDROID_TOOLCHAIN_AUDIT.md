# docs/ANDROID_TOOLCHAIN_AUDIT.md — Android 工具链实测审计

> 审计日期：2026-09-14 / 2026-09-15（会话内实测）
> 方法：逐项执行命令取真实输出，不引用旧报告结论。旧报告 B1 的「无 JDK17 / 无 Android SDK / 无 Gradle」前提**本轮被实测推翻**。
> 主机：Windows（win32），`<repo>`（工程路径含非 ASCII 字符，见 §6）

---

## 1. Java / JDK

| 项                      | 实测值                                                      | 可用性                  |
| ----------------------- | ----------------------------------------------------------- | ----------------------- |
| PATH `java`             | `1.8.0_441`（Java 8）                                       | ❌ 不满足 AGP 8.5.2     |
| PATH `javac`            | 不存在                                                      | ❌                      |
| Android Studio 内置 JBR | `openjdk 21.0.10 2026-01-20`（`<ANDROID_STUDIO_HOME>\jbr`） | ✅ **本轮构建实际使用** |
| DevEco Studio 内置 JBR  | `openjdk 17.0.12 2024-07-16`（`<DEVECO_HOME>\jbr`）         | ✅ 备选                 |
| `JAVA_HOME`             | **未设置**                                                  | 需显式导出              |

**结论**：AGP 8.5.2 + Kotlin 2.0.0 要求 JDK 17+。本机 JDK 21（Android Studio JBR）实测可完成构建（§7 记录 `gradle -v` 与真实构建均成功）。PATH 上的 Java 8 **不可用**。

---

## 2. Android SDK

| 项                  | 实测值                                                              |
| ------------------- | ------------------------------------------------------------------- |
| SDK 根              | `<ANDROID_SDK_ROOT>`                                                |
| `ANDROID_HOME`      | **未设置**（需显式导出）                                            |
| `ANDROID_SDK_ROOT`  | **未设置**                                                          |
| platforms（审计初） | `android-36.1`、`android-37.0`（**无 34**，工程 `compileSdk = 34`） |
| platforms（本轮补） | ✅ `android-34` 已安装                                              |
| build-tools         | `36.1.0`、`37.0.0` → ✅ 本轮补装 `34.0.0`                           |
| cmdline-tools       | `latest`（v20.0，可执行）                                           |
| platform-tools      | `37.0.0` → `adb` 1.0.41                                             |
| licenses            | 7 项 license 文件在盘，全部已接受                                   |
| emulator            | `36.5.11` 已安装                                                    |
| system-images       | **不存在**                                                          |
| AVD                 | **0 个**                                                            |
| `adb devices -l`    | **空**（无物理设备、无模拟器）                                      |

### 2.1 `PATH` 上的旧 `adb`

`which adb` → `<ANDROID_SDK_ROOT>\adb`（**1.0.32，2016 年文件**）。
与 SDK `platform-tools`（1.0.41）**版本冲突**：调用 PATH 版会杀掉 SDK adb server（`adb server version (32) doesn't match this client (41); killing...`）。
**处置**：所有 adb 操作必须使用 `<ANDROID_SDK_ROOT>\platform-tools\adb.exe`。

### 2.2 本轮安装动作（真实执行）

```
sdkmanager --install "platforms;android-34" "build-tools;34.0.0"
→ Installed packages 复核：platforms;android-34 / build-tools;34.0.0 均落盘
```

---

## 3. Gradle

| 项               | 实测值                                                                                             |
| ---------------- | -------------------------------------------------------------------------------------------------- |
| PATH `gradle`    | **不存在**                                                                                         |
| 旧 wrapper dists | `~/.gradle/wrapper/dists/gradle-9.3.1-bin/<hash>/` 仅 `0` 字节 `.lck` / `.part`（**损坏/未完成**） |
| 工程内 wrapper   | **无**（`platforms/android/` 无 `gradlew` / `gradle-wrapper.properties`）                          |
| 本轮安装         | `<GRADLE_HOME>`，`gradle -v` → **Gradle 8.9**（Kotlin 1.9.23 / Groovy 3.0.21 / JVM 21.0.10）       |

### 3.1 Gradle 发行版获取路径（旧报告称「CDN 不可达」——本轮实测推翻）

| 来源                                     | 结果                                                        |
| ---------------------------------------- | ----------------------------------------------------------- |
| `services.gradle.org/distributions/...`  | 307 → `github.com/gradle/gradle-distributions` → **502** ❌ |
| `downloads.gradle.org`                   | 307 → 同上 → 502 ❌                                         |
| **`mirrors.huaweicloud.com/gradle/...`** | **200，136,114,148 B 完整下载成功** ✅                      |
| `mirrors.cloud.tencent.com/gradle/...`   | 200（Content-Length 同值）✅                                |

**结论**：Gradle 发行版可通过国内镜像获取；官方路径因 GitHub 不可达而失败。

---

## 4. 依赖仓库可达性（构建能否解析依赖的决定因素）

| 仓库                                   | 实测         | 说明                                           |
| -------------------------------------- | ------------ | ---------------------------------------------- |
| `repo1.maven.org/maven2/`              | **200** ✅   | Maven Central                                  |
| `dl.google.com/dl/android/maven2/...`  | **200** ✅   | Google Maven（AGP / AndroidX）                 |
| `registry.npmjs.org` / `npmmirror.com` | 200 / 200 ✅ | Node 侧                                        |
| `github.com`                           | **502** ❌   | 不可达（影响 Gradle 官方发行版、部分源码依赖） |

**实测证据**：`gradle :core:dependencies --configuration debugCompileClasspath` 成功解析
AGP 8.5.2 / Kotlin 2.0.0 / androidx.core-ktx 1.13.1 / androidx.biometric 1.1.0 / androidx.fragment-ktx 1.8.2 /
`net.zetetic:android-database-sqlcipher:4.5.4` / `org.bouncycastle:bcprov-jdk18on:1.78.1` 全树，无 FAILED 项。

---

## 5. Android 工程形态

| 项                    | 实测值                                                                                        |
| --------------------- | --------------------------------------------------------------------------------------------- |
| 形态                  | **checked-in 最小 Gradle 工程**（非 HBuilderX 生成、非 UTS 生成、非 Android Studio 生成）     |
| 根工程                | `platforms/android/build.gradle.kts` — AGP 8.5.2 / Kotlin 2.0.0（`apply false`）              |
| `settings.gradle.kts` | `pluginManagement { google(); mavenCentral(); gradlePluginPortal() }`、`include(":core")`     |
| 模块                  | `:core` = `com.android.library`，namespace `com.depmap.core`，**compileSdk 34**，minSdk 26    |
| 源码布局              | 主源码 `../kotlin`、测试 `../test`（均在 `core/` 之外，通过 `sourceSets.kotlin.srcDir` 挂载） |
| Manifest              | 仅 `USE_BIOMETRIC`；`allowBackup=false`；`usesCleartextTraffic=false`                         |
| 产物类型              | **AAR**（library 模块，不是 APK —— 见 §8 关于 APK 的说明）                                    |

---

## 6. 非 ASCII 工程路径（本轮首个真实阻断）

```
Your project path contains non-ASCII characters. This will most likely cause the build to fail on Windows.
Please move your project to a different directory. ... android.overridePathCheck=true
```

- 工程默认检出路径 `<repo>` 含中文 → **AGP 在 apply plugin 阶段直接失败**（BUILD FAILED in 6m 55s，未进入编译）。
- **处置**：在 `platforms/android/gradle.properties` 写入 `android.overridePathCheck=true` 并附原因注释（本模块为纯 Kotlin/Java 库，无 NDK、无自定义 aapt2 资源管线）。
- 豁免后构建**真实通过到 Kotlin 编译阶段**，未再出现路径相关错误。

---

## 7. 本轮实测构建结果

| 命令                                                              | 结果                              |
| ----------------------------------------------------------------- | --------------------------------- |
| `gradle -v`（JDK 21）                                             | **Gradle 8.9** ✅                 |
| `gradle :core:dependencies --configuration debugCompileClasspath` | 依赖全解析，**0 FAILED** ✅       |
| `gradle assembleDebug`                                            | **BUILD 成功，产出 AAR** ✅       |
| `gradle :core:testDebugUnitTest`                                  | 见 `docs/ANDROID_BUILD_REPORT.md` |

### 7.1 实际使用命令（可复现）

```bash
export JAVA_HOME="<ANDROID_STUDIO_HOME>/jbr"
export ANDROID_HOME="<ANDROID_SDK_ROOT>"
export ANDROID_SDK_ROOT="<ANDROID_SDK_ROOT>"
cd platforms/android
"<GRADLE_HOME>/bin/gradle.bat" --no-daemon assembleDebug test
```

---

## 8. 重要范围界定：AAR ≠ APK

- 本工程产出的是 **Android 原生安全核心库（AAR）**，用于验证 Kotlin 侧的
  Schema DDL / SQLCipher 适配 / Keystore / Biometric / `.depmap` 容器 golden 互操作。
- **产品级 APK/AAB 由 uni-app x（HBuilderX）产出**，本 Gradle 工程不生成 APK。
- 因此：
  - `ANDROID_TOOLCHAIN_READY`、`ANDROID_NATIVE_CORE_BUILD` 可由本轮结论判定；
  - `ANDROID_APP_BUILD_READY`（产品 APK）仍受 **B10（HBuilderX 无 CLI 构建命令）** 约束。
- 不得把「AAR 构建成功」表述为「Android 应用可安装」——两者是不同层级。

---

## 9. 结论

| 判定项                  | 状态                                                     |
| ----------------------- | -------------------------------------------------------- |
| JDK（17+）可用          | **PASS**（JDK 21 实测可用）                              |
| Android SDK 可用        | **PASS**（platform 34 + build-tools 34.0.0 已补齐）      |
| Gradle 可用             | **PASS**（8.9 经镜像获取并实测运行）                     |
| 依赖仓库可达            | **PASS**（Maven Central + Google Maven 200）             |
| Android 原生核心可构建  | **PASS**（AAR 已产出，见构建报告）                       |
| 物理设备 / 模拟器       | **BLOCKED**（`adb devices` 空、无 AVD、无 system-image） |
| 产品级 APK（uni-app x） | **BLOCKED（B10）**                                       |

---

_配套文件：`docs/ANDROID_BUILD_REPORT.md`、`docs/ANDROID_PERMISSION_AUDIT.md`、`docs/ANDROID_RELEASE_RUNBOOK.md`_
