# ANDROID_UI_VNEXT_R34_SOURCE_REPORT

> 2026-10-10 · `feat/android-ui-vnext-translation`
>
> R34 is a **truth-boundary / production-source closure** after R33.
> It does not add another visual redesign round. Instead it removes stale source
> HOLDs, completes the missing Region/Globe semantic design, and makes the
> Production VNext capability matrix match the actual authority currently present.
>
> This report does **not** claim fresh phone/tablet pixel or human acceptance.

---

## 0. Current truth

```text
ANDROID_UI_VNEXT_SOURCE = R34

FIVE_PRIMARY_IA = FROZEN
REFERENCE_PRODUCT_UX_DESIGN = CLOSED
ANDROID_LIGHT_DIRECTION = DESIGN_FROZEN

PRODUCTION_READ_MODEL = SOURCE_IMPLEMENTED
PRODUCTION_CONSUMER_PROJECTION = SOURCE_IMPLEMENTED
PRODUCTION_VNEXT_SHELL = SOURCE_IMPLEMENTED
PRODUCTION_CHANGE_ACTION_GATEWAY = SOURCE_IMPLEMENTED
PRODUCTION_HUMAN_REVIEW = SOURCE_IMPLEMENTED
PRODUCTION_MANUAL_ESTABLISH = SOURCE_IMPLEMENTED
PRODUCTION_MANUAL_RELATIONSHIP = SOURCE_IMPLEMENTED_CURRENT_V3_SET
PRODUCTION_IMPORT_HANDOFF = SOURCE_IMPLEMENTED
PRODUCTION_REALITY_SEARCH = SOURCE_IMPLEMENTED
PRODUCTION_FINDINGS = SOURCE_IMPLEMENTED_PARTIAL_AUTHORITY
PRODUCTION_SECURE_REHEARSAL = SOURCE_IMPLEMENTED

REGION_FACT_DESIGN = COMPLETE
IDENTITY_SUBTYPE_DESIGN = COMPLETE
LIFECYCLE_DESIGN = COMPLETE
IDENTITY_CONTEXT_DESIGN = COMPLETE
RECOVERY_INCIDENT_DESIGN = COMPLETE
CONTINUITY_MODES_DESIGN = COMPLETE

CANONICAL_REGION_FACT = NOT_IMPLEMENTED
CANONICAL_IDENTITY_SUBTYPE = NOT_IMPLEMENTED
CANONICAL_LIFECYCLE = NOT_IMPLEMENTED

ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_RELEASE_CUTOVER = HOLD
```

---

## 1. R34 fixes a real CI/source regression

Exact-head CI exposed:

```text
ProductionVNextShell.kt
Unresolved reference: snapshot
```

Cause:
- Production Infrastructure began using the authoritative object snapshot when
  rendering generic identity names;
- the composable signature still received only `app + inventory`.

R34 repairs the dependency explicitly:

```text
ProductionContent
→ ProductionInfrastructure(app, snapshot, inventory, findings, ...)
```

The screen no longer reaches for a variable outside its scope.

This is a source compile repair, not a semantic workaround.

---

## 2. Production Weaknesses is no longer mislabeled as “waiting”

R30 already implemented an authority-bounded Production Finding report.

However the Production Infrastructure category grid still displayed:

```text
薄弱点
等待 Finding projection
```

That was stale and contradicted the actual source.

R34 now binds the category summary to:

```text
productionFindings()
→ current authoritative finding count
→ supported finding-class count
```

If the authority is unavailable:

```text
权威 Finding 暂不可用
```

If available:

```text
N findings
4 类权威输入
```

It still does not pretend the unsupported classes are implemented.

Current Production Finding classes remain:

```text
SINGLE_POINT_OF_FAILURE
RECOVERY_CYCLE
UNCONFIRMED_FALLBACK
PENDING_VERIFICATION
```

Explicitly unsupported until authoritative inputs exist:

```text
SHARED_FAILURE_DOMAIN
STALE_RECOVERY_INFORMATION
UNKNOWN_CRITICAL_PATH
```

Instrumentation now guards that the stale “等待 Finding projection” copy cannot
return while the authority-backed report exists.

---

## 3. Region / Globe design gap is now closed

Before R34 the visual Region Lens was mature, but Production semantics still had an
ambiguous sentence:

> governed region semantics required

without a complete Canonical proposal defining what “region” means.

New:

`spec/proposals/region-facts-v1.md`

The proposal freezes the semantic distinction between:

```text
ISSUANCE_JURISDICTION
NUMBERING_TERRITORY
PROVIDER_JURISDICTION
SERVICE_MARKET
PHYSICAL_LOCATION
USER_CONFIRMED_CONTEXT
```

Permanent rules:

```text
one vague Node.region string != sufficient Canonical semantics

territory display name != stored truth

territory centroid != physical object location

currency/provider/name/locale/IP != confirmed RegionFact
```

### Standard identifiers

The proposal recommends:
- ISO 3166-1 alpha-2 for territory-level facts;
- ISO 3166-2 when subdivision granularity is actually required;
- Unicode CLDR for localized display names.

The codes/localization infrastructure never determines which RegionFact belongs to
the user's object.

### Production Globe

Until RegionFact Canonical exists:

