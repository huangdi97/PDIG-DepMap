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

R37 freezes the storage representation without adding a platform-private column:

~~~text
logical field:
  Node.identityAnchorProfile.subtype : IdentityAnchorSubtype

physical storage:
  nodes.fields_json.identity_anchor_profile = {
    "version": 1,
    "subtype": "phone_number | email_address | other_identity",
    "verification_basis_type": "user_confirmed | authoritative_source",
    "confirmed_at": "ISO-8601",
    "evidence_refs": []
  }
~~~

This is a **Canonical governed structured field** inside the existing cross-platform
`Node.fields` object. It is not a free-form Android convention.

A bare legacy/prototype key such as:

~~~json
{"subtype":"phone_number"}
~~~

is **not confirmation authority** and must never activate the Number surface.

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

~~~text
all existing identity_anchor nodes
without a valid governed identity_anchor_profile
→ subtype = unknown
→ generic identity surface
~~~

Migration/activation MUST NOT inspect:
- node name;
- provider/carrier-like text;
- phone/email syntax;
- edges;
- legacy bare `fields_json.subtype`.

Historical prototype data may contain `fields_json.subtype`. R37 treats that key
as ungoverned metadata. It may become a **review candidate**, but it cannot be
silently copied into `identity_anchor_profile`.

Because the governed profile lives inside the existing `fields_json` payload
column, no physical column migration is required solely for subtype. The activation
still requires Canonical/codegen/conformance/runtime gates because the **meaning**
is new even when the bytes fit the existing storage envelope.

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


## 14. Identifier value is separate from subtype

Subtype answers **what kind of anchor this is**. It must not be overloaded with the
identifier value itself.

Recommended future typed profile shape:

~~~text
IdentityAnchorProfile
  nodeId
  subtype
  identifierState
  identifierValue?
  comparisonKey?
  providerLabel?
  regionCode?
  source
  evidenceRefs[]
  confirmedAt?
  updatedAt
~~~

`identifierState`:

~~~text
CONFIRMED_VALUE
CONFIRMED_SUBTYPE_ONLY
UNKNOWN
~~~

This lets a user confirm “this object is a phone identity” without being forced to
store the raw number.

R37 chooses the typed Node substructure for v1:

~~~text
Node.fields.identity_anchor_profile
~~~

The physical JSON envelope is shared by all runtimes and already round-trips in
graph payload v3; the **profile schema and provenance requirements** are the
Canonical contract.

Permanent invariant:

~~~text
bare/free-form fields_json subtype
!= Canonical subtype authority

governed identity_anchor_profile
+ valid enum
+ confirmation basis
+ confirmed_at
= subtype Reality
~~~

## 15. Normalization and duplicate review

### Phone

- preserve the user-confirmed value separately from any comparison form;
- E.164 normalization is allowed only when enough confirmed dialing context exists;
- locale/current SIM/provider lookup must not silently supply missing Reality;
- number recycling means equal values do not prove continuous ownership.

### Email

- preserve the user-confirmed address;
- domain comparison may be case-insensitive;
- do not globally rewrite the local part;
- provider-specific dot/plus alias rules are Provider Knowledge, not universal
  Canonical semantics.

Do not enforce silent semantic merging on a normalized value.

~~~text
same subtype + same comparison key
→ duplicate candidate / Human Review
→ user decides merge / keep separate
~~~

## 16. Recovery semantics are not subtype semantics

Permanent rule:

~~~text
PHONE_NUMBER / EMAIL_ADDRESS
!= recovery role
!= unique recovery path
!= current availability
~~~

A confirmed `recovers` edge can establish a recovery relationship. It still does
not prove uniqueness or path independence.

Likewise, during a Recovery Incident:

~~~text
PHONE_NUMBER
!= SMS currently reachable

EMAIL_ADDRESS
!= mailbox currently accessible
~~~

Availability belongs to explicit incident/runtime evidence.

