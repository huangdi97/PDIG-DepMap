# ANDROID LIGHT REFERENCE — HUMAN PIXEL REVIEW ROUND 4

> Evidence source: `70edd4c83639bc52266db35c124db6927f53df52`
>
> Evidence commit: `da86d06661793c2f8b066d3915f60664b1622e43`
>
> Direct remediation checkpoints: `86cbf558d7204b5fa8ccad53f1b115f43f253531` → `378fab2dac40c4119a81d272678158347ab82fb0` → `a3b3a05580c0cd2ed451e554ba7db3edfd523cc0`
>
> Verdict: **REJECTED FOR FREEZE / SOURCE REMEDIATION APPLIED / ROUND5 EXACT-HEAD EVIDENCE REQUIRED**

## 1. What Round4 proved

The official `SourceCompleteScreenshotEvidenceTest` completed on both API36 devices:

- Phone 24 main + 5 empty;
- Tablet 24 main + 5 empty;
- 01-04 Globe = `TEXTURE_READY` on both devices;
- 04 = `region-detail`;
- 22 = typed `search-query`;
- Current / Transition / After are distinct;
- Tablet Numbers / Number Detail / Number Studio are distinct;
- Phone bottom-navigation and Studio touch targets pass the >=48dp gate;
- Desktop Freeze Guard remains PASS;
- production-source mutation by the Local Agent = 0.

These are necessary mechanical/runtime facts, not Human Visual Acceptance.

## 2. Objective blockers found before visual acceptance

Round4's own report records three Tablet suite failures. Human review also found that some requirements were
only claimed by state/programmatic setup rather than by the required real UI path.

### R4-B1 — Region real-UI interaction is not closed

Tablet `regionListSecondTapOpensDrawerAndBackPreservesRegion` fails at the first post-click assertion:
`regionFilter` is still null.

Cause: the common interaction test forced the compact branch inside a landscape Tablet and attempted a click
without scrolling the real Region row into a stable tappable position.

Remediation: use the device's real breakpoint and `performScrollTo()` before each Region-row click.

### R4-B2 — Cards Back contract used the wrong adaptive interaction

The Tablet is an Expanded Cards workspace. A compact `CARD_ROW` is not the correct user path.

Remediation: compact = Card row → Detail; expanded = selected Card asset → inspector → 查看完整详情 → Detail.

### R4-B3 — Tablet Card Studio was not actually proven through the UI

The 07/08 screenshot setup still called `openCardCustomization()` directly. The report's wording
“via Expanded Card inspector programmatic studio” is therefore insufficient for the explicit real-UI
acceptance requirement.

Remediation: official Tablet 07/08 must start on Cards, select the target asset, click the inspector
`定制卡面`, assert the Studio preview, and only then capture.

## 3. Human pixel review actually performed

The `contact-empty-10.png.b64.txt` bridge is 651,528 characters and was successfully decoded from GitHub.
The 10 real empty-state pixels were visually inspected.

### Accepted direction

- Light-first canvas/surface hierarchy is coherent.
- Empty Cards / Numbers use restrained object-specific identity instead of generic admin-table emptiness.
- Weaknesses preserves explicit-risk / Unknown semantics rather than converting “not recorded” into safe.
- Tablet empty layouts retain the primary rail and content-level secondary infrastructure navigation.

### P1 evidence defect — Phone no-attention Globe

The Phone `no-attention` frame visibly contains an untextured dark loading/fallback sphere in the Now hero.
The Tablet counterpart is textured.

This frame is not visually acceptable evidence for the selected Light Reference. It does not justify changing
the Globe visual design; it shows that the empty-state capture path did not wait for the same `TEXTURE_READY`
gate as main Now/Overview.

Remediation: any empty state rendered on Now must also be a Globe-gated screenshot.

## 4. Main 48-screen Human Review bridge is still incomplete

The Round4 monolithic review mirrors are:

- Phone: 4,162,108 base64 characters;
- Tablet: 1,823,920 base64 characters.

They exceed the connector's large-text transport boundary, so they cannot be decoded through the current
GitHub review channel even though the PNGs exist in the repository.

This is a review-transport problem, not a visual PASS.

Round5 must generate connector-readable review sheets:

- Phone: 6 sheets × 4 screens;
- Tablet: 6 sheets × 4 screens;
- keep every base64 text mirror below 900 kB;
- preserve full frame, aspect ratio, and screen labels;
- no crop, no mock, no re-render.

