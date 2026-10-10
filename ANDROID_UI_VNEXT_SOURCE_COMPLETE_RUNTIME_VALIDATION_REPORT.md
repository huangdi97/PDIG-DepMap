# ANDROID_UI_VNEXT_SOURCE_COMPLETE_RUNTIME_VALIDATION_REPORT

> **CURRENT EVIDENCE SCOPE NOTE (2026-10-05): HISTORICAL FOR FREEZE**
>
> This report remains an accurate record of the run captured from
> `SOURCE_HEAD = 4e43511ae9754413ffedeaca9ad21a71b330aa64`. It is **not** current Freeze evidence for the
> branch after Human Review source corrections. The stale accessibility expectations and the Phone
> forced-wide geometry artifact described in §12 have been corrected in later source/tests, and the Human
> Review also tightened Region Detail behavior, Wide Overview layout, compact Studio density, Change Phone
> consumer copy, and Globe screenshot readiness. Current Android source checkpoint:
> `c0e5f34e721f22b7765aaafc2a2d374a65e5191e`.
>
> Required interpretation:
> `ANDROID_RUNTIME_EVIDENCE_FOR_CURRENT_HEAD = REQUIRED`,
> `ANDROID_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_ACCEPTANCE`,
> `ANDROID_REFERENCE_FREEZE = HOLD`. Do not rewrite the results below as if they were rerun on the new head.
>

> 2026-10-04 · `feat/android-ui-vnext-translation` · SOURCE-COMPLETE 运行时验证与证据收口
> 角色：本地执行/验证 Agent（BUILD / RUN / TEST / CAPTURE / EVIDENCE ONLY — 未设计/未改 UI）

## 0. 结论状态（任务书 §42 口径）

```
ANDROID_UI_VNEXT_SOURCE_DESIGN = COMPLETE          (ChatGPT source-design 完成，未由本 Agent 变更语义)
ANDROID_UI_VNEXT_RUNTIME_VALIDATION = BLOCKED      (见 §12：2 个既有 accessibility 契约测试与新 consumer copy 漂移)
ANDROID_RUNTIME_EVIDENCE = READY                    (证据完整：48 屏 + 10 空态 + probes + manifests)
ANDROID_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_ACCEPTANCE
ANDROID_REFERENCE_FREEZE = HOLD
IOS_UI_VNEXT = HOLD
HARMONY_UI_VNEXT = HOLD
```

> 说明：本 Agent 不写 ACCEPTED / CRAFT PASS / FREEZE PASS。运行验证主体全部 PASS，
> 但 2 个**既有** accessibility 契约测试（VNextAccessibilityEvidenceTest 的 2 条）在最新 source 上 FAIL
> （期望字符串与新的 consumer copy 不符，见 §12.1）。该 FAIL 需要 ChatGPT 在 source 侧或测试侧收口，
> 因此在人工逐屏验收前，`RUNTIME_VALIDATION` 记为 **BLOCKED**（证据已 READY，等待 source 侧裁决）。

## 1. SOURCE_HEAD

- `SOURCE_HEAD = 4e43511ae9754413ffedeaca9ad21a71b330aa64`（remote `feat/android-ui-vnext-translation`，与任务书预期一致）
- 同步方式：`git fetch origin` 后 `git reset --hard origin/feat/android-ui-vnext-translation`（本地存在 3 个从未推送、已被 remote 新行取代的旧 commit，已先以 patch 形式备份至会话 scratch；按用户指示「继续」完成同步）。
- 运行期间工作区相对 HEAD 的改动：2 个纯机械 import 修复（见 §13）+ 5 个 docs 的 prettier 格式修复 + 6 个 androidTest 证据文件（全部为本轮新增/修改，非 UI 设计）。

## 2. Build 结果

