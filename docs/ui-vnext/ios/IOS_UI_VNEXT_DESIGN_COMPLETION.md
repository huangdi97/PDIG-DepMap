# IOS_UI_VNEXT_DESIGN_COMPLETION.md

> Branch: `feat/ios-ui-vnext-translation`
>
> Scope: iPhone + iPadOS native SwiftUI translation of PDIG UI vNext.
>
> Status: **IOS_UI_VNEXT_SOURCE_DESIGN = COMPLETE**
>
> This file records source-design completeness. Runtime truth is owned by the exact-head GitHub Actions
> `iOS` and `iOS Runtime Visual` runs; this document does not turn stale screenshots into current evidence.

## 1. Product truth preserved

The iOS translation preserves the PDIG product model instead of rebuilding a generic dashboard:

- four stable primary destinations: **现在 / 基础设施 / 变更 / 记录**;
- eight infrastructure siblings: **总览 / 卡片 / 号码 / 账户 / 邮箱 / 设备 / 服务 / 薄弱点**;
- Globe remains the global infrastructure navigator and spatial identity surface;
- PresentationProfile remains local presentation state only and never mutates PersonalReality / Canonical / `.depmap`;
- Change Phone remains a make-before-break continuity workflow;
- **当前 / 迁移中 / 完成后（计划）** are visually and semantically distinct;
- missing information remains **Unknown**, never silently converted into “safe”.

## 2. Human-selected visual direction translated

The accepted mobile direction is **light-first**, not a pixel copy of Desktop Dark.

iOS therefore uses:

- off-white / cool-blue workspace canvas;
- white and cool-blue raised content surfaces;
- restrained brand blue for selected navigation, primary actions and focused identity;
- deep local canvases for Globe, card faces and number identity surfaces;
- real bundled Earth albedo / night-light / cloud material for the Globe;
- semantic positive / warning / critical states with icon + label + color rather than color alone;
- system-native SwiftUI navigation, sheets, toolbar, segmented controls and switches.

The light-first direction is intentional. Dark remains localized to identity/spatial content instead of turning
the entire application into a desktop-style dark control panel.

## 3. iPhone information architecture complete

### 现在

- task-first home;
- greeting + attention count;
- large Globe hero with real Earth material;
- infrastructure counts attached to the Globe instead of a generic statistics dashboard;
- Need Attention → Active Changes → Upcoming;
- “未记录 ≠ 无风险” boundary remains explicit;
- four-item bottom navigation remains stable.

### 基础设施

Compact iPhone uses a dedicated management Hub instead of repeating the full Home:

- 8-category object grid;
- smaller regional Globe;
- attention context;
- focused child pages do **not** keep a desktop-style sibling rail permanently mounted.

### Cards / Numbers

- card collection uses visual card identity, not generic database rows;
- number collection uses communication-identity thumbnails / faces;
- card and number detail pages keep the owned asset surface first;
- dependencies, risk, recovery and history follow the identity surface;
- region browsing is presentation-only.

### Change Phone

- Current / Transition / After segmented projection;
- old number → services/accounts → new number continuity scene;
- six-stage rail;
- stage 6 stays blocked until the new route is verified;
- After remains a **plan projection**, not fake completed reality.

### Records / Search / Data Sources / Personalization

- Records is task/timeline-oriented;
- Search is global search + quick navigation over recorded objects only;
- Data Sources explains **local-first / confirmed / unknown** fact boundaries;
- Personalization is consumer-facing and keeps P0 tasks non-hideable.

## 4. iPadOS adaptive design complete

iPad is a management workspace rather than a stretched phone:

- `NavigationSplitView` is the root adaptive shell;
- the four primary destinations remain directly visible in the sidebar;
- infrastructure siblings are content-level navigation, not mixed into the primary hierarchy;
- Now uses Globe + task rail side-by-side;
- Overview uses expanded Globe + Region List;
- Cards use gallery + persistent inspector;
- Numbers use list + persistent inspector;
- detail pages split identity from dependency/risk content;
- Studio uses theme library / live preview / inspector;
- Change Phone keeps OLD / SERVICES / NEW visible in one wider composition.

