# ANDROID POST-FREEZE CLOSURE — 2026-10-21

> Round: PDIG / DepMap · Android post-freeze 收口、exact-HEAD runtime 取证、Production rehearsal 与最终 Visual Candidate
> Worktree: `E:\AI\号卡管理` · Branch: `feat/android-ui-vnext-translation` · PR #1
> Executed: 2026-10-21 (Asia/Shanghai), continued past midnight local time for the tablet runs.

## 0. Gate summary (only what was actually run here)

| Gate | Result | Evidence |
| --- | --- | --- |
| `SOURCE_IMPLEMENTED` | YES | this commit series |
| `ANDROID_CORE_TEST_PASS` | YES — 108 tests, 0 failures, 0 skipped | `core/build/test-results`, `logs/gate-jvm.log` |
| `ANDROID_REPOS_TEST_PASS` | YES — 13 tests | same |
| `PREVIEW_UNIT_TEST_PASS` | YES — 240 tests | same |
| `PRODUCTION_UNIT_TEST_PASS` | YES — 240 tests | same |
| `CONFORMANCE_PASS` | YES — Android 161/161 PASS, 0 fail, 0 notImplemented | `conformance/reports/android.json` |
| `EXACT_HEAD_BUILD_PASS` | YES — 4 variants + 2 androidTest APKs | evidence `apk/` |
| `PREVIEW_APK_IDENTITY` | PASS — `com.pdig.app.preview.pa3049c6`, label `PDIG Preview a3049c6`, launcher `com.pdig.app.PreviewLauncherActivity`, no `MainActivity` launchable | `aapt dump badging` |
| `PHONE_RUNTIME_PASS` | PARTIAL — 13 Production screens + full Change journey captured | `phone/`, `stale-reanalysis/`, `privacy/` |
| `TABLET_RUNTIME_PASS` | PARTIAL — 5 expanded + 5 medium captures; adaptive contract class has 4 failures (see §5) | `expanded/`, `tablet-medium/` |
| `PRODUCTION_REHEARSAL_PASS` | YES — `PRODUCTION_VNEXT_SECURE_REHEARSAL=PASS` | `production/` |
| `PHONE_MBB_FULL_JOURNEY_PASS` | YES — blocked → eligible → retired, captured | `phone/production-change-*` |
| `STALE_PLAN_REANALYSIS_PASS` | YES — stale blocked, reanalysis advanced the analyzed revision | `stale-reanalysis/` |
| `PRIVACY_ALL_OBJECTS_PASS` | PARTIAL — phone/account/email/service identity + inventory + Me verified; membership/custom not present in seeded Reality | `privacy/` |
| `CURRENT_HEAD_SCREENSHOTS_COMPLETE` | NO — 31 captured, 28 §27 phone-preview ids recorded `NOT_RUN` | `MANIFEST.json` |
| `REFERENCE_COMPARISON_COMPLETE` | NO — see `PIXEL_REFERENCE_LIMITATION` | §6 |
| `APK_CANDIDATE_READY` | YES (debug-signed review candidate only) | `apk/` |

`VISUAL_CANDIDATE = READY_FOR_HUMAN`
`HUMAN_VISUAL_ACCEPTANCE = NOT_RUN`
`PREVIOUS_FREEZE = historical approved candidate (2026-10-18, different source head)`
`CURRENT_POST_FREEZE_CANDIDATE = awaiting human confirmation`
`STORE_RELEASE = NOT_READY`

## 1. Exact references

| Item | Value |
| --- | --- |
| `START_REMOTE_HEAD` | `a3049c6480a6121f5ffbd7df79364c1328bedc04` |
| `START_LOCAL_HEAD` | `5668c3f916d3ffdc7323757d26588862f175bab8` (strict ancestor, 401 behind, 0 ahead) |
| `ANDROID_TREE_BASE` | `9bad55cb` — last commit touching `android/` on the remote at round start |
| Sync method | `git fetch --all --prune` + `git merge --ff-only` (no force, no `reset --hard`) |
| Evidence root | `artifacts/runtime-evidence/2026-10-21-android-postfreeze-closure/` |
| Captured APK sha256 | `6512acb58cb9067d22c86b2a4fcb117cb7d4140b7cfe6dcc363344b9b310ee94` (productionDebug) |
| Preview APK sha256 | `506da22b8027aa6f8183b771d35cefc6d0d71fb60b12422ee9c191edc0cb575b` |
| Frozen local Gradle init-script workaround | `GRADLE_USER_HOME` pointed at an isolated home with junctions to the real caches; see §7 |

`FINAL_REMOTE_HEAD`, `LOCAL_HEAD`, `PUSHED_HEAD` and the commit list are appended in §8 after the push.

## 2. Domain / Change kernel work in this round

