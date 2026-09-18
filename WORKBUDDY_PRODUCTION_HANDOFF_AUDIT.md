# WORKBUDDY_PRODUCTION_HANDOFF_AUDIT.md

> PDIG / DepMap — ZCode → WorkBuddy 生产化接力现场审计
> 生成时间：2026-09-13（WorkBuddy Production RC V1 轮，PHASE A）
> 原则：**先恢复现场，再继续开发**；本文件只记录本次实际检查到的证据，不引用未验证的历史结论。

---

## 0. 现场恢复结论（最重要）

### 0.1 分支现场与真实线判定

| 项                                | 实测值                                                                                            |
| --------------------------------- | ------------------------------------------------------------------------------------------------- |
| 进入本会话时的工作树分支          | **`master`（错误线）**                                                                            |
| 进入本会话时 `HEAD`               | `ac511f6 docs: sync test counts (166) across FINAL_REPORT/README`                                 |
| reflog 显示                       | `HEAD@{0}: checkout: moving from feat/mvp03-living-graph to master`（21:45:53，会话开始前 13 秒） |
| `master` 与 MVP03 关系            | `git merge-base --is-ancestor master feat/mvp03-living-graph` → **YES**（master 是 mvp03 的祖先） |
| `master..feat/mvp03-living-graph` | **46 commits**（master 无任何独有提交）                                                           |
| MVP03 真实线                      | **`feat/mvp03-living-graph`** @ `f50b409`                                                         |
| 处置                              | 已执行 `git checkout feat/mvp03-living-graph`（工作区干净，无覆盖风险）                           |

**结论：`master` 上不存在 MVP03 代码**（无 `core/src/scenarios`、无 `core/src/sources`、无 `GOAL_MVP03_*` 等文件）。
本任务必须在 `feat/mvp03-living-graph` 上继续。**已恢复，无需用户介入。**

### 0.2 `[Checkout-checkpoint]` 提交性质

`f50b409 [Checkout-checkpoint] from feat/mvp03-living-graph to master (21:45:53)` 是**工具在切分支前对 mvp03 未提交改动的自动保全提交**，位于 mvp03 分支顶端。
**判定：无 ZCode 工作丢失。** 该提交内容为纯文档/状态快照，未修改任何 Core 源码（见 §0.3）。

### 0.3 工作区干净度

```
$ git status
On branch feat/mvp03-living-graph
nothing to commit, working tree clean
```

- **modified：无**
- **staged：无**
- **untracked：无**（`core/reports/` 在 mvp03 分支已被 `.gitignore` 覆盖）
- **未提交修改：无**（F 类为空）

### 0.4 未执行的危险操作（显式声明）

本会话**未**执行且不会执行：`git reset --hard` / `git clean -fd` / `git checkout .` / `git restore .` / 任何历史改写 / force push。
唯一的分支操作是 `git checkout feat/mvp03-living-graph`（非破坏性）。

### 0.5 仓库运维风险（继承自 WORK_STATUS.md，已复现）

分支 loose ref（`.git/refs/heads/<branch>/`）在本工作区会被外部进程反复删除，分支 ref 固化在 `.git/packed-refs`。
**若出现 "branch has no commits"，从 reflog 找回哈希后重写 packed-refs；不要 reset/clean。**

---

## A. ZCode 已完成且已有测试证据（Verified）

本次**实际复跑**（非引用报告）：

| 证据     | 命令                   | 实测结果                                          |
| -------- | ---------------------- | ------------------------------------------------- |
| 全量测试 | `npm test`（core）     | **453 passed / 453（43 files）**，Duration 53.05s |
| 格式     | `npm run format:check` | All matched files use Prettier code style!        |
| Lint     | `npm run lint`         | 0 errors / 0 warnings                             |
| 类型     | `npm run typecheck`    | `tsc --noEmit` 无输出（PASS）                     |

MVP03 九模块 + 迁移（代码与测试均实际存在，已抽查源码）：

| 模块                         | 源码位置（实测存在）                                            | 证据                                                                  |
| ---------------------------- | --------------------------------------------------------------- | --------------------------------------------------------------------- |
| GraphRevision                | `core/src/repositories/graph-revision.ts`                       | `bumpGraphRevision` 仅由 Reality mutation 调用；`meta.graph_revision` |
| PlanReadiness（P0 修复）     | `core/src/services/plan-readiness.ts`、`change-plan-service.ts` | **数量相减已废除**，改为 `resolvesImpactKeys[]` 显式映射（§7）        |
| Rebase                       | `core/src/services/plan-analysis.ts`                            | `claiming` 确定性分配，不自动完成动作                                 |
| RealityDrift                 | `core/src/services/reality-drift-service.ts`                    | 源码存在                                                              |
| DiscoveryCandidate           | `core/src/services/discovery-service.ts`                        | 源码存在                                                              |
| ScenarioTemplate             | `core/src/scenarios/registry.ts`                                | 3 active + planned gate                                               |
| Timeline                     | `core/src/services/timeline.ts`                                 | 纯投影                                                                |
| Verification                 | `change-plan-service.ts` Action 状态机                          | done ≠ verified                                                       |
| Migration v2→v3 / payload v3 | `core/src/schema/migrations.ts`、`services/graph-serialize.ts`  | 源码存在                                                              |

