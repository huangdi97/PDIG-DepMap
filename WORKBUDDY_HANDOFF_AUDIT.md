# WORKBUDDY_HANDOFF_AUDIT.md

> Agent: WorkBuddy / DeepSeek-V4-Flash · Mode: Agent
> 采集时间：2026-09-12
> 分支：`feat/mvp02-global-source`
> 审计基线提交：`85c68d9`（本轮修复后）
> 前置交接提交：`fd6a968`（ZCode MVP02 P0 pre-audit baseline）

---

## 0. 关键结论（先读这一段）

接手时工作区处于 **ZCode 中途重构的破损状态**：`12 failed / 161 passed`（15 文件，173 用例）。
本轮已把根因修复并恢复绿色基线：**174 passed / 0 failed / 0 skipped**。

**没有删除任何测试、没有 skip、没有降低断言、没有放宽 lint/strict。**
新增 1 条回归测试（accepted + 新来源 evidence 不重复提问）。

需要强调的判断：ZCode 的 MVP02 骨架**方向正确且大部分结构已完成**，但其提交把
`Schema v2 / SourceInstance / Adapter 化` 一次性铺开却未收敛，导致 MVP01 语义被破坏
（导入不再产生任何 Proposal）。本轮修复的是**语义回归**，不是重构。

---

## 1. 六类状态分类

### 1.1 已完成且已验证（有测试证据 PASS）

| 项 | 证据 | 状态 |
|---|---|---|
| Schema v2 迁移（v1→v2） | `migration.test.ts` T1/T2/T3/T7/T8/T9 + ×50 幂等 + 同名 UNIQUE 校验（14 用例） | **PASS** |
| `SCHEMA_VERSION = 2` | `migrations.ts:10`；断言统一引用常量 | **PASS** |
| SourceInstance 一等实体 + 表 | `source_instances` 表 + `SourceInstanceRepository`（create/get/bind/retire/touchIngested） | **PASS** |
| Fingerprint source scope | `UNIQUE(source_instance_id, fingerprint_version, fingerprint)`（`migrations.ts:189`） | **PASS** |
| Evidence v2 多源 provenance | `UNIQUE(proposal_key, source_instance_id)`；`listByProposalKey` / `getByProposalKeyAndInstance` | **PASS** |
| `proposal_evidence_refs` join table | 迁移填入 + `attachEvidenceRef` + `evidenceRefsOf` | **PASS** |
| 单流重提阈值（禁止跨流相加） | `canRepropose` + `rejected_at_stream_counts_json`；测试「多源证据：同 key 两流独立计数」 | **PASS** |
| WeChat Adapter 化 | `WeChatStatementAdapter` 实现完整契约；Domain 层无 `if source == wechat` | **PASS** |
| 三个 Adapter 契约元数据 | 三者均 `statement_file` / `event_stream` / `authoritativeFor = []` | **PASS** |
| RelationDefinitionRegistry | `relation-registry.ts`（定义 + `validateRelationUse`；本轮补接线 `validateRelationGroupUse`） | **PASS** |
| `DEPMAP_CONTAINER_V1` 未变 | crypto 38 用例（golden/负向/mutation fuzz）全绿 | **PASS** |
| MVP01 Impact Kernel | `kernel.test.ts` 17 用例（T1–T12 + determinism） | **PASS** |
| MVP01 WeChat Parser | `wechat.test.ts` 18 用例 | **PASS** |
| 导入事务完整性 | `db-integrity.test.ts` PHASE AE（本轮修复后真正成立） | **PASS** |
| quality gates | format / lint(0 err) / typecheck(0 err) / architecture(35 files) / secret(208 files) | **PASS** |

### 1.2 已实现但未验证

| 项 | 说明 | 缺口 |
|---|---|---|
| GenericCsvAdapter | 317 行实现存在（mapping profile / delimiter / BOM / decimal / sign mode） | **无专属测试文件**；`tests/fixtures/` 下无 `us-credit-card.csv` 等 MVP02 fixture |
| OfxQfxAdapter | 167 行实现存在（FITID / DTPOSTED / TRNAMT / TRNTYPE / NAME / MEMO） | **无专属测试文件**；无 `.ofx`/`.qfx` fixture |
| `ImportCoordinator` 多源能力 | 代码支持任意 adapter + SourceInstance | 只有 WeChat 一条真实路径被测 |
| `verificationBasis` 持久化 | 本轮补齐三处写入分支 | 仅有「列存在且默认 user_confirmed」的迁移断言，无写入回读断言 |
| `graph-serialize` v1 payload → v2 in-memory migrate | 代码存在 | 需确认覆盖（`idempotency` 有 export/import 等价用例） |

