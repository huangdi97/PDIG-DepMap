# ANDROID_UI_VNEXT_HUMAN_RUNTIME_REVIEW_2026-10-05

> Repo: `huangdi97/PDIG-DepMap`
>
> Branch: `feat/android-ui-vnext-translation`
>
> Runtime pack reviewed:
> `artifacts/runtime-evidence/2026-10-04-android-ui-vnext-source-complete-validation/`
>
> Runtime pack source: `4e43511ae9754413ffedeaca9ad21a71b330aa64`
>
> Post-review Android source checkpoint: `c0e5f34e721f22b7765aaafc2a2d374a65e5191e`
>
> Verdict: **NOT FROZEN — FRESH CURRENT-HEAD RUNTIME EVIDENCE REQUIRED**

## 1. Review method

This review did not accept Agent PASS labels as visual acceptance. The Phone 24-screen pack and key Tablet
screens were inspected from the committed PNG pixels, then cross-checked against the Compose source,
instrumentation contracts, manifests, and the recorded source SHA.

The existing 2026-10-04 evidence is useful and internally traceable, but it was generated from
`4e43511...`. The branch subsequently changed, first through runtime follow-up and then through this Human
Review closure. It therefore cannot be used as final evidence for the current Android reference.

## 2. What the runtime pack established

The reviewed pack established that the Android translation had reached a coherent product surface rather than
the earlier prototype state:

- Now, Overview, Cards, Numbers, secondary infrastructure, Change Phone, Records, Search,
  Personalization, and Data Sources all rendered as real Compose runtime surfaces;
- Phone and Tablet showed genuinely adaptive compositions rather than a single stretched layout;
- Card/Number PresentationProfile rendering and the major continuity semantics were visible;
- Number Detail no longer showed the earlier dead-space/composition failure;
- secondary infrastructure pages were no longer placeholders;
- consumer-facing copy was substantially cleaner than the prior engineering-oriented iterations.

This is not equivalent to Freeze acceptance.

## 3. Human Review findings that required source correction

### 3.1 Compact infrastructure navigation

Late infrastructure destinations needed reliable full-chip reveal. The branch already received the
`LazyRow` auto-reveal correction at `67d0ebd`; this still needs current-head runtime confirmation.

### 3.2 Region context hierarchy

System Back from Region Detail previously cleared the region and jumped to Global. That collapsed two
different user intents: “close this detail” and “leave this regional context.” Current source now preserves
the selected region on Back; the explicit `返回全球视图` action remains the only reset.

Phone Region Detail also now uses compact bottom-centered geometry, while wide layouts keep the bottom-end
surface.

### 3.3 Wide Overview vertical composition

The main Wide Overview row used `fillMaxSize()` inside a Column that also contained the frozen Quick Entry
row. That could consume the available height and make the bottom quick actions non-authoritative or clipped.
The row now uses weighted remaining height, and the screenshot contract asserts non-zero runtime geometry for
the Tablet quick-entry surface.

### 3.4 Change Phone truth language

The underlying semantic model remains Current / Transition / After, with After = Plan Projection. The old
consumer label `计划完成（投影）` was too easy to read as an accomplished state. Current copy uses
`完成后（计划）` / `完成后预览` and explicitly says the projection is not completion or verification.

### 3.5 Compact Studio density

Phone Card/Number Studio was functionally correct but spent too much of the viewport on title/stage spacing.
Compact-only padding, title scale, and preview inset were tightened. Wide/Desktop-oriented proportions were
not changed.

### 3.6 Accessibility contract drift

Two failures in the old report were stale expectations rather than lost accessibility:
`搜索 / 命令` vs current `搜索与快捷操作`, and the old internal label
`地区（Region List）` vs consumer `地区`. The contracts now assert the current accessible copy.

### 3.7 Invalid forced-wide Phone geometry probe

The legacy screenshot test selected the wide responsive branch with `forcedViewportWidthDp = 1280` while
still running in a physically narrow Phone AVD window, then required wide content geometry to be non-zero.
The forced value changes branch selection; it does not enlarge the actual window. The corrected test uses the
forced value only to validate the rail branch and validates content geometry at the actual viewport. Expanded
runtime geometry remains the Tablet suite's responsibility.

### 3.8 Globe evidence readiness

The old source-complete screenshot suite waited a fixed time before screens 01–04 but did not require the
Globe to reach `TEXTURE_READY`. A separate Globe contract being green did not guarantee that the Human
Review screenshots themselves represented the texture-ready frame. Current evidence code requires
`TEXTURE_READY`, records `globeTextureState`, and fails instead of capturing an early frame.

## 4. Direct GitHub commits in this Human Review closure

- `67d0ebd` — compact navigation reveal + compact Change Phone flow polish (pre-review latest source)
- `1ac46f6` — Region context/back hierarchy + adaptive Region Detail + Wide Overview sizing
- `abc8b03` — Change Phone consumer truth copy + compact Studio density
- `37803b1` — accessibility/interaction/screenshot contract corrections
- `399fed1` — compact-only Studio density correction
- `c0e5f34` — texture-ready Globe Human Review evidence + Tablet Overview quick-entry probe

No commit in this sequence changes the frozen Desktop reference, Canonical semantics, PersonalReality, or
`.depmap`.

## 5. Current gate

```
ANDROID_UI_VNEXT_SOURCE_DESIGN = COMPLETE
ANDROID_RUNTIME_EVIDENCE_FOR_CURRENT_HEAD = REQUIRED
ANDROID_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_ACCEPTANCE
ANDROID_REFERENCE_FREEZE = HOLD

IOS_UI_VNEXT = HOLD
HARMONY_UI_VNEXT = HOLD
```

This is intentionally stricter than “build passes” or “screenshots exist.”

## 6. Exact acceptance pack required next

The local execution Agent may only build/run/test/capture evidence from the exact current remote head. It must
not redesign UI.

At minimum the fresh pack must contain:

- Phone 24-screen Human Review set;
- Tablet 24-screen Human Review set;
- the 10 empty states;
- Card PresentationProfile save/apply + force-stop/restart persistence;
- Number PresentationProfile save/apply + force-stop/restart persistence;
- workspace preference persistence;
- Search → result → Back → Search;
- Region select → Region Detail → system Back preserving selected region → explicit Global reset;
- Cards/Numbers region context propagation;
- Globe `TEXTURE_READY` for the Human Review Globe frames and manifest `globeTextureState`;
- Tablet Overview `pdig.overview.quick` non-zero layout geometry;
- Number Detail layout probe;
- Change Current / Transition / After screenshots with After visibly remaining a plan projection;
- system Back and accessibility suites;
- consumer-copy audit;
- Desktop Freeze Guard unchanged.

## 7. Freeze rule

Android Final Freeze is allowed only when all of the following are true for the same exact source:

1. compile/unit/instrumentation gates are green or any exception is explicitly adjudicated;
2. Phone and Tablet evidence is generated from that exact source head;
3. evidence provenance and hashes match;
4. the runtime pixels have been reviewed rather than inferred from test PASS;
5. no P0/P1 product truth, interaction, adaptive-layout, clipping, blank/dead-space, or accessibility issue remains;
6. no subsequent Android production-UI commit invalidates the accepted evidence.

Only then may:

```
ANDROID_VISUAL_REFERENCE = ACCEPTED
ANDROID_REFERENCE_FREEZE = PASS
```

After that, and only after that, iOS Translation may start. Harmony remains separately gated.
