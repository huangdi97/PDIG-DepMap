# Infrastructure Numbers — 号码管理（/infrastructure/numbers）

> Visual Contract · spec/ui-vnext · 2026-09-29 · No-Vision 模式（实现以此契约为准，非参考图）

## 1. 职责与位置

- **职责**：管理全球手机号的基础设施二级页——过滤（国家/区号/SIM eSIM/主副号/用途/状态/恢复用途）；Desktop 默认 **List/Table + Inspector**（信息密度高，**不硬套卡片**）；Mobile 默认 number-face cards + sections（LAYOUT_CONTRACT.json `numbers.desktopDefault` / `numbers.mobileDefault`）。
- **路由**：`/infrastructure/numbers`（IA.md §4）；`pdig.nav.infra.numbers` = 最高优先二级页（IA.md §3），**不是一级导航**。
- **边界**：号码身份面组件契约见 components/phone-card.md；列表行不承载重操作。

## 2. 数据契约

号码身份字段（goal §12、§22）：昵称 / 遮罩号码 / 地区 / 运营商（若知）/ SIM eSIM / 角色（主号/副号/保号/银行验证/工作/旅行/恢复/2FA 等）/ 用途 tags / 状态。缺失 = 未设置 / 未知，禁止推断。

过滤维度（goal §12）：国家 / 区号 / SIM eSIM / 主副号 / 用途 / 状态 / 恢复用途。

- 示例号段（仅示例语义区段，非真实数据）：`+86` / `+852` / `+853` / `+44` / `+1` / `+65`（goal §22 fixture 覆盖 CN/HK/GB/US）。
- 遮罩规则：`DESIGN_TOKENS.json privacyMask.maskPhoneNumbers = true`；`PRESENTATION_PROFILE_SCHEMA.json privacy.maskNumber`。
- **建议文案**（不修改 copy-zh.json）：新增 copy key `uiVNext.phone.filter.region = 国家`、`uiVNext.phone.filter.area = 区号`、`uiVNext.phone.filter.sim = SIM 类型`、`uiVNext.phone.filter.role = 主副号`、`uiVNext.phone.filter.usage = 用途`、`uiVNext.phone.filter.status = 状态`、`uiVNext.phone.filter.recovery = 恢复用途`。

## 3. 几何

| 项 | 键名 | 数值 |
| --- | --- | --- |
| Desktop 默认 | `LAYOUT_CONTRACT.json numbers.desktopDefault` | `"list-table + inspector"` |
| Mobile 默认 | `numbers.mobileDefault` | `"number-face cards + sections"` |
| 列表 testId | `numbers.listTestId` | `pdig.phone.list` |
| Inspector testId | `numbers.inspectorTestId` | `pdig.phone.inspector` |
| 行高 | RESPONSIVE_CONTRACT.json rules「row 40-48」 | `40 – 48` |
| Inspector 右栏宽度 | 量级参考 `overview1920.activityRail.widthPx` | `300 – 380` |
| 页面留白 / 主间距 | `LAYOUT_CONTRACT.json desktopShell.pagePaddingPx 24` / `mainGapPx 20` | `24` / `20` |
| 触控 | `DESIGN_TOKENS.json components.touchTarget` | Android 48 / iOS 44 |

- 高密度列表：列头 + 分割线 + 行（`semantic.data.divider`），**不为高密度列表硬套卡片**（goal §12）。

## 4. 视觉

| 元素 | token 键 | token 值 |
| --- | --- | --- |
| 画布 | `semantic.env.background = colors.canvas` | `#061225` |
| 表头/面板 | `semantic.data.panel / .panelRaised` | `#0B1A33` / `#102340` |
| 行分隔 | `semantic.data.divider = colors.borderSubtle` | `rgba(148,180,234,0.13)` |
| 号码 | `typography.mono`（13/400/1.4）；遮罩部分 `semantic.ink.muted` | DESIGN_TOKENS.json / `#7383A3` |
| 主文字/meta | `semantic.ink.strong` / `semantic.ink.regular` | `#F4F7FF` / `#A9B8D5` |
| SIM/eSIM badge | `radius.md = 10` 徽标 | DESIGN_TOKENS.json |
| 状态 | `semantic.status.*`（三通道） | `#3BD49B` / `#F4B64B` / `#FA626B` / `#8C9AB5` |
| 聚焦 ring | `semantic.focus` | `#67A7FF` |

## 5. 交互/状态

- 列表行选择 → Inspector 联动（master-detail）；行 → 号码详情（`/infrastructure/numbers/{id}`）→（可选）定制（`/infrastructure/numbers/{id}/customize`）（INTERACTION_CONTRACT.md §6）。
- Inspector 只读为主，动作 = 链接去详情页（components/inspector.md）。
- 过滤即时生效且与路由 query 同步（`?region=` 等，INTERACTION_CONTRACT.md §6）。
- 键盘：Arrow 行导航 + Enter；Tab 在列表 ↔ inspector 间循环（§5）；focus ring 可见。
- 动画：行状态 `statusChangeMs = 180`、导航 `navigationSwitchMs = 200`（MOTION_CONTRACT.json）。

## 6. 无障碍与隐私

- 键盘全程可达（列表 + inspector）；screen reader 朗读昵称 + 遮罩号码 + 标签（不朗读真实号码）。
- 触控 ≥ 44/48；对比度 ≥ `contrast.bodyTextMin 4.5:1`。
- 隐私：号码蒙版默认开（`privacyMask.maskPhoneNumbers` / `privacy.maskNumber`）；日志不含真实号码（AGENTS §17）。

## 7. 验收自检

- [ ] testId 存在：`pdig.phone.list`、`pdig.phone.inspector`、行项 `pdig.phone.row`；导航 `pdig.nav.infra.numbers`。
- [ ] Desktop 默认 list-table + inspector（不硬套卡片）；Mobile number-face cards + sections。
- [ ] 7 类过滤维度可用且与路由 query 同步。
- [ ] 行高 40–48；遮罩生效；运营商未知不填充。
- [ ] 无远程素材；文案来自 copy-zh 或标注「新增 copy key：uiVNext.*」。