Only those decoded real pixels may authorize Human Visual Acceptance.

## 5. APK provenance anomaly

Round4 reports APK SHA-256
`584cfb36aae48a380eff81caa7a8f34d5ea78bf48d91446b900fe8a6eddc21c9`, identical to Round3 even though
a pixel-changing production commit exists between the two source heads.

The raw manifest's `BuildConfig.GIT_SHA=70edd4c` is useful evidence, but the duplicated APK hash must not be
hand-waved away.

Round5 provenance must record and compare:

1. SHA-256 of the exact APK produced by the exact-head build;
2. `adb shell pm path` for the installed package;
3. `adb pull` of installed `base.apk`;
4. SHA-256 of pulled `base.apk`;
5. built APK SHA == installed APK SHA.

## 6. Direct source remediation

Commit `86cbf558d7204b5fa8ccad53f1b115f43f253531`:

- moves Expanded Card `定制卡面` + `查看完整详情` above the fold;
- gives expanded Card assets deterministic clickable identity;
- converts Tablet 07/08 official evidence to the real inspector-to-Studio journey;
- Globe-gates the no-attention empty screenshot;
- fixes Region UI interaction to use the real breakpoint + scrolling;
- makes Cards Back adaptive;
- removes text-glyph dependence from Expanded Cards selection tests.

## 6.1 Continued main-screen pixel review

After the initial Round4 adjudication, the repository PNG files were decoded directly rather than relying on
Agent PASS labels. Human Review inspected the complete Phone 01-24 set plus Tablet 01 and 06-24.

Accepted visual/product direction in the inspected frames:

- Phone Cards / Card Detail / Card Studio preserve card identity rather than generic settings/table rows;
- Phone Numbers / Number Detail / Number Studio preserve communication identity;
- Tablet Numbers is a real list-detail workspace and the Detail / Studio frames are independent;
- Change Phone has distinct Current / Transition / After compositions on both form factors;
- After is visibly and verbally a plan projection, not completed reality;
- Records, Search, Personalization, Data Sources, Accounts, Emails, Devices, Services and Weaknesses remain
  light-first and consumer-facing enough for their supporting roles;
- no inspected supporting screen introduces a new P0 product-truth defect.

### R4-P1 — Signature Globe silhouette is visibly faceted

Phone 01 Now and Tablet 01 Now show the real textured Earth with an obvious octagonal/faceted perimeter.
The same renderer can look rounder in smaller Overview stages, which initially hid the bug. Because Globe is the
signature spatial identity of PDIG, the large Now hero cannot be accepted with a polygonal silhouette.

Root cause is renderer coordinate math, not the selected visual direction. The capped texture bitmap was sampled
using the larger on-screen radius and then culled by world-space z after inverse camera rotation.

Commit `378fab2dac40c4119a81d272678158347ab82fb0` fixes the sphere normalization/culling and adds a regression
contract for circular silhouette under the exact cap condition that produced Round4's faceting.

### Remaining visual transport gap

Tablet 02 Overview Global, 03 Region Selected, 04 Region Drawer and 05 Cards are each >1 MB and cannot be
decoded through the current single-file connector transport. They are therefore **not** silently treated as
Human-Pixel PASS. Round5 split sheets must expose those exact frames (no crop / no re-render) in connector-readable
chunks before Freeze.

## 6.2 Final source craft before Round5

After the Globe fix, final Android-light craft continued through `a3b3a05580c0cd2ed451e554ba7db3edfd523cc0`:

- Number expanded inspector continuation hierarchy was brought to parity with Cards;
- region-scope reset meets the 48dp touch-target contract;
- light-surface blue text was separated from the brighter filled-action blue to protect normal-text contrast;
- the updated palette is guarded by `AndroidLightAccessibilityPaletteContractTest`;
- test paths were hardened around real clickable semantics so Tablet evidence is not invalidated by pointer-injection
  jitter or hidden inspector actions.

Round5 must therefore be built from the latest exact remote HEAD, not from `378fab2...` or Round4's `70edd4c...`.

## 7. Gate

```ini
ANDROID_RUNTIME_EVIDENCE_FOR_CURRENT_HEAD = REQUIRED
ANDROID_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_ACCEPTANCE
ANDROID_REFERENCE_FREEZE = HOLD

IOS_UI_VNEXT = HOLD
HARMONY_UI_VNEXT = HOLD
```

Round4 is historical evidence. It cannot authorize Android Freeze.
