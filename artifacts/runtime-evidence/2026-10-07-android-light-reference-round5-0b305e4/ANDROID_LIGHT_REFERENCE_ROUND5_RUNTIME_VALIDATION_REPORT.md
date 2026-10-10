# ANDROID_LIGHT_REFERENCE_ROUND5_RUNTIME_VALIDATION_REPORT

> 2026-10-07 · `feat/android-ui-vnext-translation` · exact-head runtime acceptance + human pixel evidence closure
> 依据：用户 Round 5 指令 + `docs/ui-vnext/android/ANDROID_LIGHT_REFERENCE_ROUND5_FINAL_ACCEPTANCE_PLAN_2026-10-07.md`
> 角色：本地执行/验证 Agent（未修改任何 production source / UI / test / docs / canonical / .depmap）

## 0. 结论状态

```
ANDROID_BUILD = PASS
ANDROID_UNIT = PASS
ANDROID_RUNTIME_EVIDENCE_FOR_CURRENT_HEAD = PARTIAL
  (official SourceCompleteScreenshotEvidenceTest COMPLETE on Phone and Tablet: 24+24 main + 5+5 empty;
   globe texture_ready x10; 04=region-detail; 22=typed search; 17/18/19 distinct;
   tablet 07/08 via real Cards->asset->inspector 定制卡面->Studio; tablet 11 via Numbers inspector;
   APK provenance closed loop PASS on both devices; persistence restart PASS;
   1 mandatory layout gate FAIL on Tablet — see FAIL-1)
ANDROID_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_ACCEPTANCE
ANDROID_REFERENCE_FREEZE = HOLD
IOS_UI_VNEXT = HOLD
HARMONY_UI_VNEXT = HOLD
```

## 1. HEAD 与来源

| 项 | 值 |
| --- | --- |
| BRANCH | `feat/android-ui-vnext-translation` |
| RUN_SOURCE_HEAD | `0b305e48bf4868b1aca2bd39dac720e19f3df7f8` |
| SOURCE_TREE | `1ff386a45789b1322da3b2af949c12093c2665ce` |
| FINAL_PIXEL_SOURCE | `a3b3a05580c0cd2ed451e554ba7db3edfd523cc0`（本轮运行起点之前最后的 pixel-changing 生产 checkpoint） |
| ACCEPTANCE_JOURNEY_CHECKPOINT | `b69034d214d6070ea74edd6ed4690bc3061ba873` |
| 同步 | `git fetch origin`（da86d06..0b305e4）→ `git checkout` + `git pull --ff-only origin feat/android-ui-vnext-translation`，纯 fast-forward；`HEAD == origin/feat/android-ui-vnext-translation` |
| 参考资产 | `spec/ui-vnext/references/android/PDIG_ANDROID_LIGHT_VISUAL_REFERENCE_2026-10-05.jpg` SHA256 `3782204282dc81342d8b0311061f9d15c720b7bcd064fd66af78479f6168dbb7`（与 REFERENCE_MANIFEST.json 一致） |
| Evidence 目录 | `artifacts/runtime-evidence/2026-10-07-android-light-reference-round5-0b305e4/` |

## 2. Build / Unit（exact head）

- 命令：`android> .\gradlew.bat assembleProductionDebug assembleProductionDebugAndroidTest testProductionDebugUnitTest --offline`
- EXIT=0，BUILD SUCCESSFUL（1m32s）
- APK：`C:\Users\Kaiser\pdig-build\app\outputs\apk\production\debug\app-production-debug.apk`，44,216,739 B，SHA256 `d800c84799bc50fa4bbc44b4be7fcc624bdaa11d5725003a94cc6574e3704482`
- AndroidTest APK：`app-production-debug-androidTest.apk`，1,304,898 B
- `BuildConfig.GIT_SHA = "0b305e4"`（生成的 BuildConfig.java 已核验）
- Unit：`testProductionDebugUnitTest` 4 类 27 测试，0 failure / 0 error（CsvMappingHelpers 7 · HomePlanCounts 5 · UiLabelMappings 6 · FileWorkflowState 9）
- 注：stderr 有一条 Kotlin incremental cache 跨根告警（历史构建目录），Gradle 实际完成全量编译并 BUILD SUCCESSFUL。

