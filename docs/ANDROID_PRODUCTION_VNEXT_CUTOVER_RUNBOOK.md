# Android Production VNext Cutover / Rollback Runbook

> Date: 2026-10-10  
> Status: **R36 SOURCE-IMPLEMENTED / RELEASE NOT APPROVED**
>
> Purpose: make Production UI generation an explicit, auditable, fail-closed release
> decision without weakening the existing App Lock, Reality authority or rollback
> path.

---

## 0. Current release state

~~~text
repository default:
pdigProductionUiGeneration = legacy
pdigProductionVNextCutoverApproved = false

therefore:
productionRelease default = LEGACY_PRODUCTION
~~~

R36 does **not** change the current shipping default.

---

## 1. Why cutover is build-time

Production UI generation is not a user preference.

It must not be controlled by:
- Intent extras;
- deep links;
- PresentationProfile;
- local Settings;
- remote config;
- analytics/experimentation SDK;
- Preview state.

Reason:

~~~text
legacy vs VNext
changes which production control surface owns real Personal Reality
~~~

That is a release-authority decision.

PDIG is local-first and currently has no server-side control plane that should be
trusted with this decision.

---

## 2. Two-key activation

Production VNext release requires both:

~~~text
-PpdigProductionUiGeneration=vnext
-PpdigProductionVNextCutoverApproved=true
~~~

Any other state fails closed to legacy.

Truth table:

| generation | approval | release default |
| --- | --- | --- |
| legacy | false | legacy |
| legacy | true | legacy |
| vnext | false | legacy |
| vnext | true | Production VNext |
| unknown/typo | any | build fails or policy falls back legacy |

Gradle accepts only:

~~~text
legacy
vnext
~~~

Approval accepts only:

~~~text
true
false
~~~

The Kotlin launch policy still parses unknown runtime strings as legacy as an
additional fail-closed layer.

---

## 3. Intent boundary

Production release ignores:

~~~text
vnext_demo
vnext_production
~~~

They cannot cut over release behavior.

Debug retains:
- vnext_demo → Reference Preview;
- vnext_production → real-Reality VNext rehearsal.

Debug can also rehearse the exact release-default path by using the two build-time
keys and launching with no extras.

---

## 4. Security invariance

Both future release VNext and current debug reality rehearsal use:

~~~text
MainActivity
→ AppContainer
→ createProductionVNextSession
→ ProductionVNextSecureHost
→ PdigSecureContent
→ LockGate
~~~

There is no second VNext-specific lock implementation.

Required invariants:
- first Reality frame never renders while locked;
- background → relock;
- resume → capability refresh + relock;
- process restart → locked;
- secure-window policy remains applied on sensitive screens;
- Preview synthetic fixture never enters this branch.

---

## 5. Release-candidate build commands

### Legacy-default control build

~~~bash
cd android
./gradlew :app:assembleProductionRelease
~~~

### VNext-default candidate

~~~bash
cd android
./gradlew :app:assembleProductionRelease \
  -PpdigProductionUiGeneration=vnext \
  -PpdigProductionVNextCutoverApproved=true
~~~

Production signing remains governed by the existing signing policy. This runbook
does not introduce or store signing credentials.

---

## 6. API36 rehearsal

Workflow:

~~~text
.github/workflows/android-ui-vnext-production-rehearsal.yml
~~~

It proves two configurations.

### A. Repository-default productionDebug

Expected:
- explicit vnext_production debug extra can enter real Reality VNext after unlock;
- default no-extra launch stays legacy;
- both fail closed before unlock.

### B. Two-key VNext-default productionDebug

Expected:
- no Intent extras;
- default launcher fails closed behind LockGate;
- unlock enters real Reality Production VNext;
- five primary destinations exist;
- no SYNTHETIC/reference marker;
- background/resume relocks;
- process restart relocks.

Evidence:

~~~text
artifacts/runtime-evidence/production-vnext-rehearsal/
artifacts/runtime-evidence/production-vnext-release-default/
~~~

A debug rehearsal is still not release approval.

---

## 7. Preconditions before changing the release default

All must be explicitly satisfied:

~~~text
[ ] exact candidate CI pass
[ ] Canonical/conformance pass for current schema
[ ] API36 Preview phone runtime pass
[ ] tablet runtime pass
[ ] GPU runtime evidence pass
[ ] human pixel acceptance
[ ] Android Reference Freeze
[ ] Production secure rehearsal pass
[ ] release-default no-extra rehearsal pass
[ ] production persistence/restart E2E pass
[ ] import/review/manual-create/manual-relation flows verified
[ ] ChangePlan done != verified semantics verified
[ ] backup/restore/security regression unchanged
[ ] rollback build produced and install-tested
[ ] release signing/store prerequisites explicitly reopened
~~~

A missing line means cutover remains HOLD.

---

## 8. Cutover operation

When the release decision is approved, do not change source routing logic.

Build the release with the two keys:

~~~text
generation = vnext
approval = true
~~~

Record:
- exact source SHA;
- exact Gradle command;
- APK/AAB hash;
- signing identity;
- schema/payload version;
- conformance report;
- rehearsal evidence;
- acceptance decision.

The release artifact, not a runtime toggle, carries the UI-generation decision.

---

## 9. Rollback

Rollback does not mutate Personal Reality.

Build/release:

~~~text
generation = legacy
approval = false
~~~

or simply use repository defaults.

Rollback is permitted only if the legacy shell still reads the same current
Canonical schema/payload.

If a future VNext release also activates a new Canonical schema that legacy cannot
read, UI rollback and data rollback become different operations. At that point this
runbook must be revised before the schema release.

Permanent rule:

~~~text
UI rollback
!=
schema downgrade
~~~

Never attempt to downgrade an encrypted user database merely to restore the old UI.

---

## 10. Schema compatibility gate

Before any future Canonical expansion ships together with VNext:

~~~text
legacy reader compatibility?
VNext reader compatibility?
migration forward-only?
export/import compatibility?
rollback UI still able to open Reality?
~~~

If legacy cannot safely read the post-migration database, the rollback strategy must
be:
- previous signed app only if compatible, or
- a VNext hotfix release,
not a blind legacy switch.

R36 cutover design therefore intentionally happens before new v4 persistence.

---

## 11. Observability without analytics

PDIG does not add analytics merely for cutover.

Release evidence may use:
- local diagnostic status;
- explicit source SHA;
- build generation marker;
- crash-free manual/runtime evidence;
- user-reported diagnostics where deliberately exported.

Do not send Personal Reality, names, numbers, graph structure or recovery metadata
to an analytics backend.

---

## 12. Forbidden shortcuts

~~~text
Intent extra enables Production VNext release         FORBIDDEN
Settings toggle changes production UI generation      FORBIDDEN
remote config changes Reality control surface         FORBIDDEN
synthetic fixture enters production branch            FORBIDDEN
VNext bypasses PdigSecureContent                      FORBIDDEN
cutover approval inferred from CI green               FORBIDDEN
UI rollback silently downgrades schema                FORBIDDEN
~~~

---

## 13. Stop line

~~~text
R36_PRODUCTION_UI_GENERATION_POLICY = SOURCE_IMPLEMENTED
R36_TWO_KEY_CUTOVER = SOURCE_IMPLEMENTED
R36_RELEASE_DEFAULT_REHEARSAL = SOURCE_IMPLEMENTED
CURRENT_RELEASE_DEFAULT = LEGACY

ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_RELEASE_CUTOVER = HOLD
~~~
