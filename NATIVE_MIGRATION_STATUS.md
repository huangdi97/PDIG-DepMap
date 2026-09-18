# NATIVE_MIGRATION_STATUS.md

> 持续更新。格式：PHASE / ANDROID / HARMONY / IOS / CONFORMANCE / BLOCKERS / NEXT。
> 状态枚举：`PASS` `FAIL` `BLOCKED` `NOT_RUN` `PARTIAL_WITH_REPORT`

更新时间：2026-09-17（**Android 冻结 + Harmony N3 开工轮**）

> ## 本轮（2026-09-17 第二场）：Android 冻结 + 正式进入 Harmony N3
>
> 人工 Final Acceptance 已给出：`N1 = PASS`、`N2 = PARTIAL_WITH_REPORT`、
> `ANDROID_PRODUCTION_RELEASE_READY = BLOCKED_BY_PRODUCTION_SIGNING`、D-16 CLOSED。
>
> - **新增** `ANDROID_NATIVE_CORE_HANDOFF = PASS`（**不替代** N2，也不替代 release readiness；
>   只回答"Android 是否已可作为 Harmony N3 的 Native Reference"）→ `ANDROID_NATIVE_CORE_FREEZE.md`
> - Android 进入 **`CORE_FROZEN / MAINTENANCE_ONLY`**；不再为 parity 分数新增功能；剩余 11 格入 N2 Backlog
> - **Git 尾项已收口**：HEAD `6053f3c`；三端 codegen 产物与 `legacy/README.md` 入库；
>   `.pi/` 保持 untracked（gitignore 覆盖）
> - **正式进入 Harmony N3**：`harmony/` 由"仅 codegen"建成**可被 hvigor 真实构建并产出 HAP 的
>   Stage Model 工程**，`HARMONY_BUILD = PASS`；首个纯 ArkTS Domain（Relations）已编译并打包进 HAP
> - **两个真实 blocker**（2026-09-18 更新）：
>   `HARMONY_DEPMAP = BLOCKED_BY_NATIVE_VERIFICATION`（`cryptoFramework`/`HUKS` 无 Argon2 已证据级排除；
>   **NDK + PHC 参考实现 + NAPI 路径已打通**：主机侧 Golden Vector `MATCH=YES`、arm64 `.so` 编译通过；
>   仍缺设备上复验 —— 见 `HARMONY_ARGON2_FEASIBILITY.md`）、
>   `HARMONY_RUNTIME_E2E = RUNTIME_NOT_RUN`（无模拟器镜像，`hdc list targets = [Empty]`）
> - 按 stop condition：**未进入 iOS N4**
>
> 详见 `HARMONY_N3_BASELINE_AUDIT.md` / `HARMONY_N3_IMPLEMENTATION_STATUS.md` /
> `HARMONY_N3_CONFORMANCE_REPORT.md` / `HARMONY_N3_RUNTIME_REPORT.md`。

> **上一轮（2026-09-17 D-16 关闭轮）三个判定**：
> `N1_ANDROID_VERTICAL_SLICE` = **PASS**（核心垂直切片 E2E v4 全链路无 FAIL，崩溃 0）
> `N2_ANDROID_FULL_PARITY` = **PARTIAL_WITH_REPORT**（**62 / 73**，11 项未关闭）
> `ANDROID_PRODUCTION_RELEASE_READY` = **BLOCKED_BY_MISSING_PRODUCTION_KEYSTORE**
>
> 逐格结论见 `NATIVE_PARITY_MATRIX.md`；Gate 侧结论见 `ANDROID_N1_N2_FINAL_CLOSURE_REPORT_V2.md`。
>
> **Stop condition 已遵守**：D-16 关闭 + 全回归 + parity 重算 + Git 收口后停止，
> **未进入 Harmony N3**，等人工 Final Acceptance。

> 历史：2026-09-16 的 P0 Runtime Closure 轮记录了 `N1 = PARTIAL_WITH_REPORT` /
> `N2 = PARTIAL_WITH_REPORT` / parity `56/73`（D-16 未修）。该轮结论**已被本轮取代**，
> 但 D-16 的发现历史与根因完整保留在 `ANDROID_N1_N2_FINAL_CLOSURE_REPORT_V2.md`。

