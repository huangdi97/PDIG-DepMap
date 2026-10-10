# PDIG Canonical v4 Expansion — Execution Plan

> Date: 2026-10-10
> Status: **R35 DESIGN FROZEN / MACHINE-GATED IMPLEMENTATION ORDER**
> Machine source: spec/roadmap/canonical-v4-expansion-plan.json
>
> This document does not activate a new schema version by itself. It freezes the
> dependency order for future Canonical expansion so an implementation agent cannot
> jump from a valid UI/reference idea directly to Android-only persistence.

---

## 0. Why this plan exists

By R34, the major v2.3 product concepts already have complete proposals:

~~~text
Identity subtype
RegionFact
Asset lifecycle / maintenance
Identity Context
Access / Recovery Factor
SecretLocator
Device Continuity
Recovery Preparedness
Recovery Incident
Advanced Continuity Analysis
Digital Resource Continuity
Trusted Handoff
~~~

Separate proposal documents can still fail operationally:

~~~text
Agent sees future UI
→ implements local Android store
→ adds screen
→ later discovers the semantic prerequisite did not exist
~~~

R35 therefore freezes a dependency DAG.

Permanent rule:

~~~text
Product design can be parallel.
Canonical activation cannot ignore prerequisites.
~~~

---

## 1. Current baseline

Current machine-readable Canonical remains:

~~~text
appSchemaVersion = 3
graphPayloadVersion = 3
ChangePrimitive runtime = REPLACE
primary IA = NOW / INFRASTRUCTURE / CHANGE / RECORDS / ME
~~~

Existing production substrate already includes typed Dependency, DependencyGroup,
FailureDomain, RecoveryCycle, ProviderPolicy, TemporalChange, Action DAG,
ChangePlan, Proposal/Candidate/Drift, GraphRevision and the current review/manual
authority paths.

R35 does not rewrite this baseline.

---

## 2. Permanent activation chain

Every future persisted capability must go through:

~~~text
proposal/design approval
→ machine-readable Canonical spec
→ schema/version decision
→ migration
→ golden fixtures
→ negative/boundary fixtures
→ codegen if shared enums/constants change
→ core/runtime implementation
→ cross-platform conformance
→ consumer projection
→ UI exposure
→ runtime evidence
~~~

Forbidden inversions:

~~~text
UI → local table → later spec                 FORBIDDEN
Android fields_json convention → Canonical    FORBIDDEN
Preview fixture → Production Reality          FORBIDDEN
Provider support → user configuration         FORBIDDEN
LLM extraction → confirmed Reality            FORBIDDEN
~~~

---

## 3. Hard dependency vs optional composition

A hard dependency means the downstream semantic package cannot be activated safely
without it.

Example:

~~~text
Access / Recovery Factor
HARD DEPENDS ON
Identity Anchor Subtype
~~~

Reason: an SMS/email recovery factor must not gain phone/email semantics from the
shape of a generic identity string.

An optional composition means both packages can exist independently but produce
richer queries together.

Example:

~~~text
Identity Context
OPTIONALLY COMPOSES WITH
RegionFact
~~~

A Work context can exist without a region. Region × Identity is a later query
intersection. Optional edges never become hidden hard blockers.

---

## 4. Activation stages

### Stage 0 — existing v3 baseline

Already governed:

~~~text
Typed Control Graph
Generic Change Kernel — REPLACE only
Evidence / Proposal / Review boundary
FailureDomain / RecoveryCycle primitives
~~~

### Stage 1 — parallel semantic foundations

Four packages may be implemented independently after a shared version/migration
strategy is chosen.

**Identity Anchor Subtype** unlocks production Number/Email surfaces and real
replace_phone_number subjectSubtype enforcement. Existing identity anchors migrate
to subtype = unknown; no regex/name/provider inference is allowed.

**RegionFact** unlocks production Globe geography, region-scoped Infrastructure and
future Region × Identity queries. Currency/provider/IP never creates a RegionFact.

**Asset Lifecycle / Maintenance** unlocks authoritative annual-fee, billing,
payment-due, number keep-alive and freshness checkpoints. Time passing never means
completion.

**SecretLocator** records only where protected material exists. Locator is never
the secret itself.

### Stage 2 — factor substrate and governed grouping

**Access / Recovery Factor** hard-depends on Typed Control Graph and Identity Anchor
Subtype. It unlocks portability, carrier semantics, FactorBinding, factor-aware
FailureDomain analysis, Device Continuity and Recovery substrate.

Permanent rule:

~~~text
factor count != independent path count
~~~

**Identity Context** depends only on the existing Reality substrate. Identity
Subtype and RegionFact are optional enrichments, not artificial blockers.
Membership never creates a Dependency.

### Stage 3 — Maintain activation and Device Continuity

**Maintenance runtime** turns governed facts/schedules into occurrences, Timeline,
Now tasks, Object Detail and Records after explicit completion.

**Device Continuity** hard-depends on Generic Change Kernel + Factor substrate.
The target vertical slice is replace_device:

~~~text
enumerate confirmed carried factors
→ establish replacement device/factors
→ verify
→ migrate/restore
→ prove continuity
→ retire old device only when allowed
~~~

### Stage 4 — Recovery and advanced analysis

**Recovery Preparedness** is pre-incident maintenance under Me. It requires Factor,
SecretLocator and FailureDomain-aware analysis and has no global score.

**Recovery Incident** requires explicit incident authority, current factor
availability, FailureDomain-aware surviving roots, RecoveryCycle, provider/temporal
constraints, a deterministic solver and an authoritative action/verification path.

Only then may Recovery Lens become visible.

**Advanced Continuity Analysis** adds generic Blast Radius, bounded Minimal Cut Set,
shared-failure-domain findings, freshness findings and unknown-critical-path
findings. These feed Impact / Findings / Recovery; they do not create a graph-score
dashboard.

