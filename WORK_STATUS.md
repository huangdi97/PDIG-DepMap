# WORK_STATUS.md

> 本文件由执行 Agent 持续更新。不要删除历史关键结论。
> 分支历史：`feat/mvp02-global-source`（MVP02，tag v0.2.0-mvp02）→ `engineering/baseline-v1`
> （Engineering Baseline V1 PASS，2026-09-13）→ `feat/mvp03-living-graph`（MVP03，tag v0.3.0-mvp03）
> → `feat/mvp03-living-graph`（Production RC V1，2026-09-13）
> → `feat/mvp03-living-graph`（FINAL PRODUCTION CLOSURE V1，2026-09-14）
> → `feat/mvp03-living-graph`（PLATFORM BRINGUP，2026-09-15，**成果零提交**）
> → **`feat/mvp03-living-graph`（AGENT HANDOFF PLATFORM BRINGUP / PRODUCTION RC CONTINUE，当前）**。

## Current

- Phase: **PDIG PLATFORM BRINGUP & RELEASE VALIDATION（Production RC V1 接力）**
- Current Gate: `PRODUCTION_RC_V1 = PARTIAL_WITH_REPORT`
- Global status: **`ALL_DONE = NO`** · **`TASK_COMPLETE = NO`**
- CURRENT_HEAD: **以 `git log --oneline -1` 为准**（本轮基线 `7295fc6`；本轮提交链见
  `git log --oneline 7295fc6..HEAD`；报告不写入自身 SHA）
- CURRENT_BRANCH: `feat/mvp03-living-graph`
- NEXT_GATE: **`UI_BUILD_READY`**（= 解除 B10）
- NEXT_COMMAND（下一位 Agent 的第一步）：
  `git log --oneline -1 && git status --short -uall && cd core && npm run check`
- Blocker (only real external): **B10 / DCloud 账号** + 设备 / 签名 / macOS / 商店账号 / 真实账单

### 本轮（2026-09-15 接力轮）做了什么

1. **恢复现场并保全前序工作（最重要）**：进入时工作区**不 clean** —— 上一轮
   PLATFORM BRINGUP 的**全部**产物（源码改动 + 23 份文档 + AAR）都在工作区、**零提交**。
   逐文件判读后按 11 个逻辑提交全部入库（用 plumbing 建链，见下「Git 故障」）。
2. **独立复验，不采信旧报告**：逐项复算产物哈希、重跑门禁、重跑 Android 构建、复查环境。
3. **推翻 B10 的旧定性**：`cli pack` 是 DCloud 官方定义、支持 uni-app x 的打包命令；
   真正闸门是**账号**。旧结论「CLI 无 build 命令 ⇒ 只能靠 GUI」**不成立**（`BLOCKERS.md` B10 已改）。
4. **UTS 层首次获得真实编译证据**：发现 UTS 编译器公开在 npm 上，实测 **15/15** 通过
   （5 插件 × Android→Kotlin / iOS→Swift / HarmonyOS→ArkTS），并**因此查出并修复 2 个
   iOS UTS 语法错误**（`do { try } catch` 非法 + `DepmapSchemaV1.shared` 不存在）。新增门禁
   `npm run check:uts`。
5. **修掉上一轮登记的欠账 T-1**：不变量测试的临时目录泄漏（`rmSync`），实测 delta = 0。
6. **修掉本轮开始时发现的真实回归**：7 份平台文档未过 `format:docs:check`（`npm run check` 原本失败）。
7. **修正过期数据**：`docs/ANDROID_BUILD_REPORT.md` 的 AAR 字节数/class 数（44,147→72,376、23→37）。

### 本轮 quality state（实跑）

- `npm run check` → **EXIT=0**
  - format:check PASS；format:docs:check PASS；lint PASS；typecheck PASS
  - **453 passed / 453（43 文件）**
  - architecture PASS（48 files，circular 0）；network PASS（0 原语）；secrets PASS（0）
  - UI 静态门 PASS（30 `.uvue`，24 pages，5 components，34 色）
- `npm run check:uts` → **15/15 compiled, PASS**（本轮新增；未装编译器时 SKIPPED + exit 0）
- `npm run check:invariants` → **18 passed**，`%TEMP%/depmap-inv-*` 泄漏 **delta = 0**（T-1 修复验证）
- **Android 原生构建复现**：`assembleDebug assembleRelease collectArtifacts --rerun-tasks`
  → `BUILD SUCCESSFUL in 7m 50s`，`49 actionable tasks: 49 executed`（无 FROM-CACHE）
  → AAR 字节数与 SHA-256 与 `RELEASE_CANDIDATE_MANIFEST.md` **逐字节一致**
