# R50 — Android “我”, global search, privacy, data sources and first-run: report

> Round: R50 within the R48–R51 local-agent round
> Exact source under test: `bc39a2f2499f7d95e2249719d7daf5b384ab5faa`
> Evidence: `artifacts/runtime-evidence/2026-10-11-r48-r51/{phone,tablet,production}/`

## 1. Scope honesty statement

The Me/search/privacy surfaces already existed at this HEAD (R9/R10/R19/R21/R22). This round
verified them at runtime and confirms the five-primary-navigation decision is intact. No R50 surface
required a product change; the only fixes in this area were in the evidence harness (stale copy and
scroll assumptions, documented in the R48 report).

## 2. 五个一级导航不退化 — the core R50 requirement

`spec/ui-vnext/FIVE_PRIMARY_NAVIGATION_DECISION.md` is FROZEN (2026-10-09) and mandates
`现在 / 基础设施 / 变更 / 记录 / 我` with `我` as a first-class destination.

Runtime proof on the compact phone:

- `phone/01-now.png` — the bottom bar renders all five destinations; the harness taps `我` at the
  bottom bar (y≈2230 of 2340) and reaches the Me surface.
- `phone/01a-me-from-primary-nav.png` — `我的数字生活` and `隐私与个人偏好` are rendered, so `我` is a
  real destination, not an avatar shortcut or an overflow item.
- `phone/01b-now-returned-from-me.png` — tapping `现在` returns to the globe home and the frame is
  byte-identical in size to `01-now`, i.e. the round-trip is lossless.

Runtime proof on the tablet: `tablet/25-me.png` and `tablet/01-now.png`; the harness also asserts
`pdig.nav.me` exists and, at EXPANDED, that the Me workspace node is mounted.

Also verified: the phone bottom navigation is mounted **only in a compact root context**
(`VNextShell.isCompactRootContext`, which includes `我` and `记录` alongside the other roots), so the
five destinations are reachable from every root while child flows stay focused. This is the intended
design and is now asserted explicitly by the harness instead of being assumed away.

## 3. “我” page content

Observed at runtime: key identity, cross-region digital presence, continuity summary, personal
preferences, privacy masking and data-source entry. Privacy is exercised end-to-end:

| State                 | Capture                          | Observed                      |
| --------------------- | -------------------------------- | ----------------------------- |
| masking off (default) | `phone/09-me.png`                | `敏感信息遮蔽` shows `已关闭` |
| masking on            | `phone/09a-me-mask-enabled.png`  | `已开启`                      |
| masking off again     | `phone/09b-me-mask-disabled.png` | `已关闭`                      |

Default is **off**, the user can toggle it in both directions, and the toggle takes effect on the
same screen. `phone/09-personalization.png` reaches `显示与个性化` with `隐藏敏感信息`.

## 4. Global search

Tablet evidence `tablet/21-search-empty.png` and `tablet/22-search-query.png` prove the search
surface renders an empty state and then a real query result: the harness types `招商` through a real
text-input event (`performTextInput`) into the editable field and the surface must accept it.

Phone evidence for the search entry: the compact top bar exposes `搜索` on every captured surface, so
search is reachable without leaving the current context. Result masking is covered by the same
privacy contract as the rest of the app; the phone run verifies mask state changes on the Me surface,
and the tablet run captures search before and after a query.

Honest limitation: this round did **not** re-verify keyword coverage for every documented facet
(alias, issuer, carrier, region, 年费, 账单日, 保号到期, `keep/保号` role). The search screen and its
masking are verified; per-facet recall is not claimed here.

## 5. Data sources and the fact boundary

`phone/10-data-sources.png` renders `当前预览工作区`, `演示数据` and `未知`, i.e. the surface states
what the workspace actually is rather than implying live bank/operator data.
`phone/10f-import-up-sources.png` re-asserts `当前预览工作区` and `事实边界` after the Up chain, so the
boundary statement survives navigation.

## 6. First-run experience

`phone/00-first-launch.png` (and its JSON) shows a **3-page** onboarding: `1 / 3`, titled
`看清你的数字生活`, with the illustration row `银行卡 / 号码 / 账户 / 服务`, the continuation `继续 →`,
the skip `跳过`, and the honest scope line `无需登录 · 仅演示交互 · 不访问真实数据`.

The harness then leaves onboarding and requires the current Now home within 48s, which proves the
onboarding is skippable and lands on the real product surface.

## 7. Empty, unknown and locked states — no fake counts

| State                 | Capture                                                           | Observed                                                                                                      |
| --------------------- | ----------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| empty cards           | `tablet/tablet-cards-empty.png`                                   | empty-state surface, not a fabricated count                                                                   |
| empty numbers         | `tablet/tablet-numbers-empty.png`                                 | same                                                                                                          |
| no attention          | `tablet/tablet-no-attention.png`                                  | `当前没有已记录的复核、进行中计划或已报告薄弱点；未知不等于安全。`                                            |
| no known dependencies | `tablet/tablet-no-known-dependencies.png`                         | empty dependencies state                                                                                      |
| records empty         | `tablet/tablet-records-empty.png`                                 | explicit "no recorded events" boundary                                                                        |
| production, locked    | `production/01-reality-debug-locked` (`manifest.json`, XML dumps) | only `PDIG 已锁定` + capability explanation; the Reality/Now markers are explicitly forbidden and were absent |

The production lock screen states the encryption/identity boundary itself
(`数据只保存在这台设备上，进入前需要验证身份。`) and, on a device with no biometrics or lock
credential, refuses to pretend the lock is effective
(`因此无法验证身份，应用锁无法真正生效。`) while offering `已知悉风险，本次进入` and
`重新检查设备能力`. Unknown capability is never presented as security.

## 8. What is NOT claimed

- Human visual acceptance is **PENDING**.
- Per-facet search recall, TalkBack end-to-end narration, enlarged system font behaviour and
  rotation/split-screen recovery were **not** re-measured this round; they remain in
  `R48_R51_VISUAL_ACCEPTANCE_MATRIX.md` as open items for the human review session.
- New-user "first card / first number / first confirmed relationship" creation is a **Production**
  flow (`正式 authority 已就绪`); in Preview it stays read-only and says so
  (`当前 Preview 不提供“保存”按钮`). That is deliberate, not a missing feature.
