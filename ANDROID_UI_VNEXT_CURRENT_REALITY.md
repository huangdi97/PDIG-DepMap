# ANDROID_UI_VNEXT_CURRENT_REALITY.md

> **Current reality · 2026-10-10 · R27**
>
> Branch: `feat/android-ui-vnext-translation`
>
> This file supersedes the original pre-translation A0 audit. Repository truth on
> the current branch has moved far beyond that baseline.
>
> Status vocabulary:
> `IMPLEMENTED_SOURCE / DESIGN_FROZEN / NOT_CONNECTED / NOT_RUN / HOLD`.

## 0. Executive state

```text
ANDROID_UI_VNEXT_SOURCE = R27
ANDROID_LIGHT_REFERENCE_DIRECTION = DESIGN_FROZEN
ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_READ_MODEL = SOURCE_IMPLEMENTED
PRODUCTION_VNEXT_CUTOVER = HOLD

CANONICAL_SCHEMA_CHANGE_FOR_R27_SOURCE = NONE
DEPMAP_PAYLOAD_CHANGE_FOR_R27_SOURCE = NONE

FRESH_R27_BUILD = PENDING
FRESH_R27_UNIT_TESTS = PENDING
FRESH_R27_INSTRUMENTATION = PENDING
FRESH_R27_PHONE_PIXELS = PENDING
FRESH_R27_TABLET_PIXELS = PENDING
FRESH_R27_HUMAN_ACCEPTANCE = PENDING
```

Do not reuse pre-R27 screenshots or old PASS statements as proof of the current
source.

## 1. Navigation / shell

| Area | Current reality |
| --- | --- |
| Primary IA | **IMPLEMENTED_SOURCE** — exactly Now / Infrastructure / Change / Records / Me |
| Me | **IMPLEMENTED_SOURCE** — intentional fifth primary destination; compact consumer workspace + dedicated Medium/Expanded adaptive workspace; avatar may remain as shortcut |
| Infrastructure secondary | **IMPLEMENTED_SOURCE** — Overview/Cards/Numbers/Accounts/Emails/Devices/Services/Weaknesses |
| Breakpoints | **IMPLEMENTED_SOURCE** — Compact <600dp, Medium 600–839dp, Expanded >=840dp |
| System Back | **IMPLEMENTED_SOURCE** — chronological stack |
| Header Up | **IMPLEMENTED_SOURCE** — hierarchical parent |
| State host | **IMPLEMENTED_SOURCE** — `VNextShellViewModel` |
| Persistent presentation prefs | **IMPLEMENTED_SOURCE** — local stores, outside Canonical |
| Production launch | **HOLD** — production still lock-gated legacy application |

## 2. Now / Globe

| Area | Current reality |
| --- | --- |
| Light-first Now composition | **IMPLEMENTED_SOURCE** |
| GPU Earth | **IMPLEMENTED_SOURCE** — separate OpenGL path |
| Albedo/night/cloud textures | **IMPLEMENTED_SOURCE** |
| Atmosphere/day-night/ocean glint | **IMPLEMENTED_SOURCE** |
| CPU fallback | **IMPLEMENTED_SOURCE** |
| Drag/zoom/reset | **IMPLEMENTED_SOURCE** |
| Projected region labels | **IMPLEMENTED_SOURCE** — live camera projection |
| Region grouping/collision budget | **IMPLEMENTED_SOURCE** |
| Region card/number/attention context | **IMPLEMENTED_SOURCE** |
| Exact-head GPU runtime proof | **NOT_RUN on current R26 head** |

Globe tethers are geographic annotation tethers only. They are not graph edges.

### Now task ownership — R22

Now now owns pending work explicitly:
- Human Review;
- current attention;
- active changes;
- upcoming maintenance.

R22 removes stale links that treated Records as a generic "查看全部" page. Active
changes route to Change Center; pending review routes to Human Review; future
maintenance remains on Now. Records keeps its evidence/history boundary.


## 3. Infrastructure

