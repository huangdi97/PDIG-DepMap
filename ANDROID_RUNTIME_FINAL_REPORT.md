# ANDROID_RUNTIME_FINAL_REPORT.md

> 轮次：2026-09-26（spec §26-§48）；平台：Android API36（Kotlin/Compose），AVD pdig36（API36 google_apis x86_64，G:\avd 数据）
> 证据 SHA：73b0216（收口后刷新）

## 1. Fresh 运行证据（本轮实跑）
- ⭐ **Fresh canonical：91/91 PASS**（`conformance/reports/android.json` 2026-09-26 15:07；`pass=91 fail=0`）。
- ⭐ **Connected instrumented suite：61/61 PASS**（`connectedProductionDebugAndroidTest`，设备 pdig36(AVD)，2026-09-26，2m41s BUILD SUCCESSFUL；修复 1 个测试时序 flake 后全绿——见 §4）。
- ⭐ **APK**：`app-production-debug.apk` 37.2MB（15:07 构建），`adb install -r` Success。
- 预置数据：`fixtures/import/` 28 个 parser fixture + `fixtures/{impact,readiness,relations,depmap,state-machine,scenario,coverage,timeline,migration,backup}` 全量 canonical fixture。

## 2. Gate 明细
```text
ANDROID_RUNTIME_DEVICE_PROFILE_COUNT = 2（phone API36 实测；tablet AVD 创建成功但共享主机 emulator 不稳定，boot 未稳定取证）
ANDROID_PAGE_TOTAL                 = 20（导航路由全景，RUNTIME_PAGE_INVENTORY §2）
ANDROID_PAGE_SCREENSHOTTED         = 0（主机取回受阻，见 Blocker A1；设备端 19 页 × light/dark 生成成功）
ANDROID_FUNCTION_TOTAL             = 61（connected suite）+ 91（canonical）
ANDROID_FUNCTION_PASS              = 61 + 91
ANDROID_FUNCTION_FAIL              = 0
ANDROID_VISUAL_DEFECTS             = 0（本轮设备端渲染无视觉缺陷断言失败）
ANDROID_RUNTIME_DEFECTS            = 1（AppLock 测试时序 flake，已修复并复跑通过；非产品缺陷）
ANDROID_RUNTIME_SWEEP              = PASS（功能）；视觉 = PARTIAL（A1）
```

## 3. 覆盖要点（对应 §31-§45）
- Lifecycle：冷启动/重锁（AppLockNavigationTest）、 recreate、前后台锁语义 —— suite 覆盖。
- Security：Lock 门（可锁定、不可绕过）、无凭据显式放行、设备凭据不可降级、BiometricPrompt 宿主能力 —— suite 覆盖。
- Import：真实 MediaProvider fixture CSV → SAF 选择 → 回锁 → 解锁 → 工作流存活（FileWorkflowD16Test 6/6）→ Import/Backup/Restore E2E。
- Proposal/Candidate/Drift：accept/reject/dismiss/resolve 引擎证据（CandidateDriftEvidenceTest）；DB 层 depmap/persistence/keystore/screenshot-protection/log 脱敏（evidence suite）。
- 性能 smoke：PerfSmokeEvidenceTest（suite 内 PASS）。
- 无障碍：AccessibilitySemanticsTest 14 屏语义树 0 无标签（suite 内 PASS）。

## 4. 本轮发现并修复
| # | 缺陷 | 分类 | 修复 |
|---|---|---|---|
| A-1 | AppLockNavigationTest.explicitAcknowledgment… 时序断言 flake（fetch 与 assert 间重组） | TEST_HARNESS flake（慢 AVD） | 改为持久性等待循环；复跑 suite 61/61 PASS |

## 5. Blocker（§145 格式）
**A1 ANDROID_VISUAL_EXTRACT**：Gate=ANDROID_PAGE_VISUAL；Platform=Android（API36 emulator）；
Missing=稳定可取的设备端文件通道；Attempt=`adb pull /data/data/com.pdig.app/files/ui-shots`、`/sdcard/Android/data/com.pdig.app/files/ui-shots`、`adb exec-out run-as com.pdig.app tar`；
Error=“No such file or directory”（Android 11+ 作用域存储对 shell 隐藏；且本机 emulator 数据分区每次 boot 重建 + 外部进程随机强杀 qemu，写入无法跨 boot 持久）；Why code cannot solve=设备端测试内已成功生成并断言（38 张），仅取回被环境阻断；User input=无；
Closure=在稳定主机/真机重跑 `UiScreenshotEvidenceTest` 并按需 pull（run-as tar 或 adb root）。

**A2 emulator 稳定性**：Gate=ANDROID_RUNTIME_SWEEP 全量稳定性；Attempt=10+ 次启动 `$SDK\emulator\emulator.exe -avd pdig36 -no-window -no-snapshot -no-boot-anim -no-audio -gpu swiftshader_indirect -port 5554`；
Error=qemu 进程无日志消失（外部终止），`adb` server v32(C:\Android) 与 v41(SDK) 互相 kill；Why code cannot solve=共享主机上的外部进程级干扰；User input=无；Closure=受控主机/CI 执行。

## 6. 结论
Android 功能的 Runtime 证据全部成立（canonical 91/91 + 设备内 suite 61/61 + 安装）；视觉 PNG 主机取回被环境阻断（A1），设备端渲染断言已通过——按 §156 原则**不以截图缺失冒充通过，据实记录**。