The Change kernel itself was already correct; what was missing was *provable* coverage for three orderings and two product-visible semantics.

### 2.1 New wedge tests (instrumentation, real migrated SQLite + AppContainer)

`android/app/src/androidTest/kotlin/com/pdig/uivnext/evidence/ProductionChangeKernelContractTest.kt`

| Test | Invariant |
| --- | --- |
| `laterStepCannotCompleteWhileAPrerequisiteIsUnfinished` | `rpn-change-1` before `rpn-prepare-1` → `action_missing_prerequisite`; after the prerequisite is recorded the same command succeeds |
| `verificationCannotPrecedeRecordedCompletion` | `verifyAction` on a step that is not done → `not_done`; the step stays `pending` |
| `terminalPlanIsHistoryAndRejectsReanalysisAndMutation` | a plan walked to `completed` rejects `reanalyzePlan` (`plan_frozen`) and further `completeAction`/`verifyAction` (`action_frozen`), and its analyzed revision does not move |

Existing coverage that remains green and was re-verified this round:

- `ProductionPhoneChangeAuthorityContractTest` — retirement gated on unbound impacts and on review-required scope; `plan_stale` fail-closed; `verification_prerequisite_not_met`; full Make-Before-Break chain to `completed`.
- `PlanSupportTest` — `retirementRequiresMigrationAndRecoveryPathsCompletedAndVerified`, `retirementGateFailsClosedWhenVerificationStepIsMissing`, `readinessActionCountKeepsDoneButUnverifiedChangeWorkPending`, claiming rules.
- `PlanSupportClaimingTest` — claims tied to the current snapshot; reappearing impacts are not resolved by historical done-claims.

### 2.2 Product change: the Change workbench now names the next executable step

`ProductionChangePlanScreen.kt`

- `productionNextStep(plan, canMutate, privacyMask)` — a pure projection that returns exactly one of `REANALYZE / COMPLETE / VERIFY / NONE`, using the same eligibility functions the per-step buttons use. It never re-derives readiness and never enables an action the AppContainer would reject.
- `ProductionNextStepCard` — one primary CTA ("记录完成这一步" / "确认验证这一步"), plus why-now, prerequisites and whether verification is still required afterwards. When the plan is stale the card offers **no** button: the single authoritative reanalysis button stays in the authority banner, so the surface never presents two competing primary actions.
- The per-step `onComplete` / `onVerify` bodies were extracted into one shared `completeAction` / `verifyAction` path, so the CTA and the step cards cannot drift apart; both still re-read the live plan immediately before writing.
- `AFTER` stage now states 未发生 literally: `计划预期 · 未发生 · 完成并验证前不代表现实` and `完成后只是计划预期，尚未发生。…`.

New tests: `ProductionNextStepTest` (8 tests, JVM) and two Compose contract tests in `ProductionChangeStageInteractionContractTest` (`nextStepCardMakesTheExecutableStepTheSinglePrimaryAction`, `staleNextStepKeepsReanalysisAsTheOnlyPrimaryButton`), plus a `未发生` assertion on the AFTER stage.

`ProductionChangeStageSummaryTest` was updated to the new exact AFTER string — the assertion is still exact, not weakened.

### 2.3 Script portability fix (no assertion changed)

`production_vnext_secure_rehearsal.py` and `production_vnext_release_default_rehearsal.py` decoded adb output with the Windows ANSI code page, which made every UIAutomator label unreadable here (GBK). Both now decode as UTF-8 with replacement. Same assertions, same flow.

## 3. Make-Before-Break runtime journey (AC-5) — what was actually tapped

Seeded isolated test Reality (debug/androidTest only, never the Production data path):
`旧主号` phone identity (confirmed identifier), `主邮箱` email identity, `主账户` account, `登录服务` service; confirmed `recovers` phone→account and `authenticates` phone→service; then a `replace_phone_number` plan. **No action was pre-completed** — the whole chain below was executed by tapping the real UI, which calls `AppContainer`.

| Shot | State proven |
| --- | --- |
| `production-lock` | LockGate before any Reality (「PDIG 已锁定」 + explicit acknowledgement) |
| `production-now` / `production-infrastructure` | five frozen primary destinations; governed inventory |
| `production-account-detail` / `-email-detail` / `-service-detail` | heroes `账户身份` / `通信身份` / `服务节点`; no Passkey/Authenticator/Secure Enclave/Trusted device/MFA/通行密钥 as fact (asserted absent) |
| `production-change-center` / `-current` | CURRENT / TRANSITION / AFTER(计划) choreography + 下一步 |
| `production-change-pending-verification` | migration recorded done, verification pending: 「动作已记录完成，结果尚未验证」, CTA is verification, 「确认验证会写入正式记录」 |
| `production-change-retirement-blocked` | retirement step visible with 「等待前置步骤」 while verification is pending |
| `production-change-retirement-eligible` | after both new-path verifications, the CTA itself becomes 「停用旧手机号（新路径全部验证后）」 |
| `production-change-after-plan` | AFTER still shows 计划预期 / 未发生 after retirement |
| `production-records` | Records groups by plan: 已记录完成 and 已验证 listed as separate states |

