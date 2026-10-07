# ANDROID_LIGHT_REFERENCE_FINAL_SOURCE_AUDIT_2026-10-05

> Human-selected visual reference: `spec/ui-vnext/references/android/PDIG_ANDROID_LIGHT_VISUAL_REFERENCE_2026-10-05.jpg`
>
> Final pixel-changing Android production-UI checkpoint: `86cbf558d7204b5fa8ccad53f1b115f43f253531`
>
> Verdict: **SOURCE DESIGN RE-CLOSED AFTER ROUND4 RUNTIME + PARTIAL HUMAN-PIXEL REVIEW / FRESH EXACT-HEAD RUNTIME EVIDENCE REQUIRED**
>
> The `b659ae4...` runtime pack committed at `6cb48868...` was actually reviewed pixel-by-pixel and was
> rejected for Freeze. See `ANDROID_LIGHT_REFERENCE_HUMAN_PIXEL_REVIEW_ROUND2_2026-10-06.md`.

## 1. Authority

The selected light board is the Android visual-direction reference. It controls hierarchy, light-first tonal
language, asset identity, spatial depth, density, and product mood. It does not override product truth.

Non-negotiable truth remains:

- Primary IA: 现在 / 基础设施 / 变更 / 记录;
- Infrastructure siblings: 总览 / 卡片 / 号码 / 账户 / 邮箱 / 设备 / 服务 / 薄弱点;
- PresentationProfile != PersonalReality != Canonical;
- PresentationProfile never mutates `.depmap` / Canonical;
- Unknown never becomes safe by inference;
- Change Phone = Current / Transition / After; After = Plan Projection;
- frozen Desktop Dark Reference remains unchanged.

Generated brands, counts, dates, copy, and obsolete IA fragments inside concept art are illustrative only.

## 2. Android adaptive research applied

The source translates the visual reference instead of shrinking Desktop.

- Compact uses a four-destination bottom navigation.
- Larger windows use a navigation rail for top-level destinations.
- Infrastructure object families remain sibling/secondary navigation inside the content context.
- Compact list/detail flows remain focused single-pane flows.
- Expanded Cards/Numbers may use simultaneous list/detail composition.
- Focused child flows can hide root navigation so the task owns the viewport.

Authoritative references:

- https://developer.android.com/design/ui/mobile/guides/layout-and-content/layout-and-nav-patterns
- https://developer.android.com/develop/ui/compose/components/navigation-bar
- https://developer.android.com/develop/ui/compose/components/navigation-rail
- https://developer.android.com/develop/ui/compose/components/tabs
- https://developer.android.com/develop/adaptive-apps/guides/list-detail
- https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive

## 3. Visual system closure

Android source now uses a coherent light-first system:

- off-white / cool-blue canvas;
- white and soft-blue raised surfaces;
- restrained borders and shadows;
- strong blue primary actions;
- dark navy text hierarchy;
- semantic green / amber / red states;
- dark local canvases only where darkness communicates identity or spatial depth: Globe, Card faces and
  Number identity artwork.

This removes the old generic dark engineering/control-panel presentation while preserving the spatial
identity of the product.

## 4. Screen closure matrix

| Surface | Status | Reference intent |
| --- | --- | --- |
| Now | CLOSED | Greeting/task priority + large Globe world-view |
| Infrastructure Overview | CLOSED | Phone 8-category Hub + smaller regional Globe; Expanded spatial overview |
| Cards | CLOSED | Asset-first identities; compact visual list; optional full card-face view |
| Card Detail | CLOSED | Card identity first; facts/dependencies/risk subordinate |
| Card Studio | CLOSED | Preview-first + compact horizontal visual theme gallery |
| Numbers | CLOSED | Communication-identity surfaces, not generic phonebook rows |
| Number Detail | CLOSED | Number identity first; dependencies/recovery/history below |
| Number Studio | CLOSED | Preview-first presentation-only customization |
| Accounts / Emails / Devices | CLOSED | Light asset identity + explicit recovery/trust facts |
| Services | CLOSED | Region-grouped asset surfaces; missing relations remain unknown |
| Weaknesses | CLOSED | Confirmed-risk hierarchy; Unknown remains Unknown |
| Change Phone | CLOSED | Six-step continuity choreography; compact old→new scene; expanded 3-part scene |
| Records | CLOSED | Active change / attention / upcoming summary + timeline |
| Search | CLOSED | One recorded-data search surface preserving object identity |
| Personalization | CLOSED | Visual preview first; presentation boundary explicit |
| Data Sources | CLOSED | Coverage + explicit fact boundary |
| Region Context | CLOSED | Select/detail/back/global-reset hierarchy |
| Adaptive shell | CLOSED | <600 compact; 600–839 medium; >=840 expanded |

