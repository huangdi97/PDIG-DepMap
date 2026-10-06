# ANDROID_LIGHT_REFERENCE_FINAL_SOURCE_AUDIT_2026-10-05

> Human-selected visual reference: `spec/ui-vnext/references/android/PDIG_ANDROID_LIGHT_VISUAL_REFERENCE_2026-10-05.jpg`
>
> Final pixel-changing Android production-UI checkpoint: `0dd0cee29d3b9410f211942283d96f893149dc39`
>
> Verdict: **SOURCE DESIGN RE-CLOSED AFTER 2026-10-06 HUMAN PIXEL REMEDIATION / FRESH EXACT-HEAD RUNTIME EVIDENCE REQUIRED**
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
