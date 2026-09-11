# SCHEMA_V1.md — Schema v1

> 实现源：`core/src/schema/migrations.ts`（单一事实来源）；三端 DDL 与其逐语句一致
> （Android: `platforms/android/kotlin/com/depmap/core/schema/DepmapSchemaV1.kt`）。
> `schemaVersion = 1`；迁移事务化、幂等、失败回滚、重启安全。

## 表

| 表 | 说明 | 关键约束 |
|---|---|---|
| `meta` | schema_version / fpSecret / app 元数据 | key PK |
| `nodes` | 节点（kind 稳定 + templateId 扩展） | id PK |
| `dependencies` | 用户确认的依赖（存在即确认） | `UNIQUE(from_node, relation, to_node, capability)`；criticality CHECK(required,unknown)；state CHECK(active,retired)；origin CHECK(manual,proposal) |
| `dependency_groups` | 用户确认的来源组合 | `UNIQUE(group_key)`；mode CHECK(ANY,ALL) |
| `dependency_proposals` | 机器推断队列（非图实体） | `UNIQUE(key)`；decision CHECK |
| `dependency_group_proposals` | 备用路径提议 | `UNIQUE(key)` |
| `evidence` | Proposal key 级累计摘要 | `UNIQUE(proposal_key)` |
| `observation_fingerprints` | “以前处理过没有” | `UNIQUE(source, fingerprint)`；不存原文 |
| `import_sessions` | 导入会话统计 | id PK |

## 核心键

- Dependency logical key：`from|relation|to|capability`
- groupKey：`target|capability|mode|sorted(memberLogicalKeys...)`（成员乱序同组）
- GroupProposal key：`group|<canonicalGroupKey>`
- 指纹：`HMAC-SHA256(fpSecret, source:sourceTxnId)`；无单号时 canonicalRow sha256 + 文件内 ordinal `#N`；`fingerprintVersion=1`

## 生命周期不变量

1. Dependency：不存在→INSERT；active→verify 更新 lastVerifiedAt/evidence；retired→同一 id re-activate。禁止重复建边。
2. criticality：机器只能写 unknown；required 只能由用户确认产生。
3. Proposal：pending→累计；accepted→不再问；rejected→新观测 ≥3 且覆盖 ≥1 完整周期才软性重提。
4. Group：仅用户确认产生；成员集合 canonical 化；capability-scoped。
5. Evidence：只累计新 unique 指纹观测；first=min、last=max；count 只加不减。
6. Observation/CanonicalEvent：仅导入会话内存，绝不落库。

## 迁移要求（已测）

- `migrate()` 幂等（重复执行 no-op，重启重开安全）
- 每版本独立事务，失败 ROLLBACK 不留半迁移
- 数据库版本高于支持版本 → 明确报错拒绝打开
