# ANDROID_UI_VNEXT_R29_SOURCE_REPORT

> 2026-10-10 · `feat/android-ui-vnext-translation`
>
> R29 is the production-source-binding closure that follows the R19–R27
> Android Light Reference design work.
>
> It does **not** claim fresh exact-head Android runtime / pixel / human acceptance
> and it does **not** cut the production launcher over to UI vNext.

## 0. Current truth

```text
ANDROID_UI_VNEXT_SOURCE = R29

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

CANONICAL_SCHEMA_CHANGE_FOR_R29 = NONE
DEPMAP_PAYLOAD_CHANGE_FOR_R29 = NONE

MAINACTIVITY_PRODUCTION_CUTOVER = HOLD
PHONE_EMAIL_SUBTYPE_CANONICAL = HOLD
LIFECYCLE_CANONICAL = HOLD
MANUAL_RELATION_SCHEMA_V4 = HOLD
PRODUCTION_FINDING_SCREEN_BINDING = HOLD

FRESH_R29_BUILD = PENDING
FRESH_R29_UNIT_TESTS = PENDING
FRESH_R29_INSTRUMENTATION = PENDING
FRESH_R29_PHONE_PIXELS = PENDING
FRESH_R29_TABLET_PIXELS = PENDING
FRESH_R29_GPU_EVIDENCE = PENDING
FRESH_R29_HUMAN_ACCEPTANCE = PENDING

ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_CUTOVER = HOLD
```

Old R17–R28 screenshots or CI results are historical evidence only.

---

## 1. Permanent product shell

The product decision remains:

```text
现在
基础设施
变更
记录
我
```

`我` is an intentional fifth primary destination and MUST NOT be downgraded to:
- avatar only;
- overflow;
- Settings;
- a secondary rail utility.

Compact uses all five bottom destinations.
Medium/Expanded use the same five primary rail destinations.

Settings / Sources / Establish remain children/context of `我`, not a sixth primary
destination.

---

## 2. Reference product direction remains intact

R29 does not redesign the accepted Android Light direction.

Reference product grammar remains:

```text
Inventory
→ Typed Dependency
→ Continuity
→ Change
```

The reference UI still includes:
- light-first consumer visual system;
- shared GPU Earth family;
- camera-projected region context;
- card financial-asset identity;
- number communication identity;
- card/number recorded lifecycle reference;
- truth-bounded Impact Lens;
- keep-number role;
- Current / Transition / After continuity choreography;
- Human Review;
- Establish / Import / Manual;
- Records as evidence/verification trace;
- fifth-primary Me workspace.

R29 focuses on making the real production-data path capable of eventually using
that product shell without smuggling synthetic reference truth into production.

---

## 3. Strict Reference vs Production boundary

Two authority modes are explicit:

```text
REFERENCE_PREVIEW
PRODUCTION_REALITY
```

A `ProductionVNextSession` rejects a Reference source.

Production shell behavior:

```text
missing production fact
→ render unknown / hold / unavailable

NEVER
→ read UiVNextDemoFixture as fallback
```

This is the central anti-demo-leak invariant.

Existing unit contract:
- `productionSessionRejectsReferenceSource`.

Production-only Compose contract:
- `ProductionVNextShellContractTest`.

---

## 4. Production read model

Production source:

```text
encrypted Canonical Reality
→ AppContainer
→ AppContainerVNextReadModelSource
→ VNextProductionSnapshot
→ consumer projection
→ ProductionVNextShell
```

The read model projects only existing authoritative facts:
- graph revision;
- non-archived Nodes;
- active confirmed Dependencies;
- Timeline;
- ChangePlans;
- Human Review counts;
- SourceInstances;
- ImpactResult;
- PlanDetail;
- Records built from action completion / verification.

Pending Proposal / Candidate / Drift is never promoted into confirmed relations.

---

## 5. Production object classification

Current safe mapping:

```text
payment_instrument → payment asset
account            → account
service            → service
device             → device
membership         → membership
identity_anchor    → generic identity
custom             → generic custom
```

Critical invariant:

```text
identity_anchor != phone
identity_anchor != email
```

R29 continues to forbid:
- phone-looking string → phone subtype;
- email-looking string → email subtype;
- country prefix → carrier;
- object name → recovery semantics.

Phone / Email production pages therefore render explicit HOLD until the governed
identity subtype proposal is implemented.

---

## 6. Production Infrastructure

R28/R29 closes the source routing gap.

Production Infrastructure now provides a real category hub.

Source-bound:
- Cards;
- Accounts;
- Devices;
- Services.

Explicit HOLD:
- Numbers — waits phone subtype authority;
- Emails — waits email subtype authority;
- Weaknesses — waits authoritative Finding consumer projection.

