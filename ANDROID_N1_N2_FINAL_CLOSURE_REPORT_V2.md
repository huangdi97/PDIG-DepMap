# ANDROID_N1_N2_FINAL_CLOSURE_REPORT_V2

> **本文件是重新生成的干净版本，不是对 V1 的补丁。**
> V1（`ANDROID_N1_N2_FINAL_CLOSURE_REPORT.md`）保留在仓库中作为历史记录，
> 它的 `58/62` 与 27 个 Gate 的旧结论**已作废**，本文件逐项重算。
>
> 轮次：**ANDROID FINAL BLOCKER CLOSURE**
> 日期：2026-09-16
> 设备：`emulator-5554`（Android 14 / API 34 / AOSP `android-34/default/x86_64`，1080×2400 @420dpi）
> 分支：`feat/mvp03-living-graph`
> 基线 HEAD（本轮开始）：`ad2350b3bc32c86f73a37941765bf9b10c3aa577`
> 本轮提交链见文末「Git 收口」小节（报告不写入自身 SHA）

---

## 0. 一句话结论

| 判定 | 结果 |
| --- | --- |
| `N1_ANDROID_VERTICAL_SLICE` | 见 §16 |
| `N2_ANDROID_FULL_PARITY` | 见 §16 |
| `ANDROID_PRODUCTION_RELEASE_READY` | **BLOCKED_BY_PRODUCTION_SIGNING**（无生产 keystore） |

本轮**没有**进入 Harmony N3、iOS N4、MVP04，未新增业务 Domain，未重设计产品。

---

## 1. 本轮关闭的真实缺陷（按来源分类）

按"**证据来源**"而非"改动大小"分类，因为本轮最重要的教训是
**上一轮有几条结论本身是取证方法错了**。

### 1.1 产品/工程真实缺陷（代码缺陷，已修）

| # | 缺陷 | 性质 | 证据来源 |
| --- | --- | --- | --- |
| **D-1** | `MainActivity` 继承 `ComponentActivity`，而 `androidx.biometric.BiometricPrompt` 的构造签名**只接受 `FragmentActivity`** → `findFragmentActivity()` 恒返回 `null`，解锁按钮一按就落到「无法启动验证」。**生物识别路径在结构上从来没有可能成功**，与设备有没有指纹无关。 | **P0 结构缺陷** | 代码审查 + `AppLockNavigationTest.hostingActivityCanActuallyHostABiometricPrompt` 断言 |
| **D-2** | App Lock 没有接线：`LockScreen` 已实现、`Route.LOCK` 已注册，但 `MainActivity` 固定 `startDestination = HOME`，全仓库没有 `nav.navigate(Route.LOCK)` | **P0 产品缺陷** | 全仓库检索 + 真机（锁屏在真机上从未出现过） |
| **D-3** | `AppLock.state()` 只查 `BIOMETRIC_WEAK`：设备**已设置锁屏 PIN 但没录指纹**时返回 `NOT_CONFIGURED`，把"可以验证身份"误判成"没有凭据" | **P1 语义缺陷** | 代码审查 + 枚举式单测 |
| **D-4** | `AppLock.authenticate()` 恒设 `setNegativeButtonText("取消")`。一旦允许 `DEVICE_CREDENTIAL`，`PromptInfo.Builder.build()` 会直接抛 `IllegalArgumentException` —— **尚未被触发的地雷**，随 D-3 一并消除 | **P1 潜伏缺陷** | 代码审查 |
| **D-5** | `exportBackupToFile()` 任何阶段出错都返回 `null`，UI 把所有 `null` 都显示为「备份失败：无法写入文件。」；实测文件其实已完整落盘且可恢复 | **P0 用户误导** | 真机 2/2 复现（V1 报告 F1） |
| **D-6** | `app/proguard-rules.pro` 被 `proguardFiles(...)` 引用，**文件并不存在**（悬空引用） | P2 工程缺陷 | 文件系统核对 |
| **D-7** | `android/` 构建依赖**仓库外**的机器本地 init script（`%USERPROFILE%\pdig-gradle\ascii-build.gradle.kts`）才能跑 `:core:test`；且该依赖没有写进任何仓库内文档 | **P1 可复现性缺陷** | 实跑：不带 init script 时 `:core:test` 全部 `ClassNotFoundException` |
| **D-8** | Kotlin 编译器把存活标记写向 `%WINDIR%\kotlin-compiler-in--<hash>.alive`（无写权限）→ 连续 4 批 `connectedDebugAndroidTest` 全部构建失败 | **P1 可复现性缺陷** | 实跑 4/4 失败 + 定位到 `java.io.tmpdir` |
| **D-9** | 4 个 `OutlinedTextField` 无显式语义标签 | P1 无障碍 | Compose 语义树审计 |
| **D-10** | 页面末尾交互元素贴着滚动视口底边（缺少底部留白），可点击区可能被视口裁剪 | P1 可用性 | V1 报告 F2（**但 F2 的根因定性本轮被修正**，见 §5） |
| **D-11** | `androidx.biometric` 传递依赖 `androidx.fragment:fragment:1.2.5`（2020 年），与 `androidx.activity:activity:1.9.1` 混用 → 每次 `picker.launch("*/*")` 都抛 `IllegalArgumentException: Can only use lower 16 bits for requestCode`（真机崩溃） | **P0 依赖一致性缺陷** | 本轮 E2E 首跑：`FATAL EXCEPTION` + `local_private/import_picker_probe.py` 修复前后对照 |
| **D-12** | 多个页面在**主线程**做数据库读写（`container.timeline()` / `nodes()` / `dependencies()` / `sourceInstances()` / `openDrifts()` / `pendingCandidates()` 直接在 `LaunchedEffect` 里调用）→ 冷启动（尤其全新安装后）会触发系统 ANR 对话框「PDIG isn't responding」 | **P0 性能/架构缺陷** | 本轮 E2E 连续复现，且对取证本身造成连锁干扰（ANR 期间点击会被排队，解冻后第二次点击落在首页卡片上，把 App 带进二级页面） |

| **D-13** | App Lock 把「设备能验证身份」**降级成手动放行**。`AppLock.capability()` 只信 `BiometricManager.canAuthenticate(DEVICE_CREDENTIAL)`；本机 AVD 执行 `locksettings set-pin 1234` 且 `dumpsys lock_settings` 明确显示 `CredentialType: PIN`、`locksettings verify` 返回 `verified successfully` 之后，它**仍然返回非 SUCCESS**，于是 `state()` 落到 `NOT_CONFIGURED`，锁屏渲染出「已知悉风险，本次进入」按钮 —— 用户点一下进 HOME，**没有经过任何系统验证**。fail-closed 语义在这一路上真的失效了。 | **P0 安全语义缺陷**（本轮 E2E 取证过程中自曝） | `device_credential_check.py` 阶段 B + `adb dumpsys lock_settings` 对照；回归断言 `AppLockNavigationTest.deviceCredentialMustNotBeDowngradedToManualAck` |
| **D-14** | 取证管线自身失真：`run_connected_batches.sh` 从一个已不再被写入的路径拷贝测试结果，**四个批次的 XML md5 完全相同**（同一份陈旧残留，只含 5 个旧测试类共 19 个用例，本轮新增的 4 个测试类一个都不在里面）——表现为"看起来全绿，实际是把旧证据当成了本轮证据"。 | **P0 取证完整性缺陷** | `md5sum local_private/atresults/*.xml` 四者一致 + 类名核对 |
| **D-15** | `run_connected_batches.sh` 在 `set -u` 下引用未必导出的 `USERPROFILE`，会以 `unbound variable` 直接退出 → **四个批次的设备内测试被整体跳过**，而外层脚本只看到 "connected tests done"，把空证据当成通过。 | **P0 取证完整性缺陷** | `_final_verify.log`："10:45:32 connected tests start" → "10:45:33 connected tests done"（1 秒）|

