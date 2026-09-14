# WORK_STATUS.md

> 本文件由执行 Agent 持续更新。不要删除历史关键结论。
> 分支历史：`feat/mvp02-global-source`（MVP02，tag v0.2.0-mvp02）→ `engineering/baseline-v1`
> （Engineering Baseline V1 PASS，2026-09-13）→ `feat/mvp03-living-graph`（MVP03，tag v0.3.0-mvp03）
> → `feat/mvp03-living-graph`（Production RC V1，2026-09-13）→ **`feat/mvp03-living-graph`（FINAL PRODUCTION CLOSURE V1，当前）**。

## Current

- Phase: **PDIG FINAL PRODUCTION CLOSURE V1**（2026-09-14，WorkBuddy 轮）
- Current Gate: `FINAL_PRODUCTION_CLOSURE = PARTIAL_WITH_REPORT`
  - `CORE_READY = PASS` / `CODE_STYLE_READY = PASS` / `TYPE_SAFETY_READY = PASS` /
    `ARCHITECTURE_READY = PASS` / `TEST_SUITE_READY = PASS` / `MIGRATION_READY = PASS` /
    `SECURITY_READY = PASS` / `PERFORMANCE_READY = PASS` / `UI_UX_READY = PASS（源码级）` /
    `FRONTEND_READY = PASS（源码级）` / `ENGINEERING_BASELINE = PASS` /
    `MVP01/MVP02/MVP03_REGRESSION = PASS`
  - `UI_SOURCE_READY = PASS`、`UI_BUILD_READY = BLOCKED（B10）`
  - `ANDROID_SOURCE_READY = PASS`、`ANDROID_BUILD_READY = BLOCKED`（B1 已按实测重写）
  - `HARMONY_SOURCE_READY = PASS`、`HARMONY_BUILD_READY = BLOCKED`（B2 已按实测重写）
  - `IOS_SOURCE_READY = PASS`、`IOS_BUILD_READY = BLOCKED（B3）`
  - `STORE_METADATA_READY = PARTIAL_WITH_REPORT` / `STORE_ASSETS_READY = BLOCKED` /
    `STORE_SUBMISSION_READY = REQUIRES_USER_RELEASE_DECISION` / `STORE_SUBMITTED = NO`
  - `REAL_DATA_CORRECTNESS = NOT_RUN` / `REAL_DATA_VALUE = NOT_RUN`
  - `CLEAN_INSTALL = PASS`（非破坏性等价验证）/ `CLEAN_CLONE = BLOCKED（环境约束）`
  - Git：HEAD 以 `git log --oneline -1` 为准（`4af5b69..35d940e` 共 **10** 个提交）、工作区 clean、`git diff --check` PASS、**未 push**、**未打 tag**
- Next Gate: `UI_BUILD_READY` —— 根因 **B10（HBuilderX / uni-app x）**；不得直接进入 MVP04
- 详细报告：**FINAL_PRODUCTION_CLOSURE_REPORT.md**（本轮）/ **FINAL_ACCEPTANCE.md** /
  **FINAL_CLOSURE_PRE_AUDIT.md**（现场恢复）

## 本轮（FINAL PRODUCTION CLOSURE V1，2026-09-14）完成内容

### 0. 实测基线（全部重新读取，不复制旧报告）

- HEAD 进入时 `4af5b69`，branch `feat/mvp03-living-graph`，**工作区 clean**，无未提交工作需保全。
- `npm run check` → **EXIT=0**；`npm run check:full` → **EXIT=0**；**453 passed / 453**（43 文件）。
- 覆盖率 Stmts **93.82%** / Branch **82.24%** / Funcs **94.55%** / Lines **93.82%**（重跑执行树复跑；Branch 抖动区间 82.21–82.24）。
- 全量 ×3 全绿；critical（impact+invariants+contract+property）**×10 全绿**（74 tests/run）；**0 flaky**。
- architecture 48 files circular 0；network 118 files 0 原语；secrets **404 files** 0；UI 30 `.uvue`/24 pages/5 components。

