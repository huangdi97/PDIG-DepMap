# ANDROID_UI_VNEXT_DESIGN_COMPLETION.md

> Branch: `feat/android-ui-vnext-translation`
>
> Historical source checkpoint: `a03bc11c8bac601f95bf2e070c5f666e2b22d271`.
>
> **R19 override (2026-10-09):** current branch truth supersedes the 2026-10-05 four-tab wording below where they conflict. The product now intentionally has **five** primary destinations: 现在 / 基础设施 / 变更 / 记录 / 我. See `spec/ui-vnext/FIVE_PRIMARY_NAVIGATION_DECISION.md`.
>
> Status: **ANDROID_UI_VNEXT_SOURCE_DESIGN = COMPLETE / CURRENT_HEAD_RUNTIME_RERUN_REQUIRED**
>
> This document records source/UI completeness only. It does **not** claim the latest runtime screenshots,
> instrumentation results, or Android visual freeze are valid after this source checkpoint.

## 1. Product contract preserved

- Product: Personal Digital Infrastructure Change & Continuity Management.
- Primary navigation: 现在 / 基础设施 / 变更 / 记录 / 我（五个一级目的地；`我` 不得降级）。
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
| 我       | COMPLETE              | 我的数字生活、关键身份、连续性概览、隐私/偏好与个人管理入口；一级 root。                                |

## 3. Completed infrastructure surfaces

| Surface    | Android source status | Notes                                                                                          |
| ---------- | --------------------- | ---------------------------------------------------------------------------------------------- |
| 总览       | COMPLETE              | Real-Earth Globe + region list + attention rail + quick entries; compact/expanded translation. |
| 卡片       | COMPLETE              | Adaptive card gallery/list, region filtering, issuer identity, privacy mask.                   |
| 卡片详情   | COMPLETE              | Identity hero, recorded lifecycle, confirmed dependencies, truth-bounded Impact Lens, appearance action. |
| 卡面定制   | COMPLETE              | Consumer Studio; theme/material/privacy controls persist via PresentationProfile.              |
| 号码       | COMPLETE              | High-density communication identity list; compact List→Detail; expanded list+inspector.        |
| 号码详情   | COMPLETE              | Number identity, lifecycle/keep-alive, recovery-use vs explicit-unique truth, dependencies, Impact Lens. |
| 号码面定制 | COMPLETE              | Communication-identity Studio; theme/material/privacy persist locally.                         |
| 账户       | COMPLETE              | Identity provider, masked identifier, roles, auth methods, recovery route, attention state.    |
| 邮箱       | COMPLETE              | Login/recovery roles, linked-service count, unique-recovery warning.                           |
| 设备       | COMPLETE              | Platform/type, trust state, roles, last-seen, review warning.                                  |
| 服务       | COMPLETE              | Region-scoped service inventory and consumer service-category labels.                          |
| 薄弱点     | COMPLETE              | Explicit unique-recovery findings, expiring cards, device review, phone-migration blockers; recovery use alone is not a weakness. |

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
- Five-item bottom navigation: 现在 / 基础设施 / 变更 / 记录 / 我.
- Infrastructure uses a dedicated 8-category management Hub on Phone; the persistent sibling strip is reserved for wider layouts.
- Cards default to an asset-first visual list on Phone, with a full card-face mode still available; type + region browsing stay presentation-only.
- Numbers use List→Detail rather than desktop inspector; every compact row preserves a communication-identity visual surface rather than a generic address-book row.
- Studios use Preview-first layout; compact theme selection is a horizontal visual gallery rather than a full-width settings list.
- Change Phone uses a compact six-step rail plus a shared old-number → new-number continuity scene; services remain visible inside the migration context.

### MEDIUM / EXPANDED

- Five-item primary navigation rail; infrastructure object categories stay in a content-level sibling navigation row.
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

The next local run must build and capture from the exact current remote branch HEAD. The historical `a03bc11c...` evidence is not proof for R19. Any current acceptance run must pull and record the
exact remote R19 HEAD; all pre-R19 pixel/runtime evidence is historical only.


## 12. Adaptive navigation & compact craft closure (2026-10-05)

Human pixel review of the first source-complete runtime pack showed that the implementation was functionally
complete but still carried desktop/control-panel traits into Android. The 2026-10-05 pass separated global
navigation from Infrastructure siblings. **R19 later adds `我` as an intentional fifth global destination; this
is a product decision, not a regression to the older mixed rail.**

The Android translation now freezes the following presentation hierarchy:

- **Primary navigation stays primary**: 现在 / 基础设施 / 变更 / 记录 / 我 are the five product-level
  destinations in Bottom Navigation / wide Navigation Rail. `我` owns the personal digital-life workspace.
- **Infrastructure categories are sibling destinations inside the Infrastructure context**:
  总览 / 卡片 / 号码 / 账户 / 邮箱 / 设备 / 服务 / 薄弱点 live in a scrollable content-level row on
  both compact and wide Android layouts.
- **Utilities remain low-frequency**: 数据源 / 设置 stay separated from the five primary destinations and remain under the `我` context.
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

- **Light-first Material shell**: off-white canvas with a restrained cool-blue spatial gradient, white / cool-blue raised surfaces, navy text, blue primary actions, semantic positive/warning/critical colors; Globe and asset faces intentionally retain deep local canvases for spatial depth and identity.
- **Stable Phone brand layer**: compact root screens use `PDIG` in the top bar instead of duplicating each
  page title; detail / studio / search / change contexts still show contextual titles.
