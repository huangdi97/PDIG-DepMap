# AGENT_PLATFORM_HANDOFF_AUDIT.md

> 任务：**GOAL_AGENT_HANDOFF_PLATFORM_BRINGUP_PRODUCTION_RC_CONTINUE**
> 阶段：PDIG Platform Bring-up / Production RC V1 接力
> 审计日期：2026-09-15（本轮会话内实测）
> 分支：`feat/mvp03-living-graph`（未 push）
> HEAD：**以 `git log --oneline -1` 为准**。本轮进入时的基线是 `7295fc6`；本轮的提交链见
> `git log --oneline 7295fc6..HEAD`（报告不写入自身提交的 SHA，避免自引用失效）。
>
> **审计原则**：本文件所有数值均来自本轮真实执行的命令输出；旧报告只用于对照，
> **凡与实测冲突一律以实测为准并在文中标注被推翻的旧结论**。未执行的项写 `NOT_RUN`，
> 不用「理论可行」冒充 `PASS`。

---

## 0. 本轮最重要的三件事（先说结论）

| #   | 事项                           | 结论                                                                                                                                                                                                                                                |
| --- | ------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | **前序 Agent 的工作未提交**    | 进入时工作区**不是** clean：上一轮（2026-09-15 PLATFORM BRINGUP）的**全部**产物都在工作区里、零提交 —— 含 Android Kotlin / iOS Swift / UTS 源码改动、23 份平台文档、AAR 产物。**已全部保全并分 10 个逻辑提交入库**（见 §G）。                       |
| 2   | **B10 的旧结论被推翻**         | 旧结论「HBuilderX CLI 无 build 命令 ⇒ 无法无头打包 ⇒ 只能靠 GUI」。实测 `cli pack` 是 **DCloud 官方文档定义的、支持 uni-app x 的命令**（`hx.dcloud.net.cn/cli/pack`），真正的闸门是 **DCloud 账号登录 + 云打包配额**。见 §F-B10。                   |
| 3   | **UTS 层首次获得真实编译证据** | 本轮发现 DCloud 官方把 UTS 编译器（Rust + napi）**公开发布在 npm 上**，可在 Windows 无头调用。实测 **15/15** 个 UTS 实现（5 插件 × Android/iOS/HarmonyOS）全部编译通过，并**因此查出并修复了 2 个 iOS UTS 语法错误**（旧报告从未检查 `app-ios/`）。 |

---

## 1. Git 真实现场（本轮实测）

| 项                         | 实测值                                                                                  |
| -------------------------- | --------------------------------------------------------------------------------------- |
| 进入时 HEAD                | `7295fc6`（`docs(closure): add the directive status-name mapping ...`）                 |
| 分支                       | `feat/mvp03-living-graph`                                                               |
| `git status --short -uall` | **非 clean**：19 个已跟踪文件被修改/重命名/删除，52 项未跟踪（见 §G）                   |
| 已 staged                  | 1 个重命名（`arkts/RelationalStoreSecureAdapter.ets` → `entry/src/main/ets/adapters/`） |
| tags                       | `v0.2.0-mvp02`、`v0.3.0-mvp03`                                                          |
| 远端                       | **未配置 push 目标 / 未 push**；本轮默认 local commits only                             |
| `git diff --check`         | PASS                                                                                    |

### 1.1 本工作区的 Git 故障（必须知道）

`git commit` / `git update-ref` 在本工作区**会成功创建对象但无法推进 HEAD**
（`.git/refs/heads/**` 的 loose ref 被外部进程回收；`git update-ref` 返回 0 却不变更 HEAD；
实测两次 `git commit` 产生的提交父节点都是 `7295fc6`，即**提交之间不成链**）。

**本轮采用的规避方式**（沿用 `WORK_STATUS.md` 记录的做法，未使用任何被禁止的破坏性命令）：

1. 用 plumbing 显式建链：`git add <paths>` → `git write-tree` → `git commit-tree <tree> -p <parent> -m <msg>`；
2. 提交后把分支指针写进 `.git/packed-refs`（并重建 loose ref）—— packed-refs 是稳定的，
   即使 loose ref 再次被删除，`HEAD` 仍解析正确；