## 3. APK provenance（closed loop，双设备）

| 设备 | built SHA256 | `pm path` | pulled base.apk SHA256 | 结论 |
| --- | --- | --- | --- | --- |
| Phone (`main`) | d800c847… | `package:/data/app/~~UnUvhaJ5Q5vWE6MDcFiE5w==/com.pdig.app-K7plwbAKupD86SOcmi1hMg==/base.apk` | d800c847… | **MATCH** |
| Tablet (`pdig_tablet_api36`) | d800c847… | `package:/data/app/~~MTrt1PE8x94sd3TXTI2vOA==/com.pdig.app-8js0YOe2gHlSQo05ZgHAVA==/base.apk` | d800c847… | **MATCH** |

`BUILT_APK_SHA256 == INSTALLED_APK_SHA256` 双设备成立；本轮未复用 Round3/4 的任何 APK 哈希。

## 4. 设备矩阵

| AVD | class | model | API | px | dp | density | orientation |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `main` | Phone | Google Pixel 7 profile（sdk_gphone64_x86_64） | 36 | 1080×2400 | 411×914 | 420 | portrait |
| `pdig_tablet_api36` | Tablet | Google Pixel Tablet profile（sdk_gphone64_x86_64） | 36 | 2560×1600 | 1280×800 | 320 | landscape |

均为真实 API36 AVD runtime；无 forcedViewport / preview / mock 替代。

## 5. 官方截图套件（SourceCompleteScreenshotEvidenceTest，双设备独立 fresh 运行）

- Phone：**OK (1 test)**，29 帧（24 主屏 + 5 空态）；Tablet：**OK (1 test)**，29 帧
- raw manifest（`manifests/phone-raw-manifest.json` / `tablet-raw-manifest.json`）逐帧记录 `expectedState/actualState/stateValidation/renderReadiness/globeTextureState/fileSha256/captureTimestamp/sourceHead`；`sourceHead=0b305e4`，共 58 帧

关键帧事实（全部来自 runtime/test 产生，非手填）：

| 帧 | Phone | Tablet |
| --- | --- | --- |
| 01-now | globeTextureState=texture_ready | texture_ready |
| 02-overview-global | texture_ready / global | texture_ready / global |
| 03-overview-region-selected | texture_ready / region-selected（expected==actual） | texture_ready / region-selected |
| 04-region-drawer | texture_ready / region-detail（expected==actual，drawer 节点断言） | texture_ready / region-detail |
| phone/tablet-no-attention | texture_ready（Globe-gated，Round4 缺陷已闭合） | texture_ready |
| 07/08 tablet | glass / city 独立 Studio 帧（SHA 不同），经真实 Cards→asset→inspector 定制卡面→Studio 路径 | — |
| 11 tablet | travel 独立帧，经 Numbers inspector 定制号码面→Studio | — |
| 22-search-query | expected==actual=search-query（Compose performTextInput 实际输入 招商） | 同 |
| 17/18/19 | 三个不同 SHA（phone: 451af581…/407c3af6…/1ca1b01a…；tablet 独立），Current/Transition/After 语义 distinct | 同 |

## 6. 关键视觉门禁

