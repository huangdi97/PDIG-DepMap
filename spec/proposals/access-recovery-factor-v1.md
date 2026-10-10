# AccessFactor / RecoveryFactor v1 — Canonical Proposal

> Date: 2026-10-10  
> Status: **DESIGN_COMPLETE / PROPOSED_CANONICAL / NOT_IMPLEMENTED**
>
> Basis: PDIG v2.3-R1 §5.23. Password / Passkey / TOTP / Security Key /
> Recovery Code are capability-layer substrate, not new top-level UI categories.
>
> No current schema/payload version is changed by this proposal.

## 1. Product question

PDIG already knows that one Reality object may authenticate or recover another.
For continuity analysis, that is still insufficient when several apparently
different methods are actually carried by the same device/provider/account.

The missing semantic question is:

> **“Which concrete access/recovery factor provides this capability, what carries it,
> and what failure domains does it actually share?”**

Examples:

~~~text
Google Account
├─ SMS OTP on Phone A
├─ Passkey synced through Provider P
├─ TOTP in Authenticator on Phone A
└─ Recovery Code stored offline
~~~

The factors are different, but independence cannot be inferred from their count.

## 2. Permanent ontology boundary

~~~text
Node
= real infrastructure object

Dependency
= confirmed directed capability relation

AccessFactor / RecoveryFactor
= substrate explaining HOW a confirmed capability is established

FailureDomain
= correlated-failure semantics

SecretLocator
= where secret material is stored, never the secret itself
~~~

Therefore:

~~~text
Factor != NodeKind
Factor != Dependency
Factor != FailureDomain
Factor != ProviderPolicy
Factor != current availability
~~~

A factor never creates graph reachability on its own.

## 3. Why factors are not top-level UI categories

v2.3 keeps consumer IA understandable:

~~~text
卡片 / 号码 / 账户 / 邮箱 / 设备 / 服务 / 薄弱点
~~~

A user should not have to manage a separate inventory tab containing hundreds of
password/passkey/TOTP rows.

Factors surface contextually:
- Account / Service detail;
- Device detail;
- Number / Email detail;
- Impact Lens;
- future Recovery Preparedness;
- future Recovery Incident;
- Change plans such as replace_device.

Five-primary IA remains:

~~~text
现在 / 基础设施 / 变更 / 记录 / 我
~~~

No sixth “Authenticator” or “Recovery” primary tab.

## 4. Recommended semantic model

A common base avoids duplicated access/recovery schemas.

~~~text
Factor
  id
  kind
  purposes[]               // authentication / recovery / access
  subjectRef               // target account/service/identity
  carrierRefs[]            // objects that physically/logically carry factor
  providerRef?
  portability
  enrollmentState
  evidenceRefs[]
  source
  confirmedAt?
  lastVerifiedAt?
  state
  createdAt
  updatedAt
~~~

A single factor can have more than one purpose only when confirmed.

Example:

~~~text
SMS to +86...
purposes = [authentication, recovery]
~~~

This does not create two independent paths.

## 5. v1 factor vocabulary

Recommended narrow initial vocabulary:

~~~text
PASSWORD
PASSKEY
TOTP
SECURITY_KEY
SMS_OTP
EMAIL_OTP
PUSH_APPROVAL
RECOVERY_CODE
RECOVERY_CONTACT
PROVIDER_REPROOF
OTHER
~~~

Do not add a new kind merely for every provider product.

Provider-specific implementation belongs in Provider Knowledge / metadata.

## 6. Purpose

~~~text
FactorPurpose
  AUTHENTICATION
  RECOVERY
  ACCESS
~~~

Do not add PAYMENT merely because a banking app uses an authenticator. The factor
provides access/authentication; payment is a downstream capability.

## 7. Carrier model

A factor is carried by one or more confirmed Reality objects.

Examples:

~~~text
SMS_OTP
→ carrierRef = phone-number identity anchor

TOTP
→ carrierRef = authenticator/device/account depending on actual setup

device-bound PASSKEY
→ carrierRef = device

synced PASSKEY
→ carrierRefs may include provider account + enrolled device context

SECURITY_KEY
→ carrierRef = device(security_key subtype, future governed subtype)

RECOVERY_CONTACT
→ carrierRef = trusted contact / provider-established contact representation
~~~

The carrier model is the bridge to FailureDomain.

## 8. Portability / persistence semantics

Factor count alone is not enough; how it survives device loss matters.

Proposed vocabulary:

~~~text
DEVICE_BOUND
PROVIDER_SYNCED
ROAMING_HARDWARE
OFFLINE
HUMAN_ASSISTED
PROVIDER_REPROOF
UNKNOWN
~~~

Examples:

### Device-bound passkey
~~~text
kind = PASSKEY
portability = DEVICE_BOUND
carrier = Phone A
~~~

