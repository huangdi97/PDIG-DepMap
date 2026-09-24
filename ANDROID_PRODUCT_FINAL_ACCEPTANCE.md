# ANDROID_PRODUCT_FINAL_ACCEPTANCE.md

> 生成时间：2026-09-21（Android Product Finalization 收口）
> **更新：2026-09-23（ANDROID API36 工程全速收口轮）**——compileSdk/targetSdk→36（Android 16），
> API36 双 AVD 全量回归（androidTest 59/59×2、Core Journey 41/41、三场景 32/32）、
> edge-to-edge / predictive back / adaptive layout / D-16 四项 PASS、
> NON-PRODUCTION 签名链路验证（apksigner v2 + signed AAB）、
> **parity 保持 69/73**（剩余 4 格仍为外部 blocker / 政策项）、
> `ANDROID_GOOGLE_PLAY_RELEASED` 仍如实 BLOCKED（无 Play 账号 / applicationId 未定 / 无生产 keystore / 无真机 / 无真实数据）。
> 执行环境：Windows 11，仓库 `E:\AI\号卡管理`（非 ASCII 路径，构建输出重定向 `%USERPROFILE%\pdig-build`）
> 分支：`feat/android-production-release` · 本轮 HEAD：`b13f2f7` + 文档 commit（以 `git rev-parse HEAD` 为准）
> 设备：AVD `pdig_api36_phone`（Android 16/API36，1080×2424）+ `pdig_api36_tablet`（API36，2560×1600）
> 结论先行：**ANDROID_PRODUCT_COMPLETE = PASS**；**ANDROID_API36_READY = PASS（工程可验证范围）**；剩余项全部为 external blocker

---

## 1. Executive Summary

本轮把 Android（Kotlin/Compose 原生）从「功能可用」推进到「产品收口可发布（除用户外部输入外）」。
实现了：三个 active 场景全闭环 E2E、候选/漂移用户流、删除所有数据、设备语言收口、Onboarding 正式取消、
生物识别设备验证、Dark Mode 设备验证、CI 扩大至 app JVM + assembleDebug、全套发布文档。
自基线 62/73 起，本轮逐格重审后 parity **69/73**，**ENGINEERING_GAP = 0、TEST_EVIDENCE_GAP = 0**。

## 2. Canonical HEAD

`4f7b6d0`（最终验收 HEAD；报告本身不写死自身，以 `git rev-parse HEAD` 为准）。
本轮提交：`d88b831`（H-16/H-17/E-10/D-9）→ `3d371b2`（L-37 + 审计文档）→ `504173f`（L-37 设备验证）→
`ba6b3fe` + `82db491`（CI 扩大 + 修复）→ `61ecdd6`（三场景 E2E 报告）→ `4f7b6d0`（验收文档收口 + parity 69/73 修正）。

## 3. GitHub state

- 远端 `huangdi97/PDIG-DepMap`（PRIVATE），分支 `feat/mvp03-living-graph` 已推送（HEAD `4f7b6d0`，与远端同步 0/0）。
- CI（workflow_dispatch 手动触发，仓库已知 push 触发不生效）：末期 run **35520532048 全绿**——
  `android-core`（:core:test + :conformance:run 91/91）、`android-app`（**:app:testDebugUnitTest + :app:assembleDebug，新增**）、
  `canonical`（codegen / fixtures / oracle）、`harmony-static` 全部 success。

## 4. Architecture

`android/`：`:core`（纯 Kotlin JVM 领域层：domain/impact/plan/scenario/statemachine/schema/json/sources/serialize/timeline/crypto，
硬约束不依赖 Android）、`:conformance`（JVM runner，读取仓库 fixtures 产出 android.json）、
`:app`（Jetpack Compose + SQLCipher + Android Keystore + BiometricPrompt + SAF）。
App Lock 是应用的门（`LockGate`），锁定时 NavHost 不参与组合（结构性不可绕过）。

## 5. Product capabilities（MVP01–MVP03 全部收口）

