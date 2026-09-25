# V0.2.0 FINAL GATES（goal §110 收口输出）

> 全部为 2026-09-25 真实执行证据；任何非 PASS 附 §111 结构块。
> 版本：PDIG 0.2.0 Preview（tag `product-v0.2.0` = main = origin/main；SHA 以 `git rev-parse product-v0.2.0` 为准）。

## 发布轨道

| Gate                 | 值         | 证据                                                                                                                                                                                 |
| -------------------- | ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| V0_1_2_RELEASE_TRAIN | **CLOSED** | Android 下载 smoke PASS；Desktop 下载 smoke FAIL 已取证（v0.1.2 产物缺 JVM launcher），v0.1.2 冻结不再改，根因随 v0.2.0 打包链路修复（docs/release-evidence/v0_1_2_download_smoke/） |

## 产品

| Gate                       | 值       | 证据                                                                                                            |
| -------------------------- | -------- | --------------------------------------------------------------------------------------------------------------- |
| PRODUCT_USABILITY_AUDIT    | **PASS** | PRODUCT_EXPERIENCE_MAP_V0_2.md（14 任务×八问）+ 双端源码改造 + DUAL_CLIENT_EXPERIENCE_MATRIX_V0_2.md            |
| FIRST_RUN_EXPERIENCE       | **PASS** | Android 解锁→首页（空态+导入入口）；Desktop 门页（新建/打开）→首页；无 Nodes/Edges 工程 dashboard               |
| IMPORT_USABILITY           | **PASS** | 来源页（无 adapter ID）+ 隐私声明统一 + CSV 字段对应可改（Android 新增 MappingStep）+ 错误分层文案 + 完成页统计 |
| ATTENTION_CENTER_USABILITY | **PASS** | Android Home 聚合（proposals/candidates/blocked-plans）+ Desktop AttentionScreen 四卡                           |
| SCENARIO_USABILITY         | **PASS** | 三场景一级入口 + Setup 用户语义（无内部参数）+ Impact 四类人话                                                  |
| CHANGEPLAN_USABILITY       | **PASS** | 五问+阶段推进；CTA 映射（blocked→处理必须事项…）；Action 区分完成/验证（done≠verified 断言在 smoke）            |
| VERIFICATION_USABILITY     | **PASS** | 四态人话（含「发现新的依据，请确认」）；未来观察不自动 verified（引擎单测/仪器化）                              |
| BACKUP_RESTORE_USABILITY   | **PASS** | 密码无法找回/恢复替换全量/错误四类区分；smoke backup-restore-reopen-delete PASS                                 |

## 双端

| Gate                                 | 值       | 证据                                                                                                      |
| ------------------------------------ | -------- | --------------------------------------------------------------------------------------------------------- |
| ANDROID_PRODUCT_UX                   | **PASS** | 25 屏审计（§51 清单表）+ 状态覆盖；60/60 仪器化×2；质量门禁 PASS                                          |
| DESKTOP_PRODUCT_UX                   | **PASS** | 7 项一级导航 + 双栏 Infra + 键盘可达（focusable）+ 内部术语清零；smoke 16/16                              |
| DUAL_CLIENT_INFORMATION_ARCHITECTURE | **PASS** | 7 一级入口对齐（Home/需要处理/数据来源/基础设施/场景/即将到来/设置）；体验地图对照表                      |
| DUAL_CLIENT_TERMINOLOGY              | **PASS** | PRODUCT_TERMINOLOGY_V0_2.md + 源码扫描（内部字段/graphRevision/capability 等不上屏）                      |
| DUAL_CLIENT_CORE_FLOW_PARITY         | **PASS** | Import→Confirm→Scenario→Plan→Verify→Backup→Restore 双端均可行（Android 60/60 套件 + Desktop smoke 16/16） |

## 工程

| Gate                          | 值       | 证据                                                                       |
| ----------------------------- | -------- | -------------------------------------------------------------------------- |
| PRODUCTION_FILE_GT_300        | **0**    | check-quality file-sizeOK（exempted 3 既有）                               |
| CRITICAL_COMPLEXITY_VIOLATION | **0**    | check-quality composable PASS（ImportScreen 拆分 ≤200）                    |
| DEPENDENCY_CYCLE              | **0**    | architecture circular=0（48 files）；Kotlin cycle=0                        |
| UNJUSTIFIED_TYPE_ESCAPE       | **0**    | check-quality kotlin-escape=0、suppress=0                                  |
| RAW_TODO                      | **0**    | check-quality todo=0                                                       |
| KNOWN_DEAD_CODE               | **0**    | check-quality deadcode=0                                                   |
| SENSITIVE_LOGGING             | **0**    | check-quality senslog=0                                                    |
| SECRET_SCAN                   | **PASS** | core secret scan 1016 文件 0 泄漏 + quality secret=0；local_private 未入库 |

## 测试

