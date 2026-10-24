# SCREENSHOTS_ANDROID_TRANSLATION.md

> Android UI vNext 翻译运行时截图证据（任务书 §38-§41 / §52）。
> 生成：2026-10-02；commit `b3d8aa3`；包：`artifacts/runtime-evidence/2026-10-02-android-ui-vnext-translation/`。

## 1. 设备与方式

| 项          | Phone                                                                             | Tablet                                                                    |
| ----------- | --------------------------------------------------------------------------------- | ------------------------------------------------------------------------- |
| AVD         | `main`（API 36, Pixel 7）                                                         | `pdig_tablet_api36`（API 36, Pixel Tablet，本机 android-36 系统镜像新建） |
| viewport    | 1080×2400                                                                         | 2560×1600                                                                 |
| density     | 2.625                                                                             | 2.0                                                                       |
| orientation | portrait                                                                          | landscape                                                                 |
| 渲染        | 真实 emulator runtime（instrumentation captureToImage = 设备内 Compose 实际像素） | 同左                                                                      |
| 布局分支    | COMPACT（BottomNav + chips）                                                      | EXPANDED（NavigationRail + list-detail）                                  |

## 2. 14 张 Human Main Set（phone）

`artifacts/runtime-evidence/2026-10-02-android-ui-vnext-translation/phone/`

| #   | 文件                                                         | screen        | expectedState  | stateValidation                               |
| --- | ------------------------------------------------------------ | ------------- | -------------- | --------------------------------------------- |
| 01  | android__phone__api36__01-now__now.png                       | now           | now            | expected=now;actual=now                       |
| 02  | android__phone__api36__02-overview__global.png               | overview      | global         | expected=global;actual=global                 |
| 03  | android__phone__api36__03-cards__global.png                  | cards         | global         | expected=global;actual=global                 |
| 04  | android__phone__api36__04-card-detail__card-cn-2.png         | card-detail   | card-cn-2      | expected=card-cn-2;actual=card-cn-2           |
| 05  | android__phone__api36__05-card-studio-glass__glass.png       | card-studio   | glass          | expected=glass;actual=glass                   |
| 06  | android__phone__api36__06-card-studio-city__city.png         | card-studio   | city           | expected=city;actual=city                     |
| 07  | android__phone__api36__07-numbers__global.png                | numbers       | global         | expected=global;actual=global                 |
| 08  | android__phone__api36__08-number-detail__num-cn-1.png        | number-detail | num-cn-1       | expected=num-cn-1;actual=num-cn-1             |
| 09  | android__phone__api36__09-number-studio-travel__travel.png   | number-studio | travel         | expected=travel;actual=travel                 |
| 10  | android__phone__api36__10-change-current__current.png        | change        | current        | expected=current;actual=current               |
| 11  | android__phone__api36__11-change-transition__transition.png  | change        | transition     | expected=transition;actual=transition         |
| 12  | android__phone__api36__12-change-after__after.png            | change        | after          | expected=after;actual=after                   |
| 13  | android__phone__api36__13-cards-empty__empty-cards.png       | cards-empty   | empty-cards    | expected=empty-cards;actual=empty-cards       |
| 14  | android__phone__api36__14-search-command__search-command.png | search        | search-command | expected=search-command;actual=search-command |

## 3. Tablet 主集（8+ 屏，landscape）

`artifacts/runtime-evidence/2026-10-02-android-ui-vnext-translation/tablet/`（同 14 屏命名，deviceClass=tablet）。
至少包含：now / overview / cards / card-detail / studio（glass+city）/ numbers / number-detail / change-transition。

## 4. Variant Truth（任务书 §41）

每个变体同时验证语义状态与渲染输出（SHA256 互异）：

| 变体对                                | Phone SHA 互异         | 测试                                                                    |
| ------------------------------------- | ---------------------- | ----------------------------------------------------------------------- |
| card glass vs city                    | ✅                     | AndroidVisualVariantEvidenceContractTest.cardStudioGlassDiffersFromCity |
| number country vs travel vs recovery  | ✅                     | …numberStudioCountryTravelRecoveryPairwiseDistinct                      |
| change current vs transition vs after | ✅                     | …changePhoneCurrentTransitionAfterAreDistinct                           |
| overview global vs region(hk)         | ✅（camera 语义 + 帧） | …regionGlobalDiffersFromHk                                              |
| 证据隔离（无持久化 profile）          | ✅                     | …evidenceIsolation_noPersistedProfileReadOrWrite                        |

## 5. Manifest

- `manifest/phone-manifest.json`、`manifest/tablet-manifest.json`：设备侧生成（字段见 §6）。
- `manifest/ANDROID_UI_VNEXT_SCREENSHOT_MANIFEST.json`：合并版（28 条）。
- 仓库根 `ANDROID_UI_VNEXT_SCREENSHOT_MANIFEST.json`：同上（可发现性副本）。

## 6. 每条记录字段

platform / device / deviceClass / api / viewport / density / orientation / screen /
expectedState / actualState / stateValidation / sha256 / commit。

## 7. 完整性

- `EVIDENCE_SHA256SUMS.txt`：28 张 PNG 的 SHA256 与文件路径。
- screenshot exists ≠ visual pass：本包由 9 个自动化测试门（含 Variant Truth + a11y）守护；
  视觉验收状态为 `NEEDS_HUMAN_FINAL_ACCEPTANCE`。