| Screen | Current reality |
| --- | --- |
| Overview | **IMPLEMENTED_SOURCE** |
| Cards | **IMPLEMENTED_SOURCE** |
| Card Detail | **IMPLEMENTED_SOURCE** |
| Card appearance customization | **IMPLEMENTED_SOURCE** |
| Numbers | **IMPLEMENTED_SOURCE** |
| Number Detail | **IMPLEMENTED_SOURCE** |
| Number appearance customization | **IMPLEMENTED_SOURCE** |
| Accounts | **IMPLEMENTED_SOURCE** — collection + focused detail + Impact Lens |
| Emails | **IMPLEMENTED_SOURCE** — collection + recovery-aware focused detail + Impact Lens |
| Devices | **IMPLEMENTED_SOURCE** — collection + focused detail + Impact Lens |
| Services | **IMPLEMENTED_SOURCE** — collection + recorded incoming relations + Impact Lens |
| Weaknesses | **IMPLEMENTED_SOURCE** |
| Search | **IMPLEMENTED_SOURCE** |
| Empty states | **IMPLEMENTED_SOURCE** |

These are reference/presentation implementations. That does not mean every field is
yet backed by production Canonical persistence.

## 4. Cards

R19 Card = **Financial Asset Identity**.

Implemented at source:
- strong card identity/collection feeling;
- region/currency/type/form/network/expiry;
- status;
- confirmed fixture service relationships;
- minor local card-art customization;
- recorded lifecycle reference:
  - annual fee;
  - annual-fee checkpoint;
  - billing day;
  - payment due day;
  - installment summary;
  - recorded autopay;
- Impact Lens;
- list/detail/inspector adaptive parity.

Truth boundary:
- missing lifecycle = `未记录`;
- no statement balance/transaction/minimum-payment inference;
- lifecycle fixture is synthetic reference data, not .depmap truth.

## 5. Numbers

R19 Number = **Communication Identity**.

Implemented at source:
- local user display alias;
- blank alias fallback to recorded/masked number;
- role = primary / secondary / keep;
- carrier / SIM-eSIM / region;
- recorded recovery use;
- explicit unique-recovery evidence;
- recorded lifecycle:
  - plan cost;
  - keep-alive due;
  - keep-alive cycle;
  - last action;
  - renewal/keep method;
- Impact Lens;
- alias/lifecycle search;
- compact/adaptive parity.

Permanent correction:

```text
recoveryOnly != uniqueRecoveryPath
```

A recovery role is not automatically a single point of failure.

## 6. Change Center + supported focused changes

Primary `变更` is a distinct work center:
- active changes first;
- Prepare entries second;
- maintenance/review entry points;
- no mutation implied by opening the page;
- no unsupported generic Change CTA.

The focused Replace Phone flow remains a child of `变更`.

Change Phone source implementation:
- Current / Transition / After;
- After explicitly remains Plan Projection;
- six-stage choreography;
- Make-Before-Break block;
- service migration states;
- verification language;
- old active number / keep number / new migration target are distinct objects;
- impact summary derived from actual projection state.

Reference IDs:
```text
num-cn-1 old active
num-cn-3 keep
num-cn-4 migration target
```

These IDs are synthetic only; the identity separation is the product invariant.

### Change Card — R21

Because `replace_payment_card` is already an active production scenario, R21 adds
the missing focused card-replacement reference:

```text
Current
→ old card + recorded payment relations

Transition
→ dependency review complete
→ migrate payment relations
→ verification blocked until migration/evidence prerequisites

After
→ plan projection only
```

Card Detail now exposes **分析更换此卡的影响**. Replacement cards are user-selected
reference plan targets, never automatic recommendations or inferred independent
backups.

Route hierarchy:

```text
Change Card → Up → Change
System Back → actual previous page
```


## 7. Impact Lens

R19 source introduces a shared consumer answer to:

> **如果它发生变化？**

Reference lens can show:
- confirmed dependency count;
- recorded attention findings;
- critical accounts when known;
- explicit unique recovery when known;
- independent alternatives when known;
- unknown/unconfirmed relations.

