# Android 运行时安全证据（P0-3）

生成时间：2026-09-16
被测设备：`emulator-5554`（Android 14 / API 34）
被测 APK：`app-debug.apk`
SHA256：`bf378ec6678f04bf988921528b73ab879d9381a418d3a094ece2e60490305ff1`
体积：36,887,249 bytes
取证脚本：`local_private/security_evidence_v2.py`、`local_private/sec_settings.py`

本文只记录**真机运行时**可观测到的事实。无法在运行时观测到的部分一律标
PARTIAL，并写清原因，不用静态审查冒充运行时验证。

---

## 口径修正（重要，先读）

首轮取证脚本用 `dumpsys window windows | grep FLAG_SECURE` 统计，结果恒为 0，
一度被判成"截图保护未生效"。**这是取证口径的 bug，不是产品缺陷**：
Android 的 `dumpsys` 输出的是 flag 的裸名（`SECURE`），不带 `FLAG_` 前缀。

正确口径是解析 `com.pdig.app/.MainActivity` 窗口的 `fl=` 行，检查 token 集合中
是否含 `SECURE`；再用 `screencap` 像素统计做功能侧交叉验证。

---

## Gate 1 —— ANDROID_SCREEN_PROTECTION（FLAG_SECURE 按路由生效）

判定实现：`app/src/main/kotlin/com/pdig/app/ui/SecureWindow.kt`
（`isSensitiveRoute()` 是运行时与测试共用的唯一判定源）。

双重证据：

1. **窗口 flag**：`dumpsys window windows` 中 MainActivity 窗口 `fl=` 的 token 集合
2. **功能验证**：`screencap -p` 后统计像素均值 —— FLAG_SECURE 生效时 app 自己的
   window 区域会被 SurfaceFlinger 抹黑（状态栏/导航栏属 SystemUI 不受影响，
   所以 `max` 仍可能 > 0，判据取**均值**）

### 实测结果

| 路由 | 期望敏感 | 窗口 fl= 含 SECURE | 截图均值 | 截图判定 | 页面独有文本校验 | 结论 |
|---|---|---|---|---|---|---|
| HOME 首页 | 否 | 否 | 244.64 | 有内容 | 我的基础设施 | PASS |
| SETTINGS 设置 | 否 | 否 | 244.64 | 有内容 | 从备份恢复 | PASS |
| SOURCES 数据来源 | 是 | **是** | 0.17 | 被抹黑 | 导入账单文件 | PASS |
| IMPORT 导入账单 | 是 | **是** | 0.17 | 被抹黑 | 第 1 步 / 选择文件并解析 | PASS |
| INFRASTRUCTURE 我的基础设施 | 是 | **是** | 0.17 | 被抹黑 | 还没有记录任何对象 | PASS |
| BACKUP 备份 | 是 | **是** | 0.17 | 被抹黑 | 备份密码 / 生成加密备份 | PASS |

原始窗口 flag（敏感页）：

```
fl=DRAWS_SYSTEM_BAR_BACKGROUNDS HARDWARE_ACCELERATED LAYOUT_INSET_DECOR
   LAYOUT_IN_SCREEN SECURE SPLIT_TOUCH
```

原始窗口 flag（非敏感页，无 `SECURE`）：

```
fl=DRAWS_SYSTEM_BAR_BACKGROUNDS HARDWARE_ACCELERATED LAYOUT_INSET_DECOR
   LAYOUT_IN_SCREEN SPLIT_TOUCH
```

对照基线：启动器（`com.android.launcher3`）截图均值 69.23、体积 1.33 MB，
证明 `screencap` 通道本身正常，敏感页的 0.17 不是采集失败造成的。

**Gate 1 结论：PASS** —— 6/6 路由按预期设置/清除 FLAG_SECURE，
敏感页截屏被系统抹黑，非敏感页不受影响（可用性未被无意义地牺牲）。

诚实边界：FLAG_SECURE 只阻止系统截屏/录屏与最近任务缩略图，
**不等于**数据加密，也无法阻止已 Root 设备直接读取内存。

---

## Gate 2 —— App Lock / BiometricPrompt（spec §60）

**设备上实测状态（instrumentation 真机调用，非推断）：**

```
09-16 09:52:49.656 28914 28939 I System.out: APP_LOCK_STATE=NOT_CONFIGURED
```

来源：`RepositoryKeystoreEvidenceTest.appLock_stateIsDeterministicAndFailClosed`，
在设备上真实调用 `AppLock.state()`（走 `BiometricManager.canAuthenticate(BIOMETRIC_WEAK)`）。
本模拟器未录入任何生物特征，返回 `NOT_CONFIGURED` —— 与 fail-closed 要求一致：
没凭据时绝不报 `UNLOCKED`。

**但存在真实缺口：LockScreen 在 App 里没有入口。**

实现存在性（静态事实，非运行时证据）：

- `app/src/main/kotlin/com/pdig/app/security/AppLock.kt`
  —— `state()` 走 `BiometricManager.canAuthenticate(BIOMETRIC_WEAK)`；
  `authenticate()` 走 `BiometricPrompt`，用户取消与真实失败分开上报，两者都保持锁定（fail-closed）
