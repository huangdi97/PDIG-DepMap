# PDIG R35 — Canonical Expansion Execution Closure Report

> 2026-10-10 · branch: feat/android-ui-vnext-translation
>
> R35 does not redesign Android pixels and does not activate a new Canonical
> schema. It closes the execution-order gap between R34's individually complete
> future proposals.

---

## 0. Status

~~~text
ANDROID_UI_VNEXT_SOURCE = R34
PDIG_PRODUCT_ARCHITECTURE_CONTROL = R35

V2_3_PRODUCT_UX_DESIGN = CLOSED
FIVE_PRIMARY_IA = FROZEN
CANONICAL_V4_EXPANSION_DAG = DESIGN_FROZEN
CANONICAL_V4_DAG_VALIDATOR = IMPLEMENTED
CANONICAL_V4_DAG_CI_GATE = IMPLEMENTED

CURRENT_APP_SCHEMA_VERSION = 3
CURRENT_GRAPH_PAYLOAD_VERSION = 3
NEW_CANONICAL_SCHEMA_ACTIVATED_BY_R35 = NO
NEW_DEPMAP_PAYLOAD_ACTIVATED_BY_R35 = NO

ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_RELEASE_CUTOVER = HOLD
~~~

---

## 1. Problem R35 closes

R34 had complete design documents for the major future capabilities, but they were
still separate files.

That left a coordination risk:

~~~text
valid future proposal A
valid future proposal B
valid future proposal C

does not automatically define:

which must ship first?
which may run in parallel?
which is optional composition?
which UI gate may open when?
~~~

For a multi-platform local-first system with strict Reality authority, this is not
just project management. Wrong ordering can create semantic forks.

R35 therefore makes the future Canonical implementation order machine-readable.

---

## 2. New machine source

New:

~~~text
spec/roadmap/canonical-v4-expansion-plan.json
~~~

It describes:

- current v3 baseline;
- permanent safety invariants;
- activation stages;
- hard dependencies;
- optional composition dependencies;
- proposal/contract owner for every package;
- required Canonical artifacts;
- consumer capabilities each package unlocks;
- UI visibility policy;
- the permanent newPrimaryDestination = false rule.

Packages covered:

~~~text
typed_control_graph
generic_change_kernel
identity_anchor_subtype
region_fact
asset_lifecycle_maintenance
secret_locator
identity_context
access_recovery_factor
maintenance_runtime
device_continuity
recovery_preparedness
recovery_incident
advanced_continuity_analysis
digital_resource_continuity
trusted_handoff
~~~

No v2.3 future product family is left as an unordered design island.

---

## 3. New machine gate

New:

~~~text
tools/validate-canonical-v4-expansion-plan.mjs
~~~

The validator fails when:

- package IDs collide;
- a stage is invalid;
- a hard/optional dependency points to an unknown package;
- a package hard-depends on a later stage;
- the hard-dependency graph contains a cycle;
- a proposal/contract path does not exist;
- a package omits required Canonical artifacts;
- a package tries to create a new primary destination;
- key safety invariants disappear;
- a future package overclaims Canonical/runtime implementation.

The validator also pins the five-primary product shell:

~~~text
NOW / INFRASTRUCTURE / CHANGE / RECORDS / ME
~~~

---

## 4. CI integration

The main Canonical CI job now runs:

~~~text
CODEGEN GATE
→ CANONICAL V4 EXPANSION DAG GATE
→ Conformance orchestrator
~~~

Therefore future edits to the roadmap are reviewed as executable architecture
constraints rather than prose-only intent.

This gate does not claim the future packages are implemented. It only prevents
invalid ordering/authority claims.

---

## 5. Activation order

### Stage 0 — existing v3 baseline

~~~text
Typed Control Graph
REPLACE-only Generic Change Kernel
Evidence / Proposal / Review boundary
FailureDomain / RecoveryCycle primitives
~~~

### Stage 1 — parallel foundations

~~~text
Identity Anchor Subtype
RegionFact
Asset Lifecycle / Maintenance
SecretLocator
~~~

### Stage 2 — semantic substrate

~~~text
Access / Recovery Factor
Identity Context
~~~

### Stage 3 — living maintenance and device change

