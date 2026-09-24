# ANDROID_RUNTIME_E2E_REPORT.md

> 所有结论来自**本轮真实执行**：真实模拟器 + 本轮用 Gradle Wrapper 重新构建的 APK。
> 没有真正执行的步骤一律记为 `NOT_RUN`，不使用"基本完成"这类表述。

更新时间：2026-09-15

---

## 1. 运行环境（Task 5 环境确认）

| 项           | 实测值                                                                 |
| ------------ | ---------------------------------------------------------------------- |
| AVD          | `PDIG_API34_DEFAULT`                                                   |
| 设备号       | `emulator-5554`（状态 `device`）                                       |
| Android 版本 | **API 34（Android 14）**                                               |
| ABI          | **x86_64**                                                             |
| 型号         | Android SDK built for x86_64                                           |
| 屏幕         | 1080 x 2400，density 420                                               |
| APK          | `app-debug.apk` **36,794,370 B**                                       |
| APK SHA-256  | **`d84d8900c66e781694a4b71d9782796d858edd93a48156bf070319c1d6230879`** |
| 测试 APK     | `app-debug-androidTest.apk` 396,650 B                                  |

> 早期曾出现"两个 E2E 驱动实例并发抢同一设备"导致的假失败
> （首页文案抓不到、崩溃数虚高）。本轮结果取自**串行单次干净运行**：
> `local_private/build-chain/18-e2e-final-clean.txt`。

---

## 2. 逐步骤结果

| 步骤                           | 状态                    | 证据                                                                                                                     |
| ------------------------------ | ----------------------- | ------------------------------------------------------------------------------------------------------------------------ |
| E2E-01 全新安装                | **PASS**                | `Success`                                                                                                                |
| E2E-02 首次启动                | **PASS**                | `activity=com.pdig.app/.MainActivity crashes=0`                                                                          |
| E2E-03 Onboarding              | **NOT_RUN**             | `PdigApp(startDestination=Route.HOME)`：Onboarding 屏已实现，但**未接入启动流程**                                        |
| E2E-04 首页                    | **PASS**                | `markers=['共 ', '我的基础设施', '常用场景'] crashes=0`                                                                  |
| E2E-05 场景中心                | **PASS**                | `activity=com.pdig.app/.MainActivity crashes=0`                                                                          |
| E2E-09 基础设施总览            | **PASS**                | `activity=com.pdig.app/.MainActivity crashes=0`                                                                          |
| E2E-06 时间线                  | **NOT_RUN**             | 入口文案未找到：`查看待确认的变化`                                                                                       |
| E2E-07 可能发生变化            | **NOT_RUN**             | 入口文案未找到：`可能发生了变化`                                                                                         |
| E2E-17 加密持久化取证          | **PASS**                | 从设备拉出 `files/pdig.db`，**176,840 B**，头 16 字节 `9d7e2a434e45335301c2a2e0cebbdc4c`，**不是** `SQLite format 3\0`   |
| E2E-17b 明文 sqlite3 打不开    | **PASS**                | `Error: unable to open database file`                                                                                    |
| E2E-13 前后台切换              | **PASS**                | `warmStartTotalTime=2668ms crashes=0`                                                                                    |
| E2E-16 进程 kill → 重启        | **PASS**                | `crashes=0 homeAfterRestart=True`                                                                                        |
| E2E-14/15 App Lock / 解锁      | **NOT_RUN**             | AOSP default 镜像无指纹传感器且未录入凭据；且 AppLock 未接生命周期（无 LifecycleEventObserver / 无 `Route.LOCK` 触发点） |
| E2E-19 清空状态                | **PASS**                | `zeroObjects=True crashes=0`                                                                                             |
| 触摸目标扫描                   | **PASS**                | `clickable=4 minEdgePx=140`（48dp@420dpi ≈ 121px 阈值）                                                                  |
| 字体缩放 1.30                  | **PASS**                | `fontScale=1.30 crashes=0 texts=12`                                                                                      |
| 横屏                           | **PASS**                | `crashes=0`                                                                                                              |
| A11Y-01 语义标签               | **PARTIAL_WITH_REPORT** | `clickable=4 focusable=4 unlabeled=4` —— 仍有 4 个可点击节点无 text / content-desc                                       |
| A11Y-02 焦点顺序               | **PASS**                | TAB 遍历记录到 12 步，顺序可复现（4 个区域循环）                                                                         |
| A11Y-03 TalkBack 读屏          | **NOT_RUN**             | 镜像未预装 `com.google.android.marvin.talkback`，无法实机验证                                                            |
| SP-01/02/03 截图保护（运行时） | **NOT_RUN**             | `dumpsys window windows` 未解析到窗口 flags 字段（见 §4）                                                                |
| SP-05 最近任务留档             | **PASS**（仅留档）      | `recentsScreenshotBytes=237347`                                                                                          |
| logcat 敏感扫描（进程归属）    | **PASS**                | `appLines=44`，6 类敏感关键字命中**全为 0**                                                                              |

