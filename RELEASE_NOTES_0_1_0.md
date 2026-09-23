# PDIG 0.1.0 Developer Preview — Release Notes

> 发布：2026-09-24（GitHub Pre-release，tag `product-v0.1.0`）
> 产品版本：`0.1.0`（Developer Preview，**不是** Stable Production）

## What PDIG is

PDIG（Personal Digital Infrastructure Graph，内部代号 **DepMap**）是一个 **local-first**
的个人数字基础设施图谱：在换卡、换号、换邮箱、注销账户之前，告诉用户哪些账户、支付路径
和依赖会受到影响，以及应该先处理什么。

它不是密码管理器、不是记账软件、不是支付钱包、不是订阅管理器、不是云服务。
**没有后端、没有账户、没有 analytics、没有广告、没有云同步。**

核心语义（永久冻结，机器推断 ≠ 现实）：

```
Observation → Human Confirmation → Confirmed Reality → Impact → ChangePlan → Verification
Observation != Reality，Proposal != Reality，done != verified
```

## What 0.1.0 includes

- **全仓工程质量治理收尾**：全仓代码/架构/注释/测试审计真实完成，自动化质量 Gate
  （`scripts/quality/check-quality.mjs`）建立，10 项 counter 全绿。
- **Windows Desktop**（新增发行目标）：Kotlin/JVM + Compose Desktop，23 屏覆盖
  MVP01–MVP03 核心用户链（Home/Sources/Import/Mapping/Review/Proposal/Candidate/Drift/
  Infrastructure/Node Detail/Scenario Center/Setup/Impact/ChangePlan/Action/Verification/
  Timeline/Backup/Restore/Settings/Security/About/Gate）。
- **Android Preview**（GitHub Developer Preview，非 Play）：原生 Kotlin/Compose，
  SQLCipher + Android Keystore + biometric App Lock。
- **三个 Scenario 真实闭环**（Setup → Impact → Plan → Action → Verification）：
  - `replace_payment_card`（更换支付卡）
  - `expiring_payment_card`（支付卡到期）
  - `close_payment_instrument`（注销支付卡）
- **Import**：微信账单 CSV、通用 CSV（字段映射）、OFX/QFX、Manual。
- **Backup / Restore**：Windows 与 Android 均可用；备份始终是**加密**容器。

## Downloads

| 平台 | 文件 | 说明 |
| --- | --- | --- |
| Windows x64 | `PDIG-0.1.0-windows-x64-setup.exe` | NSIS 安装器（未签名，见 Known Limitations） |
| Windows x64 | `PDIG-0.1.0-windows-x64-portable.zip` | 便携包：解压后运行 `PDIG/PDIG.exe` |
| Android | `PDIG-0.1.0-android-preview.apk` | Preview APK（`com.pdig.app.preview`，versionCode 200001） |

校验：所有二进制的 SHA-256 见 `SHA256SUMS.txt`。依赖清单见
`PDIG-0.1.0-SBOM.cyclonedx.json`，第三方许可见 `THIRD-PARTY-NOTICES.md`。

## Data / privacy model

- Local-first：数据只存在你的设备上；无后端、无账户、无遥测。
- 原始账单（raw statement）仅存在于导入会话内存，会话结束即销毁；永不持久化单笔交易。
- 持久化的只有：指纹、Evidence Summary、Proposal 状态、用户确认后的图实体。
- Windows Desktop：数据以 `.depmap` 加密容器（`DEPMAP_CONTAINER_V1`：Argon2id v19 +
  AES-256-GCM + RFC 8785 JCS）落盘；本机解锁口令经 Windows DPAPI 保护（opt-in）。
- Android：本地数据库 SQLCipher 加密，密钥在 Android Keystore；App Lock 支持系统生物识别。

## Current scope

- MVP01–MVP03 支付 capability 核心链（Impact Kernel 仅 `payment`）。
- 本轮 Release 范围：Windows Desktop + Android Preview；HarmonyOS 与 iOS **不在** product-v0.1.0。

## Known Limitations（如实披露）

1. **Google Play Production 尚未发布**。Android 这个 APK 是 GitHub Developer Preview，
   以**非生产测试密钥**签名（证书名 `CN=PDIG NON-PRODUCTION TEST KEY`），**不是**
   Play App Signing 证书；后续 Play 上架需要新证书，用户无法直接从该 APK 覆盖更新到 Play 版。
2. **没有真人 / 真实账单验证**。全部验证基于合成 fixture 与仪器化测试；生产正确性
   （confirmed false positive = 0）尚待真实数据 Pilot。
3. **Windows 未签名**：`WINDOWS_CODE_SIGNING = BLOCKED_BY_MISSING_CODE_SIGNING_CERTIFICATE`。
   下载/运行时 Windows 可能显示 **SmartScreen 警告**；请按 SHA-256 校验文件后再运行。
4. **GitHub CI 被外部阻断**：GitHub 账户计费问题（E-10）导致 Actions 无法调度，
   记录为 `CI_EXTERNAL_BLOCKED`；本 Release 的全部 Gate 与回归均为**本地真实运行**通过。
5. **HarmonyOS / iOS 不在 v0.1.0**：Harmony N3 暂停、iOS N4 未进入（见 `NATIVE_MIGRATION_STATUS.md`）。
6. **项目 License 未定案**（`LICENSE_DECISION.md`，TBD）：下载即代表接受当前
   "all rights reserved" 默认状态，直至用户明确选择 License。
7. Desktop 的 candidate/drift **生成**管线未实现（消费既有数据，生成留给统一发现管线）。

## 工程状态（诚实口径）

| Gate | 值 | 证据 |
| --- | --- | --- |
| Quality Gate | PASS | 10 counter 全 0，`node scripts/quality/check-quality.mjs` exit 0 |
| Canonical Conformance | 91/91 | `:conformance:run` pass=91 fail=0（本地实跑） |
| Android JVM | 71/71 + 9/9 | `:core:test`、`:app:testDebugUnitTest`（本地实跑） |
| Android API36 仪器化 | 59/59 | `connectedPreviewDebugAndroidTest`（AVD pdig36，0 crash） |
| Desktop JVM + smoke | 9/9 + 14/14 | `:app:test`、`:app:run --args="--smoke"` |
| REAL_DEVICE_VERIFIED | 未宣称 | 无真机；API36 AVD 证据已齐（E-1 仍 BLOCKED） |

## 下一步（由用户决定）

0.1.0 发布后**停止**；后续方向由用户选择：0.1.x 质量迭代、Android Play Production、
Harmony N3 恢复、或真实数据 Pilot。
