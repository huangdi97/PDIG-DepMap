# ANDROID_UI_VNEXT_R25_SOURCE_REPORT

> 2026-10-10 · feat/android-ui-vnext-translation
>
> R25 closes the **Manual Relationship design gap** after R24 Manual Establish.

## 0. Truth status

~~~text
ANDROID_UI_VNEXT_SOURCE = R25

FIVE_PRIMARY_IA = PRESERVED

ESTABLISH_IMPORT = SOURCE_IMPLEMENTED
MANUAL_ESTABLISH = SOURCE_IMPLEMENTED_READ_ONLY
MANUAL_RELATIONSHIP = SOURCE_IMPLEMENTED_READ_ONLY
HUMAN_REVIEW = SOURCE_IMPLEMENTED

PRODUCTION_MANUAL_CREATE_AUTHORITY = NOT_EXPOSED_TO_VNEXT
PRODUCTION_MANUAL_DEPENDENCY_AUTHORITY = NOT_EXPOSED_TO_VNEXT

CANONICAL_SCHEMA_CHANGE = NONE
DEPMAP_PAYLOAD_CHANGE = NONE
NEW_PRIMARY_TAB = NONE

FRESH_R25_BUILD = NOT_RUN
FRESH_R25_UNIT_TESTS = NOT_RUN
FRESH_R25_INSTRUMENTATION = NOT_RUN
FRESH_R25_PHONE_PIXELS = NOT_RUN
FRESH_R25_TABLET_PIXELS = NOT_RUN
FRESH_R25_HUMAN_ACCEPTANCE = NOT_RUN

ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_CUTOVER = HOLD
~~~

## 1. Why R25 exists

R24 answered:

> How do I manually record that an object exists?

But a dependency graph also needs:

> How do I explicitly record a relationship I know is true?

Canonical already distinguishes Dependency origin:

~~~text
manual
proposal
~~~

R25 freezes the consumer UX for the manual path without bypassing the domain
authority that production VNext still lacks.

## 2. Route and hierarchy

Route label:

~~~text
手工记录关系
~~~

Hierarchy:

~~~text
Data Sources
→ 建立基础设施
  → 手工记录
    → 手工记录关系
~~~

Up returns to Manual Establish.

Search can find the route through:
- 关系;
- 依赖;
- 手工关系;
- dependency.

## 3. Consumer grammar

The sequence is:

~~~text
From
→ Relation
→ To
→ Capability
→ Criticality
→ explicit confirmation
~~~

The UI never asks the user to type a node ID.

Preview explains direction using a non-mutating example:

~~~text
支付工具
→ 支付绑定
→ 服务

capability = payment
criticality = unknown
origin = manual
~~~

## 4. Runtime relation vocabulary

R25 aligns to the current runtime relation registry:

~~~text
funding_source
merchant_agreement
recovers
authenticates
controls
~~~

Storage-known but runtime-gated:

~~~text
verifies
bound_to
~~~

The screen visibly labels those as HOLD rather than offering them as valid
production choices.

## 5. Criticality boundary

R25 makes this explicit:

~~~text
default = unknown
required = explicit human confirmation only
~~~

and:

~~~text
unknown != optional
unknown != safe
~~~

No model, provider rule or convenience UI may silently set required.

## 6. Independence boundary

The relationship page explicitly refuses to model path independence as an edge
checkbox.

It reminds the user that:
- two edges may share one provider;
- two factors may share one device;
- two recovery routes may be in one FailureDomain.

Therefore independent-path analysis remains in Continuity, not manual relation
entry.

## 7. Preview authority

Required Preview state:

> 当前 Preview 不提供“确认关系”按钮

Reason:
- AppContainer does not yet expose a tested manual Dependency creation authority
  for VNext;
- direct SQL / local state would violate graphRevision and domain validation.

This is an authority HOLD, not missing visual design.

## 8. Proposal vs Manual provenance

R25 preserves two legitimate paths:

~~~text
source/model discovery
→ Proposal
→ Human Review
→ Dependency origin=proposal
~~~

and:

~~~text
explicit user knowledge
→ Manual Relationship
→ Dependency origin=manual
~~~

Both converge on Confirmed Reality only after authoritative domain mutation.

## 9. Tests / pixel source

Added:
- ManualRelationshipR25Test;
- ManualRelationshipR25ContractTest.

Phone pixel journey extends:

~~~text
建立基础设施
→ 手工记录
→ 手工记录关系
→ truth boundary / unknown criticality / no fake action
→ Up → 手工记录
→ Up → 建立基础设施
→ Up → 数据源
~~~

## 10. Design coverage after R25

The Establish capability is now source-designed as:

~~~text
file import
→ proposals/review

manual object recording
→ object Reality authority (future production binding)

manual relationship recording
→ manual Dependency authority (future production binding)

source/discovery
→ Human Review
~~~

The missing work is production authority implementation, not consumer UX design.

## 11. Remaining gates

Still not claimed:
- exact-head compile/test PASS;
- exact-head runtime/pixels;
- Human Visual Acceptance;
- VNext production launcher cutover;
- manual Node / Dependency production gateways;
- Canonical lifecycle implementation;
- Identity subtype implementation;
- Recovery solver/runtime.

## 12. Stop line

~~~text
R25_MANUAL_RELATIONSHIP_DESIGN_GAP = CLOSED
ESTABLISH_CAPABILITY_UX = COMPLETE_AT_SOURCE

SOURCE_COMPLETE != RUNTIME_VERIFIED != HUMAN_ACCEPTED

ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_CUTOVER = HOLD
~~~