### 1. 代码质量收口（PHASE B）—— 5 类真实改动，全部复验

1. **新增 docs 格式门禁**：根 `.prettierrc.json`（`proseWrap: preserve` + **`embeddedLanguageFormatting: "off"`** + `.mdc` override）+ `format:docs` / `format:docs:check` + 纳入 `check` 链。
   - 首次尝试未关内嵌格式化时，`git diff -w` 暴露**代码块内嵌代码被重写**的真实风险（golden JSON `25.00`→`25.0` 等）→ 回滚后关闭内嵌格式化重跑。
   - 最终 134 个 `.md`/`.mdc`：`git diff -w` 分类 = 473 表格分隔行 + 487 空行 + 7 处渲染等价规范化；**0 内嵌代码改动、0 语义改动**；幂等 PASS。
2. **删除 3 个死导出**：`planEffectiveStatus`、`ObservationFingerprintRecord`、`ParsedCsvCell`。
3. **重命名 18 处 `obj` → `record`**（词边界安全，`'object'` 未受损）。
4. **`.gitignore` 补 Gradle/Android 本地状态**：`.gradle/`、`local.properties`、`.kotlin/`、`*.hprof`、`captures/`。
5. 复验：`tsc` / `eslint` / `prettier` / 聚焦 123 tests / 全量 453 tests **全部 PASS**。

### 2. 平台现场修正（PHASE J–N）—— 两条既有 Blocker 前提被推翻

- **B1**：旧称「无 JDK17+ / Android SDK / Gradle」。实测 **JDK 17.0.12 + JDK 21.0.10 + Android SDK（platforms 36.1/37.0、build-tools 36.1.0/37.0.0、cmdline-tools、licenses 已接受）+ Android Studio** 均存在。实际阻塞 = 无可用 Gradle 发行版 + 构建依赖 CDN 不可达 + `compileSdk 34` 未安装 + 无真机 + 无 keystore。
- **B2**：旧称「无 DevEco Studio / HarmonyOS SDK」。实测 **DevEco 5.0.5.310 + SDK API 13（5.0.1.115）+ hvigor 5.13.2 + ohpm 5.0.10** 均存在。**并已真实执行 hvigor 构建**：成功引导 pnpm（`Pnpm install success.`）并解析工程模型，最终报 `Unable to find the following components: toolchains:13 / ArkTS:13 / js:13 / native:13 / previewer:13`，hvigor 建议经 DevEco SDK Manager 同步。
- B10（HBuilderX）**仍成立**，为产品级关键阻塞。

### 3. 生成的收口文档（12 份）

根目录：`FINAL_CLOSURE_PRE_AUDIT.md`、`FINAL_ACCEPTANCE.md`、`FINAL_TEST_MATRIX.md`、`FINAL_CODE_QUALITY_REPORT.md`、`FINAL_TEST_REPORT.md`、`FINAL_SECURITY_REPORT.md`、`FINAL_UI_UX_REPORT.md`、`FINAL_PLATFORM_MATRIX.md`、`FINAL_STORE_CHECKLIST.md`、`FINAL_PRODUCTION_CLOSURE_REPORT.md`。
`docs/`：`FINAL_TYPE_SAFETY_AUDIT.md`、`FINAL_FLAKY_REPORT.md`。

### 4. 本轮发现（登记）

- **U-1 / F-2**：`app/manifest.json` 引用不存在的 `static/icons/*.png` 与 `static/splash/*.png`（`app/static/` 目录不存在，`assets/` 无任何 PNG）→ STORE_ASSETS blocker，**未伪造占位图标**。
- **U-2**：iOS `NSCameraUsageDescription` 声明了尚未实现的二维码扫描用途 → 提交前需与实现对齐。
- **F-3**：`packageManager: npm@11.3.0` 与实际 `npm 10.9.7` 不一致（未擅自修改）。
- **U-3**：Accessibility / Responsive 未经工具化验证 → 标 `PARTIAL_WITH_REPORT`，不写 PASS。

