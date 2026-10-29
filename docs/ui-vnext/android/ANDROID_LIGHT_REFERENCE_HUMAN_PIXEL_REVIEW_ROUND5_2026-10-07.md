# ANDROID LIGHT REFERENCE — HUMAN PIXEL REVIEW ROUND 5

> Runtime source: `0b305e48bf4868b1aca2bd39dac720e19f3df7f8`
>
> Evidence commit: `2d4045c2b334d4b78d33cc595efbb8626c718e9a`
>
> Final pixel-changing Android source: `a3b3a05580c0cd2ed451e554ba7db3edfd523cc0`
>
> Human visual direction: `spec/ui-vnext/references/android/PDIG_ANDROID_LIGHT_VISUAL_REFERENCE_2026-10-05.jpg`
>
> Verdict: **HUMAN PIXELS ACCEPTED / TARGETED CONTRACT REVALIDATION REQUIRED BEFORE REFERENCE FREEZE**

## 1. Evidence actually inspected

Human review decoded and inspected real runtime pixels from the repository, not Agent PASS labels:

- Phone 01-24 through six 4-screen contact sheets;
- Tablet 01-24 through six 4-screen contact sheets;
- 10 empty states through the connector-readable empty-state sheet.

The contact sheets are direct aspect-preserving compositions of the original runtime PNGs. Original per-screen PNGs remain in the Round5 evidence pack.

## 2. Phone final review

- Now is task-first: greeting + concrete attention items + signature Globe, not a statistics dashboard.
- Infrastructure is an 8-category management Hub with spatial/region context, not a duplicate home page.
- Globe is textured, circular and spatially legible; no fallback sphere or faceted perimeter remains.
- Cards read as owned financial assets, not generic database rows.
- Card Detail is asset-first and keeps facts/dependencies/risk subordinate to identity.
- Card Studio is preview-first and presentation-only.
- Numbers read as communication identities; Number Detail keeps role/form/recovery/dependencies visible.
- Number Studio is preview-first and visually distinct from Card Studio.
- Accounts / Emails / Devices / Services / Weaknesses remain supporting infrastructure surfaces with explicit recovery/trust/unknown semantics.
- Change Phone has three clearly different projections. After remains plan projection, never completed reality.
- Records is continuity/task-oriented, Search is a focused utility, Personalization/Data Sources remain presentation/fact-boundary utilities.
- No P0/P1 clipping, dead surface, generic-dark-dashboard regression, truth-boundary defect or adaptive defect remains in the inspected Phone set.

## 3. Tablet final review

- Primary rail contains only Now / Infrastructure / Change / Records; the eight Infrastructure siblings remain inside content context.
- Now and Overview preserve spatial depth around the Globe instead of stretching the Phone layout.
- Cards is a real gallery/list + persistent asset inspector workspace.
- Card Detail and Card Studio use wide compositions appropriate to landscape Tablet.
- Numbers is a real list-detail communication-identity workspace.
- Number Detail visibly contains Hero → Summary → Services → Risk / Backup path; the Summary strip is content, not dead space.
- Number Studio exposes a wide preview and presentation controls.
- Change Current / Transition / After use a true OLD → SERVICES → NEW scene; services remain the center of migration logic and After remains explicitly planned.
- Supporting infrastructure and utility pages maintain the light surface hierarchy without turning the product into a desktop admin console.
- No remaining P0/P1 visual or product defect is visible in Tablet 01-24.

## 4. Empty-state review

10/10 accepted:

- Phone no-attention now contains a textured Globe (`TEXTURE_READY`) rather than the Round4 dark fallback sphere.
- Cards / Numbers empties retain object-specific semantics.
- Records empty state is action-oriented.
- Weaknesses never converts missing knowledge into “safe”.
- Tablet empties preserve the rail/content hierarchy.

## 5. Runtime / provenance facts used in the decision

- Phone screenshot suite: 24 main + 5 empty PASS;
- Tablet screenshot suite: 24 main + 5 empty PASS;
- Globe `TEXTURE_READY`: 10/10 relevant frames;
- real Region select → detail → Back preserve → explicit global reset PASS;
- typed Search → result → Back PASS;
- Tablet Card inspector → Studio PASS;
- Tablet Number inspector → Studio PASS;
- Card/Number PresentationProfile restart persistence PASS;
- workspace preference restart persistence PASS;
- touch-target and light-palette contrast gates PASS;
- Desktop Freeze Guard PASS;
- built APK SHA-256 == installed base.apk SHA-256 on Phone and Tablet;
- Local Agent production/test/docs mutation = 0.

## 6. Adjudication of Round5 R5-F1

Round5 reported `TabletAdaptiveContractTest.tabletNumberDetail_noDeadSpace` with a 212dp `hero→services` gap.

This is a stale contract, not a production visual failure. Current Number Detail intentionally contains:

```text
Hero
  ↓
NumberSummaryStrip
  ↓
Services
```

The old tablet assertion skipped `NumberSummaryStrip` and counted its height plus legitimate section gaps as dead space. The separate `NumberDetailVerticalFlowContractTest` already measures the actual hierarchy and passed on Phone and Tablet.

The tablet contract is corrected to require Hero → Summary <=32dp, Summary → Services <=32dp, and strict ordering. No production UI code is changed by this correction.

## 7. Final Human decision

```ini
HUMAN_VISUAL_DIRECTION_REFERENCE = SELECTED
ANDROID_UI_VNEXT_SOURCE_DESIGN = COMPLETE
ANDROID_LIGHT_VISUAL_TRANSLATION_SOURCE = COMPLETE
ANDROID_FINAL_SOURCE_CRAFT = COMPLETE
ANDROID_VISUAL_REFERENCE = ACCEPTED
ANDROID_REFERENCE_FREEZE = HOLD
IOS_UI_VNEXT = HOLD
HARMONY_UI_VNEXT = HOLD
```

The remaining HOLD is mechanical: rerun the corrected `TabletAdaptiveContractTest` from the new exact HEAD. If it passes and no new production mutation occurs, Android Reference Freeze may be promoted to PASS without another broad visual redesign or 58-frame recapture.

## 8. Scope after acceptance

Do not reopen Android broad visual design unless fresh runtime evidence exposes a concrete P0/P1 defect.

The accepted Android reference carries forward these product invariants for platform translation:

- task hierarchy over generic statistics;
- Globe as spatial infrastructure identity;
- Cards as financial asset identity;
- Numbers as communication identity;
- Detail = object first, facts/dependencies/risk second;
- Studio = preview first and presentation-only;
- Change = Current / Transition / After projection;
- Unknown never inferred as Safe;
- PresentationProfile != PersonalReality != Canonical.

iOS must translate these invariants into iOS-native interaction and visual grammar after Android Reference Freeze; it must not pixel-copy Android or the frozen Desktop Dark Reference.
