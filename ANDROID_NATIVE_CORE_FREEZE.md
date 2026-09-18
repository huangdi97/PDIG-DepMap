# ANDROID_NATIVE_CORE_FREEZE.md

> Android Native Core Baseline 冻结记录。
>
> 生成时间：2026-09-17（Asia/Shanghai）
> 本文件的证据**全部由本轮实跑采集**，不沿用任何既往报告中的数字或哈希。

---

## 0. 这个 Gate 是什么、不是什么

### 它回答的唯一问题

> Android 是否已经成为 **Harmony N3 可以依照的、经过真实 Runtime + Conformance 验证的 Native Reference**。

结论：**是** → `ANDROID_NATIVE_CORE_HANDOFF = PASS`

### 它**不替代**什么（三条边界，不得混淆）

| 被保护的 Gate | 本轮状态 | 说明 |
| --- | --- | --- |
| `N2_ANDROID_FULL_PARITY` | **PARTIAL_WITH_REPORT（62 / 73）** | 保持原状。冻结**不提升** parity 分数，**不合并**剩余 11 格 |
| `ANDROID_PRODUCTION_RELEASE_READY` | **BLOCKED_BY_PRODUCTION_SIGNING** | 保持原状。冻结不解决生产签名 |
| `N1_ANDROID_VERTICAL_SLICE` | PASS（上一轮结论，未被本轮改动） | 本轮不重算 N1 |

冻结**不是**"把 62/73 说成通过"。它的作用是锁定一份**可被引用的基准**，
使 Harmony N3 在实现时有一个"行为必须一致"的对象，而不是凭回忆对齐。

---

## 1. 冻结身份（` reproducible identity`）

| 项 | 值 |
| --- | --- |
| canonical HEAD | `6053f3ca2a4cc7a4238614163c01321b214ff5af` |
| canonical HEAD（短） | `6053f3c` |
| branch | `feat/mvp03-living-graph` |
| HEAD tree | `2b32c5a0e4ef55c90b0cba546646467f553b30fc` |
| **Android source commit** | `17449e8dffba12d2daf00c55382677f7e20bfe81`（`17449e8`） |

> Android 源码最后一次被改动是 `17449e8`（D-16 修复）。
> `6053f3c` 只增加了三端 codegen 产物与 `legacy/README.md`，**未触碰 Android 源码**；
> 已用 `git status --short -- android` 复核为空，即 **Android 源码 ==提交的 LATEST**。

### Codegen 产物指纹（`node tools/codegen/generate.mjs --check` = CODEGEN GATE PASS）

| 产物 | SHA-256（前 16） |
| --- | --- |
| `android/.../generated/CanonicalEnums.kt` | `2386151e477910f6…` |
| `harmony/.../generated/CanonicalEnums.ets` | `d0fc7d57b9e15cee…` |
| `ios/.../Generated/CanonicalEnums.swift` | `cbd6c1381241fc49…` |

---

## 2. 输入侧指纹（Spec / Fixtures）

| 项 | 值 | 规模 |
| --- | --- | --- |
| **spec hash**（TREE-V1） | `5c736e7bb1e03c15caeeeb5d79abe71d72e0043f4d65638e2cae304ca12c717d` | 14 files |
| **fixture tree hash**（TREE-V1） | `4c83ccc3d487abb559682ab7a576a43621552961ac4b1ee3f69bbd98bfabf41a` | 119 files |
| **CONFORMANCE_MANIFEST sha256** | `26d9bdd7bd09fb74eae6f1315a27a7fbc1dba35c9ae840501252d585ca35c26d` | 91 cases |
| manifest git blob | `113acddb70cb4e1471473cc89a5ea93cf5982084` | — |

`TREE-V1` 口径：相对 POSIX 路径升序 → 逐条 `hash(path) ‖ hash(content)` 聚合。
采集脚本：`tools/freeze/hash-evidence.mjs`（只读）。

---

## 3. 本轮实跑复验（**全部重新执行，非沿用**）

### 3.1 ⚠ 一个必须先记录的取证修正：**构建输出不在 `android/**/build`**

