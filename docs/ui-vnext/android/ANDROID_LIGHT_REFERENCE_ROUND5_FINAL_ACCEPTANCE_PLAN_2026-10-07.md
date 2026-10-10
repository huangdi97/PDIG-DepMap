# ANDROID LIGHT REFERENCE — ROUND 5 FINAL ACCEPTANCE PLAN

> Branch: `feat/android-ui-vnext-translation`
>
> Required exact-head baseline: `b69034d214d6070ea74edd6ed4690bc3061ba873`
>
> Final pixel-changing production checkpoint: `a3b3a05580c0cd2ed451e554ba7db3edfd523cc0`
>
> Human visual direction: `spec/ui-vnext/references/android/PDIG_ANDROID_LIGHT_VISUAL_REFERENCE_2026-10-05.jpg`

## 1. Purpose

Round5 is not another design exploration. Android source design is closed unless fresh runtime pixels expose a new
P0/P1 product, visual, adaptive, accessibility or interaction defect.

The only valid purpose of Round5 is to prove the exact latest source and provide connector-readable real pixels
for final Human acceptance.

## 2. Exact-source rule

The Local Agent must:

1. fetch + fast-forward only;
2. verify remote branch HEAD equals `b69034d214d6070ea74edd6ed4690bc3061ba873`;
3. build ProductionDebug + AndroidTest from that exact source;
4. record source HEAD/tree in every manifest;
5. make zero production/test/docs mutation;
6. commit evidence only after the full run.

If the remote HEAD changes before capture starts, abort and re-resolve the latest exact HEAD.

## 3. Official runtime set

Run `SourceCompleteScreenshotEvidenceTest` independently on:

- API36 Phone AVD;
- API36 Tablet AVD.

Required per device:

- 24 main screens;
- 5 empty states.

Required main state truth:

1. Now — Globe `TEXTURE_READY`
2. Overview Global — Globe `TEXTURE_READY`
3. Overview Region — actual UI Region row action, `region-selected`, Globe `TEXTURE_READY`
4. Region Drawer — second actual UI Region row action, `region-detail`, drawer node present, Globe `TEXTURE_READY`
5. Cards
6. Card Detail
7. Card Studio Glass
8. Card Studio City
9. Numbers
10. Number Detail
11. Number Studio Travel
12. Accounts
13. Emails
14. Devices
15. Services
16. Weaknesses
17. Change Current
18. Change Transition
19. Change After = Plan Projection
20. Records
21. Search Empty
22. Search typed query
23. Personalization
24. Data Sources

Tablet 07/08 must use Cards → clickable selected Card asset → inspector `定制卡面` → Studio.
Tablet 11 must use Numbers inspector `定制号码面` → Studio.

## 4. Empty-state Globe rule

The `no-attention` empty state renders Now and therefore also contains the signature Globe.

It must not be captured until `GlobeRenderState.TEXTURE_READY`.
A dark loading/fallback sphere invalidates the empty-state visual set.

## 5. Adaptive interaction gates

Both real device classes must pass:

- Region first tap selects;
- Region second tap opens Detail;
- Back closes Detail and preserves region;
- explicit global reset clears region;
- Cards compact row → Detail on Phone;
- Cards expanded asset → inspector → Detail on Tablet;
- Card inspector exposes `定制卡面` and `查看完整详情`;
- Numbers expanded selection updates inspector;
- Number inspector exposes `定制号码面` and `查看完整详情`;
- Search → Result → Back;
- system Back hierarchy;
- Change Current / Transition / After distinct.

Evidence interactions should use clickable semantics rather than text-glyph targeting.

## 6. Persistence

Fresh exact-head evidence must prove:

- Card PresentationProfile save + restart restore;
- Number PresentationProfile save + restart restore;
- workspace preference restart restore.

PresentationProfile remains presentation-only and must not mutate PersonalReality, Canonical, or `.depmap`.

## 7. Accessibility / craft gates

Required:

- bottom navigation clickable target >=48dp;
- Studio controls >=48dp;
- Region-scope reset >=48dp;
- light palette contrast contract PASS;
- no clipping / 0x0 / negative layout probes;
- Number Detail vertical ordering PASS;
- consumer copy contains no leaked internal implementation language.

## 8. Globe regression

Fresh pixels must specifically verify that large Now Globe on Phone and Tablet:

- is textured;
- is circular, not faceted/octagonal;
- preserves spatial depth;
- does not regress to a near-black fallback sphere.

The circular-silhouette regression contract must pass.

## 9. APK provenance

Record all of:

1. built APK path + SHA-256;
2. `adb shell pm path`;
3. pull installed `base.apk`;
4. installed APK SHA-256;
5. assert built SHA == installed SHA.

Do not reuse a previous run's APK hash.

## 10. Human Review bridge

The monolithic Round4 base64 files were too large for the GitHub connector. Round5 must emit:

### Phone
- 6 sheets × 4 screens;
- screen order 01–24;
- full frame, no crop;
- each sheet has screen labels;
- each `.b64.txt` < 900 kB.

### Tablet
- 6 sheets × 4 screens;
- same requirements.

### Empty states
- 1 sheet × 10 is acceptable if its base64 remains <900 kB.

Also keep original per-screen PNGs.

No mock, no re-render, no screenshot substitution.

## 11. Other gates

- Android build PASS;
- Android unit/instrumentation PASS;
- Desktop Freeze Guard PASS;
- no relevant FATAL / ANR / OOM;
- production source mutation by Local Agent = 0.

## 12. Freeze authority

Local Agent must stop at evidence publication.

It may write:

```ini
ANDROID_RUNTIME_EVIDENCE_FOR_CURRENT_HEAD = READY
ANDROID_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_ACCEPTANCE
ANDROID_REFERENCE_FREEZE = HOLD
IOS_UI_VNEXT = HOLD
HARMONY_UI_VNEXT = HOLD
```

It must not write `ANDROID_VISUAL_REFERENCE=ACCEPTED` or `ANDROID_REFERENCE_FREEZE=PASS`.

Only ChatGPT Human Pixel Review may authorize those states after all real Round5 pixels are inspected and no P0/P1
defect remains.

## 13. Android source status entering Round5

```ini
HUMAN_VISUAL_DIRECTION_REFERENCE = SELECTED
ANDROID_UI_VNEXT_SOURCE_DESIGN = COMPLETE
ANDROID_LIGHT_VISUAL_TRANSLATION_SOURCE = COMPLETE
ANDROID_FINAL_SOURCE_CRAFT = COMPLETE

FINAL_PIXEL_CHANGING_SOURCE = a3b3a05580c0cd2ed451e554ba7db3edfd523cc0
ROUND5_EXACT_HEAD = b69034d214d6070ea74edd6ed4690bc3061ba873

ANDROID_RUNTIME_EVIDENCE_FOR_CURRENT_HEAD = REQUIRED
ANDROID_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_ACCEPTANCE
ANDROID_REFERENCE_FREEZE = HOLD

IOS_UI_VNEXT = HOLD
HARMONY_UI_VNEXT = HOLD
```
