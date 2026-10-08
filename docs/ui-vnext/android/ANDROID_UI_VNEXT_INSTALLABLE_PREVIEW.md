# Android UI vNext — Installable Preview Distribution

This file records the distribution boundary for the Human-accepted Android Light reference.

## What this publishes

Workflow:

`.github/workflows/android-ui-vnext-preview.yml`

Branch:

`feat/android-ui-vnext-translation`

The workflow builds two **debug-signed** installable APKs from the exact branch HEAD:

- `PDIG-Android-UI-vNext-Preview.apk`
  - application id: `com.pdig.app.preview`
  - recommended for direct phone evaluation because it can coexist with production PDIG.
- `PDIG-Android-UI-vNext-ProductionDebug.apk`
  - application id: `com.pdig.app`
  - matches the production-debug flavor used by Android UI runtime evidence.

Both APKs are uploaded to the Actions run and to a commit-specific GitHub **Pre-release** together with SHA-256
checksums and build provenance.

## What this does not claim

This preview is not:

- Play Store production-signed;
- a store submission;
- an upgrade-compatible replacement for every historical debug APK;
- evidence that Android Reference Freeze moved from HOLD to PASS.

Android Human Pixel Review already records:

`ANDROID_VISUAL_REFERENCE = ACCEPTED`

The remaining Reference Freeze HOLD is a separate targeted contract-revalidation matter and does not prevent a
user from installing the Preview APK to evaluate the accepted phone UI on real hardware.

## Safety / data note

This is an evaluation build. Before testing import/restore/delete flows on a personal phone, keep a separate
backup of any important PDIG data. UI evaluation should preferably begin with synthetic/non-critical data.

## Preview launcher correctness (2026-10-08)

**Critical behavioral distinction:** the original production `MainActivity` only entered `VNextApp` with the
instrumentation extra `vnext_demo=true`; a directly installed preview would otherwise show the old
`PdigApp` despite packaging all the new UI classes.

The `preview` flavor now enters `VNextApp` from the normal launcher automatically using
`shouldLaunchVNext(BuildConfig.FLAVOR, explicitDemo)`. The production flavor without an explicit test
extra still opens the original lock-gated `PdigApp`. This is not a new production-data integration.

The separate `com.pdig.app.preview` package uses **synthetic reference fixtures** for visual and touch
evaluation. **It does not display or edit the user's actual PDIG PersonalReality**. Use the production app
for personal records; do not enter valuable secrets into preview fixtures.

Portable CI runs both `:app:testProductionDebugUnitTest` and `:app:testPreviewDebugUnitTest`, including
the `VNextLaunchPolicyTest` regression. The release carries `BUILD_PROVENANCE.json` with exact source
HEAD, tree, APK byte count, SHA-256, version and build variant. A passing hosted build is *not* evidence
of a real-device launcher or the Android tablet-only freeze contract. The targeted
`TabletAdaptiveContractTest.tabletNumberDetail_noDeadSpace` still needs a device-backed run before
`ANDROID_REFERENCE_FREEZE = PASS` may be asserted.

## Dedicated flavor launcher / consumer-download ambiguity resolution (2026-10-08)

A real phone screenshot showed legacy `PdigApp` (vertical "需要你处理" blocks,
"Legacy WeChat Statement Source", no Globe/no four-way navigation) instead of the accepted UI vNext.
This visual evidence invalidates the earlier **consumer-entry acceptance**; hosted Gradle PASS did not
check which installed application the user opened.

The separate `preview` flavor now has a **manifest-level dedicated launcher**:
`com.pdig.app.PreviewLauncherActivity`, under `src/preview`. Its Compose content can only
open `VNextApp` and synthetic reference data. The preview manifest replaces and unexports the
legacy `MainActivity` declaration so the old launcher is not a second home-screen destination.
The `production` flavor and production lock-gated entry remain unchanged.

The workflow verifies the **built APK**, not just Gradle source assertions: SDK `aapt dump badging`
must report the preview application ID, `PDIG Preview` app label and dedicated preview launcher;
the old launcher must not be advertised. Missing SDK tooling or a mismatch fails the release gate.
It uploads `APK_BADGING.txt` as direct artifact evidence.

Only one installable APK is published on each new Preview Release, with short SHA in the filename.
ProductionDebug is compiled as a regression check but **is not published to consumer Releases**
(to eliminate ambiguous choice between old and new UIs). The released Preview is synthetic-only.
The screenshot alone cannot identify which installed package the user had opened; do not claim it
proved a defect in the previously uploaded preview binary without verifying package/signature/hash.

