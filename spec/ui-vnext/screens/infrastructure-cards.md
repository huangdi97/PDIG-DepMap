# Infrastructure Cards — 卡片管理（/infrastructure/cards）

> Visual Contract · spec/ui-vnext · 2026-09-29 · No-Vision 模式（实现以此契约为准，非参考图）

## 1. 职责与位置

- **职责**：管理全球卡片的基础设施二级页——过滤（全部/国家/实体虚拟/储蓄信用/币种/状态/即将到期）+ Desktop 默认 Visual Grid ↔ Compact List 切换（LAYOUT_CONTRACT.json `cards.gridDefault = "visual-grid"` / `alternate = "compact-list"`）。
- **路由**：`/infrastructure/cards`（IA.md §4）；iOS iPad 形态为 NavigationSplitView（RESPONSIVE_CONTRACT.json ios.ipad）。
- **优先级**：`pdig.nav.infra.cards` = 最高优先二级页（IA.md §3），**不是一级导航**。
- **边界**：卡片项 = 可独立操作对象（components/asset-card.md 契约）；缺失数据只显示「未设置 / 未知」，禁止推断（goal §12）。

## 2. 数据契约

卡信息模型（goal §12）：

| 字段 | 缺失规则 |
| --- | --- |
| `nickname / issuer / last4 / region / currency / type(储蓄|信用) / physical|virtual / expiry / status` | 缺失 = 未设置 / 未知（不推断） |

过滤维度（goal §12）：全部 / 国家 / 实体虚拟 / 储蓄信用 / 币种 / 状态 / 即将到期。

- 过滤状态与路由 query 同步（如 `?region=HK`、`?type=virtual`、`?expiry=soon`；INTERACTION_CONTRACT.md §6 抽屉「查看卡片」→ `/infrastructure/cards?region=HK`）。
- **建议文案**（不修改 copy-zh.json）：新增 copy key `uiVNext.cards.filter.all = 全部`、`uiVNext.cards.filter.region = 国家`、`uiVNext.cards.filter.kind = 实体/虚拟`、`uiVNext.cards.filter.type = 储蓄/信用`、`uiVNext.cards.filter.currency = 币种`、`uiVNext.cards.filter.status = 状态`、`uiVNext.cards.filter.expiring = 即将到期`、`uiVNext.cards.viewGrid = 网格视图`、`uiVNext.cards.viewList = 列表视图`。
- 卡片/号码双入口连通：本页不重复出现号码语义（号码在 `pdig.nav.infra.numbers`）。

## 3. 几何

| 项 | 键名 | 数值 |
| --- | --- | --- |
| Grid @1920 | `LAYOUT_CONTRACT.json cards.desktopGrid1920` | `columns 4、gapPx {min:16,max:20}、cardAspectRatio 1.586` |
| Grid @1280 | `cards.desktopGrid1280` | `columns 3`（RESPONSIVE_CONTRACT.json desktop.breakpoints.medium） |
| Grid 更小 | `cards.desktopGridSmall` | `columns 2`（compact） |
| Grid testId | `cards.testIds.grid / .list / .viewToggle` | `pdig.card.grid` / `pdig.card.list` / `pdig.card.viewToggle` |
| 卡项 testId | 本契约新增 | `pdig.card.card` |
| Compact list 行高 | RESPONSIVE_CONTRACT.json rules「row 40-48」 | `40 – 48` |
| 页面留白 | `LAYOUT_CONTRACT.json desktopShell.pagePaddingPx` | `24`（24–32） |
| 内容最大宽 | `RESPONSIVE_CONTRACT.json desktop.contentMaxWidth` | `1400` |
| 触控 | `DESIGN_TOKENS.json components.touchTarget` | Android 48 / iOS 44 |

- 下拉/过滤器行间距：`DESIGN_TOKENS.json spacing.gridGap 16` / `gridGapWide 20`。

## 4. 视觉

| 元素 | token 键 | token 值 |
| --- | --- | --- |
| 画布 | `semantic.env.background = colors.canvas` | `#061225` |
| 面板/过滤器条 | `semantic.data.panel / .panelRaised` | `#0B1A33` / `#102340` |
| 分隔 | `semantic.data.divider = colors.borderSubtle` | `rgba(148,180,234,0.13)` |
| 页面标题 | `typography.pageTitle`（26/700/1.25）；分区 `typography.sectionTitle` | DESIGN_TOKENS.json |
| 状态徽标 | `semantic.status.ok / .warning / .critical / .unknown`（三通道） | `#3BD49B` / `#F4B64B` / `#FA626B` / `#8C9AB5` |
| 激活筛选 | `colors.primary` / `colors.primaryBright` | `#4D74FF` / `#67A7FF` |
| 聚焦 ring | `semantic.focus` | `#67A7FF` |

- 卡面视觉见 components/asset-card.md（procedural/preset 本地素材，禁远程图，mask last4）。

## 5. 交互/状态

- viewToggle（`pdig.card.viewToggle`）在 visual-grid ↔ compact-list 切换（LAYOUT_CONTRACT.json `cards.alternate`）。
- 卡项 → 卡详情（`/infrastructure/cards/{id}`，identity 优先）→（可选）定制（`/infrastructure/cards/{id}/customize`）（INTERACTION_CONTRACT.md §6）。
- 过滤即时生效；`即将到期`过滤基于 `expiry` 时间桶（copy-zh.json `timelineBuckets.7d / 30d / overdue`）。
- 键盘：Tab 到达过滤器与网格；方向键在网格内移动 + Enter 进入详情（INTERACTION_CONTRACT.md §5）。
- 动画：视图切换 `navigationSwitchMs = 200`、状态 `statusChangeMs = 180`（MOTION_CONTRACT.json）；无装饰循环动画。

## 6. 无障碍与隐私

- 键盘完整可达（过滤器/网格/切换）；focus ring 可见；Arrow 网格导航。
- 触控 ≥ 44/48；对比度 ≥ `contrast.bodyTextMin 4.5:1`。
- 隐私：last4 蒙版默认开（`privacyMask.maskCardLast4`）；Privacy Mask 全局开启时卡项完全遮蔽敏感位（`privacy.maskSensitive`）。

## 7. 验收自检

- [ ] testId 存在：`pdig.card.grid`、`pdig.card.list`、`pdig.card.viewToggle`、`pdig.card.card`；导航 `pdig.nav.infra.cards`。
- [ ] 几何符合：4 列 @1920（ratio 1.586、gap 16–20）、3 列 @1280、2 列更小；list 行高 40–48；maxWidth 1400。
- [ ] 6 类过滤维度可用且与路由 query 同步。
- [ ] 缺失字段只显示未设置/未知；last4 masked；无远程素材。
- [ ] 文案来自 copy-zh 或标注「新增 copy key：uiVNext.*」。