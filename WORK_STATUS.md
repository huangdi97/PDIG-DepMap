# WORK_STATUS.md

> 本文件由执行 Agent 持续更新。不要删除历史关键结论。
>
> **⚠ 2026-09-15 技术栈已变更**：Production 切换为三端原生（Kotlin/Swift/ArkTS），
> 彻底退出 uni-app x / UTS / DCloud。本文件 2026-09-15 之前的内容属于
> **Legacy 阶段**，其结论对旧实现仍然有效，但**不再是产品未来**。
> 当前进度见 `NATIVE_MIGRATION_STATUS.md`。
> 详见 `GOAL_PDIG_NATIVE_MIGRATION.md`、`docs/ADR_NATIVE_MIGRATION.md`。
> 分支历史：`feat/mvp02-global-source`（MVP02，tag v0.2.0-mvp02）→ `engineering/baseline-v1`
> （Engineering Baseline V1 PASS，2026-09-13）→ `feat/mvp03-living-graph`（MVP03，tag v0.3.0-mvp03）
> → `feat/mvp03-living-graph`（Production RC V1，2026-09-13）
> → `feat/mvp03-living-graph`（FINAL PRODUCTION CLOSURE V1，2026-09-14）
> → `feat/mvp03-living-graph`（PLATFORM BRINGUP，2026-09-15，**成果零提交**）
> → `feat/mvp03-living-graph`（AGENT HANDOFF PLATFORM BRINGUP / PRODUCTION RC CONTINUE）
> → **PDIG NATIVE MIGRATION N1 Android 垂直切片 + N2 全量 parity**
> → **`feat/mvp03-living-graph`（ANDROID N1 / N2 RUNTIME CLOSURE，2026-09-15，当前）**
> —— 本轮只做运行时取证与构建链收口，**不新增功能、不进入 Harmony N3**。
> 结论：`N1 = PARTIAL_WITH_REPORT`、`N2 = PARTIAL_WITH_REPORT`、parity **58/62**、
> `ANDROID_PRODUCTION_RELEASE_READY = BLOCKED_BY_PRODUCTION_SIGNING`。
> 完整 27 Gate 见 `ANDROID_N1_N2_FINAL_CLOSURE_REPORT.md`。

> **（当前）ANDROID 冻结 + 正式进入 Harmony N3，2026-09-17 第二场**
> —— 人工 Final Acceptance 结论已落地：`N1 = PASS`、`N2 = PARTIAL_WITH_REPORT (62/73)`、
> `ANDROID_PRODUCTION_RELEASE_READY = BLOCKED_BY_PRODUCTION_SIGNING`、D-16 CLOSED。
> 本轮**不再把 Android 62/73 往 73/73 堆**。
> ① 新增 `ANDROID_NATIVE_CORE_HANDOFF = PASS`（**不替代** N2 / release readiness）
> → `ANDROID_NATIVE_CORE_FREEZE.md`；Android 转入 `CORE_FROZEN / MAINTENANCE_ONLY`；
> 剩余 11 格按 ENGINEERING_NOT_YET_VERIFIED(7) / RUNTIME_ENVIRONMENT_BLOCKED(2) /
> RELEASE_EXTERNAL_BLOCKED(1) / STORE_PREPARATION(1) 分类保留在 N2 Backlog。
> ② **Git 尾项收口**：实查 HEAD `bc2eeb8` → 提交 `6053f3c`（三端 codegen 产物 + `legacy/README.md` 入库，
> 均为 `codegen --check` 验证的正式产物）；`.pi/` 保持 intentionally-untracked（gitignore 覆盖）。
> ⚠ 本轮再次复现既有 Git 故障：`git commit` 成功建对象但 HEAD 不推进 —— 已核实
> `6053f3c` 的 parent/tree 后用 `.git/packed-refs` + loose ref 修正并复核通过。
> ⚠ `packed-refs` 必须写**完整 40 位 SHA**（曾误写短哈希导致 HEAD 无法解析，已修复）。
> ③ **Android 取证口径修正（重要）**：`android/settings.gradle.kts` 把构建输出重定向到
> `%USERPROFILE%/pdig-build/<module>`，**`android/**/build/**` 是自重定向后的过期残留**。
> 既往报告里 `d84d8900…` / `bf378ec6…` 等 APK 哈希来源不明，本轮起作废。
> 本轮实测：`:core:test` 71/71、`:app:testDebugUnitTest` 9/9、`:conformance:run` 91/91、
> 设备内 androidTest 51/51（emulator-5554），APK/AAB 哈希见冻结报告。
> ④ **正式进入 Harmony N3**：`harmony/` 由「仅 1 个 codegen 文件」建成可真实构建的
> Stage Model 工程 —— hvigor 全清重建 `BUILD SUCCESSFUL`，产出 HAP **60,133 B**；
> 首个纯 ArkTS Domain（`Relations.ets`）已编译并打包进 HAP。
> `HARMONY_BUILD = PASS`；`HARMONY_DEPMAP = BLOCKED`（cryptoFramework 无 Argon2）；
> `HARMONY_RUNTIME_E2E = RUNTIME_NOT_RUN`（无模拟器镜像，`hdc list targets = [Empty]`）。
> 按 stop condition **未进入 iOS N4**，等 Harmony Gate 复核。
>
> **（当前）PART A GitHub 发布收口 + PART B Harmony N3 Argon2 深挖，2026-09-18**
> —— ① **GitHub 发布完成**：历史净化门禁全 PASS 后首次 push 到 `huangdi97/PDIG-DepMap`
> （PRIVATE，5 分支 + 3 标签，**全程快进、无 force**）。
> CI 三轮：首轮 FAIL（2 个真实仓库缺陷）→ 二轮 FAIL（Android job 转绿，Canonical 仅剩
> `fixtureIntegrity`）→ **三轮全绿**（`cases 91/91`、`imports 28/28`、oracle PASS、
> `platform android PASS 91/91`、`VERDICT: PASS`）。
> 产出 `GITHUB_SECRET_PRIVACY_AUDIT.md` / `GITHUB_HISTORY_SANITIZATION_REPORT.md` /
> `GITHUB_PUBLICATION_REPORT.md`。
> ② **定因并修复了既有 fixture 缺陷**：`fixtures/import` 12/28 的 sha256 与清单不一致，
> 根因**不是字节漂移**，而是清单记录了生成机 `core.autocrlf=true` **检出态的 CRLF 假象**
> （blob 级取证：12 个文件 HEAD blob == 引入提交 `97a0348` blob、均无 CR；
> 对照 `csv-cr-only.csv` 净化后仍保留 CR → 净化不剥离 CR；12/12 命中
> `sha256(CRLF(当前字节)) == 清单原值`）。按 canonical blob 字节**修正清单 12 条哈希**
> （`oracle.commit` 未动），不改字节、不降门禁。
> ③ **Harmony N3 Argon2 深挖**：`cryptoFramework` / `HUKS` 无 Argon2（证据级排除）；
> `hash-wasm` 的 `argon2.c` 是 WASM 实现**不能**作原生源；**NDK 路径可行** ——
> 主机侧 PHC 参考实现**逐字节复现 Golden Vector**（`MATCH=YES`，version 19），
> OHOS arm64 交叉编译产出 `libargon2_ohos.so`（ELF64/AArch64/仅依赖 libc.so）
> 与 NAPI 桥接 `libpdiargon2.so`（8 个 `napi_*` 由 Ark 运行时解析）。
> `HARMONY_DEPMAP`：`BLOCKED` → **`BLOCKED_BY_NATIVE_VERIFICATION`**；
> `HARMONY_RUNTIME_E2E` 仍为 `RUNTIME_NOT_RUN`（无设备/镜像）。
> Android 保持 **CORE_FROZEN**，**未进入 iOS N4**。
> 完整内容见 `HARMONY_ARGON2_FEASIBILITY.md`。
>
> **（历史）ANDROID FINAL BLOCKER CLOSURE — D-16 关闭轮，2026-09-17**
> —— 关闭 D-16（导入 / 恢复向导在"锁定—解锁"过程中被整体丢弃），采用**方案 A**：
> 把 Import / Restore 的外部文件工作流状态提升到 Activity 作用域
> （`FileWorkflowCoordinator` + `LocalFileWorkflow`），并把 `ActivityResult` 注册
> 移到 `MainActivity`（不随 NavHost 的 uncompose 被注销）。
> **明确不采用**方案 B（锁定时继续组合 NavHost 靠遮罩隐藏）与方案 C（拉起
> DocumentsUI 时不锁定）——两者都会削弱"敏感内容结构性不可达"这一安全事实。
> 复验（全部实跑）：`:app` JVM **9/9** · 设备内 4 批 **51/51**（新增 `FileWorkflowD16Test` 6/6）·
> `:core:test` **71/71** · `:conformance:run` **91/91** · E2E v4
> **`core-journey-v4-20260917-184856` = 41/41 PASS / 0 FAIL** ·
> assembleDebug / assembleRelease / bundleRelease 全部 SUCCESSFUL。
> parity **62 / 73**（原 56）；`N1 = PASS`、`N2 = PARTIAL_WITH_REPORT`、
> `ANDROID_PRODUCTION_RELEASE_READY = BLOCKED_BY_PRODUCTION_SIGNING`。
> 完整内容见 `ANDROID_N1_N2_FINAL_CLOSURE_REPORT_V2.md`（**就地更新，未生成 V3/V4**）§3.4。
>
> → **（历史）ANDROID FINAL BLOCKER CLOSURE，2026-09-16**
> —— 关闭 App Lock 的真实接线缺口、备份导出 UI 误报、无障碍标签与滚动容器 hitbox；
> 重跑核心 E2E 与全量回归；**重新计算** N1 / N2。
> 结论见 `ANDROID_N1_N2_FINAL_CLOSURE_REPORT_V2.md`（**不沿用旧的 58/62 与旧 PASS 数量**）。
>
> **Production 依赖目标（明确冻结，不再动摇）**：
> `DCloud = 0 target` · `UTS = 0 target` · `uni-app / uni-app x = 0 target`。
> 这三者只作为 **LEGACY_REFERENCE / BEHAVIOR ORACLE** 留在仓库中，
> **不再出现在 Current 路线的任何 blocker、门禁或 Next 里**。

## Current

- **2026-09-25 Harmony N3 连续推进轮 #5（host Core-Journey E2E）**：parity 保持 29/73（纯测试 + 文档轮）。
  - 零生产源码改动；新增 `CoreJourneyHost.test.ets`（8 条 E2E）：导入→提案→确认→影响→备份/恢复往返。
  - **`HARMONY_CONFORMANCE_HOST = PASS`（142/142）**：87 canonical + 3 元测试 + 1 domain 自检 +
    14 persistence + 17 repository + 12 import-pipeline + **8 core-journey**，0 fail。
  - E2E 断言：proposal-only 永不 must_change（AGENTS §9）· confirmed required 边失效 →
    must_change(required_edge_no_alternative) · 未确认备用 → needs_review · 管线全链路无 required ·
    备份→恢复逐字节相同且影响一致 · 事务注入失败边与 revision 一并回滚。
  - HAP 3,569,752 B（生产源码与 round#4 同源）；`spec/`、`fixtures/`、`conformance/expected/` 零改动。
- **2026-09-25 Harmony N3 连续推进轮 #4（Import 管线 host 集成）**：parity 保持 29/73（工程深度集成轮）。
  - 新增 3 个纯 ArkTS 模块（`data/MerchantResolver` / `data/RecurrenceDetector` / `data/ImportPipeline`），
    零 @ohos、真实编译进 HAP；`check-compiled-reachability --build` **38/38 PASS**。
  - **`HARMONY_CONFORMANCE_HOST = PASS`（134/134）**：+12 条管线测试 —— 真实微信 CSV（base64 内嵌）→
    WechatParser → 商户分组/resolver（alias/normalized/fuzzy/ambiguous/unresolved）→ 指纹（哈希缝 mock，
    deterministic + secret 敏感）→ recurrence → merchant_agreement proposal/evidence/fingerprint/session
    同事务落库 → 幂等重导全 dup → revision 不 bump。
  - HAP 3,569,752 B（sha256 `9987c443…`，clean 30s）；`spec/`、`fixtures/`、`conformance/expected/` 零改动。
  - 诚实边界：指纹哈希（cryptoFramework）与 funding_source 路由建议属设备层；未 resolution / 非 recurring
    商户不产生 Proposal（AGENTS §15 / GOAL §14）。
- **2026-09-25 Harmony N3 连续推进轮 #3（ArkTS 内存持久化执行层）**：parity 27/73 → **29/73**。
  - 新增 4 个纯 ArkTS 模块（`data/GraphStore` / `data/GraphRepository` / `data/MigrationChain` /
    `data/RepositorySelfCheck`），零 @ohos、真实编译进 HAP；`check-compiled-reachability --build` **35/35 PASS**。
  - **`HARMONY_CONFORMANCE_HOST = PASS`（122/122）**：+17 条仓库测试（logical-key 唯一 / verify 不重复建边 /
    retire-reactivate 同 id / retire 幂等 / criticality 缺省 unknown 且永不自动 required / revision 同事务 /
    group 成员回填 / proposal UPSERT 生命周期 / evidence·fingerprint 唯一键 / export-import 逐字节往返 /
    原子回滚 / 迁移链 ×50 幂等 + 失败回滚停在旧版本）。
  - HAP 3,511,091 B（sha256 `1213f05a…`，clean 16s）；`spec/`、`fixtures/`、`conformance/expected/` 零改动。
  - 诚实边界：ArkData 物理 DB、crypto 哈希、change_plans/reality_drifts/discovery_candidates 行数据属设备层 →
    相关格保持 ICNR / NOT_STARTED；`RUNTIME_E2E=RUNTIME_NOT_RUN`（华为账号+镜像 E-9）。
- **2026-09-25 Harmony N3 连续推进轮 #2（ArkTS 持久化逻辑层）**：在 round#1 基础上继续；parity 22/73 → **27/73**。
  - 新增 5 个纯 ArkTS 模块（`data/SchemaV3` / `data/PayloadCodec` / `data/PayloadMigration` /
    `domain/SourceInstance` / `data/PersistenceSelfCheck`），零 @ohos、真实编译进 HAP；
    `check-compiled-reachability --build` **31/31 PASS**（Gate 首跑抓出 SourceInstance 缺入边 →
    由 PersistenceSelfCheck 以 data→domain 正确方向接入）。
  - **`HARMONY_CONFORMANCE_HOST = PASS`（105/105）**：87 canonical + 3 元测试 + 1 domain 自检 +
    **14 条新持久化测试**（schema 契约逐字断言 / v1→v2→v3 迁移 / evidence provenance / build-validate
    roundtrip / 5 项孤儿完整性 / 版本 fail-closed 拒绝 / SRC-01 作用域隔离 / legacy 工厂确定性）。
  - HAP 3,438,086 B（sha256 `96c974fb…`，clean 17s）；`spec/`、`fixtures/`、`conformance/expected/` 零改动。
  - 诚实边界：DB 原子导入（ArkData）、crypto 哈希、evidence repository 属设备层 → 相关格保持 ICNR /
    NOT_STARTED，主机测试不冒充设备运行时；`RUNTIME_E2E=RUNTIME_NOT_RUN`（华为账号+镜像 E-9）。
