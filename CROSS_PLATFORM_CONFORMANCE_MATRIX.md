# CROSS_PLATFORM_CONFORMANCE_MATRIX.md

> 数据来源：`conformance/reports/android.json`、`conformance/reports/SUMMARY.json`
> 状态：`PASS`（harness 独立复核通过）/ `NOT_RUN` / `FAIL`

更新时间：2026-09-17 · **D-16 关闭轮复跑：pass=91 fail=0 notImplemented=0 total=91（无回归）** · 用例总数 **91** · oracle 提交 `6d268c0`

> **计数修正（本轮）**：本文档早期版本的"用例总数 64"只统计了 §2–§8，
> 漏掉了后续并入的 Parser（22）、Timeline（3）、Migration/Backup（2）。
> 与 `conformance/reports/SUMMARY.json` 及 `./gradlew :conformance:run` 的
> `pass=91 fail=0 notImplemented=0 total=91` 对齐后，总数修正为 **91**。
> 分类加总核对：13 + 16 + 6 + 18 + 3 + 1 + 7 + 22 + 3 + 2 = **91**。
>
> **本轮（2026-09-17 第二场：Android 冻结 + Harmony N3 开工）复跑**：
> `cd android && ./gradlew --no-daemon :core:test :conformance:run`
> → `:core:test` **71/71**、conformance **`pass=91 fail=0 notImplemented=0 total=91`**（**本轮重新执行**，非沿用）。
> `:app:testDebugUnitTest` **9/9**、设备内 androidTest **51/51**、`:core:test` **71/71** 亦为本轮实跑。
> 详见 `ANDROID_NATIVE_CORE_FREEZE.md` §3。

> **Harmony 列本轮细化（N3 开工）**：
> `HARMONY_CONFORMANCE = NOT_RUN` —— **0 个用例被执行**。
> 无设备、无模拟器系统镜像（`hdc list targets = [Empty]`），且未接本地测试框架。
> 按分类拆分：**87 notImplemented / 4 blocked**（depmap 3 + backup 1，因 `cryptoFramework` 无 Argon2）。
> 见 `HARMONY_N3_CONFORMANCE_REPORT.md`。

> **⚠ 不得误用的正数**：`node tools/harmony/check-relations-semantics.mjs` 输出 **18/18**，
> 但它是**源码语义镜像**（Node 等价实现 vs 同批 fixtures），**不是 ArkTS 运行时执行**，
> 因此 **不写入本矩阵，也不计入 HARMONY_CONFORMANCE**。

---

## 1. 汇总

| 平台   | PASS | FAIL | NOT_IMPLEMENTED | NOT_RUN | 合计 |
| ------ | ---- | ---- | --------------- | ------- | ---- |
| Android | **91** | 0 | 0 | 0 | 91 |
| Harmony | 0 | 0 | 87 | 4（blocked：depmap 3 + backup 1，无 Argon2） | 91 |
| iOS     | 0 | 0 | 0 | 91 | 91 |

> Android 侧的 91/91 是**领域 / 语义 / 解析 / 迁移 / 备份**层的 conformance 结果，
> **不等于**端到端 parity——设备级运行时结论见 `NATIVE_PARITY_MATRIX.md`（**62/73**）与
> `ANDROID_N1_N2_FINAL_CLOSURE_REPORT_V2.md`。

---

## 2. Impact（13 例）

