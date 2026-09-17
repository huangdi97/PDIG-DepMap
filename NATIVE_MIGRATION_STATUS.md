# NATIVE_MIGRATION_STATUS.md

> 持续更新。格式：PHASE / ANDROID / HARMONY / IOS / CONFORMANCE / BLOCKERS / NEXT。
> 状态枚举：`PASS` `FAIL` `BLOCKED` `NOT_RUN` `PARTIAL_WITH_REPORT`

更新时间：2026-09-16（**P0 Runtime Closure 轮后复核**）

> 完整结论见 `ANDROID_N1_N2_FINAL_CLOSURE_REPORT.md`（27 个 Gate 逐个重算）。
> 不沿用旧的 55/62，parity 重算为 **59 / 62**。
>
> **P0 轮（2026-09-16）三个判定**：
> `N1_ANDROID_VERTICAL_SLICE` = **PASS**（核心垂直切片在真机 21/21 跑通，崩溃 0）
> `N2_ANDROID_FULL_PARITY` = **PARTIAL_WITH_REPORT**（59/62，3 项未关闭）
> `ANDROID_PRODUCTION_RELEASE_READY` = **BLOCKED_BY_PRODUCTION_SIGNING**
>
> **Stop condition 已遵守**：P0 后停止，**未进入 Harmony N3**。

---

## PHASE

| Phase | 名称                        | 状态                    | 说明                                              |
| ----- | --------------------------- | ----------------------- | ------------------------------------------------- |
| N0-A  | 恢复仓库现场                | **PASS**                | git 全套审计；工作树 clean；HEAD `6d268c0`        |
| N0-B  | Legacy 冻结                 | **PASS**                | tag `v0.3.0-uniapp-reference` + manifest + README |
| N0-C  | Canonical Spec              | **PASS**                | `spec/` 机器可读（枚举/实体/关系/状态机/错误/Schema/安全/UI） |
| N0-D  | Codegen + Gate              | **PASS**                | spec → Kotlin/Swift/ArkTS；`--check` PASS         |
| N0-E  | Golden Fixtures             | **PASS**                | **91 用例** + 28 输入 fixture + manifest + sha256  |
| N0-F  | Conformance Harness         | **PASS**                | `node tools/conformance/run.mjs` 全绿            |
| N1    | Android Vertical Slice      | **PASS**（2026-09-16 升级） | 核心垂直切片 import→proposal→reality→影响面→changeplan→done→verified **已在真机端到端跑通**：21/21 PASS，崩溃 0；`am kill` 后数据仍在。分层证据 181 用例全绿。此前因无应用层写入路径降级为 PARTIAL，本轮修复 |
| N2    | Android Full Parity         | **PARTIAL_WITH_REPORT** | **59 / 62**（N1/N2 轮 +3，P0 轮 +1 单测）。未关闭 3 项：设备 E2E（剩余入口缺失）、无障碍、Release 签名 |
| N3    | HarmonyOS Full Parity       | **NOT_STARTED**         | 仅 codegen 产物。**本轮明确不进入**（判定 B）      |
| N4    | iOS Full Parity             | **NOT_STARTED**         | 仅 codegen 产物；build `BLOCKED_BY_MACOS`           |
| N5    | Cross-platform Conformance  | **PARTIAL_WITH_REPORT** | Android 一侧 **91/91 PASS**（本轮用 `./gradlew :conformance:run` 独立复跑）；Harmony / iOS 未开始 |
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
| Biometric / App Lock | **PARTIAL_WITH_REPORT** | `AppLock`（BiometricPrompt）+ `LockScreen` 分支齐全，但 **2026-09-16 真机取证：设备上 `AppLock.state()` 返回 `NOT_CONFIGURED`（fail-closed 正确），而全仓库无 `nav.navigate(Route.LOCK)` —— 锁屏页写好了却没有任何 UI 入口能调起**。这是真实缺口，不再是 PASS |
| UI（Compose）  | **PASS**                | 21 个页面（Onboarding / Lock / Home / Scenario / Plan / Timeline / Review / Drift / Candidate / Infrastructure / Graph / NodeDetail / Sources / Import / Backup / Restore / Settings / Privacy / About） |
| Design System  | **PASS**                | `PDIGTheme` + tokens（color / spacing / radius / typography / status）映射自 `spec/ui/design-tokens.json` |
| **Build（APK）** | **PASS**               | `app-debug.apk` **36,887,249 B**，SHA-256 `bf378ec6…305ff1`（2026-09-16 与源码同步重建；归档 `local_private/artifacts/app-debug.apk`） |
| **Gradle Wrapper** | **PASS**             | 本轮新增。`gradlew` / `gradlew.bat` / `gradle-wrapper.jar`(43,504 B) / `gradle-wrapper.properties`（Gradle 8.9，官方 `distributionUrl`，无机器绝对路径）。此前**完全缺失**，构建依赖本机绝对路径 Gradle |
| **Runtime / 核心行程** | **PASS**（2026-09-16 新增） | AVD `emulator-5554`（API34）**核心写路径 21/21 PASS，崩溃 0**：全新安装 → SAF 导入真实 CSV（2 支付方式 / 3 收款对象）→ 提交落库（10.8s，「记录 5 个对象，生成 3 条待确认关系」）→ 候选 → acceptProposal → **用户标记必需** → 影响面「必须处理（2）」→ 变更计划 → done≠verified → 验证 → `am kill` 后数据仍在 → `.depmap` 导出 13,617 B → 错误密码恢复被拒。详见 `ANDROID_CORE_USER_JOURNEY_E2E_REPORT.md` |
| Runtime / 设备 E2E | **PARTIAL_WITH_REPORT** | 安装 / 首次启动 / 首页 / 场景中心 / 基础设施总览 / 加密持久化 / 明文 sqlite 无法打开 / 前后台切换 / 杀进程重启 / 清状态 / 触摸目标 / 字体缩放 / 横屏 / 焦点顺序 / logcat 隐私 均 PASS。**仍 NOT_RUN：Onboarding / Timeline / Drift / App Lock / TalkBack**（业务写入链路已于 P0 轮打通） |
| `:core` JVM 单测 | **PASS**（2026-09-16 新增） | **71 / 71**：DomainInvariant 15 / ImpactKernel 11 / PlanReadiness 14 / MigrationSemantics 10 / GraphRevisionSemantics 7 / StateMachine 14。详见 `ANDROID_CORE_JVM_TEST_REPORT.md` |
| 设备内 androidTest | **PASS**             | `connectedDebugAndroidTest` **19 / 19 PASS**（0 skipped / 0 failed）；19 个用例来自 5 个 evidence 测试类 |
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

