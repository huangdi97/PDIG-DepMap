# PHASE1E_SKILL_USAGE.md — PHASE 1E Skill 使用记录（No-Vision 约束下）

> 2026-10-05 · feat/pdig-ui-vnext · brief §57 要求的三项技能如实记录。
> **结论：三项技能均已真实加载并应用**（本会话可调用 Skill 加载工具；与 1B/1C/1D 的“工具不可用”记录不同，本轮 Skill 工具可用）。
> 所有 recommendation 只用于 interaction / accessibility / density / typography / layout / anti-pattern / 状态机逻辑，
> **绝不用于任何“是否好看/是否还原”的视觉自判**（No-Vision 纪律 §56）。

## 1. Skill Registry

| Skill             | 访问方式               | 本轮用途                                                                                                                                    |
| ----------------- | ---------------------- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| `ui-ux-pro-max`   | Skill 加载（真实可用） | Accessibility 层级（contrast/focus/keyboard）、Touch & Interaction（hover/pressed 状态）、Navigation（Tab 顺序）、Desktop adaptive、density |
| `impeccable`      | Skill 加载（真实可用） | Operate 模式（scanability / consistency / native expectations）、critique / polish / harden 思维、craft-floor 约束                          |
| `frontend-design` | Skill 加载（真实可用） | subject-matter grounding（数字基础设施语义）、anti-generic design（避免 SaaS-card-kit 泛化）、visual hierarchy 原则                         |

## 2. Recommendation → 决策表（≤ 15 条）

| #   | Recommendation                                                        | Source skill                                            | 决策         | 原因                               | 实现                                                                                                                                              |
| --- | --------------------------------------------------------------------- | ------------------------------------------------------- | ------------ | ---------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | 状态不可只用颜色：icon + label + color 三通道                         | ui-ux-pro-max（accessibility）                          | **ACCEPTED** | 色盲/读屏可读；spec 状态语义       | `StatusBadge`（icon+label+color）沿用；`DrawerAction`/dock 均带文字                                                                               |
| 2   | Desktop 键盘优先：全部可达 + 可见 focus + Tab 顺序                    | ui-ux-pro-max（desktop）                                | **ACCEPTED** | Desktop 是键盘首要场景             | `VKeyboard.kt`（routeKey / KeyboardFocusState）；Ctrl+K 面板；Tab/Shift+Tab/Enter/Space/Escape 7 屏覆盖（`VNextInteractionContractTest`）         |
| 3   | hover / pressed / selected 三态必须有明确视觉                         | ui-ux-pro-max（touch & interaction）                    | **ACCEPTED** | 交互状态不能只有 selected 静态截图 | `VNextInteraction.kt`（collectIsHovered/pressed + InteractionSurface）；Card/NumberRow/DockAction/MaterialTile/ThemeThumb/ProjectionSelector 接入 |
| 4   | Motion 必须尊重 reduce-motion：tilt 仅 hover、关闭 idle               | ui-ux-pro-max（animation）                              | **ACCEPTED** | 减少动效用户功能不丢               | Card hover tilt（graphicsLayer 3°，非 idle）；Globe idle rotation 由 reduceMotion 关闭；无 idle 动画                                              |
| 5   | 空状态 = 邀请行动，不是“暂无数据”                                     | impeccable（onboard/clarify）                           | **ACCEPTED** | 空屏幕是行动入口                   | `EmptyState`（§45/§46 文案 + 添加卡片/号码 CTA）for Cards/Numbers                                                                                 |
| 6   | 普通 UI 禁止内部术语                                                  | impeccable（clarify）                                   | **ACCEPTED** | 用户语言优先                       | 移除 PresentationProfile/PersonalReality/make-before-break/PLAN PROJECTION 等 UI 文案；改“外观设置只改变显示方式…”                                |
| 7   | 计划投影必须有明文“不代表已完成”                                      | impeccable（harden）                                    | **ACCEPTED** | 不把计划当现实                     | After 档 badge：“计划投影 / 计划完成后的预期状态，不代表已经完成或验证”                                                                           |
| 8   | 反 SaaS-card-kit 泛化：信息型产品 premium density                     | frontend-design                                         | **ACCEPTED** | 产品 = 数字基础设施，不是营销页    | Numbers 高密度行 + 检查器；Cards 3 列资产网格；无营销式 Hero                                                                                      |
| 9   | 视觉层次：issuer/nickname 顶部、PAN 主资产、network 锚、metadata 次级 | frontend-design（visual hierarchy）                     | **ACCEPTED** | 卡片信息不能全同权                 | CardFaceContent 排版（standard/emblem/minimal-content）分层                                                                                       |
| 10  | 实体/虚拟卡克制区分（chip vs 数字标记）                               | ui-ux-pro-max（UX）                                     | **ACCEPTED** | 物理/虚拟语义差异                  | CardFaceContent：physical=CardChip，virtual=VirtualMark（无 chip）                                                                                |
| 11  | Studio 中央预览为主角（≥70% pane、近黑舞台）                          | ui-ux-pro-max（studio）                                 | **ACCEPTED** | 编辑对象是视觉主角                 | StudioFrame preview 占 center 75%、620–720px @1920；probe 断言                                                                                    |
| 12  | Inspector 分组默认只展开当前组                                        | impeccable（distill）                                   | **ACCEPTED** | 降 cognitive load                  | StudioInspector 折叠组（材质/背景/布局/强调色/信息/隐私）                                                                                         |
| 13  | 自定义背景导入必须校验（禁止 SVG/远程）                               | impeccable（harden）+ ui-ux-pro-max（forms/validation） | **ACCEPTED** | §63 安全约束                       | `LocalBackgroundImporter`（白名单 ext、size/dimension 上限、decode validation、sha256 命名、app-managed storage）；7 项测试                       |
| 14  | 持久化证据：编辑→保存→重开保留、canonical 零变化                      | impeccable（harden）                                    | **ACCEPTED** | 外观设置不能污染真相               | `PresentationProfileStore` + `PresentationProfilePersistenceTest`（4 项）+ journey PROFILE_PERSISTENCE_EVIDENCE                                   |
| 15  | 隐私遮蔽默认 ON，截图证据含 PAN/号码遮蔽                              | ui-ux-pro-max（privacy/accessibility）                  | **ACCEPTED** | 默认安全                           | `privacyMask = true` 默认；全部截图遮蔽                                                                                                           |

## 3. 未采纳 / 边界

| #   | 建议                   | 来源                        | 决策         | 原因                                                                                         |
| --- | ---------------------- | --------------------------- | ------------ | -------------------------------------------------------------------------------------------- |
| R1  | 远程字体/大视觉资产    | ui-ux-pro-max（typography） | **REJECTED** | 离线策略（零网络运行时）；沿用系统字体与 bundled assets                                      |
| R2  | 为“高级感”全面升级动画 | ui-ux-pro-max（motion）     | **REJECTED** | 仅少量有意义 motion，idle 动画关闭（§42）                                                    |
| R3  | 把视觉判定写成 PASS    | —                           | **REJECTED** | No-Vision 纪律 §56：只报 contract/runtime/geometry/evidence；审美 = NEEDS_HUMAN_FINAL_REVIEW |

## 4. 诚实声明

- 以上条目落在契约/代码/证据上（组件、testId、token、状态机、probe），Skills 输出**未参与**任何“是否好看/是否还原”判断。
- 本会话 Skill 工具可用（区别于早期 phase 的工具不可用记录）；工具可用性已如实记录在本文件。
- 视觉最终结论只能由 Human `DESKTOP_VISUAL_REFERENCE = ACCEPTED` 给出。