- 导入（微信/CSV/OFX-QFX，SAF，D-16 工作流跨锁存活）
- Proposal → 用户确认 → Reality（Proposal ≠ Reality）
- DiscoveryCandidate → 用户确认 → Node（本轮补齐入口+动作）
- RealityDrift → 用户选择（已更换/两者都在用/没变化/稍后确认）→ Reality mutation（本轮补齐）
- Impact（必须处理/建议检查/备用路径·降级/未受影响，只从 Confirmed Reality 出 must_change）
- ChangePlan + Action done + Verification（done ≠ verified）
- Timeline（纯投影）
- .depmap 加密备份/恢复（AES-256-GCM + Argon2id，错误口令/篡改拒绝）
- App Lock（生物识别/设备凭据/无凭据三态，删除所有数据）

## 6. 73 parity result

**69 / 73**（逐格重审，非推算）：领域 16/16、持久化 10/10、安全 10/10、导入 7/7、UI 21/22、工程 5/8 计入。
未完成 4 格 = TalkBack 实机读屏（环境）、production keystore（用户）、Store 截图/图标/公开 URL（品牌决策）、
R8（release 未开 minify，如实 NOT_APPLICABLE）。详见 `ANDROID_FINAL_73_AUDIT.md`。

## 7. Core tests（:core:test）

**71 / 71 PASS**（DomainInvariant 15 / ImpactKernel 11 / PlanReadiness 14 / MigrationSemantics 10 / GraphRevisionSemantics 7 / StateMachine 14）。`ANDROID_CORE_JVM_TEST_REPORT.md` 同期有效。

## 8. App JVM（:app:testDebugUnitTest）

**9 / 9 PASS**（FileWorkflowStateTest，D-16 工作流状态机）。本轮在 hosted CI 上同样执行（android-app job）。

## 9. Android device tests（connectedDebugAndroidTest）

**59 / 59 PASS**（0 skip/0 fail），4 批取证 + pm clear 隔离：AccessibilitySemantics 14、AppLockNavigation 7、
BackupExport 3、**CandidateDrift 7（本轮新增）**、**DeleteAllData 1（本轮新增）**、DepmapRuntime 4、
FileWorkflowD16 6、ImportHitbox 2、PerfSmoke 1、Persistence 8、RepositoryKeystore 4、ScreenProtection 2。

## 10. Conformance

**:conformance:run = 91 / 91 PASS**（impact 13 / readiness 16 / coverage 6 / relations 18 / depmap 3 /
jcs 1 / scenario 1 / migration 2 / state-machine 5 / parser 22 / timeline 3 / backup 1）。`conformance/reports/android.json` 生成时间 2026-09-20T14:40。

## 11. Core Journey E2E

`core-journey-v4-20260920-200002` = **41 / 41 PASS / 0 FAIL**。覆盖：锁（冷启动/前后台/二次解锁）、
全新安装、导入（D-16 双断言×pick）、Node Resolution、确认 Reality、标记必需、影响面、创建计划、
done≠verified、验证、进程死亡（am kill 重建后锁屏+数据在）、导出、错误口令拒绝、恢复、语义等价、篡改拒绝、崩溃扫描 0。

## 12. Three scenarios（G-13）

`scenario-e2e-20260921-151631` = **40 / 40 PASS / 0 FAIL**。
`replace_payment_card`、`expiring_payment_card`、`close_payment_instrument` 各自完整闭环：
Scenario Setup → 选支付工具 → 创建计划 → done（2 动作）→ done≠verified → verified（2 动作）→ 回首页。详见 `ANDROID_SCENARIO_E2E_REPORT.md`。

## 13. Import

统一流程（Source → privacy notice → picker → parser → preview → mapping → resolution → Proposal → explicit confirmation）
在设备 E2E 全链路通过；Node Resolution 2 支付方式/3 收款对象 →「记录 6 行」。

## 14. Parser

WeChat / Generic CSV / OFX/QFX 全部 conformance PASS（28 个原始 fixture，BOM/CRLF/CR-only/GB18030/借贷列/多币种/分号/坏行全过）。
**Base64 remainder truncation 专项（I-20）**：Android 无该通道（conformance 直读字节；crypto 用 JDK RFC4648 严格解码），证据见 73 audit 附录 A。

