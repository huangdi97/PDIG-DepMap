# GOAL_MVP03_LIVING_GRAPH_CHANGE_SAFETY.md — MVP03 执行目标

> 正式名称：MVP03 — Living Graph & Change Safety（活图谱、变更安全、场景模板与时间线）
> 恢复入口：AGENTS.md → WORK_STATUS.md → MVP03_ACCEPTANCE.md → 本文件。

## 核心问题

MVP02 解决「多源数据如何进入同一张图」；MVP03 解决「**图建立之后现实变化怎么办**」与
「**用户如何通过具体生活场景安全完成数字基础设施变更**」。

产品链：Evidence → Proposal → Human Confirmation → Confirmed Reality Graph →
**Living Graph Maintenance → Scenario → Impact → ChangePlan → Plan Readiness → Action →
Verification → Reality Update**。

## 实现范围（9 个闭环模块）

1. **graphRevision**：Confirmed Reality 的语义版本。仅 Reality mutation（dependency/group 的
   create/confirm-retire/reactivate/criticality 变更、group membership 确认变化）提升 revision，
   且与 Reality 写入**同事务**。Observation/ImportSession/Fingerprint/Evidence/Proposal/
   Drift/Candidate/Timeline/UI 状态一律不提升。存 `meta.graph_revision`（初始 0）。
2. **ChangePlan**：`{id, templateId?, scenario, title, baselineGraphRevision,
   lastAnalyzedGraphRevision, workflowState(draft|analyzed|review_required|ready|in_progress|
   verifying|completed|cancelled), scenarioInputs, impactSnapshot, actionItems(含 verification)}`。
   `needs_revalidation` 为 **derived effective state**（currentRevision > lastAnalyzed →
   needs_revalidation），不批量回写存储。
3. **Plan Rebase**：revision 不一致且计划未 completed/cancelled → 重新 simulateScenario，
   产出 deterministic diff（added/removed/changed impacts + actions），更新 lastAnalyzed；
   不自动完成 action、不改写 completed 历史、不重开 cancelled。
4. **PlanReadiness**（纯规则引擎，禁 LLM/confidence 映射）：`blocked` / `review_required` /
   `ready_with_known_scope`。禁止 safe/100%/all clear 文案。
5. **ScenarioCoverage**：信息覆盖度（unknown/limited/partial/well_evidenced）+ 可解释列表；
   不是安全评分，不由 source 数量算百分比；absence 不得提高 coverage。
6. **RealityDrift**：新正向 evidence 与 Confirmed Reality 的潜在变化信号
   （possible_replacement / possible_additional_path / relation_reappeared）。
   **absence 永不触发**；drift 永不自动改图；处理动作映射为显式 Reality mutation（revision+1）
   或 dismiss。status: open/confirmed_change/dismissed/superseded；同 key upsert。
7. **DiscoveryCandidate**：Observation → Candidate → 确认 → Node；与 DependencyProposal 链严格
   分开。candidate 不进 Impact、不 bump revision；accept 幂等（不重复建 Node）；
   dismissed 需真正新 evidence 才可能复现（保守）。
8. **ScenarioTemplate**：静态注册表（数字基础设施变更模板，非提醒商店）。
   本轮 active：`replace_payment_card` / `expiring_payment_card` / `close_payment_instrument`
   （可加 `replace_primary_payment_source`，不扩 Domain）。含 recommendedLeadTime（产品建议）。
9. **Timeline / Upcoming**：derived projection（needs_attention / upcoming_change /
   verification_pending / freshness_review / drift_review / expiration），bucket 排序
   （overdue → today → 7d → 30d → 90d → later，priority + deterministic tie-break）。
   TimelineItem 不是 Reality，必须可溯源。
10. **Action Verification**：action 完成 ≠ verified。verification state:
    not_required/pending/evidence_suggested/verified/failed；kind: manual_confirmation /
    future_observation / authoritative_source（只定义不自动执行）。future evidence →
    evidence_suggested（永不自动 verified）；wrong source 不产生 suggestion；restart 后稳定。

## 硬边界（永久）

- 第一原则照旧：宁可漏报。Observation ≠ Reality；Proposal ≠ Reality；Drift ≠ Reality change；
  Candidate ≠ Node；Template ≠ 业务真相；Timeline reminder ≠ Graph fact。
- Reality 只由 user_confirmed（MVP03 默认）改变；机器不设 required。
- 禁止：PayPal/Plaid/Open Banking/Browser/OAuth/手机号-邮箱完整 capability/云同步/后端/AI/
  LLM/N-of-M/跨 capability Impact/自动执行第三方操作（→ NEXT_BACKLOG / SCENARIO_CATALOG_FUTURE）。
- ScenarioTemplate 只收数字身份/访问/支付/恢复/控制/数据/服务连续性类事件（SCENARIO_TEMPLATE_POLICY）。

## Schema / 协议

- Schema **v3**（新增 change_plans / reality_drifts / discovery_candidates 三表；revision 入 meta）。
  test-first：fresh v3 / v2→v3 / restart / ×50 / 回滚 / 既有 ID 与数据不变 / 未来版本拒绝。
- **DEPMAP_CONTAINER_V1 不变**（container version ≠ app schema version；golden vector 冻结）。
- graph payload → **v3**（含 graphRevision；v1/v2 in-memory migrate 保留）。

## 工程（继承 Engineering Baseline V1）

- failing test → implementation → focused → regression；关键模块 property/invariant 必备
  （revision 单调性、mismatch 不得 ready、drift 不改图、candidate 不进 Impact、rebase/Timeline
  deterministic）。
- 人工 mutation：PlanReadiness / GraphRevision / RealityDrift 关键语义。
- 覆盖率目标：graphRevision/PlanReadiness branch ≥95%，Rebase/Drift/Coverage/Template/Timeline ≥90%。
- perf smoke：100 plans / 1000 timeline items / 500 drifts / 500 candidates / 1k-node rebase。
- 隐私：新对象零 raw statement/transaction/token/card number；network/analytics/ads/telemetry = 0。
- 收口：check + check:full + flaky ≥3（关键域建议 10）+ MVP03_FINAL_REPORT.md（只有全 PASS
  才允许 MVP03_LIVING_GRAPH_CHANGE_SAFETY = PASS；Real Data 允许 NOT_RUN）。

## 文档清单

docs/LIVING_GRAPH.md、GRAPH_REVISION.md、CHANGEPLAN_STATE_MACHINE.md、PLAN_REBASE.md、
PLAN_READINESS.md、SCENARIO_COVERAGE.md、REALITY_DRIFT.md、DISCOVERY_CANDIDATE.md、
SCENARIO_TEMPLATE.md、SCENARIO_TEMPLATE_POLICY.md、SCENARIO_CATALOG_FUTURE.md、
TIMELINE_UPCOMING.md、ACTION_VERIFICATION.md、MVP03_TEST_MATRIX.md；README 更新（如实、不宣传未实现）。

## 测试 ID 规约

GR-xxx（revision）/ PRB-xxx（rebase）/ RD-xxx（drift）/ PC-xxx（candidate）/
ST-xxx（template）/ TL-xxx（timeline）/ VF-xxx（verification）/ MIG3-xxx（migration）/
PI-xxx（property-invariant 扩展）。
