# ANDROID_REFERENCE_MAPPING.md

> Desktop Frozen Reference → Android 翻译映射（任务书 §43）。
> 每屏：PRESERVE / TRANSLATE / DROP / PLATFORM-ADAPT 四项判定 + 理由。
> 依据：`DESKTOP_REFERENCE_FREEZE_MANIFEST.json`（12 屏冻结）、`spec/ui-vnext/`、`ANDROID_VISUAL_CONTRACT.md`。
> 原则：保留 information hierarchy / object identity / semantic prominence / material family /
> state semantics / continuity semantics / navigation intent；Android 原生化交互与排版。

## 1. Now（现在）

- Desktop Now → Android Phone Now → Android Tablet Now
- Globe hero（Global Infrastructure Navigator）+ 需要处理 + 进行中的变更 + 即将到来。

| 项                               | 判定           | 说明                                                                      |
| -------------------------------- | -------------- | ------------------------------------------------------------------------- |
| Globe 主舞台（强主角）           | PRESERVE       | 纹理地球 + region markers + 弧线 + 大气；手机 hero 高度 280dp，展开 360dp |
| 需要处理列表                     | PRESERVE       | AttentionRow（icon+label+color 三通道），critical 最突出                  |
| 进行中的变更                     | PRESERVE       | ActiveChange 卡 + 阶段标签                                                |
| 即将到来                         | PRESERVE       | 到期提醒行 + 天数徽标                                                     |
| 三列并排                         | PLATFORM-ADAPT | EXPANDED（≥1200dp）三列；COMPACT/MEDIUM 纵向 feed                         |
| 空态（无关注/无变更/无即将到来） | TRANSLATE      | honest unknown 空态（未记录 ≠ 无风险）                                    |

## 2. Infrastructure Overview（总览）

- Desktop Overview → Android Phone Overview → Android Tablet Overview
- Globe Stage + 活动轨（Region List 非视觉替代 + 需要处理）+ 快速入口。

| 项                                    | 判定      | 说明                                       |
| ------------------------------------- | --------- | ------------------------------------------ |
| Globe Stage（L1 视觉主导）            | PRESERVE  | 点击聚焦 / 再次点击抽屉 / 滚轮缩放（触控） |
| 活动轨（Region List + 需要处理）      | PRESERVE  | RegionListItem 同时是无障碍非视觉替代      |
| 快速入口（查看卡片/号码/更换/薄弱点） | TRANSLATE | 从桌面横排条目改为触控大按钮行             |
| 地区选中态（region-selected）         | PRESERVE  | globe.REGION_SELECTED + regionFilter       |
| 空态（无地区数据）                    | TRANSLATE | EmptyState(REGION)，不伪造地区             |

## 3. Cards（卡片）

- Desktop Cards Grid → Android Phone Cards → Android Tablet Cards

| 项                                                     | 判定           | 说明                                                              |
| ------------------------------------------------------ | -------------- | ----------------------------------------------------------------- |
| CardIdentitySystem（issuer identity/材质/状态/遮蔽）   | PRESERVE       | AssetCard 程序化卡面，preset 驱动                                 |
| issuer 可区分（CMB/ICBC/BOC/HSBC/BOCHK/Monzo/Revolut） | PRESERVE       | fixture 含全部 7 家，卡面按 issuer 语义配色                       |
| Grid/List 切换                                         | PLATFORM-ADAPT | 桌面网格；手机默认 2 列，MEDIUM 3 列，EXPANDED 4 列；列表视图保留 |
| 地区过滤                                               | TRANSLATE      | 桌面顶部过滤；手机 FilterChip 行                                  |
| Cards Empty                                            | TRANSLATE      | 语义空态（未记录 ≠ 无风险；绝不推断）                             |

## 4. Card Detail（卡片详情）

| 项                    | 判定           | 说明                                   |
| --------------------- | -------------- | -------------------------------------- |
| 顶部先见卡片视觉身份  | PRESERVE       | Hero AssetCard + identity 列           |
| Identity/状态/Actions | PRESERVE       | 卡组织/地区/币种/卡种/形态/有效期/状态 |
| 绑定服务              | PRESERVE       | servicesForCard（真实关系）            |
| 备用支付 / 影响与风险 | TRANSLATE      | 手机纵向堆叠；Unknown != safe 文案保留 |
| 变更历史              | PRESERVE       | 最近记录摘要                           |
| 两栏（33/67）         | PLATFORM-ADAPT | EXPANDED/MEDIUM 两栏；COMPACT 纵向     |

## 5. Card Studio（卡面定制）

| 项                                                          | 判定      | 说明                                            |
| ----------------------------------------------------------- | --------- | ----------------------------------------------- |
| Live Preview 优先                                           | PRESERVE  | 手机 Preview 在上；大屏三栏（预设/预览/属性）   |
| 预设（glass/city/deep-space/region/metal/abstract/minimal） | PRESERVE  | 程序化背景按 preset 确定性差异                  |
| PresentationProfile 边界                                    | PRESERVE  | 仅呈现层；绝不写 .depmap；保存=本地偏好         |
| 属性/遮蔽/布局                                              | TRANSLATE | 手机为行式 Inspector，不复制桌面 Inspector 面板 |
| 证据回读                                                    | PRESERVE  | evidenceThemeId 覆盖 + 回读（expected==actual） |

## 6. Numbers（号码）

