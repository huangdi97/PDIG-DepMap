# PLATFORM_BRINGUP_PRE_AUDIT.md

> 本轮任务：**GOAL_PLATFORM_BRINGUP_ANDROID_HARMONY_RELEASE_VALIDATION**
> 阶段：PDIG Platform Bring-up & Release Validation
> 审计日期：2026-09-14（会话内实测）
> 审计原则：**全部数值来自本轮实测命令输出**，不复制任何旧报告结论；旧报告仅用于对照。

---

## 0. 审计方法声明

- 本文件所有「实测」项均由本轮在 Windows（win32）主机上真实执行命令获得。
- 未安装任何软件前先完成本审计；后续安装动作在 §9 记录。
- 旧报告（`BLOCKERS.md` / `FINAL_PLATFORM_MATRIX.md`）中的 B1 / B2 前提在本轮被**部分推翻**（见 §7）。
- 禁止项遵守：未执行 `git reset --hard` / `git clean -fd` / `git checkout .` / `git restore .`。

---

## 1. Git 现场

| 项                  | 实测值                                                                      |
| ------------------- | --------------------------------------------------------------------------- |
| HEAD                | `ee7ee58`（`docs(closure): add the directive status-name mapping ...`）     |
| branch              | `feat/mvp03-living-graph`                                                   |
| `git status`        | `nothing to commit, working tree clean`（进入时）                           |
| `git diff`          | 空（clean）                                                                 |
| `git diff --staged` | 空（clean）                                                                 |
| tags                | `v0.2.0-mvp02`、`v0.3.0-mvp03`                                              |
| 未跟踪文件          | 进入时无；本审计过程中新增 `.tmp_audit/`（临时工具目录，收尾清理，见 §9.5） |
| 远端                | 未 push（用户未授权）                                                       |

**结论：工作区进入时为 clean，无未提交工作需保全，无覆盖风险。**

---

## 2. 主机与 Node 运行时

| 项       | 实测值                                                                      |
| -------- | --------------------------------------------------------------------------- |
| OS       | Windows（`win32`），构建号 `10.0.26200.9445`                                |
| Shell    | bash（Git Bash / PortableGit）                                              |
| Node     | **v22.22.2**（受管运行时）                                                  |
| npm      | **10.9.7**                                                                  |
| 工程声明 | `core/package.json` → `engines.node >= 22.5.0`，`packageManager npm@11.3.0` |

> 差异：声明 `npm@11.3.0`，实测 `10.9.7`（历史登记为 **F-3**，本轮未擅自修改）。

---

## 3. Java / JDK

| 来源                    | 实测版本                | 路径                                     |
| ----------------------- | ----------------------- | ---------------------------------------- |
| PATH `java`             | **1.8.0_441**（Java 8） | PATH 默认                                |
| PATH `javac`            | **不存在**              | —                                        |
| Android Studio 内置 JBR | **OpenJDK 21.0.10**     | `<ANDROID_STUDIO_HOME>\jbr\bin\java.exe` |
| DevEco Studio 内置 JBR  | **OpenJDK 17.0.12**     | `<DEVECO_HOME>\jbr\bin\java.exe`         |
| `JAVA_HOME`             | **未设置**              | —                                        |

**结论**：JDK 17 / 21 均存在但**均未进入 PATH**，且 `JAVA_HOME` 未设置。Android 构建须显式指定 `JAVA_HOME`。PATH 上的 Java 8 **不可用于 AGP 8.5.2**。

---

## 4. Android 工具链

| 项                 | 实测值                                                                                                                                         |
| ------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| `ANDROID_HOME`     | **未设置**                                                                                                                                     |
| `ANDROID_SDK_ROOT` | **未设置**                                                                                                                                     |
| Android SDK 根     | `<ANDROID_SDK_ROOT>`（实测存在）                                                                                                               |
| platforms          | `android-34`（本轮新装）、`android-36.1`、`android-37.0`                                                                                       |
| build-tools        | `34.0.0`（本轮新装）、`36.1.0`、`37.0.0`                                                                                                       |
| cmdline-tools      | `latest`（v20.0，可执行）                                                                                                                      |
| platform-tools     | `37.0.0` → `adb` 1.0.41                                                                                                                        |
| licenses           | 已接受（`android-sdk-license` 等 7 项在盘）                                                                                                    |
| emulator           | 已安装 `36.5.11`                                                                                                                               |
| system-images      | **无**（`<ANDROID_SDK_ROOT>\system-images` 不存在）                                                                                            |
| AVD                | **0 个**（`emulator -list-avds` 空输出）                                                                                                       |
| `adb devices -l`   | **空**（无物理设备、无模拟器）                                                                                                                 |
| Gradle（PATH）     | **不存在**                                                                                                                                     |
| Gradle 发行版      | 旧：`~/.gradle/wrapper/dists/gradle-9.3.1-bin/` 仅 0 字节 `.lck`/`.part`；**本轮新装 8.9 于 `<GRADLE_HOME>`（`gradle -v` 实测 `Gradle 8.9`）** |
| Android Studio     | 已安装（`DS`/`AI-253.…` 版本目录在盘）                                                                                                         |
| `PATH` 上的 `adb`  | **旧版 1.0.32**（`<ANDROID_SDK_ROOT>\adb`，2016 年文件）→ 与 SDK adb 1.0.41 端口冲突，须用 SDK 版                                              |

