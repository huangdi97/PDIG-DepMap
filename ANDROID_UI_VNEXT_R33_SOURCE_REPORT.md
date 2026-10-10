# ANDROID_UI_VNEXT_R33_SOURCE_REPORT

> 2026-10-10 · `feat/android-ui-vnext-translation`
>
> R33 turns the R32 debug-only Production VNext launcher rehearsal into an explicit
> **API36 security/runtime evidence gate** and repairs the exact Kotlin compile defect
> exposed by the first R32 CI run.
>
> This report does not claim the new runtime gate has passed until its exact-head
> workflow completes successfully.

## 0. Current truth

```text
ANDROID_UI_VNEXT_SOURCE = R33

FIVE_PRIMARY_IA = FROZEN
REFERENCE_PRODUCT_UX_DESIGN = COMPLETE
ANDROID_LIGHT_DIRECTION = DESIGN_FROZEN

PRODUCTION_VNEXT_DEBUG_REHEARSAL = SOURCE_IMPLEMENTED
PRODUCTION_VNEXT_SECURE_RUNTIME_GATE = WORKFLOW_IMPLEMENTED
PRODUCTION_VNEXT_FLAG_SECURE_PARITY = SOURCE_IMPLEMENTED
PRODUCTION_RELEASE_DEFAULT = LEGACY_LOCK_GATED
PRODUCTION_RELEASE_INTENT_OVERRIDE = FORBIDDEN

R32_COMPILE_DEFECT = FIXED_IN_SOURCE
R33_EXACT_HEAD_CI = PENDING
R33_SECURE_REHEARSAL_RUNTIME = PENDING

IDENTITY_SUBTYPE_CANONICAL = HOLD
LIFECYCLE_CANONICAL = HOLD
MAINACTIVITY_RELEASE_CUTOVER = HOLD

ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_CUTOVER = HOLD
```

## 1. R32 CI defect closed at source

The first R32 exact-head CI found a real Kotlin compiler error in
`ProductionManualRelationshipScreen`.

Cause:

```text
nullable From / To / relation state
→ captured inside LazyColumn item lambdas
→ Kotlin smart-cast is not stable across mutable delegated state
```

The first repair only snapshotted To/Relation and also accidentally referenced the
snapshot before declaration. R33 repairs the actual boundary:

```text
confirmedFrom
confirmedTo
confirmedRelation
→ local immutable snapshots
→ only then build lazy items
```

The relation-capability explanation outside that guarded block continues to use the
currently non-null `relation` value directly.

No domain semantics changed.

## 2. New Production VNext secure rehearsal gate

New workflow:

`/.github/workflows/android-ui-vnext-production-rehearsal.yml`

New runtime proof:

`/android/scripts/production_vnext_secure_rehearsal.py`

Target:

```text
API 36
productionDebug
real MainActivity
real AppContainer
real encrypted Reality path
same PdigSecureContent / LockGate
ProductionVNextShell
```

The proof is intentionally separate from Preview pixel evidence.

## 3. Security assertions

The rehearsal verifies these exact invariants.

### 3.1 Explicit Reality rehearsal starts fail-closed

```text
am start ... --ez vnext_production true
→ PDIG 已锁定
→ Production Reality UI absent
```

Forbidden while locked:
- `生产 Reality`;
- `个人控制面`;
- `建立基础设施`.

### 3.2 Unlock is explicit

On the clean CI emulator with no device credential:

```text
已知悉风险，本次进入
→ explicit tap
→ unlock
```

The script does not call `LockGate.unlock()` directly.

If the emulator unexpectedly exposes a real credential path, the workflow fails
instead of silently bypassing it.

### 3.3 Unlocked target must be Production VNext

After unlock:

```text
生产 Reality
现在 / 基础设施 / 变更 / 记录 / 我
```

All five primary destinations must be present in the actual runtime accessibility
tree.

This distinguishes the Production VNext rehearsal from the legacy shell.

### 3.4 Background relocks

```text
Production VNext unlocked
→ HOME / ON_STOP
→ resume MainActivity
→ PDIG 已锁定
→ no Reality content before re-auth
```

After explicit re-entry, the same Production VNext target must return.

### 3.5 Process restart relocks

```text
force-stop
→ launch vnext_production
→ locked
```

No process-lifetime unlock persistence is allowed.

### 3.6 Default productionDebug remains legacy

Without the explicit rehearsal extra:

```text
productionDebug default
→ same lock
→ unlock
→ legacy “我的基础设施”
→ NOT “生产 Reality”
```

This proves that R33 does not silently perform release/default cutover.

## 4. Production authority contracts run in the same workflow

Before the real-Activity rehearsal, the workflow also runs:

- `ProductionVNextShellContractTest`
- `ProductionManualRelationshipContractTest`

This cross-checks:
- five-primary Production shell;
- Reality-only data projection;
- authoritative manual relationship save;
- no Preview fixture fallback.

