# PDIG / DepMap — R52+ final implementation report

> Round: 2026-10-17/18 (Asia/Shanghai)  
> Branch: `feat/android-ui-vnext-translation` · PR #1 (Draft, not merged)  
> Local worktree: `E:\AI\号卡管理`  
> **Round-start local HEAD:** `508ee709108598e56f76670ac472a93b0ccf80ee`  
> **Round-start remote HEAD:** `562d83f44733def5c805c64e4ed53f8746ddbce4`  
> **Final source HEAD of this round:** `fe60a62245db17e5fba4d8dc2bbf399ae26c881f`  
> **Visual/Production evidence source HEAD:** `302a65d6` (phone 1.0), `b85c7a57`
> (1.3×), `32541cc0` (landscape/tablet 1.3×), `edc8b2f2` (phone 1.5×),
> `e202a349` (tablet 1.5×), `fe60a622` (Expanded 1.5×), `6b823fb6` (tablet),
> `7993f29d` (Expanded), `c035ce9a` (advanced Production).  
> Remote relationship at start: local was a strict ancestor, 0 ahead / 99 behind.
> It was fast-forwarded with `git merge --ff-only`; no force push, reset, clean or
> `main` merge was used.

## 1. Executive result

This round did not restart the design. It verified the actual remote state first,
then fixed concrete defects that the exact-head runtime evidence exposed:

1. the tablet screenshot pipeline silently omitted the GPU earth, so the tablet
   visual proof could not see the product's signature surface;
2. unnamed numbers repeated the same number twice on phone and tablet;
3. account detail printed the raw wire enum `active`;
4. card-change replacement copy printed the raw word `active`;
5. continuity findings and several Production surfaces printed engineering
   vocabulary (`Reality`, `RegionFact`, `maintenance_profile`, `Finding`,
   `Proposal / Candidate / Drift`, etc.);
6. the infrastructure hub called eight entries “8 类资产” even though two of them
   are not asset categories.

After the fixes, the exact-head Preview and Production builds were re-run on real
API36 emulators, and the 21 Production shell/record/relationship contracts passed
at the CI-equivalent viewport. The Production default route was re-proven Legacy,
and the explicit two-key cutover build was re-proven VNext behind the lock.

## 2. What was already on the remote (not this round's new work)

The 99 remote commits between `508ee70` and `562d83f4` already contained R52–R61:

- privacy masking across Change, Records, Search, Review, Import and manual
  relationship surfaces;
- authoritative re-read / stale-graph and stale-action fail-closed rules for
  ChangePlan completion and verification;
- the Records verification lens, Now verification attention journey, and
  Records → exact action → Back continuity (R57–R60);
- R61 governed Records route availability and honest stale-step explanations;
- the five primary roots `现在 / 基础设施 / 变更 / 记录 / 我`;
- Legacy as the compiled Production default and the explicit two-key rehearsal
  path.

This round re-verified those facts against GitHub and the code; it did not
re-count them as new features.

## 3. Source changes in this round