| 项 | 值 |
| --- | --- |
| BUILD_STATUS | **PASS**（`assembleProductionDebug` BUILD SUCCESSFUL，53s） |
| 变体 | production/debug（仓库真实 flavor；AGP 8.5.2 / Kotlin 2.0.0 / Compose BOM 2024.09.02） |
| APK_PATH | `C:\Users\Kaiser\pdig-build\app\outputs\apk\production\debug\app-production-debug.apk`（仓库将 build 输出重定向到 ASCII 根，见 `android/settings.gradle.kts`） |
| APK_SHA256 | `76df2dda5791886736a7ff88c8ad05a9cb5ae586e9d812763a3b54274851fded`（41.77 MB） |
| BUILD_DURATION | 53s（clean 后全量） |
| 首次编译阻塞 | 首轮 `compileProductionDebugKotlin` 失败：2 个新文件缺 import + 1 个无效 import（详见 §13），属 source 侧零语义机械问题，已按任务书 §2 例外修复并报告。 |

## 3. Toolchain（DEVICE_MATRIX.json 同步记录）

- Host OS：Windows 11（NT 10.0.26200）
- JDK：Temurin 21.0.12.1 LTS（JAVA_HOME = `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`）
- Gradle：8.9（`android/gradlew.bat` wrapper）
- AGP：8.5.2；Kotlin：2.0.0；Compose BOM：2024.09.02
- Android SDK：`D:\Code\Android\SDK`（platforms 34/35/36/36.1/37.0；system-images android-36 google_apis x86_64）
- adb：37.0.0（1.0.41）；emulator：36.5.11.0（build_id 15261927）
- node：v22.15.0

## 4. AVD（DEVICE_MATRIX.json）

| 设备 | AVD | API | 分辨率 | density | 方向 |
| --- | --- | --- | --- | --- | --- |
| Phone | `main`（pixel_7） | 36 | 1080×2400 | 420dpi（2.625） | portrait |
| Tablet | `pdig_tablet_api36`（pixel_tablet） | 36 | 2560×1600 | 320dpi（2.0） | landscape（运行时旋转 user_rotation=1） |

均为既有 AVD（未新建、未下载镜像）；冷启动（-no-snapshot）；未 wipe data。

## 5. Core check（`npm run check`，core/）

- **PASS**（102s）：format:check ✓ / format:docs:check ✓（首跑失败 → 对 5 个 docs 执行仓库自带 `npm run format:docs` 后绿）/ lint ✓ / typecheck ✓ / **vitest 45 files · 487 tests PASS** / architecture cycles=0（55 files）/ network gate PASS（137 business files, 0 network）/ secrets PASS（2685 files, 0 secrets）/ UI static gate PASS
- 说明：`format:docs:check` 初跑失败由 remote source 新增/改动的 5 个 .md（含 ANDROID_UI_VNEXT_DESIGN_COMPLETION.md）未过 prettier 造成；已用仓库标准 fixer 修复（零语义）。
- 日志：`logs/core-check.log`

## 6. Desktop Freeze Guard

- **PASS（12/12 SHA256 verified）**：`node tools/freeze/desktop-reference-freeze-guard.mjs` exit 0。
- 无 golden 漂移；未自动更新 baseline。

## 7. Android JVM unit tests

- `gradlew test`（app + core + repos/conformance 全模块）BUILD SUCCESSFUL。
- 结果：**98 tests PASS（0 fail / 0 error / 0 skip）** —— app（productionDebug 变体）27 + core 模块 71（DomainInvariant 15 / ImpactKernel 11 / PlanReadiness 14 / MigrationSemantics 10 / GraphRevisionSemantics 7 / StateMachine 14）。
- 日志：`logs/unit-tests.log`

## 8. Android instrumentation（真实 API36 emulator runtime）

等价 task：仓库当前变体为 `connectedProductionDebugAndroidTest`（flavor production）；本次按任务书「或仓库当前等价 task」以 `am instrument` 在已安装 APK 上逐批执行。

### 8.1 本轮新增证据测试（androidTest，`com.pdig.uivnext.evidence`）

| 测试 | Phone | Tablet |
| --- | --- | --- |
| SourceCompleteScreenshotEvidenceTest（24 屏 + 5 空态，29 captures，expected==actual 断言） | PASS | PASS |
| NumberDetailLayoutProbeTest（真实 dp 探针） | PASS | PASS |
| SourceCompleteInteractionContractTest（11 项交互契约） | PASS | PASS |
| WorkspacePreferenceDiskPersistenceTest（磁盘持久化） | PASS | —（同一机制，Phone 为代表） |

