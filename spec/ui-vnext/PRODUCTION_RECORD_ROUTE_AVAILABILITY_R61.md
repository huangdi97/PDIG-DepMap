# PDIG R61 — Production Records live-route availability & historical continuity

> Android Production VNext · 2026-10-17 · light-first consumer UI  
> Branch: `feat/android-ui-vnext-translation`  
> Gate at authoring: **SOURCE IMPLEMENTED / latest-head CI + API36 runtime pending / HUMAN ACCEPTANCE HOLD**  
> Scope: consumer navigation + historical-record presentation + regression tests. No Canonical, PersonalReality, `.depmap`, Production cutover or verification authority changes.

## 1. Actual R60 source gap

R60 correctly re-reads a governed ChangePlan and current PlanAction before navigating from Records. However, `ProductionRecords` previously showed `查看计划` and `查看对应步骤` for **any readable plan**, while `VAppState.openProductionPlan` only supports two focused consumer routes:

- `replace_payment_card` → `CHANGE_CARD`;
- `replace_phone_number` → `CHANGE_PHONE`.

A third, entirely valid governed scenario could therefore render a live-looking CTA whose click silently returned without changing screens. Similarly, a historical PlanAction absent from the current authoritative plan correctly lost its CTA but did not explain *why*. This is a discoverability/correctness defect, not a reason to delete historical evidence.

## 2. R61 source corrections

1. `productionPlanDestination(scenario)` is the single consumer-route mapping used by both `VAppState.openProductionPlan` and Production Records. Unsupported scenarios return `null`; they remain legitimate governed records but **cannot claim a focused detail screen**.
2. A Records plan group only offers `查看计划` when the current governed plan exists **and** its scenario has a consumer route. Action CTAs additionally require the matching live PlanAction.
3. Both plan-header and action click handlers re-read current authority at the instant of interaction and re-check route + action existence. A route becoming unsupported or a step disappearing between composition and click is never a navigation permit.
4. Unavailable steps remain visible as evidence with a short, privacy-safe explanation, classified by **unreadable plan**, **unsupported consumer route**, or **historical action no longer present**. Do not display `查看对应步骤` on these cards.
5. Live plan headers advertise `Role.Button` to accessibility services; read-only headers do not advertise a button. Actual tappable targets retain minimum 48dp.
6. The data shown by Records (verification state, evidence count and unknown occurrence time) is unaffected. `completed != verified`; a missing PlanAction is not fabricated as a new step or marked done.

## 3. Display states

| Governed state on current read | CTA | Honest inline text |
| --- | --- | --- |
| Readable plan, supported route, live action | `查看计划` and `查看对应步骤` | none |
| Unreadable plan | neither | `当前正式计划无法读取 · 仅保留操作记录` |
| Readable plan, unsupported scenario | neither | `当前版本尚未提供此变更类型的步骤页面 · 仅保留操作记录` |
| Readable supported plan, historical action missing | plan header may navigate; **no** action CTA | `对应步骤已不在当前正式计划中 · 仅保留操作记录` |

These are consumer affordance states, **not Canonical relationship/verification states**. Global and per-object privacy masking still hides identity-bearing historical titles and does not change whether the record is inspectable.

## 4. Regression source

- JVM `R11NavigationAndAliasesTest.recordsOnlyAdvertiseActualSupportedGovernedChangeRoutes`: supported scenarios and unsupported route refusal without presentation mutation.
- JVM `ProductionRecordTraceTest.historicalRecordsExplainWhyTheirGovernedActionCannotBeOpened`: all four availability cases, including live route.
- Android Instrumentation `ProductionRecordNavigationContractTest`: live action CTA navigates, unsupported scenario shows explanation without ghost action, stale action remains read-only and visibly explained, prior masked historical records remain nonleaking.
- Full exact-source CI + Phone Pixel + Tablet Pixel + Production Rehearsal must be checked; no prior R60 PASS transfers to this source.

## 5. Human review and release control

Inspect Records after `需处理` selection on Phone and Tablet, including live action, missing historical action and unsupported scenario states. The distinction between actionable blue CTA and muted explanatory line should be legible without shrinking core text below 12sp. The exact action should remain reachable by system Back through the existing R60 chronology.

Five first-class roots remain **现在 / 基础设施 / 变更 / 记录 / 我**. The PR remains Draft, Production release stays Legacy default, and the following are not claimed by this file:

```text
HUMAN_VISUAL_ACCEPTANCE  = PENDING
ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_CUTOVER       = HOLD
```
