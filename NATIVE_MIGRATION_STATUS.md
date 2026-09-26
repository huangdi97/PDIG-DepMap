# NATIVE_MIGRATION_STATUS.md

> 持续更新。格式：PHASE / ANDROID / HARMONY / IOS / CONFORMANCE / BLOCKERS / NEXT。
> 状态枚举：`PASS` `FAIL` `BLOCKED` `NOT_RUN` `PARTIAL_WITH_REPORT`

> 更新：2026-09-25（**Harmony N3 连续推进轮 #3 —— ArkTS 内存持久化执行层**；round#2 基础上继续）
>
> - **新增 4 个纯 ArkTS 模块**（零 @ohos，真实编译进 HAP）：`data/GraphStore.ets`（内存存储 + 快照/克隆）、
>   `data/GraphRepository.ets`（事务 + node/dependency/group/proposal/evidence/fingerprint/import-session
>   语义 + revision 同事务 bump + payload 原子导入导出）、`data/MigrationChain.ets`（schema 1→2→3 链应用，
>   幂等 / 失败回滚 / revision 不 bump）、`data/RepositorySelfCheck.ets`（编译图入边）。
> - **`HARMONY_BUILD = PASS`**（clean）：HAP `entry-default-unsigned.hap` = **3,511,091 B**，
>   SHA-256 `1213f05acd788e4f7fe0f4d0b0bc5e261bf81898627b7b6c12206a21b2c15fec`。
> - **`HARMONY_MODULE_COMPILED = PASS`**：`check-compiled-reachability.mjs --build` → **35/35 required 模块**，
>   负向 probe PASS，孤儿 0。
> - **`HARMONY_CONFORMANCE_HOST = PASS`**：**122/122 host checks**（87 canonical + 3 元测试 + 1 domain 自检
>   - 14 persistence + **17 repository**），0 fail；canonical 口径不变（87 executed / 4 DEVICE-BLOCKED）。
> - **Parity**：Harmony 27/73 → **29/73**（§2 持久化 5 → 7 TESTED：新增 Migration v1→v3 链、迁移失败回滚）。
> - 未进入 iOS N4；`spec/`、`fixtures/`、`conformance/expected/` 零改动；codegen --check PASS。
>   更新：2026-09-25（**Harmony N3 连续推进轮 #2 —— ArkTS 持久化逻辑层**；round#1 基础上继续）
>
> - **新增 5 个纯 ArkTS 模块**（零 @ohos，真实编译进 HAP）：`data/SchemaV3.ets`（schema/payload 契约常量）、
>   `data/PayloadCodec.ets`（payload 构建·校验 + 5 项孤儿完整性）、`data/PayloadMigration.ets`
>   （v1→v2→v3 纯函数迁移 + legacy 工厂）、`domain/SourceInstance.ets`（零依赖：SRC-01 作用域契约）、
>   `data/PersistenceSelfCheck.ets`（编译图入边 + 自检行）。
> - **`HARMONY_BUILD = PASS`**（clean）：HAP `entry-default-unsigned.hap` = **3,438,086 B**，
>   SHA-256 `96c974fbdaff2edfeb4bf311759b4a5096c685a6f854ed762b96d3e0aefe3926`（非字节可复现，同上轮口径）。
> - **`HARMONY_MODULE_COMPILED = PASS`**：`check-compiled-reachability.mjs --build` → **31/31 required 模块**
>   reachable + 入 `modules.abc`（461,272 B），负向 probe PASS，孤儿 0。Gate 首跑抓到真实缺陷：
>   `domain/SourceInstance.ets` 曾无入边（仅被测试引用）→ 由 `PersistenceSelfCheck`（data→domain 方向）接入。
> - **`HARMONY_CONFORMANCE_HOST = PASS`**：`run-conformance-host.mjs` → **105/105 host checks**
>   （87 canonical + 3 元测试 + 1 domain 自检 + **14 条新持久化测试**），0 fail；
>   canonical 口径不变（87 executed / 4 DEVICE-BLOCKED），`HARMONY_HOST_PASS = 87/91`。
> - **Parity**：Harmony 22/73 → **27/73**（§2 持久化 0 → 5 TESTED(host) + 2 ICNR；明细见矩阵
>   「Harmony 27/73 的来源」）。DB repository / UI 仍为下一阶段缺口。
> - 未进入 iOS N4；`spec/`、`fixtures/`、`conformance/expected/` 零改动；`codegen --check` PASS。
>   更新：2026-09-25（**Harmony N3 恢复轮 —— 代码侧 parity 推进**；E-9 PAUSED → ACTIVE，用户批准重启）
>
> - **`HARMONY_BUILD = PASS`**：`hvigorw assembleHap --mode module -p product=default -p buildMode=debug
--no-daemon`（ASCII 镜像 + `--clean` 全量重建）→ `BUILD SUCCESSFUL in 1 min 3 s`；HAP
>   `entry-default-unsigned.hap` = **3,380,659 B**，SHA-256
>   `610701e006a857dde7e6cd16a4dba57b4d74158a551ba25fe54edb82a9057c21`
>   （HAP 是 zip、含构建时间戳，**非字节可复现**：同源码两轮 clean 构建哈希依次 86f1bc53… / 610701e0…，字节数一致）。
> - **`HARMONY_MODULE_COMPILED = PASS`**：`node tools/harmony/check-compiled-reachability.mjs --build`
>   A/B/C/D 四判据全过：**26/26 required 模块 reachable**、全部存在于 `modules.abc`（432,560 B）、
>   代表模块负向 probe（注入类型错误 → clean 构建必须失败）PASS；**孤儿模块 = 0**（30 found / 30 reachable）。
> - **`HARMONY_CONFORMANCE_HOST = PASS`**（2026-09-25 新鲜）：`node tools/harmony/run-conformance-host.mjs`
>   → **91/91 host checks**（87 canonical + 3 conformance 元测试 + 1 domain 自检），0 fail；
>   **`HARMONY_HOST_PASS = 87/91`** —— 87 条 canonical 在真实 ArkTS 运行时逐字节复现冻结 expected；
>   余 4 条 = DEVICE-BLOCKED（depmap-golden-v1 / depmap-utf8-password-normalization /
>   migration-db-v1-to-v3 / backup-depmap-export-restore-roundtrip），统一 harness 如实记 FAIL
>   （no result reported），**不写 PASS**；`conformance/reports/harmony.json` + `SUMMARY.json` 已刷新。
> - **`HARMONY_DEPMAP = NATIVE_BUILD_PASS / ON_DEVICE_NOT_RUN`**（保持）：`libpdiargon2.so`
>   （arm64-v8a 40,768 B / x86_64 42,296 B，strip 后动态符号仅 3 个）在 HAP 内，Argon2id NAPI 已真实绑定。
> - **`HARMONY_RUNTIME_E2E = RUNTIME_NOT_RUN`**（本轮唯一外部项，不变）：缺 Emulator 系统镜像，
>   需华为账号登录下载（见 `HARMONY_RUNTIME_ENVIRONMENT_AUDIT.md`）。
> - **Parity**：Harmony 0/73 → **22/73**（§1 领域/语义 12 CONFORMANCE_PASS(host) + 3 IMPLEMENTED_COMPILED_NOT_RUN ·
>   §2 持久化 0（无 ArkTS repository 层，reachability 显示 data/Repository.ets = NOT_IMPLEMENTED）·
>   §3 安全 2 CONFORMANCE_PASS（jcs / bounds）+ 3 IMPLEMENTED_COMPILED_NOT_RUN（容器/Argon2+AES/UTF-8，后两者 DEVICE-BLOCKED）·
>   §4 导入/解析 7 CONFORMANCE_PASS（parser 22/22）· §5 UI 0（仅 Index.ets）· §6 工程 1 TESTED（host 单测 91 checks））。
>   逐格依据见 `NATIVE_PARITY_MATRIX.md`「Harmony 22/73 的来源」。**RUNTIME_VERIFIED 一格未增**。
> - **回归**：`core/` `npm run check` 全绿（**453/453** tests；format/lint/typecheck/architecture circular=0 /
>   network / secrets / ui 全 PASS）；`node tools/conformance/run.mjs`：codegen PASS · fixtureIntegrity 91/91+28/28 ·
>   oracle PASS · **android PASS 91/91** · harmony 87/91（4 条 device-blocked，未写 PASS）· ios 91/91
>   （2026-09-19 旧记录）。`spec/`、`fixtures/`、`conformance/expected/` **零改动**（codegen --check PASS 佐证）。
> - 本轮**未进入 iOS N4**（E-8 仍需 macOS）；Android（CORE_FROZEN）/Desktop 零改动 —— 仅修复 1 个 v0.2.0
>   既有文档格式回归（`docs/release-evidence/v0_2_0_download_smoke/desktop-download-smoke.md` prettier 空行，
>   纯格式零内容）。后续代码缺口：ArkTS 持久化层（data/Repository.ets）与 UI 页面 parity（下一阶段）。
>   更新：2026-09-23（**ANDROID API36 工程全速收口轮**：compileSdk/targetSdk→36、API36 双 AVD 全量回归、
>   Core Journey 41/41、三场景 32/32、edge-to-edge/predictive-back/adaptive-layout 三项 Gate PASS、
>   NON-PROD 签名链路验证、N2 parity 保持 69/73、外部 Gate 保持 BLOCKED；Harmony/iOS 本轮未进入）
>
> 更新：2026-09-24（**v0.1.2 质量迭代收口轮**）：
>
> - **Android**：preview flavor versionCode 200002→**200003**、versionName "0.1.1"→"0.1.2"；
>   `assemblePreviewRelease -PpdigNonProdSigning=true` BUILD SUCCESSFUL（1m19s，52 tasks）；
>   APK `com.pdig.app.preview` versionCode=200003 versionName=0.1.2 platformBuild API 36；
>   apksigner verify v2 scheme Verified（NON-PROD 测试签名，非生产签名）；`:core:test` 71/71、
>   `:app:testDebugUnitTest` 9/9（--rerun-tasks fresh）；仪器化基线
>   `TEST-pdig36(AVD)-16-_app-preview.xml` **60/60 PASS**（2026-09-24 11:39）。
> - **Android 运行时 smoke = PASS（环境修复后新鲜证据，2026-09-24 晚）**：android-36 系统镜像损坏
>   （目录仅剩 `.installer` 锁文件）经 sdkmanager 重装 `system-images;android-36;google_apis;x86_64`
>   修复，`pdig36` AVD 恢复可引导（~90s）；`adb install` preview-release APK Success +
>   launch MainActivity 进程存活（pid=3204）+ topResumedActivity=MainActivity + crash buffer 空 +
>   截图 1080×2340；`connectedPreviewDebugAndroidTest` BUILD SUCCESSFUL（1m57s）→ TEST XML
>   **60/60 PASS（0 failure/0 error/0 skipped，pdig36 AVD 新鲜）**；卸载数据 Success、包与数据移除、
>   crash 空。环境特性记录：模拟器子进程随启动 shell 退出被回收（设备操作须单命令内完成）；
>   PATH 旧 adb 1.0.32 与 platform-tools 1.0.41 冲突（前缀 platform-tools 解决）。
> - **Conformance（fresh SUMMARY 2026-09-24）**：android **91/91 PASS**（fresh，pass=91 fail=0 total=91）；
>   harmony 87/91（4 项 runtime-blocked：depmap-golden-v1、depmap-utf8-password-normalization、
>   migration-db-v1-to-v3、backup-depmap-export-restore-roundtrip，2026-09-19 旧记录，本机无 HarmonyOS
>   运行环境）；ios 91/91（2026-09-19 旧记录）。
> - **质量 Gate**：`node scripts/quality/check-quality.mjs` → VERDICT PASS exit 0（2026-09-24T06:56Z），
>   10 项计数全 0；file-size OK（exempted 3）；EXCEPTIONS.json 复核合法 UTF-8 JSON（fileSizeOver300×2
>   migration/schema、kotlinEscapes×2 lateinit 均 JUSTIFIED、secretPattern×2 fixture 合成口令）。
> - **Desktop**：packageVersion/version/描述 0.1.1→0.1.2；`:app:test` BUILD SUCCESSFUL（27s）；
>   `--smoke` **16/16 PASS**；PDIG.exe GUI 启动 20s 无崩溃（窗口 1100×720 固定）；Windows 打包产出
>   NSIS installer + portable zip + SBOM（CycloneDX 1.5，151 components）+ THIRD-PARTY-NOTICES.md +
>   SHA256SUMS.txt；新文件 `RELEASE_NOTES_0_1_2.md`、`PRODUCT_V0_1_2_RELEASE_MANIFEST.md` 已创建。
> - 已知限制：无 Play 生产发布；无真机/真实数据（全合成 fixture）；Harmony/iOS 不在本轮；
>   Windows 未签名（SmartScreen）；CI billing E-10（CI_EXTERNAL_BLOCKED）；
>   旧 tag product-v0.1.0/v0.1.1 保留。
>
> API36 轮关键结论（详见 `ANDROID_16_API36_CLOSURE_REPORT.md`）：
>
> - **API36 全绿**：`:core:test` 71/71 · `:app:testDebugUnitTest` 9/9 · `:conformance:run` 91/91 ·
>   connectedDebugAndroidTest **59/59 × 2**（phone + tablet）· Core Journey **41/41** · 三场景 **32/32** ·
>   assembleDebug/assembleRelease/bundleRelease SUCCESSFUL · crash-scan 0。
> - **ANDROID_API36_READY = PASS（工程可验证范围）**；真机级指纹匹配/字体/缩放仍 BLOCKED_BY_REAL_DEVICE（E-1）。
> - **本轮驱动脚本修复（非产品缺陷）**：① API36 新 DocumentsUI 单击=预览、长按+Select 才确认，
>   驱动 `saf_pick` 增加长按回退；② API36 手势导航导航条窗口名=Taskbar（非 NavigationBar），
>   `navigation_bar_top()` 改从 InsetsSource 读取 → `content_bottom()=2340`，底部按钮不再落进手势区。
> - **剩余 blocker 全部为真实 EXTERNAL_BLOCKER**：真机 / 生产 keystore / applicationId 与品牌决策 /
>   Play 账号 / 公网 URL / 授权账单 / closed test（见 BLOCKERS.md E-1..E-10）。
> - Harmony N3、iOS N4 本轮**不进入**（契约 Boundary；Android 主发布轨未完成发布不切换）。
>   本轮关键结论：
> - **PUSH_TRIGGER = PASS**（branches 显式列举修正后，event=push 已在基准 HEAD 出现并全绿：
>   run 35700579040 CI + 35700579087 iOS，head_sha=`89b653a…`，详见 `ANDROID_CI_EXACT_SHA_POLICY.md`）
> - **回归全绿（本轮实跑）**：`:core:test` 71/71 · `:app:testDebugUnitTest` 9/9 · `:conformance:run` 91/91 ·
>   connectedDebugAndroidTest **59/59**（emulator-5554）· assembleDebug/assembleRelease/bundleRelease SUCCESSFUL
> - **剩余 blocker 全部为 EXTERNAL_BLOCKER**：真机 / 生产 keystore / 身份决策 / 品牌 / Play 账号 / 公网 URL /
>   授权账单 / 商店人工提交（见 BLOCKERS.md E-1..E-8）
> - Harmony N3、iOS N4 本轮**不进入**（契约 Boundary）
>
> ## 本轮（2026-09-21）：Android 产品收口（由用户暂停 Harmony/iOS，Android 从 CORE_FROZEN 临时解除）
>
> - **新增功能收口（非新业务）**：DiscoveryCandidate 确认/忽略流、RealityDrift 四选一解决流、
>   首页 Attention 聚合（Proposal/Candidate/Drift 计数）、设置「删除所有数据」（L-37）、Onboarding 正式取消（D-9）。
> - **设备验证新证据**：指纹 AVD（API35 google_apis_playstore `hw.fingerprint=yes`）跑通
>   指纹成功/失败/取消 + PIN 正确/错误 + 前后台回锁；Dark Mode 像素级验证；删除所有数据设备测试 1/1；
>   CandidateDrift 设备测试 7/7。
> - **E2E**：Core Journey v4 `core-journey-v4-20260920-200002` = **41/41 PASS / 0 FAIL**；
>   三场景 E2E `scenario-e2e-20260921-151631` = **40/40 PASS / 0 FAIL**（replace/expiring/close 全闭环）。
> - **parity 62 → 69 / 73**（逐格重审）：生物识别/App Lock 与 Dark Mode 两格升级 RUNTIME_VERIFIED；
>   Onboarding 按产品决策移除。剩余 4 格 = TalkBack 实机（环境）+ production keystore（用户）+
>   Store 素材（品牌决策）+ R8（未开 minify 如实 NOT_APPLICABLE）。
> - **CI 扩大（O-51）**：新增 `android-app` job（`:app:testDebugUnitTest` + `:app:assembleDebug`，
>   hosted runner 自带 SDK），末期 run **35520532048 全绿**。
> - 见 `ANDROID_PRODUCT_FINAL_ACCEPTANCE.md`（40 节完整验收）。
>
> ## 本轮（2026-09-18 第三场）：Argon2 全链路 + 编译可达性 Gate
>
> - **`ARGON2_NATIVE_BUILD = PASS`**：vendored PHC 参考实现（tag `20190702` /
>   commit `62358ba2…` / `local_modifications = 0`）经 OHOS NDK 交叉编译，
>   **arm64-v8a (`AArch64`, 40,768 B) + x86_64 (`Advanced Micro Devices X86-64`, 42,296 B)** 双 ABI 通过；
>   导出 `argon2_*` 符号 **0** 个（此前误设 `-DA2_VISCTL=1` 会导出 22 个 —— `A2_VISCTL` 的语义是**导出**而不是隐藏）。
> - **端到端取证**：`libpdiargon2.so` 已打进 HAP
>   （`libs/{arm64-v8a,x86_64}/`，49,776 / 50,576 B），
>   **打包并 strip 后**的动态符号表恰好 3 个（`_init` / `_fini` / `RegisterPdiArgon2Module`），`argon2_*` = 0。
> - **`HARMONY_MODULE_COMPILED = PASS`**：新增 `tools/harmony/check-compiled-reachability.mjs`
>   固化 A/B/C/D 四判据。该 Gate **立刻抓到一个真实缺陷**：
>   `Argon2idNative.ets` 因依赖方向反转（为拿一个类型而 import 容器）成为**孤儿模块** ——
>   源文件存在、从未被编译、`assembleHap` 依然 BUILD SUCCESSFUL。
>   修法：抽出中立 `crypto/KdfContract.ets`，两侧只依赖它，由 `pages/Index.ets` 建立入边。
> - **`HARMONY_DEPMAP` 变化**：`BLOCKED_BY_NATIVE_VERIFICATION` → **`NATIVE_BUILD_PASS / ON_DEVICE_NOT_RUN`**。
>   平台 API 无 Argon2 仍是真的，但**不再是 blocker** —— 已改走自建 NAPI 路径且全链路打通。
> - **`HARMONY_RUNTIME_E2E` 仍为 `RUNTIME_NOT_RUN`**：阻断点已根因定位到
>   **缺 Emulator 系统镜像**（SDK 内无任何 `images`；`hdc list targets = [Empty]`），
>   需人工登录华为账号下载 —— 见 `HARMONY_RUNTIME_ENVIRONMENT_AUDIT.md`。
> - **`WORK_STATUS.md` Current 区已修正**（三段过期叙述归档至 Historical，未删除）。
> - 按 stop condition：**未进入 iOS N4**。
>
> 详见 `HARMONY_ARGON2_INTEGRATION_REPORT.md` / `HARMONY_COMPILE_REACHABILITY_GATE.md` /
> `HARMONY_N3_IMPLEMENTATION_STATUS.md` / `HARMONY_N3_RUNTIME_REPORT.md`。