```text
GPU Earth = allowed
confirmed asset geography = none
region labels = none
asset pins = none
arcs = none
```

That is exactly the current Production World Context behavior.

Preview may continue using synthetic RegionPresentation to exercise the intended
consumer experience.

---

## 4. Region and identity subtype become explicit capability gates

`VNextCapabilityMatrix` now includes:

```text
REGION_FACT
IDENTITY_SUBTYPE
```

### REGION_FACT

```text
Reference visibility = YES
Production authority = REQUIRES_CANONICAL
```

Meaning:
- the Region Lens / Globe is a valid product/reference experience;
- Production asset geography remains truth-empty until shared Canonical RegionFact
  semantics are implemented.

### IDENTITY_SUBTYPE

```text
Reference visibility = HIDDEN_UNTIL_CANONICAL
Production authority = REQUIRES_CANONICAL
```

Meaning:
- a production `identity_anchor` remains generic;
- no name/prefix/provider/edge heuristic may promote it into Number or Email.

Unit tests now pin these gates.

---

## 5. Production object normalization remains conservative

R34 retains the production read seam that now carries Canonical fields already
present in Reality:

```text
payment_instrument
  name
  issuer?
  last4?
```

No parsing of free-form `fields_json` is required for those fields.

Production surface classification remains:

```text
payment_instrument → PAYMENT_ASSET
account            → ACCOUNT
service            → SERVICE
device             → DEVICE
membership         → MEMBERSHIP
identity_anchor    → IDENTITY_ANCHOR_GENERIC
other              → CUSTOM_GENERIC
```

Permanent boundary:

```text
identity_anchor
!= phone number
!= email address
```

until the Identity Anchor Subtype proposal becomes Canonical.

---

## 6. Production consumer projection

The production consumer projection now has a safe, explicit shape for:

```text
payment assets
generic identity anchors
accounts
services
devices
memberships
custom objects
pending review
active source coverage
confirmed relationship counts
```

It does not coerce the richer Preview fixture model into Production.

A generic identity is rendered as generic identity.

A missing field is rendered as missing/unknown.

---

## 7. Change mutation authority remains domain-owned

The write-side seam remains:

`ProductionVNextActions.kt`

```text
createPlan
completeAction
verifyAction
→ AppContainer
→ authoritative mutation
→ re-read planDetail
→ render authoritative result
```

Permanent rule:

```text
done != verified
```

Compose never toggles a local completed/verified boolean and calls that Reality.

---

## 8. Five-primary IA is unchanged

R34 retains the explicit product decision:

```text
现在
基础设施
变更
记录
我
```

`我` is not downgraded.

No Region / Identity / Recovery / Maintain capability creates a sixth tab.

Lens growth remains contextual.

---

## 9. Product design closure index updated

`spec/ui-vnext/PDIG_VNEXT_DESIGN_CLOSURE_MATRIX.md`

is now indexed as R34 and includes:
- RegionFact proposal;
- latest production-source progress;
- five-primary permanence;
- explicit distinction between source completeness and runtime evidence.

The Region Lens architecture document now links directly to the governed
RegionFact proposal.

---

## 10. External research incorporated

External standards are representation inputs only, not PDIG Reality.

### ISO 3166

Used to justify stable territory/subdivision identifiers rather than storing UI
display names as truth.

### Unicode CLDR

Used for localized territory display names.

This lets PDIG preserve:

```text
Canonical code
!= localized label
!= user object's RegionFact authority
```

No external standard is used to infer a user's asset geography.

---

## 11. Remaining non-design gates

At R34, the remaining blockers fall into three classes.

### A. Canonical implementation gates

```text
Identity Anchor Subtype
Asset Lifecycle / Maintenance
RegionFact
IdentityContext
Factor / Recovery future schemas
```

Their design is complete; implementation requires the shared:

```text
Spec
→ schema/version
→ migration
→ codegen
→ fixtures
→ conformance
→ all platform runtimes
→ production UI
```

chain.

### B. Continuity runtime extensions

Still not authoritative end-to-end:

```text
shared FailureDomain Production Finding
freshness-driven Production Finding
unknown-critical-path Production Finding
generic FailureDomain-removal Blast Radius
bounded Minimal Cut Set
generic Recovery Solver
```

Their design is frozen in:
`spec/proposals/continuity-analysis-modes-v1.md`.

They must not be recreated as UI heuristics.

### C. Evidence / release gates

```text
latest exact-head CI
API36 Preview phone runtime
API36 tablet runtime
GPU runtime evidence
API36 Production secure rehearsal
human pixel acceptance
Android Reference Freeze
explicit release cutover / rollback decision
```

These are not missing design.

---

## 12. Stop line

```text
PDIG_VNEXT_PRODUCT_UX_DESIGN = CLOSED
R34_REGION_SEMANTICS_DESIGN = COMPLETE
R34_PRODUCTION_SOURCE_CONSISTENCY = IMPROVED

SOURCE_COMPLETE != CI_PASS
CI_PASS != RUNTIME_VERIFIED
RUNTIME_VERIFIED != HUMAN_ACCEPTED
HUMAN_ACCEPTED != RELEASE_CUTOVER

ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_RELEASE_CUTOVER = HOLD
```
