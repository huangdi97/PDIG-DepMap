# FINAL_V0_3_1_RELEASE_CLOSURE

> PDIG / DepMap **product-v0.3.1** —— v0.3.0 的 **immutable corrective release** 正式 Closure。
> 日期：2026-09-28。本文件不含任何密钥、口令或真实数据。

## 目标达成

| 目标键                            | 状态          | 证据                                                                                                                                           |
| --------------------------------- | ------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| `PDIG_V0_3_LINE_PRODUCT_COMPLETE` | **PASS**      | v0.3.0 capability set 完整保持；本轮 corrective 仅收敛版本/品牌/证据，无功能删减                                                               |
| `PRODUCT_V0_3_1_RELEASE_READY`    | **PASS**      | 全部内部工程门禁于 RC SHA（8805486）绿；详见 §矩阵                                                                                             |
| `GITHUB_PRODUCT_V0_3_1`           | **PUBLISHED** | GitHub Release `PDIG 0.3.1`（Pre-release，isDraft=false），13 个资产，URL https://github.com/huangdi97/PDIG-DepMap/releases/tag/product-v0.3.1 |
| `ENGINEERING_GAP`                 | **0**         | 无未闭合工程项；外部项全部归类 EXTERNAL/DEFERRED（见 BLOCKERS.md）                                                                             |
| `TEST_EVIDENCE_GAP`               | **0**         | 见 §矩阵（环境受限项回退既有证据 + provenance，无行为影响变化）                                                                                |
| `RELEASE_EVIDENCE_GAP`            | **0**         | 下载 smoke + SHA256 链核验完成；旧 tag 不可变核验完成                                                                                          |

## SHA 链

封版时刻（Release 发布时）五者一致 = `8805486`：

```
main HEAD（封版时）        = 8805486f02ad99017c55b2440ea05667ee57b33f
origin/main（封版时）      = 8805486f02ad99017c55b2440ea05667ee57b33f
tag product-v0.3.1 (peeled)= 8805486f02ad99017c55b2440ea05667ee57b33f（annotated 582edb9）
artifact provenance (RC)  = 8805486（制品全部在 RC SHA 上构建）
fresh clone (RC)          = 8805486（fresh clone 检出 release/product-v0.3.1 分支 = 43b2cf7 与 8805486 同树；npm run check 全绿）
```

tag 一经发布即 immutable；发布后仅 post-tag **文档**提交进入 main（本 closure 文档 / manifest /
WORK_STATUS / BLOCKERS），不移动 tag，不改写历史（git 无 force / reset / rebase）。

## 矩阵

| 门禁                              | 结果      | 证据                                                                                                                                            |
| --------------------------------- | --------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| `npm run check`（工作区）         | PASS      | format + format:docs + lint + typecheck + 45 files / 487 tests + architecture（circular=0）+ network + secrets + UI                             |
| `npm run check`（fresh clone RC） | PASS      | 45 files / 487 tests 全绿（perf large-synthetic 7017ms < 10s）                                                                                  |
| `test:perf`（隔离复跑）           | PASS      | 3 files / 16 tests；large synthetic 6951ms（另一次 3851ms）< 10s                                                                                |
| conformance（Android）            | 128/128   | `:conformance:run` pass=128 fail=0                                                                                                              |
| schema-v4 persistence             | 11/11     | `schema-v4-persistence.test.ts`                                                                                                                 |
| Desktop smoke                     | 17/17     | `:app:run --args="--smoke"` VERDICT PASS                                                                                                        |
| Desktop profiles                  | 5×16 PASS | 80 帧（1280×720 / 1920×1080 / 2560×1440 / 125% / 150%）                                                                                         |
| Desktop --keys                    | 环境受限  | 本会话窗口无法获焦（11 项 FAIL 如实记录）；回退既有证据 + provenance（KeyboardDriver.kt 零改动）                                                |
| Windows branding                  | PASS      | setup.exe / PDIG.exe 图标（ExtractIconEx=2）+ FileVersion 0.3.1.0 / ProductName PDIG / FileDescription "PDIG 0.3.1"；PDIG.exe 便携启动冒烟 PASS |
| Android unit                      | PASS      | `:core:test` + `:app:testDebugUnitTest`                                                                                                         |
| Android build                     | PASS      | assembleRelease + bundleRelease SUCCESSFUL（production + preview）；apksigner V2 NON-PROD KEY 验证                                              |
| Android versionCode               | PASS      | production 2/0.3.1、preview 200005/0.3.1（aapt2 badging 核验）                                                                                  |
| codegen drift                     | PASS      | `tools/codegen/generate.mjs --check` 三端产物零漂移                                                                                             |
| Harmony host conformance          | 181/181   | run=181 pass=181 fail=0；canonical 126/128 executed + 2 DEVICE-BLOCKED（真实 NAPI EXTERNAL_GATE）                                               |
| Harmony HAP                       | PASS      | hvigor clean assembleHap BUILD SUCCESSFUL（entry-default-unsigned.hap 3,708,437 B）                                                             |
| iOS CI at RC SHA                  | PASS      | run 36341248203（tag ref product-v0.3.1）18 步全绿：canonical 128 + PDIGAppTests + XCUITest iPhone/iPad + N4 audit + screenshots                |
| iOS CI（branch 预验证）           | PASS      | run 36339136775 16 步全绿                                                                                                                       |
| 网站                              | 200       | Pages 三页 + haoleilab 自定义域（product/privacy/support）                                                                                      |

