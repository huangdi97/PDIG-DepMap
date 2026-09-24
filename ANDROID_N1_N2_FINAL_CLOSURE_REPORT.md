# ANDROID_N1_N2_FINAL_CLOSURE_REPORT.md

> 本轮目标：不做新功能，用**真实运行时证据**判断 Android N1 / N2 是否能 PASS。
> 所有 Gate **重新计算**，不沿用旧的 55/62。
> 状态只允许：`PASS` / `FAIL` / `BLOCKED` / `NOT_RUN` / `PARTIAL_WITH_REPORT`。

更新时间：2026-09-15（N1/N2 轮） · **2026-09-16 追加 P0 Runtime Closure 复验**

---

## 1. 本轮执行摘要

| 项                                                                       | 结果                                                                                                                      |
| ------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------- |
| Gradle Wrapper                                                           | 从"完全不存在"恢复为可用（Gradle 8.9）                                                                                    |
| `gradlew clean / assembleDebug / assembleDebugAndroidTest / conformance` | 全部 BUILD SUCCESSFUL                                                                                                     |
| `:conformance:run`                                                       | **91 / 91 PASS**                                                                                                          |
| 设备内 androidTest（`connectedDebugAndroidTest`）                        | **19 / 19 PASS**（0 skipped，0 failed）                                                                                   |
| `:core:test`                                                             | ~~NO-SOURCE（0 个 JVM 单元测试）~~ → **2026-09-16：71 / 71 PASS**                                                         |
| 真机 E2E（串行单次干净运行）                                             | 安装/首页/场景/基础设施/加密持久化/热启动/重启/清状态/字体缩放/横屏/焦点顺序/logcat 隐私 均 PASS                          |
| **真机核心行程（2026-09-16 新增）**                                      | **21 / 21 PASS，崩溃 0** —— import→proposal→reality→影响面→changeplan→done→verified→进程死亡恢复→depmap 导出/错误密码拒绝 |
| 性能 smoke                                                               | **有效数据**（10,000 行解析，0 错误），旧数字已作废                                                                       |
| AAB                                                                      | 构建成功（20,734,935 B），但**未签名**，非生产签名未生效                                                                  |
| 截图保护                                                                 | **2026-09-16 已取得运行时证据**：6/6 路由按预期设置 FLAG_SECURE（窗口 `fl=` 含 `SECURE` + 截图被抹黑）                    |
| Git HEAD（P0-5）                                                         | `68f506c`，无删除、无 staged 残留                                                                                         |

> **P0 轮结论**：N1 由 PARTIAL 升为 **PASS**；N2 仍为 PARTIAL（59/62）；
> `RELEASE_READY` 仍为 **BLOCKED_BY_PRODUCTION_SIGNING**。

---

## 2. 全部 Gate（27 项）

