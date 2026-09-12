# TEST_STRATEGY.md — 测试体系分层（Engineering Baseline V1）

> 当前证据：27 个测试文件 / 321 tests（2026-09-13）。禁止只用「总数」评价质量；按下表分层审计。

## 层定义与现有落位

| 层 | 定义 | 落位（core/tests/） |
|---|---|---|
| Unit | 单模块纯逻辑 | `unit/`（resolver / negative / determinism / idempotency / property-fuzz / crypto-negative / db-integrity）、`parser/`、`impact/`、`domain/` |
| Repository | 持久层语义 | `repository/`（repositories / proposal-lifecycle / migration / source-instance-scope） |
| **Contract** | 跨实现共享契约 | `contract/`（adapter-contract：三 Adapter 共享 C0–C6；repository-contract：参数化 harness + NodeSqliteDriver reference impl） |
| Migration | Schema 演进 Gate | `repository/migration.test.ts`（T1–T10：fresh / v1→v2 / restart / ×50 幂等 / 数据保全 / 失败回滚 / legacy 指纹连续性 / 无孤儿 / 未来版本拒绝）+ `integration/graph-payload-v2.test.ts`（payload J 段） |
| Integration | 多模块真实协作 | `integration/`（pipeline / multi-source-e2e K 段 / coverage-semantics H 段 / graph-payload-v2） |
| **Invariant** | 跨模块语义铁律 | `invariants/`（INV1–INV14，`npm run check:invariants`） |
| **Property-based** | fast-check 随机性质 | `property/`（P1–P6，seed=20260913 固定可复现） |
| Fuzz / Mutation | 变异鲁棒性 | `crypto/container-mutation.test.ts`（逐字段 fail-closed 矩阵）、`unit/crypto-negative.test.ts`（30 轮单字符 fuzz）、`unit/property-fuzz.test.ts`（CSV 行变异 ×100） |
| Performance Smoke | 防退化 | `perf/performance-smoke.test.ts`（11 用例，含 MVP02 三项） |
| Platform | 三端编译/真机 | 未运行（外部工具链 blocker B1–B3）；`platforms/` 测试代码就绪 |
| E2E（真数据） | Real Data 双 Gate | NOT_RUN（`core/scripts/validate-real-bill.ts` 就绪） |

## 契约注册表（新实现必须挂接）

| 契约 | harness | 已注册实现 | 未运行（外部 blocker） |
|---|---|---|---|
| EvidenceSourceAdapterContract | `tests/contract/adapter-contract.test.ts` | WeChatStatementAdapter / GenericCsvAdapter / OfxQfxAdapter | — |
| RepositoryContract | `tests/contract/repository-contract.test.ts`（DRIVERS 数组） | NodeSqliteDriver | 未来平台 SQLite 封装 |
| DEPMAP_CONTAINER_V1（CryptoAdapterContract） | `tests/crypto/depmap.test.ts` golden + `tests/crypto/container-mutation.test.ts` | Node 实现 | Android/iOS 侧 golden（B1/B3） |
| SecureDatabaseAdapterContract | 平台契约接口（`src/adapters/interfaces.ts`） | — | 平台实现编译后 |

**规则：新增 Adapter / 持久实现 / crypto 实现不跑对应 Contract 就不得 Merge。**

## Invariant ↔ 证据对照

| 不变量 | 证据 |
|---|---|
| 重复逻辑键 / groupKey = 0 | INV1/INV2 + migration UNIQUE DDL 断言 + repositories.test |
| Dependency→Node、Evidence→SourceInstance、Proposal relation 已注册 | INV3/INV4/INV5 + checkGraphIntegrity |
| Group 成员存在且合法 | INV6 |
| Fingerprint 作用域唯一 | INV7 + source-instance-scope C1–C7 |
| accepted 重放无重复 Dependency | INV8 + K2b |
| retired 不传播 Impact | INV9 + kernel T9 + property P3 |
| event_stream absence 不 retire | INV10 + H1 |
| Proposal / unknown 永不 must_change | INV11/INV12 + kernel T4/T8 + property P4/P5 |
| 图完整性 / Schema 版本 | INV13/INV14 |

## 覆盖率与稳定性

- 覆盖率 Gate：见 `docs/COVERAGE_POLICY.md`。
- 稳定性（flaky）：`npm run test:stability`（全量 ×3）必须零失败零重试；报告 `docs/FLAKY_TEST_REPORT.md`。
- 性能回归：`npm run test:perf` + `docs/PERFORMANCE_BASELINE.md`。

## Migration Gate 长期规则

每出现 Schema N：必须新增 `N-1 → N` 与 `oldest-supported → N` 两组迁移测试
（含 failure injection 回滚），并保持 ×50 幂等口径。
