# Number Customization — 号码面定制工作室（/infrastructure/numbers/{id}/customize）

> Visual Contract · spec/ui-vnext · 2026-09-29 · No-Vision 模式（实现以此契约为准，非参考图）

## 1. 职责与位置

- **职责**：单号码的呈现层定制——昵称 / 地区视觉 / 国旗 / carrier / SIM eSIM badge / 主副号 / 用途 tags / masking / 主题 / 背景 / layout；Preset ≥ Country / City / Minimal / Banking / Travel / Recovery / Work / Private（goal §12）。
- **路由**：`/infrastructure/numbers/{id}/customize`（IA.md §4）；复用 components/customization-studio.md 三栏组件。
- **铁律**：编辑对象 = PresentationProfile（`targetType = "phoneNumber"`），本地 preference，**绝不写入 .depmap**（goal §13）；**preset visual ≠ semantic role**（选「Banking」主题 ≠ 声明银行角色）。

## 2. 数据契约

| 属性 | 键名 | 允许值/来源 |
| --- | --- | --- |
| 主题 | `themeId` | `country / city / minimal / banking / travel / recovery / work / private`（PRESENTATION_PROFILE_SCHEMA.json 例子） |
| 背景 | `background.kind / background.value` | `preset / gradient / procedural / bundled-image / user-image`（本地） |
| 布局 | `layout` | `standard / minimal / dense / editorial` |
| 昵称 | 呈现层昵称 | 不改变 node 真实信息 |
| 地区视觉/国旗 | 地区视觉元素（bundled 本地素材） | 禁远程图标/图片 |
| carrier | carrier 徽标显隐 | `flags.showCarrier` |
| SIM eSIM badge | SIM 类型徽标显隐 | `flags.showSimBadge` |
| 主副号 | 角色标签显隐 | `flags.showRole` |
| 用途 tags | 用途标签显隐 | `flags.showUsageTags` |
| masking | 号码遮罩偏好 | `privacy.maskNumber / maskSensitive`（默认 true） |

- 素材：bundled local / procedural；`user-image` 仅本地导入；**禁远程 URL**（goal §43、policy `noRemoteImages`）。
- **建议文案**（不修改 copy-zh.json）：新增 copy key `uiVNext.customization.number.title = 号码面定制`、`uiVNext.customization.field.mask = 号码遮罩`、`uiVNext.customization.field.regionVisual = 地区视觉`、`uiVNext.customization.field.flag = 国旗`；其余沿用 `uiVNext.customization.field.*` 与 copy-zh 现有 key。

## 3. 几何

| 项 | 键名 | 数值 |
| --- | --- | --- |
| Desktop 三栏 | `LAYOUT_CONTRACT.json customization.desktopColumns` | `0.22 / 0.46 / 0.32` |
| 三栏 testId | `customization.desktopColumns[].testId` | `pdig.customization.library` / `pdig.customization.preview` / `pdig.customization.inspector` |
| Mobile | `customization.mobile` | `"preview on top, bottom-sheet editor below"` |
| 预览号码面 | number-face（`radius.lg 14`、`typography.mono`） | DESIGN_TOKENS.json |

## 4. 视觉

| 元素 | token 键 | token 值 |
| --- | --- | --- |
| 三栏面板 | `semantic.data.panel / .panelRaised` | `#0B1A33` / `#102340` |
| 预览背景 | `colors.canvas` / `colors.canvasDeep` | `#061225` / `#030A18` |
| 分隔 | `semantic.data.divider / .dividerStrong` | `rgba(148,180,234,0.13)` / `rgba(116,161,255,0.34)` |
| 选中高亮 | `colors.primary` / `colors.primaryBright` | `#4D74FF` / `#67A7FF` |
| 号码预览 | `typography.mono`；遮罩 `semantic.ink.muted` | DESIGN_TOKENS.json / `#7383A3` |
| 聚焦 ring | `semantic.focus` | `#67A7FF` |

## 5. 交互/状态

- 三栏联动实时预览（INTERACTION_CONTRACT.md §8）；保存 = 本地 preference + `common.done` 确认；错误 + `common.retry`（§9）。
- 重置（破坏性）需显式确认（`common.cancel` / `common.confirm`）。
- Mobile bottom-sheet：`drawerOpenMs = 200`；reduce motion instant appear（MOTION_CONTRACT.json）。
- 属性反馈 ≤ `tokens.fastMs`（140ms）。

## 6. 无障碍与隐私

- 键盘三栏可达；focus ring 可见；预览区可朗读（朗读遮罩号码）。
- 触控 ≥ 44/48；对比度 ≥ 4.5:1。
- 隐私：号码始终遮罩显示（`privacyMask.maskPhoneNumbers`）；结果不写 .depmap；`user-image` 仅本地。

## 7. 验收自检

- [ ] testId 存在：`pdig.customization.library / preview / inspector`。
- [ ] Preset 列表 ≥ Country/City/Minimal/Banking/Travel/Recovery/Work/Private。
- [ ] preset visual ≠ semantic role（主题不改领域语义）。
- [ ] masking 选项生效（默认 mask）。
- [ ] 0.22/0.46/0.32 三栏；Mobile preview + bottom-sheet；无远程素材。
- [ ] 保存 = 本地 preference，不写 .depmap；文案来源合规。