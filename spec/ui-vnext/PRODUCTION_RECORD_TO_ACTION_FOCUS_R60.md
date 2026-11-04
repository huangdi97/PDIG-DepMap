# PDIG R60 — Records → Exact PlanAction, Consumer Verification Continuity

> Android Production VNext · 2026-10-14 · light-first  
> Source: `feat/android-ui-vnext-translation`  
> Gate: **SOURCE IMPLEMENTED / latest-head CI & API36 runtime pending / HUMAN ACCEPTANCE HOLD**  
> No Canonical, PersonalReality, .depmap schema, or release-default change.

## 1. Problem proven from R59 source

The governed Records scope can open a **plan group**, but the selected record may be one step among many.
The user must manually scroll through the whole plan to find the precise pending/failed action. Also,
`VAppState.openProductionPlan()` only set `navBackTarget`, not the chronological `backStack`;
System Back from Records → ChangePlan could skip Records instead of restoring the selected `需处理` lens.

This breaks the central intent: **Now → Records / 需处理 → exact step → Back → same Records lens**.

## 2. Shipped source design

- Every readable current-plan record card exposes an explicit **查看对应步骤 →** affordance
  (48dp minimum, `Role.Button`, stable semantics tag). Group header remains clickable.
- On tap, re-read the current authoritative `productionPlan(record.planId)` and check that
  `actionId` still exists in that plan. A stale record cannot authorize navigation/mutation.
- Open the existing governed ChangePlan detail with `focusActionId` held only in
  `VAppState.selectedProductionActionId`. No schema or preferences persistence.
- The ChangePlan LazyColumn scrolls to the actual source-plan action position, accounting for
  the current plan's optional unresolved-impact banner and privacy boundary.
- A focused step receives a restrained light-blue border and **从记录定位 · 请核对当前步骤**.
  That is a presentation focus, **not** a verification or completion state.
- If the action is no longer in the current read model, do not synthesize it or scroll to
  an unrelated action. Display a stale-focus explanation only.
- `openProductionPlan()` now pushes the actual source destination to chronological Back;
  on return the previous Records filter (e.g. ATTENTION / FAILED) remains selected.
  New unrelated plan openings clear the old action focus. Unsupported scenarios fail closed.

## 3. Truth and safety invariants

```text
completed != verified
Record ≠ event chronology
missing occurredAt != today
ATTENTION = actual PENDING_VERIFICATION ∪ VERIFICATION_FAILED
focusActionId != authority
record cache != executable plan
local/global masking persists through record → plan
After = Plan Projection, not Reality
```

An action card may show a governed `completeAction` or `verifyAction` affordance only when
the existing AppContainer-owned preconditions allow it. The new record navigation itself
does **not** call either method. Masked actions remain non-mutatable until verified as
specified by the pre-existing ChangePlan truth boundary.

## 4. Regression and evidence requirements

- JVM `R11NavigationAndAliasesTest`: actual chronological Back from Records to plan and
  back; previous scope preserved; stale focus not retained; unknown scenario cannot route.
- JVM `ProductionChangeStageSummaryTest`: deterministic visible action index for
  plain, unresolved, masked and removed action cases.
- API36 `ProductionVNextShellContractTest`: tap Now verification attention; select a real
  record; open its authoritative action; assert focus banner & action visible; System
  Back restores ATTENTION scope and correct record count.
- Exact-head Android App JVM/build + Canonical/Core/Harmony + iOS + Phone Pixel +
  Tablet Pixel + Production Rehearsal must finish. Do not borrow earlier PASS.
- Human visual review: Now card → Records status tabs → focused step on Phone and Tablet,
  including light typography, scroll position, 48dp targets, masked states and Back.
- Any new failure must be debugged against current remote HEAD; no weakened assertions.

## 5. Release boundary

The PR remains **Draft**. Five primary roots remain **现在 / 基础设施 / 变更 / 记录 / 我**.
Preview remains synthetic. Production release remains **Legacy by default**.
`HUMAN_ACCEPTED`, `ANDROID_REFERENCE_FREEZE`, `PRODUCTION_CUTOVER` remain **HOLD**
unless explicitly verified/authorized independently.
