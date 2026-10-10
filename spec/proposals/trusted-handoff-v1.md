# Trusted Handoff v1 — Continuity Proposal

> Date: 2026-10-10
> Status: **DESIGN_COMPLETE / FUTURE v0.7+ / NOT_IMPLEMENTED**
>
> Basis: PDIG v2.3-R1 roadmap — Evidence Automation / Trusted Handoff.
>
> Trusted Handoff is an extreme continuity state. It does not change PDIG's daily
> product positioning and must not become a legal-will product.

## 1. Product question

Trusted Handoff answers:

> **“If I cannot manage a defined part of my digital infrastructure myself, what
> information, provider-specific instructions and verified continuity context should
> a trusted person be able to receive — without PDIG giving them my secrets?”**

Possible future contexts:
- temporary incapacity;
- extended unavailability;
- business continuity;
- death/legacy.

The legal/authority trigger remains external to PDIG unless a provider supplies a
governed mechanism.

## 2. Permanent boundaries

~~~text
Trusted Handoff
!= account credential sharing
!= master password escrow
!= legal will
!= automatic ownership transfer
!= provider-policy override
!= Recovery Incident
~~~

PDIG organizes continuity metadata and tasks; providers/legal authorities decide
actual account/data transfer where required.

## 3. Why provider-specific truth matters

Providers expose materially different handoff/legacy mechanisms.

Apple Legacy Contact requires a provider-established contact/access-key flow and,
for post-death access, supporting documentation. Certain sensitive data such as
iCloud Keychain passwords/passkeys are outside the accessible legacy scope.

Reference:
- https://support.apple.com/en-us/102631
- https://support.apple.com/guide/security-pdf/legacy-contact-security-secebf027fb8/1/web/1

Google Inactive Account Manager lets a user preselect contacts and data to share
after a configured inactivity period.

Reference:
- https://support.google.com/accounts/answer/3036546

GitHub allows a pre-designated successor for certain repository-continuity actions,
while support applies an authorization/documentation process and successors cannot
simply log into the deceased user's account.

Reference:
- https://docs.github.com/en/repositories/creating-and-managing-repositories/access-to-repositories
- https://docs.github.com/en/site-policy/other-site-policies/github-deceased-user-policy

Therefore:

~~~text
Provider supports legacy/successor feature
!= user configured it
!= trusted person currently authorized
!= all account data transferable
~~~

## 4. Core model

Recommended future object:

~~~text
TrustedHandoffPlan
  id
  label
  purpose
  scopeRefs[]
  trustedPartyRefs[]
  providerArrangementRefs[]
  triggerPolicy
  disclosurePolicy
  state
  evidenceRefs[]
  confirmedAt
  lastReviewedAt
  nextReviewAt?
  createdAt
  updatedAt
~~~

This is continuity metadata, not credentials.

## 5. Trusted party

A trusted person should not be represented by a phone/email string alone.

Future concept:

~~~text
TrustedParty
  id
  displayLabel
  relationshipLabel?
  contactRefs[]
  confirmationState
  evidenceRefs[]
  lastVerifiedAt?
~~~

A contact can exist without being provider-enrolled.

~~~text
TrustedParty confirmed
!= Provider Legacy Contact configured
~~~

Provider enrollment is a separate confirmed arrangement.

## 6. Provider arrangement

~~~text
ProviderHandoffArrangement
  providerRef
  arrangementType
  subjectRef
  trustedPartyRef
  state
  configuredAt?
  lastVerifiedAt?
  policyRevision?
  evidenceRefs[]
~~~

Examples:

~~~text
APPLE_LEGACY_CONTACT
GOOGLE_INACTIVE_ACCOUNT_MANAGER
GITHUB_SUCCESSOR
BUSINESS_ADMIN_BACKUP
OTHER_PROVIDER_ARRANGEMENT
~~~

ProviderPolicy defines what is possible; this object records what the user actually
confirmed as configured.

## 7. Trigger policy

PDIG must not autonomously determine death/incapacity.

Possible future metadata:

~~~text
MANUAL_HANDOFF
PROVIDER_INACTIVITY
EXTERNAL_LEGAL_EVENT
BUSINESS_CONTINUITY_EVENT
UNKNOWN / EXTERNAL
~~~

For provider inactivity features, PDIG may record the user-confirmed provider setup.

For legal/death triggers, PDIG records only continuity instructions/arrangement
metadata; the provider/legal process owns verification.

## 8. Scope

A handoff is explicitly scoped.

Examples:

~~~text
public source repositories
domain renewal instructions
hosting billing continuity
family photo/data provider arrangement
business service administrator list
selected documents
~~~

Avoid:

~~~text
“all my accounts”
“everything”
~~~

unless every provider/resource scope is explicitly modeled.

## 9. Disclosure policy

A handoff package may contain:
- object labels;
- provider names;
- continuity checklist;
- public/support URLs;
- SecretLocator categories/hints where explicitly allowed;
- provider arrangement evidence;
- contact/owner/admin context;
- verification dates.

It must never contain:
- passwords;
- recovery codes;
- seed phrases;
- private keys;
- TOTP seeds;
- API tokens;
- session cookies;
- provider Auth-Codes.

