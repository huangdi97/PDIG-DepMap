# PDIG_UIUX_DIRECTION_FREEZE.md

> PDIG v0.3.1 UI/UX Refinement · 2026-09-28 · **Visual Direction Freeze**（spec §65、§20–§23）
> 本文件冻结本轮唯一视觉方向；实现阶段不得中途随机换风格。若与 Canonical Master 冲突，以 `spec/` 为准（本文件只描述表现层）。

## 0. Product Register / Mode

- Register：**PRODUCT REGISTER**（高信任基础设施工具；非 BRAND/MARKETING register）
- Impeccable mode：**Operate**（ksucceeded = 用户在换卡/换号前看懂影响并行动）
- 一句话定位：**Quiet Infrastructure / Calm Control Plane —— 静默基础设施**

## 1. Design Principles（从 Baseline Audit 反推）

1. **信息优先，包装退后**：能进列表/行/分割线的，不进卡片；能一屏完成的不分两屏。
2. **安静优先**：neutral-dominant；indigo 是重点（accent）不是底色；语义色只表达状态。
3. **结构即信息**：Continuity Rail 用结构表达「先→后→验证→闸门」，不靠装饰图形。
4. **桌面就是桌面**：sidebar + master-detail + 密度 + 键盘；**移动就是移动**：原生导航/触控。
5. **状态永不只靠颜色**：icon/shape + label + color（P0 Gate）。
6. **诚实表达未知**：unknown ≠ healthy；没有「安全分」；Empty 讲清「这是什么 / 为何空 / 下一步」。

## 2. Visual Tone

calm · precise · trustworthy · structured · private · native · quiet · clear · infrastructure-grade · human

明确不是：cyberpunk / hacker console / bank dashboard / generic SaaS / crypto dashboard / AI assistant / graph explorer / admin console。

## 3. Signature Motif —— Continuity Rail（唯一品牌记忆点）

- **语义**：表达 current state → prerequisite → transition → verification checkpoint → completion。
- 应用位置：Replace Phone 全链、ChangePlan 步骤、Verification 检查点、Recovery Path 审阅、Timeline 摘要。
- 形态（各平台原生实现）：纵向/横向轨道 + 步骤节点（编号或图标）+ 连接线 + 验证勾/闸门块 + 「完成/待验证/阻止」状态色（icon+label+color）。
- Make-Before-Break 语义：新路径未验证 → 旧路径停用动作 = **禁用 + 明文原因**（"新手机号验证通过后才能停用旧手机号"），绝不只禁用。
- 它**不是**装饰图；Graph/网络图仍不是主 UI（spec §23）。

## 4. Home = Personal Infrastructure Briefing（非 Dashboard of boxes）

信息顺序（自上而下；不存在的块给出「清楚的范围说明」，不显示空卡片）：

1. 需要你处理（attention/action 行，图标+文案+去往）
2. 基础设施薄弱点（findings 摘要：1-3 条强摘要 + 去 Findings）
3. 可能发生了变化（drift/待确认 counts 的紧凑行）
4. 即将到来（timeline upcoming 小结）
5. 常用场景（场景入口，2 列 action row）
6. 我的基础设施（Infrastructure 摘要 + 去 Infra）

Healthy state：不显示 "0 issues" 或健康分；显示「当前没有需要立即处理的事项 + 最近检查范围 + 仍然未知的范围 + 可主动准备的场景」。

## 5. Palette 策略

- 尊重既有 indigo 身份：**light primary #4C4FD8 / dark #8B8EF5 不动**（品牌色变化必须 intentional + documented + contrast-tested；本轮经 ui-ux-pro-max 验证属于 Trust-blue family，采纳保留）。
- 可调：neutral foundation（背景微冷灰、surface 打字、border 分离）、surface hierarchy（surface/surfaceVariant/剥一层 surfaceLow）、状态色 contrast、dark 值。
- 禁止：purple→blue AI 渐变、渐变文字、neon glow、玻璃拟态泛滥、饱和度渐变汤。
- 屏幕中 indigo = accent 而非底色；中性色主导。

## 6. Typography

- 遵守 `noRemoteFonts` + CJK = system（spec §42；不下载字体）。
- 层级靠 size/weight/spacing/line-height/内容结构，不换字体：
  - Page title: 24/600（桌面）或 20/700（移动）
  - Section header: 15/600 + 底部 hairline
  - Body: 14/400（桌面密度）/ 16（移动）
  - Secondary/meta: 12.5/400
  - Status/label: 12/500，icon 搭配
- 行宽：桌面内容 ≤ 680px；master-detail 右窗可按内容放宽到 ≤ 720px。

## 7. Spacing / Layout / Density

- 基准 4pt 网格；桌面密度：页面 padding 20，section gap 24，行高 ≥ 36（可点到 40）；移动 16 base。
- Desktop：1920+ 用 max-width 内容区（≤ 1200-1400px 居中或左侧锚定）+ sidebar 220px；不再全宽单列。
- Infrastructure：master-detail（左：item/capability 列表 280px；右：Detail pane，含 identity/capabilities/dependencies/evidence/recovery/related scenarios）；**By Item / By Capability 分段切换**（segmented/tab UI，不暴露 enum）。
- Radius：分层 —— card/sidebar 8；chip 6；按钮 6-8；输入 8；对话框 12；不再全民 14/pill（spec §44）。

## 8. Surface / Border / Elevation

