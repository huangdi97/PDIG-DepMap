# DEAD_CODE_AUDIT.md — 死代码审计（Engineering Baseline V1）

> 审计时间：2026-09-13。方法：导出符号 × 全仓引用扫描（src/tests/scripts）。

## 已删除（本轮，commit ddffe74）

| 符号                                         | 位置                                         | 理由                                                               |
| -------------------------------------------- | -------------------------------------------- | ------------------------------------------------------------------ |
| `WECHAT_PARSER_ID` / `WECHAT_PARSER_VERSION` | `src/parser/wechat/parser.ts`                | 零引用；adapter 内使用独立字面量                                   |
| `assignFingerprintsFromNormalized`           | `src/fingerprint/fingerprint.ts`             | 零引用；归一化观测可直接走 `assignFingerprintsV2`                  |
| `toScopedRecords`                            | `src/repositories/fingerprint-repository.ts` | 零引用；legacy 形状兼容无调用点                                    |
| `parseCsvCells`                              | `src/sources/generic-csv/adapter.ts`         | 被 `parseCsv`（整文件、支持引号内换行）取代                        |
| `IdFactory` / `Clock`                        | `src/utils/ids.ts`                           | 未实现注入的「预留抽象」；determinism 由调用点显式传 id/时间戳实现 |
| `GOLDEN_CONTAINER_JSON`                      | `src/crypto/golden.ts`                       | 计算后即 `void`，零引用；golden 测试走 `createGoldenContainer()`   |

## 保留（有消费方或公共契约面）

| 符号                                          | 位置                              | 理由                                                             |
| --------------------------------------------- | --------------------------------- | ---------------------------------------------------------------- |
| `getRelationDefinition`                       | `src/domain/relation-registry.ts` | invariants 套件使用（INV5）；registry 的自然查询 API             |
| `assignFingerprints`（legacy 入口）           | `src/fingerprint/fingerprint.ts`  | v1 观测形状测试与兼容路径使用                                    |
| `detect()`                                    | `src/parser/wechat/parser.ts`     | parser 测试 + `canHandle` 共享签名                               |
| `BUILTIN_ALIASES`                             | `src/resolver/resolver.ts`        | resolver 契约的显式公共面                                        |
| `src/adapters/interfaces.ts` 全部平台契约接口 | —                                 | 平台桥接公共契约（Android/ArkTS/Swift 对齐目标），非 Node 侧消费 |
| `ImpactLevel` 的 `degraded`/`unaffected`      | `src/impact/kernel.ts`            | 语义表完整性所需（severity merge / checklist 过滤）              |

## 政策

- 「以后可能有用」的未引用代码一律删除；预留能力写入 `NEXT_BACKLOG.md` / `FUTURE.md`。
- 公共导出面以 `src/index.ts` 为准；新增导出必须有消费方（src/tests/scripts）或明确的平台契约理由。
