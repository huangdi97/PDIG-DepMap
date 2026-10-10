# PHASE_1E_IMPLEMENTATION_REPORT.md

> PDIG UI vNext — Desktop Final Visual Acceptance & Interaction Closure（brief 2026-10-05）
> 执行方式：No-Vision 盲实现（确定性 in-process harness），全程契约/证据驱动。

## 状态

```
PHASE_1E_IMPLEMENTATION  = PASS        （契约、编译、测试、证据全部闭环）
DESKTOP_REFERENCE_CANDIDATE = READY
DESKTOP_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_REVIEW
VISUAL_CRAFT             = NEEDS_HUMAN_FINAL_REVIEW
ANDROID_UI_VNEXT         = HOLD
IOS_UI_VNEXT             = HOLD
HARMONY_UI_VNEXT         = HOLD
```

## HEAD / branch / worktree

- HEAD: `3a8f1720`（本轮基线）+ 本轮 8 个 commit（见 Git 小节）
- branch: `feat/pdig-ui-vnext`（未新建第二个 UI branch）
- worktree: `E:\AI\号卡管理`（REPO_ROOT；临时证据仅 `artifacts/runtime-evidence/2026-10-05-ui-vnext-phase1e/` 与 session scratch）

## What changed（本轮）

| 领域               | 变更                                                                                                                                                                                                  |
| ------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Command Palette    | 新增 `ui/CommandPalette.kt`：搜索 card/number/基础设施/更换手机号/个性化 + 直跳；键盘优先（↑↓/Enter/Esc）；接入 TopChrome（原装饰按钮 → 真实入口）                                                    |
| Keyboard-first     | 新增 `ui/VKeyboard.kt`（routeKey / KeyboardFocusState / normalizeKey）；Ctrl+K / Tab / Shift+Tab / Enter / Space / Escape 路由；7 屏可达                                                              |
| Persistence        | 新增 `persist/PresentationProfileStore.kt` + `PresentationProfilePersistenceTest`（4 项）；Studio 保存真实写入 + 重开保留；canonical .depmap 零变化                                                   |
| Import Background  | 新增 `persist/LocalBackgroundImporter.kt` + `LocalBackgroundImporterTest`（7 项）：白名单 ext、size/dimension 上限、decode validation、禁 SVG/远程、sha256 命名、app-managed storage                  |
| Interaction states | 新增 `ui/components/VNextInteraction.kt`（hover/pressed/selected）；Card（hover tilt 3°，§17/§42）、NumberRow、DockAction、MaterialTile、ThemeThumb、ProjectionSelector 接入                          |
| Empty states       | Cards（**§45 文案**）与 Numbers（**§46 文案**）空状态；unknown 不伪装 healthy                                                                                                                         |
| UI copy 合规       | 移除普通 UI 中的 PresentationProfile / PersonalReality / make-before-break / "Presentation Layer" / "PLAN PROJECTION"；改“外观设置只改变显示方式…”；计划投影在普通 UI 使用中文 + “不代表已完成或验证” |
| Continuity scene   | 服务节点含 glyph + 名称 + 关系角色 + 状态；路径 2.0–2.6px；scene 430px（420–500 内）；风险提示改 compact notice（§37）                                                                                |
| Studio             | 右侧 inspector 分组折叠（默认只展开当前组）；accent 6–8 curated swatches（30dp、ring）；中央预览 620–720px @1920；导入图片入口                                                                        |
| Numbers 列表       | 地区/状态/形态过滤功能化（§26）；行 hover；空状态；region glyph + masked number + 依赖计数                                                                                                            |
| Card identity      | 实体卡 EMV chip；虚拟卡无 chip + VirtualMark（§11）；内容层级（issuer/nickname → PAN → network → metadata → status）                                                                                  |
| Evidence harness   | 新增 `evidence/VNextPhaseEvidence1E.kt`（16 主图 + mechanical + probe）与 `evidence/VNextJourney1E.kt`（15 步 + keyboard + persistence）；Main.kt 接入 `--vnext-shots-1e`/`--vnext-journey-1e`        |
| 既有回归修复       | NowScreen 无限高度约束绑定回归（PHASE 1C 遗留，1C/1D 证据未覆盖 Now 而未暴露）→ 修复并可渲染                                                                                                          |
| metrics 工具       | `PdigImageMetrics.java` 增加 JSON 字符串转义（Windows 路径反斜杠不再产出非法 JSON）                                                                                                                   |

