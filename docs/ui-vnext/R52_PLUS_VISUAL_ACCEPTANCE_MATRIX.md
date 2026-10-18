# PDIG / DepMap — R52+ visual acceptance matrix

> **Final source HEAD:** `b85c7a579939e5466a086fd751eae770fd0a6d0d`  
> **Capture source HEAD:** `302a65d6` for phone 1.0, `b85c7a57` for phone 1.3×
> dynamic type, `6b823fb6` for tablet/Production, `7993f29d` for Expanded,
> `c035ce9a` for advanced Production.  
> **Human verdict is still empty by design.** This file is a review worksheet, not
> an acceptance record. `HUMAN_ACCEPTED = PENDING`.
>
> Evidence root: `artifacts/runtime-evidence/2026-10-17-r61-plus/`  
> Phone: 1080×2340 @440 dpi, Pixel 5 AVD, API36.  
> Tablet: 1200×1920 @240 dpi (800×1280 dp, MEDIUM), Pixel Tablet AVD, API36.  
> Expanded: 2560×1600 @320 dpi (1280×800 dp, landscape), Pixel Tablet AVD, API36.  
> Production: 1080×2400 @420 dpi, Pixel phone AVD, API36, installed
> `com.pdig.app` APK.

## 1. Reference fidelity caveat (blocking pixel diff, not progression)

The in-repo board
`spec/ui-vnext/references/android/PDIG_ANDROID_LIGHT_VISUAL_REFERENCE_2026-10-05.jpg`
is **320×213 and truncated**: Pillow fails with
`OSError: broken data stream when reading image file`, and the image harness
rejects it as malformed. The claimed 1536×1024 original is still absent. This
matrix therefore records composition, hierarchy, density, palette family,
truth states and interaction affordances against the written binding
principles; it does **not** fake a per-pixel diff.

## 2. How to review

1. Open the listed PNG(s), then decide **ACCEPT / ACCEPT_WITH_NITS / REJECT**.
2. For a REJECT, name the smallest concrete change (spacing, hierarchy, copy, a
   control or a truth-state).
3. Do not treat green CI, an existing screenshot, or this file as acceptance.
4. The system status/navigation bars are included in phone/tablet captures
   because these are real display pixels; they are not app chrome.

## 3. Phone screens (70 real PNGs)

