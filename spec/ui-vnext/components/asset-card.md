# Asset Card — 卡面（可独立操作对象卡 / custom card face）

> Visual Contract · spec/ui-vnext · 2026-09-29 · No-Vision 模式（实现以此契约为准，非参考图）

## 1. 职责与位置

- **职责**：LAYER L4 Asset Identity 的「卡片」——只有**可以独立操作的对象**才是卡（VISUAL_DNA.md §6、§4 L4）。卡面 = custom card face（可定制主题/背景/材质/主色/布局/昵称/logo/last4/network/region/currency/status 显隐）。
- **使用位置**：Infrastructure Cards（`/infrastructure/cards`）grid 与 list 视图；Card Detail（`/infrastructure/cards/{id}`）身份列。
- **边界**：卡片不承载操作命令流（操作在详情页）；数据缺失只显示「未设置 / 未知」，**禁止推断**（goal §12）。

## 2. 数据契约

卡信息模型（goal §12）：

| 字段 | 说明 | 缺失规则 |
| --- | --- | --- |
| `nickname / issuer / last4 / region / currency / type / physical|virtual / expiry / status` | type：储蓄/信用；physical|virtual：实体/虚拟 | 缺失 = 未设置 / 未知，禁止推断 |

呈现配置 = `PresentationProfile`（PRESENTATION_PROFILE_SCHEMA.json，仅本地偏好，绝不写入 .depmap）：

| 属性 | 键名 | 允许值 |
| --- | --- | --- |
| 目标 | `targetType = "card"`、`targetId` | schema 枚举 |
| 主题 | `themeId` | `deep-space / minimal / region / city / glass / metal / abstract`（与其他 preset 共用） |
| 材质 | `material` | `glass / metal / matte / paper / none` |
| 主色 | `accentColor` | hex-color（schema 格式） |
| 背景 | `background.kind / background.value` | `preset / gradient / procedural / bundled-image / user-image` |
| 布局 | `layout` | `standard / minimal / dense / editorial` |
| 隐私 | `privacy.maskSensitive / maskLast4 / maskNumber` | 默认 `maskSensitive: true` |
| 显隐 | `visibleFields`、`flags.showLogo / showNetwork / showRegion / showCurrency / showStatus` | 空数组 = 默认集 |

- **建议文案**（不修改 copy-zh.json）：新增 copy key `uiVNext.notSet = 未设置`、`uiVNext.unknown = 未知`、`uiVNext.card.type.credit = 信用`、`uiVNext.card.type.debit = 储蓄`、`uiVNext.card.physical = 实体`、`uiVNext.card.virtual = 虚拟`；过期状态可复用 copy-zh.json `timelineBuckets.overdue`（已逾期）。

## 3. 几何

| 项 | 键名 | 数值 |
| --- | --- | --- |
| Grid @1920 | `LAYOUT_CONTRACT.json cards.desktopGrid1920` | `columns 4、gapPx {min:16,max:20}、cardAspectRatio 1.586`（DESIGN_TOKENS.json `components.cardsGrid` 同值） |
| Grid @1280 | `cards.desktopGrid1280` | `columns 3` |
| Grid 更小 | `cards.desktopGridSmall` | `columns 2` |
| Grid 默认视图 | `cards.gridDefault` / `cards.alternate` | `visual-grid` / `compact-list` |
| Grid testId | `cards.testIds.grid / .list / .viewToggle` | `pdig.card.grid` / `pdig.card.list` / `pdig.card.viewToggle` |
| 卡项 testId | 本契约新增 | `pdig.card.card` |
| Compact list 行高 | RESPONSIVE_CONTRACT.json rules「row 40-48」 | `40 – 48` |
| 卡面圆角 | `DESIGN_TOKENS.json radius.lg` | `14` |
| 内容间距 | `spacing.md = 12` / `spacing.lg = 16`；网格间距 token `gridGap 16 / gridGapWide 20` | DESIGN_TOKENS.json |

- 宽屏内容最大宽：`DESIGN_TOKENS.json components.page.maxWidth = 1400`。

## 4. 视觉

| 元素 | token 键 | token 值 |
| --- | --- | --- |
| 卡面背景 | `background.kind` 限 preset/gradient/procedural/bundled-image（**禁远程 URL**，policy `noRemoteImages = true`） | — |
| 默认主色 accent | `colors.primary` | `#4D74FF`（永不铺满，VISUAL_DNA.md §5） |
| 卡片昵称 / last4 | `semantic.ink.strong = colors.textPrimary` | `#F4F7FF` |
| meta（network/region/currency） | `semantic.ink.muted = colors.textMuted`；`typography.meta`（12/400/1.4） | `#7383A3` |
| 状态徽标 | `semantic.status.ok / .warning / .critical / .unknown` | `#3BD49B` / `#F4B64B` / `#FA626B` / `#8C9AB5` |
| Chip 圆角 | `radius.sm` | `6` |
| 边框分隔 | `semantic.data.divider = colors.borderSubtle` | `rgba(148,180,234,0.13)` |

- 状态徽标 = icon + label + color 三通道（VISUAL_DNA.md §4 L5）；last4 蒙版遵循 `DESIGN_TOKENS.json privacyMask.maskCardLast4 = true`。

## 5. 交互/状态

- 网格项 → 卡详情（`/infrastructure/cards/{id}`，identity 优先）→（可选）定制工作室（`/infrastructure/cards/{id}/customize`）（INTERACTION_CONTRACT.md §6）。
- `pdig.card.viewToggle` 在 visual-grid ↔ compact-list 间切换（LAYOUT_CONTRACT.json `cards.alternate`）。
- hover/focus 反馈：聚焦 ring = `semantic.focus`；动画 ≤ `MOTION_CONTRACT.json tokens.fastMs.applied 140`。
- disabled 卡操作必须附原因（INTERACTION_CONTRACT.md §9）。
- 过滤/视图状态与路由 query 同步（如 `?region=HK`，INTERACTION_CONTRACT.md §6）。

## 6. 无障碍与隐私

- 键盘：网格 Tab 导航 + Enter 进入详情；focus ring 可见（`semantic.focus`）。
- 触控：热区 ≥ `components.touchTarget`（Android 48 / iOS 44）。
- 对比度：`contrast.bodyTextMin 4.5:1` / `contrast.largeTextMin 3:1`。
- 隐私：last4 默认蒙版（`privacyMask.maskCardLast4`）；Privacy Mask 全局开启时 `privacy.maskSensitive` 生效，demo/screenshot 无真实卡信息。

## 7. 验收自检

- [ ] testId 存在：`pdig.card.grid`、`pdig.card.list`、`pdig.card.viewToggle`、`pdig.card.card`。
- [ ] 几何符合：4 列 @1920（gap 16–20、ratio 1.586）、3 列 @1280、2 列更小；list 行高 40–48。
- [ ] 缺数据只显示未设置/未知（不推断）。
- [ ] 无远程素材（preset/procedural/bundled 本地）。
- [ ] last4 蒙版默认开；状态三通道。
- [ ] 文案来自 copy-zh 或标注「新增 copy key：uiVNext.*」。