# SCHEMA_V2_MIGRATION_SPEC.md

## 目标

安全升级到 global-source-ready Schema v2。

## 新增/修改

### SourceInstance

字段至少：id、adapterId、adapterVersion、sourceKind、providerId?、accountNodeId?、label、country?、jurisdiction?、currencies、state、createdAt、updatedAt、lastIngestedAt?。

### ObservationFingerprint

从 `UNIQUE(source, fingerprint)` 改为：
`UNIQUE(sourceInstanceId, fingerprintVersion, fingerprint)`。

### EvidenceSummary

增加：proposalKey、sourceInstanceId、adapterId、adapterVersion、evidenceKind、firstObservedAt、lastObservedAt、observationCount、lastImportSessionId。

### DependencyProposal

单 `evidenceId` 升级为 `evidenceRefs[]`；SQL 中可用 join table，不强制 JSON array。

### VerificationBasis

Dependency / Group 增加 verification basis；既有数据迁移为 `user_confirmed`。

### ImportSession

增加 sourceInstanceId / adapterId / adapterVersion。

## Legacy WeChat

创建 deterministic legacy SourceInstance，所有 v1 微信 fingerprint/evidence 归属它。重复 migration 不得创建第二个 legacy source。

## Invariants

- transactionally migrate
- rollback on failure
- idempotent
- no raw data introduced
- Dependency/Group ID 不变
- Proposal decision 不丢
- Fingerprint dedupe 不丢
- DEPMAP_CONTAINER_V1 不变

## Payload

v1 payload 解密后可 in-memory migrate 到 schemaVersion=2，再 validate/import。