~~~text
Maintenance runtime
Device Continuity
~~~

### Stage 4 — recovery / advanced continuity

~~~text
Recovery Preparedness
Recovery Incident
Advanced Continuity Analysis
~~~

### Stage 5 — digital resources

~~~text
Digital Resource Continuity
~~~

### Stage 6 — extreme continuity

~~~text
Trusted Handoff
~~~

This is a dependency order, not a promise that every stage is a single release.

---

## 6. Recommended first implementation

R35 recommends the first real Canonical expansion vertical slice be:

~~~text
Identity Anchor Subtype
~~~

Reasons:

1. Production already contains generic identity_anchor objects.
2. Preview already has mature Number and Email product identities.
3. replace_phone_number already exists as a production scenario.
4. Current Production correctly refuses to infer phone/email subtype.
5. The subtype closes a real reference-vs-Reality gap without adding a new object
   family or widening graph relations.

Required migration remains conservative:

~~~text
existing identity_anchor
→ subtype = unknown/null
~~~

No automatic regex/name/provider classification.

RegionFact, Maintenance and SecretLocator may proceed in parallel after the shared
versioning strategy is chosen.

---

## 7. Why Factor comes later

Factor is not just another form field.

A factor such as SMS OTP or recovery email needs a governed carrier identity.
Therefore:

~~~text
Access / Recovery Factor
hard-depends on
Identity Anchor Subtype
~~~

This prevents a generic identity string from silently becoming a phone/email factor.

Factor count also never becomes independent recovery path count.

---

## 8. Why Recovery remains gated

Recovery Incident is intentionally not exposed merely because its UX is designed.

It still requires:

~~~text
explicit incident authority
factor availability
FailureDomain-aware surviving roots
RecoveryCycle handling
provider/temporal constraints
deterministic solver
authoritative actions / verification
~~~

Only then may Recovery Lens become visible.

R35 preserves:

~~~text
DESIGN_COMPLETE != PRODUCTION_EXECUTABLE
~~~

---

## 9. No big-bang v4

R35 explicitly rejects a single oversized future migration containing every proposal.

“v4” is treated as the next Canonical expansion generation, while physical schema
versions remain monotonic and can advance package by package.

A package should ship as the smallest coherent vertical slice with:

~~~text
schema/migration
+ fixtures
+ conformance
+ all-platform semantic readers/writers
+ consumer projection
+ real product use
~~~

This reduces rollback and cross-platform compatibility risk.

---

## 10. UI permanence

Future Canonical growth does not create new root navigation.

~~~text
现在 / 基础设施 / 变更 / 记录 / 我
~~~

Mapping stays contextual:

- RegionFact → Globe / Infrastructure
- Maintenance → Now / detail / Records
- Identity Context → Infrastructure / Me
- Factor → detail / Impact
- Recovery Preparedness → Me child
- Device Continuity → detail / Change
- Recovery Incident → contextual incident flow
- Digital Resources → Infrastructure / Change
- Trusted Handoff → Me child

The validator rejects any package declaring a new primary destination.

---

## 11. Remaining gates after R35

R35 closes implementation-order design, not implementation itself.

Still real:

~~~text
Canonical package implementation
cross-platform migration/conformance
Android exact-head runtime/pixels
tablet runtime/pixels
GPU runtime evidence
human visual acceptance
Android Reference Freeze
Production release cutover
iOS/Harmony platform translation after Android freeze
real-user/real-data/store gates when reopened
~~~

---

## 12. Stop line

~~~text
R35_FUTURE_CANONICAL_ORDER = CLOSED
R35_EXECUTION_GRAPH = MACHINE_READABLE
R35_EXECUTION_GRAPH_CI_GATE = IMPLEMENTED

NO_BIG_BANG_SCHEMA = REQUIRED
NO_ANDROID_ONLY_CANONICAL = REQUIRED
NO_GHOST_UI_BEFORE_AUTHORITY = REQUIRED
FIVE_PRIMARY_IA = PERMANENT

DESIGN_COMPLETE != IMPLEMENTED
IMPLEMENTED != CONFORMANCE_PASS
CONFORMANCE_PASS != RUNTIME_VERIFIED
~~~
