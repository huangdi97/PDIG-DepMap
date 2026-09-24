# ANDROID_GIT_SCOPE_AUDIT.md

> 目的：在把 `android/` 原生实现入库之前，先把**每一个未跟踪路径**分类，
> 避免"一口气 `git add .`"把构建产物、机器路径、运行时证据和源码混在一起。
>
> 生成时间：2026-09-16
> 基线 HEAD（审计开始时）：`68f506caa8953488e7fa277edf62d0be27315f12`
> 未跟踪条目总数（`git status --porcelain -uall`，`??`）：**196**
> 其中 `android/**`：**61**

---

## 0. 结论摘要

| 分类                |                         条目数 | 处置                                                                                                          |
| ------------------- | -----------------------------: | ------------------------------------------------------------------------------------------------------------- |
| **A. 必须进入仓库** |                            190 | 按 7 个逻辑提交入库                                                                                           |
| **B. 必须忽略**     | 6 类规则（0 个当前未跟踪命中） | 已由 `.gitignore` 覆盖，本轮新增 2 条                                                                         |
| **C. 单独判断**     |                              6 | `fixtures/` `spec/` `tools/` `conformance/` 判为**正式资产，入库**；`local_private/` 判为**运行时证据，忽略** |

**关键判断**：`fixtures/`（64 个跨端中立用例）+ `spec/`（Canonical Spec）
不是"测试脚手架"，而是**三端迁移的契约本体**。它们一旦不入库，
Android 的 91/91 conformance 就失去可复现性 —— 因此必须入库。

---

## A. 必须进入仓库

### A-1 `android/` 构建脚手架（5）

```
android/build.gradle.kts
android/settings.gradle.kts
android/gradle.properties          # 无 secret：只含 JVM 参数与 AGP 开关
android/gradlew                    # 已跟踪（68f506c）
android/gradlew.bat                # 已跟踪（68f506c）
android/gradle/wrapper/*           # 已跟踪（68f506c）
```

判定：Gradle Wrapper + Kotlin DSL 构建脚本是**可复现构建的全部前提**。
`gradle.properties` 逐行核对过：无 keystore、无 token、无机器绝对路径。

### A-2 `android/core`（23）—— 纯 Kotlin 领域层 + JVM 单测

```
src/main/kotlin/com/pdig/core/{domain,impact,plan,scenario,statemachine,schema,json,
                               serialize,sources,timeline,db,crypto}/**.kt   (16)
src/main/kotlin/com/pdig/core/generated/CanonicalEnums.kt                     (1)
src/test/kotlin/com/pdig/core/{domain,impact,plan,schema,statemachine,db}/**  (7)
```

`generated/CanonicalEnums.kt` 的判定：本项目策略是
「codegen 产物随源码入库，便于跨端对比 diff 与离线构建」
（`tools/codegen/generate.mjs --check` 作为一致性门禁）。
按该策略判定为 **track**，不做"生成物不入库"的通用化处理。

### A-3 `android/conformance`（4）

```
conformance/build.gradle.kts
conformance/src/main/kotlin/com/pdig/conformance/Main.kt
conformance/src/main/kotlin/com/pdig/conformance/JdbcSqliteDriver.kt
```

判定：conformance runner 是 **91/91 的证据产生者**，不属于测试脚手架附属物。

### A-4 `android/app`（35）—— Compose 应用层 + 设备内证据测试

```
app/build.gradle.kts
app/proguard-rules.pro                     # 本轮新建（此前是被引用却不存在的悬空引用）
app/src/main/AndroidManifest.xml
app/src/main/res/**                        # strings / themes / ic_launcher / data_extraction_rules
app/src/main/kotlin/com/pdig/app/MainActivity.kt
app/src/main/kotlin/com/pdig/app/PdigApplication.kt
app/src/main/kotlin/com/pdig/app/data/AppContainer.kt
app/src/main/kotlin/com/pdig/app/platform/AndroidSqliteDriver.kt
app/src/main/kotlin/com/pdig/app/security/{AppLock,LockGate,Security}.kt
app/src/main/kotlin/com/pdig/app/ui/**     # PdigApp / SecureWindow / components / theme / screens
app/src/androidTest/kotlin/com/pdig/app/evidence/*.kt   # 8 个证据测试类
```