- **HarmonyOS 原生构建复现**：见「本轮 Platform 复现」小节（本轮以 Python 复刻 `build.sh`
  的 ASCII 镜像流程，因为本环境 bash 缺失 `dirname/grep/mkdir/tar/cp`）

### 本轮 Platform 复现（实测口径）

| 平台     | 命令（可直接复制）                                                                                                                                                                                                                            | 结果                                             |
| -------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------ |
| Core     | `cd core && npm run check`                                                                                                                                                                                                                    | EXIT=0（453/453）                                |
| UTS 三端 | `cd core && npm run check:uts`                                                                                                                                                                                                                | 15/15 PASS                                       |
| Android  | `cd platforms/android && JAVA_HOME="<ANDROID_STUDIO_HOME>/jbr" ANDROID_HOME="<ANDROID_SDK_ROOT>" "<GRADLE_HOME>/bin/gradle.bat" --no-daemon assembleDebug assembleRelease collectArtifacts --rerun-tasks --console=plain` | BUILD SUCCESSFUL in 7m 50s；AAR 逐字节复现       |
| Harmony  | 见 `docs/HARMONY_RELEASE_RUNBOOK.md`；本环境需用 Python 复刻 ASCII 镜像（`build.sh` 依赖 bash 工具）                                                                                                                                          | 见 `docs/HARMONY_BUILD_REPORT.md` + 本轮复现记录 |
| iOS      | 不可执行（非 macOS）                                                                                                                                                                                                                          | `IOS_TOOLCHAIN_READY = BLOCKED (B3)`             |

### Git 故障与规避（本工作区特有，务必先读）

`git commit` / `git update-ref` 在本工作区**会成功创建对象但无法推进 HEAD**：`.git/refs/heads/**`
的 loose ref 被外部进程回收，`git update-ref` 返回 0 却不变更 HEAD。实测连续两次 `git commit`
产生的提交**父节点都是基线**，即提交之间不成链。

**规避方式（本轮使用，未用任何被禁止命令）**：

```
git add <paths> && git write-tree
git commit-tree <tree> -p <parent> -m "<msg>"     # 显式建链
# 然后写 .git/packed-refs（packed-refs 稳定；loose ref 被删也不影响 HEAD 解析）
```

每次提交后必须校验 `git rev-parse HEAD` 与 `git status --short -uall`。
**禁止**：`git reset --hard`、`git clean -fd`、`git checkout .`、`git restore .`、force push。

### 本轮 Git 收口

- **前序零提交工作已全部入库**：19 个已跟踪文件 + 52 项未跟踪内容 → 11 个逻辑提交
  （gitignore / Android 原生 / UTS 桥接 / HarmonyOS 骨架 / iOS 修复 / 占位资产 / AAR 产物 /
  平台文档 / 收口报告 / 文档格式回归修复 / 本轮 UTS+交接文档）
- `git status --short -uall` = **0 行**；`git diff --check` = PASS
- **未 push**（用户未授权）；**未创建 RC tag**（三端均无产品级可安装包，打 RC 标会造成误读）
- `.tmp_audit/`（前序会话的临时工具与探测输出）已加入 `.gitignore`，**保留在磁盘**供复用

### 本轮未提交 / 未做（诚实清单）

- 未安装 HBuilderX 的打包插件、未登录 DCloud 账号（§110：账号登录属真正的用户交互闸门）
- 未下载 Android system-image（约 1.5 GB；即使装上 AVD 可启动，**仍无 APK**，B10 未解则无意义）
- 未删除 `%TEMP%` 中 2498 个历史 `depmap-inv-*` 残留目录（批量删除违反本环境 safe-delete 纪律；
  T-1 已修复，后续不再增长）
- 未实现 iOS `DepmapContainerV1.swift`（G-1，需 macOS 才能验证，写不可编译的加密实现风险过高；
  已写入 `docs/IOS_RELEASE_HANDOFF.md` 作为 Mac 侧首要待办）

## Platform Matrix（本轮口径）