The real MainActivity proof then validates launcher/security integration.

## 5. Sensitive-page screenshot / Recents protection

R33 found a second security-parity requirement beyond the lock gate:

```text
same LockGate
!=
same sensitive-window protection
```

Legacy production already uses Android `FLAG_SECURE` for concrete dependency,
import/review, timeline, source and plan surfaces. Production VNext originally reused
the lock lifecycle but did not call that window guard.

R33 now:
- refactors `SecureWindow` so non-legacy route models can reuse the same
  WindowManager implementation;
- adds an explicit `productionVNextRequiresSecureWindow(VScreen)` policy;
- applies it at the Production VNext shell root;
- pins the policy with JVM tests.

Protected Production VNext classes include:
- Infrastructure and concrete object categories/details;
- Records;
- Review;
- Sources;
- Establish / Manual Establish / Manual Relationship;
- Change plan execution;
- Reality search;
- appearance editors that can contain real identifiers/background images.

High-level Now / Me / Settings / Change Center remain aligned with the legacy
overview usability policy and do not force `FLAG_SECURE`.

This closes screenshot/recording + Recents-thumbnail parity at source level; runtime
evidence still belongs to the exact-head rehearsal/acceptance pass.

## 5. Release boundary remains unchanged

R33 does **not** change:

```text
productionRelease
→ legacy lock-gated PdigApp
→ ignores vnext_demo
→ ignores vnext_production
```

R33 only strengthens the rehearsal evidence for:

```text
productionDebug + vnext_production
```

Final release cutover still requires an explicit product/release decision after
runtime, security, persistence and rollback evidence.

## 6. Five-primary decision remains permanent

The Production rehearsal explicitly proves:

```text
现在
基础设施
变更
记录
我
```

`我` remains a first-class primary destination. It is not downgraded to an avatar
or utility shortcut.

## 7. Canonical gates remain honest

R33 does not route around the remaining shared-schema gates.

Still HOLD:
- Identity Anchor subtype → Production Number / Email specialization;
- Asset Lifecycle & Maintenance → Production fee/billing/keep-number persistence.

Current Production behavior remains conservative:
- coarse `identity_anchor` stays Generic Identity;
- lifecycle values absent from Canonical remain absent/unknown.

## 8. Evidence artifacts

The secure rehearsal workflow uploads:

```text
artifacts/runtime-evidence/production-vnext-rehearsal/
├─ device-profile.txt
├─ 01-reality-debug-locked.xml
├─ 02-reality-debug-unlocked.xml
├─ 03-background-relocked.xml
├─ 04-resume-production-vnext.xml
├─ 05-process-restart-locked.xml
├─ 06-default-production-locked.xml
├─ 07-default-production-legacy.xml
└─ manifest.json
```

plus Android instrumentation reports.

A workflow PASS is runtime/security evidence. It is still not Human Pixel
Acceptance and does not grant Android Reference Freeze by itself.

## 9. Remaining non-fakeable gates

```text
R33 exact-head CI
R33 API36 Production secure rehearsal
API36 Preview phone pixels
API36 tablet pixels
GPU runtime evidence
Human Pixel Acceptance
Android Reference Freeze
release cutover / rollback decision
future Canonical implementations where approved
```

## 10. Stop line

```text
R33_SECURE_REHEARSAL_GATE = SOURCE_IMPLEMENTED
R33_RUNTIME_RESULT = PENDING

DEBUG_REHEARSAL != RELEASE_CUTOVER
BUILD_PASS != HUMAN_ACCEPTANCE
REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_RELEASE_CUTOVER = HOLD
```


## 11. Presentation privacy and persistence closure

A production-cutover audit separated three different privacy mechanisms:

```text
LockGate
!=
FLAG_SECURE
!=
user-selected privacy masking
```

R33 now covers all three independently.

### User-selected masking

The existing Production card face already respected `app.privacyMask`, but card
tail digits could still appear in:
- Infrastructure payment-asset summaries;
- Card Detail metadata;
- Production Search result subtitles.

R33 centralizes production card-tail labels and applies masking consistently.
Search can still match the local confirmed tail value while masking is enabled,
but the result never echoes the digits back to the visible UI.

### Local persistence

Production VNext already receives `VNextShellViewModel.app` from MainActivity.
That ViewModel loads and persists:
- workspace preferences;
- PresentationProfile;
- number display aliases.

Therefore privacy/motion/upcoming/rail preferences are already store-backed in the
Production rehearsal path. They are Presentation state only and never mutate
Canonical / Personal Reality.

`ProductionPresentationPersistenceContractTest` now pins the workspace preference
round-trip across fresh ViewModel instances and restores the test-app's original
preferences afterward.

Permanent boundary:

```text
local presentation persistence
!=
Canonical persistence
```
