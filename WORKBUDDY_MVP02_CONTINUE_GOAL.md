# WORKBUDDY_MVP02_CONTINUE_GOAL.md

> Agent: WorkBuddy / CodeBuddy Agent
> Model: DeepSeek-V4-Flash
> Mode: Agent
> Task: 继续 ZCode 已经做了一部分的 MVP02，不从头重做。

## 0. 接力原则

这是 handoff / continuation task。
不要假设 ZCode 什么都没做，也不要假设它已经全做完。
必须先读取实际仓库状态、Git diff、测试和 WORK_STATUS，再从第一个未完成 Gate 继续。

禁止：

- 重新初始化项目
- 覆盖未提交 ZCode 修改
- 删除/skip 测试制造 PASS
- 降低 strict/lint 规则
- 进入 NEXT_BACKLOG
- synthetic 冒充 real data

## 1. 启动读取

完整读取：

1. AGENTS.md
2. CODEBUDDY.md
3. CANONICAL_DESIGN.md
4. GOAL_MVP02_GLOBAL_SOURCE.md
5. MVP02_ARCHITECTURE_FREEZE.md
6. SCHEMA_V2_MIGRATION_SPEC.md
7. SOURCE_ADAPTER_CONTRACT.md
8. MVP02_TEST_MATRIX.md
9. MVP02_ACCEPTANCE.md
10. WORK_STATUS.md
11. BLOCKERS.md
12. MVP01_RC_AUDIT_REPORT.md
13. MVP02_PRE_AUDIT.md（若存在）
14. MVP02_FINAL_REPORT.md（若存在）

随后检查：
git status
git diff
git diff --staged
git log --oneline -20
package.json / core/package.json
migrations / source adapters / tests

## 2. 先生成 Handoff Audit

生成 WORKBUDDY_HANDOFF_AUDIT.md，严格区分：

- 已完成且有证据
- 已实现但未验证
- 部分完成
- 未开始
- 当前 regression/failure
- 未提交修改

然后更新 WORK_STATUS.md。

## 3. 恢复顺序

从 MVP02_ACCEPTANCE.md 第一个真实未完成 Gate 开始。
优先级：

1. Schema v2 migration
2. SourceInstance
3. Fingerprint source scoping
4. multi-source Evidence
5. EvidenceSourceAdapter
6. WeChat Adapter 化
7. RelationDefinitionRegistry
8. Generic CSV
9. OFX/QFX
10. multi-source E2E
11. MVP01 regression
12. quality/security gates
13. docs/final report

## 4. 执行循环

inspect existing code
→ inspect tests
→ add/fix failing test
→ implement/fix
→ focused test
→ full regression
→ update WORK_STATUS
→ next

不要每个小步骤停下来问。

## 5. Schema v2

必须验证：

- schemaVersion=2
- v1→v2 migration
- rollback
- migration ×50 idempotent
- SourceInstance refs valid
- legacy WeChat fingerprint dedupe retained
- Proposal evidence migration
- no duplicate legacy SourceInstance
- no Dependency/Group identity loss

若已有 migration，先测试，不重写。

## 6. SourceInstance

正式实体，不只是 TypeScript type。
必须验证：

- same adapter multiple instances
- same txn id across different instances does not collide
- retired source keeps provenance
- lastIngestedAt
- no secrets/raw statement

## 7. Fingerprint

唯一性必须按：
sourceInstanceId + fingerprintVersion + fingerprint

HMAC scope:
adapterId + sourceInstanceId + sourceTxnId

必须验证：
same instance/same txn = duplicate
different instance/same txn != duplicate
legacy WeChat dedupe unchanged

## 8. Multi-source Evidence

Proposal 支持 evidenceRefs[]。
不同 SourceInstance 的 Evidence：

- provenance 分开
- count 分开
- time range 分开

严禁把多个来源计数简单相加后：

- 自动 required
- 自动 confirm
- 自动 backup
- 自动触发 re-proposal

re-proposal threshold 至少由某个单独 Evidence stream 自己满足。

## 9. EvidenceSourceAdapter

三个 Adapter 最终共用同一个 contract：

- wechat_statement
- generic_csv
- ofx_qfx

Adapter 不得：

- 写 Dependency/Group
- 设置 required
- 修改 Graph
- 保存 raw source
- 调网络
- 根据 absence retire Reality

MVP02 文件来源：
coverageMode=event_stream
authoritativeFor=[]