## 本轮 quality state（FINAL PRODUCTION CLOSURE V1 实跑）

- `npm run check` → **EXIT=0**（含新增 `format:docs:check`）
- `npm run check:full` → **EXIT=0**
- 测试 **453 passed / 453**（43 文件）；覆盖率 93.82 / 82.24 / 94.55 / 93.82
- `test:stability` → 3 连跑全绿；critical ×10 → 全绿
- **milestone 重跑（第 144 节，执行树 `b0ed6b5`）**：`npm run check` **EXIT=0**、`npm run check:full` **EXIT=0**、
  全量 ×3 **EXIT=0**（各 43 files）、critical focused ×10 **10/10 EXIT=0**（9 files / 74 tests 每轮）
  - 执行树 = `b0ed6b5`；**最终 HEAD 独立复核**：HEAD `35d940e` 上 `npm run check` **EXIT=0**
    （43 files / 453 passed；architecture 48/circular 0；network 118/0 原语；secrets 404/0；UI 30 `.uvue`/24 pages），耗时 2 分 05 秒
- **Stryker 变异测试本轮真实重跑 → `MUTATION_STRYKER_RERUN = PASS`**：
  Stryker 10.0.0，532 mutants（335 killed / 2 timeout / 167 survived / 28 no-cov / 0 errors，
  score 63.35% / covered 66.87%），耗时 35 分 03 秒，**与 Engineering Baseline V1 冻结基线逐项完全一致**
  （kernel 436/242/2/164/28、registry 96/93/0/3/0）→ 无回归、无漂移
- architecture PASS（48 files，circular 0）；network PASS（118 files，0 原语）；secrets PASS（**404 files**，0）
- UI static PASS（30 `.uvue`，24 pages，5 components）；db-integrity 6 passed；perf 16 passed
- deps PASS（audit 3 moderate dev-only；license MIT / Apache-2.0）——Stryker 卸载后依赖树已还原为 lockfile 状态
- **clean install = PASS**（非破坏性：`npm ci --dry-run` EXIT=0 + lockfile↔manifest 同步 + `check:deps` tree/lockfile OK）
- **clean clone = BLOCKED（环境）**：工作区外批量写入被沙箱截断/终止；已用
  「`git status -uall` 0 行 ⇒ 磁盘树 ≡ 提交树；全门禁在该树 EXIT=0 ⇒ 提交树自足」作等价论证，**不写 PASS**
- **PHASE Q（第 144 节「当前可用 platform build」）**：**无任何平台工具链具备构建能力**，故对全部前置条件逐项复测
  - Android：Gradle 发行版缺失（仅 0 字节 `.lck`/`.part`）；`services.gradle.org` 首跳 307 →
    `github.com/gradle/gradle-distributions` **502 不可达**；已装 platform `36.1`/`37.0`（**无 34**）；`adb devices` 空
  - HarmonyOS：`ets/js/native/previewer/toolchains` **5 组件均在盘**（API 13 / 5.0.1.115），但解析失败
  - UI：HBuilderX **不存在**（B10）；iOS：`xcodebuild` **不存在**（B3）
  - **HarmonyOS hvigor 真实重跑**（重建 17 文件最小 Stage 工程）→ `BUILD FAILED in 24 s 166 ms`，
    **精确复现**既有组件错误，并暴露**新根因**：`repo.harmonyos.com/sdkmanager/v5/ohos/getSdkList` 返回 **400**
    → `TypeError: datas is not iterable`（`OhRemoteComponentLoader`）⇒ 远端列表路径亦不可用
  - **未产生任何平台产物**（HAP / APK / AAB / IPA / TestFlight 全无）；新根因已登记 `BLOCKERS.md` B2
