# RUNTIME_EVIDENCE_INDEX.md

> 2026-09-26 multiclient sweep；组织：Platform → Device → Feature → Page → Screenshot → Test → Result
> 哈希：`EVIDENCE_SHA256SUMS.txt`（每张截图 sha256）；元数据：`screenshots.json`（15 字段/张）；装配 SHA 见 screenshots.json

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
| 页面视觉 | `artifacts/runtime-evidence/2026-09-26-multiclient-sweep/android/`（38 张：19 页×light/dark） | `UiScreenshotEvidenceTest`（MediaStore Downloads 方案） | PASS（A1 已解决） |

## Harmony（host）
| Feature | Evidence | Test | Result |
|---|---|---|---|
| host 142 | `run-conformance-host.mjs` 输出 | hvigor test | 142/142 PASS |
| HAP | `entry-default-unsigned.hap` sha256 80beb459… | assembleHap（clean） | SUCCESSFUL |
| preflight | `harmony_emulator_preflight.ps1` 输出 | — | BLOCKED（E-9） |
| 4 device-blocked | HARMONY_RUNTIME_FINAL_REPORT §3 记录表 | — | BLOCKED（E-9） |

## iOS（macOS runner run 36231032190）
| Feature | Evidence | Test | Result |
|---|---|---|---|
| build+canonical | `conformance/reports/ios.json`（pass=91 fail=0 total=91） | ios-runtime-visual.yml | PASS |
| simulator boot | `ios-simulator-boot.txt`（SIMULATOR_BOOT=PASS） | 同上 | PASS |
| app gap | `IOS_RUNTIME_BASELINE_AUDIT.md`；workflow 内 `ios-app-audit.json` | — | NOT_IMPLEMENTED |

## 汇总
- 截图总数（仓库内 curated）：**88**（Desktop 50 + Android 38）；哈希行 88；`screenshots.json` 88 条。
- 失败/缺陷：Desktop 修复 2（布局/重复行）；Android 修复 1（测试 flake）+ 1 方案级解决（A1 取回）；iOS workflow harness bug×1（已修复）。
- 剩余 Blocker：A2（emulator 稳定性，环境）、E-9（Harmony 镜像，外部）、E-1/E-2/AGC/Apple（外部）、N4_APP（iOS 范围）。