| # | Screen / state | Capture(s) | Machine-checked / observed | Human verdict |
| --- | --- | --- | --- | --- |
| 1 | First launch, 3-step guide | `phone/00-first-launch.png` | `1 / 3`, `无需登录 · 仅演示交互 · 不访问真实数据` | |
| 2 | Now / globe home | `phone/01-now.png` | GPU `TEXTURE_READY`; earth visible; central luma 211.09; readable title / metrics | |
| 3 | Me as fifth primary, and return | `phone/01a-me-from-primary-nav.png`, `01b-now-returned-from-me.png` | bottom nav has 5 items; `我的数字生活`; return is lossless | |
| 4 | Globe manipulation | `phone/01c-world-orbit.png`, `01d-world-zoom-in.png`, `01e-world-reset.png` | real drag/zoom/reset taps; projected labels move with camera | |
| 5 | Infrastructure hub | `phone/02-infrastructure.png` | **8 个入口**, `地区分布`, 6 regions, globe inset | |
| 6 | Region selected / restored | `02a-infrastructure-selected-us.png`, `02b-infrastructure-global-restored.png` | `已选地区：美国`, `查看全球`; restoration | |
| 7 | Cards list + filters | `03-cards.png`, `03a-card-expiry-filter.png`, `03b-card-filter-cleared.png` | high-quality card faces; status filter yields the expiring card; clear restores | |
| 8 | Card detail + tabs | `04-card-detail.png`, `04b-card-services.png`, `04c-card-statements.png`, `04d-card-risk.png` | identity / lifecycle / dependencies / impact / change; missing ⇒ `未记录` | |
| 9 | Card Impact Lens | `04dd-card-impact.png`, `04de-card-impact-facts.png` | `已确认依赖 / 需要关注 / 唯一恢复路径 / 未确认关系`; no health score | |
| 10 | Card artwork | `04e-card-presets-inline.png`, `04f-card-art-applied.png`, `04g-up-to-cards.png` | preset applied; tab preserved; real back stack | |
| 11 | Card change: Current | `04h-card-change-current.png` | `当前卡片`, `已记录支付关系`, per-service `当前依赖` | |
| 12 | Card change: Transition blocked | `04i-card-change-transition-blocked.png` | blocking reason inside the tapped stage; `尚未选择替代卡片…` | |
| 13 | Card change: target selected | `04j-card-change-transition-target.png` | a real active card becomes `已选计划目标` | |
| 14 | Card change: After | `04k-card-change-after.png` | `完成后是计划投影`; per-service `计划迁移`; not “already done” | |
| 15 | Change Center | `05-change-center.png` | active change, prepare-change, maintenance; no fake plan | |
| 16 | Change Phone | `05-change.png`, `05a-change-checklist.png`, `05aa-change-checklist-footer.png`, `05b-change-plan.png` | Current / Transition / After; `完成后预览`; click does not complete | |
| 17 | Records | `06-records.png` | completion vs verification counts; `完成 ≠ 已验证`; no second attention feed | |
| 18 | Accounts / Emails / Devices / Services | `07-accounts*.png`, `07-emails*.png`, `07-devices*.png`, `07-services*.png` | distinct identity language per kind; identity-top and impact-bottom frames; `状态 使用中` (no raw `active`) | |
| 19 | Weaknesses | `07-weaknesses.png` | why / basis / unknown / next; consumer Chinese; no health score | |
| 20 | Numbers list | `08-numbers.png` | one identity line when no alias; `唯一恢复`; lifecycle | |
| 21 | Number detail / alias | `08b-number-detail.png`, `08ba-number-alias*.png`, `08bb-number-renamed.png` | recorded number fallback; real alias editor; saved `HK-Main` is rendered | |
| 22 | Number studio / impact | `08c-number-studio.png`, `08d-number-studio-edited.png`, `08e-number-impact.png` | live preview; theme save; Impact Lens facts | |
| 23 | Me / privacy | `09-me.png`, `09a-me-mask-enabled.png`, `09b-me-mask-disabled.png` | `我的数字生活`; mask `已关闭 → 已开启 → 已关闭`; no silent fact mutation | |
| 24 | Personalization / Data sources | `09-personalization.png`, `10-data-sources.png` | display-only controls; preview workspace; `未知 ≠ 安全` | |
| 25 | Establish / Manual / Import / Review | `10a…10h`, `10bb`, `10cc`, `10dd`, `10gg` | read-only Preview boundary; Up chain; no local fake Reality | |
| 26 | Search | `11-search-empty.png`, `11-search-query.png` | `未找到不代表不存在`; `HSBC` yields card, account and service results | |
| 27 | Empty / masked / boundary states | tablet empty set + phone mask set; Production lock screen | no fabricated counts; masking hides identity, not truth | |

## 4. Tablet screens (34 real PNGs + manifest)