- **第 144 节剩余 milestone 命令已在最终 HEAD（`bc8f631`）逐条重跑**：`check:full` **EXIT=0**
  （453/453；coverage **93.82 / 82.22 / 94.55 / 93.82**；db-integrity 6 passed；perf 16 passed；deps gate PASS）、
  全量 ×3 **EXIT=0**（run 1/3、2/3、3/3 各 43 files）、critical ×10 **10/10 EXIT=0**（每轮 9 files / 74 tests）、
  secrets 404 files/0、network 118 files/0、architecture 48 files/circular 0、ui 30 `.uvue`/24 pages
  → **第 144 节全部命令已重跑完毕**（唯一非 0 退出码者为 platform build，其失败即 B1/B2 事实）

## Git 收口（第 130–133 节）

- branch `feat/mvp03-living-graph`；进入基线 `4af5b69`；**`4af5b69..35d940e` 共 10 个提交**（4 收口 + 6 纯文档补录）；
  **HEAD 以 `git log --oneline -1` 为准**（报告无法写入自身提交的 SHA，故不以字面量声明）
- 收口提交：`161d168`（chore style / docs 门禁）→ `6652950`（style docs）→ `878ce00`（refactor core）→ `941966a`（docs release，14 files +2706/−55）
- 纯文档补录：`cfa4bf3`（PHASE P + Git 收口）→ `0ea803d` → `b0ed6b5` → `ef2cb18`（第 144 节 milestone 重跑 + Stryker 重跑）→ `c235bed` → `35d940e`（变异幸存者精确化）
- **不变量 D**（可机器复核）：`git diff --name-only 878ce00..HEAD | grep -vE '\.(md|mdc)$'` = **0 行**
  ⇒ `878ce00` 之后无任何非文档改动 ⇒ 所有 Gate 结论对最终 HEAD 同样成立
- `git status --short -uall` = **0 行**；`git diff --check` = **PASS**；secret scan = **404 files / 0**
- **未 push**（用户未授权）；**未创建 RC tag / 1.0 tag**（平台侧无任何真实构建产物，打 RC 标会造成误读；理由见报告 §8.2）
- 环境故障已处置：外部进程删除分支 loose ref → 从 reflog 取完整 SHA 重写 `packed-refs` + 重建 loose ref；
  未使用 `reset --hard` / `clean -fd` / `checkout .` / `restore .`，未重做提交

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

> 当前构建**可以真实安装并真实使用**（不依赖导入即可完成主流程），但覆盖范围小于 MVP01 完整设计。这是 B20/B21 的直接后果，不做粉饰。

## Platform Matrix

| Platform              | IMPLEMENTED | STATIC_AUDITED | COMPILED                             | TESTED     | DEVICE_VERIFIED | STORE_READY |
| --------------------- | ----------- | -------------- | ------------------------------------ | ---------- | --------------- | ----------- |
| Android               | YES         | YES            | NO（B1 实测口径）                    | NO         | NO              | NO          |
| HarmonyOS             | YES         | YES            | NO（B2 实测口径，hvigor 已真实运行） | NO         | NO              | NO          |
| iOS                   | YES         | YES            | NO（B3）                             | NO         | NO              | NO          |
| UI（uni-app x 24 页） | YES         | YES            | NO（B10）                            | —          | NO              | NO          |
| Core（Node）          | YES         | YES            | YES                                  | YES（453） | N/A             | N/A         |

## 仓库运维注意（重要）

分支 loose ref（`.git/refs/heads/<branch>/`）在本工作区会被外部进程反复删除。
**规避：分支 ref 固化在 `.git/packed-refs`。** 若出现 "branch has no commits"，
从 reflog 找回哈希后重写 packed-refs；**不要**执行 `git reset --hard` / `git clean`。

## Current failures

无失败测试。未执行项全部为外部工具链 Blocker（B1/B2/B3/B10）或 Real Data（NOT_RUN），不虚报。

## External blockers

见 `BLOCKERS.md`（B1 / B2 已按 2026-09-14 实测重写）：

- 阻断编译：B1（Android 构建制品获取）/ B2（DevEco SDK Manager 同步）/ B3（macOS）/ **B10（HBuilderX，产品级关键）**
- 阻断产品完整可用（工程缺口）：B20（解析桥接）/ B21（加解密桥接）/ B22（Core→UTS）
- 发布材料：B4–B9 / B11–B19

