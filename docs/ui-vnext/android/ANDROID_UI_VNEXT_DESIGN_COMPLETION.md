# ANDROID_UI_VNEXT_DESIGN_COMPLETION.md

> Branch: `feat/android-ui-vnext-translation`
>
> Source-design checkpoint: `35c1103d39026d6a7c68f0ee9755a3c9777279ba`
>
> Status: **ANDROID_UI_VNEXT_SOURCE_DESIGN = COMPLETE / READY_FOR_LOCAL_RUNTIME_VALIDATION**
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

| Surface | Android source status | Notes |
| --- | --- | --- |
| 现在 | COMPLETE | Globe hero, attention, active change, upcoming; upcoming visibility is a persisted workspace preference. |
| 基础设施 | COMPLETE | Routes to Overview and all eight secondary categories. |
| 变更 | COMPLETE | Change Phone flagship with Current / Transition / After. |
| 记录 | COMPLETE | Active change card + migration timeline + attention + upcoming records. |

## 3. Completed infrastructure surfaces

| Surface | Android source status | Notes |
| --- | --- | --- |
| 总览 | COMPLETE | Real-Earth Globe + region list + attention rail + quick entries; compact/expanded translation. |
| 卡片 | COMPLETE | Adaptive card gallery/list, region filtering, issuer identity, privacy mask. |
| 卡片详情 | COMPLETE | Identity hero, metadata, bound services, risk, replacement guidance, history. |
| 卡面定制 | COMPLETE | Consumer Studio; theme/material/privacy controls persist via PresentationProfile. |
| 号码 | COMPLETE | High-density communication identity list; compact List→Detail; expanded list+inspector. |
| 号码详情 | COMPLETE | Number identity, service dependencies, recovery risk, alternate route, history. |
| 号码面定制 | COMPLETE | Communication-identity Studio; theme/material/privacy persist locally. |
| 账户 | COMPLETE | Identity provider, masked identifier, roles, auth methods, recovery route, attention state. |
| 邮箱 | COMPLETE | Login/recovery roles, linked-service count, unique-recovery warning. |
| 设备 | COMPLETE | Platform/type, trust state, roles, last-seen, review warning. |
| 服务 | COMPLETE | Region-scoped service inventory and consumer service-category labels. |
| 薄弱点 | COMPLETE | Recovery-only numbers/emails, expiring cards, device review, phone-migration blocker. |

## 4. Completed utility surfaces

| Surface | Android source status | Notes |
| --- | --- | --- |
| 搜索与快捷操作 | COMPLETE | Empty-query commands; card/number/service/region/page search; result navigation returns to Search. |
| 设置 / 个性化 | COMPLETE | Persisted privacy mask, reduce motion, rail state, upcoming visibility; links to source coverage and asset appearance. |
| 数据源 | COMPLETE | Workspace coverage, object counts, fact-boundary explanation, direct navigation to each infrastructure family. |
| 地区抽屉 | COMPLETE | Region context + all/cards/numbers actions + global reset. |

## 5. Adaptive design complete

### COMPACT
- Top command bar.
- Four-item bottom navigation.
- Scrollable infrastructure secondary navigation with selected-item auto-centering.
- Card gallery uses readable full-width presentation.
- Numbers use List→Detail rather than desktop inspector.
- Studios use Preview-first vertical layout.
- Change Phone uses compact stepper + vertical continuity flow.

### MEDIUM / EXPANDED
- Navigation rail.
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

After source checkpoint `35c1103d39026d6a7c68f0ee9755a3c9777279ba`, previous Android runtime evidence is historical.

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
