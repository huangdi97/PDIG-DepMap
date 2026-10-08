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