## 5. Regression guards

Source contracts now protect the selected direction:

- `AndroidLightVisualSourceContractTest`;
- `AndroidAdaptiveShellContractTest`;
- `PhoneStudioLayoutContractTest`;
- `PhoneCardsLayoutContractTest`;
- `PhoneNumbersListVisibilityContractTest`;
- `TabletAdaptiveContractTest`.

They protect structure and semantics; they are not pixel acceptance.

## 6. Deliberately rejected changes

The source audit does not:

- restore obsolete concept-image primary navigation;
- put all eight Infrastructure categories into the global rail;
- add decorative space backgrounds to every data page;
- make product rendering depend on real financial-brand assets;
- let Card/Number Studio modify Canonical truth;
- depict After as completed reality;
- infer unrecorded relationships as safe;
- pixel-copy Desktop onto Android.

## 7. Remaining work

The first fresh `b659ae4...` pack exposed real pixel/evidence defects (Global Globe fallback frames, invalid
Region/Search/Change captures, duplicate Tablet states, sparse Expanded Cards) plus stale instrumentation
contracts. ChatGPT directly remediated the source through the production checkpoint above. Further blind source
styling without new runtime pixels would now be speculative. The next valid artifact
must come from the exact remote HEAD and include Phone + Tablet runtime screenshots, empty states,
PresentationProfile persistence, workspace persistence, Search/back, Region context, Globe TEXTURE_READY,
Number Detail probes, Change three-state evidence, accessibility, consumer-copy audit, and Desktop Freeze
Guard.

Human review must inspect actual PNG pixels. Agent PASS labels cannot authorize visual acceptance.

## 7.1 Round3 exact-head evidence adjudication (2026-10-06)

The Local Agent recaptured `artifacts/runtime-evidence/2026-10-06-android-light-reference-round3-af25f50/`
from exact source `af25f50b43e50fc35444a6139ee915e4a83ded30` and committed that pack at `37342cc...`.

That pack is **not eligible for Freeze**, independently of visual taste, because its own report/manifests record
mandatory acceptance failures:

- official `SourceCompleteScreenshotEvidenceTest` still stops at Globe screen 03 before `TEXTURE_READY`;
- the required 10 empty states are therefore absent;
- Region Detail runtime evidence still records `actual=region-selected`, not `region-detail`;
- Phone accessibility still reports the bottom-navigation target below 48dp because the evidence tag was on
  the icon glyph rather than the clickable `NavigationBarItem`;
- Tablet Card Studio was not reachable from the expanded Card inspector;
- several adaptive/evidence contracts still exercised forced-wide or stale compact expectations.

ChatGPT directly remediated those source/test issues at production checkpoint
`9f8fb6fb7710b872d0f42c228da1fed8746e759a`:

- bottom-nav semantics now tag the clickable item;
- Region List supports select → second tap → Region Detail, and the drawer has an explicit runtime tag;
- Expanded Card inspector exposes both “定制卡面” and “查看完整详情”;
- the official Overview evidence path reuses the real 02 → 03 → 04 journey so a texture-ready Global frame is
  not discarded and redundantly re-rendered for Region Selected;
- expanded interaction contracts run only on a real expanded runtime;
- stale Tablet/compact geometry expectations were aligned with the frozen adaptive hierarchy.

Additional regression contracts require the Region List UI interaction to open the real drawer and preserve
region context on Back, require 04 evidence to contain the drawer layer, and require the expanded Card inspector
to expose both continuation actions.

