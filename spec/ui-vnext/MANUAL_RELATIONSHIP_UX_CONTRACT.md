# PDIG Manual Relationship UX Contract

> Status: **DESIGN_FROZEN / R31 AUTHORITY UPDATE**
>
> Scope: explicit human recording of a relation between confirmed Reality objects.
>
> Preview remains read-only. Production VNext now exposes an authoritative manual
> Dependency path for the **current Canonical v3 runtime relation registry only**.

## 1. Product role

A personal infrastructure graph needs a path for facts the user knows directly.

Manual relationship entry is appropriate when:

> “I know object A depends on / authenticates with / recovers through object B.”

It is not appropriate for:
- model guesses;
- inferred correlation;
- a Proposal that has not been reviewed;
- a Candidate that is not yet a confirmed Node.

Permanent boundary:

~~~text
confirmed objects
+ explicit user relation statement
→ manual Dependency authority

NOT:
object creation
→ automatic relation
~~~

## 2. Information architecture

~~~text
Data Sources
→ 建立基础设施
  → 手工记录
    → 手工记录关系
~~~

Hierarchy:

~~~text
手工记录关系 → Up → 手工记录 → Up → 建立基础设施
~~~

Search aliases:
- 关系;
- 依赖;
- 手工关系;
- dependency.

No primary navigation change.

## 3. Required fields

A future production form needs exactly the semantic fields required to create a
valid Dependency:

~~~text
From
Relation
To
Capability
Criticality
~~~

Additional domain-controlled fields:
- origin = manual;
- state = active;
- confirmedAt = authoritative user-confirmation time;
- evidence refs only when actual evidence exists.

UI must not ask the user to type internal IDs.

## 4. Endpoint authority

From and To:
- must be confirmed Reality Nodes;
- must not be Proposal-only objects;
- must not be unresolved DiscoveryCandidates;
- must respect archive/retired rules.

Object pickers display consumer identity, not raw node kind alone.

## 5. Relation runtime registry

Current runtime relation semantics are:

~~~text
funding_source
merchant_agreement
recovers
authenticates
controls
~~~

Consumer labels:

| relation | consumer label | typical capability |
| --- | --- | --- |
| funding_source | 付款来源 | payment |
| merchant_agreement | 支付绑定 | payment |
| recovers | 恢复 | recovery |
| authenticates | 登录验证 | authentication |
| controls | 控制 | access |

Storage-known legacy/future relations:

~~~text
verifies
bound_to
~~~

must not appear as active production creation choices until runtime validation
supports them cross-platform.

## 6. Capability

Current runtime capability choices:

~~~text
payment
access
authentication
recovery
communication
~~~

Identity is storage-known but not a runtime propagation capability.

The UI should narrow choices based on RelationDefinitionRegistry when production
binding exists; it must not hard-code arbitrary relation/capability pairs in Compose.

## 7. Criticality

Default:

~~~text
unknown
~~~

Meaning:

> We know the relationship exists, but have not confirmed that this edge is required.

Important:

~~~text
unknown != optional
unknown != safe
~~~

The UI may allow the user to explicitly mark a relation required only through the
governed authority.

Machine/model/provider logic must never set required automatically.

## 8. Preview state

R25 Preview demonstrates:
- semantic sequence;
- current runtime vocabulary;
- direction;
- default unknown criticality;
- manual origin;
- path-independence warning.

It exposes no “确认关系” mutation action.

Required message:

> 当前 Preview 不提供“确认关系”按钮

This is an authority gate, not an incomplete form.

## 9. Production authority — R31

R31 supersedes the earlier “wait for Native Schema v4 before any manual relationship”
assumption.

Current Canonical v3 already governs the complete current runtime registry:

~~~text
funding_source
merchant_agreement
recovers
authenticates
controls
~~~

The Production gateway is therefore enabled for exactly that set.

It must:
1. pick From/To from confirmed, non-archived Reality Nodes;
2. derive Relation + Capability from `RelationDefinitionRegistry`;
3. validate endpoint kinds and relation/capability via `validateRelationUse`;
4. set `origin=manual`;
5. default `criticality=unknown`; only an explicit human choice may set `required`;
6. insert/reactivate/reconfirm the logical Dependency in one authoritative transaction;
7. bump `graphRevision` exactly once for that confirmation;
8. preserve the existing logical row instead of creating duplicates;
9. return the re-read Dependency and graph revision.

Still forbidden:

~~~text
verifies
bound_to
~~~

Those are storage-known/future relations and are not in the runtime registry.

Preview still exposes **no** production mutation button. The executable form exists
only in the production-bound tree.

## 10. Independent path boundary

Manual Relationship must never ask:

> “Is this an independent backup?”

as a simple checkbox.

Two edges do not prove independent recovery.

Independence depends on:
- FailureDomain;
- RecoveryCycle;
- shared provider/device/identity;
- other continuity semantics.

That analysis belongs to the Continuity engine.

## 11. Review vs Manual

Proposal route:

~~~text
machine/source evidence
→ Proposal
→ Human Review
→ Dependency origin=proposal
~~~

Manual route:

~~~text
explicit human knowledge
→ Manual Relationship confirmation
→ Dependency origin=manual
~~~

They converge on Confirmed Reality but preserve provenance.

## 12. Acceptance

Source:
- Manual Relationship is secondary;
- Up → Manual Establish;
- search discovery works;
- no mutation button in Preview;
- unknown/required distinction visible;
- unsupported runtime relations visibly gated.

Future production:
- invalid relation/capability rejected;
- machine cannot set required;
- revision bumps exactly once per new Reality relation;
- duplicate logical relation behavior deterministic;
- Impact sees relation only after successful authoritative commit;
- export/restore preserves origin=manual.

## 13. Stop line

~~~text
MANUAL_RELATIONSHIP_UX = DESIGN_FROZEN
MANUAL_RELATIONSHIP_PREVIEW = SOURCE_IMPLEMENTED_READ_ONLY
PRODUCTION_MANUAL_DEPENDENCY_AUTHORITY = AVAILABLE_CURRENT_V3_RUNTIME_SET
STORAGE_ONLY_RELATIONS = HOLD
AD_HOC_RELATION_WIDENING = FORBIDDEN
NO_DEGREE_COUNT_INDEPENDENCE = REQUIRED
~~~
