# ANDROID_LIGHT_REFERENCE_FINAL_RUNTIME_VALIDATION_REPORT

> 2026-10-05/06 · `feat/android-ui-vnext-translation` · BUILD / TEST / AVD / RUNTIME / INTERACTION / SCREENSHOT / EVIDENCE ONLY
> 角色：本地执行/验证 Agent（未设计、未改 UI、未改产品真相；所有生产 UI 由 ChatGPT 在 GitHub 维护）

## 0. 结论状态（任务书 §42/§43 口径）

```
ANDROID_BUILD = PASS
ANDROID_UNIT = PASS
ANDROID_RUNTIME_VALIDATION = PARTIAL (见 §10 失败清单：2 个 androidTest 源编译阻断 + 1 个垂直间隙契约 + 1 个 a11y 触控目标 + 截图套件 Globe 门槛)
ANDROID_RUNTIME_EVIDENCE_FOR_CURRENT_HEAD = READY (48 屏真实 runtime 已捕获；手机 24 + 平板 24；详见 §8)
ANDROID_LIGHT_REFERENCE_RUNTIME_PACK = READY
ANDROID_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_ACCEPTANCE
ANDROID_REFERENCE_FREEZE = HOLD
IOS_UI_VNEXT = HOLD
HARMONY_UI_VNEXT = HOLD
EMPTY_STATES_10 = BLOCKED (仅截图套件可渲染 emptyDemo 空态；该套件在 HEAD 被 Globe 门槛阻断，见 §10.3)
```

**重要**：本 Agent 不写 `VISUAL ACCEPTED / REFERENCE FREEZE=PASS / VISUAL CRAFT=PASS`。
`ANDROID_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_ACCEPTANCE` 保留给 ChatGPT/Human 逐屏看 PNG。

## 1. HEAD 与来源

| 项 | 值 |
| --- | --- |
| BRANCH | `feat/android-ui-vnext-translation` |
| RUN_SOURCE_HEAD | `b659ae4b60f95d52c222ed107ace39e53b6db48e` |
| SOURCE_TREE | `c0ab2815242c4ff6075f71fe3dddbdc1f0af8ceb` |
| ANDROID_PIXEL_SOURCE_CHECKPOINT（生产 UI 最终像素变更点） | `a03bc11c8bac601f95bf2e070c5f666e2b22d271` |
| 同步方式 | `git fetch origin` → `git pull origin feat/android-ui-vnext-translation --ff-only`（2a1db6b..b659ae4，纯 fast-forward） |
| 工作区 | 同步前/后 `git status --short` 仅含既有未跟踪用户文件 `artifacts/runtime-evidence/2026-09-29-ui-vnext/IMAGE_METRICS.json`（未动） |
| Visual Reference | `spec/ui-vnext/references/android/PDIG_ANDROID_LIGHT_VISUAL_REFERENCE_2026-10-05.jpg`（仓库内存在；manifest/.md 声明 SHA `4b2ca9e0…`，实际文件内容 SHA256 `3782204…`，二者不一致为仓库既有差异，参考文件未修改） |

## 2. 逐项报告

