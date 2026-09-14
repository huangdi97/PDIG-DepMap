# SOURCE_INSTANCE.md（MVP02）

> 状态：IMPLEMENTED + TESTED。实现：`core/src/repositories/source-instance-repository.ts`
>
> - `core/src/domain/source.ts`；schema：`core/src/schema/migrations.ts`（v2 表 1）。

## 1. 概念

- **Adapter**（如何解析）：无状态策略，如 `generic_csv`。
- **SourceInstance**（哪个来源）：用户具体的一张卡 / 一个账单导出，持久化实体。

同一 Adapter 可以有任意多个 Instance（两张卡的 CSV 是两个 Instance）；
同一笔交易号出现在两个 Instance 中**不是**重复。

## 2. 表结构（schema v2 `source_instances`）

| 列                                               | 说明                                                                        |
| ------------------------------------------------ | --------------------------------------------------------------------------- |
| id                                               | PK（可显式指定或 UUID）                                                     |
| adapter_id / adapter_version                     | 产出自该实例的 Adapter 及版本                                               |
| source_kind                                      | CHECK：statement_file / platform_export / open_banking / manual / discovery |
| provider_id / account_node_id                    | 可选机构 / 绑定的账户节点                                                   |
| label / country / jurisdiction / currencies_json | 展示与元数据                                                                |
| state                                            | CHECK：active / retired                                                     |
| created_at / updated_at / last_ingested_at       | 时间戳                                                                      |

**无任何秘密字段**（无账号 number / credential / token）。

## 3. Repository API

`create / getById / getExisting / listByAdapter / listAll / retire /
touchIngested / bindAccountNode / countAll`

关键语义：

- `retire`：只改 state；**保留全部 provenance**（该实例的 fingerprint /
  evidence 不迁移、不删除，供审计与历史解释）。
- `touchIngested`：每次 finalize 更新 `last_ingested_at`。
- `bindAccountNode`：为 legacy WeChat 实例等补充账户节点绑定
  （funding_source 路由起点），只写 account_node_id。

## 4. Legacy 迁移

v1→v2 迁移创建**确定性** legacy 实例 `legacy-wechat-statement`
（id 固定 + `WHERE NOT EXISTS` 条件插入 ⇒ ×50 幂等不重建），并把全部 v1
fingerprints / evidence / import_sessions 回填归属到它（无孤儿引用）。

## 5. 不变量与测试证据

- 同 adapter 多实例并存 → `source-instance-scope.test.ts`
- 同 txn id 跨实例 ≠ duplicate → 同上 + multi-source E2E
- retired 保留 provenance → retire 用例
- lastIngestedAt 更新 → touchIngested 用例
- 无 secret 字段 → 表结构审查 + secret scan PASS
- 迁移幂等 ×50（空库 / 有数据库 / 重启后）→ migration.test.ts T3/T4/T4b