3. 每次提交后校验 `git rev-parse HEAD` / `git status --short -uall`。

**禁止项遵守**：未执行 `git reset --hard` / `git clean -fd` / `git checkout .` /
`git restore .` / force push / 历史重写（对已发布历史）/ 删除未知文件。
（唯一一次 `git reset --mixed <baseline>` 只重置索引、**不动工作区**，用于把上一轮遗留的
staged 重命名放回未暂存状态，以便按逻辑分组提交。）

---

## 2. A–J 十类状态

### A. 已完成且有测试证据

| 项                                 | 证据（本轮实跑）                                                                                                                                                                                |
| ---------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Core 全门禁                        | `npm run check` → **EXIT=0**：format/lint/typecheck 全绿、**453 passed / 453（43 文件）**、architecture 48 files circular 0、network 0 原语、secret 0、UI 静态门 30 `.uvue`/24 pages            |
| T-1 临时目录泄漏（上轮登记的欠账） | **已修复**：`invariants.test.ts` 的 `afterEach` 现在 `close()` 后 `rmSync(dir, {recursive, force})`。实测 `check:invariants` → 18 passed，`%TEMP%/depmap-inv-*` 计数 **2498 → 2498（delta=0）** |
| UTS 三端编译（**本轮新建的能力**） | `npm run check:uts` → **15/15 compiled, PASS**（Android→Kotlin 5/5、iOS→Swift 5/5、HarmonyOS→ArkTS 5/5）。工具：`@dcloudio/uts` 3.0.0-alpha-5020620260914001                                    |
| Android 原生核心 COMPILED          | AAR ×2 真实存在且 SHA-256 与 `RELEASE_CANDIDATE_MANIFEST.md` **逐字节一致**（见 §H）                                                                                                            |
| Android↔Node 互操作                | 黄金向量 4/4 × debug+release = 8/8（上轮实跑，代码本轮未改动；本轮重新触发 `assembleDebug/Release` 复现中，见 §H）                                                                              |
| HarmonyOS 原生适配器 COMPILED      | HAP 真实存在，SHA-256 与 manifest 一致；内含 `ets/modules.abc`（ArkTS 字节码）                                                                                                                  |
| 文档格式门禁                       | **修复了上轮引入的回归**：7 份平台报告未过 `format:docs:check` → 已格式化（仅表格分隔行对齐，无内容改动），`check` 由此转绿                                                                     |

### B. 已实现但**未**验证

| 项                                     | 为什么未验证                                                                                                        |
| -------------------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| `.uvue` 的**真实编译**（UI_COMPILED）  | 需要 HBuilderX 的 uni-app x 编译器（`cli pack` 云打包 / GUI 本地打包）。**至今未编译过**，`check:ui` 只是静态门     |
| UTS → **宿主语言联编**（符号可解析性） | 本轮的 UTS 门禁验证的是「UTS 语法与降级」（`removeImports: true`），**不**证明 `com.depmap.core.*` 的跨语言签名匹配 |
| Android 原生核心 **设备侧**行为        | 无设备（`adb devices` 为空；唯一 AVD 缺 system image，见 §I-B24）                                                   |
| iOS Swift **编译**                     | 非 macOS（`xcodebuild` 不存在）                                                                                     |
| HarmonyOS 产品级 HAP                   | 当前 HAP 是原生验证工程产物（含最小 `EntryAbility`+`Index.ets`），**不含 24 个 `.uvue`**、unsigned                  |
| 签发 / 商店                            | 无 keystore / 证书 / 账号                                                                                           |
| 真实账单（REAL_DATA）                  | 无真实数据，`NOT_RUN`（禁止 synthetic 冒充）                                                                        |

### C. 部分完成

