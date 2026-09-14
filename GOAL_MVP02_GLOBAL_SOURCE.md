# GOAL_MVP02_GLOBAL_SOURCE.md

# PDIG / DepMap — MVP02 Global Source Abstraction Validation

> 执行模型：ZCode + GLM-5.3-Flash  
> 前置状态：MVP01 DEV CLOSEOUT / RC-AUDIT 已完成到当前环境允许的最高程度  
> 真实数据：本轮仍可保持 NOT_RUN，禁止 synthetic 冒充真实数据  
> 核心目标：证明 PDIG Core 不依赖微信字段、不依赖中国支付语义，并把 global-first 必需的数据源抽象固化进 Schema v2。

---

## 0. 启动时必须完整读取

按顺序读取：

1. `AGENTS.md`
2. `CANONICAL_DESIGN.md`
3. `MVP_ACCEPTANCE.md`
4. `MVP01_RC_AUDIT_REPORT.md`
5. `WORK_STATUS.md`
6. `BLOCKERS.md`
7. `MVP02_ARCHITECTURE_FREEZE.md`
8. `SCHEMA_V2_MIGRATION_SPEC.md`
9. `SOURCE_ADAPTER_CONTRACT.md`
10. `MVP02_TEST_MATRIX.md`
11. `MVP02_ACCEPTANCE.md`
12. 本文件

如某前置报告不存在，不得伪造；先生成 `MVP02_PRE_AUDIT.md`，基于现有 repo 继续。

---

## 1. 本轮 Thesis

MVP01 已证明：

```text
WeChat statement
→ Observation
→ Fingerprint
→ Resolver
→ Proposal
→ Confirmation
→ Dependency/Group
→ Impact
→ ChangePlan
```

MVP02 要证明：

> 相同 Core 可以处理不同国家、不同机构、不同文件格式的数据源，而无需把 Domain 改成“每接一家银行就写一套业务逻辑”。

因此本轮至少打通：

```text
WeChat Statement
Generic CSV
OFX / QFX
```

并保持同一套：

```text
Node / Evidence / Proposal / Dependency / Impact / ChangePlan
```

---

## 2. 本轮真正冻结的能力

本轮重点不是“多做两个 Parser”，而是正式冻结：

- `EvidenceSourceAdapter`
- `SourceInstance`
- source-neutral `Observation` / normalization
- multi-source Evidence provenance
- Fingerprint source scope
- `coverageMode` / `authoritativeFor`
- `verificationBasis`
- `RelationDefinitionRegistry`
- Schema v1 → v2 migration
- `.depmap` payload schemaVersion 与 crypto container formatVersion 分离

---

## 3. 明确不做

禁止进入：

- 支付宝专用 Adapter
- PayPal API / OAuth
- Plaid / Open Banking
- Gmail / Browser / App Discovery
- RealityDrift
- graphRevision / Plan Rebase
- IncidentPlan / DrillPlan
- PersonalService / ContinuityGoal
- cross-capability Impact
- access / recovery Impact
- LLM / Agent / GraphRAG
- Neo4j
- 云同步 / 后端账号
- 家庭 / 遗产
- 汇率 / 资产净值

全部放 `NEXT_BACKLOG.md`。

---

## 4. 兼容性铁律

允许升级：

```text
application payload schemaVersion: 1 → 2
```

但必须保持：

```text
DEPMAP_CONTAINER_V1
```

完全不变。

也就是说：

```text
crypto container formatVersion != application payload schemaVersion
```

不得因 Schema v2 修改 Argon2id、AES-GCM、JCS AAD、salt/nonce/tag、Base64 等协议语义，除非发现真实 correctness/security bug。

---

## 5. PHASE 0 — MVP02 Pre Audit

生成 `MVP02_PRE_AUDIT.md`，记录：

- current HEAD / branch / git status
- MVP01 RC verdict
- current schemaVersion
- current DEPMAP formatVersion
- current WeChat import structure
- current fingerprint unique key
- Proposal 当前 evidence 字段
- Evidence 当前 provenance 字段
- migrations
- test counts
- platform blockers

建议新分支：

```text
feat/mvp02-global-source
```

不要破坏 MVP01 RC baseline。

---

