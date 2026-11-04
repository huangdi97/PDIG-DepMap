# Identity Context v1 — Canonical Proposal

> Date: 2026-10-09  
> Status: **PROPOSED_SCHEMA / DESIGN_COMPLETE / NOT_IMPLEMENTED**
>
> Identity Context is a governed grouping/query primitive for PDIG.
> It is **not** a NodeKind, not a Dependency, not a PresentationProfile and not a
> replacement for Region.
>
> No current `.depmap` version is changed by this proposal.

## 1. Product question

Identity Context answers:

> **“这一组真实数字基础设施，共同构成我生活中的哪个身份/角色？”**

Examples are user concepts, not inferred facts:

```text
中国主身份
英国金融身份
工作身份
旅行身份
开发者身份
隐私身份
```

The same Card / Number / Account / Email / Device / Service may belong to more than
one context.

A context references existing Reality objects. It never copies them.

## 2. Why this is not a graph edge

Membership means:

> “这个对象对这个生活身份有意义。”

It does **not** mean:

> “A technically depends on B.”

Therefore:

```text
IdentityContext membership != Dependency
IdentityContext membership != recovery path
IdentityContext membership != FailureDomain
IdentityContext membership != region
```

A UI may visually group context members, but graph algorithms must continue to use
confirmed Dependency/FailureDomain truth.

## 3. Proposed semantic object

Conceptual shape:

```text
IdentityContext
  id
  name
  purpose?
  state
  objectRefs[]
  regionScope?
  evidenceRefs[]
  source
  confirmedAt?
  createdAt
  updatedAt
```

Recommended state:

```text
proposal
confirmed
archived
```

A membership itself needs reviewability. Two viable schema forms:

### Option A — membership embedded in context

```text
IdentityContext.objectRefs[]
```

Simple, but weak provenance per member.

### Option B — first-class membership

```text
IdentityContextMembership
  id
  contextId
  objectRef
  state
  source
  evidenceRefs[]
  confirmedAt?
```

**Recommendation: Option B.**

Why:
- an AI/importer can propose one member without changing other members;
- one rejected proposal does not invalidate the context;
- provenance is inspectable per member;
- multi-source reconciliation is possible;
- membership history can be audited without turning Context into a dependency graph.

## 4. Authority pipeline

Identity Context follows the same Reality Boundary:

```text
Observation
→ Membership Proposal
→ Human Review
→ Confirm
→ governed context membership
```

Allowed machine behavior:
- suggest “these UK objects may belong to 英国金融身份”;
- explain why the suggestion was made;
- rank proposals for review.

Forbidden machine behavior:
- region = GB → silently confirm UK Financial;
- provider name contains “HSBC” → silently confirm Financial;
- search similarity → context membership;
- card/phone co-occurrence → Dependency;
- PresentationProfile tag → context truth.

## 5. Context source vocabulary

Proposed source kinds:

```text
USER
IMPORT
AI_PROPOSAL
RULE_PROPOSAL
MIGRATION
```

Only reviewed/authorized transitions may enter confirmed membership.

`AI_PROPOSAL` and `RULE_PROPOSAL` are not confirmed Reality.

## 6. Region composition

Region and Identity Context are independent dimensions.

Allowed query:

```text
Region=GB ∩ Context=Financial
```

Meaning:

> “Confirmed Financial-context members whose confirmed/presented region scope is GB.”

It does not create a new context or new graph.

The Globe may later visualize this intersection only when both dimensions have
governed membership/data.

## 7. Query semantics

Minimum queries:

```text
listContexts()
members(contextId)
contextsForObject(objectId)
intersect(contextId, region?)
contextImpact(contextId, hypotheticalChange?)
```

`contextImpact` is a query over the existing graph. It must not manufacture context
edges.

Useful derived aggregates:

```text
object type counts
confirmed dependency count among/through members
attention count
active change count
unknown/pending membership count
region distribution
```

Aggregates must preserve source state.

## 8. Consumer UI

Identity is a **Lens**, not a sixth primary tab.

Current five-primary IA remains:

```text
现在 / 基础设施 / 变更 / 记录 / 我
```

