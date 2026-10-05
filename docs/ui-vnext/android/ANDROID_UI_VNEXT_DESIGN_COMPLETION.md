# ANDROID_UI_VNEXT_DESIGN_COMPLETION.md

> Branch: `feat/android-ui-vnext-translation`
>
> Android production-UI source checkpoint: `c753a91c33e7eeea544240525174299234cfb1c8` (Human-selected light reference translation + asset-first Android craft closure)
>
> Status: **ANDROID_UI_VNEXT_SOURCE_DESIGN = COMPLETE / CURRENT_HEAD_RUNTIME_RERUN_REQUIRED**
>
> This document records source/UI completeness only. It does **not** claim the latest runtime screenshots,
> instrumentation results, or Android visual freeze are valid after this source checkpoint.

## 1. Product contract preserved

- Product: Personal Digital Infrastructure Change & Continuity Management.
- Primary navigation: 现在 / 基础设施 / 变更 / 记录.
- Infrastructure secondary: 总览 / 卡片 / 号码 / 账户 / 邮箱 / 设备 / 服务 / 薄弱点.
- Globe remains the Global Infrastructure Navigator.
- PresentationProfile remains presentation-only and never mutates PersonalReality / Canonical.
- Change Phone remains Current / Transition / After, with After = plan projection rather than confirmed reality.
- Unknown remains unknown; missing records are never converted into “safe”.

## 2. Completed primary surfaces

| Surface  | Android source status | Notes                                                                                                    |
| -------- | --------------------- | -------------------------------------------------------------------------------------------------------- |
| 现在     | COMPLETE              | Globe hero, attention, active change, upcoming; upcoming visibility is a persisted workspace preference. |
| 基础设施 | COMPLETE              | Routes to Overview and all eight secondary categories.                                                   |
| 变更     | COMPLETE              | Change Phone flagship with Current / Transition / After.                                                 |
| 记录     | COMPLETE              | Active change card + migration timeline + attention + upcoming records.                                  |

## 3. Completed infrastructure surfaces

| Surface    | Android source status | Notes                                                                                          |
| ---------- | --------------------- | ---------------------------------------------------------------------------------------------- |
| 总览       | COMPLETE              | Real-Earth Globe + region list + attention rail + quick entries; compact/expanded translation. |
| 卡片       | COMPLETE              | Adaptive card gallery/list, region filtering, issuer identity, privacy mask.                   |
| 卡片详情   | COMPLETE              | Identity hero, metadata, bound services, risk, replacement guidance, history.                  |
| 卡面定制   | COMPLETE              | Consumer Studio; theme/material/privacy controls persist via PresentationProfile.              |
| 号码       | COMPLETE              | High-density communication identity list; compact List→Detail; expanded list+inspector.        |
| 号码详情   | COMPLETE              | Number identity, service dependencies, recovery risk, alternate route, history.                |
| 号码面定制 | COMPLETE              | Communication-identity Studio; theme/material/privacy persist locally.                         |
| 账户       | COMPLETE              | Identity provider, masked identifier, roles, auth methods, recovery route, attention state.    |
| 邮箱       | COMPLETE              | Login/recovery roles, linked-service count, unique-recovery warning.                           |
| 设备       | COMPLETE              | Platform/type, trust state, roles, last-seen, review warning.                                  |
| 服务       | COMPLETE              | Region-scoped service inventory and consumer service-category labels.                          |
| 薄弱点     | COMPLETE              | Recovery-only numbers/emails, expiring cards, device review, phone-migration blocker.          |

## 4. Completed utility surfaces

| Surface        | Android source status | Notes                                                                                                                  |
| -------------- | --------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| 搜索与快捷操作 | COMPLETE              | Empty-query commands; card/number/service/region/page search; result navigation returns to Search.                     |
| 设置 / 个性化  | COMPLETE              | Persisted privacy mask, reduce motion, rail state, upcoming visibility; links to source coverage and asset appearance. |
| 数据源         | COMPLETE              | Workspace coverage, object counts, fact-boundary explanation, direct navigation to each infrastructure family.         |
| 地区抽屉       | COMPLETE              | Region context + all/cards/numbers actions + global reset.                                                             |