## 15. Repository

AppContainer 封装 driver：reality mutation 与 graphRevision bump **同事务**（acceptProposal / upsertNode /
drift resolve 均验证）；Proposal/Candidate/Drift 记录不 bump。`CandidateDriftEvidenceTest` 设备 7/7 覆盖。

## 16. SQLCipher

`net.zetetic:sqlcipher-android:4.5.5`。明文 `sqlite3` 打不开密文库（PersistenceEvidenceTest）；WAL/SHM 处理正确。

## 17. Migration

v1→v2→v3（conformance migration + JVM MigrationSemantics）；payload v1/v2→v3 内存迁移（设备）；
未来版本拒绝且不 wipe（设备）。Schema v3 逻辑三端一致。

## 18. Crypto

DEPMAP_CONTAINER_V1：Golden Vector 派生键/密文/tag 逐字节一致；口令 UTF-8 不做归一化（5 版本派生键同 oracle）；
恶意容器 bounds 前置校验（15 种坏容器 fail fast）；错误口令与篡改同为 auth_failed。

## 19. Backup / Restore

设备 E2E J10/J11 + BackupExportRegressionTest 3/3：导出 13,617 B、UI=备份已导出、错误口令拒绝、
正确口令恢复 29 条、清空后恢复语义等价、篡改容器被拒（无法恢复：密码错误、文件损坏，或版本不受支持）。

## 20. App Lock

`AppLockNavigationTest` 7/7 + E2E J0：冷启动先锁、解锁后才进入、前后台回锁、锁定时 NavHost 不参与组合；无凭据三态放行。

## 21. Biometric

**本轮从 PARTIAL 升级 RUNTIME_VERIFIED**：API35 google_apis_playstore AVD（`hw.fingerprint=yes`，
真实录入 1 枚指纹）跑通：指纹成功（accept→解锁到首页）、指纹失败（reject→停留锁屏）、取消（Back→回锁屏+『已取消验证』）、
PIN 正确（1234→解锁）、PIN 错误（9999→拒绝）、前后台回锁。真机补充可留（见 §39）。

## 22. Accessibility

Compose 语义树 14 屏 0 无标签可交互节点（AccessibilitySemanticsTest 14/14）；触摸目标 ≥48dp；
字体缩放/焦点顺序/横屏设备 PASS。**TalkBack 实机读屏 = NOT_RUN（环境受限）**——按 Goal 口径记为环境受限项而非工程缺口。

## 23. Lifecycle

进程死亡（E2E J9）、前后台（J0）、Activity recreation / 外部 picker（D-16 FileWorkflowD16Test 6/6）、
恢复向导跨锁存活；敏感内容不进 SavedStateHandle。

## 24. Performance

设备 PerfSmoke 10k CSV 全解析 0 错误（强断言）；oracle perf 套件 4/4（500 drifts 3744ms、1k-node rebase 73ms）；
冷启动存在 AVD 首次建库 ANR 窗口（驱动已处理），无产品级 ANR 缺陷（主线程零 DB 读写）。

## 25. Stability

设备批 59/59 两次独立完整运行；Core Journey 41/41；三场景 40/40；崩溃扫描 0；0 corruption。

## 26. Permission

`ANDROID_PERMISSION_FINAL_AUDIT.md`：仅 USE_BIOMETRIC/USE_FINGERPRINT；无 INTERNET、无存储权限、
无 exported 非必要组件、allowBackup=false + data extraction rules 全 exclude。

## 27. Network

无 INTERNET 权限、主机代码 0 网络原语（core check:network PASS）、无 analytics/telemetry/广告 SDK；
LOG 层零业务日志（唯一 Log.w 是备份失败的 stage+异常类型，无敏感内容）。

## 28. Logging / privacy

release+debug 均按 app PID/UID 归属扫描，6 类敏感关键字命中全 0；错误信息以 stage+errorCode 呈现。

## 29. Data deletion