- **Globe-led Now**: the Globe remains the spatial hero with infrastructure counts attached to it; attention
  and active change outrank generic statistics.
- **Asset-first Cards**: Phone defaults to a high-density visual list with real card thumbnails; card-face
  mode remains available; browsing now supports card type and region without mutating data.
- **Asset-first Numbers**: Phone list rows now render communication-identity thumbnails with visible region identity; wide inspector uses the real NumberFace rather than plain text. Saved PresentationProfile is reused by list/detail/studio.
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


## 14. Light-reference supporting-surface closure (2026-10-05)

The second Human-reference pass closed the remaining surfaces that still read as generic settings or
engineering utility pages after the first light translation.

Direct Android source changes now include:

- **Records**: task summary strip for active change / attention / upcoming before the detailed timeline.
- **Search**: product-level global search statement plus raised tonal search surface.
- **Data Sources**: explicit fact-boundary hero for 本机优先 / 已确认 / 未知; Unknown remains Unknown.
- **Personalization**: visual-direction preview first; generic setting rows no longer define the first impression.
- **Card Detail**: real card face remains hero, followed by a compact identity summary for region / currency /
  bound-service count before factual metadata.
- **Number Detail**: communication identity remains hero, followed by role / SIM form / linked-service /
  recovery summary before dependency details.
- **Regression protection**: AndroidLightVisualSourceContractTest now asserts these supporting visual
  hierarchies in addition to Cards / Numbers / Change / light-theme contracts.

The resulting Android source is now treated as **source-design complete for the selected light reference**.
No further aesthetic changes should be invented from source alone. The next design decision must come from
fresh current-head Phone + Tablet runtime pixels.

Current gate remains:

```
ANDROID_UI_VNEXT_SOURCE_DESIGN = COMPLETE
ANDROID_LIGHT_VISUAL_TRANSLATION_SOURCE = COMPLETE
ANDROID_RUNTIME_EVIDENCE_FOR_CURRENT_HEAD = REQUIRED
ANDROID_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_ACCEPTANCE
ANDROID_REFERENCE_FREEZE = HOLD
IOS_UI_VNEXT = HOLD
HARMONY_UI_VNEXT = HOLD
```

## 15. Focused Phone information architecture & density closure (2026-10-05)

A further source review against the Human-selected light reference found that the first light translation still
carried too much wide-screen chrome into Phone:

- Now and Infrastructure Overview both presented a large Globe as the first dominant surface;
- the eight Infrastructure sibling destinations remained permanently mounted above every Phone child screen;
- compact root screens kept the PDIG brand title even when the user was already working inside Cards,
  Numbers, Records, Change or another focused surface;
- filter / projection / Studio chips used their 48dp accessibility touch target as their full visual height,
  making the light UI read heavier than the selected reference.

The Android source now closes those issues:

1. **Now = world view.** The large Globe remains the product/world-view hero and the home for current attention.
2. **Infrastructure = management hub.** Phone Overview now opens with an eight-category object hub
   (总览 / 卡片 / 号码 / 账户 / 邮箱 / 设备 / 服务 / 薄弱点), followed by a smaller regional Globe and
   Region/Attention context. It is intentionally not a second copy of Now.
3. **Focused child screens.** Phone no longer mounts the persistent Infrastructure sibling strip above
   Cards / Numbers / Accounts / Emails / Devices / Services / Weaknesses. Wider layouts retain the content-level
   sibling navigation row.
4. **Contextual compact top bar.** Only Now carries the PDIG brand layer. Other Phone surfaces use the current
   screen/task title; duplicate in-content titles were removed on compact layouts.
5. **Touch target != visual pill.** Cards/Numbers filters, Change projection controls, and Studio chips preserve
   the Android 48dp interactive target while rendering a visually lighter inner control.
6. **Adaptive truth remains unchanged.** Compact uses bottom navigation and focused single-pane flows; Medium
   uses rail + single-pane content; Expanded uses rail + wider list/detail or spatial compositions.

This closure changes presentation only. It does not modify Canonical, PersonalReality, `.depmap`,
PresentationProfile persistence semantics, Unknown handling, Change Phone truth semantics, or the frozen
Desktop reference.

Regression contracts now guard:

- Phone Overview management Hub presence;
- absence of the wide Infrastructure sibling strip on compact layouts;
- Now Globe remaining visually larger than the compact Overview regional Globe;
- PDIG branding on Now and contextual top-title behavior on focused Phone pages;
- existing Cards/Numbers/Studio/Change/adaptive/accessibility contracts.

Current gate remains:

```
ANDROID_UI_VNEXT_SOURCE_DESIGN = COMPLETE
ANDROID_LIGHT_VISUAL_TRANSLATION_SOURCE = COMPLETE
ANDROID_RUNTIME_EVIDENCE_FOR_CURRENT_HEAD = REQUIRED
ANDROID_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_ACCEPTANCE
ANDROID_REFERENCE_FREEZE = HOLD
IOS_UI_VNEXT = HOLD
HARMONY_UI_VNEXT = HOLD
```

The next local execution run must capture the exact current remote HEAD (or a later documented production-UI
checkpoint). Any further Android production-UI commit invalidates prior runtime acceptance evidence.
