# LOCAL_FINAL_REPORT — PDIG / DepMap Android Production VNext 本地最终验收

> 本轮定位：**evidence first → defect only → minimal fix → regression → recapture**。
> 本报告只记录实际执行过的检查。未执行的一律 `NOT_RUN`，未看到的像素不冒充视觉验收。

- 生成时间（本地）：2026-10-28
- 分支：`feat/android-ui-vnext-translation`
- PR：#1（`huangdi97/PDIG-DepMap`）

---

## 1. Repository

| 项 | 值 |
| --- | --- |
| REMOTE_HEAD_AT_START | `e68948d54a21ca5c894a54e2435396fcd327daed` |
| LOCAL_HEAD_AT_START | `cbe99d5c8862f90b25a463585cfd7ad146e4fb82`（当时在 `main`，工作区 clean） |
| REMOTE_HEAD_AT_END | `32d9293ff27e671c7f356d34fc3d9ac8107e8448`（parent = `a3d08e63`；因 git 传输层被阻断，经 GitHub Contents API 生成，见下方说明） |
| LOCAL_HEAD_AT_END | `= git rev-parse HEAD`（与远端**内容完全相同**的本地提交；`LOCAL_FINAL_REPORT.md` 的 blob 两侧均为 `aef6c960…`） |
| GIT_TRANSPORT | `BLOCKED`：`git push/fetch` 全程 `schannel: failed to receive handshake` / `openssl: unexpected eof while reading`，而同一时刻 `curl https://github.com` = 200；已改用 Contents API 完成远端写入（未 force push、未改写历史） |
| WORKTREE_CLEAN | `YES`（`git status --short` 为空） |
| Android 树 HEAD | `e68948d5`（`git log -1 --format=%h -- android/`，与证据 manifest 的 `androidTreeHead` 一致） |
| 本轮提交 | `245d7d9e` docs: record local post-freeze acceptance evidence；`ce23b6d3` docs: pin final HEADs and correct 1.5 Records filter note；`a3d08e63` docs: make final HEAD reference self-verifying（**该提交即运行时证据的绑定 HEAD**，run `40686250651`）；最后再次更新本报告引用的 docs 提交（`git log -1 --format=%H -- LOCAL_FINAL_REPORT.md`）。全部快进 push、非 force push，只改根目录文档 |
| PR | #1，open，未合并（`PR_MERGED = NO`） |

开工时远端 `feat/android-ui-vnext-translation` 已重新核验，与任务书给出的 `e68948d5…` 一致；
本地并未停留在该分支（在 `main`，落后 2066 个提交），已安全 `checkout -B` 到远端同名分支，未丢弃任何本地改动
（开工时 `git status --short` 为空）。

远端该 HEAD 的既有自动证据：workflow run `40640200788`（`Android UI vNext Post-Freeze Production Evidence`，
`completed/success`），phone 1.0 / 1.5 两个 matrix 均成功。

> **证据绑定（重要）**：本轮把 docs 提交推上去后，仓库的 `pull_request → main` 触发器在同一分支上自动重跑了
> Post-Freeze 取证，因此拿到了一份**与最终内容 HEAD 完全一致**的证据：run `40686250651`，
> `head_sha = a3d08e63307230e0de4fc5400464471c5094007d`，`conclusion = success`。
> 本报告 §3.2 与 Gate Matrix 以这份 **新证据** 为准（`manifest.sourceHead == a3d08e63…`，
> `androidTreeHead == a3d08e6`）；更早的 `40640200788`（`e68948d5`）仅作历史对照。
> 2026-10-28 20:24–20:33（本地）实测：`status=in_progress → completed/success`。

