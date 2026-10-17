# SCREENSHOTS_PHASE1F_HF.md

> PHASE 1F-HF2（Desktop Reference Freeze Closure）—— Desktop 证据主集（有效版）。
> 全部截图 1920×1080 @1.0、Privacy Mask ON、offscreen 确定性渲染（VNextPhaseEvidence1FHF）。
> 真实窗口证据见 `REAL_WINDOW_TARGET_VALIDATION.json`（含 target-bound 验证）。
>
> **版本说明**：HF 轮生成的 `2026-10-02-ui-vnext-phase1f-hf` 12 张主集被 Human 复核发现
> glass/city 同帧无效（GitHub blob SHA 相同）；**有效 Final Review 证据 = 本页指向的
> `2026-10-02-ui-vnext-phase1f-hf2-final`**（glass/city 状态真实，SHA 不同，且带
> `FINAL_SCREENSHOT_MANIFEST.json` expected/actual 门禁）。

## 12 张 Human Final Review 主集

| #   | 屏幕                    | 文件（artifacts/runtime-evidence/2026-10-02-ui-vnext-phase1f-hf2-final/profiles/1920x1080@1.0/） |
| --- | ----------------------- | ------------------------------------------------------------------------------------------------ |
| 1   | Now                     | `vnext__now__1920x1080@1.0.png`                                                                  |
| 2   | Infrastructure Overview | `vnext__overview-global__1920x1080@1.0.png`                                                      |
| 3   | Cards                   | `vnext__cards__1920x1080@1.0.png`                                                                |
| 4   | Card Detail             | `vnext__card-detail__1920x1080@1.0.png`                                                          |
| 5   | Card Studio Glass       | `vnext__card-studio-glass__1920x1080@1.0.png`                                                    |
| 6   | Card Studio City        | `vnext__card-studio-city__1920x1080@1.0.png`                                                     |
| 7   | Numbers                 | `vnext__numbers__1920x1080@1.0.png`                                                              |
| 8   | Number Detail           | `vnext__number-detail__1920x1080@1.0.png`                                                        |
| 9   | Number Studio Travel    | `vnext__number-studio-travel__1920x1080@1.0.png`                                                 |
| 10  | Change Phone Transition | `vnext__change-transition__1920x1080@1.0.png`                                                    |
| 11  | Change Phone After      | `vnext__change-after__1920x1080@1.0.png`                                                         |
| 12  | Cards Empty             | `vnext__cards-empty__1920x1080@1.0.png`                                                          |

关键强制（§9）：shot 5（glass）SHA-256 `ae378d81…` ≠ shot 6（city）SHA-256 `17cde3fb…`
（字节 553550 ≠ 532519；采样像素差异 ≈12.9%，Glass 与 City 身份人眼可直接区分）。

## Mechanical profiles（1280 / 2560 / 1.25 / 1.5）

`mechanical/1280x720@1.0/`、`mechanical/2560x1440@1.0/`、`mechanical/1920x1080@1.25/`、
`mechanical/1920x1080@1.5/`：每档 5 屏（infrastructure-overview / card-customization /
change-phone / number-detail / cards）。

## Empty-state 机械帧

`mechanical/empty-states/`：`vnext__numbers-empty__1920x1080@1.0.png`、
`vnext__now-empty__1920x1080@1.0.png`、`vnext__region-empty__1920x1080@1.0.png`、
`vnext__card-detail-empty__1920x1080@1.0.png`。

## Real-window（真实窗口，target-bound 验证；历史证据目录）

`artifacts/runtime-evidence/2026-10-02-ui-vnext-phase1f-hf/real-window/01-now.png`：
真实 PDIG 窗口首帧（PDIG Preview 窗口、暗色主题、Now 内容；processId/windowTitle/HWND/
windowBounds/targetScreen/captureMethod/sha256 见 `REAL_WINDOW_TARGET_VALIDATION.json`）。
02–14 因本机合成器不向 GDI 暴露实时帧被如实拒绝（capture=false，未写证据）；15/16 为
`REAL_WINDOW_KEYBOARD_HUMAN_GATE`。closure 轮无新工具重跑一次：本会话前台遮挡（捕获区
亮度 ≈0.99），harness 全部如实拒绝（0/16），`REAL_WINDOW_MULTI_FRAME_ENVIRONMENT_GATE`
保持。

## 校验

- `FINAL_SCREENSHOT_MANIFEST.json`：12 条，expectedState == actualState（`stateValidation=true`
  × 12）；studio 帧 actual 来自 composition 回写（`themeSource=composition-readback`）。
- `IMAGE_METRICS.json`：12 帧 meanLum 0.061–0.217，0 error / 0 empty / 0 near-black。
- `UI_LAYOUT_PROBE.json`：§4/§8/§11 检查全部 `passed=true`（theme tile bounds/clipped、
  themeGrid noOverlap、stage、inspector defaultOpen、number tile/communication、
  path ≤2/1.5/1、continuity labels、empty.cards 480–600 + centered）。
- `EVIDENCE_SHA256SUMS.txt`：全部 PNG 哈希（37 帧）。
