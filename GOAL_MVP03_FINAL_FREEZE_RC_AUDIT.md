# GOAL_MVP03_FINAL_FREEZE_RC_AUDIT.md

> 任务性质：MVP03 最终冻结 / RC-AUDIT
> 当前前提：MVP03 九模块已实现；Engineering Baseline v1 已 PASS
> 目标：不新增功能，只验证 correctness、state-machine、migration、security、regression，并冻结 MVP03
> Real Data：NOT_RUN
> 平台真编译/真机：若外部工具链缺失可 BLOCKED，不阻塞 Core Freeze

## 0. 当前已知实现

MVP03 当前已落地：

1. graphRevision
2. ChangePlan + Rebase / needs_revalidation
3. PlanReadiness
4. ScenarioCoverage
5. RealityDrift
6. DiscoveryCandidate
7. ScenarioTemplate
8. Timeline / Upcoming
9. Verification

同时已有：
- Schema v3
- v2→v3 migration
- payload v3
- v1/v2 payload migration
- DEPMAP_CONTAINER_V1 不变
- fast-check / invariant tests
- UI 页面
- docs / README / CANONICAL 同步

本轮只做 Freeze，不进入 MVP04。

## 1. 启动必读

完整读取：
AGENTS.md
CANONICAL_DESIGN.md
WORK_STATUS.md
BLOCKERS.md
ENGINEERING_BASELINE_V1_REPORT.md
QUALITY_GATES_V1.md
MVP03_ACCEPTANCE.md
MVP03_FINAL_REPORT.md
GOAL_MVP03_LIVING_GRAPH_CHANGE_SAFETY.md
GOAL_MVP03_FINAL_FREEZE_RC_AUDIT.md
MVP03_FREEZE_ACCEPTANCE.md
MVP03_FREEZE_TEST_MATRIX.md

然后执行：
git status
git diff
git diff --staged
git log --oneline -30

保护全部未提交工作。

禁止：
git reset --hard
git clean -fd
git checkout .
git restore .

## 2. 本轮明确禁止

不要进入 MVP04。
不要新增：
PayPal / Plaid / Open Banking
card_on_file / direct_debit_mandate / payout_destination
手机号/邮箱 capability
Browser/OAuth Discovery
IncidentPlan / Recovery Drill
AI/LLM
后端/云同步
新提醒模板
新生活场景

只允许：
correctness fix
state-machine fix
migration fix
security/privacy fix
test gap fix
docs mismatch fix
performance regression fix
UI 语义错误修复

## 3. Freeze Pre-Audit

生成：
MVP03_FREEZE_PRE_AUDIT.md

记录：
current HEAD
branch
dirty files
schemaVersion
depmap formatVersion
tests total
coverage
graphRevision implementation
ChangePlan implementation
PlanReadiness implementation
ScenarioCoverage implementation
RealityDrift implementation
DiscoveryCandidate implementation
ScenarioTemplate implementation
Timeline implementation
Verification implementation
migration v2→v3
payload migration
mutation baseline
known blockers

## 4. P0：PlanReadiness correctness

这是本轮最高优先级。

检查代码是否存在错误简化：

remainingMustChange =
mustChangeTargetCount
-
completedChangeActionCount

如果存在，必须修。

原因：
1 个 Impact target 可能需要多个 Action；
1 个 Action 也可能解决多个 Impact target。

因此 Readiness 必须基于显式 requirement / impact resolution，而不是数量相减。

推荐：
ActionItem.resolvesImpactKeys[]
或
ImpactRequirement.requiredActionIds[]

然后：
must_change resolved =
该 requirement 的真实 required conditions 全部满足。

如果现有实现已经不是数量相减：
不要重写，只补测试和报告证明。

## 5. PlanReadiness 冻结规则

最终只能：
blocked
review_required
ready_with_known_scope

禁止：
safe
fully_safe
all_clear
100_percent_safe

blocked：
存在至少一个 unresolved must_change requirement。

review_required：
没有 unresolved must_change，
但存在：
needs_review
unknown criticality
unresolved candidate
relevant pending Proposal
stale relevant dependency
needs_revalidation
verification pending on required change