> **传输事件（如实记录）**：把本报告写入远端时，本机 git 的 HTTPS 传输层开始稳定失败
> （`schannel: failed to receive handshake, SSL/TLS connection failed`，改用 `openssl` 后端则是
> `error:0A000126:unexpected eof while reading`；`http.proxy=` 直连同样失败），而同一时刻
> `curl.exe https://github.com` 返回 200、`Invoke-RestMethod` 亦失败 —— 即**只有 git 的 TLS 通道被阻断**，
> 不是仓库或认证问题。
> 因此远端更新改走 GitHub Contents API（`PUT /repos/…/contents/LOCAL_FINAL_REPORT.md`），
> 生成提交 `32d9293f…`（`parent = a3d08e63`）。本地对应提交内容与远端**逐字节相同**
> （blob `aef6c960…` 两侧一致），但对象身份不同，因此本机 `rev-list --left-right` 会显示 1/1。
> 网络恢复后只需 `git fetch origin && git reset --hard origin/feat/android-ui-vnext-translation`
> 即可完成对齐 —— 该操作不丢任何内容（两侧树相同）。
> 全程**没有 force push**，也没有改写既有历史。

---
## 2. Build

工具链（全部安装到 `C:\Users\30441\pdig-toolchain`，**用户目录**，未做系统级改动，无需管理员）：

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | Temurin 21.0.12.1+1 | 仓库根 `build.gradle.kts` 强制要求 JDK 21 |
| Gradle | 8.9 | wrapper 声明的版本；wrapper 自身下载在 `services.gradle.org` 上长期 0 进度，改用同版本发行包 |
| Android SDK | platform-tools 37.0.1 / platforms;android-36 / build-tools 36.0.0 / emulator 37.2.12 | `sdkmanager --list_installed` 实测 |
| Node / Python | v22.23.4 / 3.12.10 | 备用（core/ 与 harness 依赖） |

构建命令（实际执行，日志 `artifacts/runtime-evidence/local-final-e68948d/logs/gradle.txt`）：

```
gradle.bat :repos:test :app:testProductionDebugUnitTest :app:assembleProductionDebug \
  :app:assembleProductionDebugAndroidTest --no-daemon --stacktrace \
  -PpdigProductionUiGeneration=vnext -PpdigProductionVNextCutoverApproved=true
```

结果：**`BUILD SUCCESSFUL in 14m 2s`**，`78 actionable tasks: 74 executed, 4 from cache`，退出码 0。

> 注：仓库自身对非 ASCII 工程路径有内置适配（`android/settings.gradle.kts` 把构建输出重定向到
> `%USERPROFILE%\pdig-build`），因此产物不在 `android/app/build` 下。这是仓库既有设计，不是本轮改动。

### 测试（真实执行，未删除、未跳过、未放宽）

| 模块 | 测试类 | 用例 | 失败 | 错误 | 跳过 |
| --- | --- | --- | --- | --- | --- |
| `:repos:test` | 3 | 13 | 0 | 0 | 0 |
| `:app:testProductionDebugUnitTest` | 47 | 251 | 0 | 0 | 0 |
| **合计** | **50** | **264** | **0** | **0** | **0** |

### BuildConfig 两把钥匙（直接读取生成文件，不是从命令推断）

`C:\Users\30441\pdig-build\app\generated\source\buildConfig\production\debug\com\pdig\app\BuildConfig.java`：

```
8: public static final String APPLICATION_ID = "com.pdig.app";
9: public static final String BUILD_TYPE = "debug";
11: public static final int VERSION_CODE = 2;
12: public static final String VERSION_NAME = "0.3.1";
14: public static final String GIT_SHA = "e68948d5";
16: public static final String PRODUCTION_UI_GENERATION = "vnext";
18: public static final boolean PRODUCTION_VNEXT_CUTOVER_APPROVED = true;
```

即 ProductionDebug 确实是 `vnext` + `true` 的双钥匙候选，且 `GIT_SHA=e68948d5` 与 HEAD 一致。

### APK 身份

