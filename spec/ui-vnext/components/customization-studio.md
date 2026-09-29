# Customization Studio — 卡面/号码面定制工作室（三栏 + Mobile bottom-sheet）

> Visual Contract · spec/ui-vnext · 2026-09-29 · No-Vision 模式（实现以此契约为准，非参考图）

## 1. 职责与位置

- **职责**：LAYER L4 Asset Identity 的定制工作台——用户编辑一张卡/一个号码的**呈现层配置**（PresentationProfile），实时预览。Desktop 三栏（Asset Library / Live Preview / Property Inspector）；Mobile preview 在上 + bottom-sheet 编辑在下（LAYOUT_CONTRACT.json `customization.mobile`、INTERACTION_CONTRACT.md §8）。
- **使用位置**：Card Customization（`/infrastructure/cards/{id}/customize`）与 Number Customization（`/infrastructure/numbers/{id}/customize`），共享同一组件（IA.md §4）。
- **铁律**：编辑对象 = PresentationProfile（**本地 preference**，goal §13），**绝不写入 .depmap frozen payload**、不改 Schema v4 Canonical、不改变 node identity / dependencies / evidence / confirmation（PRESENTATION_PROFILE_SCHEMA.json `$comment`、DESIGN_TOKENS.json policy `privacyRule`）。

## 2. 数据契约

编辑对象 = `PresentationProfile`（PRESENTATION_PROFILE_SCHEMA.json）：

| 属性 | 键名 | 允许值 |
| --- | --- | --- |
| 目标 | `targetType` / `targetId` | `node | card | phoneNumber`；本地 node id（可 mask） |
| 主题 | `themeId` | `deep-space / minimal / region / city / glass / metal / abstract`（card）与 `country / city / minimal / banking / travel / recovery / work / private`（number） |
| 材质 | `material` | `glass / metal / matte / paper / none` |
| 主色 | `accentColor` | hex-color（schema 格式，渲染前校验） |
| 背景 | `background.kind / background.value` | `preset / gradient / procedural / bundled-image / user-image` |
| 布局 | `layout` | `standard / minimal / dense / editorial` |
| 隐私 | `privacy.maskSensitive / maskLast4 / maskNumber` | 默认 `maskSensitive: true` |
| 显隐 | `visibleFields`；`flags.showLogo / showNetwork / showRegion / showCurrency / showStatus / showCarrier / showSimBadge / showRole / showUsageTags / showCountryFlagForNumber` | 空数组 = 默认集 |

- 素材策略：bundled local / procedural；`user-image` 仅本地导入；**禁远程 URL**（goal §43、DESIGN_TOKENS.json policy `noRemoteImages / noRemoteIconCdn`）。
- **preset visual ≠ semantic role**（goal §12）：选择「Banking」主题 ≠ 声明银行角色；主题只改呈现。
- **建议文案**（不修改 copy-zh.json）：新增 copy key `uiVNext.customization.title = 定制工作室`、`uiVNext.customization.library = 素材库`、`uiVNext.customization.preview = 预览`、`uiVNext.customization.inspector = 属性`、`uiVNext.customization.reset = 重置为默认`、`uiVNext.customization.presetNote = 主题只影响外观，不影响实际角色`；保存沿用 copy-zh.json `common.save`（保存）/ `common.done`（完成）。

## 3. 几何

| 项 | 键名 | 数值 |
| --- | --- | --- |
| 三栏占比 | `LAYOUT_CONTRACT.json customization.desktopColumns`（DESIGN_TOKENS.json `components.customization` 同值） | Asset Library `0.22` / Live Preview `0.46` / Property Inspector `0.32` |
| 三栏 testId | `customization.desktopColumns[].testId` | `pdig.customization.library` / `pdig.customization.preview` / `pdig.customization.inspector` |
| Mobile | `customization.mobile` | `"preview on top, bottom-sheet editor below"`（不复制 Desktop inspector，INTERACTION_CONTRACT.md §8） |
| 预览区卡面 | 遵循 asset-card 几何（`cards.desktopGrid1920.cardAspectRatio 1.586`、`radius.lg 14`） | LAYOUT_CONTRACT.json / DESIGN_TOKENS.json |
| 分栏间距 | `desktopShell.mainGapPx 20`（范围 16–24） | LAYOUT_CONTRACT.json |
| 触控 | `components.touchTarget` | Android 48 / iOS 44 |

