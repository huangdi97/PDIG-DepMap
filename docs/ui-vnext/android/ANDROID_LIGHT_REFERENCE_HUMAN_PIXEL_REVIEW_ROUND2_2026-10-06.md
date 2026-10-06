# ANDROID_LIGHT_REFERENCE_HUMAN_PIXEL_REVIEW_ROUND2_2026-10-06

> Reviewed runtime pack: `artifacts/runtime-evidence/2026-10-05-android-light-reference-final-validation-b659ae4/`
>
> Runtime source: `b659ae4b60f95d52c222ed107ace39e53b6db48e`
>
> Evidence commit: `6cb48868a1118e4bb62d7861f71ebc0f0d370ac7`
>
> Post-review pixel-changing Android production checkpoint: `0dd0cee29d3b9410f211942283d96f893149dc39`
>
> Verdict: **RUNTIME PACK REJECTED FOR FREEZE / SOURCE REMEDIATION APPLIED / FRESH EXACT-HEAD EVIDENCE REQUIRED**

## 1. Review authority

This is a Human pixel review of the committed Phone + Tablet PNG files, not an acceptance of Agent PASS labels.

The Human-selected light board remains a **Visual Direction Reference**, not a pixel golden and not functional
truth. Canonical/domain truth, frozen IA, Android ergonomics/accessibility, and the PresentationProfile boundary
remain higher authority.

No review finding in this document changes:

- Primary IA: 现在 / 基础设施 / 变更 / 记录;
- Infrastructure siblings: 总览 / 卡片 / 号码 / 账户 / 邮箱 / 设备 / 服务 / 薄弱点;
- `PresentationProfile != PersonalReality != Canonical`;
- Unknown never becomes Safe by inference;
- Change Phone Current / Transition / After, where After = Plan Projection;
- Desktop Dark Reference Freeze.

## 2. Provenance result

The pack is useful because all 48 main screenshots are tied to the exact pre-evidence source
`b659ae4...`. It is **not** a freeze candidate because its own report/manifests record unresolved runtime
gates and several manually captured frames do not match their requested screen/state.

The reference provenance was also inconsistent in that pack: manifest/docs declared SHA256
`4b2ca9e0...`, while the repository JPEG content was measured as
`3782204282dc81342d8b0311061f9d15c720b7bcd064fd66af78479f6168dbb7`.
The repository declaration has now been corrected to the actual content hash.

## 3. Phone pixel review

| Screen | Human result | Finding |
| --- | --- | --- |
| 01 Now | **REJECT** | Light shell/task hierarchy is directionally correct, but the Globe frame is still the dark loading/fallback sphere; headline contrast was also weak on the light surface. |
| 02 Overview Global | **REJECT** | Infrastructure Hub is correct; Global Globe is still a dark loading/fallback sphere. |
| 03 Overview Region | ACCEPTABLE DIRECTION | Textured Earth and region state are visibly present. |
| 04 Region Detail | **INVALID EVIDENCE** | PNG remains Region Selected; manifest itself records expected `region-detail` but actual `region-selected`. |
| 05 Cards | ACCEPTABLE | Asset identity and compact density are materially aligned with the selected direction. |
| 06 Card Detail | ACCEPTABLE | Asset-first detail hierarchy is clear. |
| 07/08 Card Studio | ACCEPTABLE DIRECTION | Preview-first composition is correct; exact-head a11y contract still required. |
| 09 Numbers | ACCEPTABLE | Reads as communication identity rather than a generic phonebook. |
| 10 Number Detail | ACCEPTABLE | Identity-first hierarchy and Unknown semantics are clear. |
| 11 Number Studio | ACCEPTABLE DIRECTION | Preview-first; requires fresh exact-head accessibility validation. |
| 12–16 secondary infrastructure | ACCEPTABLE WITH P2 CRAFT DEBT | Readable/light and semantically correct, but intentionally lower visual richness than Cards/Numbers. No freeze-blocking product-truth defect found. |
| 17 Change Current | **INVALID EVIDENCE** | Captured frame visibly has 迁移中 selected. |
| 18 Change Transition | **INVALID EVIDENCE** | Same visible state as 17; cannot establish Current vs Transition distinction. |
| 19 Change After | ACCEPTABLE | Plan projection is explicit and does not claim completed reality. |
| 20 Records | ACCEPTABLE | Task/progress hierarchy is clear. |
| 21 Search Empty | ACCEPTABLE | Search scope and quick navigation are clear. |
| 22 Search Query | **INVALID EVIDENCE** | PNG is a Cards surface rather than Search Query results. |
| 23 Personalization | ACCEPTABLE | Preview-first and presentation-only boundary are explicit. |
| 24 Data Sources | ACCEPTABLE | 本机优先 / 已确认 / 未知 boundary is clear and Unknown remains Unknown. |

