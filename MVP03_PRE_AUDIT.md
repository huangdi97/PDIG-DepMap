# MVP03_PRE_AUDIT.md — MVP03 启动前现状审计（2026-09-13）

> 原则：先记录现状，避免创建重复实体。全部为实跑/实测结果。

## 1. Git / 环境

| 项             | 值                                                                               |
| -------------- | -------------------------------------------------------------------------------- |
| HEAD（启动时） | `def9389` docs(status): WORK_STATUS — Engineering Baseline V1 代码侧 PASS        |
| branch         | `engineering/baseline-v1`（干净，0 dirty）→ 本轮新分支 `feat/mvp03-living-graph` |
| tag            | `v0.2.0-mvp02`（MVP02 终态锚点）                                                 |
| Node           | v22.15.0 / npm 11.3.0                                                            |

## 2. 工程基线状态（Engineering Baseline V1 = PASS）

- 324/324 tests（28 文件）、0 skip
- format / lint（0/0）/ typecheck（strict + noUncheckedIndexedAccess + exactOptionalPropertyTypes）PASS
- architecture PASS（35 files，circular = 0）；network gate PASS；secret scan PASS（284 files）
- coverage src 口径 line 94.67% / branch 81.59%
- mutation baseline：kernel 55.96% / registry 96.88%（PARTIAL_WITH_REPORT）
- clean install / clean clone / stability ×3 PASS
- 统一命令：check / check:full / check:invariants / check:contract / check:property /
  check:db-integrity / check:deps / test:stability（见 AGENTS.md §25）

## 3. Schema / 协议现状

| 项                  | 值                                                       | MVP03 处置                                                    |
| ------------------- | -------------------------------------------------------- | ------------------------------------------------------------- |
| 应用 Schema 版本    | `SCHEMA_VERSION = 2`（v1→v2 迁移 + ×50 幂等 + 回滚已测） | 新增持久化实体 → **升 v3**（v2→v3 增量迁移，test-first）      |
| DEPMAP_CONTAINER_V1 | formatVersion=1，golden vector 冻结                      | **不变**（container version ≠ app schema version）            |
| graph payload       | `GRAPH_PAYLOAD_VERSION = 2`，v1 in-memory migrate        | 升 **v3**（增加 graphRevision；v1/v2 in-memory migrate 保留） |
| meta 表             | key-value（schema_version / fpSecret 等）                | **复用**：graphRevision 存 `meta.graph_revision`（无新表）    |
| revision 元数据     | 不存在                                                   | 新建 GraphRevision API（bump 与 Reality mutation 同事务）     |

## 4. 现有可复用模型（MVP03 不重复造）

| 现有                                                               | 位置                                                      | MVP03 用途                                          |
| ------------------------------------------------------------------ | --------------------------------------------------------- | --------------------------------------------------- |
| `ImpactResult / simulateScenario / simulateDisable`                | `src/impact/kernel.ts`                                    | Scenario 执行 + Plan baseline/rebase 的 impact 快照 |
| `ImpactGraph` 组装（deps/groups/proposals）                        | invariants 测试内联 → 本轮提升为 `services/graph-view.ts` | ChangePlan / Coverage / Drift 共用                  |
| `DependencyRepository`（confirm/retire/updateCriticality）         | `src/repositories/dependency-repository.ts`               | Reality mutation 挂 revision bump                   |
| `DependencyGroupRepository`（confirm/retire）                      | `src/repositories/group-repository.ts`                    | 同上                                                |
| `Proposal repos + canRepropose(REPROPOSAL_MIN_NEW_OBSERVATIONS=3)` | `src/repositories/proposal-repository.ts`                 | Drift/Candidate 的保守重提阈值复用同口径            |
| `EvidenceRepository`（per-stream 累计）                            | `src/repositories/evidence-repository.ts`                 | Drift evidence 引用（不复制 Evidence）              |
| `ImportCoordinator`（begin/resolve/finalize 单事务）               | `src/services/import-coordinator.ts`                      | Drift 检测挂接点（finalize 后正向 evidence 信号）   |
| `checkGraphIntegrity` / graph-serialize                            | `src/services/graph-serialize.ts`                         | payload v3 迁移与完整性复用                         |
| `NodeRepository.fields_json`                                       | `src/repositories/node-repository.ts`                     | 卡片到期元数据（timeline expiration 来源）          |

## 5. 不存在、需要新建的实体（本轮范围）

| 实体                                | 形态                                          | 持久化                             |
| ----------------------------------- | --------------------------------------------- | ---------------------------------- |
| graphRevision                       | meta 键 `graph_revision`（初始 0）            | meta 表（无新表）                  |
| ChangePlan + workflowState + Rebase | domain 类型 + `change_plans` 表 + service     | **新表**                           |
| PlanReadiness                       | 纯函数规则引擎                                | 无持久化（由 plan + graph 派生）   |
| ScenarioCoverage                    | 纯函数 + 解释列表                             | 无持久化（派生 read model）        |
| RealityDrift                        | `reality_drifts` 表 + service                 | **新表**                           |
| DiscoveryCandidate                  | `discovery_candidates` 表 + service           | **新表**                           |
| ScenarioTemplate                    | 静态代码注册表（`src/scenarios/registry.ts`） | 无表（产品配置，非 Graph Reality） |
| Timeline / Upcoming                 | 纯投影函数                                    | 无表（derived read model）         |
| Action Verification                 | ChangePlan action_items_json 内嵌状态机       | 随 plan 持久化                     |

## 6. 现有测试盘点（MVP03 继承）

- 324 tests / 28 files：unit 7 / parser 1 / crypto 3 / impact 3（kernel、mutation-baseline、
  perf 前身）/ domain 1 / repository 4 / sources 2 / integration 4 / contract 2 / invariants 1 /
  property 1 / perf 1 / migration 含于 repository
- 关键既有语义锚：kernel T1–T12、H1–H4（absence）、K1–K6（multi-source）、INV1–INV14、
  P1–P6（fast-check）、C0–C6（adapter contract）、R1–R7（repository contract）

## 7. UI 现状

- `app/` uni-app x 源码 11 页 + 5 UTS 插件（IMPLEMENTED，未编译 B10）。
- MVP03 按 §46–§51 调整：首页 answer-oriented 化、场景库、Plan Detail（readiness/rebase 提示）、
  Drift 卡片、Timeline。静态审计（UI_SOURCE_AUDIT）随更新刷新；编译仍 BLOCKED。

## 8. 明确不做（本轮）

PayPal/Plaid/Open Banking/Browser/OAuth/手机号-邮箱完整 capability/GitHub/Domain/Cloud Graph/
IncidentPlan 完整系统/Recovery Drill/Digital Legacy/家庭成员/云同步/后端/AI/LLM/N-of-M/
跨 capability Impact —— 全部入 NEXT_BACKLOG / SCENARIO_CATALOG_FUTURE（只设计不实现）。

## 9. 风险分级（CHANGE_RISK_POLICY）

Schema v3 migration、GraphRevision、PlanReadiness/Rebase、RealityDrift = **HIGH**：
test-first + property/invariant + 人工 mutation + 回归全绿才可收口。
