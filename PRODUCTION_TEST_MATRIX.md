# PRODUCTION_TEST_MATRIX.md — PDIG Production RC V1 测试矩阵

> 覆盖层：Unit / Integration / Contract / Migration / Invariant / Property / Fuzz / Mutation(targeted) /
> UI-Component / Navigation / E2E(synthetic) / Upgrade / Security / Performance / Platform-Build / Platform-Smoke
> 状态：`PASS` / `FAIL` / `BLOCKED` / `NOT_RUN`

---

## 1. Core 层（Node 22，可执行）

| 层 | 位置 | 状态 | 证据 |
|---|---|---|---|
| Unit | `core/tests/unit/**` | PASS | 含 negative / determinism / idempotency / property-fuzz |
| Domain | `core/tests/domain/relation-registry.test.ts` | PASS | registry 100% |
| Impact | `core/tests/impact/kernel.test.ts` | PASS | T1–T12 + determinism |
| Parser | `core/tests/parser/wechat.test.ts` | PASS | — |
| Sources | `core/tests/sources/{generic-csv,ofx-qfx}.test.ts` | PASS | — |
| Contract | `core/tests/contract/**` | PASS | Adapter C0–C6；Repository R1–R7 |
| Crypto | `core/tests/crypto/**` | PASS | golden + container-mutation |
| Migration | `core/tests/repository/migration*.test.ts` | PASS | v1→v2、v2→v3、×50、rollback、future reject |
| Invariant | `core/tests/invariants/**` | PASS | INV1–INV21 |
| Property | `core/tests/property/**` | PASS | fast-check seed=20260913；FR-GR-012 40 序列 |
| Services | `core/tests/services/**` | PASS | readiness / coverage / rebase / drift / candidate / verification / timeline / state-machine |
| Integration | `core/tests/integration/**` | PASS | pipeline / multi-source-e2e / coverage-semantics / graph-payload-v2 |
| Performance | `core/tests/perf/**` | PASS | perf smoke + mvp03-perf + timeline-10k-freeze |
| Mutation (targeted) | `core/reports/mutation/` + `docs/MVP03_MUTATION_FREEZE_REPORT.md` | PARTIAL_WITH_REPORT | 10/10 targeted KILLED，0 critical survived |
| Coverage | 本轮实测（`local_private/check-full-rc1-committed.log`） | PASS | Statements **93.74%**（5437/5800）/ Branches **约 82.2%**（3 次实测 82.21–82.24）/ Functions **94.28%** |
| 质量套件（`npm run check:full`） | 本轮实测（提交树） | **PASS（FINAL_EXIT=0）** | format / lint / typecheck / test 453 / architecture 48 文件 circular 0 / network 118 文件 0 原语 / secrets 392 文件 0 / ui / db-integrity 6 / coverage / perf 16 / deps |
| 稳定性（`npm run test:stability`） | 本轮实测 | **PASS** | 全量 suite ×3 连续全绿，exit 0 |

## 2. UI 层（uni-app x，无编译器）

| 层 | 状态 | 说明 |
|---|---|---|
| UI-Component（编译执行） | **BLOCKED** | 无 HBuilderX（B10），`.uvue` 无法编译 |
| UI-Static（机械校验） | PASS | `core/scripts/check-ui.mjs` **9 类**：U1 pages.json↔文件、U2 tabBar、U3 禁词、U4 禁 raw SQL、U5 token 颜色、U6 未声明标识符、U7 组件存在、U8 状态处理、**U9 模板字段存在性**（24 页 / 30 `.uvue` / 0 命中） |
| U9 有效性验证 | PASS | 注入探针（`dep.peerName` 未声明字段）→ 门禁正确报 FAIL；探针已移除 |
| Navigation（静态） | PASS | 路由可达性 + 无 dead-end（静态图检查） |
| Visual Regression | **BLOCKED** | 无 Web/H5 运行环境（§73 允许在无环境时降级为 BLOCKED） |
| E2E（synthetic，UI） | **BLOCKED** | 同 B10 |

> **UI 层不可编译是本次 RC 最重要的诚实边界**：`.uvue` / UTS 从未经过编译器与真机验证，
> 所有 UI 层 `PASS` 均标注为「静态」。这是 B10 的直接后果，不是可以绕过的。

## 3. E2E（synthetic，Core 层可执行部分）

| 用例 | 状态 |
|---|---|
| E2E-1 首次使用（导入→Proposal→确认→图更新） | PASS（`multi-source-e2e` + `pipeline`） |
| E2E-2 换银行卡（Impact→readiness→actions→verification） | PASS（services 层） |
| E2E-3 卡到期（timeline→plan→impacts→verify） | PASS（`scenario-timeline`） |
| E2E-4 RealityDrift→needs_revalidation→rebase | PASS（`reality-drift` + `change-plan-rebase`） |
| E2E-5 Backup/Restore（export→import→migrate） | PASS（`graph-payload-v2` + crypto） |
| E2E-6 错误密码 | PASS（`crypto-negative`） |
| E2E-7 不支持版本 | PASS（migration future reject） |

> 说明：以上 E2E 在 **Core 应用服务层**通过；UI 端 E2E 因 B10 BLOCKED。

## 4. Upgrade / Downgrade

| 用例 | 状态 |
|---|---|
| MVP02 data → current app | PASS（v2→v3 migration 真实执行） |
| MVP03 data → current app | PASS |
| Downgrade | **不支持**（新 schema 不得被旧 app 打开；已显式拒绝） |

## 5. Security / Privacy

| 项 | 状态 |
|---|---|
| Secret scan | PASS（`check-secrets.mjs`） |
| Network scan | PASS（`check-network.mjs`，0 业务原语） |
| Logging scan | PASS（0 console 于生产路径） |
| Crypto golden | PASS |
| Permission audit | PASS（静态；真机待 B1/B2/B3） |
| Dependency / license | PASS（dev-only moderate 已登记） |

## 6. Platform

| 平台 | Build | Smoke |
|---|---|---|
| Android | **BLOCKED（B1）** | BLOCKED |
| HarmonyOS | **BLOCKED（B2）** | BLOCKED |
| iOS | **BLOCKED（B3）** | BLOCKED |

## 7. 稳定性

| 项 | 状态 | 证据 |
|---|---|---|
| 全量 suite ×3 | **PASS（本轮实测）** | `npm run test:stability` → 3 连跑全绿，exit 0（`local_private/stability-rc1.log`） |
| focused 关键套件 ×10 | PASS（**上一轮**证据） | MVP03 Freeze 轮已跑，0 flaky；**本轮未重跑**，故不作为本轮 PASS 依据 |
| Import / PlanReadiness / Rebase / Drift / Timeline / Backup-Restore focused | PASS（**上一轮**证据） | 同上 |
| clean install / clean clone | **本轮未重跑** | 上一轮 PASS；本轮 Core 依赖树未变（仅新增 2 个 scripts、调整 `package.json` scripts），**不作为本轮 PASS 依据** |
