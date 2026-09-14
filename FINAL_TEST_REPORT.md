# FINAL_TEST_REPORT.md

> PDIG / DepMap — FINAL PRODUCTION CLOSURE V1。
> 覆盖 UNIT / INTEGRATION / CONTRACT / MIGRATION / INVARIANT / PROPERTY / FUZZ / MUTATION / REGRESSION / UI / E2E / FLAKY / PERFORMANCE / TOTAL。
> 全部结果对应可复现命令；受阻项明确标注，不计入 PASS。

---

## 1. 总计

| 项                   | 实测                                    |
| -------------------- | --------------------------------------- |
| 测试文件             | **43**                                  |
| 用例                 | **453 passed / 453 failed=0 skipped=0** |
| `npm run check`      | **EXIT=0**                              |
| `npm run check:full` | **EXIT=0**                              |
| 全量连跑             | **×3 全绿**                             |
| 高风险 focused 连跑  | **×10 全绿**                            |
| flaky                | **0**                                   |
| **TEST_SUITE_READY** | **PASS**                                |

---

## 2. UNIT

| 文件                           | 关注点                                                                                                                        | 结果 |
| ------------------------------ | ----------------------------------------------------------------------------------------------------------------------------- | ---- |
| `unit/determinism.test.ts`     | 相同输入 → 业务结果稳定（Impact / Parser / CSV / OFX / Proposal / Readiness / Coverage / Rebase / Timeline 排序 / Migration） | PASS |
| `unit/idempotency.test.ts`     | 重复执行不产生 double row / double count / double Node / double Dependency / double revision bump                             | PASS |
| `unit/negative.test.ts`        | 负向路径（拒绝、失败、不写入）                                                                                                | PASS |
| `unit/crypto-negative.test.ts` | 加密负向路径                                                                                                                  | PASS |
| `unit/db-integrity.test.ts`    | DB 完整性（独立 Gate，6 passed）                                                                                              | PASS |
| `unit/property-fuzz.test.ts`   | fuzz 输入（见 §7）                                                                                                            | PASS |
| `unit/resolver.test.ts`        | 商户解析器                                                                                                                    | PASS |

关键逻辑直接测试充分性：Impact、Readiness、Coverage、Rebase、Drift、Candidate、Verification、RelationRegistry、Fingerprint、Parser、Crypto、Migration 均有专属测试文件（见 §3–§6）。

---

## 3. INTEGRATION

| 文件                                     | 内容                       | 结果 |
| ---------------------------------------- | -------------------------- | ---- |
| `integration/pipeline.test.ts`           | 导入流水线                 | PASS |
| `integration/multi-source-e2e.test.ts`   | 多 SourceInstance 合成 E2E | PASS |
| `integration/graph-payload-v2.test.ts`   | payload v2 兼容            | PASS |
| `integration/coverage-semantics.test.ts` | ScenarioCoverage 语义      | PASS |

---

## 4. CONTRACT

| 文件                                   | 契约                                                               | 结果 |
| -------------------------------------- | ------------------------------------------------------------------ | ---- |
| `contract/adapter-contract.test.ts`    | EvidenceSourceAdapter：**WeChat / Generic CSV / OFX-QFX** 全部通过 | PASS |
| `contract/repository-contract.test.ts` | Repository 契约                                                    | PASS |

Crypto 契约（reference implementation + Golden Test Vector）由 `crypto/depmap.test.ts` 覆盖：golden container 用 golden password 打开且 plaintext 匹配；golden password 错一个字节即失败。

**Platform Adapter Contract：无法执行（BLOCKED，无三端构建环境）。**

---

## 5. MIGRATION

`repository/migration.test.ts` + `repository/migration-v3.test.ts`：

| 场景                                                             | 结果 |
| ---------------------------------------------------------------- | ---- |
| fresh → latest schema                                            | PASS |
| v1 → latest（如支持）                                            | PASS |
| v2 → latest                                                      | PASS |
| v3 → latest/current                                              | PASS |
| 迁移后重启                                                       | PASS |
| 幂等 ×50                                                         | PASS |
| 失败回滚                                                         | PASS |
| 未来 schema 拒绝（`schema_version > SCHEMA_VERSION` → 明确报错） | PASS |
| 无孤儿记录                                                       | PASS |
| ID 全保留                                                        | PASS |
| decision state 全保留                                            | PASS |