---

## PHASE

| Phase | 名称                        | 状态                    | 说明                                              |
| ----- | --------------------------- | ----------------------- | ------------------------------------------------- |
| N0-A  | 恢复仓库现场                | **PASS**                | git 全套审计；工作树 clean；HEAD `7bc0ed3`        |
| N0-B  | Legacy 冻结                 | **PASS**                | tag `v0.3.0-uniapp-reference` + manifest + README |
| N0-C  | Canonical Spec              | **PASS**                | `spec/` 机器可读（枚举/实体/关系/状态机/错误/Schema/安全/UI） |
| N0-D  | Codegen + Gate              | **PASS**                | spec → Kotlin/Swift/ArkTS；`--check` PASS         |
| N0-E  | Golden Fixtures             | **PASS**                | **91 用例** + 28 输入 fixture + manifest + sha256  |
| N0-F  | Conformance Harness         | **PASS**                | `node tools/conformance/run.mjs` 全绿            |
| N1    | Android Vertical Slice      | **PASS**（2026-09-17 D-16 关闭后重新确认） | 核心垂直链 import→proposal→reality→影响面→changeplan→done→verified 在设备上端到端跑通；D-16 修复后**外部文件选择器往返不再丢工作流**，Import 真的写入（「记录 6 行」）。证据：`core-journey-v4-20260917-184856` = **41/41 PASS / 0 FAIL** + `FileWorkflowD16Test` 6/6 |
| N2    | Android Full Parity         | **PARTIAL_WITH_REPORT** | **62 / 73**（D-16 关闭 +4，§1 计数口径修正 +1，设备 E2E +1）。未关闭 11 项逐格列在 `NATIVE_PARITY_MATRIX.md` |
| N3    | HarmonyOS Full Parity       | **NOT_STARTED**（工程已开工，parity 仍 0/73） | 本轮正式进入：`HARMONY_BUILD = PASS`（hvigor 全清重建产出 HAP 60,133 B）、`HARMONY_DOMAIN = PARTIAL_WITH_REPORT`（Relations 已编译进 HAP）、`HARMONY_ARKUI = PARTIAL_WITH_REPORT`（骨架 + 1 占位页）；`HARMONY_DEPMAP = BLOCKED_BY_NATIVE_VERIFICATION`（Argon2 原生路径已打通，待设备上复验；见 `HARMONY_ARGON2_FEASIBILITY.md`）、`HARMONY_RUNTIME_E2E = RUNTIME_NOT_RUN`（无模拟器镜像）。14 个 Gate 见 `HARMONY_N3_IMPLEMENTATION_STATUS.md` |
| N4    | iOS Full Parity             | **NOT_STARTED**         | 仅 codegen 产物；build `BLOCKED_BY_MACOS`           |
| N5    | Cross-platform Conformance  | **PARTIAL_WITH_REPORT** | Android **91/91 PASS**（本轮实跑复验）；Harmony **NOT_RUN**（0 执行，87 notImplemented / 4 blocked）；iOS 未开始。见 `CROSS_PLATFORM_CONFORMANCE_MATRIX.md` |
| N6    | Legacy Cutover              | **NOT_STARTED**         | 未满足 Cutover 条件（三端 parity 未达成）           |
| N7    | Native Production RC        | **NOT_STARTED**         |                                                   |

---

## ANDROID

