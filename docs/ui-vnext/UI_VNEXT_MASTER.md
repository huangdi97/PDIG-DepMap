# UI_VNEXT_MASTER.md — PDIG UI vNext 总览（No-Vision Blind Coding 轮）

> 2026-09-29 · feature branch `feat/pdig-ui-vnext` · 执行模式：**No-Vision Blind Coding Agent**
> 本轮只写 `VISUAL_CONTRACT_IMPLEMENTATION = PASS`，**禁止**"好看 / 还原 / 与参考图一致"等视觉自判（spec §99、Goal 验收项 18）。

## 1. 目标

把已冻结的人类视觉方向 **Global Digital Infrastructure**（dark spatial · deep navy · subtle glass · premium · calm）
转译为可执行、可测量、可跨平台的 **Visual Contract**，并在四端实现：

- 契约源：`spec/ui-vnext/`（DESIGN_TOKENS.json / VISUAL_DNA.md / IA.md / LAYOUT_CONTRACT.json /
  MOTION_CONTRACT.json / INTERACTION_CONTRACT.md / RESPONSIVE_CONTRACT.json /
  PRESENTATION_PROFILE_SCHEMA.json / UIVNextDemoFixture.json / components 9 / screens 10 / references）
- 本机可验证闭环：**Desktop** 全量实现 + 离屏 90 帧 + 几何取证 + 回归；Android/iOS/Harmony 由并行平台 fixer 产出（PENDING_CONVERGENCE）
- 全程无视觉自判；只能证明：契约实现、几何符合、token 一致、功能正确、证据存在。

## 2. Canonical Authority 阅读清单（全部已读）

| 文档                               | 结论要点（本轮引用）                                                 |
| ---------------------------------- | -------------------------------------------------------------------- |
| AGENTS.md                          | §24.5 Native Migration 优先；§17 日志禁令；§19 状态声明不冒充        |
| CANONICAL_DESIGN.md                | Observation ≠ Dependency；Graph 是模型不是主界面；Precision > Recall |
| GOAL_MVP01.md                      | MVP 唯一核心 Job = 模拟更换/注销银行卡；双 Gate 前不增业务能力       |
| WORK_STATUS.md                     | 历史轮证据基线；v0.3.1 487 tests / conformance 128                   |
| BLOCKERS.md                        | 外部 Gate 全部为真实 EXTERNAL_BLOCKER；本轮新增见 BLOCKERS.md 顶部   |
| FINAL_V0_3_1_RELEASE_CLOSURE.md    | v0.3.1 冻结 SHA/tag；本轮不触碰                                      |
| PRODUCT_V0_3_1_RELEASE_MANIFEST.md | 发布产物清单；本轮零改动                                             |
| spec/ui/design-tokens.json         | v0.3.1 token 冻结；vNext 不复用、不覆盖                              |
| spec/ui/copy-zh.json               | 既有文案层；vNext 文案以 UIVNextDemoFixture + screens 契约为准       |
| docs/uiux/（10 份）                | 上一轮（feat/pdig-uiux-refinement）方向与证据基线                    |
| NATIVE_MIGRATION_STATUS.md         | 三端原生路线；spec/ 为最高真相源                                     |

## 3. 本轮范围（四端）

| 端                 | 范围                                                                                                                      | 本机状态                 | 证据                                            |
| ------------------ | ------------------------------------------------------------------------------------------------------------------------- | ------------------------ | ----------------------------------------------- |
| Desktop（Compose） | 10 屏 + VNextShell（rail 80/188 + top 48）+ 2.5D Globe + PresentationProfile + Privacy Mask + `--vnext` / `--vnext-shots` | 实现 + 编译 PASS + 90 帧 | artifacts/runtime-evidence/2026-09-29-ui-vnext/ |
| Android（Compose） | 同契约；bottom nav ≤5、compact globe hero                                                                                 | 并行 fixer 产出          | PENDING_CONVERGENCE                             |
| iOS（SwiftUI）     | NavigationStack/SplitView、sheet、Dynamic Type                                                                            | 并行 fixer 产出          | PENDING_CONVERGENCE                             |
| Harmony（ArkUI）   | 原生 ArkUI；hvigor 构建可达                                                                                               | 并行 fixer 产出          | PENDING_CONVERGENCE                             |
| 四端 Token         | 单一真源 DESIGN_TOKENS.json → codegen 4 产物                                                                              | CODEGEN GATE PASS        | tools/codegen/generate.mjs --check              |

## 4. 阶段执行记录（Stage 0–17 ↔ 实际完成情况）

按分支 Goal（`.pi/goal/…ui-vnext…20260929-1800.md`）执行顺序记录；spec §84 Stage 0→17 严格顺序，以 Goal 验收项为清单：

