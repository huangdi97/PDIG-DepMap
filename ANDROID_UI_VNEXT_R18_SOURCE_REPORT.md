# ANDROID_UI_VNEXT_R18_SOURCE_REPORT

> 2026-10-09 · `feat/android-ui-vnext-translation`
>
> **Scope**: Android Light Reference source-design closure only. This report does **not**
> claim a fresh APK/runtime/pixel PASS. The next authoritative visual gate is a fresh
> local API36 phone/tablet capture from the exact remote head.

## 0. Truth status

```text
SOURCE_BASE_BEFORE_R18 = 1e023fd691e9cbe99b151b9a5dcf52ba8f3556b4
SOURCE_CHECKPOINT_BEFORE_THIS_REPORT = 5467a47990e64f402d1f31b78203ac044aaebfc1

ANDROID_R18_SOURCE_DESIGN = COMPLETE
CANONICAL_SCHEMA_CHANGE = NONE
DEPMAP_PAYLOAD_CHANGE = NONE
RUNTIME_BUILD = NOT_RUN
RUNTIME_TESTS = NOT_RUN
PHONE_PIXEL_CAPTURE = REQUIRED
TABLET_PIXEL_CAPTURE = REQUIRED
ANDROID_REFERENCE_FREEZE = HOLD
IOS_HARMONY_TRANSLATION = HOLD
```

The source work deliberately preserves the v2.3 truth boundary:
recorded lifecycle metadata is Presentation/UI reference data and never becomes
Provider truth, Canonical dependency truth, or an inferred safety conclusion.

## 1. Why R18 exists

The reference board and final product direction require PDIG assets to feel like
owned infrastructure, not sparse generic rows. Two missing layers were still obvious:

1. cards had visual identity and dependency context but too little **financial lifecycle**
   context (annual fee, billing day, due day, installment state);
2. numbers had communication/recovery identity but too little **line lifecycle**
   context (plan cost, keep-alive due date/cycle, last action and renewal method).

The globe had already reached the R15–R17 GPU/atmosphere path. R18 therefore does
not replace that renderer. It improves the information layer that floats on the
same spatial stage: geographic callouts now carry factual asset footprint instead
of behaving like tiny corner-style country chips.

External product research only validates the need, not PDIG truth:
- CardPointers explicitly supports annual-fee reminders and renewal-oriented card
  management: https://cardpointers.com/pro/
- KeepSim demonstrates a local-first number keep-alive model based on cycles, due
  dates, reminders and explicit user actions: https://apps.apple.com/us/app/keepsim-keep-sim-esim-active/id6773044270
- QuanCard reinforces local-first card ownership/collection, while PDIG continues
  beyond collection into dependency and change continuity:
  https://quancard.app/en/

PDIG intentionally **does not** copy rewards optimization, provider operations or
automatic telecom actions.

## 2. Card lifecycle — implemented

Presentation model:
- `UiVNextCardLifecycle`
- annual fee
- annual-fee checkpoint
- billing day
- payment due day
- installment summary
- recorded autopay summary

Consumer UI:
- card rows surface compact annual-fee/billing context;
- card detail adds a dedicated **用卡周期** surface;
- Overview includes lifecycle facts;
- the former sparse **账单** tab is now **账单与分期** and exposes recorded facts;
- missing data renders as `未记录`;
- transaction amount/balance/minimum-payment values are never inferred.

Stable evidence tags:
- `pdig.r18.card.lifecycle`
- `pdig.r18.card-row.lifecycle`

## 3. Number lifecycle / keep-number — implemented

Presentation model:
- `UiVNextNumberLifecycle`
- billing mode
- plan cost
- keep-alive due
- keep-alive cycle
- last keep-alive/action
- recorded renewal method

Consumer UI:
- number rows show plan/keep-alive context;
- Number Detail adds **号码生命周期**;
- role is visible together with SIM type;
- `keep` is now a first-class presentation role;
- Numbers gets a **保号** filter;
- keep-number objects carry a visible **保号** badge;
- home attention/upcoming fixture now includes a keep-number event;
- copy explicitly says recorded dates are not live carrier state.

Stable evidence tags:
- `pdig.r18.number.lifecycle`
- `pdig.r18.number-row.lifecycle`

