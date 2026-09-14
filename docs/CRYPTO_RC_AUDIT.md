# CRYPTO_RC_AUDIT.md — Crypto RC 审计（RC PHASE R）

> 重跑：2026-09-12 · `npm test -- tests/crypto`（28 tests）+ `tests/unit/crypto-negative`（10 tests）
> 协议：DEPMAP_CONTAINER_V1（未修改协议本体；V1 冻结纪律遵守）

## 重跑结果（全部 PASS）

- Golden Vector 复现：derivedKey / ciphertext / tag 与冻结值一致
- wrong password → auth_failed；tag tamper / ciphertext tamper → auth_failed
- header in-bounds 篡改（iterations 3→4）→ AAD 失配 → auth_failed
- malicious KDF（memoryKiB 10⁹）在 KDF 前拒绝（<200ms 断言）
- empty/long password、invalid base64、truncated JSON、bad salt/nonce/tag 长度、
  oversize（>64MiB）、unsupported format/kdf version、cipher 算法替换 → 全部 fail closed
- 容器 mutation fuzz（30 次单字符翻转）：全部「拒绝」或「语义等价 no-op」，0 错误明文

## 本轮发现与处置

1. **base64 非规范编码 no-op**（fuzz 发现）：tag/salt 末位 base64 字符的低 4 位在解码时
   被丢弃，单字符替换可产生解码字节完全相同的非规范编码 → 容器解密成功。
   **安全影响：无**（解码字节相同 = 语义恒等；AAD 校验的是规范字符串但解码后密钥/密文不变）。
   处置：V1 不改（兼容冻结）；已记录为 V2 候选加固（拒绝非规范 base64）。
2. **空口令**：hash-wasm 拒绝空口令 → 现包装为 `DepmapError('kdf')`（此前为裸 Error）。
   UX 层应在导入口令为空时直接提示，不进入 KDF。
3. **KDF 错误包装**：`deriveFileEncryptionKey` 现将所有底层错误包装为
   DepmapError('kdf')，杜绝裸 Error 逃逸（fail-closed 错误模型）。

## 审计清单

| 项                        | 状态                                                                                                  |
| ------------------------- | ----------------------------------------------------------------------------------------------------- |
| CSPRNG                    | `crypto.getRandomValues`（Node/WebCrypto CSPRNG）；Android SecureRandom；iOS SecRandomCopyBytes       |
| nonce 唯一性              | 每次 encrypt `getRandomValues(12B)`；golden 用固定 nonce（测试向量专用）                              |
| secret logging            | 无（LOGGING_AUDIT.md）                                                                                |
| authenticate-before-trust | 结构→边界→Argon2→GCM 顺序（代码顺序即校验顺序，openDepmapContainer）                                  |
| decrypt 后 payload 校验   | payload 层（graph-serialize import）在触碰 DB 前完整结构校验 + schemaVersion 检查；不支持版本明确拒绝 |
| 协议冻结                  | V1 参数未改动；仅错误类型包装（correctness 修订，RC_RULES 允许）                                      |
