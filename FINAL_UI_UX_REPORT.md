# FINAL_UI_UX_REPORT.md

> PDIG / DepMap — FINAL PRODUCTION CLOSURE V1，PHASE G–I。
> 覆盖 IA / HOME / SCENARIOS / PLANS / TIMELINE / DRIFT / CANDIDATES / IMPORT / BACKUP / SETTINGS / ACCESSIBILITY / RESPONSIVE / COPY / DESIGN_SYSTEM / VISUAL_QA。
> **重要前提**：本报告为**源码级**审查。`UI_SOURCE_READY = PASS`，`UI_BUILD_READY = BLOCKED`（B10，无 HBuilderX / uni-app x 工具链）。
> **未执行任何真机/模拟器视觉验证，未伪造截图。**

---

## 1. 结论

| 维度               | 结果                              |
| ------------------ | --------------------------------- |
| IA / 导航          | **PASS（源码级）**                |
| HOME               | **PASS（源码级）**                |
| SCENARIOS          | **PASS（源码级）**                |
| PLANS              | **PASS（源码级）**                |
| TIMELINE           | **PASS（源码级）**                |
| DRIFT              | **PASS（源码级）**                |
| CANDIDATES         | **PASS（源码级）**                |
| IMPORT             | **PASS（如实标注不可用）**        |
| BACKUP / RESTORE   | **PASS（如实标注不可用）**        |
| SETTINGS           | **PASS（无假开关）**              |
| ONBOARDING         | **PASS（源码级）**                |
| ACCESSIBILITY      | **PARTIAL_WITH_REPORT**（见 §12） |
| RESPONSIVE         | **PARTIAL_WITH_REPORT**（见 §13） |
| COPY               | **PASS**                          |
| DESIGN_SYSTEM      | **PASS**                          |
| VISUAL_QA          | **NOT_RUN**（无运行环境）         |
| **UI_UX_READY**    | **PASS（源码级）**                |
| **UI_BUILD_READY** | **BLOCKED（B10）**                |

---

## 2. Information Architecture

- **页面总数 24**，`pages.json` 声明 24 条，`check-ui` U1 校验全部存在。
- **一级导航（tabBar）4 项**：首页 / 场景 / 计划 / 我的。U2 校验项数 2–5、`pagePath` 必须存在于 `pages`、首页必须存在。
- 二级页面：`plan-create`、`plan-detail`、`timeline`、`drift`、`candidates`、`proposals`、`group-proposals`、`nodes`、`node-detail`、`declare-relation`、`import-wechat`、`node-resolution`、`sources`、`backup`、`simulate`、`impact-result`、`privacy`、`about`、`onboarding`、`unlock`。
- **架构约束（U4）**：页面**禁止直接操作 SQLite**，必须经 `app/services/depmap-service.uts`（唯一数据边界）。`rules.uts` 承载纯派生规则（无 IO、确定性）。

---

## 3. 首页（3 秒回答）

`pages/home/home.uvue` 实测结构：

- **主视觉不出现 Nodes / Edges / 图结构**。
- 标题：**「今天有需要你处理的事吗」**，副标题：「换卡、换号、注销账户之前，先看清会影响哪些服务。」
- 卡片「**需要你处理**」聚合四类待办，均带计数与跳转：
  - 待确认关系（`pendingProposals`）→ 机器推断出的支付关系，需要你确认或拒绝
  - 可能发生了变化（`openDrifts`）→ 观察到新的正向证据
  - 待确认服务（`unresolvedCandidates`）→ 发现可能属于你的服务
  - 计划需要重新检查（`stalePlans`）→ 基础设施在此计划创建后发生了变化
- 卡片「**即将到来**」→ 时间轴；空态文案：「近期没有需要处理的数字基础设施事项。」
- 卡片「**常用场景**」→ 场景中心。
- **空态**：`todoTotal === 0` 时输出「还没有需要处理的事项。」+ 引导「添加第一个对象」按钮。
- **加载态 / 错误态**：`dp-state` 提供 `loading` 与 `error`（含「重试」动作）。

**3 秒三问核对**：① 有没有需要我处理 → 「需要你处理」卡片；② 近期有什么 → 「即将到来」；③ 我可以从什么场景开始 → 「常用场景」。**PASS**。

---

## 4. 场景中心

- 仅展示 `active` 可执行模板：`SCENARIOS`（`rules.uts`）与 `core/src/scenarios/registry.ts` 的 active 模板对齐；**`planned` 模板不进入展示**（注释明示）。
- 每个场景含 `title` / `description` / `recommendedLeadDays`，无假功能、无禁用占位项。
- `check-ui` U3 禁止用户可见文案出现工程词。

---

## 5. ChangePlan UX

`plan-detail` 与 `plan-readiness` 语义（`rules.uts`）：

