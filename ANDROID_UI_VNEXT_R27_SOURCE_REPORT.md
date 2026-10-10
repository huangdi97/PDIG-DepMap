# ANDROID_UI_VNEXT_R27_SOURCE_REPORT

> 2026-10-10 · feat/android-ui-vnext-translation
>
> R27 closes the remaining **visible v0.3 continuity-finding grammar** gap in UI
> vNext. R26 had already frozen deeper future capability gates; R27 does not add
> a new domain, tab, score, or solver. It makes the existing v0.3 Finding semantics
> reviewable in the new consumer UI.
>
> This report does **not** claim fresh runtime or Human Pixel Acceptance.

## 0. Truth status

~~~text
ANDROID_UI_VNEXT_SOURCE = R27
FIVE_PRIMARY_IA = PRESERVED

V0_3_FINDING_UX_GRAMMAR = SOURCE_COMPLETE
SPOF_UI_STATE = IMPLEMENTED_REFERENCE
SHARED_FAILURE_DOMAIN_UI_STATE = IMPLEMENTED_REFERENCE
RECOVERY_CYCLE_UI_STATE = IMPLEMENTED_REFERENCE
UNCONFIRMED_FALLBACK_UI_STATE = IMPLEMENTED_REFERENCE
STALE_RECOVERY_INFO_UI_STATE = IMPLEMENTED_REFERENCE
UNKNOWN_CRITICAL_PATH_UI_STATE = IMPLEMENTED_REFERENCE
PENDING_VERIFICATION_UI_STATE = IMPLEMENTED_REFERENCE

GLOBAL_HEALTH_SCORE = FORBIDDEN
PATH_COUNT_AS_INDEPENDENCE = FORBIDDEN
UI_EDGE_COUNT_SOLVER = FORBIDDEN

CANONICAL_SCHEMA_CHANGE = NONE
DEPMAP_PAYLOAD_CHANGE = NONE
NEW_PRIMARY_TAB = NONE

FRESH_R27_BUILD = PENDING
FRESH_R27_INSTRUMENTATION = PENDING
FRESH_R27_PHONE_PIXELS = PENDING
FRESH_R27_TABLET_PIXELS = PENDING
FRESH_R27_HUMAN_ACCEPTANCE = PENDING

ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_CUTOVER = HOLD
~~~

## 1. Why R27 exists

v2.3 v0.3.0 explicitly requires the product to make continuity structure
understandable:

~~~text
single point / no confirmed backup
shared failure domain
recovery cycle
unconfirmed fallback
stale recovery information
unknown critical path
pending verification
~~~

The old production Findings page already expressed part of this grammar, and the
core/runtime already owns FailureDomain / RecoveryCycle behavior.

The UI vNext Weaknesses page, however, still concentrated on:
- explicit unique-recovery objects;
- expiring cards;
- device review;
- phone-migration blocker.

That was useful operational information, but it did not fully represent the v0.3
continuity-analysis product language.

R27 closes that product-language gap.

## 2. Full Finding presentation model

New:

`android/app/src/main/kotlin/com/pdig/uivnext/demo/UiVNextContinuityFindings.kt`

Reference kinds:

~~~text
SINGLE_POINT_OF_FAILURE
SHARED_FAILURE_DOMAIN
RECOVERY_CYCLE
UNCONFIRMED_FALLBACK
STALE_RECOVERY_INFORMATION
UNKNOWN_CRITICAL_PATH
PENDING_VERIFICATION
~~~

Every item must provide:

~~~text
what / title
why
confirmed basis
unknowns
recommended next action
~~~

That matches the v0.3 Infrastructure Finding epistemic shape without creating a
health score.

## 3. Critical boundary: reference finding != UI inference

The current preview relation fixture is intentionally too small to act as a
FailureDomain solver.

Therefore R27 explicitly does **not** do:

~~~text
Compose edge count
→ infer shared failure domain
→ claim independent path count
~~~

Instead, Preview carries a clearly isolated **Synthetic Reference Finding** set so
all product states can receive visual/human review.

Production must project authoritative continuity analysis. The UI cannot upgrade
this reference fixture into Personal Reality.

## 4. Weaknesses becomes two different layers

The screen now separates:

### A. Continuity Findings

Structural/reasoning output:

~~~text
单点路径
共享故障点
恢复循环
备用待确认
信息需复核
关键性未知
等待验证
~~~

### B. Maintenance / migration reminders

Operational records:

~~~text
card expiry
explicit unique recovery object
device review
phone migration blocker
~~~

These counts are not added together into a fake “risk total”.

## 5. Finding card hierarchy

Every structural Finding card answers:

~~~text
类型
→ 发生了什么
→ 为什么
→ 确认依据
→ 仍未知什么
→ 下一步
~~~

This is intentionally explanation-first.

Forbidden:
- “Infrastructure Health = 86”;
- percent-safe badges;
- “2 methods = 2 independent paths”;
- absence of Finding = safe;
- potential/shared assumptions presented as confirmed.

## 6. Now continuity summary

Compact Now still stays task-first.

The Continuity Insight now exposes:
- recorded relation context;
- structural Finding count in the reference;
- one top continuity prompt;
- direct route to Weaknesses.

It explicitly says path count does not replace independence analysis.

The detailed seven-state grammar remains in Weaknesses rather than turning Now
into an engineering dashboard.

## 7. Target routing

Where a reference Finding has an actual referenced object:
- number Finding → Number Detail;
- device Finding → Device Detail;
- pending Change verification → Change Center.

A purely structural/reference finding may have no target route.

The UI never creates a fake graph object merely to make every card clickable.

## 8. Tests

New:
- `UiVNextContinuityFindingsTest`
- `ContinuityFindingsR27ContractTest`

They guard:
- all seven v0.3 Finding classes exist in the reference grammar;
- every item includes basis / unknown / next action;
- review-only classes do not become critical facts;
- no percentage/health-score copy;
- compact and Medium Weaknesses render structural Finding states.

## 9. Production binding

R27 does not create a second continuity engine.

Production binding remains:

~~~text
Confirmed Reality
→ authoritative Impact / FailureDomain / RecoveryCycle / Finding engine
→ production consumer projection
→ VNext Finding presentation
~~~

The production seam must not import `UI_CONTINUITY_REFERENCE_FINDINGS`.

If a Finding class cannot be produced authoritatively yet, production displays
that capability as unavailable/unknown rather than substituting reference data.

## 10. R26 authority work retained

R27 preserves the R26 work completed immediately before it:
- Human Review production authority seam;
- Import production authority seam;
- Manual Establish AppContainer authority;
- VNext Manual Establish gateway;
- Manual Relationship still gated by Native Schema v4;
- capability/authority matrix;
- five primary destinations;
- R22–R25 visible reference flows.

## 11. Remaining evidence gates

~~~text
exact remote R27 HEAD
→ codegen/checks
→ core/app unit tests
→ Android compile
→ instrumentation
→ API36 phone runtime/pixels
→ API36 tablet runtime/pixels
→ GPU runtime evidence
→ human visual review
→ Android Reference Freeze
~~~

A green build alone is not Human Acceptance.

## 12. Stop line

~~~text
V0_3_CONTINUITY_FINDING_UX = CLOSED_AT_R27
R27_SOURCE_DESIGN = COMPLETE

SOURCE_COMPLETE != RUNTIME_VERIFIED != HUMAN_ACCEPTED

ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_CUTOVER = HOLD
~~~