ready_with_known_scope：
必须同时满足：
revision 与 current graph 对齐
所有 must_change requirement 已解决
所有 required review 已处理
不存在已知 blocker

文案只能：
“基于当前已知并确认的信息，可以继续。”

## 6. Readiness Tests

至少：
FR-READ-001 one target / multiple required actions
FR-READ-002 multiple targets / one shared action
FR-READ-003 completed unrelated action does not resolve requirement
FR-READ-004 unresolved must_change = blocked
FR-READ-005 unknown criticality = review_required
FR-READ-006 pending Proposal = review_required
FR-READ-007 unresolved Candidate = review_required
FR-READ-008 revision mismatch = review_required
FR-READ-009 all known requirements resolved = ready_with_known_scope
FR-READ-010 confidence .999 cannot skip review
FR-READ-011 event_stream absence cannot improve readiness
FR-READ-012 deterministic result

## 7. graphRevision Final Audit

只有 Confirmed Reality mutation 才 +1。

必须 +1：
Dependency confirm/create
Dependency retire
Dependency reactivate
confirmed criticality change
Group confirm
Group retire
confirmed Group membership change

不能 +1：
Observation
Evidence update
Fingerprint
Proposal create/update
Candidate
Drift create
Timeline projection
Verification suggestion only
UI state

Reality mutation + graphRevision update 必须在同一 DB transaction。

注入失败验证：
mutation fail → revision 不变
revision write fail → Reality mutation rollback

## 8. graphRevision Invariants

至少：
revision monotonic
no gaps caused by failed transaction
duplicate replay does not bump
restart preserves revision
migration preserves baseline
non-Reality writes never bump

property-based 随机 Reality/non-Reality operations：
验证 revision 与 committed Reality mutations 一致。

## 9. ChangePlan Rebase Final Audit

currentGraphRevision > lastAnalyzedGraphRevision
且 plan 未 completed/cancelled：
effective state = needs_revalidation

Rebase 必须重新 simulateScenario。

输出：
addedImpacts
removedImpacts
changedImpacts
addedActions
removedActions
changedActions

必须 deterministic sorting。

禁止：
silently overwrite old plan
auto-complete new actions
reopen completed plan
reopen cancelled plan

## 10. Completed Plan Historical Integrity

completed plan 必须保持历史稳定。

后续 Graph 变化不得把 completed plan 改写成 needs_revalidation。
可以显示：
historical plan based on revision X

## 11. ScenarioCoverage Final Audit

ScenarioCoverage 是覆盖描述，不是安全评分。

允许：
unknown
limited
partial
well_evidenced

禁止：
safe
complete
100%
risk-free

必须可解释：
used sources
freshness
confirmed deps
pending proposals
unresolved candidates
stale deps
unknown criticality
verification gaps

event_stream absence 不得提高 coverage。
source 数量多不得自动 well_evidenced。

## 12. RealityDrift Final Audit

RealityDrift 只能由：
positive new Evidence
或 explicit user input
触发。

禁止 absence-only drift。

confirmed A + new positive B evidence → drift
confirmed A + new import missing A → NO drift

Drift 创建不得改变 Graph。

只有用户明确：
replacement
additional path
才允许 Reality mutation。

mutation 成功：
graphRevision +1。

## 13. Drift Dedup / Upsert

同一逻辑 Drift 不能重复创建多个 open item。

定义 deterministic drift key。

测试：
duplicate evidence
same source
multi-source
repeated import
restart
dismissed drift
new genuinely different evidence

## 14. DiscoveryCandidate Final Audit

Candidate != Node

Candidate：
不进 Impact
不 bump revision
不改变 Graph
不自动创建 Dependency

accept：
创建一个 logical Node。

accept replay：
不能 duplicate Node。

dismiss：
不创建 Node。

## 15. Candidate Privacy

不得保存：
raw statement
full URL
page content
query string
raw transaction row
secret

只保存必要 normalized identity / label / provenance / evidence refs。

## 16. ScenarioTemplate Final Audit

Runtime active templates 当前只允许真正支持的 payment templates。

至少：
replace_payment_card
expiring_payment_card
close_payment_instrument