### 2.1 明确未执行的业务链路（NOT_RUN）

| 步骤                         | 原因                                                                                                        |
| ---------------------------- | ----------------------------------------------------------------------------------------------------------- |
| E2E-05 导入文件              | Import 屏 `parseFile` 结果只留在内存（`AppContainer.parseFile` 不落库），无 Observation → proposal 写入路径 |
| E2E-06 Proposal 复核         | 无 proposal 写入路径，Pending Review 在真机上恒为空                                                         |
| E2E-07 确认 Reality          | `acceptProposal` 依赖 proposal 行，无写入路径则不可达；其事务语义由设备内 androidTest 单独验证              |
| E2E-08 换卡影响              | ChangePlan 创建 / 动作完成 / 验证无应用层写入路径                                                           |
| E2E-10/11/12 变更计划动作    | 同上                                                                                                        |
| E2E-18/20/21/22/23 `.depmap` | Backup/Restore 落盘走系统 SAF（文件选择器），adb 无法自动化；加密/序列化真实路径由设备内 androidTest 覆盖   |

---

## 3. 本轮修复的运行时真缺陷

这些是**编译期与 JVM conformance 都发现不了**、只有真机跑才暴露的问题：

| #   | 缺陷                      | 症状                                                                                                         | 修复                                                                    |
| --- | ------------------------- | ------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------- |
| 1   | SQLCipher native 库未加载 | `UnsatisfiedLinkError` at `nativeOpen`                                                                       | `PdigApplication.onCreate()` 增加 `System.loadLibrary("sqlcipher")`     |
| 2   | Cursor 惰性视图越界       | `CursorIndexOutOfBoundsException: Index 1 requested, with a size of 1`（`buildTimeline` 内 `CursorRow.str`） | `AndroidSqliteDriver` 改为 `MaterializedRow`，在 `all()` 内把值复制出来 |
| 3   | 查询了不存在的列          | `SQLiteException: no such column: criticality`（`acceptProposal`）                                           | v2/v3 的 `dependency_proposals` 无此列，SELECT 中移除                   |
| 4   | 性能 smoke 数据无效       | `csvRowsParsed=0`                                                                                            | 修正 `dateFormats` 并加 `assertEquals(10_000, rows)` 强断言             |

---

## 4. 截图保护运行时验证未完成的原因（诚实说明）

- 已实现：`SecureWindow.kt` 按路由设置 / 清除 `FLAG_SECURE`；敏感路由清单见该文件。
- 已验证（设备内）：`ScreenProtectionEvidenceTest` **2/2 PASS**，覆盖路由判定口径的完整性与"未知路由不视为敏感"的选择。
- **未验证**：运行时窗口 flag。`adb shell dumpsys window windows` 的输出在本机
  **没有解析到 `flags=0x…` / `fl=0x…` 字段**（不同 Android 版本 dump 格式不一致），
  因此 SP-01/02/03 记为 `NOT_RUN`。
  **不拿"代码里写了 FLAG_SECURE"冒充"已验证生效"。**

