# ANDROID_LIGHT_REFERENCE_ROUND3_RUNTIME_VALIDATION_REPORT

> 2026-10-06 · `feat/android-ui-vnext-translation` · exact-head build/test/runtime/evidence recapture
> 依据：`docs/ui-vnext/android/ANDROID_LIGHT_REFERENCE_HUMAN_PIXEL_REVIEW_ROUND2_2026-10-06.md` §7 Required next pack
> 角色：本地执行/验证 Agent（未修改任何 production UI/source；源码与测试修复全部由 ChatGPT 在 Round2 直接完成）

## 0. 结论状态（Round2 §8 口径）

```
HUMAN_VISUAL_DIRECTION_REFERENCE = SELECTED
ANDROID_UI_VNEXT_SOURCE_DESIGN = COMPLETE
ANDROID_SOURCE_REMEDIATION_AFTER_PIXEL_REVIEW = COMPLETE

ANDROID_RUNTIME_EVIDENCE_FOR_CURRENT_HEAD = READY (exact head af25f50 recaptured; 48 screens real)
ANDROID_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_ACCEPTANCE
ANDROID_REFERENCE_FREEZE = HOLD
EMPTY_STATES_10 = BLOCKED (official suite still interrupted at 03-globe TEXTURE_READY gate; see §6.1)
IOS_UI_VNEXT = HOLD
HARMONY_UI_VNEXT = HOLD
```

## 1. HEAD 与来源

| 项 | 值 |
| --- | --- |
| BRANCH | `feat/android-ui-vnext-translation` |
| RUN_SOURCE_HEAD | `af25f50b43e50fc35444a6139ee915e4a83ded30` |
| SOURCE_TREE | `3993cffdd2fe7894c9920e0c6393020cad510c41` |
| 同步 | `git fetch origin` → `git pull --ff-only`（6cb4886..af25f50，纯 fast-forward） |
| Round2 人类审阅裁定 | `RUNTIME PACK REJECTED FOR FREEZE / SOURCE REMEDIATION APPLIED / FRESH EXACT-HEAD EVIDENCE REQUIRED` |
| Reference SHA | 仓库 manifest/.md 已修正为实际内容哈希 `3782204…`（Round2 已修复 provenance） |
| Evidence 目录 | `artifacts/runtime-evidence/2026-10-06-android-light-reference-round3-af25f50/` |

## 2. 构建与核心门禁

| 项 | 结果 | 证据 |
| --- | --- | --- |
| CORE_CHECK | FAIL 仅 `format:docs:check`（7 个 ChatGPT 维护 md 的 prettier 漂移，WORK_STATUS 既有先例）；lint/typecheck/test/architecture/network/secrets/ui 全 EXIT=0 | `logs/core-check.log`、`logs/core-*.log` |
| DESKTOP_FREEZE_GUARD | PASS（12/12） | `logs/desktop-freeze-guard.log` |
| ANDROID_BUILD | PASS（`assembleProductionDebug`，APK SHA `584cfb36…`，44,117,283 B） | `logs/phone-instrumentation.log` |
| ANDROID_UNIT | PASS（`testProductionDebugUnitTest`） | 同上 |

## 3. Instrumentation（fresh，exact head）

### 3.1 Phone `connectedProductionDebugAndroidTest`（官方全量）
119 tests / **4 failures** / 4 skipped：

| 失败 | 原因（当前 HEAD 真实结果，不修源） |
| --- | --- |
| `AndroidAdaptiveShellContractTest.wideCards_clickUpdatesInspector…` | wide（forced viewport）场景下断言组件未显示（line 103） |
| `AndroidAdaptiveShellContractTest.wideNumbers_clickUpdatesInspector…` | 注入 touch 失败：找不到 `工作副号` 节点（line 113） |
| `SourceCompleteScreenshotEvidenceTest.capturesSourceCompleteHumanSet` | **03-overview-region-selected 在 30s×4 次重试内 renderState 仍 LOADING**（§6.1） |
| `VNextAccessibilityEvidenceTest.touchTargetsMeetMinimumSize` | 手机 bottom nav 触控目标 <48dp 高（修复后测量点变为 bottom nav；line 74） |

