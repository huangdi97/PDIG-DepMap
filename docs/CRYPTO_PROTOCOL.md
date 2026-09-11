# CRYPTO_PROTOCOL.md — DEPMAP_CONTAINER_V1

> 实现源：`core/src/crypto/{jcs,depmap,golden}.ts`；Android：`platforms/android/.../crypto/DepmapContainerV1.kt`
> 测试：`core/tests/crypto/depmap.test.ts`（28 项 PASS，含 Golden Vector 复现）

## 定位

**口令派生文件加密**（Password-Based File Encryption），无随机 DEK、无 envelope。
`password → Argon2id(v19) → fileEncryptionKey(32B) → AES-256-GCM → ciphertext`

## 容器（JCS canonical JSON）

```json
{"cipher":{"algorithm":"AES-256-GCM","nonce":"<b64>"},
 "ciphertext":"<b64>","format":"depmap","formatVersion":1,
 "kdf":{"algorithm":"argon2id","iterations":3,"memoryKiB":65536,"parallelism":1,"salt":"<b64>","version":19},
 "tag":"<b64>"}
```

固定参数：Argon2id v19；key 32B；salt 16B；nonce 12B；tag 16B；Base64 RFC4648 带填充；
password 精确 UTF-8 字节（不做 Unicode 归一化）；生产导出 65536/3/1。

## AAD

`AAD = UTF8(RFC8785-JCS({format, formatVersion, kdf, cipher}))`
`ciphertext`/`tag` 不进入 AAD。JCS 实现为受限值域（string/safe-int/object/array），
浮点与 bigint 直接抛错；键序 UTF-16 code unit。

## 解密前边界（不可信 header，先于 KDF）

```
format=depmap; formatVersion=1; kdf.algorithm=argon2id; kdf.version=19
memoryKiB ∈ [16384, 262144]（超出即拒绝，绝不触发 KDF）
iterations ∈ [1,10]; parallelism ∈ [1,4]
salt=16B; nonce=12B; tag=16B; 0 < ciphertext ≤ 64MiB
```

## Golden Test Vector（冻结）

```
password  = depmap-test
salt      = 00112233445566778899aabbccddeeff
nonce     = a1b2c3d4e5f60718293a4b5c
plaintext = {"app":"depmap","schemaVersion":1,"nodes":[],"dependencies":[]}

derivedKey  = 66c4bec7f5e98856747d7b41d0a021bdc092d5e12d492852bd80647bd0ff0c86
ciphertext  = KNSpbKK6waj4En3ADgeBB74H96Q7GCoBmNffeOG5QSQ9XJwx4LoJCQ0j8lEinA7GN85U6JwaVMqhAkqWDdG7
tag         = 5qpABhovPbNet1q2GNEhkg==
```

- Node reference：`core/tests/crypto` 断言复现（PASS）
- Android Kotlin：`platforms/android/test/.../DepmapContainerV1GoldenTest.kt`（TEST READY，RUN=NO）
- iOS Swift：`platforms/ios/Tests/.../DepmapContainerV1Tests.swift`（TEST READY，RUN=NO）
- 互操作标准：同一向量 + 两侧互相解密。**当前仅 Node 侧已运行**；Android/iOS 侧待工具链。

## 版本纪律

V1 不得静默变化；任何参数变化必须升级 `formatVersion`。