## 4. Stale → reanalysis (AC-6)

Seeded plan, then one governed mutation **after** analysis (`后来新增的账户`), making `currentGraphRevision > lastAnalyzedGraphRevision`.

- `production-change-stale`: 「基础设施已发生变化；重新分析并确认当前影响范围前，不会继续记录新的执行完成。」, the only primary action is 「重新分析当前影响」, and the check asserts **no** execution CTA is offered.
- `production-change-reanalyzed`: after tapping it, the script parses 「分析时数据版本 N · 当前数据版本 M」 and **fails the capture unless N == M**, i.e. the analyzed revision really advanced. No UI-local state is involved.

## 5. Tablet runtime (AC-10) and its known failures

AVD `pdig_tablet_api36`, 2560×1600 @320 → 1280×800 dp.

- Expanded (landscape): `tablet-now`, `tablet-infrastructure`, `tablet-change`, `tablet-records`, `tablet-me` — five primary destinations present in the rail, `REALITY_MARKER` present.
- Medium (portrait 800×1280 dp): the same five surfaces, captured as `medium-*`.
- `TabletAdaptiveContractTest` + `AndroidAdaptiveShellContractTest` on this AVD: **4 failures**, all of them geometry/breakpoint assumptions rather than crashes:
  - `tabletChangeProjections_showThreeColumnsOnFirstScreen` — `[current] OLD must be left of SERVICES`
  - `tabletNumberDetail_noDeadSpace` — `lifecycle→services gap must stay <=32dp (gap=-1600.0)`
  - `compactChildFlow_hidesRootNavigationAndGlobalUtilities` — root navigation is displayed
  - `compactInfrastructure_usesHubInsteadOfPersistentSecondaryStrip` — hub not displayed

`KNOWN_LIMITATION`: two of these classes encode an EXPANDED window class assumption and two encode a COMPACT one; this AVD lands in the window class the respective test does not describe. **No assertion was weakened, skipped or deleted** — the failures are reported as-is and remain open work. The workflow's own tablet gate runs `TabletAdaptiveContractTest` and `SourceCompleteScreenshotEvidenceTest`; that combination was not re-run here.

## 6. Privacy (AC-12) and the reference limitation

| Shot | Meaning |
| --- | --- |
| `production-privacy-default-off` | masking OFF by default, under 「隐私与偏好」, with 「本机显示偏好，不修改已确认记录」 |
| `production-privacy-on` | toggled inside the real preference row; the script fails unless the row's own state changed |
| `production-privacy-on-infrastructure` | masked inventory — the check fails if the real identity name `旧主号` is echoed |
| `production-privacy-off-restored` / `-infrastructure` | masking off restores the real value; the canonical record was never rewritten |

`PIXEL_REFERENCE_LIMITATION`: the in-repo Android reference images remain the small historical set (≈320×213). No pixel-diff was claimed here. This round compared hierarchy, density, surface rhythm, copy and responsive behaviour, not per-pixel identity.

## 7. Environment note (not a product change)

A user-level Gradle init script outside this repository — `C:\Users\Kaiser\.gradle\init.d\zhishen-local-mirror.gradle` (created 2026-10-18 by another project) — adds project-level Maven repositories, which this repository rejects (`repositoriesMode = FAIL_ON_PROJECT_REPOS`). Builds here were run with `GRADLE_USER_HOME` pointed at an isolated home whose `caches`/`wrapper`/`jdks` are junctions to the real ones and which has **no** `init.d`. The user's global configuration was not modified.

## 8. Commits, head and CI

Filled in after the push:

| Item | Value |
| --- | --- |
| `NEW_COMMITS` | see §8.1 |
| `LOCAL_HEAD` = `PUSHED_HEAD` | see §8.1 |
| `FINAL_REMOTE_HEAD` | see §8.1 |
| `PR #1` | OPEN, non-draft, base `main` |
| CI (Android five workflows) | see §8.2 |

### 8.1 Commits

(appended at push time)

### 8.2 Remote CI observed for the final Android tree

(appended at push time)

## 9. What remains for a human

1. **Human Visual Acceptance** of the captured candidate (`HUMAN_VISUAL_ACCEPTANCE = NOT_RUN`). This agent cannot grant it.
2. **Real Pixel 6 Pro walk-through** — no device was attached; the API36 AVD remains the only runtime proof.
3. **Reference-quality pixel comparison** — blocked by `PIXEL_REFERENCE_LIMITATION`.
4. **Production signing identity / store release** — untouched by design.
