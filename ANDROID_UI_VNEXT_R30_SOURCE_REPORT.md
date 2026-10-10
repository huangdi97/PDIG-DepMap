# ANDROID_UI_VNEXT_R30_SOURCE_REPORT

> 2026-10-10 · `feat/android-ui-vnext-translation`
>
> R30 continues the production-source closure after R29. It removes the all-or-nothing
> Production Weaknesses HOLD and replaces it with an **authority-bounded continuity
> finding projection**.
>
> It also includes the R29 CI compile repair for Production Compose `weight` usage.
>
> This report does **not** claim fresh exact-head runtime / pixel / human acceptance.

## 0. Current truth

```text
ANDROID_UI_VNEXT_SOURCE = R30

REFERENCE_PRODUCT_UX_DESIGN = COMPLETE
FIVE_PRIMARY_IA = FROZEN
ANDROID_LIGHT_DIRECTION = DESIGN_FROZEN

PRODUCTION_READ_MODEL = SOURCE_IMPLEMENTED
PRODUCTION_CONSUMER_PROJECTION = SOURCE_IMPLEMENTED
PRODUCTION_VNEXT_SHELL = SOURCE_IMPLEMENTED
PRODUCTION_CHANGE_ACTIONS = SOURCE_IMPLEMENTED
PRODUCTION_HUMAN_REVIEW = SOURCE_IMPLEMENTED
PRODUCTION_MANUAL_ESTABLISH = SOURCE_IMPLEMENTED
PRODUCTION_IMPORT_HOST_HANDOFF = SOURCE_IMPLEMENTED
PRODUCTION_REALITY_SEARCH = SOURCE_IMPLEMENTED

PRODUCTION_FINDINGS_PROJECTION = SOURCE_IMPLEMENTED_PARTIAL_AUTHORITY
PRODUCTION_WEAKNESSES_SCREEN = SOURCE_BOUND_WITH_COVERAGE_DISCLOSURE

CANONICAL_SCHEMA_CHANGE_FOR_R30 = NONE
DEPMAP_PAYLOAD_CHANGE_FOR_R30 = NONE

MAINACTIVITY_PRODUCTION_CUTOVER = HOLD
PHONE_EMAIL_SUBTYPE_CANONICAL = HOLD
LIFECYCLE_CANONICAL = HOLD
MANUAL_RELATION_SCHEMA_V4 = HOLD

FRESH_R30_BUILD = PENDING
FRESH_R30_UNIT_TESTS = PENDING
FRESH_R30_INSTRUMENTATION = PENDING
FRESH_R30_PHONE_PIXELS = PENDING
FRESH_R30_TABLET_PIXELS = PENDING
FRESH_R30_HUMAN_ACCEPTANCE = PENDING

ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_CUTOVER = HOLD
```

## 1. Why R30 exists

R27 completed the **seven-class consumer Finding grammar** in Reference Preview.

R29 production still had:

```text
Weaknesses
→ full-screen HOLD
```

because current Android production authority does not yet expose all seven classes.

The wrong fixes would be:

```text
A. show no production findings at all
B. fake all seven from UI heuristics
C. count edges and call that resilience
D. import the synthetic R27 finding fixture
```

R30 instead chooses:

```text
show only what current production authority can prove
+
show exactly which finding classes remain unsupported
```

## 2. Production Finding report

New:

`ProductionContinuityFindings.kt`

Projection:

```text
AppContainer
├─ confirmed ImpactGraph
├─ pending Proposals
└─ ChangePlan detail / verification
        ↓
VNextProductionFindingReport
        ↓
Production Weaknesses
```

Every result keeps:
- title / what;
- why;
- confirmed basis;
- unknowns;
- recommended next action;
- evidence refs;
- truth class.

There is still no health score.

## 3. Currently authoritative Finding classes

### SINGLE_POINT_OF_FAILURE

Input:

```text
confirmed active recovery Dependencies
```

Rule:

```text
target has exactly one confirmed active recovery incoming edge
→ single confirmed source finding
```

Important copy boundary:

> only one **confirmed** source is recorded

not:

> no other recovery method exists

Unrecorded paths stay unknown.

### RECOVERY_CYCLE

Input:

```text
confirmed active recovery Dependencies
→ shared core detectRecoveryCycles()
```

R30 does not implement a second cycle detector in UI.

A confirmed cycle carries the exact dependency IDs supporting the cycle.

### UNCONFIRMED_FALLBACK

Input:

```text
pending Proposal(capability = recovery)
```

Truth:

