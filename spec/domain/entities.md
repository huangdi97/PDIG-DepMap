# PDIG Canonical Entities

机器可读定义在 `spec/domain/domain.json`（**权威**）。本文件只解释语义，不重复字段清单。

---

## 1. 三层现实模型

PDIG 的全部正确性都建立在这一分离上：

```
                    ┌─────────────────────────────┐
   CONFIRMED REALITY │ Node / Dependency / Group   │  ← 用户确认过；唯一能 bump revision 的东西
                    └─────────────────────────────┘
                              ▲ 只能由用户确认跨过
                    ┌─────────────────────────────┐
   MACHINE INFERENCE │ Proposal / Candidate / Drift │  ← 机器产出；永不是现实
                    └─────────────────────────────┘
                              ▲ 输入
                    ┌─────────────────────────────┐
   EPHEMERAL         │ Observation / Fingerprint    │  ← 仅会话内存；只有指纹与摘要持久化
                    └─────────────────────────────┘
```

跨越这条线**只有一条合法路径**：用户显式确认。

- Parser → **只能**产生 Proposal
- 机器 → **只能**产生 `DependencyGroupProposal`
- `DiscoveryCandidate` → 只有 **accept** 才创建 Node
- `RealityDrift` → 只有用户确认才变成 Reality mutation

---

## 2. Node

数字基础设施中的一个对象：卡、账户、服务、设备、会员、身份锚点、自定义。

- `kind` 决定它可以参与哪些 relation（见 `relations` 与 `validateRelationUse`）
- `fields` 是自由 KV，但**只有约定字段**有语义（目前仅 `expiryDate` 参与 Timeline）
- `archived` 是软隐藏，不是删除；引用仍然有效
- MVP 可创建的类型限于 `constants.runtimeCreatableNodeKinds`

**方向约定（重要）**：`from` 为 `to` 提供支付。
即 `from = 付款方（卡/支付账户）`，`to = 被支付方（服务/订阅）`。
`from` 失效 → `to` 受影响。

---

## 3. Dependency

**存在即代表用户确认。** 这是 PDIG 最容易被误用的实体。

- 逻辑键 `from|relation|to|capability`，数据库 UNIQUE 保证
- `criticality` 只有 `required` / `unknown`；**机器永不产生 `required`**
- `state = retired` 表示"已不再成立"，**不是删除**；重新成立时 **re-activate 原行**
- `verificationBasis` 记录"凭什么认为这是真的"；MVP 恒为 `user_confirmed`

**禁止**：同一逻辑边出现第二行。

---

## 4. DependencyGroup

用户确认的"组合/备用"关系。**机器只能生成 Proposal。**

- `mode`：`ANY`（任一成员可用即可）/ `ALL`（全部成员可用才行）
- 必须 **capability-scoped**；只在同 capability 内判定满足性
- `groupKey` canonical：成员排序去重后拼接 → 成员顺序无关
- 目前只有 `funding_source` 允许 Group，且只允许 `ANY`

**Impact 语义**：

- 覆盖失效边的 group **全部**不满足 → `must_change`
- 否则 → `backup_path`（支付可继续，但**冗余度下降**）

---

## 5. Evidence / Fingerprint / ImportSession

**都不是交易史。**

- `Observation` / `CanonicalEvent`：**仅导入会话内存**，会话结束销毁
- `Fingerprint`：持久化的**哈希**（不可逆）；命名空间按 **SourceInstance** 隔离
- `EvidenceSummary`：按 `(proposalKey, sourceInstanceId)` 分流的**计数摘要**
- `ImportSession`：仅计数与时间

**硬规则**：`observationCount` **禁止跨 stream 相加**用于任何判定。
多源只增加 **provenance**，不增加**确定性**。

---

## 6. SourceInstance

"用户具体的某个数据源实例"。它决定了：

- absence 的语义（`coverageMode`；MVP 三个适配器全为 `event_stream`）
- fingerprint 的命名空间
- evidence 的分流边界
- Timeline 的 freshness 判定

MVP02 的三个文件适配器：

| adapterId          | sourceKind       | coverageMode   | authoritativeFor |
| ------------------ | ---------------- | -------------- | ---------------- |
| `wechat_statement` | `statement_file` | `event_stream` | `[]`             |
| `generic_csv`      | `statement_file` | `event_stream` | `[]`             |
| `ofx_qfx`          | `statement_file` | `event_stream` | `[]`             |

**全部是 `event_stream`** ⇒ 「账单里没出现」**永远不等于**「现实里不存在」。
这是 ABS-06 的结构性来源。

---

## 7. DependencyProposal

- 同 key 永远 **UPSERT**，不新建行
- `pending` → 继续累计 evidence
- `accepted` → 不再重复问
- `rejected` → **不是永久为假**；但重提必须满足**单流**阈值 3

`confidenceScore` **只用于展示**，不参与 must_change，也不参与 readiness。

---

## 8. ChangePlan

**不是 Reality。** 它是"用户确认过的变更意图 + 影响快照"。

- `workflowState` 是存储状态；`needs_revalidation` 是**派生**状态，不回写
- `impactSnapshot` 在创建/每次 rebase 时固定，用于 old vs new 差异
- `action.resolvesImpactKeys` 是 must_change 的**显式 resolution 声明**
- `action.done` ≠ `verification.status = verified`（铁律）
- `completed` / `cancelled` 是终态：不可迁出、actions 冻结、不被 rebase 改写

---

## 9. RealityDrift

"可能发生了变化"的**提议**，不是结论。

- 只接受 **positive evidence**
- 观测数 < 2 → 忽略（保守不催）
- 同 key 多信号 → 单一 open drift 累计
- `dismiss` / 创建 都**不**改变 Reality、**不** bump revision
- 只有 `resolveAsReplacement` / `resolveAsAdditionalPath` 才是 Reality mutation

---

## 10. DiscoveryCandidate

"发现了一个可能的对象"，但**还不是 Node**。

- 不进入 Impact、不 bump revision
- accept 才创建 Node（replay 幂等）
- dismiss 后需 ≥ 2 条新观测才回到 pending

---

## 11. TimelineItem

**纯投影（derived read model）**，不持久化、可随时重建。

- 只包含数字基础设施事项；**不是日历**
- 排序确定：`bucket → priority(降序) → scheduledAt → id`
- 每一项必须**可溯源**（`sourceType` + 真实存在的 `sourceId`）

---

## 12. GraphRevision

Reality 的**版本时钟**。

- 只有 Confirmed Reality Mutation 才 bump
- 与 mutation **同事务**（否则所有计划的 staleness 判定会被污染）
- 计划通过 `lastAnalyzedGraphRevision` 与它比较，**派生** `needs_revalidation`
