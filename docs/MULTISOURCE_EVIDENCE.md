# MULTISOURCE_EVIDENCE.md（MVP02）

> 状态：IMPLEMENTED + TESTED。实现：`core/src/repositories/fingerprint-repository.ts`
> / `evidence-repository` / `proposal-repository` /
> `core/src/services/import-coordinator.ts`。

## 1. 模型

一条 `DependencyProposal` 的证据 = **多条 Evidence 流**（每个 SourceInstance
至多一条，`UNIQUE(proposal_key, source_instance_id)`），Proposal 通过
`proposal_evidence_refs` 持有 `evidenceRefs[]`（position 稳定）。

每条流独立记录：

- provenance：source_instance_id / adapter_id / adapter_version / evidence_kind
- 计数：observation_count（**不跨流相加**）
- 时间范围：first_observed_at / last_observed_at

## 2. 禁止的自动决策（Precision > Recall）

多个来源都看到同一逻辑关系，也**不得**自动：

- 设 `required`
- confirm Proposal（确认只能来自用户）
- 生成 Group / backup 断言
- 触发 must_change

重提（rejected → 重新 pending）阈值必须由**单一 evidence 流自足满足**
（per-stream `rejected_at_stream_counts_json`），不允许多源计数凑数。

## 3. 指纹命名空间隔离

- DB 去重键：`(source_instance_id, fingerprint_version, fingerprint)`
- 稳定交易号：`HMAC(fpSecret, adapterId:sourceInstanceId:sourceTxnId)`
- ⇒ 同实例同交易 = duplicate；不同实例同交易号 = 两条独立证据记录。

## 4. Coverage 正确性（event_stream）

三个文件 Adapter 均为 `event_stream`：**absence 无否定语义**。
已验证行为（tests/integration/coverage-semantics.test.ts）：

- 旧确认 Dependency 存在 + 新导入不提及 → Dependency 保持 active；
- absence 不 retire Dependency / 不 reject Proposal / 不 fail Group /
  不产生 must_change。

## 5. 确认与 verificationBasis

用户确认一次 → 一条 Dependency，`verificationBasis=user_confirmed`
（写入回读断言：multi-source-e2e.test.ts:287 + migration.test.ts:265）。
文件 Adapter 永不产生 `user_confirmed`。

## 6. E2E 证据（tests/integration/multi-source-e2e.test.ts，9 用例）

- K1：Source A（Generic CSV）+ Source B（OFX/QFX）同一逻辑关系 →
  **一个 Proposal、两条 evidence refs**；
- K1b/K1c：流 provenance/count/时间范围分离，计数不相加；
- K2：用户确认一次 → 一个 Dependency；多源不自动 required/backup；
- K3：确认 → Group（备用卡，用户确认）→ Impact 给 backup_path 而非
  must_change；
- K5：仅多源 pending、零确认 → Impact 无 must_change；
- ×20 确定性；单流自足重提阈值。
