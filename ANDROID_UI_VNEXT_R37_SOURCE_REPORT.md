# ANDROID_UI_VNEXT_R37_SOURCE_REPORT

> 2026-10-10 · `feat/android-ui-vnext-translation`
>
> R37 closes the governed identity-anchor subtype **read/classification** chain across
> Canonical spec, Kotlin/Swift/ArkTS decoding, conformance fixtures, Android Production
> VNext object projection, Number/Email navigation, search, and replace-phone target
> authority.
>
> R37 does **not** claim Android Reference Freeze, Production release cutover, raw
> phone/email identifier-value Canonical support, or atomic Manual Number/Email create.

---

## 0. Exact source status

```text
ANDROID_REFERENCE_VISUAL_SOURCE = R34
PRODUCT_ARCHITECTURE_CONTROL = R35
PRODUCTION_RELEASE_CUTOVER_CONTROL = R36
GOVERNED_IDENTITY_PROFILE_CONTROL = R37

FIVE_PRIMARY_IA = PRESERVED
ME_PRIMARY_DESTINATION = PRESERVED

IDENTITY_SUBTYPE_CANONICAL_CONTRACT = IMPLEMENTED_SOURCE
IDENTITY_PROFILE_KOTLIN_DECODER = IMPLEMENTED_SOURCE
IDENTITY_PROFILE_SWIFT_DECODER = IMPLEMENTED_SOURCE
IDENTITY_PROFILE_ARKTS_DECODER = IMPLEMENTED_SOURCE
IDENTITY_PROFILE_CONFORMANCE_CASES = 5_REGISTERED

PRODUCTION_PHONE_CLASSIFICATION = IMPLEMENTED_SOURCE
PRODUCTION_EMAIL_CLASSIFICATION = IMPLEMENTED_SOURCE
GENERIC_IDENTITY_FAIL_CLOSED = IMPLEMENTED_SOURCE
REPLACE_PHONE_TARGET_SUBTYPE_GATE = IMPLEMENTED_SOURCE

RAW_PHONE_EMAIL_IDENTIFIER_VALUE = HOLD
MANUAL_NUMBER_EMAIL_ATOMIC_CREATE = HOLD
IDENTITY_CONTEXT_CANONICAL = HOLD
REGION_FACT_CANONICAL = HOLD
LIFECYCLE_CANONICAL = HOLD

CURRENT_PRODUCTION_RELEASE_DEFAULT = LEGACY
PRODUCTION_VNEXT_RELEASE_CUTOVER = HOLD
ANDROID_REFERENCE_FREEZE = HOLD
```

Exact-head CI/runtime status must be read from GitHub Actions. This report does not
turn a source commit into runtime evidence.

---

## 1. Problem R37 resolves

Before R37, Production VNext correctly refused to guess:

```text
identity_anchor
? phone number
? email
? other identity
```

That was safe, but it meant Production Number/Email remained unavailable even though
the product design was already complete.

The accepted R37 solution is not heuristic classification. It is a governed profile
inside the existing cross-platform Node fields envelope:

```json
{
  "identity_anchor_profile": {
    "version": 1,
    "subtype": "phone_number | email_address | other_identity",
    "verification_basis_type": "user_confirmed | authoritative_source",
    "confirmed_at": "ISO-8601",
    "evidence_refs": []
  }
}
```

Permanent rule:

```text
bare fields_json.subtype
!= subtype authority
```

---

## 2. Canonical registration

R37 registers:

```text
IdentityAnchorSubtype
  phone_number
  email_address
  other_identity
```

and reuses the governed:

```text
VerificationBasisType
  user_confirmed
  authoritative_source
```

The structured contract is declared in:
- `spec/domain/domain.json`
- `spec/schema/logical-schema-v4.json`

Physical storage remains:

```text
nodes.fields_json.identity_anchor_profile
```

This intentionally avoids inventing an Android-only column or ungoverned free-form
key.

