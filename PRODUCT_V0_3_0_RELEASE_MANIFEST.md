# PRODUCT_V0_3_0_RELEASE_MANIFEST

> PDIG / DepMap **0.3.0** Developer Preview（Pre-release）正式发布清单。
> 全部测试与产物在本机 / macOS CI runner 真实执行/构建；本文件不含任何密钥、口令或真实数据。
> 更新：2026-09-26。Git SHA 以 `git rev-parse product-v0.3.0` 为准（本文件随 tag 提交）。

## 发布信息

- 版本：PDIG / DepMap 0.3.0 Developer Preview（Pre-release）
- 日期：2026-09-26
- Git：tag `product-v0.3.0` = exact accepted SHA（fast-forward 到 main，无 force / 无 history rewrite）
- GitHub Release 标题：`PDIG 0.3.0`（Pre-release = YES）
- `product-v0.2.0` tag 保持 `ff69a3e` 不变（未移动/覆盖/删除）

## Canonical / Schema

| 项 | 值 |
| --- | --- |
| Canonical Spec | `spec/domain/domain.json`（additive vNext：5 capabilities、3 新 relations、7 类 finding、replace_phone_number 模板等） |
| Schema | `logical-schema-v4.json` 冻结；应用逻辑 Schema v4；`.depmap` payload 仍为 v3（`DEPMAP_CONTAINER_V1` 不变） |
| Fixtures | 128 条（旧 91 **逐字节未动** + 新增 37：failure-domain 6 / recovery-cycle 7 / action-dag 7 / make-before-break 3 / temporal-change 4 / provider-policy 4 / identity-relations 6） |
| Oracle selfcheck | 128/128 PASS |

## Windows Desktop

| 项 | 值 |
| --- | --- |
| platform / arch | Windows x64 |
| 版本 | 0.3.0 |
| 打包链路 | jlink runtime + 手工 app-image（`scripts/release/build-desktop-package.ps1`，含打包后 JVM launcher 自检） |
| 启动验证 | 打包产物（jlink runtime + app jars）直接运行 `--smoke`：17/17 PASS（含 replace_phone_number + backup/restore/reopen/delete） |
| artifact | `PDIG-0.3.0-windows-x64-setup.exe`（151,318,030 B）；`PDIG-0.3.0-windows-x64-portable.zip`（151,565,624 B） |
| 签名 | 未签名（SmartScreen 提示如实披露） |

## Android Preview / Production

| 项 | 值 |
| --- | --- |
| package | production `com.pdig.app`（preview flavor 亦构建 `com.pdig.app.preview`） |
| compileSdk / targetSdk | 36（Android 16） |
| 签名 | NON-PROD 测试签名（`-PpdigNonProdSigning=true`，apksigner v2 Verified，CN=PDIG NON-PRODUCTION TEST KEY）；生产签名为外部 blocker |
| artifact | `PDIG-0.3.0-android.apk`（33,187,757 B）；`PDIG-0.3.0-android.aab`（20,949,697 B） |
| 测试 | JVM unit tests PASS；connected instrumentation **61/61 PASS**（API36 phone，0 fail/0 skip）；conformance 128/128 PASS |
| 运行时证据 | 42 屏 light/dark（`android__phone-api36__…`，含 findings / scenario-setup-phone） |

## HarmonyOS

| 项 | 值 |
| --- | --- |
| ArkTS conformance host | **179/179 PASS**（含 3 元测试 + 域自检） |
| canonical | 124/128（旧 87 + 新 37）；4 条为**真实设备门禁**（Argon2id 原生 NAPI / ArkData），逐条理由在 harness 报告 |
| 设备运行时 | 无模拟器镜像 → 外部 Gate（emulator image 环境缺失）；工程缺口 = 0 |
| HAP | 待真实设备签名（外部 Gate） |

## iOS (N4 SwiftUI)