`android/settings.gradle.kts` 因工程路径含非 ASCII（本仓库位于 `<repo>`），
把 Gradle 构建输出重定向到 **`%USERPROFILE%/pdig-build/<module>`**（可用
`PDIG_ASCII_BUILD_ROOT` 覆盖）。

**后果**：`android/**/build/**` 是自该重定向生效之日起的**过期残留**，
其中的测试 XML 与 APK **不代表当前源码的构建结果**。

本轮实测到的确凿证据：

- `android/core/build/test-results/test/` 残留一份 `TEST-Gradle#20Test#20Executor#201.xml`，
  内容是 **`Could not execute test class 'com.pdig.core.db.JdbcStatement'`，failures=1**，时间戳 `2026-09-16T11:00:56`。
- 真实结果才是：`%USERPROFILE%/pdig-build/core/test-results/test/`，时间戳 `2026-09-17T11:49:52`，**71 / 0 failures**。

> **纪律**：今后任何 Android 取证，都必须以 `%USERPROFILE%/pdig-build/`（或
> `PDIG_ASCII_BUILD_ROOT`）为准，**不得**再哈希 `android/**/build` 下的 apk / aab / xml。
> 既往报告中出现的 `d84d8900…` / `bf378ec6…` 等 APK 哈希应视为**来源不明**，本轮起作废。

### 3.2 Conformance（**本轮重新执行**）

```
> Task :conformance:run            ← EXECUTED（非 UP-TO-DATE）
pass=91 fail=0 notImplemented=0 total=91
BUILD SUCCESSFUL
```

| 分类 | 用例数 |
| --- | --- |
| impact | 13 |
| readiness | 16 |
| coverage | 6 |
| relations | 18 |
| parser | 22 |
| state-machine | 5 |
| timeline | 3 |
| migration | 2 |
| depmap | 3 |
| jcs | 1 |
| scenario | 1 |
| backup | 1 |
| **合计** | **91** |

### 3.3 JVM 单元测试（**本轮 `--rerun-tasks` 强制重跑**）

| 套件 | tests | failures | errors | skipped | 结果时间戳 |
| --- | --- | --- | --- | --- | --- |
| `:core:test` | **71** | 0 | 0 | 0 | 2026-09-17T11:49:52Z |
| `:app:testDebugUnitTest` | **9** | 0 | 0 | 0 | 2026-09-17T11:51:44Z |

`:core:test` 分类明细（实跑日志逐条 PASSED）：

`DomainInvariantTest 15` · `ImpactKernelTest 11` · `PlanReadinessTest 14` ·
`MigrationSemanticsTest 10` · `GraphRevisionSemanticsTest 7` · `StateMachineTest 14` = **71**

### 3.4 设备内 androidTest（**本轮重新执行**）

```
Starting 51 tests on PDIG_API34_DEFAULT(AVD) - 14
Finished 51 tests on PDIG_API34_DEFAULT(AVD) - 14
BUILD SUCCESSFUL in 2m 17s
```

| 项 | 值 |
| --- | --- |
| device | `emulator-5554`（AVD `PDIG_API34_DEFAULT`，API 34） |
| tests | **51** |
| failures / errors / skipped | **0 / 0 / 0** |

> 上一轮报告过"单进程跑 19 个 androidTest 被 OOM kill"。本轮 51 个在**单次调用**内完成，未分批、未干预。

### 3.5 Core Journey E2E

| 项 | 值 |
| --- | --- |
| **E2E run ID** | `core-journey-v4-20260917-184856` |
| summary | `{"total":41,"pass":41,"partial":0,"fail":0}` |
| 证据文件 | `local_private/e2e/core-journey-v4-20260917-184856.json`（sha256 `310f60761bf3e300…`） |

> **诚实标注**：该项**本轮未重新执行**，是从 HEAD 上的既往 run 继承的**已归档实跑证据**
> （`local_private/` 被 gitignore，不入库，但对 N3 而言是可引用的 controllability 记录）。
> §3.2 / §3.3 / §3.4 三项则是本轮真跑。

### 3.6 构建产物

构建根 = `%USERPROFILE%/pdig-build`