| 层             | 状态                    | 证据                                                              |
| -------------- | ----------------------- | ----------------------------------------------------------------- |
| 工程脚手架     | **PASS**                | `android/` Gradle (Kotlin DSL)，`:core` + `:conformance` + `:app` 三模块 |
| Domain（纯 Kotlin） | **PASS**            | domain / impact / plan / scenario / statemachine / schema / json / sources / serialize / timeline |
| Codegen 枚举   | **PASS**                | `android/core/.../generated/CanonicalEnums.kt`                     |
| Crypto（`.depmap`） | **PASS**            | 黄金向量 derivedKey / ciphertext / tag **逐字节一致**              |
| JCS (RFC 8785) | **PASS**                | 规范用例 + 浮点拒绝                                                |
| **Conformance** | **PASS**               | **91/91**（harness 独立复核，非自我宣称）                          |
| 解析器（WeChat / CSV / OFX） | **PASS** | 22 个平台中立 fixture 全通过（BOM/CRLF/CR-only/引号逗号/分号/借贷列/多币种/GB18030/FITID 缺失/非法日期/坏块） |
| 持久化（SQLCipher） | **PASS**             | `net.zetetic:sqlcipher-android:4.5.5`；`Migrations.kt` v1→v2→v3，事务 / 回滚 / 幂等 / ID 保留全部验证 |
| Schema 迁移     | **PASS**                | `migration-db-v1-to-v3`：版本到 3、legacy 归属、50 次重复执行严格 no-op |
| Backup / Restore | **PASS**              | `backup-depmap-export-restore-roundtrip`：导出→加密→解密→恢复→再导出**逐字节相同**，无孤儿引用 |
| Keystore       | **PASS**                | `DatabaseKeyStore`：AES-256-GCM 密钥由 Android Keystore 生成且不可导出，包裹 DB passphrase |
| Biometric / App Lock | **PARTIAL_WITH_REPORT** | **App Lock 接线 = RUNTIME_VERIFIED**（冷启动先锁 / 解锁后才进 / 前后台回锁 / 锁定时 NavGraph 不参与组合；`AppLockNavigationTest` + E2E v4 J0 与 J9）。**生物识别匹配 = BLOCKED_BY_RUNTIME_ENVIRONMENT**（AVD 无 `hw.finger`）。两者同属一格 ⇒ 整格仍是 PARTIAL |
| UI（Compose）  | **PASS**                | NavHost 注册 **20 个目的地**：Onboarding / Home / Scenario Center / Scenario Setup / ChangePlan(`plan/{planId}`) / Timeline / Pending Review / RealityDrift / Candidate Review / Infrastructure / Graph / NodeDetail(`node/{nodeId}`) / Sources / Import / Backup / Restore / Settings / Privacy / About / Impact(`impact/{nodeId}`)。「Lock」**刻意不是导航目的地** —— 它是 App 的门，锁定时 NavHost 本体不参与组合 |
| Design System  | **PASS**                | `PDIGTheme` + tokens（color / spacing / radius / typography / status）映射自 `spec/ui/design-tokens.json` |
| **Build（APK）** | **PASS**               | `app-debug.apk` **36,887,249 B**，SHA-256 `bf378ec6…305ff1`（2026-09-16 与源码同步重建；归档 `local_private/artifacts/app-debug.apk`） |
| **Gradle Wrapper** | **PASS**             | 本轮新增。`gradlew` / `gradlew.bat` / `gradle-wrapper.jar`(43,504 B) / `gradle-wrapper.properties`（Gradle 8.9，官方 `distributionUrl`，无机器绝对路径）。此前**完全缺失**，构建依赖本机绝对路径 Gradle |
| Runtime / 核心行程 | **PASS**（2026-09-17 D-16 关闭后重新确认） | AVD `emulator-5554`（API34）核心行程 **v4** 全新 run：全新安装 → SAF 导入真实 CSV（2 支付方式 / 3 收款对象）→ 提交「记录 6 行」→ 候选 → 确认 Reality → 用户标记必需 → 影响面「必须处理（2）」→ 变更计划 → done≠verified → 验证 → `am kill`（真实进程死亡，先按 HOME 再 kill）重建后**首屏是锁屏**且数据仍在（共 5 个对象）→ `.depmap` 导出 13,617 B 且 UI 文案一致 → 错误密码恢复被拒 → 清数据 → 正确口令恢复成功 → 篡改容器被拒。崩溃 0。**最终 run id = `core-journey-v4-20260917-184856`（41/41 PASS / 0 FAIL）**，详见 `local_private\e2e\core-journey-v4-20260917-184856.{txt,json}` |
| Runtime / 设备 E2E | **PASS**（2026-09-17 升级） | 安装 / 首次启动 / 首页 / 场景中心 / 基础设施总览 / 加密持久化 / 明文 sqlite 无法打开 / 前后台切换 / 杀进程重启 / 清状态 / 触摸目标 / 字体缩放 / 横屏 / 焦点顺序 / logcat 隐私 **均 PASS**；核心行程 v4 覆盖 Import / Restore 全链路。**仍 NOT_RUN：TalkBack**（镜像未预装、无 Play 商店）；Onboarding / Timeline / Graph 二级页未被真机走过（无入口或无流程触发） |
| `:core` JVM 单测 | **PASS** | **71 / 71**：DomainInvariant 15 / ImpactKernel 11 / PlanReadiness 14 / MigrationSemantics 10 / GraphRevisionSemantics 7 / StateMachine 14。详见 `ANDROID_CORE_JVM_TEST_REPORT.md` |
| `:app` JVM 单测 | **PASS**（2026-09-17 新增） | **9 / 9**：`FileWorkflowStateTest`（D-16 工作流状态机）。**此前 `:app` 的 JVM 单测是 NO-SOURCE**（目录里放多少文件都跑 0 个用例却 BUILD SUCCESSFUL）——本轮把 `src/test/kotlin` 移到 AGP 标准源目录后真实执行 |
| 设备内 androidTest | **PASS** | **51 / 51 PASS**（0 skipped / 0 failed），4 批严格取证：13 + 18 + 15 + 5。新增 `FileWorkflowD16Test` 6 个用例。**取证口径**：每批产出独立 md5/sha256、mtime 落在本批时间窗内、类名与本批预期集合一致、四批指纹互不相同；0 test 一律记 FAIL |
| 性能 smoke      | **PASS**             | **有效数据**：`csvRowsParsed=10000`、`csvParseErrors=0`，强断言 `assertEquals(10_000, rows)` 通过。旧数字（parse=2427ms / insert=4839ms）因 `csvRowsParsed=0` 已**作废** |
| 无障碍          | **PARTIAL_WITH_REPORT** | 实机审计：触摸目标 / 焦点顺序 / 字体缩放 / 横屏 PASS；**4 个可点击节点无标签**；TalkBack 未验证（NOT_RUN） |
| 截图保护        | **PASS**（2026-09-16 升级为 RUNTIME_VERIFIED） | 真机 **6/6 路由双证据**：敏感页（SOURCES / IMPORT / INFRASTRUCTURE / BACKUP）窗口 `fl=` 含 `SECURE` 且 `screencap` 被抹黑（均值 0.17）；非敏感页（HOME / SETTINGS）无 `SECURE` 且截图正常（均值 244.64）。**注**：此前"运行时 flag 未取得"是检测口径 bug —— `dumpsys` 输出的是裸 flag 名 `SECURE`，grep `FLAG_SECURE` 恒为 0 |
| Store metadata  | **PARTIAL_WITH_REPORT** | `ANDROID_STORE_METADATA.md` 文案草稿完成；截图 / 图标 / 隐私政策公开链接 NOT_STARTED |
| Release 签名    | **BLOCKED_BY_MISSING_PRODUCTION_KEYSTORE** | `app-release.aab` 20,734,935 B 构建成功但**未签名**；非生产签名配置本轮**未生效**（与未签名产物同 SHA-256），已如实记录 |
| logcat 隐私扫描 | **PASS**             | 改为 PID/UID 进程归属扫描后：`appLines=44`，6 类敏感关键字命中**全 0**。旧扫描曾把 Launcher3 的 `password:false` 系统字段误判为应用泄露（已修） |