| 项 | 值 |
| --- | --- |
| local APK SHA-256 | `942358fdbc571ae24fd7eaa86b718898e20c1af451bed585ddb4207e72d234a7` |
| androidTest APK SHA-256 | `33192a2c442175ac81b520dc23598783ff15db1e1cc0c3d705c0449b25dfe7e7` |
| package / versionCode / versionName | `com.pdig.app` / `2` / `0.3.1` |
| compileSdk（badging） | 36 |
| signer | `C=US, O=Android, CN=Android Debug`，SHA-256 `6edd40038055b629b34c1299a6da01a0e9081de5afed57fe91287593390cc5b1` |
| installed APK SHA-256 | **NOT_RUN**（无可启动模拟器，见 §3） |

CI 侧同内容 HEAD 的 APK（不同构建机，不要求与本地字节一致，run `40686250651`）：`phone` = `ebb476b8…9172`，
`phone-font15` = `dbef57e2…fae0`。本地 APK 与 CI APK 的 SHA 不同属于正常现象，真正绑定的是
`source HEAD + BuildConfig + 本机 APK SHA + package`。

---

## 3. Runtime

### 3.1 本地模拟器（真实尝试，失败）

| 尝试 | 参数 | 结果 |
| --- | --- | --- |
| #1 | `-avd pdig_api36 -no-window -no-audio -no-boot-anim -no-snapshot -gpu swiftshader_indirect -accel off` | qemu 进程以 `-1073741819`（0xC0000005 access violation）退出，adb 无设备（`logs/emulator.txt`、`logs/emulator-run.out.txt`） |
| #2 | 同上但 `-gpu off` | 曾出现 `emulator-5554 offline`，约 2 分钟后进程消失，`sys.boot_completed` 始终为空（`logs/emulator-run2.out.txt`） |

根因（可复核）：`emulator -accel-check` → `accel: 6 / Android Emulator hypervisor driver is not installed on this machine`，
且宿主 `HypervisorPresent=False`（无 WHPX/Hyper-V）。x86_64 API36 镜像在纯 TCG 软件模拟下无法稳定启动。

**结论**：`LOCAL_EMULATOR_RUNTIME = FAIL（环境限制，非产品缺陷）`，按已批准 fallback 使用
`CI_EXACT_HEAD` 证据；不把 CI 结果表述为"本地运行"。

### 3.2 Exact-HEAD CI 证据（fallback，provenance = CI_EXACT_HEAD）

| 矩阵 | run | entries | sourceHead | apkSha256 | fontScale | assertionsMissing | humanReview |
| --- | --- | --- | --- | --- | --- | --- | --- |
| phone 1.0 | **40686250651** | 22 | `a3d08e63…` | `ebb476b8…` | 1.0（device） | `[]` | `NOT_RUN` |
| phone 1.5 | **40686250651** | 22 | `a3d08e63…` | `dbef57e2…` | 1.5（device） | `[]` | `NOT_RUN` |

设备画像：`sdk_gphone64_x86_64`，API 36，1080×2400，density 420，`dataMode=isolated-postfreeze-evidence-reality`。
共 58 条断言/矩阵，0 缺失。两个矩阵的 PNG（22）+ UIAutomator XML（22）齐全。

### 3.3 设备矩阵

| 矩阵 | 状态 | Provenance |
| --- | --- | --- |
| Phone 1.0 | PASS | CI_EXACT_HEAD |
| Phone 1.5 | PASS | CI_EXACT_HEAD |
| Medium（1200×1920@240） | NOT_RUN | 无本地可启动模拟器；该 HEAD 无 Production medium CI 证据（`tablet-pixels` 工作流走 `previewDebug`，且该 HEAD 无运行记录） |
| Expanded（2560×1600@320） | NOT_RUN | 同上 |
| 真机 | NOT_RUN | 无连接的 Android 真机 |
| 本地模拟器 | FAIL | 两次尝试崩溃，见 §3.1 |

### 3.4 语义/布局证据（我自己对 XML 做的独立分析）

`artifacts/runtime-evidence/local-final-e68948d/analysis/xml-bounds.csv`，覆盖 44 个屏幕（22×2 字级）：

- 超出屏幕边界的节点：**0**
- 可点击且带标签的 <48dp 触控目标：**0**（density 420 → 48dp = 126px）
- 叶子文本节点相互重叠（>30% 面积）：**0**

