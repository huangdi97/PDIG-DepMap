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
