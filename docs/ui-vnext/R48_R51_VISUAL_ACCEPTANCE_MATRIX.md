# R48–R51 Visual acceptance matrix (Android, light-first)

> Purpose: a per-screen Reference → Actual → Diff worksheet for the **human** review session.
> Nothing in this file is human acceptance. Every "Actual" point is a real device capture under
> `artifacts/runtime-evidence/2026-10-11-r48-r51/`.
> Reference: `spec/ui-vnext/references/android/PDIG_ANDROID_LIGHT_VISUAL_REFERENCE_2026-10-05.jpg`
> (**320×213 JPEG** — see the caveat in §1) plus the written binding principles in the companion
> `.md` and `ANDROID_VISUAL_CONTRACT.md`.

## 1. Reference fidelity caveat (blocking pixel-level diff, not progression)

The in-repo board is a 320×213 compressed copy of a 1536×1024 human-selected render
(`REFERENCE_MANIFEST.json`, SHA256 `3782204282dc81342d8b0311061f9d15c720b7bcd064fd66af78479f6168dbb7`).
At 320×213 a per-screen pixel diff is not meaningful. This round therefore compared **composition,
hierarchy, density, palette family and interaction affordances** against the written principles, and
recorded the resolution gap as a **Human Review input gap**. Supplying the original 1536×1024 board is
the single highest-value action for the review session.

## 2. How to review

1. Open the board next to the captures for the same row.
2. For each row decide: **ACCEPT** / **ACCEPT WITH NITS** / **REJECT**.
3. Any REJECT should name the smallest change (spacing, hierarchy, copy, or a specific control).

## 3. Screens — Phone (1080×2340 @440dpi)

