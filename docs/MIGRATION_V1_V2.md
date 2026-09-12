# MIGRATION_V1_V2.md（MVP02）

> 状态：IMPLEMENTED + TESTED。两层迁移：**DB schema v1→v2**（持久化库）与
> **.depmap payload v1→v2**（in-memory，不触碰 DB）。DEPMAP_CONTAINER_V1 容器
> 协议不变（Argon2id v19 / AES-256-GCM / RFC 8785 JCS AAD / Golden Vector）。

## 1. DB schema 迁移（core/src/schema/migrations.ts）

- `MIGRATIONS = [{version:1}, {version:2}]`；`meta.schema_version` 顺序推进。
- 每个 migration 在**单事务**内执行：任何 statement 失败 → 整体回滚，
  不留半成品（注入失败回滚用例覆盖）。
- 幂等：`getSchemaVersion` 判断后跳过已应用版本；在已迁移且已装载数据的库上
  连续 ×50 逐字段零漂移（T4）；进程重启后再 ×50 仍 no-op（T4b）。
- v1 数据保全：
  - fingerprints/evidence/import_sessions 全部回填归属
    `legacy-wechat-statement`（无孤儿 SourceInstance 引用）；
  - proposals 的单条 `evidence_id` 迁入 `proposal_evidence_refs`（T6）；
  - Dependency/Group **ID 与行不丢失**，verificationBasis 回填
    `user_confirmed`；
  - legacy 去重语义保持：v1 `UNIQUE(source,fingerprint)` 移除，新作用域
    `(source_instance_id, fingerprint_version, fingerprint)` 生效（T5）。
- 迁移后重启：close → reopen → 读回一致（T4b + restart 用例）。

## 2. .depmap payload 迁移（core/src/services/graph-serialize.ts）

- `migratePayloadV1toV2(payload)`：v1 payload **in-memory** 迁到 v2 ——
  提升 `payloadVersion`、生成 legacy SourceInstance 归属、补
  `source_instance_id`/`adapter_id`/`adapter_version`/`evidence_kind`、
  `evidence_id` 内联 → `proposal_evidence_refs`。**不写 DB**。
- 导入路径按 `payloadVersion` 分派：v1 → migrate → v2 import；v2 直通；
  其他版本 fail-closed（拒绝，不猜测）。
- 容器层（`DEPMAP_CONTAINER_V1`）Golden Vector / 错误口令 / tamper 负向
  测试全部保持 PASS —— 协议未变，仅 payload 内容升级。

## 3. 测试证据

- DB：migration.test.ts（17 用例，T1–T6 + T4b + 回滚 + 重启）
- payload：tests/integration/graph-payload-v2.test.ts（17 用例：
  v2 往返 / ×幂等 / 原子失败 / v1 迁移（J2，即 T10 路径）/ 不支持版本拒绝 /
  错误口令与 tamper 回归；变异测试验证非空）