**可复现命令**（本轮起**不再依赖本机绝对路径**，改用仓库内 Wrapper）：

```bash
cd android

# 0. 前置（仅首次）：下载 Gradle 8.9 发行包需要 JVM 代理
export JAVA_HOME="<ANDROID_STUDIO_HOME>/jbr"
export GRADLE_OPTS="-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=10808 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=10808"

# 1. Conformance（91 用例）
./gradlew --no-daemon :conformance:run --console=plain

# 2. 构建 debug APK
./gradlew --no-daemon :app:assembleDebug

# 2b. `:core` JVM 单测（71 用例）
# 注意：仓库路径含中文时，Gradle test worker 会 ClassNotFoundException。
# 用 init script 把 build 目录重定向到 ASCII 路径即可，源码路径不用动。
export PDIG_ASCII_BUILD_ROOT=%USERPROFILE%/pdig-build
./gradlew --no-daemon -I %USERPROFILE%/pdig-gradle/ascii-build.gradle.kts :core:test

# 3. 设备内测试（需 emulator 已启动）
./gradlew --no-daemon :app:connectedDebugAndroidTest

# 4. 跨端 Gate
cd .. && node tools/conformance/run.mjs
```

最近一次实跑：`pass=91 fail=0 notImplemented=0 total=91`