## 10. SecretLocator relationship

Trusted Handoff may say:

> “Recovery access material is stored in the offline envelope.”

It does not include the material.

~~~text
Trusted Handoff
→ SecretLocator metadata
→ external holder retrieves secret under appropriate authority
~~~

PDIG remains metadata-only.

## 11. AccessFactor relationship

A trusted person/contact may be:
- a provider recovery contact;
- a legacy contact;
- a holder of offline material.

These roles are not interchangeable.

~~~text
RECOVERY_CONTACT factor
!= LEGACY_CONTACT provider arrangement
!= TrustedParty
~~~

One person may occupy several roles only when separately confirmed.

## 12. Identity Context

Identity Context may scope handoff:

~~~text
Work Identity
Developer Identity
Family Identity
UK Financial Identity
~~~

Context membership is a lens; handoff still references actual nodes/resources and
provider arrangements.

## 13. Business continuity

For work/business infrastructure, preferred pattern is durable shared authority
before emergency handoff:

~~~text
organization ownership
multiple admins
shared billing authority
documented domain ownership
independent recovery factors
~~~

Trusted Handoff should surface missing durable shared authority rather than encourage
credential sharing.

## 14. Personal repository / public-content continuity

GitHub's successor model illustrates an important distinction:
- successor can perform provider-defined actions on eligible repositories;
- successor does not receive the user's login credentials.

PDIG should favor provider-granted role continuity over credential handoff.

## 15. Review cadence

Handoff plans decay.

Possible review triggers:
- trusted party contact changed;
- provider policy changed;
- resource archived/transferred;
- SecretLocator stale;
- provider arrangement unverified;
- context membership changed.

State:

~~~text
CONFIRMED
NEEDS_REVIEW
STALE
REVOKED
~~~

No silent auto-renewal of trust.

## 16. Revocation

User must be able to revoke:
- trusted party relationship;
- provider arrangement reference;
- handoff scope.

Revoking PDIG metadata cannot claim to revoke a provider's real legacy/recovery
contact. If provider arrangement exists, the plan should instruct verification of
provider-side revocation.

## 17. Handoff package

If future product supports package generation:

~~~text
HandoffPackage
  planId
  generatedAt
  scopeSnapshot
  providerInstructions
  objectSummary
  evidenceSummary
  excludedSecretStatement
  integrityHash
~~~

It should be:
- explicit;
- reviewable before export;
- encrypted;
- revocable only insofar as copies remain under user's control.

PDIG cannot revoke an exported file already possessed by someone else.

## 18. No cloud relay by default

Current PDIG is local-first.

Do not create a server-held “dead man's switch” without a separate trust/security
architecture.

A future relay would require:
- account system;
- encryption/key-management design;
- trigger authority;
- abuse prevention;
- audit;
- jurisdiction/privacy analysis.

That is outside this proposal.

## 19. Consumer UI

No new primary tab.

Future hierarchy:

~~~text
我
→ 连续性 / Trusted Handoff
~~~

Possible sections:
- trusted people;
- provider arrangements;
- scoped resources;
- stale items;
- handoff instructions;
- export package.

Do not use fear-driven emergency visuals for routine maintenance.

## 20. Now / Records boundary

Now:
- handoff review due;
- provider arrangement stale;
- trusted party contact needs re-verification.

Records:
- handoff plan created/updated;
- provider arrangement verified;
- package exported;
- scope revoked.

No provider-side effect is recorded as completed without evidence.

## 21. Recovery Incident boundary

Recovery Incident is about restoring access after a failure.

Trusted Handoff is about transferring/continuing scoped responsibility under an
external/trusted authority context.

They can interact, but neither is a subtype of the other.

## 22. Security / abuse model

Threats:
- coercion;
- unauthorized trusted party;
- stale contact;
- social-engineering;
- export theft;
- overbroad scope;
- false provider configuration claim.

Controls:
- explicit confirmation;
- local encryption;
- masking;
- review cadence;
- scoped export;
- no secrets;
- provider arrangement evidence;
- no autonomous trigger.

## 23. Conformance fixtures

~~~text
TH-01 trusted party exists but no provider arrangement
TH-02 provider supports legacy contact but user configuration unknown
TH-03 confirmed provider arrangement links trusted party
TH-04 two provider arrangements for same person remain separate facts
TH-05 scope references actual resources, not context label alone
TH-06 revoked PDIG plan does not claim provider revocation
TH-07 stale trusted contact produces review
TH-08 export package excludes secret values
TH-09 SecretLocator hint can be masked
TH-10 no autonomous death/incapacity inference
TH-11 provider policy revision creates revalidation requirement
TH-12 external package copy cannot be remotely revoked by PDIG
TH-13 Recovery Incident remains separate
TH-14 five-primary IA unchanged
~~~

## 24. Stop line

~~~text
TRUSTED_HANDOFF_DESIGN = COMPLETE
TRUSTED_HANDOFF_CANONICAL = NOT_IMPLEMENTED
TRUSTED_HANDOFF_UI = HOLD
SERVER_RELAY = OUT_OF_SCOPE
SECRET_ESCROW = FORBIDDEN
NO_NEW_PRIMARY_TAB = REQUIRED
~~~