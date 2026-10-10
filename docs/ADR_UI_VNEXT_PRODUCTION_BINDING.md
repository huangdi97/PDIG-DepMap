# ADR — UI vNext Production Binding

> Date: 2026-10-09  
> Status: **R31 PRODUCTION_SHELL_SOURCE_IMPLEMENTED / NOT_CUT_OVER**  
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

R19 now also defines an authoritative mutation gateway at
`uivnext/production/ProductionVNextActions.kt`:

```text
createPlan
completeAction
verifyAction
→ delegate to AppContainer
→ immediately re-read planDetail
→ render authoritative result
```

The UI must never toggle a local "done" flag and then infer verification from it.

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
- Canonical `issuer` when present;
- Canonical `last4` when present;
- fields explicitly recognized by current Canonical semantics;
- confirmed dependency count via production dependencies/impact.

The Android production read projection now carries `issuer` and `last4` directly
from the existing `nodes` table. This is not a new schema field and does not parse
free-form `fields_json`.

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

Only an identity anchor with a valid governed `identity_anchor_profile` subtype may
be projected as a Number or Email asset.

R37/R38 now implement:
- confirmed `PHONE_NUMBER` → Number surface;
- confirmed `EMAIL_ADDRESS` → Email surface;
- missing/malformed/bare subtype → Generic Identity;
- optional independently confirmed nested identifier value;
- invalid identifier → value unavailable while a valid subtype remains confirmed.

The governed contract is in `spec/proposals/identity-anchor-subtype-v1.md`.
Existing anchors without a valid profile remain generic; no name/regex/provider
heuristic is allowed.

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

R21 also implements `VNextReadModelSource.records()` over authoritative
`PlanDetailView` actions. It preserves verification Evidence refs and deliberately
leaves occurrence time unknown when the Android domain projection does not expose
`doneAt/verifiedAt`; `effectiveDate` must never be substituted as history.

## 9. Cutover phases

### P0 — Reference freeze
- exact-current-head build/test;
- API36 phone/tablet runtime evidence;
- GPU runtime evidence;
- human visual acceptance;
- Android Reference Freeze.

### P1 — Read-only production adapter — SOURCE IMPLEMENTED
Implemented:
- node inventory;
- active confirmed dependencies;
- production ImpactResult mapping;
- derived Timeline mapping;
- ChangePlan / PlanDetail mapping;
- Records action/completion/verification/evidence projection;
- pending proposal/candidate/drift counts kept separate from confirmed Reality;
- source coverage counts.

Unit contracts ensure pending proposals are not promoted into confirmed edges and
`done != verified` survives projection.

R29 also makes authority boundaries explicit in UI chrome:
- confirmed Reality pages → `生产 Reality`;
- Review → `待复核 · 未进入 Reality`;
- Settings → `本机呈现偏好`;
- Establish → `建立 · Authority`;
- ChangePlan → `ChangePlan Authority`.

A single global "confirmed" badge is forbidden because it would misclassify
Proposal/Candidate/Drift and local Presentation state.

R28/R29 additionally implements screen-level source binding inside a dedicated
`ProductionVNextShell` for:
- Now;
- Infrastructure overview + safe categories/details;
- Card Impact / confirmed relations;
- Change Center / plan detail;
- Records;
- Review;
- Sources / Establish / Manual Establish;
- Reality-only Search;
- Me / local Presentation preferences.

Still not done in P1:
- productionRelease default cutover;
- lifecycle editing/persistence;
- region/provider/SIM/keep-alive authority beyond the governed identifier;
- exact-head release/runtime acceptance.

No lifecycle editing is enabled.

### P2 — Object identity normalization — R38 SOURCE IMPLEMENTED FOR PHONE/EMAIL

Implemented safely:
- `payment_instrument` → payment-asset surface;
- Canonical `issuer` / `last4` carried from Reality;
- account/service/device/membership mapped only by confirmed NodeKind;
- governed `PHONE_NUMBER` / `EMAIL_ADDRESS` subtype mapping;
- independently confirmed raw phone/email identifier projection;
- invalid/missing identifier preserves subtype but renders value as unknown;
- privacy-safe list/detail/search presentation;
- generic/bare/malformed identity profile remains Generic Identity;
- truth-bounded consumer inventory projection with confirmed-dependency counts.