Wide layouts also get Infrastructure sibling navigation without promoting object
categories into primary navigation.

Empty lists say:

> no recorded/confirmed object

not:

> external object does not exist.

---

## 7. Production Card

Production card identity uses only current Canonical facts:
- node id;
- name;
- issuer when recorded;
- last4 when recorded;
- confirmed relations;
- authoritative Impact.

Focused card detail now shows:
- identity;
- confirmed relation list;
- consumer relation/capability/criticality wording;
- authoritative Impact states/checklist;
- explicit Change entry.

Reference-only lifecycle remains absent in production:
- annual fee;
- billing day;
- payment due day;
- installments;
- autopay summary.

Missing Canonical lifecycle = not rendered as invented fact.

---

## 8. Card → ChangePlan continuity loop

R29 closes the production source loop:

```text
Production Card Detail
→ authoritative Impact
→ explicit "建立/继续更换此卡的计划"
→ existing replace_payment_card plan, if any
OR
→ VNextChangeActionGateway.createPlan()
→ planDetail()
→ ProductionChangePlanScreen
```

No automatic replacement-card recommendation is made.

The UI does not interpret Impact analysis as execution.

---

## 9. Production Change Center / Plan

Production Change Center reads real ChangePlan summaries.

The focused plan screen reads `PlanDetailView` and provides:
- affected service count;
- unresolved must-change count;
- action DAG prerequisites;
- readiness;
- analyzed graph revision vs current graph revision;
- action completion;
- verification;
- evidence refs through Records.

Mutation chain:

```text
complete action
→ AppContainer.completeAction
→ re-read planDetail

verify action
→ AppContainer.verifyAction
→ re-read planDetail
```

Permanent rule:

```text
done != verified
```

Verification UI now distinguishes:
- no verification required;
- pending;
- evidence suggested;
- verified;
- failed.

Only pending/evidence-suggested states expose confirmation.

Stale plan rule:

```text
effectiveState = needs_revalidation
OR
currentGraphRevision != lastAnalyzedGraphRevision
→ block new completion recording
→ require re-analysis/confirmation
```

---

## 10. Production Records

Records is not a second Now page.

It projects only actionable historical/verification state from production plans:
- recorded complete;
- verified;
- pending verification;
- verification failed.

It preserves:
- action phase in consumer wording;
- evidence-ref count;
- unknown occurrence timestamp as unknown.

It MUST NOT substitute plan effective date for actual done/verified occurrence time.

---

## 11. Human Review

Production Human Review is source/action bound.

```text
Proposal / Candidate / Drift
→ Review inbox
→ explicit user decision
→ authoritative mutation
→ re-read inbox
```

There is no optimistic local disappearance that pretends Reality changed.

Consumer copy no longer labels the entire screen as “confirmed”.

Top authority badge:

```text
待复核 · 未进入已确认数据
```

An empty review inbox does not mean the graph is complete.

---

## 12. Production Search

R29 adds Reality-only global search.

Search indexes:
- confirmed production objects;
- object consumer type label;
- issuer / last4 where confirmed;
- ChangePlan title / scenario / state;
- SourceInstance labels;
- safe application routes.

It does NOT index:
- synthetic fixture lifecycle facts;
- unconfirmed review objects as confirmed assets;
- inferred phone/email subtype.

Blank query returns no implicit “everything” result.

Generic identity anchors remain generic and do not open Number/Email detail.

---

## 13. Fifth-primary Me — production personal control surface

`我` is not “Settings”.

Production Me now owns:
- graph revision/context summary;
- active source count;
- pending-review entry;
- Data Sources;
- Establish;
- privacy/presentation preferences;
- production Search.

Presentation preferences:
- privacy mask;
- reduce motion;
- show upcoming;
- expanded rail.

These mutate `VAppState` only.

Permanent boundary:

```text
Presentation preference
!=
Personal Reality
!=
Canonical truth
```

`createProductionVNextSession` accepts an injected store-backed `VAppState` so
the production launcher can later preserve preferences using the existing local
stores without adding them to .depmap.

Until the launcher injects that store-backed state, UI must not claim cross-restart
persistence.

---

## 14. Establish / Manual object

Production Manual Establish is now source-bound to the existing authority.

Current directly creatable set follows generated runtime policy:
- payment_instrument;
- account;
- service.

Creating a Node:
- creates that confirmed object;
- bumps graphRevision through the authoritative transaction;
- creates no Dependency.

Permanent rule:

```text
confirm object exists
!=
confirm relationship
```

---

## 15. File Import host handoff

VNext does not create another picker/parser/security workflow.

R29 adds an optional host action:

```text
requestFileImport()
```

Required chain:

```text
VNext Establish
→ host callback
→ existing FileWorkflowCoordinator
→ OpenDocument
→ lock / re-auth
→ parse
→ previewImport
→ commitImport
→ Human Review for proposals/candidates/drifts
```

If the host callback is not injected:
- File Import shows that the formal entry is not connected;
- no fake picker is displayed.

---

## 16. Manual Relationship

The UX design is complete but mutation remains correctly blocked.

Current native production schema is still v3 while the complete intended relation
vocabulary requires the next schema line.

R29 provides a dedicated HOLD screen explaining:
- From / Relation / To;
- Capability;
- Criticality;
- manual vs proposal provenance;
- confirmation boundary;
- why partial v3-only mutation is forbidden.

Unlock requires:
- Native Schema v4 migration;
- runtime validation;
- golden/negative fixtures;
- Android/iOS/Harmony conformance;
- authoritative relationship gateway.

No ghost “确认关系” button exists before those gates.

---

## 17. Production authority badge

A single “confirmed” top badge for every production screen would be false.

R29 makes the badge context-sensitive:

```text
ordinary production pages
→ 已确认数据

Review
→ 待复核 · 未进入已确认数据

Settings
→ 本机呈现偏好

Establish
→ 建立 · 需明确确认

ChangePlan detail
→ 变更计划 · 正式状态
```

This is a Truth UX requirement, not cosmetic copy.

---

## 18. Consumer-language closure

R29 removes primary user-facing raw wire jargon from production surfaces.

Examples:

```text
payment_instrument → 支付工具
funding_source     → 资金来源
authentication     → 认证
required           → 已确认必需
in_progress        → 进行中
prepare            → 准备
active source      → 使用中
```

Internal wire strings may remain in:
- domain data;
- tests;
- logs;
- developer documentation;
- testTag IDs.

They should not be the main consumer copy.

---

## 19. Production UI tests added

New/extended source contracts include:

- `VNextRuntimeDataSourceTest`
  - Reference source cannot become Production session;
- `ProductionVNextReadModelTest`
  - proposal != confirmed dependency;
  - generic identity != phone;
  - issuer/last4 projection;
  - done != verified;
  - record evidence semantics;
- `ProductionSearchProjectionTest`
  - confirmed production search only;
  - consumer scenario labels;
  - generic identity remains generic;
  - blank query returns empty;
- `ProductionVNextShellContractTest`
  - five primary destinations;
  - `我` remains first-class;
  - production search route;
  - production category/detail;
  - local presentation preference mutation.

Presence of tests is not a PASS claim until exact-head CI/runtime executes them.

---

## 20. What R29 intentionally does NOT implement

### 20.1 Production launcher cutover

Production remains:

```text
MainActivity
→ lock-gated PdigApp
```

R29 does not bypass or weaken:
- LockGate;
- device authentication;
- secure-window behavior;
- restore/import re-auth behavior.

### 20.2 phone/email subtype

Waits governed identity subtype Canonical implementation.

### 20.3 card/number lifecycle persistence

Waits MaintenanceFact / MaintenanceSchedule Canonical decision and cross-platform
implementation.

### 20.4 production Weaknesses

Waits authoritative Infrastructure Finding projection.

No reference finding list may be shown as production truth.

### 20.5 manual Dependency mutation

Waits Native Schema v4 + conformance + authority.

### 20.6 future roadmap capabilities

Device Continuity, Recovery Incident, Identity Context, Digital Resource Continuity,
Trusted Handoff and other later concepts remain governed by the capability matrix.
Design-complete does not make them production-executable.

---

## 21. Exact-head evidence still required

For the exact current remote head:

```text
1. codegen / spec gates
2. unit tests
3. Android compile
4. Android instrumentation
5. API36 phone Preview runtime
6. tablet runtime
7. GPU first-frame + performance evidence
8. five-primary navigation proof
9. Human Pixel Review
10. production-shell instrumentation
11. lock/security integration E2E before cutover
12. restart/persistence E2E before cutover
```

Do not reuse an older PASS after source changes.

---

## 22. Honest stop line

```text
PDIG_VNEXT_PRODUCT_UX_DESIGN = CLOSED
ANDROID_REFERENCE_SOURCE = IMPLEMENTED
PRODUCTION_VNEXT_SOURCE_BINDING = IMPLEMENTED_TO_CURRENT_CANONICAL_AUTHORITY

ANDROID_RUNTIME_ACCEPTANCE = PENDING
ANDROID_REFERENCE_FREEZE = HOLD

PRODUCTION_LAUNCHER_INTEGRATION = HOLD
PRODUCTION_SECURITY_E2E = HOLD
PRODUCTION_VNEXT_CUTOVER = HOLD

FUTURE_CANONICAL_CAPABILITIES = GATED

SOURCE_COMPLETE != RUNTIME_VERIFIED != SHIPPED
```