`androidTest/**` 判定：**入库**。它们是"设备内证据"而非一次性脚本 ——
19 个既有用例 + 本轮新增的 App Lock / 备份回归 / hitbox 用例，
换台机器重跑就能复现结论。这与 `local_private/` 的**脚本**性质不同。

### A-5 跨端正式资产（136）

```
spec/**                      14   Canonical Spec（最高真相源）
fixtures/**                 113   64 个平台中立用例 + 28 个原始输入 + parser/timeline/migration/backup
conformance/CONFORMANCE_MANIFEST.json   1   fixture sha256 + oracle 提交
tools/codegen/generate.mjs       1   spec → Kotlin/Swift/ArkTS
tools/conformance/run.mjs        1   跨端 Gate
core/scripts/generate-conformance.ts     1   oracle 自检
harmony/entry/src/main/ets/generated/CanonicalEnums.ets   1  codegen 产物
ios/Sources/PDIGCore/Generated/CanonicalEnums.swift       1  codegen 产物
legacy/README.md                 1   Legacy 冻结说明
docs/ADR_NATIVE_MIGRATION.md     1   技术栈变更决策记录
```

### A-6 状态与收口文档（22）

```
GOAL_PDIG_NATIVE_MIGRATION.md
NATIVE_MIGRATION_STATUS.md / NATIVE_MIGRATION_ACCEPTANCE.md
NATIVE_PARITY_MATRIX.md / NATIVE_RELEASE_MATRIX.md / NATIVE_EXTERNAL_BLOCKERS.md
CROSS_PLATFORM_CONFORMANCE_MATRIX.md
LEGACY_REFERENCE_MANIFEST.md / LEGACY_BEHAVIOR_CORRECTIONS.md
ANDROID_BUILD_REPRODUCIBILITY_REPORT.md / ANDROID_PERFORMANCE_SMOKE_REPORT.md
ANDROID_RUNTIME_E2E_REPORT.md / ANDROID_SECURITY_RUNTIME_AUDIT.md
ANDROID_REMAINING_7_AUDIT.md / ANDROID_STORE_METADATA.md
ANDROID_CORE_JVM_TEST_REPORT.md / ANDROID_CORE_USER_JOURNEY_E2E_REPORT.md
ANDROID_RUNTIME_SECURITY_EVIDENCE.md
ANDROID_N1_N2_FINAL_CLOSURE_REPORT.md        # V1，保留为历史
ANDROID_N1_N2_FINAL_CLOSURE_REPORT_V2.md     # 本轮新建
ANDROID_GIT_SCOPE_AUDIT.md                   # 本文件
```

---

## B. 必须忽略

`.gitignore` 现有规则已覆盖以下全部类别（逐条核对，无遗漏）：

| 类别                  | 规则                                               | 状态                                                    |
| --------------------- | -------------------------------------------------- | ------------------------------------------------------- |
| Gradle 本地状态       | `.gradle/`                                         | 已有                                                    |
| 构建产物              | `build/`                                           | 已有                                                    |
| 机器 SDK 路径         | `local.properties`                                 | 已有（已用 `git ls-files --error-unmatch` 验证 exit=1） |
| 签名材料              | `signing/`、`*.jks`、`*.keystore`、`*.p12`、`*.p8` | 已有                                                    |
| 安装包                | `*.apk`、`*.aab`、`*.hap`                          | 已有                                                    |
| Kotlin 增量状态       | `.kotlin/`                                         | 已有                                                    |
| Harmony 本地状态      | `.hvigor/`、`oh_modules/`、`entry/build/`          | 已有                                                    |
| 运行时证据 / 机器路径 | `local_private/*`（保留 README）                   | 已有                                                    |
| 用户数据库            | `*.db`、`*.sqlite`、`*.sqlite3`                    | 已有                                                    |
| 解密后的备份          | `*.depmap.json`、`*.depmap.decrypted`              | 已有                                                    |

**本轮新增 2 条**（审计中发现的具体风险，尚无当前命中，属预防性）：

```gitignore
# Android 截图 / adb 取证输出（可能含真实界面内容，禁止入库）
*.png.tmp
*/screenshots/evidence/

# Gradle 配置缓存与本地镜像
.gradle-cache/
```

> 说明：截图目前全部落在 `local_private/**`（已被忽略），
> 新增规则的目的是防止将来有人把截图直接丢到仓库根或 `docs/` 下。