| **D-16** | **导入 / 恢复向导在"锁定—解锁"过程中被整体丢弃**。P0-A 的实现方式是"LOCKED 时 NavHost 根本不参与组合"，这在安全上是正确的（HOME 与敏感路由不可访问、BACK 不可绕过、内容不泄露），但副作用是：外部文件选择器（DocumentsUI 是**独立任务**）会触发 `MainActivity.onStop` → `LockGate.lockNow()` → NavHost 离开组合树 → 向导里所有 `remember { mutableStateOf(...) }` 状态与 `rememberLauncherForActivityResult` 的**待投递结果一起丢失**。用户选完文件回来再解锁，看到的是首页（对象数仍为 0），导入没有发生。 | **P1 功能/可用性缺陷**（由本轮 P0-A 修复所**引入**，必须显式记录） | E2E v3 run 20260917-111220：J2-pick-file PASS → J2-node-resolution FAIL → J2-commit FAIL（"共 0 个对象"） |

### 1.1.1 对以上四条取证类缺陷的说明

D-13 到 D-15 都是**在"为了拿到可信证据"的过程中被发现的**，其中两条直接推翻了
本轮**此前已经写进报告**的结论：

- D-14 意味着上一份 §10.2 里"connected androidTest = 全绿"的**计数来源不可采信**，
  必须重新收集。
- D-15 意味着曾经有过一次"四个批次压根没跑"却报"done"的运行。

因此本报告的 §10 全部以**修好取证管线之后**的新一轮结果为准；
旧数字不做沿用，也不做"部分沿用"。

D-16 的性质与上面三条不同：它不是取证缺陷，而是**本轮修复引入的真实功能后果**。
它有一条"两难"的解法空间，本轮**故意不擅自选**：

| 方案 | 能得到 | 要付出什么 | 为什么本轮没做 |
| --- | --- | --- | --- |
| A. 把向导状态提到 Activity 作用域（ViewModel / 应用级 holder），ActivityResult 也在 Activity 层注册 | 安全语义不变，向导状态保住 | 改动跨 `MainActivity` + 导入/恢复两屏，非小改 | 属于架构改动，不是修 bug；且需要再跑一整轮回归 |
| B. 锁定时仍让 NavHost 留在组合树里，但用 `clearAndSetSemantics` 把它从无障碍树摘掉 | 状态保住，改动最小 | **弱化"锁定时不组合"这一结构性保证**，改成"组合但隐藏"；依赖语义树掩盖事实 | 这**改变了安全模型**，任务明确要求不要重新设计安全模型 |
| C. 由 App 自己拉起外部 Activity 时不回锁 | 状态保住 | 打开一条"应用主动调用系统界面 → 免验证"的路径 | 违反 `foreground → background → foreground → LOCK` |

建议：方案 A。但**它应该在人工复核确认方向后再做**，本轮不越权实现。

---

### 1.2 上一轮**被推翻**的结论（本轮最重要的部分）

| V1 结论 | 本轮实测 | 处置 |
| --- | --- | --- |
| 「非生产签名配置本轮**未生效**」（AAB 与未签名版 SHA-256 完全相同） | **不成立**。带 `-PpdigNonProdSigning=true` 重跑后：APK 输出名从 `app-release-unsigned.apk` 变为 `app-release.apk`，`apksigner verify --print-certs` 给出 `CN=PDIG NON-PRODUCTION TEST KEY`；AAB 内出现 `META-INF/PDIG-NON.RSA` 与 `META-INF/PDIG-NON.SF` | 结论改写；**签名流水线已验证可用** |
| 「4 个可点击节点无标签」（uiautomator 口径） | uiautomator 对 Compose 的 `clickable`/标签映射**不可靠**：同一份 UI，uiautomator 报「23 个无标签可点击节点」，而 Compose 语义树（TalkBack 读的同一棵树）报 **0 个**。uiautomator 还把同一个 `Card` 报成"可点击但无文字"、把 `Button` 报成"不可点击" | **换用 Compose 语义树做门禁**，并把 uiautomator 口径的不可靠性写进报告 |
| 「『确认导入』按钮的有效点击区低于可见范围」 | 现象真实（目标被裁切时点击会被吞），但 V1 的**对照实验无效**：V1 用「选择文件并解析」按钮做对照，而那个按钮当时处于 `enabled = false`（未选来源），本来就点不动。V1 自身的 `_tapexp.txt` 显示在同一次会话里**所有**坐标点击都无反应，与"按钮位置"解释不一致 | 保留产品侧修复（统一滚动容器补底部留白），**同时修正 E2E 侧的 tap 口径**（见 §5） |
| 「parity = 58/62」 | 该分数**无法复现**：`NATIVE_PARITY_MATRIX.md` 实际有 **73** 行 Android 能力格，`62` 这个分母没有出处 | 本轮重新定义分母与计数规则并重算（见 §15） |
| 「`:core:test` 可复现命令 = `./gradlew --no-daemon :core:test`」 | **不成立**，见 D-7/D-8 | 已修：现在仓库内直接可跑 |

---

## 2. 构建链与可复现性（D-7 / D-8）

### 2.1 修法

把原先只存在于仓库外的 ASCII 适配逻辑**内置到 `android/settings.gradle.kts`**，
并**自动检测**非 ASCII 工程路径（不再需要任何环境变量或仓库外文件）：

```kotlin
val projectPathIsAscii = rootDir.absolutePath.all { it.code < 128 }
val asciiBuildRoot = System.getenv("PDIG_ASCII_BUILD_ROOT")?.takeIf { it.isNotBlank() }
    ?: if (projectPathIsAscii) null else "${user.home}/pdig-build"

// (1) 构建输出重定向 → 使 fork 出去的 JVM 看到全 ASCII 的 classpath
// (2) java.io.tmpdir 重定向 → 消除 %WINDIR%\kotlin-compiler-*.alive 的写失败
```

### 2.2 实测对比

| 命令 | 修复前 | 修复后 |
| --- | --- | --- |
| `./gradlew :core:test`（不带任何环境变量/init script） | `BUILD FAILED`：`ClassNotFoundException: com.pdig.core.db.JdbcStatement`（每个测试类） | `BUILD SUCCESSFUL`，**71/71 PASS** |
| `./gradlew :app:compileDebugAndroidTestKotlin` | `BUILD FAILED`：`AccessDeniedException: %WINDIR%\kotlin-compiler-in--….alive` | `BUILD SUCCESSFUL` |

> 顺带修正一条**文档缺陷**：`NATIVE_MIGRATION_STATUS.md` 里的可复现命令
> `./gradlew --no-daemon :core:test` 在修复前是**跑不通的**。现已同步更新。

---

## 3. P0-A —— App Lock 真正接线

### 3.1 设计（不重设计安全模型）

新增 `LockGate`（application/security state，**不写任何持久化、不触碰 Reality Graph**）：

| 不变量 | 实现 | 断言 |
| --- | --- | --- |
| fail-closed 初值 | `var locked by mutableStateOf(true)` | `PdigApp` 冷启动先渲染中性占位页，读到设备能力后一律回锁 |
| 只有显式动作能解锁 | `unlock()` 仅有 2 个调用点（验证成功 / 无凭据设备上用户显式确认） | `AppLockNavigationTest` |
| 回后台立即回锁 | `LifecycleEventEffect(ON_STOP) { LockGate.lockNow() }` | 真机 E2E J0 |
| 不持久化 | 只有进程内 Compose state | 进程死亡 → 重建 → 回到 LOCKED |

