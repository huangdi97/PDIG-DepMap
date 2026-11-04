# Region Node — 地区锚点（Globe 节点 + Region List 数据源）

> Visual Contract · spec/ui-vnext · 2026-09-29 · No-Vision 模式（实现以此契约为准，非参考图）

## 1. 职责与位置

- **职责**：Globe 舞台上的地区锚点（region node），是 `RegionPresentation` 的可视化呈现；同一数据源驱动非视觉替代 **Region List**（INTERACTION_CONTRACT.md §4、IA.md §7）。
- **使用位置**：Infrastructure Overview 的 globe stage（`pdig.globe.stage`）与 Now 页 globe-context（`pdig.now.globe`）；Region List 挂载于 stage 旁/下。
- **三态**：`hover / active / quiet`（INTERACTION_CONTRACT.md §2、§3）。
- **边界**：锚点是导航/摘要节点，**不是卡片**（VISUAL_DNA.md §6「卡片只给可以独立操作的对象」）；不创建任何 Dependency / Proposal。

## 2. 数据契约

| 字段 | 键名 | 来源 | 说明 |
| --- | --- | --- | --- |
| 数据模型 | `RegionPresentation { regionCode, displayName, latitude, longitude, cardCount, phoneCount, accountCount, serviceCount, attentionCount }` | IA.md §5 | 值只由现有真实数据计算 |
| 无数据规则 | 无数据 = 不显示 / 0 | IA.md §5、VISUAL_DNA.md §9 | **禁止伪造节点充数**（如无 UK 数据不得虚构 UK 节点） |
| 分组 | 中国大陆 / 港澳 / 欧洲 / 北美 / 东南亚 / 自定义 | IA.md §5 | presentation only，不改 node 实际 region |
| 锚点直径 | `DESIGN_TOKENS.json globe.regionAnchorDiameter = 14` / `globe.regionAnchorActiveDiameter = 22` | DESIGN_TOKENS.json | active 态视觉放大 |
| 聚焦 state ring | `semantic.focus = colors.primaryBright` | DESIGN_TOKENS.json | 键盘/点击聚焦 |

- **建议文案**（不修改 copy-zh.json）：
  - Tooltip（name + counts，INTERACTION_CONTRACT.md §2）：新增 copy key `uiVNext.region.tooltip = "{displayName}，{cardCount} 张卡，{phoneCount} 个号码"`。
  - Region List 行（INTERACTION_CONTRACT.md §4 示例「香港，2 张卡，1 个号码」）：新增 copy key `uiVNext.region.listRow = "{displayName}，{cardCount} 张卡，{phoneCount} 个号码"`。
  - `attentionCount > 0` 时锚点追加注意徽标：复用 copy-zh.json `timelineBuckets.attention`（需要你处理）。

## 3. 几何

| 项 | 键名 | 数值 |
| --- | --- | --- |
| 锚点直径 | `globe.regionAnchorDiameter`（DESIGN_TOKENS.json） | `14` |
| 锚点 active 直径 | `globe.regionAnchorActiveDiameter` | `22` |
| 触控热区 | `components.touchTarget.android / .ios` | `48 / 44`（透明扩展热区，视觉直径不变） |
| Region List 行高 | RESPONSIVE_CONTRACT.json rules「desktop densities: row 40-48」 | `40 – 48` |
| Tooltip | 半径 `radius.md = 10`、间距 `spacing.md = 12`（DESIGN_TOKENS.json） | 跟随锚点 |

## 4. 视觉

| 状态 | token 键 | token 值 |
| --- | --- | --- |
| hover / active | `colors.regionNodeHi` | `#67A7FF` |
| quiet | `colors.regionNodeLo` | `rgba(103,167,255,0.35)` |
| Tooltip 面板 | `semantic.data.panelRaised = colors.surfaceRaised` | `#102340` |
| Tooltip 边框 | `semantic.data.divider = colors.borderSubtle` | `rgba(148,180,234,0.13)` |
| Tooltip 文字 | `semantic.ink.strong = colors.textPrimary` / `semantic.ink.regular = colors.textSecondary` | `#F4F7FF` / `#A9B8D5` |
| 地区名称标签（active 常显） | `typography.displayGlobe`（15 / 600 / 1.3） | DESIGN_TOKENS.json |
| Tooltip/计数文字 | `typography.meta`（12 / 400 / 1.4）或 `typography.secondary`（13 / 400 / 1.45） | DESIGN_TOKENS.json |
| 注意徽标（attentionCount>0） | `semantic.status.warning / .critical` | `#F4B64B` / `#FA626B` |

- 状态永不只靠颜色：徽标必须 icon / shape + label + color 三通道（VISUAL_DNA.md §4 L5、INTERACTION_CONTRACT.md §9）。

## 5. 交互/状态

- 状态机（INTERACTION_CONTRACT.md §1）段位：
  - `hover` → `REGION_HOVER`（高亮 + tooltip）；
  - `click` → `REGION_SELECTED`（focus/flyTo + active + 其它锚点降强调 + region filter + 上下文抽屉）——聚焦副作用 5 条见 INTERACTION_CONTRACT.md §3；
  - `click(selected)` → `REGION_DETAIL`（全屏 Region Drawer）；
  - `Escape` 逐级回退；`click(empty)` → `GLOBAL`。
- Region List 与锚点等价：hover/选中行 = 对应锚点高亮；Enter/Space = click（INTERACTION_CONTRACT.md §4、§5）。
- 动画：聚焦 `tokens.globeFocusMs 620`（MOTION_CONTRACT.json）；状态切换 `microInteractions.statusChangeMs 180`；reduce motion 下简化（`reduceMotion.cameraAnimation` translate-only）。

## 6. 无障碍与隐私

- **非视觉替代**：Region List 键盘可达、screen reader 可读（INTERACTION_CONTRACT.md §4、IA.md §7），行文案「香港，2 张卡，1 个号码」；不允许「只能点地球」。
- **键盘**：Tab/Shift+Tab 进入 region list；Arrow 行导航；Enter/Space 激活；Escape 回退（§5）。
- **触控**：热区 ≥ `components.touchTarget`（Android 48 / iOS 44）；视觉锚点保持 14/22。
- **隐私**：锚点只展示地区坐标与聚合计数；Privacy Mask（`privacyMask.maskAccountNames` 等，默认 true）开启时所有敏感对象名不进入 tooltip / 标签；计数摘要不含明细。

## 7. 验收自检

- [ ] testId 存在：`pdig.globe.region`（锚点）、`pdig.region.list` / `pdig.region.item`（Region List 行）、聚焦 ring 有 `semantic.focus`。
- [ ] 几何符合：锚点 14 → active 22；热区 ≥44/48；Region List 行高 40–48。
- [ ] 三态（hover/active/quiet）可区分且不依赖颜色单通道。
- [ ] 无数据地区不渲染节点、计数为 0（无伪造）。
- [ ] tooltip = name + counts；文案来自 copy-zh 或标注「新增 copy key：uiVNext.*」。
- [ ] Region List 等价驱动 filter/drawer（非唯一导航）。