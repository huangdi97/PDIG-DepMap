# RUNTIME_EVIDENCE_INDEX.md

> 2026-09-26 multiclient sweep；组织：Platform → Device → Feature → Page → Screenshot → Test → Result
> 哈希：`EVIDENCE_SHA256SUMS.txt`（每张截图 sha256）；元数据：`screenshots.json`（15 字段/张）；装配 SHA：73b0216

## Desktop（win11）
| Feature | Page(s) | Screenshot | Test | Result |
|---|---|---|---|---|
| 全功能 16 step | gate…about（24 页） | `artifacts/runtime-evidence/2026-09-26-multiclient-sweep/desktop/*.png`（50） | `--smoke` 16/16；`--shots` 50/50 | PASS |
| 3 分辨率档 | home/attention/sources/infra/scenarios/impact/plan/verification/timeline/backup/restore/settings/import | 13×2=26 张 | --shots | PASS |

## Android（API36 emulator pdig36）
| Feature | Evidence | Test | Result |
|---|---|---|---|
| canonical | `conformance/reports/android.json`（15:07） | `:conformance:run` | 91/91 PASS |
| runtime UI 全链 | suite 报告（61 tests）；JUnit/HTML at `C:\Users\Kaiser\pdig-build\app\reports\androidTests\...\production\` | `connectedProductionDebugAndroidTest` | 61/61 PASS |
| APK | `app-production-debug.apk` 37.2MB；install Success | adb install | PASS |
| 页面视觉 | 设备端生成 38 张（19 页×light/dark）→ 主机取回 BLOCKED(A1) | `UiScreenshotEvidenceTest` | 设备端 PASS / 取回受限 |

## Harmony（host）
| Feature | Evidence | Test | Result |
|---|---|---|---|
| host 142 | `run-conformance-host.mjs` 输出（harmony-host3.log） | hvigor test | 142/142 PASS |
| HAP | `entry-default-unsigned.hap` sha256 80beb459… | assembleHap（clean） | SUCCESSFUL |
| preflight | `harmony_emulator_preflight.ps1` 输出 | — | BLOCKED（E-9） |
| 4 device-blocked | §54 记录表（HARMONY_RUNTIME_FINAL_REPORT §3） | — | BLOCKED（E-9） |

## iOS（macOS runner）
| Feature | Evidence | Test | Result |
|---|---|---|---|
| build+canonical | run 36231032190（in_progress 时记录） | ios-runtime-visual.yml | 待回填 |
| app gap | `IOS_RUNTIME_BASELINE_AUDIT.md`；workflow 内 `ios-app-audit.json` | — | NOT_IMPLEMENTED |

## 汇总
- 截图总数（仓库内 curated）：**50**（desktop）；哈希行 50；`screenshots.json` 50 条。
- 失败/缺陷：Desktop 修复 2（布局/重复行）；Android 修复 1（测试 flake）；产品功能断言失败 0。
- Blocker：A1（Android 视觉取回，env）、A2（emulator 稳定性，env）、E-9（Harmony 镜像，外部）、E-1/E-2/AGC/Apple（外部）、N4_APP（iOS 范围）。