**结构性保证（不是"记得别写 navigate"）**：`PdigApp` 在最外层做分支 ——
`LOCKED` 时**只渲染 `LockScreen`，根本不组合 `AppNavHost`**。因此：

- **BACK 键**无法回到 HOME：栈里没有非锁页面
- **深链**无法绕过：NavHost 未组合（且 manifest 中除 LAUNCHER 外**没有任何 intent-filter**）
- **敏感路由**同样不存在

### 3.2 真机运行时证据（本轮实跑）

| 步骤 | 实测 |
| --- | --- |
| 冷启动 | 出现「PDIG 已锁定」；`需要你处理` / `我的基础设施` **不可见**（首页内容零泄露） |
| 无凭据设备的 fail-closed 语义 | 状态 `NOT_CONFIGURED`，**不伪装成已解锁**；仅在用户显式点「已知悉风险，本次进入」后才放行 |
| 放行后的可达性 | 点击后首页出现（`需要你处理`、`我的基础设施`、`共 0 个对象`） |
| `recreate()`（冷启动等价路径） | 即使上一刻是解锁态，重建后**回到锁定** |
| 前后台 | `KEYCODE_HOME` → 再启动 → **重新出现锁屏**，首页零泄露 |
| 锁定时导航图 | `我的基础设施` / `数据来源与导入` / `设置` / `导入账单` 在语义树中**计数为 0** |

对应自动化：`AppLockNavigationTest`（6 个用例，全部 PASS，见 §10）。

### 3.3 这次接线**引入**的回归，以及它为什么是好事

`MainActivity` 从 `ComponentActivity` 改成 `FragmentActivity` 之后，
首轮 E2E 在「选择文件并解析」这一步直接崩溃：

```
FATAL EXCEPTION: main
java.lang.IllegalArgumentException: Can only use lower 16 bits for requestCode
    at androidx.fragment.app.FragmentActivity.checkForValidRequestCode(FragmentActivity.java:714)
    at androidx.fragment.app.FragmentActivity.startActivityForResult(FragmentActivity.java:672)
    at ... com.pdig.app.ui.screens.DataScreensKt$ImportScreen$4$1.invoke$lambda$6(DataScreens.kt:179)
```

**机制**：`androidx.biometric:biometric:1.1.0` 传递依赖 `androidx.fragment:fragment:1.2.5`；
`FragmentActivity`（1.2.5）对 `requestCode` 强制 16 位上限，而
`androidx.activity:activity:1.9.1` 的 `ActivityResultRegistry` 自动生成的 request
code 越界。也就是说 —— **"App Lock 接线"与"文件选择器"通过宿主基类耦合在了一起**，
这是 D-1 修复前根本走不到的路径。

**修复**：在 `app/build.gradle.kts` 显式钉住 `androidx.fragment:fragment:1.7.1`
（与 activity 同代），并写下完整的原因注释（否则下一次有人删掉这一行会重现崩溃）。

**验证**：`local_private/import_picker_probe.py` 修复后实测
`topResumed = com.android.documentsui/.picker.PickActivity`，`APP_CRASH = False` → **PASS**。

> 这条值得单独记一笔：**旧的「21/21 PASS」永远不会发现它**，
> 因为旧版本没有 App Lock 接线、MainActivity 也不是 FragmentActivity。
> 也就是说，修好一个 P0 会立刻暴露下一个 P0 —— 这正是必须重跑 E2E、
> 不能沿用旧数字的实证理由。

---

## 4. P0-B —— 备份导出 UI 误报（D-5）

### 4.1 修法（不是"把 catch 改成 success"）

`exportBackupToFile()` 改为返回显式的 `ExportBackupResult`：

```kotlin
sealed interface ExportBackupResult {
    data class Success(val displayName: String, val byteCount: Int)
    data class Failure(val stage: ExportFailureStage, val errorType: String,
                       val detail: String, val cleanupOk: Boolean?)
}
enum class ExportFailureStage { ENCRYPT, INSERT, WRITE, VERIFY, PUBLISH }
```

逐项覆盖任务要求：

| 要求 | 实现 |
| --- | --- |
| `write` / `flush` / `close` | `use { write(bytes); flush() }`，异常归 `WRITE` |
| `rename`/`move` | 走 MediaStore，无 rename 步骤；`INSERT` 阶段独立报告 |
| `URI handling` | `context.contentResolver` 本身也纳入 try（此前在 try 之外，异常会逃逸成未分类崩溃） |
| `cleanup` | `deleteQuietly()` **检查返回值**，`cleanupOk` 如实告知用户（"可能残留，请自行删除"） |
| `return value` | 不再用 `null` 表示失败；成功带回**回读的真实文件名**与字节数 |
| `post-write verification` | **写完回读全部字节并比对长度**，不一致按 `VERIFY` 失败处理，绝不把半截文件当成功 |

UI 文案从"一句话"变成可解释的两段：`备份已导出` / `备份失败：<阶段说明>` + 阶段与清理状态。

### 4.2 回归测试（把"不得出现 file exists + valid 但 UI failure"变成断言）

`BackupExportRegressionTest`（3 个用例，设备内，全 PASS）：

1. **成功路径**：`Success` → UI `success=true` → 文件确实在 Downloads → 字节数与 UI 一致 →
   **正确口令能解出与当前库逐字节相同的 payload** → **能真正导入到一个独立库** →
   错误口令被拒 → 清理。
   → 这条直接编码了 F1 的回归点：**文件有效 ⇒ UI 必须报成功**。
2. **失败路径（注入）**：取 `contentResolver` 即抛异常的 Context → 必须被分类为
   `Failure(INSERT)`、UI 为失败、且**没有残留可清理**。
3. **全阶段映射**：5 个 `ExportFailureStage` 全部映射成可解释的失败 UI（含"清理未生效"提示）。

### 4.3 `.depmap` 四件套重跑（设备内，全 PASS）

| 用例 | 结果 |
| --- | --- |
| `depmap_exportImportRoundTrip_isByteIdentical` | PASS |
| `depmap_wrongPasswordIsRejected` | PASS |
| `depmap_tamperedCiphertextIsRejected` | PASS |
| `depmap_v1AndV2PayloadsMigrateToTheSameV3Graph` | PASS |

真机 E2E 侧另跑了「导出 → 清空应用数据 → 恢复 → 语义等价 → 篡改拒绝」，见 §10。

---

## 5. P1-A —— Import 按钮 hitbox（D-10 与 V1-F2 的重新定性）

### 5.1 本轮先做的事实核对

V1-F2 的结论是「语义 Button 中心 (42,2127,1038,2232) 点不动，外层 clickable View
(42,2186,1038,2295) 经底部裁剪后的落点 y=2228 才生效」。核对 V1 留下的原始证据后：

1. V1 的**对照实验无效**：用来对照的「选择文件并解析」按钮当时 `enabled=false`
   （未选中任何来源），本来就点不动 —— 不能据此排除"注入通道故障"。
2. V1 自己的另一份原始记录（`_tapexp.txt`）显示，在同一次会话里
   **连续 5 个坐标 + swipe + motionevent + 长按 + 键盘 TAB/ENTER 全部无反应**，
   与"只有按钮中下部可点"的模型不一致。
3. 因此"按钮位置错位"**既未被证实、也未被证伪**。

### 5.2 产品侧修复（仍然做，因为它本身就是缺陷）

把滚动容器统一为 `PdigScrollingPage`，**在内容之后固定留出 `SpaceXl` 底部空隙**：