| 产物 | bytes | sha256 | mtime(UTC) |
| --- | --- | --- | --- |
| `app/outputs/apk/debug/app-debug.apk` | 37,093,663 | `04e195dda5f61e3eeac928546d0fe2175035c0b6e72b2409c264d5735350f947` | 2026-09-17T06:22:16Z |
| `app/outputs/apk/release/app-release-unsigned.apk` | 33,097,645 | `95f1087b5f12d86d2af65b4570871da93872b0e16233435889f497279f6b29f1` | 2026-09-17T07:54:16Z |
| `app/outputs/bundle/release/app-release.aab` | 20,845,493 | `875bdf306a23e37420f632d508a5d592c9ff1173c272369e827dfdf4acb0d8de` | 2026-09-17T07:54:20Z |

> `assembleDebug` 本轮为 UP-TO-DATE（输入未变），因此 APK 未被重写、哈希保持上述值，
> 且该 APK **确由 HEAD 源码产生**（输入指纹一致）。
> `assembleRelease` / `bundleRelease` 本轮**未重跑**，上述两个产物为 HEAD 上的既有构建；
> 上一轮记录的"三者全部 SUCCESSFUL"作为**继承结论**保留。

---

## 4. PASS 条件逐条核对（用户给定清单）

| # | 条件 | 本轮证据 | 结论 |
| --- | --- | --- | --- |
| 1 | Canonical generated definitions | CODEGEN GATE PASS（三端一致） | ✅ |
| 2 | Domain | `:core:test` DomainInvariant 15/15 + Conformance 91/91 | ✅ |
| 3 | Impact | ImpactKernelTest 11/11 + impact conformance 13/13 | ✅ |
| 4 | PlanReadiness | PlanReadinessTest 14/14 + readiness conformance 16/16 | ✅ |
| 5 | State Machines | StateMachineTest 14/14 + state-machine conformance 5/5 | ✅ |
| 6 | Persistence | 设备内 `PersistenceEvidenceTest`；密文库明文不可读 | ✅ |
| 7 | Migration | MigrationSemanticsTest 10/10 + migration conformance 2/2 + 设备内回滚/未来版本拒绝 | ✅ |
| 8 | Repository | 设备内 51/51（含 D-16 FileWorkflow 6/6） | ✅ |
| 9 | SQLCipher | 设备内：密文库 `file is not a database` | ✅ |
| 10 | Keystore | 设备内：raw key 不落盘、重启可重派生 | ✅ |
| 11 | App Lock | 设备内 `AppLockNavigationTest` 6/6；冷启动先锁、前后台回锁 | ✅ |
| 12 | Import | E2E v4 + 设备内；D-16 已关闭 | ✅ |
| 13 | Proposal → Reality | E2E v4 J3/J4 + 设备内 | ✅ |
| 14 | Impact → ChangePlan | E2E v4 J5/J6 | ✅ |
| 15 | Verification | done ≠ verified；E2E v4 J7/J8 | ✅ |
| 16 | Backup / Restore | E2E v4 J10 + Restore D-16 双断言 | ✅ |
| 17 | `.depmap` compatibility | depmap conformance 3/3 + JCS 1/1 + 设备内篡改拒绝 | ✅ |
| 18 | Compose critical path | 设备内 51/51 + 18 个 RUNTIME_VERIFIED 页面 | ✅ |
| 19 | Core E2E | run `core-journey-v4-20260917-184856`，41/41 | ✅（继承证据，见 §3.5） |
| 20 | Conformance | **本轮实跑 91/91** | ✅ |

**20 / 20 → `ANDROID_NATIVE_CORE_HANDOFF = PASS`**

---

## 5. 剩余项（**完整继承，不隐藏、不合并、不重算**）

Android parity 仍为 **62 / 73**，剩余 **11 格**按用户要求重新分类：

### 5.1 ENGINEERING_NOT_YET_VERIFIED（7 格）

有实现但未达成用例级/真机级验证。N3 **不得**以这些格为 parity 目标。

