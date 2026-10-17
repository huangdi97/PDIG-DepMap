# PHASE1F_SKILL_USAGE.md

> PHASE 1F brief §52：三个已装技能必须实际加载并应用。本文记录每个技能在本轮的**具体应用点**（不是泛泛引用）。

## 1. `ui-ux-pro-max`（accessibility / density / responsive / desktop interaction）

已加载技能（SKILL.md + 本地搜索工具）并实际用于：

- **Accessibility（§47）**：
  - 空态 CTA / Studio 控件 / Continuity 投影选择器所在屏幕的键盘可达性 → `Phase1FKeyboardContractTest`（paletteRoundTrip + 中文检索 + Enter 执行到 Change Phone）。
  - 状态三通道（icon+label+color）沿用既有 StatusBadge；healthy 文案禁止伪安全词（§40 禁词断言）。
  - 启动即刻请求键盘焦点（`VNextShell` focusRequester）——键盘优先桌面应用的可达性改进。
- **Density（§12 / §25）**：
  - Card Detail 下区从「全宽横排 admin sections」改为两栏信息工作区，rows + dividers + compact panels，压缩信息密度、减少边框盒堆叠。
  - 服务节点从「小圆点 + 小字」改为 130–170px 信息盒（glyph/名称/关系/状态 pill）。
- **Responsive（§48）**：mechanical 五档（1280/2560/1.25/1.5）渲染证据 + 布局 probe（scene/studio preview/OLD-NEW/node 尺寸契约）。
- **Desktop interaction**：hover/focus/pressed/selected 在既有 VNextInteraction 体系内保持；投影选择器（current/transition/after）可点 + 键盘 route。

## 2. `impeccable`（critique / distill / polish / harden / clarify）

已加载技能（SKILL.md；context.mjs 会话上下文已执行）并实际用于：

- **critique**：对 1E 证据的盲审结论——「弱卡 = 近黑空占位」（CMB/ICBC/Monzo 等）→ 认定为 Card Identity 缺失，作为 Phase 1F 主问题 #1 处理。
- **distill**：Number Detail 从「卡语法」（rounded-rect + 左右色块）distill 为通信语法（区号锚点 + 通信线路 + 角色簇 + 恢复路径 + continuity ring），只保留语义必要的元素。
- **polish**：ContinuityScene 权重反转（Old/New 主、服务次级、路径最弱 2.5/2/1.5px）；After 态 old 强淡出 + 投影徽标「计划完成后的预期状态 / 不代表已经完成或验证」；风险提示收窄为 72% 宽；Studio 舞台 vingette/地板光。
- **harden**：自定义背景消费者流程（选择/替换/移除 + 缩略图 + 不暴露 path/hash）；空态 6 种 + Unknown ≠ none 文案；`Phase1FEmptyCopy` 文案契约 + 禁词测试防漂移。
- **clarify**：健康文案改写为诚实表述（目前没有需要立即处理的已确认事项 / 仍可能存在尚未记录或确认的关系），删除信息噪声（vNext、技术内部标签）。
- 盲实现纪律：不自判 VISUAL_CRAFT=PASS；真实窗口 Ctrl+K / 中文 IME 无法可靠自动化 → 如实记 HUMAN_GATE。

## 3. `frontend-design`（asset identity / composition / visual hierarchy / anti-generic）

已加载技能（SKILL.md）并实际用于：

- **asset identity**：Card Identity System（CardIdentity.kt）——每张 demo 卡 = 独立 palette + accent + motif（§9 issuer 冻结）；Monzo 深炭+珊瑚带、Revolut 玻璃色散、Capital One 午夜+红 sweep 等均非「生成式默认」；全部 synthetic、token 色 mix，不复制官方卡面。
- **composition**：Card Detail 两栏工作区、Studio 20/55/25 三栏、ContinuityScene 卫-节点-主位构图（0.155/0.845 位、服务 2×2）。
- **visual hierarchy**：ContinuityScene 权重层级（对象>路径），路径 2.5/2/1.5px 严格弱于对象；Number Detail 区号 +86 为最强视觉元素（36sp）。
- **anti-generic**：避免「圆角卡片堆 + 单一渐变」套路——空态改为 max 600px 紧凑组合 + 语义剪影；健康态不是「一切安全」横幅；网格第三行明确「刻意延续」标注而非意外裁剪。
- 卡片/号码 artwork 全部程序化 + 本地（无远程图片、无新资产下载），符合 brief §56。

## 结论

三个技能均在真实代码路径上生效：`ui-ux-pro-max` 提供可达性/密度/响应式检查框架，`impeccable` 提供评估/打磨/加固纪律，`frontend-design` 提供身份与构图标准；最终证据为 contract tests + probe + 截图 + 真实窗口 smoke。