Loss of Phone A may remove the factor.

### Synced passkey
~~~text
kind = PASSKEY
portability = PROVIDER_SYNCED
carrier = Provider Account P
current enrolled device context = Phone A
~~~

It may survive loss of Phone A but can still share Provider Account P as a failure
domain.

### Hardware security key
~~~text
kind = SECURITY_KEY
portability = ROAMING_HARDWARE
carrier = Key K
~~~

A second security key only improves independence if it does not share the relevant
failure domains.

## 9. Enrollment state

~~~text
PROPOSED
CONFIRMED
STALE
RETIRED
UNKNOWN
~~~

Only confirmed factors may support confirmed continuity conclusions.

A stale factor can produce a finding but must not silently disappear from Reality.

## 10. Relation binding

Factors annotate/resolve a confirmed capability relation rather than replacing it.

Conceptual link:

~~~text
FactorBinding
  factorId
  dependencyId
  role
  state
  evidenceRefs[]
~~~

Example:

~~~text
Phone Number --recovers--> Account
  supported by SMS_OTP factor
~~~

A factor proposal cannot make the Dependency confirmed.

Order:

~~~text
Observation
→ factor proposal
→ relationship proposal if needed
→ Human Review
→ confirmed Dependency
→ confirmed FactorBinding
~~~

The exact schema may allow atomic confirmation when one user decision explicitly
confirms both, but provenance remains separable.

## 11. AccessFactor vs RecoveryFactor

Do not duplicate physical factors.

Preferred representation:

~~~text
one Factor
+ purposes[]
~~~

Consumer/domain query aliases:

~~~text
AccessFactor
= confirmed Factor where AUTHENTICATION or ACCESS purpose

RecoveryFactor
= confirmed Factor where RECOVERY purpose
~~~

This handles a phone OTP that serves both login and recovery without creating two
fake independent factors.

## 12. FailureDomain integration

This is the main reason the substrate exists.

Examples:

~~~text
SMS OTP → Phone A
TOTP App → Phone A
Passkey → Phone A
~~~

Three factors, one DEVICE FailureDomain.

~~~text
Passkey synced by Provider P
Recovery email Account P
Provider account re-proof via P
~~~

Several methods may share PROVIDER / ACCOUNT failure domains.

Continuity result may expose:

~~~text
factorCount = 3
pathCount = 3
independentPathCount = 1
sharedFailureDomains = [DEVICE:Phone A]
~~~

The UI must never convert factorCount to resilience.

## 13. RecoveryCycle integration

A factor may participate in a recovery cycle only through confirmed graph semantics.

Example:

~~~text
Account A
→ recovery email B
→ access to B authenticates through Account A
~~~

The factor record itself is not a graph edge.

Confirmed cycle:
- confirmed Dependency edges only.

Factor/proposal hints:
- at most potential cycle / needs_review.

## 14. Provider Policy boundary

ProviderPolicy can describe:
- supported factor kinds;
- minimum number of authenticators;
- waiting periods;
- replacement constraints;
- whether synced/device-bound modes exist;
- re-proofing options.

It cannot claim:
- the user enrolled a factor;
- a factor is currently usable;
- a recovery contact is current;
- a passkey is synced for this user.

~~~text
Provider supports passkeys
!=
user has confirmed passkey
~~~

## 15. Current availability is not enrollment

Permanent distinction:

~~~text
factor exists / enrolled
!=
factor currently available
~~~

Normal continuity analysis uses enrollment + confirmed dependencies + FailureDomain.

Recovery Incident adds an explicit runtime availability layer:

~~~text
AVAILABLE_CONFIRMED
UNAVAILABLE_CONFIRMED
DEGRADED
UNKNOWN
~~~

Example:

~~~text
confirmed SMS factor exists
+ incident says phone unavailable
→ factor blocked for this incident
~~~

## 16. Factor freshness

Useful maintenance facts:

~~~text
lastVerifiedAt
lastUsedAt?              // only if authoritative/user-recorded
reviewDue?
providerPolicyRevision?
~~~

Staleness can create:

~~~text
STALE_RECOVERY_INFORMATION
PENDING_VERIFICATION
POLICY_CHANGED_REVALIDATION_REQUIRED
~~~

It must not automatically retire the factor.

## 17. Passkey semantics

Passkeys must not be flattened to “safe password replacement”.

PDIG needs to distinguish at least:
- device-bound passkey;
- provider-synced passkey;
- unknown portability.

FIDO guidance notes synced passkeys can support recovery across devices, while
device-bound credentials require separate backup/recovery strategy. High-assurance
design must also account for the security of the sync provider account.

Design references:
- https://fidoalliance.org/white-paper-displace-password-otp-authentication-with-passkeys/
- https://fidoalliance.org/white-paper-high-assurance-enterprise-fido-authentication/

