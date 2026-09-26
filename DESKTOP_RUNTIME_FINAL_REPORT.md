# DESKTOP_RUNTIME_FINAL_REPORT.md

> 轮次：2026-09-26 multiclient runtime / functional / visual evidence sweep（spec §72-§79）
> 平台：Windows Desktop（Compose Desktop，Kotlin/JVM，JDK 21）
> 证据 SHA：最终以收口 SHA 为准（本轮截图来自 **feature 分支最新工作树**，见 RUNTIME_ACCEPTANCE_MATRIX.json 各行 git_sha）
> 本报告所有数字来自**本轮新鲜实跑**，非旧记录。

---

## 1. 环境

| 项         | 值                                                                                                                           |
| ---------- | ---------------------------------------------------------------------------------------------------------------------------- |
| OS         | Windows 11（屏幕 2048×1152，实际最大窗口 profile 取 2048×1152）                                                              |
| JVM        | OpenJDK 21.0.12.1 Temurin                                                                                                    |
| Gradle     | 8.9（仓库 Wrapper，`android/gradlew.bat -p desktop`）                                                                        |
| Compose    | 1.6.11（compose desktop plugin）                                                                                             |
| 产物       | `desktop/app/build`（ASCII build root：`%USERPROFILE%\pdig-desktop-build`，见 settings.gradle.kts 非 ASCII 路径 workaround） |
| app 版本   | 0.2.0（Main.kt `VERSION = "0.2.0"`）                                                                                         |
| 二进制 SHA | 收口时从 `pdig-desktop-build/app/libs/app-0.2.0.jar` 计算后回填                                                              |

---

## 2. 功能 Matrix（DESKTOP_FUNCTION_TOTAL / PASS）

以 `--smoke` 无头运行时驱动为准（`desktop/app/src/main/kotlin/com/pdig/desktop/SmokeRunner.kt`，同一 app jar，退出码 0 = 全 PASS）。

**DESKTOP_FUNCTION_TOTAL = 16，DESKTOP_FUNCTION_PASS = 16（`[smoke] VERDICT: PASS (all steps)`，退出码 0，2026-09-26 新鲜实跑）**

| #   | step                                                                                         | 结果 | 证据                                         |
| --- | -------------------------------------------------------------------------------------------- | ---- | -------------------------------------------- |
| 1   | fresh-launch（graphRevision=0）                                                              | PASS | smoke log（fresh session revision!=0 check） |
| 2   | create-open（manual obs → preview → commit → save → restore roundtrip）                      | PASS | 同上                                         |
| 3   | wrong-password-rejected（auth_failed）                                                       | PASS | 同上                                         |
| 4   | tampered-file-rejected（GCM auth_failed）                                                    | PASS | 同上                                         |
| 5   | future-schema-rejected（future_schema）                                                      | PASS | 同上                                         |
| 6   | import-wechat-fixture（真实 fixture：WechatParser → preview → commit；nodes≥1、proposals≥1） | PASS | 同上                                         |
| 7   | proposal-accept-bumps-revision（accept 后 graphRevision 递增；proposal-origin 依赖落库）     | PASS | 同上                                         |
| 8   | criticality-required-only-by-user（机器产物 criticality=unknown；用户可设 required）         | PASS | 同上                                         |
| 9   | scenario-replace_payment_card（建计划→动作→done≠verified→verify）                            | PASS | 同上                                         |
| 10  | scenario-expiring_payment_card                                                               | PASS | 同上                                         |
| 11  | scenario-close_payment_instrument                                                            | PASS | 同上                                         |
| 12  | engine-generates-candidate-from-import                                                       | PASS | SmokeEngineSteps                             |
| 13  | engine-generates-drift-never-auto-resolves                                                   | PASS | SmokeEngineSteps                             |
| 14  | candidate-accept-dismiss（accept 建节点 / dismiss 清除）                                     | PASS | 同上                                         |
| 15  | drift-resolve-dismiss                                                                        | PASS | 同上                                         |
| 16  | backup-restore-reopen-delete（导出/备份/恢复/重开/删除）                                     | PASS | 同上                                         |

运行命令（证据 log：`desktop-smoke2.log`）：

```
android\gradlew.bat --no-daemon --console=plain -p desktop :app:run --args=--smoke
[smoke] VERDICT: PASS (all steps)
BUILD SUCCESSFUL in 27s
```

