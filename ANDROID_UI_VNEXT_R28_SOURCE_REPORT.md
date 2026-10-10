# ANDROID_UI_VNEXT_R28_SOURCE_REPORT

> 2026-10-10 · feat/android-ui-vnext-translation
>
> R28 starts the **production screen/source binding closure** without cutting over
> the production launcher and without allowing Synthetic Reference data to leak into
> production UI.
>
> This is source architecture / read-only production binding work. It does **not**
> claim Android Reference Freeze, production launcher cutover, lifecycle Canonical
> persistence, or phone/email subtype Canonicalization.

## 0. Truth status

~~~text
ANDROID_UI_VNEXT_SOURCE = R28

REFERENCE_PREVIEW_SOURCE = EXPLICIT
PRODUCTION_REALITY_SOURCE = EXPLICIT
REFERENCE_TO_PRODUCTION_FALLBACK = FORBIDDEN

PRODUCTION_RUNTIME_DATA_BOUNDARY = IMPLEMENTED
PRODUCTION_SESSION = IMPLEMENTED
PRODUCTION_ROOT_SHELL = IMPLEMENTED_READ_ONLY

PRODUCTION_NOW_ROOT = SOURCE_BOUND
PRODUCTION_INFRASTRUCTURE_ROOT = SOURCE_BOUND
PRODUCTION_CHANGE_ROOT = SOURCE_BOUND_READ_ONLY
PRODUCTION_RECORDS_ROOT = SOURCE_BOUND
PRODUCTION_ME_ROOT = SOURCE_BOUND
PRODUCTION_SOURCES = SOURCE_BOUND

PRODUCTION_CARD_DETAIL = HOLD
PRODUCTION_NUMBER_DETAIL = HOLD_UNTIL_IDENTITY_SUBTYPE
PRODUCTION_SECONDARY_DETAILS = HOLD
PRODUCTION_HUMAN_REVIEW_SCREEN_BINDING = HOLD
PRODUCTION_IMPORT_SCREEN_BINDING = HOLD
PRODUCTION_MANUAL_ESTABLISH_SCREEN_BINDING = HOLD
PRODUCTION_CHANGE_MUTATION_UI_BINDING = HOLD

PRODUCTION_LAUNCHER_CUTOVER = HOLD
ANDROID_REFERENCE_FREEZE = HOLD
~~~

## 1. Problem R28 closes

Before R28, the repository already had:
- production snapshot/read-model projections;
- production impact projection;
- production records projection;
- authoritative Change gateway;
- authoritative Human Review gateway;
- authoritative Import seam;
- authoritative Manual Establish gateway.

But UI vNext still had one dangerous architectural gap:

~~~text
production data APIs exist
!=
screens have a strict production source boundary
~~~

Without a source boundary, a future cutover could accidentally instantiate the
reference UI and let it reach `UiVNextDemoFixture` while running under production.

R28 closes that class of mistake.

## 2. Runtime data source boundary

New:

`android/app/src/main/kotlin/com/pdig/uivnext/production/VNextRuntimeDataSource.kt`

Two explicit modes:

~~~text
REFERENCE_PREVIEW
PRODUCTION_REALITY
~~~

Reference mode:
- cannot return a production snapshot;
- cannot return production impact;
- cannot return production plan;
- cannot return production records.

Production mode:
- wraps `VNextReadModelSource`;
- projects only authoritative AppContainer-owned data;
- never substitutes synthetic fixture facts for missing Reality.

Permanent rule:

~~~text
Production missing data
→ render unknown / unavailable

NOT

Production missing data
→ borrow Preview fixture
~~~

## 3. Production session

New:

`ProductionVNextSession`

It packages:
- normal VAppState navigation / presentation state;
- a required `PRODUCTION_REALITY` runtime data source.

The constructor rejects a Reference data source.

This gives the future launcher a single object that proves:

~~~text
this VNext tree is production-bound
~~~

without changing the current launch policy.

## 4. Fixture-free production root shell

New:

`android/app/src/main/kotlin/com/pdig/uivnext/ui/ProductionVNextShell.kt`

