# PDIG UI vNext — Capability / Authority Matrix

> Date: 2026-10-10  
> Status: **R38 CURRENT NORMATIVE CONTRACT**
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
production save authority = yes

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
| Manual Create | visible reference | available | Production supports generic runtime-creatable Nodes plus governed atomic phone/email identity establishment; Preview remains read-only |
| Manual Relationship | visible reference | available for current v3 runtime set | Production may create only funding_source / merchant_agreement / recovers / authenticates / controls via canonical relation validation; verifies / bound_to remain unavailable |
| Human Review | visible reference | available | Preview read-only; Production decisions go through Proposal/Candidate/Drift gateways |
| Replace Phone | visible reference | available | execute only through ChangePlan gateway |
| Replace Payment Card | visible reference | available | execute only through ChangePlan gateway |
| Lifecycle Persistence | visible reference | requires Canonical | R19 lifecycle facts are synthetic/reference until shared schema exists |
| RegionFact | visible reference | requires Canonical | Preview Region/Globe valid; Production geography remains zero-label until governed RegionFact exists |
| Identity Anchor Subtype | visible when governed profile is confirmed | available | Production VNext maps valid PHONE_NUMBER/EMAIL_ADDRESS profiles; missing/invalid/bare subtype stays generic |
| Identity Identifier | visible when independently confirmed | available | exact confirmed phone/email identifier is projected/searchable with privacy masking; invalid identifier fails closed without erasing valid subtype |
| Identity Context | hidden | requires Canonical | no selector/search ghost capability before governed membership |
| Device Continuity | hidden | requires Canonical | future replace-device flow needs governed device/factor semantics |
| Digital Resource Continuity | hidden | requires Canonical | future domain/DNS/repository/cloud continuity remains gated |
| Trusted Handoff | hidden | requires Canonical | no trusted-party/handoff execution without governed authority |
| Recovery Preparedness | hidden | requires Canonical | no preparedness UI before Factor/SecretLocator semantics |
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

Lifecycle Persistence, RegionFact, Identity Context,
Device/Digital Resource/Trusted Handoff future semantics and Recovery Preparedness
still need their own shared Canonical/runtime activation before Production can own
them.

Phone/email subtype, independently confirmed identifier value, and atomic manual
Number/Email creation are no longer in this HOLD list: R37/R38 govern them.

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
confirmed PHONE_NUMBER subtype != confirmed phone identifier value
confirmed EMAIL_ADDRESS subtype != confirmed email identifier value
path count != independent path count
provider support != user configuration
five-primary IA remains five
~~~


## 10.1 R37 governed identity subtype authority

R37 closes the earlier read/classification HOLD without weakening fail-closed behavior:

~~~text
Node.kind = identity_anchor
+
valid fields_json.identity_anchor_profile
+
version = 1
+
subtype ∈ {phone_number, email_address, other_identity}
+
verification_basis_type ∈ {user_confirmed, authoritative_source}
+
confirmed_at present
→ confirmed subtype Reality
~~~

Production consequences:

~~~text
PHONE_NUMBER → Production Number list/detail/search + replace-phone entry
EMAIL_ADDRESS → Production Email list/detail/search
OTHER / missing / malformed → Generic Identity
bare fields_json.subtype → Generic Identity
~~~

The UI is not authority. `PlanRepository.createPlanForScenario` independently re-checks
the target kind and governed PHONE_NUMBER subtype before creating
`replace_phone_number`.

Still unavailable:
- raw phone/email identifier value as a governed typed field;
- automatic carrier/provider inference;
- automatic recovery-role inference;
- manual Number/Email creation until Node + governed profile can be committed atomically.

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

## 11. R31 Manual Relationship authority correction

A fresh repository audit supersedes the earlier R25/R26 Native-Schema-v4 HOLD for
the **current runtime relation set**.

Current Canonical v3 already has:
- the five runtime relation values in `domain.json`;
- the same five entries in `RelationDefinitionRegistry`;
- capability and endpoint-kind validation;
- a Dependency table capable of persisting them;
- cross-platform/conformance coverage for those runtime relations.

Therefore Production VNext may authoritatively confirm exactly:

~~~text
funding_source      → payment
merchant_agreement  → payment
recovers            → recovery
authenticates       → authentication
controls            → access
~~~

Production still MUST NOT create the storage-known/future relations:

~~~text
verifies
bound_to
~~~

The R31 authority path is:

~~~text
Production form
→ relation definition from canonical runtime registry
→ AppContainerVNextManualRelationshipGateway
→ AppContainer.createManualDependency
→ GraphRepository.createManualDependency
→ validateRelationUse
→ one authoritative transaction
→ graphRevision bump
→ re-read Dependency
~~~

Preview remains read-only.

This is **not** a partial ad-hoc v3 fork: the production form exposes the entire
current Canonical runtime registry and nothing outside it. Any future relation
widening still requires the normal shared Canonical/codegen/conformance path.


## 12. R32/R33 launcher authority boundary

Production VNext source authority now has a debug-only real-Reality rehearsal route,
but that does not change release authority.

```text
productionDebug + explicit vnext_production
→ real AppContainer
→ same PdigSecureContent / LockGate
→ ProductionVNextShell

productionRelease
→ legacy production shell
→ VNext intent extras ignored
```

R33 adds an API36 runtime proof for this boundary. A passing rehearsal proves the
debug cutover path is structurally viable and fail-closed; it does **not** grant
release cutover authority.

Release activation remains a separate decision requiring:
- exact-head security/runtime evidence;
- rollback path;
- persistence/restart parity;
- Android Reference Freeze / human acceptance as applicable.

Permanent rule:

```text
debug rehearsal available
!=
release cutover approved
```
