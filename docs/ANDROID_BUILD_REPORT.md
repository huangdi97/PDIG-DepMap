# ANDROID_BUILD_REPORT.md — Android 原生核心真实构建报告

> 范围：`platforms/android`（DepMap Android 原生安全核心，Gradle `com.android.library` 模块）。
> 本报告只陈述**真实执行过**的命令与**真实存在**的产物；未执行的项一律标注 `NOT_RUN`。
> 报告时间：2026-09-15（本机系统时间）。

---

## 1. 结论摘要

| 项                               | 结论        | 证据                                               |
| -------------------------------- | ----------- | -------------------------------------------------- |
| Android 原生核心 **COMPILED**    | **PASS**    | `core-debug.aar` / `core-release.aar` 已产出       |
| Android 原生核心 **UNIT_TESTED** | **PASS**    | 黄金向量 4 用例，debug + release 双变体各 4/4 通过 |
| Android **产品 APK / AAB**       | **BLOCKED** | 依赖 HBuilderX（**B10**，见 §7）                   |
| Android **INSTALL_READY**        | **BLOCKED** | 无 APK；且无可用设备（§8）                         |
| Android **DEVICE_VERIFIED**      | **BLOCKED** | `adb devices` 为空、无 AVD、无 system-image        |

**关键界定（必须显式声明）**：本报告产出的 `*.aar` 是**原生库归档**，**不是**可安装的应用包。
AAR ≠ APK。Android 的 `INSTALL_READY` / `DEVICE_VERIFIED` 不因本报告而变为 PASS。

---

## 2. 工具链（实测版本）

| 组件                  | 版本 / 路径                                                            |
| --------------------- | ---------------------------------------------------------------------- |
| JDK（构建用）         | OpenJDK **21.0.10**（`<ANDROID_STUDIO_HOME>\jbr`）                    |
| Gradle                | **8.9**（`<GRADLE_HOME>`，经华为镜像获取）           |
| Android Gradle Plugin | **8.5.2**                                                              |
| Kotlin                | **2.0.0**                                                              |
| compileSdk / minSdk   | **34** / **26**                                                        |
| Android SDK           | `<ANDROID_SDK_ROOT>`（`platforms;android-34` + `build-tools;34.0.0`） |
| 构建产物根目录        | `%USERPROFILE%\depmap-android-build`（见 §5 非 ASCII 路径回退）      |

> 注意：PATH 上的 `java` 为 1.8.0_441，**不可用于 AGP**。构建必须显式指定
> `JAVA_HOME=<ANDROID_STUDIO_HOME>\jbr`。`JAVA_HOME` / `ANDROID_HOME` 在本机**均未预设**。

---

## 3. 源码范围

`platforms/android/kotlin` + `platforms/android/test`，共 **8 个 Kotlin 文件 / 866 行**：

| 文件                                             | 作用                                                              |
| ------------------------------------------------ | ----------------------------------------------------------------- |
| `crypto/DepmapContainerV1.kt`                    | `.depmap` 容器 V1（Argon2id v19 + AES-256-GCM），黄金向量参考实现 |
| `schema/DepmapSchemaV1.kt`                       | 与 Core 同步的 Schema DDL                                         |
| `security/SecureDatabaseAdapter.kt`              | 数据库适配器契约（**本轮新建**）                                  |
| `security/SqlCipherSecureDatabaseAdapter.kt`     | SQLCipher 加密数据库实现                                          |
| `security/SecureKeyAdapter.kt`                   | 密钥适配器契约                                                    |
| `security/KeystoreSecureKeyAdapter.kt`           | Android Keystore 密钥实现                                         |
| `security/KeystoreSecureKeyAdapterExtensions.kt` | `getOrCreateDatabaseKeyBytes()` 扩展（**本轮新建**）              |
| `security/BiometricGateAdapter.kt`               | 生物识别启动锁                                                    |
| `security/FlagSecurePrivacyAdapter.kt`           | 后台隐私遮罩（FLAG_SECURE）                                       |
| `test/.../DepmapContainerV1GoldenTest.kt`        | 黄金向量互操作测试（4 用例）                                      |

---

## 4. 本轮修复的真实缺陷（首次真实编译暴露）

工程此前**从未被编译器验证过**（旧报告状态为 `COMPILED = NO`）。首次真实构建暴露并修复 **9 项缺陷**：