## What stayed frozen（未推翻）

- EarthRenderer 架构 / NASA Visible Earth 本地资产 / TextureEarthRenderer 默认 / VectorEarthFallbackRenderer fallback
- Primary（现在/基础设施/变更/记录）+ Secondary（数据源/设置）+ Infra 二级导航结构
- PresentationProfile 分离；CardVisualRenderer 架构；NumberIdentitySurface 概念
- ContinuityScene Current/Transition/After 投影语义；`PLAN PROJECTION != Confirmed Reality`（仅在 docs/tests 内部）
- Number Detail 36% / 32% / 32% 布局
- 未新增 renderer abstraction / 新视觉名词 / 新 token

## 各屏收口

- **Now**：LEFT globe 空间 + RIGHT Now Stream（需要处理/正在进行/即将到来）；修复无限约束回归。
- **Overview / Globe**：地球渲染保持不变；globe-closeup 证据；区域聚焦（HK）+ 标签策略（仅 active/hover/attention 显示标签）。
- **Cards**：3 列网格；空状态；实体/虚拟区分；hover tilt。
- **Card Detail**：hero 舞台 + 主操作（模拟换卡 primary）；绑定服务/影响/备用/历史。
- **Card Studio**：对象库 + 主题（2 列视觉 tile）+ 中央预览 + 折叠 inspector（材质/背景/布局/强调色/信息/隐私）。
- **Numbers**：列表 + inspector；过滤功能化；空状态；行 hover。
- **Number Detail**：36/32/32；状态/角色/恢复集群。
- **Number Studio**：Country/City/Minimal/Banking/Travel/Recovery/Work/Private 预设；通信身份母题（无卡组织 badge）。
- **Change Phone**：ContinuityScene 细节强化 + 投影选择器（Current/Transition/After）可点 + 键盘可切；计划投影仅 After。
- **Personalization**：文案合规化（“本地外观偏好”），隐私遮蔽开关保留。

## Interaction / persistence / a11y / responsive / keyboard

- **Interaction**：hover/focus/pressed/selected 已在 6 类元素实现（Region node 由 globe 自有点击/hover 状态覆盖；Service node 由 scene 状态语义覆盖）；`VNextInteractionContractTest` 6 项。
- **Persistence**：`PresentationProfilePersistenceTest` 4 项 + journey `PROFILE_PERSISTENCE_EVIDENCE.json`（编辑→保存→重开保留；canonical 零变化）。
- **Accessibility**：keyboard（7 屏）、focus-visible（focusable + ring）、reduced motion（Card tilt 关闭、Globe idle 停、无 idle 动画）、Region List alternative（Overview inspector 保留）。
- **Responsive**：mechanical profiles（1280/2560/1.25/1.5）渲染全部成功；1280 为局部重排（不整体缩放）；2560 content max width。
- **Keyboard**：Ctrl+K / Tab / Shift+Tab / Enter / Space / Escape 全部路由 + 测试；palette 检索（"card" → 打开卡片列表）。

## Tests

- `desktop :app:test` — **BUILD SUCCESSFUL，全部 PASS**：
  - `PresentationProfilePersistenceTest`（4 项：重开保留 / canonical 零变化 / delete 幂等 / 多 profile 共存）
  - `LocalBackgroundImporterTest`（7 项：合法导入 / SVG 拒绝 / 伪扩展名拒绝 / 超大文件 / 超尺寸 / canonical 零新文件 / 缺失文件）
  - `VNextInteractionContractTest`（6 项：投影往返渲染 / 计划投影仅 After / palette 检索执行 / 7 屏键盘可达 / Tab-ShiftTab-Enter-Space-Escape / palette id 唯一）
  - 既有：DepmapFileStoreTest / DesktopSessionTest / DiscoveryEngineTest 全部 PASS
