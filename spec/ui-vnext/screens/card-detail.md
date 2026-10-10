# Card Detail — 卡片详情（/infrastructure/cards/{id}，身份优先）

> Visual Contract · spec/ui-vnext · 2026-09-29 · No-Vision 模式（实现以此契约为准，非参考图）

## 1. 职责与位置

- **职责**：单卡详情页，**身份优先**——顶部先展示卡面视觉身份（custom card face + nickname/issuer/masked number/network/region/currency/status），其后按信息区（使用场景 / 绑定服务 / 风险 / 恢复替代 / 变更历史）组织。
- **路由**：`/infrastructure/cards/{id}`（IA.md §4）；Desktop 左身份列 30–36% + 右信息列 64–70%；Mobile stack（LAYOUT_CONTRACT.json `cardDetail`）。
- **边界**：缺失数据只显示「未设置 / 未知」，禁止推断（goal §12）；详情页不做领域写操作（写操作在场景页）。

## 2. 数据契约

| 区 | 内容 | copy（spec/ui/copy-zh.json） |
| --- | --- | --- |
| 身份列 | 卡面 + nickname / issuer / masked number / network / region / currency / status | `uiuxV031.what`（是什么）语义参考；新增 copy key：`uiVNext.card.detail.identity = 卡片信息` |
| 使用场景 | type(储蓄/信用) / physical|virtual / expiry 等实际用途 | 新增 `uiVNext.card.detail.usage = 使用场景` |
| 绑定服务 | 依赖服务（relations 类型） | `relations.funding_source`（资金来源）、`relations.merchant_agreement`（自动扣款/订阅）、`relations.default`（支付关系） |
| 风险 | impact 摘要 | `impact.mustChange`（必须处理）、`impact.needsReview`（建议检查）、`impact.backupPath`（有备用路径）、`impact.unaffected`（不受影响） |
| 恢复替代 | 备用路径/替代卡 | `impact.backupPath`、`identity.newPath`（新路径）/ `identity.oldPath`（旧路径） |
| 变更历史 | 最近变更/验证 | `planWorkflow.*`（草稿/已分析/进行中/待验证/已完成/已取消）、`verification.verified`（已验证）、`verification.pending`（待验证） |

- 缺失字段 = `uiVNext.notSet`（未设置）/ `uiVNext.unknown`（未知），不推断。
- 呈现配置 = `PresentationProfile`（`targetType = "card"`）只改外观（PRESENTATION_PROFILE_SCHEMA.json）；显著风险行三通道（icon + label + color）。

## 3. 几何

| 项 | 键名 | 数值 |
| --- | --- | --- |
| 左身份列占比 | `LAYOUT_CONTRACT.json cardDetail.desktopIdentityColumnRatio` | `{min:0.3, max:0.36}` |
| 右信息列占比 | `cardDetail.desktopInfoColumnRatio` | `{min:0.64, max:0.7}` |
| 身份列 testId | `cardDetail.testIds.identity` | `pdig.card.detail.identity` |
| 信息列 testId | `cardDetail.testIds.info` | `pdig.card.detail.info` |
| Mobile | `cardDetail.mobile` | `"stack"`（身份面 → 信息区纵向堆叠） |
| 卡面 | asset-card 几何（ratio 1.586 参考、`radius.lg 14`） | DESIGN_TOKENS.json |
| 页面留白 / 主间距 | `desktopShell.pagePaddingPx 24` / `mainGapPx 20` | LAYOUT_CONTRACT.json |
| 信息区分节间距 | `spacing.sectionGap = 24` | DESIGN_TOKENS.json |

## 4. 视觉

| 元素 | token 键 | token 值 |
| --- | --- | --- |
| 画布 | `semantic.env.background = colors.canvas` | `#061225` |
| 身份列背景 | 程序化卡面（bundled/procedural，禁远程图） | — |
| 信息区面板 | `semantic.data.panel / .panelRaised` | `#0B1A33` / `#102340` |
| 分隔 | `semantic.data.divider = colors.borderSubtle` | `rgba(148,180,234,0.13)` |
| 标题 | `typography.pageTitle`（26/700/1.25）；分节 `typography.sectionTitle`（15/600/1.4） | DESIGN_TOKENS.json |
| 卡面主色 | `colors.primary`（默认 accent） | `#4D74FF` |
| 状态 | `semantic.status.ok / .warning / .critical / .unknown`（三通道） | `#3BD49B` / `#F4B64B` / `#FA626B` / `#8C9AB5` |
| 聚焦 ring | `semantic.focus` | `#67A7FF` |

## 5. 交互/状态

- 身份列动作：「定制」→ `/infrastructure/cards/{id}/customize`（INTERACTION_CONTRACT.md §6）。
- 风险行/绑定服务行 → 详情跳转（变化场景 `/change/card`、薄弱点 `/infrastructure/weaknesses`；恢复替代 → 备用路径明细）。
- 键盘：分节 heading 导航；行 Arrow + Enter；focus ring 可见（INTERACTION_CONTRACT.md §5）。
- 动画：分节出场 ≤ `tokens.normalMs`（200ms）；无入场动画堆叠（MOTION_CONTRACT.json rules）。

## 6. 无障碍与隐私

- Screen reader：身份列朗读 nickname + 遮罩号码 + 状态（不朗读真实 last4）。
- 触控 ≥ 44/48；对比度 ≥ `contrast.bodyTextMin 4.5:1`。
- 隐私：`privacyMask.maskCardLast4` 默认开；`privacy.maskSensitive` 全局遮罩时卡面完全遮蔽。

## 7. 验收自检

- [ ] testId 存在：`pdig.card.detail.identity`、`pdig.card.detail.info`；动作路由 `/infrastructure/cards/{id}/customize`。
- [ ] 几何符合：Desktop 30–36% / 64–70%；Mobile stack。
- [ ] 身份优先：卡面视觉身份在顶部；缺失字段未设置/未知。
- [ ] 无远程素材；last4 masked；状态三通道。
- [ ] 文案来自 copy-zh 或标注「新增 copy key：uiVNext.*」。