| #    | 缺陷                                                                                                                                | 根因                                                                                                                                                                                                                  | 处置                                                                                                          |
| ---- | ----------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| D-1  | `Your project path contains non-ASCII characters`                                                                                   | AGP 默认拒绝非 ASCII 工程路径                                                                                                                                                                                         | 新增 `gradle.properties`，写入 `android.overridePathCheck=true`                                               |
| D-2  | `Configuration ':core:debugRuntimeClasspath' contains AndroidX dependencies, but the 'android.useAndroidX' property is not enabled` | 工程从未有 `gradle.properties`                                                                                                                                                                                        | 同上文件写入 `android.useAndroidX=true`                                                                       |
| D-3  | `consumer-rules.pro` 缺失                                                                                                           | `consumerProguardFiles` 引用但文件不存在                                                                                                                                                                              | 新建该文件                                                                                                    |
| D-4  | 约 50 处 `Unresolved reference 'sqlcipher'` 等                                                                                      | 依赖坐标 `net.zetetic:sqlcipher-android` 使用 `net.zetetic.database.sqlcipher` 命名空间，而源码按 `net.sqlcipher.database` 编写                                                                                       | `javap` 比对后改用 `net.zetetic:android-database-sqlcipher:4.5.4`（同为 SQLCipher 引擎，API 与源码一致）      |
| D-5  | `Unresolved reference 'SecureDatabaseAdapter'`                                                                                      | 接口在全仓**无定义**，但类声明实现它                                                                                                                                                                                  | 新建 `security/SecureDatabaseAdapter.kt`                                                                      |
| D-6  | `Unresolved reference 'getOrCreateDatabaseKeyBytes'`                                                                                | 扩展方法不存在                                                                                                                                                                                                        | 新建 `KeystoreSecureKeyAdapterExtensions.kt`                                                                  |
| D-7  | `Syntax error: Unexpected tokens` @ `DepmapContainerV1.kt:83`                                                                       | 两条语句挤在同一行                                                                                                                                                                                                    | 拆行                                                                                                          |
| D-8  | 约 15 处 `Unresolved reference 'writableDatabase'`                                                                                  | SQLCipher 的 `getWritableDatabase` **无无参重载**，必须带口令                                                                                                                                                         | 新增 `private var db: SQLiteDatabase?`，`open()` 中经 `getWritableDatabase(passphrase)` 取得并缓存            |
| D-9  | 黄金向量 **互操作契约破裂**                                                                                                         | `android.util.Base64` 的 `NO_PADDING` 产出**无填充** Base64，而 Core Node 参考实现（`core/src/crypto/depmap.ts`）明确为 `RFC 4648 standard with padding`，冻结向量 `tagBase64 = "5qpABhovPbNet1q2GNEhkg=="` 亦带 `==` | 改用 `java.util.Base64`（API 26+ 可用，minSdk=26）标准带填充编码                                              |
| D-10 | **Android 端无法解密任何容器**                                                                                                      | 极简 JSON 解析器 `Regex("\"algorithm\":\"...\"")` 取**首个**匹配。JCS 键序中 `cipher.algorithm` 位于最前 → 取到 `"AES-256-GCM"` 而非 `"argon2id"` → `validateBounds` 恒定抛 `bounds`                                  | `algorithm` / `version` / `salt` / `memoryKiB` / `iterations` / `parallelism` 限定在 `"kdf":{...}` 子串内解析 |

> D-9 与 D-10 是**产品级 P0 缺陷**（不是测试问题）：前者破坏三端容器互操作，
> 后者使 Android 端 100% 无法打开任何 `.depmap` 容器。二者均只在**真实执行测试**后才暴露。

---

## 5. 非 ASCII 工程路径：根因取证与处置

### 5.1 现象

在中文路径（`<repo>\platforms\android`）下执行 `testDebugUnitTest`：

```
DepmapContainerV1GoldenTest > initializationError FAILED
    java.lang.ClassNotFoundException at null:-1
1 test completed, 1 failed
```

同一份源码在 ASCII 路径（`<DEPMAP_TOOLS_HOME>\android-ascii`）下 **4/4 通过**。

### 5.2 取证（非推测）

1. **排除源码与类本身问题**：在中文路径下用 Gradle 自身的 classpath 构造 `URLClassLoader`，
   `Class.forName("com.depmap.core.crypto.DepmapContainerV1GoldenTest")` → **成功**。
   且 `testClassesDirs` 指向的目录 `exists=true`，`.class` 文件确实存在。
   → 类可加载，问题在 **worker 进程的 classpath 传递**。

