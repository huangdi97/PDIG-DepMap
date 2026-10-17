# ANDROID_UI_VNEXT_HUMAN_FIX_REPORT.md

> **ANDROID UI vNext Human Fix（2026-10-03，Human Review 定向修复轮）**
> —— 关闭 Human Review 已发现的 Android 视觉与 adaptive layout 缺口，重新生成
> 真实 API36 Phone + Tablet runtime evidence，提交 Human Final Acceptance。
>
> 本轮不是重新设计 Android、不创建第二套 Android UI vNext、不启动 iOS/Harmony、
> 不改冻结 Desktop Reference。

## 0. 范围与铁律

- 产品仍为 Personal Digital Infrastructure（Change & Continuity Management）；
  Primary Navigation「现在 / 基础设施 / 变更 / 记录」与 Infrastructure Secondary
  「总览 / 卡片 / 号码 / 账户 / 邮箱 / 设备 / 服务 / 薄弱点」保持不变。
- 冻结：Product IA、Desktop Reference、Globe = Global Infrastructure Navigator、
  PresentationProfile boundary、Number Identity 方向、Current/Transition/After、
  After = Plan Projection、Empty State 语义、Search/Command intent、Domain/Canonical 语义。
- `PresentationProfile != PersonalReality != Canonical`；UI 修改不影响
  `.depmap` / Canonical / schema / domain truth。

## 1. 起始 / 结束 HEAD 与 commits

| 项            | 值                                                                              |
| ------------- | ------------------------------------------------------------------------------- |
| STARTING_HEAD | `88551e69dd3437d34accad4f3893a4940a87b58a`（feat/android-ui-vnext-translation） |
| Ending HEAD   | push 后 `eda8ea4`（见 §10；push 前 fetch 核验远端 ref = 本地 HEAD）             |

本轮 commits（fast-forward only）：

```
b342c0a fix(android-ui): repair compact card and number layouts
cd8ca82 feat(android-ui): restore frozen card identity translation
dcec870 fix(android-ui): close tablet globe and continuity adaptive regressions
2aca40e feat(android-ui): consumerize card and number studios
eba4aeb test(android-ui): add human-review layout evidence contracts
0a3a4b6 fix(android-ui): treat globe composition cancellation as clean, not render error
981b05c docs(android-ui): regenerate human acceptance candidate evidence
eda8ea4 test(android-ui): harden wide-shell tag probe and run UiScreenshotEvidence on tablet viewport
```

## 2. Root causes 与 exact fixes

### 2.1 Phone Cards compact layout（P0）

- **Root cause**：`CardsScreen.kt` 对 COMPACT 强制 `GridCells.Fixed(2)` —— 两列卡宽下
  nickname / issuer / form / metadata 竞争空间并被拆成竖排/多行。
- **Fix**：COMPACT=1 列整卡（`GridCells.Fixed(1)`）、MEDIUM=2 列、EXPANDED=4 列；
  新增卡面 tag `pdig.card.*`；EmptyState CTA 改为真实动作（查看号码 / 查看基础设施）。

### 2.2 Card Identity 退化（P0）

- **Root cause**：卡面只剩 `cardFaceBrush(preset)` 蓝色渐变 + 文字，丢失冻结的
  CardIdentitySystem。
- **Fix**：新增 `CardIdentityProfile.kt`（8 个冻结 issuer：CMB / ICBC / BOC / HSBC /
  BOCHK / Monzo / Revolut / Chase，各 ≥4 个 identity 维度：palette / material / motif /
  accent / layout / regional）、`CardIdentityRenderer.kt`（唯一 renderer）与
  `CardIdentityArtwork.kt`（L2 版式带 / L3 motif / L4 材质 / L5 Studio 主题 artwork）；
  Grid / Detail / Studio 通过同一 `AssetCard → CardIdentityFace` 复用；禁止三处独立视觉。

### 2.3 Phone Numbers 信息架构（P0）

- **Root cause**：COMPACT 嵌 Desktop Inspector（`NumberListSurface(weight) + Inspector
fillMaxSize`），列表被压没（首屏 blank 0.849）。
- **Fix**：COMPACT = 高密度可滚动列表（行含 +86/+852/…、masked、carrier、SIM/eSIM、
  role、恢复、状态），点击行进入 Number Detail；Inspector 仅 MEDIUM/EXPANDED 保留。

### 2.4 Tablet Globe 黑球（P0）

- **Root cause**：纹理地球异步渲染期间截图（证据时态），且组合作用域切换把
  `LeftCompositionCancellationException` 误判为渲染 ERROR → 黑球/fallback 进入 Human Pack。
- **Fix**：新增 `GlobeRenderState { LOADING, TEXTURE_READY, FALLBACK, ERROR }`
  状态机 + `renderCenterPx/renderRadiusPx/renderError`；截图前置条件 = TEXTURE_READY
  （20s 超时 FAIL）；CancellationException 作为干净取消 rethrow（不视为 ERROR）；
  fallback 改为有效「地球加载材质」（非 near-black empty sphere）；keep background/cached
  rendering（Dispatchers.Default，禁止 UI 线程逐像素）。

