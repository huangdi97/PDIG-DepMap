# SecretLocator v1 — Canonical Proposal

> Date: 2026-10-10
> Status: **DESIGN_COMPLETE / PROPOSED_CANONICAL / NOT_IMPLEMENTED**
>
> Basis: PDIG v2.3-R1 §5.24.
>
> SecretLocator records that recovery/authentication secret material exists and
> where it is conceptually stored. It never stores the secret value.

## 1. Problem

Continuity analysis often needs facts such as:

~~~text
Recovery code exists
→ stored in password manager

Backup security key exists
→ stored in home safe

Seed phrase exists
→ offline envelope
~~~

PDIG must not become a secret vault.

Permanent rule:

~~~text
PDIG may remember where a secret is
PDIG must not remember the secret itself
~~~

## 2. SecretLocator is not a secret store

Forbidden in ordinary PDIG storage, logs, exports or screenshots:

~~~text
password plaintext
TOTP seed
full recovery code
private key
seed phrase
CVV
session token
API token
recovery token
cryptographic key material
~~~

SecretLocator contains only location/provenance metadata.

## 3. Recommended model

~~~text
SecretLocator
  id
  secretKind
  subjectRef?
  factorRef?
  holderType
  holderRef?
  locationHint?
  state
  evidenceRefs[]
  source
  confirmedAt?
  lastVerifiedAt?
  createdAt
  updatedAt
~~~

At least one of subjectRef / factorRef identifies what the locator belongs to.

## 4. Secret kinds

Initial vocabulary:

~~~text
PASSWORD
RECOVERY_CODE
TOTP_SECRET
PRIVATE_KEY
SEED_PHRASE
BACKUP_CODE
OTHER_SECRET
~~~

A kind says only what class of secret exists. It does not reveal the value.

## 5. Holder types

~~~text
PASSWORD_MANAGER
OFFLINE_ENVELOPE
SAFE
HARDWARE_DEVICE
SECURE_OS_STORE
PROVIDER_ACCOUNT
TRUSTED_PERSON
OTHER
UNKNOWN
~~~

Examples:

~~~text
Recovery Code
→ holderType = PASSWORD_MANAGER
→ holderRef = confirmed password-manager account/service object

Seed Phrase
→ holderType = SAFE
→ locationHint = "home safe"
~~~

Location hints are user-confirmed metadata and may themselves be sensitive.

## 6. Relationship to AccessFactor / RecoveryFactor

~~~text
Factor
= a usable authentication/recovery mechanism

SecretLocator
= where secret material supporting that factor can be found
~~~

Examples:

~~~text
RECOVERY_CODE Factor
→ SecretLocator(password manager)

PASSWORD Factor
→ SecretLocator(password manager)
~~~

A locator never creates a confirmed factor or Dependency.

## 7. Relationship to Node / Dependency

~~~text
SecretLocator
!= Node
!= Dependency
!= Recovery Path
!= independent backup proof
~~~

Two locators do not mean two independent copies. They may share one provider,
account, physical location or other FailureDomain.

Independence belongs to Continuity analysis.

## 8. Secret presence vs current usability

~~~text
secret recorded as existing
!= secret currently retrievable
!= factor currently usable
~~~

During Recovery Incident:
- password manager may be unavailable;
- safe may be physically inaccessible;
- trusted person may be unreachable;
- provider account may be compromised.

Incident availability is separate runtime evidence.

## 9. Privacy / masking

Location metadata may reveal:
- physical storage location;
- password-manager provider;
- trusted person;
- security practices.

Requirements:
- encrypted local storage;
- no analytics;
- no raw hints in diagnostic logs;
- masking mode can collapse location to holder category;
- screenshot evidence uses synthetic locations;
- optional user choice to omit locationHint.

Consumer masking example:

~~~text
Normal:  恢复码 · 1Password
Masked:  恢复码 · 密码管理器
~~~

## 10. Export boundary

If SecretLocator enters the .depmap payload, export semantics must be explicitly
versioned.

Recommended:
- locator records remain inside the encrypted payload;
- no locator data in unencrypted manifest/header;
- backup UI warns that metadata may reveal continuity setup.