代表性几何（`production-now.xml`，1080×2400）：五个一级入口文字 y=2253，其可点击容器
`[x,2127][x2,2337]`，距屏幕底部 63px（24dp），未与系统导航区重叠。

---

## 4. Production journeys

以下逐项判定；`PASS` 均附可核对的证据文本。

### 4.1 Account / Email / Service Change boundary — PASS

三处证据（`MANIFEST.json` assertions + 对应 XML 全文）同时包含：

- 账户：`此对象暂不能直接创建变更计划` + `账户变更尚未开放正式可执行模板；不会用通用步骤替代真实变更语义。` + `查看变更中心 →`
- 邮箱：`此对象暂不能直接创建变更计划` + `邮箱更换尚未开放正式可执行模板；恢复与认证影响不会由界面自行猜测。` + `查看变更中心 →`
- 服务：`此对象暂不能直接创建变更计划` + `服务迁移尚未开放正式可执行模板；不会把关系数量当作迁移计划。` + `查看变更中心 →`

负向核查（邮箱 XML 全文）：`Passkey` / `Authenticator` / `MFA` / `通行密钥` / `Secure Enclave` / `provider` /
`唯一恢复` 全部 **present=False**；服务页无 `风险高` / `唯一依赖` / `迁移完成度`，并明确写
"连线仅代表已确认的对象关系，不代表独立恢复路径或安全结论"。

### 4.2 Device truthful empty — PASS

`production-device-empty` 断言命中：`当前没有该类型的已确认对象。空列表不代表外部不存在，只表示这里尚未记录。`
seed 侧注释亦记录：该构建中 Device 节点无法经任何 governed runtime 路径创建
（`android/app/src/androidTest/kotlin/com/pdig/uivnext/evidence/PostFreezeEvidenceSeedTest.kt`）。
未注入任何 synthetic Production Device。

### 4.3 Make-Before-Break — PASS

| 状态 | 证据断言 |
| --- | --- |
| Current | `变更全程` / `当前` / `过渡中` / `完成后（计划）` |
| 准备 | `记录完成这一步`（真实改变 authority，随后出现待验证态） |
| 迁移完成未验证 | `动作已记录完成，结果尚未验证` + `确认验证这一步` + `确认验证会写入正式记录` |
| 旧号码停用 blocked | `停用旧手机号（新路径全部验证后）` + `等待前置步骤` + `新手机号迁移与恢复路径尚未全部验证；全部验证完成前不能停用旧手机号。` |
| 验证完成 → eligible | `production-change-retirement-eligible` / `-cta` 断言命中 |
| After | `计划预期 · 未发生 · 完成并验证前不代表现实` |

JVM 侧同语义回归（264 个用例内、0 失败）：`ProductionChangeStageSummaryTest` 断言
`全部验证完成前不能停用旧手机号`、`计划预期 · 未发生 · 完成并验证前不代表现实`；
`ProductionNextStepTest` 断言 stale 时下一步为 `重新分析当前影响`。
源码 `ProductionChangePlanScreen.kt:447` 为该 blocker 的唯一文案来源。

### 4.4 stale → reanalysis — PASS

- stale 页：`基础设施已发生变化`（1 次）、`重新分析当前影响`（2–3 次），且
  **`记录完成这一步` = 0、`确认验证这一步` = 0**（1.0 与 1.5 均如此）→ fail-closed 成立。
- 边界：`使用上方的「重新分析当前影响」；界面不会用旧快照继续写入。`
- 重新分析后：`重新分析完成`、`分析基准 6 → 8`、`新增 1 · 已不在当前影响 0 · 状态变化 0`、
  `分析时数据版本 8 · 当前数据版本 8`、
  `这里只比较重新分析前后的正式影响投影；不会自动把任何步骤标记为已完成或已验证。`
  该页 `记录完成这一步` = 0、`确认验证这一步` = 0 → 重新分析不会自动 done / verified。