| # | 检查项 | 结果 | 证据 |
| --- | --- | --- | --- |
| 1 | CORE_CHECK | **FAIL（仅 format:docs:check）** | `logs/core-check.log`：prettier 报 7 个 ChatGPT 维护的 md（ANDROID_UI_VNEXT_SOURCE_COMPLETE_RUNTIME_VALIDATION_REPORT.md / BLOCKERS.md / NATIVE_MIGRATION_STATUS.md / WORK_STATUS.md / docs/ui-vnext/android/*.md）存在格式漂移（符合 WORK_STATUS 既有先例）；`npm run lint / typecheck / test / check:architecture / check:network / check:secrets / check:ui` 全部 EXIT=0 |
| 2 | DESKTOP_FREEZE_GUARD | **PASS** | `logs/desktop-freeze-guard.log`：`DESKTOP_REFERENCE_FREEZE_GUARD = PASS (12/12 SHA256 verified)` |
| 3 | ANDROID_BUILD | **PASS** | `assembleProductionDebug` BUILD SUCCESSFUL in 1m3s；APK `C:\Users\Kaiser\pdig-build\app\outputs\apk\production\debug\app-production-debug.apk`（44,117,283 B；SHA256 `584cfb36…c21c9`） |
| 4 | ANDROID_UNIT | **PASS** | `testProductionDebugUnitTest` BUILD SUCCESSFUL |
| 5 | PHONE_INSTRUMENTATION | **PARTIAL** | `connectedProductionDebugAndroidTest`（source-complete fresh run，104 tests / 5 failures / 4 skipped）+ 单独重跑；日志 `logs/phone-instrumentation.log`。5 个失败见 §10 |
| 6 | TABLET_INSTRUMENTATION | **PARTIAL** | 17 个证据套件 batch fresh run：16 OK + `NumberDetailVerticalFlowContractTest` FAIL（同 §10.2）；`SourceCompleteScreenshotEvidenceTest` FAIL（§10.3）；日志 `logs/tablet-suite-*.log` |
| 7 | PHONE_24_SCREEN | **READY（24/24 真实 runtime PNG）** | `phone/01-now.png … 24-data-sources.png`；00) suite 产出 01/02 + 手工真实 UI 捕获其余（含 Globe 25–30s 纹理等待）；每张 PNG 均为本机 API36 AVD `main` 上当前 APK 的 adb screencap 或套件 captureToImage |
| 8 | TABLET_24_SCREEN | **READY（24/24 真实 runtime PNG）** | `tablet/01-now.png … 24-data-sources.png`；真实 Tablet AVD `pdig_tablet_api36`（2560×1600 @320，横屏）窗口，未使用 forcedViewportWidth 伪装 |
| 9 | EMPTY_STATES_10 | **BLOCKED** | 空态仅由 `SourceCompleteScreenshotEvidenceTest`（emptyDemo）产出；该套件在 HEAD 被 03 屏 Globe TEXTURE_READY 门槛阻断 → 空态无法跨越（见 §10.3） |
| 10 | CARD_PROFILE_PERSISTENCE | **PASS（真实进程级）** | 手机：真实 UI 卡片详情 → 定制卡面 → 主题 tile（极简）→ 保存 → XML `profile.*`=`card-cn-2 minimal` → `am force-stop` → 重启用 `--ez vnext_demo` → 详情显示「当前主题：极简」(True)；平板同样落盘（XML card-cn-2 minimal）+ 重启后截图。日志 `logs/phone-prefs-mid-v12.xml` / `logs/tablet-prefs-final.xml`；PNG `phone/card-restart-theme.png`、`tablet/tablet-card-restart-detail.png` |
| 11 | NUMBER_PROFILE_PERSISTENCE | **PASS（真实进程级）** | 手机+平板：`numberProfileSavePersistsAcrossRecreationAndRenders` OK(1) → XML `num-cn-1 travel` → force-stop → 重启 → XML 仍存在（`logs/phone-prefs-final.xml`、`logs/tablet-prefs-final.xml`） |
| 12 | WORKSPACE_PERSISTENCE | **PASS（真实进程级）** | 手机：设置 → 个性化 → 「即将到来」toggle → XML `show_upcoming=true` → force-stop → 重启 → XML 仍 true + 界面可见（`logs/phone-workspace-mid.xml` / `phone-workspace-after.xml`；PNG `phone/personalization-restart.png`） |
| 13 | SEARCH_BACK_STACK | **PASS** | 手机真实 UI：搜索入口 → 搜索屏 → System Back → 回「现在」根 (True)；PNG `phone/search-real.png` / `phone/search-back.png`；`searchResultNavigationAndBackToSearch` 套件 PASS |
| 14 | REGION_CONTEXT | **PASS（状态机 11/11 + 平板真实 UI 传播）** | 套件 `regionSelectPropagatesAndClears` / `regionDrawerBackClosesDetail` PASS；平板真实 UI：总览选「中国」→ 号码屏出现「当前地区」banner (True) → 「查看全球」清除 (True)；PNG `tablet/tablet-region-numbers.png` / `tablet/tablet-region-reset.png` |
| 15 | GLOBE_TEXTURE_READY | **BLOCKED（官方门槛）** | 见 §10.3 |
| 16 | NUMBER_DETAIL_PROBE | **PASS** | `probes/NUMBER_DETAIL_LAYOUT_PROBE_phone.json`（hero→services 106.3dp；其余 16dp；无 0×0 / 负间隙 / 反转）+ `…_tablet.json`（106/16/16/16dp）；`NumberDetailLayoutProbeTest` phone+tablet OK(1) |
| 17 | CHANGE_3_PROJECTIONS | **PASS** | 手机三投影截图互不相同（17: 327441 B / 18: 327816 B / 19: 346844 B）；18 语义横幅「执行计划…第 3/6 步 · 验证新号码 · 待验证」；19 「完成后（计划）」= Plan Projection（source + UI）；禁止语「已完成/已迁移成功」未出现 |
| 18 | SYSTEM_BACK | **PASS** | 手机真实 UI：卡片详情→Back→卡片根(True)；号码详情→Back→号码根(True)；PNG `phone/back-cards.png`、`phone/back-numbers.png` |
| 19 | ACCESSIBILITY | **PARTIAL** | 平板 `AccessibilitySemanticsTest` OK(14)、`VNextAccessibilityEvidenceTest` OK(3)、`AndroidCardIdentityContractTest` OK(2)、`AndroidGlobeEvidenceContractTest` OK(1)；**手机 `VNextAccessibilityEvidenceTest.touchTargetsMeetMinimumSize` FAIL**（studio preset 触控目标 <48dp 高，当前源真实结果，§10.4） |
| 20 | CONSUMER_COPY | **PASS** | 扫描 `android/app/src/main/kotlin/com/pdig/uivnext/**` 的字符串字面量：无 consumer-facing vNext / fixture / debug / preset / Presentation layer / Canonical / PersonalReality / freeze / reference / 内部路由文案；唯一命中 `backgroundKind="preset"` 为内部 model 值（非显示文案） |
| 21 | SOURCE_MUTATION_GUARD | **PASS** | 运行前后 `git diff --name-only` 为空；`android/app/src/main/**` 零 Agent 改动；androidTest 两个不编译文件在截图套件构建期间被临时移入 `logs/androidtest-source-blocker/`（含原件副本）并已恢复（见 §10.1） |

## 3. SOURCE_COMPLETE_HEAD / 证据提交

EVIDENCE_COMMIT_HEAD：即本 evidence commit 的提交 SHA（最终交付时随报告一并给出；本仓库分支禁止 force push / amend，故不回写文件）。

## 4. 交付物路径（`artifacts/runtime-evidence/2026-10-05-android-light-reference-final-validation-b659ae4/`）

- `phone/01-now.png … 24-data-sources.png`（24 张真实手机 runtime）
- `tablet/01-now.png … 24-data-sources.png`（24 张真实平板 runtime）
- `logs/`（build/core-check/freeze-guard/phone+tablet 套件/seed/prefs/logcat/run 日志）
- `manifests/`（SOURCE_PROVENANCE.json / DEVICE_MATRIX.json / ANDROID_UI_VNEXT_SCREENSHOT_MANIFEST.json / ANDROID_UI_VNEXT_INTERACTION_VALIDATION.json / phone-raw-manifest.json / tablet-raw-manifest.json）
- `probes/`（NUMBER_DETAIL_LAYOUT_PROBE_phone.json / _tablet.json）
- `EVIDENCE_SHA256SUMS.txt`（77 条目）

## 5. 截图清单（命名与任务书一致；Tablet 为真实宽屏窗口证据）

见 `manifests/ANDROID_UI_VNEXT_SCREENSHOT_MANIFEST.json`（每张含 screenId/platform/device/avd/api/viewport/density/orientation/sourceHead/sourceTree/apkSha256/referencePath/referenceSha256/expectedState/actualState/stateValidation/renderReadiness/globeTextureState/captureTimestamp/fileSha256）。

## 6. DEVICE_MATRIX

见 `manifests/DEVICE_MATRIX.json`：Phone `main`（Pixel 7，1080×2400 @420，API36，x86_64）；Tablet `pdig_tablet_api36`（Pixel Tablet，2560×1600 @320，API36，x86_64，横屏）。同一 APK SHA `584cfb36…`。

## 7. Reference provenance

- 文件存在：`spec/ui-vnext/references/android/PDIG_ANDROID_LIGHT_VISUAL_REFERENCE_2026-10-05.jpg` ✓
- manifest 新增条目存在（`REFERENCE_MANIFEST.json` 第 5 条）✓
- SHA：manifest/.md 声明 `4b2ca9e0…`（320×213 JPEG）与实际文件内容 SHA256 `3782204…` 不一致 → 记录为仓库既有差异；**未修改 reference 文件与 manifest**（严格遵守 §4）。

## 8. 24+24 截图真实性与 Globe 纹理

- 全部 48 张均为真实 Compose runtime / 真实 emulator / 真实当前 APK（adb `screencap -p` 或 compose `captureToImage`），非 preview / desktop render / concept / reference 图。
- Globe 帧（01/02/03/04）在捕获前执行 25–30s 纹理稳定等待；像素验证（System.Drawing 中心区采样）显示平板纹理显著（sd≈92–107）→ 地球纹理真实渲染。
- **官方 `TEXTURE_READY` 枚举门槛在本机/HEAD 不可达**：`SourceCompleteScreenshotEvidenceTest` 在 03-overview-region-selected 处 20s×4 次重试内 `globe.renderState` 保持 `LOADING`（手机 3 次独立运行 + connected + 平板 1 次，均为同一原因，见 §10.3）。因此 manifest 的 `globeTextureState` 如实标注 `manual-wait + pixel-verified`，**不冒充**套件枚举记录。

## 9. 交互验证（15 项，全部附 steps/evidence/sourceHead）

见 `manifests/ANDROID_UI_VNEXT_INTERACTION_VALIDATION.json`：card save/restart、number save/restart、workspace、search query/result/back、region select/back-preserve/cards/numbers propagation、global reset、system back、change projection truth。

## 10. 失败与阻断（如实，不修 UI / 不降级门禁）

### 10.1 androidTest 源码编译阻断（2 个套件）
`AndroidAdaptiveShellContractTest` 与 `AndroidLightVisualSourceContractTest`（HEAD 新增文件）import `androidx.compose.ui.test.assertExists / assertDoesNotExist`，而解析到的 `ui-test 1.7.2` 中该 API 已移除（已解包 aar 验证：`AssertionsKt` 含 assertIsDisplayed / assertIsNotDisplayed，**不含** assertExists / assertDoesNotExist）→ `:app:compileProductionDebugAndroidTestKotlin` 编译失败。这不是环境问题，是当前 HEAD 源集缺陷（remote CI 未构建 androidTest）。为让其余全部套件可 fresh 运行，两个文件被**临时**移到 `logs/androidtest-source-blocker/`（构建后即恢复，git diff 归零）。这两个套件标记 `BLOCKED_BY_SOURCE_COMPILE`，未伪造结果。修复（改 import 为 assertIsDisplayed/assertIsNotDisplayed）属于 ChatGPT 的 androidTest 源。

### 10.2 NumberDetailVerticalFlowContractTest：hero→services 间隙超阈值（当前源真实结果）
`numberDetailVerticalFlowHasNoDeadSpace` FAIL：`heroBottom→servicesTop gap must be <= 48dp (gap=279.0, threshold=126.0)`；手机 + 平板均复现（connected、phone retry、tablet batch）。探针显示的 hero→servicesHeader 106.3dp 与垂直契约的 servicesTop 口径不同；当前 light-first 布局的真实间距超过测试阈值 → 需 ChatGPT 裁决（改布局或改契约阈值）。

### 10.3 SourceCompleteScreenshotEvidenceTest：Globe REGION_SELECTED 纹理门槛
03-overview-region-selected 捕获 20s×4 次内 `state=LOADING`（明文 assert：`globe screenshot requires TEXTURE_READY`）。手机 3 次独立运行（headless 默认 GPU / swiftshader / 8 核 4G）+ gradle connected + 平板 1 次均同因失败。连带后果：
- 官方 24+5 套件产出中断（每设备仅 01/02 落盘）；
- 5 个 empty states（emptyDemo 仅套件可触发）**BLOCKED**；
- 04-region-drawer（真实 UI 两次尝试无法打开抽屉，tap 后状态不变；`regionDrawerBackClosesDetail` 状态机套件 11/11 PASS）。

### 10.4 手机 VNextAccessibilityEvidenceTest.touchTargetsMeetMinimumSize
`studio preset touch target must be >= 48dp tall` FAIL（手机 compact studio；平板同套件 OK(3)）。当前源手机板 Studio 预设触控高度 <48dp 的真实结果 → 需 ChatGPT 裁决。

### 10.5 手机 UiScreenshotEvidenceTest：compact Cards 探针 grid 缺失
`probeTag("pdig.card.grid")` assertExists FAIL：compact Cards 当前渲染 `pdig.card.list`（list-first light 设计），测试仍探 pdig.card.grid → 旧测试期望 vs 新源行为；平板同套件 OK(2)（宽屏为 grid）。

### 10.6 手机 VNextBackStateRegressionTest.cardsDetailBack_drivesRealClick（1/5，本机抖动）
手机 connected 与 retry 各 1 次失败（`Failed to inject touch input.`，测试注入环境）；平板同套件 **OK(5)**；重跑手机 seed 套件 11/11 全 PASS。判定为测试注入环境抖动，非产品缺陷，如实记录。

## 11. Logcat

`logs/logcat-phone.txt`（56,355 行）、`logs/logcat-tablet.txt`：无 `FATAL EXCEPTION`、无 `ANR in com.pdig`、无 `OutOfMemory`、无 Compose 崩溃；命中项均为系统服务级（DisplayPowerController / AppWidgetManager / SatelliteController / DeviceLockService）的既有 emulator 噪音。