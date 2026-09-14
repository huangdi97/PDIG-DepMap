# MVP03_ACCEPTANCE.md — 逐 Gate 验收（只能按真实测试证据勾选）

> 恢复入口四件套：AGENTS.md / WORK_STATUS.md / 本文件 / GOAL_MVP03_LIVING_GRAPH_CHANGE_SAFETY.md
> 勾选依据：MVP03_FINAL_REPORT.md 收口轮实跑（427 tests / check PASS / check:full PASS / stability ×3 PASS）。

## A Graph Revision

- [x] GR-001 fresh graph revision = 0
- [x] GR-002 confirm Dependency → +1
- [x] GR-003 update Evidence → 不变
- [x] GR-004 create Proposal → 不变
- [x] GR-005 retire Dependency → +1
- [x] GR-006 reactivate Dependency → +1
- [x] GR-007 confirm Group → +1
- [x] GR-008 migration/restart 保持 revision
- [x] GR-009 failed transaction → revision 不增加
- [x] GR-010 sequential transactions 不丢 revision
- [x] GR-011 duplicate accepted replay 不重复增加
- [x] GR-012 deterministic

## B ChangePlan Rebase

- [x] PRB-001 revision unchanged → no rebase
- [x] PRB-002 revision changed → needs_revalidation
- [x] PRB-003 new Dependency creates new impact
- [x] PRB-004 retired Dependency removes impact
- [x] PRB-005 unknown relation remains review
- [x] PRB-006 Proposal only does not bump revision
- [x] PRB-007 rebase updates lastAnalyzedGraphRevision
- [x] PRB-008 rebase does not auto-complete actions
- [x] PRB-009 completed plan remains historical
- [x] PRB-010 cancelled plan not reopened
- [x] PRB-011 deterministic diff ordering

## C PlanReadiness

- [x] must_change pending → blocked
- [x] must_change done + needs_review → review_required
- [x] unknown criticality → review_required
- [x] unresolved candidate → review_required
- [x] revision mismatch → review_required
- [x] all known requirements resolved → ready_with_known_scope
- [x] Proposal confidence .999 不得绕过 review（PI-3 结构性断言）
- [x] absence 不得提高 readiness（PI-3 结构性断言）

## D ScenarioCoverage

- [x] 无来源 → unknown
- [x] 只有旧来源 → limited
- [x] 部分确认 → partial
- [x] 新来源 + 关键关系确认 → well_evidenced
- [x] coverage 不等于 ready
- [x] 新 pending Proposal → pending 说明
- [x] event_stream absence 不得提高 coverage

## E RealityDrift

- [x] RD-001 confirmed A + new positive B → drift
- [x] RD-002 absence of A alone → no drift（入口无 absence 通道）
- [x] RD-003 drift does not mutate Graph
- [x] RD-004 dismiss → Graph unchanged
- [x] RD-005 confirm replacement → Graph changes + revision increments
- [x] RD-006 confirm additional path → old path remains
- [x] RD-007 duplicate evidence does not duplicate Drift
- [x] RD-008 same drift upsert
- [x] RD-009 cross-source provenance retained
- [x] RD-010 rejected Proposal 不产生 confirmed drift

## F DiscoveryCandidate

- [x] 同 normalized key upsert
- [x] 不同 SourceInstance provenance
- [x] accept → one Node
- [x] accept replay → no duplicate Node
- [x] dismiss → no Node
- [x] dismissed 后保守重提（阈值 2）
- [x] Candidate 不进入 Impact
- [x] Candidate 不 bump graphRevision

## G ScenarioTemplate

- [x] 3 个 active 支付模板（replace/expiring/close）
- [x] planned 模板不进可执行注册（factory=null，instantiate 拒绝）
- [x] scenarioFactory 产出 ChangePlan（含 templateId）
- [x] 通用提醒类模板被政策禁止（ST-005 + SCENARIO_TEMPLATE_POLICY.md）

## H Timeline

- [x] bucket 排序（attention/overdue→today→7d→30d→90d→later）+ priority + tie-break
- [x] 只含数字基础设施类别
- [x] TimelineItem 可溯源（sourceType/sourceId）
- [x] Timeline 不是 Reality（投影不写库）

## I Verification

- [x] Action done ≠ verified
- [x] manual confirm → verified
- [x] future evidence → evidence_suggested
- [x] evidence suggestion 不修改 Reality
- [x] duplicate evidence 不重复 verification
- [x] wrong source 不产生 suggestion
- [x] verified 状态 restart 后稳定

## J Migration（Schema v2 → v3）

- [x] fresh v3（MIG3-001）
- [x] v2 → v3（MIG3-002；既有数据与 ID 不变）
- [x] v2 → v3 → restart（MIG3-003）
- [x] migration ×50（MIG3-005 + 既有 T4/T4b）
- [x] failure rollback（MIG3-004 + 既有 T8）
- [x] old v2 payload import → migrate → v3（J5c）
- [x] unsupported future schema reject（MIG3-006 + J3）
- [x] DEPMAP container / golden 不变（crypto 套件全绿）

## K depmap compatibility

- [x] golden vector 回归通过
- [x] payload v3 export/import 往返（J0/J1）
- [x] v1/v2 payload in-memory migrate → v3（J2c/J5/J5c）

## L/M/N 回归与基线

- [x] MVP01 regression PASS（既有用例全绿）
- [x] MVP02 regression PASS（273 基线全绿）
- [x] Engineering Baseline v1 PASS（check PASS 427 tests；check:full PASS；stability ×3 PASS）

## O Security/Privacy

- [x] 新对象零 raw statement / transaction history / token / full card number
      （Drift/Candidate/Timeline 只存引用与 ID；SCHEMA V3 DDL 复核）
- [x] network/analytics/ads/telemetry = 0（check:network 103 files PASS）
- [x] secret scan PASS（331 files，0 production secrets）

## P UI

- [x] 首页 answer-oriented（需要你处理 / 即将到来 / 常用场景 / 我的基础设施）
- [x] 场景库（active 显示、planned 不显示）
- [x] Plan Detail（readiness / rebase 提示 / verification）
- [x] Drift 卡片（四种处理动作）
- [x] Timeline 页
- [x] 静态审计刷新（未编译 B10 如实声明，见 docs/UI_SOURCE_AUDIT.md 口径）

## Q Documentation

- [x] 13 份 MVP03 docs + README + CANONICAL 附录整合
- [x] MVP03_FINAL_REPORT.md（真实 verdict）
