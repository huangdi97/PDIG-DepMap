# PDIG Migration Spec

两条完全独立的迁移链。**不得混淆**：

| 迁移链                | 对象                        | 与另一条的关系                       |
| --------------------- | --------------------------- | ------------------------------------ |
| **Database Schema**   | 本地 SQLite（v1 → v2 → v3） | payload `schemaVersion` 必须等于当前 DB schema |
| **Payload Version**   | `.depmap` 内的逻辑图 JSON   | 与 `DEPMAP_CONTAINER_V1` 的 `formatVersion` **无关** |

`.depmap` 的 `formatVersion` 恒为 `1`，**不随应用 Schema 变化**。
（容器格式与应用数据版本解耦，这是 DEPMAP_CONTAINER_V1 的关键设计。）

---

## 1. Database Schema 迁移

### 1.1 版本

```
SCHEMA_VERSION = 3
MIGRATIONS = [1, 2, 3]
```

### 1.2 v1 → v2（MVP02 Global Source Abstraction）

| 变更                            | 说明                                                                 |
| ------------------------------- | -------------------------------------------------------------------- |
| 新增 `source_instances`         | SourceInstance 升为一等实体                                          |
| 重建 `observation_fingerprints` | `UNIQUE(source, fingerprint)` → `UNIQUE(source_instance_id, fingerprint_version, fingerprint)` |
| 重建 `evidence`                 | 按 `(proposal_key, source_instance_id)` 分流 + adapter provenance    |
| 新增 `proposal_evidence_refs`   | `dependency_proposals.evidence_id`（单值）→ join 表（多源 provenance） |
| `dependency_proposals` 重建     | 删 `evidence_id`；新增 `rejected_at_stream_counts_json`              |
| `dependencies` / `dependency_groups` | 新增 `verification_basis_type`（默认 `user_confirmed`）+ `verification_basis_json` |
| `import_sessions`               | 新增 `source_instance_id` / `adapter_id` / `adapter_version`         |
| 注入 legacy WeChat SourceInstance | id 固定 `legacy-wechat-statement`，条件插入（重复迁移不重建）        |

**降级归属规则**：所有 v1 既有的 fingerprint / evidence / proposal / import_session
一律归属 `legacy-wechat-statement`（`adapter_id = wechat_statement`，
`evidence_kind = transaction_stream`）。

### 1.3 v2 → v3（MVP03 Living Graph）

只新增表，**不修改既有表结构**：

- `change_plans`
- `reality_drifts`
- `discovery_candidates`（唯一索引 `idx_discovery_candidates_normalized_key`）

`graphRevision` **不建新表**：沿用 `meta.graph_revision`。

### 1.4 强制要求（三端一致）

| 要求                | 说明                                                                 |
| ------------------- | -------------------------------------------------------------------- |
| transactional       | 每个 version 一个事务                                                 |
| failure rollback    | 失败后停在**旧版本**，不留半迁移状态                                  |
| idempotent          | 已是最新版本时严格 no-op；对同一库重复执行 ×50 必须零漂移              |
| IDs preserved       | Node / Dependency / Group / Proposal 的 id **不变**                   |
| decisions preserved | Proposal 的 `decision` / `decided_at` / `criticality_decision` 不变    |
| evidence preserved  | Evidence 计数与时间范围不变                                           |
| groups preserved    | Group 与其成员边不变                                                  |
| sources preserved   | SourceInstance 不变；legacy 实例**只创建一次**                        |
| revision correct    | 迁移**不得** bump `graph_revision`                                    |
| future rejected     | `schema_version` 高于支持版本 → 明确 reject（`unsupported_schema`），**不得猜测兼容** |

> 关键反例：`schema_version = 99` 必须抛错并且**不修改**数据库。

---

## 2. Payload 迁移（`.depmap` 内的逻辑图）

### 2.1 版本

```
GRAPH_PAYLOAD_KIND    = depmap-logical-graph
GRAPH_PAYLOAD_VERSION = 3
migratableFrom        = [1, 2]
```

### 2.2 表集合（payloadTables）

```
meta, nodes, dependencies, dependency_groups, dependency_proposals,
dependency_group_proposals, proposal_evidence_refs, evidence,
observation_fingerprints, import_sessions, source_instances
```