| 用例 ID                             | 覆盖语义                              | Android | Harmony | iOS |
| ----------------------------------- | ------------------------------------- | ------- | ------- | --- |
| `impact-required-edge-loss`         | required 边失效 + 下游级联             | PASS | NOT_RUN | NOT_RUN |
| `impact-unknown-criticality`        | unknown 只 needs_review（INV12）       | PASS | NOT_RUN | NOT_RUN |
| `impact-any-group-covered`          | ANY 组仍有可用成员 → backup_path        | PASS | NOT_RUN | NOT_RUN |
| `impact-any-group-failed`           | ANY 组全部失效 → must_change            | PASS | NOT_RUN | NOT_RUN |
| `impact-all-group-unsatisfied`      | ALL 组缺一 → must_change                | PASS | NOT_RUN | NOT_RUN |
| `impact-cycle-safe`                 | 环终止；processedKeys 唯一              | PASS | NOT_RUN | NOT_RUN |
| `impact-cycle-cascade`              | 真环级联 must_change 后终止              | PASS | NOT_RUN | NOT_RUN |
| `impact-dense-cycle-uncertainty-only` | 稠密环只传播不确定性（IMP-07）         | PASS | NOT_RUN | NOT_RUN |
| `impact-retired-edge`               | retired 不传播（IMP-04）                | PASS | NOT_RUN | NOT_RUN |
| `impact-proposal-only`              | confidence 0.999 仍只 needs_review      | PASS | NOT_RUN | NOT_RUN |
| `impact-multiple-alternatives`      | 未确认备用组合 → needs_review            | PASS | NOT_RUN | NOT_RUN |
| `impact-deterministic-ordering`     | 排序 + 原始操作最后                     | PASS | NOT_RUN | NOT_RUN |
| `impact-non-payment-capability-ignored` | 非 payment 不参与传播               | PASS | NOT_RUN | NOT_RUN |

---

## 3. Readiness（16 例）

| 用例 ID                                     | 覆盖语义                       | Android | Harmony | iOS |
| ------------------------------------------- | ------------------------------ | ------- | ------- | --- |
| `readiness-one-target-multiple-actions`     | 全部声明动作完成才 resolved      | PASS | NOT_RUN | NOT_RUN |
| `readiness-one-target-multiple-actions-partial` | 只完成其一 → blocked（禁减法） | PASS | NOT_RUN | NOT_RUN |
| `readiness-multiple-targets-shared-action`  | 共享动作覆盖多 target            | PASS | NOT_RUN | NOT_RUN |
| `readiness-unrelated-completed-action`      | 无关动作不解决 requirement       | PASS | NOT_RUN | NOT_RUN |
| `readiness-unresolved-must-change`          | blocked                        | PASS | NOT_RUN | NOT_RUN |
| `readiness-unknown-criticality-review`      | review_required                | PASS | NOT_RUN | NOT_RUN |
| `readiness-pending-proposal`                | review_required                | PASS | NOT_RUN | NOT_RUN |
| `readiness-unresolved-candidate`            | review_required                | PASS | NOT_RUN | NOT_RUN |
| `readiness-revision-mismatch`               | 派生 needs_revalidation         | PASS | NOT_RUN | NOT_RUN |
| `readiness-stale-dependency`                | review_required                | PASS | NOT_RUN | NOT_RUN |
| `readiness-unfinished-change-action`        | review_required                | PASS | NOT_RUN | NOT_RUN |
| `readiness-all-resolved`                    | ready_with_known_scope         | PASS | NOT_RUN | NOT_RUN |
| `readiness-confidence-cannot-bypass`        | 无 confidence 通道              | PASS | NOT_RUN | NOT_RUN |
| `readiness-absence-cannot-help`             | 无 absence 通道                 | PASS | NOT_RUN | NOT_RUN |
| `readiness-blocked-beats-review`            | blocked 优先                    | PASS | NOT_RUN | NOT_RUN |
| `readiness-completed-empty-claim-not-retroactive` | 禁止事后追认 key           | PASS | NOT_RUN | NOT_RUN |

---

## 4. Coverage（6 例）

| 用例 ID                              | 覆盖语义                    | Android | Harmony | iOS |
| ------------------------------------ | --------------------------- | ------- | ------- | --- |
| `coverage-unknown`                   | 无来源无直接依赖 → unknown   | PASS | NOT_RUN | NOT_RUN |
| `coverage-limited-stale-sources`     | 来源过期 → limited           | PASS | NOT_RUN | NOT_RUN |
| `coverage-limited-no-direct-deps`    | 无直接依赖 → limited         | PASS | NOT_RUN | NOT_RUN |
| `coverage-partial-pending-proposal`  | 有未决信号 → partial         | PASS | NOT_RUN | NOT_RUN |
| `coverage-well-evidenced`            | 全部确认 → well_evidenced    | PASS | NOT_RUN | NOT_RUN |
| `coverage-never-ingested-is-stalest` | 从未导入 = 最陈旧            | PASS | NOT_RUN | NOT_RUN |

