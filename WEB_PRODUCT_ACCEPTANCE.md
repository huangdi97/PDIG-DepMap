# WEB_PRODUCT_ACCEPTANCE — Web 产品验收

> 本文只回答一个问题：**用户视角的产品闭环，哪些真的成立，哪些诚实地说“没有”。**

## 1. 一级导航（冻结）

桌面：左侧导航栏（现在 / 基础设施 / 变更 / 记录 / 我）+ 中区工作台 + 右侧 Truth 面板。
紧凑：顶栏 + 主内容 + 底部 5 项导航。`E2E` 与 `layout audit` 在 6 个视口验证过这两套布局。

## 2. 已成立（TRUE，有 E2E 断言）

| 能力              | 说明                                                                                                   |
| ----------------- | ------------------------------------------------------------------------------------------------------ |
| Onboarding        | 创建本地加密空间 / 从 .depmap 恢复；无账号、无手机号验证、无云端                                       |
| Lock / Unlock     | 状态区分 `locked / unlocking / unlocked / locking / auth_failed / corrupt_vault / unsupported_vault`   |
| Inventory         | 对象创建、编辑、归档/恢复；空状态文案「这里没有记录，不代表现实中没有这些对象」                        |
| Dependency        | 用户显式确认；默认 `unknown`；`required` 只能由用户选择；retire 保留历史                               |
| DependencyGroup   | 仅 ANY / ALL，必须选≥2 条同目标同 capability 的关系                                                    |
| Findings          | 全部来自 Core `generateInfrastructureFindings`，展示 what/why/依据/未知/capability/建议/evidence       |
| Change Center     | 模板运行时读 `listActiveTemplates()`；无模板对象显示治理边界文案                                       |
| Change Plan       | create → analyze；Current / Transition / After(Plan Projection)                                        |
| stale fail-closed | `needs_revalidation` 时禁止记录完成/验证/完成计划                                                      |
| Reanalysis        | 调 Core `rebasePlan()`，展示真实 added/removed/changed 与「分析基准 X → Y」                            |
| done ≠ verified   | 记录完成不改变验证状态；验证需单独动作                                                                 |
| Make-Before-Break | 新路径未验证 → 旧路径 retire 被拒并解释原因                                                            |
| Records 三时钟    | 缺 occurredAt 时显示「发生时间未记录」，不填充 now/updatedAt                                           |
| Backup / Restore  | 导出 DEPMAP_CONTAINER_V1；恢复先验证后原子替换；失败不动旧现实                                         |
| Privacy Mask      | 文本、aria-label、title、搜索结果全部遮蔽；文案叫「隐私遮蔽」不是「防截图」                            |
| 多标签页          | 第二个标签页只读阻断                                                                                   |
| 离线              | Service Worker 缓存 shell；断网 reload → 解锁 → 读写                                                   |
| Import            | 通用 CSV（显式字段映射）与 OFX/QFX + **账单归属账户** → 商户解析 → Proposal（拒绝自动映射/自动猜商户） |
| Proposals         | 接受后才写入 Reality（`criticality: unknown`）；拒绝不是永久为假                                       |
| Drift             | 由证据产生「可能变化」，`忽略` 不改写 Reality；`确认替代/额外路径` 才是用户显式变更                    |

## 3. 诚实缺口（NOT_RUN / 明确不支持）

| 能力                           | 状态             | 说明                                                                    |
| ------------------------------ | ---------------- | ----------------------------------------------------------------------- |
| Account / Email / Service 变更 | 不支持（按设计） | 无正式 Scenario Template，UI 明确说明「不会用通用步骤替代真实变更语义」 |
| 全局 Graph                     | 不做             | 只有对象详情里的直接邻居                                                |
| 安全分/健康分                  | 不做             | Now 只显示事实计数                                                      |
| 云同步 / 账号 / 订阅           | 不做             | 无后端                                                                  |

## 4. 语义铁律的实现位置

- `Proposal ≠ Reality`：accept 才写 Dependency（`ConfirmationService.acceptProposal`）。
- `After ≠ Reality`：Plan 的 impactSnapshot 只作投影展示，不写回依赖。
- `done ≠ verified`：`ChangePlanService.completeAction` 不动 verification；`verifyActionManually` 必须 done 在先。
- `edge count ≠ independent paths`：Group 必须用户确认；Findings 的 SOF 只对 `required` 且唯一来源触发。
