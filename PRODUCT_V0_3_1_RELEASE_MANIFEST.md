# PRODUCT_V0_3_1_RELEASE_MANIFEST

> PDIG / DepMap **0.3.1** Developer Preview（Pre-release）—— v0.3.0 的 **immutable corrective release**。
> 全部测试与产物在本机 / macOS CI runner 真实执行/构建；本文件不含任何密钥、口令或真实数据。
> 更新：2026-09-28。Git SHA 以 `git rev-parse product-v0.3.1` 为准（本文件随 tag 提交）。

## 发布信息

- 版本：PDIG / DepMap 0.3.1 Developer Preview（Pre-release）—— corrective closure
- 日期：2026-09-28
- Git：tag `product-v0.3.1`（annotated `582edb9`）→ peeled SHA `8805486` = RC_ACCEPTED_SHA（ff-only 合回 main，无 force / 无 history rewrite）；tag 一经发布即 immutable，post-tag 文档提交不移动 tag
- GitHub Release 标题：`PDIG 0.3.1`（Pre-release = YES）
- 旧 tag 保持 immutable：`product-v0.2.0` = `ff69a3e`、`product-v0.3.0` = annotated `effbdd6`（peeled `be3bc81`）未移动/覆盖/删除；v0.3.0 Release 未改动
- 变更审计：`V0_3_0_TO_V0_3_1_CHANGE_AUDIT.md`——187 文件分类（RUNTIME_EVIDENCE 130 / BRANDING 23 / DOCUMENTATION 9 / PRODUCTION_CODE 10 / BUILD 3 / RELEASE 4 / TEST 3 / CI 1 / WEBSITE 4）；
  `NO_V0_4_SCOPE` / `NO_NEW_SCENARIO` / `NO_NEW_CAPABILITY` / `NO_DOMAIN_SCOPE_EXPANSION` = PASS

## Canonical / Schema（零改动）

| 项               | 值                                                                                         |
| ---------------- | ------------------------------------------------------------------------------------------ |
| Canonical Spec   | 未改动（v0.3.0 additive vNext 保持）                                                       |
| Schema           | `logical-schema-v4.json` 冻结不变；`.depmap` payload v3 冻结（`DEPMAP_CONTAINER_V1` 不变） |
| Fixtures         | 128 条保持（旧 91 逐字节未动）                                                             |
| Oracle selfcheck | 128/128 PASS（conformance `:conformance:run`）                                             |

## Windows Desktop（本轮 §7 品牌链闭合）

| 项              | 值                                                                                                                                                         |
| --------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| platform / arch | Windows x64                                                                                                                                                |
| 版本            | 0.3.1（Main.kt VERSION + gradle packageVersion + 根 build version）                                                                                        |
| 打包链路        | jlink runtime + 手工 app-image + NSIS 安装器 + 便携版 `PDIG.exe` 启动器（csc attributes + /win32icon）                                                     |
| 品牌            | setup.exe / PDIG.exe 均嵌入 pdig.ico 图标 + VERSIONINFO（FileVersion 0.3.1.0 / ProductName PDIG / FileDescription "PDIG 0.3.1"）；post-build 品牌校验 PASS |
| 启动验证        | `--smoke` 17/17 PASS（含 replace_phone_number + backup/restore/reopen/delete）；PDIG.exe 便携启动冒烟 PASS（javaw 子进程）                                 |
| 分辨率取证      | `--profiles` 5×16 = 80 帧 PASS（1280×720 / 1920×1080 / 2560×1440 / 125% / 150%）                                                                           |
| artifact        | `PDIG-0.3.1-windows-x64-setup.exe`；`PDIG-0.3.1-windows-x64-portable.zip`（内含 PDIG.exe + pdig.ico + PDIG.cmd 回退）                                      |
| 签名            | 未签名（SmartScreen 提示如实披露）                                                                                                                         |

## Android（版本对齐）

