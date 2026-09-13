# MVP03_ACCEPTANCE.md — 逐 Gate 验收（只能按真实测试证据勾选）

> 恢复入口四件套：AGENTS.md / WORK_STATUS.md / 本文件 / GOAL_MVP03_LIVING_GRAPH_CHANGE_SAFETY.md

## A Graph Revision
- [ ] GR-001 fresh graph revision = 0
- [ ] GR-002 confirm Dependency → +1
- [ ] GR-003 update Evidence → 不变
- [ ] GR-004 create Proposal → 不变
- [ ] GR-005 retire Dependency → +1
- [ ] GR-006 reactivate Dependency → +1
- [ ] GR-007 confirm Group → +1
- [ ] GR-008 migration/restart 保持 revision
- [ ] GR-009 failed transaction → revision 不增加
- [ ] GR-010 sequential transactions 不丢 revision
- [ ] GR-011 duplicate accepted replay 不重复增加
- [ ] GR-012 deterministic

## B ChangePlan Rebase
- [ ] PRB-001 revision unchanged → no rebase
- [ ] PRB-002 revision changed → needs_revalidation
- [ ] PRB-003 new Dependency creates new impact
- [ ] PRB-004 retired Dependency removes impact
- [ ] PRB-005 unknown relation remains review
- [ ] PRB-006 Proposal only does not bump revision
- [ ] PRB-007 rebase updates lastAnalyzedGraphRevision
- [ ] PRB-008 rebase does not auto-complete actions
- [ ] PRB-009 completed plan remains historical
- [ ] PRB-010 cancelled plan not reopened
- [ ] PRB-011 deterministic diff ordering

## C PlanReadiness
- [ ] must_change pending → blocked
- [ ] must_change done + needs_review → review_required
- [ ] unknown criticality → review_required
- [ ] unresolved candidate → review_required
- [ ] revision mismatch → review_required
- [ ] all known requirements resolved → ready_with_known_scope
- [ ] Proposal confidence .999 不得绕过 review
- [ ] absence 不得提高 readiness

## D ScenarioCoverage
- [ ] 无来源 → unknown
- [ ] 只有旧来源 → limited
- [ ] 部分确认 → partial
- [ ] 新来源 + 关键关系确认 → well_evidenced
- [ ] coverage 不等于 ready
- [ ] 新 pending Proposal → pending 说明
- [ ] event_stream absence 不得提高 coverage

## E RealityDrift
- [ ] RD-001 confirmed A + new positive B → drift
- [ ] RD-002 absence of A alone → no drift
- [ ] RD-003 drift does not mutate Graph
- [ ] RD-004 dismiss → Graph unchanged
- [ ] RD-005 confirm replacement → Graph changes + revision increments
- [ ] RD-006 confirm additional path → old path remains
- [ ] RD-007 duplicate evidence does not duplicate Drift
- [ ] RD-008 same drift upsert
- [ ] RD-009 cross-source provenance retained
- [ ] RD-010 rejected Proposal 不产生 confirmed drift

## F DiscoveryCandidate
- [ ] 同 normalized key upsert
- [ ] 不同 SourceInstance provenance
- [ ] accept → one Node
- [ ] accept replay → no duplicate Node
- [ ] dismiss → no Node
- [ ] dismissed 后保守重提
- [ ] Candidate 不进入 Impact
- [ ] Candidate 不 bump graphRevision

## G ScenarioTemplate
- [ ] 3 个 active 支付模板（replace/expiring/close）
- [ ] planned 模板不进可执行注册
- [ ] scenarioFactory 产出 ChangePlan（含 templateId）
- [ ] 通用提醒类模板被政策禁止

## H Timeline
- [ ] bucket 排序（overdue→today→7d→30d→90d→later）+ priority + tie-break
- [ ] 只含数字基础设施类别
- [ ] TimelineItem 可溯源（sourceType/sourceId）
- [ ] Timeline 不是 Reality

## I Verification
- [ ] Action done ≠ verified
- [ ] manual confirm → verified
- [ ] future evidence → evidence_suggested
- [ ] evidence suggestion 不修改 Reality
- [ ] duplicate evidence 不重复 verification
- [ ] wrong source 不产生 suggestion
- [ ] verified 状态 restart 后稳定

## J Migration（Schema v2 → v3）
- [ ] fresh v3
- [ ] v2 → v3（既有 Dependency/Group/Evidence/SourceInstance 数据与 ID 不变）
- [ ] v2 → v3 → restart
- [ ] migration ×50
- [ ] failure rollback
- [ ] old v2 payload import → migrate → v3
- [ ] unsupported future schema reject
- [ ] DEPMAP container / golden 不变

## K depmap compatibility
- [ ] golden vector 回归通过
- [ ] payload v3 export/import 往返
- [ ] v1/v2 payload in-memory migrate → v3

## L/M/N 回归与基线
- [ ] MVP01 regression PASS
- [ ] MVP02 regression PASS
- [ ] Engineering Baseline v1 PASS（check + check:full）

## O Security/Privacy
- [ ] 新对象零 raw statement / transaction history / token / full card number
- [ ] network/analytics/ads/telemetry = 0
- [ ] secret scan PASS

## P UI
- [ ] 首页 answer-oriented（需要你处理 / 即将到来 / 常用场景 / 我的基础设施）
- [ ] 场景库（active 显示、planned 不显示）
- [ ] Plan Detail（readiness / coverage / rebase 提示）
- [ ] Drift 卡片（四种处理动作）
- [ ] Timeline 页
- [ ] 静态审计刷新（未编译 B10 如实声明）

## Q Documentation
- [ ] 14 份 MVP03 docs + README + CANONICAL 整合
- [ ] MVP03_FINAL_REPORT.md（真实 verdict）