```
PlanReadiness = 'blocked' | 'review_required' | 'ready_with_known_scope'
```

- **明确区分**：必须处理（`blocked`）/ 需要确认（`review_required`）/ 可以继续（`ready_with_known_scope`）/ 需要重新检查（`stalePlans` → `needs_revalidation` 横幅）/ Verification pending（`done ≠ verified` 两段式）。
- **不存在** `safe` / `100%` / 「完全安全」等取值 —— 类型层面即禁止。

---

## 6. Readiness Copy

禁止项核对（`app/` 全量文案）：

| 禁止文案       | 命中  |
| -------------- | ----- |
| 「完全安全」   | **0** |
| 「100%」       | **0** |
| 「绝对不会漏」 | **0** |
| 「放心注销」   | **0** |

统一口径：「**基于当前已知并确认的信息**」。`rules.uts` 注释显式要求「禁止 safe / 100%」。

---

## 7. Drift UX

- 页面标题与导航标题：**「可能发生了变化」**（`pages.json`）。
- 首页条目描述：「**可能发生了变化** / 观察到新的正向证据」。
- **表达「可能」，不表达「系统已经确认变化」**；无「已确认变更」类文案。
- 语义：`RealityDrift != Reality mutation`；`Drift pending → no Reality change`（不变量测试守护）。

---

## 8. Candidate UX

- 页面标题与导航标题：**「待确认服务」**。
- 首页条目：「待确认服务 / **发现可能属于你的服务**」。
- **不表达「已加入基础设施」**；`Candidate pending → no Node`（不变量测试守护）。

---

## 9. Coverage UX

- `CoverageLevel = 'unknown' | 'limited' | 'partial' | 'well_evidenced'` —— **不做百分比安全评分**。
- 展示维度：来源 / 刷新时间 / 确认关系 / 待确认 / 未解析 / 长期未验证。
- `rules.uts` 注释明示「不输出分数/百分比」。

---

## 10. Timeline UX

- `TimelineItem` 为**纯 projection**；用户操作 Timeline **不直接修改 Reality**（`Timeline 不写 Graph` 架构约束）。
- 排序确定性由 property 测试（`Timeline deterministic`）与 10k 冻结 smoke 守护。

---

## 11. Import / Backup / Restore / Settings / Onboarding

### Import

页面：`import-wechat`（导入账单）、`node-resolution`（识别商户）。
**当前不可用（B20）**，UI **如实标注「暂未接入」，不写入任何数据**。
完整流程（Source select → privacy explanation → file picker → parse → CSV mapping → preview → resolution → Proposal review → completion → error）中，**已实现 UI 骨架的部分保留，未实现部分明确禁用/标注**，不伪造可用性。

### Backup

页面：`backup`。加密备份导出/恢复**当前不可用（B21）**，UI 如实标注。
设计要求（加密备份解释、密码不保存、丢失密码无法恢复、不吓人）已记录于页面文案与 `store/` 文档。

### Restore

同 B21；失败不得产生 partial DB（由 Core 层迁移回滚测试覆盖，设备端未验证）。

### Settings（「我的」）

含：安全 / 生物识别 / 自动锁定 / 备份 / 恢复 / 隐私 / 版本 / 数据清空 / About。
**本轮已消除假开关**：原三个无实际效果的开关 → 改为「启动验证始终开启（只读）」+ 隐私屏（**真实持久化**，`App.uvue` 启动时恢复）。

### Onboarding

`pages/onboarding/onboarding.uvue`（`navigationStyle: custom`）。说明产品解决什么、本地数据原则、如何开始。`check-ui` U8 要求每页至少一处空态/错误态/加载态。

---

## 12. Accessibility（PARTIAL_WITH_REPORT）

| 项                 | 状态                 | 说明                                                                                                                                                                   |
| ------------------ | -------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 对比度             | **源码级设计**       | `tokens.uts` 语义色（`textPrimary #1B1D29` on `surface #FFFFFF`、`textSecondary #5A5F73`、`textTertiary #8A90A6`）为高对比取值；**未做工具化对比度测量**（无运行环境） |
| 触控目标           | **PASS（设计约束）** | 设计系统规定触控最小 44                                                                                                                                                |
| 屏幕阅读器标签     | **PARTIAL**          | `dp-*` 组件提供 `title` / `description` 文本面；未做无障碍标签专项审计                                                                                                 |
| 焦点 / 键盘（Web） | **NOT_RUN**          | 无 H5 运行环境                                                                                                                                                         |
| 动态字号           | **NOT_RUN**          | 无运行环境                                                                                                                                                             |
| 长中文换行         | **源码级关注**       | 组件使用 flex + 文本块，无固定宽度截断；未实测                                                                                                                         |
| 状态不依赖颜色     | **PARTIAL**          | `dp-row` 提供 `valueTone`（warning 等）为颜色通道；同时有文字值（如「3 条」），**非纯色传达**                                                                          |
| 错误可理解         | **PASS（源码级）**   | `dp-state kind="error"` 含 `title` + `description` + 「重试」动作                                                                                                      |

