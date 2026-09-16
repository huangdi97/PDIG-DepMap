# PDIG Correctness Invariants

> 第一原则：**宁可漏报，不可把"不确定"伪装成"必须处理"。**
> Precision > Recall。

本文件是 spec 的不变量层。每条不变量都必须有一个**针对性反例**在
`fixtures/` / `conformance/expected/` 中，否则它只是口号。

---

## A. 现实与推断的分离（Observation ≠ Reality 家族）

| ID      | 不变量                    | 反例（必须失败）                                                     |
| ------- | ------------------------- | -------------------------------------------------------------------- |
| OBS-01  | Observation ≠ Reality     | 导入一条交易 → 不得产生 Dependency、不得 bump revision                |
| OBS-02  | Proposal ≠ Reality        | 存在 `confidenceScore = 0.999` 的 pending Proposal → 不得产生 must_change |
| OBS-03  | Candidate ≠ Node          | `upsertCandidate` / `dismiss` / `accept` → Node 数不变（除 accept 恰 1） |
| OBS-04  | Drift ≠ Reality change    | `detectFromEvidence` → Dependency 计数与 revision **均不变**          |
| OBS-05  | Evidence ≠ History        | 持久化层不得出现单笔交易字段                                          |
| OBS-06  | absence ≠ non-existence   | 第二次导入缺少某商户 → 原 Dependency 仍 `active`                      |
| OBS-07  | done ≠ verified           | `completeAction` → `verification.status` 仍为 `pending`               |
| OBS-08  | coverage ≠ readiness      | `well_evidenced` 不得映射为 `safe` / `100%` / `all clear`             |
| OBS-09  | Timeline ≠ Truth          | `buildTimeline` 是纯投影，不写库、不 bump revision                    |

### A.1 结构性保证（比运行时检查更强）

`PlanReadinessInput` 与 `ScenarioCoverageInput` 的**类型中根本不存在**
`confidenceScore` 与 `absence` 字段。

因此"confidence 不能绕过 review"与"absence 不能提高 readiness"
不是一句承诺，而是**类型系统层面的不可表达**。三端必须保持这一点：
不得为了"以后可能需要"而先把字段加进输入结构。

---

## B. Impact 内核（IMP）

| ID     | 不变量                                                                    |
| ------ | ------------------------------------------------------------------------- |
| IMP-01 | `must_change` 只能来自**已确认现实**；`confirmed false positive = 0`       |
| IMP-02 | 机器推断**永不得**设置 `criticality = required`                            |
| IMP-03 | Proposal 的任何 confidence 都不参与确定性失效传播（最多 `needs_review`）   |
| IMP-04 | `retired` 边不传播 Impact；re-activate 后恢复传播                          |
| IMP-05 | 状态键是 `(nodeId, capability)`，**不是** `nodeId`（禁止 `visited: Set<nodeId>`） |
| IMP-06 | 图允许有环；BFS 必须 cycle-safe，`processedKeys` 无重复                    |
| IMP-07 | `needs_review` 向下游传播"不确定性"，但**永不**升级为 `must_change`        |
| IMP-08 | 输出排序确定：`(depth, nodeId, capability)`                                |
| IMP-09 | 原始注销/停用动作在 checklist 中**强制最后**（`target_operation`）         |

### B.1 Impact 判定顺序（三端必须一致）

对目标 T（capability = payment）：

1. 若**无** confirmed 入边失效：
   1. 上游存在不确定 → `needs_review` / `upstream_uncertain`
   2. 否则有 pending proposal 指向 T 且其上游失效 → `needs_review` / `proposal_only`
   3. 否则 → `unaffected`
2. 若**有** confirmed 入边失效：
   1. 存在覆盖失效边的 confirmed group：
      - 全部 group 都不满足 → `must_change` / `confirmed_group_failed`
      - 否则 → `backup_path` / `confirmed_group_covered`
   2. 否则存在其他（未失效）入边 → `needs_review` / `unconfirmed_alternative_exists`
   3. 否则失效边中含 `required` → `must_change` / `required_edge_no_alternative`
   4. 否则 → `needs_review` / `criticality_unknown`

**Group 满足性**：

- `ANY`：至少一个成员边可用即满足
- `ALL`：全部成员边可用才满足
- Group 必须 capability-scoped，且只在**同 capability**内判定

---

## C. Graph Revision（GR）

| ID    | 不变量                                                       |
| ----- | ------------------------------------------------------------ |
| GR-01 | 只有 Confirmed Reality Mutation 才 bump                       |
| GR-02 | Reality mutation 与 revision increment 在**同一事务**内       |
| GR-03 | revision 单调递增，永不回退                                   |
| GR-04 | Evidence / Proposal / Candidate / Drift create / Timeline **不 bump** |

**反例**：先写边、另一个事务再 bump，中间崩溃 → 必须整体回滚。
若测试能观察到"边已存在但 revision 未变"，GR-02 失败。

---

## D. RealityDrift（DR）

| ID    | 不变量                                                             |
| ----- | ------------------------------------------------------------------ |
| DR-01 | 只接受 **positive evidence**；absence-only **永不**产生 drift        |
| DR-02 | 已确认来源的新信号 → `already_confirmed` 忽略，不新建 drift          |
| DR-03 | 观测数 < 2 → `below_threshold` 忽略（保守不催）                      |
| DR-04 | 同 key 多信号 → **单一 open drift** 累计（upsert，不重复建）         |
| DR-05 | 重复 evidence ref 不重复计数                                        |
| DR-06 | 跨源 provenance 保留在 `evidenceRefs`（引用合并，不复制内容）        |
| DR-07 | drift 检测 / dismiss 均不修改 Reality 与 revision                    |
| DR-08 | rejected Proposal **不**产生 confirmed drift                        |
| DR-09 | 非 open 状态不可再次 resolve / dismiss                              |

