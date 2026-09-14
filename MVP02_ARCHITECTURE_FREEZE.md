# MVP02_ARCHITECTURE_FREEZE.md

## 1. 冻结目标

MVP02 只解决 **Global Source Abstraction**。
用户 Job 仍是“换银行卡前判断 payment 影响”，Impact 仍 payment-only。

## 2. 核心结构

```text
Source File / Export
→ EvidenceSourceAdapter
→ SourceInstance
→ Observation(memory)
→ Normalization
→ Fingerprint
→ Node Resolution
→ EvidenceSummary[]
→ DependencyProposal
→ Human Confirmation
→ Dependency/Group
→ Impact
→ ChangePlan
```

## 3. 新增概念

- `SourceInstance`：用户具体的数据源实例。
- `EvidenceSummary[]`：同一 Proposal 可由多个 SourceInstance 提供证据。
- `CoverageMode`：Source 的 absence 是否有语义。
- `VerificationBasis`：记录 Reality 是如何被验证的；MVP02 仍仅 user-confirmed。
- `RelationDefinitionRegistry`：治理 relation 的方向、kind、capability 与验证政策。

## 4. 永久不变

- Observation ≠ Reality
- Proposal ≠ Reality
- Two edges ≠ fallback
- Missing record ≠ relationship absent
- Machine 不得产生 required
- Group 必须确认
- Impact payment-only
- raw statements 不持久化
- local-first
- `DEPMAP_CONTAINER_V1` 不变

## 5. Cross-source 去重边界

MVP02 不实现完整全球 CanonicalEvent 去重。因此：

- 不跨 SourceInstance 粗暴相加 observationCount
- re-proposal threshold 在单 Evidence stream 内满足
- 多源证据只增加 provenance，不自动增加 certainty class

## 6. Adapter 范围

本轮：

- WeChatStatementAdapter
- GenericCsvAdapter
- OfxQfxAdapter

Future：PayPal/Open Banking/OAuth/Browser Discovery。

## 7. Schema

应用 payload `schemaVersion=2`；Crypto 外层 `formatVersion=1` 保持不变。
