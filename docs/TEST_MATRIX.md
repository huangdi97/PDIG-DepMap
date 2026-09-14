# TEST_MATRIX.md — 测试矩阵

> 运行：`cd core && npm test`（vitest）+ `npm run typecheck`
> 当前：**273 tests / 273 PASS**，typecheck 0 error（2026-09-13 MVP02 收口轮更新；
> 历史 RC 轮为 166，MVP02 增量见 `docs/TEST_MATRIX_MVP02.md`）

## Core（全部 PASS）

| 模块              | 文件                                        | 数量 | 覆盖                                                                                            |
| ----------------- | ------------------------------------------- | ---- | ----------------------------------------------------------------------------------------------- |
| Migration         | tests/repository/migration.test.ts          | 8    | 幂等/重启安全/回滚/版本拒绝/UNIQUE/CHECK                                                        |
| Repositories      | tests/repository/repositories.test.ts       | 7    | 节点 CRUD、Dependency UPSERT/复活、criticality、group canonical/复活                            |
| Proposal 生命周期 | tests/repository/proposal-lifecycle.test.ts | 7    | UPSERT、accepted 不重问、rejected 重提门槛、evidence min/max、GroupProposal                     |
| Impact Kernel     | tests/impact/kernel.test.ts                 | 17   | T1–T12 + 确定性顺序 + checklist 末位 + canonical fixture A/B/B2                                 |
| Crypto            | tests/crypto/depmap.test.ts                 | 28   | JCS、bounds 先于 KDF、roundtrip、错误口令、tag/密文/header 篡改、AAD、Unicode 口令、Golden 复现 |
| Parser+指纹+周期  | tests/parser/wechat.test.ts                 | 18   | 10 fixtures + 指纹跨会话去重（1–6/1–8 月）+ 周期识别边界                                        |
| Resolver          | tests/unit/resolver.test.ts                 | 11   | alias/normalized/fuzzy/ambiguous/unresolved、支付方式解析、卡匹配                               |
| Integration       | tests/integration/pipeline.test.ts          | 9    | 三段式导入全流程、重复导入、evidence 累计、Group→Impact、复活、rejected 门槛                    |

## 对应 MVP_ACCEPTANCE

- A Core Correctness：全部覆盖（migration 重复执行、UNIQUE、复活、groupKey 去重、UPSERT、重提门槛、Observation 不持久化〔由架构+测试保证〕、指纹重复导入、evidence 累计）
- B Impact：12 项全 PASS + 附加
- C Crypto：wrong password / tag tamper / header tamper / malicious bounds / Golden 三值 —— PASS；
  「reference↔Android/iOS/Harmony 互操作」= 向量与测试代码就绪，**未运行**（外部工具链）
- D Parser：10 项全 PASS
- E Platform：Android/HarmonyOS/iOS 测试代码就绪，COMPILED/TESTED/DEVICE_VERIFIED = NO（BLOCKERS）
- F Product Flow：synthetic 端到端（integration）PASS；UI 为源码交付，未编译
- G Real Data：**NOT_RUN**（无真实账单；local_private 仅 README）

## 平台侧（未运行，需环境）

| 套件                          | 位置                                                      | 前置                         |
| ----------------------------- | --------------------------------------------------------- | ---------------------------- |
| Kotlin golden vector + bounds | platforms/android/test/.../DepmapContainerV1GoldenTest.kt | JDK17 + Android SDK + Gradle |
| Swift golden（skip 占位）     | platforms/ios/Tests/.../DepmapContainerV1Tests.swift      | macOS + Xcode                |