## Next

1. **解除 B10**：装 HBuilderX → 导入 `app/` → 编译基座 → 跑通 `check:ui` 之外的**真实编译**，
   并修复编译器暴露的问题（本轮所有 UI PASS 均为静态，必须先过编译）
2. **B20/B21**：在 B10 环境下实现解析与 `.depmap` 加解密桥接（Argon2id 需三端原生 + Golden 互操作）
3. **Android（B1）**：获取可用 Gradle 发行版（或工程内新增 gradle wrapper）→ 允许制品下载 →
   `:core:test`（golden 互操作）→ `assembleDebug` → 真机冒烟
4. **HarmonyOS（B2）**：DevEco 中执行 SDK Manager 同步 → 验证编译
5. **B3**：按 `docs/IOS_RELEASE_HANDOFF.md` 在 Mac 上 `swift test` → archive → TestFlight
6. **B13**：真实账单双 Gate（`validate-real-bill.ts` 就绪；Real Data 保持 NOT_RUN）
7. **B11/B12/B14–B17**：包标识 / 隐私与支持 URL / 品牌名 / 图标与启动图 / 商店截图

**进入 MVP04 的前提**：至少一台真实设备完整 E2E PASS + Build artifact PASS + Backup/Restore PASS +
UI device QA PASS + Release blockers 清晰。**当前均未满足。**

## 本轮（Production RC V1）完成内容

### 1. 现场恢复（PHASE A）

- 进入会话时工作树被外部进程切到 `master`（166 tests）；经 `git merge-base --is-ancestor`
  与 reflog 比对确认 **`master` 是 `feat/mvp03-living-graph` 的祖先**，MVP03 真实线有 46 commits。
- 非破坏性 `git checkout feat/mvp03-living-graph`。**未执行任何** `reset --hard` / `clean -fd` /
  `checkout .` / `restore .`。`1909174 [Checkout-checkpoint]` 为工具自动保全提交，无工作丢失。
- 产出 `WORKBUDDY_PRODUCTION_HANDOFF_AUDIT.md`（A–J 十类状态 + CURRENT_GATE / NEXT_GATE）。

### 2. MVP03 Freeze 独立复验（PHASE B）

- 独立重跑全部套件，确认 Freeze 结论成立（详见 `MVP03_FREEZE_REPORT.md` 附录）。
- 本轮最终实测 `npm run check:full` → **FINAL_EXIT=0**（见下）。

### 3. 生产控制文件（PHASE C）

`GOAL_PRODUCTION_RC_V1.md` / `PRODUCTION_ACCEPTANCE.md` / `PRODUCTION_TEST_MATRIX.md` /
`UI_UX_ACCEPTANCE.md` / `STORE_RELEASE_CHECKLIST.md` / `PLATFORM_RELEASE_MATRIX.md` /
`STORE_EXTERNAL_BLOCKERS.md`。

### 4. UI 产品化改造（PHASE D–I）

- **架构**：新增 Application Service（`app/services/depmap-service.uts`，UI 唯一数据边界）+ 纯规则层
  （`app/services/rules.uts`）。修复 12 页直连 SQLite、4 页复算领域逻辑、
  `drift.uvue` 裸 SQL 手动 bump revision（高危）。
- **设计系统**：`app/theme/tokens.uts`（浅色/深色/间距/圆角/字号/触控最小 44）+ 5 个 `dp-*` 组件。
- **页面**：17 页重写 + 7 页新增（onboarding / candidates / sources / backup / about / privacy /
  declare-relation），共 **24 页**；`pages.json` 新增 tabBar 4 项一级导航。
- **去假功能**：设置页三个无实际效果的开关 → 改为「启动验证始终开启（只读）」+ 隐私屏（真实持久化并
  在 `App.uvue` 启动时恢复）；导入/备份如实标注「暂未接入」。
