# MVP02_ACCEPTANCE.md

> `[x]` 只能在有测试/证据时勾选。Real Data 本轮允许保持 NOT_RUN。
> 勾选基线：ZCode 接力轮 2026-09-13 实跑（commit `7a68887` 后，259/259 PASS；
> 同日续轮补 relation-registry 直接测试 14 例后 **273/273 PASS**）。

## A Architecture
- [x] EvidenceSourceAdapter 正式化 — `core/src/sources/types.ts` 契约 + H0/H0b 契约测试；三实现 wechat_statement / generic_csv / ofx_qfx
- [x] WeChat 成为普通 Adapter，不是 Domain 特例 — `src/sources/wechat/adapter.ts`；domain/impact/schema 无 `source==wechat` 业务分支（仅 legacy 迁移常量 `LEGACY_WECHAT_SOURCE_INSTANCE_ID`）；MVP01 WeChat 回归全 PASS
- [x] SourceInstance implemented — `src/repositories/source-instance-repository.ts`（create/retire/touchIngested）+ `tests/repository/source-instance-scope.test.ts`
- [x] coverageMode implemented — `types.ts:66`；文件 Adapter 强制 `event_stream`（`types.ts:92` 抛错）
- [x] authoritativeFor implemented — `types.ts:67`；三个文件 Adapter 均为 `[]`
- [x] RelationDefinitionRegistry implemented — `src/domain/relation-registry.ts`；confirmation-service 写前 `validateRelationUse` 校验；**直接测试 14 例**（tests/domain/relation-registry.test.ts：正/负路径、Group 模式、future 词表拦截、写路径集成，registry 覆盖 100%）

## B Schema v2
- [x] schemaVersion = 2 — migration.test.ts
- [x] v1→v2 migration PASS — T1/T2
- [x] repeated migration PASS — T3（空库 ×50）+ T4（有数据库 ×50 零漂移）+ T4b（重启后 ×50）
- [x] migration rollback PASS — 注入失败回滚用例
- [x] legacy WeChat dedupe retained — T5（同实例同版本拒绝 / 不同版本、不同实例放行 + DDL UNIQUE 断言）
- [x] evidenceRefs migrated — T6（evidenceId → proposal_evidence_refs）
- [x] no orphan SourceInstance refs — migration.test.ts（fingerprints/evidence/sessions 回填 legacy 实例）

## C Fingerprint
- [x] source-scoped unique key — UNIQUE(source_instance_id, fingerprint_version, fingerprint)（T5 DDL 断言）
- [x] same txn id across sources safe — source-instance-scope.test.ts
- [x] same source duplicate safe — T2/T5 + perf smoke 命名空间断言
- [x] fpSecret continuity PASS — fingerprint HMAC 跨会话/导出导入用例

## D Evidence
- [x] per SourceInstance/proposal stream — K1b/K1c
- [x] multi-source evidenceRefs — K1（CSV+OFX → 一个 Proposal 两条 ref）
- [x] counts not blindly summed — K1c（count 不跨流相加）
- [x] conservative re-proposal rule — 单流自足阈值用例（多源不自动 required/confirm/backup）

## E Sources
### WeChat
- [x] all regression tests PASS — parser 18 fixtures 用例 + pipeline + fingerprint 全 PASS
### Generic CSV
- [x] explicit mapping — 显式 MappingProfile，无 LLM 自动 mapping
- [x] fixtures PASS — 20 用例 / 10 fixtures（US/EU/debit-credit/BOM/quoted delimiter/CRLF/CR-only/bad date/bad amount/missing mapping/multi-currency）
- [x] deterministic — ×50 确定性用例
- [x] 10k smoke — performance-smoke.test.ts（10k rows parse 64ms）
### OFX/QFX
- [x] fixtures PASS — 15 用例 / 8 fixtures
- [x] FITID + fallback fingerprint — FITID 主指纹 + 缺失 fallback 确定性用例
- [x] deterministic — ×50 确定性用例（+10k OFX parse 67ms）

