# V0_1_2_RELEASE_TRAIN（v0.2.0 goal §6 收尾记录）

> 结论：`V0_1_2_RELEASE_TRAIN = CLOSED`（Android 下载 smoke = PASS；Desktop 下载 smoke = FAIL，
> 根因已定位为 v0.1.2 Windows 打包缺陷，v0.1.2 本体不再修改，修复随 v0.2.0 打包链路落地）。
> 日期：2026-09-25。本文件是 goal §6 的官方验收记录。

## 审计输入

- 既有仓库证据（WORK_STATUS.md 2026-09-24 晚注记）：Android 运行时 smoke 来自**本地产物**
  （install/launch/uninstall + 60/60 instrumentation **本地 APK**），**不是** GitHub Release attachment。
  因此 §6 的「GitHub Release 下载 → SHA256 match → install → launch → uninstall」证据缺失 -> 补齐。

## 执行（全部真实执行，非纸面）

1. `gh release download product-v0.1.2`（release `PDIG 0.1.2 Developer Preview`, Pre-release=true），
   7 个产物 attachment 全部 SHA256 **MATCH** Release SHA256SUMS.txt 与 PRODUCT_V0_1_2_RELEASE_MANIFEST.md
   （详见 `sha256-verify-log.txt`；SHA256SUMS.txt 自身不在清单内，自比对无意义）。
2. Android（API36 AVD `pdig36`，emulator 36.5.11，加固版 `scripts/android/run_avd_gate.ps1` 单命令会话）：
   - push + `pm install -r -d -t` = **Success**（INSTALLED=True）
   - `dumpsys package` → `versionName=0.1.2`
   - `monkey -p com.pdig.app.preview` → **Events injected: 1**，进程 PID 存活（另一次独立会话 PID=4010），
     `am start -W` Status: ok / LaunchState: COLD；截图 `android-download-smoke-home.png`（1080×2340）
   - `pm uninstall` = **Success**，`pm list packages` 无 `pdig`（包与数据移除）
   - 崩溃缓冲为空：**0 crash**
   - 证据：`android-download-smoke-evidence.txt`
   - 结论：**V0_1_2_GITHUB_ANDROID_DOWNLOAD_SMOKE = PASS**
3. Desktop（下载的 portable zip + setup.exe）：
   - SETUP（NSIS 静默安装 `/S /D=...` 到 scratch）exit 0；portable zip Expand-Archive 正常
   - **启动失败**：`PDIG.exe` 进程存活但 8MB/CPU 0、无窗口；无 javaw/java 子进程；
     `tasklist /v` Window Title = N/A、MainWindowHandle=0
   - 根因（磁盘取证）：发布物的 jpackage app-image `runtime\bin` **缺少 `java.exe`/`javaw.exe`**
     （仅 java.dll + jvm.dll + 系统 DLL，共 90 个文件）；portable zip 399 条目中唯一 exe 是 `PDIG\PDIG.exe`；
     NSIS 安装树同样只有 PDIG.exe + Uninstall.exe。
     v3.0 基线核实：同机手工 `jlink --add-modules java.base,java.desktop` 正常产出 java.exe/javaw.exe，
     jpackage 本体在该 JDK（21.0.12.101）上确定性故障（连最小 hello app-image 也报
     "Cannot access file with path exceeding 32000 characters"，`-v` 模式挂起）——
     即 v0.1.2 构建当天 jpackage 产出了**缺少 JVM launcher 的残缺 image** 而未被发现
     （当时的"GUI 启动 20s 无崩溃"取证对象是本地构建镜像，不是发布 zip）。
   - 结论：**V0_1_2_GITHUB_DESKTOP_DOWNLOAD_SMOKE = FAIL（发布物缺陷，非测试问题）**

## 处置

- 不再修改 v0.1.2（tag/Release/产物冻结）。
- 根因修复随 v0.2.0：`scripts/release/build-desktop-package.ps1` 重写为
  「jlink（本机已验证可用）+ 手工 app-image + 显式 launcher + 打包后验证
  （runtime\bin\java.exe / javaw.exe 必须存在，缺失即构建失败）」，
  并在 v0.2.0 的 Release 下载后二次 smoke 中做等效验收（K5）。
- 目录 `docs/release-evidence/v0_1_2_download_smoke/` 为本验收的入库证据。