> **上一轮（2026-09-17 第二场）**：Android 冻结 + 正式进入 Harmony N3。
> 人工 Final Acceptance 已给出：`N1 = PASS`、`N2 = PARTIAL_WITH_REPORT`、
> `ANDROID_PRODUCTION_RELEASE_READY = BLOCKED_BY_PRODUCTION_SIGNING`、D-16 CLOSED。
>
> - **新增** `ANDROID_NATIVE_CORE_HANDOFF = PASS`（**不替代** N2，也不替代 release readiness；
>   只回答"Android 是否已可作为 Harmony N3 的 Native Reference"）→ `ANDROID_NATIVE_CORE_FREEZE.md`
> - Android 进入 **`CORE_FROZEN / MAINTENANCE_ONLY`**；不再为 parity 分数新增功能；剩余 11 格入 N2 Backlog
> - **Git 尾项已收口**：三端 codegen 产物与 `legacy/README.md` 入库；
>   `.pi/` 保持 untracked（gitignore 覆盖）
> - **正式进入 Harmony N3**：`harmony/` 由"仅 codegen"建成**可被 hvigor 真实构建并产出 HAP 的
>   Stage Model 工程**，`HARMONY_BUILD = PASS`；首个纯 ArkTS Domain（Relations）已编译并打包进 HAP
> - 按 stop condition：**未进入 iOS N4**