- **2026-09-25 Harmony N3 恢复轮（代码侧推进）**：用户批准重启 E-9（PAUSED → ACTIVE）；把 Harmony parity 从
  「表格未同步」归位为「与真实源码 + 真实执行一致」，**0/73 → 22/73**。全部证据本轮新鲜实跑：
  - `hvigorw assembleHap`（ASCII 镜像 + --clean）→ BUILD SUCCESSFUL 67s；HAP `entry-default-unsigned.hap`
    **3,380,659 B**，SHA-256 `610701e006a857dde7e6cd16a4dba57b4d74158a551ba25fe54edb82a9057c21`
    （HAP 非字节可复现：zip 时间戳，同源码两轮 clean 为 86f1bc53… / 610701e0…，字节数一致）。
  - `check-compiled-reachability.mjs --build` PASS：**26/26 required 模块** reachable + 全入
    `modules.abc`（432,560 B）；负向 probe（注入类型错误必须 fail clean 构建）PASS；孤儿模块 = **0**。
  - `run-conformance-host.mjs` PASS：**91/91 host checks**（87 canonical + 3 元测试 + 1 domain 自检）；
    `HARMONY_HOST_PASS = 87/91`（87 条 canonical 真实 ArkTS 运行时逐字节复现 expected，0 fail）。
  - `codegen --check` PASS（spec→generated 无漂移）；`tools/conformance/run.mjs`：android **PASS 91/91** 保持、
    harmony 87/91（4 条 DEVICE-BLOCKED：golden/utf8-norm/migration-db/backup-roundtrip，如实记 FAIL
    「no result reported」**不写 PASS**）、ios 91/91（2026-09-19 旧记录）。
  - **Parity 归位口径**：`CONFORMANCE_PASS`（host 执行面）= 源码入 HAP + canonical 用例在真实 ArkTS 运行时 PASS；
    `IMPLEMENTED_COMPILED_NOT_RUN` = 只有源码 + 编译证据；**RUNTIME_VERIFIED 一格未增**（设备面不可用，
    `HARMONY_RUNTIME_E2E = RUNTIME_NOT_RUN`，华为账号 + 模拟器镜像为唯一外部依赖 E-9）。
    明细：§1 12 CP + 3 ICNR · §2 0（无 ArkTS repository 层）· §3 2 CP + 3 ICNR · §4 7 CP（parser 22/22）·
    §5 0（UI 仅 Index.ets）· §6 1 TESTED（host 单测 91 checks）。逐格见 `NATIVE_PARITY_MATRIX.md`。
  - 回归：core `npm run check` 全绿（453/453，architecture circular=0，network/secrets/ui 全 PASS）；
    修复 1 个 v0.2.0 既有文档格式回归（desktop-download-smoke.md prettier 空行，仅格式零内容）；
    `spec/`、`fixtures/`、`conformance/expected/` 零改动；未进入 iOS N4（E-8 需 macOS）。
- **v0.2.0 Product Usability & Dual-Client Maturity 轮（2026-09-25）**：产品体验/double-client 一致性/发布收口。
  - 基线对齐：main 5069df9 起于 `feat/product-v0.2.0-usability`，ff 合入 main=origin/main=release/product-v0.2.0
    = tag `product-v0.2.0`（SHA 见 git；全程无 force）。
  - 环境取证（A3/§6）：v0.1.2 GitHub 下载 smoke —— **Android PASS**（SHA 匹配 + install/launch/uninstall +
    60/60 复跑）；**Desktop FAIL 取证**：v0.1.2 portable/installer 的 jpackage runtime 缺 java.exe/javaw.exe
    （399 条目唯一 exe 是 PDIG.exe），进程存活无窗口无 JVM 子进程，根因=v0.1.2 打包链路残 defect 未被发现。
    `V0_1_2_RELEASE_TRAIN = CLOSED`（记录见 docs/release-evidence/v0_1_2_download_smoke/）。
  - 打包链路重写（修复根因）：`scripts/release/build-desktop-package.ps1` = gradle jar → jdeps（--multi-release
    base + ASCII 清单解析）→ jlink（本机验证产出 java.exe/javaw.exe）→ 手工 app-image + PDIG.cmd launcher →
    打包后自检（JVM launcher 必须存在）→ NSIS/portable zip。实测 portable 解压启动出真实窗口
    `PDIG 0.2.0 Preview`（javaw pid）。
  - Android 产品改造：Home 聚合 blocked/review_required/needs_revalidation/verifying 计划卡（plansNeedingHandling +
    5 单测）；ChangePlanScreen 重复标题修复 + EVIDENCE_SUGGESTED→「发现新的依据，请确认」；Impact 每级稳定解释文案
    （needs_review 不上错误色）；Infrastructure 搜索+kind 分组（nodeKindGroupLabel + groupedNodes）；NodeDetail
    六问卡（是什么/确认了什么/待处理问题；依据与最近确认因模型无字段诚实省略）；CSV 字段对应可改步骤
    （ExposedDropdownMenuBox，自动建议+人工可改+示例值+重新解析）；导入错误分层
    （UNREADABLE/UNPARSEABLE 文案 + 跳过错行说明）；隐私声明统一；ImportScreen 拆分 ≤200 行 composable。
  - Desktop 产品改造：一级导航 16→7（HOME/ATTENTION/SOURCES/INFRA/SCENARIOS/TIMELINE/SETTINGS）+ 新增
    AttentionScreen（待确认关系/待确认服务/可能发生了变化/需要处理的计划四卡）；Infrastructure 双栏
    （左列表右 NodeDetailPane）；内部术语清零（Humanize.kt：kind/relation/capability/criticality/readiness/
    phase/verification 全人话，ImpactScreen/PlanScreen/VerificationScreen 的 wire 上屏点清掉、actionId/nodeId
    泄漏清除、graphRevision 三行移除）；版本 0.2.0 全端统一；内容区 focusable（键盘可达）。
  - 双端一致性文档：PRODUCT_TERMINOLOGY_V0_2.md（术语词典）、PRODUCT_EXPERIENCE_MAP_V0_2.md（14 用户任务×八问）、
    DUAL_CLIENT_EXPERIENCE_MATRIX_V0_2.md、PRODUCT_ERROR_CATALOG.md（10 类错误×四问）、docs/user/ 五份用户指南、
    README v0.2.0 安装/场景章节。
  - 门禁与测试（全部真实执行）：core `npm run check` PASS（453/453 + architecture 0 + network 0 + secrets 0）；
    quality `check-quality.mjs` VERDICT **PASS**（10 项计数 0）；Android `:core:test` 71/71、`:app:testDebugUnitTest`
    63/63、`:conformance:run` 91/91、仪器化 pdig36 **60/60**×2；API36 tablet 全量 connected 套件 **60/60 PASS**（pdig36_tablet_b，1920×1200@240dpi/3GB；设备守卫排除外来 API-15 模拟器后取得；根因与 2560×1600 时序 flake 见 FINAL GATES）
    ；Desktop :app:test 14/14 +
    `--smoke` 16/16（Desktop Core Journey：launch/import/confirm/scenario/plan/complete/verify/backup/restore）；
    Fresh Clone 最终回归（C:\pdig-fresh-020）Android JVM+conformance 91/91+签名 APK+仪器化 60/60、
    Desktop compile/test/smoke 全 PASS。
  - 发布：versionName 0.2.0 / versionCode 200004（NON-PROD 签名 apksigner v2 Verified）；
    artifacts：setup.exe（ecad6055…）、portable.zip（2adc08ed…）、APK（ff13e51b…）、
    SBOM CycloneDX 1.5 151 components、THIRD-PARTY-NOTICES、SHA256SUMS、Manifest、Release Notes。
  - 详情见 `PRODUCT_V0_2_0_RELEASE_MANIFEST.md` / `RELEASE_NOTES_0_2_0.md` / 各审计文档。
- **v0.1.2 质量迭代收口轮（2026-09-24）**：质量 Gate 复证 + 死代码清理 + Windows 0.1.2 打包。
  - 质量 Gate：`node scripts/quality/check-quality.mjs` → **VERDICT PASS exit 0**（2026-09-24T06:56Z），
    10 项计数全 0（UNJUSTIFIED_PRODUCTION_FILE_GT_300 / CRITICAL_COMPLEXITY_VIOLATION / RAW_TODO /
    FORBIDDEN_SUPPRESSION / DEPENDENCY_CYCLE / HARDCODED_SECRET / SENSITIVE_LOGGING /
    UNJUSTIFIED_KOTLIN_ESCAPE / KNOWN_DEAD_CODE / ENGINEERING_GAP 全 = 0）；file-size OK（exempted 3）；
    scope=`android/app,core,conformance,repos` + desktop。EXCEPTIONS.json 复核合法 UTF-8 JSON、reason 无乱码：
    fileSizeOver300 2 条（Migrations.kt=migration、GraphSerialize.kt=schema）、kotlinEscapes 2 条
    （MainActivity lateinit、conformance Main lateinit，均 JUSTIFIED）、secretPattern 2 条
    （SmokeRunner.kt:39、DepmapFileStoreTest.kt:21，fixture 合成口令）。
  - 纯格式轮：121 个 md 经 prettier 规范化，commit `a9c1cab`（纯格式，`git diff -w` 复核无业务语义变化）。
  - 死代码（FIXED）：desktop `io/FileOps.kt` 删除无调用者的 `DesktopFileOps.write`（6 行；Grep 确认
    desktop 全树无 `.write(` 调用者、无实现覆盖），保留 pickOpen/pickSave/readBytes。
  - 回归全绿：core `npm run check` **453 tests / 43 files PASS**、architecture circular=0、
    network gate 0 primitives（130 文件）、secret scan **988 files PASS**、UI gate 30 .uvue PASS；
    conformance fresh SUMMARY（2026-09-24）android **91/91 PASS**（pass=91 fail=0 total=91）；
    `:core:test` **71/71**、`:app:testDebugUnitTest` **9/9**（--rerun-tasks fresh，0 failure）；
    仪器化基线 `TEST-pdig36(AVD)-16-_app-preview.xml` **60/60 PASS**（2026-09-24 11:39）；
    desktop `:app:test` BUILD SUCCESSFUL（27s，0 failure）+ `--smoke` **16/16 PASS**（app-0.1.2.jar）。
  - Android 版本：preview flavor versionCode 200002→**200003**、versionName "0.1.1"→"0.1.2"；
    `assemblePreviewRelease -PpdigNonProdSigning=true` BUILD SUCCESSFUL（1m19s，52 tasks）；
    APK `com.pdig.app.preview` versionCode=200003 versionName=0.1.2 platformBuild API 36；
    apksigner verify v2 scheme Verified（NON-PROD 测试签名，非生产签名）。
  - Android 运行时 smoke：环境修复后 **A5 全流程取得新鲜证据（PASS）**（2026-09-24 晚）：
    （1）根因修复——android-36 系统镜像损坏（`system-images/android-36/google_apis/x86_64` 目录仅剩
    `.installer` 锁文件，kernel-ranchu/system.img 缺失），经 sdkmanager 重装
    `system-images;android-36;google_apis;x86_64`（system.img 4.4GB 就位）后 `pdig36` AVD 恢复可引导
    （~90s）；另确认两个环境特性：模拟器子进程随启动它的 shell 退出即被回收（故一切设备操作须在
    单命令内完成：启动→引导→操作），PATH 中旧 `C:\Android\adb.exe` 1.0.32 与 platform-tools 1.0.41
    冲突（统一前缀 platform-tools 解决）。
    （2）新鲜证据：pdig36(API36) `adb install` preview-release APK **Success**
    （com.pdig.app.preview versionCode=200003 versionName=0.1.2 targetSdk=36；sha256
    5406d9e7…与 Release Manifest 一致）；launch MainActivity 成功、进程存活（pid=3204）、
    topResumedActivity=MainActivity、crash buffer 为空；截图
    `.tmp_audit/android-smoke-v012-pdig36-home.png`（1080×2340）；
    `:app:connectedPreviewDebugAndroidTest` **BUILD SUCCESSFUL**（1m57s）→ TEST XML
    **60/60 PASS（0 failure/0 error/0 skipped，pdig36 AVD 新鲜复跑）**；
    卸载数据（install→uninstall→确认包与数据移除）全 Success、crash buffer 空。
  - Desktop 版本与产物：packageVersion/version/描述字符串 0.1.1→0.1.2；PDIG.exe GUI 启动 20s 无崩溃
    （窗口 1100×720 固定，分辨率覆盖以默认窗口实跑）；Windows 打包：
    NSIS installer `PDIG-0.1.2-windows-x64-setup.exe` 149,537,743 B（sha256
    92baac291afbd8d41da5cfebb3451593458b9afeee3066540b93b0967ff973a6）+ portable zip
    `PDIG-0.1.2-windows-x64-portable.zip` 149,769,364 B
    （fa3a229355f273e1121c534b0243525f2caed9622f624e694beb00a671545b01）；
    SBOM `PDIG-0.1.2-SBOM.cyclonedx.json`（CycloneDX 1.5，151 components，由 jpackage-libs +
    Gradle previewReleaseRuntimeClasspath 依赖树生成）+ THIRD-PARTY-NOTICES.md + SHA256SUMS.txt。
  - 发布物哈希：APK sha256 `5406d9e7edd6f274b5de23752d98451ded94716c88ff049589e4afe6b4716c8e`；
    SBOM `fc5448782262e007a20f050c56043af6b5bfc197d5c2891774e341d7376eada9`；
    NOTICES `7275ec5b40aa5bd659e9938d8fc4491afbcc9ff095d1fa138f25b0e5deaee944`。
    新文件 `RELEASE_NOTES_0_1_2.md`、`PRODUCT_V0_1_2_RELEASE_MANIFEST.md` 已创建。
  - 已知限制：无 Play 生产发布；Android 运行时 smoke 已在本轮修复并取得新鲜证据
    （API36 AVD PASS，见上；真实手机仍为外部 blocker E-1）；无真机/真实数据
    （全合成 fixture）；Harmony/iOS 不在本轮；Windows 未签名（SmartScreen）；
    CI billing E-10（CI_EXTERNAL_BLOCKED）；旧 tag product-v0.1.0/v0.1.1 保留。
- **v0.1.1 质量迭代轮（2026-09-24）**：共享发现引擎 + 桌面验证补全 + 代码卫生。
  - 引擎：`DiscoveryRepository`（:repos）在 `commitImport` 内生成/累计 DiscoveryCandidate /
    RealityDrift（Android/Desktop 共用）；宁可漏报、机器不自动确认、不 bump revision；
    Desktop smoke 与 Android 仪器化证据链全绿（引擎单测 8 条 + smoke 2 步 + androidTest 1 条）。
  - Desktop 验证：默认 1100×720、1280×720、1920×1080、最大化/恢复、最小 420×320、
    键盘 Tab/Shift+Tab/Enter/Escape、高 DPI（120%）缩放全部真实执行，截图+日志证据；
    唯一登记限制：Compose Desktop UIA 暴露有限。
  - 代码卫生：2 处 Kotlin 死条件警告消除；slf4j-nop 静默 sqlite-jdbc 噪音；
    遗留清单 #1（Android 平台绑定结论）与 #3（发现生成）关闭。
  - 版本：Android Preview versionCode 200002 / 0.1.1；Desktop PDIG 0.1.1。