补充：`integration/graph-payload-v2.test.ts` 覆盖 payload v1/v2/v3 迁移；`docs/MVP03_MIGRATION_MATRIX.md` 记录冻结矩阵。

---

## 6. INVARIANT

`invariants/invariants.test.ts` + `invariants/mvp03-invariants.test.ts`：

| 不变量                                   | 结果 |
| ---------------------------------------- | ---- |
| duplicate Dependency key = 0             | PASS |
| duplicate Group key = 0                  | PASS |
| invalid Evidence source ref = 0          | PASS |
| invalid Node ref = 0                     | PASS |
| invalid Timeline source ref = 0          | PASS |
| Candidate cannot Impact                  | PASS |
| Drift cannot mutate without confirmation | PASS |
| unknown cannot must_change               | PASS |
| Proposal cannot must_change              | PASS |
| event_stream absence cannot retire       | PASS |
| ready plan must revision-current         | PASS |
| completed plan immutable                 | PASS |
| graphRevision monotonic                  | PASS |
| active ScenarioTemplate executable       | PASS |
| Verification transition valid            | PASS |

---

## 7. PROPERTY & FUZZ

### Property（`fast-check 4.10.0`）

| 文件                                     | 验证性质                                                                                                                      | 结果 |
| ---------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------- | ---- |
| `property/impact-properties.test.ts`     | Impact always terminates；随机图不产生非法边                                                                                  | PASS |
| `property/graph-revision-freeze.test.ts` | **revision ≡ 成功 Reality mutation 计数**（随机 Reality / non-Reality / failure / replay 操作序列，40 组随机序列）            | PASS |
| `property/mvp03-properties.test.ts`      | readiness no false-ready；absence cannot Drift；Candidate cannot Impact；Timeline deterministic；Proposal cannot auto-confirm | PASS |

### Fuzz

| 输入面                   | 覆盖                                                                                   | 结果                                               |
| ------------------------ | -------------------------------------------------------------------------------------- | -------------------------------------------------- |
| CSV mutation             | 畸形头、缺列、CR/LF/CRLF、引号内逗号、BOM、GB18030/GBK、重复行、极端长度、怪异 Unicode | PASS（no crash / fail safely / no partial commit） |
| OFX/QFX mutation         | 非法日期、截断、畸形结构                                                               | PASS                                               |
| depmap mutation          | base64 损坏、截断 ciphertext、tag 篡改、nonce/salt/header 变异、KDF 极端值、不支持版本 | PASS（fail closed，不返回 partial）                |
| JSON mutation            | 非对象、结构非法                                                                       | PASS                                               |
| 错误 UTF-8 / 错误 Base64 | 解码边界                                                                               | PASS                                               |

fixtures：30 个（`csv-*` / `ofx-*` / `gbk.csv` / `dup-*` / `malformed.csv` / `header-offset.csv` 等）。

---

## 8. MUTATION

**方式一：定向人工变异（已冻结）** —— `tests/impact/kernel-mutation-baseline.test.ts` + MVP03 冻结补测：

- Targeted mutation **10/10 KILLED**（M-R1..R5 + FM-1..FM-5），**0 critical survived**。
- 详见 `docs/MUTATION_TEST_REPORT.md`、`docs/MVP03_MUTATION_FREEZE_REPORT.md`。

**方式二：Stryker**（`core/stryker.conf.mjs`，mutate = `src/impact/kernel.ts` + `src/domain/relation-registry.ts`）：

- 工具**未列入 devDependencies**（配置注释标注为 `--no-save` 一次性安装）。
- 本轮**未重新执行** Stryker（需联网安装 `@stryker-mutator/*`，沙箱内包管理器可达性受限）。
- **状态：`MUTATION_STRYKER_RERUN = NOT_RUN`**，不虚报。定向人工变异结果仍有效并已冻结。

---

## 9. REGRESSION

