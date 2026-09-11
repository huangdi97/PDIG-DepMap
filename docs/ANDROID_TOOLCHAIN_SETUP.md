# ANDROID_TOOLCHAIN_SETUP.md — Android 工具链（RC PHASE V）

> 检测结果（2026-09-12，本机 Windows）：

```
java    = 1.8.0_441 (JRE，位于 %PROGRAMFILES% (x86)\...\java8path)
javac   = 不存在
adb     = 1.0.32 (<ANDROID_SDK_ROOT>\adb.exe，仅 platform-tools)
sdkmanager = 不存在
gradle  = 不存在
```

**结论：Android 编译/测试不可执行 → COMPILED=NO / TESTED=NO（exact blocker B1）。**

## 可复现安装步骤（无需 GUI/管理员）

1. JDK 17（Temurin zip 解压即可，无需安装器）：
   `https://adoptium.net/temurin/releases/?version=17` → 下载 Windows x64 `.zip`
   → 解压到 `<TOOLS_ROOT>\jdk-17` → `JAVA_HOME=<TOOLS_ROOT>\jdk-17`，`PATH+=;%JAVA_HOME%\bin`
2. Android cmdline-tools：
   `https://developer.android.com/studio#command-line-tools-only` → zip 解压到
   `%ANDROID_HOME%\cmdline-tools\latest`
3. SDK 组件（接受 license 后自动）：
   ```
   sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
   ```
4. Gradle：项目自带 wrapper 更佳；当前仓库为轻量骨架，直接装 Gradle 8.7+ 或
   `gradle wrapper` 生成。

## 工具链就绪后立即执行（本仓库已备好）

```bash
cd platforms/android
gradle :core:test      # DepmapContainerV1GoldenTest —— golden 互操作（对齐 core/tests/golden）
gradle :core:assembleDebug
gradle :core:assembleRelease   # 不要求真实 signing（release 未经签名属预期）
```

通过后按实际结果更新 WORK_STATUS.md（COMPILED/TESTED）与 RC 报告。
manifest/permission audit 见 docs/PERMISSION_AUDIT.md（静态部分已完成）。
