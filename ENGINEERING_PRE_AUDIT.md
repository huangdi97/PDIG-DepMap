# ENGINEERING_PRE_AUDIT.md — Engineering Baseline V1 现状审计

> 生成时间：2026-09-13（Engineering Baseline V1 启动轮）
> 原则：先记录现状，不做任何修改。所有数据为实跑/实测结果。

## 1. Git 状态

| 项                | 值                                                                                  |
| ----------------- | ----------------------------------------------------------------------------------- |
| HEAD（审计时）    | `1d0d1f6` chore(engineering): track .codebuddy WorkBuddy relay rules                |
| MVP02 收口 commit | `d294ad3`（tag `v0.2.0-mvp02` 指向 `1d0d1f6`，即 MVP02 终态 + .codebuddy 规则入库） |
| branch            | `engineering/baseline-v1`（自 `feat/mvp02-global-source` 切出）                     |
| dirty files       | 0（仅 `.codebuddy/` 曾未跟踪，已审计：纯项目规则，无 secret，已入库）               |
| `.workbuddy-ai/`  | 已跟踪（agent memory 记录，无 secret）                                              |

未跟踪处理结论：`.codebuddy/rules/pdig-mvp02/RULE.mdc` 内容为 WorkBuddy 接力规则（correctness/development/scope 约束），无任何 secret，WorkBuddy 后续会话仍会使用 → **加入 Git**（commit `1d0d1f6`）。

## 2. 工具链版本（实跑）

| 项                   | 值                                                                              |
| -------------------- | ------------------------------------------------------------------------------- |
| Node                 | v22.15.0（x64, Windows 10.0.26200）                                             |
| npm                  | 11.3.0                                                                          |
| package manager 字段 | **未声明**（本轮补：`engines` + `packageManager`）                              |
| lockfile             | `core/package-lock.json` 已提交（256 packages）                                 |
| TypeScript           | 5.9.3（dev）                                                                    |
| ESLint               | 10.10.0 + typescript-eslint 8.70.0（dev，flat config + typed rules）            |
| Prettier             | 3.9.6（dev）                                                                    |
| Vitest               | 3.2.7 + @vitest/coverage-v8 3.2.7（dev）                                        |
| 运行时依赖           | hash-wasm 4.12.0（唯一 runtime dep）                                            |
| devDependencies      | @types/node 22.20.1, globals 17.12.0, iconv-lite 0.7.3（仅 fixture 生成脚本用） |

## 3. 现有 scripts 清单（core/package.json）

```
test / test:watch / test:coverage / test:perf
typecheck / lint / lint:fix
format / format:check
check:architecture（node scripts/check-architecture.mjs）
check:secrets（node scripts/check-secrets.mjs）
check = format:check && lint && typecheck && test && check:architecture && check:secrets
check:full = check && test:coverage && test:perf
```

缺口（本轮补）：`check:invariants`、`check:network`、`check:db-integrity`（或测试等价物）、
循环依赖检查、property/fuzz 套件入口、`check:full` 未含 integration/contract/migration/依赖审计。

## 4. 实跑基线（2026-09-13，Engineering Baseline 启动时）

- `npm run check`：**PASS**
  - format:check PASS（prettier）
  - lint PASS（0 errors / 0 warnings）
  - typecheck PASS（strict 全开）
  - tests：**273/273 PASS，0 skip**（22 个测试文件）
  - check:architecture PASS（35 files）
  - check:secrets PASS（249 files，0 production secrets）
- 测试文件分层：`tests/{unit,parser,crypto,impact,domain,repository,sources,integration,perf}`

## 5. 源码标记扫描（src/，实跑 grep）