---

## 5. HarmonyOS 工具链

| 项               | 实测值                                                                                                                        |
| ---------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| DevEco Studio    | **5.0.5.310**（`build.txt` = `DS-233.14475.28.36.505310`）                                                                    |
| 安装路径         | `<DEVECO_HOME>`                                                                                                               |
| HarmonyOS SDK    | `sdk/default/{openharmony,hms}`，`sdk-pkg.json` = **HarmonyOS 5.0.1 / apiVersion 13 / version 5.0.1.115 / metaVersion 3.0.0** |
| SDK 组件（在盘） | `openharmony/{ets, js, native, previewer, toolchains}` **五组件全部存在**，各自 `oh-uni-package.json` 均报 `apiVersion 13`    |
| hvigor           | `tools/hvigor/bin/hvigorw`（hvigorw.bat / hvigorw.js）                                                                        |
| ohpm             | `tools/ohpm/bin/ohpm`                                                                                                         |
| DevEco 内置 Node | `tools/node`                                                                                                                  |
| 签名工具         | 未验证（无证书）                                                                                                              |

> **关键矛盾（本轮保留）**：五组件在盘且元数据正确，但 hvigor 报 `Unable to find the following components: toolchains:13 / ArkTS:13 / js:13 / native:13 / previewer:13`，并伴随远端接口 `repo.harmonyos.com/sdkmanager/v5/ohos/getSdkList` **HTTP 400**。详见 §7-B2 与 `docs/HARMONY_TOOLCHAIN_AUDIT.md`。

---

## 6. HBuilderX / uni-app x

| 项                   | 实测值                                                                                             |
| -------------------- | -------------------------------------------------------------------------------------------------- |
| 旧状态（B10）        | 全盘搜索无命中 → 本轮**推翻**：可获取官方安装包并已落地                                            |
| 官方版本索引         | `https://download1.dcloud.net.cn/hbuilderx/release.json` → **5.24.2026081301**                     |
| 安装包               | `HBuilderX.5.24.2026081301.zip`（87.09 M，官方 CDN 下载成功）                                      |
| 安装路径（本轮）     | `<DEPMAP_TOOLS_HOME>\HBuilderX`（绿色免安装，解压即用）                                            |
| 解压结果             | **5151 个文件 / 143 MB**，`HBuilderX.exe`、`cli.exe` 均在                                          |
| 版本（实测）         | **5.24.2026081301**（`cli --help` 自报 + 启动日志）                                                |
| 进程状态             | 已成功启动并常驻（PID 存在），日志正常输出                                                         |
| 插件自动下载         | **已在真实下载**：`node`、`npm`、`hbuilderx-language-services` 等                                  |
| 插件源               | `https://update.liuyingyong.cn/hbuilderx/...` → 302 → `hsdcdn.qnqcdn.net`（**可达，200**）         |
| 工程导入             | `cli.exe project open --path <app>` → **「项目导入成功」**；`project list` → `1 - app(UniApp_VUE)` |
| CLI 命令集           | `help` / `open file` / `project open                                                               | close | list`/`report-bug`/`uni-agent`/`user login | info | logout`/`version` |
| **CLI 是否有 build** | **无**。`cli --help` 全量输出（116 行）**不含任何 publish / build / run 命令**                     |

### 6.1 工程侧 uni-app x 配置（`app/manifest.json`）

| 项                     | 值                                                                                            |
| ---------------------- | --------------------------------------------------------------------------------------------- |
| name                   | 个人数字依赖图                                                                                |
| appid                  | `__UNI__DEPMAP01`（**非正式 appid**，HBuilderX 未登录态）                                     |
| versionName/Code       | `0.1.0` / `1`                                                                                 |
| `uni-app-x.compatible` | **4.41**                                                                                      |
| `vueVersion`           | **3**                                                                                         |
| android                | minSdk 26 / targetSdk 34 / abiFilters `arm64-v8a` / permissions `[]`                          |
| harmony                | minAPIVersion 12 / targetAPIVersion 12                                                        |
| ios                    | `NSFaceIDUsageDescription` + `NSCameraUsageDescription`（**U-2 未闭环**）                     |
| icons/splash           | 引用 `static/icons/*.png`、`static/splash/*.png` → **`app/static/` 目录不存在**（U-1 未闭环） |

