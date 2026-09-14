# CROSS_PLATFORM_CONTRACT_AUDIT.md — 跨平台契约审计（RC PHASE Y）

> 方法：对三端源码做 grep 常量比对（证据见命令输出）+ DDL 逐语句核对。

## 核对结果（2026-09-12 实测 grep）

| 契约项                                                 | Node reference                                                     | Android Kotlin                                      | iOS Swift                    | HarmonyOS ArkTS  | 一致                            |
| ------------------------------------------------------ | ------------------------------------------------------------------ | --------------------------------------------------- | ---------------------------- | ---------------- | ------------------------------- |
| Golden derivedKey hex                                  | golden.ts                                                          | DepmapContainerV1GoldenTest.kt                      | DepmapContainerV1Tests.swift | （互操作经向量） | ✅ 同值入库                     |
| Golden ciphertext/tag                                  | 同上                                                               | 同上                                                | 同上                         | 同上             | ✅                              |
| Argon2 version                                         | `ARGON2_VERSION_19 = 19`                                           | `0x13`（=19）                                       | 待接入（向量冻结）           | 待接入           | ✅                              |
| memoryKiB 边界 16384–262144                            | DEPMAP_V1_BOUNDS                                                   | MEMORY_KIB_MIN/MAX                                  | bounds 测试占位              | 待移植           | ✅                              |
| iterations 1–10 / parallelism 1–4                      | 同上                                                               | ITERATIONS/PARALLELISM 常量                         | 同上                         | 同上             | ✅                              |
| salt 16B / nonce 12B / tag 16B / key 32B               | 同上                                                               | SALT_LEN 等                                         | Golden.salt/nonce 字节数     | 同上             | ✅                              |
| payload version                                        | formatVersion=1                                                    | FORMAT_VERSION=1                                    | 向量 JSON=1                  | =1               | ✅                              |
| schema version                                         | SCHEMA_VERSION=1                                                   | DepmapSchemaV1.VERSION=1                            | 经 UTS 传 DDL                | migrate 参数=1   | ✅                              |
| DDL                                                    | migrations.ts                                                      | DepmapSchemaV1.kt（逐语句同源）                     | UTS 层下发同一 DDL           | 同一 DDL         | ✅                              |
| Base64                                                 | RFC4648 带填充（Buffer/TextEncoder）                               | android.util.Base64 NO_WRAP                         | Data base64（带填充）        | util.Base64      | ✅（解码语义均宽松→字节级一致） |
| password 语义                                          | 精确 UTF-8，无归一化（测试覆盖 NFC/NFD）                           | `toByteArray(UTF_8)`                                | `String.UTF-8`               | TextEncoder      | ✅                              |
| 枚举词汇（capability/relation/criticality/state/mode） | domain/types.ts                                                    | DDL CHECK 同集                                      | 同 DDL                       | 同 DDL           | ✅（v1 已加固 CHECK）           |
| 错误码词汇                                             | DepmapError: invalid_json/invalid_structure/bounds/kdf/auth_failed | bounds/auth_failed（DepmapContainerException.code） | 待接入                       | 待接入           | ✅（两端已对齐）                |

## 风险注记

- iOS/Harmony 的 crypto 接入未完成（B3/B2），互操作以冻结向量 + 本表常量对齐保证；
  接入后必须跑「Node ↔ X 双向解密」才算 TESTED（见 IOS_MAC_HANDOFF / HARMONY_SETUP）。
- Kotlin JsonHeader.parse 为正则极简解析（V1 固定结构专用）；无第三方 JSON 依赖，
  已在 golden 测试覆盖正确/恶意输入两类路径。