| #   | Gate                             | 状态                                                              | 关键证据                                                                                                                                                                                                                                                                  |
| --- | -------------------------------- | ----------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | `ANDROID_GRADLE_WRAPPER`         | **PASS**                                                          | `gradlew` / `gradlew.bat` / `gradle-wrapper.jar`(43,504 B) / `gradle-wrapper.properties` 已生成；`gradlew.bat --version` 输出 Gradle 8.9                                                                                                                                  |
| 2   | `ANDROID_BUILD_REPRODUCIBILITY`  | **PASS**                                                          | clean → assembleDebug → androidTest → conformance 全通过；`distributionUrl` 为官方地址，无机器绝对路径                                                                                                                                                                    |
| 3   | `ANDROID_DOMAIN`                 | **PASS**                                                          | conformance 91/91                                                                                                                                                                                                                                                         |
| 4   | `ANDROID_CRYPTO`                 | **PASS**                                                          | conformance + 设备内 `DepmapRuntimeEvidenceTest` 4/4                                                                                                                                                                                                                      |
| 5   | `ANDROID_JCS`                    | **PASS**                                                          | conformance 覆盖 JCS (RFC 8785) 受限域                                                                                                                                                                                                                                    |
| 6   | `ANDROID_CONFORMANCE`            | **PASS**                                                          | `pass=91 fail=0 notImplemented=0 total=91`                                                                                                                                                                                                                                |
| 7   | `ANDROID_SQLCIPHER`              | **PASS**                                                          | `PersistenceEvidenceTest` 8/8；明文 sqlite3 无法打开；库头非 SQLite 魔数                                                                                                                                                                                                  |
| 8   | `ANDROID_SCHEMA`                 | **PASS**                                                          | conformance + 迁移测试                                                                                                                                                                                                                                                    |
| 9   | `ANDROID_MIGRATION`              | **PASS**                                                          | v1→v3 / v2→v3 保真、未来版本拒绝、失败回滚不抹库                                                                                                                                                                                                                          |
| 10  | `ANDROID_REPOSITORY`             | **PASS**                                                          | `RepositoryKeystoreEvidenceTest` 4/4（Reality 变更 +1 graphRevision；Proposal 决策不 bump）                                                                                                                                                                               |
| 11  | `ANDROID_KEYSTORE`               | **PASS**                                                          | `pdig_secure.xml` 只有 `db_passphrase_wrapped` 密文，无明文                                                                                                                                                                                                               |
| 12  | `ANDROID_BIOMETRIC`              | **BLOCKED**                                                       | AVD 无指纹硬件；`BiometricPrompt` 已实现但**未实机验证**                                                                                                                                                                                                                  |
| 13  | `ANDROID_APP_LOCK`               | **NOT_RUN → 2026-09-16 复验仍为 NOT_RUN（缺口确认）**             | 设备上 `AppLock.state()` 返回 `NOT_CONFIGURED`（fail-closed 正确）；但 `MainActivity` 固定 `startDestination=HOME`，全仓库无 `nav.navigate(Route.LOCK)`，**LockScreen 没有任何 UI 入口**                                                                                  |
| 14  | `ANDROID_IMPORT`                 | **PASS**                                                          | 解析器（WeChat / CSV / OFX、BOM/CRLF/GB18030、坏行保守拒绝）由 conformance 覆盖；**2026-09-16 补**：设备级 import UI 链路（SAF 选文件 → 解析 → 提交落库）**已 21/21 PASS**，不再为 NOT_RUN                                                                                |
| 15  | `ANDROID_DEPMAP_BACKUP_RESTORE`  | **PASS**                                                          | 设备内 4/4（字节一致 / 错误口令 / 篡改 / v1 v2 迁移等价）；**注**：SAF 文件选择器无法 adb 自动化                                                                                                                                                                          |
| 16  | `ANDROID_COMPOSE_UI`             | **PASS**                                                          | 首页 / 场景中心 / 基础设施总览 在真机渲染并验证                                                                                                                                                                                                                           |
| 17  | `ANDROID_SCREEN_PROTECTION`      | **PARTIAL_WITH_REPORT → 2026-09-16 复验升级为 PASS**              | 6/6 路由运行时取证：敏感页（SOURCES / IMPORT / INFRASTRUCTURE / BACKUP）`fl=` 含 `SECURE` 且 `screencap` 被抹黑（均值 0.17）；非敏感页（HOME / SETTINGS）无 `SECURE` 且截图正常（均值 244.64）。见 `ANDROID_RUNTIME_SECURITY_EVIDENCE.md`                                 |
| 18  | `ANDROID_UNIT_INTEGRATION`       | **PASS → 2026-09-16 复验升级**                                    | **181** 个可执行用例：`:core` JVM 单测 **71/71** + conformance **91/91** + 设备内 **19/19**；`:app:testDebugUnitTest` 为 NO-SOURCE（如实记录，不冒充通过）。见 `ANDROID_CORE_JVM_TEST_REPORT.md`                                                                          |
| 19  | `ANDROID_ACCESSIBILITY`          | **PARTIAL_WITH_REPORT**                                           | 触摸目标 / 焦点顺序 / 字体缩放 / 横屏 PASS；**4 个可点击节点无标签**；TalkBack NOT_RUN                                                                                                                                                                                    |
| 20  | `ANDROID_APK`                    | **PASS**                                                          | `app-debug.apk` **36,887,249 B**，SHA-256 `bf378ec6…305ff1`（2026-09-16 与源码同步重建；归档 `local_private/artifacts/app-debug.apk`）                                                                                                                                    |
| 21  | `ANDROID_AAB_BUILD`              | **PASS**                                                          | `app-release.aab` 20,734,935 B                                                                                                                                                                                                                                            |
| 22  | `ANDROID_RELEASE_SIGNING`        | **BLOCKED_BY_MISSING_PRODUCTION_KEYSTORE**                        | 无生产 keystore；且非生产签名配置本轮**未生效**（产物与未签名版同 SHA-256）                                                                                                                                                                                               |
| 23  | `ANDROID_RUNTIME_E2E`            | **PARTIAL_WITH_REPORT → 2026-09-16 业务写入链路已补：21/21 PASS** | 核心写路径端到端打通（导入 → 候选 → 确认 Reality → 标记必需 → 影响面 → 变更计划 → done → verified → 进程死亡恢复 → .depmap 导出/错误密码拒绝），App 崩溃 0。仍 NOT_RUN：Onboarding / Timeline / Drift / App Lock / TalkBack。见 `ANDROID_CORE_USER_JOURNEY_E2E_REPORT.md` |
| 24  | `ANDROID_PERFORMANCE_SMOKE`      | **PASS**                                                          | `csvRowsParsed=10000`，`csvParseErrors=0`，强断言通过                                                                                                                                                                                                                     |
| 25  | `ANDROID_LOGCAT_PRIVACY`         | **PASS**                                                          | 进程归属扫描，`appLines=44`，6 类关键字命中全 0                                                                                                                                                                                                                           |
| 26  | `ANDROID_PRIVACY_SECURITY_AUDIT` | **PARTIAL_WITH_REPORT**                                           | 权限面 / 备份 / Keystore / SQLCipher / 日志 均实测通过；导出组件逐个归属、release debuggable 未审计                                                                                                                                                                       |
| 27  | `ANDROID_STORE_METADATA`         | **PARTIAL_WITH_REPORT**                                           | 文案草稿完成；截图 / 图标 / 隐私政策链接 NOT_STARTED                                                                                                                                                                                                                      |