## 10. WeChat Regression

WeChat 变成普通 Adapter。
Domain 层不得新增 if source==wechat 业务分支。
全部 MVP01 WeChat tests 继续 PASS。

## 11. RelationDefinitionRegistry

runtime 本轮只支持：
funding_source
merchant_agreement

Registry 至少约束：
fromKinds / toKinds / capability / Group compatibility / default criticality / verification policy。

Proposal/Dependency writes 必须校验。

## 12. Generic CSV

若 ZCode 已写部分，继续完善。
至少 explicit mapping：
transactionId / datetime / signed amount or debit-credit / description / counterparty / currency / balance / transactionType

必须测试：
US CSV、EU semicolon、debit-credit、BOM、quoted comma、CRLF、CR-only、bad date/amount、missing mapping、same txn across instances、multi-currency、10k rows、deterministic×50。

禁止 LLM 自动映射。

## 13. OFX/QFX

至少支持：
FITID / DTPOSTED / TRNAMT / TRNTYPE / NAME / MEMO

必须：
FITID fingerprint
missing FITID fallback
malformed fail safely
SourceInstance isolation
deterministic
QFX same family

不得自动创建 Reality。

## 14. Source-neutral ImportCoordinator

最终只能是一条业务 pipeline：
SourceInstance → adapter → parse → normalize → fingerprint → resolver → evidence → proposal

Source-specific parsing 可不同，Domain semantics 只能一套。

## 15. Coverage correctness

event_stream absence 不能：

- retire Dependency
- reject Proposal
- fail Group
- produce must_change

必须有 regression test。

## 16. Multi-source E2E

至少：
Source A=Generic CSV
Source B=OFX/QFX
same logical relationship
→ one Proposal
→ two Evidence refs
→ user accepts once
→ one Dependency
→ Impact works
→ ChangePlan works

两个来源都看到也不能自动 required。

## 17. .depmap

DEPMAP_CONTAINER_V1 不变。
允许 payload schemaVersion=2。

必须：
old Golden Vector unchanged
v2 export/import
v1 payload migrate in memory
unsupported schema fail
wrong password/tamper regressions green

## 18. MVP01 Regression

全部重跑：
Schema/Core
Impact
Parser
Fingerprint
Proposal/Group lifecycle
Synthetic E2E
Crypto
RC quality gates

Regression 先修再继续。

## 19. Quality

复用现有命令：
npm run format:check
npm run lint
npm run typecheck
npm test
npm run check
npm run check:full

不要重复造同功能 script。

## 20. 外部 Blocker

Android/Harmony/iOS SDK、设备、签名、真实账单可保持 blocker。
不能阻塞 Core MVP02。
Real Data 继续 NOT_RUN。

## 21. Git

保护 ZCode 未提交工作。
禁止：
git reset --hard
git clean -fd
强制覆盖/重置

可以小步 commit，但不要自动 push。

## 22. 每轮结束

如果一次 Agent run 没跑完：
先更新 WORK_STATUS.md：
Current / Completed / Tests / Failures / Blockers / Next
然后结束。
下一任务使用 WORKBUDDY_CONTINUE_PROMPT.txt。

## 23. Final Report

最终生成 MVP02_FINAL_REPORT.md，包含：
MVP02_GLOBAL_SOURCE_ABSTRACTION
SCHEMA_V2
V1_TO_V2_MIGRATION
SOURCEINSTANCE
FINGERPRINT_SCOPE
MULTISOURCE_EVIDENCE
SOURCE_ADAPTER_CONTRACT
WECHAT_REGRESSION
GENERIC_CSV
OFX_QFX
RELATION_REGISTRY
MULTISOURCE_E2E
DEPMAP_CONTAINER_V1_COMPAT
MVP01_REGRESSION
QUALITY_GATES
REAL_DATA=NOT_RUN

只能 PASS/FAIL/BLOCKED/NOT_RUN。

## 24. 成功条件

只有 Schema v2、migration、SourceInstance、fingerprint isolation、multi-source evidence、3 adapters、relation registry、multi-source E2E、container compatibility、MVP01 regression、quality gates 全 PASS，才能：
MVP02_GLOBAL_SOURCE_ABSTRACTION=PASS

## 25. 现在开始

先读 repo 和控制文件，生成 WORKBUDDY_HANDOFF_AUDIT.md，然后从第一个未完成 Gate 接着做。
不要重新规划整个项目，实际执行。