| 项                     | 值                                                                                                                                           |
| ---------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| package                | production `com.pdig.app`；preview `com.pdig.app.preview`                                                                                    |
| 版本                   | production versionCode **2** / versionName **0.3.1**（Play 正式上传码待用户 R-3 决策，占位如实披露）；preview versionCode **200005** / 0.3.1 |
| compileSdk / targetSdk | 36（Android 16）                                                                                                                             |
| 签名                   | NON-PROD 测试签名（`-PpdigNonProdSigning=true`，apksigner v2 Verified，CN=PDIG NON-PRODUCTION TEST KEY）                                     |
| artifact               | `PDIG-0.3.1-android-production.apk` / `.aab`；`PDIG-0.3.1-android-preview.apk` / `.aab`                                                      |
| 测试                   | JVM unit PASS；conformance 128/128 PASS；codegen drift check PASS；assembleRelease/bundleRelease SUCCESSFUL                                  |
| 运行时证据             | 既有 42 屏 light/dark + provenance（本会话 emulator 不稳定，按契约回退既有证据）                                                             |

## HarmonyOS（版本对齐）

| 项               | 值                                                                                                                                                         |
| ---------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 版本             | versionCode 1000001 / versionName 0.3.1                                                                                                                    |
| conformance host | **181/181 PASS**（run=181 pass=181 fail=0）；canonical 126/128 executed，2 条 DEVICE-BLOCKED（Argon2id 原生 / ArkData）= 真实 NAPI EXTERNAL_GATE（不伪造） |
| 构建             | hvigor clean assembleHap BUILD SUCCESSFUL → `PDIG-0.3.1-harmony-default-unsigned.hap`                                                                      |

## iOS（版本对齐 + CI at RC SHA）

| 项             | 值                                                                                                                                                                                                         |
| -------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 版本           | MARKETING_VERSION 0.3.1 / CURRENT_PROJECT_VERSION 2                                                                                                                                                        |
| CI（RC SHA）   | `ios-runtime-visual.yml` workflow_dispatch at tag `product-v0.3.1`（= RC SHA `8805486`）run 36341248203 18 步全绿：canonical 128 + PDIGAppTests + XCUITest iPhone/iPad + N4 app-target audit + screenshots |
| 前置（branch） | 同 workflow at `release/product-v0.3.1` run 36339136775 全绿（xctestrun app-path 补丁确定性修复）                                                                                                          |

## 制品与哈希

`PDIG-0.3.1-SHA256SUMS.txt`（release asset，逐项 sha256）：

- `PDIG-0.3.1-windows-x64-setup.exe`
- `PDIG-0.3.1-windows-x64-portable.zip`
- `PDIG-0.3.1-android-production.apk` / `.aab`
- `PDIG-0.3.1-android-preview.apk` / `.aab`
- `PDIG-0.3.1-harmony-default-unsigned.hap`
- SBOM：`PDIG-0.3.1-SBOM.cyclonedx.json`（151 components，cyclonedx 1.5）
- NOTICES：`PDIG-0.3.1-THIRD-PARTY-NOTICES.txt`（release asset 变体）+ `THIRD-PARTY-NOTICES.md`

## 诚实声明

- 无伪 PASS：Harmony H1/H2 为真实 NAPI EXTERNAL_GATE；iOS 真机验证 NOT_RUN（需 macOS 真机）；
  REAL_WORLD_PILOT = DEFERRED_REAL_WORLD_VALIDATION；真人 screen reader = DEFERRED_HUMAN_VALIDATION（均不阻塞 corrective release）。
- 环境受限项（回退既有证据 + provenance，无行为影响变化）：desktop `--keys`（本会话窗口无法获焦，11 项 FAIL 如实记录）；
  stability 3×green（本机两个遗留 qemu 模拟器占用 CPU 68–100% 致 perf large-synthetic 阈值偶发超时；隔离复跑 6951/7017/3851ms 均 < 10s）。
- 不实际提交任何商店；Android 生产 versionCode=2 为"Play 定案前占位"，正式上传码待用户 R-3 决策。