- Surface 层级（light）：background #F5F6FA → surface #FFFFFF → elevated #FFFFFF + 细分 border；用 **border 分离**（1px #E6E8F0）替代「每个都填 surfaceVariant」。
- 卡片只在真正独立可操作对象上用；列表行用 hairline 分隔。
- 禁止 card-in-card；禁止每节彩色底。

## 9. Status 体系（spec §46）

| 状态                   | icon/shape         | label（copy-zh）         | color                          |
| ---------------------- | ------------------ | ------------------------ | ------------------------------ |
| blocked（必须处理）    | ⛔/! (danger icon) | 还有必须处理的事项       | danger                         |
| review_required        | ⚠                  | 还有信息需要确认         | warning                        |
| ready_with_known_scope | ✓                  | 基于当前信息，可以继续。 | success                        |
| verifying              | ↻（clock）         | 待验证                   | info                           |
| verified               | ✓✓（双勾）         | 已验证                   | success（强）                  |
| completed（≠verified） | ✓ 单勾             | 已完成                   | textSecondary（弱于 verified） |
| unknown                | ?                  | 还不了解                 | textTertiary                   |
| cancelled              | –                  | 已取消                   | textTertiary                   |

verified 必须视觉与语义上都强于 completed（spec §36）。影响页 must_change 视觉突出 ≠ needs_review 为 warning（spec §33）。

## 10. Card Policy（spec §43）

- 默认 NOT everything is a card。Card 保留给：真正独立可操作对象（如一条探测出的关键路径卡片）、对话框内内容块。
- 其余用：section header / list row / divider / group / inline status / master-detail。
- 首页禁 8 张同形卡。

## 11. Navigation

- Desktop：左侧 sidebar（220px）：PDIG 品牌块 + 分组导航（概览：首页/需要处理；数据：数据来源/导入；检查：基础设施/薄弱点/待确认;变更：场景中心/时间线；维护：备份/设置/安全），选中态 = indigo 左侧标记 + 文字加粗；内容区 top bar 显示当前页标题 + 当前文件状态（去掉文件名，只显示「本地数据文件已就绪」）。
- Android：bottom nav ≤5（首页/基础设施/场景中心/设置）或 NavigationRail tablet；保持现有单实例导航；状态栏 backdrop。
- iOS：NavigationStack + List；Infrastructure 用 NavigationSplitView（iPad）。
- Harmony：原生；不复制 Android。

## 12. Findings 结构（spec §26）

默认（what/why/next）：标题行 + 一句人话解释 + 「下一步建议」动作。
展开（evidence/unknown/affected）：confirmed basis（icon+label）、unknowns、affected capability/paths。
禁止只靠颜色区分；每条 finding 至少含 title + explanation + affected capability + confirmed basis + unknowns + next action（可经展开）。

## 13. Scenario Center / Replace Phone（spec §30–§32）

- Scenario Center 按用户意图分 Payment / Identity & Recovery 两区（active：更换银行卡/银行卡即将到期/注销银行卡/更换手机号），每场景「什么时候用/检查什么/大约步骤」。未来能力不显示。
- Replace Phone flagship: Continuity Rail × 全链（选旧号 → 查看影响 → 恢复路径 → 共享故障点 → 建立新路径 → 验证 → 迁移关键账户 → 再次验证 → 停用旧号），make-before-break 闸门如上。

## 14. Motion（spec §48）

- 原则：meaningful / short / interruptible / native / respect reduced motion。
- 允许：步骤推进反馈（rail 节点动画 150-250ms）、验证完成的确认、展开/收起、导航切换（fade/slide 200ms）。
- 禁止：装饰性恒定动画、parallax、滚动劫持、到处 spring、入场特效堆叠。
- Compose：animateColorAsState / AnimatedVisibility 简短使用；Android 尊重系统 reduce motion；iOS 用原生 implicit；Harmony 尊重系统。

## 15. Accessibility（P0 Gate，spec §71）

- 正文对比 ≥4.5:1、大字 ≥3:1（light/dark 都过）；触控 iOS 44pt / Android 48dp；键盘全流程 + focus 可见；screen reader label 全交互元素；状态 icon+label+color；reduced-motion；large text 不得 clip/遮 CTA。

## 16. Responsive（spec §74）

- Desktop: 1280×720 / 1920×1080 / 2560×1440 / 125% / 150% 全绿。
- Android: phone/tablet + light/dark + fontScale。
- iOS: iPhone/iPad + light/dark + Dynamic Type。
- Harmony: 能运行则 phone/tablet；否则 EXTERNAL_GATE + source evidence。

## 17. Anti-Patterns（本轮红线）

- generic AI UI（同形卡墙、All-caps 标、middle-dot meta、mono 数据标签、→ 后缀按钮）
- card-in-card / pill 汤 / radius 14 全用 / 每节彩色底
- 安全分 / 0 issues 空首页 / unknown→ready 语义漂移
- 技术标识泄漏（graphRevision、FD-003、plan-…、SHARED_FAILURE_DOMAIN、RECOVERY_CYCLE、debug 来源类名、depmap 文件名）
- 每平台抄另一个平台的像素布局

## 18. 冻结声明

上述 §1–§17 为冻结方向；实现阶段每个平台按其原生系统映射同一语义（品牌/IA/术语/优先级/CTA 意义/状态意义/进度语义一致；组件形状/pixel 不必一致，spec §53）。
