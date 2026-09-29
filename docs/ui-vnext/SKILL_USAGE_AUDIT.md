# SKILL_USAGE_AUDIT.md — Skill 使用审计（No-Vision 约束下）

> 2026-09-29 · feat/pdig-ui-vnext · 三个 Skill 均**已装未动**（不重装/不更新）
> **所有 recommendation 只用于 interaction / accessibility / density / typography / layout logic / anti-pattern / platform adaptation，
> 不用于任何视觉自判**（不得凭"好看"描述写 PASS）。

## 1. Skill Registry

| Skill | 访问方式 | 本轮用途 |
| --- | --- | --- |
| `ui-ux-pro-max` | 内置 dbs 检索（spatial UI / glassmorphism / status 通道 / keyboard-first） | Globe 空间层参考（数据面保持 solid）；状态三通道；桌面键盘优先 |
| `impeccable` | Operate 模式（scanability / consistency / native expectations） | 交互/密度/反模式核对；卡片政策；暗色对比底线 |
| `frontend-design` | subject-matter grounding | 数字基础设施语言；避免 SaaS-card-kit 泛化 |

## 2. Recommendation → 决策表（≥6 条）

| # | Recommendation | Source skill | 决策 | 原因 | 实现 |
| --- | --- | --- | --- | --- | --- |
| 1 | 状态不可只用颜色：icon + label + color 三通道 | ui-ux-pro-max（accessibility 检索） | **ACCEPTED** | 色盲/屏幕阅读器可读；spec §86 状态语义 | `StatusBadge`（desktop VNextKit）icon+label+color；语义色仅 positive/warning/critical/unknown |
| 2 | 桌面键盘优先：全部可达 + 可见 focus + 按视觉顺序 Tab | ui-ux-pro-max（desktop 检索） | **ACCEPTED** | Desktop 是锚点端；键盘是首要输入 | focusable rail + top command + 内容 + Region List 焦点序（INTERACTION_CONTRACT §5） |
| 3 | 桌面密度：列表行 40–48、master-detail、紧凑可密度 | ui-ux-pro-max（desktop/productivity） | **ACCEPTED** | 数据面 L3–L5 高可读 + 高密度；不为高密度列表硬套卡片 | Numbers 桌面默认 list-table + inspector（LAYOUT_CONTRACT numbers）；Cards grid/list 切换 |
| 4 | 空间感（glass/blur）只限环境层；数据面不可被玻璃模糊 | ui-ux-pro-max（Spatial UI / Glassmorphism 检索） | **ACCEPTED（带边界）** | DESIGN_TOKENS colorRule：L0–L2 可 glass，L3–L5 必须 solid | surfaceGlass 仅用于 L2 导航 chrome；L3+ 面板用 surface/surfaceRaised 实底 |
| 5 | 卡片政策：只有独立可操作对象才是卡（Card Policy） | impeccable（distill / anti card-pill 泛滥） | **ACCEPTED** | 沿用 v0.3.1 Card Policy；VISUAL_DNA Anti-DNA §3 | AssetCard 只用于卡片/号码身份面；数据列表走表格/行（numbers/card-detail） |
| 6 | 暗色对比底线：正文 ≥4.5:1、大字 ≥3:1、muted 元信息可略低但必须可读 | impeccable（craft-floor dark contrast） | **ACCEPTED** | token contrast.bodyTextMin/largeTextMin | textPrimary/textSecondary/textMuted 层级（计算对比见 ACCESSIBILITY_AUDIT.md §3） |
| 7 | 原生期望优先：不把桌面像素布局复制到移动端 | impeccable（native expectations）+ RESPONSIVE_CONTRACT | **ACCEPTED** | spec §88 / RESPONSIVE_CONTRACT rules | 移动端 bottom nav ≤5、compact globe hero、bottom-sheet 编辑器（PLATFORM_ADAPTATION.md） |
| 8 | 数字基础设施语言：避免 SaaS-card-kit 泛化 | frontend-design（subject-matter grounding） | **ACCEPTED** | 产品 = Personal Digital Infrastructure，不是营销 SaaS | 文案/密度/结构沿用 infrastructure 语义（现在/基础设施/变更/记录） |

## 3. 明确 REJECTED（不用于实现）

| # | Recommendation | Source skill | 决策 | 原因 |
| --- | --- | --- | --- | --- |
| R1 | 营销 Hero+CTA 页 pattern | ui-ux-pro-max | **REJECTED** | 产品是 Operate 工具，不是营销页 |
| R2 | 远程字体（Outfit/Work Sans 等） | ui-ux-pro-max | **REJECTED** | policy.noRemoteFonts = true；CJK 用系统字体 |
| R3 | 卡片套卡片 / 全民 pill 圆角 | impeccable | **REJECTED** | 沿用 Card Policy；圆角只 sm6/md10/lg14/xl18 |

## 4. 诚实声明

以上 8 条采纳均落在**契约/代码/证据**上（组件、testId、token、行为状态机），
Skills 的输出**未参与**任何"是否好看 / 是否还原"的判定。
