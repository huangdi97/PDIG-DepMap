# CROSS_PLATFORM_CONFORMANCE_MATRIX.md

> 数据来源：`conformance/reports/android.json`、`conformance/reports/SUMMARY.json`
> 状态：`PASS`（harness 独立复核通过）/ `NOT_RUN` / `FAIL`

更新时间：2026-09-17 · **D-16 关闭轮复跑：pass=91 fail=0 notImplemented=0 total=91（无回归）** · 用例总数 **91** · oracle 提交 `7bc0ed3`

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
> 见 `HARMONY_N3_CONFORMANCE_REPORT.md`。
>
> **（2026-09-18 追加）Android 91/91 已获远真复验**：GitHub Actions 第三次运行
> （`35303432883`）在 hosted Linux runner 上实跑 `node tools/conformance/run.mjs`，
> 读数 `cases 91/91`、`imports 28/28`、`ORACLE SELFCHECK PASS`、`platform android PASS 91/91`、
> `VERDICT: PASS`。此前 CI 的 Canonical job 因 `conformance/reports/` 被 gitignore 而**永远看不到**
> Android 报告，只能报 `NOT_RUN`；现已通过 `needs: android-core` + artifact 下载修正为**真门禁**。
> 见 `GITHUB_PUBLICATION_REPORT.md` §6.4、§6.5。
>
> **（2026-09-18 追加）Harmony 的 depmap/backup 阻塞性质变化**：
> `cryptoFramework` / `HUKS` 无 Argon2 已证据级排除，且**原生路径已全链路打通**：
> 双 ABI 交叉编译通过、`libpdiargon2.so` 已打进 HAP、
> 打包并 strip 后的动态符号表恰好 3 个符号（`argon2_*` = 0）。
> 因此阻塞点从"**平台无能力**"变为"**无运行时**"：
> 分类由 `BLOCKED_BY_PLATFORM` 改为 **`BLOCKED_BY_RUNTIME`**。
> **四点仍记为 blocked，不因编译/打包通过而改判** —— 它们一次都没执行过。
> 见 `HARMONY_ARGON2_INTEGRATION_REPORT.md`、`HARMONY_RUNTIME_ENVIRONMENT_AUDIT.md`。

> **⚠ 不得误用的正数**：`node tools/harmony/check-relations-semantics.mjs` 输出 **18/18**，
> 但它是**源码语义镜像**（Node 等价实现 vs 同批 fixtures），**不是 ArkTS 运行时执行**，
> 因此 **不写入本矩阵，也不计入 HARMONY_CONFORMANCE**。
>
> **（2026-09-18 追加）同类"不得误用"的正数之二**：
> `node tools/harmony/verify-container-golden.mjs` 输出 **5/5 PASS**，
> 它验证的是 **JCS / AAD / GCM 这份移植规格本身**（主机侧独立实现复现黄金向量），
> **不是** ArkTS 运行时执行结果；`assembleHap` 的 `BUILD SUCCESSFUL` 同理，
> 且在 2026-09-18 之前对未被 import 的 `.ets` 而言甚至是空证据
> （hvigor 只编译可达模块）。
> 三者**都不写入本矩阵**，Harmony 列保持 **0 执行 / 91**。
>
> **（2026-09-18 追加）同类"不得误用"的正数之三**（本轮新增两道）：
>
> | 工具 | 输出 | 它证明了什么 | 它**没有**证明什么 |
> | --- | --- | --- | --- |
> | `check-compiled-reachability.mjs --build` | `PASS`（**18/18**） | 模块真的进了编译图、真在 `modules.abc` 里、注入类型错误**真的**会让构建失败 | 行为正确性。**零个** conformance 用例执行过 |
> | `check-argon2-native-build.mjs` | `PASS`（2 ABI） | 原生库真的被交叉编译、真的进了 HAP、`argon2_*` 真的没导出 | `deriveArgon2id` 会算出**正确**的派生密钥。链接属性 ≠ 行为属性 |
>
> 特别提示第 2 行：「信任边界成立」看起来很接近 `TESTED`，
> 但它验证的是**链接/打包属性**。一个把算法写错的实现同样可以做到
> 「不导出 `argon2_*`」—— 边界正确与结果正确是两件独立的事。
> **两者都不写入本矩阵。Harmony 列保持 0 执行 / 91。**
>
> **（2026-09-18 追加）同类"不得误用"的正数之四（Domain 11 组落地后）**：
> `HARMONY_DOMAIN = COMPILED` 意味着 11 组纯 ArkTS 真的被编译并打包
> （`modules.abc` 175,732 B，15 项语义自检标识符可从产物反查）。
> 这是**比"源文件存在"强得多**的证据 —— 但仍**不是** `TESTED`：
>
> | 已成立的 | 尚未成立的 |
> | --- | --- |
> | 模块可达、进 `modules.abc`、非 tree-shaken | **ArkTS 运行时**执行过 fixtures 并逐条比对 |
> | 自检函数在构建期可编译、逻辑被编译器接受 | 自检函数的**运行时返回值**真的都是 `ok` |
> | 主机镜像语义正确（关系 18/18、容器 5/5） | 那 18/18 与 5/5 是在 **Node** 上跑的，不在 Harmony 上 |
>
> 尤其注意第 2 行：`domainSelfCheckLine()` 里的 15 项检查**目前从未在设备上执行过**。
> 它们被编译进产物、可被反查字符串，但"编译通过"与"断言通过"是两件事。
> **Harmony 列保持 0 执行 / 91。**