| 项                                                          | 判定           | 说明                                                                |
| ----------------------------------------------------------- | -------------- | ------------------------------------------------------------------- |
| 与 Cards 视觉语言明显不同                                   | PRESERVE       | NumberFace：拨号弧 + 信号条 + preset 背景（communication identity） |
| 一眼可见：dial code/carrier/role/recovery/dependency/status | PRESERVE       | 高密度行式列表                                                      |
| List + Inspector                                            | PLATFORM-ADAPT | EXPANDED/MEDIUM 两栏；COMPACT 列表+摘要                             |
| Numbers Empty                                               | TRANSLATE      | 空态（未记录 ≠ 无风险）                                             |

## 7. Number Detail（号码详情）

| 项                         | 判定     | 说明                                          |
| -------------------------- | -------- | --------------------------------------------- |
| Dial Code 最强             | PRESERVE | NumberFace hero（mono 号码 + dial code chip） |
| 状态/角色/用途             | PRESERVE | 徽标行 + 用途                                 |
| 恢复能力                   | PRESERVE | recoveryOnly →「唯一恢复路径」高风险语义      |
| 关联服务/登录/2FA/恢复依赖 | PRESERVE | 关系列表（authenticates/twoFA）               |
| 历史                       | PRESERVE | 摘要行                                        |

## 8. Number Studio（号码面定制）

| 项                             | 判定     | 说明                                                                                |
| ------------------------------ | -------- | ----------------------------------------------------------------------------------- |
| communication identity 语言    | PRESERVE | 拨号弧/信号条/预设背景（country/city/minimal/banking/travel/recovery/work/private） |
| Travel/Banking/… = 呈现 preset | PRESERVE | 绝不改变 PersonalReality role truth（提示文案显式）                                 |
| 证据变体                       | PRESERVE | country != travel != recovery（SHA 互异已测）                                       |

## 9. Change Phone（更换手机号）

| 项                                           | 判定      | 说明                                                            |
| -------------------------------------------- | --------- | --------------------------------------------------------------- |
| Current / Transition / After                 | PRESERVE  | 三投影选择器 + 语义横幅                                         |
| Old → Service Nodes → New（服务节点 > 连线） | PRESERVE  | 纵向连续性流；不做 spaghetti lines                              |
| Make-Before-Break                            | PRESERVE  | 阶段 6 阻塞 + 明文原因                                          |
| After = Plan Projection                      | PRESERVE  | 显式「计划投影 ≠ 现实」横幅；旧号 ghost；未完成服务仍「待处理」 |
| ContinuityRail 6 节点                        | TRANSLATE | 桌面横向 rail；手机允许横向滚动                                 |

## 10. Empty States（空态）

| 项                                                                      | 判定     | 说明                                  |
| ----------------------------------------------------------------------- | -------- | ------------------------------------- |
| semantic illustration + title + description + CTA                       | PRESERVE | EmptyState 组件（6 类语义图标）       |
| honest unknown 文案                                                     | PRESERVE | 无「一切安全 / 100% safe / 没有问题」 |
| 覆盖：Cards/Numbers/Region/No Change/No Attention/No Known Dependencies | PRESERVE | 已接线                                |

## 11. Search / Command

| 项                    | 判定             | 说明                                                                 |
| --------------------- | ---------------- | -------------------------------------------------------------------- |
| command/search intent | PRESERVE         | 触控入口（TopCommandBar）→ SearchScreen                              |
| 真实搜索范围          | PRESERVE         | card/number/region/service；导航命令常驻                             |
| Ctrl+K                | DROP（可选保留） | 桌面键盘 primitive；Android 主入口为触控（物理键盘可选支持，未实现） |

## 12. Navigation / Shell

| 项                                  | 判定           | 说明                                                                 |
| ----------------------------------- | -------------- | -------------------------------------------------------------------- |
| 一级导航（现在/基础设施/变更/记录） | PLATFORM-ADAPT | 手机 BottomNav（4 项 ≤5）；展开 NavigationRail                       |
| 基础设施二级（8 项）                | PLATFORM-ADAPT | 手机 InfraChipRow（横向滚动）；rail 内嵌小节                         |
| Window Size Class                   | PLATFORM-ADAPT | COMPACT(<700dp) / MEDIUM(700–1199) / EXPANDED(≥1200)；≥600dp 用 rail |
| System back                         | PLATFORM-ADAPT | BackHandler：detail/studio/search → 返回上一层；region drawer → 关闭 |
| 状态恢复                            | PLATFORM-ADAPT | VNextShellViewModel（旋转/重建保留导航/选择/投影）                   |

## 13. 视觉 / 主题

| 项                                                  | 判定           | 说明                                                    |
| --------------------------------------------------- | -------------- | ------------------------------------------------------- |
| deep navy/black foundation + controlled blue accent | PRESERVE       | PdigV2Colors 全 token 映射                              |
| 语义绿/琥珀/红                                      | PRESERVE       | Positive/Warning/Critical（icon+label+color 三通道）    |
| 低噪声 surface                                      | PRESERVE       | 无 generic Material 卡片堆砌                            |
| Globe 真实地球视觉                                  | PRESERVE       | bundled NASA 纹理（albedo/night/cloud）+ day/night/云层 |
| Material 交互基座                                   | PLATFORM-ADAPT | NavigationBar/手势/触控目标 ≥48dp                       |

## 14. DROP 清单（本轮明确不搬）

- Desktop 左 rail + 顶部二级导航布局（手机）→ 底部导航 + chips（导航语义保留）
- Desktop Studio 三栏 inspector 布局（手机）→ 预览优先纵向（编辑能力保留）
- Ctrl+K 作为唯一入口 → 触控搜索入口
- 桌面精确 1920×1080 排版 → Android 度量体系（display/headline/body/label）
