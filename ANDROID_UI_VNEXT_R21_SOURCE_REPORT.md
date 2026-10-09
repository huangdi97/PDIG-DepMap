# ANDROID_UI_VNEXT_R21_SOURCE_REPORT

> 2026-10-09 · `feat/android-ui-vnext-translation`
>
> R21 closes two remaining product-architecture gaps on top of R20:
>
> 1. **Records** is no longer a duplicate Now/attention page. It becomes an
>    evidence/verification trace.
> 2. **Replace Payment Card** is already an active production scenario, so Android
>    now has a focused card-change continuity reference instead of stopping at
>    “go back to Cards”.
>
> This report records source/design reality only. It does not claim fresh runtime
> or Human Pixel Acceptance.

## 0. Truth status

```text
ANDROID_UI_VNEXT_SOURCE = R21

FIVE_PRIMARY_IA = PRESERVED
NOW_REGION_LENS = IMPLEMENTED_SOURCE
CORE_OBJECT_DETAILS = IMPLEMENTED_SOURCE
OBJECT_IMPACT_LENS = IMPLEMENTED_SOURCE
CHANGE_CENTER = IMPLEMENTED_SOURCE
REPLACE_PHONE_REFERENCE = IMPLEMENTED_SOURCE
REPLACE_PAYMENT_CARD_REFERENCE = IMPLEMENTED_SOURCE
RECORDS_EVIDENCE_TRACE = IMPLEMENTED_SOURCE

PRODUCTION_VNEXT_READ_MODEL = SOURCE_IMPLEMENTED
PRODUCTION_CHANGE_GATEWAY = SOURCE_IMPLEMENTED

CANONICAL_SCHEMA_CHANGE = NONE
DEPMAP_PAYLOAD_CHANGE = NONE

FRESH_R21_BUILD = NOT_RUN
FRESH_R21_UNIT_TESTS = NOT_RUN
FRESH_R21_INSTRUMENTATION = NOT_RUN
FRESH_R21_PHONE_PIXELS = NOT_RUN
FRESH_R21_TABLET_PIXELS = NOT_RUN
FRESH_R21_HUMAN_ACCEPTANCE = NOT_RUN

ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_CUTOVER = HOLD
```

## 1. R21 does not change the primary IA

The five primary destinations remain:

```text
现在
基础设施
变更
记录
我
```

R21 does not add:
- a sixth “Lens” tab;
- a separate “Recovery” tab;
- a separate “Card Change” primary tab.

Focused change routes remain children of `变更`.

## 2. Records boundary repaired

Before R21, Records still contained:
- active changes;
- migration progress;
- Attention;
- Upcoming.

That made it partly a second Now page.

R21 changes the active route to a new evidence-first renderer:

```text
Records
→ 已记录完成 / 已验证 / 待验证
→ recorded change trace
→ verification boundary
→ active-plan context link
→ evidence/source boundary
```

Attention and Upcoming remain on Now.

### Reference projection

R21 derives its reference trace from the existing synthetic ChangeStage state.

Current fixture:

```text
stage 1 impact analysis       completed
stage 2 establish new number completed
stage 3 verify new number     verifying
stage 4/5                     not_started
stage 6                       blocked
```

Records projection:

```text
stage 1 → 已记录完成
stage 2 → 已记录完成
stage 3 → 待验证
stage 4/5/6 → not historical events
```

Therefore:

```text
recorded complete = 2
verified = 0
pending verification = 1
```

The key rule is explicit:

```text
done != verified
```

No fake timestamp or evidence source is generated.

Files:
- `demo/UiVNextRecordTrace.kt`
- `screens/R21RecordsScreen.kt`
- `RecordsProjectionR21Test.kt`

## 3. Why Replace Payment Card is allowed

The production ScenarioRegistry already contains active:

```text
replace_payment_card
expiring_payment_card
close_payment_instrument
replace_phone_number
```

`replace_payment_card` has the production action grammar:

```text
检查依赖
迁移必要支付关系
验证关键支付路径
```

Therefore a card-change continuity surface is not ghost capability.

What would still be ghost capability:
- executing the plan in Preview;
- marking verification locally;
- inventing a replacement card recommendation;
- writing synthetic lifecycle or plan state into Canonical.

## 4. Card Detail → Change Card

Card Detail Impact Lens now exposes:

> **分析更换此卡的影响**

This is deliberately separate from:

> 更换卡面图片

Product distinction:

```text
card appearance
= PresentationProfile
= local presentation only

replace payment card
= Change scenario
= dependency / continuity / verification semantics
```

These flows must never merge.

## 5. R21 Change Card

Route:

```text
/change/card
```

Hierarchy:

```text
Change Card → Up → Change
```

System Back still returns to the actual previous screen.

### Spatial model

Compact:

```text
OLD CARD
   ↓
recorded payment relations
   ↓
NEW CARD / not selected
```

Medium / Expanded:

```text
OLD CARD → recorded payment relations → NEW CARD
```

### Projection states

Current:

```text
review dependencies   not_started
migrate relations     not_started
verify payment path   not_started
```

Transition without replacement:

```text
review dependencies   completed
migrate relations     blocked
verify payment path   blocked
```

Transition with replacement:

```text
review dependencies   completed
migrate relations     verifying / in progress
verify payment path   blocked
```

After:

```text
all three stages = plan
```

After is Plan Projection only.

## 6. Replacement-card selection

The reference UI lists other recorded active cards.

This list means only:

> objects the user may choose as a plan target in Preview.

It does **not** mean:
- recommended;
- compatible;
- independent;
- safer;
- already migrated.

The user explicitly selects or clears the target.

Production may later add governed compatibility/provider knowledge, but R21 does not
infer it from currency, region, issuer or card network.

## 7. Payment verification boundary

Production template text already requires:

> 用一次真实支付/账单证据确认关键路径可用。

Therefore:

```text
migration action done
!=
payment path verified
```

R21 never exposes a local “mark verified” shortcut.

Production binding must go through the existing authoritative gateway:

```text
createPlanForScenario
completeAction
verifyAction
planDetail
```

## 8. Search boundary corrected

Search can discover:

```text
更换银行卡
换卡
支付迁移
```

But a search query has no concrete card target.

Therefore R21 search result routes to **Cards**, not directly to `CHANGE_CARD`.

The user must:

```text
Search
→ Cards
→ concrete Card
→ Impact Lens
→ Change Card
```

This avoids an empty/ambiguous change plan.

## 9. Adaptive parity

R21 uses one Records renderer across:
- Compact;
- Medium;
- Expanded.

Only max-width/spacing changes. The truth grammar does not fork.

Change Card adapts geometry:
- Compact vertical continuity;
- Medium/Expanded horizontal OLD→relations→NEW.

Five-primary navigation remains unchanged.

## 10. Tests / evidence source

Added:
- `CardChangeR21Test`
- `RecordsProjectionR21Test`
- SourceComplete interaction coverage for Change Card and Records
- Search target-selection contract
- screenshot evidence route for Change Center / Change Card / Records / Me

The current pixel script also stops checking a stale literal “R19” badge and instead
uses the visible exact Git SHA as the authoritative build identity.

## 11. Production binding status

Already source-implemented:
- production inventory snapshot;
- canonical card `issuer` / `last4`;
- conservative object surface classification;
- production Impact projection;
- production Plan projection;
- authoritative change-action gateway.

Still intentionally gated:
- production VNext screen injection;
- lifecycle Canonical persistence;
- identity-anchor subtype Canonical migration;
- production Records read-model normalization;
- production launcher cutover.

## 12. Future Lens architecture is now design-complete

R21 also closes the remaining architecture/design gaps without exposing unsupported
screens.

### Identity Context

`spec/proposals/identity-context-v1.md` freezes:
- context as governed grouping/query primitive, not NodeKind/Dependency;
- first-class reviewable membership recommendation;
- Region × Identity as query intersection;
- migration/conformance/privacy rules;
- no inference from region/provider/search similarity.

### Recovery Incident Mode

`spec/proposals/recovery-incident-mode-v1.md` freezes:
- explicit incident authority;
- available/unavailable/degraded/unknown factor state;
- surviving roots and viable/blocked paths;
- FailureDomain-aware independence;
- RecoveryCycle and ProviderPolicy composition;
- no score and no path-count shortcut;
- explicit recovery verification/resolution;
- secret-handling boundary.

### Visibility gate

`VNextLensAvailability` currently enforces:

```text
Region      visible
Dependency  visible
Change      visible
Identity    hidden until Canonical
Recovery    hidden until solver
```

Tests also pin that Lens growth cannot create a sixth/seventh primary tab or
searchable ghost capability.

## 13. Remaining Android closure

Remaining work after current-head compile/tests:

```text
API36 phone runtime
API36 tablet runtime
GPU first-frame / texture state
five-primary navigation pixels
R21 Card Change Current
R21 Card Change Transition blocked
R21 Card Change After plan projection
R21 Records evidence trace
core object details
Me workspace
human pixel review
```

No pre-R21 screenshot can certify this head.

## 14. Stop line

```text
PRODUCT_DESIGN_GAPS_FOUND_IN_R21 = CLOSED_AT_SOURCE
CURRENT_HEAD_COMPILE_TEST = MUST_BE_GREEN
RUNTIME_PIXEL_EVIDENCE = REQUIRED
HUMAN_VISUAL_ACCEPTANCE = REQUIRED

ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_CUTOVER = HOLD
```
