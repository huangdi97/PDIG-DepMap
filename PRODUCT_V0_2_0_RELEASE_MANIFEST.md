# PRODUCT_V0_2_0_RELEASE_MANIFEST

> 状态：DRAFT（v0.2.0 收口时由执行 Agent 以真实证据填全，禁止留空/占位冒充）。
> 本文件不含任何密钥、口令或真实数据。

## 发布信息

- 版本：PDIG / DepMap **0.2.0** Developer Preview（Pre-release）
- 发布日期：（待定）
- Git SHA / tag：`product-v0.2.0` =（待定）
- main = origin/main =（待定）
- GitHub Release 标题：`PDIG 0.2.0 Preview`

## Android（Preview）

| 项 | 值 |
| --- | --- |
| package | `com.pdig.app.preview`（preview identity 保持不变） |
| versionName | 0.2.0 |
| versionCode |（按 policy 在 200003 之上递增，待定） |
| targetSdk / compileSdk | 36 |
| 签名 | NON-PROD 测试签名（`-PpdigNonProdSigning=true`）；生产签名仍为外部 blocker（E-2） |
| artifact | `PDIG-0.2.0-android-preview.apk` |
| SHA256 |（待定） |

## Windows Desktop

| 项 | 值 |
| --- | --- |
| platform / arch | Windows x64 |
| 版本 | 0.2.0 |
| artifact | `PDIG-0.2.0-windows-x64-setup.exe` / `PDIG-0.2.0-windows-x64-portable.zip` |
| SHA256 |（待定，见 SHA256SUMS.txt） |
| 签名 | 未签名（SmartScreen 提示如实披露） |
| 打包 | jlink runtime + 手工 app-image（`scripts/release/build-desktop-package.ps1`，含打包后 JVM launcher 验证） |

## 测试证据（收口时填实际数字）

- Android：`:core:test` / `:app:testDebugUnitTest` / `:conformance:run` / 仪器化 / Core Journey / 三场景 / assemble
- Desktop：`:app:test` / `--smoke` / Core Journey / 三场景 / backup-restore / packaging
- 双端一致性：DUAL_CLIENT_EXPERIENCE_MATRIX_V0_2.md（逐行证据）
- 工程门禁：`npm run check`（core）+ `check-quality.mjs` VERDICT
- Security：secret scan / INTERNET=0 / 敏感日志 0

## 发布物清单（Release attachment）

- [ ] `PDIG-0.2.0-windows-x64-setup.exe`
- [ ] `PDIG-0.2.0-windows-x64-portable.zip`
- [ ] `PDIG-0.2.0-android-preview.apk`
- [ ] `PDIG-0.2.0-SHA256SUMS.txt`
- [ ] `PDIG-0.2.0-SBOM.cyclonedx.json`
- [ ] `PDIG-0.2.0-THIRD-PARTY-NOTICES.txt`
- [ ] `PRODUCT_V0_2_0_RELEASE_MANIFEST.md`
- [ ] `RELEASE_NOTES_0_2_0.md`

## Known Limitations（如实）

- Developer Preview；无真实用户/真实数据验证
- Android 为 GitHub Preview，非 Google Play 生产
- Windows 未签名（SmartScreen）
- Harmony / iOS 不在本轮
- 真机验证状态：无真机（API36 AVD 覆盖）
- v0.1.2 Windows 发布物存在缺 JVM launcher 缺陷（已取证，本版修复）