- **v0.1.0 Developer Preview 轮（2026-09-24）**：全仓工程治理复核收尾 + Windows Desktop + Android GitHub Preview
  **已发布**（GitHub Pre-release `product-v0.1.0`，详见 `PRODUCT_V0_1_0_RELEASE_MANIFEST.md`）。
  - 复核结论（本地真实运行）：Quality Gate PASS（10 counter 全 0）；`:conformance:run` 91/91；`:core:test` 71/71；
    `:app:testDebugUnitTest` 9/9；desktop `:app:test` 9/9 + `--smoke` 14/14 步骤 PASS；API36 AVD 仪器化 59/59（0 crash）。
  - 产物：`PDIG-0.1.0-windows-x64-setup.exe` / `-portable.zip`（未签名，SmartScreen 提示如实披露）、
    `PDIG-0.1.0-android-preview.apk`（`com.pdig.app.preview`，versionCode 200001，NON-PROD 测试签名，明确非 Play 版）。
  - `DESKTOP_V0_1_0_SCOPE_ADDENDUM.md` 声明 Desktop 不是 Canonical Truth Source；打包/SBOM 工具入仓
    （`scripts/release/`），可复现。
- Phase: **ANDROID API36 工程全速收口（ANDROID_2026_PRODUCTION_REALITY_CLOSURE）** —— Harmony/iOS **PAUSED**（契约 Boundary）
- 本轮 Gate：**`ANDROID_CANONICAL_FREEZE = PASS`**（基线 tag `v0.3.0-android-canonical-freeze`）+ **`ANDROID_API36_READY = PASS`（工程可验证范围）**
- 本轮新增/更新的文档：`ANDROID_PLATFORM_BASELINE.md`（新建）· `ANDROID_VERSIONING_POLICY.md`（新建）·
  `ANDROID_16_API36_CLOSURE_REPORT.md`（新建）· `ANDROID_PLAY_CONSOLE_READINESS.md`（新建）·
  `ANDROID_STORE_COMPLIANCE_REPORT.md`（新建）· `ANDROID_CLOSED_TEST_PLAN.md`（新建）·
  `ANDROID_REAL_DEVICE_FINAL_REPORT.md`（新建，如实 BLOCKED）· `REAL_DATA_PILOT_0_REPORT.md`（新建，如实未执行）·
  `ANDROID_PRODUCTION_RC_MANIFEST.md`（新建，NON-PROD 签名产物）· `ANDROID_RELEASE_IDENTITY_DECISION.md`（TARGET_SDK→36）·
  `NATIVE_PARITY_MATRIX.md` / `NATIVE_RELEASE_MATRIX.md` / `NATIVE_MIGRATION_STATUS.md`（API36 轮注记）
- Current gate focus: **`ANDROID_PRODUCT_COMPLETE = PASS`** / **`N2_ANDROID_FULL_PARITY = PARTIAL_WITH_REPORT（69/73）`** /
  **`ANDROID_SIGNING_READY = BLOCKED_BY_MISSING_PRODUCTION_KEYSTORE`**（NON-PROD 签名链路已验证，不以 non-prod 冒充）/
  **`ANDROID_REAL_DEVICE_VERIFIED = BLOCKED_BY_MISSING_REAL_DEVICE`**
- This round results（本轮实跑，HEAD `b13f2f7`，**API36 AVD phone + tablet**）：`:core:test` **71/71** ·
  `:app:testDebugUnitTest` **9/9** · `:conformance:run` **91/91** · connectedDebugAndroidTest **59/59 × 2**（phone + tablet）·
  **Core Journey E2E 41/41** · **三场景 E2E 32/32** · assembleDebug / assembleRelease / bundleRelease **SUCCESSFUL** ·
  **NON-PROD signed APK（apksigner v2 verify PASS）+ signed AAB（jarsigner verified）** · crash-scan 0
- **API36 行为 Gate（见 `ANDROID_16_API36_CLOSURE_REPORT.md`）**：`EDGE_TO_EDGE_API36 = PASS` /
  `PREDICTIVE_BACK_API36 = PASS` / `ANDROID16_ADAPTIVE_LAYOUT = PASS` / D-16 **PASS**（无内容压系统栏、无绕过锁）
- **CI**：基准 HEAD `89b653a` push CI 全绿记录仍有效（run 35700579040/35700579087）；
  本轮工程 HEAD（b13f2f7 + 文档 commit）CI 验证见 `git push` 结果与 `ANDROID_CI_EXACT_SHA_POLICY.md`；
  E-10（GitHub 计费/配额）阻塞与否以本轮 push 后 run 为准，如实记录
- Secret/privacy：`check-secrets.mjs` PASS（**896** 文件 0 production secrets）；无新增 INTERNET/analytics/telemetry
- Parity 保持 **69/73**：剩余 4 项逐格确认仍为外部 blocker / 政策项（TalkBack 实机 / production keystore / 最终品牌素材 / R8·minify 未开）
- 外部项（全部真实 BLOCKED，见 `BLOCKERS.md`）：applicationId/品牌决策（OPEN）、生产 keystore、真机、真实账单 Pilot-0、
  Play 开发者账号、隐私/支持公网 URL、closed test、E-10 CI 计费
- Global status: **`ALL_DONE = NO`**（剩余项全部为真实 EXTERNAL_BLOCKER）· **`TASK_COMPLETE = YES`（本轮契约完成）**
- 诚实声明：**`ANDROID_GOOGLE_PLAY_RELEASED` 非 PASS**（无 Play 账号/身份验证/付款、无 applicationId 定案、
  无生产 keystore、无真机、无真实数据、closed test 未启动 —— 本轮不宣称上架）
- CURRENT_HEAD: `b13f2f7` + 本轮文档 commit（以 `git rev-parse HEAD` 为准）· CURRENT_BRANCH: `feat/android-production-release`
- NEXT_GATE: 由用户解除外部 Gate（R-1..R-5 / keystore / Play 账号 / 真机 / 账单 / 公网 URL / closed test）后继续
  Play 提交轨；解除后按 `ANDROID_PLAY_CONSOLE_READINESS.md` 顺序执行（Internal → Closed → Production）
- NEXT_COMMAND（下一位 Agent 的第一步）：

  ```bash
  git fetch origin && git rev-parse HEAD && git status --short -uall
  # Android 回归（canonical freeze 后仍应全绿）
  cd android && ./gradlew --no-daemon :core:test :app:testDebugUnitTest :conformance:run
  # 设备：（如 AVD 在线）59 个 androidTest
  # CI 引用必须带 exact-SHA：gh api repos/huangdi97/PDIG-DepMap/actions/runs?event=push（head_sha + run URL + all jobs）
  ```

> 说明：Android 收口轮已按人工批准把 Android 从 CORE_FROZEN 临时解除；本轮完成后保持冻结范围
> （仅发布/外部项可继续），不进入 MVP04 或新业务 Domain。

- NEXT_COMMAND（下一位 Agent 的第一步）：

  ```bash
  git rev-parse HEAD && git status --short -uall
  # Harmony：先跑五道 gate（前四道不需要设备）
  PDIG_DEVECO_HOME="<DEVECO_HOME>" node tools/harmony/check-third-party-hashes.mjs
  PDIG_DEVECO_HOME="<DEVECO_HOME>" node tools/harmony/check-argon2-native-build.mjs
  PDIG_DEVECO_HOME="<DEVECO_HOME>" node tools/harmony/check-compiled-reachability.mjs --build
  # 主机执行面：63/63 运行时无关用例（真实 ArkTS，无设备）→ 期望 PASS
  PDIG_DEVECO_HOME="<DEVECO_HOME>" node tools/harmony/run-conformance-host.mjs
  # 产物符号反查
  PDIG_HARMONY_BUILD_ROOT="C:/Users/Kaiser/pdig-harmony-build" node tools/harmony/probe-abc-symbols.mjs
  # Android（冻结，仅回归）
  cd android && ./gradlew --no-daemon :core:test :conformance:run
  ```

  > 需要 `ohpm install` 先跑过一次（生成 `oh_modules`），否则镜像缺依赖、测试代码编不过。
  > 另：本仓库 `on.push` **不自动触发** CI，需 `gh workflow run CI --ref <branch>` 手动调度。

### Current 只保留这 6 个关注面（其它内容一律属于 Historical / Legacy）

| #   | 关注面                     | 状态                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| --- | -------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | Native Migration           | 进行中（**Android 已冻结 CORE_FROZEN；N3 Harmony 为唯一活跃主线**）                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| 2   | Android N1 / N2            | **N1 = PASS**，`N2 = PARTIAL_WITH_REPORT` 62/73 —— 见 `ANDROID_N1_N2_FINAL_CLOSURE_REPORT_V2.md`（**冻结，不再推进**）                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| 3   | Harmony N3                 | **ACTIVE**：`HARMONY_BUILD` = **PASS**（clean assembleHap，含 native）；`HARMONY_MODULE_COMPILED` = **PASS**（A/B/C/D 四判据，**22/22** required 模块，`modules.abc` 243,556 B，0 孤儿）；`HARMONY_CRYPTO` = **COMPILED**（JCS / AAD / AES-256-GCM）；**`HARMONY_DOMAIN` = COMPILED**（11 组纯 ArkTS 全部落地，15 项自检可从产物反查；`RelationRegistry` 按对齐 Android 冻结版的决定**不实现**）；`HARMONY_DEPMAP` = **NATIVE_BUILD_PASS / ON_DEVICE_NOT_RUN**（Argon2 NAPI 全链路已打通：vendored 溯源 + 交叉编译 arm64/x86_64 + 打包进 HAP 且符号表恰好只导出 NAPI 入口）；**`HARMONY_CONFORMANCE_RUNNER` = PASS**（真实 ArkTS runner 已落地并进编译图：`conformance/` 4 模块 2,031 行，`modules.abc` 符号取证齐全；**非 Node 镜像**）；`HARMONY_CONFORMANCE` = **NOT_RUN**（runner 已就绪但**无运行时，一次都没执行**，**执行计数仍 pass=0**，不得记 PASS —— 见 `HARMONY_N3_CONFORMANCE_REPORT.md` §0.2）；`HARMONY_ARKUI` = PARTIAL_WITH_REPORT；`HARMONY_RUNTIME_E2E` = **RUNTIME_NOT_RUN**（无模拟器镜像，见 `HARMONY_RUNTIME_ENVIRONMENT_AUDIT.md`）—— 见 `HARMONY_N3_IMPLEMENTATION_STATUS.md` / `HARMONY_ARGON2_INTEGRATION_REPORT.md` |
| 4   | iOS N4                     | `BLOCKED_BY_MACOS`（真实外部 blocker，不是工程缺口）                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| 5   | Cross-platform Conformance | Android **91/91**（本轮实跑 + **CI 远真复验**双证）；Harmony **设备侧 NOT_RUN（0 执行 / 91）**，但**主机执行面 `HARMONY_CONFORMANCE_HOST = PASS`：63/63 运行时无关用例在真实 ArkTS 下逐字节复现**（另有 `HARMONY_DOMAIN_HOST` = PASS 15/15）；其余 **28 全部 blockedByRuntime，0 notImplemented**；timeline ×3 的 fixture 欠定见报告 §7.5.1；**LC-003 `bound_to` 已裁决为 Canonical correction：三端判定一致（均为 registry 查找，非各自 wire 枚举），`bound_to → reject` 早已由既有 2 条 fixture 永久固化，用例总数维持 91**（见 `LEGACY_BEHAVIOR_CORRECTIONS.md` §LC-003、报告 §7.8 —— 该节原文曾误报为跨端差异，已撤回）；iOS 无报告                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| 6   | Legacy Cutover             | **NOT_STARTED**（Cutover 条件未满足）                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |

### 当前真实外部 blocker（只有这些）

见 `BLOCKERS.md` / `NATIVE_EXTERNAL_BLOCKERS.md`：

- **B3** 无 macOS / Xcode → iOS 无法编译验证（N4）
- **B4** 无生产 release keystore → `ANDROID_PRODUCTION_RELEASE_READY = BLOCKED_BY_PRODUCTION_SIGNING`
- **B5 / B6 / B7 / B11 / B12 / B12b / B14–B17** 商店账号、正式包名、隐私政策 URL、品牌与素材
- **B13** 真实账单（仅 REAL_DATA Gate 需要）
- **B18** 真实设备（当前只有 AVD）
- **B19** 是否需要真实数据验证的决策
- **B23** 本机沙箱限制（clean clone 闭环 / 破坏性 `npm ci`）

### 已从 Current blocker 列表移除（不再阻断当前路线）

- **B10 / DCloud 账号**、**B1 / B2（uni-app x 产品级产物）**：
  只阻断 **LEGACY uni-app x 路线**的产品级打包。Production 已退出该路线，
  `DCloud = 0 target`，因此它们**不再出现在 Next / Current Gate 里**。
- **B20 / B21 / B22**：这是 **LEGACY UTS 路线**的桥接缺口
  （账单解析桥接 / `.depmap` 加解密桥接 / 设备端 Impact 镜像）。
  原生 Android 已各自具备**真实实现 + 设备内证据**（见 `ANDROID_REMAINING_7_AUDIT.md`、
  `ANDROID_CORE_USER_JOURNEY_E2E_REPORT.md`），**对新路线不构成 blocker**。
  旧记录完整保留在下方 Historical / Legacy 区，不删除。

---

## HISTORICAL / LEGACY（不再代表当前 Production 路线）

> 本节及以下全部内容描述的是 **uni-app x / UTS / DCloud 路线**（2026-09-15 之前）。
> 其结论对旧实现仍然有效，**但不再是产品未来**，也不再出现在 Current 区。
> 保留原因：Legacy 实现仍在仓库中充当 **BEHAVIOR ORACLE**
> （见 `LEGACY_REFERENCE_MANIFEST.md`、`LEGACY_BEHAVIOR_CORRECTIONS.md`），
> 在 Cutover 条件满足前**绝不删除**。

### 已过期的 Current 叙述（2026-09-18 归档，内容原样保留不改写）

以下三段曾经出现在 Current 区，**现已不再成立**。归档而非删除，
是为了让「当时据何判断」可追溯 —— 这也是本项目对 absence ≠ nonexistence 的一贯处理方式。

1. **「N3_HARMONY_FULL_PARITY = NOT_STARTED（HARMONY_BUILD = PASS，其余多未开工/阻塞）」**
   —— 过期。实际：N3 已是唯一活跃主线，`HARMONY_BUILD` = PASS，
   `HARMONY_MODULE_COMPILED` = PASS（A/B/C/D 四判据），
   Argon2 NAPI 全链路打通并已打包进 HAP。见 `HARMONY_N3_IMPLEMENTATION_STATUS.md`。

2. **「NEXT_GATE: N3_HARMONY_FULL_PARITY —— 未开工；只有在 N1 / N2 双双 PASS
   且人工 Final Acceptance 通过之后才进入」**
   —— 过期。该前置条件已被实际推进覆盖：N3 工作自 2026-09-18 起持续进行，
   并未等待 N1/N2 的 Final Acceptance。Android 侧现为 **CORE_FROZEN / MAINTENANCE_ONLY**，
   N2 保持 62/73 不再推进；N3 不以其为前置。

3. **「本轮已按 stop condition 停止：D-16 关闭 + 全回归 + parity 重算 + Git 收口均已完成，
   **不进入 Harmony N3 / iOS N4 / MVP04**，等待人工 Final Acceptance」**
   —— 过期。D-16 关闭与全回归确已完成，但「不进入 Harmony N3」这一约束在后续轮次
   已被解除，N3 现为活跃工作。**iOS N4 与 MVP04 的禁止仍然有效。**

