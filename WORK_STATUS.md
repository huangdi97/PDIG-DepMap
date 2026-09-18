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
>   —— 本轮只做运行时取证与构建链收口，**不新增功能、不进入 Harmony N3**。
>   结论：`N1 = PARTIAL_WITH_REPORT`、`N2 = PARTIAL_WITH_REPORT`、parity **58/62**、
>   `ANDROID_PRODUCTION_RELEASE_READY = BLOCKED_BY_PRODUCTION_SIGNING`。
>   完整 27 Gate 见 `ANDROID_N1_N2_FINAL_CLOSURE_REPORT.md`。

> **（当前）ANDROID 冻结 + 正式进入 Harmony N3，2026-09-17 第二场**
>   —— 人工 Final Acceptance 结论已落地：`N1 = PASS`、`N2 = PARTIAL_WITH_REPORT (62/73)`、
>   `ANDROID_PRODUCTION_RELEASE_READY = BLOCKED_BY_PRODUCTION_SIGNING`、D-16 CLOSED。
>   本轮**不再把 Android 62/73 往 73/73 堆**。
>   ① 新增 `ANDROID_NATIVE_CORE_HANDOFF = PASS`（**不替代** N2 / release readiness）
>      → `ANDROID_NATIVE_CORE_FREEZE.md`；Android 转入 `CORE_FROZEN / MAINTENANCE_ONLY`；
>      剩余 11 格按 ENGINEERING_NOT_YET_VERIFIED(7) / RUNTIME_ENVIRONMENT_BLOCKED(2) /
>      RELEASE_EXTERNAL_BLOCKED(1) / STORE_PREPARATION(1) 分类保留在 N2 Backlog。
>   ② **Git 尾项收口**：实查 HEAD `bc2eeb8` → 提交 `6053f3c`（三端 codegen 产物 + `legacy/README.md` 入库，
>      均为 `codegen --check` 验证的正式产物）；`.pi/` 保持 intentionally-untracked（gitignore 覆盖）。
>      ⚠ 本轮再次复现既有 Git 故障：`git commit` 成功建对象但 HEAD 不推进 —— 已核实
>      `6053f3c` 的 parent/tree 后用 `.git/packed-refs` + loose ref 修正并复核通过。
>      ⚠ `packed-refs` 必须写**完整 40 位 SHA**（曾误写短哈希导致 HEAD 无法解析，已修复）。
>   ③ **Android 取证口径修正（重要）**：`android/settings.gradle.kts` 把构建输出重定向到
>      `%USERPROFILE%/pdig-build/<module>`，**`android/**/build/**` 是自重定向后的过期残留**。
>      既往报告里 `d84d8900…` / `bf378ec6…` 等 APK 哈希来源不明，本轮起作废。
>      本轮实测：`:core:test` 71/71、`:app:testDebugUnitTest` 9/9、`:conformance:run` 91/91、
>      设备内 androidTest 51/51（emulator-5554），APK/AAB 哈希见冻结报告。
>   ④ **正式进入 Harmony N3**：`harmony/` 由「仅 1 个 codegen 文件」建成可真实构建的
>      Stage Model 工程 —— hvigor 全清重建 `BUILD SUCCESSFUL`，产出 HAP **60,133 B**；
>      首个纯 ArkTS Domain（`Relations.ets`）已编译并打包进 HAP。
>      `HARMONY_BUILD = PASS`；`HARMONY_DEPMAP = BLOCKED`（cryptoFramework 无 Argon2）；
>      `HARMONY_RUNTIME_E2E = RUNTIME_NOT_RUN`（无模拟器镜像，`hdc list targets = [Empty]`）。
>      按 stop condition **未进入 iOS N4**，等 Harmony Gate 复核。
>
> **（当前）PART A GitHub 发布收口 + PART B Harmony N3 Argon2 深挖，2026-09-18**
>   —— ① **GitHub 发布完成**：历史净化门禁全 PASS 后首次 push 到 `huangdi97/PDIG-DepMap`
>      （PRIVATE，5 分支 + 3 标签，**全程快进、无 force**）。
>      CI 三轮：首轮 FAIL（2 个真实仓库缺陷）→ 二轮 FAIL（Android job 转绿，Canonical 仅剩
>      `fixtureIntegrity`）→ **三轮全绿**（`cases 91/91`、`imports 28/28`、oracle PASS、
>      `platform android PASS 91/91`、`VERDICT: PASS`）。
>      产出 `GITHUB_SECRET_PRIVACY_AUDIT.md` / `GITHUB_HISTORY_SANITIZATION_REPORT.md` /
>      `GITHUB_PUBLICATION_REPORT.md`。
>      ② **定因并修复了既有 fixture 缺陷**：`fixtures/import` 12/28 的 sha256 与清单不一致，
>      根因**不是字节漂移**，而是清单记录了生成机 `core.autocrlf=true` **检出态的 CRLF 假象**
>      （blob 级取证：12 个文件 HEAD blob == 引入提交 `97a0348` blob、均无 CR；
>      对照 `csv-cr-only.csv` 净化后仍保留 CR → 净化不剥离 CR；12/12 命中
>      `sha256(CRLF(当前字节)) == 清单原值`）。按 canonical blob 字节**修正清单 12 条哈希**
>      （`oracle.commit` 未动），不改字节、不降门禁。
>      ③ **Harmony N3 Argon2 深挖**：`cryptoFramework` / `HUKS` 无 Argon2（证据级排除）；
>      `hash-wasm` 的 `argon2.c` 是 WASM 实现**不能**作原生源；**NDK 路径可行** ——
>      主机侧 PHC 参考实现**逐字节复现 Golden Vector**（`MATCH=YES`，version 19），
>      OHOS arm64 交叉编译产出 `libargon2_ohos.so`（ELF64/AArch64/仅依赖 libc.so）
>      与 NAPI 桥接 `libpdiargon2.so`（8 个 `napi_*` 由 Ark 运行时解析）。
>      `HARMONY_DEPMAP`：`BLOCKED` → **`BLOCKED_BY_NATIVE_VERIFICATION`**；
>      `HARMONY_RUNTIME_E2E` 仍为 `RUNTIME_NOT_RUN`（无设备/镜像）。
>      Android 保持 **CORE_FROZEN**，**未进入 iOS N4**。
>      完整内容见 `HARMONY_ARGON2_FEASIBILITY.md`。
>
> **（历史）ANDROID FINAL BLOCKER CLOSURE — D-16 关闭轮，2026-09-17**
>   —— 关闭 D-16（导入 / 恢复向导在"锁定—解锁"过程中被整体丢弃），采用**方案 A**：
>   把 Import / Restore 的外部文件工作流状态提升到 Activity 作用域
>   （`FileWorkflowCoordinator` + `LocalFileWorkflow`），并把 `ActivityResult` 注册
>   移到 `MainActivity`（不随 NavHost 的 uncompose 被注销）。
>   **明确不采用**方案 B（锁定时继续组合 NavHost 靠遮罩隐藏）与方案 C（拉起
>   DocumentsUI 时不锁定）——两者都会削弱"敏感内容结构性不可达"这一安全事实。
>   复验（全部实跑）：`:app` JVM **9/9** · 设备内 4 批 **51/51**（新增 `FileWorkflowD16Test` 6/6）·
>   `:core:test` **71/71** · `:conformance:run` **91/91** · E2E v4
>   **`core-journey-v4-20260917-184856` = 41/41 PASS / 0 FAIL** ·
>   assembleDebug / assembleRelease / bundleRelease 全部 SUCCESSFUL。
>   parity **62 / 73**（原 56）；`N1 = PASS`、`N2 = PARTIAL_WITH_REPORT`、
>   `ANDROID_PRODUCTION_RELEASE_READY = BLOCKED_BY_PRODUCTION_SIGNING`。
>   完整内容见 `ANDROID_N1_N2_FINAL_CLOSURE_REPORT_V2.md`（**就地更新，未生成 V3/V4**）§3.4。
>
> → **（历史）ANDROID FINAL BLOCKER CLOSURE，2026-09-16**
>   —— 关闭 App Lock 的真实接线缺口、备份导出 UI 误报、无障碍标签与滚动容器 hitbox；
>   重跑核心 E2E 与全量回归；**重新计算** N1 / N2。
>   结论见 `ANDROID_N1_N2_FINAL_CLOSURE_REPORT_V2.md`（**不沿用旧的 58/62 与旧 PASS 数量**）。
>
> **Production 依赖目标（明确冻结，不再动摇）**：
> `DCloud = 0 target` · `UTS = 0 target` · `uni-app / uni-app x = 0 target`。
> 这三者只作为 **LEGACY_REFERENCE / BEHAVIOR ORACLE** 留在仓库中，
> **不再出现在 Current 路线的任何 blocker、门禁或 Next 里**。