Graph payload/schema physical version is not bumped solely because the existing
opaque fields envelope can carry the profile. The semantic meaning is still
Canonical and codegen/conformance-gated.

---

## 3. Cross-platform fail-closed decoders

Implemented source:
- Kotlin: `android/core/.../IdentityAnchorProfile.kt`
- Swift: `ios/Sources/PDIGCore/Domain/IdentityAnchorProfile.swift`
- ArkTS: `harmony/.../domain/IdentityAnchorProfile.ets`

A profile activates only when all required confirmation semantics are valid:

```text
kind == identity_anchor
AND profile.version == 1
AND subtype is registered
AND verification_basis_type is registered
AND confirmed_at is present/nonblank
AND evidence_refs, if present, is a string list
```

Otherwise:

```text
confirmedIdentityAnchorProfile = null
→ generic identity
```

No regex/name/provider/edge inference exists in this path.

---

## 4. Conformance expansion

R37 adds five canonical cases:

```text
identity-profile-confirmed-phone
identity-profile-confirmed-email-authoritative
identity-profile-bare-subtype-rejected
identity-profile-invalid-subtype-rejected
identity-profile-nonidentity-rejected
```

Manifest total becomes:

```text
133 canonical cases
```

CI repair in this round also corrects three stale test/fixture gates discovered on
the first R37 run:

1. Android core identity tests now use configured JUnit5 imports.
2. iOS conformance accounting is manifest-driven instead of hardcoding historical
   total 128.
3. Harmony embedded fixtures are re-synced to the 133-case manifest.

These fixes preserve the gates; no workflow was weakened.

---

## 5. Production object projection

Production VNext now classifies Reality as:

```text
payment_instrument
→ PAYMENT_ASSET

identity_anchor + confirmed PHONE_NUMBER profile
→ PHONE_IDENTITY

identity_anchor + confirmed EMAIL_ADDRESS profile
→ EMAIL_IDENTITY

identity_anchor + OTHER / missing / invalid / legacy bare subtype
→ IDENTITY_ANCHOR_GENERIC
```

The production projection carries:
- subtype;
- confirmation basis;
- confirmed_at;
- evidence refs.

It does **not** invent:
- raw phone/email identifier value;
- carrier/provider;
- SIM/eSIM;
- region;
- keep-alive data;
- authentication/recovery role;
- unique recovery path.

---

## 6. Production Number / Email UX unlocked safely

Production Infrastructure can now show real counts for:
- Numbers;
- Emails.

Production Number:
- list from confirmed PHONE_NUMBER profiles only;
- detail uses real object + confirmed relationships + authoritative Impact;
- privacy masking hides the visible identity name;
- confirmation basis is visible;
- Replace Phone entry is available.

Production Email:
- list from confirmed EMAIL_ADDRESS profiles only;
- detail uses real object + confirmed relationships + authoritative Impact;
- privacy masking hides the visible identity name;
- confirmation basis is visible.

Generic identities remain a separate explicit bucket.

This is not Preview fixture reuse.

---

## 7. Search

Production search now understands the authoritative surface classification.

Search result labels can distinguish:
- 支付工具;
- 手机号身份;
- 邮箱身份;
- 通用身份对象;
- accounts/devices/services.

Phone/Email hits open their respective production details.

Privacy masking still protects visible identity names.

A missing search result remains:

> not found in current confirmed Reality

not:

> externally nonexistent.

---

## 8. Replace Phone authority is now double-gated

UI routing is not subtype authority.

New production path:

```text
Production Number detail
→ ProductionPhoneChangeEntry
→ VNextChangeActionGateway
→ AppContainer.createPlanForScenario
→ PlanRepository.validateScenarioTarget
→ create real ChangePlan
```

`replace_phone_number` requires:

```text
target.kind == identity_anchor
AND
confirmed identity_anchor_profile.subtype == PHONE_NUMBER
```