Never export actual secret values or provider session material.

## 11. Manual confirmation

Manual flow:

~~~text
Choose secret kind
→ choose holder type
→ optionally link holder object
→ optionally enter location hint
→ confirm metadata-only boundary
→ authoritative write
~~~

The UI must say:

> 不要在这里填写密码、验证码、恢复码或密钥内容。

Avoid any field named “secret” that invites the user to paste the value.

## 12. Import / AI extraction

Automated sources may propose metadata only.

Allowed:

> “This document appears to indicate backup codes are stored offline.”

Forbidden:
- auto-confirm SecretLocator Reality;
- OCR and persist recovery codes;
- capture TOTP seed QR data;
- parse private keys into PDIG metadata.

If source material contains secret values, adapters should redact them before any
diagnostic persistence.

## 13. Trusted person boundary

~~~text
provider supports recovery contact
!= user has configured one

contact exists
!= currently reachable

trusted person holds backup code
!= independent recovery path
~~~

Provider-specific recovery enrollment remains separate Reality / ProviderPolicy.

## 14. UI placement

SecretLocator is never a primary destination.

Future contextual surfaces:
- Factor detail;
- Recovery Preparedness;
- Recovery Incident action checklist;
- Account/Service detail when relevant.

Consumer labels:

~~~text
恢复码：已记录存在
存放：密码管理器
最近核对：2026-09-01
~~~

Never provide a “copy secret” action because PDIG does not own the value.

## 15. Maintenance

State:

~~~text
CONFIRMED
NEEDS_REVIEW
STALE
RETIRED
~~~

A locator may become stale without becoming false.

Possible triggers:
- linked holder archived;
- password-manager account changed;
- user marks secret moved;
- provider policy changes.

Staleness produces a finding; it does not silently delete Reality.

## 16. Recovery Incident integration

Recovery solver may use locator metadata only as constraint/context.

Example:

~~~text
Recovery Code factor exists
locator = offline safe
incident = user traveling
physical access = UNKNOWN
→ factor availability = UNKNOWN
~~~

The solver cannot assume “offline = available”.

## 17. Device Continuity integration

Replacing a device may require:
- verify password-manager access;
- confirm security-key location;
- verify recovery codes are retrievable.

SecretLocator can provide checklist context but never execute secret transfer.

## 18. Proposed schema

~~~text
secret_locators
  id
  secret_kind
  subject_ref
  factor_ref
  holder_type
  holder_ref
  location_hint
  state
  source
  evidence_refs_json
  confirmed_at
  last_verified_at
  created_at
  updated_at
~~~

Constraints:
- referenced objects/factors exist;
- no secretValue field;
- location_hint length bounded;
- defense-in-depth rejection for obvious key/code material may be added.

## 19. Graph revision

Confirmed locator changes can alter continuity findings.

Schema review should classify:
- confirmed create / retire / move → continuity revision bump;
- display masking → no Reality bump;
- freshness timestamp → bump only when derived semantic state changes.

One authoritative transaction per semantic mutation.

## 20. Conformance fixtures

~~~text
SL-01 confirmed recovery-code locator in password manager
SL-02 locator without optional locationHint
SL-03 raw recovery code rejected from metadata path
SL-04 private key material rejected
SL-05 retired holder creates needs-review, not deletion
SL-06 two locators do not imply independent copies
SL-07 locator proposal excluded from confirmed recovery
SL-08 masked rendering hides provider/location detail
SL-09 incident unavailable holder blocks availability assumption
SL-10 old payload migrates to zero locators
SL-11 deterministic export/import after schema allocation
SL-12 no secret content in logs/evidence artifact
~~~

## 21. Stop line

~~~text
SECRET_LOCATOR_DESIGN = COMPLETE
SECRET_STORAGE_IN_PDIG = FORBIDDEN
SECRET_LOCATOR_CANONICAL = NOT_IMPLEMENTED
RECOVERY_UI_CONSUMPTION = HOLD
NO_NEW_PRIMARY_TAB = REQUIRED
~~~