| Commit | Change |
| --- | --- |
| `e1572f61` | Evidence pipeline: use the SurfaceFlinger `screencap` path, wait for a presented frame (the first SwiftShader frame measured ~4.8 s), and require real blue earth pixels for `01-now`, `02-overview-global`, `03-overview-region-selected`. Compose `captureToImage()` omits `GLSurfaceView`; `UiAutomation.takeScreenshot()` is blank on this emulator. |
| `ab2d21f5` | Unnamed number identity appears once (title), with masking applied to the title when the privacy mask is on; account status uses `statusLabelZh`; card-change copy says 使用中; the eight-entry hub says 8 个入口. |
| `fa48f634` | Continuity weakness copy is consumer Chinese; `参考事实` replaces `已确认事实` on the synthetic reference cards; JVM tests forbid engineering tokens. |
| `58a1b3f7` | The phone pixel script asserts the corrected 8 个入口 copy (the eight-entry count and region assertions remain). |
| `74fbca89` | Production Now / Me / collection / detail copy no longer prints `RegionFact`, `maintenance_profile`, `identity_anchor_profile`, `Canonical v1`, `Reality`, `Finding`, `Proposal / Candidate / Drift`, or raw bucket names. Truth boundaries are unchanged. |
| `067388f9` | Phone capture harness: target the dedicated AVD by serial, retry transient UIAutomator dumps, and capture identity-top + impact-bottom frames for long detail pages. |
| `fc29138e` | Final Production region copy: remove `RegionFact` / `Region Lens` from the Now world-context line and update the shell contract assertion. Production-only; no Preview-rendered screen changes. |
| `6b823fb6` | Round 2: show a 9sp visible label under every primary rail icon at MEDIUM (still 80dp, 48dp touch target; tablet contract asserts all five); label the Me attention metric `需关注` instead of `待处理`, because Now's `需要处理` total also includes review items. Counts are unchanged. |
| `a1ee036f` | TestTag-only: expose `pdig.nav.<route>.label` in the expanded rail branch too, so the tablet contract is independent of rail mode (MEDIUM vs EXPANDED/rotation). No visual change. |
| `7993f29d` | Advanced Production copy: ChangePlan completion/version wording, establish/import boundaries, maintenance confirmations, manual relationship type list and unavailable state no longer print `Plan Projection`, `Reality`, `Canonical v3`, `runtime registry`, `graphRevision`, `Proposal` or `authority`. Gates and fail-closed behaviour unchanged. |
| `8fef8030` | androidTest-only 851dp robustness: scroll to lazy change-entry, multi-node text matchers, compact orbit assertion. 38/38 contracts pass at 1080×2340@440; no rendered change. |
| `c035ce9a` | Real-run copy audit: remove remaining `图谱修订`, `wechat_statement`, `PHONE_NUMBER / EMAIL_ADDRESS`, `图谱完整`, `FailureDomain / RecoveryCycle / ProviderPolicy / Continuity engine` and Preview R23–R25 `authority` / `Reality` / `subtype`. Behavior unchanged. |
| `302a65d6` | Finish R24/R25 consumer copy (`Reality`, `authoritative transaction`, `graphRevision`, `From/To`, `runtime relation registry`, `Proposal/Candidate`, raw capability enums); align the phone capture script and both manual contract tests. Phone recaptured at this source (70 PNG). |
| `226e8468` | Capture-script-only: the R25 step assertion now matches the final consumer sentence. No app code or rendered screen changes. |
| `b85c7a57` | Dynamic-type 1.3× fixes: region chip scales with fontScale; Now asset rail becomes 2×2 above 1.15×; Me identity cards grow and hints wrap. 1.0 layout unchanged. |
| `32541cc0` | Short landscape rail: below 600dp height the rail scrolls and skips the weighted spacer, so 数据源 / 设置 stay reachable. Tall tablets unchanged. |
| `edc8b2f2` | 1.5× accessibility: greeting column weight + single-line badge; R9 bottom bar 84dp and no double inset; Me identity cards stack full-width above 1.35×. 1.0/1.3 unchanged. |
| `e202a349` | Tablet 1.5× top bar: hide the PDIG wordmark above 1.15× so the section title isn't ellipsized; privacy chip shortens to 已遮蔽/未遮蔽. 1.0/1.3 unchanged. |
| `fe60a622` | Expanded 1.5×: Now uses single-column sections; Change Phone uses the compact stepper and compact continuity scene. 1.0/1.3 unchanged. |

No Canonical schema, `.depmap`, cryptographic parameter, LockGate policy,
Production default flag or release configuration was changed.

## 4. Verification at the final source HEAD

### 4.1 JVM / build