| # | Screen / state | Capture(s) | Machine-checked / observed | Human verdict |
| --- | --- | --- | --- | --- |
| 1 | Now | `tablet/ui-shots/01-now.png` | rail + content; five destinations; **GPU earth present** (pixel gate >10% blue) | |
| 2 | Overview global | `02-overview-global.png` | real GPU earth; quick entries in first viewport | |
| 3 | Region selected | `03-overview-region-selected.png` | `已选地区`, camera focus, projected labels | |
| 4 | Region drawer | `04-region-drawer.png` | five regions, counts, real scroll | |
| 5 | Cards | `05-cards.png` | 2-column face collection; identity/lifecycle/status | |
| 6 | Card detail / art | `06-card-detail.png`, `07-card-art-ocean.png`, `08-card-art-night.png` | focused detail at MEDIUM; real art save | |
| 7 | Numbers | `09-numbers.png` | one identity line when no alias; 8 numbers; filters | |
| 8 | Number detail / studio | `10-number-detail.png`, `11-number-studio-travel.png` | lifecycle, relations, studio preview | |
| 9 | Accounts / Emails / Devices / Services / Weaknesses | `12-…16-….png` | distinct collections; consumer copy; no KPI wall | |
| 10 | Change current / transition / after | `17-…19-….png` | Current / Transition / After separate; After is plan projection | |
| 11 | Records / Search / Personalization / Sources | `20-…24-….png` | evidence trace; `21` empty query shortcuts, `22` real `招商` query | |
| 12 | Me | `25-me.png` | fifth primary on rail; continuity / privacy / regions | |
| 13 | Change Center + card change | `26-…29-….png` | `28` blocked copy visible; `29` After plan | |
| 14 | Empty states | `tablet-cards-empty.png`, `tablet-numbers-empty.png`, `tablet-no-attention.png`, `tablet-records-empty.png`, `tablet-no-known-dependencies.png` | no fabricated counts; unknown boundaries | |

The tablet manifest is
`tablet/ui-shots/source-complete-raw-manifest.json` (34 entries, viewport
1200×1920, density 1.5, source head recorded in the manifest).

## 4a. Expanded screens (11 real PNGs + manifest)

Captured at 1280×800 dp, landscape, source `7993f29d`, package
`com.pdig.app.preview.p7993f29`. `TabletAdaptiveContractTest` is **5/5 OK** at
this width, including the two EXPANDED-only three-pane projections that are
skipped at 800 dp.

| # | Screen / state | Capture(s) | Machine-checked / observed | Human verdict |
| --- | --- | --- | --- | --- |
| 1 | Now | `tablet-expanded/01-now.png` | five visible rail labels; wider hero; GPU earth | |
| 2 | Infrastructure | `02-infrastructure.png` | secondary chip row; wider earth; region labels | |
| 3 | Cards | `03-cards.png`, `03a-card-selected.png` | Expanded grid + right-side preview pane | |
| 4 | Card detail | `04-card-detail.png` | full detail, no cramped single-column | |
| 5 | Change Center | `05-change.png` | in-progress + prepare groups | |
| 6 | Change Phone Current | `06-change-phone-current.png` | Current projection; stage row; impact summary | |
| 7 | Change Phone Transition | `07-change-phone-transition.png` | Transition; blocked retire with reason | |
| 8 | Change Phone After | `08-change-phone-after.png` | After is plan projection; unverified services stay 待处理 | |
| 9 | Records | `09-records.png` | evidence trace; 完成 ≠ 已验证 | |
| 10 | Me | `10-me.png` | fifth primary; identity + continuity + infrastructure | |

The Expanded manifest is `tablet-expanded/MANIFEST.json` (source head, device,
breakpoint and per-screen state). This set is evidence, not acceptance.

## 5. Production screens (real installed `com.pdig.app`)

| State | Capture / XML | Observed | Verdict |
| --- | --- | --- | --- |
| Default Legacy locked | `production/03-default-legacy-locked.png/.xml` | `PDIG 已锁定`; no VNext/synthetic marker | |
| Default Legacy unlocked | `production/04-default-legacy-home.png/.xml` | Legacy home; **not** Production VNext | |
| Cutover locked (no extras) | `production/01-cutover-locked.png/.xml` | lock fails closed | |
| Cutover Production Now | `production/02-cutover-unlocked.png/.xml` | five roots; GPU earth; `尚无已确认地区` (consumer copy); `未知不等于安全`; no synthetic marker | |
| Background / restart relock | `production/production-vnext-release-default/03-…`, `05-…xml` | relocked; no Reality leak | |
| Secure rehearsal (Legacy default + explicit extra) | `production/production-vnext-rehearsal/*.xml` | locked → VNext → relock → resume; default stays Legacy | |

