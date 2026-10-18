# Android Production VNext Cutover / Rollback Runbook

> Date: 2026-10-10; cutover approval recorded 2026-10-18  
> Status: **CUTOVER APPROVED / RELEASE CANDIDATE VERIFIED / STORE RELEASE HOLD**
>
> Purpose: make Production UI generation an explicit, auditable, fail-closed release
> decision without weakening the existing App Lock, Reality authority or rollback
> path.
>
> The user approved visual acceptance, Android Reference Freeze and Production
> Cutover on 2026-10-18. The approved artifact carries `vnext` + `true`; the
> repository Gradle defaults intentionally remain `legacy` + `false`. Store
> release is still `HOLD` because no production signing identity is configured.

---

## 0. Current release state

```text
repository default:
pdigProductionUiGeneration = legacy
pdigProductionVNextCutoverApproved = false

approved cutover artifact (2026-10-18):
PRODUCTION_UI_GENERATION = vnext
PRODUCTION_VNEXT_CUTOVER_APPROVED = true

therefore:
ordinary source build default = LEGACY_PRODUCTION
approved release artifact = PRODUCTION_VNEXT
```

R36 source defaults stay Legacy. Cutover is carried by the signed release
artifact, per §8; the two keys are never inferred at runtime.

### Approved artifact record

| Item | Value |
| --- | --- |
| Code tree | `7aaa612c6a6dfdd0ef431a8a67879f1caef63f96` |
| Artifact `GIT_SHA` | `01d0de3a` |
| Version | `0.3.1` (2) |
| APK SHA-256 | `cd8afab1e5981ac844c55a648c75c921016d84fcd5a221efc997a91e0be99d9c` |
| Legacy control SHA-256 | `287afd586aba3e5ba72ae311c738ef5cf1d550c1b3c6b4809e991a59a2120316` |
| Signer | `CN=PDIG NON-PRODUCTION TEST KEY` (`953dc774…b13ad4`) |
| Default no-extra launch | Production VNext after unlock |
| Legacy control launch | Legacy shell after unlock |
| Store release | `HOLD` — no production signing identity |

Full evidence: `artifacts/runtime-evidence/2026-10-17-r61-plus/production-cutover/`.

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

```text
legacy vs VNext
changes which production control surface owns real Personal Reality
```

That is a release-authority decision.

PDIG is local-first and currently has no server-side control plane that should be
trusted with this decision.

---

## 2. Two-key activation

Production VNext release requires both:

```text
-PpdigProductionUiGeneration=vnext
-PpdigProductionVNextCutoverApproved=true
```

Any other state fails closed to legacy.

Truth table:

| generation   | approval | release default                         |
| ------------ | -------- | --------------------------------------- |
| legacy       | false    | legacy                                  |
| legacy       | true     | legacy                                  |
| vnext        | false    | legacy                                  |
| vnext        | true     | Production VNext                        |
| unknown/typo | any      | build fails or policy falls back legacy |

Gradle accepts only:

```text
legacy
vnext
```

Approval accepts only:

```text
true
false
```

The Kotlin launch policy still parses unknown runtime strings as legacy as an
additional fail-closed layer.

---

## 3. Intent boundary

Production release ignores:

```text
vnext_demo
vnext_production
```

They cannot cut over release behavior.

Debug retains:

- vnext_demo → Reference Preview;
- vnext_production → real-Reality VNext rehearsal.

Debug can also rehearse the exact release-default path by using the two build-time
keys and launching with no extras.

---

## 4. Security invariance

Both future release VNext and current debug reality rehearsal use:

```text
MainActivity
→ AppContainer
→ createProductionVNextSession
→ ProductionVNextSecureHost
→ PdigSecureContent
→ LockGate
```

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

```bash
cd android
./gradlew :app:assembleProductionRelease
```

### VNext-default candidate

```bash
cd android
./gradlew :app:assembleProductionRelease \
  -PpdigProductionUiGeneration=vnext \
  -PpdigProductionVNextCutoverApproved=true
```

Production signing remains governed by the existing signing policy. This runbook
does not introduce or store signing credentials.

---

## 6. API36 rehearsal

Workflow:

```text
.github/workflows/android-ui-vnext-production-rehearsal.yml
```

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

```text
artifacts/runtime-evidence/production-vnext-rehearsal/
artifacts/runtime-evidence/production-vnext-release-default/
```

A debug rehearsal is still not release approval.

---

## 7. Preconditions before changing the release default

All must be explicitly satisfied:

```text
[x] exact candidate CI pass
    runtime 4/4 at 7aaa612c; core 5/5 at same-tree 874e13f0
[x] Canonical/conformance pass for current schema
[x] API36 Preview phone runtime pass
[x] tablet runtime pass
[x] GPU runtime evidence pass
[x] human pixel acceptance          (user decision, 2026-10-18)
[x] Android Reference Freeze        (user decision, 2026-10-18)
[x] Production secure rehearsal pass
[x] release-default no-extra rehearsal pass
    release APK cd8afab1… launches Production VNext with no extras
[x] production persistence/restart E2E pass
[x] import/review/manual-create/manual-relation flows verified
[x] ChangePlan done != verified semantics verified
[x] backup/restore/security regression unchanged
[x] rollback build produced and install-tested
    legacy/false APK 287afd58… opens the Legacy shell
[ ] release signing/store prerequisites explicitly reopened
    HOLD: no production keystore; candidate uses the non-production test key
```

The cutover is approved and the release candidate is verified, but the store
release line remains open. Do not upload to a store until a production signing
identity is provided and the store prerequisites are reopened explicitly.

---

## 8. Cutover operation

When the release decision is approved, do not change source routing logic.

Build the release with the two keys:

```text
generation = vnext
approval = true
```

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

### 2026-10-18 executed record

```text
code tree:          7aaa612c6a6dfdd0ef431a8a67879f1caef63f96
artifact GIT_SHA:   01d0de3a
command:
  gradlew :app:assembleProductionRelease \
    -PpdigNonProdSigning=true \
    -PpdigProductionUiGeneration=vnext \
    -PpdigProductionVNextCutoverApproved=true \
    -x lintVitalProductionRelease \
    -x lintVitalAnalyzeProductionRelease \
    -x lintVitalReportProductionRelease
APK:                cd8afab1e5981ac844c55a648c75c921016d84fcd5a221efc997a91e0be99d9c
signer:             CN=PDIG NON-PRODUCTION TEST KEY
schema/payload:      unchanged by this UI-only cutover
conformance:        green at 874e13f0 (same android/ tree)
rehearsal:          runtime 4/4 at 7aaa612c
acceptance:         user approved 2026-10-18
```

`lintVital*` was excluded only because the local Gradle cache is offline and
cannot resolve `lint-gradle:31.5.2`; CI lint/conformance jobs are green. The
artifact still passed installation, signature verification, BuildConfig audit,
default-route audit and rollback audit. This exclusion is a local build-cache
limitation, not a product failure, and must be removed when a networked build is
available.

---

## 9. Rollback

Rollback does not mutate Personal Reality.

Build/release:

```text
generation = legacy
approval = false
```

or simply use repository defaults.

Rollback is permitted only if the legacy shell still reads the same current
Canonical schema/payload.

If a future VNext release also activates a new Canonical schema that legacy cannot
read, UI rollback and data rollback become different operations. At that point this
runbook must be revised before the schema release.

Permanent rule:

```text
UI rollback
!=
schema downgrade
```

Never attempt to downgrade an encrypted user database merely to restore the old UI.

---

## 10. Schema compatibility gate

Before any future Canonical expansion ships together with VNext:

```text
legacy reader compatibility?
VNext reader compatibility?
migration forward-only?
export/import compatibility?
rollback UI still able to open Reality?
```

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

```text
Intent extra enables Production VNext release         FORBIDDEN
Settings toggle changes production UI generation      FORBIDDEN
remote config changes Reality control surface         FORBIDDEN
synthetic fixture enters production branch            FORBIDDEN
VNext bypasses PdigSecureContent                      FORBIDDEN
cutover approval inferred from CI green               FORBIDDEN
UI rollback silently downgrades schema                FORBIDDEN
```

---

## 13. Stop line

```text
R36_PRODUCTION_UI_GENERATION_POLICY = SOURCE_IMPLEMENTED
R36_TWO_KEY_CUTOVER = SOURCE_IMPLEMENTED
R36_RELEASE_DEFAULT_REHEARSAL = SOURCE_IMPLEMENTED

REPOSITORY_GRADLE_DEFAULT = LEGACY (intentional)
APPROVED_RELEASE_ARTIFACT = PRODUCTION_VNEXT (two-key, nonprod-signed)

HUMAN_ACCEPTED = ACCEPT (2026-10-18)
ANDROID_REFERENCE_FREEZE = APPROVED (2026-10-18)
PRODUCTION_VNEXT_RELEASE_CUTOVER = APPROVED / CANDIDATE_VERIFIED
STORE_RELEASE = HOLD (production signing identity not configured)
```