> **更早（2026-09-17 D-16 关闭轮）三个判定**：
> `N1_ANDROID_VERTICAL_SLICE` = **PASS**（核心垂直切片 E2E v4 全链路无 FAIL，崩溃 0）
> `N2_ANDROID_FULL_PARITY` = **PARTIAL_WITH_REPORT**（**62 / 73**，11 项未关闭）
> `ANDROID_PRODUCTION_RELEASE_READY` = **BLOCKED_BY_MISSING_PRODUCTION_KEYSTORE**
>
> 逐格结论见 `NATIVE_PARITY_MATRIX.md`；Gate 侧结论见 `ANDROID_N1_N2_FINAL_CLOSURE_REPORT_V2.md`。

> 历史：2026-09-16 的 P0 Runtime Closure 轮记录了 `N1 = PARTIAL_WITH_REPORT` /
> `N2 = PARTIAL_WITH_REPORT` / parity `56/73`（D-16 未修）。该轮结论**已被取代**，
> 但 D-16 的发现历史与根因完整保留在 `ANDROID_N1_N2_FINAL_CLOSURE_REPORT_V2.md`。

---

## PHASE

| N2   | Android Full Parity        | **PARTIAL_WITH_REPORT（69/73）**           | **62→69**（逐格重审）：生物识别/App Lock、Dark Mode 升级 RUNTIME_VERIFIED；Onboarding 决策移除。剩余 4 格 = TalkBack 实机（环境）/production keystore（用户）/Store 素材（品牌）/R8（未开 minify）。详见 `ANDROID_FINAL_73_AUDIT.md`                                                                                                                                                                                                                                                                                                                                 |
| ---- | -------------------------- | ------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| N0-A | 恢复仓库现场               | **PASS**                                   | git 全套审计；工作树 clean；HEAD `7bc0ed3`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| N0-B | Legacy 冻结                | **PASS**                                   | tag `v0.3.0-uniapp-reference` + manifest + README                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| N0-C | Canonical Spec             | **PASS**                                   | `spec/` 机器可读（枚举/实体/关系/状态机/错误/Schema/安全/UI）                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| N0-D | Codegen + Gate             | **PASS**                                   | spec → Kotlin/Swift/ArkTS；`--check` PASS                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| N0-E | Golden Fixtures            | **PASS**                                   | **91 用例** + 28 输入 fixture + manifest + sha256                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| N0-F | Conformance Harness        | **PASS**                                   | `node tools/conformance/run.mjs` 全绿                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| N1   | Android Vertical Slice     | **PASS**（2026-09-17 D-16 关闭后重新确认） | 核心垂直链 import→proposal→reality→影响面→changeplan→done→verified 在设备上端到端跑通；D-16 修复后**外部文件选择器往返不再丢工作流**，Import 真的写入（「记录 6 行」）。证据：`core-journey-v4-20260917-184856` = **41/41 PASS / 0 FAIL** + `FileWorkflowD16Test` 6/6                                                                                                                                                                                                                                                                                                |
| N2   | Android Full Parity        | **PARTIAL_WITH_REPORT**                    | **62 / 73**（D-16 关闭 +4，§1 计数口径修正 +1，设备 E2E +1）。未关闭 11 项逐格列在 `NATIVE_PARITY_MATRIX.md`                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| N3   | HarmonyOS Full Parity      | **ACTIVE**（工程推进中，parity 仍 0/73）   | `HARMONY_BUILD = PASS`（clean assembleHap，含 native）；**`HARMONY_MODULE_COMPILED = PASS`**（A/B/C/D 四判据，7/7 required 模块）；**`ARGON2_NATIVE_BUILD = PASS`**（arm64-v8a + x86_64，导出 `argon2_*` = 0）；**`HARMONY_DEPMAP = NATIVE_BUILD_PASS / ON_DEVICE_NOT_RUN`**（全链路已打通至打包进 HAP 且符号表只导出 NAPI 入口）；`HARMONY_DOMAIN = PARTIAL_WITH_REPORT`（仅 Relations）；`HARMONY_ARKUI = PARTIAL_WITH_REPORT`（骨架 + 1 占位页）；**`HARMONY_RUNTIME_E2E = RUNTIME_NOT_RUN`**（缺 Emulator 系统镜像）。详见 `HARMONY_N3_IMPLEMENTATION_STATUS.md` |
| N4   | iOS Full Parity            | **NOT_STARTED**                            | 仅 codegen 产物；build `BLOCKED_BY_MACOS`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| N5   | Cross-platform Conformance | **PARTIAL_WITH_REPORT**                    | Android **91/91 PASS**（本轮实跑复验）；Harmony **NOT_RUN**（0 执行，87 notImplemented / 4 blocked）；iOS 未开始。见 `CROSS_PLATFORM_CONFORMANCE_MATRIX.md`                                                                                                                                                                                                                                                                                                                                                                                                          |
| N6   | Legacy Cutover             | **NOT_STARTED**                            | 未满足 Cutover 条件（三端 parity 未达成）                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| N7   | Native Production RC       | **NOT_STARTED**                            |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |

