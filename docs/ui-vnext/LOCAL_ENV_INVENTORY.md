# LOCAL_ENV_INVENTORY.md — 本机环境盘点（Stage 0，只查不改）

> 2026-09-29 · feat/pdig-ui-vnext · 本机 = Windows 开发机（无 macOS / 无华为设备）

## 1. 结论概览

| 项 | 状态 | 说明 |
| --- | --- | --- |
| OS | Windows 11（NT 10.0.26200 级） | 本机唯一宿主 |
| git | 可用（当前分支 feat/pdig-ui-vnext） | `git branch --show-current` 确认 |
| node / npm | v22.15.0 / 11.3.0 | core/ 工具链（conformance / codegen --check / secret scan） |
| JDK | 21（Temurin LTS） | desktop jvmToolchain(21) 匹配；`gradlew :app:compileKotlin` 依赖 |
| Gradle | 8.9（android wrapper 驱动 desktop） | `android/gradlew.bat` 统一入口；无全局 gradle |
| Android SDK | `D:\Code\Android\SDK`（ANDROID_HOME） | system-images android-34/35/36 |
| adb | 37.0.0 | `adb version` 实测 |
| AVD | `main`（pixel_7, android-36）+ `zhishen_rc`（android-35） | 上轮遗留；本轮 desktop 离屏取证未依赖模拟器 |
| DevEco Studio / hdc | **ABSENT** | 无 `hdc`、无 DevEco；Harmony 只能 `hvigor` 构建（并行平台 fixer） |
| Compose Desktop | 1.6.11（Compose BOM 2024.09.02） | desktop 依赖声明 |
| SwiftUI / ArkUI | 系统内置（iOS/Harmony 平台能力） | 本机无 Swift/Xcode；iOS 走 macOS CI（外部门禁） |
| 浏览器 | Chrome + Edge 存在 | 本轮未使用 |

## 2. 与本轮任务的对应

| 工具 | 用途 | 实际使用 |
| --- | --- | --- |
| node v22.15.0 | `tools/codegen/generate.mjs --check`、`npm run check` | 已用：CODEGEN GATE PASS；core check 全绿（487 tests） |
| JDK 21 + gradlew 8.9 | desktop 编译 + VNextShotDriver 离屏渲染 | 已用：`:app:compileKotlin` BUILD SUCCESSFUL；90 帧 PNG + probe |
| JDK ImageIO（tools/ui-vnext/image-metrics/PdigImageMetrics.java） | 截图量化取证 | 已用：IMAGE_METRICS.json（90 帧） |
| adb 37.0.0 / AVD | Android runtime 取证 | 本轮未用（Android 证据由并行 fixer 产出） |
| hvigor（harmony） | HAP 构建 | 并行 fixer 产出 |
| Swift / Xcode | iOS 构建 | 本机 ABSENT → macOS CI（外部门禁） |

## 3. 限制与诚实标注

- **无 macOS/Xcode**：iOS 构建/截图/XCUITest 只能走既有 GitHub Actions macOS workflow（E-8 类外部门禁）。
- **无 hdc/DevEco/华为账号**：Harmony runtime 视觉需设备；本机只可能做 hvigor 构建。
- **离屏渲染 ≠ 真实窗口**：desktop 90 帧由 `ImageComposeScene` 离屏渲染（无窗口 focus 语义）；
  真实窗口键盘焦点 / OS 级焦点环需 runtime 验证（NEEDS_RUNTIME_VERIFICATION，见 ACCESSIBILITY_AUDIT.md）。

## 4. 参考

- 上一轮盘点：docs/uiux/LOCAL_ENV_INVENTORY.md（2026-09-28，feat/pdig-uiux-refinement）
- 本轮新增工具：tools/ui-vnext/image-metrics/（PdigImageMetrics.java，零新依赖）