> delta 的 removed / status-changed 分支计数为 0 是本 seed 的数据事实（只新增 1 项影响），
> 不是能力缺失：UI 三类计数同时呈现，本轮没有触发后两类。

### 4.5 Records 三时钟 — PASS

`production-records-trace` 断言与 XML 全文：

- `发生时间未记录 · 记录于 2026-10-28`（发生 ≠ 记录）
- `来源：正式变更计划 · 验证证据未记录`（unknown ≠ none）
- `已记录完成` 与 `已验证` 作为两个独立状态并排出现
- 分组说明明确：`"已记录完成"和"已验证"是两个独立状态，缺少证据引用时如实说明。`

源码 `ProductionRecordTrace.kt:45/62` 是上述两段文案的唯一来源，无 `now()` 兜底。

### 4.6 FLAG_SECURE — PASS（语义层）

- `MANIFEST` 中 `secureWindowExpected=true`：Records、Records trace、三个 boundary、Device collection、Change phone、stale/reanalyzed。
- `production-lock` / `production-now` / `production-change-center` 为 `false`（根页面不强制）。
- 源码只有一处实现：`android/app/src/main/kotlin/com/pdig/app/ui/SecureWindow.kt`（按路由 add/clear `FLAG_SECURE`），无 debug 旁路、无"为截图放开"的分支。

`PIXEL_VISUAL_REVIEW = UNAVAILABLE_DUE_TO_FLAG_SECURE`（且本会话无图像读取能力）。

### 4.7 导航：System Back / Header Up / 五项一级入口

- 五项一级入口：`NavigationModel.kt:24-30` 为 `NOW/INFRASTRUCTURE/CHANGE/RECORDS/ME`，
  `UiVNextModels.kt:21-25` 的中文标题为 `现在/基础设施/变更/记录/我`；XML 中五者在同一行按序出现，
  `我` 位于最右（x=967），即第五个一级入口。**PASS**
- System Back：`BackHandler(enabled = app.canGoBack()) { app.back() }`（`ProductionVNextShell.kt:77`），
  `back()` 弹真实 history 栈（`VAppState.kt:384-392`）；CI 证据的 `navigationSequence` 中实际存在
  `基础设施 > 账户 > 主账户 > BACK > BACK > 基础设施` 的运行时遍历。**PASS**
- Header Up：`upDestination()` 明确 `ACCOUNT_DETAIL → ACCOUNTS`（`VAppState.kt:405`），
  且 `navigateUp()` 会穿栈避免与 Back 互相打转（`VAppState.kt:423-442`）。**PASS（STATIC_AUDIT）**
  —— 本轮未在设备上实际点击 Header Up（无可用设备），运行时点击记为 `NOT_RUN`。

### 4.8 Records filter 可访问性 — PASS

`production-records.xml` 五个筛选（全部/需处理/待验证/已记录完成/已验证）的父亲节点均
`focusable=true`，当前项 `selected=true`，尺寸（density 420）：

| 筛选 | 1.0 尺寸 | 1.5 尺寸 |
| --- | --- | --- |
| 全部（selected） | 59.8×49.9dp | 77.7×62.1dp |
| 需处理 | 72.0×49.9dp | 96.0×62.1dp |
| 待验证 | 72.0×49.9dp | 96.0×62.1dp |
| 已记录完成 | 96.4×49.9dp | 105.1×62.1dp |
| 已验证 | 67.8×49.9dp | 在滚动视口外（第 4 项已在右边缘截断，行需横滑） |

即选择状态不是只靠颜色（`selected` 语义存在），且 ≥48dp。

### 4.9 隐私遮罩 — NOT_RUN

本轮证据矩阵未采集"隐私开启"态（默认 OFF 属冻结设计）。源码存在遮罩管线
（`ProductionChangeEntry.kt:35/54/65`、`ProductionChangePlanScreen.kt:80-99`），但
**未在本轮运行时验证**覆盖 Card/Phone/Email/Account/Service/Device/Membership/relation peer/plan title/
action title/Records/Search，也未做 accessibility label / contentDescription / Recents 的旁路检查。
因此 `PRIVACY_MASKING = NOT_RUN`，不做任何 PASS 声明。