This follows the product's four-level hierarchy while using iPad space for simultaneity rather than simply
increasing card widths.

## 5. Presentation customization complete

Card and number appearance is functional, not screenshot-only.

Saved local PresentationProfile now controls:

- theme;
- material;
- accent;
- layout;
- per-object sensitive-field masking.

Profiles are:

1. previewed live in Studio;
2. persisted locally;
3. reused by collection/detail/Studio surfaces;
4. isolated from domain identity and dependencies;
5. excluded from `.depmap`.

Workspace preferences persist separately:

- privacy masking;
- reduced motion;
- Upcoming visibility.

## 6. Accessibility / native behavior complete

The iOS translation deliberately favors platform behavior where it conflicts with desktop pixel metrics:

- semantic SwiftUI text styles support Dynamic Type;
- touch targets remain at least the iOS 44pt contract;
- system Reduce Motion is observed in addition to the PDIG-local preference;
- Globe idle motion is disabled when effective Reduce Motion is active;
- search supports **Command-K** for hardware keyboards;
- status remains icon + label + color;
- Region List provides a non-spatial alternative to Globe interaction;
- primary navigation remains stable and does not disappear based on data availability.

Relevant Apple guidance used for the platform translation:

- Tab bars are for top-level sections and should stay stable;
- complex iPad information architecture can use a sidebar / navigation split view;
- Dynamic Type should remain usable at larger text sizes;
- Reduce Motion should alter depth/motion effects when the system preference requests it.

## 7. Real-Earth Globe closure

The iOS Globe now uses the repository's bundled shared Earth material rather than a generic blue sphere:

- `earth_albedo_2048.png`;
- `earth_night_lights_2048.png`;
- `cloud_2048.png`.

The renderer keeps:

- deterministic camera state for evidence;
- fallback rendering if the material cannot be loaded;
- region anchors;
- cross-region arcs driven only by recorded relationships;
- depth-aware interaction;
- local dark spatial canvas in the light-first shell.

## 8. Consumer-language closure

Consumer-visible iOS surfaces avoid internal implementation vocabulary such as:

- vNext;
- fixture;
- Presentation Layer;
- bundled/procedural implementation notes;
- route IDs;
- internal preset IDs;
- freeze/reference-review terminology.

Region names are rendered for people; internal region codes remain implementation identity.

## 9. Test / evidence architecture

The branch contains three distinct evidence layers:

1. **Swift build + unit/conformance**
   - SwiftPM build;
   - canonical 128/128;
   - VNextModel navigation/preferences/presentation tests;
   - bundled Earth material tests.

2. **macOS SwiftUI harness**
   - useful as a compile/render smoke layer only;
   - explicitly not treated as simulator/device visual acceptance.

3. **iPhone + iPad simulator XCUITest**
   - launches `--uitest-vnext`;
   - checks primary navigation and utilities;
   - captures runtime screenshots into xcresult;
   - is the authoritative automated runtime evidence for this source branch.

Runtime status must always be read against the **exact current HEAD**. A green older SHA is historical only.

## 10. Source-design gate

At this source checkpoint:

```
IOS_UI_VNEXT_SOURCE_DESIGN = COMPLETE
IOS_LIGHT_FIRST_TRANSLATION_SOURCE = COMPLETE
IOS_ADAPTIVE_IPHONE_IPAD_SOURCE = COMPLETE
IOS_PRESENTATION_PROFILE_SOURCE = COMPLETE
IOS_GLOBE_REAL_EARTH_SOURCE = COMPLETE
IOS_RUNTIME_VALIDATION = EXACT_HEAD_ACTIONS_REQUIRED
IOS_VISUAL_ACCEPTANCE = RUNTIME_PIXELS_REQUIRED
```

No signing, TestFlight, App Store, true-device Keychain/LocalAuthentication, or SQLCipher-at-rest claim is made
by this UI completion document.