---

## 1. 汇总

| 平台   | PASS | FAIL | NOT_IMPLEMENTED | NOT_RUN | 合计 |
| ------ | ---- | ---- | --------------- | ------- | ---- |
| Android | **91** | 0 | 0 | 0 | 91 |
| Harmony（设备执行面） | 0 | 0 | 0 | **91**（63 主机已通过 + 28 `BLOCKED_BY_RUNTIME`） | 91 |
| Harmony（**主机**执行面） | **63** | 0 | 0 | 28（`BLOCKED_BY_RUNTIME`） | 91 |
| iOS     | 0 | 0 | 0 | 91 | 91 |

> **两行 Harmony 的读法**：
> * **设备执行面**（`HARMONY_CONFORMANCE`）仍是 `NOT_RUN`，**pass = 0 / 91** ——
>   一次都没在设备上跑过。这是往矩阵里写"Harmony conformance"时唯一该引用的口径。
> * **主机执行面**（`HARMONY_CONFORMANCE_HOST`）**63/63 运行时无关用例已真跑通过**。
>   它执行的是**同一个 ArkTS runner**（非 Node 复刻），因此可以计入 conformance；
>   但它只覆盖 63/91，**不能**被当成 91/91，也**不**替代设备执行面。
> * 另有 gate `HARMONY_CONFORMANCE_RUNNER = PASS`（runner 已存在且被真的编译），
>   该 gate **不改变**本表任何计数。

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
|> **（2026-09-18 追加）同类"不得误用"的正数之五（Conformance Runner 落地后，本轮最重要）**：
>
> | 工具 | 输出 | 它证明了什么 | 它**没有**证明什么 |
> | --- | --- | --- | --- |
> | `check-compiled-reachability.mjs --build` | `PASS`（**22/22**） | 4 个 conformance 模块（`JsonText` / `HarnessFs` / `ConformanceRunner` / `ConformanceSelfCheck`）真的进了编译图与 `modules.abc`，且逐模块注入类型错误**真的**会让构建失败 | **零个** conformance 用例执行过 |
>
> 这道正数比前四道都更接近"能跑"，也正因此更需要警惕：
> `HARMONY_CONFORMANCE_RUNNER = PASS` 只说明 **runner 存在且被真的编译**。
> 它**不是** `HARMONY_CONFORMANCE = PASS`，Harmony 列仍保持 **0 执行 / 91**。
>
> 接线前后的证据对照（这条链条本身即是"真的被编译"的证明）：
>
> ```
> 未接线：BUILD SUCCESSFUL in 12 s 41 ms        ← 空证据（文件不在编译图里）
> 接线后：COMPILE RESULT:FAIL {ERROR:8 WARN:2}  ← 7 类真实 ArkTS 错误暴露
> 修复后：BUILD SUCCESSFUL in 15 s 350 ms       ← modules.abc 243,556 B / 23 modules / 0 orphans
> ```
>
> **不得**把这批编译错误"修掉"当作负面记录删除 —— 它恰好是
> §10「不得用 Node 镜像冒充 ArkTS」这条要求被**真的满足**的硬证据：
> Node 镜像不可能产生 ArkTS 编译错误。