### LEGACY 接力轮（2026-09-15）做了什么

1. **恢复现场并保全前序工作（最重要）**：进入时工作区**不 clean** —— 上一轮
   PLATFORM BRINGUP 的**全部**产物（源码改动 + 23 份文档 + AAR）都在工作区、**零提交**。
   逐文件判读后按 11 个逻辑提交全部入库（用 plumbing 建链，见下「Git 故障」）。
2. **独立复验，不采信旧报告**：逐项复算产物哈希、重跑门禁、重跑 Android 构建、复查环境。
3. **推翻 B10 的旧定性**：`cli pack` 是 DCloud 官方定义、支持 uni-app x 的打包命令；
   真正闸门是**账号**。旧结论「CLI 无 build 命令 ⇒ 只能靠 GUI」**不成立**（`BLOCKERS.md` B10 已改）。
4. **UTS 层首次获得真实编译证据**：发现 UTS 编译器公开在 npm 上，实测 **15/15** 通过
   （5 插件 × Android→Kotlin / iOS→Swift / HarmonyOS→ArkTS），并**因此查出并修复 2 个
   iOS UTS 语法错误**（`do { try } catch` 非法 + `DepmapSchemaV1.shared` 不存在）。新增门禁
   `npm run check:uts`。
5. **修掉上一轮登记的欠账 T-1**：不变量测试的临时目录泄漏（`rmSync`），实测 delta = 0。
6. **修掉本轮开始时发现的真实回归**：7 份平台文档未过 `format:docs:check`（`npm run check` 原本失败）。
7. **修正过期数据**：`docs/ANDROID_BUILD_REPORT.md` 的 AAR 字节数/class 数（44,147→72,376、23→37）。

### 本轮 quality state（实跑）

- `npm run check` → **EXIT=0**
  - format:check PASS；format:docs:check PASS；lint PASS；typecheck PASS
  - **453 passed / 453（43 文件）**
  - architecture PASS（48 files，circular 0）；network PASS（0 原语）；secrets PASS（0）
  - UI 静态门 PASS（30 `.uvue`，24 pages，5 components，34 色）
- `npm run check:uts` → **15/15 compiled, PASS**（本轮新增；未装编译器时 SKIPPED + exit 0）
- `npm run check:invariants` → **18 passed**，`%TEMP%/depmap-inv-*` 泄漏 **delta = 0**（T-1 修复验证）
- **Android 原生构建复现**：`assembleDebug assembleRelease collectArtifacts --rerun-tasks`
  → `BUILD SUCCESSFUL in 7m 50s`，`49 actionable tasks: 49 executed`（无 FROM-CACHE）
  → AAR 字节数与 SHA-256 与 `RELEASE_CANDIDATE_MANIFEST.md` **逐字节一致**
- **HarmonyOS 原生构建复现**：见「本轮 Platform 复现」小节（本轮以 Python 复刻 `build.sh`
  的 ASCII 镜像流程，因为本环境 bash 缺失 `dirname/grep/mkdir/tar/cp`）

### 本轮 Platform 复现（实测口径）

| 平台     | 命令（可直接复制）                                                                                                                                                                                                        | 结果                                             |
| -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------ |
| Core     | `cd core && npm run check`                                                                                                                                                                                                | EXIT=0（453/453）                                |
| UTS 三端 | `cd core && npm run check:uts`                                                                                                                                                                                            | 15/15 PASS                                       |
| Android  | `cd platforms/android && JAVA_HOME="<ANDROID_STUDIO_HOME>/jbr" ANDROID_HOME="<ANDROID_SDK_ROOT>" "<GRADLE_HOME>/bin/gradle.bat" --no-daemon assembleDebug assembleRelease collectArtifacts --rerun-tasks --console=plain` | BUILD SUCCESSFUL in 7m 50s；AAR 逐字节复现       |
| Harmony  | 见 `docs/HARMONY_RELEASE_RUNBOOK.md`；本环境需用 Python 复刻 ASCII 镜像（`build.sh` 依赖 bash 工具）                                                                                                                      | 见 `docs/HARMONY_BUILD_REPORT.md` + 本轮复现记录 |
| iOS      | 不可执行（非 macOS）                                                                                                                                                                                                      | `IOS_TOOLCHAIN_READY = BLOCKED (B3)`             |

### Git 故障与规避（本工作区特有，务必先读）

`git commit` / `git update-ref` 在本工作区**会成功创建对象但无法推进 HEAD**：`.git/refs/heads/**`
的 loose ref 被外部进程回收，`git update-ref` 返回 0 却不变更 HEAD。实测连续两次 `git commit`
产生的提交**父节点都是基线**，即提交之间不成链。

**规避方式（本轮使用，未用任何被禁止命令）**：

```
git add <paths> && git write-tree
git commit-tree <tree> -p <parent> -m "<msg>"     # 显式建链
# 然后写 .git/packed-refs（packed-refs 稳定；loose ref 被删也不影响 HEAD 解析）
```

每次提交后必须校验 `git rev-parse HEAD` 与 `git status --short -uall`。
**禁止**：`git reset --hard`、`git clean -fd`、`git checkout .`、`git restore .`、force push。

### 本轮 Git 收口

- **前序零提交工作已全部入库**：19 个已跟踪文件 + 52 项未跟踪内容 → 11 个逻辑提交
  （gitignore / Android 原生 / UTS 桥接 / HarmonyOS 骨架 / iOS 修复 / 占位资产 / AAR 产物 /
  平台文档 / 收口报告 / 文档格式回归修复 / 本轮 UTS+交接文档）
- `git status --short -uall` = **0 行**；`git diff --check` = PASS
- **未 push**（用户未授权）；**未创建 RC tag**（三端均无产品级可安装包，打 RC 标会造成误读）
- `.tmp_audit/`（前序会话的临时工具与探测输出）已加入 `.gitignore`，**保留在磁盘**供复用

### 本轮未提交 / 未做（诚实清单）

- 未安装 HBuilderX 的打包插件、未登录 DCloud 账号（§110：账号登录属真正的用户交互闸门）
- 未下载 Android system-image（约 1.5 GB；即使装上 AVD 可启动，**仍无 APK**，B10 未解则无意义）
- 未删除 `%TEMP%` 中 2498 个历史 `depmap-inv-*` 残留目录（批量删除违反本环境 safe-delete 纪律；
  T-1 已修复，后续不再增长）
- 未实现 iOS `DepmapContainerV1.swift`（G-1，需 macOS 才能验证，写不可编译的加密实现风险过高；
  已写入 `docs/IOS_RELEASE_HANDOFF.md` 作为 Mac 侧首要待办）

## Platform Matrix（本轮口径）

| Platform               | IMPLEMENTED | STATIC_AUDITED    | COMPILED                 | TESTED     | DEVICE_VERIFIED | STORE_READY |
| ---------------------- | ----------- | ----------------- | ------------------------ | ---------- | --------------- | ----------- |
| Core（Node 22）        | YES         | YES               | YES                      | YES（453） | N/A             | N/A         |
| UTS（5 插件 × 3 平台） | YES         | YES               | **YES（15/15，降级层）** | N/A        | N/A             | N/A         |
| UI（uni-app x，24 页） | YES         | YES（9 类静态门） | **NO（B10）**            | —          | NO              | NO          |
| Android（原生核心）    | YES         | YES               | **YES（AAR，可复现）**   | YES（8/8） | NO              | NO          |
| Android（产品包）      | YES（源码） | YES               | **NO（B10）**            | NO         | NO              | NO          |
| HarmonyOS（原生验证）  | YES         | YES               | **YES（HAP，ArkTS）**    | NO         | NO              | NO          |
| HarmonyOS（产品包）    | YES（源码） | YES               | **NO（B10）**            | NO         | NO              | NO          |
| iOS（原生）            | YES         | YES               | **NO（B3，非 macOS）**   | NO         | NO              | NO          |

## Current failures

**无失败测试。** 未执行项全部是外部闸门（B10 账号 / B3 macOS / B18·B24 设备 / B4·B7·B8·B9 签名 /
B5·B6 商店账号）或 Real Data（NOT_RUN），**不虚报、也不为凑数降门禁**。

## External blockers

见 `BLOCKERS.md`（B10 已按本轮实测重写为 AUTH 类）与 `STORE_EXTERNAL_BLOCKERS.md`：

- 阻断产品级构建：**B10（DCloud 账号）**、B1（keystore）、B2（AGC 签名）
- 阻断真机：**B18 / B24（无设备；现有 AVD 缺 system image）**
- 阻断 iOS：B3（macOS）、B8/B9（Apple 账号与证书）
- 阻断商店：B5/B6/B11/B12/B12b/B14–B17
- 环境：B23（沙箱拦截 clean clone / 破坏性 `npm ci`）
- 工程缺口（非用户可解）：B20（账单解析桥接）、B21（`.depmap` 加解密桥接）、B22（设备端 Impact 镜像）

## Next

1. **解除 B10**（唯一能同时解锁 Android/Harmony 产品级产物与 UI 编译的闸门）：
   `cli open` → `cli user login` → `cli project open --path "<repo>\app"` →
   `cli pack --project app --platform android --android.packagename <正式包名> --android.androidpacktype 1`
   （或 GUI「发行 → 原生App-云打包」）。首次需在 DCloud 后台换取正式 appid
   （当前 `app/manifest.json` 是离线占位 `__UNI__DEPMAP01`）。
2. **B24**：接真机，或在 SDK Manager 安装
   `system-images;android-35;google_apis_playstore;x86_64` 让现有 AVD `Medium_Phone_API_35` 可启动。
3. **B4**：生成 release keystore（放 `signing/`，已 gitignore），配置 Gradle signingConfig。
4. **iOS（不依赖账号，可并行）**：补 `platforms/ios/swift/DepmapContainerV1.swift`（G-1）、
   把 SQLCipher 接入 SPM（G-2）、把 `XCTSkip` 换成真实断言（G-3）；按 `docs/IOS_RELEASE_HANDOFF.md` 执行。
5. **B13**：真实账单双 Gate（`validate-real-bill.ts` 就绪；Real Data 保持 NOT_RUN）。

**进入 MVP04 的前提**：至少一台真实设备完整 E2E PASS + 产品级 Build artifact PASS +
Backup/Restore PASS + UI device QA PASS + Release blockers 清晰。**当前均未满足。**

---

## 历史：PLATFORM BRINGUP（2026-09-15，成果零提交，本轮已入库）

- Android 原生核心真实编译：AAR 产出，黄金向量 8/8 PASS；修复 D-1..D-10（含 2 个 P0：
  跨端 Base64 契约破裂、容器解析字段冲突使 Android **无法解密任何容器**）。
- HarmonyOS：原工程只有 3 文件且 `app.json5` 缺 Stage 模型 `"app"` 顶层键，
  **从未被 hvigor 解析过**；补齐 9/9 骨架后 `BUILD SUCCESSFUL`，产出 HAP（ArkTS 字节码）。
- UI：修复 3 类「首次编译必炸」的 UTS 缺陷（R-1 文件被截断、R-2 引用不存在的模块、
  R-3 UTS 调 Kotlin `suspend fun`），新增 Kotlin 侧回调桥接 `UtsSecurityBridge`。
- iOS：修 `Package.swift` 空 target（`exclude` 导致无源文件）与 `LAPolicy()` 实例化错误。
- 生成 `PLATFORM_BRINGUP_PRE_AUDIT.md` / `PLATFORM_RELEASE_MATRIX.md` /
  `PRODUCTION_RUNTIME_UI_AUDIT.md` / `RELEASE_CANDIDATE_MANIFEST.md` +
  `docs/ANDROID_*`（4 份）+ `docs/HARMONY_*`（3 份）+ `docs/IOS_RELEASE_HANDOFF.md`。

## 历史：FINAL PRODUCTION CLOSURE V1（2026-09-14）

- 新增 docs 格式门禁（`.prettierrc.json` + `format:docs`）；删 3 个死导出；18 处 `obj`→`record`；
  `.gitignore` 补 Gradle/Android 本地状态。
- B1/B2 旧前提（「无 JDK / 无 SDK / 无 DevEco」）被实测推翻并按实测重写。
- 登记 T-1（不变量测试临时目录泄漏）→ **本轮已修**。

## 历史：Production RC V1（2026-09-13）

- UI 产品化：新增 Application Service（唯一数据边界）+ 纯规则层；修复 12 页直连 SQLite、
  4 页复算领域逻辑、`drift.uvue` 裸 SQL 手动 bump revision；新增 5 个 `dp-*` 组件与设计 token；
  17 页重写 + 7 页新增（共 24 页）；新增 tabBar 一级导航；去假功能；新增手动声明支付关系；
  修复 4 处依赖方向错误；`check-ui.mjs` 从 0 → 9 类。
- 质量：`check:full` FINAL_EXIT=0；453/453；覆盖率 93.74/82.2/94.28；Stryker 重跑与基线一致。

## 历史：MVP03 FINAL FREEZE（2026-09-13，tag v0.3.0-mvp03）

- P0 修复：PlanReadiness 废除减法，改由 `PlanAction.resolvesImpactKeys[]` 显式 resolution。
- 冻结补测：FR-GR-012 revision property、4 状态机非法迁移负向、rebase 原子性、10k Timeline smoke。
- Targeted mutation 10/10 KILLED，0 critical survived（PARTIAL_WITH_REPORT）。
- 实测：453/453（43 文件）；stability ×3 + focused ×10 全绿。

## 历史：MVP03 交付 Gate 级结论

| Gate                                                       | 结果         |
| ---------------------------------------------------------- | ------------ |
| A Graph Revision（GR-001..012，同事务 bump）               | PASS         |
| B ChangePlan Rebase（PRB-001..011）                        | PASS         |
| C PlanReadiness（三值纯规则，无 confidence/absence 通道）  | PASS         |
| D ScenarioCoverage（四级 + 可解释）                        | PASS         |
| E RealityDrift（RD-001..010，absence 永不触发）            | PASS         |
| F DiscoveryCandidate（不进 Impact / 不 bump revision）     | PASS         |
| G ScenarioTemplate（3 active + planned gate + 政策）       | PASS         |
| H Timeline（确定性投影，可溯源）                           | PASS         |
| I Verification（done ≠ verified，两段式）                  | PASS         |
| J Migration v2→v3（MIG3-001..006）                         | PASS         |
| K depmap compat（golden 不变；payload v3 + v1/v2 migrate） | PASS         |
| L/M/N 回归（MVP01/MVP02/Baseline）                         | PASS         |
| O Security/Privacy（新对象只存引用/ID）                    | PASS         |
| P UI（24 页源码级；UTS 三端已编译；页面编译 BLOCKED B10）  | PASS（静态） |
| Q Documentation                                            | PASS         |

## 产品可用性（诚实口径，未变）

| 能力                                      | 状态                             |
| ----------------------------------------- | -------------------------------- |
| 手动建立对象（卡 / 账户 / 服务）          | **可用**                         |
| 手动声明支付关系（含 required / unknown） | **可用**                         |
| 影响模拟（选中卡 → 受影响下游）           | **可用**                         |
| 创建变更计划 + 计划内影响清单 + 动作/验证 | **可用**（保守口径）             |
| 时间轴 / 待确认项 / 数据来源 / 数据清空   | **可用**                         |
| 账单导入                                  | **不可用**（B20；UI 已如实标注） |
| 加密备份导出 / 恢复                       | **不可用**（B21；UI 已如实标注） |

