# Attention Row — Need Attention 行（icon + label + color 三通道）

> Visual Contract · spec/ui-vnext · 2026-09-29 · No-Vision 模式（实现以此契约为准，非参考图）

## 1. 职责与位置

- **职责**：Now 页 Need Attention 块的单行（LAYER L5 Status/Actions）——每行一个待处理对象（一条必须处理/建议检查/受限事项），以 **icon + label + color 三通道** 呈现状态（VISUAL_DNA.md §4 L5、INTERACTION_CONTRACT.md §9）。不是统计卡。
- **使用位置**：Now 页 `need-attention` 块（`LAYOUT_CONTRACT.json now.testIds.attention = pdig.now.attention`）；Infrastructure Overview 右活动轨（`pdig.overview.activity`）复用同一行组件。
- **边界**：行只承载「一个对象的一次注意点」；相关动作/详情走路由跳转。

## 2. 数据契约

行模型（建议新增 copy key 均标注；不修改 copy-zh.json）：

| 字段 | 说明 | 对应状态/文案 |
| --- | --- | --- |
| `kind` | 注意类型：must_change / needs_review / disabled / info | 文案（见下） |
| `target` | 目标对象（mask 后名称/号码） | Privacy Mask 下遮罩 |
| `reason` | 一句话原因 | 建议新增 `uiVNext.attention.reason.*` |
| `disabledReason` | disabled 时必须存在的原因 | copy-zh.json `uiuxV031.blockedReason`（新路径验证通过后才能执行这一步） |
| `actionRoute` | 去详情/场景的路由 | IA.md §4 真路由 |

状态 → 视觉/文案映射：

| kind | copy（spec/ui/copy-zh.json） | 语义色 token |
| --- | --- | --- |
| must_change | `impact.mustChange`（必须处理） | `semantic.status.critical`（最突出） |
| needs_review | `impact.needsReview`（建议检查） | `semantic.status.warning` |
| disabled | `uiuxV031.blockedReason`（必带原因，绝不只禁用） | `semantic.ink.muted` + 原因文案 |
| info | `timelineBuckets.attention`（需要你处理）等 | `semantic.status.ok` / `semantic.status.unknown` |

- 块标题：「需要你处理」→ copy-zh.json `home.needsAction`；「即将到来」→ `home.upcoming`（Now 页 upcoming 块沿用）。

## 3. 几何

| 项 | 键名 | 数值 |
| --- | --- | --- |
| 块容器 testId | `LAYOUT_CONTRACT.json now.testIds.attention` | `pdig.now.attention` |
| 行项 testId | 本契约新增 | `pdig.now.attention.item`（geometry probe 带 index） |
| 行高 | RESPONSIVE_CONTRACT.json rules「row 40-48」 | `40 – 48` |
| 触控热区 | `DESIGN_TOKENS.json components.touchTarget` | Android 48 / iOS 44 |
| 行内间距 | `spacing.md = 12` / `spacing.lg = 16` | DESIGN_TOKENS.json |
| 图标尺寸 | 基础对齐线 16–20（`spacing.lg 16` / `spacing.xl 20`） | DESIGN_TOKENS.json |

## 4. 视觉

| 元素 | token 键 | token 值 |
| --- | --- | --- |
| 行背景 | `semantic.data.panel` | `#0B1A33` |
| 行分隔 | `semantic.data.divider = colors.borderSubtle` | `rgba(148,180,234,0.13)` |
| must_change 强调 | `semantic.status.critical = colors.critical` | `#FA626B` |
| needs_review | `semantic.status.warning = colors.warning` | `#F4B64B` |
| muted/disabled | `semantic.ink.muted = colors.textMuted` | `#7383A3` |
| 主文字 | `semantic.ink.strong = colors.textPrimary`；`typography.secondary`（13/400/1.45） | `#F4F7FF` |
| meta | `typography.meta`（12/400/1.4）`semantic.ink.regular` | `#A9B8D5` |
| 聚焦 ring | `semantic.focus = colors.primaryBright` | `#67A7FF` |
| 圆角 | `radius.md`（行容器，可选） | `10` |

- 状态三通道：icon（shape）+ label + color；`semantic.status.unknown`（`#8C9AB5`）用于未知态；禁止纯色表达（VISUAL_DNA.md §4 L5）。

## 5. 交互/状态

- 点击行 → 动作路由：详情页（`/infrastructure/cards/{id}`、`/infrastructure/numbers/{id}`）或变更场景（`/change/phone`、`/change/card`）（INTERACTION_CONTRACT.md §6）。
- disabled 行：不可激活但**必须显示原因文案**（INTERACTION_CONTRACT.md §7、§9）——绝不只置灰。
- 键盘：Arrow 行导航 + Enter 激活（§5）；focus ring 可见。
- 行状态变化动画：`microInteractions.statusChangeMs = 180`；完成勾选 `checkPopMs = 220`（MOTION_CONTRACT.json）；reduce motion 下静态。

## 6. 无障碍与隐私

- 状态非颜色单通道（icon + label + color）；screen reader 朗读 label + reason。
- 触控 ≥ 44/48；对比度 ≥ `contrast.bodyTextMin 4.5:1`。
- 隐私：目标对象名/号码按 `privacyMask.*` 遮罩；行不出现真实卡号/号码/账户名。
- 键盘 Tab/Enter/Escape 全语义；disabled 行原因可被读屏读到。

## 7. 验收自检

- [ ] testId 存在：`pdig.now.attention`（容器）、`pdig.now.attention.item`（行项）。
- [ ] must_change 视觉最突出（critical 通道）；disabled 行必带原因文案。
- [ ] 三通道表达（icon + label + color），无纯色状态。
- [ ] 行高 40–48；触控 44/48；行点击走真路由。
- [ ] 无真实数据；mask 生效；文案来自 copy-zh 或标注「新增 copy key：uiVNext.*」。