Gate meanings: `APK_LAUNCHER_IDENTITY` verifies **merged manifest/packaging**, not real-device
UI pixels. A real device install → home-screen launch → screenshot remains required before
`ANDROID_REAL_DEVICE_PREVIEW_ACCEPTED=PASS`. Also keep `ANDROID_REFERENCE_FREEZE=HOLD`
until the separate targeted Tablet instrumentation test is rerun.
 
## First real-device design-feedback corrective pass (2026-10-08)

The user's phone screenshots from the `193dca0` preview show valid four-tab navigation but do not
match the human-selected Light-first reference: Earth reads too dark/small; Now lacks visible
region identities; infrastructure tiles dominate; Change/Records still feel like generic forms.
The original 58-image emulator acceptance must therefore NOT be used as real-device visual parity.

The v2.3-R1 §11.10 requires onboarding in <=3 screens. The Preview now implements:
1. 看清你的数字生活 — dependency;
2. 改变之前，先看影响 — safe change;
3. 你的数据，由你掌握 — local-first and unknown truth.

First launch shows the guide, Skip/Finish persist completion to a **preview-only** preference,
and the Help icon can reopen it. The synthetic fixture disclosure explicitly says the
Preview does not read or change real personal data. No personal information is required.
The production MainActivity remains unchanged, and .depmap/Canonical/PersonalReality is
unaffected. A production-data onboarding and actual import setup remain separate future work.

This pass also increases ambient texture lighting without removing day/night maps, draws two
actual fixture-region chips on compact Now, and reduces its overlay/hub density.
These changes affect Android production UI pixels: old screenshot evidence is superseded and
fresh Phone/Tablet runtime capture + human review are REQUIRED. No claims of achieved
pixel parity, completed real-device onboarding tests, or Reference Freeze PASS are made.

Acceptance gates:
- Preview fresh install: exactly three guide screens; Next/Back/Skip/Finish;
- second launch skips guide; Help reopens; text declares demo-only;
- APK release launcher stays `com.pdig.app.PreviewLauncherActivity`;
- region chips count from actual fixture, no fake unknown -> safe;
- Android app JVM tests and both preview/production debug builds;
- actual phone and tablet pixel review before any visual acceptance upgrade.

## Real-phone parity remediation R7 (2026-10-08) — NOT ACCEPTED YET

**User evidence:** replacing the prior preview APK did not produce an experience visibly close to
`PDIG_ANDROID_LIGHT_VISUAL_REFERENCE_2026-10-05.jpg`. Code-path presence, CI PASS and release
upload must NEVER be equated with perceived visual fidelity. Treat the selected 1536×1024
nine-panel board and the user's real-phone screenshots as the two comparison inputs.

Current discrepancy / required reference translation:

| Scope | Real issue | R7 source response | Acceptance evidence |
|---|---|---|---|
| Now first fold | giant dashboard-like globe card and long attention list | Globe-first stage, region context chips, lower fold density, one attention priority | phone screenshot at launcher, region chip tap |
| Infrastructure | giant admin tiles and a second big globe | compact 4x2 icon grid, searchable content, smaller region-distribution panel | phone screenshot, navigate cards |
| Cards list | issuer labels lack financial face detail | consistent network/EMV/contactless materials, real issuer identity | phone card-list screenshot |
| Card detail | detail data presented as stacked property panels | asset face first, four live tabs with honest bill unknown and risk state | phone detail and tab interaction |
| Change Phone | spatial visual buried below banner/form; duplicate identity panels | visible progress and service orbit before verbose details | phone change screenshot and projection switching |
| Records | timeline information exists but reference-quality craft not yet measured | pending phone pixel review | 2026 Phone runtime pixels |
| Tablet | frozen reference not to be regressed | no R7 compact-only layout port | tablet evidence required before Freeze |

The Preview app chrome now exposes `BuildConfig.GIT_SHA` beside the app identity. A
distinct runtime screenshot workflow installs the **exact** APK into an API35 Android
emulator, captures launcher/onboarding/Now/Infrastructure/Cards/CardDetail/Change/Records
and exports PNG/UI-tree manifests to an Actions artifact. It asserts that **the screen's**
visible SHA matches the workflow source, making a stale app immediately diagnosable.
The runtime workflow runs separately from unit/conformance/build: **one passing workflow
does not supersede user real-device feedback**.

Closure requires exact SHA match, fresh phone evidence, visual human comparison with
reference, no clipped nav/content at supported densities, followed by tablet re-check.
Unknown remains Unknown; preview only uses synthetic fixtures, After remains plan projection.
A visual claim cannot be PASS based on changed source alone.
