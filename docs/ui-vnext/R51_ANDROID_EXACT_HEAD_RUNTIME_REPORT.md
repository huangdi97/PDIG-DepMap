# R51 — Android exact-head runtime, evidence and build report

> Local round: R48–R51 (2026-10-11/12)
> Local HEAD at capture time: `bc39a2f2499f7d95e2249719d7daf5b384ab5faa`
> Pushed HEAD after the CI hardening commit: `86c482e4ef0f5dab800924ef8b0b6a60d62aa828`
> Evidence root: `artifacts/runtime-evidence/2026-10-11-r48-r51/`
> Branch: `feat/android-ui-vnext-translation` → PR #1 (kept Draft, never merged)

## 1. Local toolchain actually used

| Item                     | Value                                                                                                                            |
| ------------------------ | -------------------------------------------------------------------------------------------------------------------------------- |
| Worktree                 | `E:\AI\号卡管理`                                                                                                                 |
| `ANDROID_HOME`           | `D:\Code\Android\SDK`                                                                                                            |
| `ANDROID_AVD_HOME`       | `D:\avdhome`                                                                                                                     |
| JDK                      | Temurin `21.0.12.1+1` LTS                                                                                                        |
| Gradle                   | **8.9** via the repository wrapper (installed into the wrapper cache; see §6.1)                                                  |
| Emulator                 | API 36, `google_apis`, `x86_64`, `-gpu swiftshader_indirect`, `-no-window -no-snapshot`                                          |
| Phone configuration      | `wm size 1080x2340` + `density 440` — the exact overrides the phone CI runner applies                                            |
| Tablet configuration     | `wm size 1920x1200` + `density 240` + `user_rotation 1` → 800×1280dp (MEDIUM) — the exact overrides the tablet CI runner applies |
| Production configuration | `wm size 1080x2400` + `density 420` → 411×914dp — the CI `pixel_7` profile geometry                                              |

No SDK component, system image or existing AVD was installed, removed, recreated or wiped by this
round. See §6.2 for the one new, isolated AVD that was created (the originals were preserved).

## 2. Local results at the pushed commit

| Gate                              | Command                                                                                      | Result                                                                                 |
| --------------------------------- | -------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------- |
| Core module                       | `android/:core:test`                                                                         | **PASS**                                                                               |
| Android unit tests                | `:app:testDebugUnitTest`                                                                     | **PASS**                                                                               |
| Build                             | `:app:assemblePreviewDebug`, `:app:assembleProductionDebug`                                  | **PASS** (APKs produced)                                                               |
| Build (test APKs)                 | `:app:assemblePreviewDebugAndroidTest`, `:app:assembleProductionDebugAndroidTest`            | **PASS**                                                                               |
| Phone runtime proof               | `python android/scripts/ui_vnext_pixel_capture.py` on the API36 phone configuration          | **PASS** — 65 real PNGs + per-screen semantics, `manifest.json` records the source SHA |
| Tablet contracts                  | `:app:connectedPreviewDebugAndroidTest -P…class=TabletAdaptiveContractTest`                  | **PASS** (5 tests, 2 skipped)                                                          |
| Tablet capture                    | `:app:connectedPreviewDebugAndroidTest -P…class=SourceCompleteScreenshotEvidenceTest`        | **PASS** — 35 published PNGs incl. the empty states                                    |
| Production shell                  | `:app:connectedProductionDebugAndroidTest -P…class=ProductionVNextShellContractTest`         | **PASS** (11/11)                                                                       |
| Production relationship authority | `:app:connectedProductionDebugAndroidTest -P…class=ProductionManualRelationshipContractTest` | **PASS** (1/1)                                                                         |
| Secure rehearsal                  | `python android/scripts/production_vnext_secure_rehearsal.py`                                | **PASS** (`PRODUCTION_VNEXT_SECURE_REHEARSAL=PASS`)                                    |
| Release-default rehearsal         | `python android/scripts/production_vnext_release_default_rehearsal.py`                       | **PASS** (`PRODUCTION_VNEXT_RELEASE_DEFAULT_REHEARSAL=PASS`)                           |

Logs for every one of these runs are in `logs/`.

