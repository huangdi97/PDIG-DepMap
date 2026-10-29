# R48 — Android Asset Experience: implementation & runtime verification report

> Round: R48 (Android asset experience) within the R48–R51 local-agent round
> Exact source under test: `bc39a2f2499f7d95e2249719d7daf5b384ab5faa`
> Runtime base verified: `5a0918546461273efcf99684cf0c1302bc0b89b8` (asset-UI code unchanged by this round)
> Device: API 36 emulator, `wm size 1080x2340` / `density 440` (the phone configuration the CI runner pins)

## 1. Scope honesty statement

The R48 asset surfaces were **already substantially implemented** in this branch by earlier rounds
(R9/R10/R19/R20/R21 and the R45–R47 light-reference work). This round therefore did **not** rebuild
them. What this round contributed is:

1. an **exact-head runtime audit** of every R48 surface on a real API36 phone and tablet, producing
   real device pixels and real semantics dumps;
2. identification and **root-cause repair of the defects that audit found**;
3. a consolidated evidence set that a human reviewer can inspect without re-running anything.

Any claim below is backed by an artifact in
`artifacts/runtime-evidence/2026-10-11-r48-r51/` with a recorded source SHA. Nothing here asserts
human visual acceptance.

## 2. Surfaces verified at runtime (phone, 1080×2340 @440dpi)

| R48 requirement                                                                                                               | Verified by                                                                                                                                               | Observed on device                                                                                                                                                                                          |
| ----------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 基础设施总览: categories + confirmed region context + real entry points, not a KPI wall                                       | `phone/02-infrastructure.png`, `02a-infrastructure-selected-us.png`, `02b-infrastructure-global-restored.png`                                             | `8 类资产`, `地区分布`, `管理你的全球数字基础设施`; region selection shows `已选地区：美国` + `查看全球`, and returning restores the identical global frame (byte-identical size/time, same `labels` count) |
| Region state shared between globe / list / filter and returnable to global                                                    | same three captures                                                                                                                                       | selecting a region through the phone hub changes the hub state; `查看全球 →` restores it; the globe frame in `01-now`/`01b-now-returned-from-me` is unchanged after the round-trip                          |
| Card list: high-density row with card face, name, issuer, tail, status + lifecycle, filters that do not eat card space        | `phone/03-cards.png`, `03a-card-expiry-filter.png`, `03b-card-filter-cleared.png`, `04g-up-to-cards.png`                                                  | `全球支付卡片`, `招行储蓄卡`, filter `即将到期` yields `工行信用卡` only, `全部状态` restores the full list                                                                                                 |
| Card detail hierarchy `Identity → Lifecycle → Dependencies → Impact → Change/Presentation`                                    | `phone/04-card-detail.png`, `04b-card-services.png`, `04c-card-statements.png`, `04d-card-risk.png`, `04dd-card-impact.png`, `04de-card-impact-facts.png` | tabs `概览 / 关联服务 / 账单 / 安全与风险`; the detail reaches `分析更换此卡的影响`                                                                                                                         |
| Lifecycle fields read from authority, missing shown as 未记录 (never inferred)                                                | `phone/04-card-detail.json`, `04c-card-statements.json`                                                                                                   | lifecycle facts render from the authoritative field set; unknown values render as 未记录 rather than a guess                                                                                                |
| Card artwork is an in-detail micro action, not a full-screen studio on phone                                                  | `phone/04e-card-presets-inline.png`, `04f-card-art-applied.png`                                                                                           | `选择内置卡面`, `原卡面`, `海洋`, `相册换图` inside the detail; applying keeps the active tab and returns `基本信息`                                                                                        |
| Numbers are a **communication identity**, not a bank-card skin                                                                | `phone/08-numbers.png`, `08b-number-detail.png`, `08bb-number-renamed.png`                                                                                | `号码 · 通信身份`; recorded number fallback `+86 138****8823` is selectable; an alias (`HK-Main`) can be saved and is rendered                                                                              |
| Number detail: lifecycle / keep-alive / recovery semantics / relationships / impact / change entry                            | `phone/08b-number-detail.png`, `08e-number-impact.png`, `08c-number-studio.png`, `08d-number-studio-edited.png`                                           | `号码生命周期`, `关联服务`; appearance entry `号码外观 →` reaches `号码面定制` with `实时预览` and saves (`保存外观`)                                                                                       |
| Accounts / Emails / Devices / Services each have their own identity hero, core fields, source, relationships and Impact entry | `phone/07-accounts*.png`, `07-emails*.png`, `07-devices*.png`, `07-services*.png`                                                                         | distinct section headings per kind: `账户身份`, `通信 / 恢复身份`, `设备身份`, `服务身份` — the generic `identity_anchor` surface is not silently rendered as a phone number                                |
| Weaknesses are Finding-driven with 为何/依据/仍未知/下一步, not a bare number list                                            | `phone/07-weaknesses.png`                                                                                                                                 | `单点路径 / 共享故障点 / 恢复循环` each carry `已确认事实`, `为什么 …`, `依据 …`, `仍未知 …`, `下一步 …`                                                                                                    |
| Per-control real behaviour (no ghost buttons)                                                                                 | every capture is preceded by a real `input tap` recorded in `logs/phone-capture-aa.log`                                                                   | each step logs `TAP '…' x y` followed by the new screen's semantics, so a control that did nothing would fail the following assertion                                                                       |