> **已知环境脆弱点**：AVD 3GB RAM + 宿主内存紧张时，单进程一次跑完 19 个 androidTest 会被 OOM kill（signal 9）。
> 规避方式：按类分批 `am instrument` + 类间 `pm clear` / `logcat -c`。

---

## HARMONY

> **2026-09-17 更新：N3 已开工。** 工程从"仅 1 个 codegen 文件"变为可真实构建的 Stage Model 工程。

| 层             | 状态        | 说明                                        |
| -------------- | ----------- | ------------------------------------------- |
| 工程脚手架     | **PASS** | `harmony/` 已建 Stage Model 工程（AppScope / build-profile / oh-package / hvigorfile / entry module.json5 / EntryAbility / resources）。hvigor 全清重建 **BUILD SUCCESSFUL** |
| Domain（ArkTS）| **PARTIAL_WITH_REPORT** | `entry/src/main/ets/domain/Relations.ets` 已实现并**编译进 HAP**（HAP 内含域代码标记）；Impact / Readiness / Coverage / StateMachine / Timeline / Migration / GraphRevision / Scenario 未开工 |
| Build          | **PASS** | HAP `entry-default-unsigned.hap`，**60,133 B**，sha256 `ac86a5af1a7f15d2ddba70b139b4cbe862d3d1af2898efa496ac05801f9c00fa`（未签名） |
| Crypto         | **BLOCKED** | `cryptoFramework` KDF 仅 PBKDF2 / HKDF，**无 Argon2** → `.depmap` 无法实现（禁止自研原语） |
| 持久化         | **NOT_STARTED** | ArkData relationalStore 未开工 |
| UI（ArkUI）    | **PARTIAL_WITH_REPORT** | 骨架 + 1 个占位页；§L 的 17 个页面未开工 |
| Conformance    | **NOT_RUN** | 无设备/模拟器、未接本地测试框架；0 执行（87 notImplemented / 4 blocked / 91） |
| Runtime E2E    | **RUNTIME_NOT_RUN** | `hdc list targets = [Empty]`；无模拟器系统镜像（需 DevEco GUI 下载，用户侧外部闸门） |

> 注意：`platforms/harmonyos/` 是**旧 UTS 路线**产物，**仅 Legacy Reference**，
> 不得成为 Production dependency，其 HAP 也不得计入 `harmony/` 任何 Gate 证据。

> **两个必须记住的构建坑**：
> 1. hvigor 拒绝非 ASCII 工程路径（校验 `process.cwd()`，无环境变量绕过；`mklink /J` 亦无效）
>    → 用 `tools/harmony/build-ascii-mirror.mjs` 做 ASCII 镜像构建。
> 2. hvigor 增量 `CompileArkTS` 会**漏掉新增文件**（曾报 UP-TO-DATE 且 SUCCESSFUL，
>    而新文件其实未编译）→ 验证性构建必须**全清**。

---

## IOS

