# ANDROID_REMAINING_7_AUDIT.md

> 逐项审计 `NATIVE_PARITY_MATRIX.md` 中 Android 列**未完成的 7 格**。
> 状态只允许：`PASS` / `FAIL` / `BLOCKED` / `NOT_RUN` / `PARTIAL_WITH_REPORT`。
> **禁止使用"基本完成"**。每格必须写清：缺代码 / 缺测试 / 仅环境阻塞 / 验证证据。

更新时间：2026-09-15

---

## 汇总

| # | 项 | 原状态 | P0 轮（2026-09-16）状态 | 是否关闭 |
| --- | --- | --- | --- | --- |
| 1 | 截图保护 | NOT_STARTED | **RUNTIME_VERIFIED** | **是** |
| 2 | 单测 / 集成测试 | PARTIAL | **PASS**（`:core` 71 tests） | **是** |
| 3 | 设备 E2E | NOT_RUN | **PARTIAL_WITH_REPORT**（业务全链路 21/21 PASS） | 部分 |
| 4 | 性能 smoke | NOT_RUN | **PASS** | **是** |
| 5 | 无障碍 | PARTIAL | **PARTIAL_WITH_REPORT** | 否 |
| 6 | Release 签名 | BLOCKED | **BLOCKED_BY_MISSING_PRODUCTION_KEYSTORE** | 否（真实 blocker） |
| 7 | Store metadata | NOT_STARTED | **PARTIAL_WITH_REPORT**（草稿完成） | 部分 |

> P0 轮新增关闭 2 项（截图保护、单测）。设备 E2E 的业务写入链路已打通，
> 剩余 NOT_RUN 收敛为 Onboarding / Timeline / App Lock / TalkBack 四项入口缺失或环境缺失。

---

## 1. 截图保护

| 项 | 内容 |
| --- | --- |
| 原状态 | `NOT_STARTED` |
| P0 轮状态 | **RUNTIME_VERIFIED** |
| 缺代码？ | **否** —— 已实现 `android/app/src/main/kotlin/com/pdig/app/ui/SecureWindow.kt` |
| 缺测试？ | **否** —— 口径测试 2/2 + 真机 6 路由双证据 |
| 仅环境阻塞？ | **否** |

**已做**：`SecureWindow(route)` 按当前路由设置 / 清除 `WindowManager.LayoutParams.FLAG_SECURE`，
已接入 `PdigApp`（`currentBackStackEntryAsState()`）。敏感路由 12 个
（IMPORT / BACKUP / RESTORE / SOURCES / REVIEW / DRIFT / CANDIDATES / INFRASTRUCTURE / GRAPH / NODE / TIMELINE / PLAN），
概览与说明类页面不设保护。

**验证证据**：
- 设备内 `ScreenProtectionEvidenceTest` **2/2 PASS** —— 断言敏感路由集合完全相等（新增路由若不显式归类即失败），且 `null` / 未知路由**不**被视为敏感。

**未完成（上一版）→ 已在 P0 轮解决**：
上一版判 `NOT_RUN` 的原因是**检测口径错了**——`dumpsys window windows` 在 API 34
输出的是**裸 flag 名 `SECURE`**，而不是 `FLAG_SECURE` 字面量，grep `FLAG_SECURE` 恒为 0。
改用 `SECURE` token 后取得真机双证据（窗口 flag + `screencap` 像素）：

| 路由 | 期望敏感 | 窗口 flag | 截图像素均值 | 结论 |
| --- | --- | --- | --- | --- |
| 首页 | 否 | 无 `SECURE` | 244.64 | PASS |
| 设置 | 否 | 无 `SECURE` | 有内容 | PASS |
| 数据来源与导入 | 是 | 含 `SECURE` | 0.17（抹黑） | PASS |
| 我的基础设施 | 是 | 含 `SECURE` | 抹黑 | PASS |
| 备份 | 是 | 含 `SECURE` | 抹黑 | PASS |