## Current

- Phase: **PDIG NATIVE MIGRATION — Android 已冻结（CORE_FROZEN），N3 Harmony 唯一活跃主线**
- Current gate focus: **`N3_HARMONY_FULL_PARITY`**（唯一活跃 gate）/
  **`ANDROID_NATIVE_CORE_HANDOFF` = PASS**（冻结，维护模式）/
  **`N2_ANDROID_FULL_PARITY` 保持 PARTIAL_WITH_REPORT 62/73**（冻结，不再推进）
- Global status: **`ALL_DONE = NO`** · **`TASK_COMPLETE = NO`**
- CURRENT_HEAD: **以 `git rev-parse HEAD` 为准**（报告不写入自身 SHA）
- CURRENT_BRANCH: `feat/mvp03-living-graph`
- NEXT_GATE: **`N3_HARMONY_FULL_PARITY`** —— **ACTIVE**（Android N1/N2 的
  Final Acceptance 不是本 gate 的前置条件；该前置叙述已过期，见下方 Historical）
- NEXT_COMMAND（下一位 Agent 的第一步）：

  ```bash
  git rev-parse HEAD && git status --short -uall
  # Harmony：先跑三道主机侧 gate（均不需要设备）
  PDIG_DEVECO_HOME="<DEVECO_HOME>" node tools/harmony/check-third-party-hashes.mjs
  PDIG_DEVECO_HOME="<DEVECO_HOME>" node tools/harmony/check-argon2-native-build.mjs
  PDIG_DEVECO_HOME="<DEVECO_HOME>" node tools/harmony/check-compiled-reachability.mjs --build
  # Android（冻结，仅回归）
  cd android && ./gradlew --no-daemon :core:test :conformance:run
  ```