Null/unknown never becomes 0/safe.

The synthetic reference lens is not the production Continuity engine. Production
binding must consume `AppContainer.impactFor(nodeId)` and future failure-domain-aware
analysis.

## 8. Secondary object detail closure

R20 closes the remaining focused-detail gap for long-tail Infrastructure objects:

```text
Account = access / control identity
Email   = communication / recovery identity
Device  = physical access endpoint
Service = dependency endpoint
```

Each now has:
- direct collection-row entry;
- direct Search entry;
- object-specific identity/context;
- truth-bounded Impact Lens;
- explicit unknown semantics;
- stable Header Up parent;
- no executable Change CTA unless production supports that primitive.

Adaptive Account/Email collections and Search subtitles also respect the workspace
privacy mask.

## 8.1 Continuity Findings — full v0.3 consumer grammar

R27 closes the remaining visible gap between the v0.3 continuity engine vocabulary
and the vNext Weaknesses screen.

Preview now has explicit review states for all seven Infrastructure Finding classes:

```text
single point of failure
shared failure domain
recovery cycle
unconfirmed fallback
stale recovery information
unknown critical path
pending verification
```

Each card includes what / why / confirmed basis / unknowns / next action.

The reference list is synthetic and intentionally **not** derived by counting
`UiVNextDemoFixture.relations`. Production must use authoritative continuity
analysis; path count is never treated as independent-path count.

Weaknesses also keeps expiry/device/migration reminders, but they are presented as
a separate maintenance/operation layer rather than merged into a fake risk score.

## 9. Records — evidence / verification trace

R21 replaces the old “second Now” Records presentation.

Records now answers:

> **发生过什么、验证过什么、依据是什么？**

Reference projection:
- completed ChangeStage → **已记录完成**, never auto-verified;
- verifying ChangeStage → **待验证**;
- not-started / future blocked stages are not history;
- Attention / Upcoming remain on Now and are not copied into Records;
- active plan cards are navigation context only.

Current reference fixture intentionally shows:

```text
已记录完成 = 2
已验证 = 0
待验证 = 1
```

This is fixture projection, not a production-user history claim.

## 10. Search

Source search now covers:
- card/number names;
- saved number aliases;
- region/carrier/issuer;
- annual fee / billing facts;
- keep-alive facts;
- consumer role label `保号`;
- services/navigation destinations.

Search only searches recorded/reference data; absence is not proof of nonexistence.

R22 also makes Human Review discoverable through:
- 待复核;
- 复核 / 确认 / 建议;
- proposal / candidate / drift.

Search opens Review only; it never performs a review decision.

Me/Data Sources also expose consumer navigation into 建立基础设施 and 待复核. On
wide layouts these focused routes retain the `我` or `现在` primary parent context
instead of appearing as orphan screens.


## 11. Production binding

Current reference route:
```text
Preview flavor
→ VNext
→ synthetic reference fixture
```

Current production route:
```text
Production flavor
→ lock-gated PdigApp
→ AppContainer / encrypted Reality
```

They are intentionally not merged yet.

Production APIs already exist:
- `nodes()`
- `dependencies()`
- `timeline()`
- `impactFor()`
- `plans()`
- `planDetail()`
- `createPlanForScenario()`
- `completeAction()`
- `verifyAction()`
- `createManualNode()`
- Proposal / Candidate / Drift review authorities
- import preview / commit authorities

The cutover architecture is frozen in
`docs/ADR_UI_VNEXT_PRODUCTION_BINDING.md`.

Source progress now also includes:
- Canonical `issuer` / `last4` projected from production nodes without parsing free-form fields;
- conservative production surface classification;
- `identity_anchor` remains a generic identity until an explicit governed phone subtype exists;
- a consumer inventory projection with confirmed-dependency counts and pending-review/source coverage.

The inventory/impact projections are read-only seams. Separate authoritative mutation
gateways now exist for Change, Human Review and Manual Establish. None of these
switches the launcher, makes Preview writable, or claims R19 lifecycle persistence.