### 1.3 部分完成

| 项 | 已完成 | 未完成 |
|---|---|---|
| MVP02 Test Matrix A（Schema/Migration） | T1/T2/T3/T7/T8/T9 + 幂等 + UNIQUE | T4（×50 在 **已迁移且有数据** 的库上）需确认；T5 legacy dedupe retained 需确认；T6 evidenceId→evidenceRefs 需确认；T10 old payload v1 migration path |
| MVP02 Test Matrix C（Fingerprint） | 同实例重复去重（既有用例） | **不同 SourceInstance 同 txn id 不冲突**、fingerprintVersion 隔离 —— 无显式用例 |
| MVP02 Test Matrix E（Generic CSV） | — | 全部 fixture（12 类）缺失 |
| MVP02 Test Matrix F（OFX/QFX） | — | 全部 fixture（8 类）缺失 |
| MVP02 Test Matrix G（Multi-source Evidence） | 单流重提阈值、accepted 不重复问（本轮新增） | 一个 Proposal 两条 evidenceRefs 的端到端断言 |
| MVP02 Test Matrix H（Coverage Semantics） | **缺失**：absence 不 retire / 不 reject / 不确认 fallback 无测试 | 需补 |
| MVP02 Test Matrix I（Relation Registry） | 有 `fromKinds/toKinds` 校验 | 无效组合拒绝的显式用例需确认 |
| MVP02 Test Matrix J（.depmap） | container golden 不变（PASS） | payload **v2 export/import**、**payload v1 migrate**、**unsupported schema 拒绝** 需补 |
| MVP02 Test Matrix K（E2E） | WeChat E2E PASS | CSV E2E / OFX E2E / 两 SourceInstance→一 Proposal 缺 |
| 性能 | 10k fingerprint / 1k node impact PASS | **10k CSV rows**、**10k OFX**、**3 SourceInstances** 缺 |

### 1.4 未开始

- GenericCsvAdapter 的 fixture 套件与 12 类边界测试
- OfxQfxAdapter 的 fixture 套件与 8 类边界测试
- Coverage semantics（absence ≠ retirement）回归测试
- `.depmap` payload schema v2 导出/导入 + v1 payload 迁移测试
- Multi-source synthetic E2E（CSV + OFX → 单一 logical Proposal）
- `docs/SOURCE_ARCHITECTURE.md` 等 8 份 MVP02 文档
- `MVP02_FINAL_REPORT.md`
- `WORK_STATUS.md` / `BLOCKERS.md` / `MVP02_ACCEPTANCE.md` 的 MVP02 更新

### 1.5 当前 regression / failure

| 接手时 | 本轮修复后 |
|---|---|
| `12 failed / 161 passed`（5 文件失败 + 1 unhandled rejection） | **174 passed / 0 failed** |

已修复的失败（按根因，不按表象）：

1. **`begin()` 未 await** — `ImportFlow.begin` 已改 async，13 处调用点未更新 → `requirePending()` 抛 `begin() must be called first`。
2. **SQLite boolean 绑定** — `confirmation-service.ts:61` 传 `criticalityDecision ?? null`；当 `criticalityDecision` 为 `false` 时驱动抛 `cannot be bound to SQLite parameter`。`node:sqlite` 只接受 null/number/bigint/string/Uint8Array。
3. **legacy SourceInstance 未绑定账户节点** — 迁移插入 legacy 实例时 `account_node_id` 为空；`WeChatStatementAdapter.suggestRoutes` 以 `accountNodeId` 为起点，为 null 时直接 `return []` → **导入产生 0 条 Proposal**（3 条主链测试同时失败）。
4. **`merchant_agreement` 路由按观测重复 push** — 6 次腾讯视频扣款产生 6 条同 key 路由，UPSERT 累计 `observation_count = 36`，扭曲重提阈值与置信度。
5. **指纹事务外提交** — `finalizeInner` 在 proposal 事务**之前**独立提交指纹；注入失败后残留 8 条孤儿指纹，违反 RC PHASE AE 不变量。
6. **schema_version 断言写死为 1** — `db-integrity` / `idempotency` 在 v2 下必然失败（机械问题，改为引用 `SCHEMA_VERSION`）。
7. **db-integrity fixture 使用 v1 列名** — 直接 `INSERT INTO evidence` 缺 `source_instance_id` 等 NOT NULL 列。
8. **`accepted` 分支 `changed` 语义** — `upsert` 在 accepted 后追加 evidence 时返回 `changed=true`，与「accepted 不重复问」契约冲突。

