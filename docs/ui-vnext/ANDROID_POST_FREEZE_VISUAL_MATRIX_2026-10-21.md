# ANDROID POST-FREEZE VISUAL MATRIX — 2026-10-21

Source head `a3049c64` · productionDebug APK `6512acb5…` · evidence root
`artifacts/runtime-evidence/2026-10-21-android-postfreeze-closure/`
Every row was captured on a real API36 emulator and paired with a UIAutomator XML
snapshot; the strings in "asserted" are the exact on-screen labels the capture
script refuses to continue without.

`humanReview` for every row: **NOT_RUN**. Nothing here is a visual acceptance.

## A. Production — lock, shell, identity (API36 phone, 393×851 dp)

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

## F. Not captured this round (honest gaps)

The 28 ids of the §27 phone **preview** catalogue (`01-now` … `28-search`) are
present in `MANIFEST.json` with `status: NOT_RUN` and a reason each. No older
screenshot was renamed to fill them; the Production surfaces and the full Change
journey were captured instead.

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
