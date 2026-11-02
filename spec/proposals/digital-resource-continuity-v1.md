# Digital Resource Continuity v1 — Product / Canonical Proposal

> Date: 2026-10-10
> Status: **DESIGN_COMPLETE / FUTURE v0.6+ / NOT_IMPLEMENTED**
>
> Basis: PDIG v2.3-R1 roadmap — Platform / Digital Resource Continuity.
>
> This proposal extends PDIG continuity principles to domains, source repositories,
> cloud/hosting/data resources and platform administration. It does not enable new
> runtime NodeKinds/relations in the current product.

## 1. Product question

Digital Resource Continuity answers:

> **“If this platform account, domain, repository, cloud project or hosting control
> changes, what must remain under my control, what can move, what can break, and
> how do I verify the transition?”**

It does not turn PDIG into:
- a cloud control panel;
- a DNS manager;
- a Git hosting client;
- a password/secret manager;
- a deployment platform.

PDIG remains the continuity/control-plane model above those providers.

## 2. Why this domain belongs in PDIG

Personal/professional digital infrastructure often depends on:

~~~text
Domain registration
DNS authority
Git repository ownership
Organization/admin membership
Cloud account/project control
Hosting/deployment control
Data storage/backups
Billing/payment routes
Recovery/authentication factors
~~~

The dependencies cross provider boundaries.

Example:

~~~text
Domain
→ registrar account
→ recovery email
→ provider account
→ device/passkey

Domain
→ DNS provider
→ hosting deployment
→ source repository
→ organization owner
~~~

A failure can propagate through control rather than payment alone.

## 3. Node strategy

v2.3 prefers subtype over uncontrolled NodeKind expansion.

A future schema review should decide whether to add one stable:

~~~text
digital_resource
~~~

kind with subtypes, or reuse account/service/custom where behavior remains generic.

Recommended candidate subtypes:

~~~text
DOMAIN_NAME
DNS_ZONE
SOURCE_REPOSITORY
SOURCE_ORGANIZATION
CLOUD_ACCOUNT
CLOUD_PROJECT
HOSTING_SITE
DEPLOYMENT
DATA_STORE
BACKUP_SET
API_APPLICATION
OTHER_DIGITAL_RESOURCE
~~~

Do not add each provider/product as a NodeKind.

## 4. Capabilities

Future capability vocabulary may include:

~~~text
control
hosting
data
deployment
dns
payout
~~~

Current production capabilities remain unchanged until Canonical expansion.

Capability is still the propagation unit:

~~~text
(nodeId, capability)
~~~

Do not collapse everything to generic “access”.

## 5. Typed relations

Candidate future relations:

~~~text
CONTROLS
HOSTS
DEPLOYS_TO
STORES_DATA_FOR
RESOLVES_DNS_FOR
BELONGS_TO
BACKED_UP_BY
DELEGATES_RECOVERY_TO
PAYOUT_DESTINATION
~~~

Existing production relation “controls” may be reusable for part of the model.

Every new relation must define:
- direction;
- fromKinds/toKinds;
- capability;
- grouping semantics;
- verification policy;
- impact consequence.

No generic linked_to.

## 6. ProviderPolicy boundary

Provider Knowledge may describe:
- domain transfer locks;
- authorization-code requirements;
- admin-role behavior;
- organization ownership constraints;
- project transfer constraints;
- backup/export support;
- region restrictions;
- recovery procedures.

It cannot establish user Reality.

Examples:

~~~text
Registrar requires Auth-Code for transfer
!= user currently has Auth-Code

Git platform supports organization owners
!= user has another owner

Cloud provider supports project transfer
!= this project is transferable now
~~~

## 7. Domain continuity

Domain control has multiple separable layers:

~~~text
registration
registrant/contact state
registrar account
transfer lock
Auth-Code / transfer authorization
DNS authority
DNSSEC
billing
website/email dependencies
~~~