- **GLOBE TEXTURE_READY**：01-04 ×2 设备 + no-attention ×2 = 10/10 帧 `texture_ready`（官方套件 Globe 30s 死线 + 450ms 稳定窗口 + 断言，非 manual wait）
- **GLOBE CIRCULAR SILHOUETTE**：`TextureEarthBodySilhouetteContractTest` **OK(1)**（Phone+Tablet 双跑）——capped-texture 条件下中心/四向 cardinals 纹理 + 外部对角线透明（圆形 disc 回归契约）
- **REGION REAL UI**：双设备真实 clickable `pdig.region.CN` 行：第一次点按 → REGION_SELECTED（CN，计数过滤）；第二次点按 → REGION_DETAIL（drawer：查看卡片/查看号码/返回全球视图）；System Back → REGION_SELECTED 且 CN 保留；显式「返回全球视图」→ GLOBAL（regionFilter=null）。截图：`logs/{phone,tablet}-region-{selected,detail,back,global}.png`
- **TABLET CARD STUDIO REAL UI**：官方 07/08 测试体本身断言 Cards→`pdig.card.expanded.card-cn-2`（clickable asset）→inspector `pdig.card.inspector.customize`（定制卡面）→Studio preview 存在；另真实 UI 复验 inspector 同时含「定制卡面」「查看完整详情」（`logs/tablet-card-inspector.png`）
- **TABLET NUMBER STUDIO REAL UI**：官方 11 测试体断言 Numbers→`pdig.phone.inspector.customize`（定制号码面）→Studio preview（travel）；真实 UI 复验 inspector（`logs/tablet-number-inspector.png`）
- **CHANGE 3 STATES**：官方三帧 distinct + 语义横幅；真实 UI 文案「完成后（计划）」「只有实际验证完成的步骤才标记为完成」（`logs/tablet-change-current.png`）——After=Plan Projection，未完成项不被显示为已完成
- **SEARCH TYPED**：官方 22 帧实际输入「招商」（EditableText 变更 + 结果出现断言）；真实 UI：Phone+Tablet 输入 `HSBC` → 搜索结果（汇丰卓越理財 / HSBC 网银）→ 点结果 → 目标屏 → System Back → Search（`logs/*-search-{real,result,back}.png`）

## 7. Persistence（fresh exact-head，真实进程级 force-stop→relaunch）

| 项 | 结果 | 证据 |
| --- | --- | --- |
| Card PresentationProfile | **PASS**：UI Cards→详情→定制卡面→Studio→极简→保存 → XML `card::card-cn-2 themeId=minimal` → force-stop → relaunch → 详情「当前主题：极简」+ XML 不变（Phone+Tablet） | `logs/*-card-prefs-saved.xml` |
| Number PresentationProfile | **PASS**：UI Numbers→详情→定制号码面→Studio→极简→保存 → XML `phoneNumber::num-cn-1 themeId=minimal` → force-stop → relaunch → XML 不变（Phone+Tablet） | `logs/*-number-prefs-saved.xml` |
| Workspace preference | **PASS**：设置→「隐藏敏感信息」toggle → `privacy_mask=false` 落盘 → force-stop → relaunch → XML 不变 + UI「敏感字段当前可见」（Phone+Tablet） | `logs/*-workspace-{mid,after}.xml` |
| 事实边界 | PresentationProfile 只写入 `pdig_ui_vnext_presentation.xml` / `pdig_ui_vnext_workspace.xml`；`files/pdig.db` 全程未触碰（SHA 记录 `logs/phone-pdig-db-sha1-pre.txt`）；无 `.depmap` / Canonical / PersonalReality 变更路径被触发 | — |

## 8. Instrumentation 契约（fresh，standalone，双设备）

