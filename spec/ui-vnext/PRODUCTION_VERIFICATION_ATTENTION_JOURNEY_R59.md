# PDIG R59 — Now → Records 验证事项连续体验契约

> Scope: Android Production vNext, light-first consumer UI  
> State: SOURCE IMPLEMENTED, exact-HEAD instrumentation / CI / Human Visual Acceptance **pending**  
> Predecessors: R57 status read lens, R58 governed verification attention and selectable filter semantics  
> This document neither grants Production Cutover nor changes the Canonical model.

## 1. User task and observed discontinuity

The R58 Now page already displays `需要验证的操作` when authoritative
`VNextProductionRecordItem` entries are `PENDING_VERIFICATION` or
`VERIFICATION_FAILED`. Before R59 the card only navigated to the
**unfiltered** Records page. A user had to manually find the statuses that the
entry explicitly promised. The Records filter also belonged to a
`remember(session)` local composable, so opening a real ChangePlan and
returning could drop the user's filter.

R59 closes these two consumer-journey gaps **without inventing authority**.

## 2. Read-only navigation contract

```text
Now ("需要验证的操作", only when actual action states require attention)
  -> Records / 需处理 [ATTENTION]
        = PENDING_VERIFICATION ∪ VERIFICATION_FAILED
        ordered exactly as the current governed records projection
        -> filtered plan groups -> read-only or governed plan detail -> System Back
        -> Records / previous chosen scope preserved

Ordinary bottom tab Records / header navigation
  -> Records / 全部 [ALL]
```

- `ATTENTION` is a UI-only filter over existing `VNextProductionRecordState`.
  It is **not** a new persisted action state, urgency grade, risk rating,
  chronological ordering or recovery-path estimate.
- `ALL`, `NEEDS_VERIFICATION`, `COMPLETED`, `VERIFIED`, `FAILED`
  keep their original semantics. `ATTENTION` is a clearly labeled sixth chip.
- Scope selection is held only in `VAppState.productionRecordsScope` (local
  presentation state); it is **not** written to encrypted Reality, schema,
  `.depmap`, PresentationProfile or preferences.
- From `Now`, `openProductionVerificationAttention()` chooses ATTENTION and
  navigates; from ordinary first-class root `navigate(RECORDS)` the selection
  resets to ALL. System Back from a plan restores previous Records selection.
- When there are zero pending/failed records, **Now does not create an entry**;
  explicitly selecting ATTENTION in Records instead renders the honest empty
  state, not a false all-clear.
- Every record remains linked to its authoritative PlanAction and current
  governed ChangePlan read. Unreadable plans stay masked and non-actionable.
- Plan and action titles obey global and object-local masking on every scope.
  Evidence references count only actual references. Missing occurrence time
  still displays `发生时间未记录`, never a fabricated event timestamp.
- `completeAction != verifyAction`; the record filters cannot trigger either
  mutation. After remains a Plan Projection and does not leak into Records as
  a completed historical event.

## 3. Accessibility and visual translation

- A 48dp minimum target is required for the Now verification card.
- Records chips remain horizontally scrollable in compact layouts, and
  `Modifier.selectable(role=Role.Tab)` exposes the selected state to TalkBack.
- The Now action has a stable semantics tag:
  `pdig.production-vnext.now.verification-attention`.
- The state chip has `pdig.production-vnext.records.scope.attention`.
- Cards are still grouped by plan, not ordered as a fake timeline. The
  authority-derived count visible in `验证轨迹 · N 项` must equal the actual
  selected records, never a hardcoded showcase number.
- Five product-level roots remain `现在 / 基础设施 / 变更 / 记录 / 我`; no new root.

## 4. Tests and acceptance

- JVM `ProductionRecordTraceTest.attentionLensCombinesOnlyGenuinelyPendingOrFailedStatesInSourceOrder`:
  covers source membership/order, true counts, empty projection and no
  timestamps/evidence invention.
- JVM `R11NavigationAndAliasesTest.recordAttentionEntryIsFocusedWhileNormalRootBrowsingRemainsComplete`:
  covers Now attention entry, editable filter retention through plan/back and
  ordinary Records root returning to ALL.
- API36 `ProductionVNextShellContractTest.nowVerificationAttentionOpensOnlyPendingAndFailedRecordsThenNormalRootShowsAll`:
  taps the real Now action, inspects selectable chip state and truthful list
  count, then tests normal root navigation.
- Mandatory new exact-head evidence: Android CI + Production Rehearsal +
  Phone/Tablet proof; screenshots and human review of Now action and Records
  selected-state accessibility. Prior R58 PASS does **not** prove R59.
- Status cannot be promoted to `HUMAN_ACCEPTED` or
  `ANDROID_REFERENCE_FREEZE=PASS` without the user's approval.
- PR #1 stays Draft; Production default remains Legacy until separate
  human/technical release authorization.
