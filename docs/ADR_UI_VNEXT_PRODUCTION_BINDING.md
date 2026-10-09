# ADR — UI vNext Production Binding

> Date: 2026-10-09  
> Status: **READ_ONLY_ADAPTER_SOURCE_IMPLEMENTED / NOT_CUT_OVER**  
> Scope: Android UI vNext → existing Android production domain/runtime  
> Canonical change: **NONE**

## 0. Decision

UI vNext must not enter the production launcher by replacing the existing
`PdigApp` with the synthetic reference fixture.

Production cutover is a **read-model binding problem**, not a flavor switch.

The target architecture is:

```text
Canonical / SQLCipher Reality
        │
        │ AppContainer read APIs
        ▼
Production VNext Read Model Adapter
        │
        ├── Inventory projections
        ├── Confirmed dependency projections
        ├── Impact projection
        ├── Timeline / maintenance projection
        └── ChangePlan projection
        ▼
VNext presentation models
        ▼
Android Light Reference UI
```

Reference mode remains:

```text
Synthetic reference fixture
        ▼
VNext presentation models
        ▼
same Android UI
```

The UI must not be able to tell a richer story than its source can prove.

## 1. Existing production APIs already available

The current Android production application already exposes, through
`AppContainer`:

```text
nodes()
dependencies()
plans()
openDrifts()
pendingCandidates()
sourceInstances()
graphRevision()
timeline()
pendingProposals()

impactFor(nodeId)
loadImpactGraph()
integrity()

createPlanForScenario()
planDetail()
completeAction()
verifyAction()
```

This is enough to bind the core PDIG product loop without creating a second
domain implementation.

The production adapter must call these APIs. It must **not** reimplement impact,
readiness, graph revision, or verification logic in Compose.

## 2. Why a direct fixture → production swap is unsafe

Current UI vNext reference models include presentation facts that the Canonical
schema does not yet define as semantic fields:

### Card lifecycle reference
```text
annualFee
annualFeeDue
billingDay
paymentDueDay
installmentSummary
autoPaySummary
```

### Number lifecycle reference
```text
billingMode
planCost
keepAliveDue
keepAliveCycle
lastKeepAlive
renewalMethod
```

Current Canonical `Node.fields` is free KV, but only fields explicitly recognized by
the Canonical contract have cross-platform semantic meaning. Writing these R19
reference fields into `fields_json` ad hoc would create Android-only truth and violate
`spec/README.md`.

Therefore:

```text
R19 lifecycle UI = reference/proposed presentation semantics
NOT production persistence semantics
```

## 3. Production read-model interface

The read-only source seam is now implemented at:

`android/app/src/main/kotlin/com/pdig/uivnext/production/ProductionVNextReadModel.kt`

It exposes an immutable snapshot rather than leaking repositories into Composables:

```kotlin
interface VNextReadModelSource {
    fun snapshot(): VNextSnapshot
    fun impact(targetNodeId: String): VNextImpactProjection
    fun plan(planId: String): VNextPlanProjection?
}
```

Conceptual `VNextSnapshot`:

```text
revision
objects
confirmedDependencies
timeline
attention
activePlans
pendingReviewCounts
sourceCoverage
```

Every field must carry enough provenance/state to distinguish:

```text
confirmed
proposal
derived
unknown
reference-only
```

No screen should reach directly into SQL or reconstruct graph semantics.

## 4. Inventory mapping rules

### 4.1 Card

Production source:

```text
Node.kind = payment_instrument
```

Safe today:
- stable node id;
- name;
- archived state;
- fields explicitly recognized by current Canonical semantics;
- confirmed dependency count via production dependencies/impact.

Do not infer:
- issuer from name;
- region from currency;
- network from last four digits;
- annual fee from product name;
- billing day from imported transaction dates.

If a VNext card field is unavailable, render `未记录`.

### 4.2 Number

Production source:

```text
Node.kind = identity_anchor
```

Only an identity anchor that is explicitly typed/recognized as a phone number may
be projected as a Number asset.

Do not infer:
- phone role from country;
- keep-number role from inactivity;
- unique recovery from `recovers` edge count;
- carrier from number prefix unless a governed Provider rule explicitly supplies it.

### 4.3 Account / Email / Device / Service

Map only from existing node kinds and confirmed metadata. If the current coarse
NodeKind cannot distinguish an object safely, the adapter must retain a generic
object surface rather than invent a more specific type.

## 5. Dependency and Impact binding

UI vNext Impact Lens must use:

```text
AppContainer.impactFor(nodeId)
+ confirmed dependencies
+ production findings / evidence state
```

Never:

```text
count outgoing edges
=> independent paths
=> safe
```

Path independence is failure-domain-aware. UI code cannot reproduce it with degree
counting.

A Production Impact Lens should map domain truth into consumer language:

```text
must_change        → 必须处理
backup_path        → 有已确认备用路径，但冗余下降
degraded           → 能力下降
needs_review       → 需要核对
unaffected         → 当前已确认范围内未受影响
unknown            → 未知 / 未记录
```

The UI must still avoid a global health score.

## 6. Recovery semantics

Permanent invariant:

```text
used for recovery != unique recovery path
```

Production uniqueness must come from explicit confirmed graph/finding semantics,
not from the R19 reference boolean itself.

The R19 `uniqueRecoveryPath` field is a presentation fixture aid only. The
production adapter should eventually consume the real Continuity/Finding result.

## 7. Timeline and lifecycle

Production `AppContainer.timeline()` remains the authoritative timeline projection
for currently supported Canonical events.

R19 lifecycle maintenance should join the production timeline only after its
semantics are Canonicalized.

Future desired timeline sources:

```text
card expiry
annual-fee checkpoint
billing checkpoint
number keep-alive due
provider waiting window
verification due
plan action due
freshness review
```

Each item requires:
- stable source id;
- source type;
- due/effective time;
- truth/provenance state;
- no silent auto-completion.

## 8. Change binding

Production already supports executable REPLACE scenarios, including:

```text
replace_payment_card
replace_phone_number
```

The VNext UI must reuse:

```text
createPlanForScenario()
→ planDetail()
→ completeAction()
→ verifyAction()
```

The R19 phone continuity scene is a **presentation reference**. Production state
must come from `PlanDetailView` and its actions/readiness.

Mandatory rule:

```text
done != verified
```

The UI may animate a completed action, but cannot advance verification unless the
production plan says it is verified.

## 9. Cutover phases

### P0 — Reference freeze
- exact-head R19 build;
- phone/tablet runtime evidence;
- human visual acceptance;
- Android Reference Freeze.

### P1 — Read-only production adapter — SOURCE IMPLEMENTED
Implemented:
- node inventory;
- active confirmed dependencies;
- production ImpactResult mapping;
- derived Timeline mapping;
- ChangePlan / PlanDetail mapping;
- pending proposal/candidate/drift counts kept separate from confirmed Reality;
- source coverage counts.

Unit contracts ensure pending proposals are not promoted into confirmed edges and
`done != verified` survives projection.

Still not done in P1:
- production launcher/UI binding;
- object-specific identity normalization;
- lifecycle editing.

No lifecycle editing is enabled.

### P2 — Object identity normalization
Add governed mapping for:
- payment instrument fields;
- phone identity fields;
- region/context metadata;
- object-specific display names.

Unknown remains unknown.

### P3 — Change binding
Replace synthetic Change Phone projection with actual `PlanDetailView`.

Use existing production scenario creation and verification.

### P4 — Lifecycle Canonical proposal
Only if product decision remains positive:

```text
Spec
→ logical schema / migration decision
→ fixtures
→ conformance expected
→ Android/iOS/Harmony implementations
→ production UI edit/review
```

### P5 — Production launcher cutover
Only after:
- read-model parity;
- persistence migration tests;
- no synthetic fixture reachable in production;
- lock/security flow remains intact;
- production E2E passes;
- rollback route exists.

## 10. Flavor rule

Current behavior is intentionally retained:

```text
preview     → VNext reference UI
production  → lock-gated existing PdigApp
```

Do not change this to:

```text
production → synthetic VNext
```

A future production cutover must instantiate VNext with a real production source,
not merely flip `shouldLaunchVNext()`.

## 11. Security / privacy

The production adapter:
- reads through the existing encrypted database path;
- does not bypass lock state;
- does not log names, numbers, identifiers or graph content;
- must respect workspace masking;
- must never copy secrets, recovery codes, passwords or private keys into the
  VNext read model;
- may expose only labels/metadata that the existing product is permitted to show.

## 12. Acceptance gates

```text
VNext reference visual accepted
AND
production read-model adapter tested
AND
canonical parity unchanged
AND
production lock/backup/restore unaffected
AND
real ChangePlan binding verified
AND
synthetic fixture unreachable from production
= production cutover candidate
```

Until all are true:

```text
PRODUCTION_VNEXT_CUTOVER = HOLD
```