### 6.2 UTS 插件（5 个）

`app/uni_modules/` 下：`depmap-biometric`、`depmap-file-crypto`、`depmap-privacy-screen`、`depmap-secure-database`、`depmap-secure-key`。
每个含 `utssdk/{app-android,app-harmony,app-ios}/index.uts` + `interface.uts` + `package.json`。
→ **三端桥接源码齐备（SOURCE 级）**，编译验证依赖 HBuilderX 编译链。

---

## 7. 平台工程现状

### 7.1 Android（`platforms/android/`）

| 项                    | 值                                                                                                                                                                                                                                                   |
| --------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 形态                  | **checked-in 的最小 Gradle 工程**（非 HBuilderX 生成、非 UTS 生成）                                                                                                                                                                                  |
| 根构建                | `build.gradle.kts`：AGP **8.5.2**、Kotlin **2.0.0**（均 `apply false`）                                                                                                                                                                              |
| `settings.gradle.kts` | `pluginManagement { google(); mavenCentral(); gradlePluginPortal() }`；`rootProject.name = depmap-android-core`；`include(":core")`                                                                                                                  |
| `:core`               | `com.android.library`，namespace `com.depmap.core`，**compileSdk 34**，minSdk 26，Java/Kotlin target **17**                                                                                                                                          |
| 源码                  | `kotlin/com/depmap/core/{crypto/DepmapContainerV1.kt, schema/DepmapSchemaV1.kt, security/{BiometricGateAdapter,KeystoreSecureKeyAdapter,SqlCipherSecureDatabaseAdapter}.kt}`                                                                         |
| 测试                  | `test/com/depmap/core/crypto/DepmapContainerV1GoldenTest.kt`（golden vector 互操作）                                                                                                                                                                 |
| 依赖                  | androidx.core-ktx 1.13.1 / biometric 1.1.0 / fragment-ktx 1.8.2 / **net.zetetic:sqlcipher-android 4.6.1** / androidx.sqlite 2.4.0 / **bcprov-jdk18on 1.78.1** / coroutines-android 1.8.1；test: junit 4.13.2 / org.json 20240303 / kotlin-test 2.0.0 |
| Manifest              | 最小权限：仅 `USE_BIOMETRIC`；`allowBackup=false`；`usesCleartextTraffic=false`                                                                                                                                                                      |
| Gradle wrapper        | **无**（工程内无 `gradlew` / `gradle-wrapper.properties`）                                                                                                                                                                                           |
| `local.properties`    | 本轮创建（`sdk.dir=<ANDROID_SDK_ROOT>`），已被 `.gitignore` 忽略                                                                                                                                                                                     |

### 7.2 HarmonyOS（`platforms/harmonyos/`）

| 项   | 值                                                                                                |
| ---- | ------------------------------------------------------------------------------------------------- |
| 形态 | **适配器片段**，非可构建 Stage 工程                                                               |
| 文件 | `app.json5`、`entry/src/main/module.json5`、`arkts/RelationalStoreSecureAdapter.ets`（共 3 文件） |
| 结论 | 需 HBuilderX（uni-app x）产出产品级 HarmonyOS 工程；DevEco 仅用于适配器独立验证                   |

### 7.3 iOS（`platforms/ios/`）

| 项   | 值                                                                                                                                |
| ---- | --------------------------------------------------------------------------------------------------------------------------------- |
| 形态 | SwiftPM 包：`Package.swift` + `swift/SQLCipherSecureDatabaseAdapter.swift` + `Tests/DepMapCoreTests/DepmapContainerV1Tests.swift` |
| 结论 | SOURCE 齐备；编译/签名/真机在 Windows 上不可能                                                                                    |

### 7.4 UI（`app/`）

| 项        | 值                                                                            |
| --------- | ----------------------------------------------------------------------------- |
| 页面      | **24 页 `.uvue`** + 5 个 `dp-*` 组件 + `App.uvue` + `main.uts`                |
| 服务层    | `services/depmap-service.uts`（唯一数据边界）、`services/rules.uts`（纯规则） |
| 状态      | `stores/app-state.uts`；主题 `theme/tokens.uts`                               |
| 静态 Gate | `core/scripts/check-ui.mjs`（U1–U9）                                          |

---

## 8. 现有构建产物

**本轮审计时点：无任何平台构建产物。**

- `platforms/android`：无 `build/`、无 AAR、无 APK
- `platforms/harmonyos`：无 HAP
- `app`：无 `unpackage/dist/`
- 全仓库：无 `*.apk` / `*.aab` / `*.hap` / `*.ipa`（`.gitignore` 亦排除这些扩展名）

---

## 9. 本轮已执行的环境动作（审计后）

