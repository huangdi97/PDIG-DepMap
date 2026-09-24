# Security Policy

## 报告安全问题

**请通过 GitHub 的私有安全报告通道（Security advisories → "Report a vulnerability"）提交，
不要在公开 issue 中描述安全漏洞。**

若该通道不可用，请联系仓库所有者并明确标注 `[SECURITY]`。

### 期望收到

| 项           | 说明                                                       |
| ------------ | ---------------------------------------------------------- |
| 影响面       | 哪个平台（Android / HarmonyOS / iOS / Canonical / Legacy） |
| 版本         | commit SHA（不是"最新版"）                                 |
| 最小复现     | 可独立执行的步骤，**基于 synthetic fixture**               |
| 期望 vs 实际 | 明确的安全属性差异                                         |
| 证据         | 脱敏日志、测试输出；**不含真实个人数据**                   |

---

## ⛔ 严禁提交的内容

**不要在 issue、PR、讨论或任何公开渠道中提交：**

- 真实账单、银行流水、交易记录
- 银行卡号 / 账户号 / PAN
- 口令、Passphrase、恢复码
- `.depmap` 明文或解密后的内容
- 数据库文件（`.db` / `.sqlite`）或其 dump
- 含真实个人数据的截图、录屏
- 设备 logcat / runtime dump 原文
- 任何本机绝对路径中的用户名

**替代方式：**

- 用 **`fixtures/`** 下的 canonical 用例，或新建 synthetic fixture 复现
- 日志中把 ID、金额、账户、路径全部替换为占位符（如 `<ACCT>`、`<CARD>`、`<user>`）
- 截图用 synthetic 数据重新生成

> 本项目处理的是**个人金融基础设施数据**。
> 一条真实账单泄露的影响远大于一个普通 bug。**宁可少给信息，也不要给真实数据。**

---

## 支持范围

| 范围                                                      | 是否受理                            |
| --------------------------------------------------------- | ----------------------------------- |
| `spec/` / `fixtures/` / `conformance/`（Canonical 契约）  | ✅                                  |
| `android/`（原生实现）                                    | ✅                                  |
| `harmony/`（原生实现）                                    | ✅                                  |
| `core/` / `app/` / `platforms/`（Legacy Behavior Oracle） | ⚠️ 受理但优先级较低（不进生产路径） |
| 第三方依赖（DCloud / UTS / Gradle / ohpm 包）             | ❌ 请直接向对应上游报告             |

---

## 已知的安全设计前提（不是缺陷）

以下几点是**有意设计**，不作为漏洞受理：

| 事实                                                               | 原因                                                                   |
| ------------------------------------------------------------------ | ---------------------------------------------------------------------- |
| `ANDROID_PRODUCTION_RELEASE_READY = BLOCKED_BY_PRODUCTION_SIGNING` | 缺生产 keystore；**不是**签名逻辑缺陷                                  |
| Harmony `HARMONY_DEPMAP = BLOCKED`                                 | cryptoFramework 无 Argon2；**不得**降级为 PBKDF2，容器格式是跨平台契约 |
| Harmony / iOS 无运行时取证                                         | 无模拟器镜像 / 无 macOS；属外部 blocker，非实现缺陷                    |
| `criticality=required` 只能由用户设置                              | 机器永不产生 required，这是第一原则的一部分                            |

---

## 安全取证口径

本项目的所有安全结论**只认实跑证据**：

- 无运行时证据的项一律标 `SOURCE_READY` / `BUILD_READY`，**不标 PASS**
- 「实现存在」≠「安全属性成立」
- 检测口径本身必须先自证（历史教训：曾因 `dumpsys` 的 flag 是裸名 `SECURE`
  而误判截图保护未生效）

参考：`docs/SECURITY_MODEL.md` · `spec/security/security-policy.md` ·
`docs/CRYPTO_PROTOCOL.md` · `docs/FAIL_CLOSED_MATRIX.md` ·
`docs/PRIVACY_DATAFLOW_AUDIT.md` · `docs/NETWORK_AUDIT.md` · `docs/LOGGING_AUDIT.md` ·
`ANDROID_RUNTIME_SECURITY_EVIDENCE.md`。