```text
PENDING_REVIEW
```

A high confidence score never upgrades the candidate into Reality.

### PENDING_VERIFICATION

Input:

```text
ChangePlan action
done = true
verification = pending / evidence_suggested
```

Permanent rule:

```text
done != verified
```

This is a real continuity problem because the result of the recorded action is still
unknown.

## 4. Explicitly unsupported Finding classes

R30 does not pretend the current Android production seam has complete authority for:

```text
SHARED_FAILURE_DOMAIN
STALE_RECOVERY_INFORMATION
UNKNOWN_CRITICAL_PATH
```

Reasons:

### Shared Failure Domain
Current production Finding seam does not yet expose a reusable failure-domain result
that is safe to consume as a full Finding.

It is forbidden to substitute:

```text
same source node count
or
factor count
or
edge count
```

for failure-domain independence.

### Stale Recovery Information
A governed freshness policy / threshold is not yet exposed as a reusable Finding
authority.

Blank or old-looking timestamps are not enough for UI to invent “stale”.

### Unknown Critical Path
The current Android read seam does not expose the complete critical-path input
required by the seven-class v0.3 Finding generator.

Unknown remains explicit.

## 5. Production Weaknesses UI

The previous full-screen HOLD is removed.

Production Weaknesses now shows:
- authoritative findings currently available;
- finding truth state;
- basis;
- unknown boundary;
- recommended next action;
- evidence-ref count;
- supported Finding classes;
- unsupported Finding classes.

If the supported classes produce no results, the empty state says:

> current supported finding classes produced no result

and explicitly does **not** say:

> infrastructure is safe

## 6. Reference / Production isolation remains intact

R30 production code does not import:
- `UiVNextDemoFixture`;
- `UI_CONTINUITY_REFERENCE_FINDINGS`;
- R27 synthetic finding fixture;
- Preview R9/R2x screen implementation.

Reference Preview can still visually exercise all seven classes.

Production can only show current authoritative coverage.

## 7. Runtime source boundary

`VNextRuntimeDataSource` now exposes:

```text
productionFindings()
```

Reference mode returns null.

Production mode delegates to the authoritative read-model source.

This keeps the same anti-demo-leak rule as:
- snapshot;
- inventory;
- impact;
- plan;
- records.

## 8. Test contracts

Added/extended:

- `ProductionContinuityFindingsTest`
  - single confirmed recovery source;
  - confirmed recovery cycle;
  - pending recovery Proposal stays Pending Review;
  - done-but-pending-verification becomes Finding;
  - verified action does not remain pending.

- `VNextRuntimeDataSourceTest`
  - Reference cannot return Production Findings;
  - Production source owns the report.

- `ProductionVNextShellContractTest`
  - Production Weaknesses renders authoritative findings;
  - unsupported coverage remains visible.

## 9. CI repair included

R29 exact-head CI exposed a real Kotlin compile failure in five Production screens:

```text
Cannot access RowColumnParentData?.weight
```

Cause:
- explicit import of Compose internal/scoped `weight` symbol under the repository's
  Compose/Kotlin version.

Repair:
- remove the direct import;
- use the normal RowScope / ColumnScope scoped `Modifier.weight(...)` calls.

Files repaired:
- ProductionChangePlanScreen;
- ProductionEstablishScreens;
- ProductionInventoryScreens;
- ProductionReviewScreen;
- ProductionVNextShell.

The separate Android-core Maven resolution failure for
`error_prone_annotations:2.3.1` was dependency-resolution/network-class and is not
treated as a source-design PASS or source regression until a fresh run resolves it.

## 10. Remaining product/source gates

The remaining HOLD list is narrower:

```text
Identity subtype Canonical
→ production Number / Email

Lifecycle Canonical
→ production card/number maintenance facts

Manual Dependency mutation authority / schema gate
→ executable manual relationship

FailureDomain + freshness + critical-path Finding authority
→ complete seven-class production Weaknesses

Launcher/security cutover
→ production VNext becomes active app shell
```

These are not reasons to fake UI authority.

## 11. Stop line

```text
R30_PRODUCTION_FINDING_SCREEN = SOURCE_BOUND_PARTIAL_AUTHORITY
R30_FINDING_COVERAGE_DISCLOSURE = COMPLETE
R30_REFERENCE_FIXTURE_LEAK = FORBIDDEN

SOURCE_COMPLETE != BUILD_PASS != RUNTIME_VERIFIED != HUMAN_ACCEPTED

ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_CUTOVER = HOLD
```
