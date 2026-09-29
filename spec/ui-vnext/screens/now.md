# Now — 现在（/now）

> Visual Contract · spec/ui-vnext · 2026-09-29 · No-Vision 模式（实现以此契约为准，非参考图）

## 1. 职责与位置

- **职责**：回答「现在最值得处理什么」——Globe context（受控摘要）+ Need Attention + Active Changes + Upcoming + optional region quick access（IA.md §1 一级第 1 项、goal §12）。
- **路由**：`/now`（IA.md §4）。启动（有数据）时默认 now 优先（IA.md §8）；有 P0 风险时 now 直接呈现 attention。
- **禁止**：8 个统计卡行（`LAYOUT_CONTRACT.json now.forbidden = ["8-stat-card-row"]`）；**不显示 0 issues / 安全分**（VISUAL_DNA.md §9、copy-zh.json hardRules「Readiness 禁止 safe / 100% / all clear」）。

## 2. 数据契约

块结构（`LAYOUT_CONTRACT.json now.blocks`，顺序固定）：

| 块 | testId | 数据/内容 | 组件契约 |
| --- | --- | --- | --- |
| globe-context | `pdig.now.globe` | Globe 受控摘要（区域计数，聚合） | components/globe.md |
| need-attention | `pdig.now.attention` | Need Attention 行（含行项 `pdig.now.attention.item`） | components/attention-row.md |
| active-changes | `pdig.now.changes` | 进行中变更摘要（continuity rail 摘要） | components/continuity-view.md |
| upcoming | `pdig.now.upcoming` | 即将发生（时间桶） | copy-zh.json `timelineBuckets.*` |
| region-quick-access | 可选 | 地区快捷入口（Region List 形态） | components/region-node.md |

healthy 状态表达（copy-zh.json `uiuxV031`，**不显示 0 issues/安全分/全绿**）：

- `uiuxV031.healthyTitle` = 当前没有需要立即处理的事项；
- `uiuxV031.healthyScope` = 最近一次检查覆盖了这些范围；
- `uiuxV031.healthyUnknown` = 以下范围仍然未知；
- `uiuxV031.healthySuggest` = 可以主动准备的场景。

时间桶文案：`timelineBuckets.attention`（需要你处理）/ `overdue`（已逾期）/ `today`（今天）/ `7d`（7 天内）/ `30d`（30 天内）/ `90d`（90 天内）/ `later`（以后）。

## 3. 几何

| 项 | 键名 | 数值 |
| --- | --- | --- |
| 页面留白 | `LAYOUT_CONTRACT.json desktopShell.pagePaddingPx` | `24`（范围 24–32） |
| 主内容间距 | `desktopShell.mainGapPx` | `20`（范围 16–24） |
| Globe context 尺寸 | 收敛为 context 卡：上限参考 `overview1920.globeStage` 数值（55–65% 宽 × 65–78% 高为上限，非必须占满） | LAYOUT_CONTRACT.json |
| 内容最大宽 | `DESIGN_TOKENS.json components.page.maxWidth` | `1400` |
| Attention/Changes 行高 | RESPONSIVE_CONTRACT.json rules「row 40-48」 | `40 – 48` |
| 触控热区 | `DESIGN_TOKENS.json components.touchTarget` | Android 48 / iOS 44 |

## 4. 视觉

| 元素 | token 键 | token 值 |
| --- | --- | --- |
| 画布 | `semantic.env.background = colors.canvas` / `colors.canvasDeep` | `#061225` / `#030A18` |
| 面板 | `semantic.data.panel / .panelRaised` | `#0B1A33` / `#102340` |
| 分隔 | `semantic.data.divider = colors.borderSubtle` | `rgba(148,180,234,0.13)` |
| 页面标题 | `typography.pageTitle`（26/700/1.25）+ 分区标题 `typography.sectionTitle`（15/600/1.4） | DESIGN_TOKENS.json |
| 正文 | `typography.body`（14/400/1.5）`semantic.ink.regular` | `#A9B8D5` |
| 状态 | `semantic.status.ok / .warning / .critical / .unknown`（三通道） | `#3BD49B` / `#F4B64B` / `#FA626B` / `#8C9AB5` |
| 聚焦 ring | `semantic.focus = colors.primaryBright` | `#67A7FF` |

- 健康态面板不得用「通关绿」全屏强调；unknown 范围用 `semantic.status.unknown`（#8C9AB5）如实呈现。

## 5. 交互/状态

- Attention 行点击 → 真路由（详情 `/infrastructure/cards/{id}`、`/infrastructure/numbers/{id}` 或场景 `/change/phone`、`/change/card`）（INTERACTION_CONTRACT.md §6）。
- Globe context 复用 Globe 状态机（GLOBAL → REGION_HOVER → REGION_SELECTED → REGION_DETAIL，Escape 回退）；点地区 → 区域过滤 + 上下文抽屉（INTERACTION_CONTRACT.md §1–§3）。
- `Ctrl/Cmd+K` 打开命令条（INTERACTION_CONTRACT.md §5）；Region List 为非视觉替代（§4）。
- 动画：`navigationSwitchMs 200`、状态 `statusChangeMs 180`（MOTION_CONTRACT.json）；reduce motion 全静态。

## 6. 无障碍与隐私

- 键盘：Tab/Shift+Tab 焦点循环（rail → top bar → 内容 → region list）；Arrow 行导航；Enter/Space 激活；Escape 回退（INTERACTION_CONTRACT.md §5）；focus ring = `semantic.focus`。
- 触控 ≥ `components.touchTarget`（44/48）。
- 对比度 ≥ `contrast.bodyTextMin 4.5:1`。
- 隐私：attention 行的对象名/号码、globe 明细全部按 `privacyMask.*` 蒙版（默认 true）；healthy 表达不含「0 问题」欺诈性表述。

## 7. 验收自检

- [ ] testId 存在：`pdig.now.globe`、`pdig.now.attention`、`pdig.now.attention.item`、`pdig.now.changes`、`pdig.now.upcoming`。
- [ ] 禁止 8 统计卡行；无 0 issues / 安全分表述；healthy 用「检查范围 + 未知范围」。
- [ ] 块顺序 = `now.blocks`（globe-context / need-attention / active-changes / upcoming / region-quick-access）。
- [ ] 几何符合：padding 24、gap 20、maxWidth 1400、行高 40–48。
- [ ] 无远程资源；无真实数据；mask 生效。
- [ ] 文案来自 copy-zh（`uiuxV031.*`、`timelineBuckets.*`、`impact.*`）或标注「新增 copy key：uiVNext.*」。