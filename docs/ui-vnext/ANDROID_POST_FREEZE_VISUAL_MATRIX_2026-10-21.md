# ANDROID POST-FREEZE VISUAL MATRIX — 2026-10-21

Source head `a3049c64` · productionDebug APK `6512acb5…` · evidence root
`artifacts/runtime-evidence/2026-10-21-android-postfreeze-closure/`
Every row was captured on a real API36 emulator and paired with a UIAutomator XML
snapshot; the strings in "asserted" are the exact on-screen labels the capture
script refuses to continue without.

`humanReview` for every row: **NOT_RUN**. Nothing here is a visual acceptance.

## A. Production — lock, shell, identity (API36 phone, 411×914 dp)

> Device resolution `1080×2400 @420` (the shared phone AVD carries a size override on top of
> its 1080×2340 panel), font scale read from the device (`fontScaleSource = device`).
> Landscape and font-scale 1.3 / 1.5 sets are in section E.

| Shot | Screen / state | Asserted on screen |
| --- | --- | --- |
| `production-lock` | LockGate, locked | `PDIG 已锁定`, `已知悉风险，本次进入` |
| `production-now` | Now, unlocked | `你的数字基础设施 · 已记录的全球上下文`, 现在/基础设施/变更/记录/我 |
| `production-infrastructure` | Infrastructure hub | 基础设施, 账户, 邮箱, 设备, 服务 |
| `production-account-detail` | Account identity hero | `账户身份` + object name; no Passkey/Authenticator/Secure Enclave/Trusted device/MFA/通行密钥 |
| `production-email-detail` | Email identity hero | `通信身份` |
| `production-service-detail` | Service identity hero | `服务节点` |
| `production-change-center` | Change Center | 变更, 已记录变更计划, 更换手机号 |

## B. Production — Change Make-Before-Break journey (same phone)

| Shot | State | Asserted on screen |
| --- | --- | --- |
| `production-change-current` | plan opened, nothing done | 变更全程, 当前, 过渡中, 完成后（计划）, 下一步 |
| `production-change-pending-verification` | migration recorded, verification pending | 确认验证这一步, 动作已记录完成，结果尚未验证, 确认验证会写入正式记录 |
| `production-change-retirement-blocked` | retirement not reachable | 停用旧手机号（新路径全部验证后）, 等待前置步骤 |
| `production-change-retirement-eligible` | both new paths verified | 记录完成这一步 + 停用旧手机号（新路径全部验证后） as the CTA |
| `production-change-after-plan` | retirement recorded | 计划预期, 未发生 |
| `production-records` | Records | 已记录的操作与验证证据 · 按计划分组, 已记录完成, 已验证 |

## C. Production — stale → reanalysis

| Shot | State | Asserted on screen |
| --- | --- | --- |
| `production-change-stale` | graph moved after analysis | 基础设施已发生变化, 重新分析当前影响, 使用上方的「重新分析当前影响」；界面不会用旧快照继续写入。 — and no execution CTA exists |
| `production-change-reanalyzed` | after authoritative reanalysis | 准备状态 + `分析时数据版本 N · 当前数据版本 N` (script fails unless the two numbers are equal) |

## D. Production — privacy

| Shot | State | Asserted on screen |
| --- | --- | --- |
| `production-privacy-default-off` | masking OFF (default) | 隐私与偏好, 敏感信息遮蔽：已关闭, 本机显示偏好，不修改已确认记录 |
| `production-privacy-on` | masking ON in the real row | preference row state actually changed (count of 已关闭 drops) |
| `production-privacy-on-infrastructure` | masked inventory | 身份对象 present and `旧主号` **absent** |
| `production-privacy-off-restored` | masking OFF again | 敏感信息遮蔽：已关闭 |
| `production-privacy-off-restored-infrastructure` | restored values | 身份对象, 旧主号 |
| `production-me` | Me workspace | 我的数字生活 |

## E. Tablet — expanded (1280×800 dp) and medium (800×1280 dp)

| Shot | Window class | Asserted on screen |
| --- | --- | --- |
| `tablet-now` | expanded | Reality marker + five primary destinations |
| `tablet-infrastructure` | expanded | 基础设施 |
| `tablet-change` | expanded | 变更, 已记录变更计划 |
| `tablet-records` | expanded | 已记录的操作与验证证据 · 按计划分组 |
| `tablet-me` | expanded | 我的数字生活 |
| `medium-tablet-now` … `medium-tablet-me` | medium | same assertions, portrait |
| `land-tablet-now` … `land-tablet-me` | phone 914×411 dp | same assertions, landscape |
| `f13-tablet-now` … `f13-tablet-me` | phone, **font 1.3** | same assertions |
| `f15-tablet-now` … `f15-tablet-me` | phone, **font 1.5** | same assertions |
| `font15-tablet-now` … `font15-tablet-me` | tablet expanded, **font 1.5** | same assertions |