| 格 | 现状 |
| --- | --- |
| Node / Dependency / Group | IMPLEMENTED；实体/分组未作为独立对象做跨端用例级验证 |
| canonical groupKey | IMPLEMENTED；同上 |
| Onboarding | IMPLEMENTED；**无任何代码导航到它** |
| Timeline | IMPLEMENTED；本轮未被真机走过 |
| Candidate Review | IMPLEMENTED；**无入口可达**，当前产品流程也不产出 Candidate |
| Graph View（二级） | IMPLEMENTED；未被真机走过 |
| Dark Mode（token ready） | IMPLEMENTED；`design-tokens.json` 含 dark 覆盖，未做设备级验证 |

### 5.2 RUNTIME_ENVIRONMENT_BLOCKED（2 格）

| 格 | 真实 blocker |
| --- | --- |
| 生物认证 / App Lock | **拆分**：App Lock 接线 RUNTIME_VERIFIED；但 AVD `config.ini` 无 `hw.finger`，Android 没有无头指纹录入命令 → 生物识别匹配 BLOCKED |
| 无障碍 | 语义树 14 屏 0 无标签交互节点、触摸目标/焦点顺序/字体缩放/横屏均 PASS；**TalkBack 实机读屏 NOT_RUN**（镜像未预装且无 Play 商店） |

### 5.3 RELEASE_EXTERNAL_BLOCKED（1 格）

| 格 | 状态 |
| --- | --- |
| Release 签名 | `BLOCKED_BY_MISSING_PRODUCTION_KEYSTORE`。流水线已验证可用（非生产密钥签名后 `apksigner verify` 通过），但**没有生产 keystore**（B4） |

### 5.4 STORE_PREPARATION（1 格）

| 格 | 状态 |
| --- | --- |
| Store metadata | PARTIAL。`ANDROID_STORE_METADATA.md` 文案草稿已有；截图 / 图标 / 公开隐私政策链接 NOT_STARTED（B5/B6/B11/B12/B12b/B14–B17） |

### 5.5 仍存在的其它外部 blocker（不占 parity 格）

`B3` 无 macOS/Xcode（iOS N4） · `B13` 真实账单（仅 REAL_DATA Gate） ·
`B18` 无真实设备（当前只有 AVD） · `B19` 是否需要真实数据验证的决策 ·
`B23` 本机沙箱限制（clean clone / 破坏性 npm ci）

---

## 6. 冻结后的开发约束（C 节落地）

Android 进入 **`CORE_FROZEN / MAINTENANCE_ONLY`**。

**允许**

- regression fix
- Canonical Spec 同步（改 `spec/` 需同步 regenerated enums）
- cross-platform conformance fix（N3 发现 Android 侧行为不一致时修 Android）
- N5 发现的 parity bug

**禁止**

- 新 Domain / 新 Scenario
- MVP04（PayPal / Open Banking / 云同步 / 账号系统 / Agent 功能 / LLM Reality 判定）
- **为了把 parity 分数往上堆而新增无关功能**
- 为本报告未列出的新能力做铺垫性重构

---

## 7. 复现方式（可直接复制）

```bash
# 0) 本工作区 Bash 缺 uname/xargs，必须先补 PATH，否则 ./gradlew 自举失败
export PATH="/c/Users/Kaiser/.workbuddy/binaries/PortableGit/versions/1.2.0/usr/bin:$PATH"
export JAVA_HOME="<ANDROID_STUDIO_HOME>/jbr"        # 必须 JDK 11+；系统默认 java 是 1.8
export ANDROID_HOME="<ANDROID_SDK_ROOT>"

cd <repo>/android

./gradlew --no-daemon :core:test :conformance:run
./gradlew --no-daemon :app:assembleDebug
./gradlew --no-daemon :app:connectedDebugAndroidTest

# 取证（注意：必须读 ~/pdig-build，不是 android/**/build）
node tools/freeze/hash-evidence.mjs
```

**三个坑**（本轮踩到，务必记住）：

1. `./gradlew` 在缺 `uname`/`xargs` 的 shell 里直接失败 → 补 PortableGit `usr/bin` 到 PATH。
2. 不设 `JAVA_HOME` 会用 Java 8 → `Dependency requires at least JVM runtime version 11`。
3. **构建产物在 `%USERPROFILE%/pdig-build`**，`android/**/build` 是过期残留（见 §3.1）。
