# UIUX_VISUAL_REVIEW.md — Baseline (v0.3.1 重构前)

> PDIG v0.3.1 UI/UX Refinement · 2026-09-28 · 基线证据（重构前，spec §15/§16）
> 完整审计见 docs/uiux/PDIG_UIUX_BASELINE_AUDIT.md；本文件仅说明像素量化事实。

## Desktop（80 帧）

- 主色板：#F8F8F8 背景 + #E0E0E8 大色板（lavender slab 占 0.14–0.61 页面）；品牌 indigo ≈0；文本像素 0.1–0.5%。
- 结论：low density / weak hierarchy / generic grouped-card feel 的像素级证实（与 audit P0–P3 一致）。

## Android（42 帧）

- lightBg 0.89–0.99；indigo 0–4.7%（仅 plan 等少数屏）；文本像素 0.1–2.2%。
- 结论：同 Desktop 病征（audit B0-5）。

## 说明

- 基线截图与精修截图均用同一合成 fixture（示例服务/示例卡/旧手机号/新手机号），保证 before/after 可比。
- 逐页 BEFORE/AFTER 对照见浏览器画廊 `.agent-work/uiux/after-gallery.html`（含 24 图）与 `docs/uiux/PDIG_UIUX_BASELINE_AUDIT.md`。