所有 active template：
factory != null
supported capability implemented
required inputs valid

所有 factory == null：
availability = planned
且：
不能 execute
不能 create ChangePlan
production UI 不显示或明确禁用
不进入 Timeline

必须有 code-level invariant，不只靠 UI。

## 17. ScenarioTemplate Policy Gate

继续阻止通用提醒进入 PDIG：
喝水
生日
女神节
植物浇水
普通会议
普通考试
普通运动

只有涉及：
digital identity
access
payment
recovery
control
data availability
digital service continuity
的模板才允许进入 PDIG catalog。

## 18. Timeline Final Audit

Timeline 必须是 projection / read model，
不能成为第二份 Truth。

必须有 sourceType / sourceId，
可追溯到：
ChangePlan
Drift
Verification
Node expiry
freshness review

删除/隐藏 TimelineItem 不得直接改变 Graph Reality。

排序必须 deterministic：
overdue / attention
today
7d
30d
90d
later

同组：
priority
then deterministic tie-breaker。

## 19. Timeline Performance

重新 smoke：
1k items
10k items 可选

只防明显退化。

记录：
docs/MVP03_FREEZE_PERFORMANCE.md

## 20. Verification Final Audit

冻结状态：
not_required
pending
evidence_suggested
verified
failed

规则：
done != verified

future observation：
pending → evidence_suggested

用户确认后：
verified

authoritative_source 只保留未来接口；
当前不得擅自自动 verify，除非已有明确 authority policy。

## 21. Verification Tests

至少：
action done != verified
manual confirm = verified
future observation = evidence_suggested
duplicate evidence no duplicate verification
wrong source no verification
evidence suggestion no Reality mutation
verified survives restart
failed transition validation
invalid transitions rejected

## 22. State Machine Audit

显式 transition validation：

ChangePlan：
draft
analyzed
review_required
ready
in_progress
verifying
completed
cancelled
effective: needs_revalidation

RealityDrift：
open
confirmed_change
dismissed
superseded

DiscoveryCandidate：
pending
accepted
dismissed
superseded

Verification：
not_required
pending
evidence_suggested
verified
failed

非法 transition 必须 reject。

## 23. Schema v3 Freeze

验证：
fresh v3
v2→v3
v2→v3→restart
migration ×50
failure rollback
all old IDs preserved
SourceInstances preserved
Evidence preserved
Proposal decisions preserved
Group state preserved
graphRevision initialized correctly
no orphan new tables

## 24. Payload v3 / .depmap

保持：
DEPMAP_CONTAINER_V1

验证：
old Golden Vector unchanged
payload v3 export/import
v2 payload import→migrate→v3
v1 payload import→migrate→v3
unsupported future payload reject
wrong password reject
tamper reject
partial plaintext impossible

## 25. Migration Matrix

生成：
docs/MVP03_MIGRATION_MATRIX.md

至少：
fresh v3
DB v2→v3
payload v1→v3
payload v2→v3
payload v3→v3
future v4→reject

每项 PASS/FAIL。

## 26. Security / Privacy Re-Audit

MVP03 新对象不得保存：
raw statement
transaction history
full card number
password/token/key
full browser data
sensitive debug payload

RealityDrift 只引用：
Node IDs
Dependency IDs
Proposal/Evidence refs
minimal summary

Candidate 只保存 minimal normalized metadata。
Timeline 不复制完整 Graph/Evidence。
ScenarioTemplate 无 user secret。

继续要求：
business network calls = 0
analytics = 0
ads = 0
telemetry = 0
production secrets = 0

## 27. Logging Re-Audit

扫描新增代码中的：
console.log
console.error
print
println
Log.
NSLog
HiLog

禁止输出：
raw Evidence
raw source rows
Graph dump
candidate raw values
transaction ID
crypto material
password

调试只允许 IDs / enum / count。

## 28. Architecture Re-Audit

继续保证：
Domain 不依赖 UI
UI 不实现 readiness rules
Timeline 不写 Graph
ScenarioTemplate 不直接写 Dependency
Drift 不越过 confirmation 写 Reality
Candidate 不进 Impact
Verification suggestion 不写 Reality
graphRevision 由 Reality transaction 管理

