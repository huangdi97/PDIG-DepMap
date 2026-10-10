# Number Detail — 号码详情（/infrastructure/numbers/{id}，身份面优先）

> Visual Contract · spec/ui-vnext · 2026-09-29 · No-Vision 模式（实现以此契约为准，非参考图）

## 1. 职责与位置

- **职责**：单号码详情页，**身份面优先**——顶部 number identity surface（昵称 / masked number / region / carrier / SIM eSIM / role / usage / status），其后按 关联服务 / 登录用途 / 2FA / 恢复用途 / 风险 / 备用路径 / 历史。
- **路由**：`/infrastructure/numbers/{id}`（IA.md §4）；Mobile stack（RESPONSIVE_CONTRACT.json ios.phone NavigationStack + List + sheets）。
- **边界**：只读为主 + 动作链接（去定制 / 变更场景）；缺失 = 未设置 / 未知，禁止推断。

## 2. 数据契约

| 区 | 内容 | copy（spec/ui/copy-zh.json） |
| --- | --- | --- |
| 身份面 | 昵称 / masked number / region / carrier（若知）/ SIM eSIM / role / usage / status | `identity.phoneNumber`（手机号）；`uiVNext.phone.*`（见 components/phone-card.md） |
| 关联服务 | 服务依赖列表 | `relations.default`（支付关系）、`relationsV030.controls`（登录控制/管理入口） |
| 登录用途 | 用于登录的账户 | `relationsV030.authenticates`（验证方式/登录依据） |
| 2FA | 两步验证绑定 | `identity.authenticationPath`（验证方式）；新增 `uiVNext.inspector.twoFA = 两步验证` |
| 恢复用途 | 找回密码/账号 | `relationsV030.recovers`（恢复方式/找回路径）、`identity.recoveryPath`（恢复路径） |
| 风险 | 号码相关风险 | `impact.mustChange`（必须处理）、`impact.needsReview`（建议检查）、`findings.singlePointOfFailure`（只有一个来源，没有备用路径） |
| 备用路径 | 恢复/替代号码 | `impact.backupPath`（有备用路径）、`failureDomain.independentPath`（独立恢复路径）、`recoveryCycle.confirmed`（循环说明） |
| 历史 | 最近变更/验证记录 | `planWorkflow.*`、`verification.verified / pending / failed` |

- 遮罩：`DESIGN_TOKENS.json privacyMask.maskPhoneNumbers = true`；`PRESENTATION_PROFILE_SCHEMA.json privacy.maskNumber`。
- 缺失 = `uiVNext.notSet`（未设置）/ `uiVNext.unknown`（未知）。

## 3. 几何

| 项 | 键名 | 数值 |
| --- | --- | --- |
| 身份面 | number-face 形态（LAYOUT_CONTRACT.json `numbers.mobileDefault` 语义）：cards + sections | `radius.lg 14` |
| Desktop 布局 | 身份面 + 信息分节（宽度 ≤ `components.page.maxWidth`） | `1400` |
| 信息区分节间距 | `DESIGN_TOKENS.json spacing.sectionGap` | `24` |
| 列表/历史行高 | RESPONSIVE_CONTRACT.json rules「row 40-48」 | `40 – 48` |
| 页面留白 | `LAYOUT_CONTRACT.json desktopShell.pagePaddingPx` | `24`（24–32） |
| 触控 | `DESIGN_TOKENS.json components.touchTarget` | Android 48 / iOS 44 |

## 4. 视觉

| 元素 | token 键 | token 值 |
| --- | --- | --- |
| 身份面背景 | procedural / preset / bundled（禁远程图） | — |
| 号码文字 | `typography.mono`（13/400/1.4）；遮罩 `semantic.ink.muted` | DESIGN_TOKENS.json / `#7383A3` |
| 面板 | `semantic.data.panel / .panelRaised` | `#0B1A33` / `#102340` |
| 分隔 | `semantic.data.divider = colors.borderSubtle` | `rgba(148,180,234,0.13)` |
| 标题 | `typography.pageTitle`；分节 `typography.sectionTitle` | DESIGN_TOKENS.json |
| 状态徽标 | `semantic.status.ok / .warning / .critical / .unknown`（三通道） | `#3BD49B` / `#F4B64B` / `#FA626B` / `#8C9AB5` |
| 动作链接 | `colors.primaryBright` | `#67A7FF` |
| 聚焦 ring | `semantic.focus` | `#67A7FF` |

## 5. 交互/状态

- 动作：定制 → `/infrastructure/numbers/{id}/customize`；换号场景 → `/change/phone`（flagship）；恢复路径 → 备用路径明细（INTERACTION_CONTRACT.md §6）。
- 键盘：heading 导航 + Arrow/Enter；focus ring 可见（§5）。
- 异步加载：`common.loading` / 完成 inline 确认 / 错误 + `common.retry`（INTERACTION_CONTRACT.md §9）。
- 动画 ≤ `tokens.normalMs`（200ms）；无装饰循环动画。

## 6. 无障碍与隐私

- Screen reader 朗读昵称 + 遮罩号码 + role/usage（不朗读真实号码）。
- 触控 ≥ 44/48；对比度 ≥ `contrast.bodyTextMin 4.5:1`。
- 隐私：号码/账户名按 `privacyMask.maskPhoneNumbers / maskAccountNames` 遮罩；日志不记录真实号码（AGENTS §17）。

## 7. 验收自检

- [ ] testId 存在：身份面段 `pdig.phone.detail.identity`（本契约新增，前缀 `pdig.phone.`）、分节与历史行可定位。
- [ ] 身份面优先置顶；分节齐全（关联服务/登录用途/2FA/恢复用途/风险/备用路径/历史）。
- [ ] 缺失字段未设置/未知；masked number 生效；运营商未知不填充。
- [ ] 无远程素材；状态三通道。
- [ ] 文案来自 copy-zh 或标注「新增 copy key：uiVNext.*」。