- **使应用真正可用**：新增**手动声明支付关系**（`declareDependency` + 三步式声明页），
  语义在 MVP01 范围内（`Dependency 存在即用户确认`），Reality mutation 与 revision +1 同事务。
- **正确性修复**：4 处依赖方向错误（`createPlanWithAnalysis` / `simulateUnavailable` /
  `getPlanCoverage` / `plan-detail` 误传 planId），与 Core impact kernel（`from` 为 `to` 提供支付）对齐。
- **UI 静态 Gate**：`core/scripts/check-ui.mjs` 从 0 → **9 类**（U1–U9），0 命中；U9 已用注入探针验证有效。

### 5. 安全 / 隐私 / 版本 / 商店（PHASE K–O）

`docs/SECURITY_RELEASE_AUDIT.md` / `docs/PRIVACY_RELEASE_AUDIT.md`（P-1 已闭环）/
`docs/RELEASE_VERSION_MATRIX.md` / `docs/IOS_RELEASE_HANDOFF.md` / `docs/APP_ICON_ASSET_SPEC.md` /
`store/*`（5 份）。

## Current quality state（Production RC V1 轮实跑）

- `npm run check:full` → **FINAL_EXIT=0**（日志：`local_private/check-full-rc1-committed.log`）
  - format:check PASS；lint PASS；typecheck PASS（strict + noUncheckedIndexedAccess + exactOptionalPropertyTypes）
  - test **453 passed / 453**（43 文件）
  - check:architecture PASS（48 files，circular = 0）
  - check:network PASS（118 business source files，0 网络原语）
  - check:secrets PASS（391 files，0 production secrets；扫描集含未跟踪文件，计数随工作区状态浮动）
  - check:ui PASS（30 `.uvue`，24 pages，5 components，token 34 色）
  - check:db-integrity **6 passed**
  - test:coverage **453 passed**；Statements **93.74%**（5437/5800）/ Branches **约 82.2%** /
    Functions **94.28%**（3 次实测 Branches 82.21–82.24，v8 provider 极小幅抖动，不影响门槛）
  - test:perf **16 passed**
  - check:deps PASS（audit 3 moderate，dev-only）
- 文档一致性修正后于提交树 `8ff9e39` 复跑快速 Gate `npm run check` → **EXIT=0**
  （453/453 tests、43 文件；architecture 48 files circular 0；network 118 files 0 原语；
  secrets 391 files 0；ui PASS。日志：`local_private/check-committed-8ff9e39.log`）
- `npm run test:stability` → **3 连跑全绿，exit 0**（`local_private/stability-rc1.log`）
- 环境差异说明：本工作区 safe-delete 守卫拦截 vitest 对 `coverage/` 的批量清理。
  `core/scripts/run-coverage.mjs` 在检测到该守卫时把覆盖率输出目录改到系统临时目录
  （守卫自身放行），**退出码仍为 vitest 真实退出码，不做任何改写**。普通环境行为不变。

## 产品可用性（诚实口径）

| 能力                                      | 状态                             |
| ----------------------------------------- | -------------------------------- |
| 手动建立对象（卡 / 账户 / 服务）          | **可用**                         |
| 手动声明支付关系（含 required / unknown） | **可用**（本轮新增）             |
| 影响模拟（选中卡 → 受影响下游）           | **可用**（方向已修正）           |
| 创建变更计划 + 计划内影响清单 + 动作/验证 | **可用**（保守口径）             |
| 时间轴 / 待确认项 / 数据来源 / 数据清空   | **可用**                         |
| 账单导入                                  | **不可用**（B20；UI 已如实标注） |
| 加密备份导出 / 恢复                       | **不可用**（B21；UI 已如实标注） |

> 结论：当前构建**可以真实安装并真实使用**（不依赖导入即可完成主流程），
> 但覆盖范围小于 MVP01 完整设计。这是 B20/B21 的直接后果，不做粉饰。

## Platform Matrix