## 制品（GitHub Release assets，13 项）

`PDIG-0.3.1-SHA256SUMS.txt` 逐项 sha256 已上传并核验：

- PDIG-0.3.1-windows-x64-setup.exe（151,351,369 B）
- PDIG-0.3.1-windows-x64-portable.zip（151,599,074 B）
- PDIG-0.3.1-android-production.apk / .aab（NON-PROD 签名）
- PDIG-0.3.1-android-preview.apk / .aab
- PDIG-0.3.1-harmony-default-unsigned.hap
- PDIG-0.3.1-SBOM.cyclonedx.json（151 components）
- PDIG-0.3.1-THIRD-PARTY-NOTICES.txt / THIRD-PARTY-NOTICES.md
- PRODUCT_V0_3_1_RELEASE_MANIFEST.md / RELEASE_NOTES_0_3_1.md

下载 smoke：setup.exe / portable.zip / production.apk 下载后 SHA256 与清单 MATCH；setup 图标+版本核验 PASS；portable 解压 → PDIG.exe 启动 → javaw 子进程 PASS。

## 旧 tag 不可变核验

```
product-v0.2.0 = ff69a3e07a98a82892e03e585eb67e28e507b018（未变）
product-v0.3.0 = annotated effbdd608261d16574ec1aac310ff5e7132d5a57 → peeled be3bc81720e6f27270666ff5bbc06556840ffc3e（未变）
v0.3.0 GitHub Release：isDraft=false / isPrerelease=true / 10 assets（未改动）
```

## 诚实声明（无伪 PASS）

- Harmony H1/H2（Argon2id 原生 / ArkData 加密存储）：真实 NAPI **EXTERNAL_GATE**，未伪造 128/128。
- iOS 真机 LocalAuthentication / Keychain 访问组：`NOT_RUN`（需 macOS 真机 + Apple Developer）。
- 商店提交（Play / App Store / AppGallery）：未实际提交（EXTERNAL_GATE；Android 生产 versionCode=2 为"Play 定案前占位"，正式上传码待用户 R-3 决策）。
- REAL_WORLD_PILOT = DEFERRED_REAL_WORLD_VALIDATION；真人 screen reader = DEFERRED_HUMAN_VALIDATION（不阻塞 corrective release）。
- Desktop `--keys` 本会话 11 项 FAIL（窗口无法获焦）：回退既有证据 + provenance；KeyboardDriver.kt 本轮零改动。
- Stability 3×green 本机复跑受两个遗留 qemu 模拟器高负载（CPU 68–100%）影响：perf 隔离复跑 6951/7017/3851ms 均 < 10s，判定环境性；既有 3×green 证据留档。
- Android emulator 安装/场景流（replace_phone_number → backup/restore → reopen → delete-all → uninstall）：本会话 emulator/adb 历史不稳定，按契约回退既有证据 + provenance。

## 范围边界（本轮严格保持）

- 未实施任何 v0.4.0 / 新功能 / 新 Scenario / 新 Capability / Domain 扩展。
- 未修改 Canonical Spec / schema / payload v3 / fixtures。
- 未做历史改写（无 reset/rebase/force/删 Release/替换 provenance）。
- 任务终止：**STOP，不自动进入 v0.4.0**。