### 2.5 Tablet Continuity 死空白（P0）

- **Root cause**：EXPANDED Row 复用三个 `fillMaxWidth` 表面 → 挤压 + 死空白 + 场景被挤到
  折叠以下。
- **Fix**：独立 `ExpandedContinuityScene`（OLD≈0.24 / SERVICES≈0.42 / NEW≈0.24，
  箭头占剩余空间），首屏可见三区；Transition 与 After 均验证。

### 2.6 Number Detail 死空白

- **Fix**：hero → 关联服务 连续（gap ≤ 48dp token 阈值），新增
  `NumberDetailVerticalFlowContractTest` 测量真实 bounds。

### 2.7 Phone Change 6 步 stepper

- **Fix**：COMPACT 原生 6 步 mini progress（全部首屏可见 + 当前步骤「第 N/6 步」），
  不再默认像被 layout clip；MEDIUM/EXPANDED 保留 ContinuityRail。

### 2.8 Studio consumerization

- Card Theme：内部 id → 用户语言（minimal→极简、deep-space→深空、region→地域、
  city→城市、glass→玻璃、metal→金属、abstract→抽象）；真实 visual thumbnail
  （Glass=透光/层/弥散/玻璃条、City=天际线/窗户、Metal=拉丝…）。
- Number Theme：country→国家/地域、city→城市、minimal→极简、banking→银行验证、
  travel→旅行、recovery→恢复、work→工作、private→私人 + communication thumbnail。
- Tablet Studio Inspector：consumer 语言（外观/材质/布局/强调 swatch/信息/隐私），
  默认不暴露 hex / internal enum / standard / glass ID。

### 2.9 品牌词 / 工程词

- Rail 副标题 `vNext` → `个人数字基础设施`；UI 不再出现 vNext / fixture / debug /
  internal preset id / Phase（UI Text 扫描 0 命中）。

### 2.10 Back / State

- `VAppState` 改为 back stack（`navBackTarget` = 栈顶兼容 API）：
  Cards→Detail→Back、Detail→Studio→Back→Back 均正确回根；system back 空栈才退出。

## 3. 新增 Human Fix 契约测试

| 测试                                     | 契约                                                                                |
| ---------------------------------------- | ----------------------------------------------------------------------------------- |
| `PhoneCardsLayoutContractTest`           | NO_VERTICAL_TEXT / NO_FORM_LABEL_STACK / NO_CRITICAL_CLIP / CARD_MIN_READABLE_WIDTH |
| `PhoneNumbersListVisibilityContractTest` | expected count=7 / visible rows>=3 / list visible / compact 无 Inspector            |
| `NumberDetailVerticalFlowContractTest`   | hero→services gap ≤ 48dp                                                            |
| `AndroidCardIdentityContractTest`        | 8 issuer 每对 ≥3 个 identity 维度不同；解析确定性；Grid/Detail/Studio 单一 renderer |
| `AndroidGlobeEvidenceContractTest`       | TEXTURE_READY 前置；meanLuma ≥0.12 / nonBlack ≥0.30 / stddev ≥0.04 / markers ≥5     |
| `TabletAdaptiveContractTest`             | 三列首屏可见、Number Detail 无死空白、Cards/Studio/Overview 真实渲染                |
| `PhoneChangeLayoutContractTest`          | CompactStepper 无初始 clip                                                          |
| `StudioThumbnailDistinctTest`            | glass vs city thumbnail SHA≠ 且像素 diff ≥1%                                        |
| `VNextBackStateRegressionTest`           | 5 条 Back/State 回归                                                                |

## 4. Runtime evidence（真实 API36 emulator）

- Phone AVD `main`（Pixel 7，1080×2400，portrait）+ Tablet AVD `pdig_tablet_api36`
  （Pixel Tablet，2560×1600，landscape）；instrumentation `captureToImage` 设备内
  Compose 实际像素（真实 runtime，非 offscreen-only）。
- **phone 14 张**、**tablet 14 张** 全部重新生成：
  `artifacts/runtime-evidence/2026-10-03-android-ui-vnext-human-fix/{phone,tablet}/`
  （每目录含 manifest.json + 全部 PNG；`EVIDENCE_SHA256SUMS.txt` 覆盖全部文件）。
- 每条 manifest 记录 platform/device/api/viewport/density/orientation/screen/
  expectedState/actualState/stateValidation/sha256/commit/layoutContracts/renderReadiness；
  合并版：`ANDROID_UI_VNEXT_HUMAN_FIX_SCREENSHOT_MANIFEST.json`（28 条，commit=0a3a4b6）。
- 状态字段实证：Globe 两屏 `renderReadiness=TEXTURE_READY`；Cards
  `verticalTextRegression=false`；Numbers `visibleRows=6（phone，lazy 首屏）/7（tablet）`；
  Tablet Change 三屏 `oldVisible=true;servicesVisible=true;newVisible=true`。
