# PDIG Android Light Visual Reference

This file is the Human-selected visual-direction reference for Android UI vNext.

- Reference asset: `PDIG_ANDROID_LIGHT_VISUAL_REFERENCE_2026-10-05.jpg`
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
- summary hierarchy for Accounts / Emails / Devices / Services / Weaknesses.

This does not mean the reference is visually accepted at runtime. The current source still requires fresh
Phone + Tablet evidence and Human pixel review before Android Reference Freeze.

```
HUMAN_VISUAL_DIRECTION_REFERENCE = SELECTED
ANDROID_LIGHT_VISUAL_TRANSLATION_SOURCE = COMPLETE
ANDROID_RUNTIME_EVIDENCE_FOR_CURRENT_HEAD = REQUIRED
ANDROID_REFERENCE_FREEZE = HOLD
```