> 当前构建**在解除 B10 后可以真实安装并真实使用**（不依赖导入即可完成主流程），
> 但覆盖范围小于 MVP01 完整设计。这是 B20/B21 的直接后果，不做粉饰。

## 仓库运维注意（重要）

分支 loose ref（`.git/refs/heads/<branch>/`）在本工作区会被外部进程反复删除，
且 `git commit` 无法推进 HEAD。**规避：用 `commit-tree` 建链并把分支写进 `.git/packed-refs`。**
若出现 "branch has no commits"，从 reflog 取哈希后重写 packed-refs；
**不要**执行 `git reset --hard` / `git clean`。

---

## 本轮：PDIG NATIVE MIGRATION — 阶段 N0 完成 + N1 领域层（2026-09-15）

### 决策

用户最终决策：**彻底退出 uni-app x / UTS / DCloud**，切换为
Android(Kotlin/Compose) / iOS(Swift/SwiftUI) / HarmonyOS(ArkTS/ArkUI) 三端原生。
旧实现保留为 `LEGACY_REFERENCE / BEHAVIOR_ORACLE`，**未删除**。

### 本轮实测（本机，非声称）

| 项                    | 命令                                            | 结果                          |
| --------------------- | ----------------------------------------------- | ----------------------------- |
| Legacy oracle 基线    | `cd core && npm test`                           | **43 files / 453 tests PASS** |
| Codegen Gate          | `node tools/codegen/generate.mjs --check`       | **PASS**（3 端 generated）    |
| Oracle 自检           | `core/scripts/generate-conformance.ts --verify` | **PASS（64 用例逐字节复现）** |
| Android 领域层编译    | `gradle :core:compileKotlin`                    | **BUILD SUCCESSFUL**          |
| Android Conformance   | `gradle :conformance:run --args="<repo>"`       | **pass=64 fail=0**            |
| 跨端 Conformance Gate | `node tools/conformance/run.mjs`                | **VERDICT: PASS**             |

### 本轮产出（新增）

**Canonical Spec（`spec/`）**

- `README.md`、`domain/domain.json`（机器可读）、`domain/entities.md`、
  `domain/invariants.md`
- `schema/logical-schema.json`、`schema/persistence-contract.md`
- `state-machines/change-plan.json` + `state-machines.json`（7 台状态机）
- `errors/error-codes.json`
- `security/depmap-container-v1.json`（含 Golden Vector + UTF-8 + JCS 向量）、
  `security/security-policy.md`
- `ui/design-tokens.json`、`ui/copy-zh.json`
- `migration/migration-spec.md`

**Fixtures / Conformance**

- `fixtures/`：64 个平台中立用例（impact 13 / readiness 16 / coverage 6 /
  relations 18 / depmap 3 / jcs 1 / scenario 1 / migration 1 / state-machine 5）
- `fixtures/import/`：28 个原始输入（CSV/OFX/QFX）三端共用
- `conformance/CONFORMANCE_MANIFEST.json`（含 sha256 与 oracle 提交）
- `tools/conformance/run.mjs`：统一 Gate（codegen → 完整性 → oracle → 三端报告）

**Codegen**

- `tools/codegen/generate.mjs` → 三端 `generated/CanonicalEnums.{kt,swift,ets}`

**Android（新工程 `android/`，非旧 `platforms/android`）**

- `core`：domain / impact / plan / scenario / statemachine / schema / json（纯 Kotlin JVM）
- `core/.../crypto`：JCS + `DepmapContainer`（BouncyCastle Argon2id + JDK JCE）
- `conformance`：读取 fixtures → 产出 `conformance/reports/android.json`

**Legacy 冻结与控制文件**

- `LEGACY_REFERENCE_MANIFEST.md`、`legacy/README.md`、
  `LEGACY_BEHAVIOR_CORRECTIONS.md`（6 条，含 **2 条真实功能性缺陷**）
- tag `v0.3.0-uniapp-reference` → `7bc0ed3`
- `GOAL_PDIG_NATIVE_MIGRATION.md`、`NATIVE_MIGRATION_STATUS.md`、
  `NATIVE_MIGRATION_ACCEPTANCE.md`、`NATIVE_PARITY_MATRIX.md`、
  `CROSS_PLATFORM_CONFORMANCE_MATRIX.md`、`NATIVE_RELEASE_MATRIX.md`、
  `NATIVE_EXTERNAL_BLOCKERS.md`、`docs/ADR_NATIVE_MIGRATION.md`

### 本轮发现并**修复**的真实缺陷

| 编号   | 缺陷                                                                      | 处置                                   |
| ------ | ------------------------------------------------------------------------- | -------------------------------------- |
| LC-001 | `csv-utf8-bom.csv` 实际不含 BOM（该测试路径从未真正覆盖）                 | 记录；Native fixture 另用真实 BOM 文件 |
| LC-002 | `csv-missing-required-column.csv` 并未缺失必需列（命名误导）              | 记录                                   |
| LC-003 | legacy UI 暴露 runtime registry 不承认的 `bound_to`（用户可选到会被拒绝） | **Canonical Spec 锁定 2 值**           |
| LC-004 | `relationLabel` 含不存在的 `wallet_binding`                               | 记录                                   |
| LC-005 | readiness 文案两处不一致                                                  | 以 `copy-zh.json` 为准                 |
| FIX-6  | golden fixture 的 wrongPasswordOutcome 误用正确口令                       | **已修**（错误口令 + 增补篡改场景）    |
| FIX-7  | JCS reject case 含 NaN（JSON 无法表达，必然假失败）                       | **已修**（移除）                       |
| FIX-8  | conformance harness 从 manifest 读 expected（manifest 不含）              | **已修**（改读 fixture 本体）          |

### 未做（诚实清单）

- Android：**持久化 / Keystore / Biometric / Compose UI / APK** 全部未开始
- HarmonyOS：**仅 codegen 产物**，Domain/UI/Crypto 未开始
- iOS：**仅 codegen 产物**，build = `BLOCKED_BY_MACOS`
- Timeline / Migration / Parser / Backup-Restore 的 Conformance fixture **尚未建立**
- Production 仍依赖 DCloud/UTS/uni-app（未 Cutover，符合计划）

### Next

见 `NATIVE_MIGRATION_STATUS.md` § NEXT。

---

## 本轮：ANDROID N1 / N2 RUNTIME CLOSURE（2026-09-15）

### 定位

本轮**不是功能轮**。目标只有一个：用**真实运行时证据**判断 Android N1 / N2 能否 PASS，
并把上一轮遗留的「构建链依赖本机绝对路径」永久收口。

用户明确约束：**不进入 Harmony N3，不进入 MVP04，不新增业务 Domain，不重设计 UI。**

### 1. 构建链（本轮最大的隐性 blocker）

| 项                            | 发现                                                                                                                                                    | 处置                                                                                             |
| ----------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| Gradle Wrapper                | `android/` **完全没有** `gradlew` / `gradlew.bat` / `gradle-wrapper.jar` / `gradle-wrapper.properties`；构建只靠绝对路径 `<GRADLE_HOME>/bin/gradle.bat` | 生成标准 Wrapper（Gradle 8.9，官方 `distributionUrl`），已提交 `68f506c`                         |
| `android/local.properties`    | 含机器 SDK 路径                                                                                                                                         | 保持 gitignore（`.gitignore:86`），**未提交**（已用 `git ls-files --error-unmatch` 验证 exit=1） |
| `:conformance:run` 默认仓库根 | `rootProject.dir("../..")` 算错一级 → `<repo_parent>` → `FATAL: <repo_parent>/conformance\CONFORMANCE_MANIFEST.json not found`                          | 改为 `dir("..")`                                                                                 |
| JVM 代理                      | JVM 不读 `HTTP_PROXY` 环境变量，Wrapper 自举下载报 `Connection refused`                                                                                 | 用 `GRADLE_OPTS="-Dhttp.proxyHost=… -Dhttps.proxyPort=…"`（端口 10808 可通）                     |

`gradlew clean` / `assembleDebug` / `assembleDebugAndroidTest` / `:conformance:run` 全部 **BUILD SUCCESSFUL**。

### 2. 运行时实测（本机 AVD，非声称）

| 项                  | 命令                                                   | 结果                                                  |
| ------------------- | ------------------------------------------------------ | ----------------------------------------------------- |
| Conformance         | `./gradlew --no-daemon :conformance:run`               | **pass=91 fail=0 notImplemented=0 total=91**          |
| 设备内 androidTest  | `./gradlew --no-daemon :app:connectedDebugAndroidTest` | **19 / 19 PASS**（0 skipped / 0 failed）              |
| `:core` 纯 JVM 单测 | `./gradlew :core:test`                                 | **NO-SOURCE（0 个）** —— 记为真实欠账，不粉饰         |
| 真机 E2E            | `local_private/e2e_drive.py`（串行单次干净运行）       | 13 类步骤 PASS；业务写入链路等 NOT_RUN                |
| 性能 smoke          | `PerfSmokeEvidenceTest`                                | `csvRowsParsed=10000`、`csvParseErrors=0`，强断言通过 |
| logcat 隐私扫描     | PID/UID 归属扫描                                       | `appLines=44`，6 类敏感关键字命中 **全 0**            |

产物：`app-debug.apk` 36,794,370 B（SHA-256 `d84d8900…30879`）；
`app-release.aab` 20,734,935 B（SHA-256 `f10cc60d…0f76c`，**未签名**）。

### 3. 本轮修掉的真实缺陷（8 项）

| #   | 类别         | 缺陷                                                               | 修复                                                     |
| --- | ------------ | ------------------------------------------------------------------ | -------------------------------------------------------- |
| 1   | 构建链       | `android/` 完全没有 Gradle Wrapper                                 | 生成标准 Wrapper（Gradle 8.9）                           |
| 2   | 构建链       | conformance 默认仓库根算错一级                                     | `../..` → `..`                                           |
| 3   | 运行时       | SQLCipher native 库未加载 → `UnsatisfiedLinkError`                 | `System.loadLibrary("sqlcipher")`                        |
| 4   | 运行时       | Cursor 惰性视图越界 → `CursorIndexOutOfBoundsException`            | 改用 `MaterializedRow`                                   |
| 5   | 运行时       | 查询不存在的列 `criticality` → `SQLiteException`                   | 从 `acceptProposal` 的 SELECT 中移除                     |
| 6   | **取证方法** | 性能 smoke 数据**无效**（`csvRowsParsed=0`）                       | 修正 `dateFormats` + 强断言 `assertEquals(10_000, rows)` |
| 7   | **取证方法** | logcat 扫描把 Launcher3 的 `password:false` 系统字段误判为应用泄露 | 改 PID/UID 归属扫描                                      |
| 8   | 运行时       | 单进程跑 19 个 androidTest 被 OOM kill（signal 9）                 | 按类分批 + 类间 `pm clear` / `logcat -c`                 |

> 第 6、7 项尤其值得记住：**上一轮报出的性能数字和"日志泄露"结论都是假的**，
> 一个因为数据根本没解析进去，一个因为扫了别人的日志。旧数字已作废。

### 4. 三个判定（分别回答，不混为一谈）

| 判定                               | 结果                              | 一句话理由                                                                                                             |
| ---------------------------------- | --------------------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| `N1_ANDROID_VERTICAL_SLICE`        | **PARTIAL_WITH_REPORT**           | 分层证据很硬（91 + 19 全绿），但核心链路 import→proposal→reality→impact→changeplan→verification **设备级一次都没跑通** |
| `N2_ANDROID_FULL_PARITY`           | **PARTIAL_WITH_REPORT**           | **58 / 62**；截图保护 / App Lock / Biometric 属于"只有实现没有运行时证据"                                              |
| `ANDROID_PRODUCTION_RELEASE_READY` | **BLOCKED_BY_PRODUCTION_SIGNING** | 缺生产 keystore；且非生产签名流水线本轮**未生效**（产物与未签名版同 SHA-256）                                          |

**最终：判定 B —— 不进入 Harmony N3。**

### 5. 本轮报告（7 份，全部新写）

`ANDROID_BUILD_REPRODUCIBILITY_REPORT.md`、`ANDROID_PERFORMANCE_SMOKE_REPORT.md`、
`ANDROID_RUNTIME_E2E_REPORT.md`、`ANDROID_SECURITY_RUNTIME_AUDIT.md`、
`ANDROID_REMAINING_7_AUDIT.md`、`ANDROID_STORE_METADATA.md`、
`ANDROID_N1_N2_FINAL_CLOSURE_REPORT.md`

### 6. 本轮状态文档更新

- `NATIVE_MIGRATION_STATUS.md`：55/62 → **58/62**；N1 由 PASS **降级**为 PARTIAL_WITH_REPORT；可复现命令改用 `./gradlew`
- `NATIVE_PARITY_MATRIX.md`：截图保护 / 设备 E2E / 性能 smoke / 无障碍 / Store metadata / Release 签名 逐格更新；新增 `PARTIAL` 标注口径声明
- `CROSS_PLATFORM_CONFORMANCE_MATRIX.md`：用例总数 **64 → 91**（早期版本漏统计 Parser 22 / Timeline 3 / Migration-Backup 2），Android 91 PASS

### 7. 本轮 Git 收口

- 提交 `68f506c`：`android/gradlew`、`android/gradlew.bat`、
  `android/gradle/wrapper/gradle-wrapper.jar`、`android/gradle/wrapper/gradle-wrapper.properties`
  —— **仅这 4 个路径**，工作树中其余未提交内容**未动**。
- **⚠ 本轮复现了本工作区的既有 Git 故障**：`git commit` 成功创建了对象 `68f506c`
  并返回 0，但 **HEAD 未推进**（`git rev-parse HEAD` 仍是 `7bc0ed3`），
  4 个文件只停留在 index（`A`）。
  **处置**：沿用既有规避方式——确认 `68f506c` 的 parent 确为 `7bc0ed3`、tree 正确后，
  直接改写 `.git/packed-refs` 中 `refs/heads/feat/mvp03-living-graph` 的指向。
  复核：`git rev-parse --short HEAD` = `68f506c`，`git ls-files` 能列出全部 4 个 wrapper 文件。
  **下次提交后务必复查 HEAD，不要只看 `git commit` 的返回码。**
- **未 push**（用户未授权）。

### 8. 本轮自我纠偏（必须记录）

上一轮曾出现**循环检测**：反复读取同一个临时结果文件（`_adb.txt` / `_st.txt`）导致上下文空转。
本轮改为**每次产出唯一结果文件 + 直接捕获 stdout**，未再发生。
另有一次 E2E 因**两个驱动实例并发**而污染结果（首页标记为空、crashes=2），
已改为串行单次运行后取得干净结果集。

### 9. Next（仅清 blocker，不做新功能）

见 `NATIVE_MIGRATION_STATUS.md` § NEXT（P0：打通应用层写入路径 + 给 `:core` 补 JVM 单测）。
**P0 关闭前不进入 Harmony N3。**

---

## 2026-09-16 Android P0 Runtime Closure（实跑结果）

被测 APK：`app-debug.apk`，SHA256
`bf378ec6678f04bf988921528b73ab879d9381a418d3a094ece2e60490305ff1`（36,887,249 B）。
设备：`emulator-5554`（API 34）。

### P0-1 真机 E2E —— 21/21 PASS，App 崩溃 0