## 17. Manual Establish unlock

R24 correctly keeps Number/Email manual creation non-executable while subtype is
ungoverned.

After Canonical implementation, the authoritative transaction should be:

~~~text
Manual Number
→ create Node(identity_anchor)
→ create confirmed subtype PHONE_NUMBER
→ graphRevision bump once
→ no Dependency auto-created

Manual Email
→ create Node(identity_anchor)
→ create confirmed subtype EMAIL_ADDRESS
→ graphRevision bump once
→ no Dependency auto-created
~~~

Node creation and subtype confirmation must be atomic at the logical Reality
boundary. A half-created generic node must not be presented as a successfully saved
Number/Email.

## 18. Lifecycle / Identity Context composition

Subtype does not own lifecycle or personal grouping.

~~~text
IdentityAnchorProfile(PHONE_NUMBER)
+
MaintenanceSchedule(NUMBER_KEEP_ALIVE)
+
confirmed Dependency graph
+
IdentityContext membership
+
FailureDomain
= richer Number experience
~~~

Each layer retains separate authority.

Example query:

~~~text
subtype = PHONE_NUMBER
∩ IdentityContext = UK Financial
∩ Region = GB
~~~

is a query intersection, not a new NodeKind or Dependency.

## 19. External security research alignment

External guidance supports keeping identifier type, authenticator lifecycle and
recovery authority separate.

- NIST SP 800-63-4 (final July 2025) treats authenticator management and recovery as
  explicit identity lifecycle processes:
  https://csrc.nist.gov/pubs/sp/800/63/4/final
- FIDO guidance distinguishes synced passkeys, device-bound credentials, backup
  authenticators and account recovery:
  https://fidoalliance.org/white-paper-displace-password-otp-authentication-with-passkeys/
- Apple Recovery Contacts have provider-specific establishment and recovery
  semantics beyond an email/phone label:
  https://support.apple.com/guide/security/account-recovery-contact-security-secafa525057/web
- Google explicitly treats recovery phone/email as maintained recovery information:
  https://support.google.com/accounts/answer/17299765

These sources are design inputs only. They do not create PDIG Reality.

## 20. Expanded acceptance matrix

Additional required cases:

~~~text
IAS-01 confirmed PHONE_NUMBER maps to Number surface
IAS-02 confirmed EMAIL_ADDRESS maps to Email surface
IAS-03 null subtype stays Generic Identity
IAS-04 proposal subtype never changes production surface
IAS-05 CONFIRMED_SUBTYPE_ONLY is valid without raw identifier
IAS-06 phone comparison normalization requires confirmed dialing context
IAS-07 email local part is not globally rewritten
IAS-08 duplicate comparison creates review, not silent merge
IAS-09 subtype profile on non-identity node is rejected
IAS-10 phone/email subtype creates no recovery edge
IAS-11 recovery edge creates no unique-recovery finding
IAS-12 presentation alias rename changes no subtype/value
IAS-13 legacy migration performs zero heuristic classification
IAS-14 manual Node + subtype confirmation is atomic
IAS-15 confirmed subtype mutation bumps graphRevision exactly once
IAS-16 raw identifier is absent from logs/analytics evidence
~~~

Updated stop line:

~~~text
IDENTITY_SUBTYPE_DESIGN = COMPLETE
IDENTITY_SUBTYPE_STORAGE_MAPPING = FROZEN_R37
IDENTITY_SUBTYPE_ENUM = REGISTERED_IN_CANONICAL_SPEC
IDENTITY_VALUE_NORMALIZATION_DESIGN = COMPLETE
MANUAL_NUMBER_EMAIL_CREATE_DESIGN = COMPLETE
BARE_FIELDS_JSON_SUBTYPE = NOT_AUTHORITY
CANONICAL_RUNTIME_ACTIVATION = HOLD
PRODUCTION_PHONE_EMAIL_MAPPING = HOLD
~~~