运行：
npm run check:architecture
以及 circular dependency check。

## 29. Invariant Tests

至少：
revision monotonic
revision only for Reality mutation
ready plan must be revision-current
completed plan history immutable
Candidate cannot enter Impact
Drift cannot mutate Reality without resolution
active ScenarioTemplate must have executable factory
Timeline source reference valid
verified action state valid
no duplicate logical Drift
no duplicate accepted Candidate Node
Rebase deterministic
readiness never false-ready from action count mismatch

## 30. Property-based Tests

fast-check 或现有 harness。

至少：

Random Reality operations：
revision equals committed Reality mutations。

Random plan/action/impact mapping：
readiness never ready_with_known_scope if any must_change requirement unresolved。

Random Graph changes：
rebase deterministic。

Random Drift evidence：
absence-only never produces Drift。

Random Candidate operations：
Candidate never reaches Impact before acceptance。

Random Timeline ordering：
deterministic stable sort。

失败 seed 必须可复现。

## 31. Mutation Testing

针对：
PlanReadiness
graphRevision
RealityDrift
PlanRebase
Verification transition

重点杀：
=== ↔ !==
revision comparison
blocked/review branch
absence condition
user-confirmed guard
verified transition
action/impact mapping

目标不是刷分。
记录 survived critical mutants。

如果 critical survived：
补测试。

生成：
docs/MVP03_MUTATION_FREEZE_REPORT.md

## 32. Coverage

冻结关键模块至少：
graphRevision branch ≥95%
PlanReadiness branch ≥95%
PlanRebase ≥90%
RealityDrift ≥90%
DiscoveryCandidate ≥90%
ScenarioCoverage ≥90%
ScenarioTemplate registry ≥90%
Timeline ≥90%
Verification ≥90%

不得 exclude 核心代码制造高覆盖率。

## 33. Flaky / Stability

完整测试至少连续 3 次。

关键模块 focused 至少连续 10 次：
graphRevision
PlanReadiness
PlanRebase
RealityDrift
Timeline
Verification

必须 0 flaky。
禁止 retry 掩盖。

生成：
docs/MVP03_STABILITY_REPORT.md

## 34. Performance Smoke

至少：
100 ChangePlans
500 Drifts
500 Candidates
1000 TimelineItems
1000-node Graph Rebase

记录：
docs/MVP03_FREEZE_PERFORMANCE.md

## 35. UI Semantic Audit

检查当前实际页面。

重点：

首页：
answer-oriented
不以 Nodes/Edges 为主入口
需要处理 / 即将到来 / 常用场景

场景库：
只展示 active executable templates
planned 不冒充可执行

ChangePlan：
blocked / review / ready 文案准确
needs_revalidation 明显
coverage != readiness

Drift：
明确“可能变化”
不暗示已变更

Candidate：
明确“发现候选”
不暗示已加入 Graph

Timeline：
来源可追溯
projection semantics

Verification：
done 不显示成 verified

生成：
docs/MVP03_UI_FREEZE_AUDIT.md

## 36. Copy / Wording Audit

禁止：
100% 安全
绝对不会遗漏
完全可以注销
已确认变更

除非真实状态支持。

统一使用：
基于当前已知并确认的信息
可能发生变化
需要你确认
建议检查
已发现新的证据

## 37. README / Docs Freeze

确认：
README
CANONICAL_DESIGN
WORK_STATUS
MVP03_ACCEPTANCE
MVP03_FINAL_REPORT

与代码一致。

不得保留：
旧测试数量
MVP02-only 文案
未实现 Future feature 当作支持
planned Scenario 当 active

## 38. Engineering Baseline Regression

必须运行：
npm run check
npm run check:full

并确认：
format PASS
lint PASS
typecheck PASS
unit/integration PASS
contract PASS
migration PASS
invariant PASS
property PASS
architecture PASS
circular deps PASS
network zero PASS
secret scan PASS
dependency/license PASS
clean install PASS
clean clone PASS

## 39. MVP01 / MVP02 Regression

必须确认：
MVP01 regression = PASS
MVP02 regression = PASS