---

## 5. 环境不稳定性（真实风险，如实记录）

单次进程内跑 19 个 androidTest 时，测试进程曾被系统杀掉
（`INSTRUMENTATION_RESULT: shortMsg=Process crashed.` / `Process <pid> exited due to signal 9 (Killed)`）。
按类分批并清空 logcat 后，5 个测试类分别全部 `OK`。
宿主可用内存约 5.6 GB / 31.8 GB，AVD 内存 3072 MB —— **宿主内存紧张是真实脆弱点**。

---

## 6. Gate

| Gate                  | 状态                                                                                                                                                                                                                                                               |
| --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `ANDROID_RUNTIME_E2E` | **PARTIAL_WITH_REPORT**                                                                                                                                                                                                                                            |
| 依据                  | 安装 / 冷启动 / 首页 / 场景中心 / 基础设施 / 加密持久化 / 前台后台 / 进程重启 / 清状态 / 字体缩放 / 横屏 / 触摸目标 / logcat 隐私 全部 PASS；但 Onboarding、Timeline、Drift、App Lock、TalkBack，以及 import→proposal→reality→changeplan→depmap 全链路均为 NOT_RUN |

---

## 7. P0 轮补测（2026-09-16）—— 上一版 NOT_RUN 项的处置结果

> 本轮目标：把 §2.1 中"没有应用层写入路径"因而 NOT_RUN 的**业务全链路**，
> 以及 §4 中"运行时窗口 flag 未取得"的**截图保护**补成真实运行时证据。
> 驱动脚本 `local_private/core_journey_e2e_v2.py`，证据落盘 `local_private/e2e/`。

### 7.1 环境与被测物（与上一版不同）

| 项          | 本轮实测值                                                                                                                           |
| ----------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| 设备        | `emulator-5554`（`PDIG_API34_DEFAULT`，API 34，x86_64，1080x2400 / 420dpi）                                                          |
| APK         | `app-debug.apk` **36,887,249 B**                                                                                                     |
| APK SHA-256 | **`bf378ec6…305ff1`**（完整值见 `local_private/_apk_sha.txt`）                                                                       |
| 构建方式    | `gradlew -I ascii-build.gradle.kts :app:assembleDebug`，build 目录重定向到 ASCII 路径（中文路径会破坏 Gradle test worker classpath） |
| 归档        | `local_private/artifacts/app-debug.apk`                                                                                              |

### 7.2 业务全链路：21 步 **21 PASS / 0 崩溃**

| 步骤                        | 状态 | 关键证据                                                                                   |
| --------------------------- | ---- | ------------------------------------------------------------------------------------------ |
| J1 全新安装 + 冷启动        | PASS | `Success`；首页锚点「我的基础设施」出现，crashes=0                                         |
| J2 SAF 选真实 CSV           | PASS | 选 `wechat_synthetic.csv`，解析出 **2 个支付方式 / 3 个收款对象**                          |
| J2-commit 提交导入          | PASS | 耗时 **10.8 s**（上一版 379 s 超时即失败）；落库提示「记录 5 个对象，生成 3 条待确认关系」 |
| J3 候选关系列表             | PASS | 3 条 proposal 可见                                                                         |
| J4 acceptProposal → Reality | PASS | 确认后 proposal 出列，回首页计数增长                                                       |
| J5 用户显式标记必需         | PASS | 详情页「标记为必需」；**这一步此前遗漏，导致影响面恒为 0**                                 |
| J5b 影响面                  | PASS | 「必须处理（**2**）」+「处理顺序」分组                                                     |
| J6 创建变更计划             | PASS | 计划生成，动作带 `resolvesImpactKeys`                                                      |
| J7 标记完成（done）         | PASS | done ≠ verified：完成后仍有待验证项                                                        |
| J8 确认验证（verified）     | PASS | 验证后计划进入已验证状态                                                                   |
| J9 进程 kill → 重启         | PASS | `am kill` 后重启，数据仍在（「共 5 个对象」）                                              |
| J10 `.depmap` 导出          | PASS | 文件落盘 **13,617 B**（`find /sdcard -name '*.depmap'`）                                   |
| J10 错误密码恢复被拒        | PASS | 未回到首页，恢复未发生                                                                     |