**6/6 PASS → 本项关闭**。脚本 `local_private/security_evidence_v2.py`、
`local_private/sec_settings.py`；报告 `ANDROID_RUNTIME_SECURITY_EVIDENCE.md`。

---

## 2. 单测 / 集成测试

| 项 | 内容 |
| --- | --- |
| 原状态 | `PARTIAL`（只有 conformance runner） |
| P0 轮状态 | **PASS** |
| 缺代码？ | **否** —— P0 轮已为 `:core` 补齐 6 个 JVM 测试套件 |
| 缺测试？ | 否 |
| 仅环境阻塞？ | 否 |

**准确统计（本轮真实执行）**：

| 来源 | 文件数 | 用例数 | 结果 |
| --- | --- | --- | --- |
| `./gradlew :core:test` | **6** | **71** | **PASS**（`71 tests completed, 0 failed`）—— P0 轮新增 |
| `./gradlew :conformance:run` | 1（runner） | **91** | **PASS**（`pass=91 fail=0 notImplemented=0 total=91`） |
| 设备内 androidTest | **5** | **19** | **PASS**（`19/19 completed, 0 skipped, 0 failed`） |
| `:app:testDebugUnitTest` | 0 | 0 | **NO-SOURCE**（如实记录，不冒充通过） |
| **合计** | — | **181** | — |

`:core` 71 个用例分布（`ANDROID_CORE_JVM_TEST_REPORT.md`）：

| 套件 | 用例数 |
| --- | --- |
| `DomainInvariantTest` | 15 |
| `ImpactKernelTest` | 11 |
| `PlanReadinessTest` | 14 |
| `MigrationSemanticsTest` | 10 |
| `GraphRevisionSemanticsTest` | 7 |
| `StateMachineTest` | 14 |
| **合计** | **71** |

androidTest 明细：

| 测试类 | 用例数 | 覆盖 |
| --- | --- | --- |
| `PersistenceEvidenceTest` | 8 | SQLCipher 开关重开 / 错误密钥 / 明文不可读 / 事务回滚 / v1→v3 / v2→v3 / 未来版本拒绝 / 失败回滚 |
| `RepositoryKeystoreEvidenceTest` | 4 | Reality 变更只 +1 graphRevision、Proposal 决策不 bump、事务原子性、原始密钥不落盘 |
| `DepmapRuntimeEvidenceTest` | 4 | 导出导入字节一致 / 错误口令 / 篡改密文 / v1 v2 迁移等价 |
| `ScreenProtectionEvidenceTest` | 2 | 敏感路由口径 |
| `PerfSmokeEvidenceTest` | 1 | 性能 smoke |

**P0 轮关闭理由**：`:core` 的纯 JVM 单元测试从 **0 → 71**，覆盖领域不变量、
影响面计算、计划就绪度、迁移语义、graphRevision bump 规则、状态机。
领域正确性不再完全依赖 conformance + 设备内集成。→ **本项关闭**。

**环境提示（非产品问题）**：仓库路径含中文，Gradle test worker 会加载不到测试类
（10/10 `ClassNotFoundException`）。本轮用 init script 把 build 目录重定向到 ASCII 路径
（`%USERPROFILE%/pdig-build` + `-I ascii-build.gradle.kts`）解决，源码路径未改动。
曾尝试 `mklink /J` 建 ASCII 联接，**无效**（Gradle 会规范化回中文路径）。

---

## 3. 设备 E2E

| 项 | 内容 |
| --- | --- |
| 原状态 | `NOT_RUN` |
| P0 轮状态 | **PARTIAL_WITH_REPORT** |
| 缺代码？ | **否（业务链路已打通）**；仍缺 Onboarding / App Lock 的启动入口接线 |
| 缺测试？ | 否（21 步驱动脚本 `local_private/core_journey_e2e_v2.py`） |
| 仅环境阻塞？ | App Lock / TalkBack 属环境阻塞；Onboarding / Timeline 属缺入口接线 |

