# PDIG 0.1.2 Developer Preview — Release Notes

发布日期：2026-09-24（本地实跑证据）

## 本轮内容

- **文档规范化**：121 个 Markdown 文档经 Prettier 批量格式化（blockquote 缩进、表格列宽重排），
  纯格式类改动，无业务语义变更（`git diff -w` 复核后提交，commit `a9c1cab`）。
- **审计轮收口**：五份 `GLOBAL_*` 审计 + `CODE_SIZE_AUDIT` + `DEPENDENCY_AUDIT` 就地更新 v0.1.2 轮真实发现与修复；
  `scripts/quality/EXCEPTIONS.json` 复核为合法 UTF-8（无乱码）。
- **Dead code 修复**：`DesktopFileOps.write`（desktop/app/src/main/kotlin/com/pdig/desktop/io/FileOps.kt）
  无调用者、无覆盖实现，已删除（保留 pickOpen / pickSave / readBytes）。
- **版本升级**：Android preview `versionCode 200002 → 200003`、`versionName 0.1.1 → 0.1.2`；
  Windows Desktop `packageVersion / version 0.1.1 → 0.1.2`。

## 平台状态（本轮实跑）

| 平台                  | 状态                            | 证据                                                                                                                                                                           |
| --------------------- | ------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Android Preview 0.1.2 | 构建 PASS                       | `assemblePreviewRelease` BUILD SUCCESSFUL（1m19s）；APK `com.pdig.app.preview` / `versionCode=200003` / `versionName=0.1.2`；apksigner verify：v2 scheme Verified              |
| Android JVM 测试      | 71/71 + 9/9 PASS                | `:core:test` 71/71 · `:app:testDebugUnitTest` 9/9（fresh rerun，0 failure）                                                                                                    |
| Android 仪器化测试    | 60/60 PASS（基线）              | `TEST-pdig36(AVD)-16-_app-preview.xml`，2026-09-24 11:39（本轮模拟器环境阻塞前实跑）                                                                                           |
| Android 运行时 smoke  | **RUNTIME_ENVIRONMENT_BLOCKED** | 2026-09-24 下午起本机所有 AVD（pdig36 / pdig_api36_phone / pdig_api36_tablet / API35 / API34-ARM）均在 full startup 静默退出；真实运行证据无法在本机复现，见 Known Limitations |
| Windows Desktop 0.1.2 | 构建+测试+冒烟 PASS             | `:app:test` BUILD SUCCESSFUL；`--smoke` 16/16 PASS（含三 Scenario + Backup/Restore）；PDIG.exe GUI 启动 20s 无崩溃                                                             |
| conformance           | android 91/91 PASS              | 2026-09-24 fresh（codegen / fixtureIntegrity / oracleSelfcheck PASS）；harmony 87/91（4 项 runtime-blocked，环境阻塞，2026-09-19 记录）；ios 91/91（2026-09-19 记录）          |
| 质量 Gate             | PASS                            | `check-quality.mjs` exit 0，10 项计数全 0（file-size / composable / todo / suppress / cycle / secret / senslog / kotlin-escape / deadcode / gap）                              |
| core `npm run check`  | 全绿                            | 453 tests / 43 files PASS · architecture circular=0 · network 0 primitives · secret scan 988 files · UI gate 30 `.uvue`                                                        |

## 发布物（GitHub Pre-release `product-v0.1.2`）

- `PDIG-0.1.2-windows-x64-setup.exe`（NSIS installer，未签名，SmartScreen 提示属预期）
- `PDIG-0.1.2-windows-x64-portable.zip`
- `PDIG-0.1.2-android-preview.apk`（NON-PROD 测试签名，非生产签名）
- `PDIG-0.1.2-SBOM.cyclonedx.json`（CycloneDX 1.5，151 components，真实构建产物生成）
- `THIRD-PARTY-NOTICES.md`
- `SHA256SUMS.txt`
- `RELEASE_NOTES_0_1_2.md`（本文件）
- `PRODUCT_V0_1_2_RELEASE_MANIFEST.md`

## Known Limitations（如实披露）

1. **无 Play 生产发布**：APK 为 Developer Preview（`.preview` 包名 + NON-PROD 测试签名），
   Play 商店提交继续阻塞于外部项（账号、生产 keystore、隐私 URL、bundle id）。
2. **Android 运行时 smoke 本轮环境阻塞**：2026-09-24 下午起本机模拟器（API36 全部 AVD 及回退 AVD）
   在 full startup 静默退出（WHPX 检测正常、无崩溃日志）；仪器化测试以当日上午 11:39 的 60/60 实跑为基线，
   本轮无法复现新运行证据，如实记录而非伪造。
3. **无真机、无真实账单数据**：所有运行时证据来自模拟器（历史）与 JVM/Windows 本地运行，均使用合成 fixture。
4. **Harmony / iOS 不在本轮**：conformance 保持各自记录（harmony 87/91 含 4 项环境阻塞；ios 91/91），无新运行证据。
5. **Windows 未签名**：无代码签名证书，SmartScreen 提示属预期；校验请用 `SHA256SUMS.txt`。
6. **CI billing E-10**：CI 计费为外部阻塞（`CI_EXTERNAL_BLOCKED`），本轮 Gate 全部本地真实运行。
7. **旧 release 保留**：`product-v0.1.0` / `product-v0.1.1` 及对应 Release 原样保留，无 tag 重写、无 history rewrite。