> `change_plans` / `reality_drifts` / `discovery_candidates` **不进 payload**。
> `graph_revision` 通过 `meta` 行随 payload 往返。

### 2.3 v1 → v2（纯内存）

- `payloadVersion` 1 → 2；`schemaVersion` → 2
- 补 `source_instances`（legacy WeChat 实例）
- 内联的 `dependency_proposals.evidence_id` → `proposal_evidence_refs` 行
- `evidence` 补 `source_instance_id` / `adapter_id` / `adapter_version` / `evidence_kind`
- `observation_fingerprints` 补 `source_instance_id`
- `import_sessions` 补 provenance 三列
- **不触碰任何 DB**；不修改传入字符串；空图迁移仍生成 1 个 legacy 实例

### 2.4 v2 → v3（纯内存）

- `payloadVersion` 2 → 3；`schemaVersion` → 3
- `meta` 补 `graph_revision`（缺省 `0`）

### 2.5 组合迁移

`v1 → v1toV2 → v2toV3` 必须可直接被 import 消费。

### 2.6 强制要求

| 输入情形                              | 期望行为                                                |
| ------------------------------------- | ------------------------------------------------------- |
| `payloadVersion` ∈ {0, 4, 99}         | reject（`payload_version_unsupported`），目标 DB 不变    |
| `payloadVersion` 非整数               | reject（`payload_invalid`）                              |
| `schemaVersion` > 支持版本            | reject（`unsupported_schema`）                           |
| `schemaVersion` 非整数                | reject                                                   |
| `payloadKind` 不匹配                  | reject（`payload_kind_mismatch`）                        |
| 非 JSON / `'[]'` / `'null'`           | reject（`payload_invalid`）                              |
| 缺必需表                              | reject，原子替换，**DB 保持原样**                        |
| 某行缺列                              | reject，**禁止半写入**                                   |
| `migratePayloadV1toV2` 收到 v2 输入   | reject（`expects payloadVersion 1`）                     |
| `migratePayloadV2toV3` 收到 v3 输入   | reject（`expects payloadVersion 2`）                     |

**导入必须是原子替换**：任一校验失败 → 目标 DB 逐字节保持原样。

### 2.7 往返与幂等

- 导出 → 导入到新库 → 再导出：**逐字节相同**
- 对同一 payload 重复导入：幂等（逐字节不变）
- 导入后 `checkGraphIntegrity` 的 5 项孤儿检查必须全为 `[]`

---

## 3. 跨平台要求

同一 payload：

| 场景                                   | 要求                          |
| -------------------------------------- | ----------------------------- |
| Android export → Android import        | 必须 PASS                     |
| Android export → Harmony import        | 必须 PASS                     |
| Harmony export → Android import        | 必须 PASS                     |
| iOS export → Android/Harmony import    | 必须 PASS（需 macOS 才能实测） |
| Android/Harmony export → iOS import    | 必须 PASS（需 macOS 才能实测） |

环境缺失的组合可暂记 `BLOCKED_BY_MACOS`，但 **Source Contract 必须先完成**。

---

## 4. 与 Native 迁移的关系

- Native 第一版**仍然读取当前 Schema**（v3）。**不得**因为重写而降级到 v1。
- Native App 必须能读取现有 `.depmap` —— 这是 Cutover 的核心要求。
- 不要求读取旧 App 的私有 DB 文件；若旧 App 从未公开发布，通过 `.depmap` 迁移即可。
- Onboarding 必须支持"从旧 `.depmap` 恢复"。

---

## 5. Fixtures

`fixtures/migration/` 下提供匿名 synthetic fixture：

| 文件                        | 内容                                              |
| --------------------------- | ------------------------------------------------- |
| `v1-minimal.json`           | 最小 v1 payload（含内联 evidence_id）             |
| `v1-with-evidence.json`     | 带 fingerprint / evidence / proposal 的 v1         |
| `v2-sample.json`            | v2 payload（含 source_instances）                 |
| `v3-expected-from-v1.json`  | v1 → v3 的期望输出（normalized）                  |
| `v3-expected-from-v2.json`  | v2 → v3 的期望输出（normalized）                  |
| `reject-cases.json`         | 上表所有 reject 情形的输入与期望 error code        |

三端必须使用**同一份** fixture（不得各自复制一份）。
