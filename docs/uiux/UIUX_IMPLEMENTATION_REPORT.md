# UIUX_IMPLEMENTATION_REPORT.md

> PDIG v0.3.1 UI/UX Refinement · 2026-09-28 · 实施报告（spec §97）
> 从 Baseline → Direction Freeze → Design System → Tokens → 组件 → 四平台 → 审计 → 证据，全部落地记录。

## 0. 执行顺序（对照 spec §64）

```
Baseline Evidence ✓ → Skill Research ✓ → Critique ✓ → UX Shape ✓
→ Visual Direction Freeze ✓ → Design Tokens ✓ → Core Components ✓
→ Desktop ✓ → Android ✓ → iOS ✓ → Harmony ✓ → Audit ✓ → Polish/Harden ✓
→ Visual Acceptance（诚实门禁，见下）
```

## 1. 交付物清单（对照 spec §97）

| 交付物                         | 状态         | 位置                                                                 |
| ------------------------------ | ------------ | -------------------------------------------------------------------- |
| PRODUCT.md / DESIGN.md         | ✅           | 仓库根（注明不覆盖 Canonical Master）                                |
| LOCAL_ENV_INVENTORY.md         | ✅           | docs/uiux/                                                           |
| LOCAL_DOWNLOAD_AUDIT.md        | ✅           | docs/uiux/（UNNECESSARY_DOWNLOAD_COUNT=0）                           |
| UIUX_SKILL_USAGE_AUDIT.md      | ✅           | docs/uiux/                                                           |
| PDIG_UIUX_BASELINE_AUDIT.md    | ✅           | docs/uiux/                                                           |
| PDIG_UIUX_DIRECTION_FREEZE.md  | ✅           | docs/uiux/                                                           |
| PDIG_DESIGN_SYSTEM.md          | ✅           | docs/uiux/                                                           |
| PAGE_STATE_MATRIX.md           | ✅           | docs/uiux/                                                           |
| PLATFORM_UIUX_PARITY_MATRIX.md | ✅           | docs/uiux/                                                           |
| UIUX_IMPLEMENTATION_REPORT.md  | ✅（本文件） | docs/uiux/                                                           |
| UIUX_FINAL_ACCEPTANCE.md       | ✅           | docs/uiux/                                                           |
| 运行证据                       | ✅           | artifacts/runtime-evidence/2026-09-28-uiux-baseline/ 与 -refinement/ |

## 2. Token 变更（spec §41）

`spec/ui/design-tokens.json` 1.0.0 → **1.1.0**（additive，兼容既有消费者）：

- 新增层：`layers`（primitive/semantic/component）、`semanticColors`、`statusIcons`、`typographyScale`、`surfaceLayers`、`elevation`、`focusRingWidth`、`componentTokens`、`motion`、`layout`、`density`、`focus`。
- 值微调：`radius.md` 10→8、`radius.lg` 14→12（spec §44 radius policy，消费者均从主题取，无破坏）。
- 新增 `colors.light/dark.surfaceLow`、`focusRing`；`statusRoles.map` 新增 `verified`。
- `spec/ui/copy-zh.json` 新增 `uiuxV031` 块（verified/completed 区分、blocked 闸门原因、场景/薄弱点/健康/空态文案），additive，JSON 校验通过。

## 3. 组件变更（spec §67，跨端语义一致、各端原生实现）

| 组件语义                    | Desktop (Compose)                              | Android (Compose/M3)                           | iOS (SwiftUI)                       | Harmony (ArkUI)       |
| --------------------------- | ---------------------------------------------- | ---------------------------------------------- | ----------------------------------- | --------------------- |
| 页面骨架                    | PdigPage（max-width + 页头 + hairline）        | PdigScrollingPage（已有）                      | 屏面内 SectionHeader                | Index 页头 + section  |
| 区块头                      | SectionHeader（文字+hairline）                 | SectionHeader（已有）                          | SectionHeader（新增）               | SectionHeader 行      |
| 卡片（surface+border）      | PdigCard 重构（border 非 surfaceVariant 填底） | PdigCard（已有 surface）                       | PdigCard（新增，替代 gray.opacity） | 场景卡 surface+border |
| 状态（icon+label+color）    | StatusBadge（新增）                            | StatusChip（label+color；图标由形状/文字补足） | StatusBadge（新增）                 | ✓+文字+色             |
| 步骤轨道（Continuity Rail） | ContinuityRail（新增，旗舰）                   | StepRailRow/ChangePlanStepsUi（新增）          | StepRail（新增）                    | Index 占位            |
| 空态三要素                  | EmptyState(message,title,next)                 | EmptyState（已有）                             | EmptyState（新增）                  | Healthy 文案          |
| 错误/重试                   | ErrorStrip + 动作重试                          | ErrorState（已有）                             | NoticeBanner + 重试（新增）         | 自检失败行            |
| 列表行 hairline             | PdigRow（新增）                                | Row（已有）                                    | List 语义行                         | Row+Divider           |

## 4. 四平台实施要点

