# UIUX_VISUAL_REVIEW.md — Desktop (Compose Desktop)

> PDIG v0.3.1 UI/UX Refinement · 2026-09-28 · artifacts/runtime-evidence/2026-09-28-uiux-refinement/desktop-profiles/
> 依 spec §77/§80：真实像素复核（非只看非空 PNG）。量化证据 + 浏览器画廊供人工审美复核。

## 0. 证据

- 80 帧（16 页 × 5 档：1280×720 / 1920×1080 / 2560×1440 / 125% / 150%），ProfileDriver（ImageComposeScene 离屏真实渲染 PDIGAppShell），全部 VERDICT PASS。
- screenshots.json / EVIDENCE_SHA256SUMS.txt 随目录。

## 1. 逐像素量化（1280×720@1.0 代表页，vs 基线）

| 页面 | 基线主色板（lavender slab 占比） | 精修后主色板（slab 占比） | PNG 复杂度（字节）变化 | 说明 |
| --- | --- | --- | --- | --- |
| home | #E0E0E0 ≈0.61 | #F0F0F0（无 slab）≈0.955 | +63.5% | Briefing 六段 + healthy |
| findings | #E0E0E0 ≈0.43 | 白+边框 ≈0.94 | +38.1% | what/why/next 卡 |
| infra | #E0E0E0 ≈0.27 | 白+边框 ≈0.96 | +22.8% | master-detail + 分段 |
| scenarios | #E0E0E0 ≈0.43 | 白+边框 ≈0.94 | +51.8% | 支付/身份分组 |
| scenario_setup | #E0E0E0 ≈0.56 | 白+边框 ≈0.96 | +14.8% | Replace Phone Rail |
| impact | #E0E0E0 ≈0.28 | 白+边框 ≈0.96 | +59.5% | 四类分组 |
| plan | #E0E0E0 ≈0.14 | 白+边框 ≈0.99 | +55.9% | 步骤轨道 |
| verification | #E0E0E0 ≈0.14 | 白+边框 ≈0.98 | +70.4% | verified>completed |
| sources | #E0E0E0 ≈0.43 | 白+边框 ≈0.97 | +35.2% | 来源类型人话 |
| security | #E0E0E0 ≈0.35 | 白+边框 ≈0.97 | +36.9% | 无 blob 文件名 |

→ **lavender 大色板消失**（0.14–0.61 → ≈0.02 以下）；indigo 出现在选中/CTA/状态；PNG 复杂度全面 +15~70%（信息/边缘增加）。

## 2. 结构复核（对照冻结方向）

- Sidebar 分组导航 + 图标 + 选中态：✅（App.kt NAV_GROUPS + SidebarItem）
- top bar「本地数据文件已就绪」不再显示文件名：✅
- Home = Briefing 六段 + healthy（无 0 issues / 安全分）：✅
- Findings what/why/next + 展开 evidence/unknown/affected：✅
- Infrastructure By Item/By Capability segmented + 能力面板：✅
- Scenario Center 支付/身份与恢复；Scenario Setup Replace Phone Rail 9 步 + 停用旧号 blocked 明文原因：✅
- Impact 四类分组 must_change danger 突出：✅
- Plan/Actions/Verification 步骤轨道 verified>completed + 闸门文案：✅
- 状态 icon+label+color（StatusBadge）：✅

## 3. 审美判定（spec §99 诚实门）

Agent 已量化像素并打开浏览器画廊（.agent-work/uiux/after-gallery.html）。由于本工具链无法把截图像素注入上下文做主观审美评分：
> **DESKTOP_UIUX = NEEDS_HUMAN_VISUAL_REVIEW**（工程证据齐备：80 帧非空、slab 消除、复杂度上升、结构符合冻结方向；审美 PASS 由用户查看截图后判定）。

## 4. 遗留

- `--keys` Robot 注入受本会话窗口焦点限制（BLOCKERS 环境注记）；代码面 focusable + 导航顺序已兜底。
- 大字体（125%/150% 档）已渲染 PASS，无裁剪；最终以大屏复核为准。