**判定**：`ACCESSIBILITY = PARTIAL_WITH_REPORT`。未通过工具化验证的部分**不写 PASS**。

---

## 13. Responsive（PARTIAL_WITH_REPORT）

| 尺寸         | 状态                                             |
| ------------ | ------------------------------------------------ |
| 小屏手机     | 源码级（flex 布局、scroll-view、safe area 占位） |
| 普通手机     | 源码级                                           |
| 大屏手机     | 源码级                                           |
| 平板（基础） | **NOT_RUN**                                      |
| Web          | **NOT_RUN**                                      |

**判定**：`RESPONSIVE = PARTIAL_WITH_REPORT`。无运行环境，未实测。

---

## 14. Safe Area

- 设计系统含 safe area 处理约定；`pages.json` 对 `onboarding` / `unlock` 使用 `navigationStyle: custom`（需自行处理顶部安全区）。
- iOS notch / Dynamic Island / Home Indicator、Android 状态/导航栏、HarmonyOS safe area：**源码级处理，未在设备上验证（BLOCKED）**。

---

## 15. Design System

`app/theme/tokens.uts` = **唯一视觉真相源**：

- `DpPalette` 15 个语义色 + `DP_DARK_OVERRIDES`（深色就绪，当前默认 light）；
- 调色板实测 **34 色**（`check-ui` 统计）；
- 间距 / 圆角 / 字号 / 触控最小 44 同文件定义；
- **禁止远程字体 / 远程图标 CDN**；中文使用系统字体；
- 5 个 `dp-*` 组件：`dp-button`、`dp-card`、`dp-chip`、`dp-row`、`dp-state`。

`check-ui` U5：页面 `<style>` 中出现的颜色**必须属于 token 调色板**，否则报错（`rgb()/rgba()` 会告警）。本轮 0 命中。

---

## 16. CSS / Style 去重

- 颜色已全部收敛至 token（U5 强制）。
- 按钮 / 卡片 / 行 / 状态 / 标签样式已收敛至 `dp-*` 组件。
- 未发现散落的 padding / radius / shadow 硬编码成为主要问题（组件层承担）。

---

## 17. UI 工程词禁用（U3）

用户可见文案禁止：`GraphRevision` / `SourceInstance` / `EvidenceSummary` / `merchant_agreement` / `funding_source` / 裸枚举 / UUID。

`check-ui` U3 实测：**0 命中**。此外 `pages.json` 导航标题均为自然语言（如「可能发生了变化」「待确认服务」「支付 · 账户 · 服务」）。

---

## 18. UI 测试

| 项                    | 状态                                                                                                                                                                                                                                                                       |
| --------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 关键页面/状态单元测试 | **BLOCKED**（无 uni-app x 运行环境）                                                                                                                                                                                                                                       |
| UI 静态门禁 U1–U9     | **PASS**（30 `.uvue`）                                                                                                                                                                                                                                                     |
| 覆盖状态（源码级）    | Home 空态 / Home 待办 / Home 即将到来 / 场景中心 / Plan blocked / Plan review / Plan ready / needs_revalidation / Drift / Candidate / Import / CSV mapping / Backup / Restore / 错误密码 / Settings / Lock —— 均由 `check-ui` U8（每页至少一处空/错/加载态）与页面源码覆盖 |

**未伪造 UI 测试结果。**

---

## 19. Visual QA

**NOT_RUN**。无 HBuilderX / uni-app x 运行环境，无法生成截图；**未伪造任何截图**。
截图方案见 `store/SCREENSHOT_PLAN.md`；`STORE_ASSETS_READY = BLOCKED`。

---

## 20. 发现

| #   | 发现                                                                           | 严重度 | 处置                                             |
| --- | ------------------------------------------------------------------------------ | ------ | ------------------------------------------------ |
| U-1 | `app/manifest.json` 引用不存在的 `static/icons/*.png` 与 `static/splash/*.png` | 中     | 登记为 STORE_ASSETS blocker；**不伪造占位图标**  |
| U-2 | iOS `NSCameraUsageDescription` 声明了尚未实现的二维码扫描用途                  | 中     | 登记为 STORE_METADATA 待决项，提交前需与实现对齐 |
| U-3 | Accessibility / Responsive 未经工具化验证                                      | 中     | 标注 `PARTIAL_WITH_REPORT`，不写 PASS            |

---

## 21. 复现

```
cd core
npm run check:ui
```