---

## ANDROID

| 层                           | 状态                                           | 证据                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| ---------------------------- | ---------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 工程脚手架                   | **PASS**                                       | `android/` Gradle (Kotlin DSL)，`:core` + `:conformance` + `:app` 三模块                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| Domain（纯 Kotlin）          | **PASS**                                       | domain / impact / plan / scenario / statemachine / schema / json / sources / serialize / timeline                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| Codegen 枚举                 | **PASS**                                       | `android/core/.../generated/CanonicalEnums.kt`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| Crypto（`.depmap`）          | **PASS**                                       | 黄金向量 derivedKey / ciphertext / tag **逐字节一致**                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| JCS (RFC 8785)               | **PASS**                                       | 规范用例 + 浮点拒绝                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| **Conformance**              | **PASS**                                       | **91/91**（harness 独立复核，非自我宣称）                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| 解析器（WeChat / CSV / OFX） | **PASS**                                       | 22 个平台中立 fixture 全通过（BOM/CRLF/CR-only/引号逗号/分号/借贷列/多币种/GB18030/FITID 缺失/非法日期/坏块）                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| 持久化（SQLCipher）          | **PASS**                                       | `net.zetetic:sqlcipher-android:4.5.5`；`Migrations.kt` v1→v2→v3，事务 / 回滚 / 幂等 / ID 保留全部验证                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| Schema 迁移                  | **PASS**                                       | `migration-db-v1-to-v3`：版本到 3、legacy 归属、50 次重复执行严格 no-op                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| Backup / Restore             | **PASS**                                       | `backup-depmap-export-restore-roundtrip`：导出→加密→解密→恢复→再导出**逐字节相同**，无孤儿引用                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| Keystore                     | **PASS**                                       | `DatabaseKeyStore`：AES-256-GCM 密钥由 Android Keystore 生成且不可导出，包裹 DB passphrase                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| Biometric / App Lock         | **RUNTIME_VERIFIED**（2026-09-21 升级）        | API35 google_apis_playstore AVD（`hw.fingerprint=yes` + 真实录入）跑通：指纹成功/失败/取消、PIN 正确/错误、冷启动先锁/前后台回锁/锁定时 NavGraph 不参与组合。`AppLockNavigationTest` 7/7                                                                                                                                                                                                                                                                                                                                                                                                  |
| UI（Compose）                | **PASS**                                       | NavHost 注册 **21 个目的地**（本轮新增 Settings 入口变更 + 删除所有数据）；Onboarding 已按 D-9 产品决策移除注册。Lock 是 App 的门（不参与 NavHost 组合）                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| Design System                | **PASS**                                       | `PDIGTheme` + tokens（color / spacing / radius / typography / status）映射自 `spec/ui/design-tokens.json`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| **Build（APK/AAB）**         | **PASS**（2026-09-21 fresh clone 重算）        | `app-debug.apk` 37,123,275 B `E78E60B8…`；`app-release-unsigned.apk` 33,114,029 B `C9BFF575…`；`app-release.aab` 20,861,666 B `4F7090F7…`；androidTest 1,185,921 B `2423E49C…`（明细见 `ANDROID_PRODUCT_FINAL_ACCEPTANCE.md` §31）                                                                                                                                                                                                                                                                                                                                                        |
| **Gradle Wrapper**           | **PASS**                                       | 本轮新增。`gradlew` / `gradlew.bat` / `gradle-wrapper.jar`(43,504 B) / `gradle-wrapper.properties`（Gradle 8.9，官方 `distributionUrl`，无机器绝对路径）。此前**完全缺失**，构建依赖本机绝对路径 Gradle                                                                                                                                                                                                                                                                                                                                                                                   |
| Runtime / 核心行程           | **PASS**（2026-09-17 D-16 关闭后重新确认）     | AVD `emulator-5554`（API34）核心行程 **v4** 全新 run：全新安装 → SAF 导入真实 CSV（2 支付方式 / 3 收款对象）→ 提交「记录 6 行」→ 候选 → 确认 Reality → 用户标记必需 → 影响面「必须处理（2）」→ 变更计划 → done≠verified → 验证 → `am kill`（真实进程死亡，先按 HOME 再 kill）重建后**首屏是锁屏**且数据仍在（共 5 个对象）→ `.depmap` 导出 13,617 B 且 UI 文案一致 → 错误密码恢复被拒 → 清数据 → 正确口令恢复成功 → 篡改容器被拒。崩溃 0。**最终 run id = `core-journey-v4-20260917-184856`（41/41 PASS / 0 FAIL）**，详见 `local_private\e2e\core-journey-v4-20260917-184856.{txt,json}` |
| Runtime / 设备 E2E           | **PASS**（2026-09-17 升级）                    | 安装 / 首次启动 / 首页 / 场景中心 / 基础设施总览 / 加密持久化 / 明文 sqlite 无法打开 / 前后台切换 / 杀进程重启 / 清状态 / 触摸目标 / 字体缩放 / 横屏 / 焦点顺序 / logcat 隐私 **均 PASS**；核心行程 v4 覆盖 Import / Restore 全链路。**仍 NOT_RUN：TalkBack**（镜像未预装、无 Play 商店）；Onboarding / Timeline / Graph 二级页未被真机走过（无入口或无流程触发）                                                                                                                                                                                                                         |
| `:app` JVM 单测              | **PASS**（2026-09-17 新增）                    | **9 / 9**：`FileWorkflowStateTest`（D-16 工作流状态机）。**此前 `:app` 的 JVM 单测是 NO-SOURCE**（目录里放多少文件都跑 0 个用例却 BUILD SUCCESSFUL）——本轮把 `src/test/kotlin` 移到 AGP 标准源目录后真实执行                                                                                                                                                                                                                                                                                                                                                                              |
| 设备内 androidTest           | **PASS**                                       | **59 / 59 PASS**（0 skipped / 0 failed）：AccessibilitySemantics 14 + AppLockNavigation 7 + BackupExport 3 + **CandidateDrift 7 + DeleteAllData 1** + DepmapRuntime 4 + FileWorkflowD16 6 + ImportHitbox 2 + PerfSmoke 1 + Persistence 8 + RepositoryKeystore 4 + ScreenProtection 2（pm clear 隔离批次）                                                                                                                                                                                                                                                                                 |
| 性能 smoke                   | **PASS**                                       | **有效数据**：`csvRowsParsed=10000`、`csvParseErrors=0`，强断言 `assertEquals(10_000, rows)` 通过。旧数字（parse=2427ms / insert=4839ms）因 `csvRowsParsed=0` 已**作废**                                                                                                                                                                                                                                                                                                                                                                                                                  |
| 无障碍                       | **PARTIAL_WITH_REPORT**                        | Compose 语义树 14 屏 0 无标签可交互节点（AccessibilitySemanticsTest 14/14）；触摸目标/焦点/字体缩放/横屏 PASS。**TalkBack 实机读屏 NOT_RUN**（镜像无 Play 商店）→ 记为环境受限项，非工程缺口                                                                                                                                                                                                                                                                                                                                                                                              |
| 截图保护                     | **PASS**（2026-09-16 升级为 RUNTIME_VERIFIED） | 真机 **6/6 路由双证据**：敏感页（SOURCES / IMPORT / INFRASTRUCTURE / BACKUP）窗口 `fl=` 含 `SECURE` 且 `screencap` 被抹黑（均值 0.17）；非敏感页（HOME / SETTINGS）无 `SECURE` 且截图正常（均值 244.64）。**注**：此前"运行时 flag 未取得"是检测口径 bug —— `dumpsys` 输出的是裸 flag 名 `SECURE`，grep `FLAG_SECURE` 恒为 0                                                                                                                                                                                                                                                              |
| Store metadata               | **PARTIAL_WITH_REPORT**                        | `ANDROID_STORE_METADATA.md` 文案草稿完成；截图 / 图标 / 隐私政策公开链接 NOT_STARTED                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| Release 签名                 | **BLOCKED_BY_MISSING_PRODUCTION_KEYSTORE**     | `app-release.aab` 20,734,935 B 构建成功但**未签名**；非生产签名配置本轮**未生效**（与未签名产物同 SHA-256），已如实记录                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| logcat 隐私扫描              | **PASS**                                       | 改为 PID/UID 进程归属扫描后：`appLines=44`，6 类敏感关键字命中**全 0**。旧扫描曾把 Launcher3 的 `password:false` 系统字段误判为应用泄露（已修）                                                                                                                                                                                                                                                                                                                                                                                                                                           |

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