---

## 5. Relations（18 例）

| 用例组                          | 数量 | 覆盖语义                                      | Android | Harmony | iOS |
| ------------------------------- | ---- | --------------------------------------------- | ------- | ------- | --- |
| `relation-funding-source-*`     | 3    | funding_source 合法组合                        | PASS | NOT_RUN | NOT_RUN |
| `relation-merchant-agreement-*` | 2    | merchant_agreement 合法组合                    | PASS | NOT_RUN | NOT_RUN |
| `relation-reject-non-runtime-*` | 2    | `bound_to` / `verifies` 不在 runtime registry | PASS | NOT_RUN | NOT_RUN |
| `relation-reject-unknown-*`     | 1    | 未知 relation                                  | PASS | NOT_RUN | NOT_RUN |
| `relation-reject-capability-*`  | 1    | capability 不匹配                              | PASS | NOT_RUN | NOT_RUN |
| `relation-reject-from-kind*`    | 2    | fromKind 不允许（`device` / `identity_anchor`）| PASS | NOT_RUN | NOT_RUN |
| `relation-reject-to-kind*`      | 2    | toKind 不允许（`device` / `custom`）           | PASS | NOT_RUN | NOT_RUN |
| `relation-allow-null-kinds`     | 1    | kind 未知时放行（仅校验 relation+capability）  | PASS | NOT_RUN | NOT_RUN |
| `relation-group-*`              | 4    | Group 模式校验（ANY/ALL、merchant 不允许 Group）| PASS | NOT_RUN | NOT_RUN |

---

## 6. DEPMap / Crypto（3 例）

| 用例 ID                                    | 覆盖语义                                   | Android | Harmony | iOS |
| ------------------------------------------ | ------------------------------------------ | ------- | ------- | --- |
| `depmap-golden-v1`                         | derivedKey / ciphertext / tag / container 逐字节一致；错误口令与篡改均 auth_failed | **PASS** | NOT_RUN | NOT_RUN |
| `depmap-utf8-password-normalization`       | ASCII/中文/emoji/combining/NFC；combining ≠ NFC | **PASS** | NOT_RUN | NOT_RUN |
| `depmap-bounds-and-structure-rejection`    | 16 种恶意/非法容器全部 fail closed          | **PASS** | NOT_RUN | NOT_RUN |

**这是本次迁移最硬的证据**：Kotlin 侧用 BouncyCastle Argon2 + JDK JCE 复现了
Node reference 的 `DEPMAP_CONTAINER_V1` 输出，逐字节相同。

---

## 7. JCS（1 例）

| 用例 ID                          | 覆盖语义                              | Android | Harmony | iOS |
| -------------------------------- | ------------------------------------- | ------- | ------- | --- |
| `jcs-rfc8785-restricted-domain`  | 9 个规范用例 + 浮点拒绝（不静默序列化） | PASS | NOT_RUN | NOT_RUN |

---

## 8. Scenario / Migration / State Machines（7 例）

| 用例 ID                                  | 覆盖语义                        | Android | Harmony | iOS |
| ---------------------------------------- | ------------------------------- | ------- | ------- | --- |
| `scenario-template-policy`               | 3 active / planned 不可执行      | PASS | NOT_RUN | NOT_RUN |
| `migration-version-contract`             | v1→v3 链、payload 契约、未来版本拒绝 | PASS | NOT_RUN | NOT_RUN |
| `state-machine-change-plan`              | 迁移表 + 终态 + 派生状态 + 守卫   | PASS | NOT_RUN | NOT_RUN |
| `state-machine-reality-drift`            | 迁移表 + 创建规则 + 守卫          | PASS | NOT_RUN | NOT_RUN |
| `state-machine-discovery-candidate`      | 迁移表 + accept/dismiss 效应      | PASS | NOT_RUN | NOT_RUN |
| `state-machine-action-verification`      | 迁移表 + evidence 信号规则         | PASS | NOT_RUN | NOT_RUN |
| `state-machine-graph-revision`           | bumpsOn / neverBumpsOn / 原子性    | PASS | NOT_RUN | NOT_RUN |

