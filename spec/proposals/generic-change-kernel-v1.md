# Generic Change Kernel v1

> Date: 2026-10-10  
> Status: **DESIGN_FROZEN / REPLACE RUNTIME EXISTS / FUTURE PRIMITIVES GATED**
>
> Purpose: freeze the product/domain architecture that lets PDIG evolve from
> “Change Phone / Change Card” into a generic high-consequence infrastructure
> change system without forking safety semantics per screen.
>
> This document does **not** activate any ChangePrimitive beyond current runtime
> `REPLACE`.

---

## 1. Kernel equation

A PDIG change is not a wizard page.

~~~text
ChangePrimitive
× typed subject
× ScenarioTemplate
× current Personal Reality
× ProviderPolicy context
× Impact snapshot
× Action prerequisite DAG
× TemporalChange
× Verification
× graphRevision
=
ChangePlan
~~~

UI is a projection/controller over this kernel. It is not the state machine owner.

---

## 2. Current primitive registry

Canonical vocabulary:

~~~text
REPLACE
LOSE
COMPROMISE
MIGRATE
PORT
SUSPEND
~~~

Current runtime:

~~~text
REPLACE only
~~~

Current production scenarios:

~~~text
replace_payment_card
replace_phone_number
~~~

Permanent gate:

~~~text
registered vocabulary != executable capability
~~~

No button, route or agent may execute LOSE / COMPROMISE / MIGRATE / PORT / SUSPEND
until its ScenarioTemplate, domain semantics, fixtures, conformance and runtime
authority exist.

---

## 3. Primitive semantics

### REPLACE

Intent:

> replace one infrastructure object/path while preserving required capabilities.

Typical safety model:

~~~text
old path
→ establish replacement
→ verify replacement
→ migrate confirmed dependencies
→ confirm recovery/continuity
→ retire old path
~~~

Make-Before-Break applies where retiring the old path can remove access/recovery.

### LOSE

Intent:

> model an already-lost/unavailable object/path.

This is incident input, not a normal replacement wizard.

Requires:
- explicit unavailable-state authority;
- surviving-root analysis;
- FailureDomain-aware Recovery Solver.

Current status: **NON_EXECUTABLE**.

### COMPROMISE

Intent:

> a path/account/factor may no longer be trusted.

Requires:
- compromise incident authority;
- scope of compromise;
- rotate/revoke semantics;
- provider constraints;
- verification of new trustworthy paths.

Current status: **NON_EXECUTABLE**.

### MIGRATE

Intent:

> move a digital resource/control relationship between environments/providers.

Requires resource-specific Canonical semantics. Generic “move” is insufficient.

Current status: **NON_EXECUTABLE**.

### PORT

Intent:

> preserve an identity/resource while moving its provider/controller.

Examples may include telecom/provider/domain-style future scenarios.

Identity continuity and provider policy become first-class.

Current status: **NON_EXECUTABLE**.

### SUSPEND

Intent:

> intentionally make a capability/path temporarily unavailable without deleting
> or retiring its underlying Reality.

Requires temporal/resume semantics and clear distinction from failure.

Current status: **NON_EXECUTABLE**.

---

## 4. ScenarioTemplate contract

A ScenarioTemplate binds one primitive to an allowed subject and capability set.

Conceptual shape:

~~~text
ScenarioTemplate
  id
  primitive
  category
  availability
  subjectKind
  subjectSubtype?
  affectedCapabilities[]
  requiredInputs[]
  impactScenario
  actionTemplate[]
  verificationPolicy
  providerPolicySelectors[]
  temporalPolicy?
  retirementPolicy?
  version
~~~

A ScenarioTemplate is product/domain code, not user Reality.

A template may produce a ChangePlan only when:
- primitive is runtime-enabled;
- subject kind/subtype matches;
- required inputs are available;
- production authority exists.

---

## 5. ChangePlan is an intent snapshot, not Reality

Current invariant remains:

~~~text
ChangePlan != Personal Reality
~~~

A plan records:
- what the user intends to change;
- the target object;
- analyzed graph revision;
- impact snapshot;
- required/review items;
- actions and prerequisites;
- verification states;
- projection of a successful future state.

Creating a plan does not mutate the infrastructure graph.

---

## 6. Graph revision and revalidation

Every authoritative impact snapshot is pinned to a graph revision.

~~~text
plan.lastAnalyzedGraphRevision = R
current graphRevision = R+n
AND plan not terminal
→ effectiveStatus = needs_revalidation
~~~

`needs_revalidation` is derived and is never persisted by rewriting workflowState.