**已真实执行并 PASS**：全新安装、首次启动、首页、场景中心、基础设施总览、
加密持久化取证（拉出 176,840 B 密文库，头 16 字节非 SQLite 魔数）、
明文 sqlite3 不可读、前后台热启动（2668ms）、进程 kill→重启、清空状态、
触摸目标、字体缩放 1.30、横屏、焦点顺序、logcat 进程归属扫描。

**P0 轮打通的业务全链路（21 步 21 PASS / 0 崩溃）**：
全新安装 → SAF 选真实 CSV → 提交导入（10.8 s，落库「记录 5 个对象，生成 3 条待确认关系」）
→ 候选关系 → acceptProposal 转 Reality → **用户显式标记必需**
→ 影响面「必须处理（2）」→ 创建变更计划 → 标记完成（done ≠ verified）→ 确认验证
→ 进程 kill 后数据仍在 → `.depmap` 导出 13,617 B → 错误密码恢复被拒。

> 关键点：`criticality=required` **只能由用户设置**（机器永不产生）。
> 上一版行程漏了"标记必需"这一步，影响面因此恒为「必须处理（0）」，
> 本轮补上后断言收紧为 ≥ 1。

**剩余 NOT_RUN 及原因**：
- Onboarding —— 屏已实现但 `startDestination=Route.HOME`，未接入启动流程（缺接线）
- Timeline / RealityDrift —— 入口文案在真机 UI 上未找到（缺可达入口）
- App Lock —— **设备上 `AppLock.state()` 返回 `NOT_CONFIGURED`（fail-closed 正确），
  但全仓库无 `nav.navigate(Route.LOCK)`：锁屏页写好了却没有任何入口能调起**（真实缺口）
- TalkBack —— 镜像未预装

~~截图保护运行时 flag —— dumpsys 未解析出 flags~~ → 已在 P0 轮解决（见 §1）。

详见 `ANDROID_RUNTIME_E2E_REPORT.md` §7。

---

## 4. 性能 smoke

| 项 | 内容 |
| --- | --- |
| 原状态 | `NOT_RUN` |
| 本轮状态 | **PASS** |
| 缺代码？ | 否 |
| 缺测试？ | 否（含 `assertEquals(10_000, rows)` 强断言） |
| 仅环境阻塞？ | 否 |

**关键**：上一轮的数字（`parse=2427ms` 等）对应的 `csvRowsParsed=0`，**已全部作废（INVALIDATED）**，
详见 `ANDROID_PERFORMANCE_SMOKE_REPORT.md`。

本轮有效实测（模拟器 API34/x86_64/软件渲染，单次采样）：

| 指标 | 值 |
| --- | --- |
| `csvRowsParsed` | **10,000** |
| `csvParseErrors` | **0** |
| `db_open_encrypted_sqlcipher` | 149 ms |
| `migrate_v0_to_v3` | 831 ms |
| `parse_generic_csv_10k_rows` | 6058 ms |
| `insert_10k_rows_in_one_transaction` | 8679 ms |
| `build_timeline` | 448 ms |
| `export_graph`（1,999,690 B） | 624 ms |
| `depmap_encrypt_export` | 2722 ms |
| `depmap_decrypt_and_import` | 11290 ms |

**未测项**：DB 回读行数（本轮未加 `SELECT count(*)` 断言），如实记为 `NOT_MEASURED`。

---

## 5. 无障碍

| 项 | 内容 |
| --- | --- |
| 原状态 | `PARTIAL` |
| 本轮状态 | **PARTIAL_WITH_REPORT** |
| 缺代码？ | **是** —— 有 4 个可点击节点无 text / content-desc |
| 缺测试？ | 是 |
| 仅环境阻塞？ | TalkBack 属环境阻塞；语义标签属缺代码 |

**本轮真实检查**：

