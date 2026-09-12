# ZCODE_MVP02_REHANDOFF_GOAL.md

> Agent: ZCode
> Model: GLM-5.3-Flash（或当前可用 coding 模型）
> 前一执行者: WorkBuddy + DeepSeek-V4-Flash
> 任务: 从 WorkBuddy 已完成的真实仓库状态继续 MVP02，不从头重做。
> Real Data: NOT_RUN

## 1. 接力原则
这是 handoff / continuation task。先恢复现场，再继续开发。

禁止：
- 重新初始化项目
- 从头重做 MVP02
- 覆盖 WorkBuddy 未提交修改
- git reset --hard
- git clean -fd
- checkout/restore 覆盖工作区
- 删除或 skip 测试制造 PASS
- 降低 strict/lint 规则
- synthetic 冒充 real data
- 进入 NEXT_BACKLOG / MVP03 / PDIG v1.1

## 2. 启动必读
按顺序读取：
1. AGENTS.md
2. CANONICAL_DESIGN.md
3. GOAL_MVP02_GLOBAL_SOURCE.md
4. MVP02_ARCHITECTURE_FREEZE.md
5. SCHEMA_V2_MIGRATION_SPEC.md
6. SOURCE_ADAPTER_CONTRACT.md
7. MVP02_TEST_MATRIX.md
8. MVP02_ACCEPTANCE.md
9. WORK_STATUS.md
10. BLOCKERS.md
11. MVP01_RC_AUDIT_REPORT.md
12. CODEBUDDY.md（若存在）
13. WORKBUDDY_MVP02_CONTINUE_GOAL.md（若存在）
14. WORKBUDDY_HANDOFF_AUDIT.md（若存在）
15. MVP02_PRE_AUDIT.md（若存在）
16. MVP02_FINAL_REPORT.md（若存在）
17. 本文件

## 3. 第一件事：恢复 WorkBuddy 现场
先执行并记录：
- git status
- git diff
- git diff --staged
- git log --oneline -30

然后检查：
package.json / core/package.json / migrations / src / core / tests / fixtures / docs

重点搜索：
SourceInstance
EvidenceSourceAdapter
GenericCsvAdapter
OfxQfxAdapter
RelationDefinitionRegistry
schemaVersion
evidenceRefs
sourceInstanceId
fingerprintVersion

如果有未提交修改，必须保护，不清理、不覆盖。

## 4. 生成 ZCODE_REHANDOFF_AUDIT.md
严格分为：
A. WorkBuddy 已完成且已验证
B. 已实现但未验证
C. 部分完成
D. 未开始
E. 当前失败/Regression
F. 未提交工作
G. NEXT_GATE

只有“代码存在 + 测试/命令证据存在”的项目才能写入 A。

随后更新 WORK_STATUS.md，至少包含：
Current phase / Current gate / Completed / Verified / Implemented but unverified / Failures / External blockers / Next

## 5. 恢复顺序
从 MVP02_ACCEPTANCE.md 第一个真实未完成 Gate 开始，顺序：
1. Schema v2 migration
2. SourceInstance
3. Fingerprint source scoping
4. multi-source Evidence
5. EvidenceSourceAdapter
6. WeChat Adapter normalization
7. RelationDefinitionRegistry
8. Generic CSV
9. OFX/QFX
10. multi-source E2E
11. MVP01 regression
12. quality/security gates
13. docs/final report

已通过的不要重做。

## 6. 每个 Gate 的执行循环
inspect existing implementation
→ inspect existing tests
→ run focused tests
→ identify exact gap
→ add/fix failing tests
→ implement minimal fix
→ rerun focused tests
→ run related regression
→ update WORK_STATUS
→ next gate

## 7. Schema v2
如未完全 PASS，验证：
schemaVersion=2
v1→v2 migration
rollback
migration ×50 idempotent
legacy WeChat dedupe
Proposal evidence migration
no orphan SourceInstance
no Dependency/Group ID loss
restart after migration

若已有 migration，先测后修，不重写。

## 8. SourceInstance
必须是真实持久化实体，验证：
same adapter multiple instances
same txn id across instances != duplicate
retired source keeps provenance
lastIngestedAt
provider/account metadata
no secret/raw statement fields

## 9. Fingerprint
唯一 scope：
sourceInstanceId + fingerprintVersion + fingerprint

稳定 ID HMAC scope：
adapterId + sourceInstanceId + sourceTxnId

必须保持：
same instance/same txn = duplicate
different instance/same txn != duplicate
legacy WeChat dedupe unchanged

## 10. Multi-source Evidence
Proposal 使用 evidenceRefs[]。
不同 SourceInstance 的 evidence provenance/count/time-range 分开。

禁止把多个 source count 相加后自动：
required / confirm / backup / re-proposal。

Reproposal threshold 由至少一个独立 evidence stream 自己满足。

## 11. EvidenceSourceAdapter
三个实现：
wechat_statement
generic_csv
ofx_qfx

