# Device Continuity v1 — Product / Change Proposal

> Date: 2026-10-10
> Status: **DESIGN_COMPLETE / FUTURE CHANGE PRIMITIVE / NOT_IMPLEMENTED**
>
> Basis: PDIG v2.3-R1 roadmap v0.4.
>
> Device Continuity is the next planned-continuity layer after replace_phone_number.
> It is not active Incident Recovery and does not introduce a new primary tab.

## 1. Product question

Device Continuity answers:

> **“Before I replace this device, what access/authentication/recovery capabilities
> must be established and verified elsewhere so the old device can be retired?”**

It is a planned change.

~~~text
replace_device
!=
lose_device
!=
compromise_device
~~~

The latter two belong to future Incident Recovery.

## 2. Why device replacement is not a generic copy operation

A phone/computer may simultaneously carry:

~~~text
Passkeys
TOTP authenticator
Push approval
eSIM
SMS-capable number
Password manager session
Bank apps
Email sessions
Security certificates
Offline files
Provider recovery state
~~~

Some are:
- device-bound;
- provider-synced;
- remotely re-establishable;
- not migratable at all;
- unknown.

Therefore “copy data to new phone” is not enough to prove continuity.

## 3. Change Primitive

Long-term mapping:

~~~text
replace_device
= REPLACE × device(phone/computer)
~~~

Do not activate it until:
- governed device subtype exists or equivalent typed metadata is shared;
- factor substrate exists;
- production scenario/template is registered;
- cross-platform behavior is tested.

No UI-only route may simulate an executable production plan.

## 4. Device subtype

v2.3 recommends subtype over NodeKind explosion.

Minimum desired:

~~~text
device:
  PHONE
  COMPUTER
  TABLET
  SECURITY_KEY
  OTHER
~~~

Security Key may eventually need specialized behavior, but v1 should avoid a new
top-level UI category.

A generic device with unknown subtype remains generic.

## 5. Continuity input

A Device Continuity analysis consumes only governed truth:

~~~text
target device
confirmed Dependency graph
AccessFactor / RecoveryFactor bindings
FailureDomains
ProviderPolicies
active ChangePlans
IdentityAnchor profiles
Maintenance state
current graphRevision
~~~

It does not scan the operating system and silently treat installed apps as Reality.

Future local discovery may propose facts for Human Review.

## 6. Factor migration classes

Each factor/use should classify migration behavior:

~~~text
RE_ESTABLISH_ON_NEW_DEVICE
PROVIDER_SYNCED
TRANSFER_REQUIRED
DEVICE_BOUND_NON_TRANSFERABLE
ROAMING_INDEPENDENT
NO_DEVICE_DEPENDENCE
UNKNOWN
~~~

Examples:

### Synced passkey
~~~text
factor = PASSKEY
portability = PROVIDER_SYNCED
action = verify provider account + enroll/use on new device
~~~

### Device-bound passkey
~~~text
factor = PASSKEY
portability = DEVICE_BOUND
action = create/verify replacement authenticator before old-device retirement
~~~

### TOTP
~~~text
factor = TOTP
portability = UNKNOWN until confirmed
action = verify export/re-enrollment/backup strategy
~~~

### Security key
~~~text
factor = SECURITY_KEY
portability = ROAMING_INDEPENDENT
action = verify usable on new environment
~~~

### eSIM
~~~text
identity/communication carrier
action = provider-specific transfer/reissue + communication verification
~~~

## 7. Passkey boundary

A passkey is not automatically “on the device” in one universal sense.

PDIG must distinguish:
- synced passkey;
- device-bound passkey;
- unknown portability.

FIDO design references:
- https://fidoalliance.org/white-paper-displace-password-otp-authentication-with-passkeys/
- https://fidoalliance.org/white-paper-high-assurance-enterprise-fido-authentication/

A synced passkey may survive device replacement but remain correlated with the same
provider account. That can preserve availability while still sharing a FailureDomain.

## 8. Provider account bootstrap problem

A common correlated-failure pattern:

~~~text
new device setup
→ needs provider account access
→ provider account authentication/recovery
→ depends on factor carried by old device
~~~

This can produce:
- blocking prerequisite;
- recovery cycle;
- needs_review.

PDIG must show the actual dependency chain instead of saying “cloud sync available”.

## 9. Make-Before-Break

Permanent order:

~~~text
Analyze old-device capabilities
→ Prepare independent/new carriers
→ Establish new device
→ Re-establish / transfer factors
→ Verify authentication/recovery/communication
→ Verify critical downstream accounts/services
→ Retire / wipe old device
~~~

The final retire/wipe action remains blocked until required verification is complete.

~~~text
done != verified
~~~

## 10. Current / Transition / After

Reuse the same continuity grammar already established for phone/card change.

### Current
~~~text
old device active
new device absent/not established
current confirmed factors/relations
~~~

### Transition
~~~text
old + new coexist
factor migration/re-enrollment underway
unknowns remain visible
retirement blocked
~~~

### After
~~~text
Plan Projection only
new device expected to carry verified capabilities
old device planned retired
~~~

After never becomes Reality because the user selected the tab.

## 11. Action DAG

Illustrative generic actions:

~~~text
A1 Review device-carried factors
A2 Establish new device
A3 Verify provider/bootstrap account
A4 Re-establish device-bound passkeys
A5 Verify synced passkeys
A6 Transfer/re-enroll TOTP
A7 Transfer/reissue eSIM if applicable
A8 Verify password-manager access
A9 Verify critical account authentication
A10 Verify recovery paths
A11 Retire sessions on old device
A12 Wipe/retire old device
~~~

Prerequisites are explicit.

Example:

~~~text
A2 → A3
A3 → A4/A5/A6/A8
A4/A5/A6/A8 → A9/A10
A9 + A10 → A11
A11 → A12
~~~

No completed-action subtraction shortcut may determine readiness.

## 12. Scope selection

Device replacement can have a huge blast radius.

The product should group effects by consumer meaning:

~~~text
登录与验证
恢复路径
号码 / eSIM
密码管理器
关键账户
关键服务
待核对
~~~

Do not show raw edge counts as the primary view.

Impact Lens remains explanation-first.

## 13. FailureDomain

Device replacement is the canonical demonstration of correlated failure.

Example:

~~~text
SMS recovery
TOTP
push approval
device-bound passkey
~~~

may all collapse under:

~~~text
DEVICE: old phone
~~~

The UI can say:

> 这些方式看起来不同，但都依赖这台手机。

It must not say “4 backup methods”.

## 14. RecoveryCycle

Before retirement, detect confirmed recovery cycles.

Example:

~~~text
Provider Account
→ passkey sync on old phone
→ password manager on old phone
→ provider recovery depends on same account
~~~

Confirmed-cycle output must show an explainable path and an external root if one
exists.

Proposal-only hints remain potential cycle / needs_review.

## 15. SecretLocator integration

Device replacement should ask whether critical secret-backed factors remain
retrievable, without storing secret values.

Examples:
- recovery code locator;
- security-key physical location;
- password-manager account availability.

PDIG never copies secrets between devices.

## 16. eSIM / phone-number composition

Device and phone-number replacement are separate operations.

~~~text
replace_device
may include communication/eSIM transfer

replace_phone_number
changes the identity anchor itself
~~~

Do not conflate:
- moving the same eSIM/number to a new device;
- replacing the phone number.

If both occur, Scenario composition should make both operations explicit.

## 17. Session / provider actions

Future provider-aware actions may include:
- enroll replacement device;
- revoke old-device session;
- rotate compromised authenticator;
- transfer eSIM;
- approve new device.

ProviderPolicy can define possibilities/constraints.

Unless a governed connector exists, PDIG renders a checklist and verification step;
it does not claim to execute the provider action.

## 18. Old device retirement

Retirement has at least three distinct concepts:

~~~text
PDIG object archived/retired
provider sessions revoked
physical device wiped
~~~

One action must not silently imply the other two.

