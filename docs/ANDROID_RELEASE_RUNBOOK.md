# ANDROID_RELEASE_RUNBOOK.md — Android 构建与发布运行手册

> 适用对象：`platforms/android`（原生安全核心，Gradle library 模块）与 `app/`（uni-app x 产品工程）。
> 本手册区分「**可执行**」与「**当前 BLOCKED**」两类步骤，BLOCKED 步骤标注阻塞原因与解除条件。

---

## 0. 分层目标速查

| 目标           | 命令 / 入口                                    | 当前状态                         |
| -------------- | ---------------------------------------------- | -------------------------------- |
| 原生核心编译   | `gradle assembleDebug assembleRelease`         | **可执行**                       |
| 原生核心单测   | `gradle testDebugUnitTest testReleaseUnitTest` | **可执行**                       |
| 产品 APK / AAB | HBuilderX（GUI 或云打包）                      | **BLOCKED (B10)**                |
| 安装到设备     | `adb install`                                  | **BLOCKED**（无 APK、无设备）    |
| 商店提交       | AppGallery / 应用宝等                          | **BLOCKED**（B4 签名 + B5 账号） |

---

## 1. 环境准备

```bash
export JAVA_HOME="<ANDROID_STUDIO_HOME>/jbr"      # JDK 21.0.10（必需，PATH 上的 java 为 1.8 不可用）
export ANDROID_HOME="<ANDROID_SDK_ROOT>"          # 需含 platforms;android-34 与 build-tools;34.0.0
export PATH="<GRADLE_HOME>/bin:$PATH"
```

补齐 SDK 组件（如缺失）：

```bash
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager.bat" --install "platforms;android-34" "build-tools;34.0.0"
```

**注意**：`local.properties` 已写入 `sdk.dir`（且被 `.gitignore` 忽略）。若换机需重新生成。

---

## 2. 原生核心构建

```bash
cd <repo>/platforms/android
gradle --no-daemon clean assembleDebug assembleRelease collectArtifacts
```

产物落在 `platforms/android/artifacts/`：

- `core-debug.aar`
- `core-release.aar`

校验产物：

```bash
sha256sum artifacts/*.aar
```

> **非 ASCII 路径说明**：本工程位于 `<repo>\...`。`build.gradle.kts` 会自动把
> `layout.buildDirectory` 重定向到 ASCII 路径（默认 `C:\Users\<user>\depmap-android-build`，
> 可用 `DEPMAP_ANDROID_BUILD_ROOT` 覆盖），原因见 `docs/ANDROID_BUILD_REPORT.md` §5。
> `collectArtifacts` 负责把 AAR 收回仓库内。

---

## 3. 原生核心单元测试

```bash
cd <repo>/platforms/android
gradle --no-daemon testDebugUnitTest testReleaseUnitTest --rerun-tasks --no-build-cache
```

### ⚠️ 强制约束

**必须**带 `--rerun-tasks --no-build-cache`。

原因：`gradle.properties` 中 `org.gradle.caching=true`，build cache key 基于**输入内容哈希**，
与工程路径无关。因此「在 ASCII 路径下跑过一次」的测试结果会被中文路径的构建**直接复用**，
输出 `Task :core:testDebugUnitTest FROM-CACHE` 并报 `BUILD SUCCESSFUL` —— 这**不是真实执行**，
不能作为测试通过的证据。

判定标准：日志中不得出现 `FROM-CACHE` 出现在 `:core:testDebugUnitTest` 上，
且应显示 `N actionable tasks: N executed`。

测试报告位置：`<buildDirectory>/core/test-results/testDebugUnitTest/*.xml`。

---

## 4. 产品 APK / AAB（当前 BLOCKED — B10）

产品包必须由 uni-app x 工程经 HBuilderX 产出，**不能**由 `platforms/android` 产出。

### 4.1 已知事实

- HBuilderX 已安装：`5.24.2026081301`，位于 `<DEPMAP_TOOLS_HOME>\HBuilderX`；
- 工程导入成功：`cli.exe project open --path "<repo>\app"` → 「项目导入成功」；
- **其 CLI 不含任何 build / publish / run 命令**。全量命令集为：
  `help` / `open file` / `project open|close|list` / `report-bug` / `uni-agent` / `user login|info|logout` / `version`。

→ 纯命令行无法触发 uni-app x 编译。

### 4.2 解除路径（择一）

