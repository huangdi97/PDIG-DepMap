# WORK_STATUS.md

> 本文件由执行 Agent 持续更新。不要删除历史关键结论。
> 分支历史：`feat/mvp02-global-source`（MVP02，tag v0.2.0-mvp02）→ `engineering/baseline-v1`
> （Engineering Baseline V1 PASS，2026-09-13）→ **`feat/mvp03-living-graph`（当前）**。

## Current

- Phase: **MVP03 — Living Graph & Change Safety：Core 代码侧 PASS**（2026-09-13 收口）
- Status: 427/427 tests PASS（39 文件，0 skip）；check / check:full / stability ×3 全绿；
  Real Data **NOT_RUN**；平台编译 **BLOCKED**（B1–B3/B10）
- 详细报告：**MVP03_FINAL_REPORT.md** / MVP03_ACCEPTANCE.md（全 Gate 勾选）/
  docs/MVP03_TEST_MATRIX.md / docs/LIVING_GRAPH.md

## Current quality state（MVP03 收口轮实跑）

- format / lint（0 errors 0 warnings）/ typecheck（strict + noUncheckedIndexedAccess +
  exactOptionalPropertyTypes）PASS
- architecture PASS（48 files，circular = 0）；network gate PASS（103 files，0 原语）；
  secret scan PASS（331 files，0 production secrets）
- coverage：src line 93.45% / branch 81.4%（scenarios 98.7 / repositories 92.15；
  Baseline Gate 已更新 COVERAGE_POLICY.md）
- stability：3 连跑全绿（0 flaky / 0 retry）
- 人工变异 M-R1..M-R5（revision bump / blocked 规则 / drift 阈值 / rebase no-op /
  candidate 重提）**5/5 KILLED**；fast-check PI-1..3（seed=20260913）
- perf（§63）：100 plans 0.9s / 1k timeline 14ms / 500 drifts ~4.5s / 500 candidates ~2.3s /
  1k-node rebase 76ms

## MVP03 交付（Gate 级）

| Gate | 结果 |
|---|---|
| A Graph Revision（GR-001..012，同事务 bump） | PASS |
| B ChangePlan Rebase（PRB-001..011） | PASS |
| C PlanReadiness（三值纯规则，无 confidence/absence 通道） | PASS |
| D ScenarioCoverage（四级 + 可解释） | PASS |
| E RealityDrift（RD-001..010，absence 永不触发） | PASS |
| F DiscoveryCandidate（不进 Impact / 不 bump revision） | PASS |
| G ScenarioTemplate（3 active + planned gate + 政策） | PASS |
| H Timeline（确定性投影，可溯源） | PASS |
| I Verification（done ≠ verified，两段式） | PASS |
| J Migration v2→v3（MIG3-001..006） | PASS |
| K depmap compat（golden 不变；payload v3 + v1/v2 migrate） | PASS |
| L/M/N 回归（MVP01/MVP02/Baseline） | PASS |
| O Security/Privacy（新对象只存引用/ID） | PASS |
| P UI（16 页源码级，编译 BLOCKED B10） | PASS（静态） |
| Q Documentation（13 份 + README + CANONICAL 附录） | PASS |

## MVP03 生产代码变更

- Schema v3：change_plans / reality_drifts / discovery_candidates 三张新表 + 索引；
  SCHEMA_VERSION = 3；DEPMAP_CONTAINER_V1 不变
- graphRevision：meta.graph_revision，仅 Reality mutation 同事务 +1
  （dependency confirm-insert/reactivate/retire/updateCriticality 值变、group confirm/retire/reactivate）
- payload v3：graph_revision 随 meta 行；migratePayloadV2toV3 + v1→v2→v3 组合（in-memory）
- 并发协作说明：reality-drift-service 的阈值语义（「阈值只挡新建；open drift 恒累计」）
  与 upsertSignal 的 `changed` 返回值为协作编辑成果，已被 RD 测试覆盖

## Platform Matrix

| Platform | IMPLEMENTED | STATIC_AUDITED | COMPILED | TESTED | DEVICE_VERIFIED | STORE_READY |
|---|---|---|---|---|---|---|
| Android | YES | YES | NO（B1） | NO（B1） | NO | NO |
| HarmonyOS | YES | YES | NO（B2） | NO（B2） | NO | NO |
| iOS | YES | YES | NO（B3） | NO（B3） | NO | NO |
| UI（uni-app x 16 页） | YES | YES | NO（B10） | — | NO | NO |
| Core（Node） | YES | YES | YES | YES（427） | N/A | N/A |

## 仓库运维注意（重要）

分支 loose ref（`.git/refs/heads/<branch>/`）在本工作区会被外部进程反复删除。
**规避：分支 ref 固化在 `.git/packed-refs`。** 若出现"branch has no commits"，
从 reflog 找回哈希后重写 packed-refs；**不要**执行 `git reset --hard` / `git clean`。

## Current failures

无失败测试。未执行项全部为外部工具链 Blocker（B1–B3/B10）或 Real Data（NOT_RUN），不虚报。

## External blockers

见 `BLOCKERS.md`（B1–B3、B10 阻断编译；B4–B9/B11–B13 发布材料）。

## Next

1. **MVP04 — International Payment Infrastructure**（未启动；候选：PayPal / card_on_file /
   direct_debit_mandate / payout_destination / card updater semantics；等用户发起）
2. B1 → Android 编译 + golden（docs/ANDROID_TOOLCHAIN_SETUP.md）
3. B10 → HBuilderX 基座 → UI 编译 + 真机 spike（16 页 MVP03 UI 待编译验证）
4. B13 → 真实账单双 Gate（validate-real-bill.ts 就绪；Real Data 保持 NOT_RUN）
5. 变异：下轮可选对 drift/readiness 仓库层跑 Stryker 定向基线
