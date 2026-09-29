# Card Customization — 卡面定制工作室（/infrastructure/cards/{id}/customize）

> Visual Contract · spec/ui-vnext · 2026-09-29 · No-Vision 模式（实现以此契约为准，非参考图）

## 1. 职责与位置

- **职责**：单卡的呈现层定制——主题 / 背景 / 材质 / 主色 / 布局 / 昵称 / logo / last4 / network / region / currency / status 显隐；Preset ≥ Minimal / Deep Space / Region / City / Glass / Metal / Abstract（goal §12）。
- **路由**：`/infrastructure/cards/{id}/customize`（IA.md §4）；复用 components/customization-studio.md 三栏组件（0.22 / 0.46 / 0.32）。
- **铁律**：编辑对象 = PresentationProfile（`targetType = "card"`），本地 preference，**绝不写入 .depmap**（goal §13、PRESENTATION_PROFILE_SCHEMA.json）；preset visual ≠ semantic role。

## 2. 数据契约

| 属性 | 键名 | 允许值/来源 |
| --- | --- | --- |
| 主题 | `themeId` | `minimal / deep-space / region / city / glass / metal / abstract`（PRESENTATION_PROFILE_SCHEMA.json 例子） |
| 材质 | `material` | `glass / metal / matte / paper / none` |
| 主色 | `accentColor` | hex-color（渲染前校验；默认回落 `colors.primary`） |
| 背景 | `background.kind / background.value` | `preset / gradient / procedural / bundled-image / user-image`（本地） |
| 布局 | `layout` | `standard / minimal / dense / editorial` |
| 昵称 | 昵称编辑（呈现层昵称） | 不改变 node 真实信息 |
| 显隐 | `visibleFields`；`flags.showLogo / showNetwork / showRegion / showCurrency / showStatus` | 空数组 = 默认集 |
| 隐私 | `privacy.maskSensitive / maskLast4` | 默认 `maskSensitive: true` |

- 素材：bundled local / procedural；**禁远程 URL**（goal §43、DESIGN_TOKENS policy `noRemoteImages`）。
- Preset 只定义视觉外观；选「Region」≠ 修改 region 数据（preset visual ≠ semantic role）。
- **建议文案**（不修改 copy-zh.json）：新增 copy key `uiVNext.customization.card.title = 卡面定制`、`uiVNext.customization.field.theme = 主题`、`uiVNext.customization.field.background = 背景`、`uiVNext.customization.field.material = 材质`、`uiVNext.customization.field.accent = 主色`、`uiVNext.customization.field.layout = 布局`、`uiVNext.customization.field.nickname = 昵称`、`uiVNext.customization.field.show = 显示项`；保存/完成/重置沿用 copy-zh.json `common.save / common.done` 与 `uiVNext.customization.reset`。

## 3. 几何

| 项 | 键名 | 数值 |
| --- | --- | --- |
| Desktop 三栏 | `LAYOUT_CONTRACT.json customization.desktopColumns` / `DESIGN_TOKENS.json components.customization` | `0.22`（library）/ `0.46`（preview）/ `0.32`（inspector） |
| 三栏 testId | `customization.desktopColumns[].testId` | `pdig.customization.library` / `pdig.customization.preview` / `pdig.customization.inspector` |
| Mobile | `customization.mobile` | `"preview on top, bottom-sheet editor below"` |
| 预览卡面 | asset-card 几何（`cardAspectRatio 1.586`、`radius.lg 14`） | LAYOUT_CONTRACT / DESIGN_TOKENS |

## 4. 视觉

| 元素 | token 键 | token 值 |
| --- | --- | --- |
| 三栏面板 | `semantic.data.panel / .panelRaised` | `#0B1A33` / `#102340` |
| 预览背景 | `colors.canvas` / `colors.canvasDeep` | `#061225` / `#030A18` |
| 分隔 | `semantic.data.divider / .dividerStrong` | `rgba(148,180,234,0.13)` / `rgba(116,161,255,0.34)` |
| 选中高亮 | `colors.primary` / `colors.primaryBright` | `#4D74FF` / `#67A7FF` |
| 文字 | `semantic.ink.strong / .regular / .muted` | `#F4F7FF` / `#A9B8D5` / `#7383A3` |
| 聚焦 ring | `semantic.focus` | `#67A7FF` |

- 预览中 last4 蒙版（`privacyMask.maskCardLast4` / `privacy.maskSensitive`）。

## 5. 交互/状态

- 三栏联动实时预览（INTERACTION_CONTRACT.md §8）；保存 = 本地 preference 写入 + `common.done` inline/toast 确认；错误 = 软底 + `common.retry`（§9）。
- 重置主题（破坏性）需显式确认（`common.cancel` / `common.confirm`）。
- Mobile bottom-sheet：`microInteractions.drawerOpenMs = 200`；reduce motion 下 instant appear（MOTION_CONTRACT.json）。
- 属性反馈 ≤ `tokens.fastMs`（140ms）；无装饰循环动画。

## 6. 无障碍与隐私

- 键盘三栏均可达；focus ring 可见；预览区可朗读。
- 触控 ≥ 44/48；对比度 ≥ 4.5:1。
- 隐私：预览始终蒙版；结果不写 .depmap；`user-image` 仅本地。

## 7. 验收自检

- [ ] testId 存在：`pdig.customization.library / preview / inspector`。
- [ ] Preset 列表 ≥ Minimal/Deep Space/Region/City/Glass/Metal/Abstract。
- [ ] 0.22/0.46/0.32 三栏；Mobile preview + bottom-sheet。
- [ ] 保存 = 本地 preference，不写 .depmap；preset visual ≠ semantic role。
- [ ] 无远程素材；mask 生效；文案来源合规。