| 扫描项                                                               | 结果                                                                               |
| -------------------------------------------------------------------- | ---------------------------------------------------------------------------------- |
| TODO / FIXME / HACK / XXX / TEMP / PLACEHOLDER                       | **0 处真命中**（`XXX` 命中 6 处均为 ISO-4217「未知货币」合法码 `'XXX'`，非占位符） |
| `: any` / `as any`                                                   | **0**（src）                                                                       |
| `unknown as X`                                                       | **0**（src）                                                                       |
| `@ts-ignore` / `@ts-expect-error`                                    | **0**（src + tests）                                                               |
| 非空断言 `!`（critical path）                                        | 0（RC 轮已清零，eslint 禁止）                                                      |
| `console.*`                                                          | **0**（src；仅 scripts/ CLI 工具允许）                                             |
| `Date.now` / `Math.random`                                           | **0**（src）                                                                       |
| 硬编码绝对路径（`E:\` / `C:\` 等）                                   | 0（scripts 以 `import.meta.url` 解析根）                                           |
| 网络调用（fetch/axios/uni.request/XMLHttpRequest/URLSession/OkHttp） | **0**（src）                                                                       |
| eslint-disable 整文件豁免                                            | **0**（eslint.config.js 注释明确：无生成代码，无文件级豁免）                       |

## 6. TypeScript strict 现状（tsconfig.json）

已开启：`strict`、`noUncheckedIndexedAccess`、`noImplicitAny`、`noImplicitReturns`、
`noImplicitOverride`、`noFallthroughCasesInSwitch`、`verbatimModuleSyntax`、`isolatedModules`、
`forceConsistentCasingInFileNames`、`allowImportingTsExtensions`、`noEmit`。

未开启：`exactOptionalPropertyTypes`（false，显式）、`useUnknownInCatchVariables`（被 strict 蕴含开启）。
本轮评估 `exactOptionalPropertyTypes` 开启成本。

## 7. 测试与覆盖现状（MVP02 收口轮实测口径）

- 273 tests / 22 files / 0 skip
- coverage（src 口径 ≈90.6%）：crypto 98.7 / schema 100 / domain 100（registry 100%）/
  parser 96.9 / services 95.4 / fingerprint 94.2 / impact 92.4 / repos 91.5 / sources 84.6
- 已有测试类型：unit / integration / contract（H0 adapter 契约）/ migration（v1→v2 + ×50 幂等 +
  T4/T4b/T5）/ determinism（×20/×50）/ idempotency / negative / fuzz smoke / perf smoke / E2E
- 已有 fixture：微信 12 + generic-csv 10 + ofx 8 组 synthetic fixture（含 GB18030/BOM/CRLF 特例）

## 8. 依赖审计（实跑，网络可用）

- `npm ls --depth=0`：树干净，10 个直接依赖（1 runtime + 9 dev）
- `npm audit`：**3 moderate**，全部在 dev-only 链（vitest 2.1.0-beta.1–4.1.10 → @vitest/mocker）；
  修复需 breaking（vitest@5）。结论：不阻塞（无生产运行时暴露），如实记录。
- `npm outdated`：@types/node 22.20.1→22.20.2（patch）；typescript 5.9.3→7.0.2（major）；
  vitest 3.2.7→5.0.0（major）。本轮不升 major，记录即可。

## 9. 已有安全/隐私基建（RC + MVP02 轮产出，文档在 docs/）

- `check-architecture.mjs`：4 条规则（core↛app/platform、domain↛impl、crypto↛business、node:sqlite 仅 db/）
- `check-secrets.mjs`：模式扫描，249 files，0 production secrets
- docs：NETWORK_AUDIT / PRIVACY_DATAFLOW_AUDIT / LOGGING_AUDIT / SECRET_SCAN_REPORT /
  FAIL_CLOSED_AUDIT / TYPE_SAFETY_AUDIT / COVERAGE_REPORT / DEPENDENCY_AUDIT /
  TEST_MATRIX(_MVP02) / CLEAN_CLONE_REPORT / PERFORMANCE_SMOKE / VERSION_MATRIX 等 38 份

## 10. 本轮（Engineering Baseline V1）待建清单

1. `check:invariants` / `check:network` / `check:db-integrity` 脚本或测试入口 + 循环依赖检查
2. docs：ENGINEERING_STANDARDS / ARCHITECTURE_RULES / TEST_STRATEGY / TYPE_SAFETY_BASELINE /
   DEAD_CODE_AUDIT / COVERAGE_POLICY / FAIL_CLOSED_MATRIX / LOGGING_POLICY / MONEY_CURRENCY_RULES /
   PERFORMANCE_BASELINE / MEMORY_DATA_LIFETIME / DEPENDENCY_POLICY /
   SECURITY_PRIVACY_REGRESSION_MATRIX / FLAKY_TEST_REPORT / FIXTURE_POLICY /
   DEFINITION_OF_DONE / AGENT_DEVELOPMENT_PROTOCOL / COMMIT_CONVENTION / CHANGE_RISK_POLICY /
   CLEAN_CLONE_BASELINE / MUTATION_TEST_REPORT
3. 测试补强：invariant suite、contract harness 参数化三 Adapter、property-based（fast-check 评估）、
   crypto mutation/fuzz 扩充、determinism/idempotency 补口
4. 统一命令 `check` / `check:full` 收口（engines + packageManager 字段）
5. QUALITY_GATES_V1.md + ENGINEERING_BASELINE_V1_REPORT.md + README 命令复核（README 中
   `npm test # 259 tests` 已过期 → 实测 273，本轮修正）