> **（2026-09-18 追加）与"不得误用"相反的一条：**`HARMONY_CONFORMANCE_HOST`
> **可以**计入 conformance。这是本矩阵唯一一个"主机侧结果可以写进来"的例外，
> 且理由必须写清楚，否则它看起来和前面五条被拒的正数没有区别：
>
> | | 被执行的代码 | 数据 | 与"Node 镜像"的区别 |
> | --- | --- | --- | --- |
> | `check-relations-semantics.mjs` | **另写**的 Node 等价实现 | 同批 fixtures | ❌ 是镜像，**不得计入** |
> | **`HARMONY_CONFORMANCE_HOST`** | **ConformanceRunner 本身**（真 ArkTS 编译器产出） | 冻结 fixture 原文，逐字节内嵌 | ✅ 不是镜像，**可计入** |
>
> §10 禁止的是 "**Node mirror masquerading as ArkTS**" —— 即另写一份 Node 实现
> 去跑 fixtures。本门禁执行的是被测实现本人。
>
> **仍然成立的边界**：主机对 `@ohos.*` 只提供**不可调用的桩**
> （实测 `fs.readTextSync` → `is not callable`），
> 因此 28 个 @ohos 依赖用例在主机上仍记 `BLOCKED_BY_RUNTIME`；
> 主机执行面**不**覆盖它们，也**不**产出 91/91。
> 内嵌 fixture 由 `embed-fixtures.mjs --check` 作为**执行前的第一道**检查守漂移。

### 0.5 Harmony 用例的运行时相关性拆分（§11 要求）

§11 要求「**优先做运行时无关的 conformance**，crypto 相关的运行时用例可以记为
`BLOCKED_BY_RUNTIME`」。**实测**账目（已取代此前的推算值）：

| 分类 | 数量 | 说明 |
| --- | --- | --- |
| **已执行且通过** | **63** | 主机执行面真跑，actual 逐字节等于 expected |
| `BLOCKED_BY_RUNTIME` | **28** | parser 22 + depmap 3 + jcs 1 + backup 1 + `migration-db-v1-to-v3` 1 |
| `NOT_IMPLEMENTED` | **0** | — |
| **合计** | **91** | |

**汇总：已执行通过 63 / `BLOCKED_BY_RUNTIME` 28 / `NOT_IMPLEMENTED` 0 = 91。**

也就是说：**所有运行时无关用例都真的跑通并通过了**；
剩下的 28 个只受"没有设备运行时"这一个原因阻塞。

> **⚠ 更正记录（值得记住教训）**：本文档曾写「60 可执行 / 3 notImplemented」，
> 再之前写「87 notImplemented / 4 blocked」。两次都是**推算**：
> 第一次漏了 timeline 与 migration 的存在，第二次默认 state-machine 5 个用例都已实现
> （实际只有 2 个）。最终由自家测试 `accountingSplitMatchesSection11` 抓出 ——
> 它把三个数写成精确值，任何推算值一放进去即失败。
> **没有执行过的账目，就是没有被验证的账目。**


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