| 项 | 值 |
| --- | --- |
| swift build（macOS-14 CI） | PASS |
| canonical conformance | **128/128 PASS**（iOS.json；含 37 条新 fixture） |
| PDIGAppTests | 10/10 PASS（含 findings / replace_phone 前置 / retire-after-verify） |
| app target audit | `app_target=true`（@main 存在） |
| 模拟器 | iPhone 15 Pro 模拟器 boot PASS（`SIMULATOR_BOOT=PASS`） |
| 截图 | macOS SwiftUI ImageRenderer 渲染 light/dark（**MACOS_RENDER**，非真机/模拟器截图，无 XCUITest） |
| 真机 LocalAuthentication / Keychain | NOT_RUN（需 macOS 真机，外部 Gate） |

## 测试证据（本机 / CI 真实执行）

| 项 | 结果 |
| --- | --- |
| core `npm run check` | PASS（476/476 tests；format+lint+typecheck+architecture circular=0+network=0+secret scan 0+UI gate） |
| core `npm run check:full` | PASS（含 perf / stability / deps / license / db-integrity / contract / property / invariants） |
| quality gate | `check-quality.mjs` VERDICT **PASS**（file-size、composable、todo、suppress、cycle、secret、senslog、kotlin-escape、deadcode、gap 全 0） |
| Android `:core:test` | 71/71 PASS |
| Android `:conformance:run` | **128/128 PASS** |
| Android connected instrumentation | **61/61 PASS**（API36 phone） |
| Desktop `--smoke` | **17/17 PASS**（含 replace_phone_number Make-Before-Break 门禁） |
| Desktop `--shots` | 53 屏 light/dark PASS（1280×720 / 1920×1080 / 2048×1152） |
| Harmony conformance host | 179/179 PASS（canonical 124 + 4 设备门禁） |
| iOS CI (run 36266556360) | canonical 128/128 + PDIGAppTests 10/10 + screenshots PASS |
| iOS Runtime Visual CI (run 36266836728) | app_target=true、SIMULATOR_BOOT=PASS、screenshots PASS |
| 跨平台差分 | `conformance/reports/differential.json` + SUMMARY.json（android PASS / harmony PASS_WITH_EXTERNAL_GATES / ios PASS） |

## 发布物（Release attachment）

- [x] `PDIG-0.3.0-windows-x64-setup.exe`
- [x] `PDIG-0.3.0-windows-x64-portable.zip`
- [x] `PDIG-0.3.0-android.apk`
- [x] `PDIG-0.3.0-android.aab`
- [x] `PDIG-0.3.0-SHA256SUMS.txt`
- [x] `PDIG-0.3.0-SBOM.cyclonedx.json`（CycloneDX 1.5，45 components）
- [x] `PDIG-0.3.0-THIRD-PARTY-NOTICES.txt`
- [x] `THIRD-PARTY-NOTICES.md`
- [x] `RELEASE_NOTES_0_3_0.md`
- [x] `PRODUCT_V0_3_0_RELEASE_MANIFEST.md`

## 已知外部 Gate（不是工程缺陷）

| Gate | Status | Root cause | Exact external requirement | Engineering work remaining | User action |
| --- | --- | --- | --- | --- | --- |
| Google Play 提交 | EXTERNAL_GATE | 无开发者账号/正式签名 | 需 Google Play Console 开发者账号 + AAB 正式签名 + 商店审核 | 0（已 SUBMISSION_READY） | 用户注册账号/签名后提交 |
| App Store 提交 | EXTERNAL_GATE | 无 Apple Developer 账号/签名 | 需 Apple Developer Program + Distribution 证书 + App Store Connect | 0（已 SUBMISSION_READY） | 用户注册账号/签名后提交 |
| AppGallery 提交 | EXTERNAL_GATE | 无华为开发者身份/签名 | 需华为开发者认证 + AppGallery 签名 | 0（已 SUBMISSION_READY） | 用户注册账号/签名后提交 |
| Harmony 设备运行时 | EXTERNAL_GATE | 模拟器镜像不可用 | 需 Harmony 真机/模拟器（Argon2id 原生 NAPI + ArkData） | 0（4 条 canonical 为设备门禁） | 提供设备环境后复跑 |
| iOS 真机 LocalAuthentication / Keychain | EXTERNAL_GATE | 无 macOS 真机 | 需真机 + 开发者证书 | 0 | 用户提供真机环境 |
| Windows 安装包签名 | EXTERNAL_GATE | 无代码签名证书 | 需 Authenticode 证书 | 0 | 用户购证后签名 |
