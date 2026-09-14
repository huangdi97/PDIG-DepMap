# QUALITY_GATES_V1.md — 工程 Gate 清单（Engineering Baseline V1，2026-09-13）

> 状态口径：PASS / FAIL / BLOCKED（外部原因 + blocker 编号）/ NOT_RUN / PARTIAL_WITH_REPORT。
> 本表取代根目录 `QUALITY_GATES.md`（RC 轮版本保留作历史证据）。

## Q0 Git Hygiene

- [x] PASS — `git diff --check` 干净；无意外二进制 / temp 文件；`.gitignore` 覆盖
      node_modules/coverage/reports/.stryker-tmp/*.mutbak；无 secrets / 真实数据
- [x] PASS — `.codebuddy/` 已审计（纯 WorkBuddy 规则）并入库；tag `v0.2.0-mvp02` 已建立

## Q1 Format

- [x] PASS — `format:check`（prettier 3.9.6；.prettierignore 排除 node_modules/coverage/reports/.stryker-tmp）

## Q2 Lint

- [x] PASS — eslint 10 typed，0 errors / 0 warnings；无文件级豁免

## Q3 Type Safety

- [x] PASS — tsc strict 全开 + noUncheckedIndexedAccess；src 零 any/ts-ignore/非空断言（docs/TYPE_SAFETY_BASELINE.md）

## Q4 Architecture

- [x] PASS — check:architecture 35 files；4 边界规则 + **circular dependencies = 0**（内置 DFS 检测）

## Q5 Unit

- [x] PASS — unit/parser/impact/domain/repository 全绿（含 negative/determinism ×50/idempotency ×50/property-fuzz）

## Q6 Integration

- [x] PASS — pipeline / multi-source-e2e（K）/ coverage-semantics（H）/ graph-payload-v2（J）全绿

## Q7 Contract

- [x] PASS — EvidenceSourceAdapterContract ×3 Adapter（C0–C6，19 用例）；
      RepositoryContract harness（R1–R7，NodeSqliteDriver reference）；
      Crypto contract（golden + container-mutation）

## Q8 Migration

- [x] PASS — T1–T10（fresh/v1→v2/restart/×50/回滚/legacy 指纹/无孤儿/未来版本拒绝）+ payload v1→v2

## Q9 Invariants

- [x] PASS — `check:invariants`（INV1–INV14，真实导入管线上的集中语义铁律）

## Q10 Property / Fuzz

- [x] PASS — fast-check P1–P6（seed=20260913，numRuns 150–200/条）+
      crypto 结构化变异矩阵（F1–F6）+ CSV 行变异 ×100

## Q11 Crypto

- [x] PASS — golden vector 回归；fail-closed 矩阵（bounds 先于 Argon2；auth_failed 不可区分性）

## Q12 Security / Privacy

- [x] PASS — secret scan 0 production secrets（279 files）；network gate 0 业务网络调用；
      SECURITY_PRIVACY_REGRESSION_MATRIX S-01–S-15 全部成立

## Q13 Dependencies / License

- [x] PASS — `check:deps`：树健康 + lockfile 同步 + 许可证快照（全 MIT/Apache-2.0）；
      npm audit 3 moderate（dev-only，已登记 DEPENDENCY_POLICY）；THIRD_PARTY_NOTICES.md 已刷新

## Q14 Performance

- [x] PASS — test:perf 11 用例在预算内；基线固化于 docs/PERFORMANCE_BASELINE.md

## Q15 Clean Install

- [x] PASS — rm node_modules → npm ci → npm run check 全绿（324 tests）

## Q16 Clean Clone

- [x] PASS — `git archive HEAD` → temp → npm ci → check 全绿（无本地路径/未跟踪依赖）

## Q17 Docs

- [x] PASS — 工程基线文档群 19 份 + QUALITY_GATES_V1 + REPORT + README/AGENTS/WORK_STATUS 同步；
      README 命令实跑验证（README_COMMANDS_VERIFIED = PASS）

## Q18 Platform

- Android / HarmonyOS / iOS：STATIC_AUDITED = YES；COMPILED/TESTED/DEVICE_VERIFIED = **BLOCKED**
  （B1/B2/B3 外部工具链，见 BLOCKERS.md）

## Q19 Final

- [x] **ENGINEERING_BASELINE_V1 = PASS**（代码侧；平台编译与 Real Data 不在本基线判定范围，
      分别 BLOCKED / NOT_RUN）——证据：ENGINEERING_BASELINE_V1_REPORT.md

## Q20 Mutation（附加）

- [x] PARTIAL_WITH_REPORT — Stryker baseline（kernel + registry 532 mutants）+ 3 个人工变异
      （proposal 阈值 / fingerprint 作用域 / groupKey 排序）全部 KILLED；
      详见 docs/MUTATION_TEST_REPORT.md
