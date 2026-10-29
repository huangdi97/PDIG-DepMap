# R48–R51 Baseline — PDIG / DepMap Android

> Round: R48–R51 Local Agent Execution
> Captured: 2026-10-11 (local, Asia/Shanghai)
> Repo: `huangdi97/PDIG-DepMap` — worktree `E:\AI\号卡管理`
> Working branch: `feat/android-ui-vnext-translation` → PR #1

This file records **observed facts only**. Historical SHAs and historical PASS results are treated as
locating hints, never as current truth.

---

## 1. Repository / Git facts (verified 2026-10-11)

| Item | Observed value |
| --- | --- |
| Worktree | `E:\AI\号卡管理` |
| Android module root | `android/` (`core`, `conformance`, `app`, `repos`) |
| `ANDROID_HOME` | `D:\Code\Android\SDK` |
| `ANDROID_AVD_HOME` | `D:\avdhome` |
| `JAVA_HOME` | `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\` |
| JDK | Temurin `21.0.12.1+1` LTS |
| Local HEAD **before** sync | `2d4045c2b334d4b78d33cc595efbb8626c718e9a` (2026-10-07) |
| Remote branch HEAD | `5a0918546461273efcf99684cf0c1302bc0b89b8` (2026-10-10T16:33Z) |
| Relation | `compare 2d4045c...5a09185` → `status=ahead`, `behind_by=0`, `merge_base=2d4045c` ⇒ local was a **strict ancestor**; sync is a pure fast-forward of **1182 commits** |
| Sync action | `git fetch origin --prune` + `git merge --ff-only origin/feat/android-ui-vnext-translation` (exit 0) |
| Local HEAD **after** sync | `5a0918546461273efcf99684cf0c1302bc0b89b8` |
| `git rev-parse origin/feat/android-ui-vnext-translation` | `5a0918546461273efcf99684cf0c1302bc0b89b8` (equal to local HEAD) |
| Tracked-file changes after sync | none (`git status --short` shows only untracked files) |
| Preserved stash | `stash@{0}: On feat/android-ui-vnext-translation: user-doc-changes-preserved-20261004` — **still present after sync** |
| Untracked (pre-existing, preserved) | `PDIG_Android_R48-R51_LocalAgent_GOAL_2026-10-11.md`, `artifacts/runtime-evidence/2026-09-29-ui-vnext/IMAGE_METRICS.json` |
| Prunable worktrees (untouched) | `…/scratch/782e0f23…/gh-pages-wt`, `…/scratch/782e0f23…/pdig-be3bc81` |
| Unchanged branches | `main` = `cbe99d5`, plus all other local/remote branches left as-is |

### PR #1

| Field | Value |
| --- | --- |
| Number | 1 |
| State | `OPEN` |
| Draft | `true` |
| Mergeable | `MERGEABLE` |
| headRefName | `feat/android-ui-vnext-translation` |
| headRefOid | `5a0918546461273efcf99684cf0c1302bc0b89b8` |
| baseRefName | `main` |
| changedFiles | 1956 |
| additions / deletions | +127782 / −1491 |
| URL | https://github.com/huangdi97/PDIG-DepMap/pull/1 |

`gh` CLI 2.96.0 authenticated as `huangdi97`, token scopes include `repo` and `workflow` ⇒ push and PR
description updates are available locally.

---

## 2. Workflows (present at 5a09185)

Locally present at HEAD (these did **not** exist in the stale 2d4045c tree):

```
.github/workflows/android-ui-vnext-phone-pixels.yml
.github/workflows/android-ui-vnext-tablet-pixels.yml
.github/workflows/android-ui-vnext-preview.yml
.github/workflows/android-ui-vnext-production-rehearsal.yml
.github/workflows/ci.yml
.github/workflows/ios-runtime-visual.yml
.github/workflows/ios.yml
```

Repository-level workflow list also includes `android-ui-vnext-preview-metadata-fix.yml` (present on
the repository default branch view; not part of this branch tree).

---

## 3. CI status for exact HEAD `5a0918546461273efcf99684cf0c1302bc0b89b8` (observed, not inherited)

| Workflow | Run ID | Conclusion |
| --- | --- | --- |
| CI | `38068185686` | **success** |
| iOS | `38068186042` | **success** |
| Android UI vNext Preview APK | `38068178828` | **success** |
| Android UI vNext Phone Pixel Proof | `38068178942` | **failure** |
| Android UI vNext Tablet Pixel Proof | `38068178995` | **failure** |
| Android UI vNext Production Rehearsal | `38068179070` | **failure** |

### 3.1 Phone Pixel Proof — run 38068178942, job `phone-runtime-proof`

Failed step: **8. Install, launch and capture actual phone runtime pixels**

Verbatim failure line from job log:

```
RuntimeError: Screen 04i-card-change-transition-blocked did not show '尚未选择替代卡片，迁移阶段保持阻断'; captured wrong page
##[error]The process '/usr/bin/sh' failed with exit code 1
```

### 3.2 Tablet Pixel Proof — run 38068178995, job `tablet-runtime-proof`

Failed step: **6. Run API36 tablet contracts and capture Human Review set**

Verbatim failure lines:

```
com.pdig.uivnext.evidence.SourceCompleteScreenshotEvidenceTest > capturesSourceCompleteHumanSet[emulator-5554 - 16] FAILED
	java.lang.IllegalStateException: capture failed for 02-overview-global