| 层              | 状态                      | 说明                                                                                                                                                                                                                                                                                                                                                                                      |
| --------------- | ------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 工程脚手架      | **PASS**                  | `harmony/` 已建 Stage Model 工程（AppScope / build-profile / oh-package / hvigorfile / entry module.json5 / EntryAbility / resources）。hvigor 全清重建 **BUILD SUCCESSFUL**                                                                                                                                                                                                              |
| Domain（ArkTS） | **PARTIAL_WITH_REPORT**   | `entry/src/main/ets/domain/Relations.ets` 已实现并**编译进 HAP**（HAP 内含域代码标记）；Impact / Readiness / Coverage / StateMachine / Timeline / Migration / GraphRevision / Scenario 未开工                                                                                                                                                                                             |
| Build           | **PASS**                  | HAP `entry-default-unsigned.hap`，**60,133 B**，sha256 `ac86a5af1a7f15d2ddba70b139b4cbe862d3d1af2898efa496ac05801f9c00fa`（未签名）                                                                                                                                                                                                                                                       |
| Crypto          | **COMPILED / KDF 未绑定** | AES-256-GCM + AAD 平台能力**具备**（`GcmParamsSpec{iv,aad,authTag}`）；JCS（RFC 8785 受限域）、AAD 构造、容器加解密已实现于 `entry/src/main/ets/crypto/`，主机侧黄金校验 **5/5 PASS**，ArkTS **真实编译**（`modules.abc` 符号取证 `ABC_VERDICT=PRESENT`）。Argon2id 仍**未绑定**（KDF 以 `Argon2idDeriver` 注入），故 `.depmap` 端到端仍 **不可运行** —— 见 `HARMONY_CONTAINER_V1_POC.md` |
| 持久化          | **NOT_STARTED**           | ArkData relationalStore 未开工                                                                                                                                                                                                                                                                                                                                                            |
| UI（ArkUI）     | **PARTIAL_WITH_REPORT**   | 骨架 + 1 个占位页；§L 的 17 个页面未开工                                                                                                                                                                                                                                                                                                                                                  |
| Conformance     | **NOT_RUN**               | 无设备/模拟器、未接本地测试框架；0 执行（87 notImplemented / 4 blocked / 91）                                                                                                                                                                                                                                                                                                             |
| Runtime E2E     | **RUNTIME_NOT_RUN**       | `hdc list targets = [Empty]`；无模拟器系统镜像（需 DevEco GUI 下载，用户侧外部闸门）                                                                                                                                                                                                                                                                                                      |

