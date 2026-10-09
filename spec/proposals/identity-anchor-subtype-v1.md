# Identity Anchor Subtype v1 — Canonical Proposal

> Status: **PROPOSED_SCHEMA / NOT_IMPLEMENTED**
>
> Date: 2026-10-09
>
> Motivation: R19 can safely render `payment_instrument` as a financial asset,
> but current production `identity_anchor` is too coarse to prove that an object
> is a phone number or an email address. The Android reference must not infer
> subtype from names, prefixes, providers, or UI shape.

## 1. Problem

Canonical already defines:

```text
NodeKind.IDENTITY_ANCHOR
ChangePrimitive subjectSubtype = "phone_number"
replace_phone_number = REPLACE × identity_anchor(phone_number)
```

but `Node` does not currently carry a governed subtype field.

That leaves an unsafe gap:

```text
identity_anchor
  ? phone number
  ? email
  ? another identity anchor
```

A production UI cannot legally turn every `identity_anchor` into the Number
experience merely because a name happens to look like `+86 ...`.

## 2. Proposed semantic field

Add a nullable, typed subtype to `Node`.

Conceptual shape:

```text
Node
  ...
  kind
  subtype?       // governed enum; null = unknown / not applicable
  ...
```

For v1, only `identity_anchor` uses the subtype vocabulary.

Proposed enum:

```text
IdentityAnchorSubtype
  PHONE_NUMBER
  EMAIL_ADDRESS
  OTHER_IDENTITY
```

The final schema may encode this as a general `NodeSubtype` union if other
NodeKinds later need typed subcategories. The semantic rule is more important
than the storage representation.

## 3. Truth rules

```text
kind = identity_anchor, subtype = PHONE_NUMBER
→ may bind the consumer Number surface

kind = identity_anchor, subtype = EMAIL_ADDRESS
→ may bind the consumer Email identity surface

kind = identity_anchor, subtype = null / OTHER_IDENTITY
→ generic identity surface
```

Forbidden inference:

- regex on the node name;
- leading `+` / country code;
- provider/carrier-looking text;
- `recovers` or `authenticates` edges;
- a scenario being opened for the node;
- UI preset/theme;
- locale/region.

A phone-looking label is not subtype evidence.

## 4. Confirmation / evidence

Subtype is Personal Reality.

Allowed authority transitions:

```text
manual user creation/confirmation
authoritative source (future governed source)
Observation → Proposal → Review → Confirm
```

Machine extraction may propose:

```text
candidate subtype = PHONE_NUMBER
```

but cannot write the confirmed subtype by itself.

## 5. Migration

Existing databases:

```text
all existing identity_anchor nodes
→ subtype = null
```

No migration may inspect names or `fields_json` and auto-classify them.

This is intentionally conservative. Users can later confirm subtype through a
review flow.

## 6. Change primitive integration

The existing scenario:

```text
replace_phone_number
subjectKind = identity_anchor
subjectSubtype = phone_number
```

should become enforceable against Reality.

Before plan creation:

```text
target.kind == identity_anchor
AND target.subtype == PHONE_NUMBER
```

Otherwise:

```text
REJECT / NEEDS_REVIEW
```

Do not silently coerce a generic anchor into a phone number merely because the
user entered the phone-replacement scenario.

## 7. Failure-domain integration

This proposal does not change FailureDomain semantics.

A confirmed phone node may participate in:

```text
FailureDomainKind.PHONE_NUMBER
```

but:

```text
PHONE_NUMBER failure domain
!= automatic proof that every member node is a phone subtype
```

The domain still requires explicit confirmed Reality.

## 8. Android R19 production binding

Until this proposal is accepted and implemented:

```text
ProductionVNextReadModel
identity_anchor → IDENTITY_ANCHOR_GENERIC
```

After implementation:

```text
identity_anchor + PHONE_NUMBER
→ ProductionNumberIdentity
→ Number list/detail/Impact Lens/change entry

identity_anchor + EMAIL_ADDRESS
→ ProductionEmailIdentity

identity_anchor + null
→ generic identity
```

R19 synthetic reference fixtures may continue to demonstrate the intended phone
experience, but they are not production subtype evidence.

## 9. Privacy

Subtype does not make the raw identifier safe to display.

Phone/email values remain subject to:
- privacy masking;
- screenshot policy;
- local alias presentation;
- secure-window policy where applicable.

A user alias remains Presentation Layer and never changes subtype.

## 10. Cross-platform implementation order

```text
1. Approve spec vocabulary
2. Allocate schema/payload version if required
3. DB migration
4. serialization + .depmap migration
5. codegen
6. golden fixtures
7. negative fixtures
8. Kotlin / Swift / ArkTS / Desktop conformance
9. creation/review UI
10. production VNext binding
```

No Android-only `fields_json["subtype"]` shortcut.

## 11. Required conformance cases

Positive:
- confirmed phone subtype round-trips across all runtimes;
- confirmed email subtype round-trips;
- subtype survives backup/restore.

Negative:
- phone-looking node name + subtype null stays generic;
- email-looking node name + subtype null stays generic;
- recovery edge does not imply phone/email;
- proposal does not become confirmed subtype;
- unknown enum fails closed according to schema policy.

Migration:
- old payload identity anchors migrate to subtype null;
- byte/semantic compatibility rules are explicit for every supported payload
  version.

## 12. Acceptance effect

This proposal closes one specific production gap:

```text
coarse identity_anchor
→ governed consumer identity subtype
```

It does **not** add:
- carrier detection;
- live SIM/eSIM state;
- phone ownership verification;
- provider account synchronization;
- automatic recovery-path confirmation.

Those remain separate capabilities.

## 13. Current R19 decision

```text
IDENTITY_SUBTYPE_DESIGN = PROPOSED
CANONICAL_CHANGE = NOT_IMPLEMENTED
ANDROID_REFERENCE = MAY_DEMONSTRATE_SYNTHETIC_PHONE_UI
PRODUCTION_IDENTITY_ANCHOR = GENERIC_UNTIL_CONFIRMED
```
