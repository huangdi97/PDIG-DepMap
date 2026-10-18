# PDIG — R52+ Production runtime and cutover rehearsal report

> Final local source HEAD: `7aaa612c6a6dfdd0ef431a8a67879f1caef63f96`  
> Capture/build source for the rehearsals below: `6b823fb6`. Later deltas are a
> rail-label testTag, advanced Production consumer copy and androidTest-only
> 851dp robustness; Production gate behaviour is unchanged and is re-run by CI at
> the final source.  
> Branch: `feat/android-ui-vnext-translation` · PR #1 Draft  
> Device: Google `sdk_gphone64_x86_64`, API 36 (Android 16), 1080×2400 @420 dpi
> (CI-equivalent Pixel phone viewport)  
> Date: 2026-10-18 (Asia/Shanghai)

## 1. What this report proves — and what it does not

It proves, on a real API36 emulator:

- the repository-default `productionDebug` build still compiles
  `PRODUCTION_UI_GENERATION = "legacy"` and
  `PRODUCTION_VNEXT_CUTOVER_APPROVED = false`;
- its default launcher path remains **lock-gated Legacy**, not VNext;
- the same build, when explicitly launched with the debug rehearsal extra,
  fails closed, unlocks into real Production VNext, exposes the five primary
  roots, and relocks on background / process restart;
- a **separate** build with both explicit compile-time keys
  (`vnext` + `cutoverApproved=true`) starts locked with no extra, unlocks into
  Production VNext, exposes the five primary roots, relocks on background and on
  process restart, and never shows Preview/synthetic markers;
- the installed APK SHA-256 equals the locally built APK SHA-256 for the cutover
  rehearsal.

It does **not** approve a Production cutover, change the repository default,
merge PR #1, sign a release, or publish anything. `PRODUCTION_CUTOVER = HOLD`.

## 2. GitHub status inherited at the baseline

