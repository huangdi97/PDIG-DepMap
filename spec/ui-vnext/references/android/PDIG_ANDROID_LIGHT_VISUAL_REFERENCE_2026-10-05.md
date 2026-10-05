# PDIG Android Light Visual Reference

This file is the Human-selected visual-direction reference for Android UI vNext.

- Reference asset: `PDIG_ANDROID_LIGHT_VISUAL_REFERENCE_2026-10-05.jpg`
- Repository reference copy: **320×213 JPEG**, SHA256 `4b2ca9e059aa9f0d0c4b9e46063c2667b7eee2e50352b80ac795d3f8d77c3caf`
- Human-selected source render: **1536×1024**, light-first Android Phone + Tablet concept board
- Selection date: 2026-10-05
- Status: **HUMAN_VISUAL_DIRECTION_REFERENCE = SELECTED**
- Scope: Android presentation / visual hierarchy / density / light-first craft.
- Not functional truth: generated labels, counts, brands, old IA fragments, and example data in the image are illustrative only.

## Binding visual principles

1. **Light-first presentation.** Android vNext should default to a bright, airy surface system: off-white canvas, soft cool-blue containers, restrained translucency, crisp blue actions, dark navy text, and semantic status colors.
2. **Globe as infrastructure navigator.** The globe is a product surface, not decorative wallpaper. Region context and infrastructure counts attach to it.
3. **Assets look like assets.** Cards preserve recognizable card faces; numbers use communication-identity surfaces rather than generic rows.
4. **Task hierarchy over dashboard chrome.** "需要处理" and active changes outrank generic statistics. Secondary navigation and utility controls must stay quiet.
5. **Studio is preview-first.** Live asset preview and visual choices dominate; property controls are secondary and presentation-only.
6. **Change Phone is continuity choreography.** Current / Transition / After remains the semantic model. After is a plan projection, never confirmed reality.
7. **Adaptive translation, not pixel copy.** Phone uses compact bottom navigation and focused single-pane flows; Tablet uses primary rail plus content-level secondary navigation and list/detail where appropriate.

## Non-negotiable product truth

The current frozen IA remains:

- Primary: 现在 / 基础设施 / 变更 / 记录
- Infrastructure: 总览 / 卡片 / 号码 / 账户 / 邮箱 / 设备 / 服务 / 薄弱点
- PresentationProfile != PersonalReality != Canonical
- PresentationProfile must never mutate `.depmap` / Canonical truth.
- Unknown must never be inferred as safe.
- Change Phone: Current / Transition / After; After = Plan Projection.

## Reference precedence

When source/runtime appearance conflicts with this reference:

1. preserve Canonical/domain truth and frozen IA;
2. preserve Android platform ergonomics and accessibility;
3. translate this reference's visual hierarchy, light-first palette, asset identity, spatial depth, and product mood;
4. do **not** copy accidental generated text, counts, brands, or obsolete navigation.



## Source translation status

The selected direction has now been translated into Android Compose source across the principal visual
surfaces:

- light-first Material color system and shell;
- Globe-led Now / Overview hierarchy;
- asset-first Cards and communication-identity Numbers;
- Card / Number detail identity surfaces;
- preview-led Card / Number Studio;
- compact + wide Change Phone continuity scenes;
- lighter primary navigation and content-level Infrastructure sibling navigation;
- summary hierarchy for Accounts / Emails / Devices / Services / Weaknesses;
- supporting-surface closure for Records, Search, Personalization, Data Sources, Card Detail and Number Detail:
  task-first summaries, preview-first personalization, explicit fact-boundary visualization and stronger
  asset identity strips now share the same light tonal hierarchy rather than falling back to settings-page chrome.

This does not mean the reference is visually accepted at runtime. The current source still requires fresh
Phone + Tablet evidence and Human pixel review before Android Reference Freeze.

```
HUMAN_VISUAL_DIRECTION_REFERENCE = SELECTED
ANDROID_LIGHT_VISUAL_TRANSLATION_SOURCE = COMPLETE
ANDROID_RUNTIME_EVIDENCE_FOR_CURRENT_HEAD = REQUIRED
ANDROID_REFERENCE_FREEZE = HOLD
```


## Supporting-surface closure checkpoint

Latest reviewed Android production-UI checkpoint for this reference:

`47f6b9c6f592b21bd941092e7855433459a2723f`

This checkpoint completes the remaining source-level light-reference translation that was still visibly
utility/settings-like after the first pass:

- Records now opens with a light task summary for active changes / attention / upcoming;
- Search has a raised global-search surface and explicit recorded-data scope;
- Data Sources makes the fact boundary visible: 本机优先 / 已确认 / 未知，and explicitly preserves
  “未知绝不自动推断为安全”;
- Personalization is preview-first instead of beginning with generic preference rows;
- Card Detail and Number Detail add compact identity summaries directly under the real asset face;
- structural instrumentation contracts protect these presentation hierarchies from regression.

No runtime visual acceptance is implied. The exact current remote HEAD must be built and captured on the
existing API36 Phone and Tablet AVDs before Human Final Acceptance.

## Asset provenance correction

The repository reference path now contains a real compressed copy derived from the Human-selected 1536×1024 light concept board. It is a visual-direction reference, not a pixel golden; runtime Phone + Tablet pixels still require Human final acceptance.

## Focused Phone translation closure

The selected light board is now translated more literally at the information-hierarchy level without copying
generated data or obsolete IA:

- Phone Now remains the large Globe/world-view entry.
- Phone Infrastructure becomes an eight-category management Hub plus a smaller regional Globe, rather than a
  second large-Globe home screen.
- Phone child surfaces are focused: the wide Infrastructure sibling strip is not permanently mounted above
  Cards / Numbers / Accounts / Emails / Devices / Services / Weaknesses.
- Only Now uses the compact PDIG brand layer; other Phone pages use contextual top titles.
- Duplicate compact in-content titles were removed where the top bar already carries the task/screen title.
- Filters, projection selectors, and Studio chips retain 48dp interaction targets while rendering visually
  lighter controls, matching the density of the selected reference.
- Medium remains rail + single-pane; Expanded retains rail + content-level sibling navigation and wider
  spatial/list-detail compositions.

This remains a source-design closure, not runtime acceptance. Fresh exact-HEAD Phone + Tablet screenshots are
still required before Android Reference Freeze.