These changes do **not** relax `TEXTURE_READY`, accessibility, state identity, Unknown semantics, or the
PresentationProfile/Canonical boundary. They make the evidence path exercise the production choreography that
the user actually sees.

A new pack from the exact latest remote HEAD is required. The `af25f50...` Round3 screenshots remain historical
evidence and cannot authorize Android Freeze.

## 7.2 Round4 evidence adjudication and direct remediation (2026-10-07)

`artifacts/runtime-evidence/2026-10-06-android-light-reference-round4-70edd4c/` was captured from
`70edd4c83639bc52266db35c124db6927f53df52` and committed at `da86d066...`.

Round4 materially improves evidence quality: the official Phone and Tablet screenshot suites each completed
24 main screens + 5 empty states; 01-04 record `globeTextureState=texture_ready`; 04 records
`actualState=region-detail`; 22 is a typed Search Query; Phone accessibility now reports the true clickable
bottom-navigation item.

It is still **not eligible for Freeze**:

- Tablet `regionListSecondTapOpensDrawerAndBackPreservesRegion` failed before first selection (`CN` remained null),
  because the common test forced a compact viewport inside the landscape Tablet instead of exercising the real
  device breakpoint and scrolling the real Region row into view;
- Tablet Card back-state still used the compact `CARD_ROW` path on an expanded Cards workspace;
- Expanded Cards selection was still driven by a text glyph instead of the clickable asset surface;
- the official Tablet 07/08 screenshots still entered Studio by direct app-state mutation rather than proving the
  required real UI path through the Expanded Card inspector;
- the decoded `contact-empty-10` sheet shows the Phone `no-attention` empty state captured while the Now Globe is
  still an untextured dark loading/fallback sphere; empty-state Human Review therefore cannot accept that frame;
- the monolithic Phone/Tablet contact-sheet base64 mirrors are too large for the GitHub connector text transport
  used for Human Pixel Review, so the 48 main pixels have not yet been fully inspected in this review;
- Round4 reports the same APK SHA-256 as Round3 despite a pixel-changing production checkpoint between those
  runs. Raw manifests do embed the new `BuildConfig.GIT_SHA`, but the APK-hash provenance must be re-established
  by hashing both the exact built APK and the installed `base.apk` pulled from the emulator and requiring equality.

ChatGPT directly remediated the product/test paths at `86cbf558d7204b5fa8ccad53f1b115f43f253531`:

- Expanded Card assets have deterministic clickable per-card tags;
- Expanded Card inspector moves `定制卡面` + `查看完整详情` above long services/risk content, making the Studio
  continuation immediately discoverable rather than scroll-hidden;
- Tablet 07/08 official evidence now must navigate Cards → selected card → inspector `定制卡面` → Studio before
  capture, and must render the Studio preview;
- the empty `no-attention` Now screenshot is now a Globe-gated frame and cannot capture before `TEXTURE_READY`;
- Region UI interaction now uses the real device breakpoint and `performScrollTo()` before the two taps;
- Cards back-state now uses compact row on compact devices and expanded asset → inspector detail on real Tablets;
- adaptive Cards selection now targets the clickable asset surface, not a text glyph.

Next Human Review evidence must additionally split Phone/Tablet review imagery into connector-readable chunks
(recommended 4 screens per sheet, each base64 text file < 900 kB) so all 48 main screens can be decoded and
visually inspected. Agent PASS labels still cannot authorize visual acceptance.

## 8. Gate

```
HUMAN_VISUAL_DIRECTION_REFERENCE = SELECTED
ANDROID_UI_VNEXT_SOURCE_DESIGN = COMPLETE
ANDROID_LIGHT_VISUAL_TRANSLATION_SOURCE = COMPLETE

ANDROID_RUNTIME_EVIDENCE_FOR_CURRENT_HEAD = REQUIRED
ANDROID_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_ACCEPTANCE
ANDROID_REFERENCE_FREEZE = HOLD

IOS_UI_VNEXT = HOLD
HARMONY_UI_VNEXT = HOLD
```