The file intentionally has no import/reference to `UiVNextDemoFixture`.

It preserves the five primary product destinations:

~~~text
现在
基础设施
变更
记录
我
~~~

and binds root content only from the production runtime source.

### Now

Shows only:
- pending Human Review count;
- active ChangePlan count;
- active Source count;
- production Timeline items;
- conservative inventory summary.

An empty Timeline explicitly does not mean safe.

### Infrastructure

Shows:
- production `payment_instrument` as payment assets;
- generic `identity_anchor` as generic identities;
- account/service/device/membership/custom counts.

Important:

~~~text
identity_anchor
!=
phone number
!=
email
~~~

until subtype Canonical exists.

### Change

Shows production ChangePlan summaries only.

It does not create/complete/verify actions locally.

### Records

Shows authoritative plan action / verification record states:

~~~text
已记录完成
已验证
待验证
验证失败
~~~

and keeps:

~~~text
done != verified
~~~

### Me

Shows production Reality boundary:
- graph revision;
- active source count;
- pending review count;
- explicit explanation that reference lifecycle facts are absent.

### Sources

Shows actual production SourceInstance projections.

Source existence still does not imply a confirmed Dependency.

## 5. Unsupported production routes fail closed

If the production root shell reaches a route whose screen binding has not been
completed, it renders an explicit unavailable state.

It does not call the reference ContentHost.

This is deliberate.

~~~text
unsupported production screen
→ visible HOLD state

NOT
→ synthetic Preview fallback
~~~

## 6. What R28 intentionally does not coerce

The production shell does not create a fake `UiVNextCard` or
`UiVNextNumber` merely to reuse Preview screens.

For payment assets, only facts already proven by production are shown:
- name;
- issuer when present;
- last4 when present;
- confirmed relationship count.

For identity anchors:
- name;
- confirmed relationship count;
- generic identity status.

It does not invent:
- card region;
- currency;
- network;
- annual fee;
- billing day;
- phone number;
- carrier;
- SIM type;
- keep-number role;
- email subtype;
- recovery-path uniqueness.

## 7. Test contract

New:

`VNextRuntimeDataSourceTest`

It proves:
- Reference cannot return Production Reality;
- Production projects only its supplied authoritative source;
- Production Session rejects a Reference source.

This is a source-level guard against the most dangerous cutover regression:
mixing reference and Reality in one tree.

## 8. Launcher rule remains unchanged

R28 does not modify MainActivity routing.

Current production stays:

~~~text
Production flavor
→ lock / security gate
→ existing PdigApp
~~~

Current Preview stays:

~~~text
Preview flavor
→ UI vNext reference
→ Synthetic Reference data
~~~

Future cutover candidate:

~~~text
Production flavor
→ existing lock / security gate
→ createProductionVNextSession(AppContainer)
→ ProductionVNextShell
~~~

Only after all detailed production screen bindings and runtime/security gates pass.

## 9. Remaining production binding work

Root source injection is no longer an unimplemented concept.

Remaining concrete work is now narrower:

~~~text
A. Card focused detail
   production payment asset + impact + real replace_payment_card plan

B. Identity focused detail
   WAIT for identity-anchor subtype Canonical

C. Secondary details
   account/service/device generic production projections

D. Review
   bind R22 presentation to production review coordinator

E. Establish / Import
   bind R23/R24 forms to existing production authorities

F. Change execution
   bind R20/R21 controls to ProductionVNextActions

G. Persistence / restart / security / rollback
   full production E2E before launcher cutover
~~~

## 10. Stop line

~~~text
R28_PRODUCTION_SOURCE_BOUNDARY = CLOSED
R28_PRODUCTION_ROOT_READ_BINDING = CLOSED

REFERENCE_FIXTURE_IN_PRODUCTION = FORBIDDEN

PRODUCTION_DETAIL_BINDING = PARTIAL
PRODUCTION_MUTATION_UI_BINDING = HOLD
PRODUCTION_LAUNCHER_CUTOVER = HOLD

ANDROID_REFERENCE_FREEZE = HOLD
~~~