> 1.5 实测：筛选行是 `HorizontalScrollView bounds=[42,993][1038,1156]`，可见 4 项，
> 第 5 项（已验证）位于视口右侧之外 —— UIAutomator 只报告已渲染节点，因此不是缺少控件。

---

## 5. Bugs found

**0 个 P0，0 个 P1，0 个 P2。**

因此：**0 product source changes**（本轮没有为了"有产出"而修改任何产品代码、测试或 harness）。

排查过程中确认过、但经证据核实**不是**缺陷的三项（记录以说明判断依据）：

| 疑似点 | 判断 | 依据 |
| --- | --- | --- |
| Records 筛选 chip 自身 `clickable=false / selected=false` | 非缺陷 | `selected` 语义在其父 `View` 节点（`selected=true, focusable=true`），尺寸 ≥48dp；这是 Compose `selectable` 的标准语义落点 |
| 1.5 字级下 Records 只出现 4 个筛选 | 非缺陷 | 筛选行是 `HorizontalScrollView`，第 5 项在可视区外；行可横向滚动 |
| 底部导航距屏幕底 63px | 非缺陷 | 63px = 24dp，正是手势导航条高度；未与系统区重叠 |

未测试适配：本轮没有为了让任何检查变绿而修改 harness 断言、放宽 stale gate、绕过 FLAG_SECURE
或注入 synthetic Production 状态。

---

## 6. Remaining limitations

1. **`PIXEL_VISUAL_REVIEW = UNAVAILABLE`**：本会话没有图像读取能力，且 Production Records 等页面受
   `FLAG_SECURE` 保护（ADB 截屏黑屏属预期安全行为）。所有视觉结论止步于 XML/语义/布局边界。
2. **本地模拟器不可用**：宿主无 WHPX/Hyper-V（`HypervisorPresent=False`，`-accel-check` 返回 6），
   x86_64 API36 镜像在 TCG 软件模拟下两次崩溃。运行时证据来自 exact-HEAD CI（`CI_EXACT_HEAD`），
   不是本机运行。
3. **Medium / Expanded 未验收**：本地无可用设备；该 HEAD 也没有 Production medium/expanded 的 CI 证据
   （`android-ui-vnext-tablet-pixels.yml` 构建的是 `previewDebug`，不能当作 Production authority）。
4. **真机未验收**：无连接的 Android 真机。
5. **Header Up 仅静态验证**：源码映射与穿栈逻辑已确认，但没有在设备上实际点击。
6. **隐私遮罩未运行时验证**（见 §4.9）。
7. **Device 仍无 governed runtime-create 路径**：`Device` 的 truthful empty 是正确结果，不是待修缺陷；
   在正式数据语义出现前不应伪造。
8. **iOS Runtime Visual 仍为既有外部门禁**：同 HEAD `iOS = success`、`iOS Runtime Visual = failure`；
   本轮 Android 改动未触碰 shared Canonical，不进入 iOS 修复。
9. **`cancelPlan` 仍不存在**：Canonical 有 `CANCELLED` 枚举，但 Android 无 `cancelPlan` authority 与
   `cancelledAt` 事件时间；涉及取消记录一律标 `DOMAIN_CAPABILITY_REQUIRED`，本轮也未用 `updatedAt`/`now()` 伪造。

---

## 7. Gate Matrix