| #   | Screen / state                                               | Reference requirement (binding principles)                                  | Actual capture                                                                        | Machine-checked                                                                                                                  | Human verdict |
| --- | ------------------------------------------------------------ | --------------------------------------------------------------------------- | ------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ------------- |
| 1   | Now, globe home                                              | Globe as infrastructure navigator; task hierarchy over dashboard chrome     | `phone/01-now.png`                                                                    | globe GPU `R15_GPU` + `TEXTURE_READY`; central luma 211.09 (light-first, not near-black); title/metric/geo chips non-overlapping |               |
| 2   | Now, orbit / zoom / reset                                    | Globe is a real surface with real manipulation                              | `01c-world-orbit.png`, `01d-world-zoom-in.png`, `01e-world-reset.png`                 | real drag/zoom taps logged; reset frame equals the initial frame                                                                 |               |
| 3   | First run                                                    | ≤3 screens, honest scope                                                    | `00-first-launch.png`                                                                 | `1 / 3`, `无需登录 · 仅演示交互 · 不访问真实数据`                                                                                |               |
| 4   | 我 (fifth primary)                                           | `我` first-class, not avatar/overflow                                       | `01a-me-from-primary-nav.png`                                                         | `我的数字生活` + `隐私与个人偏好`; return to Now is lossless                                                                     |               |
| 5   | Infrastructure hub                                           | Categories + confirmed region context, not a KPI wall                       | `02-infrastructure.png`, `02a-…us.png`, `02b-…restored.png`                           | `8 类资产`, `地区分布`, `已选地区：美国`, `查看全球`                                                                             |               |
| 6   | Cards list + filters                                         | Dense single column, high-quality faces, filters that do not eat card space | `03-cards.png`, `03a-card-expiry-filter.png`, `03b-card-filter-cleared.png`           | filter yields the expiring card only; `全部状态` restores                                                                        |               |
| 7   | Card detail                                                  | Identity → Lifecycle → Dependencies → Impact → Change                       | `04-card-detail.png`, `04b…`, `04c…`, `04d…`                                          | tabs render authoritative fields; missing ⇒ `未记录`                                                                             |               |
| 8   | Card Impact Lens                                             | Facts with 未确认 shown honestly                                            | `04dd-card-impact.png`, `04de-card-impact-facts.png`                                  | `已确认依赖/需要关注/唯一恢复路径/关键账户/独立备用方式/未确认关系`                                                              |               |
| 9   | Card change: current                                         | Current is the authoritative read                                           | `04h-card-change-current.png`                                                         | `已记录支付关系`, per-service `当前依赖`                                                                                         |               |
| 10  | Card change: transition blocked                              | Blocked state must be visible where the user is                             | `04i-card-change-transition-blocked.png`                                              | `尚未选择替代卡片，迁移阶段保持阻断` inside the tapped stage (fixed this round)                                                  |               |
| 11  | Card change: target selected                                 | A real active card becomes a plan target                                    | `04j-card-change-transition-target.png`                                               | `已选计划目标`; stage status `待验证`                                                                                            |               |
| 12  | Card change: After                                           | After is a plan projection only                                             | `04k-card-change-after.png`                                                           | `完成后是计划投影`, per-service `计划迁移`                                                                                       |               |
| 13  | Card artwork (in-detail)                                     | A small detail action, not a studio                                         | `04e-card-presets-inline.png`, `04f-card-art-applied.png`                             | preset applied, active tab preserved                                                                                             |               |
| 14  | Change centre + Change Phone                                 | Understandable consumer workspace                                           | `05-change-center.png`, `05-change.png`                                               | `更换手机号（进行中）`, `影响分析 · 关键服务`                                                                                    |               |
| 15  | Change Phone checklist + After                               | Progress vs verification kept distinct                                      | `05a-change-checklist.png`, `05aa-change-checklist-footer.png`, `05b-change-plan.png` | `本阶段待办`, `不会通过点击自动标记已完成`, `完成后预览`                                                                         |               |
| 16  | Records                                                      | Evidence trace, not a second attention feed                                 | `06-records.png`                                                                      | `已记录完成/已验证/待验证`, `完成 ≠ 已验证`; no `需要关注`/`即将到来`                                                            |               |
| 17  | Accounts / Emails / Devices / Services collections + details | Distinct identity language per kind                                         | `07-accounts*.png`, `07-emails*.png`, `07-devices*.png`, `07-services*.png`           | headings `账户身份`, `通信 / 恢复身份`, `设备身份`, `服务身份`                                                                   |               |
| 18  | Weaknesses                                                   | Finding with why/what-is-unknown/next step                                  | `07-weaknesses.png`                                                                   | `为什么 / 依据 / 仍未知 / 下一步` present, no health score                                                                       |               |
| 19  | Numbers list / detail                                        | Communication identity, keep is first-class                                 | `08-numbers.png`, `08b-number-detail.png`, `08bb-number-renamed.png`                  | `号码 · 通信身份`; alias saved and rendered                                                                                      |               |
| 20  | Number studio + Impact                                       | Preview-first, real                                                         | `08c-number-studio.png`, `08d-number-studio-edited.png`, `08e-number-impact.png`      | theme applied and saved; lens facts present                                                                                      |               |
| 21  | 我 / privacy                                                 | Masking toggle, default off                                                 | `09-me.png`, `09a-me-mask-enabled.png`, `09b-me-mask-disabled.png`                    | `已关闭 → 已开启 → 已关闭`                                                                                                       |               |
| 22  | Personalization / Data sources                               | Preview-first; fact boundary visible                                        | `09-personalization.png`, `10-data-sources.png`                                       | `显示与个性化`, `隐藏敏感信息`, `当前预览工作区`, `未知`                                                                         |               |
| 23  | Establish → Import → Manual → Review                         | Read-only in Preview; hierarchy and Up chain intact                         | `10a…10h` (+ `10bb/10cc/10dd/10gg` boundary captures)                                 | read-only boundary copy asserted from each real scroll position                                                                  |               |

## 4. Screens — Tablet / medium (1920×1200 @240dpi → 800×1280dp, the CI tablet configuration)