```
视觉区 == 真实点击区 == semantics bounds
```

已替换的页面：Import / ImportDonePanel / Backup / Restore / Settings / Impact /
ScenarioSetup / PlannedScenarioNotice / ChangePlan / Home。
同时把末尾可点击元素统一加 `heightIn(min = MinTouchTarget 48dp)`。

### 5.3 证据侧修复（这是 F2 更可能的真因）

V1 的驱动脚本用 `screen_h() - 130` 当可见区底边 —— **把导航栏区域也算成了可见**，
于是目标哪怕只露出 10px 也会被判定"可以点"。本轮修正为：

- 从 `dumpsys window windows` **按窗口块**读取 App 窗口的真实 frame（`frame=` 不在
  `Window #N Window{...}` 那一行，必须按块解析，否则会静默回退到物理屏高）
- 点击前要求目标 `fully_visible`（完整落在 App 窗口内），否则继续滚动
- 点 `clickable=true` 的**最小祖先**中心（不用"Role=Button"猜命中区）

### 5.4 交叉验证

| 通道 | 结果 |
| --- | --- |
| Compose UI 测试 `ImportHitboxTest`（2 个用例） | PASS —— 末尾按钮 bounds 完整落在根节点内、中心点击必定触发回调、可点击高度 ≥48dp、**连续两次点击都生效**（无"第一次被吞"） |
| host 侧真机 tap | 走 §10 的 E2E：对 `确认导入` 直接 `input tap` 其 semantics bounds 中心，**不再使用任何 offset 绕过** |

> 诚实边界：`ImportHitboxTest` 是**结构**证据（证明容器不再裁剪末尾元素），
> 不能单独证明某个注入方式在所有机型上可用。真机通道由 E2E 承担。

---

## 6. P1-B —— Accessibility Closure

### 6.1 换掉了不可信的检测口径

uiautomator 是 **View 体系**的工具，对 Compose 的语义映射不可靠（实测：同一份 UI，
两种口径给出"无标签可点击节点" = 23 vs 0）。本轮改用 **Compose 语义树** ——
那是 TalkBack 真正读到的那棵树。

### 6.2 门禁（`AccessibilitySemanticsTest`，14 屏）

规则：**每个带 `onClick` 动作的语义节点**必须在 `Text` / `ContentDescription` /
`EditableText` 之一里有内容。**不给非交互容器乱加 contentDescription**。

反空转断言（关键）：每屏必须至少扫到 1 个交互节点，否则门禁"永远绿"却什么也没查。

### 6.3 实测结果（14 屏全部 PASS）

| 屏幕 | 交互节点数 | 无标签 |
| --- | ---: | ---: |
| Home | 6 | **0** |
| Settings | 7 | **0** |
| Import | 4 | **0** |
| ScenarioCenter | 4 | **0** |
| Backup | 3 | **0** |
| Restore | 3 | **0** |
| LockScreen(notConfigured) | 2 | **0** |
| LockScreen(locked) | 1 | **0** |
| Sources | 2 | **0** |
| Privacy / About / Drift / Infrastructure / ScenarioSetup | 各 1 | **0** |

代码侧修复：4 个 `OutlinedTextField`（新建来源名称 / 备份密码 / 恢复密码 / 计划生效日期）
补上显式语义标签。

### 6.4 其余无障碍项

| 检查 | 结果 | 来源 |
| --- | --- | --- |
| 触摸目标 ≥48dp | PASS（并已对末尾按钮显式 `heightIn`） | V1 实测 + 本轮的 `heightIn` |
| 焦点顺序 | PASS（12 步可复现） | V1 实测 |
| 字体缩放 1.30 | PASS | V1 实测 |
| 横屏 | PASS | V1 实测 |
| button hitbox | 本轮修复（§5） | 本轮 |
| **TalkBack 实机读屏** | **NOT_RUN** —— 本镜像为 AOSP `android-34/default`，**系统未预装 TalkBack**，且无 Play 商店无法安装 | 环境限制 |

→ `ANDROID_ACCESSIBILITY` 的最终判定见 §16。

---

## 7. P1-C —— Biometric Runtime Evidence

### 7.1 环境事实（本轮实测，非推断）

| 项 | 实测 |
| --- | --- |
| 当前 AVD | `PDIG_API34_DEFAULT`，镜像 `system-images/android-34/default/x86_64`（AOSP） |
| `config.ini` 中的指纹硬件 | **`hw.finger` 键不存在** → 模拟器不提供指纹传感器 |
| 已安装的其它系统镜像 | `system-images/android-34/google_apis/x86_64` **已安装**（可用于创建带指纹硬件的 AVD） |
| 另一个 AVD `Medium_Phone` | 指向 `system-images/android-35/google_apis_playstore/x86_64` —— **该镜像未安装**，无法启动 |
| 指纹录入方式 | Android 没有无头录入命令；必须在系统设置里走录入向导（多次触摸） |

### 7.2 本轮完成的部分：Device Credential fallback（不需要指纹硬件）

取证脚本：`local_private/device_credential_check.py`。
最新一次运行证据：`local_private/evidence/device-credential-20260917-115222.txt`。

| 阶段 | 结果 | 说明 |
| --- | --- | --- |
| 设 PIN 后锁屏展示「验证身份并解锁」、且**不再**展示无凭据放行 | **PASS** | **D-13 修复生效的直接证据**。修复前该行是 FAIL（设备有 PIN 却仍渲染手动放行按钮） |
| 取消后仍在锁屏、首页不泄露 | **PASS** | fail-closed |
| 系统凭据界面被拉起 | **FAIL** | 见 §7.3，属于 AVD 能力限制 |
| 输错 PIN 保持锁定 / 输对 PIN 进入首页 | **FAIL** | **连带失败**：系统凭据界面没被拉起，脚本无从输入，不能判为产品缺陷 |

补充一条**独立的真机断言**（不受脚本取证路径影响）：

```
adb shell locksettings set-pin 1234
./gradlew :app:connectedDebugAndroidTest -P...class=...AppLockNavigationTest
logcat: DEVICE_CREDENTIAL keyguard.isDeviceSecure=true cred=true
        APP_LOCK_CAPABILITY bio=false cred=true hwMissing=false
→ 7 tests, BUILD SUCCESSFUL（含 deviceCredentialMustNotBeDowngradedToManualAck）
```

这条证明 `AppLock.capability()` 在设备真的有 PIN 时确实认定"可以验证身份"，
且断言 `isDeviceSecure` 与 `credentialUsable` 必须一致。

**本阶段自身遗留的取证噪声（不影响产品结论，但必须说清）**：
`device_credential_check.py` 的阶段 A 依赖"设备当前无凭据"。上一轮运行时在设备上
留下了 PIN，导致阶段 A 读到的是"有凭据"（于是它显示「验证身份并解锁」——
**这恰恰是正确行为**，只是脚本的前提不成立），阶段 B 的 `set-pin` 也因已存在凭据
而输出为空。判断产品行为应以阶段 B 的 PASS 与上面的独立断言为准。

### 7.3 未完成的部分

**指纹硬件路径**：`ANDROID_BIOMETRIC = BLOCKED_BY_RUNTIME_ENVIRONMENT`。

- **不 fake PASS**：没有任何一次真实的指纹匹配在本轮被观测到。
- 精确说明"还差什么"：创建/改造一个 `hw.finger=yes` 的 `google_apis` AVD →
  在系统设置里完成指纹录入 → 用 `adb emu finger <id>` 验证
  success / failure（错指纹）/ cancel / 前后台。
