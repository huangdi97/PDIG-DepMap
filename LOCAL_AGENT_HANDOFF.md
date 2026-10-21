# LOCAL_AGENT_HANDOFF — PDIG / DepMap Android R48–R51

> **Update 2026-10-21 — Android post-freeze closure (newest state; supersedes the R48–R51 details below)**
>
> Round goal: close everything a local agent can close on the current remote Android tree.
> Start state: local `5668c3f9` was a strict ancestor of remote `a3049c64` (401 behind, 0 ahead);
> synchronised with `git merge --ff-only` after `git fetch --all --prune` (no force, no reset).
>
> What changed:
> 1. `ProductionChangePlanScreen` now renders a single deterministic primary CTA for the next
>    executable step (`productionNextStep` → REANALYZE / COMPLETE / VERIFY / NONE), shares one
>    authoritative `completeAction` / `verifyAction` path with the step cards, and states 未发生
>    literally on the AFTER stage.
> 2. New wedge coverage: `ProductionChangeKernelContractTest` (prerequisite, verify-after-done,
>    terminal-plan frozen), `ProductionNextStepTest` (8 JVM tests) and two Compose CTA contracts.
> 3. `PostFreezeEvidenceSeedTest` seeds an isolated test Reality into the app's own encrypted DB
>    (androidTest only, never the Production data path) so the Make-Before-Break journey can be
>    walked by tapping the real UI: everything from "record done" to "confirm verification" is a
>    real user action against AppContainer.
> 4. `postfreeze_closure_capture.py` captures PNG + UIAutomator XML + a manifest row per shot,
>    bound to sourceHead, androidTreeHead, installed APK sha256, device profile, font scale and
>    navigation sequence, and fails loudly when an assertion does not hold.
> 5. `production_vnext_secure_rehearsal.py` / `..._release_default_rehearsal.py` now decode adb
>    output as UTF-8 (Windows ANSI code page made every label unreadable).
>
> Verified here: core 108 / repos 13 / app-preview 240 / app-production 240 JVM tests (0 failures,
> 0 skipped); Android conformance 161/161 PASS; four APK variants built with preview identity
> `com.pdig.app.preview.pa3049c6`; `PRODUCTION_VNEXT_SECURE_REHEARSAL=PASS`; 24/24 Production
> shell/records/manual-relationship/identity tests; 31 runtime captures (phone, privacy,
> stale→reanalyzed, tablet expanded + medium).
>
> Still open: 28 §27 phone-preview ids recorded `NOT_RUN`; four window-class assertions in
> `TabletAdaptiveContractTest` / `AndroidAdaptiveShellContractTest` fail on the local tablet AVD;
> `PIXEL_REFERENCE_LIMITATION` blocks any pixel-diff claim.
>
> `HUMAN_VISUAL_ACCEPTANCE = NOT_RUN` · `STORE_RELEASE = NOT_READY`.
> Details: `docs/ui-vnext/ANDROID_POST_FREEZE_CLOSURE_2026-10-21.md`.

> Written: 2026-10-12 (local, Asia/Shanghai)
> Repository: `huangdi97/PDIG-DepMap` · worktree `E:\AI\号卡管理`
> Branch: `feat/android-ui-vnext-translation` → **PR #1 (Draft, not merged)**

## 1. Exact references