## 4. Globe / region fusion — implemented at source level

R15/R17 GPU Earth remains the renderer of record. R18 changes the projected
annotation layer only:

- labels remain camera-projected from the same globe geometry;
- no fixed-corner country labels;
- two-line glass callout = region identity + recorded asset footprint;
- callouts have a bounded 48dp hit target;
- tethers use a low-alpha line plus an anchor glow;
- overlapping regions still cluster and open an explicit chooser;
- callout width/height remains collision-budgeted.

This is still spatial context, not a fake dependency graph: tether lines connect
label to geographic anchor only.

## 5. Existing product corrections verified in source

The current branch already contains and R18 preserves:

- header arrow = **返回上一级**, while Android system Back remains chronological;
- number custom naming persists locally and blank alias falls back to the number;
- privacy masking defaults from workspace preferences and is independent of card art;
- card-art replacement is a minor inline utility, not a full-page product;
- GPU Earth includes bundled albedo/night/cloud textures, atmospheric limb, lighting,
  ocean highlight and CPU fallback;
- region labels are driven by the live camera rather than hard-coded corners.

## 6. Tests added/extended

Source contracts now protect:

- lifecycle fixture values and unknown-state behavior;
- keep-number semantic role;
- card lifecycle surface presence;
- number lifecycle surface presence;
- existing R16 geographic projection/collision budget remains intact.

Files:
- `android/app/src/test/kotlin/com/pdig/uivnext/demo/UiVNextLifecycleFixtureTest.kt`
- `android/app/src/androidTest/kotlin/com/pdig/uivnext/evidence/AndroidLightVisualSourceContractTest.kt`

## 7. Required fresh runtime closure

Do **not** reuse any pre-R18 screenshot PASS.

Run from the exact remote branch head:

```text
git fetch origin
git checkout feat/android-ui-vnext-translation
git reset --hard origin/feat/android-ui-vnext-translation

cd core
npm run check

cd ../android
gradlew.bat test
gradlew.bat connectedProductionDebugAndroidTest
```

Then execute the repository's current Android pixel-capture flow. The first Now
screen must visibly identify **R18**, and the capture script now rejects older R17
builds.

Human review set must include at minimum:

```text
01 Now / globe
02 Infrastructure overview
03 Cards
04 Card detail — lifecycle visible
05 Numbers
06 Number detail — lifecycle visible
07 Numbers — keep filter
08 Change phone current
09 Change phone transition
10 Change phone after
```

For the globe, review the actual frame for:
- round/non-faceted silhouette;
- atmospheric integration rather than a pasted rectangle;
- readable day/night texture;
- projected region callouts following drag/zoom;
- no label collisions covering critical land mass;
- region callout + world + asset rail reading as one composition.

## 8. Freeze rule

R18 source work can only advance to `ANDROID_REFERENCE_FREEZE = PASS` after the
fresh phone/tablet runtime set is produced from the exact remote head and receives
human visual acceptance. A source commit, unit test or old screenshot cannot
substitute for that gate.


## 9. R18 semantic correction — keep-number != migration target

A final source audit found a real modeling conflict: the original change-phone fixture
used `num-cn-3` as the target new number, while R18 had correctly promoted that same
object to the long-lived `keep` role. One object cannot simultaneously represent the
user's retained keep-alive number and the transitional replacement target without
making the continuity UI ambiguous.

R18 therefore separates them:

- `num-cn-3` = 保号副号, role = `keep`;
- `num-cn-4` = 新号（迁移中）, role = `secondary`, dedicated change target;
- both compact R9 and adaptive Change Phone screens use `num-cn-4` as the target;
- projection copy is updated to the new target number;
- unit tests assert the two objects remain distinct.

The change screen also no longer needs showcase-only hard-coded impact counts.
`changeImpactSummary(projection)` derives:

```text
已记录关联
需要核对
阻断 / 待解决
```

from the actual projection migrations/stages. This keeps the visual density of the
reference board while preserving PDIG's core rule that presentation must not invent
dependency or risk truth.

Evidence tags/tests:
- `pdig.r18.change.impact-summary`
- `ChangeImpactSummaryTest`
- Android Light visual contract now requires the summary on compact Change Phone.
