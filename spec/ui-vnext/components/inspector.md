# Inspector — Master-detail 右栏信息检查器（号码/节点详情摘要）

> Visual Contract · spec/ui-vnext · 2026-09-29 · No-Vision 模式（实现以此契约为准，非参考图）

## 1. 职责与位置

- **职责**：Desktop master-detail 布局的右栏信息检查器——展示号码/节点详情的紧凑摘要（identity、关联服务、登录用途、2FA、恢复用途、风险、备用路径、历史）。**只读为主**，动作是去详情页的链接（INTERACTION_CONTRACT.md §6）。
- **使用位置**：Infrastructure Numbers 列表右侧（`LAYOUT_CONTRACT.json numbers.inspectorTestId = pdig.phone.inspector`）；卡片列表场景复用同一组件（右侧摘要）。
- **边界**：不是编辑表单；不触发领域写操作；编辑经链接跳转到详情/定制页。

## 2. 数据契约

分节模型（全部可缺失，缺失项显示「未设置 / 未知」，禁止推断）：

| 分节 | 内容 | 来源 copy（spec/ui/copy-zh.json） |
| --- | --- | --- |
| identity | 昵称 / 遮罩号码 / 地区 / 运营商 / SIM eSIM / 角色 / 状态 | `identity.phoneNumber`（手机号） |
| 关联服务 | 依赖服务列表（服务名 + 关系类型） | `relations.funding_source`（资金来源）、`relations.merchant_agreement`（自动扣款/订阅） |
| 登录用途 | authenticates 关系 | `relationsV030.authenticates`（验证方式/登录依据） |
| 2FA | 2FA 绑定用途 | 新增 copy key：`uiVNext.inspector.twoFA = 两步验证` |
| 恢复用途 | recovers 关系 | `relationsV030.recovers`（恢复方式/找回路径）、`identity.recoveryPath`（恢复路径） |
| 风险 | 风险摘要（来自 findings/impact 语义） | `impact.mustChange`（必须处理）、`impact.backupPath`（有备用路径）、`impact.needsReview`（建议检查）、`impact.unaffected`（不受影响） |
| 备用路径 | recovery/fallback 摘要 | `impact.backupPath`、`failureDomain.independentPath`（独立恢复路径）、`recoveryCycle.confirmed`（非独立说明） |
| 历史 | 最近变更/验证记录 | `planWorkflow.verifying`（待验证）、`planWorkflow.completed`（已完成）、`verification.verified`（已验证） |

- 缺失项文案：新增 copy key `uiVNext.notSet = 未设置`、`uiVNext.unknown = 未知`（与 asset-card 共用）。
- 对象标识（对象名/号码）在 Privacy Mask 下必须遮罩：`DESIGN_TOKENS.json privacyMask.maskCardLast4 / maskPhoneNumbers / maskAccountNames`（默认 true）。

## 3. 几何

| 项 | 键名 | 数值 |
| --- | --- | --- |
| Inspector testId | `LAYOUT_CONTRACT.json numbers.inspectorTestId` | `pdig.phone.inspector` |
| 布局形态 | `numbers.desktopDefault` | `"list-table + inspector"`（master-detail 右栏） |
| 右栏宽度 | master-detail 分栏内自适应；量级参考 `overview1920.activityRail.widthPx {min:300, max:380}` | `300 – 380` |
| 分节间距 | `DESIGN_TOKENS.json spacing.sectionGap` | `24` |
| 分节内间距 | `spacing.md = 12` / `spacing.lg = 16` | DESIGN_TOKENS.json |
| 行高 | RESPONSIVE_CONTRACT.json rules「row 40-48」 | `40 – 48` |
| 圆角 | `radius.md`（面板） | `10` |

## 4. 视觉

| 元素 | token 键 | token 值 |
| --- | --- | --- |
| 面板背景 | `semantic.data.panel / .panelRaised` | `#0B1A33` / `#102340` |
| 分节标题 | `typography.sectionTitle`（15/600/1.4）；文字 `semantic.ink.strong` | `#F4F7FF` |
| 正文 | `typography.body`（14/400/1.5）`semantic.ink.regular` | `#A9B8D5` |
| meta | `typography.meta`（12/400/1.4）`semantic.ink.muted` | `#7383A3` |
| 分隔 | `semantic.data.divider = colors.borderSubtle` | `rgba(148,180,234,0.13)` |
| 风险等级 | `semantic.status.ok / .warning / .critical / .unknown`（icon+label+color 三通道） | `#3BD49B` / `#F4B64B` / `#FA626B` / `#8C9AB5` |
| 链接/动作 | `colors.primaryBright`（文字链接） | `#67A7FF` |
| 聚焦 ring | `semantic.focus` | `#67A7FF` |

- 数据可读层（L3–L5）不允许玻璃模糊压字（VISUAL_DNA.md §4 总规则、DESIGN_TOKENS.json policy `colorRule`）。

## 5. 交互/状态

- 只读为主：操作 = 链接，路由至真详情页：`/infrastructure/numbers/{id}`、`/infrastructure/cards/{id}`（INTERACTION_CONTRACT.md §6）。
- 列表行选择 ↔ inspector 内容联动（master-detail）；Arrow 切换行（§5 键盘）。
- 异步加载：短 loading（copy-zh.json `common.loading` 加载中…）+ 完成 toast/inline 确认；错误 = danger 软底 + 人话 + 重试（copy-zh.json `common.retry` 重试）（INTERACTION_CONTRACT.md §9）。
- 动画 ≤ `tokens.normalMs`（200ms）；无装饰循环动画（MOTION_CONTRACT.json rules）。

## 6. 无障碍与隐私

- 键盘可达：分节标题可聚焦（heading 导航）、行可用 Arrow/Enter；focus ring 可见。
- 触控：链接目标 ≥ `components.touchTarget`（44/48）。
- 对比度：`contrast.bodyTextMin 4.5:1` / `contrast.largeTextMin 3:1`。
- 隐私：号码 last4 / 完整号码 / 账户名按 `privacyMask.*` 遮罩；日志不记录明细（goal §44、AGENTS §17）。

## 7. 验收自检

- [ ] testId 存在：`pdig.phone.inspector`；分节可展开至详情路由。
- [ ] 几何符合：右栏 300–380 量级；行高 40–48；分节间距 24。
- [ ] 只读为主，无隐藏领域写操作；动作均为真路由链接。
- [ ] 缺失分节显示未设置/未知（不推断）。
- [ ] 状态三通道；无远程资源；mask 生效。
- [ ] 文案来自 copy-zh 或标注「新增 copy key：uiVNext.*」。