### 1.6 未提交（uncommitted）

接手时有 25 个已跟踪文件被修改 + 11 个未跟踪文件（含 ZCode 新增的 `src/sources/`、
`relation-registry.ts`、`source.ts`、`source-instance-repository.ts`、`import-coordinator.ts`）。

**处理方式**：未做任何丢弃或重置。全部修改经修复与验证后，作为单一提交
`85c68d9` 落入 `feat/mvp02-global-source`，ZCode 的原始工作全部保留（含逐条根因说明）。

> 注：本轮发现该分支的 `.git/refs/heads/feat/` 目录缺失（提交对象与 reflog 均正常，
> 仅 ref 文件未落盘），已按 reflog 中记录的真实提交哈希恢复，未改写任何历史。

---

## 2. 本轮修复清单（按文件）

| 文件 | 修改 |
|---|---|
| `src/db/driver.ts` | `SqlValue` 增加 `boolean` |
| `src/db/node-driver.ts` | `NodeStatement.bind()` 将 boolean 归一化为 0/1 |
| `src/repositories/meta-repository.ts` | 新增 `unknownToString()`（拒绝把对象静默串成 `[object Object]`） |
| `src/repositories/source-instance-repository.ts` | 新增 `bindAccountNode()`；清理多余类型断言 |
| `src/services/import-pipeline.ts` | legacy 实例**每次 begin 都补绑**账户节点 |
| `src/sources/wechat/adapter.ts` | `merchant_agreement` 按 service node 去重 |
| `src/services/import-coordinator.ts` | 指纹写入移入 finalize 事务（含预检与冲突守卫） |
| `src/repositories/proposal-repository.ts` | `changed` 语义修正为「会再次提问的决策变化」 |
| `src/repositories/dependency-repository.ts` | `verificationBasis` 落库（insert / verify / reactivate 三分支） |
| `src/services/confirmation-service.ts` | 接线 `validateRelationGroupUse`；显式传 `userConfirmedBasis` |
| `src/repositories/evidence-repository.ts` / `group-repository.ts` / `graph-serialize.ts` | 使用 `unknownToString` |
| 三个 adapter | 显式 `await Promise.resolve()` 保证 async 契约（不关规则） |
| 6 个测试文件 | await 补齐 / 断言引用常量 / fixture 升 v2 / 新增回归用例 |

---

## 3. 从哪个 Gate 继续

按 `MVP02_ACCEPTANCE.md` 顺序，**第一个真实未完成 Gate = E 段 Generic CSV**。

但依据依赖关系，正确顺序应为：

1. **先补 B/C 段缺口**（SourceInstance 多实例隔离、Fingerprint 跨实例不冲突）——
   这是 multi-source 一切能力的地基，且当前**完全无测试**。
2. **H 段 Coverage Semantics**（absence 不 retire Reality）—— 这是 AGENTS 第一原则的载体，优先级高于新增 Adapter。
3. **E 段 Generic CSV**（fixture 12 类 + deterministic ×50 + 10k smoke）。
4. **F 段 OFX/QFX**（fixture 8 类）。
5. **J 段 `.depmap` payload v2 / v1 迁移**。
6. **K 段 multi-source E2E**。
7. **L 段 MVP01 全量回归 + quality gates**。
8. **docs + `MVP02_FINAL_REPORT.md`**。

---

## 4. 平台状态（诚实区分）

| Platform | IMPLEMENTED | STATIC_AUDITED | COMPILED | TESTED | DEVICE_VERIFIED | STORE_READY |
|---|---|---|---|---|---|---|
| Core（Node） | YES | YES | YES | YES | N/A | N/A |
| Android | YES | YES | NO（B1） | NO（B1） | NO | NO |
| HarmonyOS | YES | YES | NO（B2） | NO（B2） | NO | NO |
| iOS | YES | YES | NO（B3） | NO（B3） | NO | NO |

Real Data Gate：**NOT_RUN**（不因本轮新增 synthetic 数据而改变）。