## 4. Tablet pixel review

The primary rail correctly contains only product-level destinations and Infrastructure siblings stay inside
content. The remaining blockers are not “Tablet = stretched Phone”; they are specific evidence and adaptive
craft problems:

- 01/02 Global Globe frames are still the dark loading/fallback sphere.
- Cards is too sparse in region-scoped states; a persistent asset inspector is needed so expanded space carries
  useful object context rather than empty canvas.
- `09-numbers.png` and `10-number-detail.png` are byte-identical in Git, so Number Detail is not independently evidenced.
- `11-number-studio-travel.png` visually remains in the Numbers list-detail flow rather than showing Studio.
- `17-change-current.png` and `18-change-transition.png` are byte-identical and both show 迁移中.
- The manifest records the Region Detail capture problem rather than proving the requested state.

Accounts / Emails / Devices / Services / Weaknesses remain light and readable. Their more utilitarian
presentation is P2 craft debt, not a P0/P1 product-truth or adaptive-layout failure.

## 5. Runtime/test blockers found in the pack

The evidence report truthfully records additional blockers:

1. `AndroidAdaptiveShellContractTest` and `AndroidLightVisualSourceContractTest` used assertion imports not
   available in the current Compose UI test artifact.
2. `NumberDetailVerticalFlowContractTest` measured Hero directly to Services even though the approved source
   intentionally inserts the Number Summary strip between them.
3. `VNextAccessibilityEvidenceTest` measured the internal “旅行” Text glyph instead of the clickable ThemeTile,
   and compared raw pixels to `48f` instead of density-correct 48dp.
4. `SourceCompleteScreenshotEvidenceTest` could not reach `TEXTURE_READY` within its bounded window, so the
   official 24+24 + empty-state capture path never completed.
5. 10 required empty-state screenshots therefore remain missing.

## 6. Direct GitHub source remediation after Human review

ChatGPT directly changed Android source/tests; the Local Agent did not redesign UI.

### Globe

- reduced the expensive per-pixel HIGH texture render cap from 768px to 512px and BALANCED from 512px to 384px;
- idle yaw now waits for a stable `TEXTURE_READY` frame and refreshes at a low cadence instead of cancelling
  the texture renderer every ~50ms;
- the official screenshot gate keeps a bounded 30s texture-ready window;
- Now/Expanded Overview Globe labels now sit on readable light surfaces;
- Now infrastructure metrics use a light frosted strip instead of a dominant dark dashboard bar.

### Adaptive Cards

Expanded Cards is now an actual list-detail workspace:

- left = card gallery/list with selected asset state;
- right = persistent card identity inspector with confirmed services/risk context;
- full detail remains a focused child flow;
- new adaptive contract guards selection → inspector update.

### Contracts/provenance

- migrated stale assertion imports to supported Compose assertions;
- Number Detail contract now validates `Hero → Summary → Services` adjacency;
- accessibility contract measures real clickable ThemeTile bounds in density-correct pixels;
- selected-reference SHA declaration now matches the actual repository JPEG.

## 7. Required next pack

The Local Agent must now pull the exact current remote HEAD and **only** build/run/test/capture evidence.

The next pack must be produced by the official source-complete screenshot test path where possible, so screen
identity is programmatic rather than reconstructed by manual adb navigation. It must include:

- Phone 24;
- Tablet 24;
- 10 empty states;
- all four Globe Human Review frames with manifest `globeTextureState=texture_ready`;
- Region Detail actual state = `region-detail`;
- Search Query actual state = `search-query`;
- Change Current / Transition / After as distinct states and distinct frames;
- independent Tablet Numbers / Number Detail / Number Studio frames;
- Card/Number PresentationProfile process restart persistence;
- workspace preference persistence;
- Search → result → Back;
- Region select → Detail → system Back preserves region → explicit Global reset;
- Number Detail flow probe;
- accessibility;
- system Back;
- Desktop Freeze Guard;
- evidence hashes/provenance;
- Local Agent production-source mutation = 0.

## 8. Gate

```
HUMAN_VISUAL_DIRECTION_REFERENCE = SELECTED

ANDROID_UI_VNEXT_SOURCE_DESIGN = COMPLETE
ANDROID_LIGHT_VISUAL_TRANSLATION_SOURCE = COMPLETE
ANDROID_SOURCE_REMEDIATION_AFTER_PIXEL_REVIEW = COMPLETE

ANDROID_RUNTIME_EVIDENCE_FOR_CURRENT_HEAD = REQUIRED
ANDROID_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_ACCEPTANCE
ANDROID_REFERENCE_FREEZE = HOLD

IOS_UI_VNEXT = HOLD
HARMONY_UI_VNEXT = HOLD
```

No iOS translation is authorized until the fresh Android pack is reviewed and no P0/P1 visual/product/adaptive
issue remains.
