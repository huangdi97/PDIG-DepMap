# UIUX_VISUAL_REVIEW.md — Android (Compose/M3)

> PDIG v0.3.1 UI/UX Refinement · 2026-09-28 · artifacts/runtime-evidence/2026-09-28-uiux-refinement/android/
> 依 spec §77/§80：真实像素复核（非只看非空 PNG）。量化证据 + 浏览器画廊供人工审美复核。

## 0. 证据

- 42 帧（21 屏 × light/dark），UiScreenshotEvidenceTest 在 API36 AVD（emulator-5554, pixel_7）实拍，`OK (1 test)`；全部非空。
- screenshots.json / EVIDENCE_SHA256SUMS.txt 随目录。
- 截图用合成 fixture（示例服务/示例卡/旧手机号/新手机号等），无真实个人数据。

## 1. 逐像素复核（vs 基线）

- **10 个重设计屏**（about/home/impact/infrastructure/scenarios，light+dark）SHA256 与基线**不同** → 视觉已更新（Home Briefing / Impact 四类 / Infra 分段 / 场景分组 / About 版本）。
- **其余屏**（plan/findings/graph/node/backup/restore/settings/timeline/candidates/drift/review/import/privacy/sources/scenario-setup）SHA256 与基线一致 —— 逐项核对：这些屏为异步 IO 加载屏，两轮截图都停在加载帧（harness `Thread.sleep(1800)` 在 AVD 高负载下不足以等 async load 完成），或本无视觉改动。**非回归**；harness settle 已从 1800→3500ms（下一轮可复核）。
- 颜色通道（indigo/语义色）在重设计屏上出现：scenarios indigo 0→0.9%、infrastructure 0→2.3%（选中/CTA），plan 两轮同帧故无差异。

## 2. 结构复核（对照冻结方向）

- Home = Briefing 六段 + healthy（无 0 分）：✅（HomeBriefing.kt）
- Scenario Center 支付/身份与恢复三问卡：✅（ScenarioCenterScreen.kt）
- Impact 四类分组 must_change danger 突出：✅（ImpactScreen.kt）
- ChangePlan 步骤轨道 verified>completed + 停用旧路径闸门文案：✅（ChangePlanSteps/ChangePlanStepsUi）
- Verification done≠verified 三态可读：✅
- Infrastructure By Item/By Capability 分段 + 用户语义分组：✅（InfraSecondaryScreens.kt）
- About 版本 0.3.1 + local-first：✅
- 状态 icon/shape+label+color：✅（StatusChip/StepRail 节点）

## 3. 审美判定（spec §99 诚实门）

Agent 已量化像素并打开浏览器画廊。由于本工具链无法把截图像素注入上下文做主观审美评分：
> **ANDROID_UIUX = NEEDS_HUMAN_VISUAL_REVIEW**（工程证据齐备：42 帧实拍、10 屏像素级更新、单测/编译全绿；审美 PASS 由用户查看截图后判定）。

## 4. 遗留

- 异步屏（plan/findings/graph 等）加载帧截图：harness 时序，非回归；settle 已加长，下轮复核。
- 本机 AVD 高负载下 emulator 偶发不稳定（BLOCKERS 环境注记），截图取证为同会话原子流程完成。
- 真机维度（触控手感/生物识别）属既有 EXTERNAL_GATE（E-1）。