### Current 只保留这 6 个关注面（其它内容一律属于 Historical / Legacy）

| #   | 关注面                       | 状态                                                                 |
| --- | ---------------------------- | -------------------------------------------------------------------- |
| 1   | Native Migration             | 进行中（**Android 已冻结 CORE_FROZEN；N3 Harmony 为唯一活跃主线**） |
| 2   | Android N1 / N2              | **N1 = PASS**，`N2 = PARTIAL_WITH_REPORT` 62/73 —— 见 `ANDROID_N1_N2_FINAL_CLOSURE_REPORT_V2.md`（**冻结，不再推进**） |
| 3   | Harmony N3                   | **ACTIVE**：`HARMONY_BUILD` = **PASS**（clean assembleHap，含 native）；`HARMONY_MODULE_COMPILED` = **PASS**（A/B/C/D 四判据，7/7 required 模块）；`HARMONY_CRYPTO` = **COMPILED**（JCS / AAD / AES-256-GCM）；`HARMONY_DEPMAP` = **NATIVE_BUILD_PASS / ON_DEVICE_NOT_RUN**（Argon2 NAPI 全链路已打通：vendored 溯源 + 交叉编译 arm64/x86_64 + 打包进 HAP 且符号表恰好只导出 NAPI 入口）；`HARMONY_DOMAIN`/`HARMONY_ARKUI` = PARTIAL_WITH_REPORT（Domain 尚未落地）；`HARMONY_RUNTIME_E2E` = **RUNTIME_NOT_RUN**（无模拟器镜像，见 `HARMONY_RUNTIME_ENVIRONMENT_AUDIT.md`）—— 见 `HARMONY_N3_IMPLEMENTATION_STATUS.md` / `HARMONY_ARGON2_INTEGRATION_REPORT.md` |
| 4   | iOS N4                       | `BLOCKED_BY_MACOS`（真实外部 blocker，不是工程缺口）                   |
| 5   | Cross-platform Conformance   | Android **91/91**（本轮实跑 + **CI 远真复验**双证）；Harmony **NOT_RUN**（0 执行：87 notImplemented / 4 blocked）；iOS 无报告。CI 已由恒 `NOT_RUN` 改为真正校验 Android 平台报告（见 `GITHUB_PUBLICATION_REPORT.md` §6.4） |
| 6   | Legacy Cutover               | **NOT_STARTED**（Cutover 条件未满足）                                  |

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

