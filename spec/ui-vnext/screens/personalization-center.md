# Personalization Center — 个性化中心（/settings/personalization）

> Visual Contract · spec/ui-vnext · 2026-09-29 · No-Vision 模式（实现以此契约为准，非参考图）

## 1. 职责与位置

- **职责**：我 → 设置 → 个性化的集中页面（goal §12）——workspace theme / globe theme / nav density / card defaults / number defaults / privacy masking / home modules 显隐重排 / region grouping / motion / reduced effects。
- **路由**：`/me/settings/personalization`（IA.md §4；`我` 为一级父上下文，设置为低频子工具）。
- **P0 原则**：**P0 critical action 不可隐藏**（goal §12）——即使 home modules 允许显隐，P0 关键动作入口（如处理 must-change 事项）永不隐藏。
- **边界**：所有设置均为本地偏好，不改变领域数据、不写 .depmap（goal §13）。

## 2. 数据契约

| 设置组 | 选项 | 契约来源 |
| --- | --- | --- |
| workspace theme | 深空主题（唯一 vNext 主题；浅色为系统跟随选项） | DESIGN_TOKENS.json colors.* |
| globe theme | 相机预置偏好 / 大气强度档 | `globe.cameraPresets`、`colors.atmosphereInner/Outer` |
| nav density | rail 折叠默认 / top command 高度档 | `LAYOUT_CONTRACT.json desktopShell.navigationRail`（80/188、`rangePx 44–52`） |
| card defaults | 卡面默认 preset / material / layout | PRESENTATION_PROFILE_SCHEMA.json（`themeId/material/layout`） |
| number defaults | 号码面默认 preset / flag 默认值 | `flags.showCarrier/showSimBadge/showRole/showUsageTags/showCountryFlagForNumber` |
| privacy masking | 全局 Privacy Mask 开关 | `privacyMask.maskCardLast4 / maskPhoneNumbers / maskAccountNames`（默认 true） |
| home modules | Now 页模块显隐 + 重排 | `LAYOUT_CONTRACT.json now.blocks`（可隐藏区：upcoming / region-quick-access；**P0 不可隐藏**） |
| region grouping | 分组偏好 | IA.md §5（中国大陆 / 港澳 / 欧洲 / 北美 / 东南亚 / 自定义） |
| motion | 动效开关 / 时长档 | MOTION_CONTRACT.json `tokens.fastMs/normalMs/slowMs` |
| reduced effects | Reduce Motion | MOTION_CONTRACT.json `reduceMotion`（idleRotation off / camera simplified / arc static / drawer instant） |

- **建议文案**（不修改 copy-zh.json）：新增 copy key `uiVNext.settings.personalization = 个性化`、`uiVNext.settings.workspaceTheme = 工作区主题`、`uiVNext.settings.globeTheme = 星球主题`、`uiVNext.settings.navDensity = 导航密度`、`uiVNext.settings.cardDefaults = 卡片默认`、`uiVNext.settings.numberDefaults = 号码默认`、`uiVNext.settings.privacyMask = 隐私遮罩`、`uiVNext.settings.homeModules = 首页模块`、`uiVNext.settings.regionGrouping = 地区分组`、`uiVNext.settings.motion = 动效`、`uiVNext.settings.reducedEffects = 减弱动态效果`、`uiVNext.settings.p0Protected = 关键操作不可隐藏`；复用 copy-zh.json `common.save / cancel / done`。

## 3. 几何

| 项 | 键名 | 数值 |
| --- | --- | --- |
| 页面宽度 | `DESIGN_TOKENS.json components.page.maxWidth` | `1400` |
| 页面留白 | `LAYOUT_CONTRACT.json desktopShell.pagePaddingPx` | `24`（24–32） |
| 设置行高 | RESPONSIVE_CONTRACT.json rules「row 40-48」 | `40 – 48` |
| 分组间距 | `DESIGN_TOKENS.json spacing.sectionGap` | `24` |
| 触控 | `components.touchTarget` | Android 48 / iOS 44 |

## 4. 视觉

| 元素 | token 键 | token 值 |
| --- | --- | --- |
| 画布 | `semantic.env.background = colors.canvas` | `#061225` |
| 分组面板 | `semantic.data.panel / .panelRaised` | `#0B1A33` / `#102340` |
| 分隔 | `semantic.data.divider = colors.borderSubtle` | `rgba(148,180,234,0.13)` |
| 页面标题/分组标题 | `typography.pageTitle`（26/700/1.25）/ `typography.sectionTitle`（15/600/1.4） | DESIGN_TOKENS.json |
| 开关/选中 | `colors.primary` / `colors.primaryBright` | `#4D74FF` / `#67A7FF` |
| P0 锁定说明 | `semantic.ink.muted` + 锁形 icon（三通道外提示） | `#7383A3` |
| 聚焦 ring | `semantic.focus` | `#67A7FF` |

## 5. 交互/状态

- 设置项即时生效（fast ≤140ms 或 normal 200ms）；偏好持久化到本地 preferences（goal §13）。
- Reduce Motion 开启 = 联动 `reduceMotion` 四项（MOTION_CONTRACT.json）：idle rotation off、camera simplified、arc static、drawer instant。
- Home modules 重排：拖拽/上移下移（键盘 Arrow 等价）；P0 模块固定且显示「不可隐藏」说明（`uiVNext.settings.p0Protected`），不给隐藏开关或给开关但置灰 + 原因（INTERACTION_CONTRACT.md §9 disabled 必带原因）。
- 破坏性（恢复默认设置）需显式确认（`common.confirm` / `common.cancel`）。

## 6. 无障碍与隐私

- 键盘全程可达（开关 = Space；行导航 = Arrow）；focus ring 可见；screen reader 朗读设置项状态（开/关/P0 锁定）。
- 触控 ≥ 44/48；对比度 ≥ `contrast.bodyTextMin 4.5:1`。
- 隐私：masking 默认全开（`privacyMask.*` = true），设置页本身不显示任何真实对象数据；设置结果不写 .depmap。

## 7. 验收自检

- [ ] testId 存在：设置项行 `pdig.settings.*`（复用前缀语义；本页建议 `pdig.settings.personalization.*`，前缀一致 per DESIGN_TOKENS `testIds.rule`）。
- [ ] 10 组设置项齐全（workspace theme / globe theme / nav density / card defaults / number defaults / privacy masking / home modules / region grouping / motion / reduced effects）。
- [ ] P0 critical action 不可隐藏（home modules 不提供隐藏；或隐藏项置灰附原因）。
- [ ] 动效开关联动 `reduceMotion` 四项。
- [ ] 所有设置为本地 preference，不写 .depmap。
- [ ] 文案来自 copy-zh 或标注「新增 copy key：uiVNext.*」。