`production/production-vnext-rehearsal/manifest.json` and
`production/production-vnext-release-default/manifest.json` both record
`result: PASS` and the final source SHA.

### 5a. Advanced Production surfaces (4 real PNGs, source `c035ce9a`)

| State | Capture / XML | Observed | Verdict |
| --- | --- | --- | --- |
| Me / management | `production-advanced/11-production-me.png/.xml` | `数据版本`; `所有写入都经过正式确认`; no authority/PHONE_NUMBER | |
| Establish hub | `production-advanced/12-production-establish-hub.png/.xml` | `微信账单` source kind; manual / import / relationship entries | |
| Manual relationship | `production-advanced/13-production-manual-relation.png/.xml` | consumer boundary copy; no FailureDomain/Continuity engine | |
| Review empty | `production-advanced/15-production-review-empty.png/.xml` | `空队列不代表已记录关系完整` | |

Launched with the documented `vnext_production` debug extra; release default stays
Legacy. The system SAF file picker is excluded because it is not PDIG UI.

### 5b. Dynamic type 1.3× (6 real PNGs, source `b85c7a57`)

| State | Capture | Observed |
| --- | --- | --- |
| Now | `font-scale-1.3/01-now.png` | full `中国大陆`; asset rail 2×2, no clipped 4th card |
| Infrastructure | `02-infrastructure.png` | eight entries and filters readable |
| Cards / detail | `03-cards.png`, `04-card-detail.png` | no label overlap; lifecycle grid fits |
| Change | `05-change.png` | cards and CTAs wrap |
| Me | `06-me.png` | identity hints visible; number card height matches |

`font-scale-1.3/MANIFEST.json` records the device, font scale and fixes. 1.0
layouts are unchanged by the `fontScale > 1.15` branches.

## 6. Deltas fixed in this round (previously visible in captures)

| Delta | Before | After |
| --- | --- | --- |
| Tablet GPU earth in evidence | blank hero, only labels/arcs | full earth in 01/02/03; pipeline fails if absent |
| Unnamed number identity | same number printed twice per row | one identity line; distinct alias adds a second line |
| Account status | `状态 active` | `状态 使用中` |
| Card-change copy | `处于 active 的卡片` | `处于使用中的卡片` |
| Findings copy | `参考 Reality`, `FailureDomain`, `RecoveryCycle` | consumer Chinese with why/basis/unknown/next |
| Production copy | `RegionFact`, `maintenance_profile`, `Finding`, `Reality` | confirmed records / maintenance facts / continuity findings |
| Infrastructure count | `8 类资产` | `8 个入口` |

## 7. Remaining visual gaps and open questions

| Priority | Gap | Note |
| --- | --- | --- |
| Resolved | Now `需要处理 6` vs Me `待处理 3` | Me now says `需关注 3` / `3 项需关注`; counts unchanged. |
| Resolved | MEDIUM tablet rail was icon-only | Each of the five primary roots now has a visible 9sp label; asserted in `TabletAdaptiveContractTest`. |
| Resolved | Advanced Production manual/import/establish copy | Consumer Chinese now; behavior unchanged. |
| Resolved | 851 dp phone test visibility | `8fef8030`: 38/38 contracts pass at 1080×2340@440; no product assertion removed. |
| Reference | 320×213 truncated board; 1536×1024 original absent | Pixel-level diff blocked. |
| Human | Overall visual acceptance, reference freeze, cutover | `PENDING` / `HOLD` / `HOLD`. |

## 8. Status

```text
VISUAL_CANDIDATE            = READY_FOR_HUMAN_REVIEW
HUMAN_ACCEPTED              = PENDING
ANDROID_REFERENCE_FREEZE    = HOLD
PRODUCTION_CUTOVER          = HOLD
```

No row in this matrix is human-accepted. A screenshot existing, a machine check
passing, or GitHub CI being green does not change these values.