`local_private/core_journey_e2e_v2.py`，证据 `local_private/e2e/core-journey-v2-20260916-172044.*`。
J1 全新安装 → J2 导入（SAF 选真实 CSV）→ J3 候选 → J4 确认 Reality → J5 标记必需 + 影响面
（必须处理（2））→ J6 变更计划 → J7 done → J8 verified → J9 进程死亡后数据仍在
（共 5 个对象）→ J10 导出 .depmap + 错误密码恢复被拒。
报告：`ANDROID_CORE_USER_JOURNEY_E2E_REPORT.md`。

### P0-2 `:core:test` —— 71/71

报告：`ANDROID_CORE_JVM_TEST_REPORT.md`。

### P0-3 运行时安全取证

- **Gate 1 FLAG_SECURE：PASS（6/6 路由）**。双证据：窗口 `fl=` 含 `SECURE` +
  `screencap` 被抹黑（均值 0.17 vs 非敏感页 244.64）。
  敏感：SOURCES / IMPORT / INFRASTRUCTURE / BACKUP；非敏感：HOME / SETTINGS。
- **Gate 2 App Lock：PARTIAL，且有真实缺口**。`AppLock.state()` 在设备上返回
  **`NOT_CONFIGURED`**（fail-closed，符合预期）；但 `MainActivity` 固定
  `startDestination=HOME`，全仓库无 `nav.navigate(Route.LOCK)` —— **LockScreen 写好了
  却没有任何入口能调起它**。
- **Gate 3 备份加密 / Keystore：PASS**（`sqlcipher_plainSqliteCannotRead`、
  `keystore_rawKeyNeverReachesDisk`、`depmap_tamperedCiphertextIsRejected` 等）。
- 报告：`ANDROID_RUNTIME_SECURITY_EVIDENCE.md`。

### P0-4 全回归

| 项                               | 结果                                           |
| -------------------------------- | ---------------------------------------------- |
| `:core:test`                     | 71/71 PASS                                     |
| `:app:testDebugUnitTest`         | **NO-SOURCE**（app 模块无 JVM 单测，如实记录） |
| `:conformance:run`               | **pass=91 fail=0 total=91**                    |
| `:app:assembleDebug`             | BUILD SUCCESSFUL（APK 见上）                   |
| `:app:connectedDebugAndroidTest` | **19/19 PASS**（0 skipped）                    |

### P0-5 Git HEAD

- HEAD = `68f506c`，branch `feat/mvp03-living-graph`，**无删除、无 staged 残留**。
- 已跟踪修改：`AGENTS.md`、`WORK_STATUS.md`。
- **未跟踪**：`android/`（全部原生源码）、`conformance/`、`fixtures/`、`spec/`、
  `harmony/`、`ios/`、`legacy/`、`tools/` 及本轮新增报告。
- **未提交**：范围太大且用户未授权；提交前需确认是"只提 android/ + 报告"还是整体入库。
- **未 push**。

### 本轮发现的两个真实缺陷（未修，只记录）

1. **备份导出 UI 误报失败**（2/2 复现）：App 显示「备份失败：无法写入文件。」，
   但 `/sdcard/Download/pdig-backup.depmap` 已完整写入，且用正确密码可成功恢复
   （「已恢复 29 条记录。」）。不丢数据，但会误导用户。
2. **「确认导入」按钮的有效点击区低于其可见范围**：点语义 `Button` 中心无效
   （DB 大小/mtime 完全不变），点外层 clickable View 经底部裁剪后的落点才生效。
   产品侧是否需要补滚动容器底部 padding 待评估。

### 本轮自我纠偏（必须记录）

- **取证口径 bug 差点造成误判**：`dumpsys` 输出的 flag 是裸名 `SECURE`，
  之前 grep `FLAG_SECURE` 恒为 0，一度被当成"截图保护未生效"。修正口径后 Gate 1 全 PASS。
  **结论：检测口径本身也要先自证。**
- **断言过松会放过 FAIL**：J5 第一版只判断"存在『必须处理（』"，被 `（0）` 蒙混通过，
  导致 J6~J8 连锁假失败。已改为断言数量 ≥ 1。
- **E2E 行程缺了"用户显式标记必需"这一步**：`criticality=required` 只能由用户设置
  （机器永不产生），漏掉后影响面恒为 0。已补进 J5。

---

## 本轮：ANDROID FINAL BLOCKER CLOSURE — D-16 关闭（2026-09-17）

### 决策：D-16 采用方案 A

- **根因**：外部文件选择器（DocumentsUI）是独立任务 → `MainActivity.onStop` →
  `LockGate.lockNow()` → NavHost 离开组合树 → 页面级 `remember` **与**
  `rememberLauncherForActivityResult` 的待投递结果一起丢失。
  丢的不是几个变量，而是**状态 + 投递通道**两样东西；只提升状态、launcher 留在页面级修不好。
- **做法**：`workflow/FileWorkflowState.kt`（纯状态机）+ `workflow/FileWorkflowCoordinator.kt`
  （Activity 作用域 ViewModel）+ `workflow/LocalFileWorkflow.kt`（CompositionLocal）；
  `ActivityResultContracts.OpenDocument()` 提到 **`MainActivity.onCreate`** 注册。
- **明确不采用**：方案 B（锁定时继续组合 NavHost 靠遮罩隐藏）、
  方案 C（拉起 DocumentsUI 时不锁定）。理由见 V2 报告 §3.4.3。

### 安全不变量（一条都没让）

拿到文件 ≠ 解锁 · 拿到文件 ≠ 自动 commit/restore · **口令不跨锁保留** ·
URI grant 最小化（`ACTION_OPEN_DOCUMENT` + 只读 + 用完归还） ·
进程死亡保守恢复（SavedState 只存 metadata，文件结果作废 → `INTERRUPTED`） ·
消费型投递（`consumePendingUri` 取到即清空，不会重复提交）。

### 复验（全部实跑）

| 项                                              | 结果                                        |
| ----------------------------------------------- | ------------------------------------------- |
| `:core:test`                                    | 71 / 71                                     |
| `:conformance:run`                              | 91 / 91                                     |
| `:app:testDebugUnitTest`（新增）                | 9 / 9（`FileWorkflowStateTest`）            |
| `:app:connectedDebugAndroidTest`（4 批）        | 51 / 51（含新增 `FileWorkflowD16Test` 6/6） |
| E2E v4（`core-journey-v4-20260917-184856`）     | **41 / 41 PASS / 0 FAIL**                   |
| assembleDebug / assembleRelease / bundleRelease | 全部 BUILD SUCCESSFUL                       |

parity：**56 / 73 → 62 / 73**；`N1 = PASS`；`N2 = PARTIAL_WITH_REPORT`。

### 本轮踩到并修掉的**取证脚本**缺陷（都不是产品问题，必须记下来）

1. **子串定位命中提示文案**：恢复页提示「…请输入备份密码后点「**开始恢复**」。」也含
   "开始恢复"，且在 a11y 树里排在按钮之前 → 点击落点落在只读 TextView 上 →
   表现为"恢复挂死"。改用 `restore_confirm()`（精确匹配）。定点探针
   `probe_restore_d16.py` 抓到对照证据：改用精确匹配后 5 秒内出现「已恢复 29 条记录。」
2. **`adb push` 绕过 MediaStore**：DocumentsUI 会列出该文件，但**点它没有任何反应**
   （24s 选择器不关闭）→ `unlocks=0` → 把「picker 期间必须回锁」打成 FAIL。
   补一次 `MEDIA_SCANNER_SCAN_FILE` 广播后 2 秒即选中并回锁（`probe_tamper_pick.py` 对照）。
3. 顺带：`saf_pick` 改为以 `topResumedActivity` 判断选择器是否真的关闭，不用固定 sleep 猜。

### 未跟踪项逐项判定（Git 收口）

| 项                                                        | 判定       | 理由                                                                |
| --------------------------------------------------------- | ---------- | ------------------------------------------------------------------- |
| `android/app/src/main/.../workflow/*.kt`（3 个）          | **入库**   | D-16 产品源码                                                       |
| `android/app/src/androidTest/.../FileWorkflowD16Test.kt`  | **入库**   | D-16 设备取证                                                       |
| `android/app/src/test/.../FileWorkflowStateTest.kt`       | **入库**   | D-16 JVM 单测                                                       |
| `harmony/entry/src/main/ets/generated/CanonicalEnums.ets` | **不入库** | N3 未开始（stop condition 明确不进入）；本机无 Harmony 工具链可验证 |
| `ios/Sources/PDIGCore/Generated/CanonicalEnums.swift`     | **不入库** | N4 `BLOCKED_BY_MACOS`，本机无法编译验证，入库等于声称已验证         |
| `legacy/README.md`                                        | **不入库** | LEGACY_REFERENCE，与本轮范围无关；历史多轮均保持未跟踪              |

### 停止条件

D-16 关闭 + 全回归 + parity 重算 + Git 收口均已完成，**到此停止**，
等待人工 Final Acceptance；**不进入 Harmony N3 / iOS N4 / MVP04**。

---

## 本轮：PART A GitHub 发布收口 + PART B Harmony Argon2 深挖（2026-09-18）

### PART A：发布与 CI

| 项               | 结果                                                                                                                                                    |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 历史净化门禁     | **全 PASS**（`SECRET/PRIVATE_KEY/TOKEN/RAW_FINANCIAL/PERSONAL_PATH_IN_HISTORY` 均为 NO；1,259 个可达 blob 复扫残留 **0**；`REACHABLE_OLD_SHA_COUNT=0`） |
| 首次 push        | **完成**，PRIVATE，5 分支 + 3 标签，**无 force**                                                                                                        |
| 二次 / 三次 push | 均快进：`443bd7e9..d9e5319`、`d9e5319..0b38bd40`、`0b38bd4..42b59a1`                                                                                    |
| `GITHUB_CI`      | **PASS**（第三次运行 `35303432883`：两个 job 全绿）                                                                                                     |

**首次 CI 暴露的两个真实仓库缺陷（均已修复并远真复验）**

| 缺陷                                 | 根因                                                      | 处置                                                  |
| ------------------------------------ | --------------------------------------------------------- | ----------------------------------------------------- |
| `fixtures/coverage/` 6 个用例被忽略  | `.gitignore` 的 `coverage/` 匹配**任意层级**同名目录      | 增加 `!fixtures/coverage/`；6 个用例入库              |
| `csv-crlf.csv` 的 CRLF 被归一化      | `.gitattributes` 的 `* text=auto eol=lf` 动了被测语义本身 | `fixtures/import/*`、`fixtures/coverage/*` 加 `-text` |
| Android job `Setup Android SDK` 失败 | `:core`/`:conformance` 是**纯 JVM 模块**，本不需要 SDK    | 改为 JDK 21 + `--configure-on-demand`                 |

**既有 fixture 缺陷的定因（推翻"清单过期"的粗判，给出机制）**

见 `GITHUB_PUBLICATION_REPORT.md` §6.3。核心证据链：

1. 12 个文件 HEAD blob == 引入提交 `97a0348` blob，**均无 CR**；
2. 对照 `csv-cr-only.csv` 净化后**仍保留 CR** → 净化不剥离 CR；
3. 12/12 命中 `sha256(CRLF(当前字节)) == 清单原值`。

⇒ **仓库字节从未漂移**，是清单记录了 `core.autocrlf=true` 检出态假象。
按 canonical blob 字节修正 12 条哈希，`oracle.commit` 保持 `7bc0ed32…` 未动。

> 说明：本轮再次复现本工作区 Git 故障 —— `git commit` 建对象成功但 HEAD 不推进。
> 已用 `.workbuddy/advance_refs.mjs`（写 loose ref + `packed-refs` + `show-ref` 复核）
> 修正三次，**未使用任何 `reset --hard` / `clean` / force push**。

### PART B：Harmony N3 Argon2

结论与证据见 `HARMONY_ARGON2_FEASIBILITY.md`。要点：

- 托管路径（`cryptoFramework` / `HUKS`）**证据级排除**；
- `hash-wasm/src/argon2.c` 是 **WASM 实现**，不能作原生源（本轮新排除的候选）；
- **NDK 路径可行**：clang 15.0.4 + sysroot + Node-API 头 + `ohos.toolchain.cmake` 齐备；
- 主机侧 PHC 参考实现**逐字节复现 Golden Vector**（`MATCH=YES`，`ARGON2_VERSION_13 = 19`）；
- OHOS arm64 交叉编译通过：`.so` 为 ELF64 / AArch64 / 仅 `NEEDED libc.so`；
- **`HARMONY_ARGON2_ON_DEVICE = NOT_RUN`** —— 编译通过**不等于**设备上通过，未虚报。

**可移植性教训（记下来）**：PHC 参考实现的 `opt.c` 是 x86 SSE2 实现，
arm64 必须用可移植的 `ref.c`；漏掉会直接 `undefined reference to 'fill_segment'`。

### 本轮未做（诚实清单）

- Harmony Domain 11 组、conformance 91/91、ArkData、HUKS、ArkUI —— **未开工**（Argon2 先行）
- Argon2 源码**未入库**（PoC 源码在仓库外临时目录），依赖登记 / `THIRD_PARTY_NOTICES` 未更新
- 设备上 Argon2 复验 —— **NOT_RUN**（无设备与模拟器镜像）
- iOS N4 —— **未进入**（保持 `BLOCKED_BY_MACOS`）

---

## 本轮：PART B Harmony N3 — AES / JCS / container（2026-09-18）

结论与证据见 `HARMONY_CONTAINER_V1_POC.md`。要点：

| 项                             | 结果                                                                                           |
| ------------------------------ | ---------------------------------------------------------------------------------------------- |
| Harmony AES-256-GCM + AAD 能力 | **具备**（`GcmParamsSpec{iv,aad,authTag}`，tag 由 `doFinalSync` 取、解密时经 `initSync` 传入） |
| 主机侧黄金校验                 | **5/5 PASS**（`tools/harmony/verify-container-golden.mjs`，仅用 `node:crypto`）                |
| ArkTS 实现                     | `Jcs.ets` / `DepmapContainerV1.ets` / `ContainerSelfCheck.ets`                                 |
| ArkTS 编译                     | **COMPILED**（真实编译，非假信号，见下）                                                       |
| 运行时                         | **NOT_RUN**（无设备）                                                                          |
| `.depmap` 协议                 | **未改动**                                                                                     |

### 本轮抓到的最严重问题：编译门曾是假信号

顺序如下，必须记住：

1. 新加两个 `.ets` 后 `assembleHap` 直接 `BUILD SUCCESSFUL` —— 可疑；
2. 负向对照一：在文件里放**类型错误** → 仍 `BUILD SUCCESSFUL`；
3. 负向对照二：放**语法错误** → 仍 `BUILD SUCCESSFUL`；
4. `modules.abc` 符号取证 → 只含 `Relations` / `EntryAbility` / `CanonicalEnums` / `Index` 四个模块，
   `jcsStringify`、`gateProbe` 命中数 **0**。

⇒ **hvigor 的 `CompileArkTS` 只编译从 ability / page 可达的模块**，未被 `import` 的 `.ets` 不进编译图。
在此之前任何"Harmony 编译通过"的表述都不构成证据。

处置：新增 `ContainerSelfCheck.ets` 并由 `Index.ets` 引用，建立真实 import 边
（`Index → ContainerSelfCheck → DepmapContainerV1 → Jcs`）。

处置后的双向证据：