## 6. PHASE 1 — EvidenceSourceAdapter 正式化

建立正式接口：

```ts
type SourceKind =
  | 'statement_file'
  | 'platform_export'
  | 'open_banking'
  | 'manual'
  | 'discovery'

type CoverageMode =
  | 'event_stream'
  | 'partial_snapshot'
  | 'complete_snapshot'
  | 'user_selected'

interface EvidenceSourceAdapter {
  readonly id: string
  readonly version: number
  readonly sourceKind: SourceKind
  readonly coverageMode: CoverageMode
  readonly authoritativeFor: readonly string[]

  canHandle(input: SourceInput): Promise<number>
  parse(input: SourceInput, context: SourceContext): Promise<Observation[]>
  normalize(
    observations: readonly Observation[],
    context: SourceContext
  ): Promise<NormalizedObservation[]>
}
```

规则：

- `canHandle` deterministic
- Adapter 不创建 Dependency
- Adapter 不创建 Group
- Adapter 不产生 `required`
- Adapter 不直接改 Graph
- Adapter 不长期保存 raw statement
- 必须声明 `coverageMode`
- 必须声明 `authoritativeFor`

MVP02 三个文件型 Adapter：

```text
wechat_statement
generic_csv
ofx_qfx
```

全部：

```text
coverageMode = event_stream
authoritativeFor = []
```

因此“账单中没出现”绝不能证明现实关系不存在。

---

## 7. PHASE 2 — SourceInstance

新增一等实体：

```ts
interface SourceInstance {
  id: UUID
  adapterId: string
  adapterVersion: number
  sourceKind: SourceKind

  providerId?: string
  accountNodeId?: UUID
  label: string

  country?: string
  jurisdiction?: string
  currencies?: string[]

  state: 'active' | 'retired'
  createdAt: Instant
  updatedAt: Instant
  lastIngestedAt?: Instant
}
```

语义：

```text
Adapter = 如何解析这一类来源
SourceInstance = 用户具体的哪个数据源
```

必须测试同一个 Adapter 存在多个 SourceInstance。

---

## 8. PHASE 3 — Fingerprint Scope 修正

旧语义如为：

```text
UNIQUE(source, fingerprint)
```

Schema v2 改成：

```text
UNIQUE(sourceInstanceId, fingerprintVersion, fingerprint)
```

优先稳定 transaction id：

```text
HMAC-SHA256(
  fpSecret,
  adapterId + ':' + sourceInstanceId + ':' + sourceTxnId
)
```

无稳定 ID 时继续 canonical-row fallback。

必须测试：

1. 同 SourceInstance + 同 txn id → duplicate
2. 不同 SourceInstance + 同 txn id → 不冲突
3. fingerprintVersion 不同 → 可并存
4. duplicate import 仍安全
5. fpSecret 仍随 `.depmap` payload 迁移

---

## 9. PHASE 4 — Evidence v2：多源 Provenance

Proposal 不再只允许单一 `evidenceId`。

Schema v2：

```text
DependencyProposal.evidenceRefs[]
Dependency.evidenceRefs[]
```

Evidence 至少绑定：

```text
proposalKey
sourceInstanceId
adapterId
adapterVersion
evidenceKind
firstObservedAt
lastObservedAt
observationCount
lastImportSessionId
```

同一现实候选的不同数据源必须保留为多个 EvidenceSummary。

例如：

```text
WeChat evidence = 6
Bank CSV evidence = 5
```

不允许粗暴变成：

```text
observationCount = 11
```

---

## 10. 多源 Reproposal 的保守规则

MVP01：rejected 后 ≥3 新 observation 且跨完整周期才允许重提。

MVP02 在尚未实现完整 cross-source CanonicalEvent dedupe 前：

> Reproposal threshold 必须在单个 Evidence stream 内独立满足。

禁止：

```text
WeChat 2 + Bank CSV 2 = 4 → 直接重提
```

这是 Precision-first 约束。

---

## 11. PHASE 5 — verificationBasis

增加：

```ts
type VerificationBasis =
  | { type: 'user_confirmed'; verifiedAt: Instant }
  | {
      type: 'authoritative_source'
      sourceInstanceId: UUID
      factType: string
      verifiedAt: Instant
    }
```