ICANN describes Auth-Codes as authorization data required for gTLD inter-registrar
transfer. They are secret-like material and must not be stored as plaintext in PDIG.

Reference:
- https://www.icann.org/resources/pages/auth-2013-05-03-en

Cloudflare documents transfer prerequisites such as account access, domain unlock,
authorization code, nameserver/DNSSEC handling and transfer-lock constraints.

Reference:
- https://developers.cloudflare.com/registrar/get-started/transfer-domain-to-cloudflare/
- https://developers.cloudflare.com/registrar/account-options/transfer-out-from-cloudflare/

PDIG mapping:
- ProviderPolicy records rules;
- Personal Reality records confirmed current setup;
- SecretLocator may record where an Auth-Code/access key is stored, never its value;
- ChangePlan models transfer/migration sequence.

## 8. Source repository / organization continuity

A repository can depend on:
- user account;
- organization ownership/admin role;
- authentication factor;
- deploy key/app;
- CI provider;
- package registry;
- hosting target.

Control of a personal repository is not identical to control of an organization.

GitHub supports collaborators/organization roles and a separate successor/deceased
user policy; therefore PDIG must model provider-specific authority rather than
assuming “repository exists = transferable”.

Reference:
- https://docs.github.com/en/repositories/creating-and-managing-repositories/access-to-repositories
- https://docs.github.com/en/site-policy/other-site-policies/github-deceased-user-policy

## 9. Cloud / hosting continuity

Important distinctions:

~~~text
provider account
billing account
project/resource ownership
IAM/admin role
runtime deployment
data persistence
backup/export
region
DNS/domain
~~~

A project migration can preserve service while changing one or more of these.

PDIG should not infer cloud-resource ownership merely from an API token or local CLI
configuration.

## 10. Change Primitives

Future primitives:

~~~text
MIGRATE
TRANSFER
REGION_CHANGE
CLOSE
COMPROMISE
REPLACE
~~~

Examples:

~~~text
transfer_domain
migrate_dns_provider
transfer_repository
move_project_between_accounts
migrate_hosting
change_cloud_region
close_platform_account
rotate_compromised_admin
~~~

Each is generated from generic primitive + subtype/policy where possible rather than
one handcrafted screen per provider.

## 11. Domain transfer ChangePlan example

~~~text
Understand
→ confirm registrar / registrant / DNS / dependencies

Prepare
→ verify target registrar account
→ verify recovery/authentication
→ review transfer lock
→ review DNSSEC
→ locate transfer authorization material

Change
→ unlock when appropriate
→ request/submit transfer
→ maintain DNS availability

Verify
→ new registrar control confirmed
→ registration contact confirmed
→ DNS resolution confirmed
→ renewal/billing confirmed

Retire
→ remove obsolete old-provider access only after verification
~~~

“Transfer requested” != “transfer complete”.

## 12. DNS migration example

Make-Before-Break:

~~~text
create target DNS zone
→ copy/verify records
→ lower TTL if policy requires
→ prepare DNSSEC transition
→ change delegation
→ verify resolution from multiple contexts
→ re-enable/verify DNSSEC
→ retire old zone
~~~

ProviderPolicy can suggest steps; Personal Reality confirms actual state.

## 13. Repository continuity example

~~~text
verify alternate owner/admin
→ verify authentication independently
→ verify repository clone/export
→ verify CI/secrets ownership boundaries
→ transfer/migrate repo
→ verify webhooks/deployments/packages
→ retire obsolete credentials
~~~

PDIG does not copy repository secrets.

## 14. Data continuity

“Backup exists” is insufficient.

A backup path needs metadata such as:
- target;
- freshness;
- restore capability;
- encryption/key-holder context;
- provider/domain;
- last verification.

Secret key material remains outside PDIG.

Possible finding:

> 备份与生产数据依赖同一云账户，当前不能确认独立恢复。

## 15. FailureDomain

Future domains may include:

~~~text
PROVIDER
ACCOUNT
DEVICE
REGION
NETWORK
PHYSICAL_LOCATION
~~~

Examples:

~~~text
repo + CI + hosting all under one provider/account
→ correlated provider/account failure

primary data + backup same account/region
→ not independent backup
~~~

Do not equate “different resources” with independent continuity.

## 16. Identity / factor composition

Digital resource control often depends on:
- passkeys;
- security keys;
- recovery email/phone;
- provider account;
- devices.

AccessFactor / RecoveryFactor is therefore a prerequisite for high-confidence
continuity analysis.

## 17. SecretLocator composition

Secret-like transfer/admin material:
- domain Auth-Code;
- emergency backup code;
- recovery key;
- offline credentials

must never be stored as plaintext.

SecretLocator may record only where such material is stored.

## 18. Identity Context composition

Future filters may include:

~~~text
工作身份
开发者身份
个人项目
某个地区/公司
~~~

Context grouping does not create ownership/control relations.

## 19. Consumer UI

No new primary tab.

Entry points:
- Infrastructure → Service/Account/future Digital Resource object;
- Change → relevant migration/transfer scenario;
- Me → continuity context;
- Now → current action/finding.

A future object detail follows:

~~~text
Identity / control
→ confirmed dependencies
→ provider constraints
→ Impact Lens
→ maintenance
→ Change
~~~

Do not expose raw IAM/graph terminology as the primary consumer view.

## 20. Records

Records stores:
- transfer plan created;
- action completion;
- verification;
- confirmed drift;
- policy revision/revalidation evidence.

Now owns:
- current transfer waiting window;
- upcoming expiration;
- action due;
- unresolved review.

## 21. Security

Never persist in normal PDIG fields:
- API tokens;
- private SSH keys;
- deploy secrets;
- registrar Auth-Code;
- cloud access keys;
- signing keys.

PDIG may store:
- existence metadata;
- SecretLocator;
- provider/account/resource relationships;
- verification timestamps.

## 22. Evidence automation

Future adapters may discover:
- repository metadata;
- registrar exports;
- cloud inventories;
- DNS records.

Discovery remains:

~~~text
Observation
→ Proposal/Candidate
→ Human Review
→ Reality
~~~

Provider API access does not automatically make every observed relation authoritative.

## 23. Canonical prerequisites

~~~text
digital resource subtype decision
future capabilities
future typed relations
ProviderPolicy coverage
AccessFactor / RecoveryFactor
SecretLocator
FailureDomain extensions
ChangePrimitive expansion
scenario factory
cross-platform conformance
~~~

## 24. Conformance fixtures

~~~text
DRC-01 domain registrar + DNS provider are separate confirmed controls
DRC-02 ProviderPolicy transfer support does not imply user eligibility
DRC-03 transfer Auth-Code never stored as plaintext
DRC-04 domain transfer waiting/lock window blocks premature completion
DRC-05 DNS migration keeps old path until verification
DRC-06 DNSSEC transition is explicit
DRC-07 repo collaborator does not automatically equal owner/admin
DRC-08 repo transfer requires authoritative completion verification
DRC-09 CI/hosting relation remains separate from repository ownership
DRC-10 backup same account/provider not assumed independent
DRC-11 proposal cloud resource excluded from confirmed blast radius
DRC-12 region migration preserves unknown constraints as review
DRC-13 completed action != verified control
DRC-14 SecretLocator metadata survives without secret value
DRC-15 provider policy revision can require revalidation
DRC-16 deterministic export/import after schema allocation
~~~

## 25. Visibility gate

~~~text
DIGITAL_RESOURCE_CONTINUITY_DESIGN = COMPLETE
DIGITAL_RESOURCE_CANONICAL = NOT_IMPLEMENTED
DIGITAL_RESOURCE_REFERENCE_UI = HOLD
DIGITAL_RESOURCE_PRODUCTION = HOLD
NO_NEW_PRIMARY_TAB = REQUIRED
~~~