R22 production governance source now additionally includes:
- `AppContainerVNextReviewSource`;
- `ProductionReviewConsumerProjection`;
- `AppContainerVNextReviewActionGateway`.

Proposal / Candidate / Drift remain outside Reality until the authoritative gateway
is called. After every production review mutation the queue must be re-read; a local
UI dismissal is never sufficient.


## 12. Human Review — truth authority

R22 closes the missing visible step between discovery and Personal Reality.

```text
Observation
→ Proposal / Candidate / Drift
→ Human Review
→ Confirmed Reality
```

UI route:
- `VScreen.REVIEW`;
- label = `待复核`;
- parent = Now;
- not a sixth primary tab.

Preview has one isolated reference example for each review class and is read-only.
Production already has source/action seams but screen injection remains gated by the
VNext production cutover.

Permanent rules:
- machine confidence != truth;
- Review visibility != confirmation;
- Proposal is not a Dependency;
- Candidate is not a Node;
- Drift is not a graph mutation;
- empty Review != complete graph.

## 13. Establish / Import

R23 closes the missing **建立** surface.

Information architecture:

~~~text
Data Sources → 建立基础设施
Search → 建立基础设施
建立基础设施 → Up → Data Sources
~~~

The Preview route is explanatory/read-only and visibly states:

> 文件留在本机 · 发现不等于依赖

Production VNext must reuse the existing hardened
`FileWorkflowCoordinator → AppContainer.parseFile / previewImport / commitImport`
pipeline. R23 adds a consumer-safe production projection, but does not create a
second picker/parser or enable real file access in Preview.

Permanent import truth rule:

~~~text
Import commit != Dependency confirmation
~~~

The production import transaction can create Source/Evidence/objects and
Proposal/Candidate/Drift review work. Generated proposals remain R22 Human Review
work before they become confirmed relations.

## 14. Canonical boundary

Current Canonical schema does **not** yet define R19 card/number lifecycle fields as
cross-platform semantic fields.

Therefore R19 does not:
- add lifecycle fields to .depmap;
- write them into `PresentationProfile`;
- smuggle them through Android-only `fields_json`;
- claim lifecycle persistence is production-ready.

A future lifecycle persistence feature must go through:

```text
Spec
→ schema/migration decision
→ golden/negative fixtures
→ conformance
→ all platform runtimes
→ UI
```

## 15. Manual Establish

R24 closes the v2.3 **手工录入** design surface without inventing a mutation path.

Hierarchy:

~~~text
Data Sources
→ 建立基础设施
  → 手工记录
~~~

The Preview screen explicitly separates:
- current Canonical runtime creation set: payment_instrument / account / service;
- storage-known but not safely exposed manual-create types;
- identity_anchor, which is too coarse to infer Number vs Email.

R26 now exposes a governed source authority through
`AppContainer.createManualNode` / `GraphRepository.createManualNode`. It validates
the generated Canonical runtime-creatable policy, performs Node + graphRevision in
one transaction, and creates no Dependency.

The Preview still intentionally shows no Save button. Production screen binding and
fresh exact-head runtime evidence remain separate gates.

Permanent rule:

~~~text
confirm object exists != confirm dependency
~~~

This is a production-screen-binding HOLD, not an unfinished visual form or missing mutation authority.

## 16. Manual Relationship

R25 closes the remaining manual-graph UX gap.

Hierarchy:

~~~text
Data Sources
→ 建立基础设施
  → 手工记录
    → 手工记录关系
~~~

The reference freezes:
- From / Relation / To;
- capability;
- criticality default = unknown;
- explicit human-only required semantics;
- current runtime relation vocabulary;
- storage-known but runtime-HOLD relations;
- no degree-count shortcut for independent paths.

Preview exposes no “确认关系” mutation. R26 identified the concrete prerequisite:
the TS reference has Schema v4 widening for the full runtime relation/capability
vocabulary, while Native production schema is still v3. A partial v3-only
relationship gateway is forbidden.