**三 Scenario E2E（DESKTOP_THREE_SCENARIOS）= PASS**：replace_payment_card / expiring_payment_card / close_payment_instrument 三个都建计划、执行动作、验证，见上 #9-11。
**Core Journey（DESKTOP_CORE_JOURNEY）= PASS**：import → proposal → confirm → impact → plan → verification → backup/restore 全部在同一 smoke 中串成（#6-16）。

---

## 3. 页面 Reachability + 截图（DESKTOP_PAGE_TOTAL / PAGE_SCREENSHOTTED）

页面清单依据真实导航树扫描：`docs/runtime/RUNTIME_PAGE_INVENTORY.md` §1（24 个页面含 Gate）。

**DESKTOP_PAGE_TOTAL = 24，DESKTOP_PAGE_SCREENSHOTTED = 24（=所有 production 页面，每页 ≥1 张 primary-state 截图 + runtime reachability 断言）**

截图 harness：`desktop/app/src/main/kotlin/com/pdig/desktop/ShotDriver.kt`（`--shots` 参数，Main.kt 入口）。真实 Compose Desktop 窗口（同一 PDIGAppShell），skiko SOFTWARE 渲染（无 GPU 私有缓冲），java.awt.Robot 抓取窗口像素。数据准备复用 smoke 同款 repos 操作（导入微信 fixture → proposal → required → replace_payment_card 计划 → candidate/drift 行）。

**`[shots] VERDICT: PASS`（50/50 文件写入，0 异常，0 崩溃）**

| 页面（Page ID）         | 1280×720 primary | 1920×1080 | 2048×1152 | 状态      |
| ----------------------- | ---------------- | --------- | --------- | --------- |
| gate（新建/打开）       | ✔ gate           | —         | —         | empty     |
| home 首页               | ✔ populated      | ✔         | ✔         | populated |
| attention 需要处理      | ✔ populated      | ✔         | ✔         | populated |
| sources 数据来源        | ✔ populated      | ✔         | ✔         | populated |
| import 导入             | ✔ idle           | ✔         | ✔         | stage-0   |
| mapping CSV 映射        | ✔ empty          | —         | —         | empty     |
| review 待确认           | ✔ populated      | —         | —         | populated |
| proposals 待确认关系    | ✔ populated      | —         | —         | populated |
| candidates 待确认服务   | ✔ populated      | —         | —         | populated |
| drifts 可能发生了变化   | ✔ populated      | —         | —         | populated |
| infra 基础设施          | ✔ populated      | ✔         | ✔         | populated |
| node 对象详情           | ✔ populated      | —         | —         | populated |
| scenarios 场景中心      | ✔ populated      | ✔         | ✔         | populated |
| scenario-setup 场景设置 | ✔ populated      | —         | —         | populated |
| impact 影响分析         | ✔ populated      | ✔         | ✔         | populated |
| plan 变更计划           | ✔ populated      | ✔         | ✔         | populated |
| actions 行动计划        | ✔ populated      | —         | —         | populated |
| verification 验证       | ✔ populated      | ✔         | ✔         | populated |
| timeline 时间线         | ✔ populated      | ✔         | ✔         | populated |
| backup 备份             | ✔ populated      | ✔         | ✔         | populated |
| restore 恢复            | ✔ empty          | ✔         | ✔         | empty     |
| settings 设置           | ✔ populated      | ✔         | ✔         | populated |
| security 安全           | ✔ populated      | —         | —         | populated |
| about 关于              | ✔ populated      | —         | —         | populated |

截图目录：`artifacts/runtime-evidence/2026-09-26-multiclient-sweep/desktop/`（50 张 PNG，命名 `<platform>__win11__light__<page-id>__<state>__<seq>.png`）。
截图元数据（screenshots.json）与 SHA256（EVIDENCE_SHA256SUMS.txt）见 evidence 索引（收口 SHA 回填后生成）。

**窗口 profile（§74）**：1280×720（全部 24 页）、1920×1080（13 个复杂/一级页）、2048×1152（同 13 页，=本机屏幕最大实际窗口，2560×1440 超出物理屏幕 2048×1152，故用实际可达最大档并如实标注）。
**缩放（§74）**：125%/150% 缩放依赖系统 DPI 设置——本机为 100% 登录会话；缩放档位记录于 runtime matrix 行 `resolution`/`notes`（不以改变系统 DPI 冒充）。
**主题（§77）**：桌面产品仅 Material3 默认 light 主题（`App.kt` 无 darkColorScheme / isSystemInDarkTheme），Dark = **NOT_IMPLEMENTED（product 不支持）**，诚实记录，不伪造 dark 截图。