> 注意：`platforms/harmonyos/` 是**旧 UTS 路线**产物，**仅 Legacy Reference**，
> 不得成为 Production dependency，其 HAP 也不得计入 `harmony/` 任何 Gate 证据。

> **两个必须记住的构建坑**：
>
> 1. hvigor 拒绝非 ASCII 工程路径（校验 `process.cwd()`，无环境变量绕过；`mklink /J` 亦无效）
>    → 用 `tools/harmony/build-ascii-mirror.mjs` 做 ASCII 镜像构建。
> 2. hvigor 增量 `CompileArkTS` 会**漏掉新增文件**（曾报 UP-TO-DATE 且 SUCCESSFUL，
>    而新文件其实未编译）→ 验证性构建必须**全清**。

---

## IOS

| 层              | 状态                 | 说明                                              |
| --------------- | -------------------- | ------------------------------------------------- |
| 工程脚手架      | **NOT_STARTED**      | 仅 codegen 产物 `ios/Sources/PDIGCore/Generated/` |
| Domain（Swift） | **NOT_STARTED**      |                                                   |
| Crypto          | **NOT_STARTED**      |                                                   |
| Conformance     | **NOT_RUN**          |                                                   |
| Build / Test    | **BLOCKED_BY_MACOS** | 当前为 Windows，无 Xcode                          |

