# ANDROID_UI_VNEXT_R20_SOURCE_REPORT

> 2026-10-09 · `feat/android-ui-vnext-translation`
>
> R20 is the **Core Object Detail Closure** on top of R19.
>
> It does not redesign PDIG. It closes the remaining mismatch against the v2.3
> product architecture: Card and Number already had identity + lifecycle + Impact
> Lens, while Account / Email / Device / Service were still collection-only rows.
>
> R20 makes every currently represented core Infrastructure object answer the same
> consumer question:
>
> **“如果它发生变化？”**
>
> without inventing unsupported Change primitives.

## 0. Truth status

```text
R20_FIVE_PRIMARY_IA = PRESERVED
R20_CARD_DETAIL = IMPLEMENTED_SOURCE
R20_NUMBER_DETAIL = IMPLEMENTED_SOURCE
R20_ACCOUNT_DETAIL = IMPLEMENTED_SOURCE
R20_EMAIL_DETAIL = IMPLEMENTED_SOURCE
R20_DEVICE_DETAIL = IMPLEMENTED_SOURCE
R20_SERVICE_DETAIL = IMPLEMENTED_SOURCE
R20_CORE_OBJECT_IMPACT_LENS = IMPLEMENTED_SOURCE
R20_SECONDARY_SEARCH_DIRECT_DETAIL = IMPLEMENTED_SOURCE
R20_SECONDARY_DETAIL_BACK_UP = IMPLEMENTED_SOURCE

CANONICAL_SCHEMA_CHANGE = NONE
DEPMAP_PAYLOAD_CHANGE = NONE
NEW_CHANGE_PRIMITIVE = NONE

ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_CUTOVER = HOLD
```

R20 source-complete does not equal runtime/pixel acceptance.

## 1. Product architecture alignment

The v2.3 architecture defines object identity as:

```text
Card     = financial asset identity
Number   = communication identity
Account  = access / control identity
Email    = communication / recovery identity
Device   = physical access endpoint
Service  = dependency endpoint
```

Before R20:
- Card: focused detail + lifecycle + Impact Lens;
- Number: focused detail + lifecycle + recovery semantics + Impact Lens;
- Account/Email/Device/Service: collection rows only.

After R20:

```text
Object Identity
→ Recorded Context / Confirmed Relations
→ Impact Lens
→ Change / Recovery only when production support exists
```

This removes the product-level asymmetry without fabricating domain capability.

## 2. Account Detail

Account remains an **access / control identity**.

The detail shows only recorded fixture facts:
- provider;
- masked login identifier;
- roles;
- authentication methods;
- recorded recovery route;
- attention state.

It explicitly says that a recorded recovery route does **not** prove:
- uniqueness;
- path independence;
- safety.

No Account Change CTA is displayed because no corresponding executable production
ChangePrimitive is currently exposed.

## 3. Email Detail

Email remains a **communication / recovery identity**.

The detail distinguishes:

```text
uniqueRecoveryPath == true
→ 已确认唯一恢复路径

recoveryOnly == true && uniqueRecoveryPath != true
→ 恢复用途 · 唯一性未知

otherwise
→ 恢复唯一性未知
```

The page never promotes a recovery role into uniqueness.

Privacy masking applies to the focused identity as well as collection/search
surfaces.

## 4. Device Detail

Device remains a **physical access endpoint**.

The page shows:
- platform;
- device kind;
- roles;
- recorded trust state;
- last recorded activity.

It explicitly refuses the shortcut:

```text
multiple devices
→ independent recovery
```

A trusted device may still share a provider, account, physical location or other
failure domain with another route. Independence remains a Continuity-engine
judgment.

## 5. Service Detail

Service remains a **dependency endpoint**.

The page shows:
- consumer service type;
- region context;
- recorded incoming relations;
- source object labels for recorded relations.

Only fixture relations are shown. Zero recorded incoming edges does not become
“nothing depends on this service”.

## 6. Impact Lens parity

R20 extends the same read-only Impact Lens model to:
- Account;
- Email;
- Device;
- Service.

Permanent invariants remain:

```text
null != 0
unknown != safe
recovery use != unique recovery
path count != independent path count
UI list count != Continuity proof
```

The synthetic lens remains a reference projection, not a replacement for
production `AppContainer.impactFor(nodeId)`.

## 7. Navigation / search

Collection rows now open focused details directly.

Search results for:
- Account;
- Email;
- Device;
- Service

also open the exact object detail instead of only navigating to the collection.

Navigation contract:

```text
System Back = actual previous page

Account Detail → Up → Accounts
Email Detail   → Up → Emails
Device Detail  → Up → Devices
Service Detail → Up → Services
```

The Infrastructure primary and corresponding secondary collection remain selected
on wider layouts.

## 8. Privacy

R20 fixes a cross-width privacy gap:
- Account identifiers respect workspace masking on adaptive collection surfaces;
- Email addresses respect workspace masking on adaptive collection surfaces;
- Search result subtitles respect masking;
- focused details respect masking.

Presentation masking still does not mutate Canonical identity.

## 9. No ghost capability

R20 deliberately does **not** add generic “开始变更” buttons to long-tail object
details.

A small boundary note explains that executable Change appears only when production
supports the relevant scenario.

This follows the v2.3 rule:

```text
UI may expose a Change CTA
IFF
corresponding ChangePrimitive / Scenario is production-supported
```

## 10. Runtime contracts added

Source/unit:
- secondary-object Impact Lens tests;
- secondary detail hierarchy/selection test;
- Search → exact secondary detail test.

Instrumentation:
- focused secondary detail runtime + Back flow;
- tablet secondary detail + Impact Lens;
- phone pixel journey captures Account / Email / Device / Service details.

The phone evidence journey now requires:
- object-specific identity heading;
- Impact Lens;
- explicit unknown-relations language;
- hierarchical Up back to the collection.

## 11. Production boundary

R20 details still render synthetic reference models in Preview.

Production VNext remains gated behind:
- production source injection;
- governed identity subtype mapping;
- Android Reference Freeze;
- security/runtime validation.

The production read seam already carries:
- confirmed inventory;
- confirmed active dependencies;
- timeline;
- Impact;
- plans;
- pending review;
- source coverage;
- canonical card issuer / last4;
- conservative object surface classification.

R20 does not bypass that boundary.

## 12. Remaining Android closure

After source compilation/tests are green, remaining work is evidence, not another
UI redesign:

```text
exact-head API36 phone runtime
exact-head API36 tablet runtime
GPU first-frame / runtime-state evidence
interaction evidence
fresh screenshots
human pixel review
ANDROID_REFERENCE_FREEZE decision
```

Only then may production VNext screen binding / launcher cutover move forward.

## 13. Acceptance stop line

```text
CORE_OBJECT_DETAIL_DESIGN = COMPLETE
CORE_OBJECT_DETAIL_SOURCE = IMPLEMENTED
CURRENT_HEAD_COMPILE_TEST = MUST_BE_GREEN
RUNTIME_PIXEL_EVIDENCE = REQUIRED
HUMAN_VISUAL_ACCEPTANCE = REQUIRED
ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_CUTOVER = HOLD
```
