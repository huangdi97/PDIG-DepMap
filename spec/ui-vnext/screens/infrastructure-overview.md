# Infrastructure Overview — 基础设施总览（/infrastructure）

> Visual Contract · spec/ui-vnext · 2026-09-29 · No-Vision 模式（实现以此契约为准，非参考图）

## 1. 职责与位置

- **职责**：基础设施默认焦点页（IA.md §8）——global globe（55–65% 宽 × 65–78% 高，**视觉主导**）+ region nodes + 区域资产摘要 + 右活动轨 + 底部快速入口；点地区 → focus + filter + drawer（查看全部 / 查看卡片 / 查看号码）。
- **路由**：`/infrastructure`（IA.md §4）；二级导航 `pdig.nav.infra.overview`（高优先级，IA.md §3）。
- **边界**：Globe 是导航增强 + 变化理解入口（goal §11），不是装饰也不是唯一导航。

## 2. 数据契约

| 块 | 数据 | testId | 组件契约 |
| --- | --- | --- | --- |
| Globe Stage | `RegionPresentation` 聚合（IA.md §5）+ 真实关系弧线（`globe.arcCountPolicy`） | `pdig.globe.stage`（canvas `pdig.globe.canvas`） | components/globe.md |
| Region List（非视觉替代） | 地区行（displayName + counts） | `pdig.region.list` / `pdig.region.item` | components/region-node.md |
| 右活动轨 | 近期活动 / attention 摘要（复用 attention-row） | `pdig.overview.activity` | components/attention-row.md |
| 底部快速入口 | 查看卡片 / 查看号码 / 查看薄弱点 | `pdig.overview.quick` | IA.md §4 路由 |

- RegionPresentation：`regionCode / displayName / latitude / longitude / cardCount / phoneCount / accountCount / serviceCount / attentionCount`；无数据 = 不显示 / 0（IA.md §5）。
- **建议文案**（不修改 copy-zh.json）：新增 copy key `uiVNext.overview.title = 基础设施`、`uiVNext.overview.quick.cards = 查看卡片`、`uiVNext.overview.quick.numbers = 查看号码`、`uiVNext.overview.quick.weaknesses = 查看薄弱点`；复用 `uiuxV031.myInfrastructure`（我的基础设施）、`uiuxV031.goToFindings`（查看薄弱点）、`uiuxV031.goToInfrastructure`（查看基础设施）。

## 3. 几何

引用 `LAYOUT_CONTRACT.json overview1920`（DESIGN_TOKENS.json `components.overview` 同值）：

| 块 | 键名 | 数值 |
| --- | --- | --- |
| Globe Stage 宽占比 | `globeStage.widthRatio` | `{min:0.55, max:0.65}` |
| Globe Stage 高占比 | `globeStage.heightRatio` | `{min:0.65, max:0.78}`（视觉主导） |
| Globe Stage 最小尺寸 | `globeStage.minWidth / minHeight` | `860 × 650` |
| Globe Stage testId | `globeStage.testId` | `pdig.globe.stage` |
| 右活动轨宽 | `activityRail.widthPx` | `{min:300, max:380}` |
| 右活动轨 testId | `activityRail.testId` | `pdig.overview.activity` |
| 底部快速入口高 | `quickEntry.heightPx` | `{min:88, max:120}` |
| 底部快速入口 testId | `quickEntry.testId` | `pdig.overview.quick` |
| 页面留白 / 主间距 | `desktopShell.pagePaddingPx 24`（24–32）/ `mainGapPx 20`（16–24） | LAYOUT_CONTRACT.json |

## 4. 视觉

| 元素 | token 键 | token 值 |
| --- | --- | --- |
| 深空画布 | `semantic.env.background = colors.canvas` / `colors.canvasDeep` | `#061225` / `#030A18` |
| 面板（活动轨/快速入口） | `semantic.data.panel / .panelRaised` | `#0B1A33` / `#102340` |
| 分隔 | `semantic.data.divider = colors.borderSubtle` | `rgba(148,180,234,0.13)` |
| 锚点 | `colors.regionNodeHi` / `colors.regionNodeLo` | `#67A7FF` / `rgba(103,167,255,0.35)` |
| 弧线 | `colors.arcActive` / `colors.arcQuiet` | `rgba(103,167,255,0.55)` / `rgba(115,131,163,0.28)` |
| 大气 | `colors.atmosphereInner → atmosphereOuter` | `rgba(77,116,255,0.18) → rgba(6,18,37,0.0)` |
| 页面标题 | `typography.pageTitle`（26/700/1.25）；标签 `typography.displayGlobe`（15/600/1.3） | DESIGN_TOKENS.json |
| 状态 | `semantic.status.*`（三通道） | `#3BD49B` / `#F4B64B` / `#FA626B` / `#8C9AB5` |
| 聚焦 ring | `semantic.focus` | `#67A7FF` |

- 数据可读层（L3–L5）不玻璃化（VISUAL_DNA.md §4 总规则）。

## 5. 交互/状态

- 点地区锚点 → `REGION_SELECTED`：focus/flyTo（620ms）+ 区域过滤 `region=xx` + 上下文抽屉（Cards/Numbers/Accounts/Services 摘要 + 查看全部/查看卡片/查看号码）（INTERACTION_CONTRACT.md §3）。
- 抽屉动作路由：查看卡片 → `/infrastructure/cards?region=HK`（带过滤状态，§6）；查看号码 → `/infrastructure/numbers?region=HK`；查看全部 → `/infrastructure?region=HK`。
- Region List 等价驱动（§4）；Globe 四级状态机 + Escape 回退（§1）。
- 快速入口：底部 `pdig.overview.quick` 直达 cards / numbers / weaknesses。
- 动画：globe focus 620ms；画布交互后 idle rotation 暂停（MOTION_CONTRACT.json `globe`）；截图冻结相机状态。

## 6. 无障碍与隐私

- 非视觉替代 Region List（键盘可达、screen reader 可读）；不允许「只能点地球」。
- 键盘：Tab 焦点循环；Arrow（region list / globe 视角微调）；Enter/Space；Escape（INTERACTION_CONTRACT.md §5）。
- 触控 ≥ 44/48（globe 锚点热区透明扩大）；对比度 ≥ 4.5:1。
- 隐私：只渲染聚合计数与地区坐标；Privacy Mask 下账户/号码明细遮蔽（`privacyMask.*`）。

## 7. 验收自检

- [ ] testId 存在：`pdig.globe.stage`、`pdig.globe.canvas`、`pdig.globe.region`、`pdig.region.list` / `pdig.region.item`、`pdig.overview.activity`、`pdig.overview.quick`。
- [ ] 几何符合：globe 55–65% 宽 × 65–78% 高（≥860×650）；活动轨 300–380；快速入口 88–120。
- [ ] 点地区 → focus + filter + drawer 三动作完整；抽屉路由带 `?region=` 过滤。
- [ ] 无远程资源（tiles/texture/terrain/CDN）；弧线仅真实关系或地区摘要。
- [ ] 无真实数据；mask 生效；文案来自 copy-zh 或标注「新增 copy key：uiVNext.*」。