重点：
WeChat
Generic CSV
OFX/QFX
SourceInstance
Fingerprint scope
multi-source Evidence
RelationRegistry
Impact
DEPMAP Golden Vector

## 40. Freeze Acceptance

更新：
MVP03_FREEZE_ACCEPTANCE.md

只能依据真实证据勾选。

## 41. Final Freeze Report

生成：
MVP03_FREEZE_REPORT.md

必须包含：

MVP03_FINAL_FREEZE =
PLAN_READINESS_CORRECTNESS =
GRAPH_REVISION_ATOMICITY =
PLAN_REBASE =
SCENARIO_COVERAGE =
REALITY_DRIFT =
DISCOVERY_CANDIDATE =
SCENARIO_TEMPLATE =
TIMELINE_PROJECTION =
ACTION_VERIFICATION =
STATE_MACHINES =
SCHEMA_V3 =
V2_TO_V3_MIGRATION =
PAYLOAD_V1_V2_TO_V3 =
DEPMAP_CONTAINER_V1_COMPAT =
INVARIANTS =
PROPERTY_TESTS =
MUTATION_TESTS =
COVERAGE =
FLAKY =
PERFORMANCE =
UI_SEMANTICS =
SECURITY_PRIVACY =
NETWORK_ZERO =
MVP01_REGRESSION =
MVP02_REGRESSION =
ENGINEERING_BASELINE_V1 =
REAL_DATA = NOT_RUN

每项只能：
PASS
FAIL
BLOCKED
NOT_RUN
PARTIAL_WITH_REPORT

## 42. 最终 PASS 标准

只有以下全部满足，才能：

MVP03_FINAL_FREEZE = PASS

必须：
PlanReadiness 无 false-ready 结构风险
graphRevision transactionally correct
Rebase deterministic/correct
Drift absence-safe
Candidate isolation correct
active template executable invariant
Timeline projection semantics correct
Verification done != verified
state machine invalid transitions rejected
Schema v3 migration PASS
payload migration PASS
DEPMAP V1 compatible
MVP01/MVP02 regression PASS
Engineering Baseline PASS
Security/Privacy PASS

Mutation 允许 PARTIAL_WITH_REPORT，
前提：没有存活的 critical correctness mutant。

Real Data 允许 NOT_RUN。
平台编译外部 Blocker 可继续存在。

## 43. Git Freeze

结束前：
git status
git diff --check

确认：
0 production secrets
0 real user data
0 temp junk
0 accidental binaries

合理 commit。

建议：
fix(readiness): harden requirement resolution semantics
test(mvp03): close freeze correctness gaps
docs(mvp03): finalize freeze evidence
chore(release): freeze mvp03

不要自动 push，除非用户明确要求。

如果全部 PASS：
可创建本地 tag：
v0.3.0-mvp03

若 tag 已存在：
不要重复。

## 44. WORK_STATUS

更新：
MVP03 FINAL FREEZE = PASS / FAIL

只有 PASS 后，Next 才写：
MVP04 — International Payment Infrastructure

但不要启动 MVP04。

## 45. 中断恢复

如果任务中断：
结束前必须更新 WORK_STATUS。

下一任务只需读取：
AGENTS.md
WORK_STATUS.md
MVP03_FREEZE_ACCEPTANCE.md
GOAL_MVP03_FINAL_FREEZE_RC_AUDIT.md

从第一个未完成 Gate 继续。

## 46. 现在开始

不要再给我架构建议。

实际执行：

1. Freeze pre-audit
2. PlanReadiness P0 audit
3. graphRevision atomicity
4. Rebase
5. ScenarioCoverage
6. RealityDrift
7. DiscoveryCandidate
8. ScenarioTemplate
9. Timeline
10. Verification
11. state-machine validation
12. Schema/payload migration
13. invariant/property/mutation
14. stability/performance
15. security/privacy
16. UI semantics
17. MVP01/MVP02 regression
18. npm run check
19. npm run check:full
20. Freeze report
21. Git freeze/tag

只有 `MVP03_FREEZE_REPORT.md` 给出真实：

MVP03_FINAL_FREEZE = PASS

后才停止。

现在开始实际执行。
