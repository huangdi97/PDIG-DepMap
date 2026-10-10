# ANDROID_UI_VNEXT_R41_SOURCE_REPORT

> 2026-10-10 · `feat/android-ui-vnext-translation`
>
> R41 records the current Android/Product/Canonical source closure after R34–R40.
> It is a **source/design report**, not a runtime acceptance certificate.

## 0. Current source truth

```text
ANDROID_REFERENCE_UI_SOURCE = R34+
ARCHITECTURE_CONTROL = R35
PRODUCTION_CUTOVER_CONTROL = R36
IDENTITY_ANCHOR_PROFILE = R37
IDENTITY_IDENTIFIER / MANUAL CREATE = R38
REGION_FACT / PRODUCTION WORLD = R39
MAINTENANCE_PROFILE CANONICAL READ/WRITE = R40
MAINTENANCE OCCURRENCE / PRODUCTION CONTROL SURFACE = R41

FIVE_PRIMARY_IA = FROZEN
PRODUCTION_VNEXT_REALITY_BINDING = SOURCE_IMPLEMENTED
PRODUCTION_VNEXT_RELEASE_DEFAULT = LEGACY
ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_RELEASE_CUTOVER = HOLD
```

No pre-R41 screenshot, CI result or runtime pack proves the current head.

## 1. Five primary destinations

Permanent primary IA:

```text
现在 / 基础设施 / 变更 / 记录 / 我
```

`我` remains a first-class root. It must not be demoted to avatar-only,
overflow, Settings, or secondary navigation.

Phone bottom navigation and Medium/Expanded primary rail keep the same five roots.

## 2. Region / world context

R39 governs RegionFact rather than inferring geography from:
- currency;
- issuer/provider name;
- phone number prefix;
- locale/IP;
- current device location.

Production world/region surfaces use governed RegionFact only.

The GPU Earth remains the spatial product signature; geographic callouts are
camera-projected context, never fake dependency edges.

## 3. Governed identity

Production Number / Email require governed `identity_anchor_profile`.

```text
bare subtype != authority
recovery use != unique recovery path
identity_anchor != phone number by default
```

Phone/email identifier values remain separately governed and privacy-maskable.

## 4. Asset maintenance Reality

R40 establishes governed `Node.fields.maintenance_profile` inside the existing
fields envelope.

### Card facts

```text
annual fee amount + currency
billing day
payment due day
autopay mode
```

### Number facts

```text
billing mode
plan cost + currency
renewal method
```

### Schedules

```text
card annual-fee checkpoint
card billing checkpoint
card payment-due checkpoint
number keep-alive
number plan renewal
fact freshness review
custom maintenance
```

Cross-platform fail-closed decoding/conformance exists. Missing data remains unknown.

## 5. Maintenance write authority

R40 write authority is source-implemented:

```text
Compose
→ VNextMaintenanceActionGateway
→ AppContainer
→ GraphRepository
→ Canonical writer
→ encrypted Reality
→ graphRevision bump
```

Consumer gateway supports:
- atomic card amount + currency;
- card billing/payment/autopay;
- annual-fee checkpoint;
- atomic phone cost + currency;
- phone billing/renewal method;
- keep-alive interval;
- plan-renewal monthly checkpoint.

UI never writes `fields_json` directly.

## 6. R41 maintenance control surface

Production Card Detail now contains a governed maintenance editor.

Production Number Detail now contains a governed plan / keep-alive editor.

The editors:
- show explicit Reality boundary copy;
- reject incomplete amount/currency pairs;
- perform convenience validation;
- delegate final validation to Core/Repository;
- request a fresh Production snapshot only after authoritative success;
- do not create optimistic domain state.

Stable test tags exist for controls and fields.

## 7. Explicit completion boundary

Permanent rule:

```text
time passed != completed
notification delivered != completed
app opened != completed
```

R41 exposes explicit keep-alive completion authority. Only a user action may write
`lastCompletedAt`.

There is no timer/background process that marks maintenance complete.

## 8. Derived occurrence → Now

R41 derives next actionable `MaintenanceOccurrence` from governed schedules.

States:

```text
UPCOMING
DUE
OVERDUE
NEEDS_REVIEW
```

Occurrences are projections, not durable Reality.

They join Production Timeline/Now with normal buckets:

```text
attention
overdue
today
7d
30d
90d
later
```

Maintenance does not use a private `upcoming` bucket and does not bypass the
existing Timeline ordering.

Priority is interpreted *inside* bucket order, so a future maintenance reminder
cannot outrank an attention/overdue item merely because it is maintenance.

## 9. Production search

Production search may match governed maintenance Reality:
- 年费;
- 账单 / 账单日;
- 还款;
- 自动还款;
- 套餐 / 资费;
- 续费;
- 保号;
- maintenance values/dates.

Matching lifecycle Reality does not promote reference/Provider information.

## 10. What remains deliberately outside v1

```text
installment summary as Canonical truth
transaction ledger
statement balance
minimum payment / interest
rewards optimization
automatic carrier action
automatic maintenance completion
provider policy silently becoming Personal Reality
```

These are not hidden TODOs.

## 11. Test/evidence source

Source now includes contracts for:
- maintenance decoder/writer;
- cross-platform maintenance fixtures;
- occurrence derivation;
- occurrence → Production Timeline;
- maintenance search;
- production editor authority seam;
- five-primary navigation;
- governed identity/region truth boundaries.

The Harmony embedded fixture bundle is regenerated from the current 161-case
manifest after the R40 fixture expansion.

## 12. CI fixes made during R41 closure

A real CI run exposed two concrete repository defects:

1. Kotlin maintenance writer used `JsonWriter` without importing it.
2. Harmony embedded `FixtureBundle.ets` still contained the old 153-fixture
   manifest after maintenance conformance expanded the manifest to 161.

Both are source-fixed. A later exact-head CI run is still required before PASS can
be claimed.

## 13. Runtime acceptance still required

```text
exact-head compile/test green
cross-platform conformance green
Production secure rehearsal
API36 phone runtime
tablet runtime
GPU first-frame/runtime-state evidence
maintenance write → relaunch → persisted readback
keep-alive explicit completion → next occurrence recompute
human pixel acceptance
Android Reference Freeze
two-key release cutover decision
```

Until these exist:

```text
SOURCE_DESIGN = CLOSED_THROUGH_R41
SOURCE_IMPLEMENTATION = ADVANCED_THROUGH_R41
CI_PASS = NOT_YET_CLAIMED_FOR_LATEST_HEAD
ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_RELEASE_CUTOVER = HOLD
```