Adapter 不得：
写 Dependency/Group
设置 required
直接修改 Graph
保存 raw statement
调网络
根据 absence retire Reality

文件来源统一：
coverageMode=event_stream
authoritativeFor=[]

## 12. WeChat Regression
WeChat 必须是普通 Adapter。
Domain 不得新增 if source==wechat 业务分支。
全部 MVP01 WeChat tests 继续 PASS。

## 13. RelationDefinitionRegistry
runtime 本轮只支持：
funding_source
merchant_agreement

验证 fromKinds/toKinds/capability/group compatibility/default criticality/verification policy。
所有 Proposal/Dependency writes 必须校验。

## 14. Generic CSV
继续 WorkBuddy 已有实现，不另写第二套。
至少支持：
transactionId
datetime
signed amount 或 debit/credit
description
counterparty
currency
balance
transactionType

必须覆盖：
US CSV
EU semicolon
debit-credit
BOM
quoted delimiter
CRLF
CR-only
bad date
bad amount
missing mapping
same txn across SourceInstances
multi-currency
10k rows
deterministic ×50

禁止 LLM 自动 mapping。

## 15. OFX/QFX
至少支持：
FITID / DTPOSTED / TRNAMT / TRNTYPE / NAME / MEMO

测试：
FITID fingerprint
missing FITID fallback
invalid date
malformed
negative/positive
multiple transactions
QFX
same FITID different SourceInstance
deterministic ×50

不得自动创建 Reality。

## 16. Source-neutral ImportCoordinator
最终仅一条业务 pipeline：
SourceInstance → Adapter → parse → normalize → fingerprint → resolver → evidence → proposal

允许 adapter 内 source-specific parsing；Domain semantics 只能一套。

## 17. Coverage correctness
三个 Adapter 都是 event_stream。

必须验证：
old confirmed Dependency exists
+ new import does not mention it
→ Dependency remains active

Absence 不得 retire Dependency / reject Proposal / fail Group / produce must_change。

## 18. Multi-source E2E
至少：
Source A=Generic CSV
Source B=OFX/QFX
same logical relationship
→ one Proposal
→ two Evidence refs
→ user confirms once
→ one Dependency
→ Impact works
→ ChangePlan works

两个来源都看到也不能自动 required/group/backup。

## 19. DEPMAP compatibility
DEPMAP_CONTAINER_V1 不变。
允许 payload schemaVersion=2。

必须测试：
old Golden Vector unchanged
v2 payload export/import
v1 payload→in-memory migrate→v2 import
unsupported payload schema fail
wrong password/tamper regressions

## 20. MVP01 regression
重跑：
Schema/Core
Impact
WeChat Parser
Fingerprint
Proposal lifecycle
Group lifecycle
Synthetic E2E
Crypto
RC quality gates

任何 regression 先修再继续。

## 21. Quality
复用已有脚本：
npm run format:check
npm run lint
npm run typecheck
npm test
npm run check
npm run check:full

WorkBuddy 已创建同类 script 则复用，不重复造。

## 22. Security / Privacy
新增 Source 后重新检查：
raw CSV/OFX not persisted
sourceTxnId not persisted plaintext
logs no raw rows
Evidence remains aggregate
SourceInstance has no secrets
.depmap remains encrypted
business network calls=0

## 23. 外部 Blockers
Android/JDK/SDK、HBuilderX、DevEco、macOS/Xcode、设备、签名、开发者账号、真实账单允许继续 BLOCKED。
不得阻塞 Core MVP02。
Real Data Correctness/Value 均保持 NOT_RUN。

## 24. Git 安全
禁止：
git reset --hard
git clean -fd
git checkout .
git restore .

除非用户明确要求。

完成稳定 Gate 后可小步 commit，但不要自动 push。

## 25. 如果一次没有跑完
结束前更新 WORK_STATUS.md：
Current gate / Completed / Verified / Failures / Next

下一次用 ZCODE_MVP02_REHANDOFF_CONTINUE.txt。

## 26. Final Report
最终生成/完成 MVP02_FINAL_REPORT.md，包括：
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
SECURITY_PRIVACY
REAL_DATA=NOT_RUN

只能 PASS/FAIL/BLOCKED/NOT_RUN。

## 27. 成功条件
只有 Schema v2、migration、SourceInstance、fingerprint isolation、multi-source Evidence、3 adapters、relation registry、multi-source E2E、DEPMAP V1 compatibility、MVP01 regression、quality/security gates 全 PASS，才允许：
MVP02_GLOBAL_SOURCE_ABSTRACTION=PASS

## 28. 现在开始
立即：
1. 读取接力文件
2. 检查 Git 现场
3. 生成 ZCODE_REHANDOFF_AUDIT.md
4. 更新 WORK_STATUS.md
5. 找第一个真实未完成 Gate
6. 从那里继续代码和测试
7. 持续执行直到 MVP02_FINAL_REPORT.md

不要只给计划，实际执行。