MVP02 三个 Adapter 都是 `event_stream`，所以所有正式 Dependency / Group 仍然只能：

```text
verificationBasis = user_confirmed
```

禁止 CSV / OFX 自动确认 Reality。

---

## 12. PHASE 6 — RelationDefinitionRegistry

把 relation 从裸字符串升级成治理定义。

本轮 runtime 只正式支持：

```text
funding_source
merchant_agreement
```

参考：

```ts
interface RelationDefinition {
  id: RelationId
  fromKinds: readonly NodeKind[]
  toKinds: readonly NodeKind[]
  capability: Capability
  allowsGroup: boolean
  allowedGroupModes: readonly ('ANY' | 'ALL')[]
  defaultCriticality: 'unknown'
  verificationPolicy: 'user_only' | 'user_or_authoritative'
  impactSemantics: 'dependency'
}
```

Proposal、Dependency write、migration、import payload 都必须通过 registry validation。

禁止把 Future relation 词表当成已实现 runtime relation。

---

## 13. PHASE 7 — Schema v2 Migration

建立：

```text
schemaVersion = 2
```

必须支持 v1 DB → v2 DB。

至少处理：

1. SourceInstance table
2. Fingerprint sourceInstanceId
3. Fingerprint UNIQUE key
4. Evidence source provenance
5. Proposal `evidenceId` → `evidenceRefs[]`
6. `verificationBasis`
7. ImportSession sourceInstanceId / adapter provenance

### Legacy WeChat 数据

创建 deterministic legacy SourceInstance：

```text
adapterId = wechat_statement
coverageMode = event_stream
label = Legacy WeChat Statement Source
```

旧 Fingerprint / Evidence 归属它。

不得丢失现有 dedupe 能力。

---

## 14. Migration Tests

至少：

```text
T1 fresh v2
T2 v1 → v2
T3 v1 → v2 → restart
T4 migration ×50 idempotent
T5 legacy fingerprint still dedupes
T6 evidenceId → evidenceRefs
T7 invalid relation/capability rejected
T8 injected migration failure rollback
T9 no orphan SourceInstance refs
T10 old payload v1 migration path
```

---

## 15. PHASE 8 — `.depmap` Payload Versioning

保持：

```text
DEPMAP_CONTAINER_V1
```

解密后的 payload：

```json
{ "schemaVersion": 2 }
```

Importer：

```text
decrypt
→ validate payload schemaVersion
→ v1 payload: in-memory migrate to v2
→ validate v2
→ transactional import
```

要求：

- crypto Golden Vector 不变
- unsupported payload schema 明确失败
- `formatVersion` 与 `schemaVersion` 永远分开

---

## 16. PHASE 9 — GenericCsvAdapter

目标：

> 用显式字段映射接入任意结构化 CSV，而不是为每家银行写 Domain 特例。

定义 Mapping Profile，至少支持：

```text
transactionId?
dateTime
amount? / debit+credit
description?
counterparty?
currency?
balance?
transactionType?
```

以及：

```text
delimiter
encoding
dateFormats
decimalSeparator
amountSignMode
```

MVP02 只做 explicit mapping。

禁止 AI 自动映射。

允许 conservative header suggestion，但最终 mapping 必须明确。

### Fixtures

至少：

```text
us-credit-card.csv
eu-bank-semicolon.csv
multi-currency.csv
debit-credit-columns.csv
utf8-bom.csv
quoted-comma.csv
crlf.csv
cr-only.csv
duplicate-id-two-sourceinstances.csv
bad-date.csv
bad-amount.csv
missing-required-column.csv
```

---

## 17. Generic CSV 统一输出

统一输出：

```text
NormalizedPaymentObservation
```

字段至少：

```text
sourceInstanceId
adapterId
sourceTxnId? (memory/fingerprint input only)
occurredAt
amount
currency?
direction
description?
counterparty?
balance?
transactionType?
paymentMethodHint?
```

Unknown 保持 unknown，禁止猜。

---

## 18. PHASE 10 — OfxQfxAdapter

实现本地 OFX/QFX statement import。

支持 synthetic fixtures 中常见字段：

```text
FITID
DTPOSTED
TRNAMT
TRNTYPE
NAME
MEMO
```

要求：

