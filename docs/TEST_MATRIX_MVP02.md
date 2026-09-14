# TEST_MATRIX_MVP02.md（MVP02 测试矩阵 → 实测映射）

> 状态：2026-09-13 ZCode 接力轮实跑。**273/273 PASS，0 skip（22 文件）**；
> quality gates 全绿（format / lint / typecheck / architecture / secret scan /
> clean install / clean clone）。Real Data 双 Gate = NOT_RUN。

## A Schema/Migration → tests/repository/migration.test.ts（17）

| 矩阵项                        | 用例                                            |
| ----------------------------- | ----------------------------------------------- |
| fresh v2                      | T1                                              |
| v1→v2                         | T2                                              |
| restart after migration       | T4b + restart 用例                              |
| migrate ×50 idempotent        | T3（空库）/ T4（有数据，零漂移）/ T4b（重启后） |
| injected failure rollback     | 回滚用例                                        |
| no orphan SourceInstance      | legacy 回填用例                                 |
| legacy WeChat dedupe retained | T5（作用域 + DDL 断言）                         |
| evidenceId→evidenceRefs       | T6                                              |

## B SourceInstance → tests/repository/source-instance-scope.test.ts

同 adapter 两实例 / 同 txn 跨实例不冲突 / retired 保留 provenance /
lastIngestedAt / 无 secret 字段 —— 全覆盖（12 用例）。

## C Fingerprint → source-instance-scope + fingerprint 单测

同实例重复拒绝 / 不同实例独立 / fingerprintVersion 隔离 / canonical fallback
确定性 / fpSecret 跨会话导出导入连续。

## D WeChat Regression → tests/parser/wechat.test.ts + pipeline.test.ts

MVP01 全部 parser（18 fixtures）/ fingerprint / pipeline 用例继续 PASS；
domain 无 `source==wechat` 业务分支。

## D+ RelationDefinitionRegistry → tests/domain/relation-registry.test.ts（14）

runtime 词表只有 funding_source/merchant_agreement（future 词表拦截）/
capability/fromKind/toKind 越界拒绝/Group 仅 funding_source+ANY（ALL 与
merchant_agreement Group 拒绝）/defaultCriticality=unknown + user_only/
ConfirmationService 写路径集成（非法 relation confirm 抛 registry rejected）。
registry 覆盖 100%。

## E Generic CSV → tests/sources/generic-csv.test.ts（20）

US signed / EU semicolon / debit-credit / BOM / quoted delimiter / CRLF /
CR-only / bad date / bad amount / missing mapping / 同 txn 不同 SourceInstance /
multi-currency / ×50 deterministic / 10k rows（perf smoke 64ms）。

## F OFX/QFX → tests/sources/ofx-qfx.test.ts（15）

basic / multiple / FITID fingerprint / missing FITID fallback / invalid date /
signed amounts / malformed / QFX / 同 FITID 不同 SourceInstance /
×50 deterministic / 10k（perf smoke 67ms）。

## G/H Coverage + Multi-source E2E

- coverage-semantics.test.ts（7）：absence 不否定 Reality 全矩阵
- multi-source-e2e.test.ts（9）：K1（2 实例→1 Proposal/2 refs）、K1b/K1c
  （流隔离、计数不相加）、K2（确认一次→1 Dependency，多源不自动
  required/backup）、K3（Group→Impact backup_path）、K5（零确认无
  must_change）、×20 确定性、单流重提阈值
- graph-payload-v2.test.ts（17）：v2 往返/幂等/原子失败、v1 in-memory migrate
  （J2=T10 路径）、不支持版本拒绝、口令/tamper 回归

## 性能 smoke → tests/perf/performance-smoke.test.ts（11）

MVP02 增量：10k CSV 64ms / 10k OFX 67ms / 3 SourceInstance 并发 2k×3 ≈4.6s
（含指纹命名空间隔离断言：每实例 2000 条、总计 6000）。MVP01 存量项全部保持。

## 明确 NOT_RUN

- Real Data Correctness Gate（需真实微信账单，B13）
- Real Data Value Gate（同上）
- Android/iOS/HarmonyOS 编译与真机（B1/B2/B3/B10 外部工具链）