| 层             | 状态                 | 说明                                        |
| -------------- | -------------------- | ------------------------------------------- |
| 工程脚手架     | **NOT_STARTED**      | 仅 codegen 产物 `ios/Sources/PDIGCore/Generated/` |
| Domain（Swift）| **NOT_STARTED**      |                                             |
| Crypto         | **NOT_STARTED**      |                                             |
| Conformance    | **NOT_RUN**          |                                             |
| Build / Test   | **BLOCKED_BY_MACOS** | 当前为 Windows，无 Xcode                     |

---

## CONFORMANCE

| Gate                | 状态 | 证据                                        |
| ------------------- | ---- | ------------------------------------------- |
| codegen gate        | PASS | 3 个 generated 文件与 spec 一致             |
| fixture integrity   | PASS | 91 用例 + 28 输入文件 sha256 全匹配          |
| oracle selfcheck    | PASS | 冻结 TS oracle 逐字节复现 91 用例            |
| platform: android   | PASS | **91/91**                                    |
| platform: harmony   | NOT_RUN | **0 执行**：无设备/模拟器，未接本地测试框架。逐分类：87 notImplemented / 4 blocked（depmap 3 + backup 1，因无 Argon2）。见 `HARMONY_N3_CONFORMANCE_REPORT.md` |
| platform: ios       | NOT_RUN | 无报告                                    |
| **VERDICT**         | **PASS** | `conformance/reports/SUMMARY.json`       |

分类覆盖：impact 13 / readiness 16 / coverage 6 / relations 18 / depmap 3 / jcs 1 / scenario 1 / **migration 2** / state-machine 5 / **parser 22** / **timeline 3** / **backup 1**

---

## BLOCKERS

见 `NATIVE_EXTERNAL_BLOCKERS.md`。

- **iOS build 阻塞于 macOS**（真实外部 blocker）。
- **Android Release 签名阻塞于生产 keystore**：`BLOCKED_BY_MISSING_PRODUCTION_KEYSTORE`；非生产签名流水线本轮**未生效**，需先修流水线再谈上架。
- **Android Release 签名阻塞于生产 keystore**（同上，仍为真实 blocker）。
- **App Lock 无 UI 入口（真实缺口，2026-09-16 确认）**：设备上 `AppLock.state()` 返回 `NOT_CONFIGURED`（fail-closed 正确），
  但 `MainActivity` 固定 `startDestination=HOME`，全仓库无 `nav.navigate(Route.LOCK)`。
  `LockScreen` 已实现却**没有任何路径能调起它**。这是 N2 不判 PASS 的**第一原因**。
- **Biometric 仍 BLOCKED**：依赖真机生物特征，AVD 无指纹硬件。
- **无障碍**：4 个无标签可点击节点未修；TalkBack 镜像未预装。

**2026-09-17 新增（Harmony N3 侧）**：

- **B25 Harmony 无 Argon2（P0）**：`@ohos.security.cryptoFramework` 的 KDF 仅 `PBKDF2Spec` / `HKDFSpec`，
  全文检索 Argon2 零命中 → `HARMONY_DEPMAP = BLOCKED`。NDK 侧亦无 openssl / libsodium / argon2 产物。
  （**2026-09-18 追加**：NDK 自身 clang/sysroot/Node-API/CMake 工具链齐备，只是**不自带**这些库；
  因此改为编译 PHC 参考实现 —— 主机 Golden Vector 已 `MATCH=YES`，arm64 `.so` 已产出，
  状态精确化为 `BLOCKED_BY_NATIVE_VERIFICATION`。见 `HARMONY_ARGON2_FEASIBILITY.md`。）
  可行解（需评审）：NAPI + 经审计的外部 Argon2 参考实现，或经审计的 ohpm 三方包。
- **B26 Harmony 无运行时目标（P0，用户侧）**：Emulator.exe 存在但**无任何系统镜像**；
  `hdc list targets = [Empty]` → `HARMONY_RUNTIME_E2E = RUNTIME_NOT_RUN`。
  需 DevEco Studio GUI 下载镜像（账号/网络），与 Android 侧 B18 同类。
- **HAP 未签名**：无 signingConfig，产物为 `entry-default-unsigned.hap`；
  与 Android 侧缺生产 keystore 同类的外部闸门。