## F. Adopted preview catalogue and the remaining gaps

The official harness `android/scripts/ui_vnext_pixel_capture.py` was re-run against
a preview APK built at the current head (`GIT_SHA cd355b5b`, package
`com.pdig.app.preview.pcd355b5`) and produced **72 screenshot + UI-dump pairs**.
21 of them are adopted here under their §27 names in `phone-preview/`:

`01-now`, `02-infrastructure`, `03-cards`, `04-card-detail`, `05-numbers`,
`06-number-detail`, `07-accounts`, `08-account-detail`, `09-emails`,
`10-email-detail`, `11-devices`, `12-device-detail`, `13-services`,
`14-service-detail`, `15-weaknesses`, `16-change-center`, `24-records`, `25-me`,
`26-privacy-on`, `27-privacy-off`, `28-search`.

A static check confirmed the harness's assertions still match current source copy
(80 of 82 expected strings present; the two "missing" are a runtime-interpolated
region label and a value the harness itself types). Each adopted row keeps
`humanReview = NOT_RUN`, records the preview package and APK hash, and states that
the assertions are the harness's own `require_screen` gates rather than mine.

**Adopted since:** `17-change-phone-current` (captured by tapping the preview's 当前
projection tab), `18-change-phone-transition` (harness `05-change`, 「第 3/6 步 · 验证新号码」)
and `23-change-phone-after-plan` (harness `05b-change-plan`, explicitly labelled
「完成后 · 预期状态（未执行）」). That makes 24 of the 28 ids real preview captures.

**Still `NOT_RUN` (4):** `19-change-phone-before-retirement-blocked`,
`20-change-phone-verification-pending`, `21-change-phone-all-required-paths-verified`,
`22-change-phone-retirement-eligible`. These are **authority** states: the preview
build has no Change authority at all, so rendering them there would make a synthetic
fixture look like a real retirement decision — the exact thing this project forbids.
Each row carries `coveredBy` pointing at the Production shot that proves the same
state (`production-change-retirement-blocked`,
`production-change-pending-verification`, `production-change-retirement-eligible`).

Also not captured: Device detail hero (`设备端点`) on the Production surface — the
canonical spec allows runtime creation only for `payment_instrument`, `account` and
`service`. The preview device detail (`12-device-detail`) is synthetic and is
labelled as such.

**First attempt, and how it was unblocked:** the harness initially aborted at its own
provenance gate:

```
RuntimeError: Displayed preview source SHA a3049c6 missing from 01-now UI XML
```

That gate is correct and must not be relaxed. The cause is local, not a product
defect, and it is a genuine build/provenance mismatch rather than a copy drift:

- `pdigPreviewBuildSha` only sets the **applicationId suffix**
  (`com.pdig.app.preview.pa3049c6`);
- `BuildConfig.GIT_SHA` is generated from `git rev-parse --short HEAD` **at build
  time** — the APK on the device was built at `1d50a272`;
- the harness asserts the displayed SHA equals `GITHUB_SHA[:7]`, which I had set to
  `a3049c6`.

So the script refused to label a `1d50a272` build as `a3049c6`, which is exactly
what a provenance gate should do. Re-running needs either
`GITHUB_SHA=<the sha the APK was actually built at>` or a rebuild at the target
commit; **this is not a blocker for the codebase, only for that local run.**

Expect one further step once the gate passes: several expectations in that script
are already stale against the current source (Records, for example, no longer says
「发生过什么」 but 「已记录的操作与验证证据 · 按计划分组」) and must be re-aligned to
the real copy, not loosened, before the catalogue can be claimed.

Also not captured: Device detail hero (`设备端点`) — the canonical spec allows
runtime creation only for `payment_instrument`, `account` and `service`, so no
governed runtime path can put a device into Reality in this build. Membership and
custom identities are absent for the same reason, so their masking state could not
be exercised at runtime in this round.

## G. Contract tests that back the matrix

| Class | Result |
| --- | --- |
| `ProductionChangeKernelContractTest` | 3/3 PASS (prerequisite, verification order, terminal plan) |
| `ProductionChangeStageInteractionContractTest` | 7/7 PASS (stage tabs, masked projection, stale reanalysis, next-step CTA) |
| `ProductionPhoneChangeAuthorityContractTest` | PASS (MBB authority chain) |
| `ProductionVNextShellContractTest` + `ProductionRecordNavigationContractTest` + `ProductionManualRelationshipContractTest` + `ProductionManualIdentityEstablishContractTest` | 24/24 PASS |
| `TabletAdaptiveContractTest` + `AndroidAdaptiveShellContractTest` | **15 run, 0 failed, 2 skipped** at font scale 1.0 — the two skips are the compact-window contracts, explicitly scoped; see closure doc §5 |