### 8.2 既有契约测试（全量重跑最新 head）

- **Phone：42 tests run，3 FAIL**；**Tablet：42 tests run，2 FAIL**（详见 §12）。
- 任务书点名的契约测试全部 PASS（除 §12 列出的）：
  AndroidVisualVariantEvidenceContractTest ✓、AndroidCardIdentityContractTest ✓、AndroidGlobeEvidenceContractTest ✓（Globe TEXTURE_READY 断言通过）、PhoneCardsLayoutContractTest ✓、PhoneNumbersListVisibilityContractTest ✓、NumberDetailVerticalFlowContractTest ✓、VNextBackStateRegressionTest ✓、UiScreenshotEvidenceTest（Tablet 2/2 ✓；Phone 见 §12.3）、AndroidVNextTranslationEvidenceTest ✓（14 屏历史集合重生成）、AndroidUiVNextHumanFixEvidenceTest ✓、TabletAdaptiveContractTest ✓、PhoneChangeLayoutContractTest ✓、StudioThumbnailDistinctTest ✓、AccessibilitySemanticsTest ✓（14/14）。
- 日志：`logs/phone-instrumentation.log`（A）、`logs/phone-instrumentation-b.log`、`logs/tablet-instrumentation.log`、`logs/tablet-instrumentation-b.log`

## 9. Phone runtime（AVD main）

- 冷启动：无 crash / 无 ANR / 无 blank root / 无 compose exception（logcat-phone.txt）。
- 安装最新 APK（SHA §2）+ 测试 APK；cold app launch OK。
- 24 屏完整 Human Review 集合捕获完成（真实 emulator 内 Compose 像素 captureToImage，非 offscreen/preview/desktop render）。
- 进程重启核验：`am force-stop` → `am start --ez vnext_demo true` → **RESTART_NO_FATAL**；`restart-launch.png` 已捕获。
- 空态 5 类：state correct（expected==actual）、渲染无崩溃（EmptyState 组件带 primary/secondary CTA，见 `EmptyState.kt`）。

## 10. Tablet runtime（AVD pdig_tablet_api36）

- 冷启动：无 crash / 无 ANR；landscape 2560×1600。
- Navigation rail / Globe / expanded layout 均可进入（Overview 24 屏 + region-selected + drawer 捕获）。
- 24 屏完整集合捕获完成；进程重启核验 RESTART_NO_FATAL。

## 11. 证据清单（artifacts/runtime-evidence/2026-10-04-android-ui-vnext-source-complete-validation/）

- `phone/`：24 屏（01-now … 24-data-sources）+ restart-launch.png —— 全部与 manifest sha256 逐一匹配
- `tablet/`：24 屏 + restart-launch.png —— 同上
- `empty-states/`：phone-cards-empty / phone-numbers-empty / phone-no-attention / phone-no-active-change / phone-no-known-dependencies + tablet 对应 5 张
- `probes/NUMBER_DETAIL_LAYOUT_PROBE.json`（phone + tablet 合并；真实 dp 值）
- `manifests/ANDROID_UI_VNEXT_SOURCE_COMPLETE_SCREENSHOT_MANIFEST.json`（58 条：platform/device/avd/api/viewport/density/orientation/screen/sourceHead/apkSha256/expectedState/actualState/stateValidation/renderReadiness/sha256/fileSha256/fileShaMatch/captureTimestamp；Studio expected/actual theme+material；Globe globeRenderState；Region expected/actual region）
- `manifests/ANDROID_UI_VNEXT_INTERACTION_VALIDATION.json`（15 项，全 PASS，含 steps/evidence/sourceHead）
- `manifests/DEVICE_MATRIX.json`
- `logs/`：build.log / core-check.log / unit-tests.log / phone-instrumentation.log / phone-instrumentation-b.log / tablet-instrumentation.log / tablet-instrumentation-b.log / phone-workspace-persist.log / logcat-phone.txt / logcat-tablet.txt
- `EVIDENCE_SHA256SUMS.txt`（64 条：48 屏 + 10 空态 + 4 manifest/probe + restart-launch ×2）
- 历史证据目录（2026-09-29 / 2026-10-02 / 2026-10-03）未动。