Still gated:
- provider/carrier/SIM/region authority;
- keep-alive/lifecycle persistence;
- IdentityContext/recovery semantics not proven by subtype/value.

Unknown remains unknown.

### P3 — Change binding — ACTION + SCREEN SOURCE IMPLEMENTED / LAUNCHER HOLD

Implemented:
- authoritative `VNextChangeActionGateway`;
- plan creation delegates to `createPlanForScenario`;
- completion delegates to `completeAction`;
- verification delegates to `verifyAction`;
- every mutation re-reads `planDetail`.

Implemented in the production shell:
- existing authoritative ChangePlan list;
- `PlanDetailView` action rendering;
- readiness + graph-revision stale boundary;
- complete/verify actions re-read authoritative `planDetail`;
- Card Detail can create or continue `replace_payment_card` explicitly;
- internal action IDs / wire states are not used as primary consumer copy.

Still gated:
- production launcher injection;
- no longer gated: confirmed Production Number objects may enter `replace_phone_number`; PlanRepository independently re-validates PHONE_NUMBER subtype before plan creation;
- new scenario primitives beyond Canonical runtime availability.

The UI must never infer verification from local presentation state.

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

R32 behavior:

```text
preview
→ VNext synthetic reference UI

productionRelease
→ lock-gated existing PdigApp
→ ignores vnext_demo / vnext_production extras

productionDebug default
→ lock-gated existing PdigApp

productionDebug + vnext_demo
→ synthetic reference evidence path (debug only)

productionDebug + vnext_production
→ AppContainer Reality
→ createProductionVNextSession
→ PdigSecureContent / LockGate
→ ProductionVNextShell
```

The productionDebug Reality path is a cutover rehearsal, not release activation.

Do not change this to:

```text
production → synthetic VNext
```

A future production cutover must instantiate VNext with a real production source,
not merely flip `shouldLaunchVNext()`.

## 10.1 R32 secure production rehearsal

R32 extracts the existing production lock lifecycle into `PdigSecureContent`.
Both the legacy production application and the debug Production VNext host reuse
that same fail-closed gate.

```text
Production Reality UI
→ LockChecking
→ LockScreen
→ explicit unlock
→ content composition
```

There is no Production VNext branch that composes encrypted Reality before unlock.
Release builds cannot enable either VNext debug route through Intent extras.
Final release cutover therefore changes the selected **unlocked content**, not the
security architecture.

## 11. Security / privacy

The production adapter:
- reads through the existing encrypted database path;
- does not bypass lock state;
- does not log names, numbers, identifiers or graph content;
- must respect workspace masking;
- must never copy secrets, recovery codes, passwords or private keys into the
  VNext read model;
- may expose only labels/metadata that the existing product is permitted to show.

## 11.1 Host / file workflow boundary — R29

The production session now accepts optional host-owned actions. File import uses:

```text
VNext Establish
→ requestFileImport()
→ existing Activity-scoped FileWorkflowCoordinator
→ OpenDocument
→ lock / re-auth
→ parse / preview
→ AppContainerVNextImportAuthority
→ commit
→ Human Review for proposals/candidates/drifts
```

VNext does not own a second ActivityResult launcher, URI lifetime, parser, or
re-authentication path.

If the host action is absent, the UI renders `Host binding 待接` and the import
entry is disabled. It must not fake a file picker.

R31 supersedes the old Native-Schema-v4 blanket HOLD for Manual Relationship.
Production may authoritatively create the complete **current Canonical v3 runtime
registry** through `AppContainer.createManualDependency`; storage-only/future
`verifies` / `bound_to` remain gated. Preview stays read-only.

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

## 13. Human Review production binding

R22 adds the consumer Human Review surface but keeps Preview read-only.

Production source:

```text
AppContainer.pendingProposals()
AppContainer.pendingCandidates()
AppContainer.openDrifts()
→ AppContainerVNextReviewSource
→ VNextProductionReviewQueue
→ ProductionReviewConsumerInbox
```