---

## E. DiscoveryCandidate（PC）

| ID    | 不变量                                                        |
| ----- | ------------------------------------------------------------- |
| PC-01 | 不进入 Impact（accept 后依赖图仍无边）                          |
| PC-02 | 不 bump graphRevision                                          |
| PC-03 | accept 才创建 Node；accept replay 幂等（同 nodeId）             |
| PC-04 | dismiss 后需 ≥ 2 条新观测才回到 pending                         |
| PC-05 | accepted 的候选被 superseded：同名新信号不再打扰                |
| PC-06 | 不同 SourceInstance 的 provenance 合并引用，不复制内容          |

---

## F. Verification（VF）

| ID     | 不变量                                                            |
| ------ | ----------------------------------------------------------------- |
| VF-001 | `completeAction` 只置 `done`，不动 verification                     |
| VF-002 | 手动确认 → `verified`（`manual_confirmation`）                      |
| VF-003 | future evidence 命中 → 最多 `evidence_suggested`（**不自动 verified**） |
| VF-004 | evidence suggestion 不修改 Reality（deps / revision 不变）           |
| VF-005 | 重复 evidence ref 不重复记录                                        |
| VF-006 | 错误来源（不匹配 `expectedFrom/To`）不产生任何 suggestion            |
| VF-007 | `verified` 状态跨进程重启稳定                                       |
| VF-008 | `verified` / `failed` / `not_required` **不可**被 evidence suggestion 覆盖 |

---

## G. Readiness（RD）

| ID     | 不变量                                                                     |
| ------ | -------------------------------------------------------------------------- |
| RD-01  | 基于 **Requirement / Impact Resolution Mapping**，**禁止** `mustChangeCount − completedActionCount` |
| RD-02  | 一个 key 只有被 change 动作**显式声明**（`resolvesImpactKeys`）且其**全部**声明动作 `done` 才算 resolved |
| RD-03  | 无人声明的 key **永远** unresolved                                          |
| RD-04  | 已完成的空声明动作**不得**事后追认 key（防事后追认）                        |
| RD-05  | `blocked` 优先级高于 `review_required`                                      |
| RD-06  | 只有三值：`blocked` / `review_required` / `ready_with_known_scope`           |
| RD-07  | 同输入恒同输出（deterministic）                                            |
| RD-08  | verify 阶段动作**不**阻塞 readiness；未完成的 **change** 阶段动作阻塞        |
| RD-09  | revision 落后 → `review_required`（派生 `needs_revalidation`）              |

---

## H. 图完整性（INV1..INV14）

| ID      | 不变量                                                                |
| ------- | --------------------------------------------------------------------- |
| INV1/8  | 逻辑键唯一：accept 重放 ×3 + retire→reactivate 后重复逻辑键 = 0          |
| INV2    | groupKey 唯一且 canonical（成员顺序无关）                              |
| INV3/4/5| 无 dangling dependency / evidence；Proposal relation 已注册且校验通过   |
| INV6    | Group 成员存在且 registry 校验通过                                     |
| INV7    | Fingerprint 作用域唯一：`(source_instance_id, fingerprint_version, fingerprint)` 全表无重复 |
| INV9    | retired 不传播；re-activate 同 id 恢复传播                             |
| INV10   | event_stream absence 不 retire Reality                                |
| INV11   | Proposal-only 图永不产生 `must_change`                                 |
| INV12   | unknown criticality 只 `needs_review`，绝不 `must_change`               |
| INV13   | 全程无 orphan / dangling                                              |
| INV14   | `currentSchemaVersion === SCHEMA_VERSION === 3`                        |
| INV15   | `lastAnalyzedGraphRevision ≤ 当前 revision`（任意序列后）               |
| INV16   | `ready_with_known_scope` 需要 revision current + must_change 已处理      |
| INV17   | completed 计划不被 rebase 改写历史                                     |
| INV18   | `confirmed_change` drift 必对应成功的 Reality mutation                  |
| INV19   | accepted candidate 不创建重复逻辑 Node                                  |
| INV20   | TimelineItem 必须有有效 source reference                               |
| INV21   | active ScenarioTemplate 的 capability 必须是已支持的（payment）          |

---

## I. 来源抽象（SRC）

| ID     | 不变量                                                       |
| ------ | ------------------------------------------------------------ |
| SRC-01 | 不同 SourceInstance **不得**共享 fingerprint 命名空间          |
| SRC-02 | 多源 evidence 增加 **provenance**，不增加**确定性**             |
| SRC-03 | 不跨 stream 相加 observationCount 做任何决策                    |
| SRC-04 | 重提阈值按**单条** stream 判定：`observationCount − rejectedAtStreamCounts[s] ≥ 3` |

**反例（SRC-03/04）**：rejected 后两条流各来 2 条新观测（合计 4）
→ **不得**重提（单流均未达 3）。

---

## J. 跨端协议（DEP / SC）

| ID     | 不变量                                                       |
| ------ | ------------------------------------------------------------ |
| DEP-01 | `DEPMAP_CONTAINER_V1` 三端字节级兼容；Golden Vector 必须相等   |
| DEP-02 | JCS 三端产生相同 bytes                                       |
| DEP-03 | password 使用精确 UTF-8，**不做** Unicode 归一化               |
| DEP-04 | 认证前 header 不可信；bounds 在 KDF 之前完成                   |
| SC-01  | active 场景模板仅 payment capability                          |
| SC-02  | 注册表中不存在日常生活提醒类模板                               |
| SC-03  | planned 模板不可执行（无 factory → reject）                    |
