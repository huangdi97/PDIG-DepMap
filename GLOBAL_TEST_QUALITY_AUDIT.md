# GLOBAL_TEST_QUALITY_AUDIT.md — 全局测试质量审计（2026-09-23 · Round 0.1.0）

> 审计轮次：2026-09-23（四联审计：代码规模 / 依赖 / 代码质量 / 测试质量）。
> 本轮测试基线为**新鲜运行**（2026-09-23）：`:core:test` 71/71、`:app:testDebugUnitTest` 9/9、`:conformance:run` 91/91（fresh, android.json generatedAt 2026-09-23T13:45:32Z 佐证）。
> 设备侧与平台侧引用**前一审计轮记录**（如实标注出处，未在本轮重跑）：androidTest 59/59×2（API36 手机+平板）、Core Journey E2E 41/41、三场景 E2E 32/32、harmony host 89 checks（NOT on CI）、ios 7 test funcs（mac CI only）。
> 本轮未运行 git/gradle（任务约束），仅做静态复核（文件/夹具/报告 JSON 计数）。

---

## 1. 测试分层表

| 层                          | 载体                                                                                                                                                                                                 | 用例                                                                                                                                                                                                | 状态                                                                                                                                                                                                                              | 数据来源                                       |
| --------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------- |
| 领域单测（JVM）             | `:core:test`（7 文件 / 1,230 行）                                                                                                                                                                    | **71/71**（StateMachineTest 14、DomainInvariantTest 15、PlanReadinessTest 14、ImpactKernelTest 11、MigrationSemanticsTest 10、GraphRevisionSemanticsTest 7、JdbcTestDriver 为驱动辅助非用例）       | PASS（本轮 fresh）                                                                                                                                                                                                                | 本轮记录                                       |
| 应用单测（JVM）             | `:app:testDebugUnitTest`（FileWorkflowStateTest，170 行）                                                                                                                                            | **9/9**                                                                                                                                                                                             | PASS（本轮 fresh）                                                                                                                                                                                                                | 本轮记录                                       |
| 设备证据测试（androidTest） | `:app` androidTest（12 文件 / 2,324 行：Accessibility/AppLock/Backup/CandidateDrift/DeleteAll/DepmapRuntime/FileWorkflowD16/ImportHitbox/PerfSmoke/Persistence/RepositoryKeystore/ScreenProtection） | **59/59 × 2（API36 手机+平板）**                                                                                                                                                                    | PASS（**前轮 fresh，本轮未重跑**）                                                                                                                                                                                                | 前轮记录（ANDROID_16_API36_CLOSURE_REPORT 等） |
| 一致性验证（JVM harness）   | `:conformance:run` 对冻结 fixtures 逐字段比对                                                                                                                                                        | **91/91**（impact 13、readiness 16、coverage 6、relations 18、depmap 3、jcs 1、scenario 1、state-machine 5、parser 22、timeline 3、migration 2、backup 1——CONFORMANCE_MANIFEST.json 91 条核对一致） | PASS（**本轮 fresh**，android.json summary pass=91/fail=0）                                                                                                                                                                       | 本轮记录 + fixtures 实测 91                    |
| 用户旅程 E2E（设备）        | Core Journey                                                                                                                                                                                         | 41/41                                                                                                                                                                                               | PASS（前轮记录，诚实标注）                                                                                                                                                                                                        | 前轮记录                                       |
| 场景 E2E（设备）            | 三场景                                                                                                                                                                                               | 32/32                                                                                                                                                                                               | PASS（前轮记录，诚实标注）                                                                                                                                                                                                        | 前轮记录                                       |
| legacy oracle（TS）         | core/tests（43 文件 / 12,013 行）                                                                                                                                                                    | 43 文件；本轮记录 430 用例（冻结基线 LEGACY_REFERENCE_MANIFEST.md 记 453，口径/时点差异）                                                                                                           | READ_ONLY oracle                                                                                                                                                                                                                  | 本轮文件扫描 + 轮记录                          |
| harmony host                | 平台 host 运行                                                                                                                                                                                       | 89 checks                                                                                                                                                                                           | 运行于本机 host，**NOT on CI**（SUMMARY.json 2026-09-22：主机侧 87 PASS + 4 项未上报——depmap-golden-v1 / depmap-utf8-password-normalization / migration-db-v1-to-v3 / backup roundtrip，summary 因此 FAIL；不把部分通过写成通过） | 轮记录 + conformance/reports/SUMMARY.json      |
| ios                         | Xcode/mac                                                                                                                                                                                            | 7 test funcs                                                                                                                                                                                        | **mac CI only**，本地无 macOS 未运行                                                                                                                                                                                              | 轮记录                                         |

