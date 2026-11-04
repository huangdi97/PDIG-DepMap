# PDIG Product Lenses & Identity Context Contract

> Date: 2026-10-09  
> Status: **DESIGN_FROZEN / PROJECTION_CONTRACT**
>
> Scope: UI/query architecture for Region / Identity / Dependency / Change /
> Recovery lenses and Identity Context.
>
> This contract does **not** create new Reality. A Lens changes how the same
> confirmed/pending/unknown state is queried and presented.

## 0. Product rule

PDIG has one truth substrate and multiple useful views.

```text
Personal Reality + Evidence State + Continuity Results + Change State
                              │
                              ▼
                         Query Lens
                              │
                              ▼
                      Consumer Projection
```

Never:

```text
Lens
→ new hidden truth container
→ Android-only semantic fork
```

The user should not need to understand graph ontology in order to answer:

- 我的数字基础设施主要分布在哪里？
- 某个生活/工作身份依赖什么？
- 谁依赖这个对象？
- 我正在改变什么？
- 如果一部分已经失效，我还剩哪些可用恢复根？

## 1. Lens vocabulary

### Region Lens

Question:

> **我在不同地区依赖什么？**

Inputs:
- confirmed object region/context when governed;
- presentation-only synthetic region in Preview;
- confirmed dependencies;
- attention/findings.

Outputs:
- region distribution;
- object composition;
- scoped attention;
- selected-region object collections.

Current Android status:

```text
REFERENCE_UI = IMPLEMENTED
REGION_FACT_DESIGN = COMPLETE
PRODUCTION_REGION_FACT_CANONICAL = NOT_IMPLEMENTED
PRODUCTION_GLOBE = GPU_CONTEXT_ONLY / ZERO_SYNTHETIC_LABELS
```

The governed semantic design is frozen in:
`spec/proposals/region-facts-v1.md`.

Production must keep the Earth visually present but its asset geography truth-empty
until confirmed RegionFacts exist. The Globe is the spatial entry to Region Lens,
not the truth owner. Territory coordinates/centroids are presentation infrastructure,
not object physical-location claims.

### Identity Lens

Question:

> **某个真实生活身份由什么组成？**

Conceptual contexts:

```text
中国主身份
英国金融身份
工作身份
旅行身份
开发者身份
隐私身份
```

A context may contain existing objects and dependencies. It must not duplicate
those objects.

Current status:

```text
IDENTITY_CONTEXT_DESIGN = COMPLETE
IDENTITY_CONTEXT_CANONICAL = NOT_IMPLEMENTED
ANDROID_VISIBLE_SELECTOR = HOLD
```

The full governed proposal is:
`spec/proposals/identity-context-v1.md`.

Do not display an Identity Lens selector until context membership has a governed
source. Android enforces this with `VNextLensAvailability`.

### Dependency Lens

Question:

> **谁依赖谁？改变这个对象会影响哪些已确认关系？**

Inputs:
- confirmed Dependency;
- DependencyGroup;
- Impact result;
- pending proposal kept visibly separate.

Outputs:
- confirmed dependency list;
- capability-aware Impact Lens;
- unknown/pending relationship context;
- path/failure-domain findings when available.

Current Android status:

```text
OBJECT_IMPACT_LENS = IMPLEMENTED_REFERENCE
PRODUCTION_IMPACT_ADAPTER = SOURCE_IMPLEMENTED
GLOBAL_DEPENDENCY_LENS_UI = HOLD
```

The object-level Impact Lens is the first consumer implementation of Dependency
Lens.

### Change Lens

Question:

> **当前变更涉及什么、处于什么阶段、还有什么不能做？**

Inputs:
- ChangePlan;
- Action DAG;
- verification state;
- current graph revision;
- impact/revalidation.

Outputs:
- active changes;
- Current / Transition / After;
- blockers;
- next verified action;
- old/new transition context.

Current Android status:

```text
CHANGE_CENTER_REFERENCE = IMPLEMENTED
REPLACE_PHONE_REFERENCE = IMPLEMENTED
REPLACE_PAYMENT_CARD_REFERENCE = R21_IMPLEMENTED
PRODUCTION_CHANGE_GATEWAY = SOURCE_IMPLEMENTED
PRODUCTION_SCREEN_BINDING = HOLD
```

