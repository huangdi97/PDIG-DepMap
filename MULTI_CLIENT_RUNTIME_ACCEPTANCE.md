# MULTI_CLIENT_RUNTIME_ACCEPTANCE.md

> 轮次：2026-09-26 multiclient runtime / functional / visual evidence sweep（规范 §137）
> 证据目录：`artifacts/runtime-evidence/2026-09-26-multiclient-sweep/`
> 机器可读矩阵：`runtime/RUNTIME_ACCEPTANCE_MATRIX.json`（292 行 = 73 特征 × 4 平台，25 字段/行）
> 单一数据源：`runtime/evidence/<platform>.json` → `tools/runtime/build-matrix.mjs` 生成；装配 SHA 见 screenshots.json

## 1. 平台 Gate 汇总
| Gate | 结果 | 主要证据 |
|---|---|---|
| DESKTOP_RUNTIME_SWEEP | **PASS** | --smoke 16/16；--shots 50/50（24 页 × ≥1 窗口档，3 分辨率档）；0 崩溃；修复 2 个布局缺陷 |
| ANDROID_RUNTIME_SWEEP | **PASS** | fresh canonical 91/91；connected 61/61 PASS（API36 pdig36 AVD）；APK install Success；页面视觉 38 张（19 页×light/dark）主机侧入仓（A1 已解决：MediaStore Downloads 方案） |
| HARMONY_RUNTIME_SWEEP | **host PASS / runtime BLOCKED(E-9)** | host 142/142；canonical 87/91 (host, fail=0)；clean assembleHap PASS（HAP sha256 80beb459…）；4 条 device-blocked 逐条记录 |
| IOS_RUNTIME_SWEEP | **build+canonical PASS / UI NOT_IMPLEMENTED** | macOS runner：fresh canonical pass=91 fail=0；SIMULATOR_BOOT=PASS；N4 app gap |
| MULTI_CLIENT_RUNTIME_SWEEP | **PASS（可执行项全闭环）＋受限项如实记录** | 见 §4 |

## 2. 逐端 Gate 明细
### Desktop（DESKTOP_RUNTIME_FINAL_REPORT.md）
```text
DESKTOP_PAGE_TOTAL=24  PAGE_SCREENSHOTTED=24  FUNCTION_TOTAL=16  FUNCTION_PASS=16
CORE_JOURNEY=PASS  THREE_SCENARIOS=PASS  DESKTOP_RUNTIME_SWEEP=PASS
```
截图：`artifacts/.../desktop/`（50 PNG；dark=NOT_IMPLEMENTED；2560×1440 受物理屏 2048×1152 限制，以 2048×1152 实测并如实标注）。

### Android（ANDROID_RUNTIME_FINAL_REPORT.md）
```text
ANDROID_CANONICAL=91/91 (fresh)   ANDROID_CONNECTED_SUITE=61/61 PASS
ANDROID_APK=production-debug 37.2MB install Success
ANDROID_PAGE_SCREENSHOTTED=38（19 页×light/dark，主机侧入仓；A1 已解决）
ANDROID_RUNTIME_SWEEP=PASS（功能 + 视觉）
```

### Harmony（HARMONY_RUNTIME_FINAL_REPORT.md）
```text
HARMONY_HOST=142/142 PASS (fresh)   HARMONY_CANONICAL=87/91 (host, fail=0)
HARMONY_DEVICE_BLOCKED=4（depmap-golden-v1 / depmap-utf8-password-normalization /
                       migration-db-v1-to-v3 / backup-depmap-export-restore-roundtrip）
HARMONY_HAP=clean assembleHap SUCCESSFUL，entry-default-unsigned.hap 3.4MB sha256 80beb459…
HARMONY_RUNTIME=BLOCKED（E-9：emulator 系统镜像缺失；preflight 实拍证据）
```

### iOS（IOS_SIMULATOR_FINAL_REPORT.md）
```text
IOS_BUILD=PASS（fresh）   IOS_CANONICAL=PASS（pass=91 fail=0 total=91，runner run 36231032190）
IOS_SIMULATOR_IPHONE=SIMULATOR_BOOT=PASS
IOS_SIMULATOR_APP_RUNTIME / UI / 截图 = NOT_IMPLEMENTED（无 app target → N4_APP gap，不伪造）
```

## 3. 受限项（BLOCKED / NOT_IMPLEMENTED 精确记录，§145 格式）
| Blocker | Platform | 缺失环境 | 尝试命令 | 结果/错误 | 为何代码无法解决 | 用户输入 | 关闭方式 |
|---|---|---|---|---|---|---|---|
| A1 ANDROID_VISUAL_EXTRACT | Android | Android 11+ 作用域存储隐藏 app 目录 | `adb pull /data/data/...`、`/sdcard/Android/data/...`、`adb exec-out run-as tar` | “No such file or directory” | 环境；已用 MediaStore Downloads 公共目录方案解决 | 无 | **已解决**：`adb pull /sdcard/Download/ui-shots` → 38 files |
| A2 emulator 稳定性 | Android | 共享主机外部进程干扰 | `emulator -no-window ...` 多轮 | qemu 偶发无日志消失；adb v32/v41 冲突 | 环境级 | 无 | 受控主机/CI；本轮单调用内完成 boot→test→pull 已成功取证 |
| E-9 HARMONY_RUNTIME | Harmony | DevEco emulator 系统镜像（需华为账号） | `harmony_emulator_preflight.ps1`；`hdc list targets` | deploy dir absent=no .img；hdc [Empty] | 镜像不随 SDK 分发 | 华为账号 + 下载镜像 | 镜像就位 → 装 HAP → 4 条 canonical 实跑 |
| E-1 ANDROID_REAL_DEVICE | Android | 真机 | — | — | 环境 | 真机 | 按验收计划 |
| E-2/AGC/Apple 签名 | 三端 | keystore/AGC/Apple 账号 | — | — | 外部 | 用户 | 签名就位 |
| N4_APP | iOS | 无 iOS app target | 审计 `ios/Sources` | 仅 SwiftPM 库 | iOS N4 属后续轮 | — | N4 轮 |

## 4. 完成判定（§150-151）
- Desktop：**完成**。Android：**完成**（功能 61/61 + canonical 91/91 + 视觉 38 张入仓；tablet emulator 稳定性 A2 如实记录）。
- Harmony：**执行到环境极限**（host 142/142、HAP、canonical 87/91 host；runtime 唯一外部阻断 = E-9 镜像）。
- iOS：**runner 可执行部分完成**（build/canonical/simulator-boot 全实证）；app/UI 属 N4 gap（不伪造）。
- `MULTI_CLIENT_RUNTIME_SWEEP = PASS（条件性）`：四端在各自环境极限内全部执行、验证、留痕；剩余受限均为真实外部环境（E-9/E-1/签名/N4）而非证据缺失。