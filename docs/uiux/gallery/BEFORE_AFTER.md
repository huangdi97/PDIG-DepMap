# PDIG 0.3.1 UI/UX Refinement — BEFORE / AFTER 截图对照

> 生成：2026-09-28 · Branch: `feat/pdig-uiux-refinement`
> 这是**仓库内可见**的画廊，任何人 clone 或从 GitHub 网页均可浏览。
> 图片路径为相对路径，指向 `artifacts/runtime-evidence/2026-09-28-uiux-{baseline,refinement}/`。
> 说明：Desktop = 真实离屏渲染（ProfileDriver，1280×720）；Android = API36 AVD 实拍（light）。
> **注意**：Android 异步加载页（plan/findings 等）截图受 AVD 高负载影响可能停在加载帧，两轮基线/精修一致，非回归。

## 1. Home

| BEFORE | AFTER |
| --- | --- |
| ![home-before](../../../artifacts/runtime-evidence/2026-09-28-uiux-baseline/desktop-profiles/desktop__profile__1280x720@1.0__home.png) | ![home-after](../../../artifacts/runtime-evidence/2026-09-28-uiux-refinement/desktop-profiles/desktop__profile__1280x720@1.0__home.png) |

## 2. Findings

| BEFORE | AFTER |
| --- | --- |
| ![findings-before](../../../artifacts/runtime-evidence/2026-09-28-uiux-baseline/desktop-profiles/desktop__profile__1280x720@1.0__findings.png) | ![findings-after](../../../artifacts/runtime-evidence/2026-09-28-uiux-refinement/desktop-profiles/desktop__profile__1280x720@1.0__findings.png) |

## 3. Infrastructure

| BEFORE | AFTER |
| --- | --- |
| ![infra-before](../../../artifacts/runtime-evidence/2026-09-28-uiux-baseline/desktop-profiles/desktop__profile__1280x720@1.0__infra.png) | ![infra-after](../../../artifacts/runtime-evidence/2026-09-28-uiux-refinement/desktop-profiles/desktop__profile__1280x720@1.0__infra.png) |

## 4. Scenario Center

| BEFORE | AFTER |
| --- | --- |
| ![scenarios-before](../../../artifacts/runtime-evidence/2026-09-28-uiux-baseline/desktop-profiles/desktop__profile__1280x720@1.0__scenarios.png) | ![scenarios-after](../../../artifacts/runtime-evidence/2026-09-28-uiux-refinement/desktop-profiles/desktop__profile__1280x720@1.0__scenarios.png) |

## 5. Scenario Setup（Replace Phone）

| BEFORE | AFTER |
| --- | --- |
| ![setup-before](../../../artifacts/runtime-evidence/2026-09-28-uiux-baseline/desktop-profiles/desktop__profile__1280x720@1.0__scenario_setup.png) | ![setup-after](../../../artifacts/runtime-evidence/2026-09-28-uiux-refinement/desktop-profiles/desktop__profile__1280x720@1.0__scenario_setup.png) |

## 6. Impact

| BEFORE | AFTER |
| --- | --- |
| ![impact-before](../../../artifacts/runtime-evidence/2026-09-28-uiux-baseline/desktop-profiles/desktop__profile__1280x720@1.0__impact.png) | ![impact-after](../../../artifacts/runtime-evidence/2026-09-28-uiux-refinement/desktop-profiles/desktop__profile__1280x720@1.0__impact.png) |

## 7. Plan

| BEFORE | AFTER |
| --- | --- |
| ![plan-before](../../../artifacts/runtime-evidence/2026-09-28-uiux-baseline/desktop-profiles/desktop__profile__1280x720@1.0__plan.png) | ![plan-after](../../../artifacts/runtime-evidence/2026-09-28-uiux-refinement/desktop-profiles/desktop__profile__1280x720@1.0__plan.png) |

## 8. Verification

| BEFORE | AFTER |
| --- | --- |
| ![verification-before](../../../artifacts/runtime-evidence/2026-09-28-uiux-baseline/desktop-profiles/desktop__profile__1280x720@1.0__verification.png) | ![verification-after](../../../artifacts/runtime-evidence/2026-09-28-uiux-refinement/desktop-profiles/desktop__profile__1280x720@1.0__verification.png) |

## 9. Android（API36 AVD 实拍，light）

| Screen | BEFORE | AFTER |
| --- | --- | --- |
| Home | ![ah-before](../../../artifacts/runtime-evidence/2026-09-28-uiux-baseline/android/android__phone-api36__light__home__populated__01.png) | ![ah-after](../../../artifacts/runtime-evidence/2026-09-28-uiux-refinement/android/android__phone-api36__light__home__populated__01.png) |
| Scenarios | ![as-before](../../../artifacts/runtime-evidence/2026-09-28-uiux-baseline/android/android__phone-api36__light__scenarios__populated__02.png) | ![as-after](../../../artifacts/runtime-evidence/2026-09-28-uiux-refinement/android/android__phone-api36__light__scenarios__populated__02.png) |
| Impact | ![ai-before](../../../artifacts/runtime-evidence/2026-09-28-uiux-baseline/android/android__phone-api36__light__impact__populated__17.png) | ![ai-after](../../../artifacts/runtime-evidence/2026-09-28-uiux-refinement/android/android__phone-api36__light__impact__populated__17.png) |
| Infrastructure | ![ainf-before](../../../artifacts/runtime-evidence/2026-09-28-uiux-baseline/android/android__phone-api36__light__infrastructure__populated__07.png) | ![ainf-after](../../../artifacts/runtime-evidence/2026-09-28-uiux-refinement/android/android__phone-api36__light__infrastructure__populated__07.png) |
| About | ![aab-before](../../../artifacts/runtime-evidence/2026-09-28-uiux-baseline/android/android__phone-api36__light__about__populated__16.png) | ![aab-after](../../../artifacts/runtime-evidence/2026-09-28-uiux-refinement/android/android__phone-api36__light__about__populated__16.png) |

## 完整证据

- `artifacts/runtime-evidence/2026-09-28-uiux-baseline/`（Desktop 80 帧 / Android 42 帧）
- `artifacts/runtime-evidence/2026-09-28-uiux-refinement/`（Desktop 80 帧 / Android 42 帧）+ 每平台 `screenshots.json` / `EVIDENCE_SHA256SUMS.txt` / `UIUX_VISUAL_REVIEW.md`
- 审计与设计文档：`docs/uiux/`（10 份）