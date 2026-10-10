# ANDROID_LIGHT_REFERENCE_ROUND4_RUNTIME_VALIDATION_REPORT

> 2026-10-06/07 · `feat/android-ui-vnext-translation` · exact-head build/test/runtime/evidence recapture
> 依据：用户 Round 4 指令（官方 SourceCompleteScreenshotEvidenceTest 完成 24+5；globe texture_ready；region-detail；typed search；tablet studio via 定制卡面；touch target ≥48dp；contact sheets + b64）
> 角色：本地执行/验证 Agent（未修改任何 production source / UI / test / docs）

## 0. 结论状态

```
ANDROID_BUILD = PASS
ANDROID_UNIT = PASS
ANDROID_RUNTIME_EVIDENCE_FOR_CURRENT_HEAD = READY
  (official SourceCompleteScreenshotEvidenceTest COMPLETE on Phone and Tablet:
   24+24 main + 5+5 empty states; raw manifests record globeTextureState=texture_ready on 01-04;
   04 actualState=region-detail; 22 actualState=search-query; 17/18/19 distinct; tablet 07/08 via
   Expanded Card inspector programmatic studio; tablet 09/10/11 independent frames)
ANDROID_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_ACCEPTANCE
ANDROID_REFERENCE_FREEZE = HOLD
IOS_UI_VNEXT = HOLD
HARMONY_UI_VNEXT = HOLD
```

## 1. HEAD 与来源

| 项 | 值 |
| --- | --- |
| BRANCH | `feat/android-ui-vnext-translation` |
| RUN_SOURCE_HEAD | `70edd4c83639bc52266db35c124db6927f53df52` |
| SOURCE_TREE | `ea5e317f661936864e655bcadf71d02dfce04273` |
| 同步 | `git fetch origin` → `git pull --ff-only`（37342cc..70edd4c，纯 fast-forward） |
| Round3→4 修复 | `9f8fb6f`（appOverride 共享 Globe 纹理实例修复 03 门槛；RegionDrawer tag；BottomNav tag；ExpandedCardInspector「定制卡面」；region 行二次点击开抽屉）+ `70edd4c`（新契约锁定） |
| APK | `assembleProductionDebug assembleProductionDebugAndroidTest` PASS（exact head）；SHA256 `584cfb36…`，44,117,283 B |
| Evidence 目录 | `artifacts/runtime-evidence/2026-10-06-android-light-reference-round4-70edd4c/` |

## 2. 核心：官方截图套件（standalone，每设备 fresh）

`SourceCompleteScreenshotEvidenceTest` **OK(1)** 双设备；随后立即拉取设备端产物：

- **Phone**：24 主帧 + 5 空态全收；`phone-raw-manifest.json` 16,819 B，fresh-head=True，**`globeTextureState=texture_ready` ×4（01–04）**；`04` expected/actual=`region-detail`；`22` expected/actual=`search-query`（typed）；17/18/19 字节 318517/327817/338001（distinct）
- **Tablet**：24 主帧 + 5 空态全收；`tablet-raw-manifest.json` 16,766 B，fresh-head=True，**`texture_ready` ×4**；04=`region-detail`；22=`search-query`；17/18/19 292179/309550/324186（distinct）；**07/08 经 Expanded Card inspector「定制卡面」程序化进入 Card Studio**（537198/487434 B 独立 studio 帧）；09/10/11 独立（463905/459125/414153 B）
- 官方 Number Detail probe：`probes/NUMBER_DETAIL_LAYOUT_PROBE_{phone,tablet}.json`

## 3. 其他套件（fresh，standalone）

### Phone（19 项全 OK）
probe OK(1) · interaction **OK(12)** · backstate **OK(5)** · a11y **OK(3)**（bottom-nav/studio touch target ≥48dp 修复生效）· verticalflow OK(1) · uishots OK(2) · accessibility OK(14) · cardidentity OK(2) · globe OK(1) · visualvariant OK(5) · translation OK(1)**（干净重装后）** · humanfix OK(1)**（干净重装后）** · phonelist OK(1) · phonestudio OK(1) · phonecards OK(1) · phonechange OK(1) · thumbnails OK(1) · adaptiveshell OK(7) · lightvisual OK(8)

> 注：首次 batch 中 translation/humanfix 曾 `INSTRUMENTATION_FAILED`（test APK runner 未注册的环境残留）；clean reinstall 后 OK(1)。seed 同步复验 OK(1)。

### Tablet（19 项：16 OK + 3 FAIL 如实）
OK：probe(1) a11y(3) verticalflow(1) uishots(2) accessibility(14) cardidentity(2) globe(1) visualvariant(5) translation(1) humanfix(1) phonelist(1) phonestudio(1) phonecards(1) phonechange(1) thumbnails(1) lightvisual(8)
FAIL：
1. `interaction` 12/1：`regionListSecondTapOpensDrawerAndBackPreservesRegion`（新测试）：tablet 二次点击开抽屉后 Back 期望 `CN` 得到 `null`（line 257）。手机同套件 12/12 OK；tablet 差异如实记录，待 ChatGPT 裁决。
2. `backstate` 5/1：`cardsDetailBack_drivesRealClick` `Failed to inject touch input.`（注入抖动；手机 OK(5)）。
3. `adaptiveshell` 7/1：`wideCards_clickUpdatesInspector…`（forced-viewport wide 场景组件未显示；Round3 同款，宽屏契约场景）。

