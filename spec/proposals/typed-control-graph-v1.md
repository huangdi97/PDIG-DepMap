# Typed Control Graph v1 — PDIG Dependency Semantics

> Date: 2026-10-10
> Status: **DESIGN_FROZEN / CURRENT V0.3 RUNTIME SEMANTICS PRESERVED**
>
> The machine-readable source of current values remains `spec/domain/domain.json`.
> This contract explains why PDIG is not a generic “linked objects” graph and
> constrains future relation/capability growth. It does not activate new values.

## 1. Graph statement

A PDIG confirmed Dependency means:

> **For capability C, target B depends on source A through typed relation R within
> currently confirmed Reality.**

Logical identity:

~~~text
(from, relation, to, capability)
~~~

Direction and capability are semantic, not visual decoration.

## 2. Capability-aware state

Continuity operates on:

~~~text
(nodeId, capability)
~~~

not nodeId alone.

One object can be available for communication, degraded for authentication and
unknown for recovery at the same time.

Current runtime capabilities:

~~~text
payment
access
authentication
recovery
communication
~~~

Storage vocabulary also contains identity, but storage vocabulary does not grant
Impact/runtime authority.

## 3. Current runtime relations

### funding_source

Capability: payment.

Source participates in providing payment capability to target.

### merchant_agreement

Capability: payment.

A confirmed payment relationship exists between the source and target
merchant/service/account. One observed transaction alone is not this relation.

### recovers

Capability: recovery.

Source participates in recovering target access/identity.

It does not prove that the path:
- currently works;
- is independent;
- is unique;
- is required.

### authenticates

Capability: authentication.

Source participates in authenticating to target. Authentication is not recovery.

### controls

Capability: access.

Source controls/enables target access under the current governed relation semantics.
It is not a generic ownership edge.

Allowed endpoint kinds and capabilities come from the runtime Relation registry,
never from UI assumptions.

## 4. Storage-known but runtime-disabled

Storage enum also contains:

~~~text
verifies
bound_to
~~~

These are not current runtime relations.

~~~text
DB can store value != runtime may create value
~~~

The UI, import pipeline and agents may expose only the current runtime registry.

Future/backlog relation names likewise require a shared Canonical activation path;
an Android-only dropdown cannot activate them.

## 5. Criticality

Current criticality:

~~~text
required
unknown
~~~

Machine inference may not set required.

~~~text
observed repeatedly != required
only known path != externally proven required
high confidence != required
~~~

Unknown is a first-class state, not a null to hide.

## 6. Dependency lifecycle

Logical-key lifecycle:

~~~text
first confirmed logical key
→ insert

same logical key already active
→ update/verify same row

retired logical key reappears
→ reactivate same row

never
→ create duplicate active row for same logical key
~~~

Retired edges do not propagate Impact.

## 7. Origin and confirmation

Current origin:

~~~text
manual
proposal
~~~

Origin says how a confirmed edge reached Reality.

Before confirmation:

~~~text
DependencyProposal != Dependency
~~~

Proposal confidence never participates in deterministic Impact.

## 8. Verification basis

Verification basis records why a fact is accepted.

Current vocabulary includes:

~~~text
user_confirmed
authoritative_source
~~~

Authority is fact/adapter specific. An event-stream import is not automatically
authoritative just because it came from a real provider.

## 9. DependencyGroup

Some semantics require a confirmed group of edges.

Current modes:

~~~text
ANY
ALL
~~~

### ANY

Any confirmed member may satisfy the grouped requirement.

But:

~~~text
ANY members != independent recovery paths
~~~

Members may share a FailureDomain.

### ALL

All members are jointly required where the runtime relation/group registry allows
that mode.

No UI visual cluster may silently create a group.

## 10. Group identity

Group logical key:

~~~text
targetNodeId
+ capability
+ mode
+ sorted(member logical keys)
~~~

Member order is irrelevant.

Machine discovery may create DependencyGroupProposal, not a confirmed
DependencyGroup. Grouping can materially change Impact, so confirmation is a real
authority boundary.

## 11. Path count vs independence

~~~text
pathCount != independentPathCount
~~~

Example:

~~~text
SMS recovery on phone A
Authenticator app on phone A

raw paths = 2
shared failure root may make independent paths = 1
~~~

Independence requires FailureDomain-aware analysis. UI degree counting is
forbidden.

## 12. FailureDomain is a different semantic layer

FailureDomain is not another Dependency relation.

It describes a shared failure root/context that changes how candidate paths are
interpreted.

Current runtime domain kinds:

~~~text
DEVICE
PHONE_NUMBER
ACCOUNT
PROVIDER
~~~

Future registered vocabulary does not become runtime authority automatically.

Machine inference may at most create needs_review domain status; confirmed domain
membership requires confirmed Reality/authorized evidence.

