# PRODUCT_V0_1_2_RELEASE_MANIFEST

- 版本：PDIG / DepMap **0.1.2** Developer Preview（Pre-release）
- 日期：2026-09-24
- Git：tag `product-v0.1.2` = `main` = origin/main（ff-only，无 force，无 history rewrite）
- 本文件不含任何密钥、口令或真实数据。

## 发布资产与 SHA256

来源：`C:\Users\Kaiser\pdig-release-012\SHA256SUMS.txt`（真实构建产物逐文件哈希）

```
5406d9e7edd6f274b5de23752d98451ded94716c88ff049589e4afe6b4716c8e  PDIG-0.1.2-android-preview.apk
fc5448782262e007a20f050c56043af6b5bfc197d5c2891774e341d7376eada9  PDIG-0.1.2-SBOM.cyclonedx.json
fa3a229355f273e1121c534b0243525f2caed9622f624e694beb00a671545b01  PDIG-0.1.2-windows-x64-portable.zip
92baac291afbd8d41da5cfebb3451593458b9afeee3066540b93b0967ff973a6  PDIG-0.1.2-windows-x64-setup.exe
7275ec5b40aa5bd659e9938d8fc4491afbcc9ff095d1fa138f25b0e5deaee944  THIRD-PARTY-NOTICES.md
```

## 构建与验证证据（全部本地实跑）

| 项                               | 结果                                                                                                                                                                                                          |
| -------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Android `assemblePreviewRelease` | BUILD SUCCESSFUL（1m19s，52 tasks）                                                                                                                                                                           |
| APK 清单                         | `package='com.pdig.app.preview'` `versionCode='200003'` `versionName='0.1.2'`（platform API 36）                                                                                                              |
| apksigner verify                 | v2 scheme Verified（NON-PROD 测试签名，**非生产签名**）                                                                                                                                                       |
| Android JVM 测试                 | `:core:test` 71/71 · `:app:testDebugUnitTest` 9/9（fresh rerun，0 failure）                                                                                                                                   |
| Android 仪器化测试基线           | `TEST-pdig36(AVD)-16-_app-preview.xml` 60/60 PASS（2026-09-24 11:39）                                                                                                                                         |
| Android 运行时 smoke（本轮）     | `RUNTIME_ENVIRONMENT_BLOCKED`（本机全部 AVD full startup 静默退出，无崩溃日志；如实记录）                                                                                                                     |
| Desktop `:app:test`              | BUILD SUCCESSFUL（27s，0 failure）                                                                                                                                                                            |
| Desktop `--smoke`                | 16/16 PASS（fresh-launch / create-open / wrong-password / tampered / future-schema / import / proposal / criticality / 三 Scenario / engine candidate+drift / accept-dismiss / backup-restore-reopen-delete） |
| Desktop GUI 启动                 | PDIG.exe（jpackage 0.1.2 镜像）启动 20s 无崩溃（窗口 1100×720，分辨率覆盖以默认窗口实跑，如实记录）                                                                                                           |
| Windows 打包                     | NSIS installer 149,537,743 B + portable zip 149,769,364 B（jpackage app-image + Compress-Archive）                                                                                                            |
| SBOM                             | `PDIG-0.1.2-SBOM.cyclonedx.json`，CycloneDX 1.5，**151 components**，由真实构建产物（jpackage-libs + Gradle dependency tree）生成                                                                             |
| 质量 Gate                        | `check-quality.mjs` VERDICT PASS exit 0（10 项计数全 0）                                                                                                                                                      |
| core `npm run check`             | 全绿：453/453 tests、circular=0、network=0、secret scan 988 files PASS、UI gate 30 `.uvue`                                                                                                                    |
| conformance                      | android 91/91 PASS（fresh 2026-09-24）；harmony 87/91（4 项 runtime-blocked，2026-09-19 记录）；ios 91/91（2026-09-19 记录）                                                                                  |
| secret scan（release commit）    | PASS（无明文秘密；凭据仅存于 git-ignored `local_private/`）                                                                                                                                                   |

## 签名声明

- **Android**：APK 使用 NON-PROD 测试签名（`local_private/build-chain/pdig-nonprod.jks`，git-ignored）。
  生产签名仍为外部阻塞（缺用户提供的真实 keystore）。包名含 `.preview` 后缀，明确非生产。
- **Windows**：未签名（无代码签名证书），SmartScreen 提示属预期；用 `SHA256SUMS.txt` 校验。

## 状态

- `gh release list`：`PDIG 0.1.2 Developer Preview`（Pre-release，tag `product-v0.1.2`）可见。
- 旧 tag `product-v0.1.0` / `product-v0.1.1` 及对应 Release **原样保留**。