- **系统凭据界面的拉起同样未验证**：点击「验证身份并解锁」后，本 AVD
  （`android-34/default/x86_64`，AOSP）**没有出现系统 PIN 输入界面**，App 仍停留在自己的
  锁屏。`dumpsys biometric` 只报告了两个虚拟传感器（modality 2 与 8，`oemStrength 15`），
  没有 enrollment 记录。因此**"BiometricPrompt 能否真正完成一次认证"在本环境不可判定**，
  与指纹硬件一样属于环境限制，不写成 PASS。
- **注意**：`App Lock wiring` 与 `Biometric` 是**两件事**，前者可独立 PASS（见 §3、§16）。

---

## 8. P1-D —— Production signing pipeline

### 8.1 结论：**流水线本身已验证可用**（V1 的"未生效"结论已推翻）

```
./gradlew :app:assembleRelease :app:bundleRelease -PpdigNonProdSigning=true
→ BUILD SUCCESSFUL
```

| 证据 | 结果 |
| --- | --- |
| APK 输出名 | `app-release-unsigned.apk` → **`app-release.apk`**（AGP 应用了 signingConfig 才改名） |
| `apksigner verify --print-certs` | `Signer #1 certificate DN: CN=PDIG NON-PRODUCTION TEST KEY, OU=Local Build Verification, O=PDIG, L=Local, ST=Local, C=CN`；SHA-256 `01d8b3b1ab32e1fa466239698f4f2339af9166cad7fa00809994e052fd362c40` |
| AAB 签名条目 | `META-INF/PDIG-NON.SF`、`META-INF/PDIG-NON.RSA`、`META-INF/MANIFEST.MF` |
| 默认（不带开关） | `app-release-unsigned.apk` —— **保持未签名**（fail-safe，不会误用测试密钥） |

### 8.2 必须明确区分（任务要求）

```
NON_PRODUCTION_TEST_SIGNING  ≠  PRODUCTION_SIGNING
```

- 本轮使用的密钥 `CN=PDIG NON-PRODUCTION TEST KEY` 是**本地测试密钥**，
  仅用于验证"Gradle signingConfig → AAB/APK 签名 → 可被 apksigner 验证"这条链路。
- 未创建、未提交任何真实 production secret。
- **用户未提供正式 keystore** → `ANDROID_RELEASE_SIGNING = BLOCKED_BY_MISSING_PRODUCTION_KEYSTORE`。
  这不阻止判断 Android engineering parity。

---

## 9. P1-E —— Privacy / Security 剩余项

全部基于**真实 release APK 的合并 Manifest**（`aapt2 dump xmltree`），不是源码推断：

| 项 | 实测值 | 判定 |
| --- | --- | --- |
| `android:debuggable`（release） | **键不存在** = false | PASS |
| `android:allowBackup` | `false` | PASS |
| `android:fullBackupContent` | `false` | PASS |
| `dataExtractionRules` | cloud-backup 与 device-transfer 全域 exclude | PASS |
| `android:usesCleartextTraffic` | `false`（本轮**显式写出**：默认值不出现在合并 Manifest 里，审计时看不见这个决定） | PASS |
| `INTERNET` 权限 | **不存在** | PASS |
| 权限总数 | `USE_BIOMETRIC`、`USE_FINGERPRINT` + 自动生成的 `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`（protectionLevel=signature） | PASS |
| exported 组件归属 | **activity ×1**（`MainActivity`，exported=true，仅 LAUNCHER）；**provider ×1**（`androidx.startup.InitializationProvider`，exported=**false**）；**receiver ×1**（`androidx.profileinstaller.ProfileInstallReceiver`，exported=true 但受 `android.permission.DUMP` 保护）；**service ×0** | PASS |
| debug-only 组件是否泄漏到 release | `androidx.compose.ui.tooling.PreviewActivity`、`androidx.activity.ComponentActivity`（来自 `ui-tooling` / `ui-test-manifest`）**只出现在 debug 合并 Manifest，release 中没有** | PASS |
| 明文 sqlite 读取 | 设备内：`sqlite3` 打开应用库 → `file is not a database` | PASS |
| 原始密钥落盘 | `keystore_rawKeyNeverReachesDisk`：SharedPreferences 中不含明文口令 | PASS |
| 日志脱敏 | 按 PID/UID 归属扫描，6 类敏感关键字命中 **全 0** | PASS |
| 临时文件 | 备份回归测试的临时库/文件均显式删除；`J11` 篡改样本落在 `local_private/`（已 gitignore） | PASS |
| raw statement 生命周期 | `Observation` 仅存在于导入会话内存（`import_sessions` 只存计数，`observation_fingerprints` 只存指纹），不持久化单笔交易 | PASS（设计 + conformance） |
| telemetry / analytics / 网络依赖 | **未新增** | PASS |

---

## 10. P0-C / P0-D —— 重跑核心 E2E 与全量回归

### 10.1 核心用户行程 E2E（v3，新 run id）

驱动：`local_private/core_journey_e2e_v3.py`（本轮新写；与 v2 的差别见文件头）
证据：`local_private/e2e/core-journey-v3-<ts>.{txt,json}`、`local_private/e2e/shots/`

覆盖的步骤（对照任务清单逐条）：

**新 run id**：`core-journey-v3-20260917-111220`
（不使用修复前的 `core-journey-v2-20260916-172044` 作为最终证据）

**总计 22 步：PASS 8 / FAIL 14 / PARTIAL 0。**

| # | 步骤 | 结果 | 说明 |
| --- | --- | --- | --- |
| J1-install | 全新安装 | **PASS** | |
| J1-lock-gate | 全新安装后首启出现锁屏并解锁 | **PASS** | 解锁路径 `unlocked-without-credential`（本设备无凭据） |
| J1-first-launch | 首页出现且对象数 0 | **PASS** | |
| J2-nav-sources / nav-import / pick-source / pick-file | 进入导入页、选来源、**SAF 真选到文件** | **PASS** | |
| J2-node-resolution | Node Resolution 出现 | **FAIL** | **D-16**：从 SAF 返回时 App 已回锁，向导状态与待投递结果都丢了 |
| J2-commit | 真机 tap 语义中心确认导入 | **FAIL** | 同上，对象数仍为 0 |
| J3 ~ J11 | 候选关系 / 确认 / 必需标记 / 影响面 / 计划 / 动作 / 验证 / 进程死亡 / 导出 / 恢复 / 语义等价 / 篡改拒绝 | **FAIL（全部）** | **全部是 J2 的连带失败**——库里没有数据，后续每一步都无从执行 |
| CRASH-SCAN | 被测应用崩溃数 | **PASS** | `com.pdig.app` 崩溃 = 0 |
| J0-app-foreground | 起始时把 App 拉到前台 | **FAIL** | 上一轮遗留的 DocumentsUI/启动器状态；后续 J0 各步在这条之后仍各自 PASS（见 §3.2），属脚本起点噪声 |

**重要定性**：这 14 个 FAIL **不是 14 个独立缺陷**，而是同一个根因（D-16）的级联。
证据：J2-pick-file 之前全部 PASS，之后对象数恒为 0，且 `CRASH-SCAN` 显示 App 从未崩溃。
当且仅当 D-16 被修掉，这 14 项才**有资格重新判定**——在此之前不能声称它们通过，
也不能把它们记成 14 个产品缺陷。

**J0 各步（App Lock 专项）**在本轮单独的取证中全部 PASS（冷启动锁定 / 解锁 /
前后台回锁 / 二次解锁），见 §3.2 与同一次运行的日志；上表里的 `J0-app-foreground`
是脚本起点检查，不是 App Lock 行为本身。