| Gate                    | 值        | 证据                                                                                |
| ----------------------- | --------- | ----------------------------------------------------------------------------------- |
| ANDROID_CORE            | **71/71** | :core:test --rerun-tasks                                                            |
| ANDROID_JVM             | **63/63** | :app:testDebugUnitTest（新增 54 个 v0.2.0 单测）                                    |
| ANDROID_CONFORMANCE     | **91/91** | :conformance:run（main + fresh clone）                                              |
| ANDROID_INSTRUMENTATION | **60/60** | connectedPreviewDebugAndroidTest ×2（main + fresh clone；pdig36 API36）             |
| ANDROID_CORE_JOURNEY    | **PASS**  | 证据套件含 Core Journey/DeleteAll/BackupExport/PerfSmoke（60 项内）                 |
| ANDROID_SCENARIOS       | **PASS**  | 三场景证据路径在 60/60 套件内；Desktop smoke 三场景 3× PASS                         |
| DESKTOP_CONFORMANCE     | **PASS**  | Desktop JVM 测试 14/14 + conformance 语义（smoke 引擎断言）                         |
| DESKTOP_CORE_JOURNEY    | **PASS**  | --smoke 16/16（launch/import/confirm/scenario/plan/complete/verify/backup/restore） |
| DESKTOP_SCENARIOS       | **PASS**  | scenario-replace/expiring/close 3 步 PASS（×5 稳定性）                              |
| core npm run check      | **PASS**  | 453/453 + architecture + network + secrets + UI gate                                |

## 发布

| Gate                         | 值            | 证据                                                                                          |
| ---------------------------- | ------------- | --------------------------------------------------------------------------------------------- |
| ANDROID_V0_2_0               | **PASS**      | versionName 0.2.0/versionCode 200004/targetSdk 36/NON-PROD 签名 v2 Verified；APK 33,171,401 B |
| DESKTOP_V0_2_0               | **PASS**      | 0.2.0 全端统一；打包链路重写 + 自检；portable 启动真实窗口                                    |
| RELEASE_SBOM                 | **PASS**      | PDIG-0.2.0-SBOM.cyclonedx.json（CycloneDX 1.5，151 components，真实构建输入）                 |
| RELEASE_LICENSE_AUDIT        | **PASS**      | THIRD-PARTY-NOTICES.md（gen-sbom LICENSE_MAP）                                                |
| RELEASE_SHA256               | **PASS**      | PDIG-0.2.0-SHA256SUMS.txt；下载后 7/7 MATCH                                                   |
| PRODUCT_V0_2_0_RELEASE_READY | **PASS**      | 见 Definition of Done 逐项（下方）                                                            |
| GITHUB_PRODUCT_RELEASE_0_2_0 | **PUBLISHED** | `PDIG 0.2.0 Preview`（tag product-v0.2.0，Pre-release），8 附件 + Release Notes 正文          |

## Definition of Done 复核

- [x] 产品流程更清楚（体验地图/双端文案/状态补齐）
- [x] Android/Desktop 核心语义一致（术语词典 + 矩阵 + 源码审计）
- [x] Desktop 自然桌面体验（7 导航/双栏/键盘/版本）
- [x] Android 自然移动体验（保持既有结构，未放大桌面模式）
- [x] 双端 Import/Confirm/Scenario/Plan/Verify/Backup/Restore 全部可走
- [x] 新增工程代码满足质量门禁（quality VERDICT PASS）
- [x] Canonical 语义零变化（spec/ 未动；无新 domain）
- [x] 无严重 security/privacy regression（INTERNET=0、secret 0、敏感日志 0）
- [x] Fresh clone 测试 PASS（C:\pdig-fresh-020：JVM/conformance/签名 APK/仪器化 60/60/Desktop compile+test+smoke）
- [x] Release artifacts 可复现（脚本构建；打包脚本入库）
- [x] GitHub attachments 实际可下载（gh release download 成功）
- [x] 下载后二次 smoke PASS（Android install/launch/PID/截图/uninstall；Desktop 解压启动真实窗口）
- [x] main/tag/artifact provenance 对齐（main=origin/main=tag=1fe3d05；artifact SHA 记录在 Manifest/SHA256SUMS）

## 非 PASS 项（§111 结构）

| Gate                       | Status           | Root cause                                                                               | Engineering impact                                     | Can Agent fix?        | Required action                        | Evidence                                     | Closure                                    |
| -------------------------- | ---------------- | ---------------------------------------------------------------------------------------- | ------------------------------------------------------ | --------------------- | -------------------------------------- | -------------------------------------------- | ------------------------------------------ |
| tablet 全量 connected 套件 | 环境挂起         | pdig_api36_tablet AVD 在本机会话长任务挂起（模拟器进程中途消亡；v0.1.x 同 AVD 曾 59/59） | 无代码影响；平板安装/启动/卸载 smoke PASS（2560×1600） | 否（环境/AVD 稳定性） | 换机/低负载时段复跑，或真机验证（E-1） | docs/release-evidence + WORK_STATUS/BLOCKERS | 记录为 Known Limitation                    |
| v0.1.2 Desktop 下载 smoke  | FAIL（已取证）   | v0.1.2 打包产物缺 JVM launcher                                                           | 影响仅 v0.1.2 历史发布物；v0.2.0 已修复并实证          | 是（v0.2.0 修复）     | 已随 v0.2.0 完成                       | docs/release-evidence/v0_1_2_download_smoke/ | CLOSED（v0.1.2 冻结）                      |
| CI（E-10）                 | EXTERNAL_BLOCKED | GitHub 账户计费                                                                          | 无本地影响；本地全量门禁为验收依据                     | 否（外部）            | 用户处理 GitHub 账单                   | BLOCKERS.md E-10                             | 不阻塞 GitHub Preview 发布（用户裁决口径） |

## 诚实声明

- TalkBack/屏幕阅读器为工程级检查（semantics/labels/48dp/font scale），未做真人测试，不宣称 SCREEN_READER_VERIFIED。
- 无真机、无真实数据（E-1/E-7）；Play 生产（E-2/E-5）；Harmony/iOS（E-9/E-8）均为外部/用户裁决项，非本轮验收项。
