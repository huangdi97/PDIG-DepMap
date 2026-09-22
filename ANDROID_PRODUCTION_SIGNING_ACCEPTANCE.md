# ANDROID_PRODUCTION_SIGNING_ACCEPTANCE.md

> 生成时间：2026-09-22（ANDROID_CANONICAL_FREEZE → Production/Reality Closure 轮）
> 配套：`ANDROID_PRODUCTION_SIGNING_RUNBOOK.md`（步骤手册，本文件为验收口径）
> 当前事实：**`ANDROID_SIGNING_READY = BLOCKED_BY_MISSING_PRODUCTION_KEYSTORE`**
> —— 不以 debug / non-prod key 冒充 production signing（AGENTS §19 / §24 / Goal §10）。

---

## 0. 验收总述

本文件定义「用户提供生产 keystore 后，如何按确定性步骤完成并**验证**生产签名」的完整验收流程与证据清单。
当前阶段（无 keystore）只验证**流水线本身可用**（non-production key 签名 + apksigner verify 通过），
**不产生、不宣称**生产签名产物。

```text
NON_PRODUCTION_TEST_SIGNING   = PASS（pdig-nonprod.jks 本地签名 → apksigner verify 通过）
PRODUCTION_SIGNING            = BLOCKED_BY_MISSING_PRODUCTION_KEYSTORE
PRODUCTION_READY_SIGNED_APK   = 无（当前 release 产物未签名，如实记录）
```

---

## 1. 前置输入（用户/外部提供）

| # | 输入 | 要求 | 说明 |
|---|------|------|------|
| K-1 | 生产 keystore（`.jks` / `.keystore`） | 发布负责人生成并安全保管 | 建议 RSA 4096；**不提交 Git**（`.gitignore` 已覆盖 `*.keystore` / `signing/`） |
| K-2 | storePassword | ≥20 位随机，独立 | 密码管理器 + 离线纸质副本 |
| K-3 | keyAlias | 与 keystore 一致 | 建议 `pdign`（示例） |
| K-4 | keyPassword | ≥20 位随机，独立 | 可与 storePassword 相同或不同（建议独立） |
| K-5 | 正式 applicationId | 见 ANDROID_RELEASE_IDENTITY_DECISION.md R-1 | 签名与包名在上架前必须一致决定 |

> keystore 生成命令样例见 RUNBOOK §1.1。**不替用户生成正式 key、不把任何凭据写入仓库。**

---

## 2. 签名链路验收步骤（确定性，收到 keystore 后照做）

以下每步都有可复核的产出。全部在 `ANDROID_PRODUCTION_SIGNING_RUNBOOK.md` §2/§3 有对应命令。

| # | 步骤 | 命令/动作 | 验收证据 |
|---|------|-----------|----------|
| 1 | 注入凭据 | 环境变量 `PDIG_PROD_KEYSTORE_PATH / _PASSWORD / _ALIAS / _KEY_PASSWORD`（或本地 `gradle.properties`，不提交） | 无凭据进入 Git；`git status` 无新增 secret 文件 |
| 2 | 构建签名 APK | `cd android && PDIG_* ... ./gradlew :app:assembleRelease -PpdigProdSigning=true` | 产出 `app-release.apk`；缺失任一值则构建 `error()`（fail closed） |
| 3 | apksigner verify | `"$ANDROID_HOME/build-tools/34.0.0/apksigner" verify --print-certs app/build/outputs/apk/release/app-release.apk` | `Verifies` 通过；Signer #1 DN = 生产主体（**非** `CN=Android Debug` / `androiddebugkey`） |
| 4 | 证书指纹捕获 | `keytool -list -v -keystore <keystore> -alias <alias>`（SHA-256 指纹） | 指纹记录到发布登记表（`NATIVE_RELEASE_MATRIX.md` §4） |
| 5 | 构建签名 AAB | `PDIG_* ... ./gradlew :app:bundleRelease -PpdigProdSigning=true` | 产出 `app-release.aab`（上传 Play 用；Play App Signing 下由 Play 再签 APK） |
| 6 | 安装签名 APK | `adb install -r app-release.apk`（真机或 AVD） | `Success` |
| 7 | release-mode smoke | 冷启动 → App Lock 解锁 → 首页渲染 → 一个场景 Setup→Impact→Plan | 无 crash / 无 ANR；功能闭环 |
| 8 | 产物 SHA256 | `Get-FileHash app-release.apk/aab -Algorithm SHA256` | 哈希登记进发布登记表 + 最终报告 |

---

## 3. 验收门槛（Definition of Done for signing）

- [ ] 生产 keystore 文件**从未**出现在 Git 历史 / 工作树（`git log --all --oneline -- '*.jks'` 为空 + `check-secrets.mjs` PASS）
- [ ] `apksigner verify` 输出 Signer #1 为生产证书（非 debug）
- [ ] 证书 SHA-256 指纹已捕获并登记
- [ ] 签名 APK 可安装（真机或 AVD）
- [ ] release-mode smoke 通过（冷启动 + 解锁 + 首页 + 一个场景闭环）
- [ ] APK/AAB 的 SHA256 已登记
- [ ] 三个口令 + keystore 双备份完成（密码管理器 + 离线介质×2）

> 任一项不满足 → `ANDROID_SIGNING_READY` 不得置为 PASS。

---

## 4. 反模式（禁止，见 AGENTS §19 / §24）

| 反模式 | 为什么禁止 |
|--------|-----------|
| 用 debug keystore（`~/.android/debug.keystore` / `androiddebugkey`）签 release | debug key 是公开已知的弱凭据；Play 上架后不可更换签名 → 灾难 |
| 用 `local_private/build-chain/pdig-nonprod.jks` 冒充生产 key | 该 key 仅用于验证流水线本身，从未也不应作为发布身份 |
| keystore / 口令提交 Git 或 CI 日志 | 永久泄露；密钥泄露只能换 applicationId 全新发布 |
| CI job 在 PR 上自动签名 | 会泄露 keystore 到不可信上下文；只在 main + secrets 齐全时签（RUNBOOK §3） |
| `continue-on-error` 掩盖签名失败 | 失败必须 fail（RUNBOOK §2 fail closed 原则） |

---

## 5. 备份 / 轮转 / 恢复

| 项 | 结论 |
|----|------|
| 备份 | keystore + 三口令 → 密码管理器 + 离线加密介质 ×2；**发布后丢失 = 无法更新应用** |
| 轮转 | Android 签名密钥**不可轮转**（固定）；密钥泄露只能换 applicationId 全新发布 → 保管优先级最高 |
| 恢复（启用 Play App Signing 时） | 上传密钥丢失可向 Play 申请重置；签名密钥由 Google 托管（详见 PLAY_APP_SIGNING_DECISION.md） |

---

## 6. 状态

```text
ANDROID_SIGNING_READY = BLOCKED_BY_MISSING_PRODUCTION_KEYSTORE
  流水线本身：已验证可用（non-production 签名 + apksigner verify PASS，见 ANDROID_PRODUCT_FINAL_ACCEPTANCE.md §8）
  阻塞类别：PRODUCTION_KEY_REQUIRED
  必需外部输入：生产 keystore + 三口令（K-1..K-4）
  解除流程：本文件 §2 步骤 1–8 顺序执行
  关闭证据：§3 全部勾选
```