| Platform               | IMPLEMENTED | STATIC_AUDITED    | COMPILED                 | TESTED     | DEVICE_VERIFIED | STORE_READY |
| ---------------------- | ----------- | ----------------- | ------------------------ | ---------- | --------------- | ----------- |
| Core（Node 22）        | YES         | YES               | YES                      | YES（453） | N/A             | N/A         |
| UTS（5 插件 × 3 平台） | YES         | YES               | **YES（15/15，降级层）** | N/A        | N/A             | N/A         |
| UI（uni-app x，24 页） | YES         | YES（9 类静态门） | **NO（B10）**            | —          | NO              | NO          |
| Android（原生核心）    | YES         | YES               | **YES（AAR，可复现）**   | YES（8/8） | NO              | NO          |
| Android（产品包）      | YES（源码） | YES               | **NO（B10）**            | NO         | NO              | NO          |
| HarmonyOS（原生验证）  | YES         | YES               | **YES（HAP，ArkTS）**    | NO         | NO              | NO          |
| HarmonyOS（产品包）    | YES（源码） | YES               | **NO（B10）**            | NO         | NO              | NO          |
| iOS（原生）            | YES         | YES               | **NO（B3，非 macOS）**   | NO         | NO              | NO          |

## Current failures

**无失败测试。** 未执行项全部是外部闸门（B10 账号 / B3 macOS / B18·B24 设备 / B4·B7·B8·B9 签名 /
B5·B6 商店账号）或 Real Data（NOT_RUN），**不虚报、也不为凑数降门禁**。

## External blockers

见 `BLOCKERS.md`（B10 已按本轮实测重写为 AUTH 类）与 `STORE_EXTERNAL_BLOCKERS.md`：

- 阻断产品级构建：**B10（DCloud 账号）**、B1（keystore）、B2（AGC 签名）
- 阻断真机：**B18 / B24（无设备；现有 AVD 缺 system image）**
- 阻断 iOS：B3（macOS）、B8/B9（Apple 账号与证书）
- 阻断商店：B5/B6/B11/B12/B12b/B14–B17
- 环境：B23（沙箱拦截 clean clone / 破坏性 `npm ci`）
- 工程缺口（非用户可解）：B20（账单解析桥接）、B21（`.depmap` 加解密桥接）、B22（设备端 Impact 镜像）

## Next

1. **解除 B10**（唯一能同时解锁 Android/Harmony 产品级产物与 UI 编译的闸门）：
   `cli open` → `cli user login` → `cli project open --path "<repo>\app"` →
   `cli pack --project app --platform android --android.packagename <正式包名> --android.androidpacktype 1`
   （或 GUI「发行 → 原生App-云打包」）。首次需在 DCloud 后台换取正式 appid
   （当前 `app/manifest.json` 是离线占位 `__UNI__DEPMAP01`）。
2. **B24**：接真机，或在 SDK Manager 安装
   `system-images;android-35;google_apis_playstore;x86_64` 让现有 AVD `Medium_Phone_API_35` 可启动。
3. **B4**：生成 release keystore（放 `signing/`，已 gitignore），配置 Gradle signingConfig。
4. **iOS（不依赖账号，可并行）**：补 `platforms/ios/swift/DepmapContainerV1.swift`（G-1）、
   把 SQLCipher 接入 SPM（G-2）、把 `XCTSkip` 换成真实断言（G-3）；按 `docs/IOS_RELEASE_HANDOFF.md` 执行。
5. **B13**：真实账单双 Gate（`validate-real-bill.ts` 就绪；Real Data 保持 NOT_RUN）。

**进入 MVP04 的前提**：至少一台真实设备完整 E2E PASS + 产品级 Build artifact PASS +
Backup/Restore PASS + UI device QA PASS + Release blockers 清晰。**当前均未满足。**

---

## 历史：PLATFORM BRINGUP（2026-09-15，成果零提交，本轮已入库）

- Android 原生核心真实编译：AAR 产出，黄金向量 8/8 PASS；修复 D-1..D-10（含 2 个 P0：
  跨端 Base64 契约破裂、容器解析字段冲突使 Android **无法解密任何容器**）。
- HarmonyOS：原工程只有 3 文件且 `app.json5` 缺 Stage 模型 `"app"` 顶层键，
  **从未被 hvigor 解析过**；补齐 9/9 骨架后 `BUILD SUCCESSFUL`，产出 HAP（ArkTS 字节码）。
- UI：修复 3 类「首次编译必炸」的 UTS 缺陷（R-1 文件被截断、R-2 引用不存在的模块、
  R-3 UTS 调 Kotlin `suspend fun`），新增 Kotlin 侧回调桥接 `UtsSecurityBridge`。
- iOS：修 `Package.swift` 空 target（`exclude` 导致无源文件）与 `LAPolicy()` 实例化错误。
- 生成 `PLATFORM_BRINGUP_PRE_AUDIT.md` / `PLATFORM_RELEASE_MATRIX.md` /
  `PRODUCTION_RUNTIME_UI_AUDIT.md` / `RELEASE_CANDIDATE_MANIFEST.md` +
  `docs/ANDROID_*`（4 份）+ `docs/HARMONY_*`（3 份）+ `docs/IOS_RELEASE_HANDOFF.md`。

