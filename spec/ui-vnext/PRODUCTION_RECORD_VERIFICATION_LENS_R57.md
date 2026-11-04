# PDIG R57 — Production Records Verification Lens

> Status: **SOURCE IMPLEMENTED / exact-head JVM, instrumentation and human visual review pending**  
> Track: Android-first light consumer UI / R56 privacy and authoritative ChangePlan continuation  
> Scope: `ProductionRecords`, `ProductionRecordTrace`, `ProductionRecordTraceTest` only. No Canonical migration.

## 1. User problem

The records page correctly shows a PlanAction verification trace, but grouping alone forces the
user to read every plan and card before answering the immediate question:
**“哪些步骤仍然需要验证，哪些已完成，哪些验证失败？”**

The light consumer reference calls for clear state hierarchy rather than dense maintenance rows.
A real chronological timeline is **not possible yet**, because `VNextProductionRecordItem.occurredAt`
is intentionally null when the authoritative action has no occurrence time.

## 2. Implemented read-only design

The existing plan groups and plan-detail navigation are preserved. When real records exist,
the page shows a horizontally scrolling, minimum-48dp status rail before the plan groups:

| Lens | Source state | Semantics |
| --- | --- | --- |
| 全部 | all records | actual recorded PlanActions |
| 待验证 | `PENDING_VERIFICATION` | recorded actions awaiting verification; not “unsafe” by inference |
| 已记录完成 | `RECORDED_COMPLETE` | done does not imply verified |
| 已验证 | `VERIFIED` | source says verified; lack of evidenceRefs remains visible |
| 验证失败 | `VERIFICATION_FAILED` | a real failed-verification state; never auto-retried |

Counts derive from the current `productionRecords()` projection. They are not:
a risk score, completion percentage, source-provider live status, or a count of independent recovery routes.

Selected state uses a restrained light-blue surface and stronger readable text.
The filtered plan-group list retains original group membership and record order from the read model:
**no date ordering, no fake “today” entry, no fabricated evidence.**

When a selected filter has no entries, the page says:
`当前筛选下没有已记录的操作；不表示其他状态或未记录历史不存在。`

## 3. Authority and privacy

- No record edits occur when filtering. `completeAction` and `verifyAction` remain gated by AppContainer.
- `ProductionRecordPlanGroup` continues to re-read the governed ChangePlan before navigation.
- Masked targets and unreadable plans continue to redact both plan titles and action titles.
- Filtering changes only the visible subset; it cannot unmask identifiers or make an unavailable plan readable.
- Record time remains `发生时间未记录` whenever the authoritative model has no timestamp.
- Completion and verification remain two separate state transitions.

## 4. Source tests

`ProductionRecordTraceTest.verificationStatusFiltersOnlyReadGovernedActionState`
covers all five scopes, zero-record count, source identity preservation, and evidence/time non-invention.

## 5. Exact-head runtime acceptance to perform

1. Build `:app:testProductionDebugUnitTest`, run the above JVM test, ensure no Compose import regression.
2. API36 productionDebug with a governed local test fixture containing one PlanAction in each state.
3. Navigate `记录` from the fifth-root consumer nav; assert all five chips visible/reachable
   via horizontal scrolling at compact widths and touch targets >=48dp.
4. Tap each chip, assert only the corresponding state cards remain and the count is correct.
5. Toggle global masking and one local card/phone target mask; neither title nor evidence-source
   identity may leak through any selected scope.
6. Open one readable plan from the filtered group; ensure navigation re-reads current authority.
7. Revoke or delete one plan from the authoritative source: cached record title must not open
   the missing plan and must remain redacted while unreadable.
8. Capture Phone compact and Tablet medium/expanded screenshots for all filters;
   compare hierarchy, typography and scroll clipping to the selected human light reference.
9. Do not claim Human Visual Acceptance or Android Reference Freeze without human approval.

## 6. Release rule

`SOURCE_IMPLEMENTED` does not mean `RUNTIME_VERIFIED`. PR #1 remains Draft.
Production default remains legacy. This change does not authorize Production cutover.
