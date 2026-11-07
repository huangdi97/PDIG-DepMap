# Android 证据覆盖收口（2026-11-04）

> 事实来源：CI run `41802104675`（workflow `Android UI vNext Post-Freeze Production Evidence`，exact source `f40a0828`）的 `MANIFEST.json`。
> 本文只做**清单与分类**，不改变任何 Gate。`humanReview` 仍为 `NOT_RUN`，不得写成 PASS。

## 1. Production 面：22 张，断言齐全

| #   | shot_id                                   | state                                              | assertions | missing |
| --- | ----------------------------------------- | -------------------------------------------------- | ---------- | ------- |
| 0   | production-account-detail                 | governed identity hero                             | 2          | 0       |
| 1   | production-account-detail-change-boundary | formal Change unavailable; routed to Change Center | 3          | 0       |
| 2   | production-change-after-plan              | after = plan projection                            | 2          | 0       |
| 3   | production-change-center                  | plan list                                          | 3          | 0       |
| 4   | production-change-current                 | current                                            | 4          | 0       |
| 5   | production-change-pending-verification    | done but not verified                              | 3          | 0       |
| 6   | production-change-retirement-blocked      | retirement blocked                                 | 2          | 0       |
| 7   | production-change-retirement-eligible     | retirement eligible / destructive step identity    | 1          | 0       |
| 8   | production-change-retirement-eligible-cta | retirement eligible / next action                  | 1          | 0       |
| 9   | production-device-empty                   | governed empty state                               | 2          | 0       |
| 10  | production-email-detail                   | governed identity hero                             | 2          | 0       |
| 11  | production-email-detail-change-boundary   | formal Change unavailable; routed to Change Center | 3          | 0       |
| 12  | production-infrastructure                 | hub / first viewport                               | 1          | 0       |
| 13  | production-lock                           | locked                                             | 2          | 0       |
| 14  | production-now                            | unlocked                                           | 6          | 0       |
| 15  | production-records                        | records workspace / secure semantic proof          | 1          | 0       |
| 16  | production-records-trace                  | recorded history / three-clock truth               | 6          | 0       |
| 17  | production-service-detail                 | governed identity hero                             | 2          | 0       |
| 18  | production-service-detail-change-boundary | formal Change unavailable; routed to Change Center | 3          | 0       |
| 19  | production-change-reanalyzed              | reanalyzed / authoritative delta                   | 6          | 0       |
| 20  | production-change-stale                   | stale / blocked authority state                    | 2          | 0       |
| 21  | production-change-stale-boundary          | stale / fail-closed explanation                    | 1          | 0       |

合计断言 55 条，`assertionsMissing = 0`。

## 2. 旧「28 张 NOT_RUN」的去向

那份清单出自 2026-10-21 的本地证据包，路径在 `artifacts/runtime-evidence/2026-10-21-android-postfreeze-closure/`，
**该目录被 `.gitignore` 忽略**（repo 内不可读），因此无法在仓库内逐条复核。
结论以 CI 的 22 张 machine-asserted 证据为准；旧数字不再作为当前状态引用。

## 3. 7 张 phone-change 状态：Production 面已有等价取证

`WORK_STATUS.md` 记为 `NOT_RUN` 的 7 张 phone-change 状态序列，及其在 Production 面的对应：

| 声明缺的序列          | Production 面证据                                |
| --------------------- | ------------------------------------------------ |
| current               | `production-change-current`                      |
| pending / done 未验证 | `production-change-pending-verification`         |
| retirement blocked    | `production-change-retirement-blocked`           |
| retirement eligible   | `production-change-retirement-eligible` + `-cta` |
| stale                 | `production-change-stale` + `stale-boundary`     |
| reanalyzed            | `production-change-reanalyzed`                   |
| after（计划投影）     | `production-change-after-plan`                   |

**仍未取证的是 phone-preview 包这 7 张本身**：需要 preview APK 在设备上运行，CI 没有 preview job。
按仓库既定纪律（不得用异源截图顶替），此项保持 `NOT_RUN`，不作为已关闭。

## 4. Tablet exact-head 重跑（Reference Freeze 等待项）

- 原状：CI 只有 `phone-production-evidence`（`profile: pixel_7` 硬编码），tablet 只能在本地 AVD 跑。
- 本次改动：workflow 的 emulator profile 参数化，`profile` 进入 matrix，新增 `tablet` / `tablet-font15`（`pixel_tablet`）。
- 状态：**等待本次 CI 结果**。绿 → 该 targeted 重跑成立；红 → 失败原因（profile 不存在 / 横向断言不符）会作为下一步输入，不得先写 PASS。

## 5. 未变更的 Gate

```
VISUAL_CANDIDATE            = READY_FOR_HUMAN
HUMAN_VISUAL_ACCEPTANCE     = NOT_RUN
ANDROID_REFERENCE_FREEZE    = HOLD（等待 §4 结果）
STORE_RELEASE               = NOT_READY
```