修复生效确认：`NumberDetailVerticalFlowContractTest`（Hero→Summary→Services 邻接）**PASS**；`AndroidLightVisualSourceContractTest`/`AndroidAdaptiveShellContractTest` 已编译并运行（assertions imports 修复生效）。

### 3.2 Tablet 套件 batch（19 项 fresh）
**16 OK**：probe OK(1)、interaction OK(11)、a11y OK(3)、verticalflow OK(1)、accessibility OK(14)、cardidentity OK(2)、globe OK(1)、visualvariant OK(5)、translation OK(1)、humanfix OK(1)、phonelist OK(1)、phonestudio OK(1)、phonecards OK(1)、phonechange OK(1)、thumbnails OK(1)、**adaptiveshell OK(7)**（宽屏 adaptive 契约全过）
**3 FAIL**（如实）：`backstate.cardsDetailBack_drivesRealClick` 5/1（`Failed to inject touch input.` 注入抖动；phone connected 同项 PASS）、`uishots` 2/1（probe `pdig.card.list` 找不到——expanded 用 grid，测试期望为旧 compact 语义）、`lightvisual.detailAndTruthSurfaces…` 8/1（某 detail surface 未显示，line 122）。

## 4. 截图（48 张真实 runtime）

- **Phone 24**：`phone/01-now.png … 24-data-sources.png`（01/02 = 官方套件 captureToImage；03–24 = 真实 UI 导航 + adb screencap，Globe 帧 20–30s 纹理等待）
- **Tablet 24**：`tablet/01-now.png … 24-data-sources.png`（01/02 官方；03–24 真实 UI；真实横屏窗口 2560×1600 @320）
- 每张条目：`manifests/ANDROID_UI_VNEXT_SCREENSHOT_MANIFEST.json`（screenId/device/avd/api/viewport/density/orientation/sourceHead/sourceTree/apkSha256/reference/referenceSha256/expected/actual/stateValidation/renderReadiness/globeTextureState/timestamp/fileSha256）

§7 逐项对照：

| §7 要求 | 状态 |
| --- | --- |
| Phone 24 | **READY**（24/24 真实；17/18/19 三投影 distinct：313142/327559/347052 B） |
| Tablet 24 | **READY**（24/24 真实；09 numbers 列表 / 10 detail inspector / 11 studio（查看完整详情→定制号码面→旅行）独立帧：464695/465400/418328 B） |
| 10 empty states | **BLOCKED**（官方套件 03 屏 Globe 门槛；emptyDemo 仅套件可达；如实记录，见 §6.1） |
| 4 Globe 帧 `globeTextureState=texture_ready` | **BLOCKED**（官方 manifest 因套件中断未写出；01/02 为官方 capture，03/04 手动 + 像素验证纹理真实渲染；manifest 如实标注 `official-capture/manual-wait+pixel-verified`，不冒充枚举） |
| Region Detail actual=`region-detail` | **BLOCKED（real-UI）**：adb tap 无法打开抽屉（状态机套件 `regionDrawerBackClosesDetail` 11/11 PASS 证明语义正确）；官方路径被 03 阻断。04 帧如实标注 actual=`region-selected` |
| Search Query actual=`search-query` | **PARTIAL**：官方 typed-query 帧被 03 阻断；22 帧 = 真实搜索 catalog 结果导航（Search→result 流程真实）；`searchQueryShowsCardResult`/`searchResultNavigationAndBackToSearch` 套件 PASS |
| Change 三投影 distinct | **PASS**（双设备 17/18/19 字节互异；18 语义横幅「第 3/6 步·验证新号码·待验证」；19「完成后（计划）」） |
| Tablet Numbers/NumberDetail/NumberStudio 独立帧 | **PASS**（09/10/11 独立真实帧） |
| Card/Number Profile 进程重启持久化 | **PASS（phone）**：card 极简 UI 保存→force-stop→重启→「当前主题：极简」True；number travel XML 跨重启（phone+tablet）。Tablet 卡片 Studio 入口在 expanded inspector 中不可见（07/08 tablet 帧 = inspector 帧，blocker 如实记录；phone 07/08 完整） |
| Workspace 偏好持久化 | **PASS**（phone+tablet：设置→即将到来→XML show_upcoming=true→force-stop→重启→仍 true） |
| Search→result→Back | **PASS**（套件 + tablet 真实 UI back→now） |
| Region select→Detail→Back preserve→Global reset | **PASS（状态机 11/11）** + tablet 真实 UI：中国→Numbers banner→查看全球 reset |
| Number Detail flow probe | **PASS**（`probes/NUMBER_DETAIL_LAYOUT_PROBE_phone.json` / `_tablet.json`：hero→services 106dp，其余 16dp，无 0×0/负间隙/反转；`NumberDetailLayoutProbeTest` OK 双设备） |
| Accessibility | **PARTIAL**：tablet AccessibilitySemanticsTest OK(14)+VNextAccessibilityEvidenceTest OK(3)；phone a11y touch-target FAIL（bottom nav <48dp，真实测量）；`AndroidCardIdentityContractTest`/`AndroidGlobeEvidenceContractTest` OK |
| System Back | **PASS**（phone 真实 UI card/number detail Back；backstate 套件 phone OK；tablet 1 次注入抖动如实记录） |
| Desktop Freeze Guard | **PASS** |
| Evidence hashes/provenance | **READY**（`EVIDENCE_SHA256SUMS.txt` 70 条 + manifests + SOURCE_PROVENANCE） |
| production-source mutation = 0 | **PASS**（见 §7） |

