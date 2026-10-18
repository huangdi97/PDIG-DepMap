# R52+ local → GitHub handoff

> Written 2026-10-18 (Asia/Shanghai)  
> Repository: `huangdi97/PDIG-DepMap`  
> Worktree: `E:\AI\号卡管理`  
> Branch: `feat/android-ui-vnext-translation`  
> PR #1: https://github.com/huangdi97/PDIG-DepMap/pull/1 (Draft, not merged)  
> **Final local/source HEAD: `edc8b2f2089c04ec95cbae0046bd38ff264ad050`**  
> **Capture source HEAD:** `302a65d6` (phone 1.0), `b85c7a57` (1.3×),
> `32541cc0` (landscape/tablet 1.3×), `edc8b2f2` (1.5×), `6b823fb6`
> (tablet/Production), `7993f29d` (Expanded), `c035ce9a` (advanced Production).  
> Local round-start HEAD: `508ee709108598e56f76670ac472a93b0ccf80ee`  
> Remote round-start HEAD: `562d83f44733def5c805c64e4ed53f8746ddbce4`

## 1. What the next agent should do first

```powershell
cd E:\AI\号卡管理
git fetch origin --prune
git status --porcelain=v1
git log --oneline -12
$sha = git rev-parse HEAD
$sha
```

Then read, in this order:

1. `docs/ui-vnext/R52_PLUS_FINAL_IMPLEMENTATION_REPORT.md`
2. `docs/ui-vnext/R52_PLUS_VISUAL_ACCEPTANCE_MATRIX.md`
3. `docs/ui-vnext/R52_PLUS_PRODUCTION_RUNTIME_REPORT.md`
4. `artifacts/runtime-evidence/2026-10-17-r61-plus/BASELINE.md`
5. `artifacts/runtime-evidence/2026-10-17-r61-plus/MANIFEST.json`

Do **not** reuse older screenshot directories as current evidence, and do not
force push, merge `main`, or enable the Production cutover.

## 2. Exact commits added in this round

| Commit | Purpose |
| --- | --- |
| `e1572f61` | Capture presented tablet GPU earth in screenshots (evidence integrity) |
| `ab2d21f5` | Remove duplicate number identity / raw status enums / misleading entry count |
| `fa48f634` | Consumer copy for continuity findings + JVM copy guards |
| `58a1b3f7` | Align phone pixel script with `8 个入口` |
| `74fbca89` | Remove Production data-model jargon from consumer copy |
| `067388f9` | Harden phone capture (serial targeting, UIAutomator retry, detail top/bottom frames) |
| `fc29138e` | Finish Production region copy localization (`RegionFact` / `Region Lens`) |
| `6b823fb6` | Round 2: visible labels on all five MEDIUM rail entries; Me says 需关注 instead of 待处理 (counts unchanged); tablet contract asserts the labels |
| `a1ee036f` | TestTag-only: same rail label tag in the expanded branch, so the tablet contract passes in MEDIUM and EXPANDED/rotation |
| `7993f29d` | Advanced Production consumer copy: ChangePlan / establish / import / maintenance / manual relationship / review; no behaviour change |
| `8fef8030` | androidTest-only 851dp robustness; 38/38 contracts pass at 1080×2340@440, no product assertion removed |
| `c035ce9a` | Real-run copy audit: remove remaining graph/authority/enum jargon; behavior unchanged |
| `302a65d6` | Finish R24/R25 consumer copy and align the phone capture script; phone recaptured at this source |
| `226e8468` | Capture-script-only: R25 step assertion matches the final consumer sentence |
| `b85c7a57` | 1.3× dynamic type: region chip scale, Now 2×2 asset rail, Me card heights |
| `32541cc0` | Short landscape rail scroll: 数据源 / 设置 reachable below 600dp height |
| `edc8b2f2` | 1.5× accessibility: greeting badge, R9 bottom bar, stacked Me identity |

The evidence commits (reports, `WORK_STATUS.md`, `BLOCKERS.md`, this handoff,
phone/tablet/Expanded/production evidence, SHA manifest) follow `7993f29d` and
do not change application source. Check `git log` for their exact SHA.

## 3. Evidence layout

