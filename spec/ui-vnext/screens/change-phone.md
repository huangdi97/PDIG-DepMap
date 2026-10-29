# Change Phone — 更换手机号（/change/phone，flagship）

> Visual Contract · spec/ui-vnext · 2026-09-29 · No-Vision 模式（实现以此契约为准，非参考图）

## 1. 职责与位置

- **职责**：**旗舰流程**（IA.md §4 `change-phone`；goal §12；VISUAL_DNA.md §7.3 Continuity Rail 旗舰语义）——把"换手机号"组织为 6 阶段 + Continuity Rail × make-before-break 闸门 + OLD→NEW 视觉模型；**未来态 plan projection 明确标注，不伪造**。
- **路由**：`/change/phone`（IA.md §4），一级「变更」`pdig.nav.change` 之下；同类 flow 入口 `/change/card` 复用同一组件（components/continuity-view.md）。
- **边界**：Rail/阶段行为流程表达；plan projection ≠ 已完成（INTERACTION_CONTRACT.md §7）；影响范围来自已确认现实状态（goal §14，Proposal/confidence 不直接产生"必须处理"）。

## 2. 数据契约

6 阶段（INTERACTION_CONTRACT.md §7；copy 均来自 spec/ui/copy-zh.json 或标注新增）：

| # | 阶段 | copy key | 文案 |
| --- | --- | --- | --- |
| 1 | 影响分析 | `scenarioV030.reviewImpact`（区域语义）/ 新增 `uiVNext.change.impactAnalysis = 影响分析` | 影响范围摘要 |
| 2 | 建立新号码 | `scenarioV030.addNewPhone` | 添加新手机号 |
| 3 | 验证 | `scenarioV030.verifyNewPhone` | 验证新手机号 |
| 4 | 迁移关键账户 | `scenarioV030.migrateAccounts` | 迁移关键账户 |
| 5 | 检查恢复路径 | `scenarioV030.reviewRecoveryPaths` | 查看恢复路径 |
| 6 | 停用旧号码 | `scenarioV030.retireOldPhone` | 停用旧手机号（新路径全部验证后） |

阶段节点状态（§7）：`completed`（实勾，`planWorkflow.completed` 已完成）/ `current`（空心+编号，`planWorkflow.in_progress` 进行中）/ `blocked`（! 徽标 + 原因）/ `verifying`（时钟，`planWorkflow.verifying` 待验证）/ `upcoming`（灰，新增 `uiVNext.change.upcoming = 后续阶段`）。

迁移项状态（§7）：`migrated`（`changePlanV030.newPathEstablished` 新路径已建立 / `newPathVerified` 新路径已验证）/ `waiting`（`waitingVerification` 等待验证）/ `not started` / `blocked`。

视觉模型（§7）：`OLD NUMBER → services/accounts → NEW NUMBER`。

**make-before-break 闸门**（§7）：stage 6 在 stage 3 验证通过前 = **disabled + 明文原因**（`uiuxV031.blockedReason` 新路径验证通过后才能执行这一步；`changePlanV030.verifyBeforeRemove` 验证后才能移除旧路径），绝不只禁用。

**plan projection**：未来态以虚线 + 「计划」label 标注（新增 copy key `uiVNext.plan = 计划`），不得与已完成混淆、不得伪造（INTERACTION_CONTRACT.md §7、VISUAL_DNA.md §9）。

**建议文案**：新增 copy key `uiVNext.change.title = 更换手机号`（或用 `scenarioV030.replacePhoneNumber` 更换手机号）、`uiVNext.change.oldNumber = 旧手机号`、`uiVNext.change.newNumber = 新手机号`、`uiVNext.change.services = 关联服务与账户`。

## 3. 几何