## 历史：FINAL PRODUCTION CLOSURE V1（2026-09-14）

- 新增 docs 格式门禁（`.prettierrc.json` + `format:docs`）；删 3 个死导出；18 处 `obj`→`record`；
  `.gitignore` 补 Gradle/Android 本地状态。
- B1/B2 旧前提（「无 JDK / 无 SDK / 无 DevEco」）被实测推翻并按实测重写。
- 登记 T-1（不变量测试临时目录泄漏）→ **本轮已修**。

## 历史：Production RC V1（2026-09-13）

- UI 产品化：新增 Application Service（唯一数据边界）+ 纯规则层；修复 12 页直连 SQLite、
  4 页复算领域逻辑、`drift.uvue` 裸 SQL 手动 bump revision；新增 5 个 `dp-*` 组件与设计 token；
  17 页重写 + 7 页新增（共 24 页）；新增 tabBar 一级导航；去假功能；新增手动声明支付关系；
  修复 4 处依赖方向错误；`check-ui.mjs` 从 0 → 9 类。
- 质量：`check:full` FINAL_EXIT=0；453/453；覆盖率 93.74/82.2/94.28；Stryker 重跑与基线一致。

## 历史：MVP03 FINAL FREEZE（2026-09-13，tag v0.3.0-mvp03）

- P0 修复：PlanReadiness 废除减法，改由 `PlanAction.resolvesImpactKeys[]` 显式 resolution。
- 冻结补测：FR-GR-012 revision property、4 状态机非法迁移负向、rebase 原子性、10k Timeline smoke。
- Targeted mutation 10/10 KILLED，0 critical survived（PARTIAL_WITH_REPORT）。
- 实测：453/453（43 文件）；stability ×3 + focused ×10 全绿。

## 历史：MVP03 交付 Gate 级结论

| Gate                                                       | 结果         |
| ---------------------------------------------------------- | ------------ |
| A Graph Revision（GR-001..012，同事务 bump）               | PASS         |
| B ChangePlan Rebase（PRB-001..011）                        | PASS         |
| C PlanReadiness（三值纯规则，无 confidence/absence 通道）  | PASS         |
| D ScenarioCoverage（四级 + 可解释）                        | PASS         |
| E RealityDrift（RD-001..010，absence 永不触发）            | PASS         |
| F DiscoveryCandidate（不进 Impact / 不 bump revision）     | PASS         |
| G ScenarioTemplate（3 active + planned gate + 政策）       | PASS         |
| H Timeline（确定性投影，可溯源）                           | PASS         |
| I Verification（done ≠ verified，两段式）                  | PASS         |
| J Migration v2→v3（MIG3-001..006）                         | PASS         |
| K depmap compat（golden 不变；payload v3 + v1/v2 migrate） | PASS         |
| L/M/N 回归（MVP01/MVP02/Baseline）                         | PASS         |
| O Security/Privacy（新对象只存引用/ID）                    | PASS         |
| P UI（24 页源码级；UTS 三端已编译；页面编译 BLOCKED B10）  | PASS（静态） |
| Q Documentation                                            | PASS         |

## 产品可用性（诚实口径，未变）

| 能力                                      | 状态                             |
| ----------------------------------------- | -------------------------------- |
| 手动建立对象（卡 / 账户 / 服务）          | **可用**                         |
| 手动声明支付关系（含 required / unknown） | **可用**                         |
| 影响模拟（选中卡 → 受影响下游）           | **可用**                         |
| 创建变更计划 + 计划内影响清单 + 动作/验证 | **可用**（保守口径）             |
| 时间轴 / 待确认项 / 数据来源 / 数据清空   | **可用**                         |
| 账单导入                                  | **不可用**（B20；UI 已如实标注） |
| 加密备份导出 / 恢复                       | **不可用**（B21；UI 已如实标注） |

> 当前构建**在解除 B10 后可以真实安装并真实使用**（不依赖导入即可完成主流程），
> 但覆盖范围小于 MVP01 完整设计。这是 B20/B21 的直接后果，不做粉饰。

## 仓库运维注意（重要）

分支 loose ref（`.git/refs/heads/<branch>/`）在本工作区会被外部进程反复删除，
且 `git commit` 无法推进 HEAD。**规避：用 `commit-tree` 建链并把分支写进 `.git/packed-refs`。**
若出现 "branch has no commits"，从 reflog 取哈希后重写 packed-refs；
**不要**执行 `git reset --hard` / `git clean`。