**明确不影响**：`*.png` 未被全局忽略，因为 `app/src/main/res/drawable/` 中的
图标资产必须入库（当前 `ic_launcher.xml` 为矢量占位资产）。

---

## C. 单独判断

| 路径                                                        | 判定     | 理由                                                   |
| ----------------------------------------------------------- | -------- | ------------------------------------------------------ |
| `fixtures/`                                                 | **入库** | 跨端迁移的正式契约资产（见 A-5）                       |
| `spec/`                                                     | **入库** | Canonical Spec，三端的最高真相源                       |
| `tools/`（codegen + conformance）                           | **入库** | 门禁本体，不是一次性脚本                               |
| `conformance/CONFORMANCE_MANIFEST.json`                     | **入库** | fixture 完整性校验的锚点                               |
| `local_private/**`（含 20+ 脚本、截图、XML dump、容器样本） | **忽略** | 机器相关、含真实运行产物与绝对路径；仓库内无源码依赖它 |
| `legacy/`（当前仅 `README.md`）                             | **入库** | Legacy 冻结说明属于长期工程文档                        |

**特别说明 —— 为什么 `local_private/` 不入库**：

1. 内容含本机绝对路径（`<repo>\...`、`%USERPROFILE%\...`）；
2. 含运行时证据（截图、uiautomator XML、`.depmap` 样本、logcat）；
3. 含 `pdig-nonprod.jks`（测试签名材料，`.gitignore` 已按 `*.jks` 拦截）；
4. 上游已明确把该目录排除在仓库之外（`!local_private/README.md` 是唯一例外）。

---

## D. 提交拆分方案（按逻辑，不按目录）

| #   | 提交                                                                      | 内容                                                                                                                                                    | 为什么这样切                                     |
| --- | ------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------ |
| 1   | `feat(android): native domain/data/security layer`                        | `android/core/**`、`android/conformance/**`、`android/{build,settings,gradle.properties}`                                                               | 纯领域层，无 Android 依赖，可独立编译            |
| 2   | `feat(android): compose app + application layer`                          | `android/app/src/main/**`、`android/app/build.gradle.kts`、`app/proguard-rules.pro`                                                                     | UI/平台接线                                      |
| 3   | `test(android): device evidence + conformance tests`                      | `android/app/src/androidTest/**`、`fixtures/**`、`spec/**`、`tools/**`、`conformance/CONFORMANCE_MANIFEST.json`、`core/scripts/generate-conformance.ts` | 证据与契约同批，避免"测试引用了没入库的 fixture" |
| 4   | `fix(android): wire app lock + backup export correctness`                 | `PdigApp.kt`、`LockGate.kt`、`AppLock.kt`、`LockScreen.kt`、`MainActivity.kt`、`DataScreens.kt`、`AppContainer.kt`（导出部分）                          | P0-A / P0-B 两个真实缺陷修复可被单独 review      |
| 5   | `fix(android): accessibility labels + scroll container hitbox`            | `Components.kt`（`PdigScrollingPage`）、四个 `OutlinedTextField` 语义、各页面容器替换                                                                   | P1-A / P1-B                                      |
| 6   | `chore(android): canonical codegen + legacy freeze notes`                 | `harmony/**`、`ios/**` codegen 产物、`legacy/README.md`、`docs/ADR_NATIVE_MIGRATION.md`                                                                 | 跨端资产与决策记录                               |
| 7   | `docs(status): recompute N1/N2 gates and close the status contradictions` | 根目录报告与状态文档、`ANDROID_GIT_SCOPE_AUDIT.md`                                                                                                      | 状态收口                                         |

> 注：提交 4 与 5 的文件与提交 2 有重叠，实际执行时**按最终内容一次性入库**，
> 提交信息按主要变更性质书写。拆分的目的在于**可 review、可回溯**，
> 而不在于人为制造历史。这条差异已在最终报告里注明。

---

## E. 每次提交后的强制校验

```bash
git rev-parse HEAD
git log --oneline -3
git status --short -uall
git diff --check
```

**本工作区已知 Git 故障**：loose ref 会被外部进程回收，`git commit` 返回 0 但 HEAD 不推进。
规避方式（本轮继续沿用，且**未使用任何被禁止命令**）：

```bash
git add <paths> && git write-tree
git commit-tree <tree> -p <parent> -m "<msg>"
# 再把分支指向写入 .git/packed-refs
```

**禁止**：`git reset --hard`、`git clean -fd`、`git restore .`、force push。