## 5. Adaptive design complete

### COMPACT

- Top command bar.
- Four-item bottom navigation.
- Scrollable, visually lightweight infrastructure sibling navigation with selected-item auto-reveal.
- Cards default to an asset-first visual list on Phone, with a full card-face mode still available; type + region browsing stay presentation-only.
- Numbers use List→Detail rather than desktop inspector; every compact row preserves a communication-identity visual surface rather than a generic address-book row.
- Studios use Preview-first layout; compact theme selection is a horizontal visual gallery rather than a full-width settings list.
- Change Phone uses a compact six-step rail plus a shared old-number → new-number continuity scene; services remain visible inside the migration context.

### MEDIUM / EXPANDED

- Primary navigation rail only; infrastructure object categories stay in a content-level sibling navigation row.
- Overview Globe + activity rail.
- Card/number list-detail where appropriate.
- Consumer Studio multi-column layout.
- Expanded continuity scene with OLD / SERVICES / NEW in one visible composition.

## 6. PresentationProfile behavior complete

Presentation customization is no longer screenshot-only.

Saved card/number appearance now:

1. stays presentation-only;
2. is persisted locally through the presentation-profile store;
3. is reused by Grid / Detail / Studio preview;
4. supports actual theme/material rendering;
5. does not modify issuer/carrier/domain identity;
6. does not write to `.depmap`.

Workspace preferences are also persisted locally:

- privacy mask;
- reduce motion;
- rail expanded state;
- home upcoming visibility.

## 7. Consumer-language closure

The current source removes consumer-visible internal review/engineering terminology such as:

- vNext;
- fixture;
- Presentation layer;
- bundled / procedural;
- route IDs;
- internal preset IDs;
- “freeze” / “reference” review terminology.

Region codes remain internal identity; consumer surfaces render region names.

## 8. Component/code-structure closure

Large UI files were split by responsibility:

- Navigation config/selection → `NavigationModel.kt`;
- Empty states → `EmptyState.kt`;
- Number identity surface separated from generic asset surfaces;
- Studio layout separated from screen state;
- Search catalog/logic separated from Search UI;
- Overview quick actions/helpers separated from Globe/rail composition.

Frozen Globe rendering logic is intentionally not redesigned.

## 9. What is not allowed to be claimed yet

The 2026-10-04 runtime pack was captured from source `4e43511ae9754413ffedeaca9ad21a71b330aa64`.
Human Review of that pack triggered additional Android-only source/test corrections through
`77c7b3692c9b3327331f52af522a2bc65ecf165e`. Therefore that pack is historical for Freeze purposes,
even though it remains valid evidence of the older source state.

Until the local toolchain reruns against this exact or later source head, do not claim:

- `ANDROID_VISUAL_REFERENCE = ACCEPTED`;
- `ANDROID_REFERENCE_FREEZE = PASS`;
- latest phone/tablet screenshot parity;
- latest instrumentation PASS;
- latest emulator visual acceptance.

Correct current state:

```
ANDROID_UI_VNEXT_SOURCE_DESIGN = COMPLETE
ANDROID_UI_VNEXT_RUNTIME_VALIDATION = REQUIRED
ANDROID_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_ACCEPTANCE
ANDROID_REFERENCE_FREEZE = HOLD
IOS_UI_VNEXT = HOLD
HARMONY_UI_VNEXT = HOLD
```

## 10. Local-agent handoff boundary

The local Agent is now an execution/verification agent, not a UI designer.

It should only:

1. pull the latest `feat/android-ui-vnext-translation`;
2. compile;
3. run Android unit/instrumentation/a11y/freeze guards;
4. run the existing API36 Phone and Tablet AVDs;
5. regenerate Phone + Tablet runtime screenshots;
6. write layout/runtime manifests;
7. push evidence without redesigning or “improving” UI.

