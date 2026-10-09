# ANDROID_UI_VNEXT_R19_SOURCE_REPORT

> 2026-10-09 · `feat/android-ui-vnext-translation`
>
> R19 is the source/design closure after R18 lifecycle density. It adds the
> product-level Impact Lens, repairs recovery semantics, preserves the intentional
> five-item primary IA, closes adaptive phone/tablet/expanded parity for lifecycle facts, and
> freezes the production-binding architecture.
>
> This report **does not claim fresh runtime PASS**.

## 0. Truth status

```text
R19_REFERENCE_SOURCE_DESIGN = COMPLETE
R19_IMPACT_LENS = IMPLEMENTED_IN_REFERENCE_UI
R19_CARD_LIFECYCLE = IMPLEMENTED_IN_REFERENCE_UI
R19_NUMBER_LIFECYCLE = IMPLEMENTED_IN_REFERENCE_UI
R19_KEEP_NUMBER_ROLE = IMPLEMENTED_IN_REFERENCE_UI
R19_FIVE_ITEM_PRIMARY_IA = PRESERVED
R19_RECOVERY_UNIQUENESS_SEMANTICS = CORRECTED
R19_ADAPTIVE_PARITY = SOURCE_COMPLETE
R19_PRODUCTION_BINDING_ARCHITECTURE = DESIGN_FROZEN

CANONICAL_SCHEMA_CHANGE = NONE
DEPMAP_PAYLOAD_CHANGE = NONE
PRODUCTION_VNEXT_CUTOVER = HOLD

FRESH_BUILD = NOT_RUN
FRESH_UNIT_TESTS = NOT_RUN
FRESH_INSTRUMENTATION = NOT_RUN
PHONE_PIXEL_CAPTURE = REQUIRED
TABLET_PIXEL_CAPTURE = REQUIRED
GPU_RUNTIME_EVIDENCE = REQUIRED
HUMAN_PIXEL_ACCEPTANCE = REQUIRED

ANDROID_REFERENCE_FREEZE = HOLD
IOS_UI_TRANSLATION = HOLD
HARMONY_UI_TRANSLATION = HOLD
```

## 1. R19 closes the product hierarchy, not just the pixels

The v2.3 product architecture is:

```text
Inventory
→ Typed Dependency
→ Continuity Analysis
→ Change Orchestration
```

R18 made Card/Number inventory richer. R19 makes focused object details answer the
next product question:

> **如果它发生变化？**

That question is now visible as an Impact Lens rather than hidden behind generic
risk copy.

## 2. Impact Lens

Reference projection:
- confirmed dependency count;
- recorded attention findings;
- critical-account count when actually known, otherwise `未记录`;
- unique-recovery status only when explicitly evidenced, otherwise `未知`;
- independent alternatives only when actually known, otherwise `未记录`;
- explicit “未确认关系仍可能存在”.

Files:
- `android/app/src/main/kotlin/com/pdig/uivnext/demo/UiVNextImpactLens.kt`
- `android/app/src/main/kotlin/com/pdig/uivnext/ui/components/ObjectImpactLens.kt`
- `android/app/src/main/kotlin/com/pdig/uivnext/ui/r9/R19ImpactLens.kt`

The reference lens is deliberately conservative. It does not invent Canonical
FailureDomain/path-independence output that the synthetic fixture does not carry.

## 3. Recovery truth repair

A source audit found a semantic shortcut:

```text
recoveryOnly → 唯一恢复路径
```

That implication is invalid.

R19 separates:

```text
recoveryOnly
= this object has a recorded recovery role

uniqueRecoveryPath == true
= there is explicit evidence that it is unique

null
= uniqueness unknown
```

Consequences:
- Number rows show “恢复用途” unless uniqueness is explicit;
- Email rows do the same;
- Weaknesses only count explicitly unique paths;
- Number Detail distinguishes confirmed-unique, recovery-use/unknown-unique, and
  completely unknown recovery state;
