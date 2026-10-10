# ANDROID_UI_VNEXT_R26_SOURCE_REPORT

> 2026-10-10 · feat/android-ui-vnext-translation
>
> R26 is a **continuity-substrate and roadmap design closure** after the visible
> R20–R25 product surfaces. It deliberately adds almost no new visible UI because
> the remaining v2.3 concepts require Canonical or solver authority before they can
> honestly appear in the product.
>
> This report does **not** claim fresh runtime PASS.

## 0. Truth status

~~~text
ANDROID_UI_VNEXT_SOURCE = R26
FIVE_PRIMARY_IA = PRESERVED

CURRENT_VISIBLE_PRODUCT_UX = SOURCE_COMPLETE
V2_3_ROADMAP_DESIGN_CLOSURE = COMPLETE

ACCESS_RECOVERY_FACTOR_DESIGN = COMPLETE
SECRET_LOCATOR_DESIGN = COMPLETE
RECOVERY_PREPAREDNESS_UX = COMPLETE_HIDDEN
DEVICE_CONTINUITY_DESIGN = COMPLETE_HIDDEN
DIGITAL_RESOURCE_CONTINUITY_DESIGN = COMPLETE_HIDDEN
TRUSTED_HANDOFF_DESIGN = COMPLETE_HIDDEN

CAPABILITY_AUTHORITY_MATRIX = IMPLEMENTED
GHOST_CAPABILITY_GUARD = IMPLEMENTED

CANONICAL_SCHEMA_CHANGE = NONE
DEPMAP_PAYLOAD_CHANGE = NONE
NEW_PRIMARY_TAB = NONE

FRESH_R26_BUILD = NOT_RUN
FRESH_R26_UNIT_TESTS = NOT_RUN
FRESH_R26_INSTRUMENTATION = NOT_RUN
FRESH_R26_PHONE_PIXELS = NOT_RUN
FRESH_R26_TABLET_PIXELS = NOT_RUN
FRESH_R26_HUMAN_ACCEPTANCE = NOT_RUN

ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_CUTOVER = HOLD
~~~

## 1. Why R26 exists

R20–R25 closed the visible consumer loop:

~~~text
Understand objects
→ Impact
→ Change
→ Records
→ Human Review
→ Import
→ Manual Establish
→ Manual Relationship
~~~

But v2.3-R1 also froze deeper continuity concepts:

~~~text
AccessFactor / RecoveryFactor
SecretLocator
Device Continuity
Incident Recovery
Digital Resource Continuity
Trusted Handoff
~~~

Several of those existed only as master-document paragraphs/roadmap lines. If left
that way, future implementation could invent incompatible local models.

R26 converts them into governed proposals and explicit capability gates.

## 2. AccessFactor / RecoveryFactor