At the round-start remote HEAD `562d83f4`, the GitHub Production Rehearsal was
already green: run
[39023996578](https://github.com/huangdi97/PDIG-DepMap/actions/runs/39023996578),
conclusion `success`. The R51 handoff's "Production Rehearsal CI_RED" statement is
historical and is **not** carried forward as current state.

The local runs below are independent, exact-source local evidence at `6b823fb6`.
The final GitHub result is recorded after the round-2 push in the handoff report.

## 3. Default (shipping-path) build

Command:

```powershell
cd android
.\gradlew.bat :app:clean :app:assembleProductionDebug
```

Compiled `BuildConfig` (read from the generated file, not inferred from arguments):

```text
PRODUCTION_UI_GENERATION = "legacy"
PRODUCTION_VNEXT_CUTOVER_APPROVED = false
```

| Item | Value |
| --- | --- |
| APK | `C:\Users\Kaiser\pdig-build\app\outputs\apk\production\debug\app-production-debug.apk` |
| APK SHA-256 | `e65a6435dbabf822cb9aabfcb4c8db0372e670f9ead67ea0429264eba228e195` |
| Installed `base.apk` SHA-256 | `e65a6435dbabf822cb9aabfcb4c8db0372e670f9ead67ea0429264eba228e195` |
| Package / activity | `com.pdig.app` / `com.pdig.app.MainActivity` |

### Secure rehearsal result

```text
PRODUCTION_VNEXT_SECURE_REHEARSAL=PASS
checks:
  REALITY_DEBUG_FAIL_CLOSED
  REALITY_DEBUG_FIVE_PRIMARY
  BACKGROUND_RELOCK
  RESUME_RETURNS_PRODUCTION_VNEXT
  PROCESS_RESTART_RELOCK
  DEFAULT_PRODUCTION_REMAINS_LEGACY
```

Evidence:

- `artifacts/runtime-evidence/2026-10-17-r61-plus/production/production-vnext-rehearsal/`
  — 13 real UI XML captures + `manifest.json` + device profile;
- `.../production/03-default-legacy-locked.png` / `.xml` — default launcher is
  locked;
- `.../production/04-default-legacy-home.png` / `.xml` — after explicit
  acknowledgement the default route shows the Legacy home
  (“需要你处理 / 基础设施薄弱点 / 可能发生了变化”), not Production VNext.

No VNext marker, five-root marker, or synthetic reference marker is present on
the default locked route; the default unlocked route is Legacy.

## 4. Explicit two-key cutover rehearsal build

Command (clean build; the two keys are compile-time facts):

```powershell
cd android
.\gradlew.bat :app:clean :app:assembleProductionDebug `
  -PpdigProductionUiGeneration=vnext `
  -PpdigProductionVNextCutoverApproved=true
```

Compiled `BuildConfig`:

```text
PRODUCTION_UI_GENERATION = "vnext"
PRODUCTION_VNEXT_CUTOVER_APPROVED = true
```

| Item | Value |
| --- | --- |
| APK SHA-256 | `df576561f5ff862c9693d32ad14007f852b7fe7e50e3d3ef4e01a1ff2f494e99` |
| Installed `base.apk` SHA-256 | matched before interpreting the UI (the rehearsal script also asserts this) |
| Launch extras | `NONE` for the release-default path |
| Launch target (logcat proof) | `PRODUCTION_REALITY_RELEASE` · `flavor=production` · `generation=vnext` · `approved=true` · `debug=true` |
| Rehearsal result | `PRODUCTION_VNEXT_RELEASE_DEFAULT_REHEARSAL=PASS` |

Checks:

```text
DEFAULT_CUTOVER_FAIL_CLOSED
DEFAULT_CUTOVER_REALITY_FIVE_PRIMARY
DEFAULT_CUTOVER_BACKGROUND_RELOCK
DEFAULT_CUTOVER_RESUME_REALITY
DEFAULT_CUTOVER_PROCESS_RELOCK
```

The script also verifies that the unlocked Reality route contains none of the
synthetic markers `参考数据` / `SYNTHETIC`. The five primary roots are
`现在 / 基础设施 / 变更 / 记录 / 我`.

Evidence:

- `artifacts/runtime-evidence/2026-10-17-r61-plus/production/production-vnext-release-default/`
  — 5 cutover XML captures + `debug-launch-proof.log` + `manifest.json`;
- `.../production/01-cutover-locked.png` / `.xml` — real cutover locked screen;
- `.../production/02-cutover-unlocked.png` / `.xml` — real cutover Production
  Now with the GPU earth, five roots, empty-reality truth boundary and no
  synthetic markers.

## 5. Production authority contract tests

Command (default Legacy build, CI-equivalent viewport 1080×2400 @420):

```powershell
cd android
.\gradlew.bat :app:connectedProductionDebugAndroidTest `
  "-Pandroid.testInstrumentationRunnerArguments.class=com.pdig.uivnext.evidence.ProductionVNextShellContractTest,com.pdig.uivnext.evidence.ProductionRecordNavigationContractTest,com.pdig.uivnext.evidence.ProductionManualRelationshipContractTest"
```

Result:

```text
Starting 21 tests on pdig_evi_phone_api36(AVD) - 16
Finished 21 tests
BUILD SUCCESSFUL
```

At 1080×2340 @440 (851 dp tall) the suite now also passes **21/21**. `8fef8030`
scrolls to the tagged lazy change entry before asserting; the earlier failure was
test visibility, not a product failure, and no assertion was weakened.

## 6. Runtime truth boundaries

- The default build remains `legacy` / `false`; no repository configuration was
  changed to make a rehearsal pass.
- The cutover build is opt-in through two explicit Gradle properties; it is not
  the default release target.
- LockGate, SQLCipher, AppContainer, ChangePlan authority and verification rules
  were not relaxed.
- Preview synthetic fixtures are not installed into `com.pdig.app`; the
  Production rehearsal uses the real app package and real encrypted DB.
- The local device's Production DB contains an older local source label
  (“Legacy WeChat Statement Source”); that is real local test data, not a
  synthetic Preview fixture. The rehearsal never imports or fabricates data.

## 7. Approved cutover artifact (2026-10-18)

After the user approved cutover, a **release** APK was built with the two keys and
a non-production signing identity:

| Item | Value |
| --- | --- |
| Code tree | `7aaa612c6a6dfdd0ef431a8a67879f1caef63f96` |
| Artifact `GIT_SHA` | `01d0de3a` |
| Version | `0.3.1` (2) |
| BuildConfig | `release`, `DEBUG=false`, generation `vnext`, approval `true` |
| APK SHA-256 | `cd8afab1e5981ac844c55a648c75c921016d84fcd5a221efc997a91e0be99d9c` |
| Installed base.apk | same hash as the candidate |
| Default launch, no extras | LockGate → Production VNext |
| Legacy control APK | `287afd58…0316`, opens the Legacy shell |
| Signer | `CN=PDIG NON-PRODUCTION TEST KEY` |

Evidence: `artifacts/runtime-evidence/2026-10-17-r61-plus/production-cutover/`
(now/me/change screenshots, FLAG_SECURE Records proof, both lock/home rollback
captures, DEX `BuildConfig` dumps, APK identity, build logs, manifest).

Repository Gradle defaults deliberately remain `legacy` / `false`; the approved
artifact carries the decision. Store release remains **HOLD** because no production
signing identity is configured.

## 8. Gate statement

```text
PRODUCTION_DEFAULT_REPOSITORY      = LEGACY (intentional, re-proven)
APPROVED_RELEASE_ARTIFACT          = PRODUCTION_VNEXT
PRODUCTION_REHEARSAL_LOCAL         = PASS (secure + release-default)
PRODUCTION_CONTRACTS_LOCAL         = PASS 21/21 at CI viewport
PRODUCTION_DEFAULT_BUILD           = PASS
PRODUCTION_CUTOVER_BUILD           = PASS (two-key release APK)
PRODUCTION_APK_IDENTITY            = MATCH (local == installed)
PRODUCTION_CUTOVER                 = APPROVED / CANDIDATE_VERIFIED
HUMAN_VISUAL_ACCEPTANCE            = ACCEPT (2026-10-18)
ANDROID_REFERENCE_FREEZE           = APPROVED (2026-10-18)
STORE_RELEASE                      = HOLD (production signing absent)
```

Production Rehearsal at the final source `7aaa612c` is green
([39129063954](https://github.com/huangdi97/PDIG-DepMap/actions/runs/39129063954));
the same-code docs head `874e13f0` has CI and iOS green
([39129093200](https://github.com/huangdi97/PDIG-DepMap/actions/runs/39129093200),
[39129093169](https://github.com/huangdi97/PDIG-DepMap/actions/runs/39129093169)).
The core jobs at `7aaa612c` were cancelled by branch concurrency, not failed.
The repository default remains Legacy; the approved artifact is VNext.