Rules:
- completed/cancelled plans remain historical;
- stale non-terminal plans cannot claim readiness from an old graph;
- reanalysis must preserve completed/verified action history where semantically
  compatible;
- if prerequisite semantics materially changed, the plan returns to explicit
  review/blocked state.

---

## 7. Impact snapshot

Plan creation/reanalysis uses the authoritative Impact engine.

Snapshot must distinguish:

~~~text
must_change
backup_path
degraded
needs_review
unaffected
unknown scope
~~~

No scenario may derive required work from:
- Proposal confidence;
- degree count;
- UI heuristics;
- ProviderPolicy alone.

The plan keeps enough reason/evidence context to explain why an action exists.

---

## 8. Readiness

Current readiness remains exactly:

~~~text
blocked
review_required
ready_with_known_scope
~~~

Forbidden synonyms:

~~~text
safe
fully_safe
all_clear
100%
~~~

Readiness must be derived from explicit requirements/impact resolution, never:

~~~text
total - resolved = ready
~~~

`ready_with_known_scope` means known required work is resolved for the pinned
scope. It does not assert complete knowledge of the user's external world.

---

## 9. Action DAG

Every action has:
- stable id;
- phase: prepare / change / verify;
- prerequisites;
- status;
- verification method/status;
- optional provider/temporal constraint.

The DAG must be deterministic and acyclic.

Rejected conditions:

~~~text
cycle
missing prerequisite
unknown action
~~~

A UI list order cannot substitute for prerequisite semantics.

---

## 10. Action state vs verification state

Action execution and verification are separate dimensions.

~~~text
action done
!=
verified
~~~

Verification:

~~~text
not_required
pending
evidence_suggested
verified
failed
~~~

Rules:
- completion may make a verification task possible;
- future observation may suggest evidence;
- evidence_suggested is not verified;
- verified/failed/not_required cannot be overwritten by incidental evidence;
- retirement gates must read authoritative verification.

---

## 11. Make-Before-Break

For high-consequence replacement:

~~~text
establish new path
→ verify new path
→ prove required continuity condition
→ allow retirement of old path
~~~

Forbidden:

~~~text
retire old path
before required replacement verification
~~~

This is a kernel invariant, not a button-disable convention.

The UI must expose the blocked reason in text.

---

## 12. TemporalChange

Change can have real waiting windows.

Authoritative timestamps may include:

~~~text
effectiveAt
verificationNotBefore
verificationDueAt
retireOldPathAfter
~~~

Order invariant:

~~~text
effectiveAt
<= verificationNotBefore
<= verificationDueAt
<= retireOldPathAfter
~~~

Time passing:
- can change a derived temporal phase;
- cannot mark an action completed;
- cannot mark verification successful.

Consumer language may include:

~~~text
等待生效
可开始验证
验证窗口中
等待服务商
验证逾期
可停用旧路径
仍被 Make-Before-Break 阻断
~~~

---

## 13. ProviderPolicy role

ProviderPolicy can constrain/suggest:
- required wait window;
- supported procedure;
- provider-specific instruction;
- verification timing;
- template variant.

It cannot prove:
- the user configured that feature;
- a dependency exists;
- an identity subtype;
- a completed action.

~~~text
Provider supports X != User configured X
~~~

A policy revision can force revalidation of a plan/template interpretation without
rewriting Personal Reality.

---

## 14. Current / Transition / After

All Change UI must retain three distinct projections.

### Current

Confirmed Reality before plan effects.

### Transition

The plan's in-progress state:
- old and new may coexist;
- some services migrated;
- verification may be pending;
- old path may remain required.

### After

~~~text
Plan Projection
~~~

It is the expected state **if the plan succeeds**.

After must never say:
- migration completed;
- Reality already changed;
- verified;
unless authoritative Reality/verification records say so.

---

## 15. Plan creation / mutation API boundary

Production UI must delegate:

~~~text
createPlan
completeAction
verifyAction
→ domain/AppContainer
→ authoritative transaction
→ re-read planDetail
~~~

Compose must not:
- create a fake local plan;
- toggle done;
- toggle verified;
- infer readiness;
- synthesize a new graph edge.

Preview may demonstrate geometry/state vocabulary but cannot execute Production
authority.

---

## 16. Scenario Factory

Future expansion should be declarative where possible.

Conceptual factory:

~~~text
primitive + subject type/subtype
→ eligible templates

template + subject + user inputs + current graph revision
→ impact analysis
→ action DAG
→ ChangePlan
~~~

