# PDIG Security Policy

三端统一安全策略。平台实现不同，**产品语义与禁止项相同**。

---

## 1. 密钥存储（Key Storage）

| 平台      | 存储介质                | 用于                                       |
| --------- | ----------------------- | ------------------------------------------ |
| Android   | Android Keystore        | DB key、fileEncryptionKey 的包装与保护     |
| HarmonyOS | HUKS                    | 同上                                       |
| iOS       | Keychain                | 同上                                       |

**禁止**：

- 硬编码密钥、密钥写入源码 / 日志 / 普通配置文件
- 明文数据库、明文备份
- 用自制 6 位 PIN 作为数据库密钥根
- 把 `.depmap` 的 `fileEncryptionKey` 持久化到普通存储

**要求**：

- 敏感密钥必须经平台密钥库保护；Keystore/Keychain/HUKS 不可用时
  → `key_unavailable`，**fail closed**，不降级为明文。

---

## 2. 数据库锁（DB Lock）

- 数据库必须以 SQLCipher（Android / iOS）或 ArkData 加密能力（HarmonyOS）加密。
- Key 由平台密钥库托管；Key 丢失 → 无法打开（这是**正确行为**，不是缺陷）。
- **禁止**任何"密钥丢失后回退明文库"的兜底逻辑。

---

## 3. 备份（Backup）— `.depmap`

- 协议：`DEPMAP_CONTAINER_V1`，**冻结**。见 `spec/security/depmap-container-v1.json`。
- 用户必须明确知道：
  1. 备份是加密的；
  2. **密码不可找回**；
  3. 文件可跨设备导入。
- 解密顺序固定：parse → structure → bounds → Argon2id → GCM auth。
- **认证前 header 一律不可信**；bounds 必须在 KDF 之前完成。

**禁止**：

- 自研 Argon2 / AES-GCM / CSPRNG 原语。只用成熟、可审计的实现。
- 静默修改 V1 协议；协议变化必须升 `formatVersion`。

---

## 4. 认证（Auth / App Lock）

三端产品语义统一（平台实现不同）：

| 状态        | 语义                                                       |
| ----------- | ---------------------------------------------------------- |
| 取消        | `auth_cancelled` → 停留在锁屏，不进入应用                   |
| 失败        | `auth_failed` → 允许重试；超过策略上限必须重新走设备凭据    |
| 不降级      | 生物认证不可用时，**不得**跳过锁直接进入                    |
| 后台/恢复   | 回到前台需按策略重新校验（默认立即）                        |

**禁止**：把生物认证失败当作成功；把"设备无生物能力"当作"无需锁"。

---

## 5. 日志（Logging）

Release 构建 **禁止**输出：

- raw statement row / 单笔交易
- password / 派生密钥 / `fileEncryptionKey` / `fpSecret`
- SQLCipher secret / Keystore / Keychain / HUKS 材料
- 解密后的 `.depmap` 明文
- 完整 Graph dump
- 真实账单内容

允许输出（Native Diagnostics，§242）：

- app version、schema version、error code、platform、counts

开发模式同样遵守本节。

---

## 6. 网络（Network）

**MVP 默认全部为 0**：

| 能力          | 值  |
| ------------- | --- |
| business network | 0 |
| analytics     | 0   |
| ads           | 0   |
| telemetry     | 0   |
| cloud sync    | 0   |
| account       | 0   |
| backend       | 0   |

**禁止**因 Native 重写引入 Retrofit / Alamofire / 任何网络 SDK
（除非产品明确需要，且需另开决策记录）。

**禁止**引入第三方 Crash Analytics；本地 diagnostics 即可。

---

## 7. 权限最小化（Permission Audit）

三端分别审计，**默认不申请任何权限**。

- 文件导入：使用系统文件选择器（SAF / 文档选择器 / 文件选择 API），
  **不申请**存储读写权限。
- 生物认证：仅使用平台认证 API，不申请额外权限。
- 相机、通讯录、短信、定位、电话：**一律不申请**。

每个平台必须产出 `docs/<PLATFORM>_PERMISSION_AUDIT.md`。

---

## 8. 截图保护（Screenshot Protection）

- 对敏感页面（锁定页、备份密码页、导入预览）按平台能力实现遮挡/禁止截图。
- **不得**因启用保护导致页面不可用或崩溃；能力缺失时降级为普通页面并记录。

---

## 9. 敏感数据生命周期

- 导入结束：raw file bytes、parsed rows、Observations 的引用必须释放。
- 锁屏/后台：内存中的明文敏感对象应尽快释放。
- 禁止把明文字符串长期驻留在全局单例中。

---

## 10. 依赖审计（Dependency Audit）

每个平台新增依赖必须记录：library / version / license / maintenance / usage。
产出 `docs/NATIVE_CRYPTO_DEPENDENCY_AUDIT.md`。

**优先平台 SDK**，避免大型第三方框架（§238）。
