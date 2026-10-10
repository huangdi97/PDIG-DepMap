# ANDROID_UI_VNEXT_CURRENT_REALITY.md

> **Current reality · 2026-10-10 · R23**
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
ANDROID_UI_VNEXT_SOURCE = R23
ANDROID_LIGHT_REFERENCE_DIRECTION = DESIGN_FROZEN
ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_READ_MODEL = SOURCE_IMPLEMENTED
PRODUCTION_VNEXT_CUTOVER = HOLD

CANONICAL_SCHEMA_CHANGE_FOR_R19_UI = NONE
DEPMAP_PAYLOAD_CHANGE_FOR_R19_UI = NONE

FRESH_R23_BUILD = NOT_RUN
FRESH_R23_UNIT_TESTS = NOT_RUN
FRESH_R23_INSTRUMENTATION = NOT_RUN
FRESH_R23_PHONE_PIXELS = NOT_RUN
FRESH_R23_TABLET_PIXELS = NOT_RUN
FRESH_R23_HUMAN_ACCEPTANCE = NOT_RUN
```

Do not reuse pre-R19 screenshots or old PASS statements as proof of the current
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
| Exact-head GPU runtime proof | **NOT_RUN on current R23 head** |

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

The cutover architecture is frozen in
`docs/ADR_UI_VNEXT_PRODUCTION_BINDING.md`.

Source progress now also includes:
- Canonical `issuer` / `last4` projected from production nodes without parsing free-form fields;
- conservative production surface classification;
- `identity_anchor` remains a generic identity until an explicit governed phone subtype exists;
- a consumer inventory projection with confirmed-dependency counts and pending-review/source coverage.

These are read-only seams. They do not switch the launcher or claim R19 lifecycle persistence.

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

## 15. Tests / evidence present in source

R22 has source contracts for:
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
- production review consumer projection.

These tests are **present**. They are not called PASS until run on the current
exact head.

## 16. Current remaining evidence gates

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

## 17. Source/design closure documents

- `ANDROID_UI_VNEXT_R19_SOURCE_REPORT.md`
- `ANDROID_UI_VNEXT_R20_SOURCE_REPORT.md`
- `ANDROID_UI_VNEXT_R21_SOURCE_REPORT.md`
- `ANDROID_UI_VNEXT_R22_SOURCE_REPORT.md`
- `ANDROID_UI_VNEXT_R23_SOURCE_REPORT.md`
- `spec/ui-vnext/ESTABLISH_IMPORT_UX_CONTRACT.md`
- `spec/ui-vnext/RECORDS_AND_PAYMENT_CHANGE_CONTRACT.md`
- `spec/ui-vnext/HUMAN_REVIEW_UX_CONTRACT.md`
- `ANDROID_REFERENCE_MAPPING.md`
- `ANDROID_VISUAL_CONTRACT.md`
- `spec/ui-vnext/ASSET_CONTINUITY_UX_CONTRACT.md`
- `docs/ADR_UI_VNEXT_PRODUCTION_BINDING.md`

## 18. Identity / Recovery future-lens closure

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

## 19. Honest stop line

```text
SOURCE_DESIGN = COMPLETE
RUNTIME_EVIDENCE = PENDING
REFERENCE_FREEZE = HOLD
PRODUCTION_CUTOVER = HOLD
```

The next blocker is no longer “missing UI design.” It is fresh exact-head R23 runtime
verification and, after reference acceptance, production read-model binding.