| 套件 | Phone | Tablet |
| --- | --- | --- |
| SourceCompleteInteractionContractTest | OK(12) | OK(12) |
| VNextBackStateRegressionTest | OK(5) | OK(5) |
| AndroidLightAccessibilityPaletteContractTest | OK(1) | OK(1) |
| TextureEarthBodySilhouetteContractTest | OK(1) | OK(1) |
| AndroidGlobeEvidenceContractTest | OK(1) | OK(1) |
| VNextAccessibilityEvidenceTest | OK(3) | OK(3) |
| NumberDetailLayoutProbeTest | OK(1) | OK(1) |
| NumberDetailVerticalFlowContractTest | OK(1) | OK(1) |
| AndroidAdaptiveShellContractTest | OK(7) | OK(7) |
| TabletAdaptiveContractTest | — | **FAILURES (1/4)** → FAIL-1 |
| PhoneCardsLayoutContractTest | OK(1) | OK(1) |
| PhoneNumbersListVisibilityContractTest | OK(1) | OK(1) |
| PhoneStudioLayoutContractTest | OK(1) | OK(1) |
| PhoneChangeLayoutContractTest | OK(1) | OK(1) |
| AndroidCardIdentityContractTest | OK(2) | OK(2) |
| AndroidLightVisualSourceContractTest | OK(8) | OK(8) |
| AndroidVisualVariantEvidenceContractTest | OK(5) | OK(5) |
| StudioThumbnailDistinctTest | OK(1) | OK(1) |
| WorkspacePreferenceDiskPersistenceTest | OK(1) | OK(1) |
| AndroidVNextTranslationEvidenceTest | OK(1) | OK(1) |
| AndroidUiVNextHumanFixEvidenceTest | OK(1) | OK(1) |
| UiScreenshotEvidenceTest | OK(2) | OK(2) |

### FAIL-1（mandatory gate，如实记录，未修复）

- **FAIL ID**：R5-F1
- **exact test**：`TabletAdaptiveContractTest.tabletNumberDetail_noDeadSpace`（`TabletAdaptiveContractTest.kt:107`）
- **exact screen**：Tablet Number Detail（expanded，2560×1600）
- **exact error**：`java.lang.AssertionError: tablet number detail hero→services gap must be <= 48dp (gap=212.0)`
- **expected**：hero→services gap ≤ 48dp（无死区）
- **actual**：gap=212.0dp
- **log path**：`logs/tablet-TabletAdaptiveContractTest.log`
- **影响**：Tablet Number Detail 存在明显纵向死区，违反「无 dead content」布局契约 → 本轮 `ANDROID_RUNTIME_EVIDENCE_FOR_CURRENT_HEAD=PARTIAL`
- **处置**：Agent 只报告，交由 ChatGPT 决定 source remediation（不修 production UI / 不改测试）

## 9. Accessibility / 布局 / 拷贝审计

- Touch targets：`VNextAccessibilityEvidenceTest`（bounds 校验）Bottom Nav `NavigationBarItem` ≥48dp、Studio 预设 ≥48dp、Region reset ≥48dp —— 双设备 OK(3)
- Palette contrast：`AndroidLightAccessibilityPaletteContractTest` —— textPrimary/textSecondary/textMuted/positive/warning/critical/unknown × surface/canvas/raised/soft 全部 ≥4.5:1，白字 on PrimaryBright ≥4.5:1 —— 双设备 OK(1)
- Number Detail probe：Phone/Tablet JSON 均为正间距（hero→services 106dp、16dp 组距）；Tablet 106dp gap 与 FAIL-1 量级一致（212dp 为该契约不同窗口测量）
- Consumer copy：对 Tablet Now/Overview/Cards/Numbers/Records/Change 运行时 dump 扫描 `PresentationProfile|PersonalReality|Canonical|make-before-break|internal state|fixture|source contract|PLAN PROJECTION|depmap` —— **0 命中**；After 使用「完成后（计划）」消费者措辞

## 10. Desktop Freeze Guard / Core / logcat

- **DESKTOP_FREEZE_GUARD = PASS（12/12 SHA256 verified，exit 0）**（`logs/desktop-freeze-guard.log`）
- Core gates：lint EXIT=0 · typecheck EXIT=0 · test 45 files/487 tests PASS · architecture PASS（0 cycles）· network PASS（0 primitives）· secrets PASS（0 secrets）· ui PASS；`format:docs:check` 为 ChatGPT 侧 9 个 docs 文件既有 Prettier 漂移（含 Round5 计划文档本身），与 Round4 结论一致，Agent 未修改 docs
- logcat：Phone 68,900 行 / Tablet 58,225 行 —— 无 `FATAL EXCEPTION`（任何 package）、无 `ANR in com.pdig`、无 `OutOfMemory`、无 Compose crash；仅 WindowManager 对 force-stop 窗口的常规 EXITING 告警（非崩溃）