Production mutations:

```text
Proposal
  acceptProposal / rejectProposal

Candidate
  acceptCandidate / dismissCandidate

Drift
  resolveDriftAsReplacement
  resolveDriftAsAdditionalPath
  dismissDrift
```

`稍后确认` performs no domain mutation.

Required interaction architecture:

```text
user chooses decision
→ VNextReviewActionGateway
→ AppContainer authoritative mutation
→ re-read review queue
→ re-read production snapshot / revision where relevant
→ render authoritative result
```

The production screen must never optimistically remove a review card and treat that
as confirmation before the AppContainer call succeeds.

Review source/action binding may be implemented before launcher cutover behind a
production-source test harness, but synthetic Preview items and production queue
items must never share mutation handlers.


## 14. Establish / Import production binding

R23 does not create a new ingestion stack.

Production VNext must reuse:

~~~text
FileWorkflowCoordinator
→ existing Activity-scoped picker
→ LockGate / re-auth lifecycle
→ AppContainer.parseFile
→ AppContainer.previewImport
→ AppContainer.commitImport
~~~

VNext-specific source added:

- ProductionVNextImport.kt — consumer-safe Preview/Commit projections.

The file workflow itself remains the existing production implementation.

Required future cutover sequence:

~~~text
VNext user opens Establish
→ existing FileWorkflowCoordinator begins IMPORT
→ picker result survives lock
→ re-auth
→ existing parser
→ VNext renders consumer preview projection
→ explicit Confirm Import
→ existing AppContainer.commitImport
→ VNext renders authoritative commit projection
→ if proposals exist, route to Human Review
~~~

Never duplicate ActivityResult launchers, parser logic, URI retention, or import SQL
inside UI vNext.


## 15. Manual Reality mutation gates

R24/R25 freeze the consumer UX for manual object and relationship establishment,
but production VNext must not implement them through direct Compose/SQL writes.

### Manual object authority — SOURCE IMPLEMENTED

Generic object creation:
- validates against `runtimeCreatableNodeKinds`;
- creates payment/account/service only;
- writes Node + one graphRevision bump;
- creates no Dependency.

R38 governed phone/email creation uses a **separate authority**, not a widened generic
identity button:
- Canonical `runtimeCreatableIdentityAnchorSubtypes` = phone_number/email_address;
- one transaction creates `identity_anchor` + confirmed subtype + confirmed exact identifier;
- one graphRevision bump;
- no Dependency side effect;
- duplicate identifier values do not silently merge;
- Preview remains read-only.

Generic identity/device/membership/custom remain gated.

### Manual Dependency authority

Required future domain API:
- confirmed From/To only;
- validate relation/capability via runtime registry;
- origin = manual;
- default criticality = unknown;
- required only from explicit human action;
- graphRevision bump in the same transaction;
- deterministic duplicate logical-key behavior;
- return authoritative Dependency/revision state.

Until that exists:
- Preview has no Confirm Relationship action;
- UI must not fake success locally.

These are production authority gates, not missing presentation designs.


## 13. R31 Manual Relationship authority

The Production VNext binding now includes a write-side relation authority:

```text
ProductionManualRelationshipScreen
→ AppContainerVNextManualRelationshipGateway
→ AppContainer.createManualDependency
→ GraphRepository.createManualDependency
→ validateRelationUse
→ DB transaction + graphRevision
→ authoritative re-read
```

Allowed today:

```text
funding_source      / payment
merchant_agreement  / payment
recovers            / recovery
authenticates       / authentication
controls            / access
```

Forbidden today:

```text
verifies
bound_to
arbitrary relation/capability pairs
UI-side SQL
silent required criticality
duplicate logical rows
```

Preview stays read-only even though Production owns this authority.

This narrows the remaining cutover blockers to capabilities that genuinely still
lack shared authority: lifecycle/region/provider context persistence, remaining
Finding inputs, release-default activation, and exact-head security/runtime/human
acceptance. Identity subtype + raw phone/email identifier authority is now R38
source-implemented.