- Impact Lens requires explicit uniqueness evidence.

This is a product correctness change in the reference presentation model only; it
does not change Canonical schema.

## 4. Five-item primary navigation is a product decision

The Android product decision is:

```text
现在 / 基础设施 / 变更 / 记录 / 我
```

`我` is intentionally promoted, not an accidental extra tab. It owns personal
preferences, privacy, sources/settings entry context, and the user's digital-life
workspace.

R19 therefore:
- keeps exactly five primary destinations on phone;
- keeps the same five destinations in the wide primary rail;
- treats `我` as a root destination with no hierarchical Up parent;
- may retain a top-right avatar as a shortcut, but never as a substitute;
- keeps Settings / Personalization / Sources semantically under the `我` primary
  selection on wide navigation;
- gives Medium / Expanded a dedicated `R19AdaptiveMeScreen` instead of stretching
  the compact phone feed.

Unit and runtime contracts pin the five-item IA so cleanup cannot silently demote it.

## 5. Card lifecycle parity

R18 card lifecycle is now consistently represented in:
- compact R9 detail;
- adaptive phone detail;
- Medium/Expanded detail;
- compact list;
- Medium/Expanded list;
- wide inspector.

Recorded fields:
```text
annual fee
annual-fee checkpoint
billing day
payment due day
installment summary
autopay summary
```

Missing values remain `未记录`.

Static/fake “change history” copy was removed from focused details. Real history
must come from an actual timeline/source before it returns.

## 6. Number lifecycle / keep-number parity

R19 keeps:
- primary / secondary / keep roles;
- plan cost;
- keep-alive due date;
- keep-alive cycle;
- recent action;
- recorded renewal/keep method;
- editable local display alias.

Adaptive lists and inspectors now use the same saved alias logic as compact R9.

Search also indexes:
- saved alias;
- keep role;
- annual-fee/billing facts;
- keep-alive lifecycle facts.

## 7. Globe composition

R15–R17 remains the GPU renderer.

R19 projected region callouts now read closer to the product reference:

```text
Region
N cards · M numbers   !attention
```

while still:
- following live camera projection;
- remaining collision-budgeted;
- using geographic anchor tethers only;
- never pretending those tethers are dependency edges.

This improves ownership/context without turning the globe into an inaccurate graph.

R19 also closes renderer parity across width classes:
- phone Now uses the R15/R16/R17 GPU world family;
- compact Infrastructure region distribution uses the same R15 GPU Earth;
- Medium / Expanded Now and Overview use `R19AdaptiveWorldScene`, which composes
  the same R15 GPU Earth with R16 camera-projected region annotations;
- wider layouts keep their own information hierarchy instead of stretching the phone screen.

The legacy `VNextGlobe` remains only as the governed CPU/failure fallback and in
non-reference legacy helpers; it is no longer the intended renderer for the active
R19 spatial surfaces.


## 8. Change Phone

R18 already separated:

```text
old active number
!= keep-number asset
!= migration target
```

R19 retains the invariant and the derived impact summary.

The compact summary is derived from projection state, not showcase constants:

```text
已记录关联
需要核对
阻断 / 待解决
```

Current/Transition/After remain distinct. After is a Plan Projection and never
claims the plan has actually executed.

## 9. Adaptive translation

R19 explicitly closes the source-level gap where lifecycle/aliases were richer in
the compact reference than in tablet/expanded layouts.

Tablet/expanded now preserve:
- card lifecycle in list/detail/inspector;
- number lifecycle in detail/inspector;
- user-visible number aliases;
- focused Impact Lens;
- list-detail behavior rather than stretching the phone page.

No pixel-copy of desktop is introduced.

## 10. Market research incorporated

R19 deliberately takes only adjacent-product interaction lessons:

