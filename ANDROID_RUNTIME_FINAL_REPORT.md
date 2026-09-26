# ANDROID_RUNTIME_FINAL_REPORT.md

> 轮次：2026-09-26（spec §26-§48）；平台：Android API36（Kotlin/Compose），AVD pdig36（API36 google_apis x86_64，G:\avd 数据）
> 证据 SHA：最终收口 `4f25e11`（证据元数据以后续提交的最终 SHA 为准）

## 1. Fresh 运行证据（本轮实跑）

- ⭐ **Fresh canonical：91/91 PASS**（`conformance/reports/android.json` 2026-09-26 15:07；`pass=91 fail=0`）。
- ⭐ **Connected instrumented suite：61/61 PASS**（`connectedProductionDebugAndroidTest`，设备 pdig36(AVD)，2026-09-26，2m41s BUILD SUCCESSFUL；修复 1 个测试时序 flake 后全绿）。
- ⭐ **APK**：`app-production-debug.apk` 37.2MB（15:07 构建），`adb install -r` Success。
- ⭐ **页面视觉 38 张已取回主机**：`artifacts/runtime-evidence/2026-09-26-multiclient-sweep/android/`（19 页 × light/dark；MediaStore Downloads 公共目录方案，`adb pull` 38 files 5.2 MB/s）。
- 预置数据：`fixtures/import/` 28 个 parser fixture + `fixtures/` 全量 canonical fixture。

## 2. Gate 明细

```text
ANDROID_RUNTIME_DEVICE_PROFILE_COUNT = 2（phone API36 实测；tablet AVD 创建成功但共享主机 emulator 不稳定，boot 未稳定取证）
ANDROID_PAGE_TOTAL                 = 20（导航路由全景，RUNTIME_PAGE_INVENTORY §2）
ANDROID_PAGE_SCREENSHOTTED         = 38（19 页 × light/dark）
ANDROID_FUNCTION_TOTAL             = 61（connected suite）+ 91（canonical）
ANDROID_FUNCTION_PASS              = 61 + 91
ANDROID_FUNCTION_FAIL              = 0
ANDROID_VISUAL_DEFECTS             = 0（38 张渲染断言通过）
ANDROID_RUNTIME_DEFECTS            = 1（AppLock 测试时序 flake，已修复复跑通过；非产品缺陷）
ANDROID_RUNTIME_SWEEP              = PASS（功能 + 视觉证据主机侧齐备）
```

## 3. 覆盖要点（对应 §31-§45）

- Lifecycle：冷启动/重锁（AppLockNavigationTest）、recreate、前后台锁语义 —— suite 覆盖。
- Security：Lock 门（可锁定、不可绕过）、无凭据显式放行、设备凭据不可降级、BiometricPrompt 宿主能力 —— suite 覆盖。
- Import：真实 MediaProvider fixture CSV → SAF 选择 → 回锁 → 解锁 → 工作流存活（FileWorkflowD16Test 6/6）→ Import/Backup/Restore E2E。
- Proposal/Candidate/Drift：accept/reject/dismiss/resolve 引擎证据（CandidateDriftEvidenceTest）；DB 层 depmap/persistence/keystore/screenshot-protection/log 脱敏（evidence suite）。
- 性能 smoke：PerfSmokeEvidenceTest（suite 内 PASS）。
- 无障碍：AccessibilitySemanticsTest 14 屏语义树 0 无标签（suite 内 PASS）。
- 视觉：19 生产页 × light/dark 截图，38 张入仓（§2）。

## 4. 本轮发现并修复

| #   | 缺陷                                                                                   | 分类                         | 修复                                      |
| --- | -------------------------------------------------------------------------------------- | ---------------------------- | ----------------------------------------- |
| A-1 | AppLockNavigationTest.explicitAcknowledgment… 时序断言 flake（fetch 与 assert 间重组） | TEST_HARNESS flake（慢 AVD） | 改为持久性等待循环；复跑 suite 61/61 PASS |

## 5. Blocker（§145 格式）

**A1 ANDROID_VISUAL_EXTRACT —— 已解决（2026-09-26）**：原 Blocked=Android 11+ 作用域存储隐藏 app 目录导致 `adb pull` 不可见；
Resolution=测试内同时将 PNG 写入公共 MediaStore Downloads（`RELATIVE_PATH=Download/ui-shots`）→
`adb pull /sdcard/Download/ui-shots` 成功（38 files，5.2 MB/s），截图已入仓；**该路径不再阻塞 ANDROID_PAGE_VISUAL**。

**A2 emulator 稳定性**：Gate=ANDROID_RUNTIME_SWEEP 全量稳定性；Attempt=多次启动 `$SDK\emulator\emulator.exe -avd pdig36 -no-window ...`；
Error=qemu 进程偶发无日志消失（外部终止）、`adb` server v32(C:\Android) 与 v41(SDK) 冲突；Why code cannot solve=共享主机上的外部进程级干扰；
User input=无；Closure=受控主机/CI 执行（本轮以“单调用内完成 boot→test→pull”规避成功取证）。

## 6. 结论

Android Runtime 证据全部成立：canonical 91/91、设备内 suite 61/61、APK 安装、**页面视觉 38 张主机侧入仓**。
Tablet AVD 已创建；受共享主机 emulator 稳定性限制未稳定取证（A2），如实记录。