Any visual discrepancy found in runtime screenshots should be reported back for source/UI correction before Android freeze.


## 11. Post-runtime Human Review closure (2026-10-05)

The first source-complete Phone/Tablet runtime pack was reviewed from actual PNG pixels rather than by
accepting manifest PASS mechanically. The review found a small set of issues that were safe to close
directly in Android source without changing Canonical or the frozen Desktop reference:

- compact infrastructure navigation received a complete-chip auto-reveal fix at `67d0ebd`;
- Region Detail now behaves as a detail layer: system Back closes the detail while preserving the selected
  region; only the explicit global-reset action clears region context;
- the compact Region Detail surface is now adapted as a bottom-centered phone surface rather than reusing
  the wide bottom-end geometry;
- Wide Overview no longer lets the main Globe/activity row consume all vertical space; the frozen quick-entry
  row is now reserved and the next runtime pack must prove it is laid out;
- Change Phone keeps Current / Transition / After semantics, but consumer copy now describes After as
  `完成后（计划）` / `完成后预览` and explicitly states that projection is not completion or verification;
- compact Card/Number Studio density was tightened without changing the Desktop/wide composition;
- two stale accessibility string expectations were updated to the current consumer copy;
- the legacy Phone screenshot geometry probe no longer pretends that a forced responsive breakpoint enlarges
  the physical AVD window;
- source-complete Globe screenshots now require `TEXTURE_READY` before capture and record
  `globeTextureState` in evidence;
- Tablet Overview evidence now asserts that `pdig.overview.quick` has non-zero runtime geometry.

These are source and evidence-contract corrections only. They do not authorize Android Freeze.

Current gate:

```
ANDROID_UI_VNEXT_SOURCE_DESIGN = COMPLETE
ANDROID_RUNTIME_EVIDENCE_FOR_CURRENT_HEAD = REQUIRED
ANDROID_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_ACCEPTANCE
ANDROID_REFERENCE_FREEZE = HOLD
IOS_UI_VNEXT = HOLD
HARMONY_UI_VNEXT = HOLD
```

The next local run must build and capture from the exact current remote branch HEAD. The latest Android
production-UI checkpoint is `77c7b369...`; any later Android production-UI commit invalidates the evidence
and requires regeneration. Documentation-only descendants do not change pixels, but the execution Agent
should still pull and record the exact remote HEAD it actually validates.


## 12. Adaptive navigation & compact craft closure (2026-10-05)

Human pixel review of the first source-complete runtime pack showed that the implementation was functionally
complete but still carried two desktop/control-panel traits into Android: the Tablet rail mixed four global
destinations with eight infrastructure object categories, and the Phone Studio rendered theme choices as a
long vertical settings list.

The Android translation now freezes the following presentation hierarchy:

- **Primary navigation stays primary**: 现在 / 基础设施 / 变更 / 记录 remain the only product-level
  destinations in Bottom Navigation / wide Navigation Rail.
- **Infrastructure categories are sibling destinations inside the Infrastructure context**:
  总览 / 卡片 / 号码 / 账户 / 邮箱 / 设备 / 服务 / 薄弱点 live in a scrollable content-level row on
  both compact and wide Android layouts.
- **Utilities remain low-frequency**: 数据源 / 设置 stay separated from the four primary destinations.
- **Compact top chrome is quieter**: search/settings/back retain 48dp interaction geometry without reading
  as three competing filled dashboard tiles.
- **Compact Studio is asset-first**: the live asset preview remains first; theme choices are a horizontal
  visual gallery; material/layout/accent/privacy controls remain secondary inspectors.
- **Wide Studio remains unchanged**: the Desktop-derived library / preview / inspector composition is still
  appropriate for wide Android windows and is not pixel-copied into phone UI.

The hierarchy is protected by Android instrumentation contracts, including the Tablet primary-rail +
content-secondary assertion and the Phone horizontal Studio-gallery assertion.

