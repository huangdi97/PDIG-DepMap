# Navigation — 导航系统（Rail / Top Command / 二级导航 / 命令条）

> Visual Contract · spec/ui-vnext · 2026-09-29 · No-Vision 模式（实现以此契约为准，非参考图）

## 1. 职责与位置

- **职责**：全应用导航骨架（LAYER L2 Navigation / Control Chrome）：一级导航、二级导航、基础设施二级、Desktop Rail、Top Command Bar、Ctrl/Cmd+K 命令条。
- **原则**：一级导航永远可见（Desktop rail / Mobile bottom nav，IA.md §7）；Globe 不是唯一导航（IA.md §7）；卡片/号码是**基础设施二级页，不是一级导航**（IA.md §3 重要）。
- **路由**：见 IA.md §4 Screen Registry（如 `/now`、`/infrastructure`、`/infrastructure/cards`、`/settings/personalization`、`/change/phone`）。

## 2. 数据契约

一级导航（IA.md §1）：

| 顺序 | 中文 | 英文 key | testId |
| --- | --- | --- | --- |
| 1 | 现在 | now | `pdig.nav.now` |
| 2 | 基础设施 | infrastructure | `pdig.nav.infrastructure` |
| 3 | 变更 | change | `pdig.nav.change` |
| 4 | 记录 | records | `pdig.nav.records` |
| 5 | 我 | me | `pdig.nav.me` |

> `我` 是产品明确新增的第五个一级目的地。头像可作为快捷入口，但不得替代 bottom nav / primary rail 中的 `我`。

二级导航（IA.md §2）：数据源 `pdig.nav.sources`；设置 `pdig.nav.settings`。它们是「我」的低频子工具/快捷入口，不是新的一级目的地。

基础设施二级（IA.md §3）：

| 中文 | 英文 key | testId | MVP 优先级 |
| --- | --- | --- | --- |
| 总览 | overview | `pdig.nav.infra.overview` | 高（Globe 舞台） |
| 卡片 | cards | `pdig.nav.infra.cards` | **最高（双入口之一，二级页非一级）** |
| 号码 | numbers | `pdig.nav.infra.numbers` | **最高（双入口之一，二级页非一级）** |
| 账户 | accounts | `pdig.nav.infra.accounts` | 中（vNext 范围外，占位） |
| 邮箱 | emails | `pdig.nav.infra.emails` | 中（占位） |
| 设备 | devices | `pdig.nav.infra.devices` | 中（占位） |
| 服务 | services | `pdig.nav.infra.services` | 中（占位） |
| 薄弱点 | weaknesses | `pdig.nav.infra.weaknesses` | 中 |

- **建议文案**（不修改 copy-zh.json）：新增 copy key `uiVNext.nav.now = 现在`、`uiVNext.nav.infrastructure = 基础设施`、`uiVNext.nav.change = 变更`、`uiVNext.nav.records = 记录`、`uiVNext.nav.me = 我`、`uiVNext.nav.sources = 数据源`、`uiVNext.nav.settings = 设置`、`uiVNext.nav.infra.overview = 总览`、`uiVNext.nav.infra.cards = 卡片`、`uiVNext.nav.infra.numbers = 号码`、`uiVNext.nav.infra.accounts = 账户`、`uiVNext.nav.infra.emails = 邮箱`、`uiVNext.nav.infra.devices = 设备`、`uiVNext.nav.infra.services = 服务`、`uiVNext.nav.infra.weaknesses = 薄弱点`。

## 3. 几何

| 项 | 键名 | 数值 |
| --- | --- | --- |
| Rail 折叠宽 | `LAYOUT_CONTRACT.json desktopShell.navigationRail.collapsedWidthPx` | `80`（范围 `collapsedRangePx {min:76, max:88}`） |
| Rail 展开宽 | `desktopShell.navigationRail.expandedWidthPx / expandedMaxPx` | `188 / 188` |
| Rail testId | `desktopShell.navigationRail.testId` | `pdig.nav.rail` |
| Top Command 高 | `desktopShell.topCommandBar.heightPx` | `48`（范围 `rangePx {min:44, max:52}`） |
| Top Command testId | `desktopShell.topCommandBar.testId` | `pdig.nav.top` |
| 页面留白 | `desktopShell.pagePaddingPx`（范围 24–32） | `24` |
| 主内容间距 | `desktopShell.mainGapPx`（范围 16–24） | `20` |
| Rail 宽度范围 token | `DESIGN_TOKENS.json components.nav.railWidthRange` | `{min:76, max:188}` |
| Top Command 高 token | `components.nav.topCommandHeight` | `48` |
| 内容最大宽 | `RESPONSIVE_CONTRACT.json desktop.contentMaxWidth` / `DESIGN_TOKENS.json components.page.maxWidth` | `1400` |
| 移动端 | `RESPONSIVE_CONTRACT.json android.phone` | bottom nav = 5 个一级目的地（Material 允许 3–5） |
| 触控 | `DESIGN_TOKENS.json components.touchTarget` | Android 48 / iOS 44 |