## 5. 空态说明

`empty-states/` 无官方文件。原因：10 个空态仅由 `SourceCompleteScreenshotEvidenceTest`（emptyDemo）渲染；该套件在本机/HEAD 于 03-overview-region-selected 处 Globe `renderState=LOADING` 中断（30s×4 次重试仍 LOADING；手机 connected + 单独 am-instrument + 平板共 6 次复现）。Round2 已修复渲染 cap（768→512）与 idle-yaw，但 REGION_SELECTED 状态在 compose-test harness 下仍无法在窗口内达到 TEXTURE_READY。这是**当前源/测试门禁的未决 blocker**，需 ChatGPT 进一步裁决（修复 globe 状态机或放宽套件窗口），非本 Agent 可修（禁止修改 production/source/test）。

## 6. 其他如实失败

1. `AndroidAdaptiveShellContractTest` wideCards/wideNumbers（forced-viewport 契约，手机 connected 场景）——源测试问题。
2. `VNextAccessibilityEvidenceTest` phone bottom-nav touch target <48dp——真实布局测量。
3. Tablet `uishots` probe `pdig.card.list` 缺失（expanded=grid vs 测试期望 list）——测试期望 vs 新 adaptive 源。
4. Tablet `lightvisual.detailAndTruthSurfaces…` 组件未显示——源测试场景。
5. Tablet `backstate.cardsDetailBack_drivesRealClick` 注入抖动（phone 同项 PASS）。

## 7. 生产源改动审计

- `git diff --name-only`（tracked）= **空**
- `android/app/src/main/**` 改动 = **0**
- evidence commit 仅含 `artifacts/runtime-evidence/2026-10-06-android-light-reference-round3-af25f50/**`
- 用户既有未跟踪文件 `artifacts/runtime-evidence/2026-09-29-ui-vnext/IMAGE_METRICS.json` 未触碰

## 8. 交付物

- `phone/`、`tablet/`（24+24 PNG）；`manifests/`（SOURCE_PROVENANCE / DEVICE_MATRIX / SCREENSHOT_MANIFEST / INTERACTION_VALIDATION / phone-raw-manifest / tablet-raw-manifest）；`probes/`；`logs/`（build/core/freeze-guard/套件/seed/prefs/logcat）；`EVIDENCE_SHA256SUMS.txt`；本报告
- logcat：无 FATAL EXCEPTION / ANR in com.pdig / OutOfMemory / Compose 崩溃

## 9. 最终状态

`ANDROID_RUNTIME_EVIDENCE_FOR_CURRENT_HEAD = READY`；`ANDROID_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_ACCEPTANCE`；`ANDROID_REFERENCE_FREEZE = HOLD`；iOS/Harmony = HOLD。48 屏已可交付 Human 逐屏 pixel review；10 空态与 Globe 官方纹理门槛为未决 blocker，需 ChatGPT 裁决后重跑。