---

## CONFORMANCE

| Gate              | 状态     | 证据                                                                                                                                                          |
| ----------------- | -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| codegen gate      | PASS     | 3 个 generated 文件与 spec 一致                                                                                                                               |
| fixture integrity | PASS     | 91 用例 + 28 输入文件 sha256 全匹配                                                                                                                           |
| oracle selfcheck  | PASS     | 冻结 TS oracle 逐字节复现 91 用例                                                                                                                             |
| platform: android | PASS     | **91/91**                                                                                                                                                     |
| platform: harmony | NOT_RUN  | **0 执行**：无设备/模拟器，未接本地测试框架。逐分类：87 notImplemented / 4 blocked（depmap 3 + backup 1，因无 Argon2）。见 `HARMONY_N3_CONFORMANCE_REPORT.md` |
| platform: ios     | NOT_RUN  | 无报告                                                                                                                                                        |
| **VERDICT**       | **PASS** | `conformance/reports/SUMMARY.json`                                                                                                                            |

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

**2026-09-18 新增（Harmony 工程侧，非外部 blocker）**：

- **B27 hvigor 只编译可达模块**（已定位并规避，但必须长期记住）：
  `CompileArkTS` **只编译从 ability / page 可达的模块**；未被 `import` 的 `.ets`
  完全不进入编译图。实证：在未被引用的文件里放**语法错误**，`assembleHap`
  依然 `BUILD SUCCESSFUL`。因此"ArkTS 编译通过"只有在文件处于可达图中时才构成证据，
  每次声称都必须附 `modules.abc` 符号取证（`tools/harmony/probe-abc-symbols.mjs`）。
  规避方式：由 `pages/Index.ets` 引用 `crypto/ContainerSelfCheck.ets` 建立 import 边。

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

| 优先级 | 事项                                                                              | 前置                      |
| ------ | --------------------------------------------------------------------------------- | ------------------------- |
| P0     | 确立 Argon2 路径（NAPI + 外部参考实现，或 ohpm 三方包），或正式升级为长期 blocker | 安全评审                  |
| P0     | DevEco 下载模拟器系统镜像，打通 `HARMONY_RUNTIME_E2E`                             | **用户操作**（账号/网络） |
| P1     | 接 DevEco 本地测试框架，让 conformance 可脱离设备执行                             | —                         |
| P1     | 继续 Domain：Impact → PlanReadiness → Coverage → StateMachine → Timeline          | Android 冻结基准          |
| P2     | ArkData persistence + migration（I 节）                                           | —                         |
| P2     | HUKS + 用户认证（J 节）                                                           | —                         |
| P2     | FileWorkflowCoordinator（M 节，防 D-16 重演）                                     | application 层            |

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