- Rail 折叠/展开动画：`MOTION_CONTRACT.json microInteractions.railStateChangeMs = 180`；导航切换 `navigationSwitchMs = 200`。

## 4. 视觉

> 下表是 Desktop/raw token 基线。**Android R19 使用 light-first semantic translation**：
> Canvas/Surface 为浅色，选中态使用受控蓝色，数据区保持白/浅灰高对比；不得因 raw token 为
> deep navy 就把 Android primary shell 恢复成全局暗色。Globe/资产身份面可保留局部深色以维持空间与材质深度。

| 元素 | token 键 | token 值 |
| --- | --- | --- |
| Rail / Top Command 背景 | `semantic.env.glass = colors.surfaceGlass`（L2 允许 blur） | `rgba(28,56,96,0.54)` |
| 选中项指示 | `colors.primary` / `colors.primaryBright`（指示条） | `#4D74FF` / `#67A7FF` |
| 文字 | `semantic.ink.strong = colors.textPrimary` / `semantic.ink.regular = colors.textSecondary` / `semantic.ink.muted = colors.textMuted` | `#F4F7FF` / `#A9B8D5` / `#7383A3` |
| 聚焦 ring | `semantic.focus = colors.primaryBright` | `#67A7FF` |
| 图标 | 系统/本地图标（政策 `noRemoteIconCdn = true`） | DESIGN_TOKENS.json policy |
| 对比度 | `contrast.bodyTextMin` / `contrast.largeTextMin` | `4.5:1` / `3:1` |

- 图标不来自远程 CDN；文案入 i18n 资源（copy-zh.json `$comment`：硬编码 inline copy 禁止）。

## 5. 交互/状态

- 一级导航切换 = 全应用状态切换；再次点击返回当前页语义（INTERACTION_CONTRACT.md §6）。
- `Ctrl/Cmd + K` 打开命令/搜索条（top bar 内，INTERACTION_CONTRACT.md §5）。
- 键盘焦点循环：rail → top bar → 内容 → region list（§5）；Enter/Space 激活；Arrow 在导航项内移动。
- 桌面启动顺序（IA.md §8）：有数据时 `now` 优先；有 P0 风险时 now 直接呈现 attention；基础设施默认 `overview`。
- 命令条内可直达：`/infrastructure/cards`、`/infrastructure/numbers`、`/change/phone`、`/infrastructure`（与一级/二级语义一致）。

## 6. 无障碍与隐私

- 键盘全程可达（§5）；焦点可见 = `semantic.focus` ring。
- 触控热区 ≥ `components.touchTarget`（44/48）；移动端 bottom nav ≤5 项（RESPONSIVE_CONTRACT android.phone）。
- 对比度达标（`contrast.bodyTextMin 4.5:1`）；状态变化不靠闪烁（MOTION_CONTRACT rules 无闪烁、无装饰性动画）。
- 隐私：导航不出现任何真实卡号/号码/账户名，只有页面语义。

## 7. 验收自检

- [ ] testId 全量存在：`pdig.nav.rail`、`pdig.nav.top`、一级 5 个（now/infrastructure/change/records/me）、二级 2 个（sources/settings）、基础设施二级 8 个（overview/cards/numbers/accounts/emails/devices/services/weaknesses）。
- [ ] 卡片/号码是二级页且非一级导航（IA.md §3）。
- [ ] 几何符合：rail 80 → 188（范围 76–88 / max 188）、top command 48（44–52）、页面 padding 24（24–32）、主 gap 20（16–24）。
- [ ] Ctrl/Cmd+K 打开命令条可测（`pdig.nav.top`）。
- [ ] 无远程图标/字体（policy）。

# 锚点引用（本文件自包含所需）

- testId 前缀集合：`DESIGN_TOKENS.json testIds.prefixes = ["pdig.nav.", "pdig.globe.", "pdig.region.", "pdig.card.", "pdig.phone.", "pdig.change.", "pdig.customization."]`，四端同一语义 id。
- 屏幕路由全集见 IA.md §4。