| Stage    | 内容（映射 Goal 条目）             | 实际完成情况                                                                                 |
| -------- | ---------------------------------- | -------------------------------------------------------------------------------------------- |
| Stage 0  | 盘点（只查不改）                   | DONE → GLOBE_TECH_INVENTORY.md / LOCAL_ENV_INVENTORY.md / LOCAL_DOWNLOAD_AUDIT.md            |
| Stage 1  | Canonical Authority 已读           | DONE → 本文件 §2                                                                             |
| Stage 2  | Skill 真实使用 + 审计              | DONE → SKILL_USAGE_AUDIT.md（ui-ux-pro-max / impeccable / frontend-design，不用于视觉自判）  |
| Stage 3  | Visual Contract 目录完整           | DONE（既有 spec/ui-vnext 29 文件，本轮零改动）                                               |
| Stage 4  | Token Codegen 单一真源             | DONE → generate.mjs 扩展；`--check` = CODEGEN GATE PASS                                      |
| Stage 5  | Geometry / testId 契约             | DONE → LAYOUT_CONTRACT.json 既有；Desktop 实现按其落地                                       |
| Stage 6  | Geometry Probe                     | DONE → UI_LAYOUT_PROBE.json（每 profile 输出，testId/x/y/w/h/visible/enabled）               |
| Stage 7  | UIVNextDemoFixture（全 synthetic） | DONE → desktop/app/.../demo/UiVNextDemoFixture.kt（CN/HK/GB/US 卡与号）                      |
| Stage 8  | Globe Spike Gate                   | DONE（桌面证据）→ 见 GLOBE_TECH_INVENTORY.md §5                                              |
| Stage 9  | Globe Interaction Contract         | DONE → VNextGlobe.kt（GLOBAL/HOVER/SELECTED/DETAIL 状态机、drag/zoom/hover/click/focus/arc） |
| Stage 10 | 十屏实现                           | Desktop DONE（10 屏）；Android/iOS/Harmony PENDING_CONVERGENCE                               |
| Stage 11 | PresentationProfile                | DONE → model/PresentationProfile + 本地偏好语义，绝不进 .depmap                              |
| Stage 12 | Motion / Accessibility / Privacy   | DONE（reduceMotion、Privacy Mask、三通道状态）→ ACCESSIBILITY_AUDIT.md                       |
| Stage 13 | 四端实现与证据                     | Desktop DONE；三端 PENDING_CONVERGENCE（并行 fixer）                                         |
| Stage 14 | Image Metrics（无眼睛量化取证）    | DONE → 90 帧 IMAGE_METRICS.json（JDK ImageIO，零新依赖）                                     |
| Stage 15 | 回归硬性 0                         | DONE → `npm run check` 全绿（487 tests）→ IMPLEMENTATION_REPORT.md                           |
| Stage 16 | 最终交付物与 Gate 矩阵             | DONE → docs/ui-vnext/ 10 份 + artifacts/ui-vnext-review/ 5 JSON + desktop 证据副本           |
| Stage 17 | Push + STOP                        | 本任务不执行 push；STOP 等待 Human/Vision Review（交付物齐备后由主 agent 执行）              |

## 5. 诚实声明（No-Vision）

- 本 agent **无视觉通道**：不能判断"好看 / 还原 / 一致 / 像素级对齐"。
- 所有 PASS 均为**可机器核验**的：编译通过、几何 probe 命中契约、token 单一真源零漂移、量化 image metrics 无空白/全黑、核心回归 0。
- 审美结论（Global Digital Infrastructure 是否成立）**必须**由 Human 或 Vision Model 判定：`VISION_REVIEW = NEEDS_HUMAN_OR_VISION_MODEL`。
- 未实测项如实标注：真实窗口 focus（离屏渲染无法验证）、Android/iOS/Harmony runtime、对比度实测为计算值。

## 6. 交付物索引

- docs/ui-vnext/：UI_VNEXT_MASTER / GLOBE_TECH_INVENTORY / LOCAL_ENV_INVENTORY / LOCAL_DOWNLOAD_AUDIT /
  SKILL_USAGE_AUDIT / PLATFORM_ADAPTATION / CUSTOMIZATION_MODEL / ACCESSIBILITY_AUDIT / IMPLEMENTATION_REPORT / BLIND_AGENT_ACCEPTANCE
- artifacts/ui-vnext-review/：desktop/（90 PNG + UI_LAYOUT_PROBE.json + EVIDENCE_SHA256SUMS.txt + IMAGE_METRICS.json）、
  TOKEN_MANIFEST.json、REFERENCE_MANIFEST.json、TEST_RESULTS.json、VISION_REVIEW_TEMPLATE.json、IMAGE_METRICS.json