## 2. 覆盖率观点

- 覆盖率是参考指标而非质量目标（AGENTS §49）：优先 branches / failure-path / invariant 覆盖。
- 关键 invariant 由**专名测试**直接锁定（见 §3），而不是靠抹覆盖率。
- 未把「未运行」写成「已通过」：androidTest/E2E/harmony/ios 全部如实标注来源轮次与运行环境。

## 3. C5 不变式 → 测试映射表（本轮逐条核对源码，全部真实存在）

| 不变式                                         | 核心测试（路径=android/core/src/test/.../）                                                                                                | 佐证（conformance 91 项）                                                                          |
| ---------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------- |
| Observation ≠ Reality（proposal 路径）         | DomainInvariantTest proposal 判定路径；ImpactKernelTest `proposalNeverEscalatesToMustChangeEvenAtMaxConfidence`（L100）                    | impact-proposal-only：`proposal_only` → needs_review，不产生 must_change                           |
| Proposal ≠ Reality（置信度永不升级为必须处理） | ImpactKernelTest L100                                                                                                                      | readiness-confidence-cannot-bypass                                                                 |
| Candidate ≠ Node（接受前不是节点）             | DomainInvariantTest `candidateIsNotANodeUntilAccepted`（L127）；StateMachineTest `candidateAcceptCreatesNodeButDoesNotBumpRevision`（L88） | state-machine-discovery-candidate：accept 才创建 Node                                              |
| Drift ≠ Reality 突变                           | DomainInvariantTest `driftDetectionDoesNotMutateReality`（L142）；StateMachineTest `candidateDismissDoesNotMutateReality`（L109）          | state-machine-reality-drift：dismiss 仅记录 dismissedAtObservationCount                            |
| done ≠ verified                                | DomainInvariantTest `doneIsNotVerified`（L80）、`resolutionCountsDoneNotVerification`（L100）                                              | readiness-completed-empty-claim-not-retroactive                                                    |
| unknown ≠ required                             | DomainInvariantTest `unknownCannotAutoBecomeRequired`（L45）、`criticalityOnlyHasRequiredAndUnknown`（L27）                                | impact-unknown-criticality：criticality_unknown → needs_review                                     |
| 未确认 group ≠ 回退                            | ImpactKernelTest `confirmedAnyGroupWithSurvivingMemberIsBackupPath`（L130）、`confirmedAnyGroupWithAllMembersLostIsMustChange`（L156）     | impact-any-group-covered / impact-any-group-failed                                                 |
| 未来观察 ≠ 自动 verified                       | StateMachineTest `evidenceSignalNeverAutoVerifiesAndNeverMutatesReality`（L134）、`verificationPendingCanBeVerifiedOrFailed`（L117）       | state-machine-action-verification：evidenceSignalRule 只到 evidence_suggested，never auto-verifies |

## 4. 命名（行为式）