测试文件：**43 个**（`find core/tests -name "*.test.ts"`），含 `plan-readiness-freeze`、`graph-revision-freeze`（property）、`state-machine-freeze`、`migration-v3`、`timeline-10k-freeze` 等冻结套件。

**A 类判定：MVP03 Core（Node 22）代码侧 = VERIFIED PASS。**

---

## B. 代码已实现但未验证（Implemented, Not Verified）

| 资产                         | 规模（实测）                                        | 未验证原因                                       |
| ---------------------------- | --------------------------------------------------- | ------------------------------------------------ |
| UI 页面（uni-app x `.uvue`） | **17 页 / 1999 行**                                 | 无 HBuilderX / uni-app x 工具链（B10）→ 从未编译 |
| UTS 平台插件                 | 5 插件 × 3 平台（`app/uni_modules/`）               | 同上（B10）+ 无 JDK17/DevEco/Xcode               |
| Android Kotlin 安全层        | `platforms/android/kotlin/**`（5 类 + golden test） | 无 JDK17 / Android SDK / Gradle（B1）            |
| HarmonyOS ArkTS              | `platforms/harmonyos/**`                            | 无 DevEco Studio / HarmonyOS SDK（B2）           |
| iOS Swift                    | `platforms/ios/**`（SPM + XCTest）                  | 非 macOS（B3）                                   |

**B 类判定：全部为 SOURCE_READY / STATIC_AUDITED，COMPILED / TESTED = BLOCKED。**

---

## C. 部分完成（Partial）

| 项          | 现状                                                                   | 缺口                                                                        |
| ----------- | ---------------------------------------------------------------------- | --------------------------------------------------------------------------- |
| UI 信息架构 | 17 页平铺，`pages.json` **无 `tabBar`**                                | 无一级导航冻结；无 onboarding；无来源管理/备份恢复/候选/关于页              |
| UI 架构分层 | **页面直接执行 SQL**（实测 12 个页面含 `getSecureDb().query/execute`） | 违反 §61「UI 不得直接操作 SQLite」；**无 Application Service 层**           |
| UI 领域逻辑 | home/timeline/drift 页内以 SQL + 客户端解析复算派生状态                | 违反 §62「UI 不得重复 Domain Logic」                                        |
| 设计系统    | 无 token、无组件目录；样式为每页内联 `<style>`                         | 无 `primary/surface/...` 变量；无 AppHeader/Card/StatusChip 等              |
| 页面状态    | 仅少量 `v-if` 空态                                                     | 缺统一 Empty/Error/Loading/Disabled 组件；多数流程无 error UI               |
| 文档        | `docs/` **78 份**                                                      | 缺 DESIGN_SYSTEM / PRIVACY_POLICY / USER_NOTICE / RELEASE_VERSION_MATRIX 等 |

---

## D. 尚未开始（Not Started）

- 生产控制文件：`GOAL_PRODUCTION_RC_V1.md` / `PRODUCTION_ACCEPTANCE.md` / `PRODUCTION_TEST_MATRIX.md` / `UI_UX_ACCEPTANCE.md` / `STORE_RELEASE_CHECKLIST.md` / `PLATFORM_RELEASE_MATRIX.md`
- Onboarding（首次启动）
- 备份 / 恢复（`.depmap` 导出导入）UI
- 来源管理 UI（Source Management）
- DiscoveryCandidate 复核 UI
- 关于 / 隐私 / 版本页
- Design System（token + 组件库）
- 商店元数据：`store/STORE_LISTING_ZH.md` / `PRIVACY_DISCLOSURE_MATRIX.md` / `SCREENSHOT_PLAN.md` / `RELEASE_NOTES.md` / `PLATFORM_REQUIREMENTS.md`
- 图标 / 启动图资产规格
- 隐私政策 / 用户须知草案
- UI 静态 Gate（无编译器条件下的机械校验）
- Android / Harmony 实际构建（工具链缺失）

---

## E. 当前失败 / regression（Failures）

**无。** `npm test` 453/453 全绿，`format:check` / `lint` / `typecheck` 全绿，0 flaky（继承 3×全量 + 10×focused 证据）。

---

## F. 未提交修改（Uncommitted）

**无。** 工作区 clean。唯一相关提交为 `f50b409`（工具自动 checkpoint，已在 mvp03 分支，内容为文档快照）。

---

## G. MVP03 Freeze 尚未完成 Gate

**无未完成 Gate。** 本次实际复跑确认：

