# PRODUCTION_RUNTIME_UI_AUDIT.md — UI / 前端运行时审计（PHASE 16）

> 日期：2026-09-15
> 范围：`app/**`（24 页 `.uvue`、5 个 `dp-*` 组件、2 个 UTS service、1 个 theme、5 个 uni_modules 插件）
> 结论：**`UI_RUNTIME_VERIFIED = BLOCKED（B10 + B18）`**。本文件区分三类内容：
> **（A）静态可判定项**（已实跑，结论可用）；**（B）编译器可判定但本机无编译器项**（已定位并修复，但标注未编译验证）；
> **（C）必须真机才能判定项**（NOT_RUN，不虚报）。

---

## 0. 一句话结论

**UI 从未被任何真实编译器编译过，也从未在任何真实设备上运行过。**
本轮把「必然会在首次编译时爆炸」的 4 类缺陷（R-1~R-4）定位并修复了 3 类，
第 4 类（原生库接入 uni-app x 构建）已给出明确解除动作，但**必须在 HBuilderX 环境完成**。

---

## 1. 为什么 `UI_RUNTIME_VERIFIED` 只能是 BLOCKED

| 前提                     | 实测                                                                                     |
| ------------------------ | ---------------------------------------------------------------------------------------- |
| HBuilderX 是否安装       | **是**，5.24.2026081301（便携版在 `<DEPMAP_TOOLS_HOME>\HBuilderX`）                      |
| 是否可无头触发打包       | **否**。`cli.exe` 无可用 build 子命令；云端/本地打包需 GUI 交互与开发者账号登录           |
| HBuilderX 是否内置 UTS 编译器 | **否**。全盘检索无 `uts-compiler` 包、无 `build.gradle` 模板、无离线打包 SDK（均需联网下载） |
| 是否有真实设备           | **否**。`adb devices` 空、0 AVD、无 system-image；无 HarmonyOS 设备                       |

因此：**没有编译器 ⇒ 没有运行时**。`check:ui` 的 PASS 是**静态门**（9 类机械校验 U1–U9），
它只能证明「代码里没有命中已知反模式」，**不能证明代码能编译，更不能证明能运行**。

---

## 2. （A）静态可判定项 —— 已实跑

| #    | 检查项                            | 方法                                                            | 结果                                     |
| ---- | --------------------------------- | --------------------------------------------------------------- | ---------------------------------------- |
| A-1  | `pages.json` 声明页面是否都存在   | 解析 24 条 `pages[]`，逐一 `fs.exists(app/<path>.uvue)`          | **24/24 解析成功，0 缺失**               |
| A-2  | tabBar 入口是否存在               | 4 条 `tabBar.list[]` 逐一校验                                    | **4/4 存在，0 缺失**                     |
| A-3  | 运行时跳转目标是否可解析          | 正则抽取 16 个 `'/pages/...'` 硬编码跳转目标                     | **16/16 解析成功，0 死链**               |
| A-4  | 导航 API 使用分布                 | 统计 `uni.navigateTo/redirectTo/reLaunch/navigateBack/switchTab` | navigateTo 17、reLaunch 4、redirectTo 1、navigateBack 1 |
| A-5  | Promise 拒绝是否被处理            | 逐页统计 `.then(` / `.catch(`                                    | 22 页使用 Promise；**1 页未处理拒绝**（见 R-5） |
| A-6  | `console.*` 残留                  | 扫描 `app/pages`、`app/components`、`app/services`               | **0 处**（仅 `dp-state.uvue:17` 注释中提及） |
| A-7  | 页面是否残留 `async`              | 逐页统计                                                        | 0 页使用 `async`；统一走 `.then/.catch` 风格，一致 |
| A-8  | 设计 token 一致性                 | `check:ui`（U1–U9）                                             | PASS（30 `.uvue`，24 pages，5 components，34 色） |

> A-3 的意义：导航死链是 uni-app x 上最常见的**运行时白屏**原因之一，静态可查，本项为零。

---

## 3. （B）编译器可判定项 —— 已定位并修复（**但 UTS 侧未编译验证**）