| Gate | Result |
| --- | --- |
| `:core:test` | PASS (up-to-date after the last core change; no core source changed this round) |
| `:app:testPreviewDebugUnitTest` | PASS |
| `:app:testProductionDebugUnitTest` | PASS (last run after the rail/Me change) |
| `:app:assemblePreviewDebug` | PASS |
| `:app:assembleProductionDebug` (default Legacy) | PASS |
| `:app:assembleProductionDebug` (vnext + cutoverApproved) | PASS |
| APK identity | recorded in `R52_PLUS_PRODUCTION_RUNTIME_REPORT.md` |

### 4.2 Real device runtime

| Gate | Result |
| --- | --- |
| Phone Preview capture (`ui_vnext_pixel_capture.py`) | PASS, see §5 |
| Tablet Preview screenshot suite (`SourceCompleteScreenshotEvidenceTest`) | PASS, 34 screens + manifest |
| Expanded contract + captures (1280×800 dp) | PASS, `TabletAdaptiveContractTest` 5/5, 11 screens + manifest |
| Production shell / record navigation / manual relationship contracts | PASS, 21/21 at 1080×2400@420 |
| Production secure rehearsal (Legacy default) | `PRODUCTION_VNEXT_SECURE_REHEARSAL=PASS` |
| Production release-default rehearsal (two keys) | `PRODUCTION_VNEXT_RELEASE_DEFAULT_REHEARSAL=PASS` |
| APK SHA vs installed APK SHA | equal (`df576561…`) for the cutover rehearsal |

A first run of the 21 Production contracts at 1080×2340@440 had **1 failure**:
`governedPhoneAndEmailAppearInProductionCategoriesAndDetails` could not see the
phone change entry because it was below the fold on the shorter viewport.

**Closed in Round 5:** `8fef8030` scrolls to the tagged LazyColumn node before
asserting, uses `onAllNodesWithText(...).onFirst()` for repeated labels, and
asserts the compact continuity orbit instead of the MEDIUM/EXPANDED-only
`pdig.change.new` tag. At 1080×2340 @440, **38/38 contracts pass** (21 Production
+ 17 interaction). No product assertion was removed and no product code was
changed. The same tests still pass at the CI 914 dp viewport.

## 5. Screens actually captured

### Phone (1080×2340 @440 dpi, Pixel 5 AVD, API36)

Capture source: Preview APK built at `6b823fb6`; later deltas are test/copy-only.
70 real PNGs, including:

- first launch, Now, Me from primary navigation, Now return, orbit / zoom / reset;
- Infrastructure hub, US region selected, global restored;
- Cards, expiry filter, filter cleared, card detail, services/statements/risk
  tabs, Impact Lens, inline card artwork, applied artwork, return to cards;
- Card change Current / Transition blocked / target selected / After (plan);
- Change Center, Change Phone, checklist, checklist footer, After (plan);
- Records;
- Accounts / Emails / Devices / Services collections and their detail pages,
  each with an identity-top frame and an impact-bottom frame where the real
  viewport requires it;
- Weaknesses;
- Numbers, number detail, alias editor result, alias saved (`HK-Main`), number
  studio, edited studio, number Impact Lens;
- Me, mask enabled, mask disabled, personalization, data sources;
- Establish → manual establish → manual relationship → review, plus the
  boundary captures and Up-chain;
- Search empty and a real `HSBC` query with three results (card, account,
  service).

### Tablet (1200×1920 @240 dpi, 800×1280 dp MEDIUM, Pixel Tablet AVD, API36)

Capture source: `6b823fb6`; later deltas are test/copy-only.
34 real PNGs + `source-complete-raw-manifest.json`, including:

- Now with the GPU earth actually visible;
- Overview global, region selected, region drawer, with the GPU earth;
- Cards 2-column grid, card detail, card artwork;
- Numbers, number detail, number studio;
- Accounts / Emails / Devices / Services / Weaknesses;
- Change current / transition / after; Records; Search empty and `招商` query;
  Personalization; Data sources; Me;
- Change Center and card-change current / transition-blocked / after-plan;
- five empty states.