| 平台     | 命令（可直接复制）                                                                                                                                                                                                                            | 结果                                             |
| -------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------ |
| Core     | `cd core && npm run check`                                                                                                                                                                                                                    | EXIT=0（453/453）                                |
| UTS 三端 | `cd core && npm run check:uts`                                                                                                                                                                                                                | 15/15 PASS                                       |
| Android  | `cd platforms/android && JAVA_HOME="<ANDROID_STUDIO_HOME>/jbr" ANDROID_HOME="<ANDROID_SDK_ROOT>" "<GRADLE_HOME>/bin/gradle.bat" --no-daemon assembleDebug assembleRelease collectArtifacts --rerun-tasks --console=plain` | BUILD SUCCESSFUL in 7m 50s；AAR 逐字节复现       |
| Harmony  | 见 `docs/HARMONY_RELEASE_RUNBOOK.md`；本环境需用 Python 复刻 ASCII 镜像（`build.sh` 依赖 bash 工具）                                                                                                                                          | 见 `docs/HARMONY_BUILD_REPORT.md` + 本轮复现记录 |
| iOS      | 不可执行（非 macOS）                                                                                                                                                                                                                          | `IOS_TOOLCHAIN_READY = BLOCKED (B3)`             |

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

| 项                          | 命令                                                              | 结果                       |
| --------------------------- | ----------------------------------------------------------------- | -------------------------- |
| Legacy oracle 基线          | `cd core && npm test`                                              | **43 files / 453 tests PASS** |
| Codegen Gate                | `node tools/codegen/generate.mjs --check`                           | **PASS**（3 端 generated） |
| Oracle 自检                 | `core/scripts/generate-conformance.ts --verify`                    | **PASS（64 用例逐字节复现）** |
| Android 领域层编译          | `gradle :core:compileKotlin`                                        | **BUILD SUCCESSFUL**       |
| Android Conformance         | `gradle :conformance:run --args="<repo>"`                    | **pass=64 fail=0**         |
| 跨端 Conformance Gate       | `node tools/conformance/run.mjs`                                    | **VERDICT: PASS**          |

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

| 编号  | 缺陷                                                          | 处置                     |
| ----- | ------------------------------------------------------------- | ------------------------ |
| LC-001 | `csv-utf8-bom.csv` 实际不含 BOM（该测试路径从未真正覆盖）      | 记录；Native fixture 另用真实 BOM 文件 |
| LC-002 | `csv-missing-required-column.csv` 并未缺失必需列（命名误导）   | 记录                     |
| LC-003 | legacy UI 暴露 runtime registry 不承认的 `bound_to`（用户可选到会被拒绝） | **Canonical Spec 锁定 2 值** |
| LC-004 | `relationLabel` 含不存在的 `wallet_binding`                    | 记录                     |
| LC-005 | readiness 文案两处不一致                                        | 以 `copy-zh.json` 为准   |
| FIX-6  | golden fixture 的 wrongPasswordOutcome 误用正确口令             | **已修**（错误口令 + 增补篡改场景） |
| FIX-7  | JCS reject case 含 NaN（JSON 无法表达，必然假失败）              | **已修**（移除）          |
| FIX-8  | conformance harness 从 manifest 读 expected（manifest 不含）    | **已修**（改读 fixture 本体） |

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

| 项 | 发现 | 处置 |
| --- | --- | --- |
| Gradle Wrapper | `android/` **完全没有** `gradlew` / `gradlew.bat` / `gradle-wrapper.jar` / `gradle-wrapper.properties`；构建只靠绝对路径 `<GRADLE_HOME>/bin/gradle.bat` | 生成标准 Wrapper（Gradle 8.9，官方 `distributionUrl`），已提交 `68f506c` |
| `android/local.properties` | 含机器 SDK 路径 | 保持 gitignore（`.gitignore:86`），**未提交**（已用 `git ls-files --error-unmatch` 验证 exit=1） |
| `:conformance:run` 默认仓库根 | `rootProject.dir("../..")` 算错一级 → `<repo_parent>` → `FATAL: <repo_parent>/conformance\CONFORMANCE_MANIFEST.json not found` | 改为 `dir("..")` |
| JVM 代理 | JVM 不读 `HTTP_PROXY` 环境变量，Wrapper 自举下载报 `Connection refused` | 用 `GRADLE_OPTS="-Dhttp.proxyHost=… -Dhttps.proxyPort=…"`（端口 10808 可通） |