The plan should track them separately where relevant.

## 19. Verification

Verification should target capabilities, not merely “new phone works”.

Examples:

~~~text
Passkey authentication succeeds for Account A
TOTP works for Account B
Recovery route verified for Account C
eSIM receives expected communication
Password manager can unlock independently
~~~

Future Observation may suggest evidence but does not auto-verify.

## 20. Consumer UI

No sixth primary tab.

Entry points after production support:
- Device Detail → 分析更换设备影响;
- Change Center → 更换设备;
- Now → active device migration card.

Focused route:

~~~text
/change/device
~~~

Hierarchy:

~~~text
Change Device → Up → Change
~~~

Phone:
- vertical OLD → affected capability groups → NEW.

Medium/Expanded:
- three-pane OLD → capability/factor migration → NEW.

## 21. Recovery Preparedness vs Device Change

Recovery Preparedness answers:
> “Do I have independently usable recovery resources before anything happens?”

Device Change answers:
> “How do I replace this device without losing capabilities?”

Recovery Incident answers:
> “The device is already unavailable; what survives now?”

Keep these separate.

## 22. Device loss mid-transition

If the old device becomes unavailable during planned replacement:

~~~text
planned ChangePlan
+ new explicit incident
→ re-evaluate against current Reality/availability
→ may enter Recovery Incident
~~~

Do not continue rendering the planned transition as if the old device still works.

## 23. Security

Never persist:
- device unlock PIN;
- biometric template;
- private keys;
- TOTP seeds;
- session tokens;
- recovery codes.

May persist governed metadata:
- factor type;
- carrier/device binding;
- enrollment/freshness state;
- SecretLocator metadata.

## 24. External security alignment

NIST and FIDO both reinforce explicit authenticator lifecycle and recovery planning:
- https://csrc.nist.gov/pubs/sp/800/63/4/final
- https://csrc.nist.gov/pubs/sp/800/63/B/4/final
- https://fidoalliance.org/white-paper-displace-password-otp-authentication-with-passkeys/
- https://fidoalliance.org/white-paper-high-assurance-enterprise-fido-authentication/

These are design constraints only, never user-specific Reality.

## 25. Required prerequisites

Before executable Production UI:

~~~text
AccessFactor / RecoveryFactor Canonical
device subtype semantics
IdentityAnchor subtype where communication factors need it
FailureDomain parity
RecoveryCycle parity
ChangePrimitive REPLACE × device
Scenario template
Action DAG
Production plan binding
phone/tablet runtime evidence
~~~

Identity Context is not required to execute replace_device.

Full Recovery Solver is not required for planned replace_device, but Incident
Recovery remains hidden until its solver exists.

## 26. Conformance fixtures

~~~text
DC-01 replace phone device with device-bound passkey
DC-02 synced passkey survives device loss but shares provider domain
DC-03 TOTP + SMS on same phone not independent
DC-04 separate hardware key remains independent only with confirmed domains
DC-05 eSIM same-number transfer distinct from replace_phone_number
DC-06 new device established but factor verification pending => retirement blocked
DC-07 done migration action but verification pending => not ready
DC-08 confirmed recovery cycle generates needs-review/block
DC-09 proposal factor cannot satisfy prerequisite
DC-10 provider support without enrollment cannot satisfy action
DC-11 old device becomes unavailable mid-transition => revalidation required
DC-12 after view remains Plan Projection
DC-13 session revoke and physical wipe are distinct
DC-14 no secret material enters plan payload
DC-15 deterministic Action DAG/topological order
DC-16 graph revision change rebases non-terminal plan
~~~

## 27. Visibility gate

Until prerequisites land:

~~~text
DEVICE_CONTINUITY_DESIGN = COMPLETE
DEVICE_CONTINUITY_REFERENCE_UI = HOLD
DEVICE_CONTINUITY_PRODUCTION = HOLD
NO_NEW_PRIMARY_TAB = REQUIRED
~~~

When enabled, it enters through Device Detail / Change Center, not a new root.