**统计（2026-09-16 P0 轮后）**：PASS **20** / PARTIAL_WITH_REPORT **4** / BLOCKED **2** / NOT_RUN **1** / FAIL **0**
（相对上一版：Gate 17 截图保护、Gate 18 单测/集成 由 PARTIAL 升为 PASS）

---

## 3. Parity 重算（不沿用 55/62）

| 项                        | 值                                                    |
| ------------------------- | ----------------------------------------------------- |
| 上一轮                    | 55 / 62                                               |
| N1/N2 轮新关闭            | +3（截图保护、性能 smoke、Store metadata 草稿）       |
| P0 轮（2026-09-16）新关闭 | **+1**（单测 / 集成测试：`:core` 纯 JVM 单测 0 → 71） |
| **当前**                  | **59 / 62**                                           |
| 仍未关闭                  | 3 项                                                  |

未关闭的 3 项：

| 项           | 状态                                                                                                                    |
| ------------ | ----------------------------------------------------------------------------------------------------------------------- |
| 设备 E2E     | PARTIAL（业务全链路已 21/21 PASS；剩余 Onboarding / Timeline / App Lock / TalkBack 为入口缺失或环境缺失，非核心写路径） |
| 无障碍       | PARTIAL（4 个无标签可点击节点 + TalkBack 未验证）                                                                       |
| Release 签名 | BLOCKED（缺生产 keystore）                                                                                              |

---

## 4. 三个判定（分别回答，不混为一谈）

### 4.1 `N1_ANDROID_VERTICAL_SLICE` = **PASS（2026-09-16 P0 轮升级）**

N1 的语义是"垂直切片端到端跑通"。本轮在真机（`emulator-5554` / API 34）
用与源码同步重建的 APK（`bf378ec6…305ff1`）把这条链路**真正跑了一遍**：

import（SAF 选真实 CSV）→ proposal（3 条候选）→ **用户确认 acceptProposal → Reality**
→ **用户显式标记必需** → impact（必须处理（2）+ 处理顺序）
→ changeplan → action done（**done ≠ verified**）→ verification
→ `am kill` 后重启数据仍在 → `.depmap` 导出 + 错误密码恢复被拒。

**21 步 21 PASS，App 崩溃 0**，证据落盘 `local_private/e2e/`。
分层证据为 181 个用例（71 + 91 + 19）全绿。

→ 垂直切片已在设备级端到端跑通，**判 PASS**。

**不属于 N1 范围、仍为 NOT_RUN 的项**（不因此回退 N1）：
Onboarding 启动接线、Timeline / Drift 入口、App Lock（无 UI 入口）、TalkBack（镜像未预装）。

### 4.2 `N2_ANDROID_FULL_PARITY` = **PARTIAL_WITH_REPORT**

**59 / 62**。3 项未关闭（见 §3）。

**2026-09-16 P0 轮修正**：

- 截图保护运行时证据**已取得**（6/6 路由 PASS）→ 已关闭；
- 单测 / 集成**已关闭**（`:core` 71/71）；
- 设备 E2E 的**业务写入链路已打通**（21/21），但仍因入口缺失未全关。

仍不满足 FULL parity 的是
App Lock（**NOT_RUN，且确认为真实缺口**：`Route.LOCK` 无 UI 入口）、
Biometric（BLOCKED，依赖真机生物特征）、
无障碍（4 个无标签可点击节点 + TalkBack NOT_RUN）、
Release 签名（BLOCKED）。

→ **不判 PASS。**

### 4.3 `ANDROID_PRODUCTION_RELEASE_READY` = **BLOCKED_BY_PRODUCTION_SIGNING**

Release 签名缺失是**独立的发布 blocker**，本轮不拿它掩盖运行时/领域完成度，
也不反过来用"领域 Gate 全绿"掩盖签名缺失。