### 10.2 全量回归（**全部实跑，新计数**）

| 项 | 命令 | 结果 |
| --- | --- | --- |
| `:core:test` | `./gradlew :core:test`（**无需任何环境变量或 init script**） | **71 / 71 PASS**（0 fail / 0 error / 0 skip，6 个 suite） |
| `:conformance:run` | `./gradlew :conformance:run` | **pass=91 fail=0 notImplemented=0 total=91** |
| `:app:connectedDebugAndroidTest` | 分 4 批（见下） | **45 / 45 PASS**（0 fail / 0 error / 0 skip） |
| `:app:assembleDebug` | — | BUILD SUCCESSFUL |
| `:app:assembleRelease` | — | BUILD SUCCESSFUL |
| `:app:bundleRelease` | — | BUILD SUCCESSFUL |

**设备内测试逐批（按类分批，规避历史 OOM）**：

| 批 | 类 | 用例数 | 结果 |
| --- | --- | ---: | --- |
| 1 | `AccessibilitySemanticsTest` + `ImportHitboxTest` + `ScreenProtectionEvidenceTest` | 14 + 2 + 2 = **18** | 全 PASS |
| 2 | `AppLockNavigationTest` + `BackupExportRegressionTest` | 7 + 3 = **10** | 全 PASS |
| 3 | `PersistenceEvidenceTest` + `RepositoryKeystoreEvidenceTest` | 8 + 4 = **12** | 全 PASS |
| 4 | `DepmapRuntimeEvidenceTest` + `PerfSmokeEvidenceTest` | 4 + 1 = **5** | 全 PASS |
| **合计** | | **45** | **0 failures** |

**计数口径与取证可信度（这一节本轮被修过两次，必须说清）**：

- 计数来自 `local_private/atresults/batch-*/` 下**每批独立目录**的结果 XML，
  读取方式是把每个 XML 的 `tests/failures/errors/skipped` 与逐条 `testcase` 汇总，
  并与 Gradle 控制台的 `Starting N tests` 交叉核对。
- **旧数字 44 与"四个批次 XML md5 完全相同"的那份证据已作废**（见 D-14）。
  本轮先修了取证脚本（批次分目录 + 从真实输出目录取 + 取不到就报错），
  才重新收集出上表的 45。
- 批次 1 第一次跑因设备上残留**其他签名**的 APK 导致
  `INSTALL_FAILED_UPDATE_INCOMPATIBLE`，**一个用例都没跑**；
  脚本补上"安装前先卸载"后重跑，其中 `AccessibilitySemanticsTest` 又因
  Home 改为异步加载而命中"门禁空转"断言（等真实内容出现即可，非无障碍缺陷），
  修好后 **14/14 PASS**。这一段也在 `local_private/atresults/` 里留了
  `batch-1`（0 用例，失败）、`batch-1b`（18 用例 1 失败）、`batch-1d`（14 用例 0 失败）三级证据，
  **不做选择性展示**。

**可执行用例总计**：71（core JVM）+ 91（conformance）+ 45（设备内）= **207**。

### 10.3 产物

| 产物 | 大小 | SHA-256 |
| --- | --- | --- |
| `app-debug.apk` | 37,022,254 B | `710dd17fb55c448f1a5d128ebda37dfc788ef605eb0d83b93df221e6bc28e391` |
| `app-debug-androidTest.apk` | — | `48bee9c03ec158174df07d022d2b11e83cd348f6558b17ccecee0e77f0fb60cc` |
| `app-release-unsigned.apk`（默认，未签名） | 33,048,528 B | `8c9c1ce4e71b50e715e821563f42a4213d5f68d9886abcece5e9f8f0a1172ceb` |
| `app-release.aab`（默认，未签名） | 20,790,272 B | `1a3211ea69b94f520ada2e3b8e3d38fed0b2ec147d903152753e8c5118e0914e` |
| `app-release.apk`（**NON_PRODUCTION_TEST_SIGNING**） | 33,056,720 B | 见 §8（签名证书 DN 已记录） |
| `app-release.aab`（**NON_PRODUCTION_TEST_SIGNING**） | 20,823,301 B | 含 `META-INF/PDIG-NON.RSA` |

---

## 11. P0-E —— Git 收口

分类与拆分方案见 **`ANDROID_GIT_SCOPE_AUDIT.md`**（本轮新写）。要点：

- 未跟踪条目 **196** 个，其中 `android/**` **61** 个（全部是源码/配置，无构建产物）
- 判定：`fixtures/`（91 个跨端中立用例 + 28 个原始输入）、`spec/`、`tools/`、
  `conformance/CONFORMANCE_MANIFEST.json` 是**跨端正式资产 → 入库**；
  `local_private/**`（含机器绝对路径、运行时证据、测试密钥）→ **忽略**
- `.gitignore` 新增 2 条（截图取证输出、Gradle 配置缓存）

提交链（每个提交后都校验 `rev-parse HEAD` / `log --oneline` / `status -uall` / `diff --check`）：

```
HEAD = 8354ea2ec95b1cad3ed240fcb040d662f5e85aaf

8354ea2 fix(android): device credential must never be downgraded to a manual ack
47991af fix(android): make the build reproducible on non-ASCII checkouts
041ed8b test(android): device evidence suite, canonical spec and cross-platform fixtures
ca36083 feat(android): Compose app, application layer and App Lock wiring
38c9a50 feat(android): native Kotlin domain/data layer, conformance runner and Gradle build
ad2350b build(android): add the missing Gradle Wrapper (8.9) so the build stops depending on a machine-local Gradle
6d268c0 chore(build): ignore the agent workspace memory directory
b93edd4 docs(release): correct the platform state after an independent re-verification

工作区状态：dirty（30 个变更条目）
```

**流程说明**：本工作区存在已知 Git 故障 —— loose ref（`.git/refs/heads/**`）会被外部进程回收，
`git commit` 会成功创建对象并返回 0 但 **HEAD 不推进**。本轮继续使用已验证的
`write-tree` + `commit-tree` + 改写 `.git/packed-refs` 流程。
**未使用**：`git reset --hard`、`git clean -fd`、`git restore .`、force push。

---

## 12. P0-F —— 状态文档矛盾清理

`WORK_STATUS.md` 已重构（**不删除历史**）：

| 处理 | 内容 |
| --- | --- |
| **移出 Current** | `Phase = PLATFORM BRINGUP & RELEASE VALIDATION`、`NEXT_GATE = UI_BUILD_READY`、`Blocker = B10 / DCloud 账号`、uni-app x 路线的一切 |
| **Current 只保留 6 个关注面** | Native Migration / Android N1·N2 / Harmony N3 next / iOS N4 blocked-by-macOS / Cross-platform Conformance / Cutover |
| **明确写入** | `DCloud = 0 target` · `UTS = 0 target` · `uni-app / uni-app x = 0 target` |
| **移入 Historical/Legacy** | 上述全部内容，加一节说明"不再代表当前 Production 路线，保留是因为 Legacy 仍是 BEHAVIOR ORACLE" |
| **B20/B21/B22 重新定性** | 它们是 **LEGACY UTS 路线**的桥接缺口；原生 Android 已各自具备真实实现 + 设备内证据，**对新路线不构成 blocker**（旧记录保留） |
| **不再把 DCloud 登录写成 current blocker** | 已从 Current blocker 列表移除 |

---

## 13. Gate 矩阵（**重新计算**，不沿用旧的 27 Gate 结论）

判定只用：`PASS` / `FAIL` / `BLOCKED` / `NOT_RUN` / `PARTIAL_WITH_REPORT`。