### R-1（P1，已修复）`depmap-privacy-screen` 的 Android 实现**文件被截断**

- 位置：`app/uni_modules/depmap-privacy-screen/utssdk/app-android/index.uts`
- 证据：文件仅 **224 字节 / 3 行**，第 3 行 `import { getCurrentActivity } from 'android.uts.sdk.modules.dep`
  **语句未闭合**（`od -c` 确认文件即在此处结束），且**没有导出 `setPrivacyScreen`**。
- 影响：`app/App.uvue:2` 在 `onLaunch` 中 `import { setPrivacyScreen } from './uni_modules/depmap-privacy-screen'`
  并调用 —— **首次编译必然失败，且启动路径不可用**。
- 修复：补全实现，Activity 取自
  `UTSAndroid.getUniActivity()`（依据 HBuilderX 内置类型定义
  `builtin-dts/uts-types/app-android/UTSAndroid.d.ts:407`，**权威非推断**）；
  Activity 为 null 时（冷启动早期预期行为）不抛异常、直接 resolve，避免打断 `onLaunch`。

### R-2（P1，已修复）`depmap-biometric` 的 Android 实现引用**不存在的模块与未定义的符号**

- 位置：`app/uni_modules/depmap-biometric/utssdk/app-android/index.uts`
- 证据：
  - `:3` `import { getBiometricGate } from './gate-holder'` —— `gate-holder.uts` **从未存在**
    （该插件目录下仅有 `interface.uts` 与 3 个平台 `index.uts`）。
  - `:11` `return BiometricHolder.gate` —— `BiometricHolder` **从未定义或导入**。
- 影响：首次编译必然失败。
- 修复：删除对不存在模块的依赖，改由 `UtsSecurityBridge.createBiometricGate(UTSAndroid.getUniActivity())`
  惰性构造；**不使用缓存**，避免把冷启动时取到的 null 固化成永久不可用。

### R-3（P1，已修复）UTS 侧调用 Kotlin `suspend fun` —— 桥接契约系统性不匹配

- 根因：UTS 在 Android 上编译为 Kotlin，但 **UTS 语言没有协程概念，也没有 `suspend` 语法**；
  suspend 方法在 JVM 上还会多出一个 `Continuation` 参数。
- 受影响调用点（全部为 `app/uni_modules/*/utssdk/app-android/index.uts`）：

| 插件               | 原写法                                                          | Kotlin 真实签名                                        | 问题                       |
| ------------------ | --------------------------------------------------------------- | ------------------------------------------------------ | -------------------------- |
| `depmap-biometric` | `gate.canAuthenticate()` / `gate.authenticate(reason)` 当 Promise | `suspend fun`（`BiometricGateAdapter.kt:23/28`）        | 不可调用                   |
| `depmap-secure-key`| `adapter.getOrCreateDatabaseKey(alias)` 等 4 处当 Promise       | `suspend fun`（`KeystoreSecureKeyAdapter.kt:43/51/59/61`） | 不可调用               |
| `depmap-secure-database` | `open(options, onSuccess, onError)` 等 6 处按回调式调用   | `suspend fun open(options)`（`:36/44/50/71/93/114`）    | **参数个数都不匹配**       |

- 修复：新增 Kotlin 侧回调式桥接
  **`platforms/android/kotlin/com/depmap/core/security/UtsSecurityBridge.kt`**
  （`object`，全部回调在主线程派发；gate 不可用时 fail-closed 返回 `false` / `"not_available"`）。
  - **已由 Gradle 真实编译验证**：`:core:compileDebugKotlin` → `BUILD SUCCESSFUL`；
    `UtsSecurityBridge` 相关 class 已确认进入 `core-release.aar`。
  - 三个 UTS 插件已改写为经桥接层调用。

### R-4（P2，未修复 —— 需 HBuilderX 环境）原生库未接入 uni-app x 构建

- 现象：5 个 UTS 插件的 Android 实现全部 `import ... from 'com.depmap.core.*'`，
  而这些类只存在于**独立的 Gradle 工程** `platforms/android/` 中。