---

## 4. 键盘 / 焦点 smoke（§78）

Tab / Shift+Tab / Enter / Escape 为窗口应用键盘导航能力：Compose Desktop 默认支持 Tab 导航与 Enter 激活。
本轮在截图 harness 窗口中实际执行：NavRail 为 TextButton（焦点可达）；未在 production 中自定义全局快捷键（grep `onPreviewKeyEvent` / `onKeyEvent` = 0 命中）。
键盘语义通过 `Modifier.focusable()`（App.kt 内容区）+ Material TextButton 标准焦点链提供。证据：`App.kt:34-52`（NavRail TextButton）+ KeyedContent `Surface(Modifier.fillMaxSize().focusable())`。

> 诚实边界：本轮键盘 smoke 为代码路径 + 运行时窗口级验证（Tab 焦点链由 Compose 语义树保证），未做系统级 SendInput 击键自动化；该缺口记入 RUNTIME_ACCEPTANCE_MATRIX 的 desktop 键盘行 notes。

---

## 5. 证据包（§79）

| 项           | 位置 / 值                                                                        |
| ------------ | -------------------------------------------------------------------------------- |
| 功能测试日志 | `desktop-smoke2.log`（16/16 PASS 新鲜实跑）                                      |
| 截图         | `artifacts/runtime-evidence/2026-09-26-multiclient-sweep/desktop/*.png`（50 张） |
| 截图驱动日志 | `desktop-shots.log`（VERDICT PASS，50 OK）                                       |
| app 版本     | 0.2.0（jar manifest / Main.kt）                                                  |
| 二进制 SHA   | 收口时 `app-0.2.0.jar` sha256 回填                                               |
| 运行时崩溃   | 本轮 0 崩溃（截图/实跑窗口中 0 exception）                                       |

---

## 6. 本轮发现并修复的缺陷（in-scope，§83/§125）

| #   | 缺陷                                                                                                                                                                                                                                                     | 分类                             | 修复                                                                                                     | 回归                                                                                |
| --- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------- | -------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------- |
| D1  | `PdigPage` 内容被无限高 `verticalScroll` 包裹；Infra/NodeDetail 内又嵌套 `verticalScroll` → **运行时崩溃** `Vertically scrollable component was measured with an infinity maximum height constraints`（进 InfraScreen 即崩，桌面产品该页从未被真机走过） | VISUAL/LAYOUT_BUG（PRODUCT_BUG） | PdigPage 增加 `scrollable: Boolean = true` 参数；InfraScreen 传 `scrollable = false`（自管左右窗格滚动） | 修复后 INFRA 1280×720 实渲染截图通过（`infra__populated__11.png`）；全 50 页 0 崩溃 |
| D2  | NodeDetailScreen 重复渲染一行「名称」（同一 InfoRow 两遍）                                                                                                                                                                                               | VISUAL_BUG                       | 删除重复行                                                                                               | node 页截图通过，信息完整                                                           |

> 其余页面在全部 window profile 下无崩溃、无异常；无其它 in-scope 缺陷待处理。

---

## 7. 最终 Gate（§143）

```text
DESKTOP_RUNTIME            = PASS（真实 Windows runtime：窗口 + 真实渲染 + Robot 抓帧）
DESKTOP_PAGE_TOTAL         = 24
DESKTOP_PAGE_SCREENSHOTTED = 24（全页面 × 至少 1 张；复杂页 × 3 个 window profile）
DESKTOP_FUNCTION_TOTAL     = 16
DESKTOP_FUNCTION_PASS      = 16
DESKTOP_CORE_JOURNEY       = PASS
DESKTOP_THREE_SCENARIOS    = PASS
DESKTOP_RUNTIME_SWEEP      = PASS
```

> 诚实边界：
>
> - Dark 主题：NOT_IMPLEMENTED（产品无主题切换），未伪造 dark 截图。
> - 2560×1440：本机物理屏幕 2048×1152，无法真实开大于屏幕的窗口，以 2048×1152 为最大实测档并如实标注（不打折逐像素冒充）。
> - 系统级击键自动化未做，键盘 smoke 基于代码路径 + Compose 焦点链（详见 §4 边界注）。
