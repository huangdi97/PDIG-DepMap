# SECURITY_MODEL.md — 安全与隐私模型

## 威胁模型

整张依赖图是敏感数据（暴露主邮箱/手机号/银行关系/支付路径/恢复结构/单点依赖）。
按“泄露后果”决定防护，而非数据量。

## 静态数据

| 平台 | 数据库 | 密钥保护 | 启动锁 |
|---|---|---|---|
| Android | SQLCipher 全库加密 | AndroidKeyStore AES-GCM 包裹 DB key | BiometricPrompt + DEVICE_CREDENTIAL |
| iOS | SQLCipher 全库加密 | Keychain（ThisDeviceOnly） | LocalAuthentication |
| HarmonyOS | ArkData relationalStore 加密(S4) | HUKS | 官方用户认证（FACE/FINGERPRINT/PIN） |

- 禁止：明文库、明文备份、自制 6 位 PIN 作为密钥根、密钥进源码/日志/普通配置。
- 错误密钥/认证取消 → 打开失败，绝不降级明文。
- 备份排除：Android `allowBackup=false`；iOS `isExcludedFromBackup`。

## `.depmap` 备份

见 `CRYPTO_PROTOCOL.md`。口令派生加密；导出时间显示在首页。

## 账单数据红线

- 原始账单仅在导入会话内存处理，无副本落盘
- Observation / CanonicalEvent 会话结束销毁，**不持久化任何单笔交易**
- 只持久化：Fingerprint（HMAC 后）、Evidence Summary、Proposal 状态、用户确认后的图实体、ImportSession 统计
- UI 明确告知用户源文件不被长期保存

## 日志红线（开发模式同样遵守）

禁止输出：raw CSV 行、完整用户对象、source transaction id、密码、SQLCipher secret、
HUKS/Keychain material、fileEncryptionKey、fpSecret、解密后的 depmap、真实账单内容。

## 网络

MVP：NO BACKEND / NO ACCOUNT / NO ANALYTICS / NO TELEMETRY / NO ADS / NO CLOUD SYNC。
Android manifest 无 INTERNET 权限；iOS 无后台模式；HarmonyOS 最小权限。

## 应用层

- Android `FLAG_SECURE`；iOS 切后台遮罩；HarmonyOS 隐私窗口
- 首页显示最近一次主动导出备份时间