> Task :app:connectedPreviewDebugAndroidTest FAILED
##[error]The process '/usr/bin/sh' failed with exit code 1
```

Adjacent emulator noise (recorded, not conclusively causal):

```
ERROR | Failed to find ColorBuffer: 427
ERROR | Failed to bind to texture: invalid color buffer.
```

### 3.3 Production Rehearsal — run 38068179070, job `production-reality-rehearsal`

Failed step: **7. Prove lock-gated real-Reality VNext rehearsal on API36**

Verbatim failure lines:

```
com.pdig.uivnext.evidence.ProductionManualRelationshipContractTest > productionFormCommitsCurrentRuntimeRelationThroughAuthority[emulator-5554 - 16] FAILED
	java.lang.AssertionError: Failed to inject touch input.
> Task :app:connectedProductionDebugAndroidTest FAILED
##[error]The process '/usr/bin/sh' failed with exit code 1
```

---

## 4. Local emulator inventory (verified)

`adb devices -l` at baseline: **no devices attached** (all offline).

`emulator -list-avds`:

```
main
pdig_tablet_api36
pli_pixel_api36
zhishen_rc
```

| AVD | Device profile | Resolution | Density | API | System image | RAM |
| --- | --- | --- | --- | --- | --- | --- |
| `pli_pixel_api36` | `pixel_5` | 1080×2340 | 440 dpi | 36 | `system-images\android-36\google_apis\x86_64` | 2G |
| `pdig_tablet_api36` | `pixel_tablet` | 2560×1600 | 320 dpi | 36 | `system-images\android-36\google_apis\x86_64` | 2G |

Installed system images: `android-34`, `android-35`, `android-36`. No SDK component was installed,
removed, or reconfigured during this round.

**Reuse policy honoured:** existing SDK, Gradle cache, system images and AVDs are reused as-is; no AVD data
was wiped and no AVD was recreated.

---

## 5. Authoritative specification sources (present at HEAD 5a09185)

Confirmed present and treated as binding:

- `spec/ui-vnext/FIVE_PRIMARY_NAVIGATION_DECISION.md` — **FROZEN 2026-10-09**: primary destinations are
  `现在 / 基础设施 / 变更 / 记录 / 我`; `我` must not be demoted to avatar-only / settings utility / overflow /
  secondary rail entry; all five are roots with `upDestination = null`; five destinations are within
  Material 3 `NavigationBar`'s intended 3–5 range.
- `spec/ui-vnext/IA.md`, `spec/ui-vnext/INTERACTION_CONTRACT.md`, `spec/ui-vnext/RESPONSIVE_CONTRACT.json`
- `spec/ui-vnext/ASSET_CONTINUITY_UX_CONTRACT.md`
- `spec/ui-vnext/PRODUCT_LENSES_AND_IDENTITY_CONTEXT.md`
- `spec/ui-vnext/RECORDS_AND_PAYMENT_CHANGE_CONTRACT.md`
- `spec/ui-vnext/RECOVERY_AND_IDENTITY_LENS_UX_CONTRACT.md`
- `spec/ui-vnext/RECOVERY_PREPAREDNESS_UX_CONTRACT.md`
- `spec/proposals/asset-lifecycle-maintenance-v1.md`
- `docs/ADR_UI_VNEXT_PRODUCTION_BINDING.md`
- `ANDROID_UI_VNEXT_R45_SOURCE_REPORT.md`, `ANDROID_UI_VNEXT_R46_REFERENCE_REALITY_AUDIT.md`
- `ANDROID_UI_VNEXT_CURRENT_REALITY.md`, `ANDROID_REFERENCE_MAPPING.md`, `ANDROID_VISUAL_CONTRACT.md`
- `spec/ui-vnext/references/REFERENCE_MANIFEST.json` + `spec/ui-vnext/references/android/PDIG_ANDROID_LIGHT_VISUAL_REFERENCE_2026-10-05.md`

---

## 6. Recorded input gaps (documented, not falsified, non-blocking)

**GAP-1 — Android light reference board is low fidelity.**
`spec/ui-vnext/references/android/PDIG_ANDROID_LIGHT_VISUAL_REFERENCE_2026-10-05.jpg` is a **320×213 JPEG**
(SHA256 `3782204282dc81342d8b0311061f9d15c720b7bcd064fd66af78479f6168dbb7`). Its own companion note states the
human-selected source render was **1536×1024**, which is not committed. Per-screen `Reference → Actual → Diff`
is therefore limited to composition / hierarchy / palette comparison against the written binding principles
rather than pixel-level diffing. Owner: user. Action: add the 1536×1024 board if pixel-level review is required.

**GAP-2 — Task-book master document absent.**
`PDIG_v2.3-R1_个人数字基础设施图谱与连续性系统_ProductArchitectureVNext_统一全量母版_2026-10-07.md` does **not
exist** locally nor in the remote tree at `5a09185`. The in-repo frozen specs listed in §5 are used as the
authoritative substitutes. Owner: user. Action: commit the master document if it is still authoritative.

**GAP-3 — Human visual acceptance.** Machine checks can verify layout, truncation, semantics tree, screenshot
differences and stability. Human Visual Acceptance must be performed by the user and is reported as `PENDING`
until confirmed.

---

## 7. Worktree protection strategy

1. Sync performed as fast-forward only. No `reset --hard`, no `push --force`, no rebase of published history.
2. Existing stash `user-doc-changes-preserved-20261004` preserved and **not** applied or dropped.
3. Pre-existing untracked files left in place.
4. Other branches, local-only branches and prunable worktrees untouched.
5. No SDK component, system image or AVD was installed, removed, recreated or wiped.
6. Existing evidence directories under `artifacts/runtime-evidence/**` are append-only; earlier rounds are
   retained as `REJECTED`/historical baselines and never overwritten.
7. New evidence for this round is written under `artifacts/runtime-evidence/2026-10-11-r48-r51/`.

---

## 8. Evidence index for this round

```
artifacts/runtime-evidence/2026-10-11-r48-r51/
  BASELINE.md                  <- this file
  logs/                        <- build / test / gradle console logs
  phone/                       <- API36 phone runtime PNGs
  tablet/                      <- API36 tablet runtime PNGs
  expanded/                    <- expanded / landscape captures (if applicable)
  production/                  <- Production lock-gated rehearsal captures
  empty-states/                <- empty / unknown / locked state captures
  manifests/                   <- screenshot manifest + interaction validation JSON
  probes/                      <- UI hierarchy dumps, layout probes
  diff/                        <- reference vs actual difference notes
  EVIDENCE_SHA256SUMS.txt      <- hashes of all evidence artifacts
```

Screenshot source SHA, device parameters, capture timestamps and, where relevant, UI hierarchy dumps are
recorded per capture in `manifests/`.