### SIM / eSIM lifecycle
Current products such as KeepSim and SimAlive show that multi-number users need:
- renewal/keep-alive cycles;
- due dates;
- reminders;
- explicit completion;
- local-first storage;
- geographic context.

PDIG absorbs the lifecycle pattern but adds dependency/continuity semantics. It does
not become a carrier-management utility and does not auto-send SMS/calls.

### Card lifecycle
Current card-management tools expose:
- annual fee;
- statement/due dates;
- renewal reminders.

PDIG uses those maintenance facts, but its focus remains “what depends on this
card and what breaks if it changes,” not rewards optimization or personal finance.

### Recovery mapping
Current recovery/dependency-map tools reinforce:
- single points of failure;
- cycles;
- independent recovery routes;
- the danger of shared failure domains.

PDIG therefore does not treat “two recovery methods” as “two independent paths.”
The production Continuity engine remains the source of that judgment.

## 11. Production binding architecture

R19 adds:

`docs/ADR_UI_VNEXT_PRODUCTION_BINDING.md`

Decision:
- Preview remains synthetic reference UI;
- Production remains the existing lock-gated application;
- production VNext requires a real read-model adapter over `AppContainer`;
- Impact must use production `impactFor(nodeId)`;
- Change must use existing `createPlanForScenario / planDetail / completeAction / verifyAction`;
- lifecycle reference fields must not be shoved into `fields_json` or
  PresentationProfile as an Android-only side channel.

This is the required path to production cutover.

## 12. Contracts added or extended

Unit/source:
- `UiVNextLifecycleFixtureTest`
- `UiVNextImpactLensTest`
- `ChangeImpactSummaryTest`
- `SearchCatalogR19Test`
- `R11NavigationAndAliasesTest`
- `R16RegionLabelLayoutTest`

Instrumentation/source:
- `AndroidLightVisualSourceContractTest`
- `TabletAdaptiveContractTest`

Design:
- `ANDROID_VISUAL_CONTRACT.md`
- `ANDROID_REFERENCE_MAPPING.md`
- `spec/ui-vnext/ASSET_CONTINUITY_UX_CONTRACT.md`
- `docs/ADR_UI_VNEXT_PRODUCTION_BINDING.md`

The capture script and visible preview marker now require **R19**.

## 13. What is intentionally not claimed

R19 does not claim:
- a fresh APK assembled from this exact head;
- Kotlin/Compose compilation after the latest source edits;
- API36 runtime success;
- GPU first-frame success on the current head;
- phone/tablet pixel parity;
- production data binding;
- production lifecycle persistence;
- Android Reference Freeze;
- iOS/Harmony UI translation completion.

Those are different evidence classes.

## 14. Fresh acceptance set

The next exact-head local run should include at least:

```text
01 Now — R19 marker + GPU globe + projected region labels
02 Infrastructure — 8-category hub
03 Cards — lifecycle density
04 Card detail — lifecycle + Impact Lens
05 Numbers — primary/secondary/keep filtering
06 Number detail — lifecycle + recovery truth + Impact Lens
07 Search — saved alias + annual fee + keep-alive query
08 Weaknesses — only explicit unique-recovery evidence
09 Change Phone Current
10 Change Phone Transition
11 Change Phone After / Plan Projection
12 Me — utility route, not primary bottom tab
13 Tablet Cards list-detail
14 Tablet Number detail hierarchy
15 Tablet Change Phone
```

Review both content and geometry:
- no stale R17/R18 build;
- no fifth primary tab;
- no label collision;
- no `recoveryOnly → unique` regression;
- no lifecycle value invented for an unknown asset;
- no old fake history copy;
- no admin-console density on expanded layouts.

## 15. Freeze rule

```text
SOURCE_COMPLETE != RUNTIME_VERIFIED != HUMAN_ACCEPTED
```

R19 may become the Android Light Reference only after fresh exact-head build,
instrumentation, screenshot capture and human visual acceptance.

Until then:

```text
ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_CUTOVER = HOLD
```
