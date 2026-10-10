# ANDROID_UI_VNEXT_R36_SOURCE_REPORT

> 2026-10-10 · feat/android-ui-vnext-translation
>
> R36 closes the Android Production VNext release-cutover / rollback architecture.
> It does not approve release cutover and does not change the repository default
> production UI from legacy to VNext.

---

## 0. Current truth

~~~text
ANDROID_UI_VNEXT_REFERENCE_SOURCE = R34
CANONICAL_EXPANSION_CONTROL = R35
ANDROID_RELEASE_CUTOVER_CONTROL = R36

PRODUCTION_VNEXT_SOURCE_BINDING = IMPLEMENTED
PRODUCTION_VNEXT_DEBUG_REHEARSAL = IMPLEMENTED
PRODUCTION_VNEXT_RELEASE_DEFAULT_REHEARSAL = IMPLEMENTED_SOURCE

CURRENT_RELEASE_DEFAULT = LEGACY
RELEASE_CUTOVER_APPROVED = NO

ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_RELEASE_CUTOVER = HOLD
~~~

---

## 1. R36 removes the final routing-design ambiguity

Before R36:

~~~text
preview → Reference VNext
productionDebug + Intent extra → real-Reality VNext rehearsal
production default/release → legacy
~~~

That was secure, but the eventual release operation was unspecified. A future agent
could have implemented release activation as:
- a user Setting;
- an Intent extra;
- remote config;
- a copied VNext-only MainActivity;
- a last-minute source edit.

R36 rejects all of those.

Production UI generation is now a build-time release decision.

---

## 2. Two-key cutover

The release target requires both:

~~~text
pdigProductionUiGeneration = vnext
pdigProductionVNextCutoverApproved = true
~~~

Any incomplete state remains legacy.

This separates:
- desired generation;
- explicit release approval.

A stale/inherited generation property cannot cut over by itself.

Gradle accepts only:

~~~text
pdigProductionUiGeneration = legacy | vnext
pdigProductionVNextCutoverApproved = true | false
~~~

Invalid build properties fail configuration.

The Kotlin policy independently maps an unknown generation to legacy so the runtime
decision remains fail-closed.

---

## 3. Release Intent boundary

Production release ignores:

~~~text
vnext_demo
vnext_production
~~~

Intent/deep-link state never owns release authority.

Debug continues to support explicit rehearsal routes.

Preview continues to use synthetic reference state.

---

## 4. Same secure Reality host

New launch target:

~~~text
PRODUCTION_REALITY_RELEASE
~~~

Both:

~~~text
PRODUCTION_REALITY_DEBUG
PRODUCTION_REALITY_RELEASE
~~~

enter the same implementation:

~~~text
real AppContainer
→ createProductionVNextSession
→ ProductionVNextSecureHost
→ PdigSecureContent / LockGate
~~~

There is no second release-specific lock implementation.

---

## 5. Release-default runtime rehearsal

New script:

~~~text
android/scripts/production_vnext_release_default_rehearsal.py
~~~

The production rehearsal workflow now tests two configurations.

### Default repository productionDebug

~~~text
default no-extra launch → legacy
explicit vnext_production → real VNext rehearsal
~~~

### Two-key productionDebug

Build:

~~~text
-PpdigProductionUiGeneration=vnext
-PpdigProductionVNextCutoverApproved=true
~~~

Then launch with:

~~~text
NO Intent extras
~~~

Required proof:
- first frame remains behind the canonical lock;
- no Reality content leaks before unlock;
- unlock enters Production VNext;
- five primary destinations exist;
- no SYNTHETIC/reference marker;
- background/resume relocks;
- process restart relocks.

This rehearses the future release-default routing path without approving a release.

---

## 6. Rollback

Rollback is another build decision:

~~~text
generation = legacy
approval = false
~~~

or repository defaults.

No Personal Reality mutation occurs merely because the UI generation changes.

Permanent distinction:

~~~text
UI rollback != schema downgrade
~~~

If a future VNext release activates a schema that the legacy shell cannot read,
legacy UI rollback can no longer be assumed safe. That compatibility must be
decided before the schema release.

R36 is deliberately implemented while current Canonical is still v3 so routing
rollback can be proven separately from future Canonical expansion.

---

## 7. Tests

VNextLaunchPolicyTest now pins:
- Preview always wins as Reference;
- production defaults to legacy;
- release Intent extras cannot cut over;
- generation alone cannot cut over;
- approval alone cannot cut over;
- both build keys produce PRODUCTION_REALITY_RELEASE;
- unknown generation fails closed;
- explicit debug Reality rehearsal remains available;
- explicit debug Reference remains available;
- debug may rehearse the release-default route without extras.

Runtime rehearsal separately proves MainActivity + LockGate behavior.

---

## 8. Runbook

Normative operational document:

~~~text
docs/ANDROID_PRODUCTION_VNEXT_CUTOVER_RUNBOOK.md
~~~

It defines:
- build commands;
- acceptance preconditions;
- artifact evidence;
- cutover operation;
- rollback operation;
- schema compatibility constraint;
- forbidden runtime toggles.

---

## 9. What R36 does not claim

~~~text
Android Reference Freeze = not claimed
human pixel acceptance = not claimed
Production VNext release approved = no
Play/store readiness = not claimed
production signing = unchanged / external
new Canonical schema = no
~~~

CI/rehearsal PASS is required but cannot self-approve release.

---

## 10. Stop line

~~~text
R36_PRODUCTION_CUTOVER_DESIGN = CLOSED
R36_TWO_KEY_POLICY = SOURCE_IMPLEMENTED
R36_RELEASE_DEFAULT_REHEARSAL = SOURCE_IMPLEMENTED
R36_ROLLBACK_RUNBOOK = FROZEN

CURRENT_PRODUCTION_RELEASE_DEFAULT = LEGACY
PRODUCTION_VNEXT_RELEASE_CUTOVER = HOLD
~~~