Settings →「删除所有数据」+ AlertDialog 确认；删除 DB/WAL/SHM/journal/包裹口令/Keystore 别名/cache/filesDir；
**不删**用户导出到外部位置（app-external / 外部存储）的 `.depmap`。`DeleteAllDataEvidenceTest` 设备 1/1。

## 30. Dependency / license / SBOM

`ANDROID_DEPENDENCY_LICENSE_REPORT.md`：直接依赖 18 项（Compose BOM/AndroidX/SQLCipher/BouncyCastle…）+ 传递依赖清单；
全部 Apache-2.0 / BSD-3 / BouncyCastle / EPL；无网络库/分析/广告；CVE 在线扫描 = 如实 NOT_RUN_ONLINE（非伪造）。

## 31. APK / AAB

| 产物                      | 大小         | SHA-256（前 16） |
| ------------------------- | ------------ | ---------------- |
| app-debug.apk             | 36,974,885 B | `030BD9E3…`      |
| app-release-unsigned.apk  | 33,130,413 B | `C659F077…`      |
| app-release.aab           | 20,862,091 B | `A24593A6…`      |
| app-debug-androidTest.apk | 1,135,942 B  | `7E641719…`      |

（fresh clone 构建；settings.gradle.kts ASCII 重定向为项目内建，产物路径以实际构建输出为准记录。）

## 32. Signing

`NON_PRODUCTION_TEST_SIGNING = PASS`（pdig-nonprod.jks 本地验证）；`PRODUCTION_SIGNING = BLOCKED_BY_MISSING_PRODUCTION_KEYSTORE`（用户提供）。
`ANDROID_PRODUCTION_SIGNING_RUNBOOK.md` 给出 keystore 生成、本地/CI 注入、Play App Signing、备份轮转完整手册。

## 33. UI / UX

`ANDROID_UI_PRODUCT_AUDIT.md`：21 个产品目的地 0 ghost 路由；每页 empty/loading/success/error/back/滚动/insets/
大字体/过程恢复核查；人话语言收口（不显示 graphRevision 等内部术语）；首页按产品首页聚合 attention。

## 34. Brand

launcher icon / adaptive / monochrome / splash / app name 均为**占位** → `NOT_FINAL_BRAND_ASSET`；
`ANDROID_BRAND_ASSET_REQUIREMENTS.md` 给出规格，最终品牌由用户决策。

## 35. Store metadata

`ANDROID_STORE_METADATA.md`：short/full description、隐私摘要、权限说明、场景清单（本轮修正为 3 个支付场景）、
release notes 模板、合规红线齐备；截图/图标/公开 URL 待用户（STORE_PREPARATION）。

## 36. Privacy policy

`PRIVACY_POLICY_DRAFT.md`：仅按真实能力撰写（local-first、无网络、导入仅内存、加密备份、删除数据）；
公司主体/联系方式/URL = TODO（不虚构法律主体）。

## 37. Real device

`adb devices` 只有 AVD → **ANDROID_REAL_DEVICE_VERIFIED = BLOCKED_BY_MISSING_REAL_DEVICE**（外部 blocker）。
真机 Gate 清单（install/launch/App Lock/Biometric/Import/Scenario/Impact/Plan/Verification/Backup/Restore/
process death/TalkBack/performance smoke）已列出，等待用户提供真机。

## 38. Real data

无授权真实账单 → `REAL_DATA_CORRECTNESS = BLOCKED_BY_REAL_DATA`、`REAL_DATA_VALUE = BLOCKED_BY_REAL_DATA`。
`ANDROID_REAL_DATA_PILOT_PROTOCOL.md` / `ANDROID_REAL_DATA_PRIVACY.md` / `ANDROID_REAL_DATA_ACCEPTANCE.md` 就绪。

## 39. External blockers（用户本人才能解除）

1. **production keystore**（B4）→ 生产签名
2. **Play/华为开发者账号**（B5/B6）→ 上架
3. **正式 applicationId / 品牌 / 图标 / splash**（R-1..R-3, B11/B14-B16）→ 不可逆身份与素材
4. **public privacy/support URL**（B12/B12b）→ 商店必填
5. **真实 Android 手机**（B18）→ 真机 Gate + TalkBack 实机
6. **授权真实账单**（B13）→ REAL_DATA 指标
7. **Store 人工提交**（B17）→ Google Play 控制台