The factory must fail closed when:
- subtype is unknown;
- primitive is not runtime-enabled;
- required provider policy is unavailable;
- target node does not match;
- required capability semantics are missing.

No “best effort” fallback to a generic checklist for high-consequence changes.

---

## 17. Replace Phone contract

Existing REPLACE phone flow remains the reference vertical slice.

Core invariant:

~~~text
old active number
!= keep-number asset
!= new migration target
~~~

Typical action topology:

~~~text
analyze impact
→ establish new number
→ verify new number
→ migrate confirmed critical bindings
→ review/establish independent recovery
→ retire old number
~~~

If identity subtype is not Canonical in Production, a generic identity_anchor may
not enter this scenario merely because its name looks like a phone number.

---

## 18. Replace Payment Card contract

The same kernel applies:

~~~text
analyze confirmed payment/service dependencies
→ establish replacement payment asset
→ migrate required bindings
→ verify provider/service updates
→ retire old card/path when allowed
~~~

Card appearance customization is unrelated Presentation state and must not enter the
ChangePlan.

Lifecycle due/expiry may become a **trigger** after Maintenance Canonical exists;
it does not change the replacement safety kernel.

---

## 19. Incident primitives

LOSE and COMPROMISE are not just REPLACE with different labels.

They begin from a changed/unavailable trust state:

~~~text
incident authority
→ surviving capability analysis
→ recovery/rotation plan
→ verification
→ Reality repair
~~~

They require Recovery Incident / Factor / FailureDomain inputs and remain hidden.

---

## 20. Resource migration primitives

MIGRATE / PORT require resource-specific identity invariants.

Examples:

~~~text
domain registration identity must survive registrar transfer
repository ownership must survive org migration
phone identity may survive carrier port
cloud workload may move region/provider while service identity persists
~~~

The kernel therefore needs explicit:

~~~text
identity preserved?
controller/provider changes?
capabilities that must remain?
data/state transfer?
provider policy?
rollback?
verification?
~~~

No universal “move resource” action can safely cover them all.

---

## 21. Cancellation and rollback

Cancel means:

> stop executing this ChangePlan.

It does not automatically undo actions already performed.

Future rollback design must distinguish:
- reversible action;
- compensating action;
- irreversible external action;
- provider waiting window;
- already verified Reality change.

Never render “Undo plan” unless every affected action has a governed reversal.

---

## 22. Records

Records shows what actually happened:
- action completion;
- verification result;
- plan cancellation/completion;
- evidence references;
- Reality mutations.

It does not record an After Projection as an occurred event.

A plan being created/analyzed is intent/audit, not proof that infrastructure changed.

---

## 23. Cross-platform conformance

Every executable new primitive needs:
- template fixture;
- allowed/forbidden subject fixtures;
- Impact expected result;
- deterministic Action DAG;
- readiness expected;
- verification transitions;
- graphRevision staleness/rebase;
- temporal gates when used;
- ProviderPolicy revision;
- Make-Before-Break negative case;
- Preview-no-authority test;
- Production mutation + authoritative re-read test.

Minimum generic negatives:

~~~text
future primitive exposed without runtime enablement       FORBIDDEN
wrong subtype enters scenario                             FORBIDDEN
Proposal creates must_change                              FORBIDDEN
stale plan remains ready                                  FORBIDDEN
done becomes verified                                     FORBIDDEN
old path retires before required verification             FORBIDDEN
ProviderPolicy creates user configuration                 FORBIDDEN
After Projection becomes Reality                          FORBIDDEN
UI-local mutation accepted as ChangePlan truth            FORBIDDEN
~~~

---

## 24. Capability gate

Product/UI capability matrix must be the final exposure gate.

~~~text
design complete
!= reference visible
!= production executable
~~~

Adding a design document for a new primitive must never automatically add a button.

---

## 25. Stop line

~~~text
GENERIC_CHANGE_KERNEL_DESIGN = COMPLETE
REPLACE_KERNEL = CURRENT_RUNTIME
REPLACE_PHONE = EXECUTABLE_WHERE_SUBTYPE_AUTHORITY_EXISTS
REPLACE_PAYMENT_CARD = EXECUTABLE
LOSE = GATED
COMPROMISE = GATED
MIGRATE = GATED
PORT = GATED
SUSPEND = GATED

DONE_NOT_VERIFIED = FROZEN
MAKE_BEFORE_BREAK = FROZEN
AFTER_IS_PLAN_PROJECTION = FROZEN
PROVIDER_POLICY_NOT_REALITY = FROZEN
~~~