| 项 | 键名 | 数值 |
| --- | --- | --- |
| testId | 本契约新增（前缀 `pdig.change.`，DESIGN_TOKENS.json `testIds.prefixes`） | `pdig.change.phone.rail`、`pdig.change.phone.stage1…6`、`pdig.change.phone.gate`、`pdig.change.phone.item` |
| 节点视觉直径 | `DESIGN_TOKENS.json spacing.xxl` | `24` |
| 节点热区 | `components.touchTarget` | Android 48 / iOS 44（透明扩大） |
| OLD/services/NEW 三列 | 面板 `radius.md = 10`；分栏 gap `spacing.xxxl = 32` | DESIGN_TOKENS.json |
| 阶段间距 | `spacing.lg = 16` / `spacing.xl = 20` | DESIGN_TOKENS.json |
| 页面留白 / 主间距 | `LAYOUT_CONTRACT.json desktopShell.pagePaddingPx 24` / `mainGapPx 20` | `24` / `20` |
| 行高 | RESPONSIVE_CONTRACT.json rules「row 40-48」 | `40 – 48` |

## 4. 视觉

| 状态 | token 键 | token 值 |
| --- | --- | --- |
| completed | `semantic.status.ok = colors.positive` | `#3BD49B` |
| current | `semantic.focus = colors.primaryBright` | `#67A7FF` |
| blocked | `semantic.status.critical = colors.critical` + 原因 `semantic.ink.strong` | `#FA626B` / `#F4F7FF` |
| verifying | `semantic.status.warning = colors.warning` | `#F4B64B` |
| upcoming / projection | `semantic.ink.muted = colors.textMuted` + 虚线边框 + 「计划」label | `#7383A3` |
| 轨道背景/连线 | `semantic.data.panel` / `semantic.data.divider = colors.borderSubtle` | `#0B1A33` / `rgba(148,180,234,0.13)` |
| OLD/NEW 号码面 | 程序化卡面（phone-card 视觉，禁远程图） | — |
| 主 CTA | `colors.primary` / `colors.primaryBright` | `#4D74FF` / `#67A7FF` |
| 聚焦 ring | `semantic.focus` | `#67A7FF` |

- 状态三通道（icon + label + color）；勾/时钟/! 均为 shape 通道（VISUAL_DNA.md §4 L5、INTERACTION_CONTRACT.md §9）。

## 5. 交互/状态

- 阶段状态机线性 `1 → 2 → 3 → 4 → 5 → 6`（INTERACTION_CONTRACT.md §7）；阶段间跳转受 prerequisite 约束（`changePlanV030.prerequisite` 必须先完成 / `completesBefore` 完成后才能继续 / `parallel` 可以并行处理）。
- 阶段行/迁移项点击 → 真详情路由（迁移账户 → `/infrastructure/numbers/{id}` 或账户/服务列表；恢复路径 → 恢复用途明细；影响分析 → `/now` attention）——动作不内联执行领域写操作。
- plan projection 项：只读、虚线、带「计划」label；切屏/截图仍可辨识。
- 动画：`statusChangeMs 180`、`checkPopMs 220`、`navigationSwitchMs 200`（MOTION_CONTRACT.json）；reduce motion 全静态（`reduceMotion` 四项）。

## 6. 无障碍与隐私

- Rail 语义化（ol/阶段 list）：screen reader 朗读 6 阶段、当前态、blocked 原因（含 disabled 原因）。
- 键盘：Arrow 阶段间移动、Enter 展开、Escape 关抽屉/回退；focus ring = `semantic.focus`（INTERACTION_CONTRACT.md §5）。
- 触控 ≥ 44/48；对比度 ≥ `contrast.bodyTextMin 4.5:1`。
- 隐私：OLD/NEW 号码与账户名按 `privacyMask.maskPhoneNumbers / maskAccountNames` 蒙版；plan projection 只展示已确认/已计划范围，不虚构（goal §10、§14）。

## 7. 验收自检

- [ ] testId 存在：`pdig.change.phone.rail`、`pdig.change.phone.stage1…6`、`pdig.change.phone.gate`、`pdig.change.phone.item`（前缀 `pdig.change.`）。
- [ ] 6 阶段顺序与状态机一致；stage 6 在 stage 3 验证前 disabled + 明文原因（make-before-break 明文）。
- [ ] OLD→services→NEW 模型存在；迁移项 4 态（migrated/waiting/not started/blocked）。
- [ ] plan projection 带「计划」label、虚线样式，不冒充已完成。
- [ ] 状态三通道；无远程资源；mask 生效。
- [ ] 文案来自 copy-zh（`scenarioV030.*` / `changePlanV030.*` / `uiuxV031.blockedReason` / `planWorkflow.*`）或标注「新增 copy key：uiVNext.*」。