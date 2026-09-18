# MVP02_PRE_AUDIT.md — MVP02 启动前审计（PHASE 0）

> 采集：2026-09-12 · 分支 `feat/mvp02-global-source`（自 master `ac511f6` 切出）

## 现状

- branch / HEAD：`feat/mvp02-global-source` / `ac511f6`（MVP01 RC 收口末次提交）
- MVP01 RC verdict：`MVP01_DEV_CLOSEOUT = PASS`（MVP01_RC_AUDIT_REPORT.md，2026-09-12）
- 未跟踪文件：仅本轮新增 MVP02 控制文件（GOAL_MVP02_GLOBAL_SOURCE 等 7 个 + NEXT_BACKLOG.md）

## 技术现状

| 项                                | 当前值                                                                                                                             | 位置                                                                                       |
| --------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| application payload schemaVersion | **1**                                                                                                                              | core/src/schema/migrations.ts（SCHEMA_VERSION=1；graph-serialize payload schemaVersion=1） |
| DEPMAP container formatVersion    | **1**（本轮不得改变）                                                                                                              | core/src/crypto/depmap.ts                                                                  |
| WeChat import 结构                | `ImportFlow`（begin/resolveMerchant/finalize 三段式）+ `parseWechatBill` 直连；parserId=`wechat`，session sourceType=`wechat_bill` | core/src/services/import-pipeline.ts、core/src/parser/wechat/parser.ts                     |
| Fingerprint unique key            | `UNIQUE(source, fingerprint)`；HMAC(fpSecret, `source:sourceTxnId`)                                                                | core/src/schema/migrations.ts、core/src/fingerprint/fingerprint.ts                         |
| Proposal evidence 字段            | 单 `evidence_id` 列                                                                                                                | dependency_proposals 表                                                                    |
| Evidence provenance               | 仅 proposal_key + source_type + parser_id/version + session + first/last/count（**无 sourceInstanceId**）                          | evidence 表                                                                                |
| verificationBasis                 | 无（Dependency 存在即确认，隐式 user）                                                                                             | —                                                                                          |
| migrations                        | 仅 version 1                                                                                                                       | core/src/schema/migrations.ts                                                              |
| tests                             | 166/166 PASS（15 文件）                                                                                                            | docs/TEST_REPORT_RC.md                                                                     |
| quality gates                     | format/lint/typecheck/architecture/secret 全绿；clean install/clone PASS                                                           | MVP01_RC_AUDIT_REPORT.md                                                                   |
| 平台 blockers                     | B1（Android SDK/JDK17）/ B2（DevEco）/ B3（macOS）/ B10（HBuilderX）                                                               | BLOCKERS.md                                                                                |

## ChangePlan 说明

MVP01 pipeline 的“Action Checklist（原始操作最后）”即 ChangePlan 等价物（impact kernel checklist，
GOAL §15 引用的 ChangePlan ordering = checklist 确定性顺序 + target_operation 强制最后）。
本轮保持其语义与顺序不变。

## 本轮范围（对应 GOAL）

Schema v2（SourceInstance/Fingerprint scope/Evidence provenance/evidenceRefs/verificationBasis/
ImportSession v2）→ EvidenceSourceAdapter 契约 → WeChat Adapter 化 + ImportCoordinator →
GenericCsvAdapter → OfxQfxAdapter → multi-source E2E → MVP01 regression → quality gates → docs →
MVP02_FINAL_REPORT.md。DEPMAP_CONTAINER_V1 冻结。

## 风险与对策

1. SQLite 改 UNIQUE 约束需表重建（observation_fingerprints / dependency_proposals / evidence）→
   全部在新事务内 create-copy-drop-rename，迁移测试覆盖 v1→v2/重启/×50/注入失败回滚。
2. MVP01 测试 API 兼容：repository/coordinator API 增加 sourceInstanceId 参数 →
   MVP01 测试输入机械更新（语义断言不降级）。
3. ImportFlow 面子兼容：facade 自动使用 deterministic legacy wechat SourceInstance，
   保证 MVP01 dup-import/dedupe 语义不变。