## 3. Remote (GitHub) status at the pushed commit — observed, not assumed

The local fixes were verified again by CI on the same source.

At `bc39a2f2` (the fix + evidence commit):

| Workflow                              | Status                                                                          |
| ------------------------------------- | ------------------------------------------------------------------------------- |
| CI                                    | **success**                                                                     |
| iOS                                   | **success**                                                                     |
| Android UI vNext Preview APK          | **success**                                                                     |
| Android UI vNext Tablet Pixel Proof   | **success** (was failing before this round)                                     |
| Android UI vNext Phone Pixel Proof    | in progress at the time of the last check; superseded by the newer commit below |
| Android UI vNext Production Rehearsal | **failure — see §4**                                                            |

Inside the production job at `bc39a2f2`, every step up to and including the secure rehearsal passed:
`ProductionVNextShellContractTest` 11/11, `ProductionManualRelationshipContractTest` 1/1,
`PRODUCTION_VNEXT_SECURE_REHEARSAL=PASS`.

## 4. Remaining open item: the production job's last sub-step

`production_vnext_release_default_rehearsal.py` failed in CI at `bc39a2f2` with

```
RuntimeError: Timed out waiting for '你的数字基础设施 · 已记录的全球上下文'
last labels=[…, '我的基础设施', '共 0 个对象', '数据与设置', '数据来源与导入', 'PDIG']
```

The labels are the **legacy** production shell, so on CI the default launcher path did not pick up
the freshly built `-PpdigProductionUiGeneration=vnext -PpdigProductionVNextCutoverApproved=true`
build, even though the CI log shows `generateProductionDebugBuildConfig` and
`packageProductionDebug` re-ran and `adb install` printed `Success` at 13:35:21.

The **identical commit passes this rehearsal locally**, including from a fresh install with an empty
Reality database (verified: uninstall → install the vnext APK → `PRODUCTION_VNEXT_RELEASE_DEFAULT_REHEARSAL=PASS`).
CI-only behaviour therefore could not be pinned to a source defect from the available logs, and this
round did **not** claim it fixed.

What was done instead (commit `86c482e4`): the CI rehearsal step now uninstalls the app, installs the
freshly built APK, and compares the on-device `base.apk` SHA-256 against the local APK SHA-256,
aborting with `INSTALLED APK DOES NOT MATCH THE BUILT APK` rather than misinterpreting the UI. The
hash read-back was validated locally (both sides `adf5ca43…`). This converts an ambiguous UI timeout
into a precise build-identity failure on the next CI run.

The commit `86c482e4` hardening added an APK-identity check, and the next CI run (`86c482e4`,
run `38262734593`) produced the **strongest available evidence**:

```
local_apk_sha256=688adf7420618c753705656fee46e4e8678479970650b96fb569409d36343e67
device_apk_sha256=688adf7420618c753705656fee46e4e8678479970650b96fb569409d36343e67
PRODUCTION_VNEXT_SECURE_REHEARSAL=PASS
RuntimeError: Timed out waiting for '你的数字基础设施 · 已记录的全球上下文'
last labels=['需要你处理', …, '我的基础设施', '共 0 个对象', '数据与设置', '数据来源与导入', 'PDIG']
```

So the **verified-correct** cutover APK is genuinely installed on the CI emulator, the shell
contracts pass 11/11, the relationship authority passes 1/1, the secure rehearsal passes, and the
default (no-extras) launcher path _still_ renders the legacy shell there.

The **identical commit** passes this rehearsal locally, including from a fresh
`adb uninstall` → `adb install` with an empty Reality database. The local device was checked for the
obvious differentiator and is _equivalent_ to CI: it also has no lock credential and no biometrics
(`locksettings get-disabled` → `true`; the app itself reports
`这台设备没有录入指纹或人脸，也没有设置锁屏密码/图案`).

Leading hypotheses for the next session (none confirmed):

1. `MainActivity`'s default-route shell selection may consult something outside
   `BuildConfig.PRODUCTION_UI_GENERATION` / `PRODUCTION_VNEXT_CUTOVER_APPROVED` (a persisted flag, a
   device/emulator-specific branch, or a build-type side condition) that differs on a freshly created
   emulator.
