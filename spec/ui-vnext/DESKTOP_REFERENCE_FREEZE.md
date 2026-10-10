# spec/ui-vnext/DESKTOP_REFERENCE_FREEZE.md

> Desktop Reference Freeze — 冻结设计契约（Human/Vision Final Review，2026-10-02，ACCEPTED）。
> 本文件是跨端翻译（Platform Translation）的 spec 层依据；不可变标识见
> `DESKTOP_REFERENCE_FREEZE_MANIFEST.json`（manifest + commit SHA，不建 tag）。
>
> **Post-freeze product IA revision（2026-10-09）**：本文件冻结的是 2026-10-02 的
> Desktop visual baseline；其中“四个 Primary Nav”的产品 IA 条目已被更晚的产品决策覆盖。
> 当前产品一级导航为 **现在 / 基础设施 / 变更 / 记录 / 我**。这不自动重写旧 Desktop
> pixel golden；跨端实现与后续 Desktop revision 必须以
> `spec/ui-vnext/IA.md` 和 `spec/ui-vnext/FIVE_PRIMARY_NAVIGATION_DECISION.md`
> 的五项 IA 为准。

## 1. 冻结范围（FROZEN）

以下设计方向已 Human 验收并冻结；任何平台翻译不得 redesign：

- **Historical Desktop visual baseline IA**：截图冻结时 Primary Nav = 现在 / 基础设施 / 变更 / 记录；
  **当前产品 IA 已在 freeze 后 revision 为五项并覆盖本条的产品语义**；
  Infrastructure Secondary = 总览 / 卡片 / 号码 / 账户 / 邮箱 / 设备 / 服务 / 薄弱点
- **Globe**：Global Infrastructure Navigator（确定性渲染；真实地球资产；非纯装饰）
- **Cards**：asset identity language（CardIdentitySystem；Grid / Detail 双尺度同渲染器）
- **Number**：communication identity language（信号条 + 拨号弧）
- **Card Studio**：consumer PresentationProfile editor（对象库 / 实时预览 / 分组编辑器）
- **Number Studio**：communication PresentationProfile editor
- **Continuity**：Current / Transition / After 三态投影
- **After**：Plan Projection —— 计划完成后的预期状态，**NOT reality**（禁止冒充已发生 / 已验证）
- **Empty State**：honest unknown semantics（紧凑 480–600px 组合，禁伪安全词）
- **Command Palette**：desktop interaction primitive（Ctrl+K 键盘导航）

## 2. Presentation Boundary（冻结）

- `PresentationProfile` ≠ `PersonalReality` ≠ `Canonical`
- PresentationProfile 只描述呈现层偏好（themeId / material / accent / background / layout / mask），
  绝不进 .depmap，绝不改变领域语义
- 平台实现可以 **translate presentation**（换平台原生导航 / 布局 / 手势 / 交互惯例）
- 平台实现**不可以**：
  - fork domain truth
  - fork Canonical
  - 改变 change semantics
  - 改变 recovery semantics

## 3. 翻译而非像素拷贝

下一阶段 = **Desktop Reference → Platform Translation**，不是 Desktop Pixel Copy → Mobile。
每个平台必须保留：information hierarchy / object identity / semantic prominence /
color·material family / state semantics / continuity semantics / navigation intent。
允许使用平台原生：navigation / sheet / bottom bar / gesture / safe area /
typography metric / window·screen density / interaction convention。

## 4. 平台优先级（只记录顺序，本轮不实现）

1. Android（`ANDROID_UI_VNEXT = READY_FOR_TRANSLATION`；下一阶段 =
   `ANDROID_UI_VNEXT_TRANSLATION`，不重新设计产品）
2. iOS（`HOLD_UNTIL_ANDROID_REFERENCE_TRANSLATION`）
3. Harmony（`HOLD_UNTIL_ANDROID_REFERENCE_TRANSLATION`）

## 5. Golden Baseline（仅桌面 dark reference）

- 允许从 `artifacts/runtime-evidence/2026-10-02-ui-vnext-phase1f-hf2-final` 建立
  `DESKTOP_VISUAL_GOLDEN_BASELINE`（仅 deterministic dark reference、冻结 viewport/profile）
- real-window 不稳定帧（stale-capture-surface / occlusion）**不作为** pixel golden
- 回归门：unexpected screenshot drift → FAIL；expected intentional change → 需显式
  reference revision（更新 freeze manifest + 记录理由）
