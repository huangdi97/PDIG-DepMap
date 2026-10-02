# PLATFORM_TRANSLATION_CONTRACT.md

> 跨端翻译合同（Desktop Reference → Android / iOS / Harmony）。
> 依据 `spec/ui-vnext/DESKTOP_REFERENCE_FREEZE.md`；本轮只建立合同，**不实现移动端 UI**。

## 1. 定义

- **Desktop Reference**：`DESKTOP_REFERENCE_FREEZE_MANIFEST.json` 冻结的
  hf2-final 12 张 1920×1080@1.0 dark 参考 + Freeze 设计契约（spec/ui-vnext）
- **Platform Translation**：把 Reference 的 hierarchy / identity / prominence /
  material family / state semantics / continuity semantics / navigation intent
  翻译到平台原生表达 —— **不是**把 Desktop 页面缩小 / 像素拷贝

## 2. 必须保留（每个平台）

- information hierarchy
- object identity
- semantic prominence
- color / material family
- state semantics（含 Plan Projection ≠ reality 标注）
- continuity semantics（Current / Transition / After）
- navigation intent（Primary Nav 现在 / 基础设施 / 变更 / 记录 + Infrastructure Secondary）

## 3. 允许平台原生

- navigation（bottom bar / tabs / drawer 等平台惯例）
- sheet / modal
- bottom bar
- gesture
- safe area
- typography metric（平台字体尺度，不照搬 dp）
- window / screen density
- interaction convention（触控 / 键盘 / 无障碍）

## 4. Presentation Boundary（与 spec 一致）

- 可 translate presentation；不可 fork domain truth / Canonical / change semantics / recovery semantics
- PresentationProfile 只落呈现层，绝不动 .depmap / PersonalReality

## 5. 优先级与状态

1. **Android** — `ANDROID_UI_VNEXT = READY_FOR_TRANSLATION`
   （下一阶段 = `ANDROID_UI_VNEXT_TRANSLATION`，不重新设计产品）
2. **iOS** — `HOLD_UNTIL_ANDROID_REFERENCE_TRANSLATION`
3. **Harmony** — `HOLD_UNTIL_ANDROID_REFERENCE_TRANSLATION`

## 6. 本轮不实现

本文件只是合同；不启动 Android / iOS / Harmony UI 实现。

## 7. 验收口径（翻译轮）

- 每平台保留 §2 七要素（对照 Freeze manifest 逐项核验）
- 使用平台原生交互惯例（§3）
- 不声称 pixel 对齐；声称 hierarchy / identity / state 对齐
- `*_RUNTIME` 状态遵守 AGENTS §19：IMPLEMENTED / COMPILED / TESTED / DEVICE_VERIFIED / STORE_READY
  （不得把理论支持写成已验证）