> **重要语义说明**：`criticality=required` **只能由用户显式设置**（机器永不产生）。
> 上一版行程缺少"用户标记必需"这一步，因此影响面恒为「必须处理（0）」。
> 本轮补上后断言收紧为"必须处理数量 ≥ 1"，不再接受 `（0）`。

### 7.3 截图保护（§4 的 NOT_RUN 已关闭）

`dumpsys window windows` 在 API 34 输出的是**裸 flag 名 `SECURE`**，
不是 `FLAG_SECURE` 字面量 —— 上一版 grep `FLAG_SECURE` 恒为 0 是**检测口径缺陷**，
不是产品缺陷。改用 `SECURE` token 后：

| 路由           | 期望敏感 | 窗口 flag   | `screencap` 均值 | 结论 |
| -------------- | -------- | ----------- | ---------------- | ---- |
| 首页           | 否       | 无 `SECURE` | 244.64（有内容） | PASS |
| 设置           | 否       | 无 `SECURE` | 有内容           | PASS |
| 数据来源与导入 | 是       | 含 `SECURE` | 0.17（被抹黑）   | PASS |
| 我的基础设施   | 是       | 含 `SECURE` | 被抹黑           | PASS |
| 备份           | 是       | 含 `SECURE` | 被抹黑           | PASS |

**6/6 路由双证据通过**。详见 `ANDROID_RUNTIME_SECURITY_EVIDENCE.md`。

### 7.4 仍然 NOT_RUN（本轮未解决，不伪装）

| 项                          | 原因                                                                                                                                  |
| --------------------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| Onboarding                  | `PdigApp(startDestination=Route.HOME)` 固定，Onboarding 未接启动流程                                                                  |
| Timeline / Drift            | 入口文案在当前首页不可达                                                                                                              |
| App Lock / 解锁             | `AppLock.state()` 在设备上返回 `NOT_CONFIGURED`（fail-closed 正确），但 **全仓库无 `nav.navigate(Route.LOCK)`**，锁屏页写好了却无入口 |
| TalkBack 读屏               | 镜像未预装 `com.google.android.marvin.talkback`                                                                                       |
| 无障碍 4 个无标签可点击节点 | 未修复                                                                                                                                |

### 7.5 本轮发现的两个真实缺陷（只记录，未改代码）

| #   | 缺陷                                   | 证据                                                                                                                               | 影响                                  |
| --- | -------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------- |
| 1   | 备份导出 **UI 误报失败**               | App 提示「备份失败：无法写入文件。」，但文件已完整落盘（13,617 B），用正确密码恢复成功（「已恢复 29 条记录。」）。**2/2 稳定复现** | 不丢数据，但误导用户                  |
| 2   | 「确认导入」按钮有效点击区低于可见范围 | Button 语义节点中心 (540,2214) 点击无反应（DB 大小与 mtime 均不变）；改用可点击祖先 View 并夹到可见区后 (540,2228) 才生效          | 可用性问题，是否补底部 padding 待评估 |

### 7.6 Gate 更新

| Gate                        | 上一版              | 本轮                                                                                             |
| --------------------------- | ------------------- | ------------------------------------------------------------------------------------------------ |
| `ANDROID_RUNTIME_E2E`       | PARTIAL_WITH_REPORT | **PARTIAL_WITH_REPORT（升级）** — 业务全链路 21/21 PASS、截图保护 6/6 PASS；剩余 NOT_RUN 见 §7.4 |
| `ANDROID_SCREEN_PROTECTION` | PARTIAL_WITH_REPORT | **RUNTIME_VERIFIED**                                                                             |