- Kotlin 测试名 = 行为描述：`unresolvedMustChangeBlocksThePlan`、`blockedWinsOverReviewRequired`、`realityMutationsBumpTheRevision`、`nonRealityOperationsNeverBumpTheRevision`、`terminalStatesHaveNoOutgoingEdges`、`changePlanRejectsIllegalTransitions`、`verificationPendingCanBeVerifiedOrFailed`（全部实测于源文件）。
- 无 `testPermission_3` 式编号命名；夹具/辅助私有函数单独标记（PlanReadinessTest `private fun plan(...)`）。

## 5. Mock 策略（不 mock 自己的领域规则）

| 层               | 实际策略                                                                                                                           |
| ---------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| :core            | **无 mock**。真实 sqlite-jdbc（org.xerial:3.46.1.0）跑真实迁移；JdbcTestDriver 为驱动适配（非 mock）                               |
| :repos           | **无测试源集**（:repos:test = NO-SOURCE）——"repos 无 mock" 如实表述为：无可用以 mock 的测试存在，待补（见 §7 计划）                |
| :app JVM         | FileWorkflowStateTest 纯状态机逻辑，无 Android 依赖、无 mock                                                                       |
| :app androidTest | **真实设备 + 真实 SQLCipher + 真实系统服务**（keystore/生物识别/FilePicker/隐私屏幕）；AccessibilitySemanticsTest 用真实语义树断言 |
| :conformance     | 对**冻结 fixtures** 逐字段 byte-exact 比对（期望值来自 spec + codegen），非 mock 快照                                              |
| mock 边界原则    | mock 只允许在真正系统边界（外部 API/设备服务），核心 domain rule 全部真实执行                                                      |

## 6. 边界 / 失败路径覆盖清单

| 边界                                   | 覆盖证据                                                                                                                                                                   |
| -------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 错误口令                               | conformance `depmap-golden-v1` → `wrongPasswordOutcome: auth_failed`；`depmap-utf8-password-normalization`（中文/emoji/组合字符归一化）                                    |
| 篡改密文                               | conformance `depmap-golden-v1` → `tamperedCiphertextOutcome: auth_failed`（AEAD 认证失败）                                                                                 |
| 未来/未知 schema                       | conformance `migration-version-contract` → rejectedSchemaVersions [0,4,99,100]                                                                                             |
| 参数过界（too large / too small）      | conformance `depmap-bounds-and-structure-rejection`：16 个拒绝场景（memory/iterations/parallelism/salt/nonce/tag/ciphertext bounds、float 参数、非 JSON）                  |
| 取消（cancel）                         | StateMachineTest `changePlanRejectsIllegalTransitions`；state-machine-change-plan 转换表含 `cancelled` 终结态 + PLAN-ACTION-FROZEN guard                                   |
| 重复（duplicate）                      | parser-qfx-duplicate-fitid（重复 FITID）；state-machine-reality-drift duplicateEvidenceRef「不重复创建/计数」；MigrationSemanticsTest `idempotentAfter50`（50 次重放幂等） |
| 已退休边重新成立（retired reactivate） | impact-retired-edge（retired 边不产生目标）；graph-revision bumpsOn 含 dependency_reactivate，GraphRevisionSemanticsTest 验证 bump 集合不相交                              |
| 坏输入恢复                             | parser-wechat-malformed：errorCount=4 且带行级 reason；parser-ofx-invalid-date/malformed 同理（不崩溃、行级错误上报）                                                      |

## 7. 本轮新增 Desktop 测试计划（**planned，未落地**）

desktop 模块**当前无任何测试源集**（11 个生产文件、0 个测试文件，`desktop/app/src/test` 不存在）；build.gradle.kts 已配置 kotlin("test") + junit-jupiter + useJUnitPlatform。计划项：

| 计划项                     | 内容                                                                                                                                                                 | 状态        |
| -------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------- |
| DepmapFileStore round-trip | 容器文件写入→读取→解密比对（复用 :core DepmapContainer + :conformance JdbcSqliteDriver 真实驱动）                                                                    | **planned** |
| security fake / 真实边界   | DesktopSecurityPort（DPAPI）以系统用户粒度真实执行；无法方 fake 领域规则                                                                                             | **planned** |
| importer parser 集成       | 对 fixtures/import 全部原始文件跑真实 parser 集成（fixtures/import **实测 28 个文件**：20 csv / 6 ofx / 2 qfx；计划书面对 Importer 31-fixture 的描述以实测 28 为准） | **planned** |