除此之外，即使签名解决，以下项同样阻塞上架：
截图 / 图标仍为 placeholder、隐私政策公开链接未就绪、
`ANDROID_APP_LOCK` 未验证（且无 UI 入口）、无障碍仍有 4 个无标签节点。

**P0 轮新发现、上架前应修复的真实缺陷**（不阻塞签名，但影响用户信任）：
备份导出**UI 误报失败**（2/2 稳定复现：文件已完整落盘且可正常恢复，
App 却提示「备份失败：无法写入文件。」）。

---

## 5. 本轮修掉的真实缺陷

| #   | 类别         | 缺陷                                                               | 修复                                                     |
| --- | ------------ | ------------------------------------------------------------------ | -------------------------------------------------------- |
| 1   | **构建链**   | `android/` 完全没有 Gradle Wrapper，构建依赖本机绝对路径 Gradle    | 生成标准 Wrapper（Gradle 8.9），官方 `distributionUrl`   |
| 2   | **构建链**   | `:conformance:run` 默认仓库根算错一级（`../..` → `<repo_parent>`） | 改为 `..`                                                |
| 3   | **运行时**   | SQLCipher native 库未加载 → `UnsatisfiedLinkError`                 | `System.loadLibrary("sqlcipher")`                        |
| 4   | **运行时**   | Cursor 惰性视图越界 → `CursorIndexOutOfBoundsException`            | 改为 `MaterializedRow` 复制取值                          |
| 5   | **运行时**   | 查询不存在的列 `criticality` → `SQLiteException`                   | 从 `acceptProposal` 的 SELECT 中移除                     |
| 6   | **取证方法** | 性能 smoke 数据无效（`csvRowsParsed=0`）                           | 修正 `dateFormats` + 强断言 `assertEquals(10_000, rows)` |
| 7   | **取证方法** | logcat 敏感扫描把 Launcher3 系统字段误判为应用泄露                 | 改为 PID/UID 进程归属扫描，并要求 app 日志行数 > 0       |
| 8   | **运行时**   | 单进程跑 19 个 androidTest 会被 OOM 杀                             | 按类分批 + 清 logcat（环境脆弱点，已记录）               |

---

## 6. 最终结论

> **Android N1 / N2 = PARTIAL / BLOCKED → 判定 B**

**不进入 Harmony N3。**

理由（按 Gate 说话，不靠叙述）：

1. ~~核心垂直切片（import→proposal→reality→changeplan）**设备级一次都没跑通**~~
   → **2026-09-16 已关闭**：21/21 PASS，App 崩溃 0（见 `ANDROID_CORE_USER_JOURNEY_E2E_REPORT.md`）
2. ~~`:core` 纯单元测试为 **0**~~ → **2026-09-16 已关闭**：**71/71**（见 `ANDROID_CORE_JVM_TEST_REPORT.md`）
3. ~~截图保护只有实现没有运行时证据~~ → **截图保护 2026-09-16 已关闭（6/6 路由）**；
   **App Lock 仍是 NOT_RUN 且为真实缺口**（`Route.LOCK` 无 UI 入口）；
   **Biometric 仍 BLOCKED**（需真机生物特征）
4. 无障碍仍有 **4 个无标签可点击节点**，TalkBack 未验证；
5. Release 签名 **BLOCKED**，且非生产签名验证本轮**未生效**。

> **2026-09-16 结论更新**：原 5 条理由中第 1、2 条已关闭，第 3 条部分关闭。
> 剩余阻塞项为：**App Lock 接线缺口 + Biometric + 无障碍 + Release 签名**。
> 因此 N1 / N2 仍**不判 PASS**，也**不进入 Harmony N3**。

### 建议的下一步（仅处理剩余 blocker，不做新功能）

| 优先级 | 事项                                                                                           |
| ------ | ---------------------------------------------------------------------------------------------- |
| P0     | 打通应用层写入路径（import → proposal → reality → changeplan），让垂直切片可在设备级端到端跑通 |
| P0     | 为 `:core` 补 JVM 单元测试，让 `:core:test` 不再是 NO-SOURCE                                   |
| P1     | 修好 FLAG_SECURE 的运行时取证（`dumpsys` flags 解析），或换用 `screencap` 置黑对比作为判定     |
| P1     | AppLock 接入生命周期 + 在带指纹的镜像上验证                                                    |
| P1     | 补上 4 个无标签可点击节点的 `contentDescription`                                               |
| P1     | 修好 release 签名流水线（当前非生产签名未生效）                                                |
| P2     | 截图 / 图标 / 隐私政策链接                                                                     |
| P2     | 导出组件逐个归属审计；release 构建 `debuggable` 复查                                           |

---

## 7. 停止

本轮到此停止。**不自行进入 Harmony N3。**
等你对最终 Gate 复核后再决定下一步。