## F Reality Safety
- [x] file adapters = event_stream — types.ts:92 强制校验 + 三 Adapter 声明
- [x] absence never retires Reality — coverage-semantics.test.ts（7 用例：旧确认 Dependency 在新导入不提及 → 仍 active；absence 不 reject Proposal / 不 fail Group / 不产生 must_change）
- [x] no adapter auto-confirms Dependency — Adapter 只产 Observation/Evidence；确认仅经 confirmation-service `userConfirmedBasis`
- [x] no adapter creates Group — Group 仅用户确认（K3）
- [x] no machine-generated required — K2/K5：多源 pending 零确认 → 无 must_change / 无 required

## G `.depmap`
- [x] DEPMAP_CONTAINER_V1 unchanged — 容器层 golden/负向测试未变，全 PASS
- [x] Golden Vector unchanged — crypto golden vector 用例 PASS
- [x] payload schema v2 PASS — J 段（v2 往返/幂等/原子失败/不支持的 payload 版本拒绝）
- [x] payload v1 migration PASS — J2（v1 payload in-memory migrate → v2 import，不触碰 DB）

## H E2E
- [x] WeChat E2E — pipeline.test.ts（10 用例）
- [x] CSV E2E — K 段（Generic CSV SourceInstance 全链路）
- [x] OFX E2E — K 段（OFX/QFX SourceInstance 全链路）
- [x] two SourceInstances → one logical Proposal — K1（same logical relationship → 1 Proposal / 2 evidence refs）
- [x] confirmation → one Dependency — K2/K3（确认一次 → 1 Dependency，verificationBasis=user_confirmed 回读断言）
- [x] Impact regression PASS — impact kernel 17 用例
- [x] ChangePlan regression PASS — K3（确认→Group→Impact→清单 backup_path 而非 must_change）+ K5

## I Quality
- [x] format PASS — prettier 3.9.6 全绿
- [x] lint PASS — eslint 10 typed 0 errors/0 warnings
- [x] typecheck PASS — tsc strict 0 errors
- [x] all tests PASS — 273/273（22 文件，0 skip）
- [x] architecture PASS — check:architecture（35 files）
- [x] secret scan PASS — check:secrets（249 files，0 production secrets）
- [x] privacy audit PASS — RC 轮 LOGGING_AUDIT/PRIVACY_DATAFLOW_AUDIT 基础 + **PHASE 22 MVP02 重审 grep 实证**（docs/PRIVACY_DATAFLOW_AUDIT.md「MVP02 Re-audit」节）：src 零网络调用、零 console 输出、无 observations 表、指纹表无 sourceTxnId 明文列、SourceInstance/ImportSession 无 secret 与 raw 列、`.depmap` 加密不变
- [x] clean install PASS — 本轮 `npm ci` + `npm run check` 全绿复验（MVP02 增量后）
- [x] clean clone PASS — 本轮 temp clone 模拟：clone → npm ci → npm run check 全绿

## J Real Data
- Correctness Gate: NOT_RUN
- Value Gate: NOT_RUN

## K Final
- [x] WORK_STATUS updated（2026-09-13 ZCode 接力轮）
- [x] BLOCKERS updated（无新增；B1–B3/B10 编译、B4–B9/B11–B13 发布材料保持）
- [x] docs updated — docs/ 八份 MVP02 文档（SOURCE_ARCHITECTURE / SOURCE_INSTANCE / GENERIC_CSV_ADAPTER / OFX_QFX_ADAPTER / SCHEMA_V2 / MIGRATION_V1_V2 / MULTISOURCE_EVIDENCE / TEST_MATRIX_MVP02）
- [x] MVP02_FINAL_REPORT generated
- [x] verdict written — MVP02_GLOBAL_SOURCE_ABSTRACTION = PASS（Real Data NOT_RUN 除外）