## 13. RecoveryCycle is derived

A recovery cycle is a derived property of typed confirmed relations.

Confirmed active Reality can produce confirmed_cycle.

Proposal/Candidate relations can at most produce potential/review state.

A cycle does not create an independent recovery root.

## 14. Object taxonomy is separate from relation semantics

Object kind answers:

> What is this object?

Relation/capability answers:

> What does it do for another object?

Do not encode dependency semantics only in an object's name.

Bad:

~~~text
name = "backup email for bank"
no recovers edge
~~~

Good:

~~~text
identity object
+ confirmed recovers relation
+ recovery capability
~~~

## 15. Identity subtype is separate

Generic identity_anchor must not be coerced to PHONE_NUMBER / EMAIL_ADDRESS /
USERNAME / PASSKEY because of its display value.

Subtype requires its own governed Canonical fact.

## 16. Region is separate

RegionFact is context, not a Dependency.

~~~text
card issued in HK != card depends on HK
~~~

Future FailureDomain REGION is also distinct from Region Lens membership. Sharing a
territory does not automatically prove a shared failure domain.

## 17. ProviderPolicy is separate

ProviderPolicy is Knowledge Plane.

~~~text
provider supports recovery email
!=
this account has a confirmed recovery email
~~~

ProviderPolicy cannot create a Dependency.

## 18. Impact semantics

Impact consumes typed relation/capability semantics.

It must not:
- infer relation meaning from names;
- treat all edges identically;
- propagate retired edges;
- use Proposal edges as Reality;
- infer requiredness from confidence/degree;
- ignore DependencyGroup mode.

Every new runtime relation needs an explicit Impact semantics decision.

## 19. Manual relationship UX

Manual creation should ask consumer questions such as:

~~~text
这张卡为哪个账户提供支付能力？
这个号码用于恢复哪个账户？
这台设备用于登录哪个账户？
~~~

Internally the governed relation/capability pair is written.

The default product should not expose a raw graph-edge editor.

## 20. Import / AI proposals

Ingestion may propose:

~~~text
from
relation
to
capability
evidence refs
confidence
~~~

But before acceptance:
- proposal stays outside Impact;
- confidence stays review-only;
- endpoint objects must be resolved;
- invalid relation/kind/capability combinations fail closed;
- acceptance uses the same runtime registry as manual creation.

No adapter gets a private relation vocabulary.

## 21. GraphRevision

GraphRevision versions confirmed Reality.

It bumps atomically with governed confirmed Reality mutation.

It does not bump for:
- Observation;
- Evidence summary;
- Proposal;
- Candidate;
- Drift creation;
- ChangePlan creation;
- derived Finding.

Impact/Change may therefore pin a stable Reality snapshot.

## 22. Consumer projection

Users do not maintain the graph directly.

Consumer language:

~~~text
已确认依赖
支付关系
恢复方式
验证方式
访问控制
如果它发生变化？
~~~

Avoid:

~~~text
node 17
edge 23
out-degree
betweenness
graph score
~~~

Graph is the compute substrate, not the home screen.

## 23. Extension checklist

Before activating a Relation:

~~~text
semantic sentence
allowed fromKinds / toKinds
capability
group allowed? / allowed modes?
default criticality
verification policy
Impact semantics
manual UX wording
ingestion proposal mapping
positive + negative fixtures
cross-platform conformance
migration compatibility
~~~

Before activating a Capability:

~~~text
StateKey meaning
relations that can provide it
Impact propagation
Finding semantics
Change implications
FailureDomain implications
consumer wording
fixtures/conformance
~~~

## 24. Negative invariants

~~~text
generic linked_to enters runtime                         FORBIDDEN
storage enum becomes executable via UI only              FORBIDDEN
AI creates confirmed Dependency                          FORBIDDEN
confidence sets required                                 FORBIDDEN
two edges imply independent fallback                     FORBIDDEN
visual cluster creates DependencyGroup                   FORBIDDEN
same region implies shared FailureDomain                 FORBIDDEN
ProviderPolicy creates Reality edge                      FORBIDDEN
identity-looking string creates subtype                  FORBIDDEN
retired edge propagates Impact                           FORBIDDEN
Proposal enters deterministic Impact                     FORBIDDEN
~~~

## 25. Stop line

~~~text
TYPED_CONTROL_GRAPH_DESIGN = COMPLETE
CURRENT_RUNTIME_RELATIONS = FROZEN_V0_3_SET
CAPABILITY_AWARE_STATE = FROZEN
DEPENDENCY_GROUP_SEMANTICS = FROZEN
FAILURE_DOMAIN_SEPARATION = FROZEN

GENERIC_LINKED_TO = FORBIDDEN
RUNTIME_RELATION_WIDENING = SHARED_CANONICAL_GATE
~~~