2. The VNext Production Now title may itself depend on Reality content in a way that does not
   reproduce locally; the captured labels were the _legacy_ shell, so this is less likely but not
   excluded.

To make this progress cheaply next time, the CI step now prints
`local_apk_sha256` / `device_apk_sha256` / `device_apk_path` on every run and refuses to interpret
the cutover UI if they differ — validated locally (both sides `adf5ca43…`).

**Status: `PRODUCTION_REHEARSAL` is locally PASS and CI-red at exactly this one sub-step.** It is
reported as unmet, not as a pass.

## 5. Evidence inventory

```
artifacts/runtime-evidence/2026-10-11-r48-r51/
  BASELINE.md                     verified git/PR/CI facts, AVD parameters, the three failing runs
  EVIDENCE_SHA256SUMS.txt         264 hashed artifacts
  phone/                          65 real PNGs + per-screen semantics JSON + manifest.json
  tablet/                         device-profile.txt + 35 published PNGs (incl. empty states)
  production/                     lock-gated rehearsal dumps + release-default dumps
  logs/                           build, instrumentation, pixel-capture and rehearsal logs
```

Every capture records the source SHA (`5a0918546461273efcf99684cf0c1302bc0b89b8` for the runtime
base; the asset UI and the evidence harness were unchanged by the later commit except for the
tablet Overview layout, whose runtime proof is the tablet set above). Device parameters and capture
timestamps are recorded per capture. No Preview render, Figma export or generated image is used as
runtime evidence, and the reference board is stored separately from the actual captures.

## 6. Environment notes worth keeping

### 6.1 The Gradle distribution had to be repaired, not downloaded

The wrapper cache held a broken 0-byte `gradle-8.9-bin.zip.part`, and the JVM could not reach
`services.gradle.org` (`ConnectException: Connection refused`) while PowerShell could. The official
`gradle-8.9-bin.zip` was fetched over HTTPS and unpacked into the wrapper cache, so the wrapper now
runs the **same Gradle 8.9 as CI** without touching `gradle-wrapper.properties`.

### 6.2 One isolated AVD was created, and why

`pli_pixel_api36` (pre-existing) carried a leftover third-party app from a different project on this
machine (`com.zhishen.prototype`, an Expo/RN build) that a cold boot restores to the foreground and
which stole focus mid-capture. A clean AVD `pdig_evi_phone_api36` was created from the existing API36
`google_apis;x86_64` image with the `pixel_5` profile. Every pre-existing AVD (`main`,
`pdig_tablet_api36`, `pli_pixel_api36`, `zhishen_rc`) was preserved untouched.

### 6.3 Harness/tooling lessons encoded in the runners

Long-running emulator and Gradle work must be launched **detached** (`DETACHED_PROCESS` + stdin from
`NUL`); otherwise the emulator attaches to the calling console, keeps stdio open, and gets killed by
the caller's timeout, which looks exactly like a random emulator crash. The adb daemon must be
started before any output redirection. `adb wait-for-device` must never be used (it blocks forever on
an `offline` device). `ADB install` after `connectedAndroidTest` is required because AGP uninstalls
both APKs after a connected run.

## 7. Status vocabulary for this round

| Label                    | Value                                                                                      |
| ------------------------ | ------------------------------------------------------------------------------------------ |
| SOURCE_IMPLEMENTED       | yes — every defect listed in the R48/R49 reports is fixed in source                        |
| EXACT_HEAD_TESTED        | yes — all local gates above ran on `bc39a2f2`                                              |
| RUNTIME_VERIFIED         | yes for phone, tablet and the production lock/authority/secure-rehearsal path              |
| VISUAL_CANDIDATE         | yes — the evidence set is ready for human review (§ `R48_R51_VISUAL_ACCEPTANCE_MATRIX.md`) |
| HUMAN_ACCEPTED           | **PENDING** — requires the user's visual review                                            |
| ANDROID_REFERENCE_FREEZE | **HOLD**                                                                                   |
| PRODUCTION_CUTOVER       | **HOLD** — never attempted; the default production route is unchanged and still legacy     |