## 40. ANDROID_PRODUCT_COMPLETE 判定

| 项                                                   | 结果                                         |
| ---------------------------------------------------- | -------------------------------------------- |
| MVP01–03 功能全部实现                                | ✅                                           |
| 核心 UI 全部可达                                     | ✅（21 目的地，0 ghost）                     |
| 三个 active 场景全流程                               | ✅（40/40）                                  |
| Import 完整                                          | ✅                                           |
| Reality Boundary 正确                                | ✅                                           |
| Impact / ChangePlan / Verification 正确              | ✅（conformance + 设备）                     |
| Backup/Restore 正确                                  | ✅（41/41 含错误口令/篡改拒绝）              |
| Migration / Persistence 正确                         | ✅                                           |
| App Lock / Privacy 正确                              | ✅（59/59 + 权限审计）                       |
| Accessibility 工程内可完成项                         | ✅（语义树 14/14；TalkBack 实机=环境受限项） |
| Process death 可靠                                   | ✅（E2E J9）                                 |
| Performance / Stability 无 blocker                   | ✅                                           |
| Release build 可复现                                 | ✅（三产物 + SHA256）                        |
| Permission / Network / Logging audit                 | ✅                                           |
| Dependency / license / SBOM                          | ✅                                           |
| Product UI QA / Store metadata draft / Privacy draft | ✅                                           |
| **剩余全部为 external blocker**                      | ✅（§39 七项，均需用户提供）                 |

```
ANDROID_PRODUCT_COMPLETE = PASS
ANDROID_SOURCE_READY      = PASS（fresh clone 源码 + 全套文档）
ANDROID_BUILD_READY       = PASS（assembleDebug/Release/bundleRelease 可复现）
ANDROID_INSTALL_READY     = PASS（debug APK 可安装运行于 API34/35）
ANDROID_EMULATOR_VERIFIED = PASS（API34 + API35（指纹）AVD 全流程验证）
ANDROID_REAL_DEVICE_VERIFIED = BLOCKED_BY_MISSING_REAL_DEVICE
ANDROID_SIGNING_READY     = BLOCKED_BY_MISSING_PRODUCTION_KEYSTORE（流程就绪 + runbook）
ANDROID_STORE_METADATA_READY = PARTIAL（文案齐，素材/URL 待用户）
ANDROID_STORE_ASSETS_READY   = BLOCKED_BY_BRAND_DECISION（图标/splash/feature graphic）
ANDROID_STORE_SUBMISSION_READY = BLOCKED（需开发账号 + 正式身份）
ANDROID_PRODUCTION_RELEASE_READY = BLOCKED_BY_PRODUCTION_SIGNING
REAL_DATA_CORRECTNESS / REAL_DATA_VALUE = BLOCKED_BY_REAL_DATA
```

## 41. Exact next human actions（按优先级）

1. 提供 **production keystore**（见 runbook §1 或按 §1.1 生成）→ 我才能产出并验证签名 APK/AAB。
2. 决定 **applicationId + 对外版本号 + 应用名**（`ANDROID_RELEASE_DECISIONS_REQUIRED.md` R-1..R-5）。
3. 提供 **品牌图标素材**（前景/背景/monochrome + splash + feature graphic）。
4. 提供一台**真实 Android 手机**（开发者模式）→ 完成真机 Gate + TalkBack/Biometric 真机验证。
5. 提供 **1 份本人授权账单**（微信/OFX/CSV）→ 执行真实数据 Pilot。
6. 建 **Play 开发者账号**并选 Play App Signing → 我走上传/商店材料提交。
7. 提供或托管 **privacy policy + support URL**。

> 以上完成后，`ANDROID_STORE_SUBMISSION_READY` 即可翻转；在此之前所有结论已如实标注 BLOCKED/PARTIAL，无虚标。
