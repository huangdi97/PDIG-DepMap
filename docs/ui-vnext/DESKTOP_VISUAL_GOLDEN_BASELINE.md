# DESKTOP_VISUAL_GOLDEN_BASELINE.md

> Desktop Dark Golden Baseline（2026-10-02 Human/Vision ACCEPTED）。
> 状态：`DESKTOP_DARK_GOLDEN_BASELINE = ESTABLISHED`。

## 1. Baseline 范围（仅这些）

- source：`artifacts/runtime-evidence/2026-10-02-ui-vnext-phase1f-hf2-final`
- 12 张 1920×1080@1.0 deterministic offscreen dark 截图（SHA256 见
  `DESKTOP_REFERENCE_FREEZE_MANIFEST.json` 与 `FINAL_SCREENSHOT_MANIFEST.json`）
- 冻结 viewport / profile：1920×1080@1.0，Privacy Mask ON

## 2. 排除（不进入 golden）

- real-window 不稳定帧（stale-capture-surface / occlusion / 前台遮挡）
- mechanical 档（1280×720 / 2560×1440 / 1.25 / 1.5）为 responsive 回归证据，不是 golden 主档
- Light theme（FUNCTIONAL_SUPPORTED / VISUAL_REFINEMENT_LATER）

## 3. Regression gate

- 生成截图与 baseline SHA256 不一致且非预期 → **FAIL**
- 预期内有意变更 → 必须先显式 reference revision（更新 freeze manifest + 记录理由）

## 4. 验证方式

- `EVIDENCE_SHA256SUMS.txt`（hf2-final）与 `DESKTOP_REFERENCE_FREEZE_MANIFEST.json`
  的 12 个 SHA256 逐项一致（Freeze 轮已核验：0 mismatch）
