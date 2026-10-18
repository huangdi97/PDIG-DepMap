# PDIG / DepMap — R52+ round baseline (2026-10-17)

> This baseline was produced before any source change in this round.
> It supersedes earlier chat claims and earlier PASS statements; GitHub and the
> local worktree are the only engineering facts used here.

## 1. Git

| Item | Value |
| --- | --- |
| Worktree | `E:\AI\号卡管理` |
| Remote | `https://github.com/huangdi97/PDIG-DepMap.git` |
| Branch | `feat/android-ui-vnext-translation` |
| Local HEAD at round start | `508ee709108598e56f76670ac472a93b0ccf80ee` |
| Remote branch HEAD at round start | `562d83f44733def5c805c64e4ed53f8746ddbce4` |
| Relationship | local was a strict ancestor: 0 ahead / 99 behind |
| Synchronisation | `git fetch origin --prune` + `git merge --ff-only` → `562d83f4` |
| Working tree at start | clean except one pre-existing untracked `artifacts/runtime-evidence/2026-09-29-ui-vnext/IMAGE_METRICS.json` |
| Stash | `stash@{0}` `user-doc-changes-preserved-20261004` preserved untouched |
| Other agents | no other worktree, Gradle/ADB/emulator process or lock was active |

No force push, no reset, no clean, no merge of `main`, no branch deletion was
performed. The untracked metrics file and the stash remain intact.

## 2. GitHub exact-head status (before this round's edits)

All six workflows / nine checks were **success** at `562d83f4`:

| Workflow | Run | Result |
| --- | --- | --- |
| CI (Core / App / Canonical / Harmony static) | [39024002448](https://github.com/huangdi97/PDIG-DepMap/actions/runs/39024002448) | SUCCESS |
| iOS | [39024002432](https://github.com/huangdi97/PDIG-DepMap/actions/runs/39024002432) | SUCCESS |
| Android UI vNext Preview APK | [39023996421](https://github.com/huangdi97/PDIG-DepMap/actions/runs/39023996421) | SUCCESS |
| Android UI vNext Phone Pixel Proof | [39023996429](https://github.com/huangdi97/PDIG-DepMap/actions/runs/39023996429) | SUCCESS |
| Android UI vNext Tablet Pixel Proof | [39023996487](https://github.com/huangdi97/PDIG-DepMap/actions/runs/39023996487) | SUCCESS |
| Android UI vNext Production Rehearsal | [39023996578](https://github.com/huangdi97/PDIG-DepMap/actions/runs/39023996578) | SUCCESS |

The exact-head artifacts were downloaded to
`artifacts/runtime-evidence/2026-10-17-r61-exact-head/ci/`:

- phone: 64 real PNGs + 139 files total;
- tablet: 35 real PNGs + 53 files total;
- production rehearsal: 40 XML / report / profile files.

PR #1 is `OPEN`, `Draft`, `MERGEABLE`, base `main`.

## 3. Local Android toolchain

| Item | Value |
| --- | --- |
| JDK | Eclipse Temurin `21.0.12.101-hotspot` |
| Android SDK | `D:\Code\Android\SDK` |
| ADB / emulator | SDK platform-tools / emulator 36.5.11 |
| AVDs present | `pdig_evi_phone_api36` (Pixel 5 profile), `pdig_tablet_api36` (Pixel Tablet profile), plus `main`, `pli_pixel_api36`, `zhishen_rc` |
| System image | `android-36/google_apis/x86_64` |
| Build output root | `C:\Users\Kaiser\pdig-build` (ASCII path required by the non-ASCII project path) |
| Gradle cache | `C:\Users\Kaiser\.gradle` (pre-existing, reused; no SDK/image re-download) |

The tablet AVD was left with stale phone overrides (`1080x2400@420`) from an
earlier session. For evidence it was restored to the CI configuration:
`wm size 1200x1920`, `wm density 240` (portrait 800×1280 dp, MEDIUM), which is
the same logical viewport as CI's `1920x1200 @240dpi` + `user_rotation=1`.

## 4. Reference and input gaps

- The in-repo light reference board
  `spec/ui-vnext/references/android/PDIG_ANDROID_LIGHT_VISUAL_REFERENCE_2026-10-05.jpg`
  is 320×213 and **truncated**: Pillow fails with
  `OSError: broken data stream when reading image file`, and the image harness
  rejects it as malformed. It cannot support a pixel-level diff. The claimed
  1536×1024 original is still not in the repository.
- `PDIG_v2.3-R1_..._统一全量母版_2026-10-07.md` is still absent from both the
  local tree and the remote tree at `562d83f4`.
- No AI-generated mockup, desktop render or Preview fixture is used as Android
  runtime evidence.

## 5. Defects visible in the exact-head captures (before edits)

These are the concrete issues that motivated this round's changes:

1. **Tablet screenshots silently omitted the GPU earth.** The CI tablet artifact
   `tablet/.../ui-shots/01-now.png` shows a blank hero with only region labels and
   link arcs. The cause is the evidence pipeline, not the product:
   `SourceCompleteScreenshotEvidenceTest` used Compose `captureToImage()`, which
   does not include the `GLSurfaceView` layer. A real adb capture of the same
   screen shows the earth. This means the previous tablet visual proof could not
   see the signature globe at all.
2. **Numbers list repeated the same number twice** on phone and tablet when the
   user had not set an alias (title = number, second line = same number).
3. **Account detail printed the raw wire enum** `active` next to “状态”.
4. **Card-change replacement copy printed the raw wire word** `active`.
5. **Continuity findings used engineering vocabulary** in consumer copy
   (`参考 Reality`, `FailureDomain`, `RecoveryCycle confirmed path`, `core
   RecoveryCycle`, `done=true`, `ChangePlan`, `Proposal`).
6. **“8 类资产” was factually wrong** for a grid of eight entries that also
   includes 总览 and 薄弱点; it is now “8 个入口”.
7. **Now said 需要处理 6 while Me said 待处理 3** — different definitions
   (attention + review vs attention only). This remains a P2 copy/definition
   question for human review, not silently “fixed” by changing counts.

## 6. Pre-change gates

```
GITHUB_EXACT_HEAD_CI       = PASS   (562d83f4, all 6 workflows)
LOCAL_BUILD_AT_ROUND_START = NOT_RUN_THIS_ROUND
PHONE_RUNTIME              = NOT_RUN_THIS_ROUND
TABLET_RUNTIME             = NOT_RUN_THIS_ROUND
PRODUCTION_REHEARSAL       = NOT_RUN_THIS_ROUND
HUMAN_VISUAL_ACCEPTANCE    = PENDING
ANDROID_REFERENCE_FREEZE   = HOLD
PRODUCTION_CUTOVER         = HOLD
```

The previous R51 `PRODUCTION_REHEARSAL = CI_RED` blocker is **not** carried
forward blindly: the exact-head Production Rehearsal at `562d83f4` is green.
The local re-run and its evidence are recorded in the R52+ production report.
