# PDIG UI vNext — Capability / Authority Matrix

> Date: 2026-10-10  
> Status: **R25 DESIGN FROZEN**
>
> Purpose: prevent a valid design/reference surface from being mistaken for an
> executable production capability.

## 1. Three different questions

Every capability must answer three independent questions:

~~~text
1. Is the product design valid?
2. May Preview/reference UI show it?
3. Does Production currently own authoritative mutation/runtime support?
~~~

These are not synonyms.

Examples:

~~~text
Manual Establish
design = yes
reference = yes
production save authority = no

File Import
design = yes
reference = yes
production import authority = yes
Preview execution = still no

Recovery Incident
design = yes
reference entry = no
production solver = no
~~~

## 2. Current matrix

| Capability | Reference visibility | Production authority | Current rule |
| --- | --- | --- | --- |
| File Import | visible reference | available | Preview explains flow; Production must reuse FileWorkflowCoordinator/AppContainer |
| Manual Create | visible reference | available | AppContainer authority exists; Preview remains read-only; production screen binding still gated |
| Manual Relationship | visible reference | requires Native Schema v4 | TS v4 exists; Native v3 SQL CHECK cannot support full R25 runtime relation set |
| Human Review | visible reference | available | Preview read-only; Production decisions go through Proposal/Candidate/Drift gateways |
| Replace Phone | visible reference | available | execute only through ChangePlan gateway |
| Replace Payment Card | visible reference | available | execute only through ChangePlan gateway |
| Lifecycle Persistence | visible reference | requires Canonical | R19 lifecycle facts are synthetic/reference until shared schema exists |
| Identity Context | hidden | requires Canonical | no selector/search ghost capability before governed membership |
| Recovery Incident | hidden | requires solver | no Recovery Mode before explicit incident + solver |

Source of truth:
- `android/app/src/main/kotlin/com/pdig/uivnext/capability/VNextCapabilityMatrix.kt`

## 3. Preview policy

Preview may demonstrate hierarchy, truth language and interaction geometry, but it
must not mutate production Reality or fake success.

Forbidden examples:
- accept a review item locally and imply Reality changed;
- open a fake file picker;
- create a fake Node on Save;
- mark Change verification locally;
- expose Identity/Recovery just because a design document exists.

## 4. Production policy

Production execution requires an authoritative lower-layer owner.

~~~text
Import
→ FileWorkflowCoordinator
→ AppContainer.previewImport / commitImport

Human Review
→ AppContainer proposal/candidate/drift actions

Change
→ createPlanForScenario
→ completeAction
→ verifyAction
→ planDetail re-read
~~~

A Compose screen is never authority.

## 5. Canonical-required capabilities

Lifecycle Persistence, Identity Context and Identity Anchor Subtype need shared
schema/version/migration/fixtures/conformance before Production can own them.

Do not route around those gates through `fields_json`, local preferences or
Android-only tables.

## 6. Solver-required capability

Recovery Incident needs:
- explicit incident authority;
- current factor availability;
- FailureDomain-aware independence;
- RecoveryCycle;
- ProviderPolicy constraints;
- deterministic solver output;
- verification/resolution.

The product must not render “2 paths = safe” or “phone exists = SMS available”.

## 7. Five-primary IA invariant

Capability growth does not create primary-tab growth.

~~~text
现在 / 基础设施 / 变更 / 记录 / 我
~~~

New capabilities enter contextually rather than creating a sixth primary tab.

## 8. External research alignment

NIST SP 800-63-4 and FIDO guidance reinforce that authentication/recovery lifecycle
needs explicit security semantics rather than UI assumptions:

- https://csrc.nist.gov/pubs/sp/800/63/4/final
- https://fidoalliance.org/white-paper-displace-password-otp-authentication-with-passkeys/
- https://fidoalliance.org/white-paper-high-assurance-enterprise-fido-authentication/

Apple/Google recovery mechanisms likewise have provider-specific establishment and
maintenance semantics:
- https://support.apple.com/guide/security/account-recovery-contact-security-secafa525057/web
- https://support.google.com/accounts/answer/17299765

Therefore PDIG keeps identifier, authenticator, recovery enrollment, current
availability, path independence and provider policy as separate authority layers.

## 9. Code review rule

Any new VNext button that changes Reality must answer:

~~~text
Which VNextCapability is this?
Is productionAuthority == AVAILABLE?
Which AppContainer/domain API owns the mutation?
What authoritative state is re-read after mutation?
What test proves Preview cannot execute it?
What test proves done != verified where applicable?
~~~

If any answer is missing, the control is a ghost capability and must not ship.

## 10. Frozen invariants

~~~text
design complete != production executable
Preview visible != Production mutable
unknown != safe
proposal != Reality
done != verified
recovery use != unique recovery
identity subtype != availability
path count != independent path count
provider support != user configuration
five-primary IA remains five
~~~


## 11. R26 concrete authority split

Manual Establish is no longer a generic authority HOLD:

~~~text
Canonical runtime-creatable policy
→ generated native constant
→ GraphRepository.createManualNode
→ AppContainer.createManualNode
→ exact transaction + graphRevision
~~~

Preview still has no Save button.

Manual Relationship remains gated for a different reason:

~~~text
TS reference Schema v4 = designed/implemented reference
Native production schema = v3
R25 relation registry includes authenticates / controls
v3 SQL CHECK does not
→ production relationship mutation remains disabled
~~~

Do not implement a partial v3-only relationship UI.
