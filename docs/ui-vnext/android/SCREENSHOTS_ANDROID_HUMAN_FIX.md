# SCREENSHOTS_ANDROID_HUMAN_FIX.md

> Android UI vNext Human Fix 轮 —— 真实 API36 emulator runtime 截图索引（2026-10-03）。
> 图片来源：Android instrumentation（`AndroidUiVNextHumanFixEvidenceTest`，
> `captureToImage` = 设备内 Compose 实际像素；Phone AVD `main` + Tablet AVD `pdig_tablet_api36`）。
> 证据包：`artifacts/runtime-evidence/2026-10-03-android-ui-vnext-human-fix/`。

## Phone（14 张，Pixel 7 / API36 / 1080×2400 portrait / commit 0a3a4b6）

| #   | screen               | expectedState  | renderReadiness | layoutContracts                |
| --- | -------------------- | -------------- | --------------- | ------------------------------ |
| 01  | now                  | now            | TEXTURE_READY   | globeRenderState=TEXTURE_READY |
| 02  | overview             | global         | TEXTURE_READY   | globeRenderState=TEXTURE_READY |
| 03  | cards                | global         | N/A             | verticalTextRegression=false   |
| 04  | card-detail          | card-cn-2      | N/A             | N/A                            |
| 05  | card-studio-glass    | glass          | N/A             | N/A                            |
| 06  | card-studio-city     | city           | N/A             | N/A                            |
| 07  | numbers              | global         | N/A             | visibleRows=6                  |
| 08  | number-detail        | num-cn-1       | N/A             | N/A                            |
| 09  | number-studio-travel | travel         | N/A             | N/A                            |
| 10  | change-current       | current        | N/A             | compact-scene                  |
| 11  | change-transition    | transition     | N/A             | compact-scene                  |
| 12  | change-after         | after          | N/A             | compact-scene                  |
| 13  | cards-empty          | empty-cards    | N/A             | N/A                            |
| 14  | search-command       | search-command | N/A             | N/A                            |

文件：`phone/android__phone__api36__<screen>__<state>.png` + `phone/manifest.json`

## Tablet（14 张，Pixel Tablet / API36 / 2560×1600 landscape / commit 0a3a4b6）

| #   | screen               | expectedState  | renderReadiness | layoutContracts                                      |
| --- | -------------------- | -------------- | --------------- | ---------------------------------------------------- |
| 01  | now                  | now            | TEXTURE_READY   | globeRenderState=TEXTURE_READY                       |
| 02  | overview             | global         | TEXTURE_READY   | globeRenderState=TEXTURE_READY                       |
| 03  | cards                | global         | N/A             | verticalTextRegression=false                         |
| 04  | card-detail          | card-cn-2      | N/A             | N/A                                                  |
| 05  | card-studio-glass    | glass          | N/A             | N/A                                                  |
| 06  | card-studio-city     | city           | N/A             | N/A                                                  |
| 07  | numbers              | global         | N/A             | visibleRows=7                                        |
| 08  | number-detail        | num-cn-1       | N/A             | N/A                                                  |
| 09  | number-studio-travel | travel         | N/A             | N/A                                                  |
| 10  | change-current       | current        | N/A             | oldVisible=true;servicesVisible=true;newVisible=true |
| 11  | change-transition    | transition     | N/A             | oldVisible=true;servicesVisible=true;newVisible=true |
| 12  | change-after         | after          | N/A             | oldVisible=true;servicesVisible=true;newVisible=true |
| 13  | cards-empty          | empty-cards    | N/A             | N/A                                                  |
| 14  | search-command       | search-command | N/A             | N/A                                                  |

文件：`tablet/android__tablet__api36__<screen>__<state>.png` + `tablet/manifest.json`

## Human Review 重点

- **03 cards（phone）**：COMPACT 单列整卡；card identity（wine/copper、graphite/red、
  cool-graphite、warm-window、geo-contour、charcoal/coral、glass dispersion、navy/brushed）
  每个 issuer 可辨；无竖排/无堆叠/无裁剪。
- **05/06 studio（glass vs city）**：theme 切换真实改变 artwork（玻璃弥散条 vs 天际线
  暖窗），thumbnail 可一眼区分。
- **07 numbers（phone）**：首屏可见号码列表（visibleRows=6 ≥3），无 Desktop Inspector。
- **08 number-detail**：Identity → 关联服务 连续（gap ≤ 48dp）。
- **10–12 change**：phone compact stepper 全 6 步可见；tablet OLD/SERVICES/NEW 三列
  首屏可见。
- **01/02 globe**：renderReadiness=TEXTURE_READY（禁黑球）；Tablet globe disc 像素断言
  通过（meanLuma≥0.12 / nonBlack≥0.30 / stddev≥0.04 / markers≥5）。

## 完整性

- 28 条 `ANDROID_UI_VNEXT_HUMAN_FIX_SCREENSHOT_MANIFEST.json` 与磁盘 PNG SHA256
  逐一核验 = 0 mismatch（`EVIDENCE_SHA256SUMS.txt` 可复跑）。