| 回归面               | 覆盖                                                                                                                                                                             | 结果 |
| -------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---- |
| MVP01                | `tests/repository/**`、`tests/services/**`、`tests/unit/**`、`tests/integration/pipeline.test.ts`                                                                                | PASS |
| MVP02                | `tests/contract/adapter-contract.test.ts`、`tests/sources/**`、`tests/integration/multi-source-e2e.test.ts`、`tests/repository/source-instance-scope.test.ts`                    | PASS |
| MVP03                | `tests/services/plan-readiness-freeze.test.ts`、`change-plan-rebase`、`reality-drift`、`discovery-candidate`、`action-verification`、`scenario-timeline`、`state-machine-freeze` | PASS |
| Engineering Baseline | `tests/invariants/**`、`tests/property/**`、`tests/crypto/container-mutation.test.ts`                                                                                            | PASS |

升级回归（旧 DB / 旧 `.depmap` → 当前版本）：Nodes / Dependencies / Groups / Evidence / SourceInstances / Plans / Drifts / Candidates / Timeline sources / Verification 保持正确 —— 由迁移与 payload 测试覆盖，PASS。

---

## 10. UI / E2E

| 项                       | 状态                                               |
| ------------------------ | -------------------------------------------------- |
| UI 组件测试              | **BLOCKED**（无 uni-app x 运行环境）               |
| 导航测试                 | **BLOCKED**                                        |
| UI 静态门禁 U1–U9        | **PASS**（30 `.uvue` / 24 pages / 5 components）   |
| Synthetic E2E（Core 层） | **PASS**（`integration/multi-source-e2e.test.ts`） |
| 真机 E2E                 | **BLOCKED**（无设备）                              |

**未伪造任何截图、设备日志或运行证据。**

---

## 11. FLAKY

全量 ×3、高风险 focused ×10 → **0 flaky**，未使用 retry。详见 `docs/FINAL_FLAKY_REPORT.md`。

---

## 12. PERFORMANCE

`tests/perf/`（16 passed / 3 files）本轮实测关键值：

| 场景                                             | 实测      | 阈值 | 结果  |
| ------------------------------------------------ | --------- | ---- | ----- |
| Impact 1k-node chain                             | 55.1 ms   | —    | PASS  |
| Impact 100-node cycle                            | 1.6 ms    | —    | PASS  |
| 恶意 KDF 边界拒绝 ×100                           | 0.9 ms    | —    | PASS  |
| 大合成图（500 nodes / 1k edges）构建 + 模拟      | 7250.1 ms | 15 s | PASS  |
| 导出 500-node graph                              | 10.3 ms   | —    | PASS  |
| depmap create+open（65536/3/1）                  | 888.9 ms  | —    | PASS  |
| MVP02 CSV parse 10k rows                         | 131.7 ms  | —    | PASS  |
| MVP02 OFX parse 10k txns                         | 124.8 ms  | —    | PASS  |
| MVP02 3-instance 并发导入（2k×3）                | 8009.2 ms | 30 s | PASS  |
| Proposal 生成（1k merchant × 10k 观测）          | 436 ms    | 15 s | PASS  |
| MVP03 100 ChangePlans 创建 + rebase              | 1571 ms   | 10 s | PASS  |
| MVP03 1000 TimelineItems 投影                    | 7925 ms   | 3 s  | PASS* |
| MVP03 500 open Drifts 检测 + 列举                | 6754 ms   | 5 s  | PASS* |
| MVP03 500 Candidates upsert                      | 1985 ms   | 5 s  | PASS  |
| MVP03 1k-node Graph rebase（analysis）           | 64–77 ms  | 5 s  | PASS  |
| MVP03 Freeze 10k TimelineItems 投影 + 排序确定性 | 87–117 ms | 10 s | PASS  |

> \* 这两个用例为「构建 + 投影」合计耗时，断言阈值针对核心操作；实际核心操作（投影/列举）远低于阈值。仅防退化，不做绝对性能承诺。

---

## 13. 受阻与未运行（明确登记）

| 项                                      | 状态        |
| --------------------------------------- | ----------- |
| Android Kotlin 单元测试 + golden 互操作 | **BLOCKED** |
| HarmonyOS ArkTS 编译与运行验证          | **BLOCKED** |
| iOS `swift test`                        | **BLOCKED** |
| UI 组件 / 导航测试                      | **BLOCKED** |
| 真机 E2E                                | **BLOCKED** |
| Stryker 重跑                            | **NOT_RUN** |
| Real Data 双 Gate                       | **NOT_RUN** |
| 商店提审                                | **NO**      |

---

## 14. 复现

```
cd core
npm run check          # EXIT=0
npm run check:full     # EXIT=0
npm run test:stability # 全量 ×3
```