Permanent provenance split:

~~~text
explicit user statement → origin=manual
source/model suggestion → Proposal → Review → origin=proposal
~~~

## 17. Tests / evidence present in source

R26 has source contracts for:
- lifecycle fixture truth/unknown behavior;
- Impact Lens unknown/evidence boundaries;
- keep-number identity;
- separate migration target;
- Change impact summary;
- payment-card Current / Transition / After semantics;
- Records completed-vs-verified projection;
- aliases/navigation hierarchy;
- five-item primary IA;
- search lifecycle/alias semantics;
- projected region label geometry;
- compact visual hierarchy;
- tablet adaptive hierarchy;
- Human Review route / Up hierarchy / Search discovery;
- Preview review authority boundary;
- production review consumer projection;
- Establish Import hierarchy/projection;
- Manual Establish route, truth boundary and no-ghost-save state;
- authoritative manual Node mutation source + device evidence test;
- generated runtime-creatable Node policy;
- Manual Relationship route, runtime relation vocabulary and unknown/required boundary;
- explicit Native Schema v4 gate for production manual relation mutation.

These tests are **present**. They are not called PASS until run on the current
exact head.

## 18. Current remaining evidence gates

```text
1. exact-head build
2. unit tests
3. conformance / app tests
4. phone runtime capture
5. tablet runtime capture
6. GPU first-frame/runtime-state proof
7. interaction checks
8. human pixel review against selected reference board
9. Android Reference Freeze decision
```

Only after Android Reference Freeze may iOS/Harmony UI translation leave HOLD.

## 19. Source/design closure documents

- `ANDROID_UI_VNEXT_R19_SOURCE_REPORT.md`
- `ANDROID_UI_VNEXT_R20_SOURCE_REPORT.md`
- `ANDROID_UI_VNEXT_R21_SOURCE_REPORT.md`
- `ANDROID_UI_VNEXT_R22_SOURCE_REPORT.md`
- `ANDROID_UI_VNEXT_R23_SOURCE_REPORT.md`
- `ANDROID_UI_VNEXT_R24_SOURCE_REPORT.md`
- `ANDROID_UI_VNEXT_R25_SOURCE_REPORT.md`
- `ANDROID_UI_VNEXT_R26_SOURCE_REPORT.md`
- `spec/ui-vnext/PDIG_VNEXT_DESIGN_CLOSURE_MATRIX.md`
- `spec/ui-vnext/CAPABILITY_AUTHORITY_MATRIX.md`
- `spec/proposals/access-recovery-factor-v1.md`
- `spec/proposals/secret-locator-v1.md`
- `spec/proposals/device-continuity-v1.md`
- `spec/proposals/digital-resource-continuity-v1.md`
- `spec/proposals/trusted-handoff-v1.md`
- `spec/ui-vnext/MANUAL_RELATIONSHIP_UX_CONTRACT.md`
- `spec/ui-vnext/MANUAL_ESTABLISH_UX_CONTRACT.md`
- `spec/ui-vnext/ESTABLISH_IMPORT_UX_CONTRACT.md`
- `spec/ui-vnext/RECORDS_AND_PAYMENT_CHANGE_CONTRACT.md`
- `spec/ui-vnext/HUMAN_REVIEW_UX_CONTRACT.md`
- `ANDROID_REFERENCE_MAPPING.md`
- `ANDROID_VISUAL_CONTRACT.md`
- `spec/ui-vnext/ASSET_CONTINUITY_UX_CONTRACT.md`
- `docs/ADR_UI_VNEXT_PRODUCTION_BINDING.md`

## 20. Identity / Recovery future-lens closure

R21 closes the **design** of the two remaining long-term lenses without exposing
ghost capabilities.

Identity:
- governed proposal complete: `spec/proposals/identity-context-v1.md`;
- membership is a reviewed grouping primitive, not a Dependency;
- Canonical schema/repository/conformance not implemented;
- visible selector = HOLD.

