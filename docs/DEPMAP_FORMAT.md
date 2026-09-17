# DEPMAP_FORMAT.md

> `.depmap` 加密备份容器格式 —— **跨平台契约**。
> 规范真源：`spec/security/depmap-container-v1.json`（`status: FROZEN`）
> 本文是入口说明，完整字段以 spec 为准。

---

## 0. 契约地位

```
protocol      : DEPMAP_CONTAINER_V1
format        : depmap
formatVersion : 1
status        : FROZEN
```

> **规范原文**：
> "DEPMAP_CONTAINER_V1 — the single most important cross-platform protocol. **FROZEN.**
> Native migration MUST NOT change it."

**Android / HarmonyOS / iOS 必须使用完全相同的容器格式。**
任何一端都不得：

- 为图方便把 Argon2id 换成 PBKDF2 或其他 KDF
- 使用独立的、平台私有的容器格式
- 静默降级到旧版本或未来版本

---

## 1. 密码学参数

| 项 | 值 |
| --- | --- |
| **KDF** | **Argon2id**，version **19** |
| KDF 参数 | `memoryKiB` / `iterations` / `parallelism`（见 spec `bounds`） |
| Salt | 16 字节 |
| 派生密钥长度 | 见 spec `kdf.derivedKeyBytes` |
| 口令编码 | UTF-8，**不做 Unicode 归一化**（见 `kdf.unicodeNormalization`） |
| **对称加密** | **AES-256-GCM** |
| Nonce | 12 字节 |
| Tag | 16 字节 |
| 二进制编码 | Base64（RFC 4648，标准带填充）；`derivedKey` 仅测试用 hex |
| 序列化 | **JCS（RFC 8785）** 确定性键序 |

### 1.1 AAD

```
source       : JCS({ format, formatVersion, kdf, cipher })
excludedKeys : ciphertext, tag          ← **不进 AAD**
```

即：AAD 覆盖容器头部的元数据，**不包含密文与 tag**（tag 由 GCM 自身提供完整性）。

### 1.2 解密顺序（`decryptOrder`，5 步）

按 spec 定义的固定顺序执行，**顺序本身是契约的一部分**（顺序错误会导致
错误分类不同，例如把「口令错误」误报为「文件损坏」）。

### 1.3 边界（`bounds`）

`memoryKiB` / `iterations` / `parallelism` / `saltBytes` / `nonceBytes` / `tagBytes` /
`ciphertextMaxBytes` 均有明确上下界。
**超出边界必须快速拒绝**，不得进入 KDF 计算（否则成为 DoS 面）。

---

## 2. Golden Vector（必须逐字节一致）

```
id           : depmap-container-v1-golden
password     : depmap-test
salt         : 00112233445566778899aabbccddeeff
nonce        : a1b2c3d4e5f60718293a4b5c
plaintext    : {"app":"depmap","schemaVersion":1,"nodes":[],"dependencies":[]}
kdfParams    : memoryKiB=65536, iterations=3, parallelism=1

derivedKeyHex    : 66c4bec7f5e98856747d7b41d0a021bdc092d5e12d492852bd80647bd0ff0c86
ciphertextBase64 : KNSpbKK6waj4En3ADgeBB74H96Q7GCoBmNffeOG5QSQ9XJwx4LoJCQ0j8lEinA7GN85U6JwaVMqhAkqWDdG7
tagBase64        : 5qpABhovPbNet1q2GNEhkg==
```

**断言**：

1. `derivedKey` 与期望**完全相等**
2. `ciphertext` 与期望**完全相等**
3. `tag` 与期望**完全相等**
4. **每个平台都能打开其他任何平台导出**的本向量

> 口令 `depmap-test` 是 **synthetic 测试常量**，属跨平台契约的一部分，不是凭据。

---

## 3. UTF-8 语义

`utf8TestVectors`（5 个）约束：

- 口令按 **UTF-8 字节**处理
- **不做 Unicode 归一化**（NFC/NFD 不同 → 派生密钥不同，这是预期行为）
- 三端必须给出一致结果

---

## 4. Payload 与迁移

- `payload.schemaVersionIndependent`：容器格式**不依赖** payload 的 schema 版本
- payload v1 / v2 **可迁移**
- **未来版本必须 reject**，不得静默降级

---

## 5. 兼容性与实现约束

| 约束 | 说明 |
| --- | --- |
| **禁止自研 Argon2 / AES 原语** | 必须链接经过审计的实现（如 libargon2 / BouncyCastle / CryptoKit） |
| **Argon2id 是 Compatibility Gate** | 不是"实现建议" |
| 三端 derivedKey 必须一致 | 只比对 Golden Vector 的 `derivedKeyHex` 即可判定 |
| 未通过 Golden 不得继续 | 容器/迁移/JCS 全部依赖它 |

### 5.1 当前各端状态（2026-09-17）

| 平台 | 状态 |
| --- | --- |
| Android | ✅ 已实现并验证（BouncyCastle Argon2id + JDK JCE） |
| Legacy Node/TS | ✅ 参考实现（oracle） |
| **HarmonyOS** | ❌ **`HARMONY_DEPMAP = BLOCKED`** —— `cryptoFramework` 的 KDF 只有 PBKDF2 / HKDF，**无 Argon2**；正在评估 NDK + libargon2 路径（见 `HARMONY_ARGON2_FEASIBILITY.md`） |
| iOS | ❌ 未实现（`BLOCKED_BY_MACOS`，N4 未开工） |

---

## 6. 相关文档

- `spec/security/depmap-container-v1.json` — **规范真源**
- `spec/security/security-policy.md`
- `docs/CRYPTO_PROTOCOL.md` — 密码学协议详解
- `docs/SECURITY_MODEL.md`
- `docs/IOS_RELEASE_HANDOFF.md` §3 — iOS 侧待办
- `HARMONY_ARGON2_FEASIBILITY.md` — Harmony Argon2 路径评估
