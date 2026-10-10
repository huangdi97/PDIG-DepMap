# IMPLEMENTATION_REPORT.md — vNext 实现清单（contract → code → evidence）

> 2026-09-29 · feat/pdig-ui-vnext · 本报告只列**本机可核验**的实现；三端状态如实标注 PENDING_CONVERGENCE。

## 1. Token 单一真源（TOKEN_CODEGEN = PASS）

| 契约                                                  | 代码                                                                                                                                                                                                      | 验证                                                                                     |
| ----------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| spec/ui-vnext/DESIGN_TOKENS.json（specVersion 2.0.0） | tools/codegen/generate.mjs（扩展 emitPdigV2Tokens{Kotlin,Swift,ArkTs}）                                                                                                                                   | `node tools/codegen/generate.mjs --check` = CODEGEN GATE PASS（含既有 canonical 零漂移） |
| 生成产物（禁止手改）                                  | desktop + android：`com/pdig/uivnext/generated/GeneratedPdigV2Tokens.kt`；ios：`Sources/PDIGApp/Generated/GeneratedPdigV2Tokens.swift`；harmony：`entry/src/main/ets/generated/GeneratedPdigV2Tokens.ets` | 四端关键品牌值一致（primary #4D74FF / canvas #061225 / textPrimary #F4F7FF 等）          |

secret 豁免：core/scripts/check-secrets.mjs `FILE_ALLOWLIST` 增加 `/^spec\/ui-vnext\/DESIGN_TOKENS\.json$/`（与既有 spec/ui/design-tokens.json 同类惯例）。

## 2. Desktop 实现清单（contract → code）

| 契约                              | 实现（desktop/app/src/main/kotlin/com/pdig/uivnext/）                                                                                                                                 | 证据                                                                  |
| --------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------- |
| VNextShell（rail 80/188、top 48） | ui/VNextShell.kt + ui/VNextContentHost.kt                                                                                                                                             | UI_LAYOUT_PROBE.json（pdig.nav.rail 188px / pdig.nav.top 48px @1920） |
| 十屏（IA.md screen registry）     | ui/screens/{NowScreen, OverviewScreen, CardsScreen, CardDetailScreen, NumbersScreen, NumberDetailScreen, ChangePhoneScreen, CustomizationScreen, PersonalizationScreen, ScreenKit}.kt | 90 帧（10 屏 × 5 档案；overview/now × 5 camera）                      |
| Globe（2.5D 状态机）              | globe/VNextGlobe.kt（285 行）+ globe/GlobeMath.kt                                                                                                                                     | 5 相机 × 分辨率帧；drag/zoom/hover/click/focus/arc 代码面             |
| Region List 非视觉替代            | VAppState.regionFilter + 列表 UI                                                                                                                                                      | 键盘可达链                                                            |
| PresentationProfile               | model/UiVNextModels.kt `PresentationProfile`                                                                                                                                          | customization/personalization 帧                                      |
| Privacy Mask                      | VNextShell MaskEnabledIndicator                                                                                                                                                       | 全部帧默认 masked fixture                                             |
| Change Phone 6 阶段               | ChangePhoneScreen（Continuity Rail：completed/current/blocked/verifying/upcoming）                                                                                                    | change-phone 帧                                                       |
| 离屏取证入口                      | Main.kt `--vnext` / `--vnext-shots` + evidence/VNextShotDriver.kt                                                                                                                     | 90 PNG + probe + sha256                                               |

Desktop 编译：`gradlew :app:compileKotlin` **BUILD SUCCESSFUL**。

## 3. 证据包（Desktop 90 帧）

位置：artifacts/runtime-evidence/2026-09-29-ui-vnext/（并复制到 artifacts/ui-vnext-review/desktop/）

- 90 帧 PNG：5 档案（1280x720@1.0 / 1920x1080@1.0 / 2560x1440@1.0 / 1920x1080@1.25 / 1920x1080@1.5）×（8 屏 × 1 + overview × 5 camera + now × 5 camera）
- UI_LAYOUT_PROBE.json：415 行条目（testId/x/y/width/height/visible/enabled）
- EVIDENCE_SHA256SUMS.txt：90 行 SHA-256
- IMAGE_METRICS.json：90 帧量化（JDK ImageIO 工具 tools/ui-vnext/image-metrics/PdigImageMetrics.java）

Image metrics 摘要（如实，90 帧）：

- meanLuminance 0.073–0.135（深色体系预期）
- darkPixelRatio 0.952–0.995
- blankAreaRatio 0.430–0.963：9 帧 >0.9（customization/personalization/number-detail 等低内容密度屏），属深色画布设计预期，留人审
- empty content bbox = 0、全黑/不可读帧 = 0

## 4. core 回归（硬性 0）

`npm run check`（core/）**全绿**：

| 项                              | 结果                         |
| ------------------------------- | ---------------------------- |
| format:check / lint / typecheck | PASS                         |
| 测试                            | 487 tests PASS（≥ 基线 487） |
| architecture                    | circular dependencies = 0    |
| network gate                    | 0                            |
| secret scan                     | 0（含新增 ui-vnext 豁免）    |
| UI gate                         | PASS                         |

canonical（spec/ 非 ui-vnext）零语义改动；Schema v4 persistence / Impact / Failure Domain / Action DAG / Make-Before-Break 未触碰（回归由既有套件覆盖）。

## 5. 遗留项（如实）

| #   | 项                                                         | 状态                                                                                            |
| --- | ---------------------------------------------------------- | ----------------------------------------------------------------------------------------------- |
| 1   | Android / iOS / Harmony 屏幕实现与 LAYOUT_CONTRACT 证据    | PENDING_CONVERGENCE（并行平台 fixer；汇合后回填 BLIND_AGENT_ACCEPTANCE / TEST_RESULTS）         |
| 2   | 真实窗口键盘焦点 / focus-visible 渲染                      | NEEDS_RUNTIME_VERIFICATION（离屏渲染无窗口焦点系统）                                            |
| 3   | 审美判定（Global Digital Infrastructure 是否成立）         | NEEDS_HUMAN_OR_VISION_MODEL（No-Vision 轮禁止自判）                                             |
| 4   | golden baseline                                            | 仅 human-approved 后写入 spec/ui-vnext/golden/（当前 none-approved）                            |
| 5   | IMAGE_METRICS.json 中 `path` 字段为原始 Windows 反斜杠路径 | 既有取证工具产物（JSON 字符串内反斜杠未转义，标准解析器需容错）；本轮未修改，量化为正则容错提取 |
| 6   | push 分支                                                  | 由主 agent 在交付核验后执行（STOP 原则）                                                        |

## 6. 状态声明

- `VISUAL_CONTRACT_IMPLEMENTATION = PASS`（契约实现/几何/token/证据）
- 视觉审美 = **FORBIDDEN**（本轮无视觉通道）