```
artifacts/runtime-evidence/2026-10-17-r61-plus/
├── BASELINE.md
├── MANIFEST.json                     # source SHAs, APKs, counts, per-file hashes
├── EVIDENCE_SHA256SUMS.txt
├── phone/                            # 70 real PNGs + JSON/XML semantics
├── tablet/ui-shots/                  # 34 real PNGs + raw manifest
├── tablet-expanded/                  # 11 real PNGs + manifest (1280x800dp)
├── production/                       # lock/unlock PNG+XML, 2 rehearsal dirs
├── production-advanced/              # 4 real PNGs: Me/hub/manual/review
├── font-scale-1.3/                   # 6 real PNGs at 1.3x dynamic type
├── font-scale-1.5/                   # 4 real PNGs at 1.5x accessibility
├── tablet-font-scale-1.3/            # 4 real PNGs at tablet MEDIUM 1.3x
├── phone-landscape/                  # 4 real PNGs at 851x393dp
└── logs/                             # capture, JVM/build, Production logs
```

Evidence rules used:

- Phone: `adb exec-out screencap` on the installed Preview APK.
- Tablet: shell `screencap` inside the instrumentation test after the display
  presented a real frame; the test fails if the GPU earth is absent.
- Expanded: `adb exec-out screencap` at 2560×1600 @320 (1280×800 dp), built from
  `7993f29d`; `TabletAdaptiveContractTest` is 5/5 at this width.
- Production advanced: real installed `com.pdig.app`, `vnext_production` debug
  extra, source `c035ce9a`; 4 screens with XML and manifest.
- Production: real installed `com.pdig.app` APK, real lock screen, real
  Production shell, real XML semantics; no Preview fixture is presented as
  Production Reality.
- No image in this evidence set is an AI mockup, desktop render or Compose
  preview.

## 4. What is still open (all human/external)

| Item | State | Why |
| --- | --- | --- |
| Human visual acceptance | `PENDING` | Only you can sign per-screen ACCEPT / ACCEPT_WITH_NITS / REJECT. |
| Android Reference Freeze | `HOLD` | Needs human acceptance and a frozen screenshot/SHA manifest. |
| Production cutover | `HOLD` | Default remains Legacy; no explicit approval was given. |
| Original 1536×1024 reference board | Missing | The in-repo 320×213 JPEG is truncated (`broken data stream`); pixel-level diff is impossible. |
| Master document `PDIG_v2.3-R1_..._2026-10-07.md` | Missing | Still absent from both local and remote trees. |
| Android Reference Freeze manifest | Not created | Must follow human acceptance. |
| P2 copy/test items | Resolved | 851 dp contracts now 38/38; no remaining known consumer-facing jargon in the reviewed Production flows. |

## 5. Suggested human review order

1. `phone/01-now.png` — globe, light-first, task hierarchy.
2. `phone/02-infrastructure.png` — eight entries, region distribution.
3. `phone/03-cards.png`, `phone/04-card-detail.png`.
4. `phone/04h-card-change-current.png`,
   `phone/04i-card-change-transition-blocked.png`,
   `phone/04k-card-change-after.png` — Current / Transition / After.
5. `phone/06-records.png` — evidence, not a second attention feed.
6. `phone/07-accounts-detail.png`, `phone/07-devices-detail-top.png`,
   `phone/08b-number-detail.png`, `phone/08-numbers.png`.
7. `phone/07-weaknesses.png` — why / basis / unknown / next.
8. `phone/09-me.png`, `phone/09a-me-mask-enabled.png`,
   `phone/09b-me-mask-disabled.png`.
9. `phone/11-search-empty.png`, `phone/11-search-query.png`.
10. `tablet/ui-shots/01-now.png`, `02-overview-global.png`,
    `05-cards.png`, `09-numbers.png`, `16-weaknesses.png`, `25-me.png`.
11. `tablet-expanded/01-now.png`, `03-cards.png`, `03a-card-selected.png`,
    `06-change-phone-current.png`, `07-change-phone-transition.png`,
    `08-change-phone-after.png`, `10-me.png` — Expanded breakpoint.
12. `production/01-cutover-locked.png`, `02-cutover-unlocked.png`,
    `03-default-legacy-locked.png`, `04-default-legacy-home.png`.
13. `production-advanced/11-production-me.png`, `12-production-establish-hub.png`,
    `13-production-manual-relation.png`, `15-production-review-empty.png` —
    final consumer copy on advanced Production surfaces.
14. `font-scale-1.3/01-now.png`, `06-me.png` — 1.3× dynamic type; chip, asset rail
    and identity hints no longer clip.
15. `phone-landscape/01-now.png`, `02-rail-scrolled.png`,
    `03-infrastructure.png`, `04-change.png` — short-height landscape rail scroll.