2. **定位传递机制**：Gradle 在 classpath 超长时会把 JVM 参数写入
   `~/.gradle/.tmp/gradle-worker-classpath*.txt`（argfile）。实测该文件内容：

   ```
   -cp
   %USERPROFILE%\\.gradle\\caches\\8.9\\workerMain\\gradle-worker.jar;<repo>\\platforms\\android\\core\\build\\...
   ```

   文件以 **UTF-8** 写入（实测字节 `e5 8f b7 e5 8d a1` = 「号卡」），
   而 JVM 解析 argfile 使用 **`sun.jnu.encoding`**（Windows 中文环境 = **GBK**）。

3. **结论**：两侧编码不一致 → classpath 中的中文路径 mojibake → worker 找不到类。
   ASCII 路径下内容为纯 ASCII，UTF-8 与 GBK 解码结果相同，故掩盖了该缺陷。

4. **排除干扰项**：`--rerun-tasks --no-build-cache` 为必要验证条件。
   仅 `clean` 不够 —— 实测 `clean testDebugUnitTest` 会报 `FROM-CACHE`
   （`org.gradle.caching=true`，cache key 基于输入内容哈希，跨路径复用），
   产生「通过」的**假象**。

### 5.3 处置

classpath 中 5 个非 ASCII 条目**全部来自 `build/` 产物目录**。因此在
`platforms/android/build.gradle.kts` 中实现路径回退：当工程路径含非 ASCII 字符时，
把 `layout.buildDirectory` 整体重定向到 ASCII 路径，classpath 即不再出现非 ASCII 条目。

```kotlin
val asciiBuildRoot: String? = run {
    val root = rootDir.absolutePath
    if (!root.any { it.code > 127 }) return@run null
    System.getenv("DEPMAP_ANDROID_BUILD_ROOT")
        ?: File(System.getProperty("user.home"), "depmap-android-build").absolutePath
}
if (asciiBuildRoot != null) {
    allprojects { layout.buildDirectory.set(file("$asciiBuildRoot/${project.name}")) }
    tasks.register<Copy>("collectArtifacts") { /* AAR 回收到 platforms/android/artifacts/ */ }
}
```

可用环境变量 `DEPMAP_ANDROID_BUILD_ROOT` 覆盖回退位置。

### 5.4 修复验证

| 条件                              | 命令                                               | 结果                                           |
| --------------------------------- | -------------------------------------------------- | ---------------------------------------------- |
| 中文路径 + 强制真实执行（修复前） | `testDebugUnitTest --rerun-tasks --no-build-cache` | **FAILED**（`ClassNotFoundException`）         |
| 中文路径 + 强制真实执行（修复后） | 同上                                               | **BUILD SUCCESSFUL**（15 tasks 全部 executed） |
| 中文路径 + build cache            | `clean testDebugUnitTest`                          | `FROM-CACHE`（**不可作为证据**）               |

---

## 6. 真实产物与测试结果

### 6.1 构建命令

```bash
cd <repo>/platforms/android
export JAVA_HOME="<ANDROID_STUDIO_HOME>/jbr"
export ANDROID_HOME="<ANDROID_SDK_ROOT>"
"<GRADLE_HOME>/bin/gradle.bat" --no-daemon clean assembleDebug assembleRelease collectArtifacts
```

结果：`BUILD SUCCESSFUL in 5m 51s`，`50 actionable tasks: 29 executed, 17 from cache, 4 up-to-date`。

### 6.2 产物清单

| 产物        | 路径                                           | 字节   | SHA-256                                                            |
| ----------- | ---------------------------------------------- | ------ | ------------------------------------------------------------------ |
| Debug AAR   | `platforms/android/artifacts/core-debug.aar`   | 44,147 | `ae2c11a47a86d2657facb6f149f5e83c6739a12f9b961886a2a3fcd51a0edd6a` |
| Release AAR | `platforms/android/artifacts/core-release.aar` | 42,352 | `d82b4a2f3005deedcd73fcc05e6d866bffa7103485da7de02b42476e86c93d67` |

`core-release.aar` 结构（实测解包）：`AndroidManifest.xml`(573B) / `R.txt`(0B) / `classes.jar`(44,687B)，
`classes.jar` 内含 **23 个 Kotlin class**，覆盖
`crypto/DepmapContainerV1`、`schema/DepmapSchemaV1`、`security/{SecureDatabaseAdapter, SqlCipherSecureDatabaseAdapter, KeystoreSecureKeyAdapter, KeystoreSecureKeyAdapterExtensionsKt, BiometricGateAdapter, FlagSecurePrivacyAdapter}`。