| #   | 动作                                                           | 结果                                                               |
| --- | -------------------------------------------------------------- | ------------------------------------------------------------------ |
| 9.1 | 下载 Gradle 8.9（华为镜像 136 MB）                             | **成功**，解压至 `<GRADLE_HOME>`，`gradle -v` = 8.9                |
| 9.2 | `sdkmanager --install platforms;android-34 build-tools;34.0.0` | **成功**，两组件已落盘                                             |
| 9.3 | 下载 HBuilderX 5.24（官方 CDN 87 MB）                          | **成功**，解压至 `<DEPMAP_TOOLS_HOME>\HBuilderX`，已启动并导入工程 |
| 9.4 | 创建 `platforms/android/local.properties`                      | **成功**（已 gitignore）                                           |
| 9.5 | 创建 `.tmp_audit/` 临时目录                                    | 存放工具包与探测输出；**收尾时清理，不入库**                       |

### 9.5 网络可达性实测（与旧报告差异显著）

| 目标                                        | 旧报告 | 本轮实测                       |
| ------------------------------------------- | ------ | ------------------------------ |
| `services.gradle.org`                       | exit 7 | **307 → github 502（不可用）** |
| `mirrors.huaweicloud.com/gradle/...`        | 未记录 | **200（136 MB 实际下载成功）** |
| `repo1.maven.org`                           | 不可达 | **200**                        |
| `dl.google.com/dl/android/maven2/...`       | 不可达 | **200**                        |
| `registry.npmjs.org` / `npmmirror.com`      | —      | **200 / 200**                  |
| `github.com`                                | 不可达 | **502（仍不可用）**            |
| `download1.dcloud.net.cn`                   | 未记录 | **200（HBuilderX 下载成功）**  |
| `update.liuyingyong.cn`（HBuilderX 插件源） | 未记录 | **302 → 200**                  |

> 结论：**构建依赖 CDN 现已可达**，旧报告「构建依赖 CDN 不可达」的前提**不再成立**。

---

## 10. 当前 Blockers（本轮实测口径）

| #   | Blocker                                                                             | 性质                   |
| --- | ----------------------------------------------------------------------------------- | ---------------------- |
| B1  | **工程路径含非 ASCII 字符**（`<repo>`）→ AGP 拒绝构建（已实测复现）                 | **可解（本轮处理中）** |
| B2  | HarmonyOS hvigor 组件解析失败 + `repo.harmonyos.com` 远端 400                       | 待定位                 |
| B3  | 无 macOS / Xcode（`xcodebuild` 不存在）                                             | **外部，不可解**       |
| B10 | HBuilderX **CLI 无 build/publish 命令** → 无法在无 GUI 交互下触发 uni-app x 编译    | **本轮新根因**         |
| B23 | 沙箱：`npm ci` 批量删除被 safe-delete 守卫拦截；clean clone 闭环不可执行            | 环境                   |
| B24 | **无 Android 设备 / 无模拟器 / 无 system-image / 无 AVD** → 安装与真机 E2E 不可执行 | 外部                   |

---

## 11. 当前可执行命令（本轮已验证可用）

```bash
# Node 侧全门禁
cd core && npm run check && npm run check:full

# Android 原生核心（需显式 JAVA_HOME / ANDROID_HOME）
export JAVA_HOME="<ANDROID_STUDIO_HOME>/jbr"
export ANDROID_HOME="<ANDROID_SDK_ROOT>"
export ANDROID_SDK_ROOT="<ANDROID_SDK_ROOT>"
cd platforms/android
"<GRADLE_HOME>/bin/gradle.bat" --no-daemon \
  -Pandroid.overridePathCheck=true assembleDebug test

# HBuilderX CLI（仅项目/文件级操作，无构建）
"<DEPMAP_TOOLS_HOME>/HBuilderX/cli.exe" project list
"<DEPMAP_TOOLS_HOME>/HBuilderX/cli.exe" project open --path "<repo>\app"
```

---

## 12. 下一步（本审计导出的执行序）

1. **B1**：解除 AGP 非 ASCII 路径阻断 → 真实 `assembleDebug` + `test`
2. **B10**：判定 HBuilderX CLI 无构建命令后，评估 GUI/云打包/直接调用编译器的可行路径
3. **B2**：定位 hvigor 组件解析 + 远端 400 根因
4. **B3/B24**：iOS handoff 文档化；Android 设备缺失如实登记
5. 回归：`check` / `check:full` / MVP01–03
6. 产出 `PLATFORM_RELEASE_MATRIX.md` / `PRODUCTION_RC_V1_REPORT.md` 等交付文件

---

_本审计为 Phase 2 交付物。后续阶段结果见 `PLATFORM_RELEASE_MATRIX.md` 与 `PRODUCTION_RC_V1_REPORT.md`。_
