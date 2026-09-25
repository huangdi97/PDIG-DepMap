# PRODUCT_V0_2_0_RELEASE_MANIFEST

> PDIG / DepMap **0.2.0** Developer Preview（Pre-release）正式发布清单。
> 全部测试与产物在本机真实执行/构建；本文件不含任何密钥、口令或真实数据。
> 更新：2026-09-25。Git SHA 以 `git rev-parse product-v0.2.0` 为准（本文件随 tag 提交）。

## 发布信息

- 版本：PDIG / DepMap 0.2.0 Developer Preview（Pre-release）
- 日期：2026-09-25
- Git：tag `product-v0.2.0` = main = origin/main（fast-forward，无 force，无 history rewrite）
- GitHub Release 标题：`PDIG 0.2.0 Preview`（Pre-release = YES）
- 发布分支：`release/product-v0.2.0`（自 main 创建）

## Android Preview

| 项                        | 值                                                                                                                                      |
| ------------------------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| package                   | `com.pdig.app.preview`（preview identity 不变）                                                                                         |
| versionName / versionCode | 0.2.0 / 200004（policy：200001→0.1.0, 200002→0.1.1, 200003→0.1.2, 200004→0.2.0）                                                        |
| targetSdk / compileSdk    | 36（Android 16）                                                                                                                        |
| 签名                      | NON-PROD 测试签名（`-PpdigNonProdSigning=true`，apksigner v2 Verified，CN=PDIG NON-PRODUCTION TEST KEY）；生产签名为外部 blocker（E-2） |
| artifact                  | `PDIG-0.2.0-android-preview.apk`（33,171,401 B）                                                                                        |
| SHA256                    | `ff13e51bee5eeec006ebde7a6c387546c8616d6a50bb36776612148303cca342`                                                                      |

## Windows Desktop

| 项              | 值                                                                                                                                                    |
| --------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| platform / arch | Windows x64                                                                                                                                           |
| 版本            | 0.2.0                                                                                                                                                 |
| 打包链路        | jlink runtime + 手工 app-image（`scripts/release/build-desktop-package.ps1`，**含打包后 JVM launcher 自检**）——修复 v0.1.2 缺 java/javaw 缺陷         |
| 启动验证        | portable 解压后启动 **javaw pid，窗口标题 `PDIG 0.2.0 Preview`**（真实窗口）                                                                          |
| artifact        | `PDIG-0.2.0-windows-x64-setup.exe`（151,172,741 B）/ `PDIG-0.2.0-windows-x64-portable.zip`（151,419,840 B）                                           |
| SHA256          | setup `ecad6055ab0f068a7ca1ccaa1dfff36d22ebb1bf155133fd803f11b2a986ad53`；portable `2adc08ed5b36f43030f33732c733aa723959e0b246dc0628b0a9326cf0399498` |
| 签名            | 未签名（SmartScreen 提示如实披露）                                                                                                                    |

## 测试证据（2026-09-25 真实执行）

| 项                               | 结果                                                                                                                                               |
| -------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- |
| core `npm run check`             | PASS：453/453 tests、architecture circular=0、network=0 原语、secret scan 1005 文件 0 泄漏                                                         |
| quality gate                     | `check-quality.mjs` VERDICT **PASS**（10 项计数全 0；EXCEPTIONS 6 条既有 justified）                                                               |
| Android `:core:test`             | **71/71**（--rerun-tasks fresh）                                                                                                                   |
| Android `:app:testDebugUnitTest` | **63/63**（新增 54 个 v0.2.0 UI 逻辑单测，0 失败）                                                                                                 |
| Android `:conformance:run`       | **91/91**（fresh clone 亦 91/91）                                                                                                                  |
| Android 仪器化（pdig36 phone）   | **60/60 PASS**（0 fail/0 skip，fresh clone 复跑 60/60）                                                                                            |
| Android API36 tablet             | 2560×1600 安装/启动（am start Status:ok）/截图/卸载 PASS；全量 connected 套件在平板 AVD 本环境会话挂起（v0.1.x 同 AVD 曾 59/59，环境限制如实记录） |
| Desktop `:app:test`              | 全部 PASSED（14 cases）                                                                                                                            |
| Desktop `--smoke`                | **16/16 PASS**（含 3 场景 + done≠verified + backup/restore/reopen/delete，即 Desktop Core Journey）                                                |
| E2E / Core Journey               | Android 证据套件 60 项（含 Core Journey 路径/三场景/DeleteAll/BackupExport/PerfSmoke）；Desktop smoke 16/16                                        |
| 双端一致性                       | DUAL_CLIENT_EXPERIENCE_MATRIX_V0_2.md + 源码审计（内部术语不上屏）+ 双端术语词典                                                                   |
| Fresh Clone 最终回归             | Android core/JVM/conformance/签名 APK 构建/仪器化 60/60 + Desktop compile/test/smoke 全 PASS                                                       |

## 双端 v0.2.0 新增（用户视角）

- 首页"需要你处理"聚合计划；Impact 必须处理项带解释；Infrastructure 搜索/分组；
  NodeDetail 六问卡；CSV 字段对应可改；导入错误分层文案；隐私声明统一（Android）
- 7 项一级导航 + Attention 屏 + Infrastructure 双栏 + 内部术语清零 + 0.2.0 版本（Desktop）
- 文档：PRODUCT_EXPERIENCE_MAP_V0_2 / DUAL_CLIENT_EXPERIENCE_MATRIX_V0_2 /
  PRODUCT_TERMINOLOGY_V0_2 / PRODUCT_ERROR_CATALOG / docs/user/*

## 发布物（Release attachment）

- [x] `PDIG-0.2.0-windows-x64-setup.exe`
- [x] `PDIG-0.2.0-windows-x64-portable.zip`
- [x] `PDIG-0.2.0-android-preview.apk`
- [x] `PDIG-0.2.0-SHA256SUMS.txt`
- [x] `PDIG-0.2.0-SBOM.cyclonedx.json`（CycloneDX 1.5，151 components）
- [x] `THIRD-PARTY-NOTICES.md`
- [x] `PRODUCT_V0_2_0_RELEASE_MANIFEST.md`
- [x] `RELEASE_NOTES_0_2_0.md`

## Known Limitations（如实）

- Developer Preview；无真实用户/真实数据验证（全合成 fixture）
- Android 为 GitHub Preview，非 Google Play 生产（无生产 keystore、无商店账号 E-2/E-5）
- Windows 未签名（SmartScreen）
- Harmony / iOS 不在本轮（E-9 暂停 / E-8 macOS 阻塞）
- 无真机（E-1）；API36 AVD phone + tablet 覆盖（tablet 全量套件挂起为本环境限制，见上）
- CI 因 GitHub 账户计费外部阻断（E-10）；本地全量门禁为验收依据
- 屏幕阅读器仅工程级检查（未做真人测试），不宣称 SCREEN_READER_VERIFIED