The previous tablet artifact omitted the earth because of the capture method;
the new evidence includes it and the test now fails if the earth layer is absent.

### Expanded (2560×1600 @320 dpi, 1280×800 dp landscape, Pixel Tablet AVD, API36)

Capture source: `7993f29d`, package `com.pdig.app.preview.p7993f29`.
`TabletAdaptiveContractTest` is **5/5 OK** at this width, including the two
EXPANDED-only three-pane projection tests skipped at 800 dp.
11 real PNGs + `tablet-expanded/MANIFEST.json`:

- Now with all five visible rail labels;
- Infrastructure with the secondary chip row;
- Cards grid, selected card + right-side preview, full card detail;
- Change Center; Change Phone Current / Transition / After;
- Records; Me.

After remains a plan projection; the After capture shows `旧号码（已停用·计划）`,
`新主号（计划）` and unverified services still `待处理`.

### Production advanced surfaces (1080×2340 @440, installed `com.pdig.app`)

Capture source `c035ce9a`, launched with the documented `vnext_production` debug
extra (release default remains Legacy). 4 real PNGs + XML under
`production-advanced/`:

- Me at the management section (`数据版本`, `所有写入都经过正式确认`);
- Establish hub (`微信账单` source kind, manual/import/relationship entries);
- Manual relationship boundary (consumer `故障影响 / 恢复路径 / 服务商规则`);
- Review empty queue (`空队列不代表已记录关系完整`).

The system file picker was intentionally excluded: it is not PDIG UI. No Preview
synthetic data was written into the encrypted Reality.

### Dynamic type 1.3× (393×851 dp, phone)

Source `b85c7a57`; 6 real PNGs + XML under `font-scale-1.3/`:

- Now: full `中国大陆` chip; asset rail becomes 2×2 instead of overflowing;
- Infrastructure / Cards / Card detail / Change: filters, lifecycle grid and CTAs
  remain readable;
- Me: identity hints (`恢复用途 · 唯一性未知`, `受信任`) are visible; the number card
  height matches the two identity cards.

At 1.0 these branches are inactive, so the earlier phone/tablet sets remain valid.

### Tablet MEDIUM 1.3× (800×1280 dp)

Source `32541cc0`; 4 real PNGs + XML under `tablet-font-scale-1.3/`.
This was a verification pass, not a new fix: Now, Me, Change and Change Phone
remain readable at 1.3×; rail labels, six-counter grid, tabs and stage rows do
not clip. No new layout defect was found.

### Phone 1.5× accessibility (393×851 dp)

Source `edc8b2f2`; 4 real PNGs + XML under `font-scale-1.5/`.
At 1.5× the greeting badge wrapped to three lines, the subtitle clipped, the R9
bottom labels were cut by the fixed 68dp bar, and Me identity hints disappeared in
narrow columns. Fixes: greeting column weight + single-line badge; R9 bar 84dp and
no double inset; Me identity stacks full-width above 1.35×. 1.0/1.3 are unchanged.

### Expanded 1.5× (1280×800 dp)

Source `fe60a622`; 4 real PNGs + XML under `expanded-1.5/`.
At 1.5× the three-column Now dashboard overlapped its section headings and the
six-stage Change Phone rail wrapped labels into each other. Above 1.15×, Now uses
the single-column stack and Change Phone uses the compact stepper and compact
continuity scene. 1.0/1.3 Expanded is unchanged.

### Tablet MEDIUM 1.5× (800×1280 dp)

Source `e202a349`; 4 real PNGs + XML under `tablet-font-scale-1.5/`.
At 1.5× the section title was ellipsized to `现…` because the PDIG wordmark and
search consumed the top row; the privacy chip wrapped over the lock icon. Hide the
wordmark above 1.15× (keep the P mark) and shorten the chip to `已遮蔽/未遮蔽`.
1.0/1.3 are unchanged.

### Phone landscape 851×393 dp