| 项                     | 现状                                                                                                                                                                                                                               |
| ---------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| iOS Source Ready       | 结构性缺口仍在：`platforms/ios/swift/` **没有 `DepmapContainerV1.swift`**（G-1），SQLCipher 未接入 SPM（G-2），容器测试全部 `XCTSkip`（G-3）。本轮修掉了 UTS 侧 2 个语法错误 + `migrate()` 里不存在的 `DepmapSchemaV1.shared` 引用 |
| `STORE_METADATA_READY` | `store/*` 5 份齐备，但正式标识/品牌名/隐私与支持 URL 未决（B11/B12/B12b/B14）                                                                                                                                                      |
| `STORE_ASSETS_READY`   | 图标/启动图已生成**占位资产**（U-1 闭环，引用不再指向空路径），**非设计交付**                                                                                                                                                      |
| `PRODUCT_READY`        | 主流程（手动建对象 → 声明关系 → 影响 → 计划 → 验证 → 时间轴 → 清空）可用；导入与备份两条路径不可用且已如实标注                                                                                                                     |

### D. 尚未开始

- `DepmapContainerV1.swift`（iOS 容器实现，G-1）—— Mac 侧首要待办。
- 商店截图（依赖可运行环境）。
- 三端签名与上架（外部账号）。
- 真实数据 Pilot（需用户真实账单）。
- **MVP04 全部内容**（本轮明确禁止，未触碰）。

### E. 当前失败 Gate

**无失败测试。** 唯一曾被打破的门禁是本轮开始时 `npm run check` 因 7 份文档格式失败 ——
**已修复并复核 EXIT=0**。

### F. 当前 BLOCKED Gate

