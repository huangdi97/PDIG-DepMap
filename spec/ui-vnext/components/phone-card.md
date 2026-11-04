# Phone Card — 号码身份面（Number Face / 列表行 + Inspector）

> Visual Contract · spec/ui-vnext · 2026-09-29 · No-Vision 模式（实现以此契约为准，非参考图）

## 1. 职责与位置

- **职责**：LAYER L4 Asset Identity 的「号码身份面」——展示一个手机号的身份信息面（昵称/遮罩号码/地区/运营商(若知)/SIM eSIM/角色/用途 tags/状态）。
- **使用位置**：Infrastructure Numbers（`/infrastructure/numbers`）——Desktop 默认 `list-table + inspector`（master-detail），Mobile 默认 `number-face cards + sections`（LAYOUT_CONTRACT.json `numbers.desktopDefault` / `numbers.mobileDefault`）；Number Detail（`/infrastructure/numbers/{id}`）顶部身份面。
- **边界**：列表/身份面不做重操作（操作进详情页）；运营商「若知」——未知不得填充。

## 2. 数据契约

身份字段（goal §12、§22）：

| 字段 | 说明 | 缺失规则 |
| --- | --- | --- |
| 昵称、遮罩号码、地区、运营商、SIM/eSIM、角色、用途 tags、状态 | 运营商仅「若知」显示；角色如主号/副号/保号/银行验证/工作/旅行/恢复/2FA | 缺失 = 未设置 / 未知 |

遮罩规则（masking）：

- `DESIGN_TOKENS.json privacyMask.maskPhoneNumbers = true`（默认）；
- `PRESENTATION_PROFILE_SCHEMA.json privacy.maskNumber / maskSensitive`（`maskSensitive` 默认 `true`）；
- 示例呈现（仅为展示格式，非数据）：`+86 138****8000`。新增 copy key：`uiVNext.phone.maskNote = 号码已隐藏`。

呈现配置 = `PresentationProfile`（`targetType = "phoneNumber"`）：

| 属性 | 键名 | 允许值 |
| --- | --- | --- |
| 主题 | `themeId` | `country / city / minimal / banking / travel / recovery / work / private` |
| 材质 | `material` | `glass / metal / matte / paper / none` |
| 布局 | `layout` | `standard / minimal / dense / editorial` |
| 显隐 | `flags.showCarrier / showSimBadge / showRole / showUsageTags / showCountryFlagForNumber` | boolean |

- **建议文案**（不修改 copy-zh.json）：新增 copy key `uiVNext.phone.region = 地区`、`uiVNext.phone.carrier = 运营商`、`uiVNext.phone.sim = SIM 卡类型`、`uiVNext.phone.role = 角色`、`uiVNext.phone.usage = 用途`、`uiVNext.phone.status = 状态`；SIM 文案 `uiVNext.phone.sim.physical = SIM`、`uiVNext.phone.sim.esim = eSIM`；角色/用途标签按业务标签入 i18n（`uiVNext.phone.role.* = 主号/副号/保号/银行验证/工作/旅行/恢复/2FA`）。

## 3. 几何

| 项 | 键名 | 数值 |
| --- | --- | --- |
| Desktop 默认 | `LAYOUT_CONTRACT.json numbers.desktopDefault` | `"list-table + inspector"` |
| Mobile 默认 | `numbers.mobileDefault` | `"number-face cards + sections"` |
| 列表 testId | `numbers.listTestId` | `pdig.phone.list` |
| Inspector testId | `numbers.inspectorTestId` | `pdig.phone.inspector` |
| 行高 | RESPONSIVE_CONTRACT.json rules「row 40-48」 | `40 – 48` |
| 触控热区 | `DESIGN_TOKENS.json components.touchTarget` | Android 48 / iOS 44 |
| 身份面圆角 | `radius.lg` | `14` |
| 号码字体 | `typography.mono`（13 / 400 / 1.4） | DESIGN_TOKENS.json |

- 信息密度高：**不硬套卡片布局**（goal §12）——Desktop 列表行 + inspector，只有 mobile number-face 视图才用卡面形态。

## 4. 视觉

| 元素 | token 键 | token 值 |
| --- | --- | --- |
| 身份面背景 | procedural / preset / bundled（禁远程 URL） | — |
| 号码文字 | `typography.mono`；遮罩部分 `semantic.ink.muted` | `#7383A3` |
| 昵称 / 主要信息 | `semantic.ink.strong = colors.textPrimary` | `#F4F7FF` |
| meta | `semantic.ink.regular = colors.textSecondary` | `#A9B8D5` |
| SIM/eSIM badge | `radius.md = 10` 小徽标 + `semantic.ink.regular` | — |
| 状态徽标 | `semantic.status.ok / .warning / .critical / .unknown`（三通道） | `#3BD49B` / `#F4B64B` / `#FA626B` / `#8C9AB5` |
| 行分隔 | `semantic.data.divider = colors.borderSubtle`；面板 `semantic.data.panel` | `rgba(148,180,234,0.13)` / `#0B1A33` |
| 聚焦 ring | `semantic.focus = colors.primaryBright` | `#67A7FF` |

## 5. 交互/状态

- 列表行 → 号码详情（`/infrastructure/numbers/{id}`，身份面优先）→（可选）定制工作室（`/infrastructure/numbers/{id}/customize`）（INTERACTION_CONTRACT.md §6）。
- 键盘：Arrow 行导航 + Enter 进入；Tab 可达；focus ring 可见（§5）。
- 行 hover/focus 状态变化 ≤ `tokens.fastMs`（140ms）；状态切换 `microInteractions.statusChangeMs = 180`（MOTION_CONTRACT.json）。
- Inspector 内容只读为主，动作链接去详情页（见 components/inspector.md 契约）。

## 6. 无障碍与隐私

- 遮罩：`privacyMask.maskPhoneNumbers` 默认 true；全局 Privacy Mask 时号码完全遮罩（`privacy.maskNumber`）。
- Screen reader：朗读昵称 + 遮罩号码 + 角色/用途标签（不朗读真实号码）。
- 触控 ≥ 44/48；对比度 ≥ `contrast.bodyTextMin 4.5:1`。
- 键盘全程可达（列表 + inspector 焦点循环）。

## 7. 验收自检

- [ ] testId 存在：`pdig.phone.list`、`pdig.phone.inspector`、行项 `pdig.phone.row`（本契约新增，可带 index）。
- [ ] Desktop 默认 list-table + inspector；Mobile number-face cards + sections。
- [ ] 遮罩规则生效（`maskPhoneNumbers` / `maskNumber` 默认开）；运营商未知不填充。
- [ ] 行高 40–48；触控 44/48。
- [ ] 无远程素材；状态三通道。
- [ ] 文案来自 copy-zh 或标注「新增 copy key：uiVNext.*」。