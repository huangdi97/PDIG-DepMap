# IA.md — PDIG UI vNext 信息架构（冻结）

> No-Vision Visual Contract 的一部分（spec §4）；语义四端一致，导航形态各端原生。

## 1. 一级导航（Primary）

| 顺序 | 中文 | 英文 key | testId | 职责 |
| --- | --- | --- | --- | --- |
| 1 | 现在 | now | pdig.nav.now | 现在最值得处理什么（Need Attention / Active Changes / Upcoming + globe context） |
| 2 | 基础设施 | infrastructure | pdig.nav.infrastructure | 全球基础设施总览（默认焦点页入口） |
| 3 | 变更 | change | pdig.nav.change | 变更场景 / 换卡换号旗舰流程 |
| 4 | 记录 | records | pdig.nav.records | 历史 / 变更记录 |
| 5 | 我 | me | pdig.nav.me | 我的数字生活、隐私、个人偏好与个人工作区入口 |

> **R19 产品决策覆盖**：`我` 是有意新增的第五个一级目的地，不得降级为头像、overflow、设置子项或仅宽屏工具项。Android compact NavigationBar 直接保留五项；Medium/Expanded 主 Rail 同样保留五项。

## 2. 二级导航（Secondary）

| 顺序 | 中文 | 英文 key | testId |
| --- | --- | --- | --- |
| 1 | 数据源 | sources | pdig.nav.sources |
| 2 | 设置 | settings | pdig.nav.settings |

## 3. 基础设施二级（Infrastructure Secondary）

| 中文 | 英文 key | testId | MVP 优先级 |
| --- | --- | --- | --- |
| 总览 | overview | pdig.nav.infra.overview | 高（Globe 舞台） |
| 卡片 | cards | pdig.nav.infra.cards | **最高（v0.x 双入口之一）** |
| 号码 | numbers | pdig.nav.infra.numbers | **最高（v0.x 双入口之一）** |
| 账户 | accounts | pdig.nav.infra.accounts | 中（collection + focused detail + Impact Lens） |
| 邮箱 | emails | pdig.nav.infra.emails | 中（recovery-aware focused detail） |
| 设备 | devices | pdig.nav.infra.devices | 中（physical access endpoint detail） |
| 服务 | services | pdig.nav.infra.services | 中（dependency endpoint detail） |
| 薄弱点 | weaknesses | pdig.nav.infra.weaknesses | 中 |

> 重要：卡片、号码是二级页面，**不是**一级导航。

## 4. 屏幕路由（Screen Registry）

| Screen id | 路径语义 | 说明 |
| --- | --- | --- |
| now | /now | Now：当前待办 / Review / Active Change / Upcoming + world context |
| review | /review | Proposal / Candidate / Drift 人工确认；Up → Now |
| me | /me | 我的数字生活（关键身份、连续性概览、隐私与个人偏好） |
| sources | /me/sources | 数据源与事实边界 |
| establish-import | /me/sources/import | 建立基础设施：本机导入参考；Up → 数据源 |
| manual-establish | /me/sources/import/manual | 手工记录设计参考；Up → 建立基础设施 |
| manual-relationship | /me/sources/import/manual/relation | 手工记录关系设计参考；Up → 手工记录 |
| settings | /me/settings | 隐私、显示与工作区偏好 |
| personalization-center | /me/settings/personalization | 个性化中心 |
| infrastructure-overview | /infrastructure | Globe 舞台 + 地区资产 + 右活动轨 + 快速入口 |
| infrastructure-cards | /infrastructure/cards | 卡片管理 |
| infrastructure-numbers | /infrastructure/numbers | 号码管理 |
| infrastructure-accounts | /infrastructure/accounts | 账户 collection |
| infrastructure-emails | /infrastructure/emails | 邮箱 collection |
| infrastructure-devices | /infrastructure/devices | 设备 collection |
| infrastructure-services | /infrastructure/services | 服务 collection |
| infrastructure-weaknesses | /infrastructure/weaknesses | evidence-backed 弱点 |
| card-detail | /infrastructure/cards/{id} | 卡片 identity / lifecycle / Impact Lens |
| number-detail | /infrastructure/numbers/{id} | 号码 identity / lifecycle / Impact Lens |
| account-detail | /infrastructure/accounts/{id} | access/control identity / Impact Lens |
| email-detail | /infrastructure/emails/{id} | communication/recovery identity / Impact Lens |
| device-detail | /infrastructure/devices/{id} | physical access identity / Impact Lens |
| service-detail | /infrastructure/services/{id} | dependency endpoint / Impact Lens |
| card-customization | /infrastructure/cards/{id}/customize | 卡面 PresentationProfile |
| number-customization | /infrastructure/numbers/{id}/customize | 号码面 PresentationProfile |
| change-center | /change | 进行中 / 准备改变 / 维护与核对 |
| change-card | /change/card | 换卡 continuity 流程 |
| change-phone | /change/phone | 换号 continuity 流程 |
| records | /records | 已发生 / 已完成 / 已验证 / 待验证 evidence trace |

## 5. 聚焦二级工作面（Focused Secondary）

这些页面不是一级或 Rail 常驻目的地：

| 页面 | 产品父级 | 入口 |
| --- | --- | --- |
| 待复核 | 现在 | Now task / Data Sources / Search |
| 建立基础设施 | 数据源 | Data Sources / Search |
| 手工记录 | 建立基础设施 | Establish / Search |
| 手工记录关系 | 手工记录 | Manual Establish / Search |
| Card/Number/Object Detail | 对应 Infrastructure collection | 列表 / Search |
| Change Phone / Card | 变更 | Change Center / object Impact CTA |
| Studio / Appearance | 对象详情 | 小型 presentation action |

Focused Secondary 在手机可隐藏 bottom nav；Header Up 返回稳定产品父级，System Back 保持真实访问历史。

## 6. 地区（Region）概念（Presentation Layer）

`RegionPresentation { regionCode, displayName, latitude, longitude, cardCount, phoneCount, accountCount, serviceCount, attentionCount }`

- 值只由现有真实数据计算；无数据 = 不显示 / 0
- 支持地区：中国大陆 CN、香港 HK、澳门 MO、英国 GB、美国 US、新加坡 SG（未来扩展）
- 分组（presentation only，不改 node 实际 region）：中国大陆 / 港澳 / 欧洲 / 北美 / 东南亚 / 自定义

## 7. Globe 状态机（Interaction Contract 摘要，详见 INTERACTION_CONTRACT.md）

`GLOBAL → REGION_HOVER → REGION_SELECTED → REGION_DETAIL`，Escape 回退。

## 8. 不可见导航原则

- Globe 是**导航增强**，不是唯一导航：必须有 Region List 等非视觉替代（screen reader / 键盘可达）
- 五个一级目的地在一级/root 上下文始终可直接到达（Desktop/Tablet rail / Mobile bottom nav）
- 手机进入详情、Studio、搜索等**聚焦子流程**时允许临时隐藏 bottom nav，System Back / Header Up 必须可恢复到所属一级上下文
- 顶部头像可以快捷进入「我」，但不得替代第五个一级导航
- 不允许「只能点地球才能到香港资产」

## 9. 页面导航顺序默认值

- 应用启动（有数据）：now 优先；有 P0 风险时 now 直接呈现 attention
- 基础设施默认：overview（globe 舞台）