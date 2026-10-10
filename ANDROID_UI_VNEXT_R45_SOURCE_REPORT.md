# PDIG Android UI vNext — R45 Production Source Closure Report

> Date: 2026-10-10  
> Branch: `feat/android-ui-vnext-translation`  
> Status: **SOURCE IMPLEMENTED / EXACT-HEAD CI AND RUNTIME PENDING / REFERENCE FREEZE HOLD**  
> Not a Release approval, not a screenshot acceptance certificate.

## 1. Product invariants preserved

- Five first-class roots: **现在 / 基础设施 / 变更 / 记录 / 我**. Do not demote Me to an avatar or Settings.
- Android is light-first, consumer-facing and object-first. The earth is a meaningful spatial lens, not a decorative dependency graph.
- Production `RegionFact` and identity subtypes come only from governed confirmed Reality. **No region inference** from phone prefix, issuer, currency, carrier or locale.
- Preview synthetic fixtures remain outside Production. Card imagery, personal aliases and visual masking are local Presentation, not Canonical fields.
- Unknown is not zero/safe; recovery-use is not unique-recovery; two paths are not necessarily independent; done is not verified.
- Official release cutover remains two-key gated and defaults to legacy; Draft PR must remain unmerged pending exact-head gates.

## 2. This execution's verified source changes

| Area | Result | Commit |
| --- | --- | --- |
| Compose androidTest compilation | Remove nonexistent top-level `assertDoesNotExist` import; method invocation remains intact | `a7c579e364` |
| Production world geographic interaction | GPU anchor click **and** projected callout update the same Region Lens filter, while only moving the presentation camera; explicit global reset | `da7d5b3c806` |
| Region filter regression | JVM test selects a confirmed region, asserts `REGION_SELECTED`, clears to `GLOBAL`, checks underlying snapshot unchanged | `63301a3cf510` |
| Production financial asset collection | Adaptive 2/3/4-column visual card gallery; true payment instruments, saved local art/masking, authoritative detail navigation | `f3d0cff37b96` |
| Confirmed relation labels | Resolve current source peer objects and mask identity names before rendering; masked missing peers never fall back to cached dependency labels | `5679c3ca5aae`, `a3d36023ea2b`, `9a9efbed1d6f` |
| Relation privacy regression | Masked phone/generic/absent peers, unmasked saved alias, missing-peer fallback | `ccb6648001ac` |
| Current-reality / visual mapping | Replace obsolete R29/R43 assertions with an explicit R45 source-only delta and exact-head pending gate language | `a4024d6edcff`, `24e568d3759b`, `6d42e50d982` |
| Production Globe layout | Phone hero is 280dp; wider surface 360dp. Continues to use the same GPU family and governed regions | `9f5ad541ebc8` |

The 2026-10-10 baseline immediately preceding these patches was an R44 branch. The old failure at `a13cbdbf` was `ProductionSearchProjectionTest` global-masking behavior (previous fixes `cd7e456efc` and `6eff7b6cdf`). The first R44 Pixel/Rehearsal failures at `6eff7b6c` shared the compile-time invalid test import above.

## 3. Reference-screen alignment

| Reference | Source outcome | Not yet proven |
| --- | --- | --- |
| Now globe / infrastructure region context | R15 GPU renderer reused in Production; region selection is now a real Region Lens | Current phone/tablet pixel quality, camera motion and GPU texture readiness |
| Financial cards | Card-identity gallery replaces generic card rows; card face and local art are reused | 2-column actual-device clipping, brand legibility, material quality, screenshot approval |
| Number communication identity | Existing governed number face and local alias controls remain; R44 masking corrections retained | Human pixel review and whole-app privacy behavior |
| Change choreography | Production ChangePlan actions continue behind AppContainer authority; no synthetic completion | Exact-head runtime action/verification/readback and Current/Transition/After visual acceptance |
| Me fifth root | Existing source contract unchanged | Compact/Medium/Expanded navigation acceptance |

## 4. Mandatory fresh gates — do not claim PASS from source review

1. Resolve any fresh `CI`, iOS, Phone Pixel, Tablet Pixel, Preview APK and Production Rehearsal failure **on the exact remote HEAD**.
2. Verify `testProductionDebugUnitTest`, `compilePreviewDebugAndroidTestKotlin` and `compileProductionDebugAndroidTestKotlin`, followed by actual instrumentation.
3. Capture current-head API36 phone and tablet UI pixels for all five primary roots, Cards, Card Detail, Number Detail, Search, Change and Me.
4. Exercise regional globe callout/anchor -> filter -> card collection -> global reset, ensuring projection anchors follow the live camera.
5. Validate local art persistence, alias/masking persistence, global privacy plus per-object masking and sensitive relation names.
6. Rehearse real encrypted Reality with lock/unlock, imports, review, maintenance write/readback, keep-alive completion/recompute, action done/verified.
7. Obtain explicit Human Visual Acceptance against the human-provided light-reference boards.
8. Freeze the accepted Android reference; only then consider explicit Production VNext cutover. Keep the release default legacy meanwhile.

## 5. Deferred / not disguised as implemented

- Fully authoritative FailureDomain-aware recovery path independence, future Recovery Solver and provider policy automation.
- Ungoverned installment statements, transaction-derived balances, reward optimizers and telecom live status.
- iOS/Harmony translated UI pixels until the Android reference is accepted.
- Any release activation or human acceptance claim based solely on source code or old screenshots.

## 6. Anchor documents

- `PDIG_v2.3-R1_个人数字基础设施图谱与连续性系统_ProductArchitectureVNext_统一全量母版_2026-10-07.md`
- `ANDROID_UI_VNEXT_CURRENT_REALITY.md`
- `ANDROID_REFERENCE_MAPPING.md`
- `ANDROID_VISUAL_CONTRACT.md`
- `spec/ui-vnext/FIVE_PRIMARY_NAVIGATION_DECISION.md`
- `spec/ui-vnext/CAPABILITY_AUTHORITY_MATRIX.md`
- `docs/ANDROID_PRODUCTION_VNEXT_CUTOVER_RUNBOOK.md`