- **负**：重建立即报出并拦截两条真实 ArkTS 错误
  （`arkts-no-obj-literals-as-types` / `arkts-no-untyped-obj-literals`），
  改为显式 `export interface AesGcmSealed` 后通过；
- **正**：`modules.abc` 42,916 B → 69,036 B，符号取证确认三个 crypto 模块及全部函数在内，
  并含 `@ohos:security.cryptoFramework` 导入（`tools/harmony/probe-abc-symbols.mjs` → `ABC_VERDICT=PRESENT`）。

### 本轮顺手修掉的工具缺陷

| 缺陷                                                                                             | 处置                                         |
| ------------------------------------------------------------------------------------------------ | -------------------------------------------- |
| `build-ascii-mirror.mjs --clean` 只在注释里存在，代码从未实现 → 想做干净构建的人拿到的是增量假绿 | 补上真实实现（构建前 `rmSync` 镜像目录）     |
| 符号取证脚本只存在于 `.workbuddy/`（不受版本控制）                                               | 固化为 `tools/harmony/probe-abc-symbols.mjs` |

### 新增产物

- `HARMONY_CONTAINER_V1_POC.md`
- `harmony/entry/src/main/ets/crypto/Jcs.ets`
- `harmony/entry/src/main/ets/crypto/DepmapContainerV1.ets`
- `harmony/entry/src/main/ets/crypto/ContainerSelfCheck.ets`
- `tools/harmony/verify-container-golden.mjs`
- `tools/harmony/probe-abc-symbols.mjs`

### 下一步（按 PART B 顺序）

1. Argon2id 绑定前置 Gate：依赖策略登记、License 选择（CC0-1.0 / Apache-2.0）、vendoring 决策；
2. hvigor/CMake 集成 + `-fvisibility=hidden`，`p = 1..4` 重新验证；
3. 设备上依次跑：`ContainerSelfCheck`（规范化层）→ Argon2id 黄金向量 → 完整容器加解密；
4. Domain 11 组 → conformance 向 91/91 → ArkData → HUKS → ArkUI；
5. `HARMONY_RUNTIME_ENVIRONMENT_AUDIT.md`。

---

## 本轮：Harmony N3 Conformance Runner — 设计决策（2026-09-18）

> 本轮**先落设计再落代码**。以下三条决策是后续所有 Harmony conformance 工作的前提，
> 写在文件里而不是留在会话里 —— 因为它们直接决定「什么才算 PASS」。

### 决策 1：ArkTS runner 读**原始源文件**，不做通用 JSON 解析

ArkTS（Stage Model / ArkTS 1.1）**没有** `JSON.parse` 到强类型对象的可用路径：
ArkTS 禁止 `any`/动态索引，`JSON.parse` 的结果无法安全地映射到 `class`。
因此「把 fixture 解析成对象再喂给 Domain」这条路在 ArkTS 上**不存在**。

处置：runner 用 `@ohos.file.fs` 读 fixture **原始文本**，用正则/受限扫描在源码侧
直接取出 `expected` 子串，再把 ArkTS 自己算出的结果序列化成同形状 JSON 做**文本比对**。

后果（必须显式记录，不得含糊）：**比对键序敏感**。
runner 侧的 `serialize` 必须逐字段复刻 fixture 的 key 顺序；
顺序不同即 FAIL —— 这是正确的，因为"逐字节复现"本就是本项目的跨端合同，
不是宽松的语义等价。verdict 措辞因此严格限定为
「expected 由 Android 端口**逐字节**复现」。

### 决策 2：runtime-independent 用例必须**真正执行** `simulateScenario`

最容易滑向"假 runner"的一步，是让 runner 只做形状检查
（"expected 里有 targets 字段吗？"）而从不调用真实的 Impact 内核。
那样跑出来的 13/13 毫无证据力。

硬性要求：`impact` 分类必须经 fixture 的 `input` 重建 `ImpactGraph`，
**真正调用 `impact/ImpactKernel.simulateScenario`**，再比对输出。
重建依赖的即时解析量（`parseJsonStringMap` / `parseDependencyArray` /
`parseGroupArray` / `parseProposalArray`）是**受控解析**，不是通用 JSON 解析：
它只认与自身契约完全一致的输入，任何结构偏离都直接失败，不猜、不兜底。

### 决策 3：runner 必须挂在可达 import 图内

runner 若不被 `pages/Index.ets` 引用，就是上一轮已经踩过的坑：
源文件存在 + `BUILD SUCCESSFUL` ≠ 已编译（见
`tools/harmony/check-compiled-reachability.mjs` 顶部的成因说明）。
因此 runner 必须由 `Index` 可达，并把自己登记进该 Gate 的 `REQUIRED_MODULES`，
否则"ArkTS runner 已实现"这句话本身不成立。

### 因此：runtime-dependent 用例如实记 `BLOCKED_BY_RUNTIME`

`parser`（22）/ `depmap`（3）/ `jcs`（1）/ `backup`（1）/ `migration-db-v1-to-v3`（1）
需要 Argon2 native 执行或 relationalStore，在无设备条件下一律记 `BLOCKED_BY_RUNTIME`，
**不计入 pass**。`HARMONY_CONFORMANCE` 只有在 91/91 全绿时才允许写 PASS。

### 落地结果（实跑，非声称）

| 项                    | 结果                                                                                                                                                                        |
| --------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 新增文件              | `conformance/JsonText.ets`(590) · `HarnessFs.ets`(135) · `ConformanceRunner.ets`(**1189**) · `ConformanceSelfCheck.ets`(117)                                                |
| 接线                  | `pages/Index.ets` 增 `conformanceProbe`；可达链 `Index → ConformanceSelfCheck → ConformanceRunner → {JsonText, HarnessFs, 9 个 Domain 模块}`                                |
| 门禁登记              | `check-compiled-reachability.mjs` 的 `REQUIRED_MODULES` **18 → 22**（+JsonText/HarnessFs/ConformanceRunner/ConformanceSelfCheck）                                           |
| 编译                  | `--clean assembleHap` → **BUILD SUCCESSFUL in 15 s 350 ms**（EXIT=0）                                                                                                       |
| `modules.abc`         | 243,556 B / 23 个模块声明；4 个 conformance 模块**全部在内**；`runConformance`/`computeCase`/`runImpactSingle`/`locateConformanceRoot`/`conformanceSummaryLine` 等逐个可查  |
| 可达性门禁            | **22/22 required**，A + B + C + D 四判据 **全部 PASS**；24 模块 0 孤儿                                                                                                      |
| 负向探针              | `--probe-module ConformanceRunner`：注入类型错误后构建**真的失败**（7 类错误，`COMPILE RESULT:FAIL`）→ 证明它确实在编译图内；探针残留已清零（`grep __pdiTypeProbe` 无命中） |
| `HARMONY_CONFORMANCE` | **NOT_RUN**（**执行计数仍 pass=0**，一次都没在设备上跑过）                                                                                                                  |

**本轮最重要的一条证据链**（值得单独记住）：

接线**之前**构建成功（15 s 102 ms）—— 因为文件不在编译图里，`CompileArkTS` 根本没碰它；
接线**之后**构建立即 `COMPILE RESULT:FAIL {ERROR:8 WARN:2}`，报出 7 类真实 ArkTS 错误
（`arkts-no-any-unknown` / `CanonicalWire` 无此导出 / `CoverageSourceInput` 实为
`CoverageSourceInfo` / `RelationValidationResult` 导入自错模块 / `root` 字段缺失 /
`string` 不可赋给 `ChangePlanWorkflowState` 与 `PlanActionPhase`）。
修复后恢复绿。这一组对照把「源文件存在 + BUILD SUCCESSFUL = 已编译」这个假结论
**在同一个文件上正反各证了一次**。

**本轮明确不主张**：不主张任何用例通过；不主张比对逻辑（键序/序列化顺序）正确
—— 本机无 ArkTS 运行时，无法自证；不主张 60 这个可执行数字会变成 60/60。
详见 `HARMONY_N3_CONFORMANCE_REPORT.md` §0.2 与 §6。

---

## 本轮：Harmony Host Closure — parser 移植，账目 65 → 85/91（2026-09-18）

### 结论（实跑，非声称）

| 项                             | 结果                                                                                   |
| ------------------------------ | -------------------------------------------------------------------------------------- |
| host 测试                      | **89/89**（85 canonical + 3 条 conformance 元测试 + 1 条 domain 自检），fail=0 error=0 |
| canonical 账目                 | **85/91** = 85 已执行 + 0 未移植 + 2 环境缺失 + 4 设备运行时                           |
| `HARMONY_CONFORMANCE_HOST`     | **PASS**                                                                               |
| `HARMONY_COMPILE_REACHABILITY` | **PASS**（A 可达 + B clean build + C modules.abc + D 负向探针）                        |
| `CODEGEN GATE`                 | PASS                                                                                   |

### 做了什么

1. 新增 `harmony/entry/src/main/ets/sources/Parsers.ets` 与 `sources/Utf8.ets`：
   纯 ArkTS 移植 CSV 词法 / 日期+金额 / OFX SGML 扫描 / 微信账单列规则。
2. `ConformanceTextSource` 契约新增 `readImportFileBase64(relPath)`：
   导入文件以**原始字节的 Base64** 提供，字节 → 文本的解码留在被测代码内。
3. `tools/conformance/embed-import-files.mjs`（新增）：把 28 个导入文件嵌入测试 bundle，
   带 `--check` 自校验，防止内嵌数据与冻结原件漂移。
4. 20 条 parser 用例从 `HOST_IMPLEMENTATION_MISSING` 迁入已执行。

### 两个真实缺陷（都不是"设备才能测"）

**1. `decodeBase64` 尾部截断**（`crypto/DepmapBounds.ets`）

原实现按完整 4 字符组分配输出长度 `(digits.length / 4 | 0) * 3`，忽略尾部残缺组。
Base64 末尾 2 个有效字符 + `==` 仍编码 1 个真实字节，3 个有效字符 + `=` 编码 2 个，
旧公式把这部分静默丢弃 → **长度 mod 3 ≠ 0 的文件丢失最后 1–2 字节**。

表现极隐蔽：只错最后一行，金额列解析正常 —— `EUR` → `EU`、`JPY` → `JP`。
命中 `parser-csv-eu-semicolon` 与 `parser-csv-multi-currency`。

修法：`rem = digits.length % 4`，`(digits.length >> 2) * 3 + (rem === 0 ? 0 : rem - 1)`，
`rem === 1` 判为非法 base64。已用 28 个导入文件逐一 round-trip 校验通过。

此前不暴露，是因为只有 depmap 的 salt / tag / ciphertext 走过这条路径，而那些字段
长度恰好整除。**这正是 D-006 存在的理由**（见 `DECISION_LOG.md`）。

**2. `export { X } from './Y'` 不产生局部绑定**（`crypto/DepmapContainerV1.ets`）

分层抽出 `DepmapBounds.ets` 时只做了 re-export，未补 import，于是
`kdfJcs(kdf: DepmapKdfHeader)` / `cipherJcs(cipher: DepmapCipherHeader)` 报
`Cannot find name`。

**为什么之前没暴露**：主机 test 构建的编译图**不覆盖**该文件（它依赖
`@kit.CryptoArchitectureKit`），只有 `--clean assembleHap` 才会编译到它。
⇒ **改 `main/ets` 后必须单独跑一次 `check-compiled-reachability.mjs --build`**，
只跑 test 会漏掉只在 hap 图里的编译错误。

### 账目迁移（分母 91 全程未变）

| 阶段                            | 已执行 | 未移植 | 环境缺失 | 设备运行时 |
| ------------------------------- | ------ | ------ | -------- | ---------- |
| N3 基线（按类一刀切）           | 57     | —      | —        | 28（整类） |
| 逐条重新定性后                  | 63     | 20     | 2        | 4          |
| state-machine + timeline 落地后 | 65     | 20     | 2        | 4          |
| **本轮（ArkTS parser 落地）**   | **85** | **0**  | **2**    | **4**      |

`HOST_IMPL_MISSING_CASES` 保留为**空数组**而非删除分支 —— 下次有人往回塞一条
必须写明理由。

### 文档与工具修正

- 新增 `HARMONY_REMAINING_6_AUDIT.md`：此前 `ConformanceRunner.ets` 与
  `run-conformance-host.mjs` 都引用 `HARMONY_REMAINING_24_AUDIT.md`，而**该文件不存在**
  —— 一个指向不存在文档的引用等于无法核查的承诺。现已补齐并改指。
- `DECISION_LOG.md` 增 **D-005**（未执行用例按性质分桶，禁按类一刀切）、
  **D-006**（导入文件 Base64 内嵌、解码留在被测代码内）。
- `tools/harmony/build-ascii-mirror.mjs`：hvigor 超时由 15 min 降到 4 min，
  并允许对**超时**重试一次（不对编译失败重试）。本机 hvigor 存在间歇性永久挂起，
  原设置会让一次门禁卡住 15 分钟才被发现；实测本次挂了 16 分钟。

### 明确不主张

不主张 Harmony 已 91/91；不主张 4 条 `BLOCKED_BY_RUNTIME` 与 2 条
`BLOCKED_BY_ENVIRONMENT` 有任何一条被验证 —— 它们**一次都没执行过**。
`HARMONY_HOST_PASS=85/91` 是分子，分母仍是 91。

---

## 本轮：REMOTE FRESH CLONE REPRODUCTION + main 集成（2026-09-19）

上一轮工作区发生过 Git 对象库损坏（见 `GIT_OPS_INCIDENT_AND_RULES.md`），
因此本轮不信任旧工作区，改为**从远端全新 clone 复现全部证据**后推进 main。
旧工作区自此只作为参考，未再执行 rebase / gc / prune / reset --hard / clean -fd / force push。

### 1. Fresh clone 与 git 完整性

| 项                   | 值                                                                                         |
| -------------------- | ------------------------------------------------------------------------------------------ |
| clone 目录           | `C:/Users/Kaiser/pdig-fresh-clone`（ASCII-only，全新，无旧 `.git` / objects / index 复用） |
| checkout             | `feat/mvp03-living-graph`                                                                  |
| `git rev-parse HEAD` | `c8ad43f29270251475e6aca489d508f69d2dde75` ✓                                               |
| `git status --short` | 空 ✓                                                                                       |
| `git fsck --full`    | exit 0，无任何输出 ✓ → **FRESH_CLONE_GIT_FSCK = PASS**                                     |

### 2. Portable gates（在 fresh clone 内实跑）

| gate                                    | 结果                                         |
| --------------------------------------- | -------------------------------------------- |
| `tools/codegen/generate.mjs --check`    | **PASS**（4 个 generated 文件一致）          |
| fixture integrity                       | **91/91** + imports **28/28**                |
| oracle selfcheck                        | **PASS**（91 cases reproduce exactly）       |
| `embed-import-files.mjs --check`        | **PASS 28/28**                               |
| `embed-fixtures.mjs --check`            | **PASS**（91 fixtures 无漂移）               |
| Android `:core:test + :conformance:run` | **pass=91 fail=0 notImplemented=0 total=91** |

→ **FRESH_CLONE_PORTABLE_GATES = PASS**

### 3. Harmony 本机工具链独立复验

全新构建根 `C:/Users/Kaiser/pdig-fresh-build`（不复用旧镜像、旧 `modules.abc`、
旧 HAP、旧日志），源码来自 fresh clone：