### Recovery Lens

Question:

> **如果故障已经发生，我现在还剩什么可以用？**

This is not the same as pre-change Impact.

It requires:
- surviving roots after node/failure-domain removal;
- independent-path analysis;
- recovery cycles;
- confirmed accessible factors;
- provider/time constraints.

Current status:

```text
RECOVERY_INCIDENT_DESIGN = COMPLETE
RECOVERY_SOLVER = NOT_IMPLEMENTED
PRODUCTION_RECOVERY_MODE = NOT_IMPLEMENTED
ANDROID_VISIBLE_RECOVERY_MODE = FORBIDDEN_UNTIL_RUNTIME_SUPPORT
```

The full incident/solver proposal is:
`spec/proposals/recovery-incident-mode-v1.md`.

The future consumer UX is frozen in:
`spec/ui-vnext/RECOVERY_AND_IDENTITY_LENS_UX_CONTRACT.md`.

No fake Emergency/Recovery page should appear merely to fill the architecture.
Android source explicitly gates Recovery as hidden until the solver exists.

## 2. Identity Context

Identity Context is a **grouping/query primitive**, not a NodeKind.

Conceptual shape:

```text
IdentityContext
  id
  name
  purpose
  objectRefs[]
  optional region scope
  source / confirmation state
```

Examples:
- UK Financial may reference a UK bank account, UK phone, recovery email and
  trusted device;
- Developer may reference source control, cloud account, hardware key and
  recovery methods.

The same object may belong to multiple contexts.

Forbidden:
- copying an object into each context;
- making context membership imply a dependency;
- deriving context automatically from region/provider names and committing it as
  Reality;
- using PresentationProfile as context truth.

## 3. Authority model

Context/Lens must obey the same epistemic pipeline:

```text
Observation
→ Proposal
→ Review
→ Confirm
→ Reality / governed grouping
→ Lens query
```

AI may propose grouping or explain a query result.

AI may not:
- assert that two recovery methods are independent;
- assign an identity context as confirmed without authority;
- convert search similarity into a dependency;
- create a safety verdict.

## 4. Lens state model

Every Lens result must be able to represent:

```text
confirmed
derived-from-confirmed
pending-review
unknown
unavailable
```

Consumer UI should translate these into understandable language rather than expose
internal state names.

Example:

```text
confirmed          → 已确认
pending-review     → 待确认
unknown            → 未记录 / 仍未知
unavailable        → 当前还不能判断
```

## 5. Navigation rule

Lenses do **not** become new primary destinations.

Primary Android IA remains:

```text
现在 / 基础设施 / 变更 / 记录 / 我
```

Lens entry belongs where the user's question occurs:

- Region → Now / Infrastructure / Globe;
- Dependency → focused object detail;
- Change → Change Center / ChangePlan;
- Identity → Infrastructure or Me after governed contexts exist;
- Recovery → future incident/recovery flow when production-supported.

This prevents a sixth/seventh top-level tab from appearing every time analysis
capability grows.

Current Android gate table:

```text
Region      VISIBLE
Dependency  VISIBLE
Change      VISIBLE
Identity    HIDDEN_UNTIL_CANONICAL
Recovery    HIDDEN_UNTIL_SOLVER
```

The gate is executable source, not documentation-only:
`android/app/src/main/kotlin/com/pdig/uivnext/lens/VNextLensAvailability.kt`.

## 6. Object-detail convergence

R20 freezes a common detail grammar:

```text
Object Identity
→ Recorded Context / Lifecycle
→ Confirmed Relations
→ Impact Lens
→ Supported Change / Recovery action
```

Current object identities:

```text
Card     financial asset identity
Number   communication identity
Account  access / control identity
Email    communication / recovery identity
Device   physical access endpoint
Service  dependency endpoint
```

The grammar is shared; the visual identity is not forced to be identical.

## 7. Prepare / Change / Recover / Maintain

