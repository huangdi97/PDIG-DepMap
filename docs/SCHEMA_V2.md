# SCHEMA_V2.md（MVP02）

> 状态：IMPLEMENTED + TESTED。实现：`core/src/schema/migrations.ts`
> （`SCHEMA_V2_STATEMENTS`，migration version=2）。DEPMAP_CONTAINER_V1 不变，
> 仅 payload schemaVersion 允许 2（见 docs/MIGRATION_V1_V2.md）。

## 1. v2 变更总览

| # | 变更 | 目的 |
|---|---|---|
| 1 | 新表 `source_instances` | 数据源实例一等实体（active/retired） |
| 2 | 重建 `observation_fingerprints` | 去重键升级为 `UNIQUE(source_instance_id, fingerprint_version, fingerprint)`；移除 v1 `UNIQUE(source, fingerprint)` |
| 3 | 重建 `evidence` | 按 `(proposal_key, source_instance_id)` 一流一行；新增 adapter_id/adapter_version/evidence_kind |
| 4 | 重建 `dependency_proposals` + 新表 `proposal_evidence_refs` | 单条 `evidence_id` → 多条 evidenceRefs join；rejected 计数 → `rejected_at_stream_counts_json`（per-stream） |
| 5 | `dependencies` / `dependency_groups` 增列 | `verification_basis_type`（既有数据默认 `user_confirmed`）+ `verification_basis_json` |
| 6 | `import_sessions` 增列 | source_instance_id / adapter_id / adapter_version（legacy 行归属 legacy 实例） |
| 7 | 确定性 legacy WeChat 实例 | `legacy-wechat-statement` 条件插入 |

## 2. Fingerprint 作用域（唯一口径）

- DB 去重键：`(source_instance_id, fingerprint_version, fingerprint)`
- 稳定交易号指纹输入：`HMAC-SHA256(fpSecret, adapterId:sourceInstanceId:sourceTxnId)`
- ⇒ 同实例同版本同交易 = duplicate；不同实例同交易号 = 两条独立记录。

## 3. Evidence 流语义

- 一条 Proposal 的证据是**多流**（每个 SourceInstance 至多一条 evidence 行，
  `UNIQUE(proposal_key, source_instance_id)`）。
- `proposal_evidence_refs(proposal_key, evidence_id, position)` 承载
  evidenceRefs[]，position 稳定排序。
- 跨流计数**不得相加**用于自动决策（required / confirm / backup /
  re-proposal）——重提阈值必须由单一 evidence 流自足满足
  （`rejected_at_stream_counts_json`）。

## 4. verificationBasis

MVP02 仅 `user_confirmed`。既有 v1 Dependency/Group 行在迁移时统一回填
`user_confirmed`（它们本就只能来自用户确认）。CSV/OFX 等 event_stream
来源**永不**自动产生 `user_confirmed`。

## 5. Relation CHECK

`dependency_proposals.relation` CHECK 仍覆盖词表
`funding_source / merchant_agreement / verifies / recovers / bound_to`，
但 **runtime 校验**（RelationDefinitionRegistry）只放行
`funding_source / merchant_agreement`（见 docs/SOURCE_ARCHITECTURE.md 与
`core/src/domain/relation-registry.ts`）。

## 6. 测试证据（core/tests/repository/migration.test.ts，17 用例）

T1 fresh v2 / T2 v1→v2 / T3 空库 ×50 / T4 有数据库 ×50 零漂移 /
T4b 重启后 ×50 / T5 legacy dedupe 作用域 + DDL 断言 / T6 evidenceId→refs /
注入失败回滚 / 无孤儿 SourceInstance / verificationBasis 列默认值 /
MIGRATIONS=[1,2] 等。