| #   | Screen                                              | Actual capture                                                                                                                                  | Machine-checked                                                                                  | Human verdict |
| --- | --------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------ | ------------- |
| 1   | Now                                                 | `tablet/ui-shots/01-now.png`                                                                                                                    | rail + content; five destinations present                                                        |               |
| 2   | Overview (global / region / drawer)                 | `02-overview-global.png`, `03-overview-region-selected.png`, `04-region-drawer.png`                                                             | **quick entries inside the first viewport** (regression fixed this round)                        |               |
| 3   | Cards / card detail / card art                      | `05-cards.png`, `06-card-detail.png`, `07-card-art-ocean.png`, `08-card-art-night.png`                                                          | MEDIUM uses the real 2-column grid + focused detail (no inspector at this breakpoint, by design) |               |
| 4   | Numbers / detail / studio                           | `09-numbers.png`, `10-number-detail.png`, `11-number-studio-travel.png`                                                                         | same real-path rule as cards                                                                     |               |
| 5   | Accounts / Emails / Devices / Services / Weaknesses | `12-…16-…`                                                                                                                                      | collection surfaces render                                                                       |               |
| 6   | Change current / transition / after                 | `17-…19-…`                                                                                                                                      | stage semantics identical to phone, layout re-flowed                                             |               |
| 7   | Records / Search / Personalization / Data sources   | `20-…24-…`                                                                                                                                      | search empty + real query typed into the field                                                   |               |
| 8   | 我                                                  | `25-me.png`                                                                                                                                     | `pdig.nav.me` + Me workspace asserted at this breakpoint                                         |               |
| 9   | Change centre + card change                         | `26-…29-…`                                                                                                                                      | `28-card-change-transition-blocked.png` proves the fixed blocked copy on tablet too              |               |
| 10  | Empty states                                        | `tablet-cards-empty.png`, `tablet-numbers-empty.png`, `tablet-no-attention.png`, `tablet-no-known-dependencies.png`, `tablet-records-empty.png` | no fabricated counts in any empty state                                                          |               |

## 5. Production (lock-gated real Reality)

| State                                  | Evidence                                                    | Machine-checked                                                     |
| -------------------------------------- | ----------------------------------------------------------- | ------------------------------------------------------------------- |
| Locked (explicit rehearsal extra)      | `production/01-reality-debug-locked.xml`                    | only `PDIG 已锁定`; Reality markers explicitly forbidden and absent |
| Unlocked Reality Now                   | `production/02-reality-debug-unlocked.xml`                  | five primary destinations + Production Now marker                   |
| Background → foreground relock         | `production/03-background-relocked.xml`                     | relocked before Reality is visible                                  |
| Resume                                 | `production/04-resume-production-vnext.xml`                 | same Reality target after resume                                    |
| Process-restart relock                 | `production/05-process-restart-locked.xml`                  | relocked                                                            |
| Default route stays legacy             | `production/07-default-production-legacy.xml`               | legacy shell; no VNext marker                                       |
| Release-default (vnext + cutover keys) | `production/release-default/*.xml`                          | lock → unlock → Reality → relock → resume                           |
| Authority write/read-back              | `logs/prod-p4-ProductionManualRelationshipContractTest.log` | 1/1 passed                                                          |

## 6. Machine-verifiable checks that ARE covered

- No text truncation or overlap on the captured surfaces (semantics bounds + the phone script's
  geometry gates such as the R16 header/asset-rail overlap check).
- 48dp minimum interaction targets for the controls the harness taps (every tap is a real hit).
- Contrast: the light-surface palette assertions and the globe luma gate (211.09) pass.
- Five primary destinations present on phone bottom bar and tablet rail.
- Empty/locked/unknown states never render a fabricated count.
- The globe is not a generic sphere: `R15_GPU` + `TEXTURE_READY`, circular textured silhouette
  contract, projected region labels within the label budget.

## 7. Open items for the human session (explicitly NOT verified this round)

| Item                                          | Why it is open                                                                |
| --------------------------------------------- | ----------------------------------------------------------------------------- |
| Pixel-level diff against the reference board  | the in-repo board is 320×213 (§1)                                             |
| TalkBack end-to-end narration and focus order | not exercised; semantics dumps exist but narration was not driven             |
| Enlarged system font scaling                  | not exercised                                                                 |
| Landscape / split-screen / rotation recovery  | not exercised                                                                 |
| Dark variant parity                           | light-first is the product direction; dark was not re-verified                |
| Per-facet search recall                       | search surface + masking verified; per-facet keyword coverage not re-verified |
| Motion/continuity choreography quality        | recorded as stills, not motion                                                |

## 8. Status

```
VISUAL_CANDIDATE            = READY_FOR_HUMAN_REVIEW
HUMAN_ACCEPTED              = PENDING   (only the user can change this)
ANDROID_REFERENCE_FREEZE    = HOLD
PRODUCTION_CUTOVER          = HOLD
```