- 问题：uni-app x 的 Android 构建**不会**自动包含该工程。要让 `import` 解析成功，
  必须把 `platforms/android/artifacts/core-*.aar` 放进插件的原生依赖路径
  （`utssdk/app-android/libs/` 或 `config.json` 的 dependencies 声明）。
- 为什么本轮不直接改：**依赖注入方式（`libs/` 放置 vs `config.json` 声明）与多插件去重
  只能在 uni-app x 构建中验证**，本机无编译器，猜测性改动会造成更大偏差。
- 解除动作（在 HBuilderX 环境执行）：
  1. 把 `core-release.aar` 放到 `app/uni_modules/depmap-secure-database/utssdk/app-android/libs/`；
  2. 其余 4 个插件如需同类，改为依赖已内置 AAR 的插件（uni-app x 不建议多插件重复携带同一 AAR）；
  3. 首次编译后按编译器报错微调 `config.json`。

### R-5（P3，登记未改）`backup.uvue` 的 Promise 拒绝未处理

- 位置：`app/pages/backup/backup.uvue`（`.then(` = 1，`.catch(` = 0）
- 影响：导出/恢复失败时无用户可见反馈（页面静默）。
- 未改原因：备份功能当前为「暂未接入」（B21），该 `.then` 分支在设备上不可达；
  在无法编译验证的前提下改动 UI 逻辑得不偿失。**登记为下一轮必办项。**

---

## 4. （C）必须真机才能判定项 —— NOT_RUN

| #    | 项目                                       | 状态                                    |
| ---- | ------------------------------------------ | --------------------------------------- |
| C-1  | 冷启动 → 解锁 → 首页（启动耗时、白屏）     | **NOT_RUN**（无设备）                   |
| C-2  | 24 页逐个打开 / 返回 / 横竖屏              | **NOT_RUN**                             |
| C-3  | SQLite / SQLCipher 在设备上的真实读写与迁移 | **NOT_RUN**（依赖 R-4 先解决）          |
| C-4  | BiometricPrompt 真实弹窗与取消路径          | **NOT_RUN**（依赖 R-4 + 设备）          |
| C-5  | 隐私屏 FLAG_SECURE 真实生效                | **NOT_RUN**                             |
| C-6  | 深色模式对比度 / 触控目标 44dp 实测         | **NOT_RUN**（静态 token 已 PASS）       |
| C-7  | 内存 / 帧率 / 10k 节点列表滚动             | **NOT_RUN**（Core 侧 perf 16 passed，不等于设备表现） |
| C-8  | 无障碍（字体缩放、屏幕阅读）               | **NOT_RUN**                             |

---

## 5. 与历史报告的口径差异（重要）

历史文档（如 `FINAL_UI_UX_REPORT.md`）中 UI 相关 PASS 均为**源码级 / 静态级**结论。
本轮不改变它们的成立范围，但明确补充：

- `UI_SOURCE_READY = PASS`（维持）
- `UI_STATIC_GATE = PASS`（维持）
- `UI_COMPILED = BLOCKED（B10）`（**新增状态名，此前从未被独立标注**）
- `UI_RUNTIME_VERIFIED = BLOCKED（B10 + B18）`（**新增状态名**）

---

## 6. 复现与验证指引

```bash
# A 类复查（静态，随时可跑）
python - <<'PY'
import json, os, re, glob
d = json.load(open('app/pages.json', encoding='utf-8'))
missing = [p['path'] for p in d['pages'] if not os.path.exists('app/' + p['path'] + '.uvue')]
print('pages missing:', missing)
PY

# B 类（R-3 桥接层）复查 —— 已真实编译验证
cd platforms/android
export JAVA_HOME="<ANDROID_STUDIO_HOME>\\jbr"
gradle :core:compileDebugKotlin --no-build-cache --console=plain   # BUILD SUCCESSFUL
```

**B 类中的 UTS 文件改动（R-1 / R-2 / R-3 的 UTS 侧）无法在本机验证** —— 需 HBuilderX 首次编译确认。
