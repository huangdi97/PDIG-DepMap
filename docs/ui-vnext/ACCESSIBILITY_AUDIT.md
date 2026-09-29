# ACCESSIBILITY_AUDIT.md — 无障碍审计（No-Vision 轮可实测项）

> 2026-09-29 · feat/pdig-ui-vnext · 契约：INTERACTION_CONTRACT §4/§5/§9 + MOTION_CONTRACT + DESIGN_TOKENS contrast
> **诚实边界**：本轮只能实测"代码面"；真实窗口 focus、OS 焦点环、屏幕阅读器实读 = NEEDS_RUNTIME_VERIFICATION。

## 1. 状态三通道（icon + label + color）

- 状态只用语义色 positive #3BD49B / warning #F4B64B / critical #FA626B / unknown #8C9AB5（DESIGN_TOKENS semantic.status）；
- 每处状态同时渲染 icon/shape + 文本 label + 颜色（`StatusBadge`）；
- 颜色不是唯一信息通道；disabled 必须附原因文案（Change Phone stage 6："新手机号验证通过后才能停用旧手机号"）。

## 2. 键盘可达（Desktop）

| 键 | 行为 | 实现 |
| --- | --- | --- |
| Tab / Shift+Tab | 焦点循环：rail → top bar → 内容 → region list | focusable 组件链（INTERACTION_CONTRACT §5） |
| Enter / Space | 激活 | 原生语义 |
| Escape | globe 回退 / 关抽屉 / 关聚焦 | globe state machine 回退 GLOBAL |
| Arrow | region list / 表格行导航；globe 微调视角 | 已接入 |
| Ctrl/Cmd+K | 命令/搜索条 | 契约项（top bar 语义） |

- 焦点可见：primaryBright ring（契约要求任何键盘可达元素必有）；
- **实测限制**：离屏渲染（ImageComposeScene）无真实窗口焦点系统 → 焦点环的 OS 级验证标记
  `NEEDS_RUNTIME_VERIFICATION`（本机曾受会话窗口获焦限制，见上轮 BLOCKERS 注记）。

## 3. 对比度（token 深色体系，计算值）

| 前景 | 背景 | 对比（WCAG 2.x 计算） | 阈值 | 用途 |
| --- | --- | --- | --- | --- |
| textPrimary #F4F7FF | canvas #061225 | ≈17.5:1 | 正文 ≥4.5:1 ✓ | 页面标题/正文 |
| textSecondary #A9B8D5 | canvas #061225 | ≈9.4:1 | 正文 ≥4.5:1 ✓ | 次级文本 |
| textMuted #7383A3 | canvas #061225 | ≈4.9:1 | 正文 ≥4.5:1 ✓（元信息 ≈4.5+） | 元信息/辅助 |
| textPrimary #F4F7FF | surface #0B1A33 | ≈16.2:1 | ✓ | 面板内正文 |
| textMuted #7383A3 | surface #0B1A33 | ≈4.6:1 | ✓ | 面板内元信息 |

契约门槛：contrast.bodyTextMin 4.5:1、largeTextMin 3:1 —— 全部满足。
（对比为 token 相对亮度计算值；真机渲染（字体渲染/亚像素）需 runtime 复核。）

## 4. Globe 非视觉替代（Region List）

- Globe 旁/下有 **Region List**（键盘可达、screen reader 可读）：「中国大陆，5 张卡，2 个号码」；
- Region List 同样驱动 region filter 与 drawer；
- `globe != sole navigation method`（IA.md §7）；
- 一级导航永远可见（Desktop rail / Mobile bottom nav）。

## 5. 触控目标

- Android ≥48dp（touchTarget.android = 48）、iOS ≥44pt（touchTarget.ios = 44）；
- 移动端由并行平台 fixer 按同一 token 落实。

## 6. Reduce Motion（MOTION_CONTRACT.reduceMotion）

| 项 | 常规 | Reduce Motion |
| --- | --- | --- |
| idle rotation | 0.8°/s 极慢，交互后暂停 | **off** |
| camera/focus | ease-in-out，globe focus 620ms | **simplified（translate-only，无 ease spin）** |
| arc | segment-draw 620ms | **static** |
| drawer | slide 200ms | 无 slide，instant appear |

Desktop 已实现：`reduceMotion` 参数关闭 idle rotation（VNextGlobe.kt LaunchedEffect 分支）；
截图取证固定 reduceMotion 态（确定性帧）。

## 7. 实测范围与 NEEDS_RUNTIME_VERIFICATION

| 项 | 实测 | 待验证 |
| --- | --- | --- |
| 状态三通道代码面 | ✓（StatusBadge icon+label+color） | screen reader 实读 |
| 键盘焦点链代码面 | ✓（focusable 链） | 真实窗口 OS 焦点环 / focus-visible 渲染 |
| 对比度（计算） | ✓ 全部 ≥4.5:1 | 真机渲染复核 |
| Region List 非视觉替代 | ✓ 已实现 | screen reader 实读 |
| 触控 ≥48/44 | Desktop 桌面端不适用；移动端 | Android/iOS runtime |
| reduce motion | ✓ 代码 + 确定性帧 | 系统开关联动（Compose LocalReduceMotion / SwiftUI accessibilityReduceMotion / Harmony 系统开关） |
| 字体缩放（Dynamic Type / fontScale） | 契约项 | iOS/Android runtime |
