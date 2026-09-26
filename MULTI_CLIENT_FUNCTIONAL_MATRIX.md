# MULTI_CLIENT_FUNCTIONAL_MATRIX.md

> 2026-09-26；机器可读主源：`runtime/RUNTIME_ACCEPTANCE_MATRIX.json`（73 特征 × 4 平台逐格状态）
> 本页为摘要 + 差异标注；逐格状态以 JSON 为准（单源，避免四份漂移表）。

## 1. 逐平台概要

| 平台    | 功能面                                                                                                                              | 本轮回合实现           | 结果                          |
| ------- | ----------------------------------------------------------------------------------------------------------------------------------- | ---------------------- | ----------------------------- |
| Desktop | --smoke 16/16（import→proposal→required→3 scenarios→candidate/drift→backup/restore→delete）                                         | PASS                   | FUNCTION 16/16                |
| Android | canonical 91/91（JVM conformance，与 Desktop 共享 core 语义）                                                                       | PASS                   | 91/91                         |
| Android | connected instrumented 61/61（lock/import/D16/backup/restore/persistence/keystore/screenshot-protection/perf/a11y/candidate/drift） | PASS                   | 61/61                         |
| Harmony | host 142/142（87 canonical host + 14 persistence + 17 repository + 12 import-pipeline + 8 core-journey）                            | PASS                   | 142/142                       |
| Harmony | 设备面（ArkData/HUKS/NAPI golden/UI）                                                                                               | BLOCKED（E-9 镜像）    | —                             |
| iOS     | macOS runner fresh build + canonical                                                                                                | runner run 36231032190 | 见 IOS_SIMULATOR_FINAL_REPORT |

## 2. 关键断言（每组平台均验证）

- Proposal-only 永不 must_change；Candidate 不直入 Impact；Drift 不自动改 Reality —— Android connected（CandidateDriftEvidenceTest）+ Desktop smoke + Harmony host canonical 同款断言全绿。
- done ≠ verified —— Android suite + Desktop smoke（completeAction 后 verification 保持非 VERIFIED 断言）+ 87 canonical state-machine。
- GraphRevision 只被 Confirmed Reality Mutation bump（同事务）—— 三端 canonical + host repository 测试。
- Backup/Restore 逐字节 roundtrip —— Android（PersistenceEvidence/DepmapRuntimeEvidence）+ Desktop smoke（backup→restore→reopen）+ canonical backup fixture（三端 expected 冻结）。

## 3. 逐格差异（相对 73 行 seed）

- Android：UI 行截图取回受限（A1）→ 功能行 PASS、screenshot_path 空（视觉部分另计）。
- Harmony：UI 22 行 NOT_IMPLEMENTED（产品未实现）；Device 能力行 BLOCKED（E-9）。
- iOS：UI 22 行 NOT_IMPLEMENTED（N4 gap）；库能力行以 runner canonical 结果为准。
- Desktop：Dark（E06）/a11y 全套（E05）NOT_IMPLEMENTED；store（E08）NOT_APPLICABLE。

> 完整 per-row status 见 `runtime/RUNTIME_ACCEPTANCE_MATRIX.json`（292 行，25 字段）。
