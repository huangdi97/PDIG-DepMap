# LOCAL_ENV_INVENTORY.md

> PDIG v0.3.1 UI/UX Refinement · 2026-09-28 · 本机环境盘点（只查不改，spec §55）

## 结论概览

| 项                                | 状态                                                                                 | 证据                                                                      |
| --------------------------------- | ------------------------------------------------------------------------------------ | ------------------------------------------------------------------------- |
| Windows 11 / 26200                | PRESENT                                                                              | `[System.Environment]::OSVersion` → Microsoft Windows NT 10.0.26200       |
| CPU / RAM                         | Intel i5-12400F / 31.8 GB                                                            | Win32_Processor / TotalPhysicalMemory                                     |
| git                               | 2.55.0.windows.5                                                                     | `git --version`                                                           |
| node / npm                        | v22.15.0 / 11.3.0                                                                    | `node --version` `npm --version`                                          |
| Java / JDK                        | OpenJDK 21.0.12.1 LTS                                                                | `java -version`                                                           |
| Gradle                            | 8.9（wrapper 锁定，首次构建经 proxy 下载）                                           | `android/gradle/wrapper/gradle-wrapper.properties`；`android/.gradle/8.9` |
| Android SDK                       | `D:\Code\Android\SDK`（ANDROID_HOME）                                                | env                                                                       |
| platform-tools / adb              | 1.0.41                                                                               | `adb version`                                                             |
| system-images                     | android-34 / 35 / 36（google_apis x86_64；android-35 还有 playstore）                | SDK/system-images                                                         |
| AVD                               | `main`（pixel_7, android-36, 1080×2400, 420dpi, x86_64）+ `zhishen_rc`（android-35） | `emulator -list-avds`；`D:\avdhome`（ANDROID_AVD_HOME）                   |
| 模拟器加速                        | WHPX 可用                                                                            | `emulator-check accel` → 0/WHPX usable                                    |
| DevEco Studio / Harmony SDK / hdc | ABSENT                                                                               | 搜索 `C:\Program Files\Huawei` 等均不存在；`Get-Command hdc` 为空         |
| 本机浏览器                        | Chrome + Edge                                                                        | Program Files 探测                                                        |
| iOS / Xcode                       | ABSENT（Windows 宿主）                                                               | 平台现实：iOS 运行验证走既有 GitHub Actions macOS workflow（E-8）         |
| Core 测试工具链                   | `core/` npm scripts 完整，node_modules 已存在                                        | `core/package.json`                                                       |

## 与 spec §55 的对照

- OS / CPU / RAM ✓
- git / node / npm ✓
- Java / JDK ✓（21 LTS，匹配 desktop jvmToolchain(21)）
- Gradle ✓（wrapper 8.9，与 android/.gradle/8.9、desktop 声明一致；无全局 gradle —— 一律用 `android\gradlew.bat`）
- Android SDK / ANDROID_HOME / adb 路径与版本 ✓（`D:\Code\Android\SDK\platform-tools\adb.exe`）
- emulator 路径 / 已装 system images / 已有 AVD ✓（见上）
- 已连接设备：无真机；测试用 `emulator-5554`（AVD `main`）
- DevEco / Harmony SDK / hdc：**未安装** → `HARMONY_RUNTIME_EXTERNAL_GATE`（与 BLOCKERS E-9 一致：无模拟器镜像/华为账号）
- 本地浏览器 ✓（Chrome/Edge 用于 HTML 预览）
- 现有项目 toolchains：desktop/settings.gradle.kts 复用 android/:core/:conformance/:repos；ASCII 构建根 `%USERPROFILE%\pdig-desktop-build` / `%USERPROFILE%\pdig-build`

## 环境性注记（诚实记录）

1. 网络：本机直连 `github.com:443` 失败；git 配置了本地代理 `http://127.0.0.1:10808`。
   Gradle/JVM 不读 git 配置，首次构建通过运行时 `_JAVA_OPTIONS` 代理参数完成 distribution 与依赖解析（见 LOCAL_DOWNLOAD_AUDIT.md）。
2. AVD 稳定性：`emulator` 进程在宿主会话结束后随之终止；截图取证均为「同会话 启动→boot→测试→拉取」一次性完成。
3. 桌面 `--keys`（Robot 聚焦注入）此前受会话窗口焦点限制（BLOCKERS 环境注记）；本轮视觉取证以离屏 `--profiles`（ImageComposeScene）为准。
4. 无真机 / 无 Harmony 模拟器 / 无 macOS：Android 真机维度与本轮 Android 截图之外的平台运行维度以外部门禁如实记录。

## 权限边界确认

- 外部工具均为只读访问（SDK / JDK / Gradle 缓存 / AVD 状态仅由工具自身 runtime 写）。
- 本轮未安装/更新任何全局工具；未修改任何已装 SDK。