These references do not establish user Reality.

## 18. NIST recovery boundary

NIST SP 800-63B-4 treats authenticator lifecycle and account recovery explicitly,
including recovery codes, recovery contacts and repeated identity proofing.

Design references:
- https://csrc.nist.gov/pubs/sp/800/63/4/final
- https://csrc.nist.gov/pubs/sp/800/63/B/4/final

PDIG therefore does not model “email/phone exists” as sufficient recovery proof.

## 19. Consumer grammar

Allowed consumer labels:

~~~text
Passkey · 同步方式待确认
TOTP · 保存在这台设备
短信验证码 · 通过香港主号
恢复码 · 已记录存在 · 存放位置已确认
恢复联系人 · 已建立 · 最近核验日期
~~~

Avoid:
- internal factor IDs;
- “3 个因素 = 安全”;
- assurance percentage;
- plaintext secret;
- provider-policy claims written as user facts.

## 20. Me / Recovery Preparedness future surface

A future **恢复准备** child surface under “我” may become visible only after Factor
Canonical support.

It is not active Incident Recovery.

Hierarchy:

~~~text
我
→ 恢复准备
  → 已确认恢复因素
  → shared failure-domain findings
  → stale/unknown items
  → “去核对 / 去建立独立备用”
~~~

No global score.

Until Canonical support:

~~~text
RECOVERY_PREPAREDNESS_VISIBLE_UI = HOLD
~~~

## 21. Change integration

### replace_phone_number
Must account for factors carried by old/new numbers:
- SMS OTP;
- recovery SMS;
- provider account phone confirmation.

### replace_device
Must account for:
- device-bound passkeys;
- authenticator/TOTP;
- push approvals;
- eSIM/phone carrier relation;
- password manager access;
- synced passkey provider access.

Make-Before-Break:

~~~text
establish new factor/carrier
→ verify
→ migrate
→ verify downstream capability
→ retire old factor/device
~~~

## 22. Secret boundary

Factor records may say a secret exists.

Forbidden fields:
- password;
- TOTP seed;
- full recovery code;
- private key;
- seed phrase;
- session token.

Secret storage location belongs to SecretLocator.

## 23. Suggested schema

Conceptual:

~~~text
factors
  id
  kind
  purposes_json
  subject_node_id
  carrier_refs_json
  provider_ref
  portability
  enrollment_state
  source
  evidence_refs_json
  confirmed_at
  last_verified_at
  created_at
  updated_at

factor_bindings
  factor_id
  dependency_id
  role
  state
  evidence_refs_json
~~~

Final shared schema decision may normalize carrier refs into a join table.

## 24. Graph revision semantics

Confirmed changes affecting continuity Reality must bump revision:
- confirmed factor created;
- factor retired/reactivated;
- carrier binding changed;
- confirmed FactorBinding changed.

Pure freshness timestamps may or may not bump revision depending on whether they
change Impact/Continuity output; this must be decided centrally.

Proposals never become confirmed by revision bump alone.

## 25. Migration

Existing payload:
- zero confirmed factors;
- existing Dependencies remain valid;
- do not infer factors from relation names.

Forbidden migration:

~~~text
recovers edge from phone
→ auto-create SMS_OTP

authenticates edge from device
→ auto-create Passkey

node name contains "Authenticator"
→ auto-create TOTP
~~~

Factors can be established later through review.

## 26. Conformance fixtures

Minimum:

~~~text
ARF-01 one SMS recovery factor bound to confirmed recovers edge
ARF-02 same factor serves auth+recovery without duplication
ARF-03 SMS + TOTP on same device => factor 2, independent path not assumed 2
ARF-04 device-bound passkey blocked by device loss
ARF-05 provider-synced passkey keeps provider FailureDomain
ARF-06 two hardware keys distinct carriers but independence requires domains
ARF-07 proposal factor excluded from confirmed continuity
ARF-08 retired factor excluded
ARF-09 stale factor generates review, not silent deletion
ARF-10 ProviderPolicy support does not create enrollment
ARF-11 factor existence does not imply incident availability
ARF-12 no secret value accepted in metadata
ARF-13 factor binding references confirmed Dependency
ARF-14 recovery cycle remains graph-derived
ARF-15 deterministic export/import after version allocation
ARF-16 old payload migrates to zero confirmed factors
~~~

## 27. Stop line

~~~text
ACCESS_RECOVERY_FACTOR_DESIGN = COMPLETE
FACTOR_CANONICAL = NOT_IMPLEMENTED
RECOVERY_PREPAREDNESS_UI = HOLD
RECOVERY_INCIDENT_UI = HOLD
NO_NEW_PRIMARY_TAB = REQUIRED
~~~