1. **GUI 打包**：在 HBuilderX 中打开工程 → 发行 → 原生App-云打包 / 本地打包；
2. **CLI 云打包**：若 DCloud 提供独立 CLI 打包工具（`cli publish` 类），安装后接入；
3. **本地离线打包**：使用 DCloud 的 Android 离线 SDK（`Android-SDK@x.x.x`），
   将 `app/` 编译产物（`unpackage/dist/build/app-android`）导入离线 SDK 工程出包。

### 4.3 打包前必须确认

- `app/manifest.json` 的 `appid` 当前为 `__UNI__DEPMAP01`（**占位值**），正式打包前须替换为 DCloud 平台申请的真实 appid；
- `app/static/icons/*` 与 `app/static/splash/*` 已补齐（9 个 PNG，见 `app/static/`）；
- `minSdkVersion=26` / `targetSdkVersion=34` / `abiFilters=["arm64-v8a"]` 与商店要求一致；
- 权限声明为空（`permissions: []`），仅原生核心库声明 `USE_BIOMETRIC`，见 `docs/ANDROID_PERMISSION_AUDIT.md`。

---

## 5. 签名（当前 BLOCKED — B4）

```bash
# 生成 release keystore（由密钥持有者执行，禁止把私钥提交进 Git）
keytool -genkeypair -v -keystore depmap-release.jks -alias depmap \
  -keyalg RSA -keysize 4096 -validity 10000
```

- keystore 文件与口令**必须存放在仓库之外**，通过 CI secret 或本地安全位置注入；
- **禁止**把 `.jks` / `.keystore` / 口令写入 Git 历史（`.gitignore` 已覆盖常见后缀，仍需人工确认）；
- 签名配置完成后，`SIGNING_READY` 才可置为 PASS。

---

## 6. 安装与设备验证（当前 BLOCKED）

```bash
# 统一使用 SDK 自带 adb（PATH 上的 1.0.32 与 SDK 1.0.41 冲突）
"$ANDROID_HOME/platform-tools/adb.exe" devices -l
"$ANDROID_HOME/platform-tools/adb.exe" install -r <app>.apk
```

当前实测：`adb devices -l` 为空；无 AVD；无 system-image。→ `INSTALL_READY` / `DEVICE_VERIFIED` 均 BLOCKED。

---

## 7. 发布前检查清单

- [ ] `gradle testDebugUnitTest testReleaseUnitTest --rerun-tasks --no-build-cache` 全绿，且无 `FROM-CACHE`
- [ ] AAR SHA-256 已记录并与交付一致
- [ ] `appid` 已替换为正式值
- [ ] `static/icons` 与 `static/splash` 全部存在且尺寸与 `manifest.json` 声明一致
- [ ] 权限声明复核（应为空或仅必要项）
- [ ] `allowBackup=false` / `usesCleartextTraffic=false` 未被改动
- [ ] release keystore 由安全位置注入，未进入 Git
- [ ] 隐私政策 URL 与 `NSCameraUsageDescription` / `NSFaceIDUsageDescription` 文案就位
- [ ] 商店素材（截图、描述、分级）就位

---

## 8. 回滚

- 原生核心：`git revert` 对应提交，重新 `assembleRelease` 并替换 AAR；
- 产品包：商店后台回滚到上一版本（需保留历史 AAB/APK）；
- **禁止**使用 `git reset --hard` / `git clean -fd` 回滚（项目永久规则）。

---

## 9. 常见故障

| 症状                                                                       | 根因                                             | 处置                                                         |
| -------------------------------------------------------------------------- | ------------------------------------------------ | ------------------------------------------------------------ |
| `Your project path contains non-ASCII characters`                          | AGP 默认拒绝非 ASCII 路径                        | 确认 `gradle.properties` 含 `android.overridePathCheck=true` |
| `contains AndroidX dependencies, but 'android.useAndroidX' is not enabled` | 缺 `gradle.properties`                           | 确认 `android.useAndroidX=true`                              |
| `ClassNotFoundException: ...DepmapContainerV1GoldenTest`                   | 非 ASCII 路径下 Gradle worker argfile 编码不一致 | 确认 `build.gradle.kts` 的 build 目录回退逻辑生效            |
| `Task :core:testDebugUnitTest FROM-CACHE` 但声称通过                       | build cache 跨路径复用                           | 加 `--rerun-tasks --no-build-cache` 重跑                     |
| `adb server version (32) doesn't match this client (41)`                   | PATH 上存在旧 adb                                | 使用 `$ANDROID_HOME/platform-tools/adb.exe`                  |