| Gate | 结果 | Provenance / 依据 |
| --- | --- | --- |
| REMOTE_HEAD_LOCKED | **PASS** | `git fetch` + `rev-parse origin/…` = `e68948d5…`，与任务书一致 |
| SOURCE_BUILD | **PASS** | `BUILD SUCCESSFUL in 14m 2s`（`logs/gradle.txt`） |
| JVM_TESTS | **PASS** | 264 用例 / 0 失败 / 0 跳过（`:repos:test` 13、`:app:testProductionDebugUnitTest` 251） |
| PRODUCTION_ANDROID_TEST | **PASS（仅组装）** | `:app:assembleProductionDebugAndroidTest` 成功，产出 `app-production-debug-androidTest.apk` |
| PRODUCTION_ANDROID_TEST_RUN | **NOT_RUN** | 无可启动模拟器/真机，instrumentation 未执行 |
| PHONE_1_0_RUNTIME | **PASS** | CI_EXACT_HEAD run 40686250651 @ `a3d08e63`，22 entries，assertionsMissing=[] |
| PHONE_1_5_RUNTIME | **PASS** | 同上，fontScale=1.5，22 entries，assertionsMissing=[] |
| MEDIUM_RUNTIME | **NOT_RUN** | 无本地设备；该 HEAD 无 Production medium CI 证据 |
| EXPANDED_RUNTIME | **NOT_RUN** | 同上 |
| REAL_DEVICE_RUNTIME | **NOT_RUN** | 无真机连接 |
| ACCOUNT_CHANGE_BOUNDARY | **PASS** | XML + manifest 断言（§4.1） |
| EMAIL_CHANGE_BOUNDARY | **PASS** | XML + manifest 断言；禁止词全 0（§4.1） |
| SERVICE_CHANGE_BOUNDARY | **PASS** | XML + manifest 断言（§4.1） |
| DEVICE_TRUTHFUL_EMPTY | **PASS** | 空状态文案 + seed 无 Device 路径（§4.2） |
| MAKE_BEFORE_BREAK | **PASS** | 七状态断言链（§4.3） |
| DONE_NOT_VERIFIED | **PASS** | `动作已记录完成，结果尚未验证` + `确认验证这一步`（§4.3） |
| RETIREMENT_GATE | **PASS** | `等待前置步骤` + 停用门槛文案 + JVM 回归（§4.3） |
| AFTER_PLAN_PROJECTION | **PASS** | `计划预期 · 未发生 · 完成并验证前不代表现实`（§4.3） |
| STALE_FAIL_CLOSED | **PASS** | stale XML 中 `记录完成这一步`=0、`确认验证这一步`=0（§4.4） |
| REANALYSIS_AUTHORITY | **PASS** | 重新分析后投影取自 authority：`分析时数据版本 8 · 当前数据版本 8`（§4.4） |
| REANALYSIS_DELTA | **PASS** | `分析基准 6 → 8`、`新增 1 · 已不在当前影响 0 · 状态变化 0`（§4.4） |
| RECORDS_THREE_CLOCKS | **PASS** | `发生时间未记录 · 记录于 …`、`验证证据未记录`（§4.5） |
| RECORDS_FLAG_SECURE | **PASS（语义）** | manifest `secureWindowExpected=true`；单一实现无旁路；像素不可见（§4.6） |
| PRIVACY_MASKING | **NOT_RUN** | 本轮无隐私开启态证据（§4.9） |
| SYSTEM_BACK_HISTORY | **PASS** | `back()` 弹真实栈 + CI 运行时 BACK 遍历（§4.7） |
| HEADER_UP_PARENT | **PASS（STATIC）** | `ACCOUNT_DETAIL → ACCOUNTS`（`VAppState.kt:405`）；运行时点击 NOT_RUN |
| FIVE_PRIMARY_DESTINATIONS | **PASS** | 源码 `NavigationModel.kt:24-30` + XML 五入口含 `我`（§4.7） |
| POSTFREEZE_MANIFEST | **PASS** | 22/22 entries，sourceHead 匹配，assertionsMissing=[]，humanReview=NOT_RUN |
| POSTFREEZE_LAYOUT_BOUNDS | **PASS** | CI gate + 我的独立 XML 分析：0 越界 / 0 过小触控 / 0 文本重叠 |
| HUMAN_VISUAL_CANDIDATE | **READY** | 见 §8 |
| HUMAN_VISUAL_ACCEPTANCE | **NOT_RUN** | 只能由用户看过候选后决定 |
| ANDROID_REFERENCE_FREEZE_CURRENT | **NO** | 未冻结（HOLD） |
| STORE_RELEASE | **HOLD** | 无生产 keystore；debug 证书候选非上架件 |
| PR_MERGED | **NO** | PR #1 未合并 |