The PlanRepository rejects:
- missing target;
- archived target;
- wrong Node kind;
- unconfirmed/malformed profile;
- wrong confirmed subtype.

Thus even a UI bug cannot coerce a generic identity into a phone plan.

The same subject-kind contract is now explicit in `ScenarioRegistry`:
- payment scenarios → payment_instrument;
- replace_phone_number → identity_anchor(phone_number).

---

## 9. Privacy / security boundary

Subtype does not make the raw identifier safe to display.

Permanent separation:

```text
confirmed subtype
!= confirmed raw identifier value
!= recovery role
!= current availability
!= independent recovery path
```

Production privacy masking applies to:
- confirmed phone identities;
- confirmed email identities;
- generic identities.

The secure Production VNext host / LockGate / FLAG_SECURE policies from R33 remain
unchanged.

---

## 10. Five-primary IA remains unchanged

R37 does not demote `我`.

Primary destinations remain:

```text
现在
基础设施
变更
记录
我
```

The R37 identity work is an Infrastructure/Change capability expansion only.

---

## 11. Tests added/updated

Source/unit:
- `IdentityAnchorProfileTest`
- `ScenarioSubjectContractTest`
- `ProductionVNextReadModelTest`

Instrumentation contract:
- `ProductionVNextShellContractTest`

New/updated assertions cover:
- confirmed phone classification;
- confirmed email classification;
- bare subtype rejection;
- malformed subtype rejection;
- generic fallback;
- Production Number/Email list/detail;
- privacy masking;
- phone Change entry;
- scenario subject-kind/subtype metadata.

Conformance:
- 5 new identity-profile fixtures;
- Swift/ArkTS/Kotlin execution path.

---

## 12. Remaining identity work — intentionally not faked

### 12.1 Raw identifier value

Subtype answers:

```text
what kind of identity anchor is this?
```

It does not yet authoritatively answer:

```text
what is the raw phone number/email value?
```

R37 therefore may still display the current Node name as the user-visible label,
subject to privacy masking, but does not reinterpret that label as a canonical
identifier-value field.

### 12.2 Manual Number / Email creation

Still HOLD because successful creation must be logically atomic:

```text
create Node(identity_anchor)
+
write valid governed profile
+
single authoritative graphRevision transition
+
no implicit dependency creation
```

The existing runtime-creatable Node set must not be widened with a UI-only shortcut.

### 12.3 Identity Context / Region / Lifecycle

Still separate Canonical layers:
- IdentityContext membership;
- RegionFact;
- MaintenanceFact / MaintenanceSchedule;
- AccessFactor / RecoveryFactor.

Subtype activation does not silently activate any of them.

---

## 13. Acceptance chain

R37 source completion is not final acceptance.

Required exact-head evidence remains:

```text
CI green
iOS green
Harmony fixture/conformance green
Preview APK green
API36 phone pixel proof
API36 tablet pixel proof
GPU runtime evidence
Production secure rehearsal
human pixel acceptance
Android Reference Freeze decision
release cutover approval
```

The debug Production VNext rehearsal may prove source/runtime behavior but cannot
self-approve release.

---

## 14. Stop line

```text
R37_IDENTITY_PROFILE_DESIGN = CLOSED
R37_CANONICAL_PROFILE_READ = SOURCE_IMPLEMENTED
R37_CROSS_PLATFORM_DECODERS = SOURCE_IMPLEMENTED
R37_CONFORMANCE_EXPANSION = REGISTERED
R37_PRODUCTION_PHONE_EMAIL_READ = SOURCE_IMPLEMENTED
R37_REPLACE_PHONE_TARGET_AUTHORITY = SOURCE_IMPLEMENTED

RAW_IDENTIFIER_VALUE = HOLD
MANUAL_NUMBER_EMAIL_CREATE = HOLD
ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_RELEASE_CUTOVER = HOLD
```