Future entry points after Canonical support:

### Infrastructure
Context selector near the inventory scope:

```text
全部基础设施
身份：英国金融
地区：英国
```

### Me
“My digital life” may show confirmed contexts as personal workspace groupings.

### Search
Confirmed context names may become search scopes/results.

No selector is rendered while membership is not governed.

## 9. Context Detail

Future context detail should use existing Product Lens grammar:

```text
Context identity
→ confirmed members
→ region distribution
→ confirmed dependencies / Impact
→ active changes / maintenance
→ unknown/pending membership
```

Do not turn it into a manual folder manager detached from PDIG truth.

## 10. Rename / archive semantics

Renaming a context:
- changes the context label;
- does not rename objects;
- does not change dependencies.

Archiving:
- hides it from normal selectors;
- does not archive member objects;
- does not delete Reality.

Deleting a membership:
- removes grouping membership only;
- never deletes the object or edge.

## 11. Suggested built-ins

Do **not** ship pre-confirmed built-ins.

The product may offer templates:

```text
主身份
金融
工作
旅行
开发者
隐私
家庭
```

A template creates an empty or proposal context. It does not auto-populate confirmed
members.

## 12. Evidence and explainability

Every proposed membership should be able to explain:

```text
why suggested
source observation
confidence / extraction evidence
what confirmation will change
```

Confidence is review metadata. It is not Reality.

## 13. Privacy

Context names may themselves reveal sensitive life structure.

Requirements:
- local-first storage under existing encryption;
- context membership included in export only after explicit schema decision;
- no analytics of context name/member graph;
- masking applies to displayed member identifiers;
- AI proposal payload should minimize identifiers.

## 14. Migration

Current files have no Identity Context.

Migration rule:

```text
old payload → zero confirmed contexts
```

Forbidden migration:

```text
infer contexts from region/provider/name during file upgrade
```

Absence means “not recorded,” not “user has no contexts.”

## 15. Conformance fixtures

Minimum positive/negative/boundary cases:

```text
IC-01 one confirmed context with two members
IC-02 same object confirmed in two contexts
IC-03 proposal membership excluded from confirmed query
IC-04 rejected proposal does not affect other membership
IC-05 archived context preserves member Reality
IC-06 rename changes label only
IC-07 region intersection is query-only
IC-08 provider similarity does not auto-confirm
IC-09 AI proposal cannot become dependency
IC-10 deleting membership does not delete object
IC-11 unknown objectRef rejected
IC-12 duplicate membership deterministic/deduplicated
IC-13 cross-platform export/import deterministic
IC-14 old payload migrates to no confirmed contexts
```

## 16. Cross-platform requirements

Shared:
- IDs/state/source/evidence;
- membership authority;
- deterministic query semantics;
- export/migration behavior.

Platform translated:
- selector control;
- context chips;
- adaptive detail layout;
- search/filter interaction.

Android cannot create an Android-only context store once Canonical implementation
starts.

## 17. External research alignment

External standards are design input, **not PDIG Reality**.

NIST SP 800-63B-4 distinguishes account recovery from normal authentication and
recognizes multiple recovery-method classes. FIDO guidance emphasizes multiple
authenticators to reduce recovery dependence. Those reinforce PDIG's need to reason
about concrete, separately established factors rather than a vague “identity score.”

References:
- https://csrc.nist.gov/pubs/sp/800/63/B/4/final
- https://fidoalliance.org/white-paper-multiple-authenticators-for-reducing-account-recovery-needs-for-fido-enabled-consumer-accounts/

Identity Context itself is a PDIG product primitive; those documents do not define it.

## 18. Implementation order

```text
proposal review
→ schema/version decision
→ membership state machine
→ fixtures
→ conformance
→ read/write repositories
→ cross-platform query API
→ Android hidden-gate → visible Lens
→ runtime evidence
```

Until then:

```text
IDENTITY_CONTEXT_DESIGN = COMPLETE
IDENTITY_CONTEXT_CANONICAL = NOT_IMPLEMENTED
IDENTITY_LENS_VISIBLE_UI = HOLD
```