`gradlew clean` / `assembleDebug` / `assembleDebugAndroidTest` / `:conformance:run` 全部 **BUILD SUCCESSFUL**。

### 2. 运行时实测（本机 AVD，非声称）

| 项 | 命令 | 结果 |
| --- | --- | --- |
| Conformance | `./gradlew --no-daemon :conformance:run` | **pass=91 fail=0 notImplemented=0 total=91** |
| 设备内 androidTest | `./gradlew --no-daemon :app:connectedDebugAndroidTest` | **19 / 19 PASS**（0 skipped / 0 failed） |
| `:core` 纯 JVM 单测 | `./gradlew :core:test` | **NO-SOURCE（0 个）** —— 记为真实欠账，不粉饰 |
| 真机 E2E | `local_private/e2e_drive.py`（串行单次干净运行） | 13 类步骤 PASS；业务写入链路等 NOT_RUN |
| 性能 smoke | `PerfSmokeEvidenceTest` | `csvRowsParsed=10000`、`csvParseErrors=0`，强断言通过 |
| logcat 隐私扫描 | PID/UID 归属扫描 | `appLines=44`，6 类敏感关键字命中 **全 0** |

产物：`app-debug.apk` 36,794,370 B（SHA-256 `d84d8900…30879`）；
`app-release.aab` 20,734,935 B（SHA-256 `f10cc60d…0f76c`，**未签名**）。

### 3. 本轮修掉的真实缺陷（8 项）

| # | 类别 | 缺陷 | 修复 |
| --- | --- | --- | --- |
| 1 | 构建链 | `android/` 完全没有 Gradle Wrapper | 生成标准 Wrapper（Gradle 8.9） |
| 2 | 构建链 | conformance 默认仓库根算错一级 | `../..` → `..` |
| 3 | 运行时 | SQLCipher native 库未加载 → `UnsatisfiedLinkError` | `System.loadLibrary("sqlcipher")` |
| 4 | 运行时 | Cursor 惰性视图越界 → `CursorIndexOutOfBoundsException` | 改用 `MaterializedRow` |
| 5 | 运行时 | 查询不存在的列 `criticality` → `SQLiteException` | 从 `acceptProposal` 的 SELECT 中移除 |
| 6 | **取证方法** | 性能 smoke 数据**无效**（`csvRowsParsed=0`） | 修正 `dateFormats` + 强断言 `assertEquals(10_000, rows)` |
| 7 | **取证方法** | logcat 扫描把 Launcher3 的 `password:false` 系统字段误判为应用泄露 | 改 PID/UID 归属扫描 |
| 8 | 运行时 | 单进程跑 19 个 androidTest 被 OOM kill（signal 9） | 按类分批 + 类间 `pm clear` / `logcat -c` |

> 第 6、7 项尤其值得记住：**上一轮报出的性能数字和"日志泄露"结论都是假的**，
> 一个因为数据根本没解析进去，一个因为扫了别人的日志。旧数字已作废。

### 4. 三个判定（分别回答，不混为一谈）

| 判定 | 结果 | 一句话理由 |
| --- | --- | --- |
| `N1_ANDROID_VERTICAL_SLICE` | **PARTIAL_WITH_REPORT** | 分层证据很硬（91 + 19 全绿），但核心链路 import→proposal→reality→impact→changeplan→verification **设备级一次都没跑通** |
| `N2_ANDROID_FULL_PARITY` | **PARTIAL_WITH_REPORT** | **58 / 62**；截图保护 / App Lock / Biometric 属于"只有实现没有运行时证据" |
| `ANDROID_PRODUCTION_RELEASE_READY` | **BLOCKED_BY_PRODUCTION_SIGNING** | 缺生产 keystore；且非生产签名流水线本轮**未生效**（产物与未签名版同 SHA-256） |

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

| 项 | 结果 |
|---|---|
| `:core:test` | 71/71 PASS |
| `:app:testDebugUnitTest` | **NO-SOURCE**（app 模块无 JVM 单测，如实记录） |
| `:conformance:run` | **pass=91 fail=0 total=91** |
| `:app:assembleDebug` | BUILD SUCCESSFUL（APK 见上） |
| `:app:connectedDebugAndroidTest` | **19/19 PASS**（0 skipped） |

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