---

## 8. Human Visual Candidate（建议优先看的 12 页）

证据根：`artifacts/runtime-evidence/local-final-e68948d/`，每页同名 `.png` 与 `xml/<label>/<shot>.xml`；
1.0 与 1.5 各有全套（`phone-1.0/`、`phone-1.5/`）。

| # | shot | 看什么 |
| --- | --- | --- |
| 1 | `production-change-current` | 当前 / 过渡中 / 完成后（计划）是否形成连续过程，而非 checklist |
| 2 | `production-change-pending-verification` | `动作已记录完成，结果尚未验证` 是否一眼可懂（done ≠ verified） |
| 3 | `production-change-retirement-blocked` | 停用旧号码的阻塞是否给出原因与下一步，而不是灰按钮 |
| 4 | `production-change-after-plan` | After 是否明确是计划投影（`计划预期 · 未发生`） |
| 5 | `production-change-stale` | `基础设施已发生变化` 是否是"停"而不是"错"，且没有执行入口 |
| 6 | `production-change-reanalyzed` | delta 是否像产品说明而非 diff 日志（6→8、新增 1） |
| 7 | `production-account-detail-change-boundary` | Change 区域是否退居次要层级、不像报错框 |
| 8 | `production-email-detail-change-boundary` | 同上，且无认证/恢复能力猜测 |
| 9 | `production-service-detail-change-boundary` | 关系数量是否被克制表述 |
| 10 | `production-device-empty` | 空状态是否像产品设计而非 debug 空页 |
| 11 | `production-now` | 五项一级入口与信息密度（首屏是否先给对象与当前事实） |
| 12 | `production-change-center` | 变更中心的入口层级与计划列表密度 |

---

## 9. 证据清单

```
artifacts/runtime-evidence/local-final-e68948d/
├── phone-1.0/            MANIFEST.json + 22 PNG + 22 XML + seed-logs + apk/
├── phone-1.5/            同上（fontScale 1.5）
├── apk/                  app-production-debug.apk、androidTest APK、SHA256SUMS.txt、IDENTITY.txt
├── analysis/             xml-bounds.csv（44 屏边界/触控/重叠分析）
└── logs/                 gradle.txt、BUILD_CONFIG(.java/.txt)、adb-device.txt、package.txt、
                          emulator.txt、emulator-run.out/err.txt、emulator-run2.out/err.txt
```

> `artifacts/runtime-evidence/local-final-e68948d/` 与 `.runtime/` 仅存在于本机，已加入
> `.git/info/exclude`，不进仓库（避免把 100MB+ 二进制证据提交进 Git）。

---

## 10. 本轮结论

- 远端 exact HEAD `e68948d5…` 上，Android Production VNext 双钥匙候选**可以构建**、**264 个 JVM 用例全绿**、
  **androidTest APK 可组装**。
- 生产语义（三条 Change 边界、Device truthful empty、Make-Before-Break 七态、stale fail-closed、
  reanalysis delta、Records 三时钟、FLAG_SECURE、五项一级入口、Back/Up）在 exact-HEAD 运行时证据与源码
  双向核对下**未发现 P0/P1 缺陷**。
- 因此本轮 **0 product source changes**，只新增本报告与 `WORK_STATUS.md` 的状态记录。
- 本地模拟器受宿主虚拟化能力限制无法启动，Medium/Expanded、真机、隐私遮罩与像素级视觉仍未验收，
  已在 §6 与 Gate Matrix 中如实标注。

**HUMAN_VISUAL_CANDIDATE = READY ／ HUMAN_VISUAL_ACCEPTANCE = NOT_RUN ／ ANDROID_REFERENCE_FREEZE = HOLD ／ STORE_RELEASE = HOLD**
