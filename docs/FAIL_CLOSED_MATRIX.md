# FAIL_CLOSED_MATRIX.md — 失败封闭矩阵（Engineering Baseline V1）

> 每行 = 一条「失败路径 → 封闭行为 → 自动化证据」。任何新失败路径必须先加证据测试再合并。

| # | 失败场景 | 封闭行为 | 证据 |
|---|---|---|---|
| F-01 | Crypto 口令错误 | `DepmapError(auth_failed)`，无明文返回；与密文篡改不可区分（同为 auth_failed） | `crypto/depmap.test.ts`（wrong password / tag tamper）、`crypto/container-mutation.test.ts` F4 |
| F-02 | Crypto 容器被篡改（tag/ciphertext/salt/nonce 1-bit） | 拒绝或逐字节等价 no-op，绝不 partial plaintext | `crypto/container-mutation.test.ts` F1/F2、`unit/crypto-negative.test.ts` 30 轮 fuzz |
| F-03 | 恶意 KDF 参数（巨大/越界） | 在昂贵 Argon2 之前 `bounds` 拒绝（<5s/百次级） | `crypto/container-mutation.test.ts` F3、perf smoke「malicious KDF rejection」 |
| F-04 | 未来/未知 depmap 格式版本 | 结构/边界层拒绝（V1 冻结，不静默兼容） | `crypto/container-mutation.test.ts`（formatVersion 2/0）、graph-payload J3 段 |
| F-05 | 数据库 schema 版本高于支持 | `migrate()` 抛错，库保持原状 | `repository/migration.test.ts`（schema_version 99 rejected） |
| F-06 | 迁移中途失败 | 事务回滚，版本不前进，可重试迁移成功 | `repository/migration.test.ts` T8（poisoned SQL）+ mid-transaction rollback |
| F-07 | 导入 finalize 中途失败（指纹冲突注入） | 单事务回滚：无半写 fingerprints/proposals/evidence | `unit/db-integrity.test.ts`（finalize failure injection） |
| F-08 | Adapter parse 失败（坏行/坏日期/坏金额） | 坏行进 `lastParseErrors`，好行继续；不抛整次导入；缺失金额绝不伪造为 0 | `sources/generic-csv.test.ts`、`sources/ofx-qfx.test.ts`、`unit/negative.test.ts` |
| F-09 | 商户无法 resolution | 不生成 DependencyProposal（先 resolved 后 proposal） | `services/import-coordinator.ts`（unresolved→continue）+ pipeline 测试 |
| F-10 | Proposal 被拒绝 | 无 Reality 写入；重提需 ≥3 单流新观测 + ≥1 周期，否则 suppressed | `repository/proposal-lifecycle.test.ts`、repository-contract R3 |
| F-11 | 未知 criticality 边失效 | `needs_review`（criticality_unknown），绝不 must_change | `impact/kernel.test.ts` T8、INV12、property P4/P5 |
| F-12 | 仅 Proposal 存在（无确认） | 只产生 needs_review/proposal_only | `impact/kernel.test.ts` T4、INV11、multi-source K5 |
| F-13 | event_stream 后续流缺少商户 | 不 retire 任何已确认 Reality | `integration/coverage-semantics.test.ts` H1、INV10 |
| F-14 | 未确认 Group | 不产生备用假设（不降级 must_change） | `coverage-semantics.test.ts` H3/H4 |
| F-15 | retired Dependency | 不参与 Impact 传播；re-confirm 复活同 id | kernel T9/T10、INV9、property P3 |
| F-16 | biometric/凭据取消 | 不打开数据库（平台待真机验证） | 平台实现就绪；DEVICE_VERIFIED = NO（B1–B3） |
| F-17 | 孤儿数据（迁移/导入产物） | `checkGraphIntegrity` 五类孤儿检测为空 | `unit/db-integrity.test.ts`、INV13、graph-payload J1c |
| F-18 | 重复操作（同账单重导/重复确认/同逻辑边） | 幂等：无 duplicate edge/count/group/corruption | `unit/idempotency.test.ts`、K6 ×20、INV1/INV8 |
| F-19 | 非法日历日期（2/30 等） | 严格 parse 拒绝（分量往返校验），不入库 | `parser/wechat.test.ts`（日历校验用例）、contract C3 ISO 断言 |
| F-20 | 业务网络调用出现 | `check:network` exit 1（84 文件 0 原语） | `core/scripts/check-network.mjs` |