- 本地解析
- 不联网
- 不长期保存原文件
- 输出同一 NormalizedPaymentObservation
- 未知/缺失字段保守
- OFX 来源不自动创建 Dependency

Fixtures 至少：

```text
ofx-basic.ofx
ofx-multiple.ofx
ofx-missing-fitid.ofx
ofx-invalid-date.ofx
ofx-negative-positive.ofx
qfx-basic.qfx
qfx-duplicate-fitid-two-sourceinstances.qfx
ofx-malformed.ofx
```

---

## 19. PHASE 11 — Source-neutral ImportCoordinator

把 WeChat-specific ImportFlow 重构为：

```text
SourceInstance
→ adapter
→ parse
→ normalize
→ fingerprint
→ resolver
→ evidence
→ proposal
```

WeChat 只是其中一个 Adapter。

Domain 层禁止出现：

```text
if source == wechat
```

Source-specific 逻辑全部留在 Adapter。

---

## 20. PHASE 12 — ImportSession v2

增加：

```text
sourceInstanceId
adapterId
adapterVersion
```

保留：

```text
startedAt
completedAt
rawCount
newUniqueCount
duplicateCount
proposalCount
errorCount
```

不保存 raw row。

---

## 21. PHASE 13 — Global Node Resolution Tests

至少测试 descriptor：

```text
NETFLIX.COM
Netflix
NETFLIX *123
腾讯视频
PAYPAL *SPOTIFY
SPOTIFY USA
```

不追求全球 Entity Resolution。

只要求：

- normalize
- alias exact
- conservative fuzzy
- user-confirmable candidate

不能因 descriptor 包含 PayPal 就自动假设 `Card → PayPal → Spotify`。

---

## 22. PHASE 14 — Multi-source Proposal Aggregation

同一 logical suggestion：

```text
from|relation|to|capability
```

来自两个 SourceInstance：

```text
仍只有一个 Proposal Item
evidenceRefs[] 增加多个 Evidence
```

决策只有一个。

accepted 后新 source evidence 不重复问。

rejected 后按单-stream 阈值判断重提。

---

## 23. PHASE 15 — Multi-source Synthetic E2E

至少建立：

```text
US_Card_1234
PayPal_US
Spotify
```

数据源：

```text
source_csv_card
source_ofx_bank
```

完整验证：

1. 两个来源导入
2. Resolver
3. 同 logical Proposal 不重复
4. evidenceRefs 有多个 provenance
5. 用户确认
6. Dependency 只有一条
7. simulateDisable
8. ChangePlan

禁止因为“两源都看见”自动把 criticality 变 required。

---

## 24. PHASE 16 — Cross-source Double-count Protection

未实现完整 CanonicalEvent cross-source dedupe 前：

禁止 `sum(all evidence observationCount)` 驱动：

- recurrence certainty
- re-proposal threshold
- criticality
- backup confirmation

每个 Evidence stream 独立。

必须写测试。

---

## 25. PHASE 17 — Coverage Semantics

三个 Adapter 都是 `event_stream`。

必须证明：

```text
previously confirmed Dependency
+ 新 CSV/OFX 未出现它
→ 不自动 retire
→ 不自动 reject
→ 不自动确认 fallback
```

Freshness 可以更新，但 absence 不产生现实否定。

---

## 26. PHASE 18 — Minimal Provider Metadata

只建立最小 `ProviderDescriptor`：

```text
id
displayName
aliases
countries?
```

用途仅限 display/source metadata/alias normalization。

不进入 Impact。

不要做联网 ProviderKnowledgePack。

---

## 27. PHASE 19 — Multi-currency Metadata

支持：

```text
amount + currency
```

不做 FX conversion、portfolio、net worth。

不同 currency 不得直接用于金额稳定性比较。

为 recurrence 写 guard test。

---

## 28. PHASE 20 — MVP01 Regression

MVP02 完成后完整重跑 MVP01：

- WeChat Parser
- Fingerprint duplicate import
- Proposal lifecycle
- Impact T1–T12 /扩展测试
- Group semantics
- ChangePlan ordering
- `.depmap` crypto Golden Vector
- security/privacy checks

任何 regression 先修再继续。

---

## 29. PHASE 21 — Quality Gates