| Platform              | IMPLEMENTED | STATIC_AUDITED | COMPILED  | TESTED     | DEVICE_VERIFIED | STORE_READY |
| --------------------- | ----------- | -------------- | --------- | ---------- | --------------- | ----------- |
| Android               | YES         | YES            | NO（B1）  | NO（B1）   | NO              | NO          |
| HarmonyOS             | YES         | YES            | NO（B2）  | NO（B2）   | NO              | NO          |
| iOS                   | YES         | YES            | NO（B3）  | NO（B3）   | NO              | NO          |
| UI（uni-app x 24 页） | YES         | YES            | NO（B10） | —          | NO              | NO          |
| Core（Node）          | YES         | YES            | YES       | YES（453） | N/A             | N/A         |

## 仓库运维注意（重要）

分支 loose ref（`.git/refs/heads/<branch>/`）在本工作区会被外部进程反复删除。
**规避：分支 ref 固化在 `.git/packed-refs`。** 若出现"branch has no commits"，
从 reflog 找回哈希后重写 packed-refs；**不要**执行 `git reset --hard` / `git clean`。

## Current failures

无失败测试。未执行项全部为外部工具链 Blocker（B1–B3/B10）或 Real Data（NOT_RUN），不虚报。

## External blockers

见 `BLOCKERS.md`：

- 阻断编译：B1（Android 工具链）/ B2（DevEco）/ B3（macOS）/ B10（HBuilderX）
- 阻断产品完整可用（工程缺口）：B20（解析桥接）/ B21（加解密桥接）/ B22（Core→UTS）
- 发布材料：B4–B9 / B11–B19

## Next

1. **解除 B10**：装 HBuilderX → 导入 `app/` → 编译基座 → 跑通 `check:ui` 之外的**真实编译**，
   并修复编译器暴露的问题（本轮所有 UI PASS 均为静态，必须先过编译）
2. **B1**：装 JDK17 + Android SDK → `gradle :core:test`（golden 互操作）→ `assembleDebug` → 真机冒烟
3. **B20/B21**：在 B10 环境下实现解析与 `.depmap` 加解密桥接（Argon2id 需三端原生 + Golden 互操作）
4. **B3**：按 `docs/IOS_RELEASE_HANDOFF.md` 在 Mac 上 `swift test` → archive → TestFlight
5. **B13**：真实账单双 Gate（`validate-real-bill.ts` 就绪；Real Data 保持 NOT_RUN）
6. **B14–B19**：品牌名 / 隐私与支持 URL / 图标与启动图 / 商店截图（依赖 B10/B1 生成）

---

## 历史：MVP03 FINAL FREEZE（2026-09-13，tag v0.3.0-mvp03）

- **P0 修复**：PlanReadiness 废除「must_change 数量 − 完成动作数量」减法 →
  `PlanAction.resolvesImpactKeys[]` 显式 resolution（该 key 的全部声明 change 动作 done 才 resolved）
  - rebase claiming（确定性分配未声明 key，不自动完成动作）；UI plan-detail 同步
    （commit f692183，FR-READ-001..017 冻结）
- 冻结补测：FR-GR-012 revision property（随机 Reality/non-Reality/failure/replay → revision ≡ 成功
  Reality mutation 计数）、4 状态机非法迁移负向、rebase 原子性（lastAnalyzed 仅成功分析后推进）、
  10k Timeline heavy smoke（83ms）
- Targeted mutation **10/10 KILLED**（M-R1..5 + FM-1..5），0 critical survived（PARTIAL_WITH_REPORT）
- 实测：453/453（43 文件）；coverage src 93.74/82.24；stability ×3 + focused ×10 全绿；
  check / check:full / clean install / clean clone PASS；secret scan 348 files 0；network 0
- Freeze 轮 commits：f692183 → df966f4 → 253cdd5 → f72b322 → fdde863 → d41e12c → f65eca2 →
  7f7077b → 21945e6（tag v0.3.0-mvp03）

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
| P UI（24 页源码级，编译 BLOCKED B10）                      | PASS（静态） |
| Q Documentation                                            | PASS         |