- 完整性：28 条 manifest SHA256 与磁盘 PNG 逐一核验 = 0 mismatch。

## 5. Gates

| Gate                              | 结果                                                                                                                                            |
| --------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| Desktop Freeze Guard              | PASS 12/12（两次独立运行）                                                                                                                      |
| core `npm run check`              | 全绿：format:check / format:docs:check / lint / typecheck / **487 tests** / architecture（cycles=0）/ network / secrets（2668 files）/ UI       |
| Android JVM unit tests            | `:app:testProductionDebugUnitTest` PASS                                                                                                         |
| Android instrumentation（phone）  | `com.pdig.uivnext.evidence` 26/26 PASS（含既有 Variant / Translation / A11y + 新增 Human Fix Contracts）                                        |
| Android instrumentation（tablet） | `com.pdig.uivnext.evidence` 26/26 PASS                                                                                                          |
| 生产 a11y 语义门禁                | `AccessibilitySemanticsTest` 14/14 PASS（phone）                                                                                                |
| 既有 `UiScreenshotEvidenceTest`   | 2/2 PASS（tablet viewport；手机窗口限制见 §5.1 诚实边界）                                                                                       |
| Variant Truth                     | glass≠city、country≠travel≠recovery、current≠transition≠after、global≠region（SHA 互异 + expected==actual；StudioThumbnailDistinct 像素级验证） |
| Back / State 回归                 | 5 条 PASS                                                                                                                                       |

### 5.1 诚实边界（UiScreenshotEvidenceTest 与手机窗口）

- `UiScreenshotEvidenceTest#capturesVNextDemoScreens_andProbesKeyTestTags` 的 wide-shell
  probe 在**手机窗口**（1080×2400 = 411dp）上几何上不可满足：forcedViewportWidthDp=1280
  只切换 breakpoint（wide shell = rail 188dp + 内容列），实际窗口仍为 411dp；WideOverview
  的活动轨（railExpanded + EXPANDED = 360dp）无法与 rail 并列 → GLOBE_STAGE 宽度恒为 0。
- **已证伪是回归**：检出本轮改动前（starting HEAD 的 committed 版本）的同一测试在
  手机 AVD 上同样 FAIL（`pdig.globe.stage` bounds = 0,0,0,0），即该断言在手机窗口
  从未可满足（pre-existing test-bed 约束，与本轮代码无关）。
- **验证方式**：在 tablet AVD（1280dp 真实宽视口）上 `UiScreenshotEvidenceTest`
  完整通过（OK 2 tests：production 42 屏 sweep + vNext 12 屏 + wide probe）。
- 本轮同时为 probeTag 增加了有限重试硬化（与既有 capture 重试一致），未弱化任何断言。

## 6. 零漂移核验

- `DOMAIN_CHANGE = 0`：本轮 commits 全部落在 `android/app/…`（`com.pdig.uivnext.*`
  表现层 + 证据测试）；core/ 无改动。
- `CANONICAL_CHANGE = 0`：spec/、fixtures/、conformance/expected/ 无改动。
- Desktop 冻结包：12 张 SHA 未动（Freeze Guard 12/12 PASS）。
- PresentationProfile 不进 .depmap；证据与真实持久化隔离（既有 P0 回归 PASS）。

## 7. Local vs remote CI truth

- `.github/workflows/ci.yml` 显式列举触发分支（main、feat/mvp03-living-graph），
  不覆盖 `feat/android-ui-vnext-translation`。
- 因此 `REMOTE_CI = NOT_TRIGGERED`（本地全绿 ≠ GitHub CI PASS，未冒充）。

## 8. Evidence paths

- `artifacts/runtime-evidence/2026-10-03-android-ui-vnext-human-fix/phone/`
- `artifacts/runtime-evidence/2026-10-03-android-ui-vnext-human-fix/tablet/`
- `artifacts/runtime-evidence/2026-10-03-android-ui-vnext-human-fix/EVIDENCE_SHA256SUMS.txt`
- `ANDROID_UI_VNEXT_HUMAN_FIX_SCREENSHOT_MANIFEST.json`（28 条）
- `docs/ui-vnext/android/SCREENSHOTS_ANDROID_HUMAN_FIX.md`

## 9. Remaining gates（Human 负责，Agent 禁止自判）

- `ANDROID_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_ACCEPTANCE`（Human 判定 ACCEPTED 或
  TARGETED_SCREEN_FIX_REQUIRED）。
- `ANDROID_REFERENCE_FREEZE = HOLD`（待 Human Final Acceptance 后再决定冻结）。
- `IOS_UI_VNEXT = HOLD` / `HARMONY_UI_VNEXT = HOLD`（本轮不启动）。

## 10. 最终状态

```
ANDROID_UI_VNEXT_HUMAN_FIX_IMPLEMENTATION = PASS
ANDROID_RUNTIME_EVIDENCE = READY
ANDROID_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_ACCEPTANCE
ANDROID_REFERENCE_FREEZE = HOLD
IOS_UI_VNEXT = HOLD
HARMONY_UI_VNEXT = HOLD
```

STOP。