## 3. Tablet (medium) verification

The R48 requirement is "Medium/Expanded re-layout, not a scaled phone". The tablet sequence in
`tablet/ui-shots/` covers `01-now` … `29-card-change-after-plan`, including the five primary
destinations, all six infrastructure collections, Records, Search, Personalization, Data Sources
and the Change workspace.

## 4. Defects found and fixed in this round

### 4.1 `R21ChangeCardScreen` — the transition stage did not show why it was blocked

**Observed (phone).** On the `迁移中` projection with no replacement chosen, the only visible
statement was the empty-target placeholder. The sentence `尚未选择替代卡片，迁移阶段保持阻断。`
was mounted in the stage-explanation panel **below the whole change flow**, i.e. below the fold on
a 1080×2340 phone. The CI job failed with
`Screen 04i-card-change-transition-blocked did not show '尚未选择替代卡片，迁移阶段保持阻断'; captured wrong page`.
The projection itself was working correctly (`当前依赖` became `待迁移`), so this was **not** a
navigation or state bug.

**Fix (product).** The empty-target slot in the transition projection now carries the blocking
sentence: `尚未选择替代卡片，迁移阶段保持阻断。\n替代对象是可选计划输入`. Same two-line height as
before, so nothing else moves; the current and After projections keep their existing copy.
The assertion was **not** changed.

### 4.2 `SinglePaneOverview` — tablet quick entries were laid out off-screen

**Observed (tablet, the real CI configuration).** `WideOverview` is used only at `EXPANDED`
(≥840dp). The CI tablet job overrides the display to 1920×1200 @240dpi with `user_rotation=1`,
which is **800×1280dp → MEDIUM**, so `SinglePaneOverview` is rendered. There, `CompactQuickEntries`
was appended after a column that already consumed the viewport, so the row measured **1008×255 px
but was entirely below the fold** — its clipped `boundsInRoot` was `Rect.Zero`. The evidence test
correctly failed:

```
java.lang.AssertionError: tablet overview quick entries must be laid out
  (boundsInRoot=Rect.fromLTRB(0.0, 0.0, 0.0, 0.0), size=1008 x 255,
   screenWidthDp=800, screenHeightDp=1280, deviceClass=tablet)
```

This was a **real defect**: a tablet user never saw 查看卡片 / 查看号码 / 更换手机号 / 薄弱点.

**Fix (product).** The quick-entry row now sits directly under the globe stage, inside the first
viewport at every non-compact breakpoint, and the activity rail follows it.

### 4.3 Coverage gaps in the evidence harness (not product defects)

The phone capture script asserted several strings that no longer exist, and assumed fixed scroll
positions. Each was replaced by the real current copy or by a real scroll on the device; the
business assertions were kept or strengthened, never relaxed:

| Stale assumption                                     | Reality at this HEAD                                                                                                                                      | Resolution                                                                               |
| ---------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| `工行信用卡` is a replacement target                 | it is `expiring_soon`; the screen explicitly lists only `active` cards (and `ANDROID_UI_VNEXT_R21_SOURCE_REPORT.md` §"lists other recorded active cards") | target the first real `active` candidate, `中行数字卡`; assert it becomes `已选计划目标` |
| replacement strip reachable at a fixed `y`           | it is mounted below the continuity panel and the Impact Lens                                                                                              | scroll the real container until the candidate is a genuine tap target                    |
| `记录` is only a bottom-nav label                    | the Change Center renders its own `记录` shortcut tile                                                                                                    | require a real bottom-navigation hit region (bottom 12% of the display)                  |
| the bottom nav is always mounted                     | `VNextShell.isCompactRootContext` mounts it only in compact **root** contexts                                                                             | step back to a root, then tap the primary destination                                    |
| `done ≠ verified`                                    | the Records surface renders `完成 ≠ 已验证`                                                                                                               | assert the current copy                                                                  |
| `依赖端点` is the service-detail heading             | the heading is `服务身份`                                                                                                                                 | assert the real heading                                                                  |
| `物理访问端点` is the device-detail heading          | the heading is `设备身份`                                                                                                                                 | assert the real heading                                                                  |
| the Weaknesses collection shows `已记录关注项`       | it renders the R9 workspace with `从已记录的到期和恢复关系识别薄弱点`                                                                                     | assert the real copy                                                                     |
| the R23 manual-establish row is `没有文件？手工记录` | `R23ImportReferenceScreen` renders `title + " →"`                                                                                                         | assert the real label                                                                    |
| read-only boundary cards fit in one viewport         | they sit below the fold on a compact phone                                                                                                                | capture each real scroll position and assert the union (see `require_across`)            |

## 5. What is NOT claimed

- **Human visual acceptance is PENDING.** The reference board in-repo is a 320×213 JPEG, so
  per-screen pixel comparison against the original 1536×1024 board was not possible.
- No `ANDROID_REFERENCE_FREEZE`: it requires human acceptance plus all technical gates.
- The light-first direction was honoured, but `dark` variant parity was not re-verified this round.
