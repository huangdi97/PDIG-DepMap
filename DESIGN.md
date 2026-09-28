# DESIGN.md — PDIG（UI/UX 视觉方向）

> **These files are UI/UX context. They DO NOT override the PDIG Canonical Master (`spec/`).**
> 完整规范见 `docs/uiux/PDIG_UIUX_DIRECTION_FREEZE.md`（冻结方向）与 `docs/uiux/PDIG_DESIGN_SYSTEM.md`（设计系统）。

## 1. 视觉方向（冻结）

**Quiet Infrastructure / Calm Control Plane —— 静默基础设施**

- 关键词：calm · precise · trustworthy · structured · private · native · quiet · clear · infrastructure-grade · human
- 明确不是：cyberpunk / hacker console / bank dashboard / generic SaaS / crypto dashboard / AI assistant / graph explorer / admin console。

## 2. 唯一 Signature Motif：Continuity Rail（连续性轨道）

- 语义：current state → prerequisite → transition → verification checkpoint → completion。
- 用在：Replace Phone 全链、ChangePlan 步骤、Verification 检查点、Recovery Path、Timeline 摘要。
- 是语义可视化，不是装饰图。Make-Before-Break：新路径未验证 → 停用旧路径 = 禁用 + 明文原因。

## 3. 视觉语言要点

- 品牌：indigo 主色（light #4C4FD8 / dark #8B8EF5）不变；neutral-dominant，indigo 是 accent 不是底色。
- 字体：system CJK（禁远程字体）；层级靠 size/weight/spacing，不换字体。
- 表面：surface + 1px border 分离，替代「每个都填 surfaceVariant」；卡片只在真正独立对象上用。
- 半径：6/8/12 分层（不再全民 14/pill）。
- 状态：icon/shape + label + color 三通道，永不只靠颜色；verified 强于 completed。
- 空态：这里是什么 / 为什么为空 / 下一步能做什么。
- Motion：meaningful / short / interruptible / respect reduced-motion。

## 4. 导航与布局

- Desktop：220dp 分组 sidebar + top bar + max-width 内容；Infrastructure = master-detail + By Item / By Capability。
- Android：Compose/M3，bottom 导航 ≤5 或 rail；触控 ≥48dp。
- iOS：SwiftUI 原生，NavigationStack/List，触控 ≥44pt，Dynamic Type。
- Harmony：ArkTS/ArkUI 原生，不复制 Android。

## 5. 红线

- 无安全分数 / 0 issues 空首页 / unknown→ready 漂移。
- 无技术标识泄漏（graphRevision、FD-003、plan-…、SHARED_FAILURE_DOMAIN、RECOVERY_CYCLE、内部 id、depmap 文件名、原始错误）。
- 不抄另一平台像素；每平台原生实现同一语义。

## 6. 参照

- 冻结方向：`docs/uiux/PDIG_UIUX_DIRECTION_FREEZE.md`
- 设计系统：`docs/uiux/PDIG_DESIGN_SYSTEM.md`
- 品牌 token：`spec/ui/design-tokens.json`（v1.1）
- 文案：`spec/ui/copy-zh.json`（新增 `uiuxV031` 块）