16. `tablet-font-scale-1.3/01-now.png`, `02-me.png`, `03-change.png`,
    `04-change-phone.png` — tablet MEDIUM at 1.3×; verified readable.
17. `font-scale-1.5/01-now.png`, `02-me.png` — 1.5× accessibility; badge, bottom
    nav and Me identity no longer clip.

For each, decide **ACCEPT / ACCEPT_WITH_NITS / REJECT**. A REJECT should name
the smallest concrete change. Green CI, source approval or a screenshot existing
is not acceptance.

## 6. Commands to reproduce the local gates

```powershell
# JVM tests + preview APK
cd E:\AI\号卡管理\android
.\gradlew.bat :app:testPreviewDebugUnitTest :app:testProductionDebugUnitTest `
  :app:assemblePreviewDebug -PpdigPreviewBuildSha=7993f29

# Tablet 34-screen evidence (one device)
.\gradlew.bat :app:connectedPreviewDebugAndroidTest -PpdigPreviewBuildSha=7993f29 `
  "-Pandroid.testInstrumentationRunnerArguments.class=com.pdig.uivnext.evidence.SourceCompleteScreenshotEvidenceTest"

# Phone capture (from repo root, one device)
$env:GITHUB_SHA='6b823fb6'
$env:PDIG_ADB_SERIAL='emulator-5554'
python android\scripts\ui_vnext_pixel_capture.py

# Production contracts (CI-equivalent 1080x2400@420)
.\gradlew.bat :app:connectedProductionDebugAndroidTest `
  "-Pandroid.testInstrumentationRunnerArguments.class=com.pdig.uivnext.evidence.ProductionVNextShellContractTest,com.pdig.uivnext.evidence.ProductionRecordNavigationContractTest,com.pdig.uivnext.evidence.ProductionManualRelationshipContractTest"

# Production rehearsals
python android\scripts\production_vnext_secure_rehearsal.py
python android\scripts\production_vnext_release_default_rehearsal.py
```

The Android build root is `C:\Users\Kaiser\pdig-build` because the project path is
non-ASCII; do not “fix” that by moving the repository without re-running the
suite.

## 7. GitHub / CI status

At the **round-start** remote HEAD `562d83f4`, all six workflows / nine checks
were green, including Production Rehearsal run
[39023996578](https://github.com/huangdi97/PDIG-DepMap/actions/runs/39023996578).

Final source `edc8b2f2` is pushed and **all six workflows / nine checks are
observed success at that SHA**:

| Gate | Run | Result |
| --- | --- | --- |
| CI | [39120366874](https://github.com/huangdi97/PDIG-DepMap/actions/runs/39120366874) | SUCCESS |
| iOS | [39120366834](https://github.com/huangdi97/PDIG-DepMap/actions/runs/39120366834) | SUCCESS |
| Android UI vNext Preview APK | [39120361728](https://github.com/huangdi97/PDIG-DepMap/actions/runs/39120361728) | SUCCESS |
| Android UI vNext Phone Pixel Proof | [39120361668](https://github.com/huangdi97/PDIG-DepMap/actions/runs/39120361668) | SUCCESS |
| Android UI vNext Tablet Pixel Proof | [39120361692](https://github.com/huangdi97/PDIG-DepMap/actions/runs/39120361692) | SUCCESS |
| Android UI vNext Production Rehearsal | [39120361804](https://github.com/huangdi97/PDIG-DepMap/actions/runs/39120361804) | SUCCESS |

Historical: `32541cc0` and `b85c7a57` were 9/9 green; `226e8468` (script-only) had
CI/iOS/Preview/Phone green; `302a65d6` had Tablet/Production green and a phone
failure caused only by the old committed script assertion, fixed in `226e8468`.

A later **docs/evidence-only** commit may not re-trigger path-filtered workflows.
That does not invalidate the runtime result at `edc8b2f2`: check the actual PR-head
check-runs rather than assuming all six re-ran.

## 8. Guardrails respected

- No force push, no history rewrite, no merge of `main`, no branch deletion.
- No Canonical schema, `.depmap`, crypto parameter, LockGate or Production
  default change.
- No test deleted, skipped or weakened; copy assertions were updated to the new
  consumer wording, and the evidence script gained retries rather than losing
  assertions.
- The pre-existing stash `user-doc-changes-preserved-20261004` and the untracked
  `artifacts/runtime-evidence/2026-09-29-ui-vnext/IMAGE_METRICS.json` were left
  untouched.
- No real bill, key, credential or store signing material entered the worktree
  or the evidence.