- `core`：typecheck / lint / architecture（circular deps = 0）/ secrets / network / **487 测试 PASS**（回归健康；本轮 core/spec/fixtures 零改动）。
- 语法/复杂度 gate：`node scripts/quality/check-quality.mjs` — 本轮新增文件全部 ≤300 行；唯一新违规（ContinuityScene 307 行）已在收口前拆分修复；剩余 FAIL 均为**既有遗留**（legacy `Kit.kt` 528 行、`HomeScreen.kt` 201 行、`MainActivity.kt:58` lateinit 行号漂移 —— 不在本轮范围，未触碰）。

## Evidence（artifact）

- `artifacts/runtime-evidence/2026-10-05-ui-vnext-phase1e/`
  - `profiles/1920x1080@1.0/`：**16 张** Human Review 主截图（§71，隐私遮蔽 ON）
  - `mechanical/`：1280×720、2560×1440、1920@1.25、1920@1.5（overview/studio/change-phone/number-detail/cards）
  - `journey/`：15 步 journey 关键帧 + `INTERACTION_LOG.txt` + `KEYBOARD_LOG.txt`
  - `IMAGE_METRICS.json`：16 帧 0 error / 0 empty / 0 near-black（meanLum 0.062–0.160）
  - `UI_LAYOUT_PROBE.json`：scene 430px、studio preview ≥620px @1920、Number Detail 36/32/32
  - `PROFILE_PERSISTENCE_EVIDENCE.json`；`EVIDENCE_SHA256SUMS.txt`
- 画廊：`docs/ui-vnext/SCREENSHOTS_PHASE1E.md` + `docs/ui-vnext/gallery-phase1e/index.html`（仅 16 张，Global→Assets→Customization→Continuity→States）

## Remaining visual limitations（如实）

1. 无视觉能力：所有“视觉”结论来自 contract/probe/geometry/metrics；sample 截图请 Human 直接查看。
2. Light theme：仅确认 dark-first；`LIGHT = FUNCTIONAL_SUPPORTED / VISUAL_REFINEMENT_LATER`，未做 full parity（不谎称）。
3. 服务节点 hover 与 keyframe 渲染为状态语义驱动（scene 状态标签），非每节点独立 pointer hit-test。
4. Custom background 导入在离屏环境经 importer 路径验证（证据 seam `lastImportedBackgroundPath`），真实文件对话框未在 OS 层执行（in-process 标准）。
5. Command palette 中文输入为 search-as-you-type 累积（字母/数字）；IME 组合输入未单独验证。
6. 1280 下限与 font scaling 以 mechanical probe 覆盖，未做跨字体渲染回归。

## Git（本轮小步 commit，`feat/pdig-ui-vnext`）

1. `feat(ui-vnext): PHASE 1E groundwork — command palette + keyboard-first, PresentationProfile persistence + test, empty states, copy compliance, card identity gating, continuity roles + compact notice`
2. `feat(ui-vnext): PHASE 1E studio inspector collapsible groups + curated accents, functional Numbers filters, ContinuityScene service-role nodes`
3. `feat(ui-vnext): PHASE 1E evidence harness — 16 shots + mechanical + journey + NowScreen scroll fix + PdigImageMetrics escaping`
4. `feat(ui-vnext): PHASE 1E interaction contract tests + hover/focus/pressed states`
5. `feat(ui-vnext): PHASE 1E import local background + tests; studio wiring`
6. `chore(ui-vnext): regenerate PHASE 1E evidence after fixes`
7. `refactor(ui-vnext): split ContinuityScene drawing helpers (<=300 lines)`

push origin：完成（见 Git 输出）。禁令遵守：无 force push / rebase / reset --hard / tag 移动 / 旧 release assets 触碰。

## 结束语

按 brief §78：本轮只写 `PHASE_1E_IMPLEMENTATION = PASS`、`DESKTOP_REFERENCE_CANDIDATE = READY`、`DESKTOP_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_REVIEW`、`VISUAL_CRAFT = NEEDS_HUMAN_FINAL_REVIEW`；不进入 Android/iOS/Harmony，不 merge main，不发布 v0.3.2，不开始 v0.4。等待 Human `DESKTOP_VISUAL_REFERENCE = ACCEPTED`。