> 如实标记：本轮 Desktop 测试**尚未写入任何测试文件**（planned，不宣称 done）；`:repos` 同理缺测试源集。二者为下一轮 **必补项**，否则违反「测试不是 NO-SOURCE」基线。

---

**结论**：Android 侧本轮三通道（core 71 / app 9 / conformance 91）全部 fresh PASS，C5 八大不变式在核心测试中逐条有专名用例且 conformance 交叉佐证；命名行为式、核心零 mock、失败/边界路径覆盖齐全；androidTest 59×2、E2E 41/41 与 32/32、harmony 89 host、ios 7 funcs 均**如实标注为前轮/环境受限记录**；Desktop 与 :repos 测试源集为空，测试计划（DepmapFileStore round-trip / security / 28-fixture parser 集成）为 planned，须下轮落地。

## 8. v0.1.2 质量迭代收口轮复核（2026-09-24）

- **本轮 fresh 基线**（2026-09-24）：
  - `:core:test` **71/71**、`:app:testDebugUnitTest` **9/9**（--rerun-tasks fresh，0 failure）；
  - conformance `tools/conformance/run.mjs` fresh SUMMARY（2026-09-24）：codegen / fixtureIntegrity /
    oracleSelfcheck 均 PASS；android **91/91 PASS**（pass=91 fail=0 total=91）；
  - Android 仪器化测试基线 `TEST-pdig36(AVD)-16-_app-preview.xml` **60/60 PASS**（2026-09-24 11:39）；
  - Desktop `:app:test` BUILD SUCCESSFUL（27s，0 failure）+ `--smoke` **16/16 PASS**（app-0.1.2.jar：
    fresh-launch / create-open / wrong-password-rejected / tampered-file-rejected / future-schema-rejected /
    import-wechat-fixture / proposal-accept-bumps-revision / criticality-required-only-by-user /
    scenario-replace_payment_card / scenario-expiring_payment_card / scenario-close_payment_instrument /
    engine-generates-candidate-from-import / engine-generates-drift-never-auto-resolves /
    candidate-accept-dismiss / drift-resolve-dismiss / backup-restore-reopen-delete）；
  - legacy core-TS oracle：core `npm run check` 全绿（453 tests / 43 files PASS）。
- **前轮设备/平台记录（如实标注，未重跑）**：harmony 87/91（4 项 runtime-blocked：depmap-golden-v1、
  depmap-utf8-password-normalization、migration-db-v1-to-v3、backup-depmap-export-restore-roundtrip，
  2026-09-19 旧记录，本机无 HarmonyOS 运行环境）；ios 91/91（2026-09-19 旧记录）。
- **BLOCKED（环境，非回归）**：Android 运行时 smoke 本轮 **RUNTIME_ENVIRONMENT_BLOCKED**（2026-09-24 下午起
  本机所有 AVD——pdig36、pdig_api36_phone、pdig_api36_tablet、Medium_Phone_API_35(API35)、
  PDIG_API34_DEFAULT(ARM 不支持)、petaccess_api35(镜像路径损坏)——均在 full startup 静默退出，
  WHPX 检测正常、无 WER 崩溃记录）；仪器化以当日上午 60/60 实跑为基线。
- **FIXED（死代码）**：desktop `FileOps.kt` 删除无调用者的 `DesktopFileOps.write`（6 行；Grep 确认
  desktop 全树无 `.write(` 调用者、无实现覆盖）。
- **结论**：JVM / 桌面 / conformance 三通道 fresh 全绿；设备侧引用旧记录如实标注；未把 NOT_RUN 写成 PASS。