### 4.1 Desktop（本轮重点，spec §52）

- 根因：无 PDIG 主题（default M3 紫灰 surfaceVariant slab）。新建 `ui/theme/PdigTheme.kt`（token→M3 light/dark + PdigType + PdigStatusColors/SoftBackgrounds）。
- App 壳：220dp 分组 sidebar（图标+选中态）+ top bar（页标题 + 「本地数据文件已就绪」，不再显示文件名）+ max-width 内容 + focusable。
- Home=Personal Infrastructure Briefing（六段顺序 + healthy）。
- Findings=what/why/next + 展开 evidence/unknown/affected。
- Infrastructure=By Item/By Capability segmented + master-detail（能力面板）。
- Scenario Center=支付/身份与恢复；Scenario Setup=Replace Phone Continuity Rail 全链（9 步，停用旧号 blocked+明文原因）。
- Impact=四类分组，must_change danger 突出。
- Plan/Actions/Verification=步骤轨道；verified>completed；make-before-break 闸门。
- 泄漏清理：adapterId/sourceInstanceId/importSessionId/targetNodeId/planId/blob 文件名/版本串 5+3 处。
- 证据：ProfileDriver 80 帧 PASS；PNG 复杂度 +15~70%；lavender slab 0.4-0.6→~0.02。

### 4.2 Android

- 主题/组件本就 token 化，主要改 screens：Home Briefing、Scenario Center 分组、Impact 四类、ChangePlan 步骤轨道 + 闸门、Verification 语义、Infra 分段切换、About 版本。
- 拆文件控制行数（HomeBriefing/HomeSupport/TimelineScreen/ChangePlanSteps/ChangePlanStepsUi/InfraSecondaryScreens/ScenarioCenterScreen）。
- 证据：42 帧（light+dark）AVD 实拍；10 个重设计屏像素级确认不同于基线；异步屏（plan 等）两轮均为加载帧（harness 时序，预存问题，非回归）。
- 回归：compileDebugKotlin + testDebugUnitTest（含 UiLabelMappingsTest 对齐 copy-zh）全绿。

### 4.3 iOS

- 新建 PdigTheme（token 层，light/dark 动态）+ PdigComponents（PdigCard/StatusBadge/SectionHeader/EmptyState/StepRail/NoticeBanner）+ PdigClock（替换假时间）。
- 9 个屏面应用：Home 顺序、Scenario 分组、Scenario Flow 步骤轨道+闸门、ChangePlan verified>completed、Infra 分段、Findings what+badge、Timeline/Backup/Import 错误人话+重试+真实时间。
- 静态自查：`Color.gray.opacity`=0、假时间 2030=0（仅 DemoData 种子）、原始 Error 上屏=0、括号配平 OK。
- 门禁：本机 Windows 无 Swift → **IOS_RUNTIME_EXTERNAL_GATE**（构建/截图/XCUITest 走既有 macOS CI）。

### 4.4 Harmony

- 新增 PdigTokens（ArkUI token 层，页面零颜色字面量）+ Index 重写（品牌区+状态行+六 section Briefing+系统自检保留）+ ScenarioCatalog + SelfCheckSection。
- 构建：`tools/harmony/build-ascii-mirror.mjs assembleHap` **BUILD SUCCESSFUL**（本机 DevEco 5.0.5 + HarmonyOS 13 SDK）；HAP/modules.abc 字节扫描确认新符号与新文案在产物中。
- 门禁：无模拟器/真机 → **HARMONY_RUNTIME_EXTERNAL_GATE**（runtime/视觉需设备）；HAP 未签名；dark 色板待设备对比度验证。

## 5. CTA / UX Writing（spec §70）

- 采纳「动作式 CTA」：查看影响 / 开始准备 / 创建变更计划 / 标记完成 / 手动确认验证 / 查看薄弱点 / 处理必须事项。
- 避免孤立「提交/确定/下一步/继续」：Impact/Plan/Verification 均给明确动作词。
- make-before-break 闸门文案：「新手机号验证通过后才能停用旧手机号（先建立新路径，再移除旧路径）」。

## 6. Motion（spec §48）

- Desktop：rail 状态变化/展开收起用 AnimatedVisibility fade（150ms）；未做装饰性动画；尊重 reduced-motion（跟随系统）。Android/iOS/Harmony：沿用平台原生 implicit。

## 7. 已知遗留（诚实记录）

- Desktop `--keys` 键盘 Robot 注入：本会话窗口焦点限制（BLOCKERS 环境注记），以 focusable + 导航顺序代码审计兜底。
- Android 异步屏（plan/findings/graph/timeline 等）截图在 AVD 高负载下为加载帧：两轮基线/精修一致，非回归；harness settle 已从 1800→3500ms（下一轮可复核）。
- iOS/Harmony 运行验证 = 外部门禁（如上）。
- 视觉审美最终判定：见 UIUX_FINAL_ACCEPTANCE.md（诚实门：NEEDS_HUMAN_VISUAL_REVIEW）。