These are product modes, not necessarily navigation tabs.

### Prepare
- choose a concrete object/change;
- inspect Impact;
- show unknowns;
- build/preview a plan only for supported scenarios.

### Change
- execute the real plan;
- respect prerequisites;
- keep old/new transition state visible;
- verify explicitly.

### Recover
- incident already happened;
- compute surviving options;
- prioritize viable recovery roots;
- currently **not production-supported**.

### Maintain
- upcoming maintenance;
- stale facts;
- pending review;
- findings;
- verification backlog.

Android mapping:

```text
Now          = Maintain-oriented task surface
Infrastructure = Inventory + object lenses
Change       = Prepare + active Change work center
Records      = evidence/history/verification trace
Me           = personal workspace and preferences
```

No one-to-one tab is required for Recover until it is a real capability.

## 8. Records boundary

Records answers:

> **发生过什么、验证过什么、依据是什么？**

It should prefer:
- confirmed/verified events;
- plan transitions;
- explicit completion/verification;
- imported/review decisions;
- due/checkpoint history once maintenance becomes Canonical.

It should not become a second Now page full of unhandled attention.

R21 makes the synthetic reference itself follow this boundary:
- Attention / Upcoming are no longer rendered as Records sections;
- completed plan stages are shown as **已记录完成**, not verified;
- verifying stages are shown as **待验证**;
- future not-started/blocked stages are not promoted into historical events;
- active plans may appear only as navigation context.

The production target remains an authoritative evidence/verification trace sourced
from Timeline / ChangePlan / Review / Verification / Evidence.

## 9. Change Center boundary

Primary Change answers:

> **我正在改变什么？我准备改变什么？**

It is distinct from a focused scenario.

```text
Change Center
  active work
  prepare entries
  maintenance/review links
      │
      ├─ Replace Phone
      ├─ Replace Payment Card (R21 reference + production scenario)
      └─ future supported scenario
```

Opening Change Center never mutates Reality.

R21 payment-card flow reuses the production scenario grammar:

```text
检查支付依赖
→ 迁移必要支付关系
→ 用真实支付 / 账单证据验证关键路径
```

Card Detail may expose **分析更换此卡的影响** because
`replace_payment_card` is already an active production scenario. Preview still
renders projections only and never writes ChangePlan state.


## 10. Region × Identity

Long term, a query may combine lenses:

```text
UK × Financial
CN × Primary
Global × Developer
```

Composition is an intersection/query, not a new graph.

The Globe may become a visual entry when both dimensions have confirmed data.
Until then, Region remains the only visible spatial Lens.

## 11. Recovery correctness

Recovery Lens requires failure-domain-aware semantics.

Permanent rules:

```text
2 methods != 2 independent paths
2 accounts != 2 independent providers
recovery use != unique recovery
path count != independent path count
unknown != safe
```

The UI must consume Continuity output rather than recreate graph algorithms.

## 12. Cross-platform contract

Shared:
- questions answered;
- truth/unknown semantics;
- lens vocabulary;
- object identities;
- Change/Recovery boundaries.

Platform-translated:
- navigation component;
- layout;
- Globe size;
- list/detail composition;
- inspector;
- gestures.

Android can be light-first while Desktop remains the frozen dark visual reference.

## 13. Implementation gates

### Now
- Region Lens reference: implemented.
- Object Impact Lens: implemented.
- Change Lens reference: implemented.

### Before Identity Lens becomes visible
- governed IdentityContext semantics;
- persistence/migration decision;
- cross-platform fixtures/conformance;
- review/confirmation flow.

### Before Recovery Lens becomes visible
- production recovery solver / surviving-root semantics;
- failure-domain evidence;
- incident-state contract;
- tests/fixtures;
- action/verification path.

## 14. Acceptance rule

A Lens is product-complete only when:

```text
question
+ governed inputs
+ explicit unknown state
+ deterministic query
+ consumer projection
+ cross-platform semantic contract
+ runtime evidence
```

If governed inputs do not exist, the correct product behavior is to keep the Lens
hidden or explicitly unavailable — not to fabricate a plausible visualization.