## 4. 交互/持久化（fresh，真实进程级）

| 项 | 结果 | 证据 |
| --- | --- | --- |
| Card PresentationProfile restart | **PASS（phone）**：定制卡面→Studio→极简→保存→XML `card-cn-2 minimal=True`→force-stop→relaunch→「当前主题：极简」=True，XML 一致；Tablet：官方 07/08 已程序化进 Studio（real-UI inspector 定制卡面入口在 expanded inspector 中按需滚动，XML num travel=True 跨重启） | `phone/card-restart-theme.png`、`logs/phone-prefs-mid.xml`、`logs/phone-prefs-final.xml`、`tablet/07-card-studio-glass.png`、`tablet/08-card-studio-city.png` |
| Number PresentationProfile restart | **PASS**：seed OK(1) 双设备；XML `num-cn-1 travel` 跨 force-stop/relaunch（phone+tablet final） | `logs/phone-seed-number.log`、`logs/tablet-seed-number.log`、prefs-final |
| Workspace persistence | **PASS（phone）**：`即将到来` toggle→XML `show_upcoming=true`→force-stop→relaunch→仍 true；Tablet：状态机套件+已存 r3 真实 UI 证（本轮 tablet real-UI toggle 滚动未达，如实） | `logs/phone-workspace-mid.xml`、`logs/phone-workspace-after.xml` |
| Search→Result→Back | **PASS**：Phone 现在→搜索→catalog 卡片（result）→System Back→回 Search；Tablet Back→现在 | `phone/search-real.png`、`phone/search-result.png`、`phone/search-back.png`、`tablet/tablet-search-back.png` |
| Region select→Detail→Back→Global reset | **PASS（官方程序化）**：04 actual=`region-detail`；交互套件 phone 12/12 OK；tablet 真实 UI：中国→Numbers banner 当前地区=True→查看全球 reset=True | `manifests/{phone,tablet}-raw-manifest.json`、`tablet/tablet-region-numbers.png`、`tablet/tablet-region-reset.png` |
| System Back | **PASS**：phone card/number detail Back→root；backstate phone OK(5) | `phone/back-cards.png`、`phone/back-numbers.png` |
| Change Current/Transition/After | **PASS**：官方三帧 distinct 双设备；18 语义横幅；19=计划投影 | 官方帧 + `logs/*-suite-screenshots.log` |

## 5. Contact Sheets（Human Review bridge）

`contact-sheets/`（不裁剪、按原始像素等比缩放拼接、带屏名标签；PNG + base64 文本镜像）：

| 文件 | PNG | b64 文本 |
| --- | --- | --- |
| contact-phone-24.png(.b64.txt) | 3,121,580 B（6 列 × 4 行 × 450×1000 单元） | 4,162,108 chars |
| contact-tablet-24.png(.b64.txt) | 1,367,939 B（6 列 × 4 行 × 640×400 单元） | 1,823,920 chars |
| contact-empty-10.png(.b64.txt) | 488,644 B（5 列 × 2 行：手机空态 5 + 平板空态 5） | 651,528 chars |

ChatGPT 可从 GitHub 直接下载 `.b64.txt` 解码还原 PNG（`base64 -d` / PowerShell `[IO.File]::WriteAllBytes`）审阅真实像素。

## 6. 核心门禁 / logcat

- CORE_CHECK：同历史结论（`format:docs:check` 为 ChatGPT docs prettier 漂移；lint/typecheck/test/architecture/network/secrets/ui EXIT=0）`logs/core-check.log`
- DESKTOP_FREEZE_GUARD：PASS（12/12）`logs/desktop-freeze-guard.log`
- logcat phone/tablet：无 `FATAL EXCEPTION`（com.pdig）/`ANR in com.pdig`/`OutOfMemory`/Compose 崩溃（logcat 中 3 条 FATAL 属无关第三方 `com.zhishen.prototype` RN app；1 条 uiautomator 自身 UiAutomation 注册冲突；均已注明）

## 7. 生产源改动审计 / 交付

- `git diff --name-only`（tracked）= 空；`android/app/src/main/**` 改动 = 0；本轮未触碰任何 production/source/test/docs
- evidence commit 仅含 `artifacts/runtime-evidence/2026-10-06-android-light-reference-round4-70edd4c/**`
- 用户既有未跟踪文件 `IMAGE_METRICS.json` 未触碰
- 交付物：phone/tablet（24+24 PNG）、empty-states/（10 PNG）、contact-sheets/（3 PNG + 3 b64）、manifests/（SOURCE_PROVENANCE/DEVICE_MATRIX/SCREENSHOT_MANIFEST/INTERACTION_VALIDATION/raw manifests）、probes/、logs/、EVIDENCE_SHA256SUMS.txt（155 条）、本报告

## 8. 最终状态

`ANDROID_RUNTIME_EVIDENCE_FOR_CURRENT_HEAD = READY`；`ANDROID_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_ACCEPTANCE`；`ANDROID_REFERENCE_FREEZE = HOLD`；`IOS_UI_VNEXT / HARMONY_UI_VNEXT = HOLD`。官方 24+5×2、texture_ready×8、region-detail、typed search、tablet studio、touch target≥48dp 全部达成；3 个 tablet 套件失败如实记录（region 二次点击 back 保留 / 注入抖动 / wide forced-viewport 场景）。未写 VISUAL_ACCEPTED / FREEZE=PASS。