| 项 | 结果 |
| --- | --- |
| `:core:test` | 71 / 71 |
| `:conformance:run` | 91 / 91 |
| `:app:testDebugUnitTest`（新增） | 9 / 9（`FileWorkflowStateTest`） |
| `:app:connectedDebugAndroidTest`（4 批） | 51 / 51（含新增 `FileWorkflowD16Test` 6/6） |
| E2E v4（`core-journey-v4-20260917-184856`） | **41 / 41 PASS / 0 FAIL** |
| assembleDebug / assembleRelease / bundleRelease | 全部 BUILD SUCCESSFUL |

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

| 项 | 判定 | 理由 |
| --- | --- | --- |
| `android/app/src/main/.../workflow/*.kt`（3 个） | **入库** | D-16 产品源码 |
| `android/app/src/androidTest/.../FileWorkflowD16Test.kt` | **入库** | D-16 设备取证 |
| `android/app/src/test/.../FileWorkflowStateTest.kt` | **入库** | D-16 JVM 单测 |
| `harmony/entry/src/main/ets/generated/CanonicalEnums.ets` | **不入库** | N3 未开始（stop condition 明确不进入）；本机无 Harmony 工具链可验证 |
| `ios/Sources/PDIGCore/Generated/CanonicalEnums.swift` | **不入库** | N4 `BLOCKED_BY_MACOS`，本机无法编译验证，入库等于声称已验证 |
| `legacy/README.md` | **不入库** | LEGACY_REFERENCE，与本轮范围无关；历史多轮均保持未跟踪 |

### 停止条件

D-16 关闭 + 全回归 + parity 重算 + Git 收口均已完成，**到此停止**，
等待人工 Final Acceptance；**不进入 Harmony N3 / iOS N4 / MVP04**。

---

## 本轮：PART A GitHub 发布收口 + PART B Harmony Argon2 深挖（2026-09-18）

### PART A：发布与 CI

| 项 | 结果 |
| --- | --- |
| 历史净化门禁 | **全 PASS**（`SECRET/PRIVATE_KEY/TOKEN/RAW_FINANCIAL/PERSONAL_PATH_IN_HISTORY` 均为 NO；1,259 个可达 blob 复扫残留 **0**；`REACHABLE_OLD_SHA_COUNT=0`） |
| 首次 push | **完成**，PRIVATE，5 分支 + 3 标签，**无 force** |
| 二次 / 三次 push | 均快进：`443bd7e9..d9e5319`、`d9e5319..0b38bd40`、`0b38bd4..42b59a1` |
| `GITHUB_CI` | **PASS**（第三次运行 `35303432883`：两个 job 全绿） |

**首次 CI 暴露的两个真实仓库缺陷（均已修复并远真复验）**

| 缺陷 | 根因 | 处置 |
| --- | --- | --- |
| `fixtures/coverage/` 6 个用例被忽略 | `.gitignore` 的 `coverage/` 匹配**任意层级**同名目录 | 增加 `!fixtures/coverage/`；6 个用例入库 |
| `csv-crlf.csv` 的 CRLF 被归一化 | `.gitattributes` 的 `* text=auto eol=lf` 动了被测语义本身 | `fixtures/import/*`、`fixtures/coverage/*` 加 `-text` |
| Android job `Setup Android SDK` 失败 | `:core`/`:conformance` 是**纯 JVM 模块**，本不需要 SDK | 改为 JDK 21 + `--configure-on-demand` |

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

| 项 | 结果 |
| --- | --- |
| Harmony AES-256-GCM + AAD 能力 | **具备**（`GcmParamsSpec{iv,aad,authTag}`，tag 由 `doFinalSync` 取、解密时经 `initSync` 传入） |
| 主机侧黄金校验 | **5/5 PASS**（`tools/harmony/verify-container-golden.mjs`，仅用 `node:crypto`） |
| ArkTS 实现 | `Jcs.ets` / `DepmapContainerV1.ets` / `ContainerSelfCheck.ets` |
| ArkTS 编译 | **COMPILED**（真实编译，非假信号，见下） |
| 运行时 | **NOT_RUN**（无设备） |
| `.depmap` 协议 | **未改动** |

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

| 缺陷 | 处置 |
| --- | --- |
| `build-ascii-mirror.mjs --clean` 只在注释里存在，代码从未实现 → 想做干净构建的人拿到的是增量假绿 | 补上真实实现（构建前 `rmSync` 镜像目录） |
| 符号取证脚本只存在于 `.workbuddy/`（不受版本控制） | 固化为 `tools/harmony/probe-abc-symbols.mjs` |

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