## 4. 视觉

| 元素 | token 键 | token 值 |
| --- | --- | --- |
| 三栏面板 | `semantic.data.panel / .panelRaised` | `#0B1A33` / `#102340` |
| 预览背景 | `semantic.env.background = colors.canvas` / `colors.canvasDeep` | `#061225` / `#030A18` |
| 分隔 | `semantic.data.divider / .dividerStrong` | `rgba(148,180,234,0.13)` / `rgba(116,161,255,0.34)` |
| 选中项高亮 | `colors.primary` / `colors.primaryBright` | `#4D74FF` / `#67A7FF` |
| 文字 | `semantic.ink.strong / .regular / .muted` | `#F4F7FF` / `#A9B8D5` / `#7383A3` |
| 主色选择器 | `accentColor`（用户 hex，渲染前校验；默认回落到 `colors.primary`） | PRESENTATION_PROFILE_SCHEMA.json |
| 材质视觉 | `material` → 本地程序化材质渲染（glass/metal/matte/paper/none） | 无远程贴图 |
| 聚焦 ring | `semantic.focus` | `#67A7FF` |

- 预览中 last4 / 号码 / 账户名遵循 `privacyMask.*` 蒙版（默认 true），不再重复铺色。

## 5. 交互/状态

- 三栏联动（INTERACTION_CONTRACT.md §8）：选素材 / 改属性 → Live Preview 实时更新；编辑对象 = PresentationProfile，保存 = 本地 preference 写入。
- 保存反馈：完成 toast / inline 确认（copy-zh.json `common.done`）；错误 = danger 软底 + 人话 + 重试（`common.retry`）（INTERACTION_CONTRACT.md §9）。
- 破坏性动作（重置主题/移除素材）需显式确认（§9；copy-zh.json `common.cancel` 取消 / `common.confirm` 确认）。
- Mobile bottom-sheet：打开 `microInteractions.drawerOpenMs = 200`；reduce motion 下 instant appear（MOTION_CONTRACT.json `reduceMotion.drawer`）。
- 属性即时反馈动画 ≤ `tokens.fastMs`（140ms）；无装饰循环动画。

## 6. 无障碍与隐私

- 键盘：三栏均可 Tab 到达；焦点 ring 可见（`semantic.focus`）；预览区可朗读。
- 触控 ≥ 44/48；对比度 ≥ `contrast.bodyTextMin 4.5:1`。
- 隐私：预览与编辑始终蒙版（`privacyMask.*`、`privacy.maskSensitive`）；`user-image` 只读本地导入，不引入网络。
- 结果绝不写入 .depmap（PRESENTATION_PROFILE_SCHEMA.json `$comment`、DESIGN_TOKENS policy `privacyRule`）。

## 7. 验收自检

- [ ] testId 存在：`pdig.customization.library`、`pdig.customization.preview`、`pdig.customization.inspector`。
- [ ] 几何符合：0.22 / 0.46 / 0.32 三栏；Mobile preview-top + bottom-sheet。
- [ ] 编辑对象 = PresentationProfile，保存 = 本地 preference，不写 .depmap。
- [ ] 素材 bundled/procedural，无远程 URL（含 user-image 仅本地）。
- [ ] preset visual ≠ semantic role（主题不改变领域语义）。
- [ ] preview 蒙版生效；文案来自 copy-zh 或标注「新增 copy key：uiVNext.*」。