### Stage 5 — Digital Resource Continuity

Prerequisites:

~~~text
Typed Control Graph
Generic Change Kernel
Factor substrate
SecretLocator
~~~

Optional enrichments are RegionFact, Identity Context and Advanced Continuity.

Target resource families include domain/registrar, DNS, source repository /
organization, cloud account/project, hosting/deployment, data store/backup and API
application.

New relations/capabilities must use the same activation checklist as the existing
graph. Generic linked_to remains forbidden.

### Stage 6 — Trusted Handoff

Trusted Handoff is deliberately last. Minimum hard substrate is governed Reality +
SecretLocator. It may compose with Identity Context, Digital Resources, Factors,
Maintenance and provider arrangements.

It never becomes password escrow, a legal-will engine, autonomous death detection
or a server dead-man switch by default.

Entry remains under Me.

---

## 5. Recommended product order

The DAG permits parallel work, but the recommended order is:

~~~text
1. Identity Anchor Subtype
2. RegionFact + Asset Lifecycle + SecretLocator in parallel
3. Access / Recovery Factor
4. Identity Context
5. Maintenance runtime
6. Device Continuity
7. Advanced Continuity Analysis
8. Recovery Preparedness
9. Recovery Incident Solver
10. Digital Resource Continuity
11. Trusted Handoff
~~~

Identity Subtype is first because Production already has generic identity_anchor
objects, Preview already demonstrates rich Number/Email experiences, and
replace_phone_number already exists. It closes the largest reference-vs-Reality
identity gap without inventing a new object family.

---

## 6. Versioning strategy

Do not pre-assign every future package to one giant physical migration.

Preferred rule:

~~~text
first persisted package
→ allocate the next schema/payload version according to migration policy

later persisted package
→ allocate another monotonic version if storage/payload changes again

runtime/query-only change over governed fields
→ no fake schema bump
~~~

“v4” in this roadmap means the next Canonical expansion generation, not a promise
that every future concept ships in one big-bang DB migration.

A release should contain the smallest coherent vertical slice that has migration,
cross-platform readers/writers, fixtures/conformance and a consumer use case.

---

## 7. Per-package Definition of Done

A Canonical package can be called implemented only when all applicable items are
true:

~~~text
[ ] proposal approved
[ ] domain.json / logical schema updated
[ ] migration defined
[ ] old fixtures unchanged
[ ] positive fixtures added
[ ] negative/boundary fixtures added
[ ] codegen regenerated/check passes
[ ] Android semantic implementation
[ ] iOS semantic implementation
[ ] Harmony semantic implementation
[ ] Desktop/host parity where applicable
[ ] conformance expected outputs
[ ] old regression remains unchanged
[ ] authority/review boundary tested
[ ] export/import deterministic
[ ] consumer projection implemented
[ ] UI gate opened only after authority
[ ] runtime evidence captured
~~~

No platform may mark a package implemented on behalf of another platform.

---

## 8. Release trains

### Train A — Governed Identity

~~~text
Identity Subtype
→ Production Number/Email
→ subjectSubtype enforcement
→ Review/manual creation
~~~

### Train B — Living Assets

~~~text
RegionFact + Maintenance
→ real production Globe regions
→ real card/number maintenance
→ Timeline / Now / Records
~~~

### Train C — Factor-aware Continuity

~~~text
Factor + SecretLocator + FailureDomain
→ Preparedness
→ Device Continuity
~~~

### Train D — Incident Recovery

~~~text
advanced analysis + incident authority + solver
→ Recovery Lens
→ LOSE / COMPROMISE scenarios only after primitive activation
~~~

### Train E — Digital Infrastructure Expansion

~~~text
Digital Resources
→ MIGRATE / PORT scenario families
→ scoped Trusted Handoff
~~~

---

## 9. UI permanence

Canonical expansion does not change the five-primary shell:

~~~text
现在 / 基础设施 / 变更 / 记录 / 我
~~~

New capabilities enter contextually:

| Capability | UI entry |
| --- | --- |
| RegionFact | Globe / Infrastructure |
| Maintenance | Now / object detail / Records |
| Identity Context | Infrastructure / Me Lens |
| Factor | object detail / Impact |
| Recovery Preparedness | Me child |
| Device Continuity | object detail / Change |
| Recovery Incident | contextual incident entry |
| Digital Resources | Infrastructure + Change |
| Trusted Handoff | Me child |

No package in the machine-readable DAG may set newPrimaryDestination = true.

---

## 10. Machine gate

Run:

~~~bash
node tools/validate-canonical-v4-expansion-plan.mjs
~~~

The gate validates:
- unique package IDs;
- valid stages;
- valid dependency references;
- hard-dependency stage ordering;
- no hard-dependency cycles;
- proposal/contract paths exist;
- every package declares Canonical artifacts;
- all packages preserve the five-primary IA;
- key safety invariants remain explicit;
- future packages do not overclaim Canonical/runtime completion.

The Canonical CI job runs this validator immediately after codegen.

---

## 11. Stop line

~~~text
R35_CANONICAL_EXPANSION_DEPENDENCY_GRAPH = DESIGN_FROZEN
R35_MACHINE_VALIDATION = IMPLEMENTED
BIG_BANG_V4 = REJECTED
ANDROID_ONLY_CANONICAL = FORBIDDEN
SIXTH_PRIMARY_TAB = FORBIDDEN

PROPOSAL_COMPLETE != CANONICAL_IMPLEMENTED
CANONICAL_IMPLEMENTED != CROSS_PLATFORM_CONFORMANCE
CONFORMANCE_PASS != RUNTIME_VERIFIED
~~~
