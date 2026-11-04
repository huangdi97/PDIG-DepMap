# Continuity View — Continuity Rail（连续性轨道 / make-before-break）

> Visual Contract · spec/ui-vnext · 2026-09-29 · No-Vision 模式（实现以此契约为准，非参考图）

## 1. 职责与位置

- **职责**：签名元素「Continuity Rail」（VISUAL_DNA.md §7.3，沿用 v0.3.1 旗舰语义）——把换卡/换号流程表达为结构化连续轨道：`current → prerequisite → transition → verification → completion`；每个阶段渲染 `completed / current / blocked / verifying / upcoming` 状态节点。
- **使用位置**：Change Phone（`/change/phone`，**flagship**）与 Change Card（`/change/card`）场景页主视觉（IA.md §4 Screen Registry）。
- **边界**：Rail 是流程结构表达，不产生领域写操作；projection（计划）态绝不冒充已完成（INTERACTION_CONTRACT.md §7）。

## 2. 数据契约

Change Phone 6 阶段（INTERACTION_CONTRACT.md §7）：

| # | 阶段 | copy（spec/ui/copy-zh.json） |
| --- | --- | --- |
| 1 | 影响分析 | `scenarioV030.reviewImpact`（查看影响）区域语义；建议新增 `uiVNext.change.impactAnalysis = 影响分析` |
| 2 | 建立新号码 | `scenarioV030.addNewPhone`（添加新手机号） |
| 3 | 验证 | `scenarioV030.verifyNewPhone`（验证新手机号） |
| 4 | 迁移关键账户 | `scenarioV030.migrateAccounts`（迁移关键账户） |
| 5 | 检查恢复路径 | `scenarioV030.reviewRecoveryPaths`（查看恢复路径） |
| 6 | 停用旧号码 | `scenarioV030.retireOldPhone`（停用旧手机号（新路径全部验证后）） |

阶段节点状态（INTERACTION_CONTRACT.md §7）：

| 状态 | 呈现 | 相关 copy |
| --- | --- | --- |
| completed | 实勾 | `planWorkflow.completed`（已完成） |
| current | 空心 + 编号 | `planWorkflow.in_progress`（进行中） |
| blocked | `!` 徽标 + 明文原因 | `changePlanV030.waitingVerification`（等待验证）、`uiuxV031.blockedReason`（新路径验证通过后才能执行这一步） |
| verifying | 时钟 | `planWorkflow.verifying`（待验证） |
| upcoming | 灰 | `planWorkflow.draft`（草稿）语义参考；新增 copy key `uiVNext.change.upcoming = 后续阶段` |

迁移项状态（§7）：`migrated / waiting / not started / blocked`（`changePlanV030.newPathEstablished` 新路径已建立 / `newPathVerified` 新路径已验证 / `oldPathRetired` 旧路径已停用 / `waitingVerification` 等待验证）。

视觉模型：`OLD NUMBER → services/accounts → NEW NUMBER`（§7）。

- make-before-break 闸门明文：stage 6 在 stage 3 验证通过前 **disabled + 明文原因**（§7），绝不只禁用；copy：`changePlanV030.verifyBeforeRemove`（验证后才能移除旧路径）、`uiuxV031.blockedReason`。
- **plan projection**（未来态）必须带「计划」label，不得伪造已完成（§7）；新增 copy key `uiVNext.plan = 计划`。
- **建议文案**：新增 copy key `uiVNext.change.title = 更换手机号`、`uiVNext.change.oldNumber = 旧手机号`、`uiVNext.change.newNumber = 新手机号`、`uiVNext.change.services = 关联服务与账户`；标题主文案可用 `scenarioV030.replacePhoneNumber`（更换手机号）。

## 3. 几何

| 项 | 键名 | 数值 |
| --- | --- | --- |
| 阶段节点 testId | 本契约新增（前缀 `pdig.change.`，DESIGN_TOKENS.json `testIds.prefixes`） | `pdig.change.phone.rail`、`pdig.change.phone.stage1…6`、`pdig.change.phone.gate`、`pdig.change.phone.item` |
| 节点视觉直径 | `DESIGN_TOKENS.json spacing.xxl` | `24`（编号 15/600 字居中） |
| 节点触控热区 | `components.touchTarget` | Android 48 / iOS 44（透明扩大） |
| 阶段间距 | `spacing.lg = 16` / `spacing.xl = 20` | DESIGN_TOKENS.json |
| OLD/services/NEW 三列 | 列内面板 `radius.md = 10`；分栏 gap `spacing.xxxl = 32` | DESIGN_TOKENS.json |
| 行高 | RESPONSIVE_CONTRACT.json rules「row 40-48」 | `40 – 48` |