New:
- \`spec/proposals/access-recovery-factor-v1.md\`

Key decision:

~~~text
Factor != Node
Factor != Dependency
Factor != FailureDomain
Factor != current availability
~~~

Factors explain **how** authentication/recovery capability is carried.

Initial design handles:
- password metadata;
- passkey;
- TOTP;
- security key;
- SMS/email OTP;
- push approval;
- recovery code;
- recovery contact;
- provider re-proof.

No secret value is stored.

### Independence boundary

~~~text
factor count != independent path count
~~~

SMS, TOTP and a passkey can all share the same physical device/provider failure
domain.

### Passkey boundary

R26 distinguishes:
- device-bound;
- provider-synced;
- roaming hardware;
- unknown portability.

A synced passkey can survive device loss while still sharing provider-account risk.

## 3. SecretLocator

New:
- \`spec/proposals/secret-locator-v1.md\`

PDIG may store:

~~~text
Recovery code exists
Stored in password manager
Last verified at ...
~~~

PDIG must not store:

~~~text
password
TOTP seed
full recovery code
private key
seed phrase
CVV
session/API token
~~~

Permanent rule:

~~~text
SecretLocator != Secret Store
SecretLocator != Recovery Path
~~~

No current/future UI is allowed to expose “copy secret” from PDIG.

## 4. Recovery Preparedness

New:
- \`spec/ui-vnext/RECOVERY_PREPAREDNESS_UX_CONTRACT.md\`

Future hierarchy:

~~~text
我
→ 恢复准备
~~~

It is a child workspace, not a sixth primary destination.

The UX is frozen around:
- confirmed factors;
- shared failure domains;
- stale/needs-review information;
- SecretLocator metadata;
- contextual improvement actions.

No safety score.

The route stays hidden until Factor Canonical exists.

## 5. Device Continuity

New:
- \`spec/proposals/device-continuity-v1.md\`

Future primitive:

~~~text
replace_device
= REPLACE × device
~~~

The design covers:
- passkeys;
- TOTP;
- push approval;
- eSIM/communication;
- password-manager access;
- provider bootstrap accounts;
- correlated device failure;
- RecoveryCycle;
- SecretLocator context;
- Action DAG;
- Make-Before-Break;
- verification before old-device retirement.

Planned device replacement remains separate from:

~~~text
lose_device
compromise_device
~~~

which belong to Incident Recovery.

No reference route is exposed yet.

## 6. Digital Resource Continuity

New:
- \`spec/proposals/digital-resource-continuity-v1.md\`

Future v0.6+ design now covers:
- domain registration/control;
- DNS;
- source repositories / organizations;
- cloud/hosting/data resources;
- future control/hosting/data/deployment/dns capability vocabulary;
- migrate/transfer/region-change primitives.

PDIG remains the continuity layer, not the provider control panel.

Secret-like material such as domain transfer Auth-Codes remains outside PDIG
plaintext storage.

## 7. Trusted Handoff

New:
- \`spec/proposals/trusted-handoff-v1.md\`

Future v0.7+ design now distinguishes:

~~~text
TrustedParty
ProviderHandoffArrangement
TrustedHandoffPlan
HandoffPackage
~~~

from:
- credentials;
- recovery factors;
- legal authority;
- secrets.

External provider programs such as legacy/successor/inactivity arrangements are
ProviderPolicy/Personal Reality inputs; provider support never proves user setup.

Permanent exclusions:
- secret escrow;
- master-password handoff;
- autonomous dead-man switch;
- legal-will positioning.

## 8. Identity-anchor subtype design completed

Existing:
- \`spec/proposals/identity-anchor-subtype-v1.md\`

R26 audit expanded/finalized:
- subtype separate from identifier value;
- subtype-only confirmation allowed;
- conservative phone/email normalization;
- duplicate review, no silent merge;
- recovery role / uniqueness / availability remain separate;
- atomic manual Number/Email future creation design;
- no Android-only \`fields_json\` shortcut.

Current production mapping remains:

~~~text
identity_anchor
→ generic identity
~~~

until Canonical subtype authority exists.

## 9. Capability / authority matrix

New source:
- \`android/app/src/main/kotlin/com/pdig/uivnext/capability/VNextCapabilityMatrix.kt\`
- \`android/app/src/test/kotlin/com/pdig/uivnext/capability/VNextCapabilityMatrixTest.kt\`
- \`spec/ui-vnext/CAPABILITY_AUTHORITY_MATRIX.md\`

It freezes three different questions:

~~~text
design valid?
reference visible?
production executable?
~~~

Current examples:

~~~text
File Import
  reference visible
  production authority exists
  Preview still read-only

Manual Create
  reference visible
  production authority not exposed

Manual Relationship
  reference visible
  production authority not exposed

Identity Context
  hidden until Canonical

Recovery Preparedness
  hidden until Factor Canonical

Device Continuity
  hidden until prerequisites

Recovery Incident
  hidden until solver

Digital Resource Continuity
  hidden until future Canonical

Trusted Handoff
  hidden until future Canonical
~~~

This prevents source/design progress from producing ghost buttons.

## 10. Existing visible screens now consume authority truth

R26 wires the capability matrix into explanatory authority copy for:
- R22 Human Review;
- R23 Import;
- R24 Manual Establish;
- R25 Manual Relationship.

Preview remains isolated/read-only even when a production authority already exists.

This is intentional:

~~~text
Production authority available
!= Preview may execute it
~~~

## 11. Me / Now / Records boundary correction

A fresh product-ownership audit found stale R19 Me links:
- “待处理” routed to Records;
- “时间节点” routed to Records;
- active change row jumped directly to Change Phone.

R22 had already redefined:

~~~text
Now
= current attention / upcoming / active work

Change
= change work center

Records
= happened / verified / evidence
~~~

R26 repairs Me accordingly:
- attention → Now;
- upcoming → Now;
- active changes → Change;
- Records remains history/evidence;
- Me remains the fifth primary root.

## 12. No redesign of already-implemented continuity algorithms

Repository audit confirms current core already has:
- FailureDomain;
- RecoveryCycle;
- ProviderPolicy;
- Action DAG;
- relation registry;
- v0.3 Change/Scenario infrastructure.

R26 does not fork those algorithms into UI code.

New substrate proposals must compose with them.

## 13. Establish capability state after R26

~~~text
File Import
  UX complete
  production authority existing

Manual Object
  UX complete
  production VNext authority not exposed

Manual Relationship
  UX complete
  production VNext authority not exposed

Human Review
  UX complete
  production authority existing
~~~

The missing object/relation gateways are engineering authority gaps, not unresolved
consumer UX.

## 14. Five-primary IA remains permanent

~~~text
现在
基础设施
变更
记录
我
~~~

R26 adds no root.

Future features enter contextually:
- Recovery Preparedness → 我;
- Device Continuity → Device Detail / Change;
- Recovery Incident → contextual incident entry;
- Digital Resource Continuity → Infrastructure / Change;
- Trusted Handoff → 我.

Capability-growth tests keep the primary list fixed at five.

## 15. Design closure index

New:
- \`spec/ui-vnext/PDIG_VNEXT_DESIGN_CLOSURE_MATRIX.md\`

It maps every major v2.3 current/future concept to:
- current source implementation;
- frozen future design;
- Canonical gate;
- solver gate;
- explicit forbidden behavior.

Within the scope explicitly stated by v2.3-R1, no known roadmap concept remains only
an ambiguous UI idea.

## 16. External research incorporated

R26 design uses external official material only as constraint/reference, never as
Personal Reality.

Relevant categories:
- NIST authenticator/recovery lifecycle;
- FIDO passkey/device-bound/synced recovery properties;
- Apple recovery/legacy-contact provider semantics;
- Google recovery/inactivity arrangements;
- GitHub account-recovery/successor policy;
- ICANN/registrar domain transfer constraints.

Provider documentation can populate future ProviderPolicy. It cannot auto-create
user configuration.

## 17. What R26 intentionally does not implement

~~~text
Factor Canonical tables
SecretLocator Canonical table
identity subtype schema
Maintenance schema
Identity Context schema
replace_device scenario
Recovery Solver
digital_resource NodeKind/subtypes
future control/hosting/data capabilities
Trusted Handoff storage
server relay
manual Node production VNext gateway
manual Dependency production VNext gateway
production launcher cutover
~~~

These are no longer undefined designs; they are explicit engineering/schema gates.

## 18. Manual Reality authority progress

R26 goes beyond UX design for one capability that does **not** require new schema:
manual creation of the current Canonical runtime-creatable Node kinds.

Source now includes:

~~~text
spec constants.runtimeCreatableNodeKinds
→ codegen native policy
→ GraphRepository.createManualNode
→ AppContainer.createManualNode
~~~

Properties:
- current allowed kinds are generated from Canonical spec, not retyped in Compose;
- IDs follow authoritative random identity policy, so equal display names do not
  silently merge;
- blank names fail closed;
- issuer/last4 cannot leak onto non-payment objects;
- Node write and graphRevision bump share one transaction;
- no Dependency is created;
- Preview remains read-only.

Device evidence:
- `ManualRealityAuthorityEvidenceTest`.

Manual Relationship remains disabled for a concrete cross-platform reason:

~~~text
TS reference Schema v4
  supports authenticates / controls
  supports authentication / communication

Native production schema v3
  SQL CHECK does not yet support full registry

→ Manual Dependency authority = REQUIRES_NATIVE_SCHEMA_V4
~~~

A partial v3-only production relationship UI is forbidden.

## 19. Current evidence gates

Still required:

~~~text
exact remote R26 HEAD
→ codegen/checks
→ core/app unit tests
→ Android compile
→ instrumentation
→ API36 phone runtime/pixels
→ tablet runtime/pixels
→ GPU runtime proof
→ human pixel review
→ Android Reference Freeze
~~~

Old R19–R25 screenshots cannot prove R26.

## 20. Stop line

~~~text
PDIG_VNEXT_PRODUCT_UX_DESIGN = CLOSED_AT_R26
PDIG_V2_3_ROADMAP_DESIGN = CLOSED_AT_R26
FIVE_PRIMARY_IA = FROZEN
FUTURE_CAPABILITIES = EXPLICITLY_GATED
SECRET_ESCROW = FORBIDDEN
GHOST_CAPABILITIES = FORBIDDEN

SOURCE_COMPLETE != RUNTIME_VERIFIED != HUMAN_ACCEPTED

ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_CUTOVER = HOLD
~~~