### 6.3 测试结果

```bash
gradle --no-daemon testDebugUnitTest --rerun-tasks --no-build-cache
```

| 变体    | 套件                                                 | tests | failures | errors | skipped |
| ------- | ---------------------------------------------------- | ----- | -------- | ------ | ------- |
| debug   | `com.depmap.core.crypto.DepmapContainerV1GoldenTest` | 4     | **0**    | **0**  | **0**   |
| release | `com.depmap.core.crypto.DepmapContainerV1GoldenTest` | 4     | **0**    | **0**  | **0**   |

用例：`goldenVector_reproduces_frozenValues` / `goldenVector_decrypts` /
`wrongPassword_fails` / `maliciousMemoryKiB_rejected_beforeKdf` —— 全部 PASS。

**互操作意义**：`goldenVector_reproduces_frozenValues` 通过，意味着 Android Kotlin 实现产出的
`ciphertext` 与 `tag` 与 Core Node 参考实现冻结值**逐字节一致**；
`goldenVector_decrypts` 通过，意味着 Android 能解开同一容器。
即 `Node encrypt → Android decrypt` 与 `Android encrypt → Node decrypt` 双向互操作成立。

---

## 7. 产品 APK / AAB：BLOCKED（B10）

AAR 是**库归档**，无法安装到设备，也不是商店交付物。Android 产品包必须由
uni-app x 工程（`app/`）经 HBuilderX 产出。当前阻塞：

- HBuilderX 已实际安装（`5.24.2026081301`，`<DEPMAP_TOOLS_HOME>\HBuilderX`），工程导入成功；
- 但其 CLI 的**全量命令集不含任何 build / publish / run 命令**
  （仅 `help` / `open file` / `project open|close|list` / `report-bug` / `uni-agent` / `user login|info|logout` / `version`），
  无法纯命令行触发 uni-app x 编译；
- 因此 `ANDROID_BUILD_READY`（产品包）仍为 **BLOCKED**，需 GUI 操作或云打包（见 `STORE_EXTERNAL_BLOCKERS.md`）。

---

## 8. 设备与安装：BLOCKED

| 项               | 实测                                                             |
| ---------------- | ---------------------------------------------------------------- |
| `adb devices -l` | **空**（无连接设备）                                             |
| AVD 列表         | **0 个**（`emulator -list-avds` 空）                             |
| system-images    | **不存在**                                                       |
| PATH 上的 `adb`  | `1.0.32`（2016 年，`<ANDROID_SDK_ROOT>\adb`）与 SDK 的 `1.0.41` **冲突** |

→ `INSTALL_READY` / `DEVICE_VERIFIED` = **BLOCKED**。本报告**不声称**任何真机验证结果。

---

## 9. 复现指引

```bash
# 1. 必需环境
export JAVA_HOME="<ANDROID_STUDIO_HOME>/jbr"      # JDK 21
export ANDROID_HOME="<ANDROID_SDK_ROOT>"          # 含 platforms;android-34
export PATH="<GRADLE_HOME>/bin:$PATH"

# 2. 构建 + 测试（--rerun-tasks --no-build-cache 才能得到真实结果）
cd <repo>/platforms/android
gradle --no-daemon clean assembleDebug assembleRelease collectArtifacts
gradle --no-daemon testDebugUnitTest testReleaseUnitTest --rerun-tasks --no-build-cache
```

**警告**：不要用 `gradle test` 不加 `--rerun-tasks --no-build-cache` 来判定测试真实性 ——
在 `org.gradle.caching=true` 下会命中跨路径缓存，产生 `FROM-CACHE` 的假通过。

---

## 10. 结论

- Android **原生安全核心**：`SOURCE_READY = PASS`，`COMPILED = PASS`，`UNIT_TESTED = PASS`。
- Android **产品应用包**：`BUILD_READY = BLOCKED (B10)`，`INSTALL_READY = BLOCKED`，`DEVICE_VERIFIED = BLOCKED`。
- 本轮修复 10 项真实缺陷，其中 2 项为 P0 级产品缺陷（跨端 Base64 契约、容器解析字段冲突）。
- **AAR ≠ APK**。不得据此声明 Android 可上线。