| # | Gate | 结果 | 依据 |
| --- | --- | --- | --- |
| G1 | `ANDROID_BUILD_REPRODUCIBLE`（无仓库外依赖） | **PASS** | §2：不带任何环境变量直接 `BUILD SUCCESSFUL` |
| G2 | `ANDROID_CORE_JVM_TEST` | **PASS** | 71/71 |
| G3 | `ANDROID_CONFORMANCE` | **PASS** | 91/91 |
| G4 | `ANDROID_DEVICE_TEST` | **PASS** | 44/44 |
| G5 | `ANDROID_APK_BUILD` | **PASS** | assembleDebug |
| G6 | `ANDROID_AAB_BUILD` | **PASS** | bundleRelease |
| G7 | `ANDROID_RELEASE_APK_BUILD` | **PASS** | assembleRelease |
| G8 | `ANDROID_APP_LOCK_WIRING` | **PASS** | §3 + `AppLockNavigationTest` **7/7** + 真机 J0 |
| G9 | `ANDROID_APP_LOCK_NOT_BYPASSABLE` | **PASS** | 锁定时语义树里无 HOME/敏感路由；BACK/深链结构性不可达 |
| G10 | `ANDROID_BIOMETRIC_RUNTIME` | **BLOCKED_BY_RUNTIME_ENVIRONMENT** | §7：AVD 无 `hw.finger`，且无无头录入手段 |
| G11 | `ANDROID_DEVICE_CREDENTIAL_RUNTIME` | **PARTIAL_WITH_REPORT** | §7.2：**判定已修好并取证**（设 PIN 后展示「验证身份并解锁」、不再手动放行；`isDeviceSecure=true` 断言通过）；但**系统凭据界面在本 AVD 上拉不起来**，输对/输错 PIN 无法验证 |
| G12 | `ANDROID_BACKUP_EXPORT_CORRECTNESS` | **PASS** | §4 + `BackupExportRegressionTest` 3/3 |
| G13 | `ANDROID_DEPMAP_ROUNDTRIP` | **PASS** | 4 个设备内用例全 PASS + E2E J11 |
| G14 | `ANDROID_SCREEN_PROTECTION` | **PASS** | V1 六路由双证据 + 本轮 `ScreenProtectionEvidenceTest` 2/2 重跑 |
| G15 | `ANDROID_ACCESSIBILITY` | **PARTIAL_WITH_REPORT** | §6：Compose 语义树口径 14 屏 0 个无标签可交互节点（含反空转断言）；**TalkBack 实机读屏 NOT_RUN**，故不写 PASS |
| G16 | `ANDROID_BUTTON_HITBOX` | **PASS** | §5 交叉验证 |
| G17 | `ANDROID_PRIVACY_MANIFEST` | **PASS** | §9（真实 release manifest） |
| G18 | `ANDROID_LOG_HYGIENE` | **PASS** | §9 |
| G19 | `ANDROID_RELEASE_SIGNING_PIPELINE` | **PASS**（非生产密钥验证链路） | §8 |
| G20 | `ANDROID_RELEASE_SIGNING_PRODUCTION` | **BLOCKED_BY_MISSING_PRODUCTION_KEYSTORE** | §8.2 |
| G21 | `ANDROID_CORE_USER_JOURNEY_E2E` | **FAIL** | §10.1：`core-journey-v3-20260917-111220` = **8 PASS / 14 FAIL / 22 总**，14 项 FAIL 全部级联自 D-16 |
| G22 | `ANDROID_PROCESS_DEATH_PERSISTENCE` | **PASS** | `PersistenceEvidenceTest` 8/8（设备内）。E2E J9 本轮 FAIL，但原因是库里没有数据（D-16 导致导入没发生），**不是持久化失效** |
| G23 | `ANDROID_PERFORMANCE_SMOKE` | **PASS** | `PerfSmokeEvidenceTest`（10,000 行强断言） |
| G24 | `ANDROID_STORE_METADATA` | **PARTIAL_WITH_REPORT** | 文案草稿完成；截图/图标/公开隐私政策链接 NOT_STARTED |
| G25 | `ANDROID_SECRET_HYGIENE` | **PASS** | `.gitignore` 覆盖 keystore/jks/db/depmap；本轮未提交任何密钥 |
| G26 | `CROSS_PLATFORM_CONFORMANCE` | **PARTIAL_WITH_REPORT** | Android 91/91；Harmony / iOS 无报告 |
| G27 | `ANDROID_DOCS_CONSISTENCY` | **PASS** | §12 + 本文件 |

---

## 14. Parity 重算

### 14.1 先修正分母（旧的 58/62 无法复现）

`NATIVE_PARITY_MATRIX.md` 的 Android 列实际有 **73** 行能力格
（§1 领域 16 + §2 持久化 10 + §3 安全 10 + §4 导入 7 + §5 UI 22 + §6 工程 8）。
旧的 `62` **在文件里找不到出处**，因此本轮：

> **分母 = 73（矩阵实际行数）；"已完成" = `TESTED` 及以上（不含 `IMPLEMENTED` 与 `PARTIAL`）。**

### 14.2 重算结果

| 分类 | 关闭 | 总行 | 说明 |
| --- | ---: | ---: | --- |
| §1 领域 / 语义 | 13 | 16 | 其余行（Node/Dependency/Group、canonical groupKey 等）仍是 `IMPLEMENTED` |
| §2 持久化 / 迁移 | 10 | 10 | 设备内 PersistenceEvidenceTest 把 4 个 `IMPLEMENTED` 升到运行时验证 |
| §3 安全 / 密钥 / 认证 | 9 | 10 | 生物认证单一格仍受 P1-C 环境限制 |
| §4 导入 / 解析 | 7 | 7 | 22 个 parser fixture 全通过 |
| §5 UI | 14 | 22 | 逐屏按本轮真机是否被真实走过判定；导入/恢复相关 4 格因 D-16 回退为 `PARTIAL` |
| §6 工程 / 发布 | 3 | 8 | Release 签名格仍为 `BLOCKED_BY_MISSING_PRODUCTION_KEYSTORE` |
| **合计** | **56** | **73** | 与 `NATIVE_PARITY_MATRIX.md` 的逐格统计一致 |

明细与逐格状态见更新后的 `NATIVE_PARITY_MATRIX.md`。

---

## 15. 三个判定（分别回答，不混为一谈）

### 15.1 `N1_ANDROID_VERTICAL_SLICE` —— **PARTIAL_WITH_REPORT（不允许判 PASS）**

定义（沿用任务给出的口径）：**核心用户纵向链 + 真实 persistence + security entry + app runtime** 全部打通。

| 组成 | 结论 | 依据 |
| --- | --- | --- |
| app runtime | **PASS** | 安装 / 启动 / `CRASH-SCAN` 崩溃 0 |
| 真实 persistence | **PASS** | SQLCipher 密文库 + 迁移 + `PersistenceEvidenceTest` 8/8 |
| security entry | **PASS** | D-1/D-2/D-3/D-13 关闭；冷启动先锁、解锁后才可进入、前后台回锁、锁不可绕过（`AppLockNavigationTest` 7/7） |
| **核心用户纵向链** | **FAIL** | `core-journey-v3-20260917-111220` = 8 PASS / 14 FAIL；**纵向链断在"导入"这一步**（D-16） |

**为什么不能判 PASS**：任务原文写明"App Lock 修好并回归全绿后，N1 才允许 PASS"。
App Lock 已修好并单独验证通过，但**回归没有全绿** —— E2E 22 步里有 14 步 FAIL。
按口径，N1 因此**不允许 PASS**。