**2026-09-16 P0 轮已解除的 blocker**：

- ~~核心垂直切片未在设备级跑通~~ → **已解除**：真机 21/21 PASS，崩溃 0。N1 转为 **PASS**。
- ~~`:core` 纯 JVM 单元测试为 0~~ → **已解除**：`:core:test` **71/71 PASS**。
- ~~FLAG_SECURE 运行时 flag 取不到~~ → **已解除**：是检测口径 bug（`dumpsys` 输出裸 flag 名 `SECURE`），
  改用正确 token 后 6/6 路由双证据 PASS。
- ~~Android 设备 E2E 待跑~~ → 已解除。

---

## NEXT（下一 Agent 的第一步）

> **⚠ 本节已更新（2026-09-17 第二场）：Harmony N3 已开工，并在真实 blocker 处停止。**
>
> **Android 侧**：已进入 `CORE_FROZEN / MAINTENANCE_ONLY`，**不再为 parity 分数新增功能**。
> 仅允许 regression fix / Canonical Spec 同步 / 跨平台 conformance fix / N5 发现的 parity bug。
>
> **Harmony 侧**：以下为解除 N3 blocker 的清单，**不进入 iOS N4**。

**N3 下一步（按优先级）**

| 优先级 | 事项 | 前置 |
| --- | --- | --- |
| P0 | 确立 Argon2 路径（NAPI + 外部参考实现，或 ohpm 三方包），或正式升级为长期 blocker | 安全评审 |
| P0 | DevEco 下载模拟器系统镜像，打通 `HARMONY_RUNTIME_E2E` | **用户操作**（账号/网络） |
| P1 | 接 DevEco 本地测试框架，让 conformance 可脱离设备执行 | — |
| P1 | 继续 Domain：Impact → PlanReadiness → Coverage → StateMachine → Timeline | Android 冻结基准 |
| P2 | ArkData persistence + migration（I 节） | — |
| P2 | HUKS + 用户认证（J 节） | — |
| P2 | FileWorkflowCoordinator（M 节，防 D-16 重演） | application 层 |


**P0 轮（2026-09-16）已完成**：

1. ✅ 应用层写入路径打通 → 核心垂直切片真机端到端 **21/21 PASS**，N1 转 PASS
2. ✅ `:core` JVM 单元测试 **71/71**
3. ✅ FLAG_SECURE 运行时取证 **6/6 路由**
4. ✅ 三扇安全门设备取证（Gate 1 PASS / Gate 2 缺入口 / Gate 3 PASS）
5. ✅ 全回归：conformance 91/91 + 设备内 19/19 + `:core` 71/71
6. ✅ Git HEAD `68f506c` 核验通过

**未清的 blocker（按优先级）**：

1. `git log --oneline -1 && git status --short -uall`（回归起步动作）
2. `cd core && npm test`（确认 oracle 仍 453/453）
3. **P0'**：**App Lock 接线** —— 给 `Route.LOCK` 一个可达入口（设置项 + 生命周期回调），
   否则 N2 无法转 PASS
4. **P1**：在带指纹的镜像上验证 Biometric（`BiometricPrompt` 成功 / 失败 / 取消分支）
5. **P1**：补上 4 个无标签可点击节点的 `contentDescription`；TalkBack 实机读屏
6. **P1**：修好 release 签名流水线（当前非生产签名未生效）
7. **P1（新发现）**：备份导出 **UI 误报失败**（文件已完整落盘且可正常恢复，App 却提示失败；2/2 稳定复现）
8. **P2**：截图 / 图标 / 隐私政策公开链接
9. **P2**：导出组件逐个归属审计；release 构建 `debuggable` 复查
10. 上述 P0' 关闭后，重新计算 27 个 Gate；**只有在 N1 / N2 双双转 PASS 之后**才进入 **N3 Harmony**

**待用户拍板（本轮未自行决定）**：

- 是否提交：`android/`（4655 文件）、`conformance/`、`fixtures/` 等目前**全部未跟踪**，
  本轮未做任何 `git add` / `commit`。