执行现有：

```text
npm run format:check
npm run lint
npm run typecheck
npm test
npm run check
npm run check:full
```

再补 Source/Migration 测试脚本。

不得降低 MVP01 RC 标准。

---

## 30. PHASE 22 — Security / Privacy Re-audit

重点确认：

- SourceInstance 不含账户秘密
- Mapping Profile 不保存 raw statement
- OFX raw 不持久化
- Evidence 仍不是交易历史
- sourceTxnId 不明文长期存储
- logs 不打印原始行
- `.depmap` schema v2 仍被容器 V1 加密

更新相关 audit 文档。

---

## 31. PHASE 23 — Performance Smoke

至少：

```text
10k Generic CSV rows
10k OFX transactions
3 SourceInstances
multi-source proposal aggregation
schema v1→v2 migration
```

只防明显退化。

---

## 32. PHASE 24 — Documentation

新增/更新：

```text
docs/SOURCE_ARCHITECTURE.md
docs/SOURCE_INSTANCE.md
docs/GENERIC_CSV_ADAPTER.md
docs/OFX_QFX_ADAPTER.md
docs/SCHEMA_V2.md
docs/MIGRATION_V1_V2.md
docs/MULTISOURCE_EVIDENCE.md
docs/TEST_MATRIX_MVP02.md
```

README 可以写支持 WeChat / Generic CSV / OFX-QFX，但不得宣称“支持所有银行”。

---

## 33. PHASE 25 — Final Acceptance

更新：

```text
WORK_STATUS.md
BLOCKERS.md
MVP02_ACCEPTANCE.md
```

生成：

```text
MVP02_FINAL_REPORT.md
```

最终报告至少包含：

```text
MVP02_GLOBAL_SOURCE_ABSTRACTION =
SCHEMA_V2 =
V1_TO_V2_MIGRATION =
SOURCEINSTANCE =
WECHAT_REGRESSION =
GENERIC_CSV =
OFX_QFX =
MULTISOURCE_EVIDENCE =
FINGERPRINT_SCOPE =
RELATION_REGISTRY =
DEPMAP_CONTAINER_V1_COMPAT =
QUALITY_GATES =
REAL_DATA = NOT_RUN
```

以及 tests/pass/fail/skip、fixtures、migration、performance、secret/type/lint counts。

平台状态仍诚实区分：

```text
IMPLEMENTED / STATIC_AUDITED / COMPILED / TESTED / DEVICE_VERIFIED
```

---

## 34. 成功条件

只有全部满足才允许：

```text
MVP02_GLOBAL_SOURCE_ABSTRACTION = PASS
```

必须证明：

1. WeChat 不再是 Domain 特例
2. 3 个 Adapter 共用同一 pipeline
3. 多 SourceInstance 安全共存
4. 同 txn ID 跨 SourceInstance 不冲突
5. multi-source evidence 不粗暴相加
6. same logical Proposal 不重复
7. Human Confirmation 语义不变
8. Impact 语义不变
9. DEPMAP_CONTAINER_V1 不变
10. payload schema v2 可从 v1 迁移
11. MVP01 regression 全绿
12. quality/security gates 全绿
13. Real Data 如实 NOT_RUN

---

## 35. 自动执行

不要每个 Phase 停下来问。

持续：

```text
audit
→ failing tests
→ implementation
→ tests
→ regression
→ security check
→ docs
→ next
```

普通 migration/type/test/compile bug 不是用户 Blocker，自己修。

平台 SDK、设备、签名等外部条件可以进 BLOCKERS，但不能停止 Core MVP02。

---

## 36. 现在开始

立即：

1. 读取全部控制文件
2. 创建 `MVP02_PRE_AUDIT.md`
3. 冻结 MVP01 baseline
4. 写 Schema v2 migration failing tests
5. 写 SourceInstance / fingerprint scope failing tests
6. 实现 EvidenceSourceAdapter
7. 将 WeChat 迁成普通 Adapter
8. 实现 Generic CSV
9. 实现 OFX/QFX
10. 完成 multi-source synthetic E2E
11. 重跑全部 MVP01 RC Gate
12. 生成 `MVP02_FINAL_REPORT.md`

不要再讨论设计，实际执行。