```
[conformance-host] 执行面 : hvigor 本地单元测试（ArkTS，无设备）
[conformance-host] 用例   : 89 条（含 3 条元测试）
[conformance-host] 汇总   : run=89 pass=89 fail=0 error=0
[ canonical ] HARMONY_HOST_EXECUTED = 85   (fail=0)
[ canonical ] HARMONY_HOST_IMPL_MISSING = 0 / ENV_BLOCKED = 2 / DEVICE_BLOCKED = 4
HARMONY_CONFORMANCE_HOST=PASS
```

结构仍为 **85 canonical + 3 meta + 1 domain selfcheck = 89**
→ **FRESH_CLONE_HARMONY_HOST = PASS 89/89**

### 4. Compile Reachability 独立复验（全新 clean build）

- 28 个 ets 模块，28 个从 `entryability/EntryAbility.ets` / `pages/Index.ets` 可达（A）
- `--clean assembleHap` 真实重建成功（B）
- `modules.abc` = 356496 bytes，26 个必需模块符号全部在列（C）
- 负向 probe：向目标文件注入类型错误后 clean build 确实在 CompileArkTS 失败（D）
- **额外定向验证本轮新增模块**：`--probe-module Parsers` / `Utf8` / `ConformanceRunner`
  三者均 PASS —— 新模块不是"碰巧在 abc 里"，而是真在编译图里

→ **FRESH_CLONE_COMPILE_REACHABILITY = PASS**

### 5. 新增模块与依赖洁净性

fresh clone 中存在且可达：`sources/Parsers.ets`、`sources/Utf8.ets`、
`generated/CanonicalRelations.ets`（`CANONICAL_RELATION_DEFINITIONS`，即 relation mapping）、
`generated/CanonicalEnums.ets`、`crypto/Jcs.ets`、`crypto/DepmapBounds.ets`、
`crypto/DepmapContainerV1.ets`、`conformance/ConformanceRunner.ets`，
以及本轮 tooling `tools/conformance/embed-import-files.mjs`。

- `.workbuddy/`：**未被 git 跟踪**（`git ls-files | grep -c '^\.workbuddy/'` = 0），
  fresh clone 内不存在该目录。
- `local_private/`：仅被两个**文档注释/可选只读脚本**提及，正式源码与门禁零引用。
- 未跟踪文件：fresh clone `git status` 为空，不存在"靠本地未跟踪文件才能跑"的情况。
- 注：`domain/RelationRegistry.ets` 在门禁里 `required: false` → **NOT_IMPLEMENTED**，
  不是本轮交付物；本轮的 relation registry 是 generated 的
  `CANONICAL_RELATION_DEFINITIONS`。

### 6. Fresh Clone Gate 结论

```
FRESH_CLONE_GIT_FSCK              = PASS
FRESH_CLONE_PORTABLE_GATES        = PASS
FRESH_CLONE_HARMONY_HOST          = PASS 89/89
FRESH_CLONE_COMPILE_REACHABILITY  = PASS
→ MAIN_INTEGRATION_GATE           = PASS
```

### 7. main 集成（fast-forward，无 force）

- `git merge-base --is-ancestor origin/main origin/feat/mvp03-living-graph` → **exit 0**
- `git merge --ff-only origin/feat/mvp03-living-graph`（在 fresh clone 内执行）
- `git push origin main` → `f6b4e01..c8ad43f`，**fast-forward**
- 远端确认 `origin/main = c8ad43f29270251475e6aca489d508f69d2dde75` ✓

### 8. main 上重新运行远端 CI

run **35415381696**（head `c8ad43f2`，手工 `workflow_dispatch` 触发）：

| job                                     | 结论             |
| --------------------------------------- | ---------------- |
| Android core (JVM tests + conformance)  | success（91/91） |
| Harmony static (no SDK, no device)      | success          |
| Canonical (codegen / fixtures / oracle) | success          |

`SUMMARY.json`：codegen PASS / fixtureIntegrity PASS（91+28）/ oracleSelfcheck PASS /
android PASS 91/0/91 / harmony **NOT_RUN** / ios NOT_RUN / verdict PASS。

### 9. CI 口径（永久分离，四条独立账）

```
GITHUB_PORTABLE_CI                  = PASS
HARMONY_HOST_CONFORMANCE_LOCAL      = PASS 89/89      (canonical 85/91)
HARMONY_COMPILE_REACHABILITY_LOCAL  = PASS            (A/B/C/D)
HARMONY_GITHUB_HOSTED_NATIVE_BUILD  = NOT_AVAILABLE
HARMONY_DEVICE_RUNTIME              = NOT_RUN
```

禁止把 `GITHUB_PORTABLE_CI = PASS` 写成「Harmony native verification PASS」；
也禁止因 hosted runner 无 DevEco 而永久阻止 main 集成。
同样口径已写入 `.github/workflows/ci.yml` 头部与 `GIT_OPS_INCIDENT_AND_RULES.md` §4。

### 10. workflow 触发修正

`on: push: branches: ["**"]` 在本仓库**从未触发过一次**运行 ——
历史运行全是 `workflow_dispatch`，包括多次真实 push。

已改为显式列举 `push: [main, feat/mvp03-living-graph]` + `pull_request: [main]`。
**但改动落地后 push 依然不触发**，如实记录证据：

- `repos/<repo>/events`：02:28:34Z 的 `PushEvent`（含该 workflow 改动）**存在**；
- `actions/runs?event=push`：`total_count = 0`；
- 该 commit 的 check-runs：`0`。

即 GitHub 收到了 push 却没启动任何运行；分支过滤器不是原因（`["**"]`
与显式列举表现一致）。API 侧已排除：Actions `enabled:true / allowed_actions:all`、
workflow 解析正常（同一文件的 dispatch 能跑）、无 `[skip ci]`、非 fork、未归档、
main 为默认分支。

**当前生效规则**：在出现第一条 `event=push` 运行之前，远端验证一律手工
`gh workflow run CI --ref <branch>` 并核对 `head_sha`。
"配了 push 触发"不得写成"push 已被验证"。

### 11. Final Status（本轮之后唯一可写的口径）

```
HARMONY_CONFORMANCE_HOST = PASS
canonical  = 85 / 91 executed
host suite = 89 / 89 PASS   (85 canonical + 3 meta + 1 domain selfcheck)
remaining  = 6              (2 = environment missing, 4 = device/runtime required)
```

不得写：`HARMONY_CONFORMANCE = 91/91`；不得写：`N3_HARMONY_FULL_PARITY = PASS`。
Harmony 尚未完成 Runtime Closure —— 下一阶段才是 **HARMONY RUNTIME / REMAINING-6 CLOSURE**。

---

## 本轮：iOS N4 canonical 全移植 + Harmony GB18030 闭合（2026-09-19）

分支 `feat/mvp03-living-graph`，末提交 `9cd55f5`。

### 1. Harmony：GB18030 字符集 ENV_BLOCKED 闭合（85 → 87）

两条 `parser-*-gb18030` 此前记 ENV_BLOCKED。本轮以**可复现证据**闭合：

| 环节     | 实现                                                              | 结果                                                                                        |
| -------- | ----------------------------------------------------------------- | ------------------------------------------------------------------------------------------- |
| 表生成   | Node ICU `TextDecoder('gb18030', {fatal:true})`                   | 双字节 23940 全定义；BMP 四字节 50400 槽（有效 39420 / 209 游程）；增补平面复验 7656 条     |
| 独立验算 | CPython `gb18030` codec（`tools/encoding/crosscheck-gb18030.py`） | mismatch = 0 → `GB18030_CROSSCHECK = PASS`                                                  |
| 分歧仲裁 | JDK `Charset.forName("GB18030")`（Android 冻结口径）              | 21 个分歧码位，JAVA 同意 ICU 20 / 同意 CPython 1 / 都不 0；仅 `A3A0` 采用 override `U+E5E5` |

产物：`tools/encoding/gb18030-divergences.json`（21 条逐条记录）、
`Gb18030Table.ets` / `Gb18030Table.swift`（生成物，未手抄）。
解析侧是**声明驱动**的：先严格 UTF-8，仅当 mapping 声明了 GB18030 系列
charset 才回退，不做静默兜底。

```
HARMONY_CONFORMANCE_HOST=PASS
HARMONY_HOST_PASS=87/91   fail=0
HARMONY_ENV_BLOCKED=0
HARMONY_DEVICE_BLOCKED=4  (Argon2id 原生 / ArkData)
```

### 2. iOS N4：canonical 91 条全部真实执行

PDIGCore 移植模块（均为 Android 冻结源的忠实移植，非重新设计）：
`Models / Relations / ImpactKernel / PlanRules / ScenarioRegistry /
StateMachines / GraphRevision / Json / Jcs / DepmapContainer / Schema /
Migrations（由 Kotlin 源生成）/ SchemaMigrator / Parsers / Gb18030 /
Timeline / SqliteDriver / GraphSerialize`；
`PDIGConformance` 为 fixture 驱动执行器；`PDIGArgon2` 通过转发头接
vendored Argon2（不复制源码进包，VENDOR.json 哈希校验才成立）。

CI（macos-14，run **35427324918**，head `9cd55f5`）：

```
IOS_TOTAL_CANONICAL      = 91
IOS_HOST_EXECUTED        = 91   (fail=0)
IOS_HOST_IMPL_MISSING    = 0
IOS_ENV_BLOCKED          = 0
IOS_CONFORMANCE_HOST     = PASS
IOS_HOST_PASS            = 91/91
```

分类账（逐项，不用汇总值代替）：relations 18 / jcs 1 / scenario 1 /
parser 22 / impact 13 / readiness 16 / coverage 6 / timeline 3 /
state-machine 5 / depmap 3 / migration 2 / backup 1 = **91**。

### 3. 本轮修掉的两个真缺陷（都不是"改测试让它绿"）

1. **门禁假绿**：`swift build 2>&1 | tail -40` 的退出码取自 `tail`，
   编译错误被吞掉、job 仍报 success。已去掉管道，退出码直接透传。
2. **Swift `Character` 是字素簇**：`"\r\n"` 是**一个** Character，
   `Array(text)` + `ch == "\r"` 永远匹配不到 CRLF → `parser-csv-crlf`
   表头最后一列变成 `currency\r\n`。已改为按 `unicodeScalars` 扫描
   （`splitLines` / `parseCsvLine` / `parseCsv`），是解析层修复，非用例规避。

另有 codegen 侧三处：保留字 case 未加反引号（`in` / `any` / `open`）、
`dq()` 未转义控制字符（TAB 分隔符字面量进源码）、raw-value enum 缺
Equatable 合成条件 —— 全部修在生成器 `tools/codegen/generate.mjs`，
不手改生成物。

### 4. 口径边界（必须照此书写，不得升级）

- iOS 91/91 证明的是 **canonical 逻辑一致性**，**不是**以下任何一项：
  - 不是「iOS App 已接入 SQLCipher」——host harness 用系统 sqlite3，
    仅覆盖 payload 序列化与迁移逻辑；at-rest 加密仍是 App 层未完成项；
  - 不是「iOS UI / Keychain / LocalAuthentication 已完成」——SwiftUI、
    生物识别解锁、Keychain 尚未开工；
  - 不是「iOS 真机跑过」——全部结论来自 macos-14 runner 的编译 + 单测，
    **无设备运行时证据**。
- Harmony 仍为 87/91（4 条设备运行时：Argon2id 原生 / ArkData）。

### 5. 跨平台差分（N5）

三份报告各自独立产生，差分工具 `tools/conformance/diff-reports.mjs` 只做比对、
**不重算任何用例**：

| 平台    | 报告                               | 产生方式                                                                        | 结果                                       |
| ------- | ---------------------------------- | ------------------------------------------------------------------------------- | ------------------------------------------ |
| android | `conformance/reports/android.json` | `JAVA_HOME="D:/Code/Android Studio/jbr" ./gradlew --no-daemon :conformance:run` | `pass=91 fail=0 notImplemented=0 total=91` |
| harmony | `conformance/reports/harmony.json` | 真 ArkTS 运行时（hvigor 本地单测）                                              | `executed=87 pass=87 fail=0`               |
| ios     | `conformance/reports/ios.json`     | macOS runner `swift test`（run 35427846349，head `61fb69d`）                    | `91/91`                                    |

```
CROSS_PLATFORM_VERDICT_MATRIX     = PASS   （0 条判定分歧）
CROSS_PLATFORM_ACTUAL_ANDROID_IOS = PASS   （91/91 actual 逐字节相同）
CROSS_PLATFORM_DIFFERENTIAL       = PASS
```

- `actual` 比对用自带的规范化序列化器（保留数字原始文本与对象键顺序）——
  `JSON.parse` 会把 `1.0` 与 `1` 合成同一个 number，正是这类差异会被漏掉。
- Harmony **只参加判定矩阵，不参加字节比对**：ArkTS 主机测试不保证可写文件，
  故不产出 `actual`。这是能力边界，已在工具与输出里明示，未用占位值填补。
- `conformance/reports/` 被 `.gitignore` 排除（生成物），证据留在运行输出与
  本报告里，不入库。

### 6. 当前唯一可写口径

```
GITHUB_PORTABLE_CI                  = PASS
HARMONY_HOST_CONFORMANCE_LOCAL      = PASS  (canonical 87/91, ENV_BLOCKED 0, DEVICE_BLOCKED 4)
IOS_CI_COMPILE_AND_HOST_CONFORMANCE = PASS  (canonical 91/91, fail 0)
ANDROID_CONFORMANCE                 = PASS 91/91 (JVM, 本机 JDK21 复跑)
ANDROID_CORE_TEST                   = PASS 71/71 (6 类, 本机 --rerun 实测)
ANDROID_DEBUG_BUILD                 = PASS (:app:clean :app:assembleDebug, APK 36,958,501 B)
ANDROID_DEVICE_RUNTIME_EMULATOR     = PASS (AVD Android 34 x86_64, install+start+home UI 无崩溃)
WEB_CORE_STATIC_GATES               = PASS (typecheck/lint/format:check/architecture/network/secrets/ui)
WEB_CORE_UNIT_TESTS                 = PASS 453/453 (43 files)
WEB_CORE_ORACLE_SELFCHECK           = PASS 91/91
WEB_UTS_COMPILE_GATE                = PASS 15/15
WEB_APP_H5_BUILD                    = NOT_AVAILABLE (BLOCKERS B10: 无 HBuilderX/uni-app x CLI)
CROSS_PLATFORM_DIFFERENTIAL         = PASS  (verdict 0 分歧；Android×iOS actual 91/91 逐字节相同)
NATIVE_MIGRATION                    = NOT PASS（11 条 Cutover 条件 8 PASS / 3 未满足，
                                       见 NATIVE_MIGRATION_ACCEPTANCE_2026-09-19.md）
IOS_DEVICE_RUNTIME                  = NOT_RUN
HARMONY_DEVICE_RUNTIME              = NOT_RUN
```

> 安卓端和 web(core) 端复验细节见 `VERIFICATION_ANDROID_WEB_2026-09-19.md`。
> 复验同时修掉 3 个真缺陷：
>
> 1. `scripts/generate-conformance.ts` 编译失败（WeChatStatementAdapter 缺 driver + any 扩散）；
> 2. `scripts/check-secrets.mjs` 误报 `spec/ui/design-tokens.json`；
> 3. `tests/integration/multi-source-e2e.test.ts` K6 在并发 perf 饱和下默认 5s flaky。