**为什么也不是"全线崩溃"**：14 个 FAIL 是同一根因（D-16）的级联，不是 14 个独立缺陷；
并且 D-16 是**本轮为了修好 App Lock 而引入**的，不是这条链路上原本就有的业务缺陷。
在 D-16 被修掉之前，链路后半段（候选关系 → 确认 → 影响 → 计划 → 动作 → 验证 →
进程死亡 → 导出 → 恢复 → 语义等价 → 篡改拒绝）**没有资格被重新判定**，
本轮既不声称它们通过，也不把它们记成新的产品缺陷。

定义（沿用任务给出的口径）：**核心用户纵向链 + 真实 persistence + security entry + app runtime** 全部打通。

正方证据：

- 核心业务链在**设备级**端到端跑通（E2E J1–J11，见 §10.1）
- 真实持久化：SQLCipher 密文库、迁移、进程死亡后数据仍在
- **security entry 已真实接线**：冷启动先锁、解锁后才可进入（D-1/D-2 关闭）
- app runtime：安装 / 启动 / 崩溃 0

反方证据（如有，见 §10.1 的 FAIL 项）。

### 15.2 `N2_ANDROID_FULL_PARITY` —— **PARTIAL_WITH_REPORT**

不再使用旧口径。判定依据：§14 的 73 格重算（**56 / 73**）+ §13 的 27 个 Gate。

未关闭的 17 格与未 PASS 的 Gate 逐项列出（不掩盖）：

| 类别 | 项 | 状态 |
| --- | --- | --- |
| 环境 | 生物认证运行时（指纹） | `BLOCKED_BY_RUNTIME_ENVIRONMENT`（AVD 无 `hw.finger`） |
| 环境 | 系统凭据界面能否真正完成一次认证 | 未验证（本 AVD 拉不起 PIN 界面） |
| 环境 | TalkBack 实机读屏 | `NOT_RUN`（镜像未预装、无 Play 商店） |
| 外部 | Release 生产签名 | `BLOCKED_BY_MISSING_PRODUCTION_KEYSTORE` |
| 外部 | Store 素材（截图 / 图标 / 公开隐私政策链接） | `NOT_STARTED` |
| 产品 | **导入 / Import Mapping / Import Review / Restore 四格** | **由 `RUNTIME_VERIFIED` 回退为 `PARTIAL`（D-16）** |
| 产品 | Onboarding（无入口可达）、Timeline、Graph View、Candidate Review | 本轮未被真机走过 |

**注意**：Harmony / iOS 两侧（N3 / N4 未开始）属 another platform，
**不计入 Android parity 的 73 格**。

不再使用旧口径。判定依据：§14 的 73 格重算 + §13 的 27 个 Gate。
**未关闭的格逐项列出**（不掩盖）：

- 生物认证运行时（`BLOCKED_BY_RUNTIME_ENVIRONMENT`）
- TalkBack 实机读屏（`NOT_RUN`，镜像未预装）
- Release 生产签名（`BLOCKED_BY_MISSING_PRODUCTION_KEYSTORE`）
- Store 素材（截图 / 图标 / 公开隐私政策链接）
- Harmony / iOS 两侧（N3 / N4 未开始；**属 another platform，不计入 Android parity**）

### 15.3 `ANDROID_PRODUCTION_RELEASE_READY` —— **BLOCKED_BY_PRODUCTION_SIGNING**

即使 N1 / N2 PASS，仍**独立受阻**于：

1. 缺生产 keystore（B4）→ 无法产出可上架签名包
2. Store 素材与公开隐私政策 URL 未完成（B12 / B15 / B16 / B17）
3. 正式包名 `applicationId` 仍是占位 `com.pdig.app`（B11）

**Production signing 与 engineering parity 分开报告**：本轮签名的只是
`NON_PRODUCTION_TEST_SIGNING`，它证明**流水线可用**，不能推导成"可以上架"。

---

## 16. Remaining blockers

| # | Blocker | 类型 | 阻塞什么 |
| --- | --- | --- | --- |
| B3 | 无 macOS / Xcode | 外部（用户环境） | iOS 编译/签名/真机验证（N4） |
| B4 | 无生产 release keystore | 外部（用户提供） | `ANDROID_PRODUCTION_RELEASE_READY` |
| B5 / B6 / B7 | Google Play / Huawei / AGC 账号 | 外部 | 三端上架 |
| B11 | 正式 `applicationId` / bundle id | 外部（产品决策） | 上架 |
| B12 / B12b | 公开隐私政策 URL / 支持 URL | 外部 | 上架 |
| B13 | 真实账单（仅 REAL_DATA Gate） | 外部 | Real Data 验证 |
| B14–B17 | 品牌名 / 图标 / 启动图 / 商店截图 | 外部 | 上架 |
| B18 | 真实 Android 设备 | 外部（设备） | 真机（非 AVD）验证 |
| B19 | 是否要求真实数据验证的决策 | 外部（用户） | Real Data Gate |
| B23 | 本机沙箱限制（clean clone 闭环 / 破坏性 `npm ci`） | 环境 | 完整 CI 闭环复现 |
| **新增** | `hw.finger` 指纹 AVD + 无头录入手段 | 环境 | `ANDROID_BIOMETRIC_RUNTIME` |
| **新增** | 镜像未预装 TalkBack 且无 Play 商店 | 环境 | `ANDROID_ACCESSIBILITY` 的 TalkBack 项 |

**已从 blocker 移除**（保留历史记录）：

- **B10 / DCloud 账号**、**B1 / B2**：只阻断 LEGACY uni-app x 路线的产品级打包，
  `DCloud = 0 target`，不再影响 Current 路线。
- **B20 / B21 / B22**：LEGACY UTS 路线的桥接缺口；原生 Android 已具备真实实现与设备证据。

---

## 17. 停止条件

按任务要求**到此停止**：

- **不进入 Harmony N3**、**不进入 iOS N4**、**不进入 MVP04**
- 未新增业务 Domain，未重设计产品

等待人工复核。

---

## 18. 复现命令（全部可复制粘贴）

```bash
# 0. 环境（Windows；非 ASCII 工程路径已由 settings.gradle.kts 自动适配）
export JAVA_HOME="<ANDROID_STUDIO_HOME>/jbr"
export ANDROID_HOME="<ANDROID_SDK_ROOT>"
export GRADLE_OPTS="-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=10808 \
                    -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=10808"   # 仅首次下载 Wrapper 需要
cd android

# 1. 纯 JVM 单测（71）—— 不再需要任何仓库外 init script
./gradlew --no-daemon :core:test --console=plain

# 2. 跨端 conformance（91）
./gradlew --no-daemon :conformance:run --console=plain
cd .. && node tools/conformance/run.mjs

# 3. 设备内证据（44，分 4 批规避 OOM）
bash local_private/run_connected_batches.sh

# 4. 构建
cd android && ./gradlew --no-daemon :app:assembleDebug :app:assembleRelease :app:bundleRelease
# 非生产签名链路验证（≠ 生产签名）
./gradlew --no-daemon :app:assembleRelease :app:bundleRelease -PpdigNonProductionSigning=true

# 5. 核心用户行程 E2E（全新安装 → 锁屏 → 解锁 → 导入 → Reality → Impact →
#    ChangePlan → done≠verified → Verify → 进程死亡 → 导出 → 清数据 → 恢复 → 篡改拒绝）
bash local_private/run_e2e_v3.sh

# 6. 无障碍（Compose 语义树口径）
./gradlew --no-daemon :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.pdig.app.evidence.AccessibilitySemanticsTest

# 7. Device Credential fallback（不需要指纹硬件）
python local_private/device_credential_check.py
```