- `MVP03_FREEZE_ACCEPTANCE.md` 80 项全勾选，逐条与代码/测试对应可查
- `MVP03_FREEZE_REPORT.md` 判定 `MVP03_FINAL_FREEZE = PASS`
- P0（PlanReadiness 数量相减）**已修复且已冻结**（`resolvesImpactKeys`，源码实测确认）

> 说明：`check:full`（coverage + perf + deps gate）在本轮 PHASE B 重新实跑，证据见 `MVP03_FREEZE_REPORT.md` 追加节与 `PRODUCTION_RC_V1_REPORT.md`。

---

## H. 产品 / UI 当前缺口（决定 PRODUCT_READY / UI_READY）

1. **一级导航未冻结**：无 `tabBar`，所有页面 `navigateTo` 平铺，无"首页/场景/基础设施/我的"层级。
2. **无 Onboarding**：首次启动直接进入 `unlock`，用户不知道产品是做什么的。
3. **首页 answer-oriented 已具雏形但信息密度不足**：有"需要你处理 / 即将到来 / 常用场景"，但缺"最近变化""最近完成"，且**直接 SQL 取数**。
4. **ChangePlan 页 CTA 未按状态分化**（§32）。
5. **Coverage UI 未做四级可解释展示**（§33）。
6. **Drift / Candidate 文案与选项**部分符合（drift 页标题已为"可能发生了变化"），Candidate 无独立复核页。
7. **导入流程无隐私说明步骤**（§37）；无 CSV 字段映射 UI（§38）。
8. **无备份 / 恢复 UI**（§69/§70）。
9. **无 Empty/Error/Loading 统一组件**；错误仅 `console.error` 级（违反 §41）。
10. **中文工程词**：页面已较多中文化，但 `nodes`/`impact-result` 等仍暴露内部概念。
11. **无设计 token / 组件库 / 图标语言 / 深色模式基线**。

---

## I. 平台 / 工具链缺口（实测）

| 检测                          | 实测结果                               | 对应 Blocker            |
| ----------------------------- | -------------------------------------- | ----------------------- |
| `node -v`                     | v22.22.2                               | —                       |
| `npm -v`                      | 10.9.7（`packageManager` 声明 11.3.0） | 低风险（`npm ci` 可用） |
| `java -version`               | **1.8.0_441**（需 17+）                | **B1**                  |
| `adb version`                 | 1.0.32（存在但过旧）                   | B1                      |
| Android SDK / Gradle wrapper  | 无                                     | B1                      |
| DevEco Studio / HarmonyOS SDK | 无                                     | B2                      |
| macOS / Xcode                 | 无（win32）                            | B3                      |
| HBuilderX / uni-app x         | 无                                     | B10                     |

**判定：Android / HarmonyOS 实际构建 = BLOCKED；iOS 编译 = BLOCKED（Windows）。**

---

## J. 推荐恢复点（Resume Plan）

| 优先级 | 恢复动作                                                                                               | 依据         |
| ------ | ------------------------------------------------------------------------------------------------------ | ------------ |
| P0     | MVP03 correctness / freeze —— **已完成，仅需记录 check:full 证据**                                     | §7/§160      |
| P1     | 产品 IA + 用户流程（tabBar 冻结、onboarding、首页、ChangePlan CTA、Coverage、Drift/Candidate 文案）    | §22–§37      |
| P2     | Design System + 组件库 + 页面状态（Empty/Error/Loading/Disabled）+ 前端架构分层（Application Service） | §44–§62      |
| P3     | 备份/恢复/安全/迁移 UX + 隐私/用户须知 + 版本矩阵                                                      | §69–§72/§107 |
| P4     | Android / Harmony 实际构建（**工具链缺失 → BLOCKED**）；iOS source-ready + Mac handoff                 | §94–§102     |
| P5     | 商店元数据 / 权限 / 截图计划 / 外部 Blocker 收敛                                                       | §127–§135    |

---

## CURRENT_GATE / NEXT_GATE

```
CURRENT_GATE = MVP03_FINAL_FREEZE = PASS
NEXT_GATE    = PRODUCTION_RC_V1
```

**未达 PRODUCTION_RC_V1 的原因（当前）**：产品/UI 层尚未产品化、无设计系统、无备份恢复 UX、无商店元数据；平台构建受 B1/B2/B3/B10 阻断。
**这些均可在本机（Windows + Node 22）继续推进至"源码就绪 + 可验证"，平台构建部分收敛为明确外部 Blocker。**

---

## 附：本文件事实来源

- `git status` / `git log --all --oneline` / `git reflog -40` / `git branch -vv` / `git merge-base --is-ancestor`
- `npm test`（453/453）/ `npm run format:check` / `npm run lint` / `npm run typecheck`（core）
- `find core/tests -name "*.test.ts"`（43）/ `find app/pages -name "*.uvue"`（17）/ `ls docs/`（78）
- 源码抽读：`plan-readiness.ts`、`graph-revision.ts`、`change-plan-service.ts`、`plan-analysis.ts`、`home.uvue`、`app-state.uts`
- `java -version` / `adb version` / `node -v` / `npm -v`
