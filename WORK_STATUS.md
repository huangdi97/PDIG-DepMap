# WORK_STATUS.md

> 本文件由执行 Agent 持续更新。不要删除历史关键结论。
> 2026-09-12 起由 WorkBuddy 接力（ZCode → WorkBuddy handoff），分支 `feat/mvp02-global-source`；
> 2026-09-13 Engineering Baseline V1 在新分支 `engineering/baseline-v1` 执行（自 MVP02 终态切出，
> tag `v0.2.0-mvp02`）。

## Current

- Phase: **ENGINEERING BASELINE V1 — 代码侧 PASS**（本轮）
- Status: 324/324 tests PASS（28 文件，0 skip）；format/lint/typecheck/architecture(circular=0)/
  network/secrets/deps 全绿；clean install + clean clone PASS；stability ×3 PASS
- Real Data Gate: **NOT_RUN**（固定）；平台编译 **BLOCKED**（B1–B3）
- 详细报告：**ENGINEERING_BASELINE_V1_REPORT.md** / **QUALITY_GATES_V1.md** /
  MVP01_RC_AUDIT_REPORT.md / MVP02_FINAL_REPORT.md

## Current quality state（Engineering Baseline V1 收口轮 2026-09-13 实跑）

- format:check PASS（prettier 3.9.6）；lint PASS（0 errors/0 warnings，无文件级豁免）
- typecheck PASS（strict + noUncheckedIndexedAccess + **exactOptionalPropertyTypes 已开启**）
- tests：**324/324 PASS，0 skip**（28 文件）= MVP02 基线 273 + 51（invariants 11 / contract 26 /
  property 6 / crypto container-mutation 5 / kernel mutation-baseline 3）
- architecture PASS（35 files，**circular dependencies = 0**）；network gate PASS（84 files 0 原语）；
  secret scan PASS（283 files，0 production secrets）
- coverage（src 口径，纯类型文件已排除）：**line 94.67% / branch 81.59%**
  （crypto 99.3 / schema 100 / domain 100 / fingerprint 100 / parser 96.9 / services 95.4 /
  repos 92.5 / impact 92.6 / sources 84.6 / resolver 82.5；双层 Gate 见 docs/COVERAGE_POLICY.md）
- 变异基线：Stryker 532 mutants（kernel 55.96% / registry 96.88%）+ 人工变异 3/3 KILLED
  （proposal 阈值 / fingerprint 作用域 / groupKey 排序）→ **PARTIAL_WITH_REPORT**
  （docs/MUTATION_TEST_REPORT.md；下一里程碑 kernel covered score ≥ 70%）
- 稳定性：`npm run test:stability` 3 连跑全绿，0 flaky，0 retry（docs/FLAKY_TEST_REPORT.md）
- 依赖：check:deps PASS（树健康 + lockfile 同步 + 许可证全 MIT/Apache-2.0）；
  npm audit 3 moderate（dev-only vitest 链，已登记 DEPENDENCY_POLICY，不阻塞）

## 本轮生产代码变更（均有回归）

1. `src/impact/kernel.ts` — 初始 unavailable 键**值级去重**（`Set<ImpactStateKey>` 值相等对象
   不去重 → processedKeys 可能重复；fast-check P1 发现，修复 + 324 回归）
2. `exactOptionalPropertyTypes` 开启 — 修复 8 处真实类型问题（domain/source.ts、
   fingerprint.ts 输入类型、OfxTransaction、evidence 输入）
3. 死代码删除 6 处（零引用逐一验证；docs/DEAD_CODE_AUDIT.md）

## 本轮新增自动化（统一命令，core/）

- `check` = format + lint + typecheck + test + architecture(circular) + **network gate** + secrets
- `check:full` = check + db-integrity + coverage + perf + **deps/license gate**
- 定向：`check:invariants` / `check:contract` / `check:property` / `check:db-integrity` /
  `check:deps` / `test:stability`；scripts：check-network.mjs / check-deps.mjs /
  test-stability.mjs / check-architecture.mjs(+循环依赖)