## 11. Human Review Bridge（分片 contact sheets）

`contact-sheets/`（每张 = 原始 runtime PNG 等比缩放拼接 + 屏名标签，不裁剪）：

| sheet | pngBytes | b64Chars | pngSha256 |
| --- | --- | --- | --- |
| phone-01-04.png | 535,272 | 713,696 | b043b8fc… |
| phone-05-08.png | 537,319 | 716,428 | 416142cc… |
| phone-09-12.png | 551,064 | 734,752 | 6009784b… |
| phone-13-16.png | 522,159 | 696,212 | a01b0ed1… |
| phone-17-20.png | 475,851 | 634,468 | 01840eb0… |
| phone-21-24.png | 363,505 | 484,676 | 3bcf6baf… |
| tablet-01-04.png | 312,780 | 417,040 | ecb962fa… |
| tablet-05-08.png | 314,547 | 419,396 | 3dcd6663… |
| tablet-09-12.png | 254,253 | 339,004 | 1e91b81a… |
| tablet-13-16.png | 192,757 | 257,012 | b5718b8c… |
| tablet-17-20.png | 207,410 | 276,548 | 0f1b6802… |
| tablet-21-24.png | 136,985 | 182,648 | f027d4be… |
| empty-10.png | 568,652 | 758,204 | f845cb30… |

**所有 13 个 `.b64.txt` < 900,000 字符**（max 758,204），每张附 `.png.b64.txt` + `HUMAN_REVIEW_BRIDGE_MANIFEST.json`；原始 individual PNG 保留于 `phone/`、`tablet/`、`empty-states/`。无 mock / 旧截图 / preview / 重绘。

## 12. 生产源改动审计 / 交付

- 本轮开始前 `git status --short` baseline：仅用户既有 untracked `artifacts/runtime-evidence/2026-09-29-ui-vnext/IMAGE_METRICS.json`
- 本轮 tracked mutation：仅 `artifacts/runtime-evidence/2026-10-07-android-light-reference-round5-0b305e4/**`（evidence）
- `android/app/src/main/**`、`android/app/src/test/**`、`android/app/src/androidTest/**`、`docs/**`、`spec/**`、`core/**`、`canonical/**`、`fixtures/**` 改动 = 0
- Evidence 交付：`phone/`（24 PNG）· `tablet/`（24 PNG）· `empty-states/`（10 PNG）· `contact-sheets/`（13 PNG + 13 b64 + manifest）· `manifests/`（SOURCE_PROVENANCE / DEVICE_MATRIX / SCREENSHOT_MANIFEST(58帧) / INTERACTION_VALIDATION / raw manifests）· `probes/` · `logs/` · `apk-provenance/`（phone/tablet JSON + pulled base.apk）· `EVIDENCE_SHA256SUMS.txt`（185 条）· 本报告

## 13. 最终状态

```
ANDROID_BUILD = PASS
ANDROID_UNIT = PASS
ANDROID_RUNTIME_EVIDENCE_FOR_CURRENT_HEAD = PARTIAL   # FAIL-1: Tablet number detail dead-space gap (212dp > 48dp)
ANDROID_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_ACCEPTANCE
ANDROID_REFERENCE_FREEZE = HOLD
IOS_UI_VNEXT = HOLD
HARMONY_UI_VNEXT = HOLD
```

未写 `ANDROID_VISUAL_REFERENCE=ACCEPTED` / `ANDROID_REFERENCE_FREEZE=PASS`。像素级 Human Final Acceptance 由 ChatGPT 依据上述真实 runtime 像素（分片 contact sheets + 原始 PNG）裁决。