---

## 9. Parser（22 例，全部 CONFORMANCE_PASS on Android）

| 用例 ID | 覆盖语义 | Android | Harmony | iOS |
| ------- | -------- | ------- | ------- | --- |
| `parser-wechat-normal` / `-utf8-bom` / `-gb18030` / `-header-offset` / `-malformed` / `-refund` | 微信账单：正常/BOM/GB18030/表头偏移/坏行拒绝/退款 | PASS | NOT_RUN | NOT_RUN |
| `parser-csv-crlf` / `-cr-only` / `-quoted-comma` / `-multi-currency` / `-eu-semicolon` / `-debit-credit` / `-us-signed` / `-gb18030` | 通用 CSV：CRLF/CR-only/引号内逗号/多币种/分号/借贷列/带符号/GB18030 | PASS | NOT_RUN | NOT_RUN |
| `parser-ofx-basic` / `-xml-multiple` / `-missing-fitid` / `-invalid-date` / `-negative-positive` / `-malformed` / `parser-qfx-basic` / `-duplicate-fitid` | OFX/QFX：SGML/XML、缺 FITID、非法日期、正负金额、坏块、重复 FITID | PASS | NOT_RUN | NOT_RUN |

## 10. Timeline（3 例）

| 用例 ID | 覆盖语义 | Android | Harmony | iOS |
| ------- | -------- | ------- | ------- | --- |
| `timeline-buckets-and-ordering` | 7 分桶边界 + 确定性排序 + 两次构建一致 | PASS | NOT_RUN | NOT_RUN |
| `timeline-attention-signals` | drift / freshness / expiry / needs_attention 四类信号 | PASS | NOT_RUN | NOT_RUN |
| `timeline-terminal-plans-excluded` | completed / cancelled 不产生时间线项 | PASS | NOT_RUN | NOT_RUN |

## 11. Migration / Backup（2 例）

| 用例 ID | 覆盖语义 | Android | Harmony | iOS |
| ------- | -------- | ------- | ------- | --- |
| `migration-db-v1-to-v3` | 版本到 3、legacy 归属、ID/决策保留、指纹作用域重建、50 次重复执行 no-op | PASS | NOT_RUN | NOT_RUN |
| `backup-depmap-export-restore-roundtrip` | 导出→加密→解密→恢复→再导出**逐字节相同** + 无孤儿引用 | PASS | NOT_RUN | NOT_RUN |

---

## 12. 尚缺的 Conformance 维度（诚实清单）

- **Backup / Restore 跨端互通矩阵**（A→A、A→H、H→A、I→A、A/H→I）：协议与黄金向量已一致，
  但 Harmony / iOS 客户端尚未实现，无法产生对端报告。
- 三端 **differential testing**（同 fixture 三端输出 normalized JSON 自动 diff）：等 N3/N4 完成后接入。

---

## 13. P0 轮（2026-09-16）回归记录

| 项 | 结果 |
| --- | --- |
| `:conformance:run` | **pass=91 fail=0 notImplemented=0 total=91**（无回归，与 N1/N2 轮一致） |
| `:core:test`（新增） | **71 / 71 PASS** |
| `:app:connectedDebugAndroidTest` | **19 / 19 PASS** |
| `:app:assembleDebug` | BUILD SUCCESSFUL，APK `bf378ec6…305ff1`（36,887,249 B） |
| `:app:testDebugUnitTest` | **NO-SOURCE**（如实记录，不冒充通过） |
| 真机核心行程 | **21 / 21 PASS**，崩溃 0 |

本轮**未新增 conformance 用例**，91 这个数字没有变化。
Harmony / iOS 两列仍为 `NOT_RUN`（未进入 N3 / N4，遵守 stop condition）。