Recovery:
- incident/solver proposal complete: `spec/proposals/recovery-incident-mode-v1.md`;
- composes existing FailureDomain, RecoveryCycle and ProviderPolicy semantics;
- requires explicit unavailable-state authority and surviving-path solver;
- visible Recovery UI = FORBIDDEN until solver/conformance/runtime support.

Shared UX:
- `spec/ui-vnext/RECOVERY_AND_IDENTITY_LENS_UX_CONTRACT.md`;
- neither becomes a sixth/seventh primary tab;
- Android `VNextLensAvailability` makes the HOLD executable.

## 21. R26 continuity-substrate design closure

R26 closes the remaining v2.3 roadmap concepts that previously existed only as
high-level master-document vocabulary.

### Access / Recovery Factor

`spec/proposals/access-recovery-factor-v1.md` freezes:
- factors as substrate, not NodeKinds or primary UI categories;
- factor count != independent path count;
- device-bound / provider-synced / roaming / offline / human-assisted semantics;
- confirmed FactorBinding to actual Dependencies;
- enrollment != current incident availability;
- passkey/provider FailureDomain boundary;
- no secret material.

### SecretLocator

`spec/proposals/secret-locator-v1.md` freezes:
- metadata-only secret location;
- no passwords/TOTP seeds/recovery codes/private keys/seed phrases/CVV/session tokens;
- locator != factor != recovery path;
- masking/export/privacy boundary;
- no “copy secret” product behavior.

### Recovery Preparedness

`spec/ui-vnext/RECOVERY_PREPAREDNESS_UX_CONTRACT.md` freezes a future `我` child
workspace while keeping the route hidden until Factor Canonical exists.

It uses no health/safety score and never converts multiple factors into independent
recovery claims.

### Device Continuity

`spec/proposals/device-continuity-v1.md` freezes the planned v0.4
`REPLACE × device` architecture:
- passkey / TOTP / push / eSIM / password-manager continuity;
- provider-synced vs device-bound distinction;
- Make-Before-Break;
- explicit Action DAG;
- old-device retirement only after capability verification.

Reference/production UI remains hidden until prerequisites exist.

### Digital Resource Continuity

`spec/proposals/digital-resource-continuity-v1.md` freezes future v0.6+ domain,
DNS, repository, cloud/hosting/data continuity without turning PDIG into a provider
control panel or storing transfer/admin secrets.

### Trusted Handoff

`spec/proposals/trusted-handoff-v1.md` freezes future v0.7+ trusted continuity:
- provider-specific legacy/successor arrangements;
- scoped handoff;
- metadata-only instructions;
- no credential/secret escrow;
- no autonomous dead-man switch;
- no legal-will claim.

### Unified capability/authority gate

R25/R26 adds:
- `VNextCapabilityMatrix.kt`;
- `spec/ui-vnext/CAPABILITY_AUTHORITY_MATRIX.md`.

The matrix distinguishes:

```text
design complete
!= reference visible
!= production executable
```

and keeps Device Continuity, Recovery Preparedness, Digital Resource Continuity,
Identity Context, Recovery Incident and Trusted Handoff hidden until their actual
authority prerequisites exist.

### Navigation and task ownership correction

R26 also re-audited the `我` workspace against the R22 Records boundary:
- active/current attention and upcoming maintenance route to Now;
- active changes route to Change;
- Records stays evidence/history/verification;
- `我` remains the fifth primary root.

### Design closure index

The authoritative index is:
- `spec/ui-vnext/PDIG_VNEXT_DESIGN_CLOSURE_MATRIX.md`.

Within the scope and roadmap explicitly described by v2.3-R1, remaining gaps are now
classified as runtime/production/Canonical implementation work rather than ambiguous
product design.

## 22. Honest stop line

```text
SOURCE_DESIGN = COMPLETE
RUNTIME_EVIDENCE = PENDING
REFERENCE_FREEZE = HOLD
PRODUCTION_CUTOVER = HOLD
```

The next blocker is no longer “missing UI design.” It is fresh exact-head R26 runtime
verification and, after reference acceptance, production read-model binding.