Research rationale: this follows Android's adaptive navigation model (compact bottom navigation, larger
windows using a navigation rail) and the Android navigation guidance that treats rail destinations as
top-level while sibling destinations belong in secondary navigation patterns. Reference material:

- https://developer.android.com/develop/ui/compose/layouts/adaptive
- https://developer.android.com/develop/ui/compose/layouts/adaptive/build-adaptive-navigation
- https://developer.android.com/design/ui/mobile/guides/layout-and-content/navigation

This is a presentation-only correction. It does not change Canonical, PersonalReality, Change Phone truth
semantics, PresentationProfile persistence boundaries, the frozen Desktop reference, or `.depmap`.


## 13. Human-selected Light Visual Reference translation closure (2026-10-05)

Human selected the Android light visual direction stored at:

- `spec/ui-vnext/references/android/PDIG_ANDROID_LIGHT_VISUAL_REFERENCE_2026-10-05.jpg`
- `spec/ui-vnext/references/android/PDIG_ANDROID_LIGHT_VISUAL_REFERENCE_2026-10-05.md`

The reference is binding for Android presentation hierarchy and mood, but generated brands, counts, old IA
fragments, and accidental copy are not functional truth.

The Android source translation now includes:

- **Light-first Material shell**: off-white canvas, white / cool-blue raised surfaces, navy text, blue primary
  actions, semantic positive/warning/critical colors; Globe and asset faces intentionally retain deep local
  canvases for spatial depth and identity.
- **Stable Phone brand layer**: compact root screens use `PDIG` in the top bar instead of duplicating each
  page title; detail / studio / search / change contexts still show contextual titles.
- **Globe-led Now**: the Globe remains the spatial hero with infrastructure counts attached to it; attention
  and active change outrank generic statistics.
- **Asset-first Cards**: Phone defaults to a high-density visual list with real card thumbnails; card-face
  mode remains available; browsing now supports card type and region without mutating data.
- **Asset-first Numbers**: Phone list rows now render communication-identity thumbnails; wide inspector uses
  the real NumberFace rather than plain text. Saved PresentationProfile is reused by list/detail/studio.
- **Continuity choreography**: compact Change Phone now places old and target number identities in one
  migration scene with critical services beneath; Tablet keeps the three-column OLD / SERVICES / NEW scene.
  Current / Transition / After truth semantics are unchanged, and After remains Plan Projection.
- **Preview-led Studios**: live preview first, horizontal theme gallery on compact, then material / layout /
  accent / display/privacy controls; current-state properties are a summary rather than the first interaction.
- **Secondary infrastructure hierarchy**: Accounts / Emails / Devices / Services / Weaknesses receive
  light-first summary heroes before detailed rows; Unknown remains explicit and is never turned into a safe
  summary.
- **Quieter navigation**: only the selected wide rail destination receives a strong surface; unselected
  primary destinations stay visually quiet. Infrastructure categories remain content-level sibling
  navigation.
- **Regression guards**: compact Cards default mode and full-face geometry remain tested; compact Numbers now
  has a contract requiring communication-identity thumbnails; prior adaptive/Studio/a11y contracts remain.

### Runtime truth after this closure

The latest old runtime pack still comes from `4e43511...` and predates this light-first source translation.
Therefore it is not visual acceptance evidence for the current source.

```
ANDROID_UI_VNEXT_SOURCE_DESIGN = COMPLETE
ANDROID_LIGHT_VISUAL_TRANSLATION_SOURCE = COMPLETE
ANDROID_RUNTIME_EVIDENCE_FOR_CURRENT_HEAD = REQUIRED
ANDROID_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_ACCEPTANCE
ANDROID_REFERENCE_FREEZE = HOLD
IOS_UI_VNEXT = HOLD
HARMONY_UI_VNEXT = HOLD
```

No build, instrumentation, or current-head runtime PASS is claimed by this source-design closure.
