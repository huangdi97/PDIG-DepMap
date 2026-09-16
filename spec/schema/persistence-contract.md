# PDIG Persistence Contract

三端共享的持久化契约。物理 DDL 可不同，**以下语义必须完全一致**。

来源：`core/src/schema/migrations.ts`、`core/src/repositories/*` @ `6d268c0`。

---

## 1. 逻辑唯一性（Logical Uniqueness）

| 实体                    | 逻辑键                                                       | 约束位置           |
| ----------------------- | ------------------------------------------------------------ | ------------------ |
| Node                    | `id`                                                         | PRIMARY KEY        |
| Dependency              | `from_node \| relation \| to_node \| capability`              | **UNIQUE 约束**    |
| DependencyGroup         | `group_key = target \| capability \| mode \| sorted(members)` | **UNIQUE 约束**    |
| DependencyProposal      | `key`（= Dependency 逻辑键同构）                              | **UNIQUE 约束**    |
| DependencyGroupProposal | `key = group \| <canonicalGroupKey>`                          | **UNIQUE 约束**    |
| Evidence                | `(proposal_key, source_instance_id)`                          | **UNIQUE 约束**    |
| ObservationFingerprint  | `(source_instance_id, fingerprint_version, fingerprint)`       | **UNIQUE 约束**    |
| DiscoveryCandidate      | `normalized_key`                                              | **UNIQUE 索引**    |
| SourceInstance          | `id`                                                          | PRIMARY KEY        |
| ChangePlan              | `id`                                                          | PRIMARY KEY        |
| RealityDrift            | `id`                                                          | PRIMARY KEY        |

**硬规则**：唯一性必须由**数据库约束**保证，不能只靠应用层检查。
INV1/INV8 与 INV2 依赖这一条。

### 1.1 逻辑键规范化

- Dependency 逻辑键：`from|relation|to|capability`，**无空白、无转义**。
- Group 逻辑键：成员先 **去重**、再按
  `locale-independent` 的 `<` 比较（UTF-16 code unit 序）**升序**排列，然后拼接。
  → 成员顺序不影响 groupKey。
- NormalizedKey（候选）：形如 `ccb:8821`、`svc:netflix`；小写 + 去空白。

---

## 2. 外键与引用完整性（无 FK 约束，但有引用不变量）

MVP 不依赖数据库外键，改由不变量检查保证（`checkGraphIntegrity`）：

| 不变量                 | 含义                                                     |
| ---------------------- | -------------------------------------------------------- |
| `orphanDependencies`   | 每条 Dependency 的 `from_node` / `to_node` 必须存在于 nodes |
| `orphanGroups`         | Group 的 `target_node_id` 必须存在                        |
| `danglingGroupMembers` | `member_edge_ids` 每项必须存在于 dependencies             |
| `orphanEvidence`       | Evidence 的 `source_instance_id` 必须存在于 source_instances |
| `orphanFingerprints`   | 同上，fingerprint 的 `source_instance_id` 必须存在         |

**要求**：三端在 import / restore 后都必须能跑出这 5 项为 `[]`。
（INV13、J1c）

---

## 3. Retire 语义（Retire Semantics）

- **不是删除**。`retired` 是状态，行保留、id 保留。
- Dependency retire → **不再传播 Impact**，但仍参与唯一性（不能建第二条逻辑边）。
- 逻辑边退休后重新成立 → **re-activate 原 row（同一 id）**，绝不新建行。
- Group retire → 不再参与 Impact 的 group 判定。
- SourceInstance retire → 不再产生 timeline 的 freshness 项。
- Node `archived = 1` → 不出现在默认列表，但引用仍有效。

---

## 4. 时间戳语义（Timestamps）

- 全部为 **ISO 8601 UTC 字符串**（`YYYY-MM-DDTHH:mm:ss.sssZ`）。
- 排序是**字典序**，因此格式必须固定为 UTC + 毫秒 + `Z`。
- **禁止**本地时区偏移输出（`+08:00`）——会破坏字典序排序确定性。
- `last_verified_at` 是 staleness 判定的唯一输入（readiness 的 90 天阈值）。
- `lastIngestedAt` 为 `null` 表示"从未导入"，在覆盖度判定中视为**最陈旧**（`Infinity` 天）。

---

## 5. Graph Revision 语义

- 存储：`meta.graph_revision`，初值 `0`，**单调递增**。
- **只有 Confirmed Reality Mutation 才 bump**：
  新边 / 边更新 / retire / reactivate / group 确认 / group retire /
  drift resolve（replacement 与 additional path）/ node create·update·archive。
- **绝不 bump**：Evidence、Proposal upsert、Proposal 决策、Candidate
  upsert / dismiss / accept、Drift 检测、Drift dismiss、Timeline 构建、
  ImportSession、ChangePlan create / rebase / transition、Action done / verify。

### 5.1 原子性（Atomicity）—— 硬要求

> 每一次 **Reality mutation + revision increment** 必须落在**同一个事务**内。

不允许先写边再单独 bump；中途失败必须整体回滚，不能留下
"边已写入但 revision 未变"的中间态（否则所有 ChangePlan 的
staleness 判定会被污染）。

### 5.2 派生而非回写

`lastAnalyzedGraphRevision < 当前 revision` 且计划未 completed/cancelled
→ **派生** `needs_revalidation`。
**禁止**批量回写全部计划的状态（每次 Reality 变更都会变成全表扫描）。

---

## 6. 事务保证（Transaction Guarantees）

| 操作                | 要求                                                           |
| ------------------- | -------------------------------------------------------------- |
| Schema migration    | 每个 version 一个事务；失败回滚；重复执行幂等（×50 严格 no-op）  |
| Import finalize     | Proposal / Evidence / Fingerprint / ImportSession 同一事务       |
| Reality mutation    | 边写入 + revision bump 同一事务                                 |
| Drift resolve       | 新边 + 旧边 retire + revision bump 同一事务                      |
| Candidate accept    | Node 创建 + 状态更新同一事务                                     |
| Payload import      | 原子替换：失败时目标 DB 保持原样（**禁止半写入**）              |

---

## 7. 并发与锁（Locking）

- 单进程本地库；`db_locked` 是必须映射的 canonical error code。
- 三端都必须做"忙等重试/明确报错"，**不得**静默吞掉锁冲突。

---

## 8. 内存与隐私生命周期

| 数据                        | 生命周期                                        |
| --------------------------- | ----------------------------------------------- |
| 原始文件字节                | 导入会话内存；**结束后释放**                    |
| Parsed rows / Observations  | 仅会话内存；**绝不持久化单笔交易**              |
| Fingerprint                 | 持久化（哈希，不可逆）                          |
| EvidenceSummary             | 持久化（计数 + 时间 + provenance，非交易内容）  |
| Proposal 状态               | 持久化                                          |
| 用户确认后的图实体          | 持久化                                          |
| ImportSession Summary       | 持久化（仅计数）                                |

审计要求：三端都必须证明"导入结束后 raw bytes 与 parsed rows 的引用被释放"。
（§150–§151）