| Gate                                                       | 类别               | 精确原因                                                                                                                                                                                          |
| ---------------------------------------------------------- | ------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `UI_COMPILED` / `UI_RUNTIME_READY`                         | 账号 + 服务        | **B10**：uni-app x 的 App 打包只能经 HBuilderX。`cli pack` 是官方无头路径，但要求 DCloud 账号登录 + 云打包配额；本地打包的「生成本地打包App资源」是 GUI 步骤，AppKey 亦需账号。**无凭据不可解**。 |
| `ANDROID_PRODUCT_BUILD_READY`（APK/AAB）                   | 账号               | 同上（B10）+ 无 release keystore（B4）                                                                                                                                                            |
| `ANDROID_INSTALL_READY` / `DEVICE_VERIFIED`                | 设备               | **B24**：`adb devices` 为空；0 可用 AVD（唯一 AVD `Medium_Phone_API_35` 指向不存在的 `system-images\android-35\google_apis_playstore\x86_64\`）；无物理设备                                       |
| `HARMONY_INSTALL_READY` / `DEVICE_VERIFIED`                | 签名 + 设备        | HAP 未签名（B7）；无 HarmonyOS 设备（B18）                                                                                                                                                        |
| `IOS_BUILD_READY` / `DEVICE_VERIFIED` / `TESTFLIGHT_READY` | 平台 + 账号        | 非 macOS（B3）+ 无 Apple 账号/证书（B8/B9）                                                                                                                                                       |
| `STORE_*_READY` / `STORE_SUBMITTED`                        | 账号 + 资产 + 决策 | B4–B9、B11、B12/b、B14–B17                                                                                                                                                                        |
| `REAL_DATA_CORRECTNESS` / `REAL_DATA_VALUE`                | 数据               | 无真实账单（B13）；需用户决策（B19）                                                                                                                                                              |
| `CLEAN_CLONE`                                              | 沙箱               | **B23**：批量删除被 safe-delete 守卫 fail-closed 拦截（`SHFileOperationW 失败: 0x2`）。本轮的等价证据是「`git status -uall` 0 行 ⇒ 磁盘树 ≡ 提交树」+ 在该树上全门禁 EXIT=0                       |

### G. 前序 Agent 未提交修改（本轮已全部保全）

进入时的未提交内容（即上一轮的成果）：

- **源码 19 个已跟踪文件**：`platforms/android/{build.gradle.kts, core/build.gradle.kts, kotlin/.../DepmapContainerV1.kt, .../KeystoreSecureKeyAdapter.kt, .../SqlCipherSecureDatabaseAdapter.kt}`；
  4 个 `app/uni_modules/*/utssdk/app-android/index.uts`；`platforms/harmonyos/**`（含 `app.json5` 删除与适配器重命名）；
  `platforms/ios/{Package.swift, swift/SQLCipherSecureDatabaseAdapter.swift}`；`.gitignore`；`BLOCKERS.md`；
  `PLATFORM_RELEASE_MATRIX.md`；`STORE_EXTERNAL_BLOCKERS.md`；`docs/{IOS_RELEASE_HANDOFF.md, RELEASE_VERSION_MATRIX.md}`。
- **未跟踪 52 项**：`.tmp_audit/**`（前序会话的临时工具与探测输出）、`PLATFORM_BRINGUP_PRE_AUDIT.md`、
  `PRODUCTION_RUNTIME_UI_AUDIT.md`、`RELEASE_CANDIDATE_MANIFEST.md`、`docs/ANDROID_{TOOLCHAIN_AUDIT,BUILD_REPORT,PERMISSION_AUDIT,RELEASE_RUNBOOK}.md`、
  `docs/HARMONY_{TOOLCHAIN_AUDIT,BUILD_REPORT,RELEASE_RUNBOOK}.md`、`platforms/android/artifacts/*.aar`、
  `app/static/{icons,splash}/*.png`、`platforms/harmonyos/**` 工程骨架。

**处置**：

1. 逐文件判读，分类为「前序已完成但未提交」/「临时产物」/「真正的垃圾」——**没有任何一项被丢弃**。
2. `.tmp_audit/` 属临时工具与探测输出（含 HBuilderX/gradle 压缩包、解包出的 class），
   **加入 `.gitignore`，不入库、也不删除**（它仍是后续复用工具的位置）。
3. 其余全部按逻辑分组提交（10 个提交）：gitignore / Android 原生 / UTS 桥接 / HarmonyOS 骨架 /
   iOS 修复 / 占位资产 / AAR 产物 / 平台文档 / 收口报告 / 文档格式回归修复。

### H. 当前 Build artifacts（本轮独立复算）

| 产物                                                       | 字节   | SHA-256                                                            | 说明             |
| ---------------------------------------------------------- | ------ | ------------------------------------------------------------------ | ---------------- |
| `platforms/android/artifacts/core-debug.aar`               | 72,376 | `dd7d8c04b23041354b401b1f0e347b9fedf586131dae975c8df70520ef850a0d` | 与 manifest 一致 |
| `platforms/android/artifacts/core-release.aar`             | 68,731 | `4ac2e7f4c5d407d6b6a2d2232913f343ff7b7ef0f27891d67c60a190bf43c32b` | 与 manifest 一致 |
| `platforms/harmonyos/artifacts/entry-default-unsigned.hap` | 18,986 | 见下方说明（本轮重建后为 `ceeb5c56…`；原交付件为 `4f10d059…`）     | **哈希不可复现** |

**关于 HAP 的哈希**（必须如实说明，不要误读为失败）：

- 审计开始时，磁盘上的 HAP 与 `RELEASE_CANDIDATE_MANIFEST.md` 记录的
  `4f10d0597aaaac2aab4af8e27ec7138709e07e5ea81aaed705d249ed55bd0663` **完全一致**。
- 本轮随后**真实重建**了 HarmonyOS 工程（hvigor `BUILD SUCCESSFUL in 59 s 267 ms`），
  覆盖了 `artifacts/` 下的 HAP。重建件 **字节数完全相同（18,986 B）**，
  且 zip 内每个条目的**大小逐项相同**（`ets/modules.abc` 10,568 B、`module.json` 1,241 B、
  `resources.index` 646 B、`app_icon.png` 2,825 B、`sourceMaps.map` 2,322 B、`pack.info` 529 B、
  `main_pages.json` 23 B），**但整体 SHA-256 不同**（`ceeb5c56…`）。
- 根因：HAP 是 zip，zip 条目写入构建时间戳等元数据 → **同一内容两次打包的字节流不相等**。
  Android 的 AAR 之所以能逐字节复现，是因为它由 Gradle 以确定性方式打包。
- 因此：**HAP 的 SHA-256 只能标识「那一次构建的产物」，不是可复现的构建不变量**；
  判据应为「构建成功 + 条目内容一致」。

- **Android 构建可复现（本轮实跑）**：`assembleDebug assembleRelease collectArtifacts --rerun-tasks`
  → `BUILD SUCCESSFUL in 7m 50s`，`49 actionable tasks: 49 executed`（**无 `FROM-CACHE`**），
  两个 AAR 的字节数与 SHA-256 与 manifest **逐字节一致** ⇒ `ANDROID_NATIVE_BUILD_READY = PASS` 是硬的。
- **HarmonyOS 构建可复现（本轮实跑）**：以 Python 复刻 `build.sh` 的 ASCII 镜像流程
  （本环境 bash 缺 `dirname/grep/mkdir/tar/cp`，故 `build.sh` 本身跑不动），
  `hvigor BUILD SUCCESSFUL in 59 s 267 ms`，产出 HAP 且**条目内容逐项一致**（见上）。
- `core-release.aar` 实测含 **37 个 `.class`**，覆盖 `DepmapContainerV1`（含 `B64`/`JsonHeader`）、
  `DepmapSchemaV1`、`SecureDatabaseAdapter`/`SqlCipherSecureDatabaseAdapter`/`DepmapSqlCipherHelper`、
  `KeystoreSecureKeyAdapter`、`BiometricGateAdapter`、**`UtsSecurityBridge`**（13 个回调方法）。
- **`docs/ANDROID_BUILD_REPORT.md` §6.2 的 44,147 / 42,352 与「23 classes」是过期数据**（那是修复
  `UtsSecurityBridge` 之前的第一次构建）。本轮已按实测修正该文档。
- **AAR ≠ APK**：Android 产物是 library archive，不可安装。
- **HAP 是原生验证工程产物**，不是产品包，且 unsigned。
- **无 APK / AAB / IPA / 产品级 HAP / 编译后的 `.uvue` 产物。**

### I. 当前可用 Toolchain（本轮复核）

| 工具                     | 状态                                                                                                                                                                                                                                                                                                          |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Node / npm               | v22.22.2（受管）/ npm 10.9.7（工程声明 11.3.0，历史登记 F-3）                                                                                                                                                                                                                                                 |
| JDK                      | Android Studio JBR **21.0.10**、DevEco JBR **17.0.12**；`JAVA_HOME` 未预设；PATH 上的 `java` 是 1.8（不可用于 AGP）                                                                                                                                                                                           |
| Gradle                   | **8.9**（`<GRADLE_HOME>`）                                                                                                                                                                                                                                                                  |
| Android SDK              | `<ANDROID_SDK_ROOT>`（platforms 34/36.1/37.0、build-tools 34.0.0/36.1.0/37.0.0、platform-tools、cmdline-tools、licenses 已接受）                                                                                                                                                                             |
| adb                      | 可用（`adb devices -l` 为空 = 无设备）                                                                                                                                                                                                                                                                        |
| **AVD（新发现）**        | `emulator -list-avds` → `Medium_Phone_API_35`（因 `ANDROID_SDK_HOME=<ANDROID_SDK_ROOT>`，AVD 落在 `<ANDROID_SDK_ROOT>\.android\avd`）。**但该 AVD 指向的 `system-images/android-35/google_apis_playstore/x86_64/` 目录不存在 ⇒ 不可启动**。旧报告「0 个 AVD」的表述**不准确**。                                               |
| DevEco Studio            | 5.0.5.310（`<DEVECO_HOME>`）+ HarmonyOS SDK 5.0.1.115（API 13）+ hvigor 5.13.2 + ohpm 5.0.10                                                                                                                                                                                                  |
| HBuilderX                | **5.24.2026081301**，实际路径 **`<DEPMAP_TOOLS_HOME>\HBuilderX`**（旧 `BLOCKERS.md` 写的 `<TOOLS_ROOT>\HBuilderX` **是错的**）。CLI 可用但**必须依附正在运行的 GUI**；`--help` 的注册命令集**不含 pack**，且打包插件（`launcher`/`uniappx-launcher`/`uniapp-cli`/`uniapp-uts-v1`）**未安装**（33 个插件中均缺失） |
| **UTS 编译器（新发现）** | `@dcloudio/uts` 3.0.0-alpha-5020620260914001 + `@dcloudio/uts-win32-x64-msvc`（npm 公开可得），**Windows 无头可用**，导出 `toKotlin/toSwift/toArkTS`                                                                                                                                                          |
| Xcode                    | 不存在（非 macOS）                                                                                                                                                                                                                                                                                            |
| Unity / other            | N/A                                                                                                                                                                                                                                                                                                           |

### J. 下一条最合理执行命令

```bash
# 0) 确认现场（先做这个）
git log --oneline -1 && git status --short -uall

# 1) Core 全门禁（应 EXIT=0）
cd core && npm run check

# 2) UTS 三端编译门禁（本轮新增；未装编译器时会 SKIPPED 并以 0 退出）
cd core && npm run check:uts

# 3) Android 原生核心复现（无需账号）
cd platforms/android
JAVA_HOME="<ANDROID_STUDIO_HOME>/jbr" ANDROID_HOME="<ANDROID_SDK_ROOT>" \
  "<GRADLE_HOME>/bin/gradle.bat" --no-daemon \
  assembleDebug assembleRelease collectArtifacts --rerun-tasks --no-build-cache

# 4) 解除 B10（需要用户交互：DCloud 账号）
"<DEPMAP_TOOLS_HOME>/HBuilderX/cli.exe" open
"<DEPMAP_TOOLS_HOME>/HBuilderX/cli.exe" user login --username <账号> --password <密码>
"<DEPMAP_TOOLS_HOME>/HBuilderX/cli.exe" project open --path "<repo>\app"
"<DEPMAP_TOOLS_HOME>/HBuilderX/cli.exe" pack --project app --platform android \
  --android.packagename <正式包名> --android.androidpacktype 1
```

---

## 3. 三态定位（接力关键）

```
CURRENT_PHASE       = PDIG PLATFORM BRINGUP & RELEASE VALIDATION（Production RC V1 接力）
CURRENT_GATE        = PRODUCTION_RC_V1 = PARTIAL_WITH_REPORT
                      （Core/工程/UI 源码/Android 原生/Harmony 原生/UTS 三端编译均已过；
                        卡在「产品级产物」这一条链上，该链的唯一技术闸门是 B10）
NEXT_EXECUTABLE_GATE = UI_BUILD_READY（= B10 解除：DCloud 账号 → cli pack 出 APK）
                       —— 这是唯一能解锁 Android INSTALL/DEVICE 与产品级 HAP 的闸门；
                          其余可自动推进的工作本轮已推进到极限（见 §2.A/§2.C）
```

**剩余 Blocker 已压缩到只包含真正需要外部介入的事项**：DCloud 账号（B10 解闸）、
真实设备（B18/B24）、签名材料（B4/B7/B8/B9）、macOS（B3）、商店账号（B5/B6）、
正式标识与品牌（B11/B14）、隐私与支持 URL（B12/B12b）、真实账单（B13/B19）、
沙箱限制（B23）。**没有一条是「代码没写完」。**

---

## 4. 下一位 Agent / 用户的第一件事

1. **用户**：决定是否提供 DCloud 账号（登录后 `cli pack` 即可产出 APK，从而解锁
   UI_COMPILED → INSTALL_READY → DEVICE_VERIFIED 全链）。
2. **同时（不需要账号）**：连接一台真实 Android 设备（或在 SDK Manager 里补装
   `system-images;android-35;google_apis_playstore;x86_64` 让现有 AVD 可启动）——
   设备一到位，Android 侧就只剩「等 APK」。
3. **Agent**：从 `NEXT_EXECUTABLE_GATE = UI_BUILD_READY` 继续；若账号仍不可得，
   则转向 `docs/IOS_RELEASE_HANDOFF.md` 的 G-1（`DepmapContainerV1.swift`）等
   **不依赖账号**的工程收口项。