Source `32541cc0`; 4 real PNGs + XML under `phone-landscape/`.
At 851 dp width the layout is EXPANDED by width, but the 393 dp height cannot hold
the bottom-aligned 数据源 / 设置 rail. The rail now scrolls below 600 dp height;
tall tablets keep the original bottom alignment. The secondary entries are
reachable with 48 dp targets.

## 6. Bugs fixed and their evidence

| Defect | Before | After | Evidence |
| --- | --- | --- | --- |
| Tablet globe missing from evidence | `01-now` blank hero | real earth in `01-now`, `02-overview-global`, `03-overview-region-selected` | tablet 01/02/03 + pixel gate |
| Duplicate number identity | number printed twice per row | one title; secondary number only when a distinct alias exists | phone 08-numbers, tablet 09-numbers |
| Raw status enum | `状态 active` | `状态 使用中` | phone 07-accounts-detail |
| Raw `active` in card-change copy | `处于 active 的卡片` | `处于使用中的卡片` | phone 04i / tablet 28 |
| Engineering jargon in findings | `参考 Reality`, `FailureDomain`, `RecoveryCycle` | consumer Chinese with the same why/basis/unknown/next structure | phone 07-weaknesses, tablet 16 |
| Production jargon | `RegionFact`, `maintenance_profile`, `Finding`, `Reality` | consumer Chinese | Production capture + shell tests |
| Misleading asset count | `8 类资产` | `8 个入口` | phone 02-infrastructure |
| MEDIUM rail was icon-only | five primary roots required icon memory | visible 9sp label under each icon; all five asserted | tablet 01-now / 25-me |
| Me mixed two pending scopes | `待处理 3` beside Now `需要处理 6` | Me says `需关注 3` / `3 项需关注`; counts unchanged | tablet 25-me, phone 09-me |
| Remaining graph/authority jargon found by real run | `图谱修订`, `wechat_statement`, `PHONE_NUMBER`, `图谱完整`, `FailureDomain / Continuity engine` | `数据版本`, `微信账单`, `手机号 / 邮箱`, `已记录关系完整`, consumer continuity wording | production-advanced 4 screens |

## 7. Remaining gaps (not claimed as PASS)

| Priority | Item | Status |
| --- | --- | --- |
| Resolved | Advanced Production manual/import/establish copy | ChangePlan / establish / import / maintenance / manual relationship / review now use consumer Chinese; behavior unchanged. |
| Resolved | 851 dp phone test visibility | `8fef8030`; 38/38 contracts pass at 1080×2340@440, no product assertion removed. |
| External | Original 1536×1024 reference board is absent; the in-repo 320×213 JPEG is truncated (`broken data stream`). | Blocks pixel-level diff, not progression. |
| Human | Visual acceptance, Android reference freeze and Production cutover. | `PENDING` / `HOLD` / `HOLD`. |

## 8. Gate statement

```text
SOURCE_IMPLEMENTED          = YES (this round's listed fixes)
EXACT_HEAD_TESTED           = YES (JVM + instrumentation + Production rehearsals;
                                  final e202a349 re-runs all nine checks)
RUNTIME_VERIFIED            = YES (phone Preview / tablet Preview / Production lock & cutover rehearsal)
VISUAL_CANDIDATE            = READY_FOR_HUMAN_REVIEW
HUMAN_ACCEPTED              = PENDING
ANDROID_REFERENCE_FREEZE    = HOLD
PRODUCTION_CUTOVER          = HOLD
PRODUCTION_DEFAULT          = LEGACY (unchanged, re-proven)
GITHUB_CI_AT_FINAL_HEAD     = PASS at e202a349 (9/9 checks; source e202a349)
```

No screenshot in this round is AI-generated, a desktop render, a Compose preview,
or a synthetic mockup. The phone evidence is `adb exec-out screencap`; the tablet
evidence is shell `screencap` of the real display; the Production evidence is
real app XML plus PNGs from the installed APK.