## 12. 已知运行时差异 / 待 ChatGPT 收口

### 12.1 VNextAccessibilityEvidenceTest（phone + tablet 均 FAIL，2 条）
- `bottomNavAndSearchHaveAccessibleSemantics`：期望 contentDescription `搜索 / 命令`。
  **新 source 实际为 `搜索与快捷操作`（`VNextShell.kt:148`），tag `pdig.search.entry` + click 均在** → 无障碍能力未丢失，**测试期望串过期**。
- `globeHasAccessibleDescription_andRegionListFallbackExists`：期望文本 `地区（Region List）`。
  **新 source 实际为 consumer label `地区`（`OverviewScreen.kt:180`），内部注解「（Region List）」已被 consumer-copy 收口移除** → 测试期望串过期。
- 判定：测试与 consumer-copy 收口漂移，**非运行时无障碍回归**；需 ChatGPT 决定更新测试期望或调整文案口径。

### 12.2 （无其他新的契约回归）—— 其余 40/42（phone）、40/42（tablet）全 PASS。

### 12.3 UiScreenshotEvidenceTest（仅 Phone FAIL；Tablet 2/2 PASS）
- `capturesVNextDemoScreens_andProbesKeyTestTags`：`pdig.globe.stage must be laid out on overview` —— 手机窗口下该 test-bed 的 wide-shell stage probe 几何不可满足。**为 pre-existing 约束**（BLOCKERS 已记录：starting-HEAD 复测同样 FAIL，非本轮回归；GLOBE_STAGE tag 在新 source 中存在，`UiVNextModels.kt:167`）。

## 13. 本地 Agent 对 production 的改动（任务书 §39 报告）

production `android/app/src/main/**` 改动 = **2 个文件 · 3 处纯机械 import 修复**（任务书 §2 唯一例外，已报告，未擅自 commit 语义）：

| 文件 | 行 | 修复 | 语义影响 |
| --- | --- | --- | --- |
| `ui/components/NumberIdentitySurface.kt` | 14 | 删除无效顶层 import `androidx.compose.foundation.layout.matchParentSize`（foundation-layout 1.7.x 中为 BoxScope 成员扩展，调用点在 Box 作用域内，无需 import） | 零 |
| `ui/screens/OverviewQuickEntries.kt` | 13/16 | 补 `import androidx.compose.foundation.shape.RoundedCornerShape`、`import androidx.compose.ui.Alignment` | 零 |

若不修则 `assembleProductionDebug` 无法编译（SOURCE_COMPILE_BLOCKER）；修复后全部 gate 绿。另：5 个 docs 由仓库标准 prettier 格式化（零语义）。

## 14. 其余验证项

- **Search**：query `招商` 真实键入（instrumented TextField）→ 结果节点渲染；`searchResultNavigationAndBackToSearch` 通过（Search→结果→Back→Search）。截图 `22-search-query.png`。
- **Region context**：`selectRegion("CN")` → region-selected（`03-`）；Region Drawer（`04-`）；filter 传播到 Cards/Numbers；`clearRegion` 恢复 GLOBAL（契约测试 PASS）。consumer 文案仅显示地区名（`中国大陆/香港…`），无裸码（CN/HK/GB/US/SG 不作为 consumer label）。
- **System Back**：Cards→Detail→Studio→Back→Detail→Back→Cards；Numbers 等价；Region Drawer Back→关闭（到 GLOBAL）—— 契约测试 PASS。
- **Change Phone 三态**：current/transition/after 截图互异（17/18/19）；After = Plan Projection 渲染无崩溃（CHANGE_NEW 节点存在）；未迁移服务保持 unresolved（source 语义未动）。
- **Globe**：AndroidGlobeEvidenceContractTest PASS（TEXTURE_READY）；01/02/03/04 截图 475–856KB（纹理地球已渲染，非黑球；供人工目验）。
- **Number Detail probe**：phone 与 tablet 四个间隙均为 **16.0dp**（Column spacedBy(16dp) 设计值），顺序不变式成立，无死空白。
- **Records**：`20-records.png`（phone/tablet）连续 timeline（Active Change card + 迁移进度 + 需要关注 + 即将到来）；tablet 非手机拉伸（独立 2560×1600 布局渲染）。
- **Infrastructure 长尾**：Accounts/Emails/Devices/Services/Weaknesses 全部可进入（12–16 屏），selected tab 自动可见、返回正常；无 placeholder/route id/debug copy/internal enum 暴露（consumer copy audit 0 hits）。
- **Personalization / Data Sources**：23/24 屏捕获；Privacy mask / reduce motion / rail / upcoming 由真实 WorkspacePreferenceStore 持久化（§8.1）。
- **Consumer copy audit**：对 consumer UI 源码全量扫描 13 个工程词（vNext/fixture/debug/preset/Presentation layer/Canonical/PersonalReality/bundled/procedural/route=/freeze/reference/Impact Kernel）→ **0 hits in consumer-facing copy**（命中均为代码标识符/注释/内部存储名，不可见）。
- **Touch / Accessibility**：AccessibilitySemanticsTest 14/14 PASS；VNextAccessibilityEvidenceTest touchTargets 项 PASS；触控目标由 VTouchTarget/48dp 约束保证（既有契约）。
- **PresentationProfile**：真实 store 落盘（shared_prefs XML 含 card-cn-2 city/frosted/mask）；force-stop 后 XML 仍在（进程级持久化）；重启后新 ViewModel 恢复 + Card Detail 渲染使用（契约测试 + 截图）。

