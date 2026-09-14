# MVP03_MIGRATION_MATRIX.md — 迁移矩阵（Freeze §56–§61）

> 全部为真实测试证据（2026-09-13 Freeze 轮实跑）。测试文件：
> `tests/repository/migration-v3.test.ts`（MIG3）、`tests/repository/migration.test.ts`（T1–T10）、
> `tests/integration/graph-payload-v2.test.ts`（J0–J5d）。

## DB Schema 迁移

| 场景                                                            | 结果 | 证据              |
| --------------------------------------------------------------- | ---- | ----------------- |
| fresh → v3（3 新表 + 索引）                                     | PASS | MIG3-001          |
| DB v2 → v3（数据/ID 零漂移）                                    | PASS | MIG3-002          |
| v2 → v3 → restart                                               | PASS | MIG3-003          |
| migration ×50（含有数据库）                                     | PASS | MIG3-005 + T4/T4b |
| injected failure → rollback（停在 v2，可重试）                  | PASS | MIG3-004 + T8     |
| future schema（99）reject                                       | PASS | MIG3-006          |
| v1 → v2 → v3 全链数据保全（legacy 指纹/evidence/proposal 归属） | PASS | T2/T3/T5/T9       |

## Payload 迁移（容器 DEPMAP_CONTAINER_V1 全程不变）

| 场景                                                                                    | 结果 | 证据                                          |
| --------------------------------------------------------------------------------------- | ---- | --------------------------------------------- |
| payload v1 → (v1→v2→v3 in-memory) → v3 import                                           | PASS | J5c / J2c                                     |
| payload v2 → (v2→v3 in-memory) → v3 import                                              | PASS | J5 / J5b                                      |
| payload v3 → v3 roundtrip（byte-equal / 幂等）                                          | PASS | J0/J1/J1b                                     |
| graph_revision 随 meta 往返保持                                                         | PASS | J5d                                           |
| payloadVersion 0/4/99 reject（DB 保持原样）                                             | PASS | J3                                            |
| payloadKind 不匹配 / 非 JSON / 缺表 / 缺列 reject（原子）                               | PASS | J3c/J4/J4b/J4c                                |
| future schemaVersion（>3）reject                                                        | PASS | J3b                                           |
| Golden Vector 复现 + wrong-byte fail（Argon2id/AES-GCM/JCS/salt/nonce/tag/Base64 不变） | PASS | crypto/depmap.test.ts + container-mutation F6 |

## Crypto / 应用 Schema 版本分离

- DEPMAP formatVersion = 1（container）≠ 应用 schemaVersion = 3（payload/business tables）。
- Freeze 轮未触碰任何 crypto 常量或 Golden Vector（grep 证据：crypto 目录零 diff）。