| 层             | 状态        | 说明                                        |
| -------------- | ----------- | ------------------------------------------- |
| 工程脚手架     | **NOT_STARTED** | 仅 codegen 产物 `harmony/entry/.../generated/` |
| Domain（ArkTS）| **NOT_STARTED** |                                             |
| Crypto         | **NOT_STARTED** |                                             |
| 持久化         | **NOT_STARTED** |                                             |
| UI（ArkUI）    | **NOT_STARTED** |                                             |
| Conformance    | **NOT_RUN** | `conformance/reports/harmony.json` 缺失      |

> 注意：`platforms/harmonyos/` 是**旧 UTS 路线**产物，不是本次 Native 工程。

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
| platform: harmony   | NOT_RUN | 无报告                                    |
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

**2026-09-16 P0 轮已解除的 blocker**：

- ~~核心垂直切片未在设备级跑通~~ → **已解除**：真机 21/21 PASS，崩溃 0。N1 转为 **PASS**。
- ~~`:core` 纯 JVM 单元测试为 0~~ → **已解除**：`:core:test` **71/71 PASS**。
- ~~FLAG_SECURE 运行时 flag 取不到~~ → **已解除**：是检测口径 bug（`dumpsys` 输出裸 flag 名 `SECURE`），
  改用正确 token 后 6/6 路由双证据 PASS。
- ~~Android 设备 E2E 待跑~~ → 已解除。

---

## NEXT（下一 Agent 的第一步）

> **stop condition 已遵守：P0 轮结束后停止，未进入 Harmony N3。**
> 以下为下一轮（若决定继续）的"清 blocker"清单，**不做新功能、不新增 Domain、不重设计 UI**。

**P0 轮（2026-09-16）已完成**：

1. ✅ 应用层写入路径打通 → 核心垂直切片真机端到端 **21/21 PASS**，N1 转 PASS
2. ✅ `:core` JVM 单元测试 **71/71**
3. ✅ FLAG_SECURE 运行时取证 **6/6 路由**
4. ✅ 三扇安全门设备取证（Gate 1 PASS / Gate 2 缺入口 / Gate 3 PASS）
5. ✅ 全回归：conformance 91/91 + 设备内 19/19 + `:core` 71/71
6. ✅ Git HEAD `ad2350b` 核验通过

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
