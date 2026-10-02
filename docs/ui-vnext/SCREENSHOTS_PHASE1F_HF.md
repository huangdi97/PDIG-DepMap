# SCREENSHOTS_PHASE1F_HF.md

> PHASE 1F-HF（Human Final Acceptance Fix）—— Desktop 证据主集。
> 全部截图 1920×1080 @1.0、Privacy Mask ON、offscreen 确定性渲染（VNextPhaseEvidence1FHF）。
> 真实窗口证据见 `REAL_WINDOW_TARGET_VALIDATION.json`（含 target-bound 验证）。

## 12 张 Human Final Review 主集

| # | 屏幕 | 文件（artifacts/runtime-evidence/2026-10-02-ui-vnext-phase1f-hf/profiles/1920x1080@1.0/） |
| --- | --- | --- |
| 1 | Now | `vnext__now__1920x1080@1.0.png` |
| 2 | Infrastructure Overview | `vnext__overview-global__1920x1080@1.0.png` |
| 3 | Cards | `vnext__cards__1920x1080@1.0.png` |
| 4 | Card Detail | `vnext__card-detail__1920x1080@1.0.png` |
| 5 | Card Studio Glass | `vnext__card-studio-glass__1920x1080@1.0.png` |
| 6 | Card Studio City | `vnext__card-studio-city__1920x1080@1.0.png` |
| 7 | Numbers | `vnext__numbers__1920x1080@1.0.png` |
| 8 | Number Detail | `vnext__number-detail__1920x1080@1.0.png` |
| 9 | Number Studio Travel | `vnext__number-studio-travel__1920x1080@1.0.png` |
| 10 | Change Phone Transition | `vnext__change-transition__1920x1080@1.0.png` |
| 11 | Change Phone After | `vnext__change-after__1920x1080@1.0.png` |
| 12 | Cards Empty | `vnext__cards-empty__1920x1080@1.0.png` |

## Mechanical profiles（1280 / 2560 / 1.25 / 1.5）

`mechanical/1280x720@1.0/`、`mechanical/2560x1440@1.0/`、`mechanical/1920x1080@1.25/`、
`mechanical/1920x1080@1.5/`：每档 5 屏（infrastructure-overview / card-customization /
change-phone / number-detail / cards）。

## Empty-state 机械帧

`mechanical/empty-states/`：`vnext__numbers-empty__1920x1080@1.0.png`、
`vnext__now-empty__1920x1080@1.0.png`、`vnext__region-empty__1920x1080@1.0.png`、
`vnext__card-detail-empty__1920x1080@1.0.png`。

## Real-window（真实窗口，target-bound 验证）

`real-window/01-now.png`：真实 PDIG 窗口首帧（PDIG Preview 窗口、暗色主题、Now 内容；
processId/windowTitle/HWND/windowBounds/targetScreen/captureMethod/sha256 见
`REAL_WINDOW_TARGET_VALIDATION.json`）。02–14 因本机合成器不向 GDI 暴露实时帧被如实
拒绝（capture=false，未写证据）；15/16 为 `REAL_WINDOW_KEYBOARD_HUMAN_GATE`。

## 校验

- `IMAGE_METRICS.json`：12 帧 meanLum 0.082–0.108，0 error / 0 empty / 0 near-black。
- `UI_LAYOUT_PROBE.json`：§4/§8/§11 新增检查 12 项全部 `passed=true`。
- `EVIDENCE_SHA256SUMS.txt`：37 个 PNG 哈希。