- Desktop：横向轨道（阶段条 + 迁移列表）；Mobile：垂直阶段列表（RESPONSIVE_CONTRACT.json android.phone「active change」入口）。

## 4. 视觉

| 状态 | token 键 | token 值 |
| --- | --- | --- |
| completed | `semantic.status.ok = colors.positive` | `#3BD49B` |
| current | `semantic.focus = colors.primaryBright` | `#67A7FF` |
| blocked | `semantic.status.critical = colors.critical` + 原因行 `semantic.ink.strong` | `#FA626B` / `#F4F7FF` |
| verifying | `semantic.status.warning = colors.warning` | `#F4B64B` |
| upcoming | `semantic.ink.muted = colors.textMuted` | `#7383A3` |
| 迁移项 migrated/waiting/not started/blocked | `semantic.status.ok`（migrated）/ `semantic.ink.regular`（waiting）/ `semantic.ink.muted`（not started）/ `semantic.status.critical`（blocked） | 同上 |
| 轨道背景 | `semantic.data.panel`；连线 `semantic.data.divider = colors.borderSubtle` | `#0B1A33` / `rgba(148,180,234,0.13)` |
| OLD/NEW 号码面 | 程序化卡面（复用 phone-card 视觉，禁远程图） | — |
| 计划（projection） | 虚线边框 + `uiVNext.plan` label + `semantic.ink.muted` | `#7383A3` |

- 状态三通道（icon + label + color），线/勾/时钟/!均为 shape 通道（VISUAL_DNA.md §4 L5）。

## 5. 交互/状态

- 阶段状态机（INTERACTION_CONTRACT.md §7）：线性 `1 → 2 → 3 → 4 → 5 → 6`；stage 6 在 stage 3 验证通过前 = disabled + 明文原因。
- 阶段行可点击 → 对应详情（迁移 → `/infrastructure/numbers/{id}` 或账户/服务列表；恢复路径 → inspector 恢复用途）；动作沿真路由（copy-zh.json `scenarioV030.reviewImpact` 等）。
- 状态更新动画：`microInteractions.statusChangeMs = 180`、完成勾选 `checkPopMs = 220`、navigation `navigationSwitchMs = 200`（MOTION_CONTRACT.json）；reduce motion 下关 idle/camera/arc（MOTION_CONTRACT.json `reduceMotion`）。
- plan projection 项不与已完成混淆：计划项一律虚线 + 「计划」label，切屏/截图仍可辨识。

## 6. 无障碍与隐私

- Rail 语义化（ol/阶段 list）：screen reader 可整体朗读 6 阶段与当前/阻塞状态；blocked/disabled 原因可读。
- 键盘：Arrow 在阶段间移动、Enter 展开；focus ring = `semantic.focus`。
- 触控 ≥ 44/48；对比度 ≥ `contrast.bodyTextMin 4.5:1`。
- 隐私：OLD/NEW 号码与账户名按 `privacyMask.maskPhoneNumbers / maskAccountNames` 蒙版；plan projection 不含真实未发生数据（不得伪造）。

## 7. 验收自检

- [ ] testId 存在：`pdig.change.phone.rail`、`pdig.change.phone.stage1…6`、`pdig.change.phone.gate`、`pdig.change.phone.item`（前缀按 DESIGN_TOKENS testIds.prefixes）。
- [ ] 6 阶段顺序与状态机一致；stage 6 在 stage 3 验证前 disabled + 明文原因。
- [ ] OLD→services→NEW 模型存在；迁移项 4 态（migrated/waiting/not started/blocked）。
- [ ] plan projection 带「计划」label，不冒充已完成。
- [ ] 状态三通道；无远程资源；mask 生效。
- [ ] 文案来自 copy-zh（`scenarioV030.*`、`changePlanV030.*`、`uiuxV031.blockedReason`）或标注「新增 copy key：uiVNext.*」。