- `app/src/main/kotlin/com/pdig/app/ui/screens/LockScreen.kt`
  —— 覆盖 LOCKED / NOT_CONFIGURED / UNAVAILABLE 三态

**运行时可达性：PARTIAL —— 存在真实缺口。**

`MainActivity.onCreate()` 固定调用 `PdigApp()`，而 `PdigApp` 的
`startDestination` 默认 `Route.HOME`；全仓库检索 **没有任何
`nav.navigate(Route.LOCK)`**，`Route.LOCK` 只被 `composable(Route.LOCK)` 注册。
也就是说：**LockScreen 写好了、也挂进了 NavGraph，但 App 在真机上没有任何入口
能调起它** —— 冷启动、前后台切换、设置页都没有接线。

因此在真机 UI 上无法产出 App Lock 的运行时证据。这不是"没测到"，
而是"App Lock 当前没有接进产品启动流程"，应作为 P0 缺口对待。

因此通过真机 UI 无法产出 App Lock 的运行时证据。这不是"没测到"，
而是"App Lock 没有接进产品启动流程"——应作为 P0 缺口对待，不能算作通过。

---

## Gate 3 —— 备份加密 / Keystore / 静态数据加密

设备端 instrumentation 测试 `connectedDebugAndroidTest`
（`PDIG_API34_DEFAULT(AVD) - 14`）：**19 tests / 0 failures / 0 errors / 0 skipped，BUILD SUCCESSFUL。**

与本门相关的用例全部通过：

| 测试类 | 用例 | 时间 |
|---|---|---|
| RepositoryKeystoreEvidenceTest | `keystore_rawKeyNeverReachesDisk` | 0.666s |
| RepositoryKeystoreEvidenceTest | `repository_transactionMechanismIsAtomic` | 0.482s |
| RepositoryKeystoreEvidenceTest | `repository_realityMutationBumpsGraphRevisionExactlyOnce` | 2.071s |
| RepositoryKeystoreEvidenceTest | `appLock_stateIsDeterministicAndFailClosed` | 0.221s |
| PersistenceEvidenceTest | `sqlcipher_wrongKeyIsRejected` | 1.559s |
| PersistenceEvidenceTest | `sqlcipher_plainSqliteCannotRead` | 0.914s |
| PersistenceEvidenceTest | `sqlcipher_transactionRollbackLeavesNoPartialWrite` | 0.760s |
| PersistenceEvidenceTest | `sqlcipher_openCloseReopen_persistsRows` | 1.609s |
| PersistenceEvidenceTest | `migration_v1_to_v3_preservesEverything` / `migration_v2_to_v3` | 3.960s / 4.467s |
| PersistenceEvidenceTest | `migration_failureRollsBack_andDatabaseNotWiped` | 2.775s |
| PersistenceEvidenceTest | `migration_futureSchemaVersionIsRejected_andDatabaseNotWiped` | 2.386s |
| DepmapRuntimeEvidenceTest | `depmap_exportImportRoundTrip_isByteIdentical` | 8.072s |
| DepmapRuntimeEvidenceTest | `depmap_wrongPasswordIsRejected` | 5.134s |
| DepmapRuntimeEvidenceTest | `depmap_tamperedCiphertextIsRejected` | 4.277s |
| DepmapRuntimeEvidenceTest | `depmap_v1AndV2PayloadsMigrateToTheSameV3Graph` | 2.165s |
| ScreenProtectionEvidenceTest | `everyRouteHasAnExplicitScreenProtectionDecision` | 0.015s |
| ScreenProtectionEvidenceTest | `unknownOrNullRouteIsNeverTreatedAsSensitive` | 0.075s |
| PerfSmokeEvidenceTest | `perfSmoke` | 83.443s |

host 侧交叉验证：真机上直接 `sqlite3` 打开应用库，返回
`Parse error: file is not a database` —— 库确实是加密的（SQLCipher），
明文 sqlite3 读不出来。

**Gate 3 结论：PASS**（加密、密钥、迁移回滚、备份容器完整性均在设备上被验证）。

**但需要在 P0-1 报告里一起看的缺陷**：备份导出时 UI 会误报
「备份失败：无法写入文件。」，而文件其实已完整落盘且可用正确密码恢复成功
（详见 `ANDROID_CORE_USER_JOURNEY_E2E_REPORT.md` F1）。这不导致数据丢失，但会误导用户。

---

## 复现方式

```bash
# 环境
export ANDROID_SERIAL=emulator-5554

# Gate 1（六路由对比，含像素校验）
python local_private/security_evidence_v2.py
python local_private/sec_settings.py          # 设置/备份两条路由的补充对照
```

产物：
- `local_private/secshots/sec_*.png` —— 各路由截图（敏感页应为全黑）
- `local_private/sec_window_dump.txt` —— 窗口 flag 原始 dump
- `local_private/security_evidence_v2.json` —— 结构化结果