- package.json：`engines.node >= 22.5.0` + `packageManager: npm@11.3.0`

## 本轮文档落地（docs/）

ENGINEERING_STANDARDS / ARCHITECTURE_RULES / TEST_STRATEGY / COVERAGE_POLICY /
FAIL_CLOSED_MATRIX（F-01–F-20）/ SECURITY_PRIVACY_REGRESSION_MATRIX（S-01–S-15）/
DEFINITION_OF_DONE / AGENT_DEVELOPMENT_PROTOCOL / COMMIT_CONVENTION / CHANGE_RISK_POLICY /
FIXTURE_POLICY / LOGGING_POLICY / MONEY_CURRENCY_RULES / DATE_TIME_RULES /
MEMORY_DATA_LIFETIME / PERFORMANCE_BASELINE / DEPENDENCY_POLICY / TYPE_SAFETY_BASELINE /
DEAD_CODE_AUDIT / MUTATION_TEST_REPORT / FLAKY_TEST_REPORT；
根目录：QUALITY_GATES_V1.md / ENGINEERING_BASELINE_V1_REPORT.md / ENGINEERING_PRE_AUDIT.md；
流程：.github/pull_request_template.md；AGENTS.md §25（长期生效）；README 同步。

## Git

- 分支 `engineering/baseline-v1`；tag `v0.2.0-mvp02`（MVP02 终态 + .codebuddy 规则入库）
- 本轮 commits：1d0d1f6（.codebuddy）→ 150f63e（测试套件+脚本）→ ddffe74（死代码）→
  0c9eef9（mutation 补测+coverage 口径+deps gate）→ 36f5d35（工程文档群）→
  e75e7e2（exactOptionalPropertyTypes）→ a9d6243（stability+报告）→ 49feeba（终版报告）
- 未 push（禁止自动 push）

## Platform Matrix

| Platform | IMPLEMENTED | STATIC_AUDITED | COMPILED | TESTED | DEVICE_VERIFIED | STORE_READY |
|---|---|---|---|---|---|---|
| Android | YES | YES | NO（B1） | NO（B1） | NO | NO |
| HarmonyOS | YES | YES | NO（B2） | NO（B2） | NO | NO |
| iOS | YES | YES | NO（B3） | NO（B3） | NO | NO |
| Core（Node） | YES | YES | YES | YES | N/A | N/A |

## 仓库运维注意（重要）

`feat/mvp02-global-source` 分支的 loose ref 文件（`.git/refs/heads/feat/`）在本工作区会被
外部进程反复删除。**规避方式：分支 ref 固化在 `.git/packed-refs`。**
若再次出现"branch has no commits"，从 reflog 找回哈希后重写 packed-refs 即可，
**不要**执行任何 `git reset --hard` / `git clean`。

## Current failures

无失败测试。未执行项全部为外部工具链 Blocker（B1–B3/B10）或 Real Data（NOT_RUN），不虚报。

## External blockers

见 `BLOCKERS.md`（B1–B3、B10 阻断编译；B4–B9/B11–B13 发布材料）。

## Next

1. **MVP03：GOAL_MVP03_LIVING_GRAPH_CHANGE_SAFETY**（未启动；等用户发起）
2. B1 → Android golden 测试 + 编译（docs/ANDROID_TOOLCHAIN_SETUP.md）
3. B10 → HBuilderX 基座 → UI 编译 + 真机 spike
4. B3 → docs/IOS_MAC_HANDOFF.md
5. B13 → 真实账单双 Gate（validate-real-bill.ts 就绪；Real Data 保持 NOT_RUN 直到用户提供）
6. 下一里程碑变异目标：kernel covered score ≥ 70%（docs/MUTATION_TEST_REPORT.md）