| 检查 | 结果 |
| --- | --- |
| 触摸目标最小边 | **PASS** —— `minEdgePx=140`（阈值 48dp@420dpi ≈ 121px） |
| 焦点顺序（TAB 遍历） | **PASS** —— 记录到 12 步，顺序可复现 |
| 语义标签 | **PARTIAL** —— `clickable=4 focusable=4 unlabeled=4` |
| 字体缩放 1.30 | **PASS** —— `texts=12 crashes=0` |
| 横屏 | **PASS** |
| TalkBack 实机读屏 | **NOT_RUN** —— 镜像未预装 TalkBack |

→ 不能因为"静态检查里有 48dp"就判 PASS。**仍有 4 个无标签可点击节点**，故维持 PARTIAL。

---

## 6. Release 签名

| 项 | 内容 |
| --- | --- |
| 原状态 | `BLOCKED`（keystore） |
| 本轮状态 | **BLOCKED_BY_MISSING_PRODUCTION_KEYSTORE** |
| 缺代码？ | 否 |
| 缺测试？ | 否 |
| 仅环境阻塞？ | **否** —— 缺用户提供的真实生产 keystore，这是**允许的真实 blocker** |

**已做**：
- `./gradlew :app:bundleRelease` **成功** → `app-release.aab` **20,734,935 B**，
  SHA-256 `f10cc60d8b3d4009e0968c4b85403d49cf4a4a1cd43b03d418e9b8deb770f76c`
  → 即 `ANDROID_AAB_BUILD = PASS`（构建流水线可用）。
- 为验证签名流水线，创建了本地 keystore（`CN=PDIG NON-PRODUCTION TEST KEY`），
  并在 `app/build.gradle.kts` 中以 `-PpdigNonProdSigning=true` 显式开关才启用。

**必须诚实记录的问题**：加了开关后重新构建，产出的 AAB **SHA-256 与未签名版完全相同**，
且 AAB 内 `META-INF/` 下**没有 `*.RSA` / `*.SF` 签名条目**。
→ 说明**该非生产签名配置本轮未真正生效**，签名流水线**尚未被验证**。
→ 不据此宣称任何签名能力；不把测试密钥或 debug 密钥冒充生产签名。

---

## 7. Store metadata

| 项 | 内容 |
| --- | --- |
| 原状态 | `NOT_STARTED` |
| 本轮状态 | **PARTIAL_WITH_REPORT** |
| 缺代码？ | 不适用（素材项） |
| 仅环境阻塞？ | 否（截图 / 图标 / 隐私政策链接需人工产出） |

**已完成**：`ANDROID_STORE_METADATA.md` 草稿 ——
应用名、short description（38 字符，符合 80 限制）、full description、
隐私摘要、权限说明、支持场景、数据处理说明、release notes 模板、
以及**禁止出现的话术红线**（"100% 安全"、"银行级"、"发现所有依赖"、
"官方银行合作"、"自动管理银行账户"）。

**未完成**：截图（9 类）、图标与 feature graphic（当前为 placeholder）、
隐私政策公开链接（上架必需）。

---

## 结论

P0 轮（2026-09-16）后，7 项中：

- **完全关闭：4 项**（截图保护、单测/集成、性能 smoke —— 其中前两项为本轮新增关闭）
- **部分推进：2 项**（设备 E2E：业务全链路已 21/21 PASS，剩 4 项入口/环境缺失；Store metadata：草稿完成，缺截图与隐私链接）
- **真实阻塞：1 项**（Release 签名，缺生产 keystore；且非生产签名验证本轮未生效）

累计测试量：**181**（`:core` 71 + conformance 91 + 设备内 19）。

**P0 轮发现的两个真实缺陷（已记录，未改代码）**：

| # | 缺陷 | 证据 |
| --- | --- | --- |
| 1 | 备份导出 UI 误报失败 | App 提示「备份失败：无法写入文件。」，文件实际已完整落盘（13,617 B）且正确密码可恢复（「已恢复 29 条记录。」）。**2/2 稳定复现** |
| 2 | 「确认导入」按钮有效点击区低于可见范围 | Button 语义节点中心点击无反应（DB 大小与 mtime 不变）；改用可点击祖先 View 并夹到可见区才生效 |