## 15. Remote CI truth

- **`REMOTE_CI = NOT_TRIGGERED`（真实查询）**：push 后以 gh 查询
  `repos/huangdi97/PDIG-DepMap/actions/runs`，head_sha=`4920949…` 无任何 workflow run；
  `actions/runs?branch=feat/android-ui-vnext-translation` 同样为空。与仓库已知事实一致
  （`ci.yml` 触发分支显式列举 main、feat/mvp03-living-graph，不含本分支）。
- 本地 PASS ≠ GitHub CI PASS（未冒充）。

## 16. Git / 证据提交

- 分支：`feat/android-ui-vnext-translation`（未新开 branch）。
- 提交内容：androidTest 证据测试 6 个文件 + 2 个 production 机械 import 修复 + 5 个 docs prettier 修复 + 全部 runtime evidence 产物 + 本报告 + WORK_STATUS/BLOCKERS/NATIVE_MIGRATION_STATUS 更新。
- push 结果：`4e43511..4920949` fast-forward 成功（`-c http.version=HTTP/1.1` 规避大包 408）；push 后核验 `origin/feat/android-ui-vnext-translation == 本地 HEAD`。
- ending HEAD：`4920949ae8e48778ca2c42845ed0e7bee8b79ba9`。

## 17. Evidence SHA 锚点

- `EVIDENCE_SHA256SUMS.txt` 覆盖全部 PNG + manifests + probe（64 条）。
- 截图 manifest 内每张 PNG 的 `fileSha256 == sha256`（0 mismatch），保证证据可复现审计。


## 18. 2026-10-05 Human Review supersession note

This report remains the authoritative record for the runtime execution performed from
`4e43511ae9754413ffedeaca9ad21a71b330aa64`; it is **not** the runtime acceptance record for current
Android UI source.

Human pixel review subsequently triggered presentation/test corrections, including Region Detail hierarchy,
Globe evidence readiness, Change Phone truth copy, compact Studio density, adaptive navigation hierarchy,
compact top-chrome craft, and compact Studio theme-gallery composition. The latest Android production-UI
checkpoint after that closure is:

```
77c7b3692c9b3327331f52af522a2bc65ecf165e
```

The old a11y-string and forced-wide-phone test-bed failures recorded in §12 were adjudicated and corrected in
later source/contracts; they are no longer the current blocker. The current blocker is evidence freshness:

```
ANDROID_RUNTIME_EVIDENCE_FOR_CURRENT_HEAD = REQUIRED
ANDROID_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_ACCEPTANCE
ANDROID_REFERENCE_FREEZE = HOLD
IOS_UI_VNEXT = HOLD
HARMONY_UI_VNEXT = HOLD
```

No current-head build/runtime PASS is claimed by this historical report.