| Item                                           | Value                                                                                                                                   |
| ---------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| Local branch HEAD (after this round's commits) | `86c482e4ef0f5dab800924ef8b0b6a60d62aa828`                                                                                              |
| Remote branch HEAD                             | `86c482e4ef0f5dab800924ef8b0b6a60d62aa828` (pushed, fast-forward only — no force push)                                                  |
| Commit before this round                       | `5a0918546461273efcf99684cf0c1302bc0b89b8` (PR #1 head when the round started)                                                          |
| Commits added                                  | `d3c6f459` fix(android) three runtime proof blockers · `bc39a2f2` evidence(r48-r51) · `86c482e4` fix(ci) APK identity + R48–R50 reports |
| PR #1                                          | https://github.com/huangdi97/PDIG-DepMap/pull/1 — `OPEN`, `Draft`, `MERGEABLE`, base `main`                                             |
| Evidence directory                             | `artifacts/runtime-evidence/2026-10-11-r48-r51/` (264 hashed artifacts)                                                                 |
| Runtime source SHA recorded in every capture   | `5a0918546461273efcf99684cf0c1302bc0b89b8`                                                                                              |

The worktree was synchronised at the start of the round with
`git fetch origin --prune` + `git merge --ff-only` (local was a strict ancestor, 1182 commits behind).
The pre-existing stash `user-doc-changes-preserved-20261004` was preserved and never applied or
dropped, and no pre-existing AVD or evidence directory was modified.

## 2. What was actually done

### Fixed (source)

1. `R21ChangeCardScreen` — the transition stage's blocking reason is now visible inside the stage the
   user tapped (it previously sat below the whole flow, off-screen on a compact phone).
2. `SinglePaneOverview` — the tablet/medium quick entries now sit inside the first viewport. Before,
   they measured 1008×255px but were entirely below the fold (clipped `boundsInRoot` = `Rect.Zero`),
   so a tablet user never saw them.

### Fixed (tests / evidence tooling — no assertion was weakened)

3. `SourceCompleteScreenshotEvidenceTest` — branches on the real breakpoint instead of the ≥600dp
   device class (the expanded inspector workspaces only exist at EXPANDED; the CI tablet viewport is
   MEDIUM), and its quick-entry assertion now reports measured geometry and viewport.
4. `ProductionManualRelationshipContractTest` — scrolls the real `LazyColumn` to each option before
   clicking (an off-screen lazy item has no semantics node at all).
5. `ui_vnext_pixel_capture.py` — real scrolls instead of fixed coordinates, real current copy instead
   of stale strings, the migration target must be a genuinely `active` card, and the bottom nav must
   be a real bottom-bar hit region.
6. `production_vnext_secure_rehearsal.py` / `production_vnext_release_default_rehearsal.py` — wait for
   a marker that genuinely identifies the unlocked Reality shell, and additionally forbid that marker
   while locked and on the legacy route.
7. `ci_emulator_production_rehearsal.sh` — reinstalls the app after `connectedAndroidTest` (AGP
   uninstalls it, which previously killed the rehearsal with "Activity class does not exist"), and now
   verifies the installed APK's SHA-256 against the built APK before interpreting the cutover UI.

### Verified locally at exact HEAD (all logs in `logs/`)

`:core:test`, `:app:testDebugUnitTest`, `:app:assembleDebug/PreviewDebug/ProductionDebug`,
`:app:assemblePreviewDebugAndroidTest`, `:app:assembleProductionDebugAndroidTest`,
`ui_vnext_pixel_capture.py` (65 real PNGs), `TabletAdaptiveContractTest` (5/5),
`SourceCompleteScreenshotEvidenceTest` (35 shots), `ProductionVNextShellContractTest` (11/11),
`ProductionManualRelationshipContractTest` (1/1), `PRODUCTION_VNEXT_SECURE_REHEARSAL=PASS`,
`PRODUCTION_VNEXT_RELEASE_DEFAULT_REHEARSAL=PASS`.

## 3. Remote CI status — observed per SHA

| Workflow                              | `5a09185` (round start) | `bc39a2f2` (fix + evidence) |
| ------------------------------------- | ----------------------- | --------------------------- |
| CI                                    | success                 | **success**                 |
| iOS                                   | success                 | **success**                 |
| Android UI vNext Preview APK          | success                 | **success**                 |
| Android UI vNext Phone Pixel Proof    | failure                 | **success**                 |
| Android UI vNext Tablet Pixel Proof   | failure                 | **success**                 |
| Android UI vNext Production Rehearsal | failure                 | **failure** (see §4)        |

`86c482e4` re-ran CI / iOS / Preview APK (**all success**) and the Production Rehearsal (**failure**).
It does not touch the phone/tablet workflow path filters, so those gates were not re-triggered for it.

The evidence commits (`bc39a2f2`) intentionally kept the phone/tablet workflow path filters in play
so the fixes were proven by CI on the same SHA rather than only locally.

## 4. Remaining gap: the production job's final sub-step

Everything in the production job now passes **except** `production_vnext_release_default_rehearsal.py`:

- The CI log at `86c482e4` prints `local_apk_sha256 == device_apk_sha256`
  (`688adf7420618c753705656fee46e4e8678479970650b96fb569409d36343e67`), so the correct
  `vnext` + `cutoverApproved` APK **is** installed.
- The shell contracts pass 11/11, the relationship authority passes 1/1, and the secure rehearsal
  passes — all in CI.
- The release-default rehearsal then still observes the **legacy** shell on the default (no-extras)
  launcher path, while the **identical commit passes it locally**, including from
  `adb uninstall` → `adb install` with an empty Reality database. The local device was checked and is
  equivalent to CI in the obvious respect: no lock credential and no biometrics.

This is reported as **unmet**, with the CI evidence recorded in
`docs/ui-vnext/R51_ANDROID_EXACT_HEAD_RUNTIME_REPORT.md` §4. Suggested next steps: read
`MainActivity` / `PdigApp`'s default-route shell selection for any input outside
`BuildConfig.PRODUCTION_UI_GENERATION` and `PRODUCTION_VNEXT_CUTOVER_APPROVED`, and compare a
locally created _fresh_ AVD (the round created `pdig_evi_phone_api36`) against the CI emulator.

## 5. What the user needs to do (the only genuinely blocking items)

1. **Human visual acceptance.** Open `docs/ui-vnext/R48_R51_VISUAL_ACCEPTANCE_MATRIX.md` and review
   the listed captures against the reference board. Suggested first screens:
   `phone/01-now.png`, `phone/02-infrastructure.png`, `phone/03-cards.png`,
   `phone/04-card-detail.png`, `phone/04i-card-change-transition-blocked.png`,
   `phone/06-records.png`, `phone/08b-number-detail.png`, `phone/09-me.png`,
   `tablet/ui-shots/02-overview-global.png`, `tablet/ui-shots/25-me.png`,
   `tablet/ui-shots/28-card-change-transition-blocked.png`.
   `HUMAN_ACCEPTED` stays **PENDING** until you say otherwise.
2. **Supply the original 1536×1024 light reference board** if pixel-level comparison is required. The
   in-repo copy is only 320×213, which is the one input gap that blocks a true per-screen diff.
3. **Supply the task book's master document** `PDIG_v2.3-R1_…_统一全量母版_2026-10-07.md` if it is
   still authoritative: it does not exist in the repository (local or remote), so the in-repo frozen
   specs were used instead.
4. Decide whether to authorise any further CI iteration on the §4 item.

## 6. Guardrails that were respected

- No force push, no history rewrite, no merge of PR #1, no change to the Production default route or
  any release/`ProductionRelease` configuration, no store release, no signing changes.
- No test deleted, skipped, or weakened; the enabled/asserted behaviour is equal or stricter
  everywhere this round touched it.
- No real user data, real bill, key, or credential entered the worktree or the evidence.
- Existing AVDs, evidence directories, branches, and other agents' commits were left intact.

---

## 7. FINAL STATE (updated after the CI runs completed)

All six workflows are **green** on the current remote branch head
`51b546cc8556e56c8a8f4521c703a7cc0c9ef4f8`:
CI · iOS · Android UI vNext Preview APK · **Phone Pixel Proof** · **Tablet Pixel Proof** ·
**Production Rehearsal**.

Attribution so the result is not misread:

- Phone Pixel Proof and Tablet Pixel Proof were fixed **by this round** (see §2) and were already green
  at `bc39a2f2`.
- The Production Rehearsal's last sub-step was closed **by a concurrent agent's commits**
  (`29f47af9 fix(ci): isolate production cutover build from previously inlined legacy BuildConfig` and
  its follow-ups), which addressed the build-authority inlining that §4 could not pin down from the
  logs available here. That fix is not this round's work — §4 is kept as the historical record.
- The first Tablet run at `51b546cc` failed in the runner's own SDK setup
  (`Error on ZipFile unknown archive` while installing the emulator; no emulator ever started).
  Re-running the failed job returned success, so it was transient hosted-runner infrastructure.

Remaining items are unchanged and are all human/external: human visual acceptance (`PENDING`), the
1536×1024 reference board, the v2.3-R1 master document, and the explicit decisions for
`ANDROID_REFERENCE_FREEZE` / `PRODUCTION_CUTOVER` (both `HOLD`).

The branch is shared: a subsequent push by another agent is expected. This round's commits are
`d3c6f459`, `bc39a2f2`, `86c482e4`, `7a331a47`, `